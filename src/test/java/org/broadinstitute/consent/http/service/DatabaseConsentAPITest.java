package org.broadinstitute.consent.http.service;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.junit.ClassRule;
import org.junit.Test;


public class DatabaseConsentAPITest extends AbstractTest {

    private ConsentAPI databaseConsentApi = DatabaseConsentAPI.getInstance();

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE =
            new DropwizardAppRule<>(ConsentApplication.class,
                    resourceFilePath("consent-config.yml"));
    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test (expected = UnknownIdentifierException.class)
    public void testRetrieveUnkownIdentifier() throws UnknownIdentifierException {
        databaseConsentApi.retrieve("non-existent Id");
    }

}
