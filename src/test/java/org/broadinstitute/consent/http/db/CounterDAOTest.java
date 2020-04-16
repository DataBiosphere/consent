package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Counter;
import org.broadinstitute.consent.http.service.CounterService;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import static org.junit.Assert.assertEquals;

public class CounterDAOTest extends DAOTestHelper {

    private static final String NAME = CounterService.DAR_COUNTER;

    @Test
    public void testIncrementDarCounter() {
        Integer counterId = counterDAO.incrementCounter(NAME);
        Counter counter1 = counterDAO.getCounterById(counterId);
        Integer lastId = counterDAO.getLastCountByName(NAME);
        Counter last = counterDAO.getCounterById(lastId);
        assertEquals(counter1.getId(), last.getId());
        assertEquals(counter1.getCount(), last.getCount());
    }

    @Test
    public void testIncrementRandomCounter() {
        String name = RandomStringUtils.random(10, true, false);
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
