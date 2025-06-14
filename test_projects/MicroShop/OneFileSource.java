
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.embedded.RedisServer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

/**
 * Test case for {@link InventoryRestAPIVerticle}.
 *
 * @author Eric Zhao
 */
public class InventoryApiTest {

  private static RedisServer server;

  private Vertx vertx;

  @BeforeClass
  static public void startRedis() throws Exception {
    server = new RedisServer(6379);
    System.out.println("Created embedded redis server on port 6379");
    server.start();
  }

  @AfterClass
  static public void stopRedis() throws Exception {
    server.stop();
  }

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    AtomicBoolean completed = new AtomicBoolean();
    vertx.deployVerticle(new InventoryRestAPIVerticle(), ar -> completed.set(ar.succeeded()));
    await().untilAtomic(completed, is(true));
  }

  @After
  public void tearDown() throws Exception {
    AtomicBoolean completed = new AtomicBoolean();
    vertx.close((v) -> completed.set(true));
    await().untilAtomic(completed, is(true));
  }

  @Test
  public void testGetAndAdd() throws Exception {
    int productId = ThreadLocalRandom.current().nextInt();
    Response response = given().port(8086).get("/" + productId);
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.asString()).isEqualTo("0");

    int inc = 10;
    response = given().port(8086).put("/" + productId + "/increase?n=" + inc);
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.asString()).isEqualTo(String.valueOf(inc));

    int dec = 8;
    response = given().port(8086).put("/" + productId + "/decrease?n=" + dec);
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.asString()).isEqualTo(String.valueOf(inc - dec));
  }

}

import io.vertx.blueprint.microservice.common.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import static io.vertx.blueprint.microservice.common.config.ConfigRetrieverHelper.configurationRetriever;
import static io.vertx.blueprint.microservice.common.config.Log4jConfigurationServiceHandler.log4jSubscriber;

/**
 * A verticle supplies HTTP endpoint for inventory service API.
 *
 * @author Eric Zhao
 */
public class InventoryRestAPIVerticle extends RestAPIVerticle {

  private static final Logger logger = LoggerFactory.getLogger(InventoryRestAPIVerticle.class);

  private static final String SERVICE_NAME = "inventory-rest-api";

  private static final String API_INCREASE = "/:productId/increase";
  private static final String API_DECREASE = "/:productId/decrease";
  private static final String API_RETRIEVE = "/:productId";

  private static final long SCAN_PERIOD = 20000L;

  private InventoryService inventoryService;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    configurationRetriever // TODO: enhance its usage
      .usingScanPeriod(SCAN_PERIOD)
      .withHttpStore("config-server", 80, "/inventory-microservice/docker.json")
      .rxCreateConfig(vertx)
      .subscribe(log4jSubscriber);

    this.inventoryService = InventoryService.createService(vertx, config());

    final Router router = Router.router(vertx);
    // body handler
    router.route().handler(BodyHandler.create());
    // API handler
    router.get(API_RETRIEVE).handler(this::apiRetrieve);
    router.put(API_INCREASE).handler(this::apiIncrease);
    router.put(API_DECREASE).handler(this::apiDecrease);

    // get HTTP host and port from configuration, or use default value
    String host = config().getString("inventory.http.address", "0.0.0.0");
    int port = config().getInteger("inventory.http.port", 8086);

    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void apiIncrease(RoutingContext context) {
    try {
      String productId = context.request().getParam("productId");
      int increase = Integer.valueOf(context.request().getParam("n"));
      if (increase <= 0) {
        badRequest(context, new IllegalStateException("Negative increase"));
      } else {
        inventoryService.increase(productId, increase)
          .setHandler(rawResultHandler(context));
      }
    } catch (Exception ex) {
      context.fail(400);
    }
  }

  private void apiDecrease(RoutingContext context) {
    try {
      String productId = context.request().getParam("productId");
      int decrease = Integer.valueOf(context.request().getParam("n"));
      if (decrease <= 0) {
        badRequest(context, new IllegalStateException("Negative decrease"));
      } else {
        inventoryService.decrease(productId, decrease)
          .setHandler(rawResultHandler(context));
      }
    } catch (Exception ex) {
      context.fail(400);
    }
  }

  private void apiRetrieve(RoutingContext context) {
    logger.info("Retrieving Product: " + context.request().getParam("productId"));
    String productId = context.request().getParam("productId");
    inventoryService.retrieveInventoryForProduct(productId)
      .setHandler(rawResultHandler(context));
  }
}


import io.vertx.blueprint.microservice.inventory.impl.InventoryServiceImpl;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Inventory service (asynchronous based on Future).
 */
public interface InventoryService {

  /**
   * Create a new inventory service instance.
   *
   * @param vertx  Vertx instance
   * @param config configuration object
   * @return a new inventory service instance
   */
  static InventoryService createService(Vertx vertx, JsonObject config) {
    return new InventoryServiceImpl(vertx, config);
  }

  /**
   * Increase the inventory amount of a certain product.
   *
   * @param productId the id of the product
   * @param increase  increase amount
   * @return the asynchronous result
   */
  Future<Integer> increase(String productId, int increase);

  /**
   * Decrease the inventory amount of a certain product.
   *
   * @param productId the id of the product
   * @param decrease  decrease amount
   * @return the asynchronous result
   */
  Future<Integer> decrease(String productId, int decrease);

  /**
   * Retrieve the inventory amount of a certain product.
   *
   * @param productId the id of the product
   * @return the asynchronous result
   */
  Future<Integer> retrieveInventoryForProduct(String productId);

}


import io.vertx.blueprint.microservice.inventory.InventoryService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * Implementation of {@link InventoryService}.
 */
public class InventoryServiceImpl implements InventoryService {

  private static final String PREFIX = "inventory:v1:"; // version I, very simple

  private final RedisClient client;

  public InventoryServiceImpl(Vertx vertx, JsonObject config) {
    RedisOptions redisOptions = new RedisOptions()
      .setHost(config.getString("redis.host", "localhost"))
      .setPort(config.getInteger("redis.port", 6379));
    this.client = RedisClient.create(vertx, redisOptions);
  }

  @Override
  public Future<Integer> increase(String productId, int increase) {
    Future<Long> future = Future.future();
    client.incrby(PREFIX + productId, increase, future.completer());
    return future.map(Long::intValue);
  }

  @Override
  public Future<Integer> decrease(String productId, int decrease) {
    Future<Long> future = Future.future();
    client.decrby(PREFIX + productId, decrease, future.completer());
    return future.map(Long::intValue);
  }

  @Override
  public Future<Integer> retrieveInventoryForProduct(String productId) {
    Future<String> future = Future.future();
    client.get(PREFIX + productId, future.completer());
    return future.map(r -> r == null ? "0" : r)
      .map(Integer::valueOf);
  }

}


import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing payment transactions query.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface PaymentQueryService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "payment-query-eb-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.payment.query";

  /**
   * Initialize the persistence.
   *
   * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates
   *                      whether the operation was successful or not.
   */
  void initializePersistence(Handler<AsyncResult<Void>> resultHandler);

  /**
   * Add a payment record into the backend persistence.
   *
   * @param payment       payment entity
   * @param resultHandler async result handler
   */
  void addPaymentRecord(Payment payment, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Retrieve payment record from backend by payment id.
   *
   * @param payId         payment id
   * @param resultHandler async result handler
   */
  void retrievePaymentRecord(String payId, Handler<AsyncResult<Payment>> resultHandler);

}


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Payment data object.
 *
 * @author Eric Zhao
 */
@DataObject(generateConverter = true)
public class Payment {

  private String payId;
  private Double payAmount;
  private Short paySource;
  private Long paymentTime;

  public Payment() {
    // Empty constructor
  }

  public Payment(JsonObject json) {
    PaymentConverter.fromJson(json, this);
  }

  public Payment(Payment other) {
    this.payId = other.payId;
    this.payAmount = other.payAmount;
    this.paySource = other.paySource;
    this.paymentTime = other.paymentTime;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    PaymentConverter.toJson(this, json);
    return json;
  }

  public Payment(Long payCounter, Double payAmount, Short paySource) {
    initId(payCounter);
    this.payAmount = payAmount;
    this.paySource = paySource;
    this.paymentTime = System.currentTimeMillis();
  }

  public String getPayId() {
    return payId;
  }

  public Payment setPayId(String payId) {
    this.payId = payId;
    return this;
  }

  public Double getPayAmount() {
    return payAmount;
  }

  public Payment setPayAmount(Double payAmount) {
    this.payAmount = payAmount;
    return this;
  }

  public Short getPaySource() {
    return paySource;
  }

  public Payment setPaySource(Short paySource) {
    this.paySource = paySource;
    return this;
  }

  public Long getPaymentTime() {
    return paymentTime;
  }

  public Payment setPaymentTime(Long paymentTime) {
    this.paymentTime = paymentTime;
    return this;
  }

  void initId(Long counter) {
    if (counter < 0) {
      throw new IllegalStateException("Negative counter");
    }
    if (this.payId != null && !this.payId.equals("")) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      String datePrefix = LocalDate.now().format(formatter);
      int mod = 12 - (int) Math.sqrt(counter);
      char[] zeroChars = new char[mod];
      for (int i = 0; i < mod; i++) {
        zeroChars[i] = '0';
      }
      String zs = new String(zeroChars);
      this.payId = datePrefix + zs + counter;
    }
  }

  @Override
  public String toString() {
    return this.toJson().encodePrettily();
  }
}

/**
 * Indicates that this module contains classes that need to be generated / processed.
 */
@ModuleGen(name = "vertx-blueprint-payment", groupPackage = "io.vertx.blueprint.microservice.payment")


import io.vertx.codegen.annotations.ModuleGen;

import io.vertx.blueprint.microservice.payment.Payment;
import io.vertx.blueprint.microservice.payment.PaymentQueryService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;

/**
 * Implementation of {@link PaymentQueryService}.
 */
public class PaymentQueryServiceImpl implements PaymentQueryService {

  private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `payment` (\n" +
    "  `payId` varchar(24) NOT NULL,\n" +
    "  `payAmount` double NOT NULL,\n" +
    "  `paySource` smallint(6) NOT NULL,\n" +
    "  `paymentTime` bigint(20) NOT NULL,\n" +
    "  PRIMARY KEY (`payId`) )";
  private static final String INSERT_STATEMENT = "INSERT INTO payment (payId, payAmount, paySource, paymentTime) VALUES (?, ?, ?, ?)";
  private static final String FETCH_STATEMENT = "SELECT * FROM payment WHERE payId = ?";

  private final JDBCClient jdbc;

  public PaymentQueryServiceImpl(Vertx vertx, JsonObject config) {
    this.jdbc = JDBCClient.createNonShared(vertx, config);
  }

  @Override
  public void initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    jdbc.getConnection(connHandler(resultHandler, connection -> {
      connection.execute(CREATE_STATEMENT, r -> {
        resultHandler.handle(r);
        connection.close();
      });
    }));
  }

  @Override
  public void addPaymentRecord(Payment payment, Handler<AsyncResult<Void>> resultHandler) {
    jdbc.getConnection(connHandler(resultHandler, connection -> {
      connection.updateWithParams(INSERT_STATEMENT, new JsonArray().add(payment.getPayId())
        .add(payment.getPayAmount())
        .add(payment.getPaySource())
        .add(payment.getPaymentTime()), r -> {
        if (r.succeeded()) {
          resultHandler.handle(Future.succeededFuture());
        } else {
          resultHandler.handle(Future.failedFuture(r.cause()));
        }
        connection.close();
      });
    }));
  }

  @Override
  public void retrievePaymentRecord(String payId, Handler<AsyncResult<Payment>> resultHandler) {
    jdbc.getConnection(connHandler(resultHandler, connection -> {
      connection.queryWithParams(FETCH_STATEMENT, new JsonArray().add(payId), r -> {
        if (r.succeeded()) {
          List<JsonObject> resList = r.result().getRows();
          if (resList == null || resList.isEmpty()) {
            resultHandler.handle(Future.succeededFuture());
          } else {
            resultHandler.handle(Future.succeededFuture(new Payment(resList.get(0))));
          }
        } else {
          resultHandler.handle(Future.failedFuture(r.cause()));
        }
        connection.close();
      });
    }));
  }

  /**
   * A helper methods that generates async handler for SQLConnection
   *
   * @return generated handler
   */
  private <T> Handler<AsyncResult<SQLConnection>> connHandler(Handler<AsyncResult<T>> h1, Handler<SQLConnection> h2) {
    return conn -> {
      if (conn.succeeded()) {
        final SQLConnection connection = conn.result();
        h2.handle(connection);
      } else {
        h1.handle(Future.failedFuture(conn.cause()));
      }
    };
  }

}

/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.payment.Payment}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.payment.Payment} original class using Vert.x codegen.
 */
public class PaymentConverter {

  public static void fromJson(JsonObject json, Payment obj) {
    if (json.getValue("payAmount") instanceof Number) {
      obj.setPayAmount(((Number)json.getValue("payAmount")).doubleValue());
    }
    if (json.getValue("payId") instanceof String) {
      obj.setPayId((String)json.getValue("payId"));
    }
    if (json.getValue("paySource") instanceof Number) {
      obj.setPaySource(((Number)json.getValue("paySource")).shortValue());
    }
    if (json.getValue("paymentTime") instanceof Number) {
      obj.setPaymentTime(((Number)json.getValue("paymentTime")).longValue());
    }
  }

  public static void toJson(Payment obj, JsonObject json) {
    if (obj.getPayAmount() != null) {
      json.put("payAmount", obj.getPayAmount());
    }
    if (obj.getPayId() != null) {
      json.put("payId", obj.getPayId());
    }
    if (obj.getPaySource() != null) {
      json.put("paySource", obj.getPaySource());
    }
    if (obj.getPaymentTime() != null) {
      json.put("paymentTime", obj.getPaymentTime());
    }
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.payment.PaymentQueryService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.blueprint.microservice.payment.Payment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentQueryServiceVertxEBProxy implements PaymentQueryService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public PaymentQueryServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public PaymentQueryServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public void initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "initializePersistence");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }

  public void addPaymentRecord(Payment payment, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("payment", payment == null ? null : payment.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "addPaymentRecord");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }

  public void retrievePaymentRecord(String payId, Handler<AsyncResult<Payment>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("payId", payId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrievePaymentRecord");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Payment(res.result().body())));
                      }
    });
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.payment.PaymentQueryService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.blueprint.microservice.payment.Payment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentQueryServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final PaymentQueryService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public PaymentQueryServiceVertxProxyHandler(Vertx vertx, PaymentQueryService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public PaymentQueryServiceVertxProxyHandler(Vertx vertx, PaymentQueryService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public PaymentQueryServiceVertxProxyHandler(Vertx vertx, PaymentQueryService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "initializePersistence": {
          service.initializePersistence(createHandler(msg));
          break;
        }
        case "addPaymentRecord": {
          service.addPaymentRecord(json.getJsonObject("payment") == null ? null : new io.vertx.blueprint.microservice.payment.Payment(json.getJsonObject("payment")), createHandler(msg));
          break;
        }
        case "retrievePaymentRecord": {
          service.retrievePaymentRecord((java.lang.String)json.getValue("payId"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import io.vertx.blueprint.microservice.payment.Payment;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing payment transactions query.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.payment.PaymentQueryService original} non RX-ified interface using Vert.x codegen.
 */

public class PaymentQueryService {

  final io.vertx.blueprint.microservice.payment.PaymentQueryService delegate;

  public PaymentQueryService(io.vertx.blueprint.microservice.payment.PaymentQueryService delegate) {
    this.delegate = delegate;
  }

  public Object getDelegate() {
    return delegate;
  }

  /**
   * Initialize the persistence.
   * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates whether the operation was successful or not.
   */
  public void initializePersistence(Handler<AsyncResult<Void>> resultHandler) { 
    delegate.initializePersistence(resultHandler);
  }

  /**
   * Initialize the persistence.
   * @return 
   */
  public Observable<Void> initializePersistenceObservable() { 
    io.vertx.rx.java.ObservableFuture<Void> resultHandler = io.vertx.rx.java.RxHelper.observableFuture();
    initializePersistence(resultHandler.toHandler());
    return resultHandler;
  }

  /**
   * Add a payment record into the backend persistence.
   * @param payment payment entity
   * @param resultHandler async result handler
   */
  public void addPaymentRecord(Payment payment, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.addPaymentRecord(payment, resultHandler);
  }

  /**
   * Add a payment record into the backend persistence.
   * @param payment payment entity
   * @return 
   */
  public Observable<Void> addPaymentRecordObservable(Payment payment) { 
    io.vertx.rx.java.ObservableFuture<Void> resultHandler = io.vertx.rx.java.RxHelper.observableFuture();
    addPaymentRecord(payment, resultHandler.toHandler());
    return resultHandler;
  }

  /**
   * Retrieve payment record from backend by payment id.
   * @param payId payment id
   * @param resultHandler async result handler
   */
  public void retrievePaymentRecord(String payId, Handler<AsyncResult<Payment>> resultHandler) { 
    delegate.retrievePaymentRecord(payId, resultHandler);
  }

  /**
   * Retrieve payment record from backend by payment id.
   * @param payId payment id
   * @return 
   */
  public Observable<Payment> retrievePaymentRecordObservable(String payId) { 
    io.vertx.rx.java.ObservableFuture<Payment> resultHandler = io.vertx.rx.java.RxHelper.observableFuture();
    retrievePaymentRecord(payId, resultHandler.toHandler());
    return resultHandler;
  }


  public static PaymentQueryService newInstance(io.vertx.blueprint.microservice.payment.PaymentQueryService arg) {
    return arg != null ? new PaymentQueryService(arg) : null;
  }
}


import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface for online store CURD operation.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface StoreCRUDService {

  String SERVICE_NAME = "store-eb-service";

  String SERVICE_ADDRESS = "service.store";

  /**
   * Save an online store to the persistence layer. This is a so called `upsert` operation.
   * This is used to update store info, or just apply for a new store.
   *
   * @param store         store object
   * @param resultHandler async result handler
   */
  void saveStore(Store store, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Retrieve an online store by seller id.
   *
   * @param sellerId seller id, refers to an independent online store
   * @param resultHandler async result handler
   */
  void retrieveStore(String sellerId, Handler<AsyncResult<Store>> resultHandler);

  /**
   * Remove an online store whose seller is {@code sellerId}.
   * This is used to close an online store.
   *
   * @param sellerId seller id, refers to an independent online store
   * @param resultHandler async result handler
   */
  void removeStore(String sellerId, Handler<AsyncResult<Void>> resultHandler);

}


import io.vertx.blueprint.microservice.common.BaseMicroserviceVerticle;
import io.vertx.blueprint.microservice.store.api.RestStoreAPIVerticle;
import io.vertx.blueprint.microservice.store.impl.StoreCRUDServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

import static io.vertx.blueprint.microservice.store.StoreCRUDService.SERVICE_ADDRESS;
import static io.vertx.blueprint.microservice.store.StoreCRUDService.SERVICE_NAME;

/**
 * A verticle for store operation (e.g. apply or close) processing.
 *
 * @author Eric Zhao
 */
public class StoreVerticle extends BaseMicroserviceVerticle {

  private StoreCRUDService crudService;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    crudService = new StoreCRUDServiceImpl(vertx, config());
    ProxyHelper.registerService(StoreCRUDService.class, vertx, crudService, SERVICE_ADDRESS);
    // publish service and deploy REST verticle
    publishEventBusService(SERVICE_NAME, SERVICE_ADDRESS, StoreCRUDService.class)
      .compose(servicePublished -> deployRestVerticle(crudService))
      .setHandler(future.completer());
  }

  private Future<Void> deployRestVerticle(StoreCRUDService service) {
    Future<String> future = Future.future();
    vertx.deployVerticle(new RestStoreAPIVerticle(service),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }
}


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Online store data object.
 *
 * @author Eric Zhao
 */
@DataObject(generateConverter = true)
public class Store {

  private String sellerId;
  private String name;
  private String description;
  private Long openTime;

  public Store() {
    this.openTime = System.currentTimeMillis();
  }

  public Store(Store other) {
    this.openTime = other.openTime;
    this.description = other.description;
    this.name = other.name;
    this.sellerId = other.sellerId;
  }

  public Store(JsonObject json) {
    StoreConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    StoreConverter.toJson(this, json);
    return json;
  }

  public String getSellerId() {
    return sellerId;
  }

  public Store setSellerId(String sellerId) {
    this.sellerId = sellerId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Store setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Store setDescription(String description) {
    this.description = description;
    return this;
  }

  public Long getOpenTime() {
    return openTime;
  }

  public Store setOpenTime(Long openTime) {
    this.openTime = openTime;
    return this;
  }

  @Override
  public String toString() {
    return this.toJson().encodePrettily();
  }
}

/**
 * Indicates that this module contains classes that need to be generated / processed.
 */
@ModuleGen(name = "vertx-blueprint-store", groupPackage = "io.vertx.blueprint.microservice.store")


import io.vertx.codegen.annotations.ModuleGen;

import io.vertx.blueprint.microservice.common.RestAPIVerticle;
import io.vertx.blueprint.microservice.store.Store;
import io.vertx.blueprint.microservice.store.StoreCRUDService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * A verticle provides REST API for online store service.
 */
public class RestStoreAPIVerticle extends RestAPIVerticle {

  private static final String SERVICE_NAME = "shop-rest-api";

  private static final String API_SAVE = "/save";
  private static final String API_RETRIEVE = "/:sellerId";
  private static final String API_CLOSE = "/:sellerId";

  private final StoreCRUDService service;

  public RestStoreAPIVerticle(StoreCRUDService service) {
    this.service = service;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    Router router = Router.router(vertx);
    // body handler
    router.route().handler(BodyHandler.create());
    // API route handler
    router.post(API_SAVE).handler(this::apiSave);
    router.get(API_RETRIEVE).handler(this::apiRetrieve);
    router.delete(API_CLOSE).handler(this::apiClose);

    // get HTTP host and port from configuration, or use default value
    String host = config().getString("store.http.address", "0.0.0.0");
    int port = config().getInteger("store.http.port", 8085);

    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void apiSave(RoutingContext context) {
    Store store = new Store(new JsonObject(context.getBodyAsString()));
    if (store.getSellerId() == null) {
      badRequest(context, new IllegalStateException("Seller id does not exist"));
    } else {
      JsonObject result = new JsonObject().put("message", "store_saved")
        .put("sellerId", store.getSellerId());
      service.saveStore(store, resultVoidHandler(context, result));
    }
  }

  private void apiRetrieve(RoutingContext context) {
    String sellerId = context.request().getParam("sellerId");
    service.retrieveStore(sellerId, resultHandlerNonEmpty(context));
  }

  private void apiClose(RoutingContext context) {
    String sellerId = context.request().getParam("sellerId");
    service.removeStore(sellerId, deleteResultHandler(context));
  }
}


import io.vertx.blueprint.microservice.store.Store;
import io.vertx.blueprint.microservice.store.StoreCRUDService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

/**
 * Implementation of {@link StoreCRUDService}. Use MongoDB as the persistence.
 */
public class StoreCRUDServiceImpl implements StoreCRUDService {

  private static final String COLLECTION = "store";

  private final MongoClient client;

  public StoreCRUDServiceImpl(Vertx vertx, JsonObject config) {
    this.client = MongoClient.createNonShared(vertx, config);
  }

  @Override
  public void saveStore(Store store, Handler<AsyncResult<Void>> resultHandler) {
    client.save(COLLECTION, new JsonObject().put("_id", store.getSellerId())
        .put("name", store.getName())
        .put("description", store.getDescription())
        .put("openTime", store.getOpenTime()),
      ar -> {
        if (ar.succeeded()) {
          resultHandler.handle(Future.succeededFuture());
        } else {
          resultHandler.handle(Future.failedFuture(ar.cause()));
        }
      }
    );
  }

  @Override
  public void retrieveStore(String sellerId, Handler<AsyncResult<Store>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", sellerId);
    client.findOne(COLLECTION, query, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() == null) {
          resultHandler.handle(Future.succeededFuture());
        } else {
          Store store = new Store(ar.result().put("sellerId", ar.result().getString("_id")));
          resultHandler.handle(Future.succeededFuture(store));
        }
      } else {
        resultHandler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  @Override
  public void removeStore(String sellerId, Handler<AsyncResult<Void>> resultHandler) {
    JsonObject query = new JsonObject().put("_id", sellerId);
    client.removeDocument(COLLECTION, query, ar -> {
      if (ar.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        resultHandler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }
}

/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.store.StoreCRUDService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.store.Store;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class StoreCRUDServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final StoreCRUDService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public StoreCRUDServiceVertxProxyHandler(Vertx vertx, StoreCRUDService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public StoreCRUDServiceVertxProxyHandler(Vertx vertx, StoreCRUDService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public StoreCRUDServiceVertxProxyHandler(Vertx vertx, StoreCRUDService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "saveStore": {
          service.saveStore(json.getJsonObject("store") == null ? null : new io.vertx.blueprint.microservice.store.Store(json.getJsonObject("store")), createHandler(msg));
          break;
        }
        case "retrieveStore": {
          service.retrieveStore((java.lang.String)json.getValue("sellerId"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        case "removeStore": {
          service.removeStore((java.lang.String)json.getValue("sellerId"), createHandler(msg));
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.store.StoreCRUDService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.store.Store;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class StoreCRUDServiceVertxEBProxy implements StoreCRUDService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public StoreCRUDServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public StoreCRUDServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public void saveStore(Store store, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("store", store == null ? null : store.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "saveStore");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }

  public void retrieveStore(String sellerId, Handler<AsyncResult<Store>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("sellerId", sellerId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveStore");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Store(res.result().body())));
                      }
    });
  }

  public void removeStore(String sellerId, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("sellerId", sellerId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "removeStore");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.store.Store}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.store.Store} original class using Vert.x codegen.
 */
public class StoreConverter {

  public static void fromJson(JsonObject json, Store obj) {
    if (json.getValue("description") instanceof String) {
      obj.setDescription((String)json.getValue("description"));
    }
    if (json.getValue("name") instanceof String) {
      obj.setName((String)json.getValue("name"));
    }
    if (json.getValue("openTime") instanceof Number) {
      obj.setOpenTime(((Number)json.getValue("openTime")).longValue());
    }
    if (json.getValue("sellerId") instanceof String) {
      obj.setSellerId((String)json.getValue("sellerId"));
    }
  }

  public static void toJson(Store obj, JsonObject json) {
    if (obj.getDescription() != null) {
      json.put("description", obj.getDescription());
    }
    if (obj.getName() != null) {
      json.put("name", obj.getName());
    }
    if (obj.getOpenTime() != null) {
      json.put("openTime", obj.getOpenTime());
    }
    if (obj.getSellerId() != null) {
      json.put("sellerId", obj.getSellerId());
    }
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import rx.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.store.Store;

/**
 * A service interface for online store CURD operation.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.store.StoreCRUDService original} non RX-ified interface using Vert.x codegen.
 */

@io.vertx.lang.rxjava.RxGen(io.vertx.blueprint.microservice.store.StoreCRUDService.class)
public class StoreCRUDService {

  public static final io.vertx.lang.rxjava.TypeArg<StoreCRUDService> __TYPE_ARG = new io.vertx.lang.rxjava.TypeArg<>(
    obj -> new StoreCRUDService((io.vertx.blueprint.microservice.store.StoreCRUDService) obj),
    StoreCRUDService::getDelegate
  );

  private final io.vertx.blueprint.microservice.store.StoreCRUDService delegate;
  
  public StoreCRUDService(io.vertx.blueprint.microservice.store.StoreCRUDService delegate) {
    this.delegate = delegate;
  }

  public io.vertx.blueprint.microservice.store.StoreCRUDService getDelegate() {
    return delegate;
  }

  /**
   * Save an online store to the persistence layer. This is a so called `upsert` operation.
   * This is used to update store info, or just apply for a new store.
   * @param store store object
   * @param resultHandler async result handler
   */
  public void saveStore(Store store, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.saveStore(store, resultHandler);
  }

  /**
   * Save an online store to the persistence layer. This is a so called `upsert` operation.
   * This is used to update store info, or just apply for a new store.
   * @param store store object
   * @return 
   */
  public Single<Void> rxSaveStore(Store store) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      saveStore(store, fut);
    }));
  }

  /**
   * Retrieve an online store by seller id.
   * @param sellerId seller id, refers to an independent online store
   * @param resultHandler async result handler
   */
  public void retrieveStore(String sellerId, Handler<AsyncResult<Store>> resultHandler) { 
    delegate.retrieveStore(sellerId, resultHandler);
  }

  /**
   * Retrieve an online store by seller id.
   * @param sellerId seller id, refers to an independent online store
   * @return 
   */
  public Single<Store> rxRetrieveStore(String sellerId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveStore(sellerId, fut);
    }));
  }

  /**
   * Remove an online store whose seller is <code>sellerId</code>.
   * This is used to close an online store.
   * @param sellerId seller id, refers to an independent online store
   * @param resultHandler async result handler
   */
  public void removeStore(String sellerId, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.removeStore(sellerId, resultHandler);
  }

  /**
   * Remove an online store whose seller is <code>sellerId</code>.
   * This is used to close an online store.
   * @param sellerId seller id, refers to an independent online store
   * @return 
   */
  public Single<Void> rxRemoveStore(String sellerId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      removeStore(sellerId, fut);
    }));
  }


  public static StoreCRUDService newInstance(io.vertx.blueprint.microservice.store.StoreCRUDService arg) {
    return arg != null ? new StoreCRUDService(arg) : null;
  }
}


import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

/**
 * A service interface managing user accounts.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface AccountService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "user-account-eb-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.user.account";

  /**
   * Initialize the persistence.
   *
   * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

  /**
   * Add a account to the persistence.
   *
   * @param account       a account entity that we want to add
   * @param resultHandler the result handler will be called as soon as the account has been added. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService addAccount(Account account, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Retrieve the user account with certain `id`.
   *
   * @param id            user account id
   * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService retrieveAccount(String id, Handler<AsyncResult<Account>> resultHandler);

  /**
   * Retrieve the user account with certain `username`.
   *
   * @param username      username
   * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService retrieveByUsername(String username, Handler<AsyncResult<Account>> resultHandler);

  /**
   * Retrieve all user accounts.
   *
   * @param resultHandler the result handler will be called as soon as the users have been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService retrieveAllAccounts(Handler<AsyncResult<List<Account>>> resultHandler);

  /**
   * Update user account info.
   *
   * @param account       a account entity that we want to update
   * @param resultHandler the result handler will be called as soon as the account has been added. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService updateAccount(Account account, Handler<AsyncResult<Account>> resultHandler);

  /**
   * Delete a user account from the persistence
   *
   * @param id            user account id
   * @param resultHandler the result handler will be called as soon as the user has been removed. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService deleteAccount(String id, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Delete all user accounts from the persistence
   *
   * @param resultHandler the result handler will be called as soon as the users have been removed. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  AccountService deleteAllAccounts(Handler<AsyncResult<Void>> resultHandler);

}


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * User account data object
 *
 * @author Eric Zhao
 */
@DataObject(generateConverter = true)
public class Account {

  private String id;
  private String username;
  private String phone;
  private String email;
  private Long birthDate;

  public Account() {
    // Empty constructor
  }

  public Account(JsonObject json) {
    AccountConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    AccountConverter.toJson(this, json);
    return json;
  }


  public String getId() {
    return id;
  }

  public Account setId(String id) {
    this.id = id;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public Account setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public Account setPhone(String phone) {
    this.phone = phone;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public Account setEmail(String email) {
    this.email = email;
    return this;
  }

  public Long getBirthDate() {
    return birthDate;
  }

  public Account setBirthDate(Long birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  @Override
  public String toString() {
    return toJson().encodePrettily();
  }
}

/**
 * Indicates that this module contains classes that need to be generated / processed.
 */
@ModuleGen(name = "vertx-blueprint-user-account", groupPackage = "io.vertx.blueprint.microservice.account")

import io.vertx.codegen.annotations.ModuleGen;

import io.vertx.blueprint.microservice.account.api.RestUserAccountAPIVerticle;
import io.vertx.blueprint.microservice.account.impl.JdbcAccountServiceImpl;
import io.vertx.blueprint.microservice.common.BaseMicroserviceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

import static io.vertx.blueprint.microservice.account.AccountService.SERVICE_ADDRESS;
import static io.vertx.blueprint.microservice.account.AccountService.SERVICE_NAME;


/**
 * A verticle publishing the user service.
 *
 * @author Eric Zhao
 */
public class UserAccountVerticle extends BaseMicroserviceVerticle {

  private AccountService accountService;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    // create the service instance
    accountService = new JdbcAccountServiceImpl(vertx, config());
    // register the service proxy on event bus
    ProxyHelper.registerService(AccountService.class, vertx, accountService, SERVICE_ADDRESS);
    // publish the service and REST endpoint in the discovery infrastructure
    publishEventBusService(SERVICE_NAME, SERVICE_ADDRESS, AccountService.class)
      .compose(servicePublished -> deployRestVerticle())
      .setHandler(future.completer());
  }

  private Future<Void> deployRestVerticle() {
    Future<String> future = Future.future();
    vertx.deployVerticle(new RestUserAccountAPIVerticle(accountService),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }
}


import io.vertx.blueprint.microservice.account.Account;
import io.vertx.blueprint.microservice.account.AccountService;
import io.vertx.blueprint.microservice.common.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * This verticle exposes a HTTP endpoint to process user data via REST API.
 *
 * @author Eric Zhao
 */
public class RestUserAccountAPIVerticle extends RestAPIVerticle {

  private static final String SERVICE_NAME = "user-account-rest-api";

  private final AccountService accountService;

  private static final String API_ADD = "/user";
  private static final String API_RETRIEVE = "/user/:id";
  private static final String API_RETRIEVE_ALL = "/user";
  private static final String API_UPDATE = "/user/:id";
  private static final String API_DELETE = "/user/:id";

  public RestUserAccountAPIVerticle(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    final Router router = Router.router(vertx);
    // body handler
    router.route().handler(BodyHandler.create());
    // api route handler
    router.post(API_ADD).handler(this::apiAddUser);
    router.get(API_RETRIEVE).handler(this::apiRetrieveUser);
    router.get(API_RETRIEVE_ALL).handler(this::apiRetrieveAll);
    router.patch(API_UPDATE).handler(this::apiUpdateUser);
    router.delete(API_DELETE).handler(this::apiDeleteUser);

    String host = config().getString("user.account.http.address", "0.0.0.0");
    int port = config().getInteger("user.account.http.port", 8081);

    // create HTTP server and publish REST service
    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void apiAddUser(RoutingContext context) {
    Account account = new Account(context.getBodyAsJson());
    accountService.addAccount(account, resultVoidHandler(context, 201));
  }

  private void apiRetrieveUser(RoutingContext context) {
    String id = context.request().getParam("id");
    accountService.retrieveAccount(id, resultHandlerNonEmpty(context));
  }

  private void apiRetrieveAll(RoutingContext context) {
    accountService.retrieveAllAccounts(resultHandler(context, Json::encodePrettily));
  }

  private void apiUpdateUser(RoutingContext context) {
    notImplemented(context);
  }

  private void apiDeleteUser(RoutingContext context) {
    String id = context.request().getParam("id");
    accountService.deleteAccount(id, deleteResultHandler(context));
  }

}


import io.vertx.blueprint.microservice.account.Account;
import io.vertx.blueprint.microservice.account.AccountService;
import io.vertx.blueprint.microservice.common.service.JdbcRepositoryWrapper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JDBC implementation of {@link AccountService}.
 *
 * @author Eric Zhao
 */
public class JdbcAccountServiceImpl extends JdbcRepositoryWrapper implements AccountService {

  public JdbcAccountServiceImpl(Vertx vertx, JsonObject config) {
    super(vertx, config);
  }

  @Override
  public AccountService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    client.getConnection(connHandler(resultHandler, connection -> {
      connection.execute(CREATE_STATEMENT, r -> {
        resultHandler.handle(r);
        connection.close();
      });
    }));
    return this;
  }

  @Override
  public AccountService addAccount(Account account, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(account.getId())
      .add(account.getUsername())
      .add(account.getPhone())
      .add(account.getEmail())
      .add(account.getBirthDate());
    this.executeNoResult(params, INSERT_STATEMENT, resultHandler);
    return this;
  }

  @Override
  public AccountService retrieveAccount(String id, Handler<AsyncResult<Account>> resultHandler) {
    this.retrieveOne(id, FETCH_STATEMENT)
      .map(option -> option.map(Account::new).orElse(null))
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public AccountService retrieveByUsername(String username, Handler<AsyncResult<Account>> resultHandler) {
    this.retrieveOne(username, FETCH_BY_USERNAME_STATEMENT)
      .map(option -> option.map(Account::new).orElse(null))
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public AccountService retrieveAllAccounts(Handler<AsyncResult<List<Account>>> resultHandler) {
    this.retrieveAll(FETCH_ALL_STATEMENT)
      .map(rawList -> rawList.stream()
        .map(Account::new)
        .collect(Collectors.toList())
      )
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public AccountService updateAccount(Account account, Handler<AsyncResult<Account>> resultHandler) {
    JsonArray params = new JsonArray()
      .add(account.getUsername())
      .add(account.getPhone())
      .add(account.getEmail())
      .add(account.getBirthDate())
      .add(account.getId());
    this.execute(params, UPDATE_STATEMENT, account, resultHandler);
    return this;
  }

  @Override
  public AccountService deleteAccount(String id, Handler<AsyncResult<Void>> resultHandler) {
    this.removeOne(id, DELETE_STATEMENT, resultHandler);
    return this;
  }

  @Override
  public AccountService deleteAllAccounts(Handler<AsyncResult<Void>> resultHandler) {
    this.removeAll(DELETE_ALL_STATEMENT, resultHandler);
    return this;
  }

  // SQL statement

  private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `user_account` (\n" +
    "  `id` varchar(30) NOT NULL,\n" +
    "  `username` varchar(20) NOT NULL,\n" +
    "  `phone` varchar(20) NOT NULL,\n" +
    "  `email` varchar(45) NOT NULL,\n" +
    "  `birthDate` bigint(20) NOT NULL,\n" +
    "  PRIMARY KEY (`id`),\n" +
    "  UNIQUE KEY `username_UNIQUE` (`username`) )";
  private static final String INSERT_STATEMENT = "INSERT INTO user_account (id, username, phone, email, birthDate) VALUES (?, ?, ?, ?, ?)";
  private static final String EXISTS_STATEMENT = "SELECT EXISTS(1) FROM user_account WHERE username = ?";
  private static final String FETCH_STATEMENT = "SELECT * FROM user_account WHERE id = ?";
  private static final String FETCH_BY_USERNAME_STATEMENT = "SELECT * FROM user_account WHERE username = ?";
  private static final String FETCH_ALL_STATEMENT = "SELECT * FROM user_account";
  private static final String UPDATE_STATEMENT = "UPDATE `user_account`\n" +
    "SET `username` = ?,\n" +
    "`phone` = ?,\n" +
    "`email` = ?,\n" +
    "`birthDate` = ? \n" +
    "WHERE `id` = ?";
  private static final String DELETE_STATEMENT = "DELETE FROM user_account WHERE id = ?";
  private static final String DELETE_ALL_STATEMENT = "DELETE FROM user_account";

}

/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.account.AccountService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import java.util.List;
import io.vertx.core.AsyncResult;
import io.vertx.blueprint.microservice.account.Account;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.account.AccountService;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class AccountServiceVertxEBProxy implements AccountService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public AccountServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public AccountServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public AccountService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "initializePersistence");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AccountService addAccount(Account account, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("account", account == null ? null : account.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "addAccount");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AccountService retrieveAccount(String id, Handler<AsyncResult<Account>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("id", id);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveAccount");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Account(res.result().body())));
                      }
    });
    return this;
  }

  public AccountService retrieveByUsername(String username, Handler<AsyncResult<Account>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("username", username);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveByUsername");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Account(res.result().body())));
                      }
    });
    return this;
  }

  public AccountService retrieveAllAccounts(Handler<AsyncResult<List<Account>>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveAllAccounts");
    _vertx.eventBus().<JsonArray>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body().stream().map(o -> o instanceof Map ? new Account(new JsonObject((Map) o)) : new Account((JsonObject) o)).collect(Collectors.toList())));
      }
    });
    return this;
  }

  public AccountService updateAccount(Account account, Handler<AsyncResult<Account>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("account", account == null ? null : account.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "updateAccount");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Account(res.result().body())));
                      }
    });
    return this;
  }

  public AccountService deleteAccount(String id, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("id", id);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "deleteAccount");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public AccountService deleteAllAccounts(Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "deleteAllAccounts");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.account.AccountService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import java.util.List;
import io.vertx.core.AsyncResult;
import io.vertx.blueprint.microservice.account.Account;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.account.AccountService;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class AccountServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final AccountService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public AccountServiceVertxProxyHandler(Vertx vertx, AccountService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public AccountServiceVertxProxyHandler(Vertx vertx, AccountService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public AccountServiceVertxProxyHandler(Vertx vertx, AccountService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "initializePersistence": {
          service.initializePersistence(createHandler(msg));
          break;
        }
        case "addAccount": {
          service.addAccount(json.getJsonObject("account") == null ? null : new io.vertx.blueprint.microservice.account.Account(json.getJsonObject("account")), createHandler(msg));
          break;
        }
        case "retrieveAccount": {
          service.retrieveAccount((java.lang.String)json.getValue("id"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        case "retrieveByUsername": {
          service.retrieveByUsername((java.lang.String)json.getValue("username"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        case "retrieveAllAccounts": {
          service.retrieveAllAccounts(res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(new JsonArray(res.result().stream().map(Account::toJson).collect(Collectors.toList())));
            }
         });
          break;
        }
        case "updateAccount": {
          service.updateAccount(json.getJsonObject("account") == null ? null : new io.vertx.blueprint.microservice.account.Account(json.getJsonObject("account")), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        case "deleteAccount": {
          service.deleteAccount((java.lang.String)json.getValue("id"), createHandler(msg));
          break;
        }
        case "deleteAllAccounts": {
          service.deleteAllAccounts(createHandler(msg));
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.account.Account}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.account.Account} original class using Vert.x codegen.
 */
public class AccountConverter {

  public static void fromJson(JsonObject json, Account obj) {
    if (json.getValue("birthDate") instanceof Number) {
      obj.setBirthDate(((Number)json.getValue("birthDate")).longValue());
    }
    if (json.getValue("email") instanceof String) {
      obj.setEmail((String)json.getValue("email"));
    }
    if (json.getValue("id") instanceof String) {
      obj.setId((String)json.getValue("id"));
    }
    if (json.getValue("phone") instanceof String) {
      obj.setPhone((String)json.getValue("phone"));
    }
    if (json.getValue("username") instanceof String) {
      obj.setUsername((String)json.getValue("username"));
    }
  }

  public static void toJson(Account obj, JsonObject json) {
    if (obj.getBirthDate() != null) {
      json.put("birthDate", obj.getBirthDate());
    }
    if (obj.getEmail() != null) {
      json.put("email", obj.getEmail());
    }
    if (obj.getId() != null) {
      json.put("id", obj.getId());
    }
    if (obj.getPhone() != null) {
      json.put("phone", obj.getPhone());
    }
    if (obj.getUsername() != null) {
      json.put("username", obj.getUsername());
    }
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import rx.Single;
import java.util.List;
import io.vertx.core.AsyncResult;
import io.vertx.blueprint.microservice.account.Account;
import io.vertx.core.Handler;

/**
 * A service interface managing user accounts.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.account.AccountService original} non RX-ified interface using Vert.x codegen.
 */

@io.vertx.lang.rxjava.RxGen(io.vertx.blueprint.microservice.account.AccountService.class)
public class AccountService {

  public static final io.vertx.lang.rxjava.TypeArg<AccountService> __TYPE_ARG = new io.vertx.lang.rxjava.TypeArg<>(
    obj -> new AccountService((io.vertx.blueprint.microservice.account.AccountService) obj),
    AccountService::getDelegate
  );

  private final io.vertx.blueprint.microservice.account.AccountService delegate;
  
  public AccountService(io.vertx.blueprint.microservice.account.AccountService delegate) {
    this.delegate = delegate;
  }

  public io.vertx.blueprint.microservice.account.AccountService getDelegate() {
    return delegate;
  }

  /**
   * Initialize the persistence.
   * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService initializePersistence(Handler<AsyncResult<Void>> resultHandler) { 
    delegate.initializePersistence(resultHandler);
    return this;
  }

  /**
   * Initialize the persistence.
   * @return 
   */
  public Single<Void> rxInitializePersistence() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      initializePersistence(fut);
    }));
  }

  /**
   * Add a account to the persistence.
   * @param account a account entity that we want to add
   * @param resultHandler the result handler will be called as soon as the account has been added. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService addAccount(Account account, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.addAccount(account, resultHandler);
    return this;
  }

  /**
   * Add a account to the persistence.
   * @param account a account entity that we want to add
   * @return 
   */
  public Single<Void> rxAddAccount(Account account) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      addAccount(account, fut);
    }));
  }

  /**
   * Retrieve the user account with certain `id`.
   * @param id user account id
   * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService retrieveAccount(String id, Handler<AsyncResult<Account>> resultHandler) { 
    delegate.retrieveAccount(id, resultHandler);
    return this;
  }

  /**
   * Retrieve the user account with certain `id`.
   * @param id user account id
   * @return 
   */
  public Single<Account> rxRetrieveAccount(String id) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveAccount(id, fut);
    }));
  }

  /**
   * Retrieve the user account with certain `username`.
   * @param username username
   * @param resultHandler the result handler will be called as soon as the user has been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService retrieveByUsername(String username, Handler<AsyncResult<Account>> resultHandler) { 
    delegate.retrieveByUsername(username, resultHandler);
    return this;
  }

  /**
   * Retrieve the user account with certain `username`.
   * @param username username
   * @return 
   */
  public Single<Account> rxRetrieveByUsername(String username) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveByUsername(username, fut);
    }));
  }

  /**
   * Retrieve all user accounts.
   * @param resultHandler the result handler will be called as soon as the users have been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService retrieveAllAccounts(Handler<AsyncResult<List<Account>>> resultHandler) { 
    delegate.retrieveAllAccounts(resultHandler);
    return this;
  }

  /**
   * Retrieve all user accounts.
   * @return 
   */
  public Single<List<Account>> rxRetrieveAllAccounts() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveAllAccounts(fut);
    }));
  }

  /**
   * Update user account info.
   * @param account a account entity that we want to update
   * @param resultHandler the result handler will be called as soon as the account has been added. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService updateAccount(Account account, Handler<AsyncResult<Account>> resultHandler) { 
    delegate.updateAccount(account, resultHandler);
    return this;
  }

  /**
   * Update user account info.
   * @param account a account entity that we want to update
   * @return 
   */
  public Single<Account> rxUpdateAccount(Account account) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      updateAccount(account, fut);
    }));
  }

  /**
   * Delete a user account from the persistence
   * @param id user account id
   * @param resultHandler the result handler will be called as soon as the user has been removed. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService deleteAccount(String id, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.deleteAccount(id, resultHandler);
    return this;
  }

  /**
   * Delete a user account from the persistence
   * @param id user account id
   * @return 
   */
  public Single<Void> rxDeleteAccount(String id) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      deleteAccount(id, fut);
    }));
  }

  /**
   * Delete all user accounts from the persistence
   * @param resultHandler the result handler will be called as soon as the users have been removed. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public AccountService deleteAllAccounts(Handler<AsyncResult<Void>> resultHandler) { 
    delegate.deleteAllAccounts(resultHandler);
    return this;
  }

  /**
   * Delete all user accounts from the persistence
   * @return 
   */
  public Single<Void> rxDeleteAllAccounts() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      deleteAllAccounts(fut);
    }));
  }


  public static AccountService newInstance(io.vertx.blueprint.microservice.account.AccountService arg) {
    return arg != null ? new AccountService(arg) : null;
  }
}


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * A product tuple represents the amount of a certain product in a shopping.
 */
@DataObject(generateConverter = true)
public class ProductTuple /*extends Tuple4<String, String, Double, Integer>*/ {

  private String productId;
  private String sellerId;
  private Double price;
  private Integer amount;

  public ProductTuple() {
    // empty constructor
  }

  public ProductTuple(String productId, String sellerId, Double price, Integer amount) {
    this.productId = productId;
    this.sellerId = sellerId;
    this.price = price;
    this.amount = amount;
  }

  public ProductTuple(Product product, Integer amount) {
    this.productId = product.getProductId();
    this.sellerId = product.getSellerId();
    this.price = product.getPrice();
    this.amount = amount;
  }

  public ProductTuple(JsonObject json) {
    ProductTupleConverter.fromJson(json, this);
  }

  public ProductTuple(ProductTuple other) {
    this.productId = other.productId;
    this.sellerId = other.sellerId;
    this.price = other.price;
    this.amount = other.amount;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ProductTupleConverter.toJson(this, json);
    return json;
  }

  public String getProductId() {
    return productId;
  }

  public ProductTuple setProductId(String productId) {
    this.productId = productId;
    return this;
  }

  public String getSellerId() {
    return sellerId;
  }

  public ProductTuple setSellerId(String sellerId) {
    this.sellerId = sellerId;
    return this;
  }

  public Double getPrice() {
    return price;
  }

  public ProductTuple setPrice(Double price) {
    this.price = price;
    return this;
  }

  public Integer getAmount() {
    return amount;
  }

  public ProductTuple setAmount(Integer amount) {
    this.amount = amount;
    return this;
  }

  @Override
  public String toString() {
    return "(" + productId + "," + sellerId + "," + amount + ")";
  }
}


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Product data object.
 *
 * @author Eric Zhao
 */
@DataObject(generateConverter = true)
public class Product {

  private String productId;
  private String sellerId;
  private String name;
  private double price = 0.0d;
  private String illustration;
  private String type;

  public Product() {
    // Empty constructor
  }

  public Product(Product other) {
    this.productId = other.productId;
    this.sellerId = other.sellerId;
    this.name = other.name;
    this.price = other.price;
    this.illustration = other.illustration;
    this.type = other.type;
  }

  public Product(JsonObject json) {
    ProductConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ProductConverter.toJson(this, json);
    return json;
  }

  public String getProductId() {
    return productId;
  }

  public Product setProductId(String productId) {
    this.productId = productId;
    return this;
  }

  public String getSellerId() {
    return sellerId;
  }

  public Product setSellerId(String sellerId) {
    this.sellerId = sellerId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Product setName(String name) {
    this.name = name;
    return this;
  }

  public double getPrice() {
    return price;
  }

  public Product setPrice(double price) {
    this.price = price;
    return this;
  }

  public String getIllustration() {
    return illustration;
  }

  public Product setIllustration(String illustration) {
    this.illustration = illustration;
    return this;
  }

  public String getType() {
    return type;
  }

  public Product setType(String type) {
    this.type = type;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Product product = (Product) o;

    return productId.equals(product.productId) && sellerId.equals(product.sellerId);
  }

  @Override
  public int hashCode() {
    int result = productId.hashCode();
    result = 31 * result + sellerId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return this.toJson().encodePrettily();
  }
}


import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * A service interface managing products.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface ProductService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "product-eb-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.product";

  /**
   * A static method that creates a product service.
   *
   * @param config a json object for configuration
   * @return initialized product service
   */
  // static ProductService createService(Vertx vertx, JsonObject config)

  /**
   * Initialize the persistence.
   *
   * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

  /**
   * Add a product to the persistence.
   *
   * @param product       a product entity that we want to add
   * @param resultHandler the result handler will be called as soon as the product has been added. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService addProduct(Product product, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Retrieve the product with certain `productId`.
   *
   * @param productId     product id
   * @param resultHandler the result handler will be called as soon as the product has been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService retrieveProduct(String productId, Handler<AsyncResult<Product>> resultHandler);

  /**
   * Retrieve the product price with certain `productId`.
   *
   * @param productId     product id
   * @param resultHandler the result handler will be called as soon as the product has been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService retrieveProductPrice(String productId, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   * Retrieve all products.
   *
   * @param resultHandler the result handler will be called as soon as the products have been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService retrieveAllProducts(Handler<AsyncResult<List<Product>>> resultHandler);

  /**
   * Retrieve products by page.
   *
   * @param resultHandler the result handler will be called as soon as the products have been retrieved. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService retrieveProductsByPage(int page, Handler<AsyncResult<List<Product>>> resultHandler);

  /**
   * Delete a product from the persistence
   *
   * @param productId     product id
   * @param resultHandler the result handler will be called as soon as the product has been removed. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService deleteProduct(String productId, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Delete all products from the persistence
   *
   * @param resultHandler the result handler will be called as soon as the products have been removed. The async result indicates
   *                      whether the operation was successful or not.
   */
  @Fluent
  ProductService deleteAllProducts(Handler<AsyncResult<Void>> resultHandler);

}

/**
 * Indicates that this module contains classes that need to be generated / processed.
 */
@ModuleGen(name = "vertx-blueprint-product", groupPackage = "io.vertx.blueprint.microservice.product")


import io.vertx.codegen.annotations.ModuleGen;

import io.vertx.blueprint.microservice.common.BaseMicroserviceVerticle;
import io.vertx.blueprint.microservice.common.service.ExampleHelper;
import io.vertx.blueprint.microservice.product.api.RestProductAPIVerticle;
import io.vertx.blueprint.microservice.product.impl.ProductServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

import static io.vertx.blueprint.microservice.product.ProductService.SERVICE_ADDRESS;


/**
 * A verticle publishing the product service.
 *
 * @author Eric Zhao
 */
public class ProductVerticle extends BaseMicroserviceVerticle {

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    // create the service instance
    ProductService productService = new ProductServiceImpl(vertx, config());
    // register the service proxy on event bus
    ProxyHelper.registerService(ProductService.class, vertx, productService, SERVICE_ADDRESS);
    // publish the service in the discovery infrastructure
    initProductDatabase(productService)
      .compose(databaseOkay -> publishEventBusService(ProductService.SERVICE_NAME, SERVICE_ADDRESS, ProductService.class))
      .compose(servicePublished -> deployRestService(productService))
      .setHandler(future.completer());
  }

  private Future<Void> initProductDatabase(ProductService service) {
    Future<Void> initFuture = Future.future();
    service.initializePersistence(initFuture.completer());
    return initFuture.map(v -> {
      ExampleHelper.initData(vertx, config());
      return null;
    });
  }

  private Future<Void> deployRestService(ProductService service) {
    Future<String> future = Future.future();
    vertx.deployVerticle(new RestProductAPIVerticle(service),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }

}


import io.vertx.blueprint.microservice.common.RestAPIVerticle;
import io.vertx.blueprint.microservice.product.Product;
import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.core.Future;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;


/**
 * This verticle exposes a HTTP endpoint to process shopping products management with REST APIs.
 *
 * @author Eric Zhao
 */
public class RestProductAPIVerticle extends RestAPIVerticle {

  public static final String SERVICE_NAME = "product-rest-api";

  private static final String API_ADD = "/add";
  private static final String API_RETRIEVE_BY_PAGE = "/products";
  private static final String API_RETRIEVE_ALL = "/products";
  private static final String API_RETRIEVE_PRICE = "/:productId/price";
  private static final String API_RETRIEVE = "/:productId";
  private static final String API_UPDATE = "/:productId";
  private static final String API_DELETE = "/:productId";
  private static final String API_DELETE_ALL = "/all";

  private final ProductService service;

  public RestProductAPIVerticle(ProductService service) {
    this.service = service;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    final Router router = Router.router(vertx);
    // body handler
    router.route().handler(BodyHandler.create());
    // API route handler
    router.post(API_ADD).handler(this::apiAdd);
    router.get(API_RETRIEVE_BY_PAGE).handler(this::apiRetrieveByPage);
    router.get(API_RETRIEVE_ALL).handler(this::apiRetrieveAll);
    router.get(API_RETRIEVE_PRICE).handler(this::apiRetrievePrice);
    router.get(API_RETRIEVE).handler(this::apiRetrieve);
    router.patch(API_UPDATE).handler(this::apiUpdate);
    router.delete(API_DELETE).handler(this::apiDelete);
    router.delete(API_DELETE_ALL).handler(context -> requireLogin(context, this::apiDeleteAll));

    // get HTTP host and port from configuration, or use default value
    String host = config().getString("product.http.address", "0.0.0.0");
    int port = config().getInteger("product.http.port", 8082);

    // create HTTP server and publish REST service
    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void apiAdd(RoutingContext context) {
    try {
      Product product = new Product(new JsonObject(context.getBodyAsString()));
      service.addProduct(product, resultHandler(context, r -> {
        String result = new JsonObject().put("message", "product_added")
          .put("productId", product.getProductId())
          .encodePrettily();
        context.response().setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(result);
      }));
    } catch (DecodeException e) {
      badRequest(context, e);
    }
  }

  private void apiRetrieve(RoutingContext context) {
    String productId = context.request().getParam("productId");
    service.retrieveProduct(productId, resultHandlerNonEmpty(context));
  }

  private void apiRetrievePrice(RoutingContext context) {
    String productId = context.request().getParam("productId");
    service.retrieveProductPrice(productId, resultHandlerNonEmpty(context));
  }

  private void apiRetrieveByPage(RoutingContext context) {
    try {
      String p = context.request().getParam("p");
      int page = p == null ? 1 : Integer.parseInt(p);
      service.retrieveProductsByPage(page, resultHandler(context, Json::encodePrettily));
    } catch (Exception ex) {
      badRequest(context, ex);
    }
  }

  private void apiRetrieveAll(RoutingContext context) {
    service.retrieveAllProducts(resultHandler(context, Json::encodePrettily));
  }

  private void apiUpdate(RoutingContext context) {
    notImplemented(context);
  }

  private void apiDelete(RoutingContext context) {
    String productId = context.request().getParam("productId");
    service.deleteProduct(productId, deleteResultHandler(context));
  }

  private void apiDeleteAll(RoutingContext context, JsonObject principle) {
    service.deleteAllProducts(deleteResultHandler(context));
  }

}


import io.vertx.blueprint.microservice.common.service.JdbcRepositoryWrapper;
import io.vertx.blueprint.microservice.product.Product;
import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JDBC implementation of {@link io.vertx.blueprint.microservice.product.ProductService}.
 *
 * @author Eric Zhao
 */
public class ProductServiceImpl extends JdbcRepositoryWrapper implements ProductService {

  private static final int PAGE_LIMIT = 10;

  public ProductServiceImpl(Vertx vertx, JsonObject config) {
    super(vertx, config);
  }

  @Override
  public ProductService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    client.getConnection(connHandler(resultHandler, connection -> {
      connection.execute(CREATE_STATEMENT, r -> {
        resultHandler.handle(r);
        connection.close();
      });
    }));
    return this;
  }

  @Override
  public ProductService addProduct(Product product, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray()
      .add(product.getProductId())
      .add(product.getSellerId())
      .add(product.getName())
      .add(product.getPrice())
      .add(product.getIllustration())
      .add(product.getType());
    executeNoResult(params, INSERT_STATEMENT, resultHandler);
    return this;
  }

  @Override
  public ProductService retrieveProduct(String productId, Handler<AsyncResult<Product>> resultHandler) {
    this.retrieveOne(productId, FETCH_STATEMENT)
      .map(option -> option.map(Product::new).orElse(null))
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public ProductService retrieveProductPrice(String productId, Handler<AsyncResult<JsonObject>> resultHandler) {
    this.retrieveOne(productId, "SELECT price FROM product WHERE productId = ?")
      .map(option -> option.orElse(null))
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public ProductService retrieveProductsByPage(int page, Handler<AsyncResult<List<Product>>> resultHandler) {
    this.retrieveByPage(page, PAGE_LIMIT, FETCH_WITH_PAGE_STATEMENT)
      .map(rawList -> rawList.stream()
        .map(Product::new)
        .collect(Collectors.toList())
      )
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public ProductService retrieveAllProducts(Handler<AsyncResult<List<Product>>> resultHandler) {
    this.retrieveAll(FETCH_ALL_STATEMENT)
      .map(rawList -> rawList.stream()
        .map(Product::new)
        .collect(Collectors.toList())
      )
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public ProductService deleteProduct(String productId, Handler<AsyncResult<Void>> resultHandler) {
    this.removeOne(productId, DELETE_STATEMENT, resultHandler);
    return this;
  }

  @Override
  public ProductService deleteAllProducts(Handler<AsyncResult<Void>> resultHandler) {
    this.removeAll(DELETE_ALL_STATEMENT, resultHandler);
    return this;
  }

  // SQL statements

  private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `product` (\n" +
    "  `productId` VARCHAR(60) NOT NULL,\n" +
    "  `sellerId` varchar(30) NOT NULL,\n" +
    "  `name` varchar(255) NOT NULL,\n" +
    "  `price` double NOT NULL,\n" +
    "  `illustration` MEDIUMTEXT NOT NULL,\n" +
    "  `type` varchar(45) NOT NULL,\n" +
    "  PRIMARY KEY (`productId`),\n" +
    "  KEY `index_seller` (`sellerId`) )";
  private static final String INSERT_STATEMENT = "INSERT INTO product (`productId`, `sellerId`, `name`, `price`, `illustration`, `type`) VALUES (?, ?, ?, ?, ?, ?)";
  private static final String FETCH_STATEMENT = "SELECT * FROM product WHERE productId = ?";
  private static final String FETCH_ALL_STATEMENT = "SELECT * FROM product";
  private static final String FETCH_WITH_PAGE_STATEMENT = "SELECT * FROM product LIMIT ?, ?";
  private static final String DELETE_STATEMENT = "DELETE FROM product WHERE productId = ?";
  private static final String DELETE_ALL_STATEMENT = "DELETE FROM product";
}

/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.product.Product}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.product.Product} original class using Vert.x codegen.
 */
public class ProductConverter {

  public static void fromJson(JsonObject json, Product obj) {
    if (json.getValue("illustration") instanceof String) {
      obj.setIllustration((String)json.getValue("illustration"));
    }
    if (json.getValue("name") instanceof String) {
      obj.setName((String)json.getValue("name"));
    }
    if (json.getValue("price") instanceof Number) {
      obj.setPrice(((Number)json.getValue("price")).doubleValue());
    }
    if (json.getValue("productId") instanceof String) {
      obj.setProductId((String)json.getValue("productId"));
    }
    if (json.getValue("sellerId") instanceof String) {
      obj.setSellerId((String)json.getValue("sellerId"));
    }
    if (json.getValue("type") instanceof String) {
      obj.setType((String)json.getValue("type"));
    }
  }

  public static void toJson(Product obj, JsonObject json) {
    if (obj.getIllustration() != null) {
      json.put("illustration", obj.getIllustration());
    }
    if (obj.getName() != null) {
      json.put("name", obj.getName());
    }
    json.put("price", obj.getPrice());
    if (obj.getProductId() != null) {
      json.put("productId", obj.getProductId());
    }
    if (obj.getSellerId() != null) {
      json.put("sellerId", obj.getSellerId());
    }
    if (obj.getType() != null) {
      json.put("type", obj.getType());
    }
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import java.util.List;
import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.product.Product;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProductServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final ProductService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public ProductServiceVertxProxyHandler(Vertx vertx, ProductService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public ProductServiceVertxProxyHandler(Vertx vertx, ProductService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public ProductServiceVertxProxyHandler(Vertx vertx, ProductService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "initializePersistence": {
          service.initializePersistence(createHandler(msg));
          break;
        }
        case "addProduct": {
          service.addProduct(json.getJsonObject("product") == null ? null : new io.vertx.blueprint.microservice.product.Product(json.getJsonObject("product")), createHandler(msg));
          break;
        }
        case "retrieveProduct": {
          service.retrieveProduct((java.lang.String)json.getValue("productId"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        case "retrieveProductPrice": {
          service.retrieveProductPrice((java.lang.String)json.getValue("productId"), createHandler(msg));
          break;
        }
        case "retrieveAllProducts": {
          service.retrieveAllProducts(res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(new JsonArray(res.result().stream().map(Product::toJson).collect(Collectors.toList())));
            }
         });
          break;
        }
        case "retrieveProductsByPage": {
          service.retrieveProductsByPage(json.getValue("page") == null ? null : (json.getLong("page").intValue()), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(new JsonArray(res.result().stream().map(Product::toJson).collect(Collectors.toList())));
            }
         });
          break;
        }
        case "deleteProduct": {
          service.deleteProduct((java.lang.String)json.getValue("productId"), createHandler(msg));
          break;
        }
        case "deleteAllProducts": {
          service.deleteAllProducts(createHandler(msg));
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import java.util.List;
import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.product.Product;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class ProductServiceVertxEBProxy implements ProductService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public ProductServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public ProductServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public ProductService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "initializePersistence");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public ProductService addProduct(Product product, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("product", product == null ? null : product.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "addProduct");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public ProductService retrieveProduct(String productId, Handler<AsyncResult<Product>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("productId", productId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveProduct");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Product(res.result().body())));
                      }
    });
    return this;
  }

  public ProductService retrieveProductPrice(String productId, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("productId", productId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveProductPrice");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public ProductService retrieveAllProducts(Handler<AsyncResult<List<Product>>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveAllProducts");
    _vertx.eventBus().<JsonArray>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body().stream().map(o -> o instanceof Map ? new Product(new JsonObject((Map) o)) : new Product((JsonObject) o)).collect(Collectors.toList())));
      }
    });
    return this;
  }

  public ProductService retrieveProductsByPage(int page, Handler<AsyncResult<List<Product>>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("page", page);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveProductsByPage");
    _vertx.eventBus().<JsonArray>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body().stream().map(o -> o instanceof Map ? new Product(new JsonObject((Map) o)) : new Product((JsonObject) o)).collect(Collectors.toList())));
      }
    });
    return this;
  }

  public ProductService deleteProduct(String productId, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("productId", productId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "deleteProduct");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public ProductService deleteAllProducts(Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "deleteAllProducts");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.product.ProductTuple}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.product.ProductTuple} original class using Vert.x codegen.
 */
public class ProductTupleConverter {

  public static void fromJson(JsonObject json, ProductTuple obj) {
    if (json.getValue("amount") instanceof Number) {
      obj.setAmount(((Number)json.getValue("amount")).intValue());
    }
    if (json.getValue("price") instanceof Number) {
      obj.setPrice(((Number)json.getValue("price")).doubleValue());
    }
    if (json.getValue("productId") instanceof String) {
      obj.setProductId((String)json.getValue("productId"));
    }
    if (json.getValue("sellerId") instanceof String) {
      obj.setSellerId((String)json.getValue("sellerId"));
    }
  }

  public static void toJson(ProductTuple obj, JsonObject json) {
    if (obj.getAmount() != null) {
      json.put("amount", obj.getAmount());
    }
    if (obj.getPrice() != null) {
      json.put("price", obj.getPrice());
    }
    if (obj.getProductId() != null) {
      json.put("productId", obj.getProductId());
    }
    if (obj.getSellerId() != null) {
      json.put("sellerId", obj.getSellerId());
    }
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import rx.Single;
import java.util.List;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.product.Product;

/**
 * A service interface managing products.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.product.ProductService original} non RX-ified interface using Vert.x codegen.
 */

@io.vertx.lang.rxjava.RxGen(io.vertx.blueprint.microservice.product.ProductService.class)
public class ProductService {

  public static final io.vertx.lang.rxjava.TypeArg<ProductService> __TYPE_ARG = new io.vertx.lang.rxjava.TypeArg<>(
    obj -> new ProductService((io.vertx.blueprint.microservice.product.ProductService) obj),
    ProductService::getDelegate
  );

  private final io.vertx.blueprint.microservice.product.ProductService delegate;
  
  public ProductService(io.vertx.blueprint.microservice.product.ProductService delegate) {
    this.delegate = delegate;
  }

  public io.vertx.blueprint.microservice.product.ProductService getDelegate() {
    return delegate;
  }

  /**
   * Initialize the persistence.
   * @param resultHandler the result handler will be called as soon as the initialization has been accomplished. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService initializePersistence(Handler<AsyncResult<Void>> resultHandler) { 
    delegate.initializePersistence(resultHandler);
    return this;
  }

  /**
   * Initialize the persistence.
   * @return 
   */
  public Single<Void> rxInitializePersistence() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      initializePersistence(fut);
    }));
  }

  /**
   * Add a product to the persistence.
   * @param product a product entity that we want to add
   * @param resultHandler the result handler will be called as soon as the product has been added. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService addProduct(Product product, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.addProduct(product, resultHandler);
    return this;
  }

  /**
   * Add a product to the persistence.
   * @param product a product entity that we want to add
   * @return 
   */
  public Single<Void> rxAddProduct(Product product) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      addProduct(product, fut);
    }));
  }

  /**
   * Retrieve the product with certain `productId`.
   * @param productId product id
   * @param resultHandler the result handler will be called as soon as the product has been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService retrieveProduct(String productId, Handler<AsyncResult<Product>> resultHandler) { 
    delegate.retrieveProduct(productId, resultHandler);
    return this;
  }

  /**
   * Retrieve the product with certain `productId`.
   * @param productId product id
   * @return 
   */
  public Single<Product> rxRetrieveProduct(String productId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveProduct(productId, fut);
    }));
  }

  /**
   * Retrieve the product price with certain `productId`.
   * @param productId product id
   * @param resultHandler the result handler will be called as soon as the product has been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService retrieveProductPrice(String productId, Handler<AsyncResult<JsonObject>> resultHandler) { 
    delegate.retrieveProductPrice(productId, resultHandler);
    return this;
  }

  /**
   * Retrieve the product price with certain `productId`.
   * @param productId product id
   * @return 
   */
  public Single<JsonObject> rxRetrieveProductPrice(String productId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveProductPrice(productId, fut);
    }));
  }

  /**
   * Retrieve all products.
   * @param resultHandler the result handler will be called as soon as the products have been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService retrieveAllProducts(Handler<AsyncResult<List<Product>>> resultHandler) { 
    delegate.retrieveAllProducts(resultHandler);
    return this;
  }

  /**
   * Retrieve all products.
   * @return 
   */
  public Single<List<Product>> rxRetrieveAllProducts() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveAllProducts(fut);
    }));
  }

  /**
   * Retrieve products by page.
   * @param page 
   * @param resultHandler the result handler will be called as soon as the products have been retrieved. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService retrieveProductsByPage(int page, Handler<AsyncResult<List<Product>>> resultHandler) { 
    delegate.retrieveProductsByPage(page, resultHandler);
    return this;
  }

  /**
   * Retrieve products by page.
   * @param page 
   * @return 
   */
  public Single<List<Product>> rxRetrieveProductsByPage(int page) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveProductsByPage(page, fut);
    }));
  }

  /**
   * Delete a product from the persistence
   * @param productId product id
   * @param resultHandler the result handler will be called as soon as the product has been removed. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService deleteProduct(String productId, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.deleteProduct(productId, resultHandler);
    return this;
  }

  /**
   * Delete a product from the persistence
   * @param productId product id
   * @return 
   */
  public Single<Void> rxDeleteProduct(String productId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      deleteProduct(productId, fut);
    }));
  }

  /**
   * Delete all products from the persistence
   * @param resultHandler the result handler will be called as soon as the products have been removed. The async result indicates whether the operation was successful or not.
   * @return 
   */
  public ProductService deleteAllProducts(Handler<AsyncResult<Void>> resultHandler) { 
    delegate.deleteAllProducts(resultHandler);
    return this;
  }

  /**
   * Delete all products from the persistence
   * @return 
   */
  public Single<Void> rxDeleteAllProducts() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      deleteAllProducts(fut);
    }));
  }


  public static ProductService newInstance(io.vertx.blueprint.microservice.product.ProductService arg) {
    return arg != null ? new ProductService(arg) : null;
  }
}


import io.vertx.blueprint.microservice.product.ProductTuple;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Shopping cart state object.
 */
@DataObject(generateConverter = true)
public class ShoppingCart {

  private List<ProductTuple> productItems = new ArrayList<>();
  private Map<String, Integer> amountMap = new HashMap<>();

  public ShoppingCart() {
    // Empty constructor.
  }

  public ShoppingCart(JsonObject json) {
    ShoppingCartConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ShoppingCartConverter.toJson(this, json);
    return json;
  }

  public List<ProductTuple> getProductItems() {
    return productItems;
  }

  public ShoppingCart setProductItems(List<ProductTuple> productItems) {
    this.productItems = productItems;
    return this;
  }

  @GenIgnore
  public Map<String, Integer> getAmountMap() {
    return amountMap;
  }

  public boolean isEmpty() {
    return productItems.isEmpty();
  }

  public ShoppingCart incorporate(CartEvent cartEvent) {
    // The cart event must be a add or remove command event.
    boolean ifValid = Stream.of(CartEventType.ADD_ITEM, CartEventType.REMOVE_ITEM)
      .anyMatch(cartEventType ->
        cartEvent.getCartEventType().equals(cartEventType));

    if (ifValid) {
      amountMap.put(cartEvent.getProductId(),
        amountMap.getOrDefault(cartEvent.getProductId(), 0) +
          (cartEvent.getAmount() * (cartEvent.getCartEventType()
            .equals(CartEventType.ADD_ITEM) ? 1 : -1)));
    }

    return this;
  }

  @Override
  public String toString() {
    return this.toJson().encode();
  }
}


import io.vertx.blueprint.microservice.cart.api.RestShoppingAPIVerticle;
import io.vertx.blueprint.microservice.cart.impl.ShoppingCartServiceImpl;
import io.vertx.blueprint.microservice.common.BaseMicroserviceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

/**
 * Shopping cart verticle.
 *
 * @author Eric Zhao
 */
public class CartVerticle extends BaseMicroserviceVerticle {

  private ShoppingCartService shoppingCartService;
  private CheckoutService checkoutService;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    // create the service instance
    this.shoppingCartService = new ShoppingCartServiceImpl(vertx, discovery, config());
    this.checkoutService = CheckoutService.createService(vertx, discovery);
    // register the service proxy on event bus
    ProxyHelper.registerService(CheckoutService.class, vertx, checkoutService, CheckoutService.SERVICE_ADDRESS);
    ProxyHelper.registerService(ShoppingCartService.class, vertx, shoppingCartService, ShoppingCartService.SERVICE_ADDRESS);

    // publish the service in the discovery infrastructure
    publishEventBusService(CheckoutService.SERVICE_NAME, CheckoutService.SERVICE_ADDRESS, CheckoutService.class)
      .compose(servicePublished ->
        publishEventBusService(ShoppingCartService.SERVICE_NAME, ShoppingCartService.SERVICE_ADDRESS, ShoppingCartService.class))
      .compose(servicePublished ->
        publishMessageSource("shopping-payment-message-source", CheckoutService.PAYMENT_EVENT_ADDRESS))
      .compose(sourcePublished ->
        publishMessageSource("shopping-order-message-source", CheckoutService.ORDER_EVENT_ADDRESS))
      .compose(sourcePublished -> deployRestVerticle())
      .setHandler(future.completer());
  }

  private Future<Void> deployRestVerticle() {
    Future<String> future = Future.future();
    vertx.deployVerticle(new RestShoppingAPIVerticle(shoppingCartService, checkoutService),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }

}


import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Cart event state object.
 *
 * @author Eric Zhao
 */
@DataObject(generateConverter = true)
public class CartEvent {

  private Long id;
  private CartEventType cartEventType;
  private String userId;
  private String productId;
  private Integer amount;

  private long createdAt;

  public CartEvent() {
    this.createdAt = System.currentTimeMillis();
  }

  public CartEvent(JsonObject json) {
    CartEventConverter.fromJson(json, this);
  }

  public CartEvent(CartEventType cartEventType, String userId, String productId, Integer amount) {
    this.cartEventType = cartEventType;
    this.userId = userId;
    this.productId = productId;
    this.amount = amount;
    this.createdAt = System.currentTimeMillis();
  }

  /**
   * Helper method to create checkout event for a user.
   *
   * @param userId user id
   * @return created checkout cart event
   */
  public static CartEvent createCheckoutEvent(String userId) {
    return new CartEvent(CartEventType.CHECKOUT, userId, "all", 0);
  }

  /**
   * Helper method to create clear cart event for a user.
   *
   * @param userId user id
   * @return created clear cart event
   */
  public static CartEvent createClearEvent(String userId) {
    return new CartEvent(CartEventType.CLEAR_CART, userId, "all", 0);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CartEventConverter.toJson(this, json);
    return json;
  }

  public Long getId() {
    return id;
  }

  public CartEvent setId(Long id) {
    this.id = id;
    return this;
  }

  public CartEventType getCartEventType() {
    return cartEventType;
  }

  public CartEvent setCartEventType(CartEventType cartEventType) {
    this.cartEventType = cartEventType;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public CartEvent setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getProductId() {
    return productId;
  }

  public CartEvent setProductId(String productId) {
    this.productId = productId;
    return this;
  }

  public Integer getAmount() {
    return amount;
  }

  public CartEvent setAmount(Integer amount) {
    this.amount = amount;
    return this;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public CartEvent setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public String toString() {
    return this.toJson().encode();
  }

  public static boolean isTerminal(CartEventType eventType) {
    return eventType == CartEventType.CLEAR_CART || eventType == CartEventType.CHECKOUT;
  }
}

/**
 * Indicates that this module contains classes that need to be generated / processed.
 */
@ModuleGen(name = "vertx-blueprint-shopping-cart", groupPackage = "io.vertx.blueprint.microservice.cart")


import io.vertx.codegen.annotations.ModuleGen;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface for shopping cart operation.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen(concrete = false)
@ProxyGen
public interface ShoppingCartService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "shopping-cart-eb-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.shopping.cart";

  /**
   * Add cart event to the event source.
   *
   * @param event         cart event
   * @param resultHandler async result handler
   */
  @Fluent
  ShoppingCartService addCartEvent(CartEvent event, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Get shopping cart of a user.
   *
   * @param userId user id
   * @param resultHandler async result handler
   */
  @Fluent
  ShoppingCartService getShoppingCart(String userId, Handler<AsyncResult<ShoppingCart>> resultHandler);

}


import io.vertx.codegen.annotations.VertxGen;

/**
 * An enum class for the type of {@link CartEvent}.
 */
@VertxGen
public enum CartEventType {
  ADD_ITEM,
  REMOVE_ITEM,
  CHECKOUT,
  CLEAR_CART
}


import io.vertx.blueprint.microservice.order.Order;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Checkout result data object.
 */
@DataObject(generateConverter = true)
public class CheckoutResult {

  private String message;
  private Order order;

  public CheckoutResult() {
    // Empty constructor
  }

  public CheckoutResult(String message, Order order) {
    this.message = message;
    this.order = order;
  }

  public CheckoutResult(JsonObject json) {
    CheckoutResultConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CheckoutResultConverter.toJson(this, json);
    return json;
  }

  public String getMessage() {
    return message;
  }

  public CheckoutResult setMessage(String message) {
    this.message = message;
    return this;
  }

  public Order getOrder() {
    return order;
  }

  public CheckoutResult setOrder(Order order) {
    this.order = order;
    return this;
  }

  @Override
  public String toString() {
    return this.toJson().encode();
  }
}


import io.vertx.blueprint.microservice.cart.impl.CheckoutServiceImpl;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.servicediscovery.ServiceDiscovery;


/**
 * A service interface for shopping cart checkout logic.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface CheckoutService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "shopping-checkout-eb-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.shopping.cart.checkout";

  /**
   * Payment and order event address.
   */
  String PAYMENT_EVENT_ADDRESS = "events.service.shopping.to.payment";
  String ORDER_EVENT_ADDRESS = "events.service.shopping.to.order";

  /**
   * Create a shopping checkout service instance
   */
  static CheckoutService createService(Vertx vertx, ServiceDiscovery discovery) {
    return new CheckoutServiceImpl(vertx, discovery);
  }

  /**
   * Shopping cart checkout.
   *
   * @param userId  user id
   * @param handler async result handler
   */
  void checkout(String userId, Handler<AsyncResult<CheckoutResult>> handler);

}


import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.blueprint.microservice.cart.CheckoutService;
import io.vertx.blueprint.microservice.cart.ShoppingCartService;
import io.vertx.blueprint.microservice.common.RestAPIVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Optional;

/**
 * This verticle exposes a HTTP endpoint to process shopping cart with REST APIs.
 *
 * @author Eric Zhao
 */
public class RestShoppingAPIVerticle extends RestAPIVerticle {

  private static final String SERVICE_NAME = "shopping-cart-rest-api";

  private final ShoppingCartService shoppingCartService;
  private final CheckoutService checkoutService;

  private static final String API_CHECKOUT = "/checkout";
  private static final String API_ADD_CART_EVENT = "/events";
  private static final String API_GET_CART = "/cart";

  public RestShoppingAPIVerticle(ShoppingCartService shoppingCartService, CheckoutService checkoutService) {
    this.shoppingCartService = shoppingCartService;
    this.checkoutService = checkoutService;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    final Router router = Router.router(vertx);
    // body handler
    router.route().handler(BodyHandler.create());
    // api route handler
    router.post(API_CHECKOUT).handler(context -> requireLogin(context, this::apiCheckout));
    router.post(API_ADD_CART_EVENT).handler(context -> requireLogin(context, this::apiAddCartEvent));
    router.get(API_GET_CART).handler(context -> requireLogin(context, this::apiGetCart));

    enableLocalSession(router);

    // get HTTP host and port from configuration, or use default value
    String host = config().getString("shopping.cart.http.address", "0.0.0.0");
    int port = config().getInteger("shopping.cart.http.port", 8084);

    // create http server for the REST service
    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void apiCheckout(RoutingContext context, JsonObject principle) {
    String userId = Optional.ofNullable(principle.getString("userId"))
      .orElse(TEST_USER);
    checkoutService.checkout(userId, resultHandler(context));
  }

  private void apiAddCartEvent(RoutingContext context, JsonObject principal) {
    String userId = Optional.ofNullable(principal.getString("userId"))
      .orElse(TEST_USER);
    CartEvent cartEvent = new CartEvent(context.getBodyAsJson());
    if (validateEvent(cartEvent, userId)) {
      shoppingCartService.addCartEvent(cartEvent, resultVoidHandler(context, 201));
    } else {
      context.fail(400);
    }
  }

  private void apiGetCart(RoutingContext context, JsonObject principal) {
    String userId = Optional.ofNullable(principal.getString("userId"))
      .orElse(TEST_USER);
    shoppingCartService.getShoppingCart(userId, resultHandler(context));
  }

  private boolean validateEvent(CartEvent event, String userId) {
    return event.getUserId() != null && event.getAmount() != null && event.getAmount() > 0
      && event.getUserId().equals(userId);
  }

  // for test
  private static final String TEST_USER = "TEST666";

}


import rx.Single;

import java.util.Optional;

/**
 * Simple Rx-fied data source service interface for CRUD.
 *
 * @param <T>  Type of the entity
 * @param <ID> Type of the persistence key
 */
public interface SimpleCrudDataSource<T, ID> {

  /**
   * Save an entity to the persistence.
   *
   * @param entity entity object
   * @return an observable async result
   */
  Single<Void> save(T entity);

  /**
   * Retrieve one certain entity by `id`.
   *
   * @param id id of the entity
   * @return an observable async result
   */
  Single<Optional<T>> retrieveOne(ID id);

  /**
   * Delete the entity by `id`.
   *
   * @param id id of the entity
   * @return an observable async result
   */
  Single<Void> delete(ID id);

}


import io.vertx.blueprint.microservice.cart.CartEvent;
import rx.Observable;

/**
 * Data source interface for processing {@link CartEvent}. Append-only operations.
 *
 * @author Eric Zhao
 */
public interface CartEventDataSource extends SimpleCrudDataSource<CartEvent, Long> {

  /**
   * Fetch cart event stream from the event source.
   *
   * @param userId user id
   * @return async stream of the cart events
   */
  Observable<CartEvent> streamByUser(String userId);

}


import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.blueprint.microservice.cart.CartEventType;
import io.vertx.blueprint.microservice.cart.repository.CartEventDataSource;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.Observable;
import rx.Single;

import java.util.Optional;


/**
 * Implementation of {@link CartEventDataSource}.
 *
 * @author Eric Zhao
 */
public class CartEventDataSourceImpl implements CartEventDataSource {

  private final JDBCClient client;

  public CartEventDataSourceImpl(io.vertx.core.Vertx vertx, JsonObject json) {
    this.client = JDBCClient.createNonShared(Vertx.newInstance(vertx), json);
    // TODO: Should not init the table here.
    client.rxGetConnection()
      .flatMap(connection ->
        connection.rxExecute(INIT_STATEMENT)
          .doAfterTerminate(connection::close)
      )
      .subscribe();
  }

  @Override
  public Observable<CartEvent> streamByUser(String userId) {
    JsonArray params = new JsonArray().add(userId).add(userId);
    return client.rxGetConnection()
      .flatMapObservable(conn ->
        conn.rxQueryWithParams(STREAM_STATEMENT, params)
          .map(ResultSet::getRows)
          .flatMapObservable(Observable::from)
          .map(this::wrapCartEvent)
          .doOnTerminate(conn::close)
      );
  }

  @Override
  public Single<Void> save(CartEvent cartEvent) {
    JsonArray params = new JsonArray().add(cartEvent.getCartEventType().name())
      .add(cartEvent.getUserId())
      .add(cartEvent.getProductId())
      .add(cartEvent.getAmount())
      .add(cartEvent.getCreatedAt() > 0 ? cartEvent.getCreatedAt() : System.currentTimeMillis());
    return client.rxGetConnection()
      .flatMap(conn -> conn.rxUpdateWithParams(SAVE_STATEMENT, params)
        .map(r -> (Void) null)
        .doAfterTerminate(conn::close)
      );
  }

  @Override
  public Single<Optional<CartEvent>> retrieveOne(Long id) {
    return client.rxGetConnection()
      .flatMap(conn ->
        conn.rxQueryWithParams(RETRIEVE_STATEMENT, new JsonArray().add(id))
          .map(ResultSet::getRows)
          .map(list -> {
            if (list.isEmpty()) {
              return Optional.<CartEvent>empty();
            } else {
              return Optional.of(list.get(0))
                .map(this::wrapCartEvent);
            }
          })
          .doAfterTerminate(conn::close)
      );
  }

  @Override
  public Single<Void> delete(Long id) {
    // This service is an append-only service, so delete is not allowed.
    return Single.error(new RuntimeException("Delete is not allowed"));
  }

  /**
   * Wrap raw cart event object from the event source.
   *
   * @param raw raw event object
   * @return wrapped cart event
   */
  private CartEvent wrapCartEvent(JsonObject raw) {
    return new CartEvent(raw)
      .setUserId(raw.getString("user_id"))
      .setProductId(raw.getString("product_id"))
      .setCreatedAt(raw.getLong("created_at"))
      .setCartEventType(CartEventType.valueOf(raw.getString("type")));
  }

  // SQL Statement

  private static final String INIT_STATEMENT = "CREATE TABLE IF NOT EXISTS `cart_event` (\n" +
    "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
    "  `type` VARCHAR(20) NOT NULL,\n" +
    "  `user_id` varchar(45) NOT NULL,\n" +
    "  `product_id` varchar(45) NOT NULL,\n" +
    "  `amount` int(11) NOT NULL,\n" +
    "  `created_at` bigint(20) NOT NULL,\n" +
    "  PRIMARY KEY (`id`),\n" +
    "  KEY `INDEX_USER` (`user_id`) )";

  private static final String SAVE_STATEMENT = "INSERT INTO `cart_event` (`type`, `user_id`, `product_id`, `amount`, `created_at`) " +
    "VALUES (?, ?, ?, ?, ?)";

  private static final String RETRIEVE_STATEMENT = "SELECT * FROM `cart_event` WHERE id = ?";

  private static final String STREAM_STATEMENT = "SELECT * FROM cart_event c\n" +
    "WHERE c.user_id = ? AND c.created_at > coalesce(\n" +
    "    (SELECT created_at FROM cart_event\n" +
    "\t WHERE user_id = ? AND (`type` = \"CHECKOUT\" OR `type` = \"CLEAR_CART\")\n" +
    "     ORDER BY cart_event.created_at DESC\n" +
    "     LIMIT 1\n" +
    "     ), 0)\n" +
    "ORDER BY c.created_at ASC;";
}


import io.vertx.blueprint.microservice.cache.CounterService;
import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.blueprint.microservice.cart.CheckoutResult;
import io.vertx.blueprint.microservice.cart.CheckoutService;
import io.vertx.blueprint.microservice.cart.ShoppingCart;
import io.vertx.blueprint.microservice.cart.ShoppingCartService;
import io.vertx.blueprint.microservice.common.functional.Functional;
import io.vertx.blueprint.microservice.order.Order;
import io.vertx.blueprint.microservice.product.ProductTuple;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple implementation for {@link CheckoutService}.
 *
 * @author Eric Zhao
 */
public class CheckoutServiceImpl implements CheckoutService {

  private final Vertx vertx;
  private final ServiceDiscovery discovery;

  public CheckoutServiceImpl(Vertx vertx, ServiceDiscovery discovery) {
    this.vertx = vertx;
    this.discovery = discovery;
  }

  @Override
  public void checkout(String userId, Handler<AsyncResult<CheckoutResult>> resultHandler) {
    if (userId == null) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Invalid user")));
      return;
    }
    Future<ShoppingCart> cartFuture = getCurrentCart(userId);
    Future<CheckoutResult> orderFuture = cartFuture.compose(cart ->
      checkAvailableInventory(cart).compose(checkResult -> {
        if (checkResult.getBoolean("res")) {
          double totalPrice = calculateTotalPrice(cart);
          // create order instance
          Order order = new Order().setBuyerId(userId)
            .setPayId("TEST") // reserved field
            .setProducts(cart.getProductItems())
            .setTotalPrice(totalPrice);
          // set id and then send order, wait for reply
          return retrieveCounter("order")
            .compose(id -> sendOrderAwaitResult(order.setOrderId(id)))
            .compose(result -> saveCheckoutEvent(userId).map(v -> result));
        } else {
          // has insufficient inventory, fail
          return Future.succeededFuture(new CheckoutResult()
            .setMessage(checkResult.getString("message")));
        }
      })
    );

    orderFuture.setHandler(resultHandler);
  }

  /**
   * Fetch global counter of order from the cache infrastructure.
   *
   * @param key counter key (type)
   * @return async result of the counter
   */
  private Future<Long> retrieveCounter(String key) {
    Future<Long> future = Future.future();
    EventBusService.getProxy(discovery, CounterService.class,
      ar -> {
        if (ar.succeeded()) {
          CounterService service = ar.result();
          service.addThenRetrieve(key, future.completer());
        } else {
          future.fail(ar.cause());
        }
      });
    return future;
  }

  /**
   * Send the order to the order microservice and wait for reply.
   *
   * @param order order data object
   * @return async result
   */
  private Future<CheckoutResult> sendOrderAwaitResult(Order order) {
    Future<CheckoutResult> future = Future.future();
    vertx.eventBus().send(CheckoutService.ORDER_EVENT_ADDRESS, order.toJson(), reply -> {
      if (reply.succeeded()) {
        future.complete(new CheckoutResult((JsonObject) reply.result().body()));
      } else {
        future.fail(reply.cause());
      }
    });
    return future;
  }

  private Future<ShoppingCart> getCurrentCart(String userId) {
    Future<ShoppingCartService> future = Future.future();
    EventBusService.getProxy(discovery, ShoppingCartService.class, future.completer());
    return future.compose(service -> {
      Future<ShoppingCart> cartFuture = Future.future();
      service.getShoppingCart(userId, cartFuture.completer());
      return cartFuture.compose(c -> {
        if (c == null || c.isEmpty())
          return Future.failedFuture(new IllegalStateException("Invalid shopping cart"));
        else
          return Future.succeededFuture(c);
      });
    });
  }

  private double calculateTotalPrice(ShoppingCart cart) {
    return cart.getProductItems().stream()
      .map(p -> p.getAmount() * p.getPrice()) // join by product id
      .reduce(0.0d, (a, b) -> a + b);
  }

  private Future<HttpClient> getInventoryEndpoint() {
    Future<HttpClient> future = Future.future();
    HttpEndpoint.getClient(discovery,
      new JsonObject().put("name", "inventory-rest-api"),
      future.completer());
    return future;
  }

  private Future<JsonObject> getInventory(ProductTuple product, HttpClient client) {
    Future<Integer> future = Future.future();
    client.get("/" + product.getProductId(), response -> {
      if (response.statusCode() == 200) {
        response.bodyHandler(buffer -> {
          try {
            int inventory = Integer.valueOf(buffer.toString());
            future.complete(inventory);
          } catch (NumberFormatException ex) {
            future.fail(ex);
          }
        });
      } else {
        future.fail("not_found:" + product.getProductId());
      }
    })
      .exceptionHandler(future::fail)
      .end();
    return future.map(inv -> new JsonObject()
      .put("id", product.getProductId())
      .put("inventory", inv)
      .put("amount", product.getAmount()));
  }

  /**
   * Check inventory for the current cart.
   *
   * @param cart shopping cart data object
   * @return async result
   */
  private Future<JsonObject> checkAvailableInventory(ShoppingCart cart) {
    Future<List<JsonObject>> allInventories = getInventoryEndpoint().compose(client -> {
      List<Future<JsonObject>> futures = cart.getProductItems()
        .stream()
        .map(product -> getInventory(product, client))
        .collect(Collectors.toList());
      return Functional.allOfFutures(futures)
        .map(r -> {
          ServiceDiscovery.releaseServiceObject(discovery, client);
          return r;
        });
    });
    return allInventories.map(inventories -> {
      JsonObject result = new JsonObject();
      // get the list of products whose inventory is lower than the demand amount
      List<JsonObject> insufficient = inventories.stream()
        .filter(item -> item.getInteger("inventory") - item.getInteger("amount") < 0)
        .collect(Collectors.toList());
      // insufficient inventory exists
      if (insufficient.size() > 0) {
        String insufficientList = insufficient.stream()
          .map(item -> item.getString("id"))
          .collect(Collectors.joining(", "));
        result.put("message", String.format("Insufficient inventory available for product %s.", insufficientList))
          .put("res", false);
      } else {
        result.put("res", true);
      }
      return result;
    });
  }

  /**
   * Save checkout cart event for current user.
   *
   * @param userId user id
   * @return async result
   */
  private Future<Void> saveCheckoutEvent(String userId) {
    Future<ShoppingCartService> future = Future.future();
    EventBusService.getProxy(discovery, ShoppingCartService.class, future.completer());
    return future.compose(service -> {
      Future<Void> resFuture = Future.future();
      CartEvent event = CartEvent.createCheckoutEvent(userId);
      service.addCartEvent(event, resFuture.completer());
      return resFuture;
    });
  }

}


import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.blueprint.microservice.cart.ShoppingCart;
import io.vertx.blueprint.microservice.cart.ShoppingCartService;
import io.vertx.blueprint.microservice.cart.repository.CartEventDataSource;
import io.vertx.blueprint.microservice.cart.repository.impl.CartEventDataSourceImpl;
import io.vertx.blueprint.microservice.common.functional.Functional;
import io.vertx.blueprint.microservice.product.Product;
import io.vertx.blueprint.microservice.product.ProductService;
import io.vertx.blueprint.microservice.product.ProductTuple;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ShoppingCartService}.
 *
 * @author Eric Zhao
 */
public class ShoppingCartServiceImpl implements ShoppingCartService {

  private final CartEventDataSource repository;
  private final ServiceDiscovery discovery;

  public ShoppingCartServiceImpl(Vertx vertx, ServiceDiscovery discovery, JsonObject config) {
    this.discovery = discovery;
    this.repository = new CartEventDataSourceImpl(vertx, config);
  }

  @Override
  public ShoppingCartService addCartEvent(CartEvent event, Handler<AsyncResult<Void>> resultHandler) {
    Future<Void> future = Future.future();
    repository.save(event).subscribe(future::complete, future::fail);
    future.setHandler(resultHandler);
    return this;
  }

  @Override
  public ShoppingCartService getShoppingCart(String userId, Handler<AsyncResult<ShoppingCart>> resultHandler) {
    aggregateCartEvents(userId)
      .setHandler(resultHandler);
    return this;
  }

  /**
   * Get the shopping cart for a certain user.
   *
   * @param userId user id
   * @return async result
   */
  private Future<ShoppingCart> aggregateCartEvents(String userId) {
    Future<ShoppingCart> future = Future.future();
    // aggregate cart events into raw shopping cart
    repository.streamByUser(userId)
      .takeWhile(cartEvent -> !CartEvent.isTerminal(cartEvent.getCartEventType()))
      .reduce(new ShoppingCart(), ShoppingCart::incorporate)
      .toSingle()
      .subscribe(future::complete, future::fail);

    return future.compose(cart ->
      getProductService()
        .compose(service -> prepareProduct(service, cart)) // prepare product data
        .compose(productList -> generateCurrentCartFromStream(cart, productList)) // prepare product items
    );
  }

  /**
   * Prepare meta product data stream for shopping cart.
   *
   * @param service product service instance
   * @param cart    raw shopping cart instance
   * @return async result
   */
  private Future<List<Product>> prepareProduct(ProductService service, ShoppingCart cart) {
    List<Future<Product>> futures = cart.getAmountMap().keySet()
      .stream()
      .map(productId -> {
        Future<Product> future = Future.future();
        service.retrieveProduct(productId, future.completer());
        return future;
      })
      .collect(Collectors.toList());
    return Functional.allOfFutures(futures);
  }

  /**
   * Generate current shopping cart from a data stream including necessary product data.
   * Note: this is not an asynchronous method. `Future` only marks whether the process is successful.
   *
   * @param rawCart       raw shopping cart
   * @param productList product data stream
   * @return async result
   */
  private Future<ShoppingCart> generateCurrentCartFromStream(ShoppingCart rawCart, List<Product> productList) {
    Future<ShoppingCart> future = Future.future();
    // check if any of the product is invalid
    if (productList.stream().anyMatch(Objects::isNull)) {
      future.fail("Error when retrieve products: empty");
      return future;
    }
    // construct the product items
    List<ProductTuple> currentItems = rawCart.getAmountMap().entrySet()
      .stream()
      .map(item -> new ProductTuple(getProductFromStream(productList, item.getKey()),
        item.getValue()))
      .filter(item -> item.getAmount() > 0)
      .collect(Collectors.toList());

    ShoppingCart cart = rawCart.setProductItems(currentItems);
    return Future.succeededFuture(cart);
  }

  /**
   * Get meta product data (seller and unit price) from a data stream of products.
   *
   * @param productList a data stream of products.
   * @param productId     product id
   * @return corresponding product
   */
  private Product getProductFromStream(List<Product> productList, String productId) {
    return productList.stream()
      .filter(product -> product.getProductId().equals(productId))
      .findFirst()
      .get();
  }

  /**
   * Get product service from the service discovery infrastructure.
   *
   * @return async result of the service.
   */
  private Future<ProductService> getProductService() {
    Future<ProductService> future = Future.future();
    EventBusService.getProxy(discovery, ProductService.class, future.completer());
    return future;
  }
}

/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.cart.ShoppingCartService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.cart.ShoppingCart;
import io.vertx.blueprint.microservice.cart.ShoppingCartService;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class ShoppingCartServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final ShoppingCartService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public ShoppingCartServiceVertxProxyHandler(Vertx vertx, ShoppingCartService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public ShoppingCartServiceVertxProxyHandler(Vertx vertx, ShoppingCartService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public ShoppingCartServiceVertxProxyHandler(Vertx vertx, ShoppingCartService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "addCartEvent": {
          service.addCartEvent(json.getJsonObject("event") == null ? null : new io.vertx.blueprint.microservice.cart.CartEvent(json.getJsonObject("event")), createHandler(msg));
          break;
        }
        case "getShoppingCart": {
          service.getShoppingCart((java.lang.String)json.getValue("userId"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.cart.ShoppingCartService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.cart.ShoppingCart;
import io.vertx.blueprint.microservice.cart.ShoppingCartService;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class ShoppingCartServiceVertxEBProxy implements ShoppingCartService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public ShoppingCartServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public ShoppingCartServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public ShoppingCartService addCartEvent(CartEvent event, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("event", event == null ? null : event.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "addCartEvent");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public ShoppingCartService getShoppingCart(String userId, Handler<AsyncResult<ShoppingCart>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("userId", userId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "getShoppingCart");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new ShoppingCart(res.result().body())));
                      }
    });
    return this;
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.cart.CheckoutService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.core.Vertx;
import io.vertx.blueprint.microservice.cart.CheckoutService;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.cart.CheckoutResult;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class CheckoutServiceVertxEBProxy implements CheckoutService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public CheckoutServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public CheckoutServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public void checkout(String userId, Handler<AsyncResult<CheckoutResult>> handler) {
    if (closed) {
      handler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("userId", userId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "checkout");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        handler.handle(Future.failedFuture(res.cause()));
      } else {
        handler.handle(Future.succeededFuture(res.result().body() == null ? null : new CheckoutResult(res.result().body())));
                      }
    });
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.cart.CartEvent}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.cart.CartEvent} original class using Vert.x codegen.
 */
public class CartEventConverter {

  public static void fromJson(JsonObject json, CartEvent obj) {
    if (json.getValue("amount") instanceof Number) {
      obj.setAmount(((Number)json.getValue("amount")).intValue());
    }
    if (json.getValue("cartEventType") instanceof String) {
      obj.setCartEventType(io.vertx.blueprint.microservice.cart.CartEventType.valueOf((String)json.getValue("cartEventType")));
    }
    if (json.getValue("createdAt") instanceof Number) {
      obj.setCreatedAt(((Number)json.getValue("createdAt")).longValue());
    }
    if (json.getValue("id") instanceof Number) {
      obj.setId(((Number)json.getValue("id")).longValue());
    }
    if (json.getValue("productId") instanceof String) {
      obj.setProductId((String)json.getValue("productId"));
    }
    if (json.getValue("userId") instanceof String) {
      obj.setUserId((String)json.getValue("userId"));
    }
  }

  public static void toJson(CartEvent obj, JsonObject json) {
    if (obj.getAmount() != null) {
      json.put("amount", obj.getAmount());
    }
    if (obj.getCartEventType() != null) {
      json.put("cartEventType", obj.getCartEventType().name());
    }
    if (obj.getCreatedAt() != null) {
      json.put("createdAt", obj.getCreatedAt());
    }
    if (obj.getId() != null) {
      json.put("id", obj.getId());
    }
    if (obj.getProductId() != null) {
      json.put("productId", obj.getProductId());
    }
    if (obj.getUserId() != null) {
      json.put("userId", obj.getUserId());
    }
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.cart.ShoppingCart}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.cart.ShoppingCart} original class using Vert.x codegen.
 */
public class ShoppingCartConverter {

  public static void fromJson(JsonObject json, ShoppingCart obj) {
    if (json.getValue("productItems") instanceof JsonArray) {
      java.util.ArrayList<io.vertx.blueprint.microservice.product.ProductTuple> list = new java.util.ArrayList<>();
      json.getJsonArray("productItems").forEach( item -> {
        if (item instanceof JsonObject)
          list.add(new io.vertx.blueprint.microservice.product.ProductTuple((JsonObject)item));
      });
      obj.setProductItems(list);
    }
  }

  public static void toJson(ShoppingCart obj, JsonObject json) {
    json.put("empty", obj.isEmpty());
    if (obj.getProductItems() != null) {
      JsonArray array = new JsonArray();
      obj.getProductItems().forEach(item -> array.add(item.toJson()));
      json.put("productItems", array);
    }
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.cart.CheckoutService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import io.vertx.core.Vertx;
import io.vertx.blueprint.microservice.cart.CheckoutService;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.cart.CheckoutResult;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class CheckoutServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final CheckoutService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public CheckoutServiceVertxProxyHandler(Vertx vertx, CheckoutService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public CheckoutServiceVertxProxyHandler(Vertx vertx, CheckoutService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public CheckoutServiceVertxProxyHandler(Vertx vertx, CheckoutService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {

        case "checkout": {
          service.checkout((java.lang.String)json.getValue("userId"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.cart.CheckoutResult}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.cart.CheckoutResult} original class using Vert.x codegen.
 */
public class CheckoutResultConverter {

  public static void fromJson(JsonObject json, CheckoutResult obj) {
    if (json.getValue("message") instanceof String) {
      obj.setMessage((String)json.getValue("message"));
    }
    if (json.getValue("order") instanceof JsonObject) {
      obj.setOrder(new io.vertx.blueprint.microservice.order.Order((JsonObject)json.getValue("order")));
    }
  }

  public static void toJson(CheckoutResult obj, JsonObject json) {
    if (obj.getMessage() != null) {
      json.put("message", obj.getMessage());
    }
    if (obj.getOrder() != null) {
      json.put("order", obj.getOrder().toJson());
    }
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import rx.Single;
import io.vertx.blueprint.microservice.cart.CartEvent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.cart.ShoppingCart;

/**
 * A service interface for shopping cart operation.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.cart.ShoppingCartService original} non RX-ified interface using Vert.x codegen.
 */

@io.vertx.lang.rxjava.RxGen(io.vertx.blueprint.microservice.cart.ShoppingCartService.class)
public interface ShoppingCartService {

  io.vertx.blueprint.microservice.cart.ShoppingCartService getDelegate();

  /**
   * Add cart event to the event source.
   * @param event cart event
   * @param resultHandler async result handler
   * @return 
   */
  public ShoppingCartService addCartEvent(CartEvent event, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Get shopping cart of a user.
   * @param userId user id
   * @param resultHandler async result handler
   * @return 
   */
  public ShoppingCartService getShoppingCart(String userId, Handler<AsyncResult<ShoppingCart>> resultHandler);


  public static ShoppingCartService newInstance(io.vertx.blueprint.microservice.cart.ShoppingCartService arg) {
    return arg != null ? new ShoppingCartServiceImpl(arg) : null;
  }
}

class ShoppingCartServiceImpl implements ShoppingCartService {
  private final io.vertx.blueprint.microservice.cart.ShoppingCartService delegate;
  
  public ShoppingCartServiceImpl(io.vertx.blueprint.microservice.cart.ShoppingCartService delegate) {
    this.delegate = delegate;
  }

  public io.vertx.blueprint.microservice.cart.ShoppingCartService getDelegate() {
    return delegate;
  }

  /**
   * Add cart event to the event source.
   * @param event cart event
   * @param resultHandler async result handler
   * @return 
   */
  public ShoppingCartService addCartEvent(CartEvent event, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.addCartEvent(event, resultHandler);
    return this;
  }

  /**
   * Add cart event to the event source.
   * @param event cart event
   * @return 
   */
  public Single<Void> rxAddCartEvent(CartEvent event) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      addCartEvent(event, fut);
    }));
  }

  /**
   * Get shopping cart of a user.
   * @param userId user id
   * @param resultHandler async result handler
   * @return 
   */
  public ShoppingCartService getShoppingCart(String userId, Handler<AsyncResult<ShoppingCart>> resultHandler) { 
    delegate.getShoppingCart(userId, resultHandler);
    return this;
  }

  /**
   * Get shopping cart of a user.
   * @param userId user id
   * @return 
   */
  public Single<ShoppingCart> rxGetShoppingCart(String userId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      getShoppingCart(userId, fut);
    }));
  }

}

/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import rx.Single;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.blueprint.microservice.cart.CheckoutResult;

/**
 * A service interface for shopping cart checkout logic.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.cart.CheckoutService original} non RX-ified interface using Vert.x codegen.
 */

@io.vertx.lang.rxjava.RxGen(io.vertx.blueprint.microservice.cart.CheckoutService.class)
public class CheckoutService {

  public static final io.vertx.lang.rxjava.TypeArg<CheckoutService> __TYPE_ARG = new io.vertx.lang.rxjava.TypeArg<>(
    obj -> new CheckoutService((io.vertx.blueprint.microservice.cart.CheckoutService) obj),
    CheckoutService::getDelegate
  );

  private final io.vertx.blueprint.microservice.cart.CheckoutService delegate;
  
  public CheckoutService(io.vertx.blueprint.microservice.cart.CheckoutService delegate) {
    this.delegate = delegate;
  }

  public io.vertx.blueprint.microservice.cart.CheckoutService getDelegate() {
    return delegate;
  }

  /**
   * Create a shopping checkout service instance
   * @param vertx 
   * @param discovery 
   * @return 
   */
  public static CheckoutService createService(Vertx vertx, ServiceDiscovery discovery) { 
    CheckoutService ret = CheckoutService.newInstance(io.vertx.blueprint.microservice.cart.CheckoutService.createService(vertx.getDelegate(), discovery.getDelegate()));
    return ret;
  }

  /**
   * Shopping cart checkout.
   * @param userId user id
   * @param handler async result handler
   */
  public void checkout(String userId, Handler<AsyncResult<CheckoutResult>> handler) { 
    delegate.checkout(userId, handler);
  }

  /**
   * Shopping cart checkout.
   * @param userId user id
   * @return 
   */
  public Single<CheckoutResult> rxCheckout(String userId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      checkout(userId, fut);
    }));
  }


  public static CheckoutService newInstance(io.vertx.blueprint.microservice.cart.CheckoutService arg) {
    return arg != null ? new CheckoutService(arg) : null;
  }
}


import io.vertx.blueprint.microservice.product.ProductTuple;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order data object.
 *
 * @author Eric Zhao
 */
@DataObject(generateConverter = true)
public class Order {

  private Long orderId = -1L;
  private String payId;
  private String buyerId;

  private Long createTime;

  private List<ProductTuple> products = new ArrayList<>();
  private Double totalPrice;


  public Order() {
  }

  public Order(Long orderId) {
    this.orderId = orderId;
  }

  public Order(JsonObject json) {
    OrderConverter.fromJson(json, this);
    if (json.getValue("products") instanceof String) {
      this.products = new JsonArray(json.getString("products"))
        .stream()
        .map(e -> (JsonObject) e)
        .map(ProductTuple::new)
        .collect(Collectors.toList());
    }
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    OrderConverter.toJson(this, json);
    return json;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Order setOrderId(Long orderId) {
    this.orderId = orderId;
    return this;
  }

  public String getPayId() {
    return payId;
  }

  public Order setPayId(String payId) {
    this.payId = payId;
    return this;
  }

  public String getBuyerId() {
    return buyerId;
  }

  public Order setBuyerId(String buyerId) {
    this.buyerId = buyerId;
    return this;
  }

  public List<ProductTuple> getProducts() {
    return products;
  }

  public Order setProducts(List<ProductTuple> products) {
    this.products = products;
    return this;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public Order setCreateTime(Long createTime) {
    this.createTime = createTime;
    return this;
  }

  public Double getTotalPrice() {
    return totalPrice;
  }

  public Order setTotalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
    return this;
  }

  @Override
  public String toString() {
    return this.toJson().encodePrettily();
  }
}


import io.vertx.blueprint.microservice.common.BaseMicroserviceVerticle;
import io.vertx.blueprint.microservice.order.api.RestOrderAPIVerticle;
import io.vertx.blueprint.microservice.order.impl.OrderServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ProxyHelper;

import static io.vertx.blueprint.microservice.order.OrderService.*;

/**
 * A verticle deploys multiple verticles for order operation and dispatching.
 *
 * @author Eric Zhao
 */
public class OrderVerticle extends BaseMicroserviceVerticle {

  private OrderService orderService;

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    this.orderService = new OrderServiceImpl(vertx, config());
    ProxyHelper.registerService(OrderService.class, vertx, orderService, SERVICE_ADDRESS);

    initOrderDatabase()
      .compose(databaseOkay -> publishEventBusService(SERVICE_NAME, SERVICE_ADDRESS, OrderService.class))
      .compose(servicePublished -> prepareDispatcher())
      .compose(dispatcherPrepared -> deployRestVerticle())
      .setHandler(future.completer());
  }

  private Future<Void> initOrderDatabase() {
    Future<Void> initFuture = Future.future();
    orderService.initializePersistence(initFuture.completer());
    return initFuture;
  }

  private Future<Void> prepareDispatcher() {
    Future<String> future = Future.future();
    vertx.deployVerticle(new RawOrderDispatcher(orderService),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }

  private Future<Void> deployRestVerticle() {
    Future<String> future = Future.future();
    vertx.deployVerticle(new RestOrderAPIVerticle(orderService),
      new DeploymentOptions().setConfig(config()),
      future.completer());
    return future.map(r -> null);
  }
}


import io.vertx.blueprint.microservice.common.BaseMicroserviceVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.MessageSource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A verticle for raw order wrapping and dispatching.
 *
 * @author Eric Zhao
 */
public class RawOrderDispatcher extends BaseMicroserviceVerticle {

  private final OrderService orderService;

  public RawOrderDispatcher(OrderService orderService) {
    this.orderService = orderService;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    MessageSource.<JsonObject>getConsumer(discovery,
      new JsonObject().put("name", "shopping-order-message-source"),
      ar -> {
        if (ar.succeeded()) {
          MessageConsumer<JsonObject> orderConsumer = ar.result();
          orderConsumer.handler(message -> {
            Order wrappedOrder = wrapRawOrder(message.body());
            dispatchOrder(wrappedOrder, message);
          });
          future.complete();
        } else {
          future.fail(ar.cause());
        }
      });
  }

  /**
   * Wrap raw order and generate new order.
   *
   * @return the new order.
   */
  private Order wrapRawOrder(JsonObject rawOrder) {
    return new Order(rawOrder)
      .setCreateTime(System.currentTimeMillis());
  }

  /**
   * Dispatch the order to the infrastructure layer.
   * Here we simply save the order to the persistence and modify inventory changes.
   *
   * @param order  order data object
   * @param sender message sender
   */
  private void dispatchOrder(Order order, Message<JsonObject> sender) {
    Future<Void> orderCreateFuture = Future.future();
    orderService.createOrder(order, orderCreateFuture.completer());
    orderCreateFuture
      .compose(orderCreated -> applyInventoryChanges(order))
      .setHandler(ar -> {
        if (ar.succeeded()) {
          CheckoutResult result = new CheckoutResult("checkout_success", order);
          sender.reply(result.toJson());
          publishLogEvent("checkout", result.toJson(), true);
        } else {
          sender.fail(5000, ar.cause().getMessage());
          ar.cause().printStackTrace();
        }
      });
  }

  /**
   * Apply inventory decrease changes according to the order.
   *
   * @param order order data object
   * @return async result
   */
  private Future<Void> applyInventoryChanges(Order order) {
    Future<Void> future = Future.future();
    // get REST endpoint
    Future<HttpClient> clientFuture = Future.future();
    HttpEndpoint.getClient(discovery,
      new JsonObject().put("name", "inventory-rest-api"),
      clientFuture.completer());
    // modify the inventory changes via REST API
    return clientFuture.compose(client -> {
      List<Future> futures = order.getProducts()
        .stream()
        .map(item -> {
          Future<Void> resultFuture = Future.future();
          String url = String.format("/%s/decrease?n=%d", item.getProductId(), item.getAmount());
          client.put(url, response -> {
            if (response.statusCode() == 200) {
              resultFuture.complete(); // need to check result?
            } else {
              resultFuture.fail(response.statusMessage());
            }
          })
            .exceptionHandler(resultFuture::fail)
            .end();
          return resultFuture;
        })
        .collect(Collectors.toList());
      // composite async results, all must be complete
      CompositeFuture.all(futures).setHandler(ar -> {
        if (ar.succeeded()) {
          future.complete();
        } else {
          future.fail(ar.cause());
        }
        ServiceDiscovery.releaseServiceObject(discovery, client); // Release the resources.
      });
      return future;
    });
  }
}


import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

/**
 * A service interface managing order storage operations.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface OrderService {

  /**
   * The name of the event bus service.
   */
  String SERVICE_NAME = "order-storage-eb-service";

  /**
   * The address on which the service is published.
   */
  String SERVICE_ADDRESS = "service.order.storage";

  /**
   * Initialize the persistence.
   *
   * @param resultHandler async result handler
   */
  @Fluent
  OrderService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

  /**
   * Retrieve orders belonging to a certain account.
   *
   * @param accountId     account id
   * @param resultHandler async result handler
   */
  @Fluent
  OrderService retrieveOrdersForAccount(String accountId, Handler<AsyncResult<List<Order>>> resultHandler);

  /**
   * Save an order into the persistence.
   *
   * @param order         order data object
   * @param resultHandler async result handler
   */
  @Fluent
  OrderService createOrder(Order order, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Retrieve the order with a certain {@code orderId}.
   *
   * @param orderId       order id
   * @param resultHandler async result handler
   */
  @Fluent
  OrderService retrieveOrder(Long orderId, Handler<AsyncResult<Order>> resultHandler);

}


import io.vertx.codegen.annotations.VertxGen;

/**
 * Order event type.
 */
@VertxGen
public enum OrderEventType {
  CREATED,
  PAID,
  SHIPPED,
  DELIVERED
}

/**
 * Indicates that this module contains classes that need to be generated / processed.
 */
@ModuleGen(name = "vertx-blueprint-order", groupPackage = "io.vertx.blueprint.microservice.order")


import io.vertx.codegen.annotations.ModuleGen;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Checkout result data object.
 */
@DataObject(generateConverter = true)
public class CheckoutResult {

  private String message;
  private Order order;

  public CheckoutResult() {
    // Empty constructor
  }

  public CheckoutResult(String message, Order order) {
    this.message = message;
    this.order = order;
  }

  public CheckoutResult(JsonObject json) {
    CheckoutResultConverter.fromJson(json, this);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    CheckoutResultConverter.toJson(this, json);
    return json;
  }

  public String getMessage() {
    return message;
  }

  public CheckoutResult setMessage(String message) {
    this.message = message;
    return this;
  }

  public Order getOrder() {
    return order;
  }

  public CheckoutResult setOrder(Order order) {
    this.order = order;
    return this;
  }

  @Override
  public String toString() {
    return this.toJson().encode();
  }
}


import io.vertx.blueprint.microservice.common.RestAPIVerticle;
import io.vertx.blueprint.microservice.order.OrderService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Optional;

/**
 * A verticle supplies REST endpoint for order service.
 *
 * @author Eric Zhao
 */
public class RestOrderAPIVerticle extends RestAPIVerticle {

  private static final String SERVICE_NAME = "order-rest-api";

  private static final String API_RETRIEVE = "/orders/:orderId";
  private static final String API_RETRIEVE_FOR_ACCOUNT = "/user/:id/orders";

  private final OrderService service;

  public RestOrderAPIVerticle(OrderService service) {
    this.service = service;
  }

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();

    Router router = Router.router(vertx);
    // body handler
    router.route().handler(BodyHandler.create());
    // API route
    router.get(API_RETRIEVE).handler(this::apiRetrieve);
    router.get(API_RETRIEVE_FOR_ACCOUNT).handler(context -> requireLogin(context, this::apiRetrieveForAccount));

    String host = config().getString("order.http.address", "0.0.0.0");
    int port = config().getInteger("order.http.port", 8090);

    // create HTTP server and publish REST service
    createHttpServer(router, host, port)
      .compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
      .setHandler(future.completer());
  }

  private void apiRetrieve(RoutingContext context) {
    try {
      Long orderId = Long.parseLong(context.request().getParam("orderId"));
      service.retrieveOrder(orderId, resultHandlerNonEmpty(context));
    } catch (NumberFormatException ex) {
      notFound(context);
    }
  }

  private void apiRetrieveForAccount(RoutingContext context, JsonObject principal) {
    String userId = context.request().getParam("id");
    String authUid = Optional.ofNullable(principal.getString("userId"))
      .orElse(TEST_USER);
    if (authUid.equals(userId)) {
      service.retrieveOrdersForAccount(userId, resultHandler(context, Json::encodePrettily));
    } else {
      context.fail(400);
    }
  }

  // for test
  private static final String TEST_USER = "TEST666";
}


import io.vertx.blueprint.microservice.common.service.JdbcRepositoryWrapper;
import io.vertx.blueprint.microservice.order.Order;
import io.vertx.blueprint.microservice.order.OrderService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link OrderService}.
 */
public class OrderServiceImpl extends JdbcRepositoryWrapper implements OrderService {

  public OrderServiceImpl(Vertx vertx, JsonObject config) {
    super(vertx, config);
  }

  @Override
  public OrderService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    client.getConnection(connHandler(resultHandler, connection -> {
      connection.execute(CREATE_STATEMENT, r -> {
        resultHandler.handle(r);
        connection.close();
      });
    }));
    return this;
  }

  @Override
  public OrderService retrieveOrdersForAccount(String accountId, Handler<AsyncResult<List<Order>>> resultHandler) {
    retrieveMany(new JsonArray().add(accountId), RETRIEVE_BY_ACCOUNT_STATEMENT)
      .map(rawList -> rawList.stream()
        .map(Order::new)
        .collect(Collectors.toList())
      )
      .setHandler(resultHandler);
    return this;
  }

  @Override
  public OrderService createOrder(Order order, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(order.getOrderId())
      .add(order.getPayId())
      .add(order.getBuyerId())
      .add(order.getCreateTime())
      .add(new JsonArray(order.getProducts()).encode())
      .add(order.getTotalPrice());
    executeNoResult(params, INSERT_STATEMENT, resultHandler);
    return this;
  }

  @Override
  public OrderService retrieveOrder(Long orderId, Handler<AsyncResult<Order>> resultHandler) {
    retrieveOne(orderId, RETRIEVE_BY_OID_STATEMENT)
      .map(option -> option.map(Order::new).orElse(null))
      .setHandler(resultHandler);
    return this;
  }

  // SQL statement

  private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS `order` (\n" +
    "  `orderId` bigint(20) NOT NULL,\n" +
    "  `payId` varchar(24) NOT NULL,\n" +
    "  `buyerId` varchar(20) NOT NULL,\n" +
    "  `createTime` bigint(20) NOT NULL,\n" +
    "  `products` varchar(512) NOT NULL,\n" +
    "  `totalPrice` double NOT NULL,\n" +
    "  PRIMARY KEY (`orderId`),\n" +
    "  KEY `INDEX_BUYER` (`buyerId`),\n" +
    "  KEY `INDEX_PAY` (`payId`) )";
  private static final String INSERT_STATEMENT = "INSERT INTO `order` VALUES (?, ?, ?, ?, ?, ?)";
  private static final String RETRIEVE_BY_ACCOUNT_STATEMENT = "SELECT * FROM `order` WHERE buyerId = ?";
  private static final String RETRIEVE_BY_OID_STATEMENT = "SELECT * FROM `order` WHERE orderId = ?";
}

/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.order.Order}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.order.Order} original class using Vert.x codegen.
 */
public class OrderConverter {

  public static void fromJson(JsonObject json, Order obj) {
    if (json.getValue("buyerId") instanceof String) {
      obj.setBuyerId((String)json.getValue("buyerId"));
    }
    if (json.getValue("createTime") instanceof Number) {
      obj.setCreateTime(((Number)json.getValue("createTime")).longValue());
    }
    if (json.getValue("orderId") instanceof Number) {
      obj.setOrderId(((Number)json.getValue("orderId")).longValue());
    }
    if (json.getValue("payId") instanceof String) {
      obj.setPayId((String)json.getValue("payId"));
    }
    if (json.getValue("products") instanceof JsonArray) {
      java.util.ArrayList<io.vertx.blueprint.microservice.product.ProductTuple> list = new java.util.ArrayList<>();
      json.getJsonArray("products").forEach( item -> {
        if (item instanceof JsonObject)
          list.add(new io.vertx.blueprint.microservice.product.ProductTuple((JsonObject)item));
      });
      obj.setProducts(list);
    }
    if (json.getValue("totalPrice") instanceof Number) {
      obj.setTotalPrice(((Number)json.getValue("totalPrice")).doubleValue());
    }
  }

  public static void toJson(Order obj, JsonObject json) {
    if (obj.getBuyerId() != null) {
      json.put("buyerId", obj.getBuyerId());
    }
    if (obj.getCreateTime() != null) {
      json.put("createTime", obj.getCreateTime());
    }
    if (obj.getOrderId() != null) {
      json.put("orderId", obj.getOrderId());
    }
    if (obj.getPayId() != null) {
      json.put("payId", obj.getPayId());
    }
    if (obj.getProducts() != null) {
      JsonArray array = new JsonArray();
      obj.getProducts().forEach(item -> array.add(item.toJson()));
      json.put("products", array);
    }
    if (obj.getTotalPrice() != null) {
      json.put("totalPrice", obj.getTotalPrice());
    }
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.order.OrderService;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import java.util.List;
import io.vertx.blueprint.microservice.order.OrderService;
import io.vertx.blueprint.microservice.order.Order;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceVertxProxyHandler extends ProxyHandler {

  public static final long DEFAULT_CONNECTION_TIMEOUT = 5 * 60; // 5 minutes 

  private final Vertx vertx;
  private final OrderService service;
  private final long timerID;
  private long lastAccessed;
  private final long timeoutSeconds;

  public OrderServiceVertxProxyHandler(Vertx vertx, OrderService service) {
    this(vertx, service, DEFAULT_CONNECTION_TIMEOUT);
  }

  public OrderServiceVertxProxyHandler(Vertx vertx, OrderService service, long timeoutInSecond) {
    this(vertx, service, true, timeoutInSecond);
  }

  public OrderServiceVertxProxyHandler(Vertx vertx, OrderService service, boolean topLevel, long timeoutSeconds) {
    this.vertx = vertx;
    this.service = service;
    this.timeoutSeconds = timeoutSeconds;
    try {
      this.vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
    if (timeoutSeconds != -1 && !topLevel) {
      long period = timeoutSeconds * 1000 / 2;
      if (period > 10000) {
        period = 10000;
      }
      this.timerID = vertx.setPeriodic(period, this::checkTimedOut);
    } else {
      this.timerID = -1;
    }
    accessed();
  }

  public MessageConsumer<JsonObject> registerHandler(String address) {
    MessageConsumer<JsonObject> consumer = vertx.eventBus().<JsonObject>consumer(address).handler(this);
    this.setConsumer(consumer);
    return consumer;
  }

  private void checkTimedOut(long id) {
    long now = System.nanoTime();
    if (now - lastAccessed > timeoutSeconds * 1000000000) {
      close();
    }
  }

  @Override
  public void close() {
    if (timerID != -1) {
      vertx.cancelTimer(timerID);
    }
    super.close();
  }

  private void accessed() {
    this.lastAccessed = System.nanoTime();
  }

  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "initializePersistence": {
          service.initializePersistence(createHandler(msg));
          break;
        }
        case "retrieveOrdersForAccount": {
          service.retrieveOrdersForAccount((java.lang.String)json.getValue("accountId"), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(new JsonArray(res.result().stream().map(Order::toJson).collect(Collectors.toList())));
            }
         });
          break;
        }
        case "createOrder": {
          service.createOrder(json.getJsonObject("order") == null ? null : new io.vertx.blueprint.microservice.order.Order(json.getJsonObject("order")), createHandler(msg));
          break;
        }
        case "retrieveOrder": {
          service.retrieveOrder(json.getValue("orderId") == null ? null : (json.getLong("orderId").longValue()), res -> {
            if (res.failed()) {
              if (res.cause() instanceof ServiceException) {
                msg.reply(res.cause());
              } else {
                msg.reply(new ServiceException(-1, res.cause().getMessage()));
              }
            } else {
              msg.reply(res.result() == null ? null : res.result().toJson());
            }
         });
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }

  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        if (res.result() != null  && res.result().getClass().isEnum()) {
          msg.reply(((Enum) res.result()).name());
        } else {
          msg.reply(res.result());
        }
      }
    };
  }

  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }

  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }

  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        if (res.cause() instanceof ServiceException) {
          msg.reply(res.cause());
        } else {
          msg.reply(new ServiceException(-1, res.cause().getMessage()));
        }
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int) chr);
        }
        msg.reply(arr);
      }
    };
  }

  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }

  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }

  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}
/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/


import io.vertx.blueprint.microservice.order.OrderService;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.serviceproxy.ServiceExceptionMessageCodec;
import java.util.List;
import io.vertx.blueprint.microservice.order.OrderService;
import io.vertx.blueprint.microservice.order.Order;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceVertxEBProxy implements OrderService {

  private Vertx _vertx;
  private String _address;
  private DeliveryOptions _options;
  private boolean closed;

  public OrderServiceVertxEBProxy(Vertx vertx, String address) {
    this(vertx, address, null);
  }

  public OrderServiceVertxEBProxy(Vertx vertx, String address, DeliveryOptions options) {
    this._vertx = vertx;
    this._address = address;
    this._options = options;
    try {
      this._vertx.eventBus().registerDefaultCodec(ServiceException.class,
          new ServiceExceptionMessageCodec());
    } catch (IllegalStateException ex) {}
  }

  public OrderService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "initializePersistence");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public OrderService retrieveOrdersForAccount(String accountId, Handler<AsyncResult<List<Order>>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("accountId", accountId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveOrdersForAccount");
    _vertx.eventBus().<JsonArray>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body().stream().map(o -> o instanceof Map ? new Order(new JsonObject((Map) o)) : new Order((JsonObject) o)).collect(Collectors.toList())));
      }
    });
    return this;
  }

  public OrderService createOrder(Order order, Handler<AsyncResult<Void>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("order", order == null ? null : order.toJson());
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "createOrder");
    _vertx.eventBus().<Void>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
    return this;
  }

  public OrderService retrieveOrder(Long orderId, Handler<AsyncResult<Order>> resultHandler) {
    if (closed) {
      resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return this;
    }
    JsonObject _json = new JsonObject();
    _json.put("orderId", orderId);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "retrieveOrder");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body() == null ? null : new Order(res.result().body())));
                      }
    });
    return this;
  }


  private List<Character> convertToListChar(JsonArray arr) {
    List<Character> list = new ArrayList<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      list.add((char)(int)jobj);
    }
    return list;
  }

  private Set<Character> convertToSetChar(JsonArray arr) {
    Set<Character> set = new HashSet<>();
    for (Object obj: arr) {
      Integer jobj = (Integer)obj;
      set.add((char)(int)jobj);
    }
    return set;
  }

  private <T> Map<String, T> convertMap(Map map) {
    if (map.isEmpty()) { 
      return (Map<String, T>) map; 
    } 
     
    Object elem = map.values().stream().findFirst().get(); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (Map<String, T>) map; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return ((Map<String, T>) map).entrySet() 
       .stream() 
       .collect(Collectors.toMap(Map.Entry::getKey, converter::apply)); 
    } 
  }
  private <T> List<T> convertList(List list) {
    if (list.isEmpty()) { 
          return (List<T>) list; 
        } 
     
    Object elem = list.get(0); 
    if (!(elem instanceof Map) && !(elem instanceof List)) { 
      return (List<T>) list; 
    } else { 
      Function<Object, T> converter; 
      if (elem instanceof List) { 
        converter = object -> (T) new JsonArray((List) object); 
      } else { 
        converter = object -> (T) new JsonObject((Map) object); 
      } 
      return (List<T>) list.stream().map(converter).collect(Collectors.toList()); 
    } 
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>(convertList(list));
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.blueprint.microservice.order.CheckoutResult}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.order.CheckoutResult} original class using Vert.x codegen.
 */
public class CheckoutResultConverter {

  public static void fromJson(JsonObject json, CheckoutResult obj) {
    if (json.getValue("message") instanceof String) {
      obj.setMessage((String)json.getValue("message"));
    }
    if (json.getValue("order") instanceof JsonObject) {
      obj.setOrder(new io.vertx.blueprint.microservice.order.Order((JsonObject)json.getValue("order")));
    }
  }

  public static void toJson(CheckoutResult obj, JsonObject json) {
    if (obj.getMessage() != null) {
      json.put("message", obj.getMessage());
    }
    if (obj.getOrder() != null) {
      json.put("order", obj.getOrder().toJson());
    }
  }
}
/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.Map;
import rx.Observable;
import rx.Single;
import java.util.List;
import io.vertx.blueprint.microservice.order.Order;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing order storage operations.
 * <p>
 * This service is an event bus service (aka. service proxy).
 * </p>
 *
 * <p/>
 * NOTE: This class has been automatically generated from the {@link io.vertx.blueprint.microservice.order.OrderService original} non RX-ified interface using Vert.x codegen.
 */

@io.vertx.lang.rxjava.RxGen(io.vertx.blueprint.microservice.order.OrderService.class)
public class OrderService {

  public static final io.vertx.lang.rxjava.TypeArg<OrderService> __TYPE_ARG = new io.vertx.lang.rxjava.TypeArg<>(
    obj -> new OrderService((io.vertx.blueprint.microservice.order.OrderService) obj),
    OrderService::getDelegate
  );

  private final io.vertx.blueprint.microservice.order.OrderService delegate;
  
  public OrderService(io.vertx.blueprint.microservice.order.OrderService delegate) {
    this.delegate = delegate;
  }

  public io.vertx.blueprint.microservice.order.OrderService getDelegate() {
    return delegate;
  }

  /**
   * Initialize the persistence.
   * @param resultHandler async result handler
   * @return 
   */
  public OrderService initializePersistence(Handler<AsyncResult<Void>> resultHandler) { 
    delegate.initializePersistence(resultHandler);
    return this;
  }

  /**
   * Initialize the persistence.
   * @return 
   */
  public Single<Void> rxInitializePersistence() { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      initializePersistence(fut);
    }));
  }

  /**
   * Retrieve orders belonging to a certain account.
   * @param accountId account id
   * @param resultHandler async result handler
   * @return 
   */
  public OrderService retrieveOrdersForAccount(String accountId, Handler<AsyncResult<List<Order>>> resultHandler) { 
    delegate.retrieveOrdersForAccount(accountId, resultHandler);
    return this;
  }

  /**
   * Retrieve orders belonging to a certain account.
   * @param accountId account id
   * @return 
   */
  public Single<List<Order>> rxRetrieveOrdersForAccount(String accountId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveOrdersForAccount(accountId, fut);
    }));
  }

  /**
   * Save an order into the persistence.
   * @param order order data object
   * @param resultHandler async result handler
   * @return 
   */
  public OrderService createOrder(Order order, Handler<AsyncResult<Void>> resultHandler) { 
    delegate.createOrder(order, resultHandler);
    return this;
  }

  /**
   * Save an order into the persistence.
   * @param order order data object
   * @return 
   */
  public Single<Void> rxCreateOrder(Order order) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      createOrder(order, fut);
    }));
  }

  /**
   * Retrieve the order with a certain <code>orderId</code>.
   * @param orderId order id
   * @param resultHandler async result handler
   * @return 
   */
  public OrderService retrieveOrder(Long orderId, Handler<AsyncResult<Order>> resultHandler) { 
    delegate.retrieveOrder(orderId, resultHandler);
    return this;
  }

  /**
   * Retrieve the order with a certain <code>orderId</code>.
   * @param orderId order id
   * @return 
   */
  public Single<Order> rxRetrieveOrder(Long orderId) { 
    return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<>(fut -> {
      retrieveOrder(orderId, fut);
    }));
  }


  public static OrderService newInstance(io.vertx.blueprint.microservice.order.OrderService arg) {
    return arg != null ? new OrderService(arg) : null;
  }
}

