package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DataAccessRequestDAO darDAO;
  private final DatasetService datasetService;

  @Inject
  public MetricsService(Jdbi jdbi, DatasetService datasetService) {
    this.dataSetDAO = jdbi.onDemand(DatasetDAO.class);
    this.darDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.datasetService = datasetService;
  }

  /**
   * The granted requests recorded against one dataset.
   *
   * <p>Gated on being able to read the dataset. The summaries carry the project titles and research
   * use statements of approved DARs, and this route used to check only that the dataset existed -
   * so any authenticated caller could walk dataset ids and read them. findDatasetByIdForRead
   * applies the existence-then-visibility rule the other dataset routes use, and still allows a
   * dataset that belongs to no study, which has no study visibility to test. It costs assembling
   * the dataset where an id lookup used to do; that is the price of the check.
   */
  public List<DarMetricsSummary> generateDarSummaries(Integer datasetId, User user) {
    datasetService.findDatasetByIdForRead(user, datasetId);
    return darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(datasetId);
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

  /**
   * Enforces the same read access StudyResource applies to the study itself: a study that is not
   * publicly visible is readable only by its creator, custodians, and admins. The shared gate reads
   * only the study's own details, which is all the rule needs.
   */
  private void requireStudy(Integer studyId, User user) {
    datasetService.requireReadableStudy(studyId, user);
  }
}
