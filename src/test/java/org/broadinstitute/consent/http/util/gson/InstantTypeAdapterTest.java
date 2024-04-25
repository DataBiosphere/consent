package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstantTypeAdapterTest {

  @Test
  void testInstantTypeAdapter() {
    Instant randomTime = Instant.ofEpochMilli(new Random().nextLong());
    long randomTimeMilli = randomTime.toEpochMilli();

    InstantTypeAdapter adapter = new InstantTypeAdapter();

    JsonElement elem = adapter.serialize(randomTime, null, null);
    assertEquals(randomTimeMilli, elem.getAsLong());

    Instant returnedInstant = adapter.deserialize(elem, null, null);

    assertEquals(randomTime, returnedInstant);
  }
}
