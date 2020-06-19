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

    /**
     * TODO: Remove in follow-up work
     * Temporary admin-only endpoint for mongo->postgres DAR conversion
     */
    public Integer getCurrentMaxDarSequence() {
        return counterDAO.getMaxCountByName(DAR_COUNTER);
    }

    /**
     * TODO: Remove in follow-up work
     * Temporary admin-only endpoint for mongo->postgres DAR conversion
     * Updates the DAR Counter to be the maximum `dar_code` value of all current DARs
     */
    public void setMaxDarCount() {
        Integer maxDarCode = findMaxDarCodeValue();
        Integer max = counterDAO.getMaxCountByName(DAR_COUNTER);
        if (Objects.isNull(max)) {
            counterDAO.addCounter(DAR_COUNTER, maxDarCode);
        } else {
            counterDAO.setCountByName(maxDarCode, DAR_COUNTER);
        }
    }

  /**
   * TODO: Remove in follow-up work
   * Dar codes come in the flavors of:
   * DAR-142-A-0, DAR-142-A-1, DAR-142-A-2, DAR-60 DAR-17, DAR-144-A-0, DAR-89, DAR-120, etc.
   * Parse out the second element and find the max.
   */
  public Integer findMaxDarCodeValue() {
        List<String> codes = counterDAO.findAllDarCodes();
        return codes.stream().
            map(s -> {
                String[] parts = s.split("-");
                if (parts.length > 0) {
                    return parts[1];
                }
                return null;
            }).
            filter(Objects::nonNull).
            filter(StringUtils::isNotEmpty).
            map(Integer::parseInt).
            sorted().
            reduce((first, second) -> second).
            orElse(0);
    }

}
