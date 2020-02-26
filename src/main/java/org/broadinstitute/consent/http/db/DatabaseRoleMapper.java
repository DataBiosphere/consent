package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Role;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DatabaseRoleMapper implements RowMapper<Role> {

    public Role map(ResultSet r, StatementContext ctx) throws SQLException {
        return new Role(r.getInt("roleId"), r.getString("name"));
    }
}
