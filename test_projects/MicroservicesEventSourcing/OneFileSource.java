import com.timeyang.config.DatabaseInitializer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.stereotype.Component;

/**
 * @author chaokunyang
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableResourceServer
@EnableHystrix
@EnableJpaAuditing
@EnableOAuth2Client
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @LoadBalanced
    @Bean
    public OAuth2RestTemplate loadBalancedRestTemplate(OAuth2ProtectedResourceDetails details, OAuth2ClientContext context) {
        return new OAuth2RestTemplate(details, context);
    }

    @Component
    public static class CustomizedRestMvcConfiguretion extends RepositoryRestConfigurerAdapter {
        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.setBasePath("/api");
        }
    }

    @Bean
    @Profile("dev")
    CommandLineRunner commandLineRunner(DatabaseInitializer databaseInitializer) {
        return args -> {
            // Initialize the database for end to end integration testing
            databaseInitializer.populate();
        };
    }
 }


import com.timeyang.address.Address;
import com.timeyang.creditcard.CreditCard;
import com.timeyang.data.BaseEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author chaokunyang
 */
@Entity
public class Account extends BaseEntity {
    private Long id;
    /**
     * 用户唯一标识。如登录用的用户名。ps: 用户名作为userId是合理的，因为登录时使用的用户名是唯一的
     */
    private String userId;
    /**
     * 账户号，即账号
     */
    @Column(unique = true)
    private String accountNumber;
    private Boolean defaultAccount;
    private Set<CreditCard> creditCards;
    private Set<Address> addresses;

    public Account() {
    }

    public Account(String userId, String accountNumber) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.creditCards = new HashSet<>();
        this.addresses = new HashSet<>();
        this.defaultAccount = false;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Boolean getDefaultAccount() {
        return defaultAccount;
    }

    public void setDefaultAccount(Boolean defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public Set<CreditCard> getCreditCards() {
        return creditCards;
    }

    public void setCreditCards(Set<CreditCard> creditCards) {
        this.creditCards = creditCards;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public Set<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<Address> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", defaultAccount=" + defaultAccount +
                ", creditCards=" + creditCards +
                ", addresses=" + addresses +
                '}';
    }
}


import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 为{@link Account}领域类提供包括分页和排序等基本的管理能力
 *
 * @author chaokunyang
 */
public interface AccountRepository extends PagingAndSortingRepository<Account, Long> {
    List<Account> findAccountsByUserId(@Param("userId") String userId);
}


import com.timeyang.data.BaseEntity;

import javax.persistence.*;

/**
 * @author chaokunyang
 */
@Entity
public class Address extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street1;
    private String street2;
    private Integer zipCode;
    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    public Address() {
    }

    public Address(String country, String province, String city, String district, String street1, String street2, Integer zipCode, AddressType addressType) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.street1 = street1;
        this.street2 = street2;
        this.zipCode = zipCode;
        this.addressType = addressType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public Integer getZipCode() {
        return zipCode;
    }

    public void setZipCode(Integer zipCode) {
        this.zipCode = zipCode;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", street1='" + street1 + '\'' +
                ", street2='" + street2 + '\'' +
                ", zipCode=" + zipCode +
                ", addressType=" + addressType +
                '}';
    }
}


/**
 * 地址类型
 * @author chaokunyang
 */
public enum AddressType {
    SHIPPING, // 货运地址
    BILLING // 发票地址
}


import com.timeyang.account.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * @author chaokunyang
 */
@RestController
@RequestMapping("v1")
public class AccountControllerV1 {
    private AccountServiceV1 accountService;

    @Autowired
    public AccountControllerV1(AccountServiceV1 accountService) {
        this.accountService = accountService;
    }

    @RequestMapping(value = "accounts", method = RequestMethod.GET)
    public ResponseEntity<List<Account>> getUserAccounts() throws Exception {
        return Optional.ofNullable(accountService.getUserAccount()).map(account -> new ResponseEntity<>(account, HttpStatus.OK)).orElseThrow(() -> new Exception("用户账户不存在"));
    }
}


import com.timeyang.account.Account;
import com.timeyang.account.AccountRepository;
import com.timeyang.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaokunyang
 */
@Service
public class AccountServiceV1 {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private OAuth2RestTemplate oAuth2RestTemplate;

    public List<Account> getUserAccount() {
        List<Account> accounts = null;
        User user = oAuth2RestTemplate.getForObject("http://user-service/auth/v1/me", User.class);
        if(user != null) {
            accounts = accountRepository.findAccountsByUserId(user.getUsername()); // 用户名作为userId是合理的，因为登录时使用的用户名是唯一的
        }

        // 掩盖信用卡除最后四位以外的数字
        if(accounts != null) {
            accounts.forEach(account -> account.getCreditCards()
                    .forEach(creditCard -> creditCard.setNumber(creditCard.getNumber().replaceAll("[\\d]{4}(?!$)", "****-"))));
        }
        return accounts;
    }
}


import com.timeyang.account.Account;
import com.timeyang.account.AccountRepository;
import com.timeyang.address.Address;
import com.timeyang.address.AddressType;
import com.timeyang.creditcard.CreditCard;
import com.timeyang.creditcard.CreditCardType;
import com.timeyang.customer.Customer;
import com.timeyang.customer.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 填充数据
 *
 * @author chaokunyang
 * @create 2017-04-05 11:24
 */
@Service
@Profile("dev")
public class DatabaseInitializer {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public void populate() {
        // 清除已有数据
        customerRepository.deleteAll(); // Customer有一个外键引用Account，因此需要先删除Customer，然后才删除Account
        accountRepository.deleteAll();

        Account account = new Account("user", "12345");
        account.setDefaultAccount(true);

        Set<Address> addresses = new HashSet<>();
        Address address = new Address("中国", "云南", "丽江", "古城区", "street1", "street2", 000000, AddressType.SHIPPING);
        addresses.add(address);
        account.setAddresses(addresses);

        Set<CreditCard> creditCards = new HashSet<>();
        CreditCard creditCard = new CreditCard("6666666666666666", CreditCardType.VISA);
        creditCards.add(creditCard);
        account.setCreditCards(creditCards);

        //account = accountRepository.save(account); // spring data save方法的jpa实现是 EntityManager.persist方法，该方法不能persist detached entity, EntityManager.merge方法可以，但spring data jpa 未采用，所以 不单独保存，而是放到保存customer时使用级联保存。

        // 创建一个customer
        Customer customer = new Customer("Time", "Yang", "timeyang@timeyang.com", account);
        customerRepository.save(customer);
    }

}


import com.timeyang.data.BaseEntity;

import javax.persistence.*;

/**
 * @author chaokunyang
 */
@Entity
public class CreditCard extends BaseEntity {

    private Long id;
    private String number;
    @Enumerated(EnumType.STRING)
    private CreditCardType type;

    public CreditCard() {
    }

    public CreditCard(String number, CreditCardType type) {
        this.number = number;
        this.type = type;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public CreditCardType getType() {
        return type;
    }

    public void setType(CreditCardType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", type=" + type +
                '}';
    }
}


/**
 * @author chaokunyang
 */
public enum CreditCardType {
    VISA,
    MASTERCARD
}


import com.timeyang.account.Account;
import com.timeyang.data.BaseEntity;

import javax.persistence.*;

/**
 * @author chaokunyang
 */
@Entity
public class Customer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    @OneToOne(cascade = CascadeType.ALL)
    private Account account;

    public Customer() {
    }

    public Customer(String firstName, String lastName, String email, Account account) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", account=" + account +
                '}';
    }
}


import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * 为{@link Customer}领域类提供包括分页和排序等基本的管理能力
 * @author chaokunyang
 */
public interface CustomerRepository extends PagingAndSortingRepository<Customer, Long> {

}


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

/**
 * @author chaokunyang
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity  {

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long lastModified;

    public BaseEntity() {
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}


import java.io.Serializable;

/**
 * @author chaokunyang
 */
public class User implements Serializable {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private Long createdAt;
    private Long lastModified;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}


import com.timeyang.account.AccountRepository;
import com.timeyang.customer.CustomerRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by chaokunyang on 2017/4/5.
 */
//@SpringBootTest(classes = {AccountServiceApplication.class})
@SpringBootTest
@RunWith(SpringRunner.class)
public class DatabaseInitializerTest {

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    public void test() {
        databaseInitializer.populate();
        System.out.println(accountRepository.findAll());
        System.out.println(customerRepository.findAll());
    }
}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * 目录服务
 *
 * @author chaokunyang
 */
@SpringBootApplication
@EnableHystrix
@EnableDiscoveryClient
public class CatalogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }

    @Bean

    
    @LoadBalanced
    public RestTemplate loadRestTemplate() {
        return new RestTemplate();
    }

}


import com.timeyang.catalog.Catalog;
import com.timeyang.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * @author chaokunyang
 */
@RestController
@RequestMapping("/v1")
public class CatalogControllerV1 {
    @Autowired
    private CatalogServiceV1 catalogService;

    @RequestMapping(value = "/catalog", method = RequestMethod.GET, name = "getCatalog")
    public ResponseEntity<Catalog> getCatalog() {
        return Optional.ofNullable(catalogService.getCatalog())
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/products/{productId}", method = RequestMethod.GET, name = "getProduct")
    public ResponseEntity<Product> getProduct(@PathVariable("productId") String productId) {
        return Optional.ofNullable(catalogService.getProduct(productId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}


import com.timeyang.catalog.Catalog;
import com.timeyang.catalog.CatalogInfo;
import com.timeyang.catalog.CatalogInfoRepository;
import com.timeyang.product.Product;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

/**
 * @author chaokunyang
 */
@Service
public class CatalogServiceV1 {
    @Autowired
    private CatalogInfoRepository catalogInfoRepository;
    @Autowired
    private RestTemplate restTemplate;

    @HystrixCommand
    public Catalog getCatalog() {
        CatalogInfo activeCatalogInfo = catalogInfoRepository.findCatalogInfoByActive(true);

        Catalog catalog = restTemplate.getForObject(String.format("http://inventory-service/api/catalogs/search/findCatalogByCatalogNumber?catalogNumber=%s", activeCatalogInfo.getCatalogId()), Catalog.class);
        ProductResource products = restTemplate.getForObject(String.format("http://inventory-service/api/catalogs/%s/products", catalog.getId()), ProductResource.class);

        catalog.setProducts(products.getContent().stream().collect(Collectors.toSet()));
        return catalog;
    }

    @HystrixCommand
    public Product getProduct(String productId) {
        return restTemplate.getForObject(String.format("http://inventory-service/v1/products/%s", productId), Product.class);
    }
}


import com.timeyang.product.Product;
import org.springframework.hateoas.Resources;

/**
 * @author chaokunyang
 */
public class ProductResource extends Resources<Product> {
}


import com.timeyang.product.Product;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author chaokunyang
 */
public class Catalog implements Serializable {
    private Long id;
    private Long catalogNumber;
    private Set<Product> products = new HashSet<>();
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogNumber() {
        return catalogNumber;
    }

    public void setCatalogNumber(Long catalogNumber) {
        this.catalogNumber = catalogNumber;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void setProducts(Set<Product> products) {
        this.products = products;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Catalog{" +
                "id=" + id +
                ", catalogNumber=" + catalogNumber +
                ", name='" + name + '\'' +
                '}';
    }
}


import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

/**
 * @author chaokunyang
 */
@Entity
public class CatalogInfo {
    @Id
    private String id;
    private Long catalogId;
    private Boolean active;

    public CatalogInfo() {
        id = UUID.randomUUID().toString();
        active = false;
    }

    public CatalogInfo(Long catalogId) {
        this();
        this.catalogId = catalogId;
    }

    public CatalogInfo(Long catalogId, Boolean active) {
        this(catalogId);
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(Long catalogId) {
        this.catalogId = catalogId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "CatalogInfo{" +
                "id='" + id + '\'' +
                ", catalogId=" + catalogId +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        CatalogInfo that = (CatalogInfo) obj;
        if(id != null ? !id.equals(that.id) : that.id != null) return false;
        if(catalogId != null ? !catalogId.equals(that.catalogId) : that.catalogId != null) return false;
        return active != null ? active.equals(that.active) : that.active == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (catalogId != null ? catalogId.hashCode() : 0);
        result = 31 * result + (active != null ? active.hashCode() : 0);
        return result;
    }
}

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

/**
 * {@link CatalogInfo}领域类提供基于关系数据库的管理能力
 * @author chaokunyang
 */
public interface CatalogInfoRepository extends JpaRepository<CatalogInfo, String> {
    CatalogInfo findCatalogInfoByActive(@Param("active") Boolean active);
}


import java.io.Serializable;

/**
 * @author chaokunyang
 */
public class Product implements Serializable {
    private Long id;
    private String name;
    private String productId;
    private String description;
    private Double unitPrice;
    private Boolean inStock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", productId='" + productId + '\'' +
                ", description='" + description + '\'' +
                ", unitPrice=" + unitPrice +
                ", inStock=" + inStock +
                '}';
    }
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * 配置中心
 * @author chaokunyang
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * 服务发现
 * @author chaokunyang
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

/**
 * 网关服务<br/>
 * OAuth2 Resource Servers, use a Spring Security filter that authenticates requests <strong>via an incoming OAuth2 token.</strong>
 * @author chaokunyang
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
@EnableResourceServer
@EnableHystrix
public class EdgeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EdgeServiceApplication.class, args);
    }
}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

/**
 * @author chaokunyang
 */
@SpringBootApplication
@EnableHystrixDashboard
public class HystrixDashboardAppilcation {
    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboardAppilcation.class, args);
    }
}



import com.timeyang.address.AddressRepository;
import com.timeyang.catalog.CatalogRepository;
import com.timeyang.inventory.InventoryRepository;
import com.timeyang.product.ProductRepository;
import com.timeyang.shipment.ShipmentRepository;
import com.timeyang.warehouse.WarehouseRepository;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * neo4j配置
 * @author chaokunyang
 */
@Component
@EnableNeo4jRepositories
@EnableTransactionManagement
public class GraphConfiguration extends Neo4jConfiguration {

    @Bean
    public SessionFactory getSessionFactory() {
        // 指定Neo4j应该扫描哪些包，使用每个包里面的类来指定包扫描路径，以避免脆弱性，类型安全，易于重构
        Class<?>[] packageClasses = {
                AddressRepository.class,
                CatalogRepository.class,
                InventoryRepository.class,
                ProductRepository.class,
                ShipmentRepository.class,
                WarehouseRepository.class
        };
        String[] packageNames =
                Arrays.asList(packageClasses)
                        .stream()
                        .map( c -> getClass().getPackage().getName())
                        .collect(Collectors.toList())
                        .toArray(new String[packageClasses.length]);
        return new SessionFactory(packageNames);
    }

    // needed for session in view in web-applications
    @Bean
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Session getSession() throws Exception {
        return super.getSession();
    }
}


import com.timeyang.catalog.Catalog;
import com.timeyang.config.DatabaseInitializer;
import com.timeyang.product.Product;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 库存服务
 *
 * @author chaokunyang
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableDiscoveryClient
@EnableHystrix
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Configuration
    public static class RepositoryMvcConfiguration extends RepositoryRestConfigurerAdapter {
        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.exposeIdsFor(Catalog.class, Product.class);
            config.setBasePath("/api");
        }
    }

    @Bean
    @Profile("dev")
    public CommandLineRunner commandLineRunner(DatabaseInitializer databaseInitializer) {
        return args -> databaseInitializer.populate();
    }

}


import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * @author chaokunyang
 */
@Component
@ConfigurationProperties(prefix = "spring.neo4j")
public class Neo4jProperties {
    private String host;
    private int port;
    private String password;
    private String username;
    private String uri;

    @PostConstruct
    public void setup() {
        Assert.hasText(this.host, "需要提供主机名");
        Assert.isTrue(this.port > 0, "需要提供端口"); // 基础int默认值为0

        if(!StringUtils.hasText(this.uri)) {
            this.uri = String.format("http://%s:%s@%s:%s", // http://user:password@localhost:7474
                    getUsername(), getPassword(), getHost(), getPort());
        }
        LogFactory.getLog(getClass()).info(String.format("host=%s, port=%s, uri=%s", getHost(), getPort(), getUri()));
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * @author chaokunyang
 */
@NodeEntity
public class Address {
    @GraphId
    private Long id;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street1;
    private String street2;
    private Integer zipCode;

    public Address() {
    }

    public Address(String country, String province, String city, String district, String street1, String street2, Integer zipCode) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.street1 = street1;
        this.street2 = street2;
        this.zipCode = zipCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public Integer getZipCode() {
        return zipCode;
    }

    public void setZipCode(Integer zipCode) {
        this.zipCode = zipCode;
    }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", street1='" + street1 + '\'' +
                ", street2='" + street2 + '\'' +
                ", zipCode=" + zipCode +
                '}';
    }
}


import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

/**
 * @author chaokunyang
 */
public interface AddressRepository extends GraphRepository<Address> {
}


import com.timeyang.inventory.Inventory;
import com.timeyang.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @author chaokunyang
 */
@RestController
@RequestMapping("/v1")
public class InventoryControllerV1 {
    @Autowired
    private InventoryServiceV1 inventoryService;

    @RequestMapping(value = "/products/{productId}", method = RequestMethod.GET, name = "getProduct")
    public ResponseEntity<Product> getProduct(@PathVariable("productId") String productId) {
        return Optional.ofNullable((inventoryService.getProduct(productId)))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/inventory", method = RequestMethod.GET, name = "getAvailableInventoryForProductIds")
    public ResponseEntity<List<Inventory>> getAvailableInventoryForProductIds(@RequestParam("productIds") String productIds) {
        return Optional.ofNullable(inventoryService.getAvailableInventoryForProductIds(productIds))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}

import com.timeyang.inventory.Inventory;
import com.timeyang.inventory.InventoryRepository;
import com.timeyang.product.Product;
import com.timeyang.product.ProductRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author chaokunyang
 */
@Service
public class InventoryServiceV1 {
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    Session neo4jTemplate;

    @HystrixCommand(fallbackMethod = "getProductFallback")
    public Product getProduct(String productId) {
        Product product = productRepository.getProductByProductId(productId);
        if(product != null) {
            Stream<Inventory> availableInventory = inventoryRepository.getAvailableInventory(productId).stream();
            product.setInStock(availableInventory.findAny().isPresent());
        }
        return product;
    }

    private Product getProductFallback(String productId) {
        return null;
    }

    public List<Inventory> getAvailableInventoryForProductIds(String productIds) {
        List<Inventory> inventories = inventoryRepository.getAvailableInventoryForProductIds(productIds.split(","));
        return neo4jTemplate.loadAll(inventories, 1)
                .stream().collect(Collectors.toList());
    }

}

import com.timeyang.product.Product;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chaokunyang
 */
@NodeEntity
public class Catalog {
    @GraphId
    private Long id;
    private Long catalogNumber;
    @Relationship(type = "HAS_PRODUCT")
    private List<Product> products = new ArrayList<>();
    private String name;

    public Catalog() {
    }

    public Catalog(Long catalogNumber, String name) {
        this.catalogNumber = catalogNumber;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogNumber() {
        return catalogNumber;
    }

    public void setCatalogNumber(Long catalogNumber) {
        this.catalogNumber = catalogNumber;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Catalog{" +
                "id=" + id +
                ", catalogNumber=" + catalogNumber +
                ", products=" + products +
                ", name='" + name + '\'' +
                '}';
    }
}


import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author chaokunyang
 */
public interface CatalogRepository extends GraphRepository<Catalog> {
    Catalog findCatalogByCatalogNumber(@Param("catalogNumber") Long catalogNumber);
}


import com.timeyang.address.Address;
import com.timeyang.address.AddressRepository;
import com.timeyang.catalog.Catalog;
import com.timeyang.catalog.CatalogRepository;
import com.timeyang.inventory.Inventory;
import com.timeyang.inventory.InventoryRepository;
import com.timeyang.inventory.InventoryStatus;
import com.timeyang.product.Product;
import com.timeyang.product.ProductRepository;
import com.timeyang.shipment.Shipment;
import com.timeyang.shipment.ShipmentRepository;
import com.timeyang.shipment.ShipmentStatus;
import com.timeyang.warehouse.Warehouse;
import com.timeyang.warehouse.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 初始化数据库
 *
 * @author yangck
 */
@Service
@Profile("dev")
public class DatabaseInitializer {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ShipmentRepository shipmentRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CatalogRepository catalogRepository;
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private Neo4jConfiguration neo4jConfiguration;

    public void populate() throws Exception {

        // 删除所有边和节点
        neo4jConfiguration.getSession().query("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r;", new HashMap<>()).queryResults();

        List<Product> products = Arrays.asList(
                new Product("巴黎", "PD-00001", "<p>法国是欧洲浪漫的中心，它悠久历史、具有丰富文化内涵的名胜古迹及乡野风光吸引着世界各地的旅游者。风情万种的花都巴黎，美丽迷人的蓝色海岸，阿尔卑斯山的滑雪场等都是令人神往的旅游胜地 </p>", 9999.0),
                new Product("马尔代夫", "PD-00002", "<p>马尔代夫位于斯里兰卡南方的海域里，被称为印度洋上人间最后的乐园。<strong>马尔代夫由露出水面及部分露出水面的大大小小千余个珊瑚岛组成。</strong><br>" +
                        "<em>马尔代夫 蜜月与爱人情陷天堂岛</em></p>", 29999.0),
                new Product("印度尼西亚，巴厘岛", "PD-00003", "<p>　　它是南太平洋最美丽的景点之一。居住在这里的热情人们让巴厘岛是如此的特别。从肉饼饭到火山，再到古朴的巴厘岛村落，巴厘岛一定是你终身难忘的旅程。</p>", 29999.0),
                new Product("法国，普罗旺斯", "PD-00004", "<p>它已不再是一个单纯的地域名称，更代表了一种简单无忧、轻松慵懒的生活方式，一种宠辱不惊，看庭前花开花落；去留无意，望天上云卷云舒的闲适意境。<strong>如果旅行是为了摆脱生活的桎梏，普罗旺斯会让你忘掉一切。</strong></p>", 9999.0),
                new Product("捷克共和国，布拉格", "PD-00005", "<p><em>布拉格是保存最完整的中世纪城市之一。</em>它一定是任何旅游名单上的必去之地。不要错过夜晚的城堡美景。城堡在灯光的映衬之下显得尤为壮观。</p>", 9999.0),
                new Product("维多利亚瀑布", "PD-00006", "维多利亚瀑布，又称莫西奥图尼亚瀑布，位于非洲赞比西河中游，赞比亚与津巴布韦接壤处。宽1,700多米(5,500多英尺)，最高处108米(355英尺)，为世界著名瀑布奇观之一。<br/>维多利亚瀑布由‘魔鬼瀑布’、‘马蹄瀑布’、‘彩虹瀑布’、‘主瀑布’及‘东瀑布’共五道宽达百米的大瀑布组成<p><em>你站在瀑布边缘，看着瀑布一泻而下，发出如雷般的轰鸣，<strong>你无论如何大喊大叫，都听不到自己的声音，你的肾上腺素在体内涌动，你似乎体会到了临近死亡的感觉。</strong></em></p>", 13999.0),
                new Product("泰姬陵", "PD-00007", "泰姬陵（印地语：ताज महल，乌尔都语：تاج محل\u200E），是位于印度北方邦阿格拉的一座用白色大理石建造的陵墓，是印度知名度最高的古迹之一。<p>它是<em>莫卧儿王朝第5代皇帝沙贾汗</em>为了<strong>纪念他的第二任妻子已故皇后姬蔓·芭奴而兴建的陵墓</strong>，竣工于1654年。泰姬陵被广泛认为是“印度穆斯林艺术的珍宝和世界遗产中被广泛赞美的杰作之一”</p>", 13999.0),
                new Product("大堡礁", "PD-00008", "大堡礁（The Great Barrier Reef），是世界最大最长的珊瑚礁群，位于南半球，它纵贯于澳洲的东北沿海，北从托雷斯海峡，南到南回归线以南，绵延伸展共有2011公里，最宽处161公里。有2900个大小珊瑚礁岛，自然景观非常特殊。这里自然条件适宜，无大风大浪，成了多种鱼类的栖息地，而在那里不同的月份还能看到不同的水生珍稀动物，让游客大饱眼福。" +
                        "<p><em>在落潮时，部分的珊瑚礁露出水面形成珊瑚岛。在礁群与海岸之间是一条极方便的交通海路。<strong>风平浪静时，游船在此间通过，船下连绵不断的多彩、多形的珊瑚景色，就成为吸引世界各地游客来猎奇观赏的最佳海底奇观。</strong></em>大堡礁属热带气候，主要受南半球气流控制。</p>", 13999.0)
        );
        productRepository.save(products);

        Catalog catalog = new Catalog(0L, "测试目录1");
        catalog.getProducts().addAll(products);
        catalogRepository.save(catalog);

        Address warehouseAddress = new Address("中国", "云南", "丽江", "古城区", "street1", "street2", 000000);
        Address shipToAddress = new Address("中国", "海南省", "三亚", "天涯区", "street1", "street2", 000001);
        addressRepository.save(Arrays.asList(warehouseAddress, shipToAddress));

        Warehouse warehouse = new Warehouse("测试仓库1");
        warehouse.setAddress(warehouseAddress);
        warehouse = warehouseRepository.save(warehouse);
        Warehouse finalWarehouse = warehouse;

        // 创建一个有着随机库存编号的库存集合。即为每个产品生成一个产品数量为1的库存，有着随机库存编号
        Random random = new Random();
        Set<Inventory> inventories = products.stream()
                .map(product -> new Inventory(IntStream.range(0, 9).mapToObj(i -> Integer.toString(random.nextInt(9))).collect(Collectors.joining("")), product, finalWarehouse, InventoryStatus.IN_STOCK)).collect(Collectors.toSet());
        inventoryRepository.save(inventories);

        // 为每个产品生成10个额外库存。因为一个库存代表一个数量为1的产品，所以每个产品将有10个库存
        for(int i = 0; i < 10; i++) {
            inventoryRepository.save(products.stream().map(product -> new Inventory(IntStream.range(0, 9).mapToObj(x -> Integer.toString(random.nextInt(9))).collect(Collectors.joining("")), product, finalWarehouse, InventoryStatus.IN_STOCK)).collect(Collectors.toSet()));
        }

        Shipment shipment = new Shipment(inventories, shipToAddress, warehouse, ShipmentStatus.SHIPPED);
        shipmentRepository.save(shipment);
    }

}


import com.timeyang.product.Product;
import com.timeyang.warehouse.Warehouse;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 * <h1>库存</h1>
 * 每个库存代表的数量为1的一个产品。如一个产品有800个库存，在neo4j就有800个inventory节点与该product节点关联<br/>
 * 一个商品的库存可以位于不同仓库<br/>
 * 这样设计易于实现幂等性，在完成订单事务失败时，能够更好地返库存。而如果一个库存记录代表多个库存数量，则在并发返库存时会遇到一些问题<br/>
 * @author chaokunyang
 */
@NodeEntity
public class Inventory {
    @GraphId
    private Long id;

    private String inventoryNumber;

    @Relationship(type = "PRODUCT_TYPE")
    private Product product;

    @Relationship(type = "STOCKED_IN")
    private Warehouse warehouse;

    private InventoryStatus status;

    public Inventory() {
    }

    public Inventory(String inventoryNumber, Product product, Warehouse warehouse, InventoryStatus status) {
        this.inventoryNumber = inventoryNumber;
        this.product = product;
        this.warehouse = warehouse;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInventoryNumber() {
        return inventoryNumber;
    }

    public void setInventoryNumber(String inventoryNumber) {
        this.inventoryNumber = inventoryNumber;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", inventoryNumber='" + inventoryNumber + '\'' +
                ", product=" + product +
                ", warehouse=" + warehouse +
                ", status=" + status +
                '}';
    }
}


import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author chaokunyang
 */

public interface InventoryRepository extends GraphRepository<Inventory> {

    /**
     * Inventory到Product关系为：PRODUCT_TYPE
     * Shipment到Inventory关系为：CONTAINS_PRODUCT
     * @param productId
     * @return
     */
    @Query("MATCH (product:Product)<-[:PRODUCT_TYPE]-(inventory:Inventory) WHERE product.productId = {productId} AND NOT (inventory)<-[:CONTAINS_PRODUCT]-() RETURN inventory")
    List<Inventory> getAvailableInventory(@Param("productId") String productId);

    @Query("MATCH (product:Product)<-[:PRODUCT_TYPE]-(inventory:Inventory)-[:STOCKED_IN]->(:Warehouse {name:{warehouseName}}) WHERE product.productId = {productId} AND NOT (inventory)<-[:CONTAINS_PRODUCT]-() RETURN inventory")
    List<Inventory> getAvailableInventoryForProductAndWarehouse(@Param("productId") String productId, @Param("warehouseName") String warehouseName);

    @Query("MATCH (product:Product)<-[:PRODUCT_TYPE]-(inventory:Inventory) WHERE product.productId in {productIds} AND NOT (inventory)<-[:CONTAINS_PRODUCT]-() RETURN inventory")
    List<Inventory> getAvailableInventoryForProductIds(@Param("productIds")String[] productIds);
}


/**
 * 库存状态
 * @author chaokunyang
 */
public enum InventoryStatus {
    IN_STOCK, // 有库存
    ORDERED, // 已下单
    RESERVED, // 预定的
    SHIPPED, // 已发货
    DELIVERED // 已交货
}


import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;

/**
 * @author chaokunyang
 */
@NodeEntity
public class Product {
    @GraphId
    private Long id;
    private String name, productId, description;
    private Double unitPrice;
    @Transient
    private Boolean inStock;

    public Product() {
    }

    public Product(String name, String productId, String description, Double unitPrice) {
        this.name = name;
        this.productId = productId;
        this.description = description;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", productId='" + productId + '\'' +
                ", description='" + description + '\'' +
                ", unitPrice=" + unitPrice +
                ", inStock=" + inStock +
                '}';
    }
}

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;

/**
 * @author chaokunyang
 */
public interface ProductRepository extends GraphRepository<Product> {
    Product getProductByProductId(@Param("productId") String productId);
}


import com.timeyang.address.Address;
import com.timeyang.inventory.Inventory;
import com.timeyang.warehouse.Warehouse;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author chaokunyang
 */
@NodeEntity
public class Shipment {
    @GraphId
    private Long id;

    @Relationship(type = "CONTAINS_PRODUCT")
    private Set<Inventory> inventories = new HashSet<>();

    @Relationship(type = "SHIP_TO")
    private Address deliveryAddress;

    @Relationship(type = "SHIP_FROM")
    private Warehouse fromWarehouse;

    private ShipmentStatus shipmentStatus;

    public Shipment() {
    }

    public Shipment(Set<Inventory> inventories, Address deliveryAddress, Warehouse fromWarehouse, ShipmentStatus shipmentStatus) {
        this.inventories = inventories;
        this.deliveryAddress = deliveryAddress;
        this.fromWarehouse = fromWarehouse;
        this.shipmentStatus = shipmentStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Inventory> getInventories() {
        return inventories;
    }

    public void setInventories(Set<Inventory> inventories) {
        this.inventories = inventories;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Warehouse getFromWarehouse() {
        return fromWarehouse;
    }

    public void setFromWarehouse(Warehouse fromWarehouse) {
        this.fromWarehouse = fromWarehouse;
    }

    public ShipmentStatus getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(ShipmentStatus shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "id=" + id +
                ", inventories=" + inventories +
                ", deliveryAddress=" + deliveryAddress +
                ", fromWarehouse=" + fromWarehouse +
                ", shipmentStatus=" + shipmentStatus +
                '}';
    }
}

import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author chaokunyang
 */
public interface ShipmentRepository extends GraphRepository<Shipment> {
}


/**
 *
 * @author chaokunyang
 */
public enum ShipmentStatus {
    PENDING, // 未发货
    SHIPPED, // 已发货
    DELIVERED // 已交货
}


import com.timeyang.address.Address;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 * @author chaokunyang
 */
@NodeEntity
public class Warehouse {
    @GraphId
    private Long id;

    private String name;

    @Relationship(type = "HAS_ADDRESS")
    private Address address;

    public Warehouse() {
    }

    public Warehouse(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address=" + address +
                '}';
    }
}

import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author chaokunyang
 */
public interface WarehouseRepository extends GraphRepository<Warehouse> {
}


/**
 * 添加是否已认证信息响应头，用于在前端判断是否获取受保护数据，因为获取受保护的数据会重定向到Oauth服务器，从而在js里面发生跨域，而且即使能够跨域，返回的也是登录页面的html代码，而不是受保护的资源
 * @author yangck
 */
//public class AuthInfoFilter implements Filter {
//    @Override
//    public void init(FilterConfig filterConfig) throws ServletException {
//
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication.isAuthenticated()) {
//            ((HttpServletResponse)response).setHeader("authenticated", "true");
//        }
//        chain.doFilter(request, response);
//
//    }
//
//    @Override
//    public void destroy() {
//
//    }
//}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * 在线商店
 * Oauth2.0 客户端
 *
 * @author chaokunyang
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableOAuth2Sso
@EnableHystrix
@EnableZuulProxy //
public class OnlineStoreApplication extends WebSecurityConfigurerAdapter {
    public static void main(String[] args) {
        SpringApplication.run(OnlineStoreApplication.class, args);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .antMatcher("/**").authorizeRequests()
                .antMatchers("/", "/index.html", "/assets/**", "/login", "/api/catalog/**").permitAll().anyRequest().authenticated()
                .and().logout().logoutSuccessUrl("/").permitAll()
                .and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()); // What Angular would like is for the server to send it a cookie called "XSRF-TOKEN" and if it sees that, it will send the value back as a header named "X-XSRF-TOKEN". 需要注意withHttpOnlyFalse后容易受到XSS攻击
                //.and().csrf().disable(); // 这样虽然可以工作，但不安全
    }

    ///**
    // * 使用认证信息过滤器添加是否已认证的相关信息
    // * @return
    // */
    //@Bean
    //public FilterRegistrationBean authInfoFilterRegistration() {
    //    FilterRegistrationBean registration = new FilterRegistrationBean();
    //    registration.setFilter(new AuthInfoFilter());
    //    registration.setName("authInfoFilter");
    //    registration.setDispatcherTypes(DispatcherType.REQUEST);
    //    registration.addUrlPatterns("/*"); // 记住是"/*"，而不是"/**"，后者不会过滤请求
    //    registration.setOrder(1);
    //    return registration;
    //}
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.stereotype.Component;

/**
 * 订单服务
 * @author chaokunyang
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
@EnableMongoAuditing
@EnableResourceServer
@EnableOAuth2Client
@EnableHystrix
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

    @LoadBalanced
    @Bean
    public OAuth2RestTemplate loadBalancedOauth2RestTemplate(OAuth2ProtectedResourceDetails resourceDetails, OAuth2ClientContext clientContext) {
        return new OAuth2RestTemplate(resourceDetails, clientContext);
    }

    @Component
    public static class RepositoryRestConfiguration extends RepositoryRestConfigurerAdapter {
        @Override
        public void configureRepositoryRestConfiguration(org.springframework.data.rest.core.config.RepositoryRestConfiguration config) {
            config.setBasePath("/api");
        }
    }
}



import com.timeyang.address.Address;
import com.timeyang.creditcard.CreditCard;
import com.timeyang.data.BaseEntityDto;

import java.util.Set;

/**
 * @author chaokunyang
 */
public class Account extends BaseEntityDto {
    private Long id;
    private String userId;
    private String accountNumber;
    private Boolean defaultAccount;
    private Set<CreditCard> creditCards;
    private Set<Address> addresses;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Boolean isDefaultAccount() {
        return defaultAccount;
    }

    public void setDefaultAccount(Boolean defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    public Set<CreditCard> getCreditCards() {
        return creditCards;
    }

    public void setCreditCards(Set<CreditCard> creditCards) {
        this.creditCards = creditCards;
    }

    public Set<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<Address> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", defaultAccount=" + defaultAccount +
                ", creditCards=" + creditCards +
                ", addresses=" + addresses +
                '}';
    }
}


import java.io.Serializable;

/**
 * @author chaokunyang
 */
public class Address implements Serializable {
    private Long id;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street1;
    private String street2;
    private Integer zipCode;
    private AddressType addressType;

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", street1='" + street1 + '\'' +
                ", street2='" + street2 + '\'' +
                ", zipCode=" + zipCode +
                ", addressType=" + addressType +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public Integer getZipCode() {
        return zipCode;
    }

    public void setZipCode(Integer zipCode) {
        this.zipCode = zipCode;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }
}


/**
 * @author yangck
 */
public enum AddressType {
    SHIPPING, // 配送地址
    BILLING // 发票地址
}

import com.timeyang.order.Order;
import com.timeyang.order.OrderEvent;
import com.timeyang.order.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @author yangck
 */
@RestController
@RequestMapping("/v1")
public class OrderControllerV1 {

    @Autowired
    private OrderServiceV1 orderService;

    @RequestMapping(value = "/accounts/{accountNumber}/orders")
    public ResponseEntity getOrders(@PathVariable("accountNumber") String accountNumber) throws Exception {
        return Optional.ofNullable(orderService.getOrdersForAccount(accountNumber))
                .map(orders -> new ResponseEntity<>(orders, HttpStatus.OK))
                .orElseThrow(() -> new Exception("用户不存在该账户"));
    }

    @RequestMapping(value = "/orders/{orderId}/events", method = RequestMethod.POST)
    public ResponseEntity addOrderEvent(@RequestBody OrderEvent orderEvent, @PathVariable("orderId") String orderId) throws Exception {
        Assert.notNull(orderEvent);
        Assert.notNull(orderId);
        return Optional.ofNullable(orderService.addOrderEvent(orderEvent, true))
                .map(result -> new ResponseEntity<>(result, HttpStatus.NO_CONTENT))
                .orElseThrow(() -> new Exception("订单事件不能被应用到该订单"));
    }

    @RequestMapping(value = "/orders/{orderId}", method = RequestMethod.GET)
    public ResponseEntity<Order> getOrder(@PathVariable("orderId") String orderId) throws Exception {
        Assert.notNull(orderId);
        return Optional.ofNullable(orderService.getOrder(orderId, true))
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElseThrow(() -> new Exception("不能够获取订单"));
    }

    @RequestMapping(value = "/orders", method = RequestMethod.POST)
    public ResponseEntity<Order> createOrder(@RequestBody List<OrderItem> orderItems) throws Exception {
        Assert.notNull(orderItems);
        Assert.isTrue(orderItems.size() > 0);
        return Optional.ofNullable(orderService.createOrder(orderItems))
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElseThrow(() -> new Exception("不能够创建订单"));
    }

}


import com.timeyang.account.Account;
import com.timeyang.address.AddressType;
import com.timeyang.order.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单服务
 *
 * @author yangck
 */
@Service
public class OrderServiceV1 {

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private OrderRepositroy orderRepositroy;

    @Autowired
    private OrderEventRepository orderEventRepository;

    @Autowired
    private OAuth2RestTemplate oAuth2RestTemplate;

    public Order createOrder(List<OrderItem> orderItems) {
        Account[] accounts = oAuth2RestTemplate.getForObject("http://account-service/v1/accounts", Account[].class);

        Account defaultAccount = Arrays.stream(accounts)
                .filter(Account::isDefaultAccount)
                .findFirst().orElse(null);

        if(defaultAccount == null) {
            return null;
        }

        Order newOrder = new Order(defaultAccount.getAccountNumber(),
                defaultAccount.getAddresses().stream()
                        .filter(address -> address.getAddressType() == AddressType.SHIPPING)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("默认账户没有收货地址")));
        newOrder.setOrderItems(orderItems);
        newOrder = orderRepositroy.save(newOrder);

        return newOrder;
    }

    public Boolean addOrderEvent(OrderEvent orderEvent, Boolean validate) throws Exception {
        // 得到订单事件对应的订单
        Order order = orderRepositroy.findOne(orderEvent.getOrderId());

        if(validate) {
            // 验证事件对应的订单的账户号(account number)属于用户。
            validateAccountNumber(order.getAccountNumber());
        }

        // 保存订单事件
        orderEventRepository.save(orderEvent);

        return true;
    }

    public Order getOrder(String orderId, Boolean validate) {
        // 获取订单
        Order order = orderRepositroy.findOne(orderId);

        if(validate) {
            // 验证事件对应的订单的账户号(account number)属于用户
            try {
                validateAccountNumber(order.getAccountNumber());
            } catch (Exception e) {
                return null;
            }
        }

        Flux<OrderEvent> orderEvents = Flux.fromStream(orderEventRepository.findOrderEventsByOrderId(orderId));

        // 聚合订单状态
        return orderEvents.takeWhile(orderEvent -> orderEvent.getType() != OrderEventType.DELIVERED)
                .reduceWith(() -> order, Order::incorporate)
                .get();
    }

    public List<Order> getOrdersForAccount(String accountNUmber) throws Exception {
        validateAccountNumber(accountNUmber);

        List<Order> orders = orderRepositroy.findByAccountNumber(accountNUmber);

        return orders.stream()
                .map(order -> getOrder(order.getOrderId(), true))
                .filter(order -> order != null)
                .collect(Collectors.toList());
    }

    /**
     * 验证账户号是否有效
     * @param accountNumber
     * @return 一个布尔值表示账户号是否有效
     * @throws Exception 账户号无效时抛出异常
     */
    public boolean validateAccountNumber(String accountNumber) throws Exception {
        Account[] accounts = oAuth2RestTemplate.getForObject("http://account-service/v1/accounts", Account[].class);

        // 确保账户号被当前验证用户拥有
        if(accounts != null && !Arrays.stream(accounts).anyMatch(account -> Objects.equals(account.getAccountNumber(), accountNumber))) {
            log.error("账户号无效:" + accountNumber);
            throw new Exception("账户号无效:" + accountNumber);
        }

        return true;
    }

}



import com.timeyang.data.BaseEntityDto;

/**
 * @author chaokunyang
 */
public class CreditCard extends BaseEntityDto {

    private Long id;
    private String number;
    private CreditCardType type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public CreditCardType getType() {
        return type;
    }

    public void setType(CreditCardType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", type=" + type +
                '}';
    }
}


/**
 * @author chaokunyang
 */
public enum CreditCardType {
    VISA,
    MASTERCARD
}


import com.timeyang.account.Account;
import com.timeyang.data.BaseEntityDto;

/**
 * @author chaokunyang
 */
public class Customer extends BaseEntityDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Account account;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", account=" + account +
                '}';
    }
}


import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yangck
 */
public class BaseEntity implements Serializable {
    @LastModifiedDate
    private Date lastModified;
    @CreatedDate
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "lastModified=" + lastModified +
                ", createdAt=" + createdAt +
                '}';
    }
}


import java.io.Serializable;

/**
 * @author yangck
 */
public class BaseEntityDto implements Serializable {
    private Long createdAt;
    private Long lastModified;

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}


import com.timeyang.address.Address;
import com.timeyang.order.Order;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chaokunyang
 */
@Document
public class Invoice {
    @Id
    private String invoiceId;
    private String customerId;
    private List<Order> orders = new ArrayList<>();
    private Address billingAddress;
    private InvoiceStatus invoiceStatus;

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public InvoiceStatus getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(InvoiceStatus invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId='" + invoiceId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", orders=" + orders +
                ", billingAddress=" + billingAddress +
                ", invoiceStatus=" + invoiceStatus +
                '}';
    }
}


import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author yangck
 */
public interface InvoiceRepository extends PagingAndSortingRepository<Invoice, String> {
}


/**
 * 描述发票{@link Invoice}的状态
 * @author yangck
 */
public enum InvoiceStatus {
    CREATED,
    SENT,
    PAID
}


import com.timeyang.data.BaseEntity;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * @author yangck
 */
@Component
public class BeforeSaveListener extends AbstractMongoEventListener<BaseEntity> {
    @Override
    public void onBeforeSave(BeforeSaveEvent<BaseEntity> event) {
        Date date = Date.from(Instant.now());
        if(event.getSource().getCreatedAt() == null)
            event.getSource().setCreatedAt(date);
        event.getSource().setLastModified(date);
        super.onBeforeSave(event);
    }
}


import com.timeyang.address.Address;
import com.timeyang.address.AddressType;
import com.timeyang.data.BaseEntity;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单领域类
 * @author chaokunyang
 */
@Document
public class Order extends BaseEntity {
    @Id
    private ObjectId orderId; // 使用ObjectId，因为它带有时间和自增顺序。注意在别的服务订单id还是要使用String，因为别的服务不应该有mongo依赖，所以getOrderId方法返回值也是String类型
    private String accountNumber;
    @Transient
    private OrderStatus orderStatus; // 订单状态是对事件聚合产生的，而不是持久化到数据库的
    private List<OrderItem> orderItems = new ArrayList<>();
    private Address shippingAddress;

    public Order() {
        this.orderStatus = OrderStatus.PURCHASED;
    }

    public Order(String accountNumber, Address shippingAddress) {
        this();
        this.accountNumber = accountNumber;
        this.shippingAddress = shippingAddress;
        if (shippingAddress.getAddressType() == null)
            this.shippingAddress.setAddressType(AddressType.SHIPPING);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
    }

    /**
     * <p>incorporate 方法通过一个针对订单状态的简单的状态机(state machine) ，使用事件源(event sourcing)
     * 和聚合(aggregation)来生成订单的当前状态</p>
     * <p>下面的事件流图表示事件怎么被合并来生成订单状态。订单状态的事件日志可以被用来在分布式事务失败时回滚订单的状态。每个状态只对应两个操作：前进/后退。PURCHASED状态没有回退，DELIVERED状态没有前进</p>
     * <p>
     * Events:   +<--PURCHASED+  +<--CREATED+  +<---ORDERED+  +<----SHIPPED+ 回退 <br/>
     * *         |            |  |          |  |           |  |            | <br/>
     * Status    +PURCHASED---+PENDING------+CONFIRMED-----+SHIPPED--------+DELIVERED <br/>
     * *         |            |  |          |  |           |  |            | <br/>
     * Events:   +CREATED---->+  +ORDERED-->+  +SHIPPED--->+  +DELIVERED-->+ 前进<br/>
     * </p>
     * @param orderEvent 是将要被合并进状态机的事件
     * @return 有着聚合的订单状态的 {@link Order} 聚合
     */
    public Order incorporate(OrderEvent orderEvent) {
        if(orderStatus == null)
            orderStatus = OrderStatus.PURCHASED;

        orderStatus = orderStatus.nextStatus(orderEvent);

        return this;
    }

    /**
     * 使用ObjectId，因为它带有时间和自增顺序
     * @return
     */
    public String getOrderId() {
        return orderId != null ? orderId.toHexString() : null;
    }

    public void setOrderId(ObjectId orderId) {
        this.orderId = orderId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", orderStatus=" + orderStatus +
                ", orderItems=" + orderItems +
                ", shippingAddress=" + shippingAddress +
                '}';
    }
}



import com.timeyang.data.BaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 订单事件
 *
 * @author yangck
 */
@Document
public class OrderEvent extends BaseEntity {
    @Id
    private String id;
    private OrderEventType type;
    private String orderId;

    @Override
    public String toString() {
        return "OrderEvent{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", orderId='" + orderId + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrderEventType getType() {
        return type;
    }

    public void setType(OrderEventType type) {
        this.type = type;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.stream.Stream;

/**
 * @author yangck
 */
public interface OrderEventRepository extends MongoRepository<OrderEvent, String> {
    Stream<OrderEvent> findOrderEventsByOrderId(@Param("orderId") String orderId);
}

/**
 * <p>Events: PURCHASED ---> CREATED ----> ORDERED ---> SHIPPED ---> DELIVERED</p>
 * <strong>注意，在订单事件里面，用户发起购买请求和生成订单是两个不同的事件，一定要分开</strong>
 * @author yangck
 */
public enum OrderEventType {
    /**
     * 用户已购买产品事件。对应用户在页面发起购买请求，系统收到用户发起购买请求<strong>这一事实</strong>，但尚未创建订单
     */
    PURCHASED,

    /**
     * 系统已创建订单事件。系统生成订单，<strong>这一事件事实与用户无关，是系统完成的操作</strong>
     */
    CREATED,

    /**
     * 用户订购物品事件。对应用户确认订单
     */
    ORDERED,

    /**
     * 在线商店已发货事件。
     */
    SHIPPED,

    /**
     * 在线商店已交货事件
     */
    DELIVERED
}

import java.io.Serializable;

/**
 * 订单项
 *
 * @author yangck
 */
public class OrderItem implements Serializable {
    private String name, productId;
    private Integer quantity;
    private Double price, tax;

    public OrderItem() {
    }

    public OrderItem(String name, String productId, Integer quantity, Double price, Double tax) {
        this.name = name;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.tax = tax;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }
}

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 订单{@link Order}常用操作
 * @author yangck
 */
public interface OrderRepositroy extends MongoRepository<Order, String> {
    List<Order> findByAccountNumber(String accountNumber);
}


/**
 * <h1>描述订单 {@link Order} 状态。同时作为订单状态机</h1>
 * <p>订单状态前进: PURCHASED ---> PENDING ---> CONFIRMED ---> SHIPPED ---> DELIVERED</p>
 * <p>订单状态回退: PURCHASED <--- PENDING <--- CONFIRMED <--- SHIPPED <--- DELIVERED</p>
 * @author yangck
 */
public enum OrderStatus {
    /**
     * <p>已购买。incorporate(合并) OrderEventType.PURCHASED 事件后，订单状态变为 PURCHASED</p>
     * <p>即系统已响应用户在页面点击的购买请求，但系统尚未创建订单</p>
     */
    PURCHASED {
        @Override
        public OrderStatus nextStatus(OrderEvent orderEvent) {
            if(orderEvent.getType() == OrderEventType.CREATED)
               return OrderStatus.PENDING; // 表示订单状态前进：PURCHASED -> PENDING
            throw new IllegalArgumentException("Illegal order event type " + orderEvent + ". only accept " + OrderEventType.CREATED);
        }
    },

    /**
     * 系统已生成订单，订单待处理。incorporate(合并) OrderEventType.CREATED 事件后，订单状态变为 PENDING
     * <p>即订单已创建，等待系统处理</p>
     */
    PENDING {
        @Override
        public OrderStatus nextStatus(OrderEvent orderEvent) {
            if(orderEvent.getType() == OrderEventType.ORDERED)
                return CONFIRMED; // 表示订单状态前进：PENDING -> CONFIRMED
            else if (orderEvent.getType() == OrderEventType.PURCHASED)
                return OrderStatus.PURCHASED; // 表示订单状态回退：PENDING -> PURCHASED
            throw new IllegalArgumentException("Illegal order event type " + orderEvent + ". only accept " + OrderEventType.ORDERED + " or " + OrderEventType.PURCHASED);
        }
    },

    /**
     * 订单已确认。incorporate(合并) OrderEventType.ORDERED 事件后，订单状态变为 CONFIRMED
     * <P>即系统已确认订单</P>
     */
    CONFIRMED {
        @Override
        public OrderStatus nextStatus(OrderEvent orderEvent) {
            if(orderEvent.getType() == OrderEventType.SHIPPED) {
                return OrderStatus.SHIPPED; // 表示订单状态前进：CONFIRMED -> SHIPPED
            }else if(orderEvent.getType() == OrderEventType.CREATED) {
                return OrderStatus.PENDING; // 表示订单状态回退：CONFIRMED -> PENDING
            }
            throw new IllegalArgumentException("Illegal order event type " + orderEvent + ". only accept " + OrderEventType.SHIPPED + " or " + OrderEventType.CREATED);
        }
    },

    /**
     * 已发货。incorporate(合并) OrderEventType.SHIPPED 事件后，订单状态变为 SHIPPED
     * <p>即在线商店已发货</p>
     */
    SHIPPED {
        @Override
        public OrderStatus nextStatus(OrderEvent orderEvent) {
            if(orderEvent.getType() == OrderEventType.DELIVERED) {
                return OrderStatus.DELIVERED; // 表示订单状态前进：SHIPPED -> DELIVERED
            }else if(orderEvent.getType() == OrderEventType.ORDERED) {
                return OrderStatus.CONFIRMED; // 表示订单状态回退：CONFIRMED -> PENDING
            }
            throw new IllegalArgumentException("Illegal order event type " + orderEvent + ". only accept " + OrderEventType.DELIVERED + " or " + OrderEventType.ORDERED);
        }
    },

    /**
     * 已交货。incorporate(合并) OrderEventType.DELIVERED 事件后，订单状态变为 DELIVERED
     * <p>即用户已收货</p>
     */
    DELIVERED {
        @Override
        public OrderStatus nextStatus(OrderEvent orderEvent) {
            if(orderEvent.getType() == OrderEventType.SHIPPED) {
                return OrderStatus.SHIPPED; // 表示订单状态回退：DELIVERED -> SHIPPED
            }
            throw new IllegalArgumentException("Illegal order event type " + orderEvent + ". only accept " + OrderEventType.SHIPPED);
        }
    };

    /**
     * get next order status according to order event
     *
     * @param orderEvent order event
     * @return next order status, according to order event, next status maybe forward or rollback
     */
    public abstract OrderStatus nextStatus(OrderEvent orderEvent);
}

import java.io.Serializable;

/**
 * @author chaokunyang
 */
public class User implements Serializable {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private Long createdAt;
    private Long lastModified;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支付服务
 * @author yangck
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}


import com.timeyang.order.Order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * 购物车服务
 *
 * @author yangck
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@EnableDiscoveryClient
@EnableResourceServer
@EnableOAuth2Client
@EnableHystrix
public class CartServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }

    @LoadBalanced
    @Bean
    public OAuth2RestTemplate loadBalancedOauth2RestTemplate(OAuth2ProtectedResourceDetails details, OAuth2ClientContext clientContext) {
        return new OAuth2RestTemplate(details, clientContext);
    }

    @LoadBalanced
    @Bean("loadBalancedRestTemplate")
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    @Component
    public static class RepositoryRestMvcConfiguration extends RepositoryRestConfigurerAdapter {
        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.exposeIdsFor(Order.class);
            config.setBasePath("/api");
        }
    }
}


import java.io.Serializable;

/**
 * @author chaokunyang
 */
public class Address implements Serializable {
    private Long id;
    private String country;
    private String province;
    private String city;
    private String district;
    private String street1;
    private String street2;
    private Integer zipCode;
    private AddressType addressType;

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", street1='" + street1 + '\'' +
                ", street2='" + street2 + '\'' +
                ", zipCode=" + zipCode +
                ", addressType=" + addressType +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public Integer getZipCode() {
        return zipCode;
    }

    public void setZipCode(Integer zipCode) {
        this.zipCode = zipCode;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }
}


/**
 * @author yangck
 */
public enum AddressType {
    SHIPPING, // 货运地址
    BILLING // 发票地址
}

import com.timeyang.cart.CartEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * @author yangck
 */
@RestController
@RequestMapping("/v1")
public class ShoppingCartControllerV1 {
    @Autowired
    private ShoppingCartServiceV1 shoppingCartService;

    /**
     * 增加购物车事件
     * @param cartEvent
     * @return
     * @throws Exception
     */
    @RequestMapping(path = "/events", method = RequestMethod.POST)
    public ResponseEntity addCartEvent(@RequestBody CartEvent cartEvent) throws Exception {
        return Optional.ofNullable(shoppingCartService.addCartEvent(cartEvent))
                .map(event -> new ResponseEntity(HttpStatus.NO_CONTENT))
                .orElseThrow(() -> new Exception("不能够找到购物车"));
    }

    /**
     * 检出购物车
     * @return
     * @throws Exception
     */
    @RequestMapping(path = "/checkout", method = RequestMethod.POST)
    public ResponseEntity checkoutCart() throws Exception {
        return Optional.ofNullable(shoppingCartService.checkOut())
                .map(checkoutResult -> new ResponseEntity<>(checkoutResult, HttpStatus.OK))
                .orElseThrow(() -> new Exception("不能够检出购物车"));
    }

    /**
     * 获取购物车
     * @return
     * @throws Exception
     */
    @RequestMapping(path = "/cart", method = RequestMethod.GET)
    public ResponseEntity getCart() throws Exception {
        return Optional.ofNullable(shoppingCartService.getShoppingCart())
                .map(cart -> new ResponseEntity(cart, HttpStatus.OK))
                .orElseThrow(() -> new Exception("不能够获取购物车"));
    }
}

import com.timeyang.cart.*;
import com.timeyang.catalog.Catalog;
import com.timeyang.inventory.Inventory;
import com.timeyang.order.Order;
import com.timeyang.order.OrderEvent;
import com.timeyang.order.OrderEventType;
import com.timeyang.order.OrderItem;
import com.timeyang.user.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
/**
 * The {@link ShoppingCartServiceV1} implements business logic for aggregating the state of a user's actions represented a sequence of {@link CartEvent}. The generated aggregate use event sourcing to produce a {@link com.timeyang.cart.ShoppingCart} containing a collection of {@link CartItem}
 * @author yangck
 */
@Service
public class ShoppingCartServiceV1 {
    public static final double TAX = .06;

    private final Log log = LogFactory.getLog(getClass());

    @Autowired
    private OAuth2RestTemplate oAuth2RestTemplate;

    @Autowired
    @Qualifier("loadBalancedRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private CartEventRepository cartEventRepository;

    /**
     * 从user-service得到已验证的用户
     * @return 当前验证的用户
     */
    public User getAuthenticatedUser() {
        //System.out.println(oAuth2RestTemplate.getForObject("http://user-service/auth/v1/me", User.class));
        return oAuth2RestTemplate.getForObject("http://user-service/auth/v1/me", User.class);
    }

    /**
     * 为当前已验证用户添加一个购物车事件
     * @param cartEvent 详细描述用户执行的action的事件
     * @return 一个标识表示结果是否成功
     */
    public Boolean addCartEvent(CartEvent cartEvent) {
        User user = getAuthenticatedUser();
        if(user != null) {
            cartEvent.setUserId(user.getId());
            cartEventRepository.save(cartEvent);
        }else {
           return null;
        }
        return true;
    }

    public Boolean addCartEvent(CartEvent cartEvent, User user) {
        if(user != null) {
            cartEvent.setUserId(user.getId());
            cartEventRepository.save(cartEvent);
        }else {
            return null;
        }
        return true;
    }


    /**
     * 为当前已验证用户得到购物车
     * @return an aggregate object derived from events performed by the user
     * @throws Exception
     */
    public ShoppingCart getShoppingCart() throws Exception {
        User user = oAuth2RestTemplate.getForObject("http://user-service/auth/v1/me", User.class);
        ShoppingCart shoppingCart = null;
        if(user != null) {
            Catalog catalog = restTemplate.getForObject("http://catalog-service/v1/catalog", Catalog.class);
            shoppingCart = aggregateCartEvents(user, catalog);
        }
        return shoppingCart;
    }

    /**
     * 聚合(Aggregate)一个用户的cart events，返回一个 {@link ShoppingCart}
     * @param user 获取购物车的用户
     * @param catalog 用于生成购物车的目录
     * @return 一个表示用户购物车聚合状态的购物车
     * @throws Exception 如果在购物车中的一个产品不在目录里面，则抛出异常
     */
    public ShoppingCart aggregateCartEvents(User user, Catalog catalog) throws Exception {
        Flux<CartEvent> cartEvents = Flux.fromStream(cartEventRepository.getCartEventStreamByUserId(user.getId()));

        // 聚合购物车的状态
        ShoppingCart shoppingCart = cartEvents.takeWhile(cartEvent -> !ShoppingCart.isTerminal(cartEvent.getCartEventType()))
                .reduceWith(() -> new ShoppingCart(catalog), ShoppingCart::incorporate)
                .get();

        shoppingCart.getCartItems();
        return shoppingCart;
    }

    /**
     * 检出用户当前的购物车，在处理完支付后生成一张新的订单
     * TODO 支付集成
     * @return 检出操作的结果
     * @throws Exception
     */
    public CheckoutResult checkOut() throws Exception {
        CheckoutResult checkOutResult = new CheckoutResult();

        // 检查可用库存
        ShoppingCart currentCart = null;
        try {
            currentCart = getShoppingCart();
        }catch (Exception e) {
            log.error("获取购物车失败", e);
        }

        if(currentCart != null) {
            // 协调当前购物车与库存
            Inventory[] inventory = oAuth2RestTemplate.getForObject(String.format("http://inventory-service/v1/inventory?productIds=%s", currentCart.getCartItems()
                    .stream()
                    .map(CartItem::getProductId)
                    .collect(Collectors.joining(","))), Inventory[].class);

            if(inventory != null) {
                Map<String, Long> inventoryItems = Arrays.stream(inventory)
                        .map(inv -> inv.getProduct().getProductId())
                        .collect(groupingBy(Function.identity(), counting()));
                        /*.collect(Collectors.groupingBy(
                                (inv -> inv.getProduct().getProductId()), Collectors.counting()));*/

                if(checkAvailableInventory(checkOutResult, currentCart, inventoryItems)) {
                    // 预定库存 Reserve the available inventory

                    // 创建订单
                    Order order = oAuth2RestTemplate.postForObject("http://order-service/v1/orders",
                            currentCart.getCartItems()
                                    .stream()
                                    .map(cartItem -> new OrderItem(cartItem.getProduct().getName(), cartItem.getProductId(),
                                            cartItem.getQuantity(), cartItem.getProduct().getUnitPrice(), TAX))
                                    .collect(Collectors.toList()),
                            Order.class);

                    if(order != null) {
                        // 订单创建成功
                        checkOutResult.setResultMessage("订单已成功创建");

                        // 增加订单事件
                        oAuth2RestTemplate.postForEntity(
                                String.format("http://order-service/v1/orders/%s/events", order.getOrderId()),
                                new OrderEvent(OrderEventType.CREATED, order.getOrderId()), ResponseEntity.class);

                        checkOutResult.setOrder(order);

                        User user = oAuth2RestTemplate.getForObject("http://user-service/auth/v1/me", User.class);

                        // 增加CartEventType.CHECKOUT事件，清除购物车，因为已经生成订单成功
                        addCartEvent(new CartEvent(CartEventType.CHECKOUT, user.getId()), user);
                    }
                }
            }
        }

        // 返回检出结果：要么库存不足，要么检出成功
        return checkOutResult;
    }

    /**
     * 检查是否有充足的库存
     * @param checkOutResult
     * @param currentCart
     * @param inventoryItems
     * @return
     */
    public Boolean checkAvailableInventory(CheckoutResult checkOutResult, ShoppingCart currentCart, Map<String, Long> inventoryItems) {
        Boolean hasInventory = true;
        // 判断库存是否可用
        try {
            List<CartItem> inventoryUnAvailableCartItems = currentCart.getCartItems()
                    .stream()
                    .filter(item -> inventoryItems.get(item.getProductId()) - item.getQuantity() < 0)
                    .collect(Collectors.toList());

            if(inventoryUnAvailableCartItems.size() > 0) {
                String productIds = inventoryUnAvailableCartItems
                        .stream()
                        .map(CartItem::getProductId)
                        .collect(Collectors.joining(", "));
                checkOutResult.setResultMessage(
                        String.format("以下产品没有充足的库存可用：%s. " +
                            "请降低这些产品的数量再次尝试.", productIds));
                hasInventory = false;
            }
        } catch (Exception e) {
            log.error("检查是否有可利用的库存时出错", e);
        }

        return hasInventory;
    }
}

import com.timeyang.data.BaseEntity;

import javax.persistence.*;

/**
 * <h1>购物车事件</h1>
 * 因为购物车事件频繁基于 id, userId 被查询，所以应该建立索引。主键id会自动建索引，这里只是为了保证在索引中的列的顺序
 * @author yangck
 */
@Entity
@Table(name = "cart_event", indexes = {@Index(name = "IDX_CART_EVENT_USER", columnList = "id, userId")})
public class CartEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    private CartEventType cartEventType;
    private Long userId;
    private String productId;
    private Integer quantity;

    public CartEvent() {
    }

    /**
     * 检出/清除购物车时使用这个构造器
     * @param cartEventType
     * @param userId
     */
    public CartEvent(CartEventType cartEventType, Long userId) {
        this.cartEventType = cartEventType;
        this.userId = userId;
    }

    /**
     * ADD_ITEM/REMOVE_ITEM时使用这个构造器
     * @param cartEventType
     * @param userId
     * @param productId
     * @param quantity
     */
    public CartEvent(CartEventType cartEventType, Long userId, String productId, Integer quantity) {
        this.cartEventType = cartEventType;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartEvent{" +
                "id=" + id +
                ", cartEventType=" + cartEventType +
                ", userId=" + userId +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CartEventType getCartEventType() {
        return cartEventType;
    }

    public void setCartEventType(CartEventType cartEventType) {
        this.cartEventType = cartEventType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.stream.Stream;

/**
 * 为购物车事件{@link CartEvent}提供数据访问功能
 *
 * @author yangck
 */
public interface CartEventRepository extends JpaRepository<CartEvent, Long> {
    /**
     * <p>先从cart_event中取得事件类型为CLEAR_CART/CHECKOUT的事件，然后以时间降序排列，然后通过'LIMIT 1'只取时间最大的那个事件，命名为t，(临时的意思)</p>
     * <p>将整个cart_event与之前的t进行右外连接，条件是两个user_id相同，同时事件创建时间在t与2900亿年后之间，通过事件id不等于t的事件id(因为't'对应的cart_event是CLEAR_CART/CHECKOUT，不应该被聚合)</p>
     * <p>这样保证获取到的事件流只包含从最后一个CLEAR_CART/CHECKOUT之后的ADD_ITEM、REMOVE_ITEM的事件，因为CLEAR_CART/CHECKOUT不应该被聚合</p>
     * @param userId
     * @return
     */
    @Query(value = "SELECT c.*\n" +
            "FROM (\n" +
            "       SELECT *\n" +
            "       FROM cart_event\n" +
            "       WHERE user_id = :userId AND (cart_event_type = 'CLEAR_CART' OR cart_event_type = 'CHECKOUT')\n" +
            "       ORDER BY cart_event.created_at DESC\n" +
            "       LIMIT 1\n" +
            "     ) t\n" +
            "RIGHT JOIN cart_event c ON c.user_id = t.user_id\n" +
            "WHERE c.created_at BETWEEN coalesce(t.created_at, 0) AND 9223372036854775807 AND coalesce(t.id, -1) != c.id\n" +
            "ORDER BY c.created_at ASC", nativeQuery = true)
    Stream<CartEvent> getCartEventStreamByUserId(@Param("userId")Long userId);
}

/**
 * 购物车事件类型
 *
 * @author yangck
 */
public enum CartEventType {
    ADD_ITEM, // 添加项
    REMOVE_ITEM, // 移除项
    CLEAR_CART, // 清除购物车
    CHECKOUT // 检出项
}


import com.timeyang.product.Product;

/**
 * 购物车项
 *
 * @author yangck
 */
public class CartItem {
    private String productId;
    private Product product;
    private Integer quantity;

    public CartItem(String productId, Product product, Integer quantity) {
        this.productId = productId;
        this.product = product;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "productId='" + productId + '\'' +
                ", product=" + product +
                ", quantity=" + quantity +
                '}';
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}


import com.timeyang.order.Order;

import java.io.Serializable;

/**
 * 检出购物车结果
 *
 * @author yangck
 */
public class CheckoutResult implements Serializable {
    private String resultMessage;
    private Order order;

    public CheckoutResult() {
    }

    public CheckoutResult(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public CheckoutResult(String resultMessage, Order order) {
        this.resultMessage = resultMessage;
        this.order = order;
    }

    @Override
    public String toString() {
        return "CheckoutResult{" +
                "resultMessage='" + resultMessage + '\'' +
                ", order=" + order +
                '}';
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}


import com.timeyang.catalog.Catalog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.log4j.Logger;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 购物车{@link ShoppingCart}对象代表用户从购物车add/remove/clear/checkout products 动作产生的{@link CartEvent}的聚合
 *
 * @author yangck
 */
public class ShoppingCart {
    private Logger logger = Logger.getLogger(ShoppingCart.class);
    private Map<String, Integer> productMap = new HashMap<>();
    private List<CartItem> cartItems = new ArrayList<>();
    private Catalog catalog;

    public ShoppingCart(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 从购物车事件的聚合(aggregate)生成并得到cart items
     * @return 代表购物车状态的一个新的 {@link CartItem} list
     * @throws Exception 如果在购物车中的一个产品不在目录里面，则抛出异常
     */
    public List<CartItem> getCartItems() throws Exception {
        cartItems = productMap.entrySet().stream()
                .map(item -> new CartItem(item.getKey(), catalog.getProducts().stream()
                        .filter(product -> Objects.equals(product.getProductId(), item.getKey()))
                        .findFirst()
                        .orElse(null), item.getValue()))
                .filter(item -> item.getQuantity() > 0)
                .collect(Collectors.toList());

        if(cartItems.stream().anyMatch(item -> item.getProduct() == null))
            throw new Exception("没有在目录里找到产品");

        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    /**
     * Incorporates a new {@link CartEvent} and update the shopping cart
     * @param cartEvent 是将要改变购物车状态的{@link CartEvent}
     * @return 在应用了新的 {@link CartEvent} 后返回 {@link ShoppingCart} 的状态
     */
    public ShoppingCart incorporate(CartEvent cartEvent) {
        Flux<CartEventType> validCartEventTypes =
                Flux.fromStream(Stream.of(CartEventType.ADD_ITEM, CartEventType.REMOVE_ITEM));

        // CartEvent类型必须是 ADD_ITEM or REMOVE_ITEM
        if(validCartEventTypes.exists(cartEventType ->
                cartEvent.getCartEventType().equals(cartEventType)).get()) {
            // 根据事件类型更新购物车每个产品的数量的聚合
            productMap.put(cartEvent.getProductId(),
                    productMap.getOrDefault(cartEvent.getProductId(), 0) +
                            (cartEvent.getQuantity() * (cartEvent.getCartEventType()
                                    .equals(CartEventType.ADD_ITEM) ? 1 : -1))
            );
        }

        // Return the updated state of the aggregate to the reactive stream's reduce method
        return this;
    }

    /**
     * Determines whether or not the {@link CartEvent} is a terminal event，causing the stream to end while generating an aggregate {@link ShoppingCart}
     * @param cartEventType is the {@link CartEventType} to evaluate
     * @return a flag 表示是否事件是terminal
     */
    public static Boolean isTerminal(CartEventType cartEventType) {
        return (cartEventType == CartEventType.CLEAR_CART || cartEventType == CartEventType.CHECKOUT);
    }

    @JsonIgnore
    public Map<String, Integer> getProductMap() {
        return productMap;
    }

    public void setProductMap(Map<String, Integer> productMap) {
        this.productMap = productMap;
    }

    @JsonIgnore
    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public String toString() {
        return "ShoppingCart{" +
                "productMap=" + productMap +
                ", cartItems=" + cartItems +
                ", catalog=" + catalog +
                '}';
    }
}


import com.timeyang.product.Product;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 目录
 * @author yangck
 */
public class Catalog implements Serializable {
    private Long id;

    private String name;

    private Set<Product> products = new HashSet<>();

    @Override
    public String toString() {
        return "Catalog{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", products=" + products +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void setProducts(Set<Product> products) {
        this.products = products;
    }
}


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * @author yangck
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity implements Serializable {
    @LastModifiedDate
    private Long lastModified;
    @CreatedDate
    private Long createdAt;

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "lastModified=" + lastModified +
                ", createdAt=" + createdAt +
                '}';
    }
}


import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yangck
 */
public class BaseEntityDto implements Serializable {
    private Date lastModified;
    private Date createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}


import com.timeyang.data.BaseEntityDto;
import com.timeyang.product.Product;
import com.timeyang.warehouse.Warehouse;

/**
 * @author yangck
 */
public class Inventory extends BaseEntityDto {
    private Long id;

    private String inventoryNumber;

    private Product product;

    private Warehouse warehouse;

    private InventoryStatus status;

    public Inventory() {
    }

    public Inventory(String inventoryNumber, Product product) {
        this.inventoryNumber = inventoryNumber;
        this.product = product;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", inventoryNumber='" + inventoryNumber + '\'' +
                ", product=" + product +
                ", warehouse=" + warehouse +
                ", status=" + status +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInventoryNumber() {
        return inventoryNumber;
    }

    public void setInventoryNumber(String inventoryNumber) {
        this.inventoryNumber = inventoryNumber;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }
}


/**
 * 库存状态
 * @author chaokunyang
 */
public enum InventoryStatus {
    IN_STOCK, // 有库存
    ORDERED, // 已下单
    RESERVED, // 预定的
    SHIPPED, // 已发货
    DELIVERED // 已交货
}


import com.timeyang.address.Address;
import com.timeyang.data.BaseEntityDto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yangck
 */
public class Order extends BaseEntityDto {
    private String orderId;
    private String accountNumber;
    private OrderStatus orderStatus;
    private List<OrderItem> lineItems = new ArrayList<>();
    private Address shippingAddress;

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", orderStatus=" + orderStatus +
                ", lineItems=" + lineItems +
                ", shippingAddress=" + shippingAddress +
                '}';
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public List<OrderItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<OrderItem> lineItems) {
        this.lineItems = lineItems;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}



import com.timeyang.data.BaseEntity;

/**
 * 订单事件
 *
 * @author yangck
 */
public class OrderEvent extends BaseEntity {
    private String id;
    private OrderEventType type;
    private String orderId;

    public OrderEvent(OrderEventType type, String orderId) {
        this.type = type;
        this.orderId = orderId;
    }

    @Override
    public String toString() {
        return "OrderEvent{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", orderId='" + orderId + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrderEventType getType() {
        return type;
    }

    public void setType(OrderEventType type) {
        this.type = type;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}

/**
 * <p>Events: PURCHASED ---> CREATED ----> ORDERED ---> SHIPPED ---> DELIVERED</p>
 * <strong>注意，在订单事件里面，用户发起购买请求和生成订单是两个不同的事件，一定要分开</strong>
 * @author yangck
 */
public enum OrderEventType {
    /**
     * 用户已购买产品事件。对应用户在页面发起购买请求，系统收到用户发起购买请求<strong>这一事实</strong>，但尚未创建订单
     */
    PURCHASED,

    /**
     * 系统已创建订单事件。系统生成订单，<strong>这一事件事实与用户无关，是系统完成的操作</strong>
     */
    CREATED,

    /**
     * 用户订购物品事件。对应用户确认订单
     */
    ORDERED,

    /**
     * 在线商店已发货事件。
     */
    SHIPPED,

    /**
     * 在线商店已交货事件
     */
    DELIVERED
}

import java.io.Serializable;

/**
 * 订单项
 *
 * @author yangck
 */
public class OrderItem implements Serializable {
    private String name, productId;
    private Integer quantity;
    private Double price, tax;

    public OrderItem() {
    }

    public OrderItem(String name, String productId, Integer quantity, Double price, Double tax) {
        this.name = name;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.tax = tax;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }
}

/**
 * 订单状态: PURCHASED ---> PENDING ---> CONFIRMED ---> SHIPPED ---> DELIVERED
 * @author yangck
 */
public enum OrderStatus {
    /**
     * <p>已购买。incorporate(合并) OrderEventType.PURCHASED 事件后，订单状态变为 PURCHASED</p>
     * <p>即系统已响应用户在页面点击的购买请求，但系统尚未创建订单</p>
     */
    PURCHASED,

    /**
     * 系统已生成订单，订单待处理。incorporate(合并) OrderEventType.CREATED 事件后，订单状态变为 PENDING
     * <p>即订单已创建，等待系统处理</p>
     */
    PENDING,

    /**
     * 订单已确认。incorporate(合并) OrderEventType.ORDERED 事件后，订单状态变为 CONFIRMED
     * <P>即系统已确认订单</P>
     */
    CONFIRMED,

    /**
     * 已发货。incorporate(合并) OrderEventType.SHIPPED 事件后，订单状态变为 SHIPPED
     * <p>即在线商店已发货</p>
     */
    SHIPPED,

    /**
     * 已交货。incorporate(合并) OrderEventType.DELIVERED 事件后，订单状态变为 DELIVERED
     * <p>即用户已收货</p>
     */
    DELIVERED
}

import java.io.Serializable;

/**
 * 产品
 * @author yangck
 */
public class Product implements Serializable {
    private Long id;
    private String name, productId, description;
    private Double unitPrice;

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", productId='" + productId + '\'' +
                ", description='" + description + '\'' +
                ", unitPrice=" + unitPrice +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }
}


import com.timeyang.data.BaseEntityDto;

/**
 * @author chaokunyang
 */
public class User extends BaseEntityDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}

import com.timeyang.address.Address;

/**
 * @author chaokunyang
 */
public class Warehouse {
    private Long id;

    private String name;

    private Address address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address=" + address +
                '}';
    }
}

import org.junit.Test;

/**
 * Created by yangck on 2017/3/14.
 */
public class ShoppingCartServiceV1Test {
    @Test
    public void checkOut() throws Exception {
       /* Inventory[] inventory = new Inventory[2];
        Product product1 = new Product();
        Product product2 = new Product();

        Map<String, Long> inventoryItems = Arrays.stream(inventory)
                .map(inv -> inv.getProduct().getProductId())
                .collect(groupingBy(Function.identity(), counting()));*/

    }

}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;

/**
 * @author yangck
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableDiscoveryClient
@EnableHystrix
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    public static class CustomizedRestMvcConfiguration extends RepositoryRestConfigurerAdapter {
        @Override
        public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.setBasePath("/api");
        }
    }
}


import com.timeyang.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

/**
 * @author yangck
 */
@RestController
@RequestMapping("/v1")
public class UserControllerV1 {

    @Autowired
    private UserServiceV1 userService;

    @RequestMapping(path = "/me")
    public ResponseEntity<User> me(Principal principal) {
        User user = null;
        if(principal != null) {
            user = userService.getUserByUsername(principal.getName());
        }
        return Optional.ofNullable(user)
                .map(a -> new ResponseEntity<>(a, HttpStatus.OK))
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
    }

}

import com.timeyang.user.User;
import com.timeyang.user.UserRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yangck
 */
@Service
public class UserServiceV1 {

    @Autowired
    private UserRepository userRepository;

    //@Cacheable(value = "user", key = "#username") // Cannot get Jedis connection ?
    @HystrixCommand
    public User getUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

}



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * @author yangck
 */
@Configuration
public class AuthorizationServerConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder authManagerBuilder) throws Exception {
        authManagerBuilder
                .inMemoryAuthentication()
                .withUser("user").password("password").roles("USER")
                .and().withUser("admin").password("password").roles("ADMIN", "USER");
    }

   /* @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .jdbcAuthentication()
                .dataSource(dataSource)
                .withDefaultSchema()
                .withUser("user").password("password").roles("USER").and()
                .withUser("admin").password("password").roles("USER", "ADMIN");
    }*/

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/assets*/**").permitAll()
                .anyRequest().authenticated()
                .and().formLogin().loginPage("/login").permitAll()
                .and().logout().permitAll()
                .and().csrf().disable();
                //.and().csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()); // 使用angularjs并且开启csrf保护的话就需要配置该行代码。需要注意withHttpOnlyFalse后容易受到XSS攻击

    }

    @Configuration
    @EnableAuthorizationServer
    protected static class OAuth2Config extends AuthorizationServerConfigurerAdapter {
        /**
         * 也可以放在application.yml配置文件里面
         *
         * security:
             oauth2:
                 client:
                     client-id: timeyang
                     client-secret: timeyangsecret
                     scope: read,write
                     auto-approve-scopes: '.*'
         *
         * @param clients
         * @throws Exception
         */
        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients
                    .inMemory()
                    .withClient("timeyang")
                    .secret("timeyangsecret")
                    .authorizedGrantTypes("authorization_code", "refresh_token", "password").scopes("openid");
        }
    }
}


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author chaokunyang
 */
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public JedisConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host}") String redisHost,
            @Value("${spring.redis.port}") Integer redisPort) {
        JedisConnectionFactory redisConnectionFactory = new JedisConnectionFactory();
        redisConnectionFactory.setHostName(redisHost);
        redisConnectionFactory.setPort(redisPort);

        return redisConnectionFactory;
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager((redisTemplate));
        cacheManager.setDefaultExpiration(10000); // 10000s后过期
        return cacheManager;
    }
}

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * @author yangck
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/assets/**", "/login").permitAll()
                .anyRequest().authenticated();
    }

    @Bean
    public HttpSessionSecurityContextRepository contextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}



import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author yangck
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/oauth/confirm_access").setViewName("authorize");
    }
}



import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

/**
 * @author chaokunyang
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long lastModified;

    public BaseEntity() {
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }
}


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.security.auth.login.CredentialException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yangck
 */
@SessionAttributes("authorizationRequest")
@Controller
public class LoginController {

    private final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private HttpSessionSecurityContextRepository httpSessionSecurityContextRepository;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, response);
        httpSessionSecurityContextRepository.loadContext(holder);

        try {
            // 使用提供的证书认证用户
            List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN");
            Authentication auth = new UsernamePasswordAuthenticationToken(request.getParameter("username"), request.getParameter("password"), authorities);
            SecurityContextHolder.getContext().setAuthentication(authenticationManager.authenticate(auth));

            // 认证用户
            if(!auth.isAuthenticated())
                throw new CredentialException("用户不能够被认证");
        } catch (Exception ex) {
            // 用户不能够被认证，重定向回登录页
            logger.info(ex);
            return "login";
        }

        // 从会话得到默认保存的请求
        DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        // 为令牌请求生成认证参数Map
        Map<String, String> authParams = getAuthParameters(defaultSavedRequest);
        AuthorizationRequest authRequest = new DefaultOAuth2RequestFactory(clientDetailsService).createAuthorizationRequest(authParams);
        authRequest.setAuthorities(AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN"));
        model.addAttribute("authorizationRequest", authRequest);

        httpSessionSecurityContextRepository.saveContext(SecurityContextHolder.getContext(), holder.getRequest(), holder.getResponse());
        return "authorize";
    }

    /**
     * 为会话的令牌请求生成认证参数Map
     * @param defaultSavedRequest 会话中默认保存的SPRING_SECURITY_SAVED_REQUEST请求
     * @return 包含OAuth2请求明细的参数Map
     */
    private Map<String,String> getAuthParameters(DefaultSavedRequest defaultSavedRequest) {
        Map<String, String> authParams = new HashMap<>();
        authParams.put(OAuth2Utils.CLIENT_ID, defaultSavedRequest.getParameterMap().get(OAuth2Utils.CLIENT_ID)[0]);
        authParams.put(OAuth2Utils.REDIRECT_URI, defaultSavedRequest.getParameterMap().get(OAuth2Utils.REDIRECT_URI)[0]);
        if(defaultSavedRequest.getParameterMap().get(OAuth2Utils.STATE) != null) {
            authParams.put(OAuth2Utils.STATE, defaultSavedRequest.getParameterMap().get(OAuth2Utils.STATE)[0]);
        }

        authParams.put(OAuth2Utils.RESPONSE_TYPE, "code");
        authParams.put(OAuth2Utils.USER_OAUTH_APPROVAL, "true");
        authParams.put(OAuth2Utils.GRANT_TYPE, "authorization_code");

        return authParams;
    }

}


import com.timeyang.data.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * @author yangck
 */
@Entity
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

/**
 * @author yangck
 */
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/user")
    public Principal user(Principal user) {
        return user;
    }

    @RequestMapping(path = "/users", method = RequestMethod.POST, name = "createUser")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        Assert.notNull(user);
        return Optional.ofNullable(userService.createUser(user))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.CONFLICT));
    }

    @RequestMapping(path = "users/{id}", method = RequestMethod.GET, name = "getUser")
    public ResponseEntity<User> getUser(@PathVariable("id") Long id) {
        return Optional.ofNullable(userService.getUserById(id))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(path = "users/{id}", method = RequestMethod.PUT, name = "updateUser")
    public ResponseEntity<User> updateUser(@PathVariable(value = "id") Long id, @RequestBody User user) {
        Assert.notNull(user);
        user.setId(id);
        return Optional.ofNullable(userService.updateUser(id, user))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(path = "users/{id}", method = RequestMethod.DELETE,
            name = "deleteUser")
    public ResponseEntity deleteUser(@PathVariable("id") Long id) {
        return Optional.ofNullable(userService.deleteUser(id))
                .map(result -> new ResponseEntity<>(result, HttpStatus.NO_CONTENT))
                .orElse(new ResponseEntity<Boolean>(HttpStatus.NOT_FOUND));
    }
}


import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * 为{@link User}领域类提供包括分页和排序等基本的管理能力
 * @author yangck
 */
@RepositoryRestResource
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    User findUserByUsername(@Param("username") String username);
}

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author yangck
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @CacheEvict(value = "user", key = "#user.getId()")
    public User createUser(User user) {
        User result = null;
        if(!userRepository.exists(user.getId())) {
            result = userRepository.save(user);
        }
        return result;
    }

    @Cacheable(value = "user", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findOne(id);
    }

    @CachePut(value = "user", key = "#id")
    public User updateUser(Long id, User user) {
        User result = null;
        if(userRepository.exists(user.getId())) {
            result = userRepository.save(user);
        }
        return result;
    }

    @CacheEvict(value = "user", key = "#id")
    public boolean deleteUser(Long id) {
        boolean deleted = false;
        if (userRepository.exists(id)) {
            userRepository.delete(id);
            deleted = true;
        }
        return deleted;
    }
}