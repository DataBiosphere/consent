package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

import java.security.Principal;

public class AuthUser implements Principal {

  private String authToken;
  private String email;
  private GoogleUser googleUser;
  private String name;
  private UserStatusInfo userStatusInfo;

  public AuthUser() {}

  public AuthUser(String email) {
    this.email = email;
  }

  public AuthUser deepCopy() {
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(this), AuthUser.class);
  }

  public AuthUser(GoogleUser googleUser) {
    this.name = googleUser.getName();
    this.email = googleUser.getEmail();
    this.googleUser = googleUser;
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

  public GoogleUser getGoogleUser() {
    return googleUser;
  }

  public AuthUser setGoogleUser(GoogleUser googleUser) {
    this.googleUser = googleUser;
    return this;
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
