package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DaaMapper implements RowMapper<DataAccessAgreement>, RowMapperHelper {

  private final Map<Integer, DataAccessAgreement> daaMap = new HashMap<>();

  @Override
  public DataAccessAgreement map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
    DataAccessAgreement daa;
    if (daaMap.containsKey(resultSet.getInt("id"))) {
      daa = daaMap.get(resultSet.getInt("id"));
    } else {
      daa = new DataAccessAgreement();
      daa.setId(resultSet.getInt("id"));
    }
    daa.setCreateUserId(resultSet.getInt("create_user_id"));
    daa.setCreateDate(resultSet.getTimestamp("create_date").toInstant());
    if (hasColumn(resultSet, "update_date")) {
      daa.setUpdateDate(resultSet.getTimestamp("update_date").toInstant());
    }
    if (hasColumn(resultSet, "update_user_id")) {
      daa.setUpdateUserId(resultSet.getInt("update_user_id"));
    }
    if (hasColumn(resultSet, "initial_dac_id")) {
      daa.setInitialDacId(resultSet.getInt("initial_dac_id"));
    }
    daaMap.put(daa.getId(), daa);
    return daa;
  }
}
