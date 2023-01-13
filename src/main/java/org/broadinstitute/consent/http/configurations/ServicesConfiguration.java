package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicesConfiguration {

  @NotNull
  private String ontologyURL;

  @NotNull
  private String localURL;

  @NotNull
  private String samUrl;

  /**
   * This represents the max time we'll wait for an external status check to return. If it does not
   * return, we assume a degradation in the overall service. This can be overridden in local configs.
   */
  private Integer timeoutSeconds = 10;

  /**
   * This represents the thread pool size for making external status checks. This can be overridden
   * in local configs.
   */
  private Integer poolSize = 10;

  /**
   * This represents the time we maintain a cache of the response of an external status check.
   * In practice, status checks hit the server every second, sometimes more often. None of
   * our external status checks are critical for minimal system operation which gives us the
   * flexibility to rely on a cached version of the response for a short period of time. This can be
   * overridden in local configs.
   */
  private Integer cacheExpireMinutes = 1;

  private boolean activateSupportNotifications = false;


  public String getOntologyURL() {
    return ontologyURL;
  }

  public void setOntologyURL(String ontologyURL) {
    this.ontologyURL = ontologyURL;
  }

  public String getLocalURL() {
    return localURL;
  }

  public void setLocalURL(String localURL) {
    this.localURL = localURL;
  }

  public String getMatchURL() {
    return getOntologyURL() + "match";
  }

  public String getMatchURL_v2() {
    return getOntologyURL() + "match/v2";
  }

  public String getValidateUseRestrictionURL() {
    return getOntologyURL() + "validate/userestriction";
  }

  public String getDARTranslateUrl() {
    return getOntologyURL() + "schemas/data-use/dar/translate";
  }

  public String getConsentTranslateUrl() {
    return getOntologyURL() + "schemas/data-use/consent/translate";
  }

  public String getSamUrl() {
    return samUrl;
  }

  public void setSamUrl(String samUrl) {
    this.samUrl = samUrl;
  }

  public String getV1ResourceTypesUrl() {
    return getSamUrl() + "api/config/v1/resourceTypes";
  }

  public String getRegisterUserV2SelfInfoUrl() {
    return getSamUrl() + "register/user/v2/self/info";
  }

  public String getV2SelfDiagnosticsUrl() {
    return getSamUrl() + "register/user/v2/self/diagnostics";
  }

  public String postRegisterUserV2SelfUrl() {
    return getSamUrl() + "register/user/v2/self";
  }

  public String getToSTextUrl() {
    return getSamUrl() + "tos/text";
  }

  public String tosRegistrationUrl() {
    return getSamUrl() + "register/user/v1/termsofservice";
  }

  public String postSupportRequestUrl() {
    return "https://broadinstitute.zendesk.com/api/v2/requests.json";
  }

  public boolean isActivateSupportNotifications() {
    return activateSupportNotifications;
  }

  public void setActivateSupportNotifications(boolean activateSupportNotifications) {
    this.activateSupportNotifications = activateSupportNotifications;
  }

  public Integer getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(Integer timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public Integer getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(Integer poolSize) {
    this.poolSize = poolSize;
  }

  public Integer getCacheExpireMinutes() {
    return cacheExpireMinutes;
  }

  public void setCacheExpireMinutes(Integer cacheExpireMinutes) {
    this.cacheExpireMinutes = cacheExpireMinutes;
  }
}
