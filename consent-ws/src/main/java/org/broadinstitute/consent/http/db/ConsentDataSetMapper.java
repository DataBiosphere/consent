package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.ConsentDataSet;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class ConsentDataSetMapper implements ResultSetMapper<ConsentDataSet> {

    private Map<String, ConsentDataSet>  consentDataSets = new HashMap<>();

    public ConsentDataSet map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        ConsentDataSet consentDataSet;
        if (index == 0 || !consentDataSets.containsKey(r.getString("consentId"))) {
            consentDataSet = new ConsentDataSet(r.getString("consentId"), new HashMap<>());
            consentDataSet.getDataSets().put(r.getString("objectId"), r.getString("name"));
            consentDataSets.put(r.getString("consentId"),consentDataSet);
        } else {
            consentDataSet = consentDataSets.get(r.getString("consentId"));
            consentDataSet.getDataSets().put(r.getString("objectId"), r.getString("name"));
            consentDataSets.put(r.getString("consentId"), consentDataSet);
        }
        return consentDataSet;
    }

}
