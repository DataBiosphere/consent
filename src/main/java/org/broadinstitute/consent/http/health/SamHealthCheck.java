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
      try {
        SimpleResponse response = clientUtil.getCachedResponse(httpGet);
        if (response.code() == HttpStatusCodes.STATUS_CODE_OK) {
          String content = response.entity();
          SamStatus samStatus = new Gson().fromJson(content, SamStatus.class);
          return Result.builder()
              .withDetail(StatusResource.OK, samStatus.ok)
              .withDetail(StatusResource.SYSTEMS, samStatus.systems)
              .healthy()
              .build();
        } else {
          return Result.unhealthy("Sam status is unhealthy: " + response.code());
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
