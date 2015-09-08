package org.genomebridge.consent.http.db;


import org.genomebridge.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleMapper implements ResultSetMapper<DACUserRole> {

    public DACUserRole map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new DACUserRole(r.getInt("roleId"), r.getString("name"));
    }

}
