package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.DataSetAudit;
import org.broadinstitute.consent.http.models.DataSetAuditProperty;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;


public interface DataSetAuditDAO extends Transactional<DataSetAuditDAO> {

    @SqlBatch("insert into dataset_audit_property (dataset_audit_id, dataSetId, propertyKey, propertyValue, modificationDate)" +
            " values (:dataSetAuditId, :dataSetId, :propertyKey, :propertyValue, :date)")
    void insertDataSetAuditProperties(@BindBean List<DataSetAuditProperty> dataSetPropertiesList);

    @SqlUpdate("insert into dataset_audit (dataSetId, changeAction, modifiedByUser, modificationDate, objectId, name, active) " +
            " values (:dataSetId, :action, :user, :date, :objectId, :name, :active )")
    @GetGeneratedKeys
    Integer insertDataSetAudit(@BindBean DataSetAudit dataSets);


}
