package org.broadinstitute.consent.http.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.parboiled.common.FileUtils;

@Path("/")
public class SwaggerResource {

  private static final Logger logger = Logger.getLogger(SwaggerResource.class.getName());
  private static final String DEFAULT_LIB = "META-INF/resources/webjars/swagger-ui/latest/";
  private static final String MEDIA_TYPE_GIF = new MediaType("image", "gif").toString();
  protected static final String MEDIA_TYPE_CSS = new MediaType("text", "css").toString();
  protected static final String MEDIA_TYPE_JS = new MediaType("application",
      "javascript").toString();
  protected static final String MEDIA_TYPE_PNG = new MediaType("image", "png").toString();

  private final GoogleOAuth2Config config;

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
    } else if (path.contains("swagger-initializer.js")) {
      response = Response.ok().entity(getInitializer()).type(MEDIA_TYPE_JS).build();
    } else {
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
    return switch (StringUtils.substringAfterLast(path, ".")) {
      case "css" -> MEDIA_TYPE_CSS;
      case "js" -> MEDIA_TYPE_JS;
      case "png" -> MEDIA_TYPE_PNG;
      case "gif" -> MEDIA_TYPE_GIF;
      default -> MediaType.TEXT_HTML;
    };
  }

  private String getIndex(String swaggerResource) {
    return FileUtils.readAllTextFromResource(swaggerResource + "index.html");
  }

  private String getInitializer() {
    String initString = """
        window.onload = function() {
          const ui = SwaggerUIBundle({
            syntaxHighlight: false,
            docExpansion: "none",
            displayRequestDuration: true,
            tryItOutEnabled: true,
            operationsSorter: "alpha",
            apisSorter: "alpha",
            tagsSorter: "alpha",
            url: "/api-docs/api-docs.yaml",
            dom_id: '#swagger-ui',
            deepLinking: true,
            presets: [
              SwaggerUIBundle.presets.apis,
              SwaggerUIStandalonePreset
            ],
            plugins: [
              SwaggerUIBundle.plugins.DownloadUrl
            ],
            layout: "StandaloneLayout"
          });
          ui.initOAuth({
            clientId: "OAUTH_CLIENT_ID",
            realm: "Broad Institute",
            appName: "Consent",
            scopeSeparator: " ",
            scopes: "openid profile email",
            additionalQueryStringParams: {},
            usePkceWithAuthorizationCodeGrant: true
          });
        };
        """;
    return initString.replace("OAUTH_CLIENT_ID", config.getClientId());
  }
}
