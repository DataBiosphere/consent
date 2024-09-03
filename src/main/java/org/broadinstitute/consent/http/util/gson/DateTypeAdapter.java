package org.broadinstitute.consent.http.util.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTypeAdapter
    implements JsonSerializer<Date>, JsonDeserializer<Date> {

  @Override
  public JsonElement serialize(Date src, Type srcType, JsonSerializationContext context) {
    return new JsonPrimitive(src.getTime());
  }

  @Override
  public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    try {
      return new Date(json.getAsLong());
    } catch (NumberFormatException e) {
      try {
        return new SimpleDateFormat("LLL dd, yyyy, hh:mm:ss a").parse(json.getAsString());
      } catch (ParseException e1) {
        throw new JsonParseException(e1.getMessage());
      }
    }
  }
}
