package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicesConfiguration {

  public static final String RESOURCE_TYPES_PATH = "api/config/v1/resourceTypes";
  public static final String REGISTER_SELF_INFO_PATH = "register/user/v2/self/info";
  public static final String REGISTER_SELF_DIAGNOSTICS_PATH = "register/user/v2/self/diagnostics";
  public static final String REGISTER_SELF_PATH = "register/user/v2/self";
  public static final String COMBINED_STATE_PATH = "api/users/v2/self/combinedState";
  public static final String TOS_TEXT_PATH = "termsOfService/v1/docs";
  public static final String TOS_SELF_PATH = "api/termsOfService/v1/user/self";
  public static final String ACCEPT_TOS_PATH = "api/termsOfService/v1/user/self/accept";
  public static final String REJECT_TOS_PATH = "api/termsOfService/v1/user/self/reject";
  public static final String SAM_V1_USER_EMAIL = "api/users/v1";
  public static final String SAM_STATUS_PATH = "status";
  public static final String ECM_RAS_PROVIDER = "api/oauth/v1/ras";
  // ECM's detailed status endpoint is auth-gated at the environment proxy. This legacy endpoint
  // remains public specifically for unauthenticated service health checks.
  public static final String ECM_STATUS_PATH = "status";
  // nosemgrep
  public static final String BROAD_ZENDESK_URL = "https://broadinstitute.zendesk.com";

  @NotBlank private String localURL;

  @NotBlank private String samUrl;

  @NotBlank private String ecmUrl;

  /**
   * This represents the max time we'll wait for an external status check to return. If it does not
   * return, we assume a degradation in the overall service. This can be overridden in local
   * configs.
   */
  private Integer timeoutSeconds = 10;

  /**
   * This represents the thread pool size for making external status checks. This can be overridden
   * in local configs.
   */
  private Integer poolSize = 10;

  /**
   * This represents the time we maintain a cache of the response of an external status check. In
   * practice, status checks hit the server every second, sometimes more often. None of our external
   * status checks are critical for minimal system operation which gives us the flexibility to rely
   * on a cached version of the response for a short period of time. This can be overridden in local
   * configs.
   */
  private Integer cacheExpireMinutes = 1;

  private boolean activateSupportNotifications = false;

  public String getLocalURL() {
    return localURL;
  }

  public void setLocalURL(String localURL) {
    this.localURL = normalizeBaseUrl(localURL);
  }

  public String getSamUrl() {
    return samUrl;
  }

  public void setSamUrl(String samUrl) {
    this.samUrl = normalizeBaseUrl(samUrl);
  }

  public String getEcmUrl() {
    return ecmUrl;
  }

  public void setEcmUrl(String ecmUrl) {
    this.ecmUrl = normalizeBaseUrl(ecmUrl);
  }

  public String getEcmRasProviderUrl() {
    return getEcmUrl() + ECM_RAS_PROVIDER;
  }

  public String getEcmStatusUrl() {
    return getEcmUrl() + ECM_STATUS_PATH;
  }

  public String getV1ResourceTypesUrl() {
    return getSamUrl() + RESOURCE_TYPES_PATH;
  }

  public String getSamStatusUrl() {
    return getSamUrl() + SAM_STATUS_PATH;
  }

  public String getRegisterUserV2SelfInfoUrl() {
    return getSamUrl() + REGISTER_SELF_INFO_PATH;
  }

  public String getV2SelfDiagnosticsUrl() {
    return getSamUrl() + REGISTER_SELF_DIAGNOSTICS_PATH;
  }

  public String postRegisterUserV2SelfUrl() {
    return getSamUrl() + REGISTER_SELF_PATH;
  }

  public String getCombinedStateUrl() {
    return getSamUrl() + COMBINED_STATE_PATH;
  }

  public String getToSTextUrl() {
    return getSamUrl() + TOS_TEXT_PATH;
  }

  public String getSelfTosUrl() {
    return getSamUrl() + TOS_SELF_PATH;
  }

  public String acceptTosUrl() {
    return getSamUrl() + ACCEPT_TOS_PATH;
  }

  public String rejectTosUrl() {
    return getSamUrl() + REJECT_TOS_PATH;
  }

  public String getV1UserUrl(String email) {
    String encoded = URLEncoder.encode(email, Charset.defaultCharset());
    return getSamUrl() + SAM_V1_USER_EMAIL + "/" + encoded;
  }

  public String postSupportRequestUrl() {
    return BROAD_ZENDESK_URL + "/api/v2/requests.json";
  }

  public String postSupportUploadUrl() {
    return BROAD_ZENDESK_URL + "/api/v2/uploads?filename=Attachment";
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

  private static String normalizeBaseUrl(String baseUrl) {
    String normalizedBaseUrl =
        Objects.requireNonNull(baseUrl, "Service base URL must not be null").strip();
    if (normalizedBaseUrl.isEmpty()) {
      throw new IllegalArgumentException("Service base URL must not be blank");
    }
    int end = normalizedBaseUrl.length();
    while (end > 0 && normalizedBaseUrl.charAt(end - 1) == '/') {
      end--;
    }
    return normalizedBaseUrl.substring(0, end) + "/";
  }
}
