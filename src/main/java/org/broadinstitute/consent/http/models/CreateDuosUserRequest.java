package org.broadinstitute.consent.http.models;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;

public record CreateDuosUserRequest(
    String displayName, String email, boolean emailPreference, List<UserRole> roles) {

  public void validate() {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required.");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("Display name is required.");
    }
  }

  public User newUser() {
    validate();
    User user = new User();
    user.setEmail(email);
    user.setDisplayName(displayName);
    user.setEmailPreference(emailPreference);
    user.setRoles(roles == null || roles.isEmpty() ? List.of(UserRoles.Researcher()) : roles);
    return user;
  }
}
