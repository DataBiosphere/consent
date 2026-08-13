package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.MoreExecutors;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.Collaborator;
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
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.transaction.TransactionalCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACAutomationRuleServiceTest extends AbstractTestHelper {

  @Mock private Jdbi jdbi;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock private DatasetDAO datasetDAO;
  @Mock private ElectionDAO electionDAO;
  @Mock private VoteDAO voteDAO;
  @Mock private VoteServiceDAO voteServiceDAO;
  @Mock private User user;

  @Mock private DACAutomationRuleDAO ruleDAO;

  @Mock private UserDAO userDAO;

  @Mock private VoteService voteService;

  @Mock private ElasticSearchService elasticSearchService;

  @Mock private ContainerRequest request;

  private DACAutomationRuleService service;

  private ExecutorService executorService;

  private static DACAutomationRule makeDacAutomationRuleGRU() {
    return new DACAutomationRule(
        1,
        DACAutomationRuleType.GRU_V1,
        "Test Rule",
        RuleState.AVAILABLE,
        FIXED_TIMESTAMP,
        1,
        "admin",
        "admin@example.com");
  }

  private static DACAutomationRule makeDacAutomationRuleHMB() {
    return new DACAutomationRule(
        2,
        DACAutomationRuleType.HMB_V1,
        "Test HMB Rule",
        RuleState.AVAILABLE,
        FIXED_TIMESTAMP,
        1,
        "admin",
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

  private static List<Dataset> datasetsWithIds(int... datasetIds) {
    return Arrays.stream(datasetIds).mapToObj(id -> makeDataset(id, "Dataset " + id, 1)).toList();
  }

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(dataAccessRequestDAO);
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(datasetDAO);
    when(jdbi.onDemand(DACAutomationRuleDAO.class)).thenReturn(ruleDAO);
    when(jdbi.onDemand(ElectionDAO.class)).thenReturn(electionDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDAO);
    when(jdbi.onDemand(VoteDAO.class)).thenReturn(voteDAO);
    // Direct executor so the best-effort reindex runs inline and these tests can assert on it
    executorService = MoreExecutors.newDirectExecutorService();
    service =
        new DACAutomationRuleService(
            jdbi, voteServiceDAO, voteService, elasticSearchService, executorService);
  }

  @AfterEach
  void tearDown() {
    executorService.shutdownNow();
  }

  @Test
  void testFindAll() {
    when(ruleDAO.findAll())
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));

    List<DACAutomationRule> rules = service.findAll();
    Assertions.assertNotNull(rules);
    assertFalse(rules.isEmpty());
  }

  @Test
  void testFindById() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    List<DACAutomationRule> rules = service.findAllByDacId(1);
    Assertions.assertNotNull(rules);
    assertFalse(rules.isEmpty());
  }

  @Test
  void testToggleRuleFromOffToOn() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);
    assertTrue(result.isRuleEnabled());
    assertEquals(1, result.getRuleId());
    Assertions.assertTrue(result.getEnabledTime() > 1);
  }

  @Test
  void testToggleRuleFromOnToOff() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    FIXED_TIMESTAMP,
                    1,
                    "alice",
                    "alice@fake.org")));
    doNothing().when(ruleDAO).auditedDeleteDACRuleSetting(anyInt(), anyInt(), anyInt());
    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);
    assertFalse(result.isRuleEnabled());
    assertEquals(1, result.getRuleId());
    assertEquals(-1, result.getEnabledTime());
  }

  @Test
  void testToggleSoApprovalRuleReindexesTheDacsDatasets() throws Exception {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
    when(datasetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(datasetsWithIds(10, 11));
    when(elasticSearchService.indexDatasets(List.of(10, 11))).thenReturn(Response.ok().build());

    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);

    assertTrue(result.isRuleEnabled());
    verify(elasticSearchService).indexDatasets(List.of(10, 11));
    // The rest of the corpus is external entries with no DAC, which no rule change can affect
    verify(datasetDAO, never()).findAllDatasetIds();
  }

  @Test
  void testToggleAutoApprovalRuleAlsoReindexes() throws Exception {
    // Auto-approve rules drive instantApprovalEligible, so their toggles stale documents too
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
    when(datasetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(datasetsWithIds(10, 11));
    when(elasticSearchService.indexDatasets(List.of(10, 11))).thenReturn(Response.ok().build());

    service.toggleRule(1, 1, user);

    verify(elasticSearchService).indexDatasets(List.of(10, 11));
  }

  @Test
  void testToggleRuleSkipsReindexWhenThereAreNoDatasets() throws Exception {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
    when(datasetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(List.of());

    service.toggleRule(1, 1, user);

    verify(elasticSearchService, never()).indexDatasets(any());
  }

  @Test
  void testTogglesDuringAnInFlightReindexCoalesceIntoOneFollowUpPass() throws Exception {
    // A real executor, so the reindex genuinely runs off-thread and can be held mid-flight
    ExecutorService realExecutor = Executors.newVirtualThreadPerTaskExecutor();
    service =
        new DACAutomationRuleService(
            jdbi, voteServiceDAO, voteService, elasticSearchService, realExecutor);
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(datasetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(datasetsWithIds(10, 11));
    CountDownLatch firstPassStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstPass = new CountDownLatch(1);
    AtomicInteger passes = new AtomicInteger();
    when(elasticSearchService.indexDatasets(any()))
        .thenAnswer(
            invocation -> {
              if (passes.incrementAndGet() == 1) {
                firstPassStarted.countDown();
                releaseFirstPass.await(5, TimeUnit.SECONDS);
              }
              return Response.ok().build();
            });

    try {
      service.toggleRule(1, 1, user);
      assertTrue(firstPassStarted.await(5, TimeUnit.SECONDS));
      // Both land while the first pass is still running, so they share a single follow-up pass
      service.toggleRule(1, 1, user);
      service.toggleRule(1, 1, user);
      releaseFirstPass.countDown();

      // Waits for the follow-up pass rather than assuming a fixed window is long enough for it
      verify(elasticSearchService, timeout(5000).times(2)).indexDatasets(List.of(10, 11));
      // Then holds to catch a third: the two toggles must share one pass, not get one each
      verify(elasticSearchService, after(500).times(2)).indexDatasets(List.of(10, 11));
      assertEquals(2, passes.get());
    } finally {
      releaseFirstPass.countDown();
      realExecutor.shutdownNow();
    }
  }

  @Test
  void testQueuedDacsAreReindexedInTheOrderTheyWereToggled() throws Exception {
    ExecutorService realExecutor = Executors.newVirtualThreadPerTaskExecutor();
    service =
        new DACAutomationRuleService(
            jdbi, voteServiceDAO, voteService, elasticSearchService, realExecutor);
    List<Integer> dacIds = List.of(7, 8, 9);
    dacIds.forEach(
        dacId -> {
          when(ruleDAO.findAllDACAutomationRulesByDACId(dacId))
              .thenReturn(
                  List.of(
                      new DACAutomationRule(
                          1,
                          DACAutomationRuleType.GRU_V1,
                          "Test Rule",
                          RuleState.AVAILABLE,
                          null,
                          null,
                          null,
                          null)));
          when(datasetDAO.findDatasetsAssociatedWithDac(dacId))
              .thenReturn(List.of(makeDataset(dacId * 10, "Dataset", dacId)));
        });
    CountDownLatch firstPassStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstPass = new CountDownLatch(1);
    List<List<Integer>> indexedInOrder = Collections.synchronizedList(new ArrayList<>());
    when(elasticSearchService.indexDatasets(any()))
        .thenAnswer(
            invocation -> {
              List<Integer> ids = invocation.getArgument(0);
              indexedInOrder.add(ids);
              if (indexedInOrder.size() == 1) {
                firstPassStarted.countDown();
                releaseFirstPass.await(5, TimeUnit.SECONDS);
              }
              return Response.ok().build();
            });

    try {
      // The first toggle holds the drain, so the other two queue behind it while it runs
      service.toggleRule(7, 1, user);
      assertTrue(firstPassStarted.await(5, TimeUnit.SECONDS));
      service.toggleRule(8, 1, user);
      service.toggleRule(9, 1, user);
      releaseFirstPass.countDown();

      verify(elasticSearchService, timeout(5000).times(3)).indexDatasets(any());
      assertEquals(List.of(List.of(70), List.of(80), List.of(90)), indexedInOrder);
    } finally {
      releaseFirstPass.countDown();
      realExecutor.shutdownNow();
    }
  }

  @Test
  void testToggleRuleSurvivesARejectedReindexAndStaysSchedulable() throws Exception {
    // An executor shutting down rejects the submission. The toggle is already committed and
    // audited by then, so it must not fail — and reindexRunning must be released, or the
    // coalescing guard would swallow every later reindex for the life of the process.
    ExecutorService rejecting = mock(ExecutorService.class);
    when(rejecting.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException("down"));
    service =
        new DACAutomationRuleService(
            jdbi, voteServiceDAO, voteService, elasticSearchService, rejecting);
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(1);

    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);
    assertTrue(result.isRuleEnabled());

    // A second toggle must still attempt to schedule rather than coalescing into a dead pass
    service.toggleRule(1, 1, user);
    verify(rejecting, times(2)).submit(any(Runnable.class));
  }

  @Test
  void testToggleRuleStaysSchedulableAfterAnErrorEscapesAReindexPass() throws Exception {
    // An Error escapes the best-effort catch, which only handles Exception. The guard must still be
    // released, or every later reindex would coalesce into a pass that never runs.
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    when(ruleDAO.auditedInsertDACRuleSetting(anyInt(), anyInt(), anyInt(), any())).thenReturn(1);
    when(datasetDAO.findDatasetsAssociatedWithDac(1))
        .thenThrow(new StackOverflowError("boom"))
        .thenReturn(datasetsWithIds(10));
    when(elasticSearchService.indexDatasets(List.of(10))).thenReturn(Response.ok().build());

    service.toggleRule(1, 1, user);
    service.toggleRule(1, 1, user);

    verify(elasticSearchService).indexDatasets(List.of(10));
  }

  @Test
  void testToggleRuleSucceedsWhenReindexFails() throws Exception {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    FIXED_TIMESTAMP,
                    1,
                    "alice",
                    "alice@fake.org")));
    doNothing().when(ruleDAO).auditedDeleteDACRuleSetting(anyInt(), anyInt(), anyInt());
    when(datasetDAO.findDatasetsAssociatedWithDac(1)).thenReturn(datasetsWithIds(10));
    when(elasticSearchService.indexDatasets(List.of(10))).thenThrow(new IOException("es down"));

    // The rule change is already committed and audited, so a failed reindex must not surface
    AutomationRuleToggleResponse result = service.toggleRule(1, 1, user);

    assertFalse(result.isRuleEnabled());
  }

  @Test
  void testToggleRuleFromOnToOffInvalidRuleNumber() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.GRU_V1,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    FIXED_TIMESTAMP,
                    1,
                    "alice",
                    "alice@fake.org")));
    assertThrows(UnprocessableEntityException.class, () -> service.toggleRule(1, 666, user));
  }

  @Test
  void testToggleRuleRequireSORuleToOnWithAutoOpenOn() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    FIXED_TIMESTAMP,
                    1,
                    "alice",
                    "alice@fake.org"),
                new DACAutomationRule(
                    2,
                    DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    assertDoesNotThrow(() -> service.toggleRule(1, 2, user));
  }

  @Test
  void testToggleRuleRAutoOpenOnWithSORuleOff() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null),
                new DACAutomationRule(
                    2,
                    DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    assertDoesNotThrow(() -> service.toggleRule(1, 1, user));
  }

  @Test
  void testToggleRuleSORuleOnWithAutoOpenOff() {
    when(ruleDAO.findAllDACAutomationRulesByDACId(1))
        .thenReturn(
            List.of(
                new DACAutomationRule(
                    1,
                    DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null),
                new DACAutomationRule(
                    2,
                    DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
                    "Test Rule",
                    RuleState.AVAILABLE,
                    null,
                    null,
                    null,
                    null)));
    assertDoesNotThrow(() -> service.toggleRule(1, 2, user));
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

    List<DACAutomationRuleAudit> mockAudits =
        List.of(
            new DACAutomationRuleAudit(
                RuleAuditAction.ADD,
                FIXED_TIMESTAMP,
                DACAutomationRuleType.GRU_V1,
                "user1@example.com",
                "User One"),
            new DACAutomationRuleAudit(
                RuleAuditAction.REMOVE,
                FIXED_TIMESTAMP,
                DACAutomationRuleType.HMB_DSV1,
                "user2@example.com",
                "User Two"));

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
    DACAutomationRule inactiveRule =
        new DACAutomationRule(
            2,
            DACAutomationRuleType.GRU_V1,
            "Inactive Rule",
            RuleState.AVAILABLE,
            null,
            null,
            null,
            null);

    DACAutomationRule soRequiredRule =
        new DACAutomationRule(
            3,
            DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
            "SO Required Rule",
            RuleState.AVAILABLE,
            FIXED_TIMESTAMP,
            null,
            "SO Rule",
            activeRule.userEmail());

    when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset1);
    when(datasetDAO.findDatasetById(2)).thenReturn(dataset2);
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset1.getDacId()))
        .thenReturn(List.of(activeRule, soRequiredRule));
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset2.getDacId()))
        .thenReturn(List.of(inactiveRule));

    DACAutomationRuleService serviceSpy = spy(service);
    // in order to test sending the email we need to add to the datasetsAuthorized list
    doAnswer(_ -> Optional.of(new Vote()))
        .when(serviceSpy)
        .applyRule(activeRule, dataset1, dar, request);

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
    DACAutomationRule inactiveRule =
        new DACAutomationRule(
            1,
            DACAutomationRuleType.GRU_V1,
            "Inactive Rule",
            RuleState.AVAILABLE,
            null,
            null,
            null,
            null);

    when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset);
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset.getDacId()))
        .thenReturn(List.of(inactiveRule));

    DACAutomationRuleService serviceSpy = spy(service);

    serviceSpy.triggerDACRuleSettings(researcher, datasetIds, referenceId, request);

    verify(serviceSpy, never()).applyRule(any(), any(), any(), any());
  }

  @Test
  void testTriggerDACRuleSettingsSOApprovalRequiredToRunRulesWithApproval() {
    User researcher = makeResearcher();
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setEmail("1" + researcher.getEmail());
    signingOfficial.setUserId(9);
    DataAccessRequest dar = makeDAR();

    String referenceId = dar.getReferenceId();
    List<Integer> datasetIds = List.of(1, 2);
    Dataset dataset1 = makeDataset(1, "Dataset One", 3);
    DACAutomationRule activeRule = makeDacAutomationRuleGRU(); // This has enabledByUserId=1
    DACAutomationRule soRequiredRule =
        new DACAutomationRule(
            3,
            DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL,
            "SO Required Rule",
            RuleState.AVAILABLE,
            FIXED_TIMESTAMP,
            1,
            "SO Rule",
            activeRule.userEmail());

    when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset1);
    when(ruleDAO.findAllDACAutomationRulesByDACId(dataset1.getDacId()))
        .thenReturn(List.of(activeRule, soRequiredRule));

    DACAutomationRuleService serviceSpy = spy(service);
    // in order to test sending the email we need to add to the datasetsAuthorized list
    doAnswer(_ -> Optional.of(new Vote()))
        .when(serviceSpy)
        .applyRule(activeRule, dataset1, dar, request);

    serviceSpy.triggerDACRuleSettings(signingOfficial, datasetIds, referenceId, request);

    verify(serviceSpy).applyRule(eq(activeRule), any(), any(), any());
  }

  @Test
  void testTriggerDACRuleSettings_does_not_throw() {
    User researcher = makeResearcher();
    DataAccessRequest dar = makeDAR();
    String referenceId = dar.getReferenceId();
    List<Integer> datasetIds = List.of(1);

    doThrow(new RuntimeException("Test exception"))
        .when(dataAccessRequestDAO)
        .findByReferenceId(referenceId);

    assertDoesNotThrow(
        () -> service.triggerDACRuleSettings(researcher, datasetIds, referenceId, request));
  }

  @Test
  void testOpenElectionAndApprove() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    RuleImplementationInterface ruleImplementation = new GeneralResearchUseV1();
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();

    Vote vote = new Vote();
    vote.setVoteId(5);
    vote.setVote(true);

    // Mocking the transaction to return the vote directly
    when(electionDAO.inTransaction(any())).thenReturn(vote);

    // Mocking only the methods called outside the transaction
    user.setEraCommonsId("eraCommonsId");
    when(userDAO.findUserById(rule.enabledByUserId())).thenReturn(user);
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(vote));

    Vote openedVote =
        service.openElectionAndApprove(rule, ruleImplementation, dar, dataset, request);

    assertEquals(vote, openedVote);
    verify(voteServiceDAO)
        .updateVotesWithValue(
            List.of(vote), true, "Rule Automated DAR (RADAR) Approval using rule: GRU_V1");
  }

  @Test
  void testOpenElectionAndApproveException() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    RuleImplementationInterface ruleImplementation = new GeneralResearchUseV1();
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();

    Vote vote = new Vote();
    vote.setVoteId(5);
    vote.setVote(true);

    // Mocking the transaction to return the vote directly
    when(electionDAO.inTransaction(any())).thenReturn(vote);

    // Mocking the exception when updating votes
    doThrow(new RuntimeException("Test error"))
        .when(voteServiceDAO)
        .updateVotesWithValue(
            List.of(vote), true, "Rule Automated DAR (RADAR) Approval using rule: GRU_V1");

    assertNull(service.openElectionAndApprove(rule, ruleImplementation, dar, dataset, request));
  }

  @Test
  void testApplyRuleApprove() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darHmb = makeDAR();
    DataAccessRequestData darData = darHmb.getData();
    Collaborator bannedActor =
        new Collaborator(true, "test", "test", "test", "test", "123", "United States of America");
    darData.setInternalCollaborators(List.of(bannedActor));
    darHmb.setData(darData);
    Vote vote = new Vote();
    vote.setVoteId(1);
    vote.setType(VoteType.RADAR_APPROVE.getValue());
    vote.setVote(true);

    // Mocking the transaction to return the vote directly
    when(electionDAO.inTransaction(any())).thenReturn(vote);

    // Mocking only the methods that are called outside the transaction
    when(userDAO.findUserById(rule.enabledByUserId())).thenReturn(user);
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(vote));

    Optional<Vote> appliedVote = service.applyRule(rule, datasetGru, darHmb, request);
    assertTrue(appliedVote.isPresent());
    assertEquals(vote, appliedVote.get());
  }

  @Test
  void testApplyRuleApprove_Banned_CFR_Country_Fails() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darHmb = makeDAR();
    DataAccessRequestData darData = darHmb.getData();
    Collaborator bannedActor =
        new Collaborator(true, "test", "test", "test", "test", "123", "russia");
    darData.setInternalCollaborators(List.of(bannedActor));
    darHmb.setData(darData);

    Optional<Vote> appliedVote = service.applyRule(rule, datasetGru, darHmb, request);
    assertFalse(appliedVote.isPresent());
  }

  @Test
  void testApplyRuleApprove_Banned_ISO_Country_Fails() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darHmb = makeDAR();
    DataAccessRequestData darData = darHmb.getData();
    Collaborator bannedActor =
        new Collaborator(true, "test", "test", "test", "test", "123", "russian federation (the)");
    darData.setInternalCollaborators(List.of(bannedActor));
    darHmb.setData(darData);

    Optional<Vote> appliedVote = service.applyRule(rule, datasetGru, darHmb, request);
    assertFalse(appliedVote.isPresent());
  }

  @Test
  void testApplyRule_Error_In_Vote() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    Dataset datasetGru = makeDataset();
    DataAccessRequest darHmb = makeDAR();

    DACAutomationRuleService serviceSpy = spy(service);
    doReturn(null).when(serviceSpy).openElectionAndApprove(any(), any(), any(), any(), any());

    Optional<Vote> appliedVote = serviceSpy.applyRule(rule, datasetGru, darHmb, request);
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

    verify(serviceSpy, never())
        .openElectionAndApprove(
            any(DACAutomationRule.class),
            any(RuleImplementationInterface.class),
            any(DataAccessRequest.class),
            any(Dataset.class),
            any(ContainerRequest.class));
  }

  @Test
  void testApplyRuleDoesNotApproveDatasetWithMultiplePrimaryDataUses() {
    Dataset dataset = makeDataset();
    dataset.getDataUse().setHmbResearch(true);
    DataAccessRequest dar = makeDAR();

    DACAutomationRuleService serviceSpy = spy(service);
    List.of(makeDacAutomationRuleGRU(), makeDacAutomationRuleHMB())
        .forEach(rule -> assertTrue(serviceSpy.applyRule(rule, dataset, dar, request).isEmpty()));
    verify(serviceSpy, never()).openElectionAndApprove(any(), any(), any(), any(), any());
  }

  @Test
  void testApplyRuleDoesNotApproveOtherOnlyPrimaryDatasetShape() {
    DACAutomationRule rule = makeDacAutomationRuleHMB();
    Dataset dataset = makeDataset();
    dataset.getDataUse().setGeneralUse(false);
    dataset.getDataUse().setOther("sensitive free text");
    DataAccessRequest dar = makeDAR();

    DACAutomationRuleService serviceSpy = spy(service);
    Optional<Vote> appliedVote = serviceSpy.applyRule(rule, dataset, dar, request);

    assertTrue(appliedVote.isEmpty());
    verify(serviceSpy, never()).openElectionAndApprove(any(), any(), any(), any(), any());
  }

  @Test
  void testApplyRuleDoesNotApproveObservedHmbOtherDatasetShape() {
    DACAutomationRule rule = makeDacAutomationRuleHMB();
    Dataset dataset = makeDataset();
    dataset.getDataUse().setGeneralUse(false);
    dataset.getDataUse().setHmbResearch(true);
    dataset.getDataUse().setOther("sensitive free text");
    DataAccessRequest dar = makeDAR();

    DACAutomationRuleService serviceSpy = spy(service);
    Optional<Vote> appliedVote = serviceSpy.applyRule(rule, dataset, dar, request);

    assertTrue(appliedVote.isEmpty());
    verify(serviceSpy, never()).openElectionAndApprove(any(), any(), any(), any(), any());
  }

  @Test
  void testOpenElectionAndApprove_transactionLambdaCreatesElectionAndVote() {
    DACAutomationRule rule = makeDacAutomationRuleGRU();
    RuleImplementationInterface ruleImplementation = new GeneralResearchUseV1();
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();

    int expectedElectionId = 10;
    int expectedVoteId = 20;
    Vote expectedVote = new Vote();
    expectedVote.setVoteId(expectedVoteId);

    DACAutomationRuleService serviceSpy = spy(service);
    doReturn(expectedElectionId).when(serviceSpy).createOpenElectionForDAR(dar, dataset);
    doReturn(expectedVoteId)
        .when(serviceSpy)
        .createVoteForElection(expectedElectionId, rule.enabledByUserId(), VoteType.RADAR_APPROVE);
    when(voteDAO.findVoteById(expectedVoteId)).thenReturn(expectedVote);

    // Make inTransaction actually execute its lambda body
    doAnswer(
            invocation -> {
              TransactionalCallback<Vote, ElectionDAO, Exception> cb = invocation.getArgument(0);
              return cb.inTransaction(electionDAO);
            })
        .when(electionDAO)
        .inTransaction(any());

    // Stub the rest of the method so it completes successfully
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any()))
        .thenReturn(List.of(expectedVote));
    when(userDAO.findUserById(rule.enabledByUserId())).thenReturn(user);

    Vote result =
        serviceSpy.openElectionAndApprove(rule, ruleImplementation, dar, dataset, request);

    assertEquals(expectedVote, result);
    verify(serviceSpy).createOpenElectionForDAR(dar, dataset);
    verify(serviceSpy)
        .createVoteForElection(expectedElectionId, rule.enabledByUserId(), VoteType.RADAR_APPROVE);
    verify(voteDAO).findVoteById(expectedVoteId);
  }

  @Test
  void testGetRuleImplementation() {
    DACAutomationRule gruRule = makeDacAutomationRuleGRU();

    RuleImplementationInterface implementation =
        DACAutomationRuleService.getRuleImplementation(gruRule);

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

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> DACAutomationRuleService.getRuleImplementation(mockRule));
    assertEquals("No rule implementation found for type: MOCK_TYPE", exception.getMessage());
  }

  // createOpenElectionForDAR tests

  @Test
  void testCreateOpenElectionForDAR_returnsElectionId() {
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();
    when(electionDAO.insertElection(
            eq(ElectionType.DATA_ACCESS.getValue()),
            eq(ElectionStatus.OPEN.getValue()),
            any(Date.class),
            eq(dar.getReferenceId()),
            eq(dataset.getDatasetId())))
        .thenReturn(42);

    int result = service.createOpenElectionForDAR(dar, dataset);

    assertEquals(42, result);
  }

  @Test
  void testCreateOpenElectionForDAR_usesDataAccessTypeAndOpenStatus() {
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
    when(electionDAO.insertElection(any(), any(), any(), any(), any())).thenReturn(1);

    service.createOpenElectionForDAR(dar, dataset);

    verify(electionDAO)
        .insertElection(typeCaptor.capture(), statusCaptor.capture(), any(), any(), any());
    assertEquals(ElectionType.DATA_ACCESS.getValue(), typeCaptor.getValue());
    assertEquals(ElectionStatus.OPEN.getValue(), statusCaptor.getValue());
  }

  @Test
  void testCreateOpenElectionForDAR_passesDarReferenceIdAndDatasetId() {
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset(7, "Custom Dataset", 3);
    ArgumentCaptor<String> referenceIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> datasetIdCaptor = ArgumentCaptor.forClass(Integer.class);
    when(electionDAO.insertElection(any(), any(), any(), any(), any())).thenReturn(1);

    service.createOpenElectionForDAR(dar, dataset);

    verify(electionDAO)
        .insertElection(
            any(), any(), any(), referenceIdCaptor.capture(), datasetIdCaptor.capture());
    assertEquals(dar.getReferenceId(), referenceIdCaptor.getValue());
    assertEquals(dataset.getDatasetId(), datasetIdCaptor.getValue());
  }

  @Test
  void testCreateOpenElectionForDAR_passesNonNullDate() {
    DataAccessRequest dar = makeDAR();
    Dataset dataset = makeDataset();
    ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
    when(electionDAO.insertElection(any(), any(), any(), any(), any())).thenReturn(1);

    service.createOpenElectionForDAR(dar, dataset);

    verify(electionDAO).insertElection(any(), any(), dateCaptor.capture(), any(), any());
    assertNotNull(dateCaptor.getValue());
  }

  // createVoteForElection tests

  @Test
  void testCreateVoteForElection_returnsVoteId() {
    when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(55);

    int result = service.createVoteForElection(10, 20, VoteType.RADAR_APPROVE);

    assertEquals(55, result);
  }

  @Test
  void testCreateVoteForElection_passesArgumentsInCorrectOrder() {
    int electionId = 100;
    int userId = 200;
    ArgumentCaptor<Integer> userIdCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> electionIdCaptor = ArgumentCaptor.forClass(Integer.class);
    when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(1);

    service.createVoteForElection(electionId, userId, VoteType.RADAR_APPROVE);

    // insertVote signature is (userId, electionId, type) — note the swap from
    // createVoteForElection's (electionId, userId, voteType)
    verify(voteDAO).insertVote(userIdCaptor.capture(), electionIdCaptor.capture(), any());
    assertEquals(userId, userIdCaptor.getValue());
    assertEquals(electionId, electionIdCaptor.getValue());
  }

  @Test
  void testCreateVoteForElection_passesVoteTypeValue() {
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(1);

    service.createVoteForElection(1, 1, VoteType.RADAR_APPROVE);

    verify(voteDAO).insertVote(anyInt(), anyInt(), typeCaptor.capture());
    assertEquals(VoteType.RADAR_APPROVE.getValue(), typeCaptor.getValue());
  }

  @Test
  void testCreateVoteForElection_withFinalVoteType() {
    ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
    when(voteDAO.insertVote(anyInt(), anyInt(), any())).thenReturn(1);

    service.createVoteForElection(1, 1, VoteType.FINAL);

    verify(voteDAO).insertVote(anyInt(), anyInt(), typeCaptor.capture());
    assertEquals(VoteType.FINAL.getValue(), typeCaptor.getValue());
  }
}
