package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.ServerErrorException;
import java.io.IOException;
import java.io.InputStream;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DaaServiceTest {

  @Mock
  private DaaServiceDAO daaServiceDAO;

  @Mock
  private DaaDAO daaDAO;

  @Mock
  private GCSService gcsService;

  @Mock
  private EmailService emailService;

  private final InputStream inputStream = mock(InputStream.class);

  private final FormDataContentDisposition contentDisposition = mock(
      FormDataContentDisposition.class);


  private DaaService service;

  private void initService() {
    service = new DaaService(daaServiceDAO, daaDAO, gcsService, emailService);
  }

  @Test
  void testCreateDaaWithFso() throws Exception {
    BlobId blobId = mock(BlobId.class);
    DataAccessAgreement daa = new DataAccessAgreement();
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(blobId);
    when(daaServiceDAO.createDaaWithFso(any(), any(), any())).thenReturn(1);
    when(daaDAO.findById(any())).thenReturn(daa);
    when(daaServiceDAO.createDaaWithFso(any(), any(), any())).thenReturn(1);
    when(daaDAO.findById(any())).thenReturn(daa);
    when(contentDisposition.getFileName()).thenReturn("file.txt");

    initService();
    assertDoesNotThrow(() -> service.createDaaWithFso(1, 1, inputStream, contentDisposition));
  }

  @Test
  void testCreateDaaWithFsoGCSError() throws Exception {
    doThrow(new IOException("gcs error")).when(gcsService).storeDocument(any(), any(), any());

    initService();
    ServerErrorException e = assertThrows(ServerErrorException.class,
        () -> service.createDaaWithFso(1, 1, inputStream, contentDisposition));
    assertNotNull(e);
  }

  @Test
  void testCreateDaaWithFsoDBError() throws Exception {
    when(contentDisposition.getFileName()).thenReturn("file.txt");
    doThrow(new Exception("db error")).when(daaServiceDAO).createDaaWithFso(any(), any(), any());

    initService();
    ServerErrorException e = assertThrows(ServerErrorException.class,
        () -> service.createDaaWithFso(1, 1, inputStream, contentDisposition));
    assertNotNull(e);
  }

  @Test
  void testAddDacToDaa() {
    doNothing().when(daaDAO).createDacDaaRelation(any(), any());

    initService();
    service.addDacToDaa(1, 1);
  }

  @Test
  void testRemoveDacFromDaa() {
    doNothing().when(daaDAO).deleteDacDaaRelation(any(), any());

    initService();
    service.removeDacFromDaa(1, 1);
  }

  @Test
  void testSendDaaRequestEmails() throws Exception {
    User user = mock();
    initService();

    DaaService daaSpy = spy(service);
    assertDoesNotThrow(() -> daaSpy.sendDaaRequestEmails(user, 1));
    verify(daaSpy, times(1)).sendDaaRequestEmails(any(), any());
  }
}
