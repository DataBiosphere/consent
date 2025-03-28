package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.RuleState;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DACAutomationRuleMapper implements RowMapper<DACAutomationRule>, RowMapperHelper {

  @Override
  public DACAutomationRule map(ResultSet rs, StatementContext ctx) throws SQLException {
    Integer id = rs.getInt("id");
    DACAutomationRuleType ruleType = DACAutomationRuleType.valueOf(rs.getString("rule_type"));
    String description = rs.getString("description");
    RuleState ruleState = RuleState.valueOf(rs.getString("rule_state"));
    return new DACAutomationRule(id, ruleType, description, ruleState);
  }

}
