package org.broadinstitute.consent.http.models;

/**
 * This class is used to represent an authenticated user that exists as a User in the Consent
 * system. It extends the AuthUser class which is not guaranteed to have a user associated with it.
 */
public class DuosUser extends AuthUser {

  private final User user;
  private final AuthUser authUser;

  public DuosUser(AuthUser authUser, User user) {
    super(authUser);
    this.authUser = authUser;
    this.user = user;
  }

  public AuthUser getAuthUser() { return authUser; }

  public User getUser() {
    return user;
  }

  public Integer getUserId() {
    return getUser().getUserId();
  }
}
