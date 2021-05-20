package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.service.MatchService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class MatchResourceTest {

  @Mock private MatchService service;

  @Mock private AuthUser authUser;

  private MatchResource resource;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void initResource() {
    resource = new MatchResource(service);
  }

  @Test
  public void testGetMatchByConsentAndPurpose() {
    when(service.findMatchByConsentIdAndPurposeId(any(), any())).thenReturn(new Match());
    initResource();

    Response response = resource.getMatchByConsentAndPurpose(authUser,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testGetMatchByConsentAndPurposeNotFound() {
    doThrow(new NotFoundException()).when(service).findMatchByConsentIdAndPurposeId(any(), any());
    initResource();

    Response response = resource.getMatchByConsentAndPurpose(authUser,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testGetMatchByConsentAndPurposeServerException() {
    doThrow(new ServerErrorException(500))
        .when(service)
        .findMatchByConsentIdAndPurposeId(any(), any());
    initResource();

    Response response = resource.getMatchByConsentAndPurpose(authUser,
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

}
