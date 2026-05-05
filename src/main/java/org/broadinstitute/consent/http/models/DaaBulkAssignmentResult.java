package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Result object for bulk DAA assignment operations to all eligible users. */
public class DaaBulkAssignmentResult {

  @JsonProperty private Integer daaId;

  @JsonProperty private Integer totalEligibleUsers;

  @JsonProperty private Integer assignedCount;

  @JsonProperty private Integer skippedCount;

  @JsonProperty private List<String> errors;

  public DaaBulkAssignmentResult() {}

  public DaaBulkAssignmentResult(
      Integer daaId, Integer totalEligibleUsers, Integer assignedCount, Integer skippedCount) {
    this.daaId = daaId;
    this.totalEligibleUsers = totalEligibleUsers;
    this.assignedCount = assignedCount;
    this.skippedCount = skippedCount;
    this.errors = List.of();
  }

  public DaaBulkAssignmentResult(
      Integer daaId,
      Integer totalEligibleUsers,
      Integer assignedCount,
      Integer skippedCount,
      List<String> errors) {
    this.daaId = daaId;
    this.totalEligibleUsers = totalEligibleUsers;
    this.assignedCount = assignedCount;
    this.skippedCount = skippedCount;
    this.errors = errors;
  }

  public Integer getDaaId() {
    return daaId;
  }

  public void setDaaId(Integer daaId) {
    this.daaId = daaId;
  }

  public Integer getTotalEligibleUsers() {
    return totalEligibleUsers;
  }

  public void setTotalEligibleUsers(Integer totalEligibleUsers) {
    this.totalEligibleUsers = totalEligibleUsers;
  }

  public Integer getAssignedCount() {
    return assignedCount;
  }

  public void setAssignedCount(Integer assignedCount) {
    this.assignedCount = assignedCount;
  }

  public Integer getSkippedCount() {
    return skippedCount;
  }

  public void setSkippedCount(Integer skippedCount) {
    this.skippedCount = skippedCount;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }
}
