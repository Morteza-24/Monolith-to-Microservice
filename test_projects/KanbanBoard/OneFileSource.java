
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import org.apache.commons.lang.builder.ToStringBuilder;

public class CreateBoardCommand implements BoardCommand {
    private BoardInfo boardInfo;

    public CreateBoardCommand(BoardInfo boardInfo) {
        this.boardInfo = boardInfo;
    }

    public BoardInfo getBoardInfo() {
        return boardInfo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import io.eventuate.EntityWithIdAndVersion;
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(value = "/api")
public class BoardCommandController {

    @Autowired
    private BoardService boardService;

    private static Logger log = LoggerFactory.getLogger(BoardCommandController.class);

    @RequestMapping(value = "/boards", method = POST)
    public CompletableFuture<BoardResponse> saveBoard(@RequestBody BoardInfo board) {
        return boardService.save(board).thenApply(this::makeBoardResponse);
    }

    private BoardResponse makeBoardResponse(EntityWithIdAndVersion<BoardAggregate> e) {
        return new BoardResponse(e.getEntityId(),
                e.getAggregate().getBoard().getTitle(),
                e.getAggregate().getBoard().getCreation().getWho(),
                e.getAggregate().getBoard().getCreation().getWhen(),
                e.getAggregate().getBoard().getUpdate().getWho(),
                e.getAggregate().getBoard().getUpdate().getWhen());
    }
}


import io.eventuate.AggregateRepository;
import io.eventuate.EntityWithIdAndVersion;
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class BoardService {

    private final AggregateRepository<BoardAggregate, BoardCommand> aggregateRepository;

    private static Logger log = LoggerFactory.getLogger(BoardService.class);

    public BoardService(AggregateRepository<BoardAggregate, BoardCommand> aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    public CompletableFuture<EntityWithIdAndVersion<BoardAggregate>> save(BoardInfo board) {
        log.info("BoardService saving : {}", board);

        return aggregateRepository.save(new CreateBoardCommand(board));
    }
}


import io.eventuate.AggregateRepository;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.javaclient.spring.EnableEventHandlers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
@EnableEventHandlers
@EnableAutoConfiguration(exclude = {MongoRepositoriesAutoConfiguration.class})
@ComponentScan
public class BoardCommandSideConfiguration {

    @Bean
    public AggregateRepository<BoardAggregate, BoardCommand> boardAggregateRepository(EventuateAggregateStore eventStore) {
        return new AggregateRepository<>(BoardAggregate.class, eventStore);
    }

    @Bean
    public BoardService boardService(AggregateRepository<BoardAggregate, BoardCommand> boardAggregateRepository) {
        return new BoardService(boardAggregateRepository);
    }

    @Bean
    public HttpMessageConverters customConverters() {
        HttpMessageConverter<?> additional = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(additional);
    }

}


import io.eventuate.Event;
import io.eventuate.EventUtil;
import io.eventuate.ReflectiveMutableCommandProcessingAggregate;
import net.chrisrichardson.eventstore.examples.kanban.common.board.event.BoardCreatedEvent;
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BoardAggregate extends ReflectiveMutableCommandProcessingAggregate<BoardAggregate, BoardCommand> {

    private BoardInfo board;

    private static Logger log = LoggerFactory.getLogger(BoardAggregate.class);

    public List<Event> process(CreateBoardCommand cmd) {
        log.info("Calling BoardAggregate.process for CreateBoardCommand : {}", cmd);
        return EventUtil.events(new BoardCreatedEvent(cmd.getBoardInfo()));
    }

    public void apply(BoardCreatedEvent event) {
        log.info("Calling BoardAggregate.APPLY for BoardCreatedEvent : {}", event);

        this.board = event.getBoardInfo();
        this.board.setCreation(event.getBoardInfo().getCreation());
        this.board.setUpdate(event.getBoardInfo().getUpdate());
        this.board.setTitle(event.getBoardInfo().getTitle());
    }

    public BoardInfo getBoard() {
        return board;
    }
}



import io.eventuate.Command;

public interface BoardCommand extends Command {
}


import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commandside.task.TaskCommandSideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@Import({TaskCommandSideConfiguration.class, EventuateDriverConfiguration.class, WebConfiguration.class, AuthConfiguration.class, CommonSwaggerConfiguration.class})
@EnableAutoConfiguration
@ComponentScan
public class TaskCommandSideServiceConfiguration {
}


import net.chrisrichardson.eventstore.examples.kanban.taskcommandsideservice.TaskCommandSideServiceConfiguration;
import org.springframework.boot.SpringApplication;

public class TaskCommandSideServiceMain {
    public static void main(String[] args) {
        SpringApplication.run(TaskCommandSideServiceConfiguration.class, args);
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.AuthRequest;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.AuthResponse;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.token.Token;
import org.springframework.security.core.token.TokenService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@Validated
public class AuthController {

    @Autowired
    private TokenService tokenService;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(value = "/api/authenticate", method = POST)
    public ResponseEntity<AuthResponse> doAuth(@RequestBody @Valid AuthRequest request) throws IOException {
        User user = new User();
        user.setEmail(request.getEmail());

        Token token = tokenService.allocateToken(objectMapper.writeValueAsString(user));
        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse(token.getKey()));
    }
}


public class AuthResponse {
    private String token;

    public AuthResponse() {
    }

    public AuthResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

public class AuthRequest {

    @NotBlank
    @Email
    private String email;

    public AuthRequest() {
    }

    public AuthRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}



import io.eventuate.javaclient.spring.EnableEventHandlers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
@EnableEventHandlers
@EnableAutoConfiguration
@ComponentScan
@EnableMongoRepositories
public class BoardQuerySideConfiguration {

    @Bean
    public BoardQueryWorkflow boardQueryWorkflow(BoardUpdateService boardInfoUpdateService) {
        return new BoardQueryWorkflow(boardInfoUpdateService);
    }

    @Bean
    public BoardUpdateService boardInfoUpdateService(BoardRepository boardRepository) {
        return new BoardUpdateService(boardRepository);
    }

    @Bean
    public HttpMessageConverters customConverters() {
        HttpMessageConverter<?> additional = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(additional);
    }
}


import io.eventuate.DispatchedEvent;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import net.chrisrichardson.eventstore.examples.kanban.common.board.event.BoardCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventSubscriber(id = "boardEventHandlers")
public class BoardQueryWorkflow{

    private BoardUpdateService boardUpdateService;
    private static Logger log = LoggerFactory.getLogger(BoardQueryWorkflow.class);

    public BoardQueryWorkflow(BoardUpdateService boardInfoUpdateService) {
        this.boardUpdateService = boardInfoUpdateService;
    }

    @EventHandlerMethod
    public void create(DispatchedEvent<BoardCreatedEvent> de) {
        BoardCreatedEvent event = de.getEvent();
        String id = de.getEntityId();

        log.info("BoardQueryWorkflow got event : {}", de.getEvent());
        boardUpdateService.create(id, event.getBoardInfo());
    }
}

import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardQueryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardsQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class BoardQueryController {

    @Autowired
    private BoardRepository boardRepository;

    @RequestMapping(value = "api/boards", method = GET)
    public ResponseEntity<BoardsQueryResponse> listAllBoards() {
        return new ResponseEntity<>(new BoardsQueryResponse(boardRepository.findAll()), OK);
    }

    @RequestMapping(value = "api/boards/{id}", method = GET)
    public ResponseEntity<BoardQueryResponse> getBoard(@PathVariable("id") String id) {
        return Optional.ofNullable(boardRepository.findOne(id))
                .map(b -> new ResponseEntity<>(new BoardQueryResponse(b), OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.Board;

public class BoardUpdateService {
    private BoardRepository boardRepository;

    public BoardUpdateService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public Board create(String boardId, BoardInfo boardInfo) {
        return boardRepository.save(new Board(boardId,
                boardInfo.getTitle(),
                boardInfo.getCreation().getWho(),
                boardInfo.getCreation().getWhen(),
                boardInfo.getUpdate().getWhen(),
                boardInfo.getUpdate().getWho()));
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.board.model.Board;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BoardRepository extends MongoRepository<Board, String> {
}


import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@ComponentScan("net.chrisrichardson.eventstore.examples.kanban.commonweb")
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public ServletListenerRegistrationBean<RequestContextListener> httpRequestContextListener() {
        return new ServletListenerRegistrationBean<RequestContextListener>(new RequestContextListener());
    }

}


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.NoSuchElementException;

@ControllerAdvice
public class HttpExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity defaultErrorHandler(HttpServletRequest request, HttpServletResponse response, Exception e) throws Exception {
        logger.error("Exception occured!", e);
        if (e instanceof NoSuchElementException) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.badRequest().build();
    }
}

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CORSFilter implements Filter {

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE, PATCH");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, x-access-token, origin, content-type, accept");
        response.setHeader("Access-Control-Max-Age", "31536000");
        chain.doFilter(req, res);
    }

    public void init(FilterConfig filterConfig) {}

    public void destroy() {}

}


import net.chrisrichardson.eventstore.examples.kanban.testutil.AbstractStompApiTest;
import org.springframework.context.ApplicationContext;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = {E2ETestConfiguration.class})
@IntegrationTest({"server.port=0"})
public class StompApiTest extends AbstractStompApiTest {

    @Value("#{systemEnvironment['DOCKER_HOST_IP']}")
    private String hostName;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("#{systemEnvironment['DOCKER_PORT']}")
    private int port;

    @Override
    protected int getPort() {
        return this.port;
    }

    @Override
    protected String getHost() {
        return hostName;
    }

    @Override
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}

import net.chrisrichardson.eventstore.examples.kanban.testutil.AbstractRestApiTest;
import org.springframework.context.ApplicationContext;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = {E2ETestConfiguration.class})
@IntegrationTest({"server.port=0"})
public class RestApiTest extends AbstractRestApiTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("#{systemEnvironment['DOCKER_HOST_IP']}")
    private String hostName;

    @Value("#{systemEnvironment['DOCKER_PORT']}")
    private int port;

    @Override
    protected int getPort() {
        return this.port;
    }

    @Override
    protected String getHost() {
        return hostName;
    }

    @Override
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}

import net.chrisrichardson.eventstore.examples.kanban.testutil.AbstractAuthTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

@SpringApplicationConfiguration(classes = {E2ETestConfiguration.class})
@WebAppConfiguration
public class AuthControllerTest extends AbstractAuthTest {

    @Value("#{systemEnvironment['DOCKER_HOST_IP']}")
    private String hostName;

    @Value("#{systemEnvironment['DOCKER_PORT']}")
    private int port;

    @Override
    protected int getPort() {
        return this.port;
    }

    @Override
    protected String getHost() {
        return hostName;
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.javaclient.spring.jdbc.EventuateJdbcEventStoreConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebsocketEventsTranslator;
import net.chrisrichardson.eventstore.examples.kanban.testutil.BasicWebTestConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@Import({EventuateJdbcEventStoreConfiguration.class, BasicWebTestConfiguration.class})
public class E2ETestConfiguration {

    @Bean
    public WebsocketEventsTranslator websocketEventsTranslator(SimpMessagingTemplate template) {
        return new WebsocketEventsTranslator(template);
    }

    @Bean
    public RestTemplate restTemplate() {
        // we have to define Apache HTTP client to use the PATCH verb
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/json"));
        converter.setObjectMapper(new ObjectMapper());

        HttpClient httpClient = HttpClients.createDefault();
        RestTemplate restTemplate = new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        return restTemplate;
    }
}


import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.queryside.board.BoardQuerySideConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({BoardQuerySideConfiguration.class, EventuateDriverConfiguration.class, WebConfiguration.class, AuthConfiguration.class, CommonSwaggerConfiguration.class})
@EnableAutoConfiguration
@ComponentScan
public class BoardQuerySideServiceConfiguration {
}


import net.chrisrichardson.eventstore.examples.kanban.boardquerysideservice.BoardQuerySideServiceConfiguration;
import org.springframework.boot.SpringApplication;

public class BoardQuerySideServiceMain {
    public static void main(String[] args) {
        SpringApplication.run(BoardQuerySideServiceConfiguration.class, args);
    }
}


import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commandside.board.BoardCommandSideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commandside.task.TaskCommandSideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebSocketConfig;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebSocketSecurityConfig;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebsocketEventsTranslator;
import net.chrisrichardson.eventstore.examples.kanban.queryside.board.BoardQuerySideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.queryside.task.TaskQuerySideConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
@Import({EventuateDriverConfiguration.class, WebConfiguration.class, AuthConfiguration.class, WebSocketConfig.class, WebSocketSecurityConfig.class, BoardQuerySideConfiguration.class, TaskQuerySideConfiguration.class, BoardCommandSideConfiguration.class, TaskCommandSideConfiguration.class, CommonSwaggerConfiguration.class})
@ComponentScan
public class StandaloneServiceConfiguration {
  @Bean
  public WebsocketEventsTranslator websocketEventsTranslator(SimpMessagingTemplate template) {
    return new WebsocketEventsTranslator(template);
  }
}

import net.chrisrichardson.eventstore.examples.kanban.standalone.StandaloneServiceConfiguration;
import org.springframework.boot.SpringApplication;

public class StandaloneServiceMain {
    public static void main(String[] args) {
        SpringApplication.run(StandaloneServiceConfiguration.class, args);
    }

}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class CompleteTaskCommand implements TaskCommand {
    private String boardId;
    private AuditEntry update;

    public CompleteTaskCommand(String boardId, AuditEntry update) {
        this.boardId = boardId;
        this.update = update;
    }

    public String getBoardId() {
        return boardId;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ScheduleTaskCommand implements TaskCommand {
    private String boardId;
    private AuditEntry update;

    public ScheduleTaskCommand(String boardId, AuditEntry update) {
        this.boardId = boardId;
        this.update = update;
    }

    public String getBoardId() {
        return boardId;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;

public class DeleteTaskCommand implements TaskCommand {
    private AuditEntry update;

    public DeleteTaskCommand(AuditEntry update) {
        this.update = update;
    }

    public AuditEntry getUpdate() {
        return update;
    }
}


import io.eventuate.EntityWithIdAndVersion;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.ChangeTaskStatusRequest;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.HistoryEvent;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.HistoryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(value = "/api")
public class TaskCommandController {

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskHistoryService taskHistoryService;

    private static Logger log = LoggerFactory.getLogger(TaskCommandController.class);

    @RequestMapping(value = "/tasks", method = POST)
    public CompletableFuture<TaskResponse> saveTask(@RequestBody TaskInfo task) {
        return taskService.save(task).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}", method = DELETE)
    public CompletableFuture<TaskResponse> deleteTask(@PathVariable("id") String taskId) {
        return taskService.remove(taskId, getCurrentUser()).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}", method = PUT)
    public CompletableFuture<TaskResponse> updateTask(@PathVariable("id") String taskId,
                                                      @RequestBody TaskDetails request) {
        return taskService.update(taskId, request, getCurrentUser()).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}/start", method = PUT)
    public CompletableFuture<TaskResponse> startTask(@PathVariable("id") String taskId, @RequestBody ChangeTaskStatusRequest request) {
        return taskService.startTask(taskId, request.getBoardId(), getCurrentUser()).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}/schedule", method = PUT)
    public CompletableFuture<TaskResponse> scheduleTask(@PathVariable("id") String taskId, @RequestBody ChangeTaskStatusRequest request) {
        return taskService.scheduleTask(taskId, request.getBoardId(), getCurrentUser()).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}/complete", method = PUT)
    public CompletableFuture<TaskResponse> completeTask(@PathVariable("id") String taskId, @RequestBody ChangeTaskStatusRequest request) {
        return taskService.completeTask(taskId, request.getBoardId(), getCurrentUser()).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}/backlog", method = PUT)
    public CompletableFuture<TaskResponse> backlogTask(@PathVariable("id") String taskId) {
        return taskService.backlogTask(taskId, getCurrentUser()).thenApply(this::makeTaskResponse);
    }

    @RequestMapping(value = "/tasks/{id}/history", method = GET)
    public CompletableFuture<HistoryResponse> getHistory(@PathVariable("id") String taskId) {
        return taskHistoryService.getHistoryEvents(taskId).thenApply(ewm -> {
            log.info("Getting Task History {}", ewm.getEntity().getTask());
            return new HistoryResponse(ewm.getEvents().stream().map(e -> {
                HistoryEvent res = new HistoryEvent();
                res.setId(ewm.getEntityIdAndVersion().getEntityId());
                res.setEventType(e.getClass().getCanonicalName());
                res.setEventData(e);
                return res;
            }).collect(Collectors.toList()));
        });
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private TaskResponse makeTaskResponse(EntityWithIdAndVersion<TaskAggregate> e) {
        return new TaskResponse(e.getEntityId(),
                e.getAggregate().getTask().getBoardId(),
                e.getAggregate().getTask().getTaskDetails().getTitle(),
                e.getAggregate().getTask().getCreation().getWho(),
                e.getAggregate().getTask().getUpdate().getWho(),
                e.getAggregate().getTask().getCreation().getWhen(),
                e.getAggregate().getTask().getUpdate().getWhen(),
                e.getAggregate().getTask().getStatus(),
                e.getAggregate().getTask().isDeleted(),
                e.getAggregate().getTask().getTaskDetails().getDescription()
        );
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class StartTaskCommand implements TaskCommand {
    private String boardId;
    private AuditEntry update;

    public StartTaskCommand(String boardId, AuditEntry update) {
        this.boardId = boardId;
        this.update = update;
    }

    public String getBoardId() {
        return boardId;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import io.eventuate.Event;
import io.eventuate.EventUtil;
import io.eventuate.ReflectiveMutableCommandProcessingAggregate;
import net.chrisrichardson.eventstore.examples.kanban.common.task.*;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TaskAggregate extends ReflectiveMutableCommandProcessingAggregate<TaskAggregate, TaskCommand> {

    private TaskInfo task;

    private static Logger log = LoggerFactory.getLogger(TaskAggregate.class);

    public List<Event> process(CreateTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for CreateTaskCommand : {}", cmd);
        return EventUtil.events(new TaskCreatedEvent(cmd.getTaskInfo()));
    }

    public List<Event> process(UpdateTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for UpdateTaskCommand : {}", cmd);
        return EventUtil.events(new TaskUpdatedEvent(cmd.getTaskDetails(),
                cmd.getUpdate()));
    }

    public List<Event> process(DeleteTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for UpdateTaskCommand : {}", cmd);
        return EventUtil.events(new TaskDeletedEvent(cmd.getUpdate()));
    }

    public List<Event> process(StartTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for StartTaskCommand : {}", cmd);
        return EventUtil.events(new TaskStartedEvent(cmd.getBoardId(),
                cmd.getUpdate()));
    }

    public List<Event> process(ScheduleTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for ScheduleTaskCommand : {}", cmd);
        return EventUtil.events(new TaskScheduledEvent(cmd.getBoardId(),
                cmd.getUpdate()));
    }

    public List<Event> process(CompleteTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for CompleteTaskCommand : {}", cmd);
        return EventUtil.events(new TaskCompletedEvent(cmd.getBoardId(),
                cmd.getUpdate()));
    }

    public List<Event> process(MoveToBacklogTaskCommand cmd) {
        log.info("Calling TaskAggregate.process for BacklogTaskCommand : {}", cmd);
        return EventUtil.events(new TaskBacklogEvent(cmd.getUpdate()));
    }

    public void apply(TaskCreatedEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskCreatedEvent : {}", event);

        this.task = event.getTaskInfo();
        this.task.setUpdate(event.getTaskInfo().getUpdate());
        this.task.setCreation(event.getTaskInfo().getCreation());
        this.task.setStatus(TaskStatus.backlog);
    }

    public void apply(TaskDeletedEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskDeletedEvent : {}", event);
        this.task.setUpdate(event.getUpdate());
        this.task.setDeleted(true);
    }

    public void apply(TaskUpdatedEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskUpdatedEvent : {}", event);
        this.task.setTaskDetails(event.getTaskDetails());
        this.task.setUpdate(event.getUpdate());
    }

    public void apply(TaskStartedEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskStartedEvent : {}", event);
        this.task.setStatus(TaskStatus.started);
        this.task.setBoardId(event.getBoardId());
        this.task.setUpdate(event.getUpdate());
    }

    public void apply(TaskScheduledEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskScheduledEvent : {}", event);
        this.task.setStatus(TaskStatus.scheduled);
        this.task.setBoardId(event.getBoardId());
        this.task.setUpdate(event.getUpdate());
    }

    public void apply(TaskCompletedEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskCompletedEvent : {}", event);
        this.task.setStatus(TaskStatus.completed);
        this.task.setBoardId(event.getBoardId());
        this.task.setUpdate(event.getUpdate());
    }

    public void apply(TaskBacklogEvent event) {
        log.info("Calling TaskAggregate.APPLY for TaskMovedToBacklogEvent : {}", event);
        this.task.setStatus(TaskStatus.backlog);
        this.task.setUpdate(event.getUpdate());
    }

    public TaskInfo getTask() {
        return task;
    }
}

import io.eventuate.AggregateRepository;
import io.eventuate.EntityWithIdAndVersion;
import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class TaskService {

    private final AggregateRepository<TaskAggregate, TaskCommand> aggregateRepository;

    private static Logger log = LoggerFactory.getLogger(TaskService.class);

    public TaskService(AggregateRepository<TaskAggregate, TaskCommand> aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> save(TaskInfo task) {
        log.info("TaskService saving : {}", task);
        return aggregateRepository.save(new CreateTaskCommand(task));
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> update(String id, TaskDetails request, String updatedBy) {
        log.info("TaskService updating {}: {}", id, request);
        return aggregateRepository.update(id, new UpdateTaskCommand(request,
                new AuditEntry(updatedBy, new Date())));
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> remove(String id, String updatedBy) {
        log.info("TaskService deleting : {}", id);
        return aggregateRepository.update(id, new DeleteTaskCommand(new AuditEntry(updatedBy, new Date())));
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> startTask(String id, String boardId, String updatedBy) {
        log.info("TaskService starting task : {}", id);
        return aggregateRepository.update(id, new StartTaskCommand(boardId,
                new AuditEntry(updatedBy, new Date())));
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> scheduleTask(String id, String boardId, String updatedBy) {
        log.info("TaskService scheduling task : {}", id);
        return aggregateRepository.update(id, new ScheduleTaskCommand(boardId,
                new AuditEntry(updatedBy, new Date())));
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> completeTask(String id, String boardId, String updatedBy) {
        log.info("TaskService completing task : {}", id);
        return aggregateRepository.update(id, new CompleteTaskCommand(boardId,
                new AuditEntry(updatedBy, new Date())));
    }

    public CompletableFuture<EntityWithIdAndVersion<TaskAggregate>> backlogTask(String id, String updatedBy) {
        log.info("TaskService moving task to backlog : {}", id);
        return aggregateRepository.update(id, new MoveToBacklogTaskCommand(new AuditEntry(updatedBy, new Date())));
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class MoveToBacklogTaskCommand implements TaskCommand {
    private AuditEntry update;

    public MoveToBacklogTaskCommand(AuditEntry update) {
        this.update = update;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import org.apache.commons.lang.builder.ToStringBuilder;

public class CreateTaskCommand implements TaskCommand {
    private TaskInfo taskInfo;

    public CreateTaskCommand(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}




import io.eventuate.Command;

public interface TaskCommand extends Command {
}


import io.eventuate.EntityWithMetadata;
import io.eventuate.EventuateAggregateStore;
import java.util.concurrent.CompletableFuture;

public class TaskHistoryService {

    private EventuateAggregateStore eventStore;

    public TaskHistoryService(EventuateAggregateStore eventStore) {
        this.eventStore = eventStore;
    }

    public CompletableFuture<EntityWithMetadata<TaskAggregate>> getHistoryEvents(String taskId) {
        return eventStore.find(TaskAggregate.class, taskId);
    }
}


import io.eventuate.AggregateRepository;
import io.eventuate.EventuateAggregateStore;
import io.eventuate.javaclient.spring.EnableEventHandlers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
@EnableAutoConfiguration(exclude = {MongoRepositoriesAutoConfiguration.class})
@ComponentScan
@EnableEventHandlers
public class TaskCommandSideConfiguration {

    @Bean
    public AggregateRepository<TaskAggregate, TaskCommand> taskAggregateRepository(EventuateAggregateStore eventStore) {
        return new AggregateRepository<>(TaskAggregate.class, eventStore);
    }

    @Bean
    public TaskService taskService(AggregateRepository<TaskAggregate, TaskCommand> taskAggregateRepository) {
        return new TaskService(taskAggregateRepository);
    }

    @Bean
    public TaskHistoryService taskHistoryService(EventuateAggregateStore eventStore) {
        return new TaskHistoryService(eventStore);
    }

    @Bean
    public HttpMessageConverters customConverters() {
        HttpMessageConverter<?> additional = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(additional);
    }

}




import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import org.apache.commons.lang.builder.ToStringBuilder;

public class UpdateTaskCommand implements TaskCommand {
    private TaskDetails taskDetails;
    private AuditEntry update;

    public UpdateTaskCommand(TaskDetails taskDetails, AuditEntry update) {
        this.taskDetails = taskDetails;
        this.update = update;
    }

    public TaskDetails getTaskDetails() {
        return taskDetails;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.javaclient.spring.jdbc.EventuateJdbcEventStoreConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commandside.board.BoardCommandSideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commandside.task.TaskCommandSideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebsocketEventsTranslator;
import net.chrisrichardson.eventstore.examples.kanban.queryside.board.BoardQuerySideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.queryside.task.TaskQuerySideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.testutil.BasicWebTestConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@Import({EventuateJdbcEventStoreConfiguration.class, BasicWebTestConfiguration.class, BoardQuerySideConfiguration.class, TaskQuerySideConfiguration.class, BoardCommandSideConfiguration.class, TaskCommandSideConfiguration.class})
public class RestAPITestConfiguration {

    @Bean
    public WebsocketEventsTranslator websocketEventsTranslator(SimpMessagingTemplate template) {
        return new WebsocketEventsTranslator(template);
    }

    @Bean
    public RestTemplate restTemplate() {
        // we have to define Apache HTTP client to use the PATCH verb
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/json"));
        converter.setObjectMapper(new ObjectMapper());

        HttpClient httpClient = HttpClients.createDefault();
        RestTemplate restTemplate = new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        return restTemplate;
    }
}



import net.chrisrichardson.eventstore.examples.kanban.testutil.AbstractStompApiTest;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = {RestAPITestConfiguration.class})
@IntegrationTest({"server.port=0"})
public class StompApiTest extends AbstractStompApiTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getHost() {
        return "localhost";
    }

    @Override
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}

import net.chrisrichardson.eventstore.examples.kanban.testutil.AbstractRestApiTest;
import org.springframework.context.ApplicationContext;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@SpringApplicationConfiguration(classes = {RestAPITestConfiguration.class})
@IntegrationTest({"server.port=0"})
public class RestApiTest extends AbstractRestApiTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getHost() {
        return "localhost";
    }

    @Override
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}

import net.chrisrichardson.eventstore.examples.kanban.testutil.AbstractAuthTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

@SpringApplicationConfiguration(classes = {RestAPITestConfiguration.class})
@WebAppConfiguration
public class AuthControllerTest extends AbstractAuthTest {

    @Value("${local.server.port}")
    private int port;

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getHost() {
        return "localhost";
    }
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class TaskDescription {

    private String description;

    public TaskDescription() {
    }

    public TaskDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskInfo {
    private String boardId;
    private TaskDetails taskDetails;
    private AuditEntry creation;
    private AuditEntry update;
    private TaskStatus status;
    private boolean deleted;

    public TaskInfo() {
    }

    public TaskInfo(String boardId, TaskDetails taskDetails, AuditEntry creation, AuditEntry update, TaskStatus status, boolean deleted) {
        this.boardId = boardId;
        this.taskDetails = taskDetails;
        this.creation = creation;
        this.update = update;
        this.status = status;
        this.deleted = deleted;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public TaskDetails getTaskDetails() {
        return taskDetails;
    }

    public void setTaskDetails(TaskDetails taskDetails) {
        this.taskDetails = taskDetails;
    }

    public AuditEntry getCreation() {
        return creation;
    }

    public void setCreation(AuditEntry creation) {
        this.creation = creation;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


public enum TaskStatus {
    backlog, scheduled, started, completed
}


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskDetails {

    private String title;
    private TaskDescription description;

    public TaskDetails() {
    }

    public TaskDetails(String title, TaskDescription description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TaskDescription getDescription() {
        return description;
    }

    public void setDescription(TaskDescription description) {
        this.description = description;
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


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;

public interface DetailedTaskEvent {

    String getBoardId();

    AuditEntry getUpdate();
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskCompletedEvent extends TaskEvent implements DetailedTaskEvent {
    private String boardId;
    private AuditEntry update;

    public TaskCompletedEvent() {
    }

    public TaskCompletedEvent(String boardId, AuditEntry update) {
        this.boardId = boardId;
        this.update = update;
    }

    @Override
    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    @Override
    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskDeletedEvent extends TaskEvent implements DetailedTaskEvent {
    private AuditEntry update;

    public TaskDeletedEvent() {
    }

    public TaskDeletedEvent(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String getBoardId() {
        return null;
    }

    @Override
    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskEvent;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskUpdatedEvent extends TaskEvent {
    private TaskDetails taskDetails;
    private AuditEntry update;

    public TaskUpdatedEvent() {
    }

    public TaskUpdatedEvent(TaskDetails taskDetails, AuditEntry update) {
        this.taskDetails = taskDetails;
        this.update = update;
    }

    public TaskDetails getTaskDetails() {
        return taskDetails;
    }

    public void setTaskDetails(TaskDetails taskDetails) {
        this.taskDetails = taskDetails;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskBacklogEvent extends TaskEvent {

    private AuditEntry update;

    public TaskBacklogEvent() {
    }

    public TaskBacklogEvent(AuditEntry update) {
        this.update = update;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.DetailedTaskEvent;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskEvent;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskStartedEvent extends TaskEvent implements DetailedTaskEvent {
    private String boardId;
    private AuditEntry update;

    public TaskStartedEvent() {
    }

    public TaskStartedEvent(String boardId, AuditEntry update) {
        this.boardId = boardId;
        this.update = update;
    }

    @Override
    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskCreatedEvent extends TaskEvent {
    @JsonUnwrapped
    private TaskInfo taskInfo;

    public TaskCreatedEvent() {
    }

    public TaskCreatedEvent(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.DetailedTaskEvent;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskEvent;
import org.apache.commons.lang.builder.ToStringBuilder;

public class TaskScheduledEvent extends TaskEvent implements DetailedTaskEvent {
    private String boardId;
    private AuditEntry update;

    public TaskScheduledEvent() {
    }

    public TaskScheduledEvent(String boardId, AuditEntry update) {
        this.boardId = boardId;
        this.update = update;
    }

    @Override
    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    @Override
    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}



import io.eventuate.Event;
import io.eventuate.EventEntity;

@EventEntity(entity="net.chrisrichardson.eventstore.examples.kanban.commandside.task.TaskAggregate")
public abstract class TaskEvent implements Event {
}


import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;

import java.util.Date;

public class Task {
    @Id
    private String id;

    private String boardId;
    private String title;
    private String createdBy;
    private String updatedBy;
    private Date createdDate;
    private Date updatedDate;
    private TaskStatus status;
    private boolean deleted;
    private String description;

    public Task() {
    }

    public Task(String id, String boardId, String title, String createdBy, String updatedBy, Date createdDate, Date updatedDate, TaskStatus status, boolean deleted, String data) {
        this.id = id;
        this.boardId = boardId;
        this.title = title;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.status = status;
        this.deleted = deleted;
        this.description = data;
    }

    public static Task transform(String id, TaskInfo taskInfo) {
        Task res = new Task();
        res.setId(id);
        res.setBoardId(taskInfo.getBoardId());
        res.setTitle(taskInfo.getTaskDetails().getTitle());
        res.setCreatedBy(taskInfo.getCreation().getWho());
        res.setCreatedDate(taskInfo.getCreation().getWhen());
        res.setUpdatedDate(taskInfo.getUpdate().getWhen());
        res.setUpdatedBy(taskInfo.getUpdate().getWho());
        res.setStatus(taskInfo.getStatus());
        res.setDeleted(taskInfo.isDeleted());
        res.setDescription(taskInfo.getTaskDetails().getDescription()!=null?
                taskInfo.getTaskDetails().getDescription().getDescription() : null);
        return res;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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


import java.util.List;

public class HistoryResponse {

    private List<HistoryEvent> data;

    public HistoryResponse() {
    }

    public HistoryResponse(List<HistoryEvent> data) {
        this.data = data;
    }

    public List<HistoryEvent> getData() {
        return data;
    }

    public void setData(List<HistoryEvent> data) {
        this.data = data;
    }
}


public class ChangeTaskStatusRequest {
    private String boardId;

    public ChangeTaskStatusRequest() {
    }

    public ChangeTaskStatusRequest(String boardId) {
        this.boardId = boardId;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }
}


import java.util.List;

public class BacklogResponse {

    private List<Task> tasks;

    private List<Task> backlog;

    public BacklogResponse() {}

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Task> getBacklog() {
        return backlog;
    }

    public void setBacklog(List<Task> backlog) {
        this.backlog = backlog;
    }
}



import io.eventuate.Event;

public class HistoryEvent {
    private String id;
    private String eventType;
    private Event eventData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Event getEventData() {
        return eventData;
    }

    public void setEventData(Event eventData) {
        this.eventData = eventData;
    }
}


import com.fasterxml.jackson.annotation.JsonProperty;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDescription;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;

import java.util.Date;

public class TaskResponse {

    private String id;
    private String boardId;
    private String title;
    private String createdBy;
    private String updatedBy;
    private Date createdDate;
    private Date updatedDate;
    private TaskStatus status;
    private boolean deleted;
    @JsonProperty("data")
    private TaskDescription description;

    public TaskResponse() {
    }

    public TaskResponse(String id, String boardId, String title, String createdBy, String updatedBy, Date createdDate, Date updatedDate, TaskStatus status, boolean deleted, TaskDescription description) {
        this.id = id;
        this.boardId = boardId;
        this.title = title;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.status = status;
        this.deleted = deleted;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public TaskDescription getDescription() {
        return description;
    }

    public void setDescription(TaskDescription description) {
        this.description = description;
    }
}

import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.concurrent.CompletableFuture;

import static springfox.documentation.schema.AlternateTypeRules.newRule;

@Configuration
@EnableSwagger2
public class CommonSwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("net.chrisrichardson.eventstore.examples.kanban"))
                .build()
                .pathMapping("/")
                .genericModelSubstitutes(ResponseEntity.class, CompletableFuture.class)
                .alternateTypeRules(
                        newRule(typeResolver.resolve(DeferredResult.class,
                                        typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
                                typeResolver.resolve(WildcardType.class))
                )
                .useDefaultResponseMessages(false)
                ;
    }

    @Autowired
    private TypeResolver typeResolver;

}

import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commandside.board.BoardCommandSideConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({BoardCommandSideConfiguration.class, EventuateDriverConfiguration.class, WebConfiguration.class, AuthConfiguration.class, CommonSwaggerConfiguration.class})
@EnableAutoConfiguration
@ComponentScan
public class BoardCommandSideServiceConfiguration {

}


import net.chrisrichardson.eventstore.examples.kanban.boardcommandsideservice.BoardCommandSideServiceConfiguration;
import org.springframework.boot.SpringApplication;

public class BoardCommandSideServiceMain {
    public static void main(String[] args) {
        SpringApplication.run(BoardCommandSideServiceConfiguration.class, args);
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.User;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.UserAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.token.Token;
import org.springframework.security.core.token.TokenService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Service
public class TokenAuthenticationService {

    @Autowired
    private TokenService tokenService;

    private static final String AUTH_HEADER_NAME = "x-access-token";
    private static final long DAY = 1000 * 60 * 60 * 24;

    private ObjectMapper mapper = new ObjectMapper();

    public Authentication getAuthentication(HttpServletRequest request) throws IOException {
        final String tokenString = request.getHeader(AUTH_HEADER_NAME);

        if (tokenString != null) {
            Token token = tokenService.verifyToken(tokenString);
            final User user = mapper.readValue(token.getExtendedInformation(), User.class);

            if (user != null && (System.currentTimeMillis() - token.getKeyCreationTime()) < DAY) {
                return new UserAuthentication(user);
            }
        }
        return null;
    }

}


import net.chrisrichardson.eventstore.examples.kanban.commonauth.filter.StatelessAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.token.KeyBasedPersistenceTokenService;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.security.SecureRandom;

@Configuration
@ComponentScan("net.chrisrichardson.eventstore.examples.kanban.commonauth")
@EnableWebSecurity
@EnableConfigurationProperties({AuthProperties.class})
public class AuthConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthProperties securityProperties;

    @Autowired
    private TokenAuthenticationService tokenAuthenticationService;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .formLogin().loginPage("/index.html").and()
                .authorizeRequests()
                .antMatchers("/health").permitAll()
                .antMatchers("/swagger-ui.html").permitAll()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers("/js/**").permitAll()
                .antMatchers("/styles/**").permitAll()
                .antMatchers("/views/**").permitAll()
                .antMatchers("/webjars/**").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/configuration/**").permitAll()
                .antMatchers("/validatorUrl/**").permitAll()
                .antMatchers("/index.html").permitAll()
                .antMatchers("/events/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/authenticate").permitAll()
                .antMatchers(HttpMethod.GET, "/events").permitAll()
                .anyRequest().authenticated().and()
                .addFilterBefore(new StatelessAuthenticationFilter(tokenAuthenticationService), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public TokenService tokenService() {
        KeyBasedPersistenceTokenService res = new KeyBasedPersistenceTokenService();
        res.setSecureRandom(new SecureRandom());
        res.setServerSecret(securityProperties.getServerSecret());
        res.setServerInteger(securityProperties.getServerInteger());

        return res;
    }
}


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(locations = "classpath:auth.properties", ignoreUnknownFields = false, prefix = "auth")
public class AuthProperties {
    private String serverSecret;
    private Integer serverInteger;

    public String getServerSecret() {
        return serverSecret;
    }

    public void setServerSecret(String serverSecret) {
        this.serverSecret = serverSecret;
    }

    public Integer getServerInteger() {
        return serverInteger;
    }

    public void setServerInteger(Integer serverInteger) {
        this.serverInteger = serverInteger;
    }
}


import net.chrisrichardson.eventstore.examples.kanban.commonauth.TokenAuthenticationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class StatelessAuthenticationFilter extends GenericFilterBean {

    private final TokenAuthenticationService tokenAuthenticationService;

    public StatelessAuthenticationFilter(TokenAuthenticationService taService) {
        this.tokenAuthenticationService = taService;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
            ServletException {
        SecurityContextHolder.getContext().setAuthentication(
                tokenAuthenticationService.getAuthentication((HttpServletRequest) req));
        chain.doFilter(req, res);
    }
}

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements UserDetails {

    private String email;

    public void setUsername(String username) {
        this.email = username;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("USER");
        Set<GrantedAuthority> res = new HashSet<GrantedAuthority>();
        res.add(authority);
        return res;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UserAuthentication implements Authentication {

    private final User user;
    private boolean authenticated = true;

    public UserAuthentication(User user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public Object getCredentials() {
        return user.getPassword();
    }

    @Override
    public User getDetails() {
        return user;
    }

    @Override
    public Object getPrincipal() {
        return user.getUsername();
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}

import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.AuthRequest;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.AuthResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest({"server.port=0"})
public abstract class AbstractAuthTest {

    protected int port;

    private String baseUrl(String path) {
        return "http://"+getHost()+":" + getPort() + "/" + path;
    }

    @Autowired
    RestTemplate restTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldFailWithoutToken() throws IOException {
        try {
            Request.Get(baseUrl("/")).execute();
        } catch(HttpClientErrorException e) {
            Assert.assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    public void shouldSuccessWithToken() throws IOException {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("test@test.com");
        HttpResponse authResp = Request.Post(baseUrl("api/authenticate"))
                .bodyString(mapper.writeValueAsString(authRequest), ContentType.APPLICATION_JSON)
                .execute().returnResponse();

        Assert.assertEquals(HttpStatus.OK.value(), authResp.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(authResp.getEntity());
        Assert.assertNotNull(content);
        AuthResponse authResponse = mapper.readValue(content, AuthResponse.class);
        Assert.assertFalse(authResponse.getToken().isEmpty());
    }

    protected abstract int getPort();

    protected abstract String getHost();
}


import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebSocketConfig;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebSocketSecurityConfig;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebsocketEventsTranslator;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@Import({WebConfiguration.class, AuthConfiguration.class, WebSocketConfig.class, WebSocketSecurityConfig.class})
public class BasicWebTestConfiguration {

    @Bean
    public WebsocketEventsTranslator websocketEventsTranslator(SimpMessagingTemplate template) {
        return new WebsocketEventsTranslator(template);
    }

    @Bean
    public RestTemplate restTemplate() {
        // we have to define Apache HTTP client to use the PATCH verb
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/json"));
        converter.setObjectMapper(new ObjectMapper());

        HttpClient httpClient = HttpClients.createDefault();
        RestTemplate restTemplate = new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

        return restTemplate;
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.Board;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardQueryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardsQueryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDescription;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.BacklogResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.ChangeTaskStatusRequest;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.Task;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.TaskResponse;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.AuthRequest;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.model.AuthResponse;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.model.KanbanWebSocketEvent;
import net.chrisrichardson.eventstore.examples.kanban.testutil.model.TestHistoryEvent;
import net.chrisrichardson.eventstore.examples.kanban.testutil.model.TestHistoryResponse;
import net.chrisrichardson.eventstore.examples.kanban.testutil.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.chrisrichardson.eventstore.examples.kanban.testutil.util.TestUtil.awaitPredicatePasses;
import static net.chrisrichardson.eventstore.examples.kanban.testutil.util.TestUtil.awaitSuccessfulRequest;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class BaseTest {

    protected int port;

    protected String token;

    private ObjectMapper mapper = new ObjectMapper();

    protected String baseUrl(String path) {
        return "http://" + getHost() + ":" + getPort() + "/" + path;
    }

    protected String boardsUrl() {
        return baseUrl("api/boards");
    }

    protected String boardUrl(String id) {
        return baseUrl("api/boards/" + id);
    }

    protected String tasksUrl() {
        return baseUrl("api/tasks");
    }

    protected String tasksUrl(String boardId) {
        return baseUrl("api/tasks?boardId=" + boardId);
    }

    protected String taskUrl(String taskId) {
        return baseUrl("api/tasks/" + taskId);
    }

    protected String taskHistoryUrl(String taskId) {
        return baseUrl("api/tasks/" + taskId + "/history");
    }

    protected String startTaskUrl(String taskId) {
        return baseUrl("api/tasks/" + taskId + "/start");
    }

    protected String scheduleTaskUrl(String taskId) {
        return baseUrl("api/tasks/" + taskId + "/schedule");
    }

    protected String completeTaskUrl(String taskId) {
        return baseUrl("api/tasks/" + taskId + "/complete");
    }

    protected String backlogTaskUrl(String taskId) {
        return baseUrl("api/tasks/" + taskId + "/backlog");
    }

    protected String getWSJsonSchemaPath(String typeName) {
        return "classpath:schemas/websocket-events-schema/"+typeName+".json";
    }

    public void withToken(Consumer<String> func) throws IOException {
        if (token == null) {
            AuthRequest authRequest = new AuthRequest();
            authRequest.setEmail("test@test.com");
            HttpResponse authHttpResp = Request.Post(baseUrl("api/authenticate"))
                    .bodyString(mapper.writeValueAsString(authRequest), ContentType.APPLICATION_JSON)
                    .execute().returnResponse();

            Assert.assertEquals(HttpStatus.OK.value(), authHttpResp.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(authHttpResp.getEntity());
            assertNotNull(content);
            AuthResponse authResponse = mapper.readValue(content, AuthResponse.class);
            Assert.assertFalse(authResponse.getToken().isEmpty());
            token = authResponse.getToken();
        }
        func.accept(token);
    }

    protected void awaitTaskCreationInView(String token, String taskId) {
        awaitPredicatePasses(idx -> getBacklogTasks(token),
                (bqr) -> taskListContainsTaskWithId(taskId, bqr.getBacklog()));
    }

    protected void awaitTaskDeletionInView(String token, String taskId) {
        awaitPredicatePasses(idx -> getBacklogTasks(token),
                (bqr) -> !taskListContainsTaskWithId(taskId, bqr.getBacklog()));
    }

    protected BoardInfo transformToBoardInfo(Board board) {
        return new BoardInfo(board.getTitle(),
                new AuditEntry(board.getCreatedBy(), board.getCreatedDate()),
                new AuditEntry(board.getUpdatedBy(), board.getUpdatedDate()));

    }

    protected BoardInfo transformToBoardInfo(BoardResponse boardResponse) {
        return new BoardInfo(boardResponse.getTitle(),
                new AuditEntry(boardResponse.getCreatedBy(), boardResponse.getCreatedDate()),
                new AuditEntry(boardResponse.getUpdatedBy(), boardResponse.getUpdatedDate()));

    }

    protected Board transformToBoard(String id, BoardResponse boardResponse) {
        return new Board(id, boardResponse.getTitle(),
                boardResponse.getCreatedBy(),
                boardResponse.getCreatedDate(),
                boardResponse.getUpdatedDate(),
                boardResponse.getUpdatedBy());

    }

    protected Task transformToTask(String id, TaskResponse taskResponse) {
        return new Task(id,
                taskResponse.getBoardId(),
                taskResponse.getTitle(),
                taskResponse.getCreatedBy(),
                taskResponse.getUpdatedBy(),
                taskResponse.getCreatedDate(),
                taskResponse.getUpdatedDate(),
                taskResponse.getStatus(),
                taskResponse.isDeleted(),
                taskResponse.getDescription()!=null?
                        taskResponse.getDescription().getDescription():null);
    }

    protected TaskInfo transformToTaskInfo(TaskResponse taskResponse) {
        return new TaskInfo(taskResponse.getBoardId(),
                new TaskDetails(taskResponse.getTitle(), taskResponse.getDescription()),
                new AuditEntry(taskResponse.getCreatedBy(), taskResponse.getCreatedDate()),
                new AuditEntry(taskResponse.getUpdatedBy(), taskResponse.getUpdatedDate()),
                taskResponse.getStatus(),
                taskResponse.isDeleted());
    }

    protected TaskInfo transformToTaskInfo(Task task) {
        return new TaskInfo(task.getBoardId(),
                new TaskDetails(task.getTitle(), new TaskDescription(task.getDescription())),
                new AuditEntry(task.getCreatedBy(), task.getCreatedDate()),
                new AuditEntry(task.getUpdatedBy(), task.getUpdatedDate()),
                task.getStatus(),
                task.isDeleted());
    }

    protected TaskInfo generateTaskInfo() {
        return new TaskInfo("1",
                new TaskDetails("big task", new TaskDescription("data")),
                new AuditEntry("doctor@who.me", new Date()),
                new AuditEntry("doctor@who.me", new Date()),
                TaskStatus.backlog,
                false);
    }

    protected BoardInfo generateBoardInfo() {
        return new BoardInfo("big project",
                new AuditEntry("doctor@who.me", new Date()),
                new AuditEntry("doctor@who.me", new Date()));
    }

    protected void validateWSMessage(String eventType, String jsonText) {
        String eventName = StringUtils.substringAfterLast(eventType, ".");
        assertNotNull(eventName);
        try {
            Resource schemaFileResource = getApplicationContext().getResource(getWSJsonSchemaPath(eventName));

            String jsonSchema = new BufferedReader(new InputStreamReader(schemaFileResource.getInputStream())).lines().collect(Collectors.joining("\n"));

            assertTrue(ValidationUtils.isJsonValid(ValidationUtils.getSchemaNode(jsonSchema), ValidationUtils.getJsonNode(jsonText)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected KanbanWebSocketEvent filterWSEvents(List<KanbanWebSocketEvent> webSocketEvents, String eventType) {
        Optional<KanbanWebSocketEvent> eventOptional = webSocketEvents.stream().filter(kwse -> kwse.getEventType().contains(eventType)).findFirst();
        assertTrue(eventOptional.isPresent());
        return eventOptional.get();
    }

    protected boolean wsEventsContainsEvent(List<KanbanWebSocketEvent> webSocketEvents, String eventType) {
        return webSocketEvents.stream().anyMatch(kwse -> kwse.getEventType().contains(eventType));
    }

    protected boolean taskListContainsTaskWithId(String taskId, List<Task> tasks) {
        return tasks.stream().anyMatch(task -> task.getId().equals(taskId));
    }

    protected boolean taskListContains(Task expectedTask, List<Task> tasks) {
        return tasks.contains(expectedTask);
    }

    protected void assertBackLogContains(String token, String taskId, TaskResponse taskResponse) {
        BacklogResponse backlogResponse = getBacklogTasks(token);

        assertTrue(taskListContains(transformToTask(taskId, taskResponse),
                backlogResponse.getBacklog()));
    }

    protected void assertTaskNotIn(Task expectedTask, List<Task> tasks) {
        Assert.assertFalse(tasks.contains(expectedTask));
    }

    protected void assertBoardContains(Board expectedBoard, List<Board> boards) {
        assertTrue(boards.contains(expectedBoard));
    }

    protected void assertTaskInfoEquals(TaskInfo expectedTaskInfo, TaskInfo taskInfo) {
        Assert.assertEquals(expectedTaskInfo.getTaskDetails(), taskInfo.getTaskDetails());
        Assert.assertEquals(expectedTaskInfo.getCreation(), taskInfo.getCreation());
        Assert.assertEquals(expectedTaskInfo.getUpdate(), taskInfo.getUpdate());
        Assert.assertEquals(expectedTaskInfo.getStatus(), taskInfo.getStatus());
        Assert.assertEquals(expectedTaskInfo.isDeleted(), taskInfo.isDeleted());
    }

    protected void assertBoardInfoEquals(BoardInfo expectedBoardInfo, BoardInfo boardInfo) {
        Assert.assertEquals(expectedBoardInfo.getTitle(), boardInfo.getTitle());
        Assert.assertEquals(expectedBoardInfo.getCreation(), boardInfo.getCreation());
        Assert.assertEquals(expectedBoardInfo.getUpdate(), boardInfo.getUpdate());
    }

    protected void assertTaskHistoryContainsEvent(List<TestHistoryEvent> historyEvents, String eventName) {
        assertTrue(historyEvents.stream().filter(he -> he.getEventType().equals(eventName)).findFirst().isPresent());
    }

    protected BoardQueryResponse getBoard(String token, String boardId) {
        try {
            HttpResponse httpResp = Request.Get(boardUrl(boardId))
                    .addHeader("x-access-token", token)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());
            Assert.assertFalse(responseContent.isEmpty());
            return mapper.readValue(responseContent, BoardQueryResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BoardsQueryResponse getAllBoards(String token) {
        try {
            HttpResponse httpResp = Request.Get(boardsUrl())
                    .addHeader("x-access-token", token)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());
            Assert.assertFalse(responseContent.isEmpty());
            return mapper.readValue(responseContent, BoardsQueryResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BacklogResponse getBacklogTasks(String token) {
        try {
            HttpResponse httpResp = Request.Get(tasksUrl())
                    .addHeader("x-access-token", token)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());
            Assert.assertFalse(responseContent.isEmpty());
            return mapper.readValue(responseContent, BacklogResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BacklogResponse getTasksForBoard(String token, String boardId) {
        try {
            HttpResponse httpResp = Request.Get(tasksUrl(boardId))
                    .addHeader("x-access-token", token)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());
            Assert.assertFalse(responseContent.isEmpty());
            return mapper.readValue(responseContent, BacklogResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TestHistoryResponse getHistoryForTask(String token, String taskId) {
        try {
            HttpResponse httpResp = Request.Get(taskHistoryUrl(taskId))
                    .addHeader("x-access-token", token)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());
            Assert.assertFalse(responseContent.isEmpty());
            return mapper.readValue(responseContent, TestHistoryResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TaskResponse createTaskAndWaitInView(String token, TaskInfo taskInfo) {
        try {
            HttpResponse httpResp = Request.Post(tasksUrl())
                    .addHeader("x-access-token", token)
                    .bodyString(mapper.writeValueAsString(taskInfo), ContentType.APPLICATION_JSON)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());

            Assert.assertFalse(responseContent.isEmpty());
            TaskResponse resp = mapper.readValue(responseContent, TaskResponse.class);

            awaitTaskCreationInView(token, resp.getId());

            return resp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TaskResponse deleteTaskAndWaitInView(String token, String taskId) {
        try {
            HttpResponse httpResp = Request.Delete(taskUrl(taskId))
                    .addHeader("x-access-token", token)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());

            Assert.assertFalse(responseContent.isEmpty());
            TaskResponse resp = mapper.readValue(responseContent, TaskResponse.class);

            awaitTaskDeletionInView(token, resp.getId());

            return resp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TaskResponse updateTaskAndWaitInView(String token, String taskId, TaskDetails request) {
        try {
            HttpResponse httpResp = Request.Put(taskUrl(taskId))
                    .addHeader("x-access-token", token)
                    .bodyString(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());

            Assert.assertFalse(responseContent.isEmpty());
            TaskResponse resp = mapper.readValue(responseContent, TaskResponse.class);

            awaitTaskCreationInView(token, resp.getId());
            return mapper.readValue(responseContent, TaskResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TaskResponse changeTaskState(String token, String url, ChangeTaskStatusRequest request) {
        try {
            HttpResponse httpResp = Request.Put(url)
                    .addHeader("x-access-token", token)
                    .bodyString(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());

            Assert.assertFalse(responseContent.isEmpty());
            return mapper.readValue(responseContent, TaskResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TaskResponse startTaskAndWaitInView(String token, String boardId, String taskId) {
        TaskResponse updatedTaskResponse = changeTaskState(token, startTaskUrl(taskId), new ChangeTaskStatusRequest(boardId));

        awaitPredicatePasses(idx -> getTasksForBoard(token, boardId),
                (bqr) -> taskListContains(transformToTask(taskId, updatedTaskResponse), bqr.getTasks()));
        return updatedTaskResponse;
    }

    protected TaskResponse scheduleTaskAndWaitInView(String token, String boardId, String taskId) {
        TaskResponse updatedTaskResponse = changeTaskState(token, scheduleTaskUrl(taskId), new ChangeTaskStatusRequest(boardId));

        awaitPredicatePasses(idx -> getTasksForBoard(token, boardId),
                (bqr) -> taskListContains(transformToTask(taskId, updatedTaskResponse), bqr.getTasks()));
        return updatedTaskResponse;
    }

    protected TaskResponse completeTaskAndWaitInView(String token, String boardId, String taskId) {
        TaskResponse updatedTaskResponse = changeTaskState(token, completeTaskUrl(taskId), new ChangeTaskStatusRequest(boardId));

        awaitPredicatePasses(idx -> getTasksForBoard(token, boardId),
                (bqr) -> taskListContains(transformToTask(taskId, updatedTaskResponse), bqr.getTasks()));
        return updatedTaskResponse;
    }

    protected BoardResponse createBoardAndWaitInView(String token, BoardInfo boardInfo) {
        try {
            HttpResponse httpResp = Request.Post(boardsUrl())
                    .addHeader("x-access-token", token)
                    .bodyString(mapper.writeValueAsString(boardInfo), ContentType.APPLICATION_JSON)
                    .execute().returnResponse();
            Assert.assertEquals(HttpStatus.OK.value(), httpResp.getStatusLine().getStatusCode());
            String responseContent = EntityUtils.toString(httpResp.getEntity());
            Assert.assertFalse(responseContent.isEmpty());
            BoardResponse resp = mapper.readValue(responseContent, BoardResponse.class);

            awaitBoardCreationInView(token, resp.getId());

            return resp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void awaitBoardCreationInView(String token, String boardId) throws IOException {
        awaitSuccessfulRequest(() -> {
            try {
                return Request.Get(boardUrl(boardId))
                        .addHeader("x-access-token", token)
                        .execute().returnResponse();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected abstract int getPort();

    protected abstract String getHost();

    protected abstract ApplicationContext getApplicationContext();
}


import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.board.event.BoardCreatedEvent;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.Board;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDescription;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.*;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.ChangeTaskStatusRequest;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.Task;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.TaskResponse;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.model.KanbanWebSocketEvent;
import net.chrisrichardson.eventstore.examples.kanban.testutil.util.StompListener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static net.chrisrichardson.eventstore.examples.kanban.testutil.util.TestUtil.awaitPredicatePasses;

@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest({"server.port=0"})
public abstract class AbstractStompApiTest extends BaseTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldCreateBoards() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);
            assertBoardInfoEquals(boardInfo, transformToBoardInfo(boardResponse));

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "BoardCreatedEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "BoardCreatedEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(BoardCreatedEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Board board = parseStompEvent(wsEvent, Board.class);
            assertBoardInfoEquals(boardInfo, transformToBoardInfo(board));
        });
    }

    @Test
    public void shouldCreateTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            assertTaskInfoEquals(taskInfo, transformToTaskInfo(taskResponse));

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskCreatedEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskCreatedEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskCreatedEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);
            assertTaskInfoEquals(taskInfo, transformToTaskInfo(task));
        });
    }

    @Test
    public void shouldUpdateTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);
            TaskDetails taskDetails = new TaskDetails(
                    "small task",
                    new TaskDescription("description"));

            TaskResponse taskUpdatedResponse = updateTaskAndWaitInView(t, taskResponse.getId(), taskDetails);

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskUpdatedEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskUpdatedEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskUpdatedEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);

            Assert.assertEquals(taskUpdatedResponse.getDescription().getDescription(), task.getDescription());
            Assert.assertEquals(taskUpdatedResponse.getUpdatedBy(), task.getUpdatedBy());
            Assert.assertEquals(taskUpdatedResponse.getUpdatedDate(), task.getUpdatedDate());
            Assert.assertEquals(taskUpdatedResponse.getTitle(), task.getTitle());
        });
    }

    @Test
    public void shouldBacklogTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskInfo taskInfo = generateTaskInfo();
            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            TaskResponse taskChangedResponse = changeTaskState(token, backlogTaskUrl(taskResponse.getId()), new ChangeTaskStatusRequest(boardResponse.getId()));

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskBacklogEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskBacklogEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskBacklogEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);

            Assert.assertEquals(taskChangedResponse.getId(), task.getId());
            Assert.assertEquals(taskChangedResponse.getUpdatedBy(), task.getUpdatedBy());
            Assert.assertEquals(taskChangedResponse.getUpdatedDate(), task.getUpdatedDate());
            Assert.assertEquals("", task.getBoardId());
            Assert.assertEquals(taskChangedResponse.getStatus(), task.getStatus());
        });
    }

    @Test
    public void shouldCompleteTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskInfo taskInfo = generateTaskInfo();
            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            TaskResponse taskChangedResponse = changeTaskState(token, completeTaskUrl(taskResponse.getId()), new ChangeTaskStatusRequest(boardResponse.getId()));

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskCompletedEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskCompletedEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskCompletedEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);

            Assert.assertEquals(taskChangedResponse.getId(), task.getId());
            Assert.assertEquals(taskChangedResponse.getUpdatedBy(), task.getUpdatedBy());
            Assert.assertEquals(taskChangedResponse.getUpdatedDate(), task.getUpdatedDate());
            Assert.assertEquals(taskChangedResponse.getBoardId(), task.getBoardId());
            Assert.assertEquals(taskChangedResponse.getStatus(), task.getStatus());
        });
    }

    @Test
    public void shouldDeleteTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            TaskInfo taskInfo = generateTaskInfo();
            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            TaskResponse taskChangedResponse = deleteTaskAndWaitInView(token, taskResponse.getId());

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskDeletedEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskDeletedEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskDeletedEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);

            Assert.assertEquals(taskChangedResponse.getId(), task.getId());
            Assert.assertEquals(taskChangedResponse.getUpdatedBy(), task.getUpdatedBy());
            Assert.assertEquals(taskChangedResponse.getUpdatedDate(), task.getUpdatedDate());
            Assert.assertTrue(task.isDeleted());
        });
    }

    @Test
    public void shouldScheduleTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskInfo taskInfo = generateTaskInfo();
            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            TaskResponse taskChangedResponse = changeTaskState(token, scheduleTaskUrl(taskResponse.getId()), new ChangeTaskStatusRequest(boardResponse.getId()));

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskScheduledEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskScheduledEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskScheduledEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);

            Assert.assertEquals(taskChangedResponse.getId(), task.getId());
            Assert.assertEquals(taskChangedResponse.getUpdatedBy(), task.getUpdatedBy());
            Assert.assertEquals(taskChangedResponse.getUpdatedDate(), task.getUpdatedDate());
            Assert.assertEquals(taskChangedResponse.getBoardId(), task.getBoardId());
            Assert.assertEquals(taskChangedResponse.getStatus(), task.getStatus());
        });
    }

    @Test
    public void shouldStartTasks() throws IOException, InterruptedException {
        withToken(t -> {
            StompListener taskEventsListener = new StompListener(t, "/events", getHost(), getPort());

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskInfo taskInfo = generateTaskInfo();
            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            TaskResponse taskChangedResponse = changeTaskState(token, startTaskUrl(taskResponse.getId()), new ChangeTaskStatusRequest(boardResponse.getId()));

            awaitPredicatePasses(idx -> taskEventsListener.getEvents(),
                    (list) -> wsEventsContainsEvent(list, "TaskStartedEvent"));
            KanbanWebSocketEvent wsEvent = filterWSEvents(taskEventsListener.getEvents(), "TaskStartedEvent");
            Assert.assertNotNull(wsEvent.getEntityId());
            Assert.assertEquals(TaskStartedEvent.class.getName(), wsEvent.getEventType());

            validateWSMessage(wsEvent.getEventType(), wsEvent.getEventData());

            Task task = parseStompEvent(wsEvent, Task.class);

            Assert.assertEquals(taskChangedResponse.getId(), task.getId());
            Assert.assertEquals(taskChangedResponse.getUpdatedBy(), task.getUpdatedBy());
            Assert.assertEquals(taskChangedResponse.getUpdatedDate(), task.getUpdatedDate());
            Assert.assertEquals(taskChangedResponse.getBoardId(), task.getBoardId());
            Assert.assertEquals(taskChangedResponse.getStatus(), task.getStatus());
        });
    }

    private <T> T parseStompEvent(KanbanWebSocketEvent stompEvent, Class<T> clazz) {
        try {
            return mapper.readValue(stompEvent.getEventData(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardQueryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.BoardsQueryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDescription;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskDetails;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.BacklogResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.ChangeTaskStatusRequest;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.HistoryResponse;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.TaskResponse;
import net.chrisrichardson.eventstore.examples.kanban.testutil.model.TestHistoryResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static net.chrisrichardson.eventstore.examples.kanban.testutil.util.TestUtil.awaitPredicatePasses;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest({"server.port=0"})
public abstract class AbstractRestApiTest extends BaseTest {

    @Test
    public void shouldGetBoardById() throws IOException, InterruptedException {
        withToken(t -> {
            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            BoardQueryResponse savedBoardResponse = getBoard(t, boardResponse.getId());

            assertBoardInfoEquals(boardInfo, transformToBoardInfo(savedBoardResponse.getData()));
        });
    }

    @Test
    public void shouldGetAllBoards() throws IOException, InterruptedException {
        withToken(t -> {
            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            BoardsQueryResponse boardsResponse = getAllBoards(t);

            assertBoardContains(transformToBoard(boardResponse.getId(), boardResponse), boardsResponse.getBoards());
        });
    }

    @Test
    public void shouldGetBacklogTask() throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            assertBackLogContains(t, taskResponse.getId(), taskResponse);
        });
    }

    @Test
    public void shouldDeleteTask() throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            deleteTaskAndWaitInView(t, taskResponse.getId());

            BacklogResponse backlogResponse = getBacklogTasks(t);

            assertTaskNotIn(transformToTask(taskResponse.getId(), taskResponse),
                    backlogResponse.getBacklog());
        });
    }


    @Test
    public void shouldUpdateTask() throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            TaskDetails taskDetails = new TaskDetails(
                    "small task",
                    new TaskDescription("description"));

            TaskResponse updatedTaskResponse = updateTaskAndWaitInView(t, taskResponse.getId(), taskDetails);

            BacklogResponse backlogResponse = getBacklogTasks(t);

            assertTrue(taskListContains(transformToTask(taskResponse.getId(), updatedTaskResponse),
                    backlogResponse.getBacklog()));
        });
    }

    @Test
    public void shouldStartTask() throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskResponse updatedTaskResponse = startTaskAndWaitInView(t, boardResponse.getId(), taskResponse.getId());

            assertTrue(taskListContains(transformToTask(taskResponse.getId(), updatedTaskResponse), getTasksForBoard(token, boardResponse.getId()).getTasks()));
        });
    }

    @Test
    public void shouldScheduleTask() throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskResponse updatedTaskResponse = scheduleTaskAndWaitInView(t, boardResponse.getId(), taskResponse.getId());

            assertTrue(taskListContains(transformToTask(taskResponse.getId(), updatedTaskResponse), getTasksForBoard(token, boardResponse.getId()).getTasks()));
        });
    }

    @Test
    public void shouldCompleteTask() throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            TaskResponse updatedTaskResponse = completeTaskAndWaitInView(t, boardResponse.getId(), taskResponse.getId());

            assertTrue(taskListContains(transformToTask(taskResponse.getId(), updatedTaskResponse), getTasksForBoard(token, boardResponse.getId()).getTasks()));
        });
    }

    @Test
    public void shouldGetTaskHistory()  throws IOException, InterruptedException {
        withToken(t -> {
            TaskInfo taskInfo = generateTaskInfo();

            TaskResponse taskResponse = createTaskAndWaitInView(t, taskInfo);

            BoardInfo boardInfo = generateBoardInfo();
            BoardResponse boardResponse = createBoardAndWaitInView(t, boardInfo);

            startTaskAndWaitInView(t, boardResponse.getId(), taskResponse.getId());
            scheduleTaskAndWaitInView(t, boardResponse.getId(), taskResponse.getId());
            completeTaskAndWaitInView(t, boardResponse.getId(), taskResponse.getId());
            deleteTaskAndWaitInView(t, taskResponse.getId());

            TestHistoryResponse historyResponse = getHistoryForTask(t, taskResponse.getId());
            assertTaskHistoryContainsEvent(historyResponse.getData(), "net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskCreatedEvent");
            assertTaskHistoryContainsEvent(historyResponse.getData(), "net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskStartedEvent");
            assertTaskHistoryContainsEvent(historyResponse.getData(), "net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskScheduledEvent");
            assertTaskHistoryContainsEvent(historyResponse.getData(), "net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskCompletedEvent");
            assertTaskHistoryContainsEvent(historyResponse.getData(), "net.chrisrichardson.eventstore.examples.kanban.common.task.event.TaskDeletedEvent");
        });
    }
}
/*
 * Copyright 2002-2014 the original author or authors.
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



public interface StompSession {

	void subscribe(String destination, String receiptId);

	void send(String destination, Object payload);

	void disconnect();

}

/*
 * Copyright 2002-2014 the original author or authors.
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



import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;


public interface StompMessageHandler {

	void afterConnected(StompSession session, StompHeaderAccessor headers);

	void handleMessage(Message<byte[]> message);

	void handleReceipt(String receiptId);

	void handleError(Message<byte[]> message);

	void afterDisconnected();

}

/*
 * Copyright 2002-2014 the original author or authors.
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



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public class WebSocketStompClient implements StompClient {

	private static Log logger = LogFactory.getLog(WebSocketStompClient.class);


	private final URI uri;

	private final WebSocketHttpHeaders headers;

	private final WebSocketClient webSocketClient;

	private MessageConverter messageConverter;


	public WebSocketStompClient(URI uri, WebSocketHttpHeaders headers, WebSocketClient webSocketClient) {
		this.uri = uri;
		this.headers = headers;
		this.webSocketClient = webSocketClient;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	public void connect(StompMessageHandler stompMessageHandler) {
		try {
			StompWebSocketHandler webSocketHandler = new StompWebSocketHandler(stompMessageHandler, this.messageConverter);
			this.webSocketClient.doHandshake(webSocketHandler, this.headers, this.uri).get();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	private static class StompWebSocketHandler extends AbstractWebSocketHandler {

		private static final Charset UTF_8 = Charset.forName("UTF-8");

		private final StompMessageHandler stompMessageHandler;

		private final MessageConverter messageConverter;

		private final StompEncoder encoder = new StompEncoder();

		private final StompDecoder decoder = new StompDecoder();


		private StompWebSocketHandler(StompMessageHandler delegate) {
			this(delegate, new MappingJackson2MessageConverter());
		}

		private StompWebSocketHandler(StompMessageHandler delegate, MessageConverter messageConverter) {
			this.stompMessageHandler = delegate;
			this.messageConverter = messageConverter;
		}


		@Override
		public void afterConnectionEstablished(WebSocketSession session) throws Exception {

			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
			headers.setAcceptVersion("1.1,1.2");
			headers.setHeartbeat(0, 0);
			Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

			TextMessage textMessage = new TextMessage(new String(this.encoder.encode(message), UTF_8));
			session.sendMessage(textMessage);
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {

			ByteBuffer payload = ByteBuffer.wrap(textMessage.getPayload().getBytes(UTF_8));
			List<Message<byte[]>> messages = this.decoder.decode(payload);

			for (Message message : messages) {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (StompCommand.CONNECTED.equals(headers.getCommand())) {
					WebSocketStompSession stompSession = new WebSocketStompSession(session, this.messageConverter);
					this.stompMessageHandler.afterConnected(stompSession, headers);
				}
				else if (StompCommand.MESSAGE.equals(headers.getCommand())) {
					this.stompMessageHandler.handleMessage(message);
				}
				else if (StompCommand.RECEIPT.equals(headers.getCommand())) {
					this.stompMessageHandler.handleReceipt(headers.getReceiptId());
				}
				else if (StompCommand.ERROR.equals(headers.getCommand())) {
					this.stompMessageHandler.handleError(message);
				}
				else if (StompCommand.ERROR.equals(headers.getCommand())) {
					this.stompMessageHandler.afterDisconnected();
				}
				else {
					logger.debug("Unhandled message " + message);
				}
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
			logger.error("WebSocket transport error", exception);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
			this.stompMessageHandler.afterDisconnected();
		}
	}


}

/*
 * Copyright 2002-2014 the original author or authors.
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



public interface StompClient {

	void connect(StompMessageHandler messageHandler);

}


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ConsumerStompMessageHandler implements StompMessageHandler {

    private final int expectedMessageCount;

    private final CountDownLatch connectLatch;

    private final CountDownLatch subscribeLatch;

    private final CountDownLatch messageLatch;

    private final CountDownLatch disconnectLatch;

    private final AtomicReference<Throwable> failure;

    private StompSession stompSession;

    private AtomicInteger messageCount = new AtomicInteger(0);

    private static Log logger = LogFactory.getLog(ConsumerStompMessageHandler.class);


    public ConsumerStompMessageHandler(int expectedMessageCount, CountDownLatch connectLatch,
                                       CountDownLatch subscribeLatch, CountDownLatch messageLatch, CountDownLatch disconnectLatch,
                                       AtomicReference<Throwable> failure) {

        this.expectedMessageCount = expectedMessageCount;
        this.connectLatch = connectLatch;
        this.subscribeLatch = subscribeLatch;
        this.messageLatch = messageLatch;
        this.disconnectLatch = disconnectLatch;
        this.failure = failure;
    }


    @Override
    public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
        this.connectLatch.countDown();
        this.stompSession = stompSession;
        stompSession.subscribe("/topic/greeting", "receipt1");
    }

    @Override
    public void handleReceipt(String receiptId) {
        this.subscribeLatch.countDown();
    }

    @Override
    public void handleMessage(Message<byte[]> message) {
        if (this.messageCount.incrementAndGet() == this.expectedMessageCount) {
            this.messageLatch.countDown();
            this.stompSession.disconnect();
        }
    }

    @Override
    public void handleError(Message<byte[]> message) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String error = "[Consumer] " + accessor.getShortLogMessage(message.getPayload());
        logger.error(error);
        this.failure.set(new Exception(error));
    }

    @Override
    public void afterDisconnected() {
        logger.trace("Disconnected in " + this.stompSession);
        this.disconnectLatch.countDown();
    }

    @Override
    public String toString() {
        return "ConsumerStompMessageHandler[messageCount=" + this.messageCount + ", " + this.stompSession +  "]";
    }
}

/*
 * Copyright 2002-2014 the original author or authors.
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



import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;


public class WebSocketStompSession implements StompSession {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	public static final byte[] EMPTY_PAYLOAD = new byte[0];


	private final String id;

	private final WebSocketSession webSocketSession;

	private final MessageConverter messageConverter;

	private final StompEncoder encoder = new StompEncoder();

	private final AtomicInteger subscriptionIndex = new AtomicInteger();


	public WebSocketStompSession(WebSocketSession delegate) {
		this(delegate, new MappingJackson2MessageConverter());
	}

	public WebSocketStompSession(WebSocketSession webSocketSession, MessageConverter messageConverter) {
		Assert.notNull(webSocketSession);
		Assert.notNull(messageConverter);
		this.id = webSocketSession.getId();
		this.webSocketSession = webSocketSession;
		this.messageConverter = messageConverter;
	}

	public void subscribe(String destination, String receiptId) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSubscriptionId("sub" + this.subscriptionIndex.getAndIncrement());
		headers.setDestination(destination);
		if (receiptId != null) {
			headers.setReceipt(receiptId);
		}
		sendInternal(MessageBuilder.withPayload(EMPTY_PAYLOAD).setHeaders(headers).build());
	}

	public void send(String destination, Object payload) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setDestination(destination);
		sendInternal((Message<byte[]>)this.messageConverter.toMessage(payload, new MessageHeaders(headers.toMap())));
	}

	public void disconnect() {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		Message<byte[]> message = MessageBuilder.withPayload(EMPTY_PAYLOAD).setHeaders(headers).build();
		sendInternal(message);
		try {
			this.webSocketSession.close();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void sendInternal(Message<byte[]> message) {
		byte[] bytes = this.encoder.encode(message);
		try {
			this.webSocketSession.sendMessage(new TextMessage(new String(bytes, UTF_8)));
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		return this.webSocketSession.toString();
	}
}


import com.fasterxml.jackson.databind.ObjectMapper;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.model.KanbanWebSocketEvent;
import net.chrisrichardson.eventstore.examples.kanban.testutil.client.StompMessageHandler;
import net.chrisrichardson.eventstore.examples.kanban.testutil.client.StompSession;
import net.chrisrichardson.eventstore.examples.kanban.testutil.client.WebSocketStompClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class StompListener {

    private String token;
    private String destination;
    private String host;
    private int port;

    private ObjectMapper mapper = new ObjectMapper();

    private List<KanbanWebSocketEvent> events = new ArrayList<>();

    private static Log log = LogFactory.getLog(StompListener.class);

    public StompListener(String token, String destination, String host, int port) {
        this.token = token;
        this.destination = destination;
        this.host = host;
        this.port = port;

        initializeStompClient();
    }

    private void initializeStompClient() {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport(new RestTemplate()));

        StompMessageHandler handler = new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                this.stompSession.subscribe(destination, null);

            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                String json = new String(message.getPayload());
                try {
                    events.add(mapper.readValue(json, KanbanWebSocketEvent.class));
                } catch (IOException e) {
                    new RuntimeException(e);
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                log.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {}

            @Override
            public void afterDisconnected() {}
        };

        try {
            URI uri = new URI("http://"+host+":"+port+"/events");
            WebSocketStompClient stompClient = new WebSocketStompClient(uri, headers, new SockJsClient(transports));
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            headers.add("x-access-token", token);
            stompClient.connect(handler);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    public List<KanbanWebSocketEvent> getEvents() {
        return events;
    }
}

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TestUtil {

    public static <T> T awaitPredicatePasses(Func1<Long, T> func, Func1<T, Boolean> predicate) {
        try {
            return Observable.interval(400, TimeUnit.MILLISECONDS)
                    .take(50)
                    .map(func)
                    .filter(predicate)
                    .toBlocking().first();

        } catch (Exception e) {
            // Rx Java throws an exception with a stack trace from a different thread
            //  https://github.com/ReactiveX/RxJava/issues/3558
            throw new RuntimeException(e);
        }
    }

    public static String awaitSuccessfulRequest(Supplier<HttpResponse> func) {
        try {
            return EntityUtils.toString(Observable.interval(400, TimeUnit.MILLISECONDS)
                    .take(50)
                    .map(x -> func.get())
                    .filter(httpResp -> httpResp.getStatusLine().getStatusCode() == HttpStatus.OK.value() && httpResp.getEntity() != null)
                    .toBlocking().first().getEntity());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ValidationUtils {

    public static final String JSON_V4_SCHEMA_IDENTIFIER = "http://json-schema.org/draft-04/schema#";
    public static final String JSON_SCHEMA_IDENTIFIER_ELEMENT = "$schema";

    public static JsonNode getJsonNode(String jsonText)
            throws IOException {
        return JsonLoader.fromString(jsonText);
    }

    public static JsonNode getJsonNode(File jsonFile)
            throws IOException {
        return JsonLoader.fromFile(jsonFile);
    }

    public static JsonNode getJsonNode(URL url)
            throws IOException {
        return JsonLoader.fromURL(url);
    }

    public static JsonNode getJsonNodeFromResource(String resource)
            throws IOException {
        return JsonLoader.fromResource(resource);
    }

    public static JsonSchema getSchemaNode(String schemaText)
            throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schemaText);
        return _getSchemaNode(schemaNode);
    }

    public static JsonSchema getSchemaNode(File schemaFile)
            throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schemaFile);
        return _getSchemaNode(schemaNode);
    }

    public static JsonSchema getSchemaNode(URL schemaFile)
            throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schemaFile);
        return _getSchemaNode(schemaNode);
    }

    public static JsonSchema getSchemaNodeFromResource(String resource)
            throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNodeFromResource(resource);
        return _getSchemaNode(schemaNode);
    }

    public static void validateJson(JsonSchema jsonSchemaNode, JsonNode jsonNode)
            throws ProcessingException {
        ProcessingReport report = jsonSchemaNode.validate(jsonNode);
        if (!report.isSuccess()) {
            for (ProcessingMessage processingMessage : report) {
                throw new ProcessingException(processingMessage);
            }
        }
    }

    public static boolean isJsonValid(JsonSchema jsonSchemaNode, JsonNode jsonNode) throws ProcessingException {
        ProcessingReport report = jsonSchemaNode.validate(jsonNode);
        return report.isSuccess();
    }

    public static boolean isJsonValid(String schemaText, String jsonText) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaText);
        final JsonNode jsonNode = getJsonNode(jsonText);
        return isJsonValid(schemaNode, jsonNode);
    }

    public static boolean isJsonValid(File schemaFile, File jsonFile) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonFile);
        return isJsonValid(schemaNode, jsonNode);
    }

    public static boolean isJsonValid(URL schemaURL, URL jsonURL) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaURL);
        final JsonNode jsonNode = getJsonNode(jsonURL);
        return isJsonValid(schemaNode, jsonNode);
    }

    public static void validateJson(String schemaText, String jsonText) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaText);
        final JsonNode jsonNode = getJsonNode(jsonText);
        validateJson(schemaNode, jsonNode);
    }

    public static void validateJson(File schemaFile, File jsonFile) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonFile);
        validateJson(schemaNode, jsonNode);
    }

    public static void validateJson(URL schemaDocument, URL jsonDocument) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaDocument);
        final JsonNode jsonNode = getJsonNode(jsonDocument);
        validateJson(schemaNode, jsonNode);
    }

    public static void validateJsonResource(String schemaResource, String jsonResource) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaResource);
        final JsonNode jsonNode = getJsonNodeFromResource(jsonResource);
        validateJson(schemaNode, jsonNode);
    }

    private static JsonSchema _getSchemaNode(JsonNode jsonNode)
            throws ProcessingException {
        final JsonNode schemaIdentifier = jsonNode.get(JSON_SCHEMA_IDENTIFIER_ELEMENT);
        if (null == schemaIdentifier) {
            ((ObjectNode) jsonNode).put(JSON_SCHEMA_IDENTIFIER_ELEMENT, JSON_V4_SCHEMA_IDENTIFIER);
        }

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        return factory.getJsonSchema(jsonNode);
    }
}

import java.util.List;

public class TestHistoryResponse {

    private List<TestHistoryEvent> data;

    public TestHistoryResponse() {
    }

    public TestHistoryResponse(List<TestHistoryEvent> data) {
        this.data = data;
    }

    public List<TestHistoryEvent> getData() {
        return data;
    }

    public void setData(List<TestHistoryEvent> data) {
        this.data = data;
    }
}


import java.util.Map;


public class TestHistoryEvent {
    private String id;
    private String eventType;
    private Map<String, Object> eventData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by popikyardo on 15.10.15.
 */
public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByBoardIdAndStatusNot(String boardId, TaskStatus taskStatus);

    List<Task> findByStatus(TaskStatus taskStatus);
}


import io.eventuate.javaclient.spring.EnableEventHandlers;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableMongoRepositories
@EnableEventHandlers
public class TaskQuerySideConfiguration {


    @Bean
    public TaskQueryWorkflow taskQueryWorkflow(TaskUpdateService taskUpdateService) {
        return new TaskQueryWorkflow(taskUpdateService);
    }

    @Bean
    public TaskUpdateService taskUpdateService(TaskRepository taskRepository) {
        return new TaskUpdateService(taskRepository);
    }

    @Bean
    public HttpMessageConverters customConverters() {
        HttpMessageConverter<?> additional = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(additional);
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDbFactory factory, MongoMappingContext context, BeanFactory beanFactory) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
        try {
            mappingConverter.setCustomConversions(beanFactory.getBean(CustomConversions.class));
        }
        catch (NoSuchBeanDefinitionException ignore) {}

        // Don't save _class to mongo
        mappingConverter.setTypeMapper(new DefaultMongoTypeMapper(null));

        return mappingConverter;
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.BacklogResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Optional;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class TaskQueryController {

    @Autowired
    private TaskRepository taskRepository;

    @RequestMapping(value = "api/tasks", method = GET)
    public ResponseEntity<BacklogResponse> listAllTasks(@RequestParam(value = "boardId", required = false) String boardId) {
        BacklogResponse resp = new BacklogResponse();
        resp.setBacklog(Optional.of(taskRepository.
                findByStatus(TaskStatus.backlog))
                .orElse(new ArrayList<>()));
        if (boardId != null && !boardId.isEmpty()) {
            resp.setTasks(taskRepository.findByBoardIdAndStatusNot(boardId, TaskStatus.backlog));
        } else {
            resp.setTasks(new ArrayList<>());
        }
        return new ResponseEntity<>(resp, OK);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.Task;

import java.util.NoSuchElementException;

/**
 * Created by popikyardo on 15.10.15.
 */
public class TaskUpdateService {
    private TaskRepository taskRepository;

    public TaskUpdateService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task create(String id, TaskInfo taskInfo) {
        Task taskToCreate = Task.transform(id, taskInfo);
        return taskRepository.save(taskToCreate);
    }

    public Task delete(String id) {
        Task taskToDelete = taskRepository.findOne(id);
        taskRepository.delete(taskToDelete);
        return taskToDelete;
    }

    public Task update(String id, TaskInfo taskInfo) {
        Task taskToUpdate = taskRepository.findOne(id);

        if(taskToUpdate== null) {
            throw new NoSuchElementException(String.format("Task with id %s doesn't exist", id));
        }

        if(taskInfo.getTaskDetails()!=null) {
            taskToUpdate.setTitle(taskInfo.getTaskDetails().getTitle());
            taskToUpdate.setDescription(taskInfo.getTaskDetails().getDescription()!=null?
                    taskInfo.getTaskDetails().getDescription().getDescription():null);
        }
        if(taskInfo.getUpdate()!=null) {
            taskToUpdate.setUpdatedBy(taskInfo.getUpdate().getWho());
            taskToUpdate.setUpdatedDate(taskInfo.getUpdate().getWhen());
        }
        if(taskInfo.getBoardId()!=null) {
            taskToUpdate.setBoardId(taskInfo.getBoardId());
        }
        if(taskInfo.getStatus()!=null) {
            taskToUpdate.setStatus(taskInfo.getStatus());
        }
        return taskRepository.save(taskToUpdate);
    }
}


import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskInfo;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventSubscriber(id = "taskEventHandlers")
public class TaskQueryWorkflow {

    private TaskUpdateService taskUpdateService;

    public TaskQueryWorkflow(TaskUpdateService taskUpdateService) {
        this.taskUpdateService = taskUpdateService;
    }

    private static Logger log = LoggerFactory.getLogger(TaskQueryWorkflow.class);

    @EventHandlerMethod
    public void create(DispatchedEvent<TaskCreatedEvent> de) {
        String id = de.getEntityId();

        taskUpdateService.create(id, de.getEvent().getTaskInfo());
    }

    @EventHandlerMethod
    public void update(DispatchedEvent<TaskUpdatedEvent> de) {
        log.info("TaskQueryWorkflow got event : {}", de.getEvent());
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskDetails(de.getEvent().getTaskDetails());
        taskInfo.setUpdate(de.getEvent().getUpdate());

        taskUpdateService.update(de.getEntityId(), taskInfo);
    }

    @EventHandlerMethod
    public void complete(DispatchedEvent<TaskCompletedEvent> de) {
        processChangeStatusEvent(de, TaskStatus.completed);
    }

    @EventHandlerMethod
    public void delete(DispatchedEvent<TaskDeletedEvent> de) {
        log.info("TaskQueryWorkflow got event : {}", de.getEvent());
        taskUpdateService.delete(de.getEntityId());
    }

    @EventHandlerMethod
    public void schedule(DispatchedEvent<TaskScheduledEvent> de) {
        processChangeStatusEvent(de, TaskStatus.scheduled);
    }

    @EventHandlerMethod
    public void backlog(DispatchedEvent<TaskBacklogEvent> de) {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setUpdate(de.getEvent().getUpdate());
        taskInfo.setStatus(TaskStatus.backlog);

        updateAndSendEvent(de, taskInfo);
    }

    @EventHandlerMethod
    public void start(DispatchedEvent<TaskStartedEvent> de) {
        processChangeStatusEvent(de, TaskStatus.started);
    }

    private void processChangeStatusEvent(DispatchedEvent<? extends DetailedTaskEvent> de, TaskStatus taskStatus) {

        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setBoardId(de.getEvent().getBoardId());
        taskInfo.setUpdate(de.getEvent().getUpdate());
        taskInfo.setStatus(taskStatus);

        updateAndSendEvent((DispatchedEvent<? extends Event>) de, taskInfo);
    }

    private void updateAndSendEvent(DispatchedEvent<? extends Event> de, TaskInfo taskInfo) {
        log.info("TaskQueryWorkflow got event : {}", de.getEvent());
        taskUpdateService.update(de.getEntityId(), taskInfo);
    }
}

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;

public class AuditEntry {

    private String who;
    private Date when;

    public AuditEntry() {
    }

    public AuditEntry(String who, Date when) {
        this.who = who;
        this.when = when;
    }

    public String getWho() {
        return who;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
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


import org.apache.commons.lang.builder.ToStringBuilder;
import net.chrisrichardson.eventstore.examples.kanban.common.model.AuditEntry;

public class BoardInfo {
    private String title;
    private AuditEntry creation;
    private AuditEntry update;


    public BoardInfo() {
    }

    public BoardInfo(String title, AuditEntry creation, AuditEntry update) {
        this.title = title;
        this.creation = creation;
        this.update = update;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AuditEntry getCreation() {
        return creation;
    }

    public void setCreation(AuditEntry creation) {
        this.creation = creation;
    }

    public AuditEntry getUpdate() {
        return update;
    }

    public void setUpdate(AuditEntry update) {
        this.update = update;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}



import io.eventuate.Event;
import io.eventuate.EventEntity;

@EventEntity(entity = "net.chrisrichardson.eventstore.examples.kanban.commandside.board.BoardAggregate")
public class BoardEvent implements Event {
}


import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.chrisrichardson.eventstore.examples.kanban.common.board.BoardInfo;
import org.apache.commons.lang.builder.ToStringBuilder;

public class BoardCreatedEvent extends BoardEvent {
    @JsonUnwrapped
    private BoardInfo boardInfo;

    public BoardCreatedEvent() {
    }

    public BoardCreatedEvent(BoardInfo boardInfo) {
        this.boardInfo = boardInfo;
    }

    public BoardInfo getBoardInfo() {
        return boardInfo;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}


import java.util.List;

public class BoardsQueryResponse {

    private List<Board> boards;

    public BoardsQueryResponse() {}

    public BoardsQueryResponse(List<Board> boards) {
        this.boards = boards;
    }

    public List<Board> getBoards() {
        return boards;
    }

    public void setBoards(List<Board> boards) {
        this.boards = boards;
    }
}


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Board {
    @Id
    private String id;

    private String title;
    private String createdBy;
    private Date createdDate;
    private Date updatedDate;
    private String updatedBy;

    public Board() {
    }

    public Board(String id, String title, String createdBy) {
        this.id = id;
        this.title = title;
        this.createdBy = createdBy;
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    public Board(String id, String title, String createdBy, Date createdDate, Date updatedDate, String updatedBy) {
        this.id = id;
        this.title = title;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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


import java.util.Date;

public class BoardResponse {
    private String id;
    private String title;
    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;

    public BoardResponse() {
    }

    public BoardResponse(String id, String title, String createdBy, Date createdDate, String updatedBy, Date updatedDate) {
        this.id = id;
        this.title = title;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}


public class BoardQueryResponse {

    private Board data;

    public BoardQueryResponse() {}

    public BoardQueryResponse(Board data) {
        this.data = data;
    }

    public Board getData() {
        return data;
    }

    public void setData(Board data) {
        this.data = data;
    }
}


import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.anyMessage().permitAll();
    }

    /**
     * Disables CSRF for Websockets.
     */
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}


import io.eventuate.javaclient.spring.EnableEventHandlers;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableAutoConfiguration
@ComponentScan("net.chrisrichardson.eventstore.examples.kanban.commonwebsocket")
@EnableWebSocketMessageBroker
@EnableEventHandlers
public class WebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/events").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/events");
    }

}

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.DispatchedEvent;
import io.eventuate.Event;
import io.eventuate.EventHandlerMethod;
import io.eventuate.EventSubscriber;
import net.chrisrichardson.eventstore.examples.kanban.common.board.event.BoardCreatedEvent;
import net.chrisrichardson.eventstore.examples.kanban.common.board.model.Board;
import net.chrisrichardson.eventstore.examples.kanban.common.task.TaskStatus;
import net.chrisrichardson.eventstore.examples.kanban.common.task.event.*;
import net.chrisrichardson.eventstore.examples.kanban.common.task.model.Task;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.model.KanbanWebSocketEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@EventSubscriber(id="websocketEventHandlers")
public class WebsocketEventsTranslator {

    protected SimpMessagingTemplate template;

    private static String DESTINATION_DEFAULT_URL = "/events";


    private static ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static Logger log = LoggerFactory.getLogger(WebsocketEventsTranslator.class);

    public WebsocketEventsTranslator(SimpMessagingTemplate template) {
        this.template = template;
    }

    @EventHandlerMethod
    public void sendBoardEvents(DispatchedEvent<BoardCreatedEvent> de) throws JsonProcessingException {
        Board board = new Board();
        board.setId(de.getEntityId());
        board.setTitle(de.getEvent().getBoardInfo().getTitle());
        board.setCreatedBy(de.getEvent().getBoardInfo().getCreation().getWho());
        board.setCreatedDate(de.getEvent().getBoardInfo().getCreation().getWhen());
        board.setUpdatedBy(de.getEvent().getBoardInfo().getUpdate().getWho());
        board.setUpdatedDate(de.getEvent().getBoardInfo().getUpdate().getWhen());

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(board));
    }

    @EventHandlerMethod
    public void sendTaskCreatedEvent(DispatchedEvent<TaskCreatedEvent> de) throws JsonProcessingException {
        Task task = Task.transform(de.getEntityId(), de.getEvent().getTaskInfo());
        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }

    @EventHandlerMethod
    public void sendTaskMovedToBacklogEvent(DispatchedEvent<TaskBacklogEvent> de) throws JsonProcessingException {
        Task task = new Task();
        task.setId(de.getEntityId());
        task.setUpdatedBy(de.getEvent().getUpdate().getWho());
        task.setUpdatedDate(de.getEvent().getUpdate().getWhen());
        task.setBoardId("");
        task.setStatus(TaskStatus.backlog);

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }

    @EventHandlerMethod
    public void sendTaskCompletedEvent(DispatchedEvent<TaskCompletedEvent> de) throws JsonProcessingException {
        Task task = new Task();
        task.setId(de.getEntityId());
        task.setBoardId(de.getEvent().getBoardId());
        task.setUpdatedBy(de.getEvent().getUpdate().getWho());
        task.setUpdatedDate(de.getEvent().getUpdate().getWhen());
        task.setStatus(TaskStatus.completed);

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }

    @EventHandlerMethod
    public void sendTaskDeletedEvent(DispatchedEvent<TaskDeletedEvent> de) throws JsonProcessingException {
        Task task = new Task();
        task.setId(de.getEntityId());
        task.setUpdatedBy(de.getEvent().getUpdate().getWho());
        task.setUpdatedDate(de.getEvent().getUpdate().getWhen());
        task.setDeleted(true);

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }

    @EventHandlerMethod
    public void sendTaskScheduledEvent(DispatchedEvent<TaskScheduledEvent> de) throws JsonProcessingException {
        Task task = new Task();
        task.setId(de.getEntityId());
        task.setBoardId(de.getEvent().getBoardId());
        task.setUpdatedBy(de.getEvent().getUpdate().getWho());
        task.setUpdatedDate(de.getEvent().getUpdate().getWhen());
        task.setStatus(TaskStatus.scheduled);

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }

    @EventHandlerMethod
    public void sendTaskStartedEvent(DispatchedEvent<TaskStartedEvent> de) throws JsonProcessingException {
        Task task = new Task();
        task.setId(de.getEntityId());
        task.setBoardId(de.getEvent().getBoardId());
        task.setUpdatedBy(de.getEvent().getUpdate().getWho());
        task.setUpdatedDate(de.getEvent().getUpdate().getWhen());
        task.setStatus(TaskStatus.started);

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }

    @EventHandlerMethod
    public void sendTaskUpdatedEvent(DispatchedEvent<TaskUpdatedEvent> de) throws JsonProcessingException {
        Task task = new Task();
        task.setId(de.getEntityId());
        task.setTitle(de.getEvent().getTaskDetails().getTitle());
        task.setDescription(de.getEvent().getTaskDetails().getDescription() != null ?
                de.getEvent().getTaskDetails().getDescription().getDescription() : null);
        task.setUpdatedBy(de.getEvent().getUpdate().getWho());
        task.setUpdatedDate(de.getEvent().getUpdate().getWhen());

        this.sendEvent(de, DESTINATION_DEFAULT_URL, objectMapper.writeValueAsString(task));
    }


    private void sendEvent(DispatchedEvent<? extends Event> de, String destination, String eventData) throws JsonProcessingException {
        log.info("Sending board event to websocket : {}", de.getEvent());
        KanbanWebSocketEvent event = new KanbanWebSocketEvent();
        event.setEntityId(de.getEntityId());
        event.setEventData(eventData);
        event.setEventId(de.getEventId().asString());
        event.setEventType(de.getEventType().getName());
        template.convertAndSend(destination, objectMapper.writeValueAsString(event));
    }
}


import org.apache.commons.lang.builder.ToStringBuilder;

public class KanbanWebSocketEvent {

    private String eventId;
    private String eventType;
    private String eventData;
    private String entityId;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class RestTemplateErrorHandler implements ResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateErrorHandler.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        log.error("Response error: {} {}", response.getStatusCode(), response.getStatusText());
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return RestUtil.isError(response.getStatusCode());
    }
}

import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebSocketConfig;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebSocketSecurityConfig;
import net.chrisrichardson.eventstore.examples.kanban.commonwebsocket.WebsocketEventsTranslator;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
@ComponentScan
@Import({EventuateDriverConfiguration.class, WebConfiguration.class, AuthConfiguration.class, WebSocketConfig.class, WebSocketSecurityConfig.class})
@EnableConfigurationProperties({ApiGatewayProperties.class})
public class ApiGatewayServiceConfiguration {

  @Bean
  public WebsocketEventsTranslator websocketEventsTranslator(SimpMessagingTemplate template) {
    return new WebsocketEventsTranslator(template);
  }

  @Bean
  public RestTemplate restTemplate(HttpMessageConverters converters) {

    // we have to define Apache HTTP client to use the PATCH verb
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/json"));
    converter.setObjectMapper(new ObjectMapper());

    HttpClient httpClient = HttpClients.createDefault();
    RestTemplate restTemplate = new RestTemplate(Collections.<HttpMessageConverter<?>>singletonList(converter));
    restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

    restTemplate.setErrorHandler(new RestTemplateErrorHandler());

    return restTemplate;
  }
}


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@ConfigurationProperties(prefix = "api.gateway")
public class ApiGatewayProperties {

    private List<Endpoint> endpoints;

    public static class Endpoint {
        private String path;
        private RequestMethod method;
        private String location;

        public Endpoint() {
        }

        public Endpoint(String location) {
            this.location = location;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public RequestMethod getMethod() {
            return method;
        }

        public void setMethod(RequestMethod method) {
            this.method = method;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
}


import org.springframework.http.HttpStatus;

public class RestUtil {

    public static boolean isError(HttpStatus status) {
        HttpStatus.Series series = status.series();
        return (HttpStatus.Series.CLIENT_ERROR.equals(series)
                || HttpStatus.Series.SERVER_ERROR.equals(series));
    }
}

import org.apache.http.client.methods.RequestBuilder;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;

public class HeadersRequestTransformer extends ProxyRequestTransformer {


    @Override
    public RequestBuilder transform(HttpServletRequest request) throws NoSuchRequestHandlingMethodException, URISyntaxException, IOException {
        RequestBuilder requestBuilder = predecessor.transform(request);

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if(headerName.equals("x-access-token")) {
                requestBuilder.addHeader(headerName, headerValue);
            }
        }

        return requestBuilder;
    }
}


import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

public class ContentRequestTransformer extends ProxyRequestTransformer {

    @Override
    public RequestBuilder transform(HttpServletRequest request) throws NoSuchRequestHandlingMethodException, URISyntaxException, IOException {
        RequestBuilder requestBuilder = predecessor.transform(request);

        String requestContent = request.getReader().lines().collect(Collectors.joining(""));
        if(!requestContent.isEmpty()) {
            StringEntity entity = new StringEntity(requestContent, ContentType.APPLICATION_JSON);
            requestBuilder.setEntity(entity);
        }

        return requestBuilder;
    }
}


import org.apache.http.client.methods.RequestBuilder;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;

public abstract class ProxyRequestTransformer {

    protected ProxyRequestTransformer predecessor;

    public abstract RequestBuilder transform(HttpServletRequest request) throws NoSuchRequestHandlingMethodException, URISyntaxException, IOException;

    public void setPredecessor(ProxyRequestTransformer transformer) {
        this.predecessor = transformer;
    };
}


import net.chrisrichardson.eventstore.examples.kanban.apigateway.ApiGatewayProperties;
import org.apache.http.client.methods.RequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

public class URLRequestTransformer extends ProxyRequestTransformer {

    private ApiGatewayProperties apiGatewayProperties;

    public URLRequestTransformer(ApiGatewayProperties apiGatewayProperties) {
        this.apiGatewayProperties = apiGatewayProperties;
    }

    @Override
    public RequestBuilder transform(HttpServletRequest request) throws NoSuchRequestHandlingMethodException, URISyntaxException {
        String requestURI = request.getRequestURI();
        URI uri;
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            uri = new URI(getServiceUrl(requestURI, request) + "?" + request.getQueryString());
        } else {
            uri = new URI(getServiceUrl(requestURI, request));
        }

        RequestBuilder rb = RequestBuilder.create(request.getMethod());
        rb.setUri(uri);
        return rb;
    }

    private String getServiceUrl(String requestURI, HttpServletRequest httpServletRequest) throws NoSuchRequestHandlingMethodException {

        ApiGatewayProperties.Endpoint endpoint =
                apiGatewayProperties.getEndpoints().stream()
                        .filter(e ->
                                        requestURI.matches(e.getPath()) && e.getMethod() == RequestMethod.valueOf(httpServletRequest.getMethod())
                        )
                        .findFirst().orElseThrow(() -> new NoSuchRequestHandlingMethodException(httpServletRequest));
        return endpoint.getLocation() + requestURI;
    }
}


import net.chrisrichardson.eventstore.examples.kanban.apigateway.ApiGatewayServiceConfiguration;
import org.springframework.boot.SpringApplication;

public class ApiGatewayServiceMain {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayServiceConfiguration.class, args);
    }
}


import net.chrisrichardson.eventstore.examples.kanban.apigateway.ApiGatewayProperties;
import net.chrisrichardson.eventstore.examples.kanban.apigateway.utils.ContentRequestTransformer;
import net.chrisrichardson.eventstore.examples.kanban.apigateway.utils.HeadersRequestTransformer;
import net.chrisrichardson.eventstore.examples.kanban.apigateway.utils.URLRequestTransformer;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/api")
public class GatewayController {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApiGatewayProperties apiGatewayProperties;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    @RequestMapping(value = "/**", method = {GET, POST, PUT, DELETE})
    public String proxyRequest(HttpServletRequest request) throws NoSuchRequestHandlingMethodException, IOException, URISyntaxException {
        HttpUriRequest proxiedRequest = createHttpUriRequest(request);
        log.info("request: {}", proxiedRequest);
        HttpResponse proxiedResponse = httpClient.execute(proxiedRequest);
        return read(proxiedResponse.getEntity().getContent());
    }

    private HttpUriRequest createHttpUriRequest(HttpServletRequest request) throws URISyntaxException, NoSuchRequestHandlingMethodException, IOException {
        URLRequestTransformer urlRequestTransformer = new URLRequestTransformer(apiGatewayProperties);
        ContentRequestTransformer contentRequestTransformer = new ContentRequestTransformer();
        HeadersRequestTransformer headersRequestTransformer = new HeadersRequestTransformer();
        headersRequestTransformer.setPredecessor(contentRequestTransformer);
        contentRequestTransformer.setPredecessor(urlRequestTransformer);

        return headersRequestTransformer.transform(request).build();
    }

    private String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}


import io.eventuate.javaclient.driver.EventuateDriverConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonauth.AuthConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonswagger.CommonSwaggerConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.commonweb.WebConfiguration;
import net.chrisrichardson.eventstore.examples.kanban.queryside.task.TaskQuerySideConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by Main on 19.01.2016.
 */
@Configuration
@Import({TaskQuerySideConfiguration.class, EventuateDriverConfiguration.class, WebConfiguration.class, AuthConfiguration.class, CommonSwaggerConfiguration.class})
@EnableAutoConfiguration
@ComponentScan
public class TaskQuerySideServiceConfiguration {
}


import net.chrisrichardson.eventstore.examples.kanban.taskquerysideservice.TaskQuerySideServiceConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * Created by Main on 19.01.2016.
 */
public class TaskQuerySideServiceMain {
    public static void main(String[] args) {
        SpringApplication.run(TaskQuerySideServiceConfiguration.class, args);
    }
}

