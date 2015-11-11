package org.genomebridge.consent.http.service;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.genomebridge.consent.http.configurations.ServicesConfiguration;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.Match;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.genomebridge.consent.http.models.matching.RequestMatchingObject;
import org.genomebridge.consent.http.models.matching.ResponseMatchingObject;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseMatchingServiceAPI extends AbstractMatchingServiceAPI {

    Client client;
    ConsentAPI consentAPI;
    DataAccessRequestAPI dataAccessAPI;
    DataSetAPI dsAPI;
    private WebTarget target;
    GenericType<ResponseMatchingObject> rmo = new GenericType<ResponseMatchingObject>(){};
    protected Logger logger() {
        return Logger.getLogger("DatabaseMatchingServiceAPI");
    }

    public static void initInstance(Client client, ServicesConfiguration config) {
        MatchAPIHolder.setInstance(new DatabaseMatchingServiceAPI(client, config));
    }

    private DatabaseMatchingServiceAPI(Client client, ServicesConfiguration config){
        this.client = ClientBuilder.newClient();
        this.dataAccessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.dsAPI = AbstractDataSetAPI.getInstance();
        client.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        client.property(ClientProperties.READ_TIMEOUT,    10000);
        target = client.target(config.getMatchURL());
    }

    @Override
    public Match findSingleMatch(String consentId, String purposeId) {
        Match match = null;
        try {
            Consent consent = findConsent(consentId);
            Document dar = dataAccessAPI.describeDataAccessRequestById(purposeId);
            if(consent != null || dar != null) {
                match = singleEntitiesMatch(consent, dar);
            }
        } catch (Exception e) {
            logger().error("Error finding single match.", e);
            return null;
        }
        return match;
    }

    @Override
    public List<Match> findMatchesForPurpose(String purposeId){
        List<Match> matches = new ArrayList<>();
        Document dar = dataAccessAPI.describeDataAccessRequestById(purposeId);
        List<Consent> consents = findRelatedConsents(purposeId);
        if(dar != null && !consents.isEmpty()){
            for (Consent consent : consents) {
                Match match;
                try {
                    match = singleEntitiesMatch(consent, dar);
                    if(match != null){
                        matches.add(match);
                    }
                } catch (Exception e) {
                    logger().error("Error finding single match.", e);
                    matches.add(createMatch(consent.getConsentId(), purposeId, true, false));
                }
            }
        }
        return matches;
    }

    @Override
    public List<Match> findMatchesForConsent(String consentId) {
        List<Match> matches = new ArrayList<>();
        Consent consent = findConsent(consentId);
        List<DataSet> dataSets = dsAPI.getDataSetsForConsent(consentId);
        List<Document> dars = findRelatedDars(dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList()));
        if (consent != null && !dars.isEmpty()) {
            Match match;
            for (Document dar : dars) {
                try {
                    match = singleEntitiesMatch(consent, dar);
                    if(match != null){
                        matches.add(match);
                    }
                } catch (Exception e) {
                    logger().error("Error finding  matches for consent.", e);
                    matches.add(createMatch(consentId, dar.get("_id").toString(), true, false));
                }
            }
        }
        return matches;
    }

    private Match singleEntitiesMatch(Consent consent, Document dar) throws IOException, UnknownIdentifierException {
        if(consent != null && dar != null){
            Match match = createMatch(consent.getConsentId(), dar.get("_id").toString(), false, false);
            RequestMatchingObject requestObject = createRequestObject(consent, dar);
            String json = new Gson().toJson(requestObject);
            Response res = target.request("application/json").post(Entity.json(json));
            if (res.getStatus() == Response.Status.OK.getStatusCode()) {
                ResponseMatchingObject entity = res.readEntity(rmo);
                if (entity.isResult()) {
                    match.setMatch(true);
                }
            }
            return match;
        }
        return null;
    }

    private Match createMatch(String consentId, String purposeId, boolean failed, boolean isMatch) {
        Match match = new Match();
        match.setConsent(consentId);
        match.setPurpose(purposeId);
        match.setFailed(failed);
        match.setMatch(isMatch);
        match.setCreateDate(new Date());
        return match;
    }

    private Consent findConsent(String consentId){
        Consent consent = null;
        try {
            consent = consentAPI.retrieve(consentId);
        } catch (UnknownIdentifierException e) {
            logger().error("Consent for specified id does not exist.", e);
        }
        return consent;
    }

    private List<Consent> findRelatedConsents(String purposeId){
        String datasetId = (dataAccessAPI.describeDataAccessRequestById(purposeId)).getString("datasetId");
        List<Consent> consents = new ArrayList<>();
        consents.add(consentAPI.getConsentFromDatasetID(datasetId));
        return consents;
    }

    private List<Document> findRelatedDars(List<String> dataSetIds){
        return dataAccessAPI.describeDataAccessWithDataSetId(dataSetIds);
    }

    private RequestMatchingObject createRequestObject(Consent consent, Document dar) throws UnknownIdentifierException, IOException {
        String restriction = new Gson().toJson(dar.get("restriction", Map.class));
        return new RequestMatchingObject(consent.getUseRestriction(), UseRestriction.parse(restriction));
    }
}
