package org.genomebridge.consent.autocomplete;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.genomebridge.consent.autocomplete.service.AutocompleteAPI;
import org.genomebridge.consent.autocomplete.service.ElasticSearchAutocompleteAPI;
import org.genomebridge.consent.autocomplete.service.ElasticSearchHealthCheck;
import org.skife.jdbi.v2.DBI;

import java.net.InetSocketAddress;

public class AutocompleteModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    private Client getClient(ElasticSearchConfiguration config) {
        TransportClient client = new TransportClient(ImmutableSettings.settingsBuilder()
                .put("cluster.name", config.clusterName));
        for(String address: config.servers) {
            int colon = address.indexOf(':');
            int port = 9300;
            String server = address;
            if (colon >= 0) {
                port = Integer.parseInt(server.substring(colon + 1));
                server = server.substring(0, colon);
            }
            client.addTransportAddress(new InetSocketTransportAddress(server, port));
        }
        return client;
    }

    @Provides
    public AutocompleteAPI providesAPI(Environment env, AutocompleteConfiguration config) {
        ElasticSearchConfiguration esConfig = config.getElasticSearchConfiguration();
        String index = esConfig.index;
        Client client = getClient(esConfig);
        env.healthChecks().register("elastic-search", new ElasticSearchHealthCheck(client, index));
        return new ElasticSearchAutocompleteAPI(client, index);
    }
}
