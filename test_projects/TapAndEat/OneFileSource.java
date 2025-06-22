
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.github.joffryferrater.itemservice.models.Item;
import com.github.joffryferrater.itemservice.models.ItemRepository;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ItemServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ItemServiceApplication.class, args);
	}
	
	@Autowired
	ItemRepository itemRepository;
	
	@PostConstruct
	public void init() {
		List<Item> items = new ArrayList<Item>();
		Item item1 = new Item("BUR01", "Burger");
		Item item2 = new Item("SPAG01", "Spaghetti");
		Item item3 = new Item("CHI01", "Crispy Chicken");
		Item item4 = new Item("BEEF01", "Beef Stick");
		Item item5 = new Item("BBQ01", "Pork Barbecue");
		Item item6 = new Item("BBQ02", "Chicken Barbecue");
		Item item7 = new Item("SFT01", "Nestea");
		Item item8 = new Item("SFT02", "Coco Cola");
		Item item9 = new Item("DES01", "Sandy Ice Cream");
		items.add(item1);
		items.add(item2);
		items.add(item3);
		items.add(item4);
		items.add(item5);
		items.add(item6);
		items.add(item7);
		items.add(item8);
		items.add(item9);
		items.forEach(item -> itemRepository.save(item));
	}
}



import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@RepositoryRestResource(path="items", collectionResourceRel="items")
public interface ItemRepository extends CrudRepository<Item, Long>{

	Item findByItemCode(@Param("itemCode")String itemCode);
	
	List<Item> findByName(@Param("name") String name);
}


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Entity
@Table(name = "items")
@Data
public class Item {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	@JsonProperty("Item Code")
	private String itemCode;
	@JsonProperty("Name")
	private String name;
	
	public Item() {
		super();
	}
	
	public Item(String itemCode, String name) {
		this();
		this.itemCode = itemCode;
		this.name = name;
	}
	
}




import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.github.joffryferrater.accountservice.models.Account;
import com.github.joffryferrater.accountservice.models.AccountRepository;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AccountServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountServiceApplication.class, args);
	}
	
	@Autowired
	AccountRepository accountRepo;
	
	@PostConstruct
	public void init() {
		Account account = new Account("joffry.ferrater@gmail.com", "Joffry", "Ferrater",
				"123", "+46", "07694540723");
		accountRepo.save(account);
	}
	
}


import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path="accounts", collectionResourceRel="accounts")
public interface AccountRepository extends CrudRepository<Account, Long>{

	Account findByEmail(@Param("email") String email);
	
	Account findByFirstName(@Param("firstName") String firstName);
	
	Account findByLastName(@Param("lastName") String lastName);


}

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Entity
@Table(name = "users")
@Data
@RequiredArgsConstructor
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@NotNull
	@JsonProperty("Email")
	private final String email;
	@JsonProperty("First Name")
	private final String firstName;
	@JsonProperty("Last Name")
	private final String lastName;
	@JsonProperty("Country Code")
	private final String countryCode;
	@JsonProperty("Phone Number")
	private final String phoneNumber;

	@JsonIgnore
	private final String password;


}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CustomerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceApplication.class, args);
	}
}


import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Transactional
@RepositoryRestResource(path="customers", collectionResourceRel="customers")
public interface CustomerRepository extends CrudRepository<Customer, Long>{

	Customer findById(Long id);
	
}


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Entity
@Table(name="customers")
@Data
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@JsonProperty("First Name")
	private String firstName;
	@JsonProperty("Last Name")
	private String lastName;
	@JsonProperty("Email")
	private String email;
	@JsonProperty("Phone")
	private String phone;
	@JsonProperty("Address Line 1")
	private String addressLine1;
	@JsonProperty("Address Line 2")
	private String addressLine2;
	@JsonProperty("City")
	private String city;
	@JsonProperty("State")
	private String state;
	@JsonProperty("Postal Code")
	private String postalCode;
	@JsonProperty("Country")
	private String country;
	
	public Customer() {
		super();
	}


}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServiceApplication.class, args);
	}
}


import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import com.github.joffryferrater.priceservice.models.Price;
import com.github.joffryferrater.priceservice.models.PriceRepository;

@SpringBootApplication
@EnableDiscoveryClient
public class PriceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceServiceApplication.class, args);
	}
	
	@Autowired
	PriceRepository priceRepo;
	
	@PostConstruct
	public void init() {
		Price price = new Price("BUR01", "150");
		priceRepo.save(price);
	}
}


import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Joffry Ferrater
 * 
 */
@RepositoryRestResource (path="prices",  collectionResourceRel= "prices")
public interface PriceRepository extends CrudRepository<Price, Long> {

	Price findByItemCode(@Param("itemCode")String itemCode);
	
}


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Entity
@Table (name="prices")
@Data
public class Price {

	@Id
	@GeneratedValue (strategy = GenerationType.AUTO)
	private Long id;
	@JsonProperty("Item Code")
	private String itemCode;
	@JsonProperty("Price")
	private String price;

	public Price() {
		super();
	}
	
	public Price(String itemCode, String price) {
		this();
		this.itemCode = itemCode;
		this.price = price;
	}
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServiceApplication.class, args);
	}
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
public class StoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoreServiceApplication.class, args);
	}
}


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Entity
@Table(name="stores")
public class Store {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private Long  id;
	@JsonProperty("Store")
	private String storeName;
	@JsonProperty("Branch")
	private String branchName;
	@JsonProperty("Location")
	private String location;
	// TO DO: Implement store type
	
	public Store() {
		super();
	}
	
	public Store(String storeName, String branchName, String location) {
		this();
		this.storeName = storeName;
		this.branchName = branchName;
		this.location = location;
	}
	

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getStoreName() {
		return storeName;
	}
	
	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}
	
	public String getBranchName() {
		return branchName;
	}
	
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	
}


import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@Transactional
@RepositoryRestResource(path="stores", collectionResourceRel="stores")
public interface StoreRepository extends CrudRepository<Store, Long>{

	Store findByStoreName(String storeName);
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.context.annotation.ComponentScan;

import com.github.joffryferrater.foodtrayservice.repository.ItemServiceRepository;
import com.github.joffryferrater.foodtrayservice.repository.PriceServiceRepository;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableHystrixDashboard
@EnableHystrix
//@EnableFeignClients
@EnableFeignClients(basePackageClasses = {ItemServiceRepository.class, PriceServiceRepository.class})
@ComponentScan(basePackageClasses = {ItemServiceRepository.class,PriceServiceRepository.class})
public class FoodTrayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodTrayServiceApplication.class, args);
	}
}



import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.joffryferrater.foodtrayservice.domain.Item;
import com.github.joffryferrater.foodtrayservice.domain.Price;
import com.github.joffryferrater.foodtrayservice.domain.TrayItem;
import com.github.joffryferrater.foodtrayservice.repository.ItemServiceRepository;
import com.github.joffryferrater.foodtrayservice.repository.PriceServiceRepository;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@RestController
@RequestMapping("/foodtrays")
public class FoodTrayController {

	@Autowired
	ItemServiceRepository itemServiceRepository;
	@Autowired
	PriceServiceRepository priceServiceRepo;
	
	private List<TrayItem> trayItems = new ArrayList<TrayItem>();
	
	@RequestMapping(value="/price/{itemCode}", method=RequestMethod.GET)
	public Price getPrice(@PathVariable("itemCode") String itemCode) {
		return priceServiceRepo.findByItemCode(itemCode);
	}
	
	@RequestMapping(value="/item/{itemCode}", method=RequestMethod.GET)
	public Item getItem(@PathVariable("itemCode") String itemCode) {
		return itemServiceRepository.findByItemCode(itemCode);
	}
	
	@RequestMapping(value="/{itemCode}", method=RequestMethod.GET)
	public TrayItem getTrayItem(@PathVariable("itemCode") String itemCode) {
		Item item = itemServiceRepository.findByItemCode(itemCode);
		Price price = priceServiceRepo.findByItemCode(itemCode);
		TrayItem trayItem = new TrayItem(item.getName(), price.getPrice());
		return trayItem;
	}
	
	@RequestMapping(value={"/", ""}, method=RequestMethod.POST)
	public List<TrayItem> addFoodTrayItem(TrayItem trayItem) {
		trayItems.add(trayItem);
		return trayItems;
	}
	
	@RequestMapping(value={"/", ""}, method=RequestMethod.GET)
	public List<TrayItem> getItems() {
		return trayItems;
	}
	
}


import org.springframework.stereotype.Service;

@Service
public class TrayItemService {

	
}


import org.springframework.stereotype.Component;

import com.github.joffryferrater.foodtrayservice.domain.Item;

/**
 * Fall back function if ItemService is not available.
 * 
 * @author Joffry Ferrater
 *
 */
@Component
public class ItemServiceFallback implements ItemServiceRepository {

	@Override
	public Item findByItemCode(String itemCode) {
		return new Item("", "");
	}

}


import com.github.joffryferrater.foodtrayservice.domain.Price;

/**
 * Fall back function if price service is not available.
 * 
 * @author Joffry Ferrater
 *
 */
public class PriceServiceFallback implements PriceServiceRepository {

	@Override
	public Price findByItemCode(String itemCode) {
		return new Price("", "");
	}

}


import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.github.joffryferrater.foodtrayservice.domain.Item;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@FeignClient(value="ITEM-SERVICE", fallback=ItemServiceFallback.class)
public interface ItemServiceRepository {

	@RequestMapping(method=RequestMethod.GET, value="/items/search/findByItemCode?itemCode={itemCode}")
	Item findByItemCode(@PathVariable("itemCode") String itemCode);
}


import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.github.joffryferrater.foodtrayservice.domain.Price;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@FeignClient(value="PRICE-SERVICE", fallback=PriceServiceRepository.class)
public interface PriceServiceRepository {

	@RequestMapping(method=RequestMethod.GET, value="/prices/search/findByItemCode?itemCode={itemCode}")
	Price findByItemCode(@PathVariable("itemCode") String itemCode);
}



import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author Joffry Ferrater
 *
 */
public class TrayItem {

	@JsonProperty("Name")
	private String name;
	@JsonProperty("Price")
	private String amount;
	

	public TrayItem() {
		super();
	}
	
	public TrayItem(String name, String amount) {
		this.name = name;
		this.amount = amount;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAmount() {
		return amount;
	}
	
}


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author Joffry Ferrater
 *
 */
@lombok.Getter
public class Price {

	@JsonProperty("Item Code")
	private String itemCode;
	@JsonProperty("Price")
	private String price;
	
	public Price(){
		super();
	}
	
	public Price(String itemCode, String price) {
		this();
		this.itemCode = itemCode;
		this.price = price;
	}

}


import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * 
 * @author Joffry Ferrater
 *
 */
@lombok.Getter
public class Item {

	@JsonProperty("Item Code")
	private String itemCode;
	@JsonProperty("Name")
	private String name;
	
	public Item() {
		super();
	}
	
	public Item(String itemCode, String name) {
		this.itemCode = itemCode;
		this.name = name;
	}
}

