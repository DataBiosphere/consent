package org.broadinstitute.consent.http.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleState;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleServiceTest {

  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock
  private DatasetDAO datasetDAO;
  @Mock
  private ElectionDAO electionDAO;
  @Mock
  private VoteDAO voteDAO;
  @Mock
  private VoteServiceDAO voteServiceDAO;
  @Mock
  private User user;

  @Mock
  private DACAutomationRuleDAO mockRuleDAO;

  @Mock
  private EmailService emailService;

  @Mock
  private UseRestrictionConverter useRestrictionConverter;

  private DACAutomationRuleService service;

  @BeforeEach
  public void setUp() {
    service =
        new DACAutomationRuleService(
            dataAccessRequestDAO,
            datasetDAO,
            mockRuleDAO,
            electionDAO,
            voteDAO,
            voteServiceDAO,
            emailService,
            useRestrictionConverter
        );
  }

  @Test
  void testFindAll() {
    when(mockRuleDAO.findAll()).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null, null)));

    List<DACAutomationRule> rules = service.findAll();
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testFindById() {
    when(mockRuleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null, null)));
    List<DACAutomationRule> rules = service.findAllByDacId(1);
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testToggleRuleFromOffToOn() {
    when(mockRuleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null, null)));
    when(mockRuleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(
        1);
    AutomationRuleToggleResponse result = service.toggleRule(
        1, 1, user);
    Assertions.assertTrue(result.isRuleEnabled());
    Assertions.assertEquals(1, (int) result.getRuleId());
    Assertions.assertTrue(result.getEnabledTime() > 1);
  }

  @Test
  void testToggleRuleFromOnToOff() {
    when(mockRuleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            Timestamp.from(Instant.now()), 1, "alice", "alice@fake.org")));
    doNothing().when(mockRuleDAO).auditedDeleteDACRuleSetting(anyInt(), anyInt(), anyInt());
    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);
    Assertions.assertFalse(result.isRuleEnabled());
    Assertions.assertEquals(1, (int) result.getRuleId());
    Assertions.assertEquals(-1, result.getEnabledTime());
  }

  @Test
  void testRemoveChairpersonFromDAC() {
    when(mockRuleDAO.auditedDeleteDACRuleSettingByUser(1, 1, 1)).thenReturn(1);
    Integer countRemoved = service.removeChairpersonFromDAC(1, 1, 1);
    Assertions.assertEquals(1, countRemoved);
  }

  @Test
  void testAuditedRemoveChairpersonFromDAC() {
    when(mockRuleDAO.auditedDeleteDACRuleSettingByUser(1, 1, 2)).thenReturn(1);
    Integer countRemoved = service.auditedRemoveChairpersonFromDAC(1, 1, 2);
    Assertions.assertEquals(1, countRemoved);
  }

  @Test
  void testRemoveChairpersonUser() {
    when(mockRuleDAO.auditedDeleteAllDACRuleSettingForUser(1, 1)).thenReturn(2);
    Integer count = service.removeChairpersonUser(1, 1);

  }


}
