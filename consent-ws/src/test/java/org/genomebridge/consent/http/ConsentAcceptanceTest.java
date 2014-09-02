/**
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
import org.genomebridge.consent.http.models.Everything;
import org.genomebridge.consent.http.models.Nothing;
import org.genomebridge.consent.http.resources.ConsentResource;
import org.junit.ClassRule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ConsentAcceptanceTest extends ConsentServiceTest {

    public static final int CREATED = ClientResponse.Status.CREATED.getStatusCode();

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));


    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateConsent() {
        Client client = new Client();

        ConsentResource rec = new ConsentResource();
        rec.requiresManualReview = true;
        rec.useRestriction = new Everything();

        ClientResponse response = checkStatus( CREATED, put(client, consentPath(), rec) );

        String createdLocation = checkHeader(response, "Location");

        ConsentResource created = retrieveConsent(client, createdLocation);

        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);
    }

    @Test
    public void testUpdateConsent() {
        Client client = new Client();

        ConsentResource rec = new ConsentResource();
        rec.requiresManualReview = true;
        rec.useRestriction = new Everything();

        ClientResponse response = checkStatus( CREATED, put(client, consentPath(), rec) );

        String createdLocation = checkHeader(response, "Location");

        ConsentResource created = retrieveConsent(client, createdLocation);

        assertThat(created.requiresManualReview).isEqualTo(rec.requiresManualReview);
        assertThat(created.useRestriction).isEqualTo(rec.useRestriction);

        ConsentResource update = new ConsentResource();
        update.requiresManualReview = false;
        update.useRestriction = new Nothing();

        check200(post(client, createdLocation, update));

        ConsentResource updated = retrieveConsent(client, createdLocation);

        assertThat(updated.requiresManualReview).isEqualTo(update.requiresManualReview);
        assertThat(updated.useRestriction).isEqualTo(update.useRestriction);
    }
}
