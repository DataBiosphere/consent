package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.CounterDAO;

public class CounterService {

    private final CounterDAO counterDAO;
    public static final String DAR_COUNTER = "DAR";

    @Inject
    public CounterService(CounterDAO counterDAO) {
        this.counterDAO = counterDAO;
    }

    public Integer getNextDarSequence() {
        counterDAO.incrementCountByName(DAR_COUNTER);
        return counterDAO.getMaxCountByName(DAR_COUNTER);
    }

}
