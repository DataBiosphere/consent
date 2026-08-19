package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/** {@link VoteMapper} plus the voting user's name, for queries that join users. */
public class VoteWithDisplayNameMapper implements RowMapper<Vote> {

  private final VoteMapper voteMapper = new VoteMapper();

  @Override
  public Vote map(ResultSet r, StatementContext ctx) throws SQLException {
    Vote vote = voteMapper.map(r, ctx);
    vote.setDisplayName(r.getString("display_name"));
    return vote;
  }
}
