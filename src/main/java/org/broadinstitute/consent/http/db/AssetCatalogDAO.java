package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetCatalogEntry;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * One entry per requestable asset, holding its DUOS identifier number. An entry outlives its asset:
 * deleting the asset nulls the reference, and {@code asset_removed_date} records the removal.
 */
@RegisterConstructorMapper(AssetCatalogEntry.class)
public interface AssetCatalogDAO {

  /**
   * Returns 0 when the dataset is missing, already has an entry, or another entry has its alias.
   */
  default int insertDatasetEntry(Integer datasetId) {
    return insertDatasetEntries(List.of(datasetId));
  }

  /** Returns how many of the datasets got an entry. */
  @SqlUpdate(
      """
      INSERT INTO asset_catalog (asset_type, asset_number, dataset_id, source_dataset_id)
      SELECT 'DATASET', d.alias::integer, d.dataset_id, d.dataset_id
      FROM dataset d
      WHERE d.dataset_id IN (<datasetIds>)
        AND NOT EXISTS (SELECT 1 FROM asset_catalog c WHERE c.dataset_id = d.dataset_id)
      ON CONFLICT (asset_type, asset_number) DO NOTHING
      """)
  int insertDatasetEntries(@BindList("datasetIds") List<Integer> datasetIds);

  /** Numbers a model, workspace or biospecimen entry by its row id. */
  default void insertAssetEntry(AssetType type, Integer rowId) {
    switch (type) {
      case MODEL -> insertModelEntry(rowId);
      case WORKSPACE -> insertWorkspaceEntry(rowId);
      case BIOSPECIMEN -> insertBiospecimenEntry(rowId);
      case DATASET ->
          throw new IllegalArgumentException("A dataset entry is numbered by its alias.");
    }
  }

  @SqlUpdate(
      "INSERT INTO asset_catalog (asset_type, asset_number, model_id) VALUES ('MODEL', :id, :id)")
  void insertModelEntry(@Bind("id") Integer modelId);

  @SqlUpdate(
      """
      INSERT INTO asset_catalog (asset_type, asset_number, workspace_id)
      VALUES ('WORKSPACE', :id, :id)
      """)
  void insertWorkspaceEntry(@Bind("id") Integer workspaceId);

  @SqlUpdate(
      """
      INSERT INTO asset_catalog (asset_type, asset_number, biospecimen_id)
      VALUES ('BIOSPECIMEN', :id, :id)
      """)
  void insertBiospecimenEntry(@Bind("id") Integer biospecimenId);

  /**
   * Gives the dataset an entry if it has none, relinking the one a restore unlinked from it.
   *
   * @return the entry that holds the dataset's alias instead, or null when there is no collision or
   *     no such dataset
   */
  default AssetCatalogEntry ensureDatasetEntry(Integer datasetId) {
    return ensureDatasetEntries(List.of(datasetId)).stream().findFirst().orElse(null);
  }

  /**
   * Gives each dataset an entry if it has none, as {@link #ensureDatasetEntry} does.
   *
   * @return the entries that hold the alias of a dataset left without one
   */
  default List<AssetCatalogEntry> ensureDatasetEntries(List<Integer> datasetIds) {
    relinkDatasetEntries(datasetIds);
    insertDatasetEntries(datasetIds);
    return findDatasetCollisions(datasetIds);
  }

  @SqlUpdate(
      """
      UPDATE asset_catalog c SET dataset_id = d.dataset_id, asset_removed_date = NULL
      FROM dataset d
      WHERE d.dataset_id IN (<datasetIds>)
        AND c.asset_type = 'DATASET'
        AND c.asset_number = d.alias
        AND c.dataset_id IS NULL
        AND c.source_dataset_id = d.dataset_id
        AND NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id)
      """)
  int relinkDatasetEntries(@BindList("datasetIds") List<Integer> datasetIds);

  @SqlQuery(
      """
      SELECT c.*
      FROM asset_catalog c
      JOIN dataset d ON c.asset_type = 'DATASET' AND c.asset_number = d.alias
      WHERE d.dataset_id IN (<datasetIds>)
        AND c.dataset_id IS DISTINCT FROM d.dataset_id
        AND NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id)
      ORDER BY c.catalog_id
      """)
  List<AssetCatalogEntry> findDatasetCollisions(@BindList("datasetIds") List<Integer> datasetIds);

  /** Gives every model, workspace and biospecimen row of the study an entry, retired rows too. */
  default void ensureStudyEntries(Integer studyId) {
    relinkStudyEntries(studyId);
    insertStudyEntries(studyId);
  }

  // An entry's number is its row id, so the only entry a row can have is the one with its number.
  @SqlUpdate(
      """
      WITH study_rows AS (
          SELECT 'MODEL' AS asset_type, model_id AS row_id
          FROM study_model WHERE study_id = :studyId
          UNION ALL
          SELECT 'WORKSPACE', workspace_id FROM study_workspace WHERE study_id = :studyId
          UNION ALL
          SELECT 'BIOSPECIMEN', biospecimen_id FROM study_biospecimen WHERE study_id = :studyId
      )
      UPDATE asset_catalog c SET
          model_id = CASE WHEN c.asset_type = 'MODEL' THEN c.asset_number END,
          workspace_id = CASE WHEN c.asset_type = 'WORKSPACE' THEN c.asset_number END,
          biospecimen_id = CASE WHEN c.asset_type = 'BIOSPECIMEN' THEN c.asset_number END,
          asset_removed_date = NULL
      FROM study_rows r
      WHERE c.asset_type = r.asset_type
        AND c.asset_number = r.row_id
        AND c.model_id IS NULL
        AND c.workspace_id IS NULL
        AND c.biospecimen_id IS NULL
      """)
  int relinkStudyEntries(@Bind("studyId") Integer studyId);

  @SqlUpdate(
      """
      WITH study_rows AS (
          SELECT 'MODEL' AS asset_type, model_id AS row_id
          FROM study_model WHERE study_id = :studyId
          UNION ALL
          SELECT 'WORKSPACE', workspace_id FROM study_workspace WHERE study_id = :studyId
          UNION ALL
          SELECT 'BIOSPECIMEN', biospecimen_id FROM study_biospecimen WHERE study_id = :studyId
      )
      INSERT INTO asset_catalog (asset_type, asset_number, model_id, workspace_id, biospecimen_id)
      SELECT r.asset_type, r.row_id,
          CASE WHEN r.asset_type = 'MODEL' THEN r.row_id END,
          CASE WHEN r.asset_type = 'WORKSPACE' THEN r.row_id END,
          CASE WHEN r.asset_type = 'BIOSPECIMEN' THEN r.row_id END
      FROM study_rows r
      ON CONFLICT (asset_type, asset_number) DO NOTHING
      """)
  int insertStudyEntries(@Bind("studyId") Integer studyId);

  default int markRemovedByDataset(Integer datasetId) {
    return markRemovedByDatasets(List.of(datasetId));
  }

  @SqlUpdate(
      """
      UPDATE asset_catalog SET asset_removed_date = now()
      WHERE dataset_id IN (<datasetIds>) AND asset_removed_date IS NULL
      """)
  int markRemovedByDatasets(@BindList("datasetIds") List<Integer> datasetIds);

  @SqlUpdate(
      """
      WITH study_rows AS (
          SELECT 'MODEL' AS asset_type, model_id AS row_id
          FROM study_model WHERE study_id = :studyId
          UNION ALL
          SELECT 'WORKSPACE', workspace_id FROM study_workspace WHERE study_id = :studyId
          UNION ALL
          SELECT 'BIOSPECIMEN', biospecimen_id FROM study_biospecimen WHERE study_id = :studyId
      )
      UPDATE asset_catalog c SET asset_removed_date = now()
      FROM study_rows r
      WHERE c.asset_type = r.asset_type
        AND c.asset_number = r.row_id
        AND COALESCE(c.model_id, c.workspace_id, c.biospecimen_id) = r.row_id
        AND c.asset_removed_date IS NULL
      """)
  int markRemovedByStudy(@Bind("studyId") Integer studyId);

  /**
   * Moves dataset_alias_seq past the highest alias of the datasets when it is ahead of the
   * sequence, as an alias inserted by hand can be, so the sequence never reissues it. A sequence
   * already past every alias is left alone, without a lock. Call it inside a transaction.
   *
   * <p>An insert takes its alias from the sequence while it holds ROW EXCLUSIVE on dataset. The
   * table lock waits for every insert in flight and blocks new ones until commit, so no alias is
   * issued between the second check and setval. An insert that took the alias itself waits on the
   * unique index for this transaction, which deadlocks with the lock, and Postgres aborts one.
   */
  default void advanceAliasSequencePast(List<Integer> datasetIds) {
    if (hasAliasAheadOfSequence(datasetIds)) {
      lockDatasetTable();
      setAliasSequenceToHighestAlias(datasetIds);
    }
  }

  @SqlQuery(
      """
      SELECT EXISTS (
          SELECT 1 FROM dataset d, dataset_alias_seq s
          WHERE d.dataset_id IN (<datasetIds>)
            AND d.alias >= (CASE WHEN s.is_called THEN s.last_value + 1 ELSE s.last_value END))
      """)
  boolean hasAliasAheadOfSequence(@BindList("datasetIds") List<Integer> datasetIds);

  @SqlUpdate("LOCK TABLE dataset IN SHARE ROW EXCLUSIVE MODE")
  void lockDatasetTable();

  /**
   * Returns the new sequence value, or null when the sequence is already past every alias. Some
   * databases type dataset.alias as numeric, and setval takes only bigint.
   */
  @SqlQuery(
      """
      SELECT setval('dataset_alias_seq', top.alias::bigint)
      FROM (SELECT MAX(alias) AS alias FROM dataset WHERE dataset_id IN (<datasetIds>)) top,
          dataset_alias_seq s
      WHERE top.alias >= (CASE WHEN s.is_called THEN s.last_value + 1 ELSE s.last_value END)
      """)
  Long setAliasSequenceToHighestAlias(@BindList("datasetIds") List<Integer> datasetIds);

  @SqlQuery(
      """
      SELECT * FROM asset_catalog
      WHERE asset_type = :type AND asset_number = :number
      """)
  AssetCatalogEntry findByTypeAndNumber(
      @Bind("type") AssetType type, @Bind("number") Integer number);
}
