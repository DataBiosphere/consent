package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataUseDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConsentMapper implements ResultSetMapper<Consent> {

    public Consent map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Consent consent = new Consent();
        consent.setConsentId(r.getString("consentId"));
        consent.setRequiresManualReview(r.getBoolean("requiresManualReview"));
        consent.setDataUseLetter(r.getString("dataUseLetter"));
        consent.setDulName(r.getString("dulName"));
        try {
            consent.setUseRestriction(UseRestriction.parse(r.getString("useRestriction")));
        } catch (IOException e) {
            throw new SQLException(e);
        }
        consent.setDataUse(DataUseDTO.parseDataUse(r.getString("dataUse")).orElse(null));
        consent.setName(r.getString("name"));
        consent.setCreateDate(r.getTimestamp("createDate"));
        consent.setSortDate(r.getTimestamp("sortDate"));
        consent.setLastUpdate((r.getTimestamp("lastUpdate") == null) ? null : r.getTimestamp("lastUpdate"));
        consent.setTranslatedUseRestriction((r.getString("translatedUseRestriction") == null) ? null : r.getString("translatedUseRestriction"));
        return consent;
    }

}
