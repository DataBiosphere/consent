package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

public class ResourceTypeRole {

  private List<String> actions;
  private Map<String, List<String>> descendantRoles;
  private List<String> includedRoles;
  private String roleName;

  public ResourceTypeRole setActions(List<String> actions) {
    this.actions = actions;
    return this;
  }

  public ResourceTypeRole setDescendantRoles(Map<String, List<String>> descendantRoles) {
    this.descendantRoles = descendantRoles;
    return this;
  }

  public ResourceTypeRole setIncludedRoles(List<String> includedRoles) {
    this.includedRoles = includedRoles;
    return this;
  }

  public ResourceTypeRole setRoleName(String roleName) {
    this.roleName = roleName;
    return this;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
