package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.BlobId;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlobIdTypeAdapterTest {

  @Test
  void testBlobIdTypeAdapter() {
    BlobId randomId = BlobId.of(
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10));
    String randomIdUri = randomId.toGsUtilUri();
    boolean failsSerialization = false;
    boolean failsDeserialization = false;
    BlobIdTypeAdapter adapter = new BlobIdTypeAdapter();

    try {
      JsonElement elem = adapter.serialize(randomId, null, null);
    } catch (RuntimeException rte) {
      failsSerialization = true;
    }
    assertTrue(failsSerialization);
    JsonPrimitive primitive = new JsonPrimitive(randomIdUri);
    try {
      BlobId returnedId = adapter.deserialize(primitive, null, null);
    } catch (RuntimeException rte) {
      failsDeserialization = true;
    }

    assertTrue(failsDeserialization);
  }
}
