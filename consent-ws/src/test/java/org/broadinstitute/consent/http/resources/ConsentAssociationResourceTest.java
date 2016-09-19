package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ConsentAssociationResourceTest extends ConsentAssociationServiceTest {

    // This consent ID is inserted in the queries file and already has a sample association
    private static final String consentId = "testId4";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }


    @Test
    public void testPostWorkspaceAssociation() throws IOException {
        UUID id = UUID.randomUUID();
        Client client = ClientBuilder.newClient();
        final ConsentAssociation consent_association = buildConsentAssociation("workspace", id.toString());
        checkStatus(CREATED, post(client, associationPath(consentId), consent_association));
        List<ConsentAssociation> created = retrieveAssociations(client, associationPath(consentId));
        System.out.println(created.toString());
    }

    private List<ConsentAssociation> retrieveAssociations(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(new GenericType<List<ConsentAssociation>>(){});
    }

    private static ConsentAssociation buildConsentAssociation(String atype, String... elements) {
        final ArrayList<String> elem_list = new ArrayList<>();
        Collections.addAll(elem_list, elements);
        return new ConsentAssociation(atype, elem_list);
    }

}