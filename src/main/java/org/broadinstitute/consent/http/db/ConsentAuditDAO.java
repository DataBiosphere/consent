package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.ConsentAudit;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface ConsentAuditDAO {

  @SqlUpdate(
      " INSERT INTO consent_audit (modified_object_id, modified_table, change_action, modified_by_user_id, modification_date) "
          + " VALUES (:modifiedObjectId, :modifiedTable, :changeAction, :modifiedByUserId, :modificationDate)")
  void insertWorkspaceAudit(@BindBean ConsentAudit consentAudit);

  @SqlBatch(
      " INSERT INTO consent_audit (modified_object_id, modified_table, change_action, modified_by_user_id, modification_date) "
          + " VALUES (:modifiedObjectId, :modifiedTable, :changeAction, :modifiedByUserId, :modificationDate)")
  void batchInsertWorkspaceAudit(@BindBean List<ConsentAudit> consentAudits);

  @SqlUpdate(" DELETE FROM consent_audit where modified_object_id in (<objectIds>) ")
  void deleteByObjectIds(@BindList("objectIds") List<String> objectIds);

  @SqlQuery(" SELECT distinct modified_object_id FROM consent_audit ")
  List<String> findAllObjectIds();
}
