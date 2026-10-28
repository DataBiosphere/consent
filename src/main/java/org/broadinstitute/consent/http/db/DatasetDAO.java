package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.broadinstitute.consent.http.db.mapper.ApprovedDatasetMapper;
import org.broadinstitute.consent.http.db.mapper.ApprovedDatasetReducer;
import org.broadinstitute.consent.http.db.mapper.DatasetAuditMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetPropertyMapper;
import org.broadinstitute.consent.http.db.mapper.DatasetReducer;
import org.broadinstitute.consent.http.db.mapper.DatasetStudySummaryMapper;
import org.broadinstitute.consent.http.db.mapper.DictionaryMapper;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.db.mapper.ShowcaseDatasetSummaryMapper;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.ShowcaseStatsResponse;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.resources.Resource;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
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

  /**
   * Find a Dataset by id.
   *
   * @param datasetId The dataset id
   * @return Dataset
   */
  default Dataset findDatasetWithoutFSOInformation(Integer datasetId) {
    if (isInTransaction()) {
      return assembleDataset(this, datasetId, false);
    }
    return inTransaction(
        TransactionIsolationLevel.REPEATABLE_READ, dao -> assembleDataset(dao, datasetId, false));
  }

  /**
   * Find a minimal version of a Dataset by alias. This query excludes file and study information
   * which is often not necessary for many operations.
   *
   * @param alias The dataset alias
   * @return Dataset
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
      SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
          d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use,
          d.dac_approval, d.study_id, d.indexed_date,
          u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
          u.create_date AS u_create_date, u.email_preference AS u_email_preference,
          u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
          k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property,
          dp.property_id
      FROM dataset d
      LEFT JOIN users u on d.create_user_id = u.user_id
      LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
      LEFT JOIN dictionary k ON k.key_id = dp.property_key
      WHERE d.alias = :alias
      """)
  Dataset findMinimalDatasetByAlias(@Bind("alias") Integer alias);

  @UseRowMapper(DatasetStudySummaryMapper.class)
  @SqlQuery(
      """
      SELECT d.dataset_id, d.create_user_id AS dataset_create_user_id, d.name AS dataset_name,
          d.alias, s.study_id, s.name AS study_name, s.public_visibility AS public_visibility,
          s.create_user_id AS study_create_user_id
        FROM dataset d
        LEFT JOIN study s ON s.study_id = d.study_id
        ORDER BY dataset_id
      """)
  List<DatasetStudySummary> findAllDatasetStudySummaries();

  @SqlUpdate(
      """
          INSERT INTO dataset
              (name, create_date, create_user_id, update_date,
              update_user_id, object_id, dac_id, data_use)
          VALUES (:name, :createDate, :createUserId, :createDate,
              :createUserId, :objectId, :dacId, :dataUse)
          """)
  @GetGeneratedKeys
  Integer insertDataset(
      @Bind("name") String name,
      @Bind("createDate") Timestamp createDate,
      @Bind("createUserId") Integer createUserId,
      @Bind("objectId") String objectId,
      @Bind("dataUse") String dataUse,
      @Bind("dacId") Integer dacId);

  @SqlUpdate(
      """
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

  /**
   * Finds a fully populated dataset without joining all child collections into one Cartesian
   * product. The focused reads run in a single REPEATABLE READ transaction so that they all observe
   * the same snapshot; under the default READ COMMITTED level each statement would take its own
   * snapshot and the assembled dataset could mix pre- and post-commit state.
   */
  default Dataset findDatasetById(Integer datasetId) {
    // A handle that is already in a transaction cannot open a nested one at a different
    // isolation level, so reuse the transaction the caller established rather than failing.
    if (isInTransaction()) {
      return assembleDataset(this, datasetId, true);
    }
    return inTransaction(
        TransactionIsolationLevel.REPEATABLE_READ, dao -> assembleDataset(dao, datasetId, true));
  }

  /**
   * Return a list of Dataset objects from a list of dataset ids. This explicitly does NOT populate
   * study information due to performance issues.
   *
   * @param datasetIds List of dataset ids
   * @return List of datasets
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use, d.study_id, d.indexed_date,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
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
              fso.delete_user_id AS fso_delete_user_id,
              fso.delete_date AS fso_delete_date
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN LATERAL (
              SELECT *
              FROM file_storage_object
              WHERE entity_id = d.dataset_id::text
                  AND deleted = false
                  AND category = :fileCategory
              ORDER BY GREATEST(create_date, update_date, delete_date) DESC,
                  file_storage_object_id DESC
              LIMIT 1
          ) fso ON true
          WHERE d.dataset_id in (<datasetIds>)
          ORDER BY d.dataset_id
      """)
  List<Dataset> findDatasetsByIdListAndFileCategory(
      @BindList(value = "datasetIds", onEmpty = EmptyHandling.NULL_STRING)
          Collection<Integer> datasetIds,
      @Bind("fileCategory") String fileCategory);

  default List<Dataset> findDatasetsByIdList(Collection<Integer> datasetIds) {
    return findDatasetsByIdListAndFileCategory(
        datasetIds, FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue());
  }

  /**
   * {@link #findDatasetsByIdList(Collection)} without the NIH Institutional Certification file. Use
   * it for datasets that go to users who cannot retrieve that file, such as the datasets in a
   * researcher's DAR collection.
   *
   * @param datasetIds List of dataset ids
   * @return List of datasets, without files and without study information
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use, d.study_id, d.indexed_date,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          WHERE d.dataset_id in (<datasetIds>)
          ORDER BY d.dataset_id
      """)
  List<Dataset> findDatasetsByIdListWithoutFiles(
      @BindList(value = "datasetIds", onEmpty = EmptyHandling.NULL_STRING)
          Collection<Integer> datasetIds);

  @SqlQuery(
      """
      SELECT dataset_id FROM dataset ORDER BY dataset_id
      """)
  List<Integer> findAllDatasetIds();

  /**
   * Find all dataset IDs that the user email has access to via their DAC association.
   *
   * @param userId User ID
   * @return List of Dataset IDs that are visible to the user via DACs.
   */
  @SqlQuery(
      """
          SELECT distinct d.dataset_id
          FROM dataset d
          INNER JOIN user_role dac_role ON dac_role.dac_id = d.dac_id
          INNER JOIN users dac_user ON dac_role.user_id = dac_user.user_id AND dac_user.user_id = :userId
      """)
  List<Integer> findDatasetIdsByDACUserId(@Bind("userId") Integer userId);

  @SqlQuery(
      """
          SELECT d.dataset_id
          FROM dataset d
          INNER JOIN dac_rule_settings settings ON d.dac_id = settings.dac_id
          INNER JOIN dac_automation_rules r ON settings.rule_id = r.id AND r.rule::text = :ruleType
          WHERE d.dataset_id IN (<datasetIds>)
      """)
  List<Integer> filterDatasetIdsByAutomationRuleType(
      @BindList(value = "datasetIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> datasetIds,
      @Bind("ruleType") String ruleType);

  /**
   * Finds all minimal dataset information for datasets assigned to this DAC and which have been
   * requested for this DAC. This explicitly does NOT populate study information due to performance
   * issues.
   *
   * @param dacId id
   * @return all datasets associated with DAC
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use, d.dac_approval,
              dar_ds_ids.id AS in_use, d.study_id, d.indexed_date,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          WHERE d.dac_id = :dacId
          OR (dp.schema_property = 'dataAccessCommitteeId' AND dp.property_value = :dacId::text)
      """)
  List<Dataset> findDatasetsAssociatedWithDac(@Bind("dacId") Integer dacId);

  @SqlQuery(
      """
          SELECT DISTINCT dp.property_value as name
          FROM dataset_property dp
          INNER JOIN dataset d ON dp.dataset_id = d.dataset_id
          WHERE (dp.schema_property = 'studyName')
          UNION DISTINCT
          SELECT DISTINCT s.name as name
          FROM study s
      """)
  Set<String> findAllStudyNames();

  @SqlQuery(
      """
          SELECT DISTINCT d.name
          FROM dataset d
          WHERE d.name IS NOT NULL
      """)
  List<String> findAllDatasetNames();

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
      SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
          d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use,
          d.dac_approval, dar_ds_ids.id AS in_use, d.study_id, d.indexed_date,
          u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
          u.create_date AS u_create_date, u.email_preference AS u_email_preference,
          u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
          k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property,
          dp.property_id
      FROM dataset d
      LEFT JOIN users u ON d.create_user_id = u.user_id
      LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids
          ON dar_ds_ids.id = d.dataset_id
      LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
      LEFT JOIN dictionary k ON k.key_id = dp.property_key
      WHERE d.dataset_id = :datasetId
      """)
  Dataset findDatasetDetailsById(@Bind("datasetId") Integer datasetId);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
      SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
          d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use,
          d.dac_approval, d.study_id, d.indexed_date,
          s.study_id AS s_study_id,
          s.name AS s_name,
          s.description AS s_description,
          s.data_types AS s_data_types,
          s.pi_name AS s_pi_name,
          s.pi_email AS s_pi_email,
          s.create_user_id AS s_create_user_id,
          s.create_date AS s_create_date,
          s.update_user_id AS s_update_user_id,
          s.update_date AS s_update_date,
          s.public_visibility AS s_public_visibility,
          s.uuid AS s_uuid,
          -- The PI's profile links are columns on the study row, so they carry the s_ prefix the
          -- Study bean mapper is registered with. Without them this route reported them null even
          -- when stored, the same way it reported no institution.
          s.pi_orcid AS s_pi_orcid,
          s.pi_linkedin_url AS s_pi_linkedin_url,
          s.pi_website_url AS s_pi_website_url,
          -- StudyReducer reads the institution columns unprefixed, as StudyDAO's queries alias
          -- them. Without these the study on this route reported no PI institution at all, though
          -- Dataset.yaml reuses Study.yaml and documents the field.
          s.pi_institution_id AS pi_institution_id,
          i.institution_name AS pi_institution_name,
          i.create_date AS pi_institution_create_date,
          i.update_date AS pi_institution_update_date,
          sp.study_property_id AS sp_study_property_id,
          sp.study_id AS sp_study_id,
          sp.key AS sp_key,
          sp.value AS sp_value,
          sp.type AS sp_type
      FROM dataset d
      LEFT JOIN study s ON s.study_id = d.study_id
      LEFT JOIN institution i ON i.institution_id = s.pi_institution_id
      LEFT JOIN study_property sp ON sp.study_id = s.study_id
      WHERE d.dataset_id = :datasetId
      """)
  Dataset findDatasetStudyById(@Bind("datasetId") Integer datasetId);

  @SqlQuery("SELECT dataset_id FROM dataset WHERE study_id = :studyId")
  List<Integer> findDatasetIdsByStudyId(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT
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
          fso.delete_user_id AS fso_delete_user_id,
          fso.delete_date AS fso_delete_date
      FROM dataset d
      LEFT JOIN study s ON s.study_id = d.study_id
      INNER JOIN file_storage_object fso
          ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text)
          AND fso.deleted = false
          AND fso.category = :category
      WHERE d.dataset_id = :datasetId
      ORDER BY GREATEST(fso.create_date, fso.update_date, fso.delete_date) DESC,
          fso.file_storage_object_id DESC
      LIMIT 1
      """)
  FileStorageObject findLatestFileByDatasetIdAndCategory(
      @Bind("datasetId") Integer datasetId, @Bind("category") String category);

  @SqlQuery("SELECT dataset_id FROM dataset WHERE dataset_id = :datasetId")
  Integer findDatasetIdById(@Bind("datasetId") Integer datasetId);

  @SqlQuery("SELECT dataset_id FROM dataset WHERE alias = :alias")
  Integer findDatasetIdByAlias(@Bind("alias") Integer alias);

  /**
   * Finds a fully populated dataset without joining all child collections into one Cartesian
   * product. The focused reads run in a single REPEATABLE READ transaction so that they all observe
   * the same snapshot; under the default READ COMMITTED level each statement would take its own
   * snapshot and the assembled dataset could mix pre- and post-commit state.
   */
  default Dataset findDatasetByAlias(Integer alias) {
    // A handle that is already in a transaction cannot open a nested one at a different isolation
    // level, so reuse the transaction the caller established rather than failing.
    if (isInTransaction()) {
      return assembleDatasetByAlias(this, alias);
    }
    return inTransaction(
        TransactionIsolationLevel.REPEATABLE_READ, dao -> assembleDatasetByAlias(dao, alias));
  }

  private Dataset assembleDatasetByAlias(DatasetDAO dao, Integer alias) {
    Integer datasetId = dao.findDatasetIdByAlias(alias);
    return datasetId == null ? null : assembleDataset(dao, datasetId, true);
  }

  private Dataset assembleDataset(DatasetDAO dao, Integer datasetId, boolean includeFiles) {
    Dataset dataset = dao.findDatasetDetailsById(datasetId);
    if (dataset == null) {
      return null;
    }

    // findDatasetDetailsById populates the study id, so a null one means there is no study to read.
    if (dataset.getStudyId() != null) {
      Dataset datasetWithStudy = dao.findDatasetStudyById(datasetId);
      if (datasetWithStudy != null && datasetWithStudy.getStudy() != null) {
        Study study = datasetWithStudy.getStudy();
        dao.findDatasetIdsByStudyId(study.getStudyId()).forEach(study::addDatasetId);
        if (includeFiles) {
          study.setAlternativeDataSharingPlan(
              dao.findLatestFileByDatasetIdAndCategory(
                  datasetId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue()));
        }
        dataset.setStudy(study);
      }
    }

    if (includeFiles) {
      dataset.setNihInstitutionalCertificationFile(
          dao.findLatestFileByDatasetIdAndCategory(
              datasetId, FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue()));
    }
    return dataset;
  }

  /**
   * Return a list of Dataset objects from a list of dataset aliases. This explicitly does NOT
   * populate study information due to performance issues.
   *
   * @param aliases List of dataset aliases
   * @return List of datasets
   */
  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
          SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
              d.update_user_id, d.object_id, d.dac_id, d.alias, d.data_use, d.translated_data_use,
              d.dac_approval, dar_ds_ids.id AS in_use, d.study_id, d.indexed_date,
              u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
              u.create_date AS u_create_date, u.email_preference AS u_email_preference,
              u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
              k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
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
              fso.delete_user_id AS fso_delete_user_id,
              fso.delete_date AS fso_delete_date
          FROM dataset d
          LEFT JOIN users u on d.create_user_id = u.user_id
          LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
          LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
          LEFT JOIN dictionary k ON k.key_id = dp.property_key
          LEFT JOIN LATERAL (
              SELECT *
              FROM file_storage_object
              WHERE entity_id = d.dataset_id::text
                  AND deleted = false
                  AND category = :fileCategory
              ORDER BY GREATEST(create_date, update_date, delete_date) DESC,
                  file_storage_object_id DESC
              LIMIT 1
          ) fso ON true
          WHERE d.alias IN (<aliases>)
      """)
  List<Dataset> findDatasetsByAliasAndFileCategory(
      @BindList(value = "aliases", onEmpty = EmptyHandling.NULL_STRING) List<Integer> aliases,
      @Bind("fileCategory") String fileCategory);

  default List<Dataset> findDatasetsByAlias(List<Integer> aliases) {
    return findDatasetsByAliasAndFileCategory(
        aliases, FileCategory.NIH_INSTITUTIONAL_CERTIFICATION.getValue());
  }

  @SqlUpdate("UPDATE dataset SET dac_id = :dacId WHERE dataset_id = :datasetId")
  void updateDatasetDacId(@Bind("datasetId") Integer datasetId, @Bind("dacId") Integer dacId);

  @SqlUpdate("UPDATE dataset SET name = :name WHERE dataset_id = :datasetId")
  void updateDatasetName(@Bind("datasetId") Integer datasetId, @Bind("name") String name);

  @SqlBatch(
      """
      INSERT INTO dataset_property
        (dataset_id, property_key, schema_property, property_value, property_type, create_date )
      VALUES
        (:datasetId, :propertyKey, :schemaProperty, :getPropertyValueAsString, :getPropertyTypeAsString, :createDate)
      """)
  void insertDatasetProperties(@BindBean @BindMethods List<DatasetProperty> dataSetPropertiesList);

  @SqlUpdate("DELETE FROM dataset_property WHERE dataset_id = :datasetId")
  void deleteDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

  @SqlUpdate(
      """
      INSERT INTO dataset_audit
        (dataset_id, change_action, modified_by_user, modification_date, object_id, name)
      VALUES
        (:datasetId, :action, :user, :date, :objectId, :name)
      """)
  @GetGeneratedKeys
  Integer insertDatasetAudit(@BindBean DatasetAudit dataSets);

  @UseRowMapper(DatasetAuditMapper.class)
  @SqlQuery(
      """
          SELECT *
          FROM dataset_audit
          WHERE dataset_id = :datasetId
      """)
  List<DatasetAudit> findAuditsByDatasetId(@Bind("datasetId") Integer datasetId);

  @SqlUpdate(
      "UPDATE dataset_property "
          + "SET property_value = :propertyValue "
          + "WHERE dataset_id = :datasetId "
          + "AND property_key = :propertyKey")
  void updateDatasetProperty(
      @Bind("datasetId") Integer datasetId,
      @Bind("propertyKey") Integer propertyKey,
      @Bind("propertyValue") String propertyValue);

  @SqlUpdate(
      """
          UPDATE dataset
          SET study_id = :studyId
          WHERE dataset_id = :datasetId
          """)
  void updateStudyId(@Bind("datasetId") Integer datasetId, @Bind("studyId") Integer studyId);

  @SqlUpdate(
      "DELETE from dataset_property "
          + "WHERE dataset_id = :datasetId "
          + "AND property_key = :propertyKey")
  void deleteDatasetPropertyByKey(
      @Bind("datasetId") Integer datasetId, @Bind("propertyKey") Integer propertyKey);

  @SqlUpdate("DELETE FROM dataset WHERE dataset_id = :datasetId")
  void deleteDatasetById(@Bind("datasetId") Integer datasetId);

  @SqlUpdate(
      """
      UPDATE dataset
      SET data_use = :dataUse,
          translated_data_use = :translation,
          update_date = current_timestamp,
          update_user_id = :updateUserId
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetDataUse(
      @Bind("datasetId") Integer datasetId,
      @Bind("dataUse") String dataUse,
      @Bind("translation") String translation,
      @Bind("updateUserId") Integer updateUserId);

  @SqlUpdate(
      """
      UPDATE dataset
      SET create_user_id = :createUserId
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetCreateUserId(
      @Bind("datasetId") Integer datasetId, @Bind("createUserId") Integer createUserId);

  @SqlUpdate(
      """
      UPDATE dataset
      SET name = :datasetName,
          update_date = :updateDate,
          update_user_id = :updateUserId
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetNameWithUpdateUser(
      @Bind("datasetId") Integer datasetId,
      @Bind("datasetName") String datasetName,
      @Bind("updateDate") Timestamp updateDate,
      @Bind("updateUserId") Integer updateUserId);

  @SqlUpdate(
      """
      UPDATE dataset
      SET update_date = :updateDate,
          update_user_id = :updateUserId
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetUpdateUser(
      @Bind("datasetId") Integer datasetId,
      @Bind("updateDate") Timestamp updateDate,
      @Bind("updateUserId") Integer updateUserId);

  @UseRowMapper(DatasetPropertyMapper.class)
  @SqlQuery(
      " SELECT p.*, d.key FROM dataset_property p "
          + " INNER JOIN dictionary d ON p.property_key = d.key_id "
          + " WHERE p.dataset_id = :datasetId ")
  Set<DatasetProperty> findDatasetPropertiesByDatasetId(@Bind("datasetId") Integer datasetId);

  @RegisterRowMapper(DictionaryMapper.class)
  @SqlQuery("SELECT * FROM dictionary ORDER BY key_id")
  List<Dictionary> getDictionaryTerms();

  @SqlQuery("SELECT * FROM dataset WHERE LOWER(name) = LOWER(:name)")
  Dataset getDatasetByName(@Bind("name") String name);

  @UseRowReducer(DatasetReducer.class)
  @SqlQuery(
      """
      SELECT distinct d.*, k.key, p.property_value, d.dac_id
      FROM dataset d
      LEFT JOIN dataset_property p ON p.dataset_id = d.dataset_id
      LEFT JOIN dictionary k ON k.key_id = p.property_key
      WHERE d.dac_id IN (<dacIds>)
      """)
  List<Dataset> findDatasetListByDacIds(
      @BindList(value = "dacIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> dacIds);

  @SqlQuery(
      """
      SELECT distinct d.dataset_id FROM dataset d WHERE d.dac_id IN (<dacIds>)
      """)
  List<Integer> findDatasetIdsByDacIds(
      @BindList(value = "dacIds", onEmpty = EmptyHandling.NULL_STRING) List<Integer> dacIds);

  @SqlUpdate(
      """
  UPDATE dataset
  SET
    dac_approval = :dacApproval,
    dac_approval_date = now(),
    update_date = :updateDate,
    update_user_id = :updateUserId
  WHERE dataset_id = :datasetId
  """)
  void updateDatasetApproval(
      @Bind("dacApproval") Boolean dacApproved,
      @Bind("updateDate") Instant updateDate,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("datasetId") Integer datasetId);

  @SqlUpdate(
      """
      UPDATE dataset
      SET indexed_date = :indexedDate
      WHERE dataset_id = :datasetId
      """)
  void updateDatasetIndexedDate(
      @Bind("datasetId") Integer datasetId, @Bind("indexedDate") Instant indexedDate);

  @SqlUpdate(
      """
        UPDATE dataset
        SET indexed_date = NOW()
        WHERE dataset_id IN (<datasetIds>)
    """)
  void updateDatasetsIndexedDate(@BindList("datasetIds") Set<Integer> datasetIds);

  @RegisterRowMapper(ApprovedDatasetMapper.class)
  @UseRowReducer(ApprovedDatasetReducer.class)
  @SqlQuery(
"""
SELECT DISTINCT c.dar_code, d.alias, d.name as dataset_name, dac.name as dac_name, dar.submission_date
FROM data_access_request dar
         LEFT JOIN dar_collection c on dar.collection_id = c.collection_id
         INNER JOIN library_card lc on dar.user_id = lc.user_id
         INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
         INNER JOIN dataset d on d.dataset_id = dd.dataset_id
         INNER JOIN dac on dac.dac_id = d.dac_id AND dac.deleted IS NOT TRUE
         INNER JOIN (
                SELECT DISTINCT e.reference_id, e.dataset_id, LAST_VALUE(v.vote)
                    OVER(
                        PARTITION BY e.reference_id, e.dataset_id
                        ORDER BY v.create_date
                        RANGE BETWEEN
                            UNBOUNDED PRECEDING AND
                            UNBOUNDED FOLLOWING
                        ) last_vote
                  FROM election e
                    INNER JOIN vote v ON e.election_id = v.election_id AND v.vote IS NOT NULL
                    AND LOWER(e.election_type) = 'dataaccess'
                    AND LOWER(v.type) IN ('final', 'radar_approve')) final_access_vote ON final_access_vote.reference_id = dar.reference_id AND final_access_vote.dataset_id = dd.dataset_id
WHERE dar.submission_date > now() - interval '8760 hours'
  AND final_access_vote.last_vote = TRUE
  AND dar.user_id = :userId
  AND dar.collection_id NOT IN (
    SELECT DISTINCT collection_id
    FROM data_access_request
    WHERE submission_date IS NOT NULL AND parent_id IS NOT NULL
      AND data ->> 'closeoutSupplement' IS NOT NULL)
  """)
  List<ApprovedDataset> getApprovedDatasets(@Bind("userId") Integer userId);

  @SqlQuery(
      """
    SELECT dataset.study_id from dataset
        INNER JOIN study on dataset.study_id = study.study_id
    WHERE dataset.dac_approval_date > now() - make_interval(hours => 24)
        AND dataset.dac_approval = true
        AND study.public_visibility = true
    """)
  List<Integer> getRecentDacApprovedDatasetStudyIds();

  @SqlQuery(
"""
      SELECT dataset.study_id from dataset
          INNER JOIN study on dataset.study_id = study.study_id
          INNER JOIN dataset_property on dataset.dataset_id = dataset_property.dataset_id
      WHERE dataset.create_date > now() - make_interval(hours => 24)
          AND study.public_visibility = true
          AND dataset_property.schema_property = 'accessManagement'
          AND dataset_property.property_value IN ('open', 'external')
""")
  List<Integer> getRecentlyCreatedOpenOrExternalDatasetStudyIds();

  /**
   * The joins that every showcase query reads eligibility from. Access follows {@link
   * Dataset#getAccessManagement()}: current key before legacy, case-insensitive, trimmed. A
   * soft-deleted DAC does not join, so its datasets are not DAC-approved here.
   */
  String SHOWCASE_ELIGIBILITY_JOINS =
      """
      FROM dataset d
      INNER JOIN study s ON s.study_id = d.study_id
      LEFT JOIN dac ON dac.dac_id = d.dac_id AND dac.deleted IS NOT TRUE
      LEFT JOIN LATERAL (
          SELECT am.value
          FROM (
              SELECT LOWER(
                      REGEXP_REPLACE(
                          p.property_value, '^[\\x01-\\x20]+|[\\x01-\\x20]+$', '', 'g')) AS value,
                  LOWER(p.schema_property) = LOWER(:accessManagementProperty) AS is_current,
                  p.property_id
              FROM dataset_property p
              WHERE p.dataset_id = d.dataset_id
                  AND LOWER(p.schema_property)
                      IN (LOWER(:accessManagementProperty), LOWER(:legacyAccessManagementProperty))
          ) am
          WHERE am.value IN ('open', 'controlled', 'external')
          ORDER BY am.is_current DESC, am.property_id
          LIMIT 1
      ) access ON TRUE
      """;

  /**
   * Eligibility: a public study, and open or external access or approval by a DAC that is not
   * deleted.
   */
  String SHOWCASE_ELIGIBLE =
      """
      s.public_visibility = true
          AND (access.value IN ('open', 'external')
              OR (d.dac_approval = true AND dac.dac_id IS NOT NULL))
      """;

  /** The S1.1 "new in DUOS" timestamp; the summary returns its date part. */
  String SHOWCASE_AVAILABLE_DATE =
      """
      CASE WHEN access.value IN ('open', 'external') THEN d.create_date
          ELSE d.dac_approval_date END
      """;

  /** Showcase summaries of the eligible datasets among the aliases, ordered by alias. */
  default List<ShowcaseDatasetSummary> findShowcaseSummaries(Collection<Integer> aliases) {
    if (aliases.isEmpty()) {
      return List.of();
    }
    return findShowcaseSummaries(
        aliases,
        ShowcasePublicField.datasetPropertyAllowlist().stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .toList(),
        ShowcasePublicField.studyPropertyAllowlist().stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .toList(),
        Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
        Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY);
  }

  /**
   * Eligibility as in {@link #SHOWCASE_ELIGIBLE}. Allowlisted properties match their lower-cased
   * names, skip blank values, and are keyed by lower-cased name.
   */
  @UseRowMapper(ShowcaseDatasetSummaryMapper.class)
  @SqlQuery(
      """
      SELECT d.alias, d.name, dac.name AS dac_name, d.data_use,
          access.value AS access_management,
      """
          + SHOWCASE_AVAILABLE_DATE
          + """
           AS available_date,
          dataset_props.properties AS dataset_properties,
          study_props.properties AS study_properties
      """
          + SHOWCASE_ELIGIBILITY_JOINS
          + """
      LEFT JOIN LATERAL (
          SELECT jsonb_object_agg(p.name, p.property_value) AS properties
          FROM (
              SELECT DISTINCT ON (LOWER(dp.schema_property))
                  LOWER(dp.schema_property) AS name, dp.property_value
              FROM dataset_property dp
              WHERE dp.dataset_id = d.dataset_id
                  AND LOWER(dp.schema_property) IN (<datasetPropertyAllowlist>)
                  AND dp.property_value !~ '^[\\x01-\\x20]*$'
              ORDER BY LOWER(dp.schema_property), dp.property_id
          ) p
      ) dataset_props ON TRUE
      LEFT JOIN LATERAL (
          SELECT jsonb_object_agg(p.name, p.value) AS properties
          FROM (
              SELECT DISTINCT ON (LOWER(sp.key)) LOWER(sp.key) AS name, sp.value
              FROM study_property sp
              WHERE sp.study_id = s.study_id
                  AND LOWER(sp.key) IN (<studyPropertyAllowlist>)
                  AND sp.value !~ '^[\\x01-\\x20]*$'
              ORDER BY LOWER(sp.key), sp.study_property_id
          ) p
      ) study_props ON TRUE
      WHERE d.alias IN (<aliases>)
          AND
      """
          + SHOWCASE_ELIGIBLE
          + """
      ORDER BY d.alias
      """)
  List<ShowcaseDatasetSummary> findShowcaseSummaries(
      @BindList(value = "aliases", onEmpty = EmptyHandling.NULL_STRING) Collection<Integer> aliases,
      @BindList(value = "datasetPropertyAllowlist", onEmpty = EmptyHandling.NULL_STRING)
          Collection<String> datasetPropertyAllowlist,
      @BindList(value = "studyPropertyAllowlist", onEmpty = EmptyHandling.NULL_STRING)
          Collection<String> studyPropertyAllowlist,
      @Bind("accessManagementProperty") String accessManagementProperty,
      @Bind("legacyAccessManagementProperty") String legacyAccessManagementProperty);

  /**
   * The aliases of the newest eligible datasets, newest date first and then in alias order. A
   * dataset with no date is left out. This returns aliases only; the caller reads the summaries
   * with {@link #findShowcaseSummaries(Collection)}.
   *
   * @param openAccessOnly true to keep open-access datasets only
   * @param dacNames exact DAC names to keep, or null for no DAC scope; an empty list keeps none
   * @param excludedAliases aliases never to return
   * @param limit the most aliases to return
   */
  default List<Integer> findShowcaseNewestAliases(
      boolean openAccessOnly,
      Collection<String> dacNames,
      Collection<Integer> excludedAliases,
      int limit) {
    if (limit <= 0 || (dacNames != null && dacNames.isEmpty())) {
      return List.of();
    }
    return findShowcaseNewestAliases(
        openAccessOnly,
        dacNames != null,
        dacNames == null ? List.of() : dacNames,
        !excludedAliases.isEmpty(),
        excludedAliases,
        limit,
        Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
        Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY);
  }

  /**
   * Each optional filter has its own switch, because an empty list binds as {@code IN (NULL)},
   * which matches nothing; with the switch off, the filter is true whatever the list holds.
   */
  @SqlQuery(
      "SELECT d.alias "
          + SHOWCASE_ELIGIBILITY_JOINS
          + "WHERE "
          + SHOWCASE_ELIGIBLE
          + """
          AND (:openAccessOnly = false OR access.value = 'open')
          AND (:scopeByDac = false OR dac.name IN (<dacNames>))
          AND (:hasExclusions = false OR d.alias NOT IN (<excludedAliases>))
          AND (
      """
          + SHOWCASE_AVAILABLE_DATE
          + """
          ) IS NOT NULL
      ORDER BY CAST(
      """
          + SHOWCASE_AVAILABLE_DATE
          + """
          AS date) DESC, d.alias
      LIMIT :limit
      """)
  List<Integer> findShowcaseNewestAliases(
      @Bind("openAccessOnly") boolean openAccessOnly,
      @Bind("scopeByDac") boolean scopeByDac,
      @BindList(value = "dacNames", onEmpty = EmptyHandling.NULL_STRING)
          Collection<String> dacNames,
      @Bind("hasExclusions") boolean hasExclusions,
      @BindList(value = "excludedAliases", onEmpty = EmptyHandling.NULL_STRING)
          Collection<Integer> excludedAliases,
      @Bind("limit") int limit,
      @Bind("accessManagementProperty") String accessManagementProperty,
      @Bind("legacyAccessManagementProperty") String legacyAccessManagementProperty);

  /**
   * One page of eligible aliases in alias order, starting after {@code afterAlias}. This returns
   * aliases only; the caller reads the summaries with {@link #findShowcaseSummaries(Collection)}.
   *
   * @param afterAlias the last alias of the previous page, or -1 for the first page
   * @param limit the most aliases to return
   */
  default List<Integer> findShowcaseAliasPage(int afterAlias, int limit) {
    return findShowcaseAliasPage(
        afterAlias,
        limit,
        Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
        Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY);
  }

  @SqlQuery(
      "SELECT d.alias "
          + SHOWCASE_ELIGIBILITY_JOINS
          + "WHERE "
          + SHOWCASE_ELIGIBLE
          + """
          AND d.alias > :afterAlias
      ORDER BY d.alias
      LIMIT :limit
      """)
  List<Integer> findShowcaseAliasPage(
      @Bind("afterAlias") int afterAlias,
      @Bind("limit") int limit,
      @Bind("accessManagementProperty") String accessManagementProperty,
      @Bind("legacyAccessManagementProperty") String legacyAccessManagementProperty);

  /**
   * The catalog-eligible datasets and their distinct studies, for the {@code stats} mode.
   *
   * @param dacNames exact DAC names to count, or null for the whole catalog; an empty list counts
   *     nothing
   */
  default ShowcaseStatsResponse countShowcaseDatasets(Collection<String> dacNames) {
    if (dacNames != null && dacNames.isEmpty()) {
      return new ShowcaseStatsResponse(0, 0);
    }
    return countShowcaseDatasets(
        dacNames != null,
        dacNames == null ? List.of() : dacNames,
        Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
        Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY);
  }

  @RegisterConstructorMapper(ShowcaseStatsResponse.class)
  @SqlQuery(
      "SELECT COUNT(DISTINCT d.study_id) AS study_count, COUNT(*) AS dataset_count "
          + SHOWCASE_ELIGIBILITY_JOINS
          + "WHERE "
          + SHOWCASE_ELIGIBLE
          + """
          AND (:scopeByDac = false OR dac.name IN (<dacNames>))
      """)
  ShowcaseStatsResponse countShowcaseDatasets(
      @Bind("scopeByDac") boolean scopeByDac,
      @BindList(value = "dacNames", onEmpty = EmptyHandling.NULL_STRING)
          Collection<String> dacNames,
      @Bind("accessManagementProperty") String accessManagementProperty,
      @Bind("legacyAccessManagementProperty") String legacyAccessManagementProperty);
}
