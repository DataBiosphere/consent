package org.broadinstitute.consent.http.cloudstore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.storage.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GCSHealthCheckTest {

    private GCSHealthCheck healthCheck;

    @Mock
    private GCSService store;

    @Mock
    private Bucket bucket;

    @Before
    public void setUpClass() {
        MockitoAnnotations.initMocks(this);
        healthCheck = new GCSHealthCheck(store);
    }

    @Test
    public void testBucketExists() {
        when(store.getRootBucketWithMetadata()).thenReturn(bucket);

        HealthCheck.Result result = healthCheck.execute();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testBucketMissing() {
        when(store.getRootBucketWithMetadata()).thenReturn(null);

        HealthCheck.Result result = healthCheck.execute();
        assertFalse(result.isHealthy());
        assertTrue(result.getMessage().contains("GCS bucket unreachable"));
    }
}
