package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConsentMapper implements ResultSetMapper<Consent> {

    private Map<String, Consent> consentMap = new HashMap<>();

    public Consent map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Consent consent;
        if (!consentMap.containsKey(r.getString("consentId"))) {
            consent = new Consent();
            consent.setConsentId(r.getString("consentId"));
        } else {
            consent = consentMap.get(r.getString("consentId"));
        }
        consent.setRequiresManualReview(r.getBoolean("requiresManualReview"));
        consent.setDataUseLetter(r.getString("dataUseLetter"));
        consent.setDulName(r.getString("dulName"));
        try {
            consent.setUseRestriction(UseRestriction.parse(r.getString("useRestriction")));
        } catch (IOException e) {
            throw new SQLException(e);
        }
        consent.setDataUse(DataUse.parseDataUse(r.getString("dataUse")).orElse(null));
        consent.setName(r.getString("name"));
        consent.setCreateDate(r.getTimestamp("createDate"));
        consent.setSortDate(r.getTimestamp("sortDate"));
        consent.setLastUpdate(r.getTimestamp("lastUpdate"));
        consent.setTranslatedUseRestriction(r.getString("translatedUseRestriction"));
        consent.setGroupName(r.getString("groupName"));
        consent.setUpdated(r.getBoolean("updated"));
        if (r.getObject("dac_id") != null) {
            consent.setDacId(r.getInt("dac_id"));
        }
        consentMap.put(consent.getConsentId(), consent);
        return consent;
    }

}
