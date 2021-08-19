package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import java.nio.charset.Charset;

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
                String content = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                SamStatus samStatus = new Gson().fromJson(content, SamStatus.class);
                if (response.getStatusLine().getStatusCode() == HttpStatusCodes.STATUS_CODE_OK) {
                    return Result.builder()
                            .withDetail("ok", samStatus.ok)
                            .withDetail("systems", samStatus.systems)
                            .healthy()
                            .build();
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

    private static class SamStatus {
        boolean ok;
        Object systems;
    }
}
