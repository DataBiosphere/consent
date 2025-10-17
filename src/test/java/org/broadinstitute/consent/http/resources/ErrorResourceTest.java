package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler.ServletRequestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErrorResourceTest {

  @Mock
  private HttpServletRequestWrapper mockRequest;
  @Mock
  private ServletApiRequest mockServletRequest;
  @Mock
  private ServletRequestInfo servletRequestInfo;

  @ParameterizedTest
  @ValueSource(strings = {"/not_found", "/context/¥"})
  void testNotFound(String path) {
    ErrorResource resource = new ErrorResource();
    when(mockRequest.getRequest()).thenReturn(mockServletRequest);
    when(mockServletRequest.getServletRequestInfo()).thenReturn(servletRequestInfo);
    when(servletRequestInfo.getDecodedPathInContext()).thenReturn(path);
    try (Response response = resource.notFound(mockRequest)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      assertTrue(response.getEntity().toString().contains(path));
    }
  }
}
