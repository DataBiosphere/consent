package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.broadinstitute.consent.http.models.mail.MailSend;
import org.broadinstitute.consent.http.models.mail.MailSendRecipient;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class MailSendMapper implements RowMapper<MailSend> {

  private static final Type RECIPIENTS_TYPE = new TypeToken<List<MailSendRecipient>>() {}.getType();

  @Override
  public MailSend map(ResultSet r, StatementContext ctx) throws SQLException {
    List<MailSendRecipient> recipients =
        GsonUtil.getInstance().fromJson(r.getString("recipients"), RECIPIENTS_TYPE);
    return new MailSend(
        r.getInt("send_id"),
        r.getInt("email_type"),
        r.getString("entity_reference_id"),
        r.getTimestamp("create_date"),
        r.getTimestamp("last_create_date"),
        r.getInt("recipient_count"),
        recipients);
  }
}
