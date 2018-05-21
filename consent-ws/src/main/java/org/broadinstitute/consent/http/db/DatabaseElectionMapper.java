package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.ElectionFields;
import org.broadinstitute.consent.http.models.Election;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseElectionMapper implements ResultSetMapper<Election> {

    @Override
    public Election map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new Election(
                r.getInt(ElectionFields.ID.getValue()),
                r.getString(ElectionFields.TYPE.getValue()),
                r.getString(ElectionFields.STATUS.getValue()),
                r.getDate(ElectionFields.CREATE_DATE.getValue()),
                r.getString(ElectionFields.REFERENCE_ID.getValue()),
                r.getDate(ElectionFields.LAST_UPDATE.getValue()),
                (r.getString(ElectionFields.FINAL_ACCESS_VOTE.getValue()) == null) ? null : r.getBoolean(ElectionFields.FINAL_ACCESS_VOTE.getValue()),
                r.getInt(ElectionFields.DATASET_ID.getValue()),
                r.getBoolean(ElectionFields.ARCHIVED.getValue())
        );
    }
}
