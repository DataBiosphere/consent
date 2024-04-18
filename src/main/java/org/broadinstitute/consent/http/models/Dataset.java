package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Dataset {

  public static final String BASE_QUERY = """
            SELECT d.dataset_id, d.name, d.create_date, d.create_user_id, d.update_date,
                d.update_user_id, d.object_id, d.active, d.dac_id, d.alias, d.data_use, d.dac_approval,
                dar_ds_ids.id AS in_use,
                u.user_id AS u_user_id, u.email AS u_email, u.display_name AS u_display_name,
                u.create_date AS u_create_date, u.email_preference AS u_email_preference,
                u.institution_id AS u_institution_id, u.era_commons_id AS u_era_commons_id,
                k.key, dp.property_value, dp.property_key, dp.property_type, dp.schema_property, dp.property_id,
                s.study_id AS s_study_id,
                s.name AS s_name,
                s.description AS s_description,
                s.data_types AS s_data_types,
                s.pi_name AS s_pi_name,
                s.create_user_id AS s_create_user_id,
                s.create_date AS s_create_date,
                s.update_user_id AS s_user_id,
                s.update_date AS s_update_date,
                s.public_visibility AS s_public_visibility,
                s_dataset.dataset_id AS s_dataset_id,
                sp.study_property_id AS sp_study_property_id,
                sp.study_id AS sp_study_id,
                sp.key AS sp_key,
                sp.value AS sp_value,
                sp.type AS sp_type,
                fso.file_storage_object_id AS fso_file_storage_object_id,
                fso.entity_id AS fso_entity_id,
                fso.file_name AS fso_file_name,
                fso.category AS fso_category,
                fso.gcs_file_uri AS fso_gcs_file_uri,
                fso.media_type AS fso_media_type,
                fso.create_date AS fso_create_date,
                fso.create_user_id AS fso_create_user_id,
                fso.update_date AS fso_update_date,
                fso.update_user_id AS fso_update_user_id,
                fso.deleted AS fso_deleted,
                fso.delete_user_id AS fso_delete_user_id
            FROM dataset d
            LEFT JOIN users u on d.create_user_id = u.user_id
            LEFT JOIN (SELECT DISTINCT dataset_id AS id FROM dar_dataset) dar_ds_ids ON dar_ds_ids.id = d.dataset_id
            LEFT JOIN dataset_property dp ON dp.dataset_id = d.dataset_id
            LEFT JOIN dictionary k ON k.key_id = dp.property_key
            LEFT JOIN study s ON s.study_id = d.study_id
            LEFT JOIN study_property sp ON sp.study_id = s.study_id
            LEFT JOIN dataset s_dataset ON s_dataset.study_id = s.study_id
            LEFT JOIN file_storage_object fso ON (fso.entity_id = d.dataset_id::text OR fso.entity_id = s.uuid::text) AND fso.deleted = false
      """;

  private Integer dataSetId;

  private String objectId;

  private String name;

  // For backwards compatibility with DatasetDTO, this is an alias to the name property.
  private String datasetName;

  private Date createDate;

  private Integer createUserId;

  private Date updateDate;

  private Integer updateUserId;

  /**
   * Active is a deprecated property. The visibility of a dataset is calculated from DAC approval
   * and the public visibility dataset property
   */
  @Deprecated(forRemoval = true)
  private Boolean active;

  /**
   * Needs Approval is a deprecated property. The visibility of a dataset is calculated from DAC
   * approval and the public visibility dataset property
   */
  @Deprecated(forRemoval = true)
  private Boolean needsApproval;

  private Integer alias;

  private String datasetIdentifier;

  public DataUse dataUse;

  private String translatedDataUse;
  private Integer dacId;

  private Boolean deletable;

  private FileStorageObject nihInstitutionalCertificationFile;

  private Set<DatasetProperty> properties;

  List<String> propertyName;
  private Boolean dacApproval;

  private User createUser;
  private Study study;

  public Dataset() {
  }

  public Dataset(Integer dataSetId, String objectId, String name, Date createDate,
      Integer createUserId, Date updateDate, Integer updateUserId, Integer alias) {
    this.dataSetId = dataSetId;
    this.objectId = objectId;
    this.name = name;
    this.datasetName = name;
    this.createDate = createDate;
    this.createUserId = createUserId;
    this.updateDate = updateDate;
    this.updateUserId = updateUserId;
    this.alias = alias;
  }

  public Dataset(Integer dataSetId, String objectId, String name, Date createDate, Integer alias) {
    this.dataSetId = dataSetId;
    this.objectId = objectId;
    this.name = name;
    this.datasetName = name;
    this.createDate = createDate;
    this.alias = alias;
  }

  public Dataset(Integer dataSetId, String objectId, String name, Date createDate) {
    this.dataSetId = dataSetId;
    this.objectId = objectId;
    this.name = name;
    this.datasetName = name;
    this.createDate = createDate;
  }

  private static final String PREFIX = "DUOS-";

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

  public List<String> getPropertyName() {
    return propertyName;
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
          "Could not parse identifier (" + identifier + "). Proper format: " + PREFIX + "XXXXXX");
    }
  }

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
  }

  public String getTranslatedDataUse() {
    return translatedDataUse;
  }

  public void setTranslatedDataUse(String translatedDataUse) {
    this.translatedDataUse = translatedDataUse;
  }

  public Boolean getDeletable() {
    return deletable;
  }

  public void setDeletable(Boolean deletable) {
    this.deletable = deletable;
  }

  /**
   * Checks if the Dataset matches a raw search query. Searches on all dataset properties and some
   * data use properties. Has optional parameter accessManagement which will search datasets on both
   * the raw search query and the access management type.
   *
   * @param query            Raw string query
   * @param accessManagement One of controlled, open, or external
   * @return if the Dataset matched query
   */

  // TODO: investigate whether we can try to coerce getPropertyValue to a boolean instead of comparing strings
  public boolean isDatasetMatch(@NonNull String query, AccessManagement accessManagement) {
    String lowerCaseQuery = query.toLowerCase();
    List<String> queryTerms = List.of(lowerCaseQuery.split("\\s+"));

    List<String> matchTerms = new ArrayList<>();
    matchTerms.add(this.getName());
    matchTerms.add(this.getDatasetIdentifier());

    if (Objects.nonNull(getProperties()) && !getProperties().isEmpty()) {
      Optional<DatasetProperty> accessManagementProp = getProperties()
          .stream()
          .filter((dp) -> Objects.nonNull(dp.getPropertyValue()))
          .filter((dp) -> Objects.equals(dp.getPropertyName(), "Access Management"))
          .findFirst();

      if (accessManagementProp.isEmpty()) {
        if (accessManagement.equals(AccessManagement.OPEN)) {
          return false;
        }
      } else if (!accessManagement.toString()
          .equals(accessManagementProp.get().getPropertyValueAsString())) {
        return false;
      }

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
                    (t) -> t.contains(q))
        );
  }

  public Study getStudy() {
    return study;
  }

  public void setStudy(Study study) {
    this.study = study;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
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

  public void setNihInstitutionalCertificationFile(
      FileStorageObject nihInstitutionalCertificationFile) {
    this.nihInstitutionalCertificationFile = nihInstitutionalCertificationFile;
  }

  public User getCreateUser() {
    return createUser;
  }

  public void setCreateUser(User createUser) {
    this.createUser = createUser;
  }

}
