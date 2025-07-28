package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.rules.AuditPageResults;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleAudit;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.GeneralResearchUseV1;
import org.broadinstitute.consent.http.rules.RuleAuditAction;
import org.broadinstitute.consent.http.rules.RuleImplementationInterface;
import org.broadinstitute.consent.http.rules.RuleState;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.glassfish.jersey.server.ContainerRequest;
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
  private DACAutomationRuleDAO ruleDAO;

  @Mock
  private UserDAO userDAO;

  @Mock
  private VoteService voteService;

  @Mock
  private ContainerRequest request;

  private DACAutomationRuleService service;

  private static DACAutomationRule makeDacAutomationRuleGRU() {
    return new DACAutomationRule(1, DACAutomationRuleType.GRU_V1,
        "Test Rule", RuleState.AVAILABLE, Timestamp.from(Instant.now()), 1, "admin",
        "admin@example.com");
  }

  private static User makeResearcher() {
    User researcher = new User();
    researcher.setUserId(1);
    researcher.setInstitutionId(1);
    researcher.setDisplayName("Test Researcher");
    return researcher;
  }

  private static DataAccessRequest makeDAR() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setDarCode("DAR-123");
    String darId = UUID.randomUUID().toString();
    dar.setReferenceId(darId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setHmb(true);
    dar.setData(data);
    return dar;
  }

  private static Dataset makeDataset(int id, String name, int dacId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(id);
    dataset.setName(name);
    dataset.setAlias(id);
    DataUse gruDataUse = new DataUse();
    gruDataUse.setGeneralUse(true);
    dataset.setDataUse(gruDataUse);
    dataset.setDacId(dacId);
    return dataset;
  }

  private static Dataset makeDataset() {
    return makeDataset(1, "Test Dataset", 0);
  }

  @BeforeEach
  void setUp() {
    service =
        new DACAutomationRuleService(
            dataAccessRequestDAO,
            datasetDAO,
            ruleDAO,
            electionDAO,
            userDAO,
            voteDAO,
            voteServiceDAO,
            voteService
        );
  }

  @Test
  void testFindAll() {
    when(ruleDAO.findAll()).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null, null)));

    List<DACAutomationRule> rules = service.findAll();
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testFindById() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null, null)));
    List<DACAutomationRule> rules = service.findAllByDacId(1);
    Assertions.assertNotNull(rules);
    Assertions.assertFalse(rules.isEmpty());
  }

  @Test
  void testToggleRuleFromOffToOn() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            null, null, null, null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(
        1);
    AutomationRuleToggleResponse result = service.toggleRule(
        1, 1, user);
    assertTrue(result.isRuleEnabled());
    assertEquals(1, result.getRuleId());
    Assertions.assertTrue(result.getEnabledTime() > 1);
  }

  @Test
  void testToggleRuleFromOnToOff() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1)).thenReturn(List.of(
        new DACAutomationRule(1, DACAutomationRuleType.GRU_V1, "Test Rule", RuleState.AVAILABLE,
            Timestamp.from(Instant.now()), 1, "alice", "alice@fake.org")));
    doNothing().when(ruleDAO).auditedDeleteDACRuleSetting(anyInt(), anyInt(), anyInt());
    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);
    Assertions.assertFalse(result.isRuleEnabled());
    assertEquals(1, result.getRuleId());
    assertEquals(-1, result.getEnabledTime());
  }

  @Test
  void testRemoveChairpersonFromDAC() {
    when(ruleDAO.auditedDeleteDACRuleSettingByUser(1, 1, 1)).thenReturn(1);
    Integer countRemoved = service.removeChairpersonFromDAC(1, 1, 1);
    assertEquals(1, countRemoved);
  }

  @Test
  void testAuditedRemoveChairpersonFromDAC() {
    when(ruleDAO.auditedDeleteDACRuleSettingByUser(1, 1, 2)).thenReturn(1);
    Integer countRemoved = service.auditedRemoveChairpersonFromDAC(1, 1, 2);
    assertEquals(1, countRemoved);
  }

  @Test
  void testRemoveChairpersonUser() {
    when(ruleDAO.auditedDeleteAllDACRuleSettingForUser(1, 1)).thenReturn(2);
    Integer count = service.removeChairpersonUser(1, 1);
    assertEquals(2, count);
  }

  @Test
  void testFindAuditRecords() {
    int dacId = 5;
    int pageSize = 10;
    int page = 2;
    int offset = (page - 1) * pageSize;
    int totalCount = 25;

    List<DACAutomationRuleAudit> mockAudits = List.of(
        new DACAutomationRuleAudit(
            RuleAuditAction.ADD,
            Timestamp.from(Instant.now()),
            DACAutomationRuleType.GRU_V1,
            "user1@example.com",
            "User One"
        ),
        new DACAutomationRuleAudit(
            RuleAuditAction.REMOVE,
            Timestamp.from(Instant.now()),
            DACAutomationRuleType.HMB_DSV1,
            "user2@example.com",
            "User Two"
        )
    );

    when(ruleDAO.findAutomationAuditsForDac(dacId, pageSize, offset)).thenReturn(mockAudits);
    when(ruleDAO.findCountOfAutomationAuditsForDac(dacId)).thenReturn(totalCount);

    AuditPageResults results = service.findAuditRecords(dacId, pageSize, page);

    assertNotNull(results);
    assertEquals(mockAudits, results.getAuditRecords());
    assertEquals(totalCount, results.getTotalRecords());
    assertEquals(pageSize, results.getPageSize());
    assertEquals(page, results.getPage());
  }

  @Test
  void testTriggerDACRuleSettings() {
    User researcher = makeResearcher();
    DataAccessRequest dar = makeDAR();
    String referenceId = dar.getReferenceId();
    List<Integer> datasetIds = List.of(1, 2);
    Dataset dataset1 = makeDataset(1, "Dataset One", 3);
    Dataset dataset2 = makeDataset(2, "Dataset Two", 4);
    DACAutomationRule activeRule = makeDacAutomationRuleGRU(); // This has enabledByUserId=1
    DACAutomationRule inactiveRule = new DACAutomationRule(2, DACAutomationRuleType.GRU_V1,
        "Inactive Rule", RuleState.AVAILABLE, null, null, null, null);

    when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset1);
    when(datasetDAO.findDatasetById(2)).thenReturn(dataset2);
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset1.getDacId())).thenReturn(List.of(activeRule));
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset2.getDacId())).thenReturn(List.of(inactiveRule));

    DACAutomationRuleService serviceSpy = spy(service);
    // in order to test sending the email we need to add to the datasetsAuthorized list
    doAnswer(inv -> Optional.of(new Vote())).when(serviceSpy).applyRule(activeRule, dataset1, dar, request);

    serviceSpy.triggerDACRuleSettings(researcher, datasetIds, referenceId, request);

    verify(serviceSpy, never()).applyRule(eq(inactiveRule), any(), any(), any());
  }

  @Test
  void testTriggerDACRuleSettingsNoAuthorizedDatasets() {
    User researcher = makeResearcher();
    DataAccessRequest dar = makeDAR();
    String referenceId = dar.getReferenceId();
    List<Integer> datasetIds = List.of(1);
    Dataset dataset = makeDataset();
    DACAutomationRule inactiveRule = new DACAutomationRule(1, DACAutomationRuleType.GRU_V1,
        "Inactive Rule", RuleState.AVAILABLE, null, null, null, null);

    when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset);
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset.getDacId())).thenReturn(List.of(inactiveRule));

    DACAutomationRuleService serviceSpy = spy(service);

    serviceSpy.triggerDACRuleSettings(researcher, datasetIds, referenceId, request);

    verify(serviceSpy, never()).applyRule(any(), any(), any(), any());
  }

  @Test
  void testTriggerDACRuleSettings_does_not_throw(){
    User researcher = makeResearcher();
    DataAccessRequest dar = makeDAR();
    String referenceId = dar.getReferenceId();
    List<Integer> datasetIds = List.of(1);

    doThrow(new RuntimeException("Test exception"))
        .when(dataAccessRequestDAO)
        .findByReferenceId(referenceId);

    assertDoesNotThrow(() -> service.triggerDACRuleSettings(researcher, datasetIds, referenceId, request));
  }

  @Test
  void testOpenElectionAndApprove() throws SQLException {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    RuleImplementationInterface ruleImplementation = new GeneralResearchUseV1();
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();

    Integer electionId = 4;
    mockInsertElection(dar, dataset, electionId);

    Integer voteId = 5;
    when(voteDAO.insertVote(rule.enabledByUserId(), electionId, VoteType.RADAR_APPROVE.getValue()))
        .thenReturn(voteId);
    user.setEraCommonsId("eraCommonsId");
    when(userDAO.findUserById(rule.enabledByUserId())).thenReturn(user);

    Vote vote = mockFindVoteById(voteId);

    Vote openedVote = service.openElectionAndApprove(rule, ruleImplementation, dar, dataset, request);

    assertEquals(vote, openedVote);
    verify(voteServiceDAO).updateVotesWithValue(
          List.of(vote),
          true,
          "Rule Automated DAR (RADAR) Approval using rule: GRU_V1");
  }

  @Test
  void testOpenElectionAndApproveException() throws SQLException {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    RuleImplementationInterface ruleImplementation = new GeneralResearchUseV1();
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();

    Integer electionId = 4;
    mockInsertElection(dar, dataset, electionId);

    Integer voteId = 5;
    when(voteDAO.insertVote(rule.enabledByUserId(), electionId, VoteType.RADAR_APPROVE.getValue()))
        .thenReturn(voteId);

    Vote vote = mockFindVoteById(voteId);

    doThrow(new SQLException("Test error")).when(voteServiceDAO).updateVotesWithValue(
          List.of(vote),
          true,
          "Rule Automated DAR (RADAR) Approval using rule: GRU_V1");
    assertNull(service.openElectionAndApprove(rule, ruleImplementation, dar, dataset, request));
  }

  @Test
  void testApplyRuleApprove() {
    User dacChair = new User();
    dacChair.setEraCommonsId("eraCommonsId");
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darHmb = makeDAR();
    Vote vote = new Vote();
    vote.setType(VoteType.RADAR_APPROVE.getValue());

    when(electionDAO.insertElection(eq(ElectionType.DATA_ACCESS.getValue()),
        eq(ElectionStatus.OPEN.getValue()), any(), eq(darHmb.getReferenceId()), eq(datasetGru.getDatasetId()))).thenReturn(1);

    when(voteDAO.insertVote(rule.enabledByUserId(), 1,
        VoteType.RADAR_APPROVE.getValue())).thenReturn(1);
    when(voteDAO.findVoteById(1)).thenReturn(vote);
    when(userDAO.findUserById(rule.enabledByUserId())).thenReturn(user);
    Optional<Vote> appliedVote = service.applyRule(rule, datasetGru, darHmb, request);
    assertTrue(appliedVote.isPresent());
    assertEquals(vote, appliedVote.get());
  }

  @Test
  void testApplyRule_Error_In_Vote() throws SQLException {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darHmb = makeDAR();
    Vote vote = new Vote();
    vote.setType(VoteType.RADAR_APPROVE.getValue());

    when(electionDAO.insertElection(eq(ElectionType.DATA_ACCESS.getValue()),
        eq(ElectionStatus.OPEN.getValue()), any(), eq(darHmb.getReferenceId()), eq(datasetGru.getDatasetId()))).thenReturn(1);

    when(voteDAO.insertVote(rule.enabledByUserId(), 1,
        VoteType.RADAR_APPROVE.getValue())).thenReturn(1);
    when(voteDAO.findVoteById(1)).thenReturn(vote);

    doThrow(new SQLException("Test error")).when(voteServiceDAO).updateVotesWithValue(any(), anyBoolean(), any());

    Optional<Vote> appliedVote = service.applyRule(rule, datasetGru, darHmb, request);
    assertTrue(appliedVote.isEmpty());
  }

  @Test
  void testApplyRuleNotApprove() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darNotHmb = makeDAR();
    darNotHmb.getData().setHmb(false);

    DACAutomationRuleService serviceSpy = spy(service);
    serviceSpy.applyRule(rule, datasetGru, darNotHmb, request);

    verify(serviceSpy, never()).openElectionAndApprove(
        any(DACAutomationRule.class),
        any(RuleImplementationInterface.class),
        any(DataAccessRequest.class),
        any(Dataset.class),
        any(ContainerRequest.class)
    );
  }

  @Test
  void testGetRuleImplementation() {
    DACAutomationRule gruRule = makeDacAutomationRuleGRU();

    RuleImplementationInterface implementation = DACAutomationRuleService.getRuleImplementation(gruRule);

    assertNotNull(implementation);
    assertEquals(DACAutomationRuleType.GRU_V1, implementation.getRuleType());
    assertEquals(GeneralResearchUseV1.class, implementation.getClass());
  }

  @Test
  void testGetRuleImplementationException() {
    // Create a mock rule with a type that doesn't have an implementation
    DACAutomationRule mockRule = mock(DACAutomationRule.class);
    DACAutomationRuleType mockType = mock(DACAutomationRuleType.class);
    when(mockRule.ruleType()).thenReturn(mockType);
    when(mockType.toString()).thenReturn("MOCK_TYPE");

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> DACAutomationRuleService.getRuleImplementation(mockRule));
    assertEquals(
        "No rule implementation found for type: MOCK_TYPE",
        exception.getMessage());
  }

  private void mockInsertElection(DataAccessRequest dar, Dataset dataset, Integer electionId) {
    when(electionDAO.insertElection(
        eq(ElectionType.DATA_ACCESS.getValue()),
        eq(ElectionStatus.OPEN.getValue()),
        any(Date.class),
        eq(dar.getReferenceId()),
        eq(dataset.getDatasetId())
    )).thenReturn(electionId);
  }

  private Vote mockFindVoteById(Integer voteId) {
    Vote vote = new Vote();
    vote.setVoteId(voteId);
    when(voteDAO.findVoteById(voteId)).thenReturn(vote);
    return vote;
  }

}
