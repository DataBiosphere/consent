package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.Vote;

public class MetricsService {

  private final MetricsDAO metricsDAO;

  @Inject
  public MetricsService(MetricsDAO metricsDAO) {
    this.metricsDAO = metricsDAO;
  }

  public List<DarDecisionMetrics> generateDarDecisionMetrics() {
    List<DataAccessRequest> dars = metricsDAO.findAllDars();
    List<String> referenceIds =
        dars.stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());
    List<Election> elections = metricsDAO.findLastElectionsByReferenceIds(referenceIds);
    List<Match> matches = metricsDAO.findMatchesForReferenceIds(referenceIds);
    List<Integer> electionIds =
        elections.stream().map(Election::getElectionId).collect(Collectors.toList());
    List<Vote> votes = metricsDAO.findVotesByElectionIds(electionIds);
    List<DarDecisionMetrics> metricsList = new ArrayList<>();
    // TODO: Populate metricsList
    return metricsList;
  }

}
