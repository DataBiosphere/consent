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

    @SqlUpdate("INSERT INTO email_entity " +
            "(vote_id, entity_reference_id, user_id, email_type, date_sent, email_text, create_date) VALUES " +
            "(:voteId, :entityReferenceId, :userId, :emailType, :dateSent, :emailText, :dateSent)")
    void insertEmail(@Bind("voteId") Integer voteId,
                     @Bind("entityReferenceId") String entityReferenceId,
                     @Bind("userId") Integer userId,
                     @Bind("emailType") Integer emailType,
                     @Bind("dateSent") Date dateSent,
                     @Bind("emailText") String emailText);

    @SqlBatch("INSERT INTO email_entity " +
            "(entity_reference_id, user_id, email_type, date_sent, email_text, create_date) VALUES " +
            "(:entityReferenceId, :userId, :emailType, :dateSent, :emailText, :dateSent)")
    void insertBulkEmailNoVotes(@Bind("userId") List<Integer> userIds,
                         @Bind("entityReferenceId") String entityReferenceId,
                         @Bind("emailType") Integer emailType,
                         @Bind("dateSent") Date dateSent,
                         @Bind("emailText") String emailText);

}
