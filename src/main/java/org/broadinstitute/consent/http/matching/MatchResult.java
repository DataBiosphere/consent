package org.broadinstitute.consent.http.matching;

import java.util.List;
import org.broadinstitute.consent.http.models.matching.DataUseMatchResultType;

public class MatchResult {

  private final DataUseMatchResultType matchResultType;
  private final List<String> message;

  public MatchResult(DataUseMatchResultType matchResultType, List<String> message) {
    this.matchResultType = matchResultType;
    this.message = message;
  }

  public DataUseMatchResultType getMatchResultType() {
    return matchResultType;
  }

  public List<String> getMessage() {
    return message;
  }

  public static MatchResult from(DataUseMatchResultType matchResultType, List<String> message) {
    return new MatchResult(matchResultType, message);
  }
}
