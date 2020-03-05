package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.PendingCase;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VoteDataRequestTest extends ElectionVoteServiceTest {

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testDataRequestPendingCaseWithInvalidUser() {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, dataRequestPendingCasesPath(789)).readEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isEmpty();
    }
}
