package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
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

public class DatabaseMatchingServiceAPITest {

    @Mock
    private ServicesConfiguration config;
    @Mock
    private ConsentService consentService;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private DatasetService datasetService;

    private static Consent sampleConsent1;
    private static Consent sampleConsent2;
    private static RequestMatchingObject reqmo1;
    private static RequestMatchingObject reqmo2;
    private static ResponseMatchingObject resmo1;
    private static ResponseMatchingObject resmo2;

    private static DatabaseMatchingServiceAPI matchApi;


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

        reqmo1 = new RequestMatchingObject(sampleConsent1.getUseRestriction(), sampleUsePurpose1);
        resmo1 = new ResponseMatchingObject(true, reqmo1);
        reqmo2 = new RequestMatchingObject(sampleConsent2.getUseRestriction(), sampleUsePurpose1);
        resmo2 = new ResponseMatchingObject(false, reqmo2);
    }

    @Before
    public void setUp() throws UnknownIdentifierException, IOException {
        MockitoAnnotations.initMocks(this);
        setUpMockedResponses();
        when(config.getMatchURL()).thenReturn("http://ontology.org/match");
        matchApi = new DatabaseMatchingServiceAPI(clientMock, dataAccessRequestDAO, config, consentService, datasetService, useRestrictionConverter);
        when(dataAccessRequestDAO.findByReferenceId("NullDar")).thenReturn(null);
        when(consentService.retrieve("NullConsent")).thenReturn(null);
        when(consentService.retrieve("AbsentConsent")).thenThrow(UnknownIdentifierException.class);
        when(consentService.retrieve("CONS-2")).thenReturn(sampleConsent2);
        when(consentService.getConsentFromDatasetID(1)).thenReturn(sampleConsent1);
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
        Mockito.when(builderMock.post(Entity.json(new Gson().toJson(reqmo2)))).thenThrow(TimeoutException.class);

        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        clientMock = Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
    }

    @Test
    public void testFindSingleMatchNull() {
        Match match = matchApi.findSingleMatch("NullConsent", "NullDar");
        assertTrue(Objects.isNull(match));
        match = matchApi.findSingleMatch("AbsentConsent", "NullDar");
        assertTrue(Objects.isNull(match));
    }

    @Test
    public void testFindSingleMatch() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        Match match = matchApi.findSingleMatch("CONS-1", "DAR-2");
        assertNotNull(match);
        assertTrue(match.getMatch());
        assertFalse(match.getFailed());
    }

    @Test
    public void testFindSingleMatchNoPurpose() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);

        Match match = matchApi.findSingleMatch("CONS-1", "DAR-2");
        assertNotNull(match);
        assertTrue(match.getMatch());
        assertFalse(match.getFailed());

    }

    @Test
    public void testFindMatchForPurpose() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);

        Match match = matchApi.findMatchForPurpose("DAR-2");
        assertTrue(match.getMatch());
        assertFalse(match.getFailed());
    }

    @Test
    public void testFindMatchesForConsent() throws Exception {
        initSingleMatchMocks("DAR-2", sampleConsent1);
        List<Match> matches = matchApi.findMatchesForConsent(sampleConsent1.getConsentId());

        assertEquals(1, matches.size());
        assertFalse(matches.get(0).getFailed());
        assertTrue(matches.get(0).getMatch());
    }

    @SuppressWarnings("unchecked")
    private void initSingleMatchMocks(String referenceId, Consent consent) throws Exception {
        DataAccessRequest dar2 = getSampleDataAccessRequest("DAR-2");
        when(dataAccessRequestDAO.findAllDataAccessRequests()).thenReturn(Collections.singletonList(dar2));
        when(dataAccessRequestDAO.findByReferenceId(referenceId)).thenReturn(dar2);
        when(consentService.retrieve(any())).thenReturn(consent);
        List<DataSet> dataSets = getSampleDataAccessRequest(referenceId)
            .getData()
            .getDatasetIds()
            .stream()
            .map(id -> {DataSet d = new DataSet(); d.setDataSetId(id); return d;} )
            .collect(Collectors.toList());
        when(datasetService.getDataSetsForConsent(consent.getConsentId())).thenReturn(dataSets);
        when(rmo.isResult()).thenReturn(true);
        when(response.readEntity(any(GenericType.class))).thenReturn(rmo);
        when(response.getStatus()).thenReturn(200);
        when(builder.post(any())).thenReturn(response);
        when(target.request(MediaType.APPLICATION_JSON)).thenReturn(builder);
        when(clientMock.target(config.getMatchURL())).thenReturn(target);
        matchApi = new DatabaseMatchingServiceAPI(clientMock, dataAccessRequestDAO, config, consentService, datasetService, useRestrictionConverter);
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