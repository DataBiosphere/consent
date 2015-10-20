package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Election;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ElectionMapper implements ResultSetMapper<Election> {

    public Election map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new Election(
                r.getInt("electionId"),
                r.getString("electionType"),
                (r.getString("finalVote") == null) ? null : r.getBoolean("finalVote"),
                r.getString("finalRationale"),
                r.getString("status"),
                r.getDate("createDate"),
                r.getDate("finalVoteDate"),
                r.getString("referenceId"),
                (r.getDate("lastUpdate") == null) ? null : r.getDate("lastUpdate"),
                (r.getString("finalAccessVote") == null) ? null : r.getBoolean("finalAccessVote")
                );
    }
}