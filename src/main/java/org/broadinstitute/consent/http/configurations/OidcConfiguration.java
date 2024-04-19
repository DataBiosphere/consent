package org.broadinstitute.consent.http.configurations;

import jakarta.validation.constraints.NotNull;

public class OidcConfiguration {
  @NotNull
  private String authorityEndpoint;
  private String extraAuthParams;
  private boolean addClientIdToScope = false;
  private String clientId;
  private String clientSecret;

  public String getAuthorityEndpoint() {
    return authorityEndpoint;
  }

  public void setAuthorityEndpoint(String authorityEndpoint) {
    this.authorityEndpoint = authorityEndpoint;
  }

  public String getExtraAuthParams() {
    return extraAuthParams;
  }

  public void setExtraAuthParams(String extraAuthParams) {
    this.extraAuthParams = extraAuthParams;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public boolean isAddClientIdToScope() {
    return addClientIdToScope;
  }

  public void setAddClientIdToScope(boolean addClientIdToScope) {
    this.addClientIdToScope = addClientIdToScope;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }
}
