package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
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
    List<Integer> datasetIds =
        dars.stream()
            .map(DataAccessRequest::getData)
            .map(DataAccessRequestData::getDatasetId)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    List<DataSet> datasets = metricsDAO.findDatasetsByIdList(datasetIds);
    List<Election> elections = metricsDAO.findLastElectionsByReferenceIds(referenceIds);
    List<Match> matches = metricsDAO.findMatchesForReferenceIds(referenceIds);
    List<Integer> electionIds =
        elections.stream().map(Election::getElectionId).collect(Collectors.toList());
    List<Vote> votes = metricsDAO.findVotesByElectionIds(electionIds);
    List<Dac> dacs = metricsDAO.findAllDacsForElectionIds(electionIds);
    return dars.stream()
        .map(
            dataAccessRequest -> {
              Integer datasetId =
                  dataAccessRequest.getData().getDatasetId().stream().findFirst().orElse(0);

              DataSet dataset =
                  datasets.stream()
                      .filter(d -> d.getDataSetId().equals(datasetId))
                      .findFirst()
                      .orElse(null);

              Optional<Election> accessElection =
                  elections.stream()
                      .filter(
                          e ->
                              e.getReferenceId()
                                  .equalsIgnoreCase(dataAccessRequest.getReferenceId()))
                      .filter(
                          e ->
                              e.getElectionType()
                                  .equalsIgnoreCase(ElectionType.DATA_ACCESS.getValue()))
                      .findFirst();

              Optional<Election> rpElection =
                  elections.stream()
                      .filter(
                          e ->
                              e.getReferenceId()
                                  .equalsIgnoreCase(dataAccessRequest.getReferenceId()))
                      .filter(e -> e.getElectionType().equalsIgnoreCase(ElectionType.RP.getValue()))
                      .findFirst();

              Optional<Match> match =
                  matches.stream()
                      .filter(
                          m -> m.getPurpose().equalsIgnoreCase(dataAccessRequest.getReferenceId()))
                      .findFirst();

              List<Vote> accessVotes =
                  accessElection
                      .map(
                          election ->
                              votes.stream()
                                  .filter(v -> v.getElectionId().equals(election.getElectionId()))
                                  .collect(Collectors.toList()))
                      .orElse(Collections.emptyList());

              List<Vote> rpVotes =
                  rpElection
                      .map(
                          election ->
                              votes.stream()
                                  .filter(v -> v.getElectionId().equals(election.getElectionId()))
                                  .collect(Collectors.toList()))
                      .orElse(Collections.emptyList());

              Optional<Dac> dac =
                  accessElection
                      .map(
                          election ->
                              dacs.stream()
                                  .filter(
                                      d -> d.getElectionIds().contains(election.getElectionId()))
                                  .findFirst())
                      .flatMap(Function.identity());

              return new DarDecisionMetrics(
                  dataAccessRequest,
                  dac.orElse(null),
                  dataset,
                  accessElection.orElse(null),
                  rpElection.orElse(null),
                  match.orElse(null),
                  accessVotes,
                  rpVotes);
            })
        .collect(Collectors.toList());
  }
}
