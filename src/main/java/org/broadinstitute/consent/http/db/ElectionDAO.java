package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DateMapper;
import org.broadinstitute.consent.http.db.mapper.ElectionMapper;
import org.broadinstitute.consent.http.db.mapper.ImmutablePairOfIntsMapper;
import org.broadinstitute.consent.http.db.mapper.SimpleElectionMapper;
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

@RegisterRowMapper(ElectionMapper.class)
public interface ElectionDAO extends Transactional<ElectionDAO> {

    @SqlQuery(
        "SELECT election_id " +
            "FROM election " +
            "WHERE reference_id = :referenceId " +
                "AND LOWER(status) = 'open'")
    Integer getOpenElectionIdByReferenceId(@Bind("referenceId") String referenceId);

    @SqlQuery(
        "SELECT DISTINCT election_id " +
            "FROM election " +
            "WHERE reference_id IN (<referenceIds>)")
    List<Integer> getElectionIdsByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

    @SqlQuery(
        "SELECT e.election_id, e.dataset_id, v.vote final_vote , e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            " e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
            "FROM election e " +
            "INNER JOIN vote v ON v.electionId = e.election_id AND LOWER(v.type) = 'chairperson' " +
            "WHERE reference_id = :referenceId")
    List<Election> findElectionsWithFinalVoteByReferenceId(@Bind("referenceId") String referenceId);

    @SqlUpdate("insert into election (electionType, status, createDate, referenceId, datasetId) values " +
            "( :electionType, :status, :createDate,:referenceId, :datasetId)")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("datasetId") Integer dataSetId);

    @SqlUpdate(
            " INSERT INTO election (electiontype, status, createdate, referenceid, finalaccessvote, datauseletter, dulname, datasetid, version) " +
            " VALUES (:electionType, :status, :createDate,:referenceId, :finalAccessVote, :dataUseLetter, :dulName, :datasetId, " +
            "        (SELECT COALESCE (MAX(version), 0) + 1 FROM election WHERE referenceid = :referenceId AND electiontype = :electionType AND datasetid = :datasetId)) ")
    @GetGeneratedKeys
    Integer insertElection(@Bind("electionType") String electionType,
                           @Bind("status") String status,
                           @Bind("createDate") Date createDate,
                           @Bind("referenceId") String referenceId,
                           @Bind("finalAccessVote") Boolean finalAccessVote,
                           @Bind("dataUseLetter") String dataUseLetter,
                           @Bind("dulName") String dulName,
                           @Bind("datasetId") Integer datasetId);

    @SqlUpdate("DELETE FROM election WHERE election_id = :electionId")
    void deleteElectionById(@Bind("electionId") Integer electionId);

    @SqlUpdate("DELETE FROM election WHERE election_id in (<electionIds>)")
    void deleteElectionsByIds(@BindList("electionIds") List<Integer> electionIds);

    @SqlUpdate("UPDATE election SET status = :status, last_update = :lastUpdate WHERE election_id = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate);

    @SqlUpdate("UPDATE election SET status = :status WHERE election_id IN (<electionIds>) ")
    void updateElectionStatus(@BindList("electionIds") List<Integer> electionIds,
                              @Bind("status") String status);

    @SqlQuery(
        "SELECT e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            "e.last_update, e.final_access_vote, e.election_type,  e.data_use_letter, e.dul_name, e.archived, e.version " +
            "FROM election e " +
            "INNER JOIN vote v ON v.electionId = e.election_id AND LOWER(v.type) = 'chairperson' " +
            "WHERE e.reference_id = :referenceId " +
                "AND LOWER(e.status) = 'open' " +
                "AND LOWER(e.election_type) = LOWER(:type)")
    Election getOpenElectionWithFinalVoteByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery(
        "SELECT e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            "e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
            "FROM election e " +
            "INNER JOIN vote v ON v.electionId = e.election_id AND LOWER(v.type) = 'chairperson' " +
            "WHERE e.reference_id = :referenceId " +
                "AND LOWER(e.election_type) = LOWER(:type) " +
            "ORDER BY create_date DESC LIMIT 1")
    Election getElectionWithFinalVoteByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery("SELECT DISTINCT "
          + "    e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, "
          + "    e.reference_id, v.rationale final_rationale, v.createdate final_vote_date, "
          + "    e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, "
          + "    e.archived, e.version "
          + "FROM election e "
          + "LEFT JOIN vote v ON v.electionid = e.election_id AND "
          + "    CASE "
          + "        WHEN LOWER(e.election_type) = 'dataaccess' THEN 'final' "
          + "        WHEN LOWER(e.election_type) = 'dataset' THEN 'data_owner' "
          + "        ELSE 'chairperson' "
          + "    END = LOWER(v.type)"
          + "WHERE e.election_id = :electionId LIMIT 1 ")
    Election findElectionWithFinalVoteById(@Bind("electionId") Integer electionId);

    @SqlQuery("SELECT e.* FROM election e INNER JOIN vote v ON v.electionId = e.election_id WHERE v.voteId = :voteId")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findElectionByVoteId(@Bind("voteId") Integer voteId);

    // TODO: This can return multiple rows per election id, e.g. ID 578 on staging.
    // The root of the duplicate rows is in the vote inner join. When there are multiple chairperson
    // votes, v.rationale and v.createDate can be different between the chairperson votes, leading
    // to duplicate rows.
    // See https://broadworkbench.atlassian.net/browse/DUOS-1526
    @SqlQuery(
        "SELECT DISTINCT " +
            "e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            "e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN vote v ON v.electionId = e.election_id AND LOWER(v.type) = 'chairperson' " +
        "WHERE LOWER(e.election_type) = LOWER(:type) " +
            "AND LOWER(e.status) = LOWER(:status) " +
            "AND v.vote IS NOT NULL " +
        "ORDER BY create_date ASC")
    List<Election> findElectionsWithFinalVoteByTypeAndStatus(@Bind("type") String type, @Bind("status") String status);

    @SqlQuery(
        "SELECT DISTINCT " +
            "e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            "e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN vote v ON v.electionId = e.election_id AND LOWER(v.type) = 'chairperson' " +
        "INNER JOIN " +
            "(SELECT reference_id, MAX(create_date) max_date " +
            "FROM election e " +
            "WHERE e.election_type = :type " +
            "GROUP BY reference_id) election_view " +
                "ON election_view.max_date = e.create_date " +
                "AND electionView.reference_id = e.reference_id " +
                "AND LOWER(e.election_type) = LOWER(:type) " +
        "ORDER BY create_date ASC")
    List<Election> findLastElectionsWithFinalVoteByType(@Bind("type") String type);

    @SqlQuery(
        "SELECT DISTINCT e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, " +
            "   v.rationale final_rationale, v.createdate final_vote_date, " +
            "   e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
            " FROM election e " +
            " INNER JOIN vote v ON v.electionid = e.election_id " +
            " INNER JOIN " +
            "   (SELECT e2.reference_id, MAX(e2.create_date) max_date " +
            "    FROM election e2 " +
            "    WHERE LOWER(e2.election_type) = 'dataaccess' " +
            "    AND LOWER(e2.status) = LOWER(:status) " +
            "    GROUP BY e2.reference_id) election_view " +
            "       ON election_view.max_date = e.create_date " +
            "       AND election_view.reference_id = e.reference_id" +
            " WHERE LOWER(e.election_type) = 'dataaccess' " +
            " AND e.final_access_vote IS true " +
            " AND LOWER(v.type) = 'final' " +
            " AND LOWER(e.status) = LOWER(:status) " +
            " ORDER BY e.create_date ASC ")
    List<Election> findLastDataAccessElectionsWithFinalVoteByStatus(@Bind("status") String status);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery(
        "SELECT e.* FROM election e " +
        "INNER JOIN data_access_request dar ON dar.reference_id = e.reference_id " +
        "INNER JOIN users u ON u.user_id = dar.user_id " +
        "INNER JOIN library_card lc ON lc.user_id = u.user_id " +
        "WHERE e.election_id IN (<electionIds>) ")
    List<Election> findElectionsWithCardHoldingUsersByElectionIds(@BindList("electionIds") List <Integer> electionIds);

    @SqlQuery(
        "SELECT DISTINCT e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, " +
            "v.rationale final_rationale, v.createDate final_vote_date, e.last_update, e.final_access_vote, " +
            "e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN vote v ON v.electionId = e.election_id AND LOWER(v.type) = 'chairperson' " +
        "INNER JOIN " +
            "(SELECT reference_id, MAX(create_date) max_date " +
            "FROM election e " +
            "WHERE LOWER(e.election_type) = LOWER(:type) " +
            "GROUP BY reference_id) election_view " +
                "ON election_view.max_date = e.create_date " +
                "AND election_view.reference_id = e.reference_id " +
                "AND LOWER(e.election_type) = LOWER(:type) " +
                "AND e.final_access_vote = :vote " +
                "AND LOWER(e.status) NOT IN ('canceled', 'closed') " +
        "ORDER BY create_date ASC")
    List<Election> findOpenLastElectionsByTypeAndFinalAccessVoteForChairPerson(@Bind("type") String type, @Bind("vote") Boolean finalAccessVote);

    @SqlQuery(
        "SELECT count(*) " +
            "FROM election e " +
            "INNER JOIN vote v " +
                "ON v.electionId = e.election_id " +
                "AND LOWER(v.type) = 'chairperson' " +
            "WHERE LOWER(e.election_type) = LOWER(:type) " +
            "AND LOWER(e.status) = LOWER(:status) " +
            "AND v.vote = :finalVote ")
    Integer findTotalElectionsByTypeStatusAndVote(@Bind("type") String type, @Bind("status") String status, @Bind("finalVote") Boolean finalVote);

    @SqlQuery(
        "SELECT count(*) " +
            "FROM election e " +
            "WHERE (LOWER(e.status) = 'open' OR LOWER(e.status) = 'final') " +
            "AND LOWER(e.election_type) != 'dataset' ")
    Integer verifyOpenElections();

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("SELECT * FROM election WHERE reference_id = :referenceId")
    List<Election> findElectionsByReferenceId(@Bind("referenceId") String referenceId);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("SELECT * FROM election WHERE reference_id in (<referenceIds>)")
    List<Election> findElectionsByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("SELECT * FROM election e " +
        "INNER JOIN vote v ON v.electionid = e.election_id " +
        "WHERE LOWER(e.election_type) = :electionType " +
        "AND v.voteid IN (<voteIds>)")
	List<Election> findElectionsByVoteIdsAndType(
	    @BindList("voteIds") List<Integer> voteIds,
	    @Bind("electionType") String electionType
	);


    @SqlQuery(
      "SELECT * FROM ( "
        + "SELECT e.*, v.vote final_vote, "
        + "     CASE "
        + "     WHEN v.updatedate IS NULL THEN v.createdate "
        + "     ELSE v.updatedate "
        + "     END as final_vote_date, "
        + " v.rationale final_rationale, MAX(e.election_id) "
        + " OVER (PARTITION BY e.reference_id, e.election_type) AS latest "
        + " FROM election e "
        + " LEFT JOIN vote v ON e.election_id = v.electionid AND "
        + "     CASE "
        + "     WHEN LOWER(e.election_type) = 'dataaccess' THEN 'final'"
        + "     WHEN LOWER(e.election_type) = 'dataset' THEN 'data_owner' "
        + "     ELSE 'chairperson' "
        + "     END = LOWER(v.type) "
        + " WHERE e.reference_id IN (<referenceIds>) "
        + ") AS results "
        + " WHERE results.latest = results.election_id "
        + " ORDER BY results.election_id DESC, "
        + "     CASE "
        + "     WHEN results.final_vote_date IS NULL THEN results.last_update "
        + "     ELSE results.final_vote_date "
        + "     END DESC"
    )
    @UseRowMapper(ElectionMapper.class)
    List<Election> findLastElectionsByReferenceIds(
      @BindList("referenceIds") List<String> referenceIds);

    @SqlQuery("SELECT DISTINCT e.* " +
        "FROM election e " +
        "WHERE LOWER(e.status) = 'open' " +
        "AND e.reference_id IN (<referenceIds>) ")
    @UseRowMapper(ElectionMapper.class)
    List<Election> findOpenElectionsByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

    @SqlQuery(
        "SELECT DISTINCT " +
            "e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, " +
            "v.createDate final_vote_date, e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN " +
            "(SELECT reference_id, MAX(create_date) max_date " +
            "FROM election e " +
            "WHERE e.election_type = :type " +
            "GROUP BY reference_id) election_view " +
                "ON election_view.max_date = e.create_date " +
                "AND election_view.reference_id = e.reference_id " +
                "AND e.reference_id in (<referenceIds>) " +
                "AND LOWER(e.election_type) = LOWER(:type) " +
        "LEFT JOIN vote v " +
            "ON v.electionId = e.election_id " +
            "AND LOWER(v.type) = 'final' ")
    List<Election> findLastElectionsWithFinalVoteByReferenceIdsAndType(@BindList("referenceIds") List<String> referenceIds, @Bind("type") String type);

    @SqlQuery(
        "SELECT DISTINCT e.* " +
            "FROM election e " +
            "INNER JOIN " +
                "(SELECT reference_id, MAX(create_date) max_date " +
                "FROM election e " +
                "WHERE e.election_type = :type " +
                "GROUP BY reference_id) election_view " +
                    "ON election_view.max_date = e.create_date " +
                    "AND election_view.reference_id = e.reference_id " +
                    "AND e.reference_id = :referenceId ")
    @UseRowMapper(SimpleElectionMapper.class)
    List<Election> findLastElectionsByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    // TODO: Update for datasetid distinction. Method can return a list, so refactor usages.
    @SqlQuery(
        "SELECT * " +
            "FROM election e " +
            "INNER JOIN " +
                "(SELECT reference_id, MAX(create_date) max_date " +
                "FROM election e " +
                "WHERE LOWER(e.status) = LOWER(:status) " +
                "GROUP BY reference_id) election_view " +
                    "ON election_view.max_date = e.create_date " +
                    "AND election_view.reference_id = e.reference_id " +
                    "AND e.reference_id = :referenceId ")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findLastElectionByReferenceIdAndStatus(@Bind("referenceId") String referenceIds, @Bind("status") String status);

    @SqlQuery(
        "SELECT distinct * " +
            "FROM election e " +
            "INNER JOIN " +
                "(SELECT reference_id, MAX(create_date) max_date " +
                "FROM election e WHERE LOWER(e.election_type) = LOWER(:type) " +
                "GROUP BY reference_id) election_view " +
                    "ON election_view.max_date = e.create_date " +
                    "AND election_view.reference_id = e.reference_id " +
            "WHERE e.reference_id in (<referenceIds>) " +
            "AND LOWER(e.election_type) = LOWER(:type)")
    @UseRowMapper(SimpleElectionMapper.class)
    List<Election> findLastElectionsByReferenceIdsAndType(@BindList("referenceIds") List<String> referenceIds, @Bind("type") String type);

    @SqlQuery(
        "SELECT DISTINCT " +
            "e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, " +
            "v.createDate final_vote_date, e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN " +
            "(SELECT reference_id, MAX(create_date) max_date " +
            "FROM election e " +
            "WHERE LOWER(e.status) = LOWER(:status) " +
            "GROUP BY reference_id) election_view " +
                "ON election_view.max_date = e.create_date " +
                "AND election_view.reference_id = e.reference_id " +
                "AND e.reference_id IN (<referenceIds>) " +
        "LEFT JOIN vote v " +
            "ON v.electionId = e.election_id " +
            "AND LOWER(v.type) = 'chairperson' ")
     List<Election> findLastElectionsWithFinalVoteByReferenceIdsTypeAndStatus(@BindList("referenceIds") List<String> referenceIds, @Bind("status") String status);

    @SqlQuery(
        "SELECT * FROM election e " +
            "INNER JOIN " +
                "(SELECT reference_id, MAX(create_date) max_date " +
                "FROM election e " +
                "WHERE LOWER(e.election_type) = LOWER(:type) " +
                "GROUP BY reference_id) election_view " +
                    "ON election_view.max_date = e.create_date " +
                    "AND election_view.reference_id = e.reference_id " +
                    "AND e.reference_id = :referenceId ")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findLastElectionByReferenceIdAndType(@Bind("referenceId") String referenceId, @Bind("type") String type);

    @SqlQuery(
        "SELECT e.* FROM election e " +
        "INNER JOIN " +
            "(SELECT reference_id, dataset_id, MAX(create_date) max_date " +
            "FROM election " +
            "WHERE LOWER(election_type) = lower(:type) AND dataset_id = :datasetId " +
            "GROUP BY reference_id, dataset_id) election_view " +
                "ON election_view.max_date = e.create_date " +
                "AND election_view.reference_id = e.reference_id " +
                "AND election_view.dataset_id = e.dataset_id " +
        "WHERE LOWER(e.election_type) = lower(:type) " +
        "AND e.reference_id = :referenceId " +
        "AND e.dataset_id = :datasetId ")
    @UseRowMapper(SimpleElectionMapper.class)
    Election findLastElectionByReferenceIdDatasetIdAndType(@Bind("referenceId") String referenceId, @Bind("datasetId") Integer datasetId, @Bind("type") String type);

    @SqlQuery("SELECT election_rp_id FROM access_rp arp WHERE arp.election_access_id = :electionAccessId ")
    Integer findRPElectionByElectionAccessId(@Bind("electionAccessId") Integer electionAccessId);

    @RegisterRowMapper(ImmutablePairOfIntsMapper.class)
    @SqlQuery("SELECT election_rp_id, election_access_id FROM access_rp arp WHERE arp.election_access_id IN (<electionAccessIds>) ")
    List<Pair<Integer, Integer>> findRpAccessElectionIdPairs(@BindList("electionAccessIds") List<Integer> electionAccessIds);

    @SqlUpdate("INSERT INTO access_rp (election_access_id, election_rp_id ) values (:electionAccessId, :electionRPId)")
    void insertAccessRP(@Bind("electionAccessId") Integer electionAccessId,
                        @Bind("electionRPId") Integer electionRPId);

    @SqlUpdate("INSERT INTO accesselection_consentelection (access_election_id, consent_election_id ) VALUES (:electionAccessId, :electionConsentId)")
    void insertAccessAndConsentElection(@Bind("electionAccessId") Integer electionAccessId,
                        @Bind("electionConsentId") Integer electionConsentId);

    @SqlQuery("SELECT consent_election_id FROM accesselection_consentelection WHERE access_election_id = :electionAccessId ")
    Integer getElectionConsentIdByDARElectionId(@Bind("electionAccessId") Integer electionAccessId);

    @SqlUpdate("DELETE FROM access_rp WHERE election_access_id = :electionAccessId")
    void deleteAccessRP(@Bind("electionAccessId") Integer electionAccessId);

    @SqlUpdate("DELETE FROM access_rp WHERE election_rp_id = :electionId OR election_access_id = :electionId")
    void deleteElectionFromAccessRP(@Bind("electionId") Integer electionId);

    @SqlUpdate("DELETE FROM access_rp WHERE election_rp_id IN (<electionIds>) OR election_access_id IN (<electionIds>)")
    void deleteElectionsFromAccessRPs(@BindList("electionIds") List<Integer> electionIds);

    @SqlQuery("SELECT election_access_id FROM access_rp arp WHERE arp.election_rp_id = :electionRPId ")
    Integer findAccessElectionByElectionRPId(@Bind("electionRPId") Integer electionRPId);

    @SqlQuery(
        "SELECT DISTINCT " +
            "e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            "e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN vote v " +
            "ON v.electionId = e.election_id " +
            "AND LOWER(v.type) = 'chairperson' " +
        "WHERE LOWER(e.election_type) = LOWER(:type) " +
            "AND LOWER(e.status) IN (<status>) " +
         "ORDER BY create_date ASC")
    List<Election> findElectionsWithFinalVoteByTypeAndStatus(@Bind("type") String type, @BindList("status") List<String> status);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("SELECT DISTINCT * FROM election e WHERE e.election_id IN (<electionIds>)")
    List<Election> findElectionsByIds(@BindList("electionIds") List<Integer> electionIds);

    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("SELECT * FROM election e WHERE e.election_id = :electionId")
    Election findElectionById(@Bind("electionId") Integer electionId);

    @SqlQuery("SELECT election_id FROM election WHERE reference_id = :referenceId AND LOWER(status) = 'open' AND dataset_id = :dataSetId")
    Integer getOpenElectionByReferenceIdAndDataSet(@Bind("referenceId") String referenceId, @Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("UPDATE election SET status = :status, last_update = :lastUpdate, final_access_vote = :finalAccessVote WHERE election_id = :electionId ")
    void updateElectionById(@Bind("electionId") Integer electionId,
                            @Bind("status") String status,
                            @Bind("lastUpdate") Date lastUpdate,
                            @Bind("finalAccessVote") Boolean finalAccessVote);

    @SqlUpdate("UPDATE election SET archived = true, last_update = :lastUpdate WHERE election_id = :electionId ")
    void archiveElectionById(@Bind("electionId") Integer electionId, @Bind("lastUpdate") Date lastUpdate);

    @SqlQuery(
        "SELECT DISTINCT " +
            "e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, e.reference_id, v.rationale final_rationale, v.createDate final_vote_date, " +
            "e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, e.archived, e.version " +
        "FROM election e " +
        "INNER JOIN vote v " +
            "ON v.electionId = e.election_id " +
            "AND LOWER(v.type) = 'final' " +
        "WHERE v.vote = :isApproved " +
            "AND LOWER(e.election_type) = 'dataaccess' " +
        "ORDER BY create_date ASC")
    List<Election> findDataAccessClosedElectionsByFinalResult(@Bind("isApproved") Boolean isApproved);

    @SqlQuery(
        "SELECT MAX(v.createDate) create_date " +
            "FROM election e " +
            "INNER JOIN vote v " +
                "ON v.electionId = e.election_id " +
                "AND LOWER(v.type) = 'final' " +
            "WHERE v.vote = true " +
                "AND LOWER(e.election_type) = 'dataaccess' " +
                "AND reference_id = :referenceId " +
            "GROUP BY e.create_date")
    @UseRowMapper(DateMapper.class)
    Date findApprovalAccessElectionDate(@Bind("referenceId") String referenceId);

    /**
     * Find the Dac for this election. Looks across associations to a dac via dataset and those
     * associated via the consent.
     *
     * @param electionId The election id
     * @return Dac for this election
     */
    @UseRowMapper(DacMapper.class)
    @SqlQuery("SELECT d0.* FROM ( " +
            "   SELECT d1.*, e1.election_id FROM dac d1 " +
            "     INNER JOIN consents c1 ON d1.dac_id = c1.dac_id " +
            "     INNER JOIN consent_associations a1 ON a1.consent_id = c1.consent_id " +
            "     INNER JOIN election e1 ON e1.dataset_id = a1.dataset_id AND e1.election_id = :electionId " +
            " UNION " +
            "   select d2.*, e2.election_id FROM dac d2 " +
            "     INNER JOIN consents c2 ON d2.dac_id = c2.dac_id " +
            "     INNER JOIN election e2 ON e2.reference_id = c2.consent_id and e2.election_id = :electionId " +
            " ) as d0 limit 1 ") // `select * from (...) limit 1` syntax is an hsqldb limitation
    Dac findDacForElection(@Bind("electionId") Integer electionId);

    @SqlQuery(
        "SELECT d.*, e.election_id as election_id "
        + "FROM election e "
        + "INNER JOIN accesselection_consentelection a ON a.access_election_id = e.election_id "
        + "INNER JOIN election consent_election ON a.consent_election_id = consent_election.election_id "
        + "INNER JOIN consents c ON consent_election.reference_id = c.consent_id "
        + "INNER JOIN dac d on d.dac_id = c.dac_id "
        + "WHERE e.election_id IN (<electionIds>) "
        + "UNION "
        + "SELECT d.*, e.election_id "
        + "FROM dac d "
        + "INNER JOIN consents "
        + "ON d.dac_id = consents.dac_id "
        + "INNER JOIN consent_associations ca "
        + "ON ca.consent_id = consents.consent_id "
        + "INNER JOIN dataset data "
        + "ON data.dataset_id = ca.dataset_id "
        + "INNER JOIN election e "
        + "ON e.dataset_id = data.dataset_id "
        + "WHERE e.election_id IN (<electionIds>)"
    )
    @UseRowMapper(DacMapper.class)
    List<Dac> findAllDacsForElectionIds(@BindList("electionIds") List<Integer> electionIds);

    /**
     * Find the OPEN elections that belong to this Dac
     *
     * @param dacId The Dac id
     * @return List of elections associated to the Dac
     */
    @UseRowMapper(SimpleElectionMapper.class)
    @SqlQuery("SELECT e1.* FROM election e1 " +
            "   INNER JOIN consent_associations a1 ON a1.dataset_id = e1.dataset_id " +
            "   INNER JOIN consents c1 ON c1.consent_id = a1.consent_id AND c1.dac_id = :dacId " +
            "   WHERE LOWER(e1.status) = 'open' " +
            " UNION " +
            " SELECT e2.* from election e2 " +
            "   INNER JOIN consents c2 on c2.consent_id = e2.reference_id and c2.dac_id = :dacId " +
            "   where lower(e2.status) = 'open' ")
    List<Election> findOpenElectionsByDacId(@Bind("dacId") Integer dacId);

}
