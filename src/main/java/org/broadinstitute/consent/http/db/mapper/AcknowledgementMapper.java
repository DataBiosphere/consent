package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Acknowledgement;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class AcknowledgementMapper implements RowMapper<Acknowledgement> {

  @Override
  public Acknowledgement map(ResultSet rs, StatementContext ctx) throws SQLException {
    Acknowledgement ack = new Acknowledgement();
    ack.setAckKey(rs.getString("ack_key"));
    ack.setUserId(rs.getInt("user_id"));
    ack.setFirstAcknowledged(rs.getTimestamp("first_acknowledged"));
    ack.setLastAcknowledged(rs.getTimestamp("last_acknowledged"));
    return ack;
  }
}
