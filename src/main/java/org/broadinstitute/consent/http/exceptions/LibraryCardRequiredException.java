package org.broadinstitute.consent.http.exceptions;

public class LibraryCardRequiredException extends UnprocessableEntityException{
  public static final String MESSAGE = "Please register in DUOS with your institution's email and obtain a library card from your Signing Official in order to submit a data access request.";
  public LibraryCardRequiredException() {
    super(MESSAGE);
  }
}
