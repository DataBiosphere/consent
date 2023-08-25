package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.AuditTable;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentAudit;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

public class ConsentAuditDAOTest extends DAOTestHelper {

  @Test
  public void testInsertWorkspaceAudit() {
    ConsentAudit audit = createConsentAudit();

    consentAuditDAO.insertWorkspaceAudit(audit);

    List<String> consentIds = consentAuditDAO.findAllObjectIds();
    assertFalse(consentIds.isEmpty());
    assertEquals(audit.getModifiedObjectId(), consentIds.get(0));
  }

  @Test
  public void testBatchInsertWorkspaceAudit() {
    List<ConsentAudit> audits =
        List.of(createConsentAudit(), createConsentAudit(), createConsentAudit());
    List<String> auditObjectIds =
        audits.stream().map(ConsentAudit::getModifiedObjectId).collect(Collectors.toList());

    consentAuditDAO.batchInsertWorkspaceAudit(audits);

    List<String> consentIds = consentAuditDAO.findAllObjectIds();
    assertFalse(consentIds.isEmpty());
    auditObjectIds.forEach(id -> assertTrue(consentIds.contains(id)));
  }

  private ConsentAudit createConsentAudit() {
    User user = createUser();
    Consent consent = createConsent();
    return new ConsentAudit(
        consent.getConsentId(),
        AuditTable.CONSENT.getValue(),
        AuditActions.CREATE.getValue(),
        user.getUserId(),
        new Date());
  }

  private Consent createConsent() {
    String consentId = UUID.randomUUID().toString();
    consentDAO.insertConsent(consentId,
        false,
        "{\"generalUse\": true }",
        "dul",
        consentId,
        "dulName",
        new Date(),
        new Date(),
        "Group");
    return consentDAO.findConsentById(consentId);
  }

}
