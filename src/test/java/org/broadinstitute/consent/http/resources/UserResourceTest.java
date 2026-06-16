package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.SamAzureB2CException;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.CreateDuosUserRequest;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.feature.InstitutionAndLibraryCardEnforcement;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementExceptions;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
class UserResourceTest extends AbstractTestHelper {

  @Mock private UserService userService;

  @Mock private SamService samService;

  @Mock private DatasetService datasetService;

  private UserResource userResource;

  @Mock private UriInfo uriInfo;

  @Mock private UriBuilder uriBuilder;

  @Mock private UserStatusInfo userStatusInfo;

  @Mock private AcknowledgementService acknowledgementService;

  @Mock private NihService nihService;

  @Mock private ServicesConfiguration servicesConfiguration;

  @Mock private InstitutionAndLibraryCardEnforcement institutionAndLibraryCardEnforcement;

  private static final String TEST_EMAIL = "test@gmail.com";

  private final Gson gson = GsonUtil.getInstance();

  private final AuthUser authUser =
      new AuthUser()
          .setAuthToken("auth-token")
          .setName("Test User")
          .setEmail(TEST_EMAIL)
          .setUserStatusInfo(userStatusInfo);

  private final DuosUser duosUser =
      new DuosUser(
          authUser, new User(1, TEST_EMAIL, "Test User", new Date(), Collections.emptyList()));

  @BeforeEach
  void initResource() {
    userResource =
        new UserResource(
            samService,
            userService,
            datasetService,
            acknowledgementService,
            nihService,
            servicesConfiguration,
            institutionAndLibraryCardEnforcement);
  }

  @Test
  void testGetMe() throws Exception {
    User user = createUserWithRole();
    DuosUser du = new DuosUser(authUser, user);

    Response response = userResource.getUser(du);
    verify(samService).asyncPostRegistrationInfo(du);
    verify(samService).getCombinedUserStatusInfo(du);
    verify(nihService).syncAccount(du);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetMeWithUserStatusInfo() throws Exception {
    User user = createUserWithRole();
    DuosUser du = new DuosUser(authUser, user);
    UserStatusInfo info =
        new UserStatusInfo()
            .setUserEmail(user.getEmail())
            .setUserSubjectId("test-subject-id")
            .setEnabled(true)
            .setTosAccepted(true);
    du.setUserStatusInfo(info);
    when(nihService.syncAccount(du)).thenReturn(user);

    Response response = userResource.getUser(du);
    User responseUser = gson.fromJson(response.getEntity().toString(), User.class);
    assertEquals(user.getEmail(), responseUser.getEmail());
    assertNotNull(responseUser.getUserStatusInfo());
    assertEquals(info.getUserSubjectId(), responseUser.getUserStatusInfo().getUserSubjectId());
    assertTrue(responseUser.getUserStatusInfo().getEnabled());
    assertTrue(responseUser.getUserStatusInfo().getTosAccepted());
    verify(samService, never()).asyncPostRegistrationInfo(du);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetMeSamFailure() throws Exception {
    User user = createUserWithRole();
    DuosUser du = new DuosUser(authUser, user);
    when(samService.getCombinedUserStatusInfo(du)).thenThrow(new RuntimeException("Sam failure"));

    Response response = userResource.getUser(du);
    // Ensure that even if Sam fails, we still return the user information.
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetMe_SamAzureB2CException_Returns500() throws Exception {
    // SamAzureB2CException should NOT be silently swallowed - it propagates and returns a 500
    User user = createUserWithRole();
    DuosUser du = new DuosUser(authUser, user);
    when(samService.getCombinedUserStatusInfo(du))
        .thenThrow(new SamAzureB2CException("AzureB2C error for user test@test.org"));

    Response response = userResource.getUser(du);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetMe_SamAzureB2CException_ErrorMessageContainsDetails() throws Exception {
    // The error message from SamAzureB2CException should be returned in the response
    User user = createUserWithRole();
    DuosUser du = new DuosUser(authUser, user);
    String errorMessage =
        "AzureB2C authentication Error for user test@test.org: Please contact support.";
    when(samService.getCombinedUserStatusInfo(du))
        .thenThrow(new SamAzureB2CException(errorMessage));

    Response response = userResource.getUser(du);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertTrue(response.getEntity().toString().contains(errorMessage));
  }

  @Test
  void testGetUserById() {

    Response response = userResource.getUserById(duosUser, 1);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUserByIdNotFound() {
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any()))
        .thenThrow(new NotFoundException());

    Response response = userResource.getUserById(duosUser, 1);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_SO() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.getUsersAsRole(user, "SigningOfficial"))
        .thenReturn(Arrays.asList(new User(), new User()));

    Response response = userResource.getUsers(authUser, "SigningOfficial");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_SO_NoRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);

    Response response = userResource.getUsers(authUser, "SigningOfficial");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_Admin() {
    User user = createUserWithRole();
    user.setAdminRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.getUsersAsRole(user, "Admin"))
        .thenReturn(Arrays.asList(new User(), new User()));

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_Admin_NoRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_UnsupportedRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);

    Response response = userResource.getUsers(authUser, "Researcher");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_InvalidRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);

    Response response = userResource.getUsers(authUser, "BadRequest");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testCreateExistingUser() {
    User user = new User();
    user.setEmail(TEST_EMAIL);
    user.addRole(UserRoles.Admin());
    user.addRole(UserRoles.Researcher());
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);

    try (Response response = userResource.createResearcher(uriInfo, authUser)) {
      assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testCreateFailingGoogleIdentity() {
    try (Response response = userResource.createResearcher(uriInfo, new AuthUser(TEST_EMAIL))) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void createUserSuccess() throws Exception {
    User user = new User();
    user.setDisplayName("Test");
    user.setEmail(TEST_EMAIL);
    user.setResearcherRole();
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/dacuser/api"));
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    when(userService.createUser(user)).thenReturn(user);

    try (Response response = userResource.createResearcher(uriInfo, authUser)) {
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testAddRoleToUser() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserNotFound() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    doThrow(new NotFoundException()).when(userService).findUserById(any());

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(404, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserNotModified() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(304, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBadRequest() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);

    try (Response response = userResource.addRoleToUser(activeDuosUser, 1, 1000)) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoWithoutUserAndSoInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoInstitutionWithoutUserInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    activeUser.setSigningOfficialRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoWithoutSoInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    user.setInstitutionId(10);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoWithDeniedRoles() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    user.setInstitutionId(10);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), user);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(400, response.getStatus());
    }
    try (Response response2 =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(400, response2.getStatus());
    }
    try (Response response3 =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.MEMBER.getRoleId())) {
      assertEquals(400, response3.getStatus());
    }
    try (Response response4 =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.CHAIRPERSON.getRoleId())) {
      assertEquals(400, response4.getStatus());
    }
    try (Response response5 =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.ALUMNI.getRoleId())) {
      assertEquals(400, response5.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoWithPermittedRoles() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    user.setInstitutionId(10);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(200, response.getStatus());
    }
    try (Response response2 =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(200, response2.getStatus());
    }
    try (Response response3 =
        userResource.addRoleToUser(activeDuosUser, 1, UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(200, response3.getStatus());
    }
  }

  @SuppressWarnings({"unchecked"})
  @Test
  void testGetSOsForInstitution() {
    User user = createUserWithInstitution();
    User so = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findSOsByInstitutionId(any()))
        .thenReturn(
            Arrays.asList(new UserService.SimplifiedUser(so), new UserService.SimplifiedUser(so)));

    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List<UserService.SimplifiedUser>) response.getEntity();
    assertFalse(body.isEmpty());
    assertEquals(so.getDisplayName(), body.getFirst().getDisplayName());
  }

  @SuppressWarnings("rawtypes")
  @Test
  void testGetSOsForInstitution_NoInstitution() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);

    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List) response.getEntity();
    assertTrue(body.isEmpty());
  }

  @Test
  void testGetSOsForInstitution_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());

    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testGetSigningOfficialsByInstitution_AsAdmin_DifferentInstitution() {
    // Admin belongs to institution 1 but queries institution 99 — must succeed.
    Integer queriedInstitutionId = 99;
    User admin = createUserWithInstitution(); // institutionId = 1
    admin.addRole(UserRoles.Admin());
    UserService.SigningOfficialUser so = new UserService.SigningOfficialUser();
    so.setUserId(2);
    so.setDisplayName("SO User");
    so.setEmail("so@test.com");
    so.setInstitutionId(queriedInstitutionId);
    so.setUserData(Map.of("department", "biology"));
    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findSOsWithDataByInstitutionId(queriedInstitutionId)).thenReturn(List.of(so));

    Response response =
        userResource.getSigningOfficialsByInstitution(authUser, queriedInstitutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    List<UserService.SigningOfficialUser> body =
        (List<UserService.SigningOfficialUser>) response.getEntity();
    assertEquals(1, body.size());
    assertEquals(so.getDisplayName(), body.getFirst().getDisplayName());
    // Confirm the result belongs to the queried institution, not the admin's own.
    assertEquals(queriedInstitutionId, body.getFirst().getInstitutionId());
    assertEquals(so.getUserData(), body.getFirst().getUserData());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testGetSigningOfficialsByInstitution_AsChairperson_DifferentInstitution() {
    // Chairperson belongs to institution 3 but queries institution 99 — must succeed.
    Integer chairInstitutionId = 3;
    Integer queriedInstitutionId = 99;
    User chair = createUserWithRole();
    chair.setInstitutionId(chairInstitutionId);
    chair.addRole(UserRoles.Chairperson());
    UserService.SigningOfficialUser so = new UserService.SigningOfficialUser();
    so.setUserId(3);
    so.setDisplayName("SO User");
    so.setEmail("so@test.com");
    so.setInstitutionId(queriedInstitutionId);
    when(userService.findUserByEmail(any())).thenReturn(chair);
    when(userService.findSOsWithDataByInstitutionId(queriedInstitutionId)).thenReturn(List.of(so));

    Response response =
        userResource.getSigningOfficialsByInstitution(authUser, queriedInstitutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    List<UserService.SigningOfficialUser> body =
        (List<UserService.SigningOfficialUser>) response.getEntity();
    assertEquals(1, body.size());
    // Confirm the result belongs to the queried institution, not the chair's own.
    assertEquals(queriedInstitutionId, body.getFirst().getInstitutionId());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testGetSigningOfficialsByInstitution_AsResearcher_OwnInstitution() {
    Integer institutionId = 5;
    User researcher = createUserWithRole();
    researcher.setInstitutionId(institutionId);
    UserService.SigningOfficialUser so = new UserService.SigningOfficialUser();
    so.setUserId(4);
    so.setDisplayName("SO User");
    so.setEmail("so@test.com");
    so.setInstitutionId(institutionId);
    when(userService.findUserByEmail(any())).thenReturn(researcher);
    when(userService.findSOsWithDataByInstitutionId(institutionId)).thenReturn(List.of(so));

    Response response = userResource.getSigningOfficialsByInstitution(authUser, institutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    List<UserService.SigningOfficialUser> body =
        (List<UserService.SigningOfficialUser>) response.getEntity();
    assertEquals(1, body.size());
  }

  @Test
  void testGetSigningOfficialsByInstitution_AsResearcher_DifferentInstitution() {
    User researcher = createUserWithRole();
    researcher.setInstitutionId(1);
    when(userService.findUserByEmail(any())).thenReturn(researcher);

    Response response = userResource.getSigningOfficialsByInstitution(authUser, 99);
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testGetSigningOfficialsByInstitution_AsResearcher_NullInstitution() {
    // Researcher with no institution set — Objects.equals(null, anyId) is false → 403
    User researcher = createUserWithRole(); // institutionId is null
    when(userService.findUserByEmail(any())).thenReturn(researcher);

    Response response = userResource.getSigningOfficialsByInstitution(authUser, 5);
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testGetSigningOfficialsByInstitution_ServiceThrows() {
    User admin = createUserWithRole();
    admin.addRole(UserRoles.Admin());
    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(userService.findSOsWithDataByInstitutionId(any()))
        .thenThrow(new RuntimeException("DB error"));

    Response response = userResource.getSigningOfficialsByInstitution(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testGetSigningOfficialsByInstitution_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());

    Response response = userResource.getSigningOfficialsByInstitution(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetUsersByInstitutionNoInstitution() {
    Integer institutionId = 1;
    doThrow(new NotFoundException()).when(userService).findUsersByInstitutionId(institutionId);

    Response response = userResource.getUsersByInstitution(authUser, institutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetUsersByInstitutionNullInstitution() {
    doThrow(new IllegalArgumentException()).when(userService).findUsersByInstitutionId(null);

    Response response = userResource.getUsersByInstitution(authUser, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetUsersByInstitutionSuccess() {
    when(userService.findUsersByInstitutionId(any())).thenReturn(Collections.emptyList());

    Response response = userResource.getUsersByInstitution(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateSelf() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    DuosUser localDuosUser = new DuosUser(authUser, user);
    when(userService.updateUserFieldsById(userUpdateFields, user.getUserId())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(localDuosUser, user.getUserId()))
        .thenReturn(gson.toJsonTree(user).getAsJsonObject());

    try (Response response =
        userResource.updateSelf(localDuosUser, uriInfo, gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateSelfInvalidName() {
    PSQLState psqlState = mock(PSQLState.class);
    // PSQLState is missing the enum constant 22021 for invalid byte sequence but returns it so we
    // mock it
    when(psqlState.getState()).thenReturn("22021");
    PSQLException psqlException =
        new PSQLException("invalid byte sequence for encoding \"UTF8\": 0x00", psqlState);
    StatementContext ctx = mock(StatementContext.class);
    StatementExceptions exceptions = mock(StatementExceptions.class);
    when(ctx.getConfig(StatementExceptions.class)).thenReturn(exceptions);
    UnableToExecuteStatementException exception =
        new UnableToExecuteStatementException("Failed to execute statement", psqlException, ctx);

    User user = createUserWithRole();
    String invalidName = "invalid\0name";
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setDisplayName(invalidName);
    when(userService.updateUserFieldsById(any(), any())).thenThrow(exception);

    try (var response =
        userResource.updateSelf(
            new DuosUser(authUser, user), uriInfo, gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateSelfRolesNotAdmin() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setUserRoleIds(List.of(1)); // any roles

    try (var response =
        userResource.updateSelf(
            new DuosUser(authUser, user), uriInfo, gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdate() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any()))
        .thenReturn(gson.toJsonTree(user).getAsJsonObject());

    try (Response response =
        userResource.update(duosUser, user.getUserId(), gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateUserNotFound() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());

    try (Response response = userResource.update(duosUser, user.getUserId(), "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testUpdateUserInvalidJson() {
    User user = createUserWithRole();

    try (Response response = userResource.update(duosUser, user.getUserId(), "}{][")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteRoleFromUser() {
    User user = createUserWithRole();
    user.setUserId(1);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.setAdminRole();
    when(userService.findUserById(any())).thenReturn(user);
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any()))
        .thenReturn(userJson.getAsJsonObject());
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    User returnedUser;
    try (Response response =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      returnedUser = new User((String) response.getEntity());
    }
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  void testDeleteRoleFromUserDacRoleId() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    when(userService.findUserById(user.getUserId())).thenReturn(user);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    try (Response response = userResource.deleteRoleFromUser(activeDuosUser, user.getUserId(), 2)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteRoleFromUserInvalidRoleId() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();

    try (Response response = userResource.deleteRoleFromUser(duosUser, 1, 1000)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteDeniedRoleBySoShouldFail() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.addRole(UserRoles.Admin());
    user.addRole(UserRoles.Chairperson());
    user.addRole(UserRoles.Member());
    user.addRole(UserRoles.Alumni());
    user.setInstitutionId(10);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.addRole(UserRoles.SigningOfficial());
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.deleteRoleFromUser(duosUser, user.getUserId(), UserRoles.ADMIN.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
    try (Response response2 =
        userResource.deleteRoleFromUser(
            duosUser, user.getUserId(), UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response2.getStatus());
    }
    try (Response response3 =
        userResource.deleteRoleFromUser(
            duosUser, user.getUserId(), UserRoles.CHAIRPERSON.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response3.getStatus());
    }
    try (Response response4 =
        userResource.deleteRoleFromUser(duosUser, user.getUserId(), UserRoles.MEMBER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response4.getStatus());
    }
    try (Response response5 =
        userResource.deleteRoleFromUser(duosUser, user.getUserId(), UserRoles.ALUMNI.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response5.getStatus());
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response5.getStatus());
    }
  }

  @Test
  void testDeletePermittedRolesBySoShouldSucceedForUserWithSameInstitution() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficial());
    user.addRole(UserRoles.DataSubmitter());
    user.addRole(UserRoles.ITDirector());
    user.setInstitutionId(10);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.addRole(UserRoles.SigningOfficial());
    activeUser.setInstitutionId(10);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
    try (Response response2 =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response2.getStatus());
    }
    try (Response response3 =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.SIGNINGOFFICIAL.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response3.getStatus());
    }
  }

  @Test
  void testDeletePermittedRolesBySoShouldFailForUserWitNullInstitution() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficial());
    user.addRole(UserRoles.DataSubmitter());
    user.addRole(UserRoles.ITDirector());
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.addRole(UserRoles.SigningOfficial());
    activeUser.setInstitutionId(10);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
    try (Response response2 =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response2.getStatus());
    }
    try (Response response3 =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.SIGNINGOFFICIAL.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response3.getStatus());
    }
  }

  @Test
  void testDeleteSORoleFromSOInOtherOrgSOShouldFail() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.setSigningOfficialRole();
    user.setInstitutionId(1);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.setSigningOfficialRole();
    activeUser.setInstitutionId(2);
    assertNotEquals(user.getInstitutionId(), activeUser.getInstitutionId());
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.SIGNINGOFFICIAL.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testDeleteSORoleFromSelfShouldFail() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    user.setInstitutionId(1);
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), user);
    when(userService.findUserById(any())).thenReturn(user);

    try (Response response =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.SIGNINGOFFICIAL.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteRoleFromUser_UserWithoutRole() {
    User user = createUserWithRole();
    user.setUserId(1);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.setAdminRole();
    when(userService.findUserById(any())).thenReturn(user);
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any()))
        .thenReturn(userJson.getAsJsonObject());
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    User returnedUser;
    try (Response response =
        userResource.deleteRoleFromUser(
            activeDuosUser, user.getUserId(), UserRoles.ADMIN.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      returnedUser = new User((String) response.getEntity());
    }
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  void testDeleteRoleFromUser_UserNotFound() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    DuosUser activeDuosUser = new DuosUser(new AuthUser(), activeUser);
    when(userService.findUserById(any())).thenThrow(new NotFoundException());

    try (Response response =
        userResource.deleteRoleFromUser(activeDuosUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetDatasetsFromUserDacsV2() {
    User user = createUserWithRole();
    user.setChairpersonRoleWithDAC(1);
    when(datasetService.findDatasetListByDacIds(anyList())).thenReturn(List.of(new Dataset()));
    when(userService.findUserByEmail(anyString())).thenReturn(user);

    Response response = userResource.getDatasetsFromUserDacsV2(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetDatasetsFromUserDacsV2DatasetsNotFound() {
    User user = createUserWithRole();
    user.setChairpersonRoleWithDAC(1);
    when(datasetService.findDatasetListByDacIds(anyList())).thenReturn(List.of());
    when(userService.findUserByEmail(anyString())).thenReturn(user);

    Response response = userResource.getDatasetsFromUserDacsV2(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetDatasetsFromUserDacsV2UserNotFound() {
    when(userService.findUserByEmail(anyString()))
        .thenThrow(new NotFoundException("User not found"));

    Response response = userResource.getDatasetsFromUserDacsV2(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testPostAcknowledgement() {
    User user = createUserWithRole();
    String acknowledgementKey = "key1";
    Map<String, Acknowledgement> acknowledgementMap =
        getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.makeAcknowledgements(anyList(), any()))
        .thenReturn(acknowledgementMap);

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));
    try (Response response = userResource.postAcknowledgements(duosUser, jsonString)) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementException() {
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("exception during post"))
        .when(acknowledgementService)
        .makeAcknowledgements(anyList(), any());

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));

    try (Response response = userResource.postAcknowledgements(duosUser, jsonString)) {
      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementBadJson() {
    String jsonString = "The quick brown fox jumped over the lazy dog.";

    try (Response response = userResource.postAcknowledgements(duosUser, jsonString)) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostCloseoutAcknowledgementSuccess() {
    User user = createUserWithRole();
    user.addRole(UserRoles.Chairperson());
    DuosUser chairUser = new DuosUser(authUser, user);
    String acknowledgementKey = AcknowledgementService.DAR_CLOSEOUT_CHAIR_REF + "12345";
    Map<String, Acknowledgement> acknowledgementMap =
        getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.makeAcknowledgements(anyList(), any()))
        .thenReturn(acknowledgementMap);

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));
    try (Response response = userResource.postAcknowledgements(chairUser, jsonString)) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostCloseoutAcknowledgementFailure() {
    String acknowledgementKey = AcknowledgementService.DAR_CLOSEOUT_CHAIR_REF + "12345";

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));
    try (Response response = userResource.postAcknowledgements(duosUser, jsonString)) {
      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementEmptyJson() {
    try (Response response = userResource.postAcknowledgements(duosUser, "")) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementEmptyJsonList() {
    try (Response response = userResource.postAcknowledgements(duosUser, "[]")) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testMissingAcknowledgement() {
    String acknowledgementKey = "key1";
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);

    Response response = userResource.getUserAcknowledgement(duosUser, acknowledgementKey);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementException() {
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("some exception during get."))
        .when(acknowledgementService)
        .findAcknowledgementForUserByKey(any(), any());

    Response response = userResource.getUserAcknowledgement(duosUser, acknowledgementKey);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementNull() {
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);

    Response response = userResource.getUserAcknowledgement(duosUser, null);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUnsetAcknowledgementsForUser() {
    when(acknowledgementService.findAcknowledgementsForUser(any())).thenReturn(null);

    Response response = userResource.getUserAcknowledgements(duosUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementsForUserException() {
    doThrow(new RuntimeException("some get exception"))
        .when(acknowledgementService)
        .findAcknowledgementsForUser(any());

    Response response = userResource.getUserAcknowledgements(duosUser);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetSetAcknowledgementForUser() {
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap =
        getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any()))
        .thenReturn(acknowledgementMap.get(acknowledgementKey));

    Response response =
        userResource.getUserAcknowledgement(new DuosUser(authUser, user), acknowledgementKey);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testDeleteAcknowledgementForUser() {
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap =
        getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any()))
        .thenReturn(acknowledgementMap.get(acknowledgementKey));

    try (Response response = userResource.deleteUserAcknowledgement(authUser, acknowledgementKey)) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testDeleteMissingAcknowledgementForUser() {
    createUserWithRole();
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);

    try (Response response = userResource.deleteUserAcknowledgement(authUser, "key")) {
      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testGetAllAcknowledgements() {
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap =
        getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.findAcknowledgementsForUser(any())).thenReturn(acknowledgementMap);
    Response response = userResource.getUserAcknowledgements(new DuosUser(authUser, user));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetApprovedDatasets() {
    ApprovedDataset example =
        new ApprovedDataset(
            1,
            "sampleDarId",
            "sampleName",
            "sampleDac",
            Timestamp.from(
                Instant.ofEpochMilli(
                    Instant.now().toEpochMilli() + DataAccessRequest.EXPIRATION_DURATION_MILLIS)));
    when(datasetService.getApprovedDatasets(any())).thenReturn(List.of(example));

    Response response = userResource.getApprovedDatasets(duosUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testCreateNewUserAsAdmin() {
    CreateDuosUserRequest request =
        new CreateDuosUserRequest(
            "New User", "test@test.com", true, List.of(UserRoles.Researcher()));
    User createdUser =
        new User(1, request.email(), request.displayName(), new Date(), request.roles());
    User adminUser = createUserWithRole();
    adminUser.setAdminRole();
    DuosUser adminDuosUser = new DuosUser(authUser, adminUser);

    when(userService.createUser(request.newUser())).thenReturn(createdUser);
    when(servicesConfiguration.getLocalURL()).thenReturn("http://localhost:8080");

    try (var response = userResource.createNewUser(adminDuosUser, gson.toJson(request))) {
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testCreateNewUserAsNonAdminWithValidDomain() {
    CreateDuosUserRequest request =
        new CreateDuosUserRequest(
            "New User", "test@example.com", true, List.of(UserRoles.Researcher()));
    User createdUser =
        new User(1, request.email(), request.displayName(), new Date(), request.roles());
    User nonAdminUser = createUserWithInstitution();
    DuosUser nonAdminDuosUser = new DuosUser(authUser, nonAdminUser);

    Institution institution = new Institution();
    institution.setId(nonAdminUser.getInstitutionId());
    institution.setDomains(List.of("@example.com"));

    when(userService.createUser(request.newUser())).thenReturn(createdUser);
    when(servicesConfiguration.getLocalURL()).thenReturn("http://localhost:8080");

    try (var response = userResource.createNewUser(nonAdminDuosUser, gson.toJson(request))) {
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testCreateNewUserAsNonAdminWithInvalidDomain() {
    CreateDuosUserRequest request =
        new CreateDuosUserRequest(
            "New User", "test@invalid.com", true, List.of(UserRoles.Researcher()));
    User nonAdminUser = createUserWithInstitution();
    DuosUser nonAdminDuosUser = new DuosUser(authUser, nonAdminUser);

    List<String> allowedDomains = List.of("@example.com");
    Institution institution = new Institution();
    institution.setId(nonAdminUser.getInstitutionId());
    institution.setDomains(allowedDomains);

    doThrow(
            new ForbiddenException(
                "You can only create users with email addresses from your institutional domains: "
                    + allowedDomains))
        .when(institutionAndLibraryCardEnforcement)
        .validateEmailsFromSameInstitution(anyString(), anyString());

    try (var response = userResource.createNewUser(nonAdminDuosUser, gson.toJson(request))) {
      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testCreateNewUserInvalidDisplayName() {
    List<UserRole> roles = List.of(UserRoles.Researcher());
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateDuosUserRequest(null, "test@test.com", true, roles));
  }

  @Test
  void testCreateNewUserInvalidEmail() {
    List<UserRole> roles = List.of(UserRoles.Researcher());
    assertThrows(
        IllegalArgumentException.class,
        () -> new CreateDuosUserRequest("New User", null, true, roles));
  }

  private static Stream<Arguments> nonResearcherRoleProvider() {
    return Stream.of(
        Arguments.of(UserRoles.Admin()),
        Arguments.of(UserRoles.Alumni()),
        Arguments.of(UserRoles.DataSubmitter()),
        Arguments.of(UserRoles.ServiceAccount()),
        Arguments.of(UserRoles.SigningOfficial()),
        Arguments.of(UserRoles.ITDirector()));
  }

  @ParameterizedTest
  @MethodSource("nonResearcherRoleProvider")
  void testCreateNewUserChairInvalidRole(UserRole role) {
    DuosUser chairUser =
        new DuosUser(
            authUser,
            new User(1, TEST_EMAIL, "Test User", new Date(), List.of(UserRoles.Chairperson())));
    CreateDuosUserRequest request =
        new CreateDuosUserRequest("New User", "test@test.com", true, List.of(role));
    try (var response = userResource.createNewUser(chairUser, gson.toJson(request))) {
      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }
  }

  @ParameterizedTest
  @MethodSource("nonResearcherRoleProvider")
  void testCreateNewUserAdmin(UserRole role) {
    DuosUser adminUser =
        new DuosUser(
            authUser, new User(1, TEST_EMAIL, "Test User", new Date(), List.of(UserRoles.Admin())));
    CreateDuosUserRequest request =
        new CreateDuosUserRequest("New User", "test@test.com", true, List.of(role));
    User createdUser =
        new User(1, request.email(), request.displayName(), new Date(), request.roles());
    when(userService.createUser(request.newUser())).thenReturn(createdUser);
    when(servicesConfiguration.getLocalURL()).thenReturn("http://localhost:8080");
    try (var response = userResource.createNewUser(adminUser, gson.toJson(request))) {
      assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
    }
  }

  private Map<String, Acknowledgement> getDefaultAcknowledgementForUser(
      User user, String acknowledgementKey) {
    Acknowledgement ack = new Acknowledgement();
    Timestamp timestamp = new Timestamp(new Date().getTime());
    ack.setAckKey(acknowledgementKey);
    ack.setLastAcknowledged(timestamp);
    ack.setFirstAcknowledged(timestamp);
    ack.setUserId(user.getUserId());
    HashMap<String, Acknowledgement> map = new HashMap<>();
    map.put(acknowledgementKey, ack);
    return map;
  }

  private User createUserWithRole() {
    User user = new User();
    user.setUserId(randomInt(1, 100));
    user.setDisplayName("Test");
    user.setEmail("Test");
    user.addRole(UserRoles.Researcher());
    return user;
  }

  private User createUserWithInstitution() {
    User user = new User();
    user.setUserId(1);
    user.setDisplayName("Test Name");
    user.setEmail("Test Email");
    user.setInstitutionId(1);
    return user;
  }

  // --- redactUser endpoint tests ---

  @Test
  void testRedactUser_success() {
    User adminUser = createUserWithRole();
    adminUser.addRole(UserRoles.Admin());
    DuosUser adminDuosUser = new DuosUser(authUser, adminUser);

    try (Response response = userResource.redactUser(adminDuosUser, "target@example.com")) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
      verify(userService).redactUser(adminUser, "target@example.com");
    }
  }

  @Test
  void testRedactUser_missingEmail() {
    User adminUser = createUserWithRole();
    DuosUser adminDuosUser = new DuosUser(authUser, adminUser);

    try (Response response = userResource.redactUser(adminDuosUser, null)) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testRedactUser_blankEmail() {
    User adminUser = createUserWithRole();
    DuosUser adminDuosUser = new DuosUser(authUser, adminUser);

    try (Response response = userResource.redactUser(adminDuosUser, "   ")) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testRedactUser_notFound() {
    User adminUser = createUserWithRole();
    DuosUser adminDuosUser = new DuosUser(authUser, adminUser);
    String email = "nobody@example.com";

    doThrow(new NotFoundException("User not found")).when(userService).redactUser(adminUser, email);

    try (Response response = userResource.redactUser(adminDuosUser, email)) {
      assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
  }
}
