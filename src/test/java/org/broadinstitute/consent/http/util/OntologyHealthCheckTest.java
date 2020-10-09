package org.broadinstitute.consent.http.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.service.ontology.OntologyHealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OntologyHealthCheckTest {

  @Mock private Client client;

  @Mock private ServicesConfiguration servicesConfiguration;

  @Mock private WebTarget target;

  @Mock private Invocation.Builder builder;

  @Mock private Response response;

  private OntologyHealthCheck healthCheck;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void initHealthCheck() {
    when(builder.get()).thenReturn(response);
    when(target.request(anyString())).thenReturn(builder);
    when(client.target(anyString())).thenReturn(target);
    when(servicesConfiguration.getOntologyURL()).thenReturn("http://localhost:8000/");
    healthCheck = new OntologyHealthCheck(client, servicesConfiguration);
  }

  @Test
  public void testCheckSuccess() {
    when(response.getStatus()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    initHealthCheck();

    HealthCheck.Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void testCheckFailure() {
    when(response.getStatus()).thenReturn(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
    initHealthCheck();

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }

  @Test
  public void testCheckException() {
    doThrow(new RuntimeException()).when(response).getStatus();
    initHealthCheck();

    HealthCheck.Result result = healthCheck.check();
    assertFalse(result.isHealthy());
  }
}
