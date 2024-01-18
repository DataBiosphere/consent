package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public record TosResponse(String acceptedOn, Boolean isCurrentVersion, String latestAcceptedVersion,
                          Boolean permitsSystemUsage) {

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

}
