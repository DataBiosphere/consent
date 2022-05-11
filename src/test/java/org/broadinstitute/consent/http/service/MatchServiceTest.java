package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.grammar.And;
import org.broadinstitute.consent.http.models.grammar.Named;
import org.broadinstitute.consent.http.models.grammar.Not;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.matching.RequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.ResponseMatchingObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

public class MatchServiceTest {
    @Mock
    ConsentDAO consentDAO;
    @Mock
    DatasetDAO dataSetDAO;
    @Mock
    private ServicesConfiguration config;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private MatchDAO matchDAO;
    @Mock
    private DataAccessRequestService dataAccessRequestService;

    private MatchService service;

    private static Consent sampleConsent1;
    private static Consent sampleConsent2;
    private static Election sampleElection1;
    private static Election sampleElection2;
    private static RequestMatchingObject reqmo1;
    private static RequestMatchingObject reqmo2;
    private static ResponseMatchingObject resmo1;
    private static ResponseMatchingObject resmo2;
    private static Match sampleMatch1;

    @Mock
    private Client clientMock;
    @Mock
    private WebTarget target;
    @Mock
    private Invocation.Builder builder;
    @Mock
    private Response response;
    @Mock
    private ResponseMatchingObject rmo;
    @Mock
    private UseRestrictionConverter useRestrictionConverter;

   private void initService() {
        service = new MatchService(clientMock, config, consentDAO, matchDAO,
                electionDAO, dataAccessRequestDAO, dataSetDAO,
                useRestrictionConverter);
    }

    @BeforeClass
    public static void setUpClass() {
        UseRestriction sampleUseRestriction1 = new And(
                new Not(new Named("http://purl.obolibrary.org/obo/DUO_0000015")),
                new Not(new Named("http://purl.obolibrary.org/obo/DUO_0000011")),
                new Not(new Named("http://www.broadinstitute.org/ontologies/DUOS/control"))
        );
        UseRestriction sampleUseRestriction2 = new And(
                new Named("http://purl.obolibrary.org/obo/DUO_0000018"),
                new Named("http://www.broadinstitute.org/ontologies/DUOS/female")
        );
        UseRestriction sampleUsePurpose1 = new And(
                new Named("http://purl.obolibrary.org/obo/DUO_0000018")
        );
        sampleConsent1 = new Consent(false, sampleUseRestriction1, "A data use letter", "sampleConsent1", null, null, null, "Group Name Test");
        sampleConsent1.setConsentId("CONS-1");
        sampleConsent2 = new Consent(false, sampleUseRestriction2, "A data use letter", "sampleConsent1", null, null, null, "Group Name Test");
        sampleConsent2.setConsentId("CONS-2");

        sampleElection1 = new Election(1, ElectionType.TRANSLATE_DUL.getValue(),
                ElectionStatus.OPEN.getValue(), new Date(),
                "CONS-1", new Date(), false, 1);
        sampleElection2 = new Election(1, ElectionType.TRANSLATE_DUL.getValue(),
                ElectionStatus.OPEN.getValue(), new Date(),
                "CONS-2", new Date(), false, 1);

        reqmo1 = new RequestMatchingObject(sampleConsent1.getUseRestriction(), sampleUsePurpose1);
        resmo1 = new ResponseMatchingObject(true, reqmo1);
        reqmo2 = new RequestMatchingObject(sampleConsent2.getUseRestriction(), sampleUsePurpose1);
        resmo2 = new ResponseMatchingObject(false, reqmo2);

        sampleMatch1 = new Match(1, "CONS-1", "DAR-2", true, false, new Date());
    }

    @Before
    public void setUp() throws UnknownIdentifierException, IOException {
        MockitoAnnotations.initMocks(this);
        setUpMockedResponses();
        when(config.getMatchURL()).thenReturn("http://ontology.org/match");
        when(dataAccessRequestDAO.findByReferenceId("NullDar")).thenReturn(null);
        when(consentDAO.findConsentById("NullConsent")).thenReturn(null);
        doAnswer(invocationOnMock -> { throw new UnknownIdentifierException("AbsentConsent"); }).when(consentDAO).findConsentById("AbsentConsent");
        when(consentDAO.findConsentById("CONS-2")).thenReturn(sampleConsent2);
        when(electionDAO.findLastElectionByReferenceIdAndType("CONS-1", ElectionType.TRANSLATE_DUL.getValue())).thenReturn(sampleElection1);
        when(electionDAO.findLastElectionByReferenceIdAndType("CONS-2", ElectionType.TRANSLATE_DUL.getValue())).thenReturn(sampleElection2);
        when(consentDAO.findConsentFromDatasetID(1)).thenReturn(sampleConsent1);
    }

    private void setUpMockedResponses() {

        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);

        final Response okResponseMock = Mockito.mock(Response.class);
        Mockito.when(okResponseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(okResponseMock.readEntity((GenericType<Object>) any())).thenReturn(resmo1);

        final Response noMatchResponseMock = Mockito.mock(Response.class);
        Mockito.when(noMatchResponseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(noMatchResponseMock.readEntity((GenericType<Object>) any())).thenReturn(resmo2);

        Mockito.when(builderMock.post(Entity.json(new Gson().toJson(reqmo1)))).thenReturn(okResponseMock);
        doAnswer(invocationOnMock -> { throw new TimeoutException(); }).when(builderMock).post(Entity.json(new Gson().toJson(reqmo2)));

        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        clientMock = Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
    }

    @Test
    public void testFindSingleMatchNull() {
        initService();
        Match match = service.findSingleMatch("NullConsent", "NullDar");
        assertTrue(Objects.isNull(match));
        match = service.findSingleMatch("AbsentConsent", "NullDar");
        assertTrue(Objects.isNull(match));
    }

    @Test
    public void testCreate() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        when(matchDAO.insertMatch(any(), any(), any(), any(), any()))
                .thenReturn(sampleMatch1.getId());
        when(matchDAO.findMatchById(any()))
                .thenReturn(sampleMatch1);
        initService();

        Match match = service.create(sampleMatch1);
        assertNotNull(match);
    }

    @Test
    public void testUpdate() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        doNothing().when(matchDAO).updateMatch(any(), any(), any(), any(), any());
        when(matchDAO.findMatchById(1)).thenReturn(sampleMatch1);
        initService();

        Match match = service.update(sampleMatch1, 1);
        assertNotNull(match);
        assertEquals("CONS-1", match.getConsent());
    }

    @Test
    public void testFindMatchById() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        when(matchDAO.findMatchById(1)).thenReturn(sampleMatch1);
        initService();

        Match match = service.findMatchById(1);
        assertNotNull(match);
        assertEquals("CONS-1", match.getConsent());
    }

    @Test
    public void testFindSingleMatch() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        initService();
        Match match = service.findSingleMatch("CONS-1", "DAR-2");
        assertNotNull(match);
        assertTrue(match.getMatch());
        assertFalse(match.getFailed());
    }

    @Test
    public void testFindSingleMatchNoPurpose() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        initService();

        Match match = service.findSingleMatch("CONS-1", "DAR-2");
        assertNotNull(match);
        assertTrue(match.getMatch());
        assertFalse(match.getFailed());

    }

    @Test
    public void testFindMatchForPurpose() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        initService();

        Match match = service.createMatchesForPurpose("DAR-2");
        assertTrue(match.getMatch());
        assertFalse(match.getFailed());
    }

    @Test
    public void testFindMatchesForConsent() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        initService();

        List<Match> matches = service.createMatchesForConsent(sampleConsent1.getConsentId());

        assertEquals(1, matches.size());
        assertFalse(matches.get(0).getFailed());
        assertTrue(matches.get(0).getMatch());
    }

    @Test
    public void testProcessMatchesForConsent() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        when(matchDAO.findMatchesByConsentId(sampleConsent1.getConsentId()))
                .thenReturn(Arrays.asList(sampleMatch1));
        when(consentDAO.checkManualReview("CONS-1"))
                .thenReturn(true);
        doNothing().when(matchDAO).insertAll(any());
        initService();

        service.reprocessMatchesForConsent("CONS-1");
    }

    @Test
    public void testProcessMatchesForPurpose() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        doNothing().when(matchDAO).insertAll(any());
        initService();

        service.reprocessMatchesForPurpose(sampleMatch1.getPurpose());
    }

    @Test
    public void testRemoveMatchesForPurpose() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);

        initService();

        service.removeMatchesForPurpose("DAR-2");
    }

    @Test
    public void testRemoveMatchesForConsent() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);

        initService();

        service.removeMatchesForConsent(sampleConsent1.getConsentId());
    }

    @Test
    public void testFindMatchesForLatestDataAccessElectionsByPurposeIds() {
        Match mockMatch = new Match(1, "test", "purpose", true, true, null);
        when(matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(anyList()))
            .thenReturn(List.of(mockMatch));
        initService();
        List<Match> matches = service.findMatchesForLatestDataAccessElectionsByPurposeIds(List.of("test"));
        assertEquals(1, matches.size());
        assertEquals(mockMatch.getId(), matches.get(0).getId());
    }

    @SuppressWarnings("unchecked")
    private void initSingleMatchMocks(String referenceId, Consent consent) throws Exception {
        DataAccessRequest dar2 = getSampleDataAccessRequest("DAR-2");
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(Collections.singletonList(dar2));
        when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar2);
        when(consentDAO.findConsentById(any())).thenReturn(consent);
        when(consentDAO.checkConsentById(any())).thenReturn(consent.getConsentId());
        List<Dataset> dataSets = getSampleDataAccessRequest(referenceId)
                .getData()
                .getDatasetIds()
                .stream()
                .map(id -> {
                    Dataset d = new Dataset(); d.setDataSetId(id); return d;} )
                .collect(Collectors.toList());
        when(dataSetDAO.getDatasetsForConsent(consent.getConsentId())).thenReturn(dataSets);
        when(rmo.isResult()).thenReturn(true);
        when(response.readEntity(any(GenericType.class))).thenReturn(rmo);
        when(response.getStatus()).thenReturn(200);
        when(builder.post(any())).thenReturn(response);
        when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(clientMock.target(config.getMatchURL())).thenReturn(target);

        when(dataAccessRequestService.findByReferenceId(referenceId)).thenReturn(dar2);
        when(matchDAO.findMatchById(any())).thenReturn(sampleMatch1);
    }

    private DataAccessRequest getSampleDataAccessRequest(String referenceId) {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setReferenceId(referenceId);
        DataAccessRequestData data = new DataAccessRequestData();
        data.setReferenceId(referenceId);
        data.setHmb(true);
        data.setDatasetIds(Collections.singletonList(1));
        dar.setData(data);
        return dar;
    }
}
