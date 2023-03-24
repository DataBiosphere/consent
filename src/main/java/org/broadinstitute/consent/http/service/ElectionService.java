package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectionService {

    private final MailMessageDAO mailMessageDAO;
    private final ConsentDAO consentDAO;
    private final ElectionDAO electionDAO;
    private final VoteDAO voteDAO;
    private final UserDAO userDAO;
    private final DataAccessRequestService dataAccessRequestService;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger("ElectionService");

    @Inject
    public ElectionService(ConsentDAO consentDAO, ElectionDAO electionDAO, VoteDAO voteDAO, UserDAO userDAO,
                           MailMessageDAO mailMessageDAO, EmailService emailService,
                           DataAccessRequestService dataAccessRequestService) {
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.voteDAO = voteDAO;
        this.userDAO = userDAO;
        this.mailMessageDAO = mailMessageDAO;
        this.emailService = emailService;
        this.dataAccessRequestService = dataAccessRequestService;
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
        verifyElectionArchiveState(electionId, rec.getStatus());
        updateSortDate(electionDAO.findElectionWithFinalVoteById(electionId).getReferenceId(), lastUpdate);
        return electionDAO.findElectionWithFinalVoteById(electionId);
    }

    /**
     * Utility method to consolidate archiving elections by status.
     *
     * @param electionId Election Id
     * @param status Election Status String
     */
    private void verifyElectionArchiveState(Integer electionId, String status) {
        Date update = new Date();
        ElectionStatus electionStatus = ElectionStatus.getStatusFromString(status);
        EnumSet<ElectionStatus> archiveStatuses = EnumSet.of(
            ElectionStatus.CANCELED,
            ElectionStatus.CLOSED,
            ElectionStatus.FINAL);
        if (Objects.nonNull(electionStatus) && archiveStatuses.contains(electionStatus)) {
            electionDAO.archiveElectionById(electionId, update);
        }
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
            if (((darVotes.size() == 0) && (rpElectionVotes.size() == 0) && (!chairIds.contains(vote.getUserId())))) {
                return true;
            } else {
                List<Vote> rpChairVotes = voteDAO.findVotesByElectionIdAndUserIds(rpElectionId, chairIds);
                List<Vote> darChairVotes = voteDAO.findVotesByElectionIdAndUserIds(vote.getElectionId(), chairIds);
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
        verifyElectionArchiveState(electionId, election.getStatus());
        try {
            List<Election> dsElections = electionDAO.findLastElectionsByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_SET.getValue());
            if(validateAllDatasetElectionsAreClosed(dsElections)){
                List<Election> darElections = new ArrayList<>();
                darElections.add(electionDAO.findLastElectionByReferenceIdAndType(election.getReferenceId(), ElectionType.DATA_ACCESS.getValue()));
                emailService.sendClosedDataSetElectionsMessage(darElections);
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

    private void validateStatus(String status) {
        if (StringUtils.isNotEmpty(status)) {
            if (ElectionStatus.getValue(status) == null) {
                throw new IllegalArgumentException(
                        "Invalid value. Valid status are: " + ElectionStatus.getValues());
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
            dataAccessRequestService.updateByReferenceId(user, dar);
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

}
