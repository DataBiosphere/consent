package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.HelpReport;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class HelpReportMapper implements ResultSetMapper<HelpReport> {

    public HelpReport map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new HelpReport(
                    r.getInt("report_id"),
                    r.getString("displayName"),
                    r.getDate("create_date"),
                    r.getString("subject"),
                    r.getString("description"));

    }
}