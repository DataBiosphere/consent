package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.AssetCatalogService;

@Path("api/admin/asset-catalog")
public class AssetCatalogResource extends Resource {

  private final AssetCatalogService assetCatalogService;

  @Inject
  public AssetCatalogResource(AssetCatalogService assetCatalogService) {
    this.assetCatalogService = assetCatalogService;
  }

  @GET
  @Path("check")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN)
  @Timed
  public Response check(@Auth DuosUser duosUser) {
    try {
      return Response.ok(assetCatalogService.check()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Path("repair")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN)
  @Timed
  public Response repair(@Auth DuosUser duosUser) {
    try {
      return Response.ok(assetCatalogService.repair()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
