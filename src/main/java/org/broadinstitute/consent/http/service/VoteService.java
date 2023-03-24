package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class VoteService implements ConsentLogger {

    private final UserDAO userDAO;
    private final DarCollectionDAO darCollectionDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DatasetAssociationDAO datasetAssociationDAO;
    private final DatasetDAO datasetDAO;
    private final ElectionDAO electionDAO;
    private final EmailService emailService;
    private final UseRestrictionConverter useRestrictionConverter;
    private final VoteDAO voteDAO;
    private final VoteServiceDAO voteServiceDAO;

    @Inject
    public VoteService(UserDAO userDAO, DarCollectionDAO darCollectionDAO, DataAccessRequestDAO dataAccessRequestDAO,
                       DatasetAssociationDAO datasetAssociationDAO, DatasetDAO datasetDAO, ElectionDAO electionDAO,
                       EmailService emailService, UseRestrictionConverter useRestrictionConverter,
                       VoteDAO voteDAO, VoteServiceDAO voteServiceDAO) {
        this.userDAO = userDAO;
        this.darCollectionDAO = darCollectionDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.datasetAssociationDAO = datasetAssociationDAO;
        this.datasetDAO = datasetDAO;
        this.electionDAO = electionDAO;
        this.emailService = emailService;
        this.useRestrictionConverter = useRestrictionConverter;
        this.voteDAO = voteDAO;
        this.voteServiceDAO = voteServiceDAO;
    }

    /**
     * Find all votes for a reference id. This can find votes for multiple elections as there
     * are usually multiple forms of election per thing being voted upon.
     *
     * @param referenceId The reference id for the election.
     * @return Collection of votes for the given reference id
     */
    public Collection<Vote> findVotesByReferenceId(String referenceId) {
        return voteDAO.findVotesByReferenceId(referenceId);
    }

    /**
     * Find all votes for an election id.
     *
     * @param electionId The election id for the election.
     * @return Collection of votes on the election specified by the election id
     */
    public List<Vote> findVotesByElectionId(Integer electionId) {
        Election election = electionDAO.findElectionById(electionId);
        if (election == null) {
          throw new NotFoundException();
        }
        return voteDAO.findVotesByElectionId(electionId);
    }


    /**s
     * Update votes such that they have the provided value and rationale.
     *
     * @param voteList  Collection of votes to advance
     * @param voteValue The new vote value
     * @param rationale The new rationale
     */
    public void advanceVotes(Collection<Vote> voteList, boolean voteValue, String rationale) {
        Date now = new Date();
        voteList.forEach(v -> {
            v.setUpdateDate(now);
            v.setCreateDate(now);
            v.setVote(voteValue);
            v.setRationale(rationale);
            updateVote(v);
        });
    }

    /**
     * @param vote Vote to update
     * @return The updated Vote
     */
    public Vote updateVote(Vote vote) {
        validateVote(vote);
        Date now = new Date();
        voteDAO.updateVote(
                vote.getVote(),
                vote.getRationale(),
                Objects.isNull(vote.getUpdateDate()) ? now : vote.getUpdateDate(),
                vote.getVoteId(),
                vote.getIsReminderSent(),
                vote.getElectionId(),
                Objects.isNull(vote.getCreateDate()) ? now : vote.getCreateDate(),
                vote.getHasConcerns()
        );
        return voteDAO.findVoteById(vote.getVoteId());
    }


    public Vote updateVote(Vote rec, Integer voteId, String referenceId) throws IllegalArgumentException {
        if (voteDAO.checkVoteById(referenceId, voteId) == null) notFoundException(voteId);
        Vote vote = voteDAO.findVoteById(voteId);
        Date updateDate = rec.getVote() == null ? null : new Date();
        String rationale = StringUtils.isNotEmpty(rec.getRationale()) ? rec.getRationale() : null;
        voteDAO.updateVote(rec.getVote(), rationale, updateDate, voteId, false,  vote.getElectionId(), vote.getCreateDate(), rec.getHasConcerns());
        return voteDAO.findVoteById(voteId);
    }

    /**
     * Create votes for an election
     *
     * TODO: Refactor duplicated code when DatabaseElectionAPI is fully replaced by ElectionService
     *
     * @param election       The Election
     * @param electionType   The Election type
     * @param isManualReview Is this a manual review election
     * @return List of votes
     */
    @SuppressWarnings("DuplicatedCode")
    public List<Vote> createVotes(Election election, ElectionType electionType, Boolean isManualReview) {
        Dac dac = electionDAO.findDacForElection(election.getElectionId());
        Set<User> users;
        if (dac != null) {
            users = userDAO.findUsersEnabledToVoteByDAC(dac.getDacId());
        } else {
            users = userDAO.findNonDacUsersEnabledToVote();
        }
        List<Vote> votes = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                votes.addAll(createVotesForUser(user, election, electionType, isManualReview));
            }
        }
        return votes;
    }

    /**
     * Create election votes for a user
     *
     * @param user DACUser
     * @param election Election
     * @param electionType ElectionType
     * @param isManualReview Is election manual review
     * @return List of created votes
     */
    public List<Vote> createVotesForUser(User user, Election election, ElectionType electionType, Boolean isManualReview) {
        Dac dac = electionDAO.findDacForElection(election.getElectionId());
        List<Vote> votes = new ArrayList<>();
        Integer dacVoteId = voteDAO.insertVote(user.getUserId(), election.getElectionId(), VoteType.DAC.getValue());
        votes.add(voteDAO.findVoteById(dacVoteId));
        if (isDacChairPerson(dac, user)) {
            Integer chairVoteId = voteDAO.insertVote(user.getUserId(), election.getElectionId(), VoteType.CHAIRPERSON.getValue());
            votes.add(voteDAO.findVoteById(chairVoteId));
            // Requires Chairperson role to create a final and agreement vote in the Data Access case
            if (electionType.equals(ElectionType.DATA_ACCESS)) {
                Integer finalVoteId = voteDAO.insertVote(user.getUserId(), election.getElectionId(), VoteType.FINAL.getValue());
                votes.add(voteDAO.findVoteById(finalVoteId));
                if (!isManualReview) {
                    Integer agreementVoteId = voteDAO.insertVote(user.getUserId(), election.getElectionId(), VoteType.AGREEMENT.getValue());
                    votes.add(voteDAO.findVoteById(agreementVoteId));
                }
            }
        }
        return votes;
    }

    /**
     * Create Votes for a data owner election
     *
     * @param election Election
     * @return Votes for the election
     */
    @SuppressWarnings("UnusedReturnValue")
    public List<Vote> createDataOwnersReviewVotes(Election election) {
        List<Integer> dataOwners = datasetAssociationDAO.getDataOwnersOfDataSet(election.getDataSetId());
        voteDAO.insertVotes(dataOwners, election.getElectionId(), VoteType.DATA_OWNER.getValue());
        return voteDAO.findVotesByElectionIdAndType(election.getElectionId(), VoteType.DATA_OWNER.getValue());
    }

    public List<Vote> findVotesByIds(List<Integer> voteIds) {
        if (voteIds.isEmpty()) {
            return Collections.emptyList();
        }
        return voteDAO.findVotesByIds(voteIds);
    }

    /**
     * Delete any votes in Open elections for the specified user in the specified Dac.
     *
     * @param dac The Dac we are restricting elections to
     * @param user The Dac member we are deleting votes for
     */
    public void deleteOpenDacVotesForUser(Dac dac, User user) {
        List<Integer> openElectionIds = electionDAO.findOpenElectionsByDacId(dac.getDacId()).stream().
                map(Election::getElectionId).
                collect(Collectors.toList());
        if (!openElectionIds.isEmpty()) {
            List<Integer> openUserVoteIds = voteDAO.findVotesByElectionIds(openElectionIds).stream().
                    filter(v -> v.getUserId().equals(user.getUserId())).
                    map(Vote::getVoteId).
                    collect(Collectors.toList());
            if (!openUserVoteIds.isEmpty()) {
                voteDAO.removeVotesByIds(openUserVoteIds);
            }
        }
    }

    /**
     * Update vote values. 'FINAL' votes impact elections so matching elections marked as
     * ElectionStatus.CLOSED as well. Approved 'FINAL' votes trigger an approval email to
     * researchers.
     *
     * @param votes List of Votes to update
     * @param voteValue Value to update the votes to
     * @param rationale Value to update the rationales to. Only update if non-null.
     * @return The updated Vote
     * @throws IllegalArgumentException when there are non-open, non-rp elections on any of the votes
     */
    public List<Vote> updateVotesWithValue(List<Vote> votes, boolean voteValue, String rationale) throws IllegalArgumentException {
        validateVotesCanUpdate(votes);
        try {
            List<Vote> updatedVotes = voteServiceDAO.updateVotesWithValue(votes, voteValue, rationale);
            if (voteValue) {
                try {
                    notifyResearchersOfDarApproval(updatedVotes);
                } catch (Exception e) {
                    // We can recover from an email error, log it and don't fail the overall process.
                    String voteIds = votes.stream().map(Vote::getVoteId).map(Object::toString).collect(Collectors.joining(","));
                    String message = "Error notifying researchers for votes: [" + voteIds + "], " + e.getMessage();
                    logException(message, e);
                }
            }
            return updatedVotes;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Unable to update election votes.");
        }
    }

    /**
     * Review all positive, FINAL votes and send a notification to the researcher describing the approved access to
     * datasets on their Data Access Request.
     *
     * @param votes List of Vote objects. In practice, this will be a batch of votes for a group of elections for
     *              datasets that all have the same data use restriction in a single DarCollection
     *              This method is flexible enough to send email for any number of unrelated elections in various
     *              DarCollections.
     */
    public void notifyResearchersOfDarApproval(List<Vote> votes) {

        List<Integer> finalElectionIds = votes.stream()
            .filter(Vote::getVote) // Safety check to ensure we're only emailing for approved election
            .filter(v -> VoteType.FINAL.getValue().equalsIgnoreCase(v.getType()))
            .map(Vote::getElectionId)
            .distinct()
            .collect(Collectors.toList());

        List<Election> finalElections = finalElectionIds.isEmpty() ? List.of() : electionDAO.findElectionsByIds(finalElectionIds);

        List<String> finalElectionReferenceIds = finalElections.stream()
            .map(Election::getReferenceId)
            .distinct()
            .collect(Collectors.toList());

        List<Integer> collectionIds = finalElectionReferenceIds.isEmpty() ? List.of() : dataAccessRequestDAO
            .findByReferenceIds(finalElectionReferenceIds).stream()
            .map(DataAccessRequest::getCollectionId)
            .collect(Collectors.toList());

        List<DarCollection> collections = collectionIds.isEmpty() ? List.of() :
            darCollectionDAO.findDARCollectionByCollectionIds(collectionIds);

        List<Integer> datasetIds = finalElections.stream()
            .map(Election::getDataSetId)
            .collect(Collectors.toList());
        List<Dataset> datasets = datasetIds.isEmpty() ? List.of() : datasetDAO.findDatasetsByIdList(datasetIds);

        // For each dar collection, email the researcher summarizing the approved datasets in that collection
        collections.forEach(c -> {
            // Get the datasets in this collection that have been approved
            List<Integer> collectionDatasetIds = c.getDars().values().stream()
                .map(DataAccessRequest::getDatasetIds)
                .flatMap(List::stream)
                .distinct()
                .toList();
            List<Dataset> approvedDatasetsInCollection = datasets.stream()
                .filter(d -> collectionDatasetIds.contains(d.getDataSetId()))
                .toList();

            if (!approvedDatasetsInCollection.isEmpty()) {
                String darCode = c.getDarCode();
                User researcher = userDAO.findUserById(c.getCreateUserId());
                Integer researcherId = researcher.getUserId();
                List<DatasetMailDTO> datasetMailDTOs = approvedDatasetsInCollection
                    .stream()
                    .map(d -> new DatasetMailDTO(d.getName(), d.getDatasetIdentifier()))
                    .toList();

                // Get all Data Use translations, distinctly in the case that there are several with the same
                // data use, and then conjoin them for email display.
                List<DataUse> dataUses = approvedDatasetsInCollection.stream()
                    .map(Dataset::getDataUse)
                    .toList();
                List<String> dataUseTranslations = dataUses.stream()
                    .map(d -> useRestrictionConverter.translateDataUse(d, DataUseTranslationType.DATASET))
                    .distinct()
                    .collect(Collectors.toList());
                String translation = String.join(";", dataUseTranslations);

                try {
                    emailService.sendResearcherDarApproved(darCode, researcherId, datasetMailDTOs, translation);
                } catch (Exception e) {
                    logException("Error sending researcher dar approved email: " + e.getMessage(), e);
                }
                try {
                    notifyCustodiansOfApprovedDatasets(approvedDatasetsInCollection, researcher, darCode);
                } catch (Exception e) {
                    logException("Error notifying custodians of dar approved email: " + e.getMessage(), e);
                }
            }
        });
    }

    /**
     * Notify all custodians and data depositors of a dataset approval.
     * @param datasets Requested datasets
     * @param researcher The approved researcher
     * @param darCode The DAR Collection Code
     * @throws IllegalArgumentException when there are no custodians or depositors to notify
     */
    protected void notifyCustodiansOfApprovedDatasets(List<Dataset> datasets, User researcher, String darCode) throws IllegalArgumentException {
        Map<User, HashSet<Dataset>> datasetCustodianMap = new HashMap<>();
        // Find all the custodians, data owners, and data submitters to notify for each dataset
        datasets.forEach(d -> {
            // Data Submitter
            User submitter = userDAO.findUserById(d.getCreateUserId());
            if (Objects.nonNull(submitter)) {
                datasetCustodianMap.putIfAbsent(submitter, new HashSet<>());
                datasetCustodianMap.get(submitter).add(d);
            }
            // Data Depositor
            Optional<DatasetProperty> depositorProp = d.getProperties()
                .stream()
                .filter(p -> p.getPropertyName().equalsIgnoreCase("Data Depositor"))
                .findFirst();
            if (depositorProp.isPresent()) {
                User depositor = userDAO.findUserByEmail(
                    depositorProp.get().getPropertyValueAsString());
                if (Objects.nonNull(depositor)) {
                    datasetCustodianMap.putIfAbsent(depositor, new HashSet<>());
                    datasetCustodianMap.get(depositor).add(d);
                }
            }
            // Data Owners
            List<Integer> datasetOwnersIdList = datasetAssociationDAO.getDataOwnersOfDataSet(d.getDataSetId());
            if (!datasetOwnersIdList.isEmpty()) {
                userDAO.findUsers(datasetOwnersIdList).forEach(u -> {
                    datasetCustodianMap.putIfAbsent(u, new HashSet<>());
                    datasetCustodianMap.get(u).add(d);
                });
            } else {
                logWarn("Unable to find dataset owner associations for dataset identifier: " + d.getDatasetIdentifier());
            }
        });
        if (datasetCustodianMap.isEmpty()) {
            String identifiers = datasets.stream().map(Dataset::getDatasetIdentifier).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("No custodians or depositors found for provided dataset identifiers: " + identifiers);
        }
        // For each custodian, notify them of their approved datasets
        for (Map.Entry<User, HashSet<Dataset>> entry: datasetCustodianMap.entrySet()) {
            List<DatasetMailDTO> datasetMailDTOs = entry.getValue()
                .stream()
                .map(d -> new DatasetMailDTO(d.getName(), d.getDatasetIdentifier()))
                .toList();
            System.out.println(entry.getKey());
            try {
                emailService.sendDataCustodianApprovalMessage(
                    entry.getKey(),
                    darCode,
                    datasetMailDTOs,
                    entry.getKey().getDisplayName(),
                    researcher.getEmail());
            } catch (Exception e) {
                logException("Error sending custodian approval email: " + e.getMessage(), e);
            }
        }
    }

    /**
     * The Rationale for RP Votes can be updated for any election status.
     * The Rationale for DataAccess Votes can only be updated for OPEN elections.
     * Votes for elections of other types are not updatable through this method.
     *
     * @param voteIds List of vote ids for DataAccess and RP elections
     * @param rationale The rationale to update
     * @return List of updated votes
     * @throws IllegalArgumentException when there are non-open, non-rp elections on any of the votes
     */
    public List<Vote> updateRationaleByVoteIds(List<Integer> voteIds, String rationale) throws IllegalArgumentException {
        List<Vote> votes = voteDAO.findVotesByIds(voteIds);
        validateVotesCanUpdate(votes);
        voteDAO.updateRationaleByVoteIds(voteIds, rationale);
        return findVotesByIds(voteIds);
    }

    private void validateVotesCanUpdate(List<Vote> votes) throws IllegalArgumentException {
        List<Election> elections = electionDAO.findElectionsByIds(votes.stream()
                .map(Vote::getElectionId)
                .collect(Collectors.toList()));

        // If there are any DataAccess elections in a non-open state, throw an error
        List<Election> nonOpenAccessElections = elections.stream()
                .filter(election -> election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue()))
                .filter(election -> !election.getStatus().equals(ElectionStatus.OPEN.getValue()))
                .toList();
        if (!nonOpenAccessElections.isEmpty()) {
            throw new IllegalArgumentException("There are non-open Data Access elections for provided votes");
        }

        // If there are non-DataAccess or non-RP elections, throw an error
        List<Election> disallowedElections = elections.stream()
                .filter(election -> !election.getElectionType().equals(ElectionType.DATA_ACCESS.getValue()))
                .filter(election -> !election.getElectionType().equals(ElectionType.RP.getValue()))
                .toList();
        if (!disallowedElections.isEmpty()) {
            throw new IllegalArgumentException("There are non-Data Access/RP elections for provided votes");
        }
    }

    private boolean isDacChairPerson(Dac dac, User user) {
        if (dac != null) {
            return user.getRoles().
                    stream().
                    anyMatch(userRole -> Objects.nonNull(userRole.getRoleId()) &&
                            Objects.nonNull(userRole.getDacId()) &&
                            userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()) &&
                            userRole.getDacId().equals(dac.getDacId()));
        }
        return user.getRoles().
                stream().
                anyMatch(userRole -> Objects.nonNull(userRole.getRoleId()) &&
                        userRole.getRoleId().equals(UserRoles.CHAIRPERSON.getRoleId()));
    }

    /**
     * Convenience method to ensure Vote non-nullable values are populated
     *
     * @param vote The Vote to validate
     */
    private void validateVote(Vote vote) {
        if (Objects.isNull(vote) ||
                Objects.isNull(vote.getVoteId()) ||
                Objects.isNull(vote.getUserId()) ||
                Objects.isNull(vote.getElectionId())) {
            throw new IllegalArgumentException("Invalid vote: " + vote);
        }
        if (Objects.isNull(voteDAO.findVoteById(vote.getVoteId()))) {
            throw new IllegalArgumentException("No vote exists with the id of " + vote.getVoteId());
        }
    }

    private void notFoundException(Integer voteId) {
        throw new NotFoundException("Could not find vote for specified id. Vote id: " + voteId);
    }
}
