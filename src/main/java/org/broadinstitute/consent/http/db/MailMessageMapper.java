package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MailMessageMapper implements RowMapper<MailMessage> {

    public MailMessage map(ResultSet r, StatementContext ctx) throws SQLException {

        return new MailMessage(
                r.getInt("emailEntityId"),
                r.getInt("voteId"),
                r.getInt("electionId"),
                r.getInt("dacUserId"),
                r.getString("type"),
                r.getDate("dateSent"),
                r.getString("emailText")
        );
    }
}