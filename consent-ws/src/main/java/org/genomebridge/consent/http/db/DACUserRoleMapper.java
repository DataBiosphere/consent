package org.genomebridge.consent.http.db;


import org.genomebridge.consent.http.models.DACUser;
import org.genomebridge.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DACUserRoleMapper implements ResultSetMapper<DACUser> {

    public DACUser map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        DACUser user = new DACUser(
                r.getInt("dacUserId"),
                r.getString("email"),
                r.getString("displayName"),
                r.getDate("createDate"));
            List<DACUserRole> roles = new ArrayList<>();
            roles.add(new DACUserRole(r.getInt("roleId"), r.getString("name")));
            user.setRoles(roles);
        return user;
    }

}
