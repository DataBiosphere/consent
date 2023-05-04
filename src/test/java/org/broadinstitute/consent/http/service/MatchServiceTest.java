package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.matching.DataUseResponseMatchingObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

@SuppressWarnings({"unchecked", "resource"})
public class MatchServiceTest {
    @Mock
    ConsentDAO consentDAO;
    @Mock
    DatasetDAO datasetDAO;
    @Mock
    private ServicesConfiguration config;
    @Mock
    private ElectionDAO electionDAO;
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
        service = new MatchService(clientMock, config, consentDAO, matchDAO,
                dataAccessRequestDAO, datasetDAO,
                useRestrictionConverter);
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @Before
    public void setUp() throws UnknownIdentifierException, IOException {
        openMocks(this);
    }

    @Test
    public void testInsertMatches() {
        when(matchDAO.insertMatch(any(), any(), any(), any(), any(), any())).thenReturn(1);
        doNothing().when(matchDAO).insertFailureReason(any(), any());
        spy(matchDAO);
        initService();

        service.insertMatches(List.of(new Match()));
        verify(matchDAO, atLeastOnce()).insertMatch(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testFindMatchById() {
        Match m = createMatchObject();
        when(matchDAO.findMatchById(m.getId())).thenReturn(m);
        spy(matchDAO);
        initService();

        Match match = service.findMatchById(m.getId());
        assertNotNull(match);
        verify(matchDAO, atLeastOnce()).findMatchById(any());
    }

    @Test(expected = NotFoundException.class)
    public void testFindMatchByIdNotFound() {
        Match m = createMatchObject();
        when(matchDAO.findMatchById(m.getId())).thenReturn(null);
        initService();

        service.findMatchById(m.getId());
    }

    @Test
    public void testFindMatchByConsentId() {
        Match m = createMatchObject();
        when(matchDAO.findMatchesByConsentId(any())).thenReturn(List.of(m));
        spy(matchDAO);
        initService();

        List<Match> matches = service.findMatchByConsentId(m.getConsent());
        assertNotNull(matches);
        assertEquals(1, matches.size());
        verify(matchDAO, atLeastOnce()).findMatchesByConsentId(any());
    }

    @Test
    public void testFindMatchByConsentIdNotFound() {
        Match m = createMatchObject();
        when(matchDAO.findMatchesByConsentId(any())).thenReturn(List.of());
        spy(matchDAO);
        initService();

        List<Match> matches = service.findMatchByConsentId(m.getConsent());
        assertTrue(matches.isEmpty());
        verify(matchDAO, atLeastOnce()).findMatchesByConsentId(any());
    }

    @Test
    public void testFindMatchForDataAccessRequest() {
        DataAccessRequest dar = getSampleDataAccessRequest("DAR-2");
        dar.setDatasetIds(List.of(1, 2, 3));
        DataUseResponseMatchingObject responseObject = Mockito.mock(DataUseResponseMatchingObject.class);
        when(response.readEntity(any(GenericType.class))).thenReturn(responseObject);
        when(response.getStatus()).thenReturn(200);
        when(builder.post(any())).thenReturn(response);
        when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(clientMock.target(config.getMatchURL_v3())).thenReturn(target);
        spy(datasetDAO);
        initService();

        service.createMatchesForDataAccessRequest(dar);
        verify(datasetDAO, times(dar.getDatasetIds().size())).findDatasetById(any());
    }

    @Test
    public void testCreateMatchesForConsent() {
        Match m = createMatchObject();
        Dataset dataset = new Dataset();
        dataset.setDataSetId(1);
        DataAccessRequest dar = new DataAccessRequest();
        dar.addDatasetId(1);
        when(datasetDAO.getDatasetsForConsent(any())).thenReturn(List.of(dataset));
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(List.of(dar));
        spy(datasetDAO);
        spy(dataAccessRequestDAO);
        initService();

        service.createMatchesForConsent(m.getConsent());
        verify(datasetDAO, atLeastOnce()).getDatasetsForConsent(any());
        verify(dataAccessRequestDAO, atLeastOnce()).findAllDataAccessRequests();
    }

    @Test
    public void testFindMatchesByPurposeId() {
        Match m = createMatchObject();
        when(matchDAO.findMatchesByPurposeId(any())).thenReturn(List.of(m));
        spy(matchDAO);
        initService();

        List<Match> matches = service.findMatchesByPurposeId(m.getConsent());
        assertFalse(matches.isEmpty());
        verify(matchDAO, atLeastOnce()).findMatchesByPurposeId(any());
    }

    @Test
    public void testReprocessMatchesForConsent() {
        Match m = createMatchObject();
        when(matchDAO.findMatchesByConsentId(any())).thenReturn(List.of(m));
        initService();

        try {
            service.reprocessMatchesForConsent(m.getConsent());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testReprocessMatchesForPurpose() {
        Match m = createMatchObject();
        initService();

        try {
            service.reprocessMatchesForPurpose(m.getPurpose());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testRemoveMatchesForPurpose() {
        spy(matchDAO);
        initService();

        service.removeMatchesForPurpose("DAR-2");
        verify(matchDAO, atLeastOnce()).deleteFailureReasonsByPurposeIds(anyList());
        verify(matchDAO, atLeastOnce()).deleteMatchesByPurposeId(any());
    }

    @Test
    public void testRemoveMatchesForConsent() {
        Match m = createMatchObject();
        spy(matchDAO);
        initService();

        service.removeMatchesForConsent(m.getConsent());
        verify(matchDAO, atLeastOnce()).deleteFailureReasonsByConsentIds(anyList());
        verify(matchDAO, atLeastOnce()).deleteMatchesByConsentId(any());
    }

    @Test
    public void testFindMatchesForLatestDataAccessElectionsByPurposeIds() {
        Match m = createMatchObject();
        when(matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(anyList())).thenReturn(List.of(m));
        spy(matchDAO);
        initService();
        List<Match> matches = service.findMatchesForLatestDataAccessElectionsByPurposeIds(List.of("test"));
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
        return new Match(1, UUID.randomUUID().toString(), UUID.randomUUID().toString(), true, true, false, new Date(), MatchAlgorithm.V3.getVersion());
    }
}
