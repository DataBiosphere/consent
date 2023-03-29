package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestDataMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestReducer;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.internal.JsonArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

/**
 * For all json queries, note the double `??` for jdbi3 escaped jsonb operators:
 * <a href="https://jdbi.org/#_postgresql">...</a>
 */
@RegisterRowMapper(DataAccessRequestMapper.class)
public interface DataAccessRequestDAO extends Transactional<DataAccessRequestDAO> {

  /**
   * Find all non-draft/partial DataAccessRequests
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      "SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
        + "  (dar.data #>> '{}')::jsonb AS data FROM data_access_request dar"
        + "  LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
        + "  WHERE dar.draft != true "
          + "  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)")
  List<DataAccessRequest> findAllDataAccessRequests();


  /**
   * Find all non-draft, approved DataAccessRequests for the given datasetId
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
          " SELECT distinct e.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, "
              + " dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
              + " (dar.data #>> '{}')::jsonb AS data "
              + " FROM data_access_request dar "
              + " INNER JOIN election e "
              + " ON e.reference_id = dar.reference_id "
              + " INNER JOIN vote v "
              + " ON e.election_id = v.electionid "
              + " WHERE lower(v.type) = 'final' AND v.vote = true AND e.dataset_id = :datasetId "
              + " AND e.archived IS NOT true AND lower(e.election_type) = 'dataaccess'"
  )
  List<DataAccessRequest> findAllApprovedDataAccessRequestsByDatasetId(@Bind("datasetId") Integer datasetId);


  @SqlQuery("""
          SELECT dar.user_id
          FROM data_access_request dar
          INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id AND dd.dataset_id = :datasetId
          INNER JOIN (
            SELECT DISTINCT e.reference_id, LAST_VALUE(v.vote)
            OVER(
              PARTITION BY e.reference_id
                ORDER BY v.createdate
                RANGE BETWEEN
                  UNBOUNDED PRECEDING AND
                  UNBOUNDED FOLLOWING
            ) last_vote
            FROM election e
            INNER JOIN vote v ON e.election_id = v.electionid AND v.vote IS NOT NULL
            WHERE e.dataset_id = :datasetId
            AND LOWER(e.election_type) = 'dataaccess'
            AND LOWER(v.type) = 'final') final_access_vote ON final_access_vote.reference_id = dar.reference_id
          WHERE dar.draft = false
          AND final_access_vote.last_vote = TRUE
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
      """)
  List<Integer> findAllUserIdsWithApprovedDARsByDatasetId(@Bind("datasetId") Integer datasetId);

  /**
   * Find all draft/partial DataAccessRequests, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      "SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
        + "  (dar.data #>> '{}')::jsonb AS data FROM data_access_request dar"
        + "  LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
        + "  WHERE dar.draft = true "
          + "  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL) "
        + "  ORDER BY dar.update_date DESC")
  List<DataAccessRequest> findAllDraftDataAccessRequests();

  /**
   * Find all draft/partial DataAccessRequests by user id, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      "SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
        + "  (dar.data #>> '{}')::jsonb AS data FROM data_access_request dar"
        + "  LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
        + "  WHERE dar.draft = true "
          + "  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL) "
          + "  AND dar.user_id = :userId "
        + "  ORDER BY dar.sort_date DESC")
  List<DataAccessRequest> findAllDraftsByUserId(@Bind("userId") Integer userId);


  /**
   * Find all complete DataAccessRequests by user id, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      "SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
        + "  (dar.data #>> '{}')::jsonb AS data FROM data_access_request dar"
        + "  LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
        + "  WHERE dar.draft = false "
          + "  AND dar.user_id = :userId "
          + "  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL) "
        + "  ORDER BY dar.sort_date DESC")
  List<DataAccessRequest> findAllDarsByUserId(@Bind("userId") Integer userId);

  /**
   * Find DataAccessRequest by reference id
   *
   * @param referenceId String
   * @return DataAccessRequest
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      "SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
        + "  (dar.data #>> '{}')::jsonb AS data FROM data_access_request dar"
        + "  LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
        + "  WHERE dar.reference_id = :referenceId "
          + "  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)")
  DataAccessRequest findByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find DataAccessRequests by reference ids
   *
   * @param referenceIds List of Strings
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      "SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, "
        + "  (dar.data #>> '{}')::jsonb AS data FROM data_access_request dar"
        + "  LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
        + "  WHERE dar.reference_id IN (<referenceIds>) "
          + "  AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)")
  List<DataAccessRequest> findByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  /**
   * Update DataAccessRequest properties by reference id.
   *
   * @param referenceId String
   * @param userId Integer User
   * @param sortDate Date Sorting Date
   * @param submissionDate Date Submission Date
   * @param updateDate Date Update Date
   * @param data DataAccessRequestData DAR Properties
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      "UPDATE data_access_request "
        + "SET data = to_jsonb(:data), user_id = :userId, sort_date = :sortDate, "
          + "submission_date = :submissionDate, update_date = :updateDate "
        + "WHERE reference_id = :referenceId")
  void updateDataByReferenceId(
      @Bind("referenceId") String referenceId,
      @Bind("userId") Integer userId,
      @Bind("sortDate") Date sortDate,
      @Bind("submissionDate") Date submissionDate,
      @Bind("updateDate") Date updateDate,
      @Bind("data") @Json DataAccessRequestData data);

  /**
   * Delete DataAccessRequest by reference id
   *
   * @param referenceId String
   */
  @SqlUpdate("DELETE FROM data_access_request WHERE reference_id = :referenceId")
  void deleteByReferenceId(@Bind("referenceId") String referenceId);


  @SqlUpdate("DELETE FROM data_access_request WHERE reference_id IN (<referenceIds>)")

  void deleteByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  @SqlUpdate(
      "UPDATE data_access_request dar "
        + "SET data=jsonb_set((dar.data #>> '{}')::jsonb, '{status}', '\"Canceled\"') "
        + "WHERE reference_id IN (<referenceIds>)")
  void cancelByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  /**
   * Delete all DataAccessRequests with the given collection id
   *
   * @param collectionId Integer
   */
  @SqlUpdate("DELETE FROM data_access_request WHERE collection_id = :collectionId")
  void deleteByCollectionId(@Bind("collectionId") Integer collectionId);

  /**
   * Create new DataAccessRequest in draft status
   *
   * @param referenceId String
   * @param userId Integer User
   * @param createDate Date Creation Date
   * @param sortDate Date Sorting Date
   * @param submissionDate Date Submission Date
   * @param updateDate Date Update Date
   * @param data DataAccessRequestData DAR Properties
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      "INSERT INTO data_access_request "
        + "(reference_id, user_id, create_date, sort_date, submission_date, update_date, data, draft) "
        + "VALUES (:referenceId, :userId, :createDate, :sortDate, "
                + ":submissionDate, :updateDate, to_jsonb(:data), true)")
  void insertDraftDataAccessRequest(
      @Bind("referenceId") String referenceId,
      @Bind("userId") Integer userId,
      @Bind("createDate") Date createDate,
      @Bind("sortDate") Date sortDate,
      @Bind("submissionDate") Date submissionDate,
      @Bind("updateDate") Date updateDate,
      @Bind("data") @Json DataAccessRequestData data);

  /**
   * Create new DataAccessRequest.
   * This version supercedes `insertV2`
   *
   * @param collectionId Integer DarCollection
   * @param referenceId String
   * @param userId Integer User
   * @param createDate Date Creation Date
   * @param sortDate Date Sorting Date
   * @param submissionDate Date Submission Date
   * @param updateDate Date Update Date
   * @param data DataAccessRequestData DAR Properties
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
    "INSERT INTO data_access_request "
      + "(collection_id, reference_id, user_id, create_date, sort_date, submission_date, update_date, data) "
      + "VALUES (:collectionId, :referenceId, :userId, :createDate, :sortDate, " +
                ":submissionDate, :updateDate, to_jsonb(:data))")
  void insertDataAccessRequest(
    @Bind("collectionId") Integer collectionId,
    @Bind("referenceId") String referenceId,
    @Bind("userId") Integer userId,
    @Bind("createDate") Date createDate,
    @Bind("sortDate") Date sortDate,
    @Bind("submissionDate") Date submissionDate,
    @Bind("updateDate") Date updateDate,
    @Bind("data") @Json DataAccessRequestData data);

  /**
   * Converts a Draft DataAccessRequest into a non-draft DataAccessRequest
   *
   * @param referenceId String
   */
  @SqlUpdate(
          "UPDATE data_access_request "
            + "SET draft = :draft "
            + "WHERE reference_id = :referenceId")
  void updateDraftByReferenceId(@Bind("referenceId") String referenceId, @Bind("draft") Boolean draft);

  @SqlUpdate(
          "UPDATE data_access_request "
            + "SET draft = :draft "
            + "WHERE collection_id = :collectionId")
  void updateDraftByCollectionId(@Bind("collectionId") Integer collectionId, @Bind("draft") Boolean draft);

  @SqlUpdate(
          "UPDATE data_access_request "
            + "SET draft = false, collection_id = :collectionId "
            + "WHERE reference_id = :referenceId")
  void updateDraftForCollection(@Bind("collectionId") Integer collectionId, @Bind("referenceId") String referenceId);

  @RegisterRowMapper(DataAccessRequestDataMapper.class)
  @SqlQuery(
          "SELECT (data #>> '{}')::jsonb AS data "
            + "FROM data_access_request "
            + "WHERE (LOWER(data->>'status') != 'archived' "
              + "OR data->>'status' IS NULL)")
  List<DataAccessRequestData> findAllDataAccessRequestDatas();

  /**
   * Find all non-draft/partial DataAccessRequests for users in the given institution
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery("SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.draft, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date, (dar.data #>> '{}')::jsonb AS data "
      + " FROM data_access_request dar "
      + " INNER JOIN users u ON u.user_id = dar.user_id AND u.institution_id = :institutionId "
      + " LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
      + " WHERE dar.draft != true "
        + " AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL)")
  List<DataAccessRequest> findAllDataAccessRequestsForInstitution(@Bind("institutionId") Integer institutionId);

  @SqlUpdate(
      " UPDATE data_access_request"
        + " SET data = jsonb_set ((data #>> '{}')::jsonb, '{status}', '\"Archived\"', true) "
        + " WHERE reference_id IN (<referenceIds>)")
  void archiveByReferenceIds(@BindList("referenceIds") List<String> referenceIds);


  /**
   * Inserts into dar_dataset collection
   *
   * @param referenceId String
   * @param datasetId Integer
   */
  @SqlUpdate(
          "INSERT INTO dar_dataset (reference_id, dataset_id) "
            + "VALUES (:referenceId, :datasetId) "
            + "ON CONFLICT DO NOTHING")
  void insertDARDatasetRelation(@Bind("referenceId") String referenceId, @Bind("datasetId") Integer datasetId);

  @SqlBatch(
          "INSERT INTO dar_dataset (reference_id, dataset_id) "
            + "VALUES (:referenceId, :datasetId) "
            + "ON CONFLICT DO NOTHING")
  void insertAllDarDatasets(@BindBean List<DarDataset> darDatasets);

  /**
   * Delete rows which have the given reference id
   *
   * @param referenceId String
   */
  @SqlUpdate("DELETE FROM dar_dataset WHERE reference_id = :referenceId")
  void deleteDARDatasetRelationByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Delete rows which have a referenceId that is in the list referenceIds
   *
   * @param referenceIds List<String>
   */
  @SqlUpdate("DELETE FROM dar_dataset WHERE reference_id in (<referenceIds>)")
  void deleteDARDatasetRelationByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  /**
   * Returns all dataset_ids that match any of the referenceIds inside of the "referenceIds" list
   *
   * @param referenceIds List<String>
   */
  @SqlQuery("SELECT distinct dataset_id FROM dar_dataset WHERE reference_id IN (<referenceIds>)")
  List<Integer> findAllDARDatasetRelations(@BindList("referenceIds") List<String> referenceIds);

  @SqlQuery("SELECT distinct dataset_id FROM dar_dataset")
  List<Integer> findAllDARDatasetRelationDatasetIds();

}
