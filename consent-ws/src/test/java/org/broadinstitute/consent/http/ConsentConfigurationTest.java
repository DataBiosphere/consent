package org.broadinstitute.consent.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import org.broadinstitute.consent.http.configurations.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by vvicario on 5/26/2016.
 */
public class ConsentConfigurationTest    {

@Valid
@NotNull
@JsonProperty
private final DataSourceFactory database = new DataSourceFactory();

private final UseRestrictionConfig ontology = new UseRestrictionConfig(
        "http://www.broadinstitute.org/ontologies/DURPO/methods_research",
        "http://www.broadinstitute.org/ontologies/DURPO/aggregate_analysis",
        "http://www.broadinstitute.org/ontologies/DURPO/control",
        "http://www.broadinstitute.org/ontologies/DURPO/population",
        "http://www.broadinstitute.org/ontologies/DURPO/male",
        "http://www.broadinstitute.org/ontologies/DURPO/female",
        "http://www.broadinstitute.org/ontologies/DURPO/For_profit",
        "http://www.broadinstitute.org/ontologies/DURPO/Non_profit",
        "http://www.broadinstitute.org/ontologies/DURPO/boys",
        "http://www.broadinstitute.org/ontologies/DURPO/girls",
        "http://www.broadinstitute.org/ontologies/DURPO/children"
);

@Valid
@NotNull
@JsonProperty
private final MongoConfiguration mongo = new MongoConfiguration();

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

@Valid
@NotNull
private BasicAuthConfig basicAuthentication = new BasicAuthConfig();

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
private final StoreOntologyConfiguration storeOntology = new StoreOntologyConfiguration();


        public DataSourceFactory getDataSourceFactory() {
            return database;
        }

        public UseRestrictionConfig getUseRestrictionConfiguration() {
            return ontology;
        }

        public MongoConfiguration getMongoConfiguration() {
            return mongo;
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

        public BasicAuthConfig getBasicAuthentication() {
            return basicAuthentication;
        }

        public void setBasicAuthentication(BasicAuthConfig basicAuthentication) {
            this.basicAuthentication = basicAuthentication;
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

        public StoreOntologyConfiguration getStoreOntologyConfiguration() {
            return storeOntology;
        }
}