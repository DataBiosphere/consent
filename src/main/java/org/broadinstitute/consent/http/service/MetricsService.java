package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;

import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacDecisionMetrics;
import org.broadinstitute.consent.http.models.DarDecisionMetrics;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.DecisionMetrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

public class MetricsService {

  private final DacService dacService;
  private final DatasetDAO dataSetDAO;
  private final MetricsDAO metricsDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final ElectionDAO electionDAO;

  @Inject
  public MetricsService(DacService dacService, DatasetDAO dataSetDAO, MetricsDAO metricsDAO, DataAccessRequestDAO dataAccessRequestDAO, ElectionDAO electionDAO) {
    this.dacService = dacService;
    this.dataSetDAO = dataSetDAO;
    this.metricsDAO = metricsDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.electionDAO = electionDAO;
  }

  public String getHeaderRow(Type type) {
    switch (type) {
      case DAR:
        return DarDecisionMetrics.headerRow;
      case DAC:
        return DacDecisionMetrics.headerRow;
      default:
        return "\n";
    }
  }

  public List<? extends DecisionMetrics> generateDecisionMetrics(Type type) {
    List<DataAccessRequest> dars = metricsDAO.findAllDars();
    List<String> referenceIds =
      dars.stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());
    List<Integer> datasetIds =
      dars.stream()
        .map(DataAccessRequest::getData)
        .map(DataAccessRequestData::getDatasetIds)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    List<DataSet> datasets = metricsDAO.findDatasetsByIds(datasetIds);
    List<Election> elections = metricsDAO.findLastElectionsByReferenceIds(referenceIds);
    List<Match> matches = metricsDAO.findMatchesForPurposeIds(referenceIds);
    List<Integer> electionIds =
      elections.stream().map(Election::getElectionId).collect(Collectors.toList());
    List<Dac> dacs = metricsDAO.findAllDacsForElectionIds(electionIds);
    List<DarDecisionMetrics> darMetrics = dars.stream()
      .map(
        dataAccessRequest -> {
          Integer datasetId =
            dataAccessRequest.getData().getDatasetIds().stream().findFirst().orElse(0);

          DataSet dataset =
            datasets.stream()
              .filter(d -> d.getDataSetId().equals(datasetId))
              .findFirst()
              .orElse(null);

          List<Election> associatedElections = elections.stream()
            .filter(e -> 
              e.getReferenceId()
                .equalsIgnoreCase(dataAccessRequest.getReferenceId())
            ).collect(Collectors.toList());

          List<Election> filteredAccessElections = associatedElections
            .stream()
            .filter(e -> e.getElectionType()
              .equalsIgnoreCase(ElectionType.DATA_ACCESS.getValue())
            ).collect(Collectors.toList());
              

          List<Election> filteredRpElections = associatedElections
            .stream()
            .filter(e -> e.getElectionType()
              .equalsIgnoreCase(ElectionType.RP.getValue())
            ).collect(Collectors.toList());

          Optional<Election> accessElection = searchFilteredElectionList(filteredAccessElections);
          Optional<Election> rpElection = searchFilteredElectionList(filteredRpElections);
          
          Optional<Match> match =
            matches.stream()
              .filter(
                m -> m.getPurpose().equalsIgnoreCase(dataAccessRequest.getReferenceId()))
              .findFirst();

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
            match.orElse(null));
        })
      .collect(Collectors.toList());
    if (type == Type.DAR) {
      return darMetrics;
    } else {
      List<Dac> allDacs = dacService.findAllDacsWithMembers();
      Set<DatasetDTO> datasetsDacs = dataSetDAO.findDatasetsWithDacs();
      return allDacs.stream()
        .map(
          dac -> {
            List<DarDecisionMetrics> dacFilteredMetrics =
              darMetrics.stream()
                .filter(m -> Objects.nonNull(m.getDacName()))
                .filter(m -> m.getDacName().equalsIgnoreCase(dac.getName()))
                .collect(Collectors.toList());
            List<DatasetDTO> dacFilteredDatasets =
              datasetsDacs.stream()
                .filter(ds -> ds.getDacId().equals(dac.getDacId()))
                .collect(Collectors.toList());
            return new DacDecisionMetrics(dac, dacFilteredDatasets, dacFilteredMetrics);
          })
        .collect(Collectors.toList());
    }
  }

  private static Optional<Election> searchFilteredElectionList(List<Election> electionList) {

    //Search for instance where election.finalVote is true
    //Order does not matter, so parallel steam makes sense
    Optional<Election> election = electionList
      .stream()
      .filter(e -> Objects.nonNull(e.getFinalVote()) && e.getFinalVote())
      .findAny();
    
    //if no vote was found, return the earliest vote
    if(Objects.isNull(election)) {
      election = electionList.stream().findFirst();
    }
    return election;
  }

  public DatasetMetrics generateDatasetMetrics(Integer datasetId) {

    DatasetMetrics metrics = new DatasetMetrics();

    //get datasetDTO with properties and data use restrictions
    Set<DatasetDTO> datasets = dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(datasetId);
    if (datasets == null || datasets.isEmpty()) {
       throw new NotFoundException("Dataset with specified ID does not exist.");
    }

    //find dars with the given datasetId in their list of datasetIds, String so it can be converted to jsonb in query
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequestsForDatasetMetrics(Integer.toString(datasetId));

    //find all associated access elections so we know how many (and which) dars are approved/denied
    List<String> referenceIds = dars.stream().map(dar -> (dar.referenceId)).collect(Collectors.toList());
    if (!referenceIds.isEmpty()) {
      List<Election> elections = electionDAO.findLastElectionsByReferenceIdsAndType(referenceIds, "DataAccess");
      metrics.setElections(elections);
    } else {
      metrics.setElections(Collections.emptyList());
    }
    metrics.setDataset(datasets.iterator().next());
    metrics.setDars(dars);
    return metrics;

  }
}
