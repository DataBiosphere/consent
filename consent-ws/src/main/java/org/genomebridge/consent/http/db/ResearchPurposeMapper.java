package org.genomebridge.consent.http.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.genomebridge.consent.http.models.ResearchPurpose;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class ResearchPurposeMapper implements ResultSetMapper<ResearchPurpose> {

    public ResearchPurpose map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new ResearchPurpose(r.getInt("purposeId"), r.getString("purpose"));
    }
}
