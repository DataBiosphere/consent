package org.broadinstitute.consent.http.util.gson;

import com.google.gson.JsonElement;
import org.junit.Test;

import java.time.Instant;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class InstantTypeAdapterTest {

    @Test
    public void testInstantTypeAdapter() {
        Instant randomTime = Instant.ofEpochMilli(new Random().nextLong());
        String randomTimeAsIsoString = randomTime.toString();

        InstantTypeAdapter adapter = new InstantTypeAdapter();

        JsonElement elem = adapter.serialize(randomTime, null, null);
        assertEquals(randomTimeAsIsoString, elem.getAsString());

        Instant returnedInstant = adapter.deserialize(elem, null, null);

        assertEquals(randomTime, returnedInstant);
    }
}
