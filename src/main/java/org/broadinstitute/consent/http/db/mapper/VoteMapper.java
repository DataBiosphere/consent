package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class VoteMapper implements RowMapper<Vote> {

  public Vote map(ResultSet r, StatementContext ctx) throws SQLException {
    return new Vote(
        r.getInt("voteId"),
        (r.getString("vote") == null) ? null : r.getBoolean("vote"),
        r.getInt("dacUserId"),
        r.getTimestamp("createDate"),
        r.getDate("updateDate"),
        r.getInt("electionId"),
        r.getString("rationale"),
        r.getString("type"),
        (r.getString("reminderSent") == null) ? null : r.getBoolean("reminderSent"),
        (r.getString("has_concerns") == null) ? null : r.getBoolean("has_concerns"));
  }
}
