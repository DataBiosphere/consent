package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Association;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class AssociationMapper implements RowMapper<Association> {

  @Override
  public Association map(ResultSet r, StatementContext statementContext) throws SQLException {
    Association association = new Association();
    association.setAssociationId(r.getInt("associationId"));
    association.setConsentId(r.getString("consentId"));
    association.setAssociationType(r.getString("associationType"));
    association.setObjectId(r.getString("objectId"));
    association.setDataSetId(r.getInt("dataSetId"));
    return association;
  }
}
