package org.broadinstitute.consent.http.models.dto.registration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
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
  private List<String> diseaseSpecificUse = new ArrayList<>();

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
  private List<FileTypeObject> fileTypes = new ArrayList<>();

  @JsonProperty("data")
  private Map<String, Object> data = new HashMap<>();

  @JsonProperty("datasetId")
  public Integer getDatasetId() {
    return datasetId;
  }

  @JsonProperty("datasetId")
  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  @JsonProperty("consentGroupName")
  public String getConsentGroupName() {
    return consentGroupName;
  }

  @JsonProperty("consentGroupName")
  public void setConsentGroupName(String consentGroupName) {
    this.consentGroupName = consentGroupName;
  }

  @JsonProperty("dataAccessCommitteeId")
  public Integer getDataAccessCommitteeId() {
    return dataAccessCommitteeId;
  }

  @JsonProperty("dataAccessCommitteeId")
  public void setDataAccessCommitteeId(Integer dataAccessCommitteeId) {
    this.dataAccessCommitteeId = dataAccessCommitteeId;
  }

  @JsonProperty("accessManagement")
  public ConsentGroup.AccessManagement getAccessManagement() {
    return accessManagement;
  }

  @JsonProperty("accessManagement")
  public void setAccessManagement(ConsentGroup.AccessManagement accessManagement) {
    this.accessManagement = accessManagement;
  }

  @JsonProperty("generalResearchUse")
  public Boolean getGeneralResearchUse() {
    return generalResearchUse;
  }

  @JsonProperty("generalResearchUse")
  public void setGeneralResearchUse(Boolean generalResearchUse) {
    this.generalResearchUse = generalResearchUse;
  }

  @JsonProperty("hmb")
  public Boolean getHmb() {
    return hmb;
  }

  @JsonProperty("hmb")
  public void setHmb(Boolean hmb) {
    this.hmb = hmb;
  }

  @JsonProperty("diseaseSpecificUse")
  public List<String> getDiseaseSpecificUse() {
    return diseaseSpecificUse;
  }

  @JsonProperty("diseaseSpecificUse")
  public void setDiseaseSpecificUse(List<String> diseaseSpecificUse) {
    this.diseaseSpecificUse = diseaseSpecificUse;
  }

  @JsonProperty("poa")
  public Boolean getPoa() {
    return poa;
  }

  @JsonProperty("poa")
  public void setPoa(Boolean poa) {
    this.poa = poa;
  }

  @JsonProperty("otherPrimary")
  public String getOtherPrimary() {
    return otherPrimary;
  }

  @JsonProperty("otherPrimary")
  public void setOtherPrimary(String otherPrimary) {
    this.otherPrimary = otherPrimary;
  }

  @JsonProperty("nmds")
  public Boolean getNmds() {
    return nmds;
  }

  @JsonProperty("nmds")
  public void setNmds(Boolean nmds) {
    this.nmds = nmds;
  }

  @JsonProperty("gso")
  public Boolean getGso() {
    return gso;
  }

  @JsonProperty("gso")
  public void setGso(Boolean gso) {
    this.gso = gso;
  }

  @JsonProperty("pub")
  public Boolean getPub() {
    return pub;
  }

  @JsonProperty("pub")
  public void setPub(Boolean pub) {
    this.pub = pub;
  }

  @JsonProperty("col")
  public Boolean getCol() {
    return col;
  }

  @JsonProperty("col")
  public void setCol(Boolean col) {
    this.col = col;
  }

  @JsonProperty("irb")
  public Boolean getIrb() {
    return irb;
  }

  @JsonProperty("irb")
  public void setIrb(Boolean irb) {
    this.irb = irb;
  }

  @JsonProperty("gs")
  public String getGs() {
    return gs;
  }

  @JsonProperty("gs")
  public void setGs(String gs) {
    this.gs = gs;
  }

  @JsonProperty("mor")
  public Boolean getMor() {
    return mor;
  }

  @JsonProperty("mor")
  public void setMor(Boolean mor) {
    this.mor = mor;
  }

  @JsonProperty("morDate")
  public String getMorDate() {
    return morDate;
  }

  @JsonProperty("morDate")
  public void setMorDate(String morDate) {
    this.morDate = morDate;
  }

  @JsonProperty("npu")
  public Boolean getNpu() {
    return npu;
  }

  @JsonProperty("npu")
  public void setNpu(Boolean npu) {
    this.npu = npu;
  }

  @JsonProperty("otherSecondary")
  public String getOtherSecondary() {
    return otherSecondary;
  }

  @JsonProperty("otherSecondary")
  public void setOtherSecondary(String otherSecondary) {
    this.otherSecondary = otherSecondary;
  }

  @JsonProperty("dataLocation")
  public ConsentGroup.DataLocation getDataLocation() {
    return dataLocation;
  }

  @JsonProperty("dataLocation")
  public void setDataLocation(ConsentGroup.DataLocation dataLocation) {
    this.dataLocation = dataLocation;
  }

  @JsonProperty("url")
  public URI getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(URI url) {
    this.url = url;
  }

  @JsonProperty("requestLocation")
  public URI getRequestLocation() {
    return requestLocation;
  }

  @JsonProperty("requestLocation")
  public void setRequestLocation(URI requestLocation) {
    this.requestLocation = requestLocation;
  }

  @JsonProperty("numberOfParticipants")
  public Integer getNumberOfParticipants() {
    return numberOfParticipants;
  }

  @JsonProperty("numberOfParticipants")
  public void setNumberOfParticipants(Integer numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  @JsonProperty("fileTypes")
  public List<FileTypeObject> getFileTypes() {
    return fileTypes;
  }

  @JsonProperty("fileTypes")
  public void setFileTypes(List<FileTypeObject> fileTypes) {
    this.fileTypes = fileTypes;
  }

  @JsonProperty("data")
  public Map<String, Object> getData() {
    return data;
  }

  @JsonProperty("data")
  public void setData(Map<String, Object> data) {
    this.data = data;
  }
}
