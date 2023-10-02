package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DarCollectionReducer;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface DarCollectionDAO extends Transactional<DarCollectionDAO> {

  String QUERY_FIELD_SEPARATOR = ", ";

  String getCollectionAndDars =
      " SELECT c.*, i.institution_name, u.display_name AS researcher, dd.dataset_id, " +
          User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR +
          Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR +
          Election.QUERY_FIELDS_WITH_E_PREFIX + QUERY_FIELD_SEPARATOR +
          Vote.QUERY_FIELDS_WITH_V_PREFIX + QUERY_FIELD_SEPARATOR +
          UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR +
          DarCollection.DAR_FILTER_QUERY_COLUMNS +
          " FROM dar_collection c " +
          " INNER JOIN users u ON u.user_id = c.create_user_id " +
          " LEFT JOIN user_property up ON u.user_id = up.userid AND up.propertykey in ('isThePI', 'piName', 'havePI', 'piERACommonsID') "
          +
          " LEFT JOIN institution i ON i.institution_id = u.institution_id " +
          " INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id " +
          " LEFT JOIN dar_dataset dd ON dd.reference_id = dar.reference_id " +
          " LEFT JOIN (" +
          "   SELECT election.*, MAX(election.election_id) OVER (PARTITION BY election.reference_id, election.election_type, election.dataset_id) AS latest "
          +
          "   FROM election " +
          "   WHERE LOWER(election.election_type) = 'dataaccess' OR LOWER(election.election_type) = 'rp'"
          +
          " ) AS e " +
          "   ON (dar.reference_id = e.reference_id AND dd.dataset_id = e.dataset_id) AND (e.latest = e.election_id OR e.latest IS NULL) "
          +
          " LEFT JOIN vote v ON v.electionid = e.election_id ";

  String archiveFilterQuery = " AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL) ";

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
          "dd.dataset_id, " +
          "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
          +
          "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, " +
          "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
          +
          "dar.update_date AS dar_update_date, (dar.data #>> '{}')::jsonb AS data " +
          "FROM dar_collection c " +
          "INNER JOIN users u ON c.create_user_id = u.user_id " +
          "LEFT JOIN user_property up ON u.user_id = up.userid " +
          "INNER JOIN data_access_request dar on c.collection_id = dar.collection_id " +
          "LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id " +
          "LEFT JOIN institution i ON i.institution_id = u.institution_id " +
          "LEFT JOIN (" +
          "   SELECT election.*, MAX(election.election_id) OVER (PARTITION BY election.reference_id, election.election_type, election.dataset_id) AS latest FROM election "
          +
          "   WHERE LOWER(election.election_type) = 'dataaccess' OR LOWER(election.election_type) = 'rp' "
          +
          ") AS e " +
          "   ON (dar.reference_id = e.reference_id AND dd.dataset_id = e.dataset_id) AND (e.latest = e.election_id OR e.latest IS NULL) "
          +
          "WHERE (LOWER(data->>'status')!='archived' OR data->>'status' IS NULL) "
  )
  List<DarCollection> findAllDARCollections();

  /**
   * Find the DARCollection and all of its Data Access Requests that contains the DAR with the given
   * referenceId
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
          "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
          +
          "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, " +
          "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
          +
          "dar.update_date AS dar_update_date, (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, dd.dataset_id " +
          "FROM dar_collection c " +
          "INNER JOIN users u ON c.create_user_id = u.user_id " +
          "LEFT JOIN user_property up ON u.user_id = up.userid " +
          "LEFT JOIN institution i ON i.institution_id = u.institution_id " +
          "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id " +
          "LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id " +
          "WHERE c.collection_id = (SELECT collection_id FROM data_access_request WHERE reference_id = :referenceId) "
          +
          "AND (LOWER(data->>'status')!='archived' OR data->>'status' IS NULL) ")
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
  @RegisterBeanMapper(value = LibraryCard.class, prefix = "lc")
  @UseRowReducer(DarCollectionReducer.class)
  @SqlQuery(
      // nosemgrep
      "SELECT c.*, "
          + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
          + Institution.QUERY_FIELDS_WITH_I_PREFIX + QUERY_FIELD_SEPARATOR
          + UserProperty.QUERY_FIELDS_WITH_UP_PREFIX + QUERY_FIELD_SEPARATOR
          + LibraryCard.QUERY_FIELDS_WITH_LC_PREFIX + QUERY_FIELD_SEPARATOR
          + "dd.dataset_id, "
          + "dar.id AS dar_id, dar.reference_id AS dar_reference_id, dar.collection_id AS dar_collection_id, "
          + "dar.parent_id AS dar_parent_id, dar.draft AS dar_draft, dar.user_id AS dar_userId, "
          + "dar.create_date AS dar_create_date, dar.sort_date AS dar_sort_date, dar.submission_date AS dar_submission_date, "
          + "dar.update_date AS dar_update_date, (regexp_replace(dar.data #>> '{}', '\\\\u0000', '', 'g'))::jsonb AS data, "
          + "e.election_id AS e_election_id, e.reference_id AS e_reference_id, e.status AS e_status, e.create_date AS e_create_date, "
          + "e.last_update AS e_last_update, e.dataset_id AS e_dataset_id, e.election_type AS e_election_type, e.latest, "
          + "v.voteid as v_vote_id, v.vote as v_vote, v.user_id as v_user_id, v.rationale as v_rationale, v.electionid as v_election_id, "
          + "v.createdate as v_create_date, v.updatedate as v_update_date, v.type as v_type, du.display_name as v_display_name "
          + "FROM dar_collection c "
          + "INNER JOIN users u ON c.create_user_id = u.user_id "
          + "LEFT JOIN user_property up ON u.user_id = up.userid "
          + "LEFT JOIN institution i ON i.institution_id = u.institution_id "
          + "LEFT JOIN library_card lc ON u.user_id = lc.user_id "
          + "INNER JOIN data_access_request dar ON c.collection_id = dar.collection_id "
          + "LEFT JOIN dar_dataset dd on dd.reference_id = dar.reference_id "
          + "LEFT JOIN ("
          + "SELECT election.*, MAX(election.election_id) OVER (PARTITION BY election.reference_id, election.election_type, election.dataset_id) AS latest "
          + "FROM election "
          + "WHERE LOWER(election.election_type) = 'dataaccess' OR LOWER(election.election_type) = 'rp'"
          + ") AS e "
          + "ON (dar.reference_id = e.reference_id AND dd.dataset_id = e.dataset_id) AND (e.latest = e.election_id OR e.latest IS NULL) "
          + "LEFT JOIN vote v "
          + "ON v.electionid = e.election_id "
          + "LEFT JOIN users du "
          + "ON du.user_id = v.user_id "
          + "WHERE c.collection_id = :collectionId "
          + "AND (LOWER(data->>'status') != 'archived' OR data->>'status' IS NULL )"
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

}






