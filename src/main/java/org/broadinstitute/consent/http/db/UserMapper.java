package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements RowMapper<User> {

    public User map(ResultSet r, StatementContext ctx) throws SQLException {
        User user = new User();
        user.setDacUserId(r.getInt("dacUserId"));
        user.setEmail(r.getString("email"));
        user.setEmailPreference(r.getBoolean("email_preference"));
        user.setDisplayName(r.getString("displayName"));
        user.setCreateDate(r.getDate("createDate"));
        user.setAdditionalEmail(r.getString("additional_email"));
        user.setStatus(getStatus(r));
        user.setRationale(r.getString("rationale"));
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
