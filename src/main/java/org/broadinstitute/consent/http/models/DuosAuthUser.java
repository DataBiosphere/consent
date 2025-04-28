package org.broadinstitute.consent.http.models;


public class DuosAuthUser extends AuthUser {

  private final User user;

  public DuosAuthUser(AuthUser authUser, User user) {
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
