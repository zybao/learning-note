package com.oab.gsondemo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by bao on 2017/10/21.
 */

public class StringSerializer implements JsonDeserializer<String>, JsonSerializer<String> {
    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String s = jsonElement.getAsString();
        if ( "".equals(s) || "null".equals(s) ) {
            return "default" + System.currentTimeMillis();
        }

        try {
            return jsonElement.getAsString() + 1;
        } catch (JsonParseException e) {
            throw new JsonParseException("");
        }
    }


    @Override
    public JsonElement serialize(String s, Type type, JsonSerializationContext jsonSerializationContext) {

        return null;
    }
}
