package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;

import org.broadinstitute.consent.http.service.CounterService;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

public class CounterDAOTest extends DAOTestHelper {

    @After
    public void tearDown() {
        counterDAO.deleteAll();
    }

    @Test
    public void testIncrementDarCounter() {
        counterDAO.addCounter(CounterService.DAR_COUNTER, 0);
        int count = 5;
        for (int i = 0; i < count; i++) {
            counterDAO.incrementCountByName(CounterService.DAR_COUNTER);
        }
        Integer lastCount = counterDAO.getMaxCountByName(CounterService.DAR_COUNTER);
        assertEquals(Integer.valueOf(count), lastCount);
    }

    @Test
    public void testIncrementRandomCounter() {
        String name = RandomStringUtils.random(10, true, false);
        counterDAO.addCounter(name, 0);
        int count = 5;
        for (int i = 0; i < count; i++) {
            counterDAO.incrementCountByName(name);
        }
        Integer maxCount = counterDAO.getMaxCountByName(name);
        assertEquals(count, maxCount.intValue());
    }

}
