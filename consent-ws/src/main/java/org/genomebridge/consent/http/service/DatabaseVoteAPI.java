package org.genomebridge.consent.http.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.Vote;

import com.sun.jersey.api.NotFoundException;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseVoteAPI extends AbstractVoteAPI {

    private VoteDAO voteDAO;
    private DACUserDAO dacUserDAO;
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
    public static void initInstance(VoteDAO dao, DACUserDAO dacUserDAO, ElectionDAO electionDAO) {
        VoteAPIHolder.setInstance(new DatabaseVoteAPI(dao, dacUserDAO, electionDAO));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseVoteAPI(VoteDAO dao, DACUserDAO dacUserDAO, ElectionDAO electionDAO) {
        this.voteDAO = dao;
        this.dacUserDAO = dacUserDAO;
        this.electionDAO = electionDAO;
    }

    @Override
    public Vote firstVoteUpdate(Vote rec, String referenceId, String voteId) throws IllegalArgumentException {
        Integer electionId = setGeneralFields(rec, referenceId);
        Integer voteID = Integer.parseInt(voteId);
        String rationale = StringUtils.isEmpty(rec.getRationale()) ? null : rec.getRationale();
        voteDAO.updateVote(rec.getVote(), rationale, null, voteID, electionId, new Date());
        return voteDAO.findVoteById(voteID);
    }

    @Override
    public Vote updateVote(Vote rec, Integer voteId, String referenceId) {
        if (voteDAO.checkVoteById(referenceId, voteId) == null) {
            throw new NotFoundException("Could not find vote for specified vote id. Vote id: " + voteId);
        }
        Vote vote = voteDAO.findVoteById(voteId);
        Date updateDate = rec.getVote() == null ? null : new Date();
        String rationale = StringUtils.isNotEmpty(rec.getRationale()) ? rec.getRationale() : null;
        voteDAO.updateVote(rec.getVote(), rationale, updateDate, voteId, getElectionId(referenceId), vote.getCreateDate());
        return voteDAO.findVoteById(voteId);
    }

    @Override
    public List<Vote> describeVotes(String referenceId) {
        List<Vote> resultVotes = voteDAO.findVotesByReferenceId(referenceId);
        if (resultVotes == null || resultVotes.isEmpty()) {
            throw new NotFoundException("Could not find vote for specified object id. Object id: " + referenceId);
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
        if (electionDAO.findElectionsByReferenceId(referenceId) == null) {
            throw new IllegalArgumentException();
        }
        voteDAO.deleteVotes(referenceId);

    }


    @Override
    public List<Vote> createVotes(Integer electionId, Boolean isConsent) {
        List<DACUser> dacUserList = dacUserDAO.findDACUsers();
        List<Vote> votes = new ArrayList<>();
        if (dacUserList != null) {
            for (DACUser user : dacUserList) {
                Integer id = voteDAO.insertVote(user.getDacUserId(), electionId, false);
                votes.add(voteDAO.findVoteById(id));
                if (!isConsent && isChairPerson(user.getDacUserId())) {
                    id = voteDAO.insertVote(user.getDacUserId(), electionId, true);
                    votes.add(voteDAO.findVoteById(id));
                }

            }
        }
        return votes;
    }


    private boolean isChairPerson(Integer dacUserId) {
        boolean isCherperson = false;
        if (dacUserDAO.checkChairpersonUser(dacUserId) != null) {
            isCherperson = true;
        }
        return isCherperson;
    }


    private Integer getElectionId(String referenceId) {
        Integer electionId = electionDAO.getOpenElectionIdByReferenceId(referenceId);
        if (electionId == null) {
            throw new IllegalArgumentException("The specified object does not have an election");
        }
        return electionId;
    }

    private Integer setGeneralFields(Vote rec, String referenceId) {
        rec.setCreateDate(new Date());
        Integer electionId = getElectionId(referenceId);
        rec.setElectionId(electionId);
        rec.setIsChairPersonVote(rec.getIsChairPersonVote() == null ? false : rec.getIsChairPersonVote());
        return electionId;
    }


}
