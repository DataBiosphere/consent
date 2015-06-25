package org.genomebridge.consent.http.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.db.DACUserDAO;
import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
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
	 * @param dao
	 *            The Data Access Object instance that the API should use to
	 *            read/write data.
	 */
	public static void initInstance(VoteDAO dao, DACUserDAO dacUserDAO,ElectionDAO electionDAO) {
		VoteAPIHolder.setInstance(new DatabaseVoteAPI(dao, dacUserDAO, electionDAO));

	}

	/**
	 * The constructor is private to force use of the factory methods and
	 * enforce the singleton pattern.
	 * 
	 * @param dao
	 *            The Data Access Object used to read/write data.
	 */
	private DatabaseVoteAPI(VoteDAO dao, DACUserDAO dacUserDAO, ElectionDAO electionDAO) {
		this.voteDAO = dao;
		this.dacUserDAO = dacUserDAO;
		this.electionDAO = electionDAO;
	}

	@Override
	public Vote createVote(Vote rec, String referenceId) throws IllegalArgumentException {
		rec.setCreateDate(new Date());
		validateDACUser(rec.getDacUserId(),referenceId);
		Integer electionId = getElectionId(referenceId);
		rec.setElectionId(electionId);
		Integer id = voteDAO.insertVote(rec.getVote(), rec.getDacUserId(), rec.getCreateDate(), rec.getUpdateDate(), electionId, rec.getRationale());
		return voteDAO.findVoteById(id);
	}

	

	@Override
	public Vote updateVote(Vote rec, Integer voteId, String referenceId)  {
		if(voteDAO.checkVoteById(referenceId,voteId) == null){
			throw new NotFoundException("Could not find vote for specified vote id. Vote id: "+voteId);
		}
		voteDAO.updateVote(rec.getVote(), rec.getRationale(), new Date(), voteId, getElectionId(referenceId));
		return voteDAO.findVoteById(voteId);
	}

	@Override
	public List<Vote> describeVotes(String referenceId) {
		List<Vote> resultVotes = voteDAO.findVotesByReferenceId(referenceId);
		if(resultVotes == null || resultVotes.isEmpty()){
			throw new NotFoundException("Could not find vote for specified object id. Object id: "+referenceId);
		}
		return resultVotes;
	}
	
	@Override
	public Vote describeVoteById(Integer voteId,String referenceId)
			throws IllegalArgumentException {
		Vote vote = voteDAO.findVoteById(voteId);
		if(vote == null){
			throw new NotFoundException("Could not find vote for specified id. Vote id: "+voteId);
		}
		return vote;
	}

	@Override
	public void deleteVote(Integer voteId, String referenceId) {
		if(voteDAO.checkVoteById(referenceId,voteId)==null){
				throw new NotFoundException("Does not exist vote for the specified id. Id: "+voteId);
		}
		voteDAO.deleteVoteById(voteId);
	
	}

	@Override
	public void deleteVotes(String referenceId)
			throws IllegalArgumentException, UnknownIdentifierException {
		if( electionDAO.getElectionByReferenceId(referenceId)==null){
			throw new IllegalArgumentException();
		}
		voteDAO.deleteVotes(referenceId);
		
	}

	private void validateDACUser(Integer dacUserId,String referenceId) {
		if(dacUserDAO.findDACUserById(dacUserId)==null){
			throw new IllegalArgumentException("Invalid dacUserId: "+dacUserId);
		}
		String voteId = voteDAO.findVoteByReferenceIdAndDacUserId(referenceId, dacUserId);
		if (StringUtils.isNotEmpty(voteId)){
			throw new IllegalArgumentException("A vote has already been posted for the specified Object and Dac User. Vote id: "+voteId);
		}
	
	}
	
	private Integer getElectionId(String referenceId) {
		Integer electionId = electionDAO.getElectionByReferenceId(referenceId);
		if(electionId == null){
			throw new IllegalArgumentException("The specified object does not have an election");
		}
		return electionId;
	}

}
