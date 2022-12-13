package org.broadinstitute.consent.http.models;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.time.Instant;

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
        assertEquals(fso.getCreateDate().toString(), fsoJsonObject.get("createDate").getAsString());
        assertTrue(fsoJsonObject.has("fileName"));
        assertEquals(fso.getFileName(), fsoJsonObject.get("fileName").getAsString());
        assertTrue(fsoJsonObject.has("category"));
        assertEquals(fso.getCategory().getValue(), fsoJsonObject.get("category").getAsString());
    }
}
