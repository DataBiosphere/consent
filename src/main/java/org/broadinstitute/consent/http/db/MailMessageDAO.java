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

  int MAX_SEND_RECIPIENTS = 100;

  default List<MailSend> fetchSendsByCreateDate(
      Date start, Date end, Integer limit, Integer offset) {
    return fetchSendsByCreateDate(start, end, limit, offset, MAX_SEND_RECIPIENTS);
  }

  // A send is a recipient's nth email of one type and reference, each within 10 minutes of the
  // last.
  @SqlQuery(
      """
      WITH in_range AS (
        SELECT email_entity_id, entity_reference_id, user_id, email_type, create_date,
          COALESCE(entity_reference_id, 'email-' || email_entity_id) AS send_key,
          ROW_NUMBER() OVER (
            PARTITION BY email_type, COALESCE(entity_reference_id, 'email-' || email_entity_id),
              user_id
            ORDER BY create_date, email_entity_id
          ) AS occurrence
        FROM email_entity
        WHERE create_date >= LEAST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
          AND create_date < GREATEST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
      ),
      flagged AS (
        SELECT in_range.*,
          CASE WHEN create_date - LAG(create_date) OVER send_order <= INTERVAL '10 minutes'
            THEN 0 ELSE 1 END AS starts_send
        FROM in_range
        WINDOW send_order AS (
          PARTITION BY email_type, send_key, occurrence ORDER BY create_date, email_entity_id)
      ),
      numbered AS (
        SELECT flagged.*,
          SUM(starts_send) OVER (
            PARTITION BY email_type, send_key, occurrence ORDER BY create_date, email_entity_id
          ) AS send_number
        FROM flagged
      ),
      page AS (
        SELECT email_type, send_key, occurrence, send_number,
          MIN(entity_reference_id) AS entity_reference_id,
          MIN(email_entity_id) AS send_id, MIN(create_date) AS create_date,
          COUNT(*) AS recipient_count
        FROM numbered
        GROUP BY email_type, send_key, occurrence, send_number
        ORDER BY MIN(create_date) DESC, MIN(email_entity_id) DESC
        OFFSET :offset
        LIMIT :limit
      ),
      named AS (
        SELECT p.send_id, n.user_id, u.display_name,
          ROW_NUMBER() OVER (
            PARTITION BY p.send_id ORDER BY u.display_name, n.email_entity_id
          ) AS position
        FROM page p
        JOIN numbered n ON n.email_type = p.email_type
          AND n.send_key = p.send_key
          AND n.occurrence = p.occurrence
          AND n.send_number = p.send_number
        LEFT JOIN users u ON u.user_id = n.user_id
      )
      SELECT p.send_id, p.email_type, p.entity_reference_id, p.create_date, p.recipient_count,
        json_agg(
          json_build_object('userId', nm.user_id, 'displayName', nm.display_name)
          ORDER BY nm.position
        ) AS recipients
      FROM page p
      JOIN named nm ON nm.send_id = p.send_id AND nm.position <= :recipientLimit
      GROUP BY p.send_id, p.email_type, p.entity_reference_id, p.create_date, p.recipient_count
      ORDER BY p.create_date DESC, p.send_id DESC
      """)
  List<MailSend> fetchSendsByCreateDate(
      @Bind("start") Date start,
      @Bind("end") Date end,
      @Bind("limit") Integer limit,
      @Bind("offset") Integer offset,
      @Bind("recipientLimit") Integer recipientLimit);
}
