package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.parboiled.common.FileUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/swagger")
public class SwaggerResource {

    private GoogleOAuth2Config config;

    public SwaggerResource(GoogleOAuth2Config config) {
        this.config = config;
    }

    private final static String swaggerResource = "META-INF/resources/webjars/swagger-ui/2.2.8/";

    @Context
    UriInfo uriInfo;

    @GET
    @Produces("text/plain")
    @Path("api-docs.yml")
    public Response apiDocsYaml() {
        String content = FileUtils.readAllTextFromResource("resources/assets/api-docs.yaml");
        return Response.ok().entity(content).build();
    }

    @GET
    @Produces("application/js")
    @Path("swagger-ui.js")
    public Response swaggerUiJs() {
        String content = FileUtils.readAllTextFromResource(swaggerResource + "swagger-ui.js");
        return Response.ok().entity(content).build();
    }

    @GET
    @Produces("application/js")
    @Path("lib/{remainder}")
    public Response lib(@PathParam("remainder") String remainder) {
        String content = FileUtils.readAllTextFromResource(swaggerResource + "lib/" + remainder);
        return Response.ok().entity(content).build();
    }

    @GET
    @Produces("text/css")
    @Path("css/{remainder}")
    public Response css(@PathParam("remainder") String remainder) {
        String content = FileUtils.readAllTextFromResource(swaggerResource + "css/" + remainder);
        return Response.ok().entity(content).build();
    }

    @GET
    @Produces("image/png")
    @Path("images/{remainder}")
    public Response images(@PathParam("remainder") String remainder) {
        byte[] content = FileUtils.readAllBytesFromResource(swaggerResource + "images/" + remainder);
        return Response.ok().entity(content).build();
    }

    @GET
    @Produces("text/html")
    @Path("{remainder}")
    public Response content(@PathParam("remainder") String remainder) {
        String content;
        if (remainder.isEmpty() || remainder.equals("index.html")) {
            content = getIndex();
        } else {
            content = FileUtils.readAllTextFromResource(swaggerResource + remainder);
        }
        return Response.ok().entity(content).build();
    }

    private String getIndex() {
        String content = FileUtils.readAllTextFromResource(swaggerResource + "index.html");
        return content
            .replace("your-client-id", config.getClientId())
            .replace("your-client-secret-if-required", "")
            .replace("your-realms", "Broad Institute")
            .replace("your-app-name", "Consent")
            .replace("scopeSeparator: \",\"", "scopeSeparator: \" \"")
            .replace("jsonEditor: false,", "jsonEditor: false," + "validatorUrl: null, apisSorter: \"alpha\", operationsSorter: \"alpha\",")
            .replace("url = \"http://petstore.swagger.io/v2/swagger.json\";", "url = '/swagger/api-docs.yaml';");
    }

}
