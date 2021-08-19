package org.broadinstitute.consent.http.cloudstore;

import com.codahale.metrics.health.HealthCheck;
import com.google.cloud.storage.Bucket;

public class GCSHealthCheck extends HealthCheck {

    private final GCSService store;

    public GCSHealthCheck(GCSService store) {
        this.store = store;
    }

    @Override
    protected Result check() {

        Bucket bucket;

        try {
            bucket = store.getRootBucketWithMetadata();
        } catch (Exception e) {
            return Result.unhealthy("GCS bucket unreachable or does not exist: " + e.getMessage());
        }

        return (bucket != null) ? Result.healthy() : Result.unhealthy("GCS bucket unreachable or does not exist.");
    }

}
