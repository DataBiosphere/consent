package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DaaMapper implements RowMapper<DataAccessAgreement>, RowMapperHelper {

  private final Map<Integer, DataAccessAgreement> daaMap = new HashMap<>();

  @Override
  public DataAccessAgreement map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
    DataAccessAgreement daa;
    Integer daaId = hasColumn(resultSet, "daa_id") ? resultSet.getInt("daa_id") : null;
    if (daaId == null) {
      if (hasColumn(resultSet, "daa_daa_id")) {
        daaId = resultSet.getInt("daa_daa_id");
      } else {
        return null;
      }
    }
    if (daaMap.containsKey(daaId)) {
      daa = daaMap.get(resultSet.getInt("daa_id"));
    } else {
      daa = new DataAccessAgreement();
      daa.setDaaId(daaId);
    }

    if (hasColumn(resultSet, "create_user_id")) {
      daa.setCreateUserId(resultSet.getInt("create_user_id"));
    } else if (hasColumn(resultSet, "daa_create_user_id")) {
      daa.setCreateUserId(resultSet.getInt("daa_create_user_id"));
    }

    if (hasColumn(resultSet, "create_date")) {
      daa.setCreateDate(resultSet.getTimestamp("create_date").toInstant());
    } else if (hasColumn(resultSet, "daa_create_date")) {
      daa.setCreateDate(resultSet.getTimestamp("daa_create_date").toInstant());
    }

    if (hasColumn(resultSet, "update_user_id")) {
      daa.setUpdateUserId(resultSet.getInt("update_user_id"));
    } else if (hasColumn(resultSet, "daa_update_user_id")) {
      daa.setUpdateUserId(resultSet.getInt("daa_update_user_id"));
    }

    if (hasColumn(resultSet, "update_date")) {
      daa.setUpdateDate(resultSet.getTimestamp("update_date").toInstant());
    } else if (hasColumn(resultSet, "daa_update_date")) {
      daa.setUpdateDate(resultSet.getTimestamp("daa_update_date").toInstant());
    }

    if (hasColumn(resultSet, "initial_dac_id")) {
      daa.setInitialDacId(resultSet.getInt("initial_dac_id"));
    } else if (hasColumn(resultSet, "daa_initial_dac_id")) {
      daa.setInitialDacId(resultSet.getInt("daa_initial_dac_id"));
    }

    daaMap.put(daa.getDaaId(), daa);
    return daa;
  }
}
