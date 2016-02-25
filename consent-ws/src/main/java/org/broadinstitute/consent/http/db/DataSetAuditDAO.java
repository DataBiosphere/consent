package org.broadinstitute.consent.http.db;
import org.broadinstitute.consent.http.DataSetAudit;
import org.broadinstitute.consent.http.models.DataSetAuditProperty;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import java.util.List;


public interface DataSetAuditDAO extends Transactional<DataSetAuditDAO> {

    @SqlBatch("insert into dataset_audit_property (dataset_audit_id, dataSetId, propertyKey, propertyValue, date)" +
            " values (:dataSetAuditId, :dataSetId, :propertyKey, :propertyValue, :date)")
    void insertDataSetAuditProperties(@BindBean List<DataSetAuditProperty> dataSetPropertiesList);

    @SqlUpdate("insert into dataset_audit (dataSetId, action, user, date, objectId, name, active) " +
            " values (:dataSetId, :action, :user, :date, :objectId, :name, :active )")
    @GetGeneratedKeys
    Integer insertDataSetAudit(@BindBean DataSetAudit dataSets);


}
