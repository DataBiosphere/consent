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
import org.broadinstitute.consent.http.service.MatchMigrationService;

/**
 * Temporary admin surface for the one-off migration that replaces {@code match_entity.consent} with
 * a real {@code dataset_id}. Retired once the constraints are applied.
 */
@Path("api/match/migration/dataset-id")
public class MatchMigrationResource extends Resource {

  private final MatchMigrationService service;

  @Inject
  public MatchMigrationResource(MatchMigrationService service) {
    this.service = service;
  }

  /** Read-only. Recounts the affected population, so a run is never scoped from stale figures. */
  @GET
  @Path("/population")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN})
  public Response getPopulation(@Auth DuosUser duosUser) {
    try {
      return Response.ok().entity(service.findPopulation()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /** Read-only. Reconciles a run that has already happened, for a later confirmation pass. */
  @GET
  @Path("/reconciliation")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({Resource.ADMIN})
  public Response getReconciliation(@Auth DuosUser duosUser) {
    try {
      return Response.ok().entity(service.reconcile()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
