package org.broadinstitute.consent.http.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.MatchMigrationDAO;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.jdbi.v3.core.Jdbi;

/**
 * Reports on the match rows that still identify their dataset by legacy text, ahead of the one-off
 * migration that gives them a real {@code dataset_id}.
 */
public class MatchMigrationService {

  private final MatchMigrationDAO matchMigrationDAO;

  @Inject
  public MatchMigrationService(Jdbi jdbi) {
    this(jdbi.onDemand(MatchMigrationDAO.class));
  }

  @VisibleForTesting
  MatchMigrationService(MatchMigrationDAO matchMigrationDAO) {
    this.matchMigrationDAO = matchMigrationDAO;
  }

  /** Read-only. What a run would do, and whether the constraints would still be refused. */
  public MatchMigrationPopulation findPopulation() {
    return matchMigrationDAO.findPopulation();
  }
}
