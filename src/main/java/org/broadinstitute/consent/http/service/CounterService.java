package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.CounterDAO;

public class CounterService {

  private final CounterDAO counterDAO;
  public static final String DAR_COUNTER = "DAR";

  @Inject
  public CounterService(CounterDAO counterDAO) {
    this.counterDAO = counterDAO;
  }

  public Integer getNextDarSequence() {
    return counterDAO.incrementCountByName(DAR_COUNTER);
  }

}
