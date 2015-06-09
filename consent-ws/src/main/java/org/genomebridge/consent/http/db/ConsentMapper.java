package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.UseRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConsentMapper implements ResultSetMapper<Consent> {
    public Consent map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Consent rec = new Consent();
        rec.requiresManualReview = r.getBoolean("requiresManualReview");
        try {
            rec.useRestriction = UseRestriction.parse(r.getString("useRestriction"));
        } catch (IOException e) {
            throw new SQLException(e);
        }
        return rec;
    }
}
