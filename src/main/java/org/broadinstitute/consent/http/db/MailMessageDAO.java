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

  // Rows sharing a send_id, type and reference are one send; older rows split on 10-minute gaps.
  @SqlQuery(
      """
      WITH in_range AS (
        SELECT email_entity_id, entity_reference_id, user_id, email_type, create_date, date_sent,
          send_id,
          CASE WHEN entity_reference_id IS NULL AND send_id IS NULL THEN email_entity_id END
            AS lone_id
        FROM email_entity
        WHERE create_date >= LEAST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
          AND create_date < GREATEST(CAST(:start AS timestamptz), CAST(:end AS timestamptz))
      ),
      flagged AS (
        SELECT in_range.*,
          CASE
            WHEN LAG(create_date) OVER send_order IS NULL THEN 1
            WHEN send_id IS NOT NULL THEN 0
            WHEN create_date - LAG(create_date) OVER send_order <= INTERVAL '10 minutes' THEN 0
            ELSE 1 END AS starts_send
        FROM in_range
        WINDOW send_order AS (
          PARTITION BY email_type, entity_reference_id, lone_id, send_id
          ORDER BY create_date, email_entity_id)
      ),
      numbered AS (
        SELECT flagged.*,
          SUM(starts_send) OVER (
            PARTITION BY email_type, entity_reference_id, lone_id, send_id
            ORDER BY create_date, email_entity_id
          ) AS send_number
        FROM flagged
      ),
      grouped AS (
        SELECT numbered.*,
          MIN(email_entity_id) OVER (
            PARTITION BY email_type, entity_reference_id, lone_id, send_id, send_number
          ) AS send_row_id
        FROM numbered
      ),
      page AS (
        SELECT send_row_id AS send_id, email_type, entity_reference_id,
          MIN(create_date) AS create_date, COUNT(DISTINCT user_id) AS recipient_count
        FROM grouped
        GROUP BY send_row_id, email_type, entity_reference_id
        ORDER BY MIN(create_date) DESC, send_row_id DESC
        OFFSET :offset
        LIMIT :limit
      ),
      named AS (
        SELECT p.send_id, n.user_id, u.display_name, bool_or(n.date_sent IS NOT NULL) AS delivered,
          ROW_NUMBER() OVER (
            PARTITION BY p.send_id ORDER BY u.display_name, MIN(n.email_entity_id)
          ) AS position
        FROM page p
        JOIN grouped n ON n.send_row_id = p.send_id
        LEFT JOIN users u ON u.user_id = n.user_id
        GROUP BY p.send_id, n.user_id, u.display_name
      )
      SELECT p.send_id, p.email_type, p.entity_reference_id, p.create_date, p.recipient_count,
        COALESCE(
          json_agg(
            json_build_object(
              'userId', nm.user_id, 'displayName', nm.display_name, 'delivered', nm.delivered)
            ORDER BY nm.position
          ) FILTER (WHERE nm.send_id IS NOT NULL),
          '[]'
        ) AS recipients
      FROM page p
      LEFT JOIN named nm ON nm.send_id = p.send_id AND nm.position <= :recipientLimit
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
