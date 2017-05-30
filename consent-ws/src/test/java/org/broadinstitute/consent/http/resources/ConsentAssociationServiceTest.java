package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ConsentAssociationServiceTest extends AbstractTest {

    public String associationPath(String id) {
        try {
            return path2Url(String.format("consent/%s/association", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return String.format("consent/%s", id);
        }
    }

}
