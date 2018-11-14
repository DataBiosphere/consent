package org.broadinstitute.consent.http.service;

import java.util.List;

public interface AuditServiceAPI {

    void saveAssociationAudit(String workspaceId, String modifiedTable, String changeAction, String modifiedByUserEmail);

    void saveAssociationAuditList(List<String> ids, String modifiedTable, String changeAction, String modifiedByUserEmail);

    void saveConsentAudit(String workspaceId, String modifiedTable, String changeAction, String modifiedByUserEmail);
}
