package org.broadinstitute.consent.http.util.gson;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

public class GsonUtilTest {
    @Test
    public void testBuildJson() {
        Gson gson = GsonUtil.buildGson();

        Vote ds = new Vote();
        ds.setDisplayName(RandomStringUtils.randomAlphanumeric(20));

        JsonObject jsonObject = gson.fromJson(gson.toJson(ds), JsonObject.class);

        Assertions.assertEquals(ds.getDisplayName(), jsonObject.get("displayName").getAsString());
    }

    @Test
    public void testInstantJson() {
        Gson gson = GsonUtil.buildGson();

        Instant instant = Instant.now();

        String instantAsJsonString = gson.toJson(instant);

        Assertions.assertEquals(Long.toString(instant.toEpochMilli()), instantAsJsonString);

        Instant parsed = gson.fromJson(instantAsJsonString, Instant.class);

        Assertions.assertEquals(instant.truncatedTo(ChronoUnit.MILLIS), parsed);
    }

    @Test
    public void testBlobIdJson() {
        Gson gson = GsonUtil.buildGson();
        boolean serializationFailed = false;
        boolean deserializationFailed = false;
        BlobId id = BlobId.of(RandomStringUtils.randomAlphabetic(20), RandomStringUtils.randomAlphabetic(20));
        try {
            gson.toJson(id);
        } catch (RuntimeException rte) {
            serializationFailed = true;
        }
        assertTrue(serializationFailed);

        try {
            String json = "{\"fileName\":\"asdf\", \"invalidField\":\"bot\", \"blobId\":\"test\"}";
            gson.fromJson(json, BlobId.class);
        } catch (RuntimeException rte) {
            deserializationFailed = true;
        }
        assertTrue(deserializationFailed);
    }

    @Test
    public void testBuildJsonWithCustomObjects_Serialization() {
        Gson gson = GsonUtil.buildGson();

        GsonTestObject obj = new GsonTestObject();
        Instant now = Instant.now();

        // date and instant should render the exact same time
        obj.setDate(new Date(now.toEpochMilli()));
        obj.setInstant(now);

        obj.setTransientField("should never serialize");

        String objAsJsonString = gson.toJson(obj);

        JsonObject parsedJsonObj = gson.fromJson(objAsJsonString, JsonObject.class);

        Assertions.assertEquals(2, parsedJsonObj.size());
        Assertions.assertEquals(parsedJsonObj.get("date"), parsedJsonObj.get("instant"));
        Assertions.assertEquals(obj.getDate().getTime(), parsedJsonObj.get("date").getAsLong());
        Assertions.assertEquals(obj.getInstant().truncatedTo(ChronoUnit.MILLIS).toEpochMilli(),
            parsedJsonObj.get("instant").getAsLong());

        assertFalse(parsedJsonObj.has("transientField"));
    }

    @Test
    public void testBuildJsonWithCustomObjects_Deserialization() {
        Gson gson = GsonUtil.buildGson();

        String json = """
                {
                    "transientField": "asdfasdfa",
                    "date": 123456,
                    "instant": 567890
                }
                """;

        GsonTestObject parsedObj = gson.fromJson(json, GsonTestObject.class);

        Assertions.assertEquals(123456, parsedObj.getDate().getTime());
        Assertions.assertEquals(567890, parsedObj.getInstant().toEpochMilli());
        Assertions.assertNull(parsedObj.getTransientField());
    }

}
