package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.util.HttpClientUtil;

import java.nio.charset.Charset;

public class SendGridHealthCheck extends HealthCheck implements Managed {
    private final HttpClientUtil clientUtil;
    private final MailConfiguration configuration;

    public SendGridHealthCheck(HttpClientUtil clientUtil, MailConfiguration configuration) {
        this.clientUtil = clientUtil;
        this.configuration = configuration;
    }

    @Override
    protected Result check() throws Exception {
        try {
            String statusUrl = configuration.getSendGridStatusUrl();
            HttpGet httpGet = new HttpGet(statusUrl);
            try (CloseableHttpResponse response = clientUtil.getHttpResponse(httpGet)) {
                if (response.getStatusLine().getStatusCode() == HttpStatusCodes.STATUS_CODE_OK) {
                    String content = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                    SendGridStatus sgStatus = new Gson().fromJson(content, SendGridStatus.class);
                    return Result.builder()
                            .withDetail("pages", sgStatus.page)
                            .withDetail("status", sgStatus.status)
                            .healthy()
                            .build();
                } else {
                    return Result.unhealthy("SendGrid status is unhealthy: " + response.getStatusLine());
                }
            } catch (Exception e) {
                return Result.unhealthy(e);
            }
        } catch (Exception e) {
            return Result.unhealthy(e);
        }
    }

    @Override
    public void start() throws Exception {}

    @Override
    public void stop() throws Exception {}

    private static class SendGridStatus {
        Object page;
        Object status;
    }
}
