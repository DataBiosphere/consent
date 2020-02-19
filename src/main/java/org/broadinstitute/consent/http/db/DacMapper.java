package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Dac;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DacMapper implements RowMapper<Dac> {
    @Override
    public Dac map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        Dac dac = new Dac();
        dac.setDacId(resultSet.getInt("dac_id"));
        dac.setName(resultSet.getString("name"));
        dac.setDescription(resultSet.getString("description"));
        dac.setCreateDate(resultSet.getDate("create_date"));
        dac.setUpdateDate(resultSet.getDate("update_date"));
        return dac;
    }
}
