package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Dac;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DacMapper implements ResultSetMapper<Dac> {
    @Override
    public Dac map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        Dac dac = new Dac();
        dac.setDacId(resultSet.getInt("dac_id"));
        dac.setName(resultSet.getString("name"));
        dac.setDescription(resultSet.getString("description"));
        dac.setCreateDate(resultSet.getDate("create_date"));
        dac.setUpdateDate(resultSet.getDate("update_date"));
        return dac;
    }
}
