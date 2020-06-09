package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DACUserRoleMapper implements RowMapper<User>, RowMapperHelper {

    private Map<Integer, User> users = new HashMap<>();

    public User map(ResultSet r, StatementContext ctx) throws SQLException {
        User user;
        if (!users.containsKey(r.getInt("dacUserId"))) {
            user = new User();
            user.setDacUserId(r.getInt("dacUserId"));
            user.setEmail(r.getString("email"));
            user.setDisplayName(r.getString("displayName"));
            user.setCreateDate(r.getDate("createDate"));
            user.setAdditionalEmail(r.getString("additional_email"));
            user.setEmailPreference(r.getBoolean("email_preference"));
            user.setStatus(getStatus(r));
            user.setRationale(r.getString("rationale"));
            user.setRoles(new ArrayList<>());
            addRole(r, user);
            if (hasColumn(r, "completed")) {
                user.setProfileCompleted(Boolean.valueOf(r.getString("completed")));
            }
        } else {
            user = users.get(r.getInt("dacUserId"));
            addRole(r, user);
        }
        users.put(user.getDacUserId(), user);
        return user;
    }

    private void addRole(ResultSet r, User user) throws SQLException {
        if (r.getObject("user_role_id") != null &&
                r.getObject("user_id") != null &&
                r.getObject("roleId") != null) {
            Integer dacId = (r.getObject("dac_id") == null) ? null : r.getInt("dac_id");
            UserRole role = new UserRole(r.getInt("user_role_id"), r.getInt("user_id"), r.getInt("roleId"), r.getString("name"), dacId);
            user.getRoles().add(role);
        }
    }

    private String getStatus(ResultSet r) {
        try {
            return RoleStatus.getStatusByValue(r.getInt("status"));
        } catch (Exception e) {
            return null;
        }
    }

}
