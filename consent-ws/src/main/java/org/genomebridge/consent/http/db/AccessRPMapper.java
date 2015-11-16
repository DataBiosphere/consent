package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.AccessRP;
import org.genomebridge.consent.http.models.Match;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccessRPMapper implements ResultSetMapper<AccessRP> {

    public AccessRP map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new AccessRP(
                r.getInt("id"),
                r.getInt("electionAccessId"),
                r.getInt("electionRPId")
                );
           }
}