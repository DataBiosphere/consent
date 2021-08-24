package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/** This represents part of the Sam response to GET /api/config/v1/resourceTypes */
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

  public Map<String, List<String>> getDescendantRoles() {
    return descendantRoles;
  }

  public ResourceTypeRole setDescendantRoles(Map<String, List<String>> descendantRoles) {
    this.descendantRoles = descendantRoles;
    return this;
  }

  public List<String> getIncludedRoles() {
    return includedRoles;
  }

  public ResourceTypeRole setIncludedRoles(List<String> includedRoles) {
    this.includedRoles = includedRoles;
    return this;
  }

  public String getRoleName() {
    return roleName;
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
