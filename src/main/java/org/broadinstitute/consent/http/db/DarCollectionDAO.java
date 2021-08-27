package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DarCollectionReducer;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import java.util.Date;
import java.util.List;

public interface DarCollectionDAO {

  final String getCollectionAndDars =
      "SELECT c.*, i.institution_name, u.displayname AS researcher, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, " +
          "dar.draft AS dar_draft, dar.user_id AS dar_userId, dar.create_date AS dar_create_date, " +
          "dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, " +
          "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data, " +
          "(dar.data #>> '{}')::jsonb ->> 'projectTitle' as projectTitle " +
      "FROM dar_collection c " + 
      "INNER JOIN dacuser u ON u.dacuserid = c.create_user " +
      "LEFT JOIN institution i ON i.institution_id = u.institution_id " +
      "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id ";
  
  final String filterQuery = 
    "WHERE COALESCE(i.institution_name, '') ~* :institutionSearchTerm " +
    "AND (dar.data #>> '{}')::jsonb ->> 'projectTitle' ~* :projectSearchTerm " +
    "AND u.displayname ~* :researcherSearchTerm " +
    "AND c.dar_code ~* :darCodeSearchTerm " + 
    "AND EXISTS " +
        "(SELECT FROM jsonb_array_elements((dar.data #>> '{}')::jsonb -> 'datasets') dataset " +
        "WHERE dataset ->> 'label' ~* :datasetSearchTerm)";

  final String projectSortFilterQuery = 
    getCollectionAndDars + filterQuery + 
    "ORDER BY <sortField> <sortOrder> " +
    "LIMIT :limit OFFSET :offset";
  
  /**
   * Find all DARCollections with their DataAccessRequests
   *
   * @return List<DarCollection>
   */
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    " SELECT c.*, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, " +
        "dar.draft AS dar_draft, dar.user_id AS dar_userId, dar.create_date AS dar_create_date, " +
        "dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, " +
        "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data " +
        "FROM dar_collection c, data_access_request dar " +
    " WHERE c.collection_id = dar.collection_id"
  )
  List<DarCollection> findAllDARCollections();

  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(" SELECT c.*, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
      + "dar.draft AS dar_draft, dar.user_id AS dar_userId, dar.create_date AS dar_create_date, "
      + "dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
      + "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data "
      + "FROM dar_collection c, data_access_request dar " 
      + "WHERE c.collection_id = dar.collection_id "
      + "AND c.create_user = :userId")
  List<DarCollection> findDARCollectionsCreatedByUserId(@Bind("userId") Integer researcherId);

  /**
   * Find all DARCollections with their DataAccessRequests that match the given filters
   *
   * FilterTerms filter on dar project title, datasetNames, collection dar code, collection update date, and DAC
   * SortField can be projectTitle, datasetNames, dar code, update date, or DAC
   * SortDirection can be ASC or DESC
   *
   * @return List<DarCollection>
   */

 @RegisterBeanMapper(value = DarCollection.class)
 @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
 @UseRowReducer(DarCollectionReducer.class)
 @SqlQuery(projectSortFilterQuery)
 List<DarCollection> findAllDARCollectionsWithFilters(
                                                    @Bind("institutionSearchTerm") String institutionSearchTerm,
                                                    @Bind("projectSearchTerm") String projectSearchTerm,
                                                    @Bind("researcherSearchTerm") String researcherSearchTerm,
                                                    @Bind("darCodeSearchTerm") String darCodeSearchTerm,
                                                    @Bind("datasetSearchTerm") String datasetSearchTerm,
                                                    @Bind("offset") Integer offset,
                                                    @Bind("limit") Integer limit,
                                                    @Define("sortField") String sortField,
                                                    @Define("sortOrder") String sortOrder);

  /**
   * Find the DARCollection and all of its Data Access Requests that contains the DAR with the given referenceId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class,  prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT " + 
      "c.*, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, " +
      "dar.draft AS dar_draft, dar.user_id AS dar_userId, dar.create_date AS dar_create_date, " +
      "dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, " +
      "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data " +
    "FROM dar_collection c " +
    "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id " +
    "WHERE c.collection_id = (SELECT collection_id FROM data_access_request WHERE reference_id = :referenceId)")
  DarCollection findDARCollectionByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find the DARCollection and all of its Data Access Requests that has the given collectionId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
      getCollectionAndDars +  " WHERE c.collection_id = dar.collection_id AND c.collection_id = :collectionId ")
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

