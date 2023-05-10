package org.broadinstitute.consent.http.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DatasetMetrics;
import org.broadinstitute.consent.http.models.Type;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class MetricsResourceTest {

    private final String darHeader = "DAR ID";

    private final String dacHeader = "DAC ID";

    @Mock
    private MetricsService service;

    private MetricsResource resource;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new MetricsResource(service);
    }

    @Test
    public void testGetDarMetricsData() {
        when(service.generateDecisionMetrics(Type.DAR)).thenReturn(Collections.emptyList());
        when(service.getHeaderRow(Type.DAR)).thenReturn(darHeader);
        initResource();
        Response response = resource.getDarMetricsData();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertFalse(response.getEntity().toString().isEmpty());
        Assertions.assertTrue(
            response.getEntity().toString().contains(service.getHeaderRow(Type.DAR)));
    }

    @Test
    public void testGetDacMetricsData() {
        when(service.generateDecisionMetrics(Type.DAC)).thenReturn(Collections.emptyList());
        when(service.getHeaderRow(Type.DAC)).thenReturn(dacHeader);
        initResource();
        Response response = resource.getDacMetricsData();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertFalse(response.getEntity().toString().isEmpty());
        String headerRow = service.getHeaderRow(Type.DAC);
        Assertions.assertTrue(response.getEntity().toString().contains(headerRow));
    }

    @Test
    public void testGetDatasetMetricsData() {
        DatasetMetrics metrics = new DatasetMetrics();
        when(service.generateDatasetMetrics(any())).thenReturn(metrics);
        initResource();
        Response response = resource.getDatasetMetricsData(1);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertFalse(response.getEntity().toString().isEmpty());
    }

    @Test
    public void testGetDatasetMetricsDataNotFound() {
        when(service.generateDatasetMetrics(any())).thenThrow(new NotFoundException());
        initResource();
        Response response = resource.getDatasetMetricsData(1);
        Assertions.assertEquals(404, response.getStatus());
    }
}
