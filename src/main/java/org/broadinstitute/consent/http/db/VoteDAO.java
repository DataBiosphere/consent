package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.ElectionReviewVoteMapper;
import org.broadinstitute.consent.http.db.mapper.VoteMapper;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
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

    @SqlQuery("SELECT v.* FROM vote v INNER JOIN election ON election.election_id = v.electionid WHERE election.reference_id = :referenceId")
    List<Vote> findVotesByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery(" SELECT v.*, u.email, u.display_name "
            + " FROM vote v "
            + " INNER JOIN election ON election.election_id = v.electionId "
            + " INNER JOIN users u ON u.user_id = v.dacUserId "
            + " WHERE election.election_id = :electionId AND LOWER(v.type) = LOWER(:type)")
    @UseRowMapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.voteId = :voteId")
    Vote findVoteById(@Bind("voteId") Integer voteId);

    @SqlQuery("SELECT * FROM vote v WHERE v.voteid IN (<voteIds>)")
    List<Vote> findVotesByIds(@BindList("voteIds") List<Integer> voteIds);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIds(@BindList("electionIds") List<Integer> electionIds);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>) and lower(v.type) = lower(:voteType)")
    List<Vote> findVotesByTypeAndElectionIds(@BindList("electionIds") List<Integer> electionIds, @Bind("voteType") String type);

    @SqlQuery("SELECT * FROM vote v WHERE v.electionid = :electionId")
    List<Vote> findVotesByElectionId(@Bind("electionId") Integer electionId);

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

    @SqlQuery(
        "SELECT vote.voteId FROM vote " +
            "INNER JOIN election ON election.election_id = vote.electionId " +
            "WHERE election.reference_id = :referenceId " +
            "AND vote.voteId = :voteId")
    Integer checkVoteById(@Bind("referenceId") String referenceId,
                          @Bind("voteId") Integer voteId);

    @SqlUpdate("insert into vote (dacUserId, electionId, type, reminderSent) values (:dacUserId, :electionId, :type, false)")
    @GetGeneratedKeys
    Integer insertVote(@Bind("dacUserId") Integer dacUserId,
                       @Bind("electionId") Integer electionId,
                       @Bind("type") String type);

    @SqlUpdate("delete from vote where voteId = :voteId")
    void deleteVoteById(@Bind("voteId") Integer voteId);

    @SqlUpdate("DELETE FROM vote v WHERE electionId IN (SELECT election_id FROM election WHERE reference_id = :referenceId) ")
    void deleteVotesByReferenceId(@Bind("referenceId") String referenceId);

    @SqlUpdate("DELETE FROM vote v WHERE electionId IN (SELECT election_id FROM election WHERE reference_id IN (<referenceIds>)) ")
    void deleteVotesByReferenceIds(@BindList("referenceIds") List<String> referenceIds);


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

    @SqlQuery(
        "SELECT v.* FROM vote v " +
            "INNER JOIN election on election.election_id = v.electionId " +
            "WHERE election.reference_id = :referenceId " +
                "AND v.dacUserId = :dacUserId " +
                "AND LOWER(v.type) = LOWER(:type)")
    Vote findVotesByReferenceIdTypeAndUser(@Bind("referenceId") String referenceId, @Bind("dacUserId") Integer dacUserId, @Bind("type") String voteType);

    @SqlQuery("select v.* from vote v where v.electionId = :electionId and lower(v.type) = lower(:type)")
    List<Vote> findVoteByTypeAndElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery(
        "SELECT count(*) FROM vote v " +
            "INNER JOIN election e ON v.electionId = e.election_id " +
            "WHERE LOWER(e.election_type) = LOWER(:type) " +
            "AND LOWER(e.status) = 'closed' " +
            "AND LOWER(v.type) = 'final' " +
            "AND v.vote = :finalVote ")
    Integer findTotalFinalVoteByElectionTypeAndVote(@Bind("type") String type, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("SELECT MAX(c) FROM (SELECT COUNT(vote) as c FROM vote WHERE lower(type) = 'dac' and electionId IN (<electionIds>) GROUP BY electionId) as members")
    Integer findMaxNumberOfDACMembers(@BindList("electionIds") List<Integer> electionIds);

    @SqlBatch("insert into vote (dacUserId, electionId, type) values (:dacUserId, :electionId, :type)")
    void insertVotes(@Bind("dacUserId") List<Integer> dacUserIds, @Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.electionId = :electionId and (v.vote is null and (v.has_concerns = false OR v.has_concerns is null)) and lower(v.type) = lower(:type)")
    List<Vote> findDataOwnerPendingVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlUpdate("delete from vote where voteId IN (<voteIds>)")
    void removeVotesByIds(@BindList("voteIds") List<Integer> voteIds);

    @SqlQuery("SELECT * FROM vote v WHERE v.dacuserid = :dacUserId ")
    List<Vote> findVotesByUserId(@Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("UPDATE vote v SET rationale = :rationale WHERE v.voteid IN (<voteIds>)")
    void updateRationaleByVoteIds(@BindList("voteIds") List<Integer> voteIds, @Bind("rationale") String rationale);

    @RegisterBeanMapper(value = User.class)
    @SqlQuery(" SELECT DISTINCT u.* " +
        " FROM users u " +
        " INNER JOIN vote v ON v.dacuserid = u.user_id " +
        " INNER JOIN election e ON v.electionid = e.election_id " +
        " WHERE e.reference_id IN (<referenceIds>) ")
    List<User> findVoteUsersByElectionReferenceIdList(@BindList("referenceIds") List<String> referenceIds);

}
