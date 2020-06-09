package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.WorkspaceAuditDAO;
import org.broadinstitute.consent.http.models.WorkspaceAudit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuditService {

    private final UserDAO userDAO;
    private final WorkspaceAuditDAO workspaceAuditDAO;

    @Inject
    public AuditService(UserDAO userDAO, WorkspaceAuditDAO dao) {
        this.userDAO = userDAO;
        this.workspaceAuditDAO = dao;
    }

    public void saveAssociationAuditList(List<String> ids, String modifiedTable, String changeAction, String modifiedByUserEmail) {
        int modifiedByUserId = userDAO.findDACUserByEmail(modifiedByUserEmail).getDacUserId();
        List<WorkspaceAudit> auditList = createAuditList(ids, modifiedTable, changeAction, modifiedByUserId);
        workspaceAuditDAO.batchInsertWorkspaceAudit(auditList);
    }

    private List<WorkspaceAudit> createAuditList(List<String> ids, String modifiedTable, String changeAction, int modifiedByUserId) {
        List<WorkspaceAudit> audits = new ArrayList<>();
        Date date = new Date();
        for (String id : ids) {
            audits.add(new WorkspaceAudit(id, modifiedTable, changeAction, modifiedByUserId, date));
        }
        return audits;
    }

    public void saveConsentAudit(String consentId, String modifiedTable, String changeAction, String modifiedByUserEmail) {
        int modifiedByUserId = userDAO.findDACUserByEmail(modifiedByUserEmail).getDacUserId();
        saveAuditInfo(new WorkspaceAudit(consentId, modifiedTable, changeAction, modifiedByUserId, new Date()));
    }

    private void saveAuditInfo(WorkspaceAudit auditInfo) {
        workspaceAuditDAO.insertWorkspaceAudit(auditInfo);
    }

}
