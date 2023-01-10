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

  private Integer timeoutSeconds = 10;

  private Integer poolSize = 10;

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
}
