package org.broadinstitute.consent.http.resources;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;
import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_CSS;
import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_JS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwaggerResourceTest extends AbstractTestHelper {

  private SwaggerResource swaggerResource;

  @BeforeEach
  void setUp() {
    OidcConfiguration config = new OidcConfiguration();
    config.setClientId(randomAlphanumeric(10));
    swaggerResource = new SwaggerResource(config);
  }

  @Test
  void testIndex() {
    Response response = swaggerResource.content("index.html");
    assertTrue(checkStatusAndHeader(response, TEXT_HTML));
    String content = response.getEntity().toString().replaceFirst("<!--[^-]+-->", "").trim();
    assertTrue(content.startsWith("<!DOCTYPE html>"));
    assertTrue(content.endsWith("</html>"));
  }

  @Test
  void testInitializer() {
    Response response = swaggerResource.content("swagger-initializer.js");
    assertTrue(checkStatusAndHeader(response, MEDIA_TYPE_JS));
    String content = response.getEntity().toString().trim();
    assertTrue(content.startsWith("window.onload"));
  }

  @Test
  void testStyle() {
    Response response = swaggerResource.content("swagger-ui.css");
    assertTrue(checkStatusAndHeader(response, MEDIA_TYPE_CSS));
    byte[] content = (byte[]) response.getEntity();
    String contentString = new String(content).trim();
    assertTrue(contentString.startsWith(".swagger-ui"));
  }

  @Test
  void testNotFound() {
    Response response = swaggerResource.content("foo/bar.txt");
    assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
  }

  @Test
  void testImageNotFound() {
    Response response = swaggerResource.content("foo/bar.png");
    assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
  }

  private boolean checkStatusAndHeader(Response response, String header) {
    assertEquals(response.getStatus(), Status.OK.getStatusCode());
    String headerObject = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
    return headerObject.contains(header);
  }
}
