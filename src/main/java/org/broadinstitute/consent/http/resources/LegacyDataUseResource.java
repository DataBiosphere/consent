package org.broadinstitute.consent.http.resources;

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
import org.broadinstitute.consent.http.service.LegacyDataUseService;

/**
 * Read-only reporting on persisted Data Use shapes, used to reconcile the legacy-record population
 * before and after it is normalized.
 */
@Path("api/datause/legacy")
public class LegacyDataUseResource extends Resource {

  private final LegacyDataUseService service;

  @Inject
  public LegacyDataUseResource(LegacyDataUseService service) {
    this.service = service;
  }

  /** Identifies the noncanonical datasets so one can be corrected; no Other free text. */
  @GET
  @Path("/noncanonical")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN})
  public Response getNoncanonicalDatasets(@Auth DuosUser duosUser) {
    try {
      return Response.ok().entity(service.findNoncanonicalViews()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /** Rewrites only match rows and rationales; no stored Data Use, no elections, no votes. */
  @POST
  @Path("/recomputeMatches")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN})
  public Response recomputeAbstainingMatches(@Auth DuosUser duosUser) {
    try {
      return Response.ok().entity(service.recomputeAbstainingMatches(duosUser.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
