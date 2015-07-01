package org.broadinstitute.dsde.consent.ui;

import org.broadinstitute.dsde.consent.ui.resources.HelloWorldResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ConsentUIApplication extends Application<ConsentUIConfiguration> {
    public static void main(String[] args) throws Exception {
        new ConsentUIApplication().run(args);
    }

    @Override
    public String getName() {
        return "consent-ui";
    }

    @Override
    public void initialize(Bootstrap<ConsentUIConfiguration> bootstrap) {

    }

    @Override
    public void run(ConsentUIConfiguration configuration, Environment environment) {
        environment.jersey().register(new HelloWorldResource());
    }

}
