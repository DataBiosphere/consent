package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.MailMessageMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import javax.annotation.Nullable;
import java.util.Date;

@RegisterRowMapper(MailMessageMapper.class)
public interface MailMessageDAO extends Transactional<MailMessageDAO> {

    @SqlQuery("SELECT email_entity_id FROM email_entity e " +
            "WHERE (e.entity_reference_id = :darReferenceId OR e.entity_reference_id = :rpReferenceId) " +
            "AND e.email_type = 1 LIMIT 1")
    Integer existsCollectDAREmail(@Bind("darReferenceId") String darReferenceId, @Bind("rpReferenceId") String rpReferenceId);

    @SqlUpdate("INSERT INTO email_entity " +
            "(entity_reference_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date) VALUES " +
            "(:entityReferenceId, :voteId, :userId, :emailType, :dateSent, :emailText, :sendGridResponse, :sendGridStatus, :createDate)")
    @GetGeneratedKeys
    Integer insert(@Nullable @Bind("entityReferenceId") String entityReferenceId,
                @Nullable @Bind("voteId") Integer voteId,
                @Bind("userId") Integer userId,
                @Bind("emailType") Integer emailType,
                @Nullable @Bind("dateSent") Date dateSent,
                @Bind("emailText") String emailText,
                @Nullable @Bind("sendGridResponse") String sendGridResponse,
                @Nullable @Bind("sendGridStatus") Integer sendGridStatus,
                @Bind("createDate") Date createDate);
}
