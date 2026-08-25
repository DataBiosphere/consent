package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseRow;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * Read-only access to persisted Data Use shapes, for reconciling the legacy-record population.
 *
 * <p>Deliberately does not join {@code match_entity}: it still keys datasets by the {@code
 * DUOS-######} alias, and the scoping audit recorded that the alias-derived join must not reach
 * application code. Matches are reconciled per DAR by {@code reference_id} instead.
 */
public interface PersistedDataUseDAO {

  /**
   * Raw {@code data_use} text rather than a parsed value, so a null, an empty string, and malformed
   * JSON stay distinguishable. The canonical access-management property wins over the legacy
   * consent-group-prefixed one, but only where its value parses as an {@code AccessManagement}, so
   * an unusable one falls back as {@code Dataset#getAccessManagement} does.
   *
   * <p>The property key is matched case-insensitively, as {@code Dataset#getAccessManagement} does;
   * an exact match would hide a differently-cased row the running application still resolves.
   *
   * <p>Reads every dataset because classification parses the JSON in Java, which SQL cannot do.
   * Four projected columns, read twice per admin-invoked run to reconcile it; revisit if the
   * dataset count grows by an order of magnitude.
   */
  @RegisterConstructorMapper(PersistedDataUseRow.class)
  @SqlQuery(
      """
      WITH access_management AS (
        SELECT dp.dataset_id,
               COALESCE(
                 MAX(LOWER(TRIM(dp.property_value)))
                   FILTER (WHERE LOWER(dp.schema_property) = 'accessmanagement'),
                 MAX(LOWER(TRIM(dp.property_value)))
                   FILTER (WHERE LOWER(dp.schema_property) = 'consentgroup.accessmanagement')
               ) AS access_management
        FROM dataset_property dp
        WHERE LOWER(dp.schema_property) IN ('accessmanagement', 'consentgroup.accessmanagement')
          AND LOWER(TRIM(dp.property_value)) IN ('open', 'controlled', 'external')
        GROUP BY dp.dataset_id
      ),
      dar_usage AS (
        SELECT dd.dataset_id, COUNT(DISTINCT dd.reference_id) AS dar_count
        FROM dar_dataset dd
        JOIN data_access_request dar ON dar.reference_id = dd.reference_id
        WHERE dar.submission_date IS NOT NULL
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
        GROUP BY dd.dataset_id
      )
      SELECT d.dataset_id,
             d.data_use,
             am.access_management,
             COALESCE(du.dar_count, 0) AS dar_count
      FROM dataset d
      LEFT JOIN access_management am ON am.dataset_id = d.dataset_id
      LEFT JOIN dar_usage du ON du.dataset_id = d.dataset_id
      ORDER BY d.dataset_id
      """)
  List<PersistedDataUseRow> findAllPersistedDataUse();

  /**
   * The submitted, unarchived DARs referencing a dataset, the unit matches are recomputed by. An
   * archived DAR is excluded because {@code reprocessMatchesForPurpose} would delete its historical
   * match rows and find no DAR to rebuild them from.
   */
  @SqlQuery(
      """
      SELECT DISTINCT dd.reference_id
      FROM dar_dataset dd
      JOIN data_access_request dar ON dar.reference_id = dd.reference_id
      WHERE dd.dataset_id = :datasetId
        AND dar.submission_date IS NOT NULL
        AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
      """)
  List<String> findDarReferenceIdsByDatasetId(@Bind("datasetId") Integer datasetId);
}
