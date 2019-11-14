package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.ElectionFields;
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
        if (r.getObject("dac_id") != null) {
            consentManage.setDacId(r.getInt("dac_id"));
        }
        consentManage.setSortDate(r.getTimestamp("sortDate"));
        consentManage.setElectionId(r.getInt(ElectionFields.ID.getValue()));
        consentManage.setElectionStatus(r.getString(ElectionFields.STATUS.getValue()));
        consentManage.setVersion(r.getInt(ElectionFields.VERSION.getValue()) < 10 ?  '0' + String.valueOf(r.getInt(ElectionFields.VERSION.getValue()))
                               : String.valueOf(r.getInt(ElectionFields.VERSION.getValue())));
        consentManage.setArchived(r.getBoolean(ElectionFields.ARCHIVED.getValue()));
        consentManage.setEditable(true);
        consentManage.setGroupName(r.getString("groupName"));
        consentManage.setUpdateStatus(r.getBoolean("updated"));
        return consentManage;
    }

}
