package org.broadinstitute.consent.http.service.passport;

import java.util.List;

public interface VisaClaimType {
  String type();

  Long asserted();

  String value();

  String source();

  String by();

  default List<VisaCondition> conditions() {
    return List.of();
  }
}
