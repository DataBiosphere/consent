package org.broadinstitute.consent.http.models.sam;

import java.util.List;
import java.util.Map;

public class ResourceTypeRole {

  private List<String> actions;
  private Map<String, List<String>> descendantRoles;
  private List<String> includedRoles;
  private String roleName;

  public List<String> getActions() {
    return actions;
  }

  public void setActions(List<String> actions) {
    this.actions = actions;
  }

  public Map<String, List<String>> getDescendantRoles() {
    return descendantRoles;
  }

  public void setDescendantRoles(Map<String, List<String>> descendantRoles) {
    this.descendantRoles = descendantRoles;
  }

  public List<String> getIncludedRoles() {
    return includedRoles;
  }

  public void setIncludedRoles(List<String> includedRoles) {
    this.includedRoles = includedRoles;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
