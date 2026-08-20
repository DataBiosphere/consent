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
   * consent-group-prefixed one, matching {@code Dataset#getAccessManagement}.
   */
  @RegisterConstructorMapper(PersistedDataUseRow.class)
  @SqlQuery(
      """
      WITH access_management AS (
        SELECT dp.dataset_id,
               COALESCE(
                 MAX(LOWER(TRIM(dp.property_value)))
                   FILTER (WHERE dp.schema_property = 'accessManagement'),
                 MAX(LOWER(TRIM(dp.property_value)))
                   FILTER (WHERE dp.schema_property = 'consentGroup.accessManagement')
               ) AS access_management
        FROM dataset_property dp
        WHERE dp.schema_property IN ('accessManagement', 'consentGroup.accessManagement')
        GROUP BY dp.dataset_id
      ),
      dar_usage AS (
        SELECT dataset_id, COUNT(DISTINCT reference_id) AS dar_count
        FROM dar_dataset
        GROUP BY dataset_id
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

  /** Every DAR referencing a dataset, which is the unit matches are recomputed by. */
  @SqlQuery("SELECT DISTINCT reference_id FROM dar_dataset WHERE dataset_id = :datasetId")
  List<String> findDarReferenceIdsByDatasetId(@Bind("datasetId") Integer datasetId);
}
