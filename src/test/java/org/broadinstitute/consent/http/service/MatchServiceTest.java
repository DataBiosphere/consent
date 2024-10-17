package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.matching.DataUseResponseMatchingObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"unchecked"})
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

  @Mock
  DatasetDAO datasetDAO;
  @Mock
  private ServicesConfiguration config;
  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock
  private MatchDAO matchDAO;

  private MatchService service;

  @Mock
  private Client clientMock;
  @Mock
  private WebTarget target;
  @Mock
  private Invocation.Builder builder;
  @Mock
  private Response response;
  @Mock
  private UseRestrictionConverter useRestrictionConverter;

  private void initService() {
    service = new MatchService(clientMock, config, matchDAO,
        dataAccessRequestDAO, datasetDAO,
        useRestrictionConverter);
  }

  @BeforeAll
  public static void setUpClass() {
  }

  @Test
  void testInsertMatches() {
    when(matchDAO.insertMatch(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    initService();

    service.insertMatches(List.of(new Match()));
    verify(matchDAO, atLeastOnce()).insertMatch(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testFindMatchById() {
    Match m = createMatchObject();
    when(matchDAO.findMatchById(m.getId())).thenReturn(m);
    initService();

    Match match = service.findMatchById(m.getId());
    assertNotNull(match);
    verify(matchDAO, atLeastOnce()).findMatchById(any());
  }

  @Test
  void testFindMatchByIdNotFound() {
    Match m = createMatchObject();
    when(matchDAO.findMatchById(m.getId())).thenReturn(null);
    initService();

    assertThrows(NotFoundException.class, () -> {
      service.findMatchById(m.getId());
    });
  }

  @Test
  void testFindMatchForDataAccessRequest() {
    DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
    dar.setDatasetIds(List.of(1, 2, 3));
    Mockito.mock(DataUseResponseMatchingObject.class);
    when(clientMock.target(config.getMatchURL_v4())).thenReturn(target);
    initService();

    service.createMatchesForDataAccessRequest(dar);
    verify(datasetDAO, times(dar.getDatasetIds().size())).findDatasetById(any());
  }

  @Test
  void testSingleEntitiesMatchV3EmptyDataset() {
    DataAccessRequest dar = new DataAccessRequest();
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.singleEntitiesMatchV3(null, dar);
    });
  }

  @Test
  void testSingleEntitiesMatchV3EmptyDar() {
    Dataset dataset = new Dataset();
    initService();
    assertThrows(IllegalArgumentException.class, () -> {
      service.singleEntitiesMatchV3(dataset, null);
    });
  }

  @Test
  void testSingleEntitiesMatchV3Failure() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setAlias(2);
    dataset.setDatasetIdentifier();
    DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
    dar.setDatasetIds(List.of(1, 2, 3));

    Response response = Mockito.mock(Response.class);
    when(response.getStatus()).thenReturn(500);
    when(builder.post(any())).thenReturn(response);
    when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
    when(clientMock.target(config.getMatchURL_v4())).thenReturn(target);

    initService();
    Match match = service.singleEntitiesMatchV3(dataset, dar);
    assertFalse(match.getMatch());
    assertFalse(match.getAbstain());
    assertTrue(match.getFailed());
  }

  @Test
  void testSingleEntitiesMatchV3Approve() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
    dar.setDatasetIds(List.of(1, 2, 3));
    String stringEntity = "{\"result\": \"APPROVE\", \"matchPair\": {}, \"failureReasons\": []}";

    when(response.readEntity(any(Class.class))).thenReturn(stringEntity);
    when(response.getStatus()).thenReturn(200);
    when(builder.post(any())).thenReturn(response);
    when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
    when(clientMock.target(config.getMatchURL_v4())).thenReturn(target);

    initService();
    Match match = service.singleEntitiesMatchV3(dataset, dar);
    assertTrue(match.getMatch());
    assertFalse(match.getAbstain());
    assertFalse(match.getFailed());
  }

  @Test
  void testSingleEntitiesMatchV3Deny() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
    dar.setDatasetIds(List.of(1, 2, 3));
    String stringEntity = "{\"result\": \"DENY\", \"matchPair\": {}, \"failureReasons\": []}";

    when(response.readEntity(any(Class.class))).thenReturn(stringEntity);
    when(response.getStatus()).thenReturn(200);
    when(builder.post(any())).thenReturn(response);
    when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
    when(clientMock.target(config.getMatchURL_v4())).thenReturn(target);

    initService();
    Match match = service.singleEntitiesMatchV3(dataset, dar);
    assertFalse(match.getMatch());
    assertFalse(match.getAbstain());
    assertFalse(match.getFailed());
  }

  @Test
  void testSingleEntitiesMatchV3Abstain() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
    dar.setDatasetIds(List.of(1, 2, 3));
    String stringEntity = "{\"result\": \"ABSTAIN\", \"matchPair\": {}, \"failureReasons\": []}";

    when(response.readEntity(any(Class.class))).thenReturn(stringEntity);
    when(response.getStatus()).thenReturn(200);
    when(builder.post(any())).thenReturn(response);
    when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
    when(clientMock.target(config.getMatchURL_v4())).thenReturn(target);

    initService();
    Match match = service.singleEntitiesMatchV3(dataset, dar);
    assertFalse(match.getMatch());
    assertTrue(match.getAbstain());
    assertFalse(match.getFailed());
  }

  @Test
  void testFindMatchesByPurposeId() {
    Match m = createMatchObject();
    when(matchDAO.findMatchesByPurposeId(any())).thenReturn(List.of(m));
    initService();

    List<Match> matches = service.findMatchesByPurposeId(m.getConsent());
    assertFalse(matches.isEmpty());
    verify(matchDAO, atLeastOnce()).findMatchesByPurposeId(any());
  }

  @Test
  void testReprocessMatchesForPurpose() {
    Match m = createMatchObject();
    initService();

    try {
      service.reprocessMatchesForPurpose(m.getPurpose());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testRemoveMatchesForPurpose() {
    initService();

    service.removeMatchesForPurpose("DAR-2");
    verify(matchDAO, atLeastOnce()).deleteRationalesByPurposeIds(anyList());
    verify(matchDAO, atLeastOnce()).deleteMatchesByPurposeId(any());
  }

  @Test
  void testFindMatchesForLatestDataAccessElectionsByPurposeIds() {
    Match m = createMatchObject();
    when(matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(anyList())).thenReturn(
        List.of(m));
    initService();
    List<Match> matches = service.findMatchesForLatestDataAccessElectionsByPurposeIds(
        List.of("test"));
    assertEquals(1, matches.size());
    assertEquals(m.getId(), matches.get(0).getId());
    verify(matchDAO, atLeastOnce()).findMatchesForLatestDataAccessElectionsByPurposeIds(anyList());
  }

  private DataAccessRequest getSampleDataAccessRequest(String referenceId) {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(referenceId);
    data.setHmb(true);
    dar.addDatasetId(1);
    dar.setData(data);
    return dar;
  }

  private Match createMatchObject() {
    return new Match(1, UUID.randomUUID().toString(), UUID.randomUUID().toString(), true, true,
        false, new Date(), MatchAlgorithm.V4.getVersion());
  }
}
