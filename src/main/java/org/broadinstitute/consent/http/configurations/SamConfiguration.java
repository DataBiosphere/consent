package org.broadinstitute.consent.http.configurations;

public class SamConfiguration {

  private final String samUrl = "https://sam.dsde-dev.broadinstitute.org/";
  private final String v2ResourcesPrefix = "/api/resources/v2/";
  private final String slash = "/";

  private String getSamUrl() {
    return samUrl;
  }

  public String getRegisterSelfUrl() {
    return getSamUrl() + "/register/user/v2/self/info";
  }

  public String postRegisterSelfUrl() {
    return getSamUrl() + "/register/user/v2/self";
  }

  public String getV1ResourceTypesUrl() {
    return getSamUrl() + "/api/config/v1/resourceTypes";
  }

  public String getV2ResourceTypeNameUrl(String resourceTypeName) {
    return getSamUrl() + v2ResourcesPrefix + resourceTypeName;
  }

  public String postV2ResourceTypeNameUrl(String resourceTypeName, String resourceId) {
    return getSamUrl() + v2ResourcesPrefix + resourceTypeName + slash + resourceId;
  }

  public String putV2ResourceTypeNameUrl(
      String resourceTypeName, String resourceId, String policyName) {
    return getSamUrl()
        + v2ResourcesPrefix
        + resourceTypeName
        + slash
        + resourceId
        + slash
        + "policies"
        + slash
        + policyName;
  }

  public String deleteV2ResourceTypeNameUrl(String resourceTypeName, String resourceId) {
    return getSamUrl() + v2ResourcesPrefix + resourceTypeName + slash + resourceId;
  }
}
