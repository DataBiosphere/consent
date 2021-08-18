package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import io.dropwizard.lifecycle.Managed;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;

public class SamHealthCheck extends HealthCheck implements Managed {

    private final HttpClientUtil clientUtil;
    private final ServicesConfiguration configuration;

    public SamHealthCheck(HttpClientUtil clientUtil, ServicesConfiguration configuration) {
        this.clientUtil = clientUtil;
        this.configuration = configuration;
    }

    @Override
    protected Result check() throws Exception {
        try {
            String statusUrl = configuration.getSamUrl() + "status";
            HttpGet httpGet = new HttpGet(statusUrl);
            try (CloseableHttpResponse response = clientUtil.getHttpResponse(httpGet)) {
                if (response.getStatusLine().getStatusCode() == HttpStatusCodes.STATUS_CODE_OK) {
                    return Result.healthy();
                } else {
                    return Result.unhealthy("Sam status is unhealthy: " + response.getStatusLine());
                }
            } catch (Exception e) {
                return Result.unhealthy(e);
            }
        } catch (Exception e) {
            return Result.unhealthy(e);
        }
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }
}
