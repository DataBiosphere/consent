package org.genomebridge.consent.http.db;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.genomebridge.consent.http.models.Election;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;


@UseStringTemplate3StatementLocator
@RegisterMapper({ElectionMapper.class})
public interface ElectionDAO extends Transactional<ElectionDAO> {


    @SqlQuery("select electionId from election  where referenceId = :referenceId and status = 'Open'")
    Integer getOpenElectionIdByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from election  where referenceId = :referenceId")
    List<Election> findElectionsByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from election  where referenceId in (<referenceId>) ")
    Set<Election> findElectionsByReferenceId(@BindIn("referenceId") List<String> referenceId);

    @SqlUpdate("insert into election (electionType, finalVote, finalRationale, status, createDate,referenceId, finalAccessVote ) values " +
            "( :electionType, :finalVote, :finalRationale, :status, :createDate,:referenceId, :finalAccessVote)")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("finalVote") Boolean finalVote,
                           @Bind("finalRationale") String finalRationale,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("finalAccessVote") Boolean finalAccessVote);

    @SqlUpdate("delete  from election where electionId = :electionId")
    void deleteElectionById(@Bind("electionId") Integer electionId);

    @SqlUpdate("update election set finalVote = :finalVote, finalVoteDate = :finalVoteDate, finalRationale = :finalRationale, " +
            "status = :status, lastUpdate = :lastUpdate where electionId = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("finalVote") Boolean finalVote,
                            @Bind("finalVoteDate") Date finalVoteDate,
                            @Bind("finalRationale") String finalRationale,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate);

    @SqlUpdate("update election set status = :status where electionId in (<electionsId>) ")
    void updateElectionStatus(@BindIn("electionsId") List<Integer> electionsId,
                              @Bind("status") String status);


    @SqlUpdate("update election set finalAccessVote = true where electionId = :electionId ")
    void updateFinalAccessVote(@Bind("electionId") Integer electionId);


    @SqlUpdate("update election set lastUpdate = :lastUpdate where electionId in (<electionsId>) ")
    void bulkUpdateElectionLastUpdate(@BindIn("electionsId") List<Integer> electionsId,
                                      @Bind("lastUpdate") Date lastUpdate);


    @SqlQuery("select typeId from electiontype where type = :type")
    String findElectionTypeByType(@Bind("type") String type);

    @SqlQuery("select e.electionId, e.finalVote, e.status, e.createDate, e.referenceId, e.finalRationale, e.finalVoteDate,"
            + " e.lastUpdate, e.finalAccessVote, et.type electionType  from election e"
            + " inner join electiontype et on e.electionType = et.typeId and et.type = :type"
            + " and  e.referenceId = :referenceId and e.status = 'Open'")
    Election getOpenElectionByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select e.electionId,e.finalVote, e.status, e.createDate, e.referenceId, e.finalRationale,"
            + " e.finalVoteDate, e.lastUpdate, e.finalAccessVote, et.type electionType from election e"
            + " inner join electiontype et on e.electionType = et.typeId"
            + " and  e.electionId = :electionId")
    Election findElectionById(@Bind("electionId") Integer electionId);

    @SqlQuery("select * from election e where e.electionType = :type and e.status = :status order by createDate asc")
    List<Election> findElectionsByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);

    @SqlQuery("select e.electionId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.finalRationale, v.createDate finalVoteDate, " +
              "e.lastUpdate, e.finalAccessVote, e.electionType from election e inner join vote v  on v.electionId = e.electionId where e.electionType = 1 "+
              "and e.finalAccessVote is true  and v.type = 'FINAL'  and e.status = :status order by e.createDate asc")
    List<Election> findRequestElectionsWithFinalVoteByStatus(@Bind("status") String status);

    @SqlQuery("select * from election e where e.referenceId = :referenceId and e.status = :status order by createDate desc limit 1")
    Election findLastElectionByReferenceIdAndStatus(@Bind("referenceId") String referenceId, @Bind("status") String status);

    @SqlQuery("select * from election e where e.electionType = :type and e.finalAccessVote = :vote and e.status != 'Canceled' order by createDate asc")
    List<Election> findElectionsByTypeAndFinalAccessVoteChairPerson(@Bind("type") String type, @Bind("vote") Boolean finalAccessVote);

    @SqlQuery("select e.electionId from election e where e.electionType = :type and e.status = :status ")
    List<Integer> findElectionsIdByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);

    @SqlQuery("select count(*) from election e where e.electionType = :type and e.status = :status and "
              + "e.finalVote = :finalVote ")
    Integer findTotalElectionsByTypeStatusAndVote(@Bind("type") String type, @Bind("status") String status, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("select count(*) from election e where e.status = 'Open' ")
    Integer verifyOpenElections();

    @SqlQuery("select * from election e inner join (select referenceId, MAX(createDate) maxDate from election e group by referenceId) " +
              "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
              "AND e.referenceId in (<referenceIds>) ")
    List<Election> findLastElectionsByReferenceIdsAndType(@BindIn("referenceIds") List<String> referenceIds, @Bind("type") Integer type);

    @SqlQuery("select electionRPId from accessRp arp where arp.electionAccessId = :electionAccessId ")
    Integer findRPElectionByElectionAccessId(@Bind("electionAccessId") Integer electionAccessId);

    @SqlUpdate("insert into accessRp (electionAccessId, electionRPId ) values ( :electionAccessId, :electionRPId)")
    void insertAccessRP(@Bind("electionAccessId") Integer electionAccessId,
                        @Bind("electionRPId") Integer electionRPId);

    @SqlUpdate("delete  from accessRp where electionAccessId = :electionAccessId")
    void deleteAccessRP(@Bind("electionAccessId") Integer electionAccessId);

    void deleteElectionByType(String type);

    @SqlQuery("select electionAccessId from accessRp arp where arp.electionRPId = :electionRPId ")
    Integer findAccessElectionByElectionRPId(@Bind("electionRPId") Integer electionRPId);

}
