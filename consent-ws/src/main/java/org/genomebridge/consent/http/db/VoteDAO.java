package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.ElectionReviewVote;
import org.genomebridge.consent.http.models.Vote;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.Date;
import java.util.List;

@RegisterMapper({VoteMapper.class})
public interface VoteDAO extends Transactional<VoteDAO> {


    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId and election.status = 'Open'")
    List<Vote> findVotesByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId " +
            "where election.electionId = :electionId")
    @Mapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId " +
            "where election.electionId = :electionId and v.type = :type")
    @Mapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId,@Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.voteId = :voteId")
    Vote findVoteById(@Bind("voteId") Integer voteId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.type = 'DAC'")
    List<Vote> findDACVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.vote is null and v.type = 'DAC'")
    List<Vote> findPendingDACVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = 'DAC'")
    Vote findVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
                                          @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.type = 'FINAL'")
    Vote findChairPersonVoteByElectionId(@Bind("electionId") Integer electionId);


    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = 'FINAL'")
    Vote findChairPersonVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
                                                     @Bind("dacUserId") Integer dacUserId);


    @SqlQuery("select vote.voteId from vote  inner join election on election.electionId = vote.electionId  "
            + "where election.referenceId = :referenceId "
            + "and vote.voteId = :voteId")
    Integer checkVoteById(@Bind("referenceId") String referenceId,
                          @Bind("voteId") Integer voteId);


    @SqlUpdate("insert into vote (dacUserId, electionId, type, reminderSent) values " +
            "(:dacUserId,:electionId, :type, :reminderSent)")
    @GetGeneratedKeys
    Integer insertVote(@Bind("dacUserId") Integer dacUserId,
                       @Bind("electionId") Integer electionId,
                       @Bind("type") String type,
                       @Bind("reminderSent") Boolean reminderSent);

    @SqlUpdate("delete from vote where  voteId = :voteId")
    void deleteVoteById(@Bind("voteId") Integer voteId);

    @SqlUpdate("delete v from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId ")
    void deleteVotes(@Bind("referenceId") String referenceId);

    @SqlUpdate("update vote set vote = :vote,  updateDate = :updateDate,  rationale = :rationale, reminderSent = :reminderSent, createDate = :createDate where voteId = :voteId")
    void updateVote(@Bind("vote") Boolean vote,
                    @Bind("rationale") String rationale,
                    @Bind("updateDate") Date updateDate,
                    @Bind("voteId") Integer voteId,
                    @Bind("reminderSent") boolean reminder,
                    @Bind("electionId") Integer electionId,
                    @Bind("createDate") Date createDate);

    @SqlUpdate("update vote set reminderSent = :reminderSent where voteId = :voteId")
    void updateVoteReminderFlag(@Bind("voteId") Integer voteId, @Bind("reminderSent") boolean reminderSent);

    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId  and election.electionType = :type and v.dacUserId = :dacUserId and v.type = 'DAC'")
    Vote findVotesByReferenceIdTypeAndUser(@Bind("referenceId") String referenceId, @Bind("type")Integer type, @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select v.* from vote v where v.electionId = :electionId and v.type = :type")
    List<Vote> findVoteByTypeAndElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);


    @SqlQuery("select count(*) from vote v inner join election e on v.electionId = e.electionId where e.electionType = :type and e.status = 'Closed' and "
            + " v.type = 'FINAL' and v.vote = :finalVote ")
    Integer findTotalFinalVoteByElectionTypeAndVote(@Bind("type") String type, @Bind("finalVote") Boolean finalVote);
}
