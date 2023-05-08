package org.broadinstitute.consent.http.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;

public class TimestampTypeAdapter
        implements JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
    @Override
    public JsonElement serialize(Timestamp src, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(src.getTime());
    }

    @Override
    public Timestamp deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            return new Timestamp(json.getAsLong());
        } catch (DateTimeParseException e) {
            throw new JsonParseException(e.getMessage());
        }
    }
}
