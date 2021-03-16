package org.broadinstitute.consent.http.service;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.Date;
import java.util.List;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
@Deprecated // Use VoteService
public class DatabaseVoteAPI extends AbstractVoteAPI {

    private VoteDAO voteDAO;
    private ElectionDAO electionDAO;

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
    public static void initInstance(VoteDAO dao, ElectionDAO electionDAO) {
        VoteAPIHolder.setInstance(new DatabaseVoteAPI(dao, electionDAO));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseVoteAPI(VoteDAO dao, ElectionDAO electionDAO) {
        this.voteDAO = dao;
        this.electionDAO = electionDAO;
    }

    @Override
    public Vote updateVoteById(Vote rec,  Integer voteId) throws IllegalArgumentException {
        Vote vote = voteDAO.findVoteById(voteId);
        if (vote == null) notFoundException(voteId);
        Integer electionId = setGeneralFields(rec, vote.getElectionId());
        String rationale = StringUtils.isEmpty(rec.getRationale()) ? null : rec.getRationale();
        boolean reminder = Objects.nonNull(rec.getIsReminderSent()) ? rec.getIsReminderSent() : false;
        Date createDate = Objects.nonNull(vote.getCreateDate()) ? vote.getCreateDate() : new Date();
        voteDAO.updateVote(rec.getVote(), rationale, new Date(), voteId, reminder, electionId, createDate, rec.getHasConcerns());
        return voteDAO.findVoteById(voteId);
    }

    @Override
    public Vote updateVote(Vote rec, Integer voteId, String referenceId) throws IllegalArgumentException {
        if (voteDAO.checkVoteById(referenceId, voteId) == null) notFoundException(voteId);
        Vote vote = voteDAO.findVoteById(voteId);
        Date updateDate = rec.getVote() == null ? null : new Date();
        String rationale = StringUtils.isNotEmpty(rec.getRationale()) ? rec.getRationale() : null;
        voteDAO.updateVote(rec.getVote(), rationale, updateDate, voteId, false,  getElectionId(referenceId), vote.getCreateDate(), rec.getHasConcerns());
        return voteDAO.findVoteById(voteId);
    }

    @Override
    public List<Vote> describeVotes(String referenceId) {
        List<Vote> resultVotes = voteDAO.findVotesByReferenceId(referenceId);
        if (resultVotes == null || resultVotes.isEmpty()) {
            throw new NotFoundException("Could not find vote for specified reference id. Reference id: " + referenceId);
        }
        return resultVotes;
    }


    @Override
    public Vote describeVoteById(Integer voteId, String referenceId)
            throws IllegalArgumentException {
        Vote vote = voteDAO.findVoteById(voteId);
        if (vote == null) {
            throw new NotFoundException("Could not find vote for specified id. Vote id: " + voteId);
        }
        return vote;
    }

    @Override
    public void deleteVote(Integer voteId, String referenceId) {
        if (voteDAO.checkVoteById(referenceId, voteId) == null) {
            throw new NotFoundException("Does not exist vote for the specified id. Id: " + voteId);
        }
        voteDAO.deleteVoteById(voteId);

    }

    @Override
    public void deleteVotes(String referenceId)
            throws IllegalArgumentException, UnknownIdentifierException {
        if (electionDAO.findElectionsWithFinalVoteByReferenceId(referenceId) == null) {
            throw new IllegalArgumentException();
        }
        voteDAO.deleteVotes(referenceId);

    }

    @Override
    public List<Vote> describeVoteByTypeAndElectionId(String type, Integer electionId) {
        return voteDAO.findVoteByTypeAndElectionId(electionId, type);
    }

    @Override
    public Vote describeDataOwnerVote(String requestId, Integer dataOwnerId) throws NotFoundException {
        Vote vote = voteDAO.findVotesByReferenceIdTypeAndUser(requestId, dataOwnerId, VoteType.DATA_OWNER.getValue());
        if(vote == null) {
            throw new NotFoundException("Vote doesn't exist for the specified dataOwnerId");
        }
        return vote;
    }

    private Integer getElectionId(String referenceId) {
        Integer electionId = electionDAO.getOpenElectionIdByReferenceId(referenceId);
        if (electionId == null) {
            throw new IllegalArgumentException("The specified object does not have an election");
        }
        return electionId;
    }

    private Integer setGeneralFields(Vote rec, Integer electionId) {
        rec.setCreateDate(new Date());
        rec.setElectionId(electionId);
        rec.setType(rec.getType());
        return electionId;
    }

    private void notFoundException(Integer voteId){
        throw new NotFoundException("Could not find vote for specified id. Vote id: " + voteId);
    }

}
