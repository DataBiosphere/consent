package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.DACAutomationRuleMapper;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(DACAutomationRuleMapper.class)
public interface DACAutomationRuleDAO extends Transactional<DACAutomationRuleDAO> {

  @SqlQuery("""
      SELECT * from dac_automation_rules
      """)
  List<DACAutomationRule> findAll();

  @SqlQuery("""
      SELECT * from dac_automation_rules where rule_state = 'AVAILABLE'
      """)
  List<DACAutomationRule> findAllAvailable();

}
