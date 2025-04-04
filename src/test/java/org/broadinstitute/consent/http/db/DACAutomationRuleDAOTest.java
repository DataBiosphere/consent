package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.RuleAuditAction;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleAudit;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleDAOTest extends DAOTestHelper {

  @Test
  void testFindAll() {
    List<DACAutomationRule> rules = dacAutomationRuleDAO.findAll();
    Assertions.assertFalse(rules.isEmpty());
    Assertions.assertTrue(
        rules.stream().anyMatch(rule -> rule.ruleType().equals(DACAutomationRuleType.GRU_V1)));
  }

  @Test
  void testInsertDACRuleSetting() {
    User user = createUser();
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rules = dacAutomationRuleDAO.findAll();
    Integer settingId = dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId, rules.get(0).id(),
        user.getUserId());
    Assertions.assertNotNull(settingId);
  }

  @Test
  void testAuditedInsertDACRuleSetting() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId1, rulesByDacId.get(0).id(),
        user.getUserId());
    List<DACAutomationRuleAudit> auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(
        dacId1, 5, 0);
    Assertions.assertNotNull(auditRecords);
    Assertions.assertFalse(auditRecords.isEmpty());
    Assertions.assertEquals(1, auditRecords.size());
    Assertions.assertEquals(RuleAuditAction.ADD, auditRecords.get(0).action());
    Assertions.assertEquals(user.getEmail(), auditRecords.get(0).email());
    Assertions.assertEquals(user.getDisplayName(), auditRecords.get(0).displayName());
    Assertions.assertEquals(1, dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId1));
  }

  @Test
  void testFindRulesByDacIdAddSetting() {
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    Assertions.assertNotNull(rulesByDacId);
    Assertions.assertFalse(rulesByDacId.isEmpty());
    rulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    User user = createUser();
    Integer settingId = dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId,
        rulesByDacId.get(0).id(),
        user.getUserId());
    Assertions.assertNotNull(settingId);
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    Assertions.assertTrue(updatedRulesByDacId.stream()
        .anyMatch(r -> Objects.equals(r.enabledByUserId(), user.getUserId())));
    Assertions.assertTrue(
        updatedRulesByDacId.stream().anyMatch(r -> Objects.equals(r.userEmail(), user.getEmail())));
    Assertions.assertTrue(updatedRulesByDacId.stream()
        .anyMatch(r -> Objects.equals(r.displayName(), user.getDisplayName())));
  }

  @Test
  void testFindRulesByDacIdRemoveSetting() {
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    User user = createUser();
    Integer settingId = dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId,
        rulesByDacId.get(0).id(),
        user.getUserId());
    Assertions.assertNotNull(settingId);
    dacAutomationRuleDAO.auditedDeleteDACRuleSetting(dacId, rulesByDacId.get(0).id(), user.getUserId());
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
  }

  @Test
  void testAuditedDeleteDACRuleSetting() {
    User user = createUser();
    User auditUser = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId1, rulesByDacId.get(0).id(),
        user.getUserId());

    dacAutomationRuleDAO.auditedDeleteDACRuleSetting(dacId1, rulesByDacId.get(0).id(),
        auditUser.getUserId());
    List<DACAutomationRuleAudit> auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(
        dacId1, 5, 0);
    Assertions.assertEquals(2, auditRecords.size());
    Assertions.assertEquals(auditRecords.size(), dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId1));
    Assertions.assertNotNull(auditRecords.get(0));
    Assertions.assertEquals(RuleAuditAction.REMOVE, auditRecords.get(0).action());
    Assertions.assertEquals(auditUser.getEmail(), auditRecords.get(0).email());
    Assertions.assertNotNull(auditRecords.get(1));
    Assertions.assertEquals(RuleAuditAction.ADD, auditRecords.get(1).action());
    Assertions.assertEquals(user.getEmail(), auditRecords.get(1).email());
  }

  @Test
  void testDeleteDACRuleSettingByUserId() {
    User user = createUser();
    User auditUser = createUser();
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId, rulesByDacId.get(0).id(),
        user.getUserId());
    Integer deletedCount = dacAutomationRuleDAO.auditedDeleteDACRuleSettingByUser(dacId, user.getUserId(), auditUser.getUserId());
    Assertions.assertEquals(1, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    List<DACAutomationRuleAudit> auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(dacId, 5, 0);
    Assertions.assertEquals(2, auditRecords.size());
    Assertions.assertNotNull(auditRecords.get(0));
    Assertions.assertEquals(RuleAuditAction.REMOVE, auditRecords.get(0).action());
    Assertions.assertEquals(auditUser.getEmail(), auditRecords.get(0).email());
  }

  @Test
  void testDeleteDACRuleSetting() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    rulesByDacId.forEach(r -> {
      dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId1, r.id(), user.getUserId());
      dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId2, r.id(), user.getUserId());
    });
    Integer deletedCount = dacAutomationRuleDAO.auditedDeleteDACRuleSettingByUser(dacId1,
        user.getUserId(), user.getUserId());
    Assertions.assertEquals(rulesByDacId.size(), deletedCount);
    Assertions.assertEquals(rulesByDacId.size() * 2, dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId1));
    Assertions.assertEquals(rulesByDacId.size(), dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId2));
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId1);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId2);
    updatedRulesByDacId.forEach(r -> Assertions.assertNotNull(r.enabledByUserId()));
  }

  @Test
  void testAuditedDeleteDACRuleSettingByUser() {
    User user = createUser();
    User auditUser = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    rulesByDacId.forEach(r -> {
      dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId1, r.id(), user.getUserId());
    });
    Integer deletedCount = dacAutomationRuleDAO.auditedDeleteDACRuleSettingByUser(dacId1,
        user.getUserId(), auditUser.getUserId());
    Assertions.assertEquals(rulesByDacId.size(), deletedCount);
    List<DACAutomationRuleAudit> deleteByUserAudits = dacAutomationRuleDAO.findAutomationAuditsForDac(
        dacId1, rulesByDacId.size(), 0);
    Assertions.assertEquals(rulesByDacId.size(), deleteByUserAudits.size());
    deleteByUserAudits.forEach(r -> {
      Assertions.assertEquals(RuleAuditAction.REMOVE, r.action());
    });
  }

  @Test
  void testDeleteAllSettingsByUserId() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId1, rulesByDacId.get(0).id(),
        user.getUserId());
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId2, rulesByDacId.get(0).id(),
        user.getUserId());
    Integer deletedCount = dacAutomationRuleDAO.auditedDeleteAllDACRuleSettingForUser(
        user.getUserId(), user.getUserId());
    Assertions.assertEquals(2, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId1);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId2);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    List<DACAutomationRuleAudit> auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(
        dacId1, rulesByDacId.size(), 0);
    Assertions.assertEquals(2, auditRecords.size());
  }

  @Test
  void testDacAutomationRuleAudit() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    rulesByDacId.forEach(r -> {
      dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId1, r.id(), user.getUserId());
    });
    List<DACAutomationRuleAudit> auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(
        dacId1, rulesByDacId.size(), 0);
    Assertions.assertEquals(rulesByDacId.size(), auditRecords.size());
    auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, rulesByDacId.size() - 1,
        0);
    Assertions.assertEquals(rulesByDacId.size() - 1, auditRecords.size());
    auditRecords = dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, 1,
        rulesByDacId.size() * 2);
    Assertions.assertTrue(auditRecords.isEmpty());
  }

  private Integer createRandomDAC() {
    return dacDAO.createDac("Test_" + randomAlphabetic(20), "Test_" + randomAlphanumeric(20),
        new Date());
  }

}
