package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserToUserRoleMapper implements ResultSetMapper<User> {

    private Map<Integer, User> users = new HashMap<>();

    public User map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        User user;
        if (index == 0 || !users.containsKey(r.getInt("dacUserId"))) {
            user = new User(r.getInt("dacUserId"),
                    r.getString("email"),
                    r.getString("displayName"),
                    null,
                    r.getDate("createDate"), new ArrayList<>(),
                    r.getString("additional_email"));
            String status = getStatus(r);
            // See DUOS-393 - we should not be setting these things to null
            UserRole role = new UserRole(r.getInt("roleId"), r.getString("name"), null, null, status);
            user.getRoles().add(role);
            users.put(user.getUserId(), user);
        } else {
            user = users.get(r.getInt("dacUserId"));
            String status = getStatus(r);
            // See DUOS-393 - we should not be setting these things to null
            UserRole role = new UserRole(r.getInt("roleId"), r.getString("name"), null, null, status);
            user.getRoles().add(role);
        }
        return user;
    }

    private String getStatus(ResultSet r) {
        try {
            return RoleStatus.getStatusByValue(r.getInt("status"));
        } catch (Exception e) {
            return null;
        }
    }


}
