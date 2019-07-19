package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.UserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRoleMapper implements ResultSetMapper<UserRole> {
    public UserRole map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new UserRole(
                r.getInt("user_role_id"),
                r.getInt("role_id"),
                r.getString("name"),
                r.getInt("dac_id"));
    }
}
