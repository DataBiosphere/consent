package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ActionPattern;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.ResourceTypeRole;
import org.broadinstitute.consent.http.models.sam.TosResponse;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SamResourceTest {

  @Mock private AuthUser authUser;

  @Mock private SamService samService;

  @Mock private UserService userService;

  @Mock private UriInfo uriInfo;

  private SamResource resource;

  @Before
  public void setUp() {
    openMocks(this);
  }

  private void initResource() {
    resource = new SamResource(samService, userService);
  }

  @Test
  public void testGetResourceTypes() throws Exception {
    ActionPattern pattern = new ActionPattern()
            .setAuthDomainConstrainable(true)
            .setDescription("description")
            .setValue("value");
    ResourceTypeRole role = new ResourceTypeRole()
            .setRoleName("roleName")
            .setActions(Collections.singletonList("action"))
            .setDescendantRoles(Collections.emptyMap())
            .setIncludedRoles(Collections.emptyList())
            .setIncludedRoles(Collections.emptyList());
    ResourceType type = new ResourceType()
            .setName("name")
            .setReuseIds(true)
            .setOwnerRoleName("ownerRoleName")
            .setActionPatterns(Collections.singletonList(pattern))
            .setRoles(Collections.singletonList(role));
    when(samService.getResourceTypes(any())).thenReturn(Collections.singletonList(type));
    initResource();
    Response response = resource.getResourceTypes(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostRegistrationInfo() throws Exception {
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true).setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    when(samService.postRegistrationInfo(any())).thenReturn(status);
    initResource();
    Response response = resource.postRegistrationInfo(authUser, uriInfo);
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
  }

  @Test
  public void testGetSelfDiagnostics() throws Exception {
    UserStatusDiagnostics diagnostics = new UserStatusDiagnostics()
            .setAdminEnabled(RandomUtils.nextBoolean())
            .setEnabled(RandomUtils.nextBoolean())
            .setInAllUsersGroup(RandomUtils.nextBoolean())
            .setInGoogleProxyGroup(RandomUtils.nextBoolean())
            .setTosAccepted(RandomUtils.nextBoolean());
    when(samService.getSelfDiagnostics(any())).thenReturn(diagnostics);
    initResource();
    Response response = resource.getSelfDiagnostics(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetRegistrationInfo() throws Exception {
    UserStatusInfo userInfo = new UserStatusInfo()
            .setAdminEnabled(RandomUtils.nextBoolean())
            .setUserEmail("test@test.org")
            .setUserSubjectId(RandomStringUtils.random(10, false, true))
            .setEnabled(RandomUtils.nextBoolean());
    when(samService.getRegistrationInfo(any())).thenReturn(userInfo);
    initResource();
    Response response = resource.getRegistrationInfo(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostSelfTos() throws Exception {
    TosResponse.Enabled enabled = new TosResponse.Enabled()
      .setAdminEnabled(true).setTosAccepted(true).setGoogle(true).setAllUsersGroup(true).setLdap(true);
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
    when(samService.postTosAcceptedStatus(any())).thenReturn(tosResponse);
    initResource();
    Response response = resource.postSelfTos(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostSelfTos_NoConsentUser() throws Exception {
    TosResponse.Enabled enabled = new TosResponse.Enabled()
      .setAdminEnabled(true).setTosAccepted(true).setGoogle(true).setAllUsersGroup(true).setLdap(true);
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
    spy(userService);
    doThrow(new NotFoundException()).when(userService).findUserByEmail(any());
    when(samService.postTosAcceptedStatus(any())).thenReturn(tosResponse);
    initResource();

    Response response = resource.postSelfTos(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    verify(userService, times(1)).createUser(any());
  }

  @Test
  public void testPostSelfTos_ExistingSamUser() throws Exception {
    TosResponse.Enabled enabled = new TosResponse.Enabled()
      .setAdminEnabled(true).setTosAccepted(true).setGoogle(true).setAllUsersGroup(true).setLdap(true);
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
    spy(samService);
    doThrow(new ConsentConflictException()).when(samService).postRegistrationInfo(any());
    when(samService.postTosAcceptedStatus(any())).thenReturn(tosResponse);
    initResource();

    Response response = resource.postSelfTos(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostSelfTos_NoSamUser() throws Exception {
    TosResponse.Enabled enabled = new TosResponse.Enabled()
      .setAdminEnabled(true).setTosAccepted(true).setGoogle(true).setAllUsersGroup(true).setLdap(true);
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
    spy(samService);
    when(samService.postTosAcceptedStatus(any())).thenReturn(tosResponse);
    initResource();

    Response response = resource.postSelfTos(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    verify(samService, times(1)).postRegistrationInfo(any());
  }

  @Test
  public void testRemoveSelfTos() throws Exception {
    TosResponse.Enabled enabled = new TosResponse.Enabled()
            .setAdminEnabled(true).setTosAccepted(false).setGoogle(true).setAllUsersGroup(true).setLdap(true);
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    TosResponse tosResponse = new TosResponse().setEnabled(enabled).setUserInfo(info);
    when(samService.removeTosAcceptedStatus(any())).thenReturn(tosResponse);
    initResource();
    Response response = resource.removeTos(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

}
