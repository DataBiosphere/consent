package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.AuthUser;
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

public class SamResourceTest {

  @Mock AuthUser authUser;

  @Mock SamService service;

  @Mock UriInfo uriInfo;

  SamResource resource;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void initResource() {
    resource = new SamResource(service);
  }

  @Test
  public void testGetResourceTypes() throws Exception {
    when(service.getResourceTypes(any())).thenReturn(Collections.emptyList());
    initResource();
    Response response = resource.getResourceTypes(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testPostRegistrationInfo() throws Exception {
    UserStatusInfo userInfo = new UserStatusInfo()
            .setUserEmail("test@test.org")
            .setUserSubjectId(RandomStringUtils.random(10, false, true))
            .setEnabled(RandomUtils.nextBoolean());
    when(service.postRegistrationInfo(any())).thenReturn(userInfo);
    initResource();
    Response response = resource.postRegistrationInfo(authUser, uriInfo);
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
  }

  @Test
  public void testGetSelfDiagnostics() throws Exception {
    UserStatusDiagnostics diagnostics = new UserStatusDiagnostics()
            .setEnabled(RandomUtils.nextBoolean())
            .setInAllUsersGroup(RandomUtils.nextBoolean())
            .setInGoogleProxyGroup(RandomUtils.nextBoolean());
    when(service.getSelfDiagnostics(any())).thenReturn(diagnostics);
    initResource();
    Response response = resource.getSelfDiagnostics(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetRegistrationInfo() throws Exception {
    UserStatusInfo userInfo = new UserStatusInfo()
            .setUserEmail("test@test.org")
            .setUserSubjectId(RandomStringUtils.random(10, false, true))
            .setEnabled(RandomUtils.nextBoolean());
    when(service.getRegistrationInfo(any())).thenReturn(userInfo);
    initResource();
    Response response = resource.getRegistrationInfo(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }
}
