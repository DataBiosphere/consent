package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DACUser;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DACUserMapper implements ResultSetMapper<DACUser> {

    public DACUser map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new DACUser(
                r.getInt("dacUserId"),
                r.getString("email"),
                r.getString("displayName"),
                r.getDate("createDate"));
    }
}