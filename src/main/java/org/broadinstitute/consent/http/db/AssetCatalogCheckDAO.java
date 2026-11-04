package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.AssetCatalogCheck.Finding;
import org.broadinstitute.consent.http.models.AssetCatalogCheck.Findings;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

/**
 * Finds drift between asset_catalog and the rows it numbers. Each query returns at most {@code
 * :limit} findings, each carrying the total count of its kind.
 */
@RegisterConstructorMapper(AssetCatalogCheckDAO.FindingRow.class)
public interface AssetCatalogCheckDAO extends Transactional<AssetCatalogCheckDAO> {

  /** Every kind read from one snapshot, so a row cannot be counted under two kinds. */
  default AssetCatalogCheck check() {
    return inTransaction(
        TransactionIsolationLevel.REPEATABLE_READ,
        dao ->
            new AssetCatalogCheck(
                findings(dao.findOrphans(AssetCatalogCheck.FINDING_LIMIT)),
                findings(dao.findMissing(AssetCatalogCheck.FINDING_LIMIT)),
                findings(dao.findCollisions(AssetCatalogCheck.FINDING_LIMIT)),
                findings(dao.findMismatches(AssetCatalogCheck.FINDING_LIMIT)),
                findings(dao.findRelinkable(AssetCatalogCheck.FINDING_LIMIT)),
                findings(dao.findSequenceBehind(AssetCatalogCheck.FINDING_LIMIT))));
  }

  private static Findings findings(List<FindingRow> rows) {
    long count = rows.isEmpty() ? 0 : rows.get(0).total();
    return new Findings(
        count,
        rows.stream()
            .map(r -> new Finding(r.assetType(), r.assetNumber(), r.catalogId(), r.rowId()))
            .toList());
  }

  /**
   * Entries that reference nothing and carry no removal stamp, which means their asset was deleted
   * outside the app's delete paths. An entry a row of its own number could be relinked to is
   * relinkable instead.
   */
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
      SELECT c.asset_type, c.asset_number, c.catalog_id, NULL::integer AS row_id,
          count(*) OVER () AS total
      FROM asset_catalog c
      WHERE num_nonnulls(c.dataset_id, c.model_id, c.workspace_id, c.biospecimen_id) = 0
        AND c.asset_removed_date IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM asset_rows r
            WHERE r.asset_type = c.asset_type
              AND r.asset_number = c.asset_number
              AND (r.asset_type <> 'DATASET' OR c.source_dataset_id = r.row_id)
              AND NOT EXISTS (
                  SELECT 1 FROM asset_catalog e
                  WHERE e.asset_type = r.asset_type
                    AND COALESCE(e.dataset_id, e.model_id, e.workspace_id, e.biospecimen_id)
                        = r.row_id))
      ORDER BY c.catalog_id
      LIMIT :limit
      """)
  List<FindingRow> findOrphans(@Bind("limit") int limit);

  /** Rows with no entry whose number no entry of their type holds. */
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
      SELECT r.asset_type, r.asset_number, NULL::integer AS catalog_id, r.row_id,
          count(*) OVER () AS total
      FROM asset_rows r
      WHERE NOT EXISTS (
          SELECT 1 FROM asset_catalog e
          WHERE e.asset_type = r.asset_type
            AND COALESCE(e.dataset_id, e.model_id, e.workspace_id, e.biospecimen_id) = r.row_id)
        AND NOT EXISTS (
            SELECT 1 FROM asset_catalog c
            WHERE c.asset_type = r.asset_type AND c.asset_number = r.asset_number)
      ORDER BY r.asset_type, r.row_id
      LIMIT :limit
      """)
  List<FindingRow> findMissing(@Bind("limit") int limit);

  /**
   * Datasets with no entry whose alias another entry holds: one that references another dataset, or
   * an unlinked one created for a different dataset. Only an ad hoc insert causes one.
   */
  @SqlQuery(
      """
      SELECT 'DATASET' AS asset_type, d.alias::integer AS asset_number, c.catalog_id,
          d.dataset_id AS row_id, count(*) OVER () AS total
      FROM dataset d
      JOIN asset_catalog c ON c.asset_type = 'DATASET' AND c.asset_number = d.alias
      WHERE NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id)
        AND (c.dataset_id IS NOT NULL OR c.source_dataset_id IS DISTINCT FROM d.dataset_id)
      ORDER BY d.dataset_id
      LIMIT :limit
      """)
  List<FindingRow> findCollisions(@Bind("limit") int limit);

  /** Dataset entries whose number is no longer their dataset's alias. */
  @SqlQuery(
      """
      SELECT 'DATASET' AS asset_type, c.asset_number, c.catalog_id, d.dataset_id AS row_id,
          count(*) OVER () AS total
      FROM asset_catalog c
      JOIN dataset d ON d.dataset_id = c.dataset_id
      WHERE c.asset_number <> d.alias
      ORDER BY c.catalog_id
      LIMIT :limit
      """)
  List<FindingRow> findMismatches(@Bind("limit") int limit);

  /**
   * Rows with no entry whose number belongs to an unlinked entry of their type, which a restore
   * done without the catalog leaves behind. A dataset's entry must also have been created for it.
   */
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
      SELECT r.asset_type, r.asset_number, c.catalog_id, r.row_id, count(*) OVER () AS total
      FROM asset_rows r
      JOIN asset_catalog c ON c.asset_type = r.asset_type AND c.asset_number = r.asset_number
      WHERE num_nonnulls(c.dataset_id, c.model_id, c.workspace_id, c.biospecimen_id) = 0
        AND (r.asset_type <> 'DATASET' OR c.source_dataset_id = r.row_id)
        AND NOT EXISTS (
            SELECT 1 FROM asset_catalog e
            WHERE e.asset_type = r.asset_type
              AND COALESCE(e.dataset_id, e.model_id, e.workspace_id, e.biospecimen_id) = r.row_id)
      ORDER BY r.asset_type, r.row_id
      LIMIT :limit
      """)
  List<FindingRow> findRelinkable(@Bind("limit") int limit);

  /**
   * Dataset aliases and dataset entry numbers the alias sequence would issue again, which an ad hoc
   * insert leaves behind.
   */
  @SqlQuery(
      """
      WITH next_alias AS (
          SELECT CASE WHEN is_called THEN last_value + 1 ELSE last_value END AS n
          FROM dataset_alias_seq
      ),
      ahead AS (
          SELECT d.alias::integer AS asset_number, NULL::integer AS catalog_id,
              d.dataset_id AS row_id
          FROM dataset d, next_alias s
          WHERE d.alias >= s.n
          UNION ALL
          SELECT c.asset_number, c.catalog_id, NULL::integer
          FROM asset_catalog c, next_alias s
          WHERE c.asset_type = 'DATASET' AND c.asset_number >= s.n
      )
      SELECT 'DATASET' AS asset_type, asset_number, catalog_id, row_id,
          count(*) OVER () AS total
      FROM ahead
      ORDER BY asset_number, catalog_id NULLS FIRST
      LIMIT :limit
      """)
  List<FindingRow> findSequenceBehind(@Bind("limit") int limit);

  record FindingRow(
      AssetType assetType, Integer assetNumber, Integer catalogId, Integer rowId, long total) {}
}
