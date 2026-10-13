package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Objects;
import java.util.UUID;
import org.broadinstitute.consent.http.mail.EmailSendId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checks the send id through a request Jersey routes itself, not by calling the filter. */
@ExtendWith(DropwizardExtensionsSupport.class)
class EmailSendIdEndpointTest {

  private static final ResourceExtension RESOURCE =
      ResourceExtension.builder()
          .addResource(new SendIdResource())
          .addProvider(EmailSendIdFilter.class)
          .build();

  @Path("/send-id")
  public static class SendIdResource {
    @GET
    public String current() {
      return Objects.toString(EmailSendId.current(), "none");
    }
  }

  @Test
  void each_request_runs_under_its_own_send_id() {
    UUID first = UUID.fromString(get());
    UUID second = UUID.fromString(get());

    assertNotEquals(first, second);
  }

  private String get() {
    return RESOURCE.target("/send-id").request().get(String.class);
  }
}
