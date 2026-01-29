package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalDateAdapterTest {

  private final LocalDateAdapter adapter = new LocalDateAdapter();

  @Test
  void testDeserializeValidDate() {
    JsonPrimitive json = new JsonPrimitive("2024-06-01");
    LocalDate date = adapter.deserialize(json, null, null);
    assertEquals(LocalDate.of(2024, 6, 1), date);
  }

  @Test
  void testSerializeValidDate() {
    LocalDate date = LocalDate.of(2024, 6, 1);
    JsonElement json = adapter.serialize(date, null, null);
    assertEquals("2024-06-01", json.getAsString());
  }

  @Test
  void testDeserializeInvalidDateThrows() {
    JsonPrimitive json = new JsonPrimitive("invalid-date");
    assertThrows(JsonParseException.class, () -> adapter.deserialize(json, null, null));
  }
}
