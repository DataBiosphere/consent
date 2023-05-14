package org.broadinstitute.consent.http.util.gson;

import java.time.Instant;
import java.util.Date;

/**
 * This object is set up to allow us to have test coverage to ensure that our Gson configuration is
 * set up properly.
 */
public class GsonTestObject {

  public GsonTestObject() {
  }

  ;

  private transient String transientField;

  private Date date;
  private Instant instant;


  public String getTransientField() {
    return transientField;
  }

  public void setTransientField(String transientField) {
    this.transientField = transientField;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Instant getInstant() {
    return instant;
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }
}
