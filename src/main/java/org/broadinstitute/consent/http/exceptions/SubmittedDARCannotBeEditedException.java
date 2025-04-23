package org.broadinstitute.consent.http.exceptions;

public class SubmittedDARCannotBeEditedException extends UnprocessableEntityException {

  public static final String MESSAGE = "This data access request is already submitted for DAC consideration amd cannot be edited.";

  public SubmittedDARCannotBeEditedException() {
    super(MESSAGE);
  }
}
