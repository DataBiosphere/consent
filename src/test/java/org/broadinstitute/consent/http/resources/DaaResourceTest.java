package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaaResourceTest {

  @Mock
  private DaaService daaService;
  @Mock
  private DacService dacService;
  @Mock
  private UserService userService;
  @Mock
  private LibraryCardService libraryCardService;
  @Mock
  private EmailService emailService;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private DaaResource resource;

  @Test
  void testCreateDaaForDac_AdminCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);

    when(dacService.findById(any())).thenReturn(dac);
    when(daaService.createDaaWithFso(any(), any(), any(), any())).thenReturn(daa);
    when(userService.findUserByEmail(any())).thenReturn(admin);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    assert response.getStatus() == HttpStatus.SC_CREATED;
  }

  @Test
  void testCreateDaaForDac_ChairCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    role.setDacId(dac.getDacId());
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);
    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(daaService.createDaaWithFso(any(), any(), any(), any())).thenReturn(new DataAccessAgreement());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    assert response.getStatus() == HttpStatus.SC_CREATED;
  }

  @Test
  void testCreateDaaForDac_InvalidChairCase() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    role.setDacId(1); // Note that this will not be the DAC we provide for DAA creation
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);
    when(userService.findUserByEmail(any())).thenReturn(admin);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    assert response.getStatus() == HttpStatus.SC_FORBIDDEN;
  }

  @Test
  void testCreateDaaForDac_InvalidFile() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    admin.setRoles(List.of(role));
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(daaService.createDaaWithFso(any(), any(), any(), any())).thenThrow(new IllegalArgumentException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createDaaForDac(info, authUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail);
    System.out.println(response.getStatus());
    assert response.getStatus() == HttpStatus.SC_BAD_REQUEST;
  }

  @Test
  public void testFindAllNoDaas() {
    when(daaService.findAll()).thenReturn(Collections.emptyList());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.findAll();
    assert response.getStatus() == HttpStatus.SC_OK;
    JsonArray daas = GsonUtil.buildGson().fromJson((response.getEntity().toString()), JsonArray.class);
    assertEquals(0, daas.size());
  }

  @Test
  public void testFindAll() {
    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    when(daaService.findAll()).thenReturn(Collections.singletonList(expectedDaa));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.findAll();
    assert response.getStatus() == HttpStatus.SC_OK;
    JsonArray daas = GsonUtil.buildGson().fromJson((response.getEntity().toString()), JsonArray.class);
    assertEquals(1, daas.size());
  }

  @Test
  public void testFindAllMultipleDaas() {
    DataAccessAgreement expectedDaa1 = new DataAccessAgreement();
    DataAccessAgreement expectedDaa2 = new DataAccessAgreement();
    when(daaService.findAll()).thenReturn(List.of(expectedDaa1, expectedDaa2));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.findAll();
    assert response.getStatus() == HttpStatus.SC_OK;
    JsonArray daas = GsonUtil.buildGson()
        .fromJson((response.getEntity().toString()), JsonArray.class);
    assertEquals(2, daas.size());
  }

 @Test
  void testFindDaaByDaaId() {
    int expectedDaaId = RandomUtils.nextInt(10, 100);
    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    expectedDaa.setDaaId(expectedDaaId);
    when(daaService.findById(expectedDaaId)).thenReturn(expectedDaa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);

    Response response = resource.findById(expectedDaaId);
    assert response.getStatus() == HttpStatus.SC_OK;
    assertEquals(expectedDaa, response.getEntity());
  }

  @Test
  void testFindDaaByDaaIdInvalidId() {
    int invalidId = RandomUtils.nextInt(10, 100);
    when(daaService.findById(invalidId)).thenThrow(new NotFoundException());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);

    Response response = resource.findById(invalidId);
    assert response.getStatus() == HttpStatus.SC_NOT_FOUND;
  }

  @Test
  void testFindDaaFileByDaaId() throws IOException {
    int expectedDaaId = RandomUtils.nextInt(10, 100);
    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    expectedDaa.setDaaId(expectedDaaId);
    String fileName = RandomStringUtils.randomAlphanumeric(10) + ".txt";
    FileStorageObject fso = new FileStorageObject();
    fso.setFileName(fileName);
    expectedDaa.setFile(fso);
    String fileContent = RandomStringUtils.randomAlphanumeric(10);

    when(daaService.findFileById(expectedDaaId)).thenReturn(new ByteArrayInputStream(fileContent.getBytes()));
    when(daaService.findById(expectedDaaId)).thenReturn(expectedDaa);
    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);

    Response response = resource.findFileById(expectedDaaId);
    assert response.getStatus() == HttpStatus.SC_OK;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ((StreamingOutput) response.getEntity()).write(out);
    assertEquals(fileContent, out.toString());
  }

  @Test
  void testFindDaaFileByDaaIdInvalid() {
    int invalidId = RandomUtils.nextInt(10, 100);
    when(daaService.findFileById(invalidId)).thenThrow(new NotFoundException());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);

    Response response = resource.findFileById(invalidId);
    assert response.getStatus() == HttpStatus.SC_NOT_FOUND;
  }

  @Test
  void testFindDaaFileByDaaIdDatabaseError() {
    int expectedDaaId = RandomUtils.nextInt(10, 100);
    when(daaService.findFileById(expectedDaaId)).thenThrow(new RuntimeException());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);

    Response response = resource.findFileById(expectedDaaId);
    assert response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  @Test
  void testCreateLibraryCardDaaRelation_AdminCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    admin.setRoles(List.of(role));
    admin.setInstitutionId(1);

    User researcher = new User();
    UserRole researcherRole = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    researcher.setRoles(List.of(researcherRole));
    researcher.setInstitutionId(1);


    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setInstitutionId(1);
    lc.setId(1);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findUserById(any())).thenReturn(researcher);
    when(libraryCardService.findLibraryCardsByUserId(any())).thenReturn(Collections.singletonList(lc));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createLibraryCardDaaRelation(info, authUser, daa.getDaaId(),  admin.getUserId());
    assert response.getStatus() == HttpStatus.SC_OK;
  }

  @Test
  void testCreateLibraryCardDaaRelation_SigningOfficialCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    admin.setRoles(List.of(role));
    admin.setInstitutionId(1);

    User researcher = new User();
    UserRole researcherRole = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    researcher.setRoles(List.of(researcherRole));
    researcher.setInstitutionId(1);

    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    lc.setInstitutionId(1);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findUserById(any())).thenReturn(researcher);
    when(libraryCardService.findLibraryCardsByUserId(any())).thenReturn(Collections.singletonList(lc));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createLibraryCardDaaRelation(info, authUser, daa.getDaaId(),  admin.getUserId());
    assert response.getStatus() == HttpStatus.SC_OK;
  }

  @Test
  void testCreateLibraryCardDaaRelation_InvalidInstitutionIdCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    admin.setRoles(List.of(role));
    admin.setInstitutionId(2);
    User researcher = new User();
    UserRole researcherRole = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    researcher.setRoles(List.of(researcherRole));
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    lc.setInstitutionId(1);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findUserById(any())).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createLibraryCardDaaRelation(info, authUser, daa.getDaaId(),  admin.getUserId());
    assert response.getStatus() == HttpStatus.SC_FORBIDDEN;
  }

  @Test
  void testCreateLibraryCardDaaRelation_InvalidUserCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    admin.setRoles(List.of(role));
    admin.setInstitutionId(2);
    User researcher = new User();
    UserRole researcherRole = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    researcher.setRoles(List.of(researcherRole));
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    lc.setInstitutionId(1);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findUserById(any())).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createLibraryCardDaaRelation(info, authUser, daa.getDaaId(),  admin.getUserId());
    assert response.getStatus() == HttpStatus.SC_FORBIDDEN;
  }

  @Test
  void testCreateLibraryCardDaaRelation_InvalidDaaIdCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    admin.setRoles(List.of(role));
    admin.setInstitutionId(1);
    User researcher = new User();
    UserRole researcherRole = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    researcher.setRoles(List.of(researcherRole));
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    lc.setInstitutionId(1);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findUserById(any())).thenReturn(researcher);
    when(libraryCardService.findLibraryCardsByUserId(any())).thenReturn(Collections.singletonList(lc));


    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createLibraryCardDaaRelation(info, authUser, daa.getDaaId(),  4);
    assert response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  @Test
  void testCreateLibraryCardDaaRelation_NoMatchingLibraryCardsCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(10, 100));
    User admin = new User();
    UserRole role = (new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    admin.setRoles(List.of(role));
    admin.setInstitutionId(1);
    User researcher = new User();
    UserRole researcherRole = (new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    researcher.setRoles(List.of(researcherRole));
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    lc.setInstitutionId(2);

    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findUserById(any())).thenReturn(researcher);
    when(libraryCardService.findLibraryCardsByUserId(any())).thenReturn(Collections.singletonList(lc));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.createLibraryCardDaaRelation(info, authUser, daa.getDaaId(),  admin.getUserId());
    assert response.getStatus() == HttpStatus.SC_NOT_FOUND;
  }

  @Test
  void testDeleteDaaForAdmin() {
    Integer daaId = RandomUtils.nextInt(10, 100);

    User user = new User();
    user.setRoles(List.of(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName())));

    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setUserId(user.getUserId());
    libraryCard.setDaaIds(List.of(daaId));

    when(userService.findUserByEmail(any())).thenReturn(user);
    when(libraryCardService.findLibraryCardsByUserId(any())).thenReturn(List.of(libraryCard));
    doNothing().when(libraryCardService).removeDaaFromLibraryCard(any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.deleteDaaForUser(authUser, daaId, RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_OK;
  }

  @Test
  void testDeleteDaaForUserForbidden() {
    Integer daaId = RandomUtils.nextInt(10, 100);

    User user = new User();
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));

    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setUserId(user.getUserId());
    libraryCard.setDaaIds(List.of(daaId));

    when(userService.findUserByEmail(any())).thenReturn(user);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.deleteDaaForUser(authUser, daaId, RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_FORBIDDEN;
  }

  @Test
  void testDeleteDaaForInvalidUser() {
    when(userService.findUserByEmail(any())).thenReturn(null);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.deleteDaaForUser(authUser, RandomUtils.nextInt(10, 100), RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  @Test
  void testSendDaaRequestMessage() throws Exception {
    User user = new User();
    LibraryCard lc = new LibraryCard();
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    user.addLibraryCard(lc);
    when(userService.findUserByEmail(any())).thenReturn(user);
    doNothing().when(daaService).sendDaaRequestEmails(any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.sendDaaRequestMessage(authUser, RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_OK;
  }

  @Test
  void testSendDaaRequestMessageUserNotFound() {
    User user = new User();
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.sendDaaRequestMessage(authUser, RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_NOT_FOUND;
  }

  @Test
  void testSendDaaRequestMessageDaaNotFound() throws Exception {
    User user = new User();
    LibraryCard lc = new LibraryCard();
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    user.addLibraryCard(lc);
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new NotFoundException()).when(daaService).sendDaaRequestEmails(any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.sendDaaRequestMessage(authUser, RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_NOT_FOUND;
  }

  @Test
  void testSendDaaRequestMessageEmailError() throws Exception {
    User user = new User();
    LibraryCard lc = new LibraryCard();
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    user.addLibraryCard(lc);
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new Exception()).when(daaService).sendDaaRequestEmails(any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.sendDaaRequestMessage(authUser, RandomUtils.nextInt(10, 100));
    assert response.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR;
  }

  @Test
  void testSendDaaRequestMessageAssociationAlreadyExists() throws Exception {
    User user = new User();
    int daaId = RandomUtils.nextInt(10, 100);
    LibraryCard lc = new LibraryCard();
    lc.setDaaIds(List.of(daaId));
    user.setRoles(List.of(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
    user.setLibraryCards(List.of(lc));
    when(userService.findUserByEmail(any())).thenReturn(user);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService, emailService);
    Response response = resource.sendDaaRequestMessage(authUser, daaId);
    assert response.getStatus() == HttpStatus.SC_BAD_REQUEST;
  }
}
