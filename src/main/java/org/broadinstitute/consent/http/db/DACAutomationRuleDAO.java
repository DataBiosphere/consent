package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DACAutomationRuleMapper;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
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

  @SqlUpdate("""
      DELETE FROM dac_rule_settings WHERE dac_id = :dacId AND rule_id = :ruleId
      """)
  void deleteDACRuleSetting(@Bind("dacId") int dacId, @Bind("ruleId") int ruleId);

  @SqlUpdate("""
      DELETE FROM dac_rule_settings WHERE dac_id = :dacId  WHERE user_id = :userId
      """)
  void deleteDACRuleSettingByUser(@Bind("dacId") int dacId, @Bind("userId") int userId);

  @SqlQuery("""
      SELECT rules.*, settings.user_id, u.*
      FROM dac_automation_rules rules
      LEFT JOIN dac_rule_settings settings ON rules.id = settings.rule_id AND settings.dac_id = :dacId
      LEFT JOIN users u on settings.user_id = u.user_id 
      WHERE state = 'AVAILABLE'
      """)
  List<DACAutomationRule> findAllDACAutomationRulesByDACId(@Bind("dacId") int dacId);

}
