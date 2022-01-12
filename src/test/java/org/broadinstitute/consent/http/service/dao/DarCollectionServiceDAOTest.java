package org.broadinstitute.consent.http.service.dao;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.After;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DarCollectionServiceDAOTest extends DAOTestHelper {

  private DarCollectionServiceDAO serviceDAO;

  private void initService() {
    serviceDAO = new DarCollectionServiceDAO(jdbi, userDAO);
  }

  @After
  public void tearDown() {

    super.tearDown();
  }

  @Test
  public void testCreateElectionsForDarCollection() throws Exception {
    initService();

    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    serviceDAO.createElectionsForDarCollection(collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    // Ensure that we have an access and rp election
    assertFalse(createdElections.isEmpty());
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
  }

  private DarCollection setUpDarCollectionWithDacDataset() {
    Dac dac = createDac();
    createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    Consent consent = createConsent(dac.getDacId());
    DataSet dataset = createDataset();
    createAssociation(consent.getConsentId(), dataset.getDataSetId());
    DarCollection collection = createDarCollectionWithSingleDataAccessRequest();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);
    assertNotNull(dar.getData());
    dar.getData().setDatasetIds(List.of(dataset.getDataSetId()));
    Date now = new Date();
    dataAccessRequestDAO.updateDataByReferenceIdVersion2(
        dar.getReferenceId(), dar.getUserId(), now, now, now, dar.getData());
    return darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId());
  }
}
