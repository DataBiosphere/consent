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

  public void setActionPatterns(List<ActionPattern> actionPatterns) {
    this.actionPatterns = actionPatterns;
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

  public void setName(String name) {
    this.name = name;
  }

  public String getOwnerRoleName() {
    return ownerRoleName;
  }

  public void setOwnerRoleName(String ownerRoleName) {
    this.ownerRoleName = ownerRoleName;
  }

  public Boolean getReuseIds() {
    return reuseIds;
  }

  public void setReuseIds(Boolean reuseIds) {
    this.reuseIds = reuseIds;
  }

  public List<ResourceTypeRole> getRoles() {
    return roles;
  }

  public void setRoles(List<ResourceTypeRole> roles) {
    this.roles = roles;
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
