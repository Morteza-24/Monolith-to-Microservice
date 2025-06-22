
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
//@EnableDiscoveryClient
public class RestClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestClientApplication.class, args);
    }

    /**
     * 实例化RestTemplate，通过@LoadBalanced注解开启均衡负载能力.
     *
     * @return restTemplate
     */
    @Bean
//    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PositionApi {
    @Autowired
    RestTemplate restTemplate;

    public void positionUpdate(String driverId, String longitude, String latitude) {
        String url = String.format(
                "http://localhost:8050/qbike-trip/trips/updatePosition?driverId=%s&longitude=%s&latitude=%s",
                driverId, longitude, latitude);
        restTemplate.getForObject(
                url, Object.class);
    }
}


import club.newtech.qbike.client.bean.MyIntention;
import club.newtech.qbike.client.bean.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserApi {
    @Autowired
    RestTemplate restTemplate;

    public void newUser(User user) {
        restTemplate.postForObject("http://qbike-uc/users", user, User.class);
    }

    public void newIntention(MyIntention myIntention) {
        restTemplate.postForObject("http://qbike-intention/intentions/place", myIntention, MyIntention.class);
    }
}


import lombok.Data;

@Data
public class User {

    private int id;
    private String userName;
    private String mobile;
    private String province;
    private String city;
    private String district;
    private String street;
    private String originAddress;
    private String type;

}

import lombok.Data;

@Data
public class MyIntention {
    private int userId;
    private Double startLongitude;
    private Double startLatitude;
    private Double destLongitude;
    private Double destLatitude;

}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan("club.newtech.qbike.uc.domain.root")
public class UcApplication {

	public static void main(String[] args) {
		SpringApplication.run(UcApplication.class, args);
	}
}


public enum Type {
    Customer, Driver
}


import club.newtech.qbike.uc.domain.Type;
import lombok.Data;

import javax.persistence.*;

import static javax.persistence.EnumType.STRING;

@Entity
@Table(name = "T_USER")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(length = 64)
    private String userName;
    @Column(length = 64, nullable = false)
    private String mobile;
    @Column(length = 64)
    private String province;
    @Column(length = 64)
    private String city;
    @Column(length = 64)
    private String district;
    private String street;
    private String originAddress;


    @Enumerated(value = STRING)
    @Column(length = 32, nullable = false)
    private Type type;

}


import club.newtech.qbike.uc.domain.Type;
import lombok.Data;

import javax.persistence.*;

import static javax.persistence.EnumType.STRING;

@Entity
@Table(name = "tb_poi")
@Data
public class Poi {
    @Id
    private int id;
    @Column(length = 64)
    private String linkMan;
    @Column(length = 64)
    private String shopName;
    @Column(length = 64, nullable = false)
    private String cellPhone;
    private Double longitude;
    private Double latitude;
    @Column(length = 64)
    private String province;
    @Column(length = 64)
    private String city;
    @Column(length = 64)
    private String district;
    private String street;
    private String streetNumber;
    private int shopType;
    private String userCode;
    private String originAddress;

}


import club.newtech.qbike.uc.domain.root.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends JpaRepository<User, Integer> {
}


import club.newtech.qbike.uc.domain.root.Poi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoiRepository extends JpaRepository<Poi, Integer> {
}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class SpringAsyncConfig {
    @Bean
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(10);
        return executor;
    }

}


import club.newtech.qbike.order.domain.service.OrderService;
import club.newtech.qbike.order.domain.service.Receiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
//
//    @Bean
//    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
//                                            MessageListenerAdapter intentionListener,
//                                            MessageListenerAdapter positionListener) {
//
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.addMessageListener(intentionListener, new PatternTopic("intention"));
//        container.addMessageListener(positionListener, new PatternTopic("position"));
//        return container;
//    }

//    @Bean(name = "intentionListener")
//    MessageListenerAdapter intentionListener(Receiver receiver) {
//        return new MessageListenerAdapter(receiver, "receiveMessage");
//    }
//
//    @Bean(name = "positionListener")
//    MessageListenerAdapter positionListener(Receiver receiver) {
//        return new MessageListenerAdapter(receiver, "receivePositionUpdate");
//    }

//    @Bean
//    Receiver receiver(OrderService service, ObjectMapper objectMapper) {
//        return new Receiver(service, objectMapper);
//    }
//
//    @Bean
//    public ObjectMapper objectMapper() {
//        return new ObjectMapper();
//    }

    /**
     * 实例化RestTemplate，通过@LoadBalanced注解开启均衡负载能力.
     *
     * @return restTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 曹祖鹏 OF506
 * company qianmi.com
 * Date    2018-06-22
 */
@Configuration
public class RabbitConfig {
    @Bean
    public Queue intentionQueue() {
        return new Queue("intention");
    }
}



import club.newtech.qbike.order.domain.core.vo.Events;
import club.newtech.qbike.order.domain.core.vo.StateRequest;
import club.newtech.qbike.order.domain.exception.OrderRuntimeException;
import club.newtech.qbike.order.domain.repository.OrderRepository;
import club.newtech.qbike.order.domain.service.FsmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
public class OrderController {
    @Autowired
    FsmService fsmService;
    @Autowired
    OrderRepository orderRepository;

    @PostMapping("/order/cancel")
    public List<String> cancelOrder(int driverId, String orderId) {
        StateRequest stateRequest = new StateRequest();
        stateRequest.setEvent(Events.CANCEL);
        stateRequest.setData(orderRepository.findById(orderId).get());
        stateRequest.setUId(UUID.randomUUID().toString());
        stateRequest.setOrderId(orderId);
        stateRequest.setUserId(driverId);
        try {
            fsmService.changeState(stateRequest);
            return Arrays.asList("success", "");
        } catch (OrderRuntimeException oe) {
            return Arrays.asList(oe.getErrorCode(), oe.getErrorMessage());
        }
    }

    @PostMapping("/order/aboard")
    public List<String> aboard(int driverId, String orderId) {
        StateRequest stateRequest = new StateRequest();
        stateRequest.setEvent(Events.ABOARD);
        stateRequest.setData(orderRepository.findById(orderId).get());
        stateRequest.setUId(UUID.randomUUID().toString());
        stateRequest.setOrderId(orderId);
        stateRequest.setUserId(driverId);
        try {
            fsmService.changeState(stateRequest);
            return Arrays.asList("success", "");
        } catch (OrderRuntimeException oe) {
            return Arrays.asList(oe.getErrorCode(), oe.getErrorMessage());
        }
    }
}


import club.newtech.qbike.order.domain.core.vo.CustomerVo;
import club.newtech.qbike.order.domain.core.vo.DriverVo;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class UserRibbonHystrixApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRibbonHystrixApi.class);
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 使用@HystrixCommand注解指定当该方法发生异常时调用的方法
     *
     * @param id customerId
     * @return 通过id查询到的用户
     */
    @HystrixCommand()
    public CustomerVo findCustomerById(Integer id) {
        Map ret = restTemplate.getForObject("http://QBIKE-UC/users/" + id, Map.class);
        CustomerVo customerVo = new CustomerVo();
        customerVo.setCustomerId(id);
        customerVo.setCustomerMobile(String.valueOf(ret.get("mobile")));
        customerVo.setCustomerName(String.valueOf(ret.get("userName")));
        return customerVo;
    }

    @HystrixCommand()
    public DriverVo findDriverById(Integer id) {
        Map ret = restTemplate.getForObject("http://QBIKE-UC/users/" + id, Map.class);
        DriverVo driverVo = new DriverVo();
        driverVo.setDriverId(id);
        driverVo.setDriverMobile(String.valueOf(ret.get("mobile")));
        driverVo.setDriverName(String.valueOf(ret.get("userName")));
        return driverVo;
    }

    /**
     * hystrix fallback方法
     *
     * @param id customerId
     * @return 默认的用户
     */
    public CustomerVo fallback(Integer id) {
        LOGGER.info("异常发生，进入fallback方法，接收的参数：customerId = {}", id);
        CustomerVo customer = new CustomerVo();
        customer.setCustomerId(-1);
        customer.setCustomerName("default username");
        customer.setCustomerMobile("0000");
        return customer;
    }

    public DriverVo fallbackDriver(Integer id) {
        LOGGER.info("异常发生，进入fallback方法，接收的参数：customerId = {}", id);
        DriverVo driverVo = new DriverVo();
        driverVo.setDriverId(-1);
        driverVo.setDriverName("default");
        driverVo.setDriverMobile("0000");
        return driverVo;

    }
}


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 获取spring上下文工具类
 * Created by aqlu on 15/12/2.
 */
@Component
@Lazy(false)
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        setApplicationContextWithStatic(applicationContext);
    }

    private static void  setApplicationContextWithStatic(ApplicationContext applicationContext){
        SpringContextHolder.applicationContext = applicationContext;

    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 根据Bean名称获取实例
     *
     * @return bean实例
     * @throws BeansException
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T)applicationContext.getBean(name);
    }

    /**
     * 根据类型获取实例
     *
     * @param type 类型
     * @return bean实例
     * @throws BeansException
     */
    public static <T> T getBean(Class<T> type) throws BeansException {
        return applicationContext.getBean(type);
    }
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class SequenceFactory {
    @Autowired
    RedisTemplate<String, String> redisTemplate;

    /**
     * @param key
     * @param value
     * @param expireTime
     * @Title: set
     * @Description: set cache.
     */
    public void set(String key, int value, Date expireTime) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        counter.set(value);
        counter.expireAt(expireTime);
    }

    /**
     * @param key
     * @param value
     * @param timeout
     * @param unit
     * @Title: set
     * @Description: set cache.
     */
    public void set(String key, int value, long timeout, TimeUnit unit) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        counter.set(value);
        counter.expire(timeout, unit);
    }

    /**
     * @param key
     * @return
     * @Title: generate
     * @Description: Atomically increments by one the current value.
     */
    public long generate(String key) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        return counter.incrementAndGet();
    }

    /**
     * @param key
     * @return
     * @Title: generate
     * @Description: Atomically increments by one the current value.
     */
    public long generate(String key, Date expireTime) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        counter.expireAt(expireTime);
        return counter.incrementAndGet();
    }

    /**
     * @param key
     * @param increment
     * @return
     * @Title: generate
     * @Description: Atomically adds the given value to the current value.
     */
    public long generate(String key, int increment) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        return counter.addAndGet(increment);
    }

    /**
     * @param key
     * @param increment
     * @param expireTime
     * @return
     * @Title: generate
     * @Description: Atomically adds the given value to the current value.
     */
    public long generate(String key, int increment, Date expireTime) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        counter.expireAt(expireTime);
        return counter.addAndGet(increment);
    }
}


import club.newtech.qbike.order.domain.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AsyncTaskInitializer {
    @Autowired
    OrderService orderService;

    @PostConstruct
    public void initialize() {
    }
}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态扭转 请求参数
 * Created by jinwei on 29/3/2017.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StateRequest {

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 事件操作
     */
    private Events event;

    private Object data;
    /**
     * 用户信息
     */
    private int userId;

    /**
     * 追踪调用链
     */
    private String uId;

}


import java.util.HashMap;
import java.util.Map;

public enum FlowState {
    WAITING_ABOARD("WAITING_ABOARD", "等待上车"),
    WAITING_ARRIVE("WAITING_ARRIVE", "等待到达目的地"),
    UNPAY("UNPAY", "等待支付"),
    PAYING("PAYING", "已支付待确认"),
    WAITING_COMMENT("WAITING_COMMENT", "等待评论"),
    CLOSED("CLOSED", "订单关闭"),
    CANCELED("CANCELED", "订单取消");
    private static Map<String, FlowState> flowStateMap = new HashMap<>();

    static {
        flowStateMap.put(WAITING_ABOARD.getStateId(), WAITING_ABOARD);
        flowStateMap.put(WAITING_ARRIVE.getStateId(), WAITING_ARRIVE);
        flowStateMap.put(UNPAY.getStateId(), UNPAY);
        flowStateMap.put(PAYING.getStateId(), PAYING);
        flowStateMap.put(WAITING_COMMENT.getStateId(), WAITING_COMMENT);
        flowStateMap.put(CLOSED.getStateId(), CLOSED);
        flowStateMap.put(CANCELED.getStateId(), CANCELED);
    }

    private String stateId;
    private String description;

    FlowState(String stateId, String description) {
        this.stateId = stateId;
        this.description = description;
    }

    public static FlowState forValue(String stateId) {
        return flowStateMap.get(stateId);
    }

    public String toValue() {
        return this.getStateId();
    }

    public String getStateId() {
        return stateId;
    }

    public String getDescription() {
        return description;
    }
}


public enum Events {
    ABOARD, ARRIVE, PAY, PAY_CALLBACK, COMMENT, CANCEL
}


import lombok.Data;

import javax.persistence.Embeddable;

@Embeddable
@Data
public class DriverVo {
    private int driverId;
    private String driverName;
    private String driverMobile;
}


import lombok.Data;
import lombok.ToString;



@ToString
@Data
public class IntentionVo {
    private int customerId;
    private double startLong;
    private double startLat;
    private double destLong;
    private double destLat;
    private int intentionId;
    private int driverId;


}


import lombok.Data;

import javax.persistence.Embeddable;

@Embeddable
@Data
public class CustomerVo {
    private int customerId;
    private String customerName;
    private String customerMobile;
}


import club.newtech.qbike.order.domain.core.vo.CustomerVo;
import club.newtech.qbike.order.domain.core.vo.DriverVo;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

@Data
@ToString
@Accessors(fluent = false, chain = true)
@Entity
@Table(name = "t_qbike_order")
public class Order {
    @Id
    private String oid;
    @Embedded
    private CustomerVo customer;
    @Embedded
    private DriverVo driver;
    private Double startLong;
    private Double startLat;
    private Double destLong;
    private Double destLat;
    @Temporal(TemporalType.TIMESTAMP)
    private Date opened;
    @Column(length = 32, nullable = false)
    private String orderStatus;
    private String intentionId;
}


import club.newtech.qbike.order.domain.core.root.Order;
import club.newtech.qbike.order.domain.core.vo.Events;
import club.newtech.qbike.order.domain.core.vo.FlowState;
import club.newtech.qbike.order.domain.core.vo.StateRequest;
import club.newtech.qbike.order.domain.exception.OrderRuntimeException;
import club.newtech.qbike.order.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

@Service
public class FsmService {

    public static final String UUID_KEY = "UUID_KEY";
    private static final Logger LOGGER = LoggerFactory.getLogger(FsmService.class);
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private StateMachineBuilderFactory builderFactory;

    /**
     * 修改订单状态
     * extendedState中存放有两个对象，key为StateRequest.class为最新获得的request数据，key为Order.class的是已有订单数据
     *
     * @param request 状态迁移请求
     * @return
     */
    public void changeState(StateRequest request) {
        //1、查找订单信息
        Order order = orderRepository.findById(request.getOrderId()).orElse(null);
        if (order == null) {
            throw new OrderRuntimeException("030001", new Object[]{request.getOrderId()});
        }


        //2. 根据订单创建状态机
        StateMachine<FlowState, Events> stateMachine = builderFactory.create(order);
        if (request.getData() != null) {
            stateMachine.getExtendedState().getVariables().put(StateRequest.class, request.getData());
        }
        //上下文 调用链UUID
        stateMachine.getExtendedState().getVariables().put(UUID_KEY, request.getUId());

        //3. 发送当前请求的状态
        boolean isSend = stateMachine.sendEvent(request.getEvent());

        if (!isSend) {
            LOGGER.error(String.format("无法从状态[%s]转向 => [%s]", FlowState.forValue(order.getOrderStatus()), request.getEvent()));
            throw new OrderRuntimeException("010003");
        }

        Exception error = stateMachine.getExtendedState().get(Exception.class, Exception.class);
        if (error != null) {
            if (error.getClass().equals(OrderRuntimeException.class)) {
                throw (OrderRuntimeException) error;
            } else {
                throw new OrderRuntimeException("000000", error);
            }
        }
    }
}


import club.newtech.qbike.order.domain.core.vo.IntentionVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = "intention")
public class Receiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);
    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void receiveMessage(String message) {
        LOGGER.info("Received new intention <" + message + ">");
        try {
//            String[] values = message.split("\\|");
//            if (values.length == 6) {
//                IntentionVo intentionVo =
//                        new IntentionVo(values[0],
//                                Double.parseDouble(values[1]),
//                                Double.parseDouble(values[2]),
//                                Double.parseDouble(values[3]),
//                                Double.parseDouble(values[4]),
//                                values[5],
//                                2000L);
//
//                orderService.put(intentionVo);
//            }
            IntentionVo vo = mapper.readValue(message, IntentionVo.class);
            orderService.createOrder(vo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receivePositionUpdate(String message) {
        LOGGER.info("Received position update " + message);
        try {
            String[] values = message.split("\\|");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}


import club.newtech.qbike.order.domain.core.root.Order;
import club.newtech.qbike.order.domain.core.vo.Events;
import club.newtech.qbike.order.domain.core.vo.FlowState;
import club.newtech.qbike.order.domain.core.vo.StateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class OrderStateMachineBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderStateMachineBuilder.class);
    @Autowired
    OrderService orderService;
    @Autowired
    ErrorAction errorAction;

    public StateMachine<FlowState, Events> build(FlowState initState, BeanFactory beanFactory) throws Exception {
        StateMachineBuilder.Builder<FlowState, Events> builder = StateMachineBuilder.builder();
        builder.configureConfiguration()
                .withConfiguration()
                .machineId("orderFSM")
                .beanFactory(beanFactory);
        builder.configureStates()
                .withStates()
                .initial(initState)
                .end(FlowState.CLOSED)
                .end(FlowState.CANCELED)
                .states(EnumSet.allOf(FlowState.class));
        builder.configureTransitions()
                .withExternal()
                .source(FlowState.WAITING_ABOARD).target(FlowState.WAITING_ARRIVE)
                .event(Events.ABOARD)
                .action(aboard(), errorAction)
                //司机接单后，三分钟内可以无条件取消
                .and()
                .withExternal()
                .source(FlowState.WAITING_ABOARD).target(FlowState.CANCELED)
                .event(Events.CANCEL)
                .action(cancel(), errorAction)
                // 上车后，等待结束
                .and()
                .withExternal()
                .source(FlowState.WAITING_ARRIVE).target(FlowState.UNPAY)
                .event(Events.ARRIVE)
                .and()
                .withExternal()
                .source(FlowState.UNPAY).target(FlowState.PAYING)
                .event(Events.PAY)
                .and()
                .withExternal()
                .source(FlowState.PAYING).target(FlowState.WAITING_COMMENT)
                .event(Events.PAY_CALLBACK)
                .and()
                .withExternal()
                .source(FlowState.WAITING_COMMENT).target(FlowState.CLOSED)
                .event(Events.COMMENT);


        return builder.build();
    }

    public Action<FlowState, Events> aboard() {
        return stateContext -> {
            Order order = stateContext.getExtendedState().get(StateRequest.class, Order.class);
            String uuId = stateContext.getExtendedState().get(OrderService.UUID_KEY, String.class);
            order.setOrderStatus(FlowState.WAITING_ARRIVE.toValue());
            orderService.aboard(order);
        };
    }

    public Action<FlowState, Events> cancel() {
        return stateContext -> {
            Order order = stateContext.getExtendedState().get(StateRequest.class, Order.class);
            String uuId = stateContext.getExtendedState().get(OrderService.UUID_KEY, String.class);
            order.setOrderStatus(FlowState.CANCELED.toValue());
            orderService.cancel(order);
        };
    }

}


import javax.transaction.Transactional;

@Transactional
public class InnerService {
}


import club.newtech.qbike.order.domain.core.root.Order;
import club.newtech.qbike.order.domain.core.vo.CustomerVo;
import club.newtech.qbike.order.domain.core.vo.DriverVo;
import club.newtech.qbike.order.domain.core.vo.FlowState;
import club.newtech.qbike.order.domain.core.vo.IntentionVo;
import club.newtech.qbike.order.domain.exception.OrderRuntimeException;
import club.newtech.qbike.order.domain.repository.OrderRepository;
import club.newtech.qbike.order.infrastructure.UserRibbonHystrixApi;
import club.newtech.qbike.order.util.SequenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderService {
    public static final String UUID_KEY = "UUID_KEY";
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    @Autowired
    UserRibbonHystrixApi userService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private SequenceFactory sequenceFactory;


    private String generateOrderId() {
        return "T" + String.format("%010d", sequenceFactory.generate("order"));
    }


    @Transactional
    public Order createOrder(IntentionVo intention) {
        //在调用远程user服务获取用户信息的时候，必须有熔断，否则在事务中很危险
        Order order = new Order();
        order.setOid(generateOrderId());
        CustomerVo customerVo = userService.findCustomerById(intention.getCustomerId());
        DriverVo driverVo = userService.findDriverById(intention.getDriverId());
        order.setCustomer(customerVo);
        order.setDriver(driverVo);
        order.setOrderStatus(FlowState.WAITING_ABOARD.toValue());
        order.setOpened(new Date());
        order.setStartLong(intention.getStartLong());
        order.setStartLat(intention.getStartLat());
        order.setDestLong(intention.getDestLong());
        order.setDestLat(intention.getDestLat());
        order.setIntentionId(String.valueOf(intention.getIntentionId()));
        orderRepository.save(order);
        return order;
    }

    @Transactional
    public void aboard(Order order) {
        order.setOrderStatus(FlowState.WAITING_ARRIVE.toValue());
        order.setOpened(new Date());
        orderRepository.save(order);
    }

    @Transactional
    public void cancel(Order order) {
        Date currentTime = new Date();
        if ((currentTime.getTime() - order.getOpened().getTime()) <= 3 * 60 * 1000L) {
            order.setOrderStatus(FlowState.CANCELED.toValue());
            orderRepository.save(order);
        } else {
            throw new OrderRuntimeException("040001");
        }
    }


}


import club.newtech.qbike.order.domain.core.vo.Events;
import club.newtech.qbike.order.domain.core.vo.FlowState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Service;

/**
 * 状态机异常Action
 *
 * @author wumeng[OF2627]
 * company qianmi.com
 * Date 2017-04-26
 */
@Service
public class ErrorAction implements Action<FlowState, Events> {

    @Override
    public void execute(StateContext context) {
        context.getExtendedState().getVariables().put(Exception.class, context.getException());
    }
}


import club.newtech.qbike.order.domain.core.root.Order;
import club.newtech.qbike.order.domain.core.vo.Events;
import club.newtech.qbike.order.domain.core.vo.FlowState;
import club.newtech.qbike.order.domain.exception.OrderRuntimeException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

@Component
public class StateMachineBuilderFactory {
    @Autowired
    private OrderStateMachineBuilder orderStateMachineBuilder;
    @Autowired
    private BeanFactory beanFactory;

    public StateMachine<FlowState, Events> create(Order order) {
        FlowState flowState = FlowState.forValue(order.getOrderStatus());

        try {
            StateMachine<FlowState, Events> sm = orderStateMachineBuilder.build(flowState, beanFactory);
//            sm.getStateMachineAccessor().withRegion().addStateMachineInterceptor(tradeCommonOptInterceptor);
            sm.start();
            sm.getExtendedState().getVariables().put(Order.class, order);
            return sm;
        } catch (Exception e) {
            throw new OrderRuntimeException(String.format("创建状态[%s]失败 => [%s]", flowState, e.getCause()));
        }

    }
}


import club.newtech.qbike.order.domain.core.root.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, String> {
}


import club.newtech.qbike.order.util.SpringContextHolder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

/**
 * 运行时异常
 * Created by jinwei on 12/1/2017.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderRuntimeException extends RuntimeException {

    private String errorCode = "";

    private String errorMessage = "";

    private Object[] params;

    private MessageSource messageSource = SpringContextHolder.getBean(MessageSource.class);

    public OrderRuntimeException() {
        super();
        this.errorCode = "000000";
    }

    public OrderRuntimeException(String errorCode) {
        super();
        this.errorCode = errorCode;
        this.params = null;
    }

    public OrderRuntimeException(String errorCode, Object[] params) {
        super();
        this.errorCode = errorCode;
        this.params = params;
    }

    public OrderRuntimeException(String errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.params = null;
    }

    public OrderRuntimeException(String errorCode, Object[] params, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.params = params;
    }

    public OrderRuntimeException(Throwable cause) {
        super(cause);

    }


    /**
     * 生成异常信息
     *
     * @return
     */
    public String getErrorMessage() {
        String msg = "";
        Throwable cause = this.getCause();

        String errorCode = this.getErrorCode();

        if (StringUtils.isNotEmpty(errorCode) && StringUtils.isEmpty(msg)) {
            //2、如果有异常码，以异常码对应的提示信息为准
            msg = this.getMessage(errorCode, this.getParams());
        }
        if (StringUtils.isEmpty(msg) && cause != null) {
            msg = cause.getMessage();
        }
        if (StringUtils.isEmpty(msg)) {
            //3、异常码为空 & msg为空，提示系统异常
            msg = this.getMessage("000000", this.getParams());
        }
        return msg;
    }

    /**
     * 获取错误码描述
     *
     * @param code
     * @return
     */
    private String getMessage(String code, Object[] params) {
        try {
            return messageSource.getMessage(code, params, Locale.CHINA);
        } catch (NoSuchMessageException e) {
            return code;
        }
    }
}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class SpringAsyncConfig {
    @Bean
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(10);
        return executor;
    }

}


import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 曹祖鹏 OF506
 * company qianmi.com
 * Date    2018-06-22
 */
@Configuration
public class RabbitConfig {
    @Bean
    public Queue intentionQueue() {
        return new Queue("intention");
    }


}



import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
public class IntentionApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntentionApplication.class, args);
    }

    /**
     * 实例化RestTemplate，通过@LoadBalanced注解开启均衡负载能力.
     *
     * @return restTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}


import club.newtech.qbike.intention.controller.bean.MyIntention;
import club.newtech.qbike.intention.domain.core.vo.Customer;
import club.newtech.qbike.intention.domain.core.vo.DriverStatusVo;
import club.newtech.qbike.intention.domain.service.IntentionService;
import club.newtech.qbike.intention.infrastructure.PositionApi;
import club.newtech.qbike.intention.infrastructure.UserRibbonHystrixApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
public class RibbonHystrixController {
    @Autowired
    private UserRibbonHystrixApi userRibbonHystrixApi;
    @Autowired
    private IntentionService intentionService;
    @Autowired
    private PositionApi positionApi;

    @GetMapping("/ribbon/{id}")
    public Customer findById(@PathVariable Integer id) {
        return this.userRibbonHystrixApi.findById(id);
    }

    @GetMapping("/ribbon/match")
    public Collection<DriverStatusVo> match(double longitude, double latitude) {
        return this.positionApi.match(longitude, latitude);
    }

    @PostMapping("/intentions/place")
    public void place(@RequestBody
                              MyIntention myIntention) {
        intentionService.placeIntention(myIntention.getUserId(),
                myIntention.getStartLongitude(), myIntention.getStartLatitude(),
                myIntention.getDestLongitude(), myIntention.getDestLatitude());
    }

    @PostMapping("/intention/confirm")
    public boolean confirm(int driverId, int intentionId) {
        return intentionService.confirmIntention(driverId, intentionId);
    }
}


import lombok.Data;

@Data
public class MyIntention {
    private int userId;
    private Double startLongitude;
    private Double startLatitude;
    private Double destLongitude;
    private Double destLatitude;

}


import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;


@ToString
@Data
@Accessors(fluent = false, chain = true)
public class IntentionVo {
    private int customerId;
    private double startLong;
    private double startLat;
    private double destLong;
    private double destLat;
    private int intentionId;
    private int driverId;
}


import club.newtech.qbike.intention.domain.core.vo.DriverStatusVo;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class PositionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionApi.class);
    @Autowired
    private RestTemplate restTemplate;

    @HystrixCommand(fallbackMethod = "defaultMatch")
    public Collection<DriverStatusVo> match(double longitude, double latitude) {
        ResponseEntity<Collection<DriverStatusVo>> matchReponse =
                restTemplate.exchange(
                        String.format("http://qbike-trip/trips/match?longitude=%s&latitude=%s", longitude, latitude),
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<Collection<DriverStatusVo>>() {
                        }
                );
        return matchReponse.getBody();
    }

    public Collection<DriverStatusVo> defaultMatch(double longitude, double latitude) {
        return new ArrayList<>();
    }
}


import club.newtech.qbike.intention.domain.core.vo.Customer;
import club.newtech.qbike.intention.domain.core.vo.DriverVo;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class UserRibbonHystrixApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRibbonHystrixApi.class);
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 使用@HystrixCommand注解指定当该方法发生异常时调用的方法
     *
     * @param id customerId
     * @return 通过id查询到的用户
     */
    @HystrixCommand
    public Customer findById(Integer id) {
//        return this.restTemplate.getForObject("http://QBIKE-UC/users/" + customerId, Customer.class);
        Map ret = restTemplate.getForObject("http://QBIKE-UC/users/" + id, Map.class);
        Customer customerVo = new Customer();
        customerVo.setCustomerId(id);
        customerVo.setCustomerMobile(String.valueOf(ret.get("mobile")));
        customerVo.setCustomerName(String.valueOf(ret.get("userName")));
        customerVo.setUserType(String.valueOf(ret.get("type")));
        return customerVo;
    }

    @HystrixCommand
    public DriverVo findDriverById(Integer id) {
        Map ret = restTemplate.getForObject("http://QBIKE-UC/users/" + id, Map.class);
        DriverVo driverVo = new DriverVo();
        driverVo.setId(id);
        driverVo.setMobile(String.valueOf(ret.get("mobile")));
        driverVo.setUserName(String.valueOf(ret.get("userName")));
        driverVo.setType(String.valueOf(ret.get("type")));
        return driverVo;
    }

    /**
     * hystrix fallback方法
     *
     * @param id customerId
     * @return 默认的用户
     */
    public Customer fallback(Integer id) {
        UserRibbonHystrixApi.LOGGER.info("异常发生，进入fallback方法，接收的参数：customerId = {}", id);
        Customer customer = new Customer();
        customer.setCustomerId(-1);
        customer.setCustomerName("default username");
        customer.setCustomerMobile("0000");
        return customer;
    }
}


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.Embeddable;
import java.util.Date;

/**
 * 和Position服务中的DriverStatus内容一样
 * 这里的值对象的目的是避免共享带来的耦合
 * 让intention成为自治的服务
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DriverStatusVo {
    private int dId;
    private DriverVo driver;
    private Double currentLongitude;
    private Double currentLatitude;
    private Date updateTime;
}


import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Getter
@ToString
/**
 * 用延时队列来实现定时的轮询任务，如果找不到匹配intention的司机，将重新压回队列
 * 分布式下可以使用redis来实现延时任务队列
 */
public class IntentionTask implements Delayed {
    /**
     * Base of nanosecond timings, to avoid wrapping
     */
    private final long NANO_ORIGIN = System.nanoTime();
    private final long executionTime;
    private int intenionId;
    private int repeatTimes;

    public IntentionTask(int intenionId, long time, TimeUnit unit, int repeatTimes) {
        this.intenionId = intenionId;
        this.repeatTimes = repeatTimes;
        this.executionTime = TimeUnit.NANOSECONDS.convert(time, unit);
    }


    final long now() {
        return System.nanoTime() - NANO_ORIGIN;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long d = unit.convert(executionTime - now(), TimeUnit.NANOSECONDS);
        return d;
    }

    @Override
    public int compareTo(Delayed other) {
        if (other == this) // compare zero ONLY if same object
            return 0;
        if (other instanceof IntentionTask) {
            IntentionTask x = (IntentionTask) other;
            long diff = executionTime - x.executionTime;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
        }
        long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }
}



public enum Status {
    Inited, UnConfirmed, Confirmed, Failed
}


import club.newtech.qbike.intention.domain.core.root.Intention;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "t_intention_candidate")
public class Candidate {
    /**
     * 按照DDD的理论，值对象是没有自己的主键的，应该用intentionId和driverId组成复合主键
     * 但是考虑JPA的实现难度和数据库管理的难度，所以这里做了一个反模式
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int cid;
    @ManyToOne
    @JoinColumn(name = "intention_id")
    private Intention intention;
    private int driverId;
    private String driverName;
    private String driverMobile;
    private Double longitude;
    private Double latitude;
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

}


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Embeddable
public class DriverVo {
    @Column(nullable = true)
    private int id;
    @Column(nullable = true)
    private String userName;
    @Column(nullable = true)
    private String mobile;
    @Column(nullable = true)
    private String type;
}


import lombok.Data;

import javax.persistence.Embeddable;

@Embeddable
@Data
public class Customer {
    private int customerId;
    private String customerName;
    private String customerMobile;
    private String userType;
}


import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * A value object representing a named lock, with a globally unique value and an expiry.
 * @author Joe
 *
 */
@Data
@ToString
public class Lock implements Comparable<Lock> {
    /**
     * The customerName of the lock.
     */
    private final String name;
    /**
     * The value of the lock (globally unique, or at least different for locks with the
     * same customerName and different expiry).
     */
    private final String value;
    /**
     * The expiry of the lock expressed as a point in time.
     */
    private final Date expires;

    public boolean isExpired() {
        return expires.before(new Date());
    }

    @Override
    public int compareTo(Lock other) {
        return expires.compareTo(other.expires);
    }
}


import club.newtech.qbike.intention.domain.core.vo.Candidate;
import club.newtech.qbike.intention.domain.core.vo.Customer;
import club.newtech.qbike.intention.domain.core.vo.DriverVo;
import club.newtech.qbike.intention.domain.core.vo.Status;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static javax.persistence.EnumType.STRING;

@Data
@ToString
@Accessors(fluent = false, chain = true)
@Entity
@Table(name = "t_intention")
public class Intention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int mid;
    private Double startLongitude;
    private Double startLatitude;
    private Double destLongitude;
    private Double destLatitude;
    @Embedded
    private Customer customer;
    @Enumerated(value = STRING)
    @Column(length = 32, nullable = false)
    private Status status;
    @Embedded
    private DriverVo selectedDriver;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated = new Date();

    @OneToMany(mappedBy = "intention", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Candidate> candidates = new ArrayList<>();

    public boolean canMatchDriver() {
        if (status.equals(Status.Inited)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean canConfirm() {
        if (status.equals(Status.UnConfirmed)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean waitingConfirm() {
        if (canMatchDriver()) {
            this.status = Status.UnConfirmed;
            this.updated = new Date();
            return true;
        } else {
            return false;
        }
    }

    public boolean fail() {
        if (this.status == Status.Inited) {
            this.status = Status.Failed;
            this.updated = new Date();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 抢单，先应答的司机抢单成功
     * 该方法线程不安全，请使用锁保证没有并发
     *
     * @param driverVo
     * @return 0 成功, -1 状态不对, -2 不是候选司机，-3 已被抢走
     */
    public int confirmIntention(DriverVo driverVo) {
        //判断状态
        if (!canConfirm()) {
            //状态不对
            return -1;
        }
        //判断是否是候选司机，不能随便什么司机都来抢单
        if (candidates.stream().map(Candidate::getDriverId).noneMatch(id -> id == driverVo.getId())) {
            return -2;
        }
        //将候选司机加入到列表中
        if (this.selectedDriver == null) {
            this.selectedDriver = driverVo;
            this.status = Status.Confirmed;
            this.updated = new Date();
            return 0;
        } else {
            return -3;
        }

    }
}



import club.newtech.qbike.intention.domain.core.vo.Lock;
import club.newtech.qbike.intention.domain.exception.LockExistsException;
import club.newtech.qbike.intention.domain.exception.LockNotHeldException;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;

@Service
public class RedisLockService implements LockService {
    private static final String DEFAULT_LOCK_PREFIX = "qbike.lock.";
    @Autowired
    StringRedisTemplate redisOperations;
    private String prefix = DEFAULT_LOCK_PREFIX;
    @Setter
    private long expiry = 30000; // 30 seconds

    /**
     * The prefix for all lock keys.
     *
     * @param prefix the prefix to set for all lock keys
     */
    public void setPrefix(String prefix) {
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        this.prefix = prefix;
    }

    @Override
    public Iterable<Lock> findAll() {
        Set<String> keys = redisOperations.keys(prefix + "*");
        Set<Lock> locks = new LinkedHashSet<Lock>();
        for (String key : keys) {
            Date expires = new Date(System.currentTimeMillis() + redisOperations.getExpire(key, TimeUnit.MILLISECONDS));
            locks.add(new Lock(nameForKey(key), redisOperations.opsForValue().get(key), expires));
        }
        return locks;
    }

    @Override
    public Lock create(String name) {
        String stored = getValue(name);
        if (stored != null) {
            throw new LockExistsException();
        }
        String value = UUID.randomUUID().toString();
        String key = keyForName(name);
        if (!redisOperations.opsForValue().setIfAbsent(key, value)) {
            throw new LockExistsException();
        }
        redisOperations.expire(key, expiry, TimeUnit.MILLISECONDS);
        Date expires = new Date(System.currentTimeMillis() + expiry);
        return new Lock(name, value, expires);
    }

    @Override
    public Set<Lock> createMultiLock(Set<String> names) {
        Set<Lock> locks = redisOperations.execute(new SessionCallback<Set<Lock>>() {
            @Override
            public Set<Lock> execute(RedisOperations operations) throws DataAccessException {
                Set<Lock> locks = new HashSet<>();
                Map<String, String> mset =
                        names.stream()
                                .collect(toMap(name -> keyForName(name), key -> UUID.randomUUID().toString()));
                //一次性获取所有的锁，如果失败直接退出，说明已经有被占用了
                boolean b = operations.opsForValue()
                        .multiSetIfAbsent(mset);
                //占领锁成功了，再给锁添加过期时间 TODO 这里有可能失败，如果用Watch也许会更有效果一些
                if (b) {
                    operations.multi();
                    for (String name : names) {
                        String key = keyForName(name);
                        operations.expire(key, expiry, TimeUnit.MILLISECONDS);
                        Date expires = new Date(System.currentTimeMillis() + expiry);
                        Lock lock = new Lock(name, mset.get(key), expires);
                        locks.add(lock);
                    }
                    operations.exec();
                    return locks;
                } else {
                    throw new LockExistsException();
                }
            }

        });
        return locks;
    }

    @Override
    public boolean release(String name, String value) {
        String stored = getValue(name);
        if (stored != null && value.equals(stored)) {
            String key = keyForName(name);
            redisOperations.delete(key);
            return true;
        }
        if (stored != null) {
            throw new LockNotHeldException();
        }
        return false;
    }

    @Override
    public Lock refresh(String name, String value) {
        String key = keyForName(name);
        String stored = getValue(name);
        if (stored != null && value.equals(stored)) {
            Date expires = new Date(System.currentTimeMillis() + expiry);
            redisOperations.expire(key, expiry, TimeUnit.MILLISECONDS);
            return new Lock(name, value, expires);
        }
        throw new LockNotHeldException();
    }

    private String getValue(String name) {
        String key = keyForName(name);
        String stored = redisOperations.opsForValue().get(key);
        return stored;
    }

    private String nameForKey(String key) {
        if (!key.startsWith(prefix)) {
            throw new IllegalStateException("Key (" + key + ") does not start with prefix (" + prefix + ")");
        }
        return key.substring(prefix.length());
    }

    private String keyForName(String name) {
        return prefix + name;
    }
}



import club.newtech.qbike.intention.domain.core.vo.Lock;
import club.newtech.qbike.intention.domain.exception.LockExistsException;
import club.newtech.qbike.intention.domain.exception.LockNotHeldException;

import java.util.Set;

public interface LockService {
    /**
     * Iterate the existing locks.
     *
     * @return an iterable of all locks
     */
    Iterable<Lock> findAll();

    /**
     * Acquire a lock by customerName. Only one process (globally) should be able to obtain and
     * hold the lock with this customerName at any given time. Locks expire and can also be
     * released by the owner, so after either of those events the lock can be acquired by
     * the same or a different process.
     *
     * @param name the customerName identifying the lock
     * @return a Lock containing a value that can be used to release or refresh the lock
     * @throws LockExistsException
     */
    Lock create(String name) throws LockExistsException;

    Set<Lock> createMultiLock(Set<String> names) throws LockExistsException;

    /**
     * Release a lock before it expires. Only the holder of a lock can release it, and the
     * holder must have the correct unique value to prove that he holds it.
     *
     * @param name  the customerName of the lock
     * @param value the value of the lock (which has to match the value when it was
     *              acquired)
     * @return true if successful
     * @throws LockNotHeldException
     */
    boolean release(String name, String value) throws LockNotHeldException;


    /**
     * The holder of a lock can refresh it, extending its expiry. If the caller does not
     * hold the lock there will be an exception, but the implementation may not be able to
     * tell if it was because he formerly held the lock and it expired, or if it simply
     * was never held.
     *
     * @param name  the customerName of the lock
     * @param value the value of the lock (which has to match the value when it was
     *              acquired)
     * @return a new lock with a new value and a new expiry
     * @throws LockNotHeldException if the value does not match the current value or if
     *                              the lock doesn't exist (e.g. if it expired)
     */
    Lock refresh(String name, String value) throws LockNotHeldException;
}


import club.newtech.qbike.intention.controller.bean.IntentionVo;
import club.newtech.qbike.intention.domain.core.root.Intention;
import club.newtech.qbike.intention.domain.core.vo.*;
import club.newtech.qbike.intention.domain.exception.LockException;
import club.newtech.qbike.intention.domain.repository.CandidateRepository;
import club.newtech.qbike.intention.domain.repository.IntentionRepository;
import club.newtech.qbike.intention.infrastructure.PositionApi;
import club.newtech.qbike.intention.infrastructure.UserRibbonHystrixApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@Service
public class IntentionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntentionService.class);
    @Autowired
    IntentionRepository intentionRepository;
    @Autowired
    UserRibbonHystrixApi userApi;
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    @Autowired
    PositionApi positionApi;
    @Autowired
    CandidateRepository candidateRepository;
    @Autowired
    LockService lockService;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    RabbitTemplate rabbitTemplate;


    private DelayQueue<IntentionTask> intentions = new DelayQueue<>();

    private static Candidate fromDriverStatus(DriverStatusVo driverStatusVo, Intention intention) {
        Candidate candidate = new Candidate();
        candidate.setDriverId(driverStatusVo.getDId());
        candidate.setCreated(new Date());
        candidate.setDriverMobile(driverStatusVo.getDriver().getMobile());
        candidate.setDriverName(driverStatusVo.getDriver().getUserName());
        candidate.setLongitude(driverStatusVo.getCurrentLongitude());
        candidate.setLatitude(driverStatusVo.getCurrentLatitude());
        candidate.setIntention(intention);
        return candidate;
    }

    @Transactional
    public void placeIntention(int userId, Double startLongitude, Double startLatitude,
                               Double destLongitude, Double destLatitude) {
        Customer customer = userApi.findById(userId);
        Intention intention = new Intention()
                .setStartLongitude(startLongitude)
                .setStartLatitude(startLatitude)
                .setDestLongitude(destLongitude)
                .setDestLatitude(destLatitude)
                .setCustomer(customer)
                .setStatus(Status.Inited);
        intentionRepository.save(intention);
        IntentionTask task = new IntentionTask(intention.getMid(), 2L, TimeUnit.SECONDS, 0);

        intentions.put(task);
    }

    @Transactional
    public void sendNotification(Collection<DriverStatusVo> result, Intention intention) {
        result.stream()
                .map(vo -> fromDriverStatus(vo, intention))
                .forEach(candidate -> candidateRepository.save(candidate));
        intention.waitingConfirm();
        intentionRepository.save(intention);
    }

    @Transactional
    public boolean confirmIntention(int driverId, int intentionId) {
        String lockName = "intention" + intentionId;
        Lock lock = null;
        try {
            lock = lockService.create(lockName);
            Intention intention = intentionRepository.findById(intentionId).orElse(null);
            DriverVo driverVo = userApi.findDriverById(driverId);
            int ret = intention.confirmIntention(driverVo);
            LOGGER.info("{}司机抢单{}结果为{}", driverId, intentionId, ret);
            if (ret == 0) {
                intentionRepository.save(intention);
                IntentionVo intentionVo = new IntentionVo().setIntentionId(intention.getMid())
                        .setCustomerId(intention.getCustomer().getCustomerId())
                        .setDestLat(intention.getDestLatitude())
                        .setDestLong(intention.getDestLongitude())
                        .setStartLong(intention.getStartLongitude())
                        .setStartLat(intention.getStartLatitude())
                        .setDriverId(intention.getSelectedDriver().getId());
                try {
                    rabbitTemplate.convertAndSend("intention", objectMapper.writeValueAsString(intentionVo));
                } catch (JsonProcessingException e) {
                    LOGGER.error("convert message fail" + intentionVo, e);
                }
                return true;
            } else {
                return false;
            }
        } catch (LockException e) {
            LOGGER.error("try lock error ", e);
            return false;
        } finally {
            if (lock != null) {
                lockService.release(lockName, lock.getValue());
            }
        }
    }

    @Transactional
    public void matchFail(Intention intention) {
        intention.fail();
        intentionRepository.save(intention);
    }

    @Async
    public void handleTask() {
        LOGGER.info("start handling intention task loop");
        for (; ; ) {
            try {
                IntentionTask task = intentions.take();
                if (task != null) {
                    LOGGER.info("got a task {}", task.getIntenionId());
                    Intention intention = intentionRepository.findById(task.getIntenionId()).orElse(null);
                    if (intention.canMatchDriver()) {
                        //调用position服务匹配司机
                        Collection<DriverStatusVo> result = positionApi.match(intention.getStartLongitude(), intention.getStartLatitude());
                        if (result.size() > 0) {
                            List<String> names = result.stream().map(s -> s.getDriver().getUserName()).collect(toList());
                            LOGGER.info("匹配司机{}，将向他们发送抢单请求", names);
                            sendNotification(result, intention);
                        } else {
                            LOGGER.info("没有匹配到司机，放入队列继续等待");
                            retryMatch(task, intention);
                        }
                    } else {
                        // 忽略
                    }

                }
            } catch (Exception e) {
                LOGGER.error("error happened", e);
            }
        }
    }

    private boolean retryMatch(IntentionTask task, Intention intention) {
        int times = task.getRepeatTimes() + 1;
        LOGGER.info("task 已经被循环{} 次", times);
        if (times > 5) {
            //超过n次匹配无法匹配，就失败
            matchFail(intention);
            return true;
        }
        IntentionTask newTask = new IntentionTask(intention.getMid(), 2 * times, TimeUnit.SECONDS, times);
        this.intentions.put(newTask);
        return false;
    }
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AsyncTaskInitializer {
    @Autowired
    IntentionService intentionService;

    @PostConstruct
    public void initialize() {
        intentionService.handleTask();
    }
}


import club.newtech.qbike.intention.domain.core.root.Intention;
import org.springframework.data.repository.CrudRepository;

public interface IntentionRepository extends CrudRepository<Intention, Integer> {
}


import club.newtech.qbike.intention.domain.core.vo.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Integer> {
}


@SuppressWarnings("serial")
public class LockException extends RuntimeException {

}

@SuppressWarnings("serial")
public class LockExistsException extends LockException {

}

@SuppressWarnings("serial")
public class NoSuchLockException extends LockException {

}


@SuppressWarnings("serial")
public class LockNotHeldException extends LockException {

}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCircuitBreaker
public class PositionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PositionApplication.class, args);
    }

    /**
     * 实例化RestTemplate，通过@LoadBalanced注解开启均衡负载能力.
     *
     * @return restTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


import club.newtech.qbike.trip.domain.core.root.DriverStatus;
import club.newtech.qbike.trip.domain.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class PositionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionController.class);
    @Autowired
    PositionService positionService;

    @GetMapping("/trips/updatePosition")
    public void positionUpdate(Integer driverId, Double longitude, Double latitude) {
        LOGGER.info(String.format("update position %s %s %s", driverId, longitude, latitude));
        positionService.updatePosition(driverId, longitude, latitude);
    }

    @GetMapping("/trips/match")
    public Collection<DriverStatus> match(Double longitude, Double latitude) {
        return positionService.matchDriver(longitude, latitude);
    }
}


import club.newtech.qbike.trip.domain.core.vo.Driver;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserRibbonHystrixApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRibbonHystrixApi.class);
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 使用@HystrixCommand注解指定当该方法发生异常时调用的方法
     *
     * @param id customerId
     * @return 通过id查询到的用户
     */
    @HystrixCommand
    public Driver findById(Integer id) {
        Driver driver = this.restTemplate.getForObject("http://QBIKE-UC/users/" + id, Driver.class);
        driver.setId(id);
        return driver;
    }

    /**
     * hystrix fallback方法
     *
     * @param id customerId
     * @return 默认的用户
     */
    public Driver fallback(Integer id) {
        UserRibbonHystrixApi.LOGGER.info("异常发生，进入fallback方法，接收的参数：customerId = {}", id);
        Driver driver = new Driver();
        driver.setId(-1);
        driver.setUserName("default driver");
        driver.setMobile("0000");
        return driver;
    }
}


public enum Status {
    BUSY, OFFLINE, ONLINE
}


import lombok.Data;

import javax.persistence.Embeddable;

@Embeddable
@Data
public class Driver {
    private int id;
    private String userName;
    private String mobile;
    private String type;
}


import club.newtech.qbike.trip.domain.core.Status;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.EnumType.STRING;

@Data
@ToString
@Accessors(fluent = false, chain = true)
@Entity
@Table(name = "t_position")
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tid;
    private Double positionLongitude;
    private Double positionLatitude;
    @Enumerated(value = STRING)
    @Column(length = 32, nullable = false)
    private Status status;
    private String driverId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadTime;

}


import club.newtech.qbike.trip.domain.core.Status;
import club.newtech.qbike.trip.domain.core.vo.Driver;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

import static javax.persistence.EnumType.STRING;

@Data
@Entity
@Table(name = "t_driver_status")
public class DriverStatus {
    @Id
    private int dId;
    @Embedded
    private Driver driver;
    private Double currentLongitude;
    private Double currentLatitude;
    @Enumerated(value = STRING)
    @Column(length = 32, nullable = false)
    private Status status;
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
}


import club.newtech.qbike.trip.domain.core.Status;
import club.newtech.qbike.trip.domain.core.root.DriverStatus;
import club.newtech.qbike.trip.domain.core.vo.Driver;
import club.newtech.qbike.trip.domain.core.vo.Position;
import club.newtech.qbike.trip.domain.repository.DriverStatusRepo;
import club.newtech.qbike.trip.domain.repository.PositionRepository;
import club.newtech.qbike.trip.infrastructure.UserRibbonHystrixApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@Transactional
public class PositionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionService.class);
    @Autowired
    DriverStatusRepo driverStatusRepo;
    @Autowired
    UserRibbonHystrixApi userService;
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    @Autowired
    PositionRepository positionRepository;

    public void updatePosition(Integer driverId, Double longitude, Double latitude) {
        //先记录轨迹
        Date current = new Date();
        Position position = new Position();
        position.setDriverId(String.valueOf(driverId));
        position.setPositionLongitude(longitude);
        position.setPositionLatitude(latitude);
        //TODO 目前没有上传上下线状态
        position.setStatus(Status.ONLINE);
        position.setUploadTime(current);
        positionRepository.save(position);
        //更新状态表
        DriverStatus driverStatus = driverStatusRepo.findByDriver_Id(driverId);
        if (driverStatus != null) {
            driverStatus.setCurrentLongitude(longitude);
            driverStatus.setCurrentLatitude(latitude);
            driverStatus.setUpdateTime(current);
            driverStatusRepo.save(driverStatus);
        } else {
            Driver driver = userService.findById(driverId);
            driverStatus = new DriverStatus();
            driverStatus.setDriver(driver);
            driverStatus.setCurrentLongitude(longitude);
            driverStatus.setCurrentLatitude(latitude);
            driverStatus.setUpdateTime(current);
            driverStatus.setStatus(Status.ONLINE);
            driverStatus.setDId(driverId);
            driverStatusRepo.save(driverStatus);
        }
        //更新Redis中的坐标数据，GeoHash
        redisTemplate.opsForGeo().geoAdd("Drivers", new Point(longitude, latitude), String.valueOf(driverId));
        LOGGER.info("position update " + driverStatus);

    }

    public Collection<DriverStatus> matchDriver(double longitude, double latitude) {
        Circle circle = new Circle(new Point(longitude, latitude), //
                new Distance(500, RedisGeoCommands.DistanceUnit.METERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> result =
                redisTemplate.opsForGeo().geoRadius("Drivers", circle);
        if (result.getContent().size() == 0) {
            LOGGER.info("没找到匹配司机");
            return new ArrayList<>();

        } else {
            List<String> drivers = result.getContent()
                    .stream()
                    .map(x -> x.getContent().getName())
                    .collect(toList());
            LOGGER.info("获取附近司机为{}", drivers);
            return drivers.stream().map(Integer::parseInt)
                    .map(id -> driverStatusRepo.findByDriver_Id(id)).collect(toList());
        }
    }
}


import club.newtech.qbike.trip.domain.core.root.DriverStatus;
import org.springframework.data.repository.CrudRepository;

public interface DriverStatusRepo extends CrudRepository<DriverStatus, Integer> {
    DriverStatus findByDriver_Id(Integer driverId);
}


import club.newtech.qbike.trip.domain.core.vo.Position;
import org.springframework.data.repository.CrudRepository;

public interface PositionRepository extends CrudRepository<Position, Integer> {
}

