package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("api/metrics")
public class MetricsResource extends Resource {

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @SuppressWarnings("unused")
  @GET
  @Path("/dar-summaries/{datasetId}")
  @Produces("application/json")
  @PermitAll
  public Response getDarSummaryData(
      @Auth DuosUser user, @PathParam("datasetId") Integer datasetId) {
    try {
      List<DarMetricsSummary> summaries = metricsService.generateDarSummaries(datasetId);
      return Response.ok().entity(summaries).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/dar-summaries/study/{studyId}")
  @Produces("application/json")
  @PermitAll
  public Response getStudyDarSummaryData(
      @Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    try {
      return Response.ok(metricsService.generateStudyDarSummaries(studyId, user.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/research-outputs/study/{studyId}")
  @Produces("application/json")
  @PermitAll
  public Response getStudyResearchOutputs(
      @Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    try {
      StudyResearchOutputs outputs =
          metricsService.generateStudyResearchOutputs(studyId, user.getUser());
      return Response.ok(outputs).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/study-recommendations/{studyId}/similar")
  @Produces("application/json")
  @PermitAll
  public Response getSimilarStudies(@Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    try {
      return Response.ok(metricsService.getSimilarStudies(studyId, user.getUser())).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/study-recommendations/{studyId}/frequently-requested-with")
  @Produces("application/json")
  @PermitAll
  public Response getFrequentlyRequestedWith(
      @Auth DuosUser user, @PathParam("studyId") Integer studyId) {
    try {
      return Response.ok(metricsService.getFrequentlyRequestedWith(studyId, user.getUser()))
          .build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
