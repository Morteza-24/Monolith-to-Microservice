
public class RestaurantServiceChannels {

  public static final String RESTAURANT_EVENT_CHANNEL = "net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant";
}


import io.eventuate.common.spring.jdbc.EventuateTransactionTemplateConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.cloudcontractsupport.EventuateContractVerifierConfiguration;
import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantDomainEventPublisher;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantMenu;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeliveryserviceMessagingBase.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class DeliveryserviceMessagingBase {

  @Configuration
  @EnableAutoConfiguration
  @Import({EventuateContractVerifierConfiguration.class, TramEventsPublisherConfiguration.class, TramInMemoryConfiguration.class, EventuateTransactionTemplateConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public RestaurantDomainEventPublisher orderAggregateEventPublisher(DomainEventPublisher eventPublisher) {
      return new RestaurantDomainEventPublisher(eventPublisher);
    }
  }


  @Autowired
  private RestaurantDomainEventPublisher restaurantDomainEventPublisher;

  protected void restaurantCreated() {
    Restaurant restaurant = new Restaurant("Yummy Indian", new RestaurantMenu(Collections.emptyList()));
    restaurant.setId(99L);
    restaurantDomainEventPublisher.publish(restaurant,
            Collections.singletonList(new RestaurantCreated(restaurant.getName(), new Address("1 Main Street", "Unit 99", "Oakland", "CA", "94611"),
                    restaurant.getMenu())));
  }

}


import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@Import(TramJdbcKafkaConfiguration.class)
public class RestaurantServiceMain {

  @Bean
  @Primary // conflicts with _halObjectMapper
  public ObjectMapper objectMapper() {
    return JSonMapper.objectMapper;
  }

  public static void main(String[] args) {
    SpringApplication.run(RestaurantServiceMain.class, args);
  }

}


import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantMenu;

public class RestaurantMenuRevised implements RestaurantDomainEvent {

  private RestaurantMenu menu;

  public RestaurantMenu getRevisedMenu() {
    return menu;
  }
}


import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantMenu;

public class RestaurantCreated implements RestaurantDomainEvent {
  private String name;
  private Address address;
  private RestaurantMenu menu;

  public String getName() {
    return name;
  }

  private RestaurantCreated() {
  }

  public RestaurantCreated(String name, Address address, RestaurantMenu menu) {
    this.name = name;
    this.address = address;
    this.menu = menu;

    if (menu == null) 
      throw new NullPointerException("Null Menu");
    if (address == null) 
      throw new NullPointerException("Null address");
  }

  public RestaurantMenu getMenu() {
    return menu;
  }

  public void setMenu(RestaurantMenu menu) {
    this.menu = menu;
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
}


import io.eventuate.tram.events.common.DomainEvent;

public interface RestaurantDomainEvent extends DomainEvent {
}


public class GetRestaurantResponse {
  private Long id;

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

  private String name;

  public GetRestaurantResponse() {
  }

  public GetRestaurantResponse(Long id, String name) {
    this.id = id;
    this.name = name;
  }
}


public class CreateRestaurantResponse {
  private long id;

  public CreateRestaurantResponse() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public CreateRestaurantResponse(long id) {
    this.id = id;
  }
}


import net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantService;
import net.chrisrichardson.ftgo.restaurantservice.domain.CreateRestaurantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/restaurants")
public class RestaurantController {

  @Autowired
  private RestaurantService restaurantService;

  @RequestMapping(method = RequestMethod.POST)
  public CreateRestaurantResponse create(@RequestBody CreateRestaurantRequest request) {
    Restaurant r = restaurantService.create(request);
    return new CreateRestaurantResponse(r.getId());
  }

  @RequestMapping(method = RequestMethod.GET, path = "/{restaurantId}")
  public ResponseEntity<GetRestaurantResponse> get(@PathVariable long restaurantId) {
    return restaurantService.findById(restaurantId)
            .map(r -> new ResponseEntity<>(makeGetRestaurantResponse(r), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  private GetRestaurantResponse makeGetRestaurantResponse(Restaurant r) {
    return new GetRestaurantResponse(r.getId(), r.getName());
  }


}


import io.eventuate.tram.events.aggregates.AbstractAggregateDomainEventPublisher;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantDomainEvent;

public class RestaurantDomainEventPublisher extends AbstractAggregateDomainEventPublisher<Restaurant, RestaurantDomainEvent> {
  public RestaurantDomainEventPublisher(DomainEventPublisher eventPublisher) {
    super(eventPublisher, Restaurant.class, Restaurant::getId);
  }
}


import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Transactional
public class RestaurantService {


  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private RestaurantDomainEventPublisher restaurantDomainEventPublisher;

  public Restaurant create(CreateRestaurantRequest request) {
    Restaurant restaurant = new Restaurant(request.getName(), request.getMenu());
    restaurantRepository.save(restaurant);
    restaurantDomainEventPublisher.publish(restaurant, Collections.singletonList(new RestaurantCreated(request.getName(), request.getAddress(), request.getMenu())));
    return restaurant;
  }

  public Optional<Restaurant> findById(long restaurantId) {
    return restaurantRepository.findById(restaurantId);
  }
}


import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
@EntityScan
@Import({TramEventsPublisherConfiguration.class, CommonConfiguration.class})
public class RestaurantServiceDomainConfiguration {

  @Bean
  public RestaurantService restaurantService() {
    return new RestaurantService();
  }

  @Bean
  public RestaurantDomainEventPublisher restaurantDomainEventPublisher(DomainEventPublisher domainEventPublisher) {
    return new RestaurantDomainEventPublisher(domainEventPublisher);
  }
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.List;

@Embeddable
@Access(AccessType.FIELD)
public class RestaurantMenu {


  @ElementCollection
  private List<MenuItem> menuItems;

  private RestaurantMenu() {
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public void setMenuItems(List<MenuItem> menuItems) {
    this.menuItems = menuItems;
  }

  public RestaurantMenu(List<MenuItem> menuItems) {

    this.menuItems = menuItems;
  }

}


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "restaurants")
@Access(AccessType.FIELD)
public class Restaurant {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Embedded
  private RestaurantMenu menu;

  private Restaurant() {
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


  public Restaurant(String name, RestaurantMenu menu) {
    this.name = name;
    this.menu = menu;
  }


  public Long getId() {
    return id;
  }

  public RestaurantMenu getMenu() {
    return menu;
  }
}


import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)
public class MenuItem {

  private String id;
  private String name;
  private Money price;

  private MenuItem() {
  }

  public MenuItem(String id, String name, Money price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(Money price) {
    this.price = price;
  }
}


import net.chrisrichardson.ftgo.common.Address;

public class CreateRestaurantRequest {

  private String name;
  private Address address;
  private RestaurantMenu menu;

  private CreateRestaurantRequest() {

  }

  public CreateRestaurantRequest(String name, Address address, RestaurantMenu menu) {
    this.name = name;
    this.address = address;
    this.menu = menu;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RestaurantMenu getMenu() {
    return menu;
  }

  public void setMenu(RestaurantMenu menu) {
    this.menu = menu;
  }

  public Address getAddress() {
    return address;
  }
}


import org.springframework.data.repository.CrudRepository;

public interface RestaurantRepository extends CrudRepository<Restaurant, Long> {
}


import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestaurantServiceLambdaConfiguration.class)
public class RestaurantServiceLambdaConfigurationTest {

  @Autowired
  private RestaurantService restaurantService;
  @Test
  public void shouldInitialize() {}
}

import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantMenu;

public class RestaurantMenuRevised implements RestaurantDomainEvent {

  private RestaurantMenu menu;

  public RestaurantMenu getRevisedMenu() {
    return menu;
  }
}


import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantMenu;

public class RestaurantCreated implements RestaurantDomainEvent {
  private String name;
  private Address address;
  private RestaurantMenu menu;

  public String getName() {
    return name;
  }

  private RestaurantCreated() {
  }

  public RestaurantCreated(String name, Address address, RestaurantMenu menu) {
    this.name = name;
    this.address = address;
    this.menu = menu;


    if (menu == null) 
      throw new NullPointerException("Null Menu");
    if (address == null) 
      throw new NullPointerException("Null address");    
  }

  public RestaurantMenu getMenu() {
    return menu;
  }

  public void setMenu(RestaurantMenu menu) {
    this.menu = menu;
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
}


import io.eventuate.tram.events.common.DomainEvent;

public interface RestaurantDomainEvent extends DomainEvent {
}


public class CreateRestaurantResponse {
  private long id;

  public CreateRestaurantResponse() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public CreateRestaurantResponse(long id) {
    this.id = id;
  }
}


public class GetRestaurantResponse {
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public GetRestaurantResponse(String name) {
    this.name = name;

  }
}


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.eventuate.common.json.mapper.JSonMapper;
import net.chrisrichardson.ftgo.restaurantservice.aws.ApiGatewayResponse;
import net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantService;
import net.chrisrichardson.ftgo.restaurantservice.domain.CreateRestaurantRequest;
import net.chrisrichardson.ftgo.restaurantservice.web.CreateRestaurantResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static net.chrisrichardson.ftgo.restaurantservice.aws.ApiGatewayResponse.applicationJsonHeaders;

@Configuration
@Import(RestaurantServiceLambdaConfiguration.class)
public class CreateRestaurantRequestHandler extends AbstractAutowiringHttpRequestHandler {

  @Autowired
  private RestaurantService restaurantService;

  @Override
  protected Class<?> getApplicationContextClass() {
    return CreateRestaurantRequestHandler.class;
  }

  @Override
  protected APIGatewayProxyResponseEvent handleHttpRequest(APIGatewayProxyRequestEvent request, Context context) {

    CreateRestaurantRequest crr = JSonMapper.fromJson(request.getBody(), CreateRestaurantRequest.class);

    Restaurant rest = restaurantService.create(crr);

    return ApiGatewayResponse.builder()
            .setStatusCode(200)
            .setObjectBody(new CreateRestaurantResponse(rest.getId()))
            .setHeaders(applicationJsonHeaders())
            .build();

  }
}


import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantServiceDomainConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RestaurantServiceDomainConfiguration.class, TramMessageProducerJdbcConfiguration.class})
@EnableAutoConfiguration
public class RestaurantServiceLambdaConfiguration {
}


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import net.chrisrichardson.ftgo.restaurantservice.aws.ApiGatewayResponse;
import net.chrisrichardson.ftgo.restaurantservice.aws.AwsLambdaError;
import net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant;
import net.chrisrichardson.ftgo.restaurantservice.domain.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static net.chrisrichardson.ftgo.restaurantservice.aws.ApiGatewayResponse.applicationJsonHeaders;
import static net.chrisrichardson.ftgo.restaurantservice.aws.ApiGatewayResponse.buildErrorResponse;

public class FindRestaurantRequestHandler extends AbstractAutowiringHttpRequestHandler {

  @Autowired
  private RestaurantService restaurantService;

  @Override
  protected Class<?> getApplicationContextClass() {
    return CreateRestaurantRequestHandler.class;
  }

  @Override
  protected APIGatewayProxyResponseEvent handleHttpRequest(APIGatewayProxyRequestEvent request, Context context) {
    long restaurantId;
    try {
      restaurantId = Long.parseLong(request.getPathParameters().get("restaurantId"));
    } catch (NumberFormatException e) {
      return makeBadRequestResponse(context);
    }

    Optional<Restaurant> possibleRestaurant = restaurantService.findById(restaurantId);

    return possibleRestaurant
            .map(this::makeGetRestaurantResponse)
            .orElseGet(() -> makeRestaurantNotFoundResponse(context, restaurantId));

  }

  private APIGatewayProxyResponseEvent makeBadRequestResponse(Context context) {
    return buildErrorResponse(new AwsLambdaError(
            "Bad response",
            "400",
            context.getAwsRequestId(),
            "bad response"));
  }

  private APIGatewayProxyResponseEvent makeRestaurantNotFoundResponse(Context context, long restaurantId) {
    return buildErrorResponse(new AwsLambdaError(
                    "No entity found",
                    "404",
                    context.getAwsRequestId(),
                    "Found no restaurant with id " + restaurantId));
  }

  private  APIGatewayProxyResponseEvent makeGetRestaurantResponse(Restaurant restaurant) {
    return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(new GetRestaurantResponse(restaurant.getName()))
                    .setHeaders(applicationJsonHeaders())
                    .build();
  }


}


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import net.chrisrichardson.ftgo.restaurantservice.aws.AbstractHttpHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractAutowiringHttpRequestHandler extends AbstractHttpHandler {

  private static ConfigurableApplicationContext ctx;
  private ReentrantReadWriteLock ctxLock = new ReentrantReadWriteLock();
  private boolean autowired = false;

  protected synchronized ApplicationContext getAppCtx() {
    ctxLock.writeLock().lock();
    try {
      if (ctx == null) {
        ctx =  SpringApplication.run(getApplicationContextClass());
      }
      return ctx;
    } finally {
      ctxLock.writeLock().unlock();
    }
  }

  protected abstract Class<?> getApplicationContextClass();

  @Override
  protected void beforeHandling(APIGatewayProxyRequestEvent request, Context context) {
    super.beforeHandling(request, context);
    if (!autowired) {
      getAppCtx().getAutowireCapableBeanFactory().autowireBean(this);
      autowired = true;
    }
  }
}


import io.eventuate.tram.events.publisher.DomainEventPublisher;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Transactional
public class RestaurantService {


  private RestaurantRepository restaurantRepository;

  @Autowired
  private DomainEventPublisher domainEventPublisher;


  public RestaurantService() {
  }

  public RestaurantService(RestaurantRepository restaurantRepository) {
    this.restaurantRepository = restaurantRepository;
  }



  public Restaurant create(CreateRestaurantRequest request) {
    Restaurant restaurant = new Restaurant(request.getName(), request.getMenu());
    restaurantRepository.save(restaurant);
    domainEventPublisher.publish(Restaurant.class, restaurant.getId(), Collections.singletonList(new RestaurantCreated(request.getName(), request.getAddress(), request.getMenu())));
    return restaurant;
  }

  public Optional<Restaurant> findById(long restaurantId) {
    return restaurantRepository.findById(restaurantId);
  }
}


import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@Configuration
@EntityScan
@Import({TramEventsPublisherConfiguration.class, CommonConfiguration.class})
public class RestaurantServiceDomainConfiguration {

  @Bean
  public RestaurantService restaurantService(JpaRepositoryFactoryBean restaurantRepository) {
    return new RestaurantService((RestaurantRepository) restaurantRepository.getObject());
  }

  @Bean
  public JpaRepositoryFactoryBean<RestaurantRepository, Restaurant, Long> restaurantRepository() {
    return new JpaRepositoryFactoryBean<>(RestaurantRepository.class);
  }

}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.List;

@Embeddable
@Access(AccessType.FIELD)
public class RestaurantMenu {


  @ElementCollection
  private List<MenuItem> menuItems;

  private RestaurantMenu() {
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public void setMenuItems(List<MenuItem> menuItems) {
    this.menuItems = menuItems;
  }

  public RestaurantMenu(List<MenuItem> menuItems) {

    this.menuItems = menuItems;
  }

}


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "restaurants")
@Access(AccessType.FIELD)
public class Restaurant {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Embedded
  private RestaurantMenu menu;

  private Restaurant() {
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


  public Restaurant(String name, RestaurantMenu menu) {
    this.name = name;
    this.menu = menu;
  }


  public Long getId() {
    return id;
  }
}


import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)
public class MenuItem {

  private String id;
  private String name;
  private Money price;

  private MenuItem() {
  }

  public MenuItem(String id, String name, Money price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(Money price) {
    this.price = price;
  }
}


import net.chrisrichardson.ftgo.common.Address;

public class CreateRestaurantRequest {

  private String name;
  private Address address;
  private RestaurantMenu menu;

  private CreateRestaurantRequest() {

  }

  public CreateRestaurantRequest(String name, Address address, RestaurantMenu menu) {
    this.name = name;
    this.address = address;
    this.menu = menu;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RestaurantMenu getMenu() {
    return menu;
  }

  public void setMenu(RestaurantMenu menu) {
    this.menu = menu;
  }

  public Address getAddress() {
    return address;
  }
}


import org.springframework.data.repository.CrudRepository;

public interface RestaurantRepository extends CrudRepository<Restaurant, Long> {
}


public class RequestContext {

  private String accountId;
  private String resourceId;
  private String stage;
  private String requestId;
  private Identity identity;
  private String resourcePath;
  private String httpMethod;
  private String apiId;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getStage() {
    return stage;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public Identity getIdentity() {
    return identity;
  }

  public void setIdentity(Identity identity) {
    this.identity = identity;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public void setResourcePath(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getApiId() {
    return apiId;
  }

  public void setApiId(String apiId) {
    this.apiId = apiId;
  }

}

public class AwsLambdaError {
  private String type;
  private String code;
  private String requestId;
  private String message;

  public AwsLambdaError() {
  }

  public AwsLambdaError(String type, String code, String requestId, String message) {
    this.type = type;
    this.code = code;
    this.requestId = requestId;
    this.message = message;
  }

  public String getType() {
    return type;
  }

  public String getCode() {
    return code;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getMessage() {
    return message;
  }
}


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.eventuate.common.json.mapper.JSonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiGatewayResponse {
  private final int statusCode;
  private final String body;
  private final Map<String, String> headers;
  private final boolean isBase64Encoded;

  public ApiGatewayResponse(int statusCode, String body, Map<String, String> headers, boolean isBase64Encoded) {
    this.statusCode = statusCode;
    this.body = body;
    this.headers = headers;
    this.isBase64Encoded = isBase64Encoded;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getBody() {
    return body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  // API Gateway expects the property to be called "isBase64Encoded" => isIs
  public boolean isIsBase64Encoded() {
    return isBase64Encoded;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int statusCode = 200;
    private Map<String, String> headers = Collections.emptyMap();
    private String rawBody;
    private Object objectBody;
    private byte[] binaryBody;
    private boolean base64Encoded;

    public Builder setStatusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public Builder setHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public Builder setRawBody(String rawBody) {
      this.rawBody = rawBody;
      return this;
    }

    public Builder setObjectBody(Object objectBody) {
      this.objectBody = objectBody;
      return this;
    }

    public Builder setBinaryBody(byte[] binaryBody) {
      this.binaryBody = binaryBody;
      setBase64Encoded(true);
      return this;
    }

    public Builder setBase64Encoded(boolean base64Encoded) {
      this.base64Encoded = base64Encoded;
      return this;
    }

    public APIGatewayProxyResponseEvent build() {
      String body = null;
      if (rawBody != null) {
        body = rawBody;
      } else if (objectBody != null) {
        body = JSonMapper.toJson(objectBody);
      } else if (binaryBody != null) {
        body = new String(Base64.getEncoder().encode(binaryBody), StandardCharsets.UTF_8);
      }
      APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
      response.setStatusCode(statusCode);
      response.setBody(body);
      response.setHeaders(headers);
      return response;
    }
  }

  public static APIGatewayProxyResponseEvent buildErrorResponse(AwsLambdaError error) {
    return ApiGatewayResponse.builder()
            .setStatusCode(Integer.valueOf(error.getCode()))
            .setObjectBody(error)
            .setHeaders(applicationJsonHeaders())
            .build();
  }

  public static Map<String, String> applicationJsonHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    return headers;
  }
}

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Map;
import java.util.Optional;

public class ApiGatewayRequest {

  private String resource;
  private String path;
  private String httpMethod;
  private Map<String, String> headers;
  private Map<String, String> queryStringParameters;
  private Map<String, String> pathParameters;
  private Map<String, String> stageVariables;
  private RequestContext requestContext;
  private String body;
  private boolean isBase64Encoded;

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public Map<String, String> getQueryStringParameters() {
    return queryStringParameters;
  }

  public void setQueryStringParameters(Map<String, String> queryStringParameters) {
    this.queryStringParameters = queryStringParameters;
  }

  public Map<String, String> getPathParameters() {
    return pathParameters;
  }

  public void setPathParameters(Map<String, String> pathParameters) {
    this.pathParameters = pathParameters;
  }

  public Map<String, String> getStageVariables() {
    return stageVariables;
  }

  public void setStageVariables(Map<String, String> stageVariables) {
    this.stageVariables = stageVariables;
  }

  public RequestContext getRequestContext() {
    return requestContext;
  }

  public void setRequestContext(RequestContext requestContext) {
    this.requestContext = requestContext;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public boolean isBase64Encoded() {
    return isBase64Encoded;
  }

  public void setBase64Encoded(boolean base64Encoded) {
    isBase64Encoded = base64Encoded;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public String getPathParam(String paramName) {
    return Optional.ofNullable(this.getPathParameters())
            .map(paramsMap -> paramsMap.get(paramName))
            .orElse(null);
  }
}

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.chrisrichardson.ftgo.restaurantservice.aws.ApiGatewayResponse.buildErrorResponse;


public abstract class AbstractHttpHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    log.debug("Got request: {}", input);
    try {
      beforeHandling(input, context);
      return handleHttpRequest(input, context);
    } catch (Exception e) {
      log.error("Error handling request id: {}", context.getAwsRequestId(), e);
      return buildErrorResponse(new AwsLambdaError(
              "Internal Server Error",
              "500",
              context.getAwsRequestId(),
              "Error handling request: " + context.getAwsRequestId() + " " + input.toString()));
    }
  }

  protected void beforeHandling(APIGatewayProxyRequestEvent request, Context context) {
    // do nothing
  }

  protected abstract APIGatewayProxyResponseEvent handleHttpRequest(APIGatewayProxyRequestEvent request, Context context);
}


public class Identity {

  private String cognitoIdentityPoolId;
  private String accountId;
  private String cognitoIdentityId;
  private String caller;
  private String apiKey;
  private String sourceIp;
  private String cognitoAuthenticationType;
  private String cognitoAuthenticationProvider;
  private String userArn;
  private String userAgent;
  private String user;

  public String getCognitoIdentityPoolId() {
    return cognitoIdentityPoolId;
  }

  public void setCognitoIdentityPoolId(String cognitoIdentityPoolId) {
    this.cognitoIdentityPoolId = cognitoIdentityPoolId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getCognitoIdentityId() {
    return cognitoIdentityId;
  }

  public void setCognitoIdentityId(String cognitoIdentityId) {
    this.cognitoIdentityId = cognitoIdentityId;
  }

  public String getCaller() {
    return caller;
  }

  public void setCaller(String caller) {
    this.caller = caller;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getSourceIp() {
    return sourceIp;
  }

  public void setSourceIp(String sourceIp) {
    this.sourceIp = sourceIp;
  }

  public String getCognitoAuthenticationType() {
    return cognitoAuthenticationType;
  }

  public void setCognitoAuthenticationType(String cognitoAuthenticationType) {
    this.cognitoAuthenticationType = cognitoAuthenticationType;
  }

  public String getCognitoAuthenticationProvider() {
    return cognitoAuthenticationProvider;
  }

  public void setCognitoAuthenticationProvider(String cognitoAuthenticationProvider) {
    this.cognitoAuthenticationProvider = cognitoAuthenticationProvider;
  }

  public String getUserArn() {
    return userArn;
  }

  public void setUserArn(String userArn) {
    this.userArn = userArn;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }
}


import io.eventuate.sync.AggregateRepository;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;
import net.chrisrichardson.ftgo.accountingservice.domain.*;
import net.chrisrichardson.ftgo.accountservice.api.AccountDisabledReply;
import net.chrisrichardson.ftgo.accountservice.api.AuthorizeCommand;
import net.chrisrichardson.ftgo.accountservice.api.ReverseAuthorizationCommand;
import net.chrisrichardson.ftgo.accountservice.api.ReviseAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.sagas.eventsourcingsupport.UpdatingOptionsBuilder.replyingTo;

public class AccountingServiceCommandHandler {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private AggregateRepository<Account, AccountCommand> accountRepository;

  public CommandHandlers commandHandlers() {
    return SagaCommandHandlersBuilder
            .fromChannel("accountingService")
            .onMessage(AuthorizeCommand.class, this::authorize)
            .onMessage(ReverseAuthorizationCommand.class, this::reverseAuthorization)
            .onMessage(ReviseAuthorization.class, this::reviseAuthorization)
            .build();
  }

  public void authorize(CommandMessage<AuthorizeCommand> cm) {

    AuthorizeCommand command = cm.getCommand();

    accountRepository.update(Long.toString(command.getConsumerId()),
            makeAuthorizeCommandInternal(command),
            replyingTo(cm)
                    .catching(AccountDisabledException.class, () -> withFailure(new AccountDisabledReply()))
                    .build());

  }

  public void reverseAuthorization(CommandMessage<ReverseAuthorizationCommand> cm) {

    ReverseAuthorizationCommand command = cm.getCommand();

    accountRepository.update(Long.toString(command.getConsumerId()),
            makeReverseAuthorizeCommandInternal(command),
            replyingTo(cm)
                    .catching(AccountDisabledException.class, () -> withFailure(new AccountDisabledReply()))
                    .build());

  }
  public void reviseAuthorization(CommandMessage<ReviseAuthorization> cm) {

    ReviseAuthorization command = cm.getCommand();

    accountRepository.update(Long.toString(command.getConsumerId()),
            makeReviseAuthorizeCommandInternal(command),
            replyingTo(cm)
                    .catching(AccountDisabledException.class, () -> withFailure(new AccountDisabledReply()))
                    .build());


  }

  private AuthorizeCommandInternal makeAuthorizeCommandInternal(AuthorizeCommand command) {
    return new AuthorizeCommandInternal(Long.toString(command.getConsumerId()), Long.toString(command.getOrderId()), command.getOrderTotal());
  }
  private ReverseAuthorizationCommandInternal makeReverseAuthorizeCommandInternal(ReverseAuthorizationCommand command) {
    return new ReverseAuthorizationCommandInternal(Long.toString(command.getConsumerId()), Long.toString(command.getOrderId()), command.getOrderTotal());
  }
  private ReviseAuthorizationCommandInternal makeReviseAuthorizeCommandInternal(ReviseAuthorization command) {
    return new ReviseAuthorizationCommandInternal(Long.toString(command.getConsumerId()), Long.toString(command.getOrderId()), command.getOrderTotal());
  }

}


import io.eventuate.javaclient.spring.EnableEventHandlers;
import io.eventuate.tram.commands.consumer.CommandDispatcher;
import io.eventuate.tram.commands.consumer.CommandDispatcherFactory;
import io.eventuate.tram.spring.commands.consumer.TramCommandConsumerConfiguration;
import io.eventuate.tram.spring.consumer.jdbc.TransactionalNoopDuplicateMessageDetectorConfiguration;
import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import io.eventuate.tram.sagas.eventsourcingsupport.SagaReplyRequestedEventSubscriber;
import net.chrisrichardson.ftgo.accountingservice.domain.Account;
import net.chrisrichardson.ftgo.accountingservice.domain.AccountServiceConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;

@Configuration
@EnableEventHandlers
@Import({AccountServiceConfiguration.class, CommonConfiguration.class, TramEventSubscriberConfiguration.class, TramCommandConsumerConfiguration.class, TransactionalNoopDuplicateMessageDetectorConfiguration.class})
public class AccountingMessagingConfiguration {

  @Bean
  public AccountingEventConsumer accountingEventConsumer() {
    return new AccountingEventConsumer();
  }

  @Bean
  public DomainEventDispatcher domainEventDispatcher(AccountingEventConsumer accountingEventConsumer, DomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("accountingServiceDomainEventDispatcher", accountingEventConsumer.domainEventHandlers());
  }

  @Bean
  public AccountingServiceCommandHandler accountCommandHandler() {
    return new AccountingServiceCommandHandler();
  }


  @Bean
  public CommandDispatcher commandDispatcher(AccountingServiceCommandHandler target,
                                             AccountServiceChannelConfiguration data, CommandDispatcherFactory commandDispatcherFactory) {
    return commandDispatcherFactory.make(data.getCommandDispatcherId(), target.commandHandlers());
  }

  @Bean
  public AccountServiceChannelConfiguration accountServiceChannelConfiguration() {
    return new AccountServiceChannelConfiguration("accountCommandDispatcher", "accountCommandChannel");
  }

  @Bean
  public SagaReplyRequestedEventSubscriber sagaReplyRequestedEventSubscriber() {
    return new SagaReplyRequestedEventSubscriber("accountingServiceSagaReplyRequestedEventSubscriber", Collections.singleton(Account.class.getName()));
  }

}


public class AccountServiceChannelConfiguration {
  private String commandDispatcherId;
  private String commandChannel;

  public AccountServiceChannelConfiguration(String commandDispatcherId, String commandChannel) {
    this.commandDispatcherId = commandDispatcherId;
    this.commandChannel = commandChannel;
  }

  public String getCommandDispatcherId() {
    return commandDispatcherId;
  }

  public String getCommandChannel() {
    return commandChannel;
  }
}


import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import net.chrisrichardson.ftgo.accountingservice.domain.AccountingService;
import net.chrisrichardson.ftgo.consumerservice.domain.ConsumerCreated;
import org.springframework.beans.factory.annotation.Autowired;


public class AccountingEventConsumer {

  @Autowired
  private AccountingService accountingService;

  public DomainEventHandlers domainEventHandlers() {
    return DomainEventHandlersBuilder
            .forAggregateType("net.chrisrichardson.ftgo.consumerservice.domain.Consumer")
            .onEvent(ConsumerCreated.class, this::createAccount) // TODO this is hack to get the correct package
            .build();
  }

  private void createAccount(DomainEventEnvelope<ConsumerCreated> dee) {
    accountingService.create(dee.getAggregateId());
  }


}


import io.eventuate.EntityNotFoundException;
import io.eventuate.sync.AggregateRepository;
import net.chrisrichardson.ftgo.accountingservice.domain.Account;
import net.chrisrichardson.ftgo.accountingservice.domain.AccountCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/accounts")
public class AccountsController {

  @Autowired
  private AggregateRepository<Account, AccountCommand> accountRepository;

  @RequestMapping(path="/{accountId}", method= RequestMethod.GET)
  public ResponseEntity<GetAccountResponse> getAccount(@PathVariable String accountId) {
       try {
          return new ResponseEntity<>(new GetAccountResponse(accountId), HttpStatus.OK);
       } catch (EntityNotFoundException e) {
         return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
       }
  }

}


public class GetAccountResponse {
  private String accountId;

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public GetAccountResponse() {

  }

  public GetAccountResponse(String accountId) {
    this.accountId = accountId;
  }
}


import net.chrisrichardson.ftgo.accountingservice.domain.AccountServiceConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AccountServiceConfiguration.class)
@ComponentScan
public class AccountingWebConfiguration {
}


public class CreateAccountCommand implements AccountCommand {
}


import io.eventuate.Event;

public class AccountAuthorizedEvent implements Event {
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;


public class ReverseAuthorizationCommandInternal implements AccountCommand, Command {
  private String consumerId;
  private String orderId;
  private Money orderTotal;

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public ReverseAuthorizationCommandInternal(String consumerId, String orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  private ReverseAuthorizationCommandInternal() {
  }

  public String getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(String consumerId) {
    this.consumerId = consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }
}


import io.eventuate.Event;

public class AccountCreatedEvent implements Event {
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;

public class AuthorizeCommandInternal implements AccountCommand, Command {
  private String consumerId;
  private String orderId;
  private Money orderTotal;

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public AuthorizeCommandInternal(String consumerId, String orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  private AuthorizeCommandInternal() {
  }

  public String getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(String consumerId) {
    this.consumerId = consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }
}


public class AccountDisabledException extends RuntimeException {
}


import io.eventuate.Event;
import io.eventuate.ReflectiveMutableCommandProcessingAggregate;
import io.eventuate.tram.sagas.eventsourcingsupport.SagaReplyRequestedEvent;

import java.util.Collections;
import java.util.List;

import static io.eventuate.EventUtil.events;

public class Account extends ReflectiveMutableCommandProcessingAggregate<Account, AccountCommand> {

  public List<Event> process(CreateAccountCommand command) {
    return events(new AccountCreatedEvent());
  }

  public void apply(AccountCreatedEvent event) {

  }


  public List<Event> process(AuthorizeCommandInternal command) {
    return events(new AccountAuthorizedEvent());
  }

  public List<Event> process(ReverseAuthorizationCommandInternal command) {
    return Collections.emptyList();
  }
  public List<Event> process(ReviseAuthorizationCommandInternal command) {
    return Collections.emptyList();
  }

  public void apply(AccountAuthorizedEvent event) {

  }

  public void apply(SagaReplyRequestedEvent event) {
    // TODO - need a way to not need this method
  }


}


import io.eventuate.Command;

public interface AccountCommand extends Command {
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;


public class ReviseAuthorizationCommandInternal implements AccountCommand, Command {
  private String consumerId;
  private String orderId;
  private Money orderTotal;

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public ReviseAuthorizationCommandInternal(String consumerId, String orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  private ReviseAuthorizationCommandInternal() {
  }

  public String getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(String consumerId) {
    this.consumerId = consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }
}


import io.eventuate.sync.AggregateRepository;
import io.eventuate.sync.EventuateAggregateStore;
import io.eventuate.tram.spring.commands.producer.TramCommandProducerConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TramCommandProducerConfiguration.class, CommonConfiguration.class})
public class AccountServiceConfiguration {


  @Bean
  public AggregateRepository<Account, AccountCommand> accountRepositorySync(EventuateAggregateStore aggregateStore) {
    return new AggregateRepository<>(Account.class, aggregateStore);
  }

  @Bean
  public AccountingService accountingService() {
    return new AccountingService();
  }
}


import io.eventuate.sync.AggregateRepository;
import io.eventuate.EntityWithIdAndVersion;
import io.eventuate.SaveOptions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AccountingService {
  @Autowired
  private AggregateRepository<Account, AccountCommand> accountRepository;

  public void create(String aggregateId) {
    EntityWithIdAndVersion<Account> account = accountRepository.save(new CreateAccountCommand(),
            Optional.of(new SaveOptions().withId(aggregateId)));
  }
}


import io.eventuate.Event;

public class AccountAuthorizationFailed implements Event {
}


import io.eventuate.local.java.spring.javaclient.driver.EventuateDriverConfiguration;
import io.eventuate.tram.spring.commands.producer.TramCommandProducerConfiguration;
import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import net.chrisrichardson.eventstore.examples.customersandorders.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.ftgo.accountingservice.messaging.AccountingMessagingConfiguration;
import net.chrisrichardson.ftgo.accountingservice.web.AccountingWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({AccountingMessagingConfiguration.class, AccountingWebConfiguration.class,
        TramCommandProducerConfiguration.class,
        EventuateDriverConfiguration.class,
        TramJdbcKafkaConfiguration.class,
        CommonSwaggerConfiguration.class})
public class AccountingServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(AccountingServiceMain.class, args);
  }
}


import io.eventuate.tram.events.common.DomainEvent;

public class ConsumerCreated implements DomainEvent {
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;

public class AuthorizeCommand implements Command {
  private long consumerId;
  private Long orderId;
  private Money orderTotal;

  private AuthorizeCommand() {
  }

  public AuthorizeCommand(long consumerId, Long orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public Long getOrderId() {

    return orderId;

  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }
}



import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;

public class ReverseAuthorizationCommand implements Command {
  private long consumerId;
  private Long orderId;
  private Money orderTotal;

  private ReverseAuthorizationCommand() {
  }

  public ReverseAuthorizationCommand(long consumerId, Long orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public Long getOrderId() {

    return orderId;

  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }
}



public class AccountingServiceChannels {

  public static String accountingServiceChannel = "accountingService";

}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;

public class ReviseAuthorization implements Command {
  private long consumerId;
  private Long orderId;
  private Money orderTotal;

  private ReviseAuthorization() {
  }

  public ReviseAuthorization(long consumerId, Long orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public Long getOrderId() {

    return orderId;

  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }
}


public class AccountDisabledReply {
}


import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.common.spring.jdbc.EventuateTransactionTemplateConfiguration;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistory;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryDao;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryFilter;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderLineItem;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {OrderHistoryDaoDynamoDbTest.OrderHistoryDaoDynamoDbTestConfiguration.class})
public class OrderHistoryDaoDynamoDbTest {

  @Configuration
  @EnableAutoConfiguration
  @ComponentScan
  @Import({OrderHistoryDynamoDBConfiguration.class, TramInMemoryConfiguration.class, EventuateTransactionTemplateConfiguration.class})
  static public class OrderHistoryDaoDynamoDbTestConfiguration {

  }

  private String consumerId;
  private Order order1;
  private String orderId;
  @Autowired
  private OrderHistoryDao dao;
  private String restaurantName;
  private String chickenVindaloo;
  private Optional<SourceEvent> eventSource;
  private long restaurantId;

  @Before
  public void setup() {
    consumerId = "consumerId" + System.currentTimeMillis();
    orderId = "orderId" + System.currentTimeMillis();
    restaurantName = "Ajanta" + System.currentTimeMillis();
    chickenVindaloo = "Chicken Vindaloo" + System.currentTimeMillis();
    restaurantId = 101L;

    order1 = new Order(orderId, consumerId, OrderState.APPROVAL_PENDING, singletonList(new OrderLineItem("-1", chickenVindaloo, Money.ZERO, 0)), null, restaurantId, restaurantName);
    order1.setCreationDate(DateTime.now().minusDays(5));
    eventSource = Optional.of(new SourceEvent("Order", orderId, "11212-34343"));

    dao.addOrder(order1, eventSource);
  }

  @Test
  public void shouldFindOrder() {
    Optional<Order> order = dao.findOrder(orderId);
    assertOrderEquals(order1, order.get());
  }

  @Test
  public void shouldIgnoreDuplicateAdd() {
    dao.updateOrderState(orderId, OrderState.CANCELLED, Optional.empty());
    assertFalse(dao.addOrder(order1, eventSource));
    Optional<Order> order = dao.findOrder(orderId);
    assertEquals(OrderState.CANCELLED, order.get().getStatus());
  }

  @Test
  public void shouldFindOrders() {
    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter());
    assertNotNull(result);
    List<Order> orders = result.getOrders();
    Order retrievedOrder = assertContainsOrderId(orderId, orders);
    assertOrderEquals(order1, retrievedOrder);
  }

  private void assertOrderEquals(Order expected, Order other) {
    System.out.println("Expected=" + JSonMapper.toJson(expected.getLineItems()));
    System.out.println("actual  =" + JSonMapper.toJson(other.getLineItems()));
    assertEquals(expected.getLineItems(), other.getLineItems());
    assertEquals(expected.getStatus(), other.getStatus());
    assertEquals(expected.getCreationDate(), other.getCreationDate());
    assertEquals(expected.getRestaurantId(), other.getRestaurantId());
    assertEquals(expected.getRestaurantName(), other.getRestaurantName());
  }


  @Test
  public void shouldFindOrdersWithStatus() throws InterruptedException {
    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withStatus(OrderState.APPROVAL_PENDING));
    assertNotNull(result);
    List<Order> orders = result.getOrders();
    assertContainsOrderId(orderId, orders);
  }

  @Test
  public void shouldCancel() throws InterruptedException {
    dao.updateOrderState(orderId, OrderState.CANCELLED, Optional.of(new SourceEvent("a", "b", "c")));
    Order order = dao.findOrder(orderId).get();
    assertEquals(OrderState.CANCELLED, order.getStatus());
  }

  @Test
  public void shouldHandleCancel() throws InterruptedException {
    assertTrue(dao.updateOrderState(orderId, OrderState.CANCELLED, Optional.of(new SourceEvent("a", "b", "c"))));
    assertFalse(dao.updateOrderState(orderId, OrderState.CANCELLED, Optional.of(new SourceEvent("a", "b", "c"))));
  }

  @Test
  public void shouldFindOrdersWithCancelledStatus() {
    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withStatus(OrderState.CANCELLED));
    assertNotNull(result);
    List<Order> orders = result.getOrders();
    assertNotContainsOrderId(orderId, orders);
  }

  // FIXME
//  @Test
//  public void shouldFindOrderByRestaurantName() {
//    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withKeywords(singleton(restaurantName)));
//    assertNotNull(result);
//    List<Order> orders = result.getOrders();
//    assertContainsOrderId(orderId, orders);
//  }

  @Test
  public void shouldFindOrderByMenuItem() {
    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withKeywords(singleton(chickenVindaloo)));
    assertNotNull(result);
    List<Order> orders = result.getOrders();
    assertContainsOrderId(orderId, orders);
  }


  @Test
  public void shouldReturnOrdersSorted() {
    String orderId2 = "orderId" + System.currentTimeMillis();
    Order order2 = new Order(orderId2, consumerId, OrderState.APPROVAL_PENDING, singletonList(new OrderLineItem("-1", "Lamb 65", Money.ZERO, -1)), null, restaurantId, restaurantName);
    order2.setCreationDate(DateTime.now().minusDays(1));
    dao.addOrder(order2, eventSource);
    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter());
    List<Order> orders = result.getOrders();

    int idx1 = indexOf(orders, orderId);
    int idx2 = indexOf(orders, orderId2);
    assertTrue(idx2 < idx1);
  }

  private int indexOf(List<Order> orders, String orderId2) {
    Order order = orders.stream().filter(o -> o.getOrderId().equals(orderId2)).findFirst().get();
    return orders.indexOf(order);
  }

  private Order assertContainsOrderId(String orderId, List<Order> orders) {
    Optional<Order> order = orders.stream().filter(o -> o.getOrderId().equals(orderId)).findFirst();
    assertTrue("Order not found", order.isPresent());
    return order.get();
  }

  private void assertNotContainsOrderId(String orderId, List<Order> orders) {
    Optional<Order> order = orders.stream().filter(o -> o.getOrderId().equals(orderId)).findFirst();
    assertFalse(order.isPresent());
  }

  @Test
  public void shouldPaginateResults() {
    String orderId2 = "orderId" + System.currentTimeMillis();
    Order order2 = new Order(orderId2, consumerId, OrderState.APPROVAL_PENDING, singletonList(new OrderLineItem("-1", "Lamb 65", Money.ZERO, -1)), null, restaurantId, restaurantName);
    order2.setCreationDate(DateTime.now().minusDays(1));
    dao.addOrder(order2, eventSource);

    OrderHistory result = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withPageSize(1));

    assertEquals(1, result.getOrders().size());
    assertTrue(result.getStartKey().isPresent());

    OrderHistory result2 = dao.findOrderHistory(consumerId, new OrderHistoryFilter().withPageSize(1).withStartKeyToken(result.getStartKey()));

    assertEquals(1, result.getOrders().size());

  }

}

import io.eventuate.tram.events.common.DomainEvent;

public class DeliveryPickedUp implements DomainEvent {
  private String orderId;

  public String getOrderId() {
    return orderId;
  }
}


import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;

public class OrderHistoryFilter {
  private DateTime since = DateTime.now().minusDays(30);
  private Optional<OrderState> status = Optional.empty();
  private Set<String> keywords = emptySet();
  private Optional<String> startKeyToken = Optional.empty();
  private Optional<Integer> pageSize = Optional.empty();

  public DateTime getSince() {
    return since;
  }

  public OrderHistoryFilter withStatus(OrderState status) {
    this.status = Optional.of(status);
    return this;
  }

  public Optional<OrderState> getStatus() {
    return status;
  }


  public OrderHistoryFilter withStartKeyToken(Optional<String> startKeyToken) {
    this.startKeyToken = startKeyToken;
    return this;
  }

  public OrderHistoryFilter withKeywords(Set<String> keywords) {
    this.keywords = keywords;
    return this;
  }


  public Set<String> getKeywords() {
    return keywords;
  }

  public Optional<String> getStartKeyToken() {
    return startKeyToken;
  }

  public OrderHistoryFilter withPageSize(int pageSize) {
    this.pageSize = Optional.of(pageSize);
    return this;
  }

  public Optional<Integer> getPageSize() {
    return pageSize;
  }
}


public class Location {
}



import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.SourceEvent;
import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.Order;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;

import java.util.Optional;

public interface OrderHistoryDao {

  boolean addOrder(Order order, Optional<SourceEvent> eventSource);

  OrderHistory findOrderHistory(String consumerId, OrderHistoryFilter filter);

  boolean updateOrderState(String orderId, OrderState newState, Optional<SourceEvent> eventSource);

  void noteTicketPreparationStarted(String orderId);

  void noteTicketPreparationCompleted(String orderId);

  void notePickedUp(String orderId, Optional<SourceEvent> eventSource);

  void updateLocation(String orderId, Location location);

  void noteDelivered(String orderId);

  Optional<Order> findOrder(String orderId);

}


import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.Order;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class OrderHistory {
  private List<Order> orders;
  private Optional<String> startKey;

  public OrderHistory(List<Order> orders, Optional<String> startKey) {
    this.orders = orders;
    this.startKey = startKey;
  }

  public List<Order> getOrders() {
    return orders;
  }

  public Optional<String> getStartKey() {
    return startKey;
  }
}


import io.eventuate.tram.spring.consumer.common.TramNoopDuplicateMessageDetectorConfiguration;
import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CommonConfiguration.class, TramNoopDuplicateMessageDetectorConfiguration.class, TramEventSubscriberConfiguration.class})
public class OrderHistoryServiceMessagingConfiguration {

  @Bean
  public OrderHistoryEventHandlers orderHistoryEventHandlers(OrderHistoryDao orderHistoryDao) {
    return new OrderHistoryEventHandlers(orderHistoryDao);
  }

  @Bean
  public DomainEventDispatcher orderHistoryDomainEventDispatcher(OrderHistoryEventHandlers orderHistoryEventHandlers, DomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("orderHistoryDomainEventDispatcher", orderHistoryEventHandlers.domainEventHandlers());
  }

}


import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import net.chrisrichardson.ftgo.cqrs.orderhistory.DeliveryPickedUp;
import net.chrisrichardson.ftgo.cqrs.orderhistory.Location;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryDao;
import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.Order;
import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.SourceEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class OrderHistoryEventHandlers {

  private OrderHistoryDao orderHistoryDao;

  public OrderHistoryEventHandlers(OrderHistoryDao orderHistoryDao) {
    this.orderHistoryDao = orderHistoryDao;
  }

  private Logger logger = LoggerFactory.getLogger(getClass());

  // TODO - determine events

  private String orderId;
  private Order order;
  private Location location; //

  public DomainEventHandlers domainEventHandlers() {
    return DomainEventHandlersBuilder
            .forAggregateType("net.chrisrichardson.ftgo.orderservice.domain.Order")
            .onEvent(OrderCreatedEvent.class, this::handleOrderCreated)
            .onEvent(OrderAuthorized.class, this::handleOrderAuthorized)
            .onEvent(OrderCancelled.class, this::handleOrderCancelled)
            .onEvent(OrderRejected.class, this::handleOrderRejected)
//            .onEvent(DeliveryPickedUp.class, this::handleDeliveryPickedUp)
            .build();
  }

  private Optional<SourceEvent> makeSourceEvent(DomainEventEnvelope<?> dee) {
    return Optional.of(new SourceEvent(dee.getAggregateType(),
            dee.getAggregateId(), dee.getEventId()));
  }

  public void handleOrderCreated(DomainEventEnvelope<OrderCreatedEvent> dee) {
    logger.debug("handleOrderCreated called {}", dee);
    boolean result = orderHistoryDao.addOrder(makeOrder(dee.getAggregateId(), dee.getEvent()), makeSourceEvent(dee));
    logger.debug("handleOrderCreated result {} {}", dee, result);
  }

  public void handleOrderAuthorized(DomainEventEnvelope<OrderAuthorized> dee) {
    logger.debug("handleOrderAuthorized called {}", dee);
    boolean result = orderHistoryDao.updateOrderState(dee.getAggregateId(), OrderState.APPROVED, makeSourceEvent(dee));
    logger.debug("handleOrderAuthorized result {} {}", dee, result);
  }

  public void handleOrderCancelled(DomainEventEnvelope<OrderCancelled> dee) {
    logger.debug("handleOrderCancelled called {}", dee);
    boolean result = orderHistoryDao.updateOrderState(dee.getAggregateId(), OrderState.CANCELLED, makeSourceEvent(dee));
    logger.debug("handleOrderCancelled result {} {}", dee, result);
  }

  public void handleOrderRejected(DomainEventEnvelope<OrderRejected> dee) {
    logger.debug("handleOrderRejected called {}", dee);
    boolean result = orderHistoryDao.updateOrderState(dee.getAggregateId(), OrderState.REJECTED, makeSourceEvent(dee));
    logger.debug("handleOrderRejected result {} {}", dee, result);
  }

  private Order makeOrder(String orderId, OrderCreatedEvent event) {
    return new Order(orderId,
            Long.toString(event.getOrderDetails().getConsumerId()),
            OrderState.APPROVAL_PENDING,
            event.getOrderDetails().getLineItems(),
            event.getOrderDetails().getOrderTotal(),
            event.getOrderDetails().getRestaurantId(),
            event.getRestaurantName());
  }

  public void handleDeliveryPickedUp(DomainEventEnvelope<DeliveryPickedUp>
                                             dee) {
    orderHistoryDao.notePickedUp(dee.getEvent().getOrderId(),
            makeSourceEvent(dee));
  }
/*

  // TODO - need a common API that abstracts message vs. event sourcing

  public void handleOrderCancelled() {

    orderHistoryDao.cancelOrder(orderId, null);
  }

  public void handleTicketPreparationStarted() {
    orderHistoryDao.noteTicketPreparationStarted(orderId);
  }

  public void handleTicketPreparationCompleted() {
    orderHistoryDao.noteTicketPreparationCompleted(orderId);
  }

  public void handleDeliveryLocationUpdated() {
    orderHistoryDao.updateLocation(orderId, location);
  }

  public void handleDeliveryDelivered() {
    orderHistoryDao.noteDelivered(orderId);
  }
  */
}


import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;

public class GetOrderResponse {
  private String orderId;
  private OrderState status;
  private long restaurantId;
  private String restaurantName;


  private GetOrderResponse() {
  }

  public OrderState getStatus() {
    return status;
  }

  public void setStatus(OrderState status) {
    this.status = status;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public GetOrderResponse(String orderId, OrderState status, long restaurantId, String restaurantName) {
    this.orderId = orderId;
    this.status = status;
    this.restaurantId = restaurantId;
    this.restaurantName = restaurantName;
  }


  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getRestaurantName() {
    return restaurantName;
  }

  public void setRestaurantName(String restaurantName) {
    this.restaurantName = restaurantName;
  }
}


import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistory;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryDao;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryFilter;
import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/orders")
public class OrderHistoryController {

  private OrderHistoryDao orderHistoryDao;

  public OrderHistoryController(OrderHistoryDao orderHistoryDao) {
    this.orderHistoryDao = orderHistoryDao;
  }

  @RequestMapping(method = RequestMethod.GET)
  public ResponseEntity<GetOrdersResponse> getOrders(@RequestParam(name = "consumerId") String consumerId) {
    OrderHistory orderHistory = orderHistoryDao.findOrderHistory(consumerId, new OrderHistoryFilter());
    return new ResponseEntity<>(new GetOrdersResponse(orderHistory.getOrders()
            .stream()
            .map(this::makeGetOrderResponse).collect(toList()), orderHistory.getStartKey().orElse(null)), HttpStatus.OK);
  }

  private GetOrderResponse makeGetOrderResponse(Order order) {
    return new GetOrderResponse(order.getOrderId(), order.getStatus(), order.getRestaurantId(), order.getRestaurantName());
  }

  @RequestMapping(path = "/{orderId}", method = RequestMethod.GET)
  public ResponseEntity<GetOrderResponse> getOrder(@PathVariable String orderId) {
    return orderHistoryDao.findOrder(orderId)
            .map(order -> new ResponseEntity<>(makeGetOrderResponse(order), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

}


import java.util.List;

public class GetOrdersResponse {
  private List<GetOrderResponse> orders;
  private String startKey;

  private GetOrdersResponse() {
  }

  public List<GetOrderResponse> getOrders() {
    return orders;
  }

  public void setOrders(List<GetOrderResponse> orders) {
    this.orders = orders;
  }

  public String getStartKey() {
    return startKey;
  }

  public void setStartKey(String startKey) {
    this.startKey = startKey;
  }

  public GetOrdersResponse(List<GetOrderResponse> orders, String startKey) {
    this.orders = orders;
    this.startKey = startKey;
  }
}


import net.chrisrichardson.ftgo.cqrs.orderhistory.dynamodb.OrderHistoryDynamoDBConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(OrderHistoryDynamoDBConfiguration.class)
public class OrderHistoryWebConfiguration {
}


import io.eventuate.tram.spring.consumer.common.TramConsumerCommonConfiguration;
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import net.chrisrichardson.eventstore.examples.customersandorders.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.ftgo.cqrs.orderhistory.messaging.OrderHistoryServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.cqrs.orderhistory.web.OrderHistoryWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({OrderHistoryWebConfiguration.class,
        OrderHistoryServiceMessagingConfiguration.class,
        CommonSwaggerConfiguration.class,
        TramConsumerCommonConfiguration.class,
        EventuateTramKafkaMessageConsumerConfiguration.class})
public class OrderHistoryServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(OrderHistoryServiceMain.class, args);
  }
}


import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class DynamoDBHealthIndicator implements HealthIndicator {
  private final Table table;
  private DynamoDB dynamoDB;

  public DynamoDBHealthIndicator(DynamoDB dynamoDB) {
    this.dynamoDB = dynamoDB;
    this.table = this.dynamoDB.getTable(OrderHistoryDaoDynamoDb.FTGO_ORDER_HISTORY_BY_ID);
  }

  @Override
  public Health health() {
    try {
      table.getItem(OrderHistoryDaoDynamoDb.makePrimaryKey("999"));
      return Health.up().build();
    } catch (Exception e) {
      return Health.down(e).build();
    }
  }
}


import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryDao;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderHistoryDynamoDBConfiguration {

  @Value("${aws.dynamodb.endpoint.url:#{null}}")
  private String awsDynamodbEndpointUrl;

  @Value("${aws.region}")
  private String awsRegion;

  @Value("${aws.access.key_id:null}")
  private String accessKey;

  @Value("${aws.secret.access.key:null}")
  private String secretKey;

  @Bean
  public AmazonDynamoDB amazonDynamoDB() {

    if (!StringUtils.isBlank(awsDynamodbEndpointUrl)) {
      return AmazonDynamoDBClientBuilder
          .standard()
          .withEndpointConfiguration(
              new AwsClientBuilder.EndpointConfiguration(awsDynamodbEndpointUrl, awsRegion))
          .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
          .build();
    } else {
      return AmazonDynamoDBClientBuilder
              .standard()
              .withRegion(awsRegion)
              .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
              .build();
    }
  }

  @Bean
  public DynamoDB dynamoDB(AmazonDynamoDB client) {
    return   new DynamoDB(client);
  }

  @Bean
  public OrderHistoryDao orderHistoryDao(AmazonDynamoDB client, DynamoDB dynamoDB) {
    return new OrderHistoryDaoDynamoDb(dynamoDB);
  }

  @Bean
  public HealthIndicator dynamoDBHealthIndicator(DynamoDB dynamoDB) {
    return new DynamoDBHealthIndicator(dynamoDB);
  }
}


import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderLineItem;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import org.joda.time.DateTime;

import java.util.List;

public class Order {
  private String consumerId;
  private DateTime creationDate = DateTime.now();
  private OrderState status;
  private String orderId;
  private List<OrderLineItem> lineItems;
  private Money orderTotal;
  private long restaurantId;
  private String restaurantName;

  public Order(String orderId, String consumerId, OrderState status, List<OrderLineItem> lineItems, Money orderTotal, long restaurantId, String restaurantName) {
    this.orderId = orderId;
    this.consumerId = consumerId;
    this.status = status;
    this.lineItems = lineItems;
    this.orderTotal = orderTotal;
    this.restaurantId = restaurantId;
    this.restaurantName = restaurantName;
  }

  public String getRestaurantName() {
    return restaurantName;
  }

  public String getOrderId() {
    return orderId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }
  
  public List<OrderLineItem> getLineItems() {
    return lineItems;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setCreationDate(DateTime creationDate) {
    this.creationDate = creationDate;
  }

  public String getConsumerId() {
    return consumerId;
  }

  public DateTime getCreationDate() {
    return creationDate;
  }

  public OrderState getStatus() {
    return status;
  }


}


import org.joda.time.DurationField;

import java.util.HashMap;
import java.util.Map;

public class Maps {

  private final Map<String, Object> map;

  public Maps() {
    this.map = new HashMap<>();
  }

  public Maps add(String key, Object value) {
    map.put(key, value);
    return this;
  }

  public Map<String, Object> map() {
    return map;
  }
}


import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class AvMapBuilder {

  private Map<String, AttributeValue> eav = new HashMap<>();

  public AvMapBuilder(String key, AttributeValue value) {
    eav.put(key, value);
  }

  public AvMapBuilder add(String key, String value) {
    eav.put(key, new AttributeValue(value));
    return this;
  }

  public AvMapBuilder add(String key, AttributeValue value) {
    eav.put(key, value);
    return this;
  }

  public Map<String, AttributeValue> map() {
    return eav;
  }
}



import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.cqrs.orderhistory.Location;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistory;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryDao;
import net.chrisrichardson.ftgo.cqrs.orderhistory.OrderHistoryFilter;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderLineItem;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.BreakIterator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class OrderHistoryDaoDynamoDb implements OrderHistoryDao {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public static final String FTGO_ORDER_HISTORY_BY_ID = "ftgo-order-history";
  public static final String FTGO_ORDER_HISTORY_BY_CONSUMER_ID_AND_DATE =
          "ftgo-order-history-by-consumer-id-and-creation-time";
  public static final String ORDER_STATUS_FIELD = "orderStatus";
  private static final String DELIVERY_STATUS_FIELD = "deliveryStatus";

  private final DynamoDB dynamoDB;

  private Table table;
  private Index index;

  public OrderHistoryDaoDynamoDb(DynamoDB dynamoDB) {
    this.dynamoDB = dynamoDB;
    table = this.dynamoDB.getTable(FTGO_ORDER_HISTORY_BY_ID);
    index = table.getIndex(FTGO_ORDER_HISTORY_BY_CONSUMER_ID_AND_DATE);
  }

  @Override
  public boolean addOrder(Order order, Optional<SourceEvent> eventSource) {
    UpdateItemSpec spec = new UpdateItemSpec()
            .withPrimaryKey("orderId", order.getOrderId())
            .withUpdateExpression("SET orderStatus = :orderStatus, " +
                    "creationDate = :creationDate, consumerId = :consumerId, lineItems =" +
                    " :lineItems, keywords = :keywords, restaurantId = :restaurantId, " +
                    " restaurantName = :restaurantName"
            )
            .withValueMap(new Maps()
                    .add(":orderStatus", order.getStatus().toString())
                    .add(":consumerId", order.getConsumerId())
                    .add(":creationDate", order.getCreationDate().getMillis())
                    .add(":lineItems", mapLineItems(order.getLineItems()))
                    .add(":keywords", mapKeywords(order))
                    .add(":restaurantId", order.getRestaurantId())
                    .add(":restaurantName", order.getRestaurantName())
                    .map())
            .withReturnValues(ReturnValue.NONE);
    return idempotentUpdate(spec, eventSource);
  }

  private boolean idempotentUpdate(UpdateItemSpec spec, Optional<SourceEvent>
          eventSource) {
    try {
      table.updateItem(eventSource.map(es -> es.addDuplicateDetection(spec))
              .orElse(spec));
      return true;
    } catch (ConditionalCheckFailedException e) {
      logger.error("not updated {}", eventSource);
      // Do nothing
      return false;
    }
  }

////  @Override
//  public void addOrderV1(Order order, Optional<SourceEvent> eventSource) {
//    Map<String, AttributeValue> keyMapBuilder = makeKey1(order.getOrderId());
//    AvMapBuilder expressionAttrs = new AvMapBuilder(":orderStatus", new
// AttributeValue(order.getStatus().toString()))
//            .add(":cd", new AttributeValue().withN(Long.toString(order
// .getCreationDate().getMillis())))
//            .add(":consumerId", order.getConsumerId())
//            .add(":lineItems", mapLineItems(order.getLineItems()))
//            .add(":keywords", mapKeywords(order))
//            .add(":restaurantName", order.getRestaurantId())
//            ;
//
//
//    UpdateItemRequest uir = new UpdateItemRequest()
//            .withTableName(FTGO_ORDER_HISTORY_BY_ID)
//            .withKey(keyMapBuilder)
//            .withUpdateExpression("SET orderStatus = :orderStatus,
// creationDate = :cd, consumerId = :consumerId, lineItems = :lineItems,
// keywords = :keywords, restaurantName = :restaurantName")
//            .withConditionExpression("attribute_not_exists(orderStatus)")
//            .withExpressionAttributeValues(expressionAttrs.map());
//    try {
//      client.updateItem(uir);
//    } catch (ConditionalCheckFailedException e) {
//      // Do nothing
//    }
//  }

  private Set mapKeywords(Order order) {
    Set<String> keywords = new HashSet<>();
    keywords.addAll(tokenize(order.getRestaurantName()));
    keywords.addAll(tokenize(order.getLineItems().stream().map
            (OrderLineItem::getName).collect(toList())));
    return keywords;
  }

  private Set<String> tokenize(Collection<String> text) {
    return text.stream().flatMap(t -> tokenize(t).stream()).collect(toSet());
  }

  private Set<String> tokenize(String text) {
    Set<String> result = new HashSet<>();
    BreakIterator bi = BreakIterator.getWordInstance();
    bi.setText(text);
    int lastIndex = bi.first();
    while (lastIndex != BreakIterator.DONE) {
      int firstIndex = lastIndex;
      lastIndex = bi.next();
      if (lastIndex != BreakIterator.DONE
              && Character.isLetterOrDigit(text.charAt(firstIndex))) {
        String word = text.substring(firstIndex, lastIndex);
        result.add(word);
      }
    }
    return result;
  }

  private List mapLineItems(List<OrderLineItem> lineItems) {
    return lineItems.stream().map(this::mapOrderLineItem).collect(toList());
  }
//  private AttributeValue mapLineItems(List<OrderLineItem> lineItems) {
//    AttributeValue result = new AttributeValue();
//    result.withL(lineItems.stream().map(this::mapOrderLineItem).collect
// (toList()));
//    return result;
//  }

  private Map mapOrderLineItem(OrderLineItem orderLineItem) {
    return new Maps()
            .add("menuItemName", orderLineItem.getName())
            .add("menuItemId", orderLineItem.getMenuItemId())
            .add("price", orderLineItem.getPrice().asString())
            .add("quantity", orderLineItem.getQuantity())
            .map();
  }
//  private AttributeValue mapOrderLineItem(OrderLineItem orderLineItem) {
//    AttributeValue result = new AttributeValue();
//    result.addMEntry("menuItem", new AttributeValue(orderLineItem
// .getName()));
//    return result;
//  }


  private Map<String, AttributeValue> makeKey1(String orderId) {
    return new AvMapBuilder("orderId", new AttributeValue(orderId)).map();
  }

  @Override
  public OrderHistory findOrderHistory(String consumerId, OrderHistoryFilter
          filter) {

    QuerySpec spec = new QuerySpec()
            .withScanIndexForward(false)
            .withHashKey("consumerId", consumerId)
            .withRangeKeyCondition(new RangeKeyCondition("creationDate").gt
                    (filter.getSince().getMillis()));

    filter.getStartKeyToken().ifPresent(token -> spec.withExclusiveStartKey
            (toStartingPrimaryKey(token)));

    Map<String, Object> valuesMap = new HashMap<>();

    String filterExpression = Expressions.and(
            keywordFilterExpression(valuesMap, filter.getKeywords()),
            statusFilterExpression(valuesMap, filter.getStatus()));

    if (!valuesMap.isEmpty())
      spec.withValueMap(valuesMap);

    if (StringUtils.isNotBlank(filterExpression)) {
      spec.withFilterExpression(filterExpression);
    }

    System.out.print("filterExpression.toString()=" + filterExpression);

    filter.getPageSize().ifPresent(spec::withMaxResultSize);

    ItemCollection<QueryOutcome> result = index.query(spec);

    return new OrderHistory(StreamSupport.stream(result.spliterator(), false)
            .map(this::toOrder).collect(toList()),
            Optional.ofNullable(result.getLastLowLevelResult().getQueryResult
                    ().getLastEvaluatedKey()).map(this::toStartKeyToken));
  }

  private PrimaryKey toStartingPrimaryKey(String token) {
    ObjectMapper om = new ObjectMapper();
    Map<String, Object> map;
    try {
      map = om.readValue(token, Map.class);
    } catch (IOException e) {
      throw new RuntimeException();
    }
    PrimaryKey pk = new PrimaryKey();
    map.entrySet().forEach(key -> {
      pk.addComponent(key.getKey(), key.getValue());
    });
    return pk;
  }

  private String toStartKeyToken(Map<String, AttributeValue> lastEvaluatedKey) {
    Map<String, Object> map = new HashMap<>();
    lastEvaluatedKey.entrySet().forEach(entry -> {
      String value = entry.getValue().getS();
      if (value == null) {
        value = entry.getValue().getN();
        map.put(entry.getKey(), Long.parseLong(value));
      } else {
        map.put(entry.getKey(), value);
      }
    });
    ObjectMapper om = new ObjectMapper();
    try {
      return om.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException();
    }
  }

  private Optional<String> statusFilterExpression(Map<String, Object>
                                                          expressionAttributeValuesBuilder, Optional<OrderState> status) {
    return status.map(s -> {
      expressionAttributeValuesBuilder.put(":orderStatus", s.toString());
      return "orderStatus = :orderStatus";
    });
  }

  private String keywordFilterExpression(Map<String, Object>
                                                 expressionAttributeValuesBuilder, Set<String> kw) {
    Set<String> keywords = tokenize(kw);
    if (keywords.isEmpty()) {
      return "";
    }
    String cuisinesExpression = "";
    int idx = 0;
    for (String cuisine : keywords) {
      String var = ":keyword" + idx;
      String cuisineExpression = String.format("contains(keywords, %s)", var);
      cuisinesExpression = Expressions.or(cuisinesExpression, cuisineExpression);
      expressionAttributeValuesBuilder.put(var, cuisine);
    }

    return cuisinesExpression;
  }

//  @Override
//  public OrderHistory findOrderHistory(String consumerId,
// OrderHistoryFilter filter) {
//    AvMapBuilder expressionAttributeValuesBuilder = new AvMapBuilder
// (":cid", new AttributeValue(consumerId))
//            .add(":oct", new AttributeValue().withN(Long.toString(filter
// .getSince().getMillis())));
//    StringBuilder filterExpression = new StringBuilder();
//    Set<String> keywords = tokenize(filter.getKeywords());
//    if (!keywords.isEmpty()) {
//      if (filterExpression.length() > 0)
//        filterExpression.append(" AND ");
//      filterExpression.append(" ( ");
//      int idx = 0;
//      for (String cuisine : keywords) {
//        if (idx++ > 0) {
//          filterExpression.append(" OR ");
//        }
//        String var = ":keyword" + idx;
//        filterExpression.append("contains(keywords, ").append(var).append
// (')');
//        expressionAttributeValuesBuilder.add(var, cuisine);
//      }
//
//      filterExpression.append(" ) ");
//    }
//    filter.getStatus().ifPresent(status -> {
//      if (filterExpression.length() > 0)
//        filterExpression.append(" AND ");
//      filterExpression.append("orderStatus = :orderStatus");
//      expressionAttributeValuesBuilder.add(":orderStatus", status.toString
// ());
//    });
//    QueryRequest ar = new QueryRequest()
//            .withTableName(FTGO_ORDER_HISTORY_BY_ID)
//            .withIndexName(FTGO_ORDER_HISTORY_BY_CONSUMER_ID_AND_DATE)
//            .withScanIndexForward(false)
//            .withKeyConditionExpression("consumerId = :cid AND
// creationDate > :oct")
//            .withExpressionAttributeValues
// (expressionAttributeValuesBuilder.map());
//    System.out.print("filterExpression.toString()=" + filterExpression
// .toString());
//    if (filterExpression.length() > 0)
//      ar.withFilterExpression(filterExpression.toString());
//
//    QuerySpec spec = new QuerySpec();
//    ItemCollection<QueryOutcome> result = table.query(spec);
//
//    List<Map<String, AttributeValue>> items = client.query(ar).getItems();
//    return new OrderHistory(items.stream().map(this::toOrder).collect
// (toList()));
//  }

  @Override
  public boolean updateOrderState(String orderId, OrderState newState, Optional<SourceEvent> eventSource) {
    UpdateItemSpec spec = new UpdateItemSpec()
            .withPrimaryKey("orderId", orderId)
            .withUpdateExpression("SET #orderStatus = :orderStatus")
            .withNameMap(Collections.singletonMap("#orderStatus",
                    ORDER_STATUS_FIELD))
            .withValueMap(Collections.singletonMap(":orderStatus", newState.toString()))
            .withReturnValues(ReturnValue.NONE);
    return idempotentUpdate(spec, eventSource);
  }


  static PrimaryKey makePrimaryKey(String orderId) {
    return new PrimaryKey().addComponent("orderId", orderId);
  }

  @Override
  public void noteTicketPreparationStarted(String orderId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void noteTicketPreparationCompleted(String orderId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void notePickedUp(String orderId, Optional<SourceEvent> eventSource) {
    UpdateItemSpec spec = new UpdateItemSpec()
            .withPrimaryKey("orderId", orderId)
            .withUpdateExpression("SET #deliveryStatus = :deliveryStatus")
            .withNameMap(Collections.singletonMap("#deliveryStatus",
                    DELIVERY_STATUS_FIELD))
            .withValueMap(Collections.singletonMap(":deliveryStatus",
                    DeliveryStatus.PICKED_UP.toString()))
            .withReturnValues(ReturnValue.NONE);
    idempotentUpdate(spec, eventSource);
  }

  @Override
  public void updateLocation(String orderId, Location location) {
    throw new UnsupportedOperationException();

  }

  @Override
  public void noteDelivered(String orderId) {
    throw new UnsupportedOperationException();

  }

  @Override
  public Optional<Order> findOrder(String orderId) {
    Item item = table.getItem(new GetItemSpec()
            .withPrimaryKey(makePrimaryKey(orderId))
            .withConsistentRead(true));
    return Optional.ofNullable(item).map(this::toOrder);
  }


  private Order toOrder(Item avs) {
    Order order = new Order(avs.getString("orderId"),
            avs.getString("consumerId"),
            OrderState.valueOf(avs.getString("orderStatus")),
            toLineItems2(avs.getList("lineItems")),
            null,
            avs.getLong("restaurantId"),
            avs.getString("restaurantName"));
    if (avs.hasAttribute("creationDate"))
      order.setCreationDate(new DateTime(avs.getLong("creationDate")));
    return order;
  }


  private List<OrderLineItem> toLineItems2(List<LinkedHashMap<String,
          Object>> lineItems) {
    return lineItems.stream().map(this::toLineItem2).collect(toList());
  }

  private OrderLineItem toLineItem2(LinkedHashMap<String, Object>
                                            attributeValue) {
    return new OrderLineItem((String) attributeValue.get("menuItemId"),
                             (String) attributeValue.get("menuItemName"),
                             new Money((String) attributeValue.get("price")),
                            ((BigDecimal) attributeValue.get("quantity")).intValue()
            );
  }

}


import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class Expressions {

  static String and(String s1, Optional<String> s2) {
    return s2.map(x -> and(s1, x)).orElse(s1);
  }

  static String and(String s1, String s2) {
    if (StringUtils.isBlank(s1))
      return s2;
    if (StringUtils.isBlank(s2))
      return s1;
    return String.format("(%s) AND (%s)", s1, s2);
  }

  static String or(String s1, String s2) {
    if (StringUtils.isBlank(s1))
      return s2;
    if (StringUtils.isBlank(s2))
      return s1;
    return String.format("(%s) AND (%s)", s1, s2);
  }
}


import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;

import java.util.HashMap;

public class SourceEvent {

  String aggregateType;
  String aggregateId;
  String eventId;

  public SourceEvent(String aggregateType, String aggregateId, String eventId) {
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventId = eventId;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public UpdateItemSpec addDuplicateDetection(UpdateItemSpec spec) {
    HashMap<String, String> nameMap = spec.getNameMap() == null ? new HashMap<>() : new HashMap<>(spec.getNameMap());
    nameMap.put("#duplicateDetection", "events." + aggregateType + aggregateId);
    HashMap<String, Object> valueMap = new HashMap<>(spec.getValueMap());
    valueMap.put(":eventId", eventId);
    return spec.withUpdateExpression(String.format("%s , #duplicateDetection = :eventId", spec.getUpdateExpression()))
            .withNameMap(nameMap)
            .withValueMap(valueMap)
            .withConditionExpression(Expressions.and(spec.getConditionExpression(), "attribute_not_exists(#duplicateDetection) OR #duplicateDetection < :eventId"));
  }

}


public enum DeliveryStatus {
  PICKED_UP
}


public class OrderServiceChannels {
  public static final String COMMAND_CHANNEL = "orderService";
  public static final String ORDER_EVENT_CHANNEL = "net.chrisrichardson.ftgo.orderservice.domain.Order";

}


public enum OrderState {
  APPROVAL_PENDING,
  APPROVED,
  REJECTED,
  CANCEL_PENDING,
  CANCELLED,
  REVISION_PENDING,
}


import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class OrderLineItem {

  public OrderLineItem() {
  }

  private int quantity;
  private String menuItemId;
  private String name;

  @Embedded
  @AttributeOverrides(@AttributeOverride(name="amount", column=@Column(name="price")))
  private Money price;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public OrderLineItem(String menuItemId, String name, Money price, int quantity) {
    this.menuItemId = menuItemId;
    this.name = name;
    this.price = price;
    this.quantity = quantity;
  }

  public Money deltaForChangedQuantity(int newQuantity) {
    return price.multiply(newQuantity - quantity);
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void setMenuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPrice(Money price) {
    this.price = price;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getMenuItemId() {
    return menuItemId;
  }

  public String getName() {
    return name;
  }

  public Money getPrice() {
    return price;
  }


  public Money getTotal() {
    return price.multiply(quantity);
  }

}


import net.chrisrichardson.ftgo.common.Address;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class OrderCreatedEvent implements OrderDomainEvent {
  private OrderDetails orderDetails;
  private Address deliveryAddress;
  private String restaurantName;

  private OrderCreatedEvent() {
  }

  public OrderCreatedEvent(OrderDetails orderDetails, Address deliveryAddress, String restaurantName) {

    this.orderDetails = orderDetails;
    this.deliveryAddress = deliveryAddress;
    this.restaurantName = restaurantName;
  }

  public OrderDetails getOrderDetails() {
    return orderDetails;
  }

  public void setOrderDetails(OrderDetails orderDetails) {
    this.orderDetails = orderDetails;
  }

  public String getRestaurantName() {
    return restaurantName;
  }

  public void setRestaurantName(String restaurantName) {
    this.restaurantName = restaurantName;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(Address deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class OrderAuthorized implements OrderDomainEvent {

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

}


public class OrderRejected implements OrderDomainEvent {
}


import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

public class OrderDetails {

  private List<OrderLineItem> lineItems;
  private Money orderTotal;

  private long restaurantId;
  private long consumerId;

  private OrderDetails() {
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public OrderDetails(long consumerId, long restaurantId, List<OrderLineItem> lineItems, Money orderTotal) {
    this.consumerId = consumerId;
    this.restaurantId = restaurantId;
    this.lineItems = lineItems;
    this.orderTotal = orderTotal;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public List<OrderLineItem> getLineItems() {
    return lineItems;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public long getConsumerId() {
    return consumerId;
  }


  public void setLineItems(List<OrderLineItem> lineItems) {
    this.lineItems = lineItems;
  }


  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }


}


import io.eventuate.tram.events.common.DomainEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;

public class OrderCancelled implements OrderDomainEvent {
}


import io.eventuate.tram.events.common.DomainEvent;

public interface OrderDomainEvent extends DomainEvent {
}


import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;

import java.util.List;

public class ReviseOrderRequest {
  private List<RevisedOrderLineItem> revisedOrderLineItems;

  private ReviseOrderRequest() {
  }

  public ReviseOrderRequest(List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.revisedOrderLineItems = revisedOrderLineItems;
  }

  public List<RevisedOrderLineItem> getRevisedOrderLineItems() {
    return revisedOrderLineItems;
  }

  public void setRevisedOrderLineItems(List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.revisedOrderLineItems = revisedOrderLineItems;
  }
}


import net.chrisrichardson.ftgo.common.Address;

import java.time.LocalDateTime;
import java.util.List;

public class CreateOrderRequest {

  private long restaurantId;
  private long consumerId;
  private LocalDateTime deliveryTime;
  private List<LineItem> lineItems;
  private Address deliveryAddress;

  public CreateOrderRequest(long consumerId, long restaurantId, Address deliveryAddress, LocalDateTime deliveryTime, List<LineItem> lineItems) {
    this.restaurantId = restaurantId;
    this.consumerId = consumerId;
    this.deliveryAddress = deliveryAddress;
    this.deliveryTime = deliveryTime;
    this.lineItems = lineItems;

  }

  private CreateOrderRequest() {
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public List<LineItem> getLineItems() {
    return lineItems;
  }

  public void setLineItems(List<LineItem> lineItems) {
    this.lineItems = lineItems;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(Address deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }

  public LocalDateTime getDeliveryTime() {
    return deliveryTime;
  }

  public void setDeliveryTime(LocalDateTime deliveryTime) {
    this.deliveryTime = deliveryTime;
  }

  public static class LineItem {

    private String menuItemId;
    private int quantity;

    private LineItem() {
    }

    public LineItem(String menuItemId, int quantity) {
      this.menuItemId = menuItemId;

      this.quantity = quantity;
    }

    public String getMenuItemId() {
      return menuItemId;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }

    public void setMenuItemId(String menuItemId) {
      this.menuItemId = menuItemId;

    }

  }


}


public class CreateOrderResponse {
  private long orderId;

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  private CreateOrderResponse() {
  }

  public CreateOrderResponse(long orderId) {
    this.orderId = orderId;
  }
}


import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceOutOfProcessComponentTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "debug=true")
public class OrderServiceOutOfProcessComponentTest extends AbstractOrderServiceComponentTest {


  @Value("${local.server.port}")
  private int port;

  @Override
  protected String baseUrl(String path) {
    return "http://localhost:" + port + path;
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({CommonTestConfiguration.class, TramJdbcKafkaConfiguration.class})
  public static class TestConfiguration {
  }
}


import io.eventuate.tram.spring.commands.producer.TramCommandProducerConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.util.test.async.Eventually;
import net.chrisrichardson.ftgo.accountservice.api.AuthorizeCommand;
import net.chrisrichardson.ftgo.consumerservice.api.ValidateOrderByConsumer;
import net.chrisrichardson.ftgo.kitchenservice.api.ConfirmCreateTicket;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicket;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicketReply;
import net.chrisrichardson.ftgo.orderservice.api.web.CreateOrderRequest;
import net.chrisrichardson.ftgo.orderservice.domain.RestaurantRepository;
import net.chrisrichardson.ftgo.orderservice.messaging.OrderServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.ApproveOrderCommand;
import net.chrisrichardson.ftgo.orderservice.service.OrderCommandHandlersConfiguration;
import net.chrisrichardson.ftgo.orderservice.web.OrderWebConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collections;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractOrderServiceComponentTest {

  protected abstract String baseUrl(String path);

  @Configuration
  @Import({CommonMessagingStubConfiguration.class, OrderWebConfiguration.class, OrderServiceMessagingConfiguration.class, OrderCommandHandlersConfiguration.class,
          TramCommandProducerConfiguration.class})
  public static class CommonTestConfiguration {
  }

  @Configuration
  @Import(SagaParticipantStubConfiguration.class)
  public static class CommonMessagingStubConfiguration {

    @Bean
    public MessagingStubConfiguration messagingStubConfiguration() {
      return new MessagingStubConfiguration("consumerService", "kitchenService", "accountingService", "orderService");
    }

    @Bean
    public MessageTracker messageTracker(MessageConsumer messageConsumer) {
      return new MessageTracker(new MessageTrackerConfiguration("orderService"), messageConsumer) ;
    }

  }

  @Autowired
  private SagaParticipantStubManager sagaParticipantStubManager;

  @Autowired
  private DomainEventPublisher domainEventPublisher;

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MessageTracker messageTracker;

  @Before
  public void setUp() throws Exception {
    sagaParticipantStubManager.reset();
  }

  @Test
  public void shouldCreateOrder() {

    // setup

    sagaParticipantStubManager.
            forChannel("consumerService")
            .when(ValidateOrderByConsumer.class).replyWith(cm -> withSuccess())
            .forChannel("kitchenService")
            .when(CreateTicket.class).replyWith(cm -> withSuccess(new CreateTicketReply(cm.getCommand().getOrderId())))
            .when(ConfirmCreateTicket.class).replyWithSuccess()
            .forChannel("accountingService")
            .when(AuthorizeCommand.class).replyWithSuccess()
            .forChannel("orderService")
            .when(ApproveOrderCommand.class).replyWithSuccess()
    ;


    domainEventPublisher.publish("net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant", RestaurantMother.AJANTA_ID,
            Collections.singletonList(RestaurantMother.makeAjantaRestaurantCreatedEvent()));


    Eventually.eventually(() -> {
      FtgoTestUtil.assertPresent(restaurantRepository.findById(RestaurantMother.AJANTA_ID));
    });

    // make HTTP request

    Integer orderId =
            given().
                    body(new CreateOrderRequest(OrderDetailsMother.CONSUMER_ID,
                            RestaurantMother.AJANTA_ID, Collections.singletonList(new CreateOrderRequest.LineItem(RestaurantMother.CHICKEN_VINDALOO_MENU_ITEM_ID,
                            OrderDetailsMother.CHICKEN_VINDALOO_QUANTITY)))).
                    contentType("application/json").
                    when().
                    post(baseUrl("/orders")).
                    then().
                    statusCode(200).
                    extract().
                    path("orderId");

    assertNotNull(orderId);

    // verify response
    // verify Order state
    // verify events published????

    messageTracker.assertCommandMessageSent("orderService", ApproveOrderCommand.class);

  }
}


import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceInProcessComponentTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderServiceInProcessComponentTest extends AbstractOrderServiceComponentTest {


  @Value("${local.server.port}")
  private int port;

  @Override
  protected String baseUrl(String path) {
    return "http://localhost:" + port + path;
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({CommonTestConfiguration.class, TramInMemoryConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public DataSource dataSource() {
      EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
      return builder.setType(EmbeddedDatabaseType.H2)
              .addScript("eventuate-tram-embedded-schema.sql")
              .addScript("eventuate-tram-sagas-embedded.sql")
              .build();
    }


  }
}


import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import io.eventuate.tram.spring.commands.producer.TramCommandProducerConfiguration;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import net.chrisrichardson.ftgo.common.CommonJsonMapperInitializer;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceExternalComponentTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "debug=true")
public class OrderServiceExternalComponentTest extends AbstractOrderServiceComponentTest {


  static {
    CommonJsonMapperInitializer.registerMoneyModule();
  }
  
  private int port = 8082;
  private String host = FtgoTestUtil.getDockerHostIp();

  @Override
  protected String baseUrl(String path) {
    return String.format("http://%s:%s%s", host, port, path);
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({CommonMessagingStubConfiguration.class,
          TramCommandProducerConfiguration.class, TramEventsPublisherConfiguration.class,
          TramJdbcKafkaConfiguration.class})
  public static class TestConfiguration {
  }
}


import io.eventuate.tram.commands.common.ChannelMapping;
import io.eventuate.tram.commands.common.DefaultChannelMapping;
import io.eventuate.tram.spring.commands.producer.TramCommandProducerConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.util.test.async.Eventually;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import net.chrisrichardson.ftgo.orderservice.domain.Order;
import net.chrisrichardson.ftgo.orderservice.domain.OrderRepository;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.orderservice.domain.RestaurantRepository;
import net.chrisrichardson.ftgo.orderservice.messaging.OrderServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.orderservice.service.OrderCommandHandlersConfiguration;
import net.chrisrichardson.ftgo.orderservice.web.OrderWebConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.BatchStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= OrderServiceOutOfProcessComponentV0Test.TestConfiguration.class,
        webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT,
      properties = "customer.service.url=http://localhost:8888/customers/{customerId}")
@AutoConfigureStubRunner(ids =
        {"net.chrisrichardson.ftgo:ftgo-accounting-service-contracts", "net.chrisrichardson.ftgo:ftgo-consumer-service-contracts",
                "net.chrisrichardson.ftgo:ftgo-kitchen-service-contracts"}
        )
@DirtiesContext
public class OrderServiceOutOfProcessComponentV0Test {

  @Configuration
  @EnableAutoConfiguration
  @Import({OrderWebConfiguration.class, OrderServiceMessagingConfiguration.class,  OrderCommandHandlersConfiguration.class,
          TramCommandProducerConfiguration.class,
          TramInMemoryConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public ChannelMapping channelMapping() {
      return new DefaultChannelMapping.DefaultChannelMappingBuilder().build();
    }


    @Bean
    public DataSource dataSource() {
      EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
      return builder.setType(EmbeddedDatabaseType.H2)
              .addScript("eventuate-tram-embedded-schema.sql")
              .addScript("eventuate-tram-sagas-embedded.sql")
              .build();
    }


    @Bean
    public EventuateTramRoutesConfigurer eventuateTramRoutesConfigurer(BatchStubRunner batchStubRunner) {
      return new EventuateTramRoutesConfigurer(batchStubRunner);
    }
  }

  @Value("${local.server.port}")
  private int port;

  private String baseUrl(String path) {
    return "http://localhost:" + port + path;
  }

  @Autowired
  private MessageVerifier verifier;

  @Autowired
  private DomainEventPublisher domainEventPublisher;

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderRepository orderRepository;


  @Test
  public void shouldCreateOrder() throws InterruptedException {
    domainEventPublisher.publish("net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant", RestaurantMother.AJANTA_ID,
            Collections.singletonList(RestaurantMother.makeAjantaRestaurantCreatedEvent()));

    Eventually.eventually(() -> {
      FtgoTestUtil.assertPresent(restaurantRepository.findById(RestaurantMother.AJANTA_ID));
    });


    Order order = orderService.createOrder(OrderDetailsMother.CONSUMER_ID,
            RestaurantMother.AJANTA_ID,
            Collections.singletonList(OrderDetailsMother.CHICKEN_VINDALOO_MENU_ITEM_AND_QUANTITY));


    Eventually.eventually(() -> {
      Order o = orderRepository.findById(order.getId());
      assertNotNull(o);
      assertEquals(OrderState.AUTHORIZED, o.getState());
    });
  }

}


import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import(GrpcConfiguration.class)
public class OrderServiceGrpIntegrationTestConfiguration {

  @Bean
  public OrderService orderService() {
    return mock(OrderService.class);
  }
}



import io.grpc.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import net.chrisrichardson.ftgo.orderservice.web.MenuItemIdAndQuantity;

public class OrderServiceClient {
  private static final Logger logger = Logger.getLogger(OrderServiceClient.class.getName());

  private final ManagedChannel channel;
  private final OrderServiceGrpc.OrderServiceBlockingStub clientStub;

  public OrderServiceClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
    clientStub = OrderServiceGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  public long createOrder(long consumerId, long restaurantId, List<MenuItemIdAndQuantity> lineItems, net.chrisrichardson.ftgo.common.Address deliveryAddress, LocalDateTime deliveryTime) {
    CreateOrderRequest.Builder builder = CreateOrderRequest.newBuilder()
            .setConsumerId(consumerId)
            .setRestaurantId(restaurantId)
            .setDeliveryAddress(makeAddress(deliveryAddress))
            .setDeliveryTime(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(deliveryTime));
    lineItems.forEach(li -> builder.addLineItems(LineItem.newBuilder().setQuantity(li.getQuantity()).setMenuItemId(li.getMenuItemId())));
    CreateOrderReply response = clientStub.createOrder(builder.build());
    return response.getOrderId();
  }

  private Address makeAddress(net.chrisrichardson.ftgo.common.Address address) {
    Address.Builder builder = Address.newBuilder()
            .setStreet1(address.getStreet1());
    if (address.getStreet2() != null)
      builder.setStreet2(address.getStreet2());
    builder
            .setCity(address.getCity())
            .setState(address.getState())
            .setZip(address.getZip());
    return builder.build();
  }


}



import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.domain.DeliveryInformation;
import net.chrisrichardson.ftgo.orderservice.domain.Order;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.orderservice.web.MenuItemIdAndQuantity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceGrpIntegrationTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class OrderServiceGrpIntegrationTest {

  @Autowired
  private OrderService orderService;

  @Test
  public void shouldCreateOrder() {

    Order order = OrderDetailsMother.CHICKEN_VINDALOO_ORDER;

    when(orderService.createOrder(any(Long.class), any(Long.class), any(DeliveryInformation.class), any(List.class))).thenReturn(order);

    OrderServiceClient client = new OrderServiceClient("localhost", 50051);

    List<MenuItemIdAndQuantity> expectedLineItems = order.getLineItems().stream().map(li -> new MenuItemIdAndQuantity(li.getMenuItemId(), li.getQuantity())).collect(Collectors.toList());

    long orderId = client.createOrder(order.getConsumerId(), order.getRestaurantId(), expectedLineItems, order.getDeliveryInformation().getDeliveryAddress(), order.getDeliveryInformation().getDeliveryTime());

    assertEquals((long)order.getId(), orderId);

    verify(orderService).createOrder(order.getConsumerId(), order.getRestaurantId(), order.getDeliveryInformation(), expectedLineItems);

  }
}


import io.eventuate.common.json.mapper.JSonMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.chrisrichardson.ftgo.common.CommonJsonMapperInitializer;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.domain.OrderRepository;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.orderservice.web.OrderController;
import org.junit.Before;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.Optional;

import static java.util.Optional.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class HttpBase {

  private StandaloneMockMvcBuilder controllers(Object... controllers) {
    CommonJsonMapperInitializer.registerMoneyModule();
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(JSonMapper.objectMapper);
    return MockMvcBuilders.standaloneSetup(controllers).setMessageConverters(converter);
  }

  @Before
  public void setup() {
    OrderService orderService = mock(OrderService.class);
    OrderRepository orderRepository = mock(OrderRepository.class);
    OrderController orderController = new OrderController(orderService, orderRepository);

    when(orderRepository.findById(OrderDetailsMother.ORDER_ID)).thenReturn(Optional.of(OrderDetailsMother.CHICKEN_VINDALOO_ORDER));
    when(orderRepository.findById(555L)).thenReturn(empty());
    RestAssuredMockMvc.standaloneSetup(controllers(orderController));

  }
}

import io.eventuate.common.spring.jdbc.EventuateTransactionTemplateConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.cloudcontractsupport.EventuateContractVerifierConfiguration;
import net.chrisrichardson.ftgo.common.CommonJsonMapperInitializer;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderCreatedEvent;
import net.chrisrichardson.ftgo.orderservice.domain.OrderDomainEventPublisher;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CHICKEN_VINDALOO_ORDER;
import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CHICKEN_VINDALOO_ORDER_DETAILS;
import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.AJANTA_RESTAURANT_NAME;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MessagingBase.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class MessagingBase {

  static {
    CommonJsonMapperInitializer.registerMoneyModule();
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({EventuateContractVerifierConfiguration.class, TramEventsPublisherConfiguration.class, TramInMemoryConfiguration.class, EventuateTransactionTemplateConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public OrderDomainEventPublisher orderAggregateEventPublisher(DomainEventPublisher eventPublisher) {
      return new OrderDomainEventPublisher(eventPublisher);
    }
  }


  @Autowired
  private OrderDomainEventPublisher orderAggregateEventPublisher;

  protected void orderCreated() {
    orderAggregateEventPublisher.publish(CHICKEN_VINDALOO_ORDER,
            Collections.singletonList(new OrderCreatedEvent(CHICKEN_VINDALOO_ORDER_DETAILS, OrderDetailsMother.DELIVERY_ADDRESS, AJANTA_RESTAURANT_NAME)));
  }

}


import io.eventuate.common.spring.jdbc.EventuateTransactionTemplateConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.cloudcontractsupport.EventuateContractVerifierConfiguration;
import net.chrisrichardson.ftgo.common.CommonJsonMapperInitializer;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderCreatedEvent;
import net.chrisrichardson.ftgo.orderservice.domain.OrderDomainEventPublisher;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CHICKEN_VINDALOO_ORDER;
import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CHICKEN_VINDALOO_ORDER_DETAILS;
import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.AJANTA_RESTAURANT_NAME;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeliveryserviceMessagingBase.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class DeliveryserviceMessagingBase {

  static {
    CommonJsonMapperInitializer.registerMoneyModule();
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({EventuateContractVerifierConfiguration.class, TramEventsPublisherConfiguration.class, TramInMemoryConfiguration.class, EventuateTransactionTemplateConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public OrderDomainEventPublisher orderAggregateEventPublisher(DomainEventPublisher eventPublisher) {
      return new OrderDomainEventPublisher(eventPublisher);
    }
  }

  @Autowired
  private OrderDomainEventPublisher orderAggregateEventPublisher;

  protected void orderCreatedEvent() {
    orderAggregateEventPublisher.publish(CHICKEN_VINDALOO_ORDER,
            Collections.singletonList(new OrderCreatedEvent(CHICKEN_VINDALOO_ORDER_DETAILS, OrderDetailsMother.DELIVERY_ADDRESS, AJANTA_RESTAURANT_NAME)));
  }
}


import com.jayway.jsonpath.JsonPath;
import io.eventuate.tram.commands.common.CommandMessageHeaders;
import io.eventuate.tram.spring.commands.producer.TramCommandProducerConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.spring.inmemory.TramSagaInMemoryConfiguration;
import io.eventuate.tram.testutil.TestMessageConsumerFactory;
import io.eventuate.util.test.async.Eventually;
import net.chrisrichardson.ftgo.consumerservice.api.ConsumerServiceChannels;
import net.chrisrichardson.ftgo.consumerservice.api.ValidateOrderByConsumer;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.RestaurantMother;
import net.chrisrichardson.ftgo.orderservice.messaging.OrderServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.orderservice.service.OrderCommandHandlersConfiguration;
import net.chrisrichardson.ftgo.orderservice.web.MenuItemIdAndQuantity;
import net.chrisrichardson.ftgo.orderservice.web.OrderWebConfiguration;
import net.chrisrichardson.ftgo.testutil.FtgoTestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.function.Predicate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderServiceIntegrationTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="eventuate.database.schema=eventuate"
)
public class OrderServiceIntegrationTest {


  public static final String RESTAURANT_ID = "1";
  @Value("${local.server.port}")
  private int port;

  private String baseUrl(String path) {
    return "http://localhost:" + port + path;
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({OrderWebConfiguration.class, OrderServiceMessagingConfiguration.class,  OrderCommandHandlersConfiguration.class,
          TramCommandProducerConfiguration.class,
          TramSagaInMemoryConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public TestMessageConsumerFactory testMessageConsumerFactory() {
      return new TestMessageConsumerFactory();
    }


    @Bean
    public TestMessageConsumer2 mockConsumerService() {
      return new TestMessageConsumer2("mockConsumerService", ConsumerServiceChannels.consumerServiceChannel);
    }
  }

  @Autowired
  private DomainEventPublisher domainEventPublisher;

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private OrderService orderService;

  private static final String CHICKED_VINDALOO_MENU_ITEM_ID = "1";

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  @Qualifier("mockConsumerService")
  private TestMessageConsumer2  mockConsumerService;

  @Test
  public void shouldCreateOrder() {
    domainEventPublisher.publish("net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant", RESTAURANT_ID,
            Collections.singletonList(RestaurantMother.makeAjantaRestaurantCreatedEvent()));

    Eventually.eventually(() -> {
      FtgoTestUtil.assertPresent(restaurantRepository.findById(Long.parseLong(RESTAURANT_ID)));
    });

    long consumerId = 1511300065921L;

    Order order = orderService.createOrder(consumerId, Long.parseLong(RESTAURANT_ID), OrderDetailsMother.DELIVERY_INFORMATION, Collections.singletonList(new MenuItemIdAndQuantity(CHICKED_VINDALOO_MENU_ITEM_ID, 5)));

    FtgoTestUtil.assertPresent(orderRepository.findById(order.getId()));

    String expectedPayload = "{\"consumerId\":1511300065921,\"orderId\":1,\"orderTotal\":\"61.70\"}";

    Message message = mockConsumerService.assertMessageReceived(
            commandMessageOfType(ValidateOrderByConsumer.class.getName()).and(withPayload(expectedPayload)));

    System.out.println("message=" + message);

  }

  private Predicate<? super Message> withPayload(String expectedPayload) {
    return (m) -> expectedPayload.equals(m.getPayload());
  }

  private Predicate<Message> forConsumer(long consumerId) {
    return (m) -> {
      Object doc = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(m.getPayload());
      Object s = JsonPath.read(doc, "$.consumerId");
      return new Long(consumerId).equals(s);
    };
  }

  private Predicate<Message> commandMessageOfType(String commandType) {
    return (m) -> m.getRequiredHeader(CommandMessageHeaders.COMMAND_TYPE).equals(commandType);
  }

}

import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CONSUMER_ID;
import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.chickenVindalooLineItems;
import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.AJANTA_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderJpaTestConfiguration.class)
public class OrderJpaTest {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  public void shouldSaveAndLoadOrder() {

    long orderId = transactionTemplate.execute((ts) -> {
      Order order = new Order(CONSUMER_ID, AJANTA_ID, OrderDetailsMother.DELIVERY_INFORMATION, chickenVindalooLineItems());
      orderRepository.save(order);
      return order.getId();
    });


    transactionTemplate.execute((ts) -> {
      Order order = orderRepository.findById(orderId).get();

      assertNotNull(order);
      assertEquals(OrderState.APPROVAL_PENDING, order.getState());
      assertEquals(AJANTA_ID, order.getRestaurantId());
      assertEquals(CONSUMER_ID, order.getConsumerId().longValue());
      assertEquals(chickenVindalooLineItems(), order.getLineItems());
      return null;
    });

  }

}


import io.eventuate.tram.spring.consumer.jdbc.TramConsumerJdbcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories
@EnableAutoConfiguration(exclude = TramConsumerJdbcAutoConfiguration.class)
public class OrderJpaTestConfiguration {
}


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OrderJpaTestConfiguration.class)
public class RestaurantJpaTest {

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  public void shouldSaveAndLoadRestaurant() {
    long restaurantId = saveRestaurant();
    assertEquals(AJANTA_ID, restaurantId);
    loadRestaurant(restaurantId);
  }

  @Test
  public void shouldSaveRestaurantTwice() {
    long restaurantId1 = saveRestaurant();
    long restaurantId2 = saveRestaurant();
    assertEquals(AJANTA_ID, restaurantId1);
    assertEquals(restaurantId1, restaurantId2);
    loadRestaurant(restaurantId1);
  }

  private void loadRestaurant(long restaurantId) {
    transactionTemplate.execute(ts -> {
      Restaurant restaurant = restaurantRepository.findById(restaurantId).get();
      assertEquals(AJANTA_RESTAURANT_NAME, restaurant.getName());
      assertEquals(AJANTA_RESTAURANT_MENU_ITEMS, restaurant.getMenuItems());
      return null;
    });
  }


  private long saveRestaurant() {
    return transactionTemplate.execute((ts) -> {
        Restaurant restaurant = new Restaurant(AJANTA_ID, AJANTA_RESTAURANT_NAME, AJANTA_RESTAURANT_MENU_ITEMS);
        restaurantRepository.save(restaurant);
        return restaurant.getId();
      });
  }

}


import io.eventuate.tram.sagas.spring.inmemory.TramSagaInMemoryConfiguration;
import io.eventuate.tram.sagas.spring.testing.contract.EventuateTramSagasSpringCloudContractSupportConfiguration;
import io.eventuate.tram.sagas.spring.testing.contract.SagaMessagingTestHelper;
import io.eventuate.tram.spring.cloudcontractsupport.EventuateTramRoutesConfigurer;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicket;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicketReply;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketLineItem;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.sagas.createorder.CreateOrderSaga;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.BatchStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static net.chrisrichardson.ftgo.orderservice.OrderDetailsMother.CHICKEN_VINDALOO_QUANTITY;
import static net.chrisrichardson.ftgo.orderservice.RestaurantMother.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= KitchenServiceProxyIntegrationTest.TestConfiguration.class,
        webEnvironment= SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(ids =
        {"net.chrisrichardson.ftgo:ftgo-kitchen-service-contracts"}
        )
@DirtiesContext
public class KitchenServiceProxyIntegrationTest {


  @Configuration
  @EnableAutoConfiguration
  @Import({TramSagaInMemoryConfiguration.class, EventuateTramSagasSpringCloudContractSupportConfiguration.class})
  public static class TestConfiguration {


    @Bean
    public EventuateTramRoutesConfigurer eventuateTramRoutesConfigurer(BatchStubRunner batchStubRunner) {
      return new EventuateTramRoutesConfigurer(batchStubRunner);
    }

    @Bean
    public KitchenServiceProxy kitchenServiceProxy() {
      return new KitchenServiceProxy();
    }
  }

  @Autowired
  private SagaMessagingTestHelper sagaMessagingTestHelper;

  @Autowired
  private KitchenServiceProxy kitchenServiceProxy;

  @Test
  public void shouldSuccessfullyCreateTicket() {
    CreateTicket command = new CreateTicket(AJANTA_ID, OrderDetailsMother.ORDER_ID,
            new TicketDetails(Collections.singletonList(new TicketLineItem(CHICKEN_VINDALOO_MENU_ITEM_ID, CHICKEN_VINDALOO, CHICKEN_VINDALOO_QUANTITY))));
    CreateTicketReply expectedReply = new CreateTicketReply(OrderDetailsMother.ORDER_ID);
    String sagaType = CreateOrderSaga.class.getName();

    CreateTicketReply reply = sagaMessagingTestHelper.sendAndReceiveCommand(kitchenServiceProxy.create, command, CreateTicketReply.class, sagaType);

    assertEquals(expectedReply, reply);

  }

}

import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.orderservice.domain.OrderServiceWithRepositoriesConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({OrderServiceWithRepositoriesConfiguration.class, TramEventSubscriberConfiguration.class})
public class OrderServiceMessagingConfiguration {

  @Bean
  public OrderEventConsumer orderEventConsumer(OrderService orderService) {
    return new OrderEventConsumer(orderService);
  }

  @Bean
  public DomainEventDispatcher domainEventDispatcher(OrderEventConsumer orderEventConsumer, DomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("orderServiceEvents", orderEventConsumer.domainEventHandlers());
  }

}


import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantMenuRevised;


public class OrderEventConsumer {

  private OrderService orderService;

  public OrderEventConsumer(OrderService orderService) {
    this.orderService = orderService;
  }

  public DomainEventHandlers domainEventHandlers() {
    return DomainEventHandlersBuilder
            .forAggregateType("net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant")
            .onEvent(RestaurantCreated.class, this::createMenu)
            .onEvent(RestaurantMenuRevised.class, this::reviseMenu)
            .build();
  }

  private void createMenu(DomainEventEnvelope<RestaurantCreated> de) {
    String restaurantIds = de.getAggregateId();
    long id = Long.parseLong(restaurantIds);
    orderService.createMenu(id, de.getEvent().getName(), RestaurantEventMapper.toMenuItems(de.getEvent().getMenu().getMenuItems()));
  }

  public void reviseMenu(DomainEventEnvelope<RestaurantMenuRevised> de) {
    String restaurantIds = de.getAggregateId();
    long id = Long.parseLong(restaurantIds);
    orderService.reviseMenu(id, RestaurantEventMapper.toMenuItems(de.getEvent().getMenu().getMenuItems()));
  }

}


import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.restaurantservice.events.Address;
import net.chrisrichardson.ftgo.restaurantservice.events.MenuItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class RestaurantEventMapper {

  @NotNull
  public static List<MenuItem> fromMenuItems(List<net.chrisrichardson.ftgo.orderservice.domain.MenuItem> menuItems) {
    return menuItems.stream().map(mi -> new MenuItem().withId(mi.getId()).withName(mi.getName()).withPrice(mi.getPrice().asString())).collect(Collectors.toList());
  }

  public static Address fromAddress(net.chrisrichardson.ftgo.common.Address a) {
    return new Address().withStreet1(a.getStreet1()).withStreet2(a.getStreet2()).withCity(a.getCity()).withZip(a.getZip());
  }

  public static List<net.chrisrichardson.ftgo.orderservice.domain.MenuItem> toMenuItems(List<MenuItem> menuItems) {
    return menuItems.stream().map(mi -> new net.chrisrichardson.ftgo.orderservice.domain.MenuItem(mi.getId(), mi.getName(), new Money(mi.getPrice()))).collect(Collectors.toList());
  }

}


import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfiguration {

  @Bean
  public OrderServiceServer helloWorldServer(OrderService orderService) {
    return new OrderServiceServer(orderService);
  }
}


import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.orderservice.domain.DeliveryInformation;
import net.chrisrichardson.ftgo.orderservice.domain.Order;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.orderservice.web.MenuItemIdAndQuantity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class OrderServiceServer {
  private static final Logger logger = LoggerFactory.getLogger(OrderServiceServer.class);

  private int port = 50051;
  private Server server;
  private OrderService orderService;

  public OrderServiceServer(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostConstruct
  public void start() throws IOException {
    server = ServerBuilder.forPort(port)
            .addService(new OrderServiceImpl())
            .build()
            .start();
    logger.info("Server started, listening on " + port);
  }

  @PreDestroy
  public void stop() {
    if (server != null) {
      logger.info("*** shutting down gRPC server since JVM is shutting down");
      server.shutdown();
      logger.info("*** server shut down");
    }
  }


  private class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    @Override
    public void createOrder(CreateOrderRequest req, StreamObserver<CreateOrderReply> responseObserver) {
      List<LineItem> lineItemsList = req.getLineItemsList();
      Order order = orderService.createOrder(req.getConsumerId(),
              req.getRestaurantId(),
              new DeliveryInformation(LocalDateTime.parse(req.getDeliveryTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME), makeAddress(req.getDeliveryAddress())),
              lineItemsList.stream().map(x -> new MenuItemIdAndQuantity(x.getMenuItemId(), x.getQuantity())).collect(toList())
      );
      CreateOrderReply reply = CreateOrderReply.newBuilder().setOrderId(order.getId()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    private Address makeAddress(net.chrisrichardson.ftgo.orderservice.grpc.Address address) {
      return new Address(address.getStreet1(), nullIfBlank(address.getStreet2()), address.getCity(), address.getState(), address.getZip());
    }

    @Override
    public void cancelOrder(CancelOrderRequest req, StreamObserver<CancelOrderReply> responseObserver) {
      CancelOrderReply reply = CancelOrderReply.newBuilder().setMessage("Hello " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void reviseOrder(ReviseOrderRequest req, StreamObserver<ReviseOrderReply> responseObserver) {
      ReviseOrderReply reply = ReviseOrderReply.newBuilder().setMessage("Hello " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }

  private String nullIfBlank(String s) {
    return StringUtils.isBlank(s) ? null : s;
  }
}


public class GetRestaurantResponse {
  private long restaurantId;

  private GetRestaurantResponse() {
  }

  public GetRestaurantResponse(long restaurantId) {

    this.restaurantId = restaurantId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class MenuItemIdAndQuantity {

  private String menuItemId;
  private int quantity;

  public String getMenuItemId() {
    return menuItemId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setMenuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
  }

  public MenuItemIdAndQuantity(String menuItemId, int quantity) {

    this.menuItemId = menuItemId;
    this.quantity = quantity;

  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}


import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;

public class GetOrderResponse {
  private long orderId;
  private OrderState state;
  private Money orderTotal;

  private GetOrderResponse() {
  }

  public GetOrderResponse(long orderId, OrderState state, Money orderTotal) {
    this.orderId = orderId;
    this.state = state;
    this.orderTotal = orderTotal;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  public OrderState getState() {
    return state;
  }

  public void setState(OrderState state) {
    this.state = state;
  }
}



import net.chrisrichardson.ftgo.orderservice.domain.Restaurant;
import net.chrisrichardson.ftgo.orderservice.domain.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/restaurants")
public class RestaurantController {

  @Autowired
  private RestaurantRepository restaurantRepository;

  @RequestMapping(path="/{restaurantId}", method= RequestMethod.GET)
  public ResponseEntity<GetRestaurantResponse> getRestaurant(@PathVariable long restaurantId) {
    return restaurantRepository.findById(restaurantId)
            .map(restaurant -> new ResponseEntity<>(new GetRestaurantResponse(restaurantId), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }
}




import brave.Span;
import brave.Tracer;
import org.springframework.cloud.sleuth.instrument.web.TraceWebServletAutoConfiguration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(TraceWebServletAutoConfiguration.TRACING_FILTER_ORDER + 1)
class TraceIdResponseFilter extends GenericFilterBean {

  private final Tracer tracer;

  public TraceIdResponseFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override public void doFilter(ServletRequest request, ServletResponse response,
                                 FilterChain chain) throws IOException, ServletException {
    Span currentSpan = this.tracer.currentSpan();
    if (currentSpan != null) {
      ((HttpServletResponse) response)
              .addHeader("ZIPKIN-TRACE-ID",
                      currentSpan.context().traceIdString());
    }
    chain.doFilter(request, response);
  }
}

import net.chrisrichardson.ftgo.orderservice.api.web.CreateOrderRequest;
import net.chrisrichardson.ftgo.orderservice.api.web.CreateOrderResponse;
import net.chrisrichardson.ftgo.orderservice.api.web.ReviseOrderRequest;
import net.chrisrichardson.ftgo.orderservice.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/orders")
public class OrderController {

  private OrderService orderService;

  private OrderRepository orderRepository;


  public OrderController(OrderService orderService, OrderRepository orderRepository) {
    this.orderService = orderService;
    this.orderRepository = orderRepository;
  }

  @RequestMapping(method = RequestMethod.POST)
  public CreateOrderResponse create(@RequestBody CreateOrderRequest request) {
    Order order = orderService.createOrder(request.getConsumerId(),
            request.getRestaurantId(),
            new DeliveryInformation(request.getDeliveryTime(), request.getDeliveryAddress()),
            request.getLineItems().stream().map(x -> new MenuItemIdAndQuantity(x.getMenuItemId(), x.getQuantity())).collect(toList())
    );
    return new CreateOrderResponse(order.getId());
  }


  @RequestMapping(path = "/{orderId}", method = RequestMethod.GET)
  public ResponseEntity<GetOrderResponse> getOrder(@PathVariable long orderId) {
    Optional<Order> order = orderRepository.findById(orderId);
    return order.map(o -> new ResponseEntity<>(makeGetOrderResponse(o), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  private GetOrderResponse makeGetOrderResponse(Order order) {
    return new GetOrderResponse(order.getId(), order.getState(), order.getOrderTotal());
  }

  @RequestMapping(path = "/{orderId}/cancel", method = RequestMethod.POST)
  public ResponseEntity<GetOrderResponse> cancel(@PathVariable long orderId) {
    try {
      Order order = orderService.cancel(orderId);
      return new ResponseEntity<>(makeGetOrderResponse(order), HttpStatus.OK);
    } catch (OrderNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(path = "/{orderId}/revise", method = RequestMethod.POST)
  public ResponseEntity<GetOrderResponse> revise(@PathVariable long orderId, @RequestBody ReviseOrderRequest request) {
    try {
      Order order = orderService.reviseOrder(orderId, new OrderRevision(Optional.empty(), request.getRevisedOrderLineItems()));
      return new ResponseEntity<>(makeGetOrderResponse(order), HttpStatus.OK);
    } catch (OrderNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

}


import brave.sampler.Sampler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.common.json.mapper.JSonMapper;
import net.chrisrichardson.ftgo.orderservice.domain.OrderServiceWithRepositoriesConfiguration;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan
@Import(OrderServiceWithRepositoriesConfiguration.class)
public class OrderWebConfiguration {

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return JSonMapper.objectMapper;
  }

  @Bean
  public Sampler defaultSampler() {
    return Sampler.ALWAYS_SAMPLE;
  }

}


import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.orderservice.domain.OrderRevision;

public class ReviseOrderSagaData  {

  private OrderRevision orderRevision;
  private Long orderId;
  private Long expectedVersion;
  private long restaurantId;
  private Money revisedOrderTotal;
  private long consumerId;

  private ReviseOrderSagaData() {
  }

  public ReviseOrderSagaData(long consumerId, Long orderId, Long expectedVersion, OrderRevision orderRevision) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.expectedVersion = expectedVersion;
    this.orderRevision = orderRevision;
  }

  public Long getExpectedVersion() {
    return expectedVersion;
  }

  public void setExpectedVersion(Long expectedVersion) {
    this.expectedVersion = expectedVersion;
  }

  public void setRevisedOrderTotal(Money revisedOrderTotal) {
    this.revisedOrderTotal = revisedOrderTotal;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }


  public OrderRevision getOrderRevision() {
    return orderRevision;
  }

  public void setOrderRevision(OrderRevision orderRevision) {
    this.orderRevision = orderRevision;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }


  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public Money getRevisedOrderTotal() {
    return revisedOrderTotal;
  }

  public long getConsumerId() {
    return consumerId;
  }
}


import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import net.chrisrichardson.ftgo.accountservice.api.AccountingServiceChannels;
import net.chrisrichardson.ftgo.orderservice.api.OrderServiceChannels;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.BeginReviseOrderCommand;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.BeginReviseOrderReply;
import net.chrisrichardson.ftgo.kitchenservice.api.BeginReviseTicketCommand;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.ConfirmReviseOrderCommand;
import net.chrisrichardson.ftgo.accountservice.api.ReviseAuthorization;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.UndoBeginReviseOrderCommand;
import net.chrisrichardson.ftgo.kitchenservice.api.ConfirmReviseTicketCommand;
import net.chrisrichardson.ftgo.kitchenservice.api.KitchenServiceChannels;
import net.chrisrichardson.ftgo.kitchenservice.api.UndoBeginReviseTicketCommand;
import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import static io.eventuate.tram.commands.consumer.CommandWithDestinationBuilder.send;

public class ReviseOrderSaga implements SimpleSaga<ReviseOrderSagaData> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private SagaDefinition<ReviseOrderSagaData> sagaDefinition;

  @PostConstruct
  public void initializeSagaDefinition() {
    sagaDefinition = step()
            .invokeParticipant(this::beginReviseOrder)
            .onReply(BeginReviseOrderReply.class, this::handleBeginReviseOrderReply)
            .withCompensation(this::undoBeginReviseOrder)
            .step()
            .invokeParticipant(this::beginReviseTicket)
            .withCompensation(this::undoBeginReviseTicket)
            .step()
            .invokeParticipant(this::reviseAuthorization)
            .step()
            .invokeParticipant(this::confirmTicketRevision)
            .step()
            .invokeParticipant(this::confirmOrderRevision)
            .build();
  }

  private void handleBeginReviseOrderReply(ReviseOrderSagaData data, BeginReviseOrderReply reply) {
    logger.info("ƒ order total: {}", reply.getRevisedOrderTotal());
    data.setRevisedOrderTotal(reply.getRevisedOrderTotal());
  }

  @Override
  public SagaDefinition<ReviseOrderSagaData> getSagaDefinition() {
    return sagaDefinition;
  }

  private CommandWithDestination confirmOrderRevision(ReviseOrderSagaData data) {
    return send(new ConfirmReviseOrderCommand(data.getOrderId(), data.getOrderRevision()))
            .to(OrderServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination confirmTicketRevision(ReviseOrderSagaData data) {
    return send(new ConfirmReviseTicketCommand(data.getRestaurantId(), data.getOrderId(), data.getOrderRevision().getRevisedOrderLineItems()))
            .to(KitchenServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination reviseAuthorization(ReviseOrderSagaData data) {
    return send(new ReviseAuthorization(data.getConsumerId(), data.getOrderId(), data.getRevisedOrderTotal()))
            .to(AccountingServiceChannels.accountingServiceChannel)
            .build();

  }

  private CommandWithDestination undoBeginReviseTicket(ReviseOrderSagaData data) {
    return send(new UndoBeginReviseTicketCommand(data.getRestaurantId(), data.getOrderId()))
            .to(KitchenServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination beginReviseTicket(ReviseOrderSagaData data) {
    return send(new BeginReviseTicketCommand(data.getRestaurantId(), data.getOrderId(), data.getOrderRevision().getRevisedOrderLineItems()))
            .to(KitchenServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination undoBeginReviseOrder(ReviseOrderSagaData data) {
    return send(new UndoBeginReviseOrderCommand(data.getOrderId()))
            .to(OrderServiceChannels.COMMAND_CHANNEL)
            .build();
  }

  private CommandWithDestination beginReviseOrder(ReviseOrderSagaData data) {
    return send(new BeginReviseOrderCommand(data.getOrderId(), data.getOrderRevision()))
            .to(OrderServiceChannels.COMMAND_CHANNEL)
            .build();

  }



}


public enum ReviseOrderSagaState {
  REQUESTING_RESTAURANT_ORDER_UPDATE, AUTHORIZATION_INCREASED, COMPLETED, REQUESTING_AUTHORIZATION, REVERSING_ORDER_UPDATE, REVERSING_AUTHORIZATION, WAITING_FOR_CHANGE_TO_BE_MADE
}


import net.chrisrichardson.ftgo.common.Money;

public class CancelOrderSagaData  {

  private Long orderId;
  private String reverseRequestId;
  private long restaurantId;
  private long consumerId;
  private Money orderTotal;

  private CancelOrderSagaData() {
  }

  public CancelOrderSagaData(long consumerId, long orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  public Long getOrderId() {
    return orderId;
  }


  public String getReverseRequestId() {
    return reverseRequestId;
  }

  public void setReverseRequestId(String reverseRequestId) {
    this.reverseRequestId = reverseRequestId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }
}


import io.eventuate.tram.commands.consumer.CommandWithDestination;
import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import net.chrisrichardson.ftgo.accountservice.api.AccountingServiceChannels;
import net.chrisrichardson.ftgo.accountservice.api.ReverseAuthorizationCommand;
import net.chrisrichardson.ftgo.orderservice.api.OrderServiceChannels;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.BeginCancelCommand;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.ConfirmCancelOrderCommand;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.UndoBeginCancelCommand;
import net.chrisrichardson.ftgo.kitchenservice.api.BeginCancelTicketCommand;
import net.chrisrichardson.ftgo.kitchenservice.api.ConfirmCancelTicketCommand;
import net.chrisrichardson.ftgo.kitchenservice.api.KitchenServiceChannels;
import net.chrisrichardson.ftgo.kitchenservice.api.UndoBeginCancelTicketCommand;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

import static io.eventuate.tram.commands.consumer.CommandWithDestinationBuilder.send;

public class CancelOrderSaga implements SimpleSaga<CancelOrderSagaData> {



  private SagaDefinition<CancelOrderSagaData> sagaDefinition;


  @PostConstruct
  public void initializeSagaDefinition() {
    sagaDefinition = step()
            .invokeParticipant(this::beginCancel)
            .withCompensation(this::undoBeginCancel)
            .step()
            .invokeParticipant(this::beginCancelTicket)
            .withCompensation(this::undoBeginCancelTicket)
            .step()
            .invokeParticipant(this::reverseAuthorization)
            .step()
            .invokeParticipant(this::confirmTicketCancel)
            .step()
            .invokeParticipant(this::confirmOrderCancel)
            .build();

  }

  private CommandWithDestination confirmOrderCancel(CancelOrderSagaData data) {
    return send(new ConfirmCancelOrderCommand(data.getOrderId()))
            .to(OrderServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination confirmTicketCancel(CancelOrderSagaData data) {
    return send(new ConfirmCancelTicketCommand(data.getRestaurantId(), data.getOrderId()))
            .to(KitchenServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination reverseAuthorization(CancelOrderSagaData data) {
    return send(new ReverseAuthorizationCommand(data.getConsumerId(), data.getOrderId(), data.getOrderTotal()))
            .to(AccountingServiceChannels.accountingServiceChannel)
            .build();

  }

  private CommandWithDestination undoBeginCancelTicket(CancelOrderSagaData data) {
    return send(new UndoBeginCancelTicketCommand(data.getRestaurantId(), data.getOrderId()))
            .to(KitchenServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination beginCancelTicket(CancelOrderSagaData data) {
    return send(new BeginCancelTicketCommand(data.getRestaurantId(), (long) data.getOrderId()))
            .to(KitchenServiceChannels.COMMAND_CHANNEL)
            .build();

  }

  private CommandWithDestination undoBeginCancel(CancelOrderSagaData data) {
    return send(new UndoBeginCancelCommand(data.getOrderId()))
            .to(OrderServiceChannels.COMMAND_CHANNEL)
            .build();
  }

  private CommandWithDestination beginCancel(CancelOrderSagaData data) {
    return send(new BeginCancelCommand(data.getOrderId()))
            .to(OrderServiceChannels.COMMAND_CHANNEL)
            .build();
  }


  @Override
  public SagaDefinition<CancelOrderSagaData> getSagaDefinition() {
    Assert.notNull(sagaDefinition);
    return sagaDefinition;
  }



}


public enum CancelOrderSagaState {
  state, WAITING_TO_AUTHORIZE, COMPLETED, REVERSING
}


import net.chrisrichardson.ftgo.accountservice.api.AuthorizeCommand;
import net.chrisrichardson.ftgo.consumerservice.api.ValidateOrderByConsumer;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDetails;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderLineItem;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.ApproveOrderCommand;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.RejectOrderCommand;
import net.chrisrichardson.ftgo.kitchenservice.api.*;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class CreateOrderSagaState {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private Long orderId;

  private OrderDetails orderDetails;
  private long ticketId;

  public Long getOrderId() {
    return orderId;
  }

  private CreateOrderSagaState() {
  }

  public CreateOrderSagaState(Long orderId, OrderDetails orderDetails) {
    this.orderId = orderId;
    this.orderDetails = orderDetails;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public OrderDetails getOrderDetails() {
    return orderDetails;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public void setTicketId(long ticketId) {
    this.ticketId = ticketId;
  }

  public long getTicketId() {
    return ticketId;
  }

  CreateTicket makeCreateTicketCommand() {
    return new CreateTicket(getOrderDetails().getRestaurantId(), getOrderId(), makeTicketDetails(getOrderDetails()));
  }

  private TicketDetails makeTicketDetails(OrderDetails orderDetails) {
    // TODO FIXME
    return new TicketDetails(makeTicketLineItems(orderDetails.getLineItems()));
  }

  private List<TicketLineItem> makeTicketLineItems(List<OrderLineItem> lineItems) {
    return lineItems.stream().map(this::makeTicketLineItem).collect(toList());
  }

  private TicketLineItem makeTicketLineItem(OrderLineItem orderLineItem) {
    return new TicketLineItem(orderLineItem.getMenuItemId(), orderLineItem.getName(), orderLineItem.getQuantity());
  }

  void handleCreateTicketReply(CreateTicketReply reply) {
    logger.debug("getTicketId {}", reply.getTicketId());
    setTicketId(reply.getTicketId());
  }

  CancelCreateTicket makeCancelCreateTicketCommand() {
    return new CancelCreateTicket(getOrderId());
  }

  RejectOrderCommand makeRejectOrderCommand() {
    return new RejectOrderCommand(getOrderId());
  }

  ValidateOrderByConsumer makeValidateOrderByConsumerCommand() {
    ValidateOrderByConsumer x = new ValidateOrderByConsumer();
    x.setConsumerId(getOrderDetails().getConsumerId());
    x.setOrderId(getOrderId());
    x.setOrderTotal(getOrderDetails().getOrderTotal().asString());
    return x;
  }

  AuthorizeCommand makeAuthorizeCommand() {
    return new AuthorizeCommand().withConsumerId(getOrderDetails().getConsumerId()).withOrderId(getOrderId()).withOrderTotal(getOrderDetails().getOrderTotal().asString());
  }

  ApproveOrderCommand makeApproveOrderCommand() {
    return new ApproveOrderCommand(getOrderId());
  }

  ConfirmCreateTicket makeConfirmCreateTicketCommand() {
    return new ConfirmCreateTicket(getTicketId());

  }
}


import io.eventuate.tram.sagas.orchestration.SagaDefinition;
import io.eventuate.tram.sagas.simpledsl.SimpleSaga;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.*;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicketReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateOrderSaga implements SimpleSaga<CreateOrderSagaState> {


  private Logger logger = LoggerFactory.getLogger(getClass());

  private SagaDefinition<CreateOrderSagaState> sagaDefinition;

  public CreateOrderSaga(OrderServiceProxy orderService, ConsumerServiceProxy consumerService, KitchenServiceProxy kitchenService,
                         AccountingServiceProxy accountingService) {
    this.sagaDefinition =
             step()
              .withCompensation(orderService.reject, CreateOrderSagaState::makeRejectOrderCommand)
            .step()
              .invokeParticipant(consumerService.validateOrder, CreateOrderSagaState::makeValidateOrderByConsumerCommand)
            .step()
              .invokeParticipant(kitchenService.create, CreateOrderSagaState::makeCreateTicketCommand)
              .onReply(CreateTicketReply.class, CreateOrderSagaState::handleCreateTicketReply)
              .withCompensation(kitchenService.cancel, CreateOrderSagaState::makeCancelCreateTicketCommand)
            .step()
                .invokeParticipant(accountingService.authorize, CreateOrderSagaState::makeAuthorizeCommand)
            .step()
              .invokeParticipant(kitchenService.confirmCreate, CreateOrderSagaState::makeConfirmCreateTicketCommand)
            .step()
              .invokeParticipant(orderService.approve, CreateOrderSagaState::makeApproveOrderCommand)
            .build();

  }


  @Override
  public SagaDefinition<CreateOrderSagaState> getSagaDefinition() {
    return sagaDefinition;
  }


}


import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcher;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcherFactory;
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SagaParticipantConfiguration.class, TramEventsPublisherConfiguration.class, CommonConfiguration.class, SagaParticipantConfiguration.class})
public class OrderCommandHandlersConfiguration {

  @Bean
  public OrderCommandHandlers orderCommandHandlers() {
    return new OrderCommandHandlers();
  }

  @Bean
  public SagaCommandDispatcher orderCommandHandlersDispatcher(OrderCommandHandlers orderCommandHandlers, SagaCommandDispatcherFactory sagaCommandDispatcherFactory) {
    return sagaCommandDispatcherFactory.make("orderService", orderCommandHandlers.commandHandlers());
  }

}


import io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder;
import net.chrisrichardson.ftgo.orderservice.domain.OrderRepository;
import net.chrisrichardson.ftgo.orderservice.domain.OrderRevision;
import net.chrisrichardson.ftgo.orderservice.domain.OrderService;
import net.chrisrichardson.ftgo.orderservice.domain.RevisedOrder;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.*;
import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;
import net.chrisrichardson.ftgo.common.UnsupportedStateTransitionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class OrderCommandHandlers {

  @Autowired
  private OrderService orderService;

  public CommandHandlers commandHandlers() {
    return SagaCommandHandlersBuilder
          .fromChannel("orderService")
          .onMessage(ApproveOrderCommand.class, this::approveOrder)
          .onMessage(RejectOrderCommand.class, this::rejectOrder)

          .onMessage(BeginCancelCommand.class, this::beginCancel)
          .onMessage(UndoBeginCancelCommand.class, this::undoCancel)
          .onMessage(ConfirmCancelOrderCommand.class, this::confirmCancel)

          .onMessage(BeginReviseOrderCommand.class, this::beginReviseOrder)
          .onMessage(UndoBeginReviseOrderCommand.class, this::undoPendingRevision)
          .onMessage(ConfirmReviseOrderCommand.class, this::confirmRevision)
          .build();

  }

  public Message approveOrder(CommandMessage<ApproveOrderCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    orderService.approveOrder(orderId);
    return withSuccess();
  }


  public Message rejectOrder(CommandMessage<RejectOrderCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    orderService.rejectOrder(orderId);
    return withSuccess();
  }


  public Message beginCancel(CommandMessage<BeginCancelCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    try {
      orderService.beginCancel(orderId);
      return withSuccess();
    } catch (UnsupportedStateTransitionException e) {
      return withFailure();
    }
  }


  public Message undoCancel(CommandMessage<UndoBeginCancelCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    orderService.undoCancel(orderId);
    return withSuccess();
  }

  public Message confirmCancel(CommandMessage<ConfirmCancelOrderCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    orderService.confirmCancelled(orderId);
    return withSuccess();
  }


  public Message beginReviseOrder(CommandMessage<BeginReviseOrderCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    OrderRevision revision = cm.getCommand().getRevision();
    try {
      return orderService.beginReviseOrder(orderId, revision).map(result -> withSuccess(new BeginReviseOrderReply(result.getChange().getNewOrderTotal()))).orElseGet(CommandHandlerReplyBuilder::withFailure);
    } catch (UnsupportedStateTransitionException e) {
      return withFailure();
    }
  }

  public Message undoPendingRevision(CommandMessage <UndoBeginReviseOrderCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    orderService.undoPendingRevision(orderId);
    return withSuccess();
  }

  public Message confirmRevision(CommandMessage<ConfirmReviseOrderCommand> cm) {
    long orderId = cm.getCommand().getOrderId();
    OrderRevision revision = cm.getCommand().getRevision();
    orderService.confirmRevision(orderId, revision);
    return withSuccess();
  }

}


public class RevisedOrder {
  private final Order order;
  private final LineItemQuantityChange change;

  public RevisedOrder(Order order, LineItemQuantityChange change) {
    this.order = order;
    this.change = change;
  }

  public Order getOrder() {
    return order;
  }

  public LineItemQuantityChange getChange() {
    return change;
  }
}


import io.eventuate.tram.events.common.DomainEvent;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Result {


  private final List<DomainEvent> events;
  private Boolean allowed;

  public List<DomainEvent> getEvents() {
    return events;
  }

  public Result(List<DomainEvent> events, Boolean allowed) {
    this.events = events;
    this.allowed = allowed;
  }

  public boolean isWhatIsThisCalled() {
    return allowed;
  }

  public static Builder build() {
    return new Builder();
  }

  public static class Builder {
    private List<DomainEvent> events = new LinkedList<>();
    private Boolean allowed;

    public Builder withEvents(DomainEvent... events) {
      Arrays.stream(events).forEach(e -> this.events.add(e));
      return this;
    }

    public Builder pending() {
      this.allowed = false;
      return this;
    }

    public Result build() {
      Assert.notNull(allowed);
      return new Result(events, allowed);
    }

    public Builder allowed() {
      this.allowed = false;
      return this;
    }
  }
}


public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(Long orderId) {
    super("Order not found" + orderId);
  }
}



import io.eventuate.tram.events.common.DomainEvent;

public class OrderRevisionRejected implements DomainEvent {

  public OrderRevisionRejected(OrderRevision orderRevision) {
    throw new UnsupportedOperationException();
  }

}


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories
@EnableAutoConfiguration
@Import({OrderServiceConfiguration.class})
public class OrderServiceWithRepositoriesConfiguration {


}


import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.common.UnsupportedStateTransitionException;
import net.chrisrichardson.ftgo.orderservice.api.events.*;

import javax.persistence.*;
import java.util.List;

import static net.chrisrichardson.ftgo.orderservice.api.events.OrderState.APPROVED;
import static net.chrisrichardson.ftgo.orderservice.api.events.OrderState.APPROVAL_PENDING;
import static net.chrisrichardson.ftgo.orderservice.api.events.OrderState.REJECTED;
import static net.chrisrichardson.ftgo.orderservice.api.events.OrderState.REVISION_PENDING;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Entity
@Table(name = "orders")
@Access(AccessType.FIELD)
public class Order {

  public static ResultWithDomainEvents<Order, OrderDomainEvent>
  createOrder(long consumerId, Restaurant restaurant, DeliveryInformation deliveryInformation, List<OrderLineItem> orderLineItems) {
    Order order = new Order(consumerId, restaurant.getId(), deliveryInformation, orderLineItems);
    List<OrderDomainEvent> events = singletonList(new OrderCreatedEvent(
            new OrderDetails(consumerId, restaurant.getId(), orderLineItems,
                    order.getOrderTotal()),
            deliveryInformation.getDeliveryAddress(),
            restaurant.getName()));
    return new ResultWithDomainEvents<>(order, events);
  }

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Long version;

  @Enumerated(EnumType.STRING)
  private OrderState state;

  private Long consumerId;
  private Long restaurantId;

  @Embedded
  private OrderLineItems orderLineItems;

  @Embedded
  private DeliveryInformation deliveryInformation;

  @Embedded
  private PaymentInformation paymentInformation;

  @Embedded
  private Money orderMinimum = new Money(Integer.MAX_VALUE);

  private Order() {
  }

  public Order(long consumerId, long restaurantId, DeliveryInformation deliveryInformation, List<OrderLineItem> orderLineItems) {
    this.consumerId = consumerId;
    this.restaurantId = restaurantId;
    this.deliveryInformation = deliveryInformation;
    this.orderLineItems = new OrderLineItems(orderLineItems);
    this.state = APPROVAL_PENDING;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public DeliveryInformation getDeliveryInformation() {
    return deliveryInformation;
  }

  public Money getOrderTotal() {
    return orderLineItems.orderTotal();
  }

  public List<OrderDomainEvent> cancel() {
    switch (state) {
      case APPROVED:
        this.state = OrderState.CANCEL_PENDING;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<OrderDomainEvent> undoPendingCancel() {
    switch (state) {
      case CANCEL_PENDING:
        this.state = OrderState.APPROVED;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<OrderDomainEvent> noteCancelled() {
    switch (state) {
      case CANCEL_PENDING:
        this.state = OrderState.CANCELLED;
        return singletonList(new OrderCancelled());
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<OrderDomainEvent> noteApproved() {
    switch (state) {
      case APPROVAL_PENDING:
        this.state = APPROVED;
        return singletonList(new OrderAuthorized());
      default:
        throw new UnsupportedStateTransitionException(state);
    }

  }

  public List<OrderDomainEvent> noteRejected() {
    switch (state) {
      case APPROVAL_PENDING:
        this.state = REJECTED;
        return singletonList(new OrderRejected());

      default:
        throw new UnsupportedStateTransitionException(state);
    }

  }


  public List<OrderDomainEvent> noteReversingAuthorization() {
    return null;
  }

  public ResultWithDomainEvents<LineItemQuantityChange, OrderDomainEvent> revise(OrderRevision orderRevision) {
    switch (state) {

      case APPROVED:
        LineItemQuantityChange change = orderLineItems.lineItemQuantityChange(orderRevision);
        if (change.newOrderTotal.isGreaterThanOrEqual(orderMinimum)) {
          throw new OrderMinimumNotMetException();
        }
        this.state = REVISION_PENDING;
        return new ResultWithDomainEvents<>(change, singletonList(new OrderRevisionProposed(orderRevision, change.currentOrderTotal, change.newOrderTotal)));

      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<OrderDomainEvent> rejectRevision() {
    switch (state) {
      case REVISION_PENDING:
        this.state = APPROVED;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<OrderDomainEvent> confirmRevision(OrderRevision orderRevision) {
    switch (state) {
      case REVISION_PENDING:
        LineItemQuantityChange licd = orderLineItems.lineItemQuantityChange(orderRevision);

        orderRevision.getDeliveryInformation().ifPresent(newDi -> this.deliveryInformation = newDi);

        if (orderRevision.getRevisedOrderLineItems() != null && orderRevision.getRevisedOrderLineItems().size() > 0) {
          orderLineItems.updateLineItems(orderRevision);
        }

        this.state = APPROVED;
        return singletonList(new OrderRevised(orderRevision, licd.currentOrderTotal, licd.newOrderTotal));
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }


  public Long getVersion() {
    return version;
  }

  public List<OrderLineItem> getLineItems() {
    return orderLineItems.getLineItems();
  }

  public OrderState getState() {
    return state;
  }

  public long getRestaurantId() {
    return restaurantId;
  }


  public Long getConsumerId() {
    return consumerId;
  }
}



public class OptimisticOfflineLockException extends RuntimeException {

}


import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderLineItem;
import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Embeddable
public class OrderLineItems {

  @ElementCollection
  @CollectionTable(name = "order_line_items")
  private List<OrderLineItem> lineItems;

  private OrderLineItems() {
  }

  public OrderLineItems(List<OrderLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  public List<OrderLineItem> getLineItems() {
    return lineItems;
  }

  public void setLineItems(List<OrderLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  OrderLineItem findOrderLineItem(String lineItemId) {
    return lineItems.stream().filter(li -> li.getMenuItemId().equals(lineItemId)).findFirst().get();
  }

  Money changeToOrderTotal(OrderRevision orderRevision) {
    return orderRevision
            .getRevisedOrderLineItems()
            .stream()
            .map(item -> {
              OrderLineItem lineItem = findOrderLineItem(item.getMenuItemId());
              return lineItem.deltaForChangedQuantity(item.getQuantity());
            })
            .reduce(Money.ZERO, Money::add);
  }

  void updateLineItems(OrderRevision orderRevision) {
    getLineItems().stream().forEach(li -> {

      Optional<Integer> revised = orderRevision.getRevisedOrderLineItems()
              .stream()
              .filter(item -> Objects.equals(li.getMenuItemId(), item.getMenuItemId()))
              .map(RevisedOrderLineItem::getQuantity)
              .findFirst();

      li.setQuantity(revised.orElseThrow(() ->
              new IllegalArgumentException(String.format("menu item id not found.", li.getMenuItemId()))));
    });
  }

  Money orderTotal() {
    return lineItems.stream().map(OrderLineItem::getTotal).reduce(Money.ZERO, Money::add);
  }

  LineItemQuantityChange lineItemQuantityChange(OrderRevision orderRevision) {
    Money currentOrderTotal = orderTotal();
    Money delta = changeToOrderTotal(orderRevision);
    Money newOrderTotal = currentOrderTotal.add(delta);
    return new LineItemQuantityChange(currentOrderTotal, newOrderTotal, delta);
  }
}

import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "order_service_restaurants")
@Access(AccessType.FIELD)
public class Restaurant {

  @Id
  private Long id;

  @Embedded
  @ElementCollection
  @CollectionTable(name = "order_service_restaurant_menu_items")
  private List<MenuItem> menuItems;
  private String name;

  private Restaurant() {
  }

  public Restaurant(long id, String name, List<MenuItem> menuItems) {
    this.id = id;
    this.name = name;
    this.menuItems = menuItems;
  }

  public List<OrderDomainEvent> reviseMenu(List<MenuItem> revisedMenu) {
    throw new UnsupportedOperationException();
  }

  public void verifyRestaurantDetails(TicketDetails ticketDetails) {
    // TODO - implement me
  }

  public Long getId() {
    return id;
  }

  public Optional<MenuItem> findMenuItem(String menuItemId) {
    return menuItems.stream().filter(mi -> mi.getId().equals(menuItemId)).findFirst();
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
  }

  public String getName() {
    return name;
  }
}


import net.chrisrichardson.ftgo.common.Money;

public class LineItemQuantityChange {
  final Money currentOrderTotal;
  final Money newOrderTotal;
  final Money delta;

  public LineItemQuantityChange(Money currentOrderTotal, Money newOrderTotal, Money delta) {
    this.currentOrderTotal = currentOrderTotal;
    this.newOrderTotal = newOrderTotal;
    this.delta = delta;
  }

  public Money getCurrentOrderTotal() {
    return currentOrderTotal;
  }

  public Money getNewOrderTotal() {
    return newOrderTotal;
  }

  public Money getDelta() {
    return delta;
  }
}


// import io.eventuate.tram.events.common.DomainEvent;

import io.eventuate.tram.events.common.DomainEvent;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;

public class OrderRevisionProposed implements OrderDomainEvent {


  private final OrderRevision orderRevision;
  private final Money currentOrderTotal;
  private final Money newOrderTotal;

  public OrderRevisionProposed(OrderRevision orderRevision, Money currentOrderTotal, Money newOrderTotal) {
    this.orderRevision = orderRevision;
    this.currentOrderTotal = currentOrderTotal;
    this.newOrderTotal = newOrderTotal;
  }

  public OrderRevision getOrderRevision() {
    return orderRevision;
  }

  public Money getCurrentOrderTotal() {
    return currentOrderTotal;
  }

  public Money getNewOrderTotal() {
    return newOrderTotal;
  }
}


import io.eventuate.tram.events.aggregates.AbstractAggregateDomainEventPublisher;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;

public class OrderDomainEventPublisher extends AbstractAggregateDomainEventPublisher<Order, OrderDomainEvent> {


  public OrderDomainEventPublisher(DomainEventPublisher eventPublisher) {
    super(eventPublisher, Order.class, Order::getId);
  }

}


import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)
public class MenuItem {

  private String id;
  private String name;
  private Money price;

  private MenuItem() {
  }

  public MenuItem(String id, String name, Money price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(Money price) {
    this.price = price;
  }
}


import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.sagas.orchestration.*;
import io.eventuate.tram.sagas.spring.orchestration.SagaOrchestratorConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.AccountingServiceProxy;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.ConsumerServiceProxy;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.KitchenServiceProxy;
import net.chrisrichardson.ftgo.orderservice.sagaparticipants.OrderServiceProxy;
import net.chrisrichardson.ftgo.orderservice.sagas.cancelorder.CancelOrderSaga;
import net.chrisrichardson.ftgo.orderservice.sagas.createorder.CreateOrderSaga;
import net.chrisrichardson.ftgo.orderservice.sagas.reviseorder.ReviseOrderSaga;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Optional;

@Configuration
@Import({TramEventsPublisherConfiguration.class, SagaOrchestratorConfiguration.class, CommonConfiguration.class})
public class OrderServiceConfiguration {

  @Bean
  public OrderService orderService(SagaInstanceFactory sagaInstanceFactory,
                                   RestaurantRepository restaurantRepository,
                                   OrderRepository orderRepository,
                                   DomainEventPublisher eventPublisher,
                                   CreateOrderSaga createOrderSaga,
                                   CancelOrderSaga cancelOrderSaga,
                                   ReviseOrderSaga reviseOrderSaga,
                                   OrderDomainEventPublisher orderAggregateEventPublisher,
                                   Optional<MeterRegistry> meterRegistry) {

    return new OrderService(sagaInstanceFactory, orderRepository, eventPublisher, restaurantRepository,
            createOrderSaga, cancelOrderSaga, reviseOrderSaga, orderAggregateEventPublisher, meterRegistry);
  }

  @Bean
  public CreateOrderSaga createOrderSaga(OrderServiceProxy orderService, ConsumerServiceProxy consumerService, KitchenServiceProxy kitchenServiceProxy, AccountingServiceProxy accountingService) {
    return new CreateOrderSaga(orderService, consumerService, kitchenServiceProxy, accountingService);
  }

  @Bean
  public CancelOrderSaga cancelOrderSaga() {
    return new CancelOrderSaga();
  }

  @Bean
  public ReviseOrderSaga reviseOrderSaga() {
    return new ReviseOrderSaga();
  }


  @Bean
  public KitchenServiceProxy kitchenServiceProxy() {
    return new KitchenServiceProxy();
  }

  @Bean
  public OrderServiceProxy orderServiceProxy() {
    return new OrderServiceProxy();
  }

  @Bean
  public ConsumerServiceProxy consumerServiceProxy() {
    return new ConsumerServiceProxy();
  }

  @Bean
  public AccountingServiceProxy accountingServiceProxy() {
    return new AccountingServiceProxy();
  }

  @Bean
  public OrderDomainEventPublisher orderAggregateEventPublisher(DomainEventPublisher eventPublisher) {
    return new OrderDomainEventPublisher(eventPublisher);
  }

  @Bean
  public MeterRegistryCustomizer meterRegistryCustomizer(@Value("${spring.application.name}") String serviceName) {
    return registry -> registry.config().commonTags("service", serviceName);
  }
}



import io.eventuate.tram.events.common.DomainEvent;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;

public class OrderRevised implements OrderDomainEvent {

  private final OrderRevision orderRevision;
  private final Money currentOrderTotal;
  private final Money newOrderTotal;

  public OrderRevision getOrderRevision() {
    return orderRevision;
  }

  public Money getCurrentOrderTotal() {
    return currentOrderTotal;
  }

  public Money getNewOrderTotal() {
    return newOrderTotal;
  }

  public OrderRevised(OrderRevision orderRevision, Money currentOrderTotal, Money newOrderTotal) {
    this.orderRevision = orderRevision;
    this.currentOrderTotal = currentOrderTotal;
    this.newOrderTotal = newOrderTotal;


  }
}


import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.sagas.orchestration.SagaInstanceFactory;
import io.micrometer.core.instrument.MeterRegistry;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDetails;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDomainEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderLineItem;
import net.chrisrichardson.ftgo.orderservice.sagas.cancelorder.CancelOrderSaga;
import net.chrisrichardson.ftgo.orderservice.sagas.cancelorder.CancelOrderSagaData;
import net.chrisrichardson.ftgo.orderservice.sagas.createorder.CreateOrderSaga;
import net.chrisrichardson.ftgo.orderservice.sagas.createorder.CreateOrderSagaState;
import net.chrisrichardson.ftgo.orderservice.sagas.reviseorder.ReviseOrderSaga;
import net.chrisrichardson.ftgo.orderservice.sagas.reviseorder.ReviseOrderSagaData;
import net.chrisrichardson.ftgo.orderservice.web.MenuItemIdAndQuantity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class OrderService {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private SagaInstanceFactory sagaInstanceFactory;

  private OrderRepository orderRepository;

  private RestaurantRepository restaurantRepository;

  private CreateOrderSaga createOrderSaga;

  private CancelOrderSaga cancelOrderSaga;

  private ReviseOrderSaga reviseOrderSaga;

  private OrderDomainEventPublisher orderAggregateEventPublisher;

  private Optional<MeterRegistry> meterRegistry;

  public OrderService(SagaInstanceFactory sagaInstanceFactory,
                      OrderRepository orderRepository,
                      DomainEventPublisher eventPublisher,
                      RestaurantRepository restaurantRepository,
                      CreateOrderSaga createOrderSaga,
                      CancelOrderSaga cancelOrderSaga,
                      ReviseOrderSaga reviseOrderSaga,
                      OrderDomainEventPublisher orderAggregateEventPublisher,
                      Optional<MeterRegistry> meterRegistry) {

    this.sagaInstanceFactory = sagaInstanceFactory;
    this.orderRepository = orderRepository;
    this.restaurantRepository = restaurantRepository;
    this.createOrderSaga = createOrderSaga;
    this.cancelOrderSaga = cancelOrderSaga;
    this.reviseOrderSaga = reviseOrderSaga;
    this.orderAggregateEventPublisher = orderAggregateEventPublisher;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public Order createOrder(long consumerId, long restaurantId, DeliveryInformation deliveryInformation,
                           List<MenuItemIdAndQuantity> lineItems) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

    List<OrderLineItem> orderLineItems = makeOrderLineItems(lineItems, restaurant);

    ResultWithDomainEvents<Order, OrderDomainEvent> orderAndEvents =
            Order.createOrder(consumerId, restaurant, deliveryInformation, orderLineItems);

    Order order = orderAndEvents.result;
    orderRepository.save(order);

    orderAggregateEventPublisher.publish(order, orderAndEvents.events);

    OrderDetails orderDetails = new OrderDetails(consumerId, restaurantId, orderLineItems, order.getOrderTotal());

    CreateOrderSagaState data = new CreateOrderSagaState(order.getId(), orderDetails);
    sagaInstanceFactory.create(createOrderSaga, data);

    meterRegistry.ifPresent(mr -> mr.counter("placed_orders").increment());

    return order;
  }


  private List<OrderLineItem> makeOrderLineItems(List<MenuItemIdAndQuantity> lineItems, Restaurant restaurant) {
    return lineItems.stream().map(li -> {
      MenuItem om = restaurant.findMenuItem(li.getMenuItemId()).orElseThrow(() -> new InvalidMenuItemIdException(li.getMenuItemId()));
      return new OrderLineItem(li.getMenuItemId(), om.getName(), om.getPrice(), li.getQuantity());
    }).collect(toList());
  }


  public Optional<Order> confirmChangeLineItemQuantity(Long orderId, OrderRevision orderRevision) {
    return orderRepository.findById(orderId).map(order -> {
      List<OrderDomainEvent> events = order.confirmRevision(orderRevision);
      orderAggregateEventPublisher.publish(order, events);
      return order;
    });
  }

  public void noteReversingAuthorization(Long orderId) {
    throw new UnsupportedOperationException();
  }

  @Transactional
  public Order cancel(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    CancelOrderSagaData sagaData = new CancelOrderSagaData(order.getConsumerId(), orderId, order.getOrderTotal());
    sagaInstanceFactory.create(cancelOrderSaga, sagaData);
    return order;
  }

  private Order updateOrder(long orderId, Function<Order, List<OrderDomainEvent>> updater) {
    return orderRepository.findById(orderId).map(order -> {
      orderAggregateEventPublisher.publish(order, updater.apply(order));
      return order;
    }).orElseThrow(() -> new OrderNotFoundException(orderId));
  }

  public void approveOrder(long orderId) {
    updateOrder(orderId, Order::noteApproved);
    meterRegistry.ifPresent(mr -> mr.counter("approved_orders").increment());
  }

  public void rejectOrder(long orderId) {
    updateOrder(orderId, Order::noteRejected);
    meterRegistry.ifPresent(mr -> mr.counter("rejected_orders").increment());
  }

  public void beginCancel(long orderId) {
    updateOrder(orderId, Order::cancel);
  }

  public void undoCancel(long orderId) {
    updateOrder(orderId, Order::undoPendingCancel);
  }

  public void confirmCancelled(long orderId) {
    updateOrder(orderId, Order::noteCancelled);
  }

  @Transactional
  public Order reviseOrder(long orderId, OrderRevision orderRevision) {
    Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    ReviseOrderSagaData sagaData = new ReviseOrderSagaData(order.getConsumerId(), orderId, null, orderRevision);
    sagaInstanceFactory.create(reviseOrderSaga, sagaData);
    return order;
  }

  public Optional<RevisedOrder> beginReviseOrder(long orderId, OrderRevision revision) {
    return orderRepository.findById(orderId).map(order -> {
      ResultWithDomainEvents<LineItemQuantityChange, OrderDomainEvent> result = order.revise(revision);
      orderAggregateEventPublisher.publish(order, result.events);
      return new RevisedOrder(order, result.result);
    });
  }

  public void undoPendingRevision(long orderId) {
    updateOrder(orderId, Order::rejectRevision);
  }

  public void confirmRevision(long orderId, OrderRevision revision) {
    updateOrder(orderId, order -> order.confirmRevision(revision));
  }

  public void createMenu(long id, String name, List<MenuItem> menuItems) {
    Restaurant restaurant = new Restaurant(id, name, menuItems);
    restaurantRepository.save(restaurant);
  }

  public void reviseMenu(long id, List<MenuItem> menuItems) {
    restaurantRepository.findById(id).map(restaurant -> {
      List<OrderDomainEvent> events = restaurant.reviseMenu(menuItems);
      return restaurant;
    }).orElseThrow(RuntimeException::new);
  }

}


public class OrderMinimumNotMetException extends RuntimeException {
}


public class InvalidMenuItemIdException extends RuntimeException {
  public InvalidMenuItemIdException(String menuItemId) {
    super("Invalid menu item id " + menuItemId);
  }
}


import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long> {
}


import javax.persistence.Access;
import javax.persistence.AccessType;

@Access(AccessType.FIELD)
public class PaymentInformation {

  private String paymentToken;
}


import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;

import java.util.List;
import java.util.Optional;

public class OrderRevision {

  private Optional<DeliveryInformation> deliveryInformation = Optional.empty();
  private List<RevisedOrderLineItem> revisedOrderLineItems;

  private OrderRevision() {
  }

  public OrderRevision(Optional<DeliveryInformation> deliveryInformation, List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.deliveryInformation = deliveryInformation;
    this.revisedOrderLineItems = revisedOrderLineItems;
  }

  public void setDeliveryInformation(Optional<DeliveryInformation> deliveryInformation) {
    this.deliveryInformation = deliveryInformation;
  }

  public Optional<DeliveryInformation> getDeliveryInformation() {
    return deliveryInformation;
  }

  public List<RevisedOrderLineItem> getRevisedOrderLineItems() {
    return revisedOrderLineItems;
  }

  public void setRevisedOrderLineItems(List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.revisedOrderLineItems = revisedOrderLineItems;
  }
}



import net.chrisrichardson.ftgo.common.Address;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import java.time.LocalDateTime;

@Access(AccessType.FIELD)
public class DeliveryInformation {

  private LocalDateTime deliveryTime;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name="state", column=@Column(name="delivery_state"))
  })
  private Address deliveryAddress;

  public DeliveryInformation() {
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public DeliveryInformation(LocalDateTime deliveryTime, Address deliveryAddress) {
    this.deliveryTime = deliveryTime;
    this.deliveryAddress = deliveryAddress;
  }

  public LocalDateTime getDeliveryTime() {
    return deliveryTime;
  }

  public void setDeliveryTime(LocalDateTime deliveryTime) {
    this.deliveryTime = deliveryTime;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public void setDeliveryAddress(Address deliveryAddress) {
    this.deliveryAddress = deliveryAddress;
  }
}


import io.eventuate.tram.events.common.DomainEvent;

public class OrderLineItemChangeQueued implements DomainEvent {
  public OrderLineItemChangeQueued(String lineItemId, int newQuantity) {

  }
}


import io.eventuate.tram.events.common.DomainEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderState;

public class OrderCancelRequested implements DomainEvent {
  private OrderState state;

  public OrderCancelRequested(OrderState state) {

    this.state = state;
  }

  public OrderState getState() {
    return state;
  }
}


public class RestaurantNotFoundException extends RuntimeException {
  public RestaurantNotFoundException(long restaurantId) {
    super("Restaurant not found with id " + restaurantId);
  }
}


import org.springframework.data.repository.CrudRepository;

public interface RestaurantRepository extends CrudRepository<Restaurant, Long> {
}


public class OrderRejectedCancelRequested {
}


import io.eventuate.tram.events.common.DomainEvent;

public class OrderAuthorizedCancelRequested implements DomainEvent {
}


public class ApproveOrderCommand extends OrderCommand {

  private ApproveOrderCommand() {
  }

  public ApproveOrderCommand(long orderId) {
    super(orderId);
  }
}



import net.chrisrichardson.ftgo.common.Money;

public class BeginReviseOrderReply {
  private Money revisedOrderTotal;

  public BeginReviseOrderReply(Money revisedOrderTotal) {
    this.revisedOrderTotal = revisedOrderTotal;
  }

  public BeginReviseOrderReply() {
  }

  public Money getRevisedOrderTotal() {
    return revisedOrderTotal;
  }

  public void setRevisedOrderTotal(Money revisedOrderTotal) {
    this.revisedOrderTotal = revisedOrderTotal;
  }
}


import io.eventuate.tram.commands.common.Success;
import io.eventuate.tram.sagas.simpledsl.CommandEndpoint;
import io.eventuate.tram.sagas.simpledsl.CommandEndpointBuilder;
import net.chrisrichardson.ftgo.consumerservice.api.ConsumerServiceChannels;
import net.chrisrichardson.ftgo.consumerservice.api.ValidateOrderByConsumer;

public class ConsumerServiceProxy {


  public final CommandEndpoint<ValidateOrderByConsumer> validateOrder= CommandEndpointBuilder
          .forCommand(ValidateOrderByConsumer.class)
          .withChannel(ConsumerServiceChannels.consumerServiceChannel)
          .withReply(Success.class)
          .build();

}


public class UndoBeginReviseOrderCommand extends OrderCommand {

  protected UndoBeginReviseOrderCommand() {
  }

  public UndoBeginReviseOrderCommand(long orderId) {
    super(orderId);
  }
}


public class ConfirmCancelOrderCommand extends OrderCommand {

  private ConfirmCancelOrderCommand() {
  }

  public ConfirmCancelOrderCommand(long orderId) {
    super(orderId);
  }
}


import io.eventuate.tram.commands.common.Command;

public abstract class OrderCommand implements Command {

  private long orderId;

  protected OrderCommand() {
  }

  protected OrderCommand(long orderId) {
    this.orderId = orderId;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }
}


import net.chrisrichardson.ftgo.orderservice.domain.OrderRevision;

public class BeginReviseOrderCommand extends OrderCommand {

  private BeginReviseOrderCommand() {
  }

  public BeginReviseOrderCommand(long orderId, OrderRevision revision) {
    super(orderId);
    this.revision = revision;
  }

  private OrderRevision revision;

  public OrderRevision getRevision() {
    return revision;
  }

  public void setRevision(OrderRevision revision) {
    this.revision = revision;
  }
}


import io.eventuate.tram.commands.common.Success;
import io.eventuate.tram.sagas.simpledsl.CommandEndpoint;
import io.eventuate.tram.sagas.simpledsl.CommandEndpointBuilder;
import net.chrisrichardson.ftgo.orderservice.api.OrderServiceChannels;

public class OrderServiceProxy {

  public final CommandEndpoint<RejectOrderCommand> reject = CommandEndpointBuilder
          .forCommand(RejectOrderCommand.class)
          .withChannel(OrderServiceChannels.COMMAND_CHANNEL)
          .withReply(Success.class)
          .build();

  public final CommandEndpoint<ApproveOrderCommand> approve = CommandEndpointBuilder
          .forCommand(ApproveOrderCommand.class)
          .withChannel(OrderServiceChannels.COMMAND_CHANNEL)
          .withReply(Success.class)
          .build();

}

public class BeginCancelCommand extends OrderCommand {

  private BeginCancelCommand() {
  }

  public BeginCancelCommand(long orderId) {
    super(orderId);
  }
}


import io.eventuate.tram.commands.common.Success;
import io.eventuate.tram.sagas.simpledsl.CommandEndpoint;
import io.eventuate.tram.sagas.simpledsl.CommandEndpointBuilder;
import net.chrisrichardson.ftgo.kitchenservice.api.*;

public class KitchenServiceProxy {

  public final CommandEndpoint<CreateTicket> create = CommandEndpointBuilder
          .forCommand(CreateTicket.class)
          .withChannel(KitchenServiceChannels.COMMAND_CHANNEL)
          .withReply(CreateTicketReply.class)
          .build();

  public final CommandEndpoint<ConfirmCreateTicket> confirmCreate = CommandEndpointBuilder
          .forCommand(ConfirmCreateTicket.class)
          .withChannel(KitchenServiceChannels.COMMAND_CHANNEL)
          .withReply(Success.class)
          .build();
  public final CommandEndpoint<CancelCreateTicket> cancel = CommandEndpointBuilder
          .forCommand(CancelCreateTicket.class)
          .withChannel(KitchenServiceChannels.COMMAND_CHANNEL)
          .withReply(Success.class)
          .build();

}

import net.chrisrichardson.ftgo.orderservice.domain.OrderRevision;

public class ConfirmReviseOrderCommand extends OrderCommand {

  private ConfirmReviseOrderCommand() {
  }

  public ConfirmReviseOrderCommand(long orderId, OrderRevision revision) {
    super(orderId);
    this.revision = revision;
  }

  private OrderRevision revision;

  public OrderRevision getRevision() {
    return revision;
  }
}


public class UndoBeginCancelCommand extends OrderCommand {
  public UndoBeginCancelCommand(long orderId) {
    super(orderId);
  }
}


import io.eventuate.tram.commands.common.Success;
import io.eventuate.tram.sagas.simpledsl.CommandEndpoint;
import io.eventuate.tram.sagas.simpledsl.CommandEndpointBuilder;
import net.chrisrichardson.ftgo.accountservice.api.AccountingServiceChannels;
import net.chrisrichardson.ftgo.accountservice.api.AuthorizeCommand;

public class AccountingServiceProxy {

  public final CommandEndpoint<AuthorizeCommand> authorize= CommandEndpointBuilder
          .forCommand(AuthorizeCommand.class)
          .withChannel(AccountingServiceChannels.accountingServiceChannel)
          .withReply(Success.class)
          .build();

}



import io.eventuate.tram.commands.common.Command;

public class ReverseOrderUpdateCommand implements Command {
}


public class RejectOrderCommand extends OrderCommand {

  private RejectOrderCommand() {
  }

  public RejectOrderCommand(long orderId) {
    super(orderId);
  }
}


import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import io.microservices.canvas.extractor.spring.annotations.ServiceDescription;
import io.microservices.canvas.springmvc.MicroserviceCanvasWebConfiguration;
import net.chrisrichardson.eventstore.examples.customersandorders.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.ftgo.orderservice.grpc.GrpcConfiguration;
import net.chrisrichardson.ftgo.orderservice.messaging.OrderServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.orderservice.service.OrderCommandHandlersConfiguration;
import net.chrisrichardson.ftgo.orderservice.web.OrderWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({OrderWebConfiguration.class, OrderCommandHandlersConfiguration.class,  OrderServiceMessagingConfiguration.class,
        TramJdbcKafkaConfiguration.class, CommonSwaggerConfiguration.class, GrpcConfiguration.class,
        MicroserviceCanvasWebConfiguration.class})
@ServiceDescription(description="Manages Orders", capabilities = "Order Management")
public class OrderServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(OrderServiceMain.class, args);
  }
}


import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.tram.sagas.testing.SagaParticipantChannels;
import io.eventuate.tram.sagas.testing.SagaParticipantStubManager;
import io.eventuate.tram.sagas.spring.testing.SagaParticipantStubManagerConfiguration;
import io.eventuate.tram.testing.MessageTracker;
import io.restassured.response.Response;
import net.chrisrichardson.ftgo.accountservice.api.AuthorizeCommand;
import net.chrisrichardson.ftgo.common.CommonJsonMapperInitializer;
import net.chrisrichardson.ftgo.consumerservice.api.ValidateOrderByConsumer;
import net.chrisrichardson.ftgo.kitchenservice.api.CancelCreateTicket;
import net.chrisrichardson.ftgo.kitchenservice.api.ConfirmCreateTicket;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicket;
import net.chrisrichardson.ftgo.kitchenservice.api.CreateTicketReply;
import net.chrisrichardson.ftgo.orderservice.OrderDetailsMother;
import net.chrisrichardson.ftgo.orderservice.RestaurantMother;
import net.chrisrichardson.ftgo.orderservice.api.web.CreateOrderRequest;
import net.chrisrichardson.ftgo.orderservice.domain.Order;
import net.chrisrichardson.ftgo.orderservice.domain.RestaurantRepository;
import net.chrisrichardson.ftgo.testutil.FtgoTestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.Collections;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;
import static io.eventuate.util.test.async.Eventually.eventually;
import static io.restassured.RestAssured.given;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;


@SpringBootTest(classes = OrderServiceComponentTestStepDefinitions.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration
public class OrderServiceComponentTestStepDefinitions {



  private Response response;
  private long consumerId;

  static {
    CommonJsonMapperInitializer.registerMoneyModule();
  }

  private int port = 8082;
  private String host = FtgoTestUtil.getDockerHostIp();

  protected String baseUrl(String path) {
    return String.format("http://%s:%s%s", host, port, path);
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({TramJdbcKafkaConfiguration.class, SagaParticipantStubManagerConfiguration.class})
  @EnableJpaRepositories(basePackageClasses = RestaurantRepository.class) // Need to verify that the restaurant has been created. Replace with verifyRestaurantCreatedInOrderService
  @EntityScan(basePackageClasses = Order.class)
  public static class TestConfiguration {

    @Bean
    public SagaParticipantChannels sagaParticipantChannels() {
      return new SagaParticipantChannels("consumerService", "kitchenService", "accountingService", "orderService");
    }

    @Bean
    public MessageTracker messageTracker(MessageConsumer messageConsumer) {
      return new MessageTracker(singleton("net.chrisrichardson.ftgo.orderservice.domain.Order"), messageConsumer) ;
    }

  }

  @Autowired
  protected SagaParticipantStubManager sagaParticipantStubManager;

  @Autowired
  protected MessageTracker messageTracker;

  @Autowired
  protected DomainEventPublisher domainEventPublisher;

  @Autowired
  protected RestaurantRepository restaurantRepository;


  @Before
  public void setUp() {
    sagaParticipantStubManager.reset();
  }

  @Given("A valid consumer")
  public void useConsumer() {
    sagaParticipantStubManager.
            forChannel("consumerService")
            .when(ValidateOrderByConsumer.class).replyWith(cm -> withSuccess());
  }

  public enum CreditCardType { valid, expired}

  @Given("using a(.?) (.*) credit card")
  public void useCreditCard(String ignore, CreditCardType creditCard) {
    switch (creditCard) {
      case valid :
        sagaParticipantStubManager
                .forChannel("accountingService")
                .when(AuthorizeCommand.class).replyWithSuccess();
        break;
      case expired:
        sagaParticipantStubManager
                .forChannel("accountingService")
                .when(AuthorizeCommand.class).replyWithFailure();
        break;
      default:
        fail("Don't know what to do with this credit card");
    }
  }

  @Given("the restaurant is accepting orders")
  public void restaurantAcceptsOrder() {
    sagaParticipantStubManager
            .forChannel("kitchenService")
            .when(CreateTicket.class).replyWith(cm -> withSuccess(new CreateTicketReply(cm.getCommand().getOrderId())))
            .when(ConfirmCreateTicket.class).replyWithSuccess()
            .when(CancelCreateTicket.class).replyWithSuccess();

    if (!restaurantRepository.findById(RestaurantMother.AJANTA_ID).isPresent()) {
      domainEventPublisher.publish("net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant", RestaurantMother.AJANTA_ID,
              Collections.singletonList(RestaurantMother.makeAjantaRestaurantCreatedEvent()));

      eventually(() -> {
        FtgoTestUtil.assertPresent(restaurantRepository.findById(RestaurantMother.AJANTA_ID));
      });
    }
  }

  @When("I place an order for Chicken Vindaloo at Ajanta")
  public void placeOrder() {

    response = given().
            body(new CreateOrderRequest(consumerId,
                    RestaurantMother.AJANTA_ID, OrderDetailsMother.DELIVERY_ADDRESS, OrderDetailsMother.DELIVERY_TIME, Collections.singletonList(
                            new CreateOrderRequest.LineItem(RestaurantMother.CHICKEN_VINDALOO_MENU_ITEM_ID,
                                                            OrderDetailsMother.CHICKEN_VINDALOO_QUANTITY)))).
            contentType("application/json").
            when().
            post(baseUrl("/orders"));
  }

  @Then("the order should be (.*)")
  public void theOrderShouldBeInState(String desiredOrderState) {

      // TODO This doesn't make sense when the `OrderService` is live => duplicate replies

//    sagaParticipantStubManager
//            .forChannel("orderService")
//            .when(ApproveOrderCommand.class).replyWithSuccess();
//
    Integer orderId =
            this.response.
                    then().
                    statusCode(200).
                    extract().
                    path("orderId");

    assertNotNull(orderId);

    eventually(() -> {
      String state = given().
              when().
              get(baseUrl("/orders/" + orderId)).
              then().
              statusCode(200)
              .extract().
                      path("state");
      assertEquals(desiredOrderState, state);
    });

    sagaParticipantStubManager.verifyCommandReceived("kitchenService", CreateTicket.class);

  }

  @And("an (.*) event should be published")
  public void verifyEventPublished(String expectedEventClass) {
    messageTracker.assertDomainEventPublished("net.chrisrichardson.ftgo.orderservice.domain.Order",
            findEventClass(expectedEventClass, "net.chrisrichardson.ftgo.orderservice.domain", "net.chrisrichardson.ftgo.orderservice.api.events"));
  }

  private String findEventClass(String expectedEventClass, String... packages) {
    return Arrays.stream(packages).map(p -> p + "." + expectedEventClass).filter(className -> {
      try {
        Class.forName(className);
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      }
    }).findFirst().orElseThrow(() -> new RuntimeException(String.format("Cannot find class %s in packages %s", expectedEventClass, String.join(",", packages))));
  }

}


import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/component-test/resources/features")
public class OrderServiceComponentTest {
}

public class UnsupportedStateTransitionException extends RuntimeException {
  public UnsupportedStateTransitionException(Enum state) {
    super("current state: " + state);
  }
}


public class NotYetImplementedException extends RuntimeException {
}


import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.eventuate.common.json.mapper.JSonMapper;

import javax.annotation.PostConstruct;

public class CommonJsonMapperInitializer {

  @PostConstruct
  public void initialize() {
    registerMoneyModule();
  }

  public static void registerMoneyModule() {
    JSonMapper.objectMapper.registerModule(new MoneyModule());
    JSonMapper.objectMapper.registerModule(new JavaTimeModule());
    JSonMapper.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  }
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.math.BigDecimal;

//@Embeddable
//@Access(AccessType.FIELD)
public class Money {

  public static Money ZERO = new Money(0);

  private BigDecimal amount;

  private Money() {
  }

  public Money(BigDecimal amount) {
    this.amount = amount;
  }

  public Money(String s) {
    this.amount = new BigDecimal(s);
  }

  public Money(int i) {
    this.amount = new BigDecimal(i);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    Money money = (Money) o;

    return new EqualsBuilder()
            .append(amount, money.amount)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(amount)
            .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("amount", amount)
            .toString();
  }


  public Money add(Money delta) {
    return new Money(amount.add(delta.amount));
  }

  public boolean isGreaterThanOrEqual(Money other) {
    return amount.compareTo(other.amount) >= 0;
  }

  public String asString() {
    return amount.toPlainString();
  }

  public Money multiply(int x) {
    return new Money(amount.multiply(new BigDecimal(x)));
  }

  public Long asLong() {
    return multiply(100).amount.longValue();
  }
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Address {

  private String street1;
  private String street2;
  private String city;
  private String state;
  private String zip;

  private Address() {
  }

  public Address(String street1, String street2, String city, String state, String zip) {
    this.street1 = street1;
    this.street2 = street2;
    this.city = city;
    this.state = state;
    this.zip = zip;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
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

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }
}


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

public class MoneyModule extends SimpleModule {

  class MoneyDeserializer extends StdScalarDeserializer<Money> {

    protected MoneyDeserializer() {
      super(Money.class);
    }

    @Override
    public Money deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      JsonToken token = jp.getCurrentToken();
      if (token == JsonToken.VALUE_STRING) {
        String str = jp.getText().trim();
        if (str.isEmpty())
          return null;
        else
          return new Money(str);
      } else
        throw ctxt.mappingException(getValueClass());
    }
  }

  class MoneySerializer extends StdScalarSerializer<Money> {
    public MoneySerializer() {
      super(Money.class);
    }

    @Override
    public void serialize(Money value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeString(value.asString());
    }
  }

    @Override
  public String getModuleName() {
    return "FtgoCommonMOdule";
  }

  public MoneyModule() {
    addDeserializer(Money.class, new MoneyDeserializer());
    addSerializer(Money.class, new MoneySerializer());
  }

}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration {

  @Bean
  public CommonJsonMapperInitializer commonJsonMapperInitializer() {
    return new CommonJsonMapperInitializer();

  }
}


public class PersonName {
  private String firstName;
  private String lastName;

  private PersonName() {
  }

  public PersonName(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }
}

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Objects;

public class RevisedOrderLineItem {
    private int quantity;
    private String menuItemId;

    public RevisedOrderLineItem() {
    }

    public RevisedOrderLineItem(int quantity, String menuItemId) {
        this.quantity = quantity;
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(String menuItemId) {
        this.menuItemId = menuItemId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, menuItemId);
    }
}



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}



import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "consumer.destinations")
public class ConsumerDestinations {

  @NotNull
  private String consumerServiceUrl;

  public String getConsumerServiceUrl() {
    return consumerServiceUrl;
  }

  public void setConsumerServiceUrl(String consumerServiceUrl) {
    this.consumerServiceUrl = consumerServiceUrl;
  }
}


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(ConsumerDestinations.class)
public class ConsumerConfiguration {

  @Bean
  public RouteLocator consumerProxyRouting(RouteLocatorBuilder builder, ConsumerDestinations consumerDestinations) {
    return builder.routes()
            .route(r -> r.path("/consumers").and().method("POST").uri(consumerDestinations.getConsumerServiceUrl()))
            .route(r -> r.path("/consumers").and().method("PUT").uri(consumerDestinations.getConsumerServiceUrl()))
            .build();
  }

}


public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException() {
  }
}


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class OrderInfo {

  private String orderId;
  private String state;

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public OrderInfo(String orderId, String state) {
    this.orderId = orderId;
    this.state = state;

  }

  private OrderInfo() {
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

}


public class BillInfo {
}


public class DeliveryInfo {
}


import net.chrisrichardson.ftgo.apiagateway.orders.OrderDestinations;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OrderServiceProxy {


  private OrderDestinations orderDestinations;

  private WebClient client;

  public OrderServiceProxy(OrderDestinations orderDestinations, WebClient client) {
    this.orderDestinations = orderDestinations;
    this.client = client;
  }

  public Mono<OrderInfo> findOrderById(String orderId) {
    Mono<ClientResponse> response = client
            .get()
            .uri(orderDestinations.getOrderServiceUrl() + "/orders/{orderId}", orderId)
            .exchange();
    return response.flatMap(resp -> {
      switch (resp.statusCode()) {
        case OK:
          return resp.bodyToMono(OrderInfo.class);
        case NOT_FOUND:
          return Mono.error(new OrderNotFoundException());
        default:
          return Mono.error(new RuntimeException("Unknown" + resp.statusCode()));
      }
    });
  }


}


import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class DeliveryService {
  public Mono<DeliveryInfo> findDeliveryByOrderId(String orderId) {
    return Mono.error(new UnsupportedOperationException());
  }
}


import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class AccountingService {
  public Mono<BillInfo> findBillByOrderId(String orderId) {
    return Mono.error(new UnsupportedOperationException());
  }
}


public class TicketInfo {
}


import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class KitchenService {
  public Mono<TicketInfo> findTicketById(String ticketId) {
    return Mono.error(new UnsupportedOperationException());
  }
}


import net.chrisrichardson.ftgo.apiagateway.proxies.AccountingService;
import net.chrisrichardson.ftgo.apiagateway.proxies.DeliveryService;
import net.chrisrichardson.ftgo.apiagateway.proxies.OrderServiceProxy;
import net.chrisrichardson.ftgo.apiagateway.proxies.KitchenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
@EnableConfigurationProperties(OrderDestinations.class)
public class OrderConfiguration {

  @Bean
  public RouteLocator orderProxyRouting(RouteLocatorBuilder builder, OrderDestinations orderDestinations) {
    return builder.routes()
            .route(r -> r.path("/orders").and().method("POST").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/orders").and().method("PUT").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/orders/**").and().method("POST").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/orders/**").and().method("PUT").uri(orderDestinations.getOrderServiceUrl()))
            .route(r -> r.path("/orders").and().method("GET").uri(orderDestinations.getOrderHistoryServiceUrl()))
            .build();
  }

  @Bean
  public RouterFunction<ServerResponse> orderHandlerRouting(OrderHandlers orderHandlers) {
    return RouterFunctions.route(GET("/orders/{orderId}"), orderHandlers::getOrderDetails);
  }

  @Bean
  public OrderHandlers orderHandlers(OrderServiceProxy orderService, KitchenService kitchenService,
                                     DeliveryService deliveryService, AccountingService accountingService) {
    return new OrderHandlers(orderService, kitchenService, deliveryService, accountingService);
  }

  @Bean
  public WebClient webClient() {
    return WebClient.create();
  }

}


import net.chrisrichardson.ftgo.apiagateway.proxies.BillInfo;
import net.chrisrichardson.ftgo.apiagateway.proxies.DeliveryInfo;
import net.chrisrichardson.ftgo.apiagateway.proxies.OrderInfo;
import net.chrisrichardson.ftgo.apiagateway.proxies.TicketInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import reactor.util.function.Tuple4;

import java.util.Optional;

public class OrderDetails {

  private OrderInfo orderInfo;

  public OrderDetails() {
  }

  public OrderDetails(OrderInfo orderInfo) {
    this.orderInfo = orderInfo;
  }

  public OrderDetails(OrderInfo orderInfo,
                      Optional<TicketInfo> ticketInfo,
                      Optional<DeliveryInfo> deliveryInfo,
                      Optional<BillInfo> billInfo) {
    this(orderInfo);
    System.out.println("FIXME");
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }


  public OrderInfo getOrderInfo() {
    return orderInfo;
  }

  public void setOrderInfo(OrderInfo orderInfo) {
    this.orderInfo = orderInfo;
  }


  public static OrderDetails makeOrderDetails(Tuple4<OrderInfo, Optional<TicketInfo>, Optional<DeliveryInfo>, Optional<BillInfo>> info) {
    return new OrderDetails(info.getT1(), info.getT2(), info.getT3(), info.getT4());
  }
}


import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "order.destinations")
public class OrderDestinations {

  @NotNull
  private String orderServiceUrl;

  @NotNull
  private String orderHistoryServiceUrl;

  public String getOrderHistoryServiceUrl() {
    return orderHistoryServiceUrl;
  }

  public void setOrderHistoryServiceUrl(String orderHistoryServiceUrl) {
    this.orderHistoryServiceUrl = orderHistoryServiceUrl;
  }


  public String getOrderServiceUrl() {
    return orderServiceUrl;
  }

  public void setOrderServiceUrl(String orderServiceUrl) {
    this.orderServiceUrl = orderServiceUrl;
  }
}


import net.chrisrichardson.ftgo.apiagateway.proxies.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.util.Optional;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;

public class OrderHandlers {

  private OrderServiceProxy orderService;
  private KitchenService kitchenService;
  private DeliveryService deliveryService;
  private AccountingService accountingService;

  public OrderHandlers(OrderServiceProxy orderService,
                       KitchenService kitchenService,
                       DeliveryService deliveryService,
                       AccountingService accountingService) {
    this.orderService = orderService;
    this.kitchenService = kitchenService;
    this.deliveryService = deliveryService;
    this.accountingService = accountingService;
  }

  public Mono<ServerResponse> getOrderDetails(ServerRequest serverRequest) {
    String orderId = serverRequest.pathVariable("orderId");

    Mono<OrderInfo> orderInfo = orderService.findOrderById(orderId);

    Mono<Optional<TicketInfo>> ticketInfo = kitchenService
            .findTicketById(orderId)
            .map(Optional::of)
            .onErrorReturn(Optional.empty());

    Mono<Optional<DeliveryInfo>> deliveryInfo = deliveryService
            .findDeliveryByOrderId(orderId)
            .map(Optional::of)
            .onErrorReturn(Optional.empty());

    Mono<Optional<BillInfo>> billInfo = accountingService
            .findBillByOrderId(orderId)
            .map(Optional::of)
            .onErrorReturn(Optional.empty());

    Mono<Tuple4<OrderInfo, Optional<TicketInfo>, Optional<DeliveryInfo>, Optional<BillInfo>>> combined =
            Mono.zip(orderInfo, ticketInfo, deliveryInfo, billInfo);

    Mono<OrderDetails> orderDetails = combined.map(OrderDetails::makeOrderDetails);

    return orderDetails.flatMap(od -> ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(fromObject(od)))
            .onErrorResume(OrderNotFoundException.class, e -> ServerResponse.notFound().build());
  }


}


public class ConsumerServiceChannels {
  public static final String consumerServiceChannel = "consumerService";
}


import net.chrisrichardson.ftgo.consumerservice.domain.ConsumerServiceConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(ConsumerServiceConfiguration.class)
public class ConsumerWebConfiguration {
}


import io.eventuate.tram.events.publisher.ResultWithEvents;
import net.chrisrichardson.ftgo.consumerservice.domain.Consumer;
import net.chrisrichardson.ftgo.consumerservice.domain.ConsumerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path="/consumers")
public class ConsumerController {

  private ConsumerService consumerService;

  public ConsumerController(ConsumerService consumerService) {
    this.consumerService = consumerService;
  }

  @RequestMapping(method= RequestMethod.POST)
  public CreateConsumerResponse create(@RequestBody CreateConsumerRequest request) {
    ResultWithEvents<Consumer> result = consumerService.create(request.getName());
    return new CreateConsumerResponse(result.result.getId());
  }

  @RequestMapping(method= RequestMethod.GET,  path="/{consumerId}")
  public ResponseEntity<GetConsumerResponse> get(@PathVariable long consumerId) {
    return consumerService.findById(consumerId)
            .map(consumer -> new ResponseEntity<>(new GetConsumerResponse(consumer.getName()), HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }
}


import net.chrisrichardson.ftgo.common.PersonName;

public class CreateConsumerRequest {
  private PersonName name;

  public PersonName getName() {
    return name;
  }

  public void setName(PersonName name) {
    this.name = name;
  }

  public CreateConsumerRequest(PersonName name) {

    this.name = name;
  }

  private CreateConsumerRequest() {
  }


}


import net.chrisrichardson.ftgo.common.PersonName;

public class GetConsumerResponse extends CreateConsumerResponse {
  private PersonName name;

  public PersonName getName() {
    return name;
  }

  public GetConsumerResponse(PersonName name) {

    this.name = name;
  }
}


public class CreateConsumerResponse {
  private long consumerId;

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public CreateConsumerResponse() {

  }

  public CreateConsumerResponse(long consumerId) {
    this.consumerId = consumerId;
  }
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ValidateOrderByConsumer implements Command {

  private long consumerId;
  private long orderId;
  private Money orderTotal;

  private ValidateOrderByConsumer() {
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public ValidateOrderByConsumer(long consumerId, long orderId, Money orderTotal) {
    this.consumerId = consumerId;
    this.orderId = orderId;
    this.orderTotal = orderTotal;
  }

  public long getOrderId() {
    return orderId;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public void setConsumerId(long consumerId) {
    this.consumerId = consumerId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  public Money getOrderTotal() {
    return orderTotal;
  }

  public void setOrderTotal(Money orderTotal) {
    this.orderTotal = orderTotal;
  }
}


public class ConsumerVerificationFailedException extends RuntimeException {
}


public class ConsumerNotFoundException extends ConsumerVerificationFailedException {
}


import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.common.PersonName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public class ConsumerService {

  @Autowired
  private ConsumerRepository consumerRepository;

  @Autowired
  private DomainEventPublisher domainEventPublisher;

  public void validateOrderForConsumer(long consumerId, Money orderTotal) {
    Optional<Consumer> consumer = consumerRepository.findById(consumerId);
    consumer.orElseThrow(ConsumerNotFoundException::new).validateOrderByConsumer(orderTotal);
  }

  @Transactional
  public ResultWithEvents<Consumer> create(PersonName name) {
    ResultWithEvents<Consumer> rwe = Consumer.create(name);
    consumerRepository.save(rwe.result);
    domainEventPublisher.publish(Consumer.class, rwe.result.getId(), rwe.events);
    return rwe;
  }

  public Optional<Consumer> findById(long consumerId) {
    return consumerRepository.findById(consumerId);
  }
}


import io.eventuate.tram.commands.consumer.CommandDispatcher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcherFactory;
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories
@EnableAutoConfiguration
@Import({SagaParticipantConfiguration.class, TramEventsPublisherConfiguration.class, CommonConfiguration.class, SagaParticipantConfiguration.class})
@EnableTransactionManagement
@ComponentScan
public class ConsumerServiceConfiguration {

  @Bean
  public ConsumerServiceCommandHandlers consumerServiceCommandHandlers() {
    return new ConsumerServiceCommandHandlers();
  }

  @Bean
  public ConsumerService consumerService() {
    return new ConsumerService();
  }

  @Bean
  public CommandDispatcher commandDispatcher(ConsumerServiceCommandHandlers consumerServiceCommandHandlers, SagaCommandDispatcherFactory sagaCommandDispatcherFactory) {
    return sagaCommandDispatcherFactory.make("consumerServiceDispatcher", consumerServiceCommandHandlers.commandHandlers());
  }


}


import io.eventuate.tram.events.common.DomainEvent;

public class ConsumerCreated implements DomainEvent {
}


import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;
import net.chrisrichardson.ftgo.consumerservice.api.ValidateOrderByConsumer;
import org.springframework.beans.factory.annotation.Autowired;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class ConsumerServiceCommandHandlers  {

  @Autowired
  private ConsumerService consumerService;

  public CommandHandlers commandHandlers() {
    return SagaCommandHandlersBuilder
              .fromChannel("consumerService")
              .onMessage(ValidateOrderByConsumer.class, this::validateOrderForConsumer)
              .build();
  }

  private Message validateOrderForConsumer(CommandMessage<ValidateOrderByConsumer> cm) {
    try {
      consumerService.validateOrderForConsumer(cm.getCommand().getConsumerId(), cm.getCommand().getOrderTotal());
      return withSuccess();
    } catch (ConsumerVerificationFailedException e) {
      return withFailure();
    }
  }
}


import io.eventuate.tram.events.publisher.ResultWithEvents;
import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.common.PersonName;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "consumers")
@Access(AccessType.FIELD)
public class Consumer {

  @Id
  @GeneratedValue
  private Long id;

  @Embedded
  private PersonName name;

  private Consumer() {
  }

  public Consumer(PersonName name) {
    this.name = name;
  }


  public void validateOrderByConsumer(Money orderTotal) {
    // implement some business logic
  }

  public Long getId() {
    return id;
  }

  public PersonName getName() {
    return name;
  }

  public static ResultWithEvents<Consumer> create(PersonName name) {
    return new ResultWithEvents<>(new Consumer(name), new ConsumerCreated());
  }
}


import org.springframework.data.repository.CrudRepository;

public interface ConsumerRepository extends CrudRepository<Consumer, Long> {
}


import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import net.chrisrichardson.eventstore.examples.customersandorders.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.ftgo.consumerservice.web.ConsumerWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ConsumerWebConfiguration.class, TramJdbcKafkaConfiguration.class, CommonSwaggerConfiguration.class})
public class ConsumerServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(ConsumerServiceMain.class, args);
  }
}


public enum DeliveryActionType { PICKUP, DROPOFF
}


public class CourierAvailability {

  private boolean available;

  public CourierAvailability() {
  }

  public CourierAvailability(boolean available) {
    this.available = available;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }
}


public class ActionInfo {
  private DeliveryActionType type;

  public ActionInfo() {
  }

  public ActionInfo(DeliveryActionType type) {
    this.type = type;
  }

  public DeliveryActionType getType() {
    return type;
  }

  public void setType(DeliveryActionType type) {
    this.type = type;
  }
}


public enum DeliveryState {
  CANCELLED, SCHEDULED, PENDING
}


public class DeliveryInfo {

  private long id;
  private DeliveryState state;

  public DeliveryInfo() {
  }

  public DeliveryInfo(long id, DeliveryState state) {

    this.id = id;
    this.state = state;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public DeliveryState getState() {
    return state;
  }

  public void setState(DeliveryState state) {
    this.state = state;
  }
}


import java.util.List;

public class DeliveryStatus {
  private DeliveryInfo deliveryInfo;
  private Long assignedCourier;
  private List<ActionInfo> courierActions;

  public DeliveryStatus() {
  }

  public DeliveryInfo getDeliveryInfo() {
    return deliveryInfo;
  }

  public void setDeliveryInfo(DeliveryInfo deliveryInfo) {
    this.deliveryInfo = deliveryInfo;
  }

  public Long getAssignedCourier() {
    return assignedCourier;
  }

  public void setAssignedCourier(Long assignedCourier) {
    this.assignedCourier = assignedCourier;
  }

  public List<ActionInfo> getCourierActions() {
    return courierActions;
  }

  public void setCourierActions(List<ActionInfo> courierActions) {
    this.courierActions = courierActions;
  }

  public DeliveryStatus(DeliveryInfo deliveryInfo, Long assignedCourier, List<ActionInfo> courierActions) {
    this.deliveryInfo = deliveryInfo;
    this.assignedCourier = assignedCourier;
    this.courierActions = courierActions;
  }
}


import io.eventuate.tram.spring.consumer.jdbc.TramConsumerJdbcAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CourierJpaTest.Config.class)
public class CourierJpaTest {

  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration(exclude = TramConsumerJdbcAutoConfiguration.class)
  public static class Config {
  }

  @Autowired
  private CourierRepository courierRepository;


  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  public void shouldSaveAndLoad() {
    long courierId = System.currentTimeMillis();
    Courier courier = Courier.create(courierId);
    long deliveryId = 103L;
    courier.addAction(Action.makePickup(deliveryId, DeliveryServiceTestData.PICKUP_ADDRESS, LocalDateTime.now()));

    Courier savedCourier = courierRepository.save(courier);

    transactionTemplate.execute((ts) -> {
      Courier loadedCourier = courierRepository.findById(courierId).get();
      assertEquals(1, loadedCourier.getPlan().getActions().size());
      return null;
    });
  }

  @Test
  public void shouldFindAllAvailable() {
    long courierId1 = System.currentTimeMillis() * 10;
    long courierId2 = System.currentTimeMillis() * 10 + 1;
    Courier courier1 = Courier.create(courierId1);
    Courier courier2 = Courier.create(courierId2);

    courier1.noteAvailable();
    courier2.noteUnavailable();

    courierRepository.save(courier1);
    courierRepository.save(courier2);

    List<Courier> availableCouriers = courierRepository.findAllAvailable();

    assertTrue(availableCouriers.stream().anyMatch(c -> c.getId() == courierId1));
    assertFalse(availableCouriers.stream().anyMatch(c -> c.getId() == courierId2));
  }

  @Test
  public void shouldFindOrCreate() {
    long courierId = System.currentTimeMillis();
    transactionTemplate.execute((ts) -> {
      Courier courier = courierRepository.findOrCreateCourier(courierId);
      assertNotNull(courier);
      return null;
    });
    transactionTemplate.execute((ts) -> {
      Courier courier2 = courierRepository.findOrCreateCourier(courierId);
      assertNotNull(courier2);
      return null;
    });
  }

}


import io.eventuate.tram.spring.consumer.jdbc.TramConsumerJdbcAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestaurantJpaTest.Config.class)
public class RestaurantJpaTest {

  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration(exclude = TramConsumerJdbcAutoConfiguration.class)
  public static class Config {
  }


  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  public void shouldSaveAndLoad() {
    long restaurantId = System.currentTimeMillis();
    Restaurant restaurant = Restaurant.create(restaurantId, "Delicious Indian", DeliveryServiceTestData.PICKUP_ADDRESS);
    restaurantRepository.save(restaurant);

    transactionTemplate.execute((ts) -> {
      Restaurant loadedCourier = restaurantRepository.findById(restaurantId).get();
      assertEquals(DeliveryServiceTestData.PICKUP_ADDRESS, loadedCourier.getAddress());
      return null;
    });

  }
}


import io.eventuate.tram.spring.consumer.jdbc.TramConsumerJdbcAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeliveryJpaTest.Config.class)
public class DeliveryJpaTest {

  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration(exclude = TramConsumerJdbcAutoConfiguration.class)
  public static class Config {
  }

  @Autowired
  private DeliveryRepository deliveryRepository;

  @Test
  public void shouldSaveAndLoadDelivery() {
    long restaurantId = 102L;
    long orderId = System.currentTimeMillis();
    Delivery delivery = Delivery.create(orderId,
            restaurantId, DeliveryServiceTestData.PICKUP_ADDRESS, DeliveryServiceTestData.PICKUP_ADDRESS );
    Delivery savedDelivery = deliveryRepository.save(delivery);

    Delivery loadedDelivery = deliveryRepository.findById(orderId).get();
    assertNull(loadedDelivery.getAssignedCourier());
  }

}


import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryService;
import net.chrisrichardson.ftgo.kitchenservice.api.KitchenServiceChannels;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketAcceptedEvent;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketCancelled;
import net.chrisrichardson.ftgo.orderservice.api.OrderServiceChannels;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderCreatedEvent;
import net.chrisrichardson.ftgo.restaurantservice.RestaurantServiceChannels;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;

import java.time.LocalDateTime;

public class DeliveryMessageHandlers {

  private DeliveryService deliveryService;

  public DeliveryMessageHandlers(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  public DomainEventHandlers domainEventHandlers() {
    return DomainEventHandlersBuilder
            .forAggregateType(KitchenServiceChannels.TICKET_EVENT_CHANNEL)
            .onEvent(TicketAcceptedEvent.class, this::handleTicketAcceptedEvent)
            .onEvent(TicketCancelled.class, this::handleTicketCancelledEvent)
            .andForAggregateType(OrderServiceChannels.ORDER_EVENT_CHANNEL)
            .onEvent(OrderCreatedEvent.class, this::handleOrderCreatedEvent)
            .andForAggregateType(RestaurantServiceChannels.RESTAURANT_EVENT_CHANNEL)
            .onEvent(RestaurantCreated.class, this::handleRestaurantCreated)
            .build();
  }

  public void handleRestaurantCreated(DomainEventEnvelope<RestaurantCreated> dee) {
    Address address = RestaurantEventMapper.toAddress(dee.getEvent().getAddress());
    deliveryService.createRestaurant(Long.parseLong(dee.getAggregateId()), dee.getEvent().getName(), address);
  }

  public void handleOrderCreatedEvent(DomainEventEnvelope<OrderCreatedEvent> dee) {
    Address address = dee.getEvent().getDeliveryAddress();
    deliveryService.createDelivery(Long.parseLong(dee.getAggregateId()),
            dee.getEvent().getOrderDetails().getRestaurantId(), address);
  }

  public void handleTicketAcceptedEvent(DomainEventEnvelope<TicketAcceptedEvent> dee) {
    LocalDateTime readyBy = dee.getEvent().getReadyBy();
    deliveryService.scheduleDelivery(Long.parseLong(dee.getAggregateId()), readyBy);
  }

  public void handleTicketCancelledEvent(DomainEventEnvelope<TicketCancelled> dee) {
    deliveryService.cancelDelivery(Long.parseLong(dee.getAggregateId()));
  }


}


import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryService;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryServiceDomainConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DeliveryServiceDomainConfiguration.class, TramEventSubscriberConfiguration.class, CommonConfiguration.class})
public class DeliveryServiceMessagingConfiguration {

  @Bean
  public DeliveryMessageHandlers deliveryMessageHandlers(DeliveryService deliveryService) {
    return new DeliveryMessageHandlers(deliveryService);
  }

  @Bean
  public DomainEventDispatcher domainEventDispatcher(DeliveryMessageHandlers deliveryMessageHandlers, DomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("deliveryService-deliveryMessageHandlers", deliveryMessageHandlers.domainEventHandlers());
  }
}


import net.chrisrichardson.ftgo.common.Address;

public class RestaurantEventMapper {

  public static Address toAddress(net.chrisrichardson.ftgo.restaurantservice.events.Address address) {
    return new Address(address.getStreet1(), address.getStreet2(), address.getCity(), address.getState(), address.getZip());
  }

  public static net.chrisrichardson.ftgo.restaurantservice.events.Address fromAddress(net.chrisrichardson.ftgo.common.Address a) {
    return new net.chrisrichardson.ftgo.restaurantservice.events.Address().withStreet1(a.getStreet1()).withStreet2(a.getStreet2()).withCity(a.getCity()).withZip(a.getZip());
  }

}


import net.chrisrichardson.ftgo.deliveryservice.api.web.CourierAvailability;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryService;
import net.chrisrichardson.ftgo.deliveryservice.api.web.DeliveryStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DeliveryServiceController {

  private DeliveryService deliveryService;

  public DeliveryServiceController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  @RequestMapping(path="/couriers/{courierId}/availability", method= RequestMethod.POST)
  public void updateCourierLocation(@PathVariable long courierId, @RequestBody CourierAvailability availability) {
    deliveryService.updateAvailability(courierId, availability.isAvailable());
  }

  @RequestMapping(path="/deliveries/{deliveryId}", method= RequestMethod.GET)
  public ResponseEntity<DeliveryStatus> getDeliveryStatus(@PathVariable long deliveryId) {
    return deliveryService.getDeliveryInfo(deliveryId).map(ds -> new ResponseEntity<>(ds, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }


}


import net.chrisrichardson.ftgo.common.CommonConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryServiceDomainConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import({DeliveryServiceDomainConfiguration.class, CommonConfiguration.class})
public class DeliveryServiceWebConfiguration {
}


import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomCourierRepository {

  Courier findOrCreateCourier(long courierId);

}


import javax.persistence.ElementCollection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Plan {

  @ElementCollection
  private List<Action> actions = new LinkedList<>();

  public void add(Action action) {
    actions.add(action);
  }

  public void removeDelivery(long deliveryId) {
    actions = actions.stream().filter(action -> !action.actionFor(deliveryId)).collect(Collectors.toList());
  }

  public List<Action> getActions() {
    return actions;
  }

  public List<Action> actionsForDelivery(long deliveryId) {
    return actions.stream().filter(action -> action.actionFor(deliveryId)).collect(Collectors.toList());
  }
}


import net.chrisrichardson.ftgo.common.Address;

import javax.persistence.*;

@Entity
@Access(AccessType.FIELD)
public class Restaurant {

  @Id
  private Long id;

  private String restaurantName;
  private Address address;

  private Restaurant() {
  }

  public Restaurant(long restaurantId, String restaurantName, Address address) {
    this.id = restaurantId;
    this.restaurantName = restaurantName;
    this.address = address;
  }

  public static Restaurant create(long restaurantId, String restaurantName, Address address) {
    return new Restaurant(restaurantId, restaurantName, address);
  }

  public Address getAddress() {
    return address;
  }
}



import net.chrisrichardson.ftgo.deliveryservice.api.web.DeliveryActionType;
import net.chrisrichardson.ftgo.common.Address;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDateTime;

@Embeddable
public class Action {

  @Enumerated(EnumType.STRING)
  private DeliveryActionType type;
  private Address address;
  private LocalDateTime time;

  protected long deliveryId;

  private Action() {
  }

  public Action(DeliveryActionType type, long deliveryId, Address address, LocalDateTime time) {
    this.type = type;
    this.deliveryId = deliveryId;
    this.address = address;
    this.time = time;
  }

  public boolean actionFor(long deliveryId) {
    return this.deliveryId == deliveryId;
  }

  public static Action makePickup(long deliveryId, Address pickupAddress, LocalDateTime pickupTime) {
    return new Action(DeliveryActionType.PICKUP, deliveryId, pickupAddress, pickupTime);
  }

  public static Action makeDropoff(long deliveryId, Address deliveryAddress, LocalDateTime deliveryTime) {
    return new Action(DeliveryActionType.DROPOFF, deliveryId, deliveryAddress, deliveryTime);
  }


  public DeliveryActionType getType() {
    return type;
  }

  public Address getAddress() {
    return address;
  }
}


import javax.persistence.*;
import java.util.List;

@Entity
@Access(AccessType.FIELD)
public class Courier {

  @Id
  private long id;

  @Embedded
  private Plan plan;

  private Boolean available;

  private Courier() {
  }

  public Courier(long courierId) {
    this.id = courierId;
    this.plan = new Plan();
  }

  public static Courier create(long courierId) {
    return new Courier(courierId);
  }

  public void noteAvailable() {
    this.available = true;

  }

  public void addAction(Action action) {
    plan.add(action);
  }

  public void cancelDelivery(long deliveryId) {
    plan.removeDelivery(deliveryId);
  }

  public boolean isAvailable() {
    return available;
  }

  public Plan getPlan() {
    return plan;
  }

  public long getId() {
    return id;
  }

  public void noteUnavailable() {
    this.available = false;
  }

  public List<Action> actionsForDelivery(long deliveryId) {
    return plan.actionsForDelivery(deliveryId);
  }
}


import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.deliveryservice.api.web.DeliveryState;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Access(AccessType.FIELD)
public class Delivery {

  @Id
  private Long id;

  @Embedded
  @AttributeOverrides({
          @AttributeOverride(name="street1", column = @Column(name="pickup_street1")),
          @AttributeOverride(name="street2", column = @Column(name="pickup_street2")),
          @AttributeOverride(name="city", column = @Column(name="pickup_city")),
          @AttributeOverride(name="state", column = @Column(name="pickup_state")),
          @AttributeOverride(name="zip", column = @Column(name="pickup_zip")),
  }
  )
  private Address pickupAddress;

  @Enumerated(EnumType.STRING)
  private DeliveryState state;

  private long restaurantId;
  private LocalDateTime pickUpTime;

  @Embedded
  @AttributeOverrides({
          @AttributeOverride(name="street1", column = @Column(name="delivery_street1")),
          @AttributeOverride(name="street2", column = @Column(name="delivery_street2")),
          @AttributeOverride(name="city", column = @Column(name="delivery_city")),
          @AttributeOverride(name="state", column = @Column(name="delivery_state")),
          @AttributeOverride(name="zip", column = @Column(name="delivery_zip")),
  }
  )

  private Address deliveryAddress;
  private LocalDateTime deliveryTime;

  private Long assignedCourier;
  private LocalDateTime readyBy;

  private Delivery() {
  }

  public Delivery(long orderId, long restaurantId, Address pickupAddress, Address deliveryAddress) {
    this.id = orderId;
    this.pickupAddress = pickupAddress;
    this.state = DeliveryState.PENDING;
    this.restaurantId = restaurantId;
    this.deliveryAddress = deliveryAddress;
  }

  public static Delivery create(long orderId, long restaurantId, Address pickupAddress, Address deliveryAddress) {
    return new Delivery(orderId, restaurantId, pickupAddress, deliveryAddress);
  }

  public void schedule(LocalDateTime readyBy, long assignedCourier) {
    this.readyBy = readyBy;
    this.assignedCourier = assignedCourier;
    this.state = DeliveryState.SCHEDULED;

  }

  public void cancel() {
    this.state = DeliveryState.CANCELLED;
    this.assignedCourier = null;
  }


  public long getId() {
    return id;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public Address getDeliveryAddress() {
    return deliveryAddress;
  }

  public Address getPickupAddress() {
    return pickupAddress;
  }

  public DeliveryState getState() {
    return state;
  }

  public Long getAssignedCourier() {
    return assignedCourier;
  }
}


import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EntityScan
@EnableJpaRepositories
@EnableTransactionManagement
public class DeliveryServiceDomainConfiguration {

  @Bean
  public DeliveryService deliveryService(RestaurantRepository restaurantRepository, DeliveryRepository deliveryRepository, CourierRepository courierRepository) {
    return new DeliveryService(restaurantRepository, deliveryRepository, courierRepository);
  }
}


import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.util.List;

public class CustomCourierRepositoryImpl implements CustomCourierRepository {

  @Autowired
  private EntityManager entityManager;

//  @Override
//  public List<Courier> findAllAvailable() {
//    return entityManager.createQuery("").getResultList();
//  }

  @Override
  public Courier findOrCreateCourier(long courierId) {
    Courier courier = entityManager.find(Courier.class, courierId);
    if (courier == null) {
      courier = Courier.create(courierId);
      entityManager.persist(courier);
    }
    return courier;
  }
}


import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.deliveryservice.api.web.ActionInfo;
import net.chrisrichardson.ftgo.deliveryservice.api.web.DeliveryInfo;
import net.chrisrichardson.ftgo.deliveryservice.api.web.DeliveryStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class DeliveryService {

  private RestaurantRepository restaurantRepository;
  private DeliveryRepository deliveryRepository;
  private CourierRepository courierRepository;
  private Random random = new Random();

  public DeliveryService(RestaurantRepository restaurantRepository, DeliveryRepository deliveryRepository, CourierRepository courierRepository) {
    this.restaurantRepository = restaurantRepository;
    this.deliveryRepository = deliveryRepository;
    this.courierRepository = courierRepository;
  }

  public void createRestaurant(long restaurantId, String restaurantName, Address address) {
    restaurantRepository.save(Restaurant.create(restaurantId, restaurantName, address));
  }

  public void createDelivery(long orderId, long restaurantId, Address deliveryAddress) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId).get();
    deliveryRepository.save(Delivery.create(orderId, restaurantId, restaurant.getAddress(), deliveryAddress));
  }

  public void scheduleDelivery(long orderId, LocalDateTime readyBy) {
    Delivery delivery = deliveryRepository.findById(orderId).get();

    // Stupid implementation

    List<Courier> couriers = courierRepository.findAllAvailable();
    Courier courier = couriers.get(random.nextInt(couriers.size()));
    courier.addAction(Action.makePickup(delivery.getId(), delivery.getPickupAddress(), readyBy));
    courier.addAction(Action.makeDropoff(delivery.getId(), delivery.getDeliveryAddress(), readyBy.plusMinutes(30)));

    delivery.schedule(readyBy, courier.getId());

  }

  public void cancelDelivery(long orderId) {
    Delivery delivery = deliveryRepository.findById(orderId).get();
    Long assignedCourierId = delivery.getAssignedCourier();
    delivery.cancel();
    if (assignedCourierId != null) {
      Courier courier = courierRepository.findById(assignedCourierId).get();
      courier.cancelDelivery(delivery.getId());
    }

  }



  // notePickedUp
  // noteDelivered
  // noteLocation

  void noteAvailable(long courierId) {
    courierRepository.findOrCreateCourier(courierId).noteAvailable();
  }

  void noteUnavailable(long courierId) {
    courierRepository.findOrCreateCourier(courierId).noteUnavailable();
  }

  private Courier findOrCreateCourier(long courierId) {
    Courier courier = Courier.create(courierId);
    try {
      return courierRepository.save(courier);
    } catch (DuplicateKeyException e) {
      return courierRepository.findById(courierId).get();
    }
  }

  @Transactional
  public void updateAvailability(long courierId, boolean available) {
    if (available)
      noteAvailable(courierId);
    else
      noteUnavailable(courierId);
  }


  // getCourierRoute()

  @Transactional
  public Optional<DeliveryStatus> getDeliveryInfo(long deliveryId) {
    return deliveryRepository.findById(deliveryId).map(delivery -> {
      Long assignedCourier = delivery.getAssignedCourier();
      List<Action> courierActions = Collections.EMPTY_LIST;
      if (assignedCourier != null) {
        Courier courier = courierRepository.findById(assignedCourier).get();
        courierActions = courier.actionsForDelivery(deliveryId);
      }
      return makeDeliveryStatus(delivery, assignedCourier, courierActions);
    });
  }

  private DeliveryStatus makeDeliveryStatus(Delivery delivery, Long assignedCourier, List<Action> courierActions) {
    return new DeliveryStatus(makeDeliveryInfo(delivery), assignedCourier, courierActions.stream().map(action -> makeActionInfo(action)).collect(Collectors.toList()));
  }

  private DeliveryInfo makeDeliveryInfo(Delivery delivery) {
    return new DeliveryInfo(delivery.getId(), delivery.getState());
  }

  private ActionInfo makeActionInfo(Action action) {
    return new ActionInfo(action.getType());
  }
}


import org.springframework.data.repository.CrudRepository;

public interface DeliveryRepository extends CrudRepository<Delivery, Long> {
}


import org.springframework.data.repository.CrudRepository;

public interface RestaurantRepository extends CrudRepository<Restaurant, Long> {
}


import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CourierRepository extends CrudRepository<Courier, Long>, CustomCourierRepository {

  @Query("SELECT c FROM Courier c WHERE c.available = true")
  List<Courier> findAllAvailable();

}


import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import net.chrisrichardson.eventstore.examples.customersandorders.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.messaging.DeliveryServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.web.DeliveryServiceWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({DeliveryServiceMessagingConfiguration.class, DeliveryServiceWebConfiguration.class,
        TramJdbcKafkaConfiguration.class, CommonSwaggerConfiguration.class
})
public class DeliveryServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(DeliveryServiceMain.class, args);
  }
}


import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryServiceTestData;
import net.chrisrichardson.ftgo.deliveryservice.messaging.RestaurantEventMapper;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;
import net.chrisrichardson.ftgo.restaurantservice.events.Menu;
import net.chrisrichardson.ftgo.restaurantservice.events.MenuItem;

import java.util.Collections;
import java.util.List;

import net.chrisrichardson.ftgo.common.Address;
import net.chrisrichardson.ftgo.common.Money;

public class RestaurantEventMother {

  public static final String CHICKEN_VINDALOO = "Chicken Vindaloo";
  public static final String CHICKEN_VINDALOO_MENU_ITEM_ID = "1";
  public static final String CHICKEN_VINDALOO_PRICE = "12.34";
  public static final Address RESTAURANT_ADDRESS = new Address("1 Main Street", "Unit 99", "Oakland", "CA", "94611");

  public static MenuItem CHICKEN_VINDALOO_MENU_ITEM = new MenuItem()
    .withId(CHICKEN_VINDALOO_MENU_ITEM_ID)
    .withName(CHICKEN_VINDALOO)
    .withPrice(CHICKEN_VINDALOO_PRICE);

  public static final List<MenuItem> AJANTA_RESTAURANT_MENU_ITEMS = Collections.singletonList(CHICKEN_VINDALOO_MENU_ITEM);


  static RestaurantCreated makeRestaurantCreated() {
    return new RestaurantCreated()
            .withName("Delicious Indian")
            .withAddress(RestaurantEventMapper.fromAddress(DeliveryServiceTestData.PICKUP_ADDRESS))
            .withMenu(new Menu().withMenuItems(AJANTA_RESTAURANT_MENU_ITEMS));
  }
}


import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryServiceTestData;
import net.chrisrichardson.ftgo.orderservice.api.OrderServiceChannels;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderCreatedEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDetails;
import net.chrisrichardson.ftgo.restaurantservice.RestaurantServiceChannels;
import net.chrisrichardson.ftgo.testutil.FtgoTestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.given;
import static io.eventuate.util.test.async.Eventually.eventually;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeliveryServiceOutOfProcessComponentTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DeliveryServiceOutOfProcessComponentTest {

  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration
  @Import({TramJdbcKafkaConfiguration.class, TramEventsPublisherConfiguration.class
  })
  public static class Config {
  }

  private String host = FtgoTestUtil.getDockerHostIp();
  private int port = 8089;
  private long restaurantId;
  private long orderId;

  @Autowired
  private DomainEventPublisher domainEventPublisher;

  // Duplication

  private String baseUrl(int port, String path, String... pathElements) {
    assertNotNull("host", host);

    StringBuilder sb = new StringBuilder("http://");
    sb.append(host);
    sb.append(":");
    sb.append(port);
    sb.append("/");
    sb.append(path);

    for (String pe : pathElements) {
      sb.append("/");
      sb.append(pe);
    }
    String s = sb.toString();
    System.out.println("url=" + s);
    return s;
  }

  @Test
  public void shouldScheduleDelivery() {

    createRestaurant();

    createOrder();

    assertDeliveryCreated();

    // createCourier
    // acceptTicket
    // TicketCancelled
  }

  private void assertDeliveryCreated() {

    eventually(() -> {
      String state = given().
              when().
              get(baseUrl(port, "deliveries", Long.toString(orderId))).
              then().
              statusCode(200).extract().path("deliveryInfo.state");

      assertEquals("PENDING", state);
    });
  }

  private void createOrder() {
    orderId = System.currentTimeMillis();
    domainEventPublisher.publish(OrderServiceChannels.ORDER_EVENT_CHANNEL, orderId, Collections.singletonList(
            new OrderCreatedEvent(new OrderDetails(0L, restaurantId, null, null),
                    DeliveryServiceTestData.DELIVERY_ADDRESS, null)));


  }

  private void createRestaurant() {
    restaurantId = System.currentTimeMillis();

    domainEventPublisher.publish(RestaurantServiceChannels.RESTAURANT_EVENT_CHANNEL, restaurantId, Collections.singletonList(RestaurantEventMother.makeRestaurantCreated()));

    sleep();
  }

  private void sleep() {
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}


import io.eventuate.common.spring.jdbc.EventuateTransactionTemplateConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryRepository;
import net.chrisrichardson.ftgo.deliveryservice.domain.DeliveryServiceTestData;
import net.chrisrichardson.ftgo.deliveryservice.domain.RestaurantRepository;
import net.chrisrichardson.ftgo.deliveryservice.messaging.DeliveryServiceMessagingConfiguration;
import net.chrisrichardson.ftgo.deliveryservice.web.DeliveryServiceWebConfiguration;
import net.chrisrichardson.ftgo.orderservice.api.OrderServiceChannels;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderCreatedEvent;
import net.chrisrichardson.ftgo.orderservice.api.events.OrderDetails;
import net.chrisrichardson.ftgo.restaurantservice.RestaurantServiceChannels;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static com.jayway.restassured.RestAssured.given;
import static io.eventuate.util.test.async.Eventually.eventually;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeliveryServiceInProcessComponentTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DeliveryServiceInProcessComponentTest {

  private long restaurantId;
  private long orderId;

  @Configuration
  @EnableAutoConfiguration
  @Import({DeliveryServiceMessagingConfiguration.class,
          DeliveryServiceWebConfiguration.class,
          TramInMemoryConfiguration.class,
          TramEventsPublisherConfiguration.class,
          EventuateTransactionTemplateConfiguration.class
  })
  public static class Config {
  }

  @LocalServerPort
  private int port;

  private String host = "localhost";

  @Autowired
  private DomainEventPublisher domainEventPublisher;

  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private DeliveryRepository deliveryRepository;

  @Test
  public void shouldScheduleDelivery() {

    createRestaurant();

    createOrder();

    assertDeliveryCreated();

    // createCourier
    // acceptTicket
    // TicketCancelled
  }

  private String baseUrl(int port, String path, String... pathElements) {
    assertNotNull("host", host);

    StringBuilder sb = new StringBuilder("http://");
    sb.append(host);
    sb.append(":");
    sb.append(port);
    sb.append("/");
    sb.append(path);

    for (String pe : pathElements) {
      sb.append("/");
      sb.append(pe);
    }
    String s = sb.toString();
    System.out.println("url=" + s);
    return s;
  }


  private void assertDeliveryCreated() {

    String state = given().
            when().
            get(baseUrl(port, "deliveries", Long.toString(orderId))).
            then().
            statusCode(200).extract().path("deliveryInfo.state");

    assertEquals("PENDING", state);
  }

  private void createOrder() {
    orderId = System.currentTimeMillis();
    domainEventPublisher.publish(OrderServiceChannels.ORDER_EVENT_CHANNEL, orderId, Collections.singletonList(
            new OrderCreatedEvent(new OrderDetails(0L, restaurantId, null, null),
                    DeliveryServiceTestData.DELIVERY_ADDRESS, null)));
    eventually(() -> assertTrue(deliveryRepository.findById(orderId).isPresent()));


  }

  private void createRestaurant() {
    restaurantId = System.currentTimeMillis();

    domainEventPublisher.publish(RestaurantServiceChannels.RESTAURANT_EVENT_CHANNEL, restaurantId,
            Collections.singletonList(RestaurantEventMother.makeRestaurantCreated()));

    eventually(() -> assertTrue(restaurantRepository.findById(restaurantId).isPresent()));
  }

}


import io.eventuate.tram.commands.common.Command;

public class ConfirmCreateTicket implements Command {
  private Long ticketId;

  private ConfirmCreateTicket() {
  }


  public ConfirmCreateTicket(Long ticketId) {
    this.ticketId = ticketId;
  }

  public Long getTicketId() {
    return ticketId;
  }

  public void setTicketId(Long ticketId) {
    this.ticketId = ticketId;
  }
}


import io.eventuate.tram.commands.common.Command;

public class UndoBeginCancelTicketCommand implements Command {

  private long restaurantId;
  private long orderId;

  private UndoBeginCancelTicketCommand() {
  }

  public UndoBeginCancelTicketCommand(long restaurantId, long orderId) {

    this.restaurantId = restaurantId;
    this.orderId = orderId;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }
}


import io.eventuate.tram.commands.CommandDestination;
import io.eventuate.tram.commands.common.Command;
import org.apache.commons.lang.builder.ToStringBuilder;

@CommandDestination("restaurantService")
public class CreateTicket implements Command {

  private Long orderId;
  private TicketDetails ticketDetails;
  private long restaurantId;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public TicketDetails getTicketDetails() {
    return ticketDetails;
  }

  public void setTicketDetails(TicketDetails orderDetails) {
    this.ticketDetails = orderDetails;
  }

  private CreateTicket() {

  }

  public CreateTicket(long restaurantId, long orderId, TicketDetails ticketDetails) {
    this.restaurantId = restaurantId;
    this.orderId = orderId;
    this.ticketDetails = ticketDetails;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }
}


import io.eventuate.tram.commands.common.Command;

public class BeginCancelTicketCommand implements Command {
  private long restaurantId;
  private long orderId;

  private BeginCancelTicketCommand() {
  }

  public BeginCancelTicketCommand(long restaurantId, long orderId) {

    this.restaurantId = restaurantId;
    this.orderId = orderId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }
}


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)
public class TicketLineItem {

  private int quantity;
  private String menuItemId;
  private String name;


  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getMenuItemId() {
    return menuItemId;
  }

  public void setMenuItemId(String menuItemId) {
    this.menuItemId = menuItemId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private TicketLineItem() {

  }

  public TicketLineItem(String menuItemId, String name, int quantity) {
    this.menuItemId = menuItemId;
    this.name = name;
    this.quantity = quantity;
  }
}


import io.eventuate.tram.commands.common.Command;

public class ConfirmCancelTicketCommand implements Command {

  private long restaurantId;
  private long orderId;

  private ConfirmCancelTicketCommand() {
  }

  public ConfirmCancelTicketCommand(long restaurantId, long orderId) {

    this.restaurantId = restaurantId;
    this.orderId = orderId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;

import java.util.List;

public class ConfirmReviseTicketCommand implements Command {
  private long restaurantId;
  private long orderId;
  private List<RevisedOrderLineItem> revisedOrderLineItems;

  private ConfirmReviseTicketCommand() {
  }

  public ConfirmReviseTicketCommand(long restaurantId, Long orderId, List<RevisedOrderLineItem> revisedOrderLineItems) {

    this.restaurantId = restaurantId;
    this.orderId = orderId;
    this.revisedOrderLineItems = revisedOrderLineItems;
  }

  public long getOrderId() {
    return orderId;
  }

  public void setOrderId(long orderId) {
    this.orderId = orderId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public List<RevisedOrderLineItem> getRevisedOrderLineItems() {
    return revisedOrderLineItems;
  }

  public void setRevisedOrderLineItems(List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.revisedOrderLineItems = revisedOrderLineItems;
  }
}


import io.eventuate.tram.commands.common.Command;

public class UndoBeginReviseTicketCommand implements Command {
  private long restaurantId;
  private Long orderId;

  public UndoBeginReviseTicketCommand() {
  }

  public UndoBeginReviseTicketCommand(long restaurantId, Long orderId) {

    this.restaurantId = restaurantId;
    this.orderId = orderId;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }
}



import io.eventuate.tram.commands.common.Command;

public class ChangeTicketLineItemQuantity implements Command {
  public ChangeTicketLineItemQuantity(Long orderId) {
  }
}


import io.eventuate.tram.commands.common.Command;
import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;

import java.util.List;
import java.util.Map;

public class BeginReviseTicketCommand implements Command {
  private long restaurantId;
  private Long orderId;
  private List<RevisedOrderLineItem> revisedOrderLineItems;

  private BeginReviseTicketCommand() {
  }

  public BeginReviseTicketCommand(long restaurantId, Long orderId, List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.restaurantId = restaurantId;
    this.orderId = orderId;
    this.revisedOrderLineItems = revisedOrderLineItems;
  }

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public List<RevisedOrderLineItem> getRevisedOrderLineItems() {
    return revisedOrderLineItems;
  }

  public void setRevisedOrderLineItems(List<RevisedOrderLineItem> revisedOrderLineItems) {
    this.revisedOrderLineItems = revisedOrderLineItems;
  }
}


public class KitchenServiceChannels {
  public static final String COMMAND_CHANNEL = "kitchenService";
  public static final String TICKET_EVENT_CHANNEL = "net.chrisrichardson.ftgo.kitchenservice.domain.Ticket";

}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class CreateTicketReply {
  private long ticketId;

  private CreateTicketReply() {
  }

  public CreateTicketReply(long ticketId) {

    this.ticketId = ticketId;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  public void setTicketId(long ticketId) {
    this.ticketId = ticketId;
  }

  public long getTicketId() {
    return ticketId;
  }
}


import io.eventuate.tram.commands.common.Command;

public class CancelCreateTicket implements Command {
  private Long ticketId;

  private CancelCreateTicket() {
  }

  public CancelCreateTicket(long ticketId) {
    this.ticketId = ticketId;
  }

  public Long getTicketId() {
    return ticketId;
  }

  public void setTicketId(Long ticketId) {
    this.ticketId = ticketId;
  }
}


import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

public class TicketDetails {
  private List<TicketLineItem> lineItems;

  public TicketDetails() {
  }

  public TicketDetails(List<TicketLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  public List<TicketLineItem> getLineItems() {
    return lineItems;
  }

  public void setLineItems(List<TicketLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}


public class TicketCancelled implements TicketDomainEvent {
}


import io.eventuate.tram.events.common.DomainEvent;

public interface TicketDomainEvent extends DomainEvent {
}


import java.time.LocalDateTime;

public class TicketAcceptedEvent implements TicketDomainEvent {
  private LocalDateTime readyBy;

  public TicketAcceptedEvent() {
  }

  public TicketAcceptedEvent(LocalDateTime readyBy) {
    this.readyBy = readyBy;
  }

  public LocalDateTime getReadyBy() {
    return readyBy;
  }

  public void setReadyBy(LocalDateTime readyBy) {
    this.readyBy = readyBy;
  }
}


import java.time.LocalDateTime;

public class TicketAcceptance {
  private LocalDateTime readyBy;

  public TicketAcceptance() {
  }

  public TicketAcceptance(LocalDateTime readyBy) {
    this.readyBy = readyBy;
  }

  public LocalDateTime getReadyBy() {
    return readyBy;
  }

  public void setReadyBy(LocalDateTime readyBy) {
    this.readyBy = readyBy;
  }
}


import io.eventuate.tram.spring.cloudcontractsupport.EventuateContractVerifierConfiguration;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.domain.KitchenService;
import net.chrisrichardson.ftgo.kitchenservice.domain.Ticket;
import net.chrisrichardson.ftgo.kitchenservice.messagehandlers.KitchenServiceMessageHandlersConfiguration;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MessagingBase.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class MessagingBase {

  @Configuration
  @EnableAutoConfiguration
  @Import({KitchenServiceMessageHandlersConfiguration.class, EventuateContractVerifierConfiguration.class})
  public static class TestConfiguration {

  }

  @MockBean
  private KitchenService kitchenService;

  @Before
  public void setup() {
     reset(kitchenService);
     when(kitchenService.createTicket(eq(1L), eq(99L), any(TicketDetails.class)))
             .thenReturn(new Ticket(1L, 99L, new TicketDetails(Collections.emptyList())));
  }

}


import io.eventuate.common.spring.jdbc.EventuateTransactionTemplateConfiguration;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.cloudcontractsupport.EventuateContractVerifierConfiguration;
import net.chrisrichardson.ftgo.common.CommonJsonMapperInitializer;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketAcceptedEvent;
import net.chrisrichardson.ftgo.kitchenservice.domain.Ticket;
import net.chrisrichardson.ftgo.kitchenservice.domain.TicketDomainEventPublisher;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DeliveryserviceMessagingBase.TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureMessageVerifier
public abstract class DeliveryserviceMessagingBase {

  static {
    CommonJsonMapperInitializer.registerMoneyModule();
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({EventuateContractVerifierConfiguration.class, TramEventsPublisherConfiguration.class, TramInMemoryConfiguration.class, EventuateTransactionTemplateConfiguration.class})
  public static class TestConfiguration {

    @Bean
    public TicketDomainEventPublisher orderAggregateEventPublisher(DomainEventPublisher eventPublisher) {
      return new TicketDomainEventPublisher(eventPublisher);
    }
  }


  @Autowired
  private TicketDomainEventPublisher ticketDomainEventPublisher;

  protected void ticketAcceptedEvent() {
    Ticket ticket = new Ticket(101L, 99L, new TicketDetails(Collections.emptyList()));
    ticketDomainEventPublisher.publish(ticket,
            Collections.singletonList(new TicketAcceptedEvent(LocalDateTime.now())));
  }

}



import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcher;
import io.eventuate.tram.sagas.participant.SagaCommandDispatcherFactory;
import io.eventuate.tram.sagas.spring.participant.SagaParticipantConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import net.chrisrichardson.ftgo.kitchenservice.domain.KitchenDomainConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({KitchenDomainConfiguration.class, SagaParticipantConfiguration.class, CommonConfiguration.class, TramEventSubscriberConfiguration.class, SagaParticipantConfiguration.class})
public class KitchenServiceMessageHandlersConfiguration {

  @Bean
  public KitchenServiceEventConsumer ticketEventConsumer() {
    return new KitchenServiceEventConsumer();
  }

  @Bean
  public KitchenServiceCommandHandler kitchenServiceCommandHandler() {
    return new KitchenServiceCommandHandler();
  }

  @Bean
  public SagaCommandDispatcher kitchenServiceSagaCommandDispatcher(KitchenServiceCommandHandler kitchenServiceCommandHandler, SagaCommandDispatcherFactory sagaCommandDispatcherFactory) {
    return sagaCommandDispatcherFactory.make("kitchenServiceCommands", kitchenServiceCommandHandler.commandHandlers());
  }

  @Bean
  public DomainEventDispatcher domainEventDispatcher(KitchenServiceEventConsumer kitchenServiceEventConsumer, DomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("kitchenServiceEvents", kitchenServiceEventConsumer.domainEventHandlers());
  }
}


import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import net.chrisrichardson.ftgo.kitchenservice.domain.KitchenService;
import net.chrisrichardson.ftgo.kitchenservice.domain.RestaurantMenu;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantCreated;
import net.chrisrichardson.ftgo.restaurantservice.events.RestaurantMenuRevised;
import org.springframework.beans.factory.annotation.Autowired;


public class KitchenServiceEventConsumer {

  @Autowired
  private KitchenService kitchenService;

  public DomainEventHandlers domainEventHandlers() {
    return DomainEventHandlersBuilder
            .forAggregateType("net.chrisrichardson.ftgo.restaurantservice.domain.Restaurant")
            .onEvent(RestaurantCreated.class, this::createMenu)
            .onEvent(RestaurantMenuRevised.class, this::reviseMenu)
            .build();
  }

  private void createMenu(DomainEventEnvelope<RestaurantCreated> de) {
    String restaurantIds = de.getAggregateId();
    long id = Long.parseLong(restaurantIds);
    RestaurantMenu menu = new RestaurantMenu(RestaurantEventMapper.toMenuItems(de.getEvent().getMenu().getMenuItems()));
    kitchenService.createMenu(id, menu);
  }

  public void reviseMenu(DomainEventEnvelope<RestaurantMenuRevised> de) {
    long id = Long.parseLong(de.getAggregateId());
    RestaurantMenu menu = new RestaurantMenu(RestaurantEventMapper.toMenuItems(de.getEvent().getMenu().getMenuItems()));
    kitchenService.reviseMenu(id, menu);
  }

}


import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;
import net.chrisrichardson.ftgo.kitchenservice.api.*;
import net.chrisrichardson.ftgo.kitchenservice.domain.RestaurantDetailsVerificationException;
import net.chrisrichardson.ftgo.kitchenservice.domain.Ticket;
import net.chrisrichardson.ftgo.kitchenservice.domain.KitchenService;
import org.springframework.beans.factory.annotation.Autowired;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;
import static io.eventuate.tram.sagas.participant.SagaReplyMessageBuilder.withLock;

public class KitchenServiceCommandHandler {

  @Autowired
  private KitchenService kitchenService;

  public CommandHandlers commandHandlers() {
    return SagaCommandHandlersBuilder
            .fromChannel(KitchenServiceChannels.COMMAND_CHANNEL)
            .onMessage(CreateTicket.class, this::createTicket)
            .onMessage(ConfirmCreateTicket.class, this::confirmCreateTicket)
            .onMessage(CancelCreateTicket.class, this::cancelCreateTicket)

            .onMessage(BeginCancelTicketCommand.class, this::beginCancelTicket)
            .onMessage(ConfirmCancelTicketCommand.class, this::confirmCancelTicket)
            .onMessage(UndoBeginCancelTicketCommand.class, this::undoBeginCancelTicket)

            .onMessage(BeginReviseTicketCommand.class, this::beginReviseTicket)
            .onMessage(UndoBeginReviseTicketCommand.class, this::undoBeginReviseTicket)
            .onMessage(ConfirmReviseTicketCommand.class, this::confirmReviseTicket)
            .build();
  }

  private Message createTicket(CommandMessage<CreateTicket>
                                                cm) {
    CreateTicket command = cm.getCommand();
    long restaurantId = command.getRestaurantId();
    Long ticketId = command.getOrderId();
    TicketDetails ticketDetails = command.getTicketDetails();


    try {
      Ticket ticket = kitchenService.createTicket(restaurantId, ticketId, ticketDetails);
      CreateTicketReply reply = new CreateTicketReply(ticket.getId());
      return withLock(Ticket.class, ticket.getId()).withSuccess(reply);
    } catch (RestaurantDetailsVerificationException e) {
      return withFailure();
    }
  }

  private Message confirmCreateTicket
          (CommandMessage<ConfirmCreateTicket> cm) {
    Long ticketId = cm.getCommand().getTicketId();
    kitchenService.confirmCreateTicket(ticketId);
    return withSuccess();
  }

  private Message cancelCreateTicket
          (CommandMessage<CancelCreateTicket> cm) {
    Long ticketId = cm.getCommand().getTicketId();
    kitchenService.cancelCreateTicket(ticketId);
    return withSuccess();
  }


  private Message beginCancelTicket(CommandMessage<BeginCancelTicketCommand> cm) {
    kitchenService.cancelTicket(cm.getCommand().getRestaurantId(), cm.getCommand().getOrderId());
    return withSuccess();
  }
  private Message confirmCancelTicket(CommandMessage<ConfirmCancelTicketCommand> cm) {
    kitchenService.confirmCancelTicket(cm.getCommand().getRestaurantId(), cm.getCommand().getOrderId());
    return withSuccess();
  }

  private Message undoBeginCancelTicket(CommandMessage<UndoBeginCancelTicketCommand> cm) {
    kitchenService.undoCancel(cm.getCommand().getRestaurantId(), cm.getCommand().getOrderId());
    return withSuccess();
  }

  public Message beginReviseTicket(CommandMessage<BeginReviseTicketCommand> cm) {
    kitchenService.beginReviseOrder(cm.getCommand().getRestaurantId(), cm.getCommand().getOrderId(), cm.getCommand().getRevisedOrderLineItems());
    return withSuccess();
  }

  public Message undoBeginReviseTicket(CommandMessage<UndoBeginReviseTicketCommand> cm) {
    kitchenService.undoBeginReviseOrder(cm.getCommand().getRestaurantId(), cm.getCommand().getOrderId());
    return withSuccess();
  }

  public Message confirmReviseTicket(CommandMessage<ConfirmReviseTicketCommand> cm) {
    kitchenService.confirmReviseTicket(cm.getCommand().getRestaurantId(), cm.getCommand().getOrderId(), cm.getCommand().getRevisedOrderLineItems());
    return withSuccess();
  }


}



import net.chrisrichardson.ftgo.common.Money;
import net.chrisrichardson.ftgo.restaurantservice.events.MenuItem;

import java.util.List;
import java.util.stream.Collectors;

public class RestaurantEventMapper {

  public static List<net.chrisrichardson.ftgo.kitchenservice.domain.MenuItem> toMenuItems(List<MenuItem> menuItems) {
    return menuItems.stream().map(mi -> new net.chrisrichardson.ftgo.kitchenservice.domain.MenuItem(mi.getId(), mi.getName(), new Money(mi.getPrice()))).collect(Collectors.toList());
  }

}


public class GetRestaurantResponse  {
  private long restaurantId;

  public long getRestaurantId() {
    return restaurantId;
  }

  public void setRestaurantId(long restaurantId) {
    this.restaurantId = restaurantId;
  }

  public GetRestaurantResponse() {

  }

  public GetRestaurantResponse(long restaurantId) {
    this.restaurantId = restaurantId;
  }
}


import net.chrisrichardson.ftgo.kitchenservice.domain.Restaurant;
import net.chrisrichardson.ftgo.kitchenservice.domain.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/restaurants")
public class RestaurantController {

  @Autowired
  private RestaurantRepository restaurantRepository;

  @RequestMapping(path = "/{restaurantId}", method = RequestMethod.GET)
  public ResponseEntity<GetRestaurantResponse> getRestaurant(@PathVariable long restaurantId) {
    return restaurantRepository.findById(restaurantId)
            .map(restaurant -> new ResponseEntity<>(new GetRestaurantResponse(restaurantId), HttpStatus.OK))
            .orElseGet( () -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }
}


import net.chrisrichardson.ftgo.kitchenservice.domain.KitchenDomainConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(KitchenDomainConfiguration.class)
@ComponentScan
public class KitchenServiceWebConfiguration {


}


import net.chrisrichardson.ftgo.kitchenservice.api.web.TicketAcceptance;
import net.chrisrichardson.ftgo.kitchenservice.domain.KitchenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class KitchenController {

  private KitchenService kitchenService;

  public KitchenController(KitchenService kitchenService) {
    this.kitchenService = kitchenService;
  }

  @RequestMapping(path="/tickets/{ticketId}/accept", method= RequestMethod.POST)
  public void acceptTicket(@PathVariable long ticketId, @RequestBody TicketAcceptance ticketAcceptance) {
    kitchenService.accept(ticketId, ticketAcceptance.getReadyBy());
  }
}


public enum TicketState {
  CREATE_PENDING, AWAITING_ACCEPTANCE, ACCEPTED, PREPARING, READY_FOR_PICKUP, PICKED_UP, CANCEL_PENDING, CANCELLED, REVISION_PENDING,
}


import org.springframework.data.repository.CrudRepository;

public interface TicketRepository extends CrudRepository<Ticket, Long> {
}



import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

public class TicketRevised implements TicketDomainEvent {
}


import io.eventuate.tram.events.aggregates.AbstractAggregateDomainEventPublisher;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

public class TicketDomainEventPublisher extends AbstractAggregateDomainEventPublisher<Ticket, TicketDomainEvent> {

  public TicketDomainEventPublisher(DomainEventPublisher eventPublisher) {
    super(eventPublisher, Ticket.class, Ticket::getId);
  }

}



import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

public class TicketCreatedEvent implements TicketDomainEvent {
  public TicketCreatedEvent(Long id, TicketDetails details) {

  }
}


import java.util.List;

public class RestaurantMenu {
  private List<MenuItem> menuItems;

  public RestaurantMenu(List<MenuItem> menuItems) {
    this.menuItems = menuItems;
  }

  public List<MenuItem> getMenuItems() {
    return menuItems;
  }
}


import io.eventuate.tram.events.common.DomainEvent;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "kitchen_service_restaurants")
@Access(AccessType.FIELD)
public class Restaurant {

  @Id
  private Long id;

  @Embedded
  @ElementCollection
  @CollectionTable(name = "kitchen_service_restaurant_menu_items")
  private List<MenuItem> menuItems;

  private Restaurant() {
  }

  public Restaurant(long id, List<MenuItem> menuItems) {
    this.id = id;
    this.menuItems = menuItems;
  }

  public List<DomainEvent> reviseMenu(RestaurantMenu revisedMenu) {
    throw new UnsupportedOperationException();
  }

  public void verifyRestaurantDetails(TicketDetails ticketDetails) {
    // TODO - implement me
  }

  public Long getId() {
    return id;
  }

}


public class ChangeLineItemQuantityCommand {
}


public class RestaurantDetailsVerificationException extends RuntimeException {
}


import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import net.chrisrichardson.ftgo.common.CommonConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories
@ComponentScan
@EntityScan
@Import({TramEventsPublisherConfiguration.class, CommonConfiguration.class})
public class KitchenDomainConfiguration {

  @Bean
  public KitchenService kitchenService() {
    return new KitchenService();
  }

  @Bean
  public TicketDomainEventPublisher restaurantAggregateEventPublisher(DomainEventPublisher domainEventPublisher) {
    return new TicketDomainEventPublisher(domainEventPublisher);
  }
}


import net.chrisrichardson.ftgo.common.Money;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.FIELD)
public class MenuItem {

  private String id;
  private String name;
  private Money price;

  private MenuItem() {
  }

  public MenuItem(String id, String name, Money price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  @Override
  public boolean equals(Object o) {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Money getPrice() {
    return price;
  }

  public void setPrice(Money price) {
    this.price = price;
  }
}


import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

public class TicketPreparationCompletedEvent implements TicketDomainEvent {
}


public class TicketNotFoundException extends RuntimeException {
  public TicketNotFoundException(long orderId) {
    super("Ticket not found: " + orderId);
  }
}


import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

public class TicketPickedUpEvent implements TicketDomainEvent {
}


import org.springframework.data.repository.CrudRepository;

public interface RestaurantRepository extends CrudRepository<Restaurant, Long> {
}


public class CancelCommand {
  private long orderId;
  private boolean force;

  public long getOrderId() {
    return orderId;
  }

  public boolean isForce() {
    return force;
  }
}


import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

public class TicketPreparationStartedEvent implements TicketDomainEvent {
}


import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class KitchenService {

  @Autowired
  private TicketRepository ticketRepository;

  @Autowired
  private TicketDomainEventPublisher domainEventPublisher;

  @Autowired
  private RestaurantRepository restaurantRepository;

  public void createMenu(long id, RestaurantMenu menu) {
    Restaurant restaurant = new Restaurant(id, menu.getMenuItems());
    restaurantRepository.save(restaurant);
  }

  public void reviseMenu(long ticketId, RestaurantMenu revisedMenu) {
    Restaurant restaurant = restaurantRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    restaurant.reviseMenu(revisedMenu);
  }

  public Ticket createTicket(long restaurantId, Long ticketId, TicketDetails ticketDetails) {
    ResultWithDomainEvents<Ticket, TicketDomainEvent> rwe = Ticket.create(restaurantId, ticketId, ticketDetails);
    ticketRepository.save(rwe.result);
    domainEventPublisher.publish(rwe.result, rwe.events);
    return rwe.result;
  }

  @Transactional
  public void accept(long ticketId, LocalDateTime readyBy) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    List<TicketDomainEvent> events = ticket.accept(readyBy);
    domainEventPublisher.publish(ticket, events);
  }

  public void confirmCreateTicket(Long ticketId) {
    Ticket ro = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    List<TicketDomainEvent> events = ro.confirmCreate();
    domainEventPublisher.publish(ro, events);
  }

  public void cancelCreateTicket(Long ticketId) {
    Ticket ro = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    List<TicketDomainEvent> events = ro.cancelCreate();
    domainEventPublisher.publish(ro, events);
  }


  public void cancelTicket(long restaurantId, long ticketId) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    // TODO - verify restaurant id
    List<TicketDomainEvent> events = ticket.cancel();
    domainEventPublisher.publish(ticket, events);
  }


  public void confirmCancelTicket(long restaurantId, long ticketId) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    // TODO - verify restaurant id
    List<TicketDomainEvent> events = ticket.confirmCancel();
    domainEventPublisher.publish(ticket, events);
  }

  public void undoCancel(long restaurantId, long ticketId) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    // TODO - verify restaurant id
    List<TicketDomainEvent> events = ticket.undoCancel();
    domainEventPublisher.publish(ticket, events);

  }

  public void beginReviseOrder(long restaurantId, Long ticketId, List<RevisedOrderLineItem> revisedOrderLineItems) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    // TODO - verify restaurant id
    List<TicketDomainEvent> events = ticket.beginReviseOrder(revisedOrderLineItems);
    domainEventPublisher.publish(ticket, events);

  }

  public void undoBeginReviseOrder(long restaurantId, Long ticketId) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    // TODO - verify restaurant id
    List<TicketDomainEvent> events = ticket.undoBeginReviseOrder();
    domainEventPublisher.publish(ticket, events);
  }

  public void confirmReviseTicket(long restaurantId, long ticketId, List<RevisedOrderLineItem> revisedOrderLineItems) {
    Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
    // TODO - verify restaurant id
    List<TicketDomainEvent> events = ticket.confirmReviseTicket(revisedOrderLineItems);
    domainEventPublisher.publish(ticket, events);
  }


  // ...
}


import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import net.chrisrichardson.ftgo.common.NotYetImplementedException;
import net.chrisrichardson.ftgo.common.RevisedOrderLineItem;
import net.chrisrichardson.ftgo.common.UnsupportedStateTransitionException;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketDetails;
import net.chrisrichardson.ftgo.kitchenservice.api.TicketLineItem;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketAcceptedEvent;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketCancelled;
import net.chrisrichardson.ftgo.kitchenservice.api.events.TicketDomainEvent;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Entity
@Table(name = "tickets")
@Access(AccessType.FIELD)
public class Ticket {

  @Id
  private Long id;

  @Enumerated(EnumType.STRING)
  private TicketState state;

  private TicketState previousState;

  private Long restaurantId;

  @ElementCollection
  @CollectionTable(name = "ticket_line_items")
  private List<TicketLineItem> lineItems;

  private LocalDateTime readyBy;
  private LocalDateTime acceptTime;
  private LocalDateTime preparingTime;
  private LocalDateTime pickedUpTime;
  private LocalDateTime readyForPickupTime;

  public static ResultWithDomainEvents<Ticket, TicketDomainEvent> create(long restaurantId, Long id, TicketDetails details) {
    return new ResultWithDomainEvents<>(new Ticket(restaurantId, id, details));
  }

  private Ticket() {
  }

  public Ticket(long restaurantId, Long id, TicketDetails details) {
    this.restaurantId = restaurantId;
    this.id = id;
    this.state = TicketState.CREATE_PENDING;
    this.lineItems = details.getLineItems();
  }

  public List<TicketDomainEvent> confirmCreate() {
    switch (state) {
      case CREATE_PENDING:
        state = TicketState.AWAITING_ACCEPTANCE;
        return singletonList(new TicketCreatedEvent(id, new TicketDetails()));
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<TicketDomainEvent> cancelCreate() {
    throw new NotYetImplementedException();
  }


  public List<TicketDomainEvent> accept(LocalDateTime readyBy) {
    switch (state) {
      case AWAITING_ACCEPTANCE:
        // Verify that readyBy is in the futurestate = TicketState.ACCEPTED;
        this.acceptTime = LocalDateTime.now();
        if (!acceptTime.isBefore(readyBy))
          throw new IllegalArgumentException(String.format("readyBy %s is not after now %s", readyBy, acceptTime));
        this.readyBy = readyBy;
        return singletonList(new TicketAcceptedEvent(readyBy));
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  // TODO reject()

  // TODO cancel()

  public List<TicketDomainEvent> preparing() {
    switch (state) {
      case ACCEPTED:
        this.state = TicketState.PREPARING;
        this.preparingTime = LocalDateTime.now();
        return singletonList(new TicketPreparationStartedEvent());
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<TicketDomainEvent> readyForPickup() {
    switch (state) {
      case PREPARING:
        this.state = TicketState.READY_FOR_PICKUP;
        this.readyForPickupTime = LocalDateTime.now();
        return singletonList(new TicketPreparationCompletedEvent());
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<TicketDomainEvent> pickedUp() {
    switch (state) {
      case READY_FOR_PICKUP:
        this.state = TicketState.PICKED_UP;
        this.pickedUpTime = LocalDateTime.now();
        return singletonList(new TicketPickedUpEvent());
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public void changeLineItemQuantity() {
    switch (state) {
      case AWAITING_ACCEPTANCE:
        // TODO
        break;
      case PREPARING:
        // TODO - too late
        break;
      default:
        throw new UnsupportedStateTransitionException(state);
    }

  }

  public List<TicketDomainEvent> cancel() {
    switch (state) {
      case AWAITING_ACCEPTANCE:
      case ACCEPTED:
        this.previousState = state;
        this.state = TicketState.CANCEL_PENDING;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public Long getId() {
    return id;
  }

  public List<TicketDomainEvent> confirmCancel() {
    switch (state) {
      case CANCEL_PENDING:
        this.state = TicketState.CANCELLED;
        return singletonList(new TicketCancelled());
      default:
        throw new UnsupportedStateTransitionException(state);

    }
  }
  public List<TicketDomainEvent> undoCancel() {
    switch (state) {
      case CANCEL_PENDING:
        this.state = this.previousState;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);

    }
  }

  public List<TicketDomainEvent> beginReviseOrder(List<RevisedOrderLineItem> revisedOrderLineItems) {
    switch (state) {
      case AWAITING_ACCEPTANCE:
      case ACCEPTED:
        this.previousState = state;
        this.state = TicketState.REVISION_PENDING;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<TicketDomainEvent> undoBeginReviseOrder() {
    switch (state) {
      case REVISION_PENDING:
        this.state = this.previousState;
        return emptyList();
      default:
        throw new UnsupportedStateTransitionException(state);
    }
  }

  public List<TicketDomainEvent> confirmReviseTicket(List<RevisedOrderLineItem> revisedOrderLineItems) {
    switch (state) {
      case REVISION_PENDING:
        this.state = this.previousState;
        return singletonList(new TicketRevised());
      default:
        throw new UnsupportedStateTransitionException(state);

    }
  }
}


import io.eventuate.tram.spring.jdbckafka.TramJdbcKafkaConfiguration;
import net.chrisrichardson.eventstore.examples.customersandorders.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.ftgo.kitchenservice.messagehandlers.KitchenServiceMessageHandlersConfiguration;
import net.chrisrichardson.ftgo.kitchenservice.web.KitchenServiceWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({KitchenServiceWebConfiguration.class,
        KitchenServiceMessageHandlersConfiguration.class,
        TramJdbcKafkaConfiguration.class,
        CommonSwaggerConfiguration.class})
public class KitchenServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(KitchenServiceMain.class, args);
  }
}

