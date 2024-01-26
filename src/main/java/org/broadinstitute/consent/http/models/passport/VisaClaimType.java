package org.broadinstitute.consent.http.models.passport;

import java.util.List;

public interface VisaClaimType {
  String type();
  Integer asserted();
  String value();
  String source();
  String by();
  default List<VisaCondition> conditions() {
    return List.of();
  }
}
