package org.broadinstitute.consent.http.models;

import jakarta.ws.rs.BadRequestException;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserRoles;

public record CreateDuosUserRequest(
    String displayName, String email, boolean emailPreference, List<UserRole> roles) {

  public User newUser() {
    if (email == null || email.isBlank()) {
      throw new BadRequestException("Email is required to create a new user.");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new BadRequestException("Display name is required to create a new user.");
    }
    User user = new User();
    user.setEmail(email);
    user.setDisplayName(displayName);
    user.setEmailPreference(emailPreference);
    user.setRoles(roles == null || roles.isEmpty() ? List.of(UserRoles.Researcher()) : roles);
    return user;
  }
}
