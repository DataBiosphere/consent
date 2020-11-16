package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;

public class MailMessageDAOTest extends DAOTestHelper {

    @Test
    public void testExistsCollectDAREmailNegative() {
        Integer exists = mailMessageDAO.existsCollectDAREmail(
                RandomStringUtils.random(10, true, false),
                RandomStringUtils.random(10, true, false)
        );
        assertNull(exists);
    }

    @Test
    public void testInsertEmail() {
        User chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac d = createDac();
        DataSet dataset = createDataset();
        Consent c = createConsent(d.getDacId());
        createAssociation(c.getConsentId(), dataset.getDataSetId());
        Election e = createElection(c.getConsentId(), dataset.getDataSetId());
        Vote vote = createChairpersonVote(chair.getDacUserId(), e.getElectionId());
        mailMessageDAO.insertEmail(
                vote.getVoteId(),
                e.getReferenceId(),
                chair.getDacUserId(),
                1,
                new Date(),
                RandomStringUtils.random(10, true, false)
        );
        Integer exists = mailMessageDAO.existsCollectDAREmail(e.getReferenceId(), "rpReferenceId");
        assertNotNull(exists);
        assertTrue(exists > 0);
    }

    @Test
    public void testInsertBulkEmailNoVotes() {
        User chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Dac d = createDac();
        DataSet dataset = createDataset();
        Consent c = createConsent(d.getDacId());
        createAssociation(c.getConsentId(), dataset.getDataSetId());
        Election e = createElection(c.getConsentId(), dataset.getDataSetId());
        mailMessageDAO.insertBulkEmailNoVotes(
                Collections.singletonList(chair.getDacUserId()),
                e.getReferenceId(),
                1,
                new Date(),
                RandomStringUtils.random(10, true, false)
        );
        Integer exists = mailMessageDAO.existsCollectDAREmail(e.getReferenceId(), "rpReferenceId");
        assertNotNull(exists);
        assertTrue(exists > 0);
    }

}
