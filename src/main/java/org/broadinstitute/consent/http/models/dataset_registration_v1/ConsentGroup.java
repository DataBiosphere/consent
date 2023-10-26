package org.broadinstitute.consent.http.models.dataset_registration_v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "datasetId",
    "datasetIdentifier",
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
    "numberOfParticipants",
    "fileTypes"
})
public class ConsentGroup {

  /**
   * Dataset Id
   */
  @JsonProperty("datasetId")
  @JsonPropertyDescription("Dataset Id")
  private Integer datasetId;
  /**
   * Dataset Identifier
   */
  @JsonProperty("datasetIdentifier")
  @JsonPropertyDescription("Dataset Identifier")
  private String datasetIdentifier;
  /**
   * Consent Group Name
   */
  @JsonProperty("consentGroupName")
  @JsonPropertyDescription("Consent Group Name")
  private String consentGroupName;
  /**
   * No Restrictions
   */
  @JsonProperty("accessManagement")
  @JsonPropertyDescription("One of Controlled, Open, or External")
  private AccessManagement accessManagement;
  /**
   * General Research Use
   */
  @JsonProperty("generalResearchUse")
  @JsonPropertyDescription("General Research Use")
  private Boolean generalResearchUse;
  /**
   * Health/Medical/Biomedical Research Use
   */
  @JsonProperty("hmb")
  @JsonPropertyDescription("Health/Medical/Biomedical Research Use")
  private Boolean hmb;
  /**
   * Disease-Specific Research Use
   */
  @JsonProperty("diseaseSpecificUse")
  @JsonPropertyDescription("Disease-Specific Research Use")
  private List<String> diseaseSpecificUse = new ArrayList<String>();
  /**
   * Populations, Origins, Ancestry Use
   */
  @JsonProperty("poa")
  @JsonPropertyDescription("Populations, Origins, Ancestry Use")
  private Boolean poa;
  /**
   * Other
   */
  @JsonProperty("otherPrimary")
  @JsonPropertyDescription("Other")
  private String otherPrimary;
  /**
   * No Methods Development or validation studies (NMDS)
   */
  @JsonProperty("nmds")
  @JsonPropertyDescription("No Methods Development or validation studies (NMDS)")
  private Boolean nmds;
  /**
   * Genetic studies only (GSO)
   */
  @JsonProperty("gso")
  @JsonPropertyDescription("Genetic studies only (GSO)")
  private Boolean gso;
  /**
   * Publication Required (PUB)
   */
  @JsonProperty("pub")
  @JsonPropertyDescription("Publication Required (PUB)")
  private Boolean pub;
  /**
   * Collaboration Required (COL)
   */
  @JsonProperty("col")
  @JsonPropertyDescription("Collaboration Required (COL)")
  private Boolean col;
  /**
   * Ethics Approval Required (IRB)
   */
  @JsonProperty("irb")
  @JsonPropertyDescription("Ethics Approval Required (IRB)")
  private Boolean irb;
  /**
   * Geographic Restriction (GS-)
   */
  @JsonProperty("gs")
  @JsonPropertyDescription("Geographic Restriction (GS-)")
  private String gs;
  /**
   * Publication Moratorium (MOR)
   */
  @JsonProperty("mor")
  @JsonPropertyDescription("Publication Moratorium (MOR)")
  private Boolean mor;
  /**
   * Publication Moratorium Date (MOR)
   */
  @JsonProperty("morDate")
  @JsonPropertyDescription("Publication Moratorium Date (MOR)")
  private String morDate;
  /**
   * Non-profit Use Only (NPU)
   */
  @JsonProperty("npu")
  @JsonPropertyDescription("Non-profit Use Only (NPU)")
  private Boolean npu;
  /**
   * Other
   */
  @JsonProperty("otherSecondary")
  @JsonPropertyDescription("Other")
  private String otherSecondary;
  /**
   * Data Access Committee ID
   */
  @JsonProperty("dataAccessCommitteeId")
  @JsonPropertyDescription("Data Access Committee ID")
  private Integer dataAccessCommitteeId;
  /**
   * Data Location
   */
  @JsonProperty("dataLocation")
  @JsonPropertyDescription("Data Location")
  private ConsentGroup.DataLocation dataLocation;
  /**
   * Free text field for entering URL of data
   */
  @JsonProperty("url")
  @JsonPropertyDescription("Free text field for entering URL of data")
  private URI url;
  /**
   * # of Participants (Required)
   */
  @JsonProperty("numberOfParticipants")
  @JsonPropertyDescription("# of Participants")
  private Integer numberOfParticipants;
  /**
   * List of File Types
   */
  @JsonProperty("fileTypes")
  @JsonPropertyDescription("List of File Types")
  private List<FileTypeObject> fileTypes = new ArrayList<FileTypeObject>();

  /**
   * Dataset Id
   */
  @JsonProperty("datasetId")
  public Integer getDatasetId() {
    return datasetId;
  }

  /**
   * Dataset Id
   */
  @JsonProperty("datasetId")
  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  /**
   * Dataset Identifier
   */
  @JsonProperty("datasetIdentifier")
  public String getDatasetIdentifier() {
    return datasetIdentifier;
  }

  /**
   * Dataset Identifier
   */
  @JsonProperty("datasetIdentifier")
  public void setDatasetIdentifier(String datasetIdentifier) {
    this.datasetIdentifier = datasetIdentifier;
  }

  /**
   * Consent Group Name
   */
  @JsonProperty("consentGroupName")
  public String getConsentGroupName() {
    return consentGroupName;
  }

  /**
   * Consent Group Name
   */
  @JsonProperty("consentGroupName")
  public void setConsentGroupName(String consentGroupName) {
    this.consentGroupName = consentGroupName;
  }

  /**
   * No Restrictions
   */
  @JsonProperty("accessManagement")
  public AccessManagement getAccessManagement() {
    return accessManagement;
  }

  /**
   * No Restrictions
   */
  @JsonProperty("accessManagement")
  public void setAccessManagement(AccessManagement accessManagement) {
    this.accessManagement = accessManagement;
  }

  /**
   * General Research Use
   */
  @JsonProperty("generalResearchUse")
  public Boolean getGeneralResearchUse() {
    return generalResearchUse;
  }

  /**
   * General Research Use
   */
  @JsonProperty("generalResearchUse")
  public void setGeneralResearchUse(Boolean generalResearchUse) {
    this.generalResearchUse = generalResearchUse;
  }

  /**
   * Health/Medical/Biomedical Research Use
   */
  @JsonProperty("hmb")
  public Boolean getHmb() {
    return hmb;
  }

  /**
   * Health/Medical/Biomedical Research Use
   */
  @JsonProperty("hmb")
  public void setHmb(Boolean hmb) {
    this.hmb = hmb;
  }

  /**
   * Disease-Specific Research Use
   */
  @JsonProperty("diseaseSpecificUse")
  public List<String> getDiseaseSpecificUse() {
    return diseaseSpecificUse;
  }

  /**
   * Disease-Specific Research Use
   */
  @JsonProperty("diseaseSpecificUse")
  public void setDiseaseSpecificUse(List<String> diseaseSpecificUse) {
    this.diseaseSpecificUse = diseaseSpecificUse;
  }

  /**
   * Populations, Origins, Ancestry Use
   */
  @JsonProperty("poa")
  public Boolean getPoa() {
    return poa;
  }

  /**
   * Populations, Origins, Ancestry Use
   */
  @JsonProperty("poa")
  public void setPoa(Boolean poa) {
    this.poa = poa;
  }

  /**
   * Other
   */
  @JsonProperty("otherPrimary")
  public String getOtherPrimary() {
    return otherPrimary;
  }

  /**
   * Other
   */
  @JsonProperty("otherPrimary")
  public void setOtherPrimary(String otherPrimary) {
    this.otherPrimary = otherPrimary;
  }

  /**
   * No Methods Development or validation studies (NMDS)
   */
  @JsonProperty("nmds")
  public Boolean getNmds() {
    return nmds;
  }

  /**
   * No Methods Development or validation studies (NMDS)
   */
  @JsonProperty("nmds")
  public void setNmds(Boolean nmds) {
    this.nmds = nmds;
  }

  /**
   * Genetic studies only (GSO)
   */
  @JsonProperty("gso")
  public Boolean getGso() {
    return gso;
  }

  /**
   * Genetic studies only (GSO)
   */
  @JsonProperty("gso")
  public void setGso(Boolean gso) {
    this.gso = gso;
  }

  /**
   * Publication Required (PUB)
   */
  @JsonProperty("pub")
  public Boolean getPub() {
    return pub;
  }

  /**
   * Publication Required (PUB)
   */
  @JsonProperty("pub")
  public void setPub(Boolean pub) {
    this.pub = pub;
  }

  /**
   * Collaboration Required (COL)
   */
  @JsonProperty("col")
  public Boolean getCol() {
    return col;
  }

  /**
   * Collaboration Required (COL)
   */
  @JsonProperty("col")
  public void setCol(Boolean col) {
    this.col = col;
  }

  /**
   * Ethics Approval Required (IRB)
   */
  @JsonProperty("irb")
  public Boolean getIrb() {
    return irb;
  }

  /**
   * Ethics Approval Required (IRB)
   */
  @JsonProperty("irb")
  public void setIrb(Boolean irb) {
    this.irb = irb;
  }

  /**
   * Geographic Restriction (GS-)
   */
  @JsonProperty("gs")
  public String getGs() {
    return gs;
  }

  /**
   * Geographic Restriction (GS-)
   */
  @JsonProperty("gs")
  public void setGs(String gs) {
    this.gs = gs;
  }

  /**
   * Publication Moratorium (MOR)
   */
  @JsonProperty("mor")
  public Boolean getMor() {
    return mor;
  }

  /**
   * Publication Moratorium (MOR)
   */
  @JsonProperty("mor")
  public void setMor(Boolean mor) {
    this.mor = mor;
  }

  /**
   * Publication Moratorium Date (MOR)
   */
  @JsonProperty("morDate")
  public String getMorDate() {
    return morDate;
  }

  /**
   * Publication Moratorium Date (MOR)
   */
  @JsonProperty("morDate")
  public void setMorDate(String morDate) {
    this.morDate = morDate;
  }

  /**
   * Non-profit Use Only (NPU)
   */
  @JsonProperty("npu")
  public Boolean getNpu() {
    return npu;
  }

  /**
   * Non-profit Use Only (NPU)
   */
  @JsonProperty("npu")
  public void setNpu(Boolean npu) {
    this.npu = npu;
  }

  /**
   * Other
   */
  @JsonProperty("otherSecondary")
  public String getOtherSecondary() {
    return otherSecondary;
  }

  /**
   * Other
   */
  @JsonProperty("otherSecondary")
  public void setOtherSecondary(String otherSecondary) {
    this.otherSecondary = otherSecondary;
  }

  /**
   * Data Access Committee ID
   */
  @JsonProperty("dataAccessCommitteeId")
  public Integer getDataAccessCommitteeId() {
    return dataAccessCommitteeId;
  }

  /**
   * Data Access Committee ID
   */
  @JsonProperty("dataAccessCommitteeId")
  public void setDataAccessCommitteeId(Integer dataAccessCommitteeId) {
    this.dataAccessCommitteeId = dataAccessCommitteeId;
  }

  /**
   * Data Location
   */
  @JsonProperty("dataLocation")
  public ConsentGroup.DataLocation getDataLocation() {
    return dataLocation;
  }

  /**
   * Data Location
   */
  @JsonProperty("dataLocation")
  public void setDataLocation(ConsentGroup.DataLocation dataLocation) {
    this.dataLocation = dataLocation;
  }

  /**
   * Free text field for entering URL of data
   */
  @JsonProperty("url")
  public URI getUrl() {
    return url;
  }

  /**
   * Free text field for entering URL of data
   */
  @JsonProperty("url")
  public void setUrl(URI url) {
    this.url = url;
  }

  /**
   * # of Participants (Required)
   */
  @JsonProperty("numberOfParticipants")
  public Integer getNumberOfParticipants() {
    return numberOfParticipants;
  }

  /**
   * # of Participants (Required)
   */
  @JsonProperty("numberOfParticipants")
  public void setNumberOfParticipants(Integer numberOfParticipants) {
    this.numberOfParticipants = numberOfParticipants;
  }

  /**
   * List of File Types
   */
  @JsonProperty("fileTypes")
  public List<FileTypeObject> getFileTypes() {
    return fileTypes;
  }

  /**
   * List of File Types
   */
  @JsonProperty("fileTypes")
  public void setFileTypes(List<FileTypeObject> fileTypes) {
    this.fileTypes = fileTypes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(ConsentGroup.class.getName()).append('@')
        .append(Integer.toHexString(System.identityHashCode(this))).append('[');
    sb.append("datasetId");
    sb.append('=');
    sb.append(((this.datasetId == null) ? "<null>" : this.datasetId));
    sb.append(',');
    sb.append("datasetIdentifier");
    sb.append('=');
    sb.append(((this.datasetIdentifier == null) ? "<null>" : this.datasetIdentifier));
    sb.append(',');
    sb.append("consentGroupName");
    sb.append('=');
    sb.append(((this.consentGroupName == null) ? "<null>" : this.consentGroupName));
    sb.append(',');
    sb.append("accessManagement");
    sb.append('=');
    sb.append(((this.accessManagement == null) ? "<null>" : this.accessManagement));
    sb.append(',');
    sb.append("generalResearchUse");
    sb.append('=');
    sb.append(((this.generalResearchUse == null) ? "<null>" : this.generalResearchUse));
    sb.append(',');
    sb.append("hmb");
    sb.append('=');
    sb.append(((this.hmb == null) ? "<null>" : this.hmb));
    sb.append(',');
    sb.append("diseaseSpecificUse");
    sb.append('=');
    sb.append(((this.diseaseSpecificUse == null) ? "<null>" : this.diseaseSpecificUse));
    sb.append(',');
    sb.append("poa");
    sb.append('=');
    sb.append(((this.poa == null) ? "<null>" : this.poa));
    sb.append(',');
    sb.append("otherPrimary");
    sb.append('=');
    sb.append(((this.otherPrimary == null) ? "<null>" : this.otherPrimary));
    sb.append(',');
    sb.append("nmds");
    sb.append('=');
    sb.append(((this.nmds == null) ? "<null>" : this.nmds));
    sb.append(',');
    sb.append("gso");
    sb.append('=');
    sb.append(((this.gso == null) ? "<null>" : this.gso));
    sb.append(',');
    sb.append("pub");
    sb.append('=');
    sb.append(((this.pub == null) ? "<null>" : this.pub));
    sb.append(',');
    sb.append("col");
    sb.append('=');
    sb.append(((this.col == null) ? "<null>" : this.col));
    sb.append(',');
    sb.append("irb");
    sb.append('=');
    sb.append(((this.irb == null) ? "<null>" : this.irb));
    sb.append(',');
    sb.append("gs");
    sb.append('=');
    sb.append(((this.gs == null) ? "<null>" : this.gs));
    sb.append(',');
    sb.append("mor");
    sb.append('=');
    sb.append(((this.mor == null) ? "<null>" : this.mor));
    sb.append(',');
    sb.append("morDate");
    sb.append('=');
    sb.append(((this.morDate == null) ? "<null>" : this.morDate));
    sb.append(',');
    sb.append("npu");
    sb.append('=');
    sb.append(((this.npu == null) ? "<null>" : this.npu));
    sb.append(',');
    sb.append("otherSecondary");
    sb.append('=');
    sb.append(((this.otherSecondary == null) ? "<null>" : this.otherSecondary));
    sb.append(',');
    sb.append("dataAccessCommitteeId");
    sb.append('=');
    sb.append(((this.dataAccessCommitteeId == null) ? "<null>" : this.dataAccessCommitteeId));
    sb.append(',');
    sb.append("dataLocation");
    sb.append('=');
    sb.append(((this.dataLocation == null) ? "<null>" : this.dataLocation));
    sb.append(',');
    sb.append("url");
    sb.append('=');
    sb.append(((this.url == null) ? "<null>" : this.url));
    sb.append(',');
    sb.append("numberOfParticipants");
    sb.append('=');
    sb.append(((this.numberOfParticipants == null) ? "<null>" : this.numberOfParticipants));
    sb.append(',');
    sb.append("fileTypes");
    sb.append('=');
    sb.append(((this.fileTypes == null) ? "<null>" : this.fileTypes));
    sb.append(',');
    if (sb.charAt((sb.length() - 1)) == ',') {
      sb.setCharAt((sb.length() - 1), ']');
    } else {
      sb.append(']');
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = ((result * 31) + ((this.col == null) ? 0 : this.col.hashCode()));
    result = ((result * 31) + ((this.gso == null) ? 0 : this.gso.hashCode()));
    result = ((result * 31) + ((this.generalResearchUse == null) ? 0
        : this.generalResearchUse.hashCode()));
    result = ((result * 31) + ((this.poa == null) ? 0 : this.poa.hashCode()));
    result = ((result * 31) + ((this.morDate == null) ? 0 : this.morDate.hashCode()));
    result = ((result * 31) + ((this.dataAccessCommitteeId == null) ? 0
        : this.dataAccessCommitteeId.hashCode()));
    result = ((result * 31) + ((this.otherPrimary == null) ? 0 : this.otherPrimary.hashCode()));
    result = ((result * 31) + ((this.gs == null) ? 0 : this.gs.hashCode()));
    result = ((result * 31) + ((this.url == null) ? 0 : this.url.hashCode()));
    result = ((result * 31) + ((this.numberOfParticipants == null) ? 0
        : this.numberOfParticipants.hashCode()));
    result = ((result * 31) + ((this.fileTypes == null) ? 0 : this.fileTypes.hashCode()));
    result = ((result * 31) + ((this.diseaseSpecificUse == null) ? 0
        : this.diseaseSpecificUse.hashCode()));
    result = ((result * 31) + ((this.datasetId == null) ? 0 : this.datasetId.hashCode()));
    result = ((result * 31) + ((this.datasetIdentifier == null) ? 0
        : this.datasetIdentifier.hashCode()));
    result = ((result * 31) + ((this.consentGroupName == null) ? 0
        : this.consentGroupName.hashCode()));
    result = ((result * 31) + ((this.mor == null) ? 0 : this.mor.hashCode()));
    result = ((result * 31) + ((this.npu == null) ? 0 : this.npu.hashCode()));
    result = ((result * 31) + ((this.dataLocation == null) ? 0 : this.dataLocation.hashCode()));
    result = ((result * 31) + ((this.accessManagement == null) ? 0
        : this.accessManagement.hashCode()));
    result = ((result * 31) + ((this.irb == null) ? 0 : this.irb.hashCode()));
    result = ((result * 31) + ((this.hmb == null) ? 0 : this.hmb.hashCode()));
    result = ((result * 31) + ((this.pub == null) ? 0 : this.pub.hashCode()));
    result = ((result * 31) + ((this.nmds == null) ? 0 : this.nmds.hashCode()));
    result = ((result * 31) + ((this.otherSecondary == null) ? 0 : this.otherSecondary.hashCode()));
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if ((other instanceof ConsentGroup) == false) {
      return false;
    }
    ConsentGroup rhs = ((ConsentGroup) other);
    return ((((((((((((((((
        ((((((this.col == rhs.col) || ((this.col != null) && this.col.equals(rhs.col))) && (
            (this.gso == rhs.gso) || ((this.gso != null) && this.gso.equals(rhs.gso)))) && (
            (this.generalResearchUse == rhs.generalResearchUse) || (
                (this.generalResearchUse != null) && this.generalResearchUse.equals(
                    rhs.generalResearchUse)))) && ((this.poa == rhs.poa) || ((this.poa != null)
            && this.poa.equals(rhs.poa)))) && ((this.morDate == rhs.morDate) || (
            (this.morDate != null) && this.morDate.equals(rhs.morDate)))) && (
            (this.dataAccessCommitteeId == rhs.dataAccessCommitteeId) || (
                (this.dataAccessCommitteeId != null) && this.dataAccessCommitteeId.equals(
                    rhs.dataAccessCommitteeId)))) && ((this.otherPrimary == rhs.otherPrimary) || (
        (this.otherPrimary != null) && this.otherPrimary.equals(rhs.otherPrimary)))) && (
        (this.gs == rhs.gs) || ((this.gs != null) && this.gs.equals(rhs.gs)))) && (
        (this.url == rhs.url) || ((this.url != null) && this.url.equals(rhs.url)))) && (
        (this.fileTypes == rhs.fileTypes) || ((this.fileTypes != null) && this.fileTypes.equals(
            rhs.fileTypes)))) && ((this.diseaseSpecificUse == rhs.diseaseSpecificUse) || (
        (this.diseaseSpecificUse != null) && this.diseaseSpecificUse.equals(
            rhs.diseaseSpecificUse)))) && ((this.datasetId == rhs.datasetId) || (
        (this.datasetId != null) && this.datasetId.equals(rhs.datasetId))) && (
        (this.datasetIdentifier == rhs.datasetIdentifier) || (
            (this.datasetIdentifier != null) && this.datasetIdentifier.equals(
                rhs.datasetIdentifier))) && (
        (this.consentGroupName == rhs.consentGroupName) || (
            (this.consentGroupName != null) && this.consentGroupName.equals(rhs.consentGroupName))))
        && ((this.mor == rhs.mor) || ((this.mor != null) && this.mor.equals(rhs.mor)))) && (
        (this.npu == rhs.npu) || ((this.npu != null) && this.npu.equals(rhs.npu)))) && (
        (this.dataLocation == rhs.dataLocation) || ((this.dataLocation != null)
            && this.dataLocation.equals(rhs.dataLocation)))) && (
        (this.accessManagement == rhs.accessManagement)
            || ((this.accessManagement != null) && this.accessManagement.equals(
            rhs.accessManagement)))) && (
        (this.irb == rhs.irb) || ((this.irb != null) && this.irb.equals(rhs.irb)))) && (
        (this.hmb == rhs.hmb) || ((this.hmb != null) && this.hmb.equals(rhs.hmb)))) && (
        (this.pub == rhs.pub) || ((this.pub != null) && this.pub.equals(rhs.pub)))) && (
        (this.nmds == rhs.nmds) || ((this.nmds != null) && this.nmds.equals(rhs.nmds)))) && (
        (this.otherSecondary == rhs.otherSecondary) || ((this.otherSecondary != null)
            && this.otherSecondary.equals(rhs.otherSecondary))));
  }


  /**
   * Data Location
   */
  public enum DataLocation {

    AN_VIL_WORKSPACE("AnVIL Workspace"),
    TERRA_WORKSPACE("Terra Workspace"),
    TDR_LOCATION("TDR Location"),
    NOT_DETERMINED("Not Determined");
    private final String value;
    private final static Map<String, ConsentGroup.DataLocation> CONSTANTS = new HashMap<String, ConsentGroup.DataLocation>();

    static {
      for (ConsentGroup.DataLocation c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    DataLocation(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    @JsonValue
    public String value() {
      return this.value;
    }

    @JsonCreator
    public static ConsentGroup.DataLocation fromValue(String value) {
      ConsentGroup.DataLocation constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }

  }

  /*
   * Access Management
   */
  public enum AccessManagement {
    OPEN("open"),
    CONTROLLED("controlled"),
    EXTERNAL("external");
    private final String value;
    private final static Map<String, ConsentGroup.AccessManagement> CONSTANTS = new HashMap<String, ConsentGroup.AccessManagement>();

    static {
      for (ConsentGroup.AccessManagement c : values()) {
        CONSTANTS.put(c.value, c);
      }
    }

    AccessManagement(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

    @JsonValue
    public String value() {
      return this.value;
    }

    @JsonCreator
    public static ConsentGroup.AccessManagement fromValue(String value) {
      ConsentGroup.AccessManagement constant = CONSTANTS.get(value);
      if (constant == null) {
        throw new IllegalArgumentException(value);
      } else {
        return constant;
      }
    }
  }

  public boolean isInvalidForUpdate() {
    return Objects.nonNull(this.accessManagement) ||
        Objects.nonNull(this.generalResearchUse) ||
        Objects.nonNull(this.hmb) ||
        (Objects.nonNull(this.diseaseSpecificUse) && this.diseaseSpecificUse.size() > 0) ||
        Objects.nonNull(this.poa) ||
        Objects.nonNull(this.otherPrimary) ||
        Objects.nonNull(this.nmds) ||
        Objects.nonNull(this.gso) ||
        Objects.nonNull(this.pub) ||
        Objects.nonNull(this.col) ||
        Objects.nonNull(this.irb) ||
        Objects.nonNull(this.gs) ||
        Objects.nonNull(this.mor) ||
        Objects.nonNull(this.morDate) ||
        Objects.nonNull(this.npu) ||
        Objects.nonNull(this.otherSecondary);
  }

  public boolean hasPrimaryDataUse() {
    return Objects.nonNull(this.accessManagement) ||
        Objects.nonNull(this.generalResearchUse) ||
        Objects.nonNull(this.hmb) ||
        (Objects.nonNull(this.diseaseSpecificUse) && !this.diseaseSpecificUse.isEmpty()) ||
        Objects.nonNull(this.poa) ||
        Objects.nonNull(this.otherPrimary);
  }

}
