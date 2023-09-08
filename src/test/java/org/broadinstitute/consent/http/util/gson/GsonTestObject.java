package org.broadinstitute.consent.http.util.gson;

import java.time.Instant;
import java.util.Date;

/**
 * This object is set up to allow us to have test coverage to ensure that our Gson configuration is
 * set up properly.
 */
class GsonTestObject {

  GsonTestObject() {
  }

  private transient String transientField;

  private Date date;
  private Instant instant;


  String getTransientField() {
    return transientField;
  }

  void setTransientField() {
    this.transientField = "should never serialize";
  }

  Date getDate() {
    return date;
  }

  void setDate(Date date) {
    this.date = date;
  }

  Instant getInstant() {
    return instant;
  }

  void setInstant(Instant instant) {
    this.instant = instant;
  }
}
