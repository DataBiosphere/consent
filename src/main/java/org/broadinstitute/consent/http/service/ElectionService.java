package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ElectionService {

    private final MailMessageDAO mailMessageDAO;
    private final ConsentDAO consentDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final UserDAO userDAO;
    private final DatasetDAO dataSetDAO;
    private final LibraryCardDAO libraryCardDAO;
    private final DatasetAssociationDAO datasetAssociationDAO;
    private final DacService dacService;
    private final DataAccessRequestService dataAccessRequestService;
    private final EmailNotifierService emailNotifierService;
    private final UseRestrictionConverter useRestrictionConverter;
    private final String INACTIVE_DS = "Election was not created. The following DataSets are disabled : ";
    private static final Logger logger = LoggerFactory.getLogger("ElectionService");

    @Inject
    public ElectionService(ConsentDAO consentDAO, ElectionDAO electionDAO, VoteDAO voteDAO, UserDAO userDAO,
                           DatasetDAO dataSetDAO, LibraryCardDAO libraryCardDAO, DatasetAssociationDAO datasetAssociationDAO, MailMessageDAO mailMessageDAO,
                           DacService dacService, EmailNotifierService emailNotifierService,
                           DataAccessRequestService dataAccessRequestService, UseRestrictionConverter useRestrictionConverter) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.userDAO = userDAO;
        this.dataSetDAO = dataSetDAO;
        this.libraryCardDAO = libraryCardDAO;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.mailMessageDAO = mailMessageDAO;
        this.emailNotifierService = emailNotifierService;
        this.dacService = dacService;
        this.dataAccessRequestService = dataAccessRequestService;
        this.useRestrictionConverter = useRestrictionConverter;
    }

    public List<Election> describeClosedElectionsByType(String type, AuthUser authUser) {
        List<Election> elections;
        if (type.equals(ElectionType.DATA_ACCESS.getValue())) {
            elections = dacService.filterElectionsByDAC(
                    electionDAO.findLastDataAccessElectionsWithFinalVoteByStatus(ElectionStatus.CLOSED.getValue()),
                    authUser);
            List<String> referenceIds = elections.stream().map(Election::getReferenceId).collect(Collectors.toList());
            List<DataAccessRequest> dataAccessRequests = dataAccessRequestService.
                    getDataAccessRequestsByReferenceIds(referenceIds);
            elections.forEach(election -> {
                Optional<DataAccessRequest> darOption = dataAccessRequests.stream().
                        filter(d -> d.getReferenceId().equals(election.getReferenceId())).
                        findFirst();
                darOption.ifPresent(dar -> {
                    election.setDisplayId(dar.getData().getDarCode());
                    election.setProjectTitle(dar.getData().getProjectTitle());
                });
            });
        } else {
            elections = dacService.filterElectionsByDAC(
                    electionDAO.findElectionsWithFinalVoteByTypeAndStatus(type, ElectionStatus.CLOSED.getValue()),
                    authUser
            );
            if (!elections.isEmpty()) {
                List<String> consentIds = elections.stream().map(Election::getReferenceId).collect(Collectors.toList());
                Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(consentIds);
                elections.forEach(election -> {
                    List<Consent> c = consents.stream().filter(cs -> cs.getConsentId().equals(election.getReferenceId())).
                            collect(Collectors.toList());
                    election.setDisplayId(c.get(0).getName());
                    election.setConsentGroupName(c.get(0).getGroupName());
                });
            }
        }

        if (elections.isEmpty()) {
            throw new NotFoundException("Couldn't find any closed elections");
        }
        return elections.stream().distinct().collect(Collectors.toList());
    }

    @Deprecated // Use election creation per entity, i.e. DarCollectionService.createElectionsForDarCollection()
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
                election.getDataUseLetter(),
                election.getDulName(),
                election.getDataSetId());
        updateSortDate(referenceId, createDate);

        switch (electionType) {
            case DATA_ACCESS:
                if (Objects.nonNull(consentElection)) {
                    electionDAO.insertAccessAndConsentElection(id, consentElection.getElectionId());
                }
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

    public Election updateElectionById(Election rec, Integer electionId) {
        validateStatus(rec.getStatus());
        if (rec.getStatus() == null) {
            rec.setStatus(ElectionStatus.OPEN.getValue());
        } else if (rec.getStatus().equals(ElectionStatus.CLOSED.getValue()) || rec.getStatus().equals(ElectionStatus.FINAL.getValue())) {
            rec.setFinalVoteDate(new Date());
        }
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

    public Election submitFinalAccessVoteDataRequestElection(Integer electionId, Boolean voteValue) throws Exception {
        Election election = electionDAO.findElectionWithFinalVoteById(electionId);
        if (Objects.isNull(election)) {
            logger.error("Unable to find an election with final vote when submitting a final access vote: " + electionId);
            election = electionDAO.findElectionById(electionId);
            if (Objects.isNull(election)) {
                logger.error("Unable to find an election by id when submitting a final access vote: " + electionId);
                throw new NotFoundException("Election for specified id does not exist");
            }
        }
        String darId = election.getReferenceId();
        DataAccessRequest dar = Objects.nonNull(darId) ? dataAccessRequestService.findByReferenceId(darId) : null;
        if (Objects.isNull(dar)) {
            throw new NotFoundException("Data Access Request for specified id does not exist");
        }
        Integer userId = dar.getUserId();
        List<LibraryCard> libraryCards = Objects.nonNull(userId) ? libraryCardDAO.findLibraryCardsByUserId(userId) : Collections.emptyList();
        //Users cannot submit a DataAccess approval if the researcher does not have a library card
        //However Chairs can still reject DataAccess elections even if the researcher does not have a Library Card
        //voteValue, which represents the vote from the payload, is referenced for this validation step
        if (libraryCards.isEmpty() && (Boolean.TRUE.equals(voteValue))) {
            throw new NotFoundException("No Library cards exist for the researcher on this DAR");
        }
        electionDAO.updateElectionById(
                electionId,
                election.getStatus(),
                new Date(),
                voteValue);
        if (voteValue) {
            sendResearcherNotification(election.getReferenceId());
            sendDataCustodianNotification(election.getReferenceId());
        }
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    public void deleteElection(Integer id) {
        Election election = electionDAO.findElectionById(id);
        if (Objects.isNull(election)) {
            throw new IllegalArgumentException("Does not exist an election for the specified id");
        }
        List<Vote> votes = voteDAO.findPendingVotesByElectionId(id);
        votes.forEach(v -> voteDAO.deleteVoteById(v.getVoteId()));
        if (election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())) {
            Integer rpElectionId = electionDAO.findRPElectionByElectionAccessId(election.getElectionId());
            List<Vote> rpVotes = voteDAO.findVotesByElectionId(rpElectionId);
            rpVotes.forEach(v -> voteDAO.deleteVoteById(v.getVoteId()));
            electionDAO.deleteAccessRP(id);
            electionDAO.deleteElectionById(rpElectionId);
        }
        electionDAO.deleteElectionById(id);
    }

    public Election describeDataRequestElection(String requestId) {
        Election election = electionDAO.getElectionWithFinalVoteByReferenceIdAndType(requestId, ElectionType.DATA_ACCESS.getValue());
        if (election == null) {
            election = electionDAO.getElectionWithFinalVoteByReferenceIdAndType(requestId, ElectionType.RP.getValue());
        }
        if (election == null) {
            throw new NotFoundException();
        }
        return election;
    }

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

    public Integer findRPElectionByElectionAccessId(Integer electionId) {
        return electionDAO.findRPElectionByElectionAccessId(electionId);
    }

    /**
     * Return true if:
     *      Everyone has already voted
     *      The only remaining votes are chairperson votes
     */
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
        List<Integer> chairIds = electionChairs.stream().map(User::getUserId).collect(Collectors.toList());
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

    public boolean checkDataOwnerToCloseElection(Integer electionId){
        Boolean closeElection = false;
        Election election = electionDAO.findElectionById(electionId);
        if(election.getElectionType().equals(ElectionType.DATA_SET.getValue())) {
            List<Vote> pendingVotes = voteDAO.findDataOwnerPendingVotesByElectionId(electionId, VoteType.DATA_OWNER.getValue());
            closeElection = CollectionUtils.isEmpty(pendingVotes) ? true : false;
        }
        return closeElection;
    }

    public List<Election> findElectionsWithCardHoldingUsersByElectionIds(List<Integer> electionIds) {
        return !electionIds.isEmpty() ? electionDAO.findElectionsWithCardHoldingUsersByElectionIds(electionIds) : Collections.emptyList();
    }

    public Election getConsentElectionByDARElectionId(Integer darElectionId){
        Integer electionId = electionDAO.getElectionConsentIdByDARElectionId(darElectionId);
        return electionId != null ? electionDAO.findElectionById(electionId) : null;
    }

    public List<Election> createDataSetElections(String referenceId, Map<User, List<Dataset>> dataOwnerDataSet){
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

    public List<Election> findElectionsByVoteIdsAndType(List<Integer> voteIds, String electionType) {
        return !voteIds.isEmpty() ? electionDAO.findElectionsByVoteIdsAndType(voteIds, electionType) : Collections.emptyList();
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
        if (electionType.equals(ElectionType.DATA_ACCESS)) {
            DataAccessRequest dataAccessRequest = dataAccessRequestService.findByReferenceId(referenceId);
            if (Objects.isNull(dataAccessRequest) || Objects.isNull(dataAccessRequest.getData())) {
                throw new NotFoundException();
            }
            List<Dataset> dataSets = verifyActiveDataSets(dataAccessRequest, referenceId);
            Consent consent = consentDAO.findConsentFromDatasetID(dataSets.get(0).getDataSetId());
            consentElection = electionDAO.findLastElectionByReferenceIdAndStatus(consent.getConsentId(), ElectionStatus.CLOSED.getValue());
        }
        return consentElection;
    }

    private List<Dataset> verifyActiveDataSets(DataAccessRequest dar, String referenceId) throws Exception {
        List<Integer> dataSets = Objects.nonNull(dar) && Objects.nonNull(dar.getData()) ? dar.getData().getDatasetIds() : Collections.emptyList();
        List<Dataset> dataSetList = dataSets.isEmpty() ? Collections.emptyList() : dataSetDAO.findDatasetsByIdList(dataSets);
        List<String> disabledDataSets = dataSetList.stream()
                .filter(ds -> !ds.getActive())
                .map(Dataset::getObjectId)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(disabledDataSets)) {
            boolean createElection = disabledDataSets.size() != dataSetList.size();
            User user = userDAO.findUserById(dar.getUserId());
            if (!createElection) {
                emailNotifierService.sendDisabledDatasetsMessage(user, disabledDataSets, dar.getData().getDarCode());
                throw new IllegalArgumentException(INACTIVE_DS + disabledDataSets.toString());
            } else {
                updateDataAccessRequest(dataSetList, dar, referenceId);
                emailNotifierService.sendDisabledDatasetsMessage(user, disabledDataSets, dar.getData().getDarCode());
            }
        }
        return dataSetList;
    }

    private void updateDataAccessRequest(List<Dataset> dataSets, DataAccessRequest dar, String referenceId) {
        List<DatasetDetailEntry> activeDatasetDetailEntries = new ArrayList<>();
        List<Integer> activeDatasetIds = new ArrayList<>();
        List<Dataset> activeDataSets = dataSets.stream()
                .filter(Dataset::getActive)
                .collect(Collectors.toList());
        activeDataSets.forEach((dataSet) -> {
            activeDatasetIds.add(dataSet.getDataSetId());
            DatasetDetailEntry entry = new DatasetDetailEntry();
            entry.setDatasetId(dataSet.getDataSetId().toString());
            entry.setName(dataSet.getName());
            entry.setObjectId(dataSet.getObjectId());
            activeDatasetDetailEntries.add(entry);
        });
        dar.getData().setDatasetIds(activeDatasetIds);
        dar.getData().setDatasetDetail(activeDatasetDetailEntries);
        dataAccessRequestService.updateByReferenceId(referenceId, dar.getData());
    }

    private DataAccessRequest describeDataAccessRequestById(String id) {
        return dataAccessRequestService.findByReferenceId(id);
    }

    private void setGeneralFields(Election election, String referenceId, ElectionType electionType) {
        election.setCreateDate(new Date());
        election.setReferenceId(referenceId);
        election.setElectionType(electionType.getValue());

        switch (electionType) {
            case TRANSLATE_DUL:
                Consent consent = consentDAO.findConsentById(referenceId);
                election.setDataUseLetter(consent.getDataUseLetter());
                election.setDulName(consent.getDulName());
                break;
            case DATA_ACCESS:
            case RP:
                DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
                List<Integer> datasetIdList = Objects.nonNull(dar) && Objects.nonNull(dar.getData()) ? dar.getData().getDatasetIds() : Collections.emptyList();
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
        DataAccessRequest dar = describeDataAccessRequestById(dataRequest);
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
        if (Objects.nonNull(election) && !ElectionType.DATA_SET.getValue().equals(election.getElectionType())) {
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

    private void updateSortDate(String referenceId, Date createDate) {
        if (Objects.nonNull(consentDAO.checkConsentById(referenceId))) {
            consentDAO.updateConsentSortDate(referenceId, createDate);
        } else {
            DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
            dar.setSortDate(new Timestamp(createDate.getTime()));
            dar.getData().setSortDate(createDate.getTime());
            User user = userDAO.findUserById(dar.getUserId());
            dataAccessRequestService.updateByReferenceIdVersion2(user, dar);
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

    private void sendResearcherNotification(String referenceId) throws Exception {
        DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
        List<Integer> dataSetIdList = dar.getData().getDatasetIds();
        if (CollectionUtils.isNotEmpty(dataSetIdList)) {
            List<Dataset> dataSets = dataSetDAO.findDatasetsByIdList(dataSetIdList);
            List<DatasetMailDTO> datasetsDetail = new ArrayList<>();
            dataSets.forEach(ds ->
                    datasetsDetail.add(new DatasetMailDTO(ds.getName(), ds.getDatasetIdentifier()))
            );
            Consent consent = consentDAO.findConsentFromDatasetID(dataSets.get(0).getDataSetId());
            // Legacy behavior was to populate the plain language translation we received from ORSP
            // If we don't have that and have a valid data use, use that instead as it is more up to date.
            String translatedUseRestriction = consent.getTranslatedUseRestriction();
            if (Objects.isNull(translatedUseRestriction)) {
                if (Objects.nonNull(consent.getDataUse())) {
                    translatedUseRestriction = useRestrictionConverter.translateDataUse(consent.getDataUse(), DataUseTranslationType.DATASET);
                    // update so we don't have to make this check again
                    consentDAO.updateConsentTranslatedUseRestriction(consent.getConsentId(), translatedUseRestriction);
                } else {
                    logger.error("Error finding translation for consent id: " + consent.getConsentId());
                    translatedUseRestriction = "";
                }
            }
            emailNotifierService.sendResearcherDarApproved(dar.getData().getDarCode(),  dar.getUserId(), datasetsDetail, translatedUseRestriction);
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
        if (dar == null) {
            logger.error("Unable to send data custodian approval message: dar could not be found with reference id "+referenceId+".");
            return;
        }

        final User researcher = userDAO.findUserById(dar.getUserId());
        if (researcher == null) {
            logger.error("Unable to send data custodian approval message: researcher could not be found with user id "+dar.getUserId()+".");
            return;
        }

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
                        map(d -> new DatasetMailDTO(d.getName(), d.getDatasetIdentifier())).
                        collect(Collectors.toList());
                try {
                    String researcherEmail = researcher.getEmail();
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
