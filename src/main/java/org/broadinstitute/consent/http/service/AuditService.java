package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.ConsentAuditDAO;
import org.broadinstitute.consent.http.models.ConsentAudit;

public class AuditService {

    private final UserDAO userDAO;
    private final ConsentAuditDAO consentAuditDAO;

    @Inject
    public AuditService(UserDAO userDAO, ConsentAuditDAO dao) {
        this.userDAO = userDAO;
        this.consentAuditDAO = dao;
    }

    public void saveAssociationAuditList(List<String> ids, String modifiedTable, String changeAction, String modifiedByUserEmail) {
        int modifiedByUserId = userDAO.findUserByEmail(modifiedByUserEmail).getDacUserId();
        List<ConsentAudit> auditList = createAuditList(ids, modifiedTable, changeAction, modifiedByUserId);
        consentAuditDAO.batchInsertWorkspaceAudit(auditList);
    }

    private List<ConsentAudit> createAuditList(List<String> ids, String modifiedTable, String changeAction, int modifiedByUserId) {
        List<ConsentAudit> audits = new ArrayList<>();
        Date date = new Date();
        for (String id : ids) {
            audits.add(new ConsentAudit(id, modifiedTable, changeAction, modifiedByUserId, date));
        }
        return audits;
    }

    public void saveConsentAudit(String consentId, String modifiedTable, String changeAction, String modifiedByUserEmail) {
        int modifiedByUserId = userDAO.findUserByEmail(modifiedByUserEmail).getDacUserId();
        saveAuditInfo(new ConsentAudit(consentId, modifiedTable, changeAction, modifiedByUserId, new Date()));
    }

    private void saveAuditInfo(ConsentAudit auditInfo) {
        consentAuditDAO.insertWorkspaceAudit(auditInfo);
    }

}
