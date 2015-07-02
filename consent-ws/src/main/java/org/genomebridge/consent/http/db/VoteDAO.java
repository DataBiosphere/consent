package org.genomebridge.consent.http.db;

import java.util.Date;
import java.util.List;

import org.genomebridge.consent.http.models.Vote;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

@RegisterMapper({VoteMapper.class})
public interface VoteDAO extends Transactional<VoteDAO> {


    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId ")
    List<Vote> findVotesByReferenceId(@Bind("referenceId") String referenceId);


    @SqlQuery("select  *  from vote v where  v.voteId = :voteId")
    Vote findVoteById(@Bind("voteId") Integer voteId);
    
    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.isChairPersonVote = false")
    List<Vote> findDACVotesByElectionId(@Bind("electionId") Integer electionId);
    
    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.vote is null and v.isChairPersonVote = false")
    List<Vote> findPendingDACVotesByElectionId(@Bind("electionId") Integer electionId);
    
    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.isChairPersonVote = false")
    Vote findVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId, 
                                          @Bind("dacUserId")  Integer dacUserId);
    
    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.isChairPersonVote = true")
    Vote findChairPersonVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId, 
                                                     @Bind("dacUserId")  Integer dacUserId);

    @SqlQuery("select  v.voteId from vote v "
            + "inner join election on election.electionId = v.electionId  "
            		+ "inner join dacuser du on du.dacUserId = v.dacUserId "
            + "and election.referenceId = :referenceId and v.dacUserId = :dacUserId")
    String findVoteByReferenceIdAndDacUserId(@Bind("referenceId") String referenceId, 
                                             @Bind("dacUserId") Integer dacUserId);


    @SqlQuery("select vote.voteId from vote  inner join election on election.electionId = vote.electionId  "
            + "where election.referenceId = :referenceId "
            + "and vote.voteId = :voteId")
    String checkVoteById(@Bind("referenceId") String referenceId, 
                         @Bind("voteId") Integer voteId);


    @SqlQuery("select  v.*  from vote v "
            + "inner join election on election.electionId = v.electionId  "
            + "inner join datarequest on datarequest.requestId = election.referenceId "
            + "where election.referenceId = :requestId and datarequest.requestId = :requestId "
            + "and v.voteId = :voteId ")
    Vote findVoteByDataRequestIdAndVoteId(@Bind("requestId") String requestId, 
                                          @Bind("voteId") String voteId);


    @SqlUpdate("insert into vote (vote, dacUserId, createDate, updateDate,electionId, rationale, isChairPersonVote, status) values " +
            "(:vote, :dacUserId, :createDate, :updateDate,:electionId, :rationale, :isChairPersonVote, :status)")
    @GetGeneratedKeys
    Integer insertVote(@Bind("vote") Boolean vote,
                       @Bind("dacUserId") Integer dacUserId,
                       @Bind("createDate") Date createDate,
                       @Bind("updateDate") Date updateDate,
                       @Bind("electionId") Integer electionId,
                       @Bind("rationale") String rationale,
                       @Bind("isChairPersonVote") Boolean isChairPersonVote,
                       @Bind("status") String status);
    
    @SqlUpdate("delete from vote where  voteId = :voteId")
    void deleteVoteById(@Bind("voteId") Integer voteId);

    @SqlUpdate("delete v from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId ")
    void deleteVotes(@Bind("referenceId") String referenceId);


    @SqlBatch("delete from vote where electionId = :electionId ")
    void deleteVotesByElection(@Bind("electionId") String electionId);

    @SqlUpdate("update vote set vote = :vote,  updateDate = :updateDate,  rationale = :rationale, status = :status where voteId = :voteId")
    void updateVote(@Bind("vote") Boolean vote,
                    @Bind("rationale") String rationale,
                    @Bind("updateDate") Date updateDate,
                    @Bind("voteId") Integer voteId,
                    @Bind("electionId") Integer electionId,
                    @Bind("status") String status);

}
