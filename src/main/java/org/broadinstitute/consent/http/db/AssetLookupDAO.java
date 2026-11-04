package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/** The rows a DUOS identifier can name, read for the identifier lookup. */
@RegisterConstructorMapper(AssetLookupDAO.DatasetRow.class)
@RegisterConstructorMapper(AssetLookupDAO.AssetRow.class)
public interface AssetLookupDAO {

  @SqlQuery(
      """
      SELECT dataset_id, study_id, alias::integer AS alias, indexed_date
      FROM dataset
      WHERE alias = :alias
      """)
  DatasetRow findDatasetByAlias(@Bind("alias") int alias);

  @SqlQuery(
      """
      SELECT dataset_id, study_id, alias::integer AS alias, indexed_date
      FROM dataset
      WHERE dataset_id = :datasetId
      """)
  DatasetRow findDatasetById(@Bind("datasetId") int datasetId);

  @SqlQuery(
      """
      SELECT model_id AS row_id, study_id, retired_date
      FROM study_model WHERE :type = 'MODEL' AND model_id = :rowId
      UNION ALL
      SELECT workspace_id, study_id, retired_date
      FROM study_workspace WHERE :type = 'WORKSPACE' AND workspace_id = :rowId
      UNION ALL
      SELECT biospecimen_id, study_id, retired_date
      FROM study_biospecimen WHERE :type = 'BIOSPECIMEN' AND biospecimen_id = :rowId
      """)
  AssetRow findAssetRow(@Bind("type") AssetType type, @Bind("rowId") int rowId);

  /** Whether the study has a dataset in the search index, which puts its assets in the library. */
  @SqlQuery(
      """
      SELECT EXISTS (
          SELECT 1 FROM dataset WHERE study_id = :studyId AND indexed_date IS NOT NULL)
      """)
  boolean hasIndexedDataset(@Bind("studyId") int studyId);

  record DatasetRow(Integer datasetId, Integer studyId, Integer alias, Timestamp indexedDate) {}

  record AssetRow(Integer rowId, Integer studyId, Timestamp retiredDate) {}
}
