package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.resources.Resource.CHAIRPERSON;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.HeaderSummary;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentSummaryDetail;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataAccessRequestSummaryDetail;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryService {

    private final VoteDAO voteDAO;
    private final ElectionDAO electionDAO;
    private final UserDAO userDAO;
    private final ConsentDAO consentDAO;
    private final DatasetDAO datasetDAO;
    private final MatchDAO matchDAO;
    private final DataAccessRequestService dataAccessRequestService;
    private static final String SEPARATOR = "\t";
    private static final String END_OF_LINE = System.lineSeparator();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public SummaryService(DataAccessRequestService dataAccessRequestService, VoteDAO dao,
        ElectionDAO electionDAO, UserDAO userDAO, ConsentDAO consentDAO, DatasetDAO datasetDAO,
        MatchDAO matchDAO) {
        this.dataAccessRequestService = dataAccessRequestService;
        this.voteDAO = dao;
        this.electionDAO = electionDAO;
        this.userDAO = userDAO;
        this.consentDAO = consentDAO;
        this.datasetDAO = datasetDAO;
        this.matchDAO = matchDAO;
    }

    public Summary describeConsentSummaryCases() {
        return getSummaryCases(ElectionType.TRANSLATE_DUL.getValue());
    }

    public Summary describeDataRequestSummaryCases(String electionType) {
        Summary summary;
        if(electionType.equals(ElectionType.DATA_ACCESS.getValue())){
            summary = getAccessSummaryCases(electionType);
        }else{
            summary = getSummaryCases(electionType);
        }
        return summary;
    }

    public List<Summary> describeMatchSummaryCases() {
        return getMatchSummaryCases();
    }

    private List<Summary> getMatchSummaryCases() {
        List<Summary> summaryList = new ArrayList<>();
        summaryList.add(createSummary(0, matchDAO.countMatchesByResult(Boolean.TRUE), matchDAO.countMatchesByResult(Boolean.FALSE)));

        List<Election> latestElections = electionDAO.findLastElectionsWithFinalVoteByType(ElectionType.DATA_ACCESS.getValue());
        List<Election> reviewedElections = null;
        if(!CollectionUtils.isEmpty(latestElections)){
            reviewedElections = latestElections.stream().filter(le -> le.getStatus().equals(ElectionStatus.CLOSED.getValue())).collect(Collectors.toList());
        }
        if( !CollectionUtils.isEmpty(reviewedElections)){
            List<Integer> electionIds = reviewedElections.stream().map(Election::getElectionId).collect(Collectors.toList());
            List<Vote> votes = voteDAO.findVotesByElectionIds(electionIds);

            List<Vote> agreementYesVotes = filterAgreementVotes(votes, Boolean.TRUE);
            List<Vote> agreementNoVotes = filterAgreementVotes(votes, Boolean.FALSE);

            if (CollectionUtils.isNotEmpty(agreementYesVotes) || CollectionUtils.isNotEmpty(agreementNoVotes)) {
                summaryList.add(createSummary(0, agreementYesVotes.size(), agreementNoVotes.size()));
            }
        }else{
            summaryList.add(createSummary(0,0,0));
        }
        return summaryList;
    }

    private List<Vote> filterAgreementVotes(List<Vote> votes, Boolean desiredValue) {
        return votes.stream().filter(
                v -> v.getType().equals(VoteType.AGREEMENT.getValue()) && v.getVote() != null && v.getVote().equals(desiredValue)
        ).collect(Collectors.toList());
    }


    protected Summary getSummaryCases(String type) {
        List<String> statuses = Stream.of(ElectionStatus.FINAL.getValue(), ElectionStatus.OPEN.getValue()).
                map(String::toLowerCase).
                collect(Collectors.toList());
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, statuses);
        Integer totalPendingCases = openElections == null ? 0 : openElections.size();
        Integer totalPositiveCases = electionDAO.findTotalElectionsByTypeStatusAndVote(type, ElectionStatus.CLOSED.getValue(), true);
        Integer totalNegativeCases = electionDAO.findTotalElectionsByTypeStatusAndVote(type, ElectionStatus.CLOSED.getValue(), false);
        return createSummary(totalPendingCases, totalPositiveCases, totalNegativeCases);
    }

    protected Summary getAccessSummaryCases(String type) {
        List<String> statuses = Stream.of(ElectionStatus.FINAL.getValue(), ElectionStatus.OPEN.getValue()).
                map(String::toLowerCase).
                collect(Collectors.toList());
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, statuses);
        Integer totalPendingCases = openElections == null ? 0 : openElections.size();
        Integer totalPositiveCases = voteDAO.findTotalFinalVoteByElectionTypeAndVote(type, true);
        Integer totalNegativeCases = voteDAO.findTotalFinalVoteByElectionTypeAndVote(type, false);
        return createSummary(totalPendingCases, totalPositiveCases, totalNegativeCases);
    }

    private Summary createSummary(Integer totalPendingCases,
                                  Integer totalPositiveCases, Integer totalNegativeCases) {
        Summary summary = new Summary();
        summary.setPendingCases(totalPendingCases);
        summary.setReviewedNegativeCases(totalNegativeCases);
        summary.setReviewedPositiveCases(totalPositiveCases);
        return summary;
    }

    /**
     * Generate a list of Summary Details for all reviewed Consent elections.
     *
     * @return List<ConsentSummaryDetail>
     */
    public List<ConsentSummaryDetail> describeConsentSummaryDetail() {
      List<ConsentSummaryDetail> details = new ArrayList<>();
      try {
        List<String> statuses =
            Stream.of(ElectionStatus.CLOSED.getValue(), ElectionStatus.CANCELED.getValue())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        List<Election> reviewedElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.TRANSLATE_DUL.getValue(), statuses).stream().filter(e -> Objects.nonNull(e.getFinalVote())).distinct().collect(Collectors.toUnmodifiableList());
        if (!CollectionUtils.isEmpty(reviewedElections)) {
          List<String> consentIds =
              reviewedElections.stream().map(Election::getReferenceId).collect(Collectors.toList());
          List<Integer> electionIds =
              reviewedElections.stream().map(Election::getElectionId).collect(Collectors.toList());
          Integer maxNumberOfDACMembers = voteDAO.findMaxNumberOfDACMembers(electionIds);
          Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(consentIds);
          List<Vote> votes = voteDAO.findVotesByElectionIds(electionIds);
          Collection<Integer> dacUserIds =
              votes.stream().map(Vote::getDacUserId).collect(Collectors.toSet());
          Collection<User> users = userDAO.findUsers(dacUserIds);
          for (Election election : reviewedElections) {
            Optional<Consent> electionConsent =
                consents.stream()
                    .filter(c -> c.getConsentId().equals(election.getReferenceId()))
                    .findFirst();
            List<Vote> electionVotes =
                votes.stream()
                    .filter(ev -> ev.getElectionId().equals(election.getElectionId()))
                    .collect(Collectors.toList());
            List<Integer> electionVotesUserIds =
                electionVotes.stream().map(Vote::getDacUserId).collect(Collectors.toList());
            Collection<User> electionUsers =
                users.stream()
                    .filter(du -> electionVotesUserIds.contains(du.getDacUserId()))
                    .collect(Collectors.toSet());
            Optional<Vote> chairPersonVote =
                electionVotes.stream()
                    .filter(ev -> ev.getType().equals(CHAIRPERSON))
                    .findFirst();
            Optional<User> chairPerson = chairPersonVote
                .flatMap(vote -> users.stream()
                .filter(du -> du.getDacUserId().equals(vote.getDacUserId()))
                .findFirst());
            ConsentSummaryDetail detail = new ConsentSummaryDetail(
                    election,
                    electionConsent.orElse(null),
                    electionVotes,
                    electionUsers,
                    chairPersonVote.orElse(null),
                    chairPerson.orElse(null),
                    maxNumberOfDACMembers);
            details.add(detail);
          }
        }
      } catch (Exception e) {
        logger.error("There is an error trying to create statistics file, error: " + e.getMessage());
      }
      return details;
    }

  /**
   * Generate a list of Summary Details for all reviewed Data Access Requests
   * Requires a significant amount of database interaction as we need to find all
   * completed elections for DARs, datasets, votes, matches, and users for those elections.
   * Batch what we can as efficiently as possible and calculate the rest.
   *
   * @return List<DataAccessRequestSummaryDetail>
   */
  public List<DataAccessRequestSummaryDetail> listDataAccessRequestSummaryDetails() {
    List<DataAccessRequestSummaryDetail> details = new ArrayList<>();
    List<Election> accessElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.CLOSED.getValue()).stream().filter(e -> Objects.nonNull(e.getFinalVote())).distinct().collect(Collectors.toUnmodifiableList());
    List<Election> rpElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.RP.getValue(), ElectionStatus.CLOSED.getValue()).stream().filter(e -> Objects.nonNull(e.getFinalVote())).distinct().collect(Collectors.toUnmodifiableList());
    if (!accessElections.isEmpty()) {
      List<String> referenceIds = accessElections.stream().map(Election::getReferenceId).collect(Collectors.toList());
      List<DataAccessRequest> dataAccessRequests = referenceIds.isEmpty() ? Collections.emptyList() : dataAccessRequestService.getDataAccessRequestsByReferenceIds(referenceIds);
      List<Integer> datasetIds =
          dataAccessRequests.stream()
            .filter(Objects::nonNull)
            .map(DataAccessRequest::getData)
            .filter(Objects::nonNull)
            .map(DataAccessRequestData::getDatasetIds)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      List<Association> associations = datasetIds.isEmpty() ? Collections.emptyList() : datasetDAO.getAssociationsForDataSetIdList(datasetIds);
      List<String> associatedConsentIds = associations.stream().map(Association::getConsentId).collect(Collectors.toList());
      List<Election> consentElections = associatedConsentIds.isEmpty() ? Collections.emptyList() : electionDAO.findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(associatedConsentIds, ElectionStatus.CLOSED.getValue());
      List<Integer> accessElectionIds = accessElections.stream().map(Election::getElectionId).collect(Collectors.toList());
      List<Integer> rpElectionIds = rpElections.stream().map(Election::getElectionId).collect(Collectors.toList());
      List<Integer> consentElectionIds = consentElections.stream().map(Election::getElectionId).collect(Collectors.toList());
      List<Vote> accessVotes = accessElectionIds.isEmpty() ? Collections.emptyList() : voteDAO.findVotesByElectionIds(accessElectionIds);
      List<Vote> rpVotes = rpElectionIds.isEmpty() ? Collections.emptyList() : voteDAO.findVotesByElectionIds(rpElectionIds);
      List<Vote> consentVotes = consentElectionIds.isEmpty() ? Collections.emptyList() : voteDAO.findVotesByElectionIds(consentElectionIds);
      List<Match> matchList = referenceIds.isEmpty() ? Collections.emptyList() : matchDAO.findMatchesForPurposeIds(referenceIds);
      List<Integer> voteUserIds = accessVotes.stream().map(Vote::getDacUserId).distinct().collect(Collectors.toList());
      List<User> voteUsers = voteUserIds.isEmpty() ? Collections.emptyList() : new ArrayList<>(userDAO.findUsers(voteUserIds));
      List<Integer> darUserIds = dataAccessRequests.stream().map(DataAccessRequest::getUserId).collect(Collectors.toList());
      List<User> darUsers = darUserIds.isEmpty() ? Collections.emptyList() : new ArrayList<>(userDAO.findUsers(darUserIds));

      // This represents the maximum possible number of DAC members across all elections. We need to
      // pre-calculate this so each row can know the correct max # of DAC members for header
      // construction.
      int maxNumberOfDACMembers = 0;
      for (Election accessElection : accessElections) {
        List<Vote> accessElectionVotes = Objects.nonNull(accessElection) ? accessVotes
            .stream()
            .filter(ev -> ev.getElectionId().equals(accessElection.getElectionId()))
            .collect(Collectors.toList()) :
            Collections.emptyList();
        List<Integer> dacUserIds = accessElectionVotes.stream().map(Vote::getDacUserId).distinct().collect(Collectors.toUnmodifiableList());
        maxNumberOfDACMembers = Math.max(maxNumberOfDACMembers, dacUserIds.size());
      }

      for (Election accessElection : accessElections) {
        List<Vote> accessElectionVotes = Objects.nonNull(accessElection) ? accessVotes
            .stream()
            .filter(ev -> ev.getElectionId().equals(accessElection.getElectionId()))
            .collect(Collectors.toList()) :
            Collections.emptyList();
        Optional<Election> rpElection = rpElections
            .stream()
            .filter(e -> e.getReferenceId().equalsIgnoreCase(accessElection.getReferenceId()))
            .findFirst();
        List<Vote> rpElectionVotes = rpElection
            .map(election -> rpVotes
            .stream()
            .filter(ev -> ev.getElectionId().equals(election.getElectionId()))
            .collect(Collectors.toList())).orElse(Collections.emptyList());
        Optional<Match> matchOption = matchList.stream().filter(m -> m.getPurpose().equals(accessElection.getReferenceId())).findFirst();
        Optional<DataAccessRequest> darOption = dataAccessRequests.stream()
            .filter(Objects::nonNull)
            .filter(d -> d.getReferenceId().equalsIgnoreCase(accessElection.getReferenceId()))
            .findFirst();
        DataAccessRequest dar = darOption.orElse(null);
        List<Integer> dacUserIds = accessElectionVotes.stream().map(Vote::getDacUserId).distinct().collect(Collectors.toUnmodifiableList());
        List<User> dacMembers = voteUsers.stream().filter(v -> dacUserIds.contains(v.getDacUserId())).collect(Collectors.toUnmodifiableList());

        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {
          List<Integer> datasetId = dar.getData().getDatasetIds();
          if (CollectionUtils.isNotEmpty(datasetId)) {
            Optional<User> darUser = darUsers.stream().filter(u -> u.getDacUserId().equals(dar.getUserId())).findFirst();
            details.add(new DataAccessRequestSummaryDetail(
                dar,
                accessElection,
                accessElectionVotes,
                rpElectionVotes,
                consentVotes,
                matchOption.orElse(null),
                dacMembers,
                darUser.orElse(null),
                maxNumberOfDACMembers
            ));
          }
        }
      }
    }
    return details;
  }

    public File describeDataSetElectionsVotesForDar(String referenceId) {
        File file = null;
        try {
            file = File.createTempFile("dar" + referenceId + "DatasetElectionsDetail", ".txt");
            try (FileWriter summaryWriter = new FileWriter(file)) {
                List<Election> elections = electionDAO.findLastElectionsByReferenceIdAndType(referenceId, ElectionType.DATA_SET.getValue());
                Map<Integer, List<Vote>> electionsData = new HashMap<>();
                int maxNumberOfVotes = 0;
                for(Election e: elections){
                    List<Vote> votes = voteDAO.findVoteByTypeAndElectionId(e.getElectionId(), VoteType.DATA_OWNER.getValue());
                    electionsData.put(e.getElectionId(), votes);
                    if(votes.size() > maxNumberOfVotes){
                        maxNumberOfVotes = votes.size();
                    }
                }
                setDatasetElectionsHeader(summaryWriter, maxNumberOfVotes);

                String dar_code = dataAccessRequestService.findByReferenceId(referenceId).getData().getDarCode();
                String dar_election_result;
                try{
                    dar_election_result = (electionDAO.findLastElectionByReferenceIdAndType(referenceId, ElectionType.DATA_ACCESS.getValue())).getFinalAccessVote() ? "Approved" : "Denied";
                } catch (NullPointerException e){
                    dar_election_result = "Pending";
                }
                for (Election election : elections) {
                    summaryWriter.write( dar_code + SEPARATOR);
                    summaryWriter.write( dar_election_result + SEPARATOR);
                    Dataset dataset = datasetDAO.findDatasetById(electionDAO.getDatasetIdByElectionId(election.getElectionId()));
                    summaryWriter.write( dataset.getObjectId() + SEPARATOR);
                    summaryWriter.write( dataset.getName() + SEPARATOR);
                    summaryWriter.write(electionResult(election.getFinalAccessVote()) + SEPARATOR);
                    List<Vote> votes = electionsData.get(election.getElectionId());
                    for(Vote datasetVote : votes){
                        User user = userDAO.findUserById(datasetVote.getDacUserId());
                        summaryWriter.write(user.getDisplayName() + SEPARATOR);
                        summaryWriter.write(user.getEmail() + SEPARATOR);
                        summaryWriter.write(datasetVoteResult(datasetVote) + SEPARATOR);
                        summaryWriter.write(datasetVote.getRationale() == null ? "None" : datasetVote.getRationale());
                        summaryWriter.write(SEPARATOR);
                    }
                   summaryWriter.write(END_OF_LINE);
                }
            }
            return file;
        } catch (Exception e) {
            logger.error("There is an error trying to create resume of dataset votes file, error: "+ e.getMessage());
        }
        return file;
    }

    private String electionResult(Boolean result){
        try{
            if(result){
                return "Approved";
            } else {
                return "Denied";
            }
        } catch( NullPointerException e) {
            return "Pending";
        }
    }

    private String datasetVoteResult(Vote vote){
        try{
            if(vote.getVote()){
                return "Approved";
            } else {
                return "Denied";
            }
        } catch( NullPointerException e) {
            if(vote.getHasConcerns()){
                return "Denied";
            }
            return "Pending";
        }
    }

    private void setDatasetElectionsHeader(FileWriter summaryWriter , Integer maxNumberOfVotes) throws IOException {
        summaryWriter.write(
                HeaderSummary.DATA_REQUEST_ID.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION_DAR.getValue() + SEPARATOR +
                        HeaderSummary.DATASET_ID.getValue() + SEPARATOR +
                        HeaderSummary.DATASET_NAME.getValue() + SEPARATOR +
                        HeaderSummary.DATASET_FINAL_STATUS.getValue() + SEPARATOR);
        for (int i = 0; i < maxNumberOfVotes; i++) {
            summaryWriter.write(
                    HeaderSummary.DATA_OWNER_NAME.getValue() + SEPARATOR +
                            HeaderSummary.DATA_OWNER_EMAIL.getValue() + SEPARATOR +
                            HeaderSummary.DATA_OWNER_VOTE.getValue() + SEPARATOR +
                            HeaderSummary.DATA_OWNER_COMMENT.getValue() + SEPARATOR);
        }
        summaryWriter.write(END_OF_LINE);
    }

}
