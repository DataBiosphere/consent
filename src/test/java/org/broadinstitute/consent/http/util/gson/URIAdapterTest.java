package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.net.URI;
import org.junit.jupiter.api.Test;

class URIAdapterTest {

  private final URIAdapter adapter = new URIAdapter();

  @Test
  void testSerializeValidURI() {
    URI uri = URI.create("https://example.com/test");
    JsonElement json = adapter.serialize(uri, null, null);
    assertEquals("https://example.com/test", json.getAsString());
  }

  @Test
  void testDeserializeValidURI() {
    JsonPrimitive json = new JsonPrimitive("https://example.com/test");
    URI uri = adapter.deserialize(json, null, null);
    assertEquals(URI.create("https://example.com/test"), uri);
  }

  @Test
  void testDeserializeInvalidURIThrows() {
    JsonPrimitive json = new JsonPrimitive("ht!tp://[invalid-uri]");
    assertThrows(JsonParseException.class, () -> adapter.deserialize(json, null, null));
  }
}
