package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Election;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseElectionMapper implements ResultSetMapper<Election> {

    @Override
    public Election map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        System.out.println("------------- DatabaseElectionMapper --------------");
        System.out.flush();
        return new Election(
                r.getInt("electionId"),
                r.getString("electionType"),
                r.getString("status"),
                r.getDate("createDate"),
                r.getString("referenceId"),
                (r.getDate("lastUpdate") == null) ? null : r.getDate("lastUpdate"),
                (r.getString("finalAccessVote") == null) ? null : r.getBoolean("finalAccessVote"),
                r.getInt("datasetId")
        );
    }
}
