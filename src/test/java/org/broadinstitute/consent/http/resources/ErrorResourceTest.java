package org.broadinstitute.consent.http.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ErrorResourceTest {

  @Mock
  private UriInfo uriInfo;

  @Before
  public void setUp() {
    openMocks(this);
  }

  @Test
  public void testNotFound() {
    ErrorResource resource = new ErrorResource();
    when(uriInfo.getPath()).thenReturn("not_found");
    Response response = resource.notFound(uriInfo);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

}
