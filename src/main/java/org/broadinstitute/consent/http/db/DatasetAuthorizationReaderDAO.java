package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.DatasetAuthorizationReader;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterConstructorMapper(DatasetAuthorizationReader.class)
public interface DatasetAuthorizationReaderDAO
    extends Transactional<DatasetAuthorizationReaderDAO> {
  @SqlQuery(
"""
SELECT * from dataset_authorized_readers WHERE dataset_id = :datasetId
""")
  List<DatasetAuthorizationReader> findAuthorizedReadersByDatasetId(
      @Bind("datasetId") long datasetId);

  @SqlQuery(
"""
SELECT * from dataset_authorized_readers WHERE dataset_id = :datasetId AND user_id = :userId
""")
  DatasetAuthorizationReader findAuthorizedReadersByDatasetIdAndUserId(
      @Bind("datasetId") long datasetId, @Bind("userId") long userId);

  @SqlQuery(
"""
SELECT * from dataset_authorized_readers WHERE id = :recordId
""")
  DatasetAuthorizationReader findAuthorizedReaderByRecordId(@Bind("recordId") long recordId);

  @SqlUpdate(
"""
INSERT INTO dataset_authorized_readers (dataset_id, user_id, created_by) VALUES (:datasetId, :userId, :operatorId)
""")
  @GetGeneratedKeys
  long addAuthorizedReaderToDataset(
      @Bind("datasetId") int datasetId,
      @Bind("userId") int userId,
      @Bind("operatorId") int operatorId);

  @SqlUpdate(
"""
DELETE FROM dataset_authorized_readers WHERE dataset_id = :datasetId
""")
  void deleteByDatasetId(@Bind("datasetId") int datasetId);

  @SqlUpdate(
"""
DELETE FROM dataset_authorized_readers WHERE user_id = :userId
""")
  void deleteByUserId(@Bind("userId") int userId);

  @SqlUpdate(
"""
DELETE FROM dataset_authorized_readers WHERE dataset_id =:datasetId AND user_id = :userId
""")
  void deleteByDatasetAndUserId(@Bind("datasetId") long datasetId, @Bind("userId") long userId);
}
