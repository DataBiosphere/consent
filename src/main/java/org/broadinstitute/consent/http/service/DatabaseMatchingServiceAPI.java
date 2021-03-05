package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.matching.RequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.ResponseMatchingObject;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseMatchingServiceAPI extends AbstractMatchingServiceAPI {

    private final ConsentAPI consentAPI;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DataSetAPI dsAPI;
    private final WebTarget matchServiceTarget;
    private final GenericType<ResponseMatchingObject> rmo = new GenericType<>(){};
    private final UseRestrictionConverter useRestrictionConverter;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void initInstance(Client client, DataAccessRequestDAO dataAccessRequestDAO,
        ServicesConfiguration config, UseRestrictionConverter useRestrictionConverter) {
        MatchAPIHolder.setInstance(new DatabaseMatchingServiceAPI(client, dataAccessRequestDAO, config, AbstractConsentAPI.getInstance(), AbstractDataSetAPI.getInstance(), useRestrictionConverter));
    }

    DatabaseMatchingServiceAPI(Client client, DataAccessRequestDAO dataAccessRequestDAO, ServicesConfiguration config, ConsentAPI consentAPI, DataSetAPI dsAPI, UseRestrictionConverter useRestrictionConverter){
        this.consentAPI = consentAPI;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.dsAPI = dsAPI;
        Integer timeout = 1000 * 60 * 3; // 3 minute timeout so ontology can properly do matching.
        client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
        client.property(ClientProperties.READ_TIMEOUT, timeout);
        matchServiceTarget = client.target(config.getMatchURL());
        this.useRestrictionConverter = useRestrictionConverter;
    }

    @Override
    public Match findSingleMatch(String consentId, String purposeId) {
        Match match = null;
        try {
            Consent consent = findConsent(consentId);
            DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
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
        Match match = null;
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
        if (Objects.nonNull(dar)) {
            List<Integer> dataSetIdList = dar.getData().getDatasetIds();
            Consent consent = findRelatedConsent(dataSetIdList);
            if (Objects.nonNull(consent)) {
                try {
                    match = singleEntitiesMatch(consent, dar);
                } catch (Exception e) {
                    logger.error("Error finding single match.", e);
                    match = createMatch(consent.getConsentId(), purposeId, true, false);
                }
            }
        }
        return match;
    }

    @Override
    public List<Match> findMatchesForConsent(String consentId) {
        List<Match> matches = new ArrayList<>();
        Consent consent = findConsent(consentId);
        List<DataSet> dataSets = dsAPI.getDataSetsForConsent(consentId);
        List<DataAccessRequest> dars = findRelatedDars(dataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList()));
        if (consent != null && !dars.isEmpty()) {
            Match match;
            for (DataAccessRequest dar : dars) {
                try {
                    match = singleEntitiesMatch(consent, dar);
                    if(match != null){
                        matches.add(match);
                    }
                } catch (Exception e) {
                    logger.error("Error finding  matches for consent.", e);
                    matches.add(createMatch(consentId, dar.getReferenceId(), true, false));
                }
            }
        }
        return matches;
    }

    private Match singleEntitiesMatch(Consent consent, DataAccessRequest dar) {
        if (consent == null) {
            logger.error("Consent is null");
            throw new IllegalArgumentException("Consent cannot be null");
        }
        if (dar == null) {
            logger.error("Data Access Request is null");
            throw new IllegalArgumentException("Data Access Request cannot be null");
        }
        Match match = createMatch(consent.getConsentId(), dar.getReferenceId(), false, false);
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

    private Consent findRelatedConsent(List<Integer> dataSetIdList) {
        Consent consent =  null;
        if (CollectionUtils.isNotEmpty(dataSetIdList)) {
            consent = consentAPI.getConsentFromDatasetID(dataSetIdList.get(0));
        }
        return consent;
    }

    private List<DataAccessRequest> findRelatedDars(List<Integer> dataSetIds){
        return dataAccessRequestDAO.findAllDataAccessRequests().stream().
            filter(d -> !Collections.disjoint(dataSetIds, d.getData().getDatasetIds())).
            collect(Collectors.toList());
    }

    private RequestMatchingObject createRequestObject(Consent consent, DataAccessRequest dar) {
        DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
        UseRestriction darUseRestriction = useRestrictionConverter.parseUseRestriction(dataUse);
        return new RequestMatchingObject(consent.getUseRestriction(), darUseRestriction);
    }
}
