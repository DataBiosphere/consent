package org.broadinstitute.consent.http.util.gson;

import com.google.gson.JsonElement;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InstantTypeAdapterTest {

    @Test
    public void testInstantTypeAdapter() {
        Instant randomTime = Instant.ofEpochMilli(new Random().nextLong());
        long randomTimeMilli = randomTime.toEpochMilli();

        InstantTypeAdapter adapter = new InstantTypeAdapter();

        JsonElement elem = adapter.serialize(randomTime, null, null);
        Assertions.assertEquals(randomTimeMilli, elem.getAsLong());

        Instant returnedInstant = adapter.deserialize(elem, null, null);

        Assertions.assertEquals(randomTime, returnedInstant);
    }
}
