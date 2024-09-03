package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.broadinstitute.consent.http.db.CounterDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CounterServiceTest {

  @Mock
  private CounterDAO counterDAO;

  private CounterService service;

  private void initService() {
    service = new CounterService(counterDAO);
  }

  @Test
  void testGetNextDarSequence() {
    int count = 10;
    when(counterDAO.incrementCountByName(any())).thenReturn(count);
    initService();

    Integer seq = service.getNextDarSequence();
    assertEquals(count, seq.intValue());
  }
}
