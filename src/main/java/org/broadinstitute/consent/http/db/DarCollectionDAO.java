package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DarCollectionReducer;
import org.broadinstitute.consent.http.db.mapper.DataAccessRequestMapper;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import java.util.Date;
import java.util.List;

public interface DarCollectionDAO {
  /**
   * Find all DARCollections with their DataAccessRequests
   *
   * @return List<DarCollection>
   */
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
     + " LEFT JOIN "
     + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
     + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
     + "    FROM data_access_request) dar "
     + " ON c.collection_id = dar.dar_collection_id ")
  List<DarCollection> findAllDARCollections();


  /**
   * Find all DARCollections with their DataAccessRequests
   *
   * @return List<DarCollection>
   */
  @RegisterBeanMapper(DarCollection.class)
  @RegisterRowMapper(DataAccessRequestMapper.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " LEFT JOIN "
      + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
      + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
      + "    FROM data_access_request) dar "
      + " ON c.collection_id = dar.dar_collection_id ")
  List<DarCollection> findAllDARCollectionsWithFilters(String sortField, String sortDirection, List<String> filterTerms);
  /**
   * Find the DARCollection and all of its Data Access Requests that contains the DAR with the given referenceId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(DarCollection.class)
  @RegisterRowMapper(DataAccessRequestMapper.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " INNER JOIN "
      + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
      + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
      + "    FROM data_access_request"
      + "    WHERE reference_id = :referenceId) dar "
      + " ON c.collection_id = dar.dar_collection_id ")
  DarCollection findDARCollectionByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find the DARCollection and all of its Data Access Requests that has the given collectionId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(DarCollection.class)
  @RegisterRowMapper(DataAccessRequestMapper.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " LEFT JOIN "
      + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
      + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
      + "    FROM data_access_request) dar "
      + " ON c.collection_id = dar.dar_collection_id "
      + " WHERE c.collection_id = :collectionId ")
  DarCollection findDARCollectionByCollectionId(@Bind("collectionId") Integer collectionId);

  /**
   * Create a new DAR Collection with the given dar code, create user ID, and create date
   *
   * @return Integer, ID of newly created DarCollection
   */
  @SqlUpdate("INSERT INTO dar_collection " +
    " (dar_code, create_user, create_date) " +
    " VALUES (:darCode, :createUserId, :createDate)")
  @GetGeneratedKeys
  Integer insertDarCollection(@Bind("darCode") String darCode,
                            @Bind("createUserId") Integer createUserId,
                            @Bind("createDate") Date createDate);

  /**
   * Update the update user and update date of the DAR Collection with the given collection ID
   */
  @SqlUpdate("UPDATE dar_collection SET update_user = :updateUserId, update_date = :updateDate WHERE collection_id = :collectionId")
  void updateDarCollection(@Bind("collectionId") Integer collectionId,
                           @Bind("updateUserId") Integer updateUserId,
                           @Bind("updateDate") Date updateDate);

  /**
   * Delete the DAR Collection with the given collection ID
   */
  @SqlUpdate("DELETE FROM dar_collection WHERE collection_id = :collectionId")
  void deleteByCollectionId(@Bind("collectionId") Integer collectionId);
}

