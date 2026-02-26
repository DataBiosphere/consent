package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleAudit;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleAuditAction;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
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
    assertTrue(
        rules.stream().anyMatch(rule -> rule.ruleType().equals(DACAutomationRuleType.GRU_V1)));
  }

  @Test
  void testInsertDACRuleSetting() {
    User user = createUser();
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rules = dacAutomationRuleDAO.findAll();
    Integer settingId =
        dacAutomationRuleDAO.auditedInsertDACRuleSetting(
            dacId, rules.get(0).id(), user.getUserId(), Instant.now());
    Assertions.assertNotNull(settingId);
  }

  @Test
  void testAuditedInsertDACRuleSetting() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dacId1, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());
    List<DACAutomationRuleAudit> auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, 5, 0);
    Assertions.assertNotNull(auditRecords);
    Assertions.assertFalse(auditRecords.isEmpty());
    Assertions.assertEquals(1, auditRecords.size());
    Assertions.assertEquals(RuleAuditAction.ADD, auditRecords.get(0).action());
    Assertions.assertEquals(user.getEmail(), auditRecords.get(0).email());
    Assertions.assertEquals(user.getDisplayName(), auditRecords.get(0).displayName());
    Assertions.assertEquals(1, dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId1));
  }

  @Test
  void testAuditedInsertDACRuleSettingRollback() {
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    List<DACAutomationRuleAudit> auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId, 5, 0);
    Assertions.assertEquals(0, auditRecords.size());

    Integer ruleId = rulesByDacId.get(0).id();
    Instant now = Instant.now();

    // Use -1 userId to force a failure and trigger a rollback
    Assertions.assertThrows(
        UnableToExecuteStatementException.class,
        () -> dacAutomationRuleDAO.auditedInsertDACRuleSetting(dacId, ruleId, -1, now));

    List<DACAutomationRuleAudit> auditRecordsAfter =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId, 5, 0);

    Assertions.assertEquals(0, auditRecordsAfter.size());
  }

  @Test
  void testFindRulesByDacIdAddSetting() {
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId);
    Assertions.assertNotNull(rulesByDacId);
    Assertions.assertFalse(rulesByDacId.isEmpty());
    rulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    User user = createUser();
    Integer settingId =
        dacAutomationRuleDAO.auditedInsertDACRuleSetting(
            dacId, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());
    Assertions.assertNotNull(settingId);
    List<DACAutomationRule> updatedRulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId);
    assertTrue(
        updatedRulesByDacId.stream()
            .anyMatch(r -> Objects.equals(r.enabledByUserId(), user.getUserId())));
    assertTrue(
        updatedRulesByDacId.stream().anyMatch(r -> Objects.equals(r.userEmail(), user.getEmail())));
    assertTrue(
        updatedRulesByDacId.stream()
            .anyMatch(r -> Objects.equals(r.displayName(), user.getDisplayName())));
  }

  @Test
  void testFindRulesByDacIdRemoveSetting() {
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId);
    User user = createUser();
    Integer settingId =
        dacAutomationRuleDAO.auditedInsertDACRuleSetting(
            dacId, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());
    Assertions.assertNotNull(settingId);
    dacAutomationRuleDAO.auditedDeleteDACRuleSetting(
        dacId, rulesByDacId.get(0).id(), user.getUserId());
    List<DACAutomationRule> updatedRulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
  }

  @Test
  void testAuditedDeleteDACRuleSetting() {
    User user = createUser();
    User auditUser = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dacId1, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());

    dacAutomationRuleDAO.auditedDeleteDACRuleSetting(
        dacId1, rulesByDacId.get(0).id(), auditUser.getUserId());
    List<DACAutomationRuleAudit> auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, 5, 0);
    Assertions.assertEquals(2, auditRecords.size());
    Assertions.assertEquals(
        auditRecords.size(), dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId1));
    var remove =
        auditRecords.stream().filter(r -> r.action().equals(RuleAuditAction.REMOVE)).findFirst();
    assertTrue(remove.isPresent());
    assertEquals(remove.get().email(), auditUser.getEmail());
    var add = auditRecords.stream().filter(r -> r.action().equals(RuleAuditAction.ADD)).findFirst();
    assertTrue(add.isPresent());
    assertEquals(add.get().email(), user.getEmail());
  }

  @Test
  void testDeleteDACRuleSettingByUserId() {
    User user = createUser();
    User auditUser = createUser();
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId);
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dacId, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());
    Integer deletedCount =
        dacAutomationRuleDAO.auditedDeleteDACRuleSettingByUser(
            dacId, user.getUserId(), auditUser.getUserId());
    Assertions.assertEquals(1, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    List<DACAutomationRuleAudit> auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId, 5, 0);
    Assertions.assertEquals(2, auditRecords.size());
    var remove =
        auditRecords.stream().filter(r -> r.action().equals(RuleAuditAction.REMOVE)).findFirst();
    assertTrue(remove.isPresent());
    assertEquals(remove.get().email(), auditUser.getEmail());
  }

  @Test
  void testDeleteDACRuleSetting() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    rulesByDacId.forEach(
        r -> {
          dacAutomationRuleDAO.auditedInsertDACRuleSetting(
              dacId1, r.id(), user.getUserId(), Instant.now());
          dacAutomationRuleDAO.auditedInsertDACRuleSetting(
              dacId2, r.id(), user.getUserId(), Instant.now());
        });
    Integer deletedCount =
        dacAutomationRuleDAO.auditedDeleteDACRuleSettingByUser(
            dacId1, user.getUserId(), user.getUserId());
    Assertions.assertEquals(rulesByDacId.size(), deletedCount);
    Assertions.assertEquals(
        rulesByDacId.size() * 2, dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId1));
    Assertions.assertEquals(
        rulesByDacId.size(), dacAutomationRuleDAO.findCountOfAutomationAuditsForDac(dacId2));
    List<DACAutomationRule> updatedRulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId1);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId2);
    updatedRulesByDacId.forEach(r -> Assertions.assertNotNull(r.enabledByUserId()));
  }

  @Test
  void testAuditedDeleteDACRuleSettingByUser() {
    User user = createUser();
    User auditUser = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    rulesByDacId.forEach(
        r ->
            dacAutomationRuleDAO.auditedInsertDACRuleSetting(
                dacId1, r.id(), user.getUserId(), Instant.now()));
    Integer deletedCount =
        dacAutomationRuleDAO.auditedDeleteDACRuleSettingByUser(
            dacId1, user.getUserId(), auditUser.getUserId());
    Assertions.assertEquals(rulesByDacId.size(), deletedCount);
    List<DACAutomationRuleAudit> deleteByUserAudits =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, rulesByDacId.size(), 0);
    Assertions.assertEquals(rulesByDacId.size(), deleteByUserAudits.size());
    deleteByUserAudits.forEach(r -> Assertions.assertEquals(RuleAuditAction.REMOVE, r.action()));
  }

  @Test
  void testRuleFindAllOrderResponse() {
    List<DACAutomationRule> rulesByDacIdDBResponse = dacAutomationRuleDAO.findAll();
    List<DACAutomationRule> sortedRules =
        rulesByDacIdDBResponse.stream()
            .sorted(Comparator.comparingInt(DACAutomationRule::id))
            .toList();
    assertEquals(rulesByDacIdDBResponse, sortedRules);
  }

  @Test
  void testFindAllByDACIdRuleOrderResponse() {
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacIdDBResponse =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId1);
    List<DACAutomationRule> sortedRules =
        rulesByDacIdDBResponse.stream()
            .sorted(Comparator.comparingInt(DACAutomationRule::id))
            .toList();
    assertEquals(rulesByDacIdDBResponse, sortedRules);
  }

  @Test
  void testDeleteAllSettingsByUserId() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dacId1, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());
    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dacId2, rulesByDacId.get(0).id(), user.getUserId(), Instant.now());
    Integer deletedCount =
        dacAutomationRuleDAO.auditedDeleteAllDACRuleSettingForUser(
            user.getUserId(), user.getUserId());
    Assertions.assertEquals(2, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId =
        dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId1);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(dacId2);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    List<DACAutomationRuleAudit> auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, rulesByDacId.size(), 0);
    Assertions.assertEquals(2, auditRecords.size());
  }

  @Test
  void testDacAutomationRuleAudit() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    rulesByDacId.forEach(
        r ->
            dacAutomationRuleDAO.auditedInsertDACRuleSetting(
                dacId1, r.id(), user.getUserId(), Instant.now()));
    List<DACAutomationRuleAudit> auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, rulesByDacId.size(), 0);
    Assertions.assertEquals(rulesByDacId.size(), auditRecords.size());
    auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, rulesByDacId.size() - 1, 0);
    Assertions.assertEquals(rulesByDacId.size() - 1, auditRecords.size());
    auditRecords =
        dacAutomationRuleDAO.findAutomationAuditsForDac(dacId1, 1, rulesByDacId.size() * 2);
    assertTrue(auditRecords.isEmpty());
  }

  private Integer createRandomDAC() {
    return dacDAO.createDac(
        "Test_" + randomAlphabetic(20), "Test_" + randomAlphanumeric(20), new Date());
  }
}
