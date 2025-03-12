package org.broadinstitute.consent.http.models.support;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zendesk.client.v2.model.Ticket;

/**
 * Wrapper around org.zendesk.client.v2.model.Ticket to do the following:
 * <ul>
 *   <li>Provide a top level field "request" that represents the native Request model which is
 *   required for Zendesk API usage. See
 *   <a href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests</a></a>
 *   for more information.</li>
 *   <li>It is required that we provide a `ticketFormId` which is not available on the `Request`
 *   class, but is only on the `Ticket` class.</li>
 *   <li>Ignore the Ticket.hasIncidents field on serialization</li>
 * </ul>
 */
public class DuosTicket {

  public final Ticket request;

  public DuosTicket(Ticket request) {
    this.request = request;
  }

  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
