package org.genomebridge.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.genomebridge.consent.http.enumeration.HeaderSummary;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

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
        Client client = new Client();
        ClientResponse response = getFile(client, consentSummaryPath());
        BufferedReader br = new BufferedReader(new InputStreamReader(
                response.getEntityInputStream()));
        String output;
        String summary = EnumSet.allOf(HeaderSummary.class).stream().
                map(HeaderSummary::getValue).collect(Collectors.joining(SEPARATOR));
        boolean isFirst = true;
        while ((output = br.readLine()) != null) {
            if (isFirst) {
                Assert.assertTrue(summary.equals(output));
                isFirst = false;
            }
        }
    }

}
