package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.db.mapper.MailMessageMapper;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(MailMessageMapper.class)
public interface MailMessageDAO extends Transactional<MailMessageDAO> {

//  @SqlQuery("SELECT email_entity_id FROM email_entity e " +
//      "WHERE (e.entity_reference_id = :darReferenceId OR e.entity_reference_id = :rpReferenceId) " +
//      "AND e.email_type = 1 LIMIT 1")
//  Integer existsCollectDAREmail(@Bind("darReferenceId") String darReferenceId,
//      @Bind("rpReferenceId") String rpReferenceId);

  @SqlUpdate("INSERT INTO email_entity " +
      "(entity_reference_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date) VALUES "
      +
      "(:entityReferenceId, :voteId, :userId, :emailType, :dateSent, :emailText, :sendGridResponse, :sendGridStatus, :createDate)")
  @GetGeneratedKeys
  Integer insert(@Nullable @Bind("entityReferenceId") String entityReferenceId,
      @Nullable @Bind("voteId") Integer voteId,
      @Bind("userId") Integer userId,
      @Bind("emailType") Integer emailType,
      @Nullable @Bind("dateSent") Instant dateSent,
      @Bind("emailText") String emailText,
      @Nullable @Bind("sendGridResponse") String sendGridResponse,
      @Nullable @Bind("sendGridStatus") Integer sendGridStatus,
      @Bind("createDate") Instant createDate);

  @SqlQuery("""
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date FROM email_entity e
      WHERE email_type = :emailType
      ORDER BY create_date DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailMessage> fetchMessagesByType(@Bind("emailType") Integer emailType,
      @Bind("limit") Integer limit, @Bind("offset") Integer offset);

  @SqlQuery("""
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date FROM email_entity e
      WHERE create_date BETWEEN SYMMETRIC :start AND :end
      ORDER BY create_date DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailMessage> fetchMessagesByCreateDate(@Bind("start") Date start, @Bind("end") Date end,
      @Bind("limit") Integer limit, @Bind("offset") Integer offset);

  @SqlQuery("""
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date FROM email_entity e
      WHERE email_entity_id = :emailId
      """)
  MailMessage fetchMessageById(@Bind("emailId") Integer emailId);
}
