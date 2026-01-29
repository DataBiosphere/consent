package org.broadinstitute.consent.http.util.gson;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.net.URI;

public class URIAdapter implements JsonSerializer<URI>, JsonDeserializer<URI> {
  public JsonElement serialize(URI src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }

  public URI deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
    try {
      return URI.create(json.getAsString());
    } catch (Exception e) {
      throw new JsonParseException(e);
    }
  }
}
