package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.Match.matchFailure;
import static org.broadinstitute.consent.http.models.Match.matchSuccess;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.matching.DataUseMatcherV5;
import org.broadinstitute.consent.http.matching.MatchResult;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class MatchService implements ConsentLogger {

  private final MatchDAO matchDAO;
  private final UseRestrictionConverter useRestrictionConverter;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final DataUseMatcherV5 dataUseMatcherV5;

  @Inject
  public MatchService(
      Jdbi jdbi,
      UseRestrictionConverter useRestrictionConverter,
      DataUseMatcherV5 dataUseMatcherV5) {
    this.matchDAO = jdbi.onDemand(MatchDAO.class);
    this.dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.useRestrictionConverter = useRestrictionConverter;
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.dataUseMatcherV5 = dataUseMatcherV5;
  }

  public void insertMatches(List<Match> match) {
    insertMatches(matchDAO, match);
  }

  private static void insertMatches(MatchDAO dao, List<Match> match) {
    match.forEach(
        m -> {
          Integer id =
              dao.insertMatch(
                  m.getConsent(),
                  m.getPurpose(),
                  m.getMatch(),
                  m.getFailed(),
                  new Date(),
                  m.getAlgorithmVersion(),
                  m.getAbstain());
          if (!m.getRationales().isEmpty()) {
            m.getRationales().forEach(f -> dao.insertRationale(id, f));
          }
        });
  }

  public List<Match> findMatchesForLatestDataAccessElectionsByPurposeIds(List<String> purposeIds) {
    return matchDAO.findMatchesForLatestDataAccessElectionsByPurposeIds(purposeIds);
  }

  /**
   * The rebuild is computed before anything is deleted and both halves share one transaction, so a
   * failure cannot leave the purpose with its matches removed and nothing put back.
   */
  public void reprocessMatchesForPurpose(String purposeId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(purposeId);
    List<Match> matches = Objects.nonNull(dar) ? createMatchesForDataAccessRequest(dar) : List.of();
    matchDAO.useTransaction(
        dao -> {
          removeMatchesForPurpose(dao, purposeId);
          insertMatches(dao, matches);
        });
  }

  public void removeMatchesForPurpose(String purposeId) {
    removeMatchesForPurpose(matchDAO, purposeId);
  }

  private static void removeMatchesForPurpose(MatchDAO dao, String purposeId) {
    dao.deleteRationalesByPurposeIds(List.of(purposeId));
    dao.deleteMatchesByPurposeId(purposeId);
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
                } catch (Exception _) {
                  String message =
                      "Error finding single match for purpose: " + dar.getReferenceId();
                  logWarn(message);
                  matches.add(
                      matchFailure(
                          dataset.getDatasetIdentifier(),
                          dar.getReferenceId(),
                          MatchAlgorithm.V5,
                          List.of(message)));
                }
              }
            });
    return matches;
  }

  public Match singleEntitiesMatch(Dataset dataset, DataAccessRequest dar) {
    if (dataset == null) {
      logWarn("Dataset is null");
      throw new IllegalArgumentException("Dataset cannot be null");
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
        dataUseMatcherV5.matchPurposeAndDatasetV5(darDataUse, dataset.getDataUse());
    return matchSuccess(
        dataset.getDatasetIdentifier(),
        dar.getReferenceId(),
        matchResult.getMatchResultType(),
        MatchAlgorithm.V5,
        matchResult.getMessage());
  }

  public List<Match> findMatchesByPurposeId(String purposeId) {
    return matchDAO.findMatchesByPurposeId(purposeId);
  }
}
