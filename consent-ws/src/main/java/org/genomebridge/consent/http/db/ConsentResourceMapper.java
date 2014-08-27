/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.genomebridge.consent.http.db;

import org.genomebridge.consent.http.models.UseRestriction;
import org.genomebridge.consent.http.resources.ConsentResource;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConsentResourceMapper implements ResultSetMapper<ConsentResource> {
    public ConsentResource map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        ConsentResource rec = new ConsentResource();

        rec.requiresManualReview = r.getBoolean("requiresManualReview");
        try {
            rec.useRestriction = UseRestriction.parse(r.getString("useRestriction"));
        } catch (IOException e) {
            throw new SQLException(e);
        }

        return rec;
    }
}
