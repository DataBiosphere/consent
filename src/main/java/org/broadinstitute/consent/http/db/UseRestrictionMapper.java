package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UseRestrictionMapper implements ResultSetMapper<UseRestrictionDTO> {

        public UseRestrictionDTO map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return new UseRestrictionDTO(
                    r.getString("name"),
                    r.getString("useRestriction"),
                    r.getString("consentId")
            );
        }

}
