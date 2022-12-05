package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class BlobIdTypeAdapter
        implements JsonSerializer<BlobId>, JsonDeserializer<BlobId> {
    @Override
    public JsonElement serialize(BlobId src, Type srcType, JsonSerializationContext context) {
        return new JsonPrimitive(src.toGsUtilUri());
    }

    @Override
    public BlobId deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            return BlobId.fromGsUtilUri(json.getAsString());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage());
        }
    }
}
