package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest extends AbstractTestHelper {

  private VoteService service;

  @Mock private UserDAO userDAO;
  @Mock private DacDAO dacDAO;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock private DatasetDAO datasetDAO;
  @Mock private ElectionDAO electionDAO;
  @Mock private EmailService emailService;
  @Mock private ElasticSearchService elasticSearchService;
  @Mock private VoteDAO voteDAO;
  @Mock private VoteServiceDAO voteServiceDAO;
  @Mock private User user;
  @Mock private OntologyService ontologyService;

  @BeforeEach
  void initService() {
    service =
        new VoteService(
            userDAO,
            dacDAO,
            dataAccessRequestDAO,
            datasetDAO,
            electionDAO,
            emailService,
            elasticSearchService,
            voteDAO,
            voteServiceDAO,
            ontologyService);
  }

  @Test
  void testUpdateVotesWithValue() {
    List<Vote> votes = service.updateVotesWithValue(List.of(), true, "rationale", user);
    assertNotNull(votes);
    assertTrue(votes.isEmpty());
  }

  @Test
  void testFindVotesByIds() {
    when(voteDAO.findVotesByIds(any())).thenReturn(List.of(new Vote()));

    List<Vote> votes = service.findVotesByIds(List.of(1));
    assertNotNull(votes);
    assertFalse(votes.isEmpty());
  }

  @Test
  void testFindVotesByIds_emptyList() {
    List<Vote> votes = service.findVotesByIds(List.of());
    assertNotNull(votes);
    assertTrue(votes.isEmpty());
  }

  @Test
  void testChairCreateVotesDataAccess() {
    setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
    assertFalse(votes.isEmpty());
    // Should create 4 votes:
    // Chairperson as a chair
    // Chairperson as a dac member
    // Final vote
    // Manual review Agreement vote
    assertEquals(4, votes.size());
  }

  @Test
  void testMemberCreateVotesDataAccess() {
    setUpUserAndElectionVotes(UserRoles.MEMBER);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, false);
    assertFalse(votes.isEmpty());
    // Should create 1 member vote
    assertEquals(1, votes.size());
  }

  @Test
  void testChairCreateVotesDataAccessManualReview() {
    setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.DATA_ACCESS, true);
    assertFalse(votes.isEmpty());
    // Should create 3 votes:
    // Chairperson as a chair
    // Chairperson as a dac member
    // Final vote
    assertEquals(3, votes.size());
  }

  @Test
  void testChairCreateVotesTranslateDUL() {
    setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.TRANSLATE_DUL, false);
    assertFalse(votes.isEmpty());
    // Should create 2 votes:
    // Chairperson as a chair
    // Chairperson as a dac member
    assertEquals(2, votes.size());
  }

  @Test
  void testMemberCreateVotesTranslateDUL() {
    setUpUserAndElectionVotes(UserRoles.MEMBER);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.TRANSLATE_DUL, false);
    assertFalse(votes.isEmpty());
    // Should create 1 member vote
    assertEquals(1, votes.size());
  }

  @Test
  void testChairCreateVotesRP() {
    setUpUserAndElectionVotes(UserRoles.CHAIRPERSON);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.RP, false);
    assertFalse(votes.isEmpty());
    // Should create 2 votes:
    // Chairperson as a chair
    // Chairperson as a dac member
    assertEquals(2, votes.size());
  }

  @Test
  void testMemberCreateVotesRP() {
    setUpUserAndElectionVotes(UserRoles.MEMBER);

    List<Vote> votes = service.createVotes(new Election(), ElectionType.RP, false);
    assertFalse(votes.isEmpty());
    // Should create 1 member vote
    assertEquals(1, votes.size());
  }

  @Test
  void testUpdateVotesWithValue_NoRationale() {
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
    Vote v = setUpTestVote();
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

    Election accessElection = new Election();
    accessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    accessElection.setStatus(ElectionStatus.OPEN.getValue());
    Election rpElection = new Election();
    rpElection.setElectionType(ElectionType.RP.getValue());
    rpElection.setStatus(ElectionStatus.OPEN.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(accessElection, rpElection));

    try {
      service.updateVotesWithValue(List.of(v), true, null, user);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testUpdateVotesWithValue_emptyList() {
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of());

    List<Vote> votes = service.updateVotesWithValue(List.of(), true, "rationale", user);
    assertNotNull(votes);
    assertTrue(votes.isEmpty());
  }

  @Test
  void testUpdateVotesWithValue_ClosedElection() {
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
    Vote v = setUpTestVote();

    Election closedAccessElection = new Election();
    closedAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    closedAccessElection.setStatus(ElectionStatus.CLOSED.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(closedAccessElection));

    List<Vote> voteList = List.of(v);
    assertThrows(
        ConsentConflictException.class,
        () -> service.updateVotesWithValue(voteList, true, "rationale", user));
  }

  @Test
  void testUpdateVotesWithValue_MultipleElectionsDifferentStatuses() {
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
    Vote v = setUpTestVote();

    Election openAccessElection = new Election();
    openAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    openAccessElection.setStatus(ElectionStatus.OPEN.getValue());
    Election closedAccessElection = new Election();
    closedAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    closedAccessElection.setStatus(ElectionStatus.CLOSED.getValue());
    Election canceledAccessElection = new Election();
    canceledAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    canceledAccessElection.setStatus(ElectionStatus.CANCELED.getValue());
    when(electionDAO.findElectionsByIds(any()))
        .thenReturn(List.of(openAccessElection, closedAccessElection, canceledAccessElection));

    List<Vote> voteList = List.of(v);
    assertThrows(
        ConsentConflictException.class,
        () -> service.updateVotesWithValue(voteList, true, "rationale", user));
  }

  @Test
  void testUpdateVotesWithValue_OpenRPElection() {
    testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.OPEN);
  }

  @Test
  void testUpdateVotesWithValue_ClosedRPElection() {
    testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.CLOSED);
  }

  @Test
  void testUpdateVotesWithValue_CanceledRPElection() {
    testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.CANCELED);
  }

  @Test
  void testUpdateVotesWithValue_FinalRPElection() {
    testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.FINAL);
  }

  @Test
  void testUpdateVotesWithValue_PendingApprovalRPElection() {
    testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus.PENDING_APPROVAL);
  }

  @Test
  void testUpdateVotesWithValue_MultipleElectionTypes() {
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
    Vote v = setUpTestVote();
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

    Election accessElection = new Election();
    accessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    accessElection.setStatus(ElectionStatus.OPEN.getValue());
    Election rpElection = new Election();
    rpElection.setElectionType(ElectionType.RP.getValue());
    rpElection.setStatus(ElectionStatus.OPEN.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(accessElection, rpElection));

    try {
      service.updateVotesWithValue(List.of(v), true, "rationale", user);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void testUpdateVotesWithValue_RPElectionWithStatus(ElectionStatus status) {
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());
    Vote v = setUpTestVote();
    when(voteServiceDAO.updateVotesWithValue(any(), anyBoolean(), any())).thenReturn(List.of(v));

    Election rpElection = new Election();
    rpElection.setElectionType(ElectionType.RP.getValue());
    rpElection.setStatus(status.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(rpElection));

    try {
      service.updateVotesWithValue(List.of(v), true, "rationale", user);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testUpdateRationaleByVoteIds() {
    doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());

    try {
      service.updateRationaleByVoteIds(List.of(1), "rationale");
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testUpdateRationaleByVoteIds_DataAccessAndRPElections() {
    doNothing().when(voteDAO).updateRationaleByVoteIds(any(), any());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of());

    Election accessElection = new Election();
    accessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    accessElection.setStatus(ElectionStatus.OPEN.getValue());
    Election rpElection = new Election();
    rpElection.setElectionType(ElectionType.RP.getValue());
    rpElection.setStatus(ElectionStatus.OPEN.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(accessElection, rpElection));

    try {
      service.updateRationaleByVoteIds(List.of(1), "rationale");
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testUpdateRationaleByVoteIds_NonOpenDataAccessElection() {
    Election election = new Election();
    election.setElectionType(ElectionType.DATA_ACCESS.getValue());
    election.setStatus(ElectionStatus.CLOSED.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(election));
    List<Integer> votes = List.of(1);
    assertThrows(
        ConsentConflictException.class, () -> service.updateRationaleByVoteIds(votes, "rationale"));
  }

  @Test
  void testUpdateRationaleByVoteIds_NonDataAccessElection() {
    Election election = new Election();
    election.setElectionType(ElectionType.TRANSLATE_DUL.getValue());
    election.setStatus(ElectionStatus.OPEN.getValue());
    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(election));
    List<Integer> votes = List.of(1);
    assertThrows(
        ConsentConflictException.class, () -> service.updateRationaleByVoteIds(votes, "rationale"));
  }

  @Test
  void testNotifyResearchersOfProgressReportApproval() throws TemplateException, IOException {
    String referenceId1 = UUID.randomUUID().toString();

    Vote v1 = new Vote();
    v1.setVote(true);
    v1.setType(VoteType.FINAL.getValue());
    v1.setElectionId(1);
    v1.setUserId(1);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());

    Election e1 = new Election();
    e1.setElectionId(1);
    e1.setReferenceId(referenceId1);
    e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e1.setDatasetId(1);

    DatasetProperty depositorProp = new DatasetProperty();
    depositorProp.setPropertyName("Data Depositor");
    depositorProp.setPropertyValue("depositor@test.com");
    depositorProp.setPropertyType(PropertyType.String);

    User researcher = new User();
    researcher.setEmail("researcher@test.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    DataAccessRequest dar1 = new DataAccessRequest();
    DataAccessRequestData data1 = new DataAccessRequestData();
    dar1.addDatasetId(d1.getDatasetId());
    dar1.setCollectionId(1);
    dar1.setData(data1);
    dar1.setSubmissionDate(Timestamp.from(Instant.now()));
    dar1.setParentId(5);
    dar1.setReferenceId(referenceId1);
    d1.setProperties(Set.of(depositorProp));

    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1));
    when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1));
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1));
    when(userDAO.findUserById(any())).thenReturn(researcher);

    service.sendDatasetApprovalNotifications(List.of(v1), researcher);

    verify(emailService)
        .sendResearcherProgressReportApproved(any(), any(), anyList(), any(), anyBoolean());
  }

  @Test
  void testNotifyResearchersOfDarApproval_2Dars() throws Exception {
    String referenceId1 = UUID.randomUUID().toString();
    String referenceId2 = UUID.randomUUID().toString();

    Vote v1 = new Vote();
    v1.setVote(true);
    v1.setType(VoteType.FINAL.getValue());
    v1.setElectionId(1);
    v1.setUserId(1);

    Vote v2 = new Vote();
    v2.setVote(true);
    v2.setType(VoteType.FINAL.getValue());
    v2.setElectionId(2);
    v2.setUserId(1);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());

    Dataset d2 = new Dataset();
    d2.setDatasetId(2);
    d2.setName(randomAlphabetic(50));
    d2.setAlias(2);
    d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());

    Election e1 = new Election();
    e1.setElectionId(1);
    e1.setReferenceId(referenceId1);
    e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e1.setDatasetId(1);

    Election e2 = new Election();
    e2.setElectionId(2);
    e2.setReferenceId(referenceId2);
    e2.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e2.setDatasetId(2);

    DatasetProperty depositorProp = new DatasetProperty();
    depositorProp.setPropertyName("Data Depositor");
    depositorProp.setPropertyValue("depositor@test.com");
    depositorProp.setPropertyType(PropertyType.String);

    DataAccessRequest dar1 = new DataAccessRequest();
    DataAccessRequestData data1 = new DataAccessRequestData();
    dar1.addDatasetId(d1.getDatasetId());
    dar1.setCollectionId(1);
    dar1.setData(data1);
    dar1.setReferenceId(referenceId1);
    d1.setProperties(Set.of(depositorProp));

    DataAccessRequest dar2 = new DataAccessRequest();
    DataAccessRequestData data2 = new DataAccessRequestData();
    dar2.addDatasetId(d2.getDatasetId());
    dar2.setCollectionId(1);
    dar2.setData(data2);
    dar2.setReferenceId(referenceId2);
    d2.setProperties(Set.of(depositorProp));

    User researcher = new User();
    researcher.setEmail("researcher@test.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
    when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
    when(userDAO.findUserById(any())).thenReturn(researcher);

    service.sendDatasetApprovalNotifications(List.of(v1, v2), researcher);
    // Since we have 1 collection with different DAR/Datasets, we should be sending 1 email
    verify(emailService, times(2))
        .sendResearcherDarApproved(any(), any(), anyList(), any(), anyBoolean());
  }

  @Test
  void testNotifyResearchersOfDarApproval_2Dars_2Collections() throws Exception {
    String referenceId1 = UUID.randomUUID().toString();
    String referenceId2 = UUID.randomUUID().toString();

    Vote v1 = new Vote();
    v1.setVote(true);
    v1.setType(VoteType.FINAL.getValue());
    v1.setElectionId(1);
    v1.setUserId(1);

    Vote v2 = new Vote();
    v2.setVote(true);
    v2.setType(VoteType.FINAL.getValue());
    v2.setElectionId(2);
    v2.setUserId(1);

    DatasetProperty depositorProp = new DatasetProperty();
    depositorProp.setPropertyName("Data Depositor");
    depositorProp.setPropertyValue("depositor@test.com");
    depositorProp.setPropertyType(PropertyType.String);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setProperties(Set.of(depositorProp));

    Dataset d2 = new Dataset();
    d2.setDatasetId(2);
    d2.setName(randomAlphabetic(50));
    d2.setAlias(2);
    d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
    d2.setProperties(Set.of(depositorProp));

    Election e1 = new Election();
    e1.setElectionId(1);
    e1.setReferenceId(referenceId1);
    e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e1.setDatasetId(1);

    Election e2 = new Election();
    e2.setElectionId(2);
    e2.setReferenceId(referenceId2);
    e2.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e2.setDatasetId(2);

    DataAccessRequest dar1 = new DataAccessRequest();
    DataAccessRequestData data1 = new DataAccessRequestData();
    dar1.addDatasetId(d1.getDatasetId());
    dar1.setCollectionId(1);
    dar1.setData(data1);
    dar1.setReferenceId(referenceId1);

    DataAccessRequest dar2 = new DataAccessRequest();
    DataAccessRequestData data2 = new DataAccessRequestData();
    dar2.addDatasetId(d2.getDatasetId());
    dar2.setCollectionId(2);
    dar2.setData(data2);
    dar2.setReferenceId(referenceId2);

    DarCollection c1 = new DarCollection();
    c1.setDarCollectionId(1);
    c1.addDar(dar1);
    c1.setDarCode("DAR-CODE-1");

    DarCollection c2 = new DarCollection();
    c2.setDarCollectionId(2);
    c2.addDar(dar2);
    c2.setDarCode("DAR-CODE-2");

    User researcher = new User();
    researcher.setEmail("researcher@test.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
    when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
    when(userDAO.findUserById(any())).thenReturn(researcher);

    service.sendDatasetApprovalNotifications(List.of(v1, v2), researcher);
    // Since we have 2 collections with different DAR/Datasets, we should be sending 2 emails
    verify(emailService, times(2))
        .sendResearcherDarApproved(any(), any(), anyList(), any(), anyBoolean());
  }

  @Test
  void testNotifyResearchersOfDarApproval_2Dars_2Collections_RADAR() throws Exception {
    String referenceId1 = UUID.randomUUID().toString();
    String referenceId2 = UUID.randomUUID().toString();

    Vote v1 = new Vote();
    v1.setVote(true);
    v1.setType(VoteType.RADAR_APPROVE.getValue());
    v1.setElectionId(1);
    v1.setUserId(1);

    Vote v2 = new Vote();
    v2.setVote(true);
    v2.setType(VoteType.RADAR_APPROVE.getValue());
    v2.setElectionId(2);
    v2.setUserId(1);

    DatasetProperty depositorProp = new DatasetProperty();
    depositorProp.setPropertyName("Data Depositor");
    depositorProp.setPropertyValue("depositor@test.com");
    depositorProp.setPropertyType(PropertyType.String);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setProperties(Set.of(depositorProp));

    Dataset d2 = new Dataset();
    d2.setDatasetId(2);
    d2.setName(randomAlphabetic(50));
    d2.setAlias(2);
    d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
    d2.setProperties(Set.of(depositorProp));

    Election e1 = new Election();
    e1.setElectionId(1);
    e1.setReferenceId(referenceId1);
    e1.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e1.setDatasetId(1);

    Election e2 = new Election();
    e2.setElectionId(2);
    e2.setReferenceId(referenceId2);
    e2.setElectionType(ElectionType.DATA_ACCESS.getValue());
    e2.setDatasetId(2);

    DataAccessRequest dar1 = new DataAccessRequest();
    DataAccessRequestData data1 = new DataAccessRequestData();
    dar1.addDatasetId(d1.getDatasetId());
    dar1.setCollectionId(1);
    dar1.setData(data1);
    dar1.setReferenceId(referenceId1);

    DataAccessRequest dar2 = new DataAccessRequest();
    DataAccessRequestData data2 = new DataAccessRequestData();
    dar2.addDatasetId(d2.getDatasetId());
    dar2.setCollectionId(2);
    dar2.setData(data2);
    dar2.setReferenceId(referenceId2);

    DarCollection c1 = new DarCollection();
    c1.setDarCollectionId(1);
    c1.addDar(dar1);
    c1.setDarCode("DAR-CODE-1");

    DarCollection c2 = new DarCollection();
    c2.setDarCollectionId(2);
    c2.addDar(dar2);
    c2.setDarCode("DAR-CODE-2");

    User researcher = new User();
    researcher.setEmail("researcher@test.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(electionDAO.findElectionsByIds(any())).thenReturn(List.of(e1, e2));
    when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(List.of(dar1, dar2));
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
    when(userDAO.findUserById(any())).thenReturn(researcher);

    service.sendDatasetApprovalNotifications(List.of(v1, v2), researcher);
    // Since we have 2 collections with different DAR/Datasets, we should be sending 2 emails
    verify(emailService, times(2))
        .sendResearcherDarApproved(any(), any(), anyList(), any(), eq(true));
  }

  @Test
  void testNotifyResearchersOfDarApproval_FalseVote() throws Exception {
    String referenceId1 = UUID.randomUUID().toString();

    Vote v1 = new Vote();
    v1.setVote(false);
    v1.setType(VoteType.FINAL.getValue());
    v1.setElectionId(1);
    v1.setUserId(1);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());

    DataAccessRequest dar1 = new DataAccessRequest();
    DataAccessRequestData data1 = new DataAccessRequestData();
    dar1.addDatasetId(d1.getDatasetId());
    dar1.setCollectionId(1);
    dar1.setData(data1);
    dar1.setReferenceId(referenceId1);

    DarCollection c1 = new DarCollection();
    c1.setDarCollectionId(1);
    c1.addDar(dar1);
    c1.setDarCode("DAR-CODE-1");

    service.sendDatasetApprovalNotifications(List.of(v1), user);
    // Since we have a false vote, we should not be sending any email
    verify(emailService, times(0))
        .sendResearcherDarApproved(any(), any(), anyList(), any(), anyBoolean());
    // Similar check for all DAO calls
    verify(dataAccessRequestDAO, times(1)).findByReferenceIds(any());
    verify(datasetDAO, times(0)).findDatasetsByIdList(any());
  }

  @Test
  void testNotifyResearchersOfDarApproval_NonFinalVote() throws Exception {
    String referenceId1 = UUID.randomUUID().toString();

    Vote v1 = new Vote();
    v1.setVote(true);
    v1.setType(VoteType.DAC.getValue());
    v1.setElectionId(1);
    v1.setUserId(1);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());

    DataAccessRequest dar1 = new DataAccessRequest();
    DataAccessRequestData data1 = new DataAccessRequestData();
    dar1.addDatasetId(d1.getDatasetId());
    dar1.setCollectionId(1);
    dar1.setData(data1);
    dar1.setReferenceId(referenceId1);

    DarCollection c1 = new DarCollection();
    c1.setDarCollectionId(1);
    c1.addDar(dar1);
    c1.setDarCode("DAR-CODE-1");

    service.sendDatasetApprovalNotifications(List.of(v1), user);
    // Since we have a non-final vote, we should not be sending any email
    verify(emailService, times(0))
        .sendResearcherDarApproved(any(), any(), anyList(), any(), anyBoolean());
    // Similar check for all DAO calls
    verify(dataAccessRequestDAO, times(1)).findByReferenceIds(any());
    verify(datasetDAO, times(0)).findDatasetsByIdList(any());
  }

  @Test
  void testNotifyCustodiansOfApprovedDatasets() {
    User submitter = new User();
    submitter.setEmail("submitter@test.com");
    submitter.setDisplayName("submitter");
    submitter.setUserId(4);

    DatasetProperty depositorProp = new DatasetProperty();
    depositorProp.setPropertyName("Data Depositor");
    depositorProp.setPropertyValue("depositor@test.com");
    depositorProp.setSchemaProperty("dataDepositorEmail");
    depositorProp.setPropertyType(PropertyType.String);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setProperties(Set.of(depositorProp));
    d1.setCreateUserId(submitter.getUserId());

    Dataset d2 = new Dataset();
    d2.setDatasetId(2);
    d2.setName(randomAlphabetic(50));
    d2.setAlias(2);
    d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
    d2.setProperties(Set.of(depositorProp));
    d2.setCreateUserId(submitter.getUserId());

    User researcher = new User();
    researcher.setEmail("researcher@test.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(userDAO.findUserById(any())).thenReturn(submitter);

    try {
      service.notifyCustodiansOfApprovedDatasets(List.of(d1, d2), researcher, "Dar Code", false);
      verify(emailService, times(1))
          .sendDataCustodianApprovalMessage(any(), any(), any(), any(), any(), eq(false));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testNotifyCustodiansOfApprovedDatasetsNoSubmitterOrDepositorOrCustodians() throws Exception {
    User submitterNotFound = new User();
    submitterNotFound.setEmail("submitter@test.com");
    submitterNotFound.setDisplayName("submitter");
    submitterNotFound.setUserId(4);

    DatasetProperty depositorProp = new DatasetProperty();
    depositorProp.setPropertyName("Data Depositor");
    depositorProp.setPropertyValue("depositor@test.com");
    depositorProp.setPropertyType(PropertyType.String);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setProperties(Set.of(depositorProp));
    d1.setCreateUserId(submitterNotFound.getUserId());

    Dataset d2 = new Dataset();
    d2.setDatasetId(2);
    d2.setName(randomAlphabetic(50));
    d2.setAlias(2);
    d2.setDataUse(new DataUseBuilder().setGeneralUse(false).setHmbResearch(true).build());
    d2.setProperties(Set.of(depositorProp));
    d2.setCreateUserId(submitterNotFound.getUserId());

    User researcher = new User();
    researcher.setEmail("researcher@test.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(userDAO.findUserById(submitterNotFound.getUserId())).thenReturn(null);

    List<Dataset> datasetsList = List.of(d1, d2);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.notifyCustodiansOfApprovedDatasets(
                datasetsList, researcher, "Dar Code", false));
    verify(emailService, times(0))
        .sendDataCustodianApprovalMessage(any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  void testNotifyStudyCustodiansAndSubmittersOfApprovedDatasets() {
    User studySubmitter = new User();
    studySubmitter.setEmail("submitter@example.com");
    studySubmitter.setDisplayName("submitter");
    studySubmitter.setUserId(4);

    User datasetSubmitter = new User();
    datasetSubmitter.setEmail("submitter2@example.com");
    datasetSubmitter.setDisplayName("submitter2");
    datasetSubmitter.setUserId(5);

    User custodian = new User();
    String custodianEmail = "custodian@example.com";
    custodian.setEmail(custodianEmail);
    custodian.setDisplayName("custodian");
    custodian.setUserId(3);

    String custodianEmailJson = GsonUtil.getInstance().toJson(List.of(custodianEmail));

    StudyProperty custodianStudyProperty = new StudyProperty();
    custodianStudyProperty.setKey("dataCustodianEmail");
    custodianStudyProperty.setType(PropertyType.Json);
    custodianStudyProperty.setValue(custodianEmailJson);

    Study study = new Study();
    study.setStudyId(1);
    study.addProperties(custodianStudyProperty);
    study.setCreateUserId(studySubmitter.getUserId());

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setCreateUserId(datasetSubmitter.getUserId());
    d1.setStudy(study);

    User researcher = new User();
    researcher.setEmail("researcher@example.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(userDAO.findUserById(studySubmitter.getUserId())).thenReturn(studySubmitter);
    when(userDAO.findUserById(datasetSubmitter.getUserId())).thenReturn(datasetSubmitter);
    when(userDAO.findUsersByEmailList(List.of(custodian.getEmail())))
        .thenReturn(List.of(custodian));

    try {
      service.notifyCustodiansOfApprovedDatasets(List.of(d1), researcher, "Dar Code", false);
      verify(emailService, times(3))
          .sendDataCustodianApprovalMessage(any(), any(), any(), any(), any(), eq(false));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testNotifyStudyCustodiansAndSubmittersOfRADARApprovedDatasets() {
    User studySubmitter = new User();
    studySubmitter.setEmail("submitter@example.com");
    studySubmitter.setDisplayName("submitter");
    studySubmitter.setUserId(4);

    User datasetSubmitter = new User();
    datasetSubmitter.setEmail("submitter2@example.com");
    datasetSubmitter.setDisplayName("submitter2");
    datasetSubmitter.setUserId(5);

    User custodian = new User();
    String custodianEmail = "custodian@example.com";
    custodian.setEmail(custodianEmail);
    custodian.setDisplayName("custodian");
    custodian.setUserId(3);

    String custodianEmailJson = GsonUtil.getInstance().toJson(List.of(custodianEmail));

    StudyProperty custodianStudyProperty = new StudyProperty();
    custodianStudyProperty.setKey("dataCustodianEmail");
    custodianStudyProperty.setType(PropertyType.Json);
    custodianStudyProperty.setValue(custodianEmailJson);

    Study study = new Study();
    study.setStudyId(1);
    study.addProperties(custodianStudyProperty);
    study.setCreateUserId(studySubmitter.getUserId());

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setCreateUserId(datasetSubmitter.getUserId());
    d1.setStudy(study);

    User researcher = new User();
    researcher.setEmail("researcher@example.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(userDAO.findUserById(studySubmitter.getUserId())).thenReturn(studySubmitter);
    when(userDAO.findUserById(datasetSubmitter.getUserId())).thenReturn(datasetSubmitter);
    when(userDAO.findUsersByEmailList(List.of(custodian.getEmail())))
        .thenReturn(List.of(custodian));

    try {
      service.notifyCustodiansOfApprovedDatasets(List.of(d1), researcher, "Dar Code", true);
      verify(emailService, times(3))
          .sendDataCustodianApprovalMessage(any(), any(), any(), any(), any(), eq(true));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  /**
   * This test exercises the bug seen in DUOS-3066: java.lang.ClassCastException: class
   * com.google.gson.JsonArray cannot be cast to class java.lang.String
   */
  @Test
  void testNotifyStudyCustodiansAndSubmittersOfApprovedDatasetsWithJsonArrayCustodians() {
    User studySubmitter = new User();
    studySubmitter.setEmail("submitter@example.com");
    studySubmitter.setDisplayName("submitter");
    studySubmitter.setUserId(4);

    User datasetSubmitter = new User();
    datasetSubmitter.setEmail("submitter2@example.com");
    datasetSubmitter.setDisplayName("submitter2");
    datasetSubmitter.setUserId(5);

    User custodian = new User();
    String custodianEmail = "custodian@example.com";
    custodian.setEmail(custodianEmail);
    custodian.setDisplayName("custodian");
    custodian.setUserId(3);

    StudyProperty custodianStudyProperty = new StudyProperty();
    custodianStudyProperty.setKey(DatasetRegistrationSchemaV1Builder.dataCustodianEmail);
    custodianStudyProperty.setType(PropertyType.Json);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(custodianEmail);
    custodianStudyProperty.setValue(jsonArray);

    Study study = new Study();
    study.setName(randomAlphabetic(10));
    study.setStudyId(1);
    study.addProperties(custodianStudyProperty);
    study.setCreateUserId(studySubmitter.getUserId());

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setName(randomAlphabetic(50));
    d1.setAlias(1);
    d1.setDataUse(new DataUseBuilder().setGeneralUse(false).setNonProfitUse(true).build());
    d1.setCreateUserId(datasetSubmitter.getUserId());
    d1.setStudy(study);

    User researcher = new User();
    researcher.setEmail("researcher@example.com");
    researcher.setDisplayName("Researcher");
    researcher.setUserId(1);

    when(userDAO.findUserById(studySubmitter.getUserId())).thenReturn(studySubmitter);
    when(userDAO.findUserById(datasetSubmitter.getUserId())).thenReturn(datasetSubmitter);
    when(userDAO.findUsersByEmailList(List.of(custodian.getEmail())))
        .thenReturn(List.of(custodian));

    try {
      service.notifyCustodiansOfApprovedDatasets(List.of(d1), researcher, "Dar Code", false);
      verify(emailService, times(3))
          .sendDataCustodianApprovalMessage(any(), any(), any(), any(), any(), eq(false));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testLogDARApprovalOrRejection(boolean voteValue) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    Election election = new Election();
    election.setElectionId(1);
    election.setDatasetId(dataset.getDatasetId());
    User localUser = new User();
    Vote vote = new Vote();
    vote.setVote(voteValue);
    vote.setType(VoteType.FINAL.getValue());
    vote.setElectionId(election.getElectionId());
    when(electionDAO.findElectionsByIds(List.of())).thenReturn(List.of());
    when(electionDAO.findElectionsByIds(List.of(election.getElectionId())))
        .thenReturn(List.of(election));
    when(datasetDAO.findDatasetsByIdList(List.of())).thenReturn(List.of());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId())))
        .thenReturn(List.of(dataset));
    ContainerRequest request = mock();

    assertDoesNotThrow(() -> service.logDARApprovalOrRejection(localUser, List.of(vote), request));
  }

  @Test
  void testNotifySigningOfficialsOfApprovedDatasets_DAR() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId()))
        .thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfApprovedDatasets(
        List.of(dataset), researcher, dar, "DAR-000123", "translation", false);
    verify(emailService, never())
        .sendNewSoProgressReportApprovedEmail(
            any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(emailService, times(1))
        .sendNewSoDARApprovedEmail(any(), any(), any(), any(), any(), any(), eq(false));
  }

  @Test
  void testNotifySigningOfficialsOfRADARApprovedDatasets_DAR()
      throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId()))
        .thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfApprovedDatasets(
        List.of(dataset), researcher, dar, "DAR-000123", "translation", true);
    verify(emailService, never())
        .sendNewSoProgressReportApprovedEmail(
            any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(emailService, times(1))
        .sendNewSoDARApprovedEmail(any(), any(), any(), any(), any(), any(), eq(true));
  }

  @Test
  void testNotifyDACOfRadarApprovals_not_RADAR() throws TemplateException, IOException {

    service.notifyDACOfRadarApprovals(List.of(new Dataset()), new User(), "", "", false);
    verify(emailService, never()).sendNewDARRADARApprovalToDAC(any(), any(), any(), any(), any());
  }

  @Test
  void testNotifyDACOfRadarApprovals() throws TemplateException, IOException {
    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    User dac1Chair = createUserWithRole(UserRoles.CHAIRPERSON);
    User dac1Member = createUserWithRole(UserRoles.MEMBER);

    User dac2Chair = createUserWithRole(UserRoles.CHAIRPERSON);
    User dac2Member = createUserWithRole(UserRoles.MEMBER);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setAlias(1);
    dataset1.setName("DUOS Dataset 1");
    dataset1.setDacId(1);

    Dataset dataset2 = new Dataset();
    dataset1.setDatasetId(2);
    dataset1.setAlias(2);
    dataset1.setName("DUOS Dataset 2");
    dataset1.setDacId(2);

    String darCode = "DAR-123";
    String referenceId = "abc-123";

    when(dacDAO.findMembersByDacId(dataset1.getDacId())).thenReturn(List.of(dac1Member, dac1Chair));
    when(dacDAO.findMembersByDacId(dataset2.getDacId())).thenReturn(List.of(dac2Member, dac2Chair));

    service.notifyDACOfRadarApprovals(
        List.of(dataset1, dataset2), researcher, referenceId, darCode, true);
    verify(emailService)
        .sendNewDARRADARApprovalToDAC(
            dac1Member,
            darCode,
            referenceId,
            List.of(new DatasetMailDTO(dataset1.getName(), dataset1.getDatasetIdentifier())),
            researcher);
    verify(emailService)
        .sendNewDARRADARApprovalToDAC(
            dac1Chair,
            darCode,
            referenceId,
            List.of(new DatasetMailDTO(dataset1.getName(), dataset1.getDatasetIdentifier())),
            researcher);
    verify(emailService)
        .sendNewDARRADARApprovalToDAC(
            dac2Member,
            darCode,
            referenceId,
            List.of(new DatasetMailDTO(dataset2.getName(), dataset2.getDatasetIdentifier())),
            researcher);
    verify(emailService)
        .sendNewDARRADARApprovalToDAC(
            dac2Chair,
            darCode,
            referenceId,
            List.of(new DatasetMailDTO(dataset2.getName(), dataset2.getDatasetIdentifier())),
            researcher);
  }

  @Test
  void testNotifySigningOfficialsOfApprovedDatasets_PR() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequest parent = new DataAccessRequest();
    parent.setId(1);
    parent.setReferenceId(UUID.randomUUID().toString());
    parent.setDatasetIds(List.of(dataset.getDatasetId()));
    parent.setSubmissionDate(Timestamp.from(Instant.now()));

    DataAccessRequest child = new DataAccessRequest();
    child.setReferenceId(UUID.randomUUID().toString());
    child.setParentId(parent.getId());
    child.setSubmissionDate(Timestamp.from(Instant.now()));
    child.setDatasetIds(List.of(dataset.getDatasetId()));

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId()))
        .thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfApprovedDatasets(
        List.of(dataset), researcher, child, "DAR-000123", "translation", false);
    verify(emailService, times(1))
        .sendNewSoProgressReportApprovedEmail(
            any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(emailService, never())
        .sendNewSoDARApprovedEmail(any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  void testNotifySigningOfficialsOfApprovedDatasets_NoResearcher()
      throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));

    service.notifySigningOfficialsOfApprovedDatasets(
        List.of(dataset), null, dar, "DAR-000123", "translation", false);
    verify(emailService, never())
        .sendNewSoProgressReportApprovedEmail(
            any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(emailService, never())
        .sendNewSoDARApprovedEmail(any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  void testNotifySigningOfficialsOfApprovedDatasets_NoInstitution()
      throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    User researcher = createUserWithRole(UserRoles.RESEARCHER);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));

    service.notifySigningOfficialsOfApprovedDatasets(
        List.of(dataset), researcher, dar, "DAR-000123", "translation", false);
    verify(emailService, never())
        .sendNewSoProgressReportApprovedEmail(
            any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(emailService, never())
        .sendNewSoDARApprovedEmail(any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  void testNotifySigningOfficialsOfApprovedDatasets_NoSigningOfficials()
      throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    researcher.setInstitutionId(1);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId())).thenReturn(List.of());

    service.notifySigningOfficialsOfApprovedDatasets(
        List.of(dataset), researcher, dar, "DAR-000123", "translation", false);
    verify(emailService, never())
        .sendNewSoProgressReportApprovedEmail(
            any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(emailService, never())
        .sendNewSoDARApprovedEmail(any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  private User createUserWithRole(UserRoles userRoles) {
    User newUser = new User();
    newUser.setUserId(randomInt(1, 1000));
    newUser.setEmail(randomAlphabetic(10) + "@test.com");
    UserRole role = new UserRole();
    role.setUserId(newUser.getUserId());
    role.setRoleId(userRoles.getRoleId());
    role.setName(userRoles.getRoleName());
    newUser.setRoles(Collections.singletonList(role));
    return newUser;
  }

  private void setUpUserAndElectionVotes(UserRoles userRoles) {
    User localUser = new User();
    localUser.setUserId(randomInt(1, 10));
    UserRole chairRole = new UserRole();
    chairRole.setUserId(localUser.getUserId());
    chairRole.setRoleId(userRoles.getRoleId());
    chairRole.setName(userRoles.getRoleName());
    localUser.setRoles(Collections.singletonList(chairRole));
    when(userDAO.findNonDacUsersEnabledToVote()).thenReturn(Collections.singleton(localUser));
    Vote v = new Vote();
    v.setVoteId(1);
    when(voteDAO.findVoteById(anyInt())).thenReturn(v);
  }

  private Vote setUpTestVote() {
    Vote v = new Vote();
    v.setVoteId(randomInt(1, 10));
    v.setUserId(randomInt(1, 10));
    v.setElectionId(randomInt(1, 10));
    v.setIsReminderSent(true);
    v.setVote(true);
    return v;
  }
}
