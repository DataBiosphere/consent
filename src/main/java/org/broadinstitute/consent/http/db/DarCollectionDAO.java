package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface DarCollectionDAO {
  /**
   * Find all DARCollections with their DataAccessRequests
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
    "SELECT * FROM dar_collection c "
     + " LEFT JOIN "
     + "    (SELECT id, reference_id, collection_id, draft, user_id, create_date, sort_date, submission_date, "
     + "            update_date, (data #>> '{}')::jsonb AS data FROM data_access_request) dar "
     + " ON c.collection_id = dar.collection_id ")
  List<DarCollection> findAllDARCollections();

  /**
   * Find all DARCollections with their DataAccessRequests
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " INNER JOIN "
      + "    (SELECT id, reference_id, collection_id, draft, user_id, create_date, sort_date, submission_date, "
      + "            update_date, (data #>> '{}')::jsonb AS data FROM data_access_request" +
      "      WHERE reference_id = :referenceId) dar "
      + " ON c.collection_id = dar.collection_id ")
  DarCollection findDARCollectionByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find all DARCollections with their DataAccessRequests
   *
   * @return List<DataAccessRequest>
   */
  @SqlQuery(
    "SELECT * FROM dar_collection c "
      + " LEFT JOIN "
      + "    (SELECT id, reference_id, collection_id, draft, user_id, create_date, sort_date, submission_date, "
      + "            update_date, (data #>> '{}')::jsonb AS data FROM data_access_request) dar "
      + " ON c.collection_id = dar.collection_id "
      + " WHERE c.collection_id = :collectionId ")
  DarCollection findDARCollectionByCollectionId(@Bind("collectionId") String collectionId);

}

