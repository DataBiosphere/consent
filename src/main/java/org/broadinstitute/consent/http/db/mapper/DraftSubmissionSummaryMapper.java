package org.broadinstitute.consent.http.db.mapper;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import org.broadinstitute.consent.http.models.DraftSummary;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DraftSubmissionSummaryMapper implements RowMapper<DraftSummary>,
    RowMapperHelper {

  @Override
  public DraftSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    String name = hasColumn(rs, "name") ? rs.getString("name") : "";
    UUID uuid = UUID.fromString(rs.getString("uuid"));
    Date createDate = rs.getTimestamp("create_date");
    Date updateDate = hasColumn(rs, "update_date") ? rs.getTimestamp("update_date") : null;

    return new DraftSummary(uuid, name, createDate, updateDate);
  }
}
