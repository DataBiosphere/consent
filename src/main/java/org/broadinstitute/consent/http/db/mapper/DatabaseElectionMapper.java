package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.db.RowMapperHelper;
import org.broadinstitute.consent.http.enumeration.ElectionFields;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseElectionMapper implements RowMapper<Election>, RowMapperHelper {

    @Override
    public Election map(ResultSet r, StatementContext ctx) throws SQLException {
        return new Election(
                r.getInt(ElectionFields.ID.getValue()),
                r.getString(ElectionFields.TYPE.getValue()),
                r.getString(ElectionFields.STATUS.getValue()),
                r.getDate(ElectionFields.CREATE_DATE.getValue()),
                r.getString(ElectionFields.REFERENCE_ID.getValue()),
                r.getDate(ElectionFields.LAST_UPDATE.getValue()),
                (r.getString(ElectionFields.FINAL_ACCESS_VOTE.getValue()) == null) ? null : r.getBoolean(ElectionFields.FINAL_ACCESS_VOTE.getValue()),
                r.getInt(ElectionFields.DATASET_ID.getValue()),
                r.getBoolean(ElectionFields.ARCHIVED.getValue()),
                r.getString(ElectionFields.DUL_NAME.getValue()),
                unescapeJava(r.getString(ElectionFields.TRANSLATED_USE_RESTRICTION.getValue())),
                r.getString(ElectionFields.DATA_USE_LETTER.getValue())
        );
    }
}
