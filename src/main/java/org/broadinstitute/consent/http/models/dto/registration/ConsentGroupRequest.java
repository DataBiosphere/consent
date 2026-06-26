package org.broadinstitute.consent.http.models.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  "datasetId",
  "consentGroupName",
  "accessManagement",
  "generalResearchUse",
  "hmb",
  "diseaseSpecificUse",
  "poa",
  "otherPrimary",
  "nmds",
  "gso",
  "pub",
  "col",
  "irb",
  "gs",
  "mor",
  "morDate",
  "npu",
  "otherSecondary",
  "dataAccessCommitteeId",
  "dataLocation",
  "url",
  "requestLocation",
  "numberOfParticipants",
  "fileTypes",
  "data"
})
public class ConsentGroupRequest {

  @JsonProperty("datasetId")
  private Integer datasetId;

  @JsonProperty("consentGroupName")
  private String consentGroupName;

  @JsonProperty("dataAccessCommitteeId")
  private Integer dataAccessCommitteeId;

  @JsonProperty("accessManagement")
  private ConsentGroup.AccessManagement accessManagement;

  @JsonProperty("generalResearchUse")
  private Boolean generalResearchUse;

  @JsonProperty("hmb")
  private Boolean hmb;

  @JsonProperty("diseaseSpecificUse")
  private List<String> diseaseSpecificUse;

  @JsonProperty("poa")
  private Boolean poa;

  @JsonProperty("otherPrimary")
  private String otherPrimary;

  @JsonProperty("nmds")
  private Boolean nmds;

  @JsonProperty("gso")
  private Boolean gso;

  @JsonProperty("pub")
  private Boolean pub;

  @JsonProperty("col")
  private Boolean col;

  @JsonProperty("irb")
  private Boolean irb;

  @JsonProperty("gs")
  private String gs;

  @JsonProperty("mor")
  private Boolean mor;

  @JsonProperty("morDate")
  private String morDate;

  @JsonProperty("npu")
  private Boolean npu;

  @JsonProperty("otherSecondary")
  private String otherSecondary;

  @JsonProperty("dataLocation")
  private ConsentGroup.DataLocation dataLocation;

  @JsonProperty("url")
  private URI url;

  @JsonProperty("requestLocation")
  private URI requestLocation;

  @JsonProperty("numberOfParticipants")
  private Integer numberOfParticipants;

  @JsonProperty("fileTypes")
  private List<FileTypeObject> fileTypes;

  @JsonProperty("data")
  private Map<String, Object> data;

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public String getConsentGroupName() {
    return consentGroupName;
  }

  public void setConsentGroupName(String consentGroupName) {
    this.consentGroupName = consentGroupName;
  }

  public Integer getDataAccessCommitteeId() {
    return dataAccessCommitteeId;
  }

  public void setDataAccessCommitteeId(Integer dataAccessCommitteeId) {
    this.dataAccessCommitteeId = dataAccessCommitteeId;
  }

  public ConsentGroup.AccessManagement getAccessManagement() {
    return accessManagement;
  }

  public void setAccessManagement(ConsentGroup.AccessManagement accessManagement) {
    this.accessManagement = accessManagement;
  }

  public Boolean getGeneralResearchUse() {
    return generalResearchUse;
  }

  public void setGeneralResearchUse(Boolean generalResearchUse) {
    this.generalResearchUse = generalResearchUse;
  }

  public Boolean getHmb() {
    return hmb;
  }

  public void setHmb(Boolean hmb) {
    this.hmb = hmb;
  }

  public List<String> getDiseaseSpecificUse() {
    return diseaseSpecificUse;
  }

  public void setDiseaseSpecificUse(List<String> diseaseSpecificUse) {
    this.diseaseSpecificUse = diseaseSpecificUse;
  }

  public Boolean getPoa() {
    return poa;
  }

  public void setPoa(Boolean poa) {
    this.poa = poa;
  }

  public String getOtherPrimary() {
    return otherPrimary;
  }

  public void setOtherPrimary(String otherPrimary) {
    this.otherPrimary = otherPrimary;
  }

  public Boolean getNmds() {
    return nmds;
  }

  public void setNmds(Boolean nmds) {
    this.nmds = nmds;
  }

  public Boolean getGso() {
    return gso;
  }

  public void setGso(Boolean gso) {
    this.gso = gso;
  }

  public Boolean getPub() {
    return pub;
  }

  public void setPub(Boolean pub) {
    this.pub = pub;
  }

  public Boolean getCol() {
    return col;
  }

  public void setCol(Boolean col) {
    this.col = col;
  }

  public Boolean getIrb() {
    return irb;
  }

  public void setIrb(Boolean irb) {
    this.irb = irb;
  }

  public String getGs() {
    return gs;
  }

  public void setGs(String gs) {
    this.gs = gs;
  }

  public Boolean getMor() {
    return mor;
  }

  public void setMor(Boolean mor) {
    this.mor = mor;
  }

  public String getMorDate() {
    return morDate;
  }

  public void setMorDate(String morDate) {
    this.morDate = morDate;
  }

  public Boolean getNpu() {
    return npu;
  }

  public void setNpu(Boolean npu) {
    this.npu = npu;
  }

  public String getOtherSecondary() {
    return otherSecondary;
  }

  public void setOtherSecondary(String otherSecondary) {
    this.otherSecondary = otherSecondary;
  }

  public ConsentGroup.DataLocation getDataLocation() {
    return dataLocation;
  }

  public void setDataLocation(ConsentGroup.DataLocation dataLocation) {
    this.dataLocation = dataLocation;
  }

  public URI getUrl() {
    return url;
  }

  public void setUrl(URI url) {
    this.url = url;
  }

  public URI getRequestLocation() {
    return requestLocation;
  }

  public void setRequestLocation(URI requestLocation) {
    this.requestLocation = requestLocation;
  }

  public Integer getNumberOfParticipants() {
    return numberOfParticipants;
  }

  public void setNumberOfParticipants(Integer numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  public List<FileTypeObject> getFileTypes() {
    return fileTypes;
  }

  public void setFileTypes(List<FileTypeObject> fileTypes) {
    this.fileTypes = fileTypes;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
