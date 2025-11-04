package org.broadinstitute.consent.http.models;

import java.security.Principal;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;

/**
 * This class is used to represent an authenticated user that may or may not exist in Sam or the
 * Consent system.
 */
public class AuthUser implements Principal {

  private String authToken;
  private String email;
  private String name;
  private String aud;
  private UserStatusInfo userStatusInfo;

  public AuthUser() {}

  public AuthUser(AuthUser authUser) {
    this.authToken = authUser.getAuthToken();
    this.email = authUser.getEmail();
    this.name = authUser.getName();
    this.aud = authUser.getAud();
    this.userStatusInfo = authUser.getUserStatusInfo();
  }

  public AuthUser(String authToken, String email, String name, String aud) {
    this.authToken = authToken;
    this.email = email;
    this.name = name;
    this.aud = aud;
  }

  public AuthUser(
      String authToken, String email, String name, String aud, UserStatusInfo userStatusInfo) {
    this.authToken = authToken;
    this.email = email;
    this.name = name;
    this.aud = aud;
    this.userStatusInfo = userStatusInfo;
  }

  public AuthUser(String email) {
    this.email = email;
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

  public UserStatusInfo getUserStatusInfo() {
    return userStatusInfo;
  }

  public AuthUser setUserStatusInfo(UserStatusInfo userStatusInfo) {
    this.userStatusInfo = userStatusInfo;
    return this;
  }
}
