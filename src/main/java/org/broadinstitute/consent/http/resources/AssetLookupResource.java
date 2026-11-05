package org.broadinstitute.consent.http.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.service.AssetLookupService;

@Path("api/asset-catalog")
public class AssetLookupResource extends Resource {

  private final AssetLookupService assetLookupService;

  @Inject
  public AssetLookupResource(AssetLookupService assetLookupService) {
    this.assetLookupService = assetLookupService;
  }

  @GET
  @Path("{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  @Timed
  public Response lookup(@Auth DuosUser duosUser, @PathParam("identifier") String identifier) {
    try {
      return Response.ok(assetLookupService.lookup(identifier, duosUser.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
