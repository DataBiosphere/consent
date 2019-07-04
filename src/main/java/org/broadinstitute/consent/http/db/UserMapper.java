package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.User;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserMapper implements ResultSetMapper<User> {

    public User map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new User(
                r.getInt("dacUserId"),
                r.getString("email"),
                r.getString("displayName"),
                r.getDate("createDate"),
                r.getString("additional_email"));
    }
}