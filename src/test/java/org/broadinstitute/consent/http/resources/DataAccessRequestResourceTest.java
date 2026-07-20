package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.enumeration.DarDocumentType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetDaaSnapshot;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.DaaService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DarCollectionService;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.MatchService;
import org.broadinstitute.consent.http.service.UserService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestResourceTest extends AbstractTestHelper {

  private final AuthUser authUser = new AuthUser("test@test.com");
  private final AuthUser adminUser = new AuthUser("admin@test.com");
  private final AuthUser chairpersonUser = new AuthUser("chariperson@test.com");
  private final AuthUser memberUser = new AuthUser("member@test.com");
  private final AuthUser anotherUser = new AuthUser("bob@test.com");
  private final List<UserRole> roles = Collections.singletonList(UserRoles.Researcher());
  private final List<UserRole> adminRoles = Collections.singletonList(UserRoles.Admin());
  private final List<UserRole> chairpersonRoles =
      Collections.singletonList(UserRoles.Chairperson());
  private final List<UserRole> memberRoles = Collections.singletonList(UserRoles.Member());
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
  private final DuosUser duosUser = new DuosUser(authUser, user);
  private final User admin =
      new User(2, adminUser.getEmail(), "Admin user", new Date(), adminRoles);
  private final User chairperson =
      new User(3, chairpersonUser.getEmail(), "Chairperson user", new Date(), chairpersonRoles);
  private final User member =
      new User(4, memberUser.getEmail(), "Member user", new Date(), memberRoles);
  private final User bob = new User(5, anotherUser.getEmail(), "Bob", new Date(), roles);
  @Mock private DaaService daaService;
  @Mock private DacService dacService;
  @Mock private DataAccessRequestService dataAccessRequestService;
  @Mock private MatchService matchService;
  @Mock private GCSService gcsService;
  @Mock private UserService userService;
  @Mock private DatasetService datasetService;
  @Mock private DarCollectionService darCollectionService;
  @Mock private ContainerRequest request;
  @Mock private UriInfo info;
  @Mock private UriBuilder builder;
  @Mock private User mockUser;
  private DataAccessRequestResource resource;

  @BeforeEach
  void initResource() {
    user.setLibraryCard(new LibraryCard());
    try {
      resource =
          new DataAccessRequestResource(
              daaService,
              dacService,
              dataAccessRequestService,
              gcsService,
              userService,
              datasetService,
              matchService,
              darCollectionService);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }
  }

  @Test
  void testCreateDataAccessRequest() {
    try {
      User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
      userWithCards.setLibraryCard(new LibraryCard());
      when(userService.findUserByEmail(any())).thenReturn(userWithCards);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
      when(dataAccessRequestService.createDataAccessRequest(any(), any(), any())).thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
      doNothing().when(darCollectionService).createElectionsForNewDarCollection(any());
      doNothing().when(darCollectionService).sendNewDARCollectionMessage(any());
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (var response = resource.createDataAccessRequest(authUser, request, info, "")) {
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testCreateDataAccessRequestWithSubmittedDAR() {
    User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
    userWithCards.setLibraryCard(new LibraryCard());
    when(userService.findUserByEmail(any())).thenReturn(userWithCards);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setCollectionId(1);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(dar.getReferenceId());
    dar.setData(data);
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    doThrow(new SubmittedDARCannotBeEditedException())
        .when(dataAccessRequestService)
        .createDataAccessRequest(any(), any(), any());

    try (var response = resource.createDataAccessRequest(authUser, request, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY, response.getStatus());
      org.broadinstitute.consent.http.models.Error error =
          (org.broadinstitute.consent.http.models.Error) response.getEntity();
      assertEquals(SubmittedDARCannotBeEditedException.MESSAGE, error.message());
    }
  }

  @Test
  void testCreateDataAccessRequestWithoutValidERACommons() {
    User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
    userWithCards.setLibraryCard(new LibraryCard());
    when(userService.findUserByEmail(any())).thenReturn(userWithCards);
    doThrow(new BadRequestException())
        .when(dataAccessRequestService)
        .createDataAccessRequest(eq(user), any(), any());

    try (var response = resource.createDataAccessRequest(authUser, request, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());

    Response response = resource.getByReferenceId(new DuosUser(authUser, user), "");
    assertEquals(200, response.getStatus());
  }

  @Test
  void testGetByReferenceIdForbidden() {
    DuosUser mockedDuosUser = new DuosUser(authUser, mockUser);
    when(mockUser.getUserId()).thenReturn(user.getUserId() + 1);
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId("id")).thenReturn(dar);

    try (Response response = resource.getByReferenceId(mockedDuosUser, "id")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = UserRoles.class,
      names = {"ADMIN", "CHAIRPERSON", "MEMBER", "SIGNINGOFFICIAL"})
  void testGetByReferenceIdAllowedRoles(UserRoles role) {
    UserRole userRole = new UserRole(role.getRoleId(), role.getRoleName());
    User roleUser = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(userRole));
    roleUser.setInstitutionId(7);
    // Set the DAR create user to be a different user from the roleUser
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(roleUser.getUserId() + 1);
    dar.setDatasetIds(List.of(10));
    when(dataAccessRequestService.findByReferenceId("id")).thenReturn(dar);

    // CHAIRPERSON / MEMBER: user must be in a DAC governing a DAR dataset
    Dac relevantDac = new Dac();
    relevantDac.setChairpersons(List.of(roleUser));
    relevantDac.setMembers(List.of(roleUser));
    lenient().when(dacService.findByDatasetId(any())).thenReturn(Set.of(relevantDac));

    // SIGNINGOFFICIAL: creator must share the same institution
    User creator = new User(dar.getUserId(), "creator@test.com", "Creator", new Date(), roles);
    creator.setInstitutionId(7);
    lenient().when(userService.findUserById(dar.getUserId())).thenReturn(creator);

    Response response = resource.getByReferenceId(new DuosUser(authUser, roleUser), "id");
    assertEquals(200, response.getStatus());
  }

  @ParameterizedTest
  @EnumSource(
      value = UserRoles.class,
      names = {"ALUMNI", "DATASUBMITTER", "ITDIRECTOR", "SERVICE_ACCOUNT", "RESEARCHER"})
  void testGetByReferenceIdDisallowedRoles(UserRoles role) {
    UserRole userRole = new UserRole(role.getRoleId(), role.getRoleName());
    User roleUser = new User(1, authUser.getEmail(), "Display Name", new Date(), List.of(userRole));
    DuosUser duosRoleUser = new DuosUser(authUser, roleUser);
    // Set the DAR create user to be a different user from the roleUser
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(roleUser.getUserId() + 1);
    when(dataAccessRequestService.findByReferenceId("id")).thenReturn(dar);

    try (Response response = resource.getByReferenceId(duosRoleUser, "id")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testUpdateByReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    user.setLibraryCard(new LibraryCard());
    try {
      when(userService.findUserByEmail(any())).thenReturn(user);
      when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
      when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
      doNothing().when(matchService).reprocessMatchesForPurpose(any());
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (var response = resource.updateByReferenceId(authUser, "", "{}")) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testUpdateByReferenceIdForbidden() {
    User invalidUser = new User(1000, authUser.getEmail(), "Display Name", new Date());
    DataAccessRequest dar = generateDataAccessRequest();
    try {
      when(userService.findUserByEmail(any())).thenReturn(invalidUser);
      when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (var response = resource.updateByReferenceId(authUser, "", "{}")) {
      assertEquals(403, response.getStatus());
    }
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
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (var response = resource.createDraftDataAccessRequest(authUser, info, "")) {
      assertEquals(201, response.getStatus());
    }
  }

  @Test
  void testUpdatePartialDataAccessRequest() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);

    try (var response = resource.updatePartialDataAccessRequest(authUser, "", "{}")) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testUpdatePartialDataAccessRequestForbidden() {
    User invalidUser = new User(1000, authUser.getEmail(), "Display Name", new Date());
    DataAccessRequest dar = generateDataAccessRequest();
    when(userService.findUserByEmail(any())).thenReturn(invalidUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

    try (var response = resource.updatePartialDataAccessRequest(authUser, "", "{}")) {
      assertEquals(403, response.getStatus());
    }
  }

  @Test
  void testGetIrbDocument() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setIrbDocumentLocation(randomAlphabetic(10));
    dar.getData().setIrbDocumentName(randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

    // For non-creator chair/member: stub DAC membership so validation passes
    Dac dacWithAll = new Dac();
    dacWithAll.setChairpersons(List.of(chairperson));
    dacWithAll.setMembers(List.of(member));
    when(dacService.findByDatasetId(any())).thenReturn(Set.of(dacWithAll));

    assertEquals(
        200, resource.getIrbDocument(new DuosUser(chairpersonUser, chairperson), "").getStatus());
    assertEquals(200, resource.getIrbDocument(new DuosUser(adminUser, admin), "").getStatus());
    assertEquals(200, resource.getIrbDocument(new DuosUser(memberUser, member), "").getStatus());
    assertEquals(200, resource.getIrbDocument(new DuosUser(authUser, user), "").getStatus());
    assertEquals(403, resource.getIrbDocument(new DuosUser(anotherUser, bob), "").getStatus());
  }

  @Test
  void testGetIrbDocumentNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());

    Response response = resource.getIrbDocument(new DuosUser(authUser, user), "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetIrbDocumentDARNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);

    Response response = resource.getIrbDocument(new DuosUser(authUser, user), "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetIrbDocumentNullOrEmptyValues() {
    Response response = resource.getIrbDocument(new DuosUser(authUser, user), "");
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

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUploadIrbDocumentDARNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUploadIrbDocumentWithPreviousIrbDocument() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setIrbDocumentLocation(randomAlphabetic(10));
    dar.getData().setIrbDocumentName(randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);

    Response response = resource.uploadIrbDocument(authUser, "", uploadInputStream, formData);
    assertEquals(200, response.getStatus());
  }

  private Pair<InputStream, FormDataContentDisposition> mockFormDataMultiPart(String fileName) {
    String name = FilenameUtils.removeExtension(fileName);
    InputStream inputStream = IOUtils.toInputStream(name, Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    return Pair.of(inputStream, formData);
  }

  private void mockProgressReportUserAndParentDar(DataAccessRequest parentDar) {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(parentDar);
  }

  private void mockNoOpenProgressReportElections(DataAccessRequest parentDar) {
    when(dataAccessRequestService.findOpenElectionsByReferenceId(parentDar.getReferenceId()))
        .thenReturn(List.of());
  }

  @Test
  void testPostProgressReportCollabAndEthicsFiles() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    mockProgressReportUserAndParentDar(parentDar);
    mockNoOpenProgressReportElections(parentDar);
    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createProgressReport(eq(user), any(), eq(parentDar), eq(request)))
        .thenReturn(childDar);
    // datasets retrieved for the compliance logger
    when(datasetService.findDatasetsByIds(user, childDar.getDatasetIds())).thenReturn(List.of());
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");

    try (var response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            "",
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testPostProgressReportDifferentUser() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setUserId(2);
    mockProgressReportUserAndParentDar(parentDar);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");

    Response response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            "",
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testPostProgressReportMissingParentDar() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenThrow(NotFoundException.class);
    Pair<InputStream, FormDataContentDisposition> collabFile = mockFormDataMultiPart("collab.txt");
    Pair<InputStream, FormDataContentDisposition> ethicsFile = mockFormDataMultiPart("ethics.txt");

    try (var response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            "",
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testPostProgressReportInvalidJson() {
    String invalidDar = "{\"projectTitle\": \"test\", \"datasetIds\": \"invalid\"}";
    DataAccessRequest parentDar = generateDataAccessRequest();
    mockProgressReportUserAndParentDar(parentDar);
    mockNoOpenProgressReportElections(parentDar);
    var collabFile = mockFormDataMultiPart("collab.txt");
    var ethicsFile = mockFormDataMultiPart("ethics.txt");

    try (var response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            invalidDar,
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
      assertTrue(response.getEntity().toString().contains("Unable to parse DAR from JSON string"));
    }
  }

  @Test
  void testPostProgressReportThrowsWhenNoERACommonsID() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(BadRequestException.class).when(userService).validateActiveERACredentials(user);

    try (var response =
        resource.postProgressReport(duosUser, request, "", "", null, null, null, null)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPostProgressReportWithOpenElections() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    mockProgressReportUserAndParentDar(parentDar);
    Election election = new Election();
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionType(ElectionType.DATA_ACCESS.getValue());
    election.setReferenceId(parentDar.getReferenceId());
    when(dataAccessRequestService.findOpenElectionsByReferenceId(parentDar.getReferenceId()))
        .thenReturn(List.of(election));
    var collabFile = mockFormDataMultiPart("collab.txt");
    var ethicsFile = mockFormDataMultiPart("ethics.txt");

    try (var response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            "",
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
      assertTrue(
          response
              .getEntity()
              .toString()
              .contains(
                  "Cannot create a progress report for a DAR with an open election: "
                      + parentDar.getReferenceId()));
    }
  }

  @Test
  void testPostProgressReportFailsWhenDAARestricted() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    mockProgressReportUserAndParentDar(parentDar);
    mockNoOpenProgressReportElections(parentDar);
    doThrow(new ForbiddenException("DAA restriction violated"))
        .when(dataAccessRequestService)
        .createProgressReport(eq(user), any(), eq(parentDar), eq(request));

    var collabFile = mockFormDataMultiPart("collab.txt");
    var ethicsFile = mockFormDataMultiPart("ethics.txt");

    try (var response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            "",
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight())) {

      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }

    verify(dataAccessRequestService, times(1))
        .createProgressReport(eq(user), any(), eq(parentDar), eq(request));
  }

  @Test
  void testPostProgressReportDelegatesToCreateProgressReport() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    mockProgressReportUserAndParentDar(parentDar);
    mockNoOpenProgressReportElections(parentDar);

    DataAccessRequest childDar = generateDataAccessRequest();
    when(dataAccessRequestService.createProgressReport(eq(user), any(), eq(parentDar), eq(request)))
        .thenReturn(childDar);

    when(datasetService.findDatasetsByIds(user, childDar.getDatasetIds())).thenReturn(List.of());

    var collabFile = mockFormDataMultiPart("collab.txt");
    var ethicsFile = mockFormDataMultiPart("ethics.txt");

    try (var response =
        resource.postProgressReport(
            duosUser,
            request,
            "",
            "",
            collabFile.getLeft(),
            collabFile.getRight(),
            ethicsFile.getLeft(),
            ethicsFile.getRight())) {

      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }

    verify(dataAccessRequestService)
        .createProgressReport(eq(user), any(), eq(parentDar), eq(request));
  }

  @Test
  void populateProgressReportWithDocuments() throws Exception {
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.getData().setCollaborationLetterLocation("existing_collab_location");
    parentDar.getData().setIrbDocumentLocation("existing_irb_location");
    DataAccessRequest childDar = generateDataAccessRequest();
    childDar.setDatasetIds(List.of(1, 2));

    Dataset dataset1 = new Dataset();
    DataUse dataUse1 =
        new DataUseBuilder().setCollaboratorRequired(true).setEthicsApprovalRequired(true).build();
    dataset1.setDataUse(dataUse1); // Both documents required
    Dataset dataset2 = new Dataset();
    DataUse dataUse2 =
        new DataUseBuilder()
            .setCollaboratorRequired(false)
            .setEthicsApprovalRequired(false)
            .build();
    dataset2.setDataUse(dataUse2); // No documents required

    when(datasetService.findDatasetById(mockUser, 1)).thenReturn(dataset1);
    when(datasetService.findDatasetById(mockUser, 2)).thenReturn(dataset2);

    String fileType = "text/plain";
    InputStream collabInputStream =
        IOUtils.toInputStream("collab content", Charset.defaultCharset());
    FormDataContentDisposition collabFileDetails = mock(FormDataContentDisposition.class);
    when(collabFileDetails.getFileName()).thenReturn("collab_document.txt");
    when(collabFileDetails.getType()).thenReturn(fileType);

    InputStream ethicsInputStream =
        IOUtils.toInputStream("ethics content", Charset.defaultCharset());
    FormDataContentDisposition ethicsFileDetails = mock(FormDataContentDisposition.class);
    when(ethicsFileDetails.getFileName()).thenReturn("ethics_document.txt");
    when(ethicsFileDetails.getType()).thenReturn(fileType);

    BlobId blobId = BlobId.of("bucket", "location");
    when(gcsService.storeDocument(eq(collabInputStream), eq(fileType), any())).thenReturn(blobId);
    when(gcsService.storeDocument(eq(ethicsInputStream), eq(fileType), any())).thenReturn(blobId);
    resource.populateProgressReportWithDocuments(
        mockUser,
        collabInputStream,
        collabFileDetails,
        ethicsInputStream,
        ethicsFileDetails,
        childDar,
        parentDar);
    verify(gcsService, times(2)).storeDocument(any(), any(), any());
  }

  @Test
  void populateProgressReportWithDocumentsMissingCollaboration() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.getData().setCollaborationLetterLocation(null);

    DataAccessRequest childDar = generateDataAccessRequest();
    childDar.setDatasetIds(List.of(1));

    Dataset dataset = new Dataset();
    DataUse dataUse =
        new DataUseBuilder().setCollaboratorRequired(true).setEthicsApprovalRequired(false).build();
    dataset.setDataUse(dataUse); // Collaboration document required
    when(datasetService.findDatasetById(mockUser, 1)).thenReturn(dataset);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                resource.populateProgressReportWithDocuments(
                    mockUser, null, null, null, null, childDar, parentDar));

    assertEquals("Collaboration document is required", exception.getMessage());
  }

  @Test
  void populateProgressReportWithDocumentsMissingEthics() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.getData().setIrbDocumentLocation(null);

    DataAccessRequest childDar = generateDataAccessRequest();
    childDar.setDatasetIds(List.of(1));

    // Mock dataset
    Dataset dataset = new Dataset();
    DataUse dataUse =
        new DataUseBuilder().setCollaboratorRequired(false).setEthicsApprovalRequired(true).build();
    dataset.setDataUse(dataUse); // Ethics approval document required
    when(datasetService.findDatasetById(mockUser, 1)).thenReturn(dataset);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                resource.populateProgressReportWithDocuments(
                    mockUser, null, null, null, null, childDar, parentDar));

    assertEquals("Ethics approval document is required", exception.getMessage());
  }

  @Test
  void populateProgressReportWithDocumentsMissingDataUse() {
    DataAccessRequest parentDar = generateDataAccessRequest();

    DataAccessRequest childDar = generateDataAccessRequest();
    childDar.setDatasetIds(List.of(1));

    Dataset dataset = new Dataset();
    dataset.setDataUse(null); // Ethics approval document required
    when(datasetService.findDatasetById(mockUser, 1)).thenReturn(dataset);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                resource.populateProgressReportWithDocuments(
                    mockUser, null, null, null, null, childDar, parentDar));

    assertEquals("Dataset 1 is missing data use(s)", exception.getMessage());
  }

  @Test
  void populateProgressReportWithDocumentsDatasetNotFound() {
    DataAccessRequest parentDar = generateDataAccessRequest();
    DataAccessRequest childDar = generateDataAccessRequest();
    childDar.setDatasetIds(List.of(1));
    when(datasetService.findDatasetById(mockUser, 1)).thenReturn(null);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                resource.populateProgressReportWithDocuments(
                    mockUser, null, null, null, null, childDar, parentDar));

    assertEquals("Dataset 1 not found", exception.getMessage());
  }

  /**
   * Provides a stream of DataUse objects for testing the `populateProgressReportWithDocuments`
   * method. Each DataUse object has a different property set to true. `ethicsApprovalRequired` and
   * `collaboratorRequired` require special handling by the method under test and are covered in
   * other tests, so they are not included here.
   */
  private static Stream<Arguments> dataUseProvider() {
    return Stream.of(
        Arguments.of(new DataUseBuilder().setGeneralUse(true).build()),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).build()),
        Arguments.of(
            new DataUseBuilder().setDiseaseRestrictions(List.of("Cancer", "Diabetes")).build()),
        Arguments.of(new DataUseBuilder().setPopulationOriginsAncestry(true).build()),
        Arguments.of(new DataUseBuilder().setMethodsResearch(true).build()),
        Arguments.of(new DataUseBuilder().setNonProfitUse(true).build()),
        Arguments.of(new DataUseBuilder().setOther("Other").build()),
        Arguments.of(new DataUseBuilder().setSecondaryOther("Other").build()),
        Arguments.of(new DataUseBuilder().setGeographicalRestrictions("Geography").build()),
        Arguments.of(new DataUseBuilder().setGeneticStudiesOnly(true).build()),
        Arguments.of(new DataUseBuilder().setPublicationResults(true).build()),
        Arguments.of(new DataUseBuilder().setPublicationMoratorium("Publication").build()),
        Arguments.of(new DataUseBuilder().setControl(true).build()),
        Arguments.of(new DataUseBuilder().setGender("Gender").build()),
        Arguments.of(new DataUseBuilder().setPediatric(true).build()),
        Arguments.of(new DataUseBuilder().setPopulation(true).build()),
        Arguments.of(new DataUseBuilder().setIllegalBehavior(true).build()),
        Arguments.of(new DataUseBuilder().setSexualDiseases(true).build()),
        Arguments.of(new DataUseBuilder().setStigmatizeDiseases(true).build()),
        Arguments.of(new DataUseBuilder().setVulnerablePopulations(true).build()),
        Arguments.of(new DataUseBuilder().setPsychologicalTraits(true).build()),
        Arguments.of(new DataUseBuilder().setNotHealth(true).build()));
  }

  @ParameterizedTest
  @MethodSource("dataUseProvider")
  void testPopulateProgressReportWithDocumentsAndValidDataUse(DataUse dataUse) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(dataUse);
    when(datasetService.findDatasetById(mockUser, 1)).thenReturn(dataset);

    DataAccessRequest parentDar = generateDataAccessRequest();
    parentDar.setDatasetIds(List.of(dataset.getDatasetId()));
    DataAccessRequest childDar = generateDataAccessRequest();
    childDar.setDatasetIds(List.of(dataset.getDatasetId()));

    assertDoesNotThrow(
        () ->
            resource.populateProgressReportWithDocuments(
                mockUser, null, null, null, null, childDar, parentDar));
  }

  @ParameterizedTest
  @EnumSource(DarDocumentType.class)
  void uploadDocumentContents(DarDocumentType documentType) throws Exception {
    InputStream uploadInputStream = IOUtils.toInputStream("test content", Charset.defaultCharset());
    FormDataContentDisposition fileDetail = mock(FormDataContentDisposition.class);
    String fileName = "document.txt";
    when(fileDetail.getFileName()).thenReturn(fileName);
    String fileType = "text/plain";
    when(fileDetail.getType()).thenReturn(fileType);

    DataAccessRequest dar = generateDataAccessRequest();
    String existingLocation = "existing_location";
    if (DarDocumentType.COLLABORATION.equals(documentType)) {
      dar.getData().setCollaborationLetterLocation(existingLocation);
      dar.getData().setCollaborationLetterName("existing_name.txt");
    } else {
      dar.getData().setIrbDocumentLocation(existingLocation);
      dar.getData().setIrbDocumentName("existing_name.txt");
    }

    String newLocation = "new_location";
    BlobId mockBlobId = BlobId.of("bucket", newLocation);
    when(gcsService.storeDocument(eq(uploadInputStream), eq(fileType), any()))
        .thenReturn(mockBlobId);
    when(gcsService.deleteDocument(existingLocation)).thenReturn(true);

    resource.uploadDocumentContents(documentType, dar, uploadInputStream, fileDetail);

    if (DarDocumentType.COLLABORATION.equals(documentType)) {
      assertEquals(newLocation, dar.getData().getCollaborationLetterLocation());
      assertEquals(fileName, dar.getData().getCollaborationLetterName());
    } else {
      assertEquals(newLocation, dar.getData().getIrbDocumentLocation());
      assertEquals(fileName, dar.getData().getIrbDocumentName());
    }
  }

  @Test
  void testGetCollaborationDocument() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setCollaborationLetterLocation(randomAlphabetic(10));
    dar.getData().setCollaborationLetterName(randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);

    // For non-creator chair/member: stub DAC membership so validation passes
    Dac dacWithAll = new Dac();
    dacWithAll.setChairpersons(List.of(chairperson));
    dacWithAll.setMembers(List.of(member));
    when(dacService.findByDatasetId(any())).thenReturn(Set.of(dacWithAll));

    assertEquals(
        200,
        resource
            .getCollaborationDocument(new DuosUser(chairpersonUser, chairperson), "")
            .getStatus());
    assertEquals(
        200, resource.getCollaborationDocument(new DuosUser(adminUser, admin), "").getStatus());
    assertEquals(
        200, resource.getCollaborationDocument(new DuosUser(memberUser, member), "").getStatus());
    assertEquals(
        200, resource.getCollaborationDocument(new DuosUser(authUser, user), "").getStatus());
    assertEquals(
        403, resource.getCollaborationDocument(new DuosUser(anotherUser, bob), "").getStatus());
  }

  @Test
  void testGetCollaborationDocumentNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(generateDataAccessRequest());

    Response response = resource.getCollaborationDocument(new DuosUser(authUser, user), "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetCollaborationDocumentDARNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);

    Response response = resource.getCollaborationDocument(new DuosUser(authUser, user), "");
    assertEquals(404, response.getStatus());
  }

  @Test
  void testGetCollaborationDocumentNullOrEmptyValues() {

    Response response = resource.getCollaborationDocument(new DuosUser(authUser, user), "");
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

    Response response =
        resource.uploadCollaborationDocument(authUser, "", uploadInputStream, formData);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testUploadCollaborationDocumentDARNotFound() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);

    Response response =
        resource.uploadCollaborationDocument(authUser, "", uploadInputStream, formData);
    assertEquals(404, response.getStatus());
  }

  @Test
  void testUploadCollaborationDocumentWithPreviousDocument() throws Exception {
    when(userService.findUserByEmail(any())).thenReturn(user);
    DataAccessRequest dar = generateDataAccessRequest();
    dar.getData().setCollaborationLetterLocation(randomAlphabetic(10));
    dar.getData().setCollaborationLetterName(randomAlphabetic(10) + ".txt");
    when(dataAccessRequestService.updateByReferenceId(any(), any())).thenReturn(dar);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    InputStream uploadInputStream = IOUtils.toInputStream("test", Charset.defaultCharset());
    FormDataContentDisposition formData = mock(FormDataContentDisposition.class);
    when(formData.getFileName()).thenReturn("temp.txt");
    when(formData.getType()).thenReturn("txt");
    when(formData.getSize()).thenReturn(1L);
    when(gcsService.storeDocument(any(), any(), any())).thenReturn(BlobId.of("bucket", "name"));
    when(gcsService.deleteDocument(any())).thenReturn(true);

    Response response =
        resource.uploadCollaborationDocument(authUser, "", uploadInputStream, formData);
    assertEquals(200, response.getStatus());
  }

  private DataAccessRequest generateDataAccessRequest() {
    Timestamp now = new Timestamp(new Date().getTime());
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setReferenceId(UUID.randomUUID().toString());
    data.setReferenceId(dar.getReferenceId());
    dar.setDarCode("DAR-" + randomInt(100, 500));
    dar.setId(new Random().nextInt());
    dar.setDatasetIds(Arrays.asList(1, 2));
    dar.setData(data);
    dar.setUserId(user.getUserId());
    dar.setCreateDate(now);
    dar.setUpdateDate(now);
    return dar;
  }

  @Test
  void getDataAccessRequests() {
    List<DataAccessRequest> list = Collections.emptyList();
    when(dataAccessRequestService.getDataAccessRequestsByUserRole(any())).thenReturn(list);
    Response res = resource.getDataAccessRequests(new DuosUser(authUser, new User()));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  void getDraftDataAccessRequests() {
    List<DataAccessRequest> list = Collections.emptyList();
    User localUser = new User();
    localUser.setUserId(1);
    when(userService.findUserByEmail(any())).thenReturn(localUser);
    when(dataAccessRequestService.findAllDraftDataAccessRequestsByUser(any())).thenReturn(list);
    Response res = resource.getDraftDataAccessRequests(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  void getDraftDataAccessRequests_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    resource.getDraftDataAccessRequests(authUser);
    Response res = resource.getDraftDataAccessRequests(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, res.getStatus());
  }

  @Test
  void getDraftDar() {
    User localUser = new User();
    localUser.setUserId(10);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(10);
    when(userService.findUserByEmail(any())).thenReturn(localUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, res.getStatus());
    assertTrue(res.hasEntity());
  }

  @Test
  void getDraftDar_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, res.getStatus());
  }

  @Test
  void getDraftDar_DarNotFound() {
    User localUser = new User();
    localUser.setUserId(10);
    when(userService.findUserByEmail(any())).thenReturn(localUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenThrow(new NotFoundException());
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, res.getStatus());
  }

  @Test
  void getDraftDar_UserNotAllowed() {
    User localUser = new User();
    localUser.setUserId(10);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setUserId(11);
    when(userService.findUserByEmail(any())).thenReturn(localUser);
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    Response res = resource.getDraftDar(authUser, "id");
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, res.getStatus());
  }

  @Test
  void testCreateDataAccessRequestWithDAARestrictions() {
    try {
      User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
      userWithCards.setLibraryCard(new LibraryCard());
      when(userService.findUserByEmail(any())).thenReturn(userWithCards);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
      when(dataAccessRequestService.insertDraftDataAccessRequest(any(), any())).thenReturn(dar);
      when(builder.path(anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(URI.create("https://test.domain.org/some/path"));
      when(info.getRequestUriBuilder()).thenReturn(builder);
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (Response response = resource.createDraftDataAccessRequest(authUser, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

  @Test
  void testCreateDataAccessRequestWithDAARestrictionsFailure() {
    try {
      User userWithCards = new User(1, authUser.getEmail(), "Display Name", new Date(), roles);
      userWithCards.setLibraryCard(new LibraryCard());
      when(userService.findUserByEmail(any())).thenReturn(userWithCards);
      DataAccessRequest dar = new DataAccessRequest();
      dar.setReferenceId(UUID.randomUUID().toString());
      dar.setCollectionId(1);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setReferenceId(dar.getReferenceId());
      dar.setData(data);
      doThrow(BadRequestException.class)
          .when(dataAccessRequestService)
          .insertDraftDataAccessRequest(any(), any());
    } catch (Exception e) {
      fail("Initialization Exception: " + e.getMessage());
    }

    try (Response response = resource.createDraftDataAccessRequest(authUser, info, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testGetDAAsByReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(daaService.findByDarReferenceId(any())).thenReturn(List.of());

    try (Response response =
        resource.getDAAsByReferenceId(new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetDAAsByReferenceIdNotFound() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);

    try (Response response =
        resource.getDAAsByReferenceId(new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetDatasetDaaSnapshotsByReferenceId() {
    DataAccessRequest dar = generateDataAccessRequest();
    Timestamp capturedAt = Timestamp.from(Instant.now());
    Map<Integer, DatasetDaaSnapshot> snapshots =
        Map.of(
            1, new DatasetDaaSnapshot(10, capturedAt), 2, new DatasetDaaSnapshot(20, capturedAt));
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.findDatasetDaaSnapshotsByReferenceId(any()))
        .thenReturn(snapshots);

    try (Response response =
        resource.getDatasetDaaSnapshotsByReferenceId(
            new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(snapshots, response.getEntity());
    }
  }

  @Test
  void testGetDatasetDaaSnapshotsByReferenceIdDarNotFound() {
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(null);

    try (Response response =
        resource.getDatasetDaaSnapshotsByReferenceId(new DuosUser(authUser, user), "missing")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetDatasetDaaSnapshotsByReferenceIdSnapshotNotFound() {
    DataAccessRequest dar = generateDataAccessRequest();
    when(dataAccessRequestService.findByReferenceId(any())).thenReturn(dar);
    when(dataAccessRequestService.findDatasetDaaSnapshotsByReferenceId(any()))
        .thenThrow(new NotFoundException("No snapshot"));

    try (Response response =
        resource.getDatasetDaaSnapshotsByReferenceId(
            new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  // ── validateAuthedRoleUser – DAC membership enforcement ─────────────────────────────────────

  /** Builds a DAR owned by a *different* user (userId = 99) with one dataset. */
  private DataAccessRequest buildDarOwnedByOther() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(99); // not the chairperson/member/SO under test
    dar.setDatasetIds(List.of(101));
    return dar;
  }

  /** Builds a Dac whose chairpersons list contains the given user. */
  private Dac dacWithChair(User chair) {
    Dac dac = new Dac();
    dac.setChairpersons(List.of(chair));
    dac.setMembers(List.of());
    return dac;
  }

  /** Builds a Dac whose members list contains the given user. */
  private Dac dacWithMember(User member) {
    Dac dac = new Dac();
    dac.setChairpersons(List.of());
    dac.setMembers(List.of(member));
    return dac;
  }

  @Test
  void testGetByReferenceId_Chairperson_InDac_Allowed() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(dacService.findByDatasetId(dar.getDatasetIds()))
        .thenReturn(Set.of(dacWithChair(chairperson)));

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(chairpersonUser, chairperson), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Chairperson_NotInDac_Forbidden() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    // The DAC for the dataset has no members/chairs that match the chairperson
    Dac emptyDac = new Dac();
    emptyDac.setChairpersons(List.of());
    emptyDac.setMembers(List.of());
    when(dacService.findByDatasetId(dar.getDatasetIds())).thenReturn(Set.of(emptyDac));

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(chairpersonUser, chairperson), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Chairperson_DarHasNoDatasets_Forbidden() {
    DataAccessRequest dar = buildDarOwnedByOther();
    dar.setDatasetIds(List.of());
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(chairpersonUser, chairperson), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Member_InDac_Allowed() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(dacService.findByDatasetId(dar.getDatasetIds())).thenReturn(Set.of(dacWithMember(member)));

    try (Response response =
        resource.getByReferenceId(new DuosUser(memberUser, member), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Member_NotInDac_Forbidden() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    Dac emptyDac = new Dac();
    emptyDac.setChairpersons(List.of());
    emptyDac.setMembers(List.of());
    when(dacService.findByDatasetId(dar.getDatasetIds())).thenReturn(Set.of(emptyDac));

    try (Response response =
        resource.getByReferenceId(new DuosUser(memberUser, member), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Member_InDac_WithNullChairList_Allowed() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    Dac dac = new Dac();
    dac.setChairpersons(null);
    dac.setMembers(List.of(member));
    when(dacService.findByDatasetId(dar.getDatasetIds())).thenReturn(Set.of(dac));

    try (Response response =
        resource.getByReferenceId(new DuosUser(memberUser, member), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Chairperson_InDac_WithNullMemberList_Allowed() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    Dac dac = new Dac();
    dac.setChairpersons(List.of(chairperson));
    dac.setMembers(null);
    when(dacService.findByDatasetId(dar.getDatasetIds())).thenReturn(Set.of(dac));

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(chairpersonUser, chairperson), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Chairperson_DarHasNullDatasets_Forbidden() {
    DataAccessRequest dar = buildDarOwnedByOther();
    dar.setDatasetIds(null);
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(chairpersonUser, chairperson), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Member_DacHasNullMembers_Forbidden() {
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    Dac dac = new Dac();
    dac.setChairpersons(List.of());
    dac.setMembers(null);
    when(dacService.findByDatasetId(dar.getDatasetIds())).thenReturn(Set.of(dac));

    try (Response response =
        resource.getByReferenceId(new DuosUser(memberUser, member), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Admin_BypassesDacCheck() {
    // Admin should never be subject to the DAC-membership check
    DataAccessRequest dar = buildDarOwnedByOther();
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);

    try (Response response =
        resource.getByReferenceId(new DuosUser(adminUser, admin), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_Creator_BypassesDacCheck() {
    // The DAR creator (userId == user.getUserId()) bypasses all additional checks
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(user.getUserId());
    dar.setDatasetIds(List.of(101));
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);

    try (Response response =
        resource.getByReferenceId(new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_InvalidDarUserId_AdminAllowed() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(null);
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);

    try (Response response =
        resource.getByReferenceId(new DuosUser(adminUser, admin), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_NonPositiveDarUserId_AdminAllowed() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(0);
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);

    try (Response response =
        resource.getByReferenceId(new DuosUser(adminUser, admin), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  // ── validateAuthedRoleUser – Signing Official institution enforcement ─────────────────────────

  private User buildSigningOfficial(int userId, Integer institutionId) {
    AuthUser soAuthUser = new AuthUser("so_" + userId + "@test.com");
    List<UserRole> soRoles = List.of(UserRoles.SigningOfficial());
    User so = new User(userId, soAuthUser.getEmail(), "SO User", new Date(), soRoles);
    so.setInstitutionId(institutionId);
    return so;
  }

  @Test
  void testGetByReferenceId_SigningOfficial_SameInstitution_Allowed() {
    User so = buildSigningOfficial(20, 5);
    User darCreator = new User(99, "creator@test.com", "Creator", new Date(), roles);
    darCreator.setInstitutionId(5); // same institution

    DataAccessRequest dar = buildDarOwnedByOther();
    dar.setUserId(darCreator.getUserId());
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(userService.findUserById(darCreator.getUserId())).thenReturn(darCreator);

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(new AuthUser(so.getEmail()), so), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_SigningOfficial_DifferentInstitution_Forbidden() {
    User so = buildSigningOfficial(20, 5);
    User darCreator = new User(99, "creator@test.com", "Creator", new Date(), roles);
    darCreator.setInstitutionId(99); // different institution

    DataAccessRequest dar = buildDarOwnedByOther();
    dar.setUserId(darCreator.getUserId());
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(userService.findUserById(darCreator.getUserId())).thenReturn(darCreator);

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(new AuthUser(so.getEmail()), so), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_SigningOfficial_NoInstitution_Forbidden() {
    // SO has no institution set
    User so = buildSigningOfficial(20, null);
    User darCreator = new User(99, "creator@test.com", "Creator", new Date(), roles);
    darCreator.setInstitutionId(5);

    DataAccessRequest dar = buildDarOwnedByOther();
    dar.setUserId(darCreator.getUserId());
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(userService.findUserById(darCreator.getUserId())).thenReturn(darCreator);

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(new AuthUser(so.getEmail()), so), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testGetByReferenceId_SigningOfficial_CreatorNotFound_Forbidden() {
    User so = buildSigningOfficial(20, 5);

    DataAccessRequest dar = buildDarOwnedByOther();
    dar.setUserId(99);
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(userService.findUserById(99)).thenReturn(null); // creator not resolvable

    try (Response response =
        resource.getByReferenceId(
            new DuosUser(new AuthUser(so.getEmail()), so), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testApproveCloseout() {
    String referenceId = UUID.randomUUID().toString();
    doNothing().when(dataAccessRequestService).approveDataAccessRequestCloseout(user, referenceId);
    when(dataAccessRequestService.findByReferenceId(referenceId))
        .thenReturn(new DataAccessRequest());
    when(datasetService.findDatasetsByIds(user, List.of())).thenReturn(List.of());
    try (Response response = resource.approveCloseout(duosUser, request, referenceId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testApproveCloseoutThrows() {
    String referenceId = UUID.randomUUID().toString();
    doThrow(BadRequestException.class)
        .when(dataAccessRequestService)
        .approveDataAccessRequestCloseout(user, referenceId);
    try (Response response = resource.approveCloseout(duosUser, request, referenceId)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteDraftDataAccessRequestForDraft() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(user.getUserId());
    dar.setReferenceId(UUID.randomUUID().toString());
    assertTrue(dar.getDraft());
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    doNothing().when(dataAccessRequestService).deleteDataAccessRequest(dar);
    try (Response response =
        resource.deleteDar(new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteDraftDataAccessRequestThrowsForSubmittedDar() {
    DataAccessRequest dar = generateDataAccessRequest();
    dar.setUserId(user.getUserId());
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    assertFalse(dar.getDraft());
    when(dataAccessRequestService.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    doThrow(BadRequestException.class).when(dataAccessRequestService).deleteDataAccessRequest(dar);
    try (Response response =
        resource.deleteDar(new DuosUser(authUser, user), dar.getReferenceId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }
}
