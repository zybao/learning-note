package com.oab.gsondemo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;

/**
 * Created by bao on 2017/10/21.
 */

public class GsonUtils {
    private static Gson instance;

    public static Gson getInstance() {
        if (instance == null) {
            synchronized (GsonUtils.class) {
                if (instance == null) {
                    instance = new GsonBuilder()
                            .serializeNulls()
//                            .registerTypeAdapter(String.class, new StringSerializer())
                           .registerTypeAdapterFactory(TypeAdapters.newFactory(String.class, new StringAdapter()))
                            .create();
                }
            }
        }

        return instance;
    }
}
