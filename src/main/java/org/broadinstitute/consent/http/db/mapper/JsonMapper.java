package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class JsonMapper implements RowMapper<JsonObject> {
  String columnName;

  public JsonMapper(String columnName) {
    this.columnName = columnName;
  }

  @Override
  public JsonObject map(ResultSet rs, StatementContext ctx) throws SQLException {
    return GsonUtil.getInstance().fromJson(rs.getString(columnName), JsonObject.class);
  }
}
