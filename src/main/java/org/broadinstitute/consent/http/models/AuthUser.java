package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.security.Principal;
import java.util.Objects;
import org.broadinstitute.consent.http.authentication.GenericUser;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

public class AuthUser implements Principal {

  private String authToken;
  private String email;
  private GenericUser genericUser;
  private String name;
  private UserStatusInfo userStatusInfo;

  public AuthUser() {
  }

  public AuthUser(String email) {
    this.email = email;
  }

  public AuthUser deepCopy() {
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(this), AuthUser.class);
  }

  public AuthUser(GenericUser genericUser) {
    if (Objects.nonNull(genericUser) && Objects.nonNull(genericUser.getName())) {
      this.name = genericUser.getName();
    }
    if (Objects.nonNull(genericUser) && Objects.nonNull(genericUser.getEmail())) {
      this.email = genericUser.getEmail();
    }
    this.genericUser = genericUser;
  }

  public String getAuthToken() {
    return authToken;
  }

  public AuthUser setAuthToken(String authToken) {
    this.authToken = authToken;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public AuthUser setEmail(String email) {
    this.email = email;
    return this;
  }

  public GenericUser getGenericUser() {
    return genericUser;
  }

  @Override
  public String getName() {
    return name;
  }

  public AuthUser setName(String name) {
    this.name = name;
    return this;
  }

  public UserStatusInfo getUserStatusInfo() {
    return userStatusInfo;
  }

  public AuthUser setUserStatusInfo(UserStatusInfo userStatusInfo) {
    this.userStatusInfo = userStatusInfo;
    return this;
  }
}
