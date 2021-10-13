package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.DarCollectionReducer;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import java.util.Date;
import java.util.List;

public interface DarCollectionDAO {

  String getCollectionAndDars =
      " SELECT c.*, i.institution_name, u.displayname AS researcher, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, " +
          "dar.draft AS dar_draft, dar.user_id AS dar_userId, dar.create_date AS dar_create_date, " +
          "dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, " +
          "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data, " +
          "(dar.data #>> '{}')::jsonb ->> 'projectTitle' as projectTitle " +
      " FROM dar_collection c " +
      " INNER JOIN dacuser u ON u.dacuserid = c.create_user_id " +
      " LEFT JOIN institution i ON i.institution_id = u.institution_id " +
      " INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id ";

  String filterQuery =
    " WHERE c.create_user_id = :userId " +
      " AND (" +
        "COALESCE(i.institution_name, '') ~* :filterTerm " +
        " OR (dar.data #>> '{}')::jsonb ->> 'projectTitle' ~* :filterTerm " +
        " OR u.displayname ~* :filterTerm " +
        " OR c.dar_code ~* :filterTerm " +
        " OR EXISTS " +
        " (SELECT FROM jsonb_array_elements((dar.data #>> '{}')::jsonb -> 'datasets') dataset " +
        " WHERE dataset ->> 'label' ~* :filterTerm)" +
      " )";

  String getCollectionsAndDarsViaIds =
  getCollectionAndDars + filterQuery +
    "ORDER BY <sortField> <sortOrder>";

  /**
   * Find all DARCollections with their DataAccessRequests that match the given filter
   *
   * <p>FilterTerms filter on dar project title, datasetNames, collection dar code, collection
   * update date, and DAC SortField can be projectTitle, datasetNames, dar code, update date, or DAC
   * SortDirection can be ASC or DESC
   *
   * @return List<DarCollection>
   */
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(getCollectionsAndDarsViaIds)
  List<DarCollection> findAllDARCollectionsWithFiltersByUser(
          @Bind("filterTerm") String filterTerm,
          @Bind("userId") Integer userId,
          @Define("sortField") String sortField,
          @Define("sortOrder") String sortOrder);

  @SqlQuery(
      " SELECT distinct c.collection_id "
          + " FROM dar_collection c, "
          + "     (SELECT distinct dar.collection_id, jsonb_array_elements((dar.data #>> '{}')::jsonb -> 'datasetIds')::integer AS dataset_id FROM data_access_request dar) AS dar_datasets, "
          + "     consentassociations ca,"
          + "     consents consent "
          + " WHERE c.collection_id = dar_datasets.collection_id "
          + " AND dar_datasets.dataset_id = ca.datasetid "
          + " AND consent.consentid = ca.consentid "
          + " AND consent.dac_id IN (<dacIds>) ")
  List<Integer> findDARCollectionIdsByDacIds(@BindList("dacIds") List<Integer> dacIds);

  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    getCollectionAndDars + " WHERE c.collection_id in (<collectionIds>)")
  List<DarCollection> findDARCollectionByCollectionIds(
          @BindList("collectionIds") List<Integer> collectionIds);

  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    getCollectionAndDars
    + " WHERE c.collection_id in (<collectionIds>)"
    +  " ORDER BY <sortField> <sortOrder>")
  List<DarCollection> findDARCollectionByCollectionIdsWithOrder(
          @BindList("collectionIds") List<Integer> collectionIds,
          @Define("sortField") String sortField,
          @Define("sortOrder") String sortOrder);

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
        "FROM dar_collection c " +
        "INNER JOIN data_access_request dar on c.collection_id = dar.collection_id;"
  )
  List<DarCollection> findAllDARCollections();

  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery("SELECT c.*, dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
      + "dar.draft AS dar_draft, dar.user_id AS dar_userId, dar.create_date AS dar_create_date, "
      + "dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
      + "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data, "
      + "e.electionid AS e_election_id, e.referenceid AS e_reference_id, e.status AS e_status, e.createdate AS e_create_date, "
      + "e.lastupdate AS e_last_update, e.datasetid AS e_dataset_id, e.electiontype AS e_election_type, e.latest "
      + "FROM dar_collection c "
      + "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id "
      + "AND c.create_user_id = :userId "
      + "LEFT JOIN ("
          + "SELECT election.*, MAX(election.electionid) OVER (PARTITION BY election.referenceid, election.electiontype) AS latest "
          + "FROM election"
      + ") AS e "
      + "ON dar.reference_id = e.referenceid AND (e.latest = e.electionid OR e.latest IS NULL)"
  )
  List<DarCollection> findDARCollectionsCreatedByUserId(@Bind("userId") Integer researcherId);

  /**
   * Find the DARCollection and all of its Data Access Requests that contains the DAR with the given referenceId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
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
      getCollectionAndDars +  " WHERE c.collection_id = :collectionId ")
  DarCollection findDARCollectionByCollectionId(@Bind("collectionId") Integer collectionId);

  /**
   * Create a new DAR Collection with the given dar code, create user ID, and create date
   *
   * @return Integer, ID of newly created DarCollection
   */
  @SqlUpdate("INSERT INTO dar_collection " +
    " (dar_code, create_user_id, create_date) " +
    " VALUES (:darCode, :createUserId, :createDate)")
  @GetGeneratedKeys
  Integer insertDarCollection(@Bind("darCode") String darCode,
                            @Bind("createUserId") Integer createUserId,
                            @Bind("createDate") Date createDate);

  /**
   * Update the update user and update date of the DAR Collection with the given collection ID
   */
  @SqlUpdate("UPDATE dar_collection SET update_user_id = :updateUserId, update_date = :updateDate WHERE collection_id = :collectionId")
  void updateDarCollection(@Bind("collectionId") Integer collectionId,
                           @Bind("updateUserId") Integer updateUserId,
                           @Bind("updateDate") Date updateDate);

  /**
   * Delete the DAR Collection with the given collection ID
   */
  @SqlUpdate("DELETE FROM dar_collection WHERE collection_id = :collectionId")
  void deleteByCollectionId(@Bind("collectionId") Integer collectionId);
}

