package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Association;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AssociationMapper implements ResultSetMapper<Association> {

    @Override
    public Association map(int i, ResultSet r, StatementContext statementContext) throws SQLException {
        Association association = new Association();
        association.setAssociationId(r.getInt("associationId"));
        association.setConsentId(r.getString("consentId"));
        association.setAssociationType(r.getString("associationType"));
        association.setObjectId(r.getString("objectId"));
        return association;
    }
}
