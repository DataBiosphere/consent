package org.broadinstitute.consent.http.db;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
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
    Integer settingId = dacAutomationRuleDAO.insertDACRuleSetting(dacId, rules.get(0).id(),
        user.getUserId());
    Assertions.assertNotNull(settingId);
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
    Integer settingId = dacAutomationRuleDAO.insertDACRuleSetting(dacId, rulesByDacId.get(0).id(),
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
    Integer settingId = dacAutomationRuleDAO.insertDACRuleSetting(dacId, rulesByDacId.get(0).id(),
        user.getUserId());
    Assertions.assertNotNull(settingId);
    dacAutomationRuleDAO.deleteDACRuleSetting(dacId, rulesByDacId.get(0).id());
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
  }

  @Test
  void testDeleteDACRuleSettingByUserId() {
    User user = createUser();
    Integer dacId = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    dacAutomationRuleDAO.insertDACRuleSetting(dacId, rulesByDacId.get(0).id(), user.getUserId());
    Integer deletedCount = dacAutomationRuleDAO.deleteDACRuleSettingByUser(dacId, user.getUserId());
    Assertions.assertEquals(1, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
  }

  @Test
  void testDeleteDACRuleSetting() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.insertDACRuleSetting(dacId1, rulesByDacId.get(0).id(), user.getUserId());
    dacAutomationRuleDAO.insertDACRuleSetting(dacId2, rulesByDacId.get(0).id(), user.getUserId());
    Integer deletedCount = dacAutomationRuleDAO.deleteDACRuleSettingByUser(dacId1, user.getUserId());
    Assertions.assertEquals(1, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId1);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId2);
    updatedRulesByDacId.forEach(r -> Assertions.assertNotNull(r.enabledByUserId()));
  }

  @Test
  void testDeleteAllSettingsByUserId() {
    User user = createUser();
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<DACAutomationRule> rulesByDacId = dacAutomationRuleDAO.findAll();
    dacAutomationRuleDAO.insertDACRuleSetting(dacId1, rulesByDacId.get(0).id(), user.getUserId());
    dacAutomationRuleDAO.insertDACRuleSetting(dacId2, rulesByDacId.get(0).id(), user.getUserId());
    Integer deletedCount = dacAutomationRuleDAO.deleteAllDACRuleSettingForUser(user.getUserId());
    Assertions.assertEquals(2, deletedCount);
    List<DACAutomationRule> updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId1);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
    updatedRulesByDacId = dacAutomationRuleDAO.findAllDACAutomationRulesByDACId(
        dacId2);
    updatedRulesByDacId.forEach(r -> Assertions.assertNull(r.enabledByUserId()));
  }

  private Integer createRandomDAC() {
    return dacDAO.createDac("Test_" + randomAlphabetic(20), "Test_" + randomAlphanumeric(20),
        new Date());
  }

}
