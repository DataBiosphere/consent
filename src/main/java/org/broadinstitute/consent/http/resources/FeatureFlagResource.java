package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.FeatureFlagService;

@Path("api/feature")
public class FeatureFlagResource extends Resource {

  private final FeatureFlagService featureFlagService;

  @Inject
  public FeatureFlagResource(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  /**
   * Create or update a feature flag (admin only)
   *
   * @param info URI information
   * @param duosUser The authenticated user
   * @param id The feature flag id
   * @param body Request body containing the value
   * @return The created or updated feature flag
   */
  @POST
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response createOrUpdateFeatureFlag(
      @Context UriInfo info,
      @Auth DuosUser duosUser,
      @PathParam("id") String id,
      Map<String, String> body) {
    try {
      String value = body.get("value");
      if (value == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Missing 'value' in request body"))
            .build();
      }

      User user = duosUser.getUser();
      boolean existed = featureFlagService.exists(id);
      FeatureFlag flag = featureFlagService.createOrUpdateFeatureFlag(id, value, user.getUserId());

      if (existed) {
        return Response.ok(flag).build();
      } else {
        URI uri = info.getBaseUriBuilder().path("feature").path(id).build();
        return Response.created(uri).entity(flag).build();
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Delete a feature flag (admin only)
   *
   * @param duosUser The authenticated user
   * @param id The feature flag id
   * @return No content response
   */
  @DELETE
  @Path("/{id}")
  @RolesAllowed({ADMIN})
  public Response deleteFeatureFlag(@Auth DuosUser duosUser, @PathParam("id") String id) {
    try {
      User user = duosUser.getUser();
      featureFlagService.deleteFeatureFlag(id, user.getUserId());
      return Response.noContent().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
