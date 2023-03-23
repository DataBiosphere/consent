package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class ImmutablePairOfIntsMapper implements RowMapper<Pair<Integer, Integer>> {

  public Pair<Integer, Integer> map(ResultSet r, StatementContext ctx) throws SQLException {
    return new ImmutablePair<>(r.getInt(1), r.getInt(2));
  }
}
