package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

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

  private final String TEST_EMAIL = "test@gmail.com";

  private final AuthUser authUser = new AuthUser()
      .setAuthToken("auth-token")
      .setName("Test User")
      .setEmail(TEST_EMAIL)
      .setUserStatusInfo(userStatusInfo);

  private void initResource() {
    userResource = new UserResource(samService, userService, datasetService,
        acknowledgementService);
  }

  @Test
  void testGetMe() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUser(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUserById() {
    initResource();

    Response response = userResource.getUserById(authUser, 1);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUserByIdNotFound() {
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenThrow(
        new NotFoundException());
    initResource();

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
    initResource();

    Response response = userResource.getUsers(authUser, "SigningOfficial");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_SO_NoRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

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
    initResource();

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_Admin_NoRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_UnsupportedRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUsers(authUser, "Researcher");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_InvalidRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUsers(authUser, "BadRequest");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUsers_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    initResource();

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testCreateExistingUser() {
    User user = new User();
    user.setEmail(TEST_EMAIL);
    user.addRole(UserRoles.AdminRole());
    user.addRole(UserRoles.ResearcherRole());
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    initResource();

    Response response = userResource.createResearcher(uriInfo, authUser);
    assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
  }

  @Test
  void testCreateFailingGoogleIdentity() {
    initResource();

    Response response = userResource.createResearcher(uriInfo, new AuthUser(TEST_EMAIL));
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
    initResource();

    Response response = userResource.createResearcher(uriInfo, authUser);
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  void testDeleteUser() {
    doNothing().when(userService).deleteUserByEmail(any());
    initResource();
    Response response = userResource.delete(RandomStringUtils.randomAlphabetic(10), uriInfo);
    assertEquals(200, response.getStatus());
  }

  @Test
  void testAddRoleToUser() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(200, response.getStatus());
  }

  @Test
  void testAddRoleToUserNotFound() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    doThrow(new NotFoundException()).when(userService).findUserById(any());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(404, response.getStatus());
  }

  @Test
  void testAddRoleToUserNotModified() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.RESEARCHER.getRoleId());
    assertEquals(304, response.getStatus());
  }

  @Test
  void testAddRoleToUserBadRequest() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, 1000);
    assertEquals(400, response.getStatus());
  }

  @Test
  void testAddRoleToUserBySoWithoutUserAndSoInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(400, response.getStatus());
  }

  @Test
  void testAddRoleToUserBySoInstitutionWithoutUserInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(200, response.getStatus());
  }

  @Test
  void testAddRoleToUserBySoWithoutSoInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setSigningOfficialRole();
    User user = createUserWithRole();
    user.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(400, response.getStatus());
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
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.RESEARCHER.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.MEMBER.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.CHAIRPERSON.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.ALUMNI.getRoleId());
    assertEquals(400, response.getStatus());
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
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1,
        UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(200, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(200, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(200, response.getStatus());
  }

  @SuppressWarnings({"unchecked"})
  @Test
  void testGetSOsForInstitution() {
    User user = createUserWithInstitution();
    User so = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findSOsByInstitutionId(any())).thenReturn(
        Arrays.asList(new UserService.SimplifiedUser(so), new UserService.SimplifiedUser(so)));
    initResource();
    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List<UserService.SimplifiedUser>) response.getEntity();
    assertFalse(body.isEmpty());
    assertEquals(so.getDisplayName(), body.get(0).displayName);
  }

  @SuppressWarnings("rawtypes")
  @Test
  void testGetSOsForInstitution_NoInstitution() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();
    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List) response.getEntity();
    assertTrue(body.isEmpty());
  }

  @Test
  void testGetSOsForInstitution_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetUnassignedUsers() {
    List<User> users = Collections.singletonList(createUserWithRole());
    when(userService.findUsersWithNoInstitution()).thenReturn(users);
    initResource();
    Response response = userResource.getUnassignedUsers(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetUsersByInstitutionNoInstitution() {
    Integer institutionId = 1;
    doThrow(new NotFoundException()).when(userService).findUsersByInstitutionId(institutionId);
    initResource();

    Response response = userResource.getUsersByInstitution(authUser, institutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetUsersByInstitutionNullInstitution() {
    Integer institutionId = null;
    doThrow(new IllegalArgumentException()).when(userService)
        .findUsersByInstitutionId(institutionId);
    initResource();

    Response response = userResource.getUsersByInstitution(authUser, institutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetUsersByInstitutionSuccess() {
    when(userService.findUsersByInstitutionId(any())).thenReturn(Collections.emptyList());
    initResource();

    Response response = userResource.getUsersByInstitution(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateSelf() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateSelfRolesNotAdmin() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setUserRoleIds(List.of(1)); // any roles
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateSelfInstitutionIdAsSO() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(10);
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateSelfInstitutionIdAsSO_ExistingInstitution() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(20);
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateSelfInstitutionIdAsSO_SameInstitution() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(10);
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateSelfInstitutionIdAsITDirector() {
    User user = createUserWithRole();
    user.setITDirectorRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(10);
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateSelfInstitutionIdAsITDirector_ExistingInstitution() {
    User user = createUserWithRole();
    user.setITDirectorRole();
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(20);
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateSelfInstitutionIdNullAsSO_ExistingInstitution() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(null);
    Gson gson = new Gson();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    user.setInstitutionId(20);
    userUpdateFields.setInstitutionId(20);
    Response response2 = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response2.getStatus());
  }


  @Test
  void testCanUpdateInstitution() {
    initResource();

    // User with no roles and no institution can update their institution
    User u1 = new User();
    boolean canUpdate = userResource.canUpdateInstitution(u1, 1);
    assertTrue(canUpdate);

    // Researcher user with no institution can update their institution
    User u2 = new User();
    u2.setResearcherRole();
    canUpdate = userResource.canUpdateInstitution(u2, 1);
    assertTrue(canUpdate);

    // Researcher user with an institution can update their institution
    User u3 = new User();
    u3.setInstitutionId(10);
    u3.setResearcherRole();
    canUpdate = userResource.canUpdateInstitution(u3, 1);
    assertTrue(canUpdate);

    // SO user with no institution can update their institution
    User u4 = new User();
    u4.setSigningOfficialRole();
    canUpdate = userResource.canUpdateInstitution(u4, 1);
    assertTrue(canUpdate);

    // SO user with an institution CANNOT update their institution
    User u4a = new User();
    u4a.setInstitutionId(10);
    u4a.setSigningOfficialRole();
    canUpdate = userResource.canUpdateInstitution(u4a, 1);
    assertFalse(canUpdate);

    // IT user with no institution can update their institution
    User u5 = new User();
    u5.setITDirectorRole();
    canUpdate = userResource.canUpdateInstitution(u5, 1);
    assertTrue(canUpdate);

    // IT user with an institution CANNOT update their institution
    User u5a = new User();
    u5a.setInstitutionId(10);
    u5a.setITDirectorRole();
    canUpdate = userResource.canUpdateInstitution(u5a, 1);
    assertFalse(canUpdate);

    // Admin user with no institution can update their institution
    User u6 = new User();
    u6.setAdminRole();
    canUpdate = userResource.canUpdateInstitution(u6, 1);
    assertTrue(canUpdate);

    // Admin user with an institution can update their institution
    User u7 = new User();
    u7.setInstitutionId(10);
    u7.setAdminRole();
    canUpdate = userResource.canUpdateInstitution(u7, 1);
    assertTrue(canUpdate);
  }

  @Test
  void testUpdate() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.update(authUser, uriInfo, user.getUserId(),
        gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateUserNotFound() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = userResource.update(authUser, uriInfo, user.getUserId(), "");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateUserInvalidJson() {
    User user = createUserWithRole();
    initResource();
    Response response = userResource.update(authUser, uriInfo, user.getUserId(), "}{][");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
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
    Gson gson = new Gson();
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        userJson.getAsJsonObject());
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.RESEARCHER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    User returnedUser = new User((String) response.getEntity());
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  void testDeleteRoleFromUser_InvalidRole() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(), 20);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testDeleteDeniedRoleBySoShouldFail() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.addRole(UserRoles.AdminRole());
    user.addRole(UserRoles.ChairpersonRole());
    user.addRole(UserRoles.MemberRole());
    user.addRole(UserRoles.AlumniRole());
    user.setInstitutionId(10);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.addRole(UserRoles.SigningOfficialRole());
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ADMIN.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.RESEARCHER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.CHAIRPERSON.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.MEMBER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ALUMNI.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testDeletePermittedRolesBySoShouldSucceedForUserWithSameInstitution() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficialRole());
    user.addRole(UserRoles.DataSubmitterRole());
    user.addRole(UserRoles.ITDirectorRole());
    user.setInstitutionId(10);
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.addRole(UserRoles.SigningOfficialRole());
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testDeletePermittedRolesBySoShouldFailForUserWitNullInstitution() {
    User user = createUserWithRole();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficialRole());
    user.addRole(UserRoles.DataSubmitterRole());
    user.addRole(UserRoles.ITDirectorRole());
    User activeUser = createUserWithRole();
    activeUser.setUserId(2);
    activeUser.addRole(UserRoles.SigningOfficialRole());
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
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
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void testDeleteSORoleFromSelfShouldFail() {
    User user = createUserWithRole();
    user.setSigningOfficialRole();
    user.setInstitutionId(1);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
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
    Gson gson = new Gson();
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(
        userJson.getAsJsonObject());
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),
        UserRoles.ADMIN.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    User returnedUser = new User((String) response.getEntity());
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  void testDeleteRoleFromUser_UserNotFound() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testDeleteRoleFromUserInvalidRoleId() {
    User activeUser = createUserWithRole();
    activeUser.setAdminRole();
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, 1, 1000);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetDatasetsFromUserDacsV2() {
    User user = createUserWithRole();
    user.setChairpersonRoleWithDAC(1);
    when(datasetService.findDatasetListByDacIds(anyList())).thenReturn(List.of(new Dataset()));
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    initResource();

    Response response = userResource.getDatasetsFromUserDacsV2(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetDatasetsFromUserDacsV2DatasetsNotFound() {
    User user = createUserWithRole();
    user.setChairpersonRoleWithDAC(1);
    when(datasetService.findDatasetListByDacIds(anyList())).thenReturn(List.of());
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    initResource();

    Response response = userResource.getDatasetsFromUserDacsV2(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetDatasetsFromUserDacsV2UserNotFound() {
    when(userService.findUserByEmail(anyString())).thenThrow(
        new NotFoundException("User not found"));
    initResource();

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
    initResource();

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));
    Response response = userResource.postAcknowledgements(authUser, jsonString);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testPostAcknowledgementException() {
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("exception during post")).when(acknowledgementService)
        .makeAcknowledgements(anyList(), any());
    initResource();
    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));

    Response response = userResource.postAcknowledgements(authUser, jsonString);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testPostAcknowledgementBadJson() {
    initResource();
    String jsonString = "The quick brown fox jumped over the lazy dog.";

    Response response = userResource.postAcknowledgements(authUser, jsonString);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testPostAcknowledgementEmptyJson() {
    initResource();

    Response response = userResource.postAcknowledgements(authUser, "");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testPostAcknowledgementEmptyJsonList() {
    initResource();

    Response response = userResource.postAcknowledgements(authUser, "[]");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void testMissingAcknowledgement() {
    String acknowledgementKey = "key1";
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementException() {
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("some exception during get.")).when(acknowledgementService)
        .findAcknowledgementForUserByKey(any(), any());
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementNull() {
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser, null);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetUnsetAcknowledgementsForUser() {
    when(acknowledgementService.findAcknowledgementsForUser(any())).thenReturn(null);
    initResource();

    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAcknowledgementsForUserException() {
    doThrow(new RuntimeException("some get exception")).when(acknowledgementService)
        .findAcknowledgementsForUser(any());
    initResource();

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
    initResource();

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
    initResource();

    Response response = userResource.deleteUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testDeleteMissingAcknowledgementForUser() {
    User user = createUserWithRole();
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);
    initResource();

    Response response = userResource.deleteUserAcknowledgement(authUser, "key");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetAllAcknowledgements() {
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user,
        acknowledgementKey);
    initResource();

    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void testGetApprovedDatasets() {
    ApprovedDataset example = new ApprovedDataset(1, "sampleDarId", "sampleName", "sampleDac",
        new Date());
    when(datasetService.getApprovedDatasets(any())).thenReturn(List.of(example));
    initResource();
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
    user.setUserId(RandomUtils.nextInt(1, 100));
    user.setDisplayName("Test");
    user.setEmail("Test");
    user.addRole(UserRoles.ResearcherRole());
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

  private List<UserProperty> createResearcherProperties() {
    return Arrays.stream(UserFields.values())
        .filter(UserFields::getRequired)
        .map(f -> new UserProperty(1, f.getValue(), f.getValue()))
        .collect(Collectors.toList());
  }

  private List<LibraryCard> createLibraryCards() {
    LibraryCard e = new LibraryCard();
    String randomValue = RandomStringUtils.random(10, true, false);
    e.setEraCommonsId(randomValue);
    e.setUserEmail(randomValue);
    e.setUserName(randomValue);
    e.setInstitutionId(new Random().nextInt());
    return Collections.singletonList(e);
  }
}
