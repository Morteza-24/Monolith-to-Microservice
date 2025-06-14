package com.daquexian.chaoli.forum.network;

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
