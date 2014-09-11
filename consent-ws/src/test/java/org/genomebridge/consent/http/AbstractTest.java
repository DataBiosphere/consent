/*
 * Copyright 2014 Broad Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.genomebridge.consent.http;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.resources.ConsentResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    abstract public DropwizardAppRule<ConsentConfiguration> rule();

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> ClientResponse post(Client client, String path, T value) { return post(client, path, "testuser", value); }

    public <T> ClientResponse post(Client client, String path, String user, T value) {
        return client.resource(path)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                .post(ClientResponse.class, value);
    }

    public <T> ClientResponse put(Client client, String path, T value) { return put(client, path, "testuser", value); }

    public <T> ClientResponse put(Client client, String path, String user, T value) {
        return client.resource(path)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                .put(ClientResponse.class, value);
    }

    public ClientResponse delete(Client client, String path) { return delete(client, path, "testuser"); }

    public ClientResponse delete(Client client, String path, String user) {
        return client.resource(path)
                .header("REMOTE_USER", user)
                .delete(ClientResponse.class);
    }

    public ClientResponse get(Client client, String path) { return get(client, path, "testuser"); }

    public ClientResponse get(Client client, String path, String user) {
        return client.resource(path)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("REMOTE_USER", user)
                .get(ClientResponse.class);
    }


    public ClientResponse check200( ClientResponse response ) {
        return checkStatus(200, response);
    }

    public ClientResponse checkStatus( int status, ClientResponse response ) {
        assertThat(response.getStatus()).isEqualTo(status);
        return response;
    }

    public String checkHeader( ClientResponse response, String header ) {
        MultivaluedMap<String,String> map = response.getHeaders();
        assertThat(map).describedAs(String.format("header \"%s\"", header)).containsKey(header);
        return map.getFirst(header);
    }

    public String randomID() {
        return UUID.randomUUID().toString();
    }

    public String path2Url(String path) {
        if(path.startsWith("/")) { path = path.substring(1, path.length()); }
        return String.format("http://localhost:%d/%s", rule().getLocalPort(), path);
    }


}
