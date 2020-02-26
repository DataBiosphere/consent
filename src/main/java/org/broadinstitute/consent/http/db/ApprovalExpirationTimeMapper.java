package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.ApprovalExpirationTime;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ApprovalExpirationTimeMapper implements RowMapper<ApprovalExpirationTime> {

    @Override
    public ApprovalExpirationTime map(ResultSet r, StatementContext statementContext) throws SQLException {
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
