
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@ComponentScan
@EnableAutoConfiguration
@EnableDiscoveryClient
@Component
public class CustomerApp {

	private final CustomerRepository customerRepository;

	@Autowired
	public CustomerApp(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@PostConstruct
	public void generateTestData() {
		customerRepository.save(new Customer("Eberhard", "Wolff",
				"eberhard.wolff@gmail.com", "Unter den Linden", "Berlin"));
		customerRepository.save(new Customer("Rod", "Johnson",
				"rod@somewhere.com", "Market Street", "San Francisco"));
	}

	public static void main(String[] args) {
		SpringApplication.run(CustomerApp.class, args);
	}

}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

@Configuration
class SpringRestDataConfig extends RepositoryRestConfigurerAdapter {

	@Bean
	public RepositoryRestConfigurer repositoryRestConfigurer() {

		return new RepositoryRestConfigurerAdapter() {
			@Override
			public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
				config.exposeIdsFor(Customer.class);
			}
		};
	}

}

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "customer", path = "customer")
public interface CustomerRepository extends
		PagingAndSortingRepository<Customer, Long> {

	List<Customer> findByName(@Param("name") String name);

}


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
public class Customer {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String firstname;

	@Column(nullable = false)
	@Email
	private String email;

	@Column(nullable = false)
	private String street;

	@Column(nullable = false)
	private String city;

	public Customer() {
		super();
		id = 0l;
	}

	public Customer(String firstname, String name, String email, String street,
			String city) {
		super();
		this.name = name;
		this.firstname = firstname;
		this.email = email;
		this.street = street;
		this.city = city;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);

	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

}


import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.ewolff.microservice.customer.Customer;
import com.ewolff.microservice.customer.CustomerRepository;

@Controller
public class CustomerController {

	private CustomerRepository customerRepository;

	@Autowired
	public CustomerController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
	}

	@RequestMapping(value = "/{id}.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public ModelAndView customer(@PathVariable("id") long id) {
		return new ModelAndView("customer", "customer",
				customerRepository.findById(id).get());
	}

	@RequestMapping("/list.html")
	public ModelAndView customerList() {
		return new ModelAndView("customerlist", "customers",
				customerRepository.findAll());
	}

	@RequestMapping(value = "/form.html", method = RequestMethod.GET)
	public ModelAndView add() {
		return new ModelAndView("customer", "customer", new Customer());
	}

	@RequestMapping(value = "/form.html", method = RequestMethod.POST)
	public ModelAndView post(Customer customer, HttpServletRequest httpRequest) {
		customer = customerRepository.save(customer);
		return new ModelAndView("success");
	}

	@RequestMapping(value = "/{id}.html", method = RequestMethod.PUT)
	public ModelAndView put(@PathVariable("id") long id, Customer customer,
			HttpServletRequest httpRequest) {
		customer.setId(id);
		customerRepository.save(customer);
		return new ModelAndView("success");
	}

	@RequestMapping(value = "/{id}.html", method = RequestMethod.DELETE)
	public ModelAndView delete(@PathVariable("id") long id) {
		customerRepository.deleteById(id);
		return new ModelAndView("success");
	}

}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

@Configuration
class SpringRestDataConfig extends RepositoryRestConfigurerAdapter {

	@Bean
	public RepositoryRestConfigurer repositoryRestConfigurer() {

		return new RepositoryRestConfigurerAdapter() {
			@Override
			public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
				config.exposeIdsFor(Item.class);
			}
		};
	}

}

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "catalog", path = "catalog")
public interface ItemRepository extends PagingAndSortingRepository<Item, Long> {

	List<Item> findByName(@Param("name") String name);

	List<Item> findByNameContaining(@Param("name") String name);

}


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
public class Item {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private double price;

	public Item() {
		super();
		id = 0l;
	}

	public Item(String name, double price) {
		super();
		this.name = name;
		this.price = price;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);

	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

}


import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

@ComponentScan
@EnableAutoConfiguration
@EnableDiscoveryClient
@Component
public class CatalogApp {

	private final ItemRepository itemRepository;

	@Autowired
	public CatalogApp(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	@PostConstruct
	public void generateTestData() {
		itemRepository.save(new Item("iPod", 42.0));
		itemRepository.save(new Item("iPod touch", 21.0));
		itemRepository.save(new Item("iPod nano", 1.0));
		itemRepository.save(new Item("Apple TV", 100.0));
	}

	public static void main(String[] args) {
		SpringApplication.run(CatalogApp.class, args);
	}

}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.ewolff.microservice.catalog.Item;
import com.ewolff.microservice.catalog.ItemRepository;

@Controller
public class CatalogController {

	private final ItemRepository itemRepository;

	@Autowired
	public CatalogController(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	@RequestMapping(value = "/{id}.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public ModelAndView Item(@PathVariable("id") long id) {
		return new ModelAndView("item", "item", itemRepository.findById(id).get());
	}

	@RequestMapping("/list.html")
	public ModelAndView ItemList() {
		return new ModelAndView("itemlist", "items", itemRepository.findAll());
	}

	@RequestMapping(value = "/form.html", method = RequestMethod.GET)
	public ModelAndView add() {
		return new ModelAndView("item", "item", new Item());
	}

	@RequestMapping(value = "/form.html", method = RequestMethod.POST)
	public ModelAndView post(Item Item) {
		Item = itemRepository.save(Item);
		return new ModelAndView("success");
	}

	@RequestMapping(value = "/{id}.html", method = RequestMethod.PUT)
	public ModelAndView put(@PathVariable("id") long id, Item item) {
		item.setId(id);
		itemRepository.save(item);
		return new ModelAndView("success");
	}

	@RequestMapping(value = "/searchForm.html", produces = MediaType.TEXT_HTML_VALUE)
	public ModelAndView searchForm() {
		return new ModelAndView("searchForm");
	}

	@RequestMapping(value = "/searchByName.html", produces = MediaType.TEXT_HTML_VALUE)
	public ModelAndView search(@RequestParam("query") String query) {
		return new ModelAndView("itemlist", "items",
				itemRepository.findByNameContaining(query));
	}

	@RequestMapping(value = "/{id}.html", method = RequestMethod.DELETE)
	public ModelAndView delete(@PathVariable("id") long id) {
		itemRepository.deleteById(id);
		return new ModelAndView("success");
	}

}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@RibbonClient("order")
public class OrderApp {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(OrderApp.class, args);
	}

}


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CustomerClient {

	private final Logger log = LoggerFactory.getLogger(CustomerClient.class);

	private RestTemplate restTemplate;
	private String customerServiceHost;
	private long customerServicePort;
	private boolean useRibbon;
	private LoadBalancerClient loadBalancer;

	static class CustomerPagedResources extends PagedModel<Customer> {

	}

	@Autowired
	public CustomerClient(@Value("${customer.service.host:customer}") String customerServiceHost,
			@Value("${customer.service.port:8080}") long customerServicePort,
			@Value("${spring.cloud.consul.ribbon.enabled:false}") boolean useRibbon,
			@Autowired RestTemplate restTemplate) {
		super();
		this.restTemplate = restTemplate;
		this.customerServiceHost = customerServiceHost;
		this.customerServicePort = customerServicePort;
		this.useRibbon = useRibbon;
	}

	@Autowired(required = false)
	public void setLoadBalancer(LoadBalancerClient loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	public boolean isValidCustomerId(long customerId) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<String> entity = restTemplate.getForEntity(customerURL() + customerId, String.class);
			return entity.getStatusCode().is2xxSuccessful();
		} catch (final HttpClientErrorException e) {
			if (e.getStatusCode().value() == 404)
				return false;
			else
				throw e;
		}
	}

	protected RestTemplate getRestTemplate() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
		converter.setObjectMapper(mapper);

		return new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
	}

	public Collection<Customer> findAll() {
		PagedModel<Customer> pagedResources = getRestTemplate().getForObject(customerURL(),
				CustomerPagedResources.class);
		return pagedResources.getContent();
	}

	private String customerURL() {
		String url;
		if (useRibbon) {
			ServiceInstance instance = loadBalancer.choose("CUSTOMER");
			url = String.format("http://%s:%s/customer/", instance.getHost(), instance.getPort());
		} else {
			url = String.format("http://%s:%s/customer/", customerServiceHost, customerServicePort);
		}
		log.trace("Customer: URL {} ", url);
		return url;

	}

	public Customer getOne(long customerId) {
		return restTemplate.getForObject(customerURL() + customerId, Customer.class);
	}
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item extends RepresentationModel {

	private String name;

	private double price;

	@JsonProperty("id")
	private long itemId;

	public Item() {
		super();
	}

	public Item(long id, String name, double price) {
		super();
		this.itemId = id;
		this.name = name;
		this.price = price;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public long getItemId() {
		return itemId;
	}

	public void setItemId(long id) {
		this.itemId = id;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);

	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer extends RepresentationModel {

	private String name;

	private String firstname;

	private String email;

	private String street;

	private String city;

	@JsonProperty("id")
	private long customerId;

	public Customer() {
	}

	public Customer(long id, String firstname, String name, String email,
			String street, String city) {
		super();
		this.customerId = id;
		this.firstname = firstname;
		this.name = name;
		this.email = email;
		this.street = street;
		this.city = city;
	}

	public long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(long id) {
		this.customerId = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);

	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

}


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CatalogClient {

	private final Logger log = LoggerFactory.getLogger(CatalogClient.class);

	public static class ItemPagedResources extends PagedModel<Item> {

	}

	private RestTemplate restTemplate;
	private String catalogServiceHost;
	private long catalogServicePort;
	private boolean useRibbon;
	private LoadBalancerClient loadBalancer;

	@Autowired
	public CatalogClient(@Value("${catalog.service.host:catalog}") String catalogServiceHost,
			@Value("${catalog.service.port:8080}") long catalogServicePort,
			@Value("${spring.cloud.consul.ribbon.enabled:false}") boolean useRibbon,
			@Autowired RestTemplate restTemplate) {
		super();
		this.restTemplate = restTemplate;
		this.catalogServiceHost = catalogServiceHost;
		this.catalogServicePort = catalogServicePort;
		this.useRibbon = useRibbon;
	}

	@Autowired(required = false)
	public void setLoadBalancer(LoadBalancerClient loadBalancer) {
		this.loadBalancer = loadBalancer;
	}

	protected RestTemplate getRestTemplate() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
		converter.setObjectMapper(mapper);

		return new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
	}

	public double price(long itemId) {
		return getOne(itemId).getPrice();
	}

	public Collection<Item> findAll() {
		PagedModel<Item> pagedResources = getRestTemplate().getForObject(catalogURL(), ItemPagedResources.class);
		return pagedResources.getContent();
	}

	private String catalogURL() {
		String url;
		if (useRibbon) {
			ServiceInstance instance = loadBalancer.choose("CATALOG");
			url = String.format("http://%s:%s/catalog/", instance.getHost(), instance.getPort());
		} else {
			url = String.format("http://%s:%s/catalog/", catalogServiceHost, catalogServicePort);
		}
		log.trace("Catalog: URL {} ", url);
		return url;
	}

	public Item getOne(long itemId) {
		return restTemplate.getForObject(catalogURL() + itemId, Item.class);
	}

}


import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.ewolff.microservice.order.clients.CatalogClient;

@Entity
@Table(name = "ORDERTABLE")
class Order {

	@Id
	@GeneratedValue
	private long id;

	private long customerId;

	@OneToMany(cascade = CascadeType.ALL)
	private List<OrderLine> orderLine;

	public Order() {
		super();
		orderLine = new ArrayList<OrderLine>();
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}

	public List<OrderLine> getOrderLine() {
		return orderLine;
	}

	public Order(long customerId) {
		super();
		this.customerId = customerId;
		this.orderLine = new ArrayList<OrderLine>();
	}

	public void setOrderLine(List<OrderLine> orderLine) {
		this.orderLine = orderLine;
	}

	public void addLine(int count, long itemId) {
		this.orderLine.add(new OrderLine(count, itemId));
	}

	public int getNumberOfLines() {
		return orderLine.size();
	}

	public double totalPrice(CatalogClient catalogClient) {
		return orderLine.stream()
				.map((ol) -> ol.getCount() * catalogClient.price(ol.getItemId()))
				.reduce(0.0, (d1, d2) -> d1 + d2);
	}

	public void setCustomer(long customerId) {
		this.customerId = customerId;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);

	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
}


import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.ewolff.microservice.order.clients.CatalogClient;
import com.ewolff.microservice.order.clients.Customer;
import com.ewolff.microservice.order.clients.CustomerClient;
import com.ewolff.microservice.order.clients.Item;

@Controller
class OrderController {

	private OrderRepository orderRepository;

	private OrderService orderService;

	private CustomerClient customerClient;
	private CatalogClient catalogClient;

	@Autowired
	private OrderController(OrderService orderService,
			OrderRepository orderRepository, CustomerClient customerClient,
			CatalogClient catalogClient) {
		super();
		this.orderRepository = orderRepository;
		this.customerClient = customerClient;
		this.catalogClient = catalogClient;
		this.orderService = orderService;
	}

	@ModelAttribute("items")
	public Collection<Item> items() {
		return catalogClient.findAll();
	}

	@ModelAttribute("customers")
	public Collection<Customer> customers() {
		return customerClient.findAll();
	}

	@RequestMapping("/")
	public ModelAndView orderList() {
		return new ModelAndView("orderlist", "orders",
				orderRepository.findAll());
	}

	@RequestMapping(value = "/form.html", method = RequestMethod.GET)
	public ModelAndView form() {
		return new ModelAndView("orderForm", "order", new Order());
	}

	@RequestMapping(value = "/line", method = RequestMethod.POST)
	public ModelAndView addLine(Order order) {
		order.addLine(0, catalogClient.findAll().iterator().next().getItemId());
		return new ModelAndView("orderForm", "order", order);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable("id") long id) {
		return new ModelAndView("order", "order", orderRepository.findById(id).get());
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ModelAndView post(Order order) {
		order = orderService.order(order);
		return new ModelAndView("success");
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public ModelAndView post(@PathVariable("id") long id) {
		orderRepository.deleteById(id);

		return new ModelAndView("success");
	}

}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

import com.ewolff.microservice.order.clients.Customer;
import com.ewolff.microservice.order.clients.Item;

@Configuration
class SpringRestDataConfig extends RepositoryRestConfigurerAdapter {

	@Bean
	public RepositoryRestConfigurer repositoryRestConfigurer() {

		return new RepositoryRestConfigurerAdapter() {
			@Override
			public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
				config.exposeIdsFor(Order.class, Item.class, Customer.class);
			}
		};
	}

}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ewolff.microservice.order.clients.CatalogClient;
import com.ewolff.microservice.order.clients.CustomerClient;

@Service
class OrderService {

	private OrderRepository orderRepository;
	private CustomerClient customerClient;
	private CatalogClient itemClient;

	@Autowired
	private OrderService(OrderRepository orderRepository,
			CustomerClient customerClient, CatalogClient itemClient) {
		super();
		this.orderRepository = orderRepository;
		this.customerClient = customerClient;
		this.itemClient = itemClient;
	}

	public Order order(Order order) {
		if (order.getNumberOfLines() == 0) {
			throw new IllegalArgumentException("No order lines!");
		}
		if (!customerClient.isValidCustomerId(order.getCustomerId())) {
			throw new IllegalArgumentException("Customer does not exist!");
		}
		return orderRepository.save(order);
	}

	public double getPrice(long orderId) {
		return orderRepository.findById(orderId).get().totalPrice(itemClient);
	}

}


import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "order", path = "order")
interface OrderRepository extends PagingAndSortingRepository<Order, Long> {

}


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@Entity
public class OrderLine {

	@Column(name = "F_COUNT")
	private int count;

	private long itemId;

	@Id
	@GeneratedValue
	private long id;

	public void setCount(int count) {
		this.count = count;
	}

	public void setItemId(long item) {
		this.itemId = item;
	}

	public OrderLine() {
	}

	public OrderLine(int count, long item) {
		this.count = count;
		this.itemId = item;
	}

	public int getCount() {
		return count;
	}

	public long getItemId() {
		return itemId;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);

	}

	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

}

