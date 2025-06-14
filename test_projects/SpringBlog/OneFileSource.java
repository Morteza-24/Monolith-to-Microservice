
import com.raysmond.blog.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private UserService userService;

    @Bean
    public TokenBasedRememberMeServices rememberMeServices() {
        return new TokenBasedRememberMeServices("remember-me-key", userService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new StandardPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .eraseCredentials(true)
                .userDetailsService(userService)
                .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/admin/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .failureUrl("/login?error=1")
                .loginProcessingUrl("/authenticate")
                .and()
                .logout()
                .logoutUrl("/logout")
                .permitAll()
                .logoutSuccessUrl("/login?logout")
                .and()
                .rememberMe()
                .rememberMeServices(rememberMeServices())
                .key("remember-me-key");
    }
}


import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableCaching
public class CacheConfiguration {
    @Bean
    public EhCacheManagerFactoryBean ehCacheManagerFactory() {
        EhCacheManagerFactoryBean cacheManagerFactoryBean = new EhCacheManagerFactoryBean();
        cacheManagerFactoryBean.setConfigLocation(new ClassPathResource("ehcache.xml"));
        cacheManagerFactoryBean.setShared(true);
        return cacheManagerFactoryBean;
    }

    @Bean
    public EhCacheCacheManager ehCacheCacheManager() {
        EhCacheCacheManager cacheManager = new EhCacheCacheManager();
        cacheManager.setCacheManager(ehCacheManagerFactory().getObject());
        cacheManager.setTransactionAware(true);
        return cacheManager;
    }
}

import com.google.common.collect.ImmutableMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication
//@EnableCaching
public class SpringBlogApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringBlogApplication.class);
        app.setDefaultProperties(ImmutableMap.of("spring.profiles.default", Constants.ENV_DEVELOPMENT));
        app.run(args);
    }
}


import com.raysmond.blog.support.web.ViewHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.raysmond.blog.Constants.ENV_DEVELOPMENT;
import static com.raysmond.blog.Constants.ENV_PRODUCTION;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Raysmond .
 */
@Configuration
@Slf4j
public class WebConfig extends WebMvcConfigurerAdapter {
    @Autowired
    private ViewHelper viewHelper;

    @Autowired
    private Environment env;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(viewObjectAddingInterceptor());
        super.addInterceptors(registry);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (env.acceptsProfiles(ENV_DEVELOPMENT)) {
            log.debug("Register CORS configuration");
            registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:8080")
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }

    @PostConstruct
    public void registerJadeViewHelpers() {
        viewHelper.setApplicationEnv(this.getApplicationEnv());
    }

    @Bean
    public HandlerInterceptor viewObjectAddingInterceptor() {
        return new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                viewHelper.setStartTime(System.currentTimeMillis());

                return true;
            }

            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                                   ModelAndView view) {
                CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (token != null) {
                    view.addObject(token.getParameterName(), token);
                }
            }
        };
    }

    public String getApplicationEnv() {
        return this.env.acceptsProfiles(ENV_PRODUCTION) ? ENV_PRODUCTION : ENV_DEVELOPMENT;
    }
}


/**
 * @author: Raysmond
 */
public final class Constants {

    public static final String ENV_PRODUCTION = "prod";

    public static final String ENV_DEVELOPMENT = "dev";

    public static final String DEFAULT_ADMIN_EMAIL = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "admin";

    public static final String ABOUT_PAGE_PERMALINK = "about";

}


import com.raysmond.blog.error.NotFoundException;
import com.raysmond.blog.models.Post;
import com.raysmond.blog.models.Tag;
import com.raysmond.blog.services.AppSetting;
import com.raysmond.blog.services.PostService;
import com.raysmond.blog.services.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Raysmond
 */
@Controller
@RequestMapping("tags")
public class TagController {
    @Autowired
    private TagService tagService;

    @Autowired
    private PostService postService;

    @Autowired
    private AppSetting appSetting;

    @RequestMapping(value = "", method = GET)
    public String index(Model model) {
        model.addAttribute("tags", postService.countPostsByTags());
        return "tags/index";
    }

    @RequestMapping(value = "{tagName}", method = GET)
    public String showTag(@PathVariable String tagName, @RequestParam(defaultValue = "1") int page, Model model) {
        Tag tag = tagService.getTag(tagName);

        if (tag == null) {
            throw new NotFoundException("Tag " + tagName + " is not found.");
        }

        page = page < 1 ? 0 : page - 1;
        Page<Post> posts = postService.findPostsByTag(tagName, page, appSetting.getPageSize());

        model.addAttribute("tag", tag);
        model.addAttribute("posts", posts);
        model.addAttribute("page", page + 1);
        model.addAttribute("totalPages", posts.getTotalPages());

        return "tags/show";
    }
}


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * User pages
 *
 * @author Raysmond
 */
@Controller
public class UserController {

    @RequestMapping("login")
    public String signin(Principal principal, RedirectAttributes ra) {
        return principal == null ? "users/login" : "redirect:/";
    }
}

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.raysmond.blog.error.NotFoundException;
import com.raysmond.blog.models.Post;
import com.raysmond.blog.models.support.PostType;
import com.raysmond.blog.services.PostService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;
import java.util.Map;

/**
 * @author Raysmond
 */
@Controller
public class PostController {
    @Autowired
    private PostService postService;

    @GetMapping(value = "posts/archive")
    public String archive(Model model) {
        Map<Integer, List<Post>> posts = Maps.newHashMap();
        postService.getArchivePosts().forEach(post -> {
            if (!posts.containsKey(post.getCreatedAt().getYear())) {
                posts.put(post.getCreatedAt().getYear(), Lists.newArrayList());
            }
            posts.get(post.getCreatedAt().getYear()).add(post);
        });
        model.addAttribute("posts", posts);
        return "posts/archive";
    }

    @GetMapping(value = "posts/{permalink}")
    public String show(@PathVariable String permalink, Model model) {
        return showPost(permalink, model, PostType.POST);
    }

    @GetMapping(value = "{permalink}")
    public String page(@PathVariable String permalink, Model model) {
        return showPost(permalink, model, PostType.PAGE);
    }

    private String showPost(String permalink, Model model, PostType postType) {
        Post post;

        try {
            post = postService.getPublishedPostByPermalink(permalink);
        } catch (NotFoundException ex) {
            if (permalink.matches("\\d+") && postType.equals(PostType.POST)) {
                post = postService.getPost(Long.valueOf(permalink));
            } else {
                throw new NotFoundException();
            }
        }

        if (!post.getPostType().equals(postType)) {
            throw new NotFoundException();
        }

        postService.incrementViews(post.getId());

        model.addAttribute("postType", postType.name());
        model.addAttribute("post", post);
        model.addAttribute("tags", postService.getPostTags(post));
        return "posts/post";
    }

}


import com.raysmond.blog.models.Post;
import com.raysmond.blog.services.AppSetting;
import com.raysmond.blog.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @Autowired
    private PostService postService;

    @Autowired
    private AppSetting appSetting;

    @GetMapping(value = "")
    public String index(@RequestParam(defaultValue = "1") int page, Model model) {
        page = page < 1 ? 0 : page - 1;
        Page<Post> posts = postService.getAllPublishedPostsByPage(page, appSetting.getPageSize());

        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("posts", posts);
        model.addAttribute("page", page + 1);
        return "home/home";
    }
}


import com.raysmond.blog.support.web.MarkdownService;
import com.raysmond.blog.support.web.impl.PegDownMarkdownService;

/**
 * A Markdown processing util class
 *
 * @author Raysmond
 */
public class Markdown {

    private static final MarkdownService MARKDOWN_SERVICE = new PegDownMarkdownService();

    public static String markdownToHtml(String content) {
        return MARKDOWN_SERVICE.renderToHtml(content);
    }
}


import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Raysmond
 */
public class DTOUtil {

    private static ModelMapper MAPPER = null;

    private static ModelMapper getMapper() {
        if (MAPPER == null) {
            MAPPER = new ModelMapper();
            MAPPER.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        }

        return MAPPER;
    }

    public static <S, T> T map(S source, Class<T> targetClass) {
        return getMapper().map(source, targetClass);
    }

    public static <S, T> void mapTo(S source, T dist) {
        getMapper().map(source, dist);
    }

    public static <S, T> List<T> mapList(List<S> source, Class<T> targetClass) {
        List<T> list = new ArrayList<>();
        for (S s : source) {
            list.add(getMapper().map(s, targetClass));
        }
        return list;
    }
}


import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * General error handler for the application.
 */
@ControllerAdvice
@Slf4j
class ExceptionHandlerController {
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(HttpServletRequest request, NotFoundException exception) {
        String uri = request.getRequestURI();

        log.error("Request page: {} raised NotFoundException {}", uri, exception);

        ModelAndView model = new ModelAndView("error/general");
        model.addObject("status", HttpStatus.NOT_FOUND.value());
        model.addObject("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        model.addObject("path", uri);
        model.addObject("customMessage", exception.getMessage());

        return model;
    }

    /**
     * Handle all exceptions
     */
//	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(Exception.class)
    public ModelAndView exception(HttpServletRequest request, Exception exception) {
        String uri = request.getRequestURI();
        log.error("Request page: {} raised exception {}", uri, exception);

        ModelAndView model = new ModelAndView("error/general");
        model.addObject("error", Throwables.getRootCause(exception).getMessage());
        model.addObject("status", Throwables.getRootCause(exception).getCause());
        model.addObject("path", uri);
        model.addObject("customMessage", exception.getMessage());

        return model;
    }
}

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Raysmond
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public final class NotFoundException extends RuntimeException {
    private String message;

    public NotFoundException() {

    }

    public NotFoundException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

import com.raysmond.blog.models.Post;
import com.raysmond.blog.models.support.PostStatus;
import com.raysmond.blog.models.support.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Raysmond
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Post findByPermalinkAndPostStatus(String permalink, PostStatus postStatus);

    Page<Post> findAllByPostType(PostType postType, Pageable pageRequest);

    Page<Post> findAllByPostTypeAndPostStatus(PostType postType, PostStatus postStatus, Pageable pageRequest);

    @Query("SELECT p FROM Post p INNER JOIN p.tags t WHERE t.name = :tag")
    Page<Post> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT t.name, count(p) as tag_count from Post p " +
            "INNER JOIN p.tags t " +
            "WHERE p.postStatus = :status " +
            "GROUP BY t.id " +
            "ORDER BY tag_count DESC")
    List<Object[]> countPostsByTags(@Param("status") PostStatus status);
}



import com.raysmond.blog.models.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raysmond
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
    Setting findByKey(String key);
}


import com.raysmond.blog.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Raysmond
 */
public interface TagRepository extends JpaRepository<Tag, Long> {
    Tag findByName(String name);
}


import com.raysmond.blog.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Raysmond
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}


import com.raysmond.blog.models.support.PostFormat;
import com.raysmond.blog.models.support.PostStatus;
import com.raysmond.blog.models.support.PostType;

import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * @author Raysmond
 */
@Data
public class PostForm {
    @NotEmpty
    private String title;

    @NotEmpty
    private String content;

    private String summary;

    @NotNull
    private PostFormat postFormat;

    @NotNull
    private PostStatus postStatus;

    @NotNull
    private String permalink;

    @NotNull
    private String postTags;

    @NotNull
    private PostType postType;

}


import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Raysmond
 */
@Data
public class UserForm {
    @NotNull
    private String password;

    @NotNull
    private String newPassword;
}


import lombok.Data;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * @author Raysmond
 */
@Data
public class SettingsForm {
    @NotEmpty
    @NotNull
    private String siteName;

    @NotNull
    private String siteSlogan;

    @NotNull
    private Integer pageSize;

    private String intro;

    private String pictureUrl;

}


import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * @author Raysmond
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "userCache")
public class User extends BaseModel {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    private String role = ROLE_USER;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
    private Collection<Post> posts = new ArrayList<>();

    public User() {

    }

    public User(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }
}


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.io.Serializable;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A generic setting model
 *
 * @author Raysmond
 */
@Entity
@Table(name = "settings")
@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "settingCache")
public class Setting extends BaseModel {
    @Column(name = "_key", unique = true, nullable = false)
    private String key;

    @Lob
    @Column(name = "_value")
    private Serializable value;

}


import com.raysmond.blog.models.support.PostFormat;
import com.raysmond.blog.models.support.PostStatus;
import com.raysmond.blog.models.support.PostType;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.springframework.util.StringUtils;

import javax.persistence.*;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Raysmond
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "postCache")
public class Post extends BaseModel {
    private static final SimpleDateFormat SLUG_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    @ManyToOne
    private User user;

    @Column(nullable = false)
    private String title;

    @Type(type = "text")
    private String content;

    @Type(type = "text")
    private String renderedContent;

    @Type(type = "text")
    private String summary;

    @Type(type = "text")
    private String renderedSummary;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostStatus postStatus = PostStatus.PUBLISHED;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostFormat postFormat = PostFormat.MARKDOWN;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PostType postType = PostType.POST;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "posts_tags",
            joinColumns = {@JoinColumn(name = "post_id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "tag_id", nullable = false, updatable = false)}
    )
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "tagCache")
    private Set<Tag> tags = new HashSet<>();

    private String permalink;

    private Integer views = 0;

    public Integer getViews() {
        return views == null ? 0 : views;
    }

    public String getRenderedContent() {
        if (this.postFormat == PostFormat.MARKDOWN) {
            return renderedContent;
        }

        return getContent();
    }

    public void setPermalink(String permalink) {
        String token = permalink.toLowerCase().replace("\n", " ").replaceAll("[^a-z\\d\\s]", " ");
        this.permalink = StringUtils.arrayToDelimitedString(StringUtils.tokenizeToStringArray(token, " "), "-");
    }
}


import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * An abstract base model class for entities
 *
 * @author Raysmond
 */
@MappedSuperclass
public abstract class BaseModel implements Comparable<BaseModel>, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    @Column(nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    @Override
    public int compareTo(BaseModel o) {
        return this.getId().compareTo(o.getId());
    }

    public boolean equals(Object other) {
        if (other == null || other.getClass() != this.getClass()) {
            return false;
        }

        return this.getId().equals(((BaseModel) other).getId());
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long _id) {
        id = _id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * @author Raysmond
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "tagCache")
public class Tag extends BaseModel {

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "tags")
    private List<Post> posts = new ArrayList<>();

    public Tag() {

    }

    public Tag(String name) {
        this.setName(name);
    }
}


/**
 * @author Raysmond
 */
public enum PostType {
    PAGE("Page"),
    POST("Post");

    private String name;

    PostType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return name();
    }

    @Override
    public String toString() {
        return getName();
    }
}


/**
 * @author Raysmond
 */
public enum PostFormat {
    HTML("Html"),
    MARKDOWN("Markdown");

    private String displayName;

    PostFormat(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return name();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}

/**
 * @author Raysmond
 */
public enum PostStatus {
    DRAFT("Draft"),
    PUBLISHED("Published");

    private String name;

    PostStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return name();
    }

    @Override
    public String toString() {
        return getName();
    }
}


import com.raysmond.blog.error.NotFoundException;
import com.raysmond.blog.forms.UserForm;
import com.raysmond.blog.models.User;
import com.raysmond.blog.repositories.UserRepository;
import com.raysmond.blog.services.UserService;
import com.raysmond.blog.support.web.MessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Raysmond
 */
@Controller("adminUserController")
@RequestMapping("admin/users")
public class UserController {

    private UserService userService;
    private UserRepository userRepository;

    @Autowired
    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @RequestMapping("profile")
    public String profile(Model model) {
        model.addAttribute("user", userService.currentUser());

        return "admin/users/profile";
    }

    @RequestMapping(value = "{userId:[0-9]+}", method = POST)
    public String update(@PathVariable Long userId, @Valid UserForm userForm, Errors errors, RedirectAttributes ra) {
        User user = userRepository.findOne(userId);

        if (null == user) {
            throw new NotFoundException("User " + userId + " is not found.");
        }

        if (errors.hasErrors()) {
            // do something

            return "admin/users/profile";
        }

        if (!userForm.getNewPassword().isEmpty()) {

            if (!userService.changePassword(user, userForm.getPassword(), userForm.getNewPassword()))
                MessageHelper.addErrorAttribute(ra, "Change password failed.");
            else
                MessageHelper.addSuccessAttribute(ra, "Change password successfully.");

        }

        return "redirect:profile";
    }
}


import com.raysmond.blog.forms.SettingsForm;
import com.raysmond.blog.services.AppSetting;
import com.raysmond.blog.support.web.MessageHelper;
import com.raysmond.blog.utils.DTOUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * @author Raysmond
 */
@Controller
@RequestMapping("admin")
public class AdminController {

    private AppSetting appSetting;

    @Autowired
    public AdminController(AppSetting appSetting) {
        this.appSetting = appSetting;
    }

    @GetMapping("")
    public String index() {
        return "admin/home/index";
    }

    @GetMapping(value = "settings")
    public String settings(Model model) {
        SettingsForm settingsForm = DTOUtil.map(appSetting, SettingsForm.class);

        model.addAttribute("settings", settingsForm);
        return "admin/home/settings";
    }

    @PostMapping(value = "settings")
    public String updateSettings(@Valid SettingsForm settingsForm, Errors errors, RedirectAttributes ra) {
        if (errors.hasErrors()) {
            return "admin/settings";
        } else {
            appSetting.setSiteName(settingsForm.getSiteName());
            appSetting.setSiteSlogan(settingsForm.getSiteSlogan());
            appSetting.setPageSize(settingsForm.getPageSize());

            MessageHelper.addSuccessAttribute(ra, "Update settings successfully.");

            return "redirect:settings";
        }
    }
}


import com.raysmond.blog.forms.PostForm;
import com.raysmond.blog.models.Post;
import com.raysmond.blog.models.support.PostFormat;
import com.raysmond.blog.models.support.PostStatus;
import com.raysmond.blog.models.support.PostType;
import com.raysmond.blog.repositories.PostRepository;
import com.raysmond.blog.repositories.UserRepository;
import com.raysmond.blog.services.PostService;
import com.raysmond.blog.utils.DTOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.security.Principal;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * @author Raysmond
 */
@Controller("adminPostController")
@RequestMapping("admin/posts")
public class PostController {

    private static final int PAGE_SIZE = 20;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostService postService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Post> posts = postRepository.findAll(new PageRequest(page, PAGE_SIZE, Sort.Direction.DESC, "id"));

        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("page", page);
        model.addAttribute("posts", posts);

        return "admin/posts/index";
    }

    @GetMapping("new")
    public String newPost(Model model) {
        PostForm postForm = DTOUtil.map(new Post(), PostForm.class);
        postForm.setPostTags("");

        model.addAttribute("postForm", postForm);
        model.addAttribute("postFormats", PostFormat.values());
        model.addAttribute("postTypes", PostType.values());
        model.addAttribute("postStatus", PostStatus.values());

        return "admin/posts/new";
    }

    @RequestMapping(value = "{postId:[0-9]+}/edit")
    public String editPost(@PathVariable Long postId, Model model) {
        Post post = postRepository.findOne(postId);
        PostForm postForm = DTOUtil.map(post, PostForm.class);

        postForm.setPostTags(postService.getTagNames(post.getTags()));

        model.addAttribute("post", post);
        model.addAttribute("postForm", postForm);
        model.addAttribute("postFormats", PostFormat.values());
        model.addAttribute("postTypes", PostType.values());
        model.addAttribute("postStatus", PostStatus.values());

        return "admin/posts/edit";
    }

    @RequestMapping(value = "{postId:[0-9]+}/delete", method = {DELETE, POST})
    public String deletePost(@PathVariable Long postId) {
        postService.deletePost(postRepository.findOne(postId));
        return "redirect:/admin/posts";
    }

    @RequestMapping(value = "", method = POST)
    public String create(Principal principal, @Valid PostForm postForm, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("postFormats", PostFormat.values());
            model.addAttribute("postStatus", PostStatus.values());

            return "admin/posts/new";
        } else {
            Post post = DTOUtil.map(postForm, Post.class);
            post.setUser(userRepository.findByEmail(principal.getName()));
            post.setTags(postService.parseTagNames(postForm.getPostTags()));

            postService.createPost(post);

            return "redirect:/admin/posts";
        }
    }

    @RequestMapping(value = "{postId:[0-9]+}", method = {PUT, POST})
    public String update(@PathVariable Long postId, @Valid PostForm postForm, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("postFormats", PostFormat.values());
            model.addAttribute("postStatus", PostStatus.values());

            return "admin/posts_edit";
        } else {
            Post post = postRepository.findOne(postId);
            DTOUtil.mapTo(postForm, post);
            post.setTags(postService.parseTagNames(postForm.getPostTags()));

            postService.updatePost(post);

            return "redirect:/admin/posts";
        }
    }

}


import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.raysmond.blog.support.web.Message.MESSAGE_ATTRIBUTE;

public final class MessageHelper {

    private MessageHelper() {

    }

    public static void addSuccessAttribute(RedirectAttributes ra, String message, Object... args) {
        addAttribute(ra, message, Message.Type.SUCCESS, args);
    }

    public static void addErrorAttribute(RedirectAttributes ra, String message, Object... args) {
        addAttribute(ra, message, Message.Type.DANGER, args);
    }

    public static void addInfoAttribute(RedirectAttributes ra, String message, Object... args) {
        addAttribute(ra, message, Message.Type.INFO, args);
    }

    public static void addWarningAttribute(RedirectAttributes ra, String message, Object... args) {
        addAttribute(ra, message, Message.Type.WARNING, args);
    }

    private static void addAttribute(RedirectAttributes ra, String message, Message.Type type, Object... args) {
        ra.addFlashAttribute(MESSAGE_ATTRIBUTE, new Message(message, type, args));
    }

    public static void addSuccessAttribute(Model model, String message, Object... args) {
        addAttribute(model, message, Message.Type.SUCCESS, args);
    }

    public static void addErrorAttribute(Model model, String message, Object... args) {
        addAttribute(model, message, Message.Type.DANGER, args);
    }

    public static void addInfoAttribute(Model model, String message, Object... args) {
        addAttribute(model, message, Message.Type.INFO, args);
    }

    public static void addWarningAttribute(Model model, String message, Object... args) {
        addAttribute(model, message, Message.Type.WARNING, args);
    }

    private static void addAttribute(Model model, String message, Message.Type type, Object... args) {
        model.addAttribute(MESSAGE_ATTRIBUTE, new Message(message, type, args));
    }
}


import org.pegdown.Printer;
import org.pegdown.VerbatimSerializer;
import org.pegdown.ast.VerbatimNode;

/**
 * @author Raysmond
 */
public class PygmentsVerbatimSerializer implements VerbatimSerializer {
    public static final PygmentsVerbatimSerializer INSTANCE = new PygmentsVerbatimSerializer();

    private SyntaxHighlightService syntaxHighlightService = new PygmentsService();

    @Override
    public void serialize(final VerbatimNode node, final Printer printer) {
        printer.print(syntaxHighlightService.highlight(node.getText()));
    }

}

/**
 * @author Raysmond
 */
public interface MarkdownService {
    String renderToHtml(String content);
}


/**
 * A message to be displayed in web context. Depending on the type, different style will be applied.
 */
public class Message implements java.io.Serializable {
    /**
     * Name of the flash attribute.
     */
    public static final String MESSAGE_ATTRIBUTE = "message";
    private final String message;
    private final Type type;
    private final Object[] args;
    public Message(String message, Type type) {
        this.message = message;
        this.type = type;
        this.args = null;
    }

    public Message(String message, Type type, Object... args) {
        this.message = message;
        this.type = type;
        this.args = args;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public Object[] getArgs() {
        return args;
    }

    /**
     * The type of the message to be displayed. The type is used to show message in a different style.
     */
    public static enum Type {
        DANGER, WARNING, INFO, SUCCESS;
    }
}


import com.domingosuarez.boot.autoconfigure.jade4j.JadeHelper;
import com.raysmond.blog.services.AppSetting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author Raysmond
 */
@Service
@JadeHelper("viewHelper")
public class ViewHelper {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");
    private static final SimpleDateFormat DATE_FORMAT_MONTH_DAY = new SimpleDateFormat("MMM dd");

    private AppSetting appSetting;

    private String applicationEnv;
    private long startTime;

    /**
     * Check if current user is authenticated
     *
     * @return true/false
     */
    public boolean isLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return null != authentication
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @Autowired
    public ViewHelper(AppSetting appSetting) {
        this.appSetting = appSetting;
    }

    public long getResponseTime() {
        return System.currentTimeMillis() - startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getFormattedDate(Date date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    public String getFormattedDate(ZonedDateTime date) {
        return date == null ? "" : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public String getMonthAndDay(Date date) {
        return date == null ? "" : DATE_FORMAT_MONTH_DAY.format(date);
    }

    public String metaTitle(String title) {
        return title + " · " + appSetting.getSiteName();
    }

    public String getApplicationEnv() {
        return applicationEnv;
    }

    public void setApplicationEnv(String applicationEnv) {
        this.applicationEnv = applicationEnv;
    }
}


import org.python.util.PythonInterpreter;
import org.springframework.stereotype.Service;

/**
 * @author Raysmond
 */
@Service
public class PygmentsService implements SyntaxHighlightService {

    @Override
    public String highlight(String content) {
        PythonInterpreter interpreter = new PythonInterpreter();

        // Set a variable with the content you want to work with
        interpreter.set("code", content);

        // Simple use Pygments as you would in Python
        interpreter.exec("from pygments import highlight\n"
                + "from pygments.lexers import PythonLexer\n"
                + "from pygments.formatters import HtmlFormatter\n"
                + "\nresult = highlight(code, PythonLexer(), HtmlFormatter())");

        return interpreter.get("result", String.class);
    }
}


/**
 * @author Raysmond
 */
public interface SyntaxHighlightService {
    String highlight(String content);
}


import java.util.Arrays;

import org.springframework.stereotype.Service;

import com.raysmond.blog.support.web.MarkdownService;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import lombok.extern.slf4j.Slf4j;

/**
 * 使用flexmark-java解析markdown
 * 参考：https://github.com/vsch/flexmark-java
 *
 * @author Raysmond
 */
@Service("flexmark")
@Slf4j
public class FlexmarkMarkdownService implements MarkdownService {
    @Override
    public String renderToHtml(String content) {
        MutableDataSet options = new MutableDataSet();

        options.set(Parser.EXTENSIONS,
                Arrays.asList(TablesExtension.create(),
                        AutolinkExtension.create(),
                        StrikethroughExtension.create()));

        // uncomment to convert soft-breaks to hard breaks
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // You can re-use parser and renderer instances
        Node document = parser.parse(content);
        return renderer.render(document);
    }
}


import org.pegdown.*;
import org.pegdown.ast.RootNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

import com.raysmond.blog.support.web.MarkdownService;

/**
 * @author Raysmond
 */
@Service
@Qualifier("pegdown")
public class PegDownMarkdownService implements MarkdownService {

    private final PegDownProcessor pegdown;

    public PegDownMarkdownService() {
        pegdown = new PegDownProcessor(Extensions.ALL ^ Extensions.EXTANCHORLINKS);
    }

    @Override
    public String renderToHtml(String markdownSource) {
        if (StringUtils.isEmpty(markdownSource)) {
            return null;
        }
        // synchronizing on pegdown instance since neither the processor nor the underlying parser is thread-safe.
        synchronized(pegdown) {
            RootNode astRoot = pegdown.parseMarkdown(markdownSource.toCharArray());
            ToHtmlSerializer serializer = new ToHtmlSerializer(new LinkRenderer());
            // Collections.singletonMap(VerbatimSerializer.DEFAULT, PygmentsVerbatimSerializer.INSTANCE));
            return serializer.toHtml(astRoot);
        }
    }
}


import com.raysmond.blog.Constants;
import com.raysmond.blog.error.NotFoundException;
import com.raysmond.blog.models.Post;
import com.raysmond.blog.models.Tag;
import com.raysmond.blog.models.support.PostFormat;
import com.raysmond.blog.models.support.PostStatus;
import com.raysmond.blog.models.support.PostType;
import com.raysmond.blog.repositories.PostRepository;
import com.raysmond.blog.support.web.MarkdownService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Raysmond
 */
@Service
@Slf4j
@Transactional
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("flexmark")
    private MarkdownService markdownService;

    public Post getPost(Long postId) {
        log.debug("Get post " + postId);

        Post post = postRepository.findOne(postId);

        if (post == null) {
            throw new NotFoundException("Post with id " + postId + " is not found.");
        }

        return post;
    }

    public Post getPublishedPostByPermalink(String permalink) {
        log.debug("Get post with permalink " + permalink);

        Post post = postRepository.findByPermalinkAndPostStatus(permalink, PostStatus.PUBLISHED);

        if (post == null) {
            try {
                post = postRepository.findOne(Long.valueOf(permalink));
            } catch (NumberFormatException e) {
                post = null;
            }
        }

        if (post == null) {
            throw new NotFoundException("Post with permalink '" + permalink + "' is not found.");
        }

        return post;
    }

    public Post createPost(Post post) {
        if (post.getPostFormat() == PostFormat.MARKDOWN) {
            post.setRenderedContent(markdownService.renderToHtml(post.getContent()));
            post.setRenderedSummary(markdownService.renderToHtml(post.getSummary()));
        }

        return postRepository.save(post);
    }

    public Post updatePost(Post post) {
        if (post.getPostFormat() == PostFormat.MARKDOWN) {
            post.setRenderedContent(markdownService.renderToHtml(post.getContent()));
            post.setRenderedSummary(markdownService.renderToHtml(post.getSummary()));
        }

        return postRepository.save(post);
    }

    public void deletePost(Post post) {
        postRepository.delete(post);
    }

    public List<Post> getArchivePosts() {
        log.debug("Get all archive posts from database.");

        Pageable page = new PageRequest(0, Integer.MAX_VALUE, Sort.Direction.DESC, "createdAt");
        return postRepository.findAllByPostTypeAndPostStatus(PostType.POST, PostStatus.PUBLISHED, page)
                .getContent()
                .stream()
                .map(this::extractPostMeta)
                .collect(Collectors.toList());
    }

    public List<Tag> getPostTags(Post post) {
        log.debug("Get tags of post {}", post.getId());

        List<Tag> tags = new ArrayList<>();

        // Load the post first. If not, when the post is cached before while the tags not,
        // then the LAZY loading of post tags will cause an initialization error because
        // of not hibernate connection session
        postRepository.findOne(post.getId()).getTags().forEach(tags::add);
        return tags;
    }

    private Post extractPostMeta(Post post) {
        Post archivePost = new Post();
        archivePost.setId(post.getId());
        archivePost.setTitle(post.getTitle());
        archivePost.setPermalink(post.getPermalink());
        archivePost.setCreatedAt(post.getCreatedAt());

        return archivePost;
    }

    public Page<Post> getAllPublishedPostsByPage(int page, int pageSize) {
        log.debug("Get posts by page " + page);

        return postRepository.findAllByPostTypeAndPostStatus(
                PostType.POST,
                PostStatus.PUBLISHED,
                new PageRequest(page, pageSize, Sort.Direction.DESC, "createdAt"));
    }

    public Post createAboutPage() {
        log.debug("Create default about page");

        Post post = new Post();
        post.setTitle(Constants.ABOUT_PAGE_PERMALINK);
        post.setContent(Constants.ABOUT_PAGE_PERMALINK.toLowerCase());
        post.setPermalink(Constants.ABOUT_PAGE_PERMALINK);
        post.setUser(userService.getSuperUser());
        post.setPostFormat(PostFormat.MARKDOWN);

        return createPost(post);
    }

    public Set<Tag> parseTagNames(String tagNames) {
        Set<Tag> tags = new HashSet<>();

        if (tagNames != null && !tagNames.isEmpty()) {
            tagNames = tagNames.toLowerCase();
            String[] names = tagNames.split("\\s*,\\s*");
            for (String name : names) {
                tags.add(tagService.findOrCreateByName(name));
            }
        }

        return tags;
    }

    public String getTagNames(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }

        StringBuilder names = new StringBuilder();
        tags.forEach(tag -> names.append(tag.getName()).append(","));
        names.deleteCharAt(names.length() - 1);

        return names.toString();
    }

    // cache or not?
    public Page<Post> findPostsByTag(String tagName, int page, int pageSize) {
        return postRepository.findByTag(tagName, new PageRequest(page, pageSize, Sort.Direction.DESC, "createdAt"));
    }

    public List<Object[]> countPostsByTags() {
        log.debug("Count posts group by tags.");

        return postRepository.countPostsByTags(PostStatus.PUBLISHED);
    }

    @Async
    public void incrementViews(Long postId) {
        synchronized(this) {
            Post post = postRepository.findOne(postId);
            post.setViews(post.getViews() + 1);
            postRepository.save(post);
        }
    }
}


import com.raysmond.blog.models.Setting;
import com.raysmond.blog.repositories.SettingRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * @author Raysmond
 */
@Service
@Slf4j
@Transactional
public class CacheSettingService implements SettingService {
    private SettingRepository settingRepository;

    @Autowired
    public CacheSettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    @Cacheable(value = "settingCache", key = "#key")
    public Serializable get(String key) {
        Setting setting = settingRepository.findByKey(key);
        Serializable value = null;
        try {
            value = setting == null ? null : setting.getValue();
        } catch (Exception ex) {
            log.info("Cannot deserialize setting value with key = " + key);
        }

        log.info("Get setting " + key + " from database. Value = " + value);

        return value;
    }

    @Override
    @Cacheable(value = "settingCache", key = "#key")
    public Serializable get(String key, Serializable defaultValue) {
        Serializable value = get(key);
        return value == null ? defaultValue : value;
    }

    @Override
    @CacheEvict(value = "settingCache", key = "#key")
    public void put(String key, Serializable value) {
        log.info("Update setting " + key + " to database. Value = " + value);

        Setting setting = settingRepository.findByKey(key);
        if (setting == null) {
            setting = new Setting();
            setting.setKey(key);
        }
        try {
            setting.setValue(value);
            settingRepository.save(setting);
        } catch (Exception ex) {

            log.info("Cannot save setting value with type: " + value.getClass() + ". key = " + key);
        }
    }
}


import com.raysmond.blog.models.Tag;
import com.raysmond.blog.repositories.TagRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Raysmond
 */
@Service
public class TagService {
    private TagRepository tagRepository;

    @Autowired
    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Tag findOrCreateByName(String name) {
        Tag tag = tagRepository.findByName(name);
        if (tag == null) {
            tag = tagRepository.save(new Tag(name));
        }
        return tag;
    }

    public Tag getTag(String tagName) {
        return tagRepository.findByName(tagName);
    }

    public void deleteTag(Tag tag) {
        tagRepository.delete(tag);
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

}


import com.raysmond.blog.Constants;
import com.raysmond.blog.models.User;
import com.raysmond.blog.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Collections;

@Transactional
@Service
@Slf4j
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    protected void initialize() {
        getSuperUser();
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User getSuperUser() {
        User user = userRepository.findByEmail(Constants.DEFAULT_ADMIN_EMAIL);

        if (user == null) {
            user = createUser(new User(Constants.DEFAULT_ADMIN_EMAIL, Constants.DEFAULT_ADMIN_PASSWORD, User.ROLE_ADMIN));
        }

        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("user not found");
        }
        return createSpringUser(user);
    }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String email = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();

        return userRepository.findByEmail(email);
    }

    public boolean changePassword(User user, String password, String newPassword) {
        if (password == null || newPassword == null || password.isEmpty() || newPassword.isEmpty())
            return false;

        boolean match = passwordEncoder.matches(password, user.getPassword());
        if (!match)
            return false;

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("User @{} changed password.", user.getEmail());

        return true;
    }

    public void signin(User user) {
        SecurityContextHolder.getContext().setAuthentication(authenticate(user));
    }

    private Authentication authenticate(User user) {
        return new UsernamePasswordAuthenticationToken(createSpringUser(user), null, Collections.singleton(createAuthority(user)));
    }

    private org.springframework.security.core.userdetails.User createSpringUser(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singleton(createAuthority(user)));
    }

    private GrantedAuthority createAuthority(User user) {
        return new SimpleGrantedAuthority(user.getRole());
    }

}


import java.io.Serializable;

/**
 * @author Raysmond
 */
public interface SettingService {
    Serializable get(String key);

    Serializable get(String key, Serializable defaultValue);

    void put(String key, Serializable value);
}


import com.domingosuarez.boot.autoconfigure.jade4j.JadeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Raysmond
 */
@JadeHelper("App")
@Service
public class AppSetting {

    public static final String SITE_NAME = "site_name";
    public static final String SITE_SLOGAN = "site_slogan";
    public static final String PAGE_SIZE = "page_size";

    @Autowired
    private SettingService settingService;

    private String siteName = "SpringBlog";
    private String siteSlogan = "An interesting place to discover";
    private Integer pageSize = 5;

    public String getSiteName() {
        return (String) settingService.get(SITE_NAME, siteName);
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
        settingService.put(SITE_NAME, siteName);
    }

    public Integer getPageSize() {
        return (Integer) settingService.get(PAGE_SIZE, pageSize);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        settingService.put(PAGE_SIZE, pageSize);
    }

    public String getSiteSlogan() {
        return (String) settingService.get(SITE_SLOGAN, siteSlogan);
    }

    public void setSiteSlogan(String siteSlogan) {
        this.siteSlogan = siteSlogan;
        settingService.put(SITE_SLOGAN, siteSlogan);
    }
}

