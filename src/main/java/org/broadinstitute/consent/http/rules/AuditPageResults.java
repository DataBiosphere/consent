package org.broadinstitute.consent.http.rules;

import java.util.List;

public class AuditPageResults {
  private final List<DACAutomationRuleAudit> auditRecords;
  private final Integer
      totalRecords; // total number of records that would have been satisfied by the query without
  // pagination.
  private final Integer pageSize; // number of entries on this page
  private final Integer page; // page number

  public AuditPageResults(
      List<DACAutomationRuleAudit> auditRecords,
      Integer totalRecords,
      Integer pageSize,
      Integer page) {
    this.auditRecords = auditRecords;
    this.totalRecords = totalRecords;
    this.pageSize = pageSize;
    this.page = page;
  }

  public List<DACAutomationRuleAudit> getAuditRecords() {
    return auditRecords;
  }

  public Integer getTotalRecords() {
    return totalRecords;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public Integer getPage() {
    return page;
  }
}
