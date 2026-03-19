package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.apache.http.HttpStatus;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DaaResourceTest extends AbstractTestHelper {

  @Mock private DaaService daaService;
  @Mock private DacService dacService;
  @Mock private UserService userService;
  @Mock private LibraryCardService libraryCardService;

  private final AuthUser authUser = new AuthUser("test@test.com");

  private DaaResource resource;

  @Test
  void testCreateDaaForDac_ChairCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User chair = new User();
    chair.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, chair);
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);
    when(daaService.createDaaWithFso(any(), any(), any(), any()))
        .thenReturn(new DataAccessAgreement());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createDaaForDac(
            info, duosUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail)) {
      assertEquals(HttpStatus.SC_CREATED, response.getStatus());
    }
  }

  @Test
  void testCreateDaaForDac_InvalidChairCase() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User chair = new User();
    chair.setChairpersonRoleWithDAC(1);
    DuosUser duosUser = new DuosUser(authUser, chair);
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(dacService.findById(any())).thenReturn(dac);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createDaaForDac(
            info, duosUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail)) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testCreateDaaForDac_InvalidFile() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User user = new User();
    user.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, user);
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);

    when(daaService.createDaaWithFso(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createDaaForDac(
            info, duosUser, dac.getDacId(), IOUtils.toInputStream("test", "UTF-8"), fileDetail)) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testFindAll() {
    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    when(daaService.findAll()).thenReturn(Collections.singletonList(expectedDaa));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response = resource.findAll(duosUser)) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
      JsonArray daas =
          GsonUtil.buildGson().fromJson((response.getEntity().toString()), JsonArray.class);
      assertEquals(1, daas.size());
    }
  }

  @Test
  void testFindAllMultipleDaas() {
    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    DataAccessAgreement expectedDaa1 = new DataAccessAgreement();
    DataAccessAgreement expectedDaa2 = new DataAccessAgreement();
    when(daaService.findAll()).thenReturn(List.of(expectedDaa1, expectedDaa2));

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response = resource.findAll(duosUser)) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
      JsonArray daas =
          GsonUtil.buildGson().fromJson((response.getEntity().toString()), JsonArray.class);
      assertEquals(2, daas.size());
    }
  }

  @Test
  void testFindAllException() {
    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findAll()).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response = resource.findAll(duosUser)) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDaaByDaaId() {
    int expectedDaaId = randomInt(10, 100);
    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    expectedDaa.setDaaId(expectedDaaId);
    DuosUser duosUser = new DuosUser(authUser, new User());
    when(daaService.findById(expectedDaaId)).thenReturn(expectedDaa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.findDaaById(duosUser, expectedDaaId)) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
      assertEquals(expectedDaa, response.getEntity());
    }
  }

  @Test
  void testFindDaaByDaaIdInvalidId() {
    int invalidId = randomInt(10, 100);
    DuosUser duosUser = new DuosUser(authUser, new User());
    when(daaService.findById(invalidId)).thenThrow(new NotFoundException());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.findDaaById(duosUser, invalidId)) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDaaFileByDaaId() throws IOException {
    DuosUser duosUser = new DuosUser(authUser, new User());
    int expectedDaaId = randomInt(10, 100);
    DataAccessAgreement expectedDaa = new DataAccessAgreement();
    expectedDaa.setDaaId(expectedDaaId);
    String fileName = randomAlphanumeric(10) + ".txt";
    FileStorageObject fso = new FileStorageObject();
    fso.setFileName(fileName);
    expectedDaa.setFile(fso);
    String fileContent = randomAlphanumeric(10);

    when(daaService.findFileById(expectedDaaId))
        .thenReturn(new ByteArrayInputStream(fileContent.getBytes()));
    when(daaService.findById(expectedDaaId)).thenReturn(expectedDaa);
    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.findFileById(duosUser, expectedDaaId)) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ((StreamingOutput) response.getEntity()).write(out);
      assertEquals(fileContent, out.toString());
    }
  }

  @Test
  void testFindDaaFileByDaaIdInvalid() {
    DuosUser duosUser = new DuosUser(authUser, new User());
    int invalidId = randomInt(10, 100);
    when(daaService.findFileById(invalidId)).thenThrow(new NotFoundException());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.findFileById(duosUser, invalidId)) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testFindDaaFileByDaaIdDatabaseError() {
    DuosUser duosUser = new DuosUser(authUser, new User());
    int expectedDaaId = randomInt(10, 100);
    when(daaService.findFileById(expectedDaaId)).thenThrow(new RuntimeException());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.findFileById(duosUser, expectedDaaId)) {
      assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardDaaRelation_InvalidInstitutionIdCase() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User signingOfficial = new User();
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(2);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);

    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);

    when(userService.findUserById(any())).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createLibraryCardDaaRelation(
            info, duosUser, daa.getDaaId(), signingOfficial.getUserId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardDaaRelation_InvalidUserCase() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(2);
    DuosUser duosUser = new DuosUser(authUser, researcher);

    User researcher2 = new User();
    researcher2.setUserId(2);
    researcher2.setResearcherRole();
    researcher2.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);

    when(userService.findUserById(researcher2.getUserId())).thenReturn(researcher2);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createLibraryCardDaaRelation(
            info, duosUser, daa.getDaaId(), researcher2.getUserId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardDaaRelation_InvalidDaaIdCase() {
    UriInfo info = mock(UriInfo.class);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User signingOfficial = new User();
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(1);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    researcher.setLibraryCard(lc);

    when(userService.findUserById(any())).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createLibraryCardDaaRelation(info, duosUser, daa.getDaaId(), 4)) {
      assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardDaaRelation_NoMatchingLibraryCardsCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User signingOfficial = new User();
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(1);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard lc = new LibraryCard();
    lc.setId(1);
    researcher.setLibraryCard(lc);

    when(userService.findUserById(any())).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createLibraryCardDaaRelation(
            info, duosUser, daa.getDaaId(), signingOfficial.getUserId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testCreateLibraryCardDaaRelation_NoLibraryCardsCase() {
    UriInfo info = mock(UriInfo.class);
    UriBuilder builder = mock(UriBuilder.class);
    when(info.getBaseUriBuilder()).thenReturn(builder);
    when(builder.replacePath(any())).thenReturn(builder);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    User signingOfficial = new User();
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(1);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);
    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(1);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(1);
    LibraryCard newLc = new LibraryCard();
    newLc.setId(1);

    when(userService.findUserById(any())).thenReturn(researcher);
    when(libraryCardService.createLibraryCardForSigningOfficial(any(), any())).thenReturn(newLc);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.createLibraryCardDaaRelation(
            info, duosUser, daa.getDaaId(), signingOfficial.getUserId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteDaaValidUser() {
    Integer daaId = randomInt(10, 100);

    User signingOfficial = new User();
    signingOfficial.setUserId(1);
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(1);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);

    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(signingOfficial.getInstitutionId());
    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setUserId(researcher.getUserId());
    libraryCard.setDaaIds(List.of(daaId));
    researcher.setLibraryCard(libraryCard);

    when(userService.findUserById(researcher.getUserId())).thenReturn(researcher);
    doNothing().when(libraryCardService).removeDaaFromLibraryCard(any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.deleteDaaForUser(duosUser, daaId, researcher.getUserId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteDaaValidUserNoLibraryCard() {
    Integer daaId = randomInt(10, 100);

    User signingOfficial = new User();
    signingOfficial.setUserId(1);
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(1);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);

    User researcher = new User();
    researcher.setResearcherRole();
    researcher.setInstitutionId(signingOfficial.getInstitutionId());

    when(userService.findUserById(researcher.getUserId())).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.deleteDaaForUser(duosUser, daaId, researcher.getUserId())) {
      verify(libraryCardService, never()).removeDaaFromLibraryCard(any(), any());
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteDaaForInvalidUser() {
    DuosUser duosUser = new DuosUser(authUser, new User());
    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.deleteDaaForUser(duosUser, randomInt(10, 100), randomInt(10, 100))) {
      assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testDeleteDaaDifferentUserInstitution() {
    Integer daaId = randomInt(10, 100);

    User signingOfficial = new User();
    signingOfficial.setUserId(1);
    signingOfficial.setSigningOfficialRole();
    signingOfficial.setInstitutionId(1);
    DuosUser duosUser = new DuosUser(authUser, signingOfficial);

    User differentInstitutionUser = new User();
    differentInstitutionUser.setUserId(2);
    differentInstitutionUser.setResearcherRole();
    differentInstitutionUser.setInstitutionId(2);

    LibraryCard libraryCard = new LibraryCard();
    libraryCard.setUserId(signingOfficial.getUserId());
    libraryCard.setDaaIds(List.of(daaId));
    signingOfficial.setLibraryCard(libraryCard);

    when(userService.findUserById(differentInstitutionUser.getUserId())).thenReturn(differentInstitutionUser);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.deleteDaaForUser(duosUser, daaId, differentInstitutionUser.getUserId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
      verify(libraryCardService, never()).removeDaaFromLibraryCard(any(), any());
    }
  }

  @Test
  void testSendNewDAAMessage() throws Exception {
    User user = new User();
    int dacId = randomInt(10, 20);
    Dac dac = new Dac();
    dac.setDacId(dacId);
    dac.setName(randomAlphabetic(10));
    user.setChairpersonRoleWithDAC(dacId);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(dacService.findById(any())).thenReturn(dac);
    doNothing().when(daaService).sendNewDaaEmails(any(), any(), any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.sendNewDaaMessage(duosUser, dacId, randomInt(10, 100), randomAlphabetic(10))) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testSendNewDAAMessageDacNotFound() {
    User user = new User();
    int dacId = randomInt(10, 20);
    user.setChairpersonRoleWithDAC(dacId);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(dacService.findById(dacId)).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.sendNewDaaMessage(duosUser, dacId, randomInt(10, 100), randomAlphabetic(10))) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testSendNewDAAMessageDaaNotFound() throws Exception {
    User user = new User();
    int dacId = randomInt(10, 20);
    Dac dac = new Dac();
    dac.setDacId(dacId);
    dac.setName(randomAlphabetic(10));
    user.setChairpersonRoleWithDAC(dacId);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(dacService.findById(dacId)).thenReturn(dac);
    doThrow(new NotFoundException()).when(daaService).sendNewDaaEmails(any(), any(), any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.sendNewDaaMessage(duosUser, dacId, randomInt(10, 100), randomAlphabetic(10))) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testSendNewDAAMessageEmailError() throws Exception {
    User user = new User();
    int dacId = randomInt(10, 20);
    Dac dac = new Dac();
    dac.setDacId(dacId);
    dac.setName(randomAlphabetic(10));
    user.setChairpersonRoleWithDAC(dacId);
    DuosUser duosUser = new DuosUser(authUser, user);
    when(dacService.findById(dacId)).thenReturn(dac);
    doThrow(new Exception()).when(daaService).sendNewDaaEmails(any(), any(), any(), any());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);
    try (Response response =
        resource.sendNewDaaMessage(duosUser, dacId, randomInt(10, 100), randomAlphabetic(10))) {
      assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }
  }

  User researcherWithInstitution(int userId, int institutionId) {
    User user = new User();
    user.setUserId(userId);
    user.setInstitutionId(institutionId);
    user.setResearcherRole();
    return user;
  }

  @Test
  void testBulkAddUsersToDaa() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);
    when(daaService.findById(daaId)).thenReturn(new DataAccessAgreement());
    when(libraryCardService.addDaaToUserLibraryCard(any(), any(), any())).thenReturn(null);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddUsersToDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testBulkAddUsers() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddUsersToDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testBulkAddUsersToDaaForbidden() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(4);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddUsersToDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testBulkAddUsersToDaaDaaNotFound() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);
    when(daaService.findById(daaId)).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddUsersToDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testBulkRemoveUsersFromDaa() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);
    when(daaService.findById(daaId)).thenReturn(new DataAccessAgreement());
    when(libraryCardService.removeDaaFromUserLibraryCard(any(), any())).thenReturn(null);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkRemoveUsersFromDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testBulkRemoveUsersFromDaaForbidden() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(4);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkRemoveUsersFromDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testBulkRemoveUsersFromDaaDaaNotFound() {
    int daaId = 4;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    List<User> users =
        List.of(
            researcherWithInstitution(1, institutionId),
            researcherWithInstitution(2, institutionId),
            researcherWithInstitution(3, institutionId));

    when(userService.findUsersInJsonArray(any(), any())).thenReturn(users);
    when(daaService.findById(daaId)).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkRemoveUsersFromDaa(duosUser, daaId, "{users:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  DataAccessAgreement createDAA(int daaId) {
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    return daa;
  }

  @Test
  void testBulkAddDAAsToUser() {
    int userId = 1;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    User researcher = researcherWithInstitution(userId, institutionId);
    List<DataAccessAgreement> agreements = List.of(createDAA(1), createDAA(2), createDAA(3));

    when(userService.findUserById(userId)).thenReturn(researcher);
    when(daaService.findDAAsInJsonArray(any(), any())).thenReturn(agreements);
    when(libraryCardService.addDaaToUserLibraryCard(any(), any(), any())).thenReturn(null);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddDAAsToUser(duosUser, userId, "{daaList:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testBulkAddDAAsToUserForbidden() {
    int userId = 1;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(4);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    User researcher = researcherWithInstitution(userId, institutionId);

    when(userService.findUserById(userId)).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddDAAsToUser(duosUser, userId, "{daaList:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testBulkAddDAAsToUserNotFound() {
    int userId = 1;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(2);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(userService.findUserById(userId)).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.bulkAddDAAsToUser(duosUser, userId, "{daaList:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testBulkRemoveDAAsFromUserAsSigningOfficial() {
    int userId = 1;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(institutionId);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    User researcher = researcherWithInstitution(userId, institutionId);
    List<DataAccessAgreement> agreements = List.of(createDAA(1), createDAA(2), createDAA(3));

    when(userService.findUserById(userId)).thenReturn(researcher);
    when(daaService.findDAAsInJsonArray(any(), any())).thenReturn(agreements);
    when(libraryCardService.removeDaaFromUserLibraryCard(any(), any())).thenReturn(null);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response =
        resource.bulkRemoveDAAsFromUser(duosUser, userId, "{daaList:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testBulkRemoveDAAsFromUserForbidden() {
    int userId = 1;
    int institutionId = 2;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(4);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    User researcher = researcherWithInstitution(userId, institutionId);

    when(userService.findUserById(userId)).thenReturn(researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response =
        resource.bulkRemoveDAAsFromUser(duosUser, userId, "{daaList:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testBulkRemoveDAAsFromUserNotFound() {
    int userId = 1;

    User authedUser = new User();
    authedUser.setSigningOfficialRole();
    authedUser.setInstitutionId(2);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(userService.findUserById(userId)).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response =
        resource.bulkRemoveDAAsFromUser(duosUser, userId, "{daaList:[1,2,3]}")) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaChairperson() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User chairperson = new User();
    chairperson.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, chairperson);

    when(daaService.findById(any())).thenReturn(daa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaChairpersonNoMatchingDac() {
    int daaId = randomInt(10, 100);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User chairperson = new User();
    chairperson.setChairpersonRoleWithDAC(randomInt(100, 200));
    DuosUser duosUser = new DuosUser(authUser, chairperson);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaFromUserForbidden() {
    int daaId = randomInt(10, 100);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User researcher = new User();
    researcher.setResearcherRole();
    DuosUser duosUser = new DuosUser(authUser, researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaDaaNotFound() {
    int daaId = randomInt(10, 100);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findById(any())).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaDacNotFound() {
    int daaId = randomInt(10, 100);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(dacService.findById(any())).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaAlreadyExists() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findById(any())).thenReturn(daa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testAddDacToDaaDaaWithDacs() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac1 = new Dac();
    dac1.setDacId(1);
    Dac dac2 = new Dac();
    dac2.setDacId(2);
    daa.addDac(dac1);
    daa.addDac(dac2);
    Dac dac3 = new Dac();
    dac3.setDacId(3);

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac3.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findById(any())).thenReturn(daa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.modifyDacDaaRelationship(duosUser, daaId, dac3.getDacId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testRemoveDacFromDaaChairperson() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    daa.addDac(dac);

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findById(any())).thenReturn(daa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.removeDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
  }

  @Test
  void testRemoveDacFromDaaChairpersonNoMatchingDac() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(1);
    daa.addDac(dac);

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(2);
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.removeDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testRemoveDacFromDaaFromUserForbidden() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    daa.addDac(dac);

    User researcher = new User();
    researcher.setResearcherRole();
    DuosUser duosUser = new DuosUser(authUser, researcher);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.removeDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testRemoveDacFromDaaDaaNotFound() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    daa.addDac(dac);

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findById(any())).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.removeDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testRemoveDacFromDaaDacNotFound() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));
    daa.addDac(dac);

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(dacService.findById(any())).thenThrow(new NotFoundException());

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.removeDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testRemoveDacFromDaaDoesNotExist() {
    int daaId = randomInt(10, 100);
    DataAccessAgreement daa = new DataAccessAgreement();
    daa.setDaaId(daaId);
    Dac dac = new Dac();
    dac.setDacId(randomInt(10, 100));

    User authedUser = new User();
    authedUser.setChairpersonRoleWithDAC(dac.getDacId());
    DuosUser duosUser = new DuosUser(authUser, authedUser);

    when(daaService.findById(any())).thenReturn(daa);

    resource = new DaaResource(daaService, dacService, userService, libraryCardService);

    try (Response response = resource.removeDacDaaRelationship(duosUser, daaId, dac.getDacId())) {
      assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }
  }
}
