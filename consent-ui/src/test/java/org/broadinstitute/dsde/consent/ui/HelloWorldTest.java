package org.broadinstitute.dsde.consent.ui;

import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fest.assertions.api.Assertions.assertThat;

public class HelloWorldTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldTest.class);

    @ClassRule
    public static final DropwizardAppRule<ConsentUIConfiguration> RULE =
            new DropwizardAppRule<>(ConsentUIApplication.class,
                    Resources.getResource("test-example.yml").getPath());


    @Test
    public void testHelloWorld() throws Exception {
        Client client = new Client();
        WebResource webResource = client.
                resource("http://localhost:" + RULE.getLocalPort() + "/hello-world");
        ClientResponse response = webResource.
                get(ClientResponse.class);

        String responseText = response.getEntity(String.class);
        LOGGER.info("Response Text: " + responseText);
        assertThat(responseText.equalsIgnoreCase("Hello World"));
    }

}
