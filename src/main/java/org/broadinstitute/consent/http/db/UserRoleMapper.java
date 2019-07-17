package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.UserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRoleMapper implements ResultSetMapper<UserRole> {
    public UserRole map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new UserRole(
                r.getInt("role_id"),
                r.getString("name"));
    }
}
