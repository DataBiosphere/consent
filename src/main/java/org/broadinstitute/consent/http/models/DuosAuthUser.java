package org.broadinstitute.consent.http.models;


public class DuosAuthUser extends AuthUser {

  private final User user;

  public DuosAuthUser(AuthUser authUser, User user) {
    super.setAuthToken(authUser.getAuthToken());
    super.setEmail(authUser.getEmail());
    super.setName(authUser.getName());
    super.setAud(authUser.getAud());
    super.setUserStatusInfo(authUser.getUserStatusInfo());
    this.user = user;
  }

  public User getUser() {
    return user;
  }

}
