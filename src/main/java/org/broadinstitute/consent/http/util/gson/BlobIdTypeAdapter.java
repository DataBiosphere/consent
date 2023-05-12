package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * Written to be clear this is an internal that should not be exposed via JSON.
 */
public class BlobIdTypeAdapter
    implements JsonSerializer<BlobId>, JsonDeserializer<BlobId> {

  @Override
  public JsonElement serialize(BlobId src, Type srcType, JsonSerializationContext context) {
    throw new RuntimeException("This is an internal that should not be serialized.");
  }

  @Override
  public BlobId deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    throw new RuntimeException("This is an internal that should not be deserialized.");
  }
}
