package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Counter;
import org.broadinstitute.consent.http.service.CounterService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import static org.junit.Assert.assertEquals;

public class CounterDAOTest extends DAOTestHelper {

    @After
    public void tearDown() {
        counterDAO.deleteAll();
    }

    @Test
    public void testIncrementDarCounter() {
        counterDAO.insertCounter(CounterService.DAR_COUNTER, 0);
        int count = 5;
        for (int i = 0; i < count; i++) {
            counterDAO.incrementCounter(CounterService.DAR_COUNTER);
        }
        Integer lastCount = counterDAO.getLastCountByName(CounterService.DAR_COUNTER);
        assertEquals(Integer.valueOf(count), lastCount);
    }

    @Test
    public void testIncrementRandomCounter() {
        String name = RandomStringUtils.random(10, true, false);
        counterDAO.insertCounter(name, 0);
        int count = 5;
        for (int i = 0; i < count; i++) {
            counterDAO.incrementCounter(name);
        }
        Integer lastId = counterDAO.getLastCountByName(name);
        Counter last = counterDAO.getCounterById(lastId);
        assertEquals(count, last.getId().intValue());
        assertEquals(count, last.getCount().intValue());
    }

}
