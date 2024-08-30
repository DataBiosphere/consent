package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.ElectionMapper;
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
      "SELECT DISTINCT election_id " +
          "FROM election " +
          "WHERE reference_id IN (<referenceIds>)")
  List<Integer> getElectionIdsByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  @SqlUpdate("INSERT INTO election " +
      "(election_type, status, create_date, reference_id, dataset_id) VALUES " +
      "(:electionType, :status, :createDate,:referenceId, :datasetId)")
  @GetGeneratedKeys
  Integer insertElection(@Bind("electionType") String electionType,
      @Bind("status") String status,
      @Bind("createDate") Date createDate,
      @Bind("referenceId") String referenceId,
      @Bind("datasetId") Integer dataSetId);

  @SqlUpdate("DELETE FROM election WHERE election_id in (<electionIds>)")
  void deleteElectionsByIds(@BindList("electionIds") List<Integer> electionIds);

  @SqlUpdate("UPDATE election SET status = :status, last_update = :lastUpdate WHERE election_id = :electionId ")
  void updateElectionById(@Bind("electionId") Integer electionId,
      @Bind("status") String status,
      @Bind("lastUpdate") Date lastUpdate);

  @SqlQuery("SELECT DISTINCT "
      + "    e.election_id, e.dataset_id, v.vote final_vote, e.status, e.create_date, "
      + "    e.reference_id, v.rationale final_rationale, v.createdate final_vote_date, "
      + "    e.last_update, e.final_access_vote, e.election_type, e.data_use_letter, e.dul_name, "
      + "    e.archived, e.version "
      + "FROM election e "
      + "INNER JOIN vote v ON v.electionid = e.election_id AND "
      + "    CASE "
      + "        WHEN LOWER(e.election_type) = 'dataaccess' THEN 'final' "
      + "        WHEN LOWER(e.election_type) = 'dataset' THEN 'data_owner' "
      + "        ELSE 'chairperson' "
      + "    END = LOWER(v.type)"
      + "WHERE e.election_id = :electionId LIMIT 1 ")
  Election findElectionWithFinalVoteById(@Bind("electionId") Integer electionId);

  @UseRowMapper(SimpleElectionMapper.class)
  @SqlQuery(
      "SELECT e.* FROM election e " +
          "INNER JOIN data_access_request dar ON dar.reference_id = e.reference_id " +
          "INNER JOIN users u ON u.user_id = dar.user_id " +
          "INNER JOIN library_card lc ON lc.user_id = u.user_id " +
          "WHERE e.election_id IN (<electionIds>) ")
  List<Election> findElectionsWithCardHoldingUsersByElectionIds(
      @BindList("electionIds") List<Integer> electionIds);

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
  List<Election> findOpenElectionsByReferenceIds(
      @BindList("referenceIds") List<String> referenceIds);

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
  List<Election> findLastElectionsByReferenceIdsAndType(
      @BindList("referenceIds") List<String> referenceIds, @Bind("type") String type);

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
  Election findLastElectionByReferenceIdDatasetIdAndType(@Bind("referenceId") String referenceId,
      @Bind("datasetId") Integer datasetId, @Bind("type") String type);

  @SqlQuery("""
      SELECT e.*
      FROM election e
      WHERE e.reference_id = :referenceId
      AND e.dataset_id = :datasetId
      """)
  @UseRowMapper(SimpleElectionMapper.class)
  List<Election> findElectionsByReferenceIdAndDatasetId(@Bind("referenceId") String referenceId,
      @Bind("datasetId") Integer datasetId);

  @UseRowMapper(SimpleElectionMapper.class)
  @SqlQuery("SELECT DISTINCT * FROM election e WHERE e.election_id IN (<electionIds>)")
  List<Election> findElectionsByIds(@BindList("electionIds") List<Integer> electionIds);

  @UseRowMapper(SimpleElectionMapper.class)
  @SqlQuery("SELECT * FROM election e WHERE e.election_id = :electionId")
  Election findElectionById(@Bind("electionId") Integer electionId);

  @SqlUpdate("UPDATE election SET status = :status, last_update = :lastUpdate, final_access_vote = :finalAccessVote WHERE election_id = :electionId ")
  void updateElectionById(@Bind("electionId") Integer electionId,
      @Bind("status") String status,
      @Bind("lastUpdate") Date lastUpdate,
      @Bind("finalAccessVote") Boolean finalAccessVote);

  @SqlUpdate("""
      UPDATE election
      SET archived = true, last_update = :lastUpdate
      WHERE election_id IN (<electionIds>)
      """)
  void archiveElectionByIds(@BindList("electionIds") List<Integer> electionIds,
      @Bind("lastUpdate") Date lastUpdate);

  /**
   * Find the Dac for this election. Looks across associations to a dac via dataset and those
   * associated via the consent.
   *
   * @param electionId The election id
   * @return Dac for this election
   */
  @UseRowMapper(DacMapper.class)
  @SqlQuery("""
      SELECT d.* FROM dac d
      INNER JOIN dataset ds on d.dac_id = ds.dac_id
      INNER JOIN election e on ds.dataset_id = e.dataset_id
      WHERE e.election_id = :electionId
      """)
  Dac findDacForElection(@Bind("electionId") Integer electionId);

  @SqlQuery("""
      SELECT d.*, e.election_id
      FROM dac d
      INNER JOIN dataset data ON d.dac_id = data.dac_id
      INNER JOIN election e
      ON e.dataset_id = data.dataset_id
      WHERE e.election_id IN (<electionIds>)
      """)
  @UseRowMapper(DacMapper.class)
  List<Dac> findAllDacsForElectionIds(@BindList("electionIds") List<Integer> electionIds);

  /**
   * Find the OPEN elections that belong to this Dac
   *
   * @param dacId The Dac id
   * @return List of elections associated to the Dac
   */
  @UseRowMapper(SimpleElectionMapper.class)
  @SqlQuery("""
      SELECT e1.* FROM election e1
      INNER JOIN dataset ds1 on ds1.dac_id = :dacId AND ds1.dataset_id = e1.dataset_id
      WHERE LOWER(e1.status) = 'open'
      """)
  List<Election> findOpenElectionsByDacId(@Bind("dacId") Integer dacId);

}
