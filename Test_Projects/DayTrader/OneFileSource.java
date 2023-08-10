/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collection;
//import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.naming.InitialContext;

import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.beans.RunStatsDataBean;
import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal;
import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBRemote;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * The TradeAction class provides the generic client side access to each of the
 * Trade brokerage user operations. These include login, logout, buy, sell,
 * getQuote, etc. The TradeAction class does not handle user interface
 * processing and should be used by a class that is UI specific. For example,
 * {trade_client.TradeServletAction}manages a web interface to Trade, making
 * calls to TradeAction methods to actually performance each operation.
 */
public class TradeAction implements TradeServices {

	// This lock is used to serialize market summary operations.
    private static final Integer marketSummaryLock = new Integer(0);
    private static long nextMarketSummary = System.currentTimeMillis();
    private static MarketSummaryDataBean cachedMSDB = MarketSummaryDataBean.getRandomInstance();
        
    // make this static so the trade impl can be cached
    // - ejb3 mode is the only thing that really uses this
    // - can go back and update other modes to take advantage (ie. TradeDirect)
    private static TradeServices trade = null;
    private static TradeServices tradeLocal = null;
    private static TradeServices tradeRemote = null;
    
    static {
                      
        // Determine if JPA Shared L2 Class is enabled
        // Depends on the <shared-cache-mode> in the persistence.xml
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();  
            InputStream is = loader.getResourceAsStream ("META-INF/persistence.xml"); 
       
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
           
            NodeList nList = doc.getElementsByTagName("shared-cache-mode");
            
            if (nList.getLength() != 0 && ((Element)nList.item(0)).getTextContent().equals("NONE")) {
                Log.log("JPA Shared L2 Cache disabled.");
            } else {
                Log.log("JPA Shared L2 Cache enabled.");
            }
        } catch (Exception e) {
            Log.log("Unable to determine if JPA Shared L2 Cache is enabled or disabled.");
            e.printStackTrace();
        }
                
    }

    public TradeAction() {
        if (Log.doTrace()) {
            Log.trace("TradeAction:TradeAction()");
        }
        createTrade();
    }

    public TradeAction(TradeServices trade) {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:TradeAction(trade)");
        }
        TradeAction.trade = trade;
    }

    private void createTrade() {
        if (TradeConfig.runTimeMode == TradeConfig.EJB3) {
            try {	     
            	
            	if (tradeLocal == null && tradeRemote == null ) {
            		InitialContext context = new InitialContext();
            		tradeLocal = (TradeSLSBLocal) context.lookup("java:comp/env/ejb/TradeSLSBBean");
            		tradeRemote = (TradeSLSBRemote) context.lookup("java:comp/env/ejb/TradeSLSBBeanRemote");
            	}
            	
            	// Determine local or remote interface.
            	if (!TradeConfig.useRemoteEJBInterface()) {
            		if (!(trade instanceof TradeSLSBLocal)) {
            			trade = tradeLocal;
            		}
            	} else if (!(trade instanceof TradeSLSBRemote)) {
            		/* TODO: For split tier this will need to be changed
            			I have not tried this yet with DT7 */
            		trade = tradeRemote;
            	}
            } catch (Exception e) {
                Log.error("TradeAction:TradeAction() Creation of Trade EJB 3 failed\n" + e);
                e.printStackTrace();
            }
        } else if (TradeConfig.runTimeMode == TradeConfig.DIRECT) {
            try {
                trade = new TradeDirect();
            } catch (Exception e) {
                Log.error("TradeAction:TradeAction() Creation of Trade Direct failed\n" + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Market Summary is inherently a heavy database operation. For servers that
     * have a caching story this is a great place to cache data that is good for
     * a period of time. In order to provide a flexible framework for this we
     * allow the market summary operation to be invoked on every transaction,
     * time delayed or never. This is configurable in the configuration panel.
     *
     * @return An instance of the market summary
     */
    @Override
    public MarketSummaryDataBean getMarketSummary() throws Exception {

        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getMarketSummary()");
        }

        // If EJB3 mode, then have the Singleton Bean handle this.
        if (TradeConfig.getRunTimeMode() == TradeConfig.EJB3)
        {
            if (Log.doActionTrace()) {
                Log.trace("TradeAction:getMarketSummary() -- EJB3 mode, using Singleton Bean");
            }
            return trade.getMarketSummary();
        }
                        
        if (TradeConfig.getMarketSummaryInterval() == 0) {
            return getMarketSummaryInternal();
        }
        if (TradeConfig.getMarketSummaryInterval() < 0) {
            return cachedMSDB;
        }

        /**
         * This is a little funky. If its time to fetch a new Market summary
         * then we'll synchronize access to make sure only one requester does
         * it. Others will merely return the old copy until the new
         * MarketSummary has been executed.
         */

        long currentTime = System.currentTimeMillis();

        if (currentTime > nextMarketSummary) {
            long oldNextMarketSummary = nextMarketSummary;
            boolean fetch = false;

            synchronized (marketSummaryLock) {
                /**
                 * Is it still ahead or did we miss lose the race? If we lost
                 * then let's get out of here as the work has already been done.
                 */
                if (oldNextMarketSummary == nextMarketSummary) {
                    fetch = true;
                    nextMarketSummary += TradeConfig.getMarketSummaryInterval() * 1000;

                    /**
                     * If the server has been idle for a while then its possible
                     * that nextMarketSummary could be way off. Rather than try
                     * and play catch up we'll simply get in sync with the
                     * current time + the interval.
                     */
                    if (nextMarketSummary < currentTime) {
                        nextMarketSummary = currentTime + TradeConfig.getMarketSummaryInterval() * 1000;
                    }
                }
            }

            /**
             * If we're the lucky one then let's update the MarketSummary
             */
            if (fetch) {
                cachedMSDB = getMarketSummaryInternal();
            }
        }

        return cachedMSDB;
    }

    /**
     * Compute and return a snapshot of the current market conditions This
     * includes the TSIA - an index of the price of the top 100 Trade stock
     * quotes The openTSIA ( the index at the open) The volume of shares traded,
     * Top Stocks gain and loss
     *
     * @return A snapshot of the current market summary
     */
    public MarketSummaryDataBean getMarketSummaryInternal() throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getMarketSummaryInternal()");
        }

        MarketSummaryDataBean marketSummaryData = null;
        marketSummaryData = trade.getMarketSummary();
        return marketSummaryData;
    }

    /**
     * Purchase a stock and create a new holding for the given user. Given a
     * stock symbol and quantity to purchase, retrieve the current quote price,
     * debit the user's account balance, and add holdings to user's portfolio.
     *
     * @param userID
     *            the customer requesting the stock purchase
     * @param symbol
     *            the symbol of the stock being purchased
     * @param quantity
     *            the quantity of shares to purchase
     * @return OrderDataBean providing the status of the newly created buy order
     */
    @Override
    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:buy", userID, symbol, new Double(quantity), new Integer(orderProcessingMode));
        }
        OrderDataBean orderData = trade.buy(userID, symbol, quantity, orderProcessingMode);
        
        // after the purchase or sell of a stock, update the stocks volume and
        // price

        updateQuotePriceVolume(symbol, TradeConfig.getRandomPriceChangeFactor(), quantity);

        return orderData;
    }

    /**
     * Sell(SOAP 2.2 Wrapper converting int to Integer) a stock holding and
     * removed the holding for the given user. Given a Holding, retrieve current
     * quote, credit user's account, and reduce holdings in user's portfolio.
     *
     * @param userID
     *            the customer requesting the sell
     * @param holdingID
     *            the users holding to be sold
     * @return OrderDataBean providing the status of the newly created sell
     *         order
     */
    public OrderDataBean sell(String userID, int holdingID, int orderProcessingMode) throws Exception {
        return sell(userID, new Integer(holdingID), orderProcessingMode);
    }

    /**
     * Sell a stock holding and removed the holding for the given user. Given a
     * Holding, retrieve current quote, credit user's account, and reduce
     * holdings in user's portfolio.
     *
     * @param userID
     *            the customer requesting the sell
     * @param holdingID
     *            the users holding to be sold
     * @return OrderDataBean providing the status of the newly created sell
     *         order
     */
    @Override
    public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:sell", userID, holdingID, new Integer(orderProcessingMode));
        }
        OrderDataBean orderData = trade.sell(userID, holdingID, orderProcessingMode);
       

        if (!orderData.getOrderStatus().equalsIgnoreCase("cancelled")) {
            updateQuotePriceVolume(orderData.getSymbol(), TradeConfig.getRandomPriceChangeFactor(), orderData.getQuantity());
        }

        return orderData;
    }

    /**
     * Queue the Order identified by orderID to be processed
     * <p/>
     * Orders are submitted through JMS to a Trading Broker and completed
     * asynchronously. This method queues the order for processing
     * <p/>
     * The boolean twoPhase specifies to the server implementation whether or
     * not the method is to participate in a global transaction
     *
     * @param orderID
     *            the Order being queued for processing
     */
    @Override
    public void queueOrder(Integer orderID, boolean twoPhase) {
        throw new UnsupportedOperationException("TradeAction: queueOrder method not supported");
    }

    /**
     * Complete the Order identefied by orderID Orders are submitted through JMS
     * to a Trading agent and completed asynchronously. This method completes
     * the order For a buy, the stock is purchased creating a holding and the
     * users account is debited For a sell, the stock holding is removed and the
     * users account is credited with the proceeds
     * <p/>
     * The boolean twoPhase specifies to the server implementation whether or
     * not the method is to participate in a global transaction
     *
     * @param orderID
     *            the Order to complete
     * @return OrderDataBean providing the status of the completed order
     */
    @Override
    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) {
        throw new UnsupportedOperationException("TradeAction: completeOrder method not supported");
    }

    /**
     * Cancel the Order identified by orderID
     * <p/>
     * Orders are submitted through JMS to a Trading Broker and completed
     * asynchronously. This method queues the order for processing
     * <p/>
     * The boolean twoPhase specifies to the server implementation whether or
     * not the method is to participate in a global transaction
     *
     * @param orderID
     *            the Order being queued for processing
     */
    @Override
    public void cancelOrder(Integer orderID, boolean twoPhase) {
        throw new UnsupportedOperationException("TradeAction: cancelOrder method not supported");
    }

    @Override
    public void orderCompleted(String userID, Integer orderID) throws Exception {

        if (Log.doActionTrace()) {
            Log.trace("TradeAction:orderCompleted", userID, orderID);
        }
        if (Log.doTrace()) {
            Log.trace("OrderCompleted", userID, orderID);
        }
    }

    /**
     * Get the collection of all orders for a given account
     *
     * @param userID
     *            the customer account to retrieve orders for
     * @return Collection OrderDataBeans providing detailed order information
     */
    @Override
    public Collection<?> getOrders(String userID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getOrders", userID);
        }
        Collection<?> orderDataBeans = trade.getOrders(userID);
        
        return orderDataBeans;
    }

    /**
     * Get the collection of completed orders for a given account that need to
     * be alerted to the user
     *
     * @param userID
     *            the customer account to retrieve orders for
     * @return Collection OrderDataBeans providing detailed order information
     */
    @Override
    public Collection<?> getClosedOrders(String userID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getClosedOrders", userID);
        }
        
        Collection<?> orderDataBeans =  trade.getClosedOrders(userID);
        
        return orderDataBeans;
    }

    /**
     * Given a market symbol, price, and details, create and return a new
     * {@link QuoteDataBean}
     *
     * @param symbol
     *            the symbol of the stock
     * @param price
     *            the current stock price
     * @return a new QuoteDataBean or null if Quote could not be created
     */
    @Override
    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:createQuote", symbol, companyName, price);
        }
      
     
        return trade.createQuote(symbol, companyName, price);
    
    }

    /**
     * Return a collection of {@link QuoteDataBean}describing all current quotes
     *
     * @return the collection of QuoteDataBean
     */
    @Override
    public Collection<?> getAllQuotes() throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getAllQuotes");
        }
        
        return trade.getAllQuotes();
        
    }

    /**
     * Return a {@link QuoteDataBean}describing a current quote for the given
     * stock symbol
     *
     * @param symbol
     *            the stock symbol to retrieve the current Quote
     * @return the QuoteDataBean
     */
    @Override
    public QuoteDataBean getQuote(String symbol) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getQuote", symbol);
        }
        if ((symbol == null) || (symbol.length() == 0) || (symbol.length() > 10)) {
            if (Log.doActionTrace()) {
                Log.trace("TradeAction:getQuote   ---  primitive workload");
            }
            return new QuoteDataBean("Invalid symbol", "", 0.0, FinancialUtils.ZERO, FinancialUtils.ZERO, FinancialUtils.ZERO, FinancialUtils.ZERO, 0.0);
        }
        
        QuoteDataBean quoteData = trade.getQuote(symbol);
        
        return quoteData;
    }

    /**
     * Update the stock quote price for the specified stock symbol
     *
     * @param symbol
     *            for stock quote to update
     * @return the QuoteDataBean describing the stock
     */
    /* avoid data collision with synch */
    @Override
    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:updateQuotePriceVolume", symbol, changeFactor, new Double(sharesTraded));
        }
        QuoteDataBean quoteData = null;
        try {
        	quoteData = trade.updateQuotePriceVolume(symbol, changeFactor, sharesTraded);
        } catch (Exception e) {
            Log.error("TradeAction:updateQuotePrice -- ", e);
        }

        return quoteData;

    }

    /**
     * Return the portfolio of stock holdings for the specified customer as a
     * collection of HoldingDataBeans
     *
     * @param userID
     *            the customer requesting the portfolio
     * @return Collection of the users portfolio of stock holdings
     */
    @Override
    public Collection<?> getHoldings(String userID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getHoldings", userID);
        }
        
        Collection<?> holdingDataBeans = trade.getHoldings(userID);
        
        return holdingDataBeans;
    }

    /**
     * Return a specific user stock holding identifed by the holdingID
     *
     * @param holdingID
     *            the holdingID to return
     * @return a HoldingDataBean describing the holding
     */
    @Override
    public HoldingDataBean getHolding(Integer holdingID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getHolding", holdingID);
        }

        return trade.getHolding(holdingID);
    }

    /**
     * Return an AccountDataBean object for userID describing the account
     *
     * @param userID
     *            the account userID to lookup
     * @return User account data in AccountDataBean
     */
    @Override
    public AccountDataBean getAccountData(String userID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getAccountData", userID);
        }
        AccountDataBean accountData = trade.getAccountData(userID);
        
        return accountData;
    }

    /**
     * Return an AccountProfileDataBean for userID providing the users profile
     *
     * @param userID
     *            the account userID to lookup
     */
    @Override
    public AccountProfileDataBean getAccountProfileData(String userID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getAccountProfileData", userID);
        }
        AccountProfileDataBean accountProfileData = trade.getAccountProfileData(userID);
        
        return accountProfileData;
    }

    /**
     * Update userID's account profile information using the provided
     * AccountProfileDataBean object
     *
     * @param accountProfileData
     *            account profile data in AccountProfileDataBean
     */
    @Override
    public AccountProfileDataBean updateAccountProfile(AccountProfileDataBean accountProfileData) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:updateAccountProfile", accountProfileData);
        }

        accountProfileData = trade.updateAccountProfile(accountProfileData);        
        return accountProfileData;
    }

    /**
     * Attempt to authenticate and login a user with the given password
     *
     * @param userID
     *            the customer to login
     * @param password
     *            the password entered by the customer for authentication
     * @return User account data in AccountDataBean
     */
    @Override
    public AccountDataBean login(String userID, String password) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:login", userID, password);
        }
        AccountDataBean accountData = trade.login(userID, password);
                
        return accountData;
    }

    /**
     * Logout the given user
     *
     * @param userID
     *            the customer to logout
     */
    @Override
    public void logout(String userID) throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:logout", userID);
        }
    
        trade.logout(userID);
        
    }

    /**
     * Register a new Trade customer. Create a new user profile, user registry
     * entry, account with initial balance, and empty portfolio.
     *
     * @param userID
     *            the new customer to register
     * @param password
     *            the customers password
     * @param fullname
     *            the customers fullname
     * @param address
     *            the customers street address
     * @param email
     *            the customers email address
     * @param creditCard
     *            the customers creditcard number
     * @param openBalance
     *            the amount to charge to the customers credit to open the
     *            account and set the initial balance
     * @return the userID if successful, null otherwise
     */
    @Override
    public AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditCard, BigDecimal openBalance)
            throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:register", userID, password, fullname, address, email, creditCard, openBalance);
        }
       
        return trade.register(userID, password, fullname, address, email, creditCard, openBalance);     
    }

    public AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditCard, String openBalanceString)
            throws Exception {
        BigDecimal openBalance = new BigDecimal(openBalanceString);
        return register(userID, password, fullname, address, email, creditCard, openBalance);
    }

    /**
     * Reset the TradeData by - removing all newly registered users by scenario
     * servlet (i.e. users with userID's beginning with "ru:") * - removing all
     * buy/sell order pairs - setting logoutCount = loginCount
     *
     * return statistics for this benchmark run
     */
    @Override
    public RunStatsDataBean resetTrade(boolean deleteAll) throws Exception {
        RunStatsDataBean runStatsData = trade.resetTrade(deleteAll);
                
        return runStatsData;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.Collection;

import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.beans.RunStatsDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;

/**
 * TradeServices interface specifies the business methods provided by the Trade
 * online broker application. These business methods represent the features and
 * operations that can be performed by customers of the brokerage such as login,
 * logout, get a stock quote, buy or sell a stock, etc. This interface is
 * implemented by {@link Trade} providing an EJB implementation of these
 * business methods and also by {@link TradeDirect} providing a JDBC
 * implementation.
 *
 * @see Trade
 * @see TradeDirect
 *
 */

public interface TradeServices {

    /**
     * Compute and return a snapshot of the current market conditions This
     * includes the TSIA - an index of the price of the top 100 Trade stock
     * quotes The openTSIA ( the index at the open) The volume of shares traded,
     * Top Stocks gain and loss
     *
     * @return A snapshot of the current market summary
     */
    MarketSummaryDataBean getMarketSummary() throws Exception;

    /**
     * Purchase a stock and create a new holding for the given user. Given a
     * stock symbol and quantity to purchase, retrieve the current quote price,
     * debit the user's account balance, and add holdings to user's portfolio.
     * buy/sell are asynchronous, using J2EE messaging, A new order is created
     * and submitted for processing to the TradeBroker
     *
     * @param userID
     *            the customer requesting the stock purchase
     * @param symbol
     *            the symbol of the stock being purchased
     * @param quantity
     *            the quantity of shares to purchase
     * @return OrderDataBean providing the status of the newly created buy order
     */

    OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws Exception;

    /**
     * Sell a stock holding and removed the holding for the given user. Given a
     * Holding, retrieve current quote, credit user's account, and reduce
     * holdings in user's portfolio.
     *
     * @param userID
     *            the customer requesting the sell
     * @param holdingID
     *            the users holding to be sold
     * @return OrderDataBean providing the status of the newly created sell
     *         order
     */
    OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws Exception;

    /**
     * Queue the Order identified by orderID to be processed
     *
     * Orders are submitted through JMS to a Trading Broker and completed
     * asynchronously. This method queues the order for processing
     *
     * The boolean twoPhase specifies to the server implementation whether or
     * not the method is to participate in a global transaction
     *
     * @param orderID
     *            the Order being queued for processing
     * @return OrderDataBean providing the status of the completed order
     */
    void queueOrder(Integer orderID, boolean twoPhase) throws Exception;

    /**
     * Complete the Order identefied by orderID Orders are submitted through JMS
     * to a Trading agent and completed asynchronously. This method completes
     * the order For a buy, the stock is purchased creating a holding and the
     * users account is debited For a sell, the stock holding is removed and the
     * users account is credited with the proceeds
     *
     * The boolean twoPhase specifies to the server implementation whether or
     * not the method is to participate in a global transaction
     *
     * @param orderID
     *            the Order to complete
     * @return OrderDataBean providing the status of the completed order
     */
    OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception;

    /**
     * Cancel the Order identefied by orderID
     *
     * The boolean twoPhase specifies to the server implementation whether or
     * not the method is to participate in a global transaction
     *
     * @param orderID
     *            the Order to complete
     * @return OrderDataBean providing the status of the completed order
     */
    void cancelOrder(Integer orderID, boolean twoPhase) throws Exception;

    /**
     * Signify an order has been completed for the given userID
     *
     * @param userID
     *            the user for which an order has completed
     * @param orderID
     *            the order which has completed
     *
     */
    void orderCompleted(String userID, Integer orderID) throws Exception;

    /**
     * Get the collection of all orders for a given account
     *
     * @param userID
     *            the customer account to retrieve orders for
     * @return Collection OrderDataBeans providing detailed order information
     */
    Collection<?> getOrders(String userID) throws Exception;

    /**
     * Get the collection of completed orders for a given account that need to
     * be alerted to the user
     *
     * @param userID
     *            the customer account to retrieve orders for
     * @return Collection OrderDataBeans providing detailed order information
     */
    Collection<?> getClosedOrders(String userID) throws Exception;

    /**
     * Given a market symbol, price, and details, create and return a new
     * {@link QuoteDataBean}
     *
     * @param symbol
     *            the symbol of the stock
     * @param price
     *            the current stock price
     * @param details
     *            a short description of the stock or company
     * @return a new QuoteDataBean or null if Quote could not be created
     */
    QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception;

    /**
     * Return a {@link QuoteDataBean} describing a current quote for the given
     * stock symbol
     *
     * @param symbol
     *            the stock symbol to retrieve the current Quote
     * @return the QuoteDataBean
     */
    QuoteDataBean getQuote(String symbol) throws Exception;

    /**
     * Return a {@link java.util.Collection} of {@link QuoteDataBean} describing
     * all current quotes
     *
     * @return A collection of QuoteDataBean
     */
    Collection<?> getAllQuotes() throws Exception;

    /**
     * Update the stock quote price and volume for the specified stock symbol
     *
     * @param symbol
     *            for stock quote to update
     * @param price
     *            the updated quote price
     * @return the QuoteDataBean describing the stock
     */
    QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal newPrice, double sharesTraded) throws Exception;

    /**
     * Return the portfolio of stock holdings for the specified customer as a
     * collection of HoldingDataBeans
     *
     * @param userID
     *            the customer requesting the portfolio
     * @return Collection of the users portfolio of stock holdings
     */
    Collection<?> getHoldings(String userID) throws Exception;

    /**
     * Return a specific user stock holding identifed by the holdingID
     *
     * @param holdingID
     *            the holdingID to return
     * @return a HoldingDataBean describing the holding
     */
    HoldingDataBean getHolding(Integer holdingID) throws Exception;

    /**
     * Return an AccountDataBean object for userID describing the account
     *
     * @param userID
     *            the account userID to lookup
     * @return User account data in AccountDataBean
     */
    AccountDataBean getAccountData(String userID) throws Exception;

    /**
     * Return an AccountProfileDataBean for userID providing the users profile
     *
     * @param userID
     *            the account userID to lookup
     * @param User
     *            account profile data in AccountProfileDataBean
     */
    AccountProfileDataBean getAccountProfileData(String userID) throws Exception;

    /**
     * Update userID's account profile information using the provided
     * AccountProfileDataBean object
     *
     * @param userID
     *            the account userID to lookup
     * @param User
     *            account profile data in AccountProfileDataBean
     */
    AccountProfileDataBean updateAccountProfile(AccountProfileDataBean profileData) throws Exception;

    /**
     * Attempt to authenticate and login a user with the given password
     *
     * @param userID
     *            the customer to login
     * @param password
     *            the password entered by the customer for authentication
     * @return User account data in AccountDataBean
     */
    AccountDataBean login(String userID, String password) throws Exception;

    /**
     * Logout the given user
     *
     * @param userID
     *            the customer to logout
     * @return the login status
     */

    void logout(String userID) throws Exception;

    /**
     * Register a new Trade customer. Create a new user profile, user registry
     * entry, account with initial balance, and empty portfolio.
     *
     * @param userID
     *            the new customer to register
     * @param password
     *            the customers password
     * @param fullname
     *            the customers fullname
     * @param address
     *            the customers street address
     * @param email
     *            the customers email address
     * @param creditcard
     *            the customers creditcard number
     * @param initialBalance
     *            the amount to charge to the customers credit to open the
     *            account and set the initial balance
     * @return the userID if successful, null otherwise
     */
    AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditcard, BigDecimal openBalance)
            throws Exception;

    /**
     * Reset the TradeData by - removing all newly registered users by scenario
     * servlet (i.e. users with userID's beginning with "ru:") * - removing all
     * buy/sell order pairs - setting logoutCount = loginCount
     *
     * return statistics for this benchmark run
     */
    RunStatsDataBean resetTrade(boolean deleteAll) throws Exception;
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

public class MarketSummaryDataBean implements Serializable {

    private static final long serialVersionUID = 650652242288745600L;
    private BigDecimal TSIA; /* Trade Stock Index Average */
    private BigDecimal openTSIA; /* Trade Stock Index Average at the open */
    private double volume; /* volume of shares traded */
    private Collection<QuoteDataBean> topGainers; /*
                                                   * Collection of top gaining
                                                   * stocks
                                                   */
    private Collection<QuoteDataBean> topLosers; /*
                                                  * Collection of top losing
                                                  * stocks
                                                  */
    // FUTURE private Collection topVolume; /* Collection of top stocks by
    // volume */
    private Date summaryDate; /* Date this summary was taken */

    // cache the gainPercent once computed for this bean
    private BigDecimal gainPercent = null;

    public MarketSummaryDataBean() {
    }

    public MarketSummaryDataBean(BigDecimal TSIA, BigDecimal openTSIA, double volume, Collection<QuoteDataBean> topGainers, Collection<QuoteDataBean> topLosers// , Collection topVolume
    ) {
        setTSIA(TSIA);
        setOpenTSIA(openTSIA);
        setVolume(volume);
        setTopGainers(topGainers);
        setTopLosers(topLosers);
        setSummaryDate(new java.sql.Date(System.currentTimeMillis()));
        gainPercent = FinancialUtils.computeGainPercent(getTSIA(), getOpenTSIA());

    }

    public static MarketSummaryDataBean getRandomInstance() {
        Collection<QuoteDataBean> gain = new ArrayList<QuoteDataBean>();
        Collection<QuoteDataBean> lose = new ArrayList<QuoteDataBean>();

        for (int ii = 0; ii < 5; ii++) {
            QuoteDataBean quote1 = QuoteDataBean.getRandomInstance();
            QuoteDataBean quote2 = QuoteDataBean.getRandomInstance();

            gain.add(quote1);
            lose.add(quote2);
        }

        return new MarketSummaryDataBean(TradeConfig.rndBigDecimal(1000000.0f), TradeConfig.rndBigDecimal(1000000.0f), TradeConfig.rndQuantity(), gain, lose);
    }

    @Override
    public String toString() {
        String ret = "\n\tMarket Summary at: " + getSummaryDate() + "\n\t\t        TSIA:" + getTSIA() + "\n\t\t    openTSIA:" + getOpenTSIA()
                + "\n\t\t        gain:" + getGainPercent() + "\n\t\t      volume:" + getVolume();

        if ((getTopGainers() == null) || (getTopLosers() == null)) {
            return ret;
        }
        ret += "\n\t\t   Current Top Gainers:";
        Iterator<QuoteDataBean> it = getTopGainers().iterator();
        while (it.hasNext()) {
            QuoteDataBean quoteData = it.next();
            ret += ("\n\t\t\t" + quoteData.toString());
        }
        ret += "\n\t\t   Current Top Losers:";
        it = getTopLosers().iterator();
        while (it.hasNext()) {
            QuoteDataBean quoteData = it.next();
            ret += ("\n\t\t\t" + quoteData.toString());
        }
        return ret;
    }

    public String toHTML() {
        String ret = "<BR>Market Summary at: " + getSummaryDate() + "<LI>        TSIA:" + getTSIA() + "</LI>" + "<LI>    openTSIA:" + getOpenTSIA() + "</LI>"
                + "<LI>      volume:" + getVolume() + "</LI>";
        if ((getTopGainers() == null) || (getTopLosers() == null)) {
            return ret;
        }
        ret += "<BR> Current Top Gainers:";
        Iterator<QuoteDataBean> it = getTopGainers().iterator();

        while (it.hasNext()) {
            QuoteDataBean quoteData = it.next();
            ret += ("<LI>" + quoteData.toString() + "</LI>");
        }
        ret += "<BR>   Current Top Losers:";
        it = getTopLosers().iterator();
        while (it.hasNext()) {
            QuoteDataBean quoteData = it.next();
            ret += ("<LI>" + quoteData.toString() + "</LI>");
        }
        return ret;
    }

    public JsonObject toJSON() {
        
        JsonObjectBuilder jObjectBuilder = Json.createObjectBuilder();
        
        int i = 1;
        for (Iterator<QuoteDataBean> iterator = topGainers.iterator(); iterator.hasNext();) {
            QuoteDataBean quote = iterator.next();

            jObjectBuilder.add("gainer" + i + "_stock",quote.getSymbol());
            jObjectBuilder.add("gainer" + i + "_price","$" + quote.getPrice());
            jObjectBuilder.add("gainer" + i + "_change",quote.getChange());
            i++;
        }

        i = 1;
        for (Iterator<QuoteDataBean> iterator = topLosers.iterator(); iterator.hasNext();) {
            QuoteDataBean quote = iterator.next();

            jObjectBuilder.add("loser" + i + "_stock",quote.getSymbol());
            jObjectBuilder.add("loser" + i + "_price","$" + quote.getPrice());
            jObjectBuilder.add("loser" + i + "_change",quote.getChange());
            i++;
        }

        jObjectBuilder.add("tsia", TSIA);
        jObjectBuilder.add("volume",volume);
        jObjectBuilder.add("date", summaryDate.toString());

        return jObjectBuilder.build();
        
    }

    public void print() {
        Log.log(this.toString());
    }

    public BigDecimal getGainPercent() {
        if (gainPercent == null) {
            gainPercent = FinancialUtils.computeGainPercent(getTSIA(), getOpenTSIA());
        }
        return gainPercent;
    }

    /**
     * Gets the tSIA
     *
     * @return Returns a BigDecimal
     */
    public BigDecimal getTSIA() {
        return TSIA;
    }

    /**
     * Sets the tSIA
     *
     * @param tSIA
     *            The tSIA to set
     */
    public void setTSIA(BigDecimal tSIA) {
        TSIA = tSIA;
    }

    /**
     * Gets the openTSIA
     *
     * @return Returns a BigDecimal
     */
    public BigDecimal getOpenTSIA() {
        return openTSIA;
    }

    /**
     * Sets the openTSIA
     *
     * @param openTSIA
     *            The openTSIA to set
     */
    public void setOpenTSIA(BigDecimal openTSIA) {
        this.openTSIA = openTSIA;
    }

    /**
     * Gets the volume
     *
     * @return Returns a BigDecimal
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Sets the volume
     *
     * @param volume
     *            The volume to set
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }

    /**
     * Gets the topGainers
     *
     * @return Returns a Collection
     */
    public Collection<QuoteDataBean> getTopGainers() {
        return topGainers;
    }

    /**
     * Sets the topGainers
     *
     * @param topGainers
     *            The topGainers to set
     */
    public void setTopGainers(Collection<QuoteDataBean> topGainers) {
        this.topGainers = topGainers;
    }

    /**
     * Gets the topLosers
     *
     * @return Returns a Collection
     */
    public Collection<QuoteDataBean> getTopLosers() {
        return topLosers;
    }

    /**
     * Sets the topLosers
     *
     * @param topLosers
     *            The topLosers to set
     */
    public void setTopLosers(Collection<QuoteDataBean> topLosers) {
        this.topLosers = topLosers;
    }

    /**
     * Gets the summaryDate
     *
     * @return Returns a Date
     */
    public Date getSummaryDate() {
        return summaryDate;
    }

    /**
     * Sets the summaryDate
     *
     * @param summaryDate
     *            The summaryDate to set
     */
    public void setSummaryDate(Date summaryDate) {
        this.summaryDate = summaryDate;
    }

}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;

public class RunStatsDataBean implements Serializable {
    private static final long serialVersionUID = 4017778674103242167L;

    // Constructors
    public RunStatsDataBean() {
    }

    // count of trade users in the database (users w/ userID like 'uid:%')
    private int tradeUserCount;
    // count of trade stocks in the database (stocks w/ symbol like 's:%')
    private int tradeStockCount;

    // count of new registered users in this run (users w/ userID like 'ru:%')
    // -- random user
    private int newUserCount;

    // sum of logins by trade users
    private int sumLoginCount;
    // sum of logouts by trade users
    private int sumLogoutCount;

    // count of holdings of trade users
    private int holdingCount;

    // count of orders of trade users
    private int orderCount;
    // count of buy orders of trade users
    private int buyOrderCount;
    // count of sell orders of trade users
    private int sellOrderCount;
    // count of cancelled orders of trade users
    private int cancelledOrderCount;
    // count of open orders of trade users
    private int openOrderCount;
    // count of orders deleted during this trade Reset
    private int deletedOrderCount;

    @Override
    public String toString() {
        return "\n\tRunStatsData for reset at " + new java.util.Date() + "\n\t\t      tradeUserCount: " + getTradeUserCount() + "\n\t\t        newUserCount: "
                + getNewUserCount() + "\n\t\t       sumLoginCount: " + getSumLoginCount() + "\n\t\t      sumLogoutCount: " + getSumLogoutCount()
                + "\n\t\t        holdingCount: " + getHoldingCount() + "\n\t\t          orderCount: " + getOrderCount() + "\n\t\t       buyOrderCount: "
                + getBuyOrderCount() + "\n\t\t      sellOrderCount: " + getSellOrderCount() + "\n\t\t cancelledOrderCount: " + getCancelledOrderCount()
                + "\n\t\t      openOrderCount: " + getOpenOrderCount() + "\n\t\t   deletedOrderCount: " + getDeletedOrderCount();
    }

    /**
     * Gets the tradeUserCount
     *
     * @return Returns a int
     */
    public int getTradeUserCount() {
        return tradeUserCount;
    }

    /**
     * Sets the tradeUserCount
     *
     * @param tradeUserCount
     *            The tradeUserCount to set
     */
    public void setTradeUserCount(int tradeUserCount) {
        this.tradeUserCount = tradeUserCount;
    }

    /**
     * Gets the newUserCount
     *
     * @return Returns a int
     */
    public int getNewUserCount() {
        return newUserCount;
    }

    /**
     * Sets the newUserCount
     *
     * @param newUserCount
     *            The newUserCount to set
     */
    public void setNewUserCount(int newUserCount) {
        this.newUserCount = newUserCount;
    }

    /**
     * Gets the sumLoginCount
     *
     * @return Returns a int
     */
    public int getSumLoginCount() {
        return sumLoginCount;
    }

    /**
     * Sets the sumLoginCount
     *
     * @param sumLoginCount
     *            The sumLoginCount to set
     */
    public void setSumLoginCount(int sumLoginCount) {
        this.sumLoginCount = sumLoginCount;
    }

    /**
     * Gets the sumLogoutCount
     *
     * @return Returns a int
     */
    public int getSumLogoutCount() {
        return sumLogoutCount;
    }

    /**
     * Sets the sumLogoutCount
     *
     * @param sumLogoutCount
     *            The sumLogoutCount to set
     */
    public void setSumLogoutCount(int sumLogoutCount) {
        this.sumLogoutCount = sumLogoutCount;
    }

    /**
     * Gets the holdingCount
     *
     * @return Returns a int
     */
    public int getHoldingCount() {
        return holdingCount;
    }

    /**
     * Sets the holdingCount
     *
     * @param holdingCount
     *            The holdingCount to set
     */
    public void setHoldingCount(int holdingCount) {
        this.holdingCount = holdingCount;
    }

    /**
     * Gets the buyOrderCount
     *
     * @return Returns a int
     */
    public int getBuyOrderCount() {
        return buyOrderCount;
    }

    /**
     * Sets the buyOrderCount
     *
     * @param buyOrderCount
     *            The buyOrderCount to set
     */
    public void setBuyOrderCount(int buyOrderCount) {
        this.buyOrderCount = buyOrderCount;
    }

    /**
     * Gets the sellOrderCount
     *
     * @return Returns a int
     */
    public int getSellOrderCount() {
        return sellOrderCount;
    }

    /**
     * Sets the sellOrderCount
     *
     * @param sellOrderCount
     *            The sellOrderCount to set
     */
    public void setSellOrderCount(int sellOrderCount) {
        this.sellOrderCount = sellOrderCount;
    }

    /**
     * Gets the cancelledOrderCount
     *
     * @return Returns a int
     */
    public int getCancelledOrderCount() {
        return cancelledOrderCount;
    }

    /**
     * Sets the cancelledOrderCount
     *
     * @param cancelledOrderCount
     *            The cancelledOrderCount to set
     */
    public void setCancelledOrderCount(int cancelledOrderCount) {
        this.cancelledOrderCount = cancelledOrderCount;
    }

    /**
     * Gets the openOrderCount
     *
     * @return Returns a int
     */
    public int getOpenOrderCount() {
        return openOrderCount;
    }

    /**
     * Sets the openOrderCount
     *
     * @param openOrderCount
     *            The openOrderCount to set
     */
    public void setOpenOrderCount(int openOrderCount) {
        this.openOrderCount = openOrderCount;
    }

    /**
     * Gets the deletedOrderCount
     *
     * @return Returns a int
     */
    public int getDeletedOrderCount() {
        return deletedOrderCount;
    }

    /**
     * Sets the deletedOrderCount
     *
     * @param deletedOrderCount
     *            The deletedOrderCount to set
     */
    public void setDeletedOrderCount(int deletedOrderCount) {
        this.deletedOrderCount = deletedOrderCount;
    }

    /**
     * Gets the orderCount
     *
     * @return Returns a int
     */
    public int getOrderCount() {
        return orderCount;
    }

    /**
     * Sets the orderCount
     *
     * @param orderCount
     *            The orderCount to set
     */
    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    /**
     * Gets the tradeStockCount
     *
     * @return Returns a int
     */
    public int getTradeStockCount() {
        return tradeStockCount;
    }

    /**
     * Sets the tradeStockCount
     *
     * @param tradeStockCount
     *            The tradeStockCount to set
     */
    public void setTradeStockCount(int tradeStockCount) {
        this.tradeStockCount = tradeStockCount;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.samples.daytrader.util.KeyBlock;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

public class KeySequenceDirect {

    private static HashMap<String, Collection<?>> keyMap = new HashMap<String, Collection<?>>();

    public static synchronized Integer getNextID(Connection conn, String keyName, boolean inSession, boolean inGlobalTxn) throws Exception {
        Integer nextID = null;
        // First verify we have allocated a block of keys
        // for this key name
        // Then verify the allocated block has not been depleted
        // allocate a new block if necessary
        if (keyMap.containsKey(keyName) == false) {
            allocNewBlock(conn, keyName, inSession, inGlobalTxn);
        }
        Collection<?> block = keyMap.get(keyName);

        Iterator<?> ids = block.iterator();
        if (ids.hasNext() == false) {
            ids = allocNewBlock(conn, keyName, inSession, inGlobalTxn).iterator();
        }
        // get and return a new unique key
        nextID = (Integer) ids.next();

        if (Log.doTrace()) {
            Log.trace("KeySequenceDirect:getNextID inSession(" + inSession + ") - return new PK ID for Entity type: " + keyName + " ID=" + nextID);
        }
        return nextID;
    }

    private static Collection<?> allocNewBlock(Connection conn, String keyName, boolean inSession, boolean inGlobalTxn) throws Exception {
        try {

            if (inGlobalTxn == false && !inSession) {
                conn.commit(); // commit any pending txns
            }

            PreparedStatement stmt = conn.prepareStatement(getKeyForUpdateSQL);
            stmt.setString(1, keyName);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // No keys found for this name - create a new one
                PreparedStatement stmt2 = conn.prepareStatement(createKeySQL);
                int keyVal = 0;
                stmt2.setString(1, keyName);
                stmt2.setInt(2, keyVal);
                stmt2.executeUpdate();
                stmt2.close();
                stmt.close();
                stmt = conn.prepareStatement(getKeyForUpdateSQL);
                stmt.setString(1, keyName);
                rs = stmt.executeQuery();
                rs.next();
            }

            int keyVal = rs.getInt("keyval");

            stmt.close();

            stmt = conn.prepareStatement(updateKeyValueSQL);
            stmt.setInt(1, keyVal + TradeConfig.KEYBLOCKSIZE);
            stmt.setString(2, keyName);
            stmt.executeUpdate();
            stmt.close();

            Collection<?> block = new KeyBlock(keyVal, keyVal + TradeConfig.KEYBLOCKSIZE - 1);
            keyMap.put(keyName, block);

            if (inGlobalTxn == false && !inSession) {
                conn.commit();
            }

            return block;
        } catch (Exception e) {
            String error = "KeySequenceDirect:allocNewBlock - failure to allocate new block of keys for Entity type: " + keyName;
            Log.error(e, error);
            throw new Exception(error + e.toString());
        }
    }

    private static final String getKeyForUpdateSQL = "select * from keygenejb kg where kg.keyname = ?  for update";

    private static final String createKeySQL = "insert into keygenejb " + "( keyname, keyval ) " + "VALUES (  ?  ,  ? )";

    private static final String updateKeyValueSQL = "update keygenejb set keyval = ? " + "where keyname = ?";

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.beans.RunStatsDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.CompleteOrderThread;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.MDBStats;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * TradeDirect uses direct JDBC and JMS access to a
 * <code>javax.sql.DataSource</code> to implement the business methods of the
 * Trade online broker application. These business methods represent the
 * features and operations that can be performed by customers of the brokerage
 * such as login, logout, get a stock quote, buy or sell a stock, etc. and are
 * specified in the {@link com.ibm.websphere.samples.daytrader.TradeServices}
 * interface
 *
 * Note: In order for this class to be thread-safe, a new TradeJDBC must be
 * created for each call to a method from the TradeInterface interface.
 * Otherwise, pooled connections may not be released.
 *
 * @see com.ibm.websphere.samples.daytrader.TradeServices
 *
 */

public class TradeDirect implements TradeServices {

    private static String dsName = TradeConfig.DATASOURCE;
    private static DataSource datasource = null;
    private static BigDecimal ZERO = new BigDecimal(0.0);
    private boolean inGlobalTxn = false;
    private boolean inSession = false;
   
    /**
     * Zero arg constructor for TradeDirect
     */
    public TradeDirect() {
        if (initialized == false) {
            init();
        }
    }

    public TradeDirect(boolean inSession) {
        if (initialized == false) {
            init();
        }

        this.inSession = inSession;
    }

    /**
     * @see TradeServices#getMarketSummary()
     */
    @Override
    public MarketSummaryDataBean getMarketSummary() throws Exception {

        MarketSummaryDataBean marketSummaryData = null;
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getMarketSummary - inSession(" + this.inSession + ")");
            }
            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getTSIAQuotesOrderByChangeSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ArrayList<QuoteDataBean> topGainersData = new ArrayList<QuoteDataBean>(5);
            ArrayList<QuoteDataBean> topLosersData = new ArrayList<QuoteDataBean>(5);

            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next() && (count++ < 5)) {
                QuoteDataBean quoteData = getQuoteDataFromResultSet(rs);
                topLosersData.add(quoteData);
            }

            stmt.close();
            stmt = getStatement(conn, "select * from quoteejb q order by q.change1 DESC", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery();

            count = 0;
            while (rs.next() && (count++ < 5)) {
                QuoteDataBean quoteData = getQuoteDataFromResultSet(rs);
                topGainersData.add(quoteData);
            }

            /*
             * rs.last(); count = 0; while (rs.previous() && (count++ < 5) ) {
             * QuoteDataBean quoteData = getQuoteDataFromResultSet(rs);
             * topGainersData.add(quoteData); }
             */

            stmt.close();

            BigDecimal TSIA = ZERO;
            BigDecimal openTSIA = ZERO;
            double volume = 0.0;

            if ((topGainersData.size() > 0) || (topLosersData.size() > 0)) {

                stmt = getStatement(conn, getTSIASQL);
                rs = stmt.executeQuery();

                if (!rs.next()) {
                    Log.error("TradeDirect:getMarketSummary -- error w/ getTSIASQL -- no results");
                } else {
                    TSIA = rs.getBigDecimal("TSIA");
                }
                stmt.close();

                stmt = getStatement(conn, getOpenTSIASQL);
                rs = stmt.executeQuery();

                if (!rs.next()) {
                    Log.error("TradeDirect:getMarketSummary -- error w/ getOpenTSIASQL -- no results");
                } else {
                    openTSIA = rs.getBigDecimal("openTSIA");
                }
                stmt.close();

                stmt = getStatement(conn, getTSIATotalVolumeSQL);
                rs = stmt.executeQuery();

                if (!rs.next()) {
                    Log.error("TradeDirect:getMarketSummary -- error w/ getTSIATotalVolumeSQL -- no results");
                } else {
                    volume = rs.getDouble("totalVolume");
                }
                stmt.close();
            }
            commit(conn);

            marketSummaryData = new MarketSummaryDataBean(TSIA, openTSIA, volume, topGainersData, topLosersData);

        }

        catch (Exception e) {
            Log.error("TradeDirect:login -- error logging in user", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return marketSummaryData;

    }

    /**
     * @see TradeServices#buy(String, String, double)
     */
    @Override
    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws Exception {

        final Connection conn = getConn();
        OrderDataBean orderData = null;
        UserTransaction txn = null;
         
        BigDecimal total;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:buy - inSession(" + this.inSession + ")", userID, symbol, new Double(quantity));
            }

            if (!inSession && orderProcessingMode == TradeConfig.ASYNCH_2PHASE) {
                if (Log.doTrace()) {
                    Log.trace("TradeDirect:buy create/begin global transaction");
                }
                // FUTURE the UserTransaction be looked up once
                txn = (javax.transaction.UserTransaction) context.lookup("java:comp/UserTransaction");
                txn.begin();
                setInGlobalTxn(true);
            }

           //conn = getConn();

            AccountDataBean accountData = getAccountData(conn, userID);
            QuoteDataBean quoteData = getQuoteData(conn, symbol);
            HoldingDataBean holdingData = null; // the buy operation will create
            // the holding

            orderData = createOrder(conn, accountData, quoteData, holdingData, "buy", quantity);

            // Update -- account should be credited during completeOrder
            BigDecimal price = quoteData.getPrice();
            BigDecimal orderFee = orderData.getOrderFee();
            total = (new BigDecimal(quantity).multiply(price)).add(orderFee);
            // subtract total from account balance
            creditAccountBalance(conn, accountData, total.negate());
            final Integer orderID = orderData.getOrderID();
            
            try {
                
                if (orderProcessingMode == TradeConfig.SYNCH) {
                    completeOrder(conn,orderID);
                } else  {
                    commit(conn);
                    queueOrder(orderID, true); // 2-phase
                }
            } catch (JMSException je) {
                Log.error("TradeBean:buy(" + userID + "," + symbol + "," + quantity + ") --> failed to queueOrder", je);
                /* On exception - cancel the order */

                cancelOrder(conn, orderData.getOrderID());
            }

            orderData = getOrderData(conn, orderData.getOrderID().intValue());

            if (txn != null) {
                if (Log.doTrace()) {
                    Log.trace("TradeDirect:buy committing global transaction");
                }
                txn.commit();
                setInGlobalTxn(false);
            } else {
                commit(conn);
            }
        } catch (Exception e) {
            Log.error("TradeDirect:buy error - rolling back", e);
            if (getInGlobalTxn()) {
                txn.rollback();
            } else {
                rollBack(conn, e);
            }
        } finally {
            releaseConn(conn);
        }

        return orderData;
    }

    /**
     * @see TradeServices#sell(String, Integer)
     */
    @Override
    public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws Exception {
        Connection conn = null;
        OrderDataBean orderData = null;
        UserTransaction txn = null;

        /*
         * total = (quantity * purchasePrice) + orderFee
         */
        BigDecimal total;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:sell - inSession(" + this.inSession + ")", userID, holdingID);
            }

            if (!inSession && orderProcessingMode == TradeConfig.ASYNCH_2PHASE) {
                if (Log.doTrace()) {
                    Log.trace("TradeDirect:sell create/begin global transaction");
                    // FUTURE the UserTransaction be looked up once
                }

                txn = (javax.transaction.UserTransaction) context.lookup("java:comp/UserTransaction");
                txn.begin();
                setInGlobalTxn(true);
            }

            conn = getConn();

            AccountDataBean accountData = getAccountData(conn, userID);
            HoldingDataBean holdingData = getHoldingData(conn, holdingID.intValue());
            QuoteDataBean quoteData = null;
            if (holdingData != null) {
                quoteData = getQuoteData(conn, holdingData.getQuoteID());
            }

            if ((accountData == null) || (holdingData == null) || (quoteData == null)) {
                String error = "TradeDirect:sell -- error selling stock -- unable to find:  \n\taccount=" + accountData + "\n\tholding=" + holdingData
                        + "\n\tquote=" + quoteData + "\nfor user: " + userID + " and holdingID: " + holdingID;
                Log.error(error);
                if (getInGlobalTxn()) {
                    txn.rollback();
                } else {
                    rollBack(conn, new Exception(error));
                }
                return orderData;
            }

            double quantity = holdingData.getQuantity();

            orderData = createOrder(conn, accountData, quoteData, holdingData, "sell", quantity);

            // Set the holdingSymbol purchaseDate to selling to signify the sell
            // is "inflight"
            updateHoldingStatus(conn, holdingData.getHoldingID(), holdingData.getQuoteID());

            // UPDATE -- account should be credited during completeOrder
            BigDecimal price = quoteData.getPrice();
            BigDecimal orderFee = orderData.getOrderFee();
            total = (new BigDecimal(quantity).multiply(price)).subtract(orderFee);
            creditAccountBalance(conn, accountData, total);
            
            try {
                if (orderProcessingMode == TradeConfig.SYNCH) {
                    completeOrder(conn, orderData.getOrderID());
                } else {
                    commit(conn);
                    queueOrder(orderData.getOrderID(), true);
                }
            } catch (JMSException je) {
                Log.error("TradeBean:sell(" + userID + "," + holdingID + ") --> failed to queueOrder", je);
                /* On exception - cancel the order */

                cancelOrder(conn, orderData.getOrderID());
            }

            orderData = getOrderData(conn, orderData.getOrderID().intValue());

            if (txn != null) {
                if (Log.doTrace()) {
                    Log.trace("TradeDirect:sell committing global transaction");
                }
                txn.commit();
                setInGlobalTxn(false);
            } else {
                commit(conn);
            }
        } catch (Exception e) {
            Log.error("TradeDirect:sell error", e);
            if (getInGlobalTxn()) {
                txn.rollback();
            } else {
                rollBack(conn, e);
            }
        } finally {
            releaseConn(conn);
        }

        return orderData;
    }

    /**
     * @see TradeServices#queueOrder(Integer)
     */
    @Override
    public void queueOrder(Integer orderID, boolean twoPhase) throws Exception {
        if (Log.doTrace()) {
            Log.trace("TradeDirect:queueOrder - inSession(" + this.inSession + ")", orderID);
        }
        
        if (TradeConfig.getOrderProcessingMode() == TradeConfig.ASYNCH_MANAGEDTHREAD) {
            
            try {
                //TODO: Do I need this lookup every time? 
                ManagedThreadFactory managedThreadFactory = (ManagedThreadFactory) context.lookup("java:comp/DefaultManagedThreadFactory");
                Thread thread = managedThreadFactory.newThread(new CompleteOrderThread(orderID, twoPhase));
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { 

            try (JMSContext context = qConnFactory.createContext();){	
                TextMessage message = context.createTextMessage();

                message.setStringProperty("command", "neworder");
                message.setIntProperty("orderID", orderID.intValue());
                message.setBooleanProperty("twoPhase", twoPhase);
                message.setBooleanProperty("direct", true);
                message.setLongProperty("publishTime", System.currentTimeMillis());
                message.setText("neworder: orderID=" + orderID + " runtimeMode=Direct twoPhase=" + twoPhase);
    		        		
                context.createProducer().send(brokerQueue, message);
            } catch (Exception e) {
                throw e; // pass the exception
            }
        }
    }

    /**
     * @see TradeServices#completeOrder(Integer)
     */
    @Override
    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception {
        OrderDataBean orderData = null;
        Connection conn = null;

        try { // twoPhase

            if (Log.doTrace()) {
                Log.trace("TradeDirect:completeOrder - inSession(" + this.inSession + ")", orderID);
            }
            setInGlobalTxn(!inSession && twoPhase);
            conn = getConn();
            orderData = completeOrder(conn, orderID);
           
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeDirect:completeOrder -- error completing order", e);
            rollBack(conn, e);
            cancelOrder(orderID, twoPhase);
        } finally {
            releaseConn(conn);
        }

        return orderData;

    }

    private OrderDataBean completeOrder(Connection conn, Integer orderID) throws Exception {
        //conn = getConn();
        OrderDataBean orderData = null;
        if (Log.doTrace()) {
            Log.trace("TradeDirect:completeOrderInternal - inSession(" + this.inSession + ")", orderID);
        }

        PreparedStatement stmt = getStatement(conn, getOrderSQL);
        stmt.setInt(1, orderID.intValue());

        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            Log.error("TradeDirect:completeOrder -- unable to find order: " + orderID);
            stmt.close();
            return orderData;
        }
        orderData = getOrderDataFromResultSet(rs);

        String orderType = orderData.getOrderType();
        String orderStatus = orderData.getOrderStatus();

        // if (order.isCompleted())
        if ((orderStatus.compareToIgnoreCase("completed") == 0) || (orderStatus.compareToIgnoreCase("alertcompleted") == 0)
                || (orderStatus.compareToIgnoreCase("cancelled") == 0)) {
            throw new Exception("TradeDirect:completeOrder -- attempt to complete Order that is already completed");
        }

        int accountID = rs.getInt("account_accountID");
        String quoteID = rs.getString("quote_symbol");
        int holdingID = rs.getInt("holding_holdingID");

        BigDecimal price = orderData.getPrice();
        double quantity = orderData.getQuantity();

        // get the data for the account and quote
        // the holding will be created for a buy or extracted for a sell

        /*
         * Use the AccountID and Quote Symbol from the Order AccountDataBean
         * accountData = getAccountData(accountID, conn); QuoteDataBean
         * quoteData = getQuoteData(conn, quoteID);
         */
        String userID = getAccountProfileData(conn, new Integer(accountID)).getUserID();

        HoldingDataBean holdingData = null;

        if (Log.doTrace()) {
            Log.trace("TradeDirect:completeOrder--> Completing Order " + orderData.getOrderID() + "\n\t Order info: " + orderData + "\n\t Account info: "
                    + accountID + "\n\t Quote info: " + quoteID);
        }

        // if (order.isBuy())
        if (orderType.compareToIgnoreCase("buy") == 0) {
            /*
             * Complete a Buy operation - create a new Holding for the Account -
             * deduct the Order cost from the Account balance
             */

            holdingData = createHolding(conn, accountID, quoteID, quantity, price);
            updateOrderHolding(conn, orderID.intValue(), holdingData.getHoldingID().intValue());
        }

        // if (order.isSell()) {
        if (orderType.compareToIgnoreCase("sell") == 0) {
            /*
             * Complete a Sell operation - remove the Holding from the Account -
             * deposit the Order proceeds to the Account balance
             */
            holdingData = getHoldingData(conn, holdingID);
            if (holdingData == null) {
                Log.debug("TradeDirect:completeOrder:sell -- user: " + userID + " already sold holding: " + holdingID);
            } else {
                removeHolding(conn, holdingID, orderID.intValue());
            }

        }

        updateOrderStatus(conn, orderData.getOrderID(), "closed");

        if (Log.doTrace()) {
            Log.trace("TradeDirect:completeOrder--> Completed Order " + orderData.getOrderID() + "\n\t Order info: " + orderData + "\n\t Account info: "
                    + accountID + "\n\t Quote info: " + quoteID + "\n\t Holding info: " + holdingData);
        }

        stmt.close();
        
        commit(conn);

        // commented out following call
        // - orderCompleted doesn't really do anything (think it was a hook for
        // old Trade caching code)

        // signify this order for user userID is complete
        // This call does not work here for Sync
        if (TradeConfig.orderProcessingMode != TradeConfig.SYNCH) {
            TradeAction tradeAction = new TradeAction(this);
            tradeAction.orderCompleted(userID, orderID);
        }

        return orderData;
    }

    /**
     * @see TradeServices#cancelOrder(Integer, boolean)
     */
    @Override
    public void cancelOrder(Integer orderID, boolean twoPhase) throws Exception {

        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:cancelOrder - inSession(" + this.inSession + ")", orderID);
            }
            setInGlobalTxn(!inSession && twoPhase);
            conn = getConn();
            cancelOrder(conn, orderID);
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeDirect:cancelOrder -- error cancelling order: " + orderID, e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
    }

    private void cancelOrder(Connection conn, Integer orderID) throws Exception {
        updateOrderStatus(conn, orderID, "cancelled");
    }

    @Override
    public void orderCompleted(String userID, Integer orderID) throws Exception {
        throw new UnsupportedOperationException("TradeDirect:orderCompleted method not supported");
    }

    private HoldingDataBean createHolding(Connection conn, int accountID, String symbol, double quantity, BigDecimal purchasePrice) throws Exception {

        Timestamp purchaseDate = new Timestamp(System.currentTimeMillis());
        PreparedStatement stmt = getStatement(conn, createHoldingSQL);

        Integer holdingID = KeySequenceDirect.getNextID(conn, "holding", inSession, getInGlobalTxn());
        stmt.setInt(1, holdingID.intValue());
        stmt.setTimestamp(2, purchaseDate);
        stmt.setBigDecimal(3, purchasePrice);
        stmt.setDouble(4, quantity);
        stmt.setString(5, symbol);
        stmt.setInt(6, accountID);
        stmt.executeUpdate();

        stmt.close();

        return getHoldingData(conn, holdingID.intValue());
    }

    private void removeHolding(Connection conn, int holdingID, int orderID) throws Exception {
        PreparedStatement stmt = getStatement(conn, removeHoldingSQL);

        stmt.setInt(1, holdingID);
        stmt.executeUpdate();
        stmt.close();

        // set the HoldingID to NULL for the purchase and sell order now that
        // the holding as been removed
        stmt = getStatement(conn, removeHoldingFromOrderSQL);

        stmt.setInt(1, holdingID);
        stmt.executeUpdate();
        stmt.close();

    }

    private OrderDataBean createOrder(Connection conn, AccountDataBean accountData, QuoteDataBean quoteData, HoldingDataBean holdingData, String orderType,
            double quantity) throws Exception {

        Timestamp currentDate = new Timestamp(System.currentTimeMillis());

        PreparedStatement stmt = getStatement(conn, createOrderSQL);

        Integer orderID = KeySequenceDirect.getNextID(conn, "order", inSession, getInGlobalTxn());
        stmt.setInt(1, orderID.intValue());
        stmt.setString(2, orderType);
        stmt.setString(3, "open");
        stmt.setTimestamp(4, currentDate);
        stmt.setDouble(5, quantity);
        stmt.setBigDecimal(6, quoteData.getPrice().setScale(FinancialUtils.SCALE, FinancialUtils.ROUND));
        stmt.setBigDecimal(7, TradeConfig.getOrderFee(orderType));
        stmt.setInt(8, accountData.getAccountID().intValue());
        if (holdingData == null) {
            stmt.setNull(9, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(9, holdingData.getHoldingID().intValue());
        }
        stmt.setString(10, quoteData.getSymbol());
        stmt.executeUpdate();

        stmt.close();

        return getOrderData(conn, orderID.intValue());
    }

    /**
     * @see TradeServices#getOrders(String)
     */
    @Override
    public Collection<OrderDataBean> getOrders(String userID) throws Exception {
        Collection<OrderDataBean> orderDataBeans = new ArrayList<OrderDataBean>();
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getOrders - inSession(" + this.inSession + ")", userID);
            }

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getOrdersByUserSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();

            // TODO: return top 5 orders for now -- next version will add a
            // getAllOrders method
            // also need to get orders sorted by order id descending
            int i = 0;
            while ((rs.next()) && (i++ < 5)) {
                OrderDataBean orderData = getOrderDataFromResultSet(rs);
                orderDataBeans.add(orderData);
            }

            stmt.close();
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeDirect:getOrders -- error getting user orders", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return orderDataBeans;
    }

    /**
     * @see TradeServices#getClosedOrders(String)
     */
    @Override
    public Collection<OrderDataBean> getClosedOrders(String userID) throws Exception {
        Collection<OrderDataBean> orderDataBeans = new ArrayList<OrderDataBean>();
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getClosedOrders - inSession(" + this.inSession + ")", userID);
            }

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getClosedOrdersSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OrderDataBean orderData = getOrderDataFromResultSet(rs);
                orderData.setOrderStatus("completed");
                updateOrderStatus(conn, orderData.getOrderID(), orderData.getOrderStatus());
                orderDataBeans.add(orderData);

            }

            stmt.close();
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getOrders -- error getting user orders", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return orderDataBeans;
    }

    /**
     * @see TradeServices#createQuote(String, String, BigDecimal)
     */
    @Override
    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception {

        QuoteDataBean quoteData = null;
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.traceEnter("TradeDirect:createQuote - inSession(" + this.inSession + ")");
            }

            price = price.setScale(FinancialUtils.SCALE, FinancialUtils.ROUND);
            double volume = 0.0, change = 0.0;

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, createQuoteSQL);
            stmt.setString(1, symbol); // symbol
            stmt.setString(2, companyName); // companyName
            stmt.setDouble(3, volume); // volume
            stmt.setBigDecimal(4, price); // price
            stmt.setBigDecimal(5, price); // open
            stmt.setBigDecimal(6, price); // low
            stmt.setBigDecimal(7, price); // high
            stmt.setDouble(8, change); // change

            stmt.executeUpdate();
            stmt.close();
            commit(conn);

            quoteData = new QuoteDataBean(symbol, companyName, volume, price, price, price, price, change);
            if (Log.doTrace()) {
                Log.traceExit("TradeDirect:createQuote");
            }
        } catch (Exception e) {
            Log.error("TradeDirect:createQuote -- error creating quote", e);
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    /**
     * @see TradeServices#getQuote(String)
     */

    @Override
    public QuoteDataBean getQuote(String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        Connection conn = null;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getQuote - inSession(" + this.inSession + ")", symbol);
            }

            conn = getConn();
            quoteData = getQuote(conn, symbol);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getQuote -- error getting quote", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    private QuoteDataBean getQuote(Connection conn, String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        PreparedStatement stmt = getStatement(conn, getQuoteSQL);
        stmt.setString(1, symbol); // symbol

        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            Log.error("TradeDirect:getQuote -- failure no result.next() for symbol: " + symbol);
        } else {
            quoteData = getQuoteDataFromResultSet(rs);
        }

        stmt.close();

        return quoteData;
    }

    private QuoteDataBean getQuoteForUpdate(Connection conn, String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        PreparedStatement stmt = getStatement(conn, getQuoteForUpdateSQL);
        stmt.setString(1, symbol); // symbol

        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            Log.error("TradeDirect:getQuote -- failure no result.next()");
        } else {
            quoteData = getQuoteDataFromResultSet(rs);
        }

        stmt.close();

        return quoteData;
    }

    /**
     * @see TradeServices#getAllQuotes(String)
     */
    @Override
    public Collection<QuoteDataBean> getAllQuotes() throws Exception {
        Collection<QuoteDataBean> quotes = new ArrayList<QuoteDataBean>();
        QuoteDataBean quoteData = null;

        Connection conn = null;
        try {
            conn = getConn();

            PreparedStatement stmt = getStatement(conn, getAllQuotesSQL);

            ResultSet rs = stmt.executeQuery();

            while (!rs.next()) {
                quoteData = getQuoteDataFromResultSet(rs);
                quotes.add(quoteData);
            }

            stmt.close();
        } catch (Exception e) {
            Log.error("TradeDirect:getAllQuotes", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }

        return quotes;
    }

    /**
     * @see TradeServices#getHoldings(String)
     */
    @Override
    public Collection<HoldingDataBean> getHoldings(String userID) throws Exception {
        Collection<HoldingDataBean> holdingDataBeans = new ArrayList<HoldingDataBean>();
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getHoldings - inSession(" + this.inSession + ")", userID);
            }

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getHoldingsForUserSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                HoldingDataBean holdingData = getHoldingDataFromResultSet(rs);
                holdingDataBeans.add(holdingData);
            }

            stmt.close();
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeDirect:getHoldings -- error getting user holings", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return holdingDataBeans;
    }

    /**
     * @see TradeServices#getHolding(Integer)
     */
    @Override
    public HoldingDataBean getHolding(Integer holdingID) throws Exception {
        HoldingDataBean holdingData = null;
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getHolding - inSession(" + this.inSession + ")", holdingID);
            }

            conn = getConn();
            holdingData = getHoldingData(holdingID.intValue());

            commit(conn);

        } catch (Exception e) {
            Log.error("TradeDirect:getHolding -- error getting holding " + holdingID + "", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return holdingData;
    }

    /**
     * @see TradeServices#getAccountData(String)
     */
    @Override
    public AccountDataBean getAccountData(String userID) throws Exception {
        try {
            AccountDataBean accountData = null;
            Connection conn = null;
            try {
                if (Log.doTrace()) {
                    Log.trace("TradeDirect:getAccountData - inSession(" + this.inSession + ")", userID);
                }

                conn = getConn();
                accountData = getAccountData(conn, userID);
                commit(conn);

            } catch (Exception e) {
                Log.error("TradeDirect:getAccountData -- error getting account data", e);
                rollBack(conn, e);
            } finally {
                releaseConn(conn);
            }
            return accountData;
        } catch (Exception e) {
            throw new Exception(e.getMessage(), e);
        }
    }

    private AccountDataBean getAccountData(Connection conn, String userID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountForUserSQL);
        stmt.setString(1, userID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }

    /*private AccountDataBean getAccountDataForUpdate(Connection conn,
            String userID) throws Exception {
        PreparedStatement stmt = getStatement(conn,
                getAccountForUserForUpdateSQL);
        stmt.setString(1, userID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }*/

    /**
     * @see TradeServices#getAccountData(String)
     */
    public AccountDataBean getAccountData(int accountID) throws Exception {
        AccountDataBean accountData = null;
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getAccountData - inSession(" + this.inSession + ")", new Integer(accountID));
            }

            conn = getConn();
            accountData = getAccountData(accountID, conn);
            commit(conn);

        } catch (Exception e) {
            Log.error("TradeDirect:getAccountData -- error getting account data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountData;
    }

    private AccountDataBean getAccountData(int accountID, Connection conn) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountSQL);
        stmt.setInt(1, accountID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }

    /*
    private AccountDataBean getAccountDataForUpdate(int accountID,
            Connection conn) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountForUpdateSQL);
        stmt.setInt(1, accountID);
        ResultSet rs = stmt.executeQuery();
        AccountDataBean accountData = getAccountDataFromResultSet(rs);
        stmt.close();
        return accountData;
    }
     */

    /*
    private QuoteDataBean getQuoteData(String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        Connection conn = null;
        try {
            conn = getConn();
            quoteData = getQuoteData(conn, symbol);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getQuoteData -- error getting data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }
     */

    private QuoteDataBean getQuoteData(Connection conn, String symbol) throws Exception {
        QuoteDataBean quoteData = null;
        PreparedStatement stmt = getStatement(conn, getQuoteSQL);
        stmt.setString(1, symbol);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) {
            Log.error("TradeDirect:getQuoteData -- could not find quote for symbol=" + symbol);
        } else {
            quoteData = getQuoteDataFromResultSet(rs);
        }
        stmt.close();
        return quoteData;
    }

    private HoldingDataBean getHoldingData(int holdingID) throws Exception {
        HoldingDataBean holdingData = null;
        Connection conn = null;
        try {
            conn = getConn();
            holdingData = getHoldingData(conn, holdingID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getHoldingData -- error getting data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return holdingData;
    }

    private HoldingDataBean getHoldingData(Connection conn, int holdingID) throws Exception {
        HoldingDataBean holdingData = null;
        PreparedStatement stmt = getStatement(conn, getHoldingSQL);
        stmt.setInt(1, holdingID);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) {
            Log.error("TradeDirect:getHoldingData -- no results -- holdingID=" + holdingID);
        } else {
            holdingData = getHoldingDataFromResultSet(rs);
        }

        stmt.close();
        return holdingData;
    }

    /*
    private OrderDataBean getOrderData(int orderID) throws Exception {
        OrderDataBean orderData = null;
        Connection conn = null;
        try {
            conn = getConn();
            orderData = getOrderData(conn, orderID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getOrderData -- error getting data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return orderData;
    }
     */

    private OrderDataBean getOrderData(Connection conn, int orderID) throws Exception {
        OrderDataBean orderData = null;
        if (Log.doTrace()) {
            Log.trace("TradeDirect:getOrderData(conn, " + orderID + ")");
        }
        PreparedStatement stmt = getStatement(conn, getOrderSQL);
        stmt.setInt(1, orderID);
        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) {
            Log.error("TradeDirect:getOrderData -- no results for orderID:" + orderID);
        } else {
            orderData = getOrderDataFromResultSet(rs);
        }
        stmt.close();
        return orderData;
    }

    /**
     * @see TradeServices#getAccountProfileData(String)
     */
    @Override
    public AccountProfileDataBean getAccountProfileData(String userID) throws Exception {
        AccountProfileDataBean accountProfileData = null;
        Connection conn = null;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getAccountProfileData - inSession(" + this.inSession + ")", userID);
            }

            conn = getConn();
            accountProfileData = getAccountProfileData(conn, userID);
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getAccountProfileData -- error getting profile data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountProfileData;
    }

    private AccountProfileDataBean getAccountProfileData(Connection conn, String userID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountProfileSQL);
        stmt.setString(1, userID);

        ResultSet rs = stmt.executeQuery();

        AccountProfileDataBean accountProfileData = getAccountProfileDataFromResultSet(rs);
        stmt.close();
        return accountProfileData;
    }

    /*
    private AccountProfileDataBean getAccountProfileData(Integer accountID)
    throws Exception {
        AccountProfileDataBean accountProfileData = null;
        Connection conn = null;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:getAccountProfileData", accountID);
            }

            conn = getConn();
            accountProfileData = getAccountProfileData(conn, accountID);
            commit(conn);
        } catch (Exception e) {
            Log.error(
                    "TradeDirect:getAccountProfileData -- error getting profile data",
                    e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountProfileData;
    }
     */

    private AccountProfileDataBean getAccountProfileData(Connection conn, Integer accountID) throws Exception {
        PreparedStatement stmt = getStatement(conn, getAccountProfileForAccountSQL);
        stmt.setInt(1, accountID.intValue());

        ResultSet rs = stmt.executeQuery();

        AccountProfileDataBean accountProfileData = getAccountProfileDataFromResultSet(rs);
        stmt.close();
        return accountProfileData;
    }

    /**
     * @see TradeServices#updateAccountProfile(AccountProfileDataBean)
     */
    @Override
    public AccountProfileDataBean updateAccountProfile(AccountProfileDataBean profileData) throws Exception {
        AccountProfileDataBean accountProfileData = null;
        Connection conn = null;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:updateAccountProfileData - inSession(" + this.inSession + ")", profileData.getUserID());
            }

            conn = getConn();
            updateAccountProfile(conn, profileData);

            accountProfileData = getAccountProfileData(conn, profileData.getUserID());
            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:getAccountProfileData -- error getting profile data", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountProfileData;
    }

    private void creditAccountBalance(Connection conn, AccountDataBean accountData, BigDecimal credit) throws Exception {
        PreparedStatement stmt = getStatement(conn, creditAccountBalanceSQL);

        stmt.setBigDecimal(1, credit);
        stmt.setInt(2, accountData.getAccountID().intValue());

        stmt.executeUpdate();
        stmt.close();

    }

    // Set Timestamp to zero to denote sell is inflight
    // UPDATE -- could add a "status" attribute to holding
    private void updateHoldingStatus(Connection conn, Integer holdingID, String symbol) throws Exception {
        Timestamp ts = new Timestamp(0);
        PreparedStatement stmt = getStatement(conn, "update holdingejb set purchasedate= ? where holdingid = ?");

        stmt.setTimestamp(1, ts);
        stmt.setInt(2, holdingID.intValue());
        stmt.executeUpdate();
        stmt.close();
    }

    private void updateOrderStatus(Connection conn, Integer orderID, String status) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateOrderStatusSQL);

        stmt.setString(1, status);
        stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        stmt.setInt(3, orderID.intValue());
        stmt.executeUpdate();
        stmt.close();
    }

    private void updateOrderHolding(Connection conn, int orderID, int holdingID) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateOrderHoldingSQL);

        stmt.setInt(1, holdingID);
        stmt.setInt(2, orderID);
        stmt.executeUpdate();
        stmt.close();
    }

    private void updateAccountProfile(Connection conn, AccountProfileDataBean profileData) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateAccountProfileSQL);

        stmt.setString(1, profileData.getPassword());
        stmt.setString(2, profileData.getFullName());
        stmt.setString(3, profileData.getAddress());
        stmt.setString(4, profileData.getEmail());
        stmt.setString(5, profileData.getCreditCard());
        stmt.setString(6, profileData.getUserID());

        stmt.executeUpdate();
        stmt.close();
    }

    /*
    private void updateQuoteVolume(Connection conn, QuoteDataBean quoteData,
            double quantity) throws Exception {
        PreparedStatement stmt = getStatement(conn, updateQuoteVolumeSQL);

        stmt.setDouble(1, quantity);
        stmt.setString(2, quoteData.getSymbol());

        stmt.executeUpdate();
        stmt.close();
    }
     */

    @Override
    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded) throws Exception {
        return updateQuotePriceVolumeInt(symbol, changeFactor, sharesTraded, TradeConfig.getPublishQuotePriceChange());
    }

    /**
     * Update a quote's price and volume
     *
     * @param symbol
     *            The PK of the quote
     * @param changeFactor
     *            the percent to change the old price by (between 50% and 150%)
     * @param sharedTraded
     *            the ammount to add to the current volume
     * @param publishQuotePriceChange
     *            used by the PingJDBCWrite Primitive to ensure no JMS is used,
     *            should be true for all normal calls to this API
     */
    public QuoteDataBean updateQuotePriceVolumeInt(String symbol, BigDecimal changeFactor, double sharesTraded, boolean publishQuotePriceChange)
            throws Exception {

        if (TradeConfig.getUpdateQuotePrices() == false) {
            return new QuoteDataBean();
        }

        QuoteDataBean quoteData = null;
        Connection conn = null;

        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:updateQuotePriceVolume - inSession(" + this.inSession + ")", symbol, changeFactor, new Double(sharesTraded));
            }

            conn = getConn();

            quoteData = getQuoteForUpdate(conn, symbol);
            BigDecimal oldPrice = quoteData.getPrice();
            BigDecimal openPrice = quoteData.getOpen();

            double newVolume = quoteData.getVolume() + sharesTraded;

            if (oldPrice.equals(TradeConfig.PENNY_STOCK_PRICE)) {
                changeFactor = TradeConfig.PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
            } else if (oldPrice.compareTo(TradeConfig.MAXIMUM_STOCK_PRICE) > 0) {
                changeFactor = TradeConfig.MAXIMUM_STOCK_SPLIT_MULTIPLIER;
            }

            BigDecimal newPrice = changeFactor.multiply(oldPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            double change = newPrice.subtract(openPrice).doubleValue();

            updateQuotePriceVolume(conn, quoteData.getSymbol(), newPrice, newVolume, change);
            quoteData = getQuote(conn, symbol);

            commit(conn);

            if (publishQuotePriceChange) {
                publishQuotePriceChange(quoteData, oldPrice, changeFactor, sharesTraded);
            }

        } catch (Exception e) {
            Log.error("TradeDirect:updateQuotePriceVolume -- error updating quote price/volume for symbol:" + symbol);
            rollBack(conn, e);
            throw e;
        } finally {
            releaseConn(conn);
        }
        return quoteData;
    }

    private void updateQuotePriceVolume(Connection conn, String symbol, BigDecimal newPrice, double newVolume, double change) throws Exception {

        PreparedStatement stmt = getStatement(conn, updateQuotePriceVolumeSQL);

        stmt.setBigDecimal(1, newPrice);
        stmt.setDouble(2, change);
        stmt.setDouble(3, newVolume);
        stmt.setString(4, symbol);

        stmt.executeUpdate();
        stmt.close();
    }

    private void publishQuotePriceChange(QuoteDataBean quoteData, BigDecimal oldPrice, BigDecimal changeFactor, double sharesTraded) throws Exception {
        if (Log.doTrace()) {
            Log.trace("TradeDirect:publishQuotePrice PUBLISHING to MDB quoteData = " + quoteData);
        }
        
        try (JMSContext context = tConnFactory.createContext();){
    		TextMessage message = context.createTextMessage();

    		message.setStringProperty("command", "updateQuote");
            message.setStringProperty("symbol", quoteData.getSymbol());
            message.setStringProperty("company", quoteData.getCompanyName());
            message.setStringProperty("price", quoteData.getPrice().toString());
            message.setStringProperty("oldPrice", oldPrice.toString());
            message.setStringProperty("open", quoteData.getOpen().toString());
            message.setStringProperty("low", quoteData.getLow().toString());
            message.setStringProperty("high", quoteData.getHigh().toString());
            message.setDoubleProperty("volume", quoteData.getVolume());

            message.setStringProperty("changeFactor", changeFactor.toString());
            message.setDoubleProperty("sharesTraded", sharesTraded);
            message.setLongProperty("publishTime", System.currentTimeMillis());
            message.setText("Update Stock price for " + quoteData.getSymbol() + " old price = " + oldPrice + " new price = " + quoteData.getPrice());

      		
    		context.createProducer().send(streamerTopic, message);

        } catch (Exception e) {
            throw e; // pass exception back

        }
    }

    /**
     * @see TradeServices#login(String, String)
     */

    @Override
    public AccountDataBean login(String userID, String password) throws Exception {

        AccountDataBean accountData = null;
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect:login - inSession(" + this.inSession + ")", userID, password);
            }

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, getAccountProfileSQL);
            stmt.setString(1, userID);

            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                Log.error("TradeDirect:login -- failure to find account for" + userID);
                throw new javax.ejb.FinderException("Cannot find account for" + userID);
            }

            String pw = rs.getString("passwd");
            stmt.close();
            if ((pw == null) || (pw.equals(password) == false)) {
                String error = "TradeDirect:Login failure for user: " + userID + "\n\tIncorrect password-->" + userID + ":" + password;
                Log.error(error);
                throw new Exception(error);
            }

            stmt = getStatement(conn, loginSQL);
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, userID);

            stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn, getAccountForUserSQL);
            stmt.setString(1, userID);
            rs = stmt.executeQuery();

            accountData = getAccountDataFromResultSet(rs);

            stmt.close();

            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:login -- error logging in user", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
        return accountData;

        /*
         * setLastLogin( new Timestamp(System.currentTimeMillis()) );
         * setLoginCount( getLoginCount() + 1 );
         */
    }

    /**
     * @see TradeServices#logout(String)
     */
    @Override
    public void logout(String userID) throws Exception {
        if (Log.doTrace()) {
            Log.trace("TradeDirect:logout - inSession(" + this.inSession + ")", userID);
        }
        Connection conn = null;
        try {
            conn = getConn();
            PreparedStatement stmt = getStatement(conn, logoutSQL);
            stmt.setString(1, userID);
            stmt.executeUpdate();
            stmt.close();

            commit(conn);
        } catch (Exception e) {
            Log.error("TradeDirect:logout -- error logging out user", e);
            rollBack(conn, e);
        } finally {
            releaseConn(conn);
        }
    }

    /**
     * @see TradeServices#register(String, String, String, String, String,
     *      String, BigDecimal, boolean)
     */

    @Override
    public AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditcard, BigDecimal openBalance)
            throws Exception {

        AccountDataBean accountData = null;
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.traceEnter("TradeDirect:register - inSession(" + this.inSession + ")");
            }

            conn = getConn();
            PreparedStatement stmt = getStatement(conn, createAccountSQL);

            Integer accountID = KeySequenceDirect.getNextID(conn, "account", inSession, getInGlobalTxn());
            BigDecimal balance = openBalance;
            Timestamp creationDate = new Timestamp(System.currentTimeMillis());
            Timestamp lastLogin = creationDate;
            int loginCount = 0;
            int logoutCount = 0;

            stmt.setInt(1, accountID.intValue());
            stmt.setTimestamp(2, creationDate);
            stmt.setBigDecimal(3, openBalance);
            stmt.setBigDecimal(4, balance);
            stmt.setTimestamp(5, lastLogin);
            stmt.setInt(6, loginCount);
            stmt.setInt(7, logoutCount);
            stmt.setString(8, userID);
            stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn, createAccountProfileSQL);
            stmt.setString(1, userID);
            stmt.setString(2, password);
            stmt.setString(3, fullname);
            stmt.setString(4, address);
            stmt.setString(5, email);
            stmt.setString(6, creditcard);
            stmt.executeUpdate();
            stmt.close();

            commit(conn);

            accountData = new AccountDataBean(accountID, loginCount, logoutCount, lastLogin, creationDate, balance, openBalance, userID);
            if (Log.doTrace()) {
                Log.traceExit("TradeDirect:register");
            }
        } catch (Exception e) {
            Log.error("TradeDirect:register -- error registering new user", e);
        } finally {
            releaseConn(conn);
        }
        return accountData;
    }

    private AccountDataBean getAccountDataFromResultSet(ResultSet rs) throws Exception {
        AccountDataBean accountData = null;

        if (!rs.next()) {
            Log.error("TradeDirect:getAccountDataFromResultSet -- cannot find account data");
        } else {
            accountData = new AccountDataBean(new Integer(rs.getInt("accountID")), rs.getInt("loginCount"), rs.getInt("logoutCount"),
                    rs.getTimestamp("lastLogin"), rs.getTimestamp("creationDate"), rs.getBigDecimal("balance"), rs.getBigDecimal("openBalance"),
                    rs.getString("profile_userID"));
        }
        return accountData;
    }

    private AccountProfileDataBean getAccountProfileDataFromResultSet(ResultSet rs) throws Exception {
        AccountProfileDataBean accountProfileData = null;

        if (!rs.next()) {
            Log.error("TradeDirect:getAccountProfileDataFromResultSet -- cannot find accountprofile data");
        } else {
            accountProfileData = new AccountProfileDataBean(rs.getString("userID"), rs.getString("passwd"), rs.getString("fullName"), rs.getString("address"),
                    rs.getString("email"), rs.getString("creditCard"));
        }

        return accountProfileData;
    }

    private HoldingDataBean getHoldingDataFromResultSet(ResultSet rs) throws Exception {
        HoldingDataBean holdingData = null;

        holdingData = new HoldingDataBean(new Integer(rs.getInt("holdingID")), rs.getDouble("quantity"), rs.getBigDecimal("purchasePrice"),
                rs.getTimestamp("purchaseDate"), rs.getString("quote_symbol"));
        return holdingData;
    }

    private QuoteDataBean getQuoteDataFromResultSet(ResultSet rs) throws Exception {
        QuoteDataBean quoteData = null;

        quoteData = new QuoteDataBean(rs.getString("symbol"), rs.getString("companyName"), rs.getDouble("volume"), rs.getBigDecimal("price"),
                rs.getBigDecimal("open1"), rs.getBigDecimal("low"), rs.getBigDecimal("high"), rs.getDouble("change1"));
        return quoteData;
    }

    private OrderDataBean getOrderDataFromResultSet(ResultSet rs) throws Exception {
        OrderDataBean orderData = null;

        orderData = new OrderDataBean(new Integer(rs.getInt("orderID")), rs.getString("orderType"), rs.getString("orderStatus"), rs.getTimestamp("openDate"),
                rs.getTimestamp("completionDate"), rs.getDouble("quantity"), rs.getBigDecimal("price"), rs.getBigDecimal("orderFee"),
                rs.getString("quote_symbol"));
        return orderData;
    }

    public String checkDBProductName() throws Exception {
        Connection conn = null;
        String dbProductName = null;

        try {
            if (Log.doTrace()) {
                Log.traceEnter("TradeDirect:checkDBProductName");
            }

            conn = getConn();
            DatabaseMetaData dbmd = conn.getMetaData();
            dbProductName = dbmd.getDatabaseProductName();
        } catch (SQLException e) {
            Log.error(e, "TradeDirect:checkDBProductName() -- Error checking the Daytrader Database Product Name");
        } finally {
            releaseConn(conn);
        }
        return dbProductName;
    }

    public boolean recreateDBTables(Object[] sqlBuffer, java.io.PrintWriter out) throws Exception {
        // Clear MDB Statistics
        MDBStats.getInstance().reset();

        Connection conn = null;
        boolean success = false;
        try {
            if (Log.doTrace()) {
                Log.traceEnter("TradeDirect:recreateDBTables");
            }

            conn = getConn();
            Statement stmt = conn.createStatement();
            int bufferLength = sqlBuffer.length;
            for (int i = 0; i < bufferLength; i++) {
                try {
                    stmt.executeUpdate((String) sqlBuffer[i]);
                    // commit(conn);
                } catch (SQLException ex) {
                    // Ignore DROP statements as tables won't always exist.
                    if (((String) sqlBuffer[i]).indexOf("DROP ") < 0) {
                        Log.error("TradeDirect:recreateDBTables SQL Exception thrown on executing the foll sql command: " + sqlBuffer[i], ex);
                        out.println("<BR>SQL Exception thrown on executing the foll sql command: <I>" + sqlBuffer[i] + "</I> . Check log for details.</BR>");
                    }
                }
            }
            stmt.close();
            commit(conn);
            success = true;
        } catch (Exception e) {
            Log.error(e, "TradeDirect:recreateDBTables() -- Error dropping and recreating the database tables");
        } finally {
            releaseConn(conn);
        }
        return success;
    }

    @Override
    public RunStatsDataBean resetTrade(boolean deleteAll) throws Exception {
        // Clear MDB Statistics
        MDBStats.getInstance().reset();
        // Reset Trade

        RunStatsDataBean runStatsData = new RunStatsDataBean();
        Connection conn = null;
        try {
            if (Log.doTrace()) {
                Log.traceEnter("TradeDirect:resetTrade deleteAll rows=" + deleteAll);
            }

            conn = getConn();
            PreparedStatement stmt = null;
            ResultSet rs = null;

            if (deleteAll) {
                try {
                    stmt = getStatement(conn, "delete from quoteejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from accountejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from accountprofileejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from holdingejb");
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = getStatement(conn, "delete from orderejb");
                    stmt.executeUpdate();
                    stmt.close();
                    // FUTURE: - DuplicateKeyException - For now, don't start at
                    // zero as KeySequenceDirect and KeySequenceBean will still
                    // give out
                    // the cached Block and then notice this change. Better
                    // solution is
                    // to signal both classes to drop their cached blocks
                    // stmt = getStatement(conn, "delete from keygenejb");
                    // stmt.executeUpdate();
                    // stmt.close();
                    commit(conn);
                } catch (Exception e) {
                    Log.error(e, "TradeDirect:resetTrade(deleteAll) -- Error deleting Trade users and stock from the Trade database");
                }
                return runStatsData;
            }

            stmt = getStatement(conn, "delete from holdingejb where holdingejb.account_accountid is null");
            stmt.executeUpdate();
            stmt.close();

            // Count and Delete newly registered users (users w/ id that start
            // "ru:%":
            stmt = getStatement(conn, "delete from accountprofileejb where userid like 'ru:%'");
            stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn, "delete from orderejb where account_accountid in (select accountid from accountejb a where a.profile_userid like 'ru:%')");
            stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn,
                    "delete from holdingejb where account_accountid in (select accountid from accountejb a where a.profile_userid like 'ru:%')");
            stmt.executeUpdate();
            stmt.close();

            stmt = getStatement(conn, "delete from accountejb where profile_userid like 'ru:%'");
            int newUserCount = stmt.executeUpdate();
            runStatsData.setNewUserCount(newUserCount);
            stmt.close();

            // Count of trade users
            stmt = getStatement(conn, "select count(accountid) as \"tradeUserCount\" from accountejb a where a.profile_userid like 'uid:%'");
            rs = stmt.executeQuery();
            rs.next();
            int tradeUserCount = rs.getInt("tradeUserCount");
            runStatsData.setTradeUserCount(tradeUserCount);
            stmt.close();

            rs.close();
            // Count of trade stocks
            stmt = getStatement(conn, "select count(symbol) as \"tradeStockCount\" from quoteejb a where a.symbol like 's:%'");
            rs = stmt.executeQuery();
            rs.next();
            int tradeStockCount = rs.getInt("tradeStockCount");
            runStatsData.setTradeStockCount(tradeStockCount);
            stmt.close();

            // Count of trade users login, logout
            stmt = getStatement(conn,
                    "select sum(loginCount) as \"sumLoginCount\", sum(logoutCount) as \"sumLogoutCount\" from accountejb a where  a.profile_userID like 'uid:%'");
            rs = stmt.executeQuery();
            rs.next();
            int sumLoginCount = rs.getInt("sumLoginCount");
            int sumLogoutCount = rs.getInt("sumLogoutCount");
            runStatsData.setSumLoginCount(sumLoginCount);
            runStatsData.setSumLogoutCount(sumLogoutCount);
            stmt.close();

            rs.close();
            // Update logoutcount and loginCount back to zero

            stmt = getStatement(conn, "update accountejb set logoutCount=0,loginCount=0 where profile_userID like 'uid:%'");
            stmt.executeUpdate();
            stmt.close();

            // count holdings for trade users
            stmt = getStatement(conn, "select count(holdingid) as \"holdingCount\" from holdingejb h where h.account_accountid in "
                    + "(select accountid from accountejb a where a.profile_userid like 'uid:%')");

            rs = stmt.executeQuery();
            rs.next();
            int holdingCount = rs.getInt("holdingCount");
            runStatsData.setHoldingCount(holdingCount);
            stmt.close();
            rs.close();

            // count orders for trade users
            stmt = getStatement(conn, "select count(orderid) as \"orderCount\" from orderejb o where o.account_accountid in "
                    + "(select accountid from accountejb a where a.profile_userid like 'uid:%')");

            rs = stmt.executeQuery();
            rs.next();
            int orderCount = rs.getInt("orderCount");
            runStatsData.setOrderCount(orderCount);
            stmt.close();
            rs.close();

            // count orders by type for trade users
            stmt = getStatement(conn, "select count(orderid) \"buyOrderCount\"from orderejb o where (o.account_accountid in "
                    + "(select accountid from accountejb a where a.profile_userid like 'uid:%')) AND " + " (o.orderType='buy')");

            rs = stmt.executeQuery();
            rs.next();
            int buyOrderCount = rs.getInt("buyOrderCount");
            runStatsData.setBuyOrderCount(buyOrderCount);
            stmt.close();
            rs.close();

            // count orders by type for trade users
            stmt = getStatement(conn, "select count(orderid) \"sellOrderCount\"from orderejb o where (o.account_accountid in "
                    + "(select accountid from accountejb a where a.profile_userid like 'uid:%')) AND " + " (o.orderType='sell')");

            rs = stmt.executeQuery();
            rs.next();
            int sellOrderCount = rs.getInt("sellOrderCount");
            runStatsData.setSellOrderCount(sellOrderCount);
            stmt.close();
            rs.close();

            // Delete cancelled orders
            stmt = getStatement(conn, "delete from orderejb where orderStatus='cancelled'");
            int cancelledOrderCount = stmt.executeUpdate();
            runStatsData.setCancelledOrderCount(cancelledOrderCount);
            stmt.close();
            rs.close();

            // count open orders by type for trade users
            stmt = getStatement(conn, "select count(orderid) \"openOrderCount\"from orderejb o where (o.account_accountid in "
                    + "(select accountid from accountejb a where a.profile_userid like 'uid:%')) AND " + " (o.orderStatus='open')");

            rs = stmt.executeQuery();
            rs.next();
            int openOrderCount = rs.getInt("openOrderCount");
            runStatsData.setOpenOrderCount(openOrderCount);

            stmt.close();
            rs.close();
            // Delete orders for holding which have been purchased and sold
            stmt = getStatement(conn, "delete from orderejb where holding_holdingid is null");
            int deletedOrderCount = stmt.executeUpdate();
            runStatsData.setDeletedOrderCount(deletedOrderCount);
            stmt.close();
            rs.close();

            commit(conn);

            System.out.println("TradeDirect:reset Run stats data\n\n" + runStatsData);
        } catch (Exception e) {
            Log.error(e, "Failed to reset Trade");
            rollBack(conn, e);
            throw e;
        } finally {
            releaseConn(conn);
        }
        return runStatsData;

    }

    private void releaseConn(Connection conn) throws Exception {
        try {
            if (conn != null) {
                conn.close();
                if (Log.doTrace()) {
                    synchronized (lock) {
                        connCount--;
                    }
                    Log.trace("TradeDirect:releaseConn -- connection closed, connCount=" + connCount);
                }
            }
        } catch (Exception e) {
            Log.error("TradeDirect:releaseConnection -- failed to close connection", e);
        }
    }

    /*
     * Lookup the TradeData datasource
     */
    private void getDataSource() throws Exception {
        datasource = (DataSource) context.lookup(dsName);
    }

    /*
     * Allocate a new connection to the datasource
     */
    private static int connCount = 0;

    private static Integer lock = new Integer(0);

    private Connection getConn() throws Exception {

        Connection conn = null;
        if (datasource == null) {
            getDataSource();
        }
        conn = datasource.getConnection();      
        
        if (!this.inGlobalTxn) {
        	conn.setAutoCommit(false);
        }
        if (Log.doTrace()) {
            synchronized (lock) {
                connCount++;
            }
            Log.trace("TradeDirect:getConn -- new connection allocated, IsolationLevel=" + conn.getTransactionIsolation() + " connectionCount = " + connCount);
        }

        return conn;
    }

    public Connection getConnPublic() throws Exception {
        return getConn();
    }

    /*
     * Commit the provided connection if not under Global Transaction scope -
     * conn.commit() is not allowed in a global transaction. the txn manager
     * will perform the commit
     */
    private void commit(Connection conn) throws Exception {
        if (!inSession) {
            if ((getInGlobalTxn() == false) && (conn != null)) {
                conn.commit();
            }
        }
    }

    /*
     * Rollback the statement for the given connection
     */
    private void rollBack(Connection conn, Exception e) throws Exception {
        if (!inSession) {
            Log.log("TradeDirect:rollBack -- rolling back conn due to previously caught exception -- inGlobalTxn=" + getInGlobalTxn());
            if ((getInGlobalTxn() == false) && (conn != null)) {
                conn.rollback();
            } else {
                throw e; // Throw the exception
                // so the Global txn manager will rollBack
            }
        }
    }

    /*
     * Allocate a new prepared statment for this connection
     */
    private PreparedStatement getStatement(Connection conn, String sql) throws Exception {
        return conn.prepareStatement(sql);
    }

    private PreparedStatement getStatement(Connection conn, String sql, int type, int concurrency) throws Exception {
        return conn.prepareStatement(sql, type, concurrency);
    }

    private static final String createQuoteSQL = "insert into quoteejb " + "( symbol, companyName, volume, price, open1, low, high, change1 ) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )";

    private static final String createAccountSQL = "insert into accountejb "
            + "( accountid, creationDate, openBalance, balance, lastLogin, loginCount, logoutCount, profile_userid) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )";

    private static final String createAccountProfileSQL = "insert into accountprofileejb " + "( userid, passwd, fullname, address, email, creditcard ) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )";

    private static final String createHoldingSQL = "insert into holdingejb "
            + "( holdingid, purchaseDate, purchasePrice, quantity, quote_symbol, account_accountid ) " + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ? )";

    private static final String createOrderSQL = "insert into orderejb "
            + "( orderid, ordertype, orderstatus, opendate, quantity, price, orderfee, account_accountid,  holding_holdingid, quote_symbol) "
            + "VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  , ? , ? , ?)";

    private static final String removeHoldingSQL = "delete from holdingejb where holdingid = ?";

    private static final String removeHoldingFromOrderSQL = "update orderejb set holding_holdingid=null where holding_holdingid = ?";

    private static final String updateAccountProfileSQL = "update accountprofileejb set " + "passwd = ?, fullname = ?, address = ?, email = ?, creditcard = ? "
            + "where userid = (select profile_userid from accountejb a " + "where a.profile_userid=?)";

    private static final String loginSQL = "update accountejb set lastLogin=?, logincount=logincount+1 " + "where profile_userid=?";

    private static final String logoutSQL = "update accountejb set logoutcount=logoutcount+1 " + "where profile_userid=?";

    private static final String getAccountSQL = "select * from accountejb a where a.accountid = ?";

    private static final String getAccountProfileSQL = "select * from accountprofileejb ap where ap.userid = "
            + "(select profile_userid from accountejb a where a.profile_userid=?)";

    private static final String getAccountProfileForAccountSQL = "select * from accountprofileejb ap where ap.userid = "
            + "(select profile_userid from accountejb a where a.accountid=?)";

    private static final String getAccountForUserSQL = "select * from accountejb a where a.profile_userid = "
            + "( select userid from accountprofileejb ap where ap.userid = ?)";

    private static final String getHoldingSQL = "select * from holdingejb h where h.holdingid = ?";

    private static final String getHoldingsForUserSQL = "select * from holdingejb h where h.account_accountid = "
            + "(select a.accountid from accountejb a where a.profile_userid = ?)";

    private static final String getOrderSQL = "select * from orderejb o where o.orderid = ?";

    private static final String getOrdersByUserSQL = "select * from orderejb o where o.account_accountid = "
            + "(select a.accountid from accountejb a where a.profile_userid = ?)";

    private static final String getClosedOrdersSQL = "select * from orderejb o " + "where o.orderstatus = 'closed' AND o.account_accountid = "
            + "(select a.accountid from accountejb a where a.profile_userid = ?)";

    private static final String getQuoteSQL = "select * from quoteejb q where q.symbol=?";

    private static final String getAllQuotesSQL = "select * from quoteejb q";

    private static final String getQuoteForUpdateSQL = "select * from quoteejb q where q.symbol=? For Update";

    private static final String getTSIAQuotesOrderByChangeSQL = "select * from quoteejb q order by q.change1";

    private static final String getTSIASQL = "select SUM(price)/count(*) as TSIA from quoteejb q ";

    private static final String getOpenTSIASQL = "select SUM(open1)/count(*) as openTSIA from quoteejb q ";

    private static final String getTSIATotalVolumeSQL = "select SUM(volume) as totalVolume from quoteejb q ";

    private static final String creditAccountBalanceSQL = "update accountejb set " + "balance = balance + ? " + "where accountid = ?";

    private static final String updateOrderStatusSQL = "update orderejb set " + "orderstatus = ?, completiondate = ? " + "where orderid = ?";

    private static final String updateOrderHoldingSQL = "update orderejb set " + "holding_holdingID = ? " + "where orderid = ?";

    private static final String updateQuotePriceVolumeSQL = "update quoteejb set " + "price = ?, change1 = ?, volume = ? " + "where symbol = ?";

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        if (Log.doTrace()) {
            Log.trace("TradeDirect:init -- *** initializing");
        }
        try {
            if (Log.doTrace()) {
                Log.trace("TradeDirect: init");
            }
            context = new InitialContext();
            datasource = (DataSource) context.lookup(dsName);
        } catch (Exception e) {
            Log.error("TradeDirect:init -- error on JNDI lookups of DataSource -- TradeDirect will not work", e);
            return;
        }
        
        try {
            qConnFactory = (ConnectionFactory) context.lookup("java:comp/env/jms/QueueConnectionFactory");
        } catch (Exception e) {
            Log.error("TradeDirect:init  Unable to locate QueueConnectionFactory.\n\t -- Asynchronous mode will not work correctly and Quote Price change publishing will be disabled");
            TradeConfig.setPublishQuotePriceChange(false);
        }

        try {
            brokerQueue = (Queue) context.lookup("java:comp/env/jms/TradeBrokerQueue");
        } catch (Exception e) {
            try {
                brokerQueue = (Queue) context.lookup("jms/TradeBrokerQueue");
            } catch (Exception e2) {
                Log.error("TradeDirect:init  Unable to locate TradeBrokerQueue.\n\t -- Asynchronous mode will not work correctly and Quote Price change publishing will be disabled");
                TradeConfig.setPublishQuotePriceChange(false);
            }
        }

        try {
            tConnFactory = (ConnectionFactory) context.lookup("java:comp/env/jms/TopicConnectionFactory");
        } catch (Exception e) {
            Log.error("TradeDirect:init  Unable to locate TopicConnectionFactory.\n\t -- Asynchronous mode will not work correctly and Quote Price change publishing will be disabled");
            TradeConfig.setPublishQuotePriceChange(false);
        }

        try {
            streamerTopic = (Topic) context.lookup("java:comp/env/jms/TradeStreamerTopic");
        } catch (Exception e) {
            try {
                streamerTopic = (Topic) context.lookup("jms/TradeStreamerTopic");
            } catch (Exception e2) {
                Log.error("TradeDirect:init  Unable to locate TradeStreamerTopic.\n\t -- Asynchronous mode will not work correctly and Quote Price change publishing will be disabled");
                TradeConfig.setPublishQuotePriceChange(false);
            }
        }
        
        		
        if (Log.doTrace()) {
            Log.trace("TradeDirect:init -- +++ initialized");
        }

        initialized = true;
    }

    public static void destroy() {
        try {
            if (!initialized) {
                return;
            }
            Log.trace("TradeDirect:destroy");
        } catch (Exception e) {
            Log.error("TradeDirect:destroy", e);
        }
    }

    private static InitialContext context;

    private static ConnectionFactory qConnFactory;

    private static Queue brokerQueue;

    private static ConnectionFactory tConnFactory;

    private static Topic streamerTopic;
     
    /**
     * Gets the inGlobalTxn
     *
     * @return Returns a boolean
     */
    private boolean getInGlobalTxn() {
        return inGlobalTxn;
    }

    /**
     * Sets the inGlobalTxn
     *
     * @param inGlobalTxn
     *            The inGlobalTxn to set
     */
    private void setInGlobalTxn(boolean inGlobalTxn) {
        this.inGlobalTxn = inGlobalTxn;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.MDBStats;
import com.ibm.websphere.samples.daytrader.util.TimerStat;

@TransactionAttribute(TransactionAttributeType.REQUIRED)
@TransactionManagement(TransactionManagementType.CONTAINER)
@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "TradeBrokerQueue"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "NonDurable") })
public class DTBroker3MDB implements MessageListener {
    private final MDBStats mdbStats;
    private int statInterval = 10000;

    
    // TODO: Using local interface, make it configurable to use remote?
    @EJB
    private TradeSLSBLocal tradeSLSB;

    @Resource
    public MessageDrivenContext mdc;

    public DTBroker3MDB() {
        if (Log.doTrace()) {
            Log.trace("DTBroker3MDB:DTBroker3MDB()");
        }
        if (statInterval <= 0) {
            statInterval = 10000;
        }
        mdbStats = MDBStats.getInstance();
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (Log.doTrace()) {
                Log.trace("TradeBroker:onMessage -- received message -->" + ((TextMessage) message).getText() + "command-->"
                        + message.getStringProperty("command") + "<--");
            }

            if (message.getJMSRedelivered()) {
                Log.log("DTBroker3MDB: The following JMS message was redelivered due to a rollback:\n" + ((TextMessage) message).getText());
                // Order has been cancelled -- ignore returned messages
                return;
            }
            String command = message.getStringProperty("command");
            if (command == null) {
                Log.debug("DTBroker3MDB:onMessage -- received message with null command. Message-->" + message);
                return;
            }
            if (command.equalsIgnoreCase("neworder")) {
                /* Get the Order ID and complete the Order */
                Integer orderID = new Integer(message.getIntProperty("orderID"));
                boolean twoPhase = message.getBooleanProperty("twoPhase");
                boolean direct = message.getBooleanProperty("direct");
                long publishTime = message.getLongProperty("publishTime");
                long receiveTime = System.currentTimeMillis();

                TradeServices trade = null;

                try {
                    trade = getTrade(direct);

                    if (Log.doTrace()) {
                        Log.trace("DTBroker3MDB:onMessage - completing order " + orderID + " twoPhase=" + twoPhase + " direct=" + direct);
                    }

                    trade.completeOrder(orderID, twoPhase);

                    TimerStat currentStats = mdbStats.addTiming("DTBroker3MDB:neworder", publishTime, receiveTime);

                    if ((currentStats.getCount() % statInterval) == 0) {
                        Log.log(" DTBroker3MDB: processed " + statInterval + " stock trading orders." +
                                " Total NewOrders process = " + currentStats.getCount() +
                                "Time (in seconds):" +
                                " min: " +currentStats.getMinSecs()+
                                " max: " +currentStats.getMaxSecs()+
                                " avg: " +currentStats.getAvgSecs());
                    }
                } catch (Exception e) {
                    Log.error("DTBroker3MDB:onMessage Exception completing order: " + orderID + "\n", e);
                    mdc.setRollbackOnly();
                    /*
                     * UPDATE - order is cancelled in trade if an error is
                     * caught try { trade.cancelOrder(orderID, twoPhase); }
                     * catch (Exception e2) { Log.error("order cancel failed",
                     * e); }
                     */
                }
            } else if (command.equalsIgnoreCase("ping")) {
                if (Log.doTrace()) {
                    Log.trace("DTBroker3MDB:onMessage  received test command -- message: " + ((TextMessage) message).getText());
                }

                long publishTime = message.getLongProperty("publishTime");
                long receiveTime = System.currentTimeMillis();

                TimerStat currentStats = mdbStats.addTiming("DTBroker3MDB:ping", publishTime, receiveTime);

                if ((currentStats.getCount() % statInterval) == 0) {
                    Log.log(" DTBroker3MDB: received " + statInterval + " ping messages." +
                            " Total ping message count = " + currentStats.getCount() +
                            " Time (in seconds):" +
                            " min: " +currentStats.getMinSecs()+
                            " max: " +currentStats.getMaxSecs()+
                            " avg: " +currentStats.getAvgSecs());
                }
            } else {
                Log.error("DTBroker3MDB:onMessage - unknown message request command-->" + command + "<-- message=" + ((TextMessage) message).getText());
            }
        } catch (Throwable t) {
            // JMS onMessage should handle all exceptions
            Log.error("DTBroker3MDB: Error rolling back transaction", t);
            mdc.setRollbackOnly();
        }
    }

    private TradeServices getTrade(boolean direct) throws Exception {
        TradeServices trade;
        if (direct) {
            trade = new TradeDirect();
        } else {
            trade = tradeSLSB;
        }

        return trade;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.MDBStats;
import com.ibm.websphere.samples.daytrader.util.TimerStat;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;
import com.ibm.websphere.samples.daytrader.util.WebSocketJMSMessage;

@TransactionAttribute(TransactionAttributeType.REQUIRED)
@TransactionManagement(TransactionManagementType.CONTAINER)
@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "TradeStreamerTopic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "NonDurable") })
public class DTStreamer3MDB implements MessageListener {

    private final MDBStats mdbStats;
    private int statInterval = 10000;

    @Resource
    public MessageDrivenContext mdc;

    /** Creates a new instance of TradeSteamerMDB */
    public DTStreamer3MDB() {
        if (Log.doTrace()) {
            Log.trace("DTStreamer3MDB:DTStreamer3MDB()");
        }
        if (statInterval <= 0) {
            statInterval = 10000;
        }
        mdbStats = MDBStats.getInstance();
    }

    @Inject
    @WebSocketJMSMessage
    Event<Message> jmsEvent;

    @Override
    public void onMessage(Message message) {

        try {
            if (Log.doTrace()) {
                Log.trace("DTStreamer3MDB:onMessage -- received message -->" + ((TextMessage) message).getText() + "command-->"
                        + message.getStringProperty("command") + "<--");
            }
            String command = message.getStringProperty("command");
            if (command == null) {
                Log.debug("DTStreamer3MDB:onMessage -- received message with null command. Message-->" + message);
                return;
            }
            if (command.equalsIgnoreCase("updateQuote")) {
                if (Log.doTrace()) {
                    Log.trace("DTStreamer3MDB:onMessage -- received message -->" + ((TextMessage) message).getText() + "\n\t symbol = "
                            + message.getStringProperty("symbol") + "\n\t current price =" + message.getStringProperty("price") + "\n\t old price ="
                            + message.getStringProperty("oldPrice"));
                }
                long publishTime = message.getLongProperty("publishTime");
                long receiveTime = System.currentTimeMillis();

                TimerStat currentStats = mdbStats.addTiming("DTStreamer3MDB:udpateQuote", publishTime, receiveTime);

                if ((currentStats.getCount() % statInterval) == 0) {
                    Log.log(" DTStreamer3MDB: " + statInterval + " prices updated:" +
                            " Total message count = " + currentStats.getCount() +
                            " Time (in seconds):" +
                            " min: " +currentStats.getMinSecs()+
                            " max: " +currentStats.getMaxSecs()+
                            " avg: " +currentStats.getAvgSecs() );
                }
                
                // Fire message to Websocket Endpoint
                // Limit Symbols that get sent with percentageToWebSocket (default 5%).
                int symbolNumber = new Integer(message.getStringProperty("symbol").substring(2));
                
                if ( symbolNumber < TradeConfig.getMAX_QUOTES() * TradeConfig.getPercentSentToWebsocket() * 0.01) {
                	jmsEvent.fire(message);
                }
                
            } else if (command.equalsIgnoreCase("ping")) {
                if (Log.doTrace()) {
                    Log.trace("DTStreamer3MDB:onMessage  received ping command -- message: " + ((TextMessage) message).getText());
                }

                long publishTime = message.getLongProperty("publishTime");
                long receiveTime = System.currentTimeMillis();

                TimerStat currentStats = mdbStats.addTiming("DTStreamer3MDB:ping", publishTime, receiveTime);

                if ((currentStats.getCount() % statInterval) == 0) {
                    Log.log(" DTStreamer3MDB: received " + statInterval + " ping messages." +
                            " Total message count = " + currentStats.getCount() +
                            " Time (in seconds):" +
                            " min: " +currentStats.getMinSecs()+
                            " max: " +currentStats.getMaxSecs()+
                            " avg: " +currentStats.getAvgSecs());
                }
            } else {
                Log.error("DTStreamer3MDB:onMessage - unknown message request command-->" + command + "<-- message=" + ((TextMessage) message).getText());
            }
        } catch (Throwable t) {
            // JMS onMessage should handle all exceptions
            Log.error("DTStreamer3MDB: Exception", t);
             //UPDATE - Not rolling back for now -- so error messages are not redelivered
             mdc.setRollbackOnly();
        }
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Singleton
public class MarketSummarySingleton {
    
    private MarketSummaryDataBean marketSummaryDataBean;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @PostConstruct 
    private void setup () {
        updateMarketSummary();
    }
    
    /* Update Market Summary every 20 seconds */
    @Schedule(second = "*/20",minute = "*", hour = "*", persistent = false)
    private void updateMarketSummary() { 
        
        if (Log.doTrace()) {
            Log.trace("MarketSummarySingleton:updateMarketSummary -- updating market summary");
        }
                        
        if (TradeConfig.getRunTimeMode() != TradeConfig.EJB3)
        {
            if (Log.doTrace()) {
                Log.trace("MarketSummarySingleton:updateMarketSummary -- Not EJB3 Mode, so not updating");
            }
            return; // Only do the actual work if in EJB3 Mode
        }
        
        List<QuoteDataBean> quotes;
        
        try {        
        	// Find Trade Stock Index Quotes (Top 100 quotes) ordered by their change in value
        	CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        	CriteriaQuery<QuoteDataBean> criteriaQuery = criteriaBuilder.createQuery(QuoteDataBean.class);
        	Root<QuoteDataBean> quoteRoot = criteriaQuery.from(QuoteDataBean.class);
        	criteriaQuery.orderBy(criteriaBuilder.desc(quoteRoot.get("change1")));
        	criteriaQuery.select(quoteRoot);
        	TypedQuery<QuoteDataBean> q = entityManager.createQuery(criteriaQuery);
        	quotes = q.getResultList();
        } catch (Exception e) {
        	Log.debug("Warning: The database has not been configured. If this is the first time the application has been started, please create and populate the database tables. Then restart the server.");
        	return;
        }	
                
        /* TODO: Make this cleaner? */
        QuoteDataBean[] quoteArray = quotes.toArray(new QuoteDataBean[quotes.size()]);
        ArrayList<QuoteDataBean> topGainers = new ArrayList<QuoteDataBean>(5);
        ArrayList<QuoteDataBean> topLosers = new ArrayList<QuoteDataBean>(5);
        BigDecimal TSIA = FinancialUtils.ZERO;
        BigDecimal openTSIA = FinancialUtils.ZERO;
        double totalVolume = 0.0;

        if (quoteArray.length > 5) {
            for (int i = 0; i < 5; i++) {
                topGainers.add(quoteArray[i]);
            }
            for (int i = quoteArray.length - 1; i >= quoteArray.length - 5; i--) {
                topLosers.add(quoteArray[i]);
            }

            for (QuoteDataBean quote : quoteArray) {
                BigDecimal price = quote.getPrice();
                BigDecimal open = quote.getOpen();
                double volume = quote.getVolume();
                TSIA = TSIA.add(price);
                openTSIA = openTSIA.add(open);
                totalVolume += volume;
            }
            TSIA = TSIA.divide(new BigDecimal(quoteArray.length), FinancialUtils.ROUND);
            openTSIA = openTSIA.divide(new BigDecimal(quoteArray.length), FinancialUtils.ROUND);
        }
        
        setMarketSummaryDataBean(new MarketSummaryDataBean(TSIA, openTSIA, totalVolume, topGainers, topLosers));
    }

    @Lock(LockType.READ)
    public MarketSummaryDataBean getMarketSummaryDataBean() {       
        return marketSummaryDataBean;
    }

    @Lock(LockType.WRITE)
    public void setMarketSummaryDataBean(MarketSummaryDataBean marketSummaryDataBean) {
        this.marketSummaryDataBean = marketSummaryDataBean;
    }

}

/**
 * (C) Copyright IBM Corporation 2015, 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.RollbackException;

import com.ibm.websphere.samples.daytrader.TradeAction;
//import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.beans.RunStatsDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.CompleteOrderThread;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class TradeSLSBBean implements TradeSLSBRemote, TradeSLSBLocal {
	
    @Resource(name = "jms/QueueConnectionFactory", authenticationType = javax.annotation.Resource.AuthenticationType.APPLICATION)
    private QueueConnectionFactory queueConnectionFactory;

    @Resource(name = "jms/TopicConnectionFactory", authenticationType = javax.annotation.Resource.AuthenticationType.APPLICATION)
    private TopicConnectionFactory topicConnectionFactory;

    @Resource(lookup = "jms/TradeStreamerTopic")
    private Topic tradeStreamerTopic;

    @Resource(lookup = "jms/TradeBrokerQueue")
    private Queue tradeBrokerQueue;
    
    @Resource 
    private ManagedThreadFactory managedThreadFactory;
	
    /* JBoss 
    @Resource(name = "java:/jms/QueueConnectionFactory", authenticationType = javax.annotation.Resource.AuthenticationType.APPLICATION)
    private QueueConnectionFactory queueConnectionFactory;

    @Resource(name = "java:/jms/TopicConnectionFactory", authenticationType = javax.annotation.Resource.AuthenticationType.APPLICATION)
    private TopicConnectionFactory topicConnectionFactory;

    @Resource(lookup = "java:/jms/TradeStreamerTopic")
    private Topic tradeStreamerTopic;
        
    @Resource(lookup = "java:/jms/TradeBrokerQueue")
    private Queue tradeBrokerQueue;
    */
    
    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private SessionContext context;
    
    @EJB
    MarketSummarySingleton marketSummarySingleton;

    /** Creates a new instance of TradeSLSBBean */
    public TradeSLSBBean() {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:ejbCreate  -- JNDI lookups of EJB and JMS resources");
        }
    }

    @Override
    public MarketSummaryDataBean getMarketSummary() {

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getMarketSummary -- getting market summary");
        }

        return marketSummarySingleton.getMarketSummaryDataBean();
    }

    @Override
    public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) {
        OrderDataBean order;
        BigDecimal total;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeSLSBBean:buy", userID, symbol, quantity, orderProcessingMode);
            }
            
            AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);
            AccountDataBean account = profile.getAccount();
            QuoteDataBean quote = entityManager.find(QuoteDataBean.class, symbol);
            HoldingDataBean holding = null; // The holding will be created by
            // this buy order

            order = createOrder(account, quote, holding, "buy", quantity);
                      
            // UPDATE - account should be credited during completeOrder
            BigDecimal price = quote.getPrice();
            BigDecimal orderFee = order.getOrderFee();
            BigDecimal balance = account.getBalance();
            total = (new BigDecimal(quantity).multiply(price)).add(orderFee);
            account.setBalance(balance.subtract(total));
            final Integer orderID=order.getOrderID(); 
            
            if (orderProcessingMode == TradeConfig.SYNCH) {
                completeOrder(orderID, false);
            } else {
                entityManager.flush();
                queueOrder(orderID, true);
            }
        } catch (Exception e) {
            Log.error("TradeSLSBBean:buy(" + userID + "," + symbol + "," + quantity + ") --> failed", e);
            /* On exception - cancel the order */
            // TODO figure out how to do this with JPA
            // if (order != null) order.cancel();
            throw new EJBException(e);
        }
        return order;
    }

    @Override
    public OrderDataBean sell(final String userID, final Integer holdingID, int orderProcessingMode) {
        OrderDataBean order;
        BigDecimal total;
        try {
            if (Log.doTrace()) {
                Log.trace("TradeSLSBBean:sell", userID, holdingID, orderProcessingMode);
            }
            
            AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);
            AccountDataBean account = profile.getAccount();

            HoldingDataBean holding = entityManager.find(HoldingDataBean.class, holdingID);
            
            if (holding == null) {
                Log.error("TradeSLSBBean:sell User " + userID + " attempted to sell holding " + holdingID + " which has already been sold");

                OrderDataBean orderData = new OrderDataBean();
                orderData.setOrderStatus("cancelled");
                entityManager.persist(orderData);

                return orderData;
            }

            QuoteDataBean quote = holding.getQuote();
            double quantity = holding.getQuantity();
            order = createOrder(account, quote, holding, "sell", quantity);

            // UPDATE the holding purchase data to signify this holding is
            // "inflight" to be sold
            // -- could add a new holdingStatus attribute to holdingEJB
            holding.setPurchaseDate(new java.sql.Timestamp(0));

            // UPDATE - account should be credited during completeOrder
            BigDecimal price = quote.getPrice();
            BigDecimal orderFee = order.getOrderFee();
            BigDecimal balance = account.getBalance();
            total = (new BigDecimal(quantity).multiply(price)).subtract(orderFee);
            account.setBalance(balance.add(total));
            final Integer orderID=order.getOrderID();

            if (orderProcessingMode == TradeConfig.SYNCH) {
                completeOrder(orderID, false);
            } else {
                entityManager.flush();
                queueOrder(orderID, true);
            }

        } catch (Exception e) {
            Log.error("TradeSLSBBean:sell(" + userID + "," + holdingID + ") --> failed", e);
            // if (order != null) order.cancel();
            // UPDATE - handle all exceptions like:
            throw new EJBException("TradeSLSBBean:sell(" + userID + "," + holdingID + ")", e);
        }
        return order;
    }

    @Override
    public void queueOrder(Integer orderID, boolean twoPhase) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:queueOrder", orderID);
        }
                	
        if (TradeConfig.getOrderProcessingMode() == TradeConfig.ASYNCH_MANAGEDTHREAD) {
        
            Thread thread = managedThreadFactory.newThread(new CompleteOrderThread(orderID, twoPhase));
            
            thread.start();
        
        } else {
        
            try (JMSContext queueContext = queueConnectionFactory.createContext();) {
                TextMessage message = queueContext.createTextMessage();

                message.setStringProperty("command", "neworder");
                message.setIntProperty("orderID", orderID);
                message.setBooleanProperty("twoPhase", twoPhase);
                message.setText("neworder: orderID=" + orderID + " runtimeMode=EJB twoPhase=" + twoPhase);
                message.setLongProperty("publishTime", System.currentTimeMillis());
        		        		
                queueContext.createProducer().send(tradeBrokerQueue, message);
        		
            } catch (Exception e) {
                throw new EJBException(e.getMessage(), e); // pass the exception
            }
        }
    }

    @Override
    public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:completeOrder", orderID + " twoPhase=" + twoPhase);
        }  
              
        OrderDataBean order = entityManager.find(OrderDataBean.class, orderID);
        
        if (order == null) {
            throw new EJBException("Error: attempt to complete Order that is null\n" + order);
        }
        
        order.getQuote();

        if (order.isCompleted()) {
            throw new EJBException("Error: attempt to complete Order that is already completed\n" + order);
        }

        AccountDataBean account = order.getAccount();
        QuoteDataBean quote = order.getQuote();
        HoldingDataBean holding = order.getHolding();
        BigDecimal price = order.getPrice();
        double quantity = order.getQuantity();

        String userID = account.getProfile().getUserID();

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBeanInternal:completeOrder--> Completing Order " + order.getOrderID() + "\n\t Order info: " + order + "\n\t Account info: "
                    + account + "\n\t Quote info: " + quote + "\n\t Holding info: " + holding);
        }

        if (order.isBuy()) {
            /*
             * Complete a Buy operation - create a new Holding for the Account -
             * deduct the Order cost from the Account balance
             */

            HoldingDataBean newHolding = createHolding(account, quote, quantity, price);
            order.setHolding(newHolding);
        }

        if (order.isSell()) {
            /*
             * Complete a Sell operation - remove the Holding from the Account -
             * deposit the Order proceeds to the Account balance
             */
            if (holding == null) {
                //Log.error("TradeSLSBBean:completeOrder -- Unable to sell order " + order.getOrderID() + " holding already sold");
                order.cancel();
                throw new EJBException("TradeSLSBBean:completeOrder -- Unable to sell order " + order.getOrderID() + " holding already sold");
            } else {
                entityManager.remove(holding);
                order.setHolding(null);
            }
            
            
        }
        order.setOrderStatus("closed");

        order.setCompletionDate(new java.sql.Timestamp(System.currentTimeMillis()));

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:completeOrder--> Completed Order " + order.getOrderID() + "\n\t Order info: " + order + "\n\t Account info: " + account
                    + "\n\t Quote info: " + quote + "\n\t Holding info: " + holding);
        }
        // if (Log.doTrace())
        // Log.trace("Calling TradeAction:orderCompleted from Session EJB using Session Object");
        // FUTURE All getEJBObjects could be local -- need to add local I/F

        TradeAction tradeAction = new TradeAction();
        tradeAction.orderCompleted(userID, orderID);

       
        
        return order;
    }

    @Override
    public void cancelOrder(Integer orderID, boolean twoPhase) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:cancelOrder", orderID + " twoPhase=" + twoPhase);
        }

        OrderDataBean order = entityManager.find(OrderDataBean.class, orderID);
        order.cancel();
    }

    @Override
    public void orderCompleted(String userID, Integer orderID) {
        throw new UnsupportedOperationException("TradeSLSBBean:orderCompleted method not supported");
    }

    @Override
    public Collection<OrderDataBean> getOrders(String userID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getOrders", userID);
        }

        AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);
        AccountDataBean account = profile.getAccount();
        return account.getOrders();
    }

    @Override
    public Collection<OrderDataBean> getClosedOrders(String userID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getClosedOrders", userID);
        }

        try {
            /* I want to do a CriteriaUpdate here, but there are issues with JBoss/Hibernate */
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<OrderDataBean> criteriaQuery = criteriaBuilder.createQuery(OrderDataBean.class);
            Root<OrderDataBean> orders = criteriaQuery.from(OrderDataBean.class);
            criteriaQuery.select(orders);
            criteriaQuery.where(
              criteriaBuilder.equal(orders.get("orderStatus"), 
              criteriaBuilder.parameter(String.class, "p_status")),
              criteriaBuilder.equal(orders.get("account").get("profile").get("userID"),
              criteriaBuilder.parameter(String.class, "p_userid")));
            
            TypedQuery<OrderDataBean> q = entityManager.createQuery(criteriaQuery);
            q.setParameter("p_status", "closed");
            q.setParameter("p_userid", userID);
            List<OrderDataBean> results = q.getResultList();
            
            Iterator<OrderDataBean> itr = results.iterator();
            
            // Spin through the orders to remove or mark completed
            while (itr.hasNext()) {
                OrderDataBean order = itr.next();
                // TODO: Investigate ConncurrentModification Exceptions                                
                if (TradeConfig.getLongRun()) {
                    //Added this for Longruns (to prevent orderejb growth)
                    entityManager.remove(order); 
                }
                else {
                    order.setOrderStatus("completed");
                }
            }

            return results;
            
        } catch (Exception e) {
            Log.error("TradeSLSBBean.getClosedOrders", e);
            throw new EJBException("TradeSLSBBean.getClosedOrders - error", e);
        }
    }

    @Override
    public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) {
        try {
            QuoteDataBean quote = new QuoteDataBean(symbol, companyName, 0, price, price, price, price, 0);
            entityManager.persist(quote);
            if (Log.doTrace()) {
                Log.trace("TradeSLSBBean:createQuote-->" + quote);
            }
            return quote;
        } catch (Exception e) {
            Log.error("TradeSLSBBean:createQuote -- exception creating Quote", e);
            throw new EJBException(e);
        }
    }

    @Override
    public QuoteDataBean getQuote(String symbol) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getQuote", symbol);
        }

        return entityManager.find(QuoteDataBean.class, symbol);
    }

    @Override
    public Collection<QuoteDataBean> getAllQuotes() {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getAllQuotes");
        }

        TypedQuery<QuoteDataBean> query = entityManager.createNamedQuery("quoteejb.allQuotes",QuoteDataBean.class);
        return query.getResultList();
    }

    @Override
    public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal changeFactor, double sharesTraded) {
        if (!TradeConfig.getUpdateQuotePrices()) {
            return new QuoteDataBean();
        }

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:updateQuote", symbol, changeFactor);
        }

        TypedQuery<QuoteDataBean> q = entityManager.createNamedQuery("quoteejb.quoteForUpdate",QuoteDataBean.class);
        q.setParameter(1, symbol);
        QuoteDataBean quote = q.getSingleResult();

        BigDecimal oldPrice = quote.getPrice();
        BigDecimal openPrice = quote.getOpen();

        if (oldPrice.equals(TradeConfig.PENNY_STOCK_PRICE)) {
            changeFactor = TradeConfig.PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
        } else if (oldPrice.compareTo(TradeConfig.MAXIMUM_STOCK_PRICE) > 0) {
            changeFactor = TradeConfig.MAXIMUM_STOCK_SPLIT_MULTIPLIER;
        }

        BigDecimal newPrice = changeFactor.multiply(oldPrice).setScale(2, BigDecimal.ROUND_HALF_UP);

        quote.setPrice(newPrice);
        quote.setChange(newPrice.subtract(openPrice).doubleValue());
        quote.setVolume(quote.getVolume() + sharesTraded);
        entityManager.merge(quote);

        context.getBusinessObject(TradeSLSBLocal.class).publishQuotePriceChange(quote, oldPrice, changeFactor, sharesTraded);
       
        return quote;
    }

    @Override
    public Collection<HoldingDataBean> getHoldings(String userID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getHoldings", userID);
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<HoldingDataBean> criteriaQuery = criteriaBuilder.createQuery(HoldingDataBean.class);
        Root<HoldingDataBean> holdings = criteriaQuery.from(HoldingDataBean.class);
        criteriaQuery.where(
          criteriaBuilder.equal(holdings.get("account").get("profile").get("userID"), 
          criteriaBuilder.parameter(String.class, "p_userid")));
        criteriaQuery.select(holdings);

        TypedQuery<HoldingDataBean> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setParameter("p_userid", userID);
               
        return typedQuery.getResultList();
    }

    @Override
    public HoldingDataBean getHolding(Integer holdingID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getHolding", holdingID);
        }
        return entityManager.find(HoldingDataBean.class, holdingID);
    }

    @Override
    public AccountDataBean getAccountData(String userID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getAccountData", userID);
        }

        AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);
        AccountDataBean account = profile.getAccount();

        // Added to populate transient field for account
        account.setProfileID(profile.getUserID());
        
        return account;
    }

    @Override
    public AccountProfileDataBean getAccountProfileData(String userID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:getProfileData", userID);
        }

        return entityManager.find(AccountProfileDataBean.class, userID);
    }

    @Override
    public AccountProfileDataBean updateAccountProfile(AccountProfileDataBean profileData) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:updateAccountProfileData", profileData);
        }
             
        AccountProfileDataBean temp = entityManager.find(AccountProfileDataBean.class, profileData.getUserID());
        temp.setAddress(profileData.getAddress());
        temp.setPassword(profileData.getPassword());
        temp.setFullName(profileData.getFullName());
        temp.setCreditCard(profileData.getCreditCard());
        temp.setEmail(profileData.getEmail());

        entityManager.merge(temp);

        return temp;
    }

    @Override
    public AccountDataBean login(String userID, String password) throws RollbackException {
        AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);

        if (profile == null) {
            throw new EJBException("No such user: " + userID);
        }
        
        AccountDataBean account = profile.getAccount();

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:login", userID, password);
        }
        account.login(password);
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:login(" + userID + "," + password + ") success" + account);
        }
        
        return account;
    }

    @Override
    public void logout(String userID) {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:logout", userID);
        }

        AccountProfileDataBean profile = entityManager.find(AccountProfileDataBean.class, userID);
        AccountDataBean account = profile.getAccount();

        account.logout();

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:logout(" + userID + ") success");
        }
        
    }

    @Override
    public AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditcard, BigDecimal openBalance) {
        AccountDataBean account = null;
        AccountProfileDataBean profile = null;

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:register", userID, password, fullname, address, email, creditcard, openBalance);
        }

        // Check to see if a profile with the desired userID already exists
        profile = entityManager.find(AccountProfileDataBean.class, userID);

        if (profile != null) {
            Log.error("Failed to register new Account - AccountProfile with userID(" + userID + ") already exists");
            return null;
        } else {
            profile = new AccountProfileDataBean(userID, password, fullname, address, email, creditcard);
            account = new AccountDataBean(0, 0, null, new Timestamp(System.currentTimeMillis()), openBalance, openBalance, userID);

            profile.setAccount(account);
            account.setProfile(profile);

            entityManager.persist(profile);
            entityManager.persist(account);
        }

        return account;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public RunStatsDataBean resetTrade(boolean deleteAll) throws Exception {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:resetTrade", deleteAll);
        }

        return new com.ibm.websphere.samples.daytrader.direct.TradeDirect(false).resetTrade(deleteAll);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void publishQuotePriceChange(QuoteDataBean quote, BigDecimal oldPrice, BigDecimal changeFactor, double sharesTraded) {
        if (!TradeConfig.getPublishQuotePriceChange()) {
            return;
        }
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:publishQuotePricePublishing -- quoteData = " + quote);
        }

        try (JMSContext topicContext = topicConnectionFactory.createContext();) {
    		TextMessage message = topicContext.createTextMessage();

    		message.setStringProperty("command", "updateQuote");
            message.setStringProperty("symbol", quote.getSymbol());
            message.setStringProperty("company", quote.getCompanyName());
            message.setStringProperty("price", quote.getPrice().toString());
            message.setStringProperty("oldPrice", oldPrice.toString());
            message.setStringProperty("open", quote.getOpen().toString());
            message.setStringProperty("low", quote.getLow().toString());
            message.setStringProperty("high", quote.getHigh().toString());
            message.setDoubleProperty("volume", quote.getVolume());
            message.setStringProperty("changeFactor", changeFactor.toString());
            message.setDoubleProperty("sharesTraded", sharesTraded);
            message.setLongProperty("publishTime", System.currentTimeMillis());
            message.setText("Update Stock price for " + quote.getSymbol() + " old price = " + oldPrice + " new price = " + quote.getPrice());
    		        		
    		topicContext.createProducer().send(tradeStreamerTopic, message);
    	} catch (Exception e) {
    		 throw new EJBException(e.getMessage(), e); // pass the exception
    	}
    }

    private OrderDataBean createOrder(AccountDataBean account, QuoteDataBean quote, HoldingDataBean holding, String orderType, double quantity) {

        OrderDataBean order;

        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:createOrder(orderID=" + " account=" + ((account == null) ? null : account.getAccountID()) + " quote="
                    + ((quote == null) ? null : quote.getSymbol()) + " orderType=" + orderType + " quantity=" + quantity);
        }
        try {
            order = new OrderDataBean(orderType, "open", new Timestamp(System.currentTimeMillis()), null, quantity, quote.getPrice().setScale(
                    FinancialUtils.SCALE, FinancialUtils.ROUND), TradeConfig.getOrderFee(orderType), account, quote, holding);
            entityManager.persist(order);
        } catch (Exception e) {
            Log.error("TradeSLSBBean:createOrder -- failed to create Order. The stock/quote may not exist in the database.", e);
            throw new EJBException("TradeSLSBBean:createOrder -- failed to create Order. Check that the symbol exists in the database.", e);
        }
        return order;
    }

    private HoldingDataBean createHolding(AccountDataBean account, QuoteDataBean quote, double quantity, BigDecimal purchasePrice) throws Exception {
        HoldingDataBean newHolding = new HoldingDataBean(quantity, purchasePrice, new Timestamp(System.currentTimeMillis()), account, quote);
        entityManager.persist(newHolding);
        return newHolding;
    }

    public double investmentReturn(double investment, double NetValue) throws Exception {
        if (Log.doTrace()) {
            Log.trace("TradeSLSBBean:investmentReturn");
        }

        double diff = NetValue - investment;
        double ir = diff / investment;
        return ir;
    }

    public QuoteDataBean pingTwoPhase(String symbol) throws Exception {
      
    	if (Log.doTrace()) {
    		Log.trace("TradeSLSBBean:pingTwoPhase", symbol);
    	}
                     
    	QuoteDataBean quoteData = null;
            
    	try (JMSContext queueContext = queueConnectionFactory.createContext();) {
    		// Get a Quote and send a JMS message in a 2-phase commit
    		quoteData = entityManager.find(QuoteDataBean.class, symbol);
                		    		
    		TextMessage message = queueContext.createTextMessage();

    		message.setStringProperty("command", "ping");
    		message.setLongProperty("publishTime", System.currentTimeMillis());
    		message.setText("Ping message for queue java:comp/env/jms/TradeBrokerQueue sent from TradeSLSBBean:pingTwoPhase at " + new java.util.Date());
    		queueContext.createProducer().send(tradeBrokerQueue, message);
    	} catch (Exception e) {
    		Log.error("TradeSLSBBean:pingTwoPhase -- exception caught", e);
    	}
            	
    	return quoteData;
    } 
    
    class quotePriceComparator implements Comparator<Object> {

        @Override
        public int compare(Object quote1, Object quote2) {
            double change1 = ((QuoteDataBean) quote1).getChange();
            double change2 = ((QuoteDataBean) quote2).getChange();
            return new Double(change2).compareTo(change1);
        }
    }

    @PostConstruct
    public void postConstruct() {
               
        if (Log.doTrace()) {
            Log.trace("updateQuotePrices: " + TradeConfig.getUpdateQuotePrices());
            Log.trace("publishQuotePriceChange: " + TradeConfig.getPublishQuotePriceChange());
        }
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;

import javax.ejb.Local;

import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;


@Local
public interface TradeSLSBLocal extends TradeServices {
    public double investmentReturn(double investment, double NetValue) throws Exception;
    
    public QuoteDataBean pingTwoPhase(String symbol) throws Exception;
    
    public void publishQuotePriceChange(QuoteDataBean quote, BigDecimal oldPrice, BigDecimal changeFactor, double sharesTraded);
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.ejb.Remote;

import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;

import java.math.BigDecimal;


@Remote
public interface TradeSLSBRemote extends TradeServices {
    public double investmentReturn(double investment, double NetValue) throws Exception;
    
    public QuoteDataBean pingTwoPhase(String symbol) throws Exception;

    public void publishQuotePriceChange(QuoteDataBean quote, BigDecimal oldPrice, BigDecimal changeFactor, double sharesTraded);
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;

import javax.ejb.EJBException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Entity(name = "accountejb")
@Table(name = "accountejb")
public class AccountDataBean implements Serializable {

    private static final long serialVersionUID = 8437841265136840545L;

    /* Accessor methods for persistent fields */
    @TableGenerator(name = "accountIdGen", table = "KEYGENEJB", pkColumnName = "KEYNAME", valueColumnName = "KEYVAL", pkColumnValue = "account", allocationSize = 1000)
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "accountIdGen")
    @Column(name = "ACCOUNTID", nullable = false)
    private Integer accountID; /* accountID */

    @NotNull
    @Column(name = "LOGINCOUNT", nullable = false)
    private int loginCount; /* loginCount */

    @NotNull
    @Column(name = "LOGOUTCOUNT", nullable = false)
    private int logoutCount; /* logoutCount */

    @Column(name = "LASTLOGIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin; /* lastLogin Date */

    @Column(name = "CREATIONDATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate; /* creationDate */

    @Column(name = "BALANCE")
    private BigDecimal balance; /* balance */

    @Column(name = "OPENBALANCE")
    private BigDecimal openBalance; /* open balance */

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Collection<OrderDataBean> orders;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private Collection<HoldingDataBean> holdings;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PROFILE_USERID")
    private AccountProfileDataBean profile;

    /*
     * Accessor methods for relationship fields are only included for the
     * AccountProfile profileID
     */
    @Transient
    private String profileID;

    public AccountDataBean() {
    }

    public AccountDataBean(Integer accountID, int loginCount, int logoutCount, Date lastLogin, Date creationDate, BigDecimal balance, BigDecimal openBalance,
            String profileID) {
        setAccountID(accountID);
        setLoginCount(loginCount);
        setLogoutCount(logoutCount);
        setLastLogin(lastLogin);
        setCreationDate(creationDate);
        setBalance(balance);
        setOpenBalance(openBalance);
        setProfileID(profileID);
    }

    public AccountDataBean(int loginCount, int logoutCount, Date lastLogin, Date creationDate, BigDecimal balance, BigDecimal openBalance, String profileID) {
        setLoginCount(loginCount);
        setLogoutCount(logoutCount);
        setLastLogin(lastLogin);
        setCreationDate(creationDate);
        setBalance(balance);
        setOpenBalance(openBalance);
        setProfileID(profileID);
    }

    public static AccountDataBean getRandomInstance() {
        return new AccountDataBean(new Integer(TradeConfig.rndInt(100000)), // accountID
                TradeConfig.rndInt(10000), // loginCount
                TradeConfig.rndInt(10000), // logoutCount
                new java.util.Date(), // lastLogin
                new java.util.Date(TradeConfig.rndInt(Integer.MAX_VALUE)), // creationDate
                TradeConfig.rndBigDecimal(1000000.0f), // balance
                TradeConfig.rndBigDecimal(1000000.0f), // openBalance
                TradeConfig.rndUserID() // profileID
        );
    }

    @Override
    public String toString() {
        return "\n\tAccount Data for account: " + getAccountID() + "\n\t\t   loginCount:" + getLoginCount() + "\n\t\t  logoutCount:" + getLogoutCount()
                + "\n\t\t    lastLogin:" + getLastLogin() + "\n\t\t creationDate:" + getCreationDate() + "\n\t\t      balance:" + getBalance()
                + "\n\t\t  openBalance:" + getOpenBalance() + "\n\t\t    profileID:" + getProfileID();
    }

    public String toHTML() {
        return "<BR>Account Data for account: <B>" + getAccountID() + "</B>" + "<LI>   loginCount:" + getLoginCount() + "</LI>" + "<LI>  logoutCount:"
                + getLogoutCount() + "</LI>" + "<LI>    lastLogin:" + getLastLogin() + "</LI>" + "<LI> creationDate:" + getCreationDate() + "</LI>"
                + "<LI>      balance:" + getBalance() + "</LI>" + "<LI>  openBalance:" + getOpenBalance() + "</LI>" + "<LI>    profileID:" + getProfileID()
                + "</LI>";
    }

    public void print() {
        Log.log(this.toString());
    }

    public Integer getAccountID() {
        return accountID;
    }

    public void setAccountID(Integer accountID) {
        this.accountID = accountID;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public int getLogoutCount() {
        return logoutCount;
    }

    public void setLogoutCount(int logoutCount) {
        this.logoutCount = logoutCount;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getOpenBalance() {
        return openBalance;
    }

    public void setOpenBalance(BigDecimal openBalance) {
        this.openBalance = openBalance;
    }

    public String getProfileID() {
        return profileID;
    }

    public void setProfileID(String profileID) {
        this.profileID = profileID;
    }

    /*
     * Disabled for D185273 public String getUserID() { return getProfileID(); }
     */

    public Collection<OrderDataBean> getOrders() {
        return orders;
    }

    public void setOrders(Collection<OrderDataBean> orders) {
        this.orders = orders;
    }

    public Collection<HoldingDataBean> getHoldings() {
        return holdings;
    }

    public void setHoldings(Collection<HoldingDataBean> holdings) {
        this.holdings = holdings;
    }

    public AccountProfileDataBean getProfile() {
        return profile;
    }

    public void setProfile(AccountProfileDataBean profile) {
        this.profile = profile;
    }

    public void login(String password) {
        AccountProfileDataBean profile = getProfile();
        if ((profile == null) || (profile.getPassword().equals(password) == false)) {
            String error = "AccountBean:Login failure for account: " + getAccountID()
                    + ((profile == null) ? "null AccountProfile" : "\n\tIncorrect password-->" + profile.getUserID() + ":" + profile.getPassword());
            throw new EJBException(error);
        }

        setLastLogin(new Timestamp(System.currentTimeMillis()));
        setLoginCount(getLoginCount() + 1);
    }

    public void logout() {
        setLogoutCount(getLogoutCount() + 1);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.accountID != null ? this.accountID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        
        if (!(object instanceof AccountDataBean)) {
            return false;
        }
        AccountDataBean other = (AccountDataBean) object;

        if (this.accountID != other.accountID && (this.accountID == null || !this.accountID.equals(other.accountID))) {
            return false;
        }

        return true;
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Entity(name = "accountprofileejb")
@Table(name = "accountprofileejb")
public class AccountProfileDataBean implements java.io.Serializable {

    /* Accessor methods for persistent fields */

    private static final long serialVersionUID = 2794584136675420624L;

    @Id
    @NotNull
    @Column(name = "USERID", nullable = false)
    private String userID; /* userID */

    @Column(name = "PASSWD")
    private String passwd; /* password */

    @Column(name = "FULLNAME")
    private String fullName; /* fullName */

    @Column(name = "ADDRESS")
    private String address; /* address */

    @Column(name = "EMAIL")
    private String email; /* email */

    @Column(name = "CREDITCARD")
    private String creditCard; /* creditCard */

    @OneToOne(mappedBy = "profile", fetch = FetchType.LAZY)
    private AccountDataBean account;

    public AccountProfileDataBean() {
    }

    public AccountProfileDataBean(String userID, String password, String fullName, String address, String email, String creditCard) {
        setUserID(userID);
        setPassword(password);
        setFullName(fullName);
        setAddress(address);
        setEmail(email);
        setCreditCard(creditCard);
    }

    public static AccountProfileDataBean getRandomInstance() {
        return new AccountProfileDataBean(TradeConfig.rndUserID(), // userID
                TradeConfig.rndUserID(), // passwd
                TradeConfig.rndFullName(), // fullname
                TradeConfig.rndAddress(), // address
                TradeConfig.rndEmail(TradeConfig.rndUserID()), // email
                TradeConfig.rndCreditCard() // creditCard
        );
    }

    @Override
    public String toString() {
        return "\n\tAccount Profile Data for userID:" + getUserID() + "\n\t\t   passwd:" + getPassword() + "\n\t\t   fullName:" + getFullName()
                + "\n\t\t    address:" + getAddress() + "\n\t\t      email:" + getEmail() + "\n\t\t creditCard:" + getCreditCard();
    }

    public String toHTML() {
        return "<BR>Account Profile Data for userID: <B>" + getUserID() + "</B>" + "<LI>   passwd:" + getPassword() + "</LI>" + "<LI>   fullName:"
                + getFullName() + "</LI>" + "<LI>    address:" + getAddress() + "</LI>" + "<LI>      email:" + getEmail() + "</LI>" + "<LI> creditCard:"
                + getCreditCard() + "</LI>";
    }

    public void print() {
        Log.log(this.toString());
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPassword() {
        return passwd;
    }

    public void setPassword(String password) {
        this.passwd = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(String creditCard) {
        this.creditCard = creditCard;
    }

    public AccountDataBean getAccount() {
        return account;
    }

    public void setAccount(AccountDataBean account) {
        this.account = account;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.userID != null ? this.userID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
       
        if (!(object instanceof AccountProfileDataBean)) {
            return false;
        }
        AccountProfileDataBean other = (AccountProfileDataBean) object;

        if (this.userID != other.userID && (this.userID == null || !this.userID.equals(other.userID))) {
            return false;
        }

        return true;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Entity(name = "holdingejb")
@Table(name = "holdingejb")
public class HoldingDataBean implements Serializable {

    /* persistent/relationship fields */

    private static final long serialVersionUID = -2338411656251935480L;

    @Id
    @TableGenerator(name = "holdingIdGen", table = "KEYGENEJB", pkColumnName = "KEYNAME", valueColumnName = "KEYVAL", pkColumnValue = "holding", allocationSize = 1000)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "holdingIdGen")
    @Column(name = "HOLDINGID", nullable = false)
    private Integer holdingID; /* holdingID */

    @NotNull
    @Column(name = "QUANTITY", nullable = false)
    private double quantity; /* quantity */

    @Column(name = "PURCHASEPRICE")
    private BigDecimal purchasePrice; /* purchasePrice */

    @Column(name = "PURCHASEDATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date purchaseDate; /* purchaseDate */

    @Transient
    private String quoteID; /* Holding(*) ---> Quote(1) */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ACCOUNTID")
    private AccountDataBean account;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "QUOTE_SYMBOL")
    private QuoteDataBean quote;

    public HoldingDataBean() {
    }

    public HoldingDataBean(Integer holdingID, double quantity, BigDecimal purchasePrice, Date purchaseDate, String quoteID) {
        setHoldingID(holdingID);
        setQuantity(quantity);
        setPurchasePrice(purchasePrice);
        setPurchaseDate(purchaseDate);
        setQuoteID(quoteID);
    }

    public HoldingDataBean(double quantity, BigDecimal purchasePrice, Date purchaseDate, AccountDataBean account, QuoteDataBean quote) {
        setQuantity(quantity);
        setPurchasePrice(purchasePrice);
        setPurchaseDate(purchaseDate);
        setAccount(account);
        setQuote(quote);
    }

    public static HoldingDataBean getRandomInstance() {
        return new HoldingDataBean(new Integer(TradeConfig.rndInt(100000)), // holdingID
                TradeConfig.rndQuantity(), // quantity
                TradeConfig.rndBigDecimal(1000.0f), // purchasePrice
                new java.util.Date(TradeConfig.rndInt(Integer.MAX_VALUE)), // purchaseDate
                TradeConfig.rndSymbol() // symbol
        );
    }

    @Override
    public String toString() {
        return "\n\tHolding Data for holding: " + getHoldingID() + "\n\t\t      quantity:" + getQuantity() + "\n\t\t purchasePrice:" + getPurchasePrice()
                + "\n\t\t  purchaseDate:" + getPurchaseDate() + "\n\t\t       quoteID:" + getQuoteID();
    }

    public String toHTML() {
        return "<BR>Holding Data for holding: " + getHoldingID() + "</B>" + "<LI>      quantity:" + getQuantity() + "</LI>" + "<LI> purchasePrice:"
                + getPurchasePrice() + "</LI>" + "<LI>  purchaseDate:" + getPurchaseDate() + "</LI>" + "<LI>       quoteID:" + getQuoteID() + "</LI>";
    }

    public void print() {
        Log.log(this.toString());
    }

    public Integer getHoldingID() {
        return holdingID;
    }

    public void setHoldingID(Integer holdingID) {
        this.holdingID = holdingID;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getQuoteID() {
        if (quote != null) {
            return quote.getSymbol();
        }
        return quoteID;
    }

    public void setQuoteID(String quoteID) {
        this.quoteID = quoteID;
    }

    public AccountDataBean getAccount() {
        return account;
    }

    public void setAccount(AccountDataBean account) {
        this.account = account;
    }

    public QuoteDataBean getQuote() {
        return quote;
    }

    public void setQuote(QuoteDataBean quote) {
        this.quote = quote;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.holdingID != null ? this.holdingID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        
        if (!(object instanceof HoldingDataBean)) {
            return false;
        }
        HoldingDataBean other = (HoldingDataBean) object;

        if (this.holdingID != other.holdingID && (this.holdingID == null || !this.holdingID.equals(other.holdingID))) {
            return false;
        }

        return true;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;
//import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Entity(name = "orderejb")
@Table(name = "orderejb")
@NamedQueries({
        @NamedQuery(name = "orderejb.findByOrderfee", query = "SELECT o FROM orderejb o WHERE o.orderFee = :orderfee"),
        @NamedQuery(name = "orderejb.findByCompletiondate", query = "SELECT o FROM orderejb o WHERE o.completionDate = :completiondate"),
        @NamedQuery(name = "orderejb.findByOrdertype", query = "SELECT o FROM orderejb o WHERE o.orderType = :ordertype"),
        @NamedQuery(name = "orderejb.findByOrderstatus", query = "SELECT o FROM orderejb o WHERE o.orderStatus = :orderstatus"),
        @NamedQuery(name = "orderejb.findByPrice", query = "SELECT o FROM orderejb o WHERE o.price = :price"),
        @NamedQuery(name = "orderejb.findByQuantity", query = "SELECT o FROM orderejb o WHERE o.quantity = :quantity"),
        @NamedQuery(name = "orderejb.findByOpendate", query = "SELECT o FROM orderejb o WHERE o.openDate = :opendate"),
        @NamedQuery(name = "orderejb.findByOrderid", query = "SELECT o FROM orderejb o WHERE o.orderID = :orderid"),
        @NamedQuery(name = "orderejb.findByAccountAccountid", query = "SELECT o FROM orderejb o WHERE o.account.accountID = :accountAccountid"),
        @NamedQuery(name = "orderejb.findByQuoteSymbol", query = "SELECT o FROM orderejb o WHERE o.quote.symbol = :quoteSymbol"),
        @NamedQuery(name = "orderejb.findByHoldingHoldingid", query = "SELECT o FROM orderejb o WHERE o.holding.holdingID = :holdingHoldingid"),
        @NamedQuery(name = "orderejb.closedOrders", query = "SELECT o FROM orderejb o WHERE o.orderStatus = 'closed' AND o.account.profile.userID  = :userID"),
        @NamedQuery(name = "orderejb.completeClosedOrders", query = "UPDATE orderejb o SET o.orderStatus = 'completed' WHERE o.orderStatus = 'closed' AND o.account.profile.userID  = :userID") })
public class OrderDataBean implements Serializable {

    private static final long serialVersionUID = 120650490200739057L;

    @Id
    @TableGenerator(name = "orderIdGen", table = "KEYGENEJB", pkColumnName = "KEYNAME", valueColumnName = "KEYVAL", pkColumnValue = "order", allocationSize = 1000)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "orderIdGen")
    @Column(name = "ORDERID", nullable = false)
    private Integer orderID; /* orderID */

    @Column(name = "ORDERTYPE")
    private String orderType; /* orderType (buy, sell, etc.) */

    @Column(name = "ORDERSTATUS")
    private String orderStatus; /*
                                 * orderStatus (open, processing, completed,
                                 * closed, cancelled)
                                 */

    @Column(name = "OPENDATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date openDate; /* openDate (when the order was entered) */

    @Column(name = "COMPLETIONDATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completionDate; /* completionDate */

    @NotNull
    @Column(name = "QUANTITY", nullable = false)
    private double quantity; /* quantity */

    @Column(name = "PRICE")
    private BigDecimal price; /* price */

    @Column(name = "ORDERFEE")
    private BigDecimal orderFee; /* price */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ACCOUNTID")
    private AccountDataBean account;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "QUOTE_SYMBOL")
    private QuoteDataBean quote;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HOLDING_HOLDINGID")
    private HoldingDataBean holding;

    /* Fields for relationship fields are not kept in the Data Bean */
    @Transient
    private String symbol;

    public OrderDataBean() {
    }

    public OrderDataBean(Integer orderID, String orderType, String orderStatus, Date openDate, Date completionDate, double quantity, BigDecimal price,
            BigDecimal orderFee, String symbol) {
        setOrderID(orderID);
        setOrderType(orderType);
        setOrderStatus(orderStatus);
        setOpenDate(openDate);
        setCompletionDate(completionDate);
        setQuantity(quantity);
        setPrice(price);
        setOrderFee(orderFee);
        setSymbol(symbol);
    }

    public OrderDataBean(String orderType, String orderStatus, Date openDate, Date completionDate, double quantity, BigDecimal price, BigDecimal orderFee,
            AccountDataBean account, QuoteDataBean quote, HoldingDataBean holding) {
        setOrderType(orderType);
        setOrderStatus(orderStatus);
        setOpenDate(openDate);
        setCompletionDate(completionDate);
        setQuantity(quantity);
        setPrice(price);
        setOrderFee(orderFee);
        setAccount(account);
        setQuote(quote);
        setHolding(holding);
    }

    public static OrderDataBean getRandomInstance() {
        return new OrderDataBean(new Integer(TradeConfig.rndInt(100000)), TradeConfig.rndBoolean() ? "buy" : "sell", "open", new java.util.Date(
                TradeConfig.rndInt(Integer.MAX_VALUE)), new java.util.Date(TradeConfig.rndInt(Integer.MAX_VALUE)), TradeConfig.rndQuantity(),
                TradeConfig.rndBigDecimal(1000.0f), TradeConfig.rndBigDecimal(1000.0f), TradeConfig.rndSymbol());
    }

    @Override
    public String toString() {
        return "Order " + getOrderID() + "\n\t      orderType: " + getOrderType() + "\n\t    orderStatus: " + getOrderStatus() + "\n\t       openDate: "
                + getOpenDate() + "\n\t completionDate: " + getCompletionDate() + "\n\t       quantity: " + getQuantity() + "\n\t          price: "
                + getPrice() + "\n\t       orderFee: " + getOrderFee() + "\n\t         symbol: " + getSymbol();
    }

    public String toHTML() {
        return "<BR>Order <B>" + getOrderID() + "</B>" + "<LI>      orderType: " + getOrderType() + "</LI>" + "<LI>    orderStatus: " + getOrderStatus()
                + "</LI>" + "<LI>       openDate: " + getOpenDate() + "</LI>" + "<LI> completionDate: " + getCompletionDate() + "</LI>"
                + "<LI>       quantity: " + getQuantity() + "</LI>" + "<LI>          price: " + getPrice() + "</LI>" + "<LI>       orderFee: " + getOrderFee()
                + "</LI>" + "<LI>         symbol: " + getSymbol() + "</LI>";
    }

    public void print() {
        Log.log(this.toString());
    }

    public Integer getOrderID() {
        return orderID;
    }

    public void setOrderID(Integer orderID) {
        this.orderID = orderID;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOrderFee() {
        return orderFee;
    }

    public void setOrderFee(BigDecimal orderFee) {
        this.orderFee = orderFee;
    }

    public String getSymbol() {
        if (quote != null) {
            return quote.getSymbol();
        }
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public AccountDataBean getAccount() {
        return account;
    }

    public void setAccount(AccountDataBean account) {
        this.account = account;
    }

    public QuoteDataBean getQuote() {
        return quote;
    }

    public void setQuote(QuoteDataBean quote) {
        this.quote = quote;
    }

    public HoldingDataBean getHolding() {
        return holding;
    }

    public void setHolding(HoldingDataBean holding) {
        this.holding = holding;
    }

    public boolean isBuy() {
        String orderType = getOrderType();
        if (orderType.compareToIgnoreCase("buy") == 0) {
            return true;
        }
        return false;
    }

    public boolean isSell() {
        String orderType = getOrderType();
        if (orderType.compareToIgnoreCase("sell") == 0) {
            return true;
        }
        return false;
    }

    public boolean isOpen() {
        String orderStatus = getOrderStatus();
        if ((orderStatus.compareToIgnoreCase("open") == 0) || (orderStatus.compareToIgnoreCase("processing") == 0)) {
            return true;
        }
        return false;
    }

    public boolean isCompleted() {
        String orderStatus = getOrderStatus();
        if ((orderStatus.compareToIgnoreCase("completed") == 0) || (orderStatus.compareToIgnoreCase("alertcompleted") == 0)
                || (orderStatus.compareToIgnoreCase("cancelled") == 0)) {
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        String orderStatus = getOrderStatus();
        if (orderStatus.compareToIgnoreCase("cancelled") == 0) {
            return true;
        }
        return false;
    }

    public void cancel() {
        setOrderStatus("cancelled");
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.orderID != null ? this.orderID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        
        if (!(object instanceof OrderDataBean)) {
            return false;
        }
        OrderDataBean other = (OrderDataBean) object;
        if (this.orderID != other.orderID && (this.orderID == null || !this.orderID.equals(other.orderID))) {
            return false;
        }
        return true;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Entity(name = "quoteejb")
@Table(name = "quoteejb")
@NamedQueries({
        @NamedQuery(name = "quoteejb.allQuotes", query = "SELECT q FROM quoteejb q")})
@NamedNativeQueries({ @NamedNativeQuery(name = "quoteejb.quoteForUpdate", query = "select * from quoteejb q where q.symbol=? for update", resultClass = com.ibm.websphere.samples.daytrader.entities.QuoteDataBean.class) })
public class QuoteDataBean implements Serializable {

    /* Accessor methods for persistent fields */

    private static final long serialVersionUID = 1847932261895838791L;

    @Id
    @NotNull
    @Column(name = "SYMBOL", nullable = false)
    private String symbol; /* symbol */

    @Column(name = "COMPANYNAME")
    private String companyName; /* companyName */

    @NotNull
    @Column(name = "VOLUME", nullable = false)
    private double volume; /* volume */

    @Column(name = "PRICE")
    private BigDecimal price; /* price */

    @Column(name = "OPEN1")
    private BigDecimal open1; /* open1 price */

    @Column(name = "LOW")
    private BigDecimal low; /* low price */

    @Column(name = "HIGH")
    private BigDecimal high; /* high price */

    @NotNull
    @Column(name = "CHANGE1", nullable = false)
    private double change1; /* price change */

    /* Accessor methods for relationship fields are not kept in the DataBean */

    public QuoteDataBean() {
    }

    public QuoteDataBean(String symbol, String companyName, double volume, BigDecimal price, BigDecimal open, BigDecimal low, BigDecimal high, double change) {
        setSymbol(symbol);
        setCompanyName(companyName);
        setVolume(volume);
        setPrice(price);
        setOpen(open);
        setLow(low);
        setHigh(high);
        setChange(change);
    }

    public static QuoteDataBean getRandomInstance() {
        return new QuoteDataBean(TradeConfig.rndSymbol(), // symbol
                TradeConfig.rndSymbol() + " Incorporated", // Company Name
                TradeConfig.rndFloat(100000), // volume
                TradeConfig.rndBigDecimal(1000.0f), // price
                TradeConfig.rndBigDecimal(1000.0f), // open1
                TradeConfig.rndBigDecimal(1000.0f), // low
                TradeConfig.rndBigDecimal(1000.0f), // high
                TradeConfig.rndFloat(100000) // volume
        );
    }

    // Create a "zero" value quoteDataBean for the given symbol
    public QuoteDataBean(String symbol) {
        setSymbol(symbol);
    }

    @Override
    public String toString() {
        return "\n\tQuote Data for: " + getSymbol() + "\n\t\t companyName: " + getCompanyName() + "\n\t\t      volume: " + getVolume() + "\n\t\t       price: "
                + getPrice() + "\n\t\t        open1: " + getOpen() + "\n\t\t         low: " + getLow() + "\n\t\t        high: " + getHigh()
                + "\n\t\t      change1: " + getChange();
    }

    public String toHTML() {
        return "<BR>Quote Data for: " + getSymbol() + "<LI> companyName: " + getCompanyName() + "</LI>" + "<LI>      volume: " + getVolume() + "</LI>"
                + "<LI>       price: " + getPrice() + "</LI>" + "<LI>        open1: " + getOpen() + "</LI>" + "<LI>         low: " + getLow() + "</LI>"
                + "<LI>        high: " + getHigh() + "</LI>" + "<LI>      change1: " + getChange() + "</LI>";
    }

    public void print() {
        Log.log(this.toString());
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOpen() {
        return open1;
    }

    public void setOpen(BigDecimal open) {
        this.open1 = open;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public double getChange() {
        return change1;
    }

    public void setChange(double change) {
        this.change1 = change;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.symbol != null ? this.symbol.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        
        if (!(object instanceof QuoteDataBean)) {
            return false;
        }
        QuoteDataBean other = (QuoteDataBean) object;
        if (this.symbol != other.symbol && (this.symbol == null || !this.symbol.equals(other.symbol))) {
            return false;
        }
        return true;
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;

public class CompleteOrderThread implements Runnable {

        final Integer orderID;
        boolean twoPhase;
        
        
        public CompleteOrderThread (Integer id, boolean twoPhase) {
            orderID = id;
            this.twoPhase = twoPhase;
        }
        
        @Override
        public void run() {
            TradeServices trade;
            UserTransaction ut = null;
            
            try {
                // TODO: Sometimes, rarely, the commit does not complete before the find in completeOrder (leads to null order)
                // Adding delay here for now, will try to find a better solution in the future.
                Thread.sleep(500);
                
                InitialContext context = new InitialContext();
                ut = (UserTransaction) context.lookup("java:comp/UserTransaction");
                
                ut.begin();
                
                if (TradeConfig.getRunTimeMode() == TradeConfig.EJB3) {
                    trade = (TradeSLSBBean) context.lookup("java:module/TradeSLSBBean");
                } else {
                    trade = new TradeDirect(); 
                }
                
                trade.completeOrder(orderID, twoPhase);
                
                ut.commit();
            } catch (Exception e) {
                
                try {
                    ut.rollback();
                } catch (Exception e1) {
                    throw new EJBException(e1);
                } 
                throw new EJBException(e);
            } 
        }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;

public class FinancialUtils {

    public static final int ROUND = BigDecimal.ROUND_HALF_UP;
    public static final int SCALE = 2;
    public static final BigDecimal ZERO = (new BigDecimal(0.00)).setScale(SCALE);
    public static final BigDecimal ONE = (new BigDecimal(1.00)).setScale(SCALE);
    public static final BigDecimal HUNDRED = (new BigDecimal(100.00)).setScale(SCALE);

    public static BigDecimal computeGain(BigDecimal currentBalance, BigDecimal openBalance) {
        return currentBalance.subtract(openBalance).setScale(SCALE);
    }

    public static BigDecimal computeGainPercent(BigDecimal currentBalance, BigDecimal openBalance) {
        if (openBalance.doubleValue() == 0.0) {
            return ZERO;
        }
        BigDecimal gainPercent = currentBalance.divide(openBalance, ROUND).subtract(ONE).multiply(HUNDRED);
        return gainPercent;
    }

    public static BigDecimal computeHoldingsTotal(Collection<?> holdingDataBeans) {
        BigDecimal holdingsTotal = new BigDecimal(0.0).setScale(SCALE);
        if (holdingDataBeans == null) {
            return holdingsTotal;
        }
        Iterator<?> it = holdingDataBeans.iterator();
        while (it.hasNext()) {
            HoldingDataBean holdingData = (HoldingDataBean) it.next();
            BigDecimal total = holdingData.getPurchasePrice().multiply(new BigDecimal(holdingData.getQuantity()));
            holdingsTotal = holdingsTotal.add(total);
        }
        return holdingsTotal.setScale(SCALE);
    }

    public static String printGainHTML(BigDecimal gain) {
        String htmlString, arrow;
        if (gain.doubleValue() < 0.0) {
            htmlString = "<FONT color=\"#ff0000\">";
            arrow = "arrowdown.gif";
        } else {
            htmlString = "<FONT color=\"#009900\">";
            arrow = "arrowup.gif";
        }

        htmlString += gain.setScale(SCALE, ROUND) + "</FONT><IMG src=\"images/" + arrow + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
        return htmlString;
    }

    public static String printChangeHTML(double change) {
        String htmlString, arrow;
        if (change < 0.0) {
            htmlString = "<FONT color=\"#ff0000\">";
            arrow = "arrowdown.gif";
        } else {
            htmlString = "<FONT color=\"#009900\">";
            arrow = "arrowup.gif";
        }

        htmlString += change + "</FONT><IMG src=\"images/" + arrow + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
        return htmlString;
    }

    public static String printGainPercentHTML(BigDecimal gain) {
        String htmlString, arrow;
        if (gain.doubleValue() < 0.0) {
            htmlString = "(<B><FONT color=\"#ff0000\">";
            arrow = "arrowdown.gif";
        } else {
            htmlString = "(<B><FONT color=\"#009900\">+";
            arrow = "arrowup.gif";
        }

        htmlString += gain.setScale(SCALE, ROUND);
        htmlString += "%</FONT></B>)<IMG src=\"images/" + arrow + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
        return htmlString;
    }

    public static String printQuoteLink(String symbol) {
        return "<A href=\"app?action=quotes&symbols=" + symbol + "\">" + symbol + "</A>";
    }

}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.AbstractSequentialList;
import java.util.ListIterator;

public class KeyBlock extends AbstractSequentialList<Object> {

    // min and max provide range of valid primary keys for this KeyBlock
    private int min = 0;
    private int max = 0;
    private int index = 0;

    /**
     * Constructor for KeyBlock
     */
    public KeyBlock() {
        super();
        min = 0;
        max = 0;
        index = min;
    }

    /**
     * Constructor for KeyBlock
     */
    public KeyBlock(int min, int max) {
        super();
        this.min = min;
        this.max = max;
        index = min;
    }

    /**
     * @see AbstractCollection#size()
     */
    @Override
    public int size() {
        return (max - min) + 1;
    }

    /**
     * @see AbstractSequentialList#listIterator(int)
     */
    @Override
    public ListIterator<Object> listIterator(int arg0) {
        return new KeyBlockIterator();
    }

    class KeyBlockIterator implements ListIterator<Object> {

        /**
         * @see ListIterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return index <= max;
        }

        /**
         * @see ListIterator#next()
         */
        @Override
        public synchronized Object next() {
            if (index > max) {
                throw new java.lang.RuntimeException("KeyBlock:next() -- Error KeyBlock depleted");
            }
            return new Integer(index++);
        }

        /**
         * @see ListIterator#hasPrevious()
         */
        @Override
        public boolean hasPrevious() {
            return index > min;
        }

        /**
         * @see ListIterator#previous()
         */
        @Override
        public Object previous() {
            return new Integer(--index);
        }

        /**
         * @see ListIterator#nextIndex()
         */
        @Override
        public int nextIndex() {
            return index - min;
        }

        /**
         * @see ListIterator#previousIndex()
         */
        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException("KeyBlock: previousIndex() not supported");
        }

        /**
         * @see ListIterator#add()
         */
        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException("KeyBlock: add() not supported");
        }

        /**
         * @see ListIterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("KeyBlock: remove() not supported");
        }

        /**
         * @see ListIterator#set(Object)
         */
        @Override
        public void set(Object arg0) {
        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Log {
    
    private final static Logger log = Logger.getLogger("daytrader");
    

    // A general purpose, high performance logging, tracing, statistic service

    public static void log(String message) {
       log.log(Level.INFO, message);
    }

    public static void log(String msg1, String msg2) {
        log(msg1 + msg2);
    }

    public static void log(String msg1, String msg2, String msg3) {
        log(msg1 + msg2 + msg3);
    }

    public static void error(String message) {
        message = "Error: " + message;
        log.severe(message);
    }

    public static void error(String message, Throwable e) {
        error(message + "\n\t" + e.toString());
        e.printStackTrace(System.out);
    }

    public static void error(String msg1, String msg2, Throwable e) {
        error(msg1 + "\n" + msg2 + "\n\t", e);
    }

    public static void error(String msg1, String msg2, String msg3, Throwable e) {
        error(msg1 + "\n" + msg2 + "\n" + msg3 + "\n\t", e);
    }

    public static void error(Throwable e, String message) {
        error(message + "\n\t", e);
        e.printStackTrace(System.out);
    }

    public static void error(Throwable e, String msg1, String msg2) {
        error(msg1 + "\n" + msg2 + "\n\t", e);
    }

    public static void error(Throwable e, String msg1, String msg2, String msg3) {
        error(msg1 + "\n" + msg2 + "\n" + msg3 + "\n\t", e);
    }

    public static void trace(String message) {
        log.log(Level.FINE, message + " threadID=" + Thread.currentThread());
    }

    public static void trace(String message, Object parm1) {
        trace(message + "(" + parm1 + ")");
    }

    public static void trace(String message, Object parm1, Object parm2) {
        trace(message + "(" + parm1 + ", " + parm2 + ")");
    }

    public static void trace(String message, Object parm1, Object parm2, Object parm3) {
        trace(message + "(" + parm1 + ", " + parm2 + ", " + parm3 + ")");
    }

    public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4) {
        trace(message + "(" + parm1 + ", " + parm2 + ", " + parm3 + ")" + ", " + parm4);
    }

    public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4, Object parm5) {
        trace(message + "(" + parm1 + ", " + parm2 + ", " + parm3 + ")" + ", " + parm4 + ", " + parm5);
    }

    public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4, Object parm5, Object parm6) {
        trace(message + "(" + parm1 + ", " + parm2 + ", " + parm3 + ")" + ", " + parm4 + ", " + parm5 + ", " + parm6);
    }

    public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4, Object parm5, Object parm6, Object parm7) {
        trace(message + "(" + parm1 + ", " + parm2 + ", " + parm3 + ")" + ", " + parm4 + ", " + parm5 + ", " + parm6 + ", " + parm7);
    }

    public static void traceEnter(String message) {
        log.log(Level.FINE,"Method enter --" + message);
    }

    public static void traceExit(String message) {
        log.log(Level.FINE,"Method exit  --" + message);
    }

    public static void stat(String message) {
        log(message);
    }

    public static void debug(String message) {
        log.log(Level.INFO,message);
    }

    public static void print(String message) {
        log(message);
    }

    public static void printObject(Object o) {
        log("\t" + o.toString());
    }

    public static void printCollection(Collection<?> c) {
        log("\t---Log.printCollection -- collection size=" + c.size());
        Iterator<?> it = c.iterator();

        while (it.hasNext()) {
            log("\t\t" + it.next().toString());
        }
        log("\t---Log.printCollection -- complete");
    }

    public static void printCollection(String message, Collection<?> c) {
        log(message);
        printCollection(c);
    }

    public static boolean doActionTrace() {
        return getTrace() || getActionTrace();
    }

    public static boolean doTrace() {
        return getTrace();
    }

    public static boolean doDebug() {
        return true;
    }

    public static boolean doStat() {
        return true;
    }

    /**
     * Gets the trace
     *
     * @return Returns a boolean
     */
    public static boolean getTrace() {
        return TradeConfig.getTrace();
    }

    /**
     * Gets the trace value for Trade actions only
     *
     * @return Returns a boolean
     */
    public static boolean getActionTrace() {
        return TradeConfig.getActionTrace();
    }

    /**
     * Sets the trace
     *
     * @param trace
     *            The trace to set
     */
    public static void setTrace(boolean traceValue) {
        TradeConfig.setTrace(traceValue);
    }

    /**
     * Sets the trace value for Trade actions only
     *
     * @param trace
     *            The trace to set
     */
    public static void setActionTrace(boolean traceValue) {
        TradeConfig.setActionTrace(traceValue);
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class MDBStats extends java.util.HashMap<String, TimerStat> {

    private static final long serialVersionUID = -3759835921094193760L;
    // Singleton class
    private static MDBStats mdbStats = null;

    private MDBStats() {
    }

    public static synchronized MDBStats getInstance() {
        if (mdbStats == null) {
            mdbStats = new MDBStats();
        }
        return mdbStats;
    }

    public TimerStat addTiming(String type, long sendTime, long recvTime) {
        TimerStat stats = null;
        synchronized (type) {

            stats = get(type);
            if (stats == null) {
                stats = new TimerStat();
            }

            long time = recvTime - sendTime;
            if (time > stats.getMax()) {
                stats.setMax(time);
            }
            if (time < stats.getMin()) {
                stats.setMin(time);
            }
            stats.setCount(stats.getCount() + 1);
            stats.setTotalTime(stats.getTotalTime() + time);

            put(type, stats);
        }
        return stats;
    }

    public synchronized void reset() {
        clear();
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates. To enable and disable the creation of type
 * comments go to Window>Preferences>Java>Code Generation.
 */
public class TimerStat {

    private double min = 1000000000.0, max = 0.0, totalTime = 0.0;
    private int count;

    /**
     * Returns the count.
     *
     * @return int
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns the max.
     *
     * @return double
     */
    public double getMax() {
        return max;
    }

    /**
     * Returns the min.
     *
     * @return double
     */
    public double getMin() {
        return min;
    }

    /**
     * Sets the count.
     *
     * @param count
     *            The count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Sets the max.
     *
     * @param max
     *            The max to set
     */
    public void setMax(double max) {
        this.max = max;
    }

    /**
     * Sets the min.
     *
     * @param min
     *            The min to set
     */
    public void setMin(double min) {
        this.min = min;
    }

    /**
     * Returns the totalTime.
     *
     * @return double
     */
    public double getTotalTime() {
        return totalTime;
    }

    /**
     * Sets the totalTime.
     *
     * @param totalTime
     *            The totalTime to set
     */
    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    /**
     * Returns the max in Secs
     *
     * @return double
     */
    public double getMaxSecs() {
        return max / 1000.0;
    }

    /**
     * Returns the min in Secs
     *
     * @return double
     */
    public double getMinSecs() {
        return min / 1000.0;
    }

    /**
     * Returns the average time in Secs
     *
     * @return double
     */
    public double getAvgSecs() {

        double avg = getTotalTime() / getCount();
        return avg / 1000.0;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;

/**
 * TradeConfig is a JavaBean holding all configuration and runtime parameters
 * for the Trade application TradeConfig sets runtime parameters such as the
 * RunTimeMode (EJB, JDBC, EJB_ALT)
 *
 */

public class TradeConfig {

    /* Trade Runtime Configuration Parameters */

    /* Trade Runtime Mode parameters */
    public static String[] runTimeModeNames = { "Full EJB3", "Direct (JDBC)"};
    public static final int EJB3 = 0;
    public static final int DIRECT = 1;
    public static int runTimeMode = EJB3;

    public static String[] orderProcessingModeNames = { "Sync", "Async_2-Phase", "Async_ManagedThread" };
    public static final int SYNCH = 0;
    public static final int ASYNCH_2PHASE = 1;
    public static final int ASYNCH_MANAGEDTHREAD = 2;
    public static int orderProcessingMode = SYNCH;

    public static String[] accessModeNames = { "Standard", "WebServices" };
    public static final int STANDARD = 0;
    private static int accessMode = STANDARD;

    /* Trade Web Interface parameters */
    public static String[] webInterfaceNames = { "JSP", "JSP-Images" };
    public static final int JSP = 0;
    public static final int JSP_Images = 1;
    public static int webInterface = JSP;

    /* Trade Caching Type parameters 
    public static String[] cachingTypeNames = { "DistributedMap", "No Caching" };
    public static final int DISTRIBUTEDMAP = 0;
    public static final int NO_CACHING = 1;
    public static int cachingType = NO_CACHING;
    public static int distributedMapCacheSize = 100000;
    */

    /* Trade Database Scaling parameters */
    private static int MAX_USERS = 15000;
    private static int MAX_QUOTES = 10000;

    /* Trade Database specific paramters */
    public static String JDBC_UID = null;
    public static String JDBC_PWD = null;
    public static String DS_NAME = "java:comp/env/jdbc/TradeDataSource";

    /* Trade XA Datasource specific parameters */
    public static boolean JDBCDriverNeedsGlobalTransation = false;

    /* Trade Config Miscellaneous itmes */
    public static String DATASOURCE = "java:comp/env/jdbc/TradeDataSource";
    public static int KEYBLOCKSIZE = 1000;
    public static int QUOTES_PER_PAGE = 10;
    public static boolean RND_USER = true;
    // public static int RND_SEED = 0;
    private static int MAX_HOLDINGS = 10;
    private static int count = 0;
    private static Object userID_count_semaphore = new Object();
    private static int userID_count = 0;
    private static String hostName = null;
    private static Random r0 = new Random(System.currentTimeMillis());
    // private static Random r1 = new Random(RND_SEED);
    private static Random randomNumberGenerator = r0;
    public static final String newUserPrefix = "ru:";
    public static final int verifyPercent = 5;
    private static boolean trace = false;
    private static boolean actionTrace = false;
    private static boolean updateQuotePrices = true;
    private static int primIterations = 1;
    private static boolean longRun = true;
    private static boolean publishQuotePriceChange = true;
    private static int percentSentToWebsocket = 5;
    private static boolean displayOrderAlerts = true;
    private static boolean useRemoteEJBInterface = false;

    /**
     * -1 means every operation 0 means never perform a market summary > 0 means
     * number of seconds between summaries. These will be synchronized so only
     * one transaction in this period will create a summary and will cache its
     * results.
     */
    private static int marketSummaryInterval = 20;

    /*
     * Penny stocks is a problem where the random price change factor gets a
     * stock down to $.01. In this case trade jumpstarts the price back to $6.00
     * to keep the math interesting.
     */
    public static BigDecimal PENNY_STOCK_PRICE;
    public static BigDecimal PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER;
    static {
        PENNY_STOCK_PRICE = new BigDecimal(0.01);
        PENNY_STOCK_PRICE = PENNY_STOCK_PRICE.setScale(2, BigDecimal.ROUND_HALF_UP);
        PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER = new BigDecimal(600.0);
        PENNY_STOCK_RECOVERY_MIRACLE_MULTIPLIER.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /*
     * CJB (DAYTRADER-25) - Also need to impose a ceiling on the quote price to
     * ensure prevent account and holding balances from exceeding the databases
     * decimal precision. At some point, this maximum value can be used to
     * trigger a stock split.
     */

    public static BigDecimal MAXIMUM_STOCK_PRICE;
    public static BigDecimal MAXIMUM_STOCK_SPLIT_MULTIPLIER;
    static {
        MAXIMUM_STOCK_PRICE = new BigDecimal(400);
        MAXIMUM_STOCK_PRICE.setScale(2, BigDecimal.ROUND_HALF_UP);
        MAXIMUM_STOCK_SPLIT_MULTIPLIER = new BigDecimal(0.5);
        MAXIMUM_STOCK_SPLIT_MULTIPLIER.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /*
     * Trade Scenario actions mixes. Each of the array rows represents a
     * specific Trade Scenario Mix. The columns give the percentages for each
     * action in the column header. Note: "login" is always 0. logout represents
     * both login and logout (because each logout operation will cause a new
     * login when the user context attempts the next action.
     */
    /* Trade Scenario Workload parameters */
    public static final int HOME_OP = 0;
    public static final int QUOTE_OP = 1;
    public static final int LOGIN_OP = 2;
    public static final int LOGOUT_OP = 3;
    public static final int REGISTER_OP = 4;
    public static final int ACCOUNT_OP = 5;
    public static final int PORTFOLIO_OP = 6;
    public static final int BUY_OP = 7;
    public static final int SELL_OP = 8;
    public static final int UPDATEACCOUNT_OP = 9;

    private static int[][] scenarioMixes = {
            // h q l o r a p b s u
            { 20, 40, 0, 4, 2, 10, 12, 4, 4, 4 }, // STANDARD
            { 20, 40, 0, 4, 2, 7, 7, 7, 7, 6 }, // High Volume
    };
    private static char[] actions = { 'h', 'q', 'l', 'o', 'r', 'a', 'p', 'b', 's', 'u' };
    private static int sellDeficit = 0;
    // Tracks the number of buys over sell when a users portfolio is empty
    // Used to maintain the correct ratio of buys/sells

    /* JSP pages for all Trade Actions */

    public static final int WELCOME_PAGE = 0;
    public static final int REGISTER_PAGE = 1;
    public static final int PORTFOLIO_PAGE = 2;
    public static final int QUOTE_PAGE = 3;
    public static final int HOME_PAGE = 4;
    public static final int ACCOUNT_PAGE = 5;
    public static final int ORDER_PAGE = 6;
    public static final int CONFIG_PAGE = 7;
    public static final int STATS_PAGE = 8;
    public static final int MARKET_SUMMARY_PAGE = 9;

    // FUTURE Add XML/XSL View
    public static String[][] webUI = {
            { "/welcome.jsp", "/register.jsp", "/portfolio.jsp", "/quote.jsp", "/tradehome.jsp", "/account.jsp", "/order.jsp", "/config.jsp", "/runStats.jsp",
                    "/marketSummary.jsp" },
            // JSP Interface
            { "/welcomeImg.jsp", "/registerImg.jsp", "/portfolioImg.jsp", "/quoteImg.jsp", "/tradehomeImg.jsp", "/accountImg.jsp", "/orderImg.jsp",
                    "/config.jsp", "/runStats.jsp", "/marketSummary.jsp" },
    // JSP Interface
    };

    // FUTURE:
    // If a "trade2.properties" property file is supplied, reset the default
    // values
    // to match those specified in the file. This provides a persistent runtime
    // property mechanism during server startup

    /**
     * Return the hostname for this system Creation date: (2/16/2000 9:02:25 PM)
     */

    private static String getHostname() {
        try {
            if (hostName == null) {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
                // Strip of fully qualifed domain if necessary
                try {
                    hostName = hostName.substring(0, hostName.indexOf('.'));
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            Log.error("Exception getting local host name using 'localhost' - ", e);
            hostName = "localhost";
        }
        return hostName;
    }

    /**
     * Return a Trade UI Web page based on the current configuration This may
     * return a JSP page or a Servlet page Creation date: (3/14/2000 9:08:34 PM)
     */

    public static String getPage(int pageNumber) {
        return webUI[webInterface][pageNumber];
    }

    /**
     * Return the list of run time mode names Creation date: (3/8/2000 5:58:34
     * PM)
     *
     * @return java.lang.String[]
     */
    public static java.lang.String[] getRunTimeModeNames() {
        return runTimeModeNames;
    }

    private static int scenarioCount = 0;

    /**
     * Return a Trade Scenario Operation based on the setting of the current mix
     * (TradeScenarioMix) Creation date: (2/10/2000 9:08:34 PM)
     */

    public static char getScenarioAction(boolean newUser) {
        int r = rndInt(100); // 0 to 99 = 100
        int i = 0;
        int sum = scenarioMixes[0][i];
        while (sum <= r) {
            i++;
            sum += scenarioMixes[0][i];
        }

        incrementScenarioCount();

        /*
         * In TradeScenarioServlet, if a sell action is selected, but the users
         * portfolio is empty, a buy is executed instead and sellDefecit is
         * incremented. This allows the number of buy/sell operations to stay in
         * sync w/ the given Trade mix.
         */

        if ((!newUser) && (actions[i] == 'b')) {
            synchronized (TradeConfig.class) {
                if (sellDeficit > 0) {
                    sellDeficit--;
                    return 's';
                    // Special case for TradeScenarioServlet to note this is a
                    // buy switched to a sell to fix sellDeficit
                }
            }
        }

        return actions[i];
    }

    public static String getUserID() {
        String userID;
        if (RND_USER) {
            userID = rndUserID();
        } else {
            userID = nextUserID();
        }
        return userID;
    }

    private static final BigDecimal orderFee = new BigDecimal("24.95");
    private static final BigDecimal cashFee = new BigDecimal("0.0");

    public static BigDecimal getOrderFee(String orderType) {
        if ((orderType.compareToIgnoreCase("BUY") == 0) || (orderType.compareToIgnoreCase("SELL") == 0)) {
            return orderFee;
        }

        return cashFee;

    }

    /**
     * Increment the sell deficit counter Creation date: (6/21/2000 11:33:45 AM)
     */
    public static synchronized void incrementSellDeficit() {
        sellDeficit++;
    }

    public static String nextUserID() {
        String userID;
        synchronized (userID_count_semaphore) {
            userID = "uid:" + userID_count;
            userID_count++;
            if (userID_count % MAX_USERS == 0) {
                userID_count = 0;
            }
        }
        return userID;
    }

    public static double random() {
        return randomNumberGenerator.nextDouble();
    }

    public static String rndAddress() {
        return rndInt(1000) + " Oak St.";
    }

    public static String rndBalance() {
        // Give all new users a cool mill in which to trade
        return "1000000";
    }

    public static String rndCreditCard() {
        return rndInt(100) + "-" + rndInt(1000) + "-" + rndInt(1000) + "-" + rndInt(1000);
    }

    public static String rndEmail(String userID) {
        return userID + "@" + rndInt(100) + ".com";
    }

    public static String rndFullName() {
        return "first:" + rndInt(1000) + " last:" + rndInt(5000);
    }

    public static int rndInt(int i) {
        return (new Float(random() * i)).intValue();
    }

    public static float rndFloat(int i) {
        return (new Float(random() * i)).floatValue();
    }

    public static BigDecimal rndBigDecimal(float f) {
        return (new BigDecimal(random() * f)).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public static boolean rndBoolean() {
        return randomNumberGenerator.nextBoolean();
    }

    /**
     * Returns a new Trade user Creation date: (2/16/2000 8:50:35 PM)
     */
    public static synchronized String rndNewUserID() {

        return newUserPrefix + getHostname() + System.currentTimeMillis() + count++;
    }

    public static float rndPrice() {
        return ((new Integer(rndInt(200))).floatValue()) + 1.0f;
    }

    private static final BigDecimal ONE = new BigDecimal(1.0);
	
    public static BigDecimal getRandomPriceChangeFactor() {
        // CJB (DAYTRADER-25) - Vary change factor between 1.1 and 0.9
        double percentGain = rndFloat(1) * 0.1;
        if (random() < .5) {
            percentGain *= -1;
        }
        percentGain += 1;

        // change factor is between +/- 20%
        BigDecimal percentGainBD = (new BigDecimal(percentGain)).setScale(2, BigDecimal.ROUND_HALF_UP);
        if (percentGainBD.doubleValue() <= 0.0) {
            percentGainBD = ONE;
        }

        return percentGainBD;
    }

    public static float rndQuantity() {
        return ((new Integer(rndInt(200))).floatValue()) + 1.0f;
    }

    public static String rndSymbol() {
        return "s:" + rndInt(MAX_QUOTES - 1);
    }

    public static String rndSymbols() {

        String symbols = "";
        int num_symbols = rndInt(QUOTES_PER_PAGE);

        for (int i = 0; i <= num_symbols; i++) {
            symbols += "s:" + rndInt(MAX_QUOTES - 1);
            if (i < num_symbols) {
                symbols += ",";
            }
        }
        return symbols;
    }

    public static String rndUserID() {
        String nextUser = getNextUserIDFromDeck();
        if (Log.doTrace()) {
            Log.trace("TradeConfig:rndUserID -- new trader = " + nextUser);
        }

        return nextUser;
    }

    private static synchronized String getNextUserIDFromDeck() {
        int numUsers = getMAX_USERS();
        if (deck == null) {
            deck = new ArrayList<Integer>(numUsers);
            for (int i = 0; i < numUsers; i++) {
                deck.add(i, new Integer(i));
            }
            java.util.Collections.shuffle(deck, r0);
        }
        if (card >= numUsers) {
            card = 0;
        }
        return "uid:" + deck.get(card++);

    }

    // Trade implements a card deck approach to selecting
    // users for trading with tradescenarioservlet
    private static ArrayList<Integer> deck = null;
    private static int card = 0;
	
    /**
     * Set the list of run time mode names Creation date: (3/8/2000 5:58:34 PM)
     *
     * @param newRunTimeModeNames
     *            java.lang.String[]
     */
    public static void setRunTimeModeNames(java.lang.String[] newRunTimeModeNames) {
        runTimeModeNames = newRunTimeModeNames;
    }

    /**
     * This is a convenience method for servlets to set Trade configuration
     * parameters from servlet initialization parameters. The servlet provides
     * the init param and its value as strings. This method then parses the
     * parameter, converts the value to the correct type and sets the
     * corresponding TradeConfig parameter to the converted value
     *
     */
    public static void setConfigParam(String parm, String value) {
        Log.log("TradeConfig setting parameter: " + parm + "=" + value);
        // Compare the parm value to valid TradeConfig parameters that can be
        // set
        // by servlet initialization

        // First check the proposed new parm and value - if empty or null ignore
        // it
        if (parm == null) {
            return;
        }
        parm = parm.trim();
        if (parm.length() <= 0) {
            return;
        }
        if (value == null) {
            return;
        }
        value = value.trim();

        if (parm.equalsIgnoreCase("runTimeMode")) {
            try {
                for (int i = 0; i < runTimeModeNames.length; i++) {
                    if (value.equalsIgnoreCase(runTimeModeNames[i])) {
                        runTimeMode = i;
                        break;
                    }
                }
            } catch (Exception e) {
                // >>rjm
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "trying to set runtimemode to " + value + "reverting to current value: "
                        + runTimeModeNames[runTimeMode], e);
            } // If the value is bad, simply revert to current
        } else if (parm.equalsIgnoreCase("orderProcessingMode")) {
            try {
                for (int i = 0; i < orderProcessingModeNames.length; i++) {
                    if (value.equalsIgnoreCase(orderProcessingModeNames[i])) {
                        orderProcessingMode = i;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "trying to set orderProcessingMode to " + value
                        + "reverting to current value: " + orderProcessingModeNames[orderProcessingMode], e);
            } // If the value is bad, simply revert to current
        } else if (parm.equalsIgnoreCase("accessMode")) {
            try {
                for (int i = 0; i < accessModeNames.length; i++) {
                    if (value.equalsIgnoreCase(accessModeNames[i])) {
                        accessMode = i;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "trying to set accessMode to " + value + "reverting to current value: "
                        + accessModeNames[accessMode], e);
            }
        } else if (parm.equalsIgnoreCase("WebInterface")) {
            try {
                for (int i = 0; i < webInterfaceNames.length; i++) {
                    if (value.equalsIgnoreCase(webInterfaceNames[i])) {
                        webInterface = i;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "trying to set WebInterface to " + value + "reverting to current value: "
                        + webInterfaceNames[webInterface], e);

            } // If the value is bad, simply revert to current
        } /*else if (parm.equalsIgnoreCase("CachingType")) {
            try {
                for (int i = 0; i < cachingTypeNames.length; i++) {
                    if (value.equalsIgnoreCase(cachingTypeNames[i])) {
                        cachingType = i;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "trying to set CachingType to " + value + "reverting to current value: "
                        + cachingTypeNames[cachingType], e);
            } // If the value is bad, simply revert to current
        }*/ else if (parm.equalsIgnoreCase("maxUsers")) {
            try {
                MAX_USERS = Integer.parseInt(value);
            } catch (Exception e) {
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "Setting maxusers, error parsing string to int:" + value
                        + "revering to current value: " + MAX_USERS, e);
            } // On error, revert to saved
        } else if (parm.equalsIgnoreCase("maxQuotes")) {
            try {
                MAX_QUOTES = Integer.parseInt(value);
            } catch (Exception e) {
                // >>rjm
                Log.error("TradeConfig.setConfigParm(...) minor exception caught" + "Setting max_quotes, error parsing string to int " + value
                        + "reverting to current value: " + MAX_QUOTES, e);
                // <<rjm
            } // On error, revert to saved
        } else if (parm.equalsIgnoreCase("primIterations")) {
            try {
                primIterations = Integer.parseInt(value);
            } catch (Exception e) {
                Log.error("TradeConfig.setConfigParm(..): minor exception caught" + "Setting primIterations, error parsing string to int:" + value
                        + "revering to current value: " + primIterations, e);
            } // On error, revert to saved
        } /*else if (parm.equalsIgnoreCase("DistMapCacheSize")) {
            try {
                distributedMapCacheSize = Integer.parseInt(value);
            } catch (Exception e) {
                // >>rjm
                Log.error("TradeConfig.setConfigParm(...) minor exception caught" + "Setting distributedMapCacheSize, error parsing string" + value
                        + "reverting to current value: " + distributedMapCacheSize, e);
                // <<rjm
            } // On error, revert to saved
        }*/
    }

    /**
     * Gets the orderProcessingModeNames
     *
     * @return Returns a String[]
     */
    public static String[] getOrderProcessingModeNames() {
        return orderProcessingModeNames;
    }

    /**
     * Gets the webInterfaceNames
     *
     * @return Returns a String[]
     */
    public static String[] getWebInterfaceNames() {
        return webInterfaceNames;
    }

    /**
     * Gets the webInterfaceNames
     *
     * @return Returns a String[]
     */
    /*public static String[] getCachingTypeNames() {
        return cachingTypeNames;
    }*/

    /**
     * Gets the scenarioMixes
     *
     * @return Returns a int[][]
     */
    public static int[][] getScenarioMixes() {
        return scenarioMixes;
    }

    /**
     * Gets the trace
     *
     * @return Returns a boolean
     */
    public static boolean getTrace() {
        return trace;
    }

    /**
     * Sets the trace
     *
     * @param trace
     *            The trace to set
     */
    public static void setTrace(boolean traceValue) {
        trace = traceValue;
    }

    /**
     * Gets the mAX_USERS.
     *
     * @return Returns a int
     */
    public static int getMAX_USERS() {
        return MAX_USERS;
    }

    /**
     * Sets the mAX_USERS.
     *
     * @param mAX_USERS
     *            The mAX_USERS to set
     */
    public static void setMAX_USERS(int mAX_USERS) {
        MAX_USERS = mAX_USERS;
        deck = null; // reset the card deck for selecting users
    }

    /**
     * Gets the mAX_QUOTES.
     *
     * @return Returns a int
     */
    public static int getMAX_QUOTES() {
        return MAX_QUOTES;
    }

    /**
     * Sets the mAX_QUOTES.
     *
     * @param mAX_QUOTES
     *            The mAX_QUOTES to set
     */
    public static void setMAX_QUOTES(int mAX_QUOTES) {
        MAX_QUOTES = mAX_QUOTES;
    }

    /**
     * Gets the mAX_HOLDINGS.
     *
     * @return Returns a int
     */
    public static int getMAX_HOLDINGS() {
        return MAX_HOLDINGS;
    }

    /**
     * Sets the mAX_HOLDINGS.
     *
     * @param mAX_HOLDINGS
     *            The mAX_HOLDINGS to set
     */
    public static void setMAX_HOLDINGS(int mAX_HOLDINGS) {
        MAX_HOLDINGS = mAX_HOLDINGS;
    }

    /**
     * Gets the actionTrace.
     *
     * @return Returns a boolean
     */
    public static boolean getActionTrace() {
        return actionTrace;
    }

    /**
     * Sets the actionTrace.
     *
     * @param actionTrace
     *            The actionTrace to set
     */
    public static void setActionTrace(boolean actionTrace) {
        TradeConfig.actionTrace = actionTrace;
    }

    /**
     * Gets the scenarioCount.
     *
     * @return Returns a int
     */
    public static int getScenarioCount() {
        return scenarioCount;
    }

    /**
     * Sets the scenarioCount.
     *
     * @param scenarioCount
     *            The scenarioCount to set
     */
    public static void setScenarioCount(int scenarioCount) {
        TradeConfig.scenarioCount = scenarioCount;
    }

    public static synchronized void incrementScenarioCount() {
        scenarioCount++;
    }

    /**
     * Gets the jdbc driver needs global transaction Some XA Drivers require a
     * global transaction to be started for all SQL calls. To work around this,
     * set this to true to cause the direct mode to start a user transaction.
     *
     * @return Returns a boolean
     */
    public static boolean getJDBCDriverNeedsGlobalTransation() {
        return JDBCDriverNeedsGlobalTransation;
    }

    /**
     * Sets the jdbc driver needs global transaction
     *
     * @param JDBCDriverNeedsGlobalTransationVal
     *            the value
     */
    public static void setJDBCDriverNeedsGlobalTransation(boolean JDBCDriverNeedsGlobalTransationVal) {
        JDBCDriverNeedsGlobalTransation = JDBCDriverNeedsGlobalTransationVal;
    }

    /**
     * Gets the updateQuotePrices.
     *
     * @return Returns a boolean
     */
    public static boolean getUpdateQuotePrices() {
        return updateQuotePrices;
    }

    /**
     * Sets the updateQuotePrices.
     *
     * @param updateQuotePrices
     *            The updateQuotePrices to set
     */
    public static void setUpdateQuotePrices(boolean updateQuotePrices) {
        TradeConfig.updateQuotePrices = updateQuotePrices;
    }

    public static int getPrimIterations() {
        return primIterations;
    }

    public static void setPrimIterations(int iter) {
        primIterations = iter;
    }

    public static boolean getLongRun() {
        return longRun;
    }

    public static void setLongRun(boolean longRun) {
        TradeConfig.longRun = longRun;
    }

    public static void setPublishQuotePriceChange(boolean publishQuotePriceChange) {
        TradeConfig.publishQuotePriceChange = publishQuotePriceChange;
    }

    public static boolean getPublishQuotePriceChange() {
        return publishQuotePriceChange;
    }

    public static void setMarketSummaryInterval(int seconds) {
        TradeConfig.marketSummaryInterval = seconds;
    }

    public static int getMarketSummaryInterval() {
        return TradeConfig.marketSummaryInterval;
    }

    public static void setRunTimeMode(int value) {
        runTimeMode = value;
    }

    public static int getRunTimeMode() {
        return runTimeMode;
    }

    public static void setOrderProcessingMode(int value) {
        orderProcessingMode = value;
    }

    public static int getOrderProcessingMode() {
        return orderProcessingMode;
    }

    public static void setAccessMode(int value) {
        accessMode = value;
    }

    public static int getAccessMode() {
        return accessMode;
    }

    public static void setWebInterface(int value) {
        webInterface = value;
    }

    public static int getWebInterface() {
        return webInterface;
    }

    /*public static void setCachingType(int value) {
        cachingType = value;
    }

    public static int getCachingType() {
        return cachingType;
    }
	*/
    public static void setDisplayOrderAlerts(boolean value) {
        displayOrderAlerts = value;
    }

    public static boolean getDisplayOrderAlerts() {
        return displayOrderAlerts;
    }
    /*
    public static void setDistributedMapCacheSize(int value) {
        distributedMapCacheSize = value;
    }

    public static int getDistributedMapCacheSize() {
        return distributedMapCacheSize;
    }*/

    public static void setPercentSentToWebsocket(int value) {
		percentSentToWebsocket = value;
	}
    
	public static int getPercentSentToWebsocket() {
		return percentSentToWebsocket;
	}
	
	public static void setUseRemoteEJBInterface(boolean value) {
		useRemoteEJBInterface = value;
	}

	public static boolean useRemoteEJBInterface() {
		return useRemoteEJBInterface;
	}	
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface WebSocketJMSMessage {
}




/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@WebFilter(filterName = "OrdersAlertFilter", urlPatterns = "/app")
public class OrdersAlertFilter implements Filter {

    /**
     * Constructor for CompletedOrdersAlertFilter
     */
    public OrdersAlertFilter() {
        super();
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    private FilterConfig filterConfig = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (filterConfig == null) {
            return;
        }

        if (TradeConfig.getDisplayOrderAlerts() == true) {

            try {
                String action = req.getParameter("action");
                if (action != null) {
                    action = action.trim();
                    if ((action.length() > 0) && (!action.equals("logout"))) {
                        String userID;
                        if (action.equals("login")) {
                            userID = req.getParameter("uid");
                        } else {
                            userID = (String) ((HttpServletRequest) req).getSession().getAttribute("uidBean");
                        }

                        if ((userID != null) && (userID.trim().length() > 0)) {
                            TradeServices tAction = null;
                            tAction = new TradeAction();
                            Collection<?> closedOrders = tAction.getClosedOrders(userID);
                            if ((closedOrders != null) && (closedOrders.size() > 0)) {
                                req.setAttribute("closedOrders", closedOrders);
                            }
                            if (Log.doTrace()) {
                                Log.printCollection("OrderAlertFilter: userID=" + userID + " closedOrders=", closedOrders);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.error(e, "OrdersAlertFilter - Error checking for closedOrders");
            }
        }

        chain.doFilter(req, resp/* wrapper */);
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
        this.filterConfig = null;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@WebServlet(name = "TestServlet", urlPatterns = { "/TestServlet" })
public class TestServlet extends HttpServlet {

    private static final long serialVersionUID = -2927579146688173127L;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Process incoming HTTP GET requests
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Process incoming HTTP POST requests
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Main service method for TradeAppServlet
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    public void performTask(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Log.debug("Enter TestServlet doGet");
            TradeConfig.runTimeMode = TradeConfig.DIRECT;
            for (int i = 0; i < 10; i++) {
                new TradeAction().createQuote("s:" + i, "Company " + i, new BigDecimal(i * 1.1));
            }
            /*
             *
             * AccountDataBean accountData = new TradeAction().register("user1",
             * "password", "fullname", "address", "email", "creditCard", new
             * BigDecimal(123.45), false);
             *
             * OrderDataBean orderData = new TradeAction().buy("user1", "s:1",
             * 100.0); orderData = new TradeAction().buy("user1", "s:2", 200.0);
             * Thread.sleep(5000); accountData = new
             * TradeAction().getAccountData("user1"); Collection
             * holdingDataBeans = new TradeAction().getHoldings("user1");
             * PrintWriter out = resp.getWriter();
             * resp.setContentType("text/html");
             * out.write("<HEAD></HEAD><BODY><BR><BR>");
             * out.write(accountData.toString());
             * Log.printCollection("user1 Holdings", holdingDataBeans);
             * ServletContext sc = getServletContext();
             * req.setAttribute("results", "Success");
             * req.setAttribute("accountData", accountData);
             * req.setAttribute("holdingDataBeans", holdingDataBeans);
             * getServletContext
             * ().getRequestDispatcher("/tradehome.jsp").include(req, resp);
             * out.write("<BR><BR>done.</BODY>");
             */
        } catch (Exception e) {
            Log.error("TestServletException", e);
        }
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * TradeAppServlet provides the standard web interface to Trade and can be
 * accessed with the Go Trade! link. Driving benchmark load using this interface
 * requires a sophisticated web load generator that is capable of filling HTML
 * forms and posting dynamic data.
 */

@WebServlet(name = "TradeAppServlet", urlPatterns = { "/app" })
public class TradeAppServlet extends HttpServlet {

    private static final long serialVersionUID = 481530522846648373L;

    /**
     * Servlet initialization method.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        java.util.Enumeration<String> en = config.getInitParameterNames();
        while (en.hasMoreElements()) {
            String parm = en.nextElement();
            String value = config.getInitParameter(parm);
            TradeConfig.setConfigParam(parm, value);
        }
        try {
            // TODO: Uncomment this once split-tier issue is resolved
            // TradeDirect.init();
        } catch (Exception e) {
            Log.error(e, "TradeAppServlet:init -- Error initializing TradeDirect");
        }
    }

    /**
     * Returns a string that contains information about TradeScenarioServlet
     *
     * @return The servlet information
     */
    @Override
    public java.lang.String getServletInfo() {
        return "TradeAppServlet provides the standard web interface to Trade";
    }

    /**
     * Process incoming HTTP GET requests
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    @Override
    public void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Process incoming HTTP POST requests
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    @Override
    public void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Main service method for TradeAppServlet
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    public void performTask(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = null;
        String userID = null;
        // String to create full dispatch path to TradeAppServlet w/ request
        // Parameters

        resp.setContentType("text/html");
        TradeServletAction tsAction = new TradeServletAction();

        // Dyna - need status string - prepended to output
        action = req.getParameter("action");

        ServletContext ctx = getServletConfig().getServletContext();

        if (action == null) {
            tsAction.doWelcome(ctx, req, resp, "");
            return;
        } else if (action.equals("login")) {
            userID = req.getParameter("uid");
            String passwd = req.getParameter("passwd");
            tsAction.doLogin(ctx, req, resp, userID, passwd);
            return;
        } else if (action.equals("register")) {
            userID = req.getParameter("user id");
            String passwd = req.getParameter("passwd");
            String cpasswd = req.getParameter("confirm passwd");
            String fullname = req.getParameter("Full Name");
            String ccn = req.getParameter("Credit Card Number");
            String money = req.getParameter("money");
            String email = req.getParameter("email");
            String smail = req.getParameter("snail mail");
            tsAction.doRegister(ctx, req, resp, userID, passwd, cpasswd, fullname, ccn, money, email, smail);
            return;
        }

        // The rest of the operations require the user to be logged in -
        // Get the Session and validate the user.
        HttpSession session = req.getSession();
        userID = (String) session.getAttribute("uidBean");

        if (userID == null) {
            System.out.println("TradeAppServlet service error: User Not Logged in");
            tsAction.doWelcome(ctx, req, resp, "User Not Logged in");
            return;
        }
        if (action.equals("quotes")) {
            String symbols = req.getParameter("symbols");
            tsAction.doQuotes(ctx, req, resp, userID, symbols);
        } else if (action.equals("buy")) {
            String symbol = req.getParameter("symbol");
            String quantity = req.getParameter("quantity");
            tsAction.doBuy(ctx, req, resp, userID, symbol, quantity);
        } else if (action.equals("sell")) {
            int holdingID = Integer.parseInt(req.getParameter("holdingID"));
            tsAction.doSell(ctx, req, resp, userID, new Integer(holdingID));
        } else if (action.equals("portfolio") || action.equals("portfolioNoEdge")) {
            tsAction.doPortfolio(ctx, req, resp, userID, "Portfolio as of " + new java.util.Date());
        } else if (action.equals("logout")) {
            tsAction.doLogout(ctx, req, resp, userID);
        } else if (action.equals("home")) {
            tsAction.doHome(ctx, req, resp, userID, "Ready to Trade");
        } else if (action.equals("account")) {
            tsAction.doAccount(ctx, req, resp, userID, "");
        } else if (action.equals("update_profile")) {
            String password = req.getParameter("password");
            String cpassword = req.getParameter("cpassword");
            String fullName = req.getParameter("fullname");
            String address = req.getParameter("address");
            String creditcard = req.getParameter("creditcard");
            String email = req.getParameter("email");
            tsAction.doAccountUpdate(ctx, req, resp, userID, password == null ? "" : password.trim(), cpassword == null ? "" : cpassword.trim(),
                    fullName == null ? "" : fullName.trim(), address == null ? "" : address.trim(), creditcard == null ? "" : creditcard.trim(),
                    email == null ? "" : email.trim());
        } else if (action.equals("mksummary")) {
            tsAction.doMarketSummary(ctx, req, resp, userID);
        } else {
            System.out.println("TradeAppServlet: Invalid Action=" + action);
            tsAction.doWelcome(ctx, req, resp, "TradeAppServlet: Invalid Action" + action);
        }
    }

}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;

import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * TradeBuildDB uses operations provided by the TradeApplication to (a) create the Database tables
 * (b)populate a DayTrader database without creating the tables. Specifically, a
 * new DayTrader User population is created using UserIDs of the form "uid:xxx"
 * where xxx is a sequential number (e.g. uid:0, uid:1, etc.). New stocks are also created of the
 * form "s:xxx", again where xxx represents sequential numbers (e.g. s:1, s:2, etc.)
 */
public class TradeBuildDB {

    /**
     * Populate a Trade DB using standard out as a log
     */
    public TradeBuildDB() throws Exception {
        this(new java.io.PrintWriter(System.out), null);
    }

    /**
     * Re-create the DayTrader db tables and populate them OR just populate a DayTrader DB, logging to the provided output stream
     */
    public TradeBuildDB(java.io.PrintWriter out, InputStream ddlFile) throws Exception {
        String symbol, companyName;
        int errorCount = 0; // Give up gracefully after 10 errors
        
        // Build db in direct mode because it is faster
        TradeDirect tradeDirect = new TradeDirect();

        //  TradeStatistics.statisticsEnabled=false;  // disable statistics
        out.println("<HEAD><BR><EM> TradeBuildDB: Building DayTrader Database...</EM><BR> This operation will take several minutes. Please wait...</HEAD>");
        out.println("<BODY>");

        if (ddlFile != null) {
            //out.println("<BR>TradeBuildDB: **** warPath= "+warPath+" ****</BR></BODY>");

        	boolean success = false;

            Object[] sqlBuffer = null;

            //parse the DDL file and fill the SQL commands into a buffer
            try {
                sqlBuffer = parseDDLToBuffer(ddlFile);
            } catch (Exception e) {
                Log.error(e, "TradeBuildDB: Unable to parse DDL file");
                out.println("<BR>TradeBuildDB: **** Unable to parse DDL file for the specified database ****</BR></BODY>");
                return;
            }
            if ((sqlBuffer == null) || (sqlBuffer.length == 0)) {
                out.println("<BR>TradeBuildDB: **** Parsing DDL file returned empty buffer, please check that a valid DB specific DDL file is available and retry ****</BR></BODY>");
                return;
            }

            // send the sql commands buffer to drop and recreate the Daytrader tables
            out.println("<BR>TradeBuildDB: **** Dropping and Recreating the DayTrader tables... ****</BR>");
            try {
                success = tradeDirect.recreateDBTables(sqlBuffer, out);
            } catch (Exception e) {
                Log.error(e, "TradeBuildDB: Unable to drop and recreate DayTrader Db Tables, please check for database consistency before continuing");
                out.println("TradeBuildDB: Unable to drop and recreate DayTrader Db Tables, please check for database consistency before continuing");
                return;
            }
            if (!success) {
                out.println("<BR>TradeBuildDB: **** Unable to drop and recreate DayTrader Db Tables, please check for database consistency before continuing ****</BR></BODY>");
                return;
            }
            out.println("<BR>TradeBuildDB: **** DayTrader tables successfully created! ****</BR><BR><b> Please Stop and Re-start your Daytrader application (or your application server) and then use the \"Repopulate Daytrader Database\" link to populate your database.</b></BR><BR><BR></BODY>");
            return;
        } // end of createDBTables

        out.println("<BR>TradeBuildDB: **** Creating " + TradeConfig.getMAX_QUOTES() + " Quotes ****</BR>");
        //Attempt to delete all of the Trade users and Trade Quotes first
        try {
            tradeDirect.resetTrade(true);
        } catch (Exception e) {
            Log.error(e, "TradeBuildDB: Unable to delete Trade users (uid:0, uid:1, ...) and Trade Quotes (s:0, s:1, ...)");
        }
        for (int i = 0; i < TradeConfig.getMAX_QUOTES(); i++) {
            symbol = "s:" + i;
            companyName = "S" + i + " Incorporated";
            try {
                tradeDirect.createQuote(symbol, companyName, new java.math.BigDecimal(TradeConfig.rndPrice()));
                if (i % 10 == 0) {
                    out.print("....." + symbol);
                    if (i % 100 == 0) {
                        out.println(" -<BR>");
                        out.flush();
                    }
                }
            } catch (Exception e) {
                if (errorCount++ >= 10) {
                    String error = "Populate Trade DB aborting after 10 create quote errors. Check the EJB datasource configuration. Check the log for details <BR><BR> Exception is: <BR> "
                            + e.toString();
                    Log.error(e, error);
                    throw e;
                }
            }
        }
        out.println("<BR>");
        out.println("<BR>**** Registering " + TradeConfig.getMAX_USERS() + " Users **** ");
        errorCount = 0; //reset for user registrations

        // Registration is a formal operation in Trade 2.
        for (int i = 0; i < TradeConfig.getMAX_USERS(); i++) {
            String userID = "uid:" + i;
            String fullname = TradeConfig.rndFullName();
            String email = TradeConfig.rndEmail(userID);
            String address = TradeConfig.rndAddress();
            String creditcard = TradeConfig.rndCreditCard();
            double initialBalance = (double) (TradeConfig.rndInt(100000)) + 200000;
            if (i == 0) {
                initialBalance = 1000000; // uid:0 starts with a cool million.
            }
            try {
                AccountDataBean accountData = tradeDirect.register(userID, "xxx", fullname, address, email, creditcard, new BigDecimal(initialBalance));

                if (accountData != null) {
                    if (i % 50 == 0) {
                        out.print("<BR>Account# " + accountData.getAccountID() + " userID=" + userID);
                    } // end-if

                    int holdings = TradeConfig.rndInt(TradeConfig.getMAX_HOLDINGS() + 1); // 0-MAX_HOLDING (inclusive), avg holdings per user = (MAX-0)/2
                    double quantity = 0;

                    for (int j = 0; j < holdings; j++) {
                        symbol = TradeConfig.rndSymbol();
                        quantity = TradeConfig.rndQuantity();
                        tradeDirect.buy(userID, symbol, quantity, TradeConfig.orderProcessingMode);
                    } // end-for
                    if (i % 50 == 0) {
                        out.println(" has " + holdings + " holdings.");
                        out.flush();
                    } // end-if
                } else {
                    out.println("<BR>UID " + userID + " already registered.</BR>");
                    out.flush();
                } // end-if

            } catch (Exception e) {
                if (errorCount++ >= 10) {
                    String error = "Populate Trade DB aborting after 10 user registration errors. Check the log for details. <BR><BR> Exception is: <BR>"
                            + e.toString();
                    Log.error(e, error);
                    throw e;
                }
            }
        } // end-for
        out.println("</BODY>");
    }

    public Object[] parseDDLToBuffer(InputStream ddlFile) throws Exception {
        BufferedReader br = null;
        ArrayList<String> sqlBuffer = new ArrayList<String>(30); //initial capacity 30 assuming we have 30 ddl-sql statements to read

        try {
            if (Log.doTrace())
                Log.traceEnter("TradeBuildDB:parseDDLToBuffer - " + ddlFile);

            br = new BufferedReader(new InputStreamReader(ddlFile));
            String s;
            String sql = new String();
            while ((s = br.readLine()) != null) {
                s = s.trim();
                if ((s.length() != 0) && (s.charAt(0) != '#')) // Empty lines or lines starting with "#" are ignored
                {
                    sql = sql + " " + s;
                    if (s.endsWith(";")) { // reached end of sql statement
                        sql = sql.replace(';', ' '); //remove the semicolon
                        sqlBuffer.add(sql);
                        sql = "";
                    }
                }
            }
        } catch (IOException ex) {
            Log.error("TradeBuildDB:parseDDLToBuffer Exeception during open/read of File: " + ddlFile, ex);
            throw ex;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Log.error("TradeBuildDB:parseDDLToBuffer Failed to close BufferedReader", ex);
                }
            }
        }
        return sqlBuffer.toArray();
    }

    public static void main(String[] args) throws Exception {
        new TradeBuildDB();

    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.beans.RunStatsDataBean;
import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * TradeConfigServlet provides a servlet interface to adjust DayTrader runtime parameters.
 * TradeConfigServlet updates values in the {@link com.ibm.websphere.samples.daytrader.web.TradeConfig} JavaBean holding
 * all configuration and runtime parameters for the Trade application
 *
 */
@WebServlet(name = "TradeConfigServlet", urlPatterns = { "/config" })
public class TradeConfigServlet extends HttpServlet {

    private static final long serialVersionUID = -1910381529792500095L;

    /**
     * Servlet initialization method.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Create the TradeConfig bean and pass it the config.jsp page
     * to display the current Trade runtime configuration
     * Creation date: (2/8/2000 3:43:59 PM)
     */
    void doConfigDisplay(HttpServletRequest req, HttpServletResponse resp, String results) throws Exception {

        TradeConfig currentConfig = new TradeConfig();

        req.setAttribute("tradeConfig", currentConfig);
        req.setAttribute("status", results);
        getServletConfig().getServletContext().getRequestDispatcher(TradeConfig.getPage(TradeConfig.CONFIG_PAGE)).include(req, resp);
    }

    void doResetTrade(HttpServletRequest req, HttpServletResponse resp, String results) throws Exception {
        RunStatsDataBean runStatsData = new RunStatsDataBean();
        TradeConfig currentConfig = new TradeConfig();
        try {
            runStatsData = new TradeAction().resetTrade(false);

            req.setAttribute("runStatsData", runStatsData);
            req.setAttribute("tradeConfig", currentConfig);
            results += "Trade Reset completed successfully";
            req.setAttribute("status", results);

        } catch (Exception e) {
            results += "Trade Reset Error  - see log for details";
            Log.error(e, results);
            throw e;
        }
        getServletConfig().getServletContext().getRequestDispatcher(TradeConfig.getPage(TradeConfig.STATS_PAGE)).include(req, resp);

    }

    /**
     * Update Trade runtime configuration paramaters
     * Creation date: (2/8/2000 3:44:24 PM)
     */
    void doConfigUpdate(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String currentConfigStr = "\n\n########## Trade configuration update. Current config:\n\n";
                        
        String runTimeModeStr = req.getParameter("RunTimeMode");
        if (runTimeModeStr != null) {
            try {
                int i = Integer.parseInt(runTimeModeStr);
                if ((i >= 0) && (i < TradeConfig.runTimeModeNames.length)) //Input validation
                    TradeConfig.setRunTimeMode(i);
            } catch (Exception e) {
                //>>rjm
                Log.error(e, "TradeConfigServlet.doConfigUpdate(..): minor exception caught", "trying to set runtimemode to " + runTimeModeStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tRunTimeMode:\t\t\t" + TradeConfig.runTimeModeNames[TradeConfig.runTimeMode] + "\n";
        
        String useRemoteEJBInterface = req.getParameter("UseRemoteEJBInterface");

        if (useRemoteEJBInterface != null)
            TradeConfig.setUseRemoteEJBInterface(true);
        else
            TradeConfig.setDisplayOrderAlerts(false);
        currentConfigStr += "\t\tUse Remote EJB Interface:\t" + TradeConfig.useRemoteEJBInterface() + "\n";

        String orderProcessingModeStr = req.getParameter("OrderProcessingMode");
        if (orderProcessingModeStr != null) {
            try {
                int i = Integer.parseInt(orderProcessingModeStr);
                if ((i >= 0) && (i < TradeConfig.orderProcessingModeNames.length)) //Input validation
                    TradeConfig.setOrderProcessingMode(i);
            } catch (Exception e) {
                //>>rjm
                Log.error(e, "TradeConfigServlet.doConfigUpdate(..): minor exception caught", "trying to set orderProcessing to " + orderProcessingModeStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tOrderProcessingMode:\t\t" + TradeConfig.orderProcessingModeNames[TradeConfig.orderProcessingMode] + "\n";

        String webInterfaceStr = req.getParameter("WebInterface");
        if (webInterfaceStr != null) {
            try {
                int i = Integer.parseInt(webInterfaceStr);
                if ((i >= 0) && (i < TradeConfig.webInterfaceNames.length)) //Input validation
                    TradeConfig.setWebInterface(i);
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet.doConfigUpdate(..): minor exception caught", "trying to set WebInterface to " + webInterfaceStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tWeb Interface:\t\t\t" + TradeConfig.webInterfaceNames[TradeConfig.webInterface] + "\n";

       /* String cachingTypeStr = req.getParameter("CachingType");
        if (cachingTypeStr != null) {
            try {
                int i = Integer.parseInt(cachingTypeStr);
                if ((i >= 0) && (i < TradeConfig.cachingTypeNames.length)) //Input validation
                    TradeConfig.setCachingType(i);
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet.doConfigUpdate(..): minor exception caught", "trying to set CachingType to " + cachingTypeStr,
                        "reverting to current value");
            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tCachingType:\t\t\t" + TradeConfig.cachingTypeNames[TradeConfig.cachingType] + "\n";

        String distMapCacheSize = req.getParameter("DistMapCacheSize");
        if ((distMapCacheSize != null) && (distMapCacheSize.length() > 0)) {
            try {
                TradeConfig.setDistributedMapCacheSize(Integer.parseInt(distMapCacheSize));
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet: minor exception caught", "trying to set DistributedMapCacheSize, error on parsing int " + distMapCacheSize,
                        "reverting to current value " + TradeConfig.getPrimIterations());

            }
        }
        currentConfigStr += "\t\tDMap Cache Size:\t\t" + TradeConfig.getDistributedMapCacheSize() + "\n";
		*/
        String parm = req.getParameter("MaxUsers");
        if ((parm != null) && (parm.length() > 0)) {
            try {
                TradeConfig.setMAX_USERS(Integer.parseInt(parm));
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet.doConfigUpdate(..): minor exception caught", "Setting maxusers, probably error parsing string to int:" + parm,
                        "revertying to current value: " + TradeConfig.getMAX_USERS());

            } //On error, revert to saved
        }
        parm = req.getParameter("MaxQuotes");
        if ((parm != null) && (parm.length() > 0)) {
            try {
                TradeConfig.setMAX_QUOTES(Integer.parseInt(parm));
            } catch (Exception e) {
                //>>rjm
                Log.error(e, "TradeConfigServlet: minor exception caught", "trying to set max_quotes, error on parsing int " + parm,
                        "reverting to current value " + TradeConfig.getMAX_QUOTES());
                //<<rjm

            } //On error, revert to saved
        }
        currentConfigStr += "\t\tTrade Users:\t\t\t" + TradeConfig.getMAX_USERS() + "\n";
        currentConfigStr += "\t\tTrade Quotes:\t\t\t" + TradeConfig.getMAX_QUOTES() + "\n";

        parm = req.getParameter("marketSummaryInterval");
        if ((parm != null) && (parm.length() > 0)) {
            try {
                TradeConfig.setMarketSummaryInterval(Integer.parseInt(parm));
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet: minor exception caught", "trying to set marketSummaryInterval, error on parsing int " + parm,
                        "reverting to current value " + TradeConfig.getMarketSummaryInterval());

            }
        }
        currentConfigStr += "\t\tMarket Summary Interval:\t" + TradeConfig.getMarketSummaryInterval() + "\n";

        parm = req.getParameter("primIterations");
        if ((parm != null) && (parm.length() > 0)) {
            try {
                TradeConfig.setPrimIterations(Integer.parseInt(parm));
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet: minor exception caught", "trying to set primIterations, error on parsing int " + parm,
                        "reverting to current value " + TradeConfig.getPrimIterations());

            }
        }
        currentConfigStr += "\t\tPrimitive Iterations:\t\t" + TradeConfig.getPrimIterations() + "\n";

        String enablePublishQuotePriceChange = req.getParameter("EnablePublishQuotePriceChange");

        if (enablePublishQuotePriceChange != null)
            TradeConfig.setPublishQuotePriceChange(true);
        else
            TradeConfig.setPublishQuotePriceChange(false);
        currentConfigStr += "\t\tTradeStreamer MDB Enabled:\t" + TradeConfig.getPublishQuotePriceChange() + "\n";

        parm = req.getParameter("percentSentToWebsocket");
        if ((parm != null) && (parm.length() > 0)) {
            try {
                TradeConfig.setPercentSentToWebsocket(Integer.parseInt(parm));
            } catch (Exception e) {
                Log.error(e, "TradeConfigServlet: minor exception caught", "trying to set percentSentToWebSocket, error on parsing int " + parm,
                        "reverting to current value " + TradeConfig.getPercentSentToWebsocket());

            }
        }
        currentConfigStr += "\t\t% of trades on Websocket:\t" + TradeConfig.getPercentSentToWebsocket() + "\n";
        
        String enableLongRun = req.getParameter("EnableLongRun");

        if (enableLongRun != null)
            TradeConfig.setLongRun(true);
        else
            TradeConfig.setLongRun(false);
        currentConfigStr += "\t\tLong Run Enabled:\t\t" + TradeConfig.getLongRun() + "\n";

        String displayOrderAlerts = req.getParameter("DisplayOrderAlerts");

        if (displayOrderAlerts != null)
            TradeConfig.setDisplayOrderAlerts(true);
        else
            TradeConfig.setDisplayOrderAlerts(false);
        currentConfigStr += "\t\tDisplay Order Alerts:\t\t" + TradeConfig.getDisplayOrderAlerts() + "\n";
                
        String enableTrace = req.getParameter("EnableTrace");
        if (enableTrace != null)
            Log.setTrace(true);
        else
            Log.setTrace(false);
        currentConfigStr += "\t\tTrace Enabled:\t\t\t" + TradeConfig.getTrace() + "\n";

        String enableActionTrace = req.getParameter("EnableActionTrace");
        if (enableActionTrace != null)
            Log.setActionTrace(true);
        else
            Log.setActionTrace(false);
        currentConfigStr += "\t\tAction Trace Enabled:\t\t" + TradeConfig.getActionTrace() + "\n";

        System.out.println(currentConfigStr);
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String action = null;
        String result = "";

        resp.setContentType("text/html");
        try {
            action = req.getParameter("action");
            if (action == null) {
                doConfigDisplay(req, resp, result + "<b><br>Current DayTrader Configuration:</br></b>");
                return;
            } else if (action.equals("updateConfig")) {
                doConfigUpdate(req, resp);
                result = "<B><BR>DayTrader Configuration Updated</BR></B>";
            } else if (action.equals("resetTrade")) {
                doResetTrade(req, resp, "");
                return;
            } else if (action.equals("buildDB")) {
                resp.setContentType("text/html");
                new TradeBuildDB(resp.getWriter(), null);
                result = "DayTrader Database Built - " + TradeConfig.getMAX_USERS() + "users created";
            } else if (action.equals("buildDBTables")) {

                resp.setContentType("text/html");

                //Find out the Database being used
                TradeDirect tradeDirect = new TradeDirect();

                String dbProductName = null;
                try {
                    dbProductName = tradeDirect.checkDBProductName();
                } catch (Exception e) {
                    Log.error(e, "TradeBuildDB: Unable to check DB Product name");
                }
                if (dbProductName == null) {
                    resp.getWriter().println(
                            "<BR>TradeBuildDB: **** Unable to check DB Product name, please check Database/AppServer configuration and retry ****</BR></BODY>");
                    return;
                }

                String ddlFile = null;
                //Locate DDL file for the specified database
                try {
                    resp.getWriter().println("<BR>TradeBuildDB: **** Database Product detected: " + dbProductName + " ****</BR>");
                    if (dbProductName.startsWith("DB2/")) {// if db is DB2
                        ddlFile = "/dbscripts/db2/Table.ddl";
                    } else if (dbProductName.startsWith("DB2 UDB for AS/400")) { //if db is DB2 on IBM i
                        ddlFile = "/dbscripts/db2i/Table.ddl";
                    }  else if (dbProductName.startsWith("Apache Derby")) { //if db is Derby
                        ddlFile = "/dbscripts/derby/Table.ddl";
                    } else if (dbProductName.startsWith("Oracle")) { // if the Db is Oracle
                        ddlFile = "/dbscripts/oracle/Table.ddl";
                    } else {// Unsupported "Other" Database, try derby ddl
                        ddlFile = "/dbscripts/derby/Table.ddl";
                        resp.getWriter().println("<BR>TradeBuildDB: **** This Database is unsupported/untested use at your own risk ****</BR>");
                    }

                    resp.getWriter().println("<BR>TradeBuildDB: **** The DDL file at path <I>" + ddlFile + "</I> will be used ****</BR>");
                    resp.getWriter().flush();
                } catch (Exception e) {
                    Log.error(e, "TradeBuildDB: Unable to locate DDL file for the specified database");
                    resp.getWriter().println("<BR>TradeBuildDB: **** Unable to locate DDL file for the specified database ****</BR></BODY>");
                    return;
                }
                new TradeBuildDB(resp.getWriter(), getServletContext().getResourceAsStream(ddlFile));

            }
            doConfigDisplay(req, resp, result + "Current DayTrader Configuration:");
        } catch (Exception e) {
            Log.error(e, "TradeConfigServlet.service(...)", "Exception trying to perform action=" + action);

            resp.sendError(500, "TradeConfigServlet.service(...)" + "Exception trying to perform action=" + action + "\nException details: " + e.toString());

        }
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * TradeScenarioServlet emulates a population of web users by generating a
 * specific Trade operation for a randomly chosen user on each access to the
 * URL. Test this servlet by clicking Trade Scenario and hit "Reload" on your
 * browser to step through a Trade Scenario. To benchmark using this URL aim
 * your favorite web load generator (such as AKStress) at the Trade Scenario URL
 * and fire away.
 */
@WebServlet(name = "TradeScenarioServlet", urlPatterns = { "/scenario" })
public class TradeScenarioServlet extends HttpServlet {

    private static final long serialVersionUID = 1410005249314201829L;

    /**
     * Servlet initialization method.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        java.util.Enumeration<String> en = config.getInitParameterNames();
        while (en.hasMoreElements()) {
            String parm = en.nextElement();
            String value = config.getInitParameter(parm);
            TradeConfig.setConfigParam(parm, value);
        }
    }

    /**
     * Returns a string that contains information about TradeScenarioServlet
     *
     * @return The servlet information
     */
    @Override
    public java.lang.String getServletInfo() {
        return "TradeScenarioServlet emulates a population of web users";
    }

    /**
     * Process incoming HTTP GET requests
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    @Override
    public void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Process incoming HTTP POST requests
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    @Override
    public void doPost(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        performTask(request, response);
    }

    /**
     * Main service method for TradeScenarioServlet
     *
     * @param request
     *            Object that encapsulates the request to the servlet
     * @param response
     *            Object that encapsulates the response from the servlet
     */
    public void performTask(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Scenario generator for Trade2
        char action = ' ';
        String userID = null;

        // String to create full dispatch path to TradeAppServlet w/ request
        // Parameters
        String dispPath = null; // Dispatch Path to TradeAppServlet

        resp.setContentType("text/html");

        String scenarioAction = req.getParameter("action");
        if ((scenarioAction != null) && (scenarioAction.length() >= 1)) {
            action = scenarioAction.charAt(0);
            if (action == 'n') { // null;
                try {
                    // resp.setContentType("text/html");
                    PrintWriter out = new PrintWriter(resp.getOutputStream());
                    out.println("<HTML><HEAD>TradeScenarioServlet</HEAD><BODY>Hello</BODY></HTML>");
                    out.close();
                    return;

                } catch (Exception e) {
                    Log.error("trade_client.TradeScenarioServlet.service(...)" + "error creating printwriter from responce.getOutputStream", e);

                    resp.sendError(500,
                            "trade_client.TradeScenarioServlet.service(...): erorr creating and writing to PrintStream created from response.getOutputStream()");
                } // end of catch

            } // end of action=='n'
        }

        ServletContext ctx = null;
        HttpSession session = null;
        try {
            ctx = getServletConfig().getServletContext();
            // These operations require the user to be logged in. Verify the
            // user and if not logged in
            // change the operation to a login
            session = req.getSession(true);
            userID = (String) session.getAttribute("uidBean");
        } catch (Exception e) {
            Log.error("trade_client.TradeScenarioServlet.service(...): performing " + scenarioAction
                    + "error getting ServletContext,HttpSession, or UserID from session" + "will make scenarioAction a login and try to recover from there", e);
            userID = null;
            action = 'l';
        }

        if (userID == null) {
            action = 'l'; // change to login
            TradeConfig.incrementScenarioCount();
        } else if (action == ' ') {
            // action is not specified perform a random operation according to
            // current mix
            // Tell getScenarioAction if we are an original user or a registered
            // user
            // -- sellDeficits should only be compensated for with original
            // users.
            action = TradeConfig.getScenarioAction(userID.startsWith(TradeConfig.newUserPrefix));
        }
        switch (action) {

        case 'q': // quote
            dispPath = tasPathPrefix + "quotes&symbols=" + TradeConfig.rndSymbols();
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 'a': // account
            dispPath = tasPathPrefix + "account";
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 'u': // update account profile
            dispPath = tasPathPrefix + "account";
            ctx.getRequestDispatcher(dispPath).include(req, resp);

            String fullName = "rnd" + System.currentTimeMillis();
            String address = "rndAddress";
            String password = "xxx";
            String email = "rndEmail";
            String creditcard = "rndCC";
            dispPath = tasPathPrefix + "update_profile&fullname=" + fullName + "&password=" + password + "&cpassword=" + password + "&address=" + address
                    + "&email=" + email + "&creditcard=" + creditcard;
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 'h': // home
            dispPath = tasPathPrefix + "home";
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 'l': // login
            userID = TradeConfig.getUserID();
            String password2 = "xxx";
            dispPath = tasPathPrefix + "login&inScenario=true&uid=" + userID + "&passwd=" + password2;
            ctx.getRequestDispatcher(dispPath).include(req, resp);

            // login is successful if the userID is written to the HTTP session
            if (session.getAttribute("uidBean") == null) {
                System.out.println("TradeScenario login failed. Reset DB between runs");
            }
            break;
        case 'o': // logout
            dispPath = tasPathPrefix + "logout";
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 'p': // portfolio
            dispPath = tasPathPrefix + "portfolio";
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 'r': // register
            // Logout the current user to become a new user
            // see note in TradeServletAction
            req.setAttribute("TSS-RecreateSessionInLogout", Boolean.TRUE);
            dispPath = tasPathPrefix + "logout";
            ctx.getRequestDispatcher(dispPath).include(req, resp);

            userID = TradeConfig.rndNewUserID();
            String passwd = "yyy";
            fullName = TradeConfig.rndFullName();
            creditcard = TradeConfig.rndCreditCard();
            String money = TradeConfig.rndBalance();
            email = TradeConfig.rndEmail(userID);
            String smail = TradeConfig.rndAddress();
            dispPath = tasPathPrefix + "register&Full Name=" + fullName + "&snail mail=" + smail + "&email=" + email + "&user id=" + userID + "&passwd="
                    + passwd + "&confirm passwd=" + passwd + "&money=" + money + "&Credit Card Number=" + creditcard;
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        case 's': // sell
            dispPath = tasPathPrefix + "portfolioNoEdge";
            ctx.getRequestDispatcher(dispPath).include(req, resp);

            Collection<?> holdings = (Collection<?>) req.getAttribute("holdingDataBeans");
            int numHoldings = holdings.size();
            if (numHoldings > 0) {
                // sell first available security out of holding

                Iterator<?> it = holdings.iterator();
                boolean foundHoldingToSell = false;
                while (it.hasNext()) {
                    HoldingDataBean holdingData = (HoldingDataBean) it.next();
                    if (!(holdingData.getPurchaseDate().equals(new java.util.Date(0)))) {
                        Integer holdingID = holdingData.getHoldingID();

                        dispPath = tasPathPrefix + "sell&holdingID=" + holdingID;
                        ctx.getRequestDispatcher(dispPath).include(req, resp);
                        foundHoldingToSell = true;
                        break;
                    }
                }
                if (foundHoldingToSell) {
                    break;
                }
                if (Log.doTrace()) {
                    Log.trace("TradeScenario: No holding to sell -switch to buy -- userID = " + userID + "  Collection count = " + numHoldings);
                }

            }
            // At this point: A TradeScenario Sell was requested with No Stocks
            // in Portfolio
            // This can happen when a new registered user happens to request a
            // sell before a buy
            // In this case, fall through and perform a buy instead

            /*
             * Trade 2.037: Added sell_deficit counter to maintain correct
             * buy/sell mix. When a users portfolio is reduced to 0 holdings, a
             * buy is requested instead of a sell. This throws off the buy/sell
             * mix by 1. This results in unwanted holding table growth To fix
             * this we increment a sell deficit counter to maintain the correct
             * ratio in getScenarioAction The 'z' action from getScenario
             * denotes that this is a sell action that was switched from a buy
             * to reduce a sellDeficit
             */
            if (userID.startsWith(TradeConfig.newUserPrefix) == false) {
                TradeConfig.incrementSellDeficit();
            }
        case 'b': // buy
            String symbol = TradeConfig.rndSymbol();
            String amount = TradeConfig.rndQuantity() + "";

            dispPath = tasPathPrefix + "quotes&symbols=" + symbol;
            ctx.getRequestDispatcher(dispPath).include(req, resp);

            dispPath = tasPathPrefix + "buy&quantity=" + amount + "&symbol=" + symbol;
            ctx.getRequestDispatcher(dispPath).include(req, resp);
            break;
        } // end of switch statement
    }

    // URL Path Prefix for dispatching to TradeAppServlet
    private static final String tasPathPrefix = "/app?action=";

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.TradeServices;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * TradeServletAction provides servlet specific client side access to each of
 * the Trade brokerage user operations. These include login, logout, buy, sell,
 * getQuote, etc. TradeServletAction manages a web interface to Trade handling
 * HttpRequests/HttpResponse objects and forwarding results to the appropriate
 * JSP page for the web interface. TradeServletAction invokes
 * {@link TradeAction} methods to actually perform each trading operation.
 *
 */
public class TradeServletAction {

    private TradeServices tAction = null;

    TradeServletAction() {
        tAction = new TradeAction();
    }

    /**
     * Display User Profile information such as address, email, etc. for the
     * given Trader Dispatch to the Trade Account JSP for display
     *
     * @param userID
     *            The User to display profile info
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @param results
     *            A short description of the results/success of this web request
     *            provided on the web page
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doAccount(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String results) throws javax.servlet.ServletException,
            java.io.IOException {
        try {

            AccountDataBean accountData = tAction.getAccountData(userID);
            AccountProfileDataBean accountProfileData = tAction.getAccountProfileData(userID);
            Collection<?> orderDataBeans = (TradeConfig.getLongRun() ? new ArrayList<Object>() : (Collection<?>) tAction.getOrders(userID));

            req.setAttribute("accountData", accountData);
            req.setAttribute("accountProfileData", accountProfileData);
            req.setAttribute("orderDataBeans", orderDataBeans);
            req.setAttribute("results", results);
            requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.ACCOUNT_PAGE));
        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "could not find account for userID = " + userID);
            requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.HOME_PAGE));
            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error("TradeServletAction.doAccount(...)", "illegal argument, information should be in exception string", e);
        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doAccount(...)" + " exception user =" + userID, e);
        }

    }

    /**
     * Update User Profile information such as address, email, etc. for the
     * given Trader Dispatch to the Trade Account JSP for display If any in put
     * is incorrect revert back to the account page w/ an appropriate message
     *
     * @param userID
     *            The User to upddate profile info
     * @param password
     *            The new User password
     * @param cpassword
     *            Confirm password
     * @param fullname
     *            The new User fullname info
     * @param address
     *            The new User address info
     * @param cc
     *            The new User credit card info
     * @param email
     *            The new User email info
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doAccountUpdate(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String password, String cpassword,
            String fullName, String address, String creditcard, String email) throws javax.servlet.ServletException, java.io.IOException {
        String results = "";

        // First verify input data
        boolean doUpdate = true;
        if (password.equals(cpassword) == false) {
            results = "Update profile error: passwords do not match";
            doUpdate = false;
        } else if (password.length() <= 0 || fullName.length() <= 0 || address.length() <= 0 || creditcard.length() <= 0 || email.length() <= 0) {
            results = "Update profile error: please fill in all profile information fields";
            doUpdate = false;
        }
        AccountProfileDataBean accountProfileData = new AccountProfileDataBean(userID, password, fullName, address, email, creditcard);
        try {
            if (doUpdate) {
                accountProfileData = tAction.updateAccountProfile(accountProfileData);
                results = "Account profile update successful";
            }

        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "invalid argument, check userID is correct, and the database is populated" + userID);
            Log.error(e, "TradeServletAction.doAccount(...)", "illegal argument, information should be in exception string",
                    "treating this as a user error and forwarding on to a new page");
        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doAccountUpdate(...)" + " exception user =" + userID, e);
        }
        doAccount(ctx, req, resp, userID, results);
    }

    /**
     * Buy a new holding of shares for the given trader Dispatch to the Trade
     * Portfolio JSP for display
     *
     * @param userID
     *            The User buying shares
     * @param symbol
     *            The stock to purchase
     * @param amount
     *            The quantity of shares to purchase
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doBuy(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String symbol, String quantity) throws ServletException,
            IOException {

        String results = "";

        try {
            OrderDataBean orderData = tAction.buy(userID, symbol, new Double(quantity).doubleValue(), TradeConfig.orderProcessingMode);

            req.setAttribute("orderData", orderData);
            req.setAttribute("results", results);
        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "illegal argument:");
            requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.HOME_PAGE));
            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error(e, "TradeServletAction.doBuy(...)", "illegal argument. userID = " + userID, "symbol = " + symbol);
        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.buy(...)" + " exception buying stock " + symbol + " for user " + userID, e);
        }
        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.ORDER_PAGE));
    }

    /**
     * Create the Trade Home page with personalized information such as the
     * traders account balance Dispatch to the Trade Home JSP for display
     *
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @param results
     *            A short description of the results/success of this web request
     *            provided on the web page
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doHome(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String results) throws javax.servlet.ServletException,
            java.io.IOException {

        try {
            AccountDataBean accountData = tAction.getAccountData(userID);
            Collection<?> holdingDataBeans = tAction.getHoldings(userID);

            // Edge Caching:
            // Getting the MarketSummary has been moved to the JSP
            // MarketSummary.jsp. This makes the MarketSummary a
            // standalone "fragment", and thus is a candidate for
            // Edge caching.
            // marketSummaryData = tAction.getMarketSummary();

            req.setAttribute("accountData", accountData);
            req.setAttribute("holdingDataBeans", holdingDataBeans);
            // See Edge Caching above
            // req.setAttribute("marketSummaryData", marketSummaryData);
            req.setAttribute("results", results);
        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "check userID = " + userID + " and that the database is populated");
            requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.HOME_PAGE));
            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error("TradeServletAction.doHome(...)" + "illegal argument, information should be in exception string"
                    + "treating this as a user error and forwarding on to a new page", e);
        } catch (javax.ejb.FinderException e) {
            // this is a user error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "\nCould not find account for + " + userID);
            // requestDispatch(ctx, req, resp,
            // TradeConfig.getPage(TradeConfig.HOME_PAGE));
            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error("TradeServletAction.doHome(...)" + "Error finding account for user " + userID
                    + "treating this as a user error and forwarding on to a new page", e);
        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doHome(...)" + " exception user =" + userID, e);
        }

        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.HOME_PAGE));
    }

    /**
     * Login a Trade User. Dispatch to the Trade Home JSP for display
     *
     * @param userID
     *            The User to login
     * @param passwd
     *            The password supplied by the trader used to authenticate
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @param results
     *            A short description of the results/success of this web request
     *            provided on the web page
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doLogin(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String passwd) throws javax.servlet.ServletException,
            java.io.IOException {

        String results = "";
        try {
            // Got a valid userID and passwd, attempt login

            AccountDataBean accountData = tAction.login(userID, passwd);

            if (accountData != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("uidBean", userID);
                session.setAttribute("sessionCreationDate", new java.util.Date());

                results = "Ready to Trade";
                doHome(ctx, req, resp, userID, results);
                return;
            } else {
                req.setAttribute("results", results + "\nCould not find account for + " + userID);
                // log the exception with an error level of 3 which means,
                // handled exception but would invalidate a automation run
                Log.log("TradeServletAction.doLogin(...)", "Error finding account for user " + userID + "",
                        "user entered a bad username or the database is not populated");
            }
        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "illegal argument:" + e.getMessage());
            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error(e, "TradeServletAction.doLogin(...)", "illegal argument, information should be in exception string",
                    "treating this as a user error and forwarding on to a new page");

        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doLogin(...)" + "Exception logging in user " + userID + "with password" + passwd, e);
        }

        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.WELCOME_PAGE));

    }

    /**
     * Logout a Trade User Dispatch to the Trade Welcome JSP for display
     *
     * @param userID
     *            The User to logout
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @param results
     *            A short description of the results/success of this web request
     *            provided on the web page
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doLogout(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID) throws ServletException, IOException {
        String results = "";

        try {
            tAction.logout(userID);

        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page, at the end of the page.
            req.setAttribute("results", results + "illegal argument:" + e.getMessage());

            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error(e, "TradeServletAction.doLogout(...)", "illegal argument, information should be in exception string",
                    "treating this as a user error and forwarding on to a new page");
        } catch (Exception e) {
            // log the exception and foward to a error page
            Log.error(e, "TradeServletAction.doLogout(...):", "Error logging out" + userID, "fowarding to an error page");
            // set the status_code to 500
            throw new ServletException("TradeServletAction.doLogout(...)" + "exception logging out user " + userID, e);
        }
        HttpSession session = req.getSession();
        if (session != null) {
            session.invalidate();
        }
        
        // Added to actually remove a user from the authentication cache
        req.logout();

        Object o = req.getAttribute("TSS-RecreateSessionInLogout");
        if (o != null && ((Boolean) o).equals(Boolean.TRUE)) {
            // Recreate Session object before writing output to the response
            // Once the response headers are written back to the client the
            // opportunity
            // to create a new session in this request may be lost
            // This is to handle only the TradeScenarioServlet case
            session = req.getSession(true);
        }
        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.WELCOME_PAGE));
    }

    /**
     * Retrieve the current portfolio of stock holdings for the given trader
     * Dispatch to the Trade Portfolio JSP for display
     *
     * @param userID
     *            The User requesting to view their portfolio
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @param results
     *            A short description of the results/success of this web request
     *            provided on the web page
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doPortfolio(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String results) throws ServletException, IOException {

        try {
            // Get the holdiings for this user

            Collection<QuoteDataBean> quoteDataBeans = new ArrayList<QuoteDataBean>();
            Collection<?> holdingDataBeans = tAction.getHoldings(userID);

            // Walk through the collection of user
            // holdings and creating a list of quotes
            if (holdingDataBeans.size() > 0) {

                Iterator<?> it = holdingDataBeans.iterator();
                while (it.hasNext()) {
                    HoldingDataBean holdingData = (HoldingDataBean) it.next();
                    QuoteDataBean quoteData = tAction.getQuote(holdingData.getQuoteID());
                    quoteDataBeans.add(quoteData);
                }
            } else {
                results = results + ".  Your portfolio is empty.";
            }
            req.setAttribute("results", results);
            req.setAttribute("holdingDataBeans", holdingDataBeans);
            req.setAttribute("quoteDataBeans", quoteDataBeans);
            requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.PORTFOLIO_PAGE));
        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // forward them to another page rather than throw a 500
            req.setAttribute("results", results + "illegal argument:" + e.getMessage());
            requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.PORTFOLIO_PAGE));
            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error(e, "TradeServletAction.doPortfolio(...)", "illegal argument, information should be in exception string", "user error");
        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doPortfolio(...)" + " exception user =" + userID, e);
        }
    }

    /**
     * Retrieve the current Quote for the given stock symbol Dispatch to the
     * Trade Quote JSP for display
     *
     * @param userID
     *            The stock symbol used to get the current quote
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doQuotes(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String symbols) throws ServletException, IOException {

        // Edge Caching:
        // Getting Quotes has been moved to the JSP
        // Quote.jsp. This makes each Quote a
        // standalone "fragment", and thus is a candidate for
        // Edge caching.
        //

        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.QUOTE_PAGE));
    }

    /**
     * Register a new trader given the provided user Profile information such as
     * address, email, etc. Dispatch to the Trade Home JSP for display
     *
     * @param userID
     *            The User to create
     * @param passwd
     *            The User password
     * @param fullname
     *            The new User fullname info
     * @param ccn
     *            The new User credit card info
     * @param money
     *            The new User opening account balance
     * @param address
     *            The new User address info
     * @param email
     *            The new User email info
     * @return The userID of the new trader
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doRegister(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String passwd, String cpasswd, String fullname,
            String ccn, String openBalanceString, String email, String address) throws ServletException, IOException {
        String results = "";

        try {
            // Validate user passwords match and are atleast 1 char in length
            if ((passwd.equals(cpasswd)) && (passwd.length() >= 1)) {

                AccountDataBean accountData = tAction.register(userID, passwd, fullname, address, email, ccn, new BigDecimal(openBalanceString));
                if (accountData == null) {
                    results = "Registration operation failed;";
                    System.out.println(results);
                    req.setAttribute("results", results);
                    requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.REGISTER_PAGE));
                } else {
                    doLogin(ctx, req, resp, userID, passwd);
                    results = "Registration operation succeeded;  Account " + accountData.getAccountID() + " has been created.";
                    req.setAttribute("results", results);

                }
            } else {
                // Password validation failed
                results = "Registration operation failed, your passwords did not match";
                System.out.println(results);
                req.setAttribute("results", results);
                requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.REGISTER_PAGE));
            }

        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doRegister(...)" + " exception user =" + userID, e);
        }
    }

    /**
     * Sell a current holding of stock shares for the given trader. Dispatch to
     * the Trade Portfolio JSP for display
     *
     * @param userID
     *            The User buying shares
     * @param symbol
     *            The stock to sell
     * @param indx
     *            The unique index identifying the users holding to sell
     * @param ctx
     *            the servlet context
     * @param req
     *            the HttpRequest object
     * @param resp
     *            the HttpResponse object
     * @exception javax.servlet.ServletException
     *                If a servlet specific exception is encountered
     * @exception javax.io.IOException
     *                If an exception occurs while writing results back to the
     *                user
     *
     */
    void doSell(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, Integer holdingID) throws ServletException, IOException {
        String results = "";
        try {
            OrderDataBean orderData = tAction.sell(userID, holdingID, TradeConfig.orderProcessingMode);

            req.setAttribute("orderData", orderData);
            req.setAttribute("results", results);
        } catch (java.lang.IllegalArgumentException e) { // this is a user
            // error so I will
            // just log the exception and then later on I will redisplay the
            // portfolio page
            // because this is just a user exception
            Log.error(e, "TradeServletAction.doSell(...)", "illegal argument, information should be in exception string", "user error");
        } catch (Exception e) {
            // log the exception with error page
            throw new ServletException("TradeServletAction.doSell(...)" + " exception selling holding " + holdingID + " for user =" + userID, e);
        }
        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.ORDER_PAGE));
    }

    void doWelcome(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String status) throws ServletException, IOException {

        req.setAttribute("results", status);
        requestDispatch(ctx, req, resp, null, TradeConfig.getPage(TradeConfig.WELCOME_PAGE));
    }

    private void requestDispatch(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID, String page) throws ServletException,
            IOException {

        ctx.getRequestDispatcher(page).include(req, resp);
    }

    void doMarketSummary(ServletContext ctx, HttpServletRequest req, HttpServletResponse resp, String userID) throws ServletException, IOException {
        req.setAttribute("results", "test");
        requestDispatch(ctx, req, resp, userID, TradeConfig.getPage(TradeConfig.MARKET_SUMMARY_PAGE));

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@WebListener()
public class TradeWebContextListener implements ServletContextListener {

    // receieve trade web app startup/shutown events to start(initialized)/stop
    // TradeDirect
    @Override
    public void contextInitialized(ServletContextEvent event) {
        Log.trace("TradeWebContextListener contextInitialized -- initializing TradeDirect");
        
        // Load settings from properties file (if it exists)
        Properties prop = new Properties();
        InputStream stream =  event.getServletContext().getResourceAsStream("/properties/daytrader.properties");
        
        try {
            prop.load(stream);
            System.out.println("Settings from daytrader.properties: " + prop);
            TradeConfig.setRunTimeMode(Integer.parseInt(prop.getProperty("runtimeMode")));
            TradeConfig.setUseRemoteEJBInterface(Boolean.parseBoolean(prop.getProperty("useRemoteEJBInterface")));
            TradeConfig.setOrderProcessingMode(Integer.parseInt(prop.getProperty("orderProcessingMode")));
            TradeConfig.setWebInterface(Integer.parseInt(prop.getProperty("webInterface")));
            //TradeConfig.setCachingType(Integer.parseInt(prop.getProperty("cachingType")));
            //TradeConfig.setDistributedMapCacheSize(Integer.parseInt(prop.getProperty("cacheSize")));
            TradeConfig.setMAX_USERS(Integer.parseInt(prop.getProperty("maxUsers")));
            TradeConfig.setMAX_QUOTES(Integer.parseInt(prop.getProperty("maxQuotes")));
            TradeConfig.setMarketSummaryInterval(Integer.parseInt(prop.getProperty("marketSummaryInterval")));
            TradeConfig.setPrimIterations(Integer.parseInt(prop.getProperty("primIterations")));
            TradeConfig.setPublishQuotePriceChange(Boolean.parseBoolean(prop.getProperty("publishQuotePriceChange")));
            TradeConfig.setPercentSentToWebsocket(Integer.parseInt(prop.getProperty("percentSentToWebsocket")));
            TradeConfig.setDisplayOrderAlerts(Boolean.parseBoolean(prop.getProperty("displayOrderAlerts")));
            TradeConfig.setLongRun(Boolean.parseBoolean(prop.getProperty("longRun")));
            TradeConfig.setTrace(Boolean.parseBoolean(prop.getProperty("trace")));
            TradeConfig.setActionTrace(Boolean.parseBoolean(prop.getProperty("actionTrace")));
        } catch (Exception e) {
            System.out.println("daytrader.properties not found");
        }
        
        TradeDirect.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Log.trace("TradeWebContextListener  contextDestroy calling TradeDirect:destroy()");
        // TradeDirect.destroy();
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Named("accountdata")
@RequestScoped
public class AccountDataJSF {
    @Inject
    private ExternalContext facesExternalContext;

    @Inject
    private TradeAction tradeAction;

    private Date sessionCreationDate;
    private Date currentTime;
    private String profileID;
    private Integer accountID;
    private Date creationDate;
    private int loginCount;
    private Date lastLogin;
    private int logoutCount;
    private BigDecimal balance;
    private BigDecimal openBalance;
    private Integer numberHoldings;
    private BigDecimal holdingsTotal;
    private BigDecimal sumOfCashHoldings;
    private BigDecimal gain;
    private BigDecimal gainPercent;

    private OrderData[] closedOrders;
    private OrderData[] allOrders;  
    
    private Integer numberOfOrders = 0;
	private Integer numberOfOrderRows = 5;
	    
	public void toggleShowAllRows() {
		setNumberOfOrderRows(0);
	}
	
    @PostConstruct
    public void home() {

        try {
            HttpSession session = (HttpSession) facesExternalContext.getSession(true);

            // Get the data and then parse
            String userID = (String) session.getAttribute("uidBean");
            AccountDataBean accountData = tradeAction.getAccountData(userID);
            Collection<?> holdingDataBeans = tradeAction.getHoldings(userID); 
                      
            if (TradeConfig.getDisplayOrderAlerts()) {

                Collection<?> closedOrders = tradeAction.getClosedOrders(userID);

                if (closedOrders != null && closedOrders.size() > 0) {
                    session.setAttribute("closedOrders", closedOrders);
                    OrderData[] orderjsfs = new OrderData[closedOrders.size()];
                    Iterator<?> it = closedOrders.iterator();
                    int i = 0;

                    while (it.hasNext()) {
                        OrderDataBean order = (OrderDataBean) it.next();
                        OrderData r = new OrderData(order.getOrderID(), order.getOrderStatus(), order.getOpenDate(), order.getCompletionDate(),
                                order.getOrderFee(), order.getOrderType(), order.getQuantity(), order.getSymbol());
                        orderjsfs[i] = r;
                        i++;
                    }

                    setClosedOrders(orderjsfs);
                }
            }

            Collection<?> orderDataBeans = (TradeConfig.getLongRun() ? new ArrayList<Object>() : (Collection<?>) tradeAction.getOrders(userID));

            if (orderDataBeans != null && orderDataBeans.size() > 0) {
                session.setAttribute("orderDataBeans", orderDataBeans);
                OrderData[] orderjsfs = new OrderData[orderDataBeans.size()];
                Iterator<?> it = orderDataBeans.iterator();
                int i = 0;

                while (it.hasNext()) {
                    OrderDataBean order = (OrderDataBean) it.next();
                    OrderData r = new OrderData(order.getOrderID(), order.getOrderStatus(), order.getOpenDate(), order.getCompletionDate(),
                            order.getOrderFee(), order.getOrderType(), order.getQuantity(), order.getSymbol(),order.getPrice());
                    orderjsfs[i] = r;
                    i++;
                }
                setNumberOfOrders(orderDataBeans.size());
                setAllOrders(orderjsfs);
            }

            setSessionCreationDate((Date) session.getAttribute("sessionCreationDate"));
            setCurrentTime(new java.util.Date());
            doAccountData(accountData, holdingDataBeans);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAccountData(AccountDataBean accountData, Collection<?> holdingDataBeans) {
        setProfileID(accountData.getProfileID());
        setAccountID(accountData.getAccountID());
        setCreationDate(accountData.getCreationDate());
        setLoginCount(accountData.getLoginCount());
        setLogoutCount(accountData.getLogoutCount());
        setLastLogin(accountData.getLastLogin());
        setOpenBalance(accountData.getOpenBalance());
        setBalance(accountData.getBalance());
        setNumberHoldings(holdingDataBeans.size());
        setHoldingsTotal(FinancialUtils.computeHoldingsTotal(holdingDataBeans));
        setSumOfCashHoldings(balance.add(holdingsTotal));
        setGain(FinancialUtils.computeGain(sumOfCashHoldings, openBalance));
        setGainPercent(FinancialUtils.computeGainPercent(sumOfCashHoldings, openBalance));
    }

    public Date getSessionCreationDate() {
        return sessionCreationDate;
    }

    public void setSessionCreationDate(Date sessionCreationDate) {
        this.sessionCreationDate = sessionCreationDate;
    }

    public Date getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Date currentTime) {
        this.currentTime = currentTime;
    }

    public String getProfileID() {
        return profileID;
    }

    public void setProfileID(String profileID) {
        this.profileID = profileID;
    }

    public void setAccountID(Integer accountID) {
        this.accountID = accountID;
    }

    public Integer getAccountID() {
        return accountID;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setOpenBalance(BigDecimal openBalance) {
        this.openBalance = openBalance;
    }

    public BigDecimal getOpenBalance() {
        return openBalance;
    }

    public void setHoldingsTotal(BigDecimal holdingsTotal) {
        this.holdingsTotal = holdingsTotal;
    }

    public BigDecimal getHoldingsTotal() {
        return holdingsTotal;
    }

    public void setSumOfCashHoldings(BigDecimal sumOfCashHoldings) {
        this.sumOfCashHoldings = sumOfCashHoldings;
    }

    public BigDecimal getSumOfCashHoldings() {
        return sumOfCashHoldings;
    }

    public void setGain(BigDecimal gain) {
        this.gain = gain;
    }

    public BigDecimal getGain() {
        return gain;
    }

    public void setGainPercent(BigDecimal gainPercent) {
        this.gainPercent = gainPercent.setScale(2);
    }

    public BigDecimal getGainPercent() {
        return gainPercent;
    }

    public void setNumberHoldings(Integer numberHoldings) {
        this.numberHoldings = numberHoldings;
    }

    public Integer getNumberHoldings() {
        return numberHoldings;
    }

    public OrderData[] getClosedOrders() {
        return closedOrders;
    }

    public void setClosedOrders(OrderData[] closedOrders) {
        this.closedOrders = closedOrders;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLogoutCount(int logoutCount) {
        this.logoutCount = logoutCount;
    }

    public int getLogoutCount() {
        return logoutCount;
    }

    public void setAllOrders(OrderData[] allOrders) {
        this.allOrders = allOrders;
    }

    public OrderData[] getAllOrders() {
        return allOrders;
    }

    public String getGainHTML() {
        return FinancialUtils.printGainHTML(gain);
    }

    public String getGainPercentHTML() {
        return FinancialUtils.printGainPercentHTML(gainPercent);
    }

	public Integer getNumberOfOrderRows() {
		return numberOfOrderRows;
	}

	public void setNumberOfOrderRows(Integer numberOfOrderRows) {
		this.numberOfOrderRows = numberOfOrderRows;
	}

    public Integer getNumberOfOrders() {
        return numberOfOrders;
    }

    public void setNumberOfOrders(Integer numberOfOrders) {
        this.numberOfOrders = numberOfOrders;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

public class ExternalContextProducer {
    @Produces
    @RequestScoped
    public ExternalContext produceFacesExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import com.ibm.websphere.samples.daytrader.util.FinancialUtils;

@Named
@SessionScoped
public class HoldingData implements Serializable {

    private static final long serialVersionUID = -4760036695773749721L;

    private Integer holdingID;
    private double quantity;
    private BigDecimal purchasePrice;
    private Date purchaseDate;
    private String quoteID;
    private BigDecimal price;
    private BigDecimal basis;
    private BigDecimal marketValue;
    private BigDecimal gain;

    public void setHoldingID(Integer holdingID) {
        this.holdingID = holdingID;
    }

    public Integer getHoldingID() {
        return holdingID;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setQuoteID(String quoteID) {
        this.quoteID = quoteID;
    }

    public String getQuoteID() {
        return quoteID;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setBasis(BigDecimal basis) {
        this.basis = basis;
    }

    public BigDecimal getBasis() {
        return basis;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setGain(BigDecimal gain) {
        this.gain = gain;
    }

    public BigDecimal getGain() {
        return gain;
    }

    public String getGainHTML() {
        return FinancialUtils.printGainHTML(gain);
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
//import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter(filterName = "JSFLoginFilter", urlPatterns = "*.faces")
public class JSFLoginFilter implements Filter {

    public JSFLoginFilter() {
        super();
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    private FilterConfig filterConfig = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (filterConfig == null) {
            return;
        }

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        HttpSession session = request.getSession();
        String userID = (String) session.getAttribute("uidBean");

        // If user has not logged in and is trying access account information,
        // redirect to login page.
        if (userID == null) {
            String url = request.getServletPath();

            if (url.contains("home") || url.contains("account") || url.contains("portfolio") || url.contains("quote") || url.contains("order")
                    || url.contains("marketSummary")) {
                System.out.println("JSF service error: User Not Logged in");
                response.sendRedirect("welcome.faces");
                return;
            }
        }

        chain.doFilter(req, resp/* wrapper */);
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
        this.filterConfig = null;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import com.ibm.websphere.samples.daytrader.util.Log;

@FacesValidator("loginValidator")
public class LoginValidator implements Validator{

  static String loginRegex = "uid:\\d+";
  static Pattern pattern = Pattern.compile(loginRegex);
  static Matcher matcher;

  // Simple JSF validator to make sure username starts with uid: and at least 1 number.
  public LoginValidator() {
  }

  @Override
  public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
	  if (Log.doTrace()) {
		  Log.trace("LoginValidator.validate","Validating submitted login name -- " + value.toString());
	  }
	  matcher = pattern.matcher(value.toString());
    
	  if (!matcher.matches()) {
		  FacesMessage msg = new FacesMessage("Username validation failed. Please provide username in this format: uid:#");
		  msg.setSeverity(FacesMessage.SEVERITY_ERROR);
      
		  throw new ValidatorException(msg);
	  }
  	}
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;

@Named("marketdata")
@RequestScoped
public class MarketSummaryJSF {
    @Inject
    private TradeAction tradeAction;

    private BigDecimal TSIA;
    private BigDecimal openTSIA;
    private double volume;
    private QuoteData[] topGainers;
    private QuoteData[] topLosers;
    private Date summaryDate;

    // cache the gainPercent once computed for this bean
    private BigDecimal gainPercent = null;

    @PostConstruct
    public void getMarketSummary() {
        try {
            MarketSummaryDataBean marketSummaryData = tradeAction.getMarketSummary();
            setSummaryDate(marketSummaryData.getSummaryDate());
            setTSIA(marketSummaryData.getTSIA());
            setVolume(marketSummaryData.getVolume());
            setGainPercent(marketSummaryData.getGainPercent());

            Collection<?> topGainers = marketSummaryData.getTopGainers();

            Iterator<?> gainers = topGainers.iterator();
            int count = 0;
            QuoteData[] gainerjsfs = new QuoteData[5];

            while (gainers.hasNext() && (count < 5)) {
                QuoteDataBean quote = (QuoteDataBean) gainers.next();
                QuoteData r = new QuoteData(quote.getPrice(), quote.getOpen(), quote.getSymbol());
                gainerjsfs[count] = r;
                count++;
            }

            setTopGainers(gainerjsfs);

            Collection<?> topLosers = marketSummaryData.getTopLosers();

            QuoteData[] loserjsfs = new QuoteData[5];
            count = 0;
            Iterator<?> losers = topLosers.iterator();

            while (losers.hasNext() && (count < 5)) {
                QuoteDataBean quote = (QuoteDataBean) losers.next();
                QuoteData r = new QuoteData(quote.getPrice(), quote.getOpen(), quote.getSymbol());
                loserjsfs[count] = r;
                count++;
            }

            setTopLosers(loserjsfs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTSIA(BigDecimal tSIA) {
        TSIA = tSIA;
    }

    public BigDecimal getTSIA() {
        return TSIA;
    }

    public void setOpenTSIA(BigDecimal openTSIA) {
        this.openTSIA = openTSIA;
    }

    public BigDecimal getOpenTSIA() {
        return openTSIA;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getVolume() {
        return volume;
    }

    public void setTopGainers(QuoteData[] topGainers) {
        this.topGainers = topGainers;
    }

    public QuoteData[] getTopGainers() {
        return topGainers;
    }

    public void setTopLosers(QuoteData[] topLosers) {
        this.topLosers = topLosers;
    }

    public QuoteData[] getTopLosers() {
        return topLosers;
    }

    public void setSummaryDate(Date summaryDate) {
        this.summaryDate = summaryDate;
    }

    public Date getSummaryDate() {
        return summaryDate;
    }

    public void setGainPercent(BigDecimal gainPercent) {
        this.gainPercent = gainPercent.setScale(2,RoundingMode.HALF_UP);
    }

    public BigDecimal getGainPercent() {
        return gainPercent;
    }

    public String getGainPercentHTML() {
        return FinancialUtils.printGainPercentHTML(gainPercent);
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.Date;

public class OrderData {
    private Integer orderID;
    private String orderStatus;
    private Date openDate;
    private Date completionDate;
    private BigDecimal orderFee;
    private String orderType;
    private double quantity;
    private String symbol;
    private BigDecimal total;
    private BigDecimal price;

    public OrderData(Integer orderID, String orderStatus, Date openDate, Date completeDate, BigDecimal orderFee, String orderType, double quantity,
            String symbol) {
        this.orderID = orderID;
        this.completionDate = completeDate;
        this.openDate = openDate;
        this.orderFee = orderFee;
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.quantity = quantity;
        this.symbol = symbol;
    }
    
    public OrderData(Integer orderID, String orderStatus, Date openDate, Date completeDate, BigDecimal orderFee, String orderType, double quantity,
            String symbol, BigDecimal price) {
        this.orderID = orderID;
        this.completionDate = completeDate;
        this.openDate = openDate;
        this.orderFee = orderFee;
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.quantity = quantity;
        this.symbol = symbol;
        this.price = price;
        this.total = price.multiply(new BigDecimal(quantity));

    }

    public void setOrderID(Integer orderID) {
        this.orderID = orderID;
    }

    public Integer getOrderID() {
        return orderID;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setOrderFee(BigDecimal orderFee) {
        this.orderFee = orderFee;
    }

    public BigDecimal getOrderFee() {
        return orderFee;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Named("orderdata")
public class OrderDataJSF {
    @Inject
    private ExternalContext facesExternalContext;

    @Inject
    private TradeAction tradeAction;

    private OrderData[] allOrders;
    private OrderData orderData;

    public OrderDataJSF() {
    }

    public void getAllOrder() {
        try {
            HttpSession session = (HttpSession) facesExternalContext.getSession(true);
            String userID = (String) session.getAttribute("uidBean");

            ArrayList<?> orderDataBeans = (TradeConfig.getLongRun() ? new ArrayList<Object>() : (ArrayList<?>) tradeAction.getOrders(userID));
            OrderData[] orders = new OrderData[orderDataBeans.size()];

            int count = 0;

            for (Object order : orderDataBeans) {
                OrderData r = new OrderData(((OrderDataBean) order).getOrderID(), ((OrderDataBean) order).getOrderStatus(),
                        ((OrderDataBean) order).getOpenDate(), ((OrderDataBean) order).getCompletionDate(), ((OrderDataBean) order).getOrderFee(),
                        ((OrderDataBean) order).getOrderType(), ((OrderDataBean) order).getQuantity(), ((OrderDataBean) order).getSymbol());
                r.setPrice(((OrderDataBean) order).getPrice());
                r.setTotal(r.getPrice().multiply(new BigDecimal(r.getQuantity())));
                orders[count] = r;
                count++;
            }

            setAllOrders(orders);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @PostConstruct
    public void getOrder() {
        HttpSession session = (HttpSession) facesExternalContext.getSession(true);
        OrderData order = (OrderData) session.getAttribute("orderData");

        if (order != null) {
            setOrderData(order);
        }
    }

    public void setAllOrders(OrderData[] allOrders) {
        this.allOrders = allOrders;
    }

    public OrderData[] getAllOrders() {
        return allOrders;
    }

    public void setOrderData(OrderData orderData) {
        this.orderData = orderData;
    }

    public OrderData getOrderData() {
        return orderData;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Named("portfolio")
@RequestScoped
public class PortfolioJSF {
    @Inject
    private ExternalContext facesExternalContext;

    @Inject
    private TradeAction tradeAction;

    private BigDecimal balance;
    private BigDecimal openBalance;
    private Integer numberHoldings;
    private BigDecimal holdingsTotal;
    private BigDecimal sumOfCashHoldings;
    private BigDecimal totalGain = new BigDecimal(0.0);
    private BigDecimal totalValue = new BigDecimal(0.0);
    private BigDecimal totalBasis = new BigDecimal(0.0);
    private BigDecimal totalGainPercent = new BigDecimal(0.0);
    private ArrayList<HoldingData> holdingDatas;
    private HtmlDataTable dataTable;

    @PostConstruct
    public void getPortfolio() {
        try {

            HttpSession session = (HttpSession) facesExternalContext.getSession(true);
            String userID = (String) session.getAttribute("uidBean");
            Collection<?> holdingDataBeans = tradeAction.getHoldings(userID);

            numberHoldings = holdingDataBeans.size();

            // Walk through the collection of user holdings and creating a list
            // of quotes
            if (holdingDataBeans.size() > 0) {
                Iterator<?> it = holdingDataBeans.iterator();
                holdingDatas = new ArrayList<HoldingData>(holdingDataBeans.size());

                while (it.hasNext()) {
                    HoldingDataBean holdingData = (HoldingDataBean) it.next();
                    QuoteDataBean quoteData = tradeAction.getQuote(holdingData.getQuoteID());

                    BigDecimal basis = holdingData.getPurchasePrice().multiply(new BigDecimal(holdingData.getQuantity()));
                    BigDecimal marketValue = quoteData.getPrice().multiply(new BigDecimal(holdingData.getQuantity()));
                    totalBasis = totalBasis.add(basis);
                    totalValue = totalValue.add(marketValue);
                    BigDecimal gain = marketValue.subtract(basis);
                    totalGain = totalGain.add(gain);

                    HoldingData h = new HoldingData();
                    h.setHoldingID(holdingData.getHoldingID());
                    h.setPurchaseDate(holdingData.getPurchaseDate());
                    h.setQuoteID(holdingData.getQuoteID());
                    h.setQuantity(holdingData.getQuantity());
                    h.setPurchasePrice(holdingData.getPurchasePrice());
                    h.setBasis(basis);
                    h.setGain(gain);
                    h.setMarketValue(marketValue);
                    h.setPrice(quoteData.getPrice());
                    holdingDatas.add(h);

                }
                // dataTable
                setTotalGainPercent(FinancialUtils.computeGainPercent(totalValue, totalBasis));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String sell() {

        HttpSession session = (HttpSession) facesExternalContext.getSession(true);
        String userID = (String) session.getAttribute("uidBean");
        TradeAction tAction = new TradeAction();
        OrderDataBean orderDataBean = null;
        HoldingData holdingData = (HoldingData) dataTable.getRowData();

        try {
            orderDataBean = tAction.sell(userID, holdingData.getHoldingID(), TradeConfig.orderProcessingMode);
            holdingDatas.remove(holdingData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        OrderData orderData = new OrderData(orderDataBean.getOrderID(), orderDataBean.getOrderStatus(), orderDataBean.getOpenDate(),
                orderDataBean.getCompletionDate(), orderDataBean.getOrderFee(), orderDataBean.getOrderType(), orderDataBean.getQuantity(),
                orderDataBean.getSymbol());
        session.setAttribute("orderData", orderData);
        return "sell";
    }

    public void setDataTable(HtmlDataTable dataTable) {
        this.dataTable = dataTable;
    }

    public HtmlDataTable getDataTable() {
        return dataTable;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setOpenBalance(BigDecimal openBalance) {
        this.openBalance = openBalance;
    }

    public BigDecimal getOpenBalance() {
        return openBalance;
    }

    public void setHoldingsTotal(BigDecimal holdingsTotal) {
        this.holdingsTotal = holdingsTotal;
    }

    public BigDecimal getHoldingsTotal() {
        return holdingsTotal;
    }

    public void setSumOfCashHoldings(BigDecimal sumOfCashHoldings) {
        this.sumOfCashHoldings = sumOfCashHoldings;
    }

    public BigDecimal getSumOfCashHoldings() {
        return sumOfCashHoldings;
    }

    public void setNumberHoldings(Integer numberHoldings) {
        this.numberHoldings = numberHoldings;
    }

    public Integer getNumberHoldings() {
        return numberHoldings;
    }

    public void setTotalGain(BigDecimal totalGain) {
        this.totalGain = totalGain;
    }

    public BigDecimal getTotalGain() {
        return totalGain;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalBasis(BigDecimal totalBasis) {
        this.totalBasis = totalBasis;
    }

    public BigDecimal getTotalBasis() {
        return totalBasis;
    }

    public void setHoldingDatas(ArrayList<HoldingData> holdingDatas) {
        this.holdingDatas = holdingDatas;
    }

    public ArrayList<HoldingData> getHoldingDatas() {
        return holdingDatas;
    }

    public void setTotalGainPercent(BigDecimal totalGainPercent) {
        this.totalGainPercent = totalGainPercent;
    }

    public BigDecimal getTotalGainPercent() {
        return totalGainPercent;
    }

    public String getTotalGainPercentHTML() {
        return FinancialUtils.printGainPercentHTML(totalGainPercent);
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.text.DecimalFormat;

import com.ibm.websphere.samples.daytrader.util.FinancialUtils;

public class QuoteData {
    private BigDecimal price;
    private BigDecimal open;
    private String symbol;
    private BigDecimal high;
    private BigDecimal low;
    private String companyName;
    private double volume;
    private double change;
    private String range;
    private BigDecimal gainPercent;
    private BigDecimal gain;

    public QuoteData(BigDecimal price, BigDecimal open, String symbol) {
        this.open = open;
        this.price = price;
        this.symbol = symbol;
        this.change = price.subtract(open).setScale(2).doubleValue();
    }

    public QuoteData(BigDecimal open, BigDecimal price, String symbol, BigDecimal high, BigDecimal low, String companyName, Double volume, Double change) {
        this.open = open;
        this.price = price;
        this.symbol = symbol;
        this.high = high;
        this.low = low;
        this.companyName = companyName;
        this.volume = volume;
        this.change = change;
        this.range = high.toString() + "-" + low.toString();
        this.gainPercent = FinancialUtils.computeGainPercent(price, open).setScale(2);
        this.gain = FinancialUtils.computeGain(price, open).setScale(2);
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getVolume() {
        return volume;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public double getChange() {
        return change;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getRange() {
        return range;
    }

    public void setGainPercent(BigDecimal gainPercent) {
        this.gainPercent = gainPercent.setScale(2);
    }

    public BigDecimal getGainPercent() {
        return gainPercent;
    }

    public void setGain(BigDecimal gain) {
        this.gain = gain;
    }

    public BigDecimal getGain() {
        return gain;
    }

    public String getGainPercentHTML() {
        return FinancialUtils.printGainPercentHTML(gainPercent);
    }

    public String getGainHTML() {
        return FinancialUtils.printGainHTML(gain);
    }

    public String getChangeHTML() {
        String htmlString, arrow;
        if (change < 0.0) {
            htmlString = "<FONT color=\"#cc0000\">";
            arrow = "arrowdown.gif";
        } else {
            htmlString = "<FONT color=\"#009900\">";
            arrow = "arrowup.gif";
        }
        DecimalFormat df = new DecimalFormat("####0.00");

        htmlString += df.format(change) + "</FONT><IMG src=\"images/" + arrow + "\" width=\"10\" height=\"10\" border=\"0\"></IMG>";
        return htmlString;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Named("quotedata")
@RequestScoped
public class QuoteJSF {

    @Inject
    private ExternalContext facesExternalContext;

    @Inject
    private TradeAction tradeAction;

    private QuoteData[] quotes;
    private String symbols = null;
    private HtmlDataTable dataTable;
    private Integer quantity = 100;

    @PostConstruct
    public void getAllQuotes() {
        getQuotesBySymbols();
    }

    public String getQuotesBySymbols() {
        HttpSession session = (HttpSession) facesExternalContext.getSession(true);

        if (symbols == null && (session.getAttribute("symbols") == null)) {
            setSymbols("s:0,s:1,s:2,s:3,s:4");
            session.setAttribute("symbols", getSymbols());
        } else if (symbols == null && session.getAttribute("symbols") != null) {
            setSymbols((String) session.getAttribute("symbols"));
        }

        else {
            session.setAttribute("symbols", getSymbols());
        }

        java.util.StringTokenizer st = new java.util.StringTokenizer(symbols, " ,");
        QuoteData[] quoteDatas = new QuoteData[st.countTokens()];
        int count = 0;

        while (st.hasMoreElements()) {
            String symbol = st.nextToken();

            try {
                QuoteDataBean quoteData = tradeAction.getQuote(symbol);
                quoteDatas[count] = new QuoteData(quoteData.getOpen(), quoteData.getPrice(), quoteData.getSymbol(), quoteData.getHigh(), quoteData.getLow(),
                        quoteData.getCompanyName(), quoteData.getVolume(), quoteData.getChange());
                count++;
            } catch (Exception e) {
                Log.error(e.toString());
            }
        }
        setQuotes(quoteDatas);
        return "quotes";
    }

    public String buy() {
        HttpSession session = (HttpSession) facesExternalContext.getSession(true);
        String userID = (String) session.getAttribute("uidBean");
        QuoteData quoteData = (QuoteData) dataTable.getRowData();
        OrderDataBean orderDataBean;

        try {
            orderDataBean = tradeAction.buy(userID, quoteData.getSymbol(), new Double(this.quantity).doubleValue(), TradeConfig.orderProcessingMode);
            OrderData orderData = new OrderData(orderDataBean.getOrderID(), orderDataBean.getOrderStatus(), orderDataBean.getOpenDate(),
                    orderDataBean.getCompletionDate(), orderDataBean.getOrderFee(), orderDataBean.getOrderType(), orderDataBean.getQuantity(),
                    orderDataBean.getSymbol());
            session.setAttribute("orderData", orderData);
        } catch (Exception e) {
            Log.error(e.toString());
            e.printStackTrace();
        }
        return "buy";
    }

    public void setQuotes(QuoteData[] quotes) {
        this.quotes = quotes;
    }

    public QuoteData[] getQuotes() {
        return quotes;
    }

    public void setSymbols(String symbols) {
        this.symbols = symbols;
    }

    public String getSymbols() {
        return symbols;
    }

    public void setDataTable(HtmlDataTable dataTable) {
        this.dataTable = dataTable;
    }

    public HtmlDataTable getDataTable() {
        return dataTable;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

import com.ibm.websphere.samples.daytrader.TradeAction;

public class TradeActionProducer {
	@Produces
    @RequestScoped
    public TradeAction produceTradeAction() {
        return new TradeAction();
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;
import java.math.BigDecimal;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.entities.AccountDataBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;

@Named("tradeapp")
@SessionScoped
public class TradeAppJSF implements Serializable {
	
    @Inject
    private ExternalContext facesExternalContext;

    @Inject
    private TradeAction tradeAction;

    private static final long serialVersionUID = 2L;
    private String userID = "uid:0";
    private String password = "xxx";
    private String cpassword;
    private String results;
    private String fullname;
    private String address;
    private String email;
    private String ccn;
    private String money;

    public String login() {
        try {        	
            AccountDataBean accountData = tradeAction.login(userID, password);

            AccountProfileDataBean accountProfileData = tradeAction.getAccountProfileData(userID);
            if (accountData != null) {
                HttpSession session = (HttpSession) facesExternalContext.getSession(true);

                session.setAttribute("uidBean", userID);
                session.setAttribute("sessionCreationDate", new java.util.Date());
                setResults("Ready to Trade");

                // Get account profile information
                setAddress(accountProfileData.getAddress());
                setCcn(accountProfileData.getCreditCard());
                setEmail(accountProfileData.getEmail());
                setFullname(accountProfileData.getFullName());
                setCpassword(accountProfileData.getPassword());
                return "Ready to Trade";
            } else {
                Log.log("TradeServletAction.doLogin(...)", "Error finding account for user " + userID + "",
                        "user entered a bad username or the database is not populated");
                throw new NullPointerException("User does not exist or password is incorrect!");
            }
        }

        catch (Exception se) {
            // Go to welcome page
            setResults("Could not find account");
            return "welcome";
        }
    }

    public String register() {
        TradeAction tAction = new TradeAction();
        // Validate user passwords match and are atleast 1 char in length
        try {
            if ((password.equals(cpassword)) && (password.length() >= 1)) {
                AccountDataBean accountData = tAction.register(userID, password, fullname, address, email, ccn, new BigDecimal(money));

                if (accountData == null) {
                    setResults("Registration operation failed;");
                    // Go to register page
                    return "Registration operation failed";

                } else {
                    login();
                    setResults("Registration operation succeeded;  Account " + accountData.getAccountID() + " has been created.");
                    return "Registration operation succeeded";
                }
            }

            else {
                // Password validation failed
                setResults("Registration operation failed, your passwords did not match");
                // Go to register page
                return "Registration operation failed";
            }
        }

        catch (Exception e) {
            // log the exception with error page
            Log.log("TradeServletAction.doRegister(...)" + " exception user =" + userID);
            try {
                throw new Exception("TradeServletAction.doRegister(...)" + " exception user =" + userID, e);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }
        return "Registration operation succeeded";
    }

    public String updateProfile() {
        TradeAction tAction = new TradeAction();
        // First verify input data
        boolean doUpdate = true;

        if (password.equals(cpassword) == false) {
            results = "Update profile error: passwords do not match";
            doUpdate = false;
        }

        AccountProfileDataBean accountProfileData = new AccountProfileDataBean(userID, password, fullname, address, email, ccn);

        try {
            if (doUpdate) {
                accountProfileData = tAction.updateAccountProfile(accountProfileData);
                results = "Account profile update successful";
            }

        } catch (java.lang.IllegalArgumentException e) {
            // this is a user error so I will
            // forward them to another page rather than throw a 500
            setResults("invalid argument, check userID is correct, and the database is populated" + userID);
            Log.error(e, "TradeServletAction.doAccount(...)", "illegal argument, information should be in exception string",
                    "treating this as a user error and forwarding on to a new page");
        } catch (Exception e) {
            // log the exception with error page
            e.printStackTrace();
        }
        // Go to account.xhtml
        return "Go to account";
    }

    public String logout() {
        TradeAction tAction = new TradeAction();
        try {
        	setResults("");
            tAction.logout(userID);
        } catch (java.lang.IllegalArgumentException e) {
            // this is a user error so I will
            // forward them to another page, at the end of the page.
            setResults("illegal argument:" + e.getMessage());

            // log the exception with an error level of 3 which means, handled
            // exception but would invalidate a automation run
            Log.error(e, "TradeServletAction.doLogout(...)", "illegal argument, information should be in exception string",
                    "treating this as a user error and forwarding on to a new page");
        } catch (Exception e) {
            // log the exception and foward to a error page
            Log.error(e, "TradeAppJSF.logout():", "Error logging out" + userID, "fowarding to an error page");
        }

        HttpSession session = (HttpSession) facesExternalContext.getSession(false);

        if (session != null) {
            session.invalidate();
        }
        
        // Added to actually remove a user from the authentication cache
        try {
            ((HttpServletRequest) facesExternalContext.getRequest()).logout();
        } catch (ServletException e) {
            Log.error(e, "TradeAppJSF.logout():", "Error logging out request" + userID, "fowarding to an error page");
        }
        
        // Go to welcome page
        return "welcome";
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCpassword() {
        return cpassword;
    }

    public void setCpassword(String cpassword) {
        this.cpassword = cpassword;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getResults() {
    	String tempResults=results;
    	results="";
        return tempResults;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCcn() {
        return ccn;
    }

    public void setCcn(String ccn) {
        this.ccn = ccn;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }
};

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.beans.RunStatsDataBean;
import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;
import com.ibm.websphere.samples.daytrader.web.TradeBuildDB;

@Named("tradeconfig")
@RequestScoped
public class TradeConfigJSF {

    @Inject
    private ExternalContext facesExternalContext;

    private String runtimeMode = TradeConfig.runTimeModeNames[TradeConfig.getRunTimeMode()];
    private String orderProcessingMode = TradeConfig.orderProcessingModeNames[TradeConfig.getOrderProcessingMode()];
    //private String cachingType = TradeConfig.cachingTypeNames[TradeConfig.getCachingType()];
    //private int distributedMapCacheSize = TradeConfig.getDistributedMapCacheSize();
    private int maxUsers = TradeConfig.getMAX_USERS();
    private int maxQuotes = TradeConfig.getMAX_QUOTES();
    private int marketSummaryInterval = TradeConfig.getMarketSummaryInterval();
    private String webInterface = TradeConfig.webInterfaceNames[TradeConfig.getWebInterface()];
    private int primIterations = TradeConfig.getPrimIterations();
    private int percentSentToWebsocket = TradeConfig.getPercentSentToWebsocket();
    private boolean publishQuotePriceChange = TradeConfig.getPublishQuotePriceChange();
    private boolean longRun = TradeConfig.getLongRun();
    private boolean displayOrderAlerts = TradeConfig.getDisplayOrderAlerts();
    private boolean useRemoteEJBInterface = TradeConfig.useRemoteEJBInterface();
    private boolean actionTrace = TradeConfig.getActionTrace();
    private boolean trace = TradeConfig.getTrace();
    private String[] runtimeModeList = TradeConfig.runTimeModeNames;
    private String[] orderProcessingModeList = TradeConfig.orderProcessingModeNames;
    //private String[] cachingTypeList = TradeConfig.cachingTypeNames;
    private String[] webInterfaceList = TradeConfig.webInterfaceNames;
    private String result = "";

    public void updateConfig() {
        String currentConfigStr = "\n\n########## Trade configuration update. Current config:\n\n";
        String runTimeModeStr = this.runtimeMode;
        if (runTimeModeStr != null) {
            try {
                for (int i = 0; i < runtimeModeList.length; i++) {
                    if (runTimeModeStr.equals(runtimeModeList[i])) {
                        TradeConfig.setRunTimeMode(i);
                    }
                }
            } catch (Exception e) {

                Log.error(e, "TradeConfigJSF.updateConfig(..): minor exception caught", "trying to set runtimemode to " + runTimeModeStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tRunTimeMode:\t\t\t" + TradeConfig.runTimeModeNames[TradeConfig.getRunTimeMode()] + "\n";

        TradeConfig.setUseRemoteEJBInterface(useRemoteEJBInterface);
        currentConfigStr += "\t\tUse Remote EJB Interface:\t" + TradeConfig.useRemoteEJBInterface() + "\n";
        
        String orderProcessingModeStr = this.orderProcessingMode;
        if (orderProcessingModeStr != null) {
            try {
                for (int i = 0; i < orderProcessingModeList.length; i++) {
                    if (orderProcessingModeStr.equals(orderProcessingModeList[i])) {
                        TradeConfig.orderProcessingMode = i;
                    }
                }
            } catch (Exception e) {
                Log.error(e, "TradeConfigJSF.updateConfig(..): minor exception caught", "trying to set orderProcessing to " + orderProcessingModeStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tOrderProcessingMode:\t\t" + TradeConfig.orderProcessingModeNames[TradeConfig.orderProcessingMode] + "\n";

        /*
        String cachingTypeStr = this.cachingType;
        if (cachingTypeStr != null) {
            try {
                for (int i = 0; i < cachingTypeList.length; i++) {
                    if (cachingTypeStr.equals(cachingTypeList[i])) {
                        TradeConfig.cachingType = i;
                    }
                }
            } catch (Exception e) {
                Log.error(e, "TradeConfigJSF.updateConfig(..): minor exception caught", "trying to set cachingType to " + cachingTypeStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tCachingType:\t\t\t" + TradeConfig.cachingTypeNames[TradeConfig.cachingType] + "\n";

        int distMapCacheSize = this.distributedMapCacheSize;

        try {
            TradeConfig.setDistributedMapCacheSize(distMapCacheSize);
        } catch (Exception e) {
            Log.error(e, "TradeConfigJSF.updateConfig(..): minor exception caught", "trying to set distributedMapCacheSize", "reverting to current value");

        } // If the value is bad, simply revert to current

        currentConfigStr += "\t\tDMapCacheSize:\t\t\t" + TradeConfig.getDistributedMapCacheSize() + "\n";
		*/
        
        String webInterfaceStr = webInterface;
        if (webInterfaceStr != null) {
            try {
                for (int i = 0; i < webInterfaceList.length; i++) {
                    if (webInterfaceStr.equals(webInterfaceList[i])) {
                        TradeConfig.webInterface = i;
                    }
                }
            } catch (Exception e) {
                Log.error(e, "TradeConfigJSF.updateConfig(..): minor exception caught", "trying to set WebInterface to " + webInterfaceStr,
                        "reverting to current value");

            } // If the value is bad, simply revert to current
        }
        currentConfigStr += "\t\tWeb Interface:\t\t\t" + TradeConfig.webInterfaceNames[TradeConfig.webInterface] + "\n";

        TradeConfig.setMAX_USERS(maxUsers);
        TradeConfig.setMAX_QUOTES(maxQuotes);

        currentConfigStr += "\t\tTrade  Users:\t\t\t" + TradeConfig.getMAX_USERS() + "\n";
        currentConfigStr += "\t\tTrade Quotes:\t\t\t" + TradeConfig.getMAX_QUOTES() + "\n";

        TradeConfig.setMarketSummaryInterval(marketSummaryInterval);

        currentConfigStr += "\t\tMarket Summary Interval:\t" + TradeConfig.getMarketSummaryInterval() + "\n";

        TradeConfig.setPrimIterations(primIterations);

        currentConfigStr += "\t\tPrimitive Iterations:\t\t" + TradeConfig.getPrimIterations() + "\n";

        TradeConfig.setPublishQuotePriceChange(publishQuotePriceChange);
        currentConfigStr += "\t\tTradeStreamer MDB Enabled:\t" + TradeConfig.getPublishQuotePriceChange() + "\n";

        TradeConfig.setPercentSentToWebsocket(percentSentToWebsocket);
        currentConfigStr += "\t\t% of trades on Websocket:\t" + TradeConfig.getPercentSentToWebsocket() + "\n";
        
        TradeConfig.setLongRun(longRun);
        currentConfigStr += "\t\tLong Run Enabled:\t\t" + TradeConfig.getLongRun() + "\n";

        TradeConfig.setDisplayOrderAlerts(displayOrderAlerts);
        currentConfigStr += "\t\tDisplay Order Alerts:\t\t" + TradeConfig.getDisplayOrderAlerts() + "\n";

        Log.setTrace(trace);
        currentConfigStr += "\t\tTrace Enabled:\t\t\t" + TradeConfig.getTrace() + "\n";

        Log.setActionTrace(actionTrace);
        currentConfigStr += "\t\tAction Trace Enabled:\t\t" + TradeConfig.getActionTrace() + "\n";

        System.out.println(currentConfigStr);
        setResult("DayTrader Configuration Updated");
    }

    public String resetTrade() {
        RunStatsDataBean runStatsData = new RunStatsDataBean();
        TradeConfig currentConfig = new TradeConfig();
        HttpSession session = (HttpSession) facesExternalContext.getSession(true);
        try {
        	// Do not inject TradeAction on this class because we dont want the 
        	// config to initialiaze at startup. 
            TradeAction tradeAction = new TradeAction();
            runStatsData = tradeAction.resetTrade(false);
            session.setAttribute("runStatsData", runStatsData);
            session.setAttribute("tradeConfig", currentConfig);
            result += "Trade Reset completed successfully";

        } catch (Exception e) {
            result += "Trade Reset Error  - see log for details";
            session.setAttribute("result", result);
            Log.error(e, result);
        }

        return "stats";
    }

    public String populateDatabase() {

        try {
            new TradeBuildDB(new java.io.PrintWriter(System.out), null);
        } catch (Exception e) {
            e.printStackTrace();
        }       

        result = "TradeBuildDB: **** DayTrader Database Built - " + TradeConfig.getMAX_USERS() + " users created, " + TradeConfig.getMAX_QUOTES()
                + " quotes created. ****<br/>";
        result += "TradeBuildDB: **** Check System.Out for any errors. ****<br/>";

        return "database";
    }

    public String buildDatabaseTables() {
        try {

            //Find out the Database being used
            TradeDirect tradeDirect = new TradeDirect();

            String dbProductName = null;
            try {
                dbProductName = tradeDirect.checkDBProductName();
            } catch (Exception e) {
                Log.error(e, "TradeBuildDB: Unable to check DB Product name");
            }
            if (dbProductName == null) {
                result += "TradeBuildDB: **** Unable to check DB Product name, please check Database/AppServer configuration and retry ****<br/>";
                return "database";
            }

            String ddlFile = null;
            //Locate DDL file for the specified database
            try {
                result = result + "TradeBuildDB: **** Database Product detected: " + dbProductName + " ****<br/>";
                if (dbProductName.startsWith("DB2/")) { // if db is DB2
                    ddlFile = "/dbscripts/db2/Table.ddl";
                } else if (dbProductName.startsWith("DB2 UDB for AS/400")) { //if db is DB2 on IBM i
                  ddlFile = "/dbscripts/db2i/Table.ddl";
                } else if (dbProductName.startsWith("Apache Derby")) { //if db is Derby
                    ddlFile = "/dbscripts/derby/Table.ddl";
                } else if (dbProductName.startsWith("Oracle")) { // if the Db is Oracle
                    ddlFile = "/dbscripts/oracle/Table.ddl";
                } else { // Unsupported "Other" Database
                    ddlFile = "/dbscripts/derby/Table.ddl";
                    result = result + "TradeBuildDB: **** This Database is unsupported/untested use at your own risk ****<br/>";
                }

                result = result + "TradeBuildDB: **** The DDL file at path" + ddlFile + " will be used ****<br/>";
            } catch (Exception e) {
                Log.error(e, "TradeBuildDB: Unable to locate DDL file for the specified database");
                result = result + "TradeBuildDB: **** Unable to locate DDL file for the specified database ****<br/>";
                return "database";
            }

            new TradeBuildDB(new java.io.PrintWriter(System.out), facesExternalContext.getResourceAsStream(ddlFile));

            result = result + "TradeBuildDB: **** DayTrader Database Created, Check System.Out for any errors. ****<br/>";

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Go to configure.xhtml
        return "database";
    }

    public void setRuntimeMode(String runtimeMode) {
        this.runtimeMode = runtimeMode;
    }

    public String getRuntimeMode() {
        return runtimeMode;
    }

    public void setOrderProcessingMode(String orderProcessingMode) {
        this.orderProcessingMode = orderProcessingMode;
    }

    public String getOrderProcessingMode() {
        return orderProcessingMode;
    }
    /*
    public void setCachingType(String cachingType) {
        this.cachingType = cachingType;
    }

    public String getCachingType() {
        return cachingType;
    }

    public void setDistributedMapCacheSize(int distributedMapCacheSize) {
        this.distributedMapCacheSize = distributedMapCacheSize;
    }

    public int getDistributedMapCacheSize() {
        return distributedMapCacheSize;
    }*/

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public void setmaxQuotes(int maxQuotes) {
        this.maxQuotes = maxQuotes;
    }

    public int getMaxQuotes() {
        return maxQuotes;
    }

    public void setMarketSummaryInterval(int marketSummaryInterval) {
        this.marketSummaryInterval = marketSummaryInterval;
    }

    public int getMarketSummaryInterval() {
        return marketSummaryInterval;
    }

    public void setPrimIterations(int primIterations) {
        this.primIterations = primIterations;
    }

    public int getPrimIterations() {
        return primIterations;
    }

    public void setPublishQuotePriceChange(boolean publishQuotePriceChange) {
        this.publishQuotePriceChange = publishQuotePriceChange;
    }

    public boolean isPublishQuotePriceChange() {
        return publishQuotePriceChange;
    }
    
    public void setPercentSentToWebsocket(int percentSentToWebsocket) {
        this. percentSentToWebsocket =  percentSentToWebsocket;
    }

    public int getPercentSentToWebsocket() {
        return  percentSentToWebsocket;
    }

    public void setDisplayOrderAlerts(boolean displayOrderAlerts) {
        this.displayOrderAlerts = displayOrderAlerts;
    }

    public boolean isDisplayOrderAlerts() {
        return displayOrderAlerts;
    }
    
    public void setUseRemoteEJBInterface(boolean useRemoteEJBInterface) {
        this.useRemoteEJBInterface = useRemoteEJBInterface;
    }

    public boolean isUseRemoteEJBInterface() {
        return useRemoteEJBInterface;
    }

    public void setLongRun(boolean longRun) {
        this.longRun = longRun;
    }

    public boolean isLongRun() {
        return longRun;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setRuntimeModeList(String[] runtimeModeList) {
        this.runtimeModeList = runtimeModeList;
    }

    public String[] getRuntimeModeList() {
        return runtimeModeList;
    }

    public void setOrderProcessingModeList(String[] orderProcessingModeList) {
        this.orderProcessingModeList = orderProcessingModeList;
    }

    public String[] getOrderProcessingModeList() {
        return orderProcessingModeList;
    }

    /*public void setCachingTypeList(String[] cachingTypeList) {
        this.cachingTypeList = cachingTypeList;
    }

    public String[] getCachingTypeList() {
        return cachingTypeList;
    }*/

    public void setWebInterface(String webInterface) {
        this.webInterface = webInterface;
    }

    public String getWebInterface() {
        return webInterface;
    }

    public void setWebInterfaceList(String[] webInterfaceList) {
        this.webInterfaceList = webInterfaceList;
    }

    public String[] getWebInterfaceList() {
        return webInterfaceList;
    }

    public void setActionTrace(boolean actionTrace) {
        this.actionTrace = actionTrace;
    }

    public boolean isActionTrace() {
        return actionTrace;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * ExplicitGC invokes System.gc(). This allows one to gather min / max heap
 * statistics.
 *
 */
@WebServlet(name = "ExplicitGC", urlPatterns = { "/servlet/ExplicitGC" })
public class ExplicitGC extends HttpServlet {

    private static final long serialVersionUID = -3758934393801102408L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (01/29/2006
     * 20:10:00 PM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/html");

            ServletOutputStream out = res.getOutputStream();
            hitCount++;
            long totalMemory = Runtime.getRuntime().totalMemory();

            long maxMemoryBeforeGC = Runtime.getRuntime().maxMemory();
            long freeMemoryBeforeGC = Runtime.getRuntime().freeMemory();
            long startTime = System.currentTimeMillis();

            System.gc(); // Invoke the GC.

            long endTime = System.currentTimeMillis();
            long maxMemoryAfterGC = Runtime.getRuntime().maxMemory();
            long freeMemoryAfterGC = Runtime.getRuntime().freeMemory();

            out.println("<html><head><title>ExplicitGC</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Explicit Garbage Collection<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
                    + initTime
                    + "<BR><BR></FONT>  <B>Hit Count: "
                    + hitCount
                    + "<br>"
                    + "<table border=\"0\"><tr>"
                    + "<td align=\"right\">Total Memory</td><td align=\"right\">"
                    + totalMemory
                    + "</td>"
                    + "</tr></table>"
                    + "<table width=\"350\"><tr><td colspan=\"2\" align=\"left\">"
                    + "Statistics before GC</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Max Memory</td><td align=\"right\">"
                    + maxMemoryBeforeGC
                    + "</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Free Memory</td><td align=\"right\">"
                    + freeMemoryBeforeGC
                    + "</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Used Memory</td><td align=\"right\">"
                    + (totalMemory - freeMemoryBeforeGC)
                    + "</td></tr>"
                    + "<tr><td colspan=\"2\" align=\"left\">Statistics after GC</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Max Memory</td><td align=\"right\">"
                    + maxMemoryAfterGC
                    + "</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Free Memory</td><td align=\"right\">"
                    + freeMemoryAfterGC
                    + "</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Used Memory</td><td align=\"right\">"
                    + (totalMemory - freeMemoryAfterGC)
                    + "</td></tr>"
                    + "<tr><td align=\"right\">"
                    + "Total Time in GC</td><td align=\"right\">"
                    + Float.toString((endTime - startTime) / 1000)
                    + "s</td></tr>"
                    + "</table>" + "</body></html>");
        } catch (Exception e) {
            Log.error(e, "ExplicitGC.doGet(...): general exception caught");
            res.sendError(500, e.toString());

        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Generate Explicit GC to VM";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }
}

/**
 * (C) Copyright IBM Corporation 2016.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Simple bean to get and set messages
 */

public class PingBean {

    private String msg;

    /**
     * returns the message contained in the bean
     *
     * @return message String
     **/
    public String getMsg() {
        return msg;
    }

    /**
     * sets the message contained in the bean param message String
     **/
    public void setMsg(String s) {
        msg = s;
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;

@RequestScoped
@PingInterceptorBinding
public class PingCDIBean {

    private static int helloHitCount = 0;
    private static int getBeanManagerHitCountJNDI = 0;
    private static int getBeanManagerHitCountSPI = 0;

    
    public int hello() {
        return ++helloHitCount;
    }

    public int getBeanMangerViaJNDI() throws Exception {
        BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        Set<Bean<?>> beans = beanManager.getBeans(Object.class);
        if (beans.size() > 0) {
            return ++getBeanManagerHitCountJNDI;
        }
        return 0;

    }
    
    public int getBeanMangerViaCDICurrent() throws Exception {
        BeanManager beanManager = CDI.current().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(Object.class);
        
        if (beans.size() > 0) {
            return ++getBeanManagerHitCountSPI;
        }
        return 0;

    }
}

/**
 * (C) Copyright IBM Corporation 2016.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@Named
@SessionScoped
public class PingCDIJSFBean implements Serializable {

    private static final long serialVersionUID = -7475815494313679416L;
    private int hitCount = 0;

    public int getHitCount() {
        return ++hitCount;
    }
}

/**
 * (C) Copyright IBM Corporation 2016.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * EJB interface
 */
public interface PingEJBIFace {

    public String getMsg();
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.ejb.Local;
import javax.ejb.Stateful;

/**
 *
 */
@Stateful
@Local
public class PingEJBLocal implements PingEJBIFace {

    private static int hitCount;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.samples.daytrader.web.prims.EJBIFace#getMsg()
     */
    @Override
    public String getMsg() {

        return "PingEJBLocal: " + hitCount++;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

@Decorator
@Priority(Interceptor.Priority.APPLICATION)
public class PingEJBLocalDecorator implements PingEJBIFace {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.samples.daytrader.web.prims.EJBIFace#getMsg()
     */
    @Delegate
    @Inject
    PingEJBIFace ejb;

    @Override
    public String getMsg() {

        return "Decorated " + ejb.getMsg();
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 */
@PingInterceptorBinding
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class PingInterceptor implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    @AroundInvoke
    public Object methodInterceptor(InvocationContext ctx) throws Exception {

        //noop
        return ctx.proceed();
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 *
 */
@InterceptorBinding
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface PingInterceptorBinding {

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingJDBCReadPrepStmt uses a prepared statement for database read access. This
 * primative uses
 * {@link com.ibm.websphere.samples.daytrader.direct.TradeDirect} to set the
 * price of a random stock (generated by
 * {@link com.ibm.websphere.samples.daytrader.util.TradeConfig}) through the use
 * of prepared statements.
 *
 */

@WebServlet(name = "PingJDBCRead", urlPatterns = { "/servlet/PingJDBCRead" })
public class PingJDBCRead extends HttpServlet {

    private static final long serialVersionUID = -8810390150632488526L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        String symbol = null;
        StringBuffer output = new StringBuffer(100);

        try {
            // TradeJDBC uses prepared statements so I am going to make use of
            // it's code.
            TradeDirect trade = new TradeDirect();
            symbol = TradeConfig.rndSymbol();

            QuoteDataBean quoteData = null;
            int iter = TradeConfig.getPrimIterations();
            for (int ii = 0; ii < iter; ii++) {
                quoteData = trade.getQuote(symbol);
            }

            output.append("<html><head><title>Ping JDBC Read w/ Prepared Stmt.</title></head>"
                    + "<body><HR><FONT size=\"+2\" color=\"#000066\">Ping JDBC Read w/ Prep Stmt:</FONT><HR><FONT size=\"-1\" color=\"#000066\">Init time : "
                    + initTime);
            hitCount++;
            output.append("<BR>Hit Count: " + hitCount);
            output.append("<HR>Quote Information <BR><BR>: " + quoteData.toHTML());
            output.append("<HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingJDBCRead w/ Prep Stmt -- error getting quote for symbol", symbol);
            res.sendError(500, "PingJDBCRead Exception caught: " + e.toString());
        }

    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JDBC Read using a prepared statment, makes use of TradeJDBC class";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingJDBCReadPrepStmt uses a prepared statement for database read access. This
 * primative uses
 * {@link com.ibm.websphere.samples.daytrader.direct.TradeDirect} to set the
 * price of a random stock (generated by
 * {@link com.ibm.websphere.samples.daytrader.util.TradeConfig}) through the use
 * of prepared statements.
 *
 */

@WebServlet(name = "PingJDBCRead2JSP", urlPatterns = { "/servlet/PingJDBCRead2JSP" })
public class PingJDBCRead2JSP extends HttpServlet {

    private static final long serialVersionUID = 1118803761565654806L;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String symbol = null;
        QuoteDataBean quoteData = null;
        ServletContext ctx = getServletConfig().getServletContext();

        try {
            // TradeJDBC uses prepared statements so I am going to make use of
            // it's code.
            TradeDirect trade = new TradeDirect();
            symbol = TradeConfig.rndSymbol();

            int iter = TradeConfig.getPrimIterations();
            for (int ii = 0; ii < iter; ii++) {
                quoteData = trade.getQuote(symbol);
            }

            req.setAttribute("quoteData", quoteData);
            // req.setAttribute("hitCount", hitCount);
            // req.setAttribute("initTime", initTime);

            ctx.getRequestDispatcher("/quoteDataPrimitive.jsp").include(req, res);
        } catch (Exception e) {
            Log.error(e, "PingJDBCRead2JPS -- error getting quote for symbol", symbol);
            res.sendError(500, "PingJDBCRead2JSP Exception caught: " + e.toString());
        }

    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JDBC Read using a prepared statment forwarded to a JSP, makes use of TradeJDBC class";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // hitCount = 0;
        // initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingJDBCReadPrepStmt uses a prepared statement for database update. Statement
 * parameters are set dynamically on each request. This primative uses
 * {@link com.ibm.websphere.samples.daytrader.direct.TradeDirect} to set the
 * price of a random stock (generated by
 * {@link com.ibm.websphere.samples.daytrader.util.TradeConfig}) through the use
 * of prepared statements.
 *
 */
@WebServlet(name = "PingJDBCWrite", urlPatterns = { "/servlet/PingJDBCWrite" })
public class PingJDBCWrite extends HttpServlet {

    private static final long serialVersionUID = -4938035109655376503L;
    private static String initTime;
    private static int hitCount;

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        String symbol = null;
        BigDecimal newPrice;
        StringBuffer output = new StringBuffer(100);
        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();

        try {
            // get a random symbol to update and a random price.
            symbol = TradeConfig.rndSymbol();
            newPrice = TradeConfig.getRandomPriceChangeFactor();

            // TradeJDBC makes use of prepared statements so I am going to reuse
            // the existing code.
            TradeDirect trade = new TradeDirect();

            // update the price of our symbol
            QuoteDataBean quoteData = null;
            int iter = TradeConfig.getPrimIterations();
            for (int ii = 0; ii < iter; ii++) {
                quoteData = trade.updateQuotePriceVolumeInt(symbol, newPrice, 100.0, false);
            }

            // write the output
            output.append("<html><head><title>Ping JDBC Write w/ Prepared Stmt.</title></head>"
                    + "<body><HR><FONT size=\"+2\" color=\"#000066\">Ping JDBC Write w/ Prep Stmt:</FONT><FONT size=\"-1\" color=\"#000066\"><HR>Init time : "
                    + initTime);
            hitCount++;
            output.append("<BR>Hit Count: " + hitCount);
            output.append("<HR>Update Information<BR>");
            output.append("<BR>" + quoteData.toHTML() + "<HR></FONT></BODY></HTML>");
            out.println(output.toString());

        } catch (Exception e) {
            Log.error(e, "PingJDBCWrite -- error updating quote for symbol", symbol);
            res.sendError(500, "PingJDBCWrite Exception caught: " + e.toString());
        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JDBC Write using a prepared statment makes use of TradeJDBC code.";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingJSONP tests JSON generating and parsing 
 *
 */

@WebServlet(name = "PingJSONP", urlPatterns = { "/servlet/PingJSONP" })
public class PingJSONP extends HttpServlet {


    /**
     * 
     */
    private static final long serialVersionUID = -5348806619121122708L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/html");

            ServletOutputStream out = res.getOutputStream();
            
            hitCount++;
            
            // JSON generate
            StringWriter sw = new StringWriter();
            JsonGenerator generator = Json.createGenerator(sw);
             
            generator.writeStartObject();
            generator.write("initTime",initTime);
            generator.write("hitCount", hitCount);
            generator.writeEnd();
            generator.flush();
            
            String generatedJSON =  sw.toString();
            StringBuffer parsedJSON = new StringBuffer(); 
            
            // JSON parse
            JsonParser parser = Json.createParser(new StringReader(generatedJSON));
            while (parser.hasNext()) {
               JsonParser.Event event = parser.next();
               switch(event) {
                  case START_ARRAY:
                  case END_ARRAY:
                  case START_OBJECT:
                  case END_OBJECT:
                  case VALUE_FALSE:
                  case VALUE_NULL:
                  case VALUE_TRUE:
                     break;
                  case KEY_NAME:
                      parsedJSON.append(parser.getString() + ":");
                     break;
                  case VALUE_STRING:
                  case VALUE_NUMBER:
                      parsedJSON.append(parser.getString() + " ");
                     break;
               }
            }
            
            out.println("<html><head><title>Ping JSONP</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping JSONP</FONT><BR>Generated JSON: " + generatedJSON + "<br>Parsed JSON: " + parsedJSON + "</body></html>");
        } catch (Exception e) {
            Log.error(e, "PingJSONP.doGet(...): general exception caught");
            res.sendError(500, e.toString());

        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JSON generation and parsing in a servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(asyncSupported=true,name = "PingManagedExecutor", urlPatterns = { "/servlet/PingManagedExecutor" })
public class PingManagedExecutor extends HttpServlet{

	private static final long serialVersionUID = -4695386150928451234L;
	private static String initTime;
    private static int hitCount;

	@Resource 
	private ManagedExecutorService mes;
	
	 /**
     * forwards post requests to the doGet method Creation date: (03/18/2014
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    	final AsyncContext asyncContext = req.startAsync();
        final ServletOutputStream out = res.getOutputStream();
    	
    	try {
    		res.setContentType("text/html");
    		    		
    		out.println("<html><head><title>Ping ManagedExecutor</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping ManagedExecutor<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : " + initTime
                    + "<BR><BR></FONT>  </body></html>");
    		   		   		    	
    		// Runnable task
    		mes.submit(new Runnable() {
    			@Override
    			public void run() {
    				try {
						out.println("<b>HitCount: " + ++hitCount  +"</b><br/>");
					} catch (IOException e) {
						e.printStackTrace();
					}
    				asyncContext.complete();
    			}
    		});   		    		
    		
    			 
    	} catch (Exception e) {
			e.printStackTrace();
		}  
    }	
    		
    	
    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Tests a ManagedExecutor";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;
    }
	
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

@WebServlet(asyncSupported=true,name = "PingManagedThread", urlPatterns = { "/servlet/PingManagedThread" })
public class PingManagedThread extends HttpServlet{

	private static final long serialVersionUID = -4695386150928451234L;
	private static String initTime;
    private static int hitCount;

	@Resource 
	private ManagedThreadFactory managedThreadFactory;
	
	 /**
     * forwards post requests to the doGet method Creation date: (03/18/2014
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    	
		final AsyncContext asyncContext = req.startAsync();
		final ServletOutputStream out = res.getOutputStream();	
		
		try {
			
			res.setContentType("text/html");
					
			out.println("<html><head><title>Ping ManagedThread</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping ManagedThread<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : " + initTime + "<BR/><BR/></FONT>");			
		
			Thread thread = managedThreadFactory.newThread(new Runnable() {
    			@Override
    			public void run() {
    				try {
						out.println("<b>HitCount: " + ++hitCount  +"</b><br/>");
					} catch (IOException e) {
						e.printStackTrace();
					}
    				asyncContext.complete();
    			}
    		});   		    		
			
			thread.start();
		
		} catch (Exception e) {
			Log.error(e, "PingManagedThreadServlet.doGet(...): general exception caught");
			res.sendError(500, e.toString());
		}
		
	}
    
    
    
    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Tests a ManagedThread asynchronous servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }
	
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "PingReentryServlet", urlPatterns = { "/servlet/PingReentryServlet" })
public class PingReentryServlet extends HttpServlet {

    private static final long serialVersionUID = -2536027021580175706L;
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/html");

            // The following 2 lines are the difference between PingServlet and
            // PingServletWriter
            // the latter uses a PrintWriter for output versus a binary output
            // stream.
            ServletOutputStream out = res.getOutputStream();
            // java.io.PrintWriter out = res.getWriter();
            int numReentriesLeft; 
            int sleepTime;
            
            if(req.getParameter("numReentries") != null){
                numReentriesLeft = Integer.parseInt(req.getParameter("numReentries"));
            } else {
                numReentriesLeft = 0;
            }
            
            if(req.getParameter("sleep") != null){
                sleepTime = Integer.parseInt(req.getParameter("sleep"));
            } else {
                sleepTime = 0;
            }
                
            if(numReentriesLeft <= 0) {
                Thread.sleep(sleepTime);
                out.println(numReentriesLeft);
            } else {
                String hostname = req.getServerName();
                int port = req.getServerPort();
                req.getContextPath();
                int saveNumReentriesLeft = numReentriesLeft;
                int nextNumReentriesLeft = numReentriesLeft - 1;
                
                // Recursively call into the same server, decrementing the counter by 1.
                String url = "http://" +  hostname + ":" + port + "/" + req.getRequestURI() + 
                        "?numReentries=" +  nextNumReentriesLeft +
                        "&sleep=" + sleepTime;
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                //Append the recursion count to the response and return it.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
         
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                Thread.sleep(sleepTime);
                out.println(saveNumReentriesLeft + response.toString());
            }
        } catch (Exception e) {
            //Log.error(e, "PingReentryServlet.doGet(...): general exception caught");
            res.sendError(500, e.toString());

        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet tests fundamental dynamic HTML creation functionality through
 * server side servlet processing.
 *
 */

@WebServlet(name = "PingServlet", urlPatterns = { "/servlet/PingServlet" })
public class PingServlet extends HttpServlet {

	private static final long serialVersionUID = 7097023236709683760L;
	private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/html");

            // The following 2 lines are the difference between PingServlet and
            // PingServletWriter
            // the latter uses a PrintWriter for output versus a binary output
            // stream.
            ServletOutputStream out = res.getOutputStream();
            // java.io.PrintWriter out = res.getWriter();
            hitCount++;
            out.println("<html><head><title>Ping Servlet</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : " + initTime
                    + "<BR><BR></FONT>  <B>Hit Count: " + hitCount + "</B></body></html>");
        } catch (Exception e) {
            Log.error(e, "PingServlet.doGet(...): general exception caught");
            res.sendError(500, e.toString());

        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet2DB tests the path of a servlet making a JDBC connection to a
 * database
 *
 */

@WebServlet(name = "PingServlet2DB", urlPatterns = { "/servlet/PingServlet2DB" })
public class PingServlet2DB extends HttpServlet {

    private static final long serialVersionUID = -6456675185605592049L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        String symbol = null;
        StringBuffer output = new StringBuffer(100);

        try {
            // TradeJDBC uses prepared statements so I am going to make use of
            // it's code.
            TradeDirect trade = new TradeDirect();
            trade.getConnPublic();

            output.append("<html><head><title>PingServlet2DB.</title></head>"
                    + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2DB:</FONT><HR><FONT size=\"-1\" color=\"#000066\">Init time : " + initTime);
            hitCount++;
            output.append("<BR>Hit Count: " + hitCount);
            output.append("<HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingServlet2DB -- error getting connection to the database", symbol);
            res.sendError(500, "PingServlet2DB Exception caught: " + e.toString());
        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JDBC Read using a prepared statment, makes use of TradeJDBC class";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingServlet2Include tests servlet to servlet request dispatching. Servlet 1,
 * the controller, creates a new JavaBean object forwards the servlet request
 * with the JavaBean added to Servlet 2. Servlet 2 obtains access to the
 * JavaBean through the Servlet request object and provides the dynamic HTML
 * output based on the JavaBean data. PingServlet2Servlet is the initial servlet
 * that sends a request to {@link PingServlet2ServletRcv}
 *
 */
@WebServlet(name = "PingServlet2Include", urlPatterns = { "/servlet/PingServlet2Include" })
public class PingServlet2Include extends HttpServlet {

    private static final long serialVersionUID = 1063447780151198793L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            res.setContentType("text/html");

            int iter = TradeConfig.getPrimIterations();
            for (int ii = 0; ii < iter; ii++) {
                getServletConfig().getServletContext().getRequestDispatcher("/servlet/PingServlet2IncludeRcv").include(req, res);
            }

            // ServletOutputStream out = res.getOutputStream();
            java.io.PrintWriter out = res.getWriter();
            out.println("<html><head><title>Ping Servlet 2 Include</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet 2 Include<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
                    + initTime + "<BR><BR></FONT>  <B>Hit Count: " + hitCount++ + "</B></body></html>");
        } catch (Exception ex) {
            Log.error(ex, "PingServlet2Include.doGet(...): general exception");
            res.sendError(500, "PingServlet2Include.doGet(...): general exception" + ex.toString());
        }
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * PingServlet2Include tests servlet to servlet request dispatching. Servlet 1,
 * the controller, creates a new JavaBean object forwards the servlet request
 * with the JavaBean added to Servlet 2. Servlet 2 obtains access to the
 * JavaBean through the Servlet request object and provides the dynamic HTML
 * output based on the JavaBean data. PingServlet2Servlet is the initial servlet
 * that sends a request to {@link PingServlet2ServletRcv}
 *
 */
@WebServlet(name = "PingServlet2IncludeRcv", urlPatterns = { "/servlet/PingServlet2IncludeRcv" })
public class PingServlet2IncludeRcv extends HttpServlet {

    private static final long serialVersionUID = 2628801298561220872L;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // do nothing but get included by PingServlet2Include
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet2JNDI performs a basic JNDI lookup of a JDBC DataSource
 *
 */

@WebServlet(name = "PingServlet2JNDI", urlPatterns = { "/servlet/PingServlet2JNDI" })
public class PingServlet2JNDI extends HttpServlet {

    private static final long serialVersionUID = -8236271998141415347L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();

        StringBuffer output = new StringBuffer(100);

        try {
            output.append("<html><head><title>Ping JNDI -- lookup of JDBC DataSource</title></head>"
                    + "<body><HR><FONT size=\"+2\" color=\"#000066\">Ping JNDI -- lookup of JDBC DataSource</FONT><HR><FONT size=\"-1\" color=\"#000066\">Init time : "
                    + initTime);
            hitCount++;
            output.append("</FONT><BR>Hit Count: " + hitCount);
            output.append("<HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingServlet2JNDI -- error look up of a JDBC DataSource");
            res.sendError(500, "PingServlet2JNDI Exception caught: " + e.toString());
        }

    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JNDI look up of a JDBC DataSource";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet2JSP tests a call from a servlet to a JavaServer Page providing
 * server-side dynamic HTML through JSP scripting.
 *
 */
@WebServlet(name = "PingServlet2Jsp", urlPatterns = { "/servlet/PingServlet2Jsp" })
public class PingServlet2Jsp extends HttpServlet {
    private static final long serialVersionUID = -5199543766883932389L;
    private static int hitCount = 0;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PingBean ab;
        try {
            ab = new PingBean();
            hitCount++;
            ab.setMsg("Hit Count: " + hitCount);
            req.setAttribute("ab", ab);

            getServletConfig().getServletContext().getRequestDispatcher("/PingServlet2Jsp.jsp").forward(req, res);
        } catch (Exception ex) {
            Log.error(ex, "PingServlet2Jsp.doGet(...): request error");
            res.sendError(500, "PingServlet2Jsp.doGet(...): request error" + ex.toString());

        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet2PDF tests a call to a servlet which then loads a PDF document.
 *
 */
@WebServlet(name = "PingServlet2PDF", urlPatterns = { "/servlet/PingServlet2PDF" })
public class PingServlet2PDF extends HttpServlet {

    private static final long serialVersionUID = -1321793174442755868L;
    private static int hitCount = 0;
    private static final int BUFFER_SIZE = 1024 * 8; // 8 KB

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PingBean ab;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            ab = new PingBean();
            hitCount++;
            ab.setMsg("Hit Count: " + hitCount);
            req.setAttribute("ab", ab);

            ServletOutputStream out = res.getOutputStream();

            // MIME type for pdf doc
            res.setContentType("application/pdf");

            // Open an InputStream to the PDF document
            String fileURL = "http://localhost:9080/daytrader/WAS_V7_64-bit_performance.pdf";
            URL url = new URL(fileURL);
            URLConnection conn = url.openConnection();
            bis = new BufferedInputStream(conn.getInputStream());

            // Transfer the InputStream (PDF Document) to OutputStream (servlet)
            bos = new BufferedOutputStream(out);
            byte[] buff = new byte[BUFFER_SIZE];
            int bytesRead;
            // Simple read/write loop.
            while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                bos.write(buff, 0, bytesRead);
            }

        } catch (Exception ex) {
            Log.error(ex, "PingServlet2Jsp.doGet(...): request error");
            res.sendError(500, "PingServlet2Jsp.doGet(...): request error" + ex.toString());

        }

        finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.close();
            }
        }

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet2Servlet tests servlet to servlet request dispatching. Servlet 1,
 * the controller, creates a new JavaBean object forwards the servlet request
 * with the JavaBean added to Servlet 2. Servlet 2 obtains access to the
 * JavaBean through the Servlet request object and provides the dynamic HTML
 * output based on the JavaBean data. PingServlet2Servlet is the initial servlet
 * that sends a request to {@link PingServlet2ServletRcv}
 *
 */
@WebServlet(name = "PingServlet2Servlet", urlPatterns = { "/servlet/PingServlet2Servlet" })
public class PingServlet2Servlet extends HttpServlet {
    private static final long serialVersionUID = -955942781902636048L;
    private static int hitCount = 0;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PingBean ab;
        try {
            ab = new PingBean();
            hitCount++;
            ab.setMsg("Hit Count: " + hitCount);
            req.setAttribute("ab", ab);

            getServletConfig().getServletContext().getRequestDispatcher("/servlet/PingServlet2ServletRcv").forward(req, res);
        } catch (Exception ex) {
            Log.error(ex, "PingServlet2Servlet.doGet(...): general exception");
            res.sendError(500, "PingServlet2Servlet.doGet(...): general exception" + ex.toString());

        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet2Servlet tests servlet to servlet request dispatching. Servlet 1,
 * the controller, creates a new JavaBean object forwards the servlet request
 * with the JavaBean added to Servlet 2. Servlet 2 obtains access to the
 * JavaBean through the Servlet request object and provides the dynamic HTML
 * output based on the JavaBean data. PingServlet2ServletRcv receives a request
 * from {@link PingServlet2Servlet} and displays output.
 *
 */
@WebServlet(name = "PingServlet2ServletRcv", urlPatterns = { "/servlet/PingServlet2ServletRcv" })
public class PingServlet2ServletRcv extends HttpServlet {
    private static final long serialVersionUID = -5241563129216549706L;
    private static String initTime = null;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PingBean ab;
        try {
            ab = (PingBean) req.getAttribute("ab");
            res.setContentType("text/html");
            PrintWriter out = res.getWriter();
            out.println("<html><head><title>Ping Servlet2Servlet</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">PingServlet2Servlet:<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time: "
                    + initTime + "</FONT><BR><BR><B>Message from Servlet: </B>" + ab.getMsg() + "</body></html>");
        } catch (Exception ex) {
            Log.error(ex, "PingServlet2ServletRcv.doGet(...): general exception");
            res.sendError(500, "PingServlet2ServletRcv.doGet(...): general exception" + ex.toString());
        }

    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet31Async tests fundamental dynamic HTML creation functionality through
 * server side servlet processing asynchronously.
 *
 */

@WebServlet(name = "PingServlet30Async", urlPatterns = { "/servlet/PingServlet30Async" }, asyncSupported=true)
public class PingServlet30Async extends HttpServlet {

    private static final long serialVersionUID = 8731300373855056660L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
                        
        AsyncContext ac = req.startAsync();
        StringBuilder sb = new StringBuilder();
        
        ServletInputStream input = req.getInputStream();
        byte[] b = new byte[1024];
        int len = -1;
        while ((len = input.read(b)) != -1) {
            String data = new String(b, 0, len);
            sb.append(data);
        }

        ServletOutputStream output = res.getOutputStream();
        
        output.println("<html><head><title>Ping Servlet 3.0 Async</title></head>"
                + "<body><hr/><br/><font size=\"+2\" color=\"#000066\">Ping Servlet 3.0 Async</font><br/>"
                + "<font size=\"+1\" color=\"#000066\">Init time : " + initTime
                + "</font><br/><br/><b>Hit Count: " + ++hitCount + "</b><br/>Data Received: "+ sb.toString() + "</body></html>");
        
        ac.complete();
    }       


    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req,res);
          
    }
    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet31Async tests fundamental dynamic HTML creation functionality through
 * server side servlet processing asynchronously with non-blocking i/o.
 *
 */

@WebServlet(name = "PingServlet31Async", urlPatterns = { "/servlet/PingServlet31Async" }, asyncSupported=true)
public class PingServlet31Async extends HttpServlet {

    private static final long serialVersionUID = 8731300373855056660L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
                
        AsyncContext ac = req.startAsync();
           
        ServletInputStream input = req.getInputStream();
        ReadListener readListener = new ReadListenerImpl(input, res, ac);
        input.setReadListener(readListener);
    }

    class ReadListenerImpl implements ReadListener {
        private ServletInputStream input = null;
        private HttpServletResponse res = null;
        private AsyncContext ac = null;
        private Queue<String> queue = new LinkedBlockingQueue<String>();

        ReadListenerImpl(ServletInputStream in, HttpServletResponse r, AsyncContext c) {
            input = in;
            res = r;
            ac = c;
        }
    
        public void onDataAvailable() throws IOException {
            StringBuilder sb = new StringBuilder();
            int len = -1;
            byte b[] = new byte[1024];
            
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                sb.append(data);
            }
            queue.add(sb.toString());
            
        }
    
        public void onAllDataRead() throws IOException {
            ServletOutputStream output = res.getOutputStream();
            WriteListener writeListener = new WriteListenerImpl(output, queue, ac);
            output.setWriteListener(writeListener);
        }
    
        public void onError(final Throwable t) {
            ac.complete();
            t.printStackTrace();
        }
    }
    
    class WriteListenerImpl implements WriteListener {
        private ServletOutputStream output = null;
        private Queue<String> queue = null;
        private AsyncContext ac = null;

        WriteListenerImpl(ServletOutputStream sos, Queue<String> q, AsyncContext c) {
            output = sos;
            queue = q;
            ac = c;
            
            try {
                output.print("<html><head><title>Ping Servlet 3.1 Async</title></head>"
                        + "<body><hr/><br/><font size=\"+2\" color=\"#000066\">Ping Servlet 3.1 Async</font>"
                        + "<br/><font size=\"+1\" color=\"#000066\">Init time : " + initTime
                        + "</font><br/><br/><b>Hit Count: " + ++hitCount + "</b><br/>Data Received: ");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onWritePossible() throws IOException {
            
            while (queue.peek() != null && output.isReady()) {
                String data = (String) queue.poll();
                output.print(data);
            }
            
            if (queue.peek() == null) {
                output.println("</body></html>");
                ac.complete();
            }
        }

        public void onError(final Throwable t) {
            ac.complete();
            t.printStackTrace();
        }
    }
        


    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req,res);          
    }
    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */  

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet31Async tests fundamental dynamic HTML creation functionality through
 * server side servlet processing asynchronously with non-blocking i/o.
 *
 */

@WebServlet(name = "PingServlet31AsyncRead", urlPatterns = { "/servlet/PingServlet31AsyncRead" }, asyncSupported=true)
public class PingServlet31AsyncRead extends HttpServlet {

    private static final long serialVersionUID = 8731300373855056660L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
                
        AsyncContext ac = req.startAsync();
           
        ServletInputStream input = req.getInputStream();
        ReadListener readListener = new ReadListenerImpl(input, res, ac);
        input.setReadListener(readListener);
    }

    class ReadListenerImpl implements ReadListener {
        private ServletInputStream input = null;
        private HttpServletResponse res = null;
        private AsyncContext ac = null;
        private StringBuilder sb = new StringBuilder();

        ReadListenerImpl(ServletInputStream in, HttpServletResponse r, AsyncContext c) {
            input = in;
            res = r;
            ac = c;
        }
    
        public void onDataAvailable() throws IOException {
            
            int len = -1;
            byte b[] = new byte[1024];
            
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                sb.append(data);
            }
            
            
        }
    
        public void onAllDataRead() throws IOException {
            ServletOutputStream output = res.getOutputStream();
            output.println("<html><head><title>Ping Servlet 3.1 Async</title></head>"
                    + "<body><hr/><br/><font size=\"+2\" color=\"#000066\">Ping Servlet 3.1 AsyncRead</font>"
                    + "<br/><font size=\"+1\" color=\"#000066\">Init time : " + initTime
                    + "</font><br/><br/><b>Hit Count: " + ++hitCount + "</b><br/>Data Received: " + sb.toString() + "</body></html>");
            ac.complete();
        }
    
        public void onError(final Throwable t) {
            ac.complete();
            t.printStackTrace();
        }
    }
        


    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doPost(req,res);          
    }
    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.web.prims.PingCDIBean;

@WebServlet("/servlet/PingServletCDI")
public class PingServletCDI extends HttpServlet {

    private static final long serialVersionUID = -1803544618879689949L;
    private static String initTime;

    @Inject
    PingCDIBean cdiBean;
    
    @EJB
    PingEJBIFace ejb;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write("<html><head><title>Ping Servlet CDI</title></head>"
                 + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet CDI<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : " + initTime
                 + "<BR><BR></FONT>");

        pw.write("<B>hitCount: " + cdiBean.hello() + "</B><BR>");
        pw.write("<B>hitCount: " + ejb.getMsg() + "</B><BR>");

        pw.flush();
        pw.close();

    }

    /**
     * called when the class is loaded to initialize the servlet
     * 
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();

    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.web.prims.PingCDIBean;

@WebServlet("/servlet/PingServletCDIBeanManagerViaCDICurrent")
public class PingServletCDIBeanManagerViaCDICurrent extends HttpServlet {

    private static final long serialVersionUID = -1803544618879689949L;
    private static String initTime;

    @Inject
    PingCDIBean cdiBean;

    
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write("<html><head><title>Ping Servlet CDI Bean Manager</title></head>"
                 + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet CDI Bean Manager<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : " + initTime
                 + "<BR><BR></FONT>");

        try {
            pw.write("<B>hitCount: " + cdiBean.getBeanMangerViaCDICurrent() + "</B></body></html>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        pw.flush();
        pw.close();

    }

    /**
     * called when the class is loaded to initialize the servlet
     * 
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();

    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.web.prims.PingCDIBean;

@WebServlet("/servlet/PingServletCDIBeanManagerViaJNDI")
public class PingServletCDIBeanManagerViaJNDI extends HttpServlet {

    private static final long serialVersionUID = -1803544618879689949L;
    private static String initTime;

    @Inject
    PingCDIBean cdiBean;

    
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write("<html><head><title>Ping Servlet CDI Bean Manager</title></head>"
                 + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet CDI Bean Manager<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : " + initTime
                 + "<BR><BR></FONT>");

        try {
            pw.write("<B>hitCount: " + cdiBean.getBeanMangerViaJNDI() + "</B></body></html>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        pw.flush();
        pw.close();

    }

    /**
     * called when the class is loaded to initialize the servlet
     * 
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initTime = new java.util.Date().toString();

    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * PingServletSetContentLength tests fundamental dynamic HTML creation
 * functionality through server side servlet processing.
 *
 */

@WebServlet(name = "PingServletLargeContentLength", urlPatterns = { "/servlet/PingServletLargeContentLength" })
public class PingServletLargeContentLength extends HttpServlet {



    /**
     * 
     */
    private static final long serialVersionUID = -7979576220528252408L;

    /**
     * forwards post requests to the doGet method Creation date: (02/07/2013
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        System.out.println("Length: " + req.getContentLengthLong());
        
        
        
        
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
       doPost(req,res);    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet, with " + "contentLength set by contentLength parameter.";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServletSetContentLength tests fundamental dynamic HTML creation
 * functionality through server side servlet processing.
 *
 */

@WebServlet(name = "PingServletSetContentLength", urlPatterns = { "/servlet/PingServletSetContentLength" })
public class PingServletSetContentLength extends HttpServlet {

    private static final long serialVersionUID = 8731300373855056661L;

    /**
     * forwards post requests to the doGet method Creation date: (02/07/2013
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/html");
            String lengthParam = req.getParameter("contentLength");
            Integer length;

            if (lengthParam == null) {
                length = 0;
            } else {
                length = Integer.parseInt(lengthParam);
            }

            ServletOutputStream out = res.getOutputStream();

            // Add characters (a's) to the SOS to equal the requested length
            // 167 is the smallest length possible.
            
            int i = 0;
            String buffer = "";

            while (i + 167 < length) {
                buffer = buffer + "a";
                i++;
            }

            out.println("<html><head><title>Ping Servlet</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet<BR></FONT><FONT size=\"+1\" color=\"#000066\">" + buffer
                    + "</B></body></html>");
        } catch (Exception e) {
            Log.error(e, "PingServlet.doGet(...): general exception caught");
            res.sendError(500, e.toString());

        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet, with " + "contentLength set by contentLength parameter.";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingServlet extends PingServlet by using a PrintWriter for formatted output
 * vs. the output stream used by {@link PingServlet}.
 *
 */
@WebServlet(name = "PingServletWriter", urlPatterns = { "/servlet/PingServletWriter" })
public class PingServletWriter extends HttpServlet {

    private static final long serialVersionUID = -267847365014523225L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/html");

            // The following 2 lines are the difference between PingServlet and
            // PingServletWriter
            // the latter uses a PrintWriter for output versus a binary output
            // stream.
            // ServletOutputStream out = res.getOutputStream();
            java.io.PrintWriter out = res.getWriter();
            hitCount++;
            out.println("<html><head><title>Ping Servlet Writer</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet Writer:<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
                    + initTime + "<BR><BR></FONT>  <B>Hit Count: " + hitCount + "</B></body></html>");
        } catch (Exception e) {
            Log.error(e, "PingServletWriter.doGet(...): general exception caught");
            res.sendError(500, e.toString());
        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/

    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet using a PrintWriter";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingHTTPSession1 - SessionID tests fundamental HTTP session functionality by
 * creating a unique session ID for each individual user. The ID is stored in
 * the users session and is accessed and displayed on each user request.
 *
 */
@WebServlet(name = "PingSession1", urlPatterns = { "/servlet/PingSession1" })
public class PingSession1 extends HttpServlet {
    private static final long serialVersionUID = -3703858656588519807L;
    private static int count;
    // For each new session created, add a session ID of the form "sessionID:" +
    // count
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = null;
        try {
            try {
                // get the users session, if the user does not have a session
                // create one.
                session = request.getSession(true);
            } catch (Exception e) {
                Log.error(e, "PingSession1.doGet(...): error getting session");
                // rethrow the exception for handling in one place.
                throw e;
            }

            // Get the session data value
            Integer ival = (Integer) session.getAttribute("sessiontest.counter");
            // if their is not a counter create one.
            if (ival == null) {
                ival = new Integer(count++);
                session.setAttribute("sessiontest.counter", ival);
            }
            String SessionID = "SessionID:" + ival.toString();

            // Output the page
            response.setContentType("text/html");
            response.setHeader("SessionKeyTest-SessionID", SessionID);

            PrintWriter out = response.getWriter();
            out.println("<html><head><title>HTTP Session Key Test</title></head><body><HR><BR><FONT size=\"+2\" color=\"#000066\">HTTP Session Test 1: Session Key<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time: "
                    + initTime + "</FONT><BR><BR>");
            hitCount++;
            out.println("<B>Hit Count: " + hitCount + "<BR>Your HTTP Session key is " + SessionID + "</B></body></html>");
        } catch (Exception e) {
            // log the excecption
            Log.error(e, "PingSession1.doGet(..l.): error.");
            // set the server responce to 500 and forward to the web app defined
            // error page
            response.sendError(500, "PingSession1.doGet(...): error. " + e.toString());
        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/

    @Override
    public String getServletInfo() {
        return "HTTP Session Key: Tests management of a read only unique id";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        count = 0;
        hitCount = 0;
        initTime = new java.util.Date().toString();

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingHTTPSession2 session create/destroy further extends the previous test by
 * invalidating the HTTP Session on every 5th user access. This results in
 * testing HTTPSession create and destroy
 *
 */
@WebServlet(name = "PingSession2", urlPatterns = { "/servlet/PingSession2" })
public class PingSession2 extends HttpServlet {

    private static final long serialVersionUID = -273579463475455800L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = null;
        try {
            try {
                session = request.getSession(true);
            } catch (Exception e) {
                Log.error(e, "PingSession2.doGet(...): error getting session");
                // rethrow the exception for handling in one place.
                throw e;

            }

            // Get the session data value
            Integer ival = (Integer) session.getAttribute("sessiontest.counter");
            // if there is not a counter then create one.
            if (ival == null) {
                ival = new Integer(1);
            } else {
                ival = new Integer(ival.intValue() + 1);
            }
            session.setAttribute("sessiontest.counter", ival);
            // if the session count is equal to five invalidate the session
            if (ival.intValue() == 5) {
                session.invalidate();
            }

            try {
                // Output the page
                response.setContentType("text/html");
                response.setHeader("SessionTrackingTest-counter", ival.toString());

                PrintWriter out = response.getWriter();
                out.println("<html><head><title>Session Tracking Test 2</title></head><body><HR><BR><FONT size=\"+2\" color=\"#000066\">HTTP Session Test 2: Session create/invalidate <BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time: "
                        + initTime + "</FONT><BR><BR>");
                hitCount++;
                out.println("<B>Hit Count: " + hitCount + "<BR>Session hits: " + ival + "</B></body></html>");
            } catch (Exception e) {
                Log.error(e, "PingSession2.doGet(...): error getting session information");
                // rethrow the exception for handling in one place.
                throw e;
            }

        }

        catch (Exception e) {
            // log the excecption
            Log.error(e, "PingSession2.doGet(...): error.");
            // set the server responce to 500 and forward to the web app defined
            // error page
            response.sendError(500, "PingSession2.doGet(...): error. " + e.toString());
        }
    } // end of the method

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "HTTP Session Key: Tests management of a read/write unique id";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *
 * PingHTTPSession3 tests the servers ability to manage and persist large
 * HTTPSession data objects. The servlet creates the large custom java object
 * {@link PingSession3Object}. This large session object is retrieved and stored
 * to the session on each user request. The default settings result in approx
 * 2024 bits being retrieved and stored upon each request.
 *
 */
@WebServlet(name = "PingSession3", urlPatterns = { "/servlet/PingSession3" })
public class PingSession3 extends HttpServlet {
    private static final long serialVersionUID = -6129599971684210414L;
    private static int NUM_OBJECTS = 2;
    private static String initTime = null;
    private static int hitCount = 0;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *            javax.servlet.http.HttpServletRequest
     * @param res2
     *            javax.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *            HttpServletRequest
     * @param responce
     *            HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        // Using a StringBuffer to output all at once.
        StringBuffer outputBuffer = new StringBuffer();
        HttpSession session = null;
        PingSession3Object[] sessionData;
        response.setContentType("text/html");

        // this is a general try/catch block. The catch block at the end of this
        // will forward the responce
        // to an error page if there is an exception
        try {

            try {
                session = request.getSession(true);
            } catch (Exception e) {
                Log.error(e, "PingSession3.doGet(...): error getting session");
                // rethrow the exception for handling in one place.
                throw e;

            }
            // Each PingSession3Object in the PingSession3Object array is 1K in
            // size
            // NUM_OBJECTS sets the size of the array to allocate and thus set
            // the size in KBytes of the session object
            // NUM_OBJECTS can be initialized by the servlet
            // Here we check for the request parameter to change the size and
            // invalidate the session if it exists
            // NOTE: Current user sessions will remain the same (i.e. when
            // NUM_OBJECTS is changed, all user thread must be restarted
            // for the change to fully take effect

            String num_objects;
            if ((num_objects = request.getParameter("num_objects")) != null) {
                // validate input
                try {
                    int x = Integer.parseInt(num_objects);
                    if (x > 0) {
                        NUM_OBJECTS = x;
                    }
                } catch (Exception e) {
                    Log.error(e, "PingSession3.doGet(...): input should be an integer, input=" + num_objects);
                } // revert to current value on exception

                outputBuffer.append("<html><head> Session object size set to " + NUM_OBJECTS + "K bytes </head><body></body></html>");
                if (session != null) {
                    session.invalidate();
                }
                out.print(outputBuffer.toString());
                out.close();
                return;
            }

            // Get the session data value
            sessionData = (PingSession3Object[]) session.getAttribute("sessiontest.sessionData");
            if (sessionData == null) {
                sessionData = new PingSession3Object[NUM_OBJECTS];
                for (int i = 0; i < NUM_OBJECTS; i++) {
                    sessionData[i] = new PingSession3Object();
                }
            }

            session.setAttribute("sessiontest.sessionData", sessionData);

            // Each PingSession3Object is about 1024 bits, there are 8 bits in a
            // byte.
            int num_bytes = (NUM_OBJECTS * 1024) / 8;
            response.setHeader("SessionTrackingTest-largeSessionData", num_bytes + "bytes");

            outputBuffer
                    .append("<html><head><title>Session Large Data Test</title></head><body><HR><BR><FONT size=\"+2\" color=\"#000066\">HTTP Session Test 3: Large Data<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time: ")
                    .append(initTime).append("</FONT><BR><BR>");
            hitCount++;
            outputBuffer.append("<B>Hit Count: ").append(hitCount)
                    .append("<BR>Session object updated. Session Object size = " + num_bytes + " bytes </B></body></html>");
            // output the Buffer to the printWriter.
            out.println(outputBuffer.toString());

        } catch (Exception e) {
            // log the excecption
            Log.error(e, "PingSession3.doGet(..l.): error.");
            // set the server responce to 500 and forward to the web app defined
            // error page
            response.sendError(500, "PingSession3.doGet(...): error. " + e.toString());
        }
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "HTTP Session Object: Tests management of a large custom session class";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *            ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();

    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Serializable;

/**
 *
 * An object that contains approximately 1024 bits of information. This is used
 * by {@link PingSession3}
 *
 */
public class PingSession3Object implements Serializable {
    // PingSession3Object represents a BLOB of session data of various.
    // Each instantiation of this class is approximately 1K in size (not
    // including overhead for arrays and Strings)
    // Using different datatype exercises the various serialization algorithms
    // for each type

    private static final long serialVersionUID = 1452347702903504717L;
    byte[] byteVal = new byte[16]; // 8 * 16 = 128 bits
    char[] charVal = new char[8]; // 16 * 8 = 128 bits
    int a, b, c, d; // 4 * 32 = 128 bits
    float e, f, g, h; // 4 * 32 = 128 bits
    double i, j; // 2 * 64 = 128 bits
    // Primitive type size = ~5*128= 640

    String s1 = new String("123456789012");
    String s2 = new String("abcdefghijkl");

    // String type size = ~2*12*16 = 384
    // Total blob size (w/o overhead) = 1024

    // The Session blob must be filled with data to avoid compression of the
    // blob during serialization
    PingSession3Object() {
        int index;
        byte b = 0x8;
        for (index = 0; index < 16; index++) {
            byteVal[index] = (byte) (b + 2);
        }

        char c = 'a';
        for (index = 0; index < 8; index++) {
            charVal[index] = (char) (c + 2);
        }

        a = 1;
        b = 2;
        c = 3;
        d = 5;
        e = (float) 7.0;
        f = (float) 11.0;
        g = (float) 13.0;
        h = (float) 17.0;
        i = 19.0;
        j = 23.0;
    }
    /**
     * Main method to test the serialization of the Session Data blob object
     * Creation date: (4/3/2000 3:07:34 PM)
     *
     * @param args
     *            java.lang.String[]
     */

    /**
     * Since the following main method were written for testing purpose, we
     * comment them out public static void main(String[] args) { try {
     * PingSession3Object data = new PingSession3Object();
     *
     * FileOutputStream ostream = new
     * FileOutputStream("c:\\temp\\datablob.xxx"); ObjectOutputStream p = new
     * ObjectOutputStream(ostream); p.writeObject(data); p.flush();
     * ostream.close(); } catch (Exception e) { System.out.println("Exception: "
     * + e.toString()); } }
     */

}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.samples.daytrader.util.Log;

@WebServlet(name = "PingUpgradeServlet", urlPatterns = { "/servlet/PingUpgradeServlet" }, asyncSupported=true)
public class PingUpgradeServlet extends HttpServlet {
    private static final long serialVersionUID = -6955518532146927509L;
   

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        doPost(req,res);
    }
    
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
      
        if (Log.doTrace()) {
            Log.trace("PingUpgradeServlet:doPost");
        }
        
        if ("echo".equals(req.getHeader("Upgrade"))) {
            
            if (Log.doTrace()) {
                Log.trace("PingUpgradeServlet:doPost -- found echo, doing upgrade");
            }
            
            res.setStatus(101);
            res.setHeader("Upgrade", "echo");
            res.setHeader("Connection", "Upgrade");

            req.upgrade(Handler.class);          
          
        } else {
            
            if (Log.doTrace()) {
                Log.trace("PingUpgradeServlet:doPost -- did not find echo, no upgrade");
            }
            
            res.getWriter().println("No upgrade: " + req.getHeader("Upgrade"));
        }
    }

    public static class Handler implements HttpUpgradeHandler {
    
        @Override
        public void init(final WebConnection wc) {
            Listener listener = null;
            try {
                listener = new Listener(wc);
              
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
      
            try {
                
                if (Log.doTrace()) {
                    Log.trace("PingUpgradeServlet$Handler.init() -- Initializing Handler");
                }
          
                // flush headers if any
                wc.getOutputStream().flush();
                wc.getInputStream().setReadListener(listener);
        
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void destroy() {
            if (Log.doTrace()) {
                Log.trace("PingUpgradeServlet$Handler.destroy() -- Destroying Handler");
            }
        }
    }

    private static class Listener implements ReadListener {
        private final WebConnection connection;
        private ServletInputStream input = null;
        private ServletOutputStream output = null;
        
        private Listener(final WebConnection connection) throws IOException  {
            this.connection = connection;
            this.input = connection.getInputStream();
            this.output = connection.getOutputStream();
        }

        @Override
        public void onDataAvailable() throws IOException {
            
            if (Log.doTrace()) {
                Log.trace("PingUpgradeServlet$Listener.onDataAvailable() called");
            }
            
            byte[] data = new byte[1024];
            int len = -1;
            
            while (input.isReady()  && (len = input.read(data)) != -1) {
                    String dataRead = new String(data, 0, len);
                    
                    if (Log.doTrace()) {
                        Log.trace("PingUpgradeServlet$Listener.onDataAvailable() -- Adding data to queue -->" + dataRead + "<--");
                    }
                    
                    output.println(dataRead);
                    output.flush();
            }
            
            closeConnection();
        }

        private void closeConnection() {
            try {
                connection.close();
            } catch (Exception e) {
                if (Log.doTrace()) {
                    Log.error(e.toString());
                }
            }
        }
        
        
        @Override
        public void onAllDataRead() throws IOException {
            closeConnection();
        }

        @Override
        public void onError(final Throwable t) {
            closeConnection();
        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/** This class a simple websocket that echos the binary it has been sent. */

@ServerEndpoint(value = "/pingBinary")
public class PingWebSocketBinary {

    private Session currentSession = null;
   
    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        currentSession = session;
    }

    @OnMessage
    public void ping(ByteBuffer data) {       
        currentSession.getAsyncRemote().sendBinary(data);
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.IOException;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ibm.websphere.samples.daytrader.web.websocket.JsonDecoder;
import com.ibm.websphere.samples.daytrader.web.websocket.JsonEncoder;
import com.ibm.websphere.samples.daytrader.web.websocket.JsonMessage;

/** This class a simple websocket that sends the number of times it has been pinged. */

@ServerEndpoint(value = "/pingWebSocketJson",encoders=JsonEncoder.class ,decoders=JsonDecoder.class)
public class PingWebSocketJson {

    private Session currentSession = null;
    private Integer sentHitCount = null;
    private Integer receivedHitCount = null;
       
    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        currentSession = session;
        sentHitCount = 0;
        receivedHitCount = 0;
        
        
        InitialContext context;
        ManagedThreadFactory mtf = null;
        
        try {
            context = new InitialContext();
            mtf = (ManagedThreadFactory) context.lookup("java:comp/DefaultManagedThreadFactory");
        
        } catch (NamingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        Thread thread = mtf.newThread(new Runnable() {

            @Override
            public void run() {
                
                try {
                
                    Thread.sleep(500);
                    
                    while (currentSession.isOpen()) {
                        sentHitCount++;
                    
                        JsonMessage response = new JsonMessage();
                        response.setKey("sentHitCount");
                        response.setValue(sentHitCount.toString());
                        currentSession.getAsyncRemote().sendObject(response);

                        Thread.sleep(100);
                    }
                    
                           
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
                
        });
        
        thread.start();
        
    }

    @OnMessage
    public void ping(JsonMessage message) throws IOException {
        receivedHitCount++;
        JsonMessage response = new JsonMessage();
        response.setKey("receivedHitCount");
        response.setValue(receivedHitCount.toString());
        currentSession.getAsyncRemote().sendObject(response);
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
       
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/** This class a simple websocket that sends the number of times it has been pinged. */

@ServerEndpoint(value = "/pingTextAsync")
public class PingWebSocketTextAsync {

    private Session currentSession = null;
    private Integer hitCount = null;
   
    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        currentSession = session;
        hitCount = 0;
    }

    @OnMessage
    public void ping(String text) {

        hitCount++;
        currentSession.getAsyncRemote().sendText(hitCount.toString());
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
     
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/** This class a simple websocket that sends the number of times it has been pinged. */

@ServerEndpoint(value = "/pingTextSync")
public class PingWebSocketTextSync {

    private Session currentSession = null;
    private Integer hitCount = null;
   
    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        currentSession = session;
        hitCount = 0;
    }

    @OnMessage
    public void ping(String text) {
        hitCount++;
    
        try {
            currentSession.getBasicRemote().sendText(hitCount.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
               
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * Primitive designed to run within the TradeApplication and makes use of
 * {@link trade_client.TradeConfig} for config parameters and random stock
 * symbols. Servlet will generate a random stock symbol and get the price of
 * that symbol using a {@link trade.Quote} Entity EJB This tests the common path
 * of a Servlet calling an Entity EJB to get data
 *
 */

public class PingServlet2Entity extends HttpServlet {
    private static final long serialVersionUID = -9004026114063894842L;

    private static String initTime;

    private static int hitCount;

    @PersistenceContext(unitName = "daytrader")
    private EntityManager em;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();

        QuoteDataBean quote = null;
        String symbol = null;

        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>Servlet2Entity</title></head>" + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2Entity<BR></FONT>"
                + "<FONT size=\"-1\" color=\"#000066\"><BR>PingServlet2Entity accesses an EntityManager"
                + " using a PersistenceContext annotaion and then gets the price of a random symbol (generated by TradeConfig)"
                + " through the EntityManager find method");
        try {
            // generate random symbol
            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    // get a random symbol to look up and get the key to that
                    // symbol.
                    symbol = TradeConfig.rndSymbol();
                    // find the EntityInstance.
                    quote = em.find(QuoteDataBean.class, symbol);
                }
            } catch (Exception e) {
                Log.error("web_primtv.PingServlet2Entity.doGet(...): error performing find");
                throw e;
            }
            // get the price and print the output.

            output.append("<HR>initTime: " + initTime + "<BR>Hit Count: ").append(hitCount++);
            output.append("<HR>Quote Information<BR><BR> " + quote.toHTML());
            output.append("</font><HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingServlet2Entity.doGet(...): error");
            // this will send an Error to teh web applications defined error
            // page.
            res.sendError(500, "PingServlet2Entity.doGet(...): error" + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Entity EJB path";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * This primitive is designed to run inside the TradeApplication and relies upon
 * the {@link com.ibm.websphere.samples.daytrader.util.TradeConfig} class to set
 * configuration parameters. PingServlet2MDBQueue tests key functionality of a
 * servlet call to a post a message to an MDB Queue. The TradeBrokerMDB receives
 * the message This servlet makes use of the MDB EJB
 * {@link com.ibm.websphere.samples.daytrader.ejb3.DTBroker3MDB} by posting a
 * message to the MDB Queue
 */
@WebServlet(name = "ejb3.PingServlet2MDBQueue", urlPatterns = { "/ejb3/PingServlet2MDBQueue" })
public class PingServlet2MDBQueue extends HttpServlet {

    private static final long serialVersionUID = 2637271552188745216L;

    private static String initTime;

    private static int hitCount;

    @Resource(name = "jms/QueueConnectionFactory")
    private ConnectionFactory queueConnectionFactory;

    @Resource(name = "jms/BrokerQueue")
    private Queue tradeBrokerQueue;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        // use a stringbuffer to avoid concatenation of Strings
        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>PingServlet2MDBQueue</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2MDBQueue<BR></FONT>" + "<FONT size=\"-1\" color=\"#000066\">"
                + "Tests the basic operation of a servlet posting a message to an EJB MDB through a JMS Queue.<BR>"
                + "<FONT color=\"red\"><B>Note:</B> Not intended for performance testing.</FONT>");

        try {
            Connection conn = queueConnectionFactory.createConnection();

            try {
                TextMessage message = null;
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    /*Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        MessageProducer producer = sess.createProducer(tradeBrokerQueue);

                        message = sess.createTextMessage();

                        String command = "ping";
                        message.setStringProperty("command", command);
                        message.setLongProperty("publishTime", System.currentTimeMillis());
                        message.setText("Ping message for queue java:comp/env/jms/TradeBrokerQueue sent from PingServlet2MDBQueue at " + new java.util.Date());
                        producer.send(message);
                    } finally {
                        sess.close();
                    }*/
                	
                	JMSContext context = queueConnectionFactory.createContext();
            		
            		message = context.createTextMessage();

            		message.setStringProperty("command", "ping");
                    message.setLongProperty("publishTime", System.currentTimeMillis());
                    message.setText("Ping message for queue java:comp/env/jms/TradeBrokerQueue sent from PingServlet2MDBQueue at " + new java.util.Date());
              		
            		context.createProducer().send(tradeBrokerQueue, message);
                }

                // write out the output
                output.append("<HR>initTime: ").append(initTime);
                output.append("<BR>Hit Count: ").append(hitCount++);
                output.append("<HR>Posted Text message to java:comp/env/jms/TradeBrokerQueue destination");
                output.append("<BR>Message: ").append(message);
                output.append("<BR><BR>Message text: ").append(message.getText());
                output.append("<BR><HR></FONT></BODY></HTML>");
                out.println(output.toString());

            } catch (Exception e) {
                Log.error("PingServlet2MDBQueue.doGet(...):exception posting message to TradeBrokerQueue destination ");
                throw e;
            } finally {
                conn.close();
            }
        } // this is where I actually handle the exceptions
        catch (Exception e) {
            Log.error(e, "PingServlet2MDBQueue.doGet(...): error");
            res.sendError(500, "PingServlet2MDBQueue.doGet(...): error, " + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, configured with trade runtime configs, tests Servlet to Session EJB path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * This primitive is designed to run inside the TradeApplication and relies upon
 * the {@link com.ibm.websphere.samples.daytrader.util.TradeConfig} class to set
 * configuration parameters. PingServlet2MDBQueue tests key functionality of a
 * servlet call to a post a message to an MDB Topic. The TradeStreamerMDB (and
 * any other subscribers) receives the message This servlet makes use of the MDB
 * EJB {@link com.ibm.websphere.samples.daytrader.ejb3.DTStreamer3MDB} by
 * posting a message to the MDB Topic
 */
@WebServlet(name = "ejb3.PingServlet2MDBTopic", urlPatterns = { "/ejb3/PingServlet2MDBTopic" })
public class PingServlet2MDBTopic extends HttpServlet {

    private static final long serialVersionUID = 5925470158886928225L;

    private static String initTime;

    private static int hitCount;

    @Resource(name = "jms/TopicConnectionFactory")
    private ConnectionFactory topicConnectionFactory;

    @Resource(name = "jms/StreamerTopic")
    private Topic tradeStreamerTopic;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        // use a stringbuffer to avoid concatenation of Strings
        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>PingServlet2MDBTopic</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2MDBTopic<BR></FONT>" + "<FONT size=\"-1\" color=\"#000066\">"
                + "Tests the basic operation of a servlet posting a message to an EJB MDB (and other subscribers) through a JMS Topic.<BR>"
                + "<FONT color=\"red\"><B>Note:</B> Not intended for performance testing.</FONT>");

        // we only want to look up the JMS resources once
        try {

            Connection conn = topicConnectionFactory.createConnection();

            try {
                TextMessage message = null;
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    /*Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        MessageProducer producer = sess.createProducer(tradeStreamerTopic);
                        message = sess.createTextMessage();

                        String command = "ping";
                        message.setStringProperty("command", command);
                        message.setLongProperty("publishTime", System.currentTimeMillis());
                        message.setText("Ping message for topic java:comp/env/jms/TradeStreamerTopic sent from PingServlet2MDBTopic at " + new java.util.Date());

                        producer.send(message);
                    } finally {
                        sess.close();
                    }*/
                	
                	JMSContext context = topicConnectionFactory.createContext();
            		
            		message = context.createTextMessage();

            		message.setStringProperty("command", "ping");
                    message.setLongProperty("publishTime", System.currentTimeMillis());
                    message.setText("Ping message for topic java:comp/env/jms/TradeStreamerTopic sent from PingServlet2MDBTopic at " + new java.util.Date());
              		
            		context.createProducer().send(tradeStreamerTopic, message);
                }

                // write out the output
                output.append("<HR>initTime: ").append(initTime);
                output.append("<BR>Hit Count: ").append(hitCount++);
                output.append("<HR>Posted Text message to java:comp/env/jms/TradeStreamerTopic topic");
                output.append("<BR>Message: ").append(message);
                output.append("<BR><BR>Message text: ").append(message.getText());
                output.append("<BR><HR></FONT></BODY></HTML>");
                out.println(output.toString());

            } catch (Exception e) {
                Log.error("PingServlet2MDBTopic.doGet(...):exception posting message to TradeStreamerTopic topic");
                throw e;
            } finally {
                conn.close();
            }
        } // this is where I actually handle the exceptions
        catch (Exception e) {
            Log.error(e, "PingServlet2MDBTopic.doGet(...): error");
            res.sendError(500, "PingServlet2MDBTopic.doGet(...): error, " + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, configured with trade runtime configs, tests Servlet to Session EJB path";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;
import com.ibm.websphere.samples.daytrader.entities.OrderDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * Primitive to test Entity Container Managed Relationshiop One to One Servlet
 * will generate a random userID and get the profile for that user using a
 * {@link trade.Account} Entity EJB This tests the common path of a Servlet
 * calling a Session to Entity EJB to get CMR One to One data
 *
 */
@WebServlet(name = "ejb3.PingServlet2Session2CMR2One2Many", urlPatterns = { "/ejb3/PingServlet2Session2CMR2One2Many" })
public class PingServlet2Session2CMROne2Many extends HttpServlet {
    private static final long serialVersionUID = -8658929449987440032L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBBean tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();

        String userID = null;

        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>Servlet2Session2CMROne20ne</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2Session2CMROne2Many<BR></FONT>"
                + "<FONT size=\"-1\" color=\"#000066\"><BR>PingServlet2Session2CMROne2Many uses the Trade Session EJB"
                + " to get the orders for a user using an EJB 3.0 Entity CMR one to many relationship");
        try {

            Collection<?> orderDataBeans = null;
            int iter = TradeConfig.getPrimIterations();
            for (int ii = 0; ii < iter; ii++) {
                userID = TradeConfig.rndUserID();

                // get the users orders and print the output.
                orderDataBeans = tradeSLSBLocal.getOrders(userID);
            }

            output.append("<HR>initTime: " + initTime + "<BR>Hit Count: ").append(hitCount++);
            output.append("<HR>One to Many CMR access of Account Orders from Account Entity<BR> ");
            output.append("<HR>User: " + userID + " currently has " + orderDataBeans.size() + " stock orders:");
            Iterator<?> it = orderDataBeans.iterator();
            while (it.hasNext()) {
                OrderDataBean orderData = (OrderDataBean) it.next();
                output.append("<BR>" + orderData.toHTML());
            }
            output.append("</font><HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingServlet2Session2CMROne2Many.doGet(...): error");
            // this will send an Error to teh web applications defined error
            // page.
            res.sendError(500, "PingServlet2Session2CMROne2Many.doGet(...): error" + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Entity EJB path";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;
import com.ibm.websphere.samples.daytrader.entities.AccountProfileDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 * Primitive to test Entity Container Managed Relationshiop One to One Servlet
 * will generate a random userID and get the profile for that user using a
 * {@link trade.Account} Entity EJB This tests the common path of a Servlet
 * calling a Session to Entity EJB to get CMR One to One data
 *
 */
@WebServlet(name = "ejb3.PingServlet2Session2CMR2One2One", urlPatterns = { "/ejb3/PingServlet2Session2CMR2One2One" })
public class PingServlet2Session2CMROne2One extends HttpServlet {
    private static final long serialVersionUID = 567062418489199248L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBBean tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();

        String userID = null;

        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>Servlet2Session2CMROne20ne</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2Session2CMROne2One<BR></FONT>"
                + "<FONT size=\"-1\" color=\"#000066\"><BR>PingServlet2Session2CMROne2One uses the Trade Session EJB"
                + " to get the profile for a user using an EJB 3.0 CMR one to one relationship");
        try {

            AccountProfileDataBean accountProfileData = null;
            int iter = TradeConfig.getPrimIterations();
            for (int ii = 0; ii < iter; ii++) {
                userID = TradeConfig.rndUserID();
                // get the price and print the output.
                accountProfileData = tradeSLSBLocal.getAccountProfileData(userID);
            }

            output.append("<HR>initTime: " + initTime + "<BR>Hit Count: ").append(hitCount++);
            output.append("<HR>One to One CMR access of AccountProfile Information from Account Entity<BR><BR> " + accountProfileData.toHTML());
            output.append("</font><HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingServlet2Session2CMROne2One.doGet(...): error");
            // this will send an Error to teh web applications defined error
            // page.
            res.sendError(500, "PingServlet2Session2CMROne2One.doGet(...): error" + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Entity EJB path";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingServlet2Session2Entity tests key functionality of a servlet call to a
 * stateless SessionEJB, and then to a Entity EJB representing data in a
 * database. This servlet makes use of the Stateless Session EJB {@link Trade},
 * and then uses {@link TradeConfig} to generate a random stock symbol. The
 * stocks price is looked up using the Quote Entity EJB.
 *
 */
@WebServlet(name = "ejb3.PingServlet2Session2Entity", urlPatterns = { "/ejb3/PingServlet2Session2Entity" })
public class PingServlet2Session2Entity extends HttpServlet {

    private static final long serialVersionUID = -5043457201022265012L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBBean tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        String symbol = null;
        QuoteDataBean quoteData = null;
        StringBuffer output = new StringBuffer(100);

        output.append("<html><head><title>PingServlet2Session2Entity</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2Session2Entity<BR></FONT>" + "<FONT size=\"-1\" color=\"#000066\">"
                + "PingServlet2Session2Entity tests the common path of a Servlet calling a Session EJB " + "which in turn calls an Entity EJB.<BR>");

        try {
            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    symbol = TradeConfig.rndSymbol();
                    // getQuote will call findQuote which will instaniate the
                    // Quote Entity Bean
                    // and then will return a QuoteObject
                    quoteData = tradeSLSBLocal.getQuote(symbol);
                }
            } catch (Exception ne) {
                Log.error(ne, "PingServlet2Session2Entity.goGet(...): exception getting QuoteData through Trade");
                throw ne;
            }

            output.append("<HR>initTime: " + initTime).append("<BR>Hit Count: " + hitCount++);
            output.append("<HR>Quote Information<BR><BR>" + quoteData.toHTML());
            out.println(output.toString());

        } catch (Exception e) {
            Log.error(e, "PingServlet2Session2Entity.doGet(...): General Exception caught");
            res.sendError(500, "General Exception caught, " + e.toString());
        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Session to Entity EJB path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();

        if (tradeSLSBLocal == null) {
            Log.error("PingServlet2Session2Entity:init - Injection of tradeSLSBLocal failed - performing JNDI lookup!");

            try {
                InitialContext context = new InitialContext();
                tradeSLSBLocal = (TradeSLSBBean) context.lookup("java:comp/env/ejb/TradeSLSBBean");
            } catch (Exception ex) {
                Log.error("PingServlet2Session2Entity:init - Lookup of tradeSLSBLocal failed!!!");
                ex.printStackTrace();
            }
        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingServlet2Session2Entity tests key functionality of a servlet call to a
 * stateless SessionEJB, and then to a Entity EJB representing data in a
 * database. This servlet makes use of the Stateless Session EJB {@link Trade},
 * and then uses {@link TradeConfig} to generate a random stock symbol. The
 * stocks price is looked up using the Quote Entity EJB.
 *
 */
@WebServlet(name = "ejb3.PingServlet2Session2Entity2JSP", urlPatterns = { "/ejb3/PingServlet2Session2Entity2JSP" })
public class PingServlet2Session2Entity2JSP extends HttpServlet {

    private static final long serialVersionUID = -8966014710582651693L;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBBean tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String symbol = null;
        QuoteDataBean quoteData = null;
        ServletContext ctx = getServletConfig().getServletContext();

        try {
            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    symbol = TradeConfig.rndSymbol();
                    // getQuote will call findQuote which will instaniate the
                    // Quote Entity Bean
                    // and then will return a QuoteObject
                    quoteData = tradeSLSBLocal.getQuote(symbol);
                }

                req.setAttribute("quoteData", quoteData);
                // req.setAttribute("hitCount", hitCount);
                // req.setAttribute("initTime", initTime);

                ctx.getRequestDispatcher("/quoteDataPrimitive.jsp").include(req, res);
            } catch (Exception ne) {
                Log.error(ne, "PingServlet2Session2Entity2JSP.goGet(...): exception getting QuoteData through Trade");
                throw ne;
            }

        } catch (Exception e) {
            Log.error(e, "PingServlet2Session2Entity2JSP.doGet(...): General Exception caught");
            res.sendError(500, "General Exception caught, " + e.toString());
        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Session to Entity EJB to JSP path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // hitCount = 0;
        // initTime = new java.util.Date().toString();

        if (tradeSLSBLocal == null) {
            Log.error("PingServlet2Session2Entity2JSP:init - Injection of tradeSLSBLocal failed - performing JNDI lookup!");

            try {
                InitialContext context = new InitialContext();
                tradeSLSBLocal = (TradeSLSBBean) context.lookup("java:comp/env/ejb/TradeSLSBBean");
            } catch (Exception ex) {
                Log.error("PingServlet2Session2EntityJSP:init - Lookup of tradeSLSBLocal failed!!!");
                ex.printStackTrace();
            }
        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;
import com.ibm.websphere.samples.daytrader.entities.HoldingDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingServlet2Session2Entity tests key functionality of a servlet call to a
 * stateless SessionEJB, and then to a Entity EJB representing data in a
 * database. This servlet makes use of the Stateless Session EJB {@link Trade},
 * and then uses {@link TradeConfig} to generate a random user. The users
 * portfolio is looked up using the Holding Entity EJB returnin a collection of
 * Holdings
 *
 */
@WebServlet(name = "ejb3.PingServlet2Session2EntityCollection", urlPatterns = { "/ejb3/PingServlet2Session2EntityCollection" })
public class PingServlet2Session2EntityCollection extends HttpServlet {

    private static final long serialVersionUID = 6171380014749902308L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBBean tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        String userID = null;
        Collection<?> holdingDataBeans = null;
        StringBuffer output = new StringBuffer(100);

        output.append("<html><head><title>PingServlet2Session2EntityCollection</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2Session2EntityCollection<BR></FONT>" + "<FONT size=\"-1\" color=\"#000066\">"
                + "PingServlet2Session2EntityCollection tests the common path of a Servlet calling a Session EJB "
                + "which in turn calls a finder on an Entity EJB returning a collection of Entity EJBs.<BR>");

        try {

            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    userID = TradeConfig.rndUserID();
                    // getQuote will call findQuote which will instaniate the
                    // Quote Entity Bean
                    // and then will return a QuoteObject
                    holdingDataBeans = tradeSLSBLocal.getHoldings(userID);
                    // trade.remove();
                }
            } catch (Exception ne) {
                Log.error(ne, "PingServlet2Session2EntityCollection.goGet(...): exception getting HoldingData collection through Trade for user " + userID);
                throw ne;
            }

            output.append("<HR>initTime: " + initTime).append("<BR>Hit Count: " + hitCount++);
            output.append("<HR>User: " + userID + " is currently holding " + holdingDataBeans.size() + " stock holdings:");
            Iterator<?> it = holdingDataBeans.iterator();
            while (it.hasNext()) {
                HoldingDataBean holdingData = (HoldingDataBean) it.next();
                output.append("<BR>" + holdingData.toHTML());
            }
            out.println(output.toString());

        } catch (Exception e) {
            Log.error(e, "PingServlet2Session2EntityCollection.doGet(...): General Exception caught");
            res.sendError(500, "General Exception caught, " + e.toString());
        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Session to Entity returning a collection of Entity EJBs";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * This primitive is designed to run inside the TradeApplication and relies upon
 * the {@link trade_client.TradeConfig} class to set configuration parameters.
 * PingServlet2SessionEJB tests key functionality of a servlet call to a
 * stateless SessionEJB. This servlet makes use of the Stateless Session EJB
 * {@link trade.Trade} by calling calculateInvestmentReturn with three random
 * numbers.
 *
 */
@WebServlet(name = "ejb3.PingServlet2SessionLocal", urlPatterns = { "/ejb3/PingServlet2SessionLocal" })
public class PingServlet2SessionLocal extends HttpServlet {

    private static final long serialVersionUID = 6854998080392777053L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBLocal tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        // use a stringbuffer to avoid concatenation of Strings
        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>PingServlet2SessionLocal</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2SessionLocal<BR></FONT>" + "<FONT size=\"-1\" color=\"#000066\">"
                + "Tests the basis path from a Servlet to a Session Bean.");

        try {

            try {
                // create three random numbers
                double rnd1 = Math.random() * 1000000;
                double rnd2 = Math.random() * 1000000;

                // use a function to do some work.
                double increase = 0.0;
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    increase = tradeSLSBLocal.investmentReturn(rnd1, rnd2);
                }

                // write out the output
                output.append("<HR>initTime: " + initTime);
                output.append("<BR>Hit Count: " + hitCount++);
                output.append("<HR>Investment Return Information <BR><BR>investment: " + rnd1);
                output.append("<BR>current Value: " + rnd2);
                output.append("<BR>investment return " + increase + "<HR></FONT></BODY></HTML>");
                out.println(output.toString());

            } catch (Exception e) {
                Log.error("PingServlet2Session.doGet(...):exception calling trade.investmentReturn ");
                throw e;
            }
        } // this is where I actually handle the exceptions
        catch (Exception e) {
            Log.error(e, "PingServlet2Session.doGet(...): error");
            res.sendError(500, "PingServlet2Session.doGet(...): error, " + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, configured with trade runtime configs, tests Servlet to Session EJB path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();

        if (tradeSLSBLocal == null) {
            Log.error("PingServlet2SessionLocal:init - Injection of TradeSLSBLocal failed - performing JNDI lookup!");

            try {
                InitialContext context = new InitialContext();
                tradeSLSBLocal = (TradeSLSBLocal) context.lookup("java:comp/env/ejb/TradeSLSBBean");
            } catch (Exception ex) {
                Log.error("PingServlet2SessionLocal:init - Lookup of TradeSLSBLocal failed!!!");
                ex.printStackTrace();
            }
        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBRemote;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * This primitive is designed to run inside the TradeApplication and relies upon
 * the {@link trade_client.TradeConfig} class to set configuration parameters.
 * PingServlet2SessionEJB tests key functionality of a servlet call to a
 * stateless SessionEJB. This servlet makes use of the Stateless Session EJB
 * {@link trade.Trade} by calling calculateInvestmentReturn with three random
 * numbers.
 *
 */
@WebServlet(name = "ejb3.PingServlet2SessionRemote", urlPatterns = { "/ejb3/PingServlet2SessionRemote" })
public class PingServlet2SessionRemote extends HttpServlet {

    private static final long serialVersionUID = -6328388347808212784L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBRemote")
    private TradeSLSBRemote tradeSLSBRemote;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        // use a stringbuffer to avoid concatenation of Strings
        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>PingServlet2SessionRemote</title></head>" + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2SessionRemote<BR></FONT>"
                + "<FONT size=\"-1\" color=\"#000066\">" + "Tests the basis path from a Servlet to a Session Bean.");

        try {

            try {
                // create three random numbers
                double rnd1 = Math.random() * 1000000;
                double rnd2 = Math.random() * 1000000;

                // use a function to do some work.
                double increase = 0.0;
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    increase = tradeSLSBRemote.investmentReturn(rnd1, rnd2);
                }

                // write out the output
                output.append("<HR>initTime: " + initTime);
                output.append("<BR>Hit Count: " + hitCount++);
                output.append("<HR>Investment Return Information <BR><BR>investment: " + rnd1);
                output.append("<BR>current Value: " + rnd2);
                output.append("<BR>investment return " + increase + "<HR></FONT></BODY></HTML>");
                out.println(output.toString());

            } catch (Exception e) {
                Log.error("PingServlet2Session.doGet(...):exception calling trade.investmentReturn ");
                throw e;
            }
        } // this is where I actually handle the exceptions
        catch (Exception e) {
            Log.error(e, "PingServlet2Session.doGet(...): error");
            res.sendError(500, "PingServlet2Session.doGet(...): error, " + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, configured with trade runtime configs, tests Servlet to Session EJB path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();

        if (tradeSLSBRemote == null) {
            Log.error("PingServlet2Session:init - Injection of tradeSLSBRemote failed - performing JNDI lookup!");

            try {
                InitialContext context = new InitialContext();
                tradeSLSBRemote = (TradeSLSBRemote) context.lookup("java:comp/env/ejb/TradeSLSBBeanRemote");
            } catch (Exception ex) {
                Log.error("PingServlet2Session:init - Lookup of tradeSLSBRemote failed!!!");
                ex.printStackTrace();
            }
        }
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

/**
 *
 * PingServlet2TwoPhase tests key functionality of a TwoPhase commit In this
 * primitive a servlet calls a Session EJB which begins a global txn The Session
 * EJB then reads a DB row and sends a message to JMS Queue The txn is closed w/
 * a 2-phase commit
 *
 */
@WebServlet(name = "ejb3.PingServlet2TwoPhase", urlPatterns = { "/ejb3/PingServlet2TwoPhase" })
public class PingServlet2TwoPhase extends HttpServlet {

    private static final long serialVersionUID = -1563251786527079548L;

    private static String initTime;

    private static int hitCount;

    @EJB(lookup="java:app/daytrader-ee7-ejb/TradeSLSBBean!com.ibm.websphere.samples.daytrader.ejb3.TradeSLSBLocal")
    private TradeSLSBBean tradeSLSBLocal;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        String symbol = null;
        QuoteDataBean quoteData = null;
        StringBuffer output = new StringBuffer(100);

        output.append("<html><head><title>PingServlet2TwoPhase</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2TwoPhase<BR></FONT>" + "<FONT size=\"-1\" color=\"#000066\">"
                + "PingServlet2TwoPhase tests the path of a Servlet calling a Session EJB "
                + "which in turn calls an Entity EJB to read a DB row (quote). The Session EJB " + "then posts a message to a JMS Queue. "
                + "<BR> These operations are wrapped in a 2-phase commit<BR>");

        try {

            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    symbol = TradeConfig.rndSymbol();
                    // getQuote will call findQuote which will instaniate the
                    // Quote Entity Bean
                    // and then will return a QuoteObject
                    quoteData = tradeSLSBLocal.pingTwoPhase(symbol);

                }
            } catch (Exception ne) {
                Log.error(ne, "PingServlet2TwoPhase.goGet(...): exception getting QuoteData through Trade");
                throw ne;
            }

            output.append("<HR>initTime: " + initTime).append("<BR>Hit Count: " + hitCount++);
            output.append("<HR>Two phase ping selected a quote and sent a message to TradeBrokerQueue JMS queue<BR>Quote Information<BR><BR>"
                    + quoteData.toHTML());
            out.println(output.toString());

        } catch (Exception e) {
            Log.error(e, "PingServlet2TwoPhase.doGet(...): General Exception caught");
            res.sendError(500, "General Exception caught, " + e.toString());
        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, tests Servlet to Session to Entity EJB and JMS -- 2-phase commit path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }
}
/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.ibm.websphere.samples.daytrader.util.Log;

// This is coded to be a Text type decoder expecting JSON format. 
// It will decode incoming messages into object of type String
public class ActionDecoder implements Decoder.Text<ActionMessage> {

    public ActionDecoder() {
    }
    
    @Override
    public void destroy() {
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public ActionMessage decode(String jsonText) throws DecodeException {
       
        if (Log.doTrace()) {
            Log.trace("ActionDecoder:decode -- received -->" + jsonText + "<--");
        }

        ActionMessage actionMessage = new ActionMessage();
        actionMessage.doDecoding(jsonText);
        return actionMessage;

    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.StringReader;

import javax.json.Json;
import javax.json.stream.JsonParser;

import com.ibm.websphere.samples.daytrader.util.Log;

/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
public class ActionMessage {
    
    String decodedAction = null;
    
    public ActionMessage() {  
    }
    
    public void doDecoding(String jsonText) {
              
        String keyName = null;
        try 
        {
            // JSON parse
            JsonParser parser = Json.createParser(new StringReader(jsonText));
            while (parser.hasNext()) {
                JsonParser.Event event = parser.next();
                switch(event) {
                case KEY_NAME:
                    keyName=parser.getString();
                    break;
                case VALUE_STRING:
                    if (keyName != null && keyName.equals("action")) {
                        decodedAction=parser.getString();
                    }
                    break;
                default:
                    break;
                }
            }
        } catch (Exception e) {
            Log.error("ActionMessage:doDecoding(" + jsonText + ") --> failed", e);
        }
        
        if (Log.doTrace()) {
            if (decodedAction != null ) {
                Log.trace("ActionMessage:doDecoding -- decoded action -->" + decodedAction + "<--");
            } else {
                Log.trace("ActionMessage:doDecoding -- decoded action -->null<--");
            }
        }
        
    }
    
    public String getDecodedAction() {
        return decodedAction;
    }
    
}


/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class JsonDecoder implements Decoder.Text<JsonMessage> {

    @Override
    public void destroy() {
    }

    @Override
    public void init(EndpointConfig ec) {
    }

    @Override
    public JsonMessage decode(String json) throws DecodeException {
        JsonObject jsonObject = Json.createReader(new StringReader(json)).readObject();
        
        JsonMessage message = new JsonMessage();
        message.setKey(jsonObject.getString("key"));
        message.setValue(jsonObject.getString("value"));
        
        return message;
    }

    @Override
    public boolean willDecode(String json) {
        try {
            Json.createReader(new StringReader(json)).readObject();
            return true;
          } catch (Exception e) {
            return false;
          }
    }

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class JsonEncoder implements Encoder.Text<JsonMessage>{

    @Override
    public void destroy() {
    }

    @Override
    public void init(EndpointConfig ec) {
    }

    @Override
    public String encode(JsonMessage message) throws EncodeException {
        
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("key", message.getKey())
                .add("value", message.getValue()).build();

        return jsonObject.toString();
    }

   

}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class JsonMessage {

    private String key;
    private String value;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.event.Observes;
import javax.jms.Message;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ibm.websphere.samples.daytrader.TradeAction;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.WebSocketJMSMessage;


/** This class is a WebSocket EndPoint that sends the Market Summary in JSON form when requested 
 *  and sends stock price changes when received from an MDB through a CDI event
 * */

@ServerEndpoint(value = "/marketsummary",decoders=ActionDecoder.class)
public class MarketSummaryWebSocket {

	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());
    private final CountDownLatch latch = new CountDownLatch(1);

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        if (Log.doTrace()) {
            Log.trace("MarketSummaryWebSocket:onOpen -- session -->" + session + "<--");
        }

        sessions.add(session);
        latch.countDown();
    } 
    
    @OnMessage
    public void sendMarketSummary(ActionMessage message, Session currentSession) {

        String action = message.getDecodedAction();
        
        if (Log.doTrace()) {
            if (action != null ) {
                Log.trace("MarketSummaryWebSocket:sendMarketSummary -- received -->" + action + "<--");
            } else {
                Log.trace("MarketSummaryWebSocket:sendMarketSummary -- received -->null<--");
            }
        }
        
        if (action != null && action.equals("update")) {
            TradeAction tAction = new TradeAction();

            try {
                
                JsonObject mkSummary = tAction.getMarketSummary().toJSON();

                if (Log.doTrace()) {
                    Log.trace("MarketSummaryWebSocket:sendMarketSummary -- sending -->" + mkSummary + "<--");
                }
                
                // Make sure onopen is finished
                latch.await();
                
                if (RecentStockChangeList.isEmpty()) {
                    currentSession.getAsyncRemote().sendText(mkSummary.toString());
                    
                }
                else { // Merge Objects 
                    JsonObject recentChangeList = RecentStockChangeList.stockChangesInJSON();
                    currentSession.getAsyncRemote().sendText(mergeJsonObjects(mkSummary,recentChangeList).toString());
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Throwable t, Session currentSession) {
        if (Log.doTrace()) {
            Log.trace("MarketSummaryWebSocket:onError -- session -->" + currentSession + "<--");
        }
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

        if (Log.doTrace()) {
            Log.trace("MarketSummaryWebSocket:onClose -- session -->" + session + "<--");
        }

        sessions.remove(session);

    }
    
    public static void onJMSMessage(@Observes @WebSocketJMSMessage Message message) {
    	
    	if (Log.doTrace()) {
            Log.trace("MarketSummaryWebSocket:onJMSMessage");
        }
    	
    	RecentStockChangeList.addStockChange(message);
        
        JsonObject stockChangeJson = RecentStockChangeList.stockChangesInJSON();
        
        synchronized(sessions) {
            for (Session s : sessions) {
                if (s.isOpen()) {
                    s.getAsyncRemote().sendText(stockChangeJson.toString());
                }
            }
        }
    }    
    
    private JsonObject mergeJsonObjects(JsonObject obj1, JsonObject obj2) {
        
        JsonObjectBuilder jObjectBuilder = Json.createObjectBuilder();
        
        Set<String> keys1 = obj1.keySet();
        Iterator<String> iter1 = keys1.iterator();
        
        while(iter1.hasNext()) {
            String key = (String)iter1.next();
            JsonValue value = obj1.get(key);
            
            jObjectBuilder.add(key, value);
            
        }
        
        Set<String> keys2 = obj2.keySet();
        Iterator<String> iter2 = keys2.iterator();
        
        while(iter2.hasNext()) {
            String key = (String)iter2.next();
            JsonValue value = obj2.get(key);
            
            jObjectBuilder.add(key, value);
            
        }
        
        return jObjectBuilder.build();
    }
}

/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jms.Message;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;


/** This class is a holds the last 5 stock changes, used by the MarketSummary WebSocket
 * */

public class RecentStockChangeList {

    private static List<Message> stockChanges = Collections.synchronizedList(new LinkedList<Message>());
       
    public static void addStockChange(Message message) {
        
        stockChanges.add(0, message);
        
        // Add stock, remove if needed
        if(stockChanges.size() > 5) {
            stockChanges.remove(5);
        }
    }
    
    public static JsonObject stockChangesInJSON() {
        
        JsonObjectBuilder jObjectBuilder = Json.createObjectBuilder();
        
        try {
            int i = 1;
            
            List<Message> temp = new LinkedList<Message>(stockChanges);
                        
            for (Iterator<Message> iterator = temp.iterator(); iterator.hasNext();) {
                Message message = iterator.next();
                            
                jObjectBuilder.add("change" + i + "_stock", message.getStringProperty("symbol"));
                jObjectBuilder.add("change" + i + "_price","$" + message.getStringProperty("price"));          
                            
                BigDecimal change = new BigDecimal(message.getStringProperty("price")).subtract(new BigDecimal(message.getStringProperty("oldPrice")));
                change.setScale(2, RoundingMode.HALF_UP);
                
                jObjectBuilder.add("change" + i + "_change", change.toString());
                
                i++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return jObjectBuilder.build();
    }
    
    public static boolean isEmpty() {
        return stockChanges.isEmpty();
    }        

}

