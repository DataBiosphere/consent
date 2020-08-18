package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DataSetElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.models.dto.ElectionStatusDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.broadinstitute.consent.http.util.DatasetUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation class for ElectionAPI on top of ElectionDAO database support.
 */
@Deprecated // Use ElectionService
public class DatabaseElectionAPI extends AbstractElectionAPI {

    private final MailMessageDAO mailMessageDAO;
    private final ElectionDAO electionDAO;
    private final EmailNotifierService emailNotifierService;
    private final ConsentDAO consentDAO;
    private final VoteDAO voteDAO;
    private final UserDAO userDAO;
    private final DataSetDAO dataSetDAO;
    private final DataSetAssociationDAO datasetAssociationDAO;
    private final DataAccessRequestService dataAccessRequestService;
    private final String DUL_NOT_APROVED = "The Data Use Limitation Election related to this Dataset has not been approved yet.";
    private final String INACTIVE_DS = "Election was not created. The following DataSets are disabled : ";
    private static final Logger logger = LoggerFactory.getLogger("DatabaseElectionAPI");
    private final String DATA_USE_LIMITATION = "Data Use Limitation";
    private final String DATA_ACCESS_REQUEST = "Data Access Request";

    /**
     * Initialize the singleton API instance using the provided DAO. This method
     * should only be called once during application initialization (from the
     * run() method). If called a second time it will throw an
     * IllegalStateException. Note that this method is not synchronized, as it
     * is not intended to be called more than once.
     */
    public static void initInstance(ConsentDAO consentDAO, UserDAO userDAO,
                                    DataAccessRequestService dataAccessRequestService,
                                    DataSetAssociationDAO datasetAssociationDAO, DataSetDAO dataSetDAO,
                                    ElectionDAO electionDAO, EmailNotifierService emailNotifierService,
                                    MailMessageDAO mailMessageDAO, VoteDAO voteDAO) {
        ElectionAPIHolder.setInstance(new DatabaseElectionAPI(consentDAO, userDAO, dataAccessRequestService,
                datasetAssociationDAO, dataSetDAO, electionDAO, emailNotifierService, mailMessageDAO, voteDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     */
    private DatabaseElectionAPI(ConsentDAO consentDAO, UserDAO userDAO,
                                DataAccessRequestService dataAccessRequestService,
                                DataSetAssociationDAO datasetAssociationDAO, DataSetDAO dataSetDAO, ElectionDAO electionDAO,
                                EmailNotifierService emailNotifierService, MailMessageDAO mailMessageDAO,
                                VoteDAO voteDAO) {
        this.consentDAO = consentDAO;
        this.userDAO = userDAO;
        this.dataAccessRequestService = dataAccessRequestService;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.dataSetDAO = dataSetDAO;
        this.electionDAO = electionDAO;
        this.emailNotifierService = emailNotifierService;
        this.mailMessageDAO = mailMessageDAO;
        this.voteDAO = voteDAO;
    }

    @Override
    public Election createElection(Election election, String referenceId, ElectionType electionType) throws Exception {
        Election consentElection = validateAndGetDULElection(referenceId, electionType);
        validateAvailableUsers(consentElection);
        validateReferenceId(referenceId, electionType);
        validateExistentElection(referenceId, electionType);
        validateStatus(election.getStatus());
        setGeneralFields(election, referenceId, electionType);
        Date createDate = new Date();
        Integer id = electionDAO.insertElection(
                election.getElectionType(),
                election.getStatus(),
                createDate,
                election.getReferenceId(),
                election.getFinalAccessVote() ,
                Objects.toString(election.getUseRestriction(), "") ,
                election.getTranslatedUseRestriction(),
                election.getDataUseLetter(),
                election.getDulName(),
                election.getDataSetId());
        updateSortDate(referenceId, createDate);

        switch (electionType) {
            case DATA_ACCESS:
                electionDAO.insertAccessAndConsentElection(id, consentElection.getElectionId());
                break;
            case TRANSLATE_DUL:
                consentDAO.updateConsentUpdateStatus(referenceId, false);
                break;
            case RP:
                Election access = describeDataRequestElection(referenceId);
                electionDAO.insertAccessRP(access.getElectionId(), id);
                break;
            case DATA_SET:
                break;
        }
        return electionDAO.findElectionWithFinalVoteById(id);
    }


    @Override
    public Election updateElectionById(Election rec, Integer electionId) {
        validateStatus(rec.getStatus());
        if (rec.getStatus() == null) {
            rec.setStatus(ElectionStatus.OPEN.getValue());
        } else if (rec.getStatus().equals(ElectionStatus.CLOSED.getValue()) || rec.getStatus().equals(ElectionStatus.FINAL.getValue())) {
            rec.setFinalVoteDate(new Date());
        }
        updateFinalVote(rec, electionId);
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if (election == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        if (rec.getStatus().equals(ElectionStatus.CANCELED.getValue()) || rec.getStatus().equals(ElectionStatus.CLOSED.getValue())) {
            updateAccessElection(electionId, election.getElectionType(), rec.getStatus());
        }
        Date lastUpdate = new Date();
        electionDAO.updateElectionById(electionId, rec.getStatus(), lastUpdate);
        if(rec.getArchived() != null && rec.getArchived()) {
            electionDAO.archiveElectionById(electionId, lastUpdate);
        }
        updateSortDate(electionDAO.findElectionWithFinalVoteById(electionId).getReferenceId(), lastUpdate);
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    @Override
    public Election updateFinalAccessVoteDataRequestElection(Integer electionId) throws Exception {
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if (election == null) {
            throw new NotFoundException("Election for specified id does not exist");
        }
        electionDAO.updateFinalAccessVote(electionId);
        List<Vote> finalVotes = voteDAO.findFinalVotesByElectionId(electionId);
        boolean isApproved = finalVotes.stream().
                filter(Objects::nonNull).
                filter(v -> Objects.nonNull(v.getVote())).
                anyMatch(Vote::getVote);
        if (isApproved) {
            sendResearcherNotification(election.getReferenceId());
            sendDataCustodianNotification(election.getReferenceId());
        }
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    @Override
    public Election describeConsentElection(String consentId) {
        if (consentDAO.checkConsentById(consentId) == null) {
            throw new NotFoundException("Invalid ConsentId");
        }
        Election election = electionDAO.getElectionWithFinalVoteByReferenceIdAndType(consentId, ElectionType.TRANSLATE_DUL.getValue());
        if (election == null) {
            throw new NotFoundException("Election was not found");
        }
        return election;
    }

    @Override
    public void deleteElection(String referenceId, Integer id) {
        if (electionDAO.
                findElectionsWithFinalVoteByReferenceId(referenceId) == null) {
            throw new IllegalArgumentException("Does not exist an election for the specified id");
        }
        Election election = electionDAO.findElectionWithFinalVoteById(id);
        if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
            Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
            electionDAO.deleteAccessRP(id);
            electionDAO.deleteElectionById(rpElectionId);
        }
        electionDAO.deleteElectionById(id);
    }

    @Override
    public Election describeDataRequestElection(String requestId) {
        Election election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(requestId, ElectionType.DATA_ACCESS.getValue());
        if (election == null) {
            election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(requestId, ElectionType.RP.getValue());
        }
        if (election == null) {
            throw new NotFoundException();
        }
        return election;
    }

    @Override
    public Election describeElectionById(Integer electionId) {
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    public Election describeElectionByVoteId(Integer voteId) {
        Election election = electionDAO.findElectionByVoteId(voteId);
        if (election == null) {
            throw new NotFoundException();
        }
        return election;
    }

    @Override
    public Integer findRPElectionByElectionAccessId(Integer electionId) {
        return electionDAO.findRPElectionByElectionAccessId(electionId);
    }

    /**
     * Return true if:
     *      Everyone has already voted
     *      The only remaining votes are chairperson votes
     */
    @Override
    public boolean validateCollectEmailCondition(Vote vote) {
        List<Vote> electionVotes = voteDAO.findPendingVotesByElectionId(vote.getElectionId());
        // Everyone has voted, return true
        if (electionVotes.isEmpty()) return true;

        List<Integer> votingUserIds = electionVotes.stream().
                map(Vote::getDacUserId).
                collect(Collectors.toList());
        Set<User> votingUsers = userDAO.findUsersWithRoles(votingUserIds);
        List<UserRole> votingMemberRoles = votingUsers.stream().flatMap(u -> u.getRoles().stream()).
                filter(r -> r.getRoleId().equals(UserRoles.MEMBER.getRoleId())).
                collect(Collectors.toList());
        // If the voting member roles are empty, we only have chairpersons left, return true
        return votingMemberRoles.isEmpty();
    }

    /**
     * Return true if:
     *      There are no RP or DAR votes and the vote is not a chairperson vote
     *      All RP chair votes have not been created
     *      All DAR chair votes have not been created
     *
     * @param vote The vote to validate
     * @return True if valid, false otherwise
     */
    @Override
    public boolean validateCollectDAREmailCondition(Vote vote) {
        Election e = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
        Integer rpElectionId, darElectionId;
        String darReferenceId = null;
        String rpReferenceId = null;
        if (e.getElectionType().equals(ElectionType.RP.getValue())) {
            rpElectionId = e.getElectionId();
            darElectionId = electionDAO.findAccessElectionByElectionRPId(rpElectionId);
            Election darElection = electionDAO.findElectionById(darElectionId);
            if (Objects.nonNull(darElection)) {
                darReferenceId = darElection.getReferenceId();
            }
        } else {
            darElectionId = e.getElectionId();
            rpElectionId = electionDAO.findRPElectionByElectionAccessId(darElectionId);
            Election rpElection = electionDAO.findElectionById(rpElectionId);
            if (Objects.nonNull(rpElection)) {
                rpReferenceId = rpElection.getReferenceId();
            }
        }
        List<Vote> rpElectionVotes = voteDAO.findPendingVotesByElectionId(rpElectionId);
        List<Vote> darVotes = voteDAO.findPendingVotesByElectionId(darElectionId);
        Set<User> electionChairs = userDAO.findUsersForElectionsByRoles(
                Arrays.asList(darElectionId, rpElectionId),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
        List<Integer> chairIds = electionChairs.stream().map(User::getDacUserId).collect(Collectors.toList());
        Integer exists = mailMessageDAO.existsCollectDAREmail(darReferenceId, rpReferenceId);
        if ((exists == null)) {
            if (((darVotes.size() == 0) && (rpElectionVotes.size() == 0) && (!chairIds.contains(vote.getDacUserId())))) {
                return true;
            } else {
                List<Vote> rpChairVotes = voteDAO.findVotesByElectionIdAndDACUserIds(rpElectionId, chairIds);
                List<Vote> darChairVotes = voteDAO.findVotesByElectionIdAndDACUserIds(vote.getElectionId(), chairIds);
                if ((((rpElectionVotes.size() == 1) && (CollectionUtils.isEmpty(darVotes))))) {
                    return rpChairVotes.stream().allMatch(v -> v.getCreateDate() == null);
                } else {
                    if ((((darVotes.size() == 1) && (CollectionUtils.isEmpty(rpElectionVotes))))) {
                        return darChairVotes.stream().allMatch(v -> v.getCreateDate() == null);
                    } else {
                        if ((((darVotes.size() == 1) && (rpElectionVotes.size() == 1)))) {
                            return darChairVotes.stream().allMatch(v -> v.getCreateDate() == null) &&
                                    rpChairVotes.stream().allMatch(v -> v.getCreateDate() == null);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void closeDataOwnerApprovalElection(Integer electionId){
        Election election = electionDAO.findElectionById(electionId);
        List<Vote> dataOwnersVote = voteDAO.findVotesByElectionIdAndType(election.getElectionId(), VoteType.DATA_OWNER.getValue());
        List<Vote> rejectedVotes = dataOwnersVote.stream().filter(dov -> (dov.getVote() != null && !dov.getVote()) || (dov.getHasConcerns() != null && dov.getHasConcerns())).collect(Collectors.toList());
        election.setFinalAccessVote(CollectionUtils.isEmpty(rejectedVotes) ? true : false);
        election.setStatus(ElectionStatus.CLOSED.getValue());
        electionDAO.updateElectionById(electionId, election.getStatus(), new Date(), election.getFinalAccessVote());
        try {
            List<Election> dsElections = electionDAO.findLastElectionsByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_SET.getValue());
            if(validateAllDatasetElectionsAreClosed(dsElections)){
                List<Election> darElections = new ArrayList<>();
                darElections.add(electionDAO.findLastElectionByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_ACCESS.getValue()));
                emailNotifierService.sendClosedDataSetElectionsMessage(darElections);
            }
        } catch (MessagingException | IOException | TemplateException e) {
            logger.error("Exception sending Closed Dataset Elections email. Cause: " + e.getMessage());
        }
    }

    @Override
    public boolean checkDataOwnerToCloseElection(Integer electionId){
        Boolean closeElection = false;
        Election election = electionDAO.findElectionById(electionId);
        if(election.getElectionType().equals(ElectionType.DATA_SET.getValue())) {
            List<Vote> pendingVotes = voteDAO.findDataOwnerPendingVotesByElectionId(electionId, VoteType.DATA_OWNER.getValue());
            closeElection = CollectionUtils.isEmpty(pendingVotes) ? true : false;
        }
        return closeElection;
    }

    @Override
    public String darDatasetElectionStatus(String darReferenceId){
        List<Integer> dataSets =  DarUtil.getIntegerList(describeDataAccessRequestById(darReferenceId), DarConstants.DATASET_ID);
        List<DataSet> dsForApproval =  dataSetDAO.findNeedsApprovalDataSetByDataSetId(dataSets);
        if(CollectionUtils.isEmpty(dsForApproval)) {
            return DataSetElectionStatus.APPROVAL_NOT_NEEDED.getValue();
        } else {
            Election darElection = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(darReferenceId, ElectionType.DATA_ACCESS.getValue());
            List<Election> dsElectionsToVoteOn = electionDAO.findLastElectionsWithFinalVoteByReferenceIdsAndType(Arrays.asList(darReferenceId), ElectionType.DATA_SET.getValue());
            if((!(Objects.isNull(darElection)) && darElection.getStatus().equals(ElectionStatus.OPEN.getValue())) || CollectionUtils.isEmpty(dsElectionsToVoteOn)){
                return DataSetElectionStatus.DS_PENDING.getValue();
            } else {
                for(Election e: dsElectionsToVoteOn){
                    if(e.getStatus().equals(ElectionStatus.OPEN.getValue())){
                        return DataSetElectionStatus.DS_PENDING.getValue();
                    } else if(!e.getFinalAccessVote()){
                        return DataSetElectionStatus.DS_DENIED.getValue();
                    }
                }
            }
            return DataSetElectionStatus.DS_APPROVED.getValue();
        }
    }

    @Override
    public List<ElectionStatusDTO> describeElectionsByConsentId(String consentId) {
       List<Election> elections = electionDAO.findElectionsWithFinalVoteByReferenceId(consentId);
       List<ElectionStatusDTO> electionStatusDTOs = new ArrayList<>();
        getElectionStatusDTO(electionStatusDTOs, elections, DATA_USE_LIMITATION);
        return electionStatusDTOs;
    }

    @Override
    public Election getConsentElectionByDARElectionId(Integer darElectionId){
        Integer electionId = electionDAO.getElectionConsentIdByDARElectionId(darElectionId);
        return electionId != null ? electionDAO.findElectionById(electionId) : null;
    }


    @Override
    public List<ElectionStatusDTO> describeElectionByDARs(List<Document> darList) {
        List<ElectionStatusDTO> electionStatusDTOs = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(darList)){
            List<String> darIds = new ArrayList<>();
            darList.stream().forEach(dar -> {
                darIds.add(dar.getString(DarConstants.REFERENCE_ID));
                dar.put(DarConstants.REFERENCE_ID, dar.getString(DarConstants.REFERENCE_ID));
            });
            List<Election> elections = electionDAO.findRequestElectionsByReferenceIds(darIds);
            getElectionStatusDTO(electionStatusDTOs, elections, DATA_ACCESS_REQUEST);
        }
        return electionStatusDTOs;
    }

    private void getElectionStatusDTO(List<ElectionStatusDTO> electionStatusDTOs, List<Election> elections, String type) {
        if(CollectionUtils.isNotEmpty(elections)){
            elections.stream().forEach(election -> electionStatusDTOs.add(new ElectionStatusDTO(election.getCreateDate(), election.getStatus(), type)));
        }
    }

    @Override
    public List<Election> createDataSetElections(String referenceId, Map<User, List<DataSet>> dataOwnerDataSet){
        List<Integer> electionsIds = new ArrayList<>();
        dataOwnerDataSet.forEach((user,dataSets) -> {
            dataSets.stream().forEach(dataSet -> {
                if(electionDAO.getOpenElectionByReferenceIdAndDataSet(referenceId, dataSet.getDataSetId()) == null) {
                    Integer electionId = electionDAO.insertElection(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue(), new Date(), referenceId, dataSet.getDataSetId());
                    electionsIds.add(electionId);
                }
            });
        });
        return CollectionUtils.isEmpty(electionsIds) ? null : electionDAO.findElectionsByIds(electionsIds);
    }

    @Override
    public boolean isDataSetElectionOpen() {
        List<Election> elections = electionDAO.getElectionByTypeAndStatus(ElectionType.DATA_SET.getValue(), ElectionStatus.OPEN.getValue());
        return CollectionUtils.isNotEmpty(elections) ? true : false;
    }

    private boolean validateAllDatasetElectionsAreClosed(List<Election> elections){
        for(Election e: elections){
            if(! e.getStatus().equals(ElectionStatus.CLOSED.getValue())){
                return false;
            }
        }
        return true;
    }


    private Election validateAndGetDULElection(String referenceId, ElectionType electionType) throws Exception {
        Election consentElection = null;
        if(electionType.equals(ElectionType.DATA_ACCESS)){
            Document dar = describeDataAccessRequestById(referenceId);
            if(dar == null){
                throw new NotFoundException();
            }
            List<DataSet> dataSets = verifyDisableDataSets(dar, referenceId);
            Consent consent = consentDAO.findConsentFromDatasetID(dataSets.get(0).getDataSetId());
            consentElection = electionDAO.findLastElectionByReferenceIdAndStatus(consent.getConsentId(), "Closed");
            if((consentElection == null)){
                throw new IllegalArgumentException(DUL_NOT_APROVED);
            } else {
                Integer openElections = electionDAO.verifyOpenElectionsForReferenceId(consent.getConsentId());
                Vote vote = voteDAO.findVoteByElectionIdAndType(consentElection.getElectionId(), VoteType.CHAIRPERSON.getValue());
                if((openElections != 0) || (vote != null && !vote.getVote())) {
                    throw new IllegalArgumentException(DUL_NOT_APROVED);
                }
            }
        }
        return consentElection;
    }

    private List<DataSet> verifyDisableDataSets(Document dar, String referenceId) throws  Exception{
        List<Integer> dataSets = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
        List<DataSet> dataSetList = dataSetDAO.findDatasetsByIdList(dataSets);
        List<String> disabledDataSets = dataSetList.stream().filter(ds -> !ds.getActive()).map(DataSet::getObjectId).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(disabledDataSets)) {
            boolean createElection = disabledDataSets.size() == dataSetList.size() ? false : true;
            User user = userDAO.findUserById(dar.getInteger("userId"));
            if(!createElection){
                emailNotifierService.sendDisabledDatasetsMessage(user, disabledDataSets, dar.getString(DarConstants.DAR_CODE));
                throw new IllegalArgumentException(INACTIVE_DS + disabledDataSets.toString());
            }else{
                updateDataAccessRequest(dataSetList, dar, referenceId);
                emailNotifierService.sendDisabledDatasetsMessage(user, disabledDataSets, dar.getString(DarConstants.DAR_CODE));
            }
        }
        return dataSetList;
    }

    private void updateDataAccessRequest(List<DataSet> dataSets, Document dar, String referenceId) {
        List<Document> dataSetList = new ArrayList<>();
        List<String> dataSetId = new ArrayList<>();
        List<DataSet> activeDataSets = dataSets.stream().filter(ds -> ds.getActive()).collect(Collectors.toList());
        activeDataSets.forEach((dataSet) -> {
            Document document = new Document();
            document.put(DarConstants.DATASET_ID, dataSet.getObjectId());
            dataSetId.add(dataSet.getObjectId());
            document.put("name", dataSet.getName());
            dataSetList.add(document);
        });
        dar.put(DarConstants.DATASET_ID, dataSetId);
        dar.put(DarConstants.DATASET_DETAIL, dataSetList);
        dataAccessRequestService.updateDocumentByReferenceId(referenceId, dar);
    }

    private Document describeDataAccessRequestById(String id) {
        return dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(id);
    }

    private void setGeneralFields(Election election, String referenceId, ElectionType electionType) {
        election.setCreateDate(new Date());
        election.setReferenceId(referenceId);
        election.setElectionType(electionType.getValue());

        switch (electionType) {
            case TRANSLATE_DUL:
                Consent consent = consentDAO.findConsentById(referenceId);
                election.setTranslatedUseRestriction(consent.getTranslatedUseRestriction());
                election.setUseRestriction(consent.getUseRestriction());
                election.setDataUseLetter(consent.getDataUseLetter());
                election.setDulName(consent.getDulName());
                break;
            case DATA_ACCESS:
            case RP:
                Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(referenceId);
                List<Integer> datasetIdList = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
                if (datasetIdList != null && !datasetIdList.isEmpty()) {
                    if (datasetIdList.size() > 1) {
                        logger.warn("DAR " +
                                referenceId +
                                " contains multiple datasetId values: " +
                                StringUtils.join(datasetIdList, ", "));
                    }
                    Optional<Integer> datasetId = datasetIdList.stream().findFirst();
                    datasetId.ifPresent(election::setDataSetId);
                }
                election.setTranslatedUseRestriction(dar.getString(DarConstants.TRANSLATED_RESTRICTION));
                try {
                    String restriction = new Gson().toJson(dar.get(DarConstants.RESTRICTION, Map.class));
                    election.setUseRestriction((UseRestriction.parse(restriction)));
                } catch (IOException e) {
                    election.setUseRestriction(null);
                }
                break;
            case DATA_SET:
                break;
        }

        if (StringUtils.isEmpty(election.getStatus())) {
            election.setStatus(ElectionStatus.OPEN.getValue());
        }
    }

    private void validateReferenceId(String referenceId, ElectionType electionType) {
        if (electionType.equals(ElectionType.TRANSLATE_DUL)) {
            validateConsentId(referenceId);
        } else {
            validateDataRequestId(referenceId);
        }
    }

    private void validateDataRequestId(String dataRequest) {
        Document dar = describeDataAccessRequestById(dataRequest);
        if (dataRequest != null && dar == null ) {
            throw new NotFoundException("Invalid id: " + dataRequest);
        }
    }

    private void validateExistentElection(String referenceId, ElectionType type) {
        Election election = electionDAO.getOpenElectionWithFinalVoteByReferenceIdAndType(referenceId, type.getValue());
        if (election != null) {
            throw new IllegalArgumentException(
                    "An open election already exists for the specified id. Election id: "
                            + election.getElectionId());
        }
    }

    private void validateConsentId(String referenceId) {
        if (referenceId == null || consentDAO.checkConsentById(referenceId) == null) {
            throw new IllegalArgumentException("Invalid id: " + referenceId);
        }
    }

    private void validateStatus(String status) {
        if (StringUtils.isNotEmpty(status)) {
            if (ElectionStatus.getValue(status) == null) {
                throw new IllegalArgumentException(
                        "Invalid value. Valid status are: " + ElectionStatus.getValues());
            }
        }
    }

    /*
     * This method shares duplicated code with `VoteService`. This class will eventually be removed
     * in favor of `ElectionService` so leaving this here for now.
     */
    @SuppressWarnings("DuplicatedCode")
    private void validateAvailableUsers(Election election) {
        if (election != null && !ElectionType.DATA_SET.getValue().equals(election.getElectionType())) {
            Dac dac = electionDAO.findDacForElection(election.getElectionId());
            Set<User> users;
            if (dac != null) {
                users = userDAO.findUsersEnabledToVoteByDAC(dac.getDacId());
            } else {
                users = userDAO.findNonDacUsersEnabledToVote();
            }
            if (users == null || users.isEmpty()) {
                throw new IllegalArgumentException("There are no enabled DAC Members or Chairpersons to hold an election.");
            }
            boolean chairpersonExists;
            if (dac == null) {
                chairpersonExists = users.stream()
                        .flatMap(u -> u.getRoles().stream())
                        .anyMatch(r -> r.getName().equalsIgnoreCase(UserRoles.CHAIRPERSON.getRoleName()));
            } else {
                chairpersonExists = users.stream()
                        .flatMap(u -> u.getRoles().stream())
                        .anyMatch(r ->
                                r.getName().equalsIgnoreCase(UserRoles.CHAIRPERSON.getRoleName()) &&
                                        r.getDacId().equals(dac.getDacId()));
            }
            if (!chairpersonExists) {
                throw new IllegalArgumentException("There has to be a Chairperson.");
            }
        }
    }

    private void updateSortDate(String referenceId, Date createDate){
        if(consentDAO.checkConsentById(referenceId) != null){
            consentDAO.updateConsentSortDate(referenceId, createDate);
        } else {
            Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(referenceId);
            dar.put(DarConstants.SORT_DATE, createDate.getTime());
            dataAccessRequestService.updateDocumentByReferenceId(referenceId, dar);
        }
    }

    private void updateAccessElection(Integer electionId, String type, String status) {
        List<Integer> ids = new ArrayList<>();
        if (type.equals(ElectionType.DATA_ACCESS.getValue())) {
            ids.add(electionDAO.findRPElectionByElectionAccessId(electionId));
        } else if (type.equals(ElectionType.RP.getValue())) {
            ids.add(electionDAO.findAccessElectionByElectionRPId(electionId));
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            electionDAO.updateElectionStatus(ids, status);
        }
    }


    private void updateFinalVote(Election rec, Integer electionId) {
        if (rec.getFinalVote() != null) {
            Vote vote = voteDAO.findVoteByElectionIdAndType(electionId, VoteType.CHAIRPERSON.getValue());
            vote.setVote(rec.getFinalVote());
            vote.setCreateDate(rec.getFinalVoteDate());
            vote.setRationale(rec.getFinalRationale());
            voteDAO.updateVote(vote.getVote(), vote.getRationale(), vote.getUpdateDate(), vote.getVoteId(), vote.getIsReminderSent(), vote.getElectionId(), vote.getCreateDate(), vote.getHasConcerns());
        }
    }

    private void sendResearcherNotification(String referenceId) throws Exception {
        Document dar = dataAccessRequestService.getDataAccessRequestByReferenceIdAsDocument(referenceId);
        List<Integer> dataSetIdList = DarUtil.getIntegerList(dar, DarConstants.DATASET_ID);
        if(CollectionUtils.isNotEmpty(dataSetIdList)) {
            List<DataSet> dataSets = dataSetDAO.findDatasetsByIdList(dataSetIdList);
            List<DatasetMailDTO> datasetsDetail = new ArrayList<>();
            dataSets.forEach(ds ->
                datasetsDetail.add(new DatasetMailDTO(ds.getName(), DatasetUtil.parseAlias(ds.getAlias())))
            );
            Consent consent = consentDAO.findConsentFromDatasetID(dataSets.get(0).getDataSetId());
            emailNotifierService.sendResearcherDarApproved(dar.get(DarConstants.DAR_CODE, String.class),  dar.get(DarConstants.USER_ID, Integer.class), datasetsDetail, consent.getTranslatedUseRestriction());
        }
    }

    /**
     * For this Data Access Request, look for any datasets that have a data custodian. Send each custodian a
     * notification email for their datasets with information about the new Data Access Request approval.
     *
     * @param referenceId The DAR reference id
     */
    private void sendDataCustodianNotification(String referenceId) {
        DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
        List<Integer> datasetIdList = dar.getData().getDatasetIds();
        if (CollectionUtils.isNotEmpty(datasetIdList)) {
            Map<Integer, List<DatasetAssociation>> userToAssociationMap = datasetAssociationDAO.
                    getDatasetAssociations(datasetIdList).stream().
                    collect(Collectors.groupingBy(DatasetAssociation::getDacuserId));
            userToAssociationMap.forEach((userId, associationList) -> {
                User custodian = userDAO.findUserById(userId);
                List<Integer> datasetIds = associationList.stream().
                        map(DatasetAssociation::getDatasetId).collect(Collectors.toList());
                List<DatasetMailDTO> mailDTOS = dataSetDAO.findDatasetsByIdList(datasetIds).stream().
                        map(d -> new DatasetMailDTO(d.getName(), DatasetUtil.parseAlias(d.getAlias()))).
                        collect(Collectors.toList());
                try {
                    String researcherEmail = Objects.nonNull(dar.getData().getAcademicEmail()) ?
                            dar.getData().getAcademicEmail() :
                            dar.getData().getResearcher();
                    String darCode = Objects.nonNull(dar.getData().getDarCode()) ?
                            dar.getData().getDarCode() :
                            dar.getReferenceId();
                    emailNotifierService.sendDataCustodianApprovalMessage(custodian.getEmail(), darCode, mailDTOS,
                            custodian.getDisplayName(), researcherEmail);
                } catch (Exception e) {
                    logger.error("Unable to send data custodian approval message: " + e);
                }
            });
        }
    }

}
