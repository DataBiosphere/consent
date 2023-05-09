package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import io.dropwizard.lifecycle.Managed;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;

public class OntologyHealthCheck extends HealthCheck implements Managed {

    private final HttpClientUtil clientUtil;
    private final ServicesConfiguration servicesConfiguration;

    public OntologyHealthCheck(
            HttpClientUtil clientUtil, ServicesConfiguration servicesConfiguration) {
        this.clientUtil = clientUtil;
        this.servicesConfiguration = servicesConfiguration;
    }

    @Override
    public Result check() {
        try {
            String statusUrl = servicesConfiguration.getOntologyURL() + "status";
            HttpGet httpGet = new HttpGet(statusUrl);
            try {
                SimpleResponse response = clientUtil.getCachedResponse(httpGet);
                if (response.code() == HttpStatusCodes.STATUS_CODE_OK) {
                    String content = response.entity();
                    Object ontologyStatus = new Gson().fromJson(content, Object.class);
                    return Result.builder()
                            .withDetail(StatusResource.OK, true)
                            .withDetail(StatusResource.SYSTEMS, ontologyStatus)
                            .healthy()
                            .build();
                } else {
                    return Result.unhealthy("Ontology status is unhealthy: " + response.code());
                }
            } catch (Exception e) {
                return Result.unhealthy(e);
            }
        } catch (Exception e) {
            return Result.unhealthy(e);
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }
}
