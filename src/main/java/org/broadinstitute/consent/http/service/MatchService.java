package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.Match.matchFailure;
import static org.broadinstitute.consent.http.models.Match.matchSuccess;

import com.google.gson.Gson;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.matching.DataUseRequestMatchingObject;
import org.broadinstitute.consent.http.models.matching.DataUseResponseMatchingObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.glassfish.jersey.client.ClientProperties;

public class MatchService implements ConsentLogger {
    private final MatchDAO matchDAO;
    private final ConsentDAO consentDAO;
    private final UseRestrictionConverter useRestrictionConverter;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DatasetDAO datasetDAO;
    private final WebTarget matchServiceTargetV3;

    @Inject
    public MatchService(Client client, ServicesConfiguration config, ConsentDAO consentDAO, MatchDAO matchDAO,
                        DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO datasetDAO,
                        UseRestrictionConverter useRestrictionConverter) {
        this.matchDAO = matchDAO;
        this.consentDAO = consentDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.useRestrictionConverter = useRestrictionConverter;
        this.datasetDAO = datasetDAO;

        Integer timeout = 1000 * 60 * 3; // 3 minute timeout so ontology can properly do matching.
        client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
        client.property(ClientProperties.READ_TIMEOUT, timeout);
        matchServiceTargetV3 = client.target(config.getMatchURL_v3());
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
                    matches.add(singleEntitiesMatchV3(dataset, dar));
                } catch (Exception e) {
                    String message = "Error finding single match for purpose: " + dar.getReferenceId();
                    logWarn(message);
                    matches.add(matchFailure(dataset.getDatasetIdentifier(), dar.getReferenceId(), List.of(message)));
                }
            }
        });
        return matches;
    }

    public List<Match> createMatchesForConsent(String consentId) {
        List<Match> matches = new ArrayList<>();
        List<Dataset> datasets = datasetDAO.getDatasetsForConsent(consentId);
        if (datasets.isEmpty()) {
            logWarn("Error finding  datasets for consent: " + consentId);
            return matches;
        }
        datasets.forEach(d -> {
            List<DataAccessRequest> dars = findRelatedDars(List.of(d.getDataSetId()));
            dars.forEach(dar -> {
                try {
                    matches.add(singleEntitiesMatchV3(d, dar));
                } catch (Exception e) {
                    logWarn("Error finding matches for consent: " + consentId);
                    matches.add(matchFailure(consentId, dar.getReferenceId(), List.of()));
                }
            });
        });
        return matches;
    }

    public Match singleEntitiesMatchV3(Dataset dataset, DataAccessRequest dar) {
        if (Objects.isNull(dataset)) {
            logWarn("Dataset is null");
            throw new IllegalArgumentException("Consent cannot be null");
        }
        if (Objects.isNull(dar)) {
            logWarn("Data Access Request is null");
            throw new IllegalArgumentException("Data Access Request cannot be null");
        }
        Match match;
        DataUseRequestMatchingObject requestObject = createRequestObject(dataset, dar);
        String json = new Gson().toJson(requestObject);
        Response res = matchServiceTargetV3.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
        String datasetId = dataset.getDatasetIdentifier();
        String darReferenceId = dar.getReferenceId();
        if (res.getStatus() == Response.Status.OK.getStatusCode()) {
            String stringEntity = res.readEntity(String.class);
            DataUseResponseMatchingObject entity = new Gson().fromJson(stringEntity, DataUseResponseMatchingObject.class);
            match = matchSuccess(datasetId, darReferenceId, entity.getResult(), entity.getFailureReasons());
        } else {
            match = matchFailure(datasetId, darReferenceId, List.of());
        }
        return match;
    }

    private List<DataAccessRequest> findRelatedDars(List<Integer> dataSetIds) {
        return dataAccessRequestDAO.findAllDataAccessRequests().stream()
                .filter(d -> !Collections.disjoint(dataSetIds, d.getDatasetIds()))
                .collect(Collectors.toList());
    }

    private DataUseRequestMatchingObject createRequestObject(Dataset dataset, DataAccessRequest dar) {
        DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
        return new DataUseRequestMatchingObject(dataset.getDataUse(), dataUse);
    }


    public List<Match> findMatchesByPurposeId(String purposeId) {
        return matchDAO.findMatchesByPurposeId(purposeId);
    }
}
