package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;

public class DataAccessRequestDataMapper implements RowMapper<DataAccessRequestData>,
    RowMapperHelper {


  @Override
  public DataAccessRequestData map(ResultSet resultSet, StatementContext statementContext)
      throws SQLException {
    String darDataString = resultSet.getObject("data", PGobject.class).getValue();
    DataAccessRequestData data = translate(darDataString);
    return data;
  }

}
