package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.service.CounterService;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CounterDAOTest extends DAOTestHelper {

  @After
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
    assertEquals(Integer.valueOf(count), lastCount);
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
    assertEquals(count, maxCount.intValue());
  }
}
