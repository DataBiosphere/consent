package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class ConsentDataSetMapper implements RowMapper<ConsentDataSet> {

    private Map<String, ConsentDataSet>  consentDataSets = new HashMap<>();

    public ConsentDataSet map(ResultSet r, StatementContext ctx) throws SQLException {
        ConsentDataSet consentDataSet;
        if (!consentDataSets.containsKey(r.getString("consentId"))) {
            consentDataSet = new ConsentDataSet(r.getString("consentId"), new HashMap<>(), (r.getString("objectId")));
            consentDataSet.getDataSets().put(r.getString("datasetId"), r.getString("name"));
            consentDataSets.put(r.getString("consentId"),consentDataSet);
        } else {
            consentDataSet = consentDataSets.get(r.getString("consentId"));
            consentDataSet.getDataSets().put(r.getString("datasetId"), r.getString("name"));
            consentDataSets.put(r.getString("consentId"), consentDataSet);
        }
        return consentDataSet;
    }

}
