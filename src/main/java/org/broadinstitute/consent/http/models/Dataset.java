package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Set;

@JsonInclude(Include.NON_NULL)
public class Dataset {

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private String objectId;

    @JsonProperty
    private String name;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Integer createUserId;

    @JsonProperty
    private Date updateDate;

    @JsonProperty
    private Integer updateUserId;

    @JsonProperty
    private Boolean active;

    @JsonProperty
    private String consentName;

    @JsonProperty
    private Boolean needsApproval;

    @JsonProperty
    private Integer alias;

    @JsonProperty
    public DataUse dataUse;

    @JsonProperty
    private Integer dacId;

    @JsonProperty
    private String consentId;

    @JsonProperty
    private String translatedUseRestriction;

    @JsonProperty
    private Boolean deletable;

    @JsonProperty
    private Boolean isAssociatedToDataOwners;

    @JsonProperty
    private Boolean updateAssociationToDataOwnerAllowed;


    private Set<DatasetProperty> properties;

    public Dataset() {
    }

    public Dataset(Integer dataSetId, String objectId, String name, Date createDate, Integer createUserId, Date updateDate, Integer updateUserId, Boolean active, Integer alias) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.createDate = createDate;
        this.createUserId = createUserId;
        this.updateDate = updateDate;
        this.updateUserId = updateUserId;
        this.active = active;
        this.alias = alias;
    }

    public Dataset(Integer dataSetId, String objectId, String name, Date createDate, Boolean active, Integer alias) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.createDate = createDate;
        this.active = active;
        this.alias = alias;
    }

    public Dataset(Integer dataSetId, String objectId, String name, Date createDate, Boolean active) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.createDate = createDate;
        this.active = active;
    }

    private static String PREFIX = "DUOS-";

    public Dataset(String objectId) {
        this.objectId = objectId;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Integer getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Integer createUserId) {
        this.createUserId = createUserId;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getUpdateUserId() {
        return updateUserId;
    }

    public void setUpdateUserId(Integer updateUserId) {
        this.updateUserId = updateUserId;
    }

    public Set<DatasetProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<DatasetProperty> properties) {
        this.properties = properties;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getNeedsApproval() {
        return needsApproval;
    }

    public void setNeedsApproval(Boolean needsApproval) {
        this.needsApproval = needsApproval;
    }

    public String getConsentName() {
        return consentName;
    }

    public void setConsentName(String consentName) {
        this.consentName = consentName;
    }

    public Integer getAlias() {
        return alias;
    }

    public DataUse getDataUse() {
        return dataUse;
    }

    public void setDataUse(DataUse dataUse) {
        this.dataUse = dataUse;
    }

    public void setAlias(Integer alias) {
        this.alias = alias;
    }

    public String getDatasetIdentifier() {
        return parseAliasToIdentifier(this.getAlias());
    }

    public static String parseAliasToIdentifier(Integer alias) {
        return PREFIX + StringUtils.leftPad(alias.toString(), 6, "0");
    }

    public Integer getDacId() {
        return dacId;
    }

    public void setDacId(Integer dacId) {
        this.dacId = dacId;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getTranslatedUseRestriction() {
        return translatedUseRestriction;
    }

    public void setTranslatedUseRestriction(String translatedUseRestriction) {
        this.translatedUseRestriction = translatedUseRestriction;
    }

    public Boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Boolean getAssociatedToDataOwners() {
        return isAssociatedToDataOwners;
    }

    public void setAssociatedToDataOwners(Boolean associatedToDataOwners) {
        isAssociatedToDataOwners = associatedToDataOwners;
    }

    public Boolean getUpdateAssociationToDataOwnerAllowed() {
        return updateAssociationToDataOwnerAllowed;
    }

    public void setUpdateAssociationToDataOwnerAllowed(Boolean updateAssociationToDataOwnerAllowed) {
        this.updateAssociationToDataOwnerAllowed = updateAssociationToDataOwnerAllowed;
    }
}
