package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import io.dropwizard.lifecycle.Managed;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;

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
            try {
                SimpleResponse response = clientUtil.getHttpResponse(httpGet);
                if (response.code() == HttpStatusCodes.STATUS_CODE_OK) {
                    String content = response.entity();
                    SendGridStatus status = new Gson().fromJson(content, SendGridStatus.class);
                    return status.getResult();
                } else {
                    return Result.unhealthy("SendGrid status is unhealthy: " + response.code());
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
}
