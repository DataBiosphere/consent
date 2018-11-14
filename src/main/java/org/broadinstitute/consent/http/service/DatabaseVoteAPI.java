package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseVoteAPI extends AbstractVoteAPI {

    private VoteDAO voteDAO;
    private DACUserDAO dacUserDAO;
    private ElectionDAO electionDAO;
    private DataSetAssociationDAO dataSetAssociationDAO;

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
    public static void initInstance(VoteDAO dao, DACUserDAO dacUserDAO, ElectionDAO electionDAO, DataSetAssociationDAO dataSetAssociationDAO) {
        VoteAPIHolder.setInstance(new DatabaseVoteAPI(dao, dacUserDAO, electionDAO, dataSetAssociationDAO));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseVoteAPI(VoteDAO dao, DACUserDAO dacUserDAO, ElectionDAO electionDAO, DataSetAssociationDAO dataSetAssociationDAO) {
        this.voteDAO = dao;
        this.dacUserDAO = dacUserDAO;
        this.electionDAO = electionDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
    }

    @Override
    public Vote firstVoteUpdate(Vote rec,  Integer voteId) throws IllegalArgumentException {
        Vote vote = voteDAO.findVoteById(voteId);
        if(vote == null) notFoundException(voteId);
        Integer electionId = setGeneralFields(rec, vote.getElectionId());
        String rationale = StringUtils.isEmpty(rec.getRationale()) ? null : rec.getRationale();
        voteDAO.updateVote(rec.getVote(), rationale, null, voteId, false, electionId, new Date(), rec.getHasConcerns());
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
    public Vote describeVoteFinalAccessVoteById(Integer voteId)
            throws IllegalArgumentException {
        Vote vote = voteDAO.findVoteByElectionIdAndType(voteId, VoteType.FINAL.getValue());
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
    public List<Vote> createVotes(Integer electionId, ElectionType electionType, Boolean isManualReview) {
        Set<DACUser> dacUserList = dacUserDAO.findDACUsersEnabledToVote();
        List<Vote> votes = new ArrayList<>();
        if (dacUserList != null) {
            for (DACUser user : dacUserList) {
                Integer id = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.DAC.getValue(), false);
                votes.add(voteDAO.findVoteById(id));
                if(isChairPerson(user.getDacUserId())){
                    Integer chairPersonVoteId = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.CHAIRPERSON.getValue(), false);
                    votes.add(voteDAO.findVoteById(chairPersonVoteId));
                }
                if (electionType.equals(ElectionType.DATA_ACCESS) && isChairPerson(user.getDacUserId())) {
                    id = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.FINAL.getValue(), false);
                    votes.add(voteDAO.findVoteById(id));
                    if(!isManualReview){
                        id = voteDAO.insertVote(user.getDacUserId(), electionId, VoteType.AGREEMENT.getValue(), false);
                        votes.add(voteDAO.findVoteById(id));
                    }
                }
             }
        }
        return votes;
    }


    @Override
    public List<Vote> describeVoteByTypeAndElectionId(String type, Integer electionId) {
        return voteDAO.findVoteByTypeAndElectionId(electionId, type);
    }

    @Override
    public List<Vote> createDataOwnersReviewVotes(Election election) {
        List<Integer> dataOwners = dataSetAssociationDAO.getDataOwnersOfDataSet(election.getDataSetId());
        voteDAO.insertVotes(dataOwners, election.getElectionId(), VoteType.DATA_OWNER.getValue());
        return voteDAO.findVotesByElectionIdAndType(election.getElectionId(), VoteType.DATA_OWNER.getValue());
    }

    @Override
    public Vote describeDataOwnerVote(String requestId, Integer dataOwnerId) throws NotFoundException {
        Vote vote = voteDAO.findVotesByReferenceIdTypeAndUser(requestId, dataOwnerId, VoteType.DATA_OWNER.getValue());
        if(vote == null) {
            throw new NotFoundException("Vote doesn't exist for the specified dataOwnerId");
        }
        return vote;
    }


    @Override
    public void createVotesForElections(List<Election> elections, Boolean isConsent){
        if(elections != null){
            for(Election election : elections){
                createVotes(election.getElectionId(), ElectionType.TRANSLATE_DUL, false);
            }
        }
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
