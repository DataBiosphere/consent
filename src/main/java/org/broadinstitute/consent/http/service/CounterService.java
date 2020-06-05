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

    public String getNextDarSequence() {
        counterDAO.incrementCountByName(DAR_COUNTER);
        Integer count = counterDAO.getMaxCountByName(DAR_COUNTER);
        return String.valueOf(count);
    }

    public void setDarCount(Integer count) {
        counterDAO.setCountByName(count, DAR_COUNTER);
    }

}
