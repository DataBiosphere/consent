package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;
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

    createDataAccessRequest();
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
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setDatasetIds(Collections.singletonList(dataset.getDataSetId()));
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.getData());
    createAssociation(consent.getConsentId(), dataset.getDataSetId());
    Election election = createElection(dar.getReferenceId(), dataset.getDataSetId());
    electionDAO.updateElectionById(
        election.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);

    List<Election> elections =
        metricsDAO.findLastElectionsByReferenceIds(Collections.singletonList(dar.referenceId));
    assertFalse(elections.isEmpty());
    assertEquals(1, elections.size());
  }

  @Test
  public void testFindMatchesForReferenceIds() {
    Dac dac = createDac();
    Consent consent = createConsent(dac.getDacId());
    DataAccessRequest dar = createDataAccessRequest();
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
    Consent consent = createConsent(dac.getDacId());
    DataSet dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setDatasetIds(Collections.singletonList(dataset.getDataSetId()));
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.getData());
    createAssociation(consent.getConsentId(), dataset.getDataSetId());
    Election election = createElection(dar.getReferenceId(), dataset.getDataSetId());
    electionDAO.updateElectionById(
        election.getElectionId(), ElectionStatus.CLOSED.getValue(), new Date(), true);

    List<Dac> dacs =
        metricsDAO.findAllDacsForElectionIds(Collections.singletonList(election.getElectionId()));
    assertFalse(dacs.isEmpty());
    assertEquals(1, dacs.size());
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
