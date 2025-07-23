package org.broadinstitute.consent.http.db;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DACAutomationRuleAuditMapper;
import org.broadinstitute.consent.http.db.mapper.DACAutomationRuleMapper;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleAudit;
import org.broadinstitute.consent.http.rules.RuleAuditAction;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DACAutomationRuleMapper.class)
public interface DACAutomationRuleDAO extends Transactional<DACAutomationRuleDAO> {

  @SqlQuery("""
      SELECT * FROM dac_automation_rules
      """)
  List<DACAutomationRule> findAll();

  default Integer auditedInsertDACRuleSetting(int dacId, int ruleId, int userId, Instant activationDate) {
    return inTransaction(d -> {
      Handle handle = getHandle();
      String auditSql = """
        INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id, action_date)
        VALUES (:auditType::rule_audit_action, :dacId, :ruleId, :auditUserId, :actionDate)
        """;
      String insertRuleSql = """
        INSERT INTO dac_rule_settings (dac_id, rule_id, user_id, activation_date)
        VALUES (:dacId, :ruleId, :userId, current_timestamp)
        """;

      handle.createUpdate(auditSql)
          .bind("dacId", dacId)
          .bind("ruleId", ruleId)
          .bind("auditUserId", userId)
          .bind("auditType", RuleAuditAction.ADD)
          .bind("actionDate", activationDate)
          .execute();

      return handle.createUpdate(insertRuleSql)
          .bind("dacId", dacId)
          .bind("ruleId", ruleId)
          .bind("userId", userId)
          .executeAndReturnGeneratedKeys("id")
          .mapTo(Integer.class)
          .one();
    });
  }

  default void auditedDeleteDACRuleSetting(int dacId, int ruleId, int auditUserId) {
    useTransaction(d -> {
      Handle handle = getHandle();
      String auditSql = """
          INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id, action_date)
          SELECT :auditType::rule_audit_action, s.dac_id, s.rule_id, :auditUserId, current_timestamp
          FROM dac_rule_settings s
          WHERE s.dac_id = :dacId AND s.rule_id = :ruleId
          """;
      String deleteRuleSql = """
          DELETE FROM dac_rule_settings WHERE dac_id = :dacId AND rule_id = :ruleId
          """;

        handle.createUpdate(auditSql)
            .bind("dacId", dacId)
            .bind("ruleId", ruleId)
            .bind("auditUserId", auditUserId)
            .bind("auditType", RuleAuditAction.REMOVE)
            .execute();
        handle.createUpdate(deleteRuleSql)
            .bind("dacId", dacId)
            .bind("ruleId", ruleId)
            .execute();
        handle.commit();
    });
  }

  default Integer auditedDeleteDACRuleSettingByUser(int dacId, int userId, int auditUserId) {
    return inTransaction(d -> {
      Handle handle = getHandle();
      // Note that we're logging the audit user as the user for the audit record
      String auditSql = """
          INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id, action_date)
          SELECT :auditType::rule_audit_action, s.dac_id, s.rule_id, :auditUserId, current_timestamp
          FROM dac_rule_settings s
          WHERE s.dac_id = :dacId  AND s.user_id = :userId;
          """;
      String deleteRuleSql = """
          DELETE FROM dac_rule_settings WHERE dac_id = :dacId  AND user_id = :userId
          """;

      handle.createUpdate(auditSql)
          .bind("dacId", dacId)
          .bind("userId", userId)
          .bind("auditUserId", auditUserId)
          .bind("auditType", RuleAuditAction.REMOVE)
          .execute();
      return handle.createUpdate(deleteRuleSql)
          .bind("dacId", dacId)
          .bind("userId", userId)
          .execute();
    });
  }

  default Integer auditedDeleteAllDACRuleSettingForUser(int userId, int auditUserId) {
    return inTransaction(d -> {
      Handle handle = getHandle();
      String auditSql = """
          INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id, action_date)
          SELECT :auditType::rule_audit_action, s.dac_id, s.rule_id, :auditUserId, current_timestamp
          FROM dac_rule_settings s
          WHERE s.user_id = :userId;
          """;
      String deleteRulesSql = """
          DELETE FROM dac_rule_settings WHERE user_id = :userId
          """;
      Integer count;
      handle.createUpdate(auditSql)
          .bind("auditUserId", auditUserId)
          .bind("userId", userId)
          .bind("auditType", RuleAuditAction.REMOVE)
          .execute();
      return handle.createUpdate(deleteRulesSql)
          .bind("userId", userId)
          .execute();
    });
  }

  @SqlQuery("""
      SELECT rules.*, settings.user_id, settings.activation_date, u.*
      FROM dac_automation_rules rules
      LEFT JOIN dac_rule_settings settings ON rules.id = settings.rule_id AND settings.dac_id = :dacId
      LEFT JOIN users u on settings.user_id = u.user_id 
      WHERE state = 'AVAILABLE'
      """)
  List<DACAutomationRule> findAllDACAutomationRulesByDACId(@Bind("dacId") int dacId);

  @RegisterRowMapper(DACAutomationRuleAuditMapper.class)
  @SqlQuery("""
      SELECT a.action, a.action_date, r.rule, u.email, u.display_name from dac_rule_audit as a
      LEFT JOIN dac_automation_rules as r on r.id = a.rule_id
      LEFT JOIN users u on a.user_id = u.user_id
      WHERE a.dac_id = :dacId
      ORDER BY a.action_date DESC
      LIMIT :limit
      OFFSET :offset
      """)
  List<DACAutomationRuleAudit> findAutomationAuditsForDac(@Bind("dacId") int dacId,
      @Bind("limit") int limit,
      @Bind("offset") int offset);

  @SqlQuery("""
      SELECT count(*) from dac_rule_audit WHERE dac_id = :dacId
      """)
  Integer findCountOfAutomationAuditsForDac(@Bind("dacId") int dacId);
}
