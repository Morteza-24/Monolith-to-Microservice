//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2004,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * @author aamortim
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
/**
 *  Utility class.
 */
public class ListProperties extends Properties {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Hashtable<String, Vector<String>> listProps = null;
    /* Method load
     * @param inStream
     */
    
	public void load(InputStream inStream) throws IOException {
        try {
        	Util.debug("ListProperties.load - loading from stream "+inStream);
            // Parse property file, remove comments, blank lines, and combine
            // continued lines.
            String propFile = "";
            BufferedReader inputLine = new BufferedReader(new InputStreamReader(inStream));
            String line = inputLine.readLine();
            boolean lineContinue = false;
            while (line != null) {
                Util.debug("ListProperties.load - Line read: " + line);
                line = line.trim();
                String currLine = "";
                if (line.startsWith("#")) {
                    // Skipping comment
                } else if (line.startsWith("!")) {
                    // Skipping comment
                } else if (line.equals("")) {
                    // Skipping blank lines
                } else {
                    if (!lineContinue) {
                        currLine = line;
                    } else {
                        // This is a continuation line.   Add to previous line.
                        currLine += line;
                    }
                    // Must be a property line
                    if (line.endsWith("\\")) {
                        // Next line is continued from the current one.
                        lineContinue = true;
                    } else {
                        // The current line is completed.   Parse the property.
                        propFile += currLine + "\n";
                        currLine = "";
                        lineContinue = false;
                    }
                }
                line = inputLine.readLine();
            }
            // Load Properties
            listProps = new Hashtable<String, Vector<String>>();
            // Now parse the Properties to create an array
            String[] props = readTokens(propFile, "\n");
            for (int index = 0; index < props.length; index++) {
                Util.debug("ListProperties.load() - props[" + index + "] = " + props[index]);
                // Parse the line to get the key,value pair
                String[] val = readTokens(props[index], "=");
                Util.debug("ListProperties.load() - val[0]: " + val[0] + " val[1]: " + val[1]);
                if (!val[0].equals("")) {
                    if (this.containsKey(val[0])) {
                        // Previous key,value was already created.
                        // Need an array
                        Vector<String> currList = (Vector<String>) listProps.get(val[0]);
                        if ((currList == null) || currList.isEmpty()) {
                            currList = new Vector<String>();
                            String prevVal = this.getProperty(val[0]);
                            currList.addElement(prevVal);
                        }
                        currList.addElement(val[1]);
                        listProps.put(val[0], currList);
                    }
                    this.setProperty(val[0], val[1]);
                }
            }
        } catch (Exception e) {
            Util.debug("ListProperties.load(): Exception: " + e);
            e.printStackTrace();
        }
    }
    /**
     * Method readTokens.
     * @param text
     * @param token
     * @return list
     */
    public String[] readTokens(String text, String token) {
        StringTokenizer parser = new StringTokenizer(text, token);
        int numTokens = parser.countTokens();
        String[] list = new String[numTokens];
        for (int i = 0; i < numTokens; i++) {
            list[i] = parser.nextToken();
        }
        return list;
    }
    /**
     * Method getProperties.
     * @param name
     * @return values
     */
    public String[] getProperties(String name) {
        String[] values = { "" };
        try {
            String value = this.getProperty(name);
            Util.debug("ListProperties.getProperties: property (" + name + ") -> " + value);
            if (listProps.containsKey(name)) {
                Vector<String> list = (Vector<String>) listProps.get(name);
                values = new String[list.size()];
                for (int index = 0; index < list.size(); index++) {
                    values[index] = (String) list.elementAt(index);
                }
            } else {
                values[0] = value;
            }
        } catch (Exception e) {
            Util.debug("ListProperties.getProperties(): Exception: " + e);
        }
        return (values);
    }
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.util.StringTokenizer;

import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *  Utility class.
 */
public class Util {
    /** Datasource name. */
    public static final String DS_NAME = "java:comp/env/jdbc/PlantsByWebSphereDataSource";
    // Constants for JSPs and HTMLs.
    public static final String PAGE_ACCOUNT = "account.jsp";
    public static final String PAGE_CART = "cart.jsp";
    public static final String PAGE_CHECKOUTFINAL = "checkout_final.jsp";
    public static final String PAGE_HELP = "help.jsp";
    public static final String PAGE_LOGIN = "login.jsp";
    public static final String PAGE_ORDERDONE = "orderdone.jsp";
    public static final String PAGE_ORDERINFO = "orderinfo.jsp";
    public static final String PAGE_PRODUCT = "product.jsp";
    public static final String PAGE_PROMO = "promo.html";
    public static final String PAGE_REGISTER = "register.jsp";
    public static final String PAGE_SHOPPING = "shopping.jsp";
    public static final String PAGE_BACKADMIN = "backorderadmin.jsp";
    public static final String PAGE_SUPPLIERCFG = "supplierconfig.jsp";
    public static final String PAGE_ADMINHOME = "admin.html";
    public static final String PAGE_ADMINACTIONS = "adminactions.html";
    // Request and session attributes.
    public static final String ATTR_ACTION = "action";
    public static final String ATTR_CART = "ShoppingCart";
//    public static final String ATTR_CART_CONTENTS = "CartContents";
    public static final String ATTR_CARTITEMS = "cartitems";
    public static final String ATTR_CATEGORY = "Category";
    public static final String ATTR_CHECKOUT = "CheckingOut";
    public static final String ATTR_CUSTOMER = "CustomerInfo";
    public static final String ATTR_EDITACCOUNTINFO = "EditAccountInfo";
    public static final String ATTR_INVITEM = "invitem";
    public static final String ATTR_INVITEMS = "invitems";
    public static final String ATTR_ORDERID = "OrderID";
    public static final String ATTR_ORDERINFO = "OrderInfo";
    public static final String ATTR_ORDERKEY = "OrderKey";
    public static final String ATTR_RESULTS = "results";
    public static final String ATTR_UPDATING = "updating";
    public static final int    ATTR_SFTIMEOUT = 10;				// if this is changed, updated session timeout
    															// in the PlantsByWebSphere web.xml
    public static final String ATTR_SUPPLIER = "SupplierInfo";
    // Admin type actions
    public static final String ATTR_ADMINTYPE = "admintype";
    public static final String ADMIN_BACKORDER = "backorder";
    public static final String ADMIN_SUPPLIERCFG = "supplierconfig";
    public static final String ADMIN_POPULATE = "populate";
    // Servlet action codes.
    // Supplier Config actions
    public static final String ACTION_GETSUPPLIER = "getsupplier";
    public static final String ACTION_UPDATESUPPLIER = "updatesupplier";
    // Backorder actions
    public static final String ACTION_ORDERSTOCK = "orderstock";
    public static final String ACTION_UPDATESTOCK = "updatestock";
    public static final String ACTION_GETBACKORDERS = "getbackorders";
    public static final String ACTION_UPDATEQUANTITY = "updatequantity";
    public static final String ACTION_ORDERSTATUS = "orderstatus";
    public static final String ACTION_CANCEL = "cancel";
    public static final String STATUS_ORDERSTOCK = "Order Stock";
    public static final String STATUS_ORDEREDSTOCK = "Ordered Stock";
    public static final String STATUS_RECEIVEDSTOCK = "Received Stock";
    public static final String STATUS_ADDEDSTOCK = "Added Stock";
    public static final String DEFAULT_SUPPLIERID = "Supplier";
    private static InitialContext initCtx = null;
    private static final String[] CATEGORY_STRINGS = { "Flowers", "Fruits & Vegetables", "Trees", "Accessories" };
    private static final String[] SHIPPING_METHOD_STRINGS = { "Standard Ground", "Second Day Air", "Next Day Air" };
    private static final String[] SHIPPING_METHOD_TIMES = { "( 3 to 6 business days )", "( 2 to 3 business days )", "( 1 to 2 business days )" };
    private static final float[] SHIPPING_METHOD_PRICES = { 4.99f, 8.99f, 12.99f };
    public static final String ZERO_14 = "00000000000000";
    /**
     * Return the cached Initial Context.
     *
     * @return InitialContext, or null if a naming exception.
     */
    static public InitialContext getInitialContext() {
        try {
            // Get InitialContext if it has not been gotten yet.
            if (initCtx == null) {
                // properties are in the system properties
                initCtx = new InitialContext();
            }
        }
        // Naming Exception will cause a null return.
        catch (NamingException e) {}
        return initCtx;
    }

    /**
     * Get the displayable name of a category.
     * @param index The int representation of a category.
     * @return The category as a String (null, if an invalid index given).
     */
    static public String getCategoryString(int index) {
        if ((index >= 0) && (index < CATEGORY_STRINGS.length))
            return CATEGORY_STRINGS[index];
        else
            return null;
    }
    /**
     * Get the category strings in an array.
     *
     * @return The category strings in an array.
     */
    static public String[] getCategoryStrings() {
        return CATEGORY_STRINGS;
    }
    /**
     * Get the shipping method.
     * @param index The int representation of a shipping method.
     * @return The shipping method (null, if an invalid index given).
     */
    static public String getShippingMethod(int index) {
        if ((index >= 0) && (index < SHIPPING_METHOD_STRINGS.length))
            return SHIPPING_METHOD_STRINGS[index];
        else
            return null;
    }
    /**
     * Get the shipping method price.
     * @param index The int representation of a shipping method.
     * @return The shipping method price (-1, if an invalid index given).
     */
    static public float getShippingMethodPrice(int index) {
        if ((index >= 0) && (index < SHIPPING_METHOD_PRICES.length))
            return SHIPPING_METHOD_PRICES[index];
        else
            return -1;
    }
    /**
     * Get the shipping method price.
     * @param index The int representation of a shipping method.
     * @return The shipping method time (null, if an invalid index given).
     */
    static public String getShippingMethodTime(int index) {
        if ((index >= 0) && (index < SHIPPING_METHOD_TIMES.length))
            return SHIPPING_METHOD_TIMES[index];
        else
            return null;
    }
    /**
     * Get the shipping method strings in an array.
     * @return The shipping method strings in an array.
     */
    static public String[] getShippingMethodStrings() {
        return SHIPPING_METHOD_STRINGS;
    }
    /**
     * Get the shipping method strings, including prices and times, in an array.
     * @return The shipping method strings, including prices and times, in an array.
     */
    static public String[] getFullShippingMethodStrings() {
        String[] shippingMethods = new String[SHIPPING_METHOD_STRINGS.length];
        for (int i = 0; i < shippingMethods.length; i++) {
            shippingMethods[i] = SHIPPING_METHOD_STRINGS[i] + " " + SHIPPING_METHOD_TIMES[i] + " " + NumberFormat.getCurrencyInstance(java.util.Locale.US).format(new Float(SHIPPING_METHOD_PRICES[i]));
        }
        return shippingMethods;
    }
    private static final String PBW_PROPERTIES = "pbw.properties";
    private static ListProperties PBW_Properties = null;
    /**
     * Method readProperties.
     */
    public static void readProperties() throws FileNotFoundException {
        if (PBW_Properties == null) {
            // Try to read the  properties file.
            ListProperties prop = new ListProperties();
            try {
                String PBW_Properties_File = PBW_PROPERTIES;
                debug("Util.readProperties(): Loading PBW Properties from file: " + PBW_Properties_File);
                prop.load(Util.class.getClassLoader().getResourceAsStream(PBW_Properties_File));
            } catch (Exception e) {
                debug("Util.readProperties(): Exception: " + e);
                // Reset properties to retry loading next time.
                PBW_Properties = null;
                e.printStackTrace();
                throw new FileNotFoundException();
            }
            PBW_Properties = prop;
        }
    }
    /**
     * Method getProperty.
     * @param name
     * @return value
     */
    public static String getProperty(String name) {
        String value = "";
        try {
            if (PBW_Properties == null) {
                readProperties();
            }
            value = PBW_Properties.getProperty(name);
        } catch (Exception e) {
            debug("Util.getProperty(): Exception: " + e);
        }
        return (value);
    }
    /**
     * Method readTokens.
     * @param text
     * @param token
     * @return list
     */
    public static String[] readTokens(String text, String token) {
        StringTokenizer parser = new StringTokenizer(text, token);
        int numTokens = parser.countTokens();
        String[] list = new String[numTokens];
        for (int i = 0; i < numTokens; i++) {
            list[i] = parser.nextToken();
        }
        return list;
    }
    /**
     * Method getProperties.
     * @param name
     * @return values
     */
    public static String[] getProperties(String name) {
        String[] values = { "" };
        try {
            if (PBW_Properties == null) {
                readProperties();
            }
            values = PBW_Properties.getProperties(name);
            debug("Util.getProperties: property (" + name + ") -> " + values.toString());
            //for (Enumeration e = PBW_Properties.propertyNames() ; e.hasMoreElements() ;) {
            //    debug((String)e.nextElement());
            //}
        } catch (Exception e) {
            debug("Util.getProperties(): Exception: " + e);
        }
        return (values);
    }
    static private boolean debug = false;
    /** Set debug setting to on or off.
     * @param val True or false.
     */
    static final public void setDebug(boolean val) {
        debug = val;
    }
    /** Is debug turned on? */
    static final public boolean debugOn() {
        return debug;
    }
    /**
     * Output RAS message.
     * @param msg Message to be output.
     */
    static final public void debug(String msg) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
        	Application app = context.getApplication();
        	if (app != null) {
        		ProjectStage stage = app.getProjectStage();
        		if (stage == ProjectStage.Development || stage == ProjectStage.UnitTest) {
        			setDebug(true);
        		}
        	}
        	if (debug) {
        		System.out.println(msg);
        	}
        }
    }

    /**
     * Utilty functions for validating user input.
     * validateString will return false if any of the invalid characters appear in the input string.
     *
     * In general, we do not want to allow special characters in user input,
     * because this can open us to a XSS security vulnerability.
     * For example, a user should not be allowed to enter javascript in an input field.
     */
	static final char[] invalidCharList={'|','&',';','$','%','\'','\"','\\','<','>',','};

	public static boolean validateString(String input){
		if (input==null) return true;
		for (int i=0;i<invalidCharList.length;i++){
			if (input.indexOf(invalidCharList[i])!=-1){
				return false;
			}
		}
		return true;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Bean mapping for the ORDERITEM table.
 */
@Entity(name = "OrderItem")
@Table(name = "ORDERITEM", schema = "APP")
@NamedQueries({ @NamedQuery(name = "removeAllOrderItem", query = "delete from OrderItem") })
public class OrderItem {
	/**
	 * Composite Key class for Entity Bean: OrderItem
	 * 
	 * Key consists of essentially two foreign key relations, but is mapped as foreign keys.
	 */
	@Embeddable
	public static class PK implements java.io.Serializable {
		static final long serialVersionUID = 3206093459760846163L;
		@Column(name = "inventoryID")
		public String inventoryID;
		@Column(name = "ORDER_ORDERID")
		public String order_orderID;

		public PK() {
			Util.debug("OrderItem.PK()");
		}

		public PK(String inventoryID, String argOrder) {
			Util.debug("OrderItem.PK() inventoryID=" + inventoryID + "=");
			Util.debug("OrderItem.PK() orderID=" + argOrder + "=");
			this.inventoryID = inventoryID;
			this.order_orderID = argOrder;
		}

		/**
		 * Returns true if both keys are equal.
		 */
		public boolean equals(java.lang.Object otherKey) {
			if (otherKey instanceof PK) {
				PK o = (PK) otherKey;
				return ((this.inventoryID.equals(o.inventoryID)) && (this.order_orderID.equals(o.order_orderID)));
			}
			return false;
		}

		/**
		 * Returns the hash code for the key.
		 */
		public int hashCode() {
			Util.debug("OrderItem.PK.hashCode() inventoryID=" + inventoryID + "=");
			Util.debug("OrderItem.PK.hashCode() orderID=" + order_orderID + "=");

			return (inventoryID.hashCode() + order_orderID.hashCode());
		}
	}

	@SuppressWarnings("unused")
	@EmbeddedId
	private OrderItem.PK id;
	private String name;
	private String pkginfo;
	private float price;
	private float cost;
	private int category;
	private int quantity;
	private String sellDate;
	@Transient
	private String inventoryId;

	@ManyToOne
	@JoinColumn(name = "INVENTORYID", insertable = false, updatable = false)
	private Inventory inventory;
	@ManyToOne
	@JoinColumn(name = "ORDER_ORDERID", insertable = false, updatable = false)
	private Order order;

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public float getCost() {
		return cost;
	}

	public void setCost(float cost) {
		this.cost = cost;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPkginfo() {
		return pkginfo;
	}

	public void setPkginfo(String pkginfo) {
		this.pkginfo = pkginfo;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getSellDate() {
		return sellDate;
	}

	public void setSellDate(String sellDate) {
		this.sellDate = sellDate;
	}

	public OrderItem() {
	}

	public OrderItem(Inventory inv) {
		Util.debug("OrderItem(inv) - id = " + inv.getInventoryId());
		setInventoryId(inv.getInventoryId());
		inventory = inv;
		name = inv.getName();
		pkginfo = inv.getPkginfo();
		price = inv.getPrice();
		cost = inv.getCost();
		category = inv.getCategory();
	}

	public OrderItem(Order order, String orderID, Inventory inv, java.lang.String name, java.lang.String pkginfo,
			float price, float cost, int quantity, int category, java.lang.String sellDate) {
		Util.debug("OrderItem(etc.)");
		inventory = inv;
		setInventoryId(inv.getInventoryId());
		setName(name);
		setPkginfo(pkginfo);
		setPrice(price);
		setCost(cost);
		setQuantity(quantity);
		setCategory(category);
		setSellDate(sellDate);
		setOrder(order);
		id = new OrderItem.PK(inv.getInventoryId(), order.getOrderID());
	}

	/*
	 * updates the primary key field with the composite orderId+inventoryId
	 */
	public void updatePK() {
		id = new OrderItem.PK(inventoryId, order.getOrderID());
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inv) {
		this.inventory = inv;
	}

	public Order getOrder() {
		return order;
	}

	/**
	 * Sets the order for this item Also updates the sellDate
	 * 
	 * @param order
	 */
	public void setOrder(Order order) {
		this.order = order;
		this.sellDate = order.getSellDate();
	}

	public String getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(String inventoryId) {
		this.inventoryId = inventoryId;
	}

}
//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Bean mapping for BACKORDER table.
 */
@Entity(name = "BackOrder")
@Table(name = "BACKORDER", schema = "APP")
@NamedQueries({ @NamedQuery(name = "findAllBackOrders", query = "select b from BackOrder b"),
		@NamedQuery(name = "findByInventoryID", query = "select b from BackOrder b where ((b.inventory.inventoryId = :id) and (b.status = 'Order Stock'))"),
		@NamedQuery(name = "removeAllBackOrder", query = "delete from BackOrder") })
public class BackOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "BackOrderSeq")
	@TableGenerator(name = "BackOrderSeq", table = "IDGENERATOR", pkColumnName = "IDNAME", pkColumnValue = "BACKORDER", valueColumnName = "IDVALUE")
	private String backOrderID;
	private int quantity;
	private String status;
	private long lowDate;
	private long orderDate;
	private String supplierOrderID; // missing table

	// relationships
	@OneToOne
	@JoinColumn(name = "INVENTORYID")
	private Inventory inventory;

	public BackOrder() {
	}

	public BackOrder(String backOrderID) {
		setBackOrderID(backOrderID);
	}

	public BackOrder(Inventory inventory, int quantity) {
			this.setInventory(inventory);
			this.setQuantity(quantity);
			this.setStatus(Util.STATUS_ORDERSTOCK);
			this.setLowDate(System.currentTimeMillis());
	}

	public String getBackOrderID() {
		return backOrderID;
	}

	public void setBackOrderID(String backOrderID) {
		this.backOrderID = backOrderID;
	}

	public long getLowDate() {
		return lowDate;
	}

	public void setLowDate(long lowDate) {
		this.lowDate = lowDate;
	}

	public long getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(long orderDate) {
		this.orderDate = orderDate;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public void increateQuantity(int delta) {
		if (!(status.equals(Util.STATUS_ORDERSTOCK))) {
			Util.debug("BackOrderMgr.createBackOrder() - Backorders found but have already been ordered from the supplier");
			throw new RuntimeException("cannot increase order size for orders already in progress");
		}
		// Increase the BackOrder quantity for an existing Back Order.
		quantity = quantity + delta;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSupplierOrderID() {
		return supplierOrderID;
	}

	public void setSupplierOrderID(String supplierOrderID) {
		this.supplierOrderID = supplierOrderID;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;

/**
 * The key class of the Order entity bean.
 **/
public class OrderKey implements Serializable {

	private static final long serialVersionUID = 7912030586849592135L;

	public String orderID;

	/**
	 * Constructs an OrderKey object.
	 */
	public OrderKey() {
	}

	/**
	 * Constructs a newly allocated OrderKey object that represents the primitive long argument.
	 */
	public OrderKey(String orderID) {
		this.orderID = orderID;
	}

	/**
	 * Determines if the OrderKey object passed to the method matches this OrderKey object.
	 * 
	 * @param obj
	 *            java.lang.Object The OrderKey object to compare to this OrderKey object.
	 * @return boolean The pass object is either equal to this OrderKey object (true) or not.
	 */
	public boolean equals(Object obj) {
		if (obj instanceof OrderKey) {
			OrderKey otherKey = (OrderKey) obj;
			return (((orderID.equals(otherKey.orderID))));
		} else
			return false;
	}

	/**
	 * Generates a hash code for this OrderKey object.
	 * 
	 * @return int The hash code.
	 */
	public int hashCode() {
		return (orderID.hashCode());
	}

	/**
	 * Get accessor for persistent attribute: orderID
	 */
	public java.lang.String getOrderID() {
		return orderID;
	}

	/**
	 * Set accessor for persistent attribute: orderID
	 */
	public void setOrderID(java.lang.String newOrderID) {
		orderID = newOrderID;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Inventory is the bean mapping for the INVENTORY table. It provides information about products the
 * store has for sale.
 * 
 * @see Inventory
 */
@Entity(name = "Inventory")
@Table(name = "INVENTORY", schema = "APP")
@NamedQueries({
		@NamedQuery(name = "getItemsByCategory", query = "select i from Inventory i where i.category = :category ORDER BY i.inventoryId"),
		@NamedQuery(name = "getItemsLikeName", query = "select i from Inventory i where i.name like :name"),
		@NamedQuery(name = "removeAllInventory", query = "delete from Inventory") })
public class Inventory implements Cloneable, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static final int DEFAULT_MINTHRESHOLD = 50;
	private static final int DEFAULT_MAXTHRESHOLD = 200;
	@Id
	private String inventoryId;
	private String name;
	private String heading;
	private String description;
	private String pkginfo;
	private String image;
	private byte[] imgbytes;
	private float price;
	private float cost;
	private int quantity;
	private int category;
	private String notes;
	private boolean isPublic;
	private int minThreshold;
	private int maxThreshold;
	
	@Version
	private long version;

	@Transient
	private BackOrder backOrder;

	public Inventory() {
	}

	/**
	 * Create a new Inventory.
	 *
	 * @param key
	 *            Inventory Key
	 * @param name
	 *            Name of inventory item.
	 * @param heading
	 *            Description heading of inventory item.
	 * @param desc
	 *            Description of inventory item.
	 * @param pkginfo
	 *            Package info of inventory item.
	 * @param image
	 *            Image of inventory item.
	 * @param price
	 *            Price of inventory item.
	 * @param cost
	 *            Cost of inventory item.
	 * @param quantity
	 *            Quantity of inventory items in stock.
	 * @param category
	 *            Category of inventory item.
	 * @param notes
	 *            Notes of inventory item.
	 * @param isPublic
	 *            Access permission of inventory item.
	 */
	public Inventory(String key, String name, String heading, String desc, String pkginfo, String image, float price,
			float cost, int quantity, int category, String notes, boolean isPublic) {
		this.setInventoryId(key);
		Util.debug("creating new Inventory, inventoryId=" + this.getInventoryId());
		this.setName(name);
		this.setHeading(heading);
		this.setDescription(desc);
		this.setPkginfo(pkginfo);
		this.setImage(image);
		this.setPrice(price);
		this.setCost(cost);
		this.setQuantity(quantity);
		this.setCategory(category);
		this.setNotes(notes);
		this.setIsPublic(isPublic);
		this.setMinThreshold(DEFAULT_MINTHRESHOLD);
		this.setMaxThreshold(DEFAULT_MAXTHRESHOLD);

	}

	/**
	 * Create a new Inventory.
	 *
	 * @param item
	 *            Inventory to use to make a new inventory item.
	 */
	public Inventory(Inventory item) {
		this.setInventoryId(item.getInventoryId());
		this.setName(item.getName());
		this.setHeading(item.getHeading());
		this.setDescription(item.getDescription());
		this.setPkginfo(item.getPkginfo());
		this.setImage(item.getImage());
		this.setPrice(item.getPrice());
		this.setCost(item.getCost());
		this.setQuantity(item.getQuantity());
		this.setCategory(item.getCategory());
		this.setNotes(item.getNotes());
		this.setMinThreshold(DEFAULT_MINTHRESHOLD);
		this.setMaxThreshold(DEFAULT_MAXTHRESHOLD);

		setIsPublic(item.isPublic());

		// does not clone BackOrder info
	}

	/**
	 * Increase the quantity of this inventory item.
	 * 
	 * @param quantity
	 *            The number to increase the inventory by.
	 */
	public void increaseInventory(int quantity) {
		this.setQuantity(this.getQuantity() + quantity);
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public float getCost() {
		return cost;
	}

	public void setCost(float cost) {
		this.cost = cost;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getHeading() {
		return heading;
	}

	public void setHeading(String heading) {
		this.heading = heading;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getPkginfo() {
		return pkginfo;
	}

	public void setPkginfo(String pkginfo) {
		this.pkginfo = pkginfo;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getMaxThreshold() {
		return maxThreshold;
	}

	public void setMaxThreshold(int maxThreshold) {
		this.maxThreshold = maxThreshold;
	}

	public int getMinThreshold() {
		return minThreshold;
	}

	public void setMinThreshold(int minThreshold) {
		this.minThreshold = minThreshold;
	}

	public String getInventoryId() {
		return inventoryId;
	}

	public void setInventoryId(String id) {
		inventoryId = id;
	}

	/**
	 * Same as getInventoryId. Added for compatability with ShoppingCartItem when used by the Client
	 * XJB sample
	 * 
	 * @return String ID of the inventory item
	 */
	public String getID() {
		return inventoryId;
	}

	/**
	 * Same as setInventoryId. Added for compatability with ShoppingCartItem when used by the Client
	 * XJB sample
	 * 
	 */
	public void setID(String id) {
		inventoryId = id;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	/** Set the inventory item's public availability. */
	public void setPrivacy(boolean isPublic) {
		setIsPublic(isPublic);
	}

	public byte[] getImgbytes() {
		return imgbytes;
	}

	public void setImgbytes(byte[] imgbytes) {
		this.imgbytes = imgbytes;
	}

	public BackOrder getBackOrder() {
		return backOrder;
	}

	public void setBackOrder(BackOrder backOrder) {
		this.backOrder = backOrder;
	}
	
	@Override
	public String toString() {
	    return getClass().getSimpleName() + "{id=" + inventoryId + "}";
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Bean mapping for the ORDER1 table.
 */
@Entity(name = "Order")
@Table(name = "ORDER1", schema = "APP")
@NamedQueries({ @NamedQuery(name = "removeAllOrders", query = "delete from Order") })
public class Order {
	public static final String ORDER_INFO_TABLE_NAME = "java:comp/env/jdbc/OrderInfoTableName";
	public static final String ORDER_ITEMS_TABLE_NAME = "java:comp/env/jdbc/OrderItemsTableName";

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "OrderSeq")
	@TableGenerator(name = "OrderSeq", table = "IDGENERATOR", pkColumnName = "IDNAME", pkColumnValue = "ORDER", valueColumnName = "IDVALUE")
	private String orderID;
	private String sellDate;
	private String billName;
	private String billAddr1;
	private String billAddr2;
	private String billCity;
	private String billState;
	private String billZip;
	private String billPhone;
	private String shipName;
	private String shipAddr1;
	private String shipAddr2;
	private String shipCity;
	private String shipState;
	private String shipZip;
	private String shipPhone;
	private String creditCard;
	private String ccNum;
	private String ccExpireMonth;
	private String ccExpireYear;
	private String cardHolder;
	private int shippingMethod;
	private float profit;

	@ManyToOne
	@JoinColumn(name = "CUSTOMERID")
	private Customer customer;
	@Transient
	private Collection orderItems;

	@Transient
	private Collection<OrderItem> items = null;

	/**
	 * Constructor to create an Order.
	 *
	 * @param customer
	 *            - customer who created the order
	 * @param billName
	 *            - billing name
	 * @param billAddr1
	 *            - billing address line 1
	 * @param billAddr2
	 *            - billing address line 2
	 * @param billCity
	 *            - billing address city
	 * @param billState
	 *            - billing address state
	 * @param billZip
	 *            - billing address zip code
	 * @param billPhone
	 *            - billing phone
	 * @param shipName
	 *            - shippng name
	 * @param shipAddr1
	 *            - shippng address line 1
	 * @param shipAddr2
	 *            - shippng address line 2
	 * @param shipCity
	 *            - shippng address city
	 * @param shipState
	 *            - shippng address state
	 * @param shipZip
	 *            - shippng address zip code
	 * @param shipPhone
	 *            - shippng phone
	 * @param creditCard
	 *            - credit card
	 * @param ccNum
	 *            - credit card number
	 * @param ccExpireMonth
	 *            - credit card expiration month
	 * @param ccExpireYear
	 *            - credit card expiration year
	 * @param cardHolder
	 *            - credit card holder name
	 * @param shippingMethod
	 *            int of shipping method used
	 * @param items
	 *            vector of StoreItems ordered
	 */
	public Order(Customer customer, String billName, String billAddr1, String billAddr2, String billCity,
			String billState, String billZip, String billPhone, String shipName, String shipAddr1, String shipAddr2,
			String shipCity, String shipState, String shipZip, String shipPhone, String creditCard, String ccNum,
			String ccExpireMonth, String ccExpireYear, String cardHolder, int shippingMethod,
			Collection<OrderItem> items) {
		this.setSellDate(Long.toString(System.currentTimeMillis()));

		// Pad it to 14 digits so sorting works properly.
		if (this.getSellDate().length() < 14) {
			StringBuffer sb = new StringBuffer(Util.ZERO_14);
			sb.replace((14 - this.getSellDate().length()), 14, this.getSellDate());
			this.setSellDate(sb.toString());
		}

		this.setCustomer(customer);
		this.setBillName(billName);
		this.setBillAddr1(billAddr1);
		this.setBillAddr2(billAddr2);
		this.setBillCity(billCity);
		this.setBillState(billState);
		this.setBillZip(billZip);
		this.setBillPhone(billPhone);
		this.setShipName(shipName);
		this.setShipAddr1(shipAddr1);
		this.setShipAddr2(shipAddr2);
		this.setShipCity(shipCity);
		this.setShipState(shipState);
		this.setShipZip(shipZip);
		this.setShipPhone(shipPhone);
		this.setCreditCard(creditCard);
		this.setCcNum(ccNum);
		this.setCcExpireMonth(ccExpireMonth);
		this.setCcExpireYear(ccExpireYear);
		this.setCardHolder(cardHolder);
		this.setShippingMethod(shippingMethod);
		this.items = items;

		// Get profit for total order.
		OrderItem oi;
		float profit;
		profit = 0.0f;
		for (Object o : items) {
			oi = (OrderItem) o;
			profit = profit + (oi.getQuantity() * (oi.getPrice() - oi.getCost()));
			oi.setOrder(this);
		}
		this.setProfit(profit);
	}

	public Order(String orderID) {
		setOrderID(orderID);
	}

	public Order() {
	}

	public String getBillAddr1() {
		return billAddr1;
	}

	public void setBillAddr1(String billAddr1) {
		this.billAddr1 = billAddr1;
	}

	public String getBillAddr2() {
		return billAddr2;
	}

	public void setBillAddr2(String billAddr2) {
		this.billAddr2 = billAddr2;
	}

	public String getBillCity() {
		return billCity;
	}

	public void setBillCity(String billCity) {
		this.billCity = billCity;
	}

	public String getBillName() {
		return billName;
	}

	public void setBillName(String billName) {
		this.billName = billName;
	}

	public String getBillPhone() {
		return billPhone;
	}

	public void setBillPhone(String billPhone) {
		this.billPhone = billPhone;
	}

	public String getBillState() {
		return billState;
	}

	public void setBillState(String billState) {
		this.billState = billState;
	}

	public String getBillZip() {
		return billZip;
	}

	public void setBillZip(String billZip) {
		this.billZip = billZip;
	}

	public String getCardHolder() {
		return cardHolder;
	}

	public void setCardHolder(String cardHolder) {
		this.cardHolder = cardHolder;
	}

	public String getCcExpireMonth() {
		return ccExpireMonth;
	}

	public void setCcExpireMonth(String ccExpireMonth) {
		this.ccExpireMonth = ccExpireMonth;
	}

	public String getCcExpireYear() {
		return ccExpireYear;
	}

	public void setCcExpireYear(String ccExpireYear) {
		this.ccExpireYear = ccExpireYear;
	}

	public String getCcNum() {
		return ccNum;
	}

	public void setCcNum(String ccNum) {
		this.ccNum = ccNum;
	}

	public String getCreditCard() {
		return creditCard;
	}

	public void setCreditCard(String creditCard) {
		this.creditCard = creditCard;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Collection<OrderItem> getItems() {
		return items;
	}

	public void setItems(Collection<OrderItem> items) {
		this.items = items;
	}

	public String getOrderID() {
		return orderID;
	}

	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}

	public Collection getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(Collection orderItems) {
		this.orderItems = orderItems;
	}

	public float getProfit() {
		return profit;
	}

	public void setProfit(float profit) {
		this.profit = profit;
	}

	public String getSellDate() {
		return sellDate;
	}

	public void setSellDate(String sellDate) {
		this.sellDate = sellDate;
	}

	public String getShipAddr1() {
		return shipAddr1;
	}

	public void setShipAddr1(String shipAddr1) {
		this.shipAddr1 = shipAddr1;
	}

	public String getShipAddr2() {
		return shipAddr2;
	}

	public void setShipAddr2(String shipAddr2) {
		this.shipAddr2 = shipAddr2;
	}

	public String getShipCity() {
		return shipCity;
	}

	public void setShipCity(String shipCity) {
		this.shipCity = shipCity;
	}

	public String getShipName() {
		return shipName;
	}

	public void setShipName(String shipName) {
		this.shipName = shipName;
	}

	public String getShipPhone() {
		return shipPhone;
	}

	public void setShipPhone(String shipPhone) {
		this.shipPhone = shipPhone;
	}

	public int getShippingMethod() {
		return shippingMethod;
	}

	public void setShippingMethod(int shippingMethod) {
		this.shippingMethod = shippingMethod;
	}

	public String getShipZip() {
		return shipZip;
	}

	public void setShipZip(String shipZip) {
		this.shipZip = shipZip;
	}

	public String getShipState() {
		return shipState;
	}

	public void setShipState(String shipState) {
		this.shipState = shipState;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2004,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Bean mapping for the SUPPLIER table.
 */
@Entity(name = "Supplier")
@Table(name = "SUPPLIER", schema = "APP")
@NamedQueries({ @NamedQuery(name = "findAllSuppliers", query = "select s from Supplier s"),
		@NamedQuery(name = "removeAllSupplier", query = "delete from Supplier") })
public class Supplier {
	@Id
	private String supplierID;
	private String name;
	private String city;
	private String usstate;
	private String zip;
	private String phone;
	private String url;
	private String street;

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getSupplierID() {
		return supplierID;
	}

	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsstate() {
		return usstate;
	}

	public void setUsstate(String usstate) {
		this.usstate = usstate;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public Supplier() {
	}

	public Supplier(String supplierID) {
		setSupplierID(supplierID);
	}

	/**
	 * @param supplierID
	 * @param name
	 * @param street
	 * @param city
	 * @param state
	 * @param zip
	 * @param phone
	 * @param url
	 */
	public Supplier(String supplierID, String name, String street, String city, String state, String zip, String phone,
			String url) {
		this.setSupplierID(supplierID);
		this.setName(name);
		this.setStreet(street);
		this.setCity(city);
		this.setUsstate(state);
		this.setZip(zip);
		this.setPhone(phone);
		this.setUrl(url);
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Customer is the bean mapping for the CUSTOMER table.
 * 
 * @see Customer
 */
@Entity(name = "Customer")
@Table(name = "CUSTOMER", schema = "APP")
@NamedQueries({ @NamedQuery(name = "removeAllCustomers", query = "delete from Customer") })
public class Customer {
	@Id
	private String customerID;
	private String password;

	@NotNull
	@Size(min = 1, message = "First name must include at least one letter.")
	private String firstName;
	@NotNull
	@Size(min = 1, message = "Last name must include at least one letter.")
	private String lastName;
	@NotNull
	@Size(min = 1, message = "Address must include at least one letter.")
	private String addr1;
	private String addr2;
	@NotNull
	@Size(min = 1, message = "City name must include at least one letter.")
	private String addrCity;
	@NotNull
	@Size(min = 2, message = "State must include at least two letters.")
	private String addrState;
	@Pattern(regexp = "\\d{5}", message = "Zip code does not have 5 digits.")
	private String addrZip;
	@NotNull
	@Pattern(regexp = "\\d{3}-\\d{3}-\\d{4}", message = "Phone number does not match xxx-xxx-xxxx.")
	private String phone;

	public Customer() {
	}

	/**
	 * Create a new Customer.
	 *
	 * @param key
	 *            CustomerKey
	 * @param password
	 *            Password used for this customer account.
	 * @param firstName
	 *            First name of the customer.
	 * @param lastName
	 *            Last name of the customer
	 * @param addr1
	 *            Street address of the customer
	 * @param addr2
	 *            Street address of the customer
	 * @param addrCity
	 *            City
	 * @param addrState
	 *            State
	 * @param addrZip
	 *            Zip code
	 * @param phone
	 *            Phone number
	 */
	public Customer(String key, String password, String firstName, String lastName, String addr1, String addr2,
			String addrCity, String addrState, String addrZip, String phone) {
		this.setCustomerID(key);
		this.setPassword(password);
		this.setFirstName(firstName);
		this.setLastName(lastName);
		this.setAddr1(addr1);
		this.setAddr2(addr2);
		this.setAddrCity(addrCity);
		this.setAddrState(addrState);
		this.setAddrZip(addrZip);
		this.setPhone(phone);
	}

	/**
	 * Verify password.
	 *
	 * @param password
	 *            value to be checked.
	 * @return True, if password matches one stored.
	 */
	public boolean verifyPassword(String password) {
		return this.getPassword().equals(password);
	}

	/**
	 * Get the customer's full name.
	 * 
	 * @return String of customer's full name.
	 */
	public String getFullName() {
		return this.getFirstName() + " " + this.getLastName();
	}

	public String getAddr1() {
		return addr1;
	}

	public void setAddr1(String addr1) {
		this.addr1 = addr1;
	}

	public String getAddr2() {
		return addr2;
	}

	public void setAddr2(String addr2) {
		this.addr2 = addr2;
	}

	public String getAddrCity() {
		return addrCity;
	}

	public void setAddrCity(String addrCity) {
		this.addrCity = addrCity;
	}

	public String getAddrState() {
		return addrState;
	}

	public void setAddrState(String addrState) {
		this.addrState = addrState;
	}

	public String getAddrZip() {
		return addrZip;
	}

	public void setAddrZip(String addrZip) {
		this.addrZip = addrZip;
	}

	public String getCustomerID() {
		return customerID;
	}

	public void setCustomerID(String customerID) {
		this.customerID = customerID;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import com.ibm.websphere.samples.pbw.jpa.BackOrder;
import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * A class to hold a back order item's data.
 */
public class BackOrderItem implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private int inventoryQuantity;
	private String backOrderID; // from BackOrder
	private int quantity; // from BackOrder
	private String status; // from BackOrder
	private long lowDate; // from BackOrder
	private long orderDate; // from BackOrder
	private String supplierOrderID; // from BackOrder
	private Inventory inventory; // from BackOrder

	/**
	 * @see java.lang.Object#Object()
	 */
	/** Default constructor. */
	public BackOrderItem() {
	}

	/**
	 * Method BackOrderItem.
	 * 
	 * @param backOrderID
	 * @param inventoryID
	 * @param name
	 * @param quantity
	 * @param status
	 */
	public BackOrderItem(String backOrderID, Inventory inventoryID, String name, int quantity, String status) {
		this.backOrderID = backOrderID;
		this.inventory = inventoryID;
		this.name = name;
		this.quantity = quantity;
		this.status = status;
	}

	/**
	 * Method BackOrderItem.
	 * 
	 * @param backOrder
	 */
	public BackOrderItem(BackOrder backOrder) {
		try {
			this.backOrderID = backOrder.getBackOrderID();
			this.inventory = backOrder.getInventory();
			this.quantity = backOrder.getQuantity();
			this.status = backOrder.getStatus();
			this.lowDate = backOrder.getLowDate();
			this.orderDate = backOrder.getOrderDate();
			this.supplierOrderID = backOrder.getSupplierOrderID();
		} catch (Exception e) {
			Util.debug("BackOrderItem - Exception: " + e);
		}
	}

	/**
	 * Method getBackOrderID.
	 * 
	 * @return String
	 */
	public String getBackOrderID() {
		return backOrderID;
	}

	/**
	 * Method setBackOrderID.
	 * 
	 * @param backOrderID
	 */
	public void setBackOrderID(String backOrderID) {
		this.backOrderID = backOrderID;
	}

	/**
	 * Method getSupplierOrderID.
	 * 
	 * @return String
	 */
	public String getSupplierOrderID() {
		return supplierOrderID;
	}

	/**
	 * Method setSupplierOrderID.
	 * 
	 * @param supplierOrderID
	 */
	public void setSupplierOrderID(String supplierOrderID) {
		this.supplierOrderID = supplierOrderID;
	}

	/**
	 * Method setQuantity.
	 * 
	 * @param quantity
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/**
	 * Method getInventoryID.
	 * 
	 * @return String
	 */
	public Inventory getInventory() {
		return inventory;
	}

	/**
	 * Method getName.
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Method setName.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Method getQuantity.
	 * 
	 * @return int
	 */
	public int getQuantity() {
		return quantity;
	}

	/**
	 * Method getInventoryQuantity.
	 * 
	 * @return int
	 */
	public int getInventoryQuantity() {
		return inventoryQuantity;
	}

	/**
	 * Method setInventoryQuantity.
	 * 
	 * @param quantity
	 */
	public void setInventoryQuantity(int quantity) {
		this.inventoryQuantity = quantity;
	}

	/**
	 * Method getStatus.
	 * 
	 * @return String
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Method getLowDate.
	 * 
	 * @return long
	 */
	public long getLowDate() {
		return lowDate;
	}

	/**
	 * Method getOrderDate.
	 * 
	 * @return long
	 */
	public long getOrderDate() {
		return orderDate;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Provides backing bean support for the product web page. Accessed via the shopping bean.
 *
 */
public class ProductBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private Inventory inventory;
	private int quantity;

	protected ProductBean(Inventory inventory) {
	    Objects.requireNonNull(inventory, "Inventory cannot be null");
		this.inventory = inventory;
		this.quantity = 1;
	}

	public String getCategoryName() {
		return Util.getCategoryString(this.inventory.getCategory());
	}

	public Inventory getInventory() {
		return this.inventory;
	}

	public String getMenuString() {
		String categoryString = getCategoryName();

		if (categoryString.equals("Flowers")) {
			return "banner:menu1";
		}

		else if (categoryString.equals("Fruits & Vegetables")) {
			return "banner:menu2";
		}

		else if (categoryString.equals("Trees")) {
			return "banner:menu3";
		}

		else {
			return "banner:menu4";
		}
	}

	public String getPrice() {
		return NumberFormat.getCurrencyInstance(Locale.US).format(new Float(this.inventory.getPrice()));
	}

	public int getQuantity() {
		return this.quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.util.Calendar;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.ibm.websphere.samples.pbw.jpa.Order;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * A class to hold an order's data.
 */
public class OrderInfo implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private String orderID;
	@NotNull
	@Size(min = 1, message = "Name for billing must include at least one letter.")
	private String billName;
	@NotNull
	@Size(min = 1, message = "Billing address must include at least one letter.")
	private String billAddr1;
	private String billAddr2;
	@NotNull
	@Size(min = 1, message = "Billing city must include at least one letter.")
	private String billCity;
	@NotNull
	@Size(min = 1, message = "Billing state must include at least one letter.")
	private String billState;

	@Pattern(regexp = "\\d{5}", message = "Billing zip code does not have 5 digits.")
	private String billZip;

	@Pattern(regexp = "\\d{3}-\\d{3}-\\d{4}", message = "Billing phone number does not match xxx-xxx-xxxx.")
	private String billPhone;
	@NotNull
	@Size(min = 1, message = "Name for shipping must include at least one letter.")
	private String shipName;
	@NotNull
	@Size(min = 1, message = "Shipping address must include at least one letter.")
	private String shipAddr1;
	private String shipAddr2;
	@NotNull
	@Size(min = 1, message = "Shipping city must include at least one letter.")
	private String shipCity;
	@NotNull
	@Size(min = 1, message = "Shipping state must include at least one letter.")
	private String shipState;

	@Pattern(regexp = "[0-9][0-9][0-9][0-9][0-9]", message = "Shipping zip code does not have 5 digits.")
	private String shipZip;

	@Pattern(regexp = "\\d{3}-\\d{3}-\\d{4}", message = "Shipping phone number does not match xxx-xxx-xxxx.")
	private String shipPhone;
	private int shippingMethod;
	@NotNull
	@Size(min = 1, message = "Card holder name must include at least one letter.")
	private String cardholderName;
	private String cardName;

	@Pattern(regexp = "\\d{4} \\d{4} \\d{4} \\d{4}", message = "Credit card numbers must be entered as XXXX XXXX XXXX XXXX.")
	private String cardNum;
	private String cardExpMonth;
	private String cardExpYear;
	private String[] cardExpYears;
	private boolean shipisbill = false;

	/**
	 * Constructor to create an OrderInfo by passing each field.
	 */
	public OrderInfo(String billName, String billAddr1, String billAddr2, String billCity, String billState,
			String billZip, String billPhone, String shipName, String shipAddr1, String shipAddr2, String shipCity,
			String shipState, String shipZip, String shipPhone, int shippingMethod, String orderID) {
		this.orderID = orderID;
		this.billName = billName;
		this.billAddr1 = billAddr1;
		this.billAddr2 = billAddr2;
		this.billCity = billCity;
		this.billState = billState;
		this.billZip = billZip;
		this.billPhone = billPhone;
		this.shipName = shipName;
		this.shipAddr1 = shipAddr1;
		this.shipAddr2 = shipAddr2;
		this.shipCity = shipCity;
		this.shipState = shipState;
		this.shipZip = shipZip;
		this.shipPhone = shipPhone;
		this.shippingMethod = shippingMethod;
		initLists();
		cardholderName = "";
		cardNum = "";
	}

	/**
	 * Constructor to create an OrderInfo using an Order.
	 * 
	 * @param order
	 */
	public OrderInfo(Order order) {
		orderID = order.getOrderID();
		billName = order.getBillName();
		billAddr1 = order.getBillAddr1();
		billAddr2 = order.getBillAddr2();
		billCity = order.getBillCity();
		billState = order.getBillState();
		billZip = order.getBillZip();
		billPhone = order.getBillPhone();
		shipName = order.getShipName();
		shipAddr1 = order.getShipAddr1();
		shipAddr2 = order.getShipAddr2();
		shipCity = order.getShipCity();
		shipState = order.getShipState();
		shipZip = order.getShipZip();
		shipPhone = order.getShipPhone();
		shippingMethod = order.getShippingMethod();
	}

	/**
	 * Get the shipping method name.
	 */
	public String getShippingMethodName() {
		return getShippingMethods()[shippingMethod];
	}

	/**
	 * Set the shipping method by name
	 */
	public void setShippingMethodName(String name) {
		String[] methodNames = Util.getShippingMethodStrings();
		for (int i = 0; i < methodNames.length; i++) {
			if (methodNames[i].equals(name))
				shippingMethod = i;
		}
	}

	/**
	 * Get shipping methods that are possible.
	 * 
	 * @return String[] of method names
	 */
	public String[] getShippingMethods() {
		return Util.getFullShippingMethodStrings();
	}

	public int getShippingMethodCount() {
		return Util.getShippingMethodStrings().length;
	}

	private void initLists() {
		int i = Calendar.getInstance().get(1);
		cardExpYears = new String[5];
		for (int j = 0; j < 5; j++)
			cardExpYears[j] = (new Integer(i + j)).toString();
	}

	/**
	 * @return the orderID
	 */
	public String getID() {
		return orderID;
	}

	/**
	 * @param orderID
	 *            the orderID to set
	 */
	public void setID(String orderID) {
		this.orderID = orderID;
	}

	/**
	 * @return the billName
	 */
	public String getBillName() {
		return billName;
	}

	/**
	 * @param billName
	 *            the billName to set
	 */
	public void setBillName(String billName) {
		this.billName = billName;
	}

	/**
	 * @return the billAddr1
	 */
	public String getBillAddr1() {
		return billAddr1;
	}

	/**
	 * @param billAddr1
	 *            the billAddr1 to set
	 */
	public void setBillAddr1(String billAddr1) {
		this.billAddr1 = billAddr1;
	}

	/**
	 * @return the billAddr2
	 */
	public String getBillAddr2() {
		return billAddr2;
	}

	/**
	 * @param billAddr2
	 *            the billAddr2 to set
	 */
	public void setBillAddr2(String billAddr2) {
		this.billAddr2 = billAddr2;
	}

	/**
	 * @return the billCity
	 */
	public String getBillCity() {
		return billCity;
	}

	/**
	 * @param billCity
	 *            the billCity to set
	 */
	public void setBillCity(String billCity) {
		this.billCity = billCity;
	}

	/**
	 * @return the billState
	 */
	public String getBillState() {
		return billState;
	}

	/**
	 * @param billState
	 *            the billState to set
	 */
	public void setBillState(String billState) {
		this.billState = billState;
	}

	/**
	 * @return the billZip
	 */
	public String getBillZip() {
		return billZip;
	}

	/**
	 * @param billZip
	 *            the billZip to set
	 */
	public void setBillZip(String billZip) {
		this.billZip = billZip;
	}

	/**
	 * @return the billPhone
	 */
	public String getBillPhone() {
		return billPhone;
	}

	/**
	 * @param billPhone
	 *            the billPhone to set
	 */
	public void setBillPhone(String billPhone) {
		this.billPhone = billPhone;
	}

	/**
	 * @return the shipName
	 */
	public String getShipName() {
		return shipName;
	}

	/**
	 * @param shipName
	 *            the shipName to set
	 */
	public void setShipName(String shipName) {
		this.shipName = shipName;
	}

	/**
	 * @return the shipAddr1
	 */
	public String getShipAddr1() {
		return shipAddr1;
	}

	/**
	 * @param shipAddr1
	 *            the shipAddr1 to set
	 */
	public void setShipAddr1(String shipAddr1) {
		this.shipAddr1 = shipAddr1;
	}

	/**
	 * @return the shipAddr2
	 */
	public String getShipAddr2() {
		return shipAddr2;
	}

	/**
	 * @param shipAddr2
	 *            the shipAddr2 to set
	 */
	public void setShipAddr2(String shipAddr2) {
		this.shipAddr2 = shipAddr2;
	}

	/**
	 * @return the shipCity
	 */
	public String getShipCity() {
		return shipCity;
	}

	/**
	 * @param shipCity
	 *            the shipCity to set
	 */
	public void setShipCity(String shipCity) {
		this.shipCity = shipCity;
	}

	/**
	 * @return the shipState
	 */
	public String getShipState() {
		return shipState;
	}

	/**
	 * @param shipState
	 *            the shipState to set
	 */
	public void setShipState(String shipState) {
		this.shipState = shipState;
	}

	/**
	 * @return the shipZip
	 */
	public String getShipZip() {
		return shipZip;
	}

	/**
	 * @param shipZip
	 *            the shipZip to set
	 */
	public void setShipZip(String shipZip) {
		this.shipZip = shipZip;
	}

	/**
	 * @return the shipPhone
	 */
	public String getShipPhone() {
		return shipPhone;
	}

	/**
	 * @param shipPhone
	 *            the shipPhone to set
	 */
	public void setShipPhone(String shipPhone) {
		this.shipPhone = shipPhone;
	}

	/**
	 * @return the shippingMethod
	 */
	public int getShippingMethod() {
		return shippingMethod;
	}

	/**
	 * @param shippingMethod
	 *            the shippingMethod to set
	 */
	public void setShippingMethod(int shippingMethod) {
		this.shippingMethod = shippingMethod;
	}

	/**
	 * @return the cardholderName
	 */
	public String getCardholderName() {
		return cardholderName;
	}

	/**
	 * @param cardholderName
	 *            the cardholderName to set
	 */
	public void setCardholderName(String cardholderName) {
		this.cardholderName = cardholderName;
	}

	/**
	 * @return the cardName
	 */
	public String getCardName() {
		return cardName;
	}

	/**
	 * @param cardName
	 *            the cardName to set
	 */
	public void setCardName(String cardName) {
		this.cardName = cardName;
	}

	/**
	 * @return the cardNum
	 */
	public String getCardNum() {
		return cardNum;
	}

	/**
	 * @param cardNum
	 *            the cardNum to set
	 */
	public void setCardNum(String cardNum) {
		this.cardNum = cardNum;
	}

	/**
	 * @return the cardExpMonth
	 */
	public String getCardExpMonth() {
		return cardExpMonth;
	}

	/**
	 * @param cardExpMonth
	 *            the cardExpMonth to set
	 */
	public void setCardExpMonth(String cardExpMonth) {
		this.cardExpMonth = cardExpMonth;
	}

	/**
	 * @return the cardExpYear
	 */
	public String getCardExpYear() {
		return cardExpYear;
	}

	/**
	 * @param cardExpYear
	 *            the cardExpYear to set
	 */
	public void setCardExpYear(String cardExpYear) {
		this.cardExpYear = cardExpYear;
	}

	/**
	 * @return the cardExpYears
	 */
	public String[] getCardExpYears() {
		return cardExpYears;
	}

	/**
	 * @param cardExpYears
	 *            the cardExpYears to set
	 */
	public void setCardExpYears(String[] cardExpYears) {
		this.cardExpYears = cardExpYears;
	}

	/**
	 * @return the shipisbill
	 */
	public boolean isShipisbill() {
		return shipisbill;
	}

	/**
	 * @param shipisbill
	 *            the shipisbill to set
	 */
	public void setShipisbill(boolean shipisbill) {
		this.shipisbill = shipisbill;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import java.io.Serializable;

import javax.validation.constraints.Min;

import com.ibm.websphere.samples.pbw.jpa.BackOrder;
import com.ibm.websphere.samples.pbw.jpa.Inventory;

/**
 * ShoppingItem wraps the JPA Inventory entity class to provide additional methods needed by the web
 * app.
 */
public class ShoppingItem implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;
	private Inventory item;

	public ShoppingItem() {

	}

	public ShoppingItem(Inventory i) {
		item = i;
	}

	public ShoppingItem(String key, String name, String heading, String desc, String pkginfo, String image, float price,
			float cost, int quantity, int category, String notes, boolean isPublic) {
		item = new Inventory(key, name, heading, desc, pkginfo, image, price, cost, quantity, category, notes,
				isPublic);
	}

	/**
	 * Subtotal price calculates a cost based on price and quantity.
	 */
	public float getSubtotalPrice() {
		return getPrice() * getQuantity();
	}

	/**
	 * @param o
	 * @return boolean true if object equals this
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return item.equals(o);
	}

	/**
	 * @return int hashcode for this object
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return item.hashCode();
	}

	/**
	 * @return String String representation of this object
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return item.toString();
	}

	/**
	 * @param quantity
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#increaseInventory(int)
	 */
	public void increaseInventory(int quantity) {
		item.increaseInventory(quantity);
	}

	/**
	 * @return int category enum int value
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getCategory()
	 */
	public int getCategory() {
		return item.getCategory();
	}

	/**
	 * @param category
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setCategory(int)
	 */
	public void setCategory(int category) {
		item.setCategory(category);
	}

	/**
	 * @return float cost of the item
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getCost()
	 */
	public float getCost() {
		return item.getCost();
	}

	/**
	 * @param cost
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setCost(float)
	 */
	public void setCost(float cost) {
		item.setCost(cost);
	}

	/**
	 * @return String description of the item
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getDescription()
	 */
	public String getDescription() {
		return item.getDescription();
	}

	/**
	 * @param description
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		item.setDescription(description);
	}

	/**
	 * @return String item heading
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getHeading()
	 */
	public String getHeading() {
		return item.getHeading();
	}

	/**
	 * @param heading
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setHeading(java.lang.String)
	 */
	public void setHeading(String heading) {
		item.setHeading(heading);
	}

	/**
	 * @return String image URI
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getImage()
	 */
	public String getImage() {
		return item.getImage();
	}

	/**
	 * @param image
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setImage(java.lang.String)
	 */
	public void setImage(String image) {
		item.setImage(image);
	}

	/**
	 * @return String name of the item
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getName()
	 */
	public String getName() {
		return item.getName();
	}

	/**
	 * @param name
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setName(java.lang.String)
	 */
	public void setName(String name) {
		item.setName(name);
	}

	/**
	 * @return String item notes
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getNotes()
	 */
	public String getNotes() {
		return item.getNotes();
	}

	/**
	 * @param notes
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setNotes(java.lang.String)
	 */
	public void setNotes(String notes) {
		item.setNotes(notes);
	}

	/**
	 * @return String package information
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getPkginfo()
	 */
	public String getPkginfo() {
		return item.getPkginfo();
	}

	/**
	 * @param pkginfo
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setPkginfo(java.lang.String)
	 */
	public void setPkginfo(String pkginfo) {
		item.setPkginfo(pkginfo);
	}

	/**
	 * @return float Price of the item
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getPrice()
	 */
	public float getPrice() {
		return item.getPrice();
	}

	/**
	 * @param price
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setPrice(float)
	 */
	public void setPrice(float price) {
		item.setPrice(price);
	}

	/**
	 * Property accessor for quantity of items ordered. Quantity may not be less than zero. Bean
	 * Validation will ensure this is true.
	 * 
	 * @return int quantity of items
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getQuantity()
	 */
	@Min(value = 0, message = "Quantity must be a number greater than or equal to zero.")
	public int getQuantity() {
		return item.getQuantity();
	}

	/**
	 * @param quantity
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setQuantity(int)
	 */
	public void setQuantity(int quantity) {
		item.setQuantity(quantity);
	}

	/**
	 * @return int maximum threshold
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getMaxThreshold()
	 */
	public int getMaxThreshold() {
		return item.getMaxThreshold();
	}

	/**
	 * @param maxThreshold
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setMaxThreshold(int)
	 */
	public void setMaxThreshold(int maxThreshold) {
		item.setMaxThreshold(maxThreshold);
	}

	/**
	 * @return int minimum threshold
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getMinThreshold()
	 */
	public int getMinThreshold() {
		return item.getMinThreshold();
	}

	/**
	 * @param minThreshold
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setMinThreshold(int)
	 */
	public void setMinThreshold(int minThreshold) {
		item.setMinThreshold(minThreshold);
	}

	/**
	 * @return String item ID in the inventory
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getInventoryId()
	 */
	public String getInventoryId() {
		return item.getInventoryId();
	}

	/**
	 * @param id
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setInventoryId(java.lang.String)
	 */
	public void setInventoryId(String id) {
		item.setInventoryId(id);
	}

	/**
	 * @return String item ID
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getID()
	 */
	public String getID() {
		return item.getID();
	}

	/**
	 * @param id
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setID(java.lang.String)
	 */
	public void setID(String id) {
		item.setID(id);
	}

	/**
	 * @return boolean true if this is a public item
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#isPublic()
	 */
	public boolean isPublic() {
		return item.isPublic();
	}

	/**
	 * @param isPublic
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setIsPublic(boolean)
	 */
	public void setIsPublic(boolean isPublic) {
		item.setIsPublic(isPublic);
	}

	/**
	 * @param isPublic
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setPrivacy(boolean)
	 */
	public void setPrivacy(boolean isPublic) {
		item.setPrivacy(isPublic);
	}

	/**
	 * @return byte[] item image as a byte array
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getImgbytes()
	 */
	public byte[] getImgbytes() {
		return item.getImgbytes();
	}

	/**
	 * @param imgbytes
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setImgbytes(byte[])
	 */
	public void setImgbytes(byte[] imgbytes) {
		item.setImgbytes(imgbytes);
	}

	/**
	 * @return BackOrder item is on back order
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#getBackOrder()
	 */
	public BackOrder getBackOrder() {
		return item.getBackOrder();
	}

	/**
	 * @param backOrder
	 * @see com.ibm.websphere.samples.pbw.jpa.Inventory#setBackOrder(com.ibm.websphere.samples.pbw.jpa.BackOrder)
	 */
	public void setBackOrder(BackOrder backOrder) {
		item.setBackOrder(backOrder);
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2004,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import com.ibm.websphere.samples.pbw.bean.BackOrderMgr;
import com.ibm.websphere.samples.pbw.bean.CatalogMgr;
import com.ibm.websphere.samples.pbw.bean.CustomerMgr;
import com.ibm.websphere.samples.pbw.bean.ResetDBBean;
import com.ibm.websphere.samples.pbw.bean.ShoppingCartBean;
import com.ibm.websphere.samples.pbw.bean.SuppliersBean;
import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * A basic POJO class for resetting the database.
 */
public class Populate {

	private ResetDBBean resetDB;

	private CatalogMgr catalog;

	private CustomerMgr login;

	private ShoppingCartBean cart;

	private BackOrderMgr backOrderStock;

	private SuppliersBean suppliers;

	/**
	 * 
	 */
	public Populate() {
	}

	public Populate(ResetDBBean resetDB, CatalogMgr c, CustomerMgr l, BackOrderMgr b, SuppliersBean s) {
		this.resetDB = resetDB;
		this.catalog = c;
		this.login = l;
		this.backOrderStock = b;
		this.suppliers = s;
	}

	/**
	 * @param itemID
	 * @param fileName
	 * @param catalog
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void addImage(String itemID,
			String fileName,
			CatalogMgr catalog) throws FileNotFoundException, IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("resources/images/" + fileName);
		Util.debug("URL: " + url);
		fileName = url.getPath();
		Util.debug("Fully-qualified Filename: " + fileName);
		File imgFile = new File(fileName);
		// Open the input file as a stream of bytes
		FileInputStream fis = new FileInputStream(imgFile);
		DataInputStream dis = new DataInputStream(fis);
		int dataSize = dis.available();
		byte[] data = new byte[dataSize];
		dis.readFully(data);
		catalog.setItemImageBytes(itemID, data);
	}

	/**
	 * 
	 */
	public void doPopulate() {
		try {
			resetDB.deleteAll();
		} catch (Exception e) {
			Util.debug("Populate:doPopulate() - Exception deleting data in database: " + e);
			e.printStackTrace();
		}
		/**
		 * Populate INVENTORY table with text
		 */
		Util.debug("Populating INVENTORY table with text...");
		try {
			String[] values = Util.getProperties("inventory");
			for (int index = 0; index < values.length; index++) {
				Util.debug("Found INVENTORY property values:  " + values[index]);
				String[] fields = Util.readTokens(values[index], "|");
				String id = fields[0];
				String name = fields[1];
				String heading = fields[2];
				String descr = fields[3];
				String pkginfo = fields[4];
				String image = fields[5];
				float price = new Float(fields[6]).floatValue();
				float cost = new Float(fields[7]).floatValue();
				int quantity = new Integer(fields[8]).intValue();
				int category = new Integer(fields[9]).intValue();
				String notes = fields[10];
				boolean isPublic = new Boolean(fields[11]).booleanValue();
				Util.debug("Populating INVENTORY with following values:  ");
				Util.debug(fields[0]);
				Util.debug(fields[1]);
				Util.debug(fields[2]);
				Util.debug(fields[3]);
				Util.debug(fields[4]);
				Util.debug(fields[5]);
				Util.debug(fields[6]);
				Util.debug(fields[7]);
				Util.debug(fields[8]);
				Util.debug(fields[9]);
				Util.debug(fields[10]);
				Util.debug(fields[11]);
				Inventory storeItem = new Inventory(id, name, heading, descr, pkginfo, image, price, cost, quantity,
						category, notes, isPublic);
				catalog.addItem(storeItem);
				addImage(id, image, catalog);
			}
			Util.debug("INVENTORY table populated with text...");
		} catch (Exception e) {
			Util.debug("Unable to populate INVENTORY table with text data: " + e);
		}
		/**
		 * Populate CUSTOMER table with text
		 */
		Util.debug("Populating CUSTOMER table with default values...");
		try {
			String[] values = Util.getProperties("customer");
			Util.debug("Found CUSTOMER properties:  " + values[0]);
			for (int index = 0; index < values.length; index++) {
				String[] fields = Util.readTokens(values[index], "|");
				String customerID = fields[0];
				String password = fields[1];
				String firstName = fields[2];
				String lastName = fields[3];
				String addr1 = fields[4];
				String addr2 = fields[5];
				String addrCity = fields[6];
				String addrState = fields[7];
				String addrZip = fields[8];
				String phone = fields[9];
				Util.debug("Populating CUSTOMER with following values:  ");
				Util.debug(fields[0]);
				Util.debug(fields[1]);
				Util.debug(fields[2]);
				Util.debug(fields[3]);
				Util.debug(fields[4]);
				Util.debug(fields[5]);
				Util.debug(fields[6]);
				Util.debug(fields[7]);
				Util.debug(fields[8]);
				Util.debug(fields[9]);
				login.createCustomer(customerID, password, firstName, lastName, addr1, addr2, addrCity, addrState, addrZip, phone);
			}
		} catch (Exception e) {
			Util.debug("Unable to populate CUSTOMER table with text data: " + e);
		}
		/**
		 * Populate ORDER table with text
		 */
		Util.debug("Populating ORDER table with default values...");
		try {
			String[] values = Util.getProperties("order");
			Util.debug("Found ORDER properties:  " + values[0]);
			if (values[0] != null && values.length > 0) {
				for (int index = 0; index < values.length; index++) {
					String[] fields = Util.readTokens(values[index], "|");
					if (fields != null && fields.length >= 21) {
						String customerID = fields[0];
						String billName = fields[1];
						String billAddr1 = fields[2];
						String billAddr2 = fields[3];
						String billCity = fields[4];
						String billState = fields[5];
						String billZip = fields[6];
						String billPhone = fields[7];
						String shipName = fields[8];
						String shipAddr1 = fields[9];
						String shipAddr2 = fields[10];
						String shipCity = fields[11];
						String shipState = fields[12];
						String shipZip = fields[13];
						String shipPhone = fields[14];
						int shippingMethod = Integer.parseInt(fields[15]);
						String creditCard = fields[16];
						String ccNum = fields[17];
						String ccExpireMonth = fields[18];
						String ccExpireYear = fields[19];
						String cardHolder = fields[20];
						Vector<Inventory> items = new Vector<Inventory>();
						Util.debug("Populating ORDER with following values:  ");
						Util.debug(fields[0]);
						Util.debug(fields[1]);
						Util.debug(fields[2]);
						Util.debug(fields[3]);
						Util.debug(fields[4]);
						Util.debug(fields[5]);
						Util.debug(fields[6]);
						Util.debug(fields[7]);
						Util.debug(fields[8]);
						Util.debug(fields[9]);
						Util.debug(fields[10]);
						Util.debug(fields[11]);
						Util.debug(fields[12]);
						Util.debug(fields[13]);
						Util.debug(fields[14]);
						Util.debug(fields[15]);
						Util.debug(fields[16]);
						Util.debug(fields[17]);
						Util.debug(fields[18]);
						Util.debug(fields[19]);
						Util.debug(fields[20]);
						cart.createOrder(customerID, billName, billAddr1, billAddr2, billCity, billState, billZip, billPhone, shipName, shipAddr1, shipAddr2, shipCity, shipState, shipZip, shipPhone, creditCard, ccNum, ccExpireMonth, ccExpireYear, cardHolder, shippingMethod, items);
					} else {
						Util.debug("Property does not contain enough fields: " + values[index]);
						Util.debug("Fields found were: " + fields);
					}
				}
			}
			// stmt.executeUpdate(" INSERT INTO ORDERITEM(INVENTORYID, NAME, PKGINFO, PRICE, COST,
			// CATEGORY, QUANTITY, SELLDATE, ORDER_ORDERID) VALUES ('A0001', 'Bulb Digger',
			// 'Assembled', 12.0, 5.0, 3, 900, '01054835419625', '1')");
		} catch (Exception e) {
			Util.debug("Unable to populate ORDERITEM table with text data: " + e);
			e.printStackTrace();
		}
		/**
		 * Populate BACKORDER table with text
		 */
		Util.debug("Populating BACKORDER table with default values...");
		try {
			String[] values = Util.getProperties("backorder");
			Util.debug("Found BACKORDER properties:  " + values[0]);
			// Inserting backorders
			for (int index = 0; index < values.length; index++) {
				String[] fields = Util.readTokens(values[index], "|");
				String inventoryID = fields[0];
				int amountToOrder = new Integer(fields[1]).intValue();
				int maximumItems = new Integer(fields[2]).intValue();
				Util.debug("Populating BACKORDER with following values:  ");
				Util.debug(inventoryID);
				Util.debug("amountToOrder -> " + amountToOrder);
				Util.debug("maximumItems -> " + maximumItems);
				backOrderStock.createBackOrder(inventoryID, amountToOrder, maximumItems);
			}
		} catch (Exception e) {
			Util.debug("Unable to populate BACKORDER table with text data: " + e);
		}
		/**
		 * Populate SUPPLIER table with text
		 */
		Util.debug("Populating SUPPLIER table with default values...");
		try {
			String[] values = Util.getProperties("supplier");
			Util.debug("Found SUPPLIER properties:  " + values[0]);
			// Inserting Suppliers
			for (int index = 0; index < values.length; index++) {
				String[] fields = Util.readTokens(values[index], "|");
				String supplierID = fields[0];
				String name = fields[1];
				String address = fields[2];
				String city = fields[3];
				String state = fields[4];
				String zip = fields[5];
				String phone = fields[6];
				String url = fields[7];
				Util.debug("Populating SUPPLIER with following values:  ");
				Util.debug(fields[0]);
				Util.debug(fields[1]);
				Util.debug(fields[2]);
				Util.debug(fields[3]);
				Util.debug(fields[4]);
				Util.debug(fields[5]);
				Util.debug(fields[6]);
				Util.debug(fields[7]);
				suppliers.createSupplier(supplierID, name, address, city, state, zip, phone, url);
			}
		} catch (Exception e) {
			Util.debug("Unable to populate SUPPLIER table with text data: " + e);
		}
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * A JSF validator class, not implemented in Bean Validation since validation is only required
 * during GUI interaction.
 */
@FacesValidator(value = "validatePasswords")
public class ValidatePasswords implements Validator {

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		UIInput otherComponent;
		String otherID = (String) component.getAttributes().get("otherPasswordID");
		String otherStr;
		String str = (String) value;

		otherComponent = (UIInput) context.getViewRoot().findComponent(otherID);
		otherStr = (String) otherComponent.getValue();

		if (!otherStr.equals(str)) {
			ValidatorUtils.addErrorMessage(context, "Passwords do not match.");
		}
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.pbw.bean.MailerAppException;
import com.ibm.websphere.samples.pbw.bean.MailerBean;
import com.ibm.websphere.samples.pbw.jpa.Customer;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * This class sends the email confirmation message.
 */
@Named("mailaction")
public class MailAction implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Inject
	private MailerBean mailer;

	/** Public constructor */
	public MailAction() {
	}

	/**
	 * Send the email order confirmation message.
	 *
	 * @param customer
	 *            The customer information.
	 * @param orderKey
	 *            The order number.
	 */
	public final void sendConfirmationMessage(Customer customer,
			String orderKey) {
		try {
			System.out.println("mailer=" + mailer);
			mailer.createAndSendMail(customer, orderKey);
		}
		// The MailerAppException will be ignored since mail may not be configured.
		catch (MailerAppException e) {
			Util.debug("Mailer threw exception, mail may not be configured. Exception:" + e);
		}
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.pbw.bean.CatalogMgr;
import com.ibm.websphere.samples.pbw.bean.CustomerMgr;
import com.ibm.websphere.samples.pbw.jpa.Customer;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Servlet to handle customer account actions, such as login and register.
 */
@Named(value = "accountservlet")
@WebServlet("/servlet/AccountServlet")
public class AccountServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	// Servlet action codes.
	public static final String ACTION_ACCOUNT = "account";
	public static final String ACTION_ACCOUNTUPDATE = "accountUpdate";
	public static final String ACTION_LOGIN = "login";
	public static final String ACTION_REGISTER = "register";
	public static final String ACTION_SETLOGGING = "SetLogging";

	@Inject
	private CustomerMgr login;
	@Inject
	private CatalogMgr catalog;

	/**
	 * Servlet initialization.
	 */
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
	public void doGet(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
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
	public void doPost(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
		performTask(request, response);
	}

	/**
	 * Main service method for AccountServlet
	 *
	 * @param request
	 *            Object that encapsulates the request to the servlet
	 * @param response
	 *            Object that encapsulates the response from the servlet
	 */
	private void performTask(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String action = null;

		action = req.getParameter(Util.ATTR_ACTION);
		Util.debug("action=" + action);

		if (action.equals(ACTION_LOGIN)) {
			try {
				HttpSession session = req.getSession(true);
				String userid = req.getParameter("userid");
				String passwd = req.getParameter("passwd");
				String updating = req.getParameter(Util.ATTR_UPDATING);

				String results = null;
				if (Util.validateString(userid)) {
					results = login.verifyUserAndPassword(userid, passwd);
				} else {
					// user id was invalid, and may contain XSS attack
					results = "\nEmail address was invalid.";
					Util.debug("User id or email address was invalid. id=" + userid);
				}

				// If results have an error msg, return it, otherwise continue.
				if (results != null) {
					// Proliferate UPDATING flag if user is trying to update his account.
					if (updating.equals("true"))
						req.setAttribute(Util.ATTR_UPDATING, "true");

					req.setAttribute(Util.ATTR_RESULTS, results);
					requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_LOGIN);
				} else {
					// If not logging in for the first time, then clear out the
					// session data for the old user.
					if (session.getAttribute(Util.ATTR_CUSTOMER) != null) {
						session.removeAttribute(Util.ATTR_CART);
						// session.removeAttribute(Util.ATTR_CART_CONTENTS);
						session.removeAttribute(Util.ATTR_CHECKOUT);
						session.removeAttribute(Util.ATTR_ORDERKEY);
					}

					// Store customer userid in HttpSession.
					Customer customer = login.getCustomer(userid);
					session.setAttribute(Util.ATTR_CUSTOMER, customer);
					Util.debug("updating=" + updating + "=");

					// Was customer trying to edit account information.
					if (updating.equals("true")) {
						req.setAttribute(Util.ATTR_EDITACCOUNTINFO, customer);

						requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_ACCOUNT);
					} else {
						// See if user was in the middle of checking out.
						Boolean checkingOut = (Boolean) session.getAttribute(Util.ATTR_CHECKOUT);
						Util.debug("checkingOut=" + checkingOut + "=");
						if ((checkingOut != null) && (checkingOut.booleanValue())) {
							Util.debug("must be checking out");
							requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_ORDERINFO);
						} else {
							Util.debug("must NOT be checking out");
							String url;
							String category = (String) session.getAttribute(Util.ATTR_CATEGORY);

							// Default to plants
							if ((category == null) || (category.equals("null"))) {
								url = Util.PAGE_PROMO;
							} else {
								url = Util.PAGE_SHOPPING;
								req.setAttribute(Util.ATTR_INVITEMS, catalog
										.getItemsByCategory(Integer.parseInt(category)));
							}

							requestDispatch(getServletConfig().getServletContext(), req, resp, url);
						}
					}
				}
			} catch (ServletException e) {
				req.setAttribute(Util.ATTR_RESULTS, "/nException occurred");
				throw e;
			} catch (Exception e) {
				req.setAttribute(Util.ATTR_RESULTS, "/nException occurred");
				throw new ServletException(e.getMessage());
			}
		} else if (action.equals(ACTION_REGISTER)) {
			// Register a new user.
			// try
			// {
			String url;
			HttpSession session = req.getSession(true);

			String userid = req.getParameter("userid");
			String password = req.getParameter("passwd");
			String cpassword = req.getParameter("vpasswd");
			String firstName = req.getParameter("fname");
			String lastName = req.getParameter("lname");
			String addr1 = req.getParameter("addr1");
			String addr2 = req.getParameter("addr2");
			String addrCity = req.getParameter("city");
			String addrState = req.getParameter("state");
			String addrZip = req.getParameter("zip");
			String phone = req.getParameter("phone");

			// validate all user input
			if (!Util.validateString(userid)) {
				req.setAttribute(Util.ATTR_RESULTS, "Email address contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(firstName)) {
				req.setAttribute(Util.ATTR_RESULTS, "First Name contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(lastName)) {
				req.setAttribute(Util.ATTR_RESULTS, "Last Name contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(addr1)) {
				req.setAttribute(Util.ATTR_RESULTS, "Address Line 1 contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(addr2)) {
				req.setAttribute(Util.ATTR_RESULTS, "Address Line 2 contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(addrCity)) {
				req.setAttribute(Util.ATTR_RESULTS, "City contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(addrState)) {
				req.setAttribute(Util.ATTR_RESULTS, "State contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(addrZip)) {
				req.setAttribute(Util.ATTR_RESULTS, "Zip contains invalid characters.");
				url = Util.PAGE_REGISTER;
			} else if (!Util.validateString(phone)) {
				req.setAttribute(Util.ATTR_RESULTS, "Phone Number contains invalid characters.");
				url = Util.PAGE_REGISTER;
			}
			// Make sure passwords match.
			else if (!password.equals(cpassword)) {
				req.setAttribute(Util.ATTR_RESULTS, "Passwords do not match.");
				url = Util.PAGE_REGISTER;
			} else {
				// Create the new user.
				Customer customer = login
						.createCustomer(userid, password, firstName, lastName, addr1, addr2, addrCity, addrState, addrZip, phone);

				if (customer != null) {
					// Store customer info in HttpSession.
					session.setAttribute(Util.ATTR_CUSTOMER, customer);

					// See if user was in the middle of checking out.
					Boolean checkingOut = (Boolean) session.getAttribute(Util.ATTR_CHECKOUT);
					if ((checkingOut != null) && (checkingOut.booleanValue())) {
						url = Util.PAGE_ORDERINFO;
					} else {
						String category = (String) session.getAttribute(Util.ATTR_CATEGORY);

						// Default to plants
						if (category == null) {
							url = Util.PAGE_PROMO;
						} else {
							url = Util.PAGE_SHOPPING;
							req.setAttribute(Util.ATTR_INVITEMS, catalog
									.getItemsByCategory(Integer.parseInt(category)));
						}
					}
				} else {
					url = Util.PAGE_REGISTER;
					req.setAttribute(Util.ATTR_RESULTS, "New user NOT created!");
				}
			}
			requestDispatch(getServletConfig().getServletContext(), req, resp, url);
			// }
			// catch (CreateException e) { }
		} else if (action.equals(ACTION_ACCOUNT)) {
			String url;
			HttpSession session = req.getSession(true);
			Customer customer = (Customer) session.getAttribute(Util.ATTR_CUSTOMER);
			if (customer == null) {
				url = Util.PAGE_LOGIN;
				req.setAttribute(Util.ATTR_UPDATING, "true");
				req.setAttribute(Util.ATTR_RESULTS, "\nYou must login first.");
			} else {
				url = Util.PAGE_ACCOUNT;
				req.setAttribute(Util.ATTR_EDITACCOUNTINFO, customer);
			}
			requestDispatch(getServletConfig().getServletContext(), req, resp, url);
		} else if (action.equals(ACTION_ACCOUNTUPDATE)) {
			// try
			// {
			String url;
			HttpSession session = req.getSession(true);
			Customer customer = (Customer) session.getAttribute(Util.ATTR_CUSTOMER);

			String userid = customer.getCustomerID();
			String firstName = req.getParameter("fname");
			String lastName = req.getParameter("lname");
			String addr1 = req.getParameter("addr1");
			String addr2 = req.getParameter("addr2");
			String addrCity = req.getParameter("city");
			String addrState = req.getParameter("state");
			String addrZip = req.getParameter("zip");
			String phone = req.getParameter("phone");

			// Create the new user.
			customer = login.updateUser(userid, firstName, lastName, addr1, addr2, addrCity, addrState, addrZip, phone);
			// Store updated customer info in HttpSession.
			session.setAttribute(Util.ATTR_CUSTOMER, customer);

			// See if user was in the middle of checking out.
			Boolean checkingOut = (Boolean) session.getAttribute(Util.ATTR_CHECKOUT);
			if ((checkingOut != null) && (checkingOut.booleanValue())) {
				url = Util.PAGE_ORDERINFO;
			} else {
				String category = (String) session.getAttribute(Util.ATTR_CATEGORY);

				// Default to plants
				if (category == null) {
					url = Util.PAGE_PROMO;
				} else {
					url = Util.PAGE_SHOPPING;
					req.setAttribute(Util.ATTR_INVITEMS, catalog.getItemsByCategory(Integer.parseInt(category)));
				}
			}

			requestDispatch(getServletConfig().getServletContext(), req, resp, url);
			// }
			// catch (CreateException e) { }
		} else if (action.equals(ACTION_SETLOGGING)) {
			String debugSetting = req.getParameter("logging");
			if ((debugSetting == null) || (!debugSetting.equals("debug")))
				Util.setDebug(false);
			else
				Util.setDebug(true);

			requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_HELP);
		}
	}

	/**
	 * Request dispatch.
	 */
	private void requestDispatch(ServletContext ctx,
			HttpServletRequest req,
			HttpServletResponse resp,
			String page) throws ServletException, IOException {
		resp.setContentType("text/html");
		ctx.getRequestDispatcher(page).include(req, resp);
	}
}
//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.pbw.bean.CatalogMgr;
import com.ibm.websphere.samples.pbw.bean.ShoppingCartBean;
import com.ibm.websphere.samples.pbw.jpa.Inventory;

/**
 * A combination JSF action bean and backing bean for the shopping web page.
 *
 */
@Named(value = "shopping")
@SessionScoped
public class ShoppingBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String ACTION_CART = "cart";
	private static final String ACTION_PRODUCT = "product";
	private static final String ACTION_SHOPPING = "shopping";

	// keep an independent list of items so we can add pricing methods
	private ArrayList<ShoppingItem> cartItems;

	@Inject
	private CatalogMgr catalog;

	private ProductBean product;
	private LinkedList<ProductBean> products;
	private float shippingCost;

	@Inject
	private ShoppingCartBean shoppingCart;

	public String performAddToCart() {
		Inventory item = new Inventory(this.product.getInventory());

		item.setQuantity(this.product.getQuantity());

		shoppingCart.addItem(item);

		return performCart();
	}

	public String performCart() {
		cartItems = wrapInventoryItems(shoppingCart.getItems());

		return ShoppingBean.ACTION_CART;
	}

	public String performProductDetail() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		Map<String, String> requestParams = externalContext.getRequestParameterMap();

		this.product = new ProductBean(this.catalog.getItemInventory(requestParams.get("itemID")));

		return ShoppingBean.ACTION_PRODUCT;
	}

	public String performRecalculate() {

		shoppingCart.removeZeroQuantityItems();

		this.cartItems = wrapInventoryItems(shoppingCart.getItems());

		return performCart();
	}

	public String performShopping() {
		int category = 0;
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		Vector<Inventory> inventories;
		Map<String, String> requestParams = externalContext.getRequestParameterMap();

		try {
			category = Integer.parseInt(requestParams.get("category"));
		}

		catch (Throwable e) {
			if (this.products != null) {
				// No category specified, so just use the last one.

				return ShoppingBean.ACTION_SHOPPING;
			}
		}

		inventories = this.catalog.getItemsByCategory(category);

		this.products = new LinkedList<ProductBean>();

		// Have to convert all the inventory objects into product beans.

		for (Object obj : inventories) {
			Inventory inventory = (Inventory) obj;

			if (inventory.isPublic()) {
				this.products.add(new ProductBean(inventory));
			}
		}

		return ShoppingBean.ACTION_SHOPPING;
	}

	public Collection<ShoppingItem> getCartItems() {
		return this.cartItems;
	}

	public ProductBean getProduct() {
		return this.product;
	}

	public Collection<ProductBean> getProducts() {
		return this.products;
	}

	public String getShippingCostString() {
		return NumberFormat.getCurrencyInstance(Locale.US).format(this.shippingCost);
	}

	/**
	 * @return the shippingCost
	 */
	public float getShippingCost() {
		return shippingCost;
	}

	public void setShippingCost(float shippingCost) {
		this.shippingCost = shippingCost;

	}

	public float getTotalCost() {
		return shoppingCart.getSubtotalCost() + this.shippingCost;
	}

	public String getTotalCostString() {
		return NumberFormat.getCurrencyInstance(Locale.US).format(getTotalCost());
	}

	public ShoppingCartBean getCart() {
		return shoppingCart;
	}

	private ArrayList<ShoppingItem> wrapInventoryItems(Collection<Inventory> invItems) {
		ArrayList<ShoppingItem> shoppingList = new ArrayList<ShoppingItem>();
		for (Inventory i : invItems) {
			shoppingList.add(new ShoppingItem(i));
		}
		return shoppingList;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import java.io.Serializable;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.pbw.bean.ResetDBBean;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * JSF action bean for the help page.
 *
 */
@Named("help")
public class HelpBean implements Serializable {

	@Inject
	private ResetDBBean rdb;

	private String dbDumpFile;

	private static final String ACTION_HELP = "help";
	private static final String ACTION_HOME = "promo";

	public String performHelp() {
		return ACTION_HELP;
	}

	public String performDBReset() {
		rdb.resetDB();
		return ACTION_HOME;
	}

	/**
	 * @return the dbDumpFile
	 */
	public String getDbDumpFile() {
		return dbDumpFile;
	}

	/**
	 * @param dbDumpFile
	 *            the dbDumpFile to set
	 */
	public void setDbDumpFile(String dbDumpFile) {
		this.dbDumpFile = dbDumpFile;
	}

	/**
	 * @return whether debug is on or not
	 */
	public boolean isDebug() {
		return Util.debugOn();
	}

	/**
	 * Debugging is currently tied to the JavaServer Faces project stage. Any change here is likely
	 * to be automatically reset.
	 * 
	 * @param debug
	 *            Sets whether debug is on or not.
	 */
	public void setDebug(boolean debug) {
		Util.setDebug(debug);
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.ibm.websphere.samples.pbw.bean.BackOrderMgr;
import com.ibm.websphere.samples.pbw.bean.CatalogMgr;
import com.ibm.websphere.samples.pbw.bean.CustomerMgr;
import com.ibm.websphere.samples.pbw.bean.ResetDBBean;
import com.ibm.websphere.samples.pbw.bean.SuppliersBean;
import com.ibm.websphere.samples.pbw.jpa.BackOrder;
import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.jpa.Supplier;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Servlet to handle Administration actions
 */
@Named(value = "admin")
@WebServlet("/servlet/AdminServlet")
public class AdminServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private SuppliersBean suppliers = null;
	@Inject
	private CustomerMgr login;
	@Inject
	private BackOrderMgr backOrderStock = null;

	@Inject
	private CatalogMgr catalog = null;

	@Inject
	private ResetDBBean resetDB;

	/**
	 * @see javax.servlet.Servlet#init(ServletConfig)
	 */
	/**
	 * Servlet initialization.
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// Uncomment the following to generated debug code.
		// Util.setDebug(true);

	}

	/**
	 * Process incoming HTTP GET requests
	 *
	 * @param req
	 *            Object that encapsulates the request to the servlet
	 * @param resp
	 *            Object that encapsulates the response from the servlet
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performTask(req, resp);
	}

	/**
	 * Process incoming HTTP POST requests
	 *
	 * @param req
	 *            Object that encapsulates the request to the servlet
	 * @param resp
	 *            Object that encapsulates the response from the servlet
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		performTask(req, resp);
	}

	/**
	 * Method performTask.
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	public void performTask(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String admintype = null;
		admintype = req.getParameter(Util.ATTR_ADMINTYPE);
		Util.debug("inside AdminServlet:performTask. admintype=" + admintype);
		if ((admintype == null) || (admintype.equals(""))) {
			// Invalid Admin
			requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_ADMINHOME);
		}
		if (admintype.equals(Util.ADMIN_BACKORDER)) {
			performBackOrder(req, resp);
		} else if (admintype.equals(Util.ADMIN_SUPPLIERCFG)) {
			performSupplierConfig(req, resp);
		} else if (admintype.equals(Util.ADMIN_POPULATE)) {
			performPopulate(req, resp);
		}
	}

	/**
	 * @param supplierID
	 * @param name
	 * @param street
	 * @param city
	 * @param state
	 * @param zip
	 * @param phone
	 * @param location_url
	 * @return supplierInfo
	 */
	public Supplier updateSupplierInfo(String supplierID,
			String name,
			String street,
			String city,
			String state,
			String zip,
			String phone,
			String location_url) {
		// Only retrieving info for 1 supplier.
		Supplier supplier = null;
		try {
			supplier = suppliers.updateSupplier(supplierID, name, street, city, state, zip, phone, location_url);
		} catch (Exception e) {
			Util.debug("AdminServlet.updateSupplierInfo() - Exception: " + e);
		}
		return (supplier);
	}

	/**
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	public void performSupplierConfig(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		Supplier supplier = null;
		String action = null;
		action = req.getParameter(Util.ATTR_ACTION);
		if ((action == null) || (action.equals("")))
			action = Util.ACTION_GETSUPPLIER;
		Util.debug("AdminServlet.performSupplierConfig() - action=" + action);
		HttpSession session = req.getSession(true);
		if (action.equals(Util.ACTION_GETSUPPLIER)) {
			// Get supplier info
			try {
				supplier = suppliers.getSupplier();
			} catch (Exception e) {
				Util.debug("AdminServlet.performSupplierConfig() Exception: " + e);
			}
		} else if (action.equals(Util.ACTION_UPDATESUPPLIER)) {
			String supplierID = req.getParameter("supplierid");
			Util.debug("AdminServlet.performSupplierConfig() - supplierid = " + supplierID);
			if ((supplierID != null) && (!supplierID.equals(""))) {
				String name = req.getParameter("name");
				String street = req.getParameter("street");
				String city = req.getParameter("city");
				String state = req.getParameter("state");
				String zip = req.getParameter("zip");
				String phone = req.getParameter("phone");
				String location_url = req.getParameter("location_url");
				supplier = updateSupplierInfo(supplierID, name, street, city, state, zip, phone, location_url);
			}
		} else {
			// Unknown Supplier Config Admin Action so go back to the
			// Administration home page
			sendRedirect(resp, "/PlantsByWebSphere/" + Util.PAGE_ADMINHOME);
		}
		session.setAttribute(Util.ATTR_SUPPLIER, supplier);
		requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_SUPPLIERCFG);
	}

	/**
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	public void performPopulate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Populate popDB = new Populate(resetDB, catalog, login, backOrderStock, suppliers);
		popDB.doPopulate();
		sendRedirect(resp, "/PlantsByWebSphere/" + Util.PAGE_HELP);
	}

	/**
	 * Method performBackOrder.
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	public void performBackOrder(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		String action = null;
		action = req.getParameter(Util.ATTR_ACTION);
		if ((action == null) || (action.equals("")))
			action = Util.ACTION_GETBACKORDERS;
		Util.debug("AdminServlet.performBackOrder() - action=" + action);
		HttpSession session = req.getSession(true);
		if (action.equals(Util.ACTION_GETBACKORDERS)) {
			getBackOrders(session);
			requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_BACKADMIN);
		} else if (action.equals(Util.ACTION_UPDATESTOCK)) {
			Util.debug("AdminServlet.performBackOrder() - AdminServlet(performTask):  Update Stock Action");
			String[] backOrderIDs = (String[]) req.getParameterValues("selectedObjectIds");
			if (backOrderIDs != null) {
				for (int i = 0; i < backOrderIDs.length; i++) {
					String backOrderID = backOrderIDs[i];
					Util.debug("AdminServlet.performBackOrder() - Selected BackOrder backOrderID: " + backOrderID);
					try {
						String inventoryID = backOrderStock.getBackOrderInventoryID(backOrderID);
						Util.debug("AdminServlet.performBackOrder() - backOrderID = " + inventoryID);
						int quantity = backOrderStock.getBackOrderQuantity(backOrderID);
						catalog.setItemQuantity(inventoryID, quantity);
						// Update the BackOrder status
						Util.debug("AdminServlet.performBackOrder() - quantity: " + quantity);
						backOrderStock.updateStock(backOrderID, quantity);
					} catch (Exception e) {
						Util.debug("AdminServlet.performBackOrder() - Exception: " + e);
						e.printStackTrace();
					}
				}
			}
			getBackOrders(session);
			requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_BACKADMIN);
		} else if (action.equals(Util.ACTION_CANCEL)) {
			Util.debug("AdminServlet.performBackOrder() - AdminServlet(performTask):  Cancel Action");
			String[] backOrderIDs = (String[]) req.getParameterValues("selectedObjectIds");
			if (backOrderIDs != null) {
				for (int i = 0; i < backOrderIDs.length; i++) {
					String backOrderID = backOrderIDs[i];
					Util.debug("AdminServlet.performBackOrder() - Selected BackOrder backOrderID: " + backOrderID);
					try {
						backOrderStock.deleteBackOrder(backOrderID);
					} catch (Exception e) {
						Util.debug("AdminServlet.performBackOrder() - Exception: " + e);
						e.printStackTrace();
					}
				}
			}
			getBackOrders(session);
			requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_BACKADMIN);
		} else if (action.equals(Util.ACTION_UPDATEQUANTITY)) {
			Util.debug("AdminServlet.performBackOrder() -  Update Quantity Action");
			try {
				String backOrderID = req.getParameter("backOrderID");
				if (backOrderID != null) {
					Util.debug("AdminServlet.performBackOrder() - backOrderID = " + backOrderID);
					String paramquantity = req.getParameter("itemqty");
					if (paramquantity != null) {
						int quantity = new Integer(paramquantity).intValue();
						Util.debug("AdminServlet.performBackOrder() - quantity: " + quantity);
						backOrderStock.setBackOrderQuantity(backOrderID, quantity);
					}
				}
			} catch (Exception e) {
				Util.debug("AdminServlet.performBackOrder() - Exception: " + e);
				e.printStackTrace();
			}
			getBackOrders(session);
			requestDispatch(getServletConfig().getServletContext(), req, resp, Util.PAGE_BACKADMIN);
		} else {
			// Unknown Backup Admin Action so go back to the Administration home
			// page
			sendRedirect(resp, "/PlantsByWebSphere/" + Util.PAGE_ADMINHOME);
		}
	}

	/**
	 * Method getBackOrders.
	 * 
	 * @param session
	 */
	public void getBackOrders(HttpSession session) {
		try {
			// Get the list of back order items.
			Util.debug("AdminServlet.getBackOrders() - Looking for BackOrders");
			Collection<BackOrder> backOrders = backOrderStock.findBackOrders();
			ArrayList<BackOrderItem> backOrderItems = new ArrayList<BackOrderItem>();
			for (BackOrder bo : backOrders) {
				BackOrderItem boi = new BackOrderItem(bo);
				backOrderItems.add(boi);
			}
			Util.debug("AdminServlet.getBackOrders() - BackOrders found!");
			Iterator<BackOrderItem> i = backOrderItems.iterator();
			while (i.hasNext()) {
				BackOrderItem backOrderItem = (BackOrderItem) i.next();
				String backOrderID = backOrderItem.getBackOrderID();
				String inventoryID = backOrderItem.getInventory().getInventoryId();
				// Get the inventory quantity and name for the back order item
				// information.
				Inventory item = catalog.getItemInventory(inventoryID);
				int quantity = item.getQuantity();
				backOrderItem.setInventoryQuantity(quantity);
				String name = item.getName();
				backOrderItem.setName(name);
				// Don't include backorders that have been completed.
				if (!(backOrderItem.getStatus().equals(Util.STATUS_ADDEDSTOCK))) {
					String invID = backOrderItem.getInventory().getInventoryId();
					String supplierOrderID = backOrderItem.getSupplierOrderID();
					String status = backOrderItem.getStatus();
					String lowDate = new Long(backOrderItem.getLowDate()).toString();
					String orderDate = new Long(backOrderItem.getOrderDate()).toString();
					Util.debug("AdminServlet.getBackOrders() - backOrderID = " + backOrderID);
					Util.debug("AdminServlet.getBackOrders() -    supplierOrderID = " + supplierOrderID);
					Util.debug("AdminServlet.getBackOrders() -    invID = " + invID);
					Util.debug("AdminServlet.getBackOrders() -    name = " + name);
					Util.debug("AdminServlet.getBackOrders() -    quantity = " + quantity);
					Util.debug("AdminServlet.getBackOrders() -    status = " + status);
					Util.debug("AdminServlet.getBackOrders() -    lowDate = " + lowDate);
					Util.debug("AdminServlet.getBackOrders() -    orderDate = " + orderDate);
				}
			}
			session.setAttribute("backorderitems", backOrderItems);
		} catch (Exception e) {
			e.printStackTrace();
			Util.debug("AdminServlet.getBackOrders() - RemoteException: " + e);
		}
	}

	/**
	 * Method sendRedirect.
	 * 
	 * @param resp
	 * @param page
	 * @throws ServletException
	 * @throws IOException
	 */
	private void sendRedirect(HttpServletResponse resp, String page) throws ServletException, IOException {
		resp.sendRedirect(resp.encodeRedirectURL(page));
	}

	/**
	 * Method requestDispatch.
	 * 
	 * @param ctx
	 * @param req
	 * @param resp
	 * @param page
	 * @throws ServletException
	 * @throws IOException
	 */
	/**
	 * Request dispatch
	 */
	private void requestDispatch(ServletContext ctx,
			HttpServletRequest req,
			HttpServletResponse resp,
			String page) throws ServletException, IOException {
		resp.setContentType("text/html");
		ctx.getRequestDispatcher(page).forward(req, resp);
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.samples.pbw.bean.CatalogMgr;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Servlet to handle image actions.
 */
@Named(value = "image")
@WebServlet("/servlet/ImageServlet")
public class ImageServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Inject
	private CatalogMgr catalog;

	/**
	 * Servlet initialization.
	 */
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
	public void doGet(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
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
	public void doPost(javax.servlet.http.HttpServletRequest request,
			javax.servlet.http.HttpServletResponse response) throws ServletException, IOException {
		performTask(request, response);
	}

	/**
	 * Main service method for ImageServlet
	 *
	 * @param request
	 *            Object that encapsulates the request to the servlet
	 * @param response
	 *            Object that encapsulates the response from the servlet
	 */
	private void performTask(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String action = null;

		action = req.getParameter("action");
		Util.debug("action=" + action);

		if (action.equals("getimage")) {
			String inventoryID = req.getParameter("inventoryID");

			byte[] buf = catalog.getItemImageBytes(inventoryID);
			if (buf != null) {
				resp.setContentType("image/jpeg");
				resp.getOutputStream().write(buf);
			}
		}
	}
}
//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

/**
 * Simple helper class for JSF validators to handle error messages.
 *
 */
public class ValidatorUtils {
	protected static void addErrorMessage(FacesContext context, String message) {
		FacesMessage facesMessage = new FacesMessage();
		facesMessage.setDetail(message);
		facesMessage.setSummary(message);
		facesMessage.setSeverity(FacesMessage.SEVERITY_ERROR);
		throw new ValidatorException(facesMessage);
	}

	protected static void addErrorMessage(FacesContext context, UIComponent component) {
		String errorMessage = (String) component.getAttributes().get("errorMessage");

		addErrorMessage(context, errorMessage);
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.websphere.samples.pbw.bean.CustomerMgr;
import com.ibm.websphere.samples.pbw.bean.MailerAppException;
import com.ibm.websphere.samples.pbw.bean.MailerBean;
import com.ibm.websphere.samples.pbw.bean.ShoppingCartBean;
import com.ibm.websphere.samples.pbw.jpa.Customer;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Provides a combination of JSF action and backing bean support for the account web page.
 *
 */
@Named(value = "account")
@SessionScoped
public class AccountBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String ACTION_ACCOUNT = "account";
	private static final String ACTION_CHECKOUT_FINAL = "checkout_final";
	private static final String ACTION_LOGIN = "login";
	private static final String ACTION_ORDERDONE = "orderdone";
	private static final String ACTION_ORDERINFO = "orderinfo";
	private static final String ACTION_PROMO = "promo";
	private static final String ACTION_REGISTER = "register";

	@Inject
	private CustomerMgr login;
	@Inject
	private MailerBean mailer;
	@Inject
	private ShoppingCartBean shoppingCart;

	private boolean checkingOut;
	private Customer customer;
	private String lastOrderNum;
	private LoginInfo loginInfo;
	private Customer newCustomer;
	private OrderInfo orderInfo;
	private int orderNum = 1;
	private boolean register;
	private boolean updating;

	public String performAccount() {
		if (customer == null || loginInfo == null) {
			checkingOut = false;
			loginInfo = new LoginInfo();
			register = false;
			updating = true;

			loginInfo.setMessage("You must log in first.");

			return AccountBean.ACTION_LOGIN;
		}

		else {
			return AccountBean.ACTION_ACCOUNT;
		}
	}

	public String performAccountUpdate() {
		if (register) {
			customer = login.createCustomer(loginInfo.getEmail(), loginInfo.getPassword(), newCustomer
					.getFirstName(), newCustomer.getLastName(), newCustomer.getAddr1(), newCustomer
							.getAddr2(), newCustomer.getAddrCity(), newCustomer
									.getAddrState(), newCustomer.getAddrZip(), newCustomer.getPhone());
			register = false;
		}

		else {
			customer = login.updateUser(customer.getCustomerID(), customer.getFirstName(), customer
					.getLastName(), customer.getAddr1(), customer.getAddr2(), customer
							.getAddrCity(), customer.getAddrState(), customer.getAddrZip(), customer.getPhone());
		}

		return AccountBean.ACTION_PROMO;
	}

	public String performCheckoutFinal() {
		FacesContext context = FacesContext.getCurrentInstance();
		Application app = context.getApplication();
		ShoppingBean shopping = (ShoppingBean) app.createValueBinding("#{shopping}").getValue(context);

		shopping.setShippingCost(Util.getShippingMethodPrice(orderInfo.getShippingMethod()));

		return AccountBean.ACTION_CHECKOUT_FINAL;
	}

	public String performCompleteCheckout() {
		FacesContext context = FacesContext.getCurrentInstance();
		Application app = context.getApplication();
		app.createValueBinding("#{shopping}").getValue(context);

		// persist the order
		OrderInfo oi = new OrderInfo(shoppingCart
				.createOrder(customer.getCustomerID(), orderInfo.getBillName(), orderInfo.getBillAddr1(), orderInfo
						.getBillAddr2(), orderInfo.getBillCity(), orderInfo.getBillState(), orderInfo
								.getBillZip(), orderInfo.getBillPhone(), orderInfo.getShipName(), orderInfo
										.getShipAddr1(), orderInfo.getShipAddr2(), orderInfo.getShipCity(), orderInfo
												.getShipState(), orderInfo.getShipZip(), orderInfo
														.getShipPhone(), orderInfo.getCardName(), orderInfo
																.getCardNum(), orderInfo.getCardExpMonth(), orderInfo
																		.getCardExpYear(), orderInfo
																				.getCardholderName(), orderInfo
																						.getShippingMethod(), shoppingCart
																								.getItems()));

		lastOrderNum = oi.getID();

		Util.debug("Account.performCompleteCheckout: order id =" + orderInfo);

		/*
		 * // Check the available inventory and backorder if necessary. if (shoppingCart != null) {
		 * Inventory si; Collection<Inventory> items = shoppingCart.getItems(); for (Object o :
		 * items) { si = (Inventory) o; shoppingCart.checkInventory(si); Util.debug(
		 * "ShoppingCart.checkInventory() - checking Inventory quantity of item: " + si.getID()); }
		 * }
		 */
		try {
			mailer.createAndSendMail(customer, oi.getID());
		} catch (MailerAppException e) {
			System.out.println("MailerAppException:" + e);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Exception during create and send mail :" + e);
			e.printStackTrace();
		}

		orderInfo = null;

		// shoppingCart.setCartContents (new ShoppingCartContents());
		shoppingCart.removeAllItems();

		return AccountBean.ACTION_ORDERDONE;
	}

	public String performLogin() {
		checkingOut = false;
		loginInfo = new LoginInfo();
		register = false;
		updating = false;

		loginInfo.setMessage("");

		return AccountBean.ACTION_LOGIN;
	}

	public String performLoginComplete() {
		String message;

		// Attempt to log in the user.

		message = login.verifyUserAndPassword(loginInfo.getEmail(), loginInfo.getPassword());

		if (message != null) {
			// Error, so go back to the login page.

			loginInfo.setMessage(message);

			return AccountBean.ACTION_LOGIN;
		}

		// Otherwise, no error, so continue to the correct page.

		customer = login.getCustomer(loginInfo.getEmail());

		if (isCheckingOut()) {
			return performOrderInfo();
		}

		if (isUpdating()) {
			return performAccount();
		}

		return AccountBean.ACTION_PROMO;
	}

	public String performOrderInfo() {
		if (customer == null) {
			checkingOut = true;
			loginInfo = new LoginInfo();
			register = false;
			updating = false;

			loginInfo.setMessage("You must log in first.");

			return AccountBean.ACTION_LOGIN;
		}

		else {
			if (orderInfo == null) {
				orderInfo = new OrderInfo(customer.getFirstName() + " " + customer.getLastName(), customer.getAddr1(),
						customer.getAddr2(), customer.getAddrCity(), customer.getAddrState(), customer.getAddrZip(),
						customer.getPhone(), "", "", "", "", "", "", "", 0, "" + (orderNum++));
			}

			return AccountBean.ACTION_ORDERINFO;
		}
	}

	public String performRegister() {
		loginInfo = new LoginInfo();
		newCustomer = new Customer("", "", "", "", "", "", "", "", "", "");
		register = true;
		updating = false;

		return AccountBean.ACTION_REGISTER;
	}

	public Customer getCustomer() {
		return (isRegister() ? newCustomer : customer);
	}

	public String getLastOrderNum() {
		return lastOrderNum;
	}

	public LoginInfo getLoginInfo() {
		return loginInfo;
	}

	public OrderInfo getOrderInfo() {
		return orderInfo;
	}

	public boolean isCheckingOut() {
		return checkingOut;
	}

	public boolean isRegister() {
		return register;
	}

	public boolean isUpdating() {
		return updating;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * A JSF backing bean used to store information for the login web page. It is accessed via the
 * account bean.
 *
 */
public class LoginInfo {
	private String checkPassword;

	@Pattern(regexp = "[a-zA-Z0-9_-]+@[a-zA-Z0-9.-]+")
	private String email;
	private String message;

	@Size(min = 6, max = 10, message = "Password must be between 6 and 10 characters.")
	private String password;

	public LoginInfo() {
	}

	public String getCheckPassword() {
		return this.checkPassword;
	}

	public String getEmail() {
		return this.email;
	}

	public String getMessage() {
		return this.message;
	}

	public String getPassword() {
		return this.password;
	}

	public void setCheckPassword(String checkPassword) {
		this.checkPassword = checkPassword;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;
import java.util.Collection;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.ibm.websphere.samples.pbw.jpa.BackOrder;
import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * The BackOrderMgr provides a transactional and secured facade to access back order information.
 * This bean no longer requires an interface as there is one and only one implementation.
 */
@Dependent
@RolesAllowed("SampAdmin")
public class BackOrderMgr implements Serializable {
	@PersistenceContext(unitName = "PBW")
	private EntityManager em;

	/**
	 * Method createBackOrder.
	 * 
	 * @param inventoryID
	 * @param amountToOrder
	 * @param maximumItems
	 */
	public void createBackOrder(String inventoryID, int amountToOrder, int maximumItems) {
		try {
			Util.debug("BackOrderMgr.createBackOrder() - Entered");
			BackOrder backOrder = null;
			try {
				// See if there is already an existing backorder and increase
				// the order quantity
				// but only if it has not been sent to the supplier.
				Query q = em.createNamedQuery("findByInventoryID");
				q.setParameter("id", inventoryID);
				backOrder = (BackOrder) q.getSingleResult();
				if (!(backOrder.getStatus().equals(Util.STATUS_ORDERSTOCK))) {
					Util.debug("BackOrderMgr.createBackOrder() - Backorders found but have already been ordered from the supplier");
					// throw new FinderException();
				}
				// Increase the BackOrder quantity for an existing Back Order.
				backOrder.setQuantity(backOrder.getQuantity() + amountToOrder);
			} catch (NoResultException e) {
				Util.debug("BackOrderMgr.createBackOrder() - BackOrder doesn't exist." + e);
				Util.debug("BackOrderMgr.createBackOrder() - Creating BackOrder for InventoryID: " + inventoryID);
				// Order enough stock from the supplier to reach the maximum
				// threshold and to
				// satisfy the back order.
				amountToOrder = maximumItems + amountToOrder;
				Inventory inv = em.find(Inventory.class, inventoryID);
				BackOrder b = new BackOrder(inv, amountToOrder);
				em.persist(b);
			}
		} catch (Exception e) {
			Util.debug("BackOrderMgr.createBackOrder() - Exception: " + e);
		}
	}

	/**
	 * Method findBackOrderItems.
	 * 
	 * @return Collection
	 */
	@SuppressWarnings("unchecked")
	public Collection<BackOrder> findBackOrders() {
		Query q = em.createNamedQuery("findAllBackOrders");
		return q.getResultList();
	}

	/**
	 * Method deleteBackOrder.
	 * 
	 * @param backOrderID
	 */
	public void deleteBackOrder(String backOrderID) {
		Util.debug("BackOrderMgr.deleteBackOrder() - Entered");
		// BackOrderLocal backOrder =
		// getBackOrderLocalHome().findByPrimaryKeyUpdate(backOrderID);
		BackOrder backOrder = em.find(BackOrder.class, backOrderID);
		em.remove(backOrder);
	}

	/**
	 * Method receiveConfirmation.
	 * 
	 * @param backOrderID
	 *            / public int receiveConfirmation(String backOrderID) { int rc = 0; BackOrder
	 *            backOrder; Util.debug(
	 *            "BackOrderMgr.receiveConfirmation() - Finding Back Order for backOrderID=" +
	 *            backOrderID); backOrder = em.find(BackOrder.class, backOrderID);
	 *            backOrder.setStatus(Util.STATUS_RECEIVEDSTOCK); Util.debug(
	 *            "BackOrderMgr.receiveConfirmation() - Updating status(" +
	 *            Util.STATUS_RECEIVEDSTOCK + ") of backOrderID(" + backOrderID + ")"); return (rc);
	 *            }
	 */

	/**
	 * Method orderStock.
	 * 
	 * @param backOrderID
	 * @param quantity
	 *            / public void orderStock(String backOrderID, int quantity) {
	 *            this.setBackOrderStatus(backOrderID, Util.STATUS_ORDEREDSTOCK);
	 *            this.setBackOrderQuantity(backOrderID, quantity);
	 *            this.setBackOrderOrderDate(backOrderID); }
	 */

	/**
	 * Method updateStock.
	 * 
	 * @param backOrderID
	 * @param quantity
	 */
	public void updateStock(String backOrderID, int quantity) {
		this.setBackOrderStatus(backOrderID, Util.STATUS_ADDEDSTOCK);
	}

	/**
	 * @param backOrderID
	 *            / public void abortorderStock(String backOrderID) { Util.debug(
	 *            "backOrderStockBean.abortorderStock() - Aborting orderStock transation for backorderID: "
	 *            + backOrderID); // Reset the back order status since the order failed.
	 *            this.setBackOrderStatus(backOrderID, Util.STATUS_ORDERSTOCK); }
	 */

	/**
	 * Method getBackOrderID.
	 * 
	 * @param backOrderID
	 * @return String / public String getBackOrderID(String backOrderID) { String retbackOrderID =
	 *         ""; Util.debug( "BackOrderMgr.getBackOrderID() - Entered"); // BackOrderLocal
	 *         backOrder = getBackOrderLocalHome().findByPrimaryKey(new BackOrderKey(backOrderID));
	 *         BackOrder backOrder = em.find(BackOrder.class, backOrderID); retbackOrderID =
	 *         backOrder.getBackOrderID(); return retbackOrderID; }
	 */

	/**
	 * Method getBackOrderInventoryID.
	 * 
	 * @param backOrderID
	 * @return String
	 */
	public String getBackOrderInventoryID(String backOrderID) {
		String retinventoryID = "";

		Util.debug("BackOrderMgr.getBackOrderID() - Entered");
		// BackOrderLocal backOrder =
		// getBackOrderLocalHome().findByPrimaryKey(new
		// BackOrderKey(backOrderID));
		BackOrder backOrder = em.find(BackOrder.class, backOrderID);
		retinventoryID = backOrder.getInventory().getInventoryId();

		return retinventoryID;
	}

	/**
	 * Method getBackOrderQuantity.
	 * 
	 * @param backOrderID
	 * @return int
	 */
	public int getBackOrderQuantity(String backOrderID) {
		int backOrderQuantity = -1;
		Util.debug("BackOrderMgr.getBackOrderQuantity() - Entered");
		// BackOrderLocal backOrder =
		// getBackOrderLocalHome().findByPrimaryKey(new
		// BackOrderKey(backOrderID));
		BackOrder backOrder = em.find(BackOrder.class, backOrderID);
		backOrderQuantity = backOrder.getQuantity();
		return backOrderQuantity;
	}

	/**
	 * Method setBackOrderQuantity.
	 * 
	 * @param backOrderID
	 * @param quantity
	 */
	public void setBackOrderQuantity(String backOrderID, int quantity) {
		Util.debug("BackOrderMgr.setBackOrderQuantity() - Entered");
		// BackOrderLocal backOrder =
		// getBackOrderLocalHome().findByPrimaryKeyUpdate(backOrderID);
		BackOrder backOrder = em.find(BackOrder.class, backOrderID);
		backOrder.setQuantity(quantity);
	}

	/**
	 * Method setBackOrderStatus.
	 * 
	 * @param backOrderID
	 * @param Status
	 */
	public void setBackOrderStatus(String backOrderID, String Status) {
		Util.debug("BackOrderMgr.setBackOrderStatus() - Entered");
		// BackOrderLocal backOrder =
		// getBackOrderLocalHome().findByPrimaryKeyUpdate(backOrderID);
		BackOrder backOrder = em.find(BackOrder.class, backOrderID);
		backOrder.setStatus(Status);
	}

	/**
	 * Method setBackOrderOrderDate.
	 * 
	 * @param backOrderID
	 */
	public void setBackOrderOrderDate(String backOrderID) {
		Util.debug("BackOrderMgr.setBackOrderQuantity() - Entered");
		// BackOrderLocal backOrder =
		// getBackOrderLocalHome().findByPrimaryKeyUpdate(backOrderID);
		BackOrder backOrder = em.find(BackOrder.class, backOrderID);
		backOrder.setOrderDate(System.currentTimeMillis());
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.ibm.websphere.samples.pbw.jpa.Customer;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * The CustomerMgr provides a transactional facade for access to a user DB as well as simple
 * authentication support for those users.
 * 
 */
@Transactional
@Dependent
public class CustomerMgr implements Serializable {
	@PersistenceContext(unitName = "PBW")
	EntityManager em;

	/**
	 * Create a new user.
	 *
	 * @param customerID
	 *            The new customer ID.
	 * @param password
	 *            The password for the customer ID.
	 * @param firstName
	 *            First name.
	 * @param lastName
	 *            Last name.
	 * @param addr1
	 *            Address line 1.
	 * @param addr2
	 *            Address line 2.
	 * @param addrCity
	 *            City address information.
	 * @param addrState
	 *            State address information.
	 * @param addrZip
	 *            Zip code address information.
	 * @param phone
	 *            User's phone number.
	 * @return Customer
	 */
	public Customer createCustomer(String customerID,
			String password,
			String firstName,
			String lastName,
			String addr1,
			String addr2,
			String addrCity,
			String addrState,
			String addrZip,
			String phone) {
		Customer c = new Customer(customerID, password, firstName, lastName, addr1, addr2, addrCity, addrState, addrZip,
				phone);
		em.persist(c);
		em.flush();
		return c;
	}

	/**
	 * Retrieve an existing user.
	 * 
	 * @param customerID
	 *            The customer ID.
	 * @return Customer
	 */
	public Customer getCustomer(String customerID) {
		Customer c = em.find(Customer.class, customerID);
		return c;

	}

	/**
	 * Update an existing user.
	 *
	 * @param customerID
	 *            The customer ID.
	 * @param firstName
	 *            First name.
	 * @param lastName
	 *            Last name.
	 * @param addr1
	 *            Address line 1.
	 * @param addr2
	 *            Address line 2.
	 * @param addrCity
	 *            City address information.
	 * @param addrState
	 *            State address information.
	 * @param addrZip
	 *            Zip code address information.
	 * @param phone
	 *            User's phone number.
	 * @return Customer
	 */
	public Customer updateUser(String customerID,
			String firstName,
			String lastName,
			String addr1,
			String addr2,
			String addrCity,
			String addrState,
			String addrZip,
			String phone) {
		Customer c = em.find(Customer.class, customerID);
		em.lock(c, LockModeType.WRITE);
		em.refresh(c);

		c.setFirstName(firstName);
		c.setLastName(lastName);
		c.setAddr1(addr1);
		c.setAddr2(addr2);
		c.setAddrCity(addrCity);
		c.setAddrState(addrState);
		c.setAddrZip(addrZip);
		c.setPhone(phone);

		return c;
	}

	/**
	 * Verify that the user exists and the password is value.
	 * 
	 * @param customerID
	 *            The customer ID
	 * @param password
	 *            The password for the customer ID
	 * @return String with a results message.
	 */
	public String verifyUserAndPassword(String customerID, String password) {
		// Try to get customer.
		String results = null;
		Customer customer = null;

		customer = em.find(Customer.class, customerID);

		// Does customer exist?
		if (customer != null) {
			if (!customer.verifyPassword(password)) // Is password correct?
			{
				results = "\nPassword does not match for : " + customerID;
				Util.debug("Password given does not match for userid=" + customerID);
			}
		} else // Customer was not found.
		{
			results = "\nCould not find account for : " + customerID;
			Util.debug("customer " + customerID + " NOT found");
		}

		return results;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2003,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//


public class NoSupplierException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Method NoSupplierException
	 * 
	 * @param message
	 */
	public NoSupplierException(String message) {
		super(message);
		return;
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;
import java.util.Vector;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * The CatalogMgr provides transactional access to the catalog of items the store is willing to sell
 * to customers.
 * 
 * @see com.ibm.websphere.samples.pbw.jpa.Inventory
 */
@Dependent
@SuppressWarnings("unchecked")
public class CatalogMgr implements Serializable {
	@PersistenceContext(unitName = "PBW")
	EntityManager em;

	/**
	 * Get all inventory items.
	 *
	 * @return Vector of Inventorys. / public Vector<Inventory> getItems() { Vector<Inventory> items
	 *         = new Vector<Inventory>(); int count = Util.getCategoryStrings().length; for (int i =
	 *         0; i < count; i++) { items.addAll(getItemsByCategory(i)); } return items; }
	 */

	/**
	 * Get all inventory items for the given category.
	 *
	 * @param category
	 *            of items desired.
	 * @return Vector of Inventory.
	 */
	public Vector<Inventory> getItemsByCategory(int category) {
		Query q = em.createNamedQuery("getItemsByCategory");
		q.setParameter("category", category);
		// The return type must be Vector because the PBW client ActiveX sample requires Vector
		return new Vector<Inventory>(q.getResultList());
	}

	/**
	 * Get inventory items that contain a given String within their names.
	 *
	 * @param name
	 *            String to search names for.
	 * @return A Vector of Inventorys that match. / public Vector<Inventory> getItemsLikeName(String
	 *         name) { Query q = em.createNamedQuery("getItemsLikeName"); q.setParameter("name", '%'
	 *         + name + '%'); //The return type must be Vector because the PBW client ActiveX sample
	 *         requires Vector return new Vector<Inventory>(q.getResultList()); }
	 */

	/**
	 * Get the StoreItem for the given ID.
	 *
	 * @param inventoryID
	 *            - ID of the Inventory item desired.
	 * @return StoreItem / public StoreItem getItem(String inventoryID) { return new
	 *         StoreItem(getItemInventory(inventoryID)); }
	 */

	/**
	 * Get the Inventory item for the given ID.
	 *
	 * @param inventoryID
	 *            - ID of the Inventory item desired.
	 * @return Inventory
	 */
	public Inventory getItemInventory(String inventoryID) {
		Inventory si = null;
		Util.debug("getItemInventory id=" + inventoryID);
		si = em.find(Inventory.class, inventoryID);
		return si;
	}

	/**
	 * Add an inventory item.
	 *
	 * @param item
	 *            The Inventory to add.
	 * @return True, if item added.
	 */
	public boolean addItem(Inventory item) {
		boolean retval = true;
		Util.debug("addItem " + item.getInventoryId());
		em.persist(item);
		em.flush();
		return retval;
	}

	/**
	 * Add an StoreItem item (same as Inventory item).
	 *
	 * @param item
	 *            The StoreItem to add.
	 * @return True, if item added. / public boolean addItem(StoreItem item) { return addItem(new
	 *         Inventory(item)); }
	 */

	/**
	 * Delete an inventory item.
	 *
	 * @param inventoryID
	 *            The ID of the inventory item to delete.
	 * @return True, if item deleted. / public boolean deleteItem(String inventoryID) { boolean
	 *         retval = true; em.remove(em.find(Inventory.class, inventoryID)); return retval; }
	 */

	/**
	 * Get the image for the inventory item.
	 * 
	 * @param inventoryID
	 *            The id of the inventory item wanted.
	 * @return Buffer containing the image.
	 */
	public byte[] getItemImageBytes(String inventoryID) {
		byte[] retval = null;
		Inventory inv = getInv(inventoryID);
		if (inv != null) {
			retval = inv.getImgbytes();
		}

		return retval;
	}

	/**
	 * Set the image for the inventory item.
	 * 
	 * @param inventoryID
	 *            The id of the inventory item wanted.
	 * @param imgbytes
	 *            Buffer containing the image.
	 */
	public void setItemImageBytes(String inventoryID, byte[] imgbytes) {
		Inventory inv = getInvUpdate(inventoryID);
		if (inv != null) {
			inv.setImgbytes(imgbytes);
		}
	}

	/**
	 * Set the inventory item's quantity.
	 *
	 * @param inventoryID
	 *            The inventory item's ID.
	 * @param quantity
	 *            The inventory item's new quantity.
	 */
	public void setItemQuantity(String inventoryID, int quantity) {
		Inventory inv = getInvUpdate(inventoryID);
		if (inv != null) {
			inv.setQuantity(quantity);
		}
	}

	/**
	 * Get a remote Inventory object.
	 *
	 * @param inventoryID
	 *            The id of the inventory item wanted.
	 * @return Reference to the remote Inventory object.
	 */
	private Inventory getInv(String inventoryID) {
		return em.find(Inventory.class, inventoryID);
	}

	/**
	 * Get a remote Inventory object to Update.
	 *
	 * @param inventoryID
	 *            The id of the inventory item wanted.
	 * @return Reference to the remote Inventory object.
	 */
	private Inventory getInvUpdate(String inventoryID) {
		Inventory inv = null;
		inv = em.find(Inventory.class, inventoryID);
		em.lock(inv, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
		em.refresh(inv);
		return inv;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

/**
 * This class encapsulates the info needed to send an email message. This object is passed to the
 * Mailer EJB sendMail() method.
 */
public class EMailMessage implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String subject;
	private String htmlContents;
	private String emailReceiver;

	public EMailMessage(String subject, String htmlContents, String emailReceiver) {
		this.subject = subject;
		this.htmlContents = htmlContents;
		this.emailReceiver = emailReceiver;
	}

	// subject field of email message
	public String getSubject() {
		return subject;
	}

	// Email address of recipient of email message
	public String getEmailReceiver() {
		return emailReceiver;
	}

	// contents of email message
	public String getHtmlContents() {
		return htmlContents;
	}

	public String toString() {
		return " subject=" + subject + " " + emailReceiver + " " + htmlContents;
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;
import java.util.Date;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.ibm.websphere.samples.pbw.jpa.Customer;
import com.ibm.websphere.samples.pbw.jpa.Order;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * MailerBean provides a transactional facade for access to Order information and notification of
 * the buyer of order state.
 * 
 */

@Named(value = "mailerbean")
@Dependent

public class MailerBean implements Serializable {
	private static final long serialVersionUID = 1L;
	// public static final String MAIL_SESSION = "java:comp/env/mail/PlantsByWebSphere";
	@Resource(name = "mail/PlantsByWebSphere")
	Session mailSession;

	@PersistenceContext(unitName = "PBW")

	EntityManager em;

	/**
	 * Create the email message.
	 *
	 * @param orderKey
	 *            The order number.
	 * @return The email message.
	 */
	private String createMessage(String orderKey) {
		Util.debug("creating email message for order:" + orderKey);
		StringBuffer msg = new StringBuffer();
		Order order = em.find(Order.class, orderKey);
		msg.append("Thank you for your order " + orderKey + ".\n");
		msg.append("Your Plants By WebSphere order will be shipped to:\n");
		msg.append("     " + order.getShipName() + "\n");
		msg.append("     " + order.getShipAddr1() + " " + order.getShipAddr2() + "\n");
		msg.append("     " + order.getShipCity() + ", " + order.getShipState() + " " + order.getShipZip() + "\n\n");
		msg.append("Please save it for your records.\n");
		return msg.toString();
	}

	/**
	 * Create the Subject line.
	 *
	 * @param orderKey
	 *            The order number.
	 * @return The Order number string.
	 */
	private String createSubjectLine(String orderKey) {
		StringBuffer msg = new StringBuffer();
		msg.append("Your order number " + orderKey);

		return msg.toString();
	}

	/**
	 * Create a mail message and send it.
	 *
	 * @param customerInfo
	 *            Customer information.
	 * @param orderKey
	 * @throws MailerAppException
	 */
	public void createAndSendMail(Customer customerInfo, String orderKey) throws MailerAppException {
		try {
			EMailMessage eMessage = new EMailMessage(createSubjectLine(orderKey), createMessage(orderKey),
					customerInfo.getCustomerID());

			Util.debug("Sending message" + "\nTo: " + eMessage.getEmailReceiver() + "\nSubject: "
					+ eMessage.getSubject() + "\nContents: " + eMessage.getHtmlContents());

			Util.debug("Sending message" + "\nTo: " + eMessage.getEmailReceiver() + "\nSubject: "
					+ eMessage.getSubject() + "\nContents: " + eMessage.getHtmlContents());

			MimeMessage msg = new MimeMessage(mailSession);
			msg.setFrom();

			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(eMessage.getEmailReceiver(), false));

			msg.setSubject(eMessage.getSubject());
			MimeBodyPart mbp = new MimeBodyPart();
			mbp.setText(eMessage.getHtmlContents(), "us-ascii");
			msg.setHeader("X-Mailer", "JavaMailer");
			Multipart mp = new MimeMultipart();
			mp.addBodyPart(mbp);
			msg.setContent(mp);
			msg.setSentDate(new Date());

			Transport.send(msg);
			Util.debug("Mail sent successfully.");

		} catch (Exception e) {

			Util.debug("Error sending mail. Have mail resources been configured correctly?");
			Util.debug("createAndSendMail exception : " + e);
			e.printStackTrace();
			throw new MailerAppException("Failure while sending mail");
		}
	}
}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.util.Enumeration;
import java.util.Hashtable;

import com.ibm.websphere.samples.pbw.jpa.Inventory;

/**
 * A class to hold a shopping cart's contents.
 */
public class ShoppingCartContent implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Hashtable<String, Integer> table = null;

	public ShoppingCartContent() {
		table = new Hashtable<String, Integer>();
	}

	/** Add the item to the shopping cart. */
	public void addItem(Inventory si) {
		table.put(si.getID(), new Integer(si.getQuantity()));
	}

	/** Update the item in the shopping cart. */
	public void updateItem(Inventory si) {
		table.put(si.getID(), new Integer(si.getQuantity()));
	}

	/** Remove the item from the shopping cart. */
	public void removeItem(Inventory si) {
		table.remove(si.getID());
	}

	/**
	 * Return the number of items in the cart.
	 *
	 * @return The number of items in the cart.
	 */
	public int size() {
		return table.size();
	}

	/**
	 * Return the inventory ID at the index given. The first element is at index 0, the second at
	 * index 1, and so on.
	 *
	 * @return The inventory ID at the index, or NULL if not present.
	 */
	public String getInventoryID(int index) {
		String retval = null;
		String inventoryID;
		int cnt = 0;
		for (Enumeration<String> myEnum = table.keys(); myEnum.hasMoreElements(); cnt++) {
			inventoryID = (String) myEnum.nextElement();
			if (index == cnt) {
				retval = inventoryID;
				break;
			}
		}
		return retval;
	}

	/**
	 * Return the quantity for the inventory ID given.
	 *
	 * @return The quantity for the inventory ID given..
	 *
	 */
	public int getQuantity(String inventoryID) {
		Integer quantity = (Integer) table.get(inventoryID);

		if (quantity == null)
			return 0;
		else
			return quantity.intValue();
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2004,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import javax.enterprise.context.Dependent;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.ibm.websphere.samples.pbw.jpa.Supplier;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * Bean implementation class for Enterprise Bean: Suppliers
 */
@Dependent
public class SuppliersBean implements Serializable {

	@PersistenceContext(unitName = "PBW")
	EntityManager em;

	/**
	 * @param supplierID
	 * @param name
	 * @param street
	 * @param city
	 * @param state
	 * @param zip
	 * @param phone
	 * @param url
	 */
	public void createSupplier(String supplierID,
			String name,
			String street,
			String city,
			String state,
			String zip,
			String phone,
			String url) {
		try {
			Util.debug("SuppliersBean.createSupplier() - Entered");
			Supplier supplier = null;
			supplier = em.find(Supplier.class, supplierID);
			if (supplier == null) {
				Util.debug("SuppliersBean.createSupplier() - supplier doesn't exist.");
				Util.debug("SuppliersBean.createSupplier() - Creating Supplier for SupplierID: " + supplierID);
				supplier = new Supplier(supplierID, name, street, city, state, zip, phone, url);
				em.persist(supplier);
			}
		} catch (Exception e) {
			Util.debug("SuppliersBean.createSupplier() - Exception: " + e);
		}
	}

	/**
	 * @return Supplier
	 */
	public Supplier getSupplier() {
		// Retrieve the first Supplier Info
		try {
			Collection<Supplier> suppliers = this.findSuppliers();
			if (suppliers != null) {
				Util.debug("AdminServlet.getSupplierInfo() - Supplier found!");
				Iterator<Supplier> i = suppliers.iterator();
				if (i.hasNext()) {
					return (Supplier) i.next();
				}
			}
		} catch (Exception e) {
			Util.debug("AdminServlet.getSupplierInfo() - Exception:" + e);
		}
		return null;
	}

	/**
	 * @param supplierID
	 * @param name
	 * @param street
	 * @param city
	 * @param state
	 * @param zip
	 * @param phone
	 * @param url
	 * @return supplierInfo
	 */
	public Supplier updateSupplier(String supplierID,
			String name,
			String street,
			String city,
			String state,
			String zip,
			String phone,
			String url) {
		Supplier supplier = null;
		try {
			Util.debug("SuppliersBean.updateSupplier() - Entered");
			supplier = em.find(Supplier.class, supplierID);
			if (supplier != null) {
				// Create a new Supplier if there is NOT an existing Supplier.
				// supplier = getSupplierLocalHome().findByPrimaryKey(new SupplierKey(supplierID));
				supplier.setName(name);
				supplier.setStreet(street);
				supplier.setCity(city);
				supplier.setUsstate(state);
				supplier.setZip(zip);
				supplier.setPhone(phone);
				supplier.setUrl(url);
			} else {
				Util.debug("SuppliersBean.updateSupplier() - supplier doesn't exist.");
				Util.debug("SuppliersBean.updateSupplier() - Couldn't update Supplier for SupplierID: " + supplierID);
			}
		} catch (Exception e) {
			Util.debug("SuppliersBean.createSupplier() - Exception: " + e);
		}
		return (supplier);
	}

	/**
	 * @return suppliers
	 */
	@SuppressWarnings("unchecked")
	private Collection<Supplier> findSuppliers() {
		Query q = em.createNamedQuery("findAllSuppliers");
		return q.getResultList();
	}
}


import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.ibm.websphere.samples.pbw.utils.Util;

@Singleton
@Startup
public class PopulateDBBean {
    
    @Inject
    ResetDBBean dbBean;
    
    @PostConstruct
    public void initDB() {
        Util.debug("Initializing database...");
        dbBean.populateDB();
    }

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2004,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Vector;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;

import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * ResetDBBean provides a transactional and secure facade to reset all the database information for
 * the PlantsByWebSphere application.
 */

@Named(value = "resetbean")
@Dependent
@RolesAllowed("SampAdmin")
public class ResetDBBean implements Serializable {

	@Inject
	private CatalogMgr catalog;
	@Inject
	private CustomerMgr customer;
	@Inject
	private ShoppingCartBean cart;
	@Inject
	private BackOrderMgr backOrderStock;
	@Inject
	private SuppliersBean suppliers;

	@PersistenceContext(unitName = "PBW")
	EntityManager em;
	
	@Resource
	UserTransaction tx;
	
	public void resetDB() {
		deleteAll();
		populateDB();
	}

	/**
	 * @param itemID
	 * @param fileName
	 * @param catalog
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void addImage(String itemID,
			String fileName,
			CatalogMgr catalog) throws FileNotFoundException, IOException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("resources/images/" + fileName);
		Util.debug("URL: " + url);
		fileName = url.getPath();
		Util.debug("Fully-qualified Filename: " + fileName);
		File imgFile = new File(fileName);
		// Open the input file as a stream of bytes
		FileInputStream fis = new FileInputStream(imgFile);
		DataInputStream dis = new DataInputStream(fis);
		int dataSize = dis.available();
		byte[] data = new byte[dataSize];
		dis.readFully(data);
		catalog.setItemImageBytes(itemID, data);
	}

	public void populateDB() {
		/**
		 * Populate INVENTORY table with text
		 */
		Util.debug("Populating INVENTORY table with text...");
		try {
			String[] values = Util.getProperties("inventory");
			for (int index = 0; index < values.length; index++) {
				Util.debug("Found INVENTORY property values:  " + values[index]);
				String[] fields = Util.readTokens(values[index], "|");
				String id = fields[0];
				String name = fields[1];
				String heading = fields[2];
				String descr = fields[3];
				String pkginfo = fields[4];
				String image = fields[5];
				float price = new Float(fields[6]).floatValue();
				float cost = new Float(fields[7]).floatValue();
				int quantity = new Integer(fields[8]).intValue();
				int category = new Integer(fields[9]).intValue();
				String notes = fields[10];
				boolean isPublic = new Boolean(fields[11]).booleanValue();
				Util.debug("Populating INVENTORY with following values:  ");
				Util.debug(fields[0]);
				Util.debug(fields[1]);
				Util.debug(fields[2]);
				Util.debug(fields[3]);
				Util.debug(fields[4]);
				Util.debug(fields[5]);
				Util.debug(fields[6]);
				Util.debug(fields[7]);
				Util.debug(fields[8]);
				Util.debug(fields[9]);
				Util.debug(fields[10]);
				Util.debug(fields[11]);
				Inventory storeItem = new Inventory(id, name, heading, descr, pkginfo, image, price, cost, quantity,
						category, notes, isPublic);
				catalog.addItem(storeItem);
				addImage(id, image, catalog);
			}
			Util.debug("INVENTORY table populated with text...");
		} catch (Exception e) {
			Util.debug("Unable to populate INVENTORY table with text data: " + e);
			e.printStackTrace();
		}
		/**
		 * Populate CUSTOMER table with text
		 */
		Util.debug("Populating CUSTOMER table with default values...");
		try {
			String[] values = Util.getProperties("customer");
			Util.debug("Found CUSTOMER properties:  " + values[0]);
			for (int index = 0; index < values.length; index++) {
				String[] fields = Util.readTokens(values[index], "|");
				String customerID = fields[0];
				String password = fields[1];
				String firstName = fields[2];
				String lastName = fields[3];
				String addr1 = fields[4];
				String addr2 = fields[5];
				String addrCity = fields[6];
				String addrState = fields[7];
				String addrZip = fields[8];
				String phone = fields[9];
				Util.debug("Populating CUSTOMER with following values:  ");
				Util.debug(fields[0]);
				Util.debug(fields[1]);
				Util.debug(fields[2]);
				Util.debug(fields[3]);
				Util.debug(fields[4]);
				Util.debug(fields[5]);
				Util.debug(fields[6]);
				Util.debug(fields[7]);
				Util.debug(fields[8]);
				Util.debug(fields[9]);
				customer.createCustomer(customerID, password, firstName, lastName, addr1, addr2, addrCity, addrState, addrZip, phone);
			}
		} catch (Exception e) {
			Util.debug("Unable to populate CUSTOMER table with text data: " + e);
			e.printStackTrace();
		}
		/**
		 * Populate ORDER table with text
		 */
		Util.debug("Populating ORDER table with default values...");
		try {
			String[] values = Util.getProperties("order");
			Util.debug("Found ORDER properties:  " + values[0]);
			if (values[0] != null && values.length > 0) {
				for (int index = 0; index < values.length; index++) {
					String[] fields = Util.readTokens(values[index], "|");
					if (fields != null && fields.length >= 21) {
						String customerID = fields[0];
						String billName = fields[1];
						String billAddr1 = fields[2];
						String billAddr2 = fields[3];
						String billCity = fields[4];
						String billState = fields[5];
						String billZip = fields[6];
						String billPhone = fields[7];
						String shipName = fields[8];
						String shipAddr1 = fields[9];
						String shipAddr2 = fields[10];
						String shipCity = fields[11];
						String shipState = fields[12];
						String shipZip = fields[13];
						String shipPhone = fields[14];
						int shippingMethod = Integer.parseInt(fields[15]);
						String creditCard = fields[16];
						String ccNum = fields[17];
						String ccExpireMonth = fields[18];
						String ccExpireYear = fields[19];
						String cardHolder = fields[20];
						Vector<Inventory> items = new Vector<Inventory>();
						Util.debug("Populating ORDER with following values:  ");
						Util.debug(fields[0]);
						Util.debug(fields[1]);
						Util.debug(fields[2]);
						Util.debug(fields[3]);
						Util.debug(fields[4]);
						Util.debug(fields[5]);
						Util.debug(fields[6]);
						Util.debug(fields[7]);
						Util.debug(fields[8]);
						Util.debug(fields[9]);
						Util.debug(fields[10]);
						Util.debug(fields[11]);
						Util.debug(fields[12]);
						Util.debug(fields[13]);
						Util.debug(fields[14]);
						Util.debug(fields[15]);
						Util.debug(fields[16]);
						Util.debug(fields[17]);
						Util.debug(fields[18]);
						Util.debug(fields[19]);
						Util.debug(fields[20]);
						cart.createOrder(customerID, billName, billAddr1, billAddr2, billCity, billState, billZip, billPhone, shipName, shipAddr1, shipAddr2, shipCity, shipState, shipZip, shipPhone, creditCard, ccNum, ccExpireMonth, ccExpireYear, cardHolder, shippingMethod, items);
					} else {
						Util.debug("Property does not contain enough fields: " + values[index]);
						Util.debug("Fields found were: " + fields);
					}
				}
			}
			// stmt.executeUpdate(" INSERT INTO ORDERITEM(INVENTORYID, NAME, PKGINFO, PRICE, COST,
			// CATEGORY, QUANTITY, SELLDATE, ORDER_ORDERID) VALUES ('A0001', 'Bulb Digger',
			// 'Assembled', 12.0, 5.0, 3, 900, '01054835419625', '1')");
		} catch (Exception e) {
			Util.debug("Unable to populate ORDERITEM table with text data: " + e);
			e.printStackTrace();
			e.printStackTrace();
		}
		/**
		 * Populate BACKORDER table with text
		 */
		Util.debug("Populating BACKORDER table with default values...");
		try {
			String[] values = Util.getProperties("backorder");
			Util.debug("Found BACKORDER properties:  " + values[0]);
			// Inserting backorders
			for (int index = 0; index < values.length; index++) {
				String[] fields = Util.readTokens(values[index], "|");
				String inventoryID = fields[0];
				int amountToOrder = new Integer(fields[1]).intValue();
				int maximumItems = new Integer(fields[2]).intValue();
				Util.debug("Populating BACKORDER with following values:  ");
				Util.debug(inventoryID);
				Util.debug("amountToOrder -> " + amountToOrder);
				Util.debug("maximumItems -> " + maximumItems);
				backOrderStock.createBackOrder(inventoryID, amountToOrder, maximumItems);
			}
		} catch (Exception e) {
			Util.debug("Unable to populate BACKORDER table with text data: " + e);
			e.printStackTrace();
		}
		/**
		 * Populate SUPPLIER table with text
		 */
		Util.debug("Populating SUPPLIER table with default values...");
		try {
			String[] values = Util.getProperties("supplier");
			Util.debug("Found SUPPLIER properties:  " + values[0]);
			// Inserting Suppliers
			for (int index = 0; index < values.length; index++) {
				String[] fields = Util.readTokens(values[index], "|");
				String supplierID = fields[0];
				String name = fields[1];
				String address = fields[2];
				String city = fields[3];
				String state = fields[4];
				String zip = fields[5];
				String phone = fields[6];
				String url = fields[7];
				Util.debug("Populating SUPPLIER with following values:  ");
				Util.debug(fields[0]);
				Util.debug(fields[1]);
				Util.debug(fields[2]);
				Util.debug(fields[3]);
				Util.debug(fields[4]);
				Util.debug(fields[5]);
				Util.debug(fields[6]);
				Util.debug(fields[7]);
				suppliers.createSupplier(supplierID, name, address, city, state, zip, phone, url);
			}
		} catch (Exception e) {
			Util.debug("Unable to populate SUPPLIER table with text data: " + e);
			e.printStackTrace();
		}
	}

	@Transactional
	public void deleteAll() {
		try {
			Query q = em.createNamedQuery("removeAllOrders");
			q.executeUpdate();
			q = em.createNamedQuery("removeAllInventory");
			q.executeUpdate();
			// q=em.createNamedQuery("removeAllIdGenerator");
			// q.executeUpdate();
			q = em.createNamedQuery("removeAllCustomers");
			q.executeUpdate();
			q = em.createNamedQuery("removeAllOrderItem");
			q.executeUpdate();
			q = em.createNamedQuery("removeAllBackOrder");
			q.executeUpdate();
			q = em.createNamedQuery("removeAllSupplier");
			q.executeUpdate();
			em.flush();
			Util.debug("Deleted all data from database");
		} catch (Exception e) {
			Util.debug("ResetDB(deleteAll) -- Error deleting data from the database: " + e);
			e.printStackTrace();
			try {
                tx.setRollbackOnly();
            } catch (IllegalStateException | SystemException ignore) {
            }
		}
	}

}
//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

/**
 * MailerAppException extends the standard Exception. This is thrown by the mailer component when
 * there is some failure sending the mail.
 */
public class MailerAppException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MailerAppException() {
	}

	public MailerAppException(String str) {
		super(str);
	}

}

//
// COPYRIGHT LICENSE: This information contains sample code provided in source code form. You may copy, 
// modify, and distribute these sample programs in any form without payment to IBM for the purposes of 
// developing, using, marketing or distributing application programs conforming to the application 
// programming interface for the operating platform for which the sample code is written. 
// Notwithstanding anything to the contrary, IBM PROVIDES THE SAMPLE SOURCE CODE ON AN "AS IS" BASIS 
// AND IBM DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED 
// WARRANTIES OR CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A PARTICULAR PURPOSE, 
// TITLE, AND ANY WARRANTY OR CONDITION OF NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR ANY DIRECT, 
// INDIRECT, INCIDENTAL, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR OPERATION OF THE 
// SAMPLE SOURCE CODE. IBM HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS 
// OR MODIFICATIONS TO THE SAMPLE SOURCE CODE.  
//
// (C) COPYRIGHT International Business Machines Corp., 2001,2011
// All Rights Reserved * Licensed Materials - Property of IBM
//

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.SessionScoped;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.ibm.websphere.samples.pbw.jpa.BackOrder;
import com.ibm.websphere.samples.pbw.jpa.Customer;
import com.ibm.websphere.samples.pbw.jpa.Inventory;
import com.ibm.websphere.samples.pbw.jpa.Order;
import com.ibm.websphere.samples.pbw.jpa.OrderItem;
import com.ibm.websphere.samples.pbw.utils.Util;

/**
 * ShopingCartBean provides a transactional facade for order collection and processing.
 * 
 */

@Transactional
@SessionScoped
public class ShoppingCartBean implements Serializable {

	@PersistenceContext(unitName = "PBW")
	EntityManager em;

	private ArrayList<Inventory> items = new ArrayList<Inventory>();

	/**
	 * Add an item to the cart.
	 *
	 * @param new_item
	 *            Item to add to the cart.
	 */
	public void addItem(Inventory new_item) {
		boolean added = false;
		// If the same item is already in the cart, just increase the quantity.
		for (Inventory old_item : items) {
			if (old_item.getID().equals(new_item.getID())) {
				old_item.setQuantity(old_item.getQuantity() + new_item.getQuantity());
				added = true;
				break;
			}
		}
		// Add this item to shopping cart, if it is a brand new item.
		if (!added)
			items.add(new_item);
	}

	/**
	 * Remove an item from the cart.
	 *
	 * @param item
	 *            Item to remove from cart.
	 */
	public void removeItem(Inventory item) {
		for (Inventory i : items) {
			if (item.equals(i)) {
				items.remove(i);
				break;
			}
		}
	}

	/**
	 * Remove all items from the cart.
	 */
	public void removeAllItems() {
		items = new ArrayList<Inventory>();
	}

	/**
	 * Remove zero quantity items.
	 */
	public void removeZeroQuantityItems() {
		ArrayList<Inventory> newItems = new ArrayList<Inventory>();

		for (Inventory i : items) {
			if (i.getQuantity() > 0) {
				newItems.add(i);
			}
		}

		items = newItems;
	}

	/**
	 * Get the items in the shopping cart.
	 *
	 * @return A Collection of ShoppingCartItems.
	 */
	public ArrayList<Inventory> getItems() {
		return items;
	}

	/**
	 * Set the items in the shopping cart.
	 *
	 * @param items
	 *            A Vector of ShoppingCartItem's.
	 */
	public void setItems(Collection<Inventory> items) {
		this.items = new ArrayList<Inventory>(items);
	}

	/**
	 * Get the contents of the shopping cart.
	 *
	 * @return The contents of the shopping cart. / public ShoppingCartContents getCartContents() {
	 *         ShoppingCartContents cartContents = new ShoppingCartContents(); // Fill it with data.
	 *         for (int i = 0; i < items.size(); i++) { cartContents.addItem((ShoppingCartItem)
	 *         items.get(i)); } return cartContents; }
	 */

	/**
	 * Create a shopping cart.
	 *
	 * @param cartContents
	 *            Contents to populate cart with. / public void setCartContents(ShoppingCartContents
	 *            cartContents) { items = new ArrayList<ShoppingCartItem>(); int qty; String
	 *            inventoryID; ShoppingCartItem si; Inventory inv; for (int i = 0; i <
	 *            cartContents.size(); i++) { inventoryID = cartContents.getInventoryID(i); qty =
	 *            cartContents.getQuantity(inventoryID); inv = em.find(Inventory.class,
	 *            inventoryID); // clone so we can use Qty as qty to purchase, not inventory in
	 *            stock si = new ShoppingCartItem(inv); si.setQuantity(qty); addItem(si); } }
	 */

	/**
	 * Get the cost of all items in the shopping cart.
	 *
	 * @return The total cost of all items in the shopping cart.
	 */
	public float getSubtotalCost() {
		float f = 0.0F;

		for (Inventory item : items) {
			f += item.getPrice() * (float) item.getQuantity();
		}
		return f;
	}

	/**
	 * Method checkInventory. Check the inventory level of a store item. Order additional inventory
	 * when necessary.
	 *
	 * @param si
	 *            - Store item
	 */
	public void checkInventory(Inventory si) {
		Util.debug("ShoppingCart.checkInventory() - checking Inventory quantity of item: " + si.getID());
		Inventory inv = getInventoryItem(si.getID());

		/**
		 * Decrease the quantity of this inventory item.
		 * 
		 * @param quantity
		 *            The number to decrease the inventory by.
		 * @return The number of inventory items removed.
		 */
		int quantity = si.getQuantity();
		int minimumItems = inv.getMinThreshold();

		int amountToOrder = 0;
		Util.debug("ShoppingCartBean:checkInventory() - Decreasing inventory item " + inv.getInventoryId());
		int quantityNotFilled = 0;
		if (inv.getQuantity() < 1) {
			quantityNotFilled = quantity;
		} else if (inv.getQuantity() < quantity) {
			quantityNotFilled = quantity - inv.getQuantity();
		}

		// When quantity becomes < 0, this will be to determine the
		// quantity of unfilled orders due to insufficient stock.
		inv.setQuantity(inv.getQuantity() - quantity);

		// Check to see if more inventory needs to be ordered from the supplier
		// based on a set minimum Threshold
		if (inv.getQuantity() < minimumItems) {
			// Calculate the amount of stock to order from the supplier
			// to get the inventory up to the maximum.
			amountToOrder = quantityNotFilled;
			backOrder(inv, amountToOrder);
		}

	}

	/**
	 * Create an order with contents of a shopping cart.
	 *
	 * @param customerID
	 *            customer's ID
	 * @param billName
	 *            billing name
	 * @param billAddr1
	 *            billing address line 1
	 * @param billAddr2
	 *            billing address line 2
	 * @param billCity
	 *            billing address city
	 * @param billState
	 *            billing address state
	 * @param billZip
	 *            billing address zip code
	 * @param billPhone
	 *            billing phone
	 * @param shipName
	 *            shippng name
	 * @param shipAddr1
	 *            shippng address line 1
	 * @param shipAddr2
	 *            shippng address line 2
	 * @param shipCity
	 *            shippng address city
	 * @param shipState
	 *            shippng address state
	 * @param shipZip
	 *            shippng address zip code
	 * @param shipPhone
	 *            shippng phone
	 * @param creditCard
	 *            credit card
	 * @param ccNum
	 *            credit card number
	 * @param ccExpireMonth
	 *            credit card expiration month
	 * @param ccExpireYear
	 *            credit card expiration year
	 * @param cardHolder
	 *            credit card holder name
	 * @param shippingMethod
	 *            int of shipping method used
	 * @param items
	 *            vector of StoreItems ordered
	 * @return OrderInfo
	 */
	public Order createOrder(String customerID,
			String billName,
			String billAddr1,
			String billAddr2,
			String billCity,
			String billState,
			String billZip,
			String billPhone,
			String shipName,
			String shipAddr1,
			String shipAddr2,
			String shipCity,
			String shipState,
			String shipZip,
			String shipPhone,
			String creditCard,
			String ccNum,
			String ccExpireMonth,
			String ccExpireYear,
			String cardHolder,
			int shippingMethod,
			Collection<Inventory> items) {
		Order order = null;
		Util.debug("ShoppingCartBean.createOrder:  Creating Order");
		Collection<OrderItem> orderitems = new ArrayList<OrderItem>();
		for (Inventory si : items) {
			Inventory inv = em.find(Inventory.class, si.getID());
			OrderItem oi = new OrderItem(inv);
			oi.setQuantity(si.getQuantity());
			orderitems.add(oi);
		}
		Customer c = em.find(Customer.class, customerID);
		order = new Order(c, billName, billAddr1, billAddr2, billCity, billState, billZip, billPhone, shipName,
				shipAddr1, shipAddr2, shipCity, shipState, shipZip, shipPhone, creditCard, ccNum, ccExpireMonth,
				ccExpireYear, cardHolder, shippingMethod, orderitems);
		em.persist(order);
		em.flush();
		// store the order items
		for (OrderItem o : orderitems) {
			o.setOrder(order);
			o.updatePK();
			em.persist(o);
		}
		em.flush();

		return order;
	}

	public int getSize() {
		return getItems().size();
	}

	/*
	 * Get the inventory item.
	 *
	 * @param id of inventory item.
	 * 
	 * @return an inventory bean.
	 */
	private Inventory getInventoryItem(String inventoryID) {
		Inventory inv = null;
		inv = em.find(Inventory.class, inventoryID);
		return inv;
	}

	/*
	 * Create a BackOrder of this inventory item.
	 * 
	 * @param quantity The number of the inventory item to be backordered
	 */
	private void backOrder(Inventory inv, int amountToOrder) {
		BackOrder b = em.find(BackOrder.class, inv.getInventoryId());
		if (b == null) {
			// create a new backorder if none exists
			BackOrder newBO = new BackOrder(inv, amountToOrder);
			em.persist(newBO);
			em.flush();
			inv.setBackOrder(newBO);
		} else {
			// update the backorder with the new quantity
			int quantity = b.getQuantity();
			quantity += amountToOrder;
			em.lock(b, LockModeType.WRITE);
			em.refresh(b);
			b.setQuantity(quantity);
			em.flush();
			inv.setBackOrder(b);
		}
	}

}

