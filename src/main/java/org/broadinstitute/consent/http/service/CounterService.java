package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.broadinstitute.consent.http.models.Counter;

public class CounterService {

    private final CounterDAO counterDAO;
    public static final String DAR_COUNTER = "DAR";

    @Inject
    public CounterService(CounterDAO counterDAO) {
        this.counterDAO = counterDAO;
    }

    public String getNextDarSequence() {
        counterDAO.incrementCounter(DAR_COUNTER);
        Integer count = counterDAO.getLastCountByName(DAR_COUNTER);
        return String.valueOf(count);
    }

}
