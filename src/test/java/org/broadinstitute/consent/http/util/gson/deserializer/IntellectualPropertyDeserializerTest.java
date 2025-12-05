package org.broadinstitute.consent.http.util.gson.deserializer;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.broadinstitute.consent.http.models.IntellectualProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntellectualPropertyDeserializerTest {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson =
        new GsonBuilder()
            .registerTypeAdapter(IntellectualProperty.class, new IntellectualPropertyDeserializer())
            .create();
  }

  @Test
  void testDeserializeV1_MigratesSuccessfully() {
    String v1Json = "{\"intellectualPropertySummary\":\"Patent ABC123\"}";

    IntellectualProperty result = gson.fromJson(v1Json, IntellectualProperty.class);

    assertNotNull(result);
    assertInstanceOf(IntellectualProperty.class, result);
    assertEquals("Patent ABC123", result.title());
  }

  @Test
  void testDeserializeV2_RemainsUnchanged() {
    String v2Json = "{\"ipId\":1,\"title\":\"New Patent\"}";

    IntellectualProperty result = gson.fromJson(v2Json, IntellectualProperty.class);

    assertNotNull(result);
    assertInstanceOf(IntellectualProperty.class, result);
    assertEquals("1", result.ipId());
    assertEquals("New Patent", result.title());
  }

  @Test
  void testDeserializeV1_EmptyString() {
    String v1Json = "{\"intellectualPropertySummary\":\"\"}";

    IntellectualProperty result = gson.fromJson(v1Json, IntellectualProperty.class);

    assertNotNull(result);
    assertEquals("", result.title());
  }
}
