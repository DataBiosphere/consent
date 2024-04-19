package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.security.Principal;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

public class AuthUser implements Principal {

  private String authToken;
  private String email;
  private String name;
  private String aud;
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

  @Override
  public String getName() {
    return name;
  }

  public AuthUser setName(String name) {
    this.name = name;
    return this;
  }

  public String getAud() {
    return aud;
  }

  public AuthUser setAud(String aud) {
    this.aud = aud;
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
