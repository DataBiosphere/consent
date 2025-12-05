package org.broadinstitute.consent.http.util.gson.deserializer;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.broadinstitute.consent.http.models.Presentation;

public class PresentationDeserializer implements JsonDeserializer<Presentation> {
  @Override
  public Presentation deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    // If V1 (has V1-specific fields but not V2-specific fields), migrate to V2
    if (!jsonObject.has("presentationId")) {
      // Ensure url exists (migrate from link if present)
      if (!jsonObject.has("url") && jsonObject.has("link")) {
        jsonObject.addProperty("url", jsonObject.get("link").getAsString());
        jsonObject.remove("link");
      }
    }

    return new Gson().fromJson(jsonObject, Presentation.class);
  }
}
