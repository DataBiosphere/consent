package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DACUserRoleMapper implements ResultSetMapper<DACUser> {

    private Map<Integer, DACUser> users = new HashMap<>();
    public DACUser map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        DACUser user;
        if (index == 0 || !users.containsKey(r.getInt("dacUserId"))) {
            user = new DACUser(r.getInt("dacUserId"),
                    r.getString("email"),
                    r.getString("displayName"),
                    null,
                    r.getDate("createDate"), new ArrayList<>());
            String status = getStatus(r);
            DACUserRole role = new DACUserRole(r.getInt("roleId"), r.getString("name"), null, null, status);
            user.getRoles().add(role);
            users.put(user.getDacUserId(), user);
        } else {
            user = users.get(r.getInt("dacUserId"));
            String status = getStatus(r);
            DACUserRole role = new DACUserRole(r.getInt("roleId"), r.getString("name"), null, null, status);
            user.getRoles().add(role);
        }
        return user;
    }

    private String getStatus(ResultSet r) {
        try{
            return  RoleStatus.getStatusByValue(r.getInt("status"));
        }catch(Exception e){
            return null;
        }
    }


}
