package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
 * <p>The probes behind this endpoint are Elasticsearch X-Pack probes — see {@link
 * ElasticSearchCapabilityService}.
 *
 * <p>Two modes, split by HTTP method rather than by a query parameter. {@code GET} is read-only:
 * nothing is created, modified, or deleted on the cluster. {@code POST} additionally creates and
 * tears down a short-lived API key and role, which is the only way to observe DLS, FLS, and API-key
 * support rather than infer it from the license tier — see {@link ElasticSearchCapabilityService}
 * for what each mode establishes.
 *
 * <p>The split is the point: minting credentials is a side effect, and a URL that mints them on
 * {@code GET} is one a link prefetcher, a monitoring crawler, or a bookmark can fire without anyone
 * deciding to. Behind {@code POST} it takes a deliberate request.
 *
 * <p>The same reasoning, one step further, is why <b>activating the trial license is its own
 * endpoint</b> rather than something a capability probe does when it finds DLS/FLS unlicensed. The
 * capability report never changes the cluster's license tier; {@code POST /license/trial} does, and
 * only when asked, by an admin, with an explicit acknowledgement. A trial can be started once per
 * major version per cluster and cannot be reverted, so it is exactly the kind of thing that must
 * not be a side effect of asking a question.
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
   * keys, and run_as. Read-only — nothing is created, modified, or deleted on the cluster, so the
   * DLS, FLS, and API-key verdicts are inferred from the license tier rather than observed.
   *
   * @param duosUser the authenticated admin
   * @param runAsUser optional username to attempt the run_as probe against; defaults to the
   *     credential's own principal, which still establishes whether the feature is licensed
   * @return the capability report
   */
  @GET
  @Path("/capabilities")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response getCapabilities(
      @Auth DuosUser duosUser, @QueryParam("runAsUser") String runAsUser) {
    return report(duosUser, runAsUser, false);
  }

  /**
   * The same report, with the write probes run: a short-lived API key and a role carrying DLS and
   * FLS filters are created, used, and torn down, so those verdicts are observed rather than
   * inferred.
   *
   * <p>A {@code POST} because it has side effects on the cluster — credentials are created and
   * removed — even though the response body is a report. Reaching it therefore takes a deliberate
   * call rather than anything that merely follows a link.
   *
   * @param duosUser the authenticated admin
   * @param runAsUser optional username to attempt the run_as probe against; defaults to the
   *     credential's own principal, which still establishes whether the feature is licensed
   * @return the capability report
   */
  @POST
  @Path("/capabilities")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response runCapabilityProbes(
      @Auth DuosUser duosUser, @QueryParam("runAsUser") String runAsUser) {
    return report(duosUser, runAsUser, true);
  }

  /**
   * Report the cluster's license tier and whether its 30-day trial is still available. Read-only.
   *
   * <p>The call to make before {@link #activateTrialLicense}: the trial can be started once per
   * major version per cluster, and this says whether this cluster still has one to spend and
   * whether it needs to.
   *
   * @param duosUser the authenticated admin
   * @return the license state
   */
  @GET
  @Path("/license")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response getLicense(@Auth DuosUser duosUser) {
    try {
      logInfo(
          "Elasticsearch license status requested by user %d"
              .formatted(duosUser.getUser().getUserId()));
      return Response.ok(capabilityService.getLicenseStatus()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Start the cluster's 30-day trial license — the only tier a self-managed cluster can reach by
   * API call that includes DLS and FLS.
   *
   * <p>An independent step, not a stage of any capability probe. The report says whether the
   * cluster is entitled to DLS/FLS; changing that entitlement is a separate decision, made here, by
   * an admin, and only for the environment whose deployment is being called.
   *
   * <p>{@code acknowledge} must be passed explicitly as {@code true}. The activation cannot be
   * undone and can happen only once per major version per cluster, so an empty {@code POST} that
   * would silently spend a production cluster's trial is refused with {@code 400} rather than
   * honored. Elasticsearch's own API takes the same precaution, and this parameter is the
   * human-facing half of it.
   *
   * <p>Repeating the call is safe: a cluster already entitled to DLS/FLS is reported as such and
   * left alone, and one whose trial is spent is reported as such rather than being asked again.
   *
   * @param duosUser the authenticated admin
   * @param acknowledge must be {@code true}; acknowledges that the trial can be started once per
   *     major version per cluster and is irreversible
   * @return what the call did, with the license state from both sides of it
   */
  @POST
  @Path("/license/trial")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({ADMIN})
  public Response activateTrialLicense(
      @Auth DuosUser duosUser, @QueryParam("acknowledge") Boolean acknowledge) {
    try {
      Integer userId = duosUser.getUser().getUserId();
      if (!Boolean.TRUE.equals(acknowledge)) {
        // Refused before the service is reached: an unacknowledged call must not touch the cluster.
        logWarn(
            "Elasticsearch trial license activation refused for user %d: acknowledge was not true"
                .formatted(userId));
        throw new BadRequestException(
            "acknowledge=true is required. Starting the trial license changes this cluster's "
                + "license tier, can be done only once per major version per cluster, and cannot be reverted. GET "
                + "/api/elasticSearch/license reports whether this cluster needs it and still has "
                + "one available.");
      }
      logWarn(
          "Elasticsearch TRIAL LICENSE ACTIVATION requested by user %d — this changes the "
                  .formatted(userId)
              + "cluster's license tier and cannot be reverted");
      return Response.ok(capabilityService.activateTrialLicense()).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  /**
   * Audits the call and runs the report. Inside the {@code try} with the report itself so that
   * anything thrown on the way — including reading the caller's id — still comes back through the
   * resource's own error mapping rather than as an unmapped server error.
   */
  private Response report(DuosUser duosUser, String runAsUser, boolean writeProbes) {
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
