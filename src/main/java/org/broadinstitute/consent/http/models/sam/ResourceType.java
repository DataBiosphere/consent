package org.broadinstitute.consent.http.models.sam;

import java.util.List;

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
}
