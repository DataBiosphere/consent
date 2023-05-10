package org.broadinstitute.consent.http.resources;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
public class VersionResourceTest {

  private static final ResourceExtension RESOURCE_EXTENSION = ResourceExtension.builder()
      .addResource(new VersionResource())
      .build();

  @Test
  public void testGetVersion() {
    Response response = RESOURCE_EXTENSION.target("/version").request().get();
    Assertions.assertEquals(200, response.getStatus());
  }

}
