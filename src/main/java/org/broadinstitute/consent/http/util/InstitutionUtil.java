package org.broadinstitute.consent.http.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.util.gson.InstantTypeAdapter;

public class InstitutionUtil implements ConsentLogger {

  private final GsonBuilder gson;

  public InstitutionUtil() {
    this.gson = new GsonBuilder().registerTypeAdapter(Instant.class, new InstantTypeAdapter());
  }

  // Gson builder and exclusion strategy helpers
  // Opting to not null values, null has the implication of an absence of value
  // Whereas the absence of a field can mean an absence of value OR an omission of data

  public Gson getGsonBuilder(Boolean isAdmin) {
    ExclusionStrategy strategy = getSerializationExclusionStrategy(isAdmin);
    return gson.addSerializationExclusionStrategy(strategy).create();
  }

  public Boolean checkIfAdmin(User user) {
    List<UserRole> roles = user.getRoles();
    if (roles == null || roles.isEmpty()) {
      logWarn("User has no roles: " + user.getEmail());
      return false;
    }
    return roles.stream()
        .anyMatch((userRole) -> Objects.equals(userRole.getRoleId(), UserRoles.ADMIN.getRoleId()));
  }

  private ExclusionStrategy getSerializationExclusionStrategy(Boolean isAdmin) {
    return new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes field) {
        String fieldName = field.getName();

        return !isAdmin && !(fieldName.equals("id")
            || fieldName.equals("name")
            || fieldName.equals("signingOfficials")
            || fieldName.equals("displayName")
            || fieldName.equals("userId")
            || fieldName.equals("email"));
      }

      // NOTE: shouldSkipClass is mandatory when creating an ExclusionStrategy
      // No reason to skip class (only dealing with Institution), so you can just
      // return false here
      @Override
      public boolean shouldSkipClass(Class<?> c) {
        return false;
      }
    };
  }
}
