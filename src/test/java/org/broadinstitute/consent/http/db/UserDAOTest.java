package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

public class UserDAOTest extends AbstractTest {

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    DACUserDAO userDAO;

    @Before
    public void setUp() {
        userDAO = getApplicationJdbi().onDemand(DACUserDAO.class);
    }

    @After
    public void tearDown() {

    }

}
