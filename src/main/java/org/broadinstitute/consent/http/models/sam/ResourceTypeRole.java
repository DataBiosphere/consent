package org.broadinstitute.consent.http.models.sam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResourceTypeRole {

  private List<String> actions;
  private Map<String, List<String>> descendantRoles;
  private List<String> includedRoles;
  private String roleName;

  public List<String> getActions() {
    return actions;
  }

  public ResourceTypeRole setActions(List<String> actions) {
    this.actions = actions;
    return this;
  }

  public void addAction(String action) {
    if (Objects.isNull(this.actions)) {
      this.actions = new ArrayList<>();
    }
    this.actions.add(action);
  }

  public Map<String, List<String>> getDescendantRoles() {
    return descendantRoles;
  }

  public ResourceTypeRole setDescendantRoles(Map<String, List<String>> descendantRoles) {
    this.descendantRoles = descendantRoles;
    return this;
  }

  public void addDescendantRole(Map.Entry<String, List<String>> descendantRole) {
    if (Objects.isNull(this.descendantRoles)) {
      this.descendantRoles = new HashMap<>();
    }
    this.descendantRoles.put(descendantRole.getKey(), descendantRole.getValue());
  }

  public List<String> getIncludedRoles() {
    return includedRoles;
  }

  public ResourceTypeRole setIncludedRoles(List<String> includedRoles) {
    this.includedRoles = includedRoles;
    return this;
  }

  public void addIncludedRole(String includedRole) {
    if (Objects.isNull(this.includedRoles)) {
      this.includedRoles = new ArrayList<>();
    }
    includedRoles.add(includedRole);
  }

  public String getRoleName() {
    return roleName;
  }

  public ResourceTypeRole setRoleName(String roleName) {
    this.roleName = roleName;
    return this;
  }
}
