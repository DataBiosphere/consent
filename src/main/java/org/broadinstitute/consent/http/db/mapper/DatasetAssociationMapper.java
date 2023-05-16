package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetAssociationMapper implements RowMapper<DatasetAssociation> {

  @Override
  public DatasetAssociation map(ResultSet r, StatementContext statementContext)
      throws SQLException {
    DatasetAssociation association = new DatasetAssociation();
    association.setDatasetId(r.getInt("datasetId"));
    association.setDacuserId(r.getInt("dacuserId"));
    association.setCreateDate(r.getDate("createDate"));
    return association;
  }
}
