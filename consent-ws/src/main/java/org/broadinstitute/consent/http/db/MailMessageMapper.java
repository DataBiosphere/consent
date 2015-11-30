package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MailMessageMapper implements ResultSetMapper<MailMessage> {

    public MailMessage map(int index, ResultSet r, StatementContext ctx) throws SQLException {

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