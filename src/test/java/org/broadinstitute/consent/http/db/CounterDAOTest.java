package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.service.CounterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CounterDAOTest extends DAOTestHelper {

    @AfterEach
    public void tearDown() {
        counterDAO.deleteAll();
    }

    @Test
    public void testIncrementDarCounter() {
        counterDAO.addCounter(CounterService.DAR_COUNTER, 0);
        int count = 5;
        Integer lastCount = 0;
        for (int i = 0; i < count; i++) {
            lastCount = counterDAO.incrementCountByName(CounterService.DAR_COUNTER);
        }
        Assertions.assertEquals(Integer.valueOf(count), lastCount);
    }

    @Test
    public void testIncrementRandomCounter() {
        String name = RandomStringUtils.random(10, true, false);
        counterDAO.addCounter(name, 0);
        int count = 5;
        Integer maxCount = 0;
        for (int i = 0; i < count; i++) {
            maxCount = counterDAO.incrementCountByName(name);
        }
        Assertions.assertEquals(count, maxCount.intValue());
    }
}
