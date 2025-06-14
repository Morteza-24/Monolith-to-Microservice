
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import com.chat.tools.Tools;

/**
 * Created by tyler on 5/24/16.
 */
public class DataSources {

    public static final String CODE_DIR = System.getProperty("user.dir");

    public static Boolean SSL = false;

    public static final Integer EXPIRE_SECONDS = 86400 * 7; // stays logged in for 7 days

    public static final String PROPERTIES_FILE = CODE_DIR + "/flowchat.properties";

    public static Properties PROPERTIES = Tools.loadProperties(PROPERTIES_FILE);

    public static final String CHANGELOG_MASTER = "liquibase/db.changelog-master.xml";

}


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpCookie;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.chat.DataSources;
import com.chat.db.Tables;
import com.chat.types.community.CommunityRole;
import com.chat.types.user.User;
import com.chat.webservice.ConstantsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import spark.Request;

/**
 * Created by tyler on 5/24/16.
 */
public class Tools {

    public static Logger log = (Logger) LoggerFactory.getLogger(Tools.class);

    public static ObjectMapper JACKSON = new ObjectMapper();
    public static TypeFactory typeFactory = JACKSON.getTypeFactory();

    public static MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final BasicPasswordEncryptor PASS_ENCRYPT = new BasicPasswordEncryptor();   
    
    public static final HikariConfig hikariConfig() {
        HikariConfig hc = new HikariConfig();
        DataSources.PROPERTIES = Tools.loadProperties(DataSources.PROPERTIES_FILE);
        hc.setJdbcUrl(DataSources.PROPERTIES.getProperty("jdbc.url"));
        hc.setUsername(DataSources.PROPERTIES.getProperty("jdbc.username"));
        hc.setPassword(DataSources.PROPERTIES.getProperty("jdbc.password"));
        hc.setMaximumPoolSize(10);
        return hc;
    }

    public static final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig());

    public static final void dbInit() {
        Base.open(hikariDataSource); // get connection from pool
    }

    public static final void dbClose() {
        Base.close();
    }

    public static final Algorithm getJWTAlgorithm() {
        Algorithm JWTAlgorithm = null;
        try {
            JWTAlgorithm = Algorithm.HMAC256(DataSources.PROPERTIES.getProperty("jdbc.password"));
        } catch (UnsupportedEncodingException | JWTCreationException exception) {
        }

        return JWTAlgorithm;
    }

    public static final DecodedJWT decodeJWTToken(String token) {

        DecodedJWT jwt = null;

        try {
            JWTVerifier verifier = JWT.require(getJWTAlgorithm()).withIssuer("flowchat").build(); 
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
        }

        return jwt;
    }

    public static final User getUserFromJWTHeader(Request req) {
        return User.create(req.headers("token"));
    }

    public static Properties loadProperties(String propertiesFileLocation) {

        Properties prop = new Properties();

        Map<String, String> env = System.getenv();
        for (String varName : env.keySet()) {
            switch (varName) {
                case "FLOWCHAT_DB_URL":
                    prop.setProperty("jdbc.url", env.get(varName));
                    break;
                case "FLOWCHAT_DB_USERNAME":
                    prop.setProperty("jdbc.username", env.get(varName));
                    break;
                case "FLOWCHAT_DB_PASSWORD":
                    prop.setProperty("jdbc.password", env.get(varName));
                    break;
                case "SORTING_CREATED_WEIGHT":
                    prop.setProperty("sorting_created_weight", env.get(varName));
                    break;
                case "SORTING_NUMBER_OF_VOTES_WEIGHT":
                    prop.setProperty("sorting_number_of_votes_weight", env.get(varName));
                    break;
                case "SORTING_AVG_RANK_WEIGHT":
                    prop.setProperty("sorting_avg_rank_weight", env.get(varName));
                    break;


            }
        }

        if (prop.getProperty("jdbc.url") != null) {
            return prop;
        }



        InputStream input = null;
        try {
            input = new FileInputStream(propertiesFileLocation);

            // load a properties file
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally  {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return prop;

    }

    public static <T> List<T> convertArrayToList(Array arr) {
        try {
            T[] larr = (T[]) arr.getArray();

            List<T> list = new ArrayList<>(Arrays.asList(larr));

            return list;
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Map<String, String> cookieListToMap(List<HttpCookie> list) {
        return list.stream().collect(Collectors.toMap(
                HttpCookie::getName, HttpCookie::getValue));
    }

    public static String generateSecureRandom() {
        return new BigInteger(256, RANDOM).toString(32);
    }

    public static Timestamp newExpireTimestamp() {
        return new Timestamp(new Date().getTime() + 1000 * DataSources.EXPIRE_SECONDS);
    }

    public static final Map<String, String> createMapFromAjaxPost(String reqBody) {
        log.debug(reqBody);
        Map<String, String> postMap = new HashMap<String, String>();
        String[] split = reqBody.split("&");
        for (int i = 0; i < split.length; i++) {
            String[] keyValue = split[i].split("=");
            try {
                if (keyValue.length > 1) {
                    postMap.put(URLDecoder.decode(keyValue[0], "UTF-8"),URLDecoder.decode(keyValue[1], "UTF-8"));
                }
            } catch (UnsupportedEncodingException |ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                throw new NoSuchElementException(e.getMessage());
            }
        }

//		log.debug(GSON2.toJson(postMap));

        return postMap;

    }

    public static final Map<String, String> createMapFromReqBody(String reqBody) {

        Map<String, String> map = new HashMap<>();
        try {
            map = JACKSON.readValue(reqBody, mapType);
        } catch(IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    public static Integer findIndexByIdInLazyList(LazyList<? extends Model> ctv, Long searchId) {
        Integer index = IntStream.range(0, ctv.size()).filter(c -> ctv.get(c).getLongId() == searchId).toArray()[0];
        return index;
    }

    public static String[] pgArrayAggToArray(String text) {
        return text.replaceAll("\\{|\\}", "").split(",");
    }

    public static String convertListToInQuery(Collection<?> col){
        return Arrays.toString(col.toArray()).replaceAll("\\[","(").replaceAll("\\]", ")");
    }

    public static String constructQueryString(String query, String columnName) {

        try {
            query = java.net.URLDecoder.decode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] splitWords = query.split(" ");
        StringBuilder queryStr = new StringBuilder();

        for(int i = 0;;) {
            String word = splitWords[i++].replaceAll("'", "_");

            String likeQuery = columnName + " ilike '%" + word + "%'";

            queryStr.append(likeQuery);

            if (i < splitWords.length) {
                queryStr.append(" and ");
            } else {
                break;
            }
        }

        return queryStr.toString();

    }

    public static String constructOrderByCustom(String orderBy, Boolean singleCommunity) {

        String orderByOut = (singleCommunity) ? "stickied desc, " : "";
        if (orderBy.startsWith("time-")) {
            Long timeValue = Long.valueOf(orderBy.split("-")[1]);

            // For the custom sorting based on ranking
            orderByOut += "ranking(created, " + timeValue +
                    ",number_of_votes, " + ConstantsService.INSTANCE.getRankingConstants().getNumberOfVotesWeight() +
                    ",avg_rank, " + ConstantsService.INSTANCE.getRankingConstants().getAvgRankWeight() +
                    ") desc nulls last";

        } else {
            orderByOut += orderBy.replaceAll("__", " ").concat(" nulls last");
        }

        return orderByOut;
    }

    public static String constructOrderByPopularTagsCustom(String orderBy) {

        String orderByOut;
        if (orderBy.startsWith("time-")) {
            Long timeValue = Long.valueOf(orderBy.split("-")[1]);

            orderByOut = "ranking(created, " + timeValue +
                    ",count, " + ConstantsService.INSTANCE.getRankingConstants().getNumberOfVotesWeight() +
                    ") desc";
        } else {
            orderByOut = "created desc";
        }

        return orderByOut;
    }

    public static Set<Long> fetchCommunitiesFromParams(String communityParam, User userObj) {

        log.debug("community param = " + communityParam);
        Set<Long> communityIds = new HashSet<>();
        if (communityParam.equals("all")) {
            return null;
        } else if (communityParam.equals("favorites")) {
            // Fetch the user's favorite communities
            LazyList<Tables.CommunityUser> favoriteCommunities =
                    Tables.CommunityUser.where("user_id = ? and community_role_id != ?",
                            userObj.getId(),
                            CommunityRole.BLOCKED.getVal());
            communityIds = favoriteCommunities.collectDistinct("community_id");
        } else {
            communityIds.add(Long.valueOf(communityParam));
        }

        if (communityIds.isEmpty()) {
            communityIds = null;
        }

        return communityIds;
    }

    public static void runLiquibase() {

        Liquibase liquibase = null;
        Connection c = null;
        try {
            c = DriverManager.getConnection(DataSources.PROPERTIES.getProperty("jdbc.url"),
                    DataSources.PROPERTIES.getProperty("jdbc.username"),
                    DataSources.PROPERTIES.getProperty("jdbc.password"));

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            log.debug(DataSources.CHANGELOG_MASTER);
            liquibase = new Liquibase(DataSources.CHANGELOG_MASTER, new ClassLoaderResourceAccessor(), database);
            liquibase.update("main");
        } catch (SQLException | LiquibaseException e) {
            e.printStackTrace();
            throw new NoSuchElementException(e.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.rollback();
                    c.close();
                } catch (SQLException e) {
                    //nothing to do
                }
            }
        }
    }

}



import ch.qos.logback.classic.Logger;
import com.chat.DataSources;
import com.chat.db.Actions;
import com.chat.db.Tables;
import com.chat.tools.Tools;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;
import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;

public class RedditImporter implements Job {

    public static Logger log = (Logger) LoggerFactory.getLogger(RedditImporter.class);

    private RedditClient redditClient;

    public void init() {

        try {
            UserAgent myUserAgent = UserAgent.of("desktop", "com.flowchat", "v0.1", "dessalines");
            redditClient = new RedditClient(myUserAgent);
            Credentials credentials = Credentials.script(DataSources.PROPERTIES.getProperty("reddit_username"),
                    DataSources.PROPERTIES.getProperty("reddit_password"),
                    DataSources.PROPERTIES.getProperty("reddit_client_id"),
                    DataSources.PROPERTIES.getProperty("reddit_client_secret"));
            OAuthData authData = redditClient.getOAuthHelper().easyAuth(credentials);
            redditClient.authenticate(authData);
        } catch (OAuthException e) {
            e.printStackTrace();
        }
    }

    public void fetchTopPosts() {

        init();

        log.info("Fetching top reddit posts...");

        SubredditPaginator paginator = new SubredditPaginator(redditClient);
        paginator.setSubreddit("popular");
        paginator.setTimePeriod(TimePeriod.DAY);
        paginator.setSorting(Sorting.TOP);
        paginator.setLimit(50);
        Integer pageLimit = 100;

        Tools.dbInit();
        for (int i = 0; i < pageLimit; i++) {
            Listing<Submission> currentPage = paginator.next();
            for (Submission s : currentPage) {
                Tables.Tag t = Actions.getOrCreateTagFromSubreddit(s.getSubredditName());

                Actions.getOrCreateDiscussionFromRedditPost(t.getLongId(),
                        StringUtils.abbreviate(s.getTitle().replaceAll("\\r|\\n", "").replaceAll("\"", "").trim(), 140),
                        s.getUrl(),
                        s.getSelftext(),
                        s.getCreated());

            }
        }
        Tools.dbClose();

        log.info("Done fetching top reddit posts.");
    }

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        fetchTopPosts();
    }

}


import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;


/**
 * Created by tyler on 5/30/17.
 */

public class ScheduledJobs {
    public static void start() {
        // Another
        try {
            // Grab the Scheduler instance from the Factory
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // and start it off
            scheduler.start();

            JobDetail fetchFromReddit = newJob(RedditImporter.class)
                    .build();

            // Trigger the job to run now, and then repeat every x minutes
            Trigger fetchFromRedditTrigger = newTrigger()
                    .startNow()
                    .withSchedule(simpleSchedule()
                            .withIntervalInHours(4)
                            .repeatForever())
                    .build();

            // Tell quartz to schedule the job using our trigger
            scheduler.scheduleJob(fetchFromReddit, fetchFromRedditTrigger);

        } catch (SchedulerException se) {
            se.printStackTrace();
        }
    }
}


import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.chat.tools.Tools;
import com.chat.types.comment.Comment;
import com.chat.types.user.User;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * This helps determine what information gets broadcast to who.
 * Users only get updates for the discussion they're in, and for the
 * top comment parent(maybe null) that they are currently viewing
 * Created by tyler on 6/11/16.
 */
public class SessionScope {

    public static Logger log = (Logger) LoggerFactory.getLogger(SessionScope.class);

    private final Session session;
    private final User userObj;
    private Long discussionId;
    private Long topParentId;
    private String sortType;

    public SessionScope(Session session, User userObj, Long discussionId, Long topParentId, String sortType) {
        this.session = session;
        this.userObj = userObj;
        this.discussionId = discussionId;
        this.topParentId = topParentId;
        this.sortType = sortType;
    }

    public static Set<User> getUserObjects(Set<SessionScope> scopes) {
        return scopes.stream().map(SessionScope::getUserObj).collect(Collectors.toSet());
    }

    public static Set<Session> getSessions(Set<SessionScope> scopes) {
        return scopes.stream().map(SessionScope::getSession).collect(Collectors.toSet());
    }

    public static SessionScope findBySession(Set<SessionScope> scopes, Session session) {
        return scopes.stream().filter(s -> s.getSession().equals(session))
                .collect(Collectors.toSet()).iterator().next();
    }

    public static Set<SessionScope> constructFilteredMessageScopesFromSessionRequest(
            Set<SessionScope> scopes, Session session, List<Long> breadcrumbs) {


        Set<SessionScope> filteredScopes;
        Long discussionId = getDiscussionIdFromSession(session);

        log.debug(Arrays.toString(breadcrumbs.toArray()));
        log.debug(scopes.toString());

        filteredScopes = scopes.stream()
                .filter(s -> s.getDiscussionId().equals(discussionId) &&
                        // Send it to all top levels(null top), or those who have the parent in their crumbs
                        (s.getTopParentId() == null || breadcrumbs.contains(s.getTopParentId())))
                .collect(Collectors.toSet());


        return filteredScopes;

    }

    public static Set<SessionScope> constructFilteredUserScopesFromSessionRequest(
            Set<SessionScope> scopes, Session session) {

            Set<SessionScope> filteredScopes;
            Long discussionId = getDiscussionIdFromSession(session);

            filteredScopes = scopes.stream()
                    .filter(s -> s.getDiscussionId().equals(discussionId))
                    .collect(Collectors.toSet());

            return filteredScopes;


    }

    public static Long getTopParentIdFromSession(Session session) {
        Long topParentId = null;

        String maybeUndefined = session.getUpgradeRequest().getParameterMap().get("topParentId").iterator().next();

        if (!maybeUndefined.equals("NaN")) {
            topParentId = Long.valueOf(maybeUndefined);
        }

        return topParentId;
    }

    public static Long getDiscussionIdFromSession(Session session) {
        return Long.valueOf(session.getUpgradeRequest().getParameterMap().get("discussionId").iterator().next());
    }

    public static String getSortTypeFromSession(Session session) {
        return session.getUpgradeRequest().getParameterMap().get("sortType").iterator().next();
    }

    public static User getUserFromSession(Session session) {
        Map<String, String> cookieMap = Tools.cookieListToMap(session.getUpgradeRequest().getCookies());
        String jwt = cookieMap.get("jwt");
    
        return User.create(jwt);
      }

    public Long getDiscussionId() {
        return discussionId;
    }

    public Session getSession() {
        return session;
    }

    public User getUserObj() {
        return userObj;
    }

    public Long getTopParentId() {
        return topParentId;
    }

    public Comparator<Comment> getCommentComparator() {
        if (this.sortType.equals("new")) {
            return new Comment.CommentObjComparatorNew();
        } else if (this.sortType.equals("top")) {
            return new Comment.CommentObjComparatorTop();
        } else {
            return new Comment.CommentObjComparatorHot();
        }
        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SessionScope that = (SessionScope) o;

        if (!session.equals(that.session)) return false;
        if (!userObj.equals(that.userObj)) return false;
        if (!discussionId.equals(that.discussionId)) return false;
        return topParentId != null ? topParentId.equals(that.topParentId) : that.topParentId == null;

    }

    @Override
    public int hashCode() {
        int result = session.hashCode();
        result = 31 * result + userObj.hashCode();
        result = 31 * result + discussionId.hashCode();
        result = 31 * result + (topParentId != null ? topParentId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SessionScope{" +
                ", userObj=" + userObj +
                ", discussionId=" + discussionId +
                ", topParentId=" + topParentId +
                ", sortTYpe=" + sortType +
                '}';
    }
}


import com.chat.DataSources;
import com.chat.db.Tables;
import com.chat.tools.Tools;

/**
 * Created by tyler on 6/27/16.
 */
public class RankingConstants {

    private Double createdWeight, numberOfVotesWeight, avgRankWeight;

    private RankingConstants(Double createdWeight, Double numberOfVotesWeight, Double avgRankWeight) {
        this.createdWeight = createdWeight;
        this.numberOfVotesWeight = numberOfVotesWeight;
        this.avgRankWeight = avgRankWeight;
    }

    public static void writeRankingConstantsToDBFromPropertiesFile() {

        Tools.dbInit();
        Tables.RankingConstants rc = Tables.RankingConstants.findFirst("id = 1");
        rc.set("created_weight", Double.valueOf(DataSources.PROPERTIES.getProperty("sorting_created_weight")),
                "number_of_votes_weight", Double.valueOf(DataSources.PROPERTIES.getProperty("sorting_number_of_votes_weight")),
                "avg_rank_weight", Double.valueOf(DataSources.PROPERTIES.getProperty("sorting_avg_rank_weight")));
        rc.saveIt();
        Tools.dbClose();

    }

    private static RankingConstants create(Tables.RankingConstants rc) {
        return new RankingConstants(rc.getDouble("created_weight"),
                rc.getDouble("number_of_votes_weight"),
                rc.getDouble("avg_rank_weight"));

    }

    public static RankingConstants fetchRankingConstants() {

        Tools.dbInit();
        Tables.RankingConstants rc = Tables.RankingConstants.findFirst("1=1");
        RankingConstants rco = create(rc);
        Tools.dbClose();

        return rco;
    }

    public Double getCreatedWeight() {
        return createdWeight;
    }
    public Double getNumberOfVotesWeight() {
        return numberOfVotesWeight;
    }
    public Double getAvgRankWeight() {
        return avgRankWeight;
    }
}


import com.chat.tools.Tools;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

/**
 * Created by tyler on 6/7/16.
 */
public interface JSONWriter {
    default String json(String wrappedName) {
        try {
            String val = Tools.JACKSON.writeValueAsString(this);

            String json = (wrappedName != null) ? "{\"" + wrappedName + "\":" +
                    val +
                    "}" : val;

            return json;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    default String json() {
        return json(null);
    }

}


import com.chat.db.Tables;
import com.chat.tools.Tools;
import com.chat.types.JSONWriter;
import com.chat.types.community.Community;
import com.chat.types.tag.Tag;
import com.chat.types.user.User;
import com.chat.webservice.ConstantsService;
import org.javalite.activejdbc.Model;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by tyler on 6/19/16.
 */
public class Discussion implements JSONWriter {
    private Long id;
    private User creator, modifiedByUser;
    private String title, link, text;
    private Boolean private_, deleted, nsfw, stickied;
    private Integer avgRank, userRank, numberOfVotes, numberOfComments;
    private Community community;
    private List<Tag> tags;
    private List<User> privateUsers, blockedUsers;
    private Timestamp created, modified;

    public Discussion() {
    }

    public Discussion(Long id,
                      String title,
                      String link,
                      String text,
                      Boolean private_,
                      Boolean nsfw,
                      Boolean stickied,
                      Integer avgRank,
                      Integer userRank,
                      Integer numberOfVotes,
                      Integer numberOfComments,
                      List<Tag> tags,
                      User creator,
                      User modifiedByUser,
                      List<User> privateUsers,
                      List<User> blockedUsers,
                      Boolean deleted,
                      Community community,
                      Timestamp created,
                      Timestamp modified) {
        this.id = id;
        this.creator = creator;
        this.modifiedByUser = modifiedByUser;
        this.title = title;
        this.link = link;
        this.text = text;
        this.private_ = private_;
        this.nsfw = nsfw;
        this.stickied = stickied;
        this.avgRank = avgRank;
        this.userRank = userRank;
        this.numberOfVotes = numberOfVotes;
        this.numberOfComments = numberOfComments;
        this.tags = tags;
        this.creator = creator;
        this.privateUsers = privateUsers;
        this.blockedUsers = blockedUsers;
        this.deleted = deleted;
        this.community = community;
        this.created = created;
        this.modified = modified;


    }

    public void checkPrivate(User userObj) {
        if (getPrivate_().equals(true)) {
            if (!userObj.equals(creator) && !getPrivateUsers().contains(userObj)) {
                throw new NoSuchElementException("Private discussion, not allowed to view");
            }
        }
    }

    public void checkBlocked(User userObj) {
        if (getBlockedUsers().contains(userObj)) {
            throw new NoSuchElementException("You have been blocked from this discussion");
        }
    }

    public static Discussion create(Model d,
                                    Tables.CommunityNoTextView cntv,
                                    List<Tables.DiscussionTagView> discussionTags,
                                    List<Tables.DiscussionUserView> discussionUsers,
                                    List<Tables.CommunityUserView> communityUsers,
                                    Integer vote) {
        // convert the tags
        List<Tag> tags = null;
        if (discussionTags != null) {
            tags = new ArrayList<>();
            for (Tables.DiscussionTagView dtv : discussionTags) {
                tags.add(Tag.create(dtv.getLong("tag_id"), dtv.getString("name")));
            }
        }

        // convert the user discussion roles
        User creator = null;
        List<User> privateUsers = new ArrayList<>();
        List<User> blockedUsers = new ArrayList<>();

        if (discussionUsers != null) {
            for (Tables.DiscussionUserView udv : discussionUsers) {

                DiscussionRole role = DiscussionRole.values()[udv.getLong("discussion_role_id").intValue() - 1];

                User userObj = User.create(udv.getLong("user_id"), udv.getString("name"));

                switch (role) {
                    case BLOCKED:
                        blockedUsers.add(userObj);
                        break;
                    case USER:
                        privateUsers.add(userObj);
                        break;
                    case CREATOR:
                        creator = userObj;
                        break;
                }
            }
        }

        // Create the community
        Community community = (cntv != null) ? Community.create(cntv, null, communityUsers, null) : null;

        // If the community is NSFW, the discussion must be
        Boolean nsfw = (community != null && community.getNsfw()) ? true :  d.getBoolean("nsfw");

        // Create the modified by user
        User modifiedByUser = User.create(d.getLong("modified_by_user_id"), d.getString("modified_by_user_name"));

        return new Discussion(d.getLongId(),
                d.getString("title"),
                d.getString("link"),
                d.getString("text_"),
                d.getBoolean("private"),
                nsfw,
                d.getBoolean("stickied"),
                d.getInteger("avg_rank"),
                vote,
                d.getInteger("number_of_votes"),
                d.getInteger("number_of_comments"),
                tags,
                creator,
                modifiedByUser,
                privateUsers,
                blockedUsers,
                d.getBoolean("deleted"),
                community,
                d.getTimestamp("created"),
                d.getTimestamp("modified"));
    }

    public static Discussion fromJson(String dataStr) {

        try {
            return Tools.JACKSON.readValue(dataStr, Discussion.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getText() {
        return ConstantsService.INSTANCE.replaceCensoredText(text);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return ConstantsService.INSTANCE.replaceCensoredText(title);
    }

    public String getLink() {
        return link;
    }

    public Boolean getPrivate_() {
        return private_;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public Boolean getStickied() {
        return stickied;
    }

    public Integer getAvgRank() {
        return avgRank;
    }

    public Integer getUserRank() {
        return userRank;
    }

    public Integer getNumberOfVotes() {
        return numberOfVotes;
    }

    public Integer getNumberOfComments() {
        return numberOfComments;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<User> getPrivateUsers() {
        return privateUsers;
    }

    public List<User> getBlockedUsers() {
        return blockedUsers;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Timestamp getModified() {
        return modified;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public User getCreator() {
        return creator;
    }

    public Community getCommunity() {
        return community;
    }

    public User getModifiedByUser() {
        return modifiedByUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Discussion that = (Discussion) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}


/**
 * Created by tyler on 8/17/16.
 */
public enum DiscussionRole {
    CREATOR(1), USER(2), BLOCKED(3);

    int val;
    DiscussionRole(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}

import com.chat.db.Tables;
import com.chat.db.Transformations;
import com.chat.types.JSONWriter;
import org.javalite.activejdbc.Model;

import java.util.*;

/**
 * Created by tyler on 6/22/16.
 */
public class Discussions implements JSONWriter {
    private Set<Discussion> discussions;
    private Long count;

    private Discussions(Set<Discussion> discussions, Long count) {
        this.count = count;
        this.discussions = discussions;
    }

    public static Discussions create(List<? extends Model> discussions,
                                     List<Tables.CommunityNoTextView> communities,
                                     List<Tables.DiscussionTagView> discussionTags,
                                     List<Tables.DiscussionUserView> discussionUsers,
                                     List<Tables.DiscussionRank> discussionRanks,
                                     Long count) {

        // Build maps keyed by discussion_id of the votes, tags, and users
        Map<Long, Integer> votes = (discussionRanks != null) ?
                Transformations.convertRankToMap(discussionRanks, "discussion_id") : null;

        Map<Long, List<Tables.DiscussionTagView>> tagMap = (discussionTags != null) ?
                Transformations.convertRowsToMap(discussionTags, "discussion_id") : null;

        Map<Long, List<Tables.DiscussionUserView>> userMap = (discussionUsers != null) ?
                Transformations.convertRowsToMap(discussionUsers, "discussion_id") : null;

        // Convert to a list of discussion objects
        Set<Discussion> dos = new LinkedHashSet<>();

        for (Model view : discussions) {
            Long id = view.getLongId();
            Integer vote = (votes != null && votes.get(id) != null) ? votes.get(id) : null;
            List<Tables.DiscussionTagView> tags = (tagMap != null && tagMap.get(id) != null) ? tagMap.get(id) : null;
            List<Tables.DiscussionUserView> users = (userMap != null && userMap.get(id) != null) ? userMap.get(id) : null;
            Tables.CommunityNoTextView community = null;
            if (communities != null) {
                for (Tables.CommunityNoTextView cntv : communities) {
                    if (view.getLong("community_id").equals(cntv.getLongId())) {
                        community = cntv;
                        break;
                    }
                }
            }

            // TODO should the list of discussions also filter for blocked communities?
            Discussion df = Discussion.create(view, community, tags, users, null, vote);
            dos.add(df);
        }

        return new Discussions(dos, count);
    }

    public Long getCount() {
        return count;
    }

    public Set<Discussion> getDiscussions() {
        return discussions;
    }
}

import com.chat.types.JSONWriter;
import com.chat.webservice.ConstantsService;

/**
 * Created by tyler on 6/19/16.
 */
public class Tag implements JSONWriter {
    private Long id;
    private String name;

    private Tag(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Tag() {}

    public static Tag create(com.chat.db.Tables.Tag tag) {
        return new Tag(tag.getLongId(),
                tag.getString("name"));
    }

    public static Tag create(Long id, String name) {
        return new Tag(id, name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return ConstantsService.INSTANCE.replaceCensoredText(name);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tagObj = (Tag) o;

        if (id != null ? !id.equals(tagObj.id) : tagObj.id != null) return false;
        return name != null ? name.equals(tagObj.name) : tagObj.name == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}


import com.chat.types.JSONWriter;
import org.javalite.activejdbc.LazyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tyler on 6/23/16.
 */
public class Tags implements JSONWriter {
    private List<Tag> tags;

    private Tags(List<Tag> tags) {
        this.tags = tags;
    }

    public static Tags create(LazyList<com.chat.db.Tables.Tag> tags) {
        // Convert to a list of discussion objects
        List<Tag> tos = new ArrayList<>();

        for (com.chat.db.Tables.Tag tag : tags) {
            Tag to = Tag.create(tag);
            tos.add(to);
        }

        return new Tags(tos);
    }

    public List<Tag> getTags() {
        return tags;
    }
}


import java.io.IOException;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.chat.tools.Tools;
import com.chat.types.JSONWriter;
import com.chat.webservice.ConstantsService;

/**
 * Created by tyler on 6/10/16.
 */
public class User implements JSONWriter {
    private Long id;
    private String name, jwt;

    private UserSettings settings;

    private User(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public User() {}

    public static User create(com.chat.db.Tables.User user) {
        return new User(user.getLongId(),
                user.getString("name"));
    }

    public static User create(Long id, String name) {
        return new User(id, name);
    }

    public static User create(String jwt) {
        DecodedJWT dJWT = Tools.decodeJWTToken(jwt);
        return new User(
            Long.valueOf(dJWT.getClaim("user_id").asString()),
            dJWT.getClaim("user_name").asString());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return ConstantsService.INSTANCE.replaceCensoredText(name);
    }

    public UserSettings getSettings() {
        return settings;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    @Override
    public String toString() {
        return this.json();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User userObj = (User) o;

        if (id != null ? !id.equals(userObj.id) : userObj.id != null) return false;
        return name != null ? name.equals(userObj.name) : userObj.name == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public static User fromJson(String dataStr) {

        try {
            return Tools.JACKSON.readValue(dataStr, User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}


import com.chat.types.JSONWriter;
import org.javalite.activejdbc.LazyList;

import java.util.*;

/**
 * Created by tyler on 6/7/16.
 */
public class Users implements JSONWriter {
    public Set<User> users;

    private Users(Set<User> users) {
        this.users = users;
    }

    public static Users create(LazyList<com.chat.db.Tables.User> users) {
        // Convert to a list of discussion objects
        Set<User> uos = new LinkedHashSet<>();

        for (com.chat.db.Tables.User user : users) {
            User uo = User.create(user);
            uos.add(uo);
        }

        return new Users(uos);
    }

    public static Users create(Set<User> users) {
        return new Users(users);
    }

    public Set<User> getUsers() {
        return users;
    }
}

import com.chat.db.Tables;
import com.chat.types.JSONWriter;

/**
 * Created by tyler on 9/23/16.
 */
public class UserSettings implements JSONWriter {
  private String defaultViewTypeRadioValue, defaultSortTypeRadioValue, defaultCommentSortTypeRadioValue;
  private Boolean readOnboardAlert;
  private Theme theme;

  public UserSettings(String defaultViewTypeRadioValue, String defaultSortTypeRadioValue,
      String defaultCommentSortTypeRadioValue, Boolean readOnboardAlert, Theme theme) {
    this.defaultViewTypeRadioValue = defaultViewTypeRadioValue;
    this.defaultSortTypeRadioValue = defaultSortTypeRadioValue;
    this.defaultCommentSortTypeRadioValue = defaultCommentSortTypeRadioValue;
    this.readOnboardAlert = readOnboardAlert;
    this.theme = theme;
  }

  public UserSettings() {
  }

  public static UserSettings create(Tables.UserSetting uv) {

    return new UserSettings(ViewType.values()[uv.getInteger("default_view_type_id") - 1].getRadioValue(),
        SortType.values()[uv.getInteger("default_sort_type_id") - 1].getRadioValue(),
        CommentSortType.values()[uv.getInteger("default_comment_sort_type_id") - 1].getRadioValue(),
        uv.getBoolean("read_onboard_alert"),
        Theme.values()[uv.getInteger("theme")]
        );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    UserSettings that = (UserSettings) o;

    if (!defaultViewTypeRadioValue.equals(that.defaultViewTypeRadioValue))
      return false;
    if (!defaultSortTypeRadioValue.equals(that.defaultSortTypeRadioValue))
      return false;
    return readOnboardAlert.equals(that.readOnboardAlert);

  }

  @Override
  public int hashCode() {
    int result = defaultViewTypeRadioValue.hashCode();
    result = 31 * result + defaultSortTypeRadioValue.hashCode();
    result = 31 * result + readOnboardAlert.hashCode();
    return result;
  }

  public String getDefaultViewTypeRadioValue() {
    return defaultViewTypeRadioValue;
  }

  public String getDefaultSortTypeRadioValue() {
    return defaultSortTypeRadioValue;
  }

  public String getDefaultCommentSortTypeRadioValue() {
    return defaultCommentSortTypeRadioValue;
  }

  public Boolean getReadOnboardAlert() {
    return readOnboardAlert;
  }

  public Integer getTheme() {
    return theme.ordinal();
  }
}


public enum Theme {
  Dark, Light
}

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tyler on 9/23/16.
 */

// TODO alter the front end code to use numbers as the radio values, and then do the string inflation from this code
public enum CommentSortType {
    NEW(1, "new"),
    HOT(2, "hot"),
    TOP(3, "top");

    Integer val;
    String radioValue;
    CommentSortType(Integer val, String radioValue) {
        this.val = val;
        this.radioValue = radioValue;
    }

    public int getVal() {
        return val;
    }
    public String getRadioValue() { return radioValue;}

    public static CommentSortType getFromRadioValue(String radioValue) {
        Map<String, CommentSortType> sortTypeMap = new LinkedHashMap<>();

        for (CommentSortType st : CommentSortType.values()) {
            sortTypeMap.put(st.getRadioValue(), st);
        }

        return sortTypeMap.get(radioValue);
    }
}


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tyler on 9/23/16.
 */

// TODO alter the front end code to use numbers as the radio values, and then do the string inflation from this code
public enum SortType {
    NEW(1, "created__desc"),
    HOUR(2, "time-3600"),
    DAY(3, "time-86400"),
    WEEK(4, "time-604800"),
    MONTH(5, "time-2628000"),
    YEAR(6, "time-31540000"),
    ALLTIME(7, "number_of_votes__desc");

    Integer val;
    String radioValue;
    SortType(Integer val, String radioValue) {
        this.val = val;
        this.radioValue = radioValue;
    }

    public int getVal() {
        return val;
    }
    public String getRadioValue() { return radioValue;}


    public static SortType getFromRadioValue(String radioValue) {
        Map<String, SortType> sortTypeMap = new LinkedHashMap<>();

        for (SortType st : SortType.values()) {
            sortTypeMap.put(st.getRadioValue(), st);
        }

        return sortTypeMap.get(radioValue);
    }
}


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by tyler on 9/23/16.
 */
public enum ViewType {

    CARD(1, "card"), LIST(2, "list");

    Integer val;
    String radioValue;

    ViewType(int val, String radioValue) {
        this.val = val;
        this.radioValue = radioValue;
    }

    public int getVal() {
        return val;
    }
    public String getRadioValue() { return radioValue;}


    public static ViewType getFromRadioValue(String radioValue) {
        Map<String, ViewType> viewTypeMap = new LinkedHashMap<>();

        for (ViewType vt : ViewType.values()) {
            viewTypeMap.put(vt.getRadioValue(), vt);
        }

        return viewTypeMap.get(radioValue);
    }
}


import com.chat.tools.Tools;
import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 6/25/16.
 */
public class DeleteData implements JSONWriter {
    private Long deleteId;

    public DeleteData(Long deleteId) {
        this.deleteId = deleteId;
    }

    public Long getDeleteId() {
        return deleteId;
    }

}

import com.chat.tools.Tools;
import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 6/7/16.
 */
public class ReplyData implements JSONWriter {
    private Long parentId;
    private String reply;

    public ReplyData(Long parentId, String reply) {
        this.parentId = parentId;
        this.reply = reply;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getReply() {
        return reply;
    }

}


import com.chat.tools.Tools;
import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 6/13/16.
 */
public class CommentRankData implements JSONWriter {
    private Integer rank;
    private Long commentId;

    public CommentRankData(Integer rank, Long commentId) {
        this.rank = rank;
        this.commentId = commentId;
    }

    public Integer getRank() {
        return rank;
    }

    public Long getCommentId() {
        return commentId;
    }

}


import com.chat.tools.Tools;
import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 6/11/16.
 */
public class StickyData implements JSONWriter {
    private Long id;
    private Boolean sticky;

    public StickyData(Long id, Boolean sticky) {
        this.id = id;
        this.sticky = sticky;
    }

    public Long getId() {
        return id;
    }

    public Boolean getSticky() {
        return sticky;
    }
}

import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 7/11/16.
 */
public class NextPageData implements JSONWriter {
    private Long topLimit, maxDepth;

    public NextPageData(Long topLimit, Long maxDepth) {
        this.topLimit = topLimit;
        this.maxDepth = maxDepth;
    }

    public Long getTopLimit() {
        return topLimit;
    }
    public Long getMaxDepth() {return maxDepth;}

}



import com.chat.tools.Tools;
import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 6/11/16.
 */
public class EditData implements JSONWriter {
    private Long id;
    private String edit;

    public EditData(Long id, String edit) {
        this.id = id;
        this.edit = edit;
    }

    public Long getId() {
        return id;
    }

    public String getEdit() {
        return edit;
    }
}

import com.chat.tools.Tools;
import com.chat.types.JSONWriter;

import java.io.IOException;

/**
 * Created by tyler on 6/13/16.
 */
public class TopReplyData implements JSONWriter {
    private String topReply;

    public TopReplyData(String topReply) {
        this.topReply = topReply;
    }

    public String getTopReply() {
        return topReply;
    }

}

import com.chat.db.Transformations;
import com.chat.types.JSONWriter;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;

import java.util.*;

/**
 * Created by tyler on 6/7/16.
 */
public class Comments implements JSONWriter {
    private List<Comment> comments;

    private Comments(List<Comment> comments) {
        this.comments = comments;
    }

    public static Comments create(
            LazyList<? extends Model> comments,
            Map<Long, Integer> votes,
            Long topLimit, Long maxDepth, Comparator<Comment> comparator) {

        List<Comment> commentObjs = Transformations.convertCommentsToEmbeddedObjects(
                comments, votes, topLimit, maxDepth, comparator);

        return new Comments(commentObjs);
    }

    public static Comments replies(LazyList<? extends Model> comments) {
        Set<Comment> commentObjs = new LinkedHashSet<>();
        for (Model c : comments) {
            commentObjs.add(Comment.create(c, null));
        }

        // Convert to a list
        List<Comment> list = new ArrayList<>(commentObjs);

        return new Comments(list);

    }

    public List<Comment> getComments() {
        return comments;
    }

}


import com.chat.tools.Tools;
import com.chat.types.JSONWriter;
import com.chat.types.RankingConstants;
import com.chat.types.user.User;
import com.chat.webservice.ConstantsService;
import org.javalite.activejdbc.Model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by tyler on 6/7/16.
 */
public class Comment implements JSONWriter {
    private Long id, discussionId, discussionOwnerId, parentId, topParentId, parentUserId, pathLength, numOfParents,
            numOfChildren;
    private String text;
    private Timestamp created, modified;
    private List<Comment> embedded;
    private List<Long> breadcrumbs;
    private Integer avgRank, userRank, numberOfVotes;
    private Boolean deleted, read, stickied;

    private User user, modifiedByUser;

    public Comment(Long id, User user, User modifiedByUser, Long discussionId, Long discussionOwnerId, String text,
            Long pathLength, Long topParentId, Long parentUserId, String breadcrumbs, Long numOfParents,
            Long numOfChildren, Integer avgRank, Integer userRank, Integer numberOfVotes, Boolean deleted, Boolean read,
            Boolean stickied, Timestamp created, Timestamp modified) {
        this.id = id;
        this.user = user;
        this.modifiedByUser = modifiedByUser;
        this.topParentId = topParentId;
        this.parentUserId = parentUserId;
        this.text = text;
        this.discussionId = discussionId;
        this.discussionOwnerId = discussionOwnerId;
        this.numOfParents = numOfParents;
        this.numOfChildren = numOfChildren;
        this.avgRank = avgRank;
        this.userRank = userRank;
        this.pathLength = pathLength;
        this.created = created;
        this.modified = modified;
        this.numberOfVotes = numberOfVotes;
        this.deleted = deleted;
        this.read = read;
        this.stickied = stickied;

        this.embedded = new ArrayList<>();

        this.breadcrumbs = setBreadCrumbsArr(breadcrumbs);
        setParentId();

    }

    public static Comment create(Model cv, Integer vote) {

        User user = User.create(cv.getLong("user_id"), cv.getString("user_name"));
        User modifiedByUser = User.create(cv.getLong("modified_by_user_id"), cv.getString("modified_by_user_name"));

        return new Comment(cv.getLong("id"), user, modifiedByUser, cv.getLong("discussion_id"),
                cv.getLong("discussion_owner_id"), cv.getString("text_"), cv.getLong("path_length"),
                cv.getLong("parent_id"), cv.getLong("parent_user_id"), cv.getString("breadcrumbs"),
                cv.getLong("num_of_parents"), cv.getLong("num_of_children"), cv.getInteger("avg_rank"), vote,
                cv.getInteger("number_of_votes"), cv.getBoolean("deleted"), cv.getBoolean("read"),
                cv.getBoolean("stickied"), cv.getTimestamp("created"), cv.getTimestamp("modified"));
    }

    public static List<Long> setBreadCrumbsArr(String breadCrumbs) {
        List<Long> breadcrumbs = new ArrayList<>();
        for (String br : Tools.pgArrayAggToArray(breadCrumbs)) {
            breadcrumbs.add(Long.valueOf(br.replace("\"", "")));
        }
        return breadcrumbs;
    }

    private void setParentId() {
        Integer cIndex = breadcrumbs.indexOf(id);

        if (cIndex > 0) {
            parentId = breadcrumbs.get(cIndex - 1);
        }

    }

    public static Comment findInEmbeddedById(List<Comment> cos, Comment co) {
        Long id = co.getParentId();

        for (Comment c : cos) {
            if (c.getId() == id) {
                return c;
            }
        }

        return co;

    }

    public static class CommentObjComparatorHot implements Comparator<Comment> {

        @Override
        public int compare(Comment o1, Comment o2) {

            Integer stickyComp = stickyComparison(o1, o2);
            if (stickyComp != 0) {
                return stickyComp;
            }

            Double o1R = getRank(o1);
            Double o2R = getRank(o2);

            return o2R.compareTo(o1R);
        }

        private static Double getRank(Comment co) {

            RankingConstants rco = ConstantsService.INSTANCE.getRankingConstants();

            Double timeDifference = (new Date().getTime() - co.getCreated().getTime()) * 0.001;
            Double timeRank = rco.getCreatedWeight() / timeDifference;
            Double numberOfVotesRank = (co.getNumberOfVotes() != null)
                    ? co.getNumberOfVotes() * rco.getNumberOfVotesWeight()
                    : 0;
            Double avgScoreRank = (co.getAvgRank() != null) ? co.getAvgRank() * rco.getAvgRankWeight() : 0;
            Double rank = timeRank + numberOfVotesRank + avgScoreRank;

            return rank;

        }

    }

    public static class CommentObjComparatorNew implements Comparator<Comment> {
        @Override
        public int compare(Comment o1, Comment o2) {

            Integer stickyComp = stickyComparison(o1, o2);
            if (stickyComp != 0) {
                return stickyComp;
            }

            return o2.getCreated().compareTo(o1.getCreated());
        }
    }

    public static class CommentObjComparatorTop implements Comparator<Comment> {

        @Override
        public int compare(Comment o1, Comment o2) {

            Integer stickyComp = stickyComparison(o1, o2);
            if (stickyComp != 0) {
                return stickyComp;
            }


            Integer o1R = (o1.getAvgRank() != null) ? o1.getAvgRank() : 50;
            Integer o2R = (o2.getAvgRank() != null) ? o2.getAvgRank() : 50;

            return o2R.compareTo(o1R);
        }

    }

    public static Integer stickyComparison(Comment o1, Comment o2) {
        return o2.getStickied().compareTo(o1.getStickied());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Comment that = (Comment) o;

        if (!id.equals(that.id))
            return false;
        if (user.getId() != null ? !user.getId().equals(that.user.getId()) : that.user.getId() != null)
            return false;
        if (discussionId != null ? !discussionId.equals(that.discussionId) : that.discussionId != null)
            return false;

        return read != null ? read.equals(that.read) : that.read == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (user.getId() != null ? user.getId().hashCode() : 0);
        result = 31 * result + (discussionId != null ? discussionId.hashCode() : 0);
        return result;
    }

    public Integer getAvgRank() {
        return avgRank;
    }

    public Integer getUserRank() {
        return userRank;
    }

    public Integer getNumberOfVotes() {
        return numberOfVotes;
    }

    public Long getNumOfChildren() {
        return numOfChildren;
    }

    public Long getId() {
        return id;
    }

    public Long getDiscussionId() {
        return discussionId;
    }

    public Long getDiscussionOwnerId() {
        return discussionOwnerId;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getTopParentId() {
        return topParentId;
    }

    public Long getPathLength() {
        return pathLength;
    }

    public Long getNumOfParents() {
        return numOfParents;
    }

    public String getText() {
        return ConstantsService.INSTANCE.replaceCensoredText(text);
    }

    public Boolean getStickied() {
        return stickied;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Timestamp getModified() {
        return modified;
    }

    public List<Comment> getEmbedded() {
        return embedded;
    }

    public List<Long> getBreadcrumbs() {
        return breadcrumbs;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public Long getParentUserId() {
        return parentUserId;
    }

    public Boolean getRead() {
        return read;
    }

    public User getUser() {
        return user;
    }

    public User getModifiedByUser() {
        return modifiedByUser;
    }

}


import com.chat.db.Tables;
import com.chat.db.Transformations;
import com.chat.types.JSONWriter;
import org.javalite.activejdbc.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by tyler on 8/18/16.
 */
public class Communities implements JSONWriter {

    private List<Community> communities;
    private Long count;

    private Communities(List<Community> communities, Long count) {
        this.count = count;
        this.communities = communities;
    }

    public static Communities create(List<? extends Model> communities,
                                     List<Tables.CommunityTagView> communityTags,
                                     List<Tables.CommunityUserView> communityUsers,
                                     List<Tables.CommunityRank> communityRanks,
                                     Long count) {

        // Build maps keyed by community_id of the votes, tags, and users
        Map<Long, Integer> votes = (communityRanks != null) ?
                Transformations.convertRankToMap(communityRanks, "community_id") : null;

        Map<Long, List<Tables.CommunityTagView>> tagMap = (communityTags != null) ?
                Transformations.convertRowsToMap(communityTags, "community_id") : null;

        Map<Long, List<Tables.CommunityUserView>> userMap = (communityUsers != null) ?
                Transformations.convertRowsToMap(communityUsers, "community_id") : null;

        // Convert to a list of community objects
        List<Community> cos = new ArrayList<>();

        for (Model view : communities) {
            Long id = view.getLongId();
            Integer vote = (votes != null && votes.get(id) != null) ? votes.get(id) : null;
            List<Tables.CommunityTagView> tags = (tagMap != null && tagMap.get(id) != null) ? tagMap.get(id) : null;
            List<Tables.CommunityUserView> users = (userMap != null && userMap.get(id) != null) ? userMap.get(id) : null;
            Community c = Community.create(view, tags, users, vote);
            cos.add(c);
        }

        return new Communities(cos, count);
    }

    public Long getCount() {
        return count;
    }

    public List<Community> getCommunities() {
        return communities;
    }
}


/**
 * Created by tyler on 8/18/16.
 */
public enum CommunityRole {
    CREATOR(1), MODERATOR(2), USER(3), BLOCKED(4);

    int val;
    CommunityRole(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}


import com.chat.db.Tables;
import com.chat.tools.Tools;
import com.chat.types.JSONWriter;
import com.chat.types.tag.Tag;
import com.chat.types.user.User;
import com.chat.webservice.ConstantsService;
import org.javalite.activejdbc.Model;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by tyler on 8/18/16.
 */
public class Community implements JSONWriter {

    private Long id;
    private User creator, modifiedByUser;
    private String name, text;
    private Boolean private_, deleted, nsfw;
    private Integer avgRank, userRank, numberOfVotes;
    private List<Tag> tags;
    private List<User> moderators, privateUsers, blockedUsers;
    private Timestamp created, modified;

    public Community() {}

    public Community(Long id,
                     String name,
                     String text,
                     Boolean private_,
                     Boolean nsfw,
                     Integer avgRank,
                     Integer userRank,
                     Integer numberOfVotes,
                     List<Tag> tags,
                     User creator,
                     User modifiedByUser,
                     List<User> moderators,
                     List<User> privateUsers,
                     List<User> blockedUsers,
                     Boolean deleted,
                     Timestamp created,
                     Timestamp modified) {
        this.id = id;
        this.creator = creator;
        this.modifiedByUser = modifiedByUser;
        this.name = name;
        this.text = text;
        this.private_ = private_;
        this.nsfw = nsfw;
        this.avgRank = avgRank;
        this.userRank = userRank;
        this.numberOfVotes = numberOfVotes;
        this.tags = tags;
        this.creator = creator;
        this.moderators = moderators;
        this.privateUsers = privateUsers;
        this.blockedUsers = blockedUsers;
        this.deleted = deleted;
        this.created = created;
        this.modified = modified;


    }


    public void checkPrivate(User userObj) {
        if (getPrivate_().equals(true)) {
            if (!getCreator().equals(userObj) &&
                    !getModerators().contains(userObj) &&
                    !getPrivateUsers().contains(userObj)) {
                throw new NoSuchElementException("Private community, not allowed to view");
            }
        }
    }

    public void checkBlocked(User userObj) {
        if (getBlockedUsers().contains(userObj)) {
            throw new NoSuchElementException("You have been blocked from this community");
        }
    }



    public static Community create(Model c,
                                   List<Tables.CommunityTagView> communityTags,
                                   List<Tables.CommunityUserView> communityUsers,
                                   Integer vote) {
        // convert the tags
        List<Tag> tags = null;
        if (communityTags != null) {
            tags = new ArrayList<>();
            for (Tables.CommunityTagView dtv : communityTags) {
                tags.add(Tag.create(dtv.getLong("tag_id"), dtv.getString("name")));
            }
        }

        // convert the user community roles
        User creator = null;
        List<User> moderators = new ArrayList<>();
        List<User> privateUsers = new ArrayList<>();
        List<User> blockedUsers = new ArrayList<>();

        if (communityUsers != null) {
            for (Tables.CommunityUserView udv : communityUsers) {

                CommunityRole role = CommunityRole.values()[udv.getLong("community_role_id").intValue() - 1];

                User userObj = User.create(udv.getLong("user_id"), udv.getString("name"));

                switch (role) {
                    case CREATOR:
                        creator = userObj;
                        break;
                    case MODERATOR:
                        moderators.add(userObj);
                        break;
                    case BLOCKED:
                        blockedUsers.add(userObj);
                        break;
                    case USER:
                        privateUsers.add(userObj);
                        break;
                }
            }
        }

        // Create the modified by user
        User modifiedByUser = User.create(c.getLong("modified_by_user_id"), c.getString("modified_by_user_name"));

        return new Community(c.getLongId(),
                c.getString("name"),
                c.getString("text_"),
                c.getBoolean("private"),
                c.getBoolean("nsfw"),
                c.getInteger("avg_rank"),
                vote,
                c.getInteger("number_of_votes"),
                tags,
                creator,
                modifiedByUser,
                moderators,
                privateUsers,
                blockedUsers,
                c.getBoolean("deleted"),
                c.getTimestamp("created"),
                c.getTimestamp("modified"));
    }

    public static Community fromJson(String dataStr) {

        try {
            return Tools.JACKSON.readValue(dataStr, Community.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getText() {
        return ConstantsService.INSTANCE.replaceCensoredText(text);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return ConstantsService.INSTANCE.replaceCensoredText(name);
    }

    public List<User> getModerators() {return moderators;}

    public Boolean getPrivate_() {
        return private_;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public Integer getAvgRank() {
        return avgRank;
    }

    public Integer getUserRank() {
        return userRank;
    }

    public Integer getNumberOfVotes() {
        return numberOfVotes;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<User> getPrivateUsers() {
        return privateUsers;
    }

    public List<User> getBlockedUsers() {
        return blockedUsers;
    }

    public Timestamp getCreated() {
        return created;
    }

    public Timestamp getModified() {
        return modified;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public User getCreator() {return creator;}

    public User getModifiedByUser() {
        return modifiedByUser;
    }
}



import static spark.Spark.init;
import static spark.Spark.staticFiles;
import static spark.Spark.webSocket;

import java.io.File;

import com.chat.DataSources;
import com.chat.scheduled.ScheduledJobs;
import com.chat.tools.Tools;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import spark.Spark;

public class ChatService {

    static Logger log = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    @Option(name="-loglevel", usage="Sets the log level [INFO, DEBUG, etc.]")
    private String loglevel = "INFO";

    @Option(name="-ssl",usage="The location of the java keystore .jks file.")
    private File jks;

    @Option(name="-liquibase", usage="Run liquibase changeset")
    private Boolean liquibase = true;

    @Option(name="-reddit_import", usage="Fetch posts from reddit")
    private Boolean redditImport = false;

    public void doMain(String[] args) {

        parseArguments(args);

        log.setLevel(Level.toLevel(loglevel));
        log.getLoggerContext().getLogger("org.eclipse.jetty").setLevel(Level.OFF);
        log.getLoggerContext().getLogger("spark.webserver").setLevel(Level.OFF);

        if (jks != null) {
            Spark.secure(jks.getAbsolutePath(), "changeit", null,null);
            DataSources.SSL = true;
        }

        if (liquibase) {
            Tools.runLiquibase();
        }

        staticFiles.location("/dist");
        staticFiles.header("Content-Encoding", "gzip");
        staticFiles.expireTime(600);

        // Instantiates the ranking constants
        ConstantsService.INSTANCE.getRankingConstants();

        // Set up websocket
        webSocket("/threaded_chat", ThreadedChatWebSocket.class);

        // Set up endpoints
        Endpoints.status();
        Endpoints.user();
        Endpoints.discussion();
        Endpoints.community();
        Endpoints.tag();
        Endpoints.reply();

        init();

        if (redditImport) {
            ScheduledJobs.start();
        }

    }

    private void parseArguments(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java -jar reddit-history.jar [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            System.exit(0);

            return;
        }
    }

    public static void main(String[] args) throws Exception {
        new ChatService().doMain(args);
    }

}


import com.chat.db.Tables;
import com.chat.tools.Tools;
import com.chat.types.RankingConstants;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tyler on 7/5/16.
 */
public enum ConstantsService {
    INSTANCE;

    private RankingConstants rco;
    private String censoredRegex;

    ConstantsService() {

        // Need to write the sorting comments from the properties file to the DB
        // They are used both in code, and in the DB
        RankingConstants.writeRankingConstantsToDBFromPropertiesFile();

        // Fetch them from the DB
        rco = RankingConstants.fetchRankingConstants();

        censoredRegex = fetchCensoredWords();

    }

    public RankingConstants getRankingConstants() {
        return rco;
    }

    public String getCensoredRegex() { return censoredRegex; }

    public String replaceCensoredText(String s) {
        if (s != null) {
            return s.replaceAll(censoredRegex, "*removed*");
        } else {
            return null;
        }
    }

    private String fetchCensoredWords() {
        Tools.dbInit();
        Set<String> words = Tables.CensoredWord.findAll().collectDistinct("regex");
        Tools.dbClose();

        return String.join("|", words);
    }

}


import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.Map;
import java.util.Set;

import com.chat.DataSources;
import com.chat.db.Actions;
import com.chat.db.Tables;
import com.chat.db.Tables.UserSetting;
import com.chat.tools.Tools;
import com.chat.types.comment.Comments;
import com.chat.types.community.Communities;
import com.chat.types.community.Community;
import com.chat.types.community.CommunityRole;
import com.chat.types.discussion.Discussion;
import com.chat.types.discussion.Discussions;
import com.chat.types.tag.Tag;
import com.chat.types.tag.Tags;
import com.chat.types.user.User;
import com.chat.types.user.UserSettings;
import com.chat.types.user.Users;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.jetty.http.HttpStatus;
import org.javalite.activejdbc.LazyList;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Created by tyler on 7/29/16.
 */
public class Endpoints {

  public static Logger log = (Logger) LoggerFactory.getLogger(Endpoints.class);

  public static void status() {

    get("/version", (req, res) -> {
      return "{\"version\":\"" + DataSources.PROPERTIES.getProperty("version") + "\"}";
    });

    before((req, res) -> {
      res.header("Access-Control-Allow-Origin", "*");
      res.header("Access-Control-Allow-Credentials", "true");
      res.header("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, token, X-Requested-With");
      res.header("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
      Tools.dbInit();
    });

    after((req, res) -> {
      res.header("Content-Encoding", "gzip");
      Tools.dbClose();
    });

    exception(Exception.class, (e, req, res) -> {
      e.printStackTrace();
      log.error(req.uri());
      Tools.dbClose();
      res.status(HttpStatus.BAD_REQUEST_400);
      res.body(e.getMessage());
    });

    // exception(NullPointerException.class, (e, req, res) -> {
    // e.printStackTrace();
    // log.error(req.uri());
    // Tools.dbClose();
    // });

    // exception(NoSuchElementException.class, (e, req, res) -> {
    // e.printStackTrace();
    // log.error(req.uri());
    // Tools.dbClose();
    // });

  }

  public static void user() {

    post("/user", (req, res) -> {

      Map<String, String> vars = Tools.createMapFromReqBody(req.body());

      String name = vars.get("name");

      User user = (name != null) ? Actions.createNewSimpleUser(name) : Actions.createNewAnonymousUser();

      return user.getJwt();

    });

    post("/login", (req, res) -> {

      Map<String, String> vars = Tools.createMapFromReqBody(req.body());

      String userOrEmail = vars.get("usernameOrEmail");
      String password = vars.get("password");

      User userObj = Actions.login(userOrEmail, password);

      return userObj.getJwt();

    });

    post("/signup", (req, res) -> {

      Long userId = (req.headers("token") != null) ? Tools.getUserFromJWTHeader(req).getId() : null;

      Map<String, String> vars = Tools.createMapFromReqBody(req.body());

      String userName = vars.get("username");
      String password = vars.get("password");
      String verifyPassword = vars.get("verifyPassword");
      String email = vars.get("email");

      User userObj = Actions.signup(userId, userName, password, verifyPassword, email);

      return userObj.getJwt();

    });

    get("/user_search/:query", (req, res) -> {

      String query = req.params(":query");

      String queryStr = Tools.constructQueryString(query, "name");

      LazyList<Tables.User> userRows = Tables.User.find(queryStr.toString()).limit(5);

      Users users = Users.create(userRows);

      return users.json();

    });

    get("/user_log/:id", (req, res) -> {

      Long userId = Long.valueOf(req.params(":id"));

      LazyList<Tables.UserAuditView> auditRows = Tables.UserAuditView.find("user_id = ?", userId);

      String json = auditRows.toJson(false, "action", "action_tstamp", "comment_text", "community_id", "community_name",
          "discussion_id", "discussion_title", "id", "modified_by_user_id", "modified_by_user_name", "table_name",
          "user_id", "user_name", "role_id");

      // Converting with jackson corrects the double quote issues
      Tools.JACKSON.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

      JsonNode j = Tools.JACKSON.readTree(json);

      // json = json.replaceAll("\\\\", "\n").replaceAll("\n", "\\n");

      return j.toString();

    });

    get("/user_setting", (req, res) -> {
      User userObj = Tools.getUserFromJWTHeader(req);

      UserSetting us = UserSetting.findFirst("user_id = ?", userObj.getId());

      UserSettings uv = UserSettings.create(us);

      return uv.json();
    });

    put("/user_setting", (req, res) -> {
      User userObj = Tools.getUserFromJWTHeader(req);
      Map<String, String> vars = Tools.createMapFromReqBody(req.body());
      Actions.saveUserSettings(userObj.getId(), vars.get("defaultViewTypeRadioValue"),
          vars.get("defaultSortTypeRadioValue"), vars.get("defaultCommentSortTypeRadioValue"),
          Boolean.valueOf(vars.get("readOnboardAlert")), Integer.valueOf(vars.get("theme")));

      res.status(HttpStatus.OK_200);

      return "{}";
    });

  }

  public static void tag() {

    get("/tag/:id", (req, res) -> {

      Long id = Long.valueOf(req.params(":id"));

      Tables.Tag t = Tables.Tag.findFirst("id = ?", id);

      Tag to = Tag.create(t);

      return to.json();

    });

    get("/tag_search/:query", (req, res) -> {

      String query = req.params(":query");

      String queryStr = Tools.constructQueryString(query, "name");

      LazyList<Tables.Tag> tagRows = Tables.Tag.find(queryStr.toString()).limit(5);

      Tags tags = Tags.create(tagRows);

      return tags.json();

    });

    post("/tag", (req, res) -> {

      String name = Tools.createMapFromReqBody(req.body()).get("name");

      Tag to = Actions.createTag(name);

      res.status(HttpStatus.CREATED_201);

      return to.json();

    });

    get("/tags/:limit/:page/:orderBy", (req, res) -> {

      Integer limit = (req.params(":limit") != null) ? Integer.valueOf(req.params(":limit")) : 10;
      Integer page = (req.params(":page") != null) ? Integer.valueOf(req.params(":page")) : 1;
      String orderBy = (req.params(":orderBy") != null) ? req.params(":orderBy") : "custom";
      Integer offset = (page - 1) * limit;

      orderBy = Tools.constructOrderByPopularTagsCustom(orderBy);

      LazyList<Tables.TagsView> popularTags = Tables.TagsView.findAll().orderBy(orderBy).limit(limit).offset(offset);

      return popularTags.toJson(false);

    });

  }

  public static void discussion() {

    get("/discussion/:id", (req, res) -> {

      Long id = Long.valueOf(req.params(":id"));

      User userObj = Tools.getUserFromJWTHeader(req);

      Tables.DiscussionFullView dfv = Tables.DiscussionFullView.findFirst("id = ?", id);

      // Get your vote for the discussion:
      Tables.DiscussionRank dr = Tables.DiscussionRank.findFirst("discussion_id = ? and user_id = ?", id,
          userObj.getId());

      Integer vote = (dr != null) ? dr.getInteger("rank") : null;

      // Get the tags for those discussions:
      LazyList<Tables.DiscussionTagView> tags = Tables.DiscussionTagView.where("discussion_id = ?", id);

      // Get the users for those discussions
      LazyList<Tables.DiscussionUserView> users = Tables.DiscussionUserView.where("discussion_id = ?", id);

      Tables.CommunityNoTextView community = Tables.CommunityNoTextView.findFirst("id = ?",
          dfv.getLong("community_id"));

      // Get the users for that community
      LazyList<Tables.CommunityUserView> communityUsers = Tables.CommunityUserView.where("community_id = ?",
          community.getLong("id"));

      Discussion df = Discussion.create(dfv, community, tags, users, communityUsers, vote);

      // check to make sure user is entitled to view it
      df.checkPrivate(userObj);

      // Check to make sure user isn't blocked
      df.checkBlocked(userObj);

      // check to make sure user is entitled to view the community
      df.getCommunity().checkPrivate(userObj);

      // Check to make sure user is isn't blocked from the community
      df.getCommunity().checkBlocked(userObj);

      return df.json();

    });

    // Get the user id
    // A test query
    // select title, created, number_of_votes, avg_rank, ranking(created, 86400,
    // number_of_votes, 0.1, avg_rank, 0.01) from discussion_notext_view order by
    // ranking(created, 86400, number_of_votes, 0.1, avg_rank, 0.01) desc limit 200;
    get("/discussions/:tagId/:communityId/:limit/:page/:orderBy", (req, res) -> {

      Long tagId = (!req.params(":tagId").equals("all")) ? Long.valueOf(req.params(":tagId")) : null;
      Integer limit = (req.params(":limit") != null) ? Integer.valueOf(req.params(":limit")) : 10;
      Integer page = (req.params(":page") != null) ? Integer.valueOf(req.params(":page")) : 1;
      Integer offset = (page - 1) * limit;

      User userObj = Tools.getUserFromJWTHeader(req);

      Set<Long> communityIds = Tools.fetchCommunitiesFromParams(req.params(":communityId"), userObj);

      String orderBy = (req.params(":orderBy") != null) ? req.params(":orderBy")
          : "time-" + ConstantsService.INSTANCE.getRankingConstants().getCreatedWeight().intValue();

      Boolean singleCommunity = (req.params(":communityId") != null && !req.params(":communityId").equals("all")
          && !req.params(":communityId").equals("favorites"));

      orderBy = Tools.constructOrderByCustom(orderBy, singleCommunity);

      LazyList<Tables.DiscussionNoTextView> dntvs;
      // TODO refactor this to a communitiesQueryBuilder, same with discussion(don't
      // use parameterized anymore)
      if (tagId != null) {
        if (communityIds != null) {
          dntvs = Tables.DiscussionNoTextView
              .find("tag_ids @> ARRAY[?]::bigint[] " + "and community_id in " + Tools.convertListToInQuery(communityIds)
                  + " " + "and private is false and deleted is false and title != ?", tagId, "A new discussion")
              .orderBy(orderBy).limit(limit).offset(offset);
        } else {
          dntvs = Tables.DiscussionNoTextView
              .find("tag_ids @> ARRAY[?]::bigint[] " + "and private is false and deleted is false and title != ?",
                  tagId, "A new discussion")
              .orderBy(orderBy).limit(limit).offset(offset);
        }

      } else {
        if (communityIds != null) {
          dntvs = Tables.DiscussionNoTextView
              .find("community_id in " + Tools.convertListToInQuery(communityIds) + " "
                  + "and private is false and deleted is false and title != ?", "A new discussion")
              .orderBy(orderBy).limit(limit).offset(offset);
        }
        // Don't show nsfw in all
        else {
          dntvs = Tables.DiscussionNoTextView
              .find("private is false and deleted is false and nsfw is false and title != ?", "A new discussion")
              .orderBy(orderBy).limit(limit).offset(offset);
        }
      }

      log.debug(dntvs.toSql(true));

      Discussions discussions = null;
      if (!dntvs.isEmpty()) {

        // Get the list of discussions
        Set<Long> ids = dntvs.collectDistinct("id");

        // Get a list of the communities
        communityIds = dntvs.collectDistinct("community_id");

        // Get your votes for those discussions:
        LazyList<Tables.DiscussionRank> votes = Tables.DiscussionRank
            .where("discussion_id in " + Tools.convertListToInQuery(ids) + " and user_id = ?", userObj.getId());

        // Get the tags for those discussions:
        LazyList<Tables.DiscussionTagView> tags = Tables.DiscussionTagView
            .where("discussion_id in " + Tools.convertListToInQuery(ids));

        // Get the users for those discussions
        LazyList<Tables.DiscussionUserView> users = Tables.DiscussionUserView
            .where("discussion_id in " + Tools.convertListToInQuery(ids));

        // Get the communities for those discussions
        LazyList<Tables.CommunityNoTextView> communities = Tables.CommunityNoTextView
            .where("id in " + Tools.convertListToInQuery(communityIds));

        // Build discussion objects
        discussions = Discussions.create(dntvs, communities, tags, users, votes, 999L);

      } else {
        discussions = Discussions.create(dntvs, null, null, null, null, 999L);
      }

      return discussions.json();

    });

    get("/discussion_search/:query", (req, res) -> {

      String query = req.params(":query");

      String queryStr = Tools.constructQueryString(query, "title");

      LazyList<Tables.DiscussionNoTextView> discussionsRows = Tables.DiscussionNoTextView
          .find("deleted is false and " + queryStr.toString()).limit(5);

      Discussions discussions = Discussions.create(discussionsRows, null, null, null, null,
          Long.valueOf(discussionsRows.size()));

      return discussions.json();

    });

    post("/discussion_rank/:id/:rank", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long discussionId = Long.valueOf(req.params(":id"));

      Integer rank = (!req.params(":rank").equals("null")) ? Integer.valueOf(req.params(":rank")) : null;

      Actions.saveDiscussionVote(userObj.getId(), discussionId, rank);

      res.status(HttpStatus.OK_200);

      return "{}";

    });

    post("/discussion_blank", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Discussion do_ = Actions.createDiscussionEmpty(userObj.getId());

      res.status(HttpStatus.CREATED_201);

      return do_.json();

    });

    post("/discussion", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Discussion doIn = Discussion.fromJson(req.body());

      Discussion do_ = Actions.createDiscussion(userObj.getId(), doIn);

      res.status(HttpStatus.CREATED_201);

      return do_.json();

    });

    put("/discussion", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Discussion doIn = Discussion.fromJson(req.body());

      Discussion do_ = Actions.saveDiscussion(userObj.getId(), doIn);

      res.status(HttpStatus.OK_200);

      return do_.json();

    });

    get("/favorite_discussions", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      LazyList<Tables.FavoriteDiscussionUserView> favs = Tables.FavoriteDiscussionUserView
          .where("user_id = ? and deleted = ?", userObj.getId(), false);

      Set<Long> favDiscussionIds = favs.collectDistinct("discussion_id");

      String json = "";
      if (favDiscussionIds.size() > 0) {
        LazyList<Tables.DiscussionNoTextView> dntv = Tables.DiscussionNoTextView
            .where("id in " + Tools.convertListToInQuery(favDiscussionIds));

        Discussions d = Discussions.create(dntv, null, null, null, null, Long.valueOf(dntv.size()));

        json = d.json();
      } else {
        json = "{\"Discussions\": []}";
      }

      return json;

    });

    delete("/favorite_discussion/:id", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long discussionId = Long.valueOf(req.params(":id"));

      Actions.deleteFavoriteDiscussion(userObj.getId(), discussionId);

      res.status(HttpStatus.OK_200);

      return "{}";

    });
  }

  public static void reply() {

    get("/unread_replies", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      // Fetch your unread replies
      LazyList<Tables.CommentBreadcrumbsView> cbv = Tables.CommentBreadcrumbsView
          .where("parent_user_id = ? and user_id != ? and read = ?", userObj.getId(), userObj.getId(), false);

      Comments comments = Comments.replies(cbv);

      return comments.json();

    });

    post("/mark_reply_as_read/:id", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long commentId = Long.valueOf(req.params(":id"));

      // Mark the reply as read
      Actions.markReplyAsRead(commentId);

      res.status(HttpStatus.OK_200);

      return "{}";

    });

    post("/mark_all_replies_as_read", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      // Mark the reply as read
      Actions.markAllRepliesAsRead(userObj.getId());

      res.status(HttpStatus.OK_200);

      return "{}";

    });
  }

  public static void community() {

    get("/community/:id", (req, res) -> {

      Long id = Long.valueOf(req.params(":id"));

      User userObj = Tools.getUserFromJWTHeader(req);

      Tables.CommunityView cv = Tables.CommunityView.findFirst("id = ?", id);

      // Get your vote for the community:
      Tables.CommunityRank cr = Tables.CommunityRank.findFirst("community_id = ? and user_id = ?", id, userObj.getId());

      Integer vote = (cr != null) ? cr.getInteger("rank") : null;

      // Get the tags for that community:
      LazyList<Tables.CommunityTagView> tags = Tables.CommunityTagView.where("community_id = ?", id);

      // Get the users for that community
      LazyList<Tables.CommunityUserView> users = Tables.CommunityUserView.where("community_id = ?", id);

      Community co = Community.create(cv, tags, users, vote);

      // check to make sure user is entitled to view it
      co.checkPrivate(userObj);

      // Check to make sure user isn't blocked
      co.checkBlocked(userObj);

      return co.json();

    });

    // Get the user id
    get("/communities/:tagId/:limit/:page/:orderBy", (req, res) -> {

      Long tagId = (!req.params(":tagId").equals("all")) ? Long.valueOf(req.params(":tagId")) : null;
      Integer limit = (req.params(":limit") != null) ? Integer.valueOf(req.params(":limit")) : 10;
      Integer page = (req.params(":page") != null) ? Integer.valueOf(req.params(":page")) : 1;
      Integer offset = (page - 1) * limit;

      String orderBy = (req.params(":orderBy") != null) ? req.params(":orderBy")
          : "time-" + ConstantsService.INSTANCE.getRankingConstants().getCreatedWeight().intValue();

      orderBy = Tools.constructOrderByCustom(orderBy, false);
      User userObj = Tools.getUserFromJWTHeader(req);

      LazyList<Tables.CommunityNoTextView> cv;
      // TODO for now don't show where private is false
      if (tagId != null) {
        // fetch the tags
        cv = Tables.CommunityNoTextView
            .find("tag_ids @> ARRAY[?]::bigint[] " + "and private is false and deleted is false and name not like ?",
                tagId, "new_community%")
            .orderBy(orderBy).limit(limit).offset(offset);
      }
      // Don't fetch nsfw communities
      else {
        cv = Tables.CommunityNoTextView
            .find("private is false and deleted is false and nsfw is false and name not like ?", "new_community%")
            .orderBy(orderBy).limit(limit).offset(offset);
      }

      Communities communities;
      if (!cv.isEmpty()) {
        // Get the list of communities
        Set<Long> ids = cv.collectDistinct("id");

        // Get your votes for those communities:
        LazyList<Tables.CommunityRank> votes = Tables.CommunityRank
            .where("community_id in " + Tools.convertListToInQuery(ids) + " and user_id = ?", userObj.getId());

        // Get the tags for those communities:
        LazyList<Tables.CommunityTagView> tags = Tables.CommunityTagView
            .where("community_id in " + Tools.convertListToInQuery(ids));

        // Get the users for those communities
        LazyList<Tables.CommunityUserView> users = Tables.CommunityUserView
            .where("community_id in " + Tools.convertListToInQuery(ids));

        // Build community objects
        communities = Communities.create(cv, tags, users, votes, 999L);

      } else {
        communities = Communities.create(cv, null, null, null, 999L);
      }

      return communities.json();

    });

    get("/community_search/:query", (req, res) -> {

      String query = req.params(":query");

      String queryStr = Tools.constructQueryString(query, "name");

      LazyList<Tables.CommunityNoTextView> communityRows = Tables.CommunityNoTextView
          .find("deleted is false and " + queryStr.toString()).limit(5);

      Communities communities = Communities.create(communityRows, null, null, null, Long.valueOf(communityRows.size()));

      return communities.json();

    });

    post("/community_rank/:id/:rank", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long id = Long.valueOf(req.params(":id"));
      Integer rank = Integer.valueOf(req.params(":rank"));

      Actions.saveCommunityVote(userObj.getId(), id, rank);

      res.status(HttpStatus.OK_200);

      return "{}";

    });

    post("/community", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Community co_ = Actions.createCommunity(userObj.getId());

      res.status(HttpStatus.CREATED_201);

      return co_.json();

    });

    put("/community", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Community coIn = Community.fromJson(req.body());

      Community co_ = Actions.saveCommunity(userObj.getId(), coIn);

      res.status(HttpStatus.OK_200);

      return co_.json();

    });

    get("/favorite_communities", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      LazyList<Tables.CommunityUserView> favs = Tables.CommunityUserView.where(
          "user_id = ? and deleted = ? and community_role_id != ?", userObj.getId(), false,
          CommunityRole.BLOCKED.getVal());

      Set<Long> favCommunityIds = favs.collectDistinct("community_id");

      String json = "";
      if (favCommunityIds.size() > 0) {
        LazyList<Tables.CommunityNoTextView> dntv = Tables.CommunityNoTextView
            .where("id in " + Tools.convertListToInQuery(favCommunityIds));

        Communities d = Communities.create(dntv, null, null, null, Long.valueOf(dntv.size()));

        json = d.json();
      } else {
        json = "{\"Communities\": []}";
      }

      return json;

    });

    post("/favorite_community/:id", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long communityId = Long.valueOf(req.params(":id"));

      Actions.saveFavoriteCommunity(userObj.getId(), communityId);

      res.status(HttpStatus.OK_200);

      return "{}";

    });

    delete("/favorite_community/:id", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long communityId = Long.valueOf(req.params(":id"));

      Actions.deleteFavoriteCommunity(userObj.getId(), communityId);

      res.status(HttpStatus.OK_200);

      return "{}";

    });

    get("/community_modlog/:id", (req, res) -> {

      User userObj = Tools.getUserFromJWTHeader(req);

      Long id = Long.valueOf(req.params(":id"));

      LazyList<Tables.CommunityAuditView> auditRows = Tables.CommunityAuditView.find("community_id = ?", id);

      String json = auditRows.toJson(false, "action", "action_tstamp", "community_id", "discussion_id",
          "discussion_title", "id", "modified_by_user_id", "modified_by_user_name", "table_name", "user_id",
          "user_name", "role_id");

      return json;

    });

  }

}


import ch.qos.logback.classic.Logger;
import com.chat.db.Actions;
import com.chat.tools.Tools;
import com.chat.types.SessionScope;
import com.chat.types.comment.Comment;
import com.chat.types.comment.Comments;
import com.chat.types.discussion.Discussion;
import com.chat.types.user.User;
import com.chat.types.user.Users;
import com.chat.types.websocket.input.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.util.*;

import static com.chat.db.Tables.*;

/**
 * Created by tyler on 6/5/16.
 */

@WebSocket
public class ThreadedChatWebSocket {

  private static Long topLimit = 20L;
  private static Long maxDepth = 20L;

  public static Logger log = (Logger) LoggerFactory.getLogger(ThreadedChatWebSocket.class);

  static Set<SessionScope> sessionScopes = new HashSet<>();

  private static final Integer PING_DELAY = 10000;

  enum MessageType {
    Comments, Users, Edit, Reply, TopReply, Vote, Delete, NextPage, Sticky, SaveFavoriteDiscussion, Ping, Pong;
  }

  public ThreadedChatWebSocket() {
  }

  @OnWebSocketConnect
  public void onConnect(Session session) {

    try {
      Tools.dbInit();

      // Get or create the session scope
      SessionScope ss = setupSessionScope(session);

      sendRecurringPings(session);

      // Send them their user info
      // TODO
      // session.getRemote().sendString(ss.getUserObj().json("user"));

      LazyList<Model> comments = fetchComments(ss);

      // send the comments
      sendMessage(session,
          messageWrapper(MessageType.Comments, Comments
              .create(comments, fetchVotesMap(ss.getUserObj().getId()), topLimit, maxDepth, ss.getCommentComparator())
              .json()));

      // send the updated users to everyone in the right scope(just discussion)
      Set<SessionScope> filteredScopes = SessionScope.constructFilteredUserScopesFromSessionRequest(sessionScopes,
          session);
      broadcastMessage(filteredScopes,
          messageWrapper(MessageType.Users, Users.create(SessionScope.getUserObjects(filteredScopes)).json()));

      log.debug("session scope " + ss + " joined");

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Tools.dbClose();
    }

  }

  @OnWebSocketClose
  public void onClose(Session session, int statusCode, String reason) {

    SessionScope ss = SessionScope.findBySession(sessionScopes, session);
    sessionScopes.remove(ss);

    log.debug("session scope " + ss + " left, " + statusCode + " " + reason);

    // Send the updated users to everyone in the right scope
    Set<SessionScope> filteredScopes = SessionScope.constructFilteredUserScopesFromSessionRequest(sessionScopes,
        session);

    broadcastMessage(filteredScopes,
        messageWrapper(MessageType.Users, Users.create(SessionScope.getUserObjects(filteredScopes)).json()));

  }

  @OnWebSocketMessage
  public void onMessage(Session session, String dataStr) {
    try {
      Tools.dbInit();
      JsonNode node = null;
      node = Tools.JACKSON.readTree(dataStr);

      JsonNode data = node.get("data");

      switch (getMessageType(node)) {
      case Reply:
        messageReply(session, data);
        break;
      case Edit:
        messageEdit(session, data);
        break;
      case Sticky:
        messageSticky(session, data);
        break;
      case TopReply:
        messageTopReply(session, data);
        break;
      case Delete:
        messageDelete(session, data);
        break;
      case Vote:
        saveCommentVote(session, data);
        break;
      case NextPage:
        messageNextPage(session, data);
        break;
      case Pong:
        pongReceived(session, data);
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Tools.dbClose();
    }

  }

  public MessageType getMessageType(JsonNode node) {
    return MessageType.values()[node.get("message_type").asInt()];
  }

  public void messageNextPage(Session session, JsonNode data) {
    SessionScope ss = SessionScope.findBySession(sessionScopes, session);

    // Get the object
    NextPageData nextPageData = new NextPageData(data.get("topLimit").asLong(), data.get("maxDepth").asLong());

    // Refetch the comments based on the new limit
    LazyList<Model> comments = fetchComments(ss);

    // send the comments from up to the new limit to them
    sendMessage(session,
        messageWrapper(MessageType.Comments, Comments.create(comments, fetchVotesMap(ss.getUserObj().getId()),
            nextPageData.getTopLimit(), nextPageData.getMaxDepth(), ss.getCommentComparator()).json()));

  }

  public void messageReply(Session session, JsonNode data) {

    SessionScope ss = SessionScope.findBySession(sessionScopes, session);

    // Get the object
    ReplyData replyData = new ReplyData(data.get("parentId").asLong(), data.get("reply").asText());

    // Collect only works on refetch
    LazyList<Model> comments = fetchComments(ss);

    log.debug(ss.toString());

    // Necessary for comment tree
    Array arr = (Array) comments.collect("breadcrumbs", "id", replyData.getParentId()).get(0);
    List<Long> parentBreadCrumbs = Tools.convertArrayToList(arr);

    com.chat.db.Tables.Comment newComment = Actions.createComment(ss.getUserObj().getId(), ss.getDiscussionId(),
        parentBreadCrumbs, replyData.getReply());

    // Fetch the comment threaded view
    CommentThreadedView ctv = CommentThreadedView.findFirst("id = ?", newComment.getLongId());

    // Convert to a proper commentObj
    Comment co = Comment.create(ctv, null);

    Set<SessionScope> filteredScopes = SessionScope.constructFilteredMessageScopesFromSessionRequest(sessionScopes,
        session, co.getBreadcrumbs());

    broadcastMessage(filteredScopes, messageWrapper(MessageType.Reply, co.json()));

    // TODO find a way to do this without having to query every time?
    com.chat.types.discussion.Discussion do_ = Actions.saveFavoriteDiscussion(ss.getUserObj().getId(),
        ss.getDiscussionId());
    if (do_ != null)
      sendMessage(session, messageWrapper(MessageType.SaveFavoriteDiscussion, do_.json()));
  }

  public void messageEdit(Session session, JsonNode data) {

    SessionScope ss = SessionScope.findBySession(sessionScopes, session);

    EditData editData = new EditData(data.get("id").asLong(), data.get("edit").asText());

    com.chat.db.Tables.Comment c = Actions.editComment(ss.getUserObj().getId(), editData.getId(), editData.getEdit());

    CommentThreadedView ctv = CommentThreadedView.findFirst("id = ?", c.getLongId());

    // Convert to a proper commentObj, but with nothing embedded
    Comment co = Comment.create(ctv, null);

    Set<SessionScope> filteredScopes = SessionScope.constructFilteredMessageScopesFromSessionRequest(sessionScopes,
        session, co.getBreadcrumbs());

    broadcastMessage(filteredScopes, messageWrapper(MessageType.Edit, co.json()));

  }

  public void messageSticky(Session session, JsonNode data) {

    StickyData stickyData = new StickyData(data.get("id").asLong(), data.get("sticky").asBoolean());

    com.chat.db.Tables.Comment c = Actions.stickyComment(stickyData.getId(), stickyData.getSticky());

    CommentThreadedView ctv = CommentThreadedView.findFirst("id = ?", c.getLongId());

    // Convert to a proper commentObj, but with nothing embedded
    Comment co = Comment.create(ctv, null);

    Set<SessionScope> filteredScopes = SessionScope.constructFilteredMessageScopesFromSessionRequest(sessionScopes,
        session, co.getBreadcrumbs());

    // Send an edit with the sticky info
    broadcastMessage(filteredScopes, messageWrapper(MessageType.Edit, co.json()));
  }

  public void messageDelete(Session session, JsonNode data) {

    SessionScope ss = SessionScope.findBySession(sessionScopes, session);

    DeleteData deleteData = new DeleteData(data.get("deleteId").asLong());

    com.chat.db.Tables.Comment c = Actions.deleteComment(ss.getUserObj().getId(), deleteData.getDeleteId());

    CommentThreadedView ctv = CommentThreadedView.findFirst("id = ?", c.getLongId());

    // Convert to a proper commentObj, but with nothing embedded
    Comment co = Comment.create(ctv, null);

    Set<SessionScope> filteredScopes = SessionScope.constructFilteredMessageScopesFromSessionRequest(sessionScopes,
        session, co.getBreadcrumbs());

    broadcastMessage(filteredScopes, messageWrapper(MessageType.Delete, co.json()));

  }

  public void messageTopReply(Session session, JsonNode data) {

    SessionScope ss = SessionScope.findBySession(sessionScopes, session);

    // Get the object
    TopReplyData topReplyData = new TopReplyData(data.get("topReply").asText());

    com.chat.db.Tables.Comment newComment = Actions.createComment(ss.getUserObj().getId(), ss.getDiscussionId(), null,
        topReplyData.getTopReply());

    // Fetch the comment threaded view
    CommentThreadedView ctv = CommentThreadedView.findFirst("id = ?", newComment.getLongId());

    // Convert to a proper commentObj
    Comment co = Comment.create(ctv, null);

    Set<SessionScope> filteredScopes = SessionScope.constructFilteredMessageScopesFromSessionRequest(sessionScopes,
        session, co.getBreadcrumbs());

    broadcastMessage(filteredScopes, messageWrapper(MessageType.TopReply, co.json()));

    // TODO find a way to do this without having to query every time?
    Discussion do_ = Actions.saveFavoriteDiscussion(ss.getUserObj().getId(), ss.getDiscussionId());
    if (do_ != null)
      sendMessage(session, messageWrapper(MessageType.SaveFavoriteDiscussion, do_.json()));

  }

  public static void saveCommentVote(Session session, JsonNode data) {

    SessionScope ss = SessionScope.findBySession(sessionScopes, session);

    // Get the object
    CommentRankData commentRankData = new CommentRankData(data.get("rank").asInt(), data.get("commentId").asLong());

    Long userId = ss.getUserObj().getId();
    log.debug(userId.toString());
    Long commentId = commentRankData.getCommentId();
    Integer rank = commentRankData.getRank();

    String message = Actions.saveCommentVote(userId, commentId, rank);

    // Getting the comment for the breadcrumbs for the scope
    CommentThreadedView ctv = CommentThreadedView.findFirst("id = ?", commentId);

    // Convert to a proper commentObj, but with nothing embedded
    Comment co = Comment.create(ctv, null);

    Set<SessionScope> filteredScopes = SessionScope.constructFilteredMessageScopesFromSessionRequest(sessionScopes,
        session, co.getBreadcrumbs());

    // This sends an edit, which contains the average rank
    broadcastMessage(filteredScopes, messageWrapper(MessageType.Edit, co.json()));

  }

  // Sends a message from one user to all users
  // TODO need to get subsets of sessions based on discussion_id, and parent_id
  // Maybe Map<discussion_id, List<sessions>

  public static void broadcastMessage(Set<SessionScope> filteredScopes, String json) {
    SessionScope.getSessions(filteredScopes).stream().filter(Session::isOpen).forEach(session -> {
      try {
        session.getRemote().sendString(json);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public static void sendMessage(Session session, String json) {
    try {
      session.getRemote().sendString(json);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private SessionScope setupSessionScope(Session session) {

    User userObj = SessionScope.getUserFromSession(session);
    Long discussionId = SessionScope.getDiscussionIdFromSession(session);
    Long topParentId = SessionScope.getTopParentIdFromSession(session);
    String sortType = SessionScope.getSortTypeFromSession(session);

    log.debug(userObj.json());

    SessionScope ss = new SessionScope(session, userObj, discussionId, topParentId, sortType);
    sessionScopes.add(ss);

    return ss;

  }

  private static LazyList<Model> fetchComments(SessionScope scope) {
    if (scope.getTopParentId() != null) {
      return CommentBreadcrumbsView.where("discussion_id = ? and parent_id = ?", scope.getDiscussionId(),
          scope.getTopParentId());
    } else {
      return CommentThreadedView.where("discussion_id = ?", scope.getDiscussionId());
    }
  }

  // These create maps from a user's comment id, to their rank/vote
  private static Map<Long, Integer> fetchVotesMap(Long userId) {
    List<CommentRank> ranks = CommentRank.where("user_id = ?", userId);

    return convertCommentRanksToVoteMap(ranks);
  }

  private static Map<Long, Integer> fetchVotesMap(Long userId, Long commentId) {
    List<CommentRank> ranks = CommentRank.where("comment_id = ? and user_id = ?", commentId, userId);

    return convertCommentRanksToVoteMap(ranks);

  }

  private static Map<Long, Integer> convertCommentRanksToVoteMap(List<CommentRank> ranks) {
    Map<Long, Integer> map = new HashMap<>();

    for (CommentRank rank : ranks) {
      map.put(rank.getLong("comment_id"), rank.getInteger("rank"));
    }
    return map;
  }

  private void sendRecurringPings(Session session) {
    final Timer timer = new Timer();
    final TimerTask tt = new TimerTask() {
      @Override
      public void run() {
        if (session.isOpen()) {
          sendMessage(session, messageWrapper(MessageType.Ping, "{\"ping\":\"ping\"}"));
        } else {
          timer.cancel();
          timer.purge();
        }
      }
    };

    timer.scheduleAtFixedRate(tt, PING_DELAY, PING_DELAY);
  }

  private void pongReceived(Session session, JsonNode pongStr) {
    log.debug("Pong received from " + session.getRemoteAddress());
  }

  private static String messageWrapper(MessageType type, String data) {
    return "{\"message_type\":" + type.ordinal() + ",\"data\":" + data + "}";
  }

}


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.auth0.jwt.JWT;
import com.chat.db.Tables.Comment;
import com.chat.db.Tables.CommentBreadcrumbsView;
import com.chat.db.Tables.CommentRank;
import com.chat.db.Tables.CommentTree;
import com.chat.db.Tables.CommunityNoTextView;
import com.chat.db.Tables.CommunityRank;
import com.chat.db.Tables.CommunityTag;
import com.chat.db.Tables.CommunityTagView;
import com.chat.db.Tables.CommunityUser;
import com.chat.db.Tables.CommunityUserView;
import com.chat.db.Tables.CommunityView;
import com.chat.db.Tables.DiscussionFullView;
import com.chat.db.Tables.DiscussionNoTextView;
import com.chat.db.Tables.DiscussionRank;
import com.chat.db.Tables.DiscussionTag;
import com.chat.db.Tables.DiscussionTagView;
import com.chat.db.Tables.DiscussionUser;
import com.chat.db.Tables.DiscussionUserView;
import com.chat.db.Tables.FavoriteDiscussionUser;
import com.chat.db.Tables.UserSetting;
import com.chat.tools.Tools;
import com.chat.types.community.Community;
import com.chat.types.community.CommunityRole;
import com.chat.types.discussion.Discussion;
import com.chat.types.discussion.DiscussionRole;
import com.chat.types.tag.Tag;
import com.chat.types.user.CommentSortType;
import com.chat.types.user.SortType;
import com.chat.types.user.Theme;
import com.chat.types.user.User;
import com.chat.types.user.ViewType;

import org.javalite.activejdbc.LazyList;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Created by tyler on 6/5/16.
 */
public class Actions {

  public static Logger log = (Logger) LoggerFactory.getLogger(Actions.class);

  public static Comment createComment(Long userId, Long discussionId, List<Long> parentBreadCrumbs, String text) {

    List<Long> pbs = (parentBreadCrumbs != null) ? new ArrayList<Long>(parentBreadCrumbs) : new ArrayList<Long>();

    // find the candidate
    Comment c = Comment.createIt("discussion_id", discussionId, "text_", text, "user_id", userId, "modified_by_user_id",
        userId);

    Long childId = c.getLong("id");

    // This is necessary, because of the 0 path length to itself one
    pbs.add(childId);

    Collections.reverse(pbs);

    // Create the comment_tree
    for (int i = 0; i < pbs.size(); i++) {

      Long parentId = pbs.get(i);

      // i is the path length
      CommentTree.createIt("parent_id", parentId, "child_id", childId, "path_length", i);
    }

    return c;

  }

  public static Comment editComment(Long userId, Long commentId, String text) {

    // Find the comment
    Comment c = Comment.findFirst("id = ?", commentId);

    Timestamp cTime = new Timestamp(new Date().getTime());

    // Create with add modified date
    c.set("text_", text, "modified", cTime, "modified_by_user_id", userId).saveIt();

    return c;
  }

  public static Comment stickyComment(Long commentId, Boolean stickied) {

    Comment c = Comment.findFirst("id = ?", commentId);

    c.set("stickied", stickied).saveIt();

    return c;
  }

  public static Comment deleteComment(Long userId, Long commentId) {
    // Find the comment
    Comment c = Comment.findFirst("id = ?", commentId);

    Timestamp cTime = new Timestamp(new Date().getTime());

    // Create with add modified date
    c.set("deleted", true, "modified", cTime, "modified_by_user_id", userId).saveIt();

    return c;
  }

  public static void saveUserSettings(Long userId, String defaultViewTypeRadioValue, String defaultSortTypeRadioValue,
      String defaultCommentSortTypeRadioValue, Boolean readOnboardAlert, Integer theme) {

    UserSetting us = UserSetting.findFirst("user_id = ?", userId);

    if (defaultViewTypeRadioValue != null)
      us.setInteger("default_view_type_id", ViewType.getFromRadioValue(defaultViewTypeRadioValue).getVal());
    if (defaultSortTypeRadioValue != null)
      us.setInteger("default_sort_type_id", SortType.getFromRadioValue(defaultSortTypeRadioValue).getVal());
    if (defaultCommentSortTypeRadioValue != null)
      us.setInteger("default_comment_sort_type_id",
          CommentSortType.getFromRadioValue(defaultCommentSortTypeRadioValue).getVal());
    if (readOnboardAlert != null)
      us.setBoolean("read_onboard_alert", readOnboardAlert);
    if (theme != null)
      us.setInteger("theme", theme);

    us.saveIt();
  }

  public static Discussion createDiscussionEmpty(Long userId) {

    log.debug("Creating discussion");
    String title = "A new discussion";

    Tables.Discussion d = Tables.Discussion.createIt("title", title, "modified_by_user_id", userId);

    DiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"), "discussion_role_id",
        DiscussionRole.CREATOR.getVal());

    FavoriteDiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"));

    DiscussionFullView dfv = DiscussionFullView.findFirst("id = ?", d.getLongId());
    List<DiscussionUserView> udv = DiscussionUserView.where("discussion_id = ?", d.getLongId());
    CommunityNoTextView cntv = CommunityNoTextView.findFirst("id = ?", dfv.getLong("community_id"));

    return Discussion.create(dfv, cntv, null, udv, null, null);
  }

  public static Discussion createDiscussion(Long userId, Discussion do_) {
    Tables.Discussion d = Tables.Discussion.createIt("title", do_.getTitle(), "modified_by_user_id", userId,
        "community_id", do_.getCommunity().getId(), "link", do_.getLink());

    DiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"), "discussion_role_id",
        DiscussionRole.CREATOR.getVal());

    FavoriteDiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"));

    do_.setId(d.getLong("id"));
    
    // Add the discussion tags
    if (do_.getTags() != null) {
      diffCreateOrDeleteDiscussionTags(do_);
    }

    DiscussionFullView dfv = DiscussionFullView.findFirst("id = ?", d.getLongId());
    List<DiscussionUserView> udv = DiscussionUserView.where("discussion_id = ?", d.getLongId());
    CommunityNoTextView cntv = CommunityNoTextView.findFirst("id = ?", dfv.getLong("community_id"));

    return Discussion.create(dfv, cntv, null, udv, null, null);
  }

  public static void createCommunityChat(Community c) {

    String title = "chat";
    Long userId = c.getCreator().getId();

    Tables.Discussion d = Tables.Discussion.createIt("title", title, "modified_by_user_id", userId, "community_id",
        c.getId(), "stickied", true);

    DiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"), "discussion_role_id",
        DiscussionRole.CREATOR.getVal());

    FavoriteDiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"));

  }

  public static Discussion saveDiscussion(Long userId, Discussion do_) {

    Timestamp cTime = new Timestamp(new Date().getTime());

    Tables.Discussion d = Tables.Discussion.findFirst("id = ?", do_.getId());
    LazyList<DiscussionUserView> udv = DiscussionUserView.where("discussion_id = ?", do_.getId());

    log.debug(udv.toJson(true));
    log.debug(do_.json());

    if (do_.getTitle() != null)
      d.set("title", do_.getTitle());
    if (do_.getLink() != null)
      d.set("link", do_.getLink());
    if (do_.getText() != null)
      d.set("text_", do_.getText());
    if (do_.getPrivate_() != null)
      d.set("private", do_.getPrivate_());
    if (do_.getNsfw() != null)
      d.set("nsfw", do_.getNsfw());
    if (do_.getStickied() != null)
      d.set("stickied", do_.getStickied());
    if (do_.getDeleted() != null)
      d.set("deleted", do_.getDeleted());
    if (do_.getCommunity() != null)
      d.set("community_id", do_.getCommunity().getId());

    d.set("modified_by_user_id", userId);
    d.set("modified", cTime);
    d.saveIt();

    // Add the discussion tags
    if (do_.getTags() != null) {
      diffCreateOrDeleteDiscussionTags(do_);
    }

    if (do_.getPrivateUsers() != null) {
      diffCreateOrDeleteDiscussionUsers(do_.getId(), do_.getPrivateUsers(), DiscussionRole.USER);
    }

    if (do_.getBlockedUsers() != null) {
      diffCreateOrDeleteDiscussionUsers(do_.getId(), do_.getBlockedUsers(), DiscussionRole.BLOCKED);
    }

    // Fetch the full view
    DiscussionFullView dfv = DiscussionFullView.findFirst("id = ?", do_.getId());
    List<DiscussionTagView> dtv = DiscussionTagView.where("discussion_id = ?", do_.getId());
    List<DiscussionUserView> ud = DiscussionUserView.where("discussion_id = ?", do_.getId());
    CommunityNoTextView cntv = CommunityNoTextView.findFirst("id = ?", dfv.getLong("community_id"));

    List<Tables.CommunityUserView> communityUsers = Tables.CommunityUserView.where("community_id = ?",
        cntv.getLong("id"));

    Discussion doOut = Discussion.create(dfv, cntv, dtv, ud, communityUsers, null);

    return doOut;
  }

  private static void diffCreateOrDeleteDiscussionUsers(Long discussionId, List<User> users, DiscussionRole role) {
    Set<Long> postUserIds = users.stream().map(user -> user.getId()).collect(Collectors.toSet());

    Set<Long> dbUserIds = DiscussionUser
        .where("discussion_id = ? and discussion_role_id = ?", discussionId, role.getVal()).collectDistinct("user_id");

    Set<Long> diffPostUserIds = new LinkedHashSet<>(postUserIds);
    Set<Long> diffDbTagIds = new LinkedHashSet<>(dbUserIds);

    diffPostUserIds.removeAll(dbUserIds);
    diffDbTagIds.removeAll(postUserIds);

    // Delete everything in the DB, that's not posted.
    if (!diffDbTagIds.isEmpty()) {
      DiscussionUser.delete(
          "discussion_id = ? and discussion_role_id = ? and user_id in " + Tools.convertListToInQuery(diffDbTagIds),
          discussionId, role.getVal());
    }

    for (Long uId : diffPostUserIds) {
      DiscussionUser.createIt("discussion_id", discussionId, "user_id", uId, "discussion_role_id", role.getVal());
    }
  }

  private static void diffCreateOrDeleteDiscussionTags(Discussion do_) {
    Set<Long> postTagIds = do_.getTags().stream().map(tag -> tag.getId()).collect(Collectors.toSet());

    // Fetch the existing community tags from the DB
    Set<Long> dbTagIds = DiscussionTag.where("discussion_id = ?", do_.getId()).collectDistinct("tag_id");

    Set<Long> diffPostTagIds = new LinkedHashSet<>(postTagIds);
    Set<Long> diffDbTagIds = new LinkedHashSet<>(dbTagIds);

    diffPostTagIds.removeAll(dbTagIds);
    diffDbTagIds.removeAll(postTagIds);

    // Delete everything in the DB, that's not posted.
    if (!diffDbTagIds.isEmpty()) {
      DiscussionTag.delete("discussion_id = ? and tag_id in " + Tools.convertListToInQuery(diffDbTagIds), do_.getId());
    }

    // Add everything posted, thats not in the db
    for (Long tagId : diffPostTagIds) {
      DiscussionTag.createIt("discussion_id", do_.getId(), "tag_id", tagId);
    }
  }

  public static Tag createTag(String name) {

    Tables.Tag t = Tables.Tag.createIt("name", name);

    return Tag.create(t);
  }

  public static Discussion saveFavoriteDiscussion(Long userId, Long discussionId) {

    FavoriteDiscussionUser fdu = FavoriteDiscussionUser.findFirst("user_id = ? and discussion_id = ?", userId,
        discussionId);

    if (fdu == null) {
      FavoriteDiscussionUser.createIt("user_id", userId, "discussion_id", discussionId);

      DiscussionNoTextView dntv = DiscussionNoTextView.findFirst("id = ?", discussionId);

      return Discussion.create(dntv, null, null, null, null, null);
    } else {
      return null;
    }

  }

  public static void deleteFavoriteDiscussion(Long userId, Long discussionId) {

    FavoriteDiscussionUser fdu = FavoriteDiscussionUser.findFirst("user_id = ? and discussion_id = ?", userId,
        discussionId);

    fdu.delete();

  }

  public static void markReplyAsRead(Long commentId) {

    Comment c = Comment.findFirst("id = ?", commentId);
    c.set("read", true).saveIt();

  }

  public static void markAllRepliesAsRead(Long userId) {

    // Fetch your unread replies
    LazyList<CommentBreadcrumbsView> cbv = CommentBreadcrumbsView
        .where("parent_user_id = ? and user_id != ? and read = false", userId, userId);

    Set<Long> ids = cbv.collectDistinct("id");

    if (ids.size() > 0) {

      String inQuery = Tools.convertListToInQuery(ids);

      Comment.update("read = ?", "id in " + inQuery, true);

    }

  }

  public static User createNewSimpleUser(String name) {
    try {
      Tables.User user = Tables.User.createIt("name", name);
      UserSetting.createIt("user_id", user.getLongId());
      return createUserObj(user, false);
    } catch (Exception e) {
      throw new NoSuchElementException("User already exists.");
    }

  }

  public static User createNewAnonymousUser() {
    Long lastId = Tables.User.findAll().orderBy("id desc").limit(1).get(0).getLongId();
    String userName = "user_" + ++lastId;
    return createNewSimpleUser(userName);
  }

  public static User login(String userOrEmail, String password) {

    // Find the user, then create a login for them

    Tables.User dbUser = Tables.User.findFirst("name = ? or email = ?", userOrEmail, userOrEmail);

    if (dbUser == null) {
      throw new NoSuchElementException("Incorrect user/email");
    } else {

      String encryptedPassword = dbUser.getString("password_encrypted");
      Boolean correctPass = Tools.PASS_ENCRYPT.checkPassword(password, encryptedPassword);

      if (correctPass) {
        return createUserObj(dbUser, true);
      } else {
        throw new NoSuchElementException("Incorrect Password");
      }
    }
  }

  public static User signup(Long loggedInUserId, String userName, String password, String verifyPassword,
      String email) {

    if (email != null && email.equals("")) {
      email = null;
    }

    if (!password.equals(verifyPassword)) {
      throw new NoSuchElementException("Passwords are different");
    }

    // Find the user, then create a login for them
    Tables.User uv;
    if (email != null) {
      uv = Tables.User.findFirst("name = ? or email = ?", userName, email);
    } else {
      uv = Tables.User.findFirst("name = ?", userName);
    }

    if (uv == null) {

      // Create the user and full user
      Tables.User user = Tables.User.createIt("name", userName);

      String encryptedPassword = Tools.PASS_ENCRYPT.encryptPassword(password);

      user.set("password_encrypted", encryptedPassword, "email", email).saveIt();
      UserSetting.createIt("user_id", user.getLongId());
      return createUserObj(user, true);

    } else if (loggedInUserId.equals(uv.getLongId())) {

      String encryptedPassword = Tools.PASS_ENCRYPT.encryptPassword(password);
      uv.set("password_encrypted", encryptedPassword, "email", email).saveIt();
      return createUserObj(uv, true);

    } else {
      throw new NoSuchElementException("Username/email already exists");
    }

  }

  public static String saveCommentVote(Long userId, Long commentId, Integer rank) {

    String message = null;
    // fetch the vote if it exists
    CommentRank c = CommentRank.findFirst("user_id = ? and comment_id = ?", userId, commentId);

    if (c == null) {
      if (rank != null) {
        CommentRank.createIt("comment_id", commentId, "user_id", userId, "rank", rank);
        message = "Comment Vote Created";
      } else {
        message = "Comment Vote not created";
      }
    } else {
      if (rank != null) {
        c.set("rank", rank).saveIt();
        message = "Comment Vote updated";
      }
      // If the rank is null, then delete the ballot
      else {
        c.delete();
        message = "Comment Vote deleted";
      }
    }

    return message;

  }

  public static void saveDiscussionVote(Long userId, Long discussionId, Integer rank) {

    // fetch the vote if it exists
    DiscussionRank d = DiscussionRank.findFirst("user_id = ? and discussion_id = ?", userId, discussionId);

    if (rank != null) {
      if (d == null) {
        DiscussionRank.createIt("discussion_id", discussionId, "user_id", userId, "rank", rank);
      } else {
        d.set("rank", rank).saveIt();
      }
    }
    // If the rank is null, then delete the ballot
    else {
      d.delete();
    }

  }

  public static void saveCommunityVote(Long userId, Long communityId, Integer rank) {

    // fetch the vote if it exists
    CommunityRank cr = CommunityRank.findFirst("user_id = ? and community_id = ?", userId, communityId);

    if (rank != null) {
      if (cr == null) {
        CommunityRank.createIt("community_id", communityId, "user_id", userId, "rank", rank);
      } else {
        cr.set("rank", rank).saveIt();
      }
    }
    // If the rank is null, then delete the ballot
    else {
      cr.delete();
    }

  }

  public static Community createCommunity(Long userId) {

    log.debug("Creating community");
    String name = "new_community_" + UUID.randomUUID().toString().substring(0, 8);

    Tables.Community c = Tables.Community.createIt("name", name, "modified_by_user_id", userId);

    CommunityUser.createIt("user_id", userId, "community_id", c.getLong("id"), "community_role_id",
        CommunityRole.CREATOR.getVal());

    CommunityView dfv = CommunityView.findFirst("id = ?", c.getLongId());
    List<CommunityUserView> udv = CommunityUserView.where("community_id = ?", c.getLongId());

    Community community = Community.create(dfv, null, udv, null);

    // Create the community stickied chat
    createCommunityChat(community);

    return community;
  }

  public static Community saveCommunity(Long userId, Community co_) {

    Timestamp cTime = new Timestamp(new Date().getTime());

    Tables.Community c = Tables.Community.findFirst("id = ?", co_.getId());
    LazyList<CommunityUserView> cuv = CommunityUserView.where("community_id = ?", co_.getId());

    log.debug(cuv.toJson(true));
    log.debug(co_.json());

    if (co_.getName() != null)
      c.set("name", co_.getName());
    if (co_.getText() != null)
      c.set("text_", co_.getText());
    if (co_.getPrivate_() != null)
      c.set("private", co_.getPrivate_());
    if (co_.getNsfw() != null)
      c.set("nsfw", co_.getNsfw());
    if (co_.getDeleted() != null)
      c.set("deleted", co_.getDeleted());

    c.set("modified_by_user_id", userId);
    c.set("modified", cTime);

    try {
      c.saveIt();
    } catch (Exception e) {
      e.printStackTrace();
      if (e.getLocalizedMessage().contains("already exists")) {
        throw new NoSuchElementException("Community already exists");
      }
    }

    // Update the community chat name
    Tables.Discussion d = Tables.Discussion.findFirst(
        "title like '%chat%' and community_id = ? and modified_by_user_id = ? and stickied = ?", co_.getId(),
        co_.getCreator().getId(), true);

    if (d != null) {
      d.set("title", co_.getName() + " chat").saveIt();
    }

    // Add the community tags
    if (co_.getTags() != null) {
      diffCreateOrDeleteCommunityTags(co_);
    }

    if (co_.getPrivateUsers() != null) {
      diffCreateOrDeleteCommunityUsers(co_.getId(), co_.getPrivateUsers(), CommunityRole.USER);
    }

    if (co_.getBlockedUsers() != null) {
      diffCreateOrDeleteCommunityUsers(co_.getId(), co_.getBlockedUsers(), CommunityRole.BLOCKED);
    }

    if (co_.getModerators() != null) {
      diffCreateOrDeleteCommunityUsers(co_.getId(), co_.getModerators(), CommunityRole.MODERATOR);
    }

    // Fetch the full view
    CommunityView cv = CommunityView.findFirst("id = ?", co_.getId());
    List<CommunityTagView> ctv = CommunityTagView.where("community_id = ?", co_.getId());
    List<CommunityUserView> cuvO = CommunityUserView.where("community_id = ?", co_.getId());

    Community coOut = Community.create(cv, ctv, cuvO, null);

    return coOut;
  }

  private static void diffCreateOrDeleteCommunityUsers(Long communityId, List<User> users, CommunityRole role) {
    Set<Long> postUserIds = users.stream().map(user -> user.getId()).collect(Collectors.toSet());

    Set<Long> dbUserIds = CommunityUser.where("community_id = ? and community_role_id = ?", communityId, role.getVal())
        .collectDistinct("user_id");

    Set<Long> diffPostUserIds = new LinkedHashSet<>(postUserIds);
    Set<Long> diffDbTagIds = new LinkedHashSet<>(dbUserIds);

    diffPostUserIds.removeAll(dbUserIds);
    diffDbTagIds.removeAll(postUserIds);

    // Delete everything in the DB, that's not posted.
    if (!diffDbTagIds.isEmpty()) {
      CommunityUser.delete(
          "community_id = ? and community_role_id = ? and user_id in " + Tools.convertListToInQuery(diffDbTagIds),
          communityId, role.getVal());
    }

    for (Long uId : diffPostUserIds) {
      CommunityUser.createIt("community_id", communityId, "user_id", uId, "community_role_id", role.getVal());
    }
  }

  private static void diffCreateOrDeleteCommunityTags(Community co_) {
    Set<Long> postTagIds = co_.getTags().stream().map(tag -> tag.getId()).collect(Collectors.toSet());

    // Fetch the existing community tags from the DB
    Set<Long> dbTagIds = CommunityTag.where("community_id = ?", co_.getId()).collectDistinct("tag_id");

    Set<Long> diffPostTagIds = new LinkedHashSet<>(postTagIds);
    Set<Long> diffDbTagIds = new LinkedHashSet<>(dbTagIds);

    diffPostTagIds.removeAll(dbTagIds);
    diffDbTagIds.removeAll(postTagIds);

    // Delete everything in the DB, that's not posted.
    if (!diffDbTagIds.isEmpty()) {
      CommunityTag.delete("community_id = ? and tag_id in " + Tools.convertListToInQuery(diffDbTagIds), co_.getId());
    }

    // Add everything posted, thats not in the db
    for (Long tagId : diffPostTagIds) {
      CommunityTag.createIt("community_id", co_.getId(), "tag_id", tagId);
    }
  }

  public static Community saveFavoriteCommunity(Long userId, Long communityId) {

    CommunityUser cu = CommunityUser.findFirst("user_id = ? and community_id = ?", userId, communityId);

    if (cu == null) {
      CommunityUser.createIt("user_id", userId, "community_id", communityId, "community_role_id",
          CommunityRole.USER.getVal());

      CommunityNoTextView cntv = CommunityNoTextView.findFirst("id = ?", communityId);

      return Community.create(cntv, null, null, null);
    } else {
      return null;
    }
  }

  public static void deleteFavoriteCommunity(Long userId, Long communityId) {

    CommunityUser cu = CommunityUser.findFirst("user_id = ? and community_id = ?", userId, communityId);

    cu.delete();

  }

  public static Tables.Tag getOrCreateTagFromSubreddit(String subredditName) {

    Long userId = 4L; // cardinal

    // Get the tag / or create it if it doesn't exist
    Tables.Tag t = Tables.Tag.findFirst("name = ?", subredditName);

    if (t == null) {
      t = Tables.Tag.createIt("name", subredditName);

    }

    return t;
  }

  public static Tables.Discussion getOrCreateDiscussionFromRedditPost(Long tagId, String title, String link,
      String selfText, Date created) {

    Long userId = 4L; // cardinal
    Long communityId = 1L; // Vanilla

    Tables.Discussion d = Tables.Discussion.findFirst("title = ?", title);

    if (d == null) {
      d = Tables.Discussion.createIt("title", title, "community_id", communityId, "modified_by_user_id", userId,
          "created", new Timestamp(created.getTime()));

      if (!link.isEmpty())
        d.set("link", link);
      if (!selfText.isEmpty())
        d.set("text_", selfText);

      d.saveIt();

      DiscussionUser.createIt("user_id", userId, "discussion_id", d.getLong("id"), "discussion_role_id",
          DiscussionRole.CREATOR.getVal());

      DiscussionTag.createIt("discussion_id", d.getLong("id"), "tag_id", tagId);
    }

    return d;

  }

  private static User createUserObj(Tables.User user, Boolean fullUser) {
    User userObj = User.create(user);

    String jwt = JWT.create().withIssuer("flowchat").withClaim("user_name", userObj.getName())
        .withClaim("user_id", userObj.getId().toString()).withClaim("full_user", fullUser)
        .sign(Tools.getJWTAlgorithm());

    userObj.setJwt(jwt);

    Tables.Login login = Tables.Login.createIt("user_id", user.getLongId(), "jwt", jwt);

    return userObj;
  }

}


import ch.qos.logback.classic.Logger;
import com.chat.tools.Tools;
import com.chat.types.comment.Comment;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.javalite.activejdbc.Model;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.chat.db.Tables.*;

/**
 * Created by tyler on 5/27/16.
 */
public class Transformations {

    public static Logger log = (Logger) LoggerFactory.getLogger(Transformations.class);

    public static Map<Long, Comment> convertCommentThreadedViewToMap(List<? extends Model> cvs,
                                                                     Map<Long, Integer> votes) {

        // Create a top level map of ids to comments
        Map<Long, Comment> commentObjMap = new LinkedHashMap<>();

        for (Model cv : cvs) {

            Long id = cv.getLong("id");

            // Check to make sure it has a vote
            Integer vote = (votes != null && votes.containsKey(id)) ? votes.get(id) : null;

            // Create the comment object
            Comment co = Comment.create(cv, vote);

            commentObjMap.put(id, co);
        }

        return commentObjMap;
    }

    public static List<Comment> convertCommentsMapToEmbeddedObjects(
            Map<Long, Comment> commentObjMap,
            Long topLimit, Long maxDepth, Comparator<Comment> comparator) {

        List<Comment> cos = new ArrayList<>();

        for (Map.Entry<Long, Comment> e : commentObjMap.entrySet()) {

            Long id = e.getKey();
            Comment co = e.getValue();

//            log.info(co.json());

            Long parentId = commentObjMap.get(id).getParentId();

            // If its top level, add it
            if (parentId == null || id.equals(co.getTopParentId())) {
                cos.add(co);
            }
            else {
                // Get the immediate parent
                Comment parent = commentObjMap.get(parentId);

                // Add it to the embedded object, if the path length/maxDepth is below a certain limit
                if (co.getPathLength() < maxDepth) {
                    parent.getEmbedded().add(co);
                    Collections.sort(parent.getEmbedded(), comparator);
                }

            }

        }

        Collections.sort(cos, comparator);

        Integer limit = (topLimit < cos.size()) ? topLimit.intValue() : cos.size();

        return cos.subList(0, limit);
    }

    public static List<Comment> convertCommentsToEmbeddedObjects(
            List<? extends Model> cvs,
            Map<Long, Integer> votes,
            Long topLimit, Long maxDepth, Comparator<Comment> comparator) {

        Map<Long, Comment> commentObjMap = convertCommentThreadedViewToMap(cvs, votes);

        List<Comment> cos = convertCommentsMapToEmbeddedObjects(commentObjMap, topLimit, maxDepth, comparator);

        return cos;
    }

    public static List<Comment> convertCommentsToEmbeddedObjects(
            List<? extends Model> cvs,
            Map<Long, Integer> votes,
            Comparator<Comment> comparator) {
        return convertCommentsToEmbeddedObjects(cvs, votes, Long.MAX_VALUE, Long.MAX_VALUE, comparator);
    }



    public static <T extends Model> Map<Long, Integer> convertRankToMap(List<T> drs, String idColumnName) {

        // Convert those votes to a map from id to rank
        Map<Long, Integer> rankMap = new HashMap<>();
        for (T dr : drs) {
            rankMap.put(dr.getLong(idColumnName), dr.getInteger("rank"));
        }

        try {
            log.debug(Tools.JACKSON.writeValueAsString(rankMap));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return rankMap;
    }


    public static <T extends Model> Map<Long,List<T>> convertRowsToMap(List<T> tags, String idColumnName) {

        Map<Long,List<T>> map = new HashMap<>();

        for (T dtv : tags) {
            Long id = dtv.getLong(idColumnName);
            List<T> arr = map.get(id);

            if (arr == null) {
                arr = new ArrayList<>();
                map.put(id, arr);
            }

            arr.add(dtv);
        }

        return map;
    }
}


import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Created by tyler on 5/24/16.
 */
public class Tables {

    @Table("user_")
    public static class User extends Model {}

    @Table("user_setting")
    public static class UserSetting extends Model {}

    @Table("user_audit_view")
    public static class UserAuditView extends Model {}

    @Table("login")
    public static class Login extends Model {}

    @Table("comment")
    public static class Comment extends Model {}

    @Table("comment_tree")
    public static class CommentTree extends Model {}

    @Table("comment_breadcrumbs_view")
    public static class CommentBreadcrumbsView extends Model {}

    @Table("comment_threaded_view")
    public static class CommentThreadedView extends Model {}

    @Table("comment_rank")
    public static class CommentRank extends Model {}

    @Table("discussion")
    public static class Discussion extends Model {}

    @Table("discussion_role")
    public static class DiscussionRole extends Model {}

    @Table("discussion_user")
    public static class DiscussionUser extends Model {}

    @Table("discussion_user_view")
    public static class DiscussionUserView extends Model {}

    @Table("discussion_full_view")
    public static class DiscussionFullView extends Model {}

    @Table("discussion_notext_view")
    public static class DiscussionNoTextView extends Model {}

    @Table("discussion_tag_view")
    public static class DiscussionTagView extends Model {}

    @Table("discussion_rank")
    public static class DiscussionRank extends Model {}

    @Table("favorite_discussion_user")
    public static class FavoriteDiscussionUser extends Model {}

    @Table("favorite_discussion_user_view")
    public static class FavoriteDiscussionUserView extends Model {}

    @Table("discussion_tag")
    public static class DiscussionTag extends Model {}

    @Table("tag")
    public static class Tag extends Model {}

    @Table("ranking_constants")
    public static class RankingConstants extends Model {}

    @Table("censored_word")
    public static class CensoredWord extends Model {}

    @Table("tags_view")
    public static class TagsView extends Model {}

    @Table("community")
    public static class Community extends Model {}

    @Table("community_view")
    public static class CommunityView extends Model {}

    @Table("community_notext_view")
    public static class CommunityNoTextView extends Model {}

    @Table("community_role")
    public static class CommunityRole extends Model {}

    @Table("community_tag")
    public static class CommunityTag extends Model {}

    @Table("community_tag_view")
    public static class CommunityTagView extends Model {}

    @Table("community_rank")
    public static class CommunityRank extends Model {}

    @Table("community_user")
    public static class CommunityUser extends Model {}

    @Table("community_user_view")
    public static class CommunityUserView extends Model {}

    @Table("community_audit_view")
    public static class CommunityAuditView extends Model {}

}

