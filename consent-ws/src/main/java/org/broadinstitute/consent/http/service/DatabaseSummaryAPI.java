package org.broadinstitute.consent.http.service;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.HeaderSummary;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseSummaryAPI extends AbstractSummaryAPI {

    private VoteDAO voteDAO;
    private ElectionDAO electionDAO;
    private DACUserDAO dacUserDAO;
    private ConsentDAO consentDAO;
    private DataSetDAO datasetDAO;
    private MatchDAO matchDAO;
    private final MongoConsentDB mongo;
    private static final String SEPARATOR = "\t";
    private static final String TEXT_DELIMITER = "\"";
    private static final String END_OF_LINE = System.lineSeparator();
    private static final String MANUAL_REVIEW = "Manual Review";
    private static final Logger logger = Logger.getLogger(DatabaseSummaryAPI.class.getName());


    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     *
     * @param dao The Data Access Object instance that the API should use to
     *            read/write data.
     */
    public static void initInstance(VoteDAO dao, ElectionDAO electionDAO, DACUserDAO userDAO, ConsentDAO consentDAO ,DataSetDAO datasetDAO, MatchDAO matchDAO, MongoConsentDB mongo) {
        SummaryAPIHolder.setInstance(new DatabaseSummaryAPI(dao, electionDAO, userDAO, consentDAO, datasetDAO, matchDAO,  mongo));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    protected DatabaseSummaryAPI(VoteDAO dao, ElectionDAO electionDAO, DACUserDAO dacUserDAO, ConsentDAO consentDAO , DataSetDAO datasetDAO, MatchDAO matchDAO ,MongoConsentDB mongo) {
        this.voteDAO = dao;
        this.electionDAO = electionDAO;
        this.dacUserDAO = dacUserDAO;
        this.consentDAO = consentDAO;
        this.datasetDAO = datasetDAO;
        this.matchDAO = matchDAO;
        this.mongo = mongo;

    }

    @Override
    public Summary describeConsentSummaryCases() {
        return getSummaryCases(ElectionType.TRANSLATE_DUL.getValue());
    }

    @Override
    public Summary describeDataRequestSummaryCases(String electionType) {
        Summary summary;
        if(electionType.equals(ElectionType.DATA_ACCESS.getValue())){
            summary = getAccessSummaryCases(electionType);
        }else{
            summary = getSummaryCases(electionType);
        }
        return summary;
    }


    @Override
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
            List<Integer> electionIds = reviewedElections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
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
        List<String> status = Arrays.asList(ElectionStatus.FINAL.getValue(), ElectionStatus.OPEN.getValue());
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, status);
        Integer totalPendingCases = openElections == null ? 0 : openElections.size();
        Integer totalPositiveCases = electionDAO.findTotalElectionsByTypeStatusAndVote(type, ElectionStatus.CLOSED.getValue(), true);
        Integer totalNegativeCases = electionDAO.findTotalElectionsByTypeStatusAndVote(type, ElectionStatus.CLOSED.getValue(), false);
        return createSummary(totalPendingCases, totalPositiveCases, totalNegativeCases);
    }

    protected Summary getAccessSummaryCases(String type) {
        List<String> status = Arrays.asList(ElectionStatus.FINAL.getValue(), ElectionStatus.OPEN.getValue());
        List<Election> openElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, status);
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

    @Override
    public File describeConsentSummaryDetail() {
        File file = null;
        try {
            file = File.createTempFile("summary", ".txt");
            try (FileWriter summaryWriter = new FileWriter(file)) {
                List<Election> reviewedElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.TRANSLATE_DUL.getValue(), ElectionStatus.CLOSED.getValue());
                if (!CollectionUtils.isEmpty(reviewedElections)) {
                    List<String> consentIds = reviewedElections.stream().map(e -> e.getReferenceId()).collect(Collectors.toList());
                    List<Integer> electionIds = reviewedElections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
                    Integer maxNumberOfDACMembers = voteDAO.findMaxNumberOfDACMembers(electionIds);
                    setSummaryHeader(summaryWriter, maxNumberOfDACMembers);
                    Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(consentIds);
                    List<Vote> votes = voteDAO.findVotesByElectionIds(electionIds);
                    Collection<Integer> dacUserIds = votes.stream().map(v -> v.getDacUserId()).collect(Collectors.toSet());
                    Collection<DACUser> dacUsers = dacUserDAO.findUsers(dacUserIds);
                    for (Election election : reviewedElections) {
                        Consent electionConsent = consents.stream().filter(c -> c.getConsentId().equals(election.getReferenceId())).collect(singletonCollector());
                        List<Vote> electionVotes = votes.stream().filter(ev -> ev.getElectionId().equals(election.getElectionId())).collect(Collectors.toList());
                        List<Integer> electionVotesUserIds = electionVotes.stream().map(e -> e.getDacUserId()).collect(Collectors.toList());
                        Collection<DACUser> electionDacUsers = dacUsers.stream().filter(du -> electionVotesUserIds.contains(du.getDacUserId())).collect(Collectors.toSet());
                        List<Vote> electionDACVotes = electionVotes.stream().filter(ev -> ev.getType().equals("DAC")).collect(Collectors.toList());
                        Vote chairPersonVote =  electionVotes.stream().filter(ev -> ev.getType().equals("CHAIRPERSON")).collect(singletonCollector());
                        DACUser chairPerson =  dacUsers.stream().filter(du -> du.getDacUserId().equals(chairPersonVote.getDacUserId())).collect(singletonCollector());
                        summaryWriter.write(delimiterCheck(electionConsent.getName()) + SEPARATOR);
                        summaryWriter.write(delimiterCheck(electionConsent.getTranslatedUseRestriction())+ SEPARATOR);
                        summaryWriter.write(formatTimeToDate(electionConsent.getCreateDate().getTime()) + SEPARATOR);
                        summaryWriter.write( chairPerson.getDisplayName() + SEPARATOR);
                        summaryWriter.write( booleanToString(chairPersonVote.getVote()) + SEPARATOR);
                        summaryWriter.write( nullToString(chairPersonVote.getRationale()) + SEPARATOR);
                        if (electionDACVotes != null && electionDACVotes.size() > 0) {
                            for (Vote vote : electionDACVotes) {
                                List<DACUser> dacUser = electionDacUsers.stream().filter(du -> du.getDacUserId().equals(vote.getDacUserId())).collect(Collectors.toList());
                                summaryWriter.write( dacUser.get(0).getDisplayName() + SEPARATOR);
                                summaryWriter.write( booleanToString(vote.getVote()) + SEPARATOR);
                                summaryWriter.write( nullToString(vote.getRationale())+ SEPARATOR);
                            }
                            for (int i = 0; i < (maxNumberOfDACMembers - electionVotes.size()); i++) {
                                summaryWriter.write(
                                        SEPARATOR);
                            }
                        }
                        summaryWriter.write(END_OF_LINE);
                    }
                }else file = null;
                summaryWriter.flush();
            }
            return file;
        } catch (Exception ignored) {
            logger.error("There is an error trying to create statistics file, error: "+ ignored.getMessage());
        }
        return file;
    }

    @Override
    public File describeDataAccessRequestSummaryDetail() {
        File file = null;
        try {
            file = File.createTempFile("DAR_summary", ".txt");
            try (FileWriter summaryWriter = new FileWriter(file)) {
                List<Election> reviewedElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.CLOSED.getValue());
                List<Election> reviewedRPElections = electionDAO.findElectionsWithFinalVoteByTypeAndStatus(ElectionType.RP.getValue(), ElectionStatus.CLOSED.getValue());
                if (!CollectionUtils.isEmpty(reviewedElections)) {
                    List<String> objectIds = reviewedElections.stream().map(e -> e.getReferenceId()).collect(Collectors.toList());
                    FindIterable<Document> dataAccessRequests = findDataAccessRequests(objectIds);
                    HashSet<String> associationObjectIds = new HashSet<>();
                    dataAccessRequests.forEach((Block<Document>) dar -> {
                        associationObjectIds.addAll(dar.get(DarConstants.DATASET_ID, List.class));
                    });
                    List<Association> associations = datasetDAO.getAssociationsForObjectIdList(new ArrayList<String>(associationObjectIds));
                    List<String> associatedConsentIds =   associations.stream().map(a -> a.getConsentId()).collect(Collectors.toList());
                    List<Election> reviewedConsentElections = electionDAO.findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(associatedConsentIds, ElectionStatus.CLOSED.getValue());
                    List<Integer> darElectionIds = reviewedElections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
                    List<Integer> consentElectionIds = reviewedConsentElections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
                    List<AccessRP> accessRPList = electionDAO.findAccessRPbyElectionAccessId(darElectionIds);
                    List<Vote> votes = voteDAO.findVotesByElectionIds(darElectionIds);
                    List<Integer> rpElectionIds = reviewedRPElections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
                    List<Vote> rpVotes;
                    if (CollectionUtils.isNotEmpty(rpElectionIds)){
                        rpVotes =  voteDAO.findVotesByElectionIds(rpElectionIds);
                    } else rpVotes = null;
                    List<Vote> consentVotes = voteDAO.findVotesByElectionIds(consentElectionIds);
                    List<Match> matchList = matchDAO.findMatchesPurposeId(objectIds);
                    Collection<Integer> dacUserIds = votes.stream().map(v -> v.getDacUserId()).collect(Collectors.toSet());
                    Collection<DACUser> dacUsers = dacUserDAO.findUsers(dacUserIds);
                    Integer maxNumberOfDACMembers = voteDAO.findMaxNumberOfDACMembers(darElectionIds);
                    setSummaryHeaderDataAccessRequest(summaryWriter, maxNumberOfDACMembers);
                    for (Election election : reviewedElections) {

                        List<Vote> electionVotes = votes.stream().filter(ev -> ev.getElectionId().equals(election.getElectionId())).collect(Collectors.toList());
                        List<Integer> electionVotesUserIds = electionVotes.stream().filter(v -> v.getType().equals("DAC")).map(e -> e.getDacUserId()).collect(Collectors.toList());
                        Collection<DACUser> electionDacUsers = dacUsers.stream().filter(du -> electionVotesUserIds.contains(du.getDacUserId())).collect(Collectors.toSet());

                        Vote finalVote =  electionVotes.stream().filter(v -> v.getType().equals(VoteType.FINAL.getValue())).collect(singletonCollector());
                        Vote chairPersonVote =  electionVotes.stream().filter(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())).collect(singletonCollector());
                        Vote  chairPersonRPVote = null;
                        Vote agreementVote = null;
                        if(CollectionUtils.isNotEmpty(reviewedRPElections) && CollectionUtils.isNotEmpty(accessRPList)) {
                            agreementVote =  electionVotes.stream().filter(v -> v.getType().equals(VoteType.AGREEMENT.getValue())).collect(singletonCollector());
                            AccessRP accessRP =  accessRPList.stream().filter(arp -> arp.getElectionAccessId().equals(election.getElectionId())).collect(singletonCollector());
                            if (Objects.nonNull(accessRP)) {
                                List<Vote> electionRPVotes = rpVotes.stream().filter(ev -> ev.getElectionId().equals(accessRP.getElectionRPId())).collect(Collectors.toList());
                                chairPersonRPVote = electionRPVotes.stream().filter(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())).collect(singletonCollector());
                            }
                        }
                        DACUser chairPerson =  dacUsers.stream().filter(du -> du.getDacUserId().equals(finalVote.getDacUserId())).collect(singletonCollector());
                        Match match;
                        try {
                            match = matchList.stream().filter(m -> m.getPurpose().equals(election.getReferenceId())).collect(singletonCollector());
                        } catch (IllegalStateException e){
                            match = null;
                        }
                        Document dar = findAssociatedDAR(dataAccessRequests, election.getReferenceId());
                        if ( !dar.isEmpty() ){
                            List<String> datasetId =  dar.get(DarConstants.DATASET_ID, List.class);
                            if(CollectionUtils.isNotEmpty(datasetId)){
                                Association association = associations.stream().filter((as) -> as.getObjectId().equals(datasetId.get(0))).collect(singletonCollector());
                                Election consentElection = reviewedConsentElections.stream().filter(re -> re.getReferenceId().equals(association.getConsentId())).collect(singletonCollector());
                                List<Vote> electionConsentVotes = consentVotes.stream().filter(cv -> cv.getElectionId().equals(consentElection.getElectionId())).collect(Collectors.toList());
                                Vote chairPersonConsentVote =  electionConsentVotes.stream().filter(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())).collect(singletonCollector());
                                summaryWriter.write(dar.get(DarConstants.DAR_CODE) + SEPARATOR);
                                summaryWriter.write(formatTimeToDate(election.getCreateDate().getTime()) + SEPARATOR);
                                summaryWriter.write(chairPerson.getDisplayName() + SEPARATOR);
                                summaryWriter.write( booleanToString(finalVote.getVote()) + SEPARATOR);
                                summaryWriter.write( nullToString(finalVote.getRationale()) + SEPARATOR);
                                if (match != null){
                                    summaryWriter.write( booleanToString(match.getMatch()) + SEPARATOR);
                                }else{
                                    summaryWriter.write(MANUAL_REVIEW + SEPARATOR);
                                }
                                if(agreementVote != null){
                                    summaryWriter.write( booleanToString(agreementVote.getVote()) + SEPARATOR);
                                    summaryWriter.write( nullToString(agreementVote.getRationale())  + SEPARATOR);
                                }else{
                                    summaryWriter.write("-" + SEPARATOR);
                                    summaryWriter.write("-"  + SEPARATOR);
                                }
                                summaryWriter.write( dar.get(DarConstants.INVESTIGATOR)  + SEPARATOR);
                                summaryWriter.write( dar.get(DarConstants.PROJECT_TITLE)  + SEPARATOR);
                                summaryWriter.write( dar.get(DarConstants.DATASET_ID)  + SEPARATOR);summaryWriter.write( formatTimeToDate(dar.getDate("sortDate").getTime())  + SEPARATOR);
                                for (DACUser dacUser : electionDacUsers){
                                    summaryWriter.write( dacUser.getDisplayName() + SEPARATOR);

                                }
                                for (int i = 0; i < (maxNumberOfDACMembers - electionDacUsers.size()); i++) {
                                    summaryWriter.write(
                                            SEPARATOR);
                                }
                                summaryWriter.write( booleanToString(!dar.containsKey(DarConstants.RESTRICTION))+ SEPARATOR);
                                summaryWriter.write( booleanToString(chairPersonVote.getVote()) + SEPARATOR);
                                summaryWriter.write( nullToString(chairPersonVote.getRationale())+ SEPARATOR);

                                if(Objects.nonNull(chairPersonRPVote)){
                                    summaryWriter.write(booleanToString(chairPersonRPVote.getVote()) + SEPARATOR);
                                    summaryWriter.write(nullToString(chairPersonRPVote.getRationale()) + SEPARATOR);
                                }else{
                                    summaryWriter.write(nullToString(null) + SEPARATOR);
                                    summaryWriter.write(nullToString(null) + SEPARATOR);
                                }

                                summaryWriter.write(booleanToString(chairPersonConsentVote.getVote()) + SEPARATOR);
                                summaryWriter.write( nullToString(chairPersonConsentVote.getRationale())+ SEPARATOR);
                            }
                        }
                        summaryWriter.write(END_OF_LINE);
                    }
                }else file = null;
                summaryWriter.flush();
            }
            return file;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return file;
    }

    @Override
    public File describeDataSetElectionsVotesForDar(String darId) {
        File file = null;
        try {
            file = File.createTempFile("dar"+darId+"DatasetElectionsDetail", ".txt");
            try (FileWriter summaryWriter = new FileWriter(file)) {
                List<Election> elections = electionDAO.findLastElectionsByReferenceIdAndType(darId, ElectionType.DATA_SET.getValue());
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

                BasicDBObject query = new BasicDBObject(DarConstants.ID, new ObjectId(darId));
                String dar_code = mongo.getDataAccessRequestCollection().find(query).first().getString(DarConstants.DAR_CODE);
                String dar_election_result;
                try{
                    dar_election_result = (electionDAO.findLastElectionByReferenceIdAndType(darId, ElectionType.DATA_ACCESS.getValue())).getFinalAccessVote() ? "Approved" : "Denied";
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
                        DACUser dacUser = dacUserDAO.findDACUserById(datasetVote.getDacUserId());
                        summaryWriter.write(dacUser.getDisplayName() + SEPARATOR);
                        summaryWriter.write(dacUser.getEmail() + SEPARATOR);
                        summaryWriter.write(datasetVoteResult(datasetVote) + SEPARATOR);
                        summaryWriter.write(datasetVote.getRationale() == null ? "None" : datasetVote.getRationale());
                        summaryWriter.write(SEPARATOR);
                    }
                   summaryWriter.write(END_OF_LINE);
                }
            }
            return file;
        } catch (Exception ignored) {
            logger.error("There is an error trying to create resume of dataset votes file, error: "+ ignored.getMessage());
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

    private void setSummaryHeader(FileWriter summaryWriter , Integer maxNumberOfDACMembers) throws IOException {
        summaryWriter.write(
                HeaderSummary.CONSENT.getValue() + SEPARATOR +
                        HeaderSummary.STRUCT_LIMITATIONS.getValue() + SEPARATOR +
                        HeaderSummary.DATE.getValue() + SEPARATOR +
                        HeaderSummary.CHAIRPERSON.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION_RATIONALE.getValue() + SEPARATOR);
        for (int i = 1; i < maxNumberOfDACMembers; i++) {
            summaryWriter.write(
                    HeaderSummary.USER.getValue() + SEPARATOR +
                    HeaderSummary.VOTE.getValue() + SEPARATOR +
                    HeaderSummary.RATIONALE.getValue() + SEPARATOR);
        }
        summaryWriter.write(
                HeaderSummary.USER.getValue() + SEPARATOR +
                HeaderSummary.VOTE.getValue() + SEPARATOR +
                HeaderSummary.RATIONALE.getValue()+ END_OF_LINE);
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

    private void setSummaryHeaderDataAccessRequest(FileWriter summaryWriter , Integer maxNumberOfDACMembers) throws IOException {
        summaryWriter.write(
                HeaderSummary.DATA_REQUEST_ID.getValue() + SEPARATOR +
                        HeaderSummary.DATE.getValue() + SEPARATOR +
                        HeaderSummary.CHAIRPERSON.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION_RATIONALE.getValue() + SEPARATOR +
                        HeaderSummary.VAULT_DECISION.getValue() + SEPARATOR +
                        HeaderSummary.VAULT_VS_DAC_AGREEMENT.getValue() + SEPARATOR +
                        HeaderSummary.CHAIRPERSON_FEEDBACK.getValue() + SEPARATOR +
                        HeaderSummary.RESEARCHER.getValue() + SEPARATOR +
                        HeaderSummary.PROJECT_TITLE.getValue() + SEPARATOR +
                        HeaderSummary.DATASET_ID.getValue() + SEPARATOR +
                        HeaderSummary.DATA_ACCESS_SUBM_DATE.getValue() + SEPARATOR);
        for (int i = 1; i <= maxNumberOfDACMembers; i++) {
            summaryWriter.write(
                    HeaderSummary.DAC_MEMBERS.getValue() + SEPARATOR);
        }
        summaryWriter.write(
                HeaderSummary.REQUIRE_MANUAL_REVIEW.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION_DAR.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_RATIONALE_DAR.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION_RP.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_RATIONALE_RP.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_DECISION_DUL.getValue() + SEPARATOR +
                        HeaderSummary.FINAL_RATIONALE_DUL.getValue() + END_OF_LINE);
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

    private Document findAssociatedDAR(FindIterable<Document> dataAccessRequests, String referenceId) {
        Document dar = null;
        MongoCursor<Document> itr = dataAccessRequests.iterator();
        try {
            while(itr.hasNext()){
                Document next = itr.next();
                if(next.get(DarConstants.ID).toString().equals(referenceId)){
                    dar = next;
                }
            }
        }finally {
            itr.close();
        }
        return dar;
    }


    private FindIterable<Document> findDataAccessRequests(List<String> objectIds) {
        ObjectId[] objarray = new ObjectId[objectIds.size()];
        for(int i=0;i<objectIds.size();i++)
            objarray[i] = new ObjectId(objectIds.get(i));
        BasicDBObject in = new BasicDBObject("$in", objarray);
        BasicDBObject q = new BasicDBObject(DarConstants.ID, in);
        return  mongo.getDataAccessRequestCollection().find(q);
    }

    private String booleanToString(boolean b) {
        return b ? "YES" : "NO";
    }

    private String nullToString(String b) {
        return b != null && !b.isEmpty()  ? b : "-";
    }

    public String formatTimeToDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        Integer day = cal.get(Calendar.DAY_OF_MONTH);
        Integer month = cal.get(Calendar.MONTH) + 1;
        Integer year = cal.get(Calendar.YEAR);
        String date = month.toString() + "/" + day.toString() + "/" + year.toString();
        return date;
    }

    public String delimiterCheck(String delimitatedString){
        if (StringUtils.isNotEmpty(delimitatedString)) {
            return TEXT_DELIMITER +
                    delimitatedString.replaceAll(TEXT_DELIMITER,"\'") + TEXT_DELIMITER;
        } else {
            return "";
        }

    }

}
