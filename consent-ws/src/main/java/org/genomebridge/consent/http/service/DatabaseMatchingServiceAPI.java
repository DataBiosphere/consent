package org.genomebridge.consent.http.service;

import com.google.gson.Gson;
import org.bson.Document;
import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.models.Match;
import org.genomebridge.consent.http.models.grammar.UseRestriction;
import org.genomebridge.consent.http.models.matching.RequestMatchingObject;
import org.genomebridge.consent.http.models.matching.ResponseMatchingObject;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMatchingServiceAPI extends AbstractMatchingServiceAPI {

    Client client;
    String matchURL;
    ConsentAPI consentAPI;
    DataAccessRequestAPI dataAccessAPI;
    MatchAPI matchAPI;
    private WebTarget target;
    GenericType<ResponseMatchingObject> rmo = new GenericType<ResponseMatchingObject>(){};

    public static void initInstance(Client client, ServicesConfiguration config) {
        MatchAPIHolder.setInstance(new DatabaseMatchingServiceAPI(client, config));
    }

    private DatabaseMatchingServiceAPI(Client client, ServicesConfiguration config){
        this.client = ClientBuilder.newClient();
        this.matchURL = config.getMatchURL();
        this.dataAccessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.consentAPI = AbstractConsentAPI.getInstance();
        this.matchAPI = AbstractMatchAPI.getInstance();
        client.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        client.property(ClientProperties.READ_TIMEOUT,    10000);
        target = client.target(config.getMatchURL());
    }

    @Override
    public Match findSingleMatch(String consentId, String purposeId) throws IllegalArgumentException, IOException, UnknownIdentifierException {
        RequestMatchingObject requestObject = createRequestObject(consentId, purposeId);
        String json = new Gson().toJson(requestObject);
        Response res = target.request("application/json").
                post(Entity.json(json));
        Match match = new Match();
        if(res.getStatus() == Response.Status.OK.getStatusCode()) {
            match.setConsent(consentId);
            match.setPurpose(purposeId);
            ResponseMatchingObject entity = res.readEntity(rmo);
            if (entity.isResult()) {
                match.setMatch(true);
                matchAPI.create(match);
            } else {
                match.setMatch(false);
            }
        }
        return match;
    }

    private void idNotFound(Exception e) throws NotFoundException {
        throw new NotFoundException("The object specified for the id does not exist" + e.getMessage());
    }

    @Override
    public List<Match> findMatchesForConsent(String consentId) throws IllegalArgumentException, NotFoundException, IOException {
        List<Document> dars = dataAccessAPI.describeDataAccessRequests();
        List<Match> matches = new ArrayList<>();
        try{
            for (Document dar: dars){
                String purposeId = dar.getString("_id");
                Match match = findSingleMatch(consentId, purposeId);
                if(match != null){
                    matches.add(match);
                }
            }} catch(UnknownIdentifierException e) {
            idNotFound(e);
        }
        return matches;
    }

    @Override
    public List<Match> findMatchesForPurpose(String purposeId) throws IOException, UnknownIdentifierException {
        List<Consent> consents = consentAPI.retrieveAllConsents();
        List<Match> matches = new ArrayList<>();
        for (Consent consent: consents){
            String consentId = consent.getConsentId();
            Match match = findSingleMatch(consentId, purposeId);
            if(match != null){
                matches.add(match);
            }
        }
        return matches;
    }

    private RequestMatchingObject createRequestObject(String consentId, String purposeId) throws UnknownIdentifierException, IOException {
        Consent consent = consentAPI.retrieve(consentId);
        UseRestriction restriction = dataAccessAPI.getUseRestriction(purposeId);
        return new RequestMatchingObject(consent.getUseRestriction(), restriction);
    }
}


