package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DacReducer;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.db.mapper.RoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserRoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DacMapper.class)
@RegisterRowMapper(FileStorageObjectMapperWithFSOPrefix.class)
public interface DacDAO extends Transactional<DacDAO> {

  String QUERY_FIELD_SEPARATOR = ", ";

  /**
   * Find all DACs
   *
   * @return List<Dac>
   */
  @RegisterBeanMapper(value = DataAccessAgreement.class, prefix = "daa")
  @RegisterBeanMapper(value = FileStorageObjectDAO.class)
  @RegisterBeanMapper(value = Dac.class)
  @RegisterBeanMapper(value = Dataset.class)
  @UseRowReducer(DacReducer.class)
  @SqlQuery("""
      SELECT
        dac.dac_id,
        dac.email,
        dac.name,
        dac.description,
        d.dataset_id,
        d.name AS dataset_name,
        DATE(d.create_date) AS dataset_create_date,
        d.object_id,
        d.alias AS dataset_alias,
        d.create_user_id,
        d.update_date AS dataset_update_date,
        d.update_user_id,
        d.data_use AS dataset_data_use,
        d.sharing_plan_document,
        d.sharing_plan_document_name,
        daa.daa_id AS daa_daa_id,
        daa.create_user_id AS daa_create_user_id,
        daa.create_date AS daa_create_date,
        daa.update_user_id AS daa_update_user_id,
        daa.update_date AS daa_update_date,
        daa.initial_dac_id AS daa_initial_dac_id,
        fso.file_storage_object_id AS fso_file_storage_object_id,
        fso.entity_id AS fso_entity_id,
        fso.file_name AS fso_file_name,
        fso.category AS fso_category,
        fso.gcs_file_uri AS fso_gcs_file_uri,
        fso.media_type AS fso_media_type,
        fso.deleted AS fso_deleted,
        fso.delete_user_id AS fso_delete_user_id,
        fso.create_date AS fso_create_date,
        fso.create_user_id AS fso_create_user_id,
        fso.update_date AS fso_update_date,
        fso.update_user_id AS fso_update_user_id
      FROM dac
      LEFT JOIN dataset d ON dac.dac_id = d.dac_id
      LEFT JOIN dac_daa dd ON dac.dac_id = dd.dac_id
      LEFT JOIN data_access_agreement daa ON dd.daa_id = daa.daa_id
      LEFT JOIN file_storage_object fso ON daa.daa_id::text = fso.entity_id
      """)
  List<Dac> findAll();

  /**
   * Find all Users associated with a DAC
   *
   * @return List<User>
   */
  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT " + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
          + " r.name, "
          + " ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id "
          + " FROM users u "
          + " INNER JOIN user_role ur ON ur.user_id = u.user_id AND ur.dac_id IS NOT NULL "
          + " INNER JOIN roles r ON r.role_id = ur.role_id")
  List<User> findAllDACUserMemberships();

  /**
   * Find all Users with a specific string in the display_name or email
   *
   * @param term The string to search against
   * @return Set<User>
   */
  @UseRowMapper(UserWithRolesMapper.class)
  @SqlQuery(
      "SELECT du.*, r.role_id, r.name, ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id FROM users du "
          + " INNER JOIN user_role ur ON ur.user_id = du.user_id "
          + " INNER JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE LOWER(du.display_name) LIKE concat('%', LOWER(:term), '%') "
          + " OR LOWER(du.email) LIKE concat('%', LOWER(:term), '%') ")
  Set<User> findAllDACUsersBySearchString(@Bind("term") String term);

  /**
   * Find a DAC by id
   *
   * @param dacId The dac_id to lookup
   * @return Dac
   */
  @RegisterBeanMapper(value = DataAccessAgreement.class, prefix = "daa")
  @RegisterBeanMapper(value = FileStorageObjectDAO.class)
  @UseRowReducer(DacReducer.class)
  @SqlQuery("""
      SELECT dac.*,
        daa.daa_id as daa_daa_id,
        daa.create_user_id as daa_create_user_id,
        daa.create_date as daa_create_date,
        daa.update_user_id as daa_update_user_id,
        daa.update_date as daa_update_date,
        daa.initial_dac_id as daa_initial_dac_id,
        fso.file_storage_object_id AS fso_file_storage_object_id,
        fso.entity_id AS fso_entity_id,
        fso.file_name AS fso_file_name,
        fso.category AS fso_category,
        fso.gcs_file_uri AS fso_gcs_file_uri,
        fso.media_type AS fso_media_type,
        fso.deleted AS fso_deleted,
        fso.delete_user_id AS fso_delete_user_id,
        fso.create_date AS fso_create_date,
        fso.create_user_id AS fso_create_user_id,
        fso.update_date AS fso_update_date,
        fso.update_user_id AS fso_update_user_id
      FROM dac
      LEFT JOIN dac_daa dd ON dac.dac_id = dd.dac_id
      LEFT JOIN data_access_agreement daa ON dd.daa_id = daa.daa_id
      LEFT JOIN file_storage_object fso ON daa.daa_id::text = fso.entity_id
      WHERE dac.dac_id = :dacId
      """)
  Dac findById(@Bind("dacId") Integer dacId);

  /**
   * Create a Dac given name, description, and create date
   *
   * @param name        The name for the new DAC
   * @param description The description for the new DAC
   * @param createDate  The date this new DAC was created
   * @return Integer
   */
  @SqlUpdate("INSERT INTO dac (name, description, create_date) VALUES (:name, :description, :createDate)")
  @GetGeneratedKeys
  Integer createDac(@Bind("name") String name, @Bind("description") String description,
      @Bind("createDate") Date createDate);

  /**
   * Create a Dac given name, description, and create date
   *
   * @param name        The name for the new DAC
   * @param description The description for the new DAC
   * @param email       The email for the new DAC
   * @param createDate  The date this new DAC was created
   * @return Integer
   */
  @SqlUpdate("INSERT INTO dac (name, description, email, create_date) VALUES (:name, :description, :email, :createDate)")
  @GetGeneratedKeys
  Integer createDac(@Bind("name") String name, @Bind("description") String description,
      @Bind("email") String email, @Bind("createDate") Date createDate);

  @SqlUpdate("UPDATE dac SET name = :name, description = :description, update_date = :updateDate WHERE dac_id = :dacId")
  void updateDac(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("updateDate") Date updateDate,
      @Bind("dacId") Integer dacId);

  @SqlUpdate("UPDATE dac SET name = :name, description = :description, email = :email, update_date = :updateDate WHERE dac_id = :dacId")
  void updateDac(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("email") String email,
      @Bind("updateDate") Date updateDate,
      @Bind("dacId") Integer dacId);

  /**
   * Delete all members from a specified DAC
   *
   * @param dacId The DAC id to remove users from
   */
  @SqlUpdate("DELETE FROM user_role WHERE dac_id = :dacId")
  void deleteDacMembers(@Bind("dacId") Integer dacId);

  @SqlUpdate("DELETE FROM dac WHERE dac_id = :dacId")
  void deleteDac(@Bind("dacId") Integer dacId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT " + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
          + " ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
          + " FROM users u "
          + " INNER JOIN user_role ur ON ur.user_id = u.user_id "
          + " INNER JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE ur.dac_id = :dacId ")
  List<User> findMembersByDacId(@Bind("dacId") Integer dacId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT " + User.QUERY_FIELDS_WITH_U_PREFIX + QUERY_FIELD_SEPARATOR
          + " ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
          + " FROM users u "
          + " INNER JOIN user_role ur ON ur.user_id = u.user_id "
          + " INNER JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE ur.dac_id = :dacId "
          + " AND ur.role_id = :roleId ")
  List<User> findMembersByDacIdAndRoleId(@Bind("dacId") Integer dacId,
      @Bind("roleId") Integer roleId);

  @SqlUpdate("INSERT INTO user_role (role_id, user_id, dac_id) VALUES (:roleId, :userId, :dacId)")
  void addDacMember(@Bind("roleId") Integer roleId, @Bind("userId") Integer userId,
      @Bind("dacId") Integer dacId);

  @SqlUpdate("DELETE FROM user_role WHERE user_role_id = :userRoleId")
  void removeDacMember(@Bind("userRoleId") Integer userRoleId);

  @UseRowMapper(RoleMapper.class)
  @SqlQuery("SELECT * FROM roles WHERE role_id = :roleId")
  Role getRoleById(@Bind("roleId") Integer roleId);

  @UseRowMapper(UserRoleMapper.class)
  @SqlQuery(
      "SELECT ur.*, r.name FROM user_role ur INNER JOIN roles r ON ur.role_id = r.role_id WHERE ur.user_id = :userId")
  List<UserRole> findUserRolesForUser(@Bind("userId") Integer userId);

  @UseRowMapper(UserRoleMapper.class)
  @SqlQuery(
      "SELECT ur.*, r.name FROM user_role ur "
          + " INNER JOIN roles r ON ur.role_id = r.role_id WHERE ur.user_id IN (<userIds>)")
  List<UserRole> findUserRolesForUsers(@BindList("userIds") List<Integer> userIds);

  /**
   * Find the Dacs for these datasets.
   *
   * @param datasetIds The list of dataset ids
   * @return All DACs that corresponds to the provided dataset ids
   */
  @RegisterRowMapper(DacMapper.class)
  @SqlQuery("""
      SELECT d.*, ds.dataset_id
      FROM dac d
      INNER JOIN dataset ds ON d.dac_id = ds.dac_id
      WHERE ds.dataset_id IN (<datasetIds>)
      """)
  Set<Dac> findDacsForDatasetIds(@BindList("datasetIds") List<Integer> datasetIds);

  @RegisterRowMapper(DacMapper.class)
  @SqlQuery("""
      SELECT dac.*
      FROM dac
      INNER JOIN dataset d ON d.dac_id = dac.dac_id
      INNER JOIN dar_dataset dd ON dd.dataset_id = d.dataset_id
      INNER JOIN data_access_request dar ON dd.reference_id = dar.reference_id
      WHERE dar.collection_id = :collectionId
      """)
  Collection<Dac> findDacsForCollectionId(@Bind("collectionId") Integer collectionId);

}
