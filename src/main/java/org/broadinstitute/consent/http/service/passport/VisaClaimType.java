package org.broadinstitute.consent.http.service.passport;

public interface VisaClaimType {
  String type();

  Long asserted();

  Object value();

  String source();

  String by();
}
