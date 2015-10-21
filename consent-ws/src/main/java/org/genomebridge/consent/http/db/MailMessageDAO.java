package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.mail.MailMessage;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.Date;

@RegisterMapper(MailMessageMapper.class)
public interface MailMessageDAO extends Transactional<MailMessageDAO> {

    @SqlQuery("select e.*, et.type from emailEntity inner join emailType et ON e.emailType = et.emailType e where emailEntityId = :emailEntityId")
    MailMessage findEmailById(@Bind("emailEntityId") String consentId);

    @SqlQuery("Select emailEntityId FROM emailentity e WHERE (e.electionId = :darElectionId or  e.electionId = :rpElectionId) AND e.emailType = 1 LIMIT 1")
    Integer existsCollectDAREmail(@Bind("darElectionId") Integer darElectionId, @Bind("rpElectionId") Integer rpElectionId);

    @SqlUpdate("insert into emailEntity " +
            "(voteId, electionId, dacUserId, emailType, dateSent, emailText) values " +
            "(:voteId, :electionId, :dacUserId, :emailType, :dateSent, :emailText)")
    void insertEmail(@Bind("voteId") Integer voteId,
                       @Bind("electionId") Integer electionId,
                       @Bind("dacUserId") Integer dacUserId,
                       @Bind("emailType") Integer emailType,
                       @Bind("dateSent") Date dateSent,
                       @Bind("emailText") String emailText);

}
