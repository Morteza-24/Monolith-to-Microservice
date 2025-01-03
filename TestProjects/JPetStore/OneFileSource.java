
import java.io.Serializable;


public class Category implements Serializable {

  /* Private Fields */

  private String categoryId;
  private String name;
  private String description;

  /* JavaBeans Properties */

  public String getCategoryId() { return categoryId; }
  public void setCategoryId(String categoryId) { this.categoryId = categoryId.trim(); }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  /* Public Methods */

  public String toString() {
    return getCategoryId();
  }

}


import java.io.Serializable;


public class Item implements Serializable {

  /* Private Fields */

  private String itemId;
  private String productId;
  private double listPrice;
  private double unitCost;
  private int supplierId;
  private String status;
  private String attribute1;
  private String attribute2;
  private String attribute3;
  private String attribute4;
  private String attribute5;
  private Product product;
  private int quantity;

  /* JavaBeans Properties */

  public String getItemId() { return itemId; }
  public void setItemId(String itemId) { this.itemId = itemId.trim(); }

  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }

  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }

  public String getProductId() { return productId; }
  public void setProductId(String productId) { this.productId = productId; }

  public int getSupplierId() { return supplierId; }
  public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

  public double getListPrice() { return listPrice; }
  public void setListPrice(double listPrice) { this.listPrice = listPrice; }

  public double getUnitCost() { return unitCost; }
  public void setUnitCost(double unitCost) { this.unitCost = unitCost; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getAttribute1() { return attribute1; }
  public void setAttribute1(String attribute1) { this.attribute1 = attribute1; }

  public String getAttribute2() { return attribute2; }
  public void setAttribute2(String attribute2) { this.attribute2 = attribute2; }

  public String getAttribute3() { return attribute3; }
  public void setAttribute3(String attribute3) { this.attribute3 = attribute3; }

  public String getAttribute4() { return attribute4; }
  public void setAttribute4(String attribute4) { this.attribute4 = attribute4; }

  public String getAttribute5() { return attribute5; }
  public void setAttribute5(String attribute5) { this.attribute5 = attribute5; }

  /* Public Methods */

  public String toString() {
    return "(" + getItemId().trim() + "-" + getProductId().trim() + ")";
  }

}


import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.support.PagedListHolder;

public class Cart implements Serializable {

  /* Private Fields */

  private final Map itemMap = Collections.synchronizedMap(new HashMap());
	
  private final PagedListHolder itemList = new PagedListHolder();

  /* JavaBeans Properties */

	public Cart() {
		this.itemList.setPageSize(4);
	}

	public Iterator getAllCartItems() { return itemList.getSource().iterator(); }
  public PagedListHolder getCartItemList() { return itemList; }
  public int getNumberOfItems() { return itemList.getSource().size(); }

  /* Public Methods */

  public boolean containsItemId(String itemId) {
    return itemMap.containsKey(itemId);
  }

  public void addItem(Item item, boolean isInStock) {
    CartItem cartItem = (CartItem) itemMap.get(item.getItemId());
    if (cartItem == null) {
      cartItem = new CartItem();
      cartItem.setItem(item);
      cartItem.setQuantity(0);
      cartItem.setInStock(isInStock);
      itemMap.put(item.getItemId(), cartItem);
      itemList.getSource().add(cartItem);
    }
    cartItem.incrementQuantity();
  }


  public Item removeItemById(String itemId) {
    CartItem cartItem = (CartItem) itemMap.remove(itemId);
    if (cartItem == null) {
      return null;
    }
		else {
      itemList.getSource().remove(cartItem);
      return cartItem.getItem();
    }
  }

  public void incrementQuantityByItemId(String itemId) {
    CartItem cartItem = (CartItem) itemMap.get(itemId);
    cartItem.incrementQuantity();
  }

  public void setQuantityByItemId(String itemId, int quantity) {
    CartItem cartItem = (CartItem) itemMap.get(itemId);
    cartItem.setQuantity(quantity);
  }

  public double getSubTotal() {
    double subTotal = 0;
    Iterator items = getAllCartItems();
    while (items.hasNext()) {
      CartItem cartItem = (CartItem) items.next();
      Item item = cartItem.getItem();
      double listPrice = item.getListPrice();
      int quantity = cartItem.getQuantity();
      subTotal += listPrice * quantity;
    }
    return subTotal;
  }

}


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Order implements Serializable {

  /* Private Fields */

  private int orderId;
  private String username;
  private Date orderDate;
  private String shipAddress1;
  private String shipAddress2;
  private String shipCity;
  private String shipState;
  private String shipZip;
  private String shipCountry;
  private String billAddress1;
  private String billAddress2;
  private String billCity;
  private String billState;
  private String billZip;
  private String billCountry;
  private String courier;
  private double totalPrice;
  private String billToFirstName;
  private String billToLastName;
  private String shipToFirstName;
  private String shipToLastName;
  private String creditCard;
  private String expiryDate;
  private String cardType;
  private String locale;
  private String status;
  private List lineItems = new ArrayList();

  /* JavaBeans Properties */

  public int getOrderId() { return orderId; }
  public void setOrderId(int orderId) { this.orderId = orderId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public Date getOrderDate() { return orderDate; }
  public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

  public String getShipAddress1() { return shipAddress1; }
  public void setShipAddress1(String shipAddress1) { this.shipAddress1 = shipAddress1; }

  public String getShipAddress2() { return shipAddress2; }
  public void setShipAddress2(String shipAddress2) { this.shipAddress2 = shipAddress2; }

  public String getShipCity() { return shipCity; }
  public void setShipCity(String shipCity) { this.shipCity = shipCity; }

  public String getShipState() { return shipState; }
  public void setShipState(String shipState) { this.shipState = shipState; }

  public String getShipZip() { return shipZip; }
  public void setShipZip(String shipZip) { this.shipZip = shipZip; }

  public String getShipCountry() { return shipCountry; }
  public void setShipCountry(String shipCountry) { this.shipCountry = shipCountry; }

  public String getBillAddress1() { return billAddress1; }
  public void setBillAddress1(String billAddress1) { this.billAddress1 = billAddress1; }

  public String getBillAddress2() { return billAddress2; }
  public void setBillAddress2(String billAddress2) { this.billAddress2 = billAddress2; }

  public String getBillCity() { return billCity; }
  public void setBillCity(String billCity) { this.billCity = billCity; }

  public String getBillState() { return billState; }
  public void setBillState(String billState) { this.billState = billState; }

  public String getBillZip() { return billZip; }
  public void setBillZip(String billZip) { this.billZip = billZip; }

  public String getBillCountry() { return billCountry; }
  public void setBillCountry(String billCountry) { this.billCountry = billCountry; }

  public String getCourier() { return courier; }
  public void setCourier(String courier) { this.courier = courier; }

  public double getTotalPrice() { return totalPrice; }
  public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

  public String getBillToFirstName() { return billToFirstName; }
  public void setBillToFirstName(String billToFirstName) { this.billToFirstName = billToFirstName; }

  public String getBillToLastName() { return billToLastName; }
  public void setBillToLastName(String billToLastName) { this.billToLastName = billToLastName; }

  public String getShipToFirstName() { return shipToFirstName; }
  public void setShipToFirstName(String shipFoFirstName) { this.shipToFirstName = shipFoFirstName; }

  public String getShipToLastName() { return shipToLastName; }
  public void setShipToLastName(String shipToLastName) { this.shipToLastName = shipToLastName; }

  public String getCreditCard() { return creditCard; }
  public void setCreditCard(String creditCard) { this.creditCard = creditCard; }

  public String getExpiryDate() { return expiryDate; }
  public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

  public String getCardType() { return cardType; }
  public void setCardType(String cardType) { this.cardType = cardType; }

  public String getLocale() { return locale; }
  public void setLocale(String locale) { this.locale = locale; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public void setLineItems(List lineItems) { this.lineItems = lineItems; }
  public List getLineItems() { return lineItems; }

  /* Public Methods */

  public void initOrder(Account account, Cart cart) {
    username = account.getUsername();
    orderDate = new Date();

    shipToFirstName = account.getFirstName();
    shipToLastName = account.getLastName();
    shipAddress1 = account.getAddress1();
    shipAddress2 = account.getAddress2();
    shipCity = account.getCity();
    shipState = account.getState();
    shipZip = account.getZip();
    shipCountry = account.getCountry();

    billToFirstName = account.getFirstName();
    billToLastName = account.getLastName();
    billAddress1 = account.getAddress1();
    billAddress2 = account.getAddress2();
    billCity = account.getCity();
    billState = account.getState();
    billZip = account.getZip();
    billCountry = account.getCountry();

    totalPrice = cart.getSubTotal();

    creditCard = "999 9999 9999 9999";
    expiryDate = "12/03";
    cardType = "Visa";
    courier = "UPS";
    locale = "CA";
    status = "P";

    Iterator i = cart.getAllCartItems();
    while (i.hasNext()) {
      CartItem cartItem = (CartItem) i.next();
      addLineItem(cartItem);
    }
  }

  public void addLineItem(CartItem cartItem) {
    LineItem lineItem = new LineItem(lineItems.size() + 1, cartItem);
    addLineItem(lineItem);
  }

  public void addLineItem(LineItem lineItem) {
    lineItems.add(lineItem);
  }


}


import java.io.Serializable;

public class LineItem implements Serializable {

  /* Private Fields */

  private int orderId;
  private int lineNumber;
  private int quantity;
  private String itemId;
  private double unitPrice;
  private Item item;

  /* Constructors */

  public LineItem() {
  }

  public LineItem(int lineNumber, CartItem cartItem) {
    this.lineNumber = lineNumber;
    this.quantity = cartItem.getQuantity();
    this.itemId = cartItem.getItem().getItemId();
    this.unitPrice = cartItem.getItem().getListPrice();
    this.item = cartItem.getItem();
  }

  /* JavaBeans Properties */

  public int getOrderId() { return orderId; }
  public void setOrderId(int orderId) { this.orderId = orderId; }

  public int getLineNumber() { return lineNumber; }
  public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

  public String getItemId() { return itemId; }
  public void setItemId(String itemId) { this.itemId = itemId; }

  public double getUnitPrice() { return unitPrice; }
  public void setUnitPrice(double unitprice) { this.unitPrice = unitprice; }

  public Item getItem() { return item; }
  public void setItem(Item item) {
    this.item = item;
  }

  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

	public double getTotalPrice() {
		return this.unitPrice * this.quantity;
	}

}


import java.io.Serializable;


public class Account implements Serializable {

  /* Private Fields */

  private String username;
  private String password;
  private String email;
  private String firstName;
  private String lastName;
  private String status;
  private String address1;
  private String address2;
  private String city;
  private String state;
  private String zip;
  private String country;
  private String phone;
  private String favouriteCategoryId;
  private String languagePreference;
  private boolean listOption;
  private boolean bannerOption;
  private String bannerName;

  /* JavaBeans Properties */

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getAddress1() { return address1; }
  public void setAddress1(String address1) { this.address1 = address1; }

  public String getAddress2() { return address2; }
  public void setAddress2(String address2) { this.address2 = address2; }

  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }

  public String getState() { return state; }
  public void setState(String state) { this.state = state; }

  public String getZip() { return zip; }
  public void setZip(String zip) { this.zip = zip; }

  public String getCountry() { return country; }
  public void setCountry(String country) { this.country = country; }

  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }

  public String getFavouriteCategoryId() { return favouriteCategoryId; }
  public void setFavouriteCategoryId(String favouriteCategoryId) { this.favouriteCategoryId = favouriteCategoryId; }

  public String getLanguagePreference() { return languagePreference; }
  public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }

  public boolean isListOption() { return listOption; }
  public void setListOption(boolean listOption) { this.listOption = listOption; }
	public int getListOptionAsInt() { return listOption ? 1 : 0; }

  public boolean isBannerOption() { return bannerOption; }
  public void setBannerOption(boolean bannerOption) { this.bannerOption = bannerOption; }
	public int getBannerOptionAsInt() { return bannerOption ? 1 : 0; }

  public String getBannerName() { return bannerName; }
  public void setBannerName(String bannerName) { this.bannerName = bannerName; }

}


import java.io.Serializable;


public class Product implements Serializable {

  /* Private Fields */

  private String productId;
  private String categoryId;
  private String name;
  private String description;

  /* JavaBeans Properties */

  public String getProductId() { return productId; }
  public void setProductId(String productId) { this.productId = productId.trim(); }

  public String getCategoryId() { return categoryId; }
  public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  /* Public Methods*/

  public String toString() {
    return getName();
  }

}


import java.io.Serializable;

public class CartItem implements Serializable {

  /* Private Fields */

  private Item item;
  private int quantity;
  private boolean inStock;

  /* JavaBeans Properties */

  public boolean isInStock() { return inStock; }
  public void setInStock(boolean inStock) { this.inStock = inStock; }

  public Item getItem() { return item; }
  public void setItem(Item item) {
    this.item = item;
  }

  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

	public double getTotalPrice() {
		if (item != null) {
			return item.getListPrice() * quantity;
		}
		else {
			return 0;
		}
	}

  /* Public methods */

  public void incrementQuantity() {
    quantity++;
  }

}


import java.util.List;

import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.samples.jpetstore.domain.Category;
import org.springframework.samples.jpetstore.domain.Item;
import org.springframework.samples.jpetstore.domain.Order;
import org.springframework.samples.jpetstore.domain.Product;

/**
 * JPetStore's central business interface.
 *
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public interface PetStoreFacade {

	Account getAccount(String username);

	Account getAccount(String username, String password);

	void insertAccount(Account account);

	void updateAccount(Account account);

	List getUsernameList();


	List getCategoryList();

	Category getCategory(String categoryId);
	

	List getProductListByCategory(String categoryId);

	List searchProductList(String keywords);

	Product getProduct(String productId);


	List getItemListByProduct(String productId);

	Item getItem(String itemId);

	boolean isItemInStock(String itemId);


	void insertOrder(Order order);

	Order getOrder(int orderId);

	List getOrdersByUsername(String username);

}


import java.util.List;

import org.springframework.samples.jpetstore.dao.AccountDao;
import org.springframework.samples.jpetstore.dao.CategoryDao;
import org.springframework.samples.jpetstore.dao.ItemDao;
import org.springframework.samples.jpetstore.dao.OrderDao;
import org.springframework.samples.jpetstore.dao.ProductDao;
import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.samples.jpetstore.domain.Category;
import org.springframework.samples.jpetstore.domain.Item;
import org.springframework.samples.jpetstore.domain.Order;
import org.springframework.samples.jpetstore.domain.Product;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPetStore primary business object.
 * 
 * <p>This object makes use of five DAO objects, decoupling it
 * from the details of working with persistence APIs. So
 * although this application uses iBATIS for data access,
 * a different persistence tool could be dropped in without
 * breaking this class.
 *
 * <p>The DAOs are made available to the instance of this object
 * using Dependency Injection. (The DAOs are in turn configured using
 * Dependency Injection themselves.) We use Setter Injection here,
 * exposing JavaBean setter methods for each DAO. This means there is
 * a JavaBean property for each DAO. In the present case, the properties
 * are write-only: there are no corresponding getter methods. Getter
 * methods for configuration properties are optional: Implement them
 * only if you want to expose those properties to other business objects.
 *
 * <p>There is one instance of this class in the JPetStore application.
 * In Spring terminology, it is a "singleton", referring to a
 * per-Application Context singleton. The factory creates a single
 * instance; there is no need for a private constructor, static
 * factory method etc as in the traditional implementation of
 * the Singleton Design Pattern. 
 *
 * <p>This is a POJO. It does not depend on any Spring APIs.
 * It's usable outside a Spring container, and can be instantiated
 * using new in a JUnit test. However, we can still apply declarative
 * transaction management to it using Spring AOP.
 *
 * <p>This class defines a default transaction annotation for all methods.
 *
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
@Transactional
public class PetStoreImpl implements PetStoreFacade, OrderService {

	private AccountDao accountDao;

	private CategoryDao categoryDao;

	private ProductDao productDao;

	private ItemDao itemDao;

	private OrderDao orderDao;


	//-------------------------------------------------------------------------
	// Setter methods for dependency injection
	//-------------------------------------------------------------------------

	public void setAccountDao(AccountDao accountDao) {
		this.accountDao = accountDao;
	}

	public void setCategoryDao(CategoryDao categoryDao) {
		this.categoryDao = categoryDao;
	}

	public void setProductDao(ProductDao productDao) {
		this.productDao = productDao;
	}

	public void setItemDao(ItemDao itemDao) {
		this.itemDao = itemDao;
	}

	public void setOrderDao(OrderDao orderDao) {
		this.orderDao = orderDao;
	}


	//-------------------------------------------------------------------------
	// Operation methods, implementing the PetStoreFacade interface
	//-------------------------------------------------------------------------

	public Account getAccount(String username) {
		return this.accountDao.getAccount(username);
	}

	public Account getAccount(String username, String password) {
		return this.accountDao.getAccount(username, password);
	}

	public void insertAccount(Account account) {
		this.accountDao.insertAccount(account);
	}

	public void updateAccount(Account account) {
		this.accountDao.updateAccount(account);
	}

	public List getUsernameList() {
		return this.accountDao.getUsernameList();
	}

	public List getCategoryList() {
		return this.categoryDao.getCategoryList();
	}

	public Category getCategory(String categoryId) {
		return this.categoryDao.getCategory(categoryId);
	}

	public List getProductListByCategory(String categoryId) {
		return this.productDao.getProductListByCategory(categoryId);
	}

	public List searchProductList(String keywords) {
		return this.productDao.searchProductList(keywords);
	}

	public Product getProduct(String productId) {
		return this.productDao.getProduct(productId);
	}

	public List getItemListByProduct(String productId) {
		return this.itemDao.getItemListByProduct(productId);
	}

	public Item getItem(String itemId) {
		return this.itemDao.getItem(itemId);
	}

	public boolean isItemInStock(String itemId) {
		return this.itemDao.isItemInStock(itemId);
	}

	public void insertOrder(Order order) {
		this.orderDao.insertOrder(order);
		this.itemDao.updateQuantity(order);
	}

	public Order getOrder(int orderId) {
		return this.orderDao.getOrder(orderId);
	}

	public List getOrdersByUsername(String username) {
		return this.orderDao.getOrdersByUsername(username);
	}

}


import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.samples.jpetstore.domain.Order;

/**
 * AOP advice that sends confirmation email after order has been submitted
 * @author Dmitriy Kopylenko
 */
public class SendOrderConfirmationEmailAdvice implements AfterReturningAdvice, InitializingBean {

	private static final String DEFAULT_MAIL_FROM = "jpetstore@springframework.org";

	private static final String DEFAULT_SUBJECT = "Thank you for your order!";

	private final Log logger = LogFactory.getLog(getClass());

	private MailSender mailSender;

	private String mailFrom = DEFAULT_MAIL_FROM;

	private String subject = DEFAULT_SUBJECT;

	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setMailFrom(String mailFrom) {
		this.mailFrom = mailFrom;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.mailSender == null) {
			throw new IllegalStateException("mailSender is required");
		}
	}

	public void afterReturning(Object returnValue, Method m, Object[] args, Object target) throws Throwable {
		Order order = (Order) args[0];
		Account account = ((PetStoreFacade) target).getAccount(order.getUsername());

		// don't do anything if email address is not set
		if (account.getEmail() == null || account.getEmail().length() == 0) {
			return;
		}

		StringBuffer text = new StringBuffer();
		text.append("Dear ").append(account.getFirstName()).append(' ').append(account.getLastName());
		text.append(", thank your for your order from JPetStore. Please note that your order number is ");
		text.append(order.getOrderId());

		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setTo(account.getEmail());
		mailMessage.setFrom(this.mailFrom);
		mailMessage.setSubject(this.subject);
		mailMessage.setText(text.toString());
		try {
			this.mailSender.send(mailMessage);
		}
		catch (MailException ex) {
			// just log it and go on
			logger.warn("An exception occured when trying to send email", ex);
		}
	}

}


import org.springframework.samples.jpetstore.domain.Order;

/**
 * Separate OrderService interface, implemented by PetStoreImpl
 * in addition to PetStoreFacade.
 *
 * <p>Mainly targeted at usage as remote service interface,
 * just exposing the <code>getOrder</code> method.
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 * @see PetStoreFacade
 * @see PetStoreImpl
 * @see org.springframework.samples.jpetstore.service.JaxRpcOrderService
 */
public interface OrderService {

	Order getOrder(int orderId);

}


import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class AccountValidator implements Validator {

	public boolean supports(Class clazz) {
		return Account.class.isAssignableFrom(clazz);
	}

	public void validate(Object obj, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "firstName", "FIRST_NAME_REQUIRED", "First name is required.");
		ValidationUtils.rejectIfEmpty(errors, "lastName", "LAST_NAME_REQUIRED", "Last name is required.");
		ValidationUtils.rejectIfEmpty(errors, "email", "EMAIL_REQUIRED", "Email address is required.");
		ValidationUtils.rejectIfEmpty(errors, "phone", "PHONE_REQUIRED", "Phone number is required.");
		ValidationUtils.rejectIfEmpty(errors, "address1", "ADDRESS_REQUIRED", "Address (1) is required.");
		ValidationUtils.rejectIfEmpty(errors, "city", "CITY_REQUIRED", "City is required.");
		ValidationUtils.rejectIfEmpty(errors, "state", "STATE_REQUIRED", "State is required.");
		ValidationUtils.rejectIfEmpty(errors, "zip", "ZIP_REQUIRED", "ZIP is required.");
		ValidationUtils.rejectIfEmpty(errors, "country", "COUNTRY_REQUIRED", "Country is required.");
	}
}


import org.springframework.samples.jpetstore.domain.Order;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class OrderValidator implements Validator {

	public boolean supports(Class clazz) {
		return Order.class.isAssignableFrom(clazz);
	}

	public void validate(Object obj, Errors errors) {
		validateCreditCard((Order) obj, errors);
		validateBillingAddress((Order) obj, errors);
		validateShippingAddress((Order) obj, errors);
	}

	public void validateCreditCard(Order order, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "creditCard", "CCN_REQUIRED", "FAKE (!) credit card number required.");
		ValidationUtils.rejectIfEmpty(errors, "expiryDate", "EXPIRY_DATE_REQUIRED", "Expiry date is required.");
		ValidationUtils.rejectIfEmpty(errors, "cardType", "CARD_TYPE_REQUIRED", "Card type is required.");
	}

	public void validateBillingAddress(Order order, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "billToFirstName", "FIRST_NAME_REQUIRED", "Billing Info: first name is required.");
		ValidationUtils.rejectIfEmpty(errors, "billToLastName", "LAST_NAME_REQUIRED", "Billing Info: last name is required.");
		ValidationUtils.rejectIfEmpty(errors, "billAddress1", "ADDRESS_REQUIRED", "Billing Info: address is required.");
		ValidationUtils.rejectIfEmpty(errors, "billCity", "CITY_REQUIRED", "Billing Info: city is required.");
		ValidationUtils.rejectIfEmpty(errors, "billState", "STATE_REQUIRED", "Billing Info: state is required.");
		ValidationUtils.rejectIfEmpty(errors, "billZip", "ZIP_REQUIRED", "Billing Info: zip/postal code is required.");
		ValidationUtils.rejectIfEmpty(errors, "billCountry", "COUNTRY_REQUIRED", "Billing Info: country is required.");
	}

	public void validateShippingAddress(Order order, Errors errors) {
		ValidationUtils.rejectIfEmpty(errors, "shipToFirstName", "FIRST_NAME_REQUIRED", "Shipping Info: first name is required.");
		ValidationUtils.rejectIfEmpty(errors, "shipToLastName", "LAST_NAME_REQUIRED", "Shipping Info: last name is required.");
		ValidationUtils.rejectIfEmpty(errors, "shipAddress1", "ADDRESS_REQUIRED", "Shipping Info: address is required.");
		ValidationUtils.rejectIfEmpty(errors, "shipCity", "CITY_REQUIRED", "Shipping Info: city is required.");
		ValidationUtils.rejectIfEmpty(errors, "shipState", "STATE_REQUIRED", "Shipping Info: state is required.");
		ValidationUtils.rejectIfEmpty(errors, "shipZip", "ZIP_REQUIRED", "Shipping Info: zip/postal code is required.");
		ValidationUtils.rejectIfEmpty(errors, "shipCountry", "COUNTRY_REQUIRED", "Shipping Info: country is required.");
	}
}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.jpetstore.domain.Category;

public interface CategoryDao {

	List getCategoryList() throws DataAccessException;

  Category getCategory(String categoryId) throws DataAccessException;

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.jpetstore.domain.Item;
import org.springframework.samples.jpetstore.domain.Order;

public interface ItemDao {

  public void updateQuantity(Order order) throws DataAccessException;

  boolean isItemInStock(String itemId) throws DataAccessException;

  List getItemListByProduct(String productId) throws DataAccessException;

  Item getItem(String itemId) throws DataAccessException;

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.jpetstore.domain.Product;

public interface ProductDao {

  List getProductListByCategory(String categoryId) throws DataAccessException;

  List searchProductList(String keywords) throws DataAccessException;

	Product getProduct(String productId) throws DataAccessException;

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.jpetstore.domain.Account;

public interface AccountDao {

  Account getAccount(String username) throws DataAccessException;

  Account getAccount(String username, String password) throws DataAccessException;

  void insertAccount(Account account) throws DataAccessException;

  void updateAccount(Account account) throws DataAccessException;

	List getUsernameList() throws DataAccessException;

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.samples.jpetstore.domain.Order;

public interface OrderDao {

  List getOrdersByUsername(String username) throws DataAccessException;

  Order getOrder(int orderId) throws DataAccessException;

  void insertOrder(Order order) throws DataAccessException;

}


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.samples.jpetstore.dao.ItemDao;
import org.springframework.samples.jpetstore.domain.Item;
import org.springframework.samples.jpetstore.domain.LineItem;
import org.springframework.samples.jpetstore.domain.Order;

public class SqlMapItemDao extends SqlMapClientDaoSupport implements ItemDao {

  public void updateQuantity(Order order) throws DataAccessException {
    for (int i = 0; i < order.getLineItems().size(); i++) {
      LineItem lineItem = (LineItem) order.getLineItems().get(i);
      String itemId = lineItem.getItemId();
      Integer increment = new Integer(lineItem.getQuantity());
      Map param = new HashMap(2);
      param.put("itemId", itemId);
      param.put("increment", increment);
      getSqlMapClientTemplate().update("updateInventoryQuantity", param, 1);
    }
  }

  public boolean isItemInStock(String itemId) throws DataAccessException {
    Integer i = (Integer) getSqlMapClientTemplate().queryForObject("getInventoryQuantity", itemId);
    return (i != null && i.intValue() > 0);
  }

  public List getItemListByProduct(String productId) throws DataAccessException {
    return getSqlMapClientTemplate().queryForList("getItemListByProduct", productId);
  }

  public Item getItem(String itemId) throws DataAccessException {
    Item item = (Item) getSqlMapClientTemplate().queryForObject("getItem", itemId);
		if (item != null) {
			Integer qty = (Integer) getSqlMapClientTemplate().queryForObject("getInventoryQuantity", itemId);
			item.setQuantity(qty.intValue());
		}
    return item;
  }

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.samples.jpetstore.dao.AccountDao;
import org.springframework.samples.jpetstore.domain.Account;

/**
 * In this and other DAOs in this package, a DataSource property
 * is inherited from the SqlMapClientDaoSupport convenience superclass
 * supplied by Spring. DAOs don't need to extend such superclasses,
 * but it saves coding in many cases. There are analogous superclasses
 * for JDBC (JdbcDaoSupport), Hibernate (HibernateDaoSupport),
 * JDO (JdoDaoSupport) etc.
 *
 * <p>This and other DAOs are configured using Dependency Injection.
 * This means, for example, that Spring can source the DataSource
 * from a local class, such as the Commons DBCP BasicDataSource,
 * or from JNDI, concealing the JNDI lookup from application code.
 * 
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 */
public class SqlMapAccountDao extends SqlMapClientDaoSupport implements AccountDao {

  public Account getAccount(String username) throws DataAccessException {
    return (Account) getSqlMapClientTemplate().queryForObject("getAccountByUsername", username);
  }

  public Account getAccount(String username, String password) throws DataAccessException {
    Account account = new Account();
    account.setUsername(username);
    account.setPassword(password);
    return (Account) getSqlMapClientTemplate().queryForObject("getAccountByUsernameAndPassword", account);
  }

  public void insertAccount(Account account) throws DataAccessException {
    getSqlMapClientTemplate().insert("insertAccount", account);
    getSqlMapClientTemplate().insert("insertProfile", account);
    getSqlMapClientTemplate().insert("insertSignon", account);
  }

  public void updateAccount(Account account) throws DataAccessException {
    getSqlMapClientTemplate().update("updateAccount", account, 1);
    getSqlMapClientTemplate().update("updateProfile", account, 1);
    if (account.getPassword() != null && account.getPassword().length() > 0) {
      getSqlMapClientTemplate().update("updateSignon", account, 1);
    }
  }
 
	public List getUsernameList() throws DataAccessException {
		return getSqlMapClientTemplate().queryForList("getUsernameList", null);
	}

}


import org.springframework.dao.DataAccessException;
import org.springframework.samples.jpetstore.domain.LineItem;
import org.springframework.samples.jpetstore.domain.Order;

public class MsSqlOrderDao extends SqlMapOrderDao {

  /**
   * Special MS SQL Server version to allow the Item ID
	 * to be retrieved from an identity column.
   */
  public void insertOrder(Order order) throws DataAccessException {
    Integer orderId = (Integer) getSqlMapClientTemplate().queryForObject("msSqlServerInsertOrder", order);
    order.setOrderId(orderId.intValue());
    getSqlMapClientTemplate().insert("insertOrderStatus", order);
    for (int i = 0; i < order.getLineItems().size(); i++) {
      LineItem lineItem = (LineItem) order.getLineItems().get(i);
      lineItem.setOrderId(order.getOrderId());
      getSqlMapClientTemplate().insert("insertLineItem", lineItem);
    }
  }
  
}


import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

public class SqlMapSequenceDao extends SqlMapClientDaoSupport {

  /**
   * This is a generic sequence ID generator that is based on a database
   * table called 'SEQUENCE', which contains two columns (NAME, NEXTID).
   * This approach should work with any database.
   * @param name the name of the sequence
   * @return the next ID
   */
  public int getNextId(String name) throws DataAccessException {
    Sequence sequence = new Sequence(name, -1);
    sequence = (Sequence) getSqlMapClientTemplate().queryForObject("getSequence", sequence);
    if (sequence == null) {
      throw new DataRetrievalFailureException(
					"Could not get next value of sequence '" + name + "': sequence does not exist");
    }
    Object parameterObject = new Sequence(name, sequence.getNextId() + 1);
    getSqlMapClientTemplate().update("updateSequence", parameterObject, 1);
    return sequence.getNextId();
  }

}


import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.samples.jpetstore.dao.ProductDao;
import org.springframework.samples.jpetstore.domain.Product;

public class SqlMapProductDao extends SqlMapClientDaoSupport implements ProductDao {

  public List getProductListByCategory(String categoryId) throws DataAccessException {
    return getSqlMapClientTemplate().queryForList("getProductListByCategory", categoryId);
  }

  public Product getProduct(String productId) throws DataAccessException {
    return (Product) getSqlMapClientTemplate().queryForObject("getProduct", productId);
  }

  public List searchProductList(String keywords) throws DataAccessException {
    Object parameterObject = new ProductSearch(keywords);
    return getSqlMapClientTemplate().queryForList("searchProductList", parameterObject);
  }


  /* Inner Classes */

  public static class ProductSearch {

    private List keywordList = new ArrayList();

    public ProductSearch(String keywords) {
      StringTokenizer splitter = new StringTokenizer(keywords, " ", false);
      while (splitter.hasMoreTokens()) {
        this.keywordList.add("%" + splitter.nextToken() + "%");
      }
    }

    public List getKeywordList() {
      return keywordList;
    }
  }

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.samples.jpetstore.dao.OrderDao;
import org.springframework.samples.jpetstore.domain.LineItem;
import org.springframework.samples.jpetstore.domain.Order;

public class SqlMapOrderDao extends SqlMapClientDaoSupport implements OrderDao {

  private SqlMapSequenceDao sequenceDao;

	public void setSequenceDao(SqlMapSequenceDao sequenceDao) {
		this.sequenceDao = sequenceDao;
	}

	public List getOrdersByUsername(String username) throws DataAccessException {
    return getSqlMapClientTemplate().queryForList("getOrdersByUsername", username);
  }

  public Order getOrder(int orderId) throws DataAccessException {
    Object parameterObject = new Integer(orderId);
    Order order = (Order) getSqlMapClientTemplate().queryForObject("getOrder", parameterObject);
		if (order != null) {
    	order.setLineItems(getSqlMapClientTemplate().queryForList("getLineItemsByOrderId", new Integer(order.getOrderId())));
		}
    return order;
  }

  public void insertOrder(Order order) throws DataAccessException {
		order.setOrderId(this.sequenceDao.getNextId("ordernum"));
		getSqlMapClientTemplate().insert("insertOrder", order);
		getSqlMapClientTemplate().insert("insertOrderStatus", order);
    for (int i = 0; i < order.getLineItems().size(); i++) {
      LineItem lineItem = (LineItem) order.getLineItems().get(i);
      lineItem.setOrderId(order.getOrderId());
      getSqlMapClientTemplate().insert("insertLineItem", lineItem);
    }
  }

}


import java.io.Serializable;

public class Sequence implements Serializable {

  /* Private Fields */

  private String name;
  private int nextId;

  /* Constructors */

  public Sequence() {
  }

  public Sequence(String name, int nextId) {
    this.name = name;
    this.nextId = nextId;
  }

  /* JavaBeans Properties */

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public int getNextId() { return nextId; }
  public void setNextId(int nextId) { this.nextId = nextId; }

}


import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;
import org.springframework.samples.jpetstore.dao.CategoryDao;
import org.springframework.samples.jpetstore.domain.Category;

public class SqlMapCategoryDao extends SqlMapClientDaoSupport implements CategoryDao {

  public List getCategoryList() throws DataAccessException {
    return getSqlMapClientTemplate().queryForList("getCategoryList", null);
  }

  public Category getCategory(String categoryId) throws DataAccessException {
    return (Category) getSqlMapClientTemplate().queryForObject("getCategory", categoryId);
  }

}


import org.springframework.dao.DataAccessException;

public class OracleSequenceDao extends SqlMapSequenceDao {

  /**
   * Get the next sequence using an Oracle thread-safe sequence
   * @param name Name is the name of the oracle sequence.
   * @return the next sequence
   */
  public int getNextId(String name) throws DataAccessException {
    Sequence sequence = new Sequence();
    sequence.setName(name);
    sequence = (Sequence) getSqlMapClientTemplate().queryForObject("oracleSequence", sequence);
    return sequence.getNextId();
  }

}


import org.springframework.remoting.jaxrpc.ServletEndpointSupport;
import org.springframework.samples.jpetstore.domain.Order;
import org.springframework.samples.jpetstore.domain.logic.OrderService;

/**
 * JAX-RPC OrderService endpoint that simply delegates to the OrderService
 * implementation in the root web application context. Implements the plain
 * OrderService interface as service interface, just like the target bean does.
 *
 * <p>This proxy class is necessary because JAX-RPC/Axis requires a dedicated
 * endpoint class to instantiate. If an existing service needs to be exported,
 * a wrapper that extends ServletEndpointSupport for simple application context
 * access is the simplest JAX-RPC compliant way.
 *
 * <p>This is the class registered with the server-side JAX-RPC implementation.
 * In the case of Axis, this happens in "server-config.wsdd" respectively via
 * deployment calls. The Web Service tool manages the lifecycle of instances
 * of this class: A Spring application context can just be accessed here.
 *
 * <p>Note that this class does <i>not</i> implement an RMI port interface,
 * despite the JAX-RPC spec requiring this for service endpoints. Axis and
 * other JAX-RPC implementations are known to accept non-RMI endpoint classes
 * too, so there's no need to maintain an RMI port interface in addition to
 * the existing non-RMI service interface (OrderService).
 *
 * <p>If your JAX-RPC implementation imposes a strict requirement on a service
 * endpoint class to implement an RMI port interface, then let your endpoint
 * class implement both the non-RMI service interface and the RMI port interface.
 * This will work as long as the methods in both interfaces just differ in the
 * declared RemoteException. Of course, this unfortunately involves double
 * maintenance: one interface for your business logic, one for JAX-RPC.
 * Therefore, it is usually preferable to avoid this if not absolutely necessary.
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 */
public class JaxRpcOrderService extends ServletEndpointSupport implements OrderService {

	private OrderService orderService;

	protected void onInit() {
		this.orderService = (OrderService) getWebApplicationContext().getBean("petStore");
	}

	public Order getOrder(int orderId) {
		return this.orderService.getOrder(orderId);
	}

}


import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.samples.jpetstore.domain.LineItem;
import org.springframework.samples.jpetstore.domain.Order;
import org.springframework.samples.jpetstore.domain.logic.OrderService;
import org.springframework.util.StopWatch;

/**
 * Demo client class for remote OrderServices, to be invoked as standalone
 * program from the command line, e.g. via "client.bat" or "run.xml".
 *
 * <p>You need to specify an order ID and optionally a number of calls,
 * e.g. for order ID 1000: 'client 1000' for a single call per service or
 * 'client 1000 10' for 10 calls each".
 *
 * <p>Reads in the application context from a "clientContext.xml" file in
 * the VM execution directory, calling all OrderService proxies defined in it.
 * See that file for details.
 *
 * @author Juergen Hoeller
 * @since 26.12.2003
 * @see org.springframework.samples.jpetstore.domain.logic.OrderService
 */
public class OrderServiceClient {

	public static final String CLIENT_CONTEXT_CONFIG_LOCATION = "client/clientContext.xml";


	private final ListableBeanFactory beanFactory;

	public OrderServiceClient(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public void invokeOrderServices(int orderId, int nrOfCalls) {
		StopWatch stopWatch = new StopWatch(nrOfCalls + " OrderService call(s)");
		Map orderServices = this.beanFactory.getBeansOfType(OrderService.class);
		for (Iterator it = orderServices.keySet().iterator(); it.hasNext();) {
			String beanName = (String) it.next();
			OrderService orderService = (OrderService) orderServices.get(beanName);
			System.out.println("Calling OrderService '" + beanName + "' with order ID " + orderId);
			stopWatch.start(beanName);
			Order order = null;
			for (int i = 0; i < nrOfCalls; i++) {
				order = orderService.getOrder(orderId);
			}
			stopWatch.stop();
			if (order != null) {
				printOrder(order);
			}
			else {
				System.out.println("Order with ID " + orderId + " not found");
			}
			System.out.println();
		}
		System.out.println(stopWatch.prettyPrint());
	}

	protected void printOrder(Order order) {
		System.out.println("Got order with order ID " + order.getOrderId() +
				" and order date " + order.getOrderDate());
		System.out.println("Shipping address is: " + order.getShipAddress1());
		for (Iterator lineItems = order.getLineItems().iterator(); lineItems.hasNext();) {
			LineItem lineItem = (LineItem) lineItems.next();
			System.out.println("LineItem " + lineItem.getLineNumber() + ": " + lineItem.getQuantity() +
					" piece(s) of item " + lineItem.getItemId());
		}
	}


	public static void main(String[] args) {
		if (args.length == 0 || "".equals(args[0])) {
			System.out.println(
					"You need to specify an order ID and optionally a number of calls, e.g. for order ID 1000: " +
					"'client 1000' for a single call per service or 'client 1000 10' for 10 calls each");
		}
		else {
			int orderId = Integer.parseInt(args[0]);
			int nrOfCalls = 1;
			if (args.length > 1 && !"".equals(args[1])) {
				nrOfCalls = Integer.parseInt(args[1]);
			}
			ListableBeanFactory beanFactory = new ClassPathXmlApplicationContext(CLIENT_CONTEXT_CONFIG_LOCATION);
			OrderServiceClient client = new OrderServiceClient(beanFactory);
			client.invokeOrderServices(orderId, nrOfCalls);
		}
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Account;

public class NewAccountFormAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm workingAcctForm = new AccountActionForm();
    request.getSession().removeAttribute("workingAccountForm");
    request.getSession().setAttribute("workingAccountForm", workingAcctForm);
    if (workingAcctForm.getAccount() == null) {
      workingAcctForm.setAccount(new Account());
    }
    if (workingAcctForm.getCategories() == null) {
      workingAcctForm.setCategories(getPetStore().getCategoryList());
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Order;

public class ViewOrderAction extends SecureBaseAction {

  protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm acctForm = (AccountActionForm) form;
    int orderId = Integer.parseInt(request.getParameter("orderId"));
    Order order = getPetStore().getOrder(orderId);
    if (acctForm.getAccount().getUsername().equals(order.getUsername())) {
      request.setAttribute("order", order);
      return mapping.findForward("success");
    }
		else {
      request.setAttribute("message", "You may only view your own orders.");
      return mapping.findForward("failure");
    }
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Item;

public class ViewItemAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    String itemId = request.getParameter("itemId");
    Item item = getPetStore().getItem(itemId);
    request.setAttribute("item", item);
    request.setAttribute("product", item.getProduct());
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Cart;
import org.springframework.samples.jpetstore.domain.Item;

public class AddItemToCartAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    CartActionForm cartForm = (CartActionForm) form;
    Cart cart = cartForm.getCart();
    String workingItemId = cartForm.getWorkingItemId();
    if (cart.containsItemId(workingItemId)) {
      cart.incrementQuantityByItemId(workingItemId);
    }
		else {
      // isInStock is a "real-time" property that must be updated
      // every time an item is added to the cart, even if other
      // item details are cached.
      boolean isInStock = getPetStore().isItemInStock(workingItemId);
      Item item = getPetStore().getItem(workingItemId);
      cartForm.getCart().addItem(item, isInStock);
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.ServletContext;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionServlet;

import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Superclass for Struts actions in JPetStore's web tier.
 *
 * <p>Looks up the Spring WebApplicationContext via the ServletContext
 * and obtains the PetStoreFacade implementation from it, making it
 * available to subclasses via a protected getter method.
 *
 * <p>As alternative to such a base class, consider using Spring's
 * ActionSupport class for Struts, which pre-implements
 * WebApplicationContext lookup in a generic fashion.
 *
 * @author Juergen Hoeller
 * @since 30.11.2003
 * @see #getPetStore
 * @see org.springframework.web.context.support.WebApplicationContextUtils#getRequiredWebApplicationContext
 * @see org.springframework.web.struts.ActionSupport
 */
public abstract class BaseAction extends Action {

  private PetStoreFacade petStore;

	public void setServlet(ActionServlet actionServlet) {
		super.setServlet(actionServlet);
		if (actionServlet != null) {
			ServletContext servletContext = actionServlet.getServletContext();
			WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
			this.petStore = (PetStoreFacade) wac.getBean("petStore");
		}
	}

	protected PetStoreFacade getPetStore() {
		return petStore;
	}

}


import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Cart;

public class CartActionForm extends BaseActionForm {

  /* Private Fields */

  private Cart cart = new Cart();
  private String workingItemId;

  /* JavaBeans Properties */

  public Cart getCart() { return cart; }
  public void setCart(Cart cart) { this.cart = cart; }

  public String getWorkingItemId() { return workingItemId; }
  public void setWorkingItemId(String workingItemId) { this.workingItemId = workingItemId; }

  /* Public Methods */

  public void reset(ActionMapping mapping, HttpServletRequest request) {
    super.reset(mapping, request);
    workingItemId = null;
  }
}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Order;

public class NewOrderAction extends SecureBaseAction {

  protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    OrderActionForm orderForm = (OrderActionForm) form;
    if (orderForm.isShippingAddressRequired()) {
      return mapping.findForward("shipping");
    }
		else if (!orderForm.isConfirmed()) {
      return mapping.findForward("confirm");
    }
		else if (orderForm.getOrder() != null) {
      Order order = orderForm.getOrder();
      getPetStore().insertOrder(order);
      request.getSession().removeAttribute("workingOrderForm");
      request.getSession().removeAttribute("cartForm");
      request.setAttribute("order", order);
      request.setAttribute("message", "Thank you, your order has been submitted.");
      return mapping.findForward("success");
    }
		else {
      request.setAttribute("message", "An error occurred processing your order (order was null).");
      return mapping.findForward("failure");
    }
  }

}


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Account;

public class AccountActionForm extends BaseActionForm {

  /* Constants */

  public static final String VALIDATE_EDIT_ACCOUNT = "editAccount";
  public static final String VALIDATE_NEW_ACCOUNT = "newAccount";
  private static final ArrayList LANGUAGE_LIST = new ArrayList();

  /* Private Fields */

  private String username;
  private String password;
  private String repeatedPassword;
  private List languages;
  private List categories;
  private String validate;
  private String forwardAction;
  private Account account;
  private PagedListHolder myList;

  /* Static Initializer */

  static {
    LANGUAGE_LIST.add("english");
    LANGUAGE_LIST.add("japanese");
  }

  /* Constructors */

  public AccountActionForm() {
    languages = LANGUAGE_LIST;
  }

  /* JavaBeans Properties */

  public PagedListHolder getMyList() { return myList; }
  public void setMyList(PagedListHolder myList) { this.myList = myList; }

  public String getForwardAction() { return forwardAction; }
  public void setForwardAction(String forwardAction) { this.forwardAction = forwardAction; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getRepeatedPassword() { return repeatedPassword; }
  public void setRepeatedPassword(String repeatedPassword) { this.repeatedPassword = repeatedPassword; }

  public Account getAccount() { return account; }
  public void setAccount(Account account) { this.account = account; }

  public List getLanguages() { return languages; }
  public void setLanguages(List languages) { this.languages = languages; }

  public List getCategories() { return categories; }
  public void setCategories(List categories) { this.categories = categories; }

  public String getValidate() { return validate; }
  public void setValidate(String validate) { this.validate = validate; }

  /* Public Methods */

  public void doValidate(ActionMapping mapping, HttpServletRequest request, List errors) {
    if (validate != null) {
      if (VALIDATE_EDIT_ACCOUNT.equals(validate) || VALIDATE_NEW_ACCOUNT.equals(validate)) {
        if (VALIDATE_NEW_ACCOUNT.equals(validate)) {
          account.setStatus("OK");
          addErrorIfStringEmpty(errors, "User ID is required.", account.getUsername());
          if (account.getPassword() == null || account.getPassword().length() < 1 || !account.getPassword().equals(repeatedPassword)) {
            errors.add("Passwords did not match or were not provided.  Matching passwords are required.");
          }
        }
        if (account.getPassword() != null && account.getPassword().length() > 0) {
          if (!account.getPassword().equals(repeatedPassword)) {
            errors.add("Passwords did not match.");
          }
        }
        addErrorIfStringEmpty(errors, "First name is required.", this.account.getFirstName());
        addErrorIfStringEmpty(errors, "Last name is required.", this.account.getLastName());
        addErrorIfStringEmpty(errors, "Email address is required.", this.account.getEmail());
        addErrorIfStringEmpty(errors, "Phone number is required.", this.account.getPhone());
        addErrorIfStringEmpty(errors, "Address (1) is required.", this.account.getAddress1());
        addErrorIfStringEmpty(errors, "City is required.", this.account.getCity());
        addErrorIfStringEmpty(errors, "State is required.", this.account.getState());
        addErrorIfStringEmpty(errors, "ZIP is required.", this.account.getZip());
        addErrorIfStringEmpty(errors, "Country is required.", this.account.getCountry());
      }
    }

  }

  public void reset(ActionMapping mapping, HttpServletRequest request) {
    super.reset(mapping, request);
    setUsername(null);
    setPassword(null);
    setRepeatedPassword(null);
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Product;

public class ViewProductAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    String productId = request.getParameter("productId");
    if (productId != null) {
			PagedListHolder itemList = new PagedListHolder(getPetStore().getItemListByProduct(productId));
			itemList.setPageSize(4);
			Product product = getPetStore().getProduct(productId);
      request.getSession().setAttribute("ViewProductAction_itemList", itemList);
			request.getSession().setAttribute("ViewProductAction_product", product);
			request.setAttribute("itemList", itemList);
      request.setAttribute("product", product);
    }
		else {
			PagedListHolder itemList = (PagedListHolder) request.getSession().getAttribute("ViewProductAction_itemList");
			Product product = (Product) request.getSession().getAttribute("ViewProductAction_product");
      String page = request.getParameter("page");
      if ("next".equals(page)) {
        itemList.nextPage();
      }
			else if ("previous".equals(page)) {
        itemList.previousPage();
      }
			request.setAttribute("itemList", itemList);
      request.setAttribute("product", product);
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class DoNothingAction extends BaseAction {

  /* Public Methods */

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    return mapping.findForward("success");
  }
}


import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.CartItem;

public class UpdateCartQuantitiesAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    CartActionForm cartForm = (CartActionForm) form;
    Iterator cartItems = cartForm.getCart().getAllCartItems();
    while (cartItems.hasNext()) {
      CartItem cartItem = (CartItem) cartItems.next();
      String itemId = cartItem.getItem().getItemId();
      try {
        int quantity = Integer.parseInt(request.getParameter(itemId));
        cartForm.getCart().setQuantityByItemId(itemId, quantity);
        if (quantity < 1) {
          cartItems.remove();
        }
      }
			catch (NumberFormatException e) {
        //ignore on purpose
      }
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public abstract class SecureBaseAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm acctForm = (AccountActionForm) request.getSession().getAttribute("accountForm");
    if (acctForm == null || acctForm.getAccount() == null) {
      String url = request.getServletPath();
      String query = request.getQueryString();
      if (query != null) {
        request.setAttribute("signonForwardAction", url+"?"+query);
      }
			else {
        request.setAttribute("signonForwardAction", url);
      }
      return mapping.findForward("global-signon");
    }
		else {
      return doExecute(mapping, form, request, response);
    }
  }

	protected abstract ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception;

}


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class BaseActionForm extends ActionForm {

  /* Public Methods */

  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    ActionErrors actionErrors = null;
    ArrayList errorList = new ArrayList();
    doValidate(mapping, request, errorList);
    request.setAttribute("errors", errorList);
    if (!errorList.isEmpty()) {
      actionErrors = new ActionErrors();
      actionErrors.add(ActionErrors.GLOBAL_ERROR, new ActionError("global.error"));
    }
    return actionErrors;
  }

  public void doValidate(ActionMapping mapping, HttpServletRequest request, List errors) {
  }

  /* Protected Methods */

  protected void addErrorIfStringEmpty(List errors, String message, String value) {
    if (value == null || value.trim().length() < 1) {
      errors.add(message);
    }
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class RemoveItemFromCartAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    CartActionForm cartForm = (CartActionForm) form;
    cartForm.getCart().removeItemById(cartForm.getWorkingItemId());
		return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Account;

public class NewAccountAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm acctForm = (AccountActionForm) form;
		if (AccountActionForm.VALIDATE_NEW_ACCOUNT.equals(acctForm.getValidate())) {
			acctForm.getAccount().setListOption(request.getParameter("account.listOption") != null);
			acctForm.getAccount().setBannerOption(request.getParameter("account.bannerOption") != null);
			Account account = acctForm.getAccount();
			String username = acctForm.getAccount().getUsername();
			getPetStore().insertAccount(account);
			acctForm.setAccount(getPetStore().getAccount(username));
			PagedListHolder myList = new PagedListHolder(getPetStore().getProductListByCategory(account.getFavouriteCategoryId()));
			myList.setPageSize(4);
			acctForm.setMyList(myList);
			request.getSession().setAttribute("accountForm", acctForm);
			request.getSession().removeAttribute("workingAccountForm");
			return mapping.findForward("success");
		}
		else {
			request.setAttribute("message", "Your account was not created because the submitted information was not validated.");
			return mapping.findForward("failure");
		}
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class ListOrdersAction extends SecureBaseAction {

  protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm acctForm = (AccountActionForm) form;
    String username = acctForm.getAccount().getUsername();
    request.setAttribute("orderList", getPetStore().getOrdersByUsername(username));
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Category;

public class ViewCategoryAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    String categoryId = request.getParameter("categoryId");
    if (categoryId != null) {
			Category category = getPetStore().getCategory(categoryId);
      PagedListHolder productList = new PagedListHolder(getPetStore().getProductListByCategory(categoryId));
			productList.setPageSize(4);
			request.getSession().setAttribute("ViewProductAction_category", category);
			request.getSession().setAttribute("ViewProductAction_productList", productList);
			request.setAttribute("category", category);
			request.setAttribute("productList", productList);
    }
		else {
			Category category = (Category) request.getSession().getAttribute("ViewProductAction_category");
			PagedListHolder productList = (PagedListHolder) request.getSession().getAttribute("ViewProductAction_productList");
			if (category == null || productList == null) {
				throw new IllegalStateException("Cannot find pre-loaded category and product list");
			}
      String page = request.getParameter("page");
      if ("next".equals(page)) {
        productList.nextPage();
      }
			else if ("previous".equals(page)) {
        productList.previousPage();
      }
			request.setAttribute("category", category);
			request.setAttribute("productList", productList);
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Account;

public class EditAccountAction extends SecureBaseAction {

  protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm acctForm = (AccountActionForm) form;
		if (AccountActionForm.VALIDATE_EDIT_ACCOUNT.equals(acctForm.getValidate())) {
			acctForm.getAccount().setListOption(request.getParameter("account.listOption") != null);
			acctForm.getAccount().setBannerOption(request.getParameter("account.bannerOption") != null);
			Account account = acctForm.getAccount();
			getPetStore().updateAccount(account);
			acctForm.setAccount(getPetStore().getAccount(account.getUsername()));
			PagedListHolder myList = new PagedListHolder(getPetStore().getProductListByCategory(account.getFavouriteCategoryId()));
			myList.setPageSize(4);
			acctForm.setMyList(myList);
			request.getSession().setAttribute("accountForm", acctForm);
			request.getSession().removeAttribute("workingAccountForm");
			return mapping.findForward("success");
		}
		else {
			request.setAttribute("message", "Your account was not updated because the submitted information was not validated.");
			return mapping.findForward("failure");
		}
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class ViewCartAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    CartActionForm cartForm = (CartActionForm) form;
    AccountActionForm acctForm = (AccountActionForm) request.getSession().getAttribute("accountForm");
    String page = request.getParameter("page");
    if (acctForm != null && acctForm.getAccount() != null) {
      if ("next".equals(page)) {
        acctForm.getMyList().nextPage();
      }
			else if ("previous".equals(page)) {
        acctForm.getMyList().previousPage();
      }
    }
    if ("nextCart".equals(page)) {
      cartForm.getCart().getCartItemList().nextPage();
    }
		else if ("previousCart".equals(page)) {
      cartForm.getCart().getCartItemList().previousPage();
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Account;

public class NewOrderFormAction extends SecureBaseAction {

  protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm acctForm = (AccountActionForm) request.getSession().getAttribute("accountForm");
    CartActionForm cartForm = (CartActionForm) request.getSession().getAttribute("cartForm");
    if (cartForm != null) {
      OrderActionForm orderForm = (OrderActionForm) form;
      // Re-read account from DB at team's request.
      Account account = getPetStore().getAccount(acctForm.getAccount().getUsername());
      orderForm.getOrder().initOrder(account, cartForm.getCart());
      return mapping.findForward("success");
    }
		else {
      request.setAttribute("message", "An order could not be created because a cart could not be found.");
      return mapping.findForward("failure");
    }
  }

}

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.util.StringUtils;

public class SearchProductsAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String keyword = request.getParameter("keyword");
		if (keyword != null) {
			if (!StringUtils.hasLength(keyword)) {
				request.setAttribute("message", "Please enter a keyword to search for, then press the search button.");
				return mapping.findForward("failure");
			}
			PagedListHolder productList = new PagedListHolder(getPetStore().searchProductList(keyword.toLowerCase()));
			productList.setPageSize(4);
			request.getSession().setAttribute("SearchProductsAction_productList", productList);
			request.setAttribute("productList", productList);
			return mapping.findForward("success");
		}
		else {
      String page = request.getParameter("page");
      PagedListHolder productList = (PagedListHolder) request.getSession().getAttribute("SearchProductsAction_productList");
			if (productList == null) {
				request.setAttribute("message", "Your session has timed out. Please start over again.");
				return mapping.findForward("failure");
			}
			if ("next".equals(page)) {
				productList.nextPage();
			}
			else if ("previous".equals(page)) {
				productList.previousPage();
			}
			request.setAttribute("productList", productList);
			return mapping.findForward("success");
    }
  }

}


import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Order;

public class OrderActionForm extends BaseActionForm {

  /* Constants */

  private static final List CARD_TYPE_LIST = new ArrayList();

  /* Private Fields */

  private Order order;
  private boolean shippingAddressRequired;
  private boolean confirmed;
  private List cardTypeList;

  /* Static Initializer */

  static {
    CARD_TYPE_LIST.add("Visa");
    CARD_TYPE_LIST.add("MasterCard");
    CARD_TYPE_LIST.add("American Express");
  }

  /* Constructors */

  public OrderActionForm() {
    this.order = new Order();
    this.shippingAddressRequired = false;
    this.cardTypeList = CARD_TYPE_LIST;
    this.confirmed = false;
  }

  /* JavaBeans Properties */

  public boolean isConfirmed() { return confirmed; }
  public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

  public Order getOrder() { return order; }
  public void setOrder(Order order) { this.order = order; }

  public boolean isShippingAddressRequired() { return shippingAddressRequired; }
  public void setShippingAddressRequired(boolean shippingAddressRequired) { this.shippingAddressRequired = shippingAddressRequired; }

  public List getCreditCardTypes() { return cardTypeList; }

  /* Public Methods */

  public void doValidate(ActionMapping mapping, HttpServletRequest request, List errors) {

    if (!this.isShippingAddressRequired()) {
      addErrorIfStringEmpty(errors, "FAKE (!) credit card number required.", order.getCreditCard());
      addErrorIfStringEmpty(errors, "Expiry date is required.", order.getExpiryDate());
      addErrorIfStringEmpty(errors, "Card type is required.", order.getCardType());

      addErrorIfStringEmpty(errors, "Shipping Info: first name is required.", order.getShipToFirstName());
      addErrorIfStringEmpty(errors, "Shipping Info: last name is required.", order.getShipToLastName());
      addErrorIfStringEmpty(errors, "Shipping Info: address is required.", order.getShipAddress1());
      addErrorIfStringEmpty(errors, "Shipping Info: city is required.", order.getShipCity());
      addErrorIfStringEmpty(errors, "Shipping Info: state is required.", order.getShipState());
      addErrorIfStringEmpty(errors, "Shipping Info: zip/postal code is required.", order.getShipZip());
      addErrorIfStringEmpty(errors, "Shipping Info: country is required.", order.getShipCountry());

      addErrorIfStringEmpty(errors, "Billing Info: first name is required.", order.getBillToFirstName());
      addErrorIfStringEmpty(errors, "Billing Info: last name is required.", order.getBillToLastName());
      addErrorIfStringEmpty(errors, "Billing Info: address is required.", order.getBillAddress1());
      addErrorIfStringEmpty(errors, "Billing Info: city is required.", order.getBillCity());
      addErrorIfStringEmpty(errors, "Billing Info: state is required.", order.getBillState());
      addErrorIfStringEmpty(errors, "Billing Info: zip/postal code is required.", order.getBillZip());
      addErrorIfStringEmpty(errors, "Billing Info: country is required.", order.getBillCountry());
    }

    if (errors.size() > 0) {
      order.setBillAddress1(order.getShipAddress1());
      order.setBillAddress2(order.getShipAddress2());
      order.setBillToFirstName(order.getShipToFirstName());
      order.setBillToLastName(order.getShipToLastName());
      order.setBillCity(order.getShipCity());
      order.setBillCountry(order.getShipCountry());
      order.setBillState(order.getShipState());
      order.setBillZip(order.getShipZip());
    }

  }

  public void reset(ActionMapping mapping, HttpServletRequest request) {
    super.reset(mapping, request);
    shippingAddressRequired = false;
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Account;

public class SignonAction extends BaseAction {

  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    request.getSession().removeAttribute("workingAccountForm");
    request.getSession().removeAttribute("accountForm");
    if (request.getParameter("signoff") != null) {
      request.getSession().invalidate();
      return mapping.findForward("success");
    }
		else {
      AccountActionForm acctForm = (AccountActionForm) form;
      String username = acctForm.getUsername();
      String password = acctForm.getPassword();
      Account account = getPetStore().getAccount(username, password);
      if (account == null) {
        request.setAttribute("message", "Invalid username or password.  Signon failed.");
        return mapping.findForward("failure");
      }
			else {
				String forwardAction = acctForm.getForwardAction();
				acctForm = new AccountActionForm();
				acctForm.setForwardAction(forwardAction);
        acctForm.setAccount(account);
        acctForm.getAccount().setPassword(null);
        PagedListHolder myList = new PagedListHolder(getPetStore().getProductListByCategory(account.getFavouriteCategoryId()));
				myList.setPageSize(4);
				acctForm.setMyList(myList);
				request.getSession().setAttribute("accountForm", acctForm);
        if (acctForm.getForwardAction() == null || acctForm.getForwardAction().length() < 1) {
          return mapping.findForward("success");
        }
				else {
          response.sendRedirect(acctForm.getForwardAction());
          return null;
        }
      }
    }
  }

}


import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.samples.jpetstore.domain.Account;

public class EditAccountFormAction extends SecureBaseAction {

  protected ActionForward doExecute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
    AccountActionForm workingAcctForm = (AccountActionForm) form;
    AccountActionForm acctForm = (AccountActionForm) request.getSession().getAttribute("accountForm");
    String username = acctForm.getAccount().getUsername();
    if (workingAcctForm.getAccount() == null) {
      Account account = getPetStore().getAccount(username);
      workingAcctForm.setAccount(account);
    }
    if (workingAcctForm.getCategories() == null) {
      List categories = getPetStore().getCategoryList();
      workingAcctForm.setCategories(categories);
    }
    return mapping.findForward("success");
  }

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class SearchProductsController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String keyword = request.getParameter("keyword");
		if (keyword != null) {
			if (!StringUtils.hasLength(keyword)) {
				return new ModelAndView("Error", "message", "Please enter a keyword to search for, then press the search button.");
			}
			PagedListHolder productList = new PagedListHolder(this.petStore.searchProductList(keyword.toLowerCase()));
			productList.setPageSize(4);
			request.getSession().setAttribute("SearchProductsController_productList", productList);
			return new ModelAndView("SearchProducts", "productList", productList);
		}
		else {
			String page = request.getParameter("page");
			PagedListHolder productList = (PagedListHolder) request.getSession().getAttribute("SearchProductsController_productList");
			if (productList == null) {
				return new ModelAndView("Error", "message", "Your session has timed out. Please start over again.");
			}
			if ("next".equals(page)) {
				productList.nextPage();
			}
			else if ("previous".equals(page)) {
				productList.previousPage();
			}
			return new ModelAndView("SearchProducts", "productList", productList);
		}
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class SignonController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		Account account = this.petStore.getAccount(username, password);
		if (account == null) {
			return new ModelAndView("Error", "message", "Invalid username or password.  Signon failed.");
		}
		else {
			UserSession userSession = new UserSession(account);
			PagedListHolder myList = new PagedListHolder(this.petStore.getProductListByCategory(account.getFavouriteCategoryId()));
			myList.setPageSize(4);
			userSession.setMyList(myList);
			request.getSession().setAttribute("userSession", userSession);
			String forwardAction = request.getParameter("forwardAction");
			if (forwardAction != null) {
				response.sendRedirect(forwardAction);
				return null;
			}
			else {
				return new ModelAndView("index");
			}
		}
	}

}


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.validation.BindException;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class AccountFormController extends SimpleFormController {

	public static final String[] LANGUAGES = {"english", "japanese"};

	private PetStoreFacade petStore;

	public AccountFormController() {
		setSessionForm(true);
		setValidateOnBinding(false);
		setCommandName("accountForm");
		setFormView("EditAccountForm");
	}

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		UserSession userSession = (UserSession) WebUtils.getSessionAttribute(request, "userSession");
		if (userSession != null) {
			return new AccountForm(this.petStore.getAccount(userSession.getAccount().getUsername()));
		}
		else {
			return new AccountForm();
		}
	}

	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors)
			throws Exception {

		AccountForm accountForm = (AccountForm) command;
		Account account = accountForm.getAccount();

		if (request.getParameter("account.listOption") == null) {
			account.setListOption(false);
		}
		if (request.getParameter("account.bannerOption") == null) {
			account.setBannerOption(false);
		}

		errors.setNestedPath("account");
		getValidator().validate(account, errors);
		errors.setNestedPath("");

		if (accountForm.isNewAccount()) {
			account.setStatus("OK");
			ValidationUtils.rejectIfEmpty(errors, "account.username", "USER_ID_REQUIRED", "User ID is required.");
			if (account.getPassword() == null || account.getPassword().length() < 1 ||
					!account.getPassword().equals(accountForm.getRepeatedPassword())) {
			 errors.reject("PASSWORD_MISMATCH",
					 "Passwords did not match or were not provided. Matching passwords are required.");
			}
		}
		else if (account.getPassword() != null && account.getPassword().length() > 0) {
		  if (!account.getPassword().equals(accountForm.getRepeatedPassword())) {
				errors.reject("PASSWORD_MISMATCH",
						"Passwords did not match. Matching passwords are required.");
		  }
	  }
 	}

	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map model = new HashMap();
		model.put("languages", LANGUAGES);
		model.put("categories", this.petStore.getCategoryList());
		return model;
	}

	protected ModelAndView onSubmit(
			HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		AccountForm accountForm = (AccountForm) command;
		try {
			if (accountForm.isNewAccount()) {
				this.petStore.insertAccount(accountForm.getAccount());
			}
			else {
				this.petStore.updateAccount(accountForm.getAccount());
			}
		}
		catch (DataIntegrityViolationException ex) {
			errors.rejectValue("account.username", "USER_ID_ALREADY_EXISTS",
					"User ID already exists: choose a different ID.");
			return showForm(request, response, errors);
		}
		
		UserSession userSession = new UserSession(this.petStore.getAccount(accountForm.getAccount().getUsername()));
		PagedListHolder myList = new PagedListHolder(
				this.petStore.getProductListByCategory(accountForm.getAccount().getFavouriteCategoryId()));
		myList.setPageSize(4);
		userSession.setMyList(myList);
		request.getSession().setAttribute("userSession", userSession);
		return super.onSubmit(request, response, command, errors);
	}

}


import java.io.Serializable;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Account;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class UserSession implements Serializable {

	private Account account;

	private PagedListHolder myList;

	public UserSession(Account account) {
		this.account = account;
	}

	public Account getAccount() {
		return account;
	}

	public void setMyList(PagedListHolder myList) {
		this.myList = myList;
	}

	public PagedListHolder getMyList() {
		return myList;
	}

}


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Category;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class ViewCategoryController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map model = new HashMap();
		String categoryId = request.getParameter("categoryId");
		if (categoryId != null) {
			Category category = this.petStore.getCategory(categoryId);
			PagedListHolder productList = new PagedListHolder(this.petStore.getProductListByCategory(categoryId));
			productList.setPageSize(4);
			request.getSession().setAttribute("ViewProductAction_category", category);
			request.getSession().setAttribute("ViewProductAction_productList", productList);
			model.put("category", category);
			model.put("productList", productList);
		}
		else {
			Category category = (Category) request.getSession().getAttribute("ViewProductAction_category");
			PagedListHolder productList = (PagedListHolder) request.getSession().getAttribute("ViewProductAction_productList");
			if (category == null || productList == null) {
				throw new IllegalStateException("Cannot find pre-loaded category and product list");
			}
			String page = request.getParameter("page");
			if ("next".equals(page)) {
				productList.nextPage();
			}
			else if ("previous".equals(page)) {
				productList.previousPage();
			}
			model.put("category", category);
			model.put("productList", productList);
		}
		return new ModelAndView("Category", model);
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class SignonInterceptor extends HandlerInterceptorAdapter {

	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		UserSession userSession = (UserSession) WebUtils.getSessionAttribute(request, "userSession");
		if (userSession == null) {
			String url = request.getServletPath();
			String query = request.getQueryString();
			ModelAndView modelAndView = new ModelAndView("SignonForm");
			if (query != null) {
				modelAndView.addObject("signonForwardAction", url+"?"+query);
			}
			else {
				modelAndView.addObject("signonForwardAction", url);
			}
			throw new ModelAndViewDefiningException(modelAndView);
		}
		else {
			return true;
		}
	}

}


import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Cart;
import org.springframework.samples.jpetstore.domain.CartItem;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class UpdateCartQuantitiesController implements Controller {

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Cart cart = (Cart) WebUtils.getOrCreateSessionAttribute(request.getSession(), "sessionCart", Cart.class);
		Iterator cartItems = cart.getAllCartItems();
		while (cartItems.hasNext()) {
			CartItem cartItem = (CartItem) cartItems.next();
			String itemId = cartItem.getItem().getItemId();
			try {
				int quantity = Integer.parseInt(request.getParameter(itemId));
				cart.setQuantityByItemId(itemId, quantity);
				if (quantity < 1) {
					cartItems.remove();
				}
			}
			catch (NumberFormatException ex) {
				// ignore on purpose
			}
		}
		request.getSession().setAttribute("sessionCart", cart);
		return new ModelAndView("Cart", "cart", cart);
	}

}


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.support.PagedListHolder;
import org.springframework.samples.jpetstore.domain.Product;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class ViewProductController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map model = new HashMap();
		String productId = request.getParameter("productId");
		if (productId != null) {
			PagedListHolder itemList = new PagedListHolder(this.petStore.getItemListByProduct(productId));
			itemList.setPageSize(4);
			Product product = this.petStore.getProduct(productId);
			request.getSession().setAttribute("ViewProductAction_itemList", itemList);
			request.getSession().setAttribute("ViewProductAction_product", product);
			model.put("itemList", itemList);
			model.put("product", product);
		}
		else {
			PagedListHolder itemList = (PagedListHolder) request.getSession().getAttribute("ViewProductAction_itemList");
			Product product = (Product) request.getSession().getAttribute("ViewProductAction_product");
			String page = request.getParameter("page");
			if ("next".equals(page)) {
				itemList.nextPage();
			}
			else if ("previous".equals(page)) {
				itemList.previousPage();
			}
			model.put("itemList", itemList);
			model.put("product", product);
		}
		return new ModelAndView("Product", model);
	}

}


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Account;
import org.springframework.samples.jpetstore.domain.Cart;
import org.springframework.samples.jpetstore.domain.logic.OrderValidator;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.mvc.AbstractWizardFormController;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class OrderFormController extends AbstractWizardFormController {

	private PetStoreFacade petStore;

	public OrderFormController() {
		setCommandName("orderForm");
		setPages(new String[] {"NewOrderForm", "ShippingForm", "ConfirmOrder"});
	}

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	protected Object formBackingObject(HttpServletRequest request) throws ModelAndViewDefiningException {
		UserSession userSession = (UserSession) request.getSession().getAttribute("userSession");
		Cart cart = (Cart) request.getSession().getAttribute("sessionCart");
		if (cart != null) {
			// Re-read account from DB at team's request.
			Account account = this.petStore.getAccount(userSession.getAccount().getUsername());
			OrderForm orderForm = new OrderForm();
			orderForm.getOrder().initOrder(account, cart);
			return orderForm;
		}
		else {
			ModelAndView modelAndView = new ModelAndView("Error");
			modelAndView.addObject("message", "An order could not be created because a cart could not be found.");
			throw new ModelAndViewDefiningException(modelAndView);
		}
	}

	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors, int page) {
		if (page == 0 && request.getParameter("shippingAddressRequired") == null) {
			OrderForm orderForm = (OrderForm) command;
			orderForm.setShippingAddressRequired(false);
		}
	}

	protected Map referenceData(HttpServletRequest request, int page) {
		if (page == 0) {
			List creditCardTypes = new ArrayList();
			creditCardTypes.add("Visa");
			creditCardTypes.add("MasterCard");
			creditCardTypes.add("American Express");
			Map model = new HashMap();
			model.put("creditCardTypes", creditCardTypes);
			return model;
		}
		return null;
	}

	protected int getTargetPage(HttpServletRequest request, Object command, Errors errors, int currentPage) {
		OrderForm orderForm = (OrderForm) command;
		if (currentPage == 0 && orderForm.isShippingAddressRequired()) {
			return 1;
		}
		else {
			return 2;
		}
	}

	protected void validatePage(Object command, Errors errors, int page) {
		OrderForm orderForm = (OrderForm) command;
		OrderValidator orderValidator = (OrderValidator) getValidator();
		errors.setNestedPath("order");
		switch (page) {
			case 0:
				orderValidator.validateCreditCard(orderForm.getOrder(), errors);
				orderValidator.validateBillingAddress(orderForm.getOrder(), errors);
				break;
			case 1:
				orderValidator.validateShippingAddress(orderForm.getOrder(), errors);
		}
		errors.setNestedPath("");
	}

	protected ModelAndView processFinish(
			HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) {
		OrderForm orderForm = (OrderForm) command;
		this.petStore.insertOrder(orderForm.getOrder());
		request.getSession().removeAttribute("sessionCart");
		Map model = new HashMap();
		model.put("order", orderForm.getOrder());
		model.put("message", "Thank you, your order has been submitted.");
		return new ModelAndView("ViewOrder", model);
	}

}


import java.io.Serializable;

import org.springframework.samples.jpetstore.domain.Order;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class OrderForm implements Serializable {

	private final Order order = new Order();

	private boolean shippingAddressRequired;

	private boolean confirmed;

	public Order getOrder() {
		return order;
	}

	public void setShippingAddressRequired(boolean shippingAddressRequired) {
		this.shippingAddressRequired = shippingAddressRequired;
	}

	public boolean isShippingAddressRequired() {
		return shippingAddressRequired;
	}

	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Cart;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class ViewCartController implements Controller {

	private String successView;

	public void setSuccessView(String successView) {
		this.successView = successView;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		UserSession userSession = (UserSession) WebUtils.getSessionAttribute(request, "userSession");
		Cart cart = (Cart) WebUtils.getOrCreateSessionAttribute(request.getSession(), "sessionCart", Cart.class);
		String page = request.getParameter("page");
		if (userSession != null) {
			if ("next".equals(page)) {
				userSession.getMyList().nextPage();
			}
			else if ("previous".equals(page)) {
				userSession.getMyList().previousPage();
			}
		}
		if ("nextCart".equals(page)) {
			cart.getCartItemList().nextPage();
		}
		else if ("previousCart".equals(page)) {
			cart.getCartItemList().previousPage();
		}
		return new ModelAndView(this.successView, "cart", cart);
	}

}


import java.io.Serializable;

import org.springframework.samples.jpetstore.domain.Account;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class AccountForm implements Serializable {

	private Account account;

	private boolean newAccount;

	private String repeatedPassword;

	public AccountForm(Account account) {
		this.account = account;
		this.newAccount = false;
	}

	public AccountForm() {
		this.account = new Account();
		this.newAccount = true;
	}

	public Account getAccount() {
		return account;
	}

	public boolean isNewAccount() {
		return newAccount;
	}

	public void setRepeatedPassword(String repeatedPassword) {
		this.repeatedPassword = repeatedPassword;
	}

	public String getRepeatedPassword() {
		return repeatedPassword;
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class SignoffController implements Controller {

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		request.getSession().removeAttribute("userSession");
		request.getSession().invalidate();
		return new ModelAndView("index");
	}

}


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class ListOrdersController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		UserSession userSession = (UserSession) WebUtils.getRequiredSessionAttribute(request, "userSession");
		String username = userSession.getAccount().getUsername();
		Map model = new HashMap();
		model.put("orderList", this.petStore.getOrdersByUsername(username));
		return new ModelAndView("ListOrders", model);
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Cart;
import org.springframework.samples.jpetstore.domain.Item;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class AddItemToCartController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Cart cart = (Cart) WebUtils.getOrCreateSessionAttribute(request.getSession(), "sessionCart", Cart.class);
		String workingItemId = request.getParameter("workingItemId");
		if (cart.containsItemId(workingItemId)) {
			cart.incrementQuantityByItemId(workingItemId);
		}
		else {
			// isInStock is a "real-time" property that must be updated
			// every time an item is added to the cart, even if other
			// item details are cached.
			boolean isInStock = this.petStore.isItemInStock(workingItemId);
			Item item = this.petStore.getItem(workingItemId);
			cart.addItem(item, isInStock);
		}
		return new ModelAndView("Cart", "cart", cart);
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Order;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 01.12.2003
 */
public class ViewOrderController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		UserSession userSession = (UserSession) WebUtils.getRequiredSessionAttribute(request, "userSession");
		int orderId = Integer.parseInt(request.getParameter("orderId"));
		Order order = this.petStore.getOrder(orderId);
		if (userSession.getAccount().getUsername().equals(order.getUsername())) {
			return new ModelAndView("ViewOrder", "order", order);
		}
		else {
			return new ModelAndView("Error", "message", "You may only view your own orders.");
		}
	}

}


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Item;
import org.springframework.samples.jpetstore.domain.logic.PetStoreFacade;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class ViewItemController implements Controller {

	private PetStoreFacade petStore;

	public void setPetStore(PetStoreFacade petStore) {
		this.petStore = petStore;
	}

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String itemId = request.getParameter("itemId");
		Item item = this.petStore.getItem(itemId);
		Map model = new HashMap();
		model.put("item", item);
		model.put("product", item.getProduct());
		return new ModelAndView("Item", model);
	}

}


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.samples.jpetstore.domain.Cart;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.util.WebUtils;

/**
 * @author Juergen Hoeller
 * @since 30.11.2003
 */
public class RemoveItemFromCartController implements Controller {

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Cart cart = (Cart) WebUtils.getOrCreateSessionAttribute(request.getSession(), "sessionCart", Cart.class);
		cart.removeItemById(request.getParameter("workingItemId"));
		return new ModelAndView("Cart", "cart", cart);
	}

}

