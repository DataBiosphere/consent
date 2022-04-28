package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DataAccessRequestDataMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestMapper;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.json.internal.JsonArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

/**
 * For all json queries, note the double `??` for jdbi3 escaped jsonb operators:
 * https://jdbi.org/#_postgresql
 */
@RegisterRowMapper(DataAccessRequestMapper.class)
public interface DataAccessRequestDAO extends Transactional<DataAccessRequestDAO> {

  /**
   * Find all non-draft/partial DataAccessRequests
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + "  WHERE not (data #>> '{}')::jsonb ??| array['partial_dar_code', 'partialDarCode'] "
          + "  AND draft != true "
          + "  AND (data->>'status' != 'Archived' OR data->>'status' IS NULL ) ")
  List<DataAccessRequest> findAllDataAccessRequests();


  /**
   * Find all non-draft DataAccessRequests for the given datasetId
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, "
          + "(data #>> '{}')::jsonb AS data FROM data_access_request "
          + " WHERE draft = false"
          + " AND ((data #>> '{}')::jsonb->>'datasetIds')::jsonb @> :datasetId::jsonb"
          + " AND (data->>'status' != 'Archived' OR data->>'status' IS NULL) ")
  List<DataAccessRequest> findAllDataAccessRequestsByDatasetId(@Bind("datasetId") String datasetId);

  /**
   * Find all draft/partial DataAccessRequests, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + "  WHERE (data #>> '{}')::jsonb ??| array['partial_dar_code', 'partialDarCode'] "
          + "  OR draft = true "
          + "  AND (data->>'status' != 'Archived' OR data->>'status' IS NULL) "
          + "  ORDER BY update_date DESC")
  List<DataAccessRequest> findAllDraftDataAccessRequests();

  /**
   * Find all draft/partial DataAccessRequests by user id, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + "  WHERE ( (data #>> '{}')::jsonb ??| array['partial_dar_code', 'partialDarCode'] "
          + "          OR draft = true "
          + "  AND (data->>'status' != 'Archived' OR data->>'status' IS NULL)) "
          + "  AND user_id = :userId "
          + "  ORDER BY sort_date DESC")
  List<DataAccessRequest> findAllDraftsByUserId(@Bind("userId") Integer userId);


  /**
   * Find all complete DataAccessRequests by user id, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + "  WHERE draft = false "
          + "  AND user_id = :userId "
          + "  AND (data->>'status' != 'Archived' OR data->>'status' IS NULL) "
          + "  ORDER BY sort_date DESC")
  List<DataAccessRequest> findAllDarsByUserId(@Bind("userId") Integer userId);

  /**
   * Find DataAccessRequest by reference id
   *
   * @param referenceId String
   * @return DataAccessRequest
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id,  draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
          + "WHERE reference_id = :referenceId "
          + "AND (data->>'status' != 'Archived' OR data->>'status' IS NULL) "
          + "limit 1 ")
  DataAccessRequest findByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find DataAccessRequests by reference ids
   *
   * @param referenceIds List of Strings
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
      "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
        + "WHERE reference_id IN (<referenceIds>) "
        + "AND (data->>'status' != 'Archived' OR data->>'status' IS NULL) ")
  List<DataAccessRequest> findByReferenceIds(@BindList("referenceIds") List<String> referenceIds);

  /**
   * Update DataAccessRequest by reference id and provided DataAccessRequestData
   * Deprecated. Use `updateDataByReferenceIdVersion2`
   *
   * @param referenceId String
   * @param data DataAccessRequestData
   */
  @Deprecated
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      "UPDATE data_access_request SET data = to_jsonb(:data) WHERE reference_id = :referenceId")
  void updateDataByReferenceId(
      @Bind("referenceId") String referenceId, @Bind("data") @Json DataAccessRequestData data);

  /**
   * Update DataAccessRequest properties by reference id.
   * This version supercedes updateDataByReferenceId.
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
      "UPDATE data_access_request SET data = to_jsonb(:data), user_id = :userId, sort_date = :sortDate, submission_date = :submissionDate, update_date = :updateDate WHERE reference_id = :referenceId")
  void updateDataByReferenceIdVersion2(
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

  @SqlUpdate(
      "UPDATE data_access_request dar SET data=jsonb_set((dar.data #>> '{}')::jsonb, '{status}', '\"Canceled\"')" +
      "WHERE reference_id IN (<referenceIds>)"
    )
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
      "INSERT INTO data_access_request (reference_id, user_id, create_date, sort_date, submission_date, update_date, data, draft) VALUES (:referenceId, :userId, :createDate, :sortDate, :submissionDate, :updateDate, to_jsonb(:data), true) ")
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
    "INSERT INTO data_access_request (collection_id, reference_id, user_id, create_date, sort_date, submission_date, update_date, data) VALUES (:collectionId, :referenceId, :userId, :createDate, :sortDate, :submissionDate, :updateDate, to_jsonb(:data)) ")
  void insertVersion3(
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
  @SqlUpdate("UPDATE data_access_request SET draft = :draft WHERE reference_id = :referenceId ")
  void updateDraftByReferenceId(@Bind("referenceId") String referenceId, @Bind("draft") Boolean draft);

  @SqlUpdate("UPDATE data_access_request SET draft = false, collection_id = :collectionId WHERE reference_id = :referenceId")
  void updateDraftForCollection(@Bind("collectionId") Integer collectionId, @Bind("referenceId") String referenceId);

  @RegisterRowMapper(DataAccessRequestDataMapper.class)
  @SqlQuery(" SELECT (data #>> '{}')::jsonb AS data FROM data_access_request "
          + "WHERE (data->>'status' != 'Archived' OR data->>'status' IS NULL) ")
  List<DataAccessRequestData> findAllDataAccessRequestDatas();

  /**
   * Find all non-draft/partial DataAccessRequests for users in the given institution
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
    "SELECT id, reference_id, collection_id, parent_id, draft, user_id, create_date, sort_date, submission_date, update_date, (data #>> '{}')::jsonb AS data FROM data_access_request "
        + " INNER JOIN dacuser d on d.dacuserid = user_id AND d.institution_id = :institutionId "
        + " WHERE draft != true "
        + " AND (data->>'status' != 'Archived' OR data->>'status' IS NULL) ")
  List<DataAccessRequest> findAllDataAccessRequestsForInstitution(@Bind("institutionId") Integer institutionId);

  @SqlUpdate(
      " UPDATE data_access_request"
          + " SET data = jsonb_set ((data #>> '{}')::jsonb, '{status}', '\"Archived\"', true) "
          + " WHERE reference_id IN (<referenceIds>)")
  void archiveByReferenceIds(@BindList("referenceIds") List<String> referenceIds);
}
