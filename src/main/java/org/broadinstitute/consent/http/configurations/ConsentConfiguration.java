package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentConfiguration extends Configuration {

  public ConsentConfiguration() {
  }

  @Valid
  @NotNull
  @JsonProperty
  private final DataSourceFactory database = new DataSourceFactory();

  @Valid
  @NotNull
  @JsonProperty
  private final StoreConfiguration googleStore = new StoreConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private final ServicesConfiguration services = new ServicesConfiguration();

  @Valid
  @NotNull
  private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

  @Valid
  @NotNull
  private MailConfiguration mailConfiguration = new MailConfiguration();

  @Valid
  @NotNull
  private FreeMarkerConfiguration freeMarkerConfiguration = new FreeMarkerConfiguration();

  @Valid
  @NotNull
  private GoogleOAuth2Config googleAuthentication = new GoogleOAuth2Config();

  @JsonProperty("httpClient")
  public JerseyClientConfiguration getJerseyClientConfiguration() {
    return httpClient;
  }


  @Valid
  @NotNull
  @JsonProperty
  private final ElasticSearchConfiguration elasticSearch = new ElasticSearchConfiguration();

  @Valid
  @NotNull
  @JsonProperty
  private final OidcConfiguration oidcConfiguration = new OidcConfiguration();

  public DataSourceFactory getDataSourceFactory() {
    return database;
  }

  public StoreConfiguration getCloudStoreConfiguration() {
    return googleStore;
  }

  public ServicesConfiguration getServicesConfiguration() {
    return services;
  }

  public MailConfiguration getMailConfiguration() {
    return mailConfiguration;
  }

  public FreeMarkerConfiguration getFreeMarkerConfiguration() {
    return freeMarkerConfiguration;
  }

  public GoogleOAuth2Config getGoogleAuthentication() {
    return googleAuthentication;
  }

  public void setGoogleAuthentication(GoogleOAuth2Config googleAuthentication) {
    this.googleAuthentication = googleAuthentication;
  }

  public ElasticSearchConfiguration getElasticSearchConfiguration() {
    return elasticSearch;
  }

  public OidcConfiguration getOidcConfiguration() {
    return oidcConfiguration;
  }
}
