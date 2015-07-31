package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
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
        try {
            consent.useRestriction = UseRestriction.parse(r.getString("useRestriction"));
        } catch (IOException e) {
            throw new SQLException(e);
        }
        return consent;
    }

}
