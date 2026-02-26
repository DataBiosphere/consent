package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.parboiled.common.FileUtils;

@Path("/")
public class SwaggerResource implements ConsentLogger {
  private static final String DEFAULT_SWAGGER_UI_PATH =
      "META-INF/resources/webjars/swagger-ui/latest/";
  private static final String MEDIA_TYPE_GIF = new MediaType("image", "gif").toString();
  protected static final String MEDIA_TYPE_CSS = new MediaType("text", "css").toString();
  protected static final String MEDIA_TYPE_JS =
      new MediaType("application", "javascript").toString();
  protected static final String MEDIA_TYPE_PNG = new MediaType("image", "png").toString();

  private final GoogleOAuth2Config config;
  private final String swaggerUiPath;

  @Inject
  public SwaggerResource(GoogleOAuth2Config config) {
    this.config = config;
    this.swaggerUiPath = loadSwaggerUiPath();
  }

  /**
   * Load the Swagger UI path from mvn.properties, which is populated from pom.xml during the Maven
   * build process.
   */
  private String loadSwaggerUiPath() {
    try (InputStream is = getClass().getResourceAsStream("/mvn.properties")) {
      if (is != null) {
        Properties props = new Properties();
        props.load(is);
        String path = props.getProperty("swagger.ui.path");
        if (StringUtils.isNotEmpty(path)) {
          return path;
        }
      }
    } catch (IOException e) {
      logWarn(e.getMessage());
      // Fall through to default
    }
    logWarn("swagger.ui.path not found in mvn.properties, using default path.");
    return DEFAULT_SWAGGER_UI_PATH;
  }

  @GET
  public Response main() {
    return content("");
  }

  @GET
  @Path("{path:.*}")
  public Response content(@PathParam("path") String path) {
    String mediaType = getMediaTypeFromPath(path);
    // Special handling for index.html and swagger-initializer.js
    if (path.isEmpty() || path.equals("index.html")) {
      return Response.ok().entity(getIndex()).type(mediaType).build();
    } else if (path.contains("swagger-initializer.js")) {
      return Response.ok().entity(getInitializer()).type(mediaType).build();
    }
    // Serve all other files as bytes
    byte[] content = FileUtils.readAllBytesFromResource(swaggerUiPath + path);
    if (content != null) {
      return Response.ok().entity(content).type(mediaType).build();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
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

  private String getIndex() {
    return FileUtils.readAllTextFromResource(swaggerUiPath + "index.html");
  }

  private String getInitializer() {
    String initString =
        """
        window.onload = function() {
          const ui = SwaggerUIBundle({
            syntaxHighlight: false,
            docExpansion: "none",
            displayRequestDuration: true,
            tryItOutEnabled: true,
            operationsSorter: "alpha",
            apisSorter: "alpha",
            tagsSorter: "alpha",
            url: "/api-docs/openapi.yaml",
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
