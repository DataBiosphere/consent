package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.HttpClientUtil.SimpleResponse;

public class EcmHealthCheck extends HealthCheck {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;

  @Inject
  public EcmHealthCheck(HttpClientUtil clientUtil, ServicesConfiguration configuration) {
    this.clientUtil = clientUtil;
    this.configuration = configuration;
  }

  @Override
  protected Result check() {
    try {
      HttpGet httpGet = new HttpGet(configuration.getEcmStatusUrl());
      SimpleResponse response = clientUtil.getCachedResponse(httpGet);
      if (response.code() == HttpStatusCodes.STATUS_CODE_OK) {
        EcmStatus ecmStatus = new Gson().fromJson(response.entity(), EcmStatus.class);
        ResultBuilder result =
            Result.builder()
                .withDetail(StatusResource.OK, ecmStatus.ok)
                .withDetail(StatusResource.SYSTEMS, ecmStatus.systems);
        if (ecmStatus.ok) {
          return result.healthy().build();
        }
        return result.unhealthy().withMessage("ECM reported an unhealthy status").build();
      }
      return Result.unhealthy("ECM status is unhealthy: " + response.code());
    } catch (Exception e) {
      return Result.unhealthy(e);
    }
  }

  private static class EcmStatus {

    boolean ok;
    Object systems;
  }
}
