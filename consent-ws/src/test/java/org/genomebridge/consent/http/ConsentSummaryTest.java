package org.genomebridge.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.configurations.ConsentConfiguration;
import org.genomebridge.consent.http.enumeration.HeaderSummary;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class ConsentSummaryTest extends ElectionVoteServiceTest {

    private static final String SEPARATOR = "\t";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testConsentSummaryFile() throws IOException {
        Client client = ClientBuilder.newClient();
        Response response = getTextPlain(client, consentSummaryPath());
        String output = new BufferedReader(new StringReader(response.readEntity(String.class))).readLine();
        String summary = EnumSet.allOf(HeaderSummary.class).stream().
                map(HeaderSummary::getValue).collect(Collectors.joining(SEPARATOR));
        Assert.assertTrue(summary.equals(output));
    }

}
