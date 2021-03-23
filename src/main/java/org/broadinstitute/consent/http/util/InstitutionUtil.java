package org.broadinstitute.consent.http.util;

import java.util.List;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;

public class InstitutionUtil {

  private final GsonBuilder gson;

  @Inject
  public InstitutionUtil() {
    this.gson = new GsonBuilder();
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
    return roles.stream().anyMatch((userRole) -> userRole.getRoleId() == UserRoles.ADMIN.getRoleId());
  }

  private ExclusionStrategy getSerializationExclusionStrategy(Boolean isAdmin) {
    return new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes field) {
        String fieldName = field.getName();
        if (!isAdmin && (fieldName != "id" && fieldName != "name")) {
          return true;
        } else {
          return false;
        }
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
