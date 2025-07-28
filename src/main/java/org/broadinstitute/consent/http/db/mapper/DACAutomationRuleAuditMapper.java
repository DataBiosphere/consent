package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.broadinstitute.consent.http.rules.RuleAuditAction;
import org.broadinstitute.consent.http.rules.DACAutomationRuleAudit;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DACAutomationRuleAuditMapper implements RowMapper<DACAutomationRuleAudit>,
    RowMapperHelper {

  @Override
  public DACAutomationRuleAudit map(ResultSet rs, StatementContext ctx) throws SQLException {
    RuleAuditAction action = RuleAuditAction.valueOf(rs.getString("action"));
    Timestamp actionDate = Timestamp.valueOf(rs.getString("action_date"));
    DACAutomationRuleType rule = DACAutomationRuleType.valueOf(rs.getString("rule"));
    String email = rs.getString("email");
    String displayName = rs.getString("display_name");
    return new DACAutomationRuleAudit(action, actionDate, rule, email, displayName);
  }
}
