package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Role;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleMapper implements RowMapper<Role> {

    @Override
    public Role map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new Role(
                resultSet.getInt("roleId"),
                resultSet.getString("name")
        );
    }

}
