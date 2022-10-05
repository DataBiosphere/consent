package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MailMessageMapper implements RowMapper<MailMessage> {

  public MailMessage map(ResultSet r, StatementContext ctx) throws SQLException {

    return new MailMessage(
        r.getInt("email_entity_id"),
        r.getInt("vote_id"),
        r.getInt("election_id"),
        r.getInt("user_id"),
        r.getString("email_type"),
        r.getDate("date_sent"),
        r.getString("email_text"),
        r.getString("sendgrid_response"),
        r.getInt("sendgrid_status"),
        r.getDate("create_date"));
  }
}
