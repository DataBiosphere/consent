package org.broadinstitute.consent.http.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Role;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class DatabaseRoleMapper implements ResultSetMapper<Role> {

    public Role map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new Role(r.getInt("roleId"), r.getString("name"));
    }
}
