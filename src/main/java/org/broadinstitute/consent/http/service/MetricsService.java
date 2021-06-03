package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import java.sql.Timestamp;

import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MetricsDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.models.UserProperty;
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
  private final UserPropertyDAO userPropertyDAO;

  @Inject
  public MetricsService(DacService dacService, DatasetDAO dataSetDAO, MetricsDAO metricsDAO, DataAccessRequestDAO dataAccessRequestDAO, ElectionDAO electionDAO, UserPropertyDAO userPropertyDAO) {
    this.dacService = dacService;
    this.dataSetDAO = dataSetDAO;
    this.metricsDAO = metricsDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.electionDAO = electionDAO;
    this.userPropertyDAO = userPropertyDAO;
  }

  public class DarMetricsSummary {
    @JsonProperty final Timestamp updateDate;
    @JsonProperty final String projectTitle;
    @JsonProperty final String darCode;
    @JsonProperty final String nonTechRus;
    @JsonProperty final String investigator;
    @JsonProperty final String referenceId;

    public DarMetricsSummary(DataAccessRequest dar) {
      if (dar != null && dar.data != null) {
        this.updateDate = dar.getUpdateDate();
        this.projectTitle = dar.data.getProjectTitle();
        this.darCode =  dar.data.getDarCode();
        this.nonTechRus =  dar.data.getNonTechRus();
        this.investigator = findPI(dar.userId);
        this.referenceId = dar.getReferenceId();
      } else {
        this.updateDate = null;
        this.projectTitle = null;
        this.darCode =  null;
        this.nonTechRus =  null;
        this.investigator = null;
        this.referenceId = null;
      }
    }
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

    //find dars with the given datasetId in their list of datasetIds, datasetId is a String so it can be converted to jsonb in query
    //convert all dars into smaller objects that only contain the information needed
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequestsForDatasetMetrics(Integer.toString(datasetId));
    List<DarMetricsSummary> darInfo = dars.stream().map(dar -> 
      new DarMetricsSummary(dar))
      .collect(Collectors.toList());

    //if there are associated dars, find associated access elections so we know how many and which dars are approved/denied
    List<String> referenceIds = dars.stream().map(dar -> (dar.referenceId)).collect(Collectors.toList());
    if (!referenceIds.isEmpty()) {
      List<Election> elections = electionDAO.findLastElectionsByReferenceIdsAndType(referenceIds, "DataAccess");
      metrics.setElections(elections);
    } else {
      metrics.setElections(Collections.emptyList());
    }
    metrics.setDataset(datasets.iterator().next());
    metrics.setDars(darInfo);
    return metrics;

  }

  public String findPI(Integer userId) {
    List<UserProperty> props = userPropertyDAO.findResearcherPropertiesByUser(userId);

    Optional<UserProperty> isResearcher = props.stream().filter(prop -> prop.getPropertyKey().equals("isThePI") && prop.getPropertyValue().toLowerCase().equals("true")).findFirst();
    if (isResearcher.isPresent()) {
      Optional<UserProperty> userName = props.stream().filter(prop -> prop.getPropertyKey().equals("profileName")).findFirst();
      if(userName.isPresent()) {
        return userName.get().getPropertyValue();
      }
    }

    Optional<UserProperty> piName = props.stream().filter(prop -> prop.getPropertyKey().equals("piName")).findFirst();
    if (piName.isPresent()) {
      return piName.get().getPropertyValue();
    }
    
    return "- -";
  }
}
