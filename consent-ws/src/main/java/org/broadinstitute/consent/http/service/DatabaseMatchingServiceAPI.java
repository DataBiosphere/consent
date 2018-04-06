package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.matching.RequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.ResponseMatchingObject;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseMatchingServiceAPI extends AbstractMatchingServiceAPI {

    private ConsentAPI consentAPI;
    private DataAccessRequestAPI dataAccessAPI;
    private DataSetAPI dsAPI;
    private WebTarget matchServiceTarget;
    private GenericType<ResponseMatchingObject> rmo = new GenericType<ResponseMatchingObject>(){};
    private Logger logger = Logger.getLogger("DatabaseMatchingServiceAPI");

    public static void initInstance(Client client, ServicesConfiguration config) {
        MatchAPIHolder.setInstance(new DatabaseMatchingServiceAPI(client, config, AbstractConsentAPI.getInstance(), AbstractDataAccessRequestAPI.getInstance(), AbstractDataSetAPI.getInstance()));
    }

    DatabaseMatchingServiceAPI(Client client, ServicesConfiguration config, ConsentAPI consentAPI, DataAccessRequestAPI darAPI, DataSetAPI dsAPI){
        this.dataAccessAPI = darAPI;
        this.consentAPI = consentAPI;
        this.dsAPI = dsAPI;
        Integer timeout = 1000 * 60 * 3; // 3 minute timeout so ontology can properly do matching.
        client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
        client.property(ClientProperties.READ_TIMEOUT, timeout);
        matchServiceTarget = client.target(config.getMatchURL());
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
            logger.error("Error finding single match.", e);
            return null;
        }
        return match;
    }

    @Override
    public Match findMatchForPurpose(String purposeId){
        Document dar = dataAccessAPI.describeDataAccessRequestById(purposeId);
        Consent consent = findRelatedConsents(purposeId);
        Match match = null;
        if(dar != null){
                try {
                    match = singleEntitiesMatch(consent, dar);
                } catch (Exception e) {
                    logger.error("Error finding single match.", e);
                    match = createMatch(consent.getConsentId(), purposeId, true, false);
                }
            }
        return match;
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
                    logger.error("Error finding  matches for consent.", e);
                    matches.add(createMatch(consentId, dar.get(DarConstants.ID).toString(), true, false));
                }
            }
        }
        return matches;
    }

    private Match singleEntitiesMatch(Consent consent, Document dar) throws Exception {
        if (consent == null) {
            logger.error("Consent is null");
            throw new IllegalArgumentException("Consent cannot be null");
        }
        if (dar == null) {
            logger.error("Data Access Request is null");
            throw new IllegalArgumentException("Data Access Request cannot be null");
        }
        if(!dar.containsKey(DarConstants.RESTRICTION)){
            logger.error("Error finding single matchData Access Request: "+ dar.getString(DarConstants.DAR_CODE) + " does not have a proper Use Restriction.");
            throw new Exception("Data Access Request: " + dar.getString(DarConstants.DAR_CODE) + " cannot be matched. Missing Use Restriction field.");
        }
        Match match = createMatch(consent.getConsentId(), dar.get(DarConstants.ID).toString(), false, false);
        RequestMatchingObject requestObject = createRequestObject(consent, dar);
        String json = new Gson().toJson(requestObject);
        Response res = matchServiceTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            ResponseMatchingObject entity = res.readEntity(rmo);
            if (entity.isResult()) {
                match.setMatch(true);
            }
        }
        return match;
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
            logger.error("Consent for specified id does not exist.", e);
        }
        return consent;
    }

    private Consent findRelatedConsents(String purposeId){
        List<String> datasetIdList = (dataAccessAPI.describeDataAccessRequestById(purposeId)).get(DarConstants.DATASET_ID,List.class);
        Consent consent =  null;
        if(CollectionUtils.isNotEmpty(datasetIdList)){
            consent = consentAPI.getConsentFromDatasetID(datasetIdList.get(0));
        }
        return consent;
    }

    private List<Document> findRelatedDars(List<String> dataSetIds){
        return dataAccessAPI.describeDataAccessWithDataSetIdAndRestriction(dataSetIds);
    }

    private RequestMatchingObject createRequestObject(Consent consent, Document dar) throws Exception {
        String restriction = new Gson().toJson(dar.get(DarConstants.RESTRICTION, Map.class));
        return new RequestMatchingObject(consent.getUseRestriction(), UseRestriction.parse(restriction));
    }
}
