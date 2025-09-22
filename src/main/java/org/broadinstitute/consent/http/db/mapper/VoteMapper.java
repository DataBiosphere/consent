package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class VoteMapper implements RowMapper<Vote> {

  public Vote map(ResultSet r, StatementContext ctx) throws SQLException {
    return new Vote(
        r.getInt("vote_id"),
        (r.getString("vote") == null) ? null : r.getBoolean("vote"),
        r.getInt("user_id"),
        r.getTimestamp("create_date"),
        r.getDate("update_date"),
        r.getInt("election_id"),
        r.getString("rationale"),
        r.getString("type"),
        (r.getString("reminder_sent") == null) ? null : r.getBoolean("reminder_sent"),
        (r.getString("has_concerns") == null) ? null : r.getBoolean("has_concerns"));
  }
}
