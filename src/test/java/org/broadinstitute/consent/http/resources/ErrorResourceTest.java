package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErrorResourceTest {

  @Mock
  private Request request;

  @Test
  void testNotFound() {
    ErrorResource resource = new ErrorResource();
    when(request.getOriginalURI()).thenReturn("not_found");
    try (Response response = resource.notFound(request)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testNotFoundDecoded() {
    String unicode = "¥";
    String encoded = URLEncoder.encode(unicode, Charset.defaultCharset());
    ErrorResource resource = new ErrorResource();
    when(request.getOriginalURI()).thenReturn(encoded);
    try (Response response = resource.notFound(request)) {
      assertTrue(response.getEntity().toString().contains(unicode));
    }
  }

}
