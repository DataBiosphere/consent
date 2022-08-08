package org.broadinstitute.consent.http.service;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.exceptions.UnknownIdentifierException;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.matching.DataUseRequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.DataUseResponseMatchingObject;
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
    private final DatasetDAO datasetDAO;
    private final WebTarget matchServiceTargetV1;
    private final WebTarget matchServiceTargetV2;

    private final GenericType<ResponseMatchingObject> rmo = new GenericType<>(){};

    @Inject
    public MatchService(Client client, ServicesConfiguration config, ConsentDAO consentDAO, MatchDAO matchDAO,
                        ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO datasetDAO,
                        UseRestrictionConverter useRestrictionConverter) {
        this.matchDAO = matchDAO;
        this.consentDAO = consentDAO;
        this.electionDAO = electionDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.useRestrictionConverter = useRestrictionConverter;
        this.datasetDAO = datasetDAO;

        Integer timeout = 1000 * 60 * 3; // 3 minute timeout so ontology can properly do matching.
        client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
        client.property(ClientProperties.READ_TIMEOUT, timeout);
        matchServiceTargetV1 = client.target(config.getMatchURL());
        matchServiceTargetV2 = client.target(config.getMatchURL_v2());
    }

    public void insertMatches(List<Match> match) {
        match.forEach(m -> {
            Integer id = matchDAO.insertMatch(
                m.getConsent(),
                m.getPurpose(),
                m.getMatch(),
                m.getFailed(),
                new Date(),
                m.getAlgorithmVersion()
            );
            if (!m.getFailureReasons().isEmpty()) {
                m.getFailureReasons().forEach(f -> {
                    matchDAO.insertFailureReason(id, f);
                });
            }
        });
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

    public List<Match> findMatchesForLatestDataAccessElectionsByPurposeIds(List<String> purposeIds) {
        return matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(purposeIds);
    }

    public void reprocessMatchesForConsent(String consentId) {
        removeMatchesForConsent(consentId);
        if (!consentDAO.checkManualReview(consentId)) {
            List<Match> matches = createMatchesForConsent(consentId);
            insertMatches(matches);
        }
    }

    public void reprocessMatchesForPurpose(String purposeId) {
        removeMatchesForPurpose(purposeId);
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
        if (Objects.nonNull(dar)) {
            List<Match> matches = createMatchesForDataAccessRequest(dar);
            insertMatches(matches);
        }
    }

    public void removeMatchesForPurpose(String purposeId) {
        matchDAO.deleteFailureReasonsByPurposeIds(List.of(purposeId));
        matchDAO.deleteMatchesByPurposeId(purposeId);
    }

    public void removeMatchesForConsent(String consentId) {
        matchDAO.deleteFailureReasonsByConsentIds(List.of(consentId));
        matchDAO.deleteMatchesByConsentId(consentId);
    }

    protected List<Match> createMatchesForDataAccessRequest(DataAccessRequest dar) {
        List<Match> matches = new ArrayList<>();
        dar.getDatasetIds().forEach(id -> {
            Dataset dataset = datasetDAO.findDatasetById(id);
            if (Objects.nonNull(dataset)) {
                try {
                    matches.add(singleEntitiesMatchV2(dataset, dar));
                } catch (Exception e) {
                    String message = "Error finding single match for purpose: " + dar.getReferenceId();
                    logger.error(message);
                    matches.add(createMatch(dataset.getDatasetIdentifier(), dar.getReferenceId(), true, false, MatchAlgorithm.V2, List.of(message)));
                }
            }
        });
        return matches;
    }

    public List<Match> createMatchesForConsent(String consentId) {
        List<Match> matches = new ArrayList<>();
        Consent consent = findConsent(consentId);
        List<Dataset> dataSets = datasetDAO.getDatasetsForConsent(consentId);
        List<DataAccessRequest> dars = findRelatedDars(dataSets.stream().map(Dataset::getDataSetId).collect(Collectors.toList()));
        if (consent != null && !dars.isEmpty()) {
            Match match;
            for (DataAccessRequest dar : dars) {
                try {
                    match = singleEntitiesMatchV1(consent, dar);
                    matches.add(match);
                } catch (Exception e) {
                    logger.error("Error finding  matches for consent: " + consentId);
                    matches.add(createMatch(consentId, dar.getReferenceId(), true, false, MatchAlgorithm.V1, List.of()));
                }
            }
        }
        return matches;
    }

    private Match singleEntitiesMatchV1(Consent consent, DataAccessRequest dar) {
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
        Response res = matchServiceTargetV1.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            ResponseMatchingObject entity = res.readEntity(rmo);
            match = createMatch(consent.getConsentId(), dar.getReferenceId(), false, entity.isResult(), MatchAlgorithm.V1, List.of());
        } else {
            match = createMatch(consent.getConsentId(), dar.getReferenceId(), true, false, MatchAlgorithm.V1, List.of());
        }
        return match;
    }

    private Match singleEntitiesMatchV2(Dataset dataset, DataAccessRequest dar) {
        if (Objects.isNull(dataset)) {
            logger.error("Dataset is null");
            throw new IllegalArgumentException("Consent cannot be null");
        }
        if (Objects.isNull(dar)) {
            logger.error("Data Access Request is null");
            throw new IllegalArgumentException("Data Access Request cannot be null");
        }
        Match match;
        DataUseRequestMatchingObject requestObject = createRequestObject(dataset, dar);
        String json = new Gson().toJson(requestObject);
        Response res = matchServiceTargetV2.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            GenericType<DataUseResponseMatchingObject> durmo = new GenericType<>(){};
            DataUseResponseMatchingObject entity = res.readEntity(durmo);
            match = createMatch(dataset.getDatasetIdentifier(), dar.getReferenceId(), false, entity.isResult(), MatchAlgorithm.V2, entity.getFailureReasons());
        } else {
            match = createMatch(dataset.getDatasetIdentifier(), dar.getReferenceId(), true, false, MatchAlgorithm.V2, List.of());
        }
        return match;
    }

    private Match createMatch(String consentId, String purposeId, boolean failed, boolean isMatch, MatchAlgorithm algorithm, List<String> failureReasons) {
        Match match = new Match();
        match.setConsent(consentId);
        match.setPurpose(purposeId);
        match.setFailed(failed);
        match.setMatch(isMatch);
        match.setCreateDate(new Date());
        match.setAlgorithmVersion(algorithm.getVersion());
        match.setFailureReasons(failureReasons);
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

    private List<DataAccessRequest> findRelatedDars(List<Integer> dataSetIds) {
        return dataAccessRequestDAO.findAllDataAccessRequests().stream()
                .filter(d -> !Collections.disjoint(dataSetIds, d.getDatasetIds()))
                .collect(Collectors.toList());
    }


    private RequestMatchingObject createRequestObject(Consent consent, DataAccessRequest dar) {
        DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
        UseRestriction darUseRestriction = useRestrictionConverter.parseUseRestriction(dataUse, DataUseTranslationType.PURPOSE);
        return new RequestMatchingObject(consent.getUseRestriction(), darUseRestriction);
    }

    private DataUseRequestMatchingObject createRequestObject(Dataset dataset, DataAccessRequest dar) {
        DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
        return new DataUseRequestMatchingObject(dataset.getDataUse(), dataUse);
    }


    public List<Match> findMatchesByPurposeId(String purposeId) {
        return matchDAO.findMatchesByPurposeId(purposeId);
    }
}
