package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.ApprovalExpirationTime;

import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ApprovalExpirationTimeTest extends ApprovalExpirationTimeServiceTest {

    public static final int CREATED = Response.Status.CREATED
            .getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();


    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateApprovalExpirationTime() throws IOException {
        Client client = ClientBuilder.newClient();
        ApprovalExpirationTime approvalExpirationTime = new ApprovalExpirationTime();
        approvalExpirationTime.setAmountOfDays(7);
        approvalExpirationTime.setUserId(3333);
        Response response = checkStatus(CREATED,
                post(client, approvalExpirationTimePath(), approvalExpirationTime));
        String createdLocation = checkHeader(response, "Location");
        ApprovalExpirationTime created = retrieveApprovalExpirationTime(client, createdLocation);
        assertThat(created.getUserId()).isEqualTo(3333);
        assertThat(created.getAmountOfDays()).isEqualTo(7);
        assertThat(created.getCreateDate()).isNotNull();;
        // try to create other election for the same consent
        testUpdateApprovalElection(created, createdLocation);

    }

    public void testUpdateApprovalElection(ApprovalExpirationTime created, String createdLocation) throws IOException {
        Client client = ClientBuilder.newClient();
        created.setAmountOfDays(9);
        checkStatus(OK, put(client, approvalExpirationTimePath(created.getId()), created));
        created = retrieveApprovalExpirationTime(client, createdLocation);
        assertThat(created.getAmountOfDays()).isEqualTo(9);
    }
}
