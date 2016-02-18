package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ApprovalExpirationTimeMapper implements ResultSetMapper<ApprovalExpirationTime> {

    @Override
    public ApprovalExpirationTime map(int i, ResultSet r, StatementContext statementContext) throws SQLException {
        return new ApprovalExpirationTime(
                r.getInt("id"),
                r.getInt("user_id"),
                r.getTimestamp("create_date"),
                r.getTimestamp("update_date"),
                r.getInt("amount_of_days"),
                r.getString("displayName")
        );

    }

}
