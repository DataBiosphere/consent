package org.broadinstitute.consent.http.health;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.storage.Bucket;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GCSHealthCheckTest {

  private GCSHealthCheck healthCheck;

  @Mock
  private GCSService store;

  @Mock
  private Bucket bucket;

  @BeforeEach
  void setUpClass() {
    healthCheck = new GCSHealthCheck(store);
  }

  @Test
  void testBucketExists() {
    when(store.getRootBucketWithMetadata()).thenReturn(bucket);

    HealthCheck.Result result = healthCheck.execute();
    assertTrue(result.isHealthy());
  }

  @Test
  void testBucketMissing() {
    when(store.getRootBucketWithMetadata()).thenReturn(null);

    HealthCheck.Result result = healthCheck.execute();
    assertFalse(result.isHealthy());
    assertTrue(result.getMessage().contains("GCS bucket unreachable"));
  }

  @Test
  void testException() {
    doThrow(new RuntimeException()).when(store).getRootBucketWithMetadata();

    HealthCheck.Result result = healthCheck.execute();
    assertFalse(result.isHealthy());
    assertTrue(result.getMessage().contains("GCS bucket unreachable"));
  }
}
