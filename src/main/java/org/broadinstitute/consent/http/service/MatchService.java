package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.matching.RequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.ResponseMatchingObject;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MatchService {
    private final MatchDAO matchDAO;
    private final ConsentDAO consentDAO;
    private final ElectionDAO electionDAO;
    private final UseRestrictionConverter useRestrictionConverter;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DatasetDAO dataSetDAO;
    private final WebTarget matchServiceTarget;

    private final GenericType<ResponseMatchingObject> rmo = new GenericType<>(){};

    @Inject
    public MatchService(Client client, ServicesConfiguration config, ConsentDAO consentDAO, MatchDAO matchDAO,
                        ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO dataSetDAO,
                        UseRestrictionConverter useRestrictionConverter) {
        this.matchDAO = matchDAO;
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.useRestrictionConverter = useRestrictionConverter;
        this.dataSetDAO = dataSetDAO;

        Integer timeout = 1000 * 60 * 3; // 3 minute timeout so ontology can properly do matching.
        client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
        client.property(ClientProperties.READ_TIMEOUT, timeout);
        matchServiceTarget = client.target(config.getMatchURL());
    }

    public Match create(Match match){
        validateConsent(match.getConsent());
        validatePurpose(match.getPurpose());
        try{
            Integer id = matchDAO.insertMatch(match.getConsent(), match.getPurpose(), match.getMatch(), match.getFailed(), new Date());
            return findMatchById(id);
        }catch (Exception e){
            throw new IllegalArgumentException("Already exist a match for the specified consent and purpose");
        }
    }

    public void createMatches(List<Match> match){
        if(CollectionUtils.isNotEmpty(match)){
            matchDAO.insertAll(match);
        }
    }

    public Match update(Match match, Integer id) {
        validateConsent(match.getConsent());
        validatePurpose(match.getPurpose());
        if (matchDAO.findMatchById(id) == null)
            throw new NotFoundException("Match for the specified id does not exist");
        matchDAO.updateMatch(id, match.getMatch(), match.getConsent(), match.getPurpose(), match.getFailed());
        return findMatchById(id);
    }

    public Match findMatchById(Integer id) {
        Match match = matchDAO.findMatchById(id);
        if (match == null) {
            throw new NotFoundException("Match for the specified id does not exist");
        }
        return match;
    }

    public Match findMatchByConsentIdAndPurposeId(String consentId, String purposeId) {
        Match match = matchDAO.findMatchByPurposeIdAndConsentId(purposeId, consentId);
        if (Objects.isNull(match)) {
            throw new NotFoundException("No match exists for consent id: " + consentId + " and purpose id: " + purposeId);
        }
        return match;
    }

    public List<Match> findMatchByConsentId(String consentId) {
        return matchDAO.findMatchesByConsentId(consentId);
    }

    public void reprocessMatchesForConsent(String consentId) {
        removeMatchesForConsent(consentId);
        if (!consentDAO.checkManualReview(consentId)) {
            List<Match> matches = createMatchesForConsent(consentId);
            createMatches(matches);
        }
    }

    public void reprocessMatchesForPurpose(String purposeId) {
        removeMatchesForPurpose(purposeId);
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
        if (Objects.nonNull(dar)) {
            Match match = createMatchesForPurpose(dar.getReferenceId());
            createMatches(Collections.singletonList(match));
        }

    }

    public void removeMatchesForPurpose(String purposeId) {
        matchDAO.deleteMatchesByPurposeId(purposeId);
    }

    public void removeMatchesForConsent(String consentId) {
        matchDAO.deleteMatchesByConsentId(consentId);
    }

    public Match findSingleMatch(String consentId, String purposeId) {
        Match match = null;
        try {
            Consent consent = findConsent(consentId);
            DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
            if(consent != null || dar != null) {
                match = singleEntitiesMatch(consent, dar);
            }
        } catch (Exception e) {
            logger.error("Error finding single match for consent: " + consentId + " and purpose: " + purposeId);
            return null;
        }
        return match;
    }

    public Match createMatchesForPurpose(String purposeId){
        Match match = null;
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
        if (Objects.nonNull(dar)) {
            List<Integer> dataSetIdList = dar.getData().getDatasetIds();
            Consent consent = findRelatedConsent(dataSetIdList);
            if (Objects.nonNull(consent)) {
                try {
                    match = singleEntitiesMatch(consent, dar);
                } catch (Exception e) {
                    logger.error("Error finding single match for purpose: " + purposeId);
                    match = createMatch(consent.getConsentId(), purposeId, true, false);
                }
            }
        }
        return match;
    }

    public List<Match> createMatchesForConsent(String consentId) {
        List<Match> matches = new ArrayList<>();
        Consent consent = findConsent(consentId);
        List<DataSet> dataSets = dataSetDAO.getDataSetsForConsent(consentId);
        List<DataAccessRequest> dars = findRelatedDars(dataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList()));
        if (consent != null && !dars.isEmpty()) {
            Match match;
            for (DataAccessRequest dar : dars) {
                try {
                    match = singleEntitiesMatch(consent, dar);
                    matches.add(match);
                } catch (Exception e) {
                    logger.error("Error finding  matches for consent: " + consentId);
                    matches.add(createMatch(consentId, dar.getReferenceId(), true, false));
                }
            }
        }
        return matches;
    }

    private void validateConsent(String consentId) {
        if (StringUtils.isEmpty(consentDAO.checkConsentById(consentId))) {
            throw new IllegalArgumentException("Consent for the specified id does not exist");
        }
    }

    private void validatePurpose(String purposeId) {
        if (dataAccessRequestDAO.findByReferenceId(purposeId) == null) {
            throw new IllegalArgumentException("Purpose for the specified id does not exist");
        }
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
        Match match;
        RequestMatchingObject requestObject = createRequestObject(consent, dar);
        String json = new Gson().toJson(requestObject);
        Response res = matchServiceTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            ResponseMatchingObject entity = res.readEntity(rmo);
            match = createMatch(consent.getConsentId(), dar.getReferenceId(), false, entity.isResult());
        } else {
            match = createMatch(consent.getConsentId(), dar.getReferenceId(), true, false);
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
            consent = consentDAO.findConsentById(consentId);
            if (Objects.isNull(consent)) {
                throw new UnknownIdentifierException(String.format("Could not find consent with id %s", consentId));
            }

            Election election = electionDAO.findLastElectionByReferenceIdAndType(consentId, ElectionType.TRANSLATE_DUL.getValue());
            if (election != null) {
                consent.setLastElectionStatus(election.getStatus());
                consent.setLastElectionArchived(election.getArchived());
            }
        } catch (UnknownIdentifierException e) {
            logger.error("Consent for specified id does not exist: " + consentId);
        }
        return consent;
    }

    private Consent findRelatedConsent(List<Integer> dataSetIdList) {
        Consent consent =  null;
        if (CollectionUtils.isNotEmpty(dataSetIdList)) {
            consent = consentDAO.findConsentFromDatasetID(dataSetIdList.get(0));
        }
        return consent;
    }

    private List<DataAccessRequest> findRelatedDars(List<Integer> dataSetIds) {
        return dataAccessRequestDAO.findAllDataAccessRequests().stream()
                .filter(d -> !Collections.disjoint(dataSetIds, d.getData().getDatasetIds()))
                .collect(Collectors.toList());
    }

    private RequestMatchingObject createRequestObject(Consent consent, DataAccessRequest dar) {
        DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
        UseRestriction darUseRestriction = useRestrictionConverter.parseUseRestriction(dataUse);
        return new RequestMatchingObject(consent.getUseRestriction(), darUseRestriction);
    }

    public List<Match> findMatchesByPurposeId(String purposeId) {
        return matchDAO.findMatchesByPurposeId(purposeId);
    }
}
