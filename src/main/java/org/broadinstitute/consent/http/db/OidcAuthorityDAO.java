package org.broadinstitute.consent.http.db;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.UrlEncodedContent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.models.OidcAuthorityConfiguration;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;

public class OidcAuthorityDAO implements ConsentLogger {
  private static final String OIDC_METADATA_URL_SUFFIX = ".well-known/openid-configuration";
  private final HttpClientUtil clientUtil;
  private final OidcConfiguration configuration;

  private OidcAuthorityConfiguration oidcAuthorityConfiguration = null;

  public OidcAuthorityDAO(HttpClientUtil clientUtil, OidcConfiguration configuration) {
    this.clientUtil = clientUtil;
    this.configuration = configuration;
  }

  public OidcAuthorityConfiguration getOidcAuthorityConfiguration() {
    if (oidcAuthorityConfiguration == null) {
      oidcAuthorityConfiguration = loadOidcAuthorityConfiguration();
    }
    return oidcAuthorityConfiguration;
  }

  @VisibleForTesting
  void setOidcAuthorityConfiguration(OidcAuthorityConfiguration oidcAuthorityConfiguration) {
    this.oidcAuthorityConfiguration = oidcAuthorityConfiguration;
  }

  public String oauthTokenPost(MultivaluedMap<String, String> formParameters, MultivaluedMap<String, String> queryParameters) {
    try {
      var uriBuilder = UriBuilder.fromUri(getOidcAuthorityConfiguration().token_endpoint());
      queryParameters.forEach((key, values) -> values.forEach(value -> uriBuilder.queryParam(key, value)));
      GenericUrl genericUrl = new GenericUrl(uriBuilder.build());
      HttpRequest request = clientUtil.buildUnAuthedPostRequest(genericUrl, new UrlEncodedContent(formParameters));
      HttpResponse response = clientUtil.handleHttpRequest(request);
      if (!response.isSuccessStatusCode()) {
        String message = String.format(
            "Error getting OIDC token from authority %s, response code %d, response body %s",
            genericUrl, response.getStatusCode(), response.parseAsString());
        var exception = new ServerErrorException(message, Response.Status.INTERNAL_SERVER_ERROR);
        logException(exception);
        throw exception;
      }
      return response.parseAsString();
    } catch (Exception e) {
      logException(e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
    }
  }

  private OidcAuthorityConfiguration loadOidcAuthorityConfiguration() {
    try {
      URI oidcMetadataUri = UriBuilder.fromUri(configuration.getAuthorityEndpoint()).path(OIDC_METADATA_URL_SUFFIX).build();
      GenericUrl genericUrl = new GenericUrl(oidcMetadataUri);
      HttpRequest request = clientUtil.buildUnAuthedGetRequest(genericUrl);
      HttpResponse response = clientUtil.handleHttpRequest(request);
      String body = response.parseAsString();
      if (!response.isSuccessStatusCode()) {
        String message = String.format(
            "Error getting OIDC configuration from authority %s, response code %d, response body %s",
            genericUrl, response.getStatusCode(), body);
        var exception = new ServerErrorException(message, Response.Status.INTERNAL_SERVER_ERROR);
        logException(exception);
        throw exception;
      }
      return new Gson().fromJson(body, OidcAuthorityConfiguration.class);
    } catch (Exception e) {
      logException(e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
    }
  }
}
