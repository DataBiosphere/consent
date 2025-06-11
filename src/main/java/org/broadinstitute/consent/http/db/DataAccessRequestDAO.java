package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;
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
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
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
      """
          SELECT collection.dar_code, dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, 
            dar.parent_id, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
            (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, dar.era_commons_id,
            dar.closeout_so_approval_timestamp, dar.closeout_approving_so_id
          FROM data_access_request dar
          LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id
          LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
          WHERE dar.submission_date is not null
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)""")
  List<DataAccessRequest> findAllDataAccessRequests();

  /**
   * This query finds DARs submitted within the last year on dar-dataset combinations where the most
   * recent vote is true. The query accomplishes this by creating a view that is a grouping of
   * election reference ids and LAST vote in the group of final votes for all data access elections.
   * We need to group them due to the case of multiple elections on a dar-dataset request. Election
   * 1 may have been denied. Election 2 may have been approved. Election 3 may have been denied
   * again. When we partition over the election reference id, we'll get all final votes. The
   * `LAST_VALUE` function selects the last result in the partition, which would be `FALSE` in the
   * example above. Outside the JOIN, we filter on groupings where the final vote value is `TRUE` so
   * the denied election in the example would be filtered out.
   *
   * @param datasetId The dataset id
   * @return List of approved DARs for the dataset
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery("""
      SELECT dar.id, dar.reference_id, dar.collection_id, dar.parent_id,
        dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
        (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data,
        dd.dataset_id, collection.dar_code, dar.era_commons_id,
        dar.closeout_so_approval_timestamp, dar.closeout_approving_so_id
      FROM data_access_request dar
      LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
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
      WHERE dar.submission_date > now() - interval '1 year'
      AND final_access_vote.last_vote = TRUE
      AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
      -- Exclude DARs that have a closeoutSupplement
      AND dar.collection_id NOT IN (
        SELECT DISTINCT collection_id
        FROM data_access_request
        WHERE (regexp_replace(data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'closeoutSupplement' IS NOT NULL)
      """)
  List<DataAccessRequest> findApprovedDARsByDatasetId(@Bind("datasetId") Integer datasetId);

  /**
   * This query finds dataset ids on dar-dataset combinations where the most recent vote is true.
   * This includes datasets that are a part of expired DARs, UNLIKE findApprovedDARsByDatasetId.
   * The query accomplishes this by creating a view that is a grouping
   * of election reference ids and LAST vote in the group of final votes for all data access
   * elections. We need to group them due to the case of multiple elections on a dar-dataset
   * request. Election 1 may have been denied. Election 2 may have been approved. Election 3 may
   * have been denied again. When we partition over the election reference id, we'll get all final
   * votes. The `LAST_VALUE` function selects the last result in the partition, which would be
   * `FALSE` in the example above. Outside the JOIN, we filter on groupings where the final vote
   * value is `TRUE` so the denied election in the example would be filtered out.
   *
   * @param darReferenceId The DARs reference UUID
   * @return Set of approved Dataset Ids for the list of DARs
   */
  @SqlQuery(
      """
      SELECT dd.dataset_id
      FROM data_access_request dar
      INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
      INNER JOIN (
        SELECT DISTINCT e.reference_id, e.dataset_id, LAST_VALUE(v.vote)
        OVER(
          PARTITION BY e.dataset_id
            ORDER BY v.createdate
            RANGE BETWEEN
              UNBOUNDED PRECEDING AND
              UNBOUNDED FOLLOWING
        ) last_vote
        FROM election e
        INNER JOIN vote v ON e.election_id = v.electionid AND v.vote IS NOT NULL
        AND LOWER(e.election_type) = 'dataaccess'
        AND LOWER(v.type) = 'final') final_access_vote ON
          final_access_vote.reference_id = dar.reference_id AND
          final_access_vote.dataset_id = dd.dataset_id
      WHERE final_access_vote.last_vote = TRUE
        AND dar.reference_id = :darReferenceId
        AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
        -- Exclude DARs that have a closeoutSupplement
        AND dar.collection_id NOT IN (
          SELECT DISTINCT collection_id
          FROM data_access_request
          WHERE (regexp_replace(data #>> '{}', '\\\\u0000', '', 'g'))::jsonb ->> 'closeoutSupplement' IS NOT NULL)
      """)
  Set<Integer> findDatasetApprovalsByDar(@Bind("darReferenceId") String darReferenceId);

  /**
   * This query finds submitted DARs based on a date range.  This would be useful if we wanted to
   * send notifications for "expiring" DARs 30 days before expiration and again at 7 days.
   *
   * @param emailType - Type of email message associated with a DAR
   * @param interval - The POSTGRESQL time interval.  This value will be subtracted from now()
   * @return List of submitted DARs within the date range provided.
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      """
              SELECT dar.id, dar.reference_id, dar.collection_id, dar.parent_id,
                dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
                (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data,
                dd.dataset_id, collection.dar_code, dar.era_commons_id,
                dar.closeout_so_approval_timestamp, dar.closeout_approving_so_id
              FROM data_access_request dar
              LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
              LEFT JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
              LEFT OUTER JOIN email_entity email ON email.entity_reference_id = dar.reference_id AND email.email_type = :emailType
              WHERE dar.submission_date >= :notBefore
              AND (dar.submission_date < now() - :interval ::interval)
              AND (email.email_type IS NULL)
          
          """)
  List<DataAccessRequest> findAgedDARsByEmailTypeOlderThanInterval(@Bind("emailType") Integer emailType,
      @Bind("interval") String interval, @Bind("notBefore") Timestamp notBefore);

  /**
   * Find all draft/partial DataAccessRequests, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      """
              SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id,
              dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
              (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, collection.dar_code, 
              dar.era_commons_id, dar.closeout_so_approval_timestamp, dar.closeout_approving_so_id
              FROM data_access_request dar
              LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
              LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id
              WHERE dar.submission_date is null
                AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
              ORDER BY dar.update_date DESC
          """)
  List<DataAccessRequest> findAllDraftDataAccessRequests();

  /**
   * Find all draft/partial DataAccessRequests by user id, sorted descending order
   *
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      """
              SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id,
              dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
              (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, 
              collection.dar_code, dar.era_commons_id, dar.closeout_so_approval_timestamp,
              dar.closeout_approving_so_id
              FROM data_access_request dar
              LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
              LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id
              WHERE dar.submission_date is null
                AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
                AND dar.user_id = :userId
              ORDER BY dar.sort_date DESC
          """)
  List<DataAccessRequest> findAllDraftsByUserId(@Bind("userId") Integer userId);

  /**
   * Find DataAccessRequest by reference id
   *
   * @param referenceId String
   * @return DataAccessRequest
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      """
              SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id,
                dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
                (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data,
                collection.dar_code, dar.era_commons_id, dar.closeout_so_approval_timestamp,
                dar.closeout_approving_so_id
              FROM data_access_request dar
              LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
              LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id
              WHERE dar.reference_id = :referenceId
                AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
          """)
  DataAccessRequest findByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find DataAccessRequests by reference ids
   *
   * @param referenceIds List of Strings
   * @return List<DataAccessRequest>
   */
  @UseRowReducer(DataAccessRequestReducer.class)
  @SqlQuery(
      """
          SELECT dd.dataset_id, dar.id, dar.reference_id, dar.collection_id, dar.parent_id, dar.user_id, dar.create_date, dar.sort_date, dar.submission_date, dar.update_date,
            (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, collection.dar_code,
            dar.era_commons_id, dar.closeout_so_approval_timestamp, dar.closeout_approving_so_id
          FROM data_access_request dar
          LEFT JOIN dar_collection collection on collection.collection_id = dar.collection_id
          LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id
          WHERE dar.reference_id IN (<referenceIds>)
            AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
      """)
  List<DataAccessRequest> findByReferenceIds(@BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING) List<String> referenceIds);

  /**
   * Update DataAccessRequest properties by reference id.
   *
   * @param referenceId    String
   * @param userId         Integer User
   * @param sortDate       Date Sorting Date
   * @param submissionDate Date Submission Date
   * @param updateDate     Date Update Date
   * @param data           DataAccessRequestData DAR Properties
   * @param eraCommonsId   The user's era commons id at the time of update
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      """
          UPDATE data_access_request
          SET data = to_jsonb(regexp_replace(:data, '\\\\u0000', '', 'g')), user_id = :userId, sort_date = :sortDate,
            submission_date = :submissionDate, update_date = :updateDate, era_commons_id = :eraCommonsId
          WHERE reference_id = :referenceId
      """)
  void updateDataByReferenceId(
      @Bind("referenceId") String referenceId,
      @Bind("userId") Integer userId,
      @Bind("sortDate") Date sortDate,
      @Bind("submissionDate") Date submissionDate,
      @Bind("updateDate") Date updateDate,
      @Bind("data") @Json DataAccessRequestData data,
      @Bind("eraCommonsId") String eraCommonsId);

  /**
   * Delete DataAccessRequest by reference id
   *
   * @param referenceId String
   */
  @SqlUpdate(
    """
        DELETE FROM data_access_request WHERE reference_id = :referenceId
    """)
  void deleteByReferenceId(@Bind("referenceId") String referenceId);


  @SqlUpdate("DELETE FROM data_access_request WHERE reference_id IN (<referenceIds>)")
  void deleteByReferenceIds(@BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING) List<String> referenceIds);

  @SqlUpdate(
      """
          UPDATE data_access_request dar
          SET data=jsonb_set((regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb, '{status}', '"Canceled"')
          WHERE reference_id IN (<referenceIds>)
      """)
  void cancelByReferenceIds(@BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING) List<String> referenceIds);

  /**
   * Delete all DataAccessRequests with the given collection id
   *
   * @param collectionId Integer
   */
  @SqlUpdate(
    """
        DELETE FROM data_access_request WHERE collection_id = :collectionId
    """)
  void deleteByCollectionId(@Bind("collectionId") Integer collectionId);

  /**
   * Create new DataAccessRequest in draft status
   *
   * @param referenceId String
   * @param userId      Integer User
   * @param createDate  Date Creation Date
   * @param sortDate    Date Sorting Date
   * @param updateDate  Date Update Date
   * @param data        DataAccessRequestData DAR Properties
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      """
          INSERT INTO data_access_request
            (reference_id, user_id, create_date, sort_date, update_date, data)
          VALUES (:referenceId, :userId, :createDate, :sortDate,
            :updateDate, to_jsonb(:data))
      """)
  void insertDraftDataAccessRequest(
      @Bind("referenceId") String referenceId,
      @Bind("userId") Integer userId,
      @Bind("createDate") Date createDate,
      @Bind("sortDate") Date sortDate,
      @Bind("updateDate") Date updateDate,
      @Bind("data") @Json DataAccessRequestData data);

  /**
   * Create new DataAccessRequest. This version supersedes `insertV2`
   *
   * @param collectionId   Integer DarCollection
   * @param referenceId    String
   * @param userId         Integer User
   * @param createDate     Date Creation Date
   * @param sortDate       Date Sorting Date
   * @param submissionDate Date Submission Date
   * @param updateDate     Date Update Date
   * @param data           DataAccessRequestData DAR Properties
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      """
          INSERT INTO data_access_request
            (collection_id, reference_id, user_id, create_date, sort_date, submission_date, update_date, data, era_commons_id)
          VALUES (:collectionId, :referenceId, :userId, :createDate, :sortDate,
            :submissionDate, :updateDate, to_jsonb(:data), :eraCommonsId)
      """)
  void insertDataAccessRequest(
      @Bind("collectionId") Integer collectionId,
      @Bind("referenceId") String referenceId,
      @Bind("userId") Integer userId,
      @Bind("createDate") Date createDate,
      @Bind("sortDate") Date sortDate,
      @Bind("submissionDate") Date submissionDate,
      @Bind("updateDate") Date updateDate,
      @Bind("data") @Json DataAccessRequestData data,
      @Bind("eraCommonsId") String eraCommonsId);

  /**
   * Create new Progress Report.
   *
   * @param parentId       String Parent ID
   * @param collectionId   Integer DarCollection
   * @param referenceId    String
   * @param userId         Integer User
   * @param data           DataAccessRequestData DAR Properties
   */
  @RegisterArgumentFactory(JsonArgumentFactory.class)
  @SqlUpdate(
      """
          INSERT INTO data_access_request
            (parent_id, collection_id, reference_id, user_id, create_date, sort_date, submission_date, update_date, data)
          VALUES (:parentId, :collectionId, :referenceId, :userId, now(), now(), now(), now(), to_jsonb(:data))
      """)
  void insertProgressReport(
      @Bind("parentId") Integer parentId,
      @Bind("collectionId") Integer collectionId,
      @Bind("referenceId") String referenceId,
      @Bind("userId") Integer userId,
      @Bind("data") @Json DataAccessRequestData data);


  /**
   * Converts a Draft DataAccessRequest into a non-draft DataAccessRequest
   *
   * @param referenceId String
   */
  @SqlUpdate(
      """
          UPDATE data_access_request
            SET  submission_date = now(), collection_id = :collectionId
          WHERE reference_id = :referenceId
       """)
  void updateDraftToSubmittedForCollection(@Bind("collectionId") Integer collectionId,
      @Bind("referenceId") String referenceId);

  @SqlUpdate(
      """
         UPDATE data_access_request
          SET data = jsonb_set ((data #>> '{}')::jsonb, '{status}', '"Archived"', true)
          WHERE reference_id IN (<referenceIds>)
      """)
  void archiveByReferenceIds(@BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING) List<String> referenceIds);

  @SqlUpdate(
    """
        UPDATE data_access_request
          SET closeout_approving_so_id = :id, closeout_so_approval_timestamp = now()
        WHERE reference_id = :referenceId
    """)
  void updateDarCloseoutSO(@Bind("id") Integer signingOfficialUserId, @Bind("referenceId") String referenceId);

  /**
   * Inserts into dar_dataset collection
   *
   * @param referenceId String
   * @param datasetId Integer
   */
  @SqlUpdate(
      """
          INSERT INTO dar_dataset (reference_id, dataset_id)
          VALUES (:referenceId, :datasetId)
          ON CONFLICT DO NOTHING
      """)
  void insertDARDatasetRelation(
      @Bind("referenceId") String referenceId, @Bind("datasetId") Integer datasetId);

  @SqlBatch(
      """
          INSERT INTO dar_dataset (reference_id, dataset_id)
          VALUES (:referenceId, :datasetId)
          ON CONFLICT DO NOTHING
      """)
  void insertAllDarDatasets(@BindBean List<DarDataset> darDatasets);

  /**
   * Delete rows which have the given reference id
   *
   * @param referenceId String
   */
  @SqlUpdate(
      """
          DELETE FROM dar_dataset WHERE reference_id = :referenceId
       """)
  void deleteDARDatasetRelationByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Delete rows which have a referenceId that is in the list referenceIds
   *
   * @param referenceIds List<String>
   */
  @SqlUpdate("DELETE FROM dar_dataset WHERE reference_id in (<referenceIds>)")
  void deleteDARDatasetRelationByReferenceIds(@BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING) List<String> referenceIds);

  /**
   * Returns all dataset_ids that match any of the referenceIds inside the "referenceIds" list
   *
   * @param referenceIds List<String>
   */
  @SqlQuery("SELECT distinct dataset_id FROM dar_dataset WHERE reference_id IN (<referenceIds>)")
  List<Integer> findAllDARDatasetRelations(@BindList(value = "referenceIds", onEmpty = EmptyHandling.NULL_STRING) List<String> referenceIds);

}
