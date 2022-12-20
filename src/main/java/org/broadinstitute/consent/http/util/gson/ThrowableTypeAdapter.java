package org.broadinstitute.consent.http.util.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class ThrowableTypeAdapter
        implements JsonSerializer<Throwable> {
    @Override
    public JsonElement serialize(Throwable src, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
