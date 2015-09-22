package org.genomebridge.consent.http.db;


import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.ElectionReview;
import org.genomebridge.consent.http.models.ElectionReviewVote;
import org.genomebridge.consent.http.models.Vote;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


public class ElectionReviewVoteMapper implements ResultSetMapper<ElectionReviewVote>{

    public ElectionReviewVote map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        Vote vote = new Vote(
                r.getInt("voteId"),
                (r.getString("vote") == null) ? null : r.getBoolean("vote"),
                r.getInt("dacUserId"),
                r.getDate("createDate"),
                r.getDate("updateDate"),
                r.getInt("electionId"),
                r.getString("rationale"),
                (r.getString("isFinalAccessVote") == null ? null : r.getBoolean("isFinalAccessVote")),
                (r.getString("reminderSent") == null) ? null : r.getBoolean("reminderSent")
        );

        return new ElectionReviewVote(
                vote,
                r.getString("displayName"),
                r.getString("email"));
    }
}