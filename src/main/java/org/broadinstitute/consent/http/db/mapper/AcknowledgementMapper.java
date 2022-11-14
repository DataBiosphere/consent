package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Acknowledgement;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AcknowledgementMapper implements RowMapper<Acknowledgement> {
    @Override
    public Acknowledgement map(ResultSet rs, StatementContext ctx) throws SQLException {
        Acknowledgement ack = new Acknowledgement();
        ack.setAck_key(rs.getString("ack_key"));
        ack.setUserId(rs.getInt("user_id"));
        ack.setFirst_acknowledged(rs.getTimestamp("first_acknowledged"));
        ack.setLast_acknowledged(rs.getTimestamp("last_acknowledged"));
        return ack;
    }
}
