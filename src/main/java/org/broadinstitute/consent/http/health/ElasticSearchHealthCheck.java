package org.broadinstitute.consent.http.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.broadinstitute.consent.http.service.ontology.ElasticSearchSupport.jsonHeader;

public class ElasticSearchHealthCheck extends HealthCheck implements Managed {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchHealthCheck.class);
    private final RestClient client;

    @Override
    public void start() throws Exception { }

    @Override
    public void stop() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    public ElasticSearchHealthCheck(ElasticSearchConfiguration config) {
        this.client = ElasticSearchSupport.createRestClient(config);
    }

    @Override
    protected Result check() throws Exception {
        try {
            RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
            builder.addHeader(jsonHeader.getName(), jsonHeader.getValue());
            Request request = new Request("GET", ElasticSearchSupport.getClusterHealthPath());
            request.setOptions(builder.build());
            Response esResponse = client.performRequest(request);
            if (esResponse.getStatusLine().getStatusCode() != 200) {
                return Result.unhealthy("Invalid health check request: " + esResponse.getStatusLine().getReasonPhrase());
            }
            String stringResponse = IOUtils.toString(esResponse.getEntity().getContent(), Charset.defaultCharset());
            JsonObject jsonResponse = JsonParser.parseString(stringResponse).getAsJsonObject();
            String status = jsonResponse.get("status").getAsString();
            if (status.equalsIgnoreCase("red")) {
                return Result.unhealthy("ClusterHealth is RED\n" + jsonResponse.toString());
            }
            if (status.equalsIgnoreCase("yellow")) {
                return Result.healthy("ClusterHealth is YELLOW\n" + jsonResponse.toString());
            }
        } catch (IOException e) {
            return Result.unhealthy(e.getMessage());
        }
        return Result.healthy("ClusterHealth is GREEN");
    }
}
