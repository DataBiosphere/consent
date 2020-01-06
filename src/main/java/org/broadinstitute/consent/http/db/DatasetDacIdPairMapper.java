package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatasetDacIdPairMapper implements ResultSetMapper<Pair<Integer, Integer>> {

    public Pair<Integer, Integer> map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new ImmutablePair<>(r.getInt(1), r.getInt(2));
    }

}
