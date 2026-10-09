package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.MailMessageMapper;
import org.broadinstitute.consent.http.db.mapper.MailMessageSummaryMapper;
import org.broadinstitute.consent.http.db.mapper.MailSendMapper;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.broadinstitute.consent.http.models.mail.MailMessageSummary;
import org.broadinstitute.consent.http.models.mail.MailSend;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(MailMessageMapper.class)
@RegisterRowMapper(MailMessageSummaryMapper.class)
@RegisterRowMapper(MailSendMapper.class)
public interface MailMessageDAO extends Transactional<MailMessageDAO> {

  @SqlQuery(
      """
      WITH insterted_row AS (
        INSERT INTO email_entity
          (entity_reference_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date, send_id)
        VALUES
          (:entityReferenceId, :voteId, :userId, :emailType, :dateSent, :emailText, :sendgridResponse, :sendgridStatus, NOW(), CAST(:sendId AS uuid))
        RETURNING *)
      SELECT * FROM insterted_row
      """)
  MailMessage insert(@BindMethods MailMessageInsert mail);

  @SqlQuery(
      """
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date
      FROM email_entity e
      WHERE email_type = :emailType
      ORDER BY create_date DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailMessage> fetchMessagesByType(
      @Bind("emailType") Integer emailType,
      @Bind("limit") Integer limit,
      @Bind("offset") Integer offset);

  @SqlQuery(
      """
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date
      FROM email_entity e
      WHERE user_id = :userId
      ORDER BY create_date DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailMessage> fetchMessagesByUserId(
      @Bind("userId") Integer userId, @Bind("limit") Integer limit, @Bind("offset") Integer offset);

  @SqlQuery(
      """
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date
      FROM email_entity e
      WHERE create_date BETWEEN SYMMETRIC :start AND :end
      ORDER BY create_date DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailMessage> fetchMessagesByCreateDate(
      @Bind("start") Date start,
      @Bind("end") Date end,
      @Bind("limit") Integer limit,
      @Bind("offset") Integer offset);

  @SqlQuery(
      """
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, email_text, sendgrid_response, sendgrid_status, create_date
      FROM email_entity e
      WHERE email_entity_id = :emailId
      """)
  MailMessage fetchMessageById(@Bind("emailId") Integer emailId);

  // The id breaks create_date ties, so offset pages neither skip nor repeat a row.
  @SqlQuery(
      """
      SELECT entity_reference_id, email_entity_id, vote_id, user_id, email_type, date_sent, sendgrid_status, create_date
      FROM email_entity e
      WHERE create_date >= LEAST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
        AND create_date < GREATEST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
      ORDER BY create_date DESC, email_entity_id DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailMessageSummary> fetchMessageSummariesByCreateDate(
      @Bind("start") Date start,
      @Bind("end") Date end,
      @Bind("limit") Integer limit,
      @Bind("offset") Integer offset);

  // Recipients of one send get their own rows, created one after another. A row more than 10
  // minutes after the previous row of its type and entity reference starts a new send.
  @SqlQuery(
      """
      WITH in_range AS (
        SELECT email_entity_id, entity_reference_id, user_id, email_type, create_date,
          CASE WHEN create_date - LAG(create_date) OVER send_order <= INTERVAL '10 minutes'
            THEN 0 ELSE 1 END AS starts_send
        FROM email_entity
        WHERE create_date >= LEAST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
          AND create_date < GREATEST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
        WINDOW send_order AS (
          PARTITION BY email_type, entity_reference_id ORDER BY create_date, email_entity_id)
      ),
      numbered AS (
        SELECT in_range.*,
          SUM(starts_send) OVER (
            PARTITION BY email_type, entity_reference_id ORDER BY create_date, email_entity_id
          ) AS send_number
        FROM in_range
      )
      SELECT MIN(n.email_entity_id) AS send_id, n.email_type, n.entity_reference_id,
        MIN(n.create_date) AS create_date, COUNT(*) AS recipient_count,
        json_agg(
          json_build_object('userId', n.user_id, 'displayName', u.display_name)
          ORDER BY u.display_name, n.email_entity_id
        ) AS recipients
      FROM numbered n
      LEFT JOIN users u ON u.user_id = n.user_id
      GROUP BY n.email_type, n.entity_reference_id, n.send_number
      ORDER BY MIN(n.create_date) DESC, MIN(n.email_entity_id) DESC
      OFFSET :offset
      LIMIT :limit
      """)
  List<MailSend> fetchSendsByCreateDate(
      @Bind("start") Date start,
      @Bind("end") Date end,
      @Bind("limit") Integer limit,
      @Bind("offset") Integer offset);
}
