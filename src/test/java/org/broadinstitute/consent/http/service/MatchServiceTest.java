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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.matching.DataUseMatcherV4;
import org.broadinstitute.consent.http.matching.DataUseUtil;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest extends AbstractTestHelper {

  @Mock private Jdbi jdbi;
  @Mock DatasetDAO datasetDAO;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock private MatchDAO matchDAO;
  @Mock private UseRestrictionConverter useRestrictionConverter;
  @Mock private OntologyService ontologyService;

  private DataUseMatcherV4 dataUseMatcherV4;
  private MatchService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(MatchDAO.class)).thenReturn(matchDAO);
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(dataAccessRequestDAO);
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(datasetDAO);
    dataUseMatcherV4 = new DataUseMatcherV4(new DataUseUtil(ontologyService));
    service = new MatchService(jdbi, useRestrictionConverter, dataUseMatcherV4);
  }

  @Test
  void testInsertMatches() {
    when(matchDAO.insertMatch(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

    service.insertMatches(List.of(new Match()));
    verify(matchDAO, atLeastOnce()).insertMatch(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testFindMatchForDataAccessRequest() {
    DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
    dar.setDatasetIds(List.of(1, 2, 3));

    service.createMatchesForDataAccessRequest(dar);
    verify(datasetDAO, times(dar.getDatasetIds().size())).findDatasetById(any());
  }

  @Test
  void testCreateMatchesForDataAccessRequestFailure() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setName("Test Dataset 1");
    dataset.setDataUse(new DataUseBuilder().setHmbResearch(true).build());
    dataset.setProperties(Collections.emptySet());
    DataAccessRequest dar = getSampleDataAccessRequest(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    dar.setData(new DataAccessRequestData());
    dar.getData().setHmb(true);
    dar.getData().setDiseases(false);
    when(datasetDAO.findDatasetById(dataset.getDatasetId())).thenReturn(dataset);
    when(useRestrictionConverter.parseDataUsePurpose(dar))
        .thenThrow(new IllegalArgumentException());

    try {
      List<Match> matches = service.createMatchesForDataAccessRequest(dar);
      assertFalse(matches.isEmpty());
      // Each match should be false since the exception is thrown during the matching process
      matches.forEach(m -> assertFalse(m.getMatch()));
    } catch (Exception e) {
      fail(
          "createMatchesForDataAccessRequest should not throw an exception even if singleEntitiesMatch fails: "
              + e.getMessage());
    }
  }

  @Test
  void testSingleEntitiesMatchEmptyDataset() {
    DataAccessRequest dar = new DataAccessRequest();

    assertThrows(IllegalArgumentException.class, () -> service.singleEntitiesMatch(null, dar));
  }

  @Test
  void testSingleEntitiesMatchEmptyDar() {
    Dataset dataset = new Dataset();

    assertThrows(IllegalArgumentException.class, () -> service.singleEntitiesMatch(dataset, null));
  }

  @Test
  void testSingleEntitiesMatchNullDarDataUse() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setName("Test Dataset 1");
    dataset.setDataUse(new DataUseBuilder().setHmbResearch(true).build());
    dataset.setProperties(Collections.emptySet());
    DataAccessRequest dar = getSampleDataAccessRequest(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    dar.setData(new DataAccessRequestData());
    dar.getData().setHmb(true);
    dar.getData().setDiseases(false);
    when(useRestrictionConverter.parseDataUsePurpose(dar)).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> service.singleEntitiesMatch(dataset, dar));
  }

  @Test
  void testSingleEntitiesMatch() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setName("Test Dataset 1");
    dataset.setDataUse(new DataUseBuilder().setHmbResearch(true).build());
    dataset.setProperties(Collections.emptySet());
    DataAccessRequest dar = getSampleDataAccessRequest(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    dar.setData(new DataAccessRequestData());
    dar.getData().setHmb(true);
    dar.getData().setDiseases(false);
    when(useRestrictionConverter.parseDataUsePurpose(dar))
        .thenReturn(new DataUseBuilder().setHmbResearch(true).build());

    Match match = service.singleEntitiesMatch(dataset, dar);
    assertNotNull(match);
    assertTrue(match.getMatch());
  }

  @Test
  void testFindMatchesByPurposeId() {
    Match m = createMatchObject();
    when(matchDAO.findMatchesByPurposeId(any())).thenReturn(List.of(m));

    List<Match> matches = service.findMatchesByPurposeId(m.getConsent());
    assertFalse(matches.isEmpty());
    verify(matchDAO, atLeastOnce()).findMatchesByPurposeId(any());
  }

  @Test
  void testReprocessMatchesForPurpose() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setName("Test Dataset 1");
    dataset.setDataUse(new DataUseBuilder().setHmbResearch(true).build());
    dataset.setProperties(Collections.emptySet());
    DataAccessRequest dar = getSampleDataAccessRequest(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    dar.setData(new DataAccessRequestData());
    dar.getData().setHmb(true);
    dar.getData().setDiseases(false);
    when(datasetDAO.findDatasetById(dataset.getDatasetId())).thenReturn(dataset);
    when(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId())).thenReturn(dar);
    when(useRestrictionConverter.parseDataUsePurpose(dar))
        .thenReturn(new DataUseBuilder().setHmbResearch(true).build());

    service.reprocessMatchesForPurpose(dar.getReferenceId());
    verify(matchDAO).deleteRationalesByPurposeIds(List.of(dar.getReferenceId()));
    verify(matchDAO).deleteMatchesByPurposeId(dar.getReferenceId());
    verify(matchDAO).insertMatch(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testRemoveMatchesForPurpose() {
    service.removeMatchesForPurpose("DAR-2");
    verify(matchDAO, atLeastOnce()).deleteRationalesByPurposeIds(anyList());
    verify(matchDAO, atLeastOnce()).deleteMatchesByPurposeId(any());
  }

  @Test
  void testFindMatchesForLatestDataAccessElectionsByPurposeIds() {
    Match m = createMatchObject();
    when(matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(anyList()))
        .thenReturn(List.of(m));

    List<Match> matches =
        service.findMatchesForLatestDataAccessElectionsByPurposeIds(List.of("test"));
    assertEquals(1, matches.size());
    assertEquals(m.getId(), matches.getFirst().getId());
    verify(matchDAO, atLeastOnce()).findMatchesForLatestDataAccessElectionsByPurposeIds(anyList());
  }

  private DataAccessRequest getSampleDataAccessRequest(String referenceId) {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(referenceId);
    data.setHmb(true);
    data.setDiseases(false);
    dar.addDatasetId(1);
    dar.setData(data);
    return dar;
  }

  private Match createMatchObject() {
    return new Match(
        1,
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        true,
        true,
        false,
        new Date(),
        MatchAlgorithm.V4.getVersion());
  }
}
