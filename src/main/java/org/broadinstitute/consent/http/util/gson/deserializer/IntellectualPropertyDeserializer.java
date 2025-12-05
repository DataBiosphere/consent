package org.broadinstitute.consent.http.util.gson.deserializer;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.broadinstitute.consent.http.models.IntellectualProperty;

public class IntellectualPropertyDeserializer implements JsonDeserializer<IntellectualProperty> {
  @Override
  public IntellectualProperty deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    // If V1 (has intellectualPropertySummary attribute), migrate to V2
    if (jsonObject.has("intellectualPropertySummary") && !jsonObject.has("ipId")) {
      String summary = jsonObject.get("intellectualPropertySummary").getAsString();
      jsonObject.addProperty("title", summary);
      jsonObject.remove("intellectualPropertySummary");
    }

    return new Gson().fromJson(jsonObject, IntellectualProperty.class);
  }
}
