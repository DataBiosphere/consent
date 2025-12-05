package org.broadinstitute.consent.http.util.gson.deserializer;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.broadinstitute.consent.http.models.Presentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PresentationDeserializerTest {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson =
        new GsonBuilder()
            .registerTypeAdapter(Presentation.class, new PresentationDeserializer())
            .create();
  }

  @Test
  void testDeserializeV1_MigratesLinkToUrl() {
    String v1Json = "{\"link\":\"https://example.com/presentation\",\"title\":\"My Presentation\"}";

    Presentation result = gson.fromJson(v1Json, Presentation.class);

    assertNotNull(result);
    assertEquals("https://example.com/presentation", result.url());
    assertEquals("My Presentation", result.title());
  }

  @Test
  void testDeserializeV1_WithoutLink() {
    String v1Json = "{\"title\":\"My Presentation\"}";

    Presentation result = gson.fromJson(v1Json, Presentation.class);

    assertNotNull(result);
    assertEquals("My Presentation", result.title());
    assertNull(result.url());
  }

  @Test
  void testDeserializeV2_RemainsUnchanged() {
    String v2Json =
        "{\"presentationId\":1,\"title\":\"Conference Talk\",\"url\":\"https://example.com/talk\"}";

    Presentation result = gson.fromJson(v2Json, Presentation.class);

    assertNotNull(result);
    assertEquals("1", result.presentationId());
    assertEquals("Conference Talk", result.title());
    assertEquals("https://example.com/talk", result.url());
  }

  @Test
  void testDeserializeV2_UrlTakesPrecedenceOverLink() {
    String v2Json =
        "{\"presentationId\":1,\"url\":\"https://example.com/new\",\"link\":\"https://example.com/old\"}";

    Presentation result = gson.fromJson(v2Json, Presentation.class);

    assertNotNull(result);
    assertEquals("https://example.com/new", result.url());
  }
}
