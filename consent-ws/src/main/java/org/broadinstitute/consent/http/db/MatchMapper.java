package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Match;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MatchMapper implements ResultSetMapper<Match> {

    public Match map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new Match(
                r.getInt("matchId"),
                r.getString("consent"),
                r.getString("purpose"),
                r.getBoolean("matchEntity"),
                r.getBoolean("failed"),
                r.getDate("createDate")
        );


    }

}
