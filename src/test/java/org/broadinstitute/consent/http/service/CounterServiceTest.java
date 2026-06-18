package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.broadinstitute.consent.http.db.CounterDAO;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CounterServiceTest {

  @Mock private Jdbi jdbi;
  @Mock private CounterDAO counterDAO;

  private CounterService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(CounterDAO.class)).thenReturn(counterDAO);
    service = new CounterService(jdbi);
  }

  @Test
  void testGetNextDarSequence() {
    int count = 10;
    when(counterDAO.incrementCountByName(any())).thenReturn(count);

    Integer seq = service.getNextDarSequence();
    assertEquals(count, seq.intValue());
  }
}
