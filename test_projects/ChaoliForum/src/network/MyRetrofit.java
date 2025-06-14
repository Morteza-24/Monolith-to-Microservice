package com.daquexian.chaoli.forum.network;

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
