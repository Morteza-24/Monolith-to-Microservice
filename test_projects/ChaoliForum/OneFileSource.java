
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.NightModeHelper;
import com.facebook.stetho.Stetho;

import io.github.kbiakov.codeview.classifier.CodeProcessor;

/**
 * Created by jianhao on 16-8-25.
 */
public class ChaoliApplication extends Application {
    private static Context appContext;
    @Override
    public void onCreate() {
        super.onCreate();
        // train classifier on app start
        CodeProcessor.init(this);
        ChaoliApplication.appContext = getApplicationContext();
        if (NightModeHelper.isDay()){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        Stetho.initializeWithDefaults(this);
    }

    public static Context getAppContext() {
        return appContext;
    }

    /**
     * get the app-wide shared preference.
     * @return app-wide shared preference
     */
    public static SharedPreferences getSp() {
        return appContext.getSharedPreferences(Constants.APP_NAME, MODE_PRIVATE);
    }
}


import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by jianhao on 16-9-3.
 * A OkHttp Wrapper
 */
public class MyOkHttp {
    private final static String TAG = "MyOkHttp";
    private static OkHttpClient okHttpClient;
    private static CookiesManager mCookiesManager;
    private static Context mContext = ChaoliApplication.getAppContext();

    public static void cancel(Object tag) {
        for (Call call : okHttpClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) call.cancel();
        }
        for (Call call : okHttpClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) call.cancel();
        }
    }

    private static class MyInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            Log.w("Retrofit@Response", response.body().string());
            return response;
        }
    }

    public synchronized static OkHttpClient getClient(){
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            /**
             * 解决5.0以下系统默认不支持TLS协议导致网络访问失败的问题。
             */
            try {
                // Create a trust manager that does not validate certificate chains
                final X509TrustManager trustAllCert =
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        };
                // Install the all-trusting trust manager
                final SSLSocketFactory sslSocketFactory = new SSLSocketFactoryCompat(trustAllCert);
                builder.sslSocketFactory(sslSocketFactory, trustAllCert);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            mCookiesManager = new CookiesManager();
            okHttpClient = builder
                    .cookieJar(mCookiesManager)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addNetworkInterceptor(new StethoInterceptor())
                    //.addInterceptor(new MyInterceptor())
                    //.connectTimeout(5, TimeUnit.SECONDS)
                    //.readTimeout(5, TimeUnit.SECONDS)
                    .build();
        }
        return okHttpClient;
    }

    public synchronized static void clearCookie(){
        if (mCookiesManager != null) {
            mCookiesManager.clear();
        }
    }

    public static class MyOkHttpClient {
        private MultipartBody.Builder builder;
        //private FormBody.Builder formBuilder;
        private RequestBody requestBody;
        private Request.Builder requestBuilder;
        private Request request;

        public MyOkHttpClient get(String url){
            requestBuilder = new Request.Builder().get();
            requestBuilder = requestBuilder.url(url);
            return this;
        }
        public MyOkHttpClient post(String url){
            requestBody = builder.build();
            requestBuilder = new Request.Builder().post(requestBody);
            requestBuilder = requestBuilder.url(url);
            return this;
        }
        public MyOkHttpClient add(String key, String value) {
            if (builder == null) builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart(key, value);
            //if (formBuilder == null) formBuilder = new FormBody.Builder();
            //formBuilder.add(key, value);
            return this;
        }

        public MyOkHttpClient add(String key, String type, File file) {
            if (builder == null) builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if (file != null) builder.addFormDataPart(key, file.getName(), RequestBody.create(MediaType.parse(type), file));
            return this;
        }
        /*public MyOkHttpClient url(String url) {
            requestBuilder = requestBuilder.url(url);
            return this;
        }*/

        public void enqueue(final Callback callback) {
            request = requestBuilder.build();
            Call call = getClient().newCall(request);
            call.enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(final Call call, final IOException e) {
                    new Handler(ChaoliApplication.getAppContext().getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailure(call, e);
                        }
                    });
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                    final String responseStr = response.body().string();
                    new Handler(ChaoliApplication.getAppContext().getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.onResponse(call, response, responseStr);
                            } catch (IOException e) {
                                onFailure(call, e);
                            }
                        }
                    });
                    response.body().close();
                }
            });
        }
        @Deprecated
        public void enqueue(final Context context, final Callback callback) {
            enqueue(callback);
        }

        public void enqueue(final Callback1 callback) {
            request = requestBuilder.build();
            Call call = getClient().newCall(request);
            call.enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(final Call call, final IOException e) {
                            callback.onFailure(call, e);
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                                callback.onResponse(call, response);
                }
            });
        }

        @Deprecated
        public void enqueue(final Context context, final Callback1 callback) {
            enqueue(callback);
        }
    }
    public static abstract class Callback {
        public abstract void onFailure(Call call, IOException e);
        public abstract void onResponse(Call call, Response response, String responseStr) throws IOException;
    }

    public static abstract class Callback1 {
        public abstract void onFailure(Call call, IOException e);
        public abstract void onResponse(Call call, Response response) throws IOException;
    }

    /**
     * https://segmentfault.com/a/1190000004345545
     */
    private static class CookiesManager implements CookieJar {
        private final PersistentCookieStore cookieStore = new PersistentCookieStore(mContext);

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            if (cookies != null && cookies.size() > 0) {
                for (Cookie item : cookies) {
                    cookieStore.add(url, item);
                }
            }
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            return cookieStore.get(url);
        }

        public void clear(){
            cookieStore.removeAll();
        }
    }
}

/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
public class SSLSocketFactoryCompat extends SSLSocketFactory {
    private SSLSocketFactory defaultFactory;
    // Android 5.0+ (API level21) provides reasonable default settings
    // but it still allows SSLv3
    // https://developer.android.com/about/versions/android-5.0-changes.html#ssl
    static String protocols[] = null, cipherSuites[] = null;
    static {
        try {
            SSLSocket socket = (SSLSocket)SSLSocketFactory.getDefault().createSocket();
            if (socket != null) {
                /* set reasonable protocol versions */
                // - enable all supported protocols (enables TLSv1.1 and TLSv1.2 on Android <5.0)
                // - remove all SSL versions (especially SSLv3) because they're insecure now
                List<String> protocols = new LinkedList<>();
                for (String protocol : socket.getSupportedProtocols())
                    if (!protocol.toUpperCase().contains("SSL"))
                        protocols.add(protocol);
                SSLSocketFactoryCompat.protocols = protocols.toArray(new String[protocols.size()]);
                /* set up reasonable cipher suites */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    // choose known secure cipher suites
                    List<String> allowedCiphers = Arrays.asList(
                            // TLS 1.2
                            "TLS_RSA_WITH_AES_256_GCM_SHA384",
                            "TLS_RSA_WITH_AES_128_GCM_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                            "TLS_ECHDE_RSA_WITH_AES_128_GCM_SHA256",
                            // maximum interoperability
                            "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
                            "TLS_RSA_WITH_AES_128_CBC_SHA",
                            // additionally
                            "TLS_RSA_WITH_AES_256_CBC_SHA",
                            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
                    List<String> availableCiphers = Arrays.asList(socket.getSupportedCipherSuites());
                    // take all allowed ciphers that are available and put them into preferredCiphers
                    HashSet<String> preferredCiphers = new HashSet<>(allowedCiphers);
                    preferredCiphers.retainAll(availableCiphers);
                    /* For maximum security, preferredCiphers should *replace* enabled ciphers (thus disabling
                     * ciphers which are enabled by default, but have become unsecure), but I guess for
                     * the security level of DAVdroid and maximum compatibility, disabling of insecure
                     * ciphers should be a server-side task */
                    // add preferred ciphers to enabled ciphers
                    HashSet<String> enabledCiphers = preferredCiphers;
                    enabledCiphers.addAll(new HashSet<>(Arrays.asList(socket.getEnabledCipherSuites())));
                    SSLSocketFactoryCompat.cipherSuites = enabledCiphers.toArray(new String[enabledCiphers.size()]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public SSLSocketFactoryCompat(X509TrustManager tm) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, (tm != null) ? new X509TrustManager[] { tm } : null, null);
            defaultFactory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new AssertionError(); // The system has no TLS. Just give up.
        }
    }
    private void upgradeTLS(SSLSocket ssl) {
        // Android 5.0+ (API level21) provides reasonable default settings
        // but it still allows SSLv3
        // https://developer.android.com/about/versions/android-5.0-changes.html#ssl
        if (protocols != null) {
            ssl.setEnabledProtocols(protocols);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && cipherSuites != null) {
            ssl.setEnabledCipherSuites(cipherSuites);
        }
    }
    @Override
    public String[] getDefaultCipherSuites() {
        return cipherSuites;
    }
    @Override
    public String[] getSupportedCipherSuites() {
        return cipherSuites;
    }
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        Socket ssl = defaultFactory.createSocket(s, host, port, autoClose);
        if (ssl instanceof SSLSocket)
            upgradeTLS((SSLSocket)ssl);
        return ssl;
    }
    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        Socket ssl = defaultFactory.createSocket(host, port);
        if (ssl instanceof SSLSocket)
            upgradeTLS((SSLSocket)ssl);
        return ssl;
    }
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        Socket ssl = defaultFactory.createSocket(host, port, localHost, localPort);
        if (ssl instanceof SSLSocket)
            upgradeTLS((SSLSocket)ssl);
        return ssl;
    }
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket ssl = defaultFactory.createSocket(host, port);
        if (ssl instanceof SSLSocket)
            upgradeTLS((SSLSocket)ssl);
        return ssl;
    }
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket ssl = defaultFactory.createSocket(address, port, localAddress, localPort);
        if (ssl instanceof SSLSocket)
            upgradeTLS((SSLSocket)ssl);
        return ssl;
    }
}


import com.daquexian.chaoli.forum.model.ConversationListResult;
import com.daquexian.chaoli.forum.model.HistoryResult;
import com.daquexian.chaoli.forum.model.NotificationList;
import com.daquexian.chaoli.forum.model.PostListResult;
import com.daquexian.chaoli.forum.model.Question;
import com.daquexian.chaoli.forum.model.User;
import com.daquexian.chaoli.forum.model.UserIdAndTokenResult;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by jianhao on 16-8-25.
 */
public interface ChaoliService {
    @GET("index.php/conversation/index.json/{conversationId}/p{page}")
    Call<PostListResult> listPosts(@Path("conversationId") int conversationId, @Path("page") int page);
    @GET("index.php/conversations/index.json/{channel}")
    Observable<ConversationListResult> listConversations(@Path("channel") String channel, @Query("search") String search);
    @GET("index.php/user/login.json")
    Call<UserIdAndTokenResult> getUserIdAndToken();
    @POST("index.php/user/login")
    @FormUrlEncoded
    Call<UserIdAndTokenResult> login(@Field("username") String username, @Field("password") String password,
                             @Field("token") String token, @Field("return") String returnLocation, @Field("login") String login);
    @GET("index.php/settings/general.json")
    Call<User> getProfile();
    @POST("index.php/?p=settings/notificationCheck.ajax")
    Observable<NotificationList> checkNotification();
    @GET("index.php/member/activity.json/{userId}/{page}")
    Call<HistoryResult> getHistory(@Path("userId") int userId, @Path("page")int page);
    @GET("reg-exam/get-q.php")
    Call<ArrayList<Question>> getQuestion(@Query("tags") String tag);
}


import com.daquexian.chaoli.forum.meta.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by jianhao on 16-9-2.
 */
public class MyRetrofit {
    private final static String TAG = "MyRetrofit";

    private static Retrofit retrofit;
    private static ChaoliService service;

    public synchronized static ChaoliService getService(){
        if (service == null) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Integer.class, new IntegerTypeAdapter()).create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(MyOkHttp.getClient())
                    .build();
            service = retrofit.create(ChaoliService.class);
        }
        return service;
    }

    private static class IntegerTypeAdapter extends TypeAdapter<Integer> {
        @Override
        public void write(JsonWriter writer, Integer value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(value);
        }

        @Override
        public Integer read(JsonReader reader) throws IOException {
            if(reader.peek() == JsonToken.NULL){
                reader.nextNull();
                return null;
            }
            String stringValue = reader.nextString();
            try{
                int value = Integer.valueOf(stringValue);
                return value;
            }catch(NumberFormatException e){
                return null;
            }
        }
    }
}


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import okhttp3.Cookie;

/**
 * self-explanatory
 * https://segmentfault.com/a/1190000004345545
 * Created by jianhao on 16-9-6.
 */
public class SerializableOkHttpCookies implements Serializable {

    private transient final Cookie cookies;
    private transient Cookie clientCookies;

    public SerializableOkHttpCookies(Cookie cookies) {
        this.cookies = cookies;
    }

    public Cookie getCookies() {
        Cookie bestCookies = cookies;
        if (clientCookies != null) {
            bestCookies = clientCookies;
        }
        return bestCookies;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(cookies.name());
        out.writeObject(cookies.value());
        out.writeLong(cookies.expiresAt());
        out.writeObject(cookies.domain());
        out.writeObject(cookies.path());
        out.writeBoolean(cookies.secure());
        out.writeBoolean(cookies.httpOnly());
        out.writeBoolean(cookies.hostOnly());
        out.writeBoolean(cookies.persistent());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        String name = (String) in.readObject();
        String value = (String) in.readObject();
        long expiresAt = in.readLong();
        String domain = (String) in.readObject();
        String path = (String) in.readObject();
        boolean secure = in.readBoolean();
        boolean httpOnly = in.readBoolean();
        boolean hostOnly = in.readBoolean();
        boolean persistent = in.readBoolean();
        Cookie.Builder builder = new Cookie.Builder();
        builder = builder.name(name);
        builder = builder.value(value);
        builder = builder.expiresAt(expiresAt);
        builder = hostOnly ? builder.hostOnlyDomain(domain) : builder.domain(domain);
        builder = builder.path(path);
        builder = secure ? builder.secure() : builder;
        builder = httpOnly ? builder.httpOnly() : builder;
        clientCookies =builder.build();
    }
}


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * self-explanatory
 * https://segmentfault.com/a/1190000004345545
 * Created by jianhao on 16-9-6.
 */
public class PersistentCookieStore {
    private static final String LOG_TAG = "PersistentCookieStore";
    private static final String COOKIE_PREFS = "Cookies_Prefs";

    private final Map<String, ConcurrentHashMap<String, Cookie>> cookies;
    private final SharedPreferences cookiePrefs;


    public PersistentCookieStore(Context context) {
        cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
        cookies = new HashMap<>();

        //将持久化的cookies缓存到内存中 即map cookies
        Map<String, ?> prefsMap = cookiePrefs.getAll();
        for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
            String[] cookieNames = TextUtils.split((String) entry.getValue(), ",");
            for (String name : cookieNames) {
                String encodedCookie = cookiePrefs.getString(name, null);
                if (encodedCookie != null) {
                    Cookie decodedCookie = decodeCookie(encodedCookie);
                    if (decodedCookie != null) {
                        if (!cookies.containsKey(entry.getKey())) {
                            cookies.put(entry.getKey(), new ConcurrentHashMap<String, Cookie>());
                        }
                        cookies.get(entry.getKey()).put(name, decodedCookie);
                    }
                }
            }
        }
    }

    protected String getCookieToken(Cookie cookie) {
        return cookie.name() + "@" + cookie.domain();
    }

    public void add(HttpUrl url, Cookie cookie) {
        String name = getCookieToken(cookie);

        //将cookies缓存到内存中 如果缓存过期 就重置此cookie
        if (!cookie.persistent()) {
            if (!cookies.containsKey(url.host())) {
                cookies.put(url.host(), new ConcurrentHashMap<String, Cookie>());
            }
            cookies.get(url.host()).put(name, cookie);
        } else {
            if (cookies.containsKey(url.host())) {
                cookies.get(url.host()).remove(name);
            }
        }

        //讲cookies持久化到本地
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        prefsWriter.putString(url.host(), TextUtils.join(",", cookies.get(url.host()).keySet()));
        prefsWriter.putString(name, encodeCookie(new SerializableOkHttpCookies(cookie)));
        prefsWriter.apply();
    }

    public List<Cookie> get(HttpUrl url) {
        ArrayList<Cookie> ret = new ArrayList<>();
        if (cookies.containsKey(url.host()))
            ret.addAll(cookies.get(url.host()).values());
        return ret;
    }

    public boolean removeAll() {
        SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
        prefsWriter.clear();
        prefsWriter.apply();
        cookies.clear();
        return true;
    }

    public boolean remove(HttpUrl url, Cookie cookie) {
        String name = getCookieToken(cookie);

        if (cookies.containsKey(url.host()) && cookies.get(url.host()).containsKey(name)) {
            cookies.get(url.host()).remove(name);

            SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
            if (cookiePrefs.contains(name)) {
                prefsWriter.remove(name);
            }
            prefsWriter.putString(url.host(), TextUtils.join(",", cookies.get(url.host()).keySet()));
            prefsWriter.apply();

            return true;
        } else {
            return false;
        }
    }

    public List<Cookie> getCookies() {
        ArrayList<Cookie> ret = new ArrayList<>();
        for (String key : cookies.keySet())
            ret.addAll(cookies.get(key).values());

        return ret;
    }

    /**
     * cookies 序列化成 string
     *
     * @param cookie 要序列化的cookie
     * @return 序列化之后的string
     */
    protected String encodeCookie(SerializableOkHttpCookies cookie) {
        if (cookie == null)
            return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(os);
            outputStream.writeObject(cookie);
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in encodeCookie", e);
            return null;
        }

        return byteArrayToHexString(os.toByteArray());
    }

    /**
     * 将字符串反序列化成cookies
     *
     * @param cookieString cookies string
     * @return cookie object
     */
    protected Cookie decodeCookie(String cookieString) {
        byte[] bytes = hexStringToByteArray(cookieString);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Cookie cookie = null;
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            cookie = ((SerializableOkHttpCookies) objectInputStream.readObject()).getCookies();
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException in decodeCookie", e);
        } catch (ClassNotFoundException e) {
            Log.d(LOG_TAG, "ClassNotFoundException in decodeCookie", e);
        }

        return cookie;
    }

    /**
     * 二进制数组转十六进制字符串
     *
     * @param bytes byte array to be converted
     * @return string containing hex values
     */
    protected String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte element : bytes) {
            int v = element & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    /**
     * 十六进制字符串转二进制数组
     *
     * @param hexString string of hex-encoded values
     * @return decoded byte array
     */
    protected byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}


import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.widget.Toast;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Post;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * various strange things
 * Created by jianhao on 16-10-2.
 */

public class MyUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "MyUtils";

    private static final String quoteRegex = "\\[quote((.|\n)*?)\\[/quote]";
    private static final String codeRegex = "\\[code]((.|\n)*?)\\[/code]";
    private static final String imgRegex = "\\[img](.*?)\\[/img]";
    private static final String attRegex = "\\[attachment:(.*?)]";

    /**
     * 针对可能有其他帖子被顶到最上方，导致下一页的主题帖与这一页的主题帖有重合的现象
     * @param A 已有的主题帖列表
     * @param B 下一页主题帖列表
     * @return 合成后的新列表的长度
     */
    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B) {
        return expandUnique(A, B, true);
    }

    /**
     * a > b 表示 a 排在 b 后面
     */
    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B, Boolean addBehind) {
        return expandUnique(A, B, addBehind, false);
    }

    public static <T extends Comparable<T>> int expandUnique(List<T> A, List<T> B, Boolean addBehind, Boolean reversed) {
        int lenA = A.size();
        if (lenA == 0) A.addAll(B);
        else {
            if (addBehind) {
                int i;
                for (i = 0; i < B.size(); i++)
                    if ((!reversed && B.get(i).compareTo(A.get(A.size() - 1)) > 0) || (reversed && B.get(i).compareTo(A.get(A.size() - 1)) < 0))
                        break;
                A.addAll(B.subList(i, B.size()));
            } else {
                int i;
                for (i = 0; i < B.size(); i++)
                    if ((!reversed && B.get(i).compareTo(A.get(0)) >= 0) || (reversed && B.get(i).compareTo(A.get(0)) <= 0))
                        break;
                A.addAll(0, B.subList(0, i));
            }
        }
        return A.size();
    }

    public static <T> List reverse(List<T> list) {
        List<T> reversed = new ArrayList<>();
        for (T item : list) {
            reversed.add(0, item);
        }
        return reversed;
    }

    public static String formatSignature(String str) {
        return str.replace("[code]", "").replace("[/code]", "");
    }

    public static void downloadAttachment(Context context, Post.Attachment attachment) {
        try {
            String attUrl = MyUtils.getAttachmentFileUrl(attachment);
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(attUrl));
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, attachment.getFilename());
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
                request.allowScanningByMediaScanner();// if you want to be available from media players
                DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);
            } catch (SecurityException e) {
                // in the case of user rejects the permission request
                e.printStackTrace();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(attUrl));
                context.startActivity(intent);
            }
        } catch (UnsupportedEncodingException e) {
            // say bye-bye to your poor phone
            Toast.makeText(context, "say bye-bye to your poor phone", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setToolbarOffset(CoordinatorLayout coordinatorLayout, AppBarLayout appBarLayout, int offset) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) layoutParams.getBehavior();
        if (behavior != null) {
            behavior.setTopAndBottomOffset(offset);
            behavior.onNestedPreScroll(coordinatorLayout, appBarLayout, null, 0, 1, new int[2]);
        }
    }

    public static String getAttachmentImageUrl(Post.Attachment attachment) {
        return Constants.ATTACHMENT_IMAGE_URL + attachment.getAttachmentId() + attachment.getSecret();
    }

    @SuppressWarnings("WeakerAccess")
    public static String getAttachmentFileUrl(Post.Attachment attachment) throws UnsupportedEncodingException{
        return "https://chaoli.club/index.php/attachment/" + attachment.getAttachmentId() + "_" + URLEncoder.encode(attachment.getFilename(), "UTF-8");
    }

    /**
     * 去除引用
     * @param content API获得的帖子内容
     * @return 去除引用之后的内容，显示给用户或用于在发帖时避免多重引用
     */
    public static String removeQuote(String content) {
        return content.replaceAll(quoteRegex, "");
    }

    private static String replaceMany(String content) {
        return content.replaceAll(codeRegex, ChaoliApplication.getAppContext().getString(R.string.see_codes_in_original_post))
                .replaceAll(imgRegex, ChaoliApplication.getAppContext().getString(R.string.see_img_in_original_post))
                .replaceAll(attRegex, ChaoliApplication.getAppContext().getString(R.string.see_att_in_original_post));
    }

    public static String formatQuote(String quote) {
        return removeQuote(replaceMany(quote));
    }

}


import android.databinding.Observable;
import android.support.v4.util.ArrayMap;

import com.daquexian.chaoli.forum.view.BaseActivity;

/**
 * Manage callbacks, nothing complex
 * Created by daquexian on 17-1-25.
 */

public class DataBindingUtils {
    private static ArrayMap<BaseActivity, ArrayMap<Observable, Observable.OnPropertyChangedCallback>> mGlobalMap = new ArrayMap<>();

    public static void addCallback(BaseActivity activity, Observable observable, Observable.OnPropertyChangedCallback callback) {
        ArrayMap<Observable, Observable.OnPropertyChangedCallback> localMap = mGlobalMap.get(activity);
        if (localMap == null) {
            localMap = new ArrayMap<>();
            mGlobalMap.put(activity, localMap);
        }
        observable.addOnPropertyChangedCallback(callback);
        localMap.put(observable, callback);
    }

    public static void removeCallbacks(BaseActivity activity) {
        ArrayMap<Observable, Observable.OnPropertyChangedCallback> localMap = mGlobalMap.get(activity);
        if (localMap != null) {
            for (Observable observable : localMap.keySet()) {
                observable.removeOnPropertyChangedCallback(localMap.get(observable));
            }
        }
    }
}


import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.daquexian.chaoli.forum.view.MainActivity;

public class SingleUtils
{
	private int dpi;

	private static SingleUtils ourInstance = new SingleUtils();

	public static SingleUtils getInstance()
	{
		return ourInstance;
	}

	private SingleUtils()
	{
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) new MainActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		dpi = dm.densityDpi;
	}

	public int getDpi()
	{
		return dpi;
	}
}

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.UserIdAndTokenResult;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;
import com.daquexian.chaoli.forum.network.MyRetrofit;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class LoginUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "LoginUtils";
    private static final String LOGIN_SP_NAME = "username_and_password";
    private static final String IS_LOGGED_IN = "is_logged_in";
    private static final String SP_USERNAME_KEY = "sUsername";
    private static final String SP_PASSWORD_KEY = "sPassword";
    public static final int FAILED_AT_OPEN_LOGIN_PAGE = 0;
    public static final int FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE = 1;
    public static final int FAILED_AT_LOGIN = 2;
    public static final int WRONG_USERNAME_OR_PASSWORD = 3;
    public static final int FAILED_AT_OPEN_HOMEPAGE = 4;
    public static final int COOKIE_EXPIRED = 5;
    public static final int EMPTY_UN_OR_PW = 6;
    public static final int ERROR_LOGIN_STATUS = 7; // TODO: 17-1-2 handle this error

    private static void setToken(String token) {
        LoginUtils.sToken = token;
    }

    public static String getToken() {
        return sToken;
    }

    private static String sUsername;
    private static String sPassword;
    private static String sToken;
    private static boolean sIsLoggedIn;

    private static SharedPreferences sSharedPreferences;

    public static void begin_login(final String username, final String password, final LoginObserver loginObserver){
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);

        Log.d("login", username + ", " + password);

        sUsername = username;
        sPassword = password;
        pre_login(loginObserver);
    }

    public static void begin_login(LoginObserver loginObserver){

        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);

        sUsername = sSharedPreferences.getString(SP_USERNAME_KEY, "");
        sPassword = sSharedPreferences.getString(SP_PASSWORD_KEY, "");

        if("".equals(sUsername) || "".equals(sPassword)){
            loginObserver.onLoginFailure(EMPTY_UN_OR_PW);
            return;
        }

        Log.d("login", sUsername + ", " + sPassword);

        begin_login(sUsername, sPassword, loginObserver);
    }

    private static void pre_login(final LoginObserver loginObserver){//获取登录页面的token
        MyRetrofit.getService().getUserIdAndToken()
                .enqueue(new retrofit2.Callback<UserIdAndTokenResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<UserIdAndTokenResult> call, retrofit2.Response<UserIdAndTokenResult> response) {
                        if (response.body() == null) {
                            onFailure(call, new RuntimeException("poor network"));
                            return;
                        }

                        if (response.body().getUserId() == 0) {
                            setToken(response.body().getToken());
                            login(loginObserver);
                        } else {
                            saveUserInfo(response.body().getUserId(), sUsername, response.body().getToken());
                            loginObserver.onLoginSuccess(Me.getMyUserId(), getToken());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<UserIdAndTokenResult> call, Throwable t) {
                        loginObserver.onLoginFailure(FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE);
                    }
                });
    }

    private static void login(final LoginObserver loginObserver){ //发送请求登录
        MyRetrofit.getService()
                .login(sUsername, sPassword, getToken(), "user/login.json", "登录")
                .enqueue(new retrofit2.Callback<UserIdAndTokenResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<UserIdAndTokenResult> call, retrofit2.Response<UserIdAndTokenResult> response) {
                        saveUserInfo(response.body().getUserId(), sUsername, response.body().getToken());

                        saveUsernameAndPasswordToSp(sUsername, sPassword);

                        loginObserver.onLoginSuccess(Me.getMyUserId(), getToken());
                    }

                    @Override
                    public void onFailure(retrofit2.Call<UserIdAndTokenResult> call, Throwable t) {
                        loginObserver.onLoginFailure(WRONG_USERNAME_OR_PASSWORD);
                        t.printStackTrace();
                    }
                });
    }

    public static void logout(final LogoutObserver logoutObserver){
        String logoutURL = Constants.LOGOUT_PRE_URL + getToken();
        clear(ChaoliApplication.getAppContext());
        Me.clear();
        new MyOkHttp.MyOkHttpClient()
                .get(logoutURL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logoutObserver.onLogoutFailure(0);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        logoutObserver.onLogoutSuccess();
                    }
                });
    }

    public static void clear(Context context){
        MyOkHttp.clearCookie();
        sIsLoggedIn = false;
        sSharedPreferences = context.getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        if(sSharedPreferences != null) {
            SharedPreferences.Editor editor = sSharedPreferences.edit();
            editor.remove(IS_LOGGED_IN);
            editor.remove(SP_USERNAME_KEY);
            editor.remove(SP_PASSWORD_KEY);
            editor.apply();
        }
    }

    public static boolean isLoggedIn() {
        return sIsLoggedIn;
    }

    public static boolean hasSavedData() {
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        return !sSharedPreferences.getString(SP_USERNAME_KEY, "").equals("") && !sSharedPreferences.getString(SP_PASSWORD_KEY, "").equals("");
    }

    public static String getSavedUsername() {
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        return sSharedPreferences.getString(SP_USERNAME_KEY, "");
    }

    public static void saveUsernameAndPasswordToSp(String username, String password) {
        sSharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(LOGIN_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sSharedPreferences.edit();
        editor.putString(SP_USERNAME_KEY, username);
        // TODO: 16-3-11 1915 Encrypt saved sPassword
        editor.putString(SP_PASSWORD_KEY, password);
        editor.apply();
    }

    private static void saveUserInfo(int userId, String username, String token) {
        Me.setUserId(userId);
        Me.setUsername(username);
        setToken(token);
        sIsLoggedIn = true;
    }
    public interface LoginObserver
    {
        void onLoginSuccess(int userId, String token);
        void onLoginFailure(int statusCode);
    }

    public interface LogoutObserver
    {
        void onLogoutSuccess();
        void onLogoutFailure(int statusCode);
    }
}


import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class PostUtils
{
	public static final String TAG = "PostUtils";

	public static void reply(int conversationId, String content, final ReplyObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("conversationId", String.valueOf(conversationId))
				.add("content", content)
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.replyURL + conversationId)
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onReplyFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onReplyFailure(response.code());
						else observer.onReplySuccess();
					}
				});
	}

	public static void edit(int postId, String content, final EditObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("content", content)
				.add("save", "true")
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.editURL + postId)
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onEditFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onEditFailure(response.code());
						else observer.onEditSuccess();
					}
				});
	}

	public static void delete(int postId, final DeleteObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.deleteURL + postId)		// Or get?
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onDeleteFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onDeleteFailure(response.code());
						else observer.onDeleteSuccess();
					}
				});
	}

	public static void restore(int postId, final RestoreObserver observer)
	{
		new MyOkHttp.MyOkHttpClient()
				.add("userId", String.valueOf(Me.getUserId()))
				.add("token", LoginUtils.getToken())
				.post(Constants.deleteURL + postId)		// Or get?
				.enqueue(new MyOkHttp.Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						observer.onRestoreFailure(-1);
					}

					@Override
					public void onResponse(Call call, Response response, String responseStr) throws IOException {
						if (response.code() != 200) observer.onRestoreFailure(response.code());
						else observer.onRestoreSuccess();
					}
				});
	}

	public static Boolean canEdit(int postId) {
		return false;
	}

	public static Boolean canDelete(int postId) {
		return false;
	}

	public interface ReplyObserver
	{
		void onReplySuccess();
		void onReplyFailure(int statusCode);
	}

	public interface EditObserver
	{
		void onEditSuccess();
		void onEditFailure(int statusCode);
	}

	public interface QuoteObserver
	{
		void onQuoteSuccess();
		void onQuoteFailure(int statusCode);
	}

	public interface DeleteObserver
	{
		void onDeleteSuccess();
		void onDeleteFailure(int statusCode);
	}

	public interface RestoreObserver
	{
		void onRestoreSuccess();
		void onRestoreFailure(int statusCode);
	}
}


import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.BusinessQuestion;
import com.daquexian.chaoli.forum.model.Question;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by jianhao on 16-3-27.
 */
public class SignUpUtils {
    private static final String TAG = "SignUpUtils";

    public static final Map<String, String> subjectTags = new HashMap<String, String>(){{
        put("数学", "math");
        put("生物", "bio");
        put("化学", "chem");
        put("物理", "phys");
        put("综合", "");
    }};

    public static int ANSWERS_WRONG = -1;

    public static void getQuestionObjList(final GetQuestionObserver observer, String subject){
        MyRetrofit.getService()
                .getQuestion(subject)
                .enqueue(new Callback<ArrayList<Question>>() {
                    @Override
                    public void onResponse(Call<ArrayList<Question>> call, Response<ArrayList<Question>> response) {
                        observer.onGetQuestionObjList(BusinessQuestion.fromList(response.body()));
                    }

                    @Override
                    public void onFailure(Call<ArrayList<Question>> call, Throwable t) {

                    }
                });
        /*String url = GET_QUESTION_URL + subject;
        client.get(context, url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);
                ArrayList<Question> questionList = "".equals(response) ? null : (ArrayList<Question>) JSON.parseArray(response, Question.class);
                observer.onGetQuestionObjList(questionList);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("error", "link");
                observer.onGetQuestionObjList(null);
            }
        });*/
        //String jsonStr = "{\"_id\":{\"$id\":\"55d7f15669cb38bb2e8b4574\"},\"question\":\"“如无必要，勿增实体”是（）剃刀原理的内容。\",\"choice\":true,\"options\":[\"飞利浦\",\"草薙\",\"无毁之湖光\",\"奥卡姆\"],\"multi_answer\":false}";
        //String jsonStr = "{\"$id\":\"55d7f15669cb38bb2e8b4574\",\"question\":\"“如无必要，勿增实体”是（）剃刀原理的内容。\",\"choice\":true,\"options\":[\"飞利浦\",\"草薙\",\"无毁之湖光\",\"奥卡姆\"],\"multi_answer\":false}";
        //String jsonStr = "{\"id\":\"a\", \"question\":\"b\", \"choice\":\"c\", \"options\":[\"d\"], \"multi_answer\":\"e\" }";
        //String jsonStr = "{\"hi\":\"sdf\"}";
        //ArrayList<Question> questionList = (ArrayList<Question>) JSONArray.parseArray(jsonStr, Question.class);
    }

    public static void submitAnswers(List<BusinessQuestion> questionList, final SubmitObserver observer){
        MyOkHttp.MyOkHttpClient myOkHttpClient = new MyOkHttp.MyOkHttpClient().add("questions", new Gson().toJson(Question.fromList(questionList)));

        //String str = JSON.toJSONString(questionList);
        //RequestParams params = new RequestParams();
        //params.put("questions", str);

        for (BusinessQuestion i: questionList) {
            if (i.choice)
                for (int j = 0; j < i.isChecked.size(); j++) {
                    if (i.isChecked.get(j)) {
                        myOkHttpClient.add(i.id + "_opt[]", String.valueOf(j));
                        Log.d(TAG, "submitAnswers:  id = " + i.id + ", item value = " + j);
                    }
                }
            else
                myOkHttpClient.add(i.id + "_ans", i.answer.get());
        }

        myOkHttpClient.add("simplified", "1");
        myOkHttpClient.post(Constants.CONFIRM_ANSWER_URL)
                .enqueue(ChaoliApplication.getAppContext(), new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        observer.onFailure(-1);
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response, String responseStr) throws IOException {
                        Log.d(TAG, "onResponse: " + responseStr);
                        if ("failed".equals(responseStr)) {
                            observer.onFailure(ANSWERS_WRONG);
                        } else {
                            observer.onAnswersPass(responseStr);
                        }
                    }
                });

        /*client.post(context, CONFIRM_ANSWER_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody).trim();
                if ("failed".equals(response)) {
                    observer.onFailure(ANSWERS_WRONG);
                } else {
                    observer.onAnswersPass(response);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                observer.onFailure(statusCode);
            }
        });*/
    }

    public interface GetQuestionObserver{
        void onGetQuestionObjList(ArrayList<BusinessQuestion> questionList);
    }
    public interface SubmitObserver {
        void onAnswersPass(String code);
        void onFailure(int statusCode);
    }
}


import android.content.Context;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by jianhao on 16-3-11.
 */
public class ConversationUtils {
    private static final String TAG = "ConversationUtils";
    /* 茶馆 */
    public static final int CAFF_ID = 1;
    /* 数学 */
    public static final int MATH_ID = 4;
    /* 物理 */
    public static final int PHYS_ID = 5;
    /* 化学 */
    public static final int CHEM_ID = 6;
    /* 生物 */
    public static final int BIO_ID = 7;
    /* 技术 */
    public static final int TECH_ID = 8;
    /* 公告 */
    public static final int ANNOUN_ID = 28;
    /* 申诉 */
    public static final int COURT_ID = 25;
    /* 回收站 */
    public static final int RECYCLED_ID = 27;
    /* 语言 */
    public static final int LANG_ID = 40;
    /* 社科 */
    public static final int SOCSCI_ID = 34;

    /* 返回的数据错误 */
    public static final int RETURN_ERROR = -1;
    /* 取消可见用户时没有可见用户列表没有这个用户 */
    public static final int NO_THIS_MEMBER = -2;

    private static ArrayList<Integer> memberList = new ArrayList<>();

    public static void setChannel(int channel, SetChannelObserver Observer){
        setChannel(channel, 0, Observer);
    }

    /**
     * 设置主题的板块
     * conversationId为0时，会设置正在编辑、还未发出的conversation的板块
     * addMember, removeMember也是同样
     * @param channel 板块号
     * @param conversationId 主题id
     * @param observer observer
     */
    public static void setChannel(int channel, int conversationId, final SetChannelObserver observer){
        String url = Constants.SET_CHANNEL_URL + String.valueOf(conversationId);
        new MyOkHttp.MyOkHttpClient()
                .add("channel", String.valueOf(channel))
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(url)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onSetChannelFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onSetChannelFailure(response.code());
                        else {
                            if (responseStr.startsWith("{\"allowedSummary\"")) {
                                observer.onSetChannelSuccess();
                            } else {
                                Log.d(TAG, "onResponse: " + responseStr);
                                observer.onSetChannelFailure(RETURN_ERROR);                 //返回数据错误
                            }
                        }
                    }
                });
    }

    /*  添加可见用户（默认任何人均可见）    */
    public static void addMember(String member, AddMemberObserver Observer){
        addMember(member, 0, Observer);
    }

    public static void addMember(String member, int conversationId, final AddMemberObserver observer){
        String url = Constants.ADD_MEMBER_URL + String.valueOf(conversationId);
        new MyOkHttp.MyOkHttpClient()
                .add("member", member)
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(url)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onAddMemberFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onAddMemberFailure(response.code());
                        else {
                            //Log.i("addMember", response);
                            String idFormat = "data-id='(\\d+)'";
                            Pattern pattern = Pattern.compile(idFormat);
                            Matcher matcher = pattern.matcher(responseStr);
                            //Log.i("response", String.valueOf(response.length()));
                            if (matcher.find()) {
                                //Log.i("id", matcher.group(1));
                                int userIdAdded = Integer.parseInt(matcher.group(1));
                                memberList.add(userIdAdded);
                                observer.onAddMemberSuccess();
                            } else {
                                observer.onAddMemberFailure(RETURN_ERROR);              //返回数据错误
                            }
                        }
                    }
                });
    }

    /*  取消可见用户  */
    public static void removeMember(final Context context, int userId, RemoveMemberObserver Observer){
        removeMember(context, userId, 0, Observer);
    }

    public static void removeMember(final Context context, final int userId,
                                    int conversationId, final RemoveMemberObserver observer){
        String url = Constants.REMOVE_MEMBER_URL + String.valueOf(conversationId);
        new MyOkHttp.MyOkHttpClient()
                .add("member", String.valueOf(userId))
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(url)
                .enqueue(context, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onRemoveMemberFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onRemoveMemberFailure(response.code());
                        else {
                            Log.i("removeMember", responseStr);
                            if (responseStr.startsWith("{\"allowedSummary\"")) {
                                if (memberList.contains(Integer.valueOf(userId))) {
                                    memberList.remove(Integer.valueOf(userId));
                                    observer.onRemoveMemberSuccess();
                                } else {
                                    observer.onRemoveMemberFailure(NO_THIS_MEMBER);         //无此会员
                                }
                            } else {
                                observer.onRemoveMemberFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    /*  发表主题    */
    public static void postConversation(String title, String content,
                                        final PostConversationObserver observer){
        new MyOkHttp.MyOkHttpClient()
                .add("title", title)
                .add("content", content)
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .post(Constants.POST_CONVERSATION_URL)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onPostConversationFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onPostConversationFailure(response.code());
                        else {
                            Log.d(TAG, "onResponse: " + responseStr);
                            if(responseStr.startsWith("{\"redirect\"")){
                                String conIdFormat = "/(\\d+)";
                                Pattern pattern = Pattern.compile(conIdFormat);
                                Matcher matcher = pattern.matcher(responseStr);
                                if(matcher.find()){
                                    observer.onPostConversationSuccess(Integer.parseInt(matcher.group(1)));
                                }else{
                                    observer.onPostConversationFailure(RETURN_ERROR);
                                }
                            } else {
                                observer.onPostConversationFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    /*  获取可见用户列表    */
    public static void getMembersAllowed(Context context, int conId, final GetMembersAllowedObserver observer){
        final List<Integer> memberList = new ArrayList<>();
        String url = Constants.GET_MEMBERS_ALLOWED_URL + "?p=conversation/membersAllowed.ajax/" + conId;
        new MyOkHttp.MyOkHttpClient()
                .add("token", LoginUtils.getToken())
                .add("userId", String.valueOf(Me.getUserId()))
                .get(url)
                .enqueue(context, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onGetMembersAllowedFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onGetMembersAllowedFailure(response.code());
                        else {
                            Log.i("get", responseStr);
                            String idFormat = "data-id='(\\d+)'";           //按此格式从返回的数据中获取id
                            Pattern pattern = Pattern.compile(idFormat);
                            Matcher matcher = pattern.matcher(responseStr);
                            while (matcher.find()) {
                                memberList.add(Integer.valueOf(matcher.group(1)));
                            }

                            //返回的数据中，若可见用户只有自己，则返回自己的id，若有其他人，则不包含自己的id，所以要加上自己的id
                            if (memberList.size() > 1 || (memberList.size() == 1 && memberList.get(0) != Me.getUserId())) {
                                memberList.add(Me.getUserId());
                            }
                            observer.onGetMembersAllowedSuccess(memberList);
                        }

                    }
                });
    }

    /*  隐藏/取消隐藏该主题
    *   执行操作后主题的状态为隐藏，则isIgnored为true，否则为false*/
    public static void ignoreConversation(Context context, int conversationId,
                                          final IgnoreAndStarConversationObserver observer){
        String url = Constants.IGNORE_CONVERSATION_URL + conversationId;
        new MyOkHttp.MyOkHttpClient()
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .get(url)
                .enqueue(context, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onIgnoreConversationFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onIgnoreConversationFailure(response.code());
                        else {
                            if(responseStr.contains("\"ignored\":true")) {
                                observer.onIgnoreConversationSuccess(true);
                            }else if(responseStr.contains("\"ignored\":false")){
                                observer.onIgnoreConversationSuccess(false);
                            }else{
                                Log.e("ignore", "response = " + responseStr);
                                observer.onIgnoreConversationFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    /*  关注/取消关注该主题
    *   执行操作后主题的状态为被关注，则isStarred为true，否则为false*/
    public static void starConversation(Context context, int conversationId,
                                        final IgnoreAndStarConversationObserver observer) {
        String url = Constants.STAR_CONVERSATION_URL + conversationId;
        new MyOkHttp.MyOkHttpClient()
                .add("userId", String.valueOf(Me.getUserId()))
                .add("token", LoginUtils.getToken())
                .get(url)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        observer.onStarConversationFailure(-3);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) observer.onStarConversationFailure(response.code());
                        else {
                            if (responseStr.contains("\"starred\":true")) {
                                observer.onStarConversationSuccess(true);
                            } else if (responseStr.contains("\"starred\":false")) {
                                observer.onStarConversationSuccess(false);
                            } else {
                                observer.onStarConversationFailure(RETURN_ERROR);
                            }
                        }
                    }
                });
    }

    public static Boolean canDelete(int conversationId) {
        return false;
    }

    public static Boolean canEdit(int conversationId) {
        return false;
    }

    public interface SetChannelObserver {
        void onSetChannelSuccess();
        void onSetChannelFailure(int statusCode);
    }

    public interface AddMemberObserver {
        void onAddMemberSuccess();
        void onAddMemberFailure(int statusCode);
    }

    public interface RemoveMemberObserver {
        void onRemoveMemberSuccess();
        void onRemoveMemberFailure(int statusCode);
    }

    public interface PostConversationObserver {
        void onPostConversationSuccess(int conversationId);
        void onPostConversationFailure(int statusCode);
    }

    public interface GetMembersAllowedObserver {
        void onGetMembersAllowedSuccess(List<Integer> memberList);
        void onGetMembersAllowedFailure(int statusCode);
    }

    public interface IgnoreAndStarConversationObserver{
        void onIgnoreConversationSuccess(Boolean isIgnored);
        void onIgnoreConversationFailure(int statusCode);
        void onStarConversationSuccess(Boolean isStarred);
        void onStarConversationFailure(int statusCode);
    }
}


import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.NotificationList;
import com.daquexian.chaoli.forum.model.User;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by daquexian on 16-3-17.
 * 和账户相关的类，包括获取自己的用户信息、检查是否帖子更新、是否有新动态及更改账户设置
 */
public class AccountUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "AccountUtils";

    public static void getProfile(final GetProfileObserver observer){
        MyRetrofit.getService().getProfile()
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        Me.setProfile(ChaoliApplication.getAppContext(), response.body());
                        observer.onGetProfileSuccess();
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        observer.onGetProfileFailure();
                    }
                });
    }

    public static void modifySettings(File avatar, String language, Boolean privateAdd, Boolean starOnReply, Boolean starPrivate, Boolean hideOnline,
                                      final String signature, String userStatus, final ModifySettingsObserver observer){
        MyOkHttp.MyOkHttpClient myOkHttpClient = new MyOkHttp.MyOkHttpClient()
                .add("token", LoginUtils.getToken())
                .add("language", language)
                .add("userStatus", userStatus)
                .add("signature", signature);
        myOkHttpClient.add("avatar", "image/*", avatar);
        if(hideOnline) myOkHttpClient.add("hideOnline", hideOnline.toString());
        if(starPrivate) myOkHttpClient.add("starPrivate", starPrivate.toString());
        if(starOnReply) myOkHttpClient.add("starOnReply", starOnReply.toString());
        if(privateAdd) myOkHttpClient.add("privateAdd", privateAdd.toString());
        myOkHttpClient.add("save", "保存更改");
        myOkHttpClient
                .post(Constants.MODIFY_SETTINGS_URL)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        observer.onModifySettingsFailure(-3);
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response, String responseStr) throws IOException {
                        observer.onModifySettingsSuccess();
                    }
                });
    }


    /* 生成类似 1,2,3, 格式的字符串 */
    @SuppressWarnings("unused")
    private static String intJoin(int[] aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i:
                aArr) {
            sbStr.append(i);
            sbStr.append(sSep);
        }
        return sbStr.toString();
    }

    public interface ModifySettingsObserver{
        void onModifySettingsSuccess();
        void onModifySettingsFailure(int statusCode);
    }

    public interface MessageObserver {
        void onCheckNotificationSuccess(NotificationList notificationList);
        void onCheckNotificationFailure(int statusCode);
    }

    public interface GetProfileObserver{
        void onGetProfileSuccess();
        void onGetProfileFailure();
    }
}


import android.content.Context;
import android.content.SharedPreferences;

import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.User;
import com.google.gson.Gson;

/**
 * Created by jianhao on 16-9-3.
 * Manage and store data about user himself.
 */
public class Me {
    private static User me = new User();

    public static void clear(){
        me = new User();
    }

    public static boolean isEmpty(){
        return me.isEmpty();
    }

    public static int getMyUserId(){
        return me.getUserId();
    }

    public static String getMyUsername(){
        return me.getUsername();
    }

    public static String getMyAvatarSuffix(){
        return me.getAvatarSuffix();
    }

    @SuppressWarnings("unused")
    public static String getMyAvatarURL(){
        return Constants.avatarURL + "avatar_" + getMyUserId() + "." + getMyAvatarSuffix();
    }

    public static String getMyStatus(){
        return me.getStatus();
    }

    public static String getMySignature(){
        return me.getPreferences() != null && me.getPreferences().getSignature() != null ? me.getPreferences().getSignature() : "";
    }

    public static Boolean getMyPrivateAdd(){
        return me.getPreferences().getPrivateAdd();
    }

    public static Boolean getMyStarOnReply(){
        return me.getPreferences().getStarOnReply();
    }

    public static Boolean getMyStarPrivate(){
        return me.getPreferences().getStarPrivate();
    }

    public static Boolean getMyHideOnline(){
        return me.getPreferences().getHideOnline() != null ? me.getPreferences().getHideOnline() : false;
    }

    public static User.Preferences getPreferences() {
        return me.getPreferences();
    }

    public static String getUsername(){
        return me.getUsername();
    }

    public static void setUsername(String username){
        me.setUsername(username);
    }

    @SuppressWarnings("unused")
    public static void setPreferences(User.Preferences preferences){
        me.setPreferences(preferences);
    }


    public static int getUserId() {
        return getMyUserId();
    }

    public static void setUserId(int userId) {
        me.setUserId(userId);
    }

    public static String getStatus() {
        return me.getStatus();
    }

    public static void setStatus(String status) {
        me.setStatus(status);
    }

    public static String getAvatarSuffix() {
        return me.getAvatarSuffix();
    }

    public static void setAvatarSuffix(String avatarSuffix) {
        me.setAvatarSuffix(avatarSuffix);
    }

    public static void setProfile(Context context, User user) {
        user.setUsername(me.getUsername());
        user.setUserId(me.getUserId());
        me = user;
        me.setEmpty(false);
        if(user.getAvatarSuffix() == null){
            user.setAvatarSuffix(Constants.NONE);
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getUsername(), new Gson().toJson(user));
        editor.apply();
    }

    private static void setInstanceFromJSONStr(Context context, String jsonStr){
        User user2 = new Gson().fromJson(jsonStr, User.class);
        user2.setUserId(me.getUserId());
        user2.setUsername(me.getUsername());
        me = user2;
        me.setEmpty(false);
        if(getAvatarSuffix() == null){
            me.setAvatarSuffix(Constants.NONE);
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getMyUsername(), new Gson().toJson(me));
        editor.apply();
    }
    public static void setInstanceFromSharedPreference(Context context, String username) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        if(sharedPreferences.contains(username)) {
            String info = sharedPreferences.getString(username, "bing mei you");
            setInstanceFromJSONStr(context, info);
        }
    }

}


import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.TextView;

import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * 需要在线获取的图片交给它来显示
 * 其他处理在SFXParser3中进行
 */
public class OnlineImgTextView extends TextView implements IOnlineImgView
{
	private OnlineImgImpl mImpl;

	private static final String TAG = "OnlineImgTextView";

	public OnlineImgTextView(Context context, @Nullable List<Post.Attachment> attachmentList)
	{
		super(context);
		init(context, attachmentList);
	}

	public OnlineImgTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context, null);
	}

	public OnlineImgTextView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context, null);
	}

	public void setText(String text){
		mImpl.setText(text);
	}

	public void setText(String text, OnlineImgImpl.OnCompleteListener listener) {
		mImpl.setListener(listener);
		mImpl.setText(text);
	}

	private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
		setTextIsSelectable(true);
		mImpl = new OnlineImgImpl(this);
		mImpl.mAttachmentList = attachmentList;
	}

	@Override
	public void setText(SpannableStringBuilder builder) {
		((TextView) this).setText(builder);
	}
}


import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitute {@link OnlineImgImpl} temporarily
 * Created by daquexian on 17-2-6.
 */

public class OnlineImgUtils {
    private static final String TAG = "OnlineImgUtils";

    private static final String SITE = "http://latex.codecogs.com/gif.latex?\\dpi{220}";

    private static final Pattern PATTERN1 = Pattern.compile("(?i)\\$\\$?((.|\\n)+?)\\$\\$?");
    private static final Pattern PATTERN2 = Pattern.compile("(?i)\\\\[(\\[]((.|\\n)*?)\\\\[\\])]");
    private static final Pattern PATTERN3 = Pattern.compile("(?i)\\[tex]((.|\\n)*?)\\[/tex]");
    private static final Pattern PATTERN4 = Pattern.compile("(?i)\\\\begin\\{.*?\\}(.|\\n)*?\\\\end\\{.*?\\}");
    private static final Pattern PATTERN5 = Pattern.compile("(?i)\\$\\$(.+?)\\$\\$");
    private static final Pattern IMG_PATTERN = Pattern.compile("(?i)\\[img(=\\d+)?](.*?)\\[/img]");
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("(?i)\\[attachment:(.*?)]");
    private static final Pattern[] PATTERNS = {PATTERN1, PATTERN2, PATTERN3, PATTERN4, PATTERN5, IMG_PATTERN, ATTACHMENT_PATTERN};
    private static final int[] indexInRegex = {1, 1, 1, 0, 1, 2, 1};

    public static List<Formula> getAll(CharSequence charSequence, List<Post.Attachment> attachmentList) {
        return getSpecific(charSequence, attachmentList, new int[]{0, 1, 2, 3, 4, 5, 6});
    }

    public static List<Formula> getLaTeXFormula(CharSequence charSequence, List<Post.Attachment> attachmentList) {
        return getSpecific(charSequence, attachmentList, new int[]{0, 1, 2, 3, 4});
    }

    public static List<Formula> getImgAndAtt(CharSequence charSequence, List<Post.Attachment> attachmentList) {
        return getSpecific(charSequence, attachmentList, new int[]{5, 6});
    }

    /**
     * @param charSequence 包含公式的字符串
     * @return 公式List
     */
    private static List<Formula> getSpecific(CharSequence charSequence, List<Post.Attachment> attachmentList, int[] indexs) {
        Matcher[] matchers = new Matcher[indexs.length];
        int[] indexInRegexLocal = new int[indexs.length];
        for (int i = 0; i < indexs.length; i++) {
            int index = indexs[i];
            matchers[i] = PATTERNS[index].matcher(charSequence);
            indexInRegexLocal[i] = indexInRegex[index];
        }

        List<Formula> formulaList = new ArrayList<>();

        int[] types = {Formula.TYPE_1, Formula.TYPE_2, Formula.TYPE_3, Formula.TYPE_4, Formula.TYPE_5, Formula.TYPE_IMG, Formula.TYPE_ATT};

        for (int i = 0; i < matchers.length; i++) {
            Matcher matcher = matchers[i];
            int index = indexInRegexLocal[i];

            int start, end, size;
            String content, url;
            int type = types[i];

            while (matcher.find()) {
                url = "";
                size = -1;
                start = matcher.start();
                end = matcher.end();
                content = matcher.group(index);

                if (type == Formula.TYPE_IMG) {
                    url = content;
                    if (!TextUtils.isEmpty(matcher.group(1))) {
                        try {
                            size = Integer.parseInt(matcher.group(1).substring(1));
                        } catch (NumberFormatException e) {
                            size = -1;
                        }
                    }
                } else if (type == Formula.TYPE_ATT) {
                    for (int j = attachmentList.size() - 1; j >= 0; j--) {
                        Post.Attachment attachment = attachmentList.get(j);
                        if (attachment.getAttachmentId().equals(content)) {
                            for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
                                if (attachment.getFilename().endsWith(image_ext)) {
                                    url = MyUtils.getAttachmentImageUrl(attachment);
                                }
                            }
                        }
                    }
                } else {
                    url = SITE + content;
                }
                formulaList.add(new Formula(start, end, content, url, type, size));
            }
        }

        removeOverlappingFormula(formulaList);
        Collections.sort(formulaList);
        return formulaList;
    }

    /**
     * 去掉相交的区间
     * @param formulaList 每个LaTeX公式的起始下标和终止下标组成的List
     */
    public static void removeOverlappingFormula(List<Formula> formulaList) {
        Collections.sort(formulaList, new Comparator<Formula>() {
            @Override
            public int compare(Formula p1, Formula p2) {
                return p1.start - p2.start;
            }
        });
        int size = formulaList.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size;) {
                if (formulaList.get(j).start < formulaList.get(i).end) {
                    formulaList.remove(j);
                    size--;
                } else j++;
            }
        }
    }

    /**
     * Fix the "bug" that ImageSpan shows as many as the num of lines of the string substituted
     * @param str string containing formulas
     * @return string containing single-line formulas
     */
    public static String removeNewlineInFormula(String str){
        Matcher m1 = PATTERN1.matcher(str);
        Matcher m2 = PATTERN2.matcher(str);
        Matcher m3 = PATTERN3.matcher(str);
        Matcher m4 = PATTERN4.matcher(str);
        Matcher m5 = PATTERN5.matcher(str);
        Matcher[] matchers = {m1, m2, m3, m4, m5};
        for (Matcher matcher : matchers) {
            while (matcher.find()) {
                String oldStr = matcher.group();
                String newStr = oldStr.replaceAll("[\\n\\r]", "");

                str = str.replace(oldStr, newStr);
            }
        }

        return str;
    }

    /**
     * 获取特定的公式渲染出的图片，结束时会调用回调函数（假如存在listener的话）
     * @param builder 包含公式的builder
     * @param i 公式在mFormulaList中的下标
     * @param start index in full text of first char in builder, use for adjust the beginning and ending of formula to fit with builder
     */
    public static void retrieveFormulaOnlineImg(final List<Formula> formulaList, final TextView view, final SpannableStringBuilder builder, final int i, final int start) {
        if (i >= formulaList.size()) {
            return;
        }
        final Formula formula = formulaList.get(i);
        Log.d(TAG, "retrieveFormulaOnlineImg: " + formula.url);
        final int finalStart = formula.start;
        final int finalEnd = formula.end;
        Glide.with(view.getContext())
                .load(formula.url)
                .asBitmap()
                .placeholder(new ColorDrawable(ContextCompat.getColor(view.getContext(), android.R.color.darker_gray)))
                .into(new SimpleTarget<Bitmap>()
                {
                    @Override
                    public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
                    {
                        final int HEIGHT_THRESHOLD = 60;
                        // post to avoid ConcurrentModificationException, from https://github.com/bumptech/glide/issues/375
                        view.post(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap newImage;
                                if (resource.getWidth() > Constants.MAX_IMAGE_WIDTH) {
                                    int newHeight = resource.getHeight() * Constants.MAX_IMAGE_WIDTH / resource.getWidth();
                                    newImage = Bitmap.createScaledBitmap(resource, Constants.MAX_IMAGE_WIDTH, newHeight, true);
                                } else {
                                    newImage = resource;
                                }

                                if(newImage.getHeight() > HEIGHT_THRESHOLD) {
                                    builder.setSpan(new ImageSpan(((View)view).getContext(), newImage), finalStart - start, finalEnd - start, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                } else {
                                    builder.setSpan(new CenteredImageSpan(((View)view).getContext(), newImage), finalStart - start, finalEnd - start, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                                }

                                if (view instanceof EditText) {
                                    EditText editText = (EditText) view;
                                    int selectionStart = editText.getSelectionStart();
                                    int selectionEnd = editText.getSelectionEnd();
                                    view.setText(builder);
                                    editText.setSelection(selectionStart, selectionEnd);
                                } else {
                                    view.setText(builder);
                                }
                                retrieveFormulaOnlineImg(formulaList, view, builder, i + 1, start);
                            }
                        });
                    }
                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        if (e != null) e.printStackTrace();
                        retrieveFormulaOnlineImg(formulaList, view, builder, i + 1, start);
                    }

                });
    }

    /**
     *
     * @param formulaList the whole formula list
     * @param start [start, end)
     * @param end [start, end)
     * @return formulas in [start, end)
     */
    public static List<Formula> formulasBetween(List<Formula> formulaList, int start, int end) {
        int first = -1, last = -1;
        for (int i = 0; i < formulaList.size(); i++) {
            Formula formula = formulaList.get(i);
            if (first == -1 && formula.start >= start) {
                first = i;
            }
            if (formula.end > end) {
                last = i;
                break;
            }
        }

        if (first == -1 && last == -1) {
            return formulaList;
        } else if (last == -1) {
            return formulaList.subList(first, formulaList.size());
        }

        return formulaList.subList(first, last);
    }

    public static class Formula implements Comparable<Formula> {
        static final int TYPE_1 = 1;
        static final int TYPE_2 = 2;
        static final int TYPE_3 = 3;
        static final int TYPE_4 = 4;
        static final int TYPE_5 = 5;
        static final int TYPE_IMG = 6;
        static final int TYPE_ATT = 7;
        int start, end;
        String content, url;
        int type;
        int size;

        Formula(int start, int end, String content, String url, int type) {
            this(start, end, content, url, type, -1);
        }

        Formula(int start, int end, String content, String url, int type, int size) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.url = url;
            this.type = type;
            this.size = size;
        }

        @Override
        public int compareTo(@NotNull Formula formula) {
            return Integer.valueOf(start).compareTo(formula.start);
        }
    }
}


import android.content.Context;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;

public enum Channel
{
	caff(R.string.channel_caff, 1, 0xFFA0A0A0),
	ad(R.string.channel_ad, 3, 0xFF999999),
	maths(R.string.channel_maths, 4, 0xFF673AB7),
	physics(R.string.channel_physics, 5, 0xFFFF5722),
	chem(R.string.channel_chem, 6, 0xFFF44336),
	biology(R.string.channel_biology, 7, 0xFF4CAF50),
	tech(R.string.channel_tech, 8, 0xFF2196F3),
	test(R.string.channel_test, 9, 0xFF607D8B),
	admin(R.string.channel_admin, 24, 0xFFEAEAEA),
	court(R.string.channel_court, 25, 0xFFE040D0),
	recycled(R.string.channel_recycled, 27, 0xFFA6B3E0),
	announ(R.string.channel_announ, 28, 0xFF999999),
	others(R.string.channel_others, 30, 0xFF3F5185),
	socsci(R.string.channel_socsci, 34, 0xFFE04000),
	lang(R.string.channel_lang, 40, 0xFF9030C0);

	private int name;
	private int channelId;
	private int color;

	Channel(int name, int channelId, int color)
	{
		this.name = name;
		this.channelId = channelId;
		this.color = color;
	}

	public int getChannelId()
	{
		return channelId;
	}

	public int getColor()
	{
		return color;
	}

	public String toString()
	{
		return ChaoliApplication.getAppContext().getString(this.name);
	}

	@Deprecated
	public String toString(Context context)
	{
		return context.getString(this.name);
	}


	public static Channel getChannel(int channelId)
	{
		for (Channel c : Channel.values())
			if (c.getChannelId() == channelId) return c;
		return null;
	}

	public static Channel getChannel(String channelName)
	{
		for (Channel c : Channel.values())
			if (c.toString().equals(channelName)) return c;
		return null;
	}
}


import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

import static android.content.Context.MODE_PRIVATE;

/**
 * The helper to night mode
 * Created by root on 1/22/17.
 */

public class NightModeHelper{
    private static final String MODE = "night_mode";
    private static final String NIGHT = "night";
    private static final String DAY = "day";

    private static BaseViewModel mViewModel;

    private static SharedPreferences getSp(){
        return ChaoliApplication.getSp();
    }

    public static boolean isDay(){
        String temp = getSp().getString(MODE,DAY);
        return temp.equals(DAY);
    }

    public static void changeMode(BaseViewModel viewModel) {
        if (isDay()) {
            setNight();
        } else {
            setDay();
        }
        mViewModel = viewModel;
    }

    public static BaseViewModel getViewModel() {
        return mViewModel;
    }

    public static void removeViewModel() {
            mViewModel = null;
    }

    private static void setNight(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        SharedPreferences.Editor editor = getSp().edit();
        editor.putString(MODE,NIGHT);
        editor.apply();
    }

    private static void setDay(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        SharedPreferences.Editor editor = getSp().edit();
        editor.putString(MODE,DAY);
        editor.apply();
    }
}


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.utils.MyUtils;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SFXParser3 {
	// Finally, I decide to use this way.

	private static final String TAG = "SFXParser3";

	private static final String[] iconStrs = new String[]{"/:)", "/:D", "/^b^", "/o.o", "/xx", "/#", "/))", "/--", "/TT", "/==",
			"/.**", "/:(", "/vv", "/$$", "/??", "/:/", "/xo", "/o0", "/><", "/love",
			"/...", "/XD", "/ii", "/^^", "/<<", "/>.", "/-_-", "/0o0", "/zz", "/O!O",
			"/##", "/:O", "/<", "/heart", "/break", "/rose", "/gift", "/bow", "/moon", "/sun",
			"/coin", "/bulb", "/tea", "/cake", "/music", "/rock", "/v", "/good", "/bad", "/ok",
			"/asnowwolf-smile", "/asnowwolf-laugh", "/asnowwolf-upset", "/asnowwolf-tear",
			"/asnowwolf-worry", "/asnowwolf-shock", "/asnowwolf-amuse"};
	private static final int[] icons = new int[]{R.drawable.emoticons__0050_1, R.drawable.emoticons__0049_2, R.drawable.emoticons__0048_3, R.drawable.emoticons__0047_4,
			R.drawable.emoticons__0046_5, R.drawable.emoticons__0045_6, R.drawable.emoticons__0044_7, R.drawable.emoticons__0043_8, R.drawable.emoticons__0042_9,
			R.drawable.emoticons__0041_10, R.drawable.emoticons__0040_11, R.drawable.emoticons__0039_12, R.drawable.emoticons__0038_13, R.drawable.emoticons__0037_14,
			R.drawable.emoticons__0036_15, R.drawable.emoticons__0035_16, R.drawable.emoticons__0034_17, R.drawable.emoticons__0033_18, R.drawable.emoticons__0032_19,
			R.drawable.emoticons__0031_20, R.drawable.emoticons__0030_21, R.drawable.emoticons__0029_22, R.drawable.emoticons__0028_23, R.drawable.emoticons__0027_24,
			R.drawable.emoticons__0026_25, R.drawable.emoticons__0025_26, R.drawable.emoticons__0024_27, R.drawable.emoticons__0023_28, R.drawable.emoticons__0022_29,
			R.drawable.emoticons__0021_30, R.drawable.emoticons__0020_31, R.drawable.emoticons__0019_32, R.drawable.emoticons__0018_33, R.drawable.emoticons__0017_34,
			R.drawable.emoticons__0016_35, R.drawable.emoticons__0015_36, R.drawable.emoticons__0014_37, R.drawable.emoticons__0013_38, R.drawable.emoticons__0012_39,
			R.drawable.emoticons__0011_40, R.drawable.emoticons__0010_41, R.drawable.emoticons__0009_42, R.drawable.emoticons__0008_43, R.drawable.emoticons__0007_44,
			R.drawable.emoticons__0006_45, R.drawable.emoticons__0005_46, R.drawable.emoticons__0004_47, R.drawable.emoticons__0003_48, R.drawable.emoticons__0002_49,
			R.drawable.emoticons__0001_50, R.drawable.asonwwolf_smile, R.drawable.asonwwolf_laugh, R.drawable.asonwwolf_upset, R.drawable.asonwwolf_tear,
			R.drawable.asonwwolf_worry, R.drawable.asonwwolf_shock, R.drawable.asonwwolf_amuse};

	private static String[] TAGS = {"(?i)\\[center]", "(?i)\\[/center]", "(?i)\\[h]", "(?i)\\[/h]", "(?i)\\[s]", "(?i)\\[/s]", "(?i)\\[b]", "(?i)\\[/b]",
			"(?i)\\[i]", "(?i)\\[/i]", "(?i)\\[c=.{3,6}]", "(?i)\\[/c]", "(?i)\\[curtain]", "(?i)\\[/curtain]"};

	/**
	 * remove remaining tags (they indicate resolve error)
	 * plan to refactor the code to avoid these error
     */
	public static SpannableStringBuilder removeTags(SpannableStringBuilder builder) {
		for (String tag : TAGS) {
			Pattern pattern = Pattern.compile(tag);
			Matcher matcher = pattern.matcher(builder);
			while (matcher.find()) {
				builder.replace(matcher.start(), matcher.end(), "");
				matcher = pattern.matcher(builder);
			}
		}
		return builder;
	}

	public static SpannableStringBuilder parse(final Context context, SpannableStringBuilder builder, List<Post.Attachment> attachmentList) {
		Pattern cPattern = Pattern.compile("(?i)\\[c=(.*?)]((.|\n)*?)\\[/c]");
		Matcher c = cPattern.matcher(builder);
		while (c.find()) {
			try {
				int color = Color.parseColor(c.group(1));
				builder.setSpan(new ForegroundColorSpan(color), c.start(), c.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

				builder.replace(c.end(2), c.end(), "");
				builder.replace(c.start(), c.start(2), "");
				c = cPattern.matcher(builder);
			} catch (IllegalArgumentException e) {
				//避免不支持的颜色引起crash
			}
		}

		cPattern = Pattern.compile("(?i)\\[color=(.*?)]((.|\n)*?)\\[/color]");
		c = cPattern.matcher(builder);
		while (c.find()) {
			try {
				int color = Color.parseColor(c.group(1));
				builder.setSpan(new ForegroundColorSpan(color), c.start(), c.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

				builder.replace(c.end(2), c.end(), "");
				builder.replace(c.start(), c.start(2), "");
				c = cPattern.matcher(builder);
			} catch (IllegalArgumentException e) {
				//避免不支持的颜色引起crash
			}
		}

		Pattern urlPattern = Pattern.compile("(?i)\\[url=(.*?)](.*?)\\[/url]");
		Matcher url = urlPattern.matcher(builder);
		while (url.find()) {
			String site = url.group(1).toLowerCase();
			if (!site.startsWith("http://") && !site.startsWith("https://")) {
				site = "http://" + site;
			}

			final String finalSite = site;
			builder.setSpan(new ClickableSpan() {
				@Override
				public void onClick(View widget) {
					context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalSite)));
				}
			}, url.start(2), url.end(2), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(url.end(2), url.end(), "");
			builder.replace(url.start(), url.start(2), "");
			url = urlPattern.matcher(builder);
		}

		Pattern curtainPattern = Pattern.compile("(?i)(?<=\\[curtain\\])((.|\\n)+?)(?=\\[/curtain\\])");
		Matcher curtain = curtainPattern.matcher(builder);
		while (curtain.find()) {
			builder.setSpan(new BackgroundColorSpan(Color.DKGRAY), curtain.start(), curtain.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//			spannable.setSpan(new Touchable, curtain.start(), curtain.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(curtain.end(), curtain.end() + 10, "");
			builder.replace(curtain.start() - 9, curtain.start(), "");
			curtain = Pattern.compile("(?i)(?<=\\[curtain\\])(.+?)(?=\\[/curtain\\])").matcher(builder);
		}

		Pattern bPattern = Pattern.compile("(?i)\\[b]((.|\\n)+?)\\[/b]");
		Matcher b = bPattern.matcher(builder);
		while (b.find()) {
			builder.setSpan(new StyleSpan(Typeface.BOLD), b.start(1), b.end(1), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(b.end(1), b.end(), "");
			builder.replace(b.start(), b.start(1), "");
			b = bPattern.matcher(builder);
		}

		Pattern iPattern = Pattern.compile("(?i)(?<=\\[i\\])((.|\\n)+?)(?=\\[/i\\])");
		Matcher i = iPattern.matcher(builder);
		while (i.find()) {
			builder.setSpan(new StyleSpan(Typeface.ITALIC), i.start(), i.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(i.end(), i.end() + 4, "");
			builder.replace(i.start() - 3, i.start(), "");
			i = iPattern.matcher(builder);
		}

		Pattern uPattern = Pattern.compile("(?i)(?<=\\[u\\])((.|\\n)+?)(?=\\[/u\\])");
		Matcher u = uPattern.matcher(builder);
		while (u.find()) {
			builder.setSpan(new UnderlineSpan(), u.start(), u.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(u.end(), u.end() + 4, "");
			builder.replace(u.start() - 3, u.start(), "");
			u = uPattern.matcher(builder);
		}

		Pattern sPattern = Pattern.compile("(?i)(?<=\\[s\\])((.|\\n)+?)(?=\\[/s\\])");
		Matcher s = sPattern.matcher(builder);
		while (s.find()) {
			builder.setSpan(new StrikethroughSpan(), s.start(), s.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(s.end(), s.end() + 4, "");
			builder.replace(s.start() - 3, s.start(), "");
			s = sPattern.matcher(builder);
		}

		Pattern centerPattern = Pattern.compile("(?i)(?<=\\[center\\])((.|\\n)+?)(?=\\[/center\\])");
		Matcher center = centerPattern.matcher(builder);
		while (center.find()) {
			builder.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), center.start(), center.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(center.end(), center.end() + 9, "\n\n");
			builder.replace(center.start() - 8, center.start(), "\n\n");
			center = centerPattern.matcher(builder);
		}

		Pattern hPattern = Pattern.compile("(?i)(?<=\\[h\\])((.|\\n)+?)(?=\\[/h\\])");
		Matcher h = hPattern.matcher(builder);
		while (h.find()) {
			builder.setSpan(new RelativeSizeSpan(1.3f), h.start(), h.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			builder.replace(h.end(), h.end() + 4, "\n\n");
			builder.replace(h.start() - 3, h.start(), "\n\n");
			h = hPattern.matcher(builder);
		}


		Pattern attachmentPattern = Pattern.compile("(?i)\\[attachment:(.*?)]");
		Matcher attachmentM = attachmentPattern.matcher(builder);
		boolean isImage = false;
		while (attachmentList != null && attachmentM.find()) {
			for (int j = attachmentList.size() - 1; j >= 0; j--) {
				final Post.Attachment attachment = attachmentList.get(j);
				if (attachment.getAttachmentId().equals(attachmentM.group(1))) {
					// skip images
					for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
						if (attachment.getFilename().endsWith(image_ext)) {
							isImage = true;
						}
					}

					if (!isImage) {
						builder.replace(attachmentM.start(), attachmentM.end(), attachment.getFilename());
						builder.setSpan(new ClickableSpan() {
							@Override
							public void onClick(View view) {
								MyUtils.downloadAttachment(context, attachment);
							}
						}, attachmentM.start(), attachmentM.start() + attachment.getFilename().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
						attachmentM = attachmentPattern.matcher(builder);
					}
					break;
				}
			}
		}

		String str = builder.toString();
		for (int j = 0; j < iconStrs.length; j++) {
			int from = 0;
			String iconStr = iconStrs[j];
			while ((from = str.indexOf(iconStr, from)) >= 0) {
				if (("/<".equals(iconStr) && str.substring(from).startsWith("/<<") || ("/#".equals(iconStr) && str.substring(from).startsWith("/##")))) {
					from++;
					continue;
				}

				/**
				 * only show icons when iconStr is surrounded by spaces
				 */
				if (iconStr.equals("/^^")) Log.d(TAG, "parse: " + str.trim().length() + ", " + iconStr.length() + ", " + (from + iconStr.length()) + ", " + str.length());
				if (str.trim().length() == iconStr.length() ||
						((from == 0 || ' ' == str.charAt(from - 1)) && (from + iconStr.length() == str.length() || ' ' == str.charAt(from + iconStr.length()) || '\n' == str.charAt(from + iconStr.length())))) {
					builder.setSpan(new ImageSpan(context, icons[j]), from, from + iconStr.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
				}
				from += iconStr.length();
			}
		}
		return builder;
	}
	public static SpannableStringBuilder parse(final Context context, String string, List<Post.Attachment> attachmentList) {
		final SpannableStringBuilder builder = new SpannableStringBuilder(string);
		return parse(context, builder, attachmentList);
	}
}


import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.EditText;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * Created by daquexian on 16-11-15.
 */

public class OnlineImgEditText extends EditText implements IOnlineImgView {
    private OnlineImgImpl mImpl;
    private boolean onlineImgEnabled = false;

    public static final String TAG = "OnlineImgEditText";

    public OnlineImgEditText(Context context, @Nullable List<Post.Attachment> attachmentList)
    {
        super(context);
        init(context, attachmentList);
    }

    public OnlineImgEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, null);
    }

    public OnlineImgEditText(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void update() {
        mySetText(getText().toString());
    }

    public void setOnlineImgEnabled(boolean onlineImgEnabled) {
        this.onlineImgEnabled = onlineImgEnabled;
    }

    /*
        如果命名成setText(String text)就会引起Data Binding的双向绑定导致的循环。。不创建这个方法或者是其他名字就不会，好奇怪哈哈哈
         */
    public void mySetText(String text){
        if (onlineImgEnabled) mImpl.setText(text);
        else setText(SFXParser3.parse(ChaoliApplication.getAppContext(), text, null));
    }

    public void mySetText(String text, OnlineImgImpl.OnCompleteListener listener) {
        if (onlineImgEnabled) {
            mImpl.setListener(listener);
            mImpl.setText(text);
        } else {
            SpannableStringBuilder builder = SFXParser3.parse(ChaoliApplication.getAppContext(), text, null);
            super.setText(builder);
            listener.onComplete(builder);
        }
    }

    private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
        setTextIsSelectable(true);
        mImpl = new OnlineImgImpl(this);
        mImpl.mAttachmentList = attachmentList;
    }

    @Override
    public void setText(SpannableStringBuilder builder) {
        ((EditText) this).setText(builder);
    }
}


import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class implements almost all method about retrieve images from web
 * Created by jianhao on 16-10-22.
 */

// TODO: 17-2-10 refactor like OnlineImgUtils
class OnlineImgImpl {
    List<Post.Attachment> mAttachmentList;
    private List<Formula> mFormulaList;
    private ArrayMap<Formula, ImageSpan> placeHolders = new ArrayMap<>();

    private OnCompleteListener mListener;

    private IOnlineImgView mView;

    private static final String SITE = "http://latex.codecogs.com/gif.latex?\\dpi{220}";

    private static final Pattern PATTERN1 = Pattern.compile("(?i)\\$\\$?((.|\\n)+?)\\$\\$?");
    private static final Pattern PATTERN2 = Pattern.compile("(?i)\\\\[(\\[]((.|\\n)*?)\\\\[\\])]");
    private static final Pattern PATTERN3 = Pattern.compile("(?i)\\[tex]((.|\\n)*?)\\[/tex]");
    private static final Pattern IMG_PATTERN = Pattern.compile("(?i)\\[img](.*?)\\[/img]");
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("(?i)\\[attachment:(.*?)]");
    private static final Pattern PATTERN4 = Pattern.compile("(?i)\\\\begin\\{.*?\\}(.|\\n)*?\\\\end\\{.*?\\}");
    private static final Pattern PATTERN5 = Pattern.compile("(?i)\\$\\$(.+?)\\$\\$");

    private static final String TAG = "OnlineImgImpl";

    OnlineImgImpl(IOnlineImgView view) {
        //maxWidthPixels = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.8);
        mView = view;
    }

    public void setText(String text){
        text = removeNewlineInFormula(text);
        text += '\n';

        SpannableStringBuilder builder = SFXParser3.parse(((View) mView).getContext(), text, mAttachmentList);

        if (mView instanceof EditText) {
            EditText editText = (EditText) mView;
            int selectionStart = editText.getSelectionStart();
            int selectionEnd = editText.getSelectionEnd();
            mView.setText(builder);
            editText.setSelection(selectionStart, selectionEnd);
        } else {
            mView.setText(builder);
        }

        retrieveOnlineImg(builder);
    }

    public void setView(IOnlineImgView view) {
        mView = view;
    }

    /**
     * 此操作是异步的，注意
     * @param builder 包含公式的文本，以SpannableStringBuilder身份传入
     */
    private void retrieveOnlineImg(final SpannableStringBuilder builder) {
        String text = builder.toString();

        mFormulaList = getAllFormulas(text);

        showPlaceHolder(builder);
        retrieveFormulaOnlineImg(builder, 0);
    }

    /**
     * 获取所有起始位置和终止位置不相交的公式
     * @param string 包含公式的字符串
     * @return 公式List
     */
    private List<Formula> getAllFormulas(String string) {
        Matcher m1 = PATTERN1.matcher(string);
        Matcher m2 = PATTERN2.matcher(string);
        Matcher m3 = PATTERN3.matcher(string);
        Matcher m4 = PATTERN4.matcher(string);
        Matcher m5 = PATTERN5.matcher(string);
        Matcher imgMatcher = IMG_PATTERN.matcher(string);
        Matcher attachmentMatcher = ATTACHMENT_PATTERN.matcher(string);

        List<Formula> formulaList = new ArrayList<>();
        String content;
        int type;

        // TODO: 16-10-22 replace it with a loop
        Boolean flag1 = false, flag2 = false, flag3 = false, flag4 = false, flag5 = false, flagImg = false, flagAttachment = false;
        while ((flagAttachment = attachmentMatcher.find()) || (flagImg = imgMatcher.find()) || (flag1 = m1.find()) || (flag2 = m2.find()) || (flag3 = m3.find())
                || (flag4 = m4.find()) || (flag5 = m5.find())) {
            int start, end;
            if (flagAttachment) {
                start = attachmentMatcher.start();
                end = attachmentMatcher.end();
                content = attachmentMatcher.group(1);
                type = Formula.TYPE_ATT;
            } else if (flagImg) {
                start = imgMatcher.start();
                end = imgMatcher.end();
                content = imgMatcher.group(1);
                type = Formula.TYPE_IMG;
            } else if (flag5) {
                start = m5.start();
                end = m5.end();
                content = m5.group(1);
                type = Formula.TYPE_5;
            } else if (flag4) {
                start = m4.start();
                end = m4.end();
                content = m4.group(0);
                type = Formula.TYPE_4;
            } else if (flag3) {
                start = m3.start();
                end = m3.end();
                content = m3.group(1);
                type = Formula.TYPE_3;
            } else if (flag2) {
                start = m2.start();
                end = m2.end();
                content = m2.group(1);//.replaceAll("[ \\t\\r\\n]", "");
                type = Formula.TYPE_2;
            } else {
                start = m1.start();
                end = m1.end();
                content = m1.group(1);
                type = Formula.TYPE_1;
            }
            String url = "";
            if (flagImg) {
                url = content;
            } else if (flagAttachment) {
                for (int i = mAttachmentList.size() - 1; i >= 0; i--) {
                    Post.Attachment attachment = mAttachmentList.get(i);
                    if (attachment.getAttachmentId().equals(content)) {
                        for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
                            if (attachment.getFilename().endsWith(image_ext)) {
                                url = MyUtils.getAttachmentImageUrl(attachment);
                            }
                        }
                    }
                }
            } else {
                url = SITE + content;
            }
            formulaList.add(new Formula(start, end, content, url, type));
        }
        removeOverlappingFormula(formulaList);
        return formulaList;
    }

    /**
     * show placeholder on attachment images
     * @param builder SpannableStringBuilder in which placeholder shows
     */
    private void showPlaceHolder(final SpannableStringBuilder builder) {
        for (Formula formula : mFormulaList) {
            Log.d(TAG, "showPlaceHolder: " + formula.content + ", " + formula.type);
            if (formula.type == Formula.TYPE_ATT || formula.type == Formula.TYPE_IMG) {
                final ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(((View) mView).getContext(), android.R.color.darker_gray));
                colorDrawable.setBounds(0, 0, 600, 300);
                final ImageSpan imageSpan = new ImageSpan(colorDrawable);
                placeHolders.put(formula, imageSpan);
                builder.setSpan(imageSpan, formula.start, formula.end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
        mView.setText(builder);
    }

    /**
     * 获取特定的公式渲染出的图片，结束时会调用回调函数（假如存在listener的话）
     * @param builder 包含公式的builder
     * @param i 公式在mFormulaList中的下标
     */
    private void retrieveFormulaOnlineImg(final SpannableStringBuilder builder, final int i) {
        if (i >= mFormulaList.size()) {
            if (mListener != null) {
                mListener.onComplete(builder);
            }
            return;
        }
        Spannable spannable;
        final Formula formula = mFormulaList.get(i);
        Log.d(TAG, "retrieveFormulaOnlineImg: " + formula.url);
        final int finalType = formula.type;
        final int finalStart = formula.start;
        final int finalEnd = formula.end;
        Glide.with(((View)mView).getContext())
                .load(formula.url)
                .asBitmap()
                .placeholder(new ColorDrawable(ContextCompat.getColor(((View)mView).getContext(),android.R.color.darker_gray)))
                .into(new SimpleTarget<Bitmap>()
        {
            @Override
            public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation)
            {
                final int HEIGHT_THRESHOLD = 60;
                // post to avoid ConcurrentModificationException, from https://github.com/bumptech/glide/issues/375
                ((View)mView).post(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap newImage;
                        if (resource.getWidth() > Constants.MAX_IMAGE_WIDTH) {
                            int newHeight = resource.getHeight() * Constants.MAX_IMAGE_WIDTH / resource.getWidth();
                            newImage = Bitmap.createScaledBitmap(resource, Constants.MAX_IMAGE_WIDTH, newHeight, true);
                        } else {
                            newImage = resource;
                        }

                        if(finalType == Formula.TYPE_ATT || finalType == Formula.TYPE_IMG || newImage.getHeight() > HEIGHT_THRESHOLD) {
                            if (finalType == Formula.TYPE_ATT || finalType == Formula.TYPE_IMG) {
                                builder.removeSpan(placeHolders.get(formula));
                                placeHolders.remove(formula);
                            }
                            builder.setSpan(new ImageSpan(((View)mView).getContext(), newImage), finalStart, finalEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        } else {
                            builder.setSpan(new CenteredImageSpan(((View)mView).getContext(), resource), finalStart, finalEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        }

                        if (mView instanceof EditText) {
                            EditText editText = (EditText) mView;
                            int selectionStart = editText.getSelectionStart();
                            int selectionEnd = editText.getSelectionEnd();
                            mView.setText(builder);
                            editText.setSelection(selectionStart, selectionEnd);
                        } else {
                            mView.setText(builder);
                        }
                        retrieveFormulaOnlineImg(builder, i + 1);
                    }
                });
            }
            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                super.onLoadFailed(e, errorDrawable);
                if (e != null) e.printStackTrace();
                retrieveFormulaOnlineImg(builder, i + 1);
            }

        });
    }

    /**
     * 去掉相交的区间
     * @param formulaList 每个LaTeX公式的起始下标和终止下标组成的List
     */
    private void removeOverlappingFormula(List<Formula> formulaList) {
        Collections.sort(formulaList, new Comparator<Formula>() {
            @Override
            public int compare(Formula p1, Formula p2) {
                return p1.start - p2.start;
            }
        });
        int size = formulaList.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size;) {
                if (formulaList.get(j).start < formulaList.get(i).end) {
                    formulaList.remove(j);
                    size--;
                } else j++;
            }
        }
    }

    private String removeNewlineInFormula(String str){
        Matcher m1 = PATTERN1.matcher(str);
        Matcher m2 = PATTERN2.matcher(str);
        Matcher m3 = PATTERN3.matcher(str);
        Matcher m4 = PATTERN4.matcher(str);
        Matcher m5 = PATTERN5.matcher(str);
        Boolean flag5 = false, flag4 = false, flag3 = false, flag2 = false, flag1 = false;
        while ((flag1 = m1.find()) || (flag2 = m2.find()) || (flag3 = m3.find()) || (flag4 = m4.find()) || (flag5 = m5.find())) {
            String oldStr;
            if (flag5) oldStr = m5.group();
            else if (flag4) oldStr = m4.group();
            else if (flag3) oldStr = m3.group();
            else if (flag2) oldStr = m2.group();
            else oldStr = m1.group();
            String newStr = oldStr.replaceAll("[\\n\\r]", "");
            str = str.replace(oldStr, newStr);
        }

        return str;
    }


    public void setListener(OnCompleteListener listener){
        mListener = listener;
    }

    public interface OnCompleteListener {
        void onComplete(SpannableStringBuilder spannableStringBuilder);
    }

    private static class Formula {
        static final int TYPE_1 = 1;
        static final int TYPE_2 = 2;
        static final int TYPE_3 = 3;
        static final int TYPE_4 = 4;
        static final int TYPE_5 = 5;
        static final int TYPE_IMG = 4;
        static final int TYPE_ATT = 5;
        int start, end;
        String content, url;
        int type;

        Formula(int start, int end, String content, String url, int type) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.url = url;
            this.type = type;
        }
    }
}


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by jianhao on 16-8-31.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private Drawable mDivider;

    /**
     * Default divider will be used
     */
    public DividerItemDecoration(Context context) {
        final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
        mDivider = styledAttributes.getDrawable(0);
        styledAttributes.recycle();
    }

    /**
     * Custom divider will be used
     */
    public DividerItemDecoration(Context context, int resId) {
        mDivider = ContextCompat.getDrawable(context, resId);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }
}


import android.text.SpannableStringBuilder;

/**
 * Created by jianhao on 16-10-22.
 */

public interface IOnlineImgView {
    void setText(SpannableStringBuilder builder);
}


import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

/**
 * Extended AppBarLayout, add getOffset and getState method and so on.
 * Created by daquexian on 16-12-2.
 */

public class MyAppBarLayout extends AppBarLayout
        implements AppBarLayout.OnOffsetChangedListener {

    private State state;
    private int offset;
    private OnStateChangeListener onStateChangeListener;

    public MyAppBarLayout(Context context) {
        super(context);
    }

    public MyAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!(getLayoutParams() instanceof CoordinatorLayout.LayoutParams)
                || !(getParent() instanceof CoordinatorLayout)) {
            throw new IllegalStateException(
                    "MyAppBarLayout must be a direct child of CoordinatorLayout.");
        }
        addOnOffsetChangedListener(this);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0) {
            if (onStateChangeListener != null && state != State.EXPANDED) {
                onStateChangeListener.onStateChange(State.EXPANDED);
            }
            state = State.EXPANDED;
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            if (onStateChangeListener != null && state != State.COLLAPSED) {
                onStateChangeListener.onStateChange(State.COLLAPSED);
            }
            state = State.COLLAPSED;
        } else {
            if (onStateChangeListener != null && state != State.IDLE) {
                onStateChangeListener.onStateChange(State.IDLE);
            }
            state = State.IDLE;
        }
        offset = verticalOffset;
    }

    public int getOffset() {
        return offset;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    public interface OnStateChangeListener {
        void onStateChange(State toolbarChange);
    }

    public State getState() {
        return state;
    }

    public enum State {
        COLLAPSED,
        EXPANDED,
        IDLE
    }
}


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;

public class CenteredImageSpan extends ImageSpan
{
	public CenteredImageSpan(Bitmap b)
	{
		super(b);
	}

	public CenteredImageSpan(Bitmap b, int verticalAlignment)
	{
		super(b, verticalAlignment);
	}

	public CenteredImageSpan(Context context, Bitmap b)
	{
		super(context, b);
	}

	public CenteredImageSpan(Context context, Bitmap b, int verticalAlignment)
	{
		super(context, b, verticalAlignment);
	}

	public CenteredImageSpan(Drawable d)
	{
		super(d);
	}

	public CenteredImageSpan(Drawable d, int verticalAlignment)
	{
		super(d, verticalAlignment);
	}

	public CenteredImageSpan(Drawable d, String source)
	{
		super(d, source);
	}

	public CenteredImageSpan(Drawable d, String source, int verticalAlignment)
	{
		super(d, source, verticalAlignment);
	}

	public CenteredImageSpan(Context context, Uri uri)
	{
		super(context, uri);
	}

	public CenteredImageSpan(Context context, Uri uri, int verticalAlignment)
	{
		super(context, uri, verticalAlignment);
	}

	public CenteredImageSpan(Context context, int resourceId)
	{
		super(context, resourceId);
	}

	public CenteredImageSpan(Context context, int resourceId, int verticalAlignment)
	{
		super(context, resourceId, verticalAlignment);
	}

	@Override
	public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint)
	{
		Drawable d = getDrawable();
		Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		int transY = y + (fm.descent + fm.ascent - d.getBounds().bottom) / 2;
		canvas.save();
		canvas.translate(x, transY);
		d.draw(canvas);
		canvas.restore();
	}

	public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
		Drawable d = getDrawable();
		Rect rect = d.getBounds();
		if (fm != null) {
			Paint.FontMetricsInt fmPaint=paint.getFontMetricsInt();
			int fontHeight = fmPaint.bottom - fmPaint.top;
			int drHeight=rect.bottom-rect.top;
			int top= drHeight/2 - fontHeight/4;
			int bottom=drHeight/2 + fontHeight/4;

			fm.ascent=-bottom;
			fm.top=-bottom;
			fm.bottom=top;
			fm.descent=top;
		}

		return rect.right;
	}
}


import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.kbiakov.codeview.CodeView;

/**
 * 包含QuoteView和OnlineImgTextView
 * 用于显示帖子
 * Created by jianhao on 16-8-26.
 */
public class PostContentView extends LinearLayout {
    private final static String TAG = "PostContentView";
    private final static String QUOTE_START_TAG = "[quote";
    private final static Pattern QUOTE_START_PATTERN = Pattern.compile("\\[quote(=(\\d+?):@(.*?))?]");
    private final static String QUOTE_END_TAG = "[/quote]";
    private final static String CODE_START_TAG = "[code]";
    private final static String CODE_END_TAG = "[/code]";
    private final static Pattern ATTACHMENT_PATTERN = Pattern.compile("\\[attachment:(.*?)]");
    private final static String[] TAGS = {QUOTE_START_TAG, QUOTE_END_TAG, CODE_START_TAG, CODE_END_TAG};

    private Context mContext;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private Post mPost;
    private int mConversationId;
    private List<Post.Attachment> mAttachmentList;
    private OnViewClickListener mOnViewClickListener;

    private Boolean mShowQuote = true;

    public PostContentView(Context context) {
        super(context);
        init(context);
    }

    @SuppressWarnings("unused")
    public PostContentView(Context context, OnViewClickListener onViewClickListener) {
        super(context);
        init(context, onViewClickListener);
    }
    public PostContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }
    public PostContentView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context);
    }


    /**
     * Recursive descent
     *
     * fullContent  -> codeBlock attachment
     *
     * codeBlock    -> quote CODE_START code CODE_END codeBlock
     *                  | quote
     *
     * quote        -> table QUOTE_START codeBlock QUOTE_END quote      // due to tech limit, codeBlock here is actually LaTeX
     *                  | table
     *
     * table        -> LaTeX TABLE_START codeBlock TABLE_END table
     *                  | LaTeX
     *
     * LaTeX        -> plainText img LaTeX plainText
     *
     * @param post the post
     */
    public void setPost(Post post) {
        removeAllViews();
        mPost = post;
        mAttachmentList = post.getAttachments();
        List<Post.Attachment> attachmentList = new ArrayList<>(post.getAttachments());
        String content = post.getContent();
        content = content.replaceAll("\u00AD", "");
        fullContent(content, attachmentList);
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void fullContent(String str, List<Post.Attachment> attachmentList) {
        Matcher attachmentMatcher = ATTACHMENT_PATTERN.matcher(str);
        while (attachmentMatcher.find()) {
            String id = attachmentMatcher.group(1);
            for (int i = attachmentList.size() - 1; i >= 0; i--) {
                Post.Attachment attachment = attachmentList.get(i);
                if (attachment.getAttachmentId().equals(id)) {
                    attachmentList.remove(i);
                }
            }
        }

        codeBlock(str);

        SpannableStringBuilder builder = new SpannableStringBuilder();

        boolean isImage = false;
        for (final Post.Attachment attachment : attachmentList) {
            for (String image_ext : Constants.IMAGE_FILE_EXTENSION) {
                if (attachment.getFilename().endsWith(image_ext)) {
                    isImage = true;
                    break;
                }
            }

            if (isImage) {
                String url = MyUtils.getAttachmentImageUrl(attachment);
                final ImageView imageView = new ImageView(mContext);

                LinearLayout.LayoutParams layoutParams = new LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                imageView.setLayoutParams(layoutParams);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + url);

                Glide.with(mContext)
                        .load(url)
                        .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                imageView.setImageDrawable(resource);

                                imageView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mOnViewClickListener != null) {
                                            mOnViewClickListener.onImgClick(imageView);
                                        }
                                    }
                                });
                                return false;
                            }
                        })
                        .into(imageView);
                addView(imageView);
            } else {
                int start = builder.length();
                builder.append(attachment.getFilename());
                builder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "onClick() called with: view = [" + view + "]");
                        MyUtils.downloadAttachment(mContext, attachment);
                    }
                }, start, start + attachment.getFilename().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                builder.append("\n\n");
            }
        }

        if (builder.length() > 0) {
            TextView textView = new TextView(mContext);
            textView.setText(builder);
            /**
             * make links clickable
             */
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            addView(textView);
        }
    }

    private void codeBlock(String str) {
        int codeStartPos, codeEndPos = 0;
        String piece, code;
        while (codeEndPos != -1 && (codeStartPos = str.indexOf(CODE_START_TAG, codeEndPos)) >= 0) {//codeMatcher.find(codeEndPos)) {
            if (codeEndPos != codeStartPos) {
                piece = str.substring(codeEndPos, codeStartPos);
                quote(piece);
            }
            codeEndPos = pairedIndex(str, codeStartPos, CODE_START_TAG, CODE_END_TAG);
            //codeEndPos = content.indexOf(CODE_END_TAG, codeStartPos) + CODE_END_TAG.length();
            if (codeEndPos == -1) {
                piece = str.substring(codeStartPos);
                quote(piece);
                codeEndPos = str.length();
            } else {
                code = str.substring(codeStartPos + CODE_START_TAG.length(), codeEndPos - CODE_END_TAG.length());
                code(code);
            }
        }
        if (codeEndPos != str.length()) {
            piece = str.substring(codeEndPos);
            quote(piece);
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void quote(String str) {
        int quoteStartPos, quoteEndPos = 0;
        String piece, quote;
        Matcher quoteMatcher = QUOTE_START_PATTERN.matcher(str);
        while (quoteEndPos != -1 && quoteMatcher.find(quoteEndPos)) {
            quoteStartPos = quoteMatcher.start();

            if (quoteEndPos != quoteStartPos) {
                piece = str.substring(quoteEndPos, quoteStartPos);
                table(piece);
            }
            quoteEndPos = pairedIndex(str, quoteStartPos, QUOTE_START_TAG, QUOTE_END_TAG);

            if (quoteEndPos == -1) {
                piece = str.substring(quoteStartPos);
                table(piece);
                quoteEndPos = str.length();
            } else if (mShowQuote) {
                quote = str.substring(quoteStartPos + quoteMatcher.group().length(), quoteEndPos - QUOTE_END_TAG.length());
                addQuoteView(quote);
            } else {
                addQuoteView("...");
            }
        }

        if (quoteEndPos != str.length()) {
            piece = str.substring(quoteEndPos);
            table(piece);
        }
    }

    private void table(String str) {
        final String SPECIAL_CHAR = "\uF487";
        Pattern pattern = Pattern.compile("(?:\\n|^)( *\\|.+\\| *\\n)??( *\\|(?: *:?----*:? *\\|)+ *\\n)((?: *\\|.+\\| *(?:\\n|$))+)");
        Matcher matcher = pattern.matcher(str);
        int[] margins;
        final int LEFT = 0, RIGHT = 1, CENTER = 2;

        int startIndex = 0, endIndex;

        while (matcher.find()) {
            endIndex = matcher.start();
            if (endIndex != startIndex) {
                LaTeX2(str.substring(startIndex, endIndex));
            }
            startIndex = matcher.end();

            List<String> headers = null;
            if (!TextUtils.isEmpty(matcher.group(1))) {
                String wholeHeader = matcher.group(1);

                headers = new ArrayList<>(Arrays.asList(wholeHeader.split("\\|")));
                format(headers);
            }

            List<String> partitions = new ArrayList<>(Arrays.asList(matcher.group(2).split("\\|")));
            format(partitions);
            final int columnNum = partitions.size();
            margins = new int[columnNum];

            for (int i = 0; i < partitions.size(); i++) {
                String partition = partitions.get(i);
                if (partition.startsWith(":") && partition.endsWith(":")) {
                    margins[i] = CENTER;
                } else if (partition.startsWith(":")) {
                    margins[i] = LEFT;
                } else if (partition.endsWith(":")) {
                    margins[i] = RIGHT;
                } else {
                    margins[i] = CENTER;
                }
            }

            String[] rows = matcher.group(3).replace("\\|", SPECIAL_CHAR).split("\n");
            final List<List<String>> content = new ArrayList<>();
            for (String row : rows) {
                content.add(format(new ArrayList<>(Arrays.asList(row.split("\\|")))));
            }

            final List<String[]> whole = new ArrayList<>();
            if (headers != null) {
                whole.add(headers.toArray(new String[columnNum]));
            }
            for (List<String> strings : content) {
                whole.add(strings.toArray(new String[columnNum]));
            }

            // render table
            HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
            TableLayout tableLayout = new TableLayout(mContext);

            tableLayout.addView(getHorizontalDivider());
            for (int i = 0; i < whole.size(); i++) {
                String[] row = whole.get(i);
                TableRow tableRow = new TableRow(mContext);
                final TableLayout.LayoutParams params = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(params);

                tableRow.addView(getVerticalDivider());
                for (int j = 0; j < row.length; j++) {
                    String cell = row[j];
                    if (cell != null) {
                        cell = cell.replace(SPECIAL_CHAR, "|");
                    }
                    PostContentView postContentView = PostContentView.newInstance(getContext(), cell, mOnViewClickListener);
                    // TextView textView = new TextView(mContext);
                    // textView.setBackgroundResource((i % 2 == 0) ? R.drawable.cell_shape_black : R.drawable.code_shape_white);
                    // postContentView.setBackgroundResource(R.drawable.code_shape_white);
                    TableRow.LayoutParams pcvParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    switch (margins[j]) {
                        case CENTER:
                            pcvParams.gravity = Gravity.CENTER;
                            break;
                        case LEFT:
                            pcvParams.gravity = Gravity.START;
                            break;
                        case RIGHT:
                            pcvParams.gravity = Gravity.END;
                            break;
                    }
                    postContentView.setPadding(10, 10, 10, 10);
                    // pcvParams.setMargins(10, 10, 10, 10);
                    postContentView.setLayoutParams(pcvParams);
                    tableRow.addView(postContentView);
                    tableRow.addView(getVerticalDivider());
                }
                tableLayout.addView(tableRow);
                tableLayout.addView(getHorizontalDivider());
            }

            scrollView.addView(tableLayout);

            addView(scrollView);

            LayoutParams svParams = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
            svParams.setMargins(0, 10, 0, 10);
            scrollView.setLayoutParams(svParams);

            /* Button button = new Button(mContext);
            button.setText("click to see table");
            final List<String> finalHeaders = headers;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnViewClickListener.onTableButtonClick(finalHeaders != null ? finalHeaders.toArray(new String[columnNum]) : new String[0], whole);
                }
            });
            addView(button); */

            //TableView tableView = (TableView) LayoutInflater.from(mContext).inflate(R.layout.table_view, this, false);
            /* final TableView tableView = new TableView(mContext);
            final String[][] DATA_TO_SHOW = { { "This", "is", "a", "test" },
                    { "and", "a", "second", "test" } };
            final SimpleTableDataAdapter dataAdapter = new SimpleTableDataAdapter(mContext, whole);
            tableView.setDataAdapter(dataAdapter);
            tableView.setColumnCount(columnNum);

            Log.d(TAG, "table: " + tableView.getColumnCount());
            if (headers != null) {
                tableView.setHeaderAdapter(new SimpleTableHeaderAdapter(mContext, headers.toArray(new String[columnNum])));
            }
            addView(tableView);

            tableView.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: " + tableView.getHeight());
                    // tableView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    tableView.invalidate();
                }
            });

            //Log.d(TAG, "table: " + tableView.getColumnCount());*/
        }

        if (startIndex != str.length()) {
            LaTeX2(str.substring(startIndex));
        }
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void LaTeX2(String str) {
        Log.d(TAG, "LaTeX2: " + str);
        str = removeTags(str);
        SpannableStringBuilder builder = new SpannableStringBuilder(OnlineImgUtils.removeNewlineInFormula(str));
        builder = SFXParser3.removeTags(SFXParser3.parse(mContext, builder, mAttachmentList));
        List<OnlineImgUtils.Formula> formulaList = OnlineImgUtils.getAll(builder, mAttachmentList);
        formulaList.add(new OnlineImgUtils.Formula(builder.length(), builder.length(), "", "", OnlineImgUtils.Formula.TYPE_IMG));

        int beginIndex = 0, endIndex;
        for (int i = 0; i < formulaList.size() && beginIndex < builder.length(); i++) {
            final OnlineImgUtils.Formula formula = formulaList.get(i);
            endIndex = formula.start;
            if (formula.type == OnlineImgUtils.Formula.TYPE_ATT || formula.type == OnlineImgUtils.Formula.TYPE_IMG) {
                TextView textView = new TextView(mContext);
                final CharSequence subSequence = builder.subSequence(beginIndex, endIndex);
                final SpannableStringBuilder subBuilder = new SpannableStringBuilder(subSequence);
                textView.setText(subSequence);
                /**
                 * make links clickable
                 */
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                addView(textView);
                OnlineImgUtils.retrieveFormulaOnlineImg(OnlineImgUtils.formulasBetween(formulaList, beginIndex, endIndex), textView, subBuilder, 0, beginIndex);
                beginIndex = formula.end + 1;

                if (formula.url.equals("")) {
                    continue;
                }

                final ImageView imageView = new ImageView(mContext);

                LinearLayout.LayoutParams layoutParams;
                if (formula.size == -1) {
                    layoutParams = new LayoutParams(Constants.MAX_IMAGE_WIDTH, Constants.MAX_IMAGE_WIDTH / 2);
                } else {
                    layoutParams = new LayoutParams(formula.size, formula.size);
                }
                imageView.setLayoutParams(layoutParams);
                imageView.setAdjustViewBounds(true);
                imageView.setPadding(0, 0, 0, 10);
                Log.d(TAG, "fullContent: " + formula.url);
                Glide.with(mContext)
                        .load(formula.url)
                        .placeholder(new ColorDrawable(ContextCompat.getColor(mContext, android.R.color.darker_gray)))
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                /**
                                 * adjust the size of ImageView according to image
                                 */
                                if (formula.size == -1) {
                                    imageView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                }

                                imageView.setOnClickListener(new OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        if (mOnViewClickListener != null) {
                                            mOnViewClickListener.onImgClick(imageView);
                                        }
                                    }
                                });
                                return false;
                            }
                        })
                        .into(imageView);
                addView(imageView);
            }
        }
    }

    private List<String> format(List<String> strings) {
        for (int i = strings.size() - 1; i >= 0; i--) {
            String str = strings.get(i);
            if (TextUtils.isEmpty(str) || str.equals("\n")) {
                strings.remove(i);
            }
        }

        for (int i = 0; i < strings.size(); i++) {
            strings.set(i, strings.get(i).trim());
        }

        return strings;
    }

    /**
     * see {@link #setPost(Post)}
     */
    private void code(String str) {
        str = removeTags(str);
        CodeView codeView = (CodeView) LayoutInflater.from(mContext).inflate(R.layout.code_view, this, false);
        codeView.setCode(str);
        addView(codeView);
    }

    public static PostContentView newInstance(Context context, String string, OnViewClickListener onViewClickListener) {
        PostContentView postContentView = new PostContentView(context, onViewClickListener);

        if (!TextUtils.isEmpty(string)) {
            postContentView.codeBlock(string);
        }

        return postContentView;
    }

    private int pairedIndex(String str, int from, String startTag, String endTag) {
        int times = 0;
        for (int i = from; i < str.length(); i++) {
            if (str.substring(i).startsWith(startTag)) {
                times++;
            } else if (str.substring(i).startsWith(endTag)) {
                times--;
                if (times == 0) {
                    return i + endTag.length();
                }
            }
        }
        return -1;
    }

    private void addQuoteView(String content) {
        QuoteView quoteView = new QuoteView(mContext, mAttachmentList);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = 20;
        quoteView.setLayoutParams(params);
        quoteView.setOrientation(VERTICAL);
        quoteView.setText(content);
        addView(quoteView);
    }

    private String removeTags(String str) {
        for (String tag : TAGS) {
            str = str.replace(tag, "");
        }

        return str;
    }

    private View getHorizontalDivider() {
        View horizontalDivider = new View(mContext);
        horizontalDivider.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        horizontalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return horizontalDivider;
    }

    private View getVerticalDivider() {
        View verticalDivider = new View(mContext);
        verticalDivider.setLayoutParams(new TableRow.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        verticalDivider.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));

        return verticalDivider;
    }

    private void init(Context context) {
        init(context, null);
    }

    private void init(Context context, OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
        mContext = context;
        removeAllViews();
    }

    public void setOnImgClickListener(OnViewClickListener onViewClickListener) {
        mOnViewClickListener = onViewClickListener;
    }

    public int getConversationId() {
        return mConversationId;
    }

    public void setConversationId(int mConversationId) {
        this.mConversationId = mConversationId;
    }

    @SuppressWarnings("unused")
    public void showQuote(Boolean showQuote) {
        mShowQuote = showQuote;
    }

    public interface OnViewClickListener {
        void onImgClick(ImageView imageView);
    }

    /* private static class Formula {
        static final int TYPE_1 = 1;
        static final int TYPE_2 = 2;
        static final int TYPE_3 = 3;
        static final int TYPE_4 = 4;
        static final int TYPE_5 = 5;
        static final int TYPE_IMG = 4;
        static final int TYPE_ATT = 5;
        int start, end;
        String content, url;
        int type;

        Formula(int start, int end, String content, String url, int type) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.url = url;
            this.type = type;
        }
    } */
}


import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by jianhao on 16-6-2.
 */
public class ScrollFABBehavior extends FloatingActionButton.Behavior {
    final String TAG = "ScrollFABBehavior";
    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {

        final boolean ret = nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target,
                        nestedScrollAxes);
        return ret;
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                               View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed);

        if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
            child.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    super.onHidden(fab);
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        } else if (dyConsumed < 0 && child.getVisibility() != View.VISIBLE) {
            child.show();
        }
    }

    public ScrollFABBehavior(Context context, AttributeSet attrs) {
        super();
    }
}


public enum ConversationState
{
	normal("普通"),
	sticky("版内置顶"),
	starred("关注"),
	featured("精品"),
	draft("草稿"),
	ignored("隐藏"),
	question("未解决"),
	answered("已解决");

	private String detail;

	ConversationState(String detail)
	{
		this.detail = detail;
	}

	public String getDetail()
	{
		return detail;
	}

	@Override
	public String toString()
	{
		return this.detail;
	}
}


import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * 和OnlineImgTextView类似
 * 用于答题时显示选项中的LaTeX
 * Created by jianhao on 16-9-4.
 */
public class OnlineImgCheckBox extends CheckBox implements IOnlineImgView {
    private OnlineImgImpl mImpl;

    public OnlineImgCheckBox(Context context, @Nullable List<Post.Attachment> attachmentList)
    {
        super(context);
        init(context, attachmentList);
    }

    public OnlineImgCheckBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, null);
    }

    public OnlineImgCheckBox(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void setText(String text) {
        mImpl.setText(text);
    }

    public void setText(String text, OnlineImgImpl.OnCompleteListener listener) {
        mImpl.setListener(listener);
        mImpl.setText(text);
    }

    private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
        mImpl = new OnlineImgImpl(this);
        mImpl.mAttachmentList = attachmentList;
    }

    @Override
    public void setText(SpannableStringBuilder builder) {
        ((CheckBox) this).setText(builder);
    }
}


import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;

/**
 * 用于显示帖子中的引用
 * Created by jianhao on 16-8-26.
 */
public class QuoteView extends LinearLayout {
    OnlineImgTextView mTextView;
    Button mButton;
    Boolean mCollapsed;
    Context mContext;

    private static final String TAG = "QuoteView";
    private static int BG_COLOR ;

    public QuoteView(Context context, List<Post.Attachment> attachmentList) {
        super(context);
        init(context, attachmentList);
    }
    public QuoteView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, null);
    }
    public QuoteView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init(context, null);
    }

    /*public void setPost(final Post post) {
        mPostContentView.setPost(post);
        mTextView.setText(post.getContent().replaceAll("\\[quote(.*?)\\[/quote]", "..."));
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                if (mTextView.getLineCount() > 3) {
                    collapse();
                    //mButton.setTextSize(BUTTON_TEXT_SIZE);
                    //LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    mButton.setTextSize(10);
                    //mButton.setLayoutParams(params);
                    //mButton.setMinHeight(0);
                    mButton.setBackgroundColor(BG_COLOR);
                    mButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick() called with: " + "view = [" + view + "]");
                            if (mCollapsed) {
                                expand();
                            } else {
                                collapse();
                            }
                        }
                    });
                    addView(mButton);
                }
            }
        });
    }*/
    public void setText(String content) {
        mTextView.setText(MyUtils.formatQuote(content));
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                if (mTextView.getLineCount() > 3) {
                    collapse();
                    //mButton.setTextSize(BUTTON_TEXT_SIZE);
                    //LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    mButton.setTextSize(10);
                    //mButton.setLayoutParams(params);
                    //mButton.setMinHeight(0);
                    mButton.setBackgroundColor(BG_COLOR);
                    mButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.d(TAG, "onClick() called with: " + "view = [" + view + "]");
                            if (mCollapsed) {
                                expand();
                            } else {
                                collapse();
                            }
                        }
                    });
                    addView(mButton);
                }
            }
        });
    }

    private void collapse(){
        Log.d(TAG, "collapse() called with: " + "");
        mTextView.setMaxLines(3);
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        //mTextView.setVisibility(VISIBLE);
        //mPostContentView.setVisibility(GONE);
        mButton.setText(R.string.expand);
        mCollapsed = true;
    }

    private void expand(){
        mTextView.setMaxLines(Integer.MAX_VALUE);
        mTextView.setEllipsize(null);
        //mTextView.setVisibility(GONE);
        //mPostContentView.setVisibility(VISIBLE);
        mButton.setText(R.string.collapse);
        mCollapsed = false;
    }

    private void init(Context context, List<Post.Attachment> attachmentList) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.QuoteViewBackground,value,true);
        BG_COLOR = value.data;
        mContext = context;
        mCollapsed = false;
        mTextView = new OnlineImgTextView(context, attachmentList);
        //mButton = new Button(context);
        mButton = (Button) LinearLayout.inflate(context, R.layout.button, null);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mTextView.setLayoutParams(params);
        mTextView.setBackgroundColor(BG_COLOR);
        addView(mTextView);
        mTextView.setTextIsSelectable(true);
    }
}


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.daquexian.chaoli.forum.R;

import java.io.File;

public class AvatarView extends RelativeLayout
{
	final String TAG = "AvatarView";

	Context mContext;
	String mImagePath, mUsername;
	int mUserId;
	Boolean firstLoad = true;
	RelativeLayout v;
	TextView t;
	ImageView i;

	public AvatarView(final Context context, final String imagePath, int userId, String username)
	{
		this(context, null);
		update(imagePath, userId, username);
	}

	// adjust the size
	public void scale(int length){
		int lengthdp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, length, getResources().getDisplayMetrics());
		ViewGroup.LayoutParams layoutParams = getLayoutParams();
		layoutParams.height = lengthdp;
		layoutParams.width = lengthdp;
		t.setTextSize((float)20 * lengthdp / 80); //这个80显示效果好一些,然而我不知道为什么
	}
	public void update(String imagePath, int userId, String username) {
		setVisibility(VISIBLE);
		mImagePath = imagePath;
		mUserId = userId;
		mUsername = username;

		if (Constants.NONE.equals(imagePath) || imagePath == null)
		{
			if (username == null)
				t.setText("?");
			else
				t.setText(String.format("%s", username.toUpperCase().charAt(0)));
			i.setVisibility(INVISIBLE);
			t.setVisibility(VISIBLE);
		}
		else
		{
			Glide.with(mContext)
					.load(Constants.avatarURL + "avatar_" + userId + "." + imagePath)
                    .placeholder(new ColorDrawable(ContextCompat.getColor(mContext,android.R.color.darker_gray)))
                    .into(i);
			t.setVisibility(INVISIBLE);
			i.setVisibility(VISIBLE);
		}
	}

	/**
	 * 用于在设置页面的改变头像功能中显示新头像
	 * @param file 选择的新头像文件
     */
	public void update(File file) {
		setVisibility(VISIBLE);
		Glide.with(mContext)
                .load(file)
                .placeholder(new ColorDrawable(ContextCompat.getColor(mContext,android.R.color.darker_gray)))
                .into(i);
		t.setVisibility(INVISIBLE);
		i.setVisibility(VISIBLE);
	}

	public void init(Context context){
		setVisibility(INVISIBLE);
		v = (RelativeLayout) inflate(context, R.layout.avatar_view, this);
		t = (TextView) v.findViewById(R.id.avatarTxt);
		i = (ImageView) v.findViewById(R.id.avatarImg);
		t.setTextSize(20);
		mContext = context;
		firstLoad = false;
	}

	public void setLoginImage(Context context){
		setVisibility(VISIBLE);
		if(firstLoad) init(context);
		Glide.with(context)
                .load(R.drawable.ic_account_plus_white_48dp)
                .placeholder(new ColorDrawable(ContextCompat.getColor(context,android.R.color.darker_gray)))
                .into(i);
		t.setVisibility(INVISIBLE);
	}

	public AvatarView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public AvatarView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public String getImagePath() {
		return mImagePath;
	}

	public String getUsername() {
		return mUsername;
	}

	public int getUserId() {
		return mUserId;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public AvatarView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context);
	}
}


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

public class ChannelTextView extends TextView
{
	private Channel channel;

	private static final String TAG = "ChannelTextView";

	public ChannelTextView(Context context, Channel channel)
	{
		this(context);
		this.channel = channel;
		this.setPadding(5, 5, 5, 5);
		this.setText(channel.toString());
		this.setTextColor(channel.getColor());
	}

	public ChannelTextView(Context context)
	{
		this(context, (AttributeSet) null);
	}

	public ChannelTextView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public ChannelTextView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		Paint paint = new Paint();
		paint.setColor(this.getChannel().getColor());
		canvas.drawLine(0, 0, this.getWidth() - 1, 0, paint);
		canvas.drawLine(0, 0, 0, this.getHeight() - 1, paint);
		canvas.drawLine(this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight() - 1, paint);
		canvas.drawLine(0, this.getHeight() - 1, this.getWidth() - 1, this.getHeight() - 1, paint);
	}

	public Channel getChannel()
	{
		if (channel == null) return Channel.caff;
		return channel;
	}

	public void setChannel(Channel channel)
	{
		this.channel = channel;
		this.setPadding(5, 5, 5, 5);
		this.setText(channel.toString());
		this.setTextColor(channel.getColor());
		//invalidate();
	}
}


import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.RadioButton;

import com.daquexian.chaoli.forum.model.Post;

import java.util.List;

/**
 * 和OnlineImgTextView类似，只是继承了RadioButton
 * 用于答题时显示选项中的LaTeX
 * Created by jianhao on 16-9-4.
 */
public class OnlineImgRadioButton extends RadioButton implements IOnlineImgView {
    OnlineImgImpl mImpl;

    public OnlineImgRadioButton(Context context, @Nullable List<Post.Attachment> attachmentList)
    {
        super(context);
        init(context, attachmentList);
    }

    public OnlineImgRadioButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, null);
    }

    public OnlineImgRadioButton(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    public void setText(String text) {
        mImpl.setText(text);
    }

    public void setText(String text, OnlineImgImpl.OnCompleteListener listener) {
        mImpl.setText(text);
        mImpl.setListener(listener);
    }

    @Override
    public void setText(SpannableStringBuilder builder) {
        ((RadioButton) this).setText(builder);
    }

    private void init(Context context, @Nullable List<Post.Attachment> attachmentList) {
        mImpl = new OnlineImgImpl(this);
        mImpl.mAttachmentList = attachmentList;
    }
}


import android.content.res.Resources;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Constants
{
	public static final String APP_NAME = "chaoli";		// for shared preference
	public static final String APP_DIR_NAME = "ChaoLi";	// for directory where save attachments
	public static final int paddingLeft = 16;
	public static final int paddingTop = 16;
	public static final int paddingRight = 16;
	public static final int paddingBottom = 16;
	public static final int getNotificationInterval = 15;

	public static final int MAX_IMAGE_WIDTH = (int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.8);

	public static final String BASE_BASE_URL = "https://chaoli.club/";
	public static final String BASE_URL = BASE_BASE_URL + "index.php/";
	public static final String GET_TOKEN_URL = BASE_URL + "user/login.json";
	// index.php/member/name.json/我是大缺弦
	public static final String GET_USER_ID_URL = BASE_URL + "member/name.json/";
	public static final String LOGIN_URL = BASE_URL + "user/login";
	public static final String HOMEPAGE_URL = BASE_URL;
	public static final String LOGOUT_PRE_URL = BASE_URL + "user/logout?token=";
	public static final String GET_CAPTCHA_URL = BASE_URL + "mscaptcha";
	public static final String SIGN_UP_URL = BASE_URL + "user/join?invite=";
	public static final String GET_ALL_NOTIFICATIONS_URL = BASE_URL + "settings/notifications.json";
	// activity.json/32/2
	public static final String GET_ACTIVITIES_URL = BASE_URL + "member/activity.json/";
	// statistics.ajax/32
	public static final String GET_STATISTICS_URL = BASE_URL + "member/statistics.ajax/";
	// index.json/channelName?searchDetail
	// index.json/all?search=%23%E4%B8%8A%E9%99%90%EF%BC%9A0%20~%20100
	// index.json/chem?search=%23%E7%B2%BE%E5%93%81
	//public static final String conversationListURL = BASE_URL + "/conversations/index.json/";
	public static final String conversationListURL = BASE_URL + "conversations/index.json/";
	public static final String ATTACHMENT_IMAGE_URL = "https://dn-chaoli-upload.qbox.me/";
	// index.json/1430/p2
	public static final String postListURL = BASE_URL + "conversation/index.json/";
	public static final String loginURL = BASE_URL + "user/login";
	public static final String replyURL = BASE_URL + "?p=conversation/reply.ajax/";
	public static final String editURL = BASE_URL + "?p=conversation/editPost.ajax/";
	public static final String cancelEditURL = BASE_URL + "?p=attachment/removeSession/";
	public static final String notifyNewMsgURL = BASE_URL + "settings/notificationCheck/";
	public static final String avatarURL = "https://dn-chaoli-upload.qbox.me/";
	// .ajax/<conversationId>/<floor>&userId=<myId>&token=<token>
	public static final String preQuoteURL = BASE_URL + "?p=conversation/quotePost.json/";
	// .json/<postId>&userId=<myId>&token=<token>
	public static final String quoteURL = BASE_URL + "?p=conversation/reply.ajax/";
	public static final String deleteURL = BASE_URL + "?p=conversation/deletePost.ajax/";
	public static final String restoreURL = BASE_URL + "conversation/restorePost/";

	public static final String GET_QUESTION_URL = "https://chaoli.club/reg-exam/get-q.php?tags=";
	public static final String CONFIRM_ANSWER_URL = "https://chaoli.club/reg-exam/confirm.php";

	public static final String GET_PROFILE_URL = BASE_URL + "settings/general.json";
	public static final String CHECK_NOTIFICATION_URL = BASE_URL + "?p=settings/notificationCheck.ajax";
	public static final String UPDATE_URL = BASE_URL + "?p=conversations/update.ajax/all/";
	public static final String MODIFY_SETTINGS_URL = BASE_URL + "settings/general";

	public static final String GO_TO_POST_URL = BASE_URL + "conversation/post/";

	/* 给主题设置版块 */
	public static final String SET_CHANNEL_URL = BASE_URL + "?p=conversation/save.json/";
	/* 发表主题 */
	public static final String POST_CONVERSATION_URL = BASE_URL + "?p=conversation/start.ajax";
	/* 添加可见用户 */
	public static final String ADD_MEMBER_URL = BASE_URL + "?p=conversation/addMember.ajax/";
	/* 取消可见用户 */
	public static final String REMOVE_MEMBER_URL = BASE_URL + "?p=conversation/removeMember.ajax/";
	/* 获取可见用户列表 */
	public static final String GET_MEMBERS_ALLOWED_URL = BASE_URL + "";
	/* 隐藏主题 */
	public static final String IGNORE_CONVERSATION_URL = BASE_URL + "?p=conversation/ignore.ajax/";
	/* 关注主题 */
	public static final String STAR_CONVERSATION_URL = BASE_URL + "?p=conversation/star.json/";

	public static final String SETTINGS_SP = "settings";
	public static final String INVITING_CODE_SP = "icsp";
	public static final String NIGHT_MODE = "nightMode";
	public static final String CLICK_TWICE_TO_EXIT = "ctte";

	public static final String GLOBAL = "global";
	public static final String FIRST_ENTER_DEMO_MODE = "fedm";

	public static final String conversationSP = "conversationList";
	public static final String conversationSPKey = "listJSON";

	public static final String postSP = "postList";
	public static final String postSPKey = "listJSON";

	public static final String loginSP = "loginReturn";
	public static final String loginSPKey = "listJSON";
	public static final String loginBool = "logged";

	public static final String[] IMAGE_FILE_EXTENSION = {".jpg", ".png", ".gif", ".jpeg"};

	public static final String NONE = "none";

	public static final float MIN_EXPRESSION_ALPHA = 0.4f;

	public static final int POST_PER_PAGE = 20;

	public static final String[] iconStrs = new String[]{ " /:) " ,  " /:D " ,  " /^b^ " ,  " /o.o " ,  " /xx " ,  " /# " ,  " /)) " ,  " /-- " ,  " /TT " ,  " /== " ,
			" /.** " ,  " /:( " ,  " /vv " ,  " /$$ " ,  " /?? " ,  " /:/ " ,  " /xo " ,  " /o0 " ,  " />< " ,  " /love " ,
			" /... " ,  " /XD " ,  " /ii " ,  " /^^ " ,  " /<< " ,  " />. " ,  " /-_- " ,  " /0o0 " ,  " /zz " ,  " /O!O " ,
			" /## " ,  " /:O " ,  " /< " ,  " /heart " ,  " /break " ,  " /rose " ,  " /gift " ,  " /bow " ,  " /moon " ,  " /sun " ,
			" /coin " ,  " /bulb " ,  " /tea " ,  " /cake " ,  " /music " ,  " /rock " ,  " /v " ,  " /good " ,  " /bad " ,  " /ok " ,
			" /asnowwolf-smile " ,  " /asnowwolf-laugh " ,  " /asnowwolf-upset " ,  " /asnowwolf-tear " ,
			" /asnowwolf-worry " ,  " /asnowwolf-shock " ,  " /asnowwolf-amuse " };
	public static final String[] icons = new String[]{"1", "2", "3", "4",
			"5", "6", "7", "8", "9",
			"10", "11", "12", "13", "14",
			"15", "16", "17", "18", "19",
			"20", "21", "22", "23", "24",
			"25", "26", "27", "28", "29",
			"30", "31", "32", "33", "34",
			"35", "36", "37", "38", "39",
			"40", "41", "42", "43", "44",
			"45", "46", "47", "48", "49",
			"50", "asonwwolf_smile", "asonwwolf_laugh", "asonwwolf_upset", "asonwwolf_tear",
			"asonwwolf_worry", "asonwwolf_shock", "asonwwolf_amuse"};
}


import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Parcel;
import android.os.Parcelable;

import com.daquexian.chaoli.forum.BR;
import com.google.gson.annotations.SerializedName;

/**
 * Created by daquexian on 16-4-8.
 * 保存用户账户信息的类
 */
public class User extends BaseObservable implements Parcelable {
    private static final String TAG = "User";
    private boolean isEmpty = true;
    private int userId;
    private String username;

    @SerializedName("avatarFormat")
    private String avatarSuffix;
    private String status;
    private Preferences preferences;
    private static User user;

    public User(){}

    private User(Parcel in){
        userId = in.readInt();
        username = in.readString();
        avatarSuffix = in.readString();
        status = in.readString();
        preferences.privateAdd = (in.readByte() == 1);
        preferences.starOnReply = (in.readByte() == 1);
        preferences.starPrivate = (in.readByte() == 1);
        preferences.hideOnline = (in.readByte() == 1);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(userId);
        dest.writeString(username);
        dest.writeString(avatarSuffix);
        dest.writeString(status);
        dest.writeString(preferences.signature);
        dest.writeByte((byte)(preferences.privateAdd ? 1 : 0));
        dest.writeByte((byte)(preferences.starOnReply ? 1 : 0));
        dest.writeByte((byte)(preferences.starPrivate ? 1 : 0));
        dest.writeByte((byte)(preferences.hideOnline ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static final Parcelable.Creator<User> CREATOR
            = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };


    public class Preferences extends BaseObservable {
        private String signature;
        @SerializedName("email.privateAdd")
        private Boolean privateAdd;
        private Boolean starOnReply;
        private Boolean starPrivate;
        private Boolean hideOnline;

        @Bindable
        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
            notifyPropertyChanged(BR.signature);
        }

        @Bindable
        public Boolean getPrivateAdd() {
            return privateAdd;
        }

        public void setPrivateAdd(Boolean privateAdd) {
            this.privateAdd = privateAdd;
            notifyPropertyChanged(BR.privateAdd);
        }

        @Bindable
        public Boolean getStarOnReply() {
            return starOnReply;
        }

        public void setStarOnReply(Boolean starOnReply) {
            this.starOnReply = starOnReply;
            notifyPropertyChanged(BR.starOnReply);
        }

        @Bindable
        public Boolean getStarPrivate() {
            return starPrivate;
        }

        public void setStarPrivate(Boolean starPrivate) {
            this.starPrivate = starPrivate;
            notifyPropertyChanged(BR.starPrivate);
        }

        @Bindable
        public Boolean getHideOnline() {
            return hideOnline;
        }

        public void setHideOnline(Boolean hideOnline) {
            this.hideOnline = hideOnline;
            notifyPropertyChanged(BR.hideOnline);
        }
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }

    @Bindable
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        notifyPropertyChanged(BR.userId);
    }

    @Bindable
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        notifyPropertyChanged(BR.username);
    }

    @Bindable
    public String getAvatarSuffix() {
        return avatarSuffix;
    }

    public void setAvatarSuffix(String avatarSuffix) {
        this.avatarSuffix = avatarSuffix;
        notifyPropertyChanged(BR.avatarSuffix);
    }

    @Bindable
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        notifyPropertyChanged(BR.status);
    }

    @Bindable
    public Preferences getPreferences() {
        return preferences;
    }

    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
        notifyPropertyChanged(BR.preferences);
    }
}


import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.PropertyChangeRegistry;
import android.os.Parcel;
import android.os.Parcelable;

import com.daquexian.chaoli.forum.BR;
import com.daquexian.chaoli.forum.binding.DiffItem;
import com.google.gson.annotations.SerializedName;

public class Conversation extends BaseObservable implements DiffItem, Comparable<Conversation>, Parcelable {
	private static final String TAG = "Conversation";
	private int conversationId;
	private int channelId;
	private String title;
	private String firstPost;
	private String link;
	private String startMemberId;
	private String lastPostMemberId;
	private String startMember;
	@SerializedName("startMemberAvatarFormat")
	private String startMemberAvatarSuffix;
	private String startTime;
	@SerializedName("lastPostMemberAvatarFormat")
	private String lastPostMemberAvatarSuffix;
	private String lastPostMember;
	private String lastPostTime;
	private String unread;
	private int replies;
	private transient PropertyChangeRegistry propertyChangeRegistry = new PropertyChangeRegistry();


	@Override
	public boolean areContentsTheSame(DiffItem anotherItem) {
		Conversation newConversation = (Conversation) anotherItem;
		return !(this.getFirstPost() == null && newConversation.getFirstPost() != null)
				&& !(this.getFirstPost() != null && newConversation.getFirstPost() == null)
				&& ((this.getFirstPost() == null && newConversation.getFirstPost() == null) || this.getFirstPost().equals(newConversation.getFirstPost()))
				&& this.getReplies() == newConversation.getReplies();
	}

	@Override
	public boolean areItemsTheSame(DiffItem anotherItem) {
		Conversation newConversation = (Conversation) anotherItem;
		return this.getConversationId() == newConversation.getConversationId();
	}

	/**
	 * 对于主题帖来说，最新的排在最前，不同于单个主题帖中的楼层，最先发表的排在最前
	 *
	 * @param o 另一个主题帖
	 * @return 比较结果
	 */
	@Override
	public int compareTo(Conversation o) {
		return o.getLastPostTime().compareTo(getLastPostTime());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.conversationId);
		dest.writeInt(this.channelId);
		dest.writeString(this.title);
		dest.writeString(this.firstPost);
		dest.writeString(this.link);
		dest.writeString(this.startMemberId);
		dest.writeString(this.lastPostMemberId);
		dest.writeString(this.startMember);
		dest.writeString(this.startMemberAvatarSuffix);
		dest.writeString(this.lastPostMemberAvatarSuffix);
		dest.writeString(this.lastPostMember);
		dest.writeString(this.lastPostTime);
		dest.writeInt(this.replies);
		dest.writeString(this.startTime);
		dest.writeString(this.unread);
	}

	public Conversation() {
		replies = -1;
	}

	protected Conversation(Parcel in) {
		this.conversationId = in.readInt();
		this.channelId = in.readInt();
		this.title = in.readString();
		this.firstPost = in.readString();
		this.link = in.readString();
		this.startMemberId = in.readString();
		this.lastPostMemberId = in.readString();
		this.startMember = in.readString();
		this.startMemberAvatarSuffix = in.readString();
		this.lastPostMemberAvatarSuffix = in.readString();
		this.lastPostMember = in.readString();
		this.lastPostTime = in.readString();
		this.replies = in.readInt();
		this.startTime = in.readString();
		this.unread = in.readString();
	}

	public static final Creator<Conversation> CREATOR = new Creator<Conversation>() {
		@Override
		public Conversation createFromParcel(Parcel source) {
			return new Conversation(source);
		}

		@Override
		public Conversation[] newArray(int size) {
			return new Conversation[size];
		}
	};

	@Bindable
	public int getConversationId() {
		return conversationId;
	}

	public void setConversationId(int conversationId) {
		this.conversationId = conversationId;
		notifyChange(BR.conversationId);
	}

	@Bindable
	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
		notifyChange(BR.channelId);
	}

	@Bindable
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
		notifyChange(BR.title);
	}

	@Bindable
	public String getFirstPost() {
		return firstPost;
	}

	public void setFirstPost(String firstPost) {
		this.firstPost = firstPost;
		notifyChange(BR.firstPost);
	}

	@Bindable
	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
		notifyChange(BR.link);
	}

	@Bindable
	public String getStartMemberId() {
		return startMemberId;
	}

	public void setStartMemberId(String startMemberId) {
		this.startMemberId = startMemberId;
		notifyChange(BR.startMemberId);
	}

	@Bindable
	public String getLastPostMemberId() {
		return lastPostMemberId;
	}

	public void setLastPostMemberId(String lastPostMemberId) {
		this.lastPostMemberId = lastPostMemberId;
		notifyChange(BR.lastPostMemberId);
	}

	@Bindable
	public String getStartMember() {
		return startMember;
	}

	public void setStartMember(String startMember) {
		this.startMember = startMember;
		notifyChange(BR.startMember);
	}

	@Bindable
	public String getStartMemberAvatarSuffix() {
		return startMemberAvatarSuffix;
	}

	public void setStartMemberAvatarSuffix(String startMemberAvatarSuffix) {
		this.startMemberAvatarSuffix = startMemberAvatarSuffix;
		notifyChange(BR.startMemberAvatarSuffix);
	}

	@Bindable
	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
		notifyChange(BR.startTime);
	}

	@Bindable
	public String getLastPostMemberAvatarSuffix() {
		return lastPostMemberAvatarSuffix;
	}

	public void setLastPostMemberAvatarSuffix(String lastPostMemberAvatarSuffix) {
		this.lastPostMemberAvatarSuffix = lastPostMemberAvatarSuffix;
		notifyChange(BR.lastPostMemberAvatarSuffix);
	}

	@Bindable
	public String getLastPostMember() {
		return lastPostMember;
	}

	public void setLastPostMember(String lastPostMember) {
		this.lastPostMember = lastPostMember;
		notifyChange(BR.lastPostMember);
	}

	@Bindable
	public String getLastPostTime() {
		return lastPostTime;
	}

	public void setLastPostTime(String lastPostTime) {
		this.lastPostTime = lastPostTime;
		notifyChange(BR.lastPostTime);
	}

	@Bindable
	public String getUnread() {
		return unread;
	}

	public void setUnread(String unread) {
		this.unread = unread;
		notifyChange(BR.unread);
	}

	@Bindable
	public int getReplies() {
		return replies;
	}

	public void setReplies(int replies) {
		this.replies = replies;
		notifyChange(BR.replies);
	}

	private void notifyChange(int propertyId) {
		if (propertyChangeRegistry == null) {
			propertyChangeRegistry = new PropertyChangeRegistry();
		}
		propertyChangeRegistry.notifyChange(this, propertyId);
	}

	@Override
	public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
		if (propertyChangeRegistry == null) {
			propertyChangeRegistry = new PropertyChangeRegistry();
		}
		propertyChangeRegistry.add(callback);

	}

	@Override
	public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
		if (propertyChangeRegistry != null) {
			propertyChangeRegistry.remove(callback);
		}
	}
}


import java.util.List;

/**
 * Created by jianhao on 16-8-25.
 */
public class PostListResult {
    Conversation conversation;
    List<Post> posts;

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }
}


/**
 * Created by jianhao on 16-10-2.
 */

public interface IExpanded {
    /**
     * 比较两个IExpanded元素，A大于B时返回大于0的整数，等于时返回0
     * @param B 比较的对象
     * @return  表示大小关系的整数
     */
    int compareTo(IExpanded B);
}


import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.google.gson.annotations.SerializedName;

/**
 * 存储个人主页中的历史活动的类
 * Created by jianhao on 16-9-3.
 */
public class HistoryItem extends HistoryFragmentVM.ListItem {
    public static final String POST_ACTIVITY  = "postActivity";
    public static final String STATUS         = "status";
    public static final String JOIN           = "join";

    String start;
    String postId;
    @SerializedName("avatarFormat")
    String avatarSuffix;
    int fromMemberId;
    String fromMemberName;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    String content;
    String title;
    String description;
    Data data;

    public static class Data{
        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public String getNewSignature() {
            return newSignature;
        }

        public void setNewSignature(String newSignature) {
            this.newSignature = newSignature;
        }

        String newStatus;
        String newSignature;
    }

    @Override
    public String getAvatarUsername() {
        return fromMemberName;
    }

    @Override
    public int getConversationId() {
        return 0;
    }

    @Override
    public int getAvatarUserId() {
        return fromMemberId;
    }

    @Override
    public String getAvatarSuffix() {
        return avatarSuffix;
    }

    @Override
    public int getShowingPostId() {
        return getPostId() != null ? Integer.valueOf(getPostId()) : 0;
    }

    @Override
    public String getShowingTitle() {
        switch (getType()) {
            case POST_ACTIVITY:
                if ("1".equals(getStart())) return ChaoliApplication.getAppContext().getString(R.string.opened_a_conversation);
                else return ChaoliApplication.getAppContext().getString(R.string.updated, getTitle());
            case STATUS:
                return ChaoliApplication.getAppContext().getString(R.string.modified_his_or_her_information);
            case JOIN:
                return ChaoliApplication.getAppContext().getString(R.string.join_the_forum);
        }
        return "";
    }

    @Override
    public String getShowingContent() {
        switch (getType()) {
            case POST_ACTIVITY:
                return "1".equals(getStart()) ? getTitle() : getContent();
            case STATUS:
                if (data != null && data.getNewStatus() != null) return data.getNewStatus();
                return "";
            case JOIN:
                return "";
        }
        return "";
    }
}


import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.support.annotation.Nullable;

import com.daquexian.chaoli.forum.BR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Post extends BaseObservable implements Comparable<Post>
{
	public int postId;
	public int conversationId;
	public int memberId;
	public long time;
	public int editMemberId;
	public long editTime;
	public int deleteMemberId;
	public long deleteTime;
	public String title;
	public String content;
	public int floor;
	public String username;
	public String avatarFormat;
	public String signature;
	public List<Attachment> attachments = new ArrayList<>();

	public Post(){}
	public Post(int memberId, String username, String avatarSuffix, String content, String time) {
		this.memberId = memberId;
		this.username = username;
		this.avatarFormat = avatarSuffix;
		this.content = content;
		this.time = Long.parseLong(time);
		this.floor = 1;
	}

	public Post(int postId, int conversationId,
				int memberId, long time,
				int editMemberId, long editTime,
				int deleteMemberId, long deleteTime,
				String title, String content,
				int floor,
				String username, String avatarFormat,
				@Nullable Map<Integer, String> groups, @Nullable String groupNames,
				@Nullable String signature,
				@Nullable List<Attachment> attachments)
	{
		this.postId = postId;
		notifyPropertyChanged(BR.postId);
		this.conversationId = conversationId;
		notifyPropertyChanged(BR.conversationId);
		this.memberId = memberId;
		notifyPropertyChanged(BR.memberId);
		this.time = time;
		notifyPropertyChanged(BR.time);
		this.editMemberId = editMemberId;
		notifyPropertyChanged(BR.editMemberId);
		this.editTime = editTime;
		notifyPropertyChanged(BR.editTime);
		this.deleteMemberId = deleteMemberId;
		notifyPropertyChanged(BR.deleteMemberId);
		this.deleteTime = deleteTime;
		notifyPropertyChanged(BR.deleteTime);
		this.title = title;
		notifyPropertyChanged(BR.title);
		this.content = content;
		notifyPropertyChanged(BR.content);
		this.floor = floor;
		notifyPropertyChanged(BR.floor);
		this.username = username;
		notifyPropertyChanged(BR.username);
		this.avatarFormat = avatarFormat;
		notifyPropertyChanged(BR.avatarFormat);
		this.attachments = attachments;
		notifyPropertyChanged(BR.attachments);
	}

	@Bindable
	public int getPostId()
	{
		return postId;
	}

	public void setPostId(int postId)
	{
		this.postId = postId;
		notifyPropertyChanged(BR.postId);
	}

	@Bindable
	public int getConversationId()
	{
		return conversationId;
	}

	public void setConversationId(int conversationId)
	{
		this.conversationId = conversationId;
		notifyPropertyChanged(BR.conversationId);
	}

	@Bindable
	public int getMemberId()
	{
		return memberId;
	}

	public void setMemberId(int memberId)
	{
		this.memberId = memberId;
		notifyPropertyChanged(BR.memberId);
	}

	@Bindable
	public long getTime()
	{
		return time;
	}

	public void setTime(long time)
	{
		this.time = time;
		notifyPropertyChanged(BR.time);
	}

	@Bindable
	public int getEditMemberId()
	{
		return editMemberId;
	}

	public void setEditMemberId(int editMemberId)
	{
		this.editMemberId = editMemberId;
		notifyPropertyChanged(BR.editMemberId);
	}

	@Bindable
	public long getEditTime()
	{
		return editTime;
	}

	public void setEditTime(long editTime)
	{
		this.editTime = editTime;
		notifyPropertyChanged(BR.editTime);
	}

	@Bindable
	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
		notifyPropertyChanged(BR.signature);
	}

	@Bindable
	public int getDeleteMemberId()
	{
		return deleteMemberId;
	}

	public void setDeleteMemberId(int deleteMemberId)
	{
		this.deleteMemberId = deleteMemberId;
		notifyPropertyChanged(BR.deleteMemberId);
	}

	@Bindable
	public long getDeleteTime()
	{
		return deleteTime;
	}

	public void setDeleteTime(long deleteTime)
	{
		this.deleteTime = deleteTime;
		notifyPropertyChanged(BR.deleteTime);
	}

	@Bindable
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
		notifyPropertyChanged(BR.title);
	}

	@Bindable
	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
		notifyPropertyChanged(BR.content);
	}

	@Bindable
	public int getFloor()
	{
		return floor;
	}

	public void setFloor(int floor)
	{
		this.floor = floor;
		notifyPropertyChanged(BR.floor);
	}

	@Bindable
	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
		notifyPropertyChanged(BR.username);
	}

	@Bindable
	public String getAvatarFormat()
	{
		return avatarFormat;
	}

	public void setAvatarFormat(String avatarFormat)
	{
		this.avatarFormat = avatarFormat;
		notifyPropertyChanged(BR.avatarFormat);
	}

	@Bindable
	public List<Attachment> getAttachments()
	{
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments)
	{
		this.attachments = attachments;
		notifyPropertyChanged(BR.attachments);
	}

	/*public void setAvatarView(AvatarView avatarView)
	{
		this.avatarView = avatarView;
notifyPropertyChanged(BR.avatarView);
	}

	public void setAvatarView()
	{
		this.avatarView = new AvatarView(context, avatarFormat, memberId, username);
notifyPropertyChanged(BR.new AvatarView(context, avatarFormat, memberId, username));
	}*/

	/*public AvatarView getAvatarView()
	{
		return avatarView;
	}*/

	public class Attachment extends BaseObservable
	{
		public String attachmentId;
		public String filename;
		public String secret;
		public int postId;
		public int draftMemberId;
		public int draftConversationId;

		public Attachment()
		{
			this("", "", "", 0);
		}

		public Attachment(String attachmentId, String filename, String secret, int postId)
		{
			this(attachmentId, filename, secret, postId, 0, 0);
		}

		public Attachment(String attachmentId, String filename, String secret, int postId,
						  int draftMemberId, int draftConversationId)
		{
			this.attachmentId = attachmentId;
			notifyPropertyChanged(BR.attachmentId);
			this.filename = filename;
			notifyPropertyChanged(BR.filename);
			this.secret = secret;
			notifyPropertyChanged(BR.secret);
			this.postId = postId;
			notifyPropertyChanged(BR.postId);
			this.draftMemberId = draftMemberId;
			notifyPropertyChanged(BR.draftMemberId);
			this.draftConversationId = draftConversationId;
			notifyPropertyChanged(BR.draftConversationId);
		}

		@Bindable
		public String getAttachmentId()
		{
			return attachmentId;
		}

		public void setAttachmentId(String attachmentId)
		{
			this.attachmentId = attachmentId;
			notifyPropertyChanged(BR.attachmentId);
		}

		@Bindable
		public String getFilename()
		{
			return filename.toLowerCase();
		}

		public void setFilename(String filename)
		{
			this.filename = filename;
			notifyPropertyChanged(BR.filename);
		}

		@Bindable
		public String getSecret()
		{
			return secret;
		}

		public void setSecret(String secret)
		{
			this.secret = secret;
			notifyPropertyChanged(BR.secret);
		}

		@Bindable
		public int getPostId()
		{
			return postId;
		}

		public void setPostId(int postId)
		{
			this.postId = postId;
			notifyPropertyChanged(BR.postId);
		}

		@Bindable
		public int getDraftMemberId()
		{
			return draftMemberId;
		}

		public void setDraftMemberId(int draftMemberId)
		{
			this.draftMemberId = draftMemberId;
			notifyPropertyChanged(BR.draftMemberId);
		}

		@Bindable
		public int getDraftConversationId()
		{
			return draftConversationId;
		}

		public void setDraftConversationId(int draftConversationId)
		{
			this.draftConversationId = draftConversationId;
			notifyPropertyChanged(BR.draftConversationId);
		}
	}

	@Override
	public int compareTo(Post post) {
        if (this.getTime() < post.getTime()) return -1;
		if (this.getTime() == post.getTime()) return 0;
		else return 1;
	}
}


/**
 * Java Bean for GSON
 * Created by daquexian on 17-1-18.
 */

public class TokenResult {
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.databinding.ObservableList;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-10-13.
 */

public class BusinessQuestion {
    private static final String TAG = "BusinessQuestion";
    public String id;
    public String question;
    public Boolean choice;
    public Boolean multiAnswer;
    public ObservableList<String> options;

    public ObservableList<Boolean> isChecked = new ObservableArrayList<>();
    public ObservableField<String> answer = new ObservableField<>();

    public BusinessQuestion(Question item) {
        for (int i = 0; i < 4; i++) isChecked.add(false);
        id = item._id.$id;
        question = item.question;
        choice = Boolean.valueOf(item.choice);
        multiAnswer = Boolean.valueOf(item.multi_answer);
        options = new ObservableArrayList<>();
        options.addAll(item.options);
        while (options.size() < 4) {
            options.add(ChaoliApplication.getAppContext().getString(R.string.useless_option));
        }
    }

    public static ArrayList<BusinessQuestion> fromList(List<Question> questionList) {
        ArrayList<BusinessQuestion> businessQuestionList = new ArrayList<>();
        for (Question item : questionList) {
            businessQuestionList.add(new BusinessQuestion(item));
        }
        return businessQuestionList;
    }
}


import java.util.List;

/**
 * Created by jianhao on 16-9-3.
 */
public class HistoryResult {
    public List<HistoryItem> activity;

    public List<HistoryItem> getActivity() {
        return activity;
    }

    public void setActivity(List<HistoryItem> activity) {
        this.activity = activity;
    }
}


import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;

import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * 对JSON Model的数据进一步处理，专用于显示
 * Created by jianhao on 16-10-3.
 */

public class BusinessHomepageListItem implements Comparable<BusinessHomepageListItem> {
    private final static String TAG = "BusinessHomepageLI";

    public final static String ITEM         = "item";
    public final static String DIVIDER      = "divider";
    public final static String SPACE        = "space";

    public ObservableInt avatarUserId = new ObservableInt();
    public ObservableField<String> avatarUsername = new ObservableField<>();
    public ObservableField<String> avatarSuffix   = new ObservableField<>();
    public ObservableField<String> title          = new ObservableField<>();
    public ObservableField<String> content = new ObservableField<>();
    public ObservableInt postId = new ObservableInt();
    public ObservableInt conversationId = new ObservableInt();
    public ObservableBoolean isFirst = new ObservableBoolean(false);               // 用于决定是否显示分割线（第一个divider不显示分割线） // TODO: 16-10-4
    public String time;
    public String type;

    /**
     * 变换整个List，练习一下RxJava :)
     * @param items List<HistoryFragmentVM.ListItem>
     * @return List<BusinessHomepageListItem>
     */
    public static List<BusinessHomepageListItem> parseList(List<? extends HistoryFragmentVM.ListItem> items) {
        final List<BusinessHomepageListItem> res = new ArrayList<>();
        Observable.from(items)
                .map(new Func1<HistoryFragmentVM.ListItem, BusinessHomepageListItem>() {
                    @Override
                    public BusinessHomepageListItem call(HistoryFragmentVM.ListItem item) {
                        return new BusinessHomepageListItem(item);
                    }
                })
                .subscribe(new Action1<BusinessHomepageListItem>() {
                    @Override
                    public void call(BusinessHomepageListItem businessHomepageListItem) {
                        res.add(businessHomepageListItem);
                    }
                });
        return res;
    }

    public BusinessHomepageListItem(HistoryFragmentVM.ListItem listItem) {
        avatarUserId.set(listItem.getAvatarUserId());
        avatarUsername.set(listItem.getAvatarUsername());
        avatarSuffix.set(listItem.getAvatarSuffix());
        title.set(listItem.getShowingTitle());
        content.set(listItem.getShowingContent());
        postId.set(listItem.getShowingPostId());
        conversationId.set(listItem.getConversationId());
        time = listItem.getTime();
        type = listItem.getType();
    }

    /**
     * 定义全序关系 a > b 表示 ”a排在b后面"，哈哈哈
     * @param o 比较的对象
     * @return 比较的结果
     */
    @Override
    public int compareTo(BusinessHomepageListItem o) {
        if (Long.valueOf(time) < Long.valueOf(o.time)) {
            return 1;
        } else if (Long.valueOf(time).equals(Long.valueOf(o.time))){
            return 0;
        } else {
            return -1;
        }
    }

    public String getType() {
        return type;
    }

    public String getTime() {
        return time;
    }
    /**
     * 将HistoryItem转化为供显示的HomepageListItem
     * @param historyItem 要转化的historyItem
     */
    /*
    public BusinessHomepageListItem(HistoryItem historyItem) {
        switch (historyItem.getType()) {
            case HistoryItem.POST_ACTIVITY:
                break;
            case HistoryItem.STATUS:
                title.set(ChaoliApplication.getAppContext().getString(R.string.modified_his_or_her_information));
                content.set("");
                //holder.description_tv.setText(R.string.modified_his_or_her_information);
                //holder.content_tv.setText("");
                HistoryItem.Data data = historyItem.getData();
                if (data != null && data.getNewStatus() != null) {
                    //holder.content_tv.setText(data.getNewStatus());
                    content.set(data.getNewStatus());
                }
                break;
            case HistoryItem.JOIN:
                title.set(ChaoliApplication.getAppContext().getString(R.string.join_the_forum));
                content.set("");
                //holder.description_tv.setText(R.string.join_the_forum);
                //holder.content_tv.setText("");
                break;
            case DIVIDER:
                //if(position == 1)
                    //holder.divider.setVisibility(View.INVISIBLE);
                // FIXME: 16-10-3
        }
    }

    /**
     * 将NotificationItem转化为HomepageListItem
     * @param notificationItem 要转化的NotificationItem
     */
    /*public BusinessHomepageListItem(NotificationItem notificationItem) {
        switch (notificationItem.getType()) {
            case NotificationItem.DIVIDER:
                //// FIXME: 16-10-3
                //if(position == 1)
                 //   holder.divider.setVisibility(View.INVISIBLE);
                int timeDiff = Integer.parseInt(notificationItem.getTime());
                if(timeDiff == 0){
                    content.set(ChaoliApplication.getAppContext().getString(R.string.today));
                } else {
                    content.set(ChaoliApplication.getAppContext().getString(R.string.days_ago, timeDiff));
                }
                break;
            case NotificationItem.SPACE:

                break;
            default:
                content.set(notificationItem.getData().getTitle());
                postId.set(Integer.parseInt(notificationItem.getData().getPostId()));
                conversationId.set(Integer.parseInt(notificationItem.getData().getConversationId()));
                //holder.content_tv.setText(thisItem.getData().getTitle());
                //holder.description_tv.setText(getString(R.string.mention_you, thisItem.getFromMemberName()));
                //holder.content_tv.setHint(thisItem.getData().getShowingPostId());
                //holder.description_tv.setHint(thisItem.getData().getConversationId());
        }
    }*/
}


import java.util.List;

/**
 * Created by jianhao on 16-9-3.
 */
public class NotificationList {
    public int count;
    public List<Notification> results;
}


import java.util.List;

/**
 * Created by jianhao on 16-10-3.
 */

public class NotificationResult {
    public List<NotificationItem> getResults() {
        return results;
    }

    public void setResults(List<NotificationItem> results) {
        this.results = results;
    }

    List<NotificationItem> results;
}


import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.google.gson.annotations.SerializedName;

/**
 * Created by jianhao on 16-9-3.
 */
public class NotificationItem extends HistoryFragmentVM.ListItem {
    public static final String TYPE_MENTION        = "mention";
    public static final String TYPE_POST           = "post";
    public static final String TYPE_PRIVATE_ADD    = "privateAdd";

    String fromMemberId;
    String fromMemberName;

    public String getFromMemberId() {
        return fromMemberId;
    }

    public void setFromMemberId(String fromMemberId) {
        this.fromMemberId = fromMemberId;
    }

    public String getFromMemberName() {
        return fromMemberName;
    }

    public void setFromMemberName(String fromMemberName) {
        this.fromMemberName = fromMemberName;
    }

    public Boolean getUnread() {
        return unread;
    }

    public void setUnread(Boolean unread) {
        this.unread = unread;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAvatarSuffix() {
        return avatarSuffix;
    }

    public void setAvatarSuffix(String avatarSuffix) {
        this.avatarSuffix = avatarSuffix;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    Boolean unread;
    String content;
    @SerializedName("avatarFormat")
    String avatarSuffix;
    Data data;

    public static class Data{
        String conversationId;
        String postId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getPostId() {
            return postId;
        }

        public void setPostId(String postId) {
            this.postId = postId;
        }

        String title;
    }

    @Override
    public int getShowingPostId() {
        return getData().getPostId() != null ? Integer.parseInt(getData().getPostId()) : -1;
    }

    @Override
    public int getAvatarUserId() {
        return Integer.parseInt(getFromMemberId());
    }

    @Override
    public String getShowingTitle() {
        switch (getType()) {
            case NotificationItem.TYPE_MENTION:
                //title.set(ChaoliApplication.getAppContext().getString(R.string.mention_you, notificationItem.getFromMemberName()));
                return ChaoliApplication.getAppContext().getString(R.string.mention_you, getFromMemberName());
            case NotificationItem.TYPE_POST:
                //title.set(ChaoliApplication.getAppContext().getString(R.string.updated, notificationItem.getFromMemberName()));
                return ChaoliApplication.getAppContext().getString(R.string.updated, getFromMemberName());
            case NotificationItem.TYPE_PRIVATE_ADD:
                //title.set(ChaoliApplication.getAppContext().getString(R.string.send_you_a_private_post, notificationItem.getFromMemberName()));
                return ChaoliApplication.getAppContext().getString(R.string.send_you_a_private_post, getFromMemberName());
            default:
                return null;
        }
    }

    @Override
    public String getAvatarUsername() {
        return getFromMemberName();
    }

    @Override
    public int getConversationId() {
        switch (getType()) {
            case NotificationItem.DIVIDER:
            case NotificationItem.SPACE:
                return 0;
            default:
                return Integer.parseInt(getData().getConversationId());
        }
    }

    @Override
    public String getShowingContent() {
        return getData().getTitle();
    }
}


import com.google.gson.annotations.SerializedName;

/**
 * Created by jianhao on 16-9-3.
 */
public class Notification {
    public String fromMemberId;
    public String fromMemberName;
    @SerializedName("avatarFormat")
    public String avatarSuffix;
    public Data data;
    public String type;

    public static class Data{
        public String conversationId;
        public String postId;
        public String title;
    }
}


import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-9-3.
 * 注册时的问题
 */
public class Question {
    public class id{
        public String $id;
    }
    public id _id;

    public String getQuestion() {
        return question;
    }

    public Boolean isMultiAnswer(){
        return Boolean.valueOf(multi_answer);
    }

    public List<String> getOptions() {
        return options;
    }

    public String getChoice() {
        return choice;
    }

    public String question, choice, multi_answer;

    public List<String> options = new ArrayList<>();
    public List<String> answers = new ArrayList<>();

    public Question() {}
    public Question(BusinessQuestion businessQuestion) {
        _id = new id();
        _id.$id = businessQuestion.id;
        choice = String.valueOf(businessQuestion.choice);
        multi_answer = String.valueOf(businessQuestion.multiAnswer);
        if (choice.equals("false")) answers.add(businessQuestion.answer.get());
        else {
            for (int i = 0; i < businessQuestion.isChecked.size(); i++) {
                if (businessQuestion.isChecked.get(i)) answers.add(String.valueOf(i));
            }
        }
    }

    public static ArrayList<Question> fromList(List<BusinessQuestion> list) {
        ArrayList<Question> questionList = new ArrayList<>();
        for (BusinessQuestion businessQuestion : list) {
            questionList.add(new Question(businessQuestion));
        }
        return questionList;
    }
}


import java.util.List;

/**
 * Created by jianhao on 16-8-25.
 */
public class ConversationListResult {
    public List<Conversation> getResults() {
        return results;
    }

    public void setResults(List<Conversation> results) {
        this.results = results;
    }

    List<Conversation> results;
}


/**
 * Created by daquexian on 17-1-21.
 */

public class UserIdAndTokenResult {
    private int userId;
    private String token;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.utils.LoginUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by jianhao on 16-9-21.
 */

public class SignUpVM extends BaseViewModel {
    private static final String TAG = "SignUpVM";

    String inviteCode;
    String signUpUrl;
    String token;

    public ObservableField<String> username = new ObservableField<>("");
    public ObservableField<String> password = new ObservableField<>("");
    public ObservableField<String> confirm = new ObservableField<>("");
    public ObservableField<String> captcha = new ObservableField<>("");
    public ObservableField<String> email = new ObservableField<>("");
    public ObservableField<String> usernameError = new ObservableField<>("");
    public ObservableField<String> passwordError = new ObservableField<>("");
    public ObservableField<String> confirmError = new ObservableField<>("");
    public ObservableField<String> captchaError = new ObservableField<>("");
    public ObservableField<String> emailError = new ObservableField<>("");
    public ObservableField<Drawable> captchaImg = new ObservableField<>();

    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean showProcessDialog = new ObservableBoolean();
    public ObservableBoolean signUpSuccess = new ObservableBoolean();

    public SignUpVM(String inviteCode) {
        this.inviteCode = inviteCode;
        signUpUrl = Constants.SIGN_UP_URL + inviteCode;
    }

    public void init() {
        MyOkHttp.clearCookie();
        new MyOkHttp.MyOkHttpClient()
                .get(signUpUrl)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        showToast.notifyChange();
                        toastContent.set(getString(R.string.network_err));
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200) {
                            showToast.notifyChange();
                            toastContent.set(getString(R.string.network_err));
                        } else {
                            response.body().close();
                            String tokenFormat = "\"token\":\"([\\dabcdef]+)";
                            Pattern pattern = Pattern.compile(tokenFormat);
                            Matcher matcher = pattern.matcher(responseStr);
                            if (matcher.find()) {
                                token = matcher.group(1);
                                getAndShowCaptchaImage();
                            } else {
                                showToast.notifyChange();
                                toastContent.set(getString(R.string.network_err));
                            }
                        }
                    }
                });
    }
    private void getAndShowCaptchaImage(){
        captchaImg.set(ResourcesCompat.getDrawable(ChaoliApplication.getAppContext().getResources(), R.drawable.refreshing, null));
        new MyOkHttp.MyOkHttpClient()
                .get(Constants.GET_CAPTCHA_URL)
                .enqueue(new MyOkHttp.Callback1() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "onFailure: ");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        byte[] bytes = response.body().bytes();
                        captchaImg.set(new BitmapDrawable(ChaoliApplication.getAppContext().getResources(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length)));
                        Log.d(TAG, "onResponse: ");
                    }
                });
    }

    public void clickSignUp() {
        final String USERNAME_HAS_BEEN_USED = "用户名已经被注册了";
        final String EMAIL_HAS_BEEN_USED = "邮箱已被注册";
        final String WRONG_CAPTCHA = "你也许需要一个计算器";

        Boolean flagError = false;

        if (username.get().length() < 4 || username.get().length() > 21) {
            usernameError.set(getString(R.string.length_of_username_should_be_between_4_and_21));
            flagError = true;
        }
        if (password.get().length() < 6) {
            passwordError.set(getString(R.string.at_least_six_character));
            flagError = true;
        }
        if (!password.get().equals(confirm.get())) {
            confirmError.set(getString(R.string.should_be_same_with_password));
            flagError = true;
        }
        if (!email.get().contains("@") || !email.get().contains(".")) {
            emailError.set(getString(R.string.invaild_email));
            flagError = true;
        }
        if (flagError) return;

        showProcessDialog.set(true);

        MyOkHttp.MyOkHttpClient myOkHttpClient = new MyOkHttp.MyOkHttpClient()
                .add("username", username.get())
                .add("email", email.get())
                .add("password", password.get())
                .add("confirm", confirm.get())
                .add("mscaptcha", captcha.get())
                .add("token", token)
                .add("submit", "注册");

        myOkHttpClient.post(signUpUrl)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        toastContent.set(getString(R.string.network_err));
                        showToast.notifyChange();
                        getAndShowCaptchaImage();
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        if (response.code() != 200){
                            toastContent.set(getString(R.string.network_err));
                            showToast.notifyChange();
                            getAndShowCaptchaImage();
                            showProcessDialog.set(false);
                        }
                        else {
                            response.body().close();
                            if(responseStr.contains(USERNAME_HAS_BEEN_USED)){
                                usernameError.set(getString(R.string.username_has_been_used));
                            }else if(responseStr.contains(EMAIL_HAS_BEEN_USED)){
                                emailError.set(getString(R.string.email_has_been_used));
                            } else if (responseStr.contains(WRONG_CAPTCHA)) {
                                captchaError.set(getString(R.string.wrong_captcha));
                            } else {
                                /**
                                 * success
                                 */
                                Log.d(TAG, "onResponse: " + responseStr);
                                ChaoliApplication.getSp().edit().remove(Constants.INVITING_CODE_SP).apply();
                                LoginUtils.saveUsernameAndPasswordToSp(username.get(), password.get());
                                signUpSuccess.notifyChange();
                            }
                            showProcessDialog.set(false);
                        }

                    }
                });
    }

    public void clickRefreshCaptcha() {
        getAndShowCaptchaImage();
    }
}


import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;

import com.daquexian.chaoli.forum.binding.QuestionLayoutSelector;
import com.daquexian.chaoli.forum.model.BusinessQuestion;
import com.daquexian.chaoli.forum.utils.SignUpUtils;

import java.util.ArrayList;

/**
 * Created by jianhao on 16-9-21.
 */

public class AnswerQuestionsVM extends BaseViewModel {
    public ObservableArrayList<BusinessQuestion> questions = new ObservableArrayList<>();
    public QuestionLayoutSelector selector = new QuestionLayoutSelector();
    public ObservableBoolean pass = new ObservableBoolean(false);
    public String code;     // inviting code for passing or status code for failing
    public ObservableBoolean fail = new ObservableBoolean(false);
    public ObservableBoolean showDialog = new ObservableBoolean(false);

    public void getQuestions(String subject) {
        SignUpUtils.getQuestionObjList(new SignUpUtils.GetQuestionObserver() {
            @Override
            public void onGetQuestionObjList(ArrayList<BusinessQuestion> questionList) {
                questions.clear();
                questions.addAll(questionList);
            }
        }, subject);
    }

    public void submit() {
        showDialog.set(true);
        SignUpUtils.submitAnswers(questions, new SignUpUtils.SubmitObserver() {
            @Override
            public void onAnswersPass(String code) {
                AnswerQuestionsVM.this.code = code;
                showDialog.set(false);
                pass.notifyChange();
            }

            @Override
            public void onFailure(int statusCode) {
                AnswerQuestionsVM.this.code = String.valueOf(statusCode);
                showDialog.set(false);
                fail.notifyChange();
            }
        });
    }
}


import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.utils.LoginUtils;

/**
 * ViewModel for LoginActivity
 * Created by jianhao on 16-9-21.
 */

public class LoginActivityVM extends BaseViewModel {
    public ObservableField<String> username = new ObservableField<>();
    public ObservableField<String> password = new ObservableField<>();

    public ObservableInt showToast = new ObservableInt();
    public String toastContent;
    public ObservableBoolean showProgressDialog = new ObservableBoolean();
    public ObservableInt goToMainActivity = new ObservableInt();
    public ObservableInt clickAQ = new ObservableInt();
    public ObservableInt clickSignUp = new ObservableInt();

    public void clickLogin() {
        if ("".equals(username.get()) || "".equals(password.get())) {
            toastContent = "".equals(username.get()) ? getString(R.string.username)
                    + ("".equals(password.get()) ? " " + getString(R.string.and_password) : "") : getString(R.string.password);
            showToast.notifyChange();
        } else {
            showProgressDialog.set(true);
            LoginUtils.LoginObserver observer = new LoginUtils.LoginObserver() {
                @Override
                public void onLoginSuccess(int userId, String token) {
                    showProgressDialog.set(false);
                    goToMainActivity.notifyChange();
                }

                @Override
                public void onLoginFailure(int statusCode) {
                    showProgressDialog.set(false);
                    switch (statusCode) {
                        case LoginUtils.FAILED_AT_OPEN_LOGIN_PAGE:
                            toastContent = getString(R.string.network_err_open_login_page);
                            break;
                        case LoginUtils.FAILED_AT_GET_TOKEN_ON_LOGIN_PAGE:
                            toastContent = getString(R.string.network_err_get_token);
                            break;
                        case LoginUtils.FAILED_AT_LOGIN:
                            toastContent = getString(R.string.network_err_login);
                            break;
                        case LoginUtils.WRONG_USERNAME_OR_PASSWORD:
                            toastContent = getString(R.string.login_err_wrong_name_pwd);
                            break;
                        case LoginUtils.FAILED_AT_OPEN_HOMEPAGE:
                            toastContent = getString(R.string.network_err_homepage);
                            break;
                        case LoginUtils.COOKIE_EXPIRED:
                            toastContent = getString(R.string.login_err_cookie_expire);
                            break;
                        case LoginUtils.EMPTY_UN_OR_PW:
                            toastContent = getString(R.string.login_err_empty);
                            break;
                        case LoginUtils.ERROR_LOGIN_STATUS:
                            toastContent = getString(R.string.try_again);
                            break;
                    }
                    showToast.notifyChange();
                    LoginUtils.clear(ChaoliApplication.getAppContext());
                }
            };
            LoginUtils.begin_login(username.get(), password.get(), observer);
        }
    }

    public void clickSignUp() {
        clickSignUp.set(clickSignUp.get() + 1);
    }

    public void clickAnswerQuestion() {
        clickAQ.set(clickAQ.get() + 1);
    }
}


import android.content.SharedPreferences;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.AccountUtils;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jianhao on 16-10-6.
 */

public class SettingsVM extends BaseViewModel {
    private static final String TAG = "SVM";

    public ObservableBoolean showProcessDialog = new ObservableBoolean();
    public String dialogContent;
    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableInt goToAlbum = new ObservableInt();
    public ObservableBoolean complete = new ObservableBoolean();

    public ObservableField<String> signature = new ObservableField<>();
    public ObservableField<String> userStatus = new ObservableField<>();
    public ObservableBoolean privateAdd = new ObservableBoolean();
    public ObservableBoolean starOnReply = new ObservableBoolean();
    public ObservableBoolean starPrivate = new ObservableBoolean();
    public ObservableBoolean hideOnline = new ObservableBoolean();
    public File avatarFile;

    /* App Settings */
    public ObservableBoolean clickTwiceToExit = new ObservableBoolean();

    private SharedPreferences sharedPreferences;

    public SettingsVM(String signature, String userStatus, Boolean privateAdd, Boolean starOnReply, Boolean starPrivate, Boolean hideOnline) {
        this.signature.set(signature);
        this.userStatus.set(userStatus);
        this.privateAdd.set(privateAdd);
        this.starOnReply.set(starOnReply);
        this.starPrivate.set(starPrivate);
        this.hideOnline.set(hideOnline);

        sharedPreferences = getSharedPreferences(Constants.SETTINGS_SP, MODE_PRIVATE);
        clickTwiceToExit.set(sharedPreferences.getBoolean(Constants.CLICK_TWICE_TO_EXIT, false));
    }

    public void save() {
        dialogContent = getString(R.string.just_a_sec);
        showProcessDialog.set(true);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.CLICK_TWICE_TO_EXIT, clickTwiceToExit.get());
        editor.apply();
        AccountUtils.modifySettings(avatarFile, "Chinese", privateAdd.get(), starOnReply.get(),
                starPrivate.get(), hideOnline.get(), signature.get(), userStatus.get(), new AccountUtils.ModifySettingsObserver() {
                    @Override
                    public void onModifySettingsSuccess() {
                        AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
                            @Override
                            public void onGetProfileSuccess() {
                                Log.d(TAG, "onGetProfileSuccess: hi");
                                showProcessDialog.set(false);
                                dialogContent = getString(R.string.retrieving_new_data);
                                showProcessDialog.set(true);
                                // TODO: 16-11-16 adjust it with RxJava and Retrofit
                                AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
                                    @Override
                                    public void onGetProfileSuccess() {
                                        showProcessDialog.set(false);
                                        toastContent.set(getString(R.string.modified_successfully));
                                        showToast.notifyChange();
                                        complete.notifyChange();
                                    }

                                    @Override
                                    public void onGetProfileFailure() {
                                        showProcessDialog.set(false);
                                        toastContent.set(getString(R.string.network_err));
                                        showToast.notifyChange();
                                    }
                                });
                            }

                            @Override
                            public void onGetProfileFailure() {
                                showProcessDialog.set(false);
                                toastContent.set(getString(R.string.network_err));
                                showToast.notifyChange();
                            }
                        });
                    }

                    @Override
                    public void onModifySettingsFailure(int statusCode) {
                        showProcessDialog.set(false);
                        toastContent.set(getString(R.string.fail_on_modifying));
                        showToast.notifyChange();
                    }
                });
    }

    public void clickChangeAvatar() {
        goToAlbum.notifyChange();
    }

}


import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.ConversationLayoutSelector;
import com.daquexian.chaoli.forum.binding.LayoutSelector;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.model.ConversationListResult;
import com.daquexian.chaoli.forum.model.NotificationList;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.daquexian.chaoli.forum.utils.AccountUtils;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * ViewModel of MainActivity
 * Created by jianhao on 16-9-19.
 */
public class MainActivityVM extends BaseViewModel {
    private final String TAG = "MainActivityVM";

    public ObservableArrayList<Conversation> conversationList = new ObservableArrayList<>();

    public ObservableBoolean canRefresh = new ObservableBoolean(true);
    public ObservableBoolean isRefreshing = new ObservableBoolean();

    public ObservableField<Integer> myUserId = new ObservableField<>(-1);
    public ObservableField<String> myUsername = new ObservableField<>();
    public ObservableField<String> myAvatarSuffix = new ObservableField<>();
    public ObservableField<String> mySignature = new ObservableField<>();

    /**
     * isLoggedIn is only used to determine whether there are available username, signature etc to show
     * To check account status, please use LoginUtils.isLoggedIn() or Me.isEmpty()
     */
    public ObservableBoolean isLoggedIn = new ObservableBoolean(false);
    public ObservableBoolean loginComplete = new ObservableBoolean(false);

    public ObservableBoolean smoothToFirst = new ObservableBoolean(false);

    public ObservableInt goToHomepage = new ObservableInt();
    public ObservableInt goToLogin = new ObservableInt();
    public ObservableInt goToConversation = new ObservableInt();
    public Conversation clickedConversation;
    public ObservableInt notificationsNum = new ObservableInt(0);
    public ObservableBoolean showLoginProcessDialog = new ObservableBoolean(false);
    public ObservableBoolean toFirstLoadConversation = new ObservableBoolean();
    public ObservableInt selectedItem = new ObservableInt(-1);
    public ObservableInt goToPost = new ObservableInt();
    public ObservableBoolean failed = new ObservableBoolean();
    public ObservableBoolean showToast = new ObservableBoolean();
    public String toastContent;

    public LayoutSelector<Conversation> layoutSelector = new ConversationLayoutSelector();

    private static int RETURN_ERROR = -1;
    private CompositeSubscription subscription;
    private String channel;
    private int page;

    private Boolean canAutoLoad = false;

    public MainActivityVM() {
        //conversationList.add(new Conversation());
        subscription = new CompositeSubscription();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    private void getList(final int page) {
        getList(page, false);
    }
    private void getList(final int page, final Boolean refresh)
    {
        Log.d(TAG, "getList() called with: page = [" + page + "]");
        subscription.add(MyRetrofit.getService()
                .listConversations(channel, "#第 " + page + " 页")
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        showCircle();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ConversationListResult>() {
                    @Override
                    public void onCompleted() {
                        removeCircle();
                    }

                    @Override
                    public void onError(Throwable e) {
                        removeCircle();
                        if (refresh) {
                            failed.set(true);
                            failed.notifyChange();
                        }
                    }

                    @Override
                    public void onNext(ConversationListResult conversationListResult) {
                        //conversationList.remove(conversationList.size() - 1);
                        int oldLen = conversationList.size();
                        Log.d(TAG, "onNext: " + oldLen);
                        List<Conversation> newConversationList = conversationListResult.getResults();
                        canAutoLoad = true;
                        if (page == 1) {
                            conversationList.clear();
                            conversationList.addAll(newConversationList);
                        } else {
                            MyUtils.expandUnique(conversationList, newConversationList);

                            if (conversationList.size() == oldLen) {
                                canAutoLoad = false;
                            }
                        }
                        if (refresh) {
                            smoothToFirst.notifyChange();
                        }
                        MainActivityVM.this.page = page;
                    }
                }));
    }

    public void refresh(){
        page = 1;
        getList(page, true);
    }
    public void loadMore() {
        if (isRefreshing.get()) return;
        getList(page + 1);
    }

    /**
     * 去掉刷新时的圆圈
     */
    private void removeCircle() {
        isRefreshing.set(false);
        isRefreshing.notifyChange();
    }

    /**
     * 显示刷新时的圆圈
     */
    private void showCircle() {
        isRefreshing.set(true);
        isRefreshing.notifyChange();
    }

    public void onClickAvatar(View view) {
        if (isLoggedIn.get()) {
            goToHomepage.notifyChange();
        } else {
            goToLogin.notifyChange();
        }
    }

    public void onClickPostFab(View view) {
        goToPost.notifyChange();
    }

    public void onClickConversation(Conversation conversation) {
        Log.d(TAG, "onClickConversation: ");
        clickedConversation = conversation;
        goToConversation.notifyChange();
    }

    public void tryToLoadFromBottom() {
        if (canAutoLoad) {
            canAutoLoad = false;
            loadMore();
        }
    }

    public void startUp() {
        Log.d(TAG, "startUp() called");
        isRefreshing.set(true);

        if (LoginUtils.hasSavedData()) {
            isLoggedIn.set(true);   // isLoggedIn is only to determine whether there are available username, signature etc to show

            Me.setInstanceFromSharedPreference(ChaoliApplication.getAppContext(), LoginUtils.getSavedUsername());
            if (!Me.isEmpty()) {
                myUsername.set(LoginUtils.getSavedUsername());
                myAvatarSuffix.set(Me.getMyAvatarSuffix());
                mySignature.set(Me.getMySignature());
            } else {
                myUsername.set(getString(R.string.loading));
                mySignature.set(getString(R.string.loading));
            }
        }

        LoginUtils.begin_login(new LoginUtils.LoginObserver() {
            @Override
            public void onLoginSuccess(int userId, String token) {
                failed.set(false);
                isLoggedIn.set(true);
                loginComplete.set(true);

                Log.d(TAG, "onLoginSuccess: success");
                getProfile();

                if (channel.equals("") || channel.equals("all")) {
                    toFirstLoadConversation.notifyChange();   // update conversations
                }
                intervalSend();
            }

            @Override
            public void onLoginFailure(int statusCode) {
                if (statusCode == LoginUtils.EMPTY_UN_OR_PW) {
                    if (channel.equals("") || channel.equals("all")) {
                        toFirstLoadConversation.notifyChange();   // update conversations
                    }
                    failed.set(false);
                    isLoggedIn.set(false);
                    loginComplete.set(true);
                } else {
                    /**
                     * show error screen here
                     */
                    failed.set(true);
                    isRefreshing.set(false);
                    // no isLoggedIn.set(false) here, show name, signature in drawer
                    loginComplete.set(true);
                    toastContent = getString(R.string.network_err);
                    showToast.notifyChange();
                }
                Log.d(TAG, "onLoginFailure: " + statusCode);
            }
        });
    }

    public void getProfile() {
        Log.d(TAG, "getProfile() called, username = " + myUsername.get() + ", " + Me.getMyUsername());
        AccountUtils.getProfile(new AccountUtils.GetProfileObserver() {
            @Override
            public void onGetProfileSuccess() {
                myUserId.set(Me.getMyUserId());
                myUsername.set(Me.getMyUsername());
                myAvatarSuffix.set(Me.getMyAvatarSuffix());
                mySignature.set(Me.getMySignature());
            }

            @Override
            public void onGetProfileFailure() {

            }
        });
    }


    private void intervalSend(){
        subscription.add(Observable.interval(Constants.getNotificationInterval,
                Constants.getNotificationInterval,
                TimeUnit.SECONDS)
                .filter(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        return !Me.isEmpty();
                    }
                }).flatMap(new Func1<Long, Observable<NotificationList>>() {
                    @Override
                    public Observable<NotificationList> call(Long aLong) {
                        return MyRetrofit.getService().checkNotification();
                    }
                }).filter(new Func1<NotificationList, Boolean>() {
                    @Override
                    public Boolean call(NotificationList notificationList) {
                        return notificationList != null;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NotificationList>() {
                    @Override
                    public void onCompleted() {
                        notificationsNum.set(RETURN_ERROR);
                    }

                    @Override
                    public void onError(Throwable e) {
                        notificationsNum.set(RETURN_ERROR);
                    }

                    @Override
                    public void onNext(NotificationList notificationList) {
                        notificationsNum.set(notificationList.count);
                    }
                }));
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelByPosition(int position) {
        String[] channel =
                new String[]
                        {
                                "",
                                Channel.caff.name(),
                                Channel.maths.name(),
                                Channel.physics.name(),
                                Channel.chem.name(),
                                Channel.biology.name(),
                                Channel.tech.name(),
                                Channel.court.name(),
                                Channel.announ.name(),
                                Channel.others.name(),
                                Channel.socsci.name(),
                                Channel.lang.name(),
                        };
        return channel[position];
    }

    public void resume() {
        if(!Me.isEmpty()) {
            //timer.cancel();
            mySignature.set(Me.getMySignature());
            myAvatarSuffix.set(Me.getAvatarSuffix());
            intervalSend();
        }
    }

    public void destory() {
        MyOkHttp.getClient().dispatcher().cancelAll();
        subscription.clear();
    }
}


import android.content.SharedPreferences;

import com.daquexian.chaoli.forum.ChaoliApplication;

/**
 * Created by jianhao on 16-9-20.
 */

public abstract class BaseViewModel {
    protected String getString(int resId) {
        return ChaoliApplication.getAppContext().getString(resId);
    }

    protected SharedPreferences getSharedPreferences(String name, int mode) {
        return ChaoliApplication.getAppContext().getSharedPreferences(name, mode);
    }
}


import android.content.SharedPreferences;
import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;
import android.view.View;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.PostUtils;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jianhao on 16-9-21.
 */

public class ReplyActionVM extends BaseViewModel {
    private static final String TAG = "ReplyActionVM";

    public ObservableInt flag = new ObservableInt();
    public ObservableInt conversationId = new ObservableInt();
    public ObservableInt postId = new ObservableInt();
    public ObservableField<String> replyTo = new ObservableField<>();
    public ObservableField<String> replyMsg = new ObservableField<>();
    public ObservableField<String> content = new ObservableField<>("");
    private String prevContent;
    public ObservableBoolean selectionLast = new ObservableBoolean();

    public ObservableBoolean showDialog = new ObservableBoolean();
    public ObservableBoolean showWelcome = new ObservableBoolean();
    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean replyComplete = new ObservableBoolean(false);
    public ObservableBoolean editComplete = new ObservableBoolean(false);
    public ObservableBoolean demoMode = new ObservableBoolean();
    public ObservableBoolean updateRichText = new ObservableBoolean();

    private SharedPreferences sp;
    private SharedPreferences.Editor e;

    public ReplyActionVM(int flag, int conversationId, int postId, String replyTo, String replyMsg) {
        this.flag.set(flag);
        this.conversationId.set(conversationId);
        this.postId.set(postId);
        this.replyTo.set(replyTo);
        this.replyMsg.set(replyMsg);
        sp = getSharedPreferences(TAG, MODE_PRIVATE);
        e = sp.edit();
        String draft = sp.getString(String.valueOf(conversationId), "");
        if (!"".equals(draft)) content.set(draft);
        if (this.postId.get() != -1) content.set(String.format(Locale.ENGLISH, "[quote=%d:@%s]%s[/quote]\n", this.postId.get(), this.replyTo.get(), this.replyMsg.get()));
        //selectionLast.notifyChange();
        //selectionLast.set(content.get().length());
    }

    public void reply() {
        showDialog.set(true);
        PostUtils.reply(conversationId.get(), content.get(), new PostUtils.ReplyObserver()
        {
            @Override
            public void onReplySuccess()
            {
                toastContent.set(getString(R.string.reply_successfully));
                showToast.notifyChange();
                clearSaved();
                showDialog.set(false);
                replyComplete.set(true);
            }

            @Override
            public void onReplyFailure(int statusCode)
            {
                showDialog.set(false);
                toastContent.set("Fail: " + statusCode);
                showToast.notifyChange();
            }
        });

    }

    public void edit() {
        PostUtils.edit(postId.get(), content.get(), new PostUtils.EditObserver()
        {
            @Override
            public void onEditSuccess()
            {
                showToast.set(showToast.get() + 1);
                toastContent.set("Post"); // TODO: 16-10-11
                editComplete.set(true);
            }

            @Override
            public void onEditFailure(int statusCode)
            {
                showToast.set(showToast.get() + 1);
                toastContent.set("Fail:" + statusCode); // TODO: 16-10-11
            }
        });
    }

    public void changeDemoMode() {
        demoMode.set(!demoMode.get());
        SharedPreferences globalSP = getSharedPreferences(Constants.GLOBAL, MODE_PRIVATE);
        if (globalSP.getBoolean(Constants.FIRST_ENTER_DEMO_MODE, true) && demoMode.get()) {
            showWelcome.notifyChange();
            globalSP.edit().putBoolean(Constants.FIRST_ENTER_DEMO_MODE, false).apply();
        }
    }

    public void doAfterContentChanged() {
        Log.d(TAG, "doAfterContentChanged() called");
        String newContent = content.get();
        if (newContent.equals(prevContent)) return;
        prevContent = newContent;
        updateRichText.notifyChange();
        saveReply();
    }

    private void saveReply() {
        e.putString(String.valueOf(conversationId.get()), content.get()).apply();
    }

    private void clearSaved() {
        e.clear().apply();
    }

    @BindingAdapter("alpha")
    public static void setAlpha(View view,float alpha){
        view.setAlpha(alpha);
    }
}



import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;
import android.widget.ImageView;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.PostLayoutSelector;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.PostContentView;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.model.PostListResult;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;

/**
 * Created by jianhao on 16-9-21.
 */

public class PostActivityVM extends BaseViewModel implements PostContentView.OnViewClickListener {
    public Conversation conversation;
    public int conversationId;
    public String title;
    public ObservableBoolean isRefreshing = new ObservableBoolean(false);
    //public ObservableField<SwipyRefreshLayoutDirection> direction = new ObservableField<>(SwipyRefreshLayoutDirection.BOTH);
    public ObservableBoolean canRefresh = new ObservableBoolean(true);
    public final ObservableArrayList<Post> postList = new ObservableArrayList();
    public int minPage;
    public int maxPage;
    boolean isAuthorOnly;

    private Boolean reversed = false;
    private Boolean preview = true;     //是否是只加载了第一条帖子的状态
    private Boolean canAutoLoad = false;  // 是否可以自动加载
    private boolean hitBottom = false;

    public ObservableInt listPosition = new ObservableInt();
    public ObservableBoolean showToast = new ObservableBoolean(false);
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean updateToolbar = new ObservableBoolean();

    public ObservableBoolean goToReply = new ObservableBoolean(false);
    public ObservableBoolean goToQuote = new ObservableBoolean();
    public ObservableBoolean goToHomepage = new ObservableBoolean(false);
    public ObservableBoolean imgClicked = new ObservableBoolean(false);
    public ObservableBoolean tableButtonClicked = new ObservableBoolean(false);
    public Post clickedPost;
    public ImageView clickedImageView;
    public String[] header;
    public List<String[]> data;

    private static final String TAG = "PostActivityVM";

    public PostLayoutSelector layoutSelector = new PostLayoutSelector();

    public PostActivityVM(int conversationId, String title) {
        this.conversationId = conversationId;
        this.title = title;
        init();
    }

    public PostActivityVM(Conversation conversation) {
        this.conversation = conversation;
        postList.add(new Post(Integer.valueOf(conversation.getStartMemberId()), conversation.getStartMember(), conversation.getStartMemberAvatarSuffix(), conversation.getFirstPost(), conversation.getStartTime()));
        conversationId = conversation.getConversationId();
        title = conversation.getTitle();
        init();
    }

    private void init() {
        //postList.add(new Post());
    }

    public Boolean isReversed() {
        return reversed;
    }

    /*private void getList(final int page) {
        getList(page, false);
    }*/

    private void getList(final int page, final SuccessCallback callback) {
        MyRetrofit.getService()
                .listPosts(conversationId, page)
                .enqueue(new retrofit2.Callback<PostListResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<PostListResult> call, retrofit2.Response<PostListResult> response) {
                        Log.d(TAG, "onResponse: " + conversationId);
                        if (conversation == null) {
                            conversation = response.body().getConversation();
                            updateToolbar.notifyChange();
                        }

                        List<Post> newPosts = response.body().getPosts();
                        callback.doWhenSuccess(newPosts);
                        removeCircle();
                        if (reversed) {
                            canAutoLoad = page > 1;
                        } else {
                            final int postNum = conversation.getReplies() + 1;
                            canAutoLoad = page < (postNum + (Constants.POST_PER_PAGE - 1)) / Constants.POST_PER_PAGE;
                        }
                        // if (newPosts.size() == Constants.POST_PER_PAGE) canAutoLoad = true;
                    }

                    @Override
                    public void onFailure(retrofit2.Call<PostListResult> call, Throwable t) {
                        isRefreshing.set(false);
                        if (call.isCanceled()) return;
                        if (t.getMessage().contains("CANCEL")) return;
                        toastContent.set(ChaoliApplication.getAppContext().getString(R.string.network_err));
                        showToast.notifyChange();
                        t.printStackTrace();
                        //postList.get(postList.size() - 1).content = getString(R.string.error_click_to_retry);
                    }
                });
    }

    public void tryToLoadFromBottom() {
        if (canAutoLoad) {
            canAutoLoad = false;
            pullFromBottom();
        }
    }
    /*private void getList(final int page, final Boolean refresh) {
        MyRetrofit.getService()
                .listPosts(conversationId, page)
                .enqueue(new retrofit2.Callback<PostListResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<PostListResult> call, retrofit2.Response<PostListResult> response) {
                        Log.d(TAG, "onResponse: " + call.request().url().toString());
                        int oldLen = postList.size();
                        List<Post> newPostList = response.body().getPosts();
                        if (!reversed)
                            MyUtils.expandUnique(postList, newPostList);
                        else
                            postList.addAll(MyUtils.reverse(newPostList));
                        moveToPosition(refresh ? 0 : oldLen);
                        isRefreshing.set(false);

                        PostActivityVM.this.page = page;

                        //direction.set(SwipyRefreshLayoutDirection.BOTTOM);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<PostListResult> call, Throwable t) {
                        isRefreshing.set(false);
                        toastContent.set(ChaoliApplication.getAppContext().getString(R.string.network_err));
                        showToast.notifyChange();
                        t.printStackTrace();
                    }
                });
    }*/

    /*private Call<PostListResult> getList(int page) {
        return MyRetrofit.getService().listPosts(conversationId, page);
    }
*/
    public void reverse() {
        reversed = !reversed;
        firstLoad();
    }

    public void reverseBtnClick() {
        if (conversation == null) return;
        reverse();
    }

    public boolean hasFooterView() {
        return postList.size() > 0 && postList.get(postList.size() - 1).username == null;
    }

    public void firstLoad() {
        //direction.set(SwipyRefreshLayoutDirection.BOTH);
        showCircle();
        //postList.clear();
        maxPage = minPage = reversed ? (int) Math.ceil((conversation.getReplies() + 1) / (float) Constants.POST_PER_PAGE) : 1;

        getList(maxPage, new SuccessCallback() {
            @Override
            public void doWhenSuccess(List<Post> newPostList) {
                //if (preview) {
                postList.clear();
                preview = false;
                //}
                //if (hasFooterView()) postList.remove(postList.size() - 1);
                if (reversed) postList.addAll(MyUtils.reverse(newPostList));
                else postList.addAll(newPostList);

                if (postList.size() < 3) {
                    tryToLoadFromBottom();
                }
            }
        });

    }

    private void moveToPosition(int position) {
        listPosition.set(position);
        listPosition.notifyChange();
    }
    /*public void loadMore() {
        if (reversed && page == 1) {
            Log.d(TAG, "loadMore: ");
            isRefreshing.set(false);
            isRefreshing.notifyChange();
            return;
        }
        int nextPage = page;
        if (reversed) {
            nextPage = page - 1;
        } else
            if (postList.size() >= page * Constants.POST_PER_PAGE)
                nextPage = page + 1;
        getList(nextPage);
        //getList(postList.size() < page * Constants.POST_PER_PAGE ? page : (reversed ? page - 1 : page + 1));
    }*/

    private void loadAfterward() {
        final int nextPage;
        if (postList.size() >= (maxPage - minPage + 1) * Constants.POST_PER_PAGE) nextPage = maxPage + 1;
        else nextPage = maxPage;

        showCircle();
        final int oldLen = postList.size();
        getList(nextPage, new SuccessCallback() {
            @Override
            public void doWhenSuccess(List<Post> newPostList) {
                //if (postList.size() > 0) postList.remove(postList.size() - 1);
                Log.d(TAG, "doWhenSuccess: " + newPostList.size());
                if (reversed) {
                    MyUtils.expandUnique(postList, MyUtils.reverse(newPostList), false, reversed);
                    moveToPosition(0);
                }
                else MyUtils.expandUnique(postList, newPostList);
                //if (!reversed) moveToPosition(oldLen);
                maxPage = nextPage;
                //postList.add(new Post());
            }
        });
    }

    private void loadBackward() {
        if (minPage == 1) {
            removeCircle();
            return;
        }
        final int nextPage = minPage - 1;
        showCircle();
        //getList(nextPage);
        final int oldLen = postList.size();
        getList(nextPage, new SuccessCallback() {
            @Override
            public void doWhenSuccess(List<Post> newPostList) {
                //if (postList.size() > 0) postList.remove(postList.size() - 1);
                if (reversed) postList.addAll(MyUtils.reverse(newPostList));
                else MyUtils.expandUnique(postList, newPostList, false);
                //if (reversed) moveToPosition(oldLen);
                minPage = nextPage;
                //postList.add(new Post());
            }
        });
    }

    /**
     * 去掉刷新时的圆圈
     */
    public void removeCircle() {
        isRefreshing.set(false);
        isRefreshing.notifyChange();
    }

    /**
     * 显示刷新时的圆圈
     */
    public void showCircle() {
        isRefreshing.set(true);
        isRefreshing.notifyChange();
    }

    public void pullFromTop() {
        if (isRefreshing.get()) return;
        if (isReversed()) loadAfterward();
        else loadBackward();
    }

    public void pullFromBottom() {
        if (isRefreshing.get()) return;
        if (isReversed()) loadBackward();
        else loadAfterward();
    }

    public void clickFab() {
        goToReply.notifyChange();
    }

    public void quote(Post post) {
        if (!preview) {
            clickedPost = post;
            goToQuote.notifyChange();
        }
    }

    public void replyComplete() {
        isRefreshing.set(true);
        loadAfterward();
    }

    public void clickAvatar(Post post) {
        if (post.username == null) return;
        clickedPost = post;
        goToHomepage.notifyChange();
    }

    public void setPage(int page) {
        //this.page = page;
        maxPage = minPage = page;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isAuthorOnly() {
        return isAuthorOnly;
    }

    public void setAuthorOnly(boolean authorOnly) {
        isAuthorOnly = authorOnly;
    }

    @Override
    public void onImgClick(ImageView imageView) {
        clickedImageView = imageView;
        imgClicked.notifyChange();
    }

    private interface SuccessCallback {
        void doWhenSuccess(List<Post> newPostList);
    }
}


import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;

import com.daquexian.chaoli.forum.data.Me;

/**
 * Created by jianhao on 16-9-21.
 */

public class HomepageVM extends BaseViewModel {
    public ObservableField<String> username = new ObservableField<>();
    public ObservableField<String> signature = new ObservableField<>();
    public ObservableField<String> avatarSuffix = new ObservableField<>();
    public ObservableInt userId = new ObservableInt();
    public ObservableBoolean isSelf = new ObservableBoolean();

    public HomepageVM(String username, String signature, String avatarSuffix, int userId, Boolean isSelf) {
        this.username.set(username);
        this.userId.set(userId);
        this.signature.set(signature);
        this.avatarSuffix.set(avatarSuffix);
        this.isSelf.set(isSelf);
    }

    public void updateSelfProfile() {
        signature.set(Me.getMySignature());
        avatarSuffix.set(Me.getAvatarSuffix());
    }
}


import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.ObservableList;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.HistoryLayoutSelector;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.BusinessHomepageListItem;
import com.daquexian.chaoli.forum.model.HistoryItem;
import com.daquexian.chaoli.forum.model.HistoryResult;
import com.daquexian.chaoli.forum.model.NotificationResult;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.utils.MyUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;

/**
 * ViewModel for HistoryFragment
 * Created by jianhao on 16-10-2.
 */

public class HistoryFragmentVM extends BaseViewModel {
    private static String TAG = "HistoryFVM";

    public static final int TYPE_ACTIVITY = 0;
    public static final int TYPE_NOTIFICATION = 1;

    private int type = TYPE_ACTIVITY;   //type == 0表示History, type == 1表示Notification

    public ObservableList<BusinessHomepageListItem> showingItemList = new ObservableArrayList<>();

    private boolean canLoad = true;

    public ObservableBoolean isRefreshing = new ObservableBoolean();
    public ObservableBoolean showProgressDialog = new ObservableBoolean(false);
    public ObservableInt intendedConversationId = new ObservableInt();
    public ObservableField<String> intendedConversationTitle = new ObservableField<>();
    public ObservableInt intendedConversationPage = new ObservableInt();
    public ObservableInt goToPost = new ObservableInt();
    public ObservableBoolean showToast = new ObservableBoolean();
    public String toastContent;

    private int userId;
    private String username;
    private String avatarSuffix;

    public String url = Constants.GET_ACTIVITIES_URL;

    public int page = 1;

    public HistoryFragmentVM(int type, int userId, String username, String avatarSuffix) {
        this.type = type;
        this.userId = userId;
        this.username = username;
        this.avatarSuffix = avatarSuffix;
        url = type == TYPE_ACTIVITY ? Constants.GET_ACTIVITIES_URL + userId : Constants.GET_ALL_NOTIFICATIONS_URL;
    }

    public HistoryLayoutSelector layoutSelector = new HistoryLayoutSelector();

    public void clickItem(BusinessHomepageListItem item) {
        //final ProgressDialog progressDialog = ProgressDialog.show(context, "", getResources().getString(R.string.just_a_sec));
        Log.d(TAG, "clickItem() called with: item = [" + item + "]");
        showProgressDialog.set(true);
        new MyOkHttp.MyOkHttpClient()
                .get(Constants.GO_TO_POST_URL + item.postId.get())
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        showProgressDialog.set(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        //Intent intent = new Intent(mCallback, PostActivity.class);

                        Pattern pattern = Pattern.compile("\"conversationId\":(\\d+)");
                        Matcher matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            intendedConversationId.set(Integer.parseInt(matcher.group(1)));
                        } else {
                            toastContent = getString(R.string.conversation_has_been_deleted);
                            showToast.notifyChange();
                            return;
                        }

                        pattern = Pattern.compile("<h1 id='conversationTitle'>(.*?)</h1>");
                        matcher = pattern.matcher(responseStr);
                        if (matcher.find()) {
                            String title = matcher.group(1);
                            title = title.replaceAll("(^<(.*?)>)|(<(.*?)>$)", "");
                            //intent.putExtra("title", title);
                            intendedConversationTitle.set(title);
                        } else {
                            toastContent = getString(R.string.conversation_has_been_deleted);
                            showToast.notifyChange();
                            return;
                        }

                        //if (v.equals(holder.content_tv)) {
                            pattern = Pattern.compile("\"startFrom\":(\\d+)");
                            matcher = pattern.matcher(responseStr);
                            if (matcher.find()) {
                                int intentToPage = Integer.parseInt(matcher.group(1)) / 20 + 1;
                                intendedConversationPage.set(intentToPage);
                            } else {
                                intendedConversationPage.set(-1);
                            }
                        //}
                        showProgressDialog.set(false);
                        goToPost.notifyChange();
                    }
                });
    }

    public void refresh() {
        isRefreshing.set(true);
        new MyOkHttp.MyOkHttpClient()
                .get(url)
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        //swipyRefreshLayout.setRefreshing(false);
                        isRefreshing.set(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        List<BusinessHomepageListItem> listItems = parseItems(responseStr);

                        //MyUtils.expandUnique(showingItemList, listItems, false);
                        //Log.d(TAG, String.valueOf(myAdapter.listItems.size()));
                        showingItemList.clear();
                        showingItemList.addAll(listItems);
                        addTimeDivider(showingItemList);

                        //myAdapter.notifyDataSetChanged();
                        //swipyRefreshLayout.setRefreshing(false);
                        isRefreshing.set(false);
                    }
                });
    }

    public void tryToLoadMore() {
        if (canLoad) {
            canLoad = false;
            loadMore();
        }
    }

    public void loadMore() {
        isRefreshing.set(true);
        Log.d(TAG, "loadMore: " + page);
        new MyOkHttp.MyOkHttpClient()
                .get(url + "/" + (page + 1))
                .enqueue(new MyOkHttp.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        isRefreshing.set(false);
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        page++;
                        List<BusinessHomepageListItem> listItems = parseItems(responseStr);

                        final int listSize = listItems.size();
                        if(listSize == 0){
                            isRefreshing.set(false);
                            return;
                        }

                        int oldSize = showingItemList.size();

                        MyUtils.expandUnique(showingItemList, listItems);

                        canLoad = oldSize != showingItemList.size();

                        addTimeDivider(showingItemList);
                        isRefreshing.set(false);
                    }
                });
    }

    private List<BusinessHomepageListItem> parseItems(String responseStr) {
        if (type == TYPE_ACTIVITY) {
            Gson gson = new GsonBuilder().registerTypeAdapterFactory(new HistoryAdapterFactory()).create();
            HistoryResult historyResult = gson.fromJson(responseStr, HistoryResult.class);
            return BusinessHomepageListItem.parseList(historyResult.activity);
        } else {
            NotificationResult notificationResult = new Gson().fromJson(responseStr, NotificationResult.class);
            return BusinessHomepageListItem.parseList(notificationResult.getResults());
        }
    }

    /**
     * Data不存在时为false，存在时为一个json对象，必须在它为false的时候跳过，否则会产生错误
     */
    private static class HistoryAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != HistoryItem.Data.class) return null;

            TypeAdapter<HistoryItem.Data> defaultAdapter = (TypeAdapter<HistoryItem.Data>) gson.getDelegateAdapter(this, type);
            return (TypeAdapter<T>) new DataAdapter(defaultAdapter);

        }

        public class DataAdapter extends TypeAdapter<HistoryItem.Data> {
            TypeAdapter<HistoryItem.Data> defaultAdapter;

            DataAdapter(TypeAdapter<HistoryItem.Data> defaultAdapter) {
                this.defaultAdapter = defaultAdapter;
            }
            @Override
            public void write(JsonWriter out, HistoryItem.Data value) throws IOException {
                defaultAdapter.write(out, value);
            }

            @Override
            public HistoryItem.Data read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.BOOLEAN) {
                    in.skipValue();
                    return null;
                }
                return defaultAdapter.read(in);
            }
        }
    }


    public void addTimeDivider(List<BusinessHomepageListItem> items){
        int dateNow;
        if(items.size() > 0 && !BusinessHomepageListItem.DIVIDER.equals(items.get(0).getType()) && !BusinessHomepageListItem.SPACE.equals(items.get(0).getType())){
            int firstDate = (int) ((Long.parseLong(items.get(0).getTime()) + 8 * 60 * 60) / 24 / 60 / 60);
            dateNow = (int) (Calendar.getInstance().getTimeInMillis() / 1000 / 24 / 60 / 60);
            items.add(0, new BusinessHomepageListItem(new Divider(dateNow - firstDate)));
        }
        //if(items.size() > 0 && !(items.get(0).getType().equals(BusinessHomepageListItem.SPACE))) items.add(0, new BusinessHomepageListItem(new Space()));
        for(int i = 0; i < items.size() - 1; i++){
            BusinessHomepageListItem thisItem = items.get(i), nextItem = items.get(i + 1);
            if(!BusinessHomepageListItem.DIVIDER.equals(thisItem.getType()) && !BusinessHomepageListItem.DIVIDER.equals(nextItem.getType())
                    && !BusinessHomepageListItem.SPACE.equals(thisItem.getType()) && !BusinessHomepageListItem.SPACE.equals(nextItem.getType())){
                int thisDate = (int) ((Long.parseLong(thisItem.getTime()) + 8 * 60 * 60) / 24 / 60 / 60);
                int nextDate = (int) ((Long.parseLong(nextItem.getTime()) + 8 * 60 * 60) / 24 / 60 / 60);
                if(thisDate != nextDate) {
                    dateNow = (int) (Calendar.getInstance().getTimeInMillis() / 1000 / 24 / 60 / 60);
                    items.add(i + 1, new BusinessHomepageListItem(new Divider(dateNow - nextDate)));
                }
            }
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public abstract static class ListItem implements Comparable<ListItem> {
        public final static String ITEM         = "item";
        public final static String DIVIDER      = "divider";
        public final static String SPACE        = "space";

        public String getType() {
            return type;
        }

        public String getTime() {
            return time;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public abstract String getShowingTitle();
        public abstract String getShowingContent();
        public abstract int getShowingPostId();
        public abstract int getConversationId();
        public abstract String getAvatarSuffix();
        public abstract String getAvatarUsername();
        public abstract int getAvatarUserId();

        String type, time;

        @Override
        public int compareTo(ListItem B) {
            if (Long.valueOf(time) > Long.valueOf(B.time)) {
                return 1;
            } else if (Long.valueOf(time) == Long.valueOf(B.time)){
                return 0;
            } else {
                return -1;
            }
        }
    }

    static class Divider extends ListItem{
        String time;
        @Override
        public String getType() {
            return ListItem.DIVIDER;
        }

        @Override
        public String getTime() {
            return time;
        }

        Divider(int time){
            this.time = String.valueOf(time);
        }

        @Override
        public String getShowingTitle() {
            return null;
        }

        @Override
        public String getShowingContent() {
            int timeDiff = Integer.parseInt(getTime());
            if(timeDiff == 0){
                //content.set(ChaoliApplication.getAppContext().getString(R.string.today));
                return ChaoliApplication.getAppContext().getString(R.string.today);
            } else {
                //holder.time_tv.setText(getString(R.string.days_ago, timeDiff));
                //content.set(ChaoliApplication.getAppContext().getString(R.string.days_ago, timeDiff));
                return ChaoliApplication.getAppContext().getString(R.string.days_ago, timeDiff);
            }
        }

        @Override
        public int getAvatarUserId() {
            return 0;
        }

        @Override
        public int getConversationId() {
            return 0;
        }

        @Override
        public int getShowingPostId() {
            return 0;
        }

        @Override
        public String getAvatarSuffix() {
            return null;
        }

        @Override
        public String getAvatarUsername() {
            return null;
        }
    }

    static class Space extends ListItem{
        @Override
        public String getTime() {
            return null;
        }

        @Override
        public String getType() {
            return ListItem.SPACE;
        }

        @Override
        public int getShowingPostId() {
            return 0;
        }

        @Override
        public int getConversationId() {
            return 0;
        }

        @Override
        public String getShowingContent() {
            return null;
        }

        @Override
        public String getAvatarUsername() {
            return null;
        }

        @Override
        public String getAvatarSuffix() {
            return null;
        }

        @Override
        public int getAvatarUserId() {
            return 0;
        }

        @Override
        public String getShowingTitle() {
            return null;
        }
    }
}


import android.content.SharedPreferences;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.ConversationUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by jianhao on 16-9-21.
 */

public class PostActionVM extends BaseViewModel {
    private static final String TAG = "PostActionVM";
    public ObservableField<String> title = new ObservableField<>();
    public ObservableField<String> content = new ObservableField<>();
    private String prevContent;
    private String prevTitle;
    public ObservableInt channelId = new ObservableInt();

    public ObservableBoolean postComplete = new ObservableBoolean(false);
    public ObservableInt showToast = new ObservableInt();
    public ObservableField<String> toastContent = new ObservableField<>();
    public ObservableBoolean showWelcome = new ObservableBoolean();
    public ObservableBoolean showDialog = new ObservableBoolean();

    public ObservableBoolean updateContentRichText = new ObservableBoolean();
    public ObservableBoolean updateTitleRichText = new ObservableBoolean();
    public ObservableBoolean demoMode = new ObservableBoolean();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    //private SharedPreferences globalSP;
    //private SharedPreferences.Editor globalSPEditor;
    private Channel preChannel = Channel.caff;
    private Channel curChannel;

    private static final String DRAFT_CONTENT = "draft_content";
    private static final String DRAFT_TITLE = "draft_title";
    private static final String DRAFT_CHANNEL = "draft_channel";

    public PostActionVM() {
        sharedPreferences = ChaoliApplication.getAppContext().getSharedPreferences(TAG, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        title.set(sharedPreferences.getString(DRAFT_TITLE, ""));
        content.set(sharedPreferences.getString(DRAFT_CONTENT, ""));
        prevContent = content.get();
        channelId.set(sharedPreferences.getInt(DRAFT_CHANNEL, Channel.caff.getChannelId()));
        curChannel = Channel.getChannel(channelId.get());

        content.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {

            }
        });
    }

    public void postConversation() {
        if (content.get().length() == 0) {
            toastContent.set(getString(R.string.content_cannot_be_null));
            showToast.notifyChange();
            return;
        }

        showDialog.set(true);
        ConversationUtils.postConversation(title.get(), content.get(), new ConversationUtils.PostConversationObserver() {
            @Override
            public void onPostConversationSuccess(int conversationId) {
                Log.d(TAG, "onPostConversationSuccess() called with: conversationId = [" + conversationId + "]");
                editor.clear().apply();
                postComplete.notifyChange();
                //postComplete.set(true);
                showDialog.set(false);
            }

            @Override
            public void onPostConversationFailure(int statusCode) {
                Log.d(TAG, "onPostConversationFailure() called with: statusCode = [" + statusCode + "]");
                toastContent.set(getString(R.string.network_err));
                showToast.notifyChange();
                showDialog.set(false);
            }
        });
    }

    public void changeDemoMode() {
        demoMode.set(!demoMode.get());
        SharedPreferences globalSP = getSharedPreferences(Constants.GLOBAL, MODE_PRIVATE);
        if (globalSP.getBoolean(Constants.FIRST_ENTER_DEMO_MODE, true) && demoMode.get()) {
            showWelcome.notifyChange();
            globalSP.edit().putBoolean(Constants.FIRST_ENTER_DEMO_MODE, false).apply();
        }
    }

    // TODO: 16-11-16 这样写很不利于维护，理想的做法应该是把自动显示表情的功能集中到自定义控件里，外部“即插即用”，但偏偏和双向绑定冲突（引起不断循环），最好考虑一种解决办法
    public void doAfterContentChanged() {
        String newContent = content.get();
        if (newContent.equals(prevContent)) return;
        prevContent = newContent;
        //updateContentRichText.notifyChange();
        saveContent(newContent);
    }

    public void doAfterTitleChanged() {
        String newTitle = title.get();
        if (newTitle.equals(prevTitle)) return;
        prevTitle = newTitle;
        //updateTitleRichText.notifyChange();
        saveTitle(newTitle);
    }

    public void setChannelId(final int channelId) {
        this.channelId.set(channelId);
        preChannel = curChannel;
        saveChannelId(channelId);
        ConversationUtils.setChannel(channelId, new ConversationUtils.SetChannelObserver() {
            @Override
            public void onSetChannelSuccess() {
                // what need to do has done in advance
            }

            @Override
            public void onSetChannelFailure(int statusCode) {   // restore
                showToast.notifyChange();
                toastContent.set(getString(R.string.network_err));

                PostActionVM.this.channelId.set(preChannel.getChannelId());
                curChannel = preChannel;
                saveChannelId(curChannel.getChannelId());
            }
        });
    }

    public void saveTitle(String title) {
        editor.putString(DRAFT_TITLE, title).apply();
    }

    private void saveContent(String content) {
        editor.putString(DRAFT_CONTENT, content).apply();
    }

    private void saveChannelId(int channelId) {
        editor.putInt(DRAFT_CHANNEL, channelId).apply();
    }
}


import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Post;

/**
 * Created by jianhao on 16-9-27.
 */

public class PostLayoutSelector extends LayoutSelector<Post> {
    @Override
    int getType(Post item) {
        if (item.content == null && item.conversationId == 0) return FOOTER_VIEW;
        if (item.deleteMemberId != 0) return 1;
        return 0;
    }

    @Override
    int getLayout(int type) {
        switch (type) {
            case 0:
                return R.layout.post_view;
            case 1:
                return R.layout.post_view_delete;
            case FOOTER_VIEW:
                return R.layout.loading_item;
        }
        return 0;
    }
}


import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.BusinessQuestion;

/**
 * Created by jianhao on 16-10-13.
 */

public class QuestionLayoutSelector extends LayoutSelector<BusinessQuestion> {
    private final int CHECK_BTN_ITEM_TYPE = 0;
    private final int RADIO_BTN_ITEM_TYPE = 1;
    private final int EDIT_TEXT_ITEM_TYPE = 2;

    @Override
    int getLayout(int type) {
        switch (type) {
            case EDIT_TEXT_ITEM_TYPE:
                return R.layout.question_item_et;
            case CHECK_BTN_ITEM_TYPE:
                return R.layout.question_item_cb;
            case RADIO_BTN_ITEM_TYPE:
                return R.layout.question_item_rb;
            default:
                throw new IllegalArgumentException("Wrong type");
        }
    }

    @Override
    int getType(BusinessQuestion item) {
        if((!item.choice)) return EDIT_TEXT_ITEM_TYPE;
        return item.multiAnswer ? CHECK_BTN_ITEM_TYPE : RADIO_BTN_ITEM_TYPE;
    }
}


import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.BusinessHomepageListItem;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;

/**
 * Created by jianhao on 16-10-2.
 */

public class HistoryLayoutSelector extends LayoutSelector<BusinessHomepageListItem> {
    @Override
    int getLayout(int type) {
        switch (type) {
            case 0:
                return R.layout.history_item;
            case 1:
                return R.layout.history_divider;
            case 2:
                return R.layout.history_space;
            default:
                throw new RuntimeException("Impossible");
        }
    }

    @Override
    int getType(BusinessHomepageListItem item) {
        if (item.getType().equals(HistoryFragmentVM.ListItem.DIVIDER)) return 1;
        else if (item.getType().equals(HistoryFragmentVM.ListItem.SPACE)) return 2;
        else return 0;
    }
}


/**
 * Created by jianhao on 16-9-19.
 */
public interface DiffItem {
    boolean areContentsTheSame(DiffItem anotherItem);
    boolean areItemsTheSame(DiffItem anotherItem);
}


import android.databinding.BindingAdapter;
import android.text.TextPaint;
import android.widget.TextView;

/**
 * Binding adapter for TextView
 * Created by jianhao on 16-10-3.
 */

public class TextViewBA {
    @SuppressWarnings("unused")
    private static final String TAG = "TVBA";
    @BindingAdapter("app:bold")
    public static void setBold(TextView textView, Boolean isBold) {
        TextPaint tp = textView.getPaint();
        tp.setFakeBoldText(isBold);
    }
}



import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Conversation;

/**
 * Created by daquexian on 16-11-10.
 */

public class ConversationLayoutSelector extends LayoutSelector<Conversation> {
    @Override
    int getLayout(int type) {
        switch (type) {
            case 0:
                return R.layout.conversation_view;
            case FOOTER_VIEW:
                return R.layout.loading_item_for_conversation;
        }
        throw new IllegalArgumentException("wrong type");
    }

    @Override
    int getType(Conversation item) {
        if (item.getReplies() == -1) return FOOTER_VIEW;
        return 0;
    }
}


import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.ChannelTextView;

/**
 * Created by jianhao on 16-9-25.
 */

public class ChannelBA {
    @BindingAdapter("app:channelId")
    public static void setChannel(ChannelTextView channelTextView, int channelId) {
        channelTextView.setChannel(Channel.getChannel(channelId));
    }
}


/**
 * Created by jianhao on 16-9-27.
 */

public abstract class LayoutSelector<T> {
    abstract int getLayout(int type);
    abstract int getType(T item);
    public static final int FOOTER_VIEW = -1;
}


import android.databinding.BindingAdapter;
import android.util.Log;

import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

/**
 * SwipyRefreshLayout的BindingAdapter
 * Created by daquexian on 16-9-19.
 */
public class SwipyRefreshBA {
    private static final String TAG = "SWBA";
    //trigger the circle to animate
    @BindingAdapter("app:isRefreshing")
    public static void setRefreshing(final SwipyRefreshLayout swipyRefreshLayout, final Boolean isRefreshing) {
        swipyRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipyRefreshLayout.setRefreshing(isRefreshing);
            }
        });
    }

    /**
     * 为了应对奇怪的事情：把方向设为BOTTOM就没法在setRefreshing(true)之后看到小圆圈
     * @param swipyRefreshLayout ..
     * @param direction ..
     */
    @BindingAdapter("app:direction")
    public static void setDirection(final SwipyRefreshLayout swipyRefreshLayout, final SwipyRefreshLayoutDirection direction) {
        Log.d(TAG, "setDirection() called with: swipyRefreshLayout = [" + swipyRefreshLayout + "], direction = [" + direction + "]");
        swipyRefreshLayout.setDirection(direction);
    }

    @BindingAdapter("app:canRefresh")
    public static void canRefresh(final SwipyRefreshLayout swipyRefreshLayout, final Boolean canRefresh) {
        swipyRefreshLayout.setEnabled(canRefresh);
    }
}


/**
 * Created by jianhao on 16-9-27.
 */

public class DefaultLayoutSelector<T> extends LayoutSelector<T>{
    int mLayoutId;

    public DefaultLayoutSelector(int layoutId) {
        mLayoutId = layoutId;
    }

    @Override
    int getLayout(int type) {
        return mLayoutId;
    }

    @Override
    int getType(T item) {
        return 0;
    }
}


import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.meta.PostContentView;
import com.daquexian.chaoli.forum.model.Post;

/**
 * Created by jianhao on 16-9-27.
 */

public class PostContentViewBA {
    @BindingAdapter("app:post")
    public static void setPost(PostContentView postContentView, Post post) {
        postContentView.setPost(post);
    }

    @BindingAdapter("app:listener")
    public static void setListener(PostContentView postContentView, PostContentView.OnViewClickListener onViewClickListener) {
        postContentView.setOnImgClickListener(onViewClickListener);
    }
}

import android.databinding.BindingAdapter;

import com.daquexian.chaoli.forum.meta.AvatarView;

/**
 * Created by jianhao on 16-9-19.
 */
public class AvatarViewBA {
    @BindingAdapter({"bind:imageSuffix", "bind:userId", "bind:username"})
    public static void loadImage(AvatarView avatarView, String imageSuffix, int userId, String username) {
        if (userId != -1) avatarView.update(imageSuffix, userId, username);
    }

    @BindingAdapter({"bind:imageSuffix", "bind:userId", "bind:username", "app:login"})
    public static void loadImage(AvatarView avatarView, String imageSuffix, int userId, String username, Boolean login) {
        if (login) {
            loadImage(avatarView, imageSuffix, userId, username);
        } else {
            avatarView.setLoginImage(avatarView.getContext());
        }
    }

    @BindingAdapter("bind:length")
    public static void scale(AvatarView avatarView, int length) {
        avatarView.scale(length);
    }

    @BindingAdapter("bind:isLoggedIn")
    public static void loadLoginImage(AvatarView avatarView, Boolean login) {
        if (!login) avatarView.setLoginImage(avatarView.getContext());
    }
}


import android.databinding.BindingAdapter;
import android.support.design.widget.TextInputLayout;

/**
 * Created by jianhao on 16-10-6.
 */

public class TextInputLayoutBA {
    @BindingAdapter("app:error")
    public static void setError(TextInputLayout textInputLayout, String error) {
        textInputLayout.setError(error);
    }
}


import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableList;
import android.databinding.ViewDataBinding;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daquexian.chaoli.forum.BR;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-9-19.
 */
public class RecyclerViewBA {
    private static final String TAG = "RVAdapter";

    @BindingAdapter("app:position")
    public static void setPosition(RecyclerView recyclerView, int position) {
        recyclerView.smoothScrollToPosition(position);
    }

    @BindingAdapter({"app:itemList", "app:selector", "app:handler"})
    @SuppressWarnings("unchecked")
    public static void setItems(RecyclerView recyclerView, ObservableList newItems, LayoutSelector layoutSelector, BaseViewModel viewModel) {
        if (recyclerView.getAdapter() == null) {
            MyAdapter adapter = new MyAdapter(layoutSelector, newItems);
            adapter.setHandler(viewModel);
            recyclerView.setAdapter(adapter);
        } else {
            MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
            List oldItems = adapter.getItemList();
            adapter.setItemList(newItems);
            if ((newItems.size() > 0 && newItems.get(0) instanceof DiffItem) || (oldItems.size() > 0 && oldItems.get(0) instanceof DiffItem)) {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(oldItems, newItems), true);
                diffResult.dispatchUpdatesTo(adapter);
            } else {
                if (adapter.preview) {
                    adapter.notifyDataSetChanged();
                    newItems.addOnListChangedCallback(adapter.onListChangedCallback);
                    adapter.preview = false;
                }
            }
        }
    }

    @BindingAdapter({"app:itemList", "app:itemRes", "app:handler"})
    @SuppressWarnings("unchecked")
    public static void setItems(RecyclerView recyclerView, ObservableList newItems, int itemRes, BaseViewModel viewModel) {
        if (recyclerView.getAdapter() == null) {
            MyAdapter adapter = new MyAdapter(new DefaultLayoutSelector(itemRes), newItems);
            adapter.setHandler(viewModel);
            recyclerView.setAdapter(adapter);
        } else {
            MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
            List oldItems = adapter.getItemList();
            adapter.setItemList(newItems);
            if ((newItems.size() > 0 && newItems.get(0) instanceof DiffItem) || (oldItems.size() > 0 && oldItems.get(0) instanceof DiffItem)) {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(oldItems, newItems), true);
                diffResult.dispatchUpdatesTo(adapter);
            } else {
                if (adapter.preview) {
                    adapter.notifyDataSetChanged();
                    newItems.addOnListChangedCallback(adapter.onListChangedCallback);
                    adapter.preview = false;
                }
            }
        }
    }

    @BindingAdapter({"app:itemList", "app:selector"})
    @SuppressWarnings("unchecked")
    public static void setItems(RecyclerView recyclerView, ObservableList newItems, LayoutSelector layoutSelector) {
        if (recyclerView.getAdapter() == null) {
            MyAdapter adapter = new MyAdapter(layoutSelector, newItems);
            recyclerView.setAdapter(adapter);
        } else {
            MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
            List oldItems = adapter.getItemList();
            adapter.setItemList(newItems);
            if ((newItems.size() > 0 && newItems.get(0) instanceof DiffItem) || (oldItems.size() > 0 && oldItems.get(0) instanceof DiffItem)) {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(oldItems, newItems), true);
                diffResult.dispatchUpdatesTo(adapter);
            } else {
                if (adapter.preview) {
                    adapter.notifyDataSetChanged();
                    newItems.addOnListChangedCallback(adapter.onListChangedCallback);
                    adapter.preview = false;
                }
            }
        }
    }

    @BindingAdapter({"app:itemList", "app:itemRes"})
    @SuppressWarnings("unchecked")
    public static void setItems(RecyclerView recyclerView, ObservableList newItems, int itemRes) {
        if (recyclerView.getAdapter() == null) {
            MyAdapter adapter = new MyAdapter(new DefaultLayoutSelector(itemRes), newItems);
            recyclerView.setAdapter(adapter);
        } else {
            MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
            List oldItems = adapter.getItemList();
            adapter.setItemList(newItems);
            if ((newItems.size() > 0 && newItems.get(0) instanceof DiffItem) || (oldItems.size() > 0 && oldItems.get(0) instanceof DiffItem)) {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(oldItems, newItems), true);
                diffResult.dispatchUpdatesTo(adapter);
            } else {
                if (adapter.preview) {
                    adapter.notifyDataSetChanged();
                    newItems.addOnListChangedCallback(adapter.onListChangedCallback);
                    adapter.preview = false;
                }
            }
        }
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        //int resId;
        Boolean preview = true;
        List itemList = new ArrayList();
        BaseViewModel handler;
        LayoutSelector selector;
        ObservableList.OnListChangedCallback onListChangedCallback = new ObservableList.OnListChangedCallback() {
            @Override
            public void onChanged(ObservableList observableList) {
                Log.d(TAG, "onChanged() called with: observableList = [" + observableList + "]");
                MyAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(ObservableList observableList, int i, int i1) {
                Log.d(TAG, "onItemRangeChanged() called with: observableList = [" + observableList + "], i = [" + i + "], i1 = [" + i1 + "]");
                MyAdapter.this.notifyItemRangeChanged(i, i1);
            }

            @Override
            public void onItemRangeInserted(ObservableList observableList, int i, int i1) {
                Log.d(TAG, "onItemRangeInserted() called with: observableList = [" + observableList + "], i = [" + i + "], i1 = [" + i1 + "]");
                MyAdapter.this.notifyItemRangeInserted(i, i1);
            }

            @Override
            public void onItemRangeMoved(ObservableList observableList, int i, int i1, int i2) {
                Log.d(TAG, "onItemRangeMoved() called with: observableList = [" + observableList + "], i = [" + i + "], i1 = [" + i1 + "], i2 = [" + i2 + "]");
                for (int i3 = 0; i3 < i1; i3++) {
                    MyAdapter.this.notifyItemMoved(i + i3, i2 + i3);
                }
            }

            @Override
            public void onItemRangeRemoved(ObservableList observableList, int i, int i1) {
                Log.d(TAG, "onItemRangeRemoved() called with: observableList = [" + observableList + "], i = [" + i + "], i1 = [" + i1 + "]");
                MyAdapter.this.notifyItemRangeRemoved(i, i1);
            }
        };

        /*@SuppressWarnings("unchecked")
        MyAdapter(int resId, ArrayList itemList) {
            this.resId = resId;
            this.itemList = new ArrayList(itemList);
        }*/

        @SuppressWarnings("unchecked")
        MyAdapter(LayoutSelector layoutSelector, List itemList) {
            this.selector = layoutSelector;
            this.itemList = new ArrayList(itemList);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            if (holder.hasItem) {
                ViewDataBinding binding = holder.getBinding();
                if (holder.hasItem) binding.setVariable(BR.item, itemList.get(position));
                if (handler != null) binding.setVariable(BR.handler, handler);
            }
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return selector.getType(itemList.get(position));
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //if (viewType == LayoutSelector.FOOTER_VIEW) return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(selector.getLayout(viewType), parent, false));
            ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), selector.getLayout(viewType), parent, false);
            return new MyViewHolder(binding);
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            Boolean hasItem = true;
            ViewDataBinding binding;
            MyViewHolder(ViewDataBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            MyViewHolder(View view) {
                super(view);
                hasItem = false;
            }

            public ViewDataBinding getBinding() {
                return binding;
            }
        }

        public List getItemList() {
            return itemList;
        }

        @SuppressWarnings("unchecked")
        public void setItemList(List itemList) {
            this.itemList = new ArrayList(itemList);
        }

        public BaseViewModel getHandler() {
            return handler;
        }

        public void setHandler(BaseViewModel handler) {
            this.handler = handler;
        }

        public LayoutSelector getSelector() {
            return selector;
        }
    }

    private static class DiffCallback extends DiffUtil.Callback {
        List<DiffItem> oldDiffItemList, newDiffItemList;

        DiffCallback(List<DiffItem> oldDiffItemList, List<DiffItem> newDiffItemList){
            this.oldDiffItemList = oldDiffItemList;
            this.newDiffItemList = newDiffItemList;
        }

        @Override
        public int getNewListSize() {
            return newDiffItemList.size();
        }

        @Override
        public int getOldListSize() {
            return oldDiffItemList.size();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DiffItem oldDiffItem = oldDiffItemList.get(oldItemPosition);
            DiffItem newDiffItem = newDiffItemList.get(newItemPosition);
            return oldDiffItem.areContentsTheSame(newDiffItem);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            DiffItem oldDiffItem = oldDiffItemList.get(oldItemPosition);
            DiffItem newDiffItem = newDiffItemList.get(newItemPosition);
            return oldDiffItem.areItemsTheSame(newDiffItem);
        }
    }

}



import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.databinding.MainActivityBinding;
import com.daquexian.chaoli.forum.databinding.NavigationHeaderBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.NightModeHelper;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.utils.DataBindingUtils;
import com.daquexian.chaoli.forum.utils.MyUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.MainActivityVM;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

public class MainActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener
{
	private static final String LAYOUTMANAGER＿STATE = "layoutManager";
	private static final String TOOLBAR_OFFSET = "toolbar_offset";
	public static final String TAG = "MainActivity";

	public Toolbar toolbar;
	public DrawerLayout mDrawerLayout;
	public LinearLayoutManager layoutManager;
	public Parcelable layoutManagerState = null;

	private Context mContext = this;

	private ProgressDialog loginProgressDialog;

	private final int POST_CONVERSATION_CODE = 1;
	private final int LOGIN_CODE = 2;

	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;

	RecyclerView l;

	public SwipyRefreshLayout swipyRefreshLayout;

	ActionBarDrawerToggle actionBarDrawerToggle;

	MainActivityVM viewModel;
	MainActivityBinding binding;

	boolean bottom = true;	//是否滚到底部
	boolean needTwoClick = false;
	boolean clickedOnce = false;	//点击Back键两次退出

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(LAYOUTMANAGER＿STATE,layoutManager.onSaveInstanceState());
		outState.putInt(TOOLBAR_OFFSET, binding.appbar.getOffset());
		super.onSaveInstanceState(outState);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (NightModeHelper.getViewModel() == null) {
			viewModel = new MainActivityVM();
			setViewModel(viewModel);
			addCallbacks();
			viewModel.setChannel("all");
			viewModel.startUp();
		} else {
			viewModel = (MainActivityVM) NightModeHelper.getViewModel();	// remove the reference to ViewModel in NightModeHelper later in initUI
			setViewModel(viewModel);
			addCallbacks();
		}

		initUI(savedInstanceState);

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	private void addCallbacks() {
		DataBindingUtils.addCallback(this, viewModel.goToLogin, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToLogin();
			}
		});

		DataBindingUtils.addCallback(this, viewModel.goToHomepage, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToMyHomePage();
			}
		});

		DataBindingUtils.addCallback(this, viewModel.goToConversation, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToConversation(viewModel.clickedConversation);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.notificationsNum, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableInt) observable).get() > 0) {
					setCircleIndicator();
				} else {
					setNormalIndicator();
				}
			}
		});

		DataBindingUtils.addCallback(this, viewModel.showLoginProcessDialog, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) {
					showLoginProcessDialog();
				} else {
					dismissLoginProcessDialog();
				}
			}
		});

		DataBindingUtils.addCallback(this, viewModel.showToast, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(viewModel.toastContent);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.selectedItem, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				selectItem(((ObservableInt) observable).get());
			}
		});

		DataBindingUtils.addCallback(this, viewModel.toFirstLoadConversation, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				selectItem(0, false);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.goToPost, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				goToPostAction();
			}
		});

		DataBindingUtils.addCallback(this, viewModel.failed, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) showToast(R.string.network_err);
			}
		});

		DataBindingUtils.addCallback(this, viewModel.smoothToFirst, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable sender, int propertyId) {
				l.smoothScrollToPosition(0);
			}
		});

		/**
		 * 根据登录状态更改侧栏菜单
		 */
		DataBindingUtils.addCallback(this, viewModel.isLoggedIn, new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				binding.navigationView.getMenu().clear();
				binding.navigationView.inflateMenu(viewModel.isLoggedIn.get() ? R.menu.menu_navigation : R.menu.menu_navigation_no_login);
			}
		});

	}

	public void selectItem(int position) {
		selectItem(position, true);
	}

	public void selectItem(int position, boolean closeDrawers) {
		viewModel.setChannel(viewModel.getChannelByPosition(position));
		viewModel.refresh();
		if (closeDrawers) {
			mDrawerLayout.closeDrawers();
		}
	}

	public void initUI(Bundle savedInstanceState) {
		Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
		configToolbar(R.string.app_name);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
		actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
		actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
		actionBarDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDrawerLayout.openDrawer(GravityCompat.START);
			}
		});
		actionBarDrawerToggle.syncState();
		mDrawerLayout.addDrawerListener(actionBarDrawerToggle);

		l = binding.conversationList;
		l.addItemDecoration(new android.support.v7.widget.DividerItemDecoration(mContext, android.support.v7.widget.DividerItemDecoration.VERTICAL));

		layoutManager = new LinearLayoutManager(mContext);
		l.setLayoutManager(layoutManager);
		if (NightModeHelper.getViewModel() != null) {
			layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUTMANAGER＿STATE));
		}

		swipyRefreshLayout = binding.conversationListRefreshLayout;

		if (NightModeHelper.getViewModel() != null) {
			MyUtils.setToolbarOffset(binding.cl, binding.appbar, savedInstanceState.getInt(TOOLBAR_OFFSET));
		}

		binding.appbar.addOnOffsetChangedListener(this);
		binding.conversationList.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				//得到当前显示的最后一个item的view
                View lastChildView = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount()-1);
				if (lastChildView == null) return;
                //得到lastChildView的bottom坐标值
                int lastChildBottom = lastChildView.getBottom();
                //得到Recyclerview的底部坐标减去底部padding值，也就是显示内容最底部的坐标
                int recyclerBottom =  recyclerView.getBottom()-recyclerView.getPaddingBottom();
                //通过这个lastChildView得到这个view当前的position值
                int lastVisiblePosition  = layoutManager.findLastVisibleItemPosition();

                //判断lastChildView的bottom值跟recyclerBottom
                //判断lastPosition是不是最后一个position
                //如果两个条件都满足则说明是真正的滑动到了底部
				int lastPosition = recyclerView.getLayoutManager().getItemCount() - 1;
				if(lastChildBottom == recyclerBottom && lastVisiblePosition == lastPosition){
                    bottom = true;
                    viewModel.canRefresh.set(true);
                }else{
                    bottom = false;
                }
				if (lastVisiblePosition >= lastPosition - 3) viewModel.tryToLoadFromBottom();
			}
		});

		final NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
		navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				if (item.getItemId()==R.id.nightMode){
					NightModeHelper.changeMode(viewModel);
					getWindow().setWindowAnimations(R.style.modechange);
					recreate();
				}else {
					selectItem(item.getOrder());
					item.setChecked(true);
				}
				return true;
			}
		});

		swipyRefreshLayout.setDirection(SwipyRefreshLayoutDirection.BOTH);

		swipyRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener()
		{
			@Override
			public void onRefresh(SwipyRefreshLayoutDirection direction)
			{
				if (direction == SwipyRefreshLayoutDirection.TOP) {
					viewModel.refresh();
				} else {
					viewModel.loadMore();
				}
			}
		});

		NightModeHelper.removeViewModel();
	}

	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) mDrawerLayout.closeDrawer(GravityCompat.START);
		else if (clickedOnce || !needTwoClick) super.onBackPressed();
		else {
			showToast(R.string.click_once_more_to_exit);
			clickedOnce = true;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					clickedOnce = false;
				}
			}, 2500);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy() called");
		DataBindingUtils.removeCallbacks(this);
		viewModel.destory();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (layoutManagerState!=null){
			layoutManager.onRestoreInstanceState(layoutManagerState);
		}
		viewModel.resume();
		needTwoClick = getSharedPreferences(Constants.SETTINGS_SP, MODE_PRIVATE).getBoolean(Constants.CLICK_TWICE_TO_EXIT, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a mTitle for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.daquexian.chaoli.forum/http/host/path")
		);
		AppIndex.AppIndexApi.start(client, viewAction);
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a mTitle for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app URL is correct.
				Uri.parse("android-app://com.daquexian.chaoli.forum/http/host/path")
		);
		AppIndex.AppIndexApi.end(client, viewAction);
		client.disconnect();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case POST_CONVERSATION_CODE:
				if (resultCode == RESULT_OK) {
					viewModel.refresh();
				}
				break;
			case LOGIN_CODE:
				if (resultCode == RESULT_OK) {
					viewModel.refresh();
					viewModel.getProfile();
					viewModel.loginComplete.set(true);
					viewModel.isLoggedIn.set(true);
					viewModel.myUsername.set(getString(R.string.loading));
					viewModel.mySignature.set(getString(R.string.loading));
				}
				break;
		}
	}

	public void goToConversation(Conversation conversation) {
		conversation.setUnread("0");
		Intent jmp = new Intent();
		jmp.putExtra("conversation", conversation);
		//jmp.putExtra("conversationId", conversation.getConversationId());
		//jmp.putExtra("conversationTitle", conversation.getTitle());
		jmp.setClass(this, PostActivity.class);
		startActivity(jmp);
	}

	public void goToMyHomePage() {
		Log.d(TAG, "goToMyHomePage: " + Me.isEmpty());
		if(viewModel.isLoggedIn.get()){
			if(!Me.isEmpty()) {
				Intent intent = new Intent(this, HomepageActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("username", Me.getUsername());
				bundle.putInt("userId", Me.getUserId());
				bundle.putString("signature", Me.getPreferences().getSignature());
				bundle.putString("avatarSuffix", Me.getAvatarSuffix() == null ? Constants.NONE : Me.getAvatarSuffix());
				bundle.putBoolean("isSelf",  true);
				intent.putExtras(bundle);
				startActivity(intent);
			}
		}
	}

	public void goToLogin() {
		// startActivity(new Intent(this, LoginActivity.class));
		startActivityForResult(new Intent(this, LoginActivity.class), LOGIN_CODE);
	}

	public void goToPostAction() {
		Intent intent = new Intent(this, PostAction.class);
		startActivityForResult(intent, POST_CONVERSATION_CODE);
	}

	public void setCircleIndicator() {
		actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_menu_black_with_a_circle_24dp);
		actionBarDrawerToggle.syncState();
	}

	public void setNormalIndicator() {
		actionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
		actionBarDrawerToggle.syncState();
	}

	public void showLoginProcessDialog() {
		loginProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.logging_in));
	}

	public void dismissLoginProcessDialog() {
		loginProgressDialog.dismiss();
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		/**
		 * verticalOffset == 0说明appbar已经是展开状态
		 */
		viewModel.canRefresh.set(verticalOffset == 0 || bottom);
	}

	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (MainActivityVM) viewModel;
		binding = DataBindingUtil.setContentView(this, R.layout.main_activity);
		binding.setViewModel(this.viewModel);
		NavigationHeaderBinding navigationHeaderBinding = NavigationHeaderBinding.bind(binding.navigationView.getHeaderView(0));
		navigationHeaderBinding.setViewModel(this.viewModel);
	}
}


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.PostActionBinding;
import com.daquexian.chaoli.forum.meta.Channel;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.SFXParser3;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.PostActionVM;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianhao on 16-5-31.
 * Activity for start a new conversation
 */
public class PostAction extends BaseActivity implements IView {

    private static final String TAG = "PostAction";

    public static final int MENU_POST = 2;
    public static final int MENU_DEMO = 1;

    private PostActionVM viewModel;
    private PostActionBinding binding;

    private ProgressDialog progressDialog;

    private List<View> expressionsIVList = new ArrayList<>();

    /* 切换至演示模式时保存光标位置，切换回普通模式时恢复 */
    private int selectionStart, selectionEnd;

    private BottomSheetBehavior behavior;

    private final Context mContext = this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setViewModel(new PostActionVM());

        init();
    }

    private void init() {
        configToolbar(R.string.post);

        final String[] channelArr = {getString(R.string.channel_caff), getString(R.string.channel_maths),
                getString(R.string.channel_physics), getString(R.string.channel_chem),
                getString(R.string.channel_biology), getString(R.string.channel_tech), getString(R.string.channel_lang),
                getString(R.string.channel_socsci)};

        viewModel.updateContentRichText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
            }
        });

        viewModel.updateTitleRichText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
            }
        });

        viewModel.demoMode.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    selectionStart = binding.content.getSelectionStart();
                    selectionEnd = binding.content.getSelectionEnd();
                    binding.content.setEnabled(false);
                    binding.content.setOnlineImgEnabled(true);
                    binding.content.update();
                } else {
                    binding.content.setEnabled(true);
                    binding.content.setOnlineImgEnabled(false);
                    binding.content.update();
                    binding.content.setSelection(selectionStart, selectionEnd);
                }
            }
        });

        binding.title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.doAfterTitleChanged();
            }
        });

        View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) tryToShowSoftKeyboard(view);
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToShowSoftKeyboard(view);
            }
        };

        binding.title.setOnFocusChangeListener(onFocusChangeListener);
        binding.content.setOnFocusChangeListener(onFocusChangeListener);
        binding.title.setOnClickListener(onClickListener);
        binding.content.setOnClickListener(onClickListener);

        binding.content.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.doAfterContentChanged();
            }
        });

        binding.channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(mContext).setTitle("选择板块").setItems(channelArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //noinspection ConstantConditions
                        viewModel.setChannelId(Channel.getChannel(channelArr[which]).getChannelId());
                    }
                }).setCancelable(false).show();
            }
        });

        viewModel.postComplete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                setResult(RESULT_OK);
                finish();
            }
        });

        viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showToast(viewModel.toastContent.get());
            }
        });

        viewModel.showWelcome.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                new AlertDialog.Builder(mContext).setMessage(R.string.welcome_to_demo_mode)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();
            }
        });

        viewModel.showDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) progressDialog = ProgressDialog.show(PostAction.this, "", getString(R.string.just_a_sec));
                else progressDialog.dismiss();
            }
        });

        /*
         * 让各个表情按钮响应单击事件
         */

        View.OnClickListener onExpressionClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    final View focused = getCurrentFocus();
                    if (focused instanceof EditText) {
                        final EditText focusedET = (EditText) focused;
                        for (int j = 0; j < Constants.icons.length; j++) {
                            String icon = Constants.icons[j];
                            CharSequence contentDescription = view.getContentDescription();
                            if (icon.equals(contentDescription)) {
                                int start = Math.max(focusedET.getSelectionStart(), 0);
                                int end = Math.max(focusedET.getSelectionEnd(), 0);
                                focusedET.getText().replace(Math.min(start, end), Math.max(start, end),
                                        Constants.iconStrs[j], 0, Constants.iconStrs[j].length());
                                updateRichText();
                                break;
                            }
                        }
                    }
                }
            }
        };

        final ViewGroup expressions = (ViewGroup) ((ViewGroup) binding.expressions).getChildAt(0);
        for (int i = 0; i < expressions.getChildCount(); i++) {
            ViewGroup subView = (ViewGroup) expressions.getChildAt(i);
            for (int j = 0; j < subView.getChildCount(); j++) {
                View expressionView = subView.getChildAt(j);
                expressionsIVList.add(expressionView);
                expressionView.setAlpha(Constants.MIN_EXPRESSION_ALPHA);
                expressionView.setOnClickListener(onExpressionClickListener);
            }
        }

        behavior = BottomSheetBehavior.from(binding.bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //bottomSheetState = newState;
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                for (View expressionView : expressionsIVList) {
                    expressionView.setAlpha(Constants.MIN_EXPRESSION_ALPHA + slideOffset * (1 - Constants.MIN_EXPRESSION_ALPHA));
                }
            }
        });

        binding.title.requestFocus();
        tryToShowSoftKeyboard(binding.title);
    }

    private void updateRichText() {
        int selectionStart = binding.content.getSelectionStart();
        int selectionEnd = binding.content.getSelectionEnd();
        binding.content.setText(SFXParser3.parse(getApplicationContext(), viewModel.content.get(), null));
        binding.content.setSelection(selectionStart, selectionEnd);
        selectionStart = binding.title.getSelectionStart();
        selectionEnd = binding.title.getSelectionEnd();
        binding.title.setText(SFXParser3.parse(getApplicationContext(), viewModel.title.get(), null));
        binding.title.setSelection(selectionStart, selectionEnd);
    }

    private void tryToShowSoftKeyboard(View view) {
        if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Menu.NONE, MENU_DEMO, R.string.post).setIcon(R.drawable.ic_functions_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, Menu.NONE, MENU_POST, R.string.post).setIcon(R.drawable.ic_cab_done_mtrl_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        super.onOptionsItemSelected(item);
        switch (item.getOrder())
        {
            case MENU_POST:
                Log.d(TAG, "onOptionsItemSelected: ");
                if (!LoginUtils.isLoggedIn()){
                    showToast(R.string.please_login);
                    break;
                }
                viewModel.postConversation();
                break;
            case MENU_DEMO:
                viewModel.changeDemoMode();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (PostActionVM) viewModel;
        binding = DataBindingUtil.setContentView(this, R.layout.post_action);
        binding.setViewModel(this.viewModel);
    }
}


/**
 * 计划中这个是ReplyAction和PostAction共同的父类，处理关于表情和LaTeX的事务
 * Created by daquexian on 16-11-16.
 */

public class NewContentAction {
    // TODO: 16-11-16
}


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.PostActivityBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.DividerItemDecoration;
import com.daquexian.chaoli.forum.meta.MyAppBarLayout;
import com.daquexian.chaoli.forum.meta.NightModeHelper;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.utils.ConversationUtils;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.utils.MyUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.PostActivityVM;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PostActivity extends BaseActivity implements ConversationUtils.IgnoreAndStarConversationObserver, MyAppBarLayout.OnStateChangeListener, AppBarLayout.OnOffsetChangedListener {
	public static final String TAG = "PostActivity";

	private static final int POST_NUM_PER_PAGE = 20;
	public static final int REPLY_CODE = 1;

	private final Context mContext = this;

	public FloatingActionButton reply;

	public static SharedPreferences sp;
	public SharedPreferences.Editor e;

	Conversation mConversation;
	int mConversationId;
	String mTitle;
	int mPage;

	boolean mShowPhotoView;
	int mToolbarOffset;

	RecyclerView postListRv;
	SwipyRefreshLayout swipyRefreshLayout;
	MyAppBarLayout appBarLayout;

	LinearLayoutManager mLinearLayoutManager;

	PostActivityVM viewModel;
	PostActivityBinding binding;

	Boolean bottom = true; //是否滚动到底部

	public static final int menu_settings = 0;
	public static final int menu_reverse = menu_settings + 1;
	public static final int menu_share = menu_reverse + 1;
	public static final int menu_author_only = menu_share + 1;
	public static final int menu_star = menu_author_only + 1;

	private void initUI() {
		reply = binding.reply;
		postListRv = binding.postList;
		swipyRefreshLayout = binding.swipyRefreshLayout;
		appBarLayout = binding.appbar;
		appBarLayout.addOnOffsetChangedListener(this);
		appBarLayout.setOnStateChangeListener(this);
		//appBarLayout.addOnOffsetChangedListener(this);
		binding.postList.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				//得到当前显示的最后一个item的view
				View lastChildView = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount()-1);
				if (lastChildView == null) return;
				//得到lastChildView的bottom坐标值
				int lastChildBottom = lastChildView.getBottom();
				//得到Recyclerview的底部坐标减去底部padding值，也就是显示内容最底部的坐标
				int recyclerBottom =  recyclerView.getBottom()-recyclerView.getPaddingBottom();
				//通过这个lastChildView得到这个view当前的position值
				int lastVisiblePosition  = recyclerView.getLayoutManager().getPosition(lastChildView);

				//判断lastChildView的bottom值跟recyclerBottom
				//判断lastPosition是不是最后一个position
				//如果两个条件都满足则说明是真正的滑动到了底部
				/* Why <= ? */
				int lastPosition = recyclerView.getLayoutManager().getItemCount() - 1;
				if(lastChildBottom <= recyclerBottom && lastVisiblePosition == lastPosition){
					bottom = true;
					if (appBarLayout.getState() == MyAppBarLayout.State.COLLAPSED) {
						viewModel.canRefresh.set(true);
						Log.d(TAG, "onScrolled: collapsed");
					}
				}else{
					bottom = false;
				}

				if (lastVisiblePosition >= lastPosition - 3) viewModel.tryToLoadFromBottom();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Bundle data = getIntent().getExtras();
		mConversation = data.getParcelable("conversation");
		if (mConversation != null) {
			mConversationId = mConversation.getConversationId();
			//viewModel.setConversationId(mConversationId);
			mTitle = mConversation.getTitle();
			viewModel = new PostActivityVM(mConversation);
		} else {
			mConversationId = data.getInt("conversationId");
			mTitle = data.getString("conversationTitle", getString(R.string.app_name));
			viewModel = new PostActivityVM(mConversationId, mTitle);
		}
		setTitle(mTitle);
		mPage = data.getInt("page", 1);
		setViewModel(viewModel);
		viewModel.setPage(mPage);
		viewModel.setAuthorOnly(data.getBoolean("isAuthorOnly", false));
		sp = getSharedPreferences(Constants.postSP + mConversationId, MODE_PRIVATE);

		initUI();

		configToolbar(mTitle);

		getToolbar().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
                new AlertDialog.Builder(PostActivity.this)
                        .setMessage(mTitle)
						.setPositiveButton(android.R.string.ok, null)
						.show();
			}
		});

		swipyRefreshLayout.setDirection(SwipyRefreshLayoutDirection.BOTH);
		swipyRefreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh(SwipyRefreshLayoutDirection direction) {
				if (direction == SwipyRefreshLayoutDirection.BOTTOM) {
                    PostActivity.this.viewModel.pullFromBottom();
				} else {
					PostActivity.this.viewModel.pullFromTop();
				}
			}
		});

		mLinearLayoutManager = new LinearLayoutManager(mContext);
        postListRv.setLayoutManager(mLinearLayoutManager);
		postListRv.addItemDecoration(new DividerItemDecoration(mContext));

		viewModel.firstLoad();

		viewModel.updateToolbar.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				mTitle = viewModel.conversation.getTitle();
				configToolbar(mTitle);
				setTitle(mTitle);
			}
		});

		this.viewModel.goToReply.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				Intent toReply = new Intent(mContext, ReplyAction.class);
				toReply.putExtra("conversationId", PostActivity.this.viewModel.conversationId);
				startActivityForResult(toReply, REPLY_CODE);
			}
		});

		this.viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(PostActivity.this.viewModel.toastContent.get());
			}
		});

		this.viewModel.goToHomepage.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				Post post = PostActivity.this.viewModel.clickedPost;
				Intent intent = new Intent(PostActivity.this, HomepageActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("username", post.getUsername());
				bundle.putInt("userId", post.getMemberId());
				bundle.putString("signature", post.getSignature());
				bundle.putString("avatarSuffix", post.getAvatarFormat() == null ? Constants.NONE : post.getAvatarFormat());
				intent.putExtras(bundle);
				startActivity(intent);
			}
		});

		this.viewModel.goToQuote.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				quote(PostActivity.this.viewModel.clickedPost);
			}
		});

		this.viewModel.imgClicked.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showPhotoView();
			}
		});

		this.viewModel.tableButtonClicked.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showTableView();
			}
		});
	}

	private void showTableView() {
	}

	@SuppressWarnings("ConstantConditions")
	private void showPhotoView() {
		mShowPhotoView = true;
		binding.photoView.setImageDrawable(viewModel.clickedImageView.getDrawable());
		binding.photoView.setVisibility(View.VISIBLE);
		binding.photoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		mToolbarOffset = binding.appbar.getOffset();
		appBarLayout.setExpanded(false, false);
		reply.hide();
		binding.photoView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
			@Override
			public void onPhotoTap(View view, float x, float y) {

			}

			@Override
			public void onOutsidePhotoTap() {
				hidePhotoView();
			}
		});
	}

	@SuppressWarnings("ConstantConditions")
	private void hidePhotoView() {
		binding.photoView.setVisibility(View.GONE);
		binding.photoView.setSystemUiVisibility(0);
		MyUtils.setToolbarOffset(binding.cl, binding.appbar, mToolbarOffset);
		reply.show();
		mShowPhotoView = false;
	}

	private void quote(Post post) {
		Intent toReply = new Intent(PostActivity.this, ReplyAction.class);
		toReply.putExtra("conversationId", mConversationId);
		toReply.putExtra("postId", post.getPostId());
		toReply.putExtra("replyTo", post.getUsername());
		toReply.putExtra("replyMsg", MyUtils.formatQuote(post.getContent()));
		startActivityForResult(toReply, REPLY_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REPLY_CODE) {
			if (resultCode == RESULT_OK) {
				viewModel.replyComplete();
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (mShowPhotoView) {
			hidePhotoView();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		if (NightModeHelper.isDay()) {
			menu.add(Menu.NONE, Menu.NONE, menu_reverse, R.string.descend).setIcon(R.drawable.ic_sort_black_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}else {
			menu.add(Menu.NONE, Menu.NONE, menu_reverse, R.string.descend).setIcon(R.drawable.ic_sort_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
		menu.add(Menu.NONE, Menu.NONE, menu_share, R.string.share).setIcon(R.drawable.ic_share_black_24dp);//.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		// menu.add(Menu.NONE, Menu.NONE, menu_star, R.string.star).setIcon(R.drawable.ic_menu_star);
		menu.add(Menu.NONE, Menu.NONE, menu_settings, R.string.settings).setIcon(android.R.drawable.ic_menu_manage);
		// menu.add(Menu.NONE, Menu.NONE, menu_author_only, viewModel.isAuthorOnly() ? R.string.cancel_author_only : R.string.author_only).setIcon(android.R.drawable.ic_menu_view);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);
		switch (item.getOrder())
		{
			case menu_settings:
				CharSequence[] settingsMenu = {getString(R.string.ignore_this), getString(R.string.mark_as_unread)};
				AlertDialog.Builder ab = new AlertDialog.Builder(this)
						.setTitle(R.string.settings)
						.setCancelable(true)
						.setItems(settingsMenu, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								switch (which)
								{
									case 0:
										ConversationUtils.ignoreConversation(mContext, viewModel.conversationId, (PostActivity) mContext);
										break;
									case 1:
										showToast(R.string.mark_as_unread);
										break;
								}
							}
						});
				ab.show();
				break;
			case menu_reverse:
				if (viewModel.isReversed()) {
					item.setTitle(R.string.descend);
				} else {
					item.setTitle(R.string.ascend);
				}
				viewModel.reverseBtnClick();
				break;
			case menu_share:
				share();
				break;
			case menu_author_only:
				/*finish();
				Intent author_only = new Intent(this, PostActivity.class);
				author_only.putExtra("conversationId", viewModel.conversationId);
				author_only.putExtra("page", viewModel.isAuthorOnly ? "" : "?author=lz");
				author_only.putExtra("title", viewModel.title);
				author_only.putExtra("isAuthorOnly", !isAuthorOnly);
				startActivity(author_only);*/
				break;
			case menu_star:
				// TODO: 16-3-28 2201 Star light
				if (!LoginUtils.isLoggedIn()){
                    showToast(R.string.please_login);
                    break;
                }
				ConversationUtils.starConversation(this, viewModel.conversationId, this);
				break;
		}

		return true;
	}

	@Override
	public void onIgnoreConversationSuccess(Boolean isIgnored)
	{
		Toast.makeText(PostActivity.this, isIgnored ? R.string.ignore_this_success : R.string.ignore_this_cancel_success, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onIgnoreConversationFailure(int statusCode)
	{
		Toast.makeText(PostActivity.this, getString(R.string.failed, statusCode), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStarConversationSuccess(Boolean isStarred)
	{
		Toast.makeText(PostActivity.this, isStarred ? R.string.star_success : R.string.star_cancel_success, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onStarConversationFailure(int statusCode)
	{
		Toast.makeText(PostActivity.this, getString(R.string.failed, statusCode), Toast.LENGTH_SHORT).show();
	}

	private void share() {
		String shareContent =  viewModel.conversation.getTitle() + "\n" + Constants.BASE_URL + viewModel.conversationId
				+ "\n\n" + getString(R.string.share_message);
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
		shareIntent.setType("text/plain");
		startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
	}

	/*@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		viewModel.canRefresh.set(verticalOffset == 0 || bottom);
		Log.d(TAG, "onOffsetChanged: " + viewModel.canRefresh.get());
	}*/



	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (PostActivityVM) viewModel;
		binding = DataBindingUtil.setContentView(this, R.layout.post_activity);
		binding.setViewModel(this.viewModel);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		MyOkHttp.getClient().dispatcher().cancelAll();
	}

	@Override
	public void onStateChange(MyAppBarLayout.State state) {
		Log.d(TAG, "onStateChange: " + state);
		if (state == MyAppBarLayout.State.IDLE) viewModel.canRefresh.set(false);
		else if (state == MyAppBarLayout.State.EXPANDED) {
			viewModel.canRefresh.set(true);
			Log.d(TAG, "onStateChange: expanded");
		}
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		if (this.appBarLayout.getState() == MyAppBarLayout.State.IDLE) viewModel.canRefresh.set(false);
	}
}


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.network.MyOkHttp;
import com.daquexian.chaoli.forum.network.MyOkHttp.Callback;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Response;


/**
 * 在个人主页显示用户数据的Fragment
 * Created by jianhao on 16-6-5.
 */
public class StatisticFragment extends Fragment {
    Context mCallback;
    int mUserId;

    private TextView postTxt;
    private TextView conversationTxt;
    private TextView joinedConversationTxt;
    private TextView earliestPostTxt;
    private TextView joinBBSTxt;
    private TextView jinpinTxt;

    private final String TAG = "StatisticsFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = getArguments().getInt("userId");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.homepage_statistics, container, false);
        final int[] intStats = new int[3];
        final String[] strStats = new String[3];

        postTxt = (TextView) view.findViewById(R.id.postTxt);
        conversationTxt = (TextView) view.findViewById(R.id.conversationTxt);
        joinedConversationTxt = (TextView) view.findViewById(R.id.joinedConversationTxt);
        earliestPostTxt = (TextView) view.findViewById(R.id.earliestPostTxt);
        joinBBSTxt = (TextView) view.findViewById(R.id.joinBBSTxt);
        jinpinTxt = (TextView) view.findViewById(R.id.jinpinTxt);

        new MyOkHttp.MyOkHttpClient()
                .get(Constants.GET_STATISTICS_URL + mUserId)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Toast.makeText(mCallback, R.string.network_err, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(Call call, Response response, String responseStr) throws IOException {
                        response.body().close();
                        Pattern pattern = Pattern.compile("<div>(\\\\n)?(.*?)<");
                        Matcher matcher = pattern.matcher(responseStr);
                        if(matcher.find()){
                            intStats[0] = Integer.parseInt(matcher.group(2));
                            postTxt.setText(String.valueOf(intStats[0]));
                        }
                        if(matcher.find()){
                            intStats[1] = Integer.parseInt(matcher.group(2));
                            conversationTxt.setText(String.valueOf(intStats[1]));
                        }
                        if (matcher.find()) {
                            intStats[2] = Integer.parseInt(matcher.group(2));
                            joinedConversationTxt.setText(String.valueOf(intStats[2]));
                        }
                        if (matcher.find()) {
                            strStats[0] = unicodeToString(matcher.group(2));
                            earliestPostTxt.setText(strStats[0]);
                        }
                        if (matcher.find()) {
                            strStats[1] = unicodeToString(matcher.group(2));
                            joinBBSTxt.setText(strStats[1]);
                        }
                        if (matcher.find()) {
                            strStats[2] = unicodeToString(matcher.group(2));
                            jinpinTxt.setText(strStats[2]);
                        }
                    }
                });
        return view;
    }


    public static String unicodeToString(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), ch + "");
        }
        return str;
    }
}


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivityHomepageBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.daquexian.chaoli.forum.viewmodel.HomepageVM;

/**
 * Activity for user's homepage
 * Created by daquexian on 16-4-14.
 */

public class HomepageActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener{
    final String TAG = "HomePageActivity";
    final Context mContext = this;

    final int MENU_SETTING = 0;
    final int MENU_LOGOUT = 1;

    final int SETTING_CODE = 0;

    AppBarLayout mAppBarLayout;
    ViewPager mViewPager;
    TabLayout mTabLayout;

    ProgressDialog progressDialog;

    HistoryFragment mHistoryFragment;
    StatisticFragment mStatisticFragment;
    HistoryFragment mNotificationFragment;

    HomepageVM viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        if(bundle == null){
            Log.e(TAG, "bundle can't be null");
            this.finish();
            return;
        }

        viewModel = new HomepageVM(bundle.getString("username", ""), bundle.getString("signature", null), bundle.getString("avatarSuffix", Constants.NONE), bundle.getInt("userId", -1),
                bundle.getBoolean("isSelf", false));
        setViewModel(viewModel);

        /*if("".equals(mUsername) || mUserId == -1){
            this.finish();
            return;
        }*/

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(viewModel.username.get());
        collapsingToolbarLayout.setExpandedTitleGravity(0x01 | 0x50);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //final SwipyRefreshLayout mSwipyRefreshLayout = (SwipyRefreshLayout) findViewById(R.id.mSwipyRefreshLayout);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        mViewPager.setAdapter(new MyViewPagerAdapter(getSupportFragmentManager()));
        mViewPager.setOffscreenPageLimit(2);
        mTabLayout.addTab(mTabLayout.newTab().setText(viewModel.isSelf.get() ? R.string.notification : R.string.activity));
        mTabLayout.addTab(mTabLayout.newTab().setText(viewModel.isSelf.get() ? R.string.activity : R.string.statistics));
        if(viewModel.isSelf.get()) mTabLayout.addTab(mTabLayout.newTab().setText(R.string.statistics));
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        final TabLayout.TabLayoutOnPageChangeListener listener =
                new TabLayout.TabLayoutOnPageChangeListener(mTabLayout);
        mViewPager.addOnPageChangeListener(listener);
        //mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

    }

    public class MyViewPagerAdapter extends FragmentPagerAdapter {
        MyViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    if(viewModel.isSelf.get()) {
                        return addNotificationFragment();
                    }else {
                        return addHistoryFragment();
                    }
                case 1:
                    if(viewModel.isSelf.get()) {
                        return addHistoryFragment();
                    }else {
                        return addStatisticFragment();
                    }
                case 2:
                    return addStatisticFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return viewModel.isSelf.get() ? 3 : 2;
        }

        private Fragment addNotificationFragment() {
            mNotificationFragment = new HistoryFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("type", HistoryFragmentVM.TYPE_NOTIFICATION);
            mNotificationFragment.setArguments(bundle);
            return mNotificationFragment;
        }
        private Fragment addHistoryFragment() {
            mHistoryFragment = new HistoryFragment();
            Bundle args0 = new Bundle();
            args0.putInt("type", HistoryFragmentVM.TYPE_ACTIVITY);
            args0.putInt("userId", viewModel.userId.get());
            args0.putString("username", viewModel.username.get());
            args0.putString("avatarSuffix", viewModel.avatarSuffix.get());
            mHistoryFragment.setArguments(args0);
            return mHistoryFragment;
        }

        private Fragment addStatisticFragment() {
            mStatisticFragment = new StatisticFragment();
            Bundle args1 = new Bundle();
            args1.putInt("userId", viewModel.userId.get());
            mStatisticFragment.setArguments(args1);
            return mStatisticFragment;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if(viewModel.isSelf.get()) {
            menu.add(Menu.NONE, Menu.NONE, MENU_SETTING, R.string.config).setIcon(R.drawable.ic_account_settings_variant).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(Menu.NONE, Menu.NONE, MENU_LOGOUT, R.string.logout).setIcon(R.drawable.ic_logout).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getOrder()){
            case MENU_SETTING:
                //startActivity(new Intent(HomepageActivity.this, SettingsActivity.class));
                startActivityForResult(new Intent(HomepageActivity.this, SettingsActivity.class), SETTING_CODE);
                break;
            case MENU_LOGOUT:
                // showProcessDialog(getString(R.string.just_a_sec));
                Toast.makeText(getApplicationContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomepageActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                LoginUtils.logout(new LoginUtils.LogoutObserver() {
                    @Override
                    public void onLogoutSuccess() {
                        // dismissProcessDialog();
                    }

                    @Override
                    public void onLogoutFailure(int statusCode) {
                        dismissProcessDialog();
                        showToast(R.string.network_err);
                    }
                });

                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SETTING_CODE:
                if (resultCode == RESULT_OK) viewModel.updateSelfProfile();
                break;
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        //The Refresh must be only active when the offset is zero :
        mHistoryFragment.setRefreshEnabled(i == 0);
        if(mNotificationFragment != null) mNotificationFragment.setRefreshEnabled(i == 0);
    }

    public void showProcessDialog(String str) {
        progressDialog = ProgressDialog.show(this, "", str);
    }

    public void dismissProcessDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAppBarLayout.removeOnOffsetChangedListener(this);
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (HomepageVM) viewModel;
        ActivityHomepageBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_homepage);
        binding.setViewModel(this.viewModel);
    }
}

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivitySettingsBinding;
import com.daquexian.chaoli.forum.utils.AccountUtils;
import com.daquexian.chaoli.forum.meta.AvatarView;

import java.io.File;

import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.SettingsVM;

/**
 * Created by jianhao on 16-3-12.
 */
public class SettingsActivity extends BaseActivity implements AccountUtils.GetProfileObserver{
    private static final String TAG = "SettingsActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private final String IMAGE_TYPE = "image/*";
    private final int IMAGE_CODE = 0;   //这里的IMAGE_CODE是自己任意定义的

    SettingsVM viewModel;

    ProgressDialog progressDialog;

    Context mContext;
    AvatarView avatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();

        viewModel.showProcessDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    progressDialog = ProgressDialog.show(mContext, "", getString(R.string.just_a_sec));
                } else {
                    if (progressDialog != null) progressDialog.dismiss();
                }
            }
        });

        viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showToast(viewModel.toastContent.get());
            }
        });

        viewModel.goToAlbum.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                goToAlbum();
            }
        });

        viewModel.complete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String selectedPath = getPath(selectedImage);
            Log.d(TAG, "onActivityResult: " + selectedPath);
            File selectedFile = new File(selectedPath);
            viewModel.avatarFile = selectedFile;
            ((AvatarView) findViewById(R.id.iv_avatar)).update(selectedFile);
        }
    }

    @Override
    public void onGetProfileSuccess() {
        updateViews();
    }

    @Override
    public void onGetProfileFailure() {

    }

    private void init() {
        setViewModel(new SettingsVM(Me.getMySignature(), Me.getMyStatus(), Me.getMyPrivateAdd(), Me.getMyStarOnReply(), Me.getMyStarPrivate(), Me.getMyHideOnline()));
        configToolbar(R.string.settings);

        mContext = this;
        avatar = (AvatarView)findViewById(R.id.iv_avatar);

        updateViews();
    }

    public void updateViews(){
        avatar.update(Me.getAvatarSuffix(), Me.getMyUserId(), Me.getUsername());
        //private_add_chk.setChecked(Me.getPreferences().getPrivateAdd());
        //star_on_reply_chk.setChecked(Me.getPreferences().getStarOnReply());
        //star_private_chk.setChecked(Me.getPreferences().getStarPrivate());
        //hide_online_chk.setChecked(Me.getPreferences().getHideOnline());
        //signature_edtTxt.setText(Me.getPreferences().getSignature());
        //user_status_edtTxt.setText(Me.getStatus());
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        // this is our fallback here
        return uri.getPath();
    }

    /* Get the real path from the URI */
    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    public void goToAlbum() {
        //Intent getAlbum = new Intent(Intent.ACTION_GET_CONTENT);
        //getAlbum.setType(IMAGE_TYPE);
        //startActivityForResult(Intent.createChooser(getAlbum, "Select Picture"), IMAGE_CODE);
        if (Build.VERSION.SDK_INT >= 23){
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(SettingsActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(SettingsActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                ActivityCompat.requestPermissions(SettingsActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }else {

            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, IMAGE_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, IMAGE_CODE);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
        }
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (SettingsVM) viewModel;
        ActivitySettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        binding.setViewModel(this.viewModel);
    }
}


import android.app.Activity;
import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.widget.Toast;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivityAnswerQuestionsBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.utils.SignUpUtils;

import com.daquexian.chaoli.forum.viewmodel.AnswerQuestionsVM;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

/**
 * 注册时显示问题、回答问题的Activity
 * Created by jianhao on 16-3-28.
 */
public class AnswerQuestionsActivity extends BaseActivity {
    Context mContext;
    Boolean isFirst = true;

    AnswerQuestionsVM viewModel;

    private ProgressDialog mProcessDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        init();

        configToolbar(R.string.answer_quesiton);
    }

    private void init(){
        setViewModel(new AnswerQuestionsVM());
        ActivityAnswerQuestionsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_answer_questions);
        binding.setViewModel(this.viewModel);

        binding.questionRv.setLayoutManager(new LinearLayoutManager(mContext));
        binding.questionRv.setNestedScrollingEnabled(false);

        final String[] subjectTagsArr = SignUpUtils.subjectTags.keySet().toArray(new String[SignUpUtils.subjectTags.keySet().size()]);
        new AlertDialog.Builder(this).setTitle("请选择科目，综合类测试为6道题，至少需答对4道，分科测试为8道题，至少需答对6道。加油！")
                .setItems(subjectTagsArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //SignUpUtils.getQuestionObjList((SignUpUtils.SubmitObserver) mContext, SignUpUtils.subjectTags.get(subjectTagsArr[which]));
                        String subject = SignUpUtils.subjectTags.get(subjectTagsArr[which]);
                        viewModel.getQuestions(subject);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();

        viewModel.showDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) mProcessDialog = ProgressDialog.show(AnswerQuestionsActivity.this, "", getString(R.string.just_a_sec));
                else mProcessDialog.dismiss();
            }
        });

        viewModel.pass.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Toast.makeText(getApplicationContext(), "恭喜,答题通过", Toast.LENGTH_SHORT).show();
                ChaoliApplication.getSp().edit().putString(Constants.INVITING_CODE_SP, viewModel.code).apply();
                Bundle bundle = new Bundle();
                bundle.putString("inviteCode", viewModel.code);
                Intent intent = new Intent();
                intent.putExtras(bundle);
                intent.setClass(AnswerQuestionsActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        viewModel.fail.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                new AlertDialog.Builder(mContext).setMessage(R.string.you_dont_answer_enough_quesitions_correctly)
                        .setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isFirst = false;
                                init();
                            }
                        }).setNegativeButton(R.string.dont_try_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((Activity)mContext).finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }
    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (AnswerQuestionsVM) viewModel;
    }
}


import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;

/**
 * Created by jianhao on 16-5-31.
 * base class of all activity classes
 */
public abstract class BaseActivity extends AppCompatActivity implements IView {
    public void configToolbar(String title){
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public Toolbar getToolbar() {
        return (Toolbar) findViewById(R.id.tl_custom);
    }
    public void configToolbar(int resId){
        configToolbar(getString(resId));
    }

    public void showToast(int strId){
        Toast.makeText(this, strId, Toast.LENGTH_SHORT).show();
    }

    public void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

}


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ReplyActionBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.meta.SFXParser3;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.ReplyActionVM;

import java.util.ArrayList;
import java.util.List;


public class ReplyAction extends BaseActivity
{
	public static final String TAG = "ReplyAction";

	public static final int FLAG_NORMAL = 0;
	public static final int FLAG_REPLY = 1;
	public static final int FLAG_EDIT = 2;

	private static final int MENU_REPLY = 2;
	private static final int MENU_DEMO = 1;

	private List<View> expressionsIVList = new ArrayList<>();

	private ReplyActionVM viewModel;
	private ReplyActionBinding binding;

	private ProgressDialog progressDialog;

	/* 切换至演示模式时保存光标位置，切换回普通模式时恢复 */
	private int selectionStart, selectionEnd;

	private BottomSheetBehavior behavior;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		int flag;
		int conversationId, postId;
		String replyTo;
		String replyMsg;
		Bundle data = getIntent().getExtras();
		flag = data.getInt("flag");
		conversationId = data.getInt("conversationId");
		postId = data.getInt("postId", -1);
		replyTo = data.getString("replyTo", "");
		replyMsg = data.getString("replyMsg", "");

		//setContentView(R.layout.reply_action);
		setViewModel(new ReplyActionVM(flag, conversationId, postId, replyTo, replyMsg));

		Toolbar toolbar = (Toolbar) findViewById(R.id.tl_custom);
		toolbar.setTitle(R.string.reply);
		toolbar.setTitleTextColor(getResources().getColor(R.color.white));
		setSupportActionBar(toolbar);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		binding.replyText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				Log.d(TAG, "afterTextChanged() called with: editable = [" + editable + "]");
				viewModel.doAfterContentChanged();
			}
		});

		viewModel.replyComplete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				setResult(RESULT_OK);
				finish();
			}
		});

		binding.replyText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				tryToShowSoftKeyboard(view);
			}
		});
		viewModel.editComplete.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				finish();
			}
		});

		viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(viewModel.toastContent.get());
			}
		});

		viewModel.updateRichText.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
			}
		});

		viewModel.demoMode.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) {
					selectionStart = binding.replyText.getSelectionStart();
					selectionEnd = binding.replyText.getSelectionEnd();
					binding.replyText.setEnabled(false);
					binding.replyText.setOnlineImgEnabled(true);
					binding.replyText.update();
				} else {
					binding.replyText.setEnabled(true);
					binding.replyText.setOnlineImgEnabled(false);
					binding.replyText.update();
					binding.replyText.setSelection(selectionStart, selectionEnd);
				}
			}
		});

		viewModel.showWelcome.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				new AlertDialog.Builder(ReplyAction.this).setMessage(R.string.welcome_to_demo_mode)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int i) {

							}
						})
						.show();
			}
		});

		viewModel.showDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) progressDialog = ProgressDialog.show(ReplyAction.this, "", getString(R.string.just_a_sec));
				else progressDialog.dismiss();
			}
		});

		/**
		 * 让各个表情按钮响应单击事件
		 */

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
					for (int j = 0; j < Constants.icons.length; j++) {
						String icon = Constants.icons[j];
						CharSequence contentDescription =  view.getContentDescription();
						if (icon.equals(contentDescription)) {
							int start = Math.max(binding.replyText.getSelectionStart(), 0);
							int end = Math.max(binding.replyText.getSelectionEnd(), 0);
							binding.replyText.getText().replace(Math.min(start, end), Math.max(start, end),
									Constants.iconStrs[j], 0, Constants.iconStrs[j].length());
							updateRichText();
							break;
						}
					}
				}
			}
		};

		final ViewGroup expressions = (ViewGroup) ((ViewGroup) binding.expressions).getChildAt(0);
		for (int i = 0; i < expressions.getChildCount(); i++) {
			ViewGroup subView = (ViewGroup) expressions.getChildAt(i);
			for (int j = 0; j < subView.getChildCount(); j++) {
				View expressionView = subView.getChildAt(j);
				expressionsIVList.add(expressionView);
				expressionView.setAlpha(Constants.MIN_EXPRESSION_ALPHA);
				expressionView.setOnClickListener(onClickListener);
			}
		}

		behavior = BottomSheetBehavior.from(binding.bottomSheet);
		behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				//bottomSheetState = newState;
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
				for (View expressionView : expressionsIVList) {
					expressionView.setAlpha(Constants.MIN_EXPRESSION_ALPHA + slideOffset * (1 - Constants.MIN_EXPRESSION_ALPHA));
				}
			}
		});

        binding.replyText.requestFocus();
        tryToShowSoftKeyboard(binding.replyText);
	}

	private void updateRichText() {
		int selectionStart = binding.replyText.getSelectionStart();
		int selectionEnd = binding.replyText.getSelectionEnd();
		binding.replyText.setText(SFXParser3.parse(getApplicationContext(), viewModel.content.get(), null));
		binding.replyText.setSelection(selectionStart, selectionEnd);
	}

	private void tryToShowSoftKeyboard(View view) {
		if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED || behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Menu.NONE, MENU_DEMO, R.string.post).setIcon(R.drawable.ic_functions_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(Menu.NONE, Menu.NONE, MENU_REPLY, R.string.reply).setIcon(R.drawable.ic_cab_done_mtrl_alpha).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		super.onOptionsItemSelected(item);
		switch (item.getOrder())
		{
			case MENU_REPLY:
				switch (viewModel.flag.get())
				{
					case FLAG_NORMAL:
						if (!LoginUtils.isLoggedIn()){
							showToast(R.string.please_login);
							break;
						}
						viewModel.reply();
						break;
					case FLAG_EDIT:
						viewModel.edit();
						break;
				}
				break;
			case MENU_DEMO:
				viewModel.changeDemoMode();
				break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		else super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (ReplyActionVM) viewModel;
		binding = DataBindingUtil.setContentView(this, R.layout.reply_action);
		binding.setViewModel(this.viewModel);
	}
}


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.LoginActivityBinding;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.LoginActivityVM;

public class LoginActivity extends BaseActivity
{
	public static final String TAG = "LoginActivity";

	Context mContext = this;
	LoginActivityVM viewModel;
	ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setViewModel(new LoginActivityVM());

		viewModel.clickAQ.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				startActivity(new Intent(LoginActivity.this, AnswerQuestionsActivity.class));
			}
		});

		viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				showToast(viewModel.toastContent);
			}
		});

		viewModel.goToMainActivity.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				setResult(RESULT_OK);
				finish();
			}
		});

		viewModel.showProgressDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				if (((ObservableBoolean) observable).get()) {
					progressDialog = ProgressDialog.show(LoginActivity.this, "", getString(R.string.just_a_sec));
					progressDialog.show();
				} else {
					if (progressDialog != null) progressDialog.dismiss();
				}
			}
		});

		viewModel.clickSignUp.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
			@Override
			public void onPropertyChanged(Observable observable, int i) {
				final EditText inviteCodeET = new EditText(mContext);
				new AlertDialog.Builder(mContext).setTitle(R.string.please_enter_your_invite_code).setView(inviteCodeET)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(mContext, SignUpActivity.class);
								intent.putExtra("inviteCode", inviteCodeET.getText().toString());
								mContext.startActivity(intent);
							}
						}).setNegativeButton(android.R.string.cancel, null).show();
			}
		});

		if (ChaoliApplication.getSp().contains(Constants.INVITING_CODE_SP)) {
			showFirstDialog();
		}
	}

	private void showFirstDialog() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.exist_inviting_code_message)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						signUpWithExistingCode();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						showSecondDialog();
					}
				})
				.show();
	}

	private void showSecondDialog() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.exist_inviting_code_message_2)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						signUpWithExistingCode();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						ChaoliApplication.getSp().edit().remove(Constants.INVITING_CODE_SP).apply();
					}
				})
				.show();
	}

	private void signUpWithExistingCode() {
		Intent intent = new Intent(mContext, SignUpActivity.class);
		intent.putExtra("inviteCode", ChaoliApplication.getSp()
				.getString(Constants.INVITING_CODE_SP, ""));
		mContext.startActivity(intent);
	}

	@Override
	public void setViewModel(BaseViewModel viewModel) {
		this.viewModel = (LoginActivityVM) viewModel;
		LoginActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.login_activity);
		binding.setViewModel(this.viewModel);
	}
}


import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

/**
 * Created by jianhao on 16-9-20.
 */

public interface IView {
    void setViewModel(BaseViewModel viewModel);
}


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.databinding.ActivitySignUpBinding;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.SignUpVM;

/**
 * Created by jianhao on 16-4-7.
 * SignUpActivity
 */

public class SignUpActivity extends BaseActivity {
    final static String TAG = "SignUpActivity";

    Context mContext;
    String mToken;

    ProgressDialog progressDialog;

    SignUpVM viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_sign_up);
        mContext = this;

        Bundle bundle = getIntent().getExtras();
        String inviteCode = bundle == null ? "" :bundle.getString("inviteCode", "");

        if("".equals(inviteCode)){
            Toast.makeText(getApplicationContext(), R.string.you_can_only_sign_up_with_an_invite_code, Toast.LENGTH_SHORT).show();
            finish();
        }

        setViewModel(new SignUpVM(inviteCode));
        viewModel.init();
        configToolbar(R.string.sign_up);

        viewModel.showToast.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                showToast(viewModel.toastContent.get());
            }
        });

        viewModel.showProcessDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    progressDialog = ProgressDialog.show(mContext, "", getString(R.string.just_a_sec));
                } else {
                    if (progressDialog != null) progressDialog.dismiss();
                }
            }
        });

        viewModel.signUpSuccess.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                signUpSuccess();
            }
        });
    }

    private void signUpSuccess() {
        Toast.makeText(getApplicationContext(), R.string.sign_up_successfully, Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.setClass(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);   //清除所在栈所有Activity
        startActivity(intent);
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (SignUpVM) viewModel;
        ActivitySignUpBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up);
        binding.setViewModel(this.viewModel);
    }
}


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

@Deprecated
public class Splash extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//TODO Any splash screen here?
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//Toast.makeText(Splash.this, String.format(Locale.getDefault(), "%.2f%%", TODO.getStatus() * 100), Toast.LENGTH_SHORT).show();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				super.run();
				try
				{
					sleep(0);
					startActivity(new Intent(Splash.this, Class.forName("com.geno.chaoli.forum.view.MainActivity")));
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					finish();
				}
			}
		};
		t.start();
	}
}


import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.databinding.ObservableBoolean;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.data.Me;
import com.daquexian.chaoli.forum.databinding.HomepageHistoryBinding;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;
import com.daquexian.chaoli.forum.viewmodel.HistoryFragmentVM;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

/**
 * Created by jianhao on 16-6-5.
 */

public class HistoryFragment extends Fragment implements IView, SwipyRefreshLayout.OnRefreshListener {
    //String mUsername, mAvatarSuffix;
    //int mUserId;
    SwipyRefreshLayout mSwipyRefreshLayout;
    Boolean bottom = true;
    Context activityContext;
    int type;

    private static final String TAG = "HistoryFragment";
    private HistoryFragmentVM viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        type = arguments.getInt("type");
        if (type == HistoryFragmentVM.TYPE_ACTIVITY) {
            viewModel = new HistoryFragmentVM(type,
                    arguments.getInt("userId"),
                    arguments.getString("username"),
                    arguments.getString("avatarSuffix"));
        } else if (type == HistoryFragmentVM.TYPE_NOTIFICATION) {
            viewModel = new HistoryFragmentVM(type, Me.getMyUserId(), Me.getMyUsername(), Me.getMyAvatarSuffix());
        } else {
            throw new RuntimeException("type can only be 0 or 1");
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSwipyRefreshLayout = (SwipyRefreshLayout) inflater.inflate(R.layout.homepage_history, container, false);

        mSwipyRefreshLayout.setDirection(type == HistoryFragmentVM.TYPE_ACTIVITY ? SwipyRefreshLayoutDirection.BOTH : SwipyRefreshLayoutDirection.TOP);
        mSwipyRefreshLayout.setOnRefreshListener(this);

        setViewModel(viewModel);

        onRefresh(SwipyRefreshLayoutDirection.TOP);

        RecyclerView rvHistory = (RecyclerView) mSwipyRefreshLayout.findViewById(R.id.rvHomepageItems);
        rvHistory.setLayoutManager(new LinearLayoutManager(activityContext));
        //rvHistory.setAdapter(myAdapter);

        rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //得到当前显示的最后一个item的view
                View lastChildView = recyclerView.getLayoutManager().getChildAt(recyclerView.getLayoutManager().getChildCount()-1);
                //得到lastChildView的bottom坐标值
                int lastChildBottom = lastChildView.getBottom();
                //得到Recyclerview的底部坐标减去底部padding值，也就是显示内容最底部的坐标
                int recyclerBottom =  recyclerView.getBottom()-recyclerView.getPaddingBottom();
                //通过这个lastChildView得到这个view当前的position值
                int lastVisiblePosition  = recyclerView.getLayoutManager().getPosition(lastChildView);

                //判断lastChildView的bottom值跟recyclerBottom
                //判断lastPosition是不是最后一个position
                //如果两个条件都满足则说明是真正的滑动到了底部
				/* Why <= ? */
                int lastPosition = recyclerView.getLayoutManager().getItemCount() - 1;

                if(lastChildBottom == recyclerBottom && lastPosition == recyclerView.getLayoutManager().getItemCount()-1 ){
                    bottom = true;
                    mSwipyRefreshLayout.setEnabled(true);
                }else{
                    bottom = false;
                }

                if (lastVisiblePosition >= lastPosition - 3) viewModel.tryToLoadMore();
            }
        });

        viewModel.showProgressDialog.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if (((ObservableBoolean) observable).get()) {
                    ((HomepageActivity) activityContext).showProcessDialog(ChaoliApplication.getAppContext().getString(R.string.just_a_sec));
                } else {
                    ((HomepageActivity) activityContext).dismissProcessDialog();
                }
            }
        });

        viewModel.goToPost.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                Intent intent = new Intent(activityContext, PostActivity.class);
                intent.putExtra("conversationId", viewModel.intendedConversationId.get());
                intent.putExtra("conversationTitle", viewModel.intendedConversationTitle.get());
                if (viewModel.intendedConversationPage.get() != -1) intent.putExtra("page", viewModel.intendedConversationPage.get());
                startActivity(intent);
            }
        });

        return mSwipyRefreshLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activityContext = context;
    }

    @Override
    public void onRefresh(SwipyRefreshLayoutDirection direction) {
        if (direction == SwipyRefreshLayoutDirection.TOP) {
            viewModel.refresh();
        } else {
            viewModel.loadMore();
        }
    }

    public void setRefreshEnabled(Boolean enabled){
        mSwipyRefreshLayout.setEnabled(enabled || bottom);
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        this.viewModel = (HistoryFragmentVM) viewModel;
        //this.viewModel.setUrl(Constants.GET_ACTIVITIES_URL + mUserId);
        HomepageHistoryBinding binding = DataBindingUtil.bind(mSwipyRefreshLayout);
        binding.setViewModel(this.viewModel);
    }
}


import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.utils.LoginUtils;
import com.daquexian.chaoli.forum.viewmodel.BaseViewModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry point of whole application
 * parse link(if there is one) and distribute to corresponding activity, or start MainActivity directly
 * Created by daquexian on 17-1-28.
 */

public class EntryPointActivity extends BaseActivity {
    private static final String TAG = "EntryPointActivity";

    private static final String BASE_PATTERN = "https://(www.)?chaoli.club/(index.php)?$";
    private static final String CONVERSATION_PATTERN = "https://(www.)?chaoli.club/index.php/(\\d+)$";
    private static final String HOMEPAGE_PATTERN = "https://(www.)?chaoli.club/index.php/member/(\\d+)$";

    private static final int REQUEST_PERMISSION_CODE = 1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_point);

        configToolbar(R.string.app_name);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.need_write_permission))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(EntryPointActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                        }
                    })
                    .show();
        } else {
            core();
        }

    }

    private void core() {
        Intent intent = getIntent();
        Uri uri = intent.getData();

        if (uri == null) {
            Intent goToMainActivity = new Intent(this, MainActivity.class);
            startActivityWithoutAnimation(goToMainActivity);
        } else {
            final String url = uri.toString();
            Log.d(TAG, "onCreate: url = " + url);

            final Pattern[] pattern = {Pattern.compile(BASE_PATTERN)};
            final Matcher[] matcher = {pattern[0].matcher(url)};

            if (matcher[0].find()) {
                Intent goToMainActivity = new Intent(this, MainActivity.class);
                startActivityWithoutAnimation(goToMainActivity);
            } else {
                final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl);
                swipeRefreshLayout.setRefreshing(true);

                LoginUtils.begin_login(new LoginUtils.LoginObserver() {
                    @Override
                    public void onLoginSuccess(int userId, String token) {

                        pattern[0] = Pattern.compile(CONVERSATION_PATTERN);
                        matcher[0] = pattern[0].matcher(url);

                        if (matcher[0].find()) {
                            String conversationId = matcher[0].group(2);
                            Intent goToPostActivity = new Intent(EntryPointActivity.this, PostActivity.class);
                            goToPostActivity.putExtra("conversationId", Integer.parseInt(conversationId));
                            startActivityWithoutAnimation(goToPostActivity);
                        } else {
                            showToast(R.string.not_support_temporarily);
                            finish();
                        }

                /*
                pattern = Pattern.compile(HOMEPAGE_PATTERN);
                matcher = pattern.matcher(url);

                if (matcher.find()) {
                    String memberId = matcher.group(2);
                    Intent goToHomepageActivity = new Intent(EntryPointActivity.this, HomepageActivity.class);

                }*/
                    }

                    @Override
                    public void onLoginFailure(int statusCode) {
                        swipeRefreshLayout.setRefreshing(false);
                        showToast(R.string.network_err);
                        finish();
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        core();
    }

    private void startActivityWithoutAnimation(Intent intent) {
        // getWindow().setWindowAnimations(0);
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    @Override
    public void setViewModel(BaseViewModel viewModel) {
        // empty method
    }
}

