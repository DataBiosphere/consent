package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.DacAuditMapper;
import org.broadinstitute.consent.http.db.mapper.DacMapper;
import org.broadinstitute.consent.http.db.mapper.DacReducer;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.db.mapper.RoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserRoleMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesMapper;
import org.broadinstitute.consent.http.db.mapper.UserWithRolesReducer;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacAudit;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
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
  @SqlQuery(
      """
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
      ORDER BY dac.name
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
      "SELECT "
          + User.QUERY_FIELDS_WITH_U_PREFIX
          + QUERY_FIELD_SEPARATOR
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
  @SqlQuery(
      """
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
   * Create a Dac given name and description. Atomically writes a CREATE audit entry.
   *
   * @param name The name for the new DAC
   * @param description The description for the new DAC
   * @param userId The user performing the operation (for the audit record)
   * @return Integer the new dac_id
   */
  @SqlQuery(
      """
      WITH new_dac AS (
        INSERT INTO dac (name, description, create_date) VALUES (:name, :description, NOW())
        RETURNING dac_id
      ),
      audit AS (
        INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
        SELECT dac_id, :userId, NULL, NULL, 'CREATE', NOW()
        FROM new_dac
      )
      SELECT dac_id FROM new_dac
      """)
  Integer createDac(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("userId") Integer userId);

  @SqlUpdate(
      """
      WITH deleted AS (
        DELETE FROM dac
        WHERE dac_id = :dacId
        RETURNING dac_id
      )
      INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
      SELECT dac_id, :userId, NULL, NULL, 'DELETE', NOW()
      FROM deleted
      """)
  void deleteDac(@Bind("dacId") Integer dacId, @Bind("userId") Integer userId);

  /**
   * Create a Dac given name, description, and email. Atomically writes a CREATE audit entry.
   *
   * @param name The name for the new DAC
   * @param description The description for the new DAC
   * @param email The email for the new DAC
   * @param userId The user performing the operation (for the audit record)
   * @return Integer the new dac_id
   */
  @SqlQuery(
      """
      WITH new_dac AS (
        INSERT INTO dac (name, description, email, create_date)
        VALUES (:name, :description, :email, NOW())
        RETURNING dac_id
      ),
      audit AS (
        INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
        SELECT dac_id, :userId, NULL, NULL, 'CREATE', NOW()
        FROM new_dac
      )
      SELECT dac_id FROM new_dac
      """)
  Integer createDac(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("email") String email,
      @Bind("userId") Integer userId);

  /**
   * Update a DAC's name and description. Atomically writes an UPDATE audit entry.
   *
   * @param name The new name
   * @param description The new description
   * @param dacId The DAC id
   * @param userId The user performing the operation (for the audit record)
   */
  @SqlUpdate(
      """
      WITH updated AS (
        UPDATE dac SET name = :name, description = :description, update_date = NOW()
        WHERE dac_id = :dacId
        RETURNING dac_id
      )
      INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
      SELECT dac_id, :userId, NULL, NULL, 'UPDATE', NOW()
      FROM updated
      """)
  void updateDac(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("dacId") Integer dacId,
      @Bind("userId") Integer userId);

  /**
   * Update a DAC's name, description, and email. Atomically writes an UPDATE audit entry.
   *
   * @param name The new name
   * @param description The new description
   * @param email The new email
   * @param dacId The DAC id
   * @param userId The user performing the operation (for the audit record)
   */
  @SqlUpdate(
      """
      WITH updated AS (
        UPDATE dac SET name = :name, description = :description, email = :email,
          update_date = NOW()
        WHERE dac_id = :dacId
        RETURNING dac_id
      )
      INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
      SELECT dac_id, :userId, NULL, NULL, 'UPDATE', NOW()
      FROM updated
      """)
  void updateDac(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("email") String email,
      @Bind("dacId") Integer dacId,
      @Bind("userId") Integer userId);

  @RegisterBeanMapper(value = User.class, prefix = "u")
  @RegisterBeanMapper(value = UserRole.class)
  @UseRowReducer(UserWithRolesReducer.class)
  @SqlQuery(
      "SELECT "
          + User.QUERY_FIELDS_WITH_U_PREFIX
          + QUERY_FIELD_SEPARATOR
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
      "SELECT "
          + User.QUERY_FIELDS_WITH_U_PREFIX
          + QUERY_FIELD_SEPARATOR
          + " ur.user_role_id, ur.user_id, ur.role_id, ur.dac_id, r.name "
          + " FROM users u "
          + " INNER JOIN user_role ur ON ur.user_id = u.user_id "
          + " INNER JOIN roles r ON r.role_id = ur.role_id "
          + " WHERE ur.dac_id = :dacId "
          + " AND ur.role_id = :roleId ")
  List<User> findMembersByDacIdAndRoleId(
      @Bind("dacId") Integer dacId, @Bind("roleId") Integer roleId);

  /**
   * Add a member or chair to a DAC and atomically record an audit entry.
   *
   * @param roleId The role to grant (CHAIRPERSON or MEMBER)
   * @param userId The user receiving the role
   * @param dacId The DAC the user is being added to
   * @param auditUserId The user performing the operation (for the audit record)
   */
  @SqlUpdate(
      """
      WITH inserted_role AS (
        INSERT INTO user_role (role_id, user_id, dac_id) VALUES (:roleId, :userId, :dacId)
        RETURNING user_id, role_id, dac_id
      )
      INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
      SELECT dac_id, :auditUserId, user_id, role_id, 'ADD', NOW()
      FROM inserted_role
      """)
  void addDacMember(
      @Bind("roleId") Integer roleId,
      @Bind("userId") Integer userId,
      @Bind("dacId") Integer dacId,
      @Bind("auditUserId") Integer auditUserId);

  /**
   * Remove a member or chair from a DAC and atomically record an audit entry.
   *
   * @param userRoleId The user_role row to delete
   * @param auditUserId The user performing the operation (for the audit record)
   */
  @SqlUpdate(
      """
      WITH deleted_role AS (
        DELETE FROM user_role WHERE user_role_id = :userRoleId
        RETURNING user_id, role_id, dac_id
      )
      INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
      SELECT dac_id, :auditUserId, user_id, role_id, 'REMOVE', NOW()
      FROM deleted_role
      WHERE dac_id IS NOT NULL
      """)
  void removeDacMember(
      @Bind("userRoleId") Integer userRoleId, @Bind("auditUserId") Integer auditUserId);

  /**
   * Insert a single audit row for a DAC-level operation (CREATE, UPDATE, DELETE). {@code
   * affectedUserId} and {@code roleId} may be null for non-member operations.
   *
   * @param dacId The DAC being acted upon
   * @param userId The actor
   * @param affectedUserId The user whose role is being changed (null for DAC-level events)
   * @param roleId The role involved (null for DAC-level events)
   * @param action The audit action string (e.g. 'CREATE', 'UPDATE', 'DELETE')
   */
  @SqlUpdate(
      """
      INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
      VALUES (:dacId, :userId, :affectedUserId, :roleId, :action, NOW())
      """)
  void insertDacAudit(
      @Bind("dacId") Integer dacId,
      @Bind("userId") Integer userId,
      @Bind("affectedUserId") Integer affectedUserId,
      @Bind("roleId") Integer roleId,
      @Bind("action") String action);

  /**
   * Return all audit records for a given DAC, newest first.
   *
   * @param dacId The DAC id
   * @return List of DacAudit records
   */
  @RegisterRowMapper(DacAuditMapper.class)
  @SqlQuery(
      """
      SELECT * FROM dac_audit
      WHERE dac_id = :dacId
      ORDER BY action_date DESC
      """)
  List<DacAudit> findAuditsByDacId(@Bind("dacId") Integer dacId);

  @UseRowMapper(RoleMapper.class)
  @SqlQuery("SELECT * FROM roles WHERE role_id = :roleId")
  Role getRoleById(@Bind("roleId") Integer roleId);

  @UseRowMapper(UserRoleMapper.class)
  @SqlQuery(
      "SELECT ur.*, r.name FROM user_role ur "
          + " INNER JOIN roles r ON ur.role_id = r.role_id WHERE ur.user_id IN (<userIds>)")
  List<UserRole> findUserRolesForUsers(
      @BindList(value = "userIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> userIds);

  /**
   * Find the Dacs for these datasets.
   *
   * @param datasetIds The list of dataset ids
   * @return All DACs that corresponds to the provided dataset ids
   */
  @RegisterRowMapper(DacMapper.class)
  @SqlQuery(
      """
      SELECT d.*, ds.dataset_id
      FROM dac d
      INNER JOIN dataset ds ON d.dac_id = ds.dac_id
      WHERE ds.dataset_id IN (<datasetIds>)
      """)
  Set<Dac> findDacsForDatasetIds(
      @BindList(value = "datasetIds", onEmpty = EmptyHandling.NULL_STRING)
          List<Integer> datasetIds);

  @RegisterRowMapper(DacMapper.class)
  @SqlQuery(
      """
      SELECT dac.*
      FROM dac
      INNER JOIN dataset d ON d.dac_id = dac.dac_id
      INNER JOIN dar_dataset dd ON dd.dataset_id = d.dataset_id
      INNER JOIN data_access_request dar ON dd.reference_id = dar.reference_id
      WHERE dar.collection_id = :collectionId
      """)
  Collection<Dac> findDacsForCollectionId(@Bind("collectionId") Integer collectionId);
}
