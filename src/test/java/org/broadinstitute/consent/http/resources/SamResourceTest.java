package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.ActionPattern;
import org.broadinstitute.consent.http.models.sam.ResourceType;
import org.broadinstitute.consent.http.models.sam.ResourceTypeRole;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusDiagnostics;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SamResourceTest {

  @Mock private AuthUser authUser;

  @Mock private SamService service;

  @Mock private UriInfo uriInfo;

  private SamResource resource;

  @Before
  public void setUp() {
    openMocks(this);
  }

  private void initResource() {
    resource = new SamResource(service);
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
    when(service.getResourceTypes(any())).thenReturn(Collections.singletonList(type));
    initResource();
    Response response = resource.getResourceTypes(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostRegistrationInfo() throws Exception {
    UserStatus.UserInfo info = new UserStatus.UserInfo().setUserEmail("test@test.org").setUserSubjectId("subjectId");
    UserStatus.Enabled enabled = new UserStatus.Enabled().setAllUsersGroup(true).setGoogle(true).setLdap(true);
    UserStatus status = new UserStatus().setUserInfo(info).setEnabled(enabled);
    when(service.postRegistrationInfo(any())).thenReturn(status);
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
    when(service.getSelfDiagnostics(any())).thenReturn(diagnostics);
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
    when(service.getRegistrationInfo(any())).thenReturn(userInfo);
    initResource();
    Response response = resource.getRegistrationInfo(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
