package org.broadinstitute.consent.http.util.gson;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

        assertEquals(Long.toString(instant.toEpochMilli()), instantAsJsonString);

        Instant parsed = gson.fromJson(instantAsJsonString, Instant.class);

        assertEquals(instant.truncatedTo(ChronoUnit.MILLIS), parsed);
    }

    @Test
    public void testBlobIdJson() {
        Gson gson = GsonUtil.buildGson();
        boolean serializationFailed = false;
        boolean deserializationFailed = false;
        BlobId id = BlobId.of(RandomStringUtils.randomAlphabetic(20), RandomStringUtils.randomAlphabetic(20));
        try {
            String blobIdAsJsonString = gson.toJson(id);
        } catch (RuntimeException rte) {
            serializationFailed = true;
        }
        assertTrue(serializationFailed);

        try {
            String json = "{\"fileName\":\"asdf\", \"invalidField\":\"bot\", \"blobId\":\"test\"}";
            BlobId parsed = gson.fromJson(json, BlobId.class);
        } catch (RuntimeException rte) {
            deserializationFailed = true;
        }
        assertTrue(deserializationFailed);
    }

    @Test
    public void testBuildJsonWithCustomObjects() {
        Gson gson = GsonUtil.buildGson();

        FileStorageObject fso = new FileStorageObject();
        fso.setCreateDate(Instant.now());
        fso.setBlobId(BlobId.of(
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(10)));
        fso.setFileName(RandomStringUtils.randomAlphanumeric(20));

        String fsoAsJsonString = gson.toJson(fso);

        JsonObject parsedJsonObj = gson.fromJson(fsoAsJsonString, JsonObject.class);

        assertEquals(fso.getCreateDate().toEpochMilli(), parsedJsonObj.get("createDate").getAsLong());
        assertNull(parsedJsonObj.get("blobId"));
        assertEquals(fso.getFileName(), parsedJsonObj.get("fileName").getAsString());

        FileStorageObject parsedFso = gson.fromJson(fsoAsJsonString, FileStorageObject.class);

        assertEquals(fso.getCreateDate().truncatedTo(ChronoUnit.MILLIS), parsedFso.getCreateDate());
        assertNull(parsedFso.getBlobId()); // should not be parsed, since transient
        assertEquals(fso.getFileName(), parsedFso.getFileName());
    }

}
