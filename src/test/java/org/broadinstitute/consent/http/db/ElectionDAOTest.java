package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Assert;
import org.junit.Test;

public class ElectionDAOTest extends DAOTestHelper {

    @Test
    public void testFindDacForConsentElection() {
        Dac dac = createDac();
        Consent consent = createConsent(dac.getDacId());
        DataSet dataset = createDataset();
        createAssociation(consent.getConsentId(), dataset.getDataSetId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForConsentElection(election.getElectionId());
        Assert.assertNotNull(foundDac);
        Assert.assertEquals(dac.getDacId(), foundDac.getDacId());
    }

    @Test
    public void testFindDacForConsentElectionNotFound() {
        Consent consent = createConsent(null);
        DataSet dataset = createDataset();
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());

        Dac foundDac = electionDAO.findDacForConsentElection(election.getElectionId());
        Assert.assertNull(foundDac);
    }

}
