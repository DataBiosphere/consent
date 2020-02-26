package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UseRestrictionMapper implements RowMapper<UseRestrictionDTO> {

        public UseRestrictionDTO map(ResultSet r, StatementContext ctx) throws SQLException {
            return new UseRestrictionDTO(
                    r.getString("name"),
                    r.getString("useRestriction"),
                    r.getString("consentId")
            );
        }

}
