package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DarDataset;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DarDatasetMapper implements RowMapper<DarDataset>, RowMapperHelper {
    @Override
    public DarDataset map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DarDataset darDataset = new DarDataset();
        darDataset.setReferenceId(resultSet.getString("reference_id"));
        darDataset.setDatasetId(resultSet.getInt("dataset_id"));

        return darDataset;
    }
}