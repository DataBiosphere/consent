package org.broadinstitute.consent.http.service;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.db.OidcAuthorityDAO;
import org.broadinstitute.consent.http.models.OAuth2Configuration;

public class OidcService {
  public static final String SCOPE_PARAM = "scope";

  // nosemgrep
  public static final String CLIENT_SECRET_PARAM = "client_secret";

  private final OidcAuthorityDAO oidcAuthorityDAO;
  private final OidcConfiguration configuration;

  public OidcService(OidcAuthorityDAO oidcAuthorityDAO, OidcConfiguration configuration) {
    this.oidcAuthorityDAO = oidcAuthorityDAO;
    this.configuration = configuration;
  }

  public URI getAuthorizationURI(MultivaluedMap<String, String> parameters) {
    var uriBuilder = UriBuilder.fromUri(oidcAuthorityDAO.getOidcAuthorityConfiguration().authorization_endpoint());
    parameters.forEach((key, value) -> uriBuilder.queryParam(key, value.toArray()));
    getExtraAuthParams().forEach(param -> uriBuilder.queryParam(param.getName(), param.getValue()));
    if (configuration.isAddClientIdToScope()) {
      uriBuilder.replaceQueryParam(SCOPE_PARAM, addClientIdToScopes(parameters.getFirst(SCOPE_PARAM)));
    }
    return uriBuilder.build();
  }

  public String tokenExchange(MultivaluedMap<String, String> formParameters, MultivaluedMap<String, String> queryParameters) {
    var updatedFormParams = new MultivaluedHashMap<>(formParameters);
    maybeAddClientSecret(updatedFormParams);
    return oidcAuthorityDAO.oauthTokenPost(updatedFormParams, queryParameters);
  }

  public OAuth2Configuration getOAuth2Configuration() {
    return new OAuth2Configuration(
        oidcAuthorityDAO.getOidcAuthorityConfiguration().authorization_endpoint(),
        configuration.getClientId());
  }

  private List<NameValuePair> getExtraAuthParams() {
    return URLEncodedUtils.parse(configuration.getExtraAuthParams(), StandardCharsets.UTF_8);
  }

  private String addClientIdToScopes(String existingScopes) {
    if (StringUtils.isBlank(existingScopes)) {
      return configuration.getClientId();
    } else {
      return existingScopes + " " + configuration.getClientId();
    }
  }

  private void maybeAddClientSecret(MultivaluedMap<String, String> formParameters) {
    if (!StringUtils.isBlank(configuration.getClientSecret()) && !formParameters.containsKey(CLIENT_SECRET_PARAM)) {
      formParameters.add(CLIENT_SECRET_PARAM, configuration.getClientSecret());
    }
  }
}
