package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.VoteMapper;
import org.broadinstitute.consent.http.db.mapper.VoteWithDisplayNameMapper;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(VoteMapper.class)
public interface VoteDAO extends Transactional<VoteDAO> {

  @SqlQuery("select * from vote v where v.vote_id = :voteId")
  Vote findVoteById(@Bind("voteId") Integer voteId);

  @SqlQuery("SELECT * FROM vote v WHERE v.vote_id IN (<voteIds>)")
  List<Vote> findVotesByIds(
      @BindList(value = "voteIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> voteIds);

  @SqlQuery("select * from vote v where v.election_id IN (<electionIds>)")
  List<Vote> findVotesByElectionIds(
      @BindList(value = "electionIds", onEmpty = EmptyHandling.NULL_STRING)
          List<Integer> electionIds);

  /**
   * DAC member votes for the given elections, with the voting member's name. One query for a whole
   * page of collections, rather than one per collection.
   */
  @RegisterRowMapper(VoteWithDisplayNameMapper.class)
  @SqlQuery(
      """
      SELECT v.*, u.display_name
      FROM vote v
      INNER JOIN users u ON u.user_id = v.user_id
      WHERE LOWER(v.type) = 'dac'
      AND v.election_id IN (<electionIds>)
      """)
  List<Vote> findDacVotesWithNamesByElectionIds(
      @BindList(value = "electionIds", onEmpty = EmptyHandling.NULL_STRING)
          List<Integer> electionIds);

  @SqlQuery(
      "select * from vote v where v.election_id = :electionId and lower(v.type) = lower(:type)")
  List<Vote> findVotesByElectionIdAndType(
      @Bind("electionId") Integer electionId, @Bind("type") String type);

  @SqlQuery(
      "select * from vote v where v.election_id = :electionId and v.user_id = :userId and lower(v.type) = 'dac'")
  Vote findVoteByElectionIdAndUserId(
      @Bind("electionId") Integer electionId, @Bind("userId") Integer userId);

  @SqlQuery(
      """
      SELECT vote.vote_id FROM vote
      INNER JOIN election ON election.election_id = vote.election_id
      WHERE election.reference_id = :referenceId
      AND vote.vote_id = :voteId
      """)
  Integer checkVoteById(@Bind("referenceId") String referenceId, @Bind("voteId") Integer voteId);

  @SqlUpdate(
      "INSERT INTO vote (user_id, election_id, type, reminder_sent, create_date) VALUES (:userId, :electionId, :type, false, current_timestamp)")
  @GetGeneratedKeys
  Integer insertVote(
      @Bind("userId") Integer userId,
      @Bind("electionId") Integer electionId,
      @Bind("type") String type);

  @SqlUpdate("update vote set reminder_sent = :reminderSent where vote_id = :voteId")
  void updateVoteReminderFlag(
      @Bind("voteId") Integer voteId, @Bind("reminderSent") boolean reminderSent);

  @SqlQuery(
      """
      SELECT count(*) FROM vote v
      INNER JOIN election e ON v.election_id = e.election_id
      WHERE LOWER(e.election_type) = LOWER(:type)
      AND LOWER(e.status) = 'closed'
      AND LOWER(v.type) = 'final'
      AND v.vote = :finalVote
      """)
  Integer findTotalFinalVoteByElectionTypeAndVote(
      @Bind("type") String type, @Bind("finalVote") Boolean finalVote);

  @SqlQuery(
      "SELECT MAX(c) FROM (SELECT COUNT(vote) as c FROM vote WHERE lower(type) = 'dac' and election_id IN (<electionIds>) GROUP BY election_id) as members")
  Integer findMaxNumberOfDACMembers(
      @BindList(value = "electionIds", onEmpty = EmptyHandling.NULL_STRING)
          List<Integer> electionIds);

  @SqlBatch("insert into vote (user_id, election_id, type) values (:userId, :electionId, :type)")
  void insertVotes(
      @Bind("userId") List<Integer> userIds,
      @Bind("electionId") Integer electionId,
      @Bind("type") String type);

  @SqlUpdate("delete from vote where vote_id IN (<voteIds>)")
  void removeVotesByIds(
      @BindList(value = "voteIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> voteIds);

  @SqlQuery("SELECT * FROM vote v WHERE v.user_id = :userId ")
  List<Vote> findVotesByUserId(@Bind("userId") Integer userId);

  @SqlUpdate("UPDATE vote v SET rationale = :rationale WHERE v.vote_id IN (<voteIds>)")
  void updateRationaleByVoteIds(
      @BindList(value = "voteIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> voteIds,
      @Bind("rationale") String rationale);

  @RegisterBeanMapper(value = User.class)
  @SqlQuery(
      """
      SELECT DISTINCT u.*
      FROM users u
      INNER JOIN vote v ON v.user_id = u.user_id
      INNER JOIN election e ON v.election_id = e.election_id
      WHERE e.reference_id IN (<referenceIds>)
      """)
  List<User> findVoteUsersByElectionReferenceIdList(
      @BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING)
          List<String> referenceIds);
}
