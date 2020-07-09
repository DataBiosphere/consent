package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.ElectionReviewVoteMapper;
import org.broadinstitute.consent.http.db.mapper.VoteMapper;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(VoteMapper.class)
public interface VoteDAO extends Transactional<VoteDAO> {

    @SqlQuery("SELECT v.* FROM vote v INNER JOIN election ON election.electionid = v.electionid WHERE election.referenceid = :referenceId")
    List<Vote> findVotesByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId "
            + "where election.electionId = :electionId and lower(v.type) != 'chairperson'")
    @UseRowMapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId "
            + "where election.electionId = :electionId and lower(v.type) = lower(:type)")
    @UseRowMapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.voteId = :voteId")
    Vote findVoteById(@Bind("voteId") Integer voteId);

    @SqlQuery("select * from vote v where v.electionId = :electionId and lower(v.type) = 'dac'")
    List<Vote> findDACVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIds(@BindList("electionIds") List<Integer> electionIds);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>) and lower(v.type) = lower(:voteType)")
    List<Vote> findVotesByTypeAndElectionIds(@BindList("electionIds") List<Integer> electionIds, @Bind("voteType") String type);

    @SqlQuery("select * from vote v where v.electionId = :electionId and lower(v.type) = lower(:type)")
    List<Vote> findVotesByElectionIdAndType(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.electionId = :electionId and v.vote is null and lower(v.type) = 'dac'")
    List<Vote> findPendingVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select * from vote v where v.electionId = :electionId and v.dacUserId = :dacUserId and lower(v.type) = 'dac'")
    Vote findVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
                                          @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from vote v where v.electionId = :electionId and v.dacUserId in (<dacUserIds>) and lower(v.type) = 'dac'")
    List<Vote> findVotesByElectionIdAndDACUserIds(@Bind("electionId") Integer electionId,
                                                  @BindList("dacUserIds") List<Integer> dacUserIds);

    @SqlQuery("select * from vote v where v.electionId = :electionId and v.dacUserId = :dacUserId and lower(v.type) = lower(:voteType)")
    List<Vote> findVotesByElectionIdAndType(@Bind("electionId") Integer electionId,
                                            @Bind("dacUserId") Integer dacUserId,
                                            @Bind("voteType") String voteType);

    @SqlQuery("select * from vote v where v.electionId = :electionId and lower(v.type) = lower(:type)")
    @Deprecated // This query can return a list of votes and should be avoided
    Vote findVoteByElectionIdAndType(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("SELECT * FROM vote v WHERE v.electionid = :electionId AND LOWER(v.type) = 'final'")
    List<Vote> findFinalVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select * from vote v where v.electionId = :electionId and v.dacUserId = :dacUserId and lower(v.type) = 'final'")
    Vote findChairPersonVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
                                                     @Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("insert into vote (dacUserId, electionId, type, reminderSent) values (:dacUserId, :electionId, :type, false)")
    @GetGeneratedKeys
    Integer insertVote(@Bind("dacUserId") Integer dacUserId,
                       @Bind("electionId") Integer electionId,
                       @Bind("type") String type);

    @SqlUpdate("delete from vote where voteId = :voteId")
    void deleteVoteById(@Bind("voteId") Integer voteId);

    @SqlUpdate("delete from vote v where electionId in (select electionId from election where referenceId = :referenceId) ")
    void deleteVotes(@Bind("referenceId") String referenceId);

    @SqlUpdate("update vote set vote = :vote, updateDate = :updateDate, rationale = :rationale, reminderSent = :reminderSent, createDate = :createDate, has_concerns = :hasConcerns where voteId = :voteId")
    void updateVote(@Bind("vote") Boolean vote,
                    @Bind("rationale") String rationale,
                    @Bind("updateDate") Date updateDate,
                    @Bind("voteId") Integer voteId,
                    @Bind("reminderSent") boolean reminder,
                    @Bind("electionId") Integer electionId,
                    @Bind("createDate") Date createDate,
                    @Bind("hasConcerns") Boolean hasConcerns);


    @SqlUpdate("update vote set reminderSent = :reminderSent where voteId = :voteId")
    void updateVoteReminderFlag(@Bind("voteId") Integer voteId, @Bind("reminderSent") boolean reminderSent);

    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId where election.referenceId = :referenceId and v.dacUserId = :dacUserId and lower(v.type) = lower(:type)")
    Vote findVotesByReferenceIdTypeAndUser(@Bind("referenceId") String referenceId, @Bind("dacUserId") Integer dacUserId, @Bind("type") String voteType);

    @SqlQuery("select v.* from vote v where v.electionId = :electionId and lower(v.type) = lower(:type)")
    List<Vote> findVoteByTypeAndElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select count(*) from vote v inner join election e on v.electionId = e.electionId where lower(e.electionType) = lower(:type) and lower(e.status) = 'closed' and "
            + " lower(v.type) = 'final' and v.vote = :finalVote ")
    Integer findTotalFinalVoteByElectionTypeAndVote(@Bind("type") String type, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("SELECT MAX(c) FROM (SELECT COUNT(vote) as c FROM vote WHERE lower(type) = 'dac' and electionId IN (<electionIds>) GROUP BY electionId) as members")
    Integer findMaxNumberOfDACMembers(@BindList("electionIds") List<Integer> electionIds);

    @SqlBatch("insert into vote (dacUserId, electionId, type) values (:dacUserId, :electionId, :type)")
    void insertVotes(@Bind("dacUserId") List<Integer> dacUserIds, @Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.electionId = :electionId and (v.vote is null and (v.has_concerns = false OR v.has_concerns is null)) and lower(v.type) = lower(:type)")
    List<Vote> findDataOwnerPendingVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.electionId IN (select e.electionId from election e where lower(e.electionType) != 'dataset' "
            + " and (lower(e.status) = 'open' OR lower(e.status) = 'final'))")
    List<Vote> findVotesOnOpenElections(@Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("delete from vote where voteId IN (<voteIds>)")
    void removeVotesByIds(@BindList("voteIds") List<Integer> voteIds);

    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIdsAndUser(@BindList("electionIds") List<Integer> electionIds, @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("SELECT * FROM vote v WHERE v.dacuserid = :dacUserId ")
    List<Vote> findVotesByUserId(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select vote from vote v where v.electionId = :electionId and lower(v.type) = 'chairperson'")
    Boolean findChairPersonVoteByElectionId(@Bind("electionId") Integer electionId);

}
