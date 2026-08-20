package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
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

  /** Classification counts only; the response carries no dataset identifier or Other free text. */
  @GET
  @Path("/report")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN})
  public Response getReport(@Auth DuosUser duosUser) {
    try {
      return Response.ok().entity(service.report()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
