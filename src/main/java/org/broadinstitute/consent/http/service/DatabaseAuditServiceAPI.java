package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.WorkspaceAuditDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.WorkspaceAudit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseAuditServiceAPI extends AbstractAuditServiceAPI{

    private UserDAO userDAO;
    private WorkspaceAuditDAO workspaceAuditDAO;
    private AssociationDAO associationDAO;

    public static void initInstance(WorkspaceAuditDAO auditDAO, UserDAO userDAO, AssociationDAO associationDAO) {
        AuditServiceAPIHolder.setInstance(new DatabaseAuditServiceAPI(auditDAO, userDAO, associationDAO));
    }

    public DatabaseAuditServiceAPI(WorkspaceAuditDAO dao, UserDAO userDAO, AssociationDAO associationDAO) {
        this.workspaceAuditDAO = dao;
        this.userDAO = userDAO;
        this.associationDAO = associationDAO;
    }

    @Override
    public void saveAssociationAudit(String workspaceId, String modifiedTable, String changeAction, String modifiedByUserEmail){
        int modifiedByUserId = userDAO.findUserByEmail(modifiedByUserEmail).getUserId();
        String modifiedObjectId = associationDAO.findAssociationIdByTypeAndObjectId(AssociationType.WORKSPACE.getValue(), workspaceId).toString();
        saveAuditInfo(new WorkspaceAudit(modifiedObjectId, modifiedTable, changeAction, modifiedByUserId, new Date()));
    }

    @Override
    public void saveAssociationAuditList(List<String> ids, String modifiedTable, String changeAction, String modifiedByUserEmail){
        int modifiedByUserId = userDAO.findUserByEmail(modifiedByUserEmail).getUserId();
        List<WorkspaceAudit> auditList = createAuditList(ids, modifiedTable, changeAction, modifiedByUserId);
        workspaceAuditDAO.batchInsertWorkspaceAudit(auditList);
    }

    private List<WorkspaceAudit> createAuditList(List<String> ids, String modifiedTable, String changeAction, int modifiedByUserId) {
        List<WorkspaceAudit> audits = new ArrayList<>();
        Date date = new Date();
        for(String id: ids){
            audits.add(new WorkspaceAudit(id, modifiedTable, changeAction, modifiedByUserId, date));
        }
        return audits;
    }

    @Override
    public void saveConsentAudit(String consentId, String modifiedTable, String changeAction, String modifiedByUserEmail){
        int modifiedByUserId = userDAO.findUserByEmail(modifiedByUserEmail).getUserId();
        saveAuditInfo(new WorkspaceAudit(consentId, modifiedTable, changeAction, modifiedByUserId, new Date()));
    }

    private void saveAuditInfo(WorkspaceAudit auditInfo) {
        workspaceAuditDAO.insertWorkspaceAudit(auditInfo);
    }
}
