package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.Date;
import java.util.List;

@RegisterMapper(MailMessageMapper.class)
public interface MailMessageDAO extends Transactional<MailMessageDAO> {

    @SqlQuery("Select emailEntityId FROM email_entity e WHERE (e.entityReferenceId = :darElectionId or  e.entityReferenceId = :rpElectionId) AND e.emailType = 1 LIMIT 1")
    Integer existsCollectDAREmail(@Bind("darElectionId") Integer darElectionId, @Bind("rpElectionId") Integer rpElectionId);

    @SqlUpdate("insert into email_entity " +
            "(voteId, entityReferenceId, dacUserId, emailType, dateSent, emailText) values " +
            "(:voteId, :entityReferenceId, :dacUserId, :emailType, :dateSent, :emailText)")
    void insertEmail(@Bind("voteId") Integer voteId,
                     @Bind("entityReferenceId") String entityReferenceId,
                     @Bind("dacUserId") Integer dacUserId,
                     @Bind("emailType") Integer emailType,
                     @Bind("dateSent") Date dateSent,
                     @Bind("emailText") String emailText);

    @SqlBatch("insert into email_entity " +
            "(entityReferenceId, dacUserId, emailType, dateSent, emailText) values " +
            "(:entityReferenceId, :dacUserId, :emailType, :dateSent, :emailText)")
    void insertBulkEmailNoVotes(@Bind("dacUserId") List<Integer> userIds,
                         @Bind("entityReferenceId") String entityReferenceId,
                         @Bind("emailType") Integer emailType,
                         @Bind("dateSent") Date dateSent,
                         @Bind("emailText") String emailText);

}
