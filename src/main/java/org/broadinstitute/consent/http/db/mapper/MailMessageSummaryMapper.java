package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.mail.MailMessageSummary;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MailMessageSummaryMapper implements RowMapper<MailMessageSummary> {

  public MailMessageSummary map(ResultSet r, StatementContext ctx) throws SQLException {
    return new MailMessageSummary(
        r.getString("entity_reference_id"),
        r.getInt("email_entity_id"),
        nullableInt(r, "vote_id"),
        nullableInt(r, "user_id"),
        r.getInt("email_type"),
        r.getTimestamp("date_sent"),
        nullableInt(r, "sendgrid_status"),
        r.getTimestamp("create_date"));
  }

  // getInt reads SQL NULL as 0, which would pass for a real vote id or SendGrid status.
  private static Integer nullableInt(ResultSet r, String column) throws SQLException {
    int value = r.getInt(column);
    return r.wasNull() ? null : value;
  }
}
