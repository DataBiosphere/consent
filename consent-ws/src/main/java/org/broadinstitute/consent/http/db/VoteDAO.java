package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

@UseStringTemplate3StatementLocator
@RegisterMapper({VoteMapper.class})
public interface VoteDAO extends Transactional<VoteDAO> {

    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId and election.status = 'Open'")
    List<Vote> findVotesByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>) and v.dacUserId = :dacUserId and v.vote is null")
    List<Vote> findPendingVotesByElectionsIdsAndUserId(@BindIn("electionIds") List<Integer> electionIds, @Bind("dacUserId") Integer dacUserId);

    // ALL VOTES, EVEN THE ONES NOT NULL
    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>) and v.dacUserId = :dacUserId")
    List<Vote> findVotesByElectionsIdsAndUserIdIncludeMade(@BindIn("electionIds") List<Integer> electionIds, @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>) and v.dacUserId = :dacUserId and v.vote is null and v.has_concerns is false")
    List<Vote> findDataOwnerVotesByElectionsIdsAndUserId(@BindIn("electionIds") List<Integer> electionIds, @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId "
            + "where election.electionId = :electionId and v.type != 'CHAIRPERSON'")
    @Mapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId "
            + "where election.electionId = :electionId and v.type = :type")
    @Mapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.voteId = :voteId")
    Vote findVoteById(@Bind("voteId") Integer voteId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.type = 'DAC'")
    List<Vote> findDACVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select  *  from vote v where  v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIds(@BindIn("electionIds") List<Integer> electionIds);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionIds and v.type = :type")
    List<Vote> findVotesByElectionIdAndType(@Bind("electionIds") Integer electionIds, @Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.vote is null and v.type = 'DAC'")
    List<Vote> findPendingVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = 'DAC'")
    Vote findVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
            @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = :voteType")
    List<Vote> findVotesByElectionIdAndType(@Bind("electionId") Integer electionId,
            @Bind("dacUserId") Integer dacUserId,
            @Bind("voteType") String voteType);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.type = :type")
    Vote findVoteByElectionIdAndType(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = 'FINAL'")
    Vote findChairPersonVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
            @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select vote.voteId from vote  inner join election on election.electionId = vote.electionId  "
            + "where election.referenceId = :referenceId "
            + "and vote.voteId = :voteId")
    Integer checkVoteById(@Bind("referenceId") String referenceId,
            @Bind("voteId") Integer voteId);

    @SqlUpdate("insert into vote (dacUserId, electionId, type, reminderSent) values "
            + "(:dacUserId,:electionId, :type, :reminderSent)")
    @GetGeneratedKeys
    Integer insertVote(@Bind("dacUserId") Integer dacUserId,
            @Bind("electionId") Integer electionId,
            @Bind("type") String type,
            @Bind("reminderSent") Boolean reminderSent);

    @SqlUpdate("delete from vote where  voteId = :voteId")
    void deleteVoteById(@Bind("voteId") Integer voteId);

    @SqlUpdate("delete v from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId ")
    void deleteVotes(@Bind("referenceId") String referenceId);

    @SqlUpdate("update vote set vote = :vote,  updateDate = :updateDate,  rationale = :rationale, reminderSent = :reminderSent, createDate = :createDate, has_concerns = :hasConcerns where voteId = :voteId")
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

    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId  and v.dacUserId = :dacUserId and v.type = :voteType")
    Vote findVotesByReferenceIdTypeAndUser(@Bind("referenceId") String referenceId, @Bind("dacUserId") Integer dacUserId, @Bind("type") String voteType);

    @SqlQuery("select v.* from vote v where v.electionId = :electionId and v.type = :type")
    List<Vote> findVoteByTypeAndElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select count(*) from vote v inner join election e on v.electionId = e.electionId where e.electionType = :type and e.status = 'Closed' and "
            + " v.type = 'FINAL' and v.vote = :finalVote ")
    Integer findTotalFinalVoteByElectionTypeAndVote(@Bind("type") String type, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("SELECT MAX(c) FROM  ((SELECT  COUNT(vote) as c FROM vote  WHERE type = 'DAC' and electionId  IN (<electionIds>) GROUP BY electionId  ) as members)")
    Integer findMaxNumberOfDACMembers(@BindIn("electionIds") List<Integer> electionIds);

    @SqlBatch("insert into vote (dacUserId, electionId, type) values (:dacUserId,:electionId, :type)")
    void insertVotes(@Bind("dacUserId") List<Integer> dacUserIds, @Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlBatch("insert into vote (voteId, vote, dacUserId, createDate, updateDate, electionId, rationale, type, reminderSent, has_concerns) values (:voteId, :vote, :dacUserId, :createDate, :updateDate, :electionId, :rationale, :type, :isReminderSent, hasConcerns)")
    void batchVotesInsert(@BindBean("votes") List<Vote> dacUserIds);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and (v.vote is null and  (v.has_concerns = false || v.has_concerns is null)) and v.type = :type")
    List<Vote> findDataOwnerPendingVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlUpdate("update vote set dacUserId = :dacUserId, vote = null, createDate = null where voteId IN (<voteIds>)")
    void updateUserIdForVotes(@BindIn("voteIds") List<Integer> voteIds, @Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("delete from vote where voteId IN (<voteIds>)")
    void removeVotesById(@BindIn("voteIds") List<Integer> votesIds);

    @SqlQuery("select * from vote v where v.electionId IN (<electionIds>) and v.vote is null and v.dacUserId = :dacUserId and v.type = 'DAC' group by v.electionId")
    List<Vote> findVotesNotMadeForUser(@BindIn("electionIds") List<Integer> openElectionIdsForThisUser, @Bind("dacUserId") Integer dacUserI);

    @SqlQuery("select count(*) from vote v where v.electionId IN (<electionIds>) and v.type = 'DAC' group by v.electionId")
    List<Integer> findVoteCountForElections(@BindIn("electionIds") List<Integer> openElectionIdsForThisUser);

    @SqlUpdate("delete from vote v where v.dacUserId = :dacUserId and v.electionId IN (<electionIds>)")
    void removeVotes(@Bind("dacUserId") Integer dacUserId, @BindIn("electionIds") List<Integer> electionIds);

    /*
    This query moves all chairperson's votes on open elections to another DacUser, 
     */
    @SqlQuery(" update vote v set v.dacUserId = :toDacUserId, v.vote = null "
            + " where v.dacUserId = :fromDacUserId "
            + " and v.electionId IN (select e.electionId from election e where e.electionType != 'DataSet' "
            + " and (e.status = 'Closed' OR e.status = 'Final'))")
    public List<Integer> delegateChairPersonOpenElectionsVotes(@Bind("fromDacUserId") Integer fromDacUserId,
            @Bind("toDacUserId") Integer toDacUserId);

    @SqlQuery(" update vote v set v.dacUserId = :toDOUserId, v.vote = null "
            + " where v.dacUserId = :fromDOUserId "
            + " and v.electionId IN (select *  from election e where e.status = 'Open' and e.electionType = 'Dataset')")
    public List<Integer> delegateDatasetOpenElectionsVotes(@Bind("fromDOUserId") Integer fromDOUserId,
            @Bind("toDOUserId") Integer toDOUserId);

    /*
    This query returns all votes, null ot not, for a user on pending elections
    */
    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.electionId IN (select e.electionId from election e where e.electionType != 'DataSet' "
            + " and (e.status = 'Closed' OR e.status = 'Final'))")
    List<Vote> findVotesOnOpenElections(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.type = 'DAC' and v.vote = null "
            + " and v.electionId IN (select e.electionId from election e where e.electionType != 'DataSet' "
            + " and (e.status = 'Closed' OR e.status = 'Final'))")
    List<Vote> findCPVotesOnOpenElections(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.type = :voteType "
            + " and v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIdAndTypeAnduser(@BindIn("electionIds") List<Integer> electionIds, @Bind("voteType") String voteType, @Bind("dacUserId") Integer dacUserId);

    
}
