package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.MailMessageMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(MailMessageMapper.class)
public interface MailMessageDAO extends Transactional<MailMessageDAO> {

    @SqlQuery("SELECT email_entity_id FROM email_entity e " +
            "WHERE (e.entity_reference_id = :darReferenceId OR e.entity_reference_id = :rpReferenceId) " +
            "AND e.email_type = 1 LIMIT 1")
    Integer existsCollectDAREmail(@Bind("darReferenceId") String darReferenceId, @Bind("rpReferenceId") String rpReferenceId);

    @SqlBatch("INSERT INTO email_entity " +
            "(entity_reference_id, vote_id, election_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date) VALUES " +
            "(:entityReferenceId, :voteId, :electionId, :userId, :emailType, :dateSent, :emailText, :sendGridResponse, :sendGridStatus, :createDate)")
    void insert(@Bind("entityReferenceId") String entityReferenceId,
                @Bind("voteId") Integer voteId,
                @Bind("electionId") Integer electionId,
                @Bind("userId") Integer userId,
                @Bind("emailType") Integer emailType,
                @Bind("dateSent") Date dateSent,
                @Bind("emailText") String emailText,
                @Bind("sendGridResponse") String sendGridResponse,
                @Bind("sendGridStatus") Integer sendGridStatus,
                @Bind("createDate") Date createDate);
}
