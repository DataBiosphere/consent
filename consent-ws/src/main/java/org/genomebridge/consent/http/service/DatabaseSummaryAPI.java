package org.genomebridge.consent.http.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.genomebridge.consent.http.db.ElectionDAO;
import org.genomebridge.consent.http.db.VoteDAO;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.enumeration.HeaderSummary;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Summary;
import org.genomebridge.consent.http.models.Vote;

/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseSummaryAPI extends AbstractSummaryAPI {

    private VoteDAO voteDAO;
    private ElectionDAO electionDAO;
    private static final String SEPARATOR = "\t";
    private final String END_OF_LINE = System.lineSeparator();

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
        SummaryAPIHolder.setInstance(new DatabaseSummaryAPI(dao, electionDAO));

    }

    /**
     * The constructor is private to force use of the factory methods and
     * enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseSummaryAPI(VoteDAO dao, ElectionDAO electionDAO) {
        this.voteDAO = dao;
        this.electionDAO = electionDAO;
    }

    @Override
   	public Summary describeConsentSummaryCases() {
       	String type = electionDAO.findElectionTypeByType(ElectionType.TRANSLATE_DUL.getValue());
        return getSummaryCases(type);
   	}

   	@Override
   	public Summary describeDataRequestSummaryCases() {
   		String type = electionDAO.findElectionTypeByType(ElectionType.DATA_ACCESS.getValue());
        return getSummaryCases(type);
   	}

   	private Summary getSummaryCases(String type) {
   		List<Election> openElections = electionDAO.findElectionsByTypeAndStatus(type, ElectionStatus.OPEN.getValue());
   		Integer totalPendingCases = openElections == null ? 0 : openElections.size(); 
        Integer totalPositiveCases = electionDAO.findTotalElectionsByTypeStatusAndVote(type, ElectionStatus.CLOSED.getValue(), true);
        Integer totalNegativeCases = electionDAO.findTotalElectionsByTypeStatusAndVote(type, ElectionStatus.CLOSED.getValue(), false);
        Summary summary = createSummary(totalPendingCases, totalPositiveCases, totalNegativeCases);
   		return summary;
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
		File file =  null;
		try{
			file = File.createTempFile("summary", ".txt"); 
			FileWriter summaryWriter = new FileWriter(file); 
			
			List<Election> reviewedElections = electionDAO.findElectionsByTypeAndStatus("2", ElectionStatus.CLOSED.getValue());
			setSummaryHeader(summaryWriter);
			if(reviewedElections != null && reviewedElections.size() > 0){
				for(Election election : reviewedElections){
					List<Vote> votes = voteDAO.findDACVotesByElectionId(election.getElectionId());
					
					if(votes != null && votes.size() > 0){
						for(Vote vote : votes){
							summaryWriter.write(election.getReferenceId() + SEPARATOR);
							summaryWriter.write(vote.getVote() + SEPARATOR);
							summaryWriter.write(vote.getRationale() + SEPARATOR);
							summaryWriter.write(election.getFinalVote() + SEPARATOR);
							summaryWriter.write(election.getFinalRationale() + SEPARATOR);
							//sdul is pending
							summaryWriter.write("sDUL" + SEPARATOR);
						    summaryWriter.write(END_OF_LINE);
						}
					}
					
				}
			}
			summaryWriter.flush();
			summaryWriter.close();
			return file;
		}catch(Exception e){
			
		}
		return file;
	}


	private void setSummaryHeader(FileWriter summaryWriter) throws IOException {
		summaryWriter.write(
		HeaderSummary.CASEID.getValue() + SEPARATOR +
		HeaderSummary.VOTE.getValue() + SEPARATOR +
		HeaderSummary.RATIONALE.getValue() + SEPARATOR +
	    HeaderSummary.FINAL_VOTE.getValue() + SEPARATOR + 
		HeaderSummary.FINAL_RATIONALE.getValue() + SEPARATOR +
		HeaderSummary.SDUL.getValue() + END_OF_LINE);
		
	}
	
	
	      
}
