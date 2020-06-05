package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.CounterDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class CounterServiceTest {

    @Mock
    private CounterDAO counterDAO;

    private CounterService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new CounterService(counterDAO);
    }

    @Test
    public void testGetNextDarSequence() {
        int count = 10;
        doNothing().when(counterDAO).incrementCountByName(any());
        when(counterDAO.getMaxCountByName(any())).thenReturn(count);
        initService();

        Integer seq = service.getNextDarSequence();
        assertEquals(count, seq.intValue());
    }

    @Test
    public void testSetDarCount() {
        doNothing().when(counterDAO).setCountByName(any(), any());
        initService();
        try {
            service.setDarCount(10);
        } catch (Exception e) {
            fail("Failed: " + e);
        }
    }

}
