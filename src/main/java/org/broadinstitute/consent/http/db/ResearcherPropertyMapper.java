package org.broadinstitute.consent.http.db;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResearcherPropertyMapper implements ResultSetMapper<ResearcherProperty> {

    @Override
    public ResearcherProperty map(int index, ResultSet r, StatementContext statementContext) throws SQLException {
        return new ResearcherProperty(r.getInt("propertyId"), r.getInt("userId"), r.getString("propertyKey"), r.getString("propertyValue"));
    }
}
