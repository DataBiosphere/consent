package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.DACRuleAssignment;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DACRuleAssignmentMapper implements RowMapper<DACRuleAssignment> {

  @Override
  public DACRuleAssignment map(ResultSet rs, StatementContext ctx) throws SQLException {
    return new DACRuleAssignment(
        rs.getInt("dac_id"), DACAutomationRuleType.valueOf(rs.getString("rule")));
  }
}
