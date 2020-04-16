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
        Integer counterId = counterDAO.incrementCounter(DAR_COUNTER);
        Counter counter = counterDAO.getCounterById(counterId);
        return DAR_COUNTER + "-" + counter.getCount();
    }

}
