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
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
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

  @Mock
  private InstitutionService institutionService;

  private final InputStream inputStream = mock(InputStream.class);

  private final FormDataContentDisposition contentDisposition = mock(
      FormDataContentDisposition.class);


  private DaaService service;

  private void initService() {
    service = new DaaService(daaServiceDAO, daaDAO, gcsService, emailService, institutionService);
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
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    Institution institution = mock(Institution.class);
    when(institution.getId()).thenReturn(1);
    when(institution.getSigningOfficials()).thenReturn(List.of());

    when(institutionService.findAllInstitutions()).thenReturn(
        Collections.singletonList(institution));

    initService();

    DaaService daaSpy = spy(service);
    assertDoesNotThrow(() -> daaSpy.sendDaaRequestEmails(user, 1));
    verify(daaSpy, times(1)).sendDaaRequestEmails(any(), any());
    verify(emailService, times(0)).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendDaaRequestEmailsWithSigningOfficials() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    Institution institution = mock(Institution.class);
    when(institution.getId()).thenReturn(1);
    when(institution.getSigningOfficials()).thenReturn(List.of(signingOfficial));

    when(institutionService.findAllInstitutions()).thenReturn(Collections.singletonList(institution));

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("daaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    doNothing().when(emailService).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());

    initService();

    DaaService daaSpy = spy(service);
    assertDoesNotThrow(() -> daaSpy.sendDaaRequestEmails(user, 1));
    verify(daaSpy, times(1)).sendDaaRequestEmails(any(), any());
    verify(emailService, times(1)).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendDaaRequestEmailsWithMultipleSigningOfficials() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    SimplifiedUser signingOfficial2 = mock(SimplifiedUser.class);
    signingOfficial2.displayName = "Official Name2";
    signingOfficial2.email = "official2@example.com";

    Institution institution = mock(Institution.class);
    when(institution.getId()).thenReturn(1);
    when(institution.getSigningOfficials()).thenReturn(List.of(signingOfficial, signingOfficial2));

    when(institutionService.findAllInstitutions()).thenReturn(Collections.singletonList(institution));

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("daaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    doNothing().when(emailService).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());

    initService();

    DaaService daaSpy = spy(service);
    assertDoesNotThrow(() -> daaSpy.sendDaaRequestEmails(user, 1));
    verify(daaSpy, times(1)).sendDaaRequestEmails(any(), any());
    verify(emailService, times(2)).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testFindFileById() {
    BlobId blobId = mock(BlobId.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getBlobId()).thenReturn(blobId);

    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setFile(file);

    String fileContent = RandomStringUtils.randomAlphanumeric(10);
    InputStream is = new ByteArrayInputStream((fileContent).getBytes());

    when(daaDAO.findById(any())).thenReturn(daa);

    initService();
    assertDoesNotThrow(() -> service.findFileById(1));
  }

  @Test
  void testFindFileByIdInvalidDaaId() {
    BlobId blobId = mock(BlobId.class);
    FileStorageObject file = mock(FileStorageObject.class);

    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setFile(file);

    String fileContent = RandomStringUtils.randomAlphanumeric(10);
    InputStream is = new ByteArrayInputStream((fileContent).getBytes());

    when(daaDAO.findById(any())).thenThrow(new NotFoundException("not found"));

    initService();
    assertThrows(NotFoundException.class, () -> service.findFileById(1));
  }

  @Test
  void testFindFileByIdNullFile() {
    DataAccessAgreement daa = new DataAccessAgreement();

    when(daaDAO.findById(any())).thenReturn(daa);

    initService();
    assertThrows(NotFoundException.class, () -> service.findFileById(1));
  }
}
