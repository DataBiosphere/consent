package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Dataset {

    private Integer dataSetId;

    private String objectId;

    private String name;

    // For backwards compatibility with DatasetDTO, this is an alias to the name property.
    private String datasetName;

    private Date createDate;

    private Integer createUserId;

    private Date updateDate;

    private Integer updateUserId;

    private Boolean active;

    private String consentName;

    private Boolean needsApproval;

    private Integer alias;

    private String datasetIdentifier;

    public DataUse dataUse;

    private Integer dacId;

    private String consentId;

    private String translatedUseRestriction;

    private Boolean deletable;

    private FileStorageObject nihInstitutionalCertificationFile;
    private FileStorageObject alternativeDataSharingPlanFile;

    private Set<DatasetProperty> properties;

    private Boolean dacApproval;

    private User createUser;

    public Dataset() {
    }

    public Dataset(Integer dataSetId, String objectId, String name, Date createDate, Integer createUserId, Date updateDate, Integer updateUserId, Boolean active, Integer alias) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.datasetName = name;
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
        this.datasetName = name;
        this.createDate = createDate;
        this.active = active;
        this.alias = alias;
    }

    public Dataset(Integer dataSetId, String objectId, String name, Date createDate, Boolean active) {
        this.dataSetId = dataSetId;
        this.objectId = objectId;
        this.name = name;
        this.datasetName = name;
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

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
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

    public void addProperty(DatasetProperty property) {
        if (Objects.isNull(this.properties)) {
            this.properties = new HashSet<>();
        }
        this.properties.add(property);
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

    public Boolean getDacApproval() {
        return dacApproval;
    }

    public void setDacApproval(Boolean dacApproval) {
        this.dacApproval = dacApproval;
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

    public void setAlias(Integer alias) {
        this.alias = alias;
    }

    public DataUse getDataUse() {
        return dataUse;
    }

    public void setDataUse(DataUse dataUse) {
        this.dataUse = dataUse;
    }

    public void setDatasetIdentifier() {
        this.datasetIdentifier = parseAliasToIdentifier(this.getAlias());
    }

    public String getDatasetIdentifier() {
        if (Objects.isNull(this.getAlias())) {
            return null;
        }

        return parseAliasToIdentifier(this.getAlias());
    }

    public static String parseAliasToIdentifier(Integer alias) {
        return PREFIX + StringUtils.leftPad(alias.toString(), 6, "0");
    }

    public static Integer parseIdentifierToAlias(String identifier) throws IllegalArgumentException {
        try {
            String givenPrefix = identifier.substring(0, PREFIX.length());
            if (!givenPrefix.equals(PREFIX)) {
                throw new IllegalArgumentException("Invalid prefix.");
            }

            String aliasAsString = identifier.substring(PREFIX.length()); // cut off DUOS-
            return Integer.parseInt(aliasAsString); // parse remaining as integer
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not parse identifier ("+identifier+"). Proper format: "+PREFIX+"XXXXXX");
        }
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


    /**
     * Checks if the Dataset matches a raw search query. Searches on all dataset properties
     * and some data use properties.
     *
     * @param query Raw string query
     * @return if the Dataset matched query
     */
    public boolean isStringMatch(@NonNull String query) {
        String lowerCaseQuery = query.toLowerCase();
        List<String> queryTerms = List.of(lowerCaseQuery.split("\\s+"));

        List<String> matchTerms = new ArrayList<>();
        matchTerms.add(this.getName());
        matchTerms.add(this.getDatasetIdentifier());

        if (Objects.nonNull(getProperties()) && !getProperties().isEmpty()) {
            List<String> propVals = getProperties()
                    .stream()
                    .filter((dp) -> Objects.nonNull(dp.getPropertyValue()))
                    .map(DatasetProperty::getPropertyValueAsString)
                    .map(String::toLowerCase)
                    .toList();
            matchTerms.addAll(propVals);
        }

        if (Objects.nonNull(dataUse)) {
            if (Objects.nonNull(dataUse.getEthicsApprovalRequired())
                    && dataUse.getEthicsApprovalRequired()) {
                matchTerms.add("irb");
            }

            if (Objects.nonNull(dataUse.getCollaboratorRequired())
                    && dataUse.getCollaboratorRequired()) {
                matchTerms.add("collaborator");
            }

            if (Objects.nonNull(dataUse.getDiseaseRestrictions())) {
                matchTerms.addAll(dataUse.getDiseaseRestrictions());
            }
        }

        return queryTerms
                .stream()
                .filter(Objects::nonNull)
                // all terms must match at least one thing
                .allMatch((q) ->
                        matchTerms
                                .stream()
                                .filter(Objects::nonNull)
                                .map(String::toLowerCase)
                                .anyMatch(
                                        (t) -> t.contains(q)
                                ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dataset dataset = (Dataset) o;
        return com.google.common.base.Objects.equal(dataSetId, dataset.dataSetId);
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(dataSetId);
    }

    public FileStorageObject getNihInstitutionalCertificationFile() {
        return nihInstitutionalCertificationFile;
    }

    public void setNihInstitutionalCertificationFile(FileStorageObject nihInstitutionalCertificationFile) {
        this.nihInstitutionalCertificationFile = nihInstitutionalCertificationFile;
    }

    public FileStorageObject getAlternativeDataSharingPlanFile() {
        return alternativeDataSharingPlanFile;
    }

    public void setAlternativeDataSharingPlanFile(FileStorageObject alternativeDataSharingPlanFile) {
        this.alternativeDataSharingPlanFile = alternativeDataSharingPlanFile;
    }

    public User getCreateUser() {
        return createUser;
    }

    public void setCreateUser(User createUser) {
        this.createUser = createUser;
    }
}
