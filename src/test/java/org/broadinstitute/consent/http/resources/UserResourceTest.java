package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.AcknowledgementService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.LibraryCardService;
import org.broadinstitute.consent.http.service.SupportRequestService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class UserResourceTest {

  @Mock private UserService userService;

  @Mock private LibraryCardService libraryCardService;

  @Mock private SamService samService;

  @Mock private DatasetService datasetService;

  @Mock private SupportRequestService supportRequestService;

  private UserResource userResource;

  @Mock private UriInfo uriInfo;

  @Mock private UriBuilder uriBuilder;

  @Mock private UserStatusInfo userStatusInfo;

  @Mock private AcknowledgementService acknowledgementService;

  private final String TEST_EMAIL = "test@gmail.com";

  private AuthUser authUser;

  @Before
  public void setUp() throws URISyntaxException {
    GoogleUser googleUser = new GoogleUser();
    googleUser.setName("Test User");
    googleUser.setEmail(TEST_EMAIL);
    authUser = new AuthUser(googleUser)
            .setAuthToken("auth-token")
            .setUserStatusInfo(userStatusInfo);
    openMocks(this);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/dacuser/api"));
  }

  private void initResource() {
    userResource = new UserResource(samService, userService, datasetService, supportRequestService, acknowledgementService);
  }

  @Test
  public void testGetMe() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
        .thenReturn(createLibraryCards());
    initResource();

    Response response = userResource.getUser(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUserById() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
        .thenReturn(createLibraryCards());
    initResource();

    Response response = userResource.getUserById(authUser, 1);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUserByIdNotFound() {
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenThrow(new NotFoundException());
    initResource();

    Response response = userResource.getUserById(authUser, 1);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUsers_SO() {
    User user = createUserWithRole();
    user.setRoles(List.of(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName())));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.getUsersAsRole(user, "SigningOfficial")).thenReturn(Arrays.asList(new User(), new User()));
    initResource();

    Response response = userResource.getUsers(authUser, "SigningOfficial");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUsers_SO_NoRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUsers(authUser, "SigningOfficial");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUsers_Admin() {
    User user = createUserWithRole();
    user.setRoles(List.of(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName())));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.getUsersAsRole(user, "Admin")).thenReturn(Arrays.asList(new User(), new User()));
    initResource();

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUsers_Admin_NoRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUsers_UnsupportedRole() {
      User user = createUserWithRole();
      when(userService.findUserByEmail(any())).thenReturn(user);
      initResource();

      Response response = userResource.getUsers(authUser, "Researcher");
      assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

  @Test
  public void testGetUsers_InvalidRole() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();

    Response response = userResource.getUsers(authUser, "BadRequest");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUsers_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    initResource();

    Response response = userResource.getUsers(authUser, "Admin");
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateExistingUser() {
    User user = new User();
    user.setEmail(TEST_EMAIL);
    List<UserRole> roles = new ArrayList<>();
    UserRole admin = new UserRole();
    admin.setName(UserRoles.ADMIN.getRoleName());
    UserRole researcher = new UserRole();
    researcher.setName(UserRoles.RESEARCHER.getRoleName());
    roles.add(researcher);
    roles.add(admin);
    user.setRoles(roles);
    when(userService.findUserByEmail(user.getEmail())).thenReturn(user);
    initResource();

    Response response = userResource.createResearcher(uriInfo, authUser);
    Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
  }

  @Test
  public void testCreateFailingGoogleIdentity() {
    initResource();

    Response response = userResource.createResearcher(uriInfo, new AuthUser(TEST_EMAIL));
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void createUserSuccess() {
    User user = new User();
    user.setDisplayName("Test");
    user.setEmail(TEST_EMAIL);
    UserRole researcher = new UserRole();
    List<UserRole> roles = new ArrayList<>();
    researcher.setName(UserRoles.RESEARCHER.getRoleName());
    roles.add(researcher);
    user.setRoles(roles);
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    when(userService.createUser(user)).thenReturn(user);
    initResource();

    Response response = userResource.createResearcher(uriInfo, authUser);
    Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
  }

  @Test
  public void testDeleteUser() {
    doNothing().when(userService).deleteUserByEmail(any());
    initResource();
    Response response = userResource.delete(RandomStringUtils.randomAlphabetic(10), uriInfo);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testAddRoleToUser() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
        .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testAddRoleToUserNotFound() {
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    doThrow(new NotFoundException()).when(userService).findUserById(any());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testAddRoleToUserNotModified() {
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
        .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.RESEARCHER.getRoleId());
    assertEquals(304, response.getStatus());
  }

  @Test
  public void testAddRoleToUserBadRequest() {
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
        .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, 1000);
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testAddRoleToUserBySoWithoutUserAndSoInstitution() {
    User activeUser = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
            .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testAddRoleToUserBySoInstitutionWithoutUserInstitution() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
            .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testAddRoleToUserBySoWithoutSoInstitution() {
    User activeUser = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    User user = createUserWithRole();
    user.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
            .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testAddRoleToUserBySoWithDeniedRoles() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    User user = createUserWithRole();
    user.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
            .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.RESEARCHER.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.MEMBER.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.CHAIRPERSON.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.DATAOWNER.getRoleId());
    assertEquals(400, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.ALUMNI.getRoleId());
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testAddRoleToUserBySoWithPermittedRoles() {
    User activeUser = createUserWithRole();
    activeUser.setInstitutionId(10);
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    User user = createUserWithRole();
    user.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(libraryCardService.findLibraryCardsByUserId(any()))
            .thenReturn(createLibraryCards());
    initResource();
    Response response = userResource.addRoleToUser(authUser, 1, UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(200, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(200, response.getStatus());
    response = userResource.addRoleToUser(authUser, 1, UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(200, response.getStatus());
  }

  @SuppressWarnings({"unchecked"})
  @Test
  public void testGetSOsForInstitution() {
    User user = createUserWithInstitution();
    User so = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findSOsByInstitutionId(any())).thenReturn(Arrays.asList(new UserService.SimplifiedUser(so), new UserService.SimplifiedUser(so)));
    initResource();
    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List<UserService.SimplifiedUser>) response.getEntity();
    assertFalse(body.isEmpty());
    assertEquals(so.getDisplayName(), body.get(0).displayName);
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testGetSOsForInstitution_NoInstitution() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findSOsByInstitutionId(any())).thenReturn(Collections.emptyList());
    initResource();
    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var body = (List) response.getEntity();
    assertTrue(body.isEmpty());
  }

  @Test
  public void testGetSOsForInstitution_UserNotFound() {
    when(userService.findUserByEmail(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = userResource.getSOsForInstitution(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetUnassignedUsers() {
    List<User> users = Collections.singletonList(createUserWithRole());
    when(userService.findUsersWithNoInstitution()).thenReturn(users);
    initResource();
    Response response = userResource.getUnassignedUsers(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetUsersByInstitutionNoInstitution() {
    Integer institutionId = 1;
    doThrow(new NotFoundException()).when(userService).findUsersByInstitutionId(institutionId);
    initResource();

    Response response = userResource.getUsersByInstitution(authUser, institutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetUsersByInstitutionNullInstitution() {
    Integer institutionId = null;
    doThrow(new IllegalArgumentException()).when(userService).findUsersByInstitutionId(institutionId);
    initResource();

    Response response = userResource.getUsersByInstitution(authUser, institutionId);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetUsersByInstitutionSuccess() {
    when(userService.findUsersByInstitutionId(any())).thenReturn(Collections.emptyList());
    initResource();

    Response response = userResource.getUsersByInstitution(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateSelf() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    verify(supportRequestService, times(1)).handleInstitutionSOSupportRequest(any(), any());
  }

  @Test
  public void testUpdateSelfRolesNotAdmin() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setUserRoleIds(List.of(1)); // any roles
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    //no support request sent if update to user fails
    verify(supportRequestService, times(0)).handleInstitutionSOSupportRequest(any(), any());
  }

  @Test
  public void testUpdateSelfInstitutionIdAsSO() {
    User user = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.addRole(so);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(10);
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateSelfInstitutionIdAsSO_ExistingInstitution() {
    User user = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.addRole(so);
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(20);
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateSelfInstitutionIdAsSO_SameInstitution() {
    User user = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.addRole(so);
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(10);
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateSelfInstitutionIdAsITDirector() {
    User user = createUserWithRole();
    UserRole itd = new UserRole(UserRoles.ITDIRECTOR.getRoleId(), UserRoles.ITDIRECTOR.getRoleName());
    user.addRole(itd);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(10);
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateSelfInstitutionIdAsITDirector_ExistingInstitution() {
    User user = createUserWithRole();
    UserRole itd = new UserRole(UserRoles.ITDIRECTOR.getRoleId(), UserRoles.ITDIRECTOR.getRoleName());
    user.addRole(itd);
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(20);
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateSelfInstitutionIdNullAsSO_ExistingInstitution() {
    User user = createUserWithRole();
    UserRole itd = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.addRole(itd);
    user.setInstitutionId(10);
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    userUpdateFields.setInstitutionId(null);
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    spy(supportRequestService);
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    user.setInstitutionId(20);
    userUpdateFields.setInstitutionId(20);
    Response response2 = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response2.getStatus());
  }


  @Test
  public void testUpdateSelfSupportRequestError() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    doThrow(new ServerErrorException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR))
            .when(supportRequestService).handleInstitutionSOSupportRequest(any(), any());
    initResource();
    Response response = userResource.updateSelf(authUser, uriInfo, gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  public void testUpdate() {
    User user = createUserWithRole();
    UserUpdateFields userUpdateFields = new UserUpdateFields();
    Gson gson = new Gson();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.updateUserFieldsById(any(), any())).thenReturn(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(gson.toJsonTree(user).getAsJsonObject());
    initResource();
    Response response = userResource.update(authUser, uriInfo, user.getUserId(), gson.toJson(userUpdateFields));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testUpdateUserNotFound() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = userResource.update(authUser, uriInfo, user.getUserId(), "");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateUserInvalidJson() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    initResource();
    Response response = userResource.update(authUser, uriInfo, user.getUserId(), "}{][");
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  public void testDeleteRoleFromUser() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    Gson gson = new Gson();
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(userJson.getAsJsonObject());
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(), UserRoles.RESEARCHER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    User returnedUser = new User((String)response.getEntity());
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  public void testDeleteRoleFromUser_InvalidRole() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(), 20);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testDeleteDeniedRoleBySoShouldFail() {
    User user = createUserWithRole();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    user.addRole(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    user.addRole(new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName()));
    user.addRole(new UserRole(UserRoles.ALUMNI.getRoleId(), UserRoles.ALUMNI.getRoleName()));
    user.addRole(new UserRole(UserRoles.DATAOWNER.getRoleId(), UserRoles.DATAOWNER.getRoleName()));
    user.setInstitutionId(10);
    User activeUser = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.ADMIN.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.RESEARCHER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.CHAIRPERSON.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.MEMBER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.ALUMNI.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.DATAOWNER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  public void testDeletePermittedRolesBySoShouldSucceedForUserWithSameInstitution() {
    User user = createUserWithRole();
    user.addRole(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    user.addRole(new UserRole(UserRoles.DATASUBMITTER.getRoleId(), UserRoles.DATASUBMITTER.getRoleName()));
    user.addRole(new UserRole(UserRoles.ITDIRECTOR.getRoleId(), UserRoles.ITDIRECTOR.getRoleName()));
    user.setInstitutionId(10);
    User activeUser = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testDeletePermittedRolesBySoShouldFailForUserWitNullInstitution() {
    User user = createUserWithRole();
    user.addRole(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    user.addRole(new UserRole(UserRoles.DATASUBMITTER.getRoleId(), UserRoles.DATASUBMITTER.getRoleName()));
    user.addRole(new UserRole(UserRoles.ITDIRECTOR.getRoleId(), UserRoles.ITDIRECTOR.getRoleName()));
    User activeUser = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    activeUser.addRole(so);
    activeUser.setInstitutionId(10);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.ITDIRECTOR.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.DATASUBMITTER.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  public void testDeleteSORoleFromSOInOtherOrgSOShouldFail() {
    User user = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.addRole(so);
    user.setInstitutionId(1);
    User activeUser = createUserWithRole();
    activeUser.addRole(so);
    activeUser.setInstitutionId(2);
    assertNotEquals(user.getInstitutionId(), activeUser.getInstitutionId());
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  public void testDeleteSORoleFromSelfShouldFail() {
    User user = createUserWithRole();
    UserRole so = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
    user.addRole(so);
    user.setInstitutionId(1);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(user);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(),  UserRoles.SIGNINGOFFICIAL.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }
  @Test
  public void testDeleteRoleFromUser_UserWithoutRole() {
    User user = createUserWithRole();
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    Gson gson = new Gson();
    JsonElement userJson = gson.toJsonTree(user);
    when(userService.findUserWithPropertiesByIdAsJsonObject(any(), any())).thenReturn(userJson.getAsJsonObject());
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, user.getUserId(), UserRoles.ADMIN.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    User returnedUser = new User((String)response.getEntity());
    assertEquals(user.getEmail(), returnedUser.getEmail());
  }

  @Test
  public void testDeleteRoleFromUser_UserNotFound() {
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, 1, UserRoles.ADMIN.getRoleId());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testDeleteRoleFromUserInvalidRoleId() {
    User activeUser = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    activeUser.addRole(admin);
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    when(userService.findUserByEmail(any())).thenReturn(activeUser);
    initResource();
    Response response = userResource.deleteRoleFromUser(authUser, 1, 1000);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testGetDatasetsFromUserDacs() {
    User user = createUserWithRole();
    UserRole admin = new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName());
    user.addRole(admin);
    when(datasetService.findDatasetsByDacIds(anyList())).thenReturn(Collections.emptySet());
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    initResource();

    Response response = userResource.getDatasetsFromUserDacs(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostAcknowledgement(){
    User user = createUserWithRole();
    String acknowledgementKey = "key1";
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.makeAcknowledgements(anyList(), any())).thenReturn(acknowledgementMap);
    initResource();

    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));
    Response response = userResource.postAcknowledgements(authUser, jsonString);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testPostAcknowledgementException(){
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("exception during post")).when(acknowledgementService).makeAcknowledgements(anyList(), any());
    initResource();
    String jsonString = userResource.unmarshal(List.of(acknowledgementKey));

    Response response = userResource.postAcknowledgements(authUser, jsonString);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  public void testPostAcknowledgementBadJson(){
    doThrow(new RuntimeException("exception during post")).when(acknowledgementService).makeAcknowledgements(anyList(), any());
    initResource();
    String jsonString = "The quick brown fox jumped over the lazy dog.";

    Response response = userResource.postAcknowledgements(authUser, jsonString);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testPostAcknowledgementEmptyJson(){
    initResource();

    Response response = userResource.postAcknowledgements(authUser, "");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testPostAcknowledgementEmptyJsonList(){
    initResource();

    Response response = userResource.postAcknowledgements(authUser, "[]");
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void testMissingAcknowledgement(){
    String acknowledgementKey = "key1";
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetAcknowledgementException(){
    String acknowledgementKey = "key1";
    doThrow(new RuntimeException("some exception during get.")).when(acknowledgementService).findAcknowledgementForUserByKey(any(), any());
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser, acknowledgementKey);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetAcknowledgementNull(){
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(null);
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser, null);
    assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUnsetAcknowledgementsForUser(){
    when(acknowledgementService.findAcknowledgementsForUser(any())).thenReturn(null);
    initResource();

    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetAcknowledgementsForUserException(){
    doThrow(new RuntimeException("some get exception")).when(acknowledgementService).findAcknowledgementsForUser(any());
    initResource();

    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetSetAcknowledgementForUser(){
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(acknowledgementMap.get(acknowledgementKey));
    initResource();

    Response response = userResource.getUserAcknowledgement(authUser,acknowledgementKey);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetAllAcknowledgements(){
    String acknowledgementKey = "key1";
    User user = createUserWithRole();
    Map<String, Acknowledgement> acknowledgementMap = getDefaultAcknowledgementForUser(user, acknowledgementKey);
    when(acknowledgementService.findAcknowledgementForUserByKey(any(), any())).thenReturn(acknowledgementMap.get(acknowledgementKey));
    initResource();

    Response response = userResource.getUserAcknowledgements(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  private Map<String, Acknowledgement> getDefaultAcknowledgementForUser(User user, String acknowledgementKey) {
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
    user.setUserId(RandomUtils.nextInt(1,100));
    user.setDisplayName("Test");
    user.setEmail("Test");
    UserRole researcher = new UserRole();
    List<UserRole> roles = new ArrayList<>();
    researcher.setName(UserRoles.RESEARCHER.getRoleName());
    researcher.setRoleId(UserRoles.RESEARCHER.getRoleId());
    roles.add(researcher);
    user.setRoles(roles);
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
