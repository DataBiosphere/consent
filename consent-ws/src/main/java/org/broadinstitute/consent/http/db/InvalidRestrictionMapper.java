package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.dto.InvalidRestriction;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InvalidRestrictionMapper implements ResultSetMapper<InvalidRestriction> {

        public InvalidRestriction map(int index, ResultSet r, StatementContext ctx) throws SQLException {

            return new InvalidRestriction(
                    r.getString("name"),
                    r.getString("useRestriction")
            );
        }

}
