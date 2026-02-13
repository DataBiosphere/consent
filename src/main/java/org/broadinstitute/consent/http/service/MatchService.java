package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.Match.matchFailure;
import static org.broadinstitute.consent.http.models.Match.matchSuccess;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.matching.DataUseMatcherV4;
import org.broadinstitute.consent.http.matching.MatchResult;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class MatchService implements ConsentLogger {

  private final MatchDAO matchDAO;
  private final UseRestrictionConverter useRestrictionConverter;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final DataUseMatcherV4 dataUseMatcherV4 = new DataUseMatcherV4();

  @Inject
  public MatchService(
      ServicesConfiguration config,
      MatchDAO matchDAO,
      DataAccessRequestDAO dataAccessRequestDAO,
      DatasetDAO datasetDAO,
      UseRestrictionConverter useRestrictionConverter) {
    this.matchDAO = matchDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.useRestrictionConverter = useRestrictionConverter;
    this.datasetDAO = datasetDAO;
  }

  public void insertMatches(List<Match> match) {
    match.forEach(
        m -> {
          Integer id =
              matchDAO.insertMatch(
                  m.getConsent(),
                  m.getPurpose(),
                  m.getMatch(),
                  m.getFailed(),
                  new Date(),
                  m.getAlgorithmVersion(),
                  m.getAbstain());
          if (!m.getRationales().isEmpty()) {
            m.getRationales()
                .forEach(
                    f -> {
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
    dar.getDatasetIds()
        .forEach(
            id -> {
              Dataset dataset = datasetDAO.findDatasetById(id);
              if (Objects.nonNull(dataset)) {
                try {
                  matches.add(singleEntitiesMatch(dataset, dar));
                } catch (Exception e) {
                  String message =
                      "Error finding single match for purpose: " + dar.getReferenceId();
                  logWarn(message);
                  matches.add(
                      matchFailure(
                          dataset.getDatasetIdentifier(), dar.getReferenceId(), List.of(message)));
                }
              }
            });
    return matches;
  }

  public Match singleEntitiesMatch(Dataset dataset, DataAccessRequest dar) {
    if (dataset == null) {
      logWarn("Dataset is null");
      throw new IllegalArgumentException("Consent cannot be null");
    }
    if (dar == null) {
      logWarn("Data Access Request is null");
      throw new IllegalArgumentException("Data Access Request cannot be null");
    }
    DataUse darDataUse = useRestrictionConverter.parseDataUsePurpose(dar);
    if (darDataUse == null) {
      logWarn("Data Use for the provided Data Access Request is null");
      throw new IllegalArgumentException(
          "Data Use for the provided Data Access Request cannot be null");
    }
    MatchResult matchResult =
        dataUseMatcherV4.matchPurposeAndDatasetV4(darDataUse, dataset.getDataUse());
    return matchSuccess(
        dataset.getDatasetIdentifier(),
        dar.getReferenceId(),
        matchResult.getMatchResultType(),
        matchResult.getMessage());
  }

  public List<Match> findMatchesByPurposeId(String purposeId) {
    return matchDAO.findMatchesByPurposeId(purposeId);
  }
}
