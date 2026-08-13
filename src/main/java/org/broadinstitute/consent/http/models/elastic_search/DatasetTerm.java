package org.broadinstitute.consent.http.models.elastic_search;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.SoApprovalModel;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;

public class DatasetTerm {

  private Integer datasetId;
  private Integer createUserId;
  @Deprecated // Use submitter.displayName instead
  private String createUserDisplayName;
  private String datasetIdentifier;
  private Boolean deletable;
  private String datasetName;
  private Integer participantCount;
  private DataUseSummary dataUse;
  private String dataLocation;
  private String url;
  private String requestLocation;
  private Integer dacId;
  private Boolean dacApproval;
  private String accessManagement;
  private StudyTerm study;
  private UserTerm submitter;
  private UserTerm updateUser;
  private DacTerm dac;
  private SoApprovalModel soApprovalModel;
  private Boolean instantApprovalEligible;
  private Boolean hasInstitutionCertification;
  private Map<String, Object> data;

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public Integer getCreateUserId() {
    return createUserId;
  }

  public void setCreateUserId(Integer createUserId) {
    this.createUserId = createUserId;
  }

  public String getCreateUserDisplayName() {
    return createUserDisplayName;
  }

  public void setCreateUserDisplayName(String createUserDisplayName) {
    this.createUserDisplayName = createUserDisplayName;
  }

  public String getDatasetIdentifier() {
    return datasetIdentifier;
  }

  public void setDatasetIdentifier(String datasetIdentifier) {
    this.datasetIdentifier = datasetIdentifier;
  }

  public Boolean getDeletable() {
    return deletable;
  }

  public void setDeletable(Boolean deletable) {
    this.deletable = deletable;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public Integer getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Integer participantCount) {
    this.participantCount = participantCount;
  }

  public DataUseSummary getDataUse() {
    return dataUse;
  }

  public void setDataUse(DataUseSummary dataUse) {
    this.dataUse = dataUse;
  }

  public String getDataLocation() {
    return dataLocation;
  }

  public void setDataLocation(String dataLocation) {
    this.dataLocation = dataLocation;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRequestLocation() {
    return requestLocation;
  }

  public void setRequestLocation(String requestLocation) {
    this.requestLocation = requestLocation;
  }

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
  }

  public Boolean getDacApproval() {
    return dacApproval;
  }

  public void setDacApproval(Boolean dacApproval) {
    this.dacApproval = dacApproval;
  }

  public String getAccessManagement() {
    return accessManagement;
  }

  public void setAccessManagement(String accessManagement) {
    this.accessManagement = accessManagement;
  }

  public StudyTerm getStudy() {
    return study;
  }

  public void setStudy(StudyTerm study) {
    this.study = study;
  }

  public UserTerm getSubmitter() {
    return submitter;
  }

  public void setSubmitter(UserTerm submitter) {
    this.submitter = submitter;
  }

  public UserTerm getUpdateUser() {
    return updateUser;
  }

  public void setUpdateUser(UserTerm updateUser) {
    this.updateUser = updateUser;
  }

  public DacTerm getDac() {
    return dac;
  }

  public void setDac(DacTerm dac) {
    this.dac = dac;
  }

  public SoApprovalModel getSoApprovalModel() {
    return soApprovalModel;
  }

  public void setSoApprovalModel(SoApprovalModel soApprovalModel) {
    this.soApprovalModel = soApprovalModel;
  }

  public Boolean getInstantApprovalEligible() {
    return instantApprovalEligible;
  }

  public void setInstantApprovalEligible(Boolean instantApprovalEligible) {
    this.instantApprovalEligible = instantApprovalEligible;
  }

  public Boolean getHasInstitutionCertification() {
    return hasInstitutionCertification;
  }

  public void setHasInstitutionCertification(Boolean hasInstitutionCertification) {
    this.hasInstitutionCertification = hasInstitutionCertification;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  public Map<String, Object> getData() {
    return this.data;
  }
}
