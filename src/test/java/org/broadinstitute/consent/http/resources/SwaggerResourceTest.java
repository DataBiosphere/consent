package org.broadinstitute.consent.http.resources;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;
import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_CSS;
import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_JS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SwaggerResourceTest {

  private SwaggerResource swaggerResource;

  @BeforeEach
  void setUp() {
    GoogleOAuth2Config config = new GoogleOAuth2Config();
    config.setClientId(RandomStringUtils.random(10, true, true));
    swaggerResource = new SwaggerResource(config);
  }

  @Test
  void testIndex() {
    Response response = swaggerResource.content("index.html");
    assertTrue(checkStatusAndHeader(response, TEXT_HTML));
    String content = response.getEntity().toString()
        .replaceFirst("<!--[^-]+-->", "").trim();
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
    String content = response.getEntity().toString().trim();
    assertTrue(content.startsWith(".swagger-ui"));
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
