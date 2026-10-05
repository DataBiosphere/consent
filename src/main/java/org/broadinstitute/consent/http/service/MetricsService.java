package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.StudyRecommendationDAO;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.models.DarDatasetDecision;
import org.broadinstitute.consent.http.models.DarDatasetTurnaround;
import org.broadinstitute.consent.http.models.DarDecision;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DarTurnaround;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DecisionReport;
import org.broadinstitute.consent.http.models.ExpirationReport;
import org.broadinstitute.consent.http.models.RenewalReport;
import org.broadinstitute.consent.http.models.SoApprovalReport;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.models.TurnaroundReport;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.VolumeReport;
import org.broadinstitute.consent.http.service.DatasetService.DatasetRead;
import org.broadinstitute.consent.http.service.DatasetService.DatasetReadBasis;
import org.jdbi.v3.core.Jdbi;

public class MetricsService {

  // null reads every DAC's datasets; see DarMetricsDAO.IN_DAC_SCOPE
  private static final List<Integer> ALL_DACS = null;

  private final DataAccessRequestDAO darDAO;
  private final DarMetricsDAO darMetricsDAO;
  private final StudyRecommendationDAO recommendationDAO;
  private final DatasetService datasetService;

  @Inject
  public MetricsService(Jdbi jdbi, DatasetService datasetService) {
    this.darDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.darMetricsDAO = jdbi.onDemand(DarMetricsDAO.class);
    this.recommendationDAO = jdbi.onDemand(StudyRecommendationDAO.class);
    this.datasetService = datasetService;
  }

  /**
   * The granted requests recorded against one dataset.
   *
   * <p>Gated on being able to read the dataset: this route once checked only that the dataset
   * existed, so any authenticated caller could walk ids and read the project titles and research
   * use statements. findDatasetByIdForRead applies the existence-then-visibility rule the other
   * dataset routes use, but admits a dataset with no study to everyone, having no study visibility
   * to test - so the requester's institution is withheld on those.
   */
  public List<DarMetricsSummary> generateDarSummaries(Integer datasetId, User user) {
    DatasetRead read = datasetService.findDatasetByIdForReadWithBasis(user, datasetId);
    List<DarMetricsSummary> summaries =
        darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(datasetId);
    // Withheld only where no visibility decision covers the dataset at all. A dataset with no
    // study has none to test, so it is returned to every authenticated caller and the requester's
    // affiliation would be enumerable by walking ids. Everything else is covered by a decision
    // someone made: a published study was deliberately opened to any authenticated caller, and a
    // hidden one is reachable only by its creator, its custodians or an admin. STUDY_READABLE
    // spans both, so it does not mean this particular caller was vetted - only that the study's
    // own visibility already answered who may see what it carries.
    if (read.basis() != DatasetReadBasis.NO_STUDY) {
      return summaries;
    }
    return summaries.stream().map(DarMetricsSummary::withoutRequesterIdentity).toList();
  }

  public List<DarMetricsSummary> generateStudyDarSummaries(Integer studyId, User user) {
    requireStudy(studyId, user);
    return darDAO.findSummaryMetricApprovedDARsByStudyIdIncludesExpired(studyId);
  }

  public StudyResearchOutputs generateStudyResearchOutputs(Integer studyId, User user) {
    requireStudy(studyId, user);
    List<DataAccessRequest> reports = darDAO.findProgressReportsByStudyId(studyId);
    return new StudyResearchOutputs(
        collectOutputs(reports, DataAccessRequestData::getPresentations),
        collectOutputs(reports, DataAccessRequestData::getPublications),
        collectOutputs(reports, DataAccessRequestData::getIntellectualProperties));
  }

  private static <T> List<T> collectOutputs(
      List<DataAccessRequest> reports, Function<DataAccessRequestData, List<T>> getOutputs) {
    return reports.stream()
        .map(DataAccessRequest::getData)
        .filter(Objects::nonNull)
        .map(getOutputs)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .toList();
  }

  public List<StudyRecommendation> getSimilarStudies(Integer studyId, User user) {
    requireStudy(studyId, user);
    return recommendationDAO.findSimilar(studyId);
  }

  public List<StudyRecommendation> getFrequentlyRequestedWith(Integer studyId, User user) {
    requireStudy(studyId, user);
    return recommendationDAO.findFrequentlyRequestedWith(studyId);
  }

  /**
   * DAC decisions per DAR-dataset pair on original DARs submitted from {@code from} to {@code to}.
   */
  public DecisionReport<DarDatasetDecision> getDarDatasetDecisions(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return DecisionReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countPairDecisions(start, end, ALL_DACS, bucket.truncUnit()),
        darMetricsDAO.findPairDecisions(start, end, ALL_DACS, limit, offset));
  }

  /** DAC decisions rolled up per original DAR submitted from {@code from} to {@code to}. */
  public DecisionReport<DarDecision> getDarDecisions(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return DecisionReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countDarDecisions(start, end, ALL_DACS, bucket.truncUnit()),
        darMetricsDAO.findDarDecisions(start, end, ALL_DACS, limit, offset));
  }

  /**
   * Time from submission to DAC decision per decided DAR-dataset pair on original DARs submitted
   * from {@code from} to {@code to}.
   */
  public TurnaroundReport<DarDatasetTurnaround> getDarDatasetDecisionTurnaround(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return TurnaroundReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countPairTurnaround(start, end, ALL_DACS, bucket.truncUnit()),
        darMetricsDAO.findPairTurnaround(start, end, ALL_DACS, limit, offset));
  }

  /**
   * Time from submission to the last DAC decision per decided original DAR submitted from {@code
   * from} to {@code to}.
   */
  public TurnaroundReport<DarTurnaround> getDarDecisionTurnaround(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return TurnaroundReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countDarTurnaround(start, end, ALL_DACS, bucket.truncUnit()),
        darMetricsDAO.findDarTurnaround(start, end, ALL_DACS, limit, offset));
  }

  /**
   * Submission volume and composition of original DARs submitted from {@code from} to {@code to}.
   */
  public VolumeReport getDarVolume(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return VolumeReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countDarVolume(start, end, ALL_DACS, bucket.truncUnit()),
        darMetricsDAO.countDarsByInstitution(start, end, ALL_DACS),
        darMetricsDAO.countDarsByResearcher(start, end, ALL_DACS),
        darMetricsDAO.findDarVolume(start, end, ALL_DACS, limit, offset));
  }

  /**
   * Where original DARs, progress reports and closeouts submitted from {@code from} to {@code to}
   * stand with their signing official, and how long approval took.
   */
  public SoApprovalReport getDarSoApprovals(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return SoApprovalReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countSoApprovals(start, end, bucket.truncUnit()),
        darMetricsDAO.findSoApprovals(start, end, limit, offset));
  }

  /** DAR collections whose access to their datasets ended from {@code from} to {@code to}. */
  public ExpirationReport getDarExpirations(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    Instant asOf = Instant.now();
    return ExpirationReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countExpirations(start, end, ALL_DACS, asOf, bucket.truncUnit()),
        darMetricsDAO.findExpirations(start, end, ALL_DACS, asOf, limit, offset));
  }

  /** Datasets renewed by progress reports submitted from {@code from} to {@code to}. */
  public RenewalReport getDarRenewals(
      LocalDate from, LocalDate to, MetricsBucket bucket, int limit, int offset) {
    Instant start = startOfDay(from);
    Instant end = startOfDay(to.plusDays(1));
    return RenewalReport.of(
        from,
        to,
        bucket,
        darMetricsDAO.countRenewals(start, end, ALL_DACS, bucket.truncUnit()),
        darMetricsDAO.findRenewals(start, end, ALL_DACS, limit, offset));
  }

  // submission_date is stored without a zone in the server's zone, which date_trunc buckets in too
  private static Instant startOfDay(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  /**
   * Enforces the same read access StudyResource applies to the study itself: a study that is not
   * publicly visible is readable only by its creator, custodians, and admins. The shared gate reads
   * only the study's own details, which is all the rule needs.
   */
  private void requireStudy(Integer studyId, User user) {
    datasetService.requireReadableStudy(studyId, user);
  }
}
