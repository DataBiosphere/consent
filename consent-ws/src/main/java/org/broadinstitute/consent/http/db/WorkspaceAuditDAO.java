package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.WorkspaceAudit;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import java.util.List;

@UseStringTemplate3StatementLocator
public interface WorkspaceAuditDAO {

    @SqlUpdate("insert into workspace_audit (modifiedObjectId, modifiedTable, changeAction, modifiedByUserId, modificationDate) " +
            " values (:modifiedObjectId, :modifiedTable, :changeAction, :modifiedByUserId, :modificationDate)")
    void insertWorkspaceAudit(@BindBean WorkspaceAudit workspaceAudit);

    @SqlBatch("insert into workspace_audit (modifiedObjectId, modifiedTable, changeAction, modifiedByUserId, modificationDate) " +
            " values (:modifiedObjectId, :modifiedTable, :changeAction, :modifiedByUserId, :modificationDate)")
    void batchInsertWorkspaceAudit(@BindBean List<WorkspaceAudit> workspaceAudits);

}
