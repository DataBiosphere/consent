package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementExceptions;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
class UserResourceTest extends AbstractTestHelper {

  @Mock
  private UserService userService;

  @Mock
  private SamService samService;

  @Mock
  private DatasetService datasetService;

  private UserResource userResource;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private UriBuilder uriBuilder;

  @Mock
  private UserStatusInfo userStatusInfo;

  @Mock
  private AcknowledgementService acknowledgementService;

  private static final String TEST_EMAIL = "test@gmail.com";

  private final Gson gson = GsonUtil.getInstance();

  private final AuthUser authUser = new AuthUser()
      .setAuthToken("auth-token")
      .setName("Test User")
      .setEmail(TEST_EMAIL)
      .setUserStatusInfo(userStatusInfo);

  @BeforeEach
  void initResource() {
    userResource = new UserResource(samService, userService, datasetService,
        acknowledgementService);
  }

  @Test
  void testGetMe() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);

    Response response = userResource.getUser(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUserById() {

    Response response = userResource.getUserById(authUser, 1);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUserByIdNotFound() {
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenThrow(
        new NotFoundException());

    Response response = userResource.getUserById(authUser, 1);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_SO() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.getUsersAsRole(user, "SigningOfficial")).thenReturn(
        Arrays.asList(new User(), new User()));

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
    when(userService.getUsersAsRole(user, "Admin")).thenReturn(
        Arrays.asList(new User(), new User()));

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
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserNotFound() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    doThrow(new NotFoundException()).when(userService).findUserById(any());

    try (Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(404, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserNotModified() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(304, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBadRequest() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();

    try (Response response = userResource.addRoleToUser(authUser, 1, 1000)) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoWithoutUserAndSoInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(400, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoInstitutionWithoutUserInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testAddRoleToUserBySoWithoutSoInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    user.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId())) {
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
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId())) {
      assertEquals(400, response.getStatus());
    }
    try (Response response2 = userResource.addRoleToUser(authUser, 1,
        UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(400, response2.getStatus());
    }
    try (Response response3 = userResource.addRoleToUser(authUser, 1,
        UserRoles.MEMBER.getRoleId())) {
      assertEquals(400, response3.getStatus());
    }
    try (Response response4 = userResource.addRoleToUser(authUser, 1,
        UserRoles.CHAIRPERSON.getRoleId())) {
      assertEquals(400, response4.getStatus());
    }
    try (Response response5 = userResource.addRoleToUser(authUser, 1,
        UserRoles.ALUMNI.getRoleId())) {
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
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(200, response.getStatus());
    }
    try (Response response2 = userResource.addRoleToUser(authUser, 1,
        UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(200, response2.getStatus());
    }
    try (Response response3 = userResource.addRoleToUser(authUser, 1,
        UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(200, response3.getStatus());
    }
  }

  @SuppressWarnings({"unchecked"})
  @Test
  void testGetSOsForInstitution() {
    User user = createUserWithInstitution();
    User so = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findSOsByInstitutionId(any())).thenReturn(
        Arrays.asList(new UserService.SimplifiedUser(so), new UserService.SimplifiedUser(so)));

    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List<UserService.SimplifiedUser>) response.getEntity();
    assertFalse(body.isEmpty());
    assertEquals(so.getDisplayName(), body.get(0).getDisplayName());
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
  void testGetUnassignedUsers() {
    List<User> users = Collections.singletonList(createUserWithRole());
    when(userService.findUsersWithNoInstitution()).thenReturn(users);

    Response response = userResource.getUnassignedUsers(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
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
    doThrow(new IllegalArgumentException()).when(userService)
        .findUsersByInstitutionId(null);

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
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(userService.updateUserFieldsById(userUpdateFields, user.getUserId())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(authUser, user.getUserId())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());

    try (Response response = userResource.updateSelf(authUser, uriInfo,
        gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateSelfInvalidName() {
    PSQLState psqlState = mock(PSQLState.class);
    // PSQLState is missing the enum constant 22021 for invalid byte sequence but returns it so we mock it
    when(psqlState.getState()).thenReturn("22021");
    PSQLException psqlException = new PSQLException(
        "invalid byte sequence for encoding \"UTF8\": 0x00", psqlState);
    StatementContext ctx = mock(StatementContext.class);
    StatementExceptions exceptions = mock(StatementExceptions.class);
    when(ctx.getConfig(StatementExceptions.class)).thenReturn(exceptions);
    UnableToExecuteStatementException exception = new UnableToExecuteStatementException(
        "Failed to execute statement", psqlException, ctx);

    User user = createUserWithRole();
    String invalidName = "invalid\0name";
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setDisplayName(invalidName);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenThrow(exception);

    try (var response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateSelfRolesNotAdmin() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setUserRoleIds(List.of(1)); // any roles
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);

    try (var response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateSelfShouldNotPassInstitutionId() {
    User user = createUserWithRole();
    user.setITDirectorRole();
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(20);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);

    try (var response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdate() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());

    try (Response response = userResource.update(authUser, uriInfo, user.getUserId(),
        gson.toJson(userUpdateFields))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateUserNotFound() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());

    try (Response response = userResource.update(authUser, uriInfo, user.getUserId(), "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testUpdateUserInvalidJson() {
    User user = createUserWithRole();

    try (Response response = userResource.update(authUser, uriInfo, user.getUserId(), "}{][")) {
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
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        userJson.getAsJsonObject());

    User returnedUser;
    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.RESEARCHER.getRoleId())) {
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
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(activeUser);
    when(userService.findUserById(user.getUserId())).thenReturn(user);

    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(), 2)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testDeleteRoleFromUserInvalidRoleId() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();

    try (Response response = userResource.deleteRoleFromUser(authUser, 1, 1000)) {
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
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ADMIN.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
    try (Response response2 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.RESEARCHER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response2.getStatus());
    }
    try (Response response3 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.CHAIRPERSON.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response3.getStatus());
    }
    try (Response response4 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.MEMBER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response4.getStatus());
    }
    try (Response response5 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ALUMNI.getRoleId())) {
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
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
    try (Response response2 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response2.getStatus());
    }
    try (Response response3 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId())) {
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
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ITDIRECTOR.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
    try (Response response2 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.DATASUBMITTER.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response2.getStatus());
    }
    try (Response response3 = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId())) {
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
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testDeleteSORoleFromSelfShouldFail() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    user.setInstitutionId(1);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);

    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId())) {
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
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        userJson.getAsJsonObject());

    User returnedUser;
    try (Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ADMIN.getRoleId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      returnedUser = new User((String) response.getEntity());
    }
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  void testDeleteRoleFromUser_UserNotFound() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    when(userService.findUserByEmail(any())).thenReturn(activeUser);

    try (Response response = userResource.deleteRoleFromUser(authUser, 1,
        UserRoles.ADMIN.getRoleId())) {
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
    when(userService.findUserByEmail(anyString())).thenThrow(
        new NotFoundException("User not found"));


    Response response = userResource.getDatasetsFromUserDacsV2(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testPostAcknowledgement() {
    User user = createUserWithRole();
    String acknowledgementKey = "key1";
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user,
        acknowledgementKey);
    when(acknowledgementService.makeAcknowledgements(anyList(), any())).thenReturn(
        acknowledgementMap);


    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));
    try (Response response = userResource.postAcknowledgements(authUser, jsonString)) {
      assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementException() {
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("exception during post")).when(acknowledgementService)
        .makeAcknowledgements(anyList(), any());

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));

    try (Response response = userResource.postAcknowledgements(authUser, jsonString)) {
      assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementBadJson() {
    String jsonString = "The quick brown fox jumped over the lazy dog.";

    try (Response response = userResource.postAcknowledgements(authUser, jsonString)) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementEmptyJson() {
    try (Response response = userResource.postAcknowledgements(authUser, "")) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testPostAcknowledgementEmptyJsonList() {
    try (Response response = userResource.postAcknowledgements(authUser, "[]")) {
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
  }

  @Test
  void testMissingAcknowledgement() {
    String acknowledgementKey = "key1";
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);


    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementException() {
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("some exception during get.")).when(acknowledgementService)
        .findAcknowledgementForUserByKey(any(), any());


    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementNull() {
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);


    Response response = userResource.getUserAcknowledgement(authUser, null);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUnsetAcknowledgementsForUser() {
    when(acknowledgementService.findAcknowledgementsForUser(any())).thenReturn(null);


    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementsForUserException() {
    doThrow(new RuntimeException("some get exception")).when(acknowledgementService)
        .findAcknowledgementsForUser(any());


    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetSetAcknowledgementForUser() {
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user,
        acknowledgementKey);
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(
        acknowledgementMap.get(acknowledgementKey));


    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testDeleteAcknowledgementForUser() {
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user,
        acknowledgementKey);
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(
        acknowledgementMap.get(acknowledgementKey));

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
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user,
        acknowledgementKey);
    when(acknowledgementService.findAcknowledgementsForUser(any())).thenReturn(acknowledgementMap);
    Response response = userResource.getUserAcknowledgements(authUser);
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

    Response response = userResource.getApprovedDatasets(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  private Map<String, Acknowledgement> getDefaultAcknowledgementForUser(User user,
      String acknowledgementKey) {
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

}
