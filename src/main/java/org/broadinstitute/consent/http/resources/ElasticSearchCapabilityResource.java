package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchCapabilityReport;
import org.broadinstitute.consent.http.service.ElasticSearchCapabilityService;

/**
 * Admin-only view onto the Elasticsearch security feature inventory of whichever cluster this
 * deployment is configured against.
 *
 * <p>Each environment runs its own Consent deployment against its own cluster, so calling this in
 * dev, staging, and production produces the per-environment record without anyone needing direct
 * network access to the clusters or a copy of their credentials — the application already holds
 * them.
 *
 * <p>The probes behind this endpoint are read-only by default. Pass {@code writeProbes=true} to
 * additionally create and tear down a short-lived API key and role, which is the only way to
 * observe DLS, FLS, and API-key support rather than infer it from the license tier — see {@link
 * ElasticSearchCapabilityService} for what each mode establishes.
 */
@Path("api/elasticSearch")
public class ElasticSearchCapabilityResource extends Resource {

  private final ElasticSearchCapabilityService capabilityService;

  @Inject
  public ElasticSearchCapabilityResource(ElasticSearchCapabilityService capabilityService) {
    this.capabilityService = capabilityService;
  }

  /**
   * Report the cluster's security capabilities: version, edition, X-Pack Security, DLS, FLS, API
   * keys, and run_as.
   *
   * @param duosUser the authenticated admin
   * @param runAsUser optional username to attempt the run_as probe against; defaults to the
   *     credential's own principal, which still establishes whether the feature is licensed
   * @param writeProbes when true, create and tear down a short-lived API key and role so DLS, FLS,
   *     and API-key support are observed rather than inferred. Off by default: the caller has to
   *     ask for writes against the cluster their environment depends on.
   * @return the capability report
   */
  @GET
  @Path("/capabilities")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response getCapabilities(
      @Auth DuosUser duosUser,
      @QueryParam("runAsUser") String runAsUser,
      @QueryParam("writeProbes") @DefaultValue("false") boolean writeProbes) {
    try {
      // Worth an audit trail either way: this reports on the cluster's security posture, and with
      // write probes it also creates and removes credentials on that cluster.
      Integer userId = duosUser.getUser().getUserId();
      if (writeProbes) {
        logWarn(
            "Elasticsearch capability report with WRITE PROBES requested by user %d — a short-lived "
                    .formatted(userId)
                + "API key and role will be created and torn down");
      } else {
        logInfo("Elasticsearch capability report requested by user %d".formatted(userId));
      }
      ElasticSearchCapabilityReport report =
          capabilityService.getCapabilityReport(runAsUser, writeProbes);
      return Response.ok(report).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
