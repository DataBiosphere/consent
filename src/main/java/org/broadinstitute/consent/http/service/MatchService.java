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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
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
  private final UseRestrictionConverter useRestrictionConverter;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final WebTarget matchServiceTargetV4;

  @Inject
  public MatchService(Client client, ServicesConfiguration config, MatchDAO matchDAO,
      DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO datasetDAO,
      UseRestrictionConverter useRestrictionConverter) {
    this.matchDAO = matchDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.useRestrictionConverter = useRestrictionConverter;
    this.datasetDAO = datasetDAO;

    Integer timeout = 1000 * 60 * 3; // 3 minute timeout so ontology can properly do matching.
    client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
    client.property(ClientProperties.READ_TIMEOUT, timeout);
    matchServiceTargetV4 = client.target(config.getMatchURL_v4());
  }

  public void insertMatches(List<Match> match) {
    match.forEach(m -> {
      Integer id = matchDAO.insertMatch(
          m.getConsent(),
          m.getPurpose(),
          m.getMatch(),
          m.getFailed(),
          new Date(),
          m.getAlgorithmVersion(),
          m.getAbstain()
      );
      if (!m.getRationales().isEmpty()) {
        m.getRationales().forEach(f -> {
          matchDAO.insertRationale(id, f);
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

  public void reprocessMatchesForPurpose(String purposeId) {
    removeMatchesForPurpose(purposeId);
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
    if (Objects.nonNull(dar)) {
      List<Match> matches = createMatchesForDataAccessRequest(dar);
      insertMatches(matches);
    }
  }

  public void removeMatchesForPurpose(String purposeId) {
    matchDAO.deleteRationalesByPurposeIds(List.of(purposeId));
    matchDAO.deleteMatchesByPurposeId(purposeId);
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
          matches.add(
              matchFailure(dataset.getDatasetIdentifier(), dar.getReferenceId(), List.of(message)));
        }
      }
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
    Response res = matchServiceTargetV4.request(MediaType.APPLICATION_JSON).post(Entity.json(json));
    String datasetId = dataset.getDatasetIdentifier();
    String darReferenceId = dar.getReferenceId();
    if (res.getStatus() == Response.Status.OK.getStatusCode()) {
      String stringEntity = res.readEntity(String.class);
      DataUseResponseMatchingObject entity = new Gson().fromJson(stringEntity,
          DataUseResponseMatchingObject.class);
      match = matchSuccess(datasetId, darReferenceId, entity.getResult(),
          entity.getRationale());
    } else {
      match = matchFailure(datasetId, darReferenceId, List.of());
    }
    return match;
  }

  private DataUseRequestMatchingObject createRequestObject(Dataset dataset, DataAccessRequest dar) {
    DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
    return new DataUseRequestMatchingObject(dataset.getDataUse(), dataUse);
  }


  public List<Match> findMatchesByPurposeId(String purposeId) {
    return matchDAO.findMatchesByPurposeId(purposeId);
  }
}
