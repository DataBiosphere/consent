package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleMapper implements ResultSetMapper<DACUserRole> {

    public DACUserRole map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new DACUserRole(r.getInt("roleId"), r.getString("name"),r.getBoolean("email_preference"), r.getString("rationale"),  RoleStatus.getStatusByValue(r.getInt("status")));
    }
}
