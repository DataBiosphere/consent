package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.DataSetAudit;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;


public interface DataSetAuditDAO extends Transactional<DataSetAuditDAO> {

    @SqlUpdate("insert into dataset_audit (dataSetId, changeAction, modifiedByUser, modificationDate, objectId, name, active) " +
            " values (:dataSetId, :action, :user, :date, :objectId, :name, :active )")
    @GetGeneratedKeys
    Integer insertDataSetAudit(@BindBean DataSetAudit dataSets);


}
