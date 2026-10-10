package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import java.util.UUID;
import org.broadinstitute.consent.http.mail.EmailSendId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EmailSendIdFilterTest {

  private final EmailSendIdFilter filter = new EmailSendIdFilter();
  private final ContainerRequestContext request = mock(ContainerRequestContext.class);
  private final ContainerResponseContext response = mock(ContainerResponseContext.class);

  @AfterEach
  void clearSendId() {
    EmailSendId.clear();
  }

  @Test
  void each_request_gets_its_own_send_id() {
    filter.filter(request);
    UUID first = EmailSendId.current();
    filter.filter(request, response);
    filter.filter(request);

    assertNotNull(first);
    assertNotEquals(first, EmailSendId.current());
  }

  @Test
  void the_response_clears_the_send_id() {
    filter.filter(request);
    filter.filter(request, response);

    assertNull(EmailSendId.current());
  }
}
