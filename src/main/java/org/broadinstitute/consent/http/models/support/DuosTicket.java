package org.broadinstitute.consent.http.models.support;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
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

  private final Ticket request;
  private final ExclusionStrategy strategy = new ExclusionStrategy() {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return f.getName().equals("hasIncidents");
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
      return false;
    }
  };

  public DuosTicket(Ticket request) {
    this.request = request;
  }

  public String toString() {
    return GsonUtil.gsonBuilderWithAdapters().addSerializationExclusionStrategy(strategy).create()
        .toJson(this);
  }

}
