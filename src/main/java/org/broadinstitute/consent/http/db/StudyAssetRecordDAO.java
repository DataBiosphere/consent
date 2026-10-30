package org.broadinstitute.consent.http.db;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.models.StoredStudyProperty;
import org.broadinstitute.consent.http.models.StudyAssetKey;
import org.broadinstitute.consent.http.models.StudyBiospecimenRecord;
import org.broadinstitute.consent.http.models.StudyModelRecord;
import org.broadinstitute.consent.http.models.StudyWorkspaceRecord;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Permanent rows for a study's models, workspaces and biospecimens. Updates and retirement only
 * touch live rows of the given study, so an id from another study or a retired row changes nothing.
 */
@RegisterConstructorMapper(StudyAssetKey.class)
public interface StudyAssetRecordDAO {

  /** Locks the study row, as a registration update's study write does. Empty for no such study. */
  @SqlQuery("SELECT study_id FROM study WHERE study_id = :studyId FOR UPDATE")
  Optional<Integer> lockStudy(@Bind("studyId") Integer studyId);

  /** A study's properties as stored, including values that do not parse. */
  @RegisterConstructorMapper(StoredStudyProperty.class)
  @SqlQuery(
      """
      SELECT study_property_id, key, type, value FROM study_property
      WHERE study_id = :studyId
      ORDER BY study_property_id
      """)
  List<StoredStudyProperty> findStudyProperties(@Bind("studyId") Integer studyId);

  @SqlUpdate("UPDATE study_property SET value = :value WHERE study_property_id = :id")
  void updateStudyPropertyValue(@Bind("id") Integer studyPropertyId, @Bind("value") String value);

  @SqlUpdate("DELETE FROM study_property WHERE study_property_id = :id")
  void deleteStudyProperty(@Bind("id") Integer studyPropertyId);

  @SqlBatch(
      """
      INSERT INTO study_model (
          study_id, legacy_key, name, description, url, cloud, format, license,
          trained_on_datasets, maintainer, tags, access_management, data_use
      ) VALUES (
          :studyId, :legacyKey, :name, :description, :url, :cloud, :format, :license,
          :trainedOnDatasets, :maintainer::jsonb, :tags, :accessManagement, :dataUse::jsonb
      )
      """)
  @GetGeneratedKeys("model_id")
  List<Integer> insertModels(@BindMethods List<StudyModelRecord> models);

  @SqlBatch(
      """
      UPDATE study_model SET
          name = :name, description = :description, url = :url, cloud = :cloud,
          format = :format, license = :license, trained_on_datasets = :trainedOnDatasets,
          maintainer = :maintainer::jsonb, tags = :tags, access_management = :accessManagement,
          data_use = :dataUse::jsonb, update_date = now()
      WHERE model_id = :modelId AND study_id = :studyId AND retired_date IS NULL
      """)
  int[] updateModelRows(@BindMethods List<StudyModelRecord> models);

  default int[] updateModels(List<StudyModelRecord> models) {
    models.forEach(r -> requireIds(r.modelId(), r.studyId()));
    return updateModelRows(models);
  }

  @RegisterConstructorMapper(StudyModelRecord.class)
  @SqlQuery("SELECT * FROM study_model WHERE study_id = :studyId ORDER BY model_id")
  List<StudyModelRecord> findModelsByStudyId(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT model_id AS id, legacy_key FROM study_model
      WHERE study_id = :studyId AND retired_date IS NULL
      ORDER BY model_id
      """)
  List<StudyAssetKey> findLiveModels(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT legacy_key FROM study_model
      WHERE study_id = :studyId AND legacy_key IS NOT NULL
      ORDER BY legacy_key
      """)
  List<String> findUsedModelLegacyKeys(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      UPDATE study_model SET retired_date = now(), update_date = now()
      WHERE study_id = :studyId AND retired_date IS NULL AND model_id <> ALL(:keepIds)
      """)
  int retireModelsNotIn(@Bind("studyId") Integer studyId, @Bind("keepIds") List<Integer> keepIds);

  default int retireModelsExcept(Integer studyId, List<Integer> keepIds) {
    return retireModelsNotIn(studyId, requireKeepIds(keepIds));
  }

  @SqlBatch(
      """
      INSERT INTO study_workspace (
          study_id, legacy_key, name, platform, url, cloud, description, tools, access, tags,
          access_management, data_use
      ) VALUES (
          :studyId, :legacyKey, :name, :platform, :url, :cloud, :description, :tools, :access, :tags,
          :accessManagement, :dataUse::jsonb
      )
      """)
  @GetGeneratedKeys("workspace_id")
  List<Integer> insertWorkspaces(@BindMethods List<StudyWorkspaceRecord> workspaces);

  @SqlBatch(
      """
      UPDATE study_workspace SET
          name = :name, platform = :platform, url = :url, cloud = :cloud,
          description = :description, tools = :tools, access = :access, tags = :tags,
          access_management = :accessManagement, data_use = :dataUse::jsonb, update_date = now()
      WHERE workspace_id = :workspaceId AND study_id = :studyId AND retired_date IS NULL
      """)
  int[] updateWorkspaceRows(@BindMethods List<StudyWorkspaceRecord> workspaces);

  default int[] updateWorkspaces(List<StudyWorkspaceRecord> workspaces) {
    workspaces.forEach(r -> requireIds(r.workspaceId(), r.studyId()));
    return updateWorkspaceRows(workspaces);
  }

  @RegisterConstructorMapper(StudyWorkspaceRecord.class)
  @SqlQuery("SELECT * FROM study_workspace WHERE study_id = :studyId ORDER BY workspace_id")
  List<StudyWorkspaceRecord> findWorkspacesByStudyId(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT workspace_id AS id, legacy_key FROM study_workspace
      WHERE study_id = :studyId AND retired_date IS NULL
      ORDER BY workspace_id
      """)
  List<StudyAssetKey> findLiveWorkspaces(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT legacy_key FROM study_workspace
      WHERE study_id = :studyId AND legacy_key IS NOT NULL
      ORDER BY legacy_key
      """)
  List<String> findUsedWorkspaceLegacyKeys(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      UPDATE study_workspace SET retired_date = now(), update_date = now()
      WHERE study_id = :studyId AND retired_date IS NULL AND workspace_id <> ALL(:keepIds)
      """)
  int retireWorkspacesNotIn(
      @Bind("studyId") Integer studyId, @Bind("keepIds") List<Integer> keepIds);

  default int retireWorkspacesExcept(Integer studyId, List<Integer> keepIds) {
    return retireWorkspacesNotIn(studyId, requireKeepIds(keepIds));
  }

  @SqlBatch(
      """
      INSERT INTO study_biospecimen (
          study_id, legacy_key, sample_id, donor_id, specimen_type, preservation_method,
          preservation_details, date_of_collection, post_mortem_interval_value,
          post_mortem_interval_unit, sex, age, race, country_of_origin, extracted_diagnoses,
          pathology, organization, source_site, optional_data_use
      ) VALUES (
          :studyId, :legacyKey, :sampleId, :donorId, :specimenType, :preservationMethod,
          :preservationDetails, :dateOfCollection, :postMortemIntervalValue,
          :postMortemIntervalUnit, :sex, :age, :race, :countryOfOrigin, :extractedDiagnoses,
          :pathology, :organization, :sourceSite, :optionalDataUse
      )
      """)
  @GetGeneratedKeys("biospecimen_id")
  List<Integer> insertBiospecimens(@BindMethods List<StudyBiospecimenRecord> biospecimens);

  @SqlBatch(
      """
      UPDATE study_biospecimen SET
          sample_id = :sampleId, donor_id = :donorId, specimen_type = :specimenType,
          preservation_method = :preservationMethod, preservation_details = :preservationDetails,
          date_of_collection = :dateOfCollection,
          post_mortem_interval_value = :postMortemIntervalValue,
          post_mortem_interval_unit = :postMortemIntervalUnit, sex = :sex, age = :age,
          race = :race, country_of_origin = :countryOfOrigin,
          extracted_diagnoses = :extractedDiagnoses, pathology = :pathology,
          organization = :organization, source_site = :sourceSite,
          optional_data_use = :optionalDataUse, update_date = now()
      WHERE biospecimen_id = :biospecimenId AND study_id = :studyId AND retired_date IS NULL
      """)
  int[] updateBiospecimenRows(@BindMethods List<StudyBiospecimenRecord> biospecimens);

  default int[] updateBiospecimens(List<StudyBiospecimenRecord> biospecimens) {
    biospecimens.forEach(r -> requireIds(r.biospecimenId(), r.studyId()));
    return updateBiospecimenRows(biospecimens);
  }

  @RegisterConstructorMapper(StudyBiospecimenRecord.class)
  @SqlQuery("SELECT * FROM study_biospecimen WHERE study_id = :studyId ORDER BY biospecimen_id")
  List<StudyBiospecimenRecord> findBiospecimensByStudyId(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT biospecimen_id AS id, legacy_key FROM study_biospecimen
      WHERE study_id = :studyId AND retired_date IS NULL
      ORDER BY biospecimen_id
      """)
  List<StudyAssetKey> findLiveBiospecimens(@Bind("studyId") Integer studyId);

  @KeyColumn("biospecimen_id")
  @ValueColumn("sample_id")
  @SqlQuery(
      """
      SELECT biospecimen_id, sample_id FROM study_biospecimen
      WHERE study_id = :studyId AND retired_date IS NULL AND sample_id IS NOT NULL
      """)
  Map<Integer, String> findLiveBiospecimenSampleIds(@Bind("studyId") Integer studyId);

  @SqlQuery(
      """
      SELECT legacy_key FROM study_biospecimen
      WHERE study_id = :studyId AND legacy_key IS NOT NULL
      ORDER BY legacy_key
      """)
  List<String> findUsedBiospecimenLegacyKeys(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      UPDATE study_biospecimen SET retired_date = now(), update_date = now()
      WHERE study_id = :studyId AND retired_date IS NULL AND biospecimen_id <> ALL(:keepIds)
      """)
  int retireBiospecimensNotIn(
      @Bind("studyId") Integer studyId, @Bind("keepIds") List<Integer> keepIds);

  default int retireBiospecimensExcept(Integer studyId, List<Integer> keepIds) {
    return retireBiospecimensNotIn(studyId, requireKeepIds(keepIds));
  }

  /** {@code <> ALL} is NULL, so retires nothing, when the list or any element is null. */
  private static List<Integer> requireKeepIds(List<Integer> keepIds) {
    if (keepIds == null || keepIds.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("keepIds must be a list with no null ids");
    }
    return keepIds;
  }

  /** A null id matches no row, which would look the same as a retired one. */
  private static void requireIds(Integer rowId, Integer studyId) {
    if (rowId == null || studyId == null) {
      throw new IllegalArgumentException("Updates need a row id and a study id");
    }
  }
}
