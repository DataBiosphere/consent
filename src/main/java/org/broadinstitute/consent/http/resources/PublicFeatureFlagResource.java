package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.broadinstitute.consent.http.service.FeatureFlagService;

@Path("feature")
public class PublicFeatureFlagResource extends Resource {

  private final FeatureFlagService featureFlagService;

  @Inject
  public PublicFeatureFlagResource(FeatureFlagService featureFlagService) {
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
}
