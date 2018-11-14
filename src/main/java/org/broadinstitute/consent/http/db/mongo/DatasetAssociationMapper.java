package org.broadinstitute.consent.http.db.mongo;

import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatasetAssociationMapper implements ResultSetMapper<DatasetAssociation> {

    @Override
    public DatasetAssociation map(int i, ResultSet r, StatementContext statementContext) throws SQLException {
        DatasetAssociation association = new DatasetAssociation();
        association.setDatasetId(r.getInt("datasetId"));
        association.setDacuserId(r.getInt("dacuserId"));
        association.setCreateDate(  r.getDate("createDate"));
        return association;
    }
}
