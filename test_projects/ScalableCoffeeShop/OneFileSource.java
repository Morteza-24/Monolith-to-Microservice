
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.logging.Logger;

public class LoggerProducer {

    @Produces
    public Logger exposeLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
    }

}


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("resources")
public class JAXRSConfiguration extends Application {

    // nothing to configure

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.logging.Logger;

public class EventDeserializer implements Deserializer<CoffeeEvent> {

    private static final Logger logger = Logger.getLogger(EventDeserializer.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // nothing to configure
    }

    @Override
    public CoffeeEvent deserialize(final String topic, final byte[] data) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            final JsonObject jsonObject = Json.createReader(input).readObject();
            final Class<? extends CoffeeEvent> eventClass = (Class<? extends CoffeeEvent>) Class.forName(jsonObject.getString("class"));
            return eventClass.getConstructor(JsonObject.class).newInstance(jsonObject.getJsonObject("data"));
        } catch (Exception e) {
            logger.severe("Could not deserialize event: " + e.getMessage());
            throw new SerializationException("Could not deserialize event", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.ProducerFencedException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class EventProducer {

    private Producer<String, CoffeeEvent> producer;
    private String topic;

    @Inject
    Properties kafkaProperties;

    @Inject
    Logger logger;

    @PostConstruct
    private void init() {
        kafkaProperties.put("transactional.id", UUID.randomUUID().toString());
        producer = new KafkaProducer<>(kafkaProperties);
        topic = kafkaProperties.getProperty("orders.topic");
        producer.initTransactions();
    }

    public void publish(CoffeeEvent... events) {
        try {
            producer.beginTransaction();
            send(events);
            producer.commitTransaction();
        } catch (ProducerFencedException e) {
            producer.close();
        } catch (KafkaException e) {
            producer.abortTransaction();
        }
    }

    private void send(CoffeeEvent... events) {
        for (final CoffeeEvent event : events) {
            final ProducerRecord<String, CoffeeEvent> record = new ProducerRecord<>(topic, event);
            logger.info("publishing = " + record);
            producer.send(record);
        }
    }

    @PreDestroy
    public void close() {
        producer.close();
    }

}



import javax.json.bind.adapter.JsonbAdapter;
import java.util.UUID;

public class UUIDAdapter implements JsonbAdapter<UUID, String> {

    @Override
    public String adaptToJson(UUID uuid) {
        return uuid.toString();
    }

    @Override
    public UUID adaptFromJson(String string) {
        return UUID.fromString(string);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class EventSerializer implements Serializer<CoffeeEvent> {

    private static final Logger logger = Logger.getLogger(EventSerializer.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // nothing to configure
    }

    @Override
    public byte[] serialize(final String topic, final CoffeeEvent event) {
        try {
            if (event == null)
                return null;

            final JsonbConfig config = new JsonbConfig()
                    .withAdapters(new UUIDAdapter())
                    .withSerializers(new EventJsonbSerializer());

            final Jsonb jsonb = JsonbBuilder.create(config);

            return jsonb.toJson(event, CoffeeEvent.class).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Could not serialize event: " + e.getMessage());
            throw new SerializationException("Could not serialize event", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

}


import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.util.Properties;

@ApplicationScoped
public class KafkaConfigurator {

    private Properties kafkaProperties;

    @PostConstruct
    private void initProperties() {
        try {
            kafkaProperties = new Properties();
            kafkaProperties.load(KafkaConfigurator.class.getResourceAsStream("/kafka.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Produces
    @RequestScoped
    public Properties exposeKafkaProperties() throws IOException {
        final Properties properties = new Properties();
        properties.putAll(kafkaProperties);
        return properties;
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

public class EventConsumer implements Runnable {

    private final KafkaConsumer<String, CoffeeEvent> consumer;
    private final Consumer<CoffeeEvent> eventConsumer;
    private final AtomicBoolean closed = new AtomicBoolean();

    public EventConsumer(Properties kafkaProperties, Consumer<CoffeeEvent> eventConsumer, String... topics) {
        this.eventConsumer = eventConsumer;
        consumer = new KafkaConsumer<>(kafkaProperties);
        consumer.subscribe(asList(topics));
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                consume();
            }
        } catch (WakeupException e) {
            // will wakeup for closing
        } finally {
            consumer.close();
        }
    }

    private void consume() {
        ConsumerRecords<String, CoffeeEvent> records = consumer.poll(Long.MAX_VALUE);
        for (ConsumerRecord<String, CoffeeEvent> record : records) {
            eventConsumer.accept(record.value());
        }
        consumer.commitSync();
    }

    public void stop() {
        closed.set(true);
        consumer.wakeup();
    }

}



import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

public class EventJsonbSerializer implements JsonbSerializer<CoffeeEvent> {

    @Override
    public void serialize(final CoffeeEvent event, final JsonGenerator generator, final SerializationContext ctx) {
        generator.writeStartObject();
        generator.write("class", event.getClass().getCanonicalName());
        ctx.serialize("data", event, generator);
        generator.writeEnd();
        generator.close();
    }

}


import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.Objects;

public abstract class CoffeeEvent {

    @JsonbProperty
    private final Instant instant;

    protected CoffeeEvent() {
        instant = Instant.now();
    }

    protected CoffeeEvent(final Instant instant) {
        Objects.requireNonNull(instant);
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CoffeeEvent that = (CoffeeEvent) o;

        return instant.equals(that.instant);
    }

    @Override
    public int hashCode() {
        return instant.hashCode();
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class BeansFetched extends CoffeeEvent {

    private final String beanOrigin;

    public BeansFetched(final String beanOrigin) {
        this.beanOrigin = beanOrigin;
    }

    public BeansFetched(final String beanOrigin, final Instant instant) {
        super(instant);
        this.beanOrigin = beanOrigin;
    }

    public BeansFetched(final JsonObject jsonObject) {
        this(jsonObject.getString("beanOrigin"), Instant.parse(jsonObject.getString("instant")));

    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderStarted extends CoffeeEvent {

    private final UUID orderId;

    public OrderStarted(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderStarted(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderStarted(final JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class OrderPlaced extends CoffeeEvent {

    private final OrderInfo orderInfo;

    public OrderPlaced(final OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public OrderPlaced(final OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public OrderPlaced(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.util.UUID;

public class OrderInfo {

    private final UUID orderId;
    private final CoffeeType type;
    private final String beanOrigin;

    public OrderInfo(final UUID orderId, final CoffeeType type, final String beanOrigin) {
        this.orderId = orderId;
        this.type = type;
        this.beanOrigin = beanOrigin;
    }

    public OrderInfo(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")),
                CoffeeType.fromString(jsonObject.getString("type")),
                jsonObject.getString("beanOrigin"));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public CoffeeType getType() {
        return type;
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class CoffeeBrewFinished extends CoffeeEvent {

    private final UUID orderId;

    public CoffeeBrewFinished(final UUID orderId) {
        this.orderId = orderId;
    }

    public CoffeeBrewFinished(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public CoffeeBrewFinished(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderDelivered extends CoffeeEvent {

    private final UUID orderId;

    public OrderDelivered(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderDelivered(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderDelivered(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.UUID;

public class CoffeeDelivered extends CoffeeEvent {

    @JsonbProperty
    private final UUID orderId;

    public CoffeeDelivered(final UUID orderId) {
        this.orderId = orderId;
    }

    public CoffeeDelivered(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public CoffeeDelivered(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final CoffeeDelivered that = (CoffeeDelivered) o;

        return orderId != null ? orderId.equals(that.orderId) : that.orderId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (orderId != null ? orderId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CoffeeDelivered{" +
                "instant=" + getInstant() +
                ", orderId=" + orderId +
                '}';
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class OrderAccepted extends CoffeeEvent {

    private final OrderInfo orderInfo;

    public OrderAccepted(final OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public OrderAccepted(final OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public OrderAccepted(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderCancelled extends CoffeeEvent {

    private final UUID orderId;
    private final String reason;

    public OrderCancelled(final UUID orderId, final String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public OrderCancelled(final UUID orderId, final String reason, Instant instant) {
        super(instant);
        this.orderId = orderId;
        this.reason = reason;
    }

    public OrderCancelled(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), jsonObject.getString("reason"), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

}


import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;

public class CoffeeBrewStarted extends CoffeeEvent {

    @JsonbProperty
    private final OrderInfo orderInfo;

    public CoffeeBrewStarted(OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public CoffeeBrewStarted(OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public CoffeeBrewStarted(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderFailedBeansNotAvailable extends CoffeeEvent {

    private final UUID orderId;

    public OrderFailedBeansNotAvailable(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderFailedBeansNotAvailable(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderFailedBeansNotAvailable(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderFinished extends CoffeeEvent {

    private final UUID orderId;

    public OrderFinished(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderFinished(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderFinished(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderBeansReserved extends CoffeeEvent {

    private final UUID orderId;

    public OrderBeansReserved(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderBeansReserved(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderBeansReserved(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class BeansStored extends CoffeeEvent {

    private final String beanOrigin;
    private final int amount;

    public BeansStored(final String beanOrigin, final int amount) {
        this.beanOrigin = beanOrigin;
        this.amount = amount;
    }

    public BeansStored(final String beanOrigin, final int amount, final Instant instant) {
        super(instant);
        this.beanOrigin = beanOrigin;
        this.amount = amount;
    }

    public BeansStored(JsonObject jsonObject) {
        this(jsonObject.getString("beanOrigin"), jsonObject.getInt("amount"), Instant.parse(jsonObject.getString("instant")));
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

    public int getAmount() {
        return amount;
    }

}


import java.util.stream.Stream;

public enum CoffeeType {

    ESPRESSO, POUR_OVER, FRENCH_PRESS;

    public static CoffeeType fromString(final String name) {
        return Stream.of(values())
                .filter(v -> v.name().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventConsumer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Properties;
import java.util.logging.Logger;

@Singleton
@Startup
public class OrderEventHandler {

    private EventConsumer eventConsumer;

    @Resource
    ManagedExecutorService mes;

    @Inject
    Properties kafkaProperties;

    @Inject
    Event<CoffeeEvent> events;

    @Inject
    OrderCommandService orderService;

    @Inject
    Logger logger;

    public void handle(@Observes OrderBeansReserved event) {
        orderService.acceptOrder(event.getOrderId());
    }

    public void handle(@Observes OrderFailedBeansNotAvailable event) {
        orderService.cancelOrder(event.getOrderId(), "No beans of the origin were available");
    }

    public void handle(@Observes CoffeeBrewStarted event) {
        orderService.startOrder(event.getOrderInfo().getOrderId());
    }

    public void handle(@Observes CoffeeBrewFinished event) {
        orderService.finishOrder(event.getOrderId());
    }

    public void handle(@Observes CoffeeDelivered event) {
        orderService.deliverOrder(event.getOrderId());
    }

    @PostConstruct
    private void initConsumer() {
        kafkaProperties.put("group.id", "order-handler");
        String barista = kafkaProperties.getProperty("barista.topic");
        String beans = kafkaProperties.getProperty("beans.topic");

        eventConsumer = new EventConsumer(kafkaProperties, ev -> {
            logger.info("firing = " + ev);
            events.fire(ev);
        }, barista, beans);

        mes.execute(eventConsumer);
    }

    @PreDestroy
    public void closeConsumer() {
        eventConsumer.stop();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventProducer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.*;
import com.sebastian_daschner.scalable_coffee_shop.orders.control.CoffeeOrders;

import javax.inject.Inject;
import java.util.UUID;

public class OrderCommandService {

    @Inject
    EventProducer eventProducer;

    @Inject
    CoffeeOrders coffeeOrders;

    public void placeOrder(final OrderInfo orderInfo) {
        eventProducer.publish(new OrderPlaced(orderInfo));
    }

    void acceptOrder(final UUID orderId) {
        final OrderInfo orderInfo = coffeeOrders.get(orderId).getOrderInfo();
        eventProducer.publish(new OrderAccepted(orderInfo));
    }

    void cancelOrder(final UUID orderId, final String reason) {
        eventProducer.publish(new OrderCancelled(orderId, reason));
    }

    void startOrder(final UUID orderId) {
        eventProducer.publish(new OrderStarted(orderId));
    }

    void finishOrder(final UUID orderId) {
        eventProducer.publish(new OrderFinished(orderId));
    }

    void deliverOrder(final UUID orderId) {
        eventProducer.publish(new OrderDelivered(orderId));
    }

}


import com.sebastian_daschner.scalable_coffee_shop.orders.control.CoffeeOrders;
import com.sebastian_daschner.scalable_coffee_shop.orders.entity.CoffeeOrder;

import javax.inject.Inject;
import java.util.UUID;

public class OrderQueryService {

    @Inject
    CoffeeOrders coffeeOrders;

    public CoffeeOrder getOrder(final UUID orderId) {
        return coffeeOrders.get(orderId);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeType;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderInfo;
import com.sebastian_daschner.scalable_coffee_shop.orders.entity.CoffeeOrder;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

@Path("orders")
public class OrdersResource {

    @Inject
    OrderCommandService commandService;

    @Inject
    OrderQueryService queryService;

    @Context
    UriInfo uriInfo;

    @POST
    public Response orderCoffee(JsonObject order) {
        final String beanOrigin = order.getString("beanOrigin", null);
        final CoffeeType coffeeType = CoffeeType.fromString(order.getString("coffeeType", null));

        if (beanOrigin == null || coffeeType == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        final UUID orderId = UUID.randomUUID();
        commandService.placeOrder(new OrderInfo(orderId, coffeeType, beanOrigin));

        final URI uri = uriInfo.getRequestUriBuilder().path(OrdersResource.class, "getOrder").build(orderId);
        return Response.accepted().header(HttpHeaders.LOCATION, uri).build();
    }

    @GET
    @Path("{id}")
    public JsonObject getOrder(@PathParam("id") UUID orderId) {
        final CoffeeOrder order = queryService.getOrder(orderId);

        if (order == null)
            throw new NotFoundException();

        return Json.createObjectBuilder()
                .add("status", order.getState().name().toLowerCase())
                .add("type", order.getOrderInfo().getType().name().toLowerCase())
                .add("beanOrigin", order.getOrderInfo().getBeanOrigin())
                .build();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventConsumer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

@Startup
@Singleton
public class OrderUpdateConsumer {

    private EventConsumer eventConsumer;

    @Resource
    ManagedExecutorService mes;

    @Inject
    Properties kafkaProperties;

    @Inject
    Event<CoffeeEvent> events;

    @Inject
    Logger logger;

    @PostConstruct
    private void init() {
        kafkaProperties.put("group.id", "order-consumer-" + UUID.randomUUID());
        String orders = kafkaProperties.getProperty("orders.topic");

        eventConsumer = new EventConsumer(kafkaProperties, ev -> {
            logger.info("firing = " + ev);
            events.fire(ev);
        }, orders);

        mes.execute(eventConsumer);
    }

    @PreDestroy
    public void close() {
        eventConsumer.stop();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.*;
import com.sebastian_daschner.scalable_coffee_shop.orders.entity.CoffeeOrder;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Observes;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CoffeeOrders {

    private Map<UUID, CoffeeOrder> coffeeOrders = new ConcurrentHashMap<>();

    public CoffeeOrder get(final UUID orderId) {
        return coffeeOrders.get(orderId);
    }

    public void apply(@Observes OrderPlaced event) {
        coffeeOrders.putIfAbsent(event.getOrderInfo().getOrderId(), new CoffeeOrder());
        applyFor(event.getOrderInfo().getOrderId(), o -> o.place(event.getOrderInfo()));
    }

    public void apply(@Observes OrderCancelled event) {
        applyFor(event.getOrderId(), CoffeeOrder::cancel);
    }

    public void apply(@Observes OrderAccepted event) {
        applyFor(event.getOrderInfo().getOrderId(), CoffeeOrder::accept);
    }

    public void apply(@Observes OrderStarted event) {
        applyFor(event.getOrderId(), CoffeeOrder::start);
    }

    public void apply(@Observes OrderFinished event) {
        applyFor(event.getOrderId(), CoffeeOrder::finish);
    }

    public void apply(@Observes OrderDelivered event) {
        applyFor(event.getOrderId(), CoffeeOrder::deliver);
    }

    private void applyFor(final UUID orderId, final Consumer<CoffeeOrder> consumer) {
        final CoffeeOrder coffeeOrder = coffeeOrders.get(orderId);
        if (coffeeOrder != null)
            consumer.accept(coffeeOrder);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderInfo;

public class CoffeeOrder {

    private CoffeeOrderState state;
    private OrderInfo orderInfo;

    public void place(final OrderInfo orderInfo) {
        state = CoffeeOrderState.PLACED;
        this.orderInfo = orderInfo;
    }

    public void accept() {
        state = CoffeeOrderState.ACCEPTED;
    }

    public void cancel() {
        state = CoffeeOrderState.CANCELLED;
    }

    public void start() {
        state = CoffeeOrderState.STARTED;
    }

    public void finish() {
        state = CoffeeOrderState.FINISHED;
    }

    public void deliver() {
        state = CoffeeOrderState.DELIVERED;
    }

    public CoffeeOrderState getState() {
        return state;
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

    public enum CoffeeOrderState {
        PLACED,
        ACCEPTED,
        STARTED,
        FINISHED,
        DELIVERED,
        CANCELLED
    }

}


import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.logging.Logger;

public class LoggerProducer {

    @Produces
    public Logger exposeLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
    }

}


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("resources")
public class JAXRSConfiguration extends Application {

    // nothing to configure

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.logging.Logger;

public class EventDeserializer implements Deserializer<CoffeeEvent> {

    private static final Logger logger = Logger.getLogger(EventDeserializer.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // nothing to configure
    }

    @Override
    public CoffeeEvent deserialize(final String topic, final byte[] data) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            final JsonObject jsonObject = Json.createReader(input).readObject();
            final Class<? extends CoffeeEvent> eventClass = (Class<? extends CoffeeEvent>) Class.forName(jsonObject.getString("class"));
            return eventClass.getConstructor(JsonObject.class).newInstance(jsonObject.getJsonObject("data"));
        } catch (Exception e) {
            logger.severe("Could not deserialize event: " + e.getMessage());
            throw new SerializationException("Could not deserialize event", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.ProducerFencedException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class EventProducer {

    private Producer<String, CoffeeEvent> producer;
    private String topic;

    @Inject
    Properties kafkaProperties;

    @Inject
    Logger logger;

    @PostConstruct
    private void init() {
        kafkaProperties.put("transactional.id", UUID.randomUUID().toString());
        producer = new KafkaProducer<>(kafkaProperties);
        topic = kafkaProperties.getProperty("beans.topic");
        producer.initTransactions();
    }

    public void publish(CoffeeEvent... events) {
        try {
            producer.beginTransaction();
            send(events);
            producer.commitTransaction();
        } catch (ProducerFencedException e) {
            producer.close();
        } catch (KafkaException e) {
            producer.abortTransaction();
        }
    }

    private void send(CoffeeEvent... events) {
        for (final CoffeeEvent event : events) {
            final ProducerRecord<String, CoffeeEvent> record = new ProducerRecord<>(topic, event);
            logger.info("publishing = " + record);
            producer.send(record);
        }
    }

    @PreDestroy
    public void close() {
        producer.close();
    }

}



import javax.json.bind.adapter.JsonbAdapter;
import java.util.UUID;

public class UUIDAdapter implements JsonbAdapter<UUID, String> {

    @Override
    public String adaptToJson(UUID uuid) {
        return uuid.toString();
    }

    @Override
    public UUID adaptFromJson(String string) {
        return UUID.fromString(string);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class EventSerializer implements Serializer<CoffeeEvent> {

    private static final Logger logger = Logger.getLogger(EventSerializer.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // nothing to configure
    }

    @Override
    public byte[] serialize(final String topic, final CoffeeEvent event) {
        try {
            if (event == null)
                return null;

            final JsonbConfig config = new JsonbConfig()
                    .withAdapters(new UUIDAdapter())
                    .withSerializers(new EventJsonbSerializer());

            final Jsonb jsonb = JsonbBuilder.create(config);

            return jsonb.toJson(event, CoffeeEvent.class).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Could not serialize event: " + e.getMessage());
            throw new SerializationException("Could not serialize event", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

}


import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.util.Properties;

@ApplicationScoped
public class KafkaConfigurator {

    private Properties kafkaProperties;

    @PostConstruct
    private void initProperties() {
        try {
            kafkaProperties = new Properties();
            kafkaProperties.load(KafkaConfigurator.class.getResourceAsStream("/kafka.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Produces
    @RequestScoped
    public Properties exposeKafkaProperties() throws IOException {
        final Properties properties = new Properties();
        properties.putAll(kafkaProperties);
        return properties;
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

public class EventConsumer implements Runnable {

    private final KafkaConsumer<String, CoffeeEvent> consumer;
    private final Consumer<CoffeeEvent> eventConsumer;
    private final AtomicBoolean closed = new AtomicBoolean();

    public EventConsumer(Properties kafkaProperties, Consumer<CoffeeEvent> eventConsumer, String... topics) {
        this.eventConsumer = eventConsumer;
        consumer = new KafkaConsumer<>(kafkaProperties);
        consumer.subscribe(asList(topics));
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                consume();
            }
        } catch (WakeupException e) {
            // will wakeup for closing
        } finally {
            consumer.close();
        }
    }

    private void consume() {
        ConsumerRecords<String, CoffeeEvent> records = consumer.poll(Long.MAX_VALUE);
        for (ConsumerRecord<String, CoffeeEvent> record : records) {
            eventConsumer.accept(record.value());
        }
        consumer.commitSync();
    }

    public void stop() {
        closed.set(true);
        consumer.wakeup();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

public class EventJsonbSerializer implements JsonbSerializer<CoffeeEvent> {

    @Override
    public void serialize(final CoffeeEvent event, final JsonGenerator generator, final SerializationContext ctx) {
        generator.writeStartObject();
        generator.write("class", event.getClass().getCanonicalName());
        ctx.serialize("data", event, generator);
        generator.writeEnd();
        generator.close();
    }

}


import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.Objects;

public abstract class CoffeeEvent {

    @JsonbProperty
    private final Instant instant;

    protected CoffeeEvent() {
        instant = Instant.now();
    }

    protected CoffeeEvent(final Instant instant) {
        Objects.requireNonNull(instant);
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CoffeeEvent that = (CoffeeEvent) o;

        return instant.equals(that.instant);
    }

    @Override
    public int hashCode() {
        return instant.hashCode();
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class BeansFetched extends CoffeeEvent {

    private final String beanOrigin;

    public BeansFetched(final String beanOrigin) {
        this.beanOrigin = beanOrigin;
    }

    public BeansFetched(final String beanOrigin, final Instant instant) {
        super(instant);
        this.beanOrigin = beanOrigin;
    }

    public BeansFetched(final JsonObject jsonObject) {
        this(jsonObject.getString("beanOrigin"), Instant.parse(jsonObject.getString("instant")));
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderStarted extends CoffeeEvent {

    private final UUID orderId;

    public OrderStarted(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderStarted(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderStarted(final JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class OrderPlaced extends CoffeeEvent {

    private final OrderInfo orderInfo;

    public OrderPlaced(final OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public OrderPlaced(final OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public OrderPlaced(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.util.UUID;

public class OrderInfo {

    private final UUID orderId;
    private final CoffeeType type;
    private final String beanOrigin;

    public OrderInfo(final UUID orderId, final CoffeeType type, final String beanOrigin) {
        this.orderId = orderId;
        this.type = type;
        this.beanOrigin = beanOrigin;
    }

    public OrderInfo(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")),
                CoffeeType.fromString(jsonObject.getString("type")),
                jsonObject.getString("beanOrigin"));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public CoffeeType getType() {
        return type;
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class CoffeeBrewFinished extends CoffeeEvent {

    private final UUID orderId;

    public CoffeeBrewFinished(final UUID orderId) {
        this.orderId = orderId;
    }

    public CoffeeBrewFinished(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public CoffeeBrewFinished(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderDelivered extends CoffeeEvent {

    private final UUID orderId;

    public OrderDelivered(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderDelivered(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderDelivered(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.UUID;

public class CoffeeDelivered extends CoffeeEvent {

    @JsonbProperty
    private final UUID orderId;

    public CoffeeDelivered(final UUID orderId) {
        this.orderId = orderId;
    }

    public CoffeeDelivered(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public CoffeeDelivered(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final CoffeeDelivered that = (CoffeeDelivered) o;

        return orderId != null ? orderId.equals(that.orderId) : that.orderId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (orderId != null ? orderId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CoffeeDelivered{" +
                "instant=" + getInstant() +
                ", orderId=" + orderId +
                '}';
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class OrderAccepted extends CoffeeEvent {

    private final OrderInfo orderInfo;

    public OrderAccepted(final OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public OrderAccepted(final OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public OrderAccepted(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderCancelled extends CoffeeEvent {

    private final UUID orderId;
    private final String reason;

    public OrderCancelled(final UUID orderId, final String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public OrderCancelled(final UUID orderId, final String reason, Instant instant) {
        super(instant);
        this.orderId = orderId;
        this.reason = reason;
    }

    public OrderCancelled(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), jsonObject.getString("reason"), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

}


import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;

public class CoffeeBrewStarted extends CoffeeEvent {

    @JsonbProperty
    private final OrderInfo orderInfo;

    public CoffeeBrewStarted(OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public CoffeeBrewStarted(OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public CoffeeBrewStarted(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderFailedBeansNotAvailable extends CoffeeEvent {

    private final UUID orderId;

    public OrderFailedBeansNotAvailable(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderFailedBeansNotAvailable(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderFailedBeansNotAvailable(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderFinished extends CoffeeEvent {

    private final UUID orderId;

    public OrderFinished(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderFinished(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderFinished(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderBeansReserved extends CoffeeEvent {

    private final UUID orderId;

    public OrderBeansReserved(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderBeansReserved(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderBeansReserved(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class BeansStored extends CoffeeEvent {

    private final String beanOrigin;
    private final int amount;

    public BeansStored(final String beanOrigin, final int amount) {
        this.beanOrigin = beanOrigin;
        this.amount = amount;
    }

    public BeansStored(final String beanOrigin, final int amount, final Instant instant) {
        super(instant);
        this.beanOrigin = beanOrigin;
        this.amount = amount;
    }

    public BeansStored(JsonObject jsonObject) {
        this(jsonObject.getString("beanOrigin"), jsonObject.getInt("amount"), Instant.parse(jsonObject.getString("instant")));
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

    public int getAmount() {
        return amount;
    }

}


import java.util.stream.Stream;

public enum CoffeeType {

    ESPRESSO, POUR_OVER, FRENCH_PRESS;

    public static CoffeeType fromString(final String name) {
        return Stream.of(values())
                .filter(v -> v.name().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

}


import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("beans")
public class BeansResource {

    @Inject
    BeanCommandService commandService;

    @Inject
    BeanQueryService queryService;

    @GET
    public JsonObject getBeans() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        queryService.getStoredBeans()
                .entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    @POST
    public void storeBeans(JsonObject object) {
        final String beanOrigin = object.getString("beanOrigin", null);
        final int amount = object.getInt("amount", 0);

        if (beanOrigin == null || amount == 0)
            throw new BadRequestException();

        commandService.storeBeans(beanOrigin, amount);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.beans.control.BeanStorage;

import javax.inject.Inject;
import java.util.Map;

public class BeanQueryService {

    @Inject
    BeanStorage beanStorage;

    public Map<String, Integer> getStoredBeans() {
        return beanStorage.getStoredBeans();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventConsumer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderPlaced;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Properties;
import java.util.logging.Logger;

@Singleton
@Startup
public class BeanEventHandler {

    private EventConsumer eventConsumer;

    @Resource
    ManagedExecutorService mes;

    @Inject
    Properties kafkaProperties;

    @Inject
    Event<CoffeeEvent> events;

    @Inject
    BeanCommandService beanService;

    @Inject
    Logger logger;

    public void handle(@Observes OrderPlaced event) {
        beanService.reserveBeans(event.getOrderInfo().getBeanOrigin(), event.getOrderInfo().getOrderId());
    }

    @PostConstruct
    private void initConsumer() {
        kafkaProperties.put("group.id", "beans-handler");
        String orders = kafkaProperties.getProperty("orders.topic");
        String barista = kafkaProperties.getProperty("barista.topic");

        eventConsumer = new EventConsumer(kafkaProperties, ev -> {
            logger.info("firing = " + ev);
            events.fire(ev);
        }, orders, barista);

        mes.execute(eventConsumer);
    }

    @PreDestroy
    public void closeConsumer() {
        eventConsumer.stop();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.beans.control.BeanStorage;
import com.sebastian_daschner.scalable_coffee_shop.events.control.EventProducer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.BeansFetched;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.BeansStored;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderBeansReserved;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderFailedBeansNotAvailable;

import javax.inject.Inject;
import java.util.UUID;

public class BeanCommandService {

    @Inject
    EventProducer eventProducer;

    @Inject
    BeanStorage beanStorage;

    public void storeBeans(final String beanOrigin, final int amount) {
        eventProducer.publish(new BeansStored(beanOrigin, amount));
    }

    void reserveBeans(final String beanOrigin, final UUID orderId) {
        if (beanStorage.getRemainingAmount(beanOrigin) > 0)
            eventProducer.publish(new OrderBeansReserved(orderId), new BeansFetched(beanOrigin));
        else
            eventProducer.publish(new OrderFailedBeansNotAvailable(orderId));
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventConsumer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

@Startup
@Singleton
public class BeanUpdateConsumer {

    private EventConsumer eventConsumer;

    @Resource
    ManagedExecutorService mes;

    @Inject
    Properties kafkaProperties;

    @Inject
    Event<CoffeeEvent> events;

    @Inject
    Logger logger;

    @PostConstruct
    private void init() {
        kafkaProperties.put("group.id", "beans-consumer-" + UUID.randomUUID());
        String beans = kafkaProperties.getProperty("beans.topic");

        eventConsumer = new EventConsumer(kafkaProperties, ev -> {
            logger.info("firing = " + ev);
            events.fire(ev);
        }, beans);

        mes.execute(eventConsumer);
    }

    @PreDestroy
    public void close() {
        eventConsumer.stop();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.BeansFetched;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.BeansStored;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Observes;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class BeanStorage {

    private Map<String, Integer> beanOrigins = new ConcurrentHashMap<>();

    public Map<String, Integer> getStoredBeans() {
        return Collections.unmodifiableMap(beanOrigins);
    }

    public int getRemainingAmount(final String beanOrigin) {
        return beanOrigins.getOrDefault(beanOrigin, 0);
    }

    public void apply(@Observes BeansStored beansStored) {
        beanOrigins.merge(beansStored.getBeanOrigin(), beansStored.getAmount(), Math::addExact);
    }

    public void apply(@Observes BeansFetched beansFetched) {
        beanOrigins.merge(beansFetched.getBeanOrigin(), 0, (i1, i2) -> i1 - 1);
    }

}


import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.logging.Logger;

public class LoggerProducer {

    @Produces
    public Logger exposeLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
    }

}


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("resources")
public class JAXRSConfiguration extends Application {

    // nothing to configure

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.logging.Logger;

public class EventDeserializer implements Deserializer<CoffeeEvent> {

    private static final Logger logger = Logger.getLogger(EventDeserializer.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // nothing to configure
    }

    @Override
    public CoffeeEvent deserialize(final String topic, final byte[] data) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            final JsonObject jsonObject = Json.createReader(input).readObject();
            final Class<? extends CoffeeEvent> eventClass = (Class<? extends CoffeeEvent>) Class.forName(jsonObject.getString("class"));
            return eventClass.getConstructor(JsonObject.class).newInstance(jsonObject.getJsonObject("data"));
        } catch (Exception e) {
            logger.severe("Could not deserialize event: " + e.getMessage());
            throw new SerializationException("Could not deserialize event", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.ProducerFencedException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class EventProducer {

    private Producer<String, CoffeeEvent> producer;
    private String topic;

    @Inject
    Properties kafkaProperties;

    @Inject
    Logger logger;

    @PostConstruct
    private void init() {
        kafkaProperties.put("transactional.id", UUID.randomUUID().toString());
        producer = new KafkaProducer<>(kafkaProperties);
        topic = kafkaProperties.getProperty("barista.topic");
        producer.initTransactions();
    }

    public void publish(CoffeeEvent... events) {
        try {
            producer.beginTransaction();
            send(events);
            producer.commitTransaction();
        } catch (ProducerFencedException e) {
            producer.close();
        } catch (KafkaException e) {
            producer.abortTransaction();
        }
    }

    private void send(CoffeeEvent... events) {
        for (final CoffeeEvent event : events) {
            final ProducerRecord<String, CoffeeEvent> record = new ProducerRecord<>(topic, event);
            logger.info("publishing = " + record);
            producer.send(record);
        }
    }

    @PreDestroy
    public void close() {
        producer.close();
    }

}



import javax.json.bind.adapter.JsonbAdapter;
import java.util.UUID;

public class UUIDAdapter implements JsonbAdapter<UUID, String> {

    @Override
    public String adaptToJson(UUID uuid) {
        return uuid.toString();
    }

    @Override
    public UUID adaptFromJson(String string) {
        return UUID.fromString(string);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

public class EventSerializer implements Serializer<CoffeeEvent> {

    private static final Logger logger = Logger.getLogger(EventSerializer.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // nothing to configure
    }

    @Override
    public byte[] serialize(final String topic, final CoffeeEvent event) {
        try {
            if (event == null)
                return null;

            final JsonbConfig config = new JsonbConfig()
                    .withAdapters(new UUIDAdapter())
                    .withSerializers(new EventJsonbSerializer());

            final Jsonb jsonb = JsonbBuilder.create(config);

            return jsonb.toJson(event, CoffeeEvent.class).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Could not serialize event: " + e.getMessage());
            throw new SerializationException("Could not serialize event", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

}


import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.util.Properties;

@ApplicationScoped
public class KafkaConfigurator {

    private Properties kafkaProperties;

    @PostConstruct
    private void initProperties() {
        try {
            kafkaProperties = new Properties();
            kafkaProperties.load(KafkaConfigurator.class.getResourceAsStream("/kafka.properties"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Produces
    @RequestScoped
    public Properties exposeKafkaProperties() throws IOException {
        final Properties properties = new Properties();
        properties.putAll(kafkaProperties);
        return properties;
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

public class EventConsumer implements Runnable {

    private final KafkaConsumer<String, CoffeeEvent> consumer;
    private final Consumer<CoffeeEvent> eventConsumer;
    private final AtomicBoolean closed = new AtomicBoolean();

    public EventConsumer(Properties kafkaProperties, Consumer<CoffeeEvent> eventConsumer, String... topics) {
        this.eventConsumer = eventConsumer;
        consumer = new KafkaConsumer<>(kafkaProperties);
        consumer.subscribe(asList(topics));
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                consume();
            }
        } catch (WakeupException e) {
            // will wakeup for closing
        } finally {
            consumer.close();
        }
    }

    private void consume() {
        ConsumerRecords<String, CoffeeEvent> records = consumer.poll(Long.MAX_VALUE);
        for (ConsumerRecord<String, CoffeeEvent> record : records) {
            eventConsumer.accept(record.value());
        }
        consumer.commitSync();
    }

    public void stop() {
        closed.set(true);
        consumer.wakeup();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

public class EventJsonbSerializer implements JsonbSerializer<CoffeeEvent> {

    @Override
    public void serialize(final CoffeeEvent event, final JsonGenerator generator, final SerializationContext ctx) {
        generator.writeStartObject();
        generator.write("class", event.getClass().getCanonicalName());
        ctx.serialize("data", event, generator);
        generator.writeEnd();
        generator.close();
    }

}


import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.Objects;

public abstract class CoffeeEvent {

    @JsonbProperty
    private final Instant instant;

    protected CoffeeEvent() {
        instant = Instant.now();
    }

    protected CoffeeEvent(final Instant instant) {
        Objects.requireNonNull(instant);
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CoffeeEvent that = (CoffeeEvent) o;

        return instant.equals(that.instant);
    }

    @Override
    public int hashCode() {
        return instant.hashCode();
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class BeansFetched extends CoffeeEvent {

    private final String beanOrigin;

    public BeansFetched(final String beanOrigin) {
        this.beanOrigin = beanOrigin;
    }

    public BeansFetched(final String beanOrigin, final Instant instant) {
        super(instant);
        this.beanOrigin = beanOrigin;
    }

    public BeansFetched(final JsonObject jsonObject) {
        this(jsonObject.getString("beanOrigin"), Instant.parse(jsonObject.getString("instant")));

    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderStarted extends CoffeeEvent {

    private final UUID orderId;

    public OrderStarted(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderStarted(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderStarted(final JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class OrderPlaced extends CoffeeEvent {

    private final OrderInfo orderInfo;

    public OrderPlaced(final OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public OrderPlaced(final OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public OrderPlaced(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.util.UUID;

public class OrderInfo {

    private final UUID orderId;
    private final CoffeeType type;
    private final String beanOrigin;

    public OrderInfo(final UUID orderId, final CoffeeType type, final String beanOrigin) {
        this.orderId = orderId;
        this.type = type;
        this.beanOrigin = beanOrigin;
    }

    public OrderInfo(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")),
                CoffeeType.fromString(jsonObject.getString("type")),
                jsonObject.getString("beanOrigin"));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public CoffeeType getType() {
        return type;
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class CoffeeBrewFinished extends CoffeeEvent {

    private final UUID orderId;

    public CoffeeBrewFinished(final UUID orderId) {
        this.orderId = orderId;
    }

    public CoffeeBrewFinished(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public CoffeeBrewFinished(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderDelivered extends CoffeeEvent {

    private final UUID orderId;

    public OrderDelivered(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderDelivered(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderDelivered(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;
import java.util.UUID;

public class CoffeeDelivered extends CoffeeEvent {

    @JsonbProperty
    private final UUID orderId;

    public CoffeeDelivered(final UUID orderId) {
        this.orderId = orderId;
    }

    public CoffeeDelivered(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public CoffeeDelivered(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final CoffeeDelivered that = (CoffeeDelivered) o;

        return orderId != null ? orderId.equals(that.orderId) : that.orderId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (orderId != null ? orderId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CoffeeDelivered{" +
                "instant=" + getInstant() +
                ", orderId=" + orderId +
                '}';
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class OrderAccepted extends CoffeeEvent {

    private final OrderInfo orderInfo;

    public OrderAccepted(final OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public OrderAccepted(final OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public OrderAccepted(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderCancelled extends CoffeeEvent {

    private final UUID orderId;
    private final String reason;

    public OrderCancelled(final UUID orderId, final String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public OrderCancelled(final UUID orderId, final String reason, Instant instant) {
        super(instant);
        this.orderId = orderId;
        this.reason = reason;
    }

    public OrderCancelled(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), jsonObject.getString("reason"), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

}


import javax.json.JsonObject;
import javax.json.bind.annotation.JsonbProperty;
import java.time.Instant;

public class CoffeeBrewStarted extends CoffeeEvent {

    @JsonbProperty
    private final OrderInfo orderInfo;

    public CoffeeBrewStarted(OrderInfo orderInfo) {
        this.orderInfo = orderInfo;
    }

    public CoffeeBrewStarted(OrderInfo orderInfo, Instant instant) {
        super(instant);
        this.orderInfo = orderInfo;
    }

    public CoffeeBrewStarted(JsonObject jsonObject) {
        this(new OrderInfo(jsonObject.getJsonObject("orderInfo")), Instant.parse(jsonObject.getString("instant")));
    }

    public OrderInfo getOrderInfo() {
        return orderInfo;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderFailedBeansNotAvailable extends CoffeeEvent {

    private final UUID orderId;

    public OrderFailedBeansNotAvailable(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderFailedBeansNotAvailable(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderFailedBeansNotAvailable(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderFinished extends CoffeeEvent {

    private final UUID orderId;

    public OrderFinished(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderFinished(final UUID orderId, Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderFinished(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;
import java.util.UUID;

public class OrderBeansReserved extends CoffeeEvent {

    private final UUID orderId;

    public OrderBeansReserved(final UUID orderId) {
        this.orderId = orderId;
    }

    public OrderBeansReserved(final UUID orderId, final Instant instant) {
        super(instant);
        this.orderId = orderId;
    }

    public OrderBeansReserved(JsonObject jsonObject) {
        this(UUID.fromString(jsonObject.getString("orderId")), Instant.parse(jsonObject.getString("instant")));
    }

    public UUID getOrderId() {
        return orderId;
    }

}


import javax.json.JsonObject;
import java.time.Instant;

public class BeansStored extends CoffeeEvent {

    private final String beanOrigin;
    private final int amount;

    public BeansStored(final String beanOrigin, final int amount) {
        this.beanOrigin = beanOrigin;
        this.amount = amount;
    }

    public BeansStored(final String beanOrigin, final int amount, final Instant instant) {
        super(instant);
        this.beanOrigin = beanOrigin;
        this.amount = amount;
    }

    public BeansStored(JsonObject jsonObject) {
        this(jsonObject.getString("beanOrigin"), jsonObject.getInt("amount"), Instant.parse(jsonObject.getString("instant")));
    }

    public String getBeanOrigin() {
        return beanOrigin;
    }

    public int getAmount() {
        return amount;
    }

}


import java.util.stream.Stream;

public enum CoffeeType {

    ESPRESSO, POUR_OVER, FRENCH_PRESS;

    public static CoffeeType fromString(final String name) {
        return Stream.of(values())
                .filter(v -> v.name().equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

}


import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class BaristaTimer {

    @Inject
    BaristaCommandService baristaService;

    @Schedule(second = "7/7", minute = "*", hour = "*", persistent = false)
    void checkCoffee() {
        baristaService.checkCoffee();
    }

    @Schedule(second = "8/8", minute = "*", hour = "*", persistent = false)
    void checkCustomerDelivery() {
        baristaService.checkCustomerDelivery();
    }

}


import com.sebastian_daschner.scalable_coffee_shop.barista.control.CoffeeBrews;
import com.sebastian_daschner.scalable_coffee_shop.events.control.EventProducer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeBrewFinished;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeBrewStarted;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeDelivered;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderInfo;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class BaristaCommandService {

    @Inject
    EventProducer eventProducer;

    @Inject
    CoffeeBrews coffeeBrews;

    @Inject
    Logger logger;

    void makeCoffee(final OrderInfo orderInfo) {
        eventProducer.publish(new CoffeeBrewStarted(orderInfo));
    }

    void checkCoffee() {
        final Collection<UUID> unfinishedBrews = coffeeBrews.getUnfinishedBrews();
        logger.info("checking " + unfinishedBrews.size() + " unfinished brews");
        unfinishedBrews.forEach(i -> {
            if (new Random().nextBoolean())
                eventProducer.publish(new CoffeeBrewFinished(i));
        });
    }

    void checkCustomerDelivery() {
        final Collection<UUID> undeliveredOrder = coffeeBrews.getUndeliveredOrders();
        logger.info("checking " + undeliveredOrder.size() + " un-served orders");
        undeliveredOrder.forEach(i -> {
            if (new Random().nextBoolean())
                eventProducer.publish(new CoffeeDelivered(i));
        });
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventConsumer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.OrderAccepted;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Properties;
import java.util.logging.Logger;

@Singleton
@Startup
public class BaristaEventHandler {

    private EventConsumer eventConsumer;

    @Resource
    ManagedExecutorService mes;

    @Inject
    Properties kafkaProperties;

    @Inject
    Event<CoffeeEvent> events;

    @Inject
    BaristaCommandService baristaService;

    @Inject
    Logger logger;

    public void handle(@Observes OrderAccepted event) {
        baristaService.makeCoffee(event.getOrderInfo());
    }

    @PostConstruct
    private void initConsumer() {
        kafkaProperties.put("group.id", "barista-handler");
        String orders = kafkaProperties.getProperty("orders.topic");

        eventConsumer = new EventConsumer(kafkaProperties, ev -> {
            logger.info("firing = " + ev);
            events.fire(ev);
        }, orders);

        mes.execute(eventConsumer);
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeBrewFinished;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeBrewStarted;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeDelivered;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Observes;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.util.Collections.unmodifiableCollection;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CoffeeBrews {

    private final Set<UUID> unfinishedBrews = new ConcurrentSkipListSet<>();
    private final Set<UUID> undeliveredOrders = new ConcurrentSkipListSet<>();

    public Collection<UUID> getUnfinishedBrews() {
        return unmodifiableCollection(unfinishedBrews);
    }

    public Collection<UUID> getUndeliveredOrders() {
        return unmodifiableCollection(undeliveredOrders);
    }

    public void apply(@Observes CoffeeBrewStarted event) {
        unfinishedBrews.add(event.getOrderInfo().getOrderId());
    }

    public void apply(@Observes CoffeeBrewFinished event) {
        final Iterator<UUID> iterator = unfinishedBrews.iterator();
        while (iterator.hasNext()) {
            final UUID orderId = iterator.next();
            if (orderId.equals(event.getOrderId())) {
                iterator.remove();
                undeliveredOrders.add(orderId);
            }
        }
    }

    public void apply(@Observes CoffeeDelivered event) {
        undeliveredOrders.removeIf(i -> i.equals(event.getOrderId()));
    }

}


import com.sebastian_daschner.scalable_coffee_shop.events.control.EventConsumer;
import com.sebastian_daschner.scalable_coffee_shop.events.entity.CoffeeEvent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

@Startup
@Singleton
public class BaristaUpdateConsumer {

    private EventConsumer eventConsumer;

    @Resource
    ManagedExecutorService mes;

    @Inject
    Properties kafkaProperties;

    @Inject
    Event<CoffeeEvent> events;

    @Inject
    Logger logger;

    @PostConstruct
    private void init() {
        kafkaProperties.put("group.id", "barista-consumer-" + UUID.randomUUID());
        String barista = kafkaProperties.getProperty("barista.topic");

        eventConsumer = new EventConsumer(kafkaProperties, ev -> {
            logger.info("firing = " + ev);
            events.fire(ev);
        }, barista);

        mes.execute(eventConsumer);
    }

    @PreDestroy
    public void close() {
        eventConsumer.stop();
    }

}

