package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class GsonUtilTest {
    @Test
    public void testBuildJson() {
        Gson gson = GsonUtil.buildGson();

        Vote ds = new Vote();
        ds.setDisplayName(RandomStringUtils.randomAlphanumeric(20));

        JsonObject jsonObject = gson.fromJson(gson.toJson(ds), JsonObject.class);

        assertEquals(ds.getDisplayName(), jsonObject.get("displayName").getAsString());
    }

    @Test
    public void testInstantJson() {
        Gson gson = GsonUtil.buildGson();

        Instant instant = Instant.now();

        String instantAsJsonString = gson.toJson(instant);

        assertEquals("\"" + instant.toString() + "\"", instantAsJsonString);

        Instant parsed = gson.fromJson(instantAsJsonString, Instant.class);

        assertEquals(instant, parsed);
    }

    @Test
    public void testBlobIdJson() {
        Gson gson = GsonUtil.buildGson();

        BlobId id = BlobId.of(RandomStringUtils.randomAlphabetic(20), RandomStringUtils.randomAlphabetic(20));

        String blobIdAsJsonString = gson.toJson(id);

        assertEquals("\"" + id.toGsUtilUri() + "\"", blobIdAsJsonString);

        BlobId parsed = gson.fromJson(blobIdAsJsonString, BlobId.class);

        assertEquals(id, parsed);
    }
}
