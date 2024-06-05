package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DaaMapper;
import org.broadinstitute.consent.http.db.mapper.DataAccessAgreementReducer;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DaaMapper.class)
@RegisterRowMapper(FileStorageObjectMapper.class)
public interface DaaDAO extends Transactional<DaaDAO> {

  /**
   * Find all DAAs
   *
   * @return List<DataAccessAgreement>
   */
  @RegisterBeanMapper(value = DataAccessAgreement.class, prefix = "daa")
  @RegisterBeanMapper(value = Dac.class)
  @RegisterBeanMapper(value = FileStorageObjectDAO.class)
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("""
          SELECT daa.daa_id as daa_daa_id,
                daa.create_user_id as daa_create_user_id,
                daa.create_date as daa_create_date,
                daa.update_user_id as daa_update_user_id,
                daa.update_date as daa_update_date,
                daa.initial_dac_id as daa_initial_dac_id,
                fso.file_storage_object_id AS file_storage_object_id,
                fso.entity_id AS entity_id,
                fso.file_name AS file_name,
                fso.category AS category,
                fso.gcs_file_uri AS gcs_file_uri,
                fso.media_type AS media_type,
                fso.create_date AS create_date,
                fso.create_user_id AS create_user_id,
                fso.update_date AS update_date,
                fso.update_user_id AS update_user_id,
                fso.deleted AS deleted,
                fso.delete_user_id AS delete_user_id,
                dac.dac_id,
                dac.email,
                dac.name,
                dac.description
          FROM data_access_agreement daa
          LEFT JOIN file_storage_object fso ON daa.daa_id::text = fso.entity_id
          LEFT JOIN dac_daa dd ON daa.daa_id = dd.daa_id
          LEFT JOIN dac ON dd.dac_id = dac.dac_id
      """)
  List<DataAccessAgreement> findAll();


  /**
   * Find a DAA by id
   *
   * @param daaId The daa_id to lookup
   * @return Daa
   */
  @RegisterBeanMapper(value = DataAccessAgreement.class, prefix = "daa")
  @RegisterBeanMapper(value = Dac.class)
  @RegisterBeanMapper(value = FileStorageObjectDAO.class)
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("""
          SELECT daa.daa_id as daa_daa_id,
                daa.create_user_id as daa_create_user_id,
                daa.create_date as daa_create_date,
                daa.update_user_id as daa_update_user_id,
                daa.update_date as daa_update_date,
                daa.initial_dac_id as daa_initial_dac_id,
                fso.file_storage_object_id AS file_storage_object_id,
                fso.entity_id AS entity_id,
                fso.file_name AS file_name,
                fso.category AS category,
                fso.gcs_file_uri AS gcs_file_uri,
                fso.media_type AS media_type,
                fso.create_date AS create_date,
                fso.create_user_id AS create_user_id,
                fso.update_date AS update_date,
                fso.update_user_id AS update_user_id,
                fso.deleted AS deleted,
                fso.delete_user_id AS delete_user_id,
                dac.dac_id,
                dac.email,
                dac.name,
                dac.description
          FROM data_access_agreement daa
          LEFT JOIN file_storage_object fso ON daa.daa_id::text = fso.entity_id
          LEFT JOIN dac_daa dd ON daa.daa_id = dd.daa_id
          LEFT JOIN dac ON dd.dac_id = dac.dac_id
          WHERE daa.daa_id = :daaId
      """)
  DataAccessAgreement findById(@Bind("daaId") Integer daaId);

  /**
   * Find a DAA by DAC
   *
   * @param dacId The initial_dac_id to lookup
   * @return Daa
   */
  @RegisterBeanMapper(value = DataAccessAgreement.class, prefix = "daa")
  @RegisterBeanMapper(value = Dac.class)
  @RegisterBeanMapper(value = FileStorageObjectDAO.class)
  @UseRowReducer(DataAccessAgreementReducer.class)
  @SqlQuery("""
          SELECT daa.daa_id as daa_daa_id,
                daa.create_user_id as daa_create_user_id,
                daa.create_date as daa_create_date,
                daa.update_user_id as daa_update_user_id, 
                daa.update_date as daa_update_date,
                daa.initial_dac_id as daa_initial_dac_id,
                fso.file_storage_object_id AS file_storage_object_id,
                fso.entity_id AS entity_id,
                fso.file_name AS file_name,
                fso.category AS category,
                fso.gcs_file_uri AS gcs_file_uri,
                fso.media_type AS media_type,
                fso.create_date AS create_date,
                fso.create_user_id AS create_user_id,
                fso.update_date AS update_date,
                fso.update_user_id AS update_user_id,
                fso.deleted AS deleted,
                fso.delete_user_id AS delete_user_id,
                dac.dac_id,
                dac.email,
                dac.name,
                dac.description
          FROM data_access_agreement daa
          LEFT JOIN file_storage_object fso ON daa.daa_id::text = fso.entity_id
          LEFT JOIN dac_daa dd ON daa.daa_id = dd.daa_id
          LEFT JOIN dac ON dd.dac_id = dac.dac_id
          WHERE daa.initial_dac_id = :dacId
      """)
  DataAccessAgreement findByDacId(@Bind("dacId") Integer dacId);

  /**
   * Create a Daa given name, description, and create date
   *
   * @param createUserId The id of the user who created this DAA
   * @param createDate   The date this new DAA was created
   * @param updateUserId The id of the user who updated this DAA
   * @param updateDate   The date that this DAA was updated
   * @param initialDacId The id for the initial DAC this DAA was created for
   * @return Integer
   */
  @SqlUpdate("""
      INSERT INTO data_access_agreement (create_user_id, create_date, update_user_id, update_date, initial_dac_id)
      VALUES (:createUserId, :createDate, :updateUserId, :updateDate, :initialDacId)
      """)
  @GetGeneratedKeys
  Integer createDaa(@Bind("createUserId") Integer createUserId,
      @Bind("createDate") Instant createDate, @Bind("updateUserId") Integer updateUserId,
      @Bind("updateDate") Instant updateDate, @Bind("initialDacId") Integer initialDacId);

  @SqlUpdate("""
    INSERT INTO dac_daa (dac_id, daa_id)
    VALUES (:dacId, :daaId)
    ON CONFLICT (dac_id) DO UPDATE SET daa_id = :daaId
  """)
  void createDacDaaRelation(@Bind("dacId") Integer dacId, @Bind("daaId") Integer daaId);

  @SqlUpdate("""
      DELETE FROM dac_daa
      WHERE dac_id = :dacId
      AND daa_id = :daaId
      """)
  void deleteDacDaaRelation(@Bind("dacId") Integer dacId, @Bind("daaId") Integer daaId);

  /**
   * Relationship chain:
   * User -> Library Card -> Data Access Agreement -> DAC -> Dataset
   * @param userId The requesting User ID
   * @return List of Dataset Ids for which a user has DAA acceptances
   */
  @SqlQuery(
      """
      SELECT DISTINCT d.dataset_id
      FROM dataset d
      INNER JOIN dac_daa ON dac_daa.dac_id = d.dac_id
      INNER JOIN lc_daa ON dac_daa.daa_id = lc_daa.daa_id
      INNER JOIN library_card lc on lc.id = lc_daa.lc_id
      WHERE lc.user_id = :userId
      """)
  List<Integer> findDaaDatasetIdsByUserId(@Bind("userId") Integer userId);

  @SqlUpdate("""
    WITH lc_deletes AS (DELETE FROM lc_daa WHERE lc_daa.daa_id = :daaId),
    dac_delete AS (DELETE FROM dac_daa WHERE dac_daa.daa_id = :daaId)
    DELETE FROM data_access_agreement WHERE daa_id = :daaId
    """)
  void deleteDaa(@Bind("daaId") Integer daaId);

}
