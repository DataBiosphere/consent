package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit Tests for ConsentAssociation object.
 * <p/>
 * Created by egolin on 9/16/14.
 */
public class DatasetAssociationTest extends DatasetAssociationServiceTest {

    private static final String DATASET_OBJECTID = "SC-20660";
    private static final String INVALID_DATASET_OBJECTID = "SC-0000";


    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateDatasetAssociation() throws IOException {
        Client client = ClientBuilder.newClient();
        List<Integer> dataOwnerIds = new ArrayList();
        dataOwnerIds.add(1);
        dataOwnerIds.add(2);
        List<Integer> nonOwnerIds = new ArrayList();
        nonOwnerIds.add(3);
        //test Create
        checkStatus(CREATED, post(client, datasetAssociationPath(DATASET_OBJECTID),dataOwnerIds));
        //test Duplicated association
        checkStatus(BAD_REQUEST, post(client, datasetAssociationPath(DATASET_OBJECTID), dataOwnerIds));
        //test Invalid userId
        checkStatus(BAD_REQUEST, post(client, datasetAssociationPath(DATASET_OBJECTID),nonOwnerIds));
        //test Invalid userId
        checkStatus(NOT_FOUND, post(client, datasetAssociationPath(INVALID_DATASET_OBJECTID),dataOwnerIds));
    }


}