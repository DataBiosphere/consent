package org.broadinstitute.consent.http.models;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileStorageObjectTest {

    @Test
    public void testFileStorageObjectGsonSerialization() {
        FileStorageObject fso = new FileStorageObject();

        fso.setFileName("asdf");
        fso.setBlobId(BlobId.of("sensitive", "information")); // should not be serialized
        fso.setCreateDate(Instant.now());
        fso.setCategory(FileCategory.IRB_COLLABORATION_LETTER);
        fso.setUploadedFile(new ByteArrayInputStream(new byte[]{})); // should not be serialized

        Gson gson = GsonUtil.buildGson();
        JsonObject fsoJsonObject = gson.fromJson(gson.toJson(fso), JsonObject.class);

        Assertions.assertEquals(3, fsoJsonObject.size());
        Assertions.assertTrue(fsoJsonObject.has("createDate"));
        Assertions.assertEquals(fso.getCreateDate().toEpochMilli(),
            fsoJsonObject.get("createDate").getAsLong());
        Assertions.assertTrue(fsoJsonObject.has("fileName"));
        Assertions.assertEquals(fso.getFileName(), fsoJsonObject.get("fileName").getAsString());
        Assertions.assertTrue(fsoJsonObject.has("category"));
        Assertions.assertEquals(fso.getCategory().getValue(),
            fsoJsonObject.get("category").getAsString());

        // should not have these fields ever
        Assertions.assertFalse(fsoJsonObject.has("blobId"));
        Assertions.assertFalse(fsoJsonObject.has("uploadedFile"));

    }

    @Test
    public void testFileStorageObjectGsonDeserialization_no_BlobId() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("fileName", new JsonPrimitive("asdf"));
        jsonObject.add("blobId", new JsonPrimitive(BlobId.of("abcd", "hjkl").toGsUtilUri()));
        jsonObject.add("uploadedFile", new JsonPrimitive("content"));

        FileStorageObject fso = GsonUtil.buildGson().fromJson(jsonObject, FileStorageObject.class);

        Assertions.assertEquals("asdf", fso.getFileName());
        Assertions.assertNull(fso.getBlobId());
        Assertions.assertNull(fso.getUploadedFile());
    }

    @Test
    public void testFileStorageObjectDeserializationFromString_no_BlobId() {
        String json = "{\"fileName\":\"asdf\", \"invalidField\":\"bot\", \"blobId\":\"test\"}";

        FileStorageObject fso = GsonUtil.buildGson().fromJson(json, FileStorageObject.class);
        Assertions.assertNull(fso.getBlobId());
    }
}
