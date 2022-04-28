package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DarCollectionReducer;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

public interface DarCollectionDAO {

  String QUERY_FIELD_SEPARATOR = ", ";

  String getCollectionAndDars =
      " SELECT c.*, i.institution_name, u.displayname AS researcher, " +
          User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
          Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR +
          Election.QUERY_FIELDS_WITH_E_PREFIX + QUERY_FIELD_SEPARATOR +
          Vote.QUERY_FIELDS_WITH_V_PREFIX + QUERY_FIELD_SEPARATOR +
          UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR +
          DarCollection.DAR_FILTER_QUERY_COLUMNS +
      " FROM dar_collection c " +
      " INNER JOIN dacuser u ON u.dacuserid = c.create_user_id " +
      " LEFT JOIN user_property up ON u.dacuserid = up.userid AND up.propertykey in ('isThePI', 'piName', 'havePI', 'piERACommonsID') " +
      " LEFT JOIN institution i ON i.institution_id = u.institution_id " +
      " INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id " +
      " LEFT JOIN (SELECT election.*, MAX(election.electionid) OVER (PARTITION BY election.referenceid, election.electiontype) AS latest FROM election) AS e " +
      "   ON dar.reference_id = e.referenceid AND (e.latest = e.electionid OR e.latest IS NULL) " +
      " LEFT JOIN vote v ON v.electionid = e.electionid ";

  String filterQuery =
    " WHERE c.create_user_id = :userId " +
      " AND (" +
      DarCollection.FILTER_TERMS_QUERY +
      " )";

  String archiveFilterQuery = " AND ((dar.data #>> '{}')::jsonb->>'status' != 'Archived' OR (dar.data #>> '{}')::jsonb->>'status' IS NULL) ";

  String getCollectionsAndDarsViaIds =
  getCollectionAndDars
  + filterQuery + archiveFilterQuery +
    "ORDER BY <sortField> <sortOrder>";

  String orderStatement = " ORDER BY <sortField> <sortOrder>";

  /**
   * Find all DARCollections with their DataAccessRequests that match the given filter
   *
   * <p>FilterTerms filter on dar project title, datasetNames, collection dar code, collection
   * update date, and DAC SortField can be projectTitle, datasetNames, dar code, update date, or DAC
   * SortDirection can be ASC or DESC
   *
   * @return List<DarCollection>
   */
  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
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
          + "     (SELECT distinct dar.collection_id, "
          + "     jsonb_array_elements((dar.data #>> '{}')::jsonb -> 'datasetIds')::integer AS dataset_id, "
          + "     (dar.data #>> '{}')::jsonb ->> 'status' as status "
          + "     FROM data_access_request dar) AS dar_datasets, "
          + "     consentassociations ca,"
          + "     consents consent "
          + " WHERE c.collection_id = dar_datasets.collection_id "
          + " AND dar_datasets.dataset_id = ca.datasetid "
          + " AND consent.consentid = ca.consentid "
          + " AND consent.dac_id IN (<dacIds>) "
          + " AND (dar_datasets.status != 'Archived' OR dar_datasets.status IS NULL) " )
  List<Integer> findDARCollectionIdsByDacIds(@BindList("dacIds") List<Integer> dacIds);

  @SqlQuery(
      " SELECT distinct c.collection_id "
          + " FROM dar_collection c"
          + " INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id"
          + " INNER JOIN dacuser u ON dar.user_id = u.dacuserid"
          + " WHERE u.institution_id = :institutionId "
          + " AND ((dar.data #>> '{}')::jsonb->>'status'!='Archived' OR (dar.data #>> '{}')::jsonb->>'status' IS NULL)" )
  List<Integer> findDARCollectionIdsByInstitutionId(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    getCollectionAndDars + " WHERE c.collection_id in (<collectionIds>)" + archiveFilterQuery)
  List<DarCollection> findDARCollectionByCollectionIds(
          @BindList("collectionIds") List<Integer> collectionIds);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    getCollectionAndDars
    + " WHERE c.collection_id in (<collectionIds>)"
    + archiveFilterQuery
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
  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    " SELECT c.*, " +
        User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
        Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR +
        UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR +
        Election.QUERY_FIELDS_WITH_E_PREFIX + QUERY_FIELD_SEPARATOR +
        "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, " +
        "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, " +
        "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, " +
        "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data " +
        "FROM dar_collection c " +
        "INNER JOIN dacuser u ON c.create_user_id = u.dacuserid " +
        "LEFT JOIN user_property up ON u.dacuserid = up.userid " +
        "INNER JOIN data_access_request dar on c.collection_id = dar.collection_id " +
        "LEFT JOIN institution i ON i.institution_id = u.institution_id " +
        "LEFT JOIN (SELECT election.*, MAX(election.electionid) OVER (PARTITION BY election.referenceid, election.electiontype) AS latest FROM election) AS e " +
        "ON dar.reference_id = e.referenceid AND (e.latest = e.electionid OR e.latest IS NULL) " +
        "WHERE (data->>'status'!='Archived' OR data->>'status' IS NULL) "
  )
  List<DarCollection> findAllDARCollections();

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery("SELECT c.*, " +
      User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
      Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR +
      UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR
      + "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
      + "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, "
      + "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
      + "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data, "
      + "e.electionid AS e_election_id, e.referenceid AS e_reference_id, e.status AS e_status, e.createdate AS e_create_date, "
      + "e.lastupdate AS e_last_update, e.datasetid AS e_dataset_id, e.electiontype AS e_election_type, e.latest "
      + "FROM dar_collection c "
      + "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id "
      + "INNER JOIN dacuser u ON c.create_user_id = u.dacuserid "
      + "LEFT JOIN user_property up ON u.dacuserid = up.userid "
      + "LEFT JOIN institution i ON i.institution_id = u.institution_id "
      + "LEFT JOIN ("
          + "SELECT election.*, MAX(election.electionid) OVER (PARTITION BY election.referenceid, election.electiontype) AS latest "
          + "FROM election"
      + ") AS e "
      + "ON dar.reference_id = e.referenceid AND (e.latest = e.electionid OR e.latest IS NULL) "
      + "WHERE c.create_user_id = :userId "
      + " AND (data->>'status'!='Archived' OR data->>'status' IS NULL) "
  )
  List<DarCollection> findDARCollectionsCreatedByUserId(@Bind("userId") Integer researcherId);

  /**
   * Find the DARCollection and all of its Data Access Requests that contains the DAR with the given referenceId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT c.*, " +
      User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
      Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR +
      UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR +
      "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, " +
      "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, " +
      "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, " +
      "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data " +
    "FROM dar_collection c " +
    "INNER JOIN dacuser u ON c.create_user_id = u.dacuserid " +
    "LEFT JOIN user_property up ON u.dacuserid = up.userid " +
    "LEFT JOIN institution i ON i.institution_id = u.institution_id " +
    "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id " +
    "WHERE c.collection_id = (SELECT collection_id FROM data_access_request WHERE reference_id = :referenceId) " +
    "AND (data->>'status'!='Archived' OR data->>'status' IS NULL) ")
  DarCollection findDARCollectionByReferenceId(@Bind("referenceId") String referenceId);

  /**
   * Find the DARCollection and all of its Data Access Requests that has the given collectionId
   *
   * @return DarCollection
   */
  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
    "SELECT c.*, "
      + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
      + Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR
      + UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR
      + "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
      + "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, "
      + "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
      + "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data, "
      + "e.electionid AS e_election_id, e.referenceid AS e_reference_id, e.status AS e_status, e.createdate AS e_create_date, "
      + "e.lastupdate AS e_last_update, e.datasetid AS e_dataset_id, e.electiontype AS e_election_type, e.latest, "
      + "v.voteid as v_vote_id, v.vote as v_vote, v.dacuserid as v_dac_user_id, v.rationale as v_rationale, v.electionid as v_election_id, "
      + "v.createdate as v_create_date, v.updatedate as v_update_date, v.type as v_type, du.displayname as v_display_name "
      + "FROM dar_collection c "
      + "INNER JOIN dacuser u ON c.create_user_id = u.dacuserid "
      + "LEFT JOIN user_property up ON u.dacuserid = up.userid "
      + "LEFT JOIN institution i ON i.institution_id = u.institution_id "
      + "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id "
      + "LEFT JOIN ("
          + "SELECT election.*, MAX(election.electionid) OVER (PARTITION BY election.referenceid, election.electiontype) AS latest "
          + "FROM election"
      + ") AS e "
      + "ON dar.reference_id = e.referenceid AND (e.latest = e.electionid OR e.latest IS NULL) "
      + "LEFT JOIN vote v "
      + "ON v.electionid = e.electionid "
      + "LEFT JOIN dacuser du "
      + "ON du.dacuserid = v.dacuserid "
      + "WHERE c.collection_id = :collectionId "
      + "AND (data->>'status' != 'Archived' OR data->>'status' IS NULL );"
  )
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


  String coreCountQuery = "SELECT COUNT(DISTINCT c.collection_id) "
      + "FROM dar_collection c "
      + "INNER JOIN dacuser u ON u.dacuserid = c.create_user_id "
      + "LEFT JOIN institution i ON i.institution_id = u.institution_id "
      + "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id "
      + " WHERE ((dar.data #>> '{}')::jsonb->>'status'!='Archived' OR (dar.data #>> '{}')::jsonb->>'status' IS NULL) ";

  //Count methods for unfiltered results listed below
  //DAC version is not included since a method that returns collectionIds for a DAC already exists
  @SqlQuery(coreCountQuery)
  Integer returnUnfilteredCollectionCount();

  @SqlQuery(
    coreCountQuery + "AND c.create_user_id = :userId")
  Integer returnUnfilteredResearcherCollectionCount(@Bind("userId") Integer userId);

  @SqlQuery(coreCountQuery + "AND u.institution_id = :institutionId")
  Integer returnUnfilteredCountForInstitution(@Bind("institutionId") Integer institutionId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(getCollectionAndDars
          + " WHERE (" + DarCollection.FILTER_TERMS_QUERY + ") " + archiveFilterQuery + orderStatement)
  List<DarCollection> getFilteredCollectionsForAdmin(
    @Define("sortField") String sortField,
    @Define("sortOrder") String sortOrder,
    @Bind("filterTerm") String filterTerm
  );

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(getCollectionAndDars
      + " WHERE u.institution_id = :institutionId AND ("
      + DarCollection.FILTER_TERMS_QUERY + ") " + archiveFilterQuery + orderStatement)
  List<DarCollection> getFilteredCollectionsForSigningOfficial(
      @Define("sortField") String sortField,
      @Define("sortOrder") String sortOrder,
      @Bind("institutionId") Integer institutionId,
      @Bind("filterTerm") String filterTerm);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(getCollectionAndDars
      + " WHERE c.create_user_id = :userId AND ("
      + DarCollection.FILTER_TERMS_QUERY + ") " + archiveFilterQuery + orderStatement)
  List<DarCollection> getFilteredListForResearcher(
      @Define("sortField") String sortField,
      @Define("sortOrder") String sortOrder,
      @Bind("userId") Integer userId,
      @Bind("filterTerm") String filterTerm);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = Institution.class, prefix = "i")
  @RegisterBeanMapper(value = DarCollection.class)
  @RegisterBeanMapper(value = DataAccessRequest.class, prefix = "dar")
  @RegisterBeanMapper(value = Election.class, prefix = "e")
  @RegisterBeanMapper(value = Vote.class, prefix = "v")
  @RegisterBeanMapper(value = UserProperty.class, prefix = "up")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(getCollectionAndDars
          + " WHERE c.collection_id IN (<collectionIds>) AND ("
          + DarCollection.FILTER_TERMS_QUERY + ") " + archiveFilterQuery + orderStatement)
  List<DarCollection> getFilteredCollectionsForDACByCollectionIds(
          @Define("sortField") String sortField,
          @Define("sortOrder") String sortOrder,
          @BindList("collectionIds") List<Integer> collectionIds,
          @Bind("filterTerm") String filterTerm
  );
}





