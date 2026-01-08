package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.CacheDocument;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;
import java.util.Collection;

public interface CacheTableDAO extends Transactional<CacheTableDAO> {
  public static int BATCH_CHUNK_SIZE = 250;
  @SqlBatch(
      """
      INSERT into cache_table
      (key, jsondocument)
        (SELECT :key, :jsonValue::jsonb)
      """)
  @BatchChunkSize(BATCH_CHUNK_SIZE)
  int[] insert(@BindBean Collection<CacheDocument> documents);


  @SqlQuery(
      """
        SELECT key, jsondocument from cache_table
      """)
  ResultIterable<CacheDocument> streamDocuments();
}
