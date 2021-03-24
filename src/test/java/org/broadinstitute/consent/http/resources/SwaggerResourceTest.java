package org.broadinstitute.consent.http.resources;

import static org.broadinstitute.consent.http.resources.SwaggerResource.MEDIA_TYPE_CSS;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SwaggerResourceTest {

    private SwaggerResource swaggerResource;

    @Before
    public void setUp() {
        GoogleOAuth2Config config = new GoogleOAuth2Config();
        config.setClientId(RandomStringUtils.random(10, true, true));
        swaggerResource = new SwaggerResource(config);
    }

    @Test
    public void testIndex() {
        Response response = swaggerResource.content("index.html");
        Assert.assertTrue(checkStatusAndHeader(response, MediaType.TEXT_HTML));
        String content = response.getEntity().toString()
                .replaceFirst("<!--[^-]+-->", "").trim();
        Assert.assertTrue(content.startsWith("<!DOCTYPE html>"));
        Assert.assertTrue(content.endsWith("</html>"));
    }

    @Test
    public void testStyle() {
        Response response = swaggerResource.content("swagger-ui.css");
        Assert.assertTrue(checkStatusAndHeader(response, MEDIA_TYPE_CSS));
        String content = response.getEntity().toString().trim();
        Assert.assertTrue(content.startsWith(".swagger-ui"));
    }

    @Test
    public void testNotFound() {
        Response response = swaggerResource.content("foo/bar.txt");
        Assert.assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testImageNotFound() {
        Response response = swaggerResource.content("foo/bar.png");
        Assert.assertEquals(response.getStatus(), Response.Status.NOT_FOUND.getStatusCode());
    }

    private boolean checkStatusAndHeader(Response response, String header) {
        Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
        Object headerObject = response.getHeaders().get("Content-type");
        return headerObject.toString().contains(header);
    }

}
