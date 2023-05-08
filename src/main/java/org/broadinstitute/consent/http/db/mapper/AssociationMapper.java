package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Association;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AssociationMapper implements RowMapper<Association> {

    @Override
    public Association map(ResultSet r, StatementContext statementContext) throws SQLException {
        Association association = new Association();
        association.setAssociationId(r.getInt("association_id"));
        association.setConsentId(r.getString("consent_id"));
        association.setAssociationType(r.getString("association_type"));
        association.setObjectId(r.getString("object_id"));
        association.setDataSetId(r.getInt("dataset_id"));
        return association;
    }
}
