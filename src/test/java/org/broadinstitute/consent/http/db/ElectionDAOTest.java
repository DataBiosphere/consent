package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ElectionDAOTest extends DAOTestHelper {

    @Test
    public void testFindDacForConsentElection() {
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
        Assert.assertNotNull(foundDac);
        Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
    }

    @Test
    public void testFindDacForConsentElectionWithNoAssociation() {
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
        Assert.assertNotNull(foundDac);
        Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
    }

    @Test
    public void testFindDacForConsentElectionNotFound() {
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForElection(election.getElectionId());
        Assert.assertNull(foundDac);
    }

    @Test
    public void testFindElectionByDacId() {
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        Assert.assertNotNull(foundElections);
        Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
    }

    @Test
    public void testFindElectionByDacIdWithNoAssociation() {
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        Assert.assertNotNull(foundElections);
        Assert.assertEquals(election.getElectionId(), foundElections.get(0).getElectionId());
    }

    @Test
    public void testFindElectionByDacIdNotFound() {
        Dac dac = createDac();
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        createElection(consent.getConsentId(), dataset.getDataSetId());

        List<Election> foundElections = electionDAO.findOpenElectionsByDacId(dac.getDacId());
        Assert.assertTrue(foundElections.isEmpty());
    }

    @Test
    public void testFindDacsForConsentElections() {
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        Dac dac2 = createDac();
        Consent consent2 = createConsent(dac2.getDacId());
        DataSet dataset2 = createDataset();
        createAssociation(consent2.getConsentId(), dataset2.getDataSetId());
        Election election2 = createElection(consent2.getConsentId(), dataset2.getDataSetId());
        List<Integer> electionIds = new ArrayList<>();
        electionIds.add(election.getElectionId());
        electionIds.add(election2.getElectionId());

        List<Dac> foundDacs = electionDAO.findDacsForElections(electionIds);
        Assert.assertNotNull(foundDacs);
        Assert.assertFalse(foundDacs.isEmpty());
        Assert.assertTrue(foundDacs.stream().map(Dac::getDacId).collect(Collectors.toList()).contains(dac.getDacId()));
        Assert.assertTrue(foundDacs.stream().map(Dac::getDacId).collect(Collectors.toList()).contains(dac2.getDacId()));
    }

}
