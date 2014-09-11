package org.genomebridge.consent.autocomplete.service;

import com.codahale.metrics.health.HealthCheck;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;

public class ElasticSearchHealthCheck extends HealthCheck {
    Client client;
    String index;

    public ElasticSearchHealthCheck(Client client, String index) {
        this.client = client;
        this.index = index;
    }

    @Override
    protected Result check() throws Exception {
        ClusterHealthResponse health = client.admin().cluster().prepareHealth(index).get();
        if (health.isTimedOut()) {
            return Result.unhealthy("HealthCheck timed out");
        }
        ClusterHealthStatus status = health.getStatus();
        if (status == ClusterHealthStatus.RED) {
            return Result.unhealthy("ClusterHealth is RED\n" + health.toString());
        }
        if (status == ClusterHealthStatus.YELLOW) {
            return Result.unhealthy("ClusterHealth is YELLOW\n" + health.toString());
        }
        return Result.healthy("ClusterHealth is GREEN");
    }
}
