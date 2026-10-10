package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.MailMessageMapper;
import org.broadinstitute.consent.http.db.mapper.MailMessageSummaryMapper;
import org.broadinstitute.consent.http.db.mapper.MailSendMapper;
import org.broadinstitute.consent.http.models.mail.EmailTypeLists;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.broadinstitute.consent.http.models.mail.MailMessageSummary;
import org.broadinstitute.consent.http.models.mail.MailSend;
import org.broadinstitute.consent.http.models.mail.MailSendSearch;
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

  // Rows sharing a send_id, type and reference are one send; older rows split on 10-minute gaps.
  // :types.kinds and :types.roles hold, at each email type's number, what it stores and whether
  // it's an approval or about a progress report. A search pages after filtering, not before.
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
          MIN(create_date) AS create_date, MAX(create_date) AS last_create_date,
          COUNT(DISTINCT user_id) AS recipient_count
        FROM grouped
        GROUP BY send_row_id, email_type, entity_reference_id
        ORDER BY MIN(create_date) DESC, send_row_id DESC
        OFFSET CASE WHEN CAST(:search.pattern AS text) IS NULL THEN :offset ELSE 0 END
        LIMIT CASE WHEN CAST(:search.pattern AS text) IS NULL THEN :limit END
      ),
      named AS (
        SELECT p.send_id, n.user_id, u.display_name, bool_or(n.date_sent IS NOT NULL) AS sent,
          ROW_NUMBER() OVER (
            PARTITION BY p.send_id ORDER BY u.display_name, MIN(n.email_entity_id)
          ) AS position
        FROM page p
        JOIN grouped n ON n.send_row_id = p.send_id
        LEFT JOIN users u ON u.user_id = n.user_id
        GROUP BY p.send_id, n.user_id, u.display_name
      ),
      name_matches AS (
        SELECT DISTINCT g.send_row_id FROM grouped g
        JOIN users u ON u.user_id = g.user_id
        WHERE u.display_name ILIKE :search.pattern
      ),
      sends AS (
        SELECT p.send_id, p.email_type, p.entity_reference_id, p.create_date, p.last_create_date,
          p.recipient_count,
          COALESCE(
            json_agg(
              json_build_object(
                'userId', nm.user_id, 'displayName', nm.display_name, 'sent', nm.sent)
              ORDER BY nm.position
            ) FILTER (WHERE nm.send_id IS NOT NULL),
            '[]'
          ) AS recipients
        FROM page p
        LEFT JOIN named nm ON nm.send_id = p.send_id AND nm.position <= :recipientLimit
        GROUP BY p.send_id, p.email_type, p.entity_reference_id, p.create_date,
          p.last_create_date, p.recipient_count
      )
      SELECT s.*, COALESCE(by_code.dar_code, by_dar.dar_code) AS dar_code, aliases.dataset_aliases
      FROM sends s
      CROSS JOIN LATERAL (
        SELECT (CAST(:types.kinds AS text[]))[s.email_type] AS kind,
          COALESCE((CAST(:types.roles AS text[]))[s.email_type], '') AS role
      ) k
      LEFT JOIN dar_collection by_code
        ON k.kind IN ('DAR_CODE', 'DAR_REFERENCE_ID') AND by_code.dar_code = s.entity_reference_id
      LEFT JOIN election el
        ON k.kind = 'ELECTION_ID' AND el.election_id = CASE
          WHEN s.entity_reference_id ~ '^[0-9]{1,9}$'
          THEN CAST(s.entity_reference_id AS integer) END
      LEFT JOIN data_access_request dar
        ON dar.reference_id = CASE
          WHEN k.kind = 'DAR_REFERENCE_ID'
            OR (k.kind = 'DAR_CODE' AND s.entity_reference_id !~ '^DAR-')
          THEN s.entity_reference_id
          ELSE el.reference_id END
      LEFT JOIN dar_collection by_dar ON by_dar.collection_id = dar.collection_id
      LEFT JOIN LATERAL (
        SELECT MAX(pr.id) AS progress_report_id FROM data_access_request pr
        WHERE k.role LIKE 'PROGRESS_REPORT%'
          AND pr.collection_id = by_code.collection_id
          AND pr.parent_id IS NOT NULL
          AND pr.submission_date <= s.create_date
          AND pr.data ->> 'closeoutSupplement' IS NULL
      ) target ON TRUE
      LEFT JOIN LATERAL (
        SELECT array_agg(DISTINCT related.alias ORDER BY related.alias) AS dataset_aliases
        FROM (
          SELECT ds.alias, ds.dataset_id, el.reference_id
          FROM dataset ds WHERE ds.dataset_id = el.dataset_id
          UNION ALL
          SELECT ds.alias, ds.dataset_id, dd.reference_id
          FROM dar_dataset dd JOIN dataset ds ON ds.dataset_id = dd.dataset_id
          WHERE el.dataset_id IS NULL AND dd.reference_id = dar.reference_id
          UNION ALL
          SELECT ds.alias, ds.dataset_id, dd.reference_id
          FROM data_access_request d
          JOIN dar_dataset dd ON dd.reference_id = d.reference_id
          JOIN dataset ds ON ds.dataset_id = dd.dataset_id
          WHERE d.collection_id = by_code.collection_id
            AND (d.id = target.progress_report_id
              OR (target.progress_report_id IS NULL
                AND d.parent_id IS NULL
                AND d.submission_date IS NOT NULL))
          UNION ALL
          SELECT ds.alias, ds.dataset_id, NULL
          FROM dataset ds
          WHERE k.kind = 'DUOS_ID' AND ds.alias = CASE
            WHEN s.entity_reference_id ~ '^DUOS-[0-9]{1,18}$'
            THEN CAST(substr(s.entity_reference_id, 6) AS bigint) END
          UNION ALL
          SELECT ds.alias, ds.dataset_id, NULL
          FROM dataset ds
          WHERE ds.name = s.entity_reference_id
            AND (k.kind = 'DATASET_NAME'
              OR (k.kind = 'DUOS_ID' AND s.entity_reference_id !~ '^DUOS-'))
          UNION ALL
          SELECT ds.alias, ds.dataset_id, NULL
          FROM study st JOIN dataset ds ON ds.study_id = st.study_id
          WHERE k.kind = 'STUDY_UUID' AND st.uuid = CASE
            WHEN s.entity_reference_id
              ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            THEN CAST(s.entity_reference_id AS uuid) END
        ) related
        WHERE k.role NOT LIKE '%APPROVAL'
          OR related.reference_id IS NULL
          OR (
            SELECT v.vote FROM election e
            JOIN vote v ON v.election_id = e.election_id
            WHERE e.reference_id = related.reference_id
              AND (e.dataset_id = related.dataset_id OR e.dataset_id IS NULL)
              AND LOWER(e.election_type) = 'dataaccess'
              AND LOWER(v.type) IN ('final', 'radar_approve')
              AND v.vote IS NOT NULL
            ORDER BY COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
            LIMIT 1
          ) IS TRUE
      ) aliases ON TRUE
      WHERE CAST(:search.pattern AS text) IS NULL
        OR s.email_type = ANY(CAST(:search.types AS integer[]))
        OR COALESCE(by_code.dar_code, by_dar.dar_code) ILIKE :search.pattern
        OR EXISTS (
          SELECT 1 FROM unnest(aliases.dataset_aliases) alias
          WHERE alias = CAST(:search.alias AS bigint)
            OR 'DUOS-' || lpad(alias::text, GREATEST(6, length(alias::text)), '0')
              ILIKE :search.pattern)
        OR s.send_id IN (SELECT send_row_id FROM name_matches)
      ORDER BY s.create_date DESC, s.send_id DESC
      OFFSET CASE WHEN CAST(:search.pattern AS text) IS NULL THEN 0 ELSE :offset END
      LIMIT :limit
      """)
  List<MailSend> fetchSendsByCreateDate(
      @Bind("start") Date start,
      @Bind("end") Date end,
      @Bind("limit") Integer limit,
      @Bind("offset") Integer offset,
      @Bind("recipientLimit") Integer recipientLimit,
      @BindMethods("types") EmailTypeLists types,
      @BindMethods("search") MailSendSearch search);
}
