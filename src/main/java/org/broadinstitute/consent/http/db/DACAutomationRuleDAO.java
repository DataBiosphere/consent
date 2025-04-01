package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DACAutomationRuleMapper;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DACAutomationRuleMapper.class)
public interface DACAutomationRuleDAO extends Transactional<DACAutomationRuleDAO> {

  @SqlQuery("""
      SELECT * FROM dac_automation_rules
      """)
  List<DACAutomationRule> findAll();

  @SqlUpdate("""
      INSERT INTO dac_rule_settings (dac_id, rule_id, user_id) VALUES (:dacId, :ruleId, :userId)
      """)
  @GetGeneratedKeys
  Integer insertDACRuleSetting(@Bind("dacId") int dacId, @Bind("ruleId") int ruleId, @Bind("userId") int userId);

  default Integer auditedInsertDACRuleSetting(int dacId, int ruleId, int userId) {
    Handle handle = getHandle();
    Integer id;
    String auditSql = """
        INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id)
        VALUES ('ADD', :dacId, :ruleId, :auditUserId)
        """;
    try (var audit = getHandle().createUpdate(auditSql)) {
      audit
          .bind("dacId", dacId)
          .bind("ruleId", ruleId)
          .bind("auditUserId", userId)
          .execute();
      id = insertDACRuleSetting(dacId, ruleId, userId);
      handle.commit();
    } catch (Exception e) {
      handle.rollback();
      throw e;
    } finally {
      handle.close();
    }
    return id;
  }

  @SqlUpdate("""
      DELETE FROM dac_rule_settings WHERE dac_id = :dacId AND rule_id = :ruleId
      """)
  void deleteDACRuleSetting(@Bind("dacId") int dacId, @Bind("ruleId") int ruleId);

  default void auditedDeleteDACRuleSetting(int dacId, int ruleId, int auditUserId) {
    Handle handle = getHandle();
    String auditSql = """
        INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id)
        SELECT 'REMOVE', s.dac_id, s.rule_id, :auditUserId
        FROM dac_rule_settings s
        WHERE s.dac_id = :dacId AND s.rule_id = :ruleId
        """;
    try (var audit = getHandle().createUpdate(auditSql)) {
      audit
          .bind("dacId", dacId)
          .bind("ruleId", ruleId)
          .bind("auditUserId", auditUserId)
          .execute();
      deleteDACRuleSetting(dacId, ruleId);
      handle.commit();
    } catch (Exception e) {
      handle.rollback();
      throw e;
    } finally {
      handle.close();
    }
  }

  @SqlUpdate("""
      DELETE FROM dac_rule_settings WHERE dac_id = :dacId  AND user_id = :userId
      """)
  Integer deleteDACRuleSettingByUser(@Bind("dacId") int dacId, @Bind("userId") int userId);

  default Integer auditedDeleteDACRuleSettingByUser(int dacId, int userId, int auditUserId) {
    Handle handle = getHandle();
    // Note that we're logging the audit user as the user for the audit record
    String auditSql = """
        INSERT INTO dac_rule_audit (action, dac_id, rule_id, user_id)
        SELECT 'REMOVE', s.dac_id, s.rule_id, :auditUserId
        FROM dac_rule_settings s
        WHERE s.dac_id = :dacId  AND s.user_id = :userId;
        """;
    Integer count;
    try (var audit = getHandle().createUpdate(auditSql)) {
      audit
          .bind("dacId", dacId)
          .bind("userId", userId)
          .bind("auditUserId", auditUserId)
          .execute();
      count = deleteDACRuleSettingByUser(dacId, userId);
      handle.commit();
    } catch (Exception e) {
      handle.rollback();
      throw e;
    } finally {
      handle.close();
    }
    return count;
  }

  @SqlUpdate("""
      DELETE FROM dac_rule_settings WHERE user_id = :userId 
      """)
  Integer deleteAllDACRuleSettingForUser(@Bind("userId") int userId);

  @SqlUpdate("""
      DELETE FROM dac_rule_settings WHERE dac_id = :dacId 
      """)
  Integer deleteAllDACRuleSettings(@Bind("dacId") int dacId);


  @SqlQuery("""
      SELECT rules.*, settings.user_id, u.*
      FROM dac_automation_rules rules
      LEFT JOIN dac_rule_settings settings ON rules.id = settings.rule_id AND settings.dac_id = :dacId
      LEFT JOIN users u on settings.user_id = u.user_id 
      WHERE state = 'AVAILABLE'
      """)
  List<DACAutomationRule> findAllDACAutomationRulesByDACId(@Bind("dacId") int dacId);

}
