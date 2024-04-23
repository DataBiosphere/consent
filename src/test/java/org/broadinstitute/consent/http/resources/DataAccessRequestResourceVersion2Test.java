package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DataAccessRequestResourceVersion2Test {

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

  private DataAccessRequestResourceVersion2 resource;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initResource() {
    try {
      when(builder.path(anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
      resource =
          new DataAccessRequestResourceVersion2(
              dataAccessRequestService, emailService, gcsService, userService, datasetService,
              matchService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
  }

  @Test
  public void testCreateDataAccessRequestNoLibraryCard() {
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
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
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
    initResource();
    Response response = resource.createDataAccessRequest(authUser, info, "");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateDataAccessRequest() {
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
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
    initResource();
    Response response = resource.createDataAccessRequest(authUser, info, "");
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetByReferenceId() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();
    Response response = resource.getByReferenceId(authUser, "");
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testGetByReferenceIdForbidden() {
    when(mockUser.getUserId()).thenReturn(user.getUserId() + 1);
    when(userService.findUserByEmail(any())).thenReturn(mockUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    assertThrows(ForbiddenException.class, () -> {
      resource.getByReferenceId(authUser, "");
    });
  }

  @Test
  public void testUpdateByReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
      when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
    initResource();
    Response response = resource.updateByReferenceId(authUser, "", "{}");
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testUpdateByReferenceIdForbidden() {
    User invalidUser = new User(1000, authUser.getEmail(), "Display Name", new Date());
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(invalidUser);
      when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
      when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
    initResource();
    Response response = resource.updateByReferenceId(authUser, "", "{}");
    assertEquals(403, response.getStatus());
  }

  @Test
  public void testCreateDraftDataAccessRequest() {
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(dataAccessRequestService.insertDraftDataAccessRequest(any(), any())).thenReturn(dar);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
    initResource();
    Response response = resource.createDraftDataAccessRequest(authUser, info, "");
    assertEquals(201, response.getStatus());
  }

  @Test
  public void testUpdatePartialDataAccessRequest() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    initResource();

    Response response = resource.updatePartialDataAccessRequest(authUser, "", "{}");
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testUpdatePartialDataAccessRequestForbidden() {
    User invalidUser = new User(1000, authUser.getEmail(), "Display Name", new Date());
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(invalidUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    initResource();

    Response response = resource.updatePartialDataAccessRequest(authUser, "", "{}");
    assertEquals(403, response.getStatus());
  }

  @Test
  public void testGetIrbDocument() {
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
  public void testGetIrbDocumentNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetIrbDocumentDARNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetIrbDocumentNullValues() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    when(dar.getData()).thenReturn(data);
    when(data.getIrbDocumentLocation()).thenReturn(null);
    when(data.getIrbDocumentName()).thenReturn(null);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetIrbDocumentEmptyValues() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    when(dar.getData()).thenReturn(data);
    when(data.getIrbDocumentLocation()).thenReturn("");
    when(data.getIrbDocumentName()).thenReturn("");
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUploadIrbDocument() throws Exception {
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
  public void testUploadIrbDocumentDARNotFound() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    initResource();

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUploadIrbDocumentWithPreviousIrbDocument() throws Exception {
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
    String ext = FilenameUtils.getExtension(fileName);

    InputStream inputStream = IOUtils.toInputStream(name, Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn(fileName);
    when(formData.getType()).thenReturn(ext);
    when(formData.getSize()).thenReturn(1L);
    when(formData.getName()).thenReturn(name);

    return Pair.of(inputStream, formData);
  }

  @Test
  public void testPostProgressReportCollabAndEthicsFiles() throws IOException {
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
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(childDar);
    initResource();

    Response response = resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
        collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testPostProgressReportUserNotAuthorized() throws IOException {
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
  public void testPostProgressReportMissingParentDar() throws IOException {
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
  public void testPostProgressReportInvalidJson() {
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
  public void testPostProgressReportNullCollabFile() throws IOException {
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
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(childDar);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  public void testPostProgressReportEmptyEthicsFile() throws IOException {
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    DataAccessRequest parentDar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createDataAccessRequest(any(), any())).thenReturn(childDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");
    when(ethicsFile.getRight().getSize()).thenReturn(0L);
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

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  public void testPostProgressReportNullDataset() throws IOException {
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
  public void testPostProgressReportNullDataUse() throws IOException {
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
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(childDar);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  public void testPostProgressReportNullCollaboratorDataUse() throws IOException {
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
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(childDar);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  public void testPostProgressReportNullEthicsApprovalDataUse() throws IOException {
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
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(childDar);
    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.postProgressReport(authUser, "", "", collabFile.getLeft(),
          collabFile.getRight(), ethicsFile.getLeft(), ethicsFile.getRight());
    });
  }

  @Test
  public void testGetCollaborationDocument() {
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
  public void testGetCollaborationDocumentNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());
    initResource();

    Response response = resource.getCollaborationDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetCollaborationDocumentDARNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    initResource();

    Response response = resource.getCollaborationDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetCollaborationDocumentNullValues() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    when(dar.getData()).thenReturn(data);
    when(data.getCollaborationLetterLocation()).thenReturn(null);
    when(data.getCollaborationLetterName()).thenReturn(null);
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetCollaborationDocumentEmptyValues() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = Mockito.mock(DataAccessRequest.class);
    DataAccessRequestData data = Mockito.mock(DataAccessRequestData.class);
    when(dar.getData()).thenReturn(data);
    when(data.getCollaborationLetterLocation()).thenReturn("");
    when(data.getCollaborationLetterName()).thenReturn("");
    initResource();

    Response response = resource.getIrbDocument(authUser, "");
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUploadCollaborationDocument() throws Exception {
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
  public void testUploadCollaborationDocumentDARNotFound() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    initResource();

    Response response = resource.uploadCollaborationDocument(authUser, "", uploadInputStream,
        formData);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUploadCollaborationDocumentWithPreviousDocument() throws Exception {
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
  public void getDataAccessRequests() {
    initResource();
    List<DataAccessRequest> list = Collections.emptyList();
    when(dataAccessRequestService.getDataAccessRequestsByUserRole(any())).thenReturn(list);
    Response res = resource.getDataAccessRequests(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  public void getDraftDataAccessRequests() {
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
  public void getDraftDataAccessRequests_UserNotFound() {
    initResource();
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    resource.getDraftDataAccessRequests(authUser);
    Response res = resource.getDraftDataAccessRequests(authUser);
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_NOT_FOUND);
  }

  @Test
  public void getDraftDar() {
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
  public void getDraftDar_UserNotFound() {
    initResource();
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_NOT_FOUND);
  }

  @Test
  public void getDraftDar_DarNotFound() {
    initResource();
    User user = new User();
    user.setUserId(10);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenThrow(new NotFoundException());
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(res.getStatus(), HttpStatusCodes.STATUS_CODE_NOT_FOUND);
  }

  @Test
  public void getDraftDar_UserNotAllowed() {
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

}
