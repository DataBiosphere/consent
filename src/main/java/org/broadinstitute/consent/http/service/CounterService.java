package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.jdbi.v3.core.Jdbi;

public class CounterService {

  private final CounterDAO counterDAO;
  public static final String DAR_COUNTER = "DAR";

  @Inject
  public CounterService(Jdbi jdbi) {
    this.counterDAO = jdbi.onDemand(CounterDAO.class);
  }

  public Integer getNextDarSequence() {
    return counterDAO.incrementCountByName(DAR_COUNTER);
  }

  public Integer getNextDarSequence(CounterDAO transactionalCounterDAO) {
    return transactionalCounterDAO.incrementCountByName(DAR_COUNTER);
  }
}
