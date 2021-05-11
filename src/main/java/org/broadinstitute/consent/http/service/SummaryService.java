package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.HeaderSummary;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Summary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryService {

    private final VoteDAO voteDAO;
    private final ElectionDAO electionDAO;
    private final UserDAO userDAO;
    private final DatasetDAO datasetDAO;
    private final MatchDAO matchDAO;
    private final DataAccessRequestService dataAccessRequestService;
    private static final String SEPARATOR = "\t";
    private static final String TEXT_DELIMITER = "\"";
    private static final String END_OF_LINE = System.lineSeparator();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    public SummaryService(DataAccessRequestService dataAccessRequestService, VoteDAO dao,
        ElectionDAO electionDAO, UserDAO userDAO, DatasetDAO datasetDAO,
        MatchDAO matchDAO) {
        this.dataAccessRequestService = dataAccessRequestService;
        this.voteDAO = dao;
        this.electionDAO = electionDAO;
        this.userDAO = userDAO;
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
                    DataSet dataset = datasetDAO.findDataSetById(electionDAO.getDatasetIdByElectionId(election.getElectionId()));
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

    public static <T> Collector<T, ?, T> singletonCollector() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if(CollectionUtils.isEmpty(list)){
                        return null;
                    }
                    if (list.size() != 1) {
                        throw new IllegalStateException();
                    }
                    return list.get(0);
                }
        );
    }

    public String formatLongToDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        return String.format("%d/%d/%d", month, day, year);
    }

    public String delimiterCheck(String delimitatedString){
        if (StringUtils.isNotEmpty(delimitatedString)) {
            return TEXT_DELIMITER +
                    delimitatedString.replaceAll(TEXT_DELIMITER, "'") + TEXT_DELIMITER;
        } else {
            return "";
        }

    }

}
