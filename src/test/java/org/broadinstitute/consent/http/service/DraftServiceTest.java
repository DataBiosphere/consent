package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DraftServiceTest {

  @Mock
  private DraftServiceDAO draftServiceDAO;

  @Mock
  private GCSService gcsService;

  @Test
  public void testCreateDraft() throws SQLException {
    doThrow(new BadRequestException("Bad Request")).when(draftServiceDAO).insertDraft(any());
    DraftService draftService = new DraftService(draftServiceDAO, gcsService);
    assertThrows(BadRequestException.class, ()-> draftService.insertDraft(null));
  }

  @Test
  public void testStreamingOutput() throws SQLException, IOException {
    DraftService draftService = new DraftService(draftServiceDAO, gcsService);
    User user = new User();
    user.setEmail("test@test.com");
    user.setUserId(1);
    DraftStudyDataset studyDataset = new DraftStudyDataset("{}", user);
    StreamingOutput output = draftService.draftAsJson(studyDataset);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    output.write(byteArrayOutputStream);
    byteArrayOutputStream.close();
    Gson gson = GsonUtil.buildGson();
    StreamingDeserializer streamedData = gson.fromJson(byteArrayOutputStream.toString(),
        StreamingDeserializer.class);
    assertEquals(studyDataset.getCreateDate().getTime(), streamedData.meta.getCreateDate().getTime());
    assertEquals("{}", streamedData.document.toString());
  }

  @Test
  void testGetDraftFile() throws IOException, SQLException {
    when(gcsService.getDocument((BlobId) any()))
        .thenAnswer(inputStream -> new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)) {
        });
    DraftService draftService = new DraftService(draftServiceDAO, gcsService);
      FileStorageObject fileStorageObject = new FileStorageObject();
      fileStorageObject.setBlobId(BlobId.of(UUID.randomUUID().toString(), "test"));
      InputStream fileContents = draftService.getDraftAttachmentStream(fileStorageObject);
      assertEquals("{}",
          new String(fileContents.readAllBytes(), StandardCharsets.UTF_8));
  }

  private static class StreamingDeserializer {

    private final Object document;
    private final DraftStudyDataset meta;

    public StreamingDeserializer(String document, DraftStudyDataset meta) {
      this.document = document;
      this.meta = meta;
    }
  }
}