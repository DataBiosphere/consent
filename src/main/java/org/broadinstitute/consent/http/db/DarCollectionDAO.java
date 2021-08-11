package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DarCollectionReducer;
import org.broadinstitute.consent.http.models.DarCollection;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
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
   * @return List<DataAccessRequest>
   */
  @RegisterBeanMapper(DarCollection.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
     + " LEFT JOIN "
     + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
     + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
     + "    FROM data_access_request) dar "
     + " ON c.collection_id = dar.collection_id ")
  List<DarCollection> findAllDARCollections();

  /**
   * Find the DARCollection and all of its Data Access Requests that contains the DAR with the given referenceId
   *
   * @return List<DataAccessRequest>
   */
  @RegisterBeanMapper(DarCollection.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " INNER JOIN "
      + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
      + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
      + "    FROM data_access_request"
      + "    WHERE reference_id = :referenceId) dar "
      + " ON c.collection_id = dar.collection_id ")
  DarCollection findDARCollectionByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find the DARCollection and all of its Data Access Requests that has the given collectionId
   *
   * @return List<DataAccessRequest>
   */
  @RegisterBeanMapper(DarCollection.class)
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " LEFT JOIN "
      + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
      + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
      + "    FROM data_access_request) dar "
      + " ON c.collection_id = dar.collection_id "
      + " WHERE c.collection_id = :collectionId ")
  DarCollection findDARCollectionByCollectionId(@Bind("collectionId") Integer collectionId);

  @SqlUpdate("INSERT INTO dar_collection " +
    " (dar_code, create_user, create_date) " +
    " VALUES (:darCode, :createUserId, :createDate)")
  @GetGeneratedKeys
  Integer insertDarCollection(@Bind("darCode") String darCode,
                            @Bind("createUserId") Integer createUserId,
                            @Bind("createDate") Date createDate);

  @SqlUpdate("DELETE * FROM dar_collection WHERE collection_id = :collectionId")
  void deleteByCollectionId(@Bind("collectionId") Integer collectionId);
}

