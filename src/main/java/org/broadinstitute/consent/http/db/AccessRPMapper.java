package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.AccessRP;
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