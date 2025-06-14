
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
public class ForumApplication{

    public static void main(String[] args){
        ConfigurableApplicationContext application = SpringApplication.run(ForumApplication.class, args);

        Environment env = application.getEnvironment();
        String port = env.getProperty("server.port");
        port = port == null ? "8080" : port;
        String path = env.getProperty("server.servlet.context-path");
        path = path == null ? "" : path;
        log.info("\n----------------------------------------------------------\n\t" +
                "ThinkMoreForum Backend is running! Access URLs:\n\t" +
                "Local:   \thttp://localhost:" + port + path + "/\n\t" +
                "Swagger: \thttp://localhost:" + port + path + "/swagger-ui.html\n\t" +
//                "H2:      \thttp://localhost:" + port + path + "/h2-console/\n" +
                "----------------------------------------------------------");
    }
    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer(@Value("${spring.application.name}") String applicationName) {
        return (registry) -> registry.config().commonTags("application", applicationName);
    }
}


import com.thinkmore.forum.configuration.SecurityConfig;
import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.entity.JwtUser;
import com.thinkmore.forum.service.JwtRouterService;
import com.thinkmore.forum.util.Util;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class JwtCheckFilter extends OncePerRequestFilter {
    private final JwtRouterService jwtRouterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (stringIsNullOrEmpty(authorizationHeader) || !authorizationHeader.startsWith(StaticConfig.JwtPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        String fakeJwt = authorizationHeader.replace(StaticConfig.JwtPrefix, "");
        String realJwt = jwtRouterService.getRealJwt(fakeJwt);
        if (stringIsNullOrEmpty(realJwt)) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            log.info("Cannot find fakeJwt: {}", fakeJwt);
            return;
        }

        try {
            //check jwt
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                                        .setSigningKey(SecurityConfig.secretKey)
                                        .build()
                                        .parseClaimsJws(realJwt);
            Claims body = claimsJws.getBody();

            ArrayList<String> principal = new ArrayList<>();
            principal.add(body.getId());
            principal.add(body.getSubject());
            principal.add(body.getAudience());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    null
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            //update jwt
            JwtUser jwtUser = new JwtUser(principal);
            String newJwt = Util.generateJwt(jwtUser);
            response.addHeader(HttpHeaders.AUTHORIZATION, StaticConfig.JwtPrefix + jwtRouterService.getFakeJwt(newJwt));
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            log.info(String.valueOf(e));
            return;
        }

        filterChain.doFilter(request, response);
    }

    static boolean stringIsNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.thinkmore.forum.dto.users.UsersGetDto;
import com.thinkmore.forum.entity.JwtUser;
import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.service.JwtRouterService;
import com.thinkmore.forum.service.UsersService;
import com.thinkmore.forum.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JwtGenerateFilter extends UsernamePasswordAuthenticationFilter {
    private final UsersService usersService;
    private final JwtRouterService jwtRouterService;
    private final AuthenticationManager authenticationManager;

    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        //check username and password
        String dataString = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        JsonObject dataObject = JsonParser.parseString(dataString).getAsJsonObject();

        String email = dataObject.get("email").toString().replace("\"", "");
        String password = dataObject.get("password").toString().replace("\"", "");

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, password);
        return authenticationManager.authenticate(authentication);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {

        //update last login timestamp
        UsersGetDto usersGetDto = usersService.updateLastLoginTimestamp(authResult.getName());

        //generate jwt
        JwtUser jwtUser = (JwtUser) authResult.getPrincipal();
        String newJwt = Util.generateJwt(jwtUser);
        response.addHeader(HttpHeaders.AUTHORIZATION, StaticConfig.JwtPrefix + jwtRouterService.getFakeJwt(newJwt));

        //return user info
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response.getWriter().write(objectMapper.writeValueAsString(usersGetDto));
    }
}


import com.thinkmore.forum.dto.component.ComponentGetDto;
import com.thinkmore.forum.dto.component.ComponentPostDto;
import com.thinkmore.forum.dto.component.ComponentPutDto;
import com.thinkmore.forum.entity.Component;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ComponentMapper {

    Component toEntity(ComponentPutDto componentPutDto);

    Component toEntity(ComponentPostDto ComponentPostDto);

    ComponentGetDto fromEntity(Component component);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void copy(ComponentPutDto componentPutDto, @MappingTarget Component component);
}


import com.thinkmore.forum.dto.img.ImgGetDto;
import com.thinkmore.forum.entity.Img;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ImgMapper {
    ImgGetDto fromEntity(Img img);
}


import com.thinkmore.forum.dto.roles.RolesGetDto;
import com.thinkmore.forum.dto.roles.RolesPostDto;
import com.thinkmore.forum.dto.roles.RolesPutDto;
import com.thinkmore.forum.entity.Roles;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface RolesMapper {
    Roles toEntity(RolesPutDto rolesPutDto);

    RolesGetDto fromEntity(Roles roles);

}


import com.thinkmore.forum.dto.notification.NotificationGetDto;
import com.thinkmore.forum.entity.Notification;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface NotificationMapper {

    NotificationGetDto fromEntity(Notification notification);
}


import com.thinkmore.forum.dto.followPost.FollowPostGetDto;
import com.thinkmore.forum.dto.followPost.FollowPostPostDto;
import com.thinkmore.forum.dto.followPost.FollowPostPutDto;
import com.thinkmore.forum.entity.FollowPost;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring", uses = {PostMapper.class})
public interface FollowPostMapper {
    FollowPost toEntity(FollowPostPostDto followPostPostDto);

    FollowPostGetDto fromEntity(FollowPost followPost);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void copy(FollowPostPutDto followPostPutDto, @MappingTarget FollowPost followPost);
}


import com.thinkmore.forum.dto.category.*;
import com.thinkmore.forum.entity.Category;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CategoryMapper {
    Category toEntity(CategoryPutDto categoryPutDto);

    CategoryGetDto fromEntity(Category category);

    CategoryMiniGetDto entityToMiniDto(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void copy(CategoryPutDto categoryPutDto, @MappingTarget Category category);
}


import com.thinkmore.forum.dto.post.PostGetDto;
import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.post.PostPostDto;
import com.thinkmore.forum.dto.post.PostPutDto;
import com.thinkmore.forum.entity.Post;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring", uses = {CategoryMapper.class})
public interface PostMapper {
    Post toEntity(PostPostDto postPostDto);

    PostGetDto fromEntity(Post post);

    PostMiniGetDto entityToMiniDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void copy(PostPutDto postPutDto, @MappingTarget Post post);
}


import com.thinkmore.forum.dto.comment.CommentGetDto;
import com.thinkmore.forum.dto.comment.CommentPostDto;
import com.thinkmore.forum.entity.Comment;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CommentMapper {
    Comment toEntity(CommentPostDto commentPostDto);

    CommentGetDto fromEntity(Comment comment);
}


import com.thinkmore.forum.dto.followerUsers.FollowerUsersGetDto;
import com.thinkmore.forum.dto.followerUsers.FollowerUsersPostDto;
import com.thinkmore.forum.entity.FollowUser;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FollowerUsersMapper {
    FollowerUsersGetDto fromEntity(FollowUser followUser);

    FollowUser toEntity(FollowerUsersPostDto followerUsersPostDto);
}


import com.thinkmore.forum.dto.users.UsersGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import com.thinkmore.forum.dto.users.UsersPostDto;
import com.thinkmore.forum.dto.users.UsersPutDto;
import com.thinkmore.forum.entity.Users;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UsersMapper {
    Users toEntity(UsersPostDto usersPostDto);

    UsersGetDto fromEntity(Users users);

    UsersMiniGetDto entityToMiniDto(Users users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void copy(UsersPutDto usersPutDto, @MappingTarget Users users);
}


import com.thinkmore.forum.dto.notification.NotificationGetDto;
import com.thinkmore.forum.service.NotificationService;
import com.thinkmore.forum.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationGetDto>> getNotifications() {
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId));
    }

    @GetMapping(path = "/viewed/{notificationId}")
    public ResponseEntity<Boolean> markAsViewed(@PathVariable String notificationId) {
        UUID notificationIdUuid = UUID.fromString(notificationId);
        return ResponseEntity.ok(notificationService.markAsViewed(notificationIdUuid));
    }

    @GetMapping(path = "/viewed_all")
    public ResponseEntity<Boolean> viewNotification() {
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(notificationService.markAllAsViewed(userId));
    }
}

import com.thinkmore.forum.service.WebsocketService;
import com.thinkmore.forum.entity.websocket.OnlineMessage;
import com.thinkmore.forum.entity.websocket.ReminderMessage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebsocketController {

    private final WebsocketService websocketService;

    @MessageMapping("/hello")
    @SendTo("/hall/greetings")
    public List<String> signOnline(@Header("simpSessionId") String sessionId, OnlineMessage onlineMsg) {
        return websocketService.signOnline(sessionId, onlineMsg);
    }

    @MessageMapping("/reminder")
    public ReminderMessage forwardReminder(ReminderMessage reminder) {
        return websocketService.forwardReminder(reminder);
    }

    @EventListener
    public void onDisconnectEvent(SessionDisconnectEvent event) {
        websocketService.signOffline(event);
    }
}

import com.thinkmore.forum.dto.component.ComponentGetDto;
import com.thinkmore.forum.dto.component.ComponentPostDto;
import com.thinkmore.forum.dto.component.ComponentPutDto;
import com.thinkmore.forum.service.ComponentService;
import com.thinkmore.forum.util.Util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/v1/component")
@RequiredArgsConstructor
public class ComponentController {
    private final ComponentService componentService;

    @PostMapping
    public ResponseEntity<ComponentGetDto> postComponent(@RequestBody ComponentPostDto componentPostDto) {
        Util.checkPermission("adminManagement");
        return ResponseEntity.ok(componentService.postComponent(componentPostDto));
    }

    @PutMapping
    public ResponseEntity<ComponentGetDto> putComponent(@RequestBody ComponentPutDto componentPutDto) {
        Util.checkPermission("adminManagement");
        return ResponseEntity.ok(componentService.putComponent(componentPutDto));
    }

    @DeleteMapping(path = "/{name}")
    public ResponseEntity<Boolean> deleteComponent(@PathVariable String name) {
        Util.checkPermission("adminManagement");
        return ResponseEntity.ok(componentService.deleteComponent(name));
    }
}


import com.thinkmore.forum.dto.followerUsers.FollowerUsersGetDto;
import com.thinkmore.forum.service.FollowerUsersService;
import com.thinkmore.forum.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/users")
@RequiredArgsConstructor
public class FollowerUsersController {
    private final FollowerUsersService followerUsersService;

    @GetMapping(path = "/followed_status/{username}")
    public ResponseEntity<Boolean> checkFollowedStatus(@PathVariable("username") String username) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(followerUsersService.followStatus(username, usersId));
    }

    @PostMapping(path = "/follow/{username}")
    public ResponseEntity<FollowerUsersGetDto> follow_user(@PathVariable("username") String username) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(followerUsersService.followUsers(usersId, username));
    }

    @DeleteMapping(path = "/unfollow/{username}")
    public ResponseEntity<?> unfollow_user(@PathVariable("username") String target_username) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        try {
            followerUsersService.unfollowUsers(usersId, target_username);
            return ResponseEntity.ok().body("Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Not found");
        }
    }
}


import com.thinkmore.forum.dto.comment.CommentGetDto;
import com.thinkmore.forum.dto.category.CategoryGetDto;
import com.thinkmore.forum.dto.component.ComponentGetDto;
import com.thinkmore.forum.dto.followPost.FollowPostGetDto;
import com.thinkmore.forum.dto.followerUsers.FollowerUsersGetDto;
import com.thinkmore.forum.dto.post.PostCommentGetDto;
import com.thinkmore.forum.dto.post.PostGetDto;
import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.users.UsersGetDto;
import com.thinkmore.forum.dto.users.UsersPostDto;
import com.thinkmore.forum.service.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.prometheus.client.Histogram;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final UsersService usersService;
    private final MessageService messageService;
    private final CategoryService categoryService;
    private final PostService postService;
    private final CommentService commentService;
    private final FollowPostService followPostService;
    private final FollowerUsersService followerUsersService;
    private final ComponentService componentService;
    private static final Counter newUserCounter = Metrics.
            counter("newUser.counter.total", "controller", "public");
    private static final Histogram postRequestLatency = Histogram.build()
            .name("post_requests_latency_seconds").help("Post request latency in seconds.").register();

    // Users
    @PostMapping(path = "/users/register")
    public ResponseEntity<Boolean> register(@RequestBody UsersPostDto usersPostDto) {
        newUserCounter.increment(1D);
        return ResponseEntity.ok(usersService.register(usersPostDto));
    }

    @GetMapping(path = "/users/unique_email/{email}")
    public ResponseEntity<Boolean> uniqueEmail(@PathVariable String email) {
        return ResponseEntity.ok(usersService.uniqueEmail(email));
    }

    @GetMapping(path = "/users/unique_username/{username}")
    public ResponseEntity<Boolean> uniqueUsername(@PathVariable String username) {
        return ResponseEntity.ok(usersService.uniqueUsername(username));
    }

    @GetMapping("/users/reset_password/{email}")
    public ResponseEntity<Boolean> sendResetPasswordEmail(@PathVariable String email) {
        return ResponseEntity.ok(messageService.sendResetPasswordEmail(email));
    }

    @GetMapping(path = "/users/username/{username}")
    public ResponseEntity<UsersGetDto> getUserById(@PathVariable String username) {
        return ResponseEntity.ok(usersService.getUserByUsername(username));
    }

    // Category
    @GetMapping(path = "/category")
    public ResponseEntity<List<CategoryGetDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping(path = "/category/{category_title}")
    public ResponseEntity<CategoryGetDto> getCategoryByTitle(@PathVariable("category_title") String category_title) throws Exception {
        return ResponseEntity.ok(categoryService.getCategoryByCategoryTitle(category_title));
    }

    @GetMapping(path = "/category/{category_id}/visible_post")
    public ResponseEntity<List<PostGetDto>> findVisiblePostsByCategoryId(@PathVariable("category_id") String category_id, @PageableDefault(page = 0, size = 10, sort = {"createTimestamp"}, direction = Sort.Direction.DESC) Pageable pageable) throws Exception {
        UUID categoryId = UUID.fromString(category_id);
        return ResponseEntity.ok(postService.getVisiblePostsByCategoryId(categoryId, pageable));
    }

    @GetMapping(path = "/category/{category_id}/visible_count")
    public ResponseEntity<Long> findNumOfVisiblePostsInCategory(@PathVariable("category_id") String category_id) {
        UUID categoryId = UUID.fromString(category_id);
        return ResponseEntity.ok(postService.getCountOfVisiblePostsByCategoryId(categoryId));
    }

    // Post
    @GetMapping(path = "/post")
    public ResponseEntity<List<PostMiniGetDto>> findAllPosts(){
        Histogram.Timer postRequestTimer = postRequestLatency.startTimer();
        List <PostMiniGetDto> result = postService.getAllPostsCoreInfo();
        postRequestTimer.observeDuration();
        return ResponseEntity.ok(result);
    }

    @GetMapping(path = "/post/{post_id}")
    public ResponseEntity<PostGetDto> getPostById(@PathVariable String post_id) throws Exception {
        UUID postId = UUID.fromString(post_id);
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @GetMapping(path = "/post/user/{username}")
    public ResponseEntity<List<PostGetDto>> findPostByPostUsersId(@PathVariable String username) {
        return ResponseEntity.ok(postService.getPostsByPostUsersName(username));
    }

    @PutMapping(path = "/post/{post_id}/view_count")
    public void updatePostViewCount(@PathVariable("post_id") String post_id) {
        UUID postId = UUID.fromString(post_id);
        postService.updateViewCount(postId);
    }

    @GetMapping(path = "/post/follows/find_all_by_username/{username}")
    public ResponseEntity<List<FollowPostGetDto>> getFollowPostByUsername(@PathVariable String username) {
        return ResponseEntity.ok(followPostService.getAllFollowPostsByUsername(username));
    }

    @GetMapping(path="/post/max_count_comment")
    public ResponseEntity<PostCommentGetDto> getMaxCountCommentPost(@PageableDefault(page = 0, size = 3) Pageable pageable) {
        return ResponseEntity.ok(postService.getMaxCountCommentPost(pageable));
    }

    // Comment
    @GetMapping(path = "/comment/{post_id}")
    public ResponseEntity<List<CommentGetDto>> getCommentsByPostId(@PathVariable String post_id) {
        UUID postId = UUID.fromString(post_id);
        return ResponseEntity.ok(commentService.getAllByPost(postId));
    }

    // Followers, Following
    @GetMapping(path = "/follower/{username}")
    public ResponseEntity<List<FollowerUsersGetDto>> getFollower(@PathVariable("username") String target_username) {
        return ResponseEntity.ok(followerUsersService.getFollowersByUsername(target_username));
    }

    @GetMapping(path = "/following/{username}")
    public ResponseEntity<List<FollowerUsersGetDto>> getFollowing(@PathVariable("username") String target_username) {
        return ResponseEntity.ok(followerUsersService.getFollowingByUsername(target_username));
    }

    // Component
    @GetMapping(path = "/component/{name}")
    public ResponseEntity<ComponentGetDto> getComponentByName(@PathVariable String name) throws Exception {
        return ResponseEntity.ok(componentService.getComponent(name));
    }
}


import com.thinkmore.forum.dto.roles.RolesGetDto;
import com.thinkmore.forum.dto.roles.RolesPutDto;
import com.thinkmore.forum.service.RoleService;
import com.thinkmore.forum.util.Util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/v1/role")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping(path = "/all")
    public ResponseEntity<List<RolesGetDto>> getAllRoles() {
        Util.checkPermission("adminManagement");
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PutMapping
    public ResponseEntity<Boolean> putRole(@RequestBody List<RolesPutDto> rolesPutDtoList) {
        Util.checkPermission("adminManagement");
        return ResponseEntity.ok(roleService.putRole(rolesPutDtoList));
    }
}


import com.thinkmore.forum.dto.users.UsersGetDto;
import com.thinkmore.forum.dto.users.UsersImgPutDto;
import com.thinkmore.forum.dto.users.UsersMiniPutDto;
import com.thinkmore.forum.dto.users.UsersPasswordPutDto;
import com.thinkmore.forum.service.MessageService;
import com.thinkmore.forum.service.UsersService;
import com.thinkmore.forum.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {
    private final UsersService usersService;
    private final MessageService messageService;

    @GetMapping(path="/all")
    public  ResponseEntity<List<UsersGetDto>> getAllUsers(){
        return ResponseEntity.ok(usersService.getAllUsers());
    }

    @GetMapping(path = "/search/{string}")
    public ResponseEntity<List<UsersGetDto>> getUserByContainingString(@PathVariable("string") String string) {
        List<UsersGetDto> response = usersService.getUserByContainingString(string);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password_reset")
    public ResponseEntity<Boolean> passwordReset(@RequestBody UsersPasswordPutDto new_password) {
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(usersService.resetPassword(userId, new_password));
    }

    @PutMapping("/password")
    public ResponseEntity<Boolean> changePassword(@RequestBody UsersMiniPutDto usersMiniPutDto) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(usersService.changePassword(usersId, usersMiniPutDto));
    }

    @PutMapping("/username/{new_username}")
    public ResponseEntity<Boolean> changeUsername(@PathVariable String new_username) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(usersService.changeUsername(usersId, new_username));
    }

    @PutMapping("/headimg")
    public ResponseEntity<Boolean> changeHeadImg(@RequestBody UsersImgPutDto usersImgPutDto) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(usersService.changeHeadImgUrl(usersId, usersImgPutDto));
    }

    @PutMapping("/profile_img")
    public ResponseEntity<Boolean> changeProfileImg(@RequestBody UsersImgPutDto usersImgPutDto) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(usersService.changeProfileImgUrl(usersId, usersImgPutDto));
    }

    @GetMapping("/email/{new_email}")
    public ResponseEntity<Boolean> sendVerificationEmail(@PathVariable String new_email) throws Exception {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(messageService.sendVerificationEmail(usersId, new_email));
    }

    @PutMapping("/email/{new_email}")
    public ResponseEntity<Boolean> changeEmail(@PathVariable String new_email) {
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(usersService.changeEmail(usersId, new_email));
    }

    @PutMapping(path="/roles")
    public ResponseEntity<String> changeUsersRoles(@RequestBody List<UsersGetDto> usersGetDtoList) {
        Util.checkPermission("adminManagement");
        usersService.changeUsersRoles(usersGetDtoList);
        return ResponseEntity.ok("Done");
    }
}


import com.thinkmore.forum.dto.post.PostGetDto;
import com.thinkmore.forum.dto.post.PostMiniPutDto;
import com.thinkmore.forum.dto.post.PostPostDto;
import com.thinkmore.forum.service.PostService;
import com.thinkmore.forum.util.Util;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/post")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    static final Counter postCounter = Metrics.
            counter("post.counter.total", "controller", "post");

    @GetMapping(path = "/search/{string}")
    public ResponseEntity<List<PostGetDto>> getPostByTitleContainingString(@PathVariable("string") String string) {
        List<PostGetDto> response = postService.getPostByTitleContainingString(string);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<String> postPost(@RequestBody PostPostDto postPostDto) {
        Util.checkPermission("makePost");
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        String response = postService.postPost(userId, postPostDto);
        postCounter.increment(1D);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{post_id}")
    public ResponseEntity<Boolean> editPost(@PathVariable("post_id") String post_id, @RequestBody PostMiniPutDto postMiniPutDto) {
        Util.checkPermission("postManagement");
        UUID postId = UUID.fromString(post_id);
        UUID usersId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(postService.editPost(postId, usersId, postMiniPutDto));
    }

    @PutMapping(path = "/{post_id}/visibility")
    public ResponseEntity<Boolean> changePostVisibility(@PathVariable("post_id") String post_id) {
        Util.checkPermission("postManagement");
        UUID postId = UUID.fromString(post_id);
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        Boolean response = postService.changePostVisibility(postId, userId);
        return ResponseEntity.ok(response);
    }
}


import com.thinkmore.forum.dto.category.CategoryGetDto;
import com.thinkmore.forum.dto.category.CategoryPutDto;
import com.thinkmore.forum.service.CategoryService;
import com.thinkmore.forum.util.Util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PutMapping
    public ResponseEntity<Boolean> putCategories(@RequestBody List<CategoryPutDto> categoryPutDtoList) {
        Util.checkPermission("adminManagement");
        return ResponseEntity.ok(categoryService.putCategory(categoryPutDtoList));
    }

    @PutMapping(path = "{category_id}/pin/{post_id}")
    public ResponseEntity<CategoryGetDto> putCategoryPinPostById(@PathVariable String category_id, @PathVariable String post_id) {
        Util.checkPermission("postManagement");
        UUID categoryId = UUID.fromString(category_id);
        UUID postId = UUID.fromString(post_id);
        return ResponseEntity.ok(categoryService.putCategoryPinPostById(categoryId, postId));
    }

    @PutMapping(path = "{category_id}/unpin")
    public ResponseEntity<CategoryGetDto> putCategoryPinPostNull(@PathVariable String category_id) {
        Util.checkPermission("postManagement");
        UUID categoryId = UUID.fromString(category_id);
        return ResponseEntity.ok(categoryService.putCategoryPinPostNull(categoryId));
    }
}


import com.thinkmore.forum.dto.comment.CommentPostDto;
import com.thinkmore.forum.service.CommentService;
import com.thinkmore.forum.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<String> postComment(@RequestBody CommentPostDto commentDto) {
        Util.checkPermission("postComment");
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(commentService.postComment(userId, commentDto));
    }
}


import com.thinkmore.forum.entity.Img;
import com.thinkmore.forum.service.ImgService;
import com.thinkmore.forum.util.Util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/v1/img")
@RequiredArgsConstructor
public class ImgController {

    private final ImgService imgService;

    @PostMapping(path = "/upload")
    public ResponseEntity<Img> upload(@RequestParam MultipartFile img) throws Exception {
        Util.checkPermission("uploadImg");
        return ResponseEntity.ok(imgService.upload(img));
    }
}


import com.thinkmore.forum.dto.followPost.FollowPostGetDto;
import com.thinkmore.forum.service.FollowPostService;
import com.thinkmore.forum.util.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/v1/post/follows")
@RequiredArgsConstructor
public class FollowPostController {
    private final FollowPostService followPostService;

    @GetMapping(path = "/find_all_by_user_id")
    public ResponseEntity<List<FollowPostGetDto>> findAllByUserId() {
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        List<FollowPostGetDto> followPostList = followPostService.getAllFollowPostsByUserId(userId);
        return ResponseEntity.ok(followPostList);
    }

    @GetMapping(path="/check_user_following_state/{post_id}")
    public ResponseEntity<Boolean> checkUserFollowingState(@PathVariable String post_id) {
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        UUID postId = UUID.fromString(post_id);
        Boolean userIsFollowingPost = followPostService.checkUserFollowingState(postId, userId);
        return ResponseEntity.ok(userIsFollowingPost);
    }

    @PostMapping(path = "/user_follow_post/{post_id}")
    public ResponseEntity<String> userFollowPost(@PathVariable String post_id) {
        UUID postId = UUID.fromString(post_id);
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        followPostService.postFollowPostToUser(postId, userId);
        return ResponseEntity.ok(String.format("successfully followed post with id %s", post_id));
    }

    @DeleteMapping(path = "/user_unfollow_post/{post_id}")
    public ResponseEntity<String> userUnfollowPost(@PathVariable String post_id) {
        UUID postId = UUID.fromString(post_id);
        UUID userId = UUID.fromString(Util.getJwtContext().get(0));
        return ResponseEntity.ok(followPostService.userUnfollowPost(postId, userId));
    }
}


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private Integer redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setPort(redisPort);
        redisStandaloneConfiguration.setPassword(redisPassword);
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}


import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Value("${swagger.enable}")
    private boolean swaggerEnable;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .enable(swaggerEnable)
                .apiInfo(apiInfo())
                .globalOperationParameters(jwt())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("ThinkMoreForum Api")
                .version("1.2")
                .build();
    }

    private List<Parameter> jwt() {
        ParameterBuilder tokenPar = new ParameterBuilder();
        List<Parameter> pars = new ArrayList<>();
        tokenPar.name("Authorization")
                .description("JWT")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .defaultValue(StaticConfig.JwtPrefix + "{jwt}")
                .required(false);
        pars.add(tokenPar.build());
        return pars;
    }
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
                                                                         ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
                                                                         EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
                                                                         WebEndpointProperties webEndpointProperties, Environment environment) {

        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping =
                webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
                        || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
        return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
                corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping, null);
    }

}


import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import com.thinkmore.forum.filter.JwtGenerateFilter;
import com.thinkmore.forum.filter.JwtCheckFilter;
import com.thinkmore.forum.service.JwtRouterService;
import com.thinkmore.forum.service.UsersService;

import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UsersService usersService;
    private final JwtRouterService jwtRouterService;

    public final static SecretKey secretKey = Keys.hmacShaKeyFor(StaticConfig.JwtSecretKey.getBytes(StandardCharsets.UTF_8));

    @SneakyThrows
    protected void configure(HttpSecurity http) {
        http
                .cors()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilter(new JwtGenerateFilter(usersService, jwtRouterService, authenticationManager()))
                .addFilterAfter(new JwtCheckFilter(jwtRouterService), JwtGenerateFilter.class)
                .authorizeRequests()
                .antMatchers(StaticConfig.ignoreUrl).permitAll()
                .anyRequest().authenticated();
    }

    @Override
    @SneakyThrows
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(daoAuthenticationProvider());
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(usersService);
        return provider;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder();
    }
}


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${domain.name}")
    public String domainName;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/hall", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/v1/public/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("X-Requested-With", "Origin", "Content-Type", "Accept", "Authorization")
                        .exposedHeaders("Access-Control-Allow-Headers", "Authorization, x-xsrf-token, Access-Control-Allow-Headers, Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers")
                        .allowedOrigins("*");
            }
        };
    }
}

public class StaticConfig {
    public final static String[] ignoreUrl = new String[] {
            // -- Swagger UI v2
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            // -- Prometheus Metrics
            "/actuator/**",
            // -- API
            "/v1/public/**",
    };

    // Role
    public final static String DefaultRole = "general_user";

    // Email
    public final static String DecodedKey = "1a53d4469f513e1ae3856fc2c603b8d6";
    public final static String Apikey = "bf7429164ac97e5dae68b01c9b5f4db2fdf172b5e843d2c2d8f3f829e9b785c02cd38bda9be56537804f8e5626eeca116989564aa0d603c40355d037f4713c55c2638ef376ce12e5455e4dbfd5cd49cc";
    public final static String fromEmail = "jiangjianglovezhou@gmail.com";
    public final static String ResetPasswordUrl = "/password-reset?token=";
    public final static String ResetPasswordContext = "Please click the link to reset your new password:\n";
    public final static String VerifyEmailUrl = "/verify-email?token=";
    public final static String VerifyEmailContext = "Please click the link to verify your new email:\n";

    // Oss
    public final static String BucketName = "image";

    // JWT
    public final static String JwtSecretKey = "https://github.com/Qiming-Liu/ThinkMoreForum-Backend";
    public final static String JwtPrefix = "Bearer ";
}


import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableRabbit
public class RabbitMinioConfig {

    @Value("${minio.username}")
    private String minioUsername;

    @Value("${minio.password}")
    private String minioPassword;

    @Value("${minio.url}")
    private String minioUrl;

    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient minioClient = MinioClient.builder().endpoint(minioUrl)
                                             .credentials(minioUsername, minioPassword).build();

        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(StaticConfig.BucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(StaticConfig.BucketName).build());
        }

        String policyJson = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:ListBucketMultipartUploads\",\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::image\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\",\"s3:PutObject\"],\"Resource\":[\"arn:aws:s3:::image/*\"]}]}";
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(StaticConfig.BucketName).config(policyJson).build());

        return minioClient;
    }
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PostMiniGetDto implements Serializable {
    private UUID id;
    private String title;
}


import com.thinkmore.forum.dto.category.CategoryPutDto;
import com.thinkmore.forum.dto.users.UsersPutDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PostMiniPutDto implements Serializable {
    private String headImgUrl;
    private String title;
    private String context;
}


import com.thinkmore.forum.dto.comment.CommentGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class PostCommentGetDto implements Serializable {
    private PostGetDto post;
    private List<CommentGetDto> comments;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class PostPostDto implements Serializable {
    private String categoryTitle;
    private String headImgUrl;
    private String title;
    private String context;
    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.category.CategoryMiniGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PostGetDto implements Serializable {
    private UUID id;
    private UsersMiniGetDto postUsers;
    private CategoryMiniGetDto category;
    private String headImgUrl;
    private String title;
    private String context;
    private Integer viewCount;
    private Integer followCount;
    private Integer commentCount;
    private Boolean visibility;
    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.category.CategoryPutDto;
import com.thinkmore.forum.dto.users.UsersPutDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PostPutDto implements Serializable {
    private UUID id;
    private UsersPutDto postUsers;
    private CategoryPutDto category;
    private String headImgUrl;
    private String title;
    private String context;
    private Integer viewCount;
    private Integer followCount;
    private Integer commentCount;
    private Boolean visibility;
    private OffsetDateTime createTimestamp;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CategoryMiniGetDto implements Serializable {
    private UUID id;
    private String title;
}


import com.thinkmore.forum.dto.post.PostMiniGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CategoryGetDto implements Serializable {
    private UUID id;
    private PostMiniGetDto pinPost;
    private String headImgUrl;
    private String color;
    private String title;
    private String description;
    private Integer viewCount;
    private Integer postCount;
    private Integer sortOrder;
    private Integer participantCount;
    private OffsetDateTime lastUpdateTimestamp;
}


import com.thinkmore.forum.dto.post.PostPutDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CategoryPutDto implements Serializable {
    private UUID id;
    private PostPutDto pinPost;
    private String headImgUrl;
    private Integer type;
    private String color;
    private String title;
    private String description;
    private Integer viewCount;
    private Integer postCount;
    private Integer sortOrder;
    private Integer participantCount;
    private OffsetDateTime lastUpdateTimestamp;
}


import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FollowPostPostDto implements Serializable {
    private UUID id;
    private UsersMiniGetDto users;
    private PostMiniGetDto post;
    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.post.PostPutDto;
import com.thinkmore.forum.dto.users.UsersPutDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FollowPostPutDto implements Serializable {
    private UUID id;
    private UsersPutDto users;
    private PostPutDto post;
    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FollowPostGetDto implements Serializable {
    private UUID id;
    private UsersMiniGetDto users;
    private PostMiniGetDto post;
    private OffsetDateTime createTimestamp;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RolesGetDto implements Serializable {
    private UUID id;
    private String roleName;
    private String permission;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RolesPostDto implements Serializable {
    private UUID id;
    private String roleName;
    private String permission;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RolesPutDto implements Serializable {
    private UUID id;
    private String roleName;
    private String permission;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ImgGetDto implements Serializable {
    private UUID id;
    private String url;
    private String md5;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ComponentPostDto implements Serializable {
    private UUID id;
    private String name;
    private String code;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ComponentGetDto implements Serializable {
    private UUID id;
    private String name;
    private String code;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ComponentPutDto implements Serializable {
    private UUID id;
    private String name;
    private String code;
}


import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.thinkmore.forum.dto.users.UsersMiniGetDto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationGetDto implements Serializable {
    private UUID id;
    private UsersMiniGetDto triggerUsers;
    private UsersMiniGetDto notifyUsers;
    private String context;
    private Boolean viewed;
    private OffsetDateTime createTimestamp;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UsersPasswordPutDto implements Serializable {
    private String password;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UsersPostDto implements Serializable {
    private String username;
    private String email;
    private String password;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UsersImgPutDto implements Serializable {
    private String headImgUrl;
    private String profileImgUrl;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UsersMiniGetDto implements Serializable {
    private UUID id;
    private String headImgUrl;
    private String username;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UsersMiniPutDto implements Serializable {
    private String oldPassword;
    private String newPassword;
}


import com.thinkmore.forum.dto.roles.RolesPutDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UsersPutDto implements Serializable {
    private UUID id;
    private String username;
    private String email;
    private String headImgUrl;
    private String profileImgUrl;
    private RolesPutDto role;
    private OffsetDateTime lastLoginTimestamp;
    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.roles.RolesGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UsersGetDto implements Serializable {
    private UUID id;
    private String username;
    private String email;
    private String headImgUrl;
    private String profileImgUrl;
    private RolesGetDto role;
    private OffsetDateTime lastLoginTimestamp;
    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class CommentPostDto implements Serializable {
    private UsersMiniGetDto commentUsers;
    private UsersMiniGetDto mentionUsers;
    private PostMiniGetDto post;
    private CommentMiniGetDto parentComment;
    private String context;
    private Boolean visibility;
}


import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CommentGetDto implements Serializable {
    private UUID id;
    private UsersMiniGetDto commentUsers;
    private UsersMiniGetDto mentionUsers;
    private PostMiniGetDto post;
    private CommentMiniGetDto parentComment;
    private String context;
    private Boolean visibility;
    private OffsetDateTime createTimestamp;
}


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CommentMiniGetDto implements Serializable {
    private UUID id;
}


import com.thinkmore.forum.dto.users.UsersPostDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FollowerUsersPostDto implements Serializable {
    private UUID id;

    private UsersPostDto usersPostDto;

    private UsersPostDto followedUsersPostDto;

    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.users.UsersGetDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class FollowerUsersGetDto implements Serializable {
    private UUID id;

    private UsersGetDto users;

    private UsersGetDto followedUsers;

    private OffsetDateTime createTimestamp;
}


import com.thinkmore.forum.dto.comment.CommentGetDto;
import com.thinkmore.forum.dto.post.*;
import com.thinkmore.forum.entity.Category;
import com.thinkmore.forum.entity.Comment;
import com.thinkmore.forum.entity.Post;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.exception.UserNotFoundException;
import com.thinkmore.forum.mapper.CommentMapper;
import com.thinkmore.forum.mapper.PostMapper;
import com.thinkmore.forum.repository.CategoryRepository;
import com.thinkmore.forum.repository.CommentRepository;
import com.thinkmore.forum.repository.PostRepository;
import com.thinkmore.forum.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final UsersRepository usersRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    public PostGetDto getPostById(UUID postId) throws Exception {
        Optional<Post> targetPost = postRepository.findById(postId);
        PostGetDto targetPostGetDto;
        if (targetPost.isPresent()) {
            targetPostGetDto = postMapper.fromEntity(targetPost.get());
        } else {
            throw new Exception("Couldn't find the post with provided ID");
        }
        return targetPostGetDto;
    }

    public PostCommentGetDto getMaxCountCommentPost(Pageable pageable) {
        List<Post> posts = postRepository.findByOrderByCommentCountDesc(pageable);

        if (posts.size() < 3) {
            return null;
        }

        Random random = new Random();
        int i = random.nextInt(3);

        List<Comment> comments = commentRepository.findByPost_IdOrderByCreateTimestampAsc(posts.get(i).getId());
        PostCommentGetDto postCommentGetDto = new PostCommentGetDto();
        List<CommentGetDto> commentGetDtoList = comments.stream().map(commentMapper::fromEntity).collect(Collectors.toList());

        postCommentGetDto.setComments(commentGetDtoList);
        postCommentGetDto.setPost(postMapper.fromEntity(posts.get(i)));
        return postCommentGetDto;
    }

    @Transactional
    public String postPost
            (UUID
                     userId, PostPostDto
                     postPostDto) {

        Post post = postMapper.toEntity(postPostDto);
        post.setPostUsers(usersRepository.getById(userId));
        post.setCategory(categoryRepository.findByTitle(postPostDto.getCategoryTitle()).get());

        postRepository.save(post);

        Category categoryToUpdate = categoryRepository.findByTitle(post.getCategory().getTitle()).get();
        int newPostCount = (int) postRepository.countByCategory_IdAndVisibilityIsTrue(categoryToUpdate.getId());
        categoryToUpdate.setPostCount(newPostCount);
        categoryRepository.save(categoryToUpdate);

        categoryService.updateParticipant(post.getCategory().getId());

        return post.getId().toString();
    }

    @Transactional
    public boolean editPost (UUID postId, UUID userId, PostMiniPutDto postMiniPutDto) {
        Post currentPost = postRepository.findById(postId).get();
        Users requestUser = usersRepository.findById(userId).get();
        if (!requestUser.getRole().getRoleName().equals("admin")) {
            if (!currentPost.getPostUsers().getId().equals(userId)) {
                return false;
            }
        }

        currentPost.setHeadImgUrl(postMiniPutDto.getHeadImgUrl());
        currentPost.setTitle(postMiniPutDto.getTitle());
        currentPost.setContext(postMiniPutDto.getContext());
        postRepository.save(currentPost);

        Category categoryToUpdate = categoryRepository.findById(currentPost.getCategory().getId()).get();
        int newPostCount = (int) postRepository.countByCategory_IdAndVisibilityIsTrue(categoryToUpdate.getId());
        categoryToUpdate.setPostCount(newPostCount);
        categoryRepository.save(categoryToUpdate);

        return true;
    }

    public List<PostMiniGetDto> getAllPostsCoreInfo
            () {
        return postRepository.findAll().stream()
                             .map(postMapper::entityToMiniDto)
                             .collect(Collectors.toList());
    }

    public List<PostGetDto> getPostByTitleContainingString
            (String
                     string) {
        return postRepository.findByTitleContainingIgnoreCase(string).stream()
                             .map(postMapper::fromEntity)
                             .collect(Collectors.toList());
    }

    public List<PostGetDto> getPostsByPostUsersName
            (String
                     username) {
        Users user = usersRepository.findByUsername(username)
                                    .orElseThrow(() -> new UserNotFoundException("Invalid UserName"));
        return postRepository.findByPostUsersId(user.getId()).stream()
                             .map(postMapper::fromEntity)
                             .collect(Collectors.toList());
    }

    public long getCountOfVisiblePostsByCategoryId
            (UUID
                     categoryId) {
        return postRepository.countByCategory_IdAndVisibilityIsTrue(categoryId);
    }

    public List<PostGetDto> getVisiblePostsByCategoryId
            (UUID
                     categoryId, Pageable
                     pageable) {
        return postRepository.findByCategory_IdAndVisibilityIsTrue(categoryId, pageable).stream()
                             .map(postMapper::fromEntity)
                             .collect(Collectors.toList());
    }

    @Transactional
    public Boolean changePostVisibility
            (UUID
                     postId, UUID
                     userId) {
        Post oldPost = postRepository.findById(postId).get();
        Users requestInitiator = usersRepository.findById(userId).get();
        if (!requestInitiator.getRole().getRoleName().equals("admin")) {
            if (!oldPost.getPostUsers().getId().equals(userId)) {
                return false;
            }
        }

        oldPost.setVisibility(!oldPost.getVisibility());
        postRepository.save(oldPost);

        Category categoryToUpdate = categoryRepository.findById(oldPost.getCategory().getId()).get();
        int newPostCount = (int) postRepository.countByCategory_IdAndVisibilityIsTrue(categoryToUpdate.getId());
        categoryToUpdate.setPostCount(newPostCount);
        categoryRepository.save(categoryToUpdate);

        return true;
    }

    @Transactional
    public void updateViewCount
            (UUID
                     postId) {
        Post post = postRepository.findById(postId).get();
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);

        Category category = categoryRepository.findById(post.getCategory().getId()).get();
        List<Post> posts = postRepository.findByCategory_IdAndVisibilityIsTrue(category.getId());
        category.setViewCount(posts.stream().mapToInt(Post::getViewCount).sum());
        categoryRepository.save(category);
    }
}


import com.thinkmore.forum.dto.component.ComponentGetDto;
import com.thinkmore.forum.dto.component.ComponentPostDto;
import com.thinkmore.forum.dto.component.ComponentPutDto;
import com.thinkmore.forum.entity.Component;
import com.thinkmore.forum.mapper.ComponentMapper;
import com.thinkmore.forum.repository.ComponentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComponentService {
    private final ComponentRepository componentRepository;
    private final ComponentMapper componentMapper;

    public ComponentGetDto getComponent(String name) throws Exception {
        Optional<Component> component = componentRepository.findByName(name);
        return componentMapper.fromEntity(component.orElseThrow(() -> new Exception("Component not found")));
    }

    @Transactional
    public ComponentGetDto postComponent(ComponentPostDto componentPostDto) {
        Component component = componentMapper.toEntity(componentPostDto);
        componentRepository.save(component);
        return componentMapper.fromEntity(component);
    }

    @Transactional
    public ComponentGetDto putComponent(ComponentPutDto componentPutDto) {
        Component component = componentMapper.toEntity(componentPutDto);
        componentRepository.save(component);
        return componentMapper.fromEntity(component);
    }

    @Transactional
    public Boolean deleteComponent(String name) {
        Optional<Component> component = componentRepository.findByName(name);
        component.ifPresent(componentRepository::delete);
        return true;
    }
}

import com.thinkmore.forum.entity.redis.OnlineUser;
import com.thinkmore.forum.entity.websocket.OnlineMessage;
import com.thinkmore.forum.entity.websocket.ReminderMessage;
import com.thinkmore.forum.repository.OnlineUsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebsocketService {
    private final OnlineUsersRepository onlineUsersRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public List<String> getOnlineUser() {
        List<OnlineUser> onlineUserList = onlineUsersRepository.findAll();
        Set<String> onlineUserSet = new HashSet<>();
        if (onlineUserList.size() > 0) {
            onlineUserList.forEach(onlineUser -> {
                if (onlineUser != null) {
                    onlineUserSet.add(onlineUser.getUsername());
                }
            });
            return new ArrayList<>(onlineUserSet);
        } else {
            return new ArrayList<>();
        }
    }

    @Transactional
    public List<String> signOnline(String sessionId, OnlineMessage onlineMsg) {

        if (onlineMsg.getUsername().length() > 0) {
            OnlineUser onlineUser = new OnlineUser();
            onlineUser.setId(sessionId);
            onlineUser.setUsername(onlineMsg.getUsername());
            onlineUsersRepository.save(onlineUser);
        }

        return getOnlineUser();
    }

    public ReminderMessage forwardReminder(ReminderMessage reminder) {
        simpMessagingTemplate.convertAndSendToUser(reminder.getRecipient(), "/reminded", reminder);
        return reminder;
    }

    @Transactional
    public void signOffline(SessionDisconnectEvent event) {
        onlineUsersRepository.findById(event.getSessionId()).ifPresent(onlineUsersRepository::delete);
        this.simpMessagingTemplate.convertAndSend("/hall/greetings", getOnlineUser());
    }
}


import java.util.Optional;

import com.thinkmore.forum.entity.redis.JwtRouter;
import com.thinkmore.forum.repository.JwtRouterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtRouterService {
    private final JwtRouterRepository jwtRouterRepository;

    @Transactional
    public String getRealJwt(String fakeJwt) {
        Optional<JwtRouter> jwtRouter = jwtRouterRepository.findById(fakeJwt);
        return jwtRouter.map(JwtRouter::getRealJwt).orElse(null);
    }

    @Transactional
    public String getFakeJwt(String realJwt) {
        String md5 = DigestUtils.md5Hex(realJwt + Math.random());
        String fakeJwt = md5 + Math.random();

        JwtRouter jwtRouter = new JwtRouter();
        jwtRouter.setFakeJwt(fakeJwt);
        jwtRouter.setRealJwt(realJwt);
        jwtRouterRepository.save(jwtRouter);
        return fakeJwt;
    }
}


import com.thinkmore.forum.dto.followerUsers.FollowerUsersGetDto;
import com.thinkmore.forum.entity.FollowUser;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.exception.UserNotFoundException;
import com.thinkmore.forum.mapper.FollowerUsersMapper;
import com.thinkmore.forum.repository.FollowerUsersRepository;
import com.thinkmore.forum.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowerUsersService {
    private final FollowerUsersRepository followerUsersRepository;
    private final UsersRepository usersRepository;
    private final FollowerUsersMapper followerUsersMapper;
    private final NotificationService notificationService;

    public List<FollowerUsersGetDto> getFollowersByUsername(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserName"));
        return followerUsersRepository.findAllByFollowedUsersId(user.getId()).stream().map(followerUsersMapper::fromEntity).collect(Collectors.toList());
    }

    public List<FollowerUsersGetDto> getFollowingByUsername(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserName"));
        return followerUsersRepository.findAllByUsersId(user.getId()).stream().map(followerUsersMapper::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public FollowerUsersGetDto followUsers(UUID myUsersId, String hisUsername) {
        Users myUser = usersRepository.findById(myUsersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));
        Users hisUser = usersRepository.findByUsername(hisUsername)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        FollowUser followUser = new FollowUser();
        followUser.setUsers(myUser);
        followUser.setFollowedUsers(hisUser);
        followerUsersRepository.save(followUser);
        notificationService.postNotification(myUser, hisUser, " followed you.");

        return followerUsersMapper.fromEntity(followUser);
    }

    @Transactional
    public void unfollowUsers(UUID userID, String followedUsername) {
        Users followedUser = usersRepository.findByUsername(followedUsername)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserName"));
        followerUsersRepository.deleteByUsersIdAndFollowedUsersId(userID, followedUser.getId());
    }

    public boolean followStatus(String username, UUID usersId) {
        Users tampUser = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserName"));

        boolean status;
        status = !followerUsersRepository.findByUsersIdAndFollowedUsersId(usersId, tampUser.getId()).isEmpty();
        return status;
    }
}


import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.dto.roles.RolesGetDto;
import com.thinkmore.forum.dto.roles.RolesPutDto;
import com.thinkmore.forum.entity.Roles;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.mapper.RolesMapper;
import com.thinkmore.forum.repository.RolesRepository;
import com.thinkmore.forum.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RolesRepository roleRepository;
    private final RolesMapper rolesMapper;
    private final UsersRepository usersRepository;

    public List<RolesGetDto> getAllRoles() {
        return roleRepository.findAll().stream().map(rolesMapper::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public boolean putRole(List<RolesPutDto> rolesPutDtoList) {
        List<Roles> roleNewList = rolesPutDtoList.stream().map(rolesMapper::toEntity).collect(Collectors.toList());
        List<Roles> roleOldList = roleRepository.findAll();

        List<Roles> removeList = roleOldList.stream().filter(roles -> {
            for (Roles roleNew : roleNewList) {
                if (roleNew.getId().equals(roles.getId())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        List<Roles> addList = roleNewList.stream().filter(roles -> {
            if (roles.getId() != null) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        List<Roles> updateList = roleNewList.stream().filter(roles -> {
            if (roles.getId() == null) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());

        List<List<Users>> usersList = removeList.stream().map(
                (r) -> usersRepository.findByRole(r)).collect(Collectors.toList());

        roleRepository.deleteAll(removeList);
        roleRepository.saveAll(updateList);
        roleRepository.saveAll(addList);

        Roles role = roleRepository.findByRoleName(StaticConfig.DefaultRole).get();
        usersList.stream().map(list -> {
            if (!list.isEmpty()) {
                list.stream().map(users -> {
                    users.setRole(role);
                    return users;
                });
            }
            usersRepository.saveAll(list);
            return list;
        });
        return true;
    }
}


import java.util.Optional;
import java.util.UUID;

import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.entity.JwtUser;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.entity.rabbitmq.ResetPasswordEmailMessage;
import com.thinkmore.forum.entity.rabbitmq.VerificationEmailMessage;
import com.thinkmore.forum.exception.UserNotFoundException;
import com.thinkmore.forum.repository.UsersRepository;
import com.thinkmore.forum.util.Util;

import io.prometheus.client.Gauge;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    @Value("${domain.name}")
    public String domainName;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private static final Gauge emailQueue = Gauge.build()
                                                 .name("email")
                                                 .help("size of email queue.")
                                                 .register();

    private final UsersRepository usersRepository;
    private final JwtRouterService jwtRouterService;

    @Transactional
    public boolean sendVerificationEmail(UUID usersId, String newEmail) {
        VerificationEmailMessage message = new VerificationEmailMessage(usersId, newEmail);
        emailQueue.inc();
        rabbitTemplate.convertAndSend("VerificationEmail", message);
        return true;
    }

    @Transactional
    @RabbitListener(queues = "VerificationEmail")
    public void handleVerificationEmail(VerificationEmailMessage message) throws Exception {
        Users user = usersRepository.findById(message.getUsersId())
                                    .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));
        emailQueue.dec();

        Util.createMail(
                StaticConfig.fromEmail,
                user.getEmail(),
                "Change Email Request",
                "Your account " + user.getUsername() + " is changing email to " + message.getNew_email());

        Util.createMail(
                StaticConfig.fromEmail,
                message.getNew_email(),
                "Verify Email",
                StaticConfig.VerifyEmailContext + domainName + StaticConfig.VerifyEmailUrl + message.getNew_email());
    }

    @Transactional
    public boolean sendResetPasswordEmail(String email) {
        ResetPasswordEmailMessage message = new ResetPasswordEmailMessage(email);
        rabbitTemplate.convertAndSend("ResetPasswordEmail", message);
        return true;
    }

    @Transactional
    @RabbitListener(queues = "ResetPasswordEmail")
    public void handleResetPasswordEmail(ResetPasswordEmailMessage message) throws Exception {
        Optional<Users> user = usersRepository.findByEmail(message.getEmail());
        emailQueue.dec();

        if (user.isPresent()) {
            String fakeJwt = StaticConfig.JwtPrefix + jwtRouterService.getFakeJwt(Util.generateJwt(new JwtUser(user.get())));
            String encode = Util.UrlEncoder(fakeJwt);
            Util.createMail(
                    StaticConfig.fromEmail,
                    message.getEmail(),
                    "Reset password",
                    StaticConfig.ResetPasswordContext +
                            domainName + StaticConfig.ResetPasswordUrl + encode);
        }
    }
}

import com.thinkmore.forum.dto.users.*;
import com.thinkmore.forum.entity.JwtUser;
import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.entity.Roles;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.exception.InvalidOldPasswordException;
import com.thinkmore.forum.exception.UserNotFoundException;
import com.thinkmore.forum.mapper.UsersMapper;
import com.thinkmore.forum.repository.RolesRepository;
import com.thinkmore.forum.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsersService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;

    private final UsersRepository usersRepository;
    private final UsersMapper usersMapper;
    private final RolesRepository rolesRepository;

    //only for jwt
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user = usersRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException(String.format("Login Email %s not found", email)));
        return new JwtUser(user);
    }

    @Transactional
    public Boolean register(UsersPostDto usersPostDto) {
        Users user = usersMapper.toEntity(usersPostDto);

        user.setUsername(usersPostDto.getUsername());
        user.setPassword(passwordEncoder.encode(usersPostDto.getPassword()));
        user.setEmail(usersPostDto.getEmail());
        user.setRole(rolesRepository.findByRoleName(StaticConfig.DefaultRole).orElseThrow());

        usersRepository.save(user);

        return true;
    }

    public UsersGetDto updateLastLoginTimestamp(String username) {
        Users user = usersRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException(String.format("Username %s not found", username)));
        user.setLastLoginTimestamp(OffsetDateTime.now());
        usersRepository.save(user);
        return usersMapper.fromEntity(user);
    }

    public boolean uniqueEmail(String email) {
        return usersRepository.findByEmail(email).isEmpty();
    }

    public boolean uniqueUsername(String username) {
        return usersRepository.findByUsername(username).isEmpty();
    }

    public boolean changeUsername(UUID usersId, String newUsername) {
        Users user = usersRepository.findById(usersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        user.setUsername(newUsername);
        usersRepository.save(user);
        return true;
    }

    @Transactional
    public boolean changeHeadImgUrl(UUID usersId, UsersImgPutDto usersImgPutDto) {
        Users user = usersRepository.findById(usersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        user.setHeadImgUrl(usersImgPutDto.getHeadImgUrl());
        usersRepository.save(user);
        return true;
    }

    @Transactional
    public boolean changeProfileImgUrl(UUID usersId, UsersImgPutDto usersImgPutDto) {
        Users user = usersRepository.findById(usersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        user.setProfileImgUrl(usersImgPutDto.getProfileImgUrl());
        usersRepository.save(user);
        return true;
    }

    public boolean changeEmail(UUID usersId, String newEmail) {
        Users user = usersRepository.findById(usersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        user.setEmail(newEmail);
        usersRepository.save(user);

        return true;
    }

    public boolean changePassword(UUID usersId, UsersMiniPutDto usersMiniPutDto) {
        Users user = usersRepository.findById(usersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        if (!passwordEncoder.matches(usersMiniPutDto.getOldPassword(), user.getPassword())) {
            throw new InvalidOldPasswordException("Old password is wrong");
        }

        user.setPassword(passwordEncoder.encode(usersMiniPutDto.getNewPassword()));
        usersRepository.save(user);
        return true;
    }

    public boolean resetPassword(UUID usersId, UsersPasswordPutDto password) {
        Users user = usersRepository.findById(usersId)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserID"));

        user.setPassword(passwordEncoder.encode(password.getPassword()));
        usersRepository.save(user);
        return true;
    }

    public UsersGetDto getUserByUsername(String username) {
        Optional<Users> targetUsers = usersRepository.findByUsername(username);
        return targetUsers.map(usersMapper::fromEntity).orElse(null);
    }

    public List<UsersGetDto> getUserByContainingString(String string) {
        List<Users> users = usersRepository.findByUsernameContainingIgnoreCase(string);
        List<UsersGetDto> usersGetDto = new ArrayList<>();
        for (Users user : users) {
            usersGetDto.add(usersMapper.fromEntity(user));
        }
        return usersGetDto;
    }

    public List<UsersGetDto> getAllUsers() {
        return usersRepository.findAll().stream()
                .map(usersMapper::fromEntity)
                .collect(Collectors.toList());
    }

    public void changeSingleUserRole(UsersGetDto inputUserGetDto) {
        Users userToUpdate = usersRepository.findById(inputUserGetDto.getId()).get();
        Roles newRoleOfUser = rolesRepository.findByRoleName(inputUserGetDto.getRole().getRoleName()).orElseThrow();
        userToUpdate.setRole(newRoleOfUser);
        usersRepository.save(userToUpdate);
    }

    public void changeUsersRoles(List<UsersGetDto> usersGetDtoList) {
        usersGetDtoList.forEach(singleUserGetDto -> changeSingleUserRole(singleUserGetDto));
    }
}


import com.thinkmore.forum.dto.followPost.FollowPostGetDto;
import com.thinkmore.forum.dto.followPost.FollowPostPostDto;
import com.thinkmore.forum.dto.post.PostMiniGetDto;
import com.thinkmore.forum.dto.users.UsersMiniGetDto;
import com.thinkmore.forum.entity.FollowPost;
import com.thinkmore.forum.entity.Post;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.exception.UserNotFoundException;
import com.thinkmore.forum.mapper.FollowPostMapper;
import com.thinkmore.forum.mapper.PostMapper;
import com.thinkmore.forum.mapper.UsersMapper;
import com.thinkmore.forum.repository.FollowPostRepository;
import com.thinkmore.forum.repository.PostRepository;
import com.thinkmore.forum.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowPostService {

    private final FollowPostRepository followPostRepository;
    private final FollowPostMapper followPostMapper;

    private final UsersRepository usersRepository;
    private final UsersMapper usersMapper;

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    private final NotificationService notificationService;

    public List<FollowPostGetDto> getAllFollowPostsByUserId(UUID userId) {
        return followPostRepository.findByUsers_IdOrderByCreateTimestampDesc(userId).stream()
                .map(followPostMapper::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FollowPostGetDto> getAllFollowPostsByUsername(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Invalid UserName"));
        return followPostRepository.findByUsers_IdOrderByCreateTimestampDesc(user.getId()).stream()
                .map(followPostMapper::fromEntity)
                .collect(Collectors.toList());
    }

    public Boolean checkUserFollowingState(UUID postId, UUID userId) {
        Optional<FollowPost> targetFollowPost = followPostRepository.findByPost_IdAndUsers_Id(postId, userId);
        if (targetFollowPost.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public void postFollowPostToUser(UUID postId, UUID userId) {
        Users users = usersRepository.findById(userId).get();
        UsersMiniGetDto usersMiniGetDto = usersMapper.entityToMiniDto(users);
        Post post = postRepository.findById(postId).get();
        PostMiniGetDto postMiniGetDto = postMapper.entityToMiniDto(post);

        FollowPostPostDto followPostPostDto = new FollowPostPostDto();
        followPostPostDto.setUsers(usersMiniGetDto);
        followPostPostDto.setPost(postMiniGetDto);
        FollowPost followPost = followPostMapper.toEntity(followPostPostDto);
        followPostRepository.save(followPost);

        notificationService.postNotification(users, post.getPostUsers(), " followed your post.");

        Post postToUpdate = postRepository.findById(postId).get();
        int newFollowCount = (int) followPostRepository.countByPost_Id(postId);
        postToUpdate.setFollowCount(newFollowCount);
        postRepository.save(postToUpdate);

    }

    @Transactional
    public String userUnfollowPost(UUID postId, UUID userId) {
        long responseValue = followPostRepository.deleteByUsers_IdAndPost_Id(userId, postId);
        if (responseValue > 0) {
            Post postToUpdate = postRepository.findById(postId).get();
            int newFollowCount = (int) followPostRepository.countByPost_Id(postId);
            postToUpdate.setFollowCount(newFollowCount);
            postRepository.save(postToUpdate);
            return "Successfully unfollowed!";
        }
        return "Unfollow failed or you didn't follow this post";
    }

}


import com.thinkmore.forum.dto.notification.NotificationGetDto;
import com.thinkmore.forum.entity.Notification;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.mapper.NotificationMapper;
import com.thinkmore.forum.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public List<NotificationGetDto> getNotificationsByUserId(UUID userId) {

        List<NotificationGetDto> all = notificationRepository.findByNotifyUsers_IdOrderByCreateTimestampDesc(userId).stream()
                .map(notificationMapper::fromEntity)
                .collect(Collectors.toList());

        // remove viewed notification
        all.removeIf(NotificationGetDto::getViewed);

        return all;
    }

    public boolean markAsViewed(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setViewed(true);
            notificationRepository.save(notification);
        });
        return true;
    }

    public boolean markAllAsViewed(UUID userId) {
        notificationRepository.findByNotifyUsers_IdOrderByCreateTimestampDesc(userId)
                .forEach(notification -> {
                    notification.setViewed(true);
                    notificationRepository.save(notification);
                });

        return true;
    }

    public void postNotification(Users triggerUser, Users notifyUser, String context) {

        Notification notification = new Notification();
        notification.setTriggerUsers(triggerUser);
        notification.setNotifyUsers(notifyUser);
        notification.setContext(triggerUser.getUsername() + context);
        notification.setViewed(false);
        notificationRepository.save(notification);
    }
}


import com.thinkmore.forum.dto.category.CategoryGetDto;
import com.thinkmore.forum.dto.category.CategoryPutDto;
import com.thinkmore.forum.entity.Category;
import com.thinkmore.forum.entity.Comment;
import com.thinkmore.forum.entity.Post;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.mapper.CategoryMapper;
import com.thinkmore.forum.repository.CategoryRepository;
import com.thinkmore.forum.repository.CommentRepository;
import com.thinkmore.forum.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public List<CategoryGetDto> getAllCategories() {
        return categoryRepository.findByOrderBySortOrderAsc().stream().map(categoryMapper::fromEntity).collect(Collectors.toList());
    }

    public CategoryGetDto getCategoryByCategoryTitle(String category_title) throws Exception {
        Optional<Category> targetCategory = categoryRepository.findByTitle(category_title);
        CategoryGetDto targetCategoryGetDto;
        if (targetCategory.isPresent()) {
            targetCategoryGetDto = categoryMapper.fromEntity(targetCategory.get());
        } else {
            throw new Exception("Couldn't find the category with provided ID");
        }
        return targetCategoryGetDto;
    }

    @Transactional
    public Boolean putCategory(List<CategoryPutDto> categoryPutDtoList) {
        List<Category> categoryNewList = categoryPutDtoList.stream().map(categoryMapper::toEntity).collect(Collectors.toList());
        List<Category> categoryOldList = categoryRepository.findByOrderBySortOrderAsc();
        for (int i = 0; i < categoryNewList.size(); i++) {
            categoryNewList.get(i).setSortOrder(i);
        }

        List<Category> removeList = categoryOldList.stream().filter(category -> {
            for (Category categoryNew : categoryNewList) {
                if (categoryNew.getId().equals(category.getId())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());

        List<Category> addList = categoryNewList.stream().filter(category -> {
            if (category.getId() != null) {
                return false;
            }
            category.setLastUpdateTimestamp(OffsetDateTime.now());
            category.setParticipantCount(0);
            return true;
        }).collect(Collectors.toList());

        List<Category> updateList = categoryNewList.stream().filter(category -> {
            if (category.getId() == null) {
                return false;
            }
            category.setLastUpdateTimestamp(OffsetDateTime.now());
            return true;
        }).collect(Collectors.toList());

        removeList.forEach(category ->
                postRepository.findByCategory_Title(category.getTitle())
                              .forEach(post -> post.setCategory(null)));

        categoryRepository.deleteAll(removeList);
        categoryRepository.saveAll(updateList);
        categoryRepository.saveAll(addList);
        return true;
    }

    @Transactional
    public CategoryGetDto putCategoryPinPostById(UUID categoryId, UUID postId) {
        Category category = categoryRepository.findById(categoryId).get();
        category.setPinPost(postRepository.findById(postId).get());
        categoryRepository.save(category);
        return categoryMapper.fromEntity(category);
    }

    @Transactional
    public CategoryGetDto putCategoryPinPostNull(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).get();
        category.setPinPost(null);
        categoryRepository.save(category);
        return categoryMapper.fromEntity(category);
    }

    @Transactional
    public void updateParticipant(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId).get();
        List<Post> postList = postRepository.findByCategory_Title(category.getTitle());
        List<Comment> commentList = postList.stream().flatMap(post -> commentRepository.findByPost_IdOrderByCreateTimestampAsc(post.getId()).stream()).collect(Collectors.toList());
        List<Users> usersList = commentList.stream().map(Comment::getCommentUsers).collect(Collectors.toList());
        usersList.addAll(postList.stream().map(Post::getPostUsers).collect(Collectors.toList()));
        HashSet<UUID> set = new HashSet<>();
        for (Users user : usersList) {
            set.add(user.getId());
        }
        category.setParticipantCount(set.size());
        category.setLastUpdateTimestamp(OffsetDateTime.now());
        categoryRepository.save(category);
    }
}


import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.entity.Img;
import com.thinkmore.forum.repository.ImgRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImgService {
    private final ImgRepository imgRepository;
    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    @Transactional
    public Img upload(MultipartFile img) throws Exception {
        byte[] imgBytes = img.getBytes();
        String md5 = DigestUtils.md5Hex(imgBytes);

        // check
        Optional<Img> image = imgRepository.findByMd5(md5);
        if (image.isPresent()) {
            return image.get();
        }

        // put
        String fileName = img.getOriginalFilename();
        if (fileName == null) {
            throw new RuntimeException();
        }
        fileName = md5 + (fileName.endsWith(".png") ? ".png" : ".jpg");

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(StaticConfig.BucketName)
                        .object(fileName)
                        .stream(new ByteArrayInputStream(imgBytes), imgBytes.length, -1)
                        .contentType(fileName.endsWith(".png") ? "image/png" : "image/jpeg")
                        .build());

        // set
        Img theImg = new Img();
        theImg.setUrl(minioUrl + "/" + StaticConfig.BucketName + "/" + fileName);
        theImg.setMd5(md5);
        imgRepository.save(theImg);

        return theImg;
    }
}


import com.thinkmore.forum.dto.comment.CommentGetDto;
import com.thinkmore.forum.dto.comment.CommentPostDto;
import com.thinkmore.forum.entity.Comment;
import com.thinkmore.forum.entity.Post;
import com.thinkmore.forum.entity.Users;
import com.thinkmore.forum.mapper.CommentMapper;
import com.thinkmore.forum.repository.CommentRepository;
import com.thinkmore.forum.repository.PostRepository;
import com.thinkmore.forum.repository.UsersRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UsersRepository usersRepository;
    private final PostRepository postRepository;

    private final CommentMapper commentMapper;
    private final NotificationService notificationService;
    private final CategoryService categoryService;

    public List<CommentGetDto> getAllByPost(UUID postId) {
        return commentRepository.findByPost_IdOrderByCreateTimestampAsc(postId).stream()
                                .map(commentMapper::fromEntity)
                                .collect(Collectors.toList());
    }

    @Transactional
    public String postComment(UUID userId, CommentPostDto commentPostDto) {
        Users users = usersRepository.findById(userId).get();
        Post post = postRepository.getById(commentPostDto.getPost().getId());
        Comment comment = commentMapper.toEntity(commentPostDto);
        comment.setCommentUsers(users);
        comment.setPost(post);
        if (commentPostDto.getParentComment() != null) {
            Comment parentComment = commentRepository.getById(commentPostDto.getParentComment().getId());
            comment.setParentComment(parentComment);
        }
        commentRepository.save(comment);

        String context;
        Users notifyUser;
        if (comment.getMentionUsers() != null) {
            notifyUser = comment.getMentionUsers();
            context = comment.getCommentUsers().getUsername() + " mentioned you in a comment";
        } else if (comment.getParentComment() == null) {
            notifyUser = comment.getPost().getPostUsers();
            context = " replied your post.";
        } else {
            notifyUser = comment.getParentComment().getCommentUsers();
            context = " replied your comment.";
        }

        notificationService.postNotification(users, notifyUser, context);

        Post postToUpdate = postRepository.findById(commentPostDto.getPost().getId()).get();
        int newCommentCount = (int) commentRepository.countByPost_IdAndVisibilityIsTrue(postToUpdate.getId());
        postToUpdate.setCommentCount(newCommentCount);
        postRepository.save(postToUpdate);

        categoryService.updateParticipant(post.getCategory().getId());

        return "You've successfully replied the post!";
    }
}


import com.thinkmore.forum.entity.FollowUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowerUsersRepository extends JpaRepository<FollowUser, UUID> {

    List<FollowUser> findAllByUsersId(UUID userId);

    List<FollowUser> findAllByFollowedUsersId(UUID FollowedUsersId);

    List<FollowUser> findByUsersIdAndFollowedUsersId(UUID userId, UUID FollowedUsersId);

    long deleteByUsersIdAndFollowedUsersId(UUID userId, UUID FollowedUsersId);

}


import com.thinkmore.forum.entity.Post;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {

    @Override
    void deleteById(@NotNull UUID uuid);

    List<Post> findByCategory_Title(String title);

    List<Post> findByTitleContainingIgnoreCase(String title);

    List<Post> findByCategory_Title(String title, Pageable pageable);

    List<Post> findByPostUsersId(UUID Id);

    long countByCategory_Title(String title);

    List<Post> findByOrderByCommentCountDesc(Pageable pageable);

    long countByCategory_IdAndVisibilityIsTrue(UUID id);

    List<Post> findByCategory_IdAndVisibilityIsTrue(UUID id);

    List<Post> findByCategory_IdAndVisibilityIsTrue(UUID id, Pageable pageable);

}

import com.thinkmore.forum.entity.Roles;
import com.thinkmore.forum.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepository  extends JpaRepository<Users, UUID> {

    Optional<Users> findByUsername(String username);

    //get user by username containing string return list of user
    List<Users> findByUsernameContainingIgnoreCase(String username);

    Optional<Users> findByEmail(String email);
    List<Users> findByRole(Roles role);
}


import com.thinkmore.forum.entity.Oauth;
import com.thinkmore.forum.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OauthRepository extends JpaRepository<Oauth, UUID> {
    Optional<Oauth> findByUsers(Users users);
    Optional<Oauth> findByOpenid(String openid);
}


import com.thinkmore.forum.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, JpaSpecificationExecutor<Comment> {

    List<Comment> findByPost_IdOrderByCreateTimestampAsc(UUID id);

    @Override
    void deleteById(UUID uuid);

    long countByPost_IdAndVisibilityIsTrue(UUID id);

}

import java.util.Optional;

import com.thinkmore.forum.entity.redis.JwtRouter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface JwtRouterRepository extends CrudRepository<JwtRouter, String> {
    Optional<JwtRouter> findById(String fakeJwt);
}


import com.thinkmore.forum.entity.Img;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImgRepository extends JpaRepository<Img, UUID> {
    Optional<Img> findByMd5(String hash);
}


import com.thinkmore.forum.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComponentRepository extends JpaRepository<Component, UUID>, JpaSpecificationExecutor<Component> {

    Optional<Component> findByName(String title);
}

import com.thinkmore.forum.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    List<Notification> findByNotifyUsers_IdOrderByCreateTimestampDesc(UUID id);
}

import com.thinkmore.forum.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RolesRepository  extends JpaRepository<Roles, UUID> {

    Optional<Roles> findByRoleName(String roleName);
}


import java.util.List;
import java.util.Optional;

import com.thinkmore.forum.entity.redis.OnlineUser;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnlineUsersRepository extends CrudRepository<OnlineUser, String> {

    List<OnlineUser> findAll();

    Optional<OnlineUser> findById(String Id);

    Optional<OnlineUser> findByUsername(String username);
}

import com.thinkmore.forum.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    @Override
    Optional<Category> findById(UUID uuid);

    @Override
    void deleteById(UUID uuid);

    Optional<Category> findByTitle(String title);

    List<Category> findByOrderBySortOrderAsc();
}

import com.thinkmore.forum.entity.FollowPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowPostRepository extends JpaRepository<FollowPost, UUID>, JpaSpecificationExecutor<FollowPost> {

    @Modifying
    long deleteByUsers_IdAndPost_Id(UUID id, UUID id1);

    List<FollowPost> findByUsers_IdOrderByCreateTimestampDesc(UUID id);

    Optional<FollowPost> findByPost_IdAndUsers_Id(UUID id, UUID id1);

    long countByPost_Id(UUID id);
}


public class InvalidPasswordException extends RuntimeException{
    public InvalidPasswordException(String message) {
        super(message);
    }
}


public class UserHasPasswordException extends RuntimeException{

    public  UserHasPasswordException(String message) {
        super(message);
    }
}


public class InvalidOldPasswordException extends RuntimeException{
    public InvalidOldPasswordException(String message) {
        super(message);
    }
}


public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}


public class UserNotFoundException extends RuntimeException{

    public  UserNotFoundException(String message) {
        super(message);
    }
}


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import com.thinkmore.forum.configuration.SecurityConfig;
import com.thinkmore.forum.configuration.StaticConfig;
import com.thinkmore.forum.entity.JwtUser;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;

@Slf4j
public class Util {

    public static ArrayList<String> getJwtContext() {
        return (ArrayList<String>) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static String generateJwt(JwtUser user) {
        return Jwts.builder()
                   .setId(user.getId() + "")
                   .setSubject(user.getRoleName())
                   .setAudience(user.getPermission())
                   .setIssuedAt(new Date())
                   .setExpiration(java.sql.Date.valueOf(LocalDate.now().plusDays(1)))
                   .signWith(SecurityConfig.secretKey)
                   .compact();
    }

    public static void createMail(String from, String to, String emailTitle, String emailContent) throws Exception {
        Content content = new Content("text/plain", emailContent);
        Mail mail = new Mail(new Email(from), emailTitle, new Email(to), content);
        Key key = new SecretKeySpec(Hex.decodeHex(StaticConfig.DecodedKey), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        SendGrid sg = new SendGrid(new String(cipher.doFinal(Hex.decodeHex(StaticConfig.Apikey))));
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);
        log.info(response.toString());
    }

    public static String UrlEncoder(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
    }

    public static void checkPermission(String permissionName){
        JsonObject permissionObject = JsonParser.parseString(getJwtContext().get(2)).getAsJsonObject();
        if (!permissionObject.get(permissionName).getAsBoolean()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED);
        }
    }
}



import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "roles")
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "role_name", nullable = false, length = 20)
    private String roleName;

    @Column(name = "permission", nullable = false)
    private String permission;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "pin_post_id")
    private Post pinPost;

    @Column(name = "head_img_url")
    private String headImgUrl;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "title", nullable = false, length = 20)
    private String title;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "post_count")
    private Integer postCount;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount;

    @Column(name = "last_update_timestamp", nullable = false)
    private OffsetDateTime lastUpdateTimestamp;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 20)
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "head_img_url")
    private String headImgUrl;

    @Column(name = "profile_img_url")
    private String profileImgUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Roles role;

    @Column(name = "last_login_timestamp")
    private OffsetDateTime lastLoginTimestamp;

    @Column(name = "create_timestamp")
    private OffsetDateTime createTimestamp;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "component")
public class Component {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false)
    private String code;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "follow_post")
public class FollowPost {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "users_id", nullable = false)
    private Users users;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "create_timestamp")
    private OffsetDateTime createTimestamp;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_users_id", nullable = false)
    private Users postUsers;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "head_img_url")
    private String headImgUrl;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "context", nullable = false, length = 65535)
    private String context;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "follow_count")
    private Integer followCount;

    @Column(name = "comment_count")
    private Integer commentCount;

    @Column(name = "visibility")
    private Boolean visibility;

    @Column(name = "create_timestamp")
    private OffsetDateTime createTimestamp;
}

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtUser implements UserDetails {

    private final String username;
    private final String password;

    @Getter
    @Setter
    private UUID id;
    @Getter
    @Setter
    private String roleName;
    @Getter
    @Setter
    private String permission;

    public JwtUser(Users user) {
        username = user.getUsername();
        password = user.getPassword();

        id = user.getId();
        roleName = user.getRole().getRoleName();
        permission = user.getRole().getPermission();
    }

    public JwtUser(ArrayList<String> principal) {
        username = null;
        password = null;

        id = UUID.fromString(principal.get(0));
        roleName = principal.get(1);
        permission = principal.get(2);
    }

    @Override
    public Set<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}


import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "oauth")
public class Oauth {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "users_id", nullable = false)
    private Users users;

    @Column(name = "oauth_type", nullable = false, length = 20)
    private String oauthType;

    @Column(name = "openid", nullable = false)
    private String openid;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "img")
public class Img {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "md5", nullable = false)
    private String md5;
}


import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "follow_user")
public class FollowUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "users_id", nullable = false)
    private Users users;

    @ManyToOne(optional = false)
    @JoinColumn(name = "followed_users_id", nullable = false)
    private Users followedUsers;

    @Column(name = "create_timestamp")
    private OffsetDateTime createTimestamp;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trigger_users_id", nullable = false)
    private Users triggerUsers;

    @ManyToOne(optional = false)
    @JoinColumn(name = "notify_users_id", nullable = false)
    private Users notifyUsers;

    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "viewed")
    private Boolean viewed = false;

    @Column(name = "create_timestamp")
    private OffsetDateTime createTimestamp;
}

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comment_users_id", nullable = false)
    private Users commentUsers;

    @ManyToOne
    @JoinColumn(name = "mention_users_id")
    private Users mentionUsers;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(name = "context", nullable = false, length = 5000)
    private String context;

    @Column(name = "visibility")
    private Boolean visibility;

    @Column(name = "create_timestamp")
    private OffsetDateTime createTimestamp;
}

import java.io.Serializable;
import java.util.UUID;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class VerificationEmailMessage implements Serializable {
    private final UUID usersId;
    private final String new_email;
}



import java.io.Serializable;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ResetPasswordEmailMessage implements Serializable {
    private final String email;
}



import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OnlineMessage {
    private String username;
    private String status;
}


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReminderMessage {
    private String sender;
    private String recipient;
    private String content;
}


import javax.persistence.Column;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "OnlineUser", timeToLive = 86400)
@Getter
@Setter
public class OnlineUser {

    @Id
    @Column(name = "id", nullable = false)
    String Id;
    @Column(name = "username", nullable = false)
    String username;
}


import javax.persistence.Column;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "JwtRouter", timeToLive = 86400)
@Getter
@Setter
public class JwtRouter {

    @Id
    @Column(name = "fakeJwt", nullable = false)
    String fakeJwt;
    @Column(name = "realJwt", nullable = false)
    String realJwt;
}

