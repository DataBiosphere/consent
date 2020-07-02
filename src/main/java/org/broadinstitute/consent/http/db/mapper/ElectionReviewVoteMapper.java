package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.ElectionReviewVote;
import org.broadinstitute.consent.http.models.Vote;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class ElectionReviewVoteMapper implements RowMapper<ElectionReviewVote> {

  public ElectionReviewVote map(ResultSet r, StatementContext ctx) throws SQLException {

    Vote vote =
        new Vote(
            r.getInt("voteId"),
            (r.getString("vote") == null) ? null : r.getBoolean("vote"),
            r.getInt("dacUserId"),
            r.getDate("createDate"),
            r.getDate("updateDate"),
            r.getInt("electionId"),
            r.getString("rationale"),
            r.getString("type"),
            (r.getString("reminderSent") == null) ? null : r.getBoolean("reminderSent"),
            (r.getString("has_concerns") == null) ? null : r.getBoolean("has_concerns"));

    return new ElectionReviewVote(vote, r.getString("displayName"), r.getString("email"));
  }
}
