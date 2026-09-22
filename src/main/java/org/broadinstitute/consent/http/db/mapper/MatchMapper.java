package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MatchMapper implements RowMapper<Match> {

  public Match map(ResultSet r, StatementContext ctx) throws SQLException {

    return new Match(
        r.getInt("match_id"),
        datasetIdentifier(r),
        (r.getObject("dataset_id") == null) ? null : r.getInt("dataset_id"),
        r.getString("purpose"),
        r.getBoolean("match_entity"),
        r.getBoolean("abstain"),
        r.getBoolean("failed"),
        r.getDate("create_date"),
        r.getString("algorithm_version"));
  }

  /**
   * Every query reaching this mapper inner joins dataset, whose alias has been NOT NULL since the
   * 2026-08-10 changeset, so a null here is a schema regression. getInt would report it as
   * DUOS-000000, which is a valid identifier belonging to another dataset.
   */
  private static String datasetIdentifier(ResultSet r) throws SQLException {
    int alias = r.getInt("alias");
    if (r.wasNull()) {
      throw new SQLException("match row joined a dataset with no alias");
    }
    return Dataset.parseAliasToIdentifier(alias);
  }
}
