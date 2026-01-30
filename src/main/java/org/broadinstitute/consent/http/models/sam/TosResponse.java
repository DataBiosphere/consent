package org.broadinstitute.consent.http.models.sam;

import org.broadinstitute.consent.http.util.gson.GsonUtil;

public record TosResponse(
    String acceptedOn,
    Boolean isCurrentVersion,
    String latestAcceptedVersion,
    Boolean permitsSystemUsage) {

  @Override
  public String toString() {
    return GsonUtil.gsonBuilderWithAdapters().create().toJson(this);
  }
}
