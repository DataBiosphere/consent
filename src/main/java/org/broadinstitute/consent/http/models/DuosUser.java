package org.broadinstitute.consent.http.models;

/**
 * This class is used to represent an authenticated user that exists as a previously existing User.
 * It extends the AuthUser class which is not guaranteed to have a user associated with it.
 */
public class DuosUser extends AuthUser {

  private final User user;

  public DuosUser(AuthUser authUser, User user) {
    super(authUser.getAuthToken(),
        authUser.getEmail(),
        authUser.getName(),
        authUser.getAud(),
        authUser.getUserStatusInfo());
    this.user = user;
  }

  public User getUser() {
    return user;
  }

}
