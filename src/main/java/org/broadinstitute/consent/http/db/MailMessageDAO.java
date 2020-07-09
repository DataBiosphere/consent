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

    @SqlQuery("SELECT emailEntityId FROM email_entity e WHERE (e.entityReferenceId = :darReferenceId or  e.entityReferenceId = :rpReferenceId) AND e.emailType = 1 LIMIT 1")
    Integer existsCollectDAREmail(@Bind("darReferenceId") String darReferenceId, @Bind("rpReferenceId") String rpReferenceId);

    @SqlUpdate("INSERT INTO email_entity " +
            "(voteId, entityReferenceId, dacUserId, emailType, dateSent, emailText) VALUES " +
            "(:voteId, :entityReferenceId, :dacUserId, :emailType, :dateSent, :emailText)")
    void insertEmail(@Bind("voteId") Integer voteId,
                     @Bind("entityReferenceId") String entityReferenceId,
                     @Bind("dacUserId") Integer dacUserId,
                     @Bind("emailType") Integer emailType,
                     @Bind("dateSent") Date dateSent,
                     @Bind("emailText") String emailText);

    @SqlBatch("INSERT INTO email_entity " +
            "(entityReferenceId, dacUserId, emailType, dateSent, emailText) VALUES " +
            "(:entityReferenceId, :dacUserId, :emailType, :dateSent, :emailText)")
    void insertBulkEmailNoVotes(@Bind("dacUserId") List<Integer> userIds,
                         @Bind("entityReferenceId") String entityReferenceId,
                         @Bind("emailType") Integer emailType,
                         @Bind("dateSent") Date dateSent,
                         @Bind("emailText") String emailText);

}
