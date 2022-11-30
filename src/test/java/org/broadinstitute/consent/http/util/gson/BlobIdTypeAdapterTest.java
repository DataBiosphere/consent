package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.JsonElement;
import org.junit.Test;
import org.apache.commons.lang3.RandomStringUtils;

import static org.junit.Assert.assertEquals;

public class BlobIdTypeAdapterTest {

    @Test
    public void testInstantTypeAdapter() {
        BlobId randomId = BlobId.of(
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10));
        String randomIdUri = randomId.toGsUtilUri();

        BlobIdTypeAdapter adapter = new BlobIdTypeAdapter();

        JsonElement elem = adapter.serialize(randomId, null, null);
        assertEquals(randomIdUri, elem.getAsString());

        BlobId returnedId = adapter.deserialize(elem, null, null);

        assertEquals(randomId, returnedId);
    }
}
