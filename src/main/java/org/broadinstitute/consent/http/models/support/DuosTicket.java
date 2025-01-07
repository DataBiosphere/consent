package org.broadinstitute.consent.http.models.support;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.model.Ticket;

/**
 * Wrapper around org.zendesk.client.v2.model.Ticket to do two things:
 *  1. Provide a top level field "request"
 *  2. Ignore the Ticket.hasIncidents field on serialization
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
    return GsonUtil.gsonBuilderWithAdapters()
        .addSerializationExclusionStrategy(strategy)
        .create()
        .toJson(this);
  }

}
