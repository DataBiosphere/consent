package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.WhitelistEntry;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.WhitelistService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UserResourceTest {

  @Mock private UserService userService;

  @Mock private WhitelistService whitelistService;

  private UserResource userResource;

  @Mock private UriInfo uriInfo;

  @Mock private UriBuilder uriBuilder;

  private final String TEST_EMAIL = "test@gmail.com";

  private AuthUser authUser;

  @Before
  public void setUp() throws URISyntaxException {
    GoogleUser googleUser = new GoogleUser();
    googleUser.setName("Test User");
    googleUser.setEmail(TEST_EMAIL);
    authUser = new AuthUser(googleUser);
    MockitoAnnotations.initMocks(this);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build(anyString())).thenReturn(new URI("http://localhost:8180/dacuser/api"));
  }

  private void initResource() {
    userResource = new UserResource(userService, whitelistService);
  }

  @Test
  public void testGetMe() {
    User user = createUserWithRole();
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(whitelistService.findWhitelistEntriesForUser(any(), any()))
        .thenReturn(createWhitelistEntries());
    initResource();

    Response response = userResource.getUser(authUser);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUserById() {
    User user = createUserWithRole();
    when(userService.findUserById(any())).thenReturn(user);
    when(userService.findAllUserProperties(any())).thenReturn(createResearcherProperties());
    when(whitelistService.findWhitelistEntriesForUser(any(), any()))
        .thenReturn(createWhitelistEntries());
    initResource();

    Response response = userResource.getUserById(authUser, 1);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void testGetUserByIdNotFound() {
    when(userService.findUserById(any())).thenThrow(new NotFoundException());
    initResource();

    Response response = userResource.getUserById(authUser, 1);
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
    User user = new User();
    user.setEmail(TEST_EMAIL);
    initResource();

    Response response = userResource.createResearcher(uriInfo, new AuthUser(TEST_EMAIL));
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void createUserSuccess() {
    User user = new User();
    user.setDisplayName("Test");
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
    Response response = userResource.delete(RandomStringUtils.random(10), uriInfo);
    assertEquals(200, response.getStatus());
  }

  private User createUserWithRole() {
    User user = new User();
    user.setDacUserId(1);
    user.setDisplayName("Test");
    user.setEmail("Test");
    UserRole researcher = new UserRole();
    List<UserRole> roles = new ArrayList<>();
    researcher.setName(UserRoles.RESEARCHER.getRoleName());
    roles.add(researcher);
    user.setRoles(roles);
    return user;
  }

  private List<ResearcherProperty> createResearcherProperties() {
    return Arrays.stream(ResearcherFields.values())
        .filter(ResearcherFields::getRequired)
        .map(f -> new ResearcherProperty(1, f.getValue(), f.getValue()))
        .collect(Collectors.toList());
  }

  private List<WhitelistEntry> createWhitelistEntries() {
    WhitelistEntry e = new WhitelistEntry();
    String randomValue = RandomStringUtils.random(10, true, false);
    e.setCommonsId(randomValue);
    e.setEmail(randomValue);
    e.setItDirectorEmail(randomValue);
    e.setItDirectorName(randomValue);
    e.setName(randomValue);
    e.setOrganization(randomValue);
    e.setSigningOfficialEmail(randomValue);
    e.setSigningOfficialName(randomValue);
    return Collections.singletonList(e);
  }
}
