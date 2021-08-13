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
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT c.*, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, dar.draft AS dar_draft," +
      " dar.user_id AS dar_userId, dar.create_date AS dar_create_date, dar.submission_date AS dar_submission_date, " +
      " dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data FROM dar_collection c "
     + " LEFT JOIN data_access_request dar"
     + " ON c.collection_id = dar.collection_id "
  )
  List<DarCollection> findAllDARCollections();


  /**
   * Find all DARCollections with their DataAccessRequests that match the given filters
   *
   * FilterTerms filter on dar project title, datasetNames, collection dar code, collection update date, and DAC
   * SortField can be projectTitle, datasetNames, dar code, update date, or DAC
   * SortDirection can be ASC or DESC
   *
   * @return List<DarCollection>
   */

  //this is merely a rough draft, an idea, it does not work (CONTAINS can be replaced with @>)
  //look at changeSet 73, could do something like that: WHEN POSITION(filterTerm in  (data #>> '{}')::jsonb->>'dar_code') > 0
  // for ANY filterTerm in filterTerms

//  @RegisterBeanMapper(DarCollection.class)
//  @RegisterRowMapper(DataAccessRequestMapper.class)
//  @UseRowReducer(DarCollectionReducer.class)
//  @SqlQuery(
//    "SELECT * FROM dar_collection c "
//      + " LEFT JOIN "
//      + "    (SELECT id, reference_id, collection_id AS dar_collection_id, draft, user_id, create_date AS dar_create_date, "
//      + "            submission_date, update_date AS dar_update_date, (data #>> '{}')::jsonb AS data "
//      + "    FROM data_access_request) dar "
//      + " ON c.collection_id = dar.dar_collection_id "
//      + " WHERE (dar.data->>'projectTitle') AS project_title CONTAINS ANY filterTerms "
//      + " OR ((dar.data->>'datasets')->>'labels') AS datasets CONTAINS ANY filterTerms "
//      + " OR c.dar_code CONTAINS ANY filterTerms "
//      + " OR format(c.update_date) CONTAINS ANY filterTerms "
//      + " OR (SELECT name FROM dac "
//      + "        LEFT OUTER JOIN consents c ON c.dac_id = dac.dac_id "
//      + "        LEFT OUTER JOIN consentassociations ca ON ca.consentid = c.consentid "
//      + "        LEFT OUTER JOIN dataset d ON ca.datasetid = d.datasetid"m
//      + "    WHERE ((dar.data->>'datasetIds') CONTAINS d.datasetid) dacname "
//      + "    CONTAINS ANY filterTerms "
//      + " IF sortField = 'projectTitle' ORDER BY projectTitle :sortDirection "
//      + " IF sortField = 'datasets' ORDER BY projectTitle :sortDirection "
//      + " IF sortField = 'darCode' ORDER BY c.dar_code :sortDirection "
//      + " IF sortField = 'lastUpdated' ORDER BY c.update_date :sortDirection "
//      + " IF sortField = 'dac' ORDER BY dacname :sortDirection "
//      + " OFFSET :offset LIMIT :limit" )
//  List<DarCollection> findAllDARCollectionsWithFilters(@Bind("sortField") String sortField,
//                                                       @Bind("sortDirection") String sortDirection,
//                                                      @Bind("filterterms) List<String> filterTerms,
//                                                      @Bind("offset") Integer offset,
//                                                      @Bind("limit") Integer limit);
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

