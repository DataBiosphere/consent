package org.broadinstitute.consent.http.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.mail.freemarker.VoteAndElectionModel;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class VoteAndElectionModelMapper implements ResultSetMapper<VoteAndElectionModel> {
    @Override
    public VoteAndElectionModel map(int i, ResultSet r, StatementContext statementContext) throws SQLException {
        return new VoteAndElectionModel(Integer.toString(r.getInt("electionId")), r.getString("referenceId"), r.getString("electionType"), r.getString("type"));
    }
}
