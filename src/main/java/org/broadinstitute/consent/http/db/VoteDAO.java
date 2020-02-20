package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.resources.Resource;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Date;
import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper({VoteMapper.class})
public interface VoteDAO extends Transactional<VoteDAO> {

    String OPEN = "Open";
    String CHAIRPERSON = Resource.CHAIRPERSON;
    String FINAL = "FINAL";
    String DAC = "DAC";
    String CLOSED = "Closed";
    String DATASET = "DataSet";

    @SqlQuery("select v.* from vote v inner join election on election.electionId = v.electionId  where election.referenceId = :referenceId and election.status = '"+ OPEN +"'")
    List<Vote> findVotesByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId "
            + "where election.electionId = :electionId and v.type != '" + CHAIRPERSON + "'")
    @Mapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select v.*, u.email, u.displayName from vote v inner join election on election.electionId = v.electionId inner join dacuser u on u.dacUserId = v.dacUserId "
            + "where election.electionId = :electionId and v.type = :type")
    @Mapper(ElectionReviewVoteMapper.class)
    List<ElectionReviewVote> findElectionReviewVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.voteId = :voteId")
    Vote findVoteById(@Bind("voteId") Integer voteId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.type = '" + DAC +"'")
    List<Vote> findDACVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select  *  from vote v where  v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIds(@BindIn("electionIds") List<Integer> electionIds);

    @SqlQuery("select  *  from vote v where  v.electionId IN (<electionIds>) and v.type = :voteType")
    List<Vote> findVotesByTypeAndElectionIds(@BindIn("electionIds") List<Integer> electionIds, @Bind("voteType") String type);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionIds and v.type = :type")
    List<Vote> findVotesByElectionIdAndType(@Bind("electionIds") Integer electionIds, @Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.vote is null and v.type = '" + DAC +"'")
    List<Vote> findPendingVotesByElectionId(@Bind("electionId") Integer electionId);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = '" + DAC +"'")
    Vote findVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
                                          @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select * from vote v where v.electionId = :electionId and v.dacUserId in (<dacUserIds>) and v.type = '" + DAC + "'")
    List<Vote> findVotesByElectionIdAndDACUserIds(@Bind("electionId") Integer electionId,
                                          @BindIn("dacUserIds") List<Integer> dacUserIds);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = :voteType")
    List<Vote> findVotesByElectionIdAndType(@Bind("electionId") Integer electionId,
                                            @Bind("dacUserId") Integer dacUserId,
                                            @Bind("voteType") String voteType);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.type = :type")
    Vote findVoteByElectionIdAndType(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select  *  from vote v where  v.electionId = :electionId and v.dacUserId = :dacUserId and v.type = '"+ FINAL +"'")
    Vote findChairPersonVoteByElectionIdAndDACUserId(@Bind("electionId") Integer electionId,
                                                     @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select vote.voteId from vote  inner join election on election.electionId = vote.electionId  "
            + "where election.referenceId = :referenceId "
            + "and vote.voteId = :voteId")
    Integer checkVoteById(@Bind("referenceId") String referenceId,
                          @Bind("voteId") Integer voteId);

    @SqlUpdate("insert into vote (dacUserId, electionId, type, reminderSent) values (:dacUserId, :electionId, :type, false)")
    @GetGeneratedKeys
    Integer insertVote(@Bind("dacUserId") Integer dacUserId,
                       @Bind("electionId") Integer electionId,
                       @Bind("type") String type);

    @SqlUpdate("delete from vote where  voteId = :voteId")
    void deleteVoteById(@Bind("voteId") Integer voteId);

    @SqlUpdate("delete from vote v where electionId in (select electionId from election where referenceId = :referenceId) ")
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

    @SqlQuery("select count(*) from vote v inner join election e on v.electionId = e.electionId where e.electionType = :type and e.status = '"+ CLOSED +"' and "
            + " v.type = 'FINAL' and v.vote = :finalVote ")
    Integer findTotalFinalVoteByElectionTypeAndVote(@Bind("type") String type, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("SELECT MAX(c) FROM  ((SELECT  COUNT(vote) as c FROM vote  WHERE type = '" + DAC + "' and electionId  IN (<electionIds>) GROUP BY electionId  ) as members)")
    Integer findMaxNumberOfDACMembers(@BindIn("electionIds") List<Integer> electionIds);

    @SqlBatch("insert into vote (dacUserId, electionId, type) values (:dacUserId,:electionId, :type)")
    void insertVotes(@Bind("dacUserId") List<Integer> dacUserIds, @Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlQuery("select * from vote v where v.electionId = :electionId and (v.vote is null and (v.has_concerns = false OR v.has_concerns is null)) and v.type = :type")
    List<Vote> findDataOwnerPendingVotesByElectionId(@Bind("electionId") Integer electionId, @Bind("type") String type);

    @SqlUpdate("delete from vote where electionId IN (<electionId>) and dacUserId = :userId")
    void removeVotesByElectionIdAndUser(@BindIn("electionId") List<Integer> electionId, @Bind("userId") Integer userId);

    @SqlUpdate(" update vote v set v.dacUserId = :toDOUserId, v.vote = null, v.createDate = null, v.updateDate = null"
            + " where v.dacUserId = :fromDOUserId "
            + " and v.electionId IN (<electionIds>)")
    void delegateDataSetOpenElectionsVotes(@Bind("fromDOUserId") Integer fromDOUserId, @BindIn("electionIds") List<Integer> electionIds,
                                                  @Bind("toDOUserId") Integer toDOUserId);

    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.electionId IN (select e.electionId from election e where e.electionType != '" + DATASET +"' "
            + " and (e.status = 'Open' OR e.status = 'Final'))")
    List<Vote> findVotesOnOpenElections(@Bind("dacUserId") Integer dacUserId);

    @SqlUpdate("delete from vote where voteId IN (<voteIds>)")
    void removeVotesByIds(@BindIn("voteIds") List<Integer> voteIds);

    @SqlQuery("select * from vote v where v.dacUserId = :dacUserId "
            + " and v.electionId IN (<electionIds>)")
    List<Vote> findVotesByElectionIdsAndUser(@BindIn("electionIds") List<Integer> electionIds, @Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select vote from vote v where v.electionId = :electionId and v.type = '"+ CHAIRPERSON +"'")
    Boolean findChairPersonVoteByElectionId(@Bind("electionId") Integer electionId);

}
