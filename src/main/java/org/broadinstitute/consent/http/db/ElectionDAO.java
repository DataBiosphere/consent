package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.mapper.AccessRPMapper;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DateMapper;
import org.broadinstitute.consent.http.db.mapper.ImmutablePairOfIntsMapper;
import org.broadinstitute.consent.http.db.mapper.SimpleElectionMapper;
import org.broadinstitute.consent.http.db.mapper.ElectionMapper;
import org.broadinstitute.consent.http.models.AccessRP;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(ElectionMapper.class)
public interface ElectionDAO extends Transactional<ElectionDAO> {

    String CANCELED = "Canceled";

    @SqlQuery("select electionId from election where referenceId = :referenceId and lower(status) = 'open'")
    Integer getOpenElectionIdByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select  e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, " +
            " e.lastUpdate, e.finalAccessVote, e.electionType,  e.dataUseLetter, e.dulName, e.archived, e.version from election e inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' where referenceId = :referenceId")
    List<Election> findElectionsWithFinalVoteByReferenceId(@Bind("referenceId") String referenceId);

    @SqlUpdate("insert into election (electionType, status, createDate, referenceId, datasetId) values " +
            "( :electionType, :status, :createDate,:referenceId, :datasetId)")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("datasetId") Integer dataSetId);

    @SqlUpdate("insert into election (electionType, status, createDate,referenceId, finalAccessVote, dataUseLetter, dulName, datasetId, version) values " +
            "(:electionType, :status, :createDate,:referenceId, :finalAccessVote, :dataUseLetter, :dulName, :datasetId, " +
            " (SELECT coalesce (MAX(version), 0) + 1 FROM election AS electionVersion  where referenceId = :referenceId))")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("finalAccessVote") Boolean finalAccessVote,
                           @Bind("dataUseLetter") String dataUseLetter,
                           @Bind("dulName") String dulName,
                           @Bind("datasetId") Integer dataSetId);

    @SqlUpdate("delete  from election where electionId = :electionId")
    void deleteElectionById(@Bind("electionId") Integer electionId);

    @SqlUpdate("update election set status = :status, lastUpdate = :lastUpdate where electionId = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate);

    @SqlUpdate("update election set status = :status where electionId in (<electionsId>) ")
    void updateElectionStatus(@BindList("electionsId") List<Integer> electionsId,
                              @Bind("status") String status);

    @SqlUpdate("update election set finalAccessVote = true where electionId = :electionId ")
    void updateFinalAccessVote(@Bind("electionId") Integer electionId);

    @SqlUpdate("update election set status = '" + CANCELED + "' where referenceId in (<referenceId>) and lower(status) = 'open' and lower(electiontype) = lower(:electionType)")
    void bulkCancelOpenElectionByReferenceIdAndType(@Bind("electionType") String electionType,
                                                    @BindList("referenceId") List<String> referenceId);

    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            + "e.lastUpdate, e.finalAccessVote, e.electionType,  e.dataUseLetter, e.dulName, e.archived, e.version   from election e"
            + " inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' "
            + " where e.referenceId = :referenceId and lower(e.status) = 'open' and lower(e.electionType) = lower(:type)")
    Election getOpenElectionWithFinalVoteByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            + "e.lastUpdate, e.finalAccessVote, e.electionType, e.dataUseLetter, e.dulName, e.archived, e.version  from election e"
            + " inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' "
            + " where e.referenceId = :referenceId and lower(e.electionType) = lower(:type) and e.version = (select MAX(version) from election where referenceId = :referenceId)")
    Election getElectionWithFinalVoteByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            + " e.lastUpdate, e.finalAccessVote, e.electionType,  e.dataUseLetter, e.dulName, e.archived, e.version  from election e"
            + " left join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' "
            + " where  e.electionId = :electionId")
    Election findElectionWithFinalVoteById(@Bind("electionId") Integer electionId);

    @SqlQuery("select e.* from election e inner join vote v on v.electionId = e.electionId where  v.voteId = :voteId")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findElectionByVoteId(@Bind("voteId") Integer voteId);

    @SqlQuery("select distinct e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            + "e.lastUpdate, e.finalAccessVote, e.electionType,  e.dataUseLetter, e.dulName, e.archived, e.version from election e "
            + "inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' "
            + "where lower(e.electionType) = lower(:type) and lower(e.status) = lower(:status) order by createDate asc")
    List<Election> findElectionsWithFinalVoteByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);

    @SqlQuery("select distinct e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            +" e.lastUpdate, e.finalAccessVote, e.electionType, e.dataUseLetter, e.dulName, e.archived, e.version  from election e inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' "
            +" inner join (select referenceId, MAX(createDate) maxDate from election e where  e.electionType = :type  group by referenceId) "
            +" electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId"
            +" AND lower(e.electionType) = lower(:type)  order by createDate asc")
    List<Election> findLastElectionsWithFinalVoteByType(@Bind("type") String type);

    @SqlQuery(" SELECT DISTINCT e.electionid, e.datasetid, v.vote finalvote, e.status, e.createdate, e.referenceid, " +
            "   v.rationale finalrationale, v.createdate finalvotedate, " +
            "   e.lastupdate, e.finalaccessvote, e.electiontype, e.datauseletter, e.dulname, e.archived, e.version " +
            " FROM election e " +
            " INNER JOIN vote v ON v.electionid = e.electionid " +
            " INNER JOIN " +
            "   (SELECT e2.referenceid, MAX(e2.createDate) maxdate " +
            "    FROM election e2 " +
            "    WHERE LOWER(e2.electiontype) = 'dataaccess' " +
            "    AND LOWER(e2.status) = LOWER(:status) " +
            "    GROUP BY e2.referenceid) electionview " +
            "       ON electionview.maxdate = e.createdate " +
            "       AND electionview.referenceid = e.referenceid" +
            " WHERE LOWER(e.electiontype) = 'dataaccess' " +
            " AND e.finalAccessVote IS true " +
            " AND LOWER(v.type) = 'final' " +
            " AND LOWER(e.status) = LOWER(:status) " +
            " ORDER BY e.createdate ASC ")
    List<Election> findLastDataAccessElectionsWithFinalVoteByStatus(@Bind("status") String status);

    @SqlQuery("select e.electionId,  e.datasetId, e.lastUpdate, v.vote finalVote, e.finalAccessVote, e.status, e.createDate, e.referenceId, e.electionType, " +
            "v.rationale finalRationale, v.createDate finalVoteDate, e.dataUseLetter, e.dulName, e.archived, e.version  from election e inner join " +
            "vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' where e.referenceId = :referenceId  " +
            "and lower(e.status) in (<status>) order by createDate desc limit 1")
    Election findLastElectionWithFinalVoteByReferenceIdAndStatus(@Bind("referenceId") String referenceId, @BindList("status") List<String> status);

    @SqlQuery("SELECT DISTINCT e.electionId, e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, " +
            "       v.rationale finalRationale, v.createDate finalVoteDate, e.lastUpdate, e.finalAccessVote, " +
            "       e.electionType, e.dataUseLetter, e.dulName, e.archived, e.version " +
            " FROM election e " +
            " INNER JOIN vote v ON v.electionId = e.electionId AND LOWER(v.type) = 'chairperson' " +
            " INNER JOIN (SELECT referenceId, MAX(createDate) maxDate FROM election e WHERE LOWER(e.electionType) = LOWER(:type) GROUP BY referenceId) electionView " +
            "     ON electionView.maxDate = e.createDate " +
            "     AND electionView.referenceId = e.referenceId " +
            "     AND LOWER(e.electionType) = LOWER(:type) " +
            "     AND e.finalAccessVote = :vote " +
            "     AND LOWER(e.status) not in ('canceled', 'closed') " +
            " ORDER BY createDate ASC")
    List<Election> findOpenLastElectionsByTypeAndFinalAccessVoteForChairPerson(@Bind("type") String type, @Bind("vote") Boolean finalAccessVote);

    @SqlQuery("select count(*) from election e inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' where lower(e.electionType) = lower(:type) and lower(e.status) = lower(:status) and " +
            " v.vote = :finalVote ")
    Integer findTotalElectionsByTypeStatusAndVote(@Bind("type") String type, @Bind("status") String status, @Bind("finalVote") Boolean finalVote);

    @SqlQuery("select count(*) from election e where (lower(e.status) = 'open' or lower(e.status) = 'final') and lower(e.electionType) != 'dataset' ")
    Integer verifyOpenElections();

    @SqlQuery("select distinct e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, " +
            "v.createDate finalVoteDate, e.lastUpdate, e.finalAccessVote, e.electionType, e.dataUseLetter, e.dulName, e.archived, e.version  from election e " +
            "inner join (select referenceId, MAX(createDate) maxDate from election e where e.electionType = :type group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId in (<referenceIds>) AND lower(e.electionType) = lower(:type) " +
            "left join vote v on v.electionId = e.electionId and lower(v.type) = 'final' ")
    List<Election> findLastElectionsWithFinalVoteByReferenceIdsAndType(@BindList("referenceIds") List<String> referenceIds, @Bind("type") String type);

    @SqlQuery("select distinct e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, " +
            "v.createDate finalVoteDate, e.lastUpdate, e.finalAccessVote, e.electionType, e.dataUseLetter, e.dulName, e.archived, e.version  from election e " +
            "inner join (select referenceId, MAX(createDate) maxDate from election e where lower(e.status) = lower(:status) group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId in (<referenceIds>) " +
            "left join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' ")
    List<Election> findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(@BindList("referenceIds") List<String> referenceIds, @Bind("status") String status);

    @SqlQuery("select distinct e.* " +
            "from election e " +
            "inner join (select referenceId, MAX(createDate) maxDate from election e where e.electionType = :type group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId ")
    @UseRowMapper(SimpleElectionMapper.class)
    List<Election> findLastElectionsByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select count(*) from election e where lower(e.status) = 'open' and e.referenceId = :referenceId")
    Integer verifyOpenElectionsForReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from election e inner join (select referenceId, MAX(createDate) maxDate from election e where lower(e.status) = lower(:status) group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId ")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findLastElectionByReferenceIdAndStatus(@Bind("referenceId") String referenceIds, @Bind("status") String status);

    @SqlQuery("select * from election e inner join (select referenceId, MAX(createDate) maxDate from election e where lower(e.electionType) = lower(:type) group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId ")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findLastElectionByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("select electionRPId from access_rp arp where arp.electionAccessId = :electionAccessId ")
    Integer findRPElectionByElectionAccessId(@Bind("electionAccessId") Integer electionAccessId);

    @RegisterRowMapper(ImmutablePairOfIntsMapper.class)
    @SqlQuery(" select electionRPId, electionAccessId from access_rp arp where arp.electionAccessId in (<electionAccessIds>) ")
    List<Pair<Integer, Integer>> findRpAccessElectionIdPairs(@BindList("electionAccessIds") List<Integer> electionAccessIds);

    @SqlUpdate("insert into access_rp (electionAccessId, electionRPId ) values (:electionAccessId, :electionRPId)")
    void insertAccessRP(@Bind("electionAccessId") Integer electionAccessId,
                        @Bind("electionRPId") Integer electionRPId);

    @SqlUpdate("insert into accesselection_consentelection (access_election_id, consent_election_id ) values (:electionAccessId, :electionConsentId)")
    void insertAccessAndConsentElection(@Bind("electionAccessId") Integer electionAccessId,
                        @Bind("electionConsentId") Integer electionConsentId);

    @SqlQuery("select consent_election_id from accesselection_consentelection where access_election_id = :electionAccessId ")
    Integer getElectionConsentIdByDARElectionId(@Bind("electionAccessId") Integer electionAccessId);

    @RegisterRowMapper(AccessRPMapper.class)
    @SqlQuery("select * from access_rp where electionAccessId in (<electionAccessIds>) ")
    List<AccessRP> findAccessRPbyElectionAccessId(@BindList("electionAccessIds") List<Integer> electionAccessIds);

    @SqlUpdate("delete  from access_rp where electionAccessId = :electionAccessId")
    void deleteAccessRP(@Bind("electionAccessId") Integer electionAccessId);

    @SqlQuery("select electionAccessId from access_rp arp where arp.electionRPId = :electionRPId ")
    Integer findAccessElectionByElectionRPId(@Bind("electionRPId") Integer electionRPId);

    @SqlQuery("select distinct e.electionId, e.datasetId,  v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            + " e.lastUpdate, e.finalAccessVote, e.electionType,  e.dataUseLetter, e.dulName, e.archived, e.version from election e inner join vote v on v.electionId = e.electionId and lower(v.type) = 'chairperson' "
            + " where lower(e.electionType) = lower(:type)  and lower(e.status) in (<status>) order by createDate asc")
    List<Election> findElectionsWithFinalVoteByTypeAndStatus(@Bind("type") String type, @BindList("status") List<String> status);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("select distinct * from election e where  e.electionId in  (<electionIds>)")
    List<Election> findElectionsByIds(@BindList("electionIds") List<Integer> electionIds);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("select * from election e where e.electionId = :electionId")
    Election findElectionById(@Bind("electionId") Integer electionId);

    @SqlQuery("select electionId from election  where referenceId = :referenceId and lower(status) = 'open' and datasetId = :dataSetId")
    Integer getOpenElectionByReferenceIdAndDataSet(@Bind("referenceId") String referenceId, @Bind("dataSetId") Integer dataSetId);

    @SqlQuery("select datasetId from election where electionId = :electionId ")
    Integer getDatasetIdByElectionId(@Bind("electionId") Integer electionId);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("select distinct * from election where lower(status) = lower(:status) and lower(electionType) = lower(:electionType)")
    List<Election> getElectionByTypeAndStatus(@Bind("electionType") String electionType, @Bind("status") String status);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("select distinct * from election  where lower(status) = lower(:status) and lower(electionType) = lower(:electionType) and referenceId = :referenceId")
    List<Election> getElectionByTypeStatusAndReferenceId(@Bind("electionType") String electionType, @Bind("status") String status, @Bind("referenceId") String referenceId);

    @SqlUpdate("update election set status = :status, lastUpdate = :lastUpdate, finalAccessVote = :finalAccessVote where electionId = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate,
                            @Bind("finalAccessVote") Boolean finalAccessVote);

    @SqlQuery("SELECT v.electionId FROM vote v, election e where v.dacUserId = :dacUserId and e.electionId = v.electionId and v.vote is null AND lower(e.electionType) = 'dataset' AND lower(e.status) = 'open'")
    List<Integer> findDataSetOpenElectionIds(@Bind("dacUserId")Integer dacUserId);

    @SqlQuery("select distinct e.electionId,  e.datasetId, v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, " +
            "e.lastUpdate, e.finalAccessVote, e.electionType e.dataUseLetter, e.dulName, e.archived, e.version from election e inner join vote v  on v.electionId = e.electionId where lower(e.electionType) = 'dataaccess' "+
            " and lower(v.type) = 'final'  and e.referenceId in (<darIds>) order by e.createDate asc")
    List<Election> findRequestElectionsByReferenceIds(@BindList("darIds") List<String> darIds);

    @SqlUpdate("update election set archived = true, lastUpdate = :lastUpdate where electionId = :electionId ")
    void archiveElectionById(@Bind("electionId") Integer electionId, @Bind("lastUpdate") Date lastUpdate);

    @SqlQuery("select distinct e.electionId, e.datasetId,  v.vote finalVote, e.status, e.createDate, e.referenceId, v.rationale finalRationale, v.createDate finalVoteDate, "
            + " e.lastUpdate, e.finalAccessVote, e.electionType,  e.dataUseLetter, e.dulName, e.archived, e.version from election e inner join vote v on v.electionId = e.electionId and lower(v.type) = 'final' "
            + " where v.vote = :isApproved and lower(e.electionType) = 'dataaccess' order by createDate asc")
    List<Election> findDataAccessClosedElectionsByFinalResult(@Bind("isApproved") Boolean isApproved);

    @SqlQuery("select  MAX(v.createDate) createDate from election e inner join vote v on v.electionId = e.electionId and lower(v.type) = 'final' " +
            "where v.vote = true and lower(e.electionType) = 'dataaccess' and referenceId = :referenceId GROUP BY e.createDate")
    @UseRowMapper(DateMapper.class)
    Date findApprovalAccessElectionDate(@Bind("referenceId") String referenceId);

    @SqlQuery("select * from election e inner join (select referenceId, MAX(createDate) maxDate from election e where e.status = 'Closed' group by referenceId) " +
            "electionView ON electionView.maxDate = e.createDate AND electionView.referenceId = e.referenceId  " +
            "AND e.referenceId = :referenceId " +
            "inner join vote v on v.electionId = e.electionId and v.vote = true  and lower(v.type) = 'chairperson' ")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findDULApprovedElectionByReferenceId(@Bind("referenceId") String referenceId);

    /**
     * Find the Dac for this election. Looks across associations to a dac via dataset and those
     * associated via the consent.
     *
     * @param electionId The election id
     * @return Dac for this election
     */
    @UseRowMapper(DacMapper.class)
    @SqlQuery("select d0.* from ( " +
            "   select d1.*, e1.electionId from dac d1 " +
            "     inner join consents c1 on d1.dac_id = c1.dac_id " +
            "     inner join consentassociations a1 on a1.consentId = c1.consentId " +
            "     inner join election e1 on e1.datasetId = a1.dataSetId and e1.electionId = :electionId " +
            " union " +
            "   select d2.*, e2.electionId from dac d2 " +
            "     inner join consents c2 on d2.dac_id = c2.dac_id " +
            "     inner join election e2 on e2.referenceId = c2.consentId and e2.electionId = :electionId " +
            " ) as d0 limit 1 ") // `select * from (...) limit 1` syntax is an hsqldb limitation
    Dac findDacForElection(@Bind("electionId") Integer electionId);

    /**
     * Find the OPEN elections that belong to this Dac
     *
     * @param dacId The Dac id
     * @return List of elections associated to the Dac
     */
    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("select e1.* from election e1 " +
            "   inner join consentassociations a1 on a1.dataSetId = e1.datasetId " +
            "   inner join consents c1 on c1.consentId = a1.consentId and c1.dac_id = :dacId " +
            "   where lower(e1.status) = 'open' " +
            " union " +
            " select e2.* from election e2 " +
            "   inner join consents c2 on c2.consentId = e2.referenceId and c2.dac_id = :dacId " +
            "   where lower(e2.status) = 'open' ")
    List<Election> findOpenElectionsByDacId(@Bind("dacId") Integer dacId);

}
