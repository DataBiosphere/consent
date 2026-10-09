package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.mail.MailMessageSummary;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MailMessageSummaryMapper implements RowMapper<MailMessageSummary>, RowMapperHelper {

  @Override
  public MailMessageSummary map(ResultSet r, StatementContext ctx) throws SQLException {
    return new MailMessageSummary(
        r.getString("entity_reference_id"),
        r.getInt("email_entity_id"),
        hasNonZeroColumn(r, "vote_id") ? r.getInt("vote_id") : null,
        hasNonZeroColumn(r, "user_id") ? r.getInt("user_id") : null,
        r.getInt("email_type"),
        r.getTimestamp("date_sent"),
        hasNonZeroColumn(r, "sendgrid_status") ? r.getInt("sendgrid_status") : null,
        r.getTimestamp("create_date"));
  }
}
