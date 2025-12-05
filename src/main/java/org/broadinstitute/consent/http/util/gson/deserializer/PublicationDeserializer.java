package org.broadinstitute.consent.http.util.gson.deserializer;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.broadinstitute.consent.http.models.Publication;

public class PublicationDeserializer implements JsonDeserializer<Publication> {
  @Override
  public Publication deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = json.getAsJsonObject();

    // If V1 (has V1-specific fields but not V2-specific fields), migrate to V2
    if ((jsonObject.has("authors") || jsonObject.has("pubmedId"))
        && !jsonObject.has("publicationId")) {
      // Convert authors string to Author array (V1 had authors as string, V2 expects List<Author>)
      if (jsonObject.has("authors") && jsonObject.get("authors").isJsonPrimitive()) {
        String authorsString = jsonObject.get("authors").getAsString();
        JsonArray authorsArray = new JsonArray();
        if (authorsString != null && !authorsString.isEmpty()) {
          String[] authorNames = authorsString.split(",");
          for (String authorName : authorNames) {
            JsonObject author = new JsonObject();
            author.addProperty("name", authorName.trim());
            authorsArray.add(author);
          }
        }
        jsonObject.remove("authors");
        jsonObject.add("authors", authorsArray);
      }
      // Ensure publishedDate exists (migrate from date if present)
      if (!jsonObject.has("publishedDate") && jsonObject.has("date")) {
        jsonObject.addProperty("publishedDate", jsonObject.get("date").getAsString());
      }
    }

    return new Gson().fromJson(jsonObject, Publication.class);
  }
}
