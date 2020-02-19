package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.WorkspaceAudit;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface WorkspaceAuditDAO {

    @SqlUpdate("insert into workspace_audit (modifiedObjectId, modifiedTable, changeAction, modifiedByUserId, modificationDate) " +
            " values (:modifiedObjectId, :modifiedTable, :changeAction, :modifiedByUserId, :modificationDate)")
    void insertWorkspaceAudit(@BindBean WorkspaceAudit workspaceAudit);

    @SqlBatch("insert into workspace_audit (modifiedObjectId, modifiedTable, changeAction, modifiedByUserId, modificationDate) " +
            " values (:modifiedObjectId, :modifiedTable, :changeAction, :modifiedByUserId, :modificationDate)")
    void batchInsertWorkspaceAudit(@BindBean List<WorkspaceAudit> workspaceAudits);

}
