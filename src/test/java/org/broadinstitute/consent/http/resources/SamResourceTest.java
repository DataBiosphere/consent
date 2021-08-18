package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SamResourceTest {

  @Mock AuthUser authUser;

  @Mock SamService service;

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
    assertEquals(200, response.getStatus());
  }
}
