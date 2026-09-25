package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DecisionReport;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.service.MetricsService;

@Path("api/metrics")
public class MetricsResource extends Resource {

  private static final int MAX_LIMIT = 1000;

  private final MetricsService metricsService;

  @Inject
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GET
  @Path("/dar-summaries/{datasetId}")
  @Produces("application/json")
  @PermitAll
  public Response getDarSummaryData(
      @Auth DuosUser user, @PathParam("datasetId") Integer datasetId) {
    try {
      List<DarMetricsSummary> summaries =
          metricsService.generateDarSummaries(datasetId, user.getUser());
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

  @GET
  @Path("/dar-decisions")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getDarDecisions(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    return decisionReport(from, to, bucket, limit, offset, metricsService::getDarDecisions);
  }

  @GET
  @Path("/dar-dataset-decisions")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getDarDatasetDecisions(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    return decisionReport(from, to, bucket, limit, offset, metricsService::getDarDatasetDecisions);
  }

  private interface DecisionReportQuery<T> {
    DecisionReport<T> run(
        LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset);
  }

  private <T> Response decisionReport(
      String from,
      String to,
      String bucket,
      Integer limit,
      Integer offset,
      DecisionReportQuery<T> query) {
    try {
      LocalDate start = parseDate("from", from);
      LocalDate end = parseRangeEnd(start, to);
      validatePage(limit, offset);
      return Response.ok(query.run(start, end, parseBucket(bucket), limit, offset)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private static LocalDate parseDate(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new BadRequestException(name + " is required, as yyyy-MM-dd");
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new BadRequestException(name + " must be a date in yyyy-MM-dd format");
    }
  }

  private static LocalDate parseRangeEnd(LocalDate start, String to) {
    LocalDate end = parseDate("to", to);
    if (end.isBefore(start)) {
      throw new BadRequestException("to must not be before from");
    }
    return end;
  }

  private static MetricsBucket parseBucket(String bucket) {
    try {
      return MetricsBucket.valueOf(bucket.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("bucket must be one of day, week, month or quarter");
    }
  }

  private static void validatePage(Integer limit, Integer offset) {
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
    }
    if (offset < 0) {
      throw new BadRequestException("offset must not be negative");
    }
  }
}
