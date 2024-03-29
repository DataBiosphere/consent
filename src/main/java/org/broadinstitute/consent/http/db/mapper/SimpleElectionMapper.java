package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.enumeration.ElectionFields;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class SimpleElectionMapper implements RowMapper<Election>, RowMapperHelper {

  @Override
  public Election map(ResultSet r, StatementContext ctx) throws SQLException {
    Election e = new Election(
        r.getInt(ElectionFields.ID.getValue()),
        r.getString(ElectionFields.TYPE.getValue()),
        r.getString(ElectionFields.STATUS.getValue()),
        r.getDate(ElectionFields.CREATE_DATE.getValue()),
        r.getString(ElectionFields.REFERENCE_ID.getValue()),
        r.getDate(ElectionFields.LAST_UPDATE.getValue()),
        (r.getString(ElectionFields.FINAL_ACCESS_VOTE.getValue()) == null)
            ? null
            : r.getBoolean(ElectionFields.FINAL_ACCESS_VOTE.getValue()),
        r.getInt(ElectionFields.DATASET_ID.getValue()),
        r.getBoolean(ElectionFields.ARCHIVED.getValue()),
        r.getString(ElectionFields.DUL_NAME.getValue()),
        r.getString(ElectionFields.DATA_USE_LETTER.getValue()));
    if (hasColumn(r, ElectionFields.FINAL_VOTE.getValue())) {
      if (r.getString(ElectionFields.FINAL_VOTE.getValue()) != null) {
        e.setFinalVote(r.getBoolean(ElectionFields.FINAL_VOTE.getValue()));
      }
    }
    return e;
  }
}
