package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MatchMapper implements RowMapper<Match> {

  public Match map(ResultSet r, StatementContext ctx) throws SQLException {

    return new Match(
        r.getInt("match_id"),
        r.getString("consent"),
        r.getString("purpose"),
        r.getBoolean("match_entity"),
        r.getBoolean("abstain"),
        r.getBoolean("failed"),
        r.getDate("create_date"),
        r.getString("algorithm_version"));
  }
}
