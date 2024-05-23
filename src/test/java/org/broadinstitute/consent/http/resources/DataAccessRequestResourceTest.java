package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestResourceTest {

  @Mock
  private DataAccessRequestService dataAccessRequestService;
  @Mock
  private MatchService matchService;
  @Mock
  private EmailService emailService;
  @Mock
  private GCSService gcsService;
  @Mock
  private UserService userService;
  @Mock
  private DatasetService datasetService;
  @Mock
  private UriInfo info;
  @Mock
  private UriBuilder builder;
  @Mock
  private User mockUser;

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final AuthUser adminUser = new AuthUser("admin@test.com");
  private final AuthUser chairpersonUser = new AuthUser("chariperson@test.com");
  private final AuthUser memberUser = new AuthUser("member@test.com");
  private final AuthUser anotherUser = new AuthUser("bob@test.com");
  private final List<UserRole> roles = Collections.singletonList(UserRoles.Researcher());
  private final List<UserRole> adminRoles = Collections.singletonList(UserRoles.Admin());
  private final List<UserRole> chairpersonRoles = Collections.singletonList(UserRoles.Chairperson());
  private final List<UserRole> memberRoles = Collections.singletonList(UserRoles.Member());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
  private final User admin = new User(2, adminUser.getEmail(), "Admin user", new Date(),
      adminRoles);
  private final User chairperson = new User(3, chairpersonUser.getEmail(), "Chairperson user",
      new Date(), chairpersonRoles);
  private final User member = new User(4, memberUser.getEmail(), "Member user", new Date(),
      memberRoles);
  private final User bob = new User(5, anotherUser.getEmail(), "Bob", new Date(), roles);

  private DataAccessRequestResource resource;

  private void initResource() {
    try {
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
  }

  @Test
  void testCreateDataAccessRequestNoLibraryCard() {
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
    initResource();

    Response response = resource.createDataAccessRequest(authUser, info, "");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testCreateDataAccessRequest() {
    try {
      User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
      userWithCards.setLibraryCards(List.of(new LibraryCard()));
      when(userService.findUserByEmail(any())).thenReturn(userWithCards);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
      when(dataAccessRequestService.createDataAccessRequest(any(), any()))
          .thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
      doNothing().when(emailService).sendNewDARCollectionMessage(any());
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    Response response = resource.createDataAccessRequest(authUser, info, "");
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetByReferenceId() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    Response response = resource.getByReferenceId(authUser, "");
    assertEquals(200, response.getStatus());
  }

  @Test
  void testGetByReferenceIdForbidden() {
    when(mockUser.getUserId()).thenReturn(user.getUserId() + 1);
    when(userService.findUserByEmail(any())).thenReturn(mockUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    assertThrows(ForbiddenException.class, () -> {
      resource.getByReferenceId(authUser, "");
    });
  }

  @Test
  void testUpdateByReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
      when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    Response response = resource.updateByReferenceId(authUser, "", "{}");
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUpdateByReferenceIdForbidden() {
    User invalidUser = new User(1000, authUser.getEmail(), "Display Name", new Date());
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(invalidUser);
      when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    Response response = resource.updateByReferenceId(authUser, "", "{}");
    assertEquals(403, response.getStatus());
  }

  @Test
  void testCreateDraftDataAccessRequest() {
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(dataAccessRequestService.insertDraftDataAccessRequest(any(), any())).thenReturn(dar);
      when(builder.path(anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    Response response = resource.createDraftDataAccessRequest(authUser, info, "");
    assertEquals(201, response.getStatus());
  }

  @Test
  void testUpdatePartialDataAccessRequest() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    initResource();

    Response response = resource.updatePartialDataAccessRequest(authUser, "", "{}");
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUpdatePartialDataAccessRequestForbidden() {
    User invalidUser = new User(1000, authUser.getEmail(), "Display Name", new Date());
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(invalidUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    initResource();

    Response response = resource.updatePartialDataAccessRequest(authUser, "", "{}");
    assertEquals(403, response.getStatus());
  }

  @Test
  void testGetIrbDocument() {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    when(userService.findUserByEmail(chairperson.getEmail())).thenReturn(chairperson);
    when(userService.findUserByEmail(admin.getEmail())).thenReturn(admin);
    when(userService.findUserByEmail(member.getEmail())).thenReturn(member);
    when(userService.findUserByEmail(bob.getEmail())).thenReturn(bob);
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setIrbDocumentLocation(RandomStringUtils.randomAlphabetic(10));
    dar.getData().setIrbDocumentName(RandomStringUtils.randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    initResource();

    assertEquals(200, resource.getIrbDocument(chairpersonUser, "").getStatus());
    assertEquals(200, resource.getIrbDocument(adminUser, "").getStatus());
    assertEquals(200, resource.getIrbDocument(memberUser, "").getStatus());
    assertEquals(200, resource.getIrbDocument(authUser, "").getStatus());
    assertEquals(403, resource.getIrbDocument(anotherUser, "").getStatus());
  }

  @Test
  void testGetIrbDocumentNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetIrbDocumentDARNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetIrbDocumentNullValues() {
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetIrbDocumentEmptyValues() {
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUploadIrbDocument() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    initResource();

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUploadIrbDocumentDARNotFound() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    initResource();

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUploadIrbDocumentWithPreviousIrbDocument() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setIrbDocumentLocation(RandomStringUtils.randomAlphabetic(10));
    dar.getData().setIrbDocumentName(RandomStringUtils.randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    initResource();

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(200, response.getStatus());
  }

  private Pair<InputStream, FormDataContentDisposition> mockFormDataMultiPart(String fileName) {
    String name = FilenameUtils.removeExtension(fileName);
    InputStream inputStream = IOUtils.toInputStream(name, Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    return Pair.of(inputStream, formData);
  }

  @Test
  void testPostProgressReportCollabAndEthicsFiles() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.getData().setCollaborationLetterLocation("collaborationLetterLocation");
    parentDar.getData().setIrbDocumentLocation("irbDocumentLocation");
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    Dataset dataset = mock(Dataset.class);
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    DataUse dataUseCollabAndEthics = new DataUseBuilder()
        .setCollaboratorRequired(true)
        .setEthicsApprovalRequired(true).build();
    when(dataset.getDataUse()).thenReturn(dataUseCollabAndEthics);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(childDar);
    initResource();

    Response response = resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
        collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    assertEquals(200, response.getStatus());
  }

  @Test
  void testPostProgressReportUserNotAuthorized() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(member);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    initResource();

    assertThrows(ForbiddenException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportMissingParentDar() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenThrow(NotFoundException.class);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    initResource();

    assertThrows(NotFoundException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportInvalidJson() {
    String invalidDar = "{\"projectTitle\": \"test\", \"datasetIds\": \"invalid\"}";
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", invalidDar,
          collabFile.getLeft(), collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportNullCollabFile() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = Pair.of(null, null);
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    Dataset dataset = mock(Dataset.class);
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    DataUse dataUseCollabAndEthics = new DataUseBuilder()
        .setCollaboratorRequired(true)
        .setEthicsApprovalRequired(true).build();
    when(dataset.getDataUse()).thenReturn(dataUseCollabAndEthics);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportEmptyEthicsFile() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    Dataset dataset = mock(Dataset.class);
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    DataUse dataUseCollabAndEthics = new DataUseBuilder()
        .setCollaboratorRequired(true)
        .setEthicsApprovalRequired(true).build();
    when(dataset.getDataUse()).thenReturn(dataUseCollabAndEthics);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportNullDataset() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    when(datasetService.findDatasetById(any())).thenReturn(null);
    initResource();

    assertThrows(NotFoundException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportNullDataUse() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    Dataset dataset = mock(Dataset.class);
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    when(dataset.getDataUse()).thenReturn(null);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportNullCollaboratorDataUse() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    Dataset dataset = mock(Dataset.class);
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    DataUse dataUseEthicsOnly = new DataUseBuilder().setEthicsApprovalRequired(true).build();
    when(dataset.getDataUse()).thenReturn(dataUseEthicsOnly);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testPostProgressReportNullEthicsApprovalDataUse() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    Dataset dataset = mock(Dataset.class);
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    DataUse dataUseCollaboratorOnly = new DataUseBuilder().setCollaboratorRequired(true).build();
    when(dataset.getDataUse()).thenReturn(dataUseCollaboratorOnly);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  void testGetCollaborationDocument() {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    when(userService.findUserByEmail(chairperson.getEmail())).thenReturn(chairperson);
    when(userService.findUserByEmail(admin.getEmail())).thenReturn(admin);
    when(userService.findUserByEmail(member.getEmail())).thenReturn(member);
    when(userService.findUserByEmail(bob.getEmail())).thenReturn(bob);
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setCollaborationLetterLocation(RandomStringUtils.randomAlphabetic(10));
    dar.getData().setCollaborationLetterName(RandomStringUtils.randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    initResource();

    assertEquals(200,
        resource.getCollaborationDocument(chairpersonUser, "").getStatus());
    assertEquals(200, resource.getCollaborationDocument(adminUser, "").getStatus());
    assertEquals(200, resource.getCollaborationDocument(memberUser, "").getStatus());
    assertEquals(200, resource.getCollaborationDocument(authUser, "").getStatus());
    assertEquals(403,
        resource.getCollaborationDocument(anotherUser, "").getStatus());
  }

  @Test
  void testGetCollaborationDocumentNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    Response response = resource.getCollaborationDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetCollaborationDocumentDARNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    initResource();

    Response response = resource.getCollaborationDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetCollaborationDocumentNullValues() {
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetCollaborationDocumentEmptyValues() {
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUploadCollaborationDocument() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("buket", "name"));
    initResource();

    Response response = resource.uploadCollaborationDocument(authUser, "", uploadInputStream,
        formData);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUploadCollaborationDocumentDARNotFound() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    initResource();

    Response response = resource.uploadCollaborationDocument(authUser, "", uploadInputStream,
        formData);
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUploadCollaborationDocumentWithPreviousDocument() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setCollaborationLetterLocation(RandomStringUtils.randomAlphabetic(10));
    dar.getData().setCollaborationLetterName(RandomStringUtils.randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    initResource();

    Response response = resource.uploadCollaborationDocument(authUser, "", uploadInputStream,
        formData);
    assertEquals(200, response.getStatus());
  }

  private DataAccessRequest generateDataAccessRequest() {
    Timestamp now = new Timestamp(new Date().getTime());
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setReferenceId(UUID.randomUUID().toString());
    data.setReferenceId(dar.getReferenceId());
    dar.setDatasetIds(Arrays.asList(1, 2));
    dar.setData(data);
    dar.setUserId(user.getUserId());
    dar.setCreateDate(now);
    dar.setUpdateDate(now);
    dar.setSortDate(now);
    return dar;
  }

  @Test
  void getDataAccessRequests() {
    initResource();
    List<DataAccessRequest> list = Collections.emptyList();
    when(dataAccessRequestService.getDataAccessRequestsByUserRole(any())).thenReturn(list);
    Response res = resource.getDataAccessRequests(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  void getDraftDataAccessRequests() {
    initResource();
    List<DataAccessRequest> list = Collections.emptyList();
    User user = new User();
    user.setUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findAllDraftDataAccessRequestsByUser(any())).thenReturn(list);
    Response res = resource.getDraftDataAccessRequests(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  void getDraftDataAccessRequests_UserNotFound() {
    initResource();
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    resource.getDraftDataAccessRequests(authUser);
    Response res = resource.getDraftDataAccessRequests(authUser);
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_NOT_FOUND);
  }

  @Test
  void getDraftDar() {
    initResource();
    User user = new User();
    user.setUserId(10);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(10);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  void getDraftDar_UserNotFound() {
    initResource();
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_NOT_FOUND);
  }

  @Test
  void getDraftDar_DarNotFound() {
    initResource();
    User user = new User();
    user.setUserId(10);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenThrow(new NotFoundException());
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_NOT_FOUND);
  }

  @Test
  void getDraftDar_UserNotAllowed() {
    initResource();
    User user = new User();
    user.setUserId(10);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(11);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_FORBIDDEN);
  }

  @Test
  void testCreateDataAccessRequestWithDAARestrictions() {
    try {
      User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
      userWithCards.setLibraryCards(List.of(new LibraryCard()));
      when(userService.findUserByEmail(any())).thenReturn(userWithCards);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
      when(dataAccessRequestService.createDataAccessRequest(any(), any()))
          .thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
      doNothing().when(emailService).sendNewDARCollectionMessage(any());
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (Response response = resource.createDataAccessRequestWithDAARestrictions(authUser, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

  @Test
  void testCreateDataAccessRequestWithDAARestrictionsFailure() {
    try {
      User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
      userWithCards.setLibraryCards(List.of(new LibraryCard()));
      when(userService.findUserByEmail(any())).thenReturn(userWithCards);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
      doThrow(BadRequestException.class).when(datasetService).enforceDAARestrictions(any(), any());
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (Response response = resource.createDataAccessRequestWithDAARestrictions(authUser, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateDraftDataAccessRequestWithDAARestrictions() {
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(dataAccessRequestService.insertDraftDataAccessRequest(any(), any())).thenReturn(dar);
      when(builder.path(anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (Response response = resource.createDraftDataAccessRequestWithDAARestrictions(authUser, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

  @Test
  void testCreateDraftDataAccessRequestWithDAARestrictionsFailure() {
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      doThrow(BadRequestException.class).when(datasetService).enforceDAARestrictions(any(), any());
      resource =
          new DataAccessRequestResource(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (Response response = resource.createDraftDataAccessRequestWithDAARestrictions(authUser, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdatePartialDataAccessRequestWithDAARestrictions() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    initResource();

    try (Response response = resource.updatePartialDataAccessRequestWithDAARestrictions(authUser, "", "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdatePartialDataAccessRequestWithDAARestrictionsFailure() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    doThrow(BadRequestException.class).when(datasetService).enforceDAARestrictions(any(), any());
    initResource();

    try (Response response = resource.updatePartialDataAccessRequestWithDAARestrictions(authUser, "", "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

}
