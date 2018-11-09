package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.ElectionFields;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ElectionMapper implements ResultSetMapper<Election> {

    @Override
    public Election map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        UseRestriction useRestriction = null;
        if (StringUtils.isNoneBlank((r.getString(ElectionFields.USE_RESTRICTION.getValue())))) {
            try {
                useRestriction = UseRestriction.parse(r.getString(ElectionFields.USE_RESTRICTION.getValue()));
            } catch (IOException e) {
                throw new SQLException(e);
            }
        }

        return new Election(
                r.getInt(ElectionFields.ID.getValue()),
                r.getString(ElectionFields.TYPE.getValue()),
                (r.getString(ElectionFields.FINAL_VOTE.getValue()) == null) ? null : r.getBoolean(ElectionFields.FINAL_VOTE.getValue()),
                r.getString(ElectionFields.FINAL_RATIONALE.getValue()),
                r.getString(ElectionFields.STATUS.getValue()),
                r.getDate(ElectionFields.CREATE_DATE.getValue()),
                r.getDate(ElectionFields.FINAL_VOTE_DATE.getValue()),
                r.getString(ElectionFields.REFERENCE_ID.getValue()),
                r.getDate(ElectionFields.LAST_UPDATE.getValue()),
                (r.getString(ElectionFields.FINAL_ACCESS_VOTE.getValue()) == null) ? null : r.getBoolean(ElectionFields.FINAL_ACCESS_VOTE.getValue()),
                useRestriction,
                r.getString(ElectionFields.TRANSLATED_USE_RESTRICTION.getValue()),
                r.getString(ElectionFields.DATA_USE_LETTER.getValue()),
                r.getString(ElectionFields.DUL_NAME.getValue()),
                r.getInt(ElectionFields.VERSION.getValue()),
                (r.getString(ElectionFields.ARCHIVED.getValue()) == null) ? null : r.getBoolean(ElectionFields.ARCHIVED.getValue())
        );
    }
}
