package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.jdbi.v3.core.Jdbi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class ConsentServiceTest {

    private ConsentService service;

    @Mock
    ConsentDAO consentDAO;

    @Mock
    ElectionDAO electionDAO;

    @Mock
    VoteDAO voteDAO;

    @Mock
    DacService dacService;

    @Mock
    DataAccessRequestDAO dataAccessRequestDAO;

    @Mock
    AuditService auditService;

    @Mock
    DataSetDAO dataSetDAO;

    @Mock
    AssociationDAO associationDAO;

    @Mock
    Jdbi jdbi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new ConsentService(consentDAO, electionDAO, voteDAO, dacService, dataAccessRequestDAO, auditService, associationDAO, jdbi, dataSetDAO);
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

    @Test
    public void testUpdateConsentDac() {
        doNothing().when(consentDAO).updateConsentDac(anyString(), anyInt());
        initService();

        try {
            service.updateConsentDac(UUID.randomUUID().toString(), RandomUtils.nextInt(1, 10));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDescribeConsentManage() {
        AuthUser user = new AuthUser("user@test.com");
        when(consentDAO.findUnreviewedConsents()).thenReturn(Collections.emptyList());
        when(consentDAO.findConsentManageByStatus(anyString())).thenReturn(Collections.emptyList());
        when(voteDAO.findChairPersonVoteByElectionId(anyInt())).thenReturn(true);
        when(electionDAO.findElectionsWithFinalVoteByTypeAndStatus(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(dataAccessRequestDAO.findByReferenceIds(any())).thenReturn(Collections.emptyList());
        when(dacService.filterConsentManageByDAC(anyList(), any(AuthUser.class))).thenReturn(Collections.emptyList());
        initService();

        try {
            service.describeConsentManage(user);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
