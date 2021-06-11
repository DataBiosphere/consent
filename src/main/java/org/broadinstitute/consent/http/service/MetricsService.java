package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import java.sql.Timestamp;

import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.models.User;
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
import java.util.Comparator;
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
  private final DataAccessRequestDAO darDAO;
  private final MatchDAO matchDAO;
  private final ElectionDAO electionDAO;
  private final UserDAO userDAO;

  @Inject
  public MetricsService(DacService dacService, DatasetDAO dataSetDAO, DataAccessRequestDAO darDAO, MatchDAO matchDAO, ElectionDAO electionDAO, UserDAO userDAO) {
    this.dacService = dacService;
    this.dataSetDAO = dataSetDAO;
    this.darDAO = darDAO;
    this.matchDAO = matchDAO;
    this.electionDAO = electionDAO;
    this.userDAO = userDAO;
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
    List<DataAccessRequest> dars = darDAO.findAllDataAccessRequests();
    List<String> referenceIds =
      dars.stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());
    List<Integer> datasetIds =
      dars.stream()
        .map(DataAccessRequest::getData)
        .map(DataAccessRequestData::getDatasetIds)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    List<DataSet> datasets = dataSetDAO.findDatasetsByIdList(datasetIds);
    List<Election> elections = electionDAO.findLastElectionsByReferenceIds(referenceIds);
    List<Match> matches = matchDAO.findMatchesForPurposeIds(referenceIds);
    List<Integer> electionIds =
      elections.stream().map(Election::getElectionId).collect(Collectors.toList());
    List<Dac> dacs = electionDAO.findAllDacsForElectionIds(electionIds);
    //this method generates a list of DarDecisionMetrics representing the given list of dars and including
    //information about each dar's datasets, elections, match, and dac
    List<DarDecisionMetrics> darMetrics = getDarMetrics(dars, datasets, elections, matches, dacs);
    if (type == Type.DAR) {
      return darMetrics;
    } else {
      //if the type is not DAR then it is DAC, so this method returns a list of DacDecisionMetrics representing
      //each dac including information about their datasets and dars that they've reviewed
      List<Dac> allDacs = dacService.findAllDacsWithMembers();
      Set<DatasetDTO> datasetsDacs = dataSetDAO.findDatasetsWithDacs();
      return getDacMetrics(allDacs, datasetsDacs, darMetrics);
    }
  }

  private List<DarDecisionMetrics> getDarMetrics(List<DataAccessRequest> dars,
    List<DataSet> datasets, List<Election> elections,  List<Match> matches, List<Dac> dacs) {
    return dars.stream()
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
  }

  private List<DacDecisionMetrics> getDacMetrics(List<Dac> allDacs, Set<DatasetDTO> datasetsDacs,
    List<DarDecisionMetrics> darMetrics) {
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

  private static Optional<Election> searchFilteredElectionList(List<Election> electionList) {
    //Search for first instance where finalVote is non-null
    //Only one chairperson vote is registered with current flow (later votes won't be registered due to closed status of election)
    //Legacy vote flow had odd behavior where, upon Chair vote submission, all Chair votes within an election would have their vote updated
    //Example: Chair A submits YES, vote for CHAIR A, CHAIR B, CHAIR C would all be YES
    //This has already been corrected in a previous PR so newer votes will not have this issue
    //As such, the above pattern largely holds to older records, (maybe Q3 2020 and earlier)
    //In this instance findFirst is still appropriate since the vote value is still accurate
    //Currently user id is not used in the DataAccess/RP analysis (% agreement, % accurate, etc), so user-vote attribution isn't a concern
    //That said if, for whatever reason, you want to do vote tracking/analysis by user, legacy pattern/records will make the task difficult
    Optional<Election> election = electionList
      .stream()
      .sorted(Comparator.comparing(Election::getFinalVote, Comparator.nullsLast(Comparator.naturalOrder())))
      .findFirst();
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
    List<DataAccessRequest> dars = darDAO.findAllDataAccessRequestsForDatasetMetrics(Integer.toString(datasetId));
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
    if (userId != null) {
      User user = userDAO.findUserWithPropertiesById(userId);

      if (user != null) {
        Optional<UserProperty> isResearcher = user.getProperties().stream().filter(prop -> prop.getPropertyKey().equals("isThePI") && prop.getPropertyValue().equalsIgnoreCase("true")).findFirst();
        if (isResearcher.isPresent()) {
          Optional<UserProperty> userName = user.getProperties().stream().filter(prop -> prop.getPropertyKey().equals("profileName")).findFirst();
          if(userName.isPresent()) {
            return userName.get().getPropertyValue();
          }
        }

        Optional<UserProperty> piName = user.getProperties().stream().filter(prop -> prop.getPropertyKey().equals("piName")).findFirst();
        if (piName.isPresent()) {
          return piName.get().getPropertyValue();
        }
      }
    }
    return "- -";
  }
}
