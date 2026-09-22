package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyRecommendationDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.DatasetService.DatasetRead;
import org.broadinstitute.consent.http.service.DatasetService.DatasetReadBasis;
import org.jdbi.v3.core.Jdbi;

public class MetricsService {

  private final DatasetDAO dataSetDAO;
  private final DataAccessRequestDAO darDAO;
  private final StudyRecommendationDAO recommendationDAO;
  private final DatasetService datasetService;

  @Inject
  public MetricsService(Jdbi jdbi, DatasetService datasetService) {
    this.dataSetDAO = jdbi.onDemand(DatasetDAO.class);
    this.darDAO = jdbi.onDemand(DataAccessRequestDAO.class);
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
   * Enforces the same read access StudyResource applies to the study itself: a study that is not
   * publicly visible is readable only by its creator, custodians, and admins. The shared gate reads
   * only the study's own details, which is all the rule needs.
   */
  private void requireStudy(Integer studyId, User user) {
    datasetService.requireReadableStudy(studyId, user);
  }
}
