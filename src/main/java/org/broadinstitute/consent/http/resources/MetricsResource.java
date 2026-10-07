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
import java.util.function.Supplier;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
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
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarDecisions(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds), from, to, bucket, limit, offset, metricsService::getDarDecisions);
  }

  @GET
  @Path("/dar-dataset-decisions")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarDatasetDecisions(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds),
        from,
        to,
        bucket,
        limit,
        offset,
        metricsService::getDarDatasetDecisions);
  }

  @GET
  @Path("/dar-decision-turnaround")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarDecisionTurnaround(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds),
        from,
        to,
        bucket,
        limit,
        offset,
        metricsService::getDarDecisionTurnaround);
  }

  @GET
  @Path("/dar-dataset-decision-turnaround")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarDatasetDecisionTurnaround(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds),
        from,
        to,
        bucket,
        limit,
        offset,
        metricsService::getDarDatasetDecisionTurnaround);
  }

  @GET
  @Path("/dar-volume")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarVolume(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds), from, to, bucket, limit, offset, metricsService::getDarVolume);
  }

  @GET
  @Path("/dar-so-approvals")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getDarSoApprovals(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset) {
    return rangeReport(
        List::of,
        from,
        to,
        bucket,
        limit,
        offset,
        (start, end, dacIds, b, l, o) -> metricsService.getDarSoApprovals(start, end, b, l, o));
  }

  @GET
  @Path("/dar-expirations")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarExpirations(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds), from, to, bucket, limit, offset, metricsService::getDarExpirations);
  }

  @GET
  @Path("/dar-renewals")
  @Produces("application/json")
  @RolesAllowed({ADMIN, CHAIRPERSON, MEMBER})
  public Response getDarRenewals(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket,
      @DefaultValue("100") @QueryParam("limit") Integer limit,
      @DefaultValue("0") @QueryParam("offset") Integer offset,
      @QueryParam("dacId") List<String> dacIds) {
    return rangeReport(
        dacScope(user, dacIds), from, to, bucket, limit, offset, metricsService::getDarRenewals);
  }

  @GET
  @Path("/users")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getUsers(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket) {
    return createdReport(from, to, bucket, metricsService::getUsers);
  }

  @GET
  @Path("/institutions")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getInstitutions(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket) {
    return createdReport(from, to, bucket, metricsService::getInstitutions);
  }

  @GET
  @Path("/datasets")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getDatasets(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket) {
    return createdReport(from, to, bucket, metricsService::getDatasets);
  }

  @GET
  @Path("/studies")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getStudies(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket) {
    return createdReport(from, to, bucket, metricsService::getStudies);
  }

  @GET
  @Path("/elections")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getElections(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("quarter") @QueryParam("bucket") String bucket) {
    return createdReport(from, to, bucket, metricsService::getElections);
  }

  @GET
  @Path("/dar-terms")
  @Produces("application/json")
  @RolesAllowed(ADMIN)
  public Response getDarTerms(
      @Auth DuosUser user,
      @QueryParam("from") String from,
      @QueryParam("to") String to,
      @DefaultValue("10") @QueryParam("limit") Integer limit) {
    try {
      LocalDate start = parseDate("from", from);
      LocalDate end = parseRangeEnd(start, to);
      validatePage(limit, 0);
      return Response.ok(metricsService.getDarTerms(start, end, limit)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private interface CreatedReportQuery<R> {
    R run(LocalDate from, LocalDate to, MetricsBucket bucket);
  }

  /** A range report with no rows to page, so no limit or offset. */
  private <R> Response createdReport(
      String from, String to, String bucket, CreatedReportQuery<R> query) {
    try {
      LocalDate start = parseDate("from", from);
      LocalDate end = parseRangeEnd(start, to);
      return Response.ok(query.run(start, end, parseBucket(bucket))).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private interface RangeReportQuery<R> {
    R run(
        LocalDate from,
        LocalDate to,
        List<Integer> dacIds,
        MetricsBucket bucket,
        int limit,
        int offset);
  }

  private Supplier<List<Integer>> dacScope(DuosUser user, List<String> dacIds) {
    return () -> metricsService.resolveDacScope(user.getUser(), parseDacIds(dacIds));
  }

  private <R> Response rangeReport(
      Supplier<List<Integer>> dacScope,
      String from,
      String to,
      String bucket,
      Integer limit,
      Integer offset,
      RangeReportQuery<R> query) {
    try {
      LocalDate start = parseDate("from", from);
      LocalDate end = parseRangeEnd(start, to);
      MetricsBucket unit = parseBucket(bucket);
      validatePage(limit, offset);
      return Response.ok(query.run(start, end, dacScope.get(), unit, limit, offset)).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  private static List<Integer> parseDacIds(List<String> dacIds) {
    try {
      return dacIds.stream().map(Integer::valueOf).distinct().toList();
    } catch (NumberFormatException e) {
      throw new BadRequestException("dacId must be an integer");
    }
  }

  private static LocalDate parseDate(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new BadRequestException(name + " is required, as yyyy-MM-dd");
    }
    LocalDate date;
    try {
      date = LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new BadRequestException(name + " must be a date in yyyy-MM-dd format");
    }
    // LocalDate.parse accepts signed years like +999999999, which overflow the range's end
    if (date.getYear() < 1900 || date.getYear() > 9999) {
      throw new BadRequestException(name + " must be a date between 1900 and 9999");
    }
    return date;
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
