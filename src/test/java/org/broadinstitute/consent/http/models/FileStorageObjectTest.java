package org.broadinstitute.consent.http.models;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

        assertEquals(3, fsoJsonObject.size());
        assertTrue(fsoJsonObject.has("createDate"));
        assertEquals(fso.getCreateDate().toEpochMilli(), fsoJsonObject.get("createDate").getAsLong());
        assertTrue(fsoJsonObject.has("fileName"));
        assertEquals(fso.getFileName(), fsoJsonObject.get("fileName").getAsString());
        assertTrue(fsoJsonObject.has("category"));
        assertEquals(fso.getCategory().getValue(), fsoJsonObject.get("category").getAsString());

        // should not have these fields ever
        assertFalse(fsoJsonObject.has("blobId"));
        assertFalse(fsoJsonObject.has("uploadedFile"));

    }

    @Test
    public void testFileStorageObjectGsonDeserialization_no_BlobId() {

        JsonObject jsonObject = new JsonObject();

        jsonObject.add("fileName", new JsonPrimitive("asdf"));
        jsonObject.add("blobId", new JsonPrimitive(BlobId.of("abcd", "hjkl").toGsUtilUri()));
        jsonObject.add("uploadedFile", new JsonPrimitive("content"));

        FileStorageObject fso = GsonUtil.buildGson().fromJson(jsonObject, FileStorageObject.class);

        assertEquals("asdf", fso.getFileName());
        assertNull(fso.getBlobId());
        assertNull(fso.getUploadedFile());
    }

    @Test
    public void testFileStorageObjectDeserializationFromString_no_BlobId() {
        String json = "{\"fileName\":\"asdf\", \"invalidField\":\"bot\", \"blobId\":\"test\"}";

        FileStorageObject fso = GsonUtil.buildGson().fromJson(json, FileStorageObject.class);
        assertNull(fso.getBlobId());
    }
}
