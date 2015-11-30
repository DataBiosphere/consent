package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConsentMapper implements ResultSetMapper<Consent> {

    public Consent map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Consent consent = new Consent();
        consent.consentId = r.getString("consentId");
        consent.requiresManualReview = r.getBoolean("requiresManualReview");
        consent.dataUseLetter = r.getString("dataUseLetter");
        consent.dulName = r.getString("dulName");
        try {
            consent.useRestriction = UseRestriction.parse(r.getString("useRestriction"));
        } catch (IOException e) {
            throw new SQLException(e);
        }
        consent.name = r.getString("name");
        consent.setCreateDate(r.getTimestamp("createDate"));
        consent.setSortDate(r.getTimestamp("sortDate"));
        consent.setLastUpdate((r.getTimestamp("lastUpdate") == null) ? null : r.getTimestamp("lastUpdate"));
        consent.setTranslatedUseRestriction((r.getString("translatedUseRestriction") == null) ? null : r.getString("translatedUseRestriction"));
        return consent;
    }

}
