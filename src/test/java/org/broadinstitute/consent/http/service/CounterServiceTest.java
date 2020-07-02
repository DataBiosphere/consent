package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
            service.setMaxDarCount();
        } catch (Exception e) {
            fail("Failed: " + e);
        }
    }

    @Test
    public void testFindMaxDarCode() {
        int min = 1;
        int max = 201;
        List<String> darCodes = generateDarCodes(min, max);
        Collections.shuffle(darCodes);
        when(counterDAO.findAllDarCodes()).thenReturn(darCodes);
        initService();
        Integer maxCode = service.findMaxDarCodeValue();
        assertEquals(max, maxCode + 1);
    }

    private List<String> generateDarCodes(int min, int max) {
        return IntStream.range(min, max)
            .mapToObj(i -> {
                if (i % 2 == 0) {
                    return Collections.singletonList("DAR-" + i);
                } else {
                    return Arrays.asList("DAR-" + i + "-A-0", "DAR-" + i + "-A-1", "DAR-" + i + "-A-2");
                }
            }).
            flatMap(List::stream).
            collect(Collectors.toList());
    }

}
