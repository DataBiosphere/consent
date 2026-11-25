package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentConfiguration extends Configuration {

  public ConsentConfiguration() {}

  @Valid @NotNull @JsonProperty private final DataSourceFactory database = new DataSourceFactory();

  @Valid @NotNull @JsonProperty
  private final StoreConfiguration googleStore = new StoreConfiguration();

  @Valid @NotNull @JsonProperty
  private final ServicesConfiguration services = new ServicesConfiguration();

  @Valid @NotNull private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

  @Valid @NotNull private MailConfiguration mailConfiguration = new MailConfiguration();

  @Valid @NotNull
  private FreeMarkerConfiguration freeMarkerConfiguration = new FreeMarkerConfiguration();

  @JsonProperty("httpClient")
  public JerseyClientConfiguration getJerseyClientConfiguration() {
    return httpClient;
  }

  @Valid @NotNull @JsonProperty
  private final ElasticSearchConfiguration elasticSearch = new ElasticSearchConfiguration();

  @Valid @NotNull @JsonProperty
  private final OidcConfiguration oidcConfiguration = new OidcConfiguration();

  @Valid @NotNull @JsonProperty
  private final RateLimitConfiguration rateLimit = new RateLimitConfiguration();

  // Optional. If the config omits the liquibase block, these defaults apply and Liquibase does not
  // run at startup. See LiquibaseConfiguration.
  @Valid @NotNull @JsonProperty
  private final LiquibaseConfiguration liquibase = new LiquibaseConfiguration();

  // Optional. If the config omits the showcase block, the public showcase lookup is off. See
  // ShowcaseConfiguration.
  @Valid @NotNull @JsonProperty
  private final ShowcaseConfiguration showcase = new ShowcaseConfiguration();

  @Valid @NotNull @JsonProperty
  private final UploadConfiguration uploads = new UploadConfiguration();

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

  public ElasticSearchConfiguration getElasticSearchConfiguration() {
    return elasticSearch;
  }

  public OidcConfiguration getOidcConfiguration() {
    return oidcConfiguration;
  }

  public RateLimitConfiguration getRateLimitConfiguration() {
    return rateLimit;
  }

  public LiquibaseConfiguration getLiquibaseConfiguration() {
    return liquibase;
  }

  public UploadConfiguration getUploadConfiguration() {
    return uploads;
  }

  public ShowcaseConfiguration getShowcaseConfiguration() {
    return showcase;
  }

  /**
   * A file arrives inside an upload request, so the file limit has to be below the request limit:
   * otherwise a file within its limit can be cut off by the request limit, and an endpoint that
   * reads the body itself (support attachments) answers 500 rather than 413.
   */
  @SuppressWarnings("unused") // invoked reflectively by Bean Validation via @AssertTrue
  @AssertTrue(message = "googleStore.maxUploadBytes must be less than uploads.maxRequestBytes")
  public boolean isFileLimitBelowRequestLimit() {
    return googleStore.getMaxUploadBytes() < uploads.getMaxRequestBytes();
  }
}
