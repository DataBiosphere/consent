package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Acknowledgment;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AcknowledgmentMapper implements RowMapper<Acknowledgment> {
    @Override
    public Acknowledgment map(ResultSet rs, StatementContext ctx) throws SQLException {
        Acknowledgment ack = new Acknowledgment();
        ack.setAckKey(rs.getString("ack_key"));
        ack.setUserId(rs.getInt("user_id"));
        ack.setFirstAcknowledged(rs.getTimestamp("first_acknowledged"));
        ack.setLastAcknowledged(rs.getTimestamp("last_acknowledged"));
        return ack;
    }
}
