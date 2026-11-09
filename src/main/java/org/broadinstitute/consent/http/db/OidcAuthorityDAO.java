package org.broadinstitute.consent.http.db;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.UrlEncodedContent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.models.OidcAuthorityConfiguration;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.UriQueryParams;

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
  protected void setOidcAuthorityConfiguration(
      OidcAuthorityConfiguration oidcAuthorityConfiguration) {
    this.oidcAuthorityConfiguration = oidcAuthorityConfiguration;
  }

  public String oauthTokenPost(
      MultivaluedMap<String, String> formParameters,
      MultivaluedMap<String, String> queryParameters) {
    var uriBuilder = UriBuilder.fromUri(getOidcAuthorityConfiguration().token_endpoint());
    UriQueryParams.addAll(uriBuilder, queryParameters);
    GenericUrl genericUrl = new GenericUrl(uriBuilder.build());
    int status;
    String body;
    try {
      HttpRequest request =
          clientUtil.buildUnAuthedPostRequest(genericUrl, new UrlEncodedContent(formParameters));
      HttpResponse response = clientUtil.executeRequest(request);
      status = response.getStatusCode();
      body = response.parseAsString();
    } catch (Exception e) {
      logException(e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
    }
    var family = Response.Status.Family.familyOf(status);
    if (family == Response.Status.Family.SUCCESSFUL) {
      return body;
    }
    if (family == Response.Status.Family.CLIENT_ERROR) {
      // A bad, expired or used grant, or a probe from a scanner. Return the same status to the
      // client and do not send an alert to Sentry. The B2C error fields let a log search find
      // token expirations.
      String message =
          "OIDC token request failed with status %d: %s"
              .formatted(status, describeOAuthError(body));
      logWarn(message);
      throw new ClientErrorException(message, status);
    }
    String message =
        String.format(
            "Error getting OIDC token from authority %s, response code %d, response body %s",
            genericUrl, status, body);
    var exception = new ServerErrorException(message, Response.Status.INTERNAL_SERVER_ERROR);
    logException(exception);
    throw exception;
  }

  /**
   * Reads the "error" and "error_description" fields of an OAuth 2.0 error response. Only these
   * fields go to the log. They contain no tokens.
   */
  private static String describeOAuthError(String body) {
    if (body == null || body.isBlank()) {
      // The JDK HTTP client drops the body of a 401 response to a POST, so a 401 arrives here.
      return "no response body";
    }
    try {
      JsonObject json = JsonParser.parseString(body).getAsJsonObject();
      return "error %s, error_description %s"
          .formatted(stringField(json, "error"), stringField(json, "error_description"));
    } catch (RuntimeException e) {
      return "the response body is not an OAuth error";
    }
  }

  private static String stringField(JsonObject json, String name) {
    return json.has(name) ? json.get(name).getAsString() : "(none)";
  }

  private OidcAuthorityConfiguration loadOidcAuthorityConfiguration() {
    try {
      URI oidcMetadataUri =
          UriBuilder.fromUri(configuration.getAuthorityEndpoint())
              .path(OIDC_METADATA_URL_SUFFIX)
              .build();
      GenericUrl genericUrl = new GenericUrl(oidcMetadataUri);
      HttpRequest request = clientUtil.buildUnAuthedGetRequest(genericUrl);
      HttpResponse response = clientUtil.handleHttpRequest(request);
      String body = response.parseAsString();
      if (!response.isSuccessStatusCode()) {
        String message =
            String.format(
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
