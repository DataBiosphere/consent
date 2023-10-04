package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DarCollectionSummaryReducer;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface DarCollectionSummaryDAO extends Transactional<DarCollectionSummaryDAO> {

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery("""
      SELECT c.collection_id as dar_collection_id, c.dar_code, dar.submission_date, dar.reference_id as dar_reference_id, u.display_name as researcher_name,
        i.institution_name, e.election_id, e.status, e.dataset_id, e.reference_id, v.voteid as v_vote_id, dd.dataset_id as dd_datasetid,
        v.user_id as v_user_id, v.vote as v_vote, v.electionid as v_election_id, v.createdate as v_create_date, v.updatedate as v_update_date, v.type as v_type,
        (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' AS name
      FROM dar_collection c
      INNER JOIN users u
        ON u.user_id = c.create_user_id
      LEFT JOIN institution i
        ON i.institution_id = u.institution_id
      INNER JOIN data_access_request dar
        ON dar.collection_id = c.collection_id
      LEFT JOIN (
        SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
        FROM election
        WHERE LOWER(election.election_type) = 'dataaccess' AND election.dataset_id IN (<datasetIds>)
      ) AS e
        ON e.reference_id = dar.reference_id
      LEFT JOIN vote v
        ON e.election_id = v.electionid
      INNER JOIN dar_dataset dd
        ON dar.reference_id = dd.reference_id
      WHERE dd.dataset_id IN (<datasetIds>)
        AND (e.latest = e.election_id OR e.election_id IS NULL)
        AND (LOWER(v.type) = 'final' OR (v.user_id = :currentUserId OR v.voteid IS NULL))
        AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL )
      """)
  List<DarCollectionSummary> getDarCollectionSummariesForDAC(
      @Bind("currentUserId") Integer currentUserId,
      @BindList("datasetIds") List<Integer> datasetIds);

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery
      (
          "SELECT c.collection_id as dar_collection_id, c.dar_code, dar.submission_date, dar.reference_id as dar_reference_id, u.display_name as researcher_name, "
              +
              "i.institution_name, e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id as dd_datasetid, "
              +
              "(regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' AS name " +
              "FROM dar_collection c " +
              "INNER JOIN users u " +
              "ON u.user_id = c.create_user_id " +
              "LEFT JOIN institution i " +
              "ON i.institution_id = u.institution_id " +
              "INNER JOIN data_access_request dar " +
              "ON dar.collection_id = c.collection_id " +
              "LEFT JOIN ( " +
              "SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest "
              +
              "FROM election " +
              "WHERE LOWER(election.election_type) = 'dataaccess'" +
              ") AS e " +
              "ON e.reference_id = dar.reference_id " +
              "INNER JOIN dar_dataset dd " +
              "ON dar.reference_id = dd.reference_id " +
              "WHERE u.institution_id = :institutionId " +
              "AND (e.latest = e.election_id OR e.election_id IS NULL) " +
              "AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL ) "
      )
  List<DarCollectionSummary> getDarCollectionSummariesForSO(
      @Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery("""
      SELECT c.collection_id as dar_collection_id, c.dar_code, dar.submission_date, dar.reference_id as dar_reference_id, u.display_name as researcher_name,
        i.institution_name, e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id as dd_datasetid,
        (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' AS name,
        dac.name as dac_name
      FROM dar_collection c
      INNER JOIN users u ON u.user_id = c.create_user_id
      LEFT JOIN institution i ON i.institution_id = u.institution_id
      INNER JOIN data_access_request dar ON dar.collection_id = c.collection_id
      LEFT JOIN (
        SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
        FROM election
        WHERE LOWER(election.election_type) = 'dataaccess'
      ) AS e ON e.reference_id = dar.reference_id
      INNER JOIN dar_dataset dd ON dar.reference_id = dd.reference_id
      LEFT JOIN dataset dataset on dataset.dataset_id = dd.dataset_id
      LEFT JOIN dac dac on dac.dac_id = dataset.dac_id
      WHERE (e.latest = e.election_id OR e.election_id IS NULL)
        AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL )
      """)
  List<DarCollectionSummary> getDarCollectionSummariesForAdmin();

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery
      (
          "SELECT c.collection_id as dar_collection_id, c.dar_code, dar.submission_date, dar.reference_id as dar_reference_id, u.display_name as researcher_name, "
              +
              "i.institution_name, e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id as dd_datasetid, "
              +
              "(regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' AS name, " +
              "(regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'status' AS dar_status " +
              "FROM dar_collection c " +
              "INNER JOIN users u " +
              "ON u.user_id = c.create_user_id " +
              "LEFT JOIN institution i " +
              "ON i.institution_id = u.institution_id " +
              "INNER JOIN data_access_request dar " +
              "ON dar.collection_id = c.collection_id " +
              "LEFT JOIN ( " +
              "SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest "
              +
              "FROM election " +
              "WHERE LOWER(election.election_type) = 'dataaccess'" +
              ") AS e " +
              "ON e.reference_id = dar.reference_id " +
              "INNER JOIN dar_dataset dd " +
              "ON dar.reference_id = dd.reference_id " +
              "WHERE c.create_user_id = :userId " +
              "AND (e.latest = e.election_id OR e.election_id IS NULL) " +
              "AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL ) " +
              "AND (EXISTS (SELECT 1 FROM data_access_request WHERE (collection_id = c.collection_id and draft = false)))"
      )
  List<DarCollectionSummary> getDarCollectionSummariesForResearcher(
      @Bind("userId") Integer userId);


  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery("""
      SELECT c.collection_id as dar_collection_id, c.dar_code, dar.submission_date, u.display_name as researcher_name, u.user_id as researcher_id,
        i.institution_name, i.institution_id, e.election_id, e.status, e.dataset_id, e.reference_id, v.voteid as v_vote_id, dd.dataset_id as dd_datasetid,
        v.user_id as v_user_id, v.vote as v_vote, v.electionid as v_election_id, v.createdate as v_create_date, v.updatedate as v_update_date, v.type as v_type,
        (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' AS name
      FROM dar_collection c
      INNER JOIN users u
        ON u.user_id = c.create_user_id
      LEFT JOIN institution i
        ON i.institution_id = u.institution_id
      INNER JOIN data_access_request dar
        ON dar.collection_id = c.collection_id
      LEFT JOIN (
        SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
        FROM election
        WHERE LOWER(election.election_type) = 'dataaccess' AND election.dataset_id IN (<datasetIds>)
      ) AS e
        ON e.reference_id = dar.reference_id
      LEFT JOIN vote v
        ON e.election_id = v.electionid
      INNER JOIN dar_dataset dd
        ON dar.reference_id = dd.reference_id
      WHERE c.collection_id= :collectionId
        AND dd.dataset_id IN (<datasetIds>)
        AND (e.latest = e.election_id OR e.election_id IS NULL)
        AND (LOWER(v.type) = 'final' OR (v.user_id = :currentUserId OR v.voteid IS NULL))
        AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL )
      """)
  DarCollectionSummary getDarCollectionSummaryForDACByCollectionId(
      @Bind("currentUserId") Integer currentUserId,
      @BindList("datasetIds") List<Integer> datasetIds,
      @Bind("collectionId") Integer collectionId);


  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery
      (
          "SELECT c.collection_id as dar_collection_id, c.dar_code, dar.submission_date, dar.reference_id as dar_reference_id, u.display_name as researcher_name, "
              +
              "u.user_id as researcher_id, i.institution_name, i.institution_id, e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id as dd_datasetid, "
              +
              "(regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'projectTitle' AS name, " +
              "(regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'status' AS dar_status " +
              "FROM dar_collection c " +
              "INNER JOIN users u " +
              "ON u.user_id = c.create_user_id " +
              "LEFT JOIN institution i " +
              "ON i.institution_id = u.institution_id " +
              "INNER JOIN data_access_request dar " +
              "ON dar.collection_id = c.collection_id " +
              "LEFT JOIN ( " +
              "SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest "
              +
              "FROM election " +
              "WHERE LOWER(election.election_type) = 'dataaccess'" +
              ") AS e " +
              "ON e.reference_id = dar.reference_id " +
              "INNER JOIN dar_dataset dd " +
              "ON dar.reference_id = dd.reference_id " +
              "WHERE c.collection_id = :collectionId " +
              "AND (e.latest = e.election_id OR e.election_id IS NULL) " +
              "AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL )"
      )
  DarCollectionSummary getDarCollectionSummaryByCollectionId(
      @Bind("collectionId") Integer collectionId);
}
