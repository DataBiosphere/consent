package org.broadinstitute.consent.http.cloudstore;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpResponse;
import org.apache.commons.io.IOUtils;

public class GCSHealthCheck extends HealthCheck {

    private GCSStore store;

    public GCSHealthCheck(GCSStore store) {
        this.store = store;
    }

    @Override
    protected Result check() throws Exception {

        // Attempt to read the root of the bucket; returns file list
        try {
            HttpResponse response = store.getStorageDocument(store.generateURLForDocument("").toString());
        } catch (Exception e) {
            return Result.unhealthy("GCS bucket unreachable: " + e.getMessage());
        }

        return Result.healthy();
    }

}
