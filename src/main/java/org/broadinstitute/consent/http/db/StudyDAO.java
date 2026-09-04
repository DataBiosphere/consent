package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.db.mapper.FileStorageObjectMapperWithFSOPrefix;
import org.broadinstitute.consent.http.db.mapper.StudyReducer;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindList.EmptyHandling;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterBeanMapper(Study.class)
@RegisterRowMapper(FileStorageObjectMapperWithFSOPrefix.class)
public interface StudyDAO extends Transactional<StudyDAO> {

  /**
   * Finds a fully populated study without joining all child collections into one Cartesian product.
   * The focused reads run in a single REPEATABLE READ transaction so that they all observe the same
   * snapshot; under the default READ COMMITTED level each statement would take its own snapshot and
   * the assembled study could mix pre- and post-commit state.
   */
  default Study findStudyById(Integer studyId) {
    // A handle that is already in a transaction cannot open a nested one at a different isolation
    // level, so reuse the transaction the caller established rather than failing.
    if (isInTransaction()) {
      return assembleStudy(this, studyId);
    }
    return inTransaction(
        TransactionIsolationLevel.REPEATABLE_READ, dao -> assembleStudy(dao, studyId));
  }

  private Study assembleStudy(StudyDAO dao, Integer studyId) {
    Study study = dao.findStudyDetailsById(studyId);
    if (study == null) {
      return null;
    }

    dao.findDatasetIdsByStudyId(studyId).forEach(study::addDatasetId);
    study.setAlternativeDataSharingPlan(
        dao.findLatestFileByStudyIdAndCategory(
            studyId, FileCategory.ALTERNATIVE_DATA_SHARING_PLAN.getValue()));
    return study;
  }

  @UseRowReducer(StudyReducer.class)
  @SqlQuery(
      """
      SELECT
          s.*,
          i.institution_name AS pi_institution_name,
          sp.study_property_id AS sp_study_property_id,
          sp.study_id AS sp_study_id,
          sp.key AS sp_key,
          sp.value AS sp_value,
          sp.type AS sp_type
      FROM
          study s
      LEFT JOIN institution i ON i.institution_id = s.pi_institution_id
      LEFT JOIN study_property sp ON sp.study_id = s.study_id
      WHERE s.study_id = :studyId
      """)
  Study findStudyDetailsById(@Bind("studyId") Integer studyId);

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
      FROM study s
      INNER JOIN file_storage_object fso
          ON fso.entity_id = s.uuid::text
          AND fso.deleted = false
          AND fso.category = :category
      WHERE s.study_id = :studyId
      ORDER BY GREATEST(fso.create_date, fso.update_date, fso.delete_date) DESC,
          fso.file_storage_object_id DESC
      LIMIT 1
      """)
  FileStorageObject findLatestFileByStudyIdAndCategory(
      @Bind("studyId") Integer studyId, @Bind("category") String category);

  @SqlUpdate(
      """
          INSERT INTO study (
              name, description,
              pi_name, pi_email, data_types,
              public_visibility,
              create_user_id, create_date,
              uuid
          ) VALUES (
              :name, :description,
              :piName, :piEmail, :dataTypes,
              :publicVisibility,
              :createUserId, :createDate,
              :uuid
          )
      """)
  @GetGeneratedKeys
  Integer insertStudy(
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("piName") String piName,
      @Bind("piEmail") String piEmail,
      @Bind("dataTypes") List<String> dataTypes,
      @Bind("publicVisibility") Boolean publicVisibility,
      @Bind("createUserId") Integer createUserId,
      @Bind("createDate") Instant createDate,
      @Bind("uuid") UUID uuid);

  /**
   * Sets the PI institution on a newly registered study. Registration collects the PI institution
   * as the numeric `piInstitution` field, but the study page reads the column, so the create path
   * records both. Separate from {@link #insertStudy} so the many callers that register a study
   * without one are unaffected.
   */
  @SqlUpdate("UPDATE study SET pi_institution_id = :piInstitutionId WHERE study_id = :studyId")
  void updateStudyPiInstitutionId(
      @Bind("studyId") Integer studyId, @Bind("piInstitutionId") Integer piInstitutionId);

  @SqlUpdate(
      """
          UPDATE study
          SET name = :name,
              description = :description,
              pi_name = :piName,
              pi_email = :piEmail,
              pi_institution_id = :piInstitutionId,
              pi_orcid = :piOrcid,
              pi_linkedin_url = :piLinkedinUrl,
              pi_website_url = :piWebsiteUrl,
              data_types = :dataTypes,
              public_visibility = :publicVisibility,
              update_user_id = :updateUserId,
              update_date = :updateDate
          WHERE study_id = :studyId
      """)
  void updateStudy(
      @Bind("studyId") Integer studyId,
      @Bind("name") String name,
      @Bind("description") String description,
      @Bind("piName") String piName,
      @Bind("piEmail") String piEmail,
      @Bind("piInstitutionId") Integer piInstitutionId,
      @Bind("piOrcid") String piOrcid,
      @Bind("piLinkedinUrl") String piLinkedinUrl,
      @Bind("piWebsiteUrl") String piWebsiteUrl,
      @Bind("dataTypes") List<String> dataTypes,
      @Bind("publicVisibility") Boolean publicVisibility,
      @Bind("updateUserId") Integer updateUserId,
      @Bind("updateDate") Instant updateDate);

  @SqlUpdate(
      """
          INSERT INTO study_property (
              study_id, key,
              type, value
          ) VALUES (
              :studyId, :key,
              :type, :value
          )
      """)
  @GetGeneratedKeys
  Integer insertStudyProperty(
      @Bind("studyId") Integer studyId,
      @Bind("key") String key,
      @Bind("type") String type,
      @Bind("value") String value);

  @SqlUpdate(
      """
          UPDATE study_property
          SET value = :value
          WHERE study_id = :studyId
          AND key = :key
          AND type = :type
      """)
  void updateStudyProperty(
      @Bind("studyId") Integer studyId,
      @Bind("key") String key,
      @Bind("type") String type,
      @Bind("value") String value);

  @SqlUpdate(
      """
          WITH property_deletes AS (
              DELETE from study_property where study_id = :studyId returning study_id
          )
          DELETE FROM study WHERE study_id in (select study_id from property_deletes)
      """)
  void deleteStudyByStudyId(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      DELETE FROM study_property where study_id = :studyId
      """)
  void deleteStudyPropertiesByStudyId(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      DELETE FROM study_property WHERE study_id = :studyId AND key = :key
      """)
  void deleteStudyPropertyByKey(@Bind("studyId") Integer studyId, @Bind("key") String key);

  @UseRowReducer(StudyReducer.class)
  @SqlQuery(
      """
      SELECT s.*, i.institution_name AS pi_institution_name
      FROM study s
      LEFT JOIN institution i ON i.institution_id = s.pi_institution_id
      WHERE s.name = :name
      """)
  Study findStudyByName(@Bind("name") String name);

  @RegisterConstructorMapper(StudyDatasetCountRecord.class)
  @SqlQuery(
      """
      SELECT
          study.study_id AS id,
          study.name,
          string_agg(DISTINCT prop.property_value, ',' ORDER BY prop.property_value) AS access_types,
          count(DISTINCT dataset.dataset_id) AS dataset_count
      FROM study
          INNER JOIN dataset
              ON study.study_id = dataset.study_id
          INNER JOIN LATERAL (
              SELECT dataset_property.property_value
              FROM dataset_property
              WHERE dataset_property.dataset_id = dataset.dataset_id
                AND LOWER(dataset_property.schema_property)
                    IN ('accessmanagement', 'consentgroup.accessmanagement')
              ORDER BY
                  CASE
                      WHEN LOWER(dataset_property.schema_property) = 'accessmanagement' THEN 0
                      ELSE 1
                  END
              LIMIT 1
          ) prop ON TRUE
      WHERE study.study_id IN (<studyIds>)
      GROUP BY study.study_id, study.name
      """)
  List<StudyDatasetCountRecord> findStudyDatasetCounts(
      @BindList(value = "studyIds", onEmpty = EmptyHandling.NULL_STRING) Set<Integer> studyIds);
}
