package org.broadinstitute.consent.http.resources;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.service.OidcService;

@Path("oauth2")
public class OAuth2Resource extends Resource {
  private final OidcService oidcService;

  @Inject
  public OAuth2Resource(OidcService oidcService) {
    this.oidcService = oidcService;
  }

  @Path("authorize")
  @GET
  public Response getAuthorizationEndpoint(@Context UriInfo uriInfo) {
    return Response.status(Status.FOUND).location(oidcService.getAuthorizationURI(uriInfo.getQueryParameters())).build();
  }

  @Path("token")
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getToken(@Context UriInfo uriInfo, MultivaluedMap<String, String> bodyParams) {
    return Response.ok(oidcService.tokenExchange(bodyParams, uriInfo.getQueryParameters()), MediaType.APPLICATION_JSON).build();
  }

  @Path("configuration")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getOAuth2Configuration() {
    return Response.ok(oidcService.getOAuth2Configuration()).build();
  }
}
