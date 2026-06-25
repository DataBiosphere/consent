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
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface DarCollectionSummaryDAO extends Transactional<DarCollectionSummaryDAO> {

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
      SELECT c.collection_id as dar_collection_id, c.dar_code,
        latest_dar.submission_date, latest_dar.reference_id as latest_dar_reference_id,
        latest_dar.parent_id as latest_dar_parent_id,
        latest_dar.requires_so_approval as latest_dar_requires_so_approval,
        latest_dar.approving_so_id as latest_dar_so_approver_id,
        latest_dar.approving_so_timestamp as latest_dar_so_approver_timestamp,
        latest_dar.update_date as latest_dar_update_date,
        researcher.display_name as researcher_name, i.institution_name,
        e.election_id, e.status, e.dataset_id, e.reference_id,
        v.vote_id as v_vote_id, dd.dataset_id as dd_datasetid,
        v.user_id as v_user_id, v.vote as v_vote, v.election_id as v_election_id,
        v.create_date as v_create_date,v.update_date as v_update_date, v.type as v_type,
        latest_dar.data ->> 'projectTitle' AS name,
        latest_dar.data ->> 'status' AS dar_status,
        latest_dar.data ->> 'closeoutSupplement' AS closeout,
        latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
        dac.name AS dac_name,
        ARRAY_AGG(dar_all.reference_id) AS reference_ids
      FROM dar_collection c
      -- DAR Collection Researcher join
      INNER JOIN users researcher
        ON researcher.user_id = c.create_user_id
      LEFT JOIN institution i
        ON i.institution_id = researcher.institution_id
      -- DAC User join
      INNER JOIN users dacUser
        ON dacUser.user_id = :currentUserId
      INNER JOIN user_role ur
        ON dacUser.user_id = ur.user_id AND ur.role_id = :roleId AND ur.dac_id IS NOT NULL
      INNER JOIN dac dac
        ON ur.dac_id = dac.dac_id AND dac.deleted IS NOT TRUE
      -- Datasets available to DAC
      INNER JOIN dataset d
        ON d.dac_id = dac.dac_id
      -- Restrict DARs to the most recent submission per collection
      INNER JOIN (
        SELECT DISTINCT ON (collection_id) *
        FROM data_access_request
        WHERE submission_date IS NOT NULL
        AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
        ORDER BY collection_id, submission_date DESC
        ) latest_dar ON latest_dar.collection_id = c.collection_id
      -- All DARs for the collection
      INNER JOIN data_access_request dar_all
        ON dar_all.collection_id = c.collection_id
        AND dar_all.submission_date IS NOT NULL
        AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
      -- Most recent terminal or active Data Access Elections for DAC User datasets
      LEFT JOIN (
        SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
        FROM election
        WHERE LOWER(election.election_type) = 'dataaccess'
        AND LOWER(election.status) IN ('open', 'closed', 'canceled')
      ) AS e
        ON e.reference_id = latest_dar.reference_id
        AND e.dataset_id = d.dataset_id
      -- Votes for DAC User
      LEFT JOIN vote v
        ON e.election_id = v.election_id
        AND (LOWER(v.type) IN ('final', 'radar_approve') OR v.user_id = :currentUserId)
      -- Restrict DARs to the datasets available to the DAC User
      INNER JOIN dar_dataset dd
        ON latest_dar.reference_id = dd.reference_id
      WHERE dd.dataset_id = d.dataset_id
        AND (e.latest = e.election_id OR e.election_id IS NULL)
      GROUP BY
        c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id,
        latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp,
        latest_dar.update_date, researcher.display_name, i.institution_name,
        e.election_id, e.status, e.reference_id, e.dataset_id, v.vote_id, dd.dataset_id, v.user_id,
        v.vote, v.election_id, v.create_date, v.update_date, v.type, latest_dar.data, dac.name
      """)
  List<DarCollectionSummary> getDarCollectionSummariesForDACRole(
      @Bind("currentUserId") Integer currentUserId, @Bind("roleId") Integer roleId);

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
              SELECT c.collection_id as dar_collection_id,
               c.dar_code,
               latest_dar.submission_date,
               latest_dar.reference_id as latest_dar_reference_id,
               latest_dar.parent_id as latest_dar_parent_id,
               latest_dar.requires_so_approval as latest_dar_requires_so_approval,
               latest_dar.approving_so_id as latest_dar_so_approver_id,
               latest_dar.approving_so_timestamp as latest_dar_so_approver_timestamp,
               latest_dar.update_date as latest_dar_update_date,
               u.display_name as researcher_name,
               i.institution_name,
               e.election_id,
               e.status,
               e.dataset_id,
               e.reference_id,
               dd.dataset_id as dd_datasetid,
               dac.name AS dac_name,
               latest_dar.data ->> 'projectTitle' AS name,
               latest_dar.data ->> 'status' AS dar_status,
               latest_dar.data ->> 'closeoutSupplement' AS closeout,
               latest_dar.data ->> 'signingOfficialEmail' AS signingOfficialEmail,
               latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
               ARRAY_AGG(dar_all.reference_id) AS reference_ids
              FROM dar_collection c
              INNER JOIN users u
                ON u.user_id = c.create_user_id
              LEFT JOIN institution i
                ON i.institution_id = u.institution_id
              INNER JOIN (
                SELECT DISTINCT ON (collection_id) *
                FROM data_access_request
                WHERE submission_date IS NOT NULL
                AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
                ORDER BY collection_id, submission_date DESC
              ) latest_dar ON latest_dar.collection_id = c.collection_id
              INNER JOIN data_access_request dar_all
               ON dar_all.collection_id = c.collection_id
               AND dar_all.submission_date IS NOT NULL
               AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
              LEFT JOIN (
                SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
                FROM election
                WHERE LOWER(election.election_type) = 'dataaccess'
                ) AS e
              ON e.reference_id = latest_dar.reference_id
              INNER JOIN dar_dataset dd
              ON latest_dar.reference_id = dd.reference_id
              LEFT JOIN dataset ON dataset.dataset_id = dd.dataset_id
              LEFT JOIN dac ON dac.dac_id = dataset.dac_id AND dac.deleted IS NOT TRUE
              WHERE u.institution_id = :institutionId
                AND (e.latest = e.election_id OR e.election_id IS NULL)
              GROUP BY
              c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id,
              latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp,
              latest_dar.update_date, u.display_name,
              i.institution_name, e.election_id, e.status, e.reference_id, e.dataset_id,
              dd.dataset_id, latest_dar.data, dac.name
          """)
  List<DarCollectionSummary> getDarCollectionSummariesForSO(
      @Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
          SELECT
              c.collection_id AS dar_collection_id,
              c.dar_code,
              latest_dar.submission_date,
              latest_dar.reference_id AS latest_dar_reference_id,
              latest_dar.parent_id AS latest_dar_parent_id,
              latest_dar.requires_so_approval as latest_dar_requires_so_approval,
              latest_dar.approving_so_id as latest_dar_so_approver_id,
              latest_dar.approving_so_timestamp as latest_dar_so_approver_timestamp,
              latest_dar.update_date as latest_dar_update_date,
              u.display_name AS researcher_name,
              i.institution_name,
              e.election_id,
              e.status,
              e.dataset_id,
              dd.dataset_id AS dd_datasetid,
              latest_dar.data ->> 'projectTitle' AS name,
              latest_dar.data ->> 'status' AS dar_status,
              latest_dar.data ->> 'closeoutSupplement' AS closeout,
              latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
              dac.name AS dac_name,
              ARRAY_AGG(dar_all.reference_id) AS reference_ids
          FROM dar_collection c
          INNER JOIN users u ON u.user_id = c.create_user_id
          LEFT JOIN institution i ON i.institution_id = u.institution_id
          INNER JOIN (
              SELECT DISTINCT ON (collection_id) *
              FROM data_access_request
              WHERE submission_date IS NOT NULL
              AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
              ORDER BY collection_id, submission_date DESC
          ) latest_dar ON latest_dar.collection_id = c.collection_id
          INNER JOIN data_access_request dar_all
              ON dar_all.collection_id = c.collection_id
              AND dar_all.submission_date IS NOT NULL
              AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
          LEFT JOIN (
                  SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
                  FROM election
                  WHERE LOWER(election.election_type) = 'dataaccess'
                ) AS e ON e.reference_id = latest_dar.reference_id
          INNER JOIN dar_dataset dd ON latest_dar.reference_id = dd.reference_id
          LEFT JOIN dataset ON dataset.dataset_id = dd.dataset_id
          LEFT JOIN dac ON dac.dac_id = dataset.dac_id AND dac.deleted IS NOT TRUE
          WHERE (e.latest = e.election_id OR e.election_id IS NULL)
          GROUP BY
              c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id,
              latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp,
              latest_dar.update_date, u.display_name,
              i.institution_name, e.election_id, e.status, e.dataset_id, dd.dataset_id, latest_dar.data, dac.name
      """)
  List<DarCollectionSummary> getDarCollectionSummariesForAdmin();

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
          SELECT
              c.collection_id AS dar_collection_id,
              c.dar_code,
              latest_dar.submission_date,
              latest_dar.reference_id AS latest_dar_reference_id,
              latest_dar.parent_id AS latest_dar_parent_id,
              latest_dar.requires_so_approval as latest_dar_requires_so_approval,
              latest_dar.approving_so_id as latest_dar_so_approver_id,
              latest_dar.approving_so_timestamp as latest_dar_so_approver_timestamp,
              latest_dar.update_date as latest_dar_update_date,
              u.display_name AS researcher_name,
              i.institution_name,
              e.election_id,
              e.status,
              e.dataset_id,
              e.reference_id AS election_reference_id,
              dd.dataset_id AS dd_datasetid,
              dac.name AS dac_name,
              latest_dar.data ->> 'projectTitle' AS name,
              latest_dar.data ->> 'status' AS dar_status,
              latest_dar.data ->> 'closeoutSupplement' AS closeout,
              latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
              ARRAY_AGG(dar_all.reference_id) AS reference_ids
          FROM
              dar_collection c
          INNER JOIN
              users u ON u.user_id = c.create_user_id
          LEFT JOIN
              institution i ON i.institution_id = u.institution_id
          INNER JOIN (
               SELECT DISTINCT ON (collection_id) *
               FROM data_access_request
               WHERE submission_date IS NOT NULL
               AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
               ORDER BY collection_id, submission_date DESC
          ) latest_dar ON latest_dar.collection_id = c.collection_id
          INNER JOIN
              data_access_request dar_all
              ON dar_all.collection_id = c.collection_id
              AND dar_all.submission_date IS NOT NULL
              AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
          LEFT JOIN (
              SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
              FROM election
              WHERE LOWER(election.election_type) = 'dataaccess'
          ) AS e ON e.reference_id = latest_dar.reference_id
          INNER JOIN
              dar_dataset dd ON latest_dar.reference_id = dd.reference_id
          LEFT JOIN dataset ON dataset.dataset_id = dd.dataset_id
          LEFT JOIN dac ON dac.dac_id = dataset.dac_id AND dac.deleted IS NOT TRUE
          WHERE
              c.create_user_id = :userId
              AND (e.latest = e.election_id OR e.election_id IS NULL)
          GROUP BY
              c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id, u.display_name, i.institution_name,
              latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp, latest_dar.update_date,
              e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id, latest_dar.data, dac.name
      """)
  List<DarCollectionSummary> getDarCollectionSummariesForResearcher(@Bind("userId") Integer userId);

  /**
   * Returns DarCollectionSummaries for all collections that contain at least one approved DAR (or
   * closeout supplement) for the given dataset. The approval filter mirrors {@code
   * DataAccessRequestDAO#findSummaryMetricApprovedDARsByDatasetIdIncludesExpired}: the last
   * final/radar_approve vote per (reference_id, dataset_id) must be {@code TRUE}, plus any closeout
   * supplement DARs for the dataset are included via UNION.
   *
   * @param datasetId The dataset id
   * @return List of DarCollectionSummary
   */
  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
          WITH approved_collections AS (
              SELECT DISTINCT dar.collection_id
              FROM data_access_request dar
              INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
              INNER JOIN (
                  SELECT DISTINCT e.reference_id, e.dataset_id,
                      LAST_VALUE(v.vote) OVER(
                          PARTITION BY e.reference_id, e.dataset_id
                          ORDER BY v.create_date
                          RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                      ) last_vote
                  FROM election e
                  INNER JOIN vote v ON e.election_id = v.election_id
                      AND v.vote IS NOT NULL
                      AND LOWER(e.election_type) = 'dataaccess'
                      AND LOWER(v.type) IN ('final', 'radar_approve')
              ) final_access_vote ON final_access_vote.reference_id = dar.reference_id
                  AND final_access_vote.dataset_id = dd.dataset_id
              WHERE dd.dataset_id = :datasetId
                  AND dar.submission_date IS NOT NULL
                  AND final_access_vote.last_vote = TRUE
                  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
              UNION
              SELECT DISTINCT dar.collection_id
              FROM data_access_request dar
              INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
              WHERE dd.dataset_id = :datasetId
                  AND dar.submission_date IS NOT NULL
                  AND dar.data ->> 'closeoutSupplement' IS NOT NULL
          )
          SELECT
              c.collection_id AS dar_collection_id,
              c.dar_code,
              latest_dar.submission_date,
              latest_dar.reference_id AS latest_dar_reference_id,
              latest_dar.parent_id AS latest_dar_parent_id,
              latest_dar.requires_so_approval AS latest_dar_requires_so_approval,
              latest_dar.approving_so_id AS latest_dar_so_approver_id,
              latest_dar.approving_so_timestamp AS latest_dar_so_approver_timestamp,
              latest_dar.update_date as latest_dar_update_date,
              u.display_name AS researcher_name,
              i.institution_name,
              e.election_id,
              e.status,
              e.dataset_id,
              e.reference_id AS election_reference_id,
              dd.dataset_id AS dd_datasetid,
              dac.name AS dac_name,
              latest_dar.data ->> 'projectTitle' AS name,
              latest_dar.data ->> 'status' AS dar_status,
              latest_dar.data ->> 'closeoutSupplement' AS closeout,
              latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
              ARRAY_AGG(dar_all.reference_id) AS reference_ids
          FROM dar_collection c
          INNER JOIN approved_collections ON c.collection_id = approved_collections.collection_id
          INNER JOIN users u ON u.user_id = c.create_user_id
          LEFT JOIN institution i ON i.institution_id = u.institution_id
          INNER JOIN (
              SELECT DISTINCT ON (collection_id) *
              FROM data_access_request
              WHERE submission_date IS NOT NULL
              AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
              ORDER BY collection_id, submission_date DESC
          ) latest_dar ON latest_dar.collection_id = c.collection_id
          INNER JOIN data_access_request dar_all
              ON dar_all.collection_id = c.collection_id
              AND dar_all.submission_date IS NOT NULL
              AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
          LEFT JOIN (
              SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
              FROM election
              WHERE LOWER(election.election_type) = 'dataaccess'
          ) AS e ON e.reference_id = latest_dar.reference_id
          INNER JOIN dar_dataset dd ON latest_dar.reference_id = dd.reference_id
          LEFT JOIN dataset ON dataset.dataset_id = dd.dataset_id
          LEFT JOIN dac ON dac.dac_id = dataset.dac_id AND dac.deleted IS NOT TRUE
          WHERE dd.dataset_id = :datasetId
              AND (e.latest = e.election_id OR e.election_id IS NULL)
          GROUP BY
              c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id,
              latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp,
              latest_dar.update_date, u.display_name, i.institution_name,
              e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id, latest_dar.data, dac.name
          ORDER BY c.dar_code
      """)
  List<DarCollectionSummary> getDarCollectionSummariesByDatasetId(
      @Bind("datasetId") Integer datasetId);

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
      SELECT c.collection_id as dar_collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id AS latest_dar_reference_id,
        latest_dar.parent_id AS latest_dar_parent_id,
        latest_dar.requires_so_approval as latest_dar_requires_so_approval,
        latest_dar.approving_so_id as latest_dar_so_approver_id,
        latest_dar.approving_so_timestamp as latest_dar_so_approver_timestamp,
        latest_dar.update_date as latest_dar_update_date,
        u.display_name as researcher_name, u.user_id as researcher_id,
        i.institution_name, i.institution_id, e.election_id, e.status, e.dataset_id, e.reference_id, v.vote_id as v_vote_id, dd.dataset_id as dd_datasetid,
        v.user_id as v_user_id, v.vote as v_vote, v.election_id as v_election_id, v.create_date as v_create_date, v.update_date as v_update_date, v.type as v_type,
        latest_dar.data ->> 'projectTitle' AS name,
        latest_dar.data ->> 'status' AS dar_status,
        latest_dar.data ->> 'closeoutSupplement' AS closeout,
        latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
        dac.name AS dac_name,
        ARRAY_AGG(dar_all.reference_id) AS reference_ids
      FROM dar_collection c
      INNER JOIN users u
        ON u.user_id = c.create_user_id
      LEFT JOIN institution i
        ON i.institution_id = u.institution_id
      INNER JOIN (
        SELECT DISTINCT ON (collection_id) *
        FROM data_access_request
        WHERE submission_date IS NOT NULL
        AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
        ORDER BY collection_id, submission_date DESC
      ) latest_dar ON latest_dar.collection_id = c.collection_id
      INNER JOIN
        data_access_request dar_all ON dar_all.collection_id = c.collection_id
        AND dar_all.submission_date IS NOT NULL
        AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
      LEFT JOIN (
        SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
        FROM election
        WHERE LOWER(election.election_type) = 'dataaccess'
        AND LOWER(election.status) IN ('open', 'closed', 'canceled')
        AND election.dataset_id IN (<datasetIds>)
      ) AS e
        ON e.reference_id = latest_dar.reference_id
      LEFT JOIN vote v
        ON e.election_id = v.election_id
      INNER JOIN dar_dataset dd
        ON latest_dar.reference_id = dd.reference_id
      LEFT JOIN dataset ON dataset.dataset_id = dd.dataset_id
      LEFT JOIN dac ON dac.dac_id = dataset.dac_id AND dac.deleted IS NOT TRUE
      WHERE c.collection_id= :collectionId
        AND dd.dataset_id IN (<datasetIds>)
        AND (e.latest = e.election_id OR e.election_id IS NULL)
        AND (LOWER(v.type) IN ('final', 'radar_approve') OR (v.user_id = :currentUserId OR v.vote_id IS NULL))
      GROUP BY
        c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id,
        latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp,
        latest_dar.update_date, u.display_name, u.user_id,
        i.institution_name, i.institution_id, e.election_id, e.status,
        e.reference_id, e.dataset_id, v.vote_id, dd.dataset_id, v.user_id,
        v.vote, v.election_id, v.create_date, v.update_date, v.type, latest_dar.data, dac.name
      """)
  DarCollectionSummary getDarCollectionSummaryForDACByCollectionId(
      @Bind("currentUserId") Integer currentUserId,
      @BindList(value = "datasetIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> datasetIds,
      @Bind("collectionId") Integer collectionId);

  @RegisterBeanMapper(value = DarCollectionSummary.class)
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = Election.class)
  @UseRowReducer(DarCollectionSummaryReducer.class)
  @SqlQuery(
      """
              SELECT c.collection_id as dar_collection_id, c.dar_code, latest_dar.submission_date,
                latest_dar.reference_id as latest_dar_reference_id, latest_dar.parent_id as latest_dar_parent_id,
                latest_dar.requires_so_approval as latest_dar_requires_so_approval,
                latest_dar.approving_so_id as latest_dar_so_approver_id,
                latest_dar.approving_so_timestamp as latest_dar_so_approver_timestamp,
                latest_dar.update_date as latest_dar_update_date,
                u.display_name as researcher_name,
                u.user_id as researcher_id, i.institution_name, i.institution_id, e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id as dd_datasetid,
                dac.name AS dac_name,
                latest_dar.data ->> 'projectTitle' AS name,
                latest_dar.data ->> 'status' AS dar_status,
                latest_dar.data ->> 'closeoutSupplement' AS closeout,
                latest_dar.data ->> 'nonTechRus' AS non_tech_rus,
                ARRAY_AGG(dar_all.reference_id) AS reference_ids
              FROM dar_collection c
              INNER JOIN users u
              ON u.user_id = c.create_user_id
              LEFT JOIN institution i
              ON i.institution_id = u.institution_id
              INNER JOIN (
               SELECT DISTINCT ON (collection_id) *
               FROM data_access_request
               WHERE submission_date IS NOT NULL
               AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)
               ORDER BY collection_id, submission_date DESC
              ) latest_dar ON latest_dar.collection_id = c.collection_id
              INNER JOIN
               data_access_request dar_all ON dar_all.collection_id = c.collection_id
               AND dar_all.submission_date IS NOT NULL
               AND (LOWER(dar_all.data->>'status') != 'archived' OR dar_all.data->>'status' IS NULL)
              LEFT JOIN (
                SELECT election.*, MAX(election.election_id) OVER(PARTITION BY election.reference_id, election.dataset_id) AS latest
                FROM election
                WHERE LOWER(election.election_type) = 'dataaccess'
              ) AS e
              ON e.reference_id = latest_dar.reference_id
              INNER JOIN dar_dataset dd
              ON latest_dar.reference_id = dd.reference_id
              LEFT JOIN dataset ON dataset.dataset_id = dd.dataset_id
              LEFT JOIN dac ON dac.dac_id = dataset.dac_id AND dac.deleted IS NOT TRUE
              WHERE c.collection_id = :collectionId
                AND (e.latest = e.election_id OR e.election_id IS NULL)
              GROUP BY
                c.collection_id, c.dar_code, latest_dar.submission_date, latest_dar.reference_id, latest_dar.parent_id,
                latest_dar.requires_so_approval, latest_dar.approving_so_id, latest_dar.approving_so_timestamp,
                latest_dar.update_date, u.display_name, u.user_id, i.institution_name,
                i.institution_id, e.election_id, e.status, e.dataset_id, e.reference_id, dd.dataset_id, latest_dar.data, dac.name
          """)
  DarCollectionSummary getDarCollectionSummaryByCollectionId(
      @Bind("collectionId") Integer collectionId);
}
