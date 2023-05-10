package org.broadinstitute.consent.http.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_CSS;
import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_JS;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SwaggerResourceTest {

    private SwaggerResource swaggerResource;

    @BeforeEach
    public void setUp() {
        GoogleOAuth2Config config = new GoogleOAuth2Config();
        config.setClientId(RandomStringUtils.random(10, true, true));
        swaggerResource = new SwaggerResource(config);
    }

    @Test
    public void testIndex() {
        Response response = swaggerResource.content("index.html");
        Assertions.assertTrue(checkStatusAndHeader(response, TEXT_HTML));
        String content = response.getEntity().toString()
                .replaceFirst("<!--[^-]+-->", "").trim();
        Assertions.assertTrue(content.startsWith("<!DOCTYPE html>"));
        Assertions.assertTrue(content.endsWith("</html>"));
    }

    @Test
    public void testInitializer() {
        Response response = swaggerResource.content("swagger-initializer.js");
        Assertions.assertTrue(checkStatusAndHeader(response, MEDIA_TYPE_JS));
        String content = response.getEntity().toString().trim();
        Assertions.assertTrue(content.startsWith("window.onload"));
    }

    @Test
    public void testStyle() {
        Response response = swaggerResource.content("swagger-ui.css");
        Assertions.assertTrue(checkStatusAndHeader(response, MEDIA_TYPE_CSS));
        String content = response.getEntity().toString().trim();
        Assertions.assertTrue(content.startsWith(".swagger-ui"));
    }

    @Test
    public void testNotFound() {
        Response response = swaggerResource.content("foo/bar.txt");
        Assertions.assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testImageNotFound() {
        Response response = swaggerResource.content("foo/bar.png");
        Assertions.assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
    }

    private boolean checkStatusAndHeader(Response response, String header) {
        Assertions.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        String headerObject = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
        return headerObject.contains(header);
    }

}
