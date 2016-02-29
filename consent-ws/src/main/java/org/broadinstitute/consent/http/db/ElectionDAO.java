package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.models.AccessRP;
import org.broadinstitute.consent.http.models.Election;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

@UseStringTemplate3StatementLocator
@RegisterMapper({ElectionMapper.class})
public interface ElectionDAO extends Transactional<ElectionDAO> {

    String CHAIRPERSON = "CHAIRPERSON";
    String FINAL = "FINAL";

    @SqlQuery("select electionId from election  where referenceId = :referenceId and status = 'Open'")
    Integer getOpenElectionIdByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select  e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, " +
             " e.lastUpdate, e.finalAccessVote, e.electionType from election e inner join vote v on v.electionId = e.electionId and v.type = '"+ CHAIRPERSON +"' where referenceId = :referenceId")
    List<Election> findElectionsWithFinalVoteByReferenceId(@Bind("referenceId") String referenceId);

    @SqlUpdate("insert into election (electionType, status, createDate,referenceId, finalAccessVote, useRestriction, translatedUseRestriction) values " +
            "( :electionType, :status, :createDate,:referenceId, :finalAccessVote, :useRestriction, :translatedUseRestriction)")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("finalAccessVote") Boolean finalAccessVote,
                           @Bind("useRestriction") String useRestriction,
                           @Bind("translatedUseRestriction") String translatedUseRestriction);

    @SqlUpdate("insert into election (electionType, status, createDate, referenceId, datasetId) values " +
            "( :electionType, :status, :createDate,:referenceId, :datasetId)")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("datasetId") Integer dataSetId);

    @SqlUpdate("delete  from election where electionId = :electionId")
    void deleteElectionById(@Bind("electionId") Integer electionId);

    @SqlUpdate("update election set status = :status, lastUpdate = :lastUpdate where electionId = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate);

    @SqlUpdate("update election set status = :status where electionId in (<electionsId>) ")
    void updateElectionStatus(@BindIn("electionsId") List<Integer> electionsId,
                              @Bind("status") String status);

    @SqlUpdate("update election set status = :newStatus where referenceId in (<referenceId>) and status = :status")
    void updateElectionStatusByReferenceAndStatus(@BindIn("referenceId") List<String> referenceId,
                              @Bind("status") String status,
                              @Bind("newStatus") String newStatus);


    @SqlUpdate("update election set finalAccessVote = true where electionId = :electionId ")
    void updateFinalAccessVote(@Bind("electionId") Integer electionId);


    @SqlUpdate("update election set lastUpdate = :lastUpdate where electionId in (<electionsId>) ")
    void bulkUpdateElectionLastUpdate(@BindIn("electionsId") List<Integer> electionsId,
                                      @Bind("lastUpdate") Date lastUpdate);


    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, "
            +  "e.lastUpdate, e.finalAccessVote, e.electionType  from election e"
            + " inner join vote v on v.electionId = e.electionId and v.type = '"+ CHAIRPERSON
            + "'  where   e.referenceId = :referenceId and e.status = 'Open' and e.electionType = :type")
    Election getOpenElectionWithFinalVoteByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, "
            + " e.lastUpdate, e.finalAccessVote, e.electionType  from election e"
            + " left join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON
            + "' where  e.electionId = :electionId")
    Election findElectionWithFinalVoteById(@Bind("electionId") Integer electionId);

    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, "
            + "e.lastUpdate, e.finalAccessVote, e.electionType   from election e "
            + "inner join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON
            + "' where e.electionType = :type and e.status = :status order by createDate asc")
    List<Election> findElectionsWithFinalVoteByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);


    @SqlQuery("select * from election where electiontype = :electionType and status = 'Open' and datediff(NOW(), createDate) > :amountOfDays")
    @Mapper(DatabaseElectionMapper.class)
    List<Election> findExpiredElections(@Bind("electionType") String electionType, @Bind("amountOfDays")Integer amountOfDays);


    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, "
            +" e.lastUpdate, e.finalAccessVote, e.electionType from election e inner join vote v on v.electionId = e.electionId and v.type = '"  + CHAIRPERSON
            +"' inner join (select referenceId, MAX(createDate) maxDate from election e where  e.electionType = :type  group by referenceId) "
            +" electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId"
            +" AND e.electionType = :type  order by createDate asc")
    List<Election> findLastElectionsWithFinalVoteByType(@Bind("type") String type);

    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, " +
            "e.lastUpdate, e.finalAccessVote, e.electionType from election e inner join vote v  on v.electionId = e.electionId where e.electionType = 'DataAccess' "+
            "and e.finalAccessVote is true  and v.type = 'FINAL'  and e.status = :status order by e.createDate asc")
    List<Election> findRequestElectionsWithFinalVoteByStatus(@Bind("status") String status);

    @SqlQuery("select e.electionId,  e.datasetId, e.lastUpdate, v.vote finalVote, e.finalAccessVote, e.translatedUseRestriction, e.useRestriction, e.status, e.createDate, e.referenceId, e.electionType, " +
            "v.rationale finalRationale, v.createDate finalVoteDate from election e inner join " +
            "vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON +"' where e.referenceId = :referenceId  " +
            "and e.status in (<status>) order by createDate desc limit 1")
    Election findLastElectionWithFinalVoteByReferenceIdAndStatus(@Bind("referenceId") String referenceId, @BindIn("status") List<String> status);

    @SqlQuery("select  e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, "
            + " e.lastUpdate, e.finalAccessVote, e.electionType from election e inner join vote v  on v.electionId = e.electionId and v.type = '" + CHAIRPERSON
            + "' where e.electionType = :type and e.finalAccessVote = :vote and e.status != 'Canceled' order by createDate asc")
    List<Election> findElectionsByTypeAndFinalAccessVoteChairPerson(@Bind("type") String type, @Bind("vote") Boolean finalAccessVote);

    @SqlQuery("select e.electionId from election e where e.electionType = :type and e.status = :status ")
    List<Integer> findElectionsIdByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);


    @SqlQuery("select count(*) from election e inner join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON + "' where e.electionType = :type and e.status = :status and " +
            " v.vote = :finalVote ")
    Integer findTotalElectionsByTypeStatusAndVote(@Bind("type") String type, @Bind("status") String status, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("select count(*) from election e where (e.status = 'Open' or e.status = 'Final') and e.electionType != 'DataSet'")
    Integer verifyOpenElections();

    @SqlQuery("select  e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, " +
            "v.createDate finalVoteDate, e.lastUpdate, e.finalAccessVote, e.electionType from election e " +
            "inner join (select referenceId, MAX(createDate) maxDate from election e where e.electionType = :type group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId in (<referenceIds>) AND e.electionType = :type " +
            "left join vote v on v.electionId = e.electionId and v.type = '" + FINAL + "'")
    List<Election> findLastElectionsWithFinalVoteByReferenceIdsAndType(@BindIn("referenceIds") List<String> referenceIds, @Bind("type") String type);

    @SqlQuery("select  e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, " +
            "v.createDate finalVoteDate, e.lastUpdate, e.finalAccessVote, e.electionType from election e " +
            "inner join (select referenceId, MAX(createDate) maxDate from election e where e.status = :status group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId in (<referenceIds>) " +
            "left join vote v on v.electionId = e.electionId and v.type = '" + CHAIRPERSON + "'")
    List<Election> findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(@BindIn("referenceIds") List<String> referenceIds, @Bind("status") String status);

    @SqlQuery("select  e.* " +
            "from election e " +
            "inner join (select referenceId, MAX(createDate) maxDate from election e where e.electionType = :type group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId ")
    @Mapper(DatabaseElectionMapper.class)
    List<Election> findLastElectionsByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select count(*) from election e where e.status = 'Open' and e.referenceId = :referenceId")
    Integer verifyOpenElectionsForReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from election e inner join (select referenceId, MAX(createDate) maxDate from election e where e.status = :status group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId ")
    @Mapper(DatabaseElectionMapper.class)
    Election findLastElectionByReferenceIdAndStatus(@Bind("referenceId") String referenceIds, @Bind("status") String status);

    @SqlQuery("select * from election e inner join (select referenceId, MAX(createDate) maxDate from election e where e.electionType = :type group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId ")
    @Mapper(DatabaseElectionMapper.class)
    Election findLastElectionByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select electionRPId from access_rp arp where arp.electionAccessId = :electionAccessId ")
    Integer findRPElectionByElectionAccessId(@Bind("electionAccessId") Integer electionAccessId);

    @SqlUpdate("insert into access_rp (electionAccessId, electionRPId ) values ( :electionAccessId, :electionRPId)")
    void insertAccessRP(@Bind("electionAccessId") Integer electionAccessId,
                        @Bind("electionRPId") Integer electionRPId);

    @RegisterMapper({AccessRPMapper.class})
    @SqlQuery("select * from access_rp where electionAccessId in (<electionAccessIds>) ")
    List<AccessRP> findAccessRPbyElectionAccessId(@BindIn("electionAccessIds") List<Integer> electionAccessIds);

    @SqlUpdate("delete  from access_rp where electionAccessId = :electionAccessId")
    void deleteAccessRP(@Bind("electionAccessId") Integer electionAccessId);

    @SqlQuery("select electionAccessId from access_rp arp where arp.electionRPId = :electionRPId ")
    Integer findAccessElectionByElectionRPId(@Bind("electionRPId") Integer electionRPId);

    @SqlQuery("select e.electionId, e.datasetId,  v.vote finalVote, e.status, e.createDate, e.referenceId, e.useRestriction, e.translatedUseRestriction, v.rationale finalRationale, v.createDate finalVoteDate, "
            + " e.lastUpdate, e.finalAccessVote, e.electionType from election e inner join vote v on v.electionId = e.electionId and v.type = '"  + CHAIRPERSON
            + "' where e.electionType = :type  and e.status in  (<status>) order by createDate asc")
    List<Election> findElectionsWithFinalVoteByTypeAndStatus(@Bind("type") String type, @BindIn("status") List<String> status);

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select *  from election e where  e.electionId in  (<electionIds>)")
    List<Election> findElectionsByIds(@BindIn("electionIds") List<Integer> electionIds);

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select *  from election e where e.status = 'Open' and e.electionType = :electionType")
    List<Election> getOpenElections(@Bind("electionType")String electionType);

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select *  from election e where e.status = 'Open' and e.electionType != 'DataSet'")
    List<Election> getOpenElectionsForMember();

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select *  from election e where (e.status = 'Open' or e.status = 'Final') and e.electionType = :electionType")
    List<Election> getOpenAndFinalElections(@Bind("electionType")String electionType);

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select *  from election e where  e.electionId = :electionId")
    Election findElectionById(@Bind("electionId") Integer electionId);

    @SqlQuery("select electionId from election  where referenceId = :referenceId and status = 'Open' and datasetId = :dataSetId")
    Integer getOpenElectionByReferenceIdAndDataSet(@Bind("referenceId") String referenceId, @Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select datasetId from election  where electionId = :electionId ")
    Integer getDatasetIdByElectionId(@Bind("electionId") Integer electionId);

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select * from election  where  status = :status and  electionType = :electionType")
    List<Election> getElectionByTypeAndStatus(@Bind("electionType") String electionType, @Bind("status") String status);

    @Mapper(DatabaseElectionMapper.class)
    @SqlQuery("select * from election  where  status = :status and  electionType = :electionType and referenceId = :referenceId")
    List<Election> getElectionByTypeStatusAndReferenceId(@Bind("electionType") String electionType, @Bind("status") String status, @Bind("referenceId") String referenceId);

    @SqlUpdate("update election set status = :status, lastUpdate = :lastUpdate, finalAccessVote = :finalAccessVote where electionId = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate,
                            @Bind("finalAccessVote") Boolean finalAccessVote);

    @SqlQuery("SELECT e.electionId FROM election e WHERE e.status = 'Open' AND e.electionType != 'DataSet' AND EXISTS (SELECT * FROM vote v WHERE e.electionId = v.electionId AND v.dacUserId = :dacUserId)")
    List<Integer> findOpenElectionIdsForMember(@Bind("dacUserId")Integer dacUserId);

    @SqlQuery("SELECT e.electionId FROM election e WHERE e.status = 'Open' AND e.electionType = 'DataSet' AND EXISTS (SELECT * FROM vote v WHERE e.electionId = v.electionId AND v.dacUserId = :dacUserId)")
    List<Integer> findOpenDatasetElectionIdsForMember(@Bind("dacUserId")Integer dacUserId);

    @SqlQuery("SELECT e.electionId FROM election e WHERE e.status = 'Open' AND e.electionType != 'DataSet' AND EXISTS (SELECT * FROM vote v WHERE e.electionId = v.electionId AND v.dacUserId = :dacUserId)")
    List<Integer> findOpenElectionIdsForMemberMinusDataset(@Bind("dacUserId")Integer dacUserId);

}
