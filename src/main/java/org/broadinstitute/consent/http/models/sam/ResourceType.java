package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResourceType {

  private List<ActionPattern> actionPatterns;
  private String name;
  private String ownerRoleName;
  private Boolean reuseIds;
  private List<ResourceTypeRole> roles;

  public List<ActionPattern> getActionPatterns() {
    return actionPatterns;
  }

  public ResourceType setActionPatterns(List<ActionPattern> actionPatterns) {
    this.actionPatterns = actionPatterns;
    return this;
  }

  public void addActionPattern(ActionPattern actionPattern) {
    if ( Objects.isNull(this.actionPatterns)) {
      this.actionPatterns = new ArrayList<>();
    }
    this.actionPatterns.add(actionPattern);
  }

  public String getName() {
    return name;
  }

  public ResourceType setName(String name) {
    this.name = name;
    return this;
  }

  public String getOwnerRoleName() {
    return ownerRoleName;
  }

  public ResourceType setOwnerRoleName(String ownerRoleName) {
    this.ownerRoleName = ownerRoleName;
    return this;
  }

  public Boolean getReuseIds() {
    return reuseIds;
  }

  public ResourceType setReuseIds(Boolean reuseIds) {
    this.reuseIds = reuseIds;
    return this;
  }

  public List<ResourceTypeRole> getRoles() {
    return roles;
  }

  public ResourceType setRoles(List<ResourceTypeRole> roles) {
    this.roles = roles;
    return this;
  }

  public void addRole(ResourceTypeRole role) {
    if (Objects.isNull(this.roles)) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(role);
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
