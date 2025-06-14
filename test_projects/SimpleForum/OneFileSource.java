
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}


import com.github.dawidstankiewicz.forum.config.Routes;
import com.github.dawidstankiewicz.forum.model.dto.UserRegistrationForm;
import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class HelloController {

    private final UserService userService;

    public HelloController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = {"/hello", "/welcome"})
    public String hello() {
        return "hello";
    }

    @PostMapping(value = "/hello")
    public String loginOrRegister(@ModelAttribute("email") String email,
                                  RedirectAttributes model) {
        User user = userService.findByEmail(email);
        model.addFlashAttribute("email", email);

        if (user == null) {
            model.addFlashAttribute("userRegistrationForm", new UserRegistrationForm());
            return Routes.REDIRECT + Routes.NEW_USER_FORM_PAGE;
        }
        model.addFlashAttribute("username", user.getUsername());
        return Routes.REDIRECT + Routes.LOGIN_PAGE;
    }
}


import com.github.dawidstankiewicz.forum.post.PostService;
import com.github.dawidstankiewicz.forum.section.SectionService;
import com.github.dawidstankiewicz.forum.topic.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class HomeController {

    private final SectionService sectionService;
    private final TopicService topicService;
    private final PostService postService;

    public HomeController(SectionService sectionService, TopicService topicService, PostService postService) {
        this.sectionService = sectionService;
        this.topicService = topicService;
        this.postService = postService;
    }

    @RequestMapping(value = {"/",
            "/home"})
    public String home(Model model) {
        model.addAttribute("sections", sectionService.findAll());
        model.addAttribute("topics", topicService.findRecent());
        model.addAttribute("posts", postService.findRecent());
        return "home";
    }

}


import com.github.dawidstankiewicz.forum.exception.ResourceNotFoundException;
import com.github.dawidstankiewicz.forum.model.entity.Post;
import com.github.dawidstankiewicz.forum.model.entity.Topic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post findOneOrExit(int id) {
        Optional<Post> postOptional = postRepository.findById(id);
        if (!postOptional.isPresent()) {
            throw new ResourceNotFoundException();
        }
        return postOptional.get();
    }

    public Set<Post> findRecent() {
        return postRepository.findTop5ByOrderByCreationDateDesc();
    }

    public List<Post> findByTopic(Topic topic) {
        return postRepository.findByTopicOrderByCreationDate(topic);
    }

    public void save(Post post) {
        postRepository.save(post);
    }

    public void delete(Post post) {
        postRepository.delete(post);
    }

}


import com.github.dawidstankiewicz.forum.model.entity.Post;
import com.github.dawidstankiewicz.forum.model.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;


public interface PostRepository extends JpaRepository<Post, Integer> {

    List<Post> findByTopicOrderByCreationDate(Topic topic);

    Set<Post> findTop5ByOrderByCreationDateDesc();
}


import com.github.dawidstankiewicz.forum.model.entity.Post;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping(value = "/post")
public class PostController {
    
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public String delete(@PathVariable int id,
                         Authentication authentication,
                         RedirectAttributes model) {
        Post post = postService.findOneOrExit(id);
        if (post == null || authentication == null || authentication.getName() == null
                || !authentication.getName().equals(post.getUser().getEmail())) {
            return "redirect:/";
        }
        
        postService.delete(post);
        
        model.addFlashAttribute("message", "post.successfully.deleted");
        return "redirect:/topics/" + post.getTopic().getId();
    }
}


import com.github.dawidstankiewicz.forum.exception.ResourceNotFoundException;
import com.github.dawidstankiewicz.forum.model.entity.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Slf4j
public class PathTopicArgumentResolver implements HandlerMethodArgumentResolver {

    private final TopicRepository topicRepository;

    public PathTopicArgumentResolver(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(PathTopic.class) != null &&
                parameter.getParameterType().equals(Topic.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        @SuppressWarnings("unchecked") Map<String, String> pathVariables = (Map<String, String>) webRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (pathVariables == null) {
            throw new ResourceNotFoundException();
        }
        String idStr = pathVariables.get("idTopic");
        if (idStr == null) {
            throw new ResourceNotFoundException();
        }
        int id = Integer.parseInt(idStr);
        log.debug("Resolving topic with id {}", idStr);
        return topicRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    }
}

import com.github.dawidstankiewicz.forum.model.entity.Section;
import com.github.dawidstankiewicz.forum.model.entity.Topic;
import com.github.dawidstankiewicz.forum.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;


public interface TopicRepository extends JpaRepository<Topic, Integer> {
    
    Set<Topic> findBySection(Section section);
    
    Set<Topic> findByUser(User user);
    
    Set<Topic> findAllByOrderByCreationDateDesc();
    
    Set<Topic> findTop5ByOrderByCreationDateDesc();
    
    
}


import com.github.dawidstankiewicz.forum.model.dto.NewPostForm;
import com.github.dawidstankiewicz.forum.model.dto.NewTopicForm;
import com.github.dawidstankiewicz.forum.model.entity.Post;
import com.github.dawidstankiewicz.forum.model.entity.Section;
import com.github.dawidstankiewicz.forum.model.entity.Topic;
import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.post.PostService;
import com.github.dawidstankiewicz.forum.section.SectionService;
import com.github.dawidstankiewicz.forum.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;


@Controller
@Slf4j
public class TopicController {

    private final PostService postService;
    private final TopicService topicService;
    private final SectionService sectionService;
    private final UserService userService;

    public TopicController(PostService postService, TopicService topicService, SectionService sectionService, UserService userService) {
        this.postService = postService;
        this.topicService = topicService;
        this.sectionService = sectionService;
        this.userService = userService;
    }

    @RequestMapping(value = "/topics/{idTopic}", method = RequestMethod.GET)
    public String getTopicById(@PathTopic Topic topic, Model model) {
        model.addAttribute("topic", topic);
        List<Post> posts = postService.findByTopic(topic);
        model.addAttribute("topicPost", posts.get(0));
        model.addAttribute("posts", posts.stream().skip(1).collect(Collectors.toList()));
        model.addAttribute("newPost", new NewPostForm());
        return "topics/topic";
    }

    @RequestMapping(value = "/topics/{idTopic}", method = RequestMethod.POST)
    public String addPost(
            @Valid
            @ModelAttribute("newPost") NewPostForm newPost,
            BindingResult result,
            Authentication authentication,
            @PathTopic Topic topic,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("topic", topic);
            model.addAttribute("posts", postService.findByTopic(topic));
            return "topics/topic";
        }

        Post post = new Post();
        post.setContent(newPost.getContent());
        post.setTopic(topic);
        post.setUser(userService.findByEmail(authentication.getName()));
        postService.save(post);

        model.asMap().clear();
        return "redirect:/topics/" + topic.getId();
    }

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping(value = {"/topics/new"})
    public String getNewTopicForm(@Param("sectionId") Integer sectionId, Model model) {
        if (sectionId != null) {
            model.addAttribute("selectedSection", sectionService.findOneOrExit(sectionId));
        }
        model.addAttribute("newTopic", NewTopicForm.builder().sectionId(sectionId).build());
        model.addAttribute("sections", sectionService.findAll());
        return "topics/new_topic_form";
    }

    @PreAuthorize("hasAuthority('USER')")
    @PostMapping(value = "/topics/new")
    public String processAndAddNewTopic(@Valid @ModelAttribute("newTopic") NewTopicForm newTopic,
                                        BindingResult result,
                                        Authentication authentication,
                                        Model model) {
        log.info("Create new topic requested by user: " + authentication.getName());
        if (result.hasErrors()) {
            return getNewTopicForm(newTopic.getSectionId(), model);
        }
        User user = userService.findByEmailOrExit(authentication.getName());
        Section section = sectionService.findOneOrExit(newTopic.getSectionId());
        Topic topic = topicService.createNewTopic(newTopic, user, section);
        return "redirect:/topics/" + topic.getId();
    }

    @PreAuthorize("hasAuthority('USER')")
    @RequestMapping(value = "delete/{idTopic}", method = RequestMethod.GET)
    public String delete(@PathTopic Topic topic,
                         Authentication authentication,
                         RedirectAttributes model) {
        if (!isTopicOwner(topic, authentication)) {
            return "redirect:/topics/" + topic.getId();
        }
        topicService.delete(topic);

        model.addFlashAttribute("message", "topic.successfully.deleted");
        return "redirect:/section/" + topic.getSection().getId();
    }

    private static boolean isTopicOwner(Topic topic, Authentication authentication) {
        return authentication.getName().equals(topic.getUser().getEmail());
    }

}


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PathTopic {
}

import com.github.dawidstankiewicz.forum.exception.ResourceNotFoundException;
import com.github.dawidstankiewicz.forum.model.dto.NewTopicForm;
import com.github.dawidstankiewicz.forum.model.entity.Post;
import com.github.dawidstankiewicz.forum.model.entity.Section;
import com.github.dawidstankiewicz.forum.model.entity.Topic;
import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.post.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class TopicService {
    
    private final TopicRepository topicRepository;

    private final PostRepository postRepository;
    
    public TopicService(TopicRepository topicRepository, PostRepository postRepository) {
        this.topicRepository = topicRepository;
        this.postRepository = postRepository;
    }

    public List<Topic> findAll() {
        return topicRepository.findAll();
    }
    
    public Topic findOne(int id) {
        Optional<Topic> topicOptional = topicRepository.findById(id);
        if (!topicOptional.isPresent()) {
            throw new ResourceNotFoundException();
        }
        return topicOptional.get();
    }
    
    public Set<Topic> findRecent() {
        return topicRepository.findTop5ByOrderByCreationDateDesc();
    }

    public Set<Topic> findBySection(Section section) {
        return topicRepository.findBySection(section);
    }

    public Topic createNewTopic(NewTopicForm topicForm, User author, Section section) {
        Topic topic = Topic.builder()
                .section(section)
                .user(author)
                .title(topicForm.getTitle()).build();
        topic = topicRepository.save(topic);
        Post post = Post.builder()
                .topic(topic)
                .content(topicForm.getContent())
                .user(author)
                .build();
        postRepository.save(post);
        return topic;
    }

    public void delete(Topic topic) {
        topicRepository.delete(topic);
    }
    
}


import com.github.dawidstankiewicz.forum.config.Routes;
import com.github.dawidstankiewicz.forum.model.dto.UserRegistrationForm;
import com.github.dawidstankiewicz.forum.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;

@Controller
@Slf4j
public class RegistrationController {

    private final UserRegistrationService registrationService;

    public RegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping(value = "/new-user")
    public String userRegistrationForm(Model model, String error) {
        log.info("User registration form");
        if (error != null) {
            model.addAttribute("error", error);
        }
        model.addAttribute("userRegistrationForm", new UserRegistrationForm());
        return Routes.NEW_USER_FORM;
    }

    @PostMapping(value = "/new-user")
    public String registerNewUser(@Valid UserRegistrationForm registrationForm,
                                  BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return Routes.NEW_USER_FORM;
        }
        User created = registrationService.registerUser(registrationForm);
        model.addAttribute("username", created.getUsername());
        return Routes.REGISTRATION_CONFIRMATION;
    }
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.model.entity.UserProfile;
import com.github.dawidstankiewicz.forum.post.PostService;
import com.github.dawidstankiewicz.forum.topic.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserProfileService {
    
    private final UserService userService;
    
    private final PostService postService;
    
    private final TopicService topicService;

    public UserProfileService(UserService userService, PostService postService, TopicService topicService) {
        this.userService = userService;
        this.postService = postService;
        this.topicService = topicService;
    }

    public UserProfile findOne(int userId) {
        UserProfile userProfile = new UserProfile();
        User user = userService.findOne(userId);
        userProfile.setUser(user);
//        userProfile.setPosts(postService.findByUser(user));
//        userProfile.setTopics(topicService.findByUser(user));
        return userProfile;
    }
    
    public UserProfile findOne(String username) {
        return findOne(userService.findByUsername(username).getId());
    }
    
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.model.entity.UserProfile;
import com.github.dawidstankiewicz.forum.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;


@Controller
public class UserController {

    private final UserService userService;

    private final UserProfileService userProfileService;

    public UserController(UserService userService, UserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }
//
//    @RequestMapping(value = "/user/{username}")
//    public String findUserByUsernameAndViewProfilePage(@PathVariable String username,
//        Model model) {
//        UserProfile userProfile;
//        try {
//            userProfile = userProfileService.findOne(username);
//        } catch (NullPointerException e) {
//            throw new UserNotFoundException();
//        }
//        model.addAttribute("userProfile", userProfile);
//        return "user";
//    }

    @RequestMapping(value = "/users")
    public String listOfAllUser(Model model) {
        model.addAttribute("users", userService.findAll());
        return "users";
    }

    @RequestMapping(value = "/myprofile")
    public String myProfile(Authentication authentication,
        Model model) {
        String username = authentication.getName();
        UserProfile userProfile;
        try {
            userProfile = userProfileService.findOne(username);
        } catch (NullPointerException e) {
            throw new UserNotFoundException();
        }
        model.addAttribute("userProfile", userProfile);
        return "user";
    }

    @RequestMapping(value = "/myprofile/edit/picture", method = RequestMethod.POST)
    public String processAndSaveProfilePicture(@RequestPart MultipartFile profilePicture,
        HttpServletRequest request,
        Authentication authentication,
        RedirectAttributes redirectModel) {
        if (authentication.getName() == null) {
            return "redirect:/login";
        }
        if (profilePicture.isEmpty()) {
            return "redirect:/myprofile";
        }
        User user = userService.findByUsername(authentication.getName());
        try {
            String path =
                request.getSession().getServletContext().getRealPath("/resources/uploads/avatars/");
            profilePicture.transferTo(new File(path + user.getId() + ".jpg"));
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }

        userService.save(user);
        redirectModel.addFlashAttribute("message", "user.picture.successfully.saved");
        return "redirect:/myprofile";
    }

    @RequestMapping(value = "/logout")
    public String logOutAndRedirectToLoginPage(Authentication authentication,
        HttpServletRequest request,
        HttpServletResponse response) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout=true";
    }
}


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class LoginController {

    @GetMapping(value = "/login")
    public String login(Model model, String error, String logout) {
        if (error != null) {
            log.info("Logging in failed: " + error);
            model.addAttribute("error", error);
        }

        if (logout != null) {
            model.addAttribute("message", "login.logout");
        }

        return "login";
    }
}


import com.github.dawidstankiewicz.forum.email.EmailMessageService;
import com.github.dawidstankiewicz.forum.model.dto.UserRegistrationForm;
import com.github.dawidstankiewicz.forum.model.entity.EmailMessage;
import com.github.dawidstankiewicz.forum.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailMessageService emailMessageService;

    public UserRegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailMessageService emailMessageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailMessageService = emailMessageService;
    }

    public User registerUser(UserRegistrationForm form) {
        log.info("Register new user {}, {}", form.getEmail(), form.getUsername());
        String confirmationCode = scheduleConfirmationMessage(form.getEmail());
        User newUser = buildUser(form, confirmationCode);
        return userRepository.save(newUser);
    }

    private String scheduleConfirmationMessage(String email) {
        String randomString = Long.toHexString(Double.doubleToLongBits(Math.random()));
        EmailMessage confirmationMessage = EmailMessage.builder()
                .recipient(email)
                .content(randomString)
                .type(EmailMessage.EmailMessageType.CONFIRMATION)
                .build();
        emailMessageService.scheduleMessage(confirmationMessage);
        return randomString;
    }

    private User buildUser(UserRegistrationForm form, String confirmationCode) {
        return User.builder()
                .email(form.getEmail())
                .emailToken(confirmationCode)
                .username(form.getUsername())
                .createdAt(LocalDateTime.now())
                .enabled(false)
                .password(passwordEncoder.encode(form.getPassword()))
                .build();
    }
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Integer> {
    
    User findByUsername(String username);
    
    User findByEmail(String email);
    
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findOne(int id) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
        return user;
    }

    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByEmailOrExit(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException();
        }
        return user;
    }
}


import com.github.dawidstankiewicz.forum.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ActivationService {

    private final UserService userService;

    public ActivationService(UserService userService) {
        this.userService = userService;
    }

    public void activate(String username, String activationCodeId) {
        //todo
    }

}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ActivationController {

    private final ActivationService activationService;

    public ActivationController(ActivationService activationService) {
        this.activationService = activationService;
    }

    @RequestMapping(value = "/users/{username}/activation")
    public String activateUserAndRedirectToLoginPage(@PathVariable String username,
        @RequestParam String id) {
        activationService.activate(username, id);
        return "activation_success";
    }

}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailScheduler {

    private final EmailMessageRepository emailMessageRepository;

    public EmailScheduler(EmailMessageRepository emailMessageRepository) {
        this.emailMessageRepository = emailMessageRepository;
    }
}


import com.github.dawidstankiewicz.forum.model.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {
}


import com.github.dawidstankiewicz.forum.model.entity.EmailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


@Service
@Slf4j
public class SenderService {

    private final JavaMailSender javaMailSender;

    public SenderService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void sendEmail(EmailMessage emailMessage) {
        try {
            tryParseAndSendEmail(emailMessage);
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void tryParseAndSendEmail(EmailMessage emailMessage) throws MessagingException {
        MimeMessage mail = javaMailSender.createMimeMessage();
        parseMessage(emailMessage, mail);
        javaMailSender.send(mail);
    }

    private void parseMessage(EmailMessage emailMessage,
                              MimeMessage mail) throws MessagingException {
        MimeMessageHelper messageHelper = new MimeMessageHelper(mail, true);
        messageHelper.setTo(emailMessage.getRecipient());
        messageHelper.setSubject(emailMessage.getSubject());
        boolean HTMLFormat = true;
        messageHelper.setText(emailMessage.getContent(), HTMLFormat);
    }

    private void handleException(Exception e) {
        log.error("Mail Send Exception - smtp service unavailable");
        e.printStackTrace();
        throw new RuntimeException();
    }
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ConfirmationMessageStrategy {

    private final TemplateEngine templateEngine;

    private final String TEMPLATE = "messages/activation_message";
    private final String ACTIVATION_CODE_VARIABLE = "activationCode";

    public ConfirmationMessageStrategy(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    //todo
    public String createMessageContent(String activationCode) {
        Context context = new Context();
        context.setVariable(ACTIVATION_CODE_VARIABLE, activationCode);
        return templateEngine.process(TEMPLATE, context);
    }
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "User not found!")
public class UserNotFoundException extends RuntimeException {
    
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Incorrect password!")
public class IncorrectPasswordException extends RuntimeException {
    
    private static final long serialVersionUID = -5692096819031290349L;
    
}


import com.github.dawidstankiewicz.forum.model.entity.EmailMessage;
import com.github.dawidstankiewicz.forum.user.email.EmailMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailMessageService {

    private final EmailMessageRepository repository;

    public EmailMessageService(EmailMessageRepository repository) {
        this.repository = repository;
    }

    public void scheduleMessage(EmailMessage message) {
        message.setScheduledSentDate(LocalDateTime.now());
        message.setSent(false);
        repository.save(message);
    }
}



import com.github.dawidstankiewicz.forum.model.entity.Section;
import com.github.dawidstankiewicz.forum.topic.TopicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/sections/")
public class SectionController {

    private final SectionService sectionService;
    private final TopicService topicService;

    public SectionController(SectionService sectionService, TopicService topicService) {
        this.sectionService = sectionService;
        this.topicService = topicService;
    }

    @RequestMapping("{id}")
    public String getSection(@PathVariable int id,
                             Model model) {
        Section section = sectionService.findOneOrExit(id);
        model.addAttribute("section", section);
        model.addAttribute("topics", topicService.findBySection(section));
        return "sections/section";
    }

}


import com.github.dawidstankiewicz.forum.model.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SectionRepository extends JpaRepository<Section, Integer> {
    
    Section findByName(String name);
    
}


import com.github.dawidstankiewicz.forum.exception.ResourceNotFoundException;
import com.github.dawidstankiewicz.forum.model.entity.Section;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public Page<Section> findSections(Pageable pageable) {
        return sectionRepository.findAll(pageable);
    }

    public List<Section> findAll() {
        return sectionRepository.findAll();
    }

    public Section findOneOrExit(int id) {
        Optional<Section> sectionOptional = sectionRepository.findById(id);
        if (!sectionOptional.isPresent()) {
            throw new ResourceNotFoundException();
        }
        return sectionOptional.get();
    }

    public Section save(Section section) {
        return sectionRepository.save(section);
    }

    public void delete(int id) {
        delete(findOneOrExit(id));
    }

    public void delete(Section section) {
        sectionRepository.delete(section);
    }

}



import com.github.dawidstankiewicz.forum.config.Templates;
import com.github.dawidstankiewicz.forum.file.FileService;
import com.github.dawidstankiewicz.forum.model.ForumModelMapper;
import com.github.dawidstankiewicz.forum.model.dto.SectionDto;
import com.github.dawidstankiewicz.forum.model.dto.SectionForm;
import com.github.dawidstankiewicz.forum.model.entity.Section;
import com.github.dawidstankiewicz.forum.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.File;
import java.util.Arrays;
import java.util.List;


@Controller
@RequestMapping("/a/sections")
@Slf4j
public class SectionAdminController {

    private final SectionService sectionService;
    private final FileService fileService;
    private final ForumModelMapper modelMapper;

    public SectionAdminController(SectionService sectionService, FileService fileService, ForumModelMapper modelMapper) {
        this.sectionService = sectionService;
        this.fileService = fileService;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public String getSectionsPage(Model model, Pageable pageable) {
        Page<Section> sections = sectionService.findSections(pageable);
        model.addAttribute("sections", modelMapper.mapPage(sections, SectionDto.class));
        return Templates.ADMIN_SECTIONS_PANEL;
    }

    @GetMapping(value = "/new")
    public String getNewSectionForm(Model model) {
        model.addAttribute("newSection", new SectionForm());
        model.addAttribute("isNew", true);
        return "sections/section_form";
    }

    @PostMapping
    public String processAndAddNewSection(
            @Valid @ModelAttribute("newSection") SectionForm newSection, BindingResult result,
            @RequestParam(value = "file", required = false) MultipartFile imageFile,
            Model model) {
        log.info("Adding new section requested");
        validateImageFile(imageFile, result);
        if (result.hasErrors()) {
            log.info("Adding new section failed - invalid form");
            model.mergeAttributes(result.getModel());
            model.addAttribute("isNew", true);
            return "sections/section_form";
        }
        Section section = new Section();
        if (isImageUploaded(imageFile)) {
            File file = fileService.saveFile(imageFile, "/images/sections/");
            section.setImageFilename(file.getName());
        }
        section.setName(newSection.getName());
        section.setDescription(newSection.getDescription());
        section = sectionService.save(section);
        log.info("Adding new section succeeded: {}", section.getId());
        return "redirect:/sections/" + section.getId();
    }

    @GetMapping(value = "/{id}/details")
    public String getSectionForm(Model model, @PathVariable int id) {
        Section section = sectionService.findOneOrExit(id);
        model.addAttribute("newSection",
                SectionForm.builder()
                        .id(section.getId())
                        .name(section.getName())
                        .description(section.getDescription())
                        .imageFilename(section.getImageFilename())
                        .build());
        model.addAttribute("isNew", false);
        return "sections/section_form";
    }

    @PostMapping("/{id}")
    public String updateSection(@Valid @ModelAttribute("newSection") SectionForm newSection,
                                BindingResult result,
                                @RequestParam(value = "file", required = false) MultipartFile imageFile,
                                Model model) {
        log.info("Updating section: {}", newSection.getId());
        validateImageFile(imageFile, result);
        if (result.hasErrors()) {
            log.info("Section form has errors (id: {})", newSection.getId());
            model.mergeAttributes(result.getModel());
            return "sections/section_form";
        }
        Section section = sectionService.findOneOrExit(newSection.getId());
        if (isImageUploaded(imageFile)) {
            File file = fileService.saveFile(imageFile, "/images/sections/");
            section.setImageFilename(file.getName());
        } else {
            section.setImageFilename(newSection.getImageFilename());
        }
        section.setName(newSection.getName());
        section.setDescription(newSection.getDescription());
        section = sectionService.save(section);
        log.info("Updated section: {}", newSection.getId());
        return "redirect:/sections/" + section.getId();
    }

    @GetMapping(value = "/{id}/delete")
    public String getDeleteSectionConfirmationForm(Model model, @PathVariable int id) {
        Section section = sectionService.findOneOrExit(id);
        model.addAttribute("type", "section");
        model.addAttribute("name", section.getName());
        model.addAttribute("deleteUrl", "/a/sections/" + section.getId() + "/delete");
        return "confirm_delete_form";
    }

    @PostMapping(value = "/{id}/delete")
    public String delete(@PathVariable int id, RedirectAttributes model) {
        sectionService.delete(id);
        model.addFlashAttribute("message", "section.successfully.deleted");
        return "redirect:/home";
    }

    private void validateImageFile(MultipartFile imageFile, BindingResult result) {
        if (!isImageUploaded(imageFile)) {
            return;
        }
        int maxAllowedImageFileSize = 4 * 1024 * 1024;
        if (imageFile.getSize() > maxAllowedImageFileSize) {
            result.reject("Size.Section.image.validation", "Image size should be up to 4 MB");
        }
        String fileExtension = FileUtils.getFileExtension(imageFile.getOriginalFilename()).get().toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png");
        if (!allowedExtensions.contains(fileExtension)) {
            result.reject("Format.Section.image.validation", "Select image with one of the following extension: PNG, JPG, JPEG");
        }
    }

    private static boolean isImageUploaded(MultipartFile imageFile) {
        return imageFile != null && !imageFile.isEmpty();
    }
}


import java.util.Optional;

public class FileUtils {

    public static Optional<String> getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .or(() -> Optional.of(""));
    }
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Resource not found")
public class ResourceNotFoundException extends RuntimeException {
}


import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport
public class PaginationConfig {
}


public class Templates {
    private static final String ADMIN_PREFIX = "admin/";


    public static final String ADMIN_SECTIONS_PANEL = ADMIN_PREFIX + "sections";

}


public class Routes {

    public final static String REDIRECT = "redirect:";

    public static final String NEW_USER_FORM_PAGE = "/new-user";
    public static final String LOGIN_PAGE = "/login";

    public static final String REGISTRATION_CONFIRMATION = "registration/confirmation_sent";
    public static final String NEW_USER_FORM = "registration/new_user_form";

}


import com.github.dawidstankiewicz.forum.topic.PathTopicArgumentResolver;
import com.github.dawidstankiewicz.forum.topic.TopicRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TopicRepository topicRepository;

    public WebConfig(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(
                        "file:src/main/resources/uploads/avatars/",
                        "file:src/main/resources/static/img/default-avatars/");
        String userHomePath = System.getProperty("user.home");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + userHomePath + "/forum/images/sections/");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PathTopicArgumentResolver(topicRepository));
    }
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ForumUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public ForumUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Login user by email: " + email);
        User user = userService.findByEmailOrExit(email);
        Set<GrantedAuthority> grantedAuthorities = getGrantedAuthorities(user);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                grantedAuthorities);
    }

    private Set<GrantedAuthority> getGrantedAuthorities(User user) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        if (user.getRoles() != null) {
            grantedAuthorities = user.getRoles()
                    .stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toSet());
        }
        return grantedAuthorities;
    }
}


public class AccessRules {

    protected static final String[] FOR_EVERYONE = {
            "/hello",
            "/login",
            "/error",
            "/users/**"
    };

    protected static final String[] FOR_AUTHORIZED_USERS = {
            "/user/**",
            "/topics/new/**",
            "/topics/delete/**",
            "/section/delete/**",
            "/section/new/**",
            "/post/**",
            "/myprofile/**"
    };

    protected static final String[] FOR_ADMINS = {
            "/admin/**",
            "/a/**",
            "/users/**",
            "/section/new"
    };

    protected static final String[] ADMINS_ROLES = {
            "HEAD_ADMIN",
            "ADMIN"
    };

}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.CharacterEncodingFilter;

import static com.github.dawidstankiewicz.forum.security.AccessRules.*;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final ForumUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CsrfTokenRepository csrfTokenRepository;

    public SecurityConfig(ForumUserDetailsService userDetailsService, PasswordEncoder passwordEncoder, CsrfTokenRepository csrfTokenRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        configureAccessRules(http);
        configureLoginForm(http);
        configureLogout(http);
        configureRememberMe(http);
        configureCsrf(http);
        configureEncodingFilter(http);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder);
    }

    private void configureAccessRules(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers(FOR_AUTHORIZED_USERS).authenticated()
            .antMatchers(FOR_ADMINS).hasAnyAuthority(ADMINS_ROLES)
            .antMatchers(FOR_EVERYONE).permitAll();
    }

    private void configureLoginForm(HttpSecurity http) throws Exception {
        http.formLogin()
            .loginPage("/login")
            .permitAll();
    }

    private void configureLogout(HttpSecurity http) throws Exception {
        http.logout()
            .permitAll();
    }

    private void configureRememberMe(HttpSecurity http) throws Exception {
        http.rememberMe()
            .tokenValiditySeconds(2419200)
            .key("forum-key");
    }

    private void configureCsrf(HttpSecurity http) throws Exception {
        http.csrf()
            .csrfTokenRepository(csrfTokenRepository);
    }

    private void configureEncodingFilter(HttpSecurity http) {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        http.addFilterBefore(filter, CsrfFilter.class);
    }
}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
public class CsrfConfig {

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

}



import com.github.dawidstankiewicz.forum.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    private final String storageRootPath;

    public FileService(@Value("${forum.storage.rootPath}") String storageRootPath) {
        this.storageRootPath = storageRootPath;
    }

    public File saveFile(MultipartFile file, String subdirectory) {
        try {
            StringJoiner newFilename = new StringJoiner(".").add(UUID.randomUUID().toString()).add(FileUtils.getFileExtension(file.getOriginalFilename()).get());
            String filePath = storageRootPath + subdirectory;
            Files.createDirectories(Paths.get(filePath));
            File finalFile = new File(filePath + newFilename);
            Files.createFile(finalFile.toPath());
            log.info("Saving file to {}", finalFile.getAbsolutePath());
            file.transferTo(finalFile);
            return finalFile;
        } catch (
                IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}


import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ForumModelMapper extends ModelMapper {


    public <T> Page<T> mapPage(Page<?> sections, Class<T> destinationClass) {
        return new PageImpl<>(
                sections.getContent()
                        .stream()
                        .map(item -> map(item, destinationClass))
                        .collect(Collectors.toList()),
                sections.getPageable(),
                sections.getTotalElements());
    }
}


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {
    String message() default "{Email.AlreadyUsed}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        User user = userRepository.findByEmail(email);
        return user == null;
    }
}


import com.github.dawidstankiewicz.forum.model.entity.User;
import com.github.dawidstankiewicz.forum.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userRepository;

    public UniqueUsernameValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        User user = userRepository.findByUsername(email);
        return user == null;
    }
}


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueUsernameValidator.class)
public @interface UniqueUsername {
    String message() default "{Username.AlreadyUsed}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}


import com.github.dawidstankiewicz.forum.model.validator.UniqueEmail;
import com.github.dawidstankiewicz.forum.model.validator.UniqueUsername;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationForm {

    @Email(message = "{Email.Invalid}")
    @NotNull(message = "{Email.Empty}")
    @UniqueEmail
    private String email;

    @NotNull(message = "{Field.Required}")
    @Size(min = 8, max = 100, message = "{Password.InvalidSize}")
    private String password;

    @NotNull(message = "{Field.Required}")
    @Size(min = 4, max = 60, message = "{Username.InvalidSize}")
    @UniqueUsername
    private String username;
}


import com.github.dawidstankiewicz.forum.model.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewPostForm {

    private String content;
    private Post.ContentType contentType;

}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SectionForm {

    private Integer id;
    @Size(min = 3)
    private String name;
    private String description;
    private String imageFilename;

}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTopicForm {
    @NotEmpty
    private String title;
    @NotEmpty
    private Integer sectionId;
    @NotEmpty
    private String content;

}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SectionDto {

    private int id;
    private String name;
    private String description;
}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String secondaryEmail;

    @Column(unique = true)
    private String emailToken;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(length = 60)
    private String password;

    private boolean enabled;

    private boolean removed;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginTime;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @OneToOne(mappedBy = "user")
    private UserProfile info;
}


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    private Long id;

}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "topics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn
    private User user;

    @ManyToOne
    @JoinColumn
    private Section section;

    @Column(length = 50)
    private String title;

    @Column
    private int views;

    @Column(updatable = false, nullable = false)
    private LocalDateTime creationDate;

    @Column
    private LocalDateTime lastUpdateDate;

    @Column
    private boolean closed;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdateDate = LocalDateTime.now();
    }

}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn
    private Topic topic;

    @ManyToOne
    @JoinColumn
    private User user;

    @Column(length = 100000)
    private String content;

    private ContentType contentType;

    @Column(updatable = false, nullable = false)
    private LocalDateTime creationDate;

    @Column
    private LocalDateTime modificationDate;

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = LocalDateTime.now();
    }

    public enum ContentType {
        TEXT,
        MARKDOWN,
    }
}



import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_profiles")
@Data
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class UserProfile {

    @Id
    @Column(name = "id")
    private int id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(length = 15)
    private String phone;

    @Column(length = 20)
    private String name;

    @Column(length = 30)
    private String lastName;

    private Date birthday;

    @Column(length = 20)
    private String city;

    @Column(length = 150)
    private String aboutMe;

    @Column(length = 50)
    private String footer;
}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String recipient;
    private String subject;
    private String content;
    private boolean sent;
    private LocalDateTime scheduledSentDate;
    private LocalDateTime sentDate;
    private EmailMessageType type;

    public enum EmailMessageType {
        CONFIRMATION,
        PASSWORD_RESET,
    }
}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;


@Entity
@Table(name = "sections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(length = 50)
    private String name;

    @Column(length = 150)
    private String description;

    @Column(length = 50)
    private String imageFilename;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Topic> topics;
}


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "roles")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;

}

