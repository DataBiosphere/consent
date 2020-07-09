package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.Dac;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DacMapper implements RowMapper<Dac>, RowMapperHelper {

  private Map<Integer, Dac> dacMap = new HashMap<>();

  @Override
  public Dac map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
    Dac dac;
    if (dacMap.containsKey(resultSet.getInt("dac_id"))) {
      dac = dacMap.get(resultSet.getInt("dac_id"));
    } else {
      dac = new Dac();
      dac.setDacId(resultSet.getInt("dac_id"));
    }
    dac.setName(resultSet.getString("name"));
    dac.setDescription(resultSet.getString("description"));
    dac.setCreateDate(resultSet.getDate("create_date"));
    dac.setUpdateDate(resultSet.getDate("update_date"));
    if (hasColumn(resultSet, "electionId")) {
      dac.addElectionId(resultSet.getInt("electionId"));
    }
    dacMap.put(dac.getDacId(), dac);
    return dac;
  }
}
