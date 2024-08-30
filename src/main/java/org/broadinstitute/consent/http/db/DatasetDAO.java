package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.ApprovedDatasetMapper;
import org.broadinstitute.consent.http.db.mapper.ApprovedDatasetReducer;
import org.broadinstitute.consent.http.db.mapper.DatasetDTOWithPropertiesMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetPropertyMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetReducer;
import org.broadinstitute.consent.http.db.mapper.DatasetStudySummaryMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetSummaryMapper;
import org.broadinstitute.consent.http.db.mapper.DictionaryMapper;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.resources.Resource;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterBeanMapper(value = User.class, prefix = "u")
@RegisterBeanMapper(value = Study.class, prefix = "s")
@RegisterRowMapper(DatasetMapper.class)
@RegisterRowMapper(FileStorageObjectMapperWithFSOPrefix.class)
public interface DatasetDAO extends Transactional<DatasetDAO> {

  String CHAIRPERSON = Resource.CHAIRPERSON;

  @UseRowMapper(DatasetStudySummaryMapper.class)
  @SqlQuery("""
      SELECT d.dataset_id, d.name AS dataset_name, d.alias, s.study_id, s.name AS study_name
        FROM dataset d
        LEFT JOIN study s ON s.study_id = d.study_id
        ORDER BY dataset_id
      """)
  List<DatasetStudySummary> findAllDatasetStudySummaries();

  @SqlUpdate(
      """
          INSERT INTO dataset
              (name, create_date, create_user_id, update_date,
              update_user_id, object_id, dac_id, alias, data_use)
          (SELECT :name, :createDate, :createUserId, :createDate,
              :createUserId, :objectId, :dacId, COALESCE(MAX(alias),0)+1, :dataUse FROM dataset)
          """)
  @GetGeneratedKeys
  Integer insertDataset(
      @Bind("name") String name,
      @Bind("createDate") Timestamp createDate,
      @Bind("createUserId") Integer createUserId,
      @Bind("objectId") String objectId,
      @Bind("dataUse") String dataUse,
      @Bind("dacId") Integer dacId);

  @SqlUpdate("""
            UPDATE dataset
            SET name = :datasetName,
                update_date = :updateDate,
                update_user_id = :updateUserId,
                dac_id = :dacId
            WHERE dataset_id = :datasetId
      """)
  void updateDatasetByDatasetId(
      @Bind("datasetId") Integer datasetId,
      @Bind("datasetName") String datasetName,
      @Bind("updateDate") Timestamp updateDate,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("dacId") Integer updatedDacId);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              fso.file_storage_object_id AS fso_file_storage_object_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
          WHERE d.dataset_id = :datasetId
      """)
  Dataset findDatasetById(@Bind("datasetId") Integer datasetId);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
              fso.file_storage_object_id AS fso_file_storage_object_id,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
          WHERE d.dataset_id in (<datasetIds>)
          ORDER BY d.dataset_id
      """)
  List<Dataset> findDatasetsByIdList(@BindList("datasetIds") List<Integer> datasetIds);

  @Deprecated
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
              fso.file_storage_object_id AS fso_file_storage_object_id,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
      """)
  List<Dataset> findAllDatasets();

  @SqlQuery("""
        SELECT dataset_id FROM dataset ORDER BY dataset_id
        """)
  List<Integer> findAllDatasetIds();

  /**
   * Original implementation of dacs -> datasets is via an association through consent. Subsequent
   * refactoring moves the dataset to a top level field on the DAC: User -> UserRoles -> DACs ->
   * Datasets
   *
   * @param email User email
   * @return List of datasets that are visible to the user via DACs.
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
              fso.file_storage_object_id AS fso_file_storage_object_id,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
          INNER JOIN user_role dac_role ON dac_role.dac_id = d.dac_id
          INNER JOIN users dac_user ON dac_role.user_id = dac_user.user_id AND dac_user.email = :email
      """)
  List<Dataset> findDatasetsByAuthUserEmail(@Bind("email") String email);

  /**
   * Finds all minimal dataset/study  information for datasets assigned to this DAC and which have
   * been requested for this DAC.
   *
   * @param dacId id
   * @return all datasets associated with DAC
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          WHERE d.dac_id = :dacId
          OR (dp.schema_property = 'dataAccessCommitteeId' AND dp.property_value = :dacId::text)
      """)
  List<Dataset> findDatasetsAssociatedWithDac(@Bind("dacId") Integer dacId);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
              fso.file_storage_object_id AS fso_file_storage_object_id,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
          WHERE d.name IS NOT NULL
      """)
  List<Dataset> getDatasets();

  @SqlQuery("""
          SELECT DISTINCT dp.property_value as name
          FROM dataset_property dp
          INNER JOIN dataset d ON dp.dataset_id = d.dataset_id
          WHERE (dp.schema_property = 'studyName')
          UNION DISTINCT
          SELECT DISTINCT s.name as name
          FROM study s
      """)
  Set<String> findAllStudyNames();

  @SqlQuery("""
          SELECT DISTINCT d.name
          FROM dataset d
          WHERE d.name IS NOT NULL
      """)
  List<String> findAllDatasetNames();

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
              fso.file_storage_object_id AS fso_file_storage_object_id,
              fso.entity_id AS fso_entity_id,
              fso.file_name AS fso_file_name,
              fso.category AS fso_category,
              fso.gcs_file_uri AS fso_gcs_file_uri,
              fso.media_type AS fso_media_type,
              fso.create_date AS fso_create_date,
              fso.create_user_id AS fso_create_user_id,
              fso.update_date AS fso_update_date,
              fso.update_user_id AS fso_update_user_id,
              fso.deleted AS fso_deleted,
              fso.delete_user_id AS fso_delete_user_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN study s ON s.study_id = d.study_id
          LEFT JOIN study_property sp ON sp.study_id = s.study_id
          LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
          LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
          WHERE d.alias = :alias
      """)
  Dataset findDatasetByAlias(@Bind("alias") Integer alias);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
          SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_id, d.dac_id, dar_ds_ids.id as in_use,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
          """
          + FileStorageObject.QUERY_FIELDS_WITH_FSO_PREFIX + " " +
          """
                   FROM dataset d
                   LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
                   LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
                   LEFT JOIN dictionary k ON k.key_id = dp.property_key
                   LEFT JOIN study s ON s.study_id = d.study_id
                   LEFT JOIN study_property sp ON sp.study_id = s.study_id
                   LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
                   LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
                   WHERE d.alias IN (<aliases>)
              """)
  List<Dataset> findDatasetsByAlias(@BindList("aliases") List<Integer> aliases);

  @Deprecated
  @SqlBatch("INSERT INTO dataset (name, create_date, object_id, alias, data_use) VALUES (:name, :createDate, :objectId, :alias, :dataUse)")
  void insertAll(@BindBean Collection<Dataset> datasets);

  @SqlUpdate("UPDATE dataset SET dac_id = :dacId WHERE dataset_id = :datasetId")
  void updateDatasetDacId(@Bind("datasetId") Integer datasetId, @Bind("dacId") Integer dacId);


  @SqlUpdate("UPDATE dataset SET translated_data_use = :translatedDataUse WHERE dataset_id = :datasetId")
  void updateDatasetTranslatedDataUse(@Bind("datasetId") Integer datasetId,
      @Bind("translatedDataUse") String translatedDataUse);

  @SqlUpdate("UPDATE dataset SET name = :name WHERE dataset_id = :datasetId")
  void updateDatasetName(@Bind("datasetId") Integer datasetId, @Bind("name") String name);


  @SqlBatch(
      "INSERT INTO dataset_property (dataset_id, property_key, schema_property, property_value, property_type, create_date )"
          +
          " VALUES (:dataSetId, :propertyKey, :schemaProperty, :getPropertyValueAsString, :getPropertyTypeAsString, :createDate)")
  void insertDatasetProperties(@BindBean @BindMethods List<DatasetProperty> dataSetPropertiesList);

  @SqlUpdate("DELETE FROM dataset_property WHERE dataset_id = :datasetId")
  void deleteDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

  @SqlUpdate("""
      INSERT INTO dataset_audit
        (dataset_id, change_action, modified_by_user, modification_date, object_id, name)
      VALUES
        (:dataSetId, :action, :user, :date, :objectId, :name)
      """)
  @GetGeneratedKeys
  Integer insertDatasetAudit(@BindBean DatasetAudit dataSets);

  @SqlUpdate(
      "UPDATE dataset_property "
          + "SET property_value = :propertyValue "
          + "WHERE dataset_id = :datasetId "
          + "AND property_key = :propertyKey")
  void updateDatasetProperty(@Bind("datasetId") Integer datasetId,
      @Bind("propertyKey") Integer propertyKey, @Bind("propertyValue") String propertyValue);

  @SqlUpdate(
      """
          UPDATE dataset
          SET study_id = :studyId
          WHERE dataset_id = :datasetId
          """
  )
  void updateStudyId(@Bind("datasetId") Integer datasetId, @Bind("studyId") Integer studyId);

  @SqlUpdate(
      "DELETE from dataset_property "
          + "WHERE dataset_id = :datasetId "
          + "AND property_key = :propertyKey")
  void deleteDatasetPropertyByKey(@Bind("datasetId") Integer datasetId,
      @Bind("propertyKey") Integer propertyKey);

  @SqlUpdate("DELETE FROM dataset WHERE dataset_id = :datasetId")
  void deleteDatasetById(@Bind("datasetId") Integer datasetId);

  @SqlUpdate("""
      UPDATE dataset
      SET name = :datasetName,
          update_date = :updateDate,
          update_user_id = :updateUserId,
          dac_id = :dacId
      WHERE dataset_id = :datasetId
      """)
  void updateDataset(@Bind("datasetId") Integer datasetId,
      @Bind("datasetName") String datasetName, @Bind("updateDate") Timestamp updateDate,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("dacId") Integer updatedDacId);

  @SqlUpdate("""
      UPDATE dataset
      SET data_use = :dataUse
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetDataUse(@Bind("datasetId") Integer datasetId, @Bind("dataUse") String dataUse);

  @SqlUpdate("""
      UPDATE dataset
      SET create_user_id = :createUserId
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetCreateUserId(@Bind("datasetId") Integer datasetId, @Bind("createUserId") Integer createUserId);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
          SELECT d.*, k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id, d.dac_id, dar_ds_ids.id as in_use,
              s.study_id AS s_study_id,
              s.name AS s_name,
              s.description AS s_description,
              s.data_types AS s_data_types,
              s.pi_name AS s_pi_name,
              s.create_user_id AS s_create_user_id,
              s.create_date AS s_create_date,
              s.update_user_id AS s_user_id,
              s.update_date AS s_update_date,
              s.public_visibility AS s_public_visibility,
              s_dataset.dataset_id AS s_dataset_id,
              sp.study_property_id AS sp_study_property_id,
              sp.study_id AS sp_study_id,
              sp.key AS sp_key,
              sp.value AS sp_value,
              sp.type AS sp_type,
          """
          + FileStorageObject.QUERY_FIELDS_WITH_FSO_PREFIX + " " +
          """
                  FROM dataset d
                  LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
                  LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
                  LEFT JOIN dictionary k ON k.key_id = dp.property_key
                  LEFT JOIN study s ON s.study_id = d.study_id
                  LEFT JOIN study_property sp ON sp.study_id = s.study_id
                  LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
                  LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
                  WHERE d.dataset_id IN (<datasetIds>)
                  ORDER BY d.dataset_id, k.display_order
              """)
  Set<Dataset> findDatasetWithDataUseByIdList(@BindList("datasetIds") List<Integer> datasetIds);

  @Deprecated
  @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
  @SqlQuery("""
      SELECT d.*, k.key, dp.property_value, d.dac_id
      FROM dataset d
      LEFT OUTER JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
      LEFT OUTER JOIN dictionary k ON k.key_id = dp.property_key
      WHERE d.dataset_id = :datasetId ORDER BY d.dataset_id, k.display_order
      """)
  Set<DatasetDTO> findDatasetDTOWithPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

  @UseRowMapper(DatasetPropertyMapper.class)
  @SqlQuery(
      " SELECT p.*, d.key FROM dataset_property p " +
          " INNER JOIN dictionary d ON p.property_key = d.key_id " +
          " WHERE p.dataset_id = :datasetId ")
  Set<DatasetProperty> findDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

  @Deprecated // Use getDictionaryTerms()
  @RegisterRowMapper(DictionaryMapper.class)
  @SqlQuery("SELECT * FROM dictionary d ORDER BY receive_order")
  List<Dictionary> getMappedFieldsOrderByReceiveOrder();

  @RegisterRowMapper(DictionaryMapper.class)
  @SqlQuery("SELECT * FROM dictionary ORDER BY key_id")
  List<Dictionary> getDictionaryTerms();

  @SqlQuery("SELECT * FROM dataset WHERE LOWER(name) = LOWER(:name)")
  Dataset getDatasetByName(@Bind("name") String name);

  /**
   * DACs -> Datasets Datasets -> DatasetProperties ->
   * Dictionary
   *
   * @return Set of datasets, with properties, that are associated with the provided DAC IDs
   */
  @Deprecated
  @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
  @SqlQuery("""
      SELECT d.*, k.key, p.property_value, d.dac_id
      FROM dataset d
      LEFT OUTER JOIN dataset_property p ON p.dataset_id = d.dataset_id
      LEFT OUTER JOIN dictionary k ON k.key_id = p.property_key
      WHERE d.dac_id IN (<dacIds>)
      """)
  Set<DatasetDTO> findDatasetsByDacIds(@BindList("dacIds") List<Integer> dacIds);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery("""
      SELECT distinct d.*, k.key, p.property_value, d.dac_id
      FROM dataset d
      LEFT JOIN dataset_property p ON p.dataset_id = d.dataset_id
      LEFT JOIN dictionary k ON k.key_id = p.property_key
      WHERE d.dac_id IN (<dacIds>)
      """)
  List<Dataset> findDatasetListByDacIds(@BindList("dacIds") List<Integer> dacIds);

  /**
   * DACs -> Datasets Datasets -> DatasetProperties ->
   * Dictionary
   *
   * @return Set of datasets, with properties, that are associated to any Dac.
   */
  @Deprecated
  @UseRowMapper(DatasetDTOWithPropertiesMapper.class)
  @SqlQuery("""
      SELECT distinct d.*, k.key, p.property_value, d.dac_id
      FROM dataset d
      LEFT JOIN dataset_property p ON p.dataset_id = d.dataset_id
      LEFT JOIN dictionary k ON k.key_id = p.property_key
      WHERE d.dac_id IS NOT NULL
      """)
  Set<DatasetDTO> findDatasetsWithDacs();

  @SqlUpdate(
      "UPDATE dataset " +
          "SET dac_approval = :dacApproval, " +
          "update_date = :updateDate, " +
          "update_user_id = :updateUserId " +
          "WHERE dataset_id = :datasetId"
  )
  void updateDatasetApproval(
      @Bind("dacApproval") Boolean dacApproved,
      @Bind("updateDate") Instant updateDate,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("datasetId") Integer datasetId
  );

  @RegisterRowMapper(ApprovedDatasetMapper.class)
  @UseRowReducer(ApprovedDatasetReducer.class)
  @SqlQuery("""
        SELECT DISTINCT c.dar_code, d.alias, d.name as dataset_name, dac.name as dac_name, vote_view.update_date
        FROM data_access_request dar
        INNER JOIN dar_collection c on dar.collection_id = c.collection_id
        INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        INNER JOIN dataset d on d.dataset_id = dd.dataset_id
        INNER JOIN dac dac on dac.dac_id = d.dac_id
        INNER JOIN election e on dar.reference_id = e.reference_id AND e.dataset_id = d.dataset_id
        INNER JOIN vote v ON e.election_id = v.electionid AND v.vote IS NOT NULL
        INNER JOIN (
          SELECT voteid, MAX(updatedate) update_date
          FROM vote
          GROUP BY voteid) vote_view ON v.voteid = vote_view.voteid
        WHERE d.dac_approval = TRUE AND dar.user_id = :userId
  """)
  List<ApprovedDataset> getApprovedDatasets(@Bind("userId") Integer userId);

  @RegisterRowMapper(DatasetSummaryMapper.class)
  @SqlQuery("""
      SELECT DISTINCT d.dataset_id, d.alias, d.name
      FROM dataset d
      LEFT JOIN dataset_property p ON p.dataset_id = d.dataset_id
      WHERE d.dac_approval = TRUE
      AND (
        LOWER(d.name) LIKE concat('%', LOWER(:query), '%') OR
        LOWER(p.property_value) LIKE concat('%', LOWER(:query), '%')
      )
      """)
  List<DatasetSummary> findDatasetSummariesByQuery(@Bind("query") String query);
}
