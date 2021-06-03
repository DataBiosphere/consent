package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;

import org.junit.Test;

public class MetricsDAOTest extends DAOTestHelper {

  @Test
  public void testFindAllDars() {
    List<DataAccessRequest> dars = metricsDAO.findAllDars();
    assertTrue(dars.isEmpty());

    createDataAccessRequestV2();
    createDraftDataAccessRequest();
    List<DataAccessRequest> newDars = metricsDAO.findAllDars();
    assertFalse(newDars.isEmpty());
    assertEquals(1, newDars.size());
  }

  @Test
  public void testFindLastElectionsByReferenceIds() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    DataSet dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequestV2();

    String darReferenceId = dar.getReferenceId();
    Integer datasetId = dataset.getDataSetId();
    dar.getData().setDatasetIds(Collections.singletonList(datasetId));
    dataAccessRequestDAO.updateDataByReferenceId(darReferenceId, dar.getData());
    createAssociation(consent.getConsentId(), datasetId);
    
    Election cancelledAccessElection = createAccessElection(darReferenceId, datasetId);
    Election cancelledRPElection = createRPElection(darReferenceId, datasetId);
    electionDAO.updateElectionById(
        cancelledAccessElection.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date(), true);
    electionDAO.updateElectionById(
      cancelledRPElection.getElectionId(), ElectionStatus.CANCELED.getValue(),new Date(), true);
    
    Election prevClosedAccessElection = createAccessElection(darReferenceId, dataset.getDataSetId());
    Election prevClosedRPElection = createAccessElection(darReferenceId, datasetId);
    electionDAO.updateElectionById(
      prevClosedAccessElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
    electionDAO.updateElectionById(
      prevClosedRPElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);

    Election recentClosedAccessElection = createAccessElection(darReferenceId, dataset.getDataSetId());
    Election recentClosedRPElection = createRPElection(darReferenceId, datasetId);
    electionDAO.updateElectionById(
      recentClosedAccessElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
    electionDAO.updateElectionById(
      recentClosedRPElection.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);
    List<Election> elections =
        metricsDAO.findLastElectionsByReferenceIds(Collections.singletonList(dar.referenceId));
    List<Integer> electionIds = elections.stream().map(e -> e.getElectionId()).collect(Collectors.toList());
    assertFalse(elections.isEmpty());
    assertEquals(2, elections.size());
    assertTrue(electionIds.contains(recentClosedAccessElection.getElectionId()));
    assertTrue(electionIds.contains(recentClosedRPElection.getElectionId()));

  }

  @Test
  public void testFindMatchesForReferenceIds() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequestV2();
    Match m = new Match();
    m.setConsent(consent.getConsentId());
    m.setPurpose(dar.getReferenceId());
    m.setMatch(true);
    m.setFailed(false);
    m.setCreateDate(new Date());
    matchDAO.insertAll(Collections.singletonList(m));

    List<Match> matches =
        metricsDAO.findMatchesForPurposeIds(Collections.singletonList(dar.getReferenceId()));
    assertFalse(matches.isEmpty());
    assertEquals(1, matches.size());
  }

  @Test
  public void testFindAllDacsForElectionIds() {
    Dac dac = createDac();
    String accessReferenceId = UUID.randomUUID().toString();
    DataSet dataset = createDataset();
    Integer datasetId = dataset.getDataSetId();
    Consent consent = createConsent(dac.getDacId());
    Election dulElection = createDULElection(consent.getConsentId(), datasetId);
    Election accessElection = createAccessElection(accessReferenceId, datasetId);
    electionDAO.insertAccessAndConsentElection(accessElection.getElectionId(), dulElection.getElectionId());

    List<Integer> electionIds = Collections.singletonList(accessElection.getElectionId());
    List<Dac> dacList = metricsDAO.findAllDacsForElectionIds(electionIds);
    Dac dacRecord = dacList.get(0);
    assertEquals(1, dacList.size());
    assertEquals(dac.getName(), dacRecord.getName());
    assertEquals(dac.getDacId(), dacRecord.getDacId());
  }

  @Test
  public void testFindDatasetsByIdList() {
    DataSet dataset = createDataset();

    List<DataSet> datasets =
        metricsDAO.findDatasetsByIds(Collections.singletonList(dataset.getDataSetId()));
    assertFalse(datasets.isEmpty());
    assertEquals(1, datasets.size());
  }
}
