package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.DataSet;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConsentDataSetMapper implements ResultSetMapper<Map<String, List<String>>> {

    private Map<String, List<String>> consentDataSets = new HashMap<>();

    public Map<String, List<String>> map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        List<String> dataSets = new ArrayList<>();
        if (index == 0 || !consentDataSets.containsKey(r.getString("consentId"))) {
            dataSets.add(r.getString("objectId"));
            consentDataSets.put(r.getString("consentId"),dataSets);
        } else {
            dataSets = consentDataSets.get(r.getInt("consentId"));
            dataSets.add(r.getString("objectId"));
        }
        return consentDataSets;
    }

}
