package org.broadinstitute.consent.http.resources;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.parboiled.common.FileUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Logger;

@Path("/")
public class SwaggerResource {

    private static final Logger logger = Logger.getLogger(SwaggerResource.class.getName());
    // Default swagger ui library if not found in properties
    // should not hard-code the actual version here!
    private final static String DEFAULT_LIB = "META-INF/resources/webjars/swagger-ui/latest/";
    final static String MEDIA_TYPE_CSS = new MediaType("text", "css").toString();
    final static String MEDIA_TYPE_JS = new MediaType("application", "js").toString();
    final static String MEDIA_TYPE_PNG = new MediaType("image", "png").toString();
    final static String MEDIA_TYPE_GIF = new MediaType("image", "gif").toString();

    private GoogleOAuth2Config config;

    public SwaggerResource(GoogleOAuth2Config config) {
        this.config = config;
    }

    private String swaggerResource = null;

    private String getSwaggerResource() {
        if (swaggerResource == null) {
            try (InputStream is = this.getClass().getResourceAsStream("/mvn.properties")) {
                Properties p = new Properties();
                p.load(is);
                if (StringUtils.isNotEmpty(p.getProperty("swagger.ui.path"))) {
                    swaggerResource = p.getProperty("swagger.ui.path");
                } else {
                    logger.warning("swagger.ui.path is not configured correctly");
                    swaggerResource = DEFAULT_LIB;
                }
            } catch (Exception e) {
                logger.warning(e.getMessage());
                swaggerResource = DEFAULT_LIB;
            }
        }
        return swaggerResource;
    }

    @Context
    UriInfo uriInfo;

    @GET
    public Response main() {
        return content("");
    }

    @GET
    @Path("swagger")
    public Response swagger() {
        URI uri = UriBuilder.fromPath("/").scheme("https").build();
        return Response.seeOther(uri).build();
    }

    @GET
    @Path("{path:.*}")
    public Response content(@PathParam("path") String path) {
        String swaggerResource = getSwaggerResource();
        Response response;
        String mediaType = getMediaTypeFromPath(path);
        if (path.isEmpty() || path.equals("index.html")) {
            response = Response.ok().entity(getIndex(swaggerResource)).type(mediaType).build();
        } else {
            mediaType = getMediaTypeFromPath(path);
            if (path.endsWith("png") || path.endsWith("gif")) {
                byte[] content = FileUtils.readAllBytesFromResource(swaggerResource + path);
                if (content != null) {
                    response = Response.ok().entity(content).type(mediaType).build();
                } else {
                    response = Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                String content = FileUtils.readAllTextFromResource(swaggerResource + path);
                if (content != null) {
                    response = Response.ok().entity(content).type(mediaType).build();
                } else {
                    response = Response.status(Response.Status.NOT_FOUND).build();
                }
            }
        }
        return response;
    }

    private String getMediaTypeFromPath(String path) {
        String mediaType;
        switch (StringUtils.substringAfterLast(path, ".")) {
            case "css":
                mediaType = MEDIA_TYPE_CSS;
                break;
            case "js":
                mediaType = MEDIA_TYPE_JS;
                break;
            case "png":
                mediaType = MEDIA_TYPE_PNG;
                break;
            case "gif":
                mediaType = MEDIA_TYPE_GIF;
                break;
            default:
                mediaType = MediaType.TEXT_HTML;
                break;
        }
        return mediaType;
    }

    private String getIndex(String swaggerResource) {
        String content = FileUtils.readAllTextFromResource(swaggerResource + "index.html");
        return content
            .replace("your-client-id", config.getClientId())
            .replace("your-client-secret-if-required", "")
            .replace("your-realms", "Broad Institute")
            .replace("your-app-name", "Consent")
            .replace("scopeSeparator: \",\"", "scopeSeparator: \" \"")
            .replace("jsonEditor: false,", "jsonEditor: false," + "validatorUrl: null, apisSorter: \"alpha\", operationsSorter: \"alpha\",")
            .replace("url = \"http://petstore.swagger.io/v2/swagger.json\";", "url = '/api-docs/api-docs.yaml';");
    }

}
