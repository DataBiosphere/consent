package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.broadinstitute.consent.http.service.FeatureFlagService;

@Path("api/feature")
public class FeatureFlagResource extends Resource {

  private final FeatureFlagService featureFlagService;

  @Inject
  public FeatureFlagResource(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  /**
   * Get all feature flags
   *
   * @return List of all feature flags
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response getAllFeatureFlags() {
    try {
      List<FeatureFlag> flags = featureFlagService.getAllFeatureFlags();
      return Response.ok(flags).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Get a specific feature flag by id
   *
   * @param id The feature flag id
   * @return The feature flag
   */
  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response getFeatureFlagById(@PathParam("id") String id) {
    try {
      FeatureFlag flag = featureFlagService.getFeatureFlagById(id);
      return Response.ok(flag).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Create or update a feature flag (admin only)
   *
   * @param info URI information
   * @param authUser The authenticated user
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
      @Auth AuthUser authUser,
      @PathParam("id") String id,
      Map<String, String> body) {
    try {
      String value = body.get("value");
      if (value == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Missing 'value' in request body"))
            .build();
      }

      boolean existed = featureFlagService.exists(id);
      FeatureFlag flag = featureFlagService.createOrUpdateFeatureFlag(id, value);

      if (existed) {
        return Response.ok(flag).build();
      } else {
        URI uri = info.getAbsolutePathBuilder().path(id).build();
        return Response.created(uri).entity(flag).build();
      }
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Delete a feature flag (admin only)
   *
   * @param authUser The authenticated user
   * @param id The feature flag id
   * @return No content response
   */
  @DELETE
  @Path("/{id}")
  @RolesAllowed({ADMIN})
  public Response deleteFeatureFlag(@Auth AuthUser authUser, @PathParam("id") String id) {
    try {
      featureFlagService.deleteFeatureFlag(id);
      return Response.noContent().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
