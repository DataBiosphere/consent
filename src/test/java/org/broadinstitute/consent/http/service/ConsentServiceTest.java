package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ConsentServiceTest {

    private ConsentService service;

    @Mock
    ConsentDAO consentDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    MongoConsentDB mongo;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DacService dacService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new ConsentService(consentDAO, electionDAO, mongo, voteDAO, dacService);
    }

    @Test
    public void testGetById() throws Exception {
        when(consentDAO.findConsentById(anyString())).thenReturn(new Consent());
        Election mockElection = new Election();
        mockElection.setStatus(ElectionStatus.OPEN.getValue());
        mockElection.setArchived(false);
        when(electionDAO.findLastElectionByReferenceIdAndType(anyString(), anyString())).thenReturn(mockElection);
        initService();

        Consent consent = service.getById(UUID.randomUUID().toString());
        Assert.assertNotNull(consent);
        Assert.assertEquals(ElectionStatus.OPEN.getValue(), consent.getLastElectionStatus());
        Assert.assertFalse(consent.getLastElectionArchived());
    }

}
