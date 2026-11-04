package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * The writes of an asset catalog repair, each returning the entries it changed. Run them inside one
 * transaction that holds {@link #tryRepairLock}; none touches an entry that references an asset.
 */
@RegisterConstructorMapper(AssetCatalogRepairDAO.ChangedEntry.class)
public interface AssetCatalogRepairDAO {

  /** False while another transaction holds it; released when this transaction ends. */
  @SqlQuery("SELECT pg_try_advisory_xact_lock(hashtext('asset_catalog_repair'))")
  boolean tryRepairLock();

  @SqlQuery(
      """
      WITH asset_rows AS (
          SELECT 'MODEL' AS asset_type, model_id AS row_id FROM study_model
          UNION ALL
          SELECT 'WORKSPACE', workspace_id FROM study_workspace
          UNION ALL
          SELECT 'BIOSPECIMEN', biospecimen_id FROM study_biospecimen
      )
      UPDATE asset_catalog c SET
          model_id = CASE WHEN c.asset_type = 'MODEL' THEN c.asset_number END,
          workspace_id = CASE WHEN c.asset_type = 'WORKSPACE' THEN c.asset_number END,
          biospecimen_id = CASE WHEN c.asset_type = 'BIOSPECIMEN' THEN c.asset_number END,
          asset_removed_date = NULL
      FROM asset_rows r
      WHERE c.asset_type = r.asset_type
        AND c.asset_number = r.row_id
        AND num_nonnulls(c.dataset_id, c.model_id, c.workspace_id, c.biospecimen_id) = 0
      RETURNING c.catalog_id, c.asset_type, c.asset_number
      """)
  List<ChangedEntry> relinkAssetEntries();

  /** Only an entry made for the same dataset, whose number is still its alias. */
  @SqlQuery(
      """
      UPDATE asset_catalog c SET dataset_id = d.dataset_id, asset_removed_date = NULL
      FROM dataset d
      WHERE c.asset_type = 'DATASET'
        AND c.asset_number = d.alias
        AND c.source_dataset_id = d.dataset_id
        AND num_nonnulls(c.dataset_id, c.model_id, c.workspace_id, c.biospecimen_id) = 0
        AND NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id)
      RETURNING c.catalog_id, c.asset_type, c.asset_number
      """)
  List<ChangedEntry> relinkDatasetEntries();

  /** A dataset whose alias another entry holds is a collision, left for a person to fix. */
  @SqlQuery(
      """
      INSERT INTO asset_catalog (asset_type, asset_number, dataset_id, source_dataset_id)
      SELECT 'DATASET', d.alias::integer, d.dataset_id, d.dataset_id
      FROM dataset d
      WHERE NOT EXISTS (SELECT 1 FROM asset_catalog c WHERE c.dataset_id = d.dataset_id)
      ON CONFLICT (asset_type, asset_number) DO NOTHING
      RETURNING catalog_id, asset_type, asset_number
      """)
  List<ChangedEntry> insertMissingDatasetEntries();

  @SqlQuery(
      """
      WITH asset_rows AS (
          SELECT 'MODEL' AS asset_type, model_id AS row_id FROM study_model
          UNION ALL
          SELECT 'WORKSPACE', workspace_id FROM study_workspace
          UNION ALL
          SELECT 'BIOSPECIMEN', biospecimen_id FROM study_biospecimen
      )
      INSERT INTO asset_catalog (asset_type, asset_number, model_id, workspace_id, biospecimen_id)
      SELECT r.asset_type, r.row_id,
          CASE WHEN r.asset_type = 'MODEL' THEN r.row_id END,
          CASE WHEN r.asset_type = 'WORKSPACE' THEN r.row_id END,
          CASE WHEN r.asset_type = 'BIOSPECIMEN' THEN r.row_id END
      FROM asset_rows r
      WHERE NOT EXISTS (
          SELECT 1 FROM asset_catalog c
          WHERE c.asset_type = r.asset_type AND c.asset_number = r.row_id)
      ON CONFLICT (asset_type, asset_number) DO NOTHING
      RETURNING catalog_id, asset_type, asset_number
      """)
  List<ChangedEntry> insertMissingAssetEntries();

  /** The orphans {@link AssetCatalogCheckDAO#findOrphans} reports. */
  @SqlQuery(
      """
      WITH asset_rows AS (
          SELECT 'DATASET' AS asset_type, dataset_id AS row_id, alias::integer AS asset_number
          FROM dataset
          UNION ALL
          SELECT 'MODEL', model_id, model_id FROM study_model
          UNION ALL
          SELECT 'WORKSPACE', workspace_id, workspace_id FROM study_workspace
          UNION ALL
          SELECT 'BIOSPECIMEN', biospecimen_id, biospecimen_id FROM study_biospecimen
      )
      UPDATE asset_catalog c SET asset_removed_date = now()
      WHERE num_nonnulls(c.dataset_id, c.model_id, c.workspace_id, c.biospecimen_id) = 0
        AND c.asset_removed_date IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM asset_rows r
            WHERE r.asset_type = c.asset_type
              AND r.asset_number = c.asset_number
              AND NOT EXISTS (
                  SELECT 1 FROM asset_catalog e
                  WHERE e.asset_type = r.asset_type
                    AND COALESCE(e.dataset_id, e.model_id, e.workspace_id, e.biospecimen_id)
                        = r.row_id))
      RETURNING c.catalog_id, c.asset_type, c.asset_number
      """)
  List<ChangedEntry> stampOrphans();

  /**
   * Returns the new sequence value, or null when it is already past every alias and dataset entry
   * number. Call it holding {@link AssetCatalogDAO#lockDatasetTable}.
   */
  @SqlQuery(
      """
      SELECT setval('dataset_alias_seq', top.n::bigint)
      FROM (
          SELECT GREATEST(
              (SELECT MAX(alias) FROM dataset),
              (SELECT MAX(asset_number) FROM asset_catalog WHERE asset_type = 'DATASET')) AS n
      ) top, dataset_alias_seq s
      WHERE top.n >= (CASE WHEN s.is_called THEN s.last_value + 1 ELSE s.last_value END)
      """)
  Long advanceAliasSequence();

  record ChangedEntry(Integer catalogId, AssetType assetType, Integer assetNumber) {}
}
