package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.ConsentManage;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ConsentManageMapper implements ResultSetMapper<ConsentManage> {

    public ConsentManage map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        ConsentManage consentManage = new ConsentManage();
        consentManage.setConsentId(r.getString("consentId"));
        consentManage.setConsentName(r.getString("name"));
        consentManage.setCreateDate(r.getTimestamp("createDate"));
        consentManage.setSortDate(r.getTimestamp("sortDate"));
        consentManage.setElectionId(r.getInt("electionId"));
        consentManage.setElectionStatus(r.getString("status"));
        consentManage.setEditable(true);
        return consentManage;
    }

}
