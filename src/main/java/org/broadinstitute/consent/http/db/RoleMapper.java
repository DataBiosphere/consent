package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Role;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleMapper implements ResultSetMapper<Role> {

    @Override
    public Role map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new Role(
                resultSet.getInt("roleId"),
                resultSet.getString("name")
        );
    }

}
