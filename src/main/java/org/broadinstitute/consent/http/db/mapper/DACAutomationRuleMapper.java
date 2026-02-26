package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleState;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DACAutomationRuleMapper implements RowMapper<DACAutomationRule>, RowMapperHelper {

  @Override
  public DACAutomationRule map(ResultSet rs, StatementContext ctx) throws SQLException {
    Integer id = rs.getInt("id");
    DACAutomationRuleType ruleType = DACAutomationRuleType.valueOf(rs.getString("rule"));
    String description = rs.getString("description");
    RuleState ruleState = RuleState.valueOf(rs.getString("state"));
    Timestamp activationDate = null;
    if (hasColumn(rs, "activation_date")) {
      activationDate = rs.getTimestamp("activation_date");
    }
    Integer enabledByUserId = null;
    String userName = null;
    String userEmail = null;
    if (hasNonZeroColumn(rs, "user_id")) {
      enabledByUserId = rs.getInt("user_id");
    }
    if (hasColumn(rs, "email")) {
      userEmail = rs.getString("email");
    }
    if (hasColumn(rs, "display_name")) {
      userName = rs.getString("display_name");
    }
    return new DACAutomationRule(
        id, ruleType, description, ruleState, activationDate, enabledByUserId, userName, userEmail);
  }
}
