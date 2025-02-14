package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.broadinstitute.consent.http.service.dao.DaaServiceDAO;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaaServiceTest {

  @Mock
  private DaaServiceDAO daaServiceDAO;

  @Mock
  private DaaDAO daaDAO;

  @Mock
  private GCSService gcsService;

  @Mock
  private EmailService emailService;

  @Mock
  private UserService userService;

  @Mock
  private InstitutionDAO institutionDAO;

  @Mock
  private DacDAO dacDAO;

  private final InputStream inputStream = mock(InputStream.class);

  private final FormDataContentDisposition contentDisposition = mock(
      FormDataContentDisposition.class);


  private DaaService service;

  private void initService() {
    service = new DaaService(daaServiceDAO, daaDAO, gcsService, emailService, userService, institutionDAO, dacDAO);
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
  void testFindAllNoDaas() {
    when(daaDAO.findAll()).thenReturn(List.of());

    initService();
    assertEquals(List.of(), service.findAll());
  }

  @Test
  void testFindAllWithBroadDaa() {
    Dac broadDac = new Dac();
    broadDac.setName("Broad DAC");
    broadDac.setDacId(RandomUtils.nextInt(1, 10));

    Dac otherDac = new Dac();
    otherDac.setName("Other DAC");
    otherDac.setDacId(RandomUtils.nextInt(11, 20));

    DataAccessAgreement broadDAA = new DataAccessAgreement();
    broadDAA.setDaaId(RandomUtils.nextInt(1, 10));
    broadDAA.setInitialDacId(broadDac.getDacId());

    DataAccessAgreement nonBroadDAA1 = new DataAccessAgreement();
    nonBroadDAA1.setDaaId(RandomUtils.nextInt(11, 20));
    nonBroadDAA1.setInitialDacId(otherDac.getDacId());

    DataAccessAgreement nonBroadDAA2 = new DataAccessAgreement();
    nonBroadDAA2.setDaaId(RandomUtils.nextInt(21, 30));
    nonBroadDAA2.setInitialDacId(otherDac.getDacId());

    when(daaDAO.findAll()).thenReturn(List.of(broadDAA, nonBroadDAA1, nonBroadDAA2));
    when(dacDAO.findAll()).thenReturn(List.of(broadDac, otherDac));

    initService();
    List<DataAccessAgreement> foundDaas = service.findAll();

    Optional<DataAccessAgreement> foundBroadDAA = foundDaas.stream()
        .filter(d -> d.getDaaId().equals(broadDAA.getDaaId())).findFirst();
    assertTrue(foundBroadDAA.isPresent() && foundBroadDAA.get().getBroadDaa());

    Optional<DataAccessAgreement> foundNonBroadDAA1 = foundDaas.stream()
        .filter(d -> d.getDaaId().equals(nonBroadDAA1.getDaaId())).findFirst();
    assertTrue(foundNonBroadDAA1.isPresent() && !foundNonBroadDAA1.get().getBroadDaa());

    Optional<DataAccessAgreement> foundNonBroadDAA2 = foundDaas.stream()
        .filter(d -> d.getDaaId().equals(nonBroadDAA2.getDaaId())).findFirst();
    assertTrue(foundNonBroadDAA2.isPresent() && !foundNonBroadDAA2.get().getBroadDaa());
  }

  @Test
  void testSendDaaRequestEmailsNoSigningOfficials() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    Institution institution = mock(Institution.class);
    when(institution.getSigningOfficials()).thenReturn(List.of());

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(institution);

    initService();

    assertThrows(NotFoundException.class, () -> service.sendDaaRequestEmails(user, 1));
  }

  @Test
  void testSendDaaRequestEmailsUserWithoutInstitution() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(null);

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(null);

    initService();

    assertThrows(BadRequestException.class, () -> service.sendDaaRequestEmails(user, 1));
  }

  @Test
  void testSendDaaRequestEmailsWithSigningOfficials() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    Institution institution = mock(Institution.class);
    when(institution.getSigningOfficials()).thenReturn(List.of(signingOfficial));

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(institution);

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("daaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    doNothing().when(emailService).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());

    initService();

    assertDoesNotThrow(() -> service.sendDaaRequestEmails(user, 1));
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
    when(institution.getSigningOfficials()).thenReturn(List.of(signingOfficial, signingOfficial2));

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(institution);

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("daaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    doNothing().when(emailService).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());

    initService();

    assertDoesNotThrow(() -> service.sendDaaRequestEmails(user, 1));
    verify(emailService, times(2)).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendNewDaaEmails() throws Exception {
    User user = mock(User.class);

    SimplifiedUser researcher = mock(SimplifiedUser.class);
    researcher.displayName = "Official Name";
    researcher.email = "official@example.com";
    researcher.institutionId = RandomUtils.nextInt(0,50);

    SimplifiedUser researcher2 = mock(SimplifiedUser.class);
    researcher2.displayName = "Official Name2";
    researcher2.email = "official2@example.com";
    researcher2.institutionId = RandomUtils.nextInt(0,50);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    SimplifiedUser signingOfficial2 = mock(SimplifiedUser.class);
    signingOfficial2.displayName = "Official Name2";
    signingOfficial2.email = "official2@example.com";

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("previousDaaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    initService();

    when(userService.getUsersByDaaId(any())).thenReturn(List.of(researcher, researcher2));
    when(userService.findSOsByInstitutionId(any())).thenReturn(List.of(signingOfficial, signingOfficial2));
    assertDoesNotThrow(() -> service.sendNewDaaEmails(user, 1, "dacName", "newDaaName"));
    verify(emailService, times(2)).sendNewDAAUploadResearcherMessage(any(), any(), any(), any(), any(), any());
    verify(emailService, times(2)).sendNewDAAUploadSOMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendNewDaaEmailsOneResearcher() throws Exception {
    User user = mock(User.class);

    SimplifiedUser researcher = mock(SimplifiedUser.class);
    researcher.displayName = "Official Name";
    researcher.email = "official@example.com";
    researcher.institutionId = RandomUtils.nextInt(0,50);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    SimplifiedUser signingOfficial2 = mock(SimplifiedUser.class);
    signingOfficial2.displayName = "Official Name2";
    signingOfficial2.email = "official2@example.com";

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("previousDaaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    initService();

    when(userService.getUsersByDaaId(any())).thenReturn(List.of(researcher));
    when(userService.findSOsByInstitutionId(any())).thenReturn(List.of(signingOfficial, signingOfficial2));
    assertDoesNotThrow(() -> service.sendNewDaaEmails(user, 1, "dacName", "newDaaName"));
    verify(emailService, times(1)).sendNewDAAUploadResearcherMessage(any(), any(), any(), any(), any(), any());
    verify(emailService, times(2)).sendNewDAAUploadSOMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendNewDaaEmailsDAANotFound() throws Exception {
    User user = mock(User.class);

    initService();
    assertThrows(NotFoundException.class, () -> service.sendNewDaaEmails(user, 1, "dacName", "newDaaName"));
  }

  @Test
  void testSendNewDaaEmailsDAANoResearchersAndSOs() throws Exception {
    User user = mock(User.class);

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("previousDaaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);


    initService();

    when(userService.getUsersByDaaId(any())).thenReturn(List.of());
    assertDoesNotThrow(() -> service.sendNewDaaEmails(user, 1, "dacName", "newDaaName"));
    verify(emailService, times(0)).sendNewDAAUploadResearcherMessage(any(), any(), any(), any(), any(), any());
    verify(emailService, times(0)).sendNewDAAUploadSOMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendNewDaaEmailsUserWithoutInstitution() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(null);

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(null);

    initService();

    assertThrows(BadRequestException.class, () -> service.sendDaaRequestEmails(user, 1));
  }

  @Test
  void testSendNewDaaEmailsWithSigningOfficials() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    Institution institution = mock(Institution.class);
    when(institution.getSigningOfficials()).thenReturn(List.of(signingOfficial));

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(institution);

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("daaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    doNothing().when(emailService).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());

    initService();

    assertDoesNotThrow(() -> service.sendDaaRequestEmails(user, 1));
    verify(emailService, times(1)).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());
  }

  @Test
  void testSendNewDaaEmailsWithMultipleSigningOfficials() throws Exception {
    User user = mock(User.class);
    when(user.getInstitutionId()).thenReturn(1);

    SimplifiedUser signingOfficial = mock(SimplifiedUser.class);
    signingOfficial.displayName = "Official Name";
    signingOfficial.email = "official@example.com";

    SimplifiedUser signingOfficial2 = mock(SimplifiedUser.class);
    signingOfficial2.displayName = "Official Name2";
    signingOfficial2.email = "official2@example.com";

    Institution institution = mock(Institution.class);
    when(institution.getSigningOfficials()).thenReturn(List.of(signingOfficial, signingOfficial2));

    when(institutionDAO.findInstitutionWithSOById(any())).thenReturn(institution);

    DataAccessAgreement daa = mock(DataAccessAgreement.class);
    FileStorageObject file = mock(FileStorageObject.class);
    when(file.getFileName()).thenReturn("daaName");
    when(daa.getFile()).thenReturn(file);
    when(daaDAO.findById(any())).thenReturn(daa);

    doNothing().when(emailService).sendDaaRequestMessage(any(), any(), any(), any(), any(), any());

    initService();

    assertDoesNotThrow(() -> service.sendDaaRequestEmails(user, 1));
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

  private DataAccessAgreement createDaa(int daaId) {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    return daa;
  }

  @Test
  void testFindDAAsInJsonArray() {
    String json = "{daaList:[1,2,3]}";
    List <DataAccessAgreement> daaList = List.of(createDaa(0), createDaa(1), createDaa(2));
    when(daaDAO.findById(any())).thenReturn(daaList.get(0), daaList.get(1), daaList.get(2));
    initService();
    List<DataAccessAgreement> result = service.findDAAsInJsonArray(json, "daaList");
    assertEquals(3, result.size());
  }

  @Test
  void testFindDAAsInJsonArrayRemoveDuplicates() {
    String json = "{daaList:[1,1,2,3]}";
    List <DataAccessAgreement> daaList = List.of(createDaa(0), createDaa(1), createDaa(2));
    when(daaDAO.findById(any())).thenReturn(daaList.get(0), daaList.get(1), daaList.get(2));
    initService();
    List<DataAccessAgreement> result = service.findDAAsInJsonArray(json, "daaList");
    assertEquals(3, result.size());
  }

  @Test
  void testFindDAAsInJsonArrayEmptyArray() {
    String json = "{daaList:[]}";
    initService();
    List<DataAccessAgreement> result = service.findDAAsInJsonArray(json, "daaList");
    assertTrue(result.isEmpty());
  }

  @Test
  void testFindDAAsInJsonArrayInvalidJson() {
    // Missing closing bracket
    String json = "{daaList:[1,2,3}";
    initService();
    assertThrows(BadRequestException.class, () -> service.findDAAsInJsonArray(json, "daaList"));
  }

  @Test
  void testFindDAAsInJsonArrayInvalidArrayKey() {
    String json = "{daaList:[1,2,3]}";
    initService();
    assertThrows(BadRequestException.class, () -> service.findDAAsInJsonArray(json, "invalidKey"));
  }

  @Test
  void testDeleteDaa() {
    when(daaDAO.findById(any())).thenReturn(new DataAccessAgreement());
    doNothing().when(daaDAO).deleteDaa(any());
    initService();
    service.deleteDaa(1);
  }

  @Test
  void testDeleteDaaDaaNotFound() {
    initService();
    assertThrows(NotFoundException.class, () -> service.deleteDaa(1));
  }

  @Test
  void testIsBroadDAANoDaasNoDacs() {
    initService();
    assertFalse(service.isBroadDAA(RandomUtils.nextInt(0,50), List.of(), List.of()));
  }

  @Test
  void testIsBroadDAANoDacs() {
    initService();

    DataAccessAgreement daa1 = new DataAccessAgreement();
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa1.setDaaId(1);
    daa2.setDaaId(2);

    assertTrue(service.isBroadDAA(1, List.of(daa1, daa2), List.of()));
    assertFalse(service.isBroadDAA(2, List.of(daa1, daa2), List.of()));
  }

  @Test
  void testIsBroadDAANoBroadDacs() {
    initService();

    Dac dac = new Dac();
    dac.setName("dacName");

    Dac dac2= new Dac();
    dac2.setName("dacName2");

    DataAccessAgreement daa1 = new DataAccessAgreement();
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa1.setDaaId(1);
    daa2.setDaaId(2);

    assertTrue(service.isBroadDAA(1, List.of(daa1, daa2), List.of(dac, dac2)));
    assertFalse(service.isBroadDAA(2, List.of(daa1, daa2), List.of(dac, dac2)));
  }

  @Test
  void testIsBroadDAANoMatchingDaa() {
    initService();

    Dac dac = new Dac();
    dac.setName("dacName");
    dac.setDacId(1);

    Dac dac2= new Dac();
    dac2.setName("broadDac");
    dac2.setDacId(2);

    DataAccessAgreement daa1 = new DataAccessAgreement();
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa1.setDaaId(1);
    daa1.setInitialDacId(RandomUtils.nextInt(3,50));
    daa2.setDaaId(2);
    daa2.setInitialDacId(RandomUtils.nextInt(3,50));

    assertFalse(service.isBroadDAA(1, List.of(daa1, daa2), List.of(dac, dac2)));
    assertFalse(service.isBroadDAA(2, List.of(daa1, daa2), List.of(dac, dac2)));
  }

  @Test
  void testIsBroadDAAMatching() {
    initService();

    Dac dac = new Dac();
    dac.setName("dacName");
    dac.setDacId(1);

    Dac dac2= new Dac();
    dac2.setName("broadDac");
    dac2.setDacId(2);

    DataAccessAgreement daa1 = new DataAccessAgreement();
    DataAccessAgreement daa2 = new DataAccessAgreement();
    daa1.setDaaId(1);
    daa1.setInitialDacId(RandomUtils.nextInt(3,50));
    daa2.setDaaId(2);
    daa2.setInitialDacId(2);

    assertFalse(service.isBroadDAA(1, List.of(daa1, daa2), List.of(dac, dac2)));
    assertTrue(service.isBroadDAA(2, List.of(daa1, daa2), List.of(dac, dac2)));
  }

  @Test
  void testFindByDarReferenceId() {
    initService();
    DataAccessAgreement daa1 = new DataAccessAgreement();
    when(daaDAO.findByDarReferenceId(any())).thenReturn(List.of(daa1));

    List<DataAccessAgreement> daas = service.findByDarReferenceId(RandomStringUtils.randomAlphabetic(5));
    assertFalse(daas.isEmpty());
    assertTrue(daas.stream().map(DataAccessAgreement::getDaaId).toList().contains(daa1.getDaaId()));
  }

  @Test
  void testFindByDarReferenceIdNoResults() {
    initService();
    when(daaDAO.findByDarReferenceId(any())).thenReturn(List.of());

    List<DataAccessAgreement> daas = service.findByDarReferenceId(RandomStringUtils.randomAlphabetic(5));
    assertTrue(daas.isEmpty());
  }

}
