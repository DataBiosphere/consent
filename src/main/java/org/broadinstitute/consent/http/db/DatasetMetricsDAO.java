package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.CreatedBucket;
import org.broadinstitute.consent.http.models.DatasetBucket;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/** Admin reporting over datasets and studies created in [:from, :to). */
public interface DatasetMetricsDAO {

  /**
   * Datasets created per bucket, and how many of those the DAC has approved now; a bucket with none
   * is left out.
   */
  @RegisterConstructorMapper(DatasetBucket.class)
  @SqlQuery(
      """
      SELECT date_trunc(:bucket, create_date) AS bucket_start,
             COUNT(*) AS count,
             COUNT(*) FILTER (WHERE dac_approval) AS dac_approved
      FROM dataset
      WHERE create_date >= :from AND create_date < :to
      GROUP BY 1
      ORDER BY 1
      """)
  List<DatasetBucket> countDatasetsCreated(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  /** Studies created per bucket; a bucket with none is left out. */
  @RegisterConstructorMapper(CreatedBucket.class)
  @SqlQuery(
      """
      SELECT date_trunc(:bucket, create_date) AS bucket_start, COUNT(*) AS count
      FROM study
      WHERE create_date >= :from AND create_date < :to
      GROUP BY 1
      ORDER BY 1
      """)
  List<CreatedBucket> countStudiesCreated(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);
}
