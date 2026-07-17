package org.broadinstitute.consent.http.models.dto.registration;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.data;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Centralizes the mapping from registration request DTOs ({@link StudyRegistrationRequest}, {@link
 * ConsentGroupRequest}) to the {@link StudyProperty}/{@link DatasetProperty}/{@link DataUse} values
 * consumed by {@link org.broadinstitute.consent.http.service.dao.DatasetServiceDAO} insert/update
 * commands.
 */
public class RegistrationRequestMapper {

  /**
   * Distinguishes a brand-new consent group (created via registration create, or added to a study
   * during an update) from an existing consent group being edited in place. Existing consent groups
   * have a subset of fields that are not editable post-creation; see {@link
   * DatasetPropertyExtractor#excludeOnExistingConsentGroupUpdate}.
   */
  public enum ConsentGroupContext {
    NEW,
    EXISTING_UPDATE
  }

  public List<StudyProperty> toStudyProperties(StudyRegistrationRequest request) {
    return STUDY_PROPERTY_EXTRACTORS.stream()
        .map(e -> e.extract(request))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  public List<DatasetProperty> toDatasetProperties(
      ConsentGroupRequest group, ConsentGroupContext context) {
    return DATASET_PROPERTY_EXTRACTORS.stream()
        .map(e -> e.extract(group, context))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  public DataUse toDataUse(ConsentGroupRequest group) {
    DataUse dataUse = new DataUse();

    dataUse.setCollaboratorRequired(group.getCol());
    dataUse.setDiseaseRestrictions(group.getDiseaseSpecificUse());
    dataUse.setEthicsApprovalRequired(group.getIrb());
    dataUse.setGeneralUse(group.getGeneralResearchUse());
    dataUse.setGeographicalRestrictions(group.getGs());
    dataUse.setGeneticStudiesOnly(group.getGso());
    dataUse.setHmbResearch(group.getHmb());
    dataUse.setPublicationMoratorium(
        Objects.nonNull(group.getMor()) && Boolean.TRUE.equals(group.getMor())
            ? group.getMorDate()
            : null);
    dataUse.setMethodsResearch(
        Objects.nonNull(group.getNmds()) && Boolean.TRUE.equals(group.getNmds()) ? false : null);
    dataUse.setNonProfitUse(Objects.nonNull(group.getNpu()) ? group.getNpu() : null);
    dataUse.setOther(group.getOtherPrimary());
    dataUse.setSecondaryOther(group.getOtherSecondary());
    dataUse.setPopulationOriginsAncestry(group.getPoa());
    dataUse.setPublicationResults(group.getPub());

    return dataUse;
  }

  /**
   * Extracts an individual field as a study property.
   *
   * @param key The schema property name (camelCase)
   * @param type The type of the field, e.g. Boolean, String
   * @param getField Lambda which gets the field's value
   */
  record StudyPropertyExtractor(
      String key, PropertyType type, Function<StudyRegistrationRequest, Object> getField) {

    Optional<StudyProperty> extract(StudyRegistrationRequest request) {
      Object value = this.getField.apply(request);
      if (Objects.isNull(value)) {
        return Optional.empty();
      }

      StudyProperty studyProperty = new StudyProperty();
      studyProperty.setKey(this.key);
      studyProperty.setType(this.type);
      studyProperty.setValue(this.type.coerce(value.toString()));

      return Optional.of(studyProperty);
    }
  }

  /**
   * Extracts an individual field as a dataset property.
   *
   * @param name The human-readable name of the field
   * @param schemaProp The schema property name (camelCase)
   * @param type The type of the field, e.g. Boolean, String
   * @param excludeOnExistingConsentGroupUpdate When true, this field is not editable on an existing
   *     consent group; it is only ever extracted for new consent groups.
   * @param getField Lambda which gets the field's value
   */
  record DatasetPropertyExtractor(
      String name,
      String schemaProp,
      PropertyType type,
      boolean excludeOnExistingConsentGroupUpdate,
      Function<ConsentGroupRequest, Object> getField) {

    static DatasetPropertyExtractor of(
        String name,
        String schemaProp,
        PropertyType type,
        Function<ConsentGroupRequest, Object> getField) {
      return new DatasetPropertyExtractor(name, schemaProp, type, false, getField);
    }

    Optional<DatasetProperty> extract(ConsentGroupRequest group, ConsentGroupContext context) {
      if (context == ConsentGroupContext.EXISTING_UPDATE && excludeOnExistingConsentGroupUpdate) {
        return Optional.empty();
      }

      Object value = this.getField.apply(group);
      if (Objects.isNull(value)) {
        return Optional.empty();
      }

      DatasetProperty datasetProperty = new DatasetProperty();
      datasetProperty.setPropertyName(this.name);
      datasetProperty.setPropertyType(this.type);
      datasetProperty.setSchemaProperty(this.schemaProp);
      datasetProperty.setPropertyValue(this.type.coerce(value.toString()));

      return Optional.of(datasetProperty);
    }
  }

  private static final List<StudyPropertyExtractor> STUDY_PROPERTY_EXTRACTORS =
      List.of(
          new StudyPropertyExtractor(
              "studyType",
              PropertyType.String,
              request -> {
                if (Objects.nonNull(request.getStudyType())) {
                  return request.getStudyType().value();
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "phenotypeIndication",
              PropertyType.String,
              StudyRegistrationRequest::getPhenotypeIndication),
          new StudyPropertyExtractor(
              "species", PropertyType.String, StudyRegistrationRequest::getSpecies),
          new StudyPropertyExtractor(
              "dataCustodianEmail",
              PropertyType.Json,
              request -> {
                if (Objects.nonNull(request.getDataCustodianEmail())) {
                  return GsonUtil.getInstance().toJson(request.getDataCustodianEmail());
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "throughBioId", PropertyType.String, StudyRegistrationRequest::getThroughBioId),
          new StudyPropertyExtractor(
              "nihAnvilUse", PropertyType.String, StudyRegistrationRequest::getNihAnvilUse),
          new StudyPropertyExtractor(
              "submittingToAnvil",
              PropertyType.Boolean,
              StudyRegistrationRequest::getSubmittingToAnvil),
          new StudyPropertyExtractor(
              "dbGaPPhsID", PropertyType.String, StudyRegistrationRequest::getDbGaPPhsID),
          new StudyPropertyExtractor(
              "dbGaPStudyRegistrationName",
              PropertyType.String,
              StudyRegistrationRequest::getDbGaPStudyRegistrationName),
          new StudyPropertyExtractor(
              "embargoReleaseDate",
              PropertyType.Date,
              StudyRegistrationRequest::getEmbargoReleaseDate),
          new StudyPropertyExtractor(
              "sequencingCenter",
              PropertyType.String,
              StudyRegistrationRequest::getSequencingCenter),
          new StudyPropertyExtractor(
              "piInstitution", PropertyType.Number, StudyRegistrationRequest::getPiInstitution),
          new StudyPropertyExtractor(
              "nihGrantContractNumber",
              PropertyType.String,
              StudyRegistrationRequest::getNihGrantContractNumber),
          new StudyPropertyExtractor(
              "nihICsSupportingStudy",
              PropertyType.Json,
              request -> {
                if (Objects.nonNull(request.getNihICsSupportingStudy())) {
                  return GsonUtil.getInstance()
                      .toJson(
                          request.getNihICsSupportingStudy().stream()
                              .map(NihICsSupportingStudy::value)
                              .toList());
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "nihProgramOfficerName",
              PropertyType.String,
              StudyRegistrationRequest::getNihProgramOfficerName),
          new StudyPropertyExtractor(
              "nihInstitutionCenterSubmission",
              PropertyType.String,
              request -> {
                if (Objects.nonNull(request.getNihInstitutionCenterSubmission())) {
                  return request.getNihInstitutionCenterSubmission().value();
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "nihGenomicProgramAdministratorName",
              PropertyType.String,
              StudyRegistrationRequest::getNihGenomicProgramAdministratorName),
          new StudyPropertyExtractor(
              "multiCenterStudy",
              PropertyType.Boolean,
              StudyRegistrationRequest::getMultiCenterStudy),
          new StudyPropertyExtractor(
              "collaboratingSites",
              PropertyType.Json,
              request -> {
                if (Objects.nonNull(request.getCollaboratingSites())) {
                  return GsonUtil.getInstance().toJson(request.getCollaboratingSites());
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "controlledAccessRequiredForGenomicSummaryResultsGSR",
              PropertyType.Boolean,
              StudyRegistrationRequest::getControlledAccessRequiredForGenomicSummaryResultsGSR),
          new StudyPropertyExtractor(
              "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
              PropertyType.String,
              StudyRegistrationRequest
                  ::getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlan",
              PropertyType.Boolean,
              StudyRegistrationRequest::getAlternativeDataSharingPlan),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanReasons",
              PropertyType.Json,
              request -> {
                if (Objects.nonNull(request.getAlternativeDataSharingPlanReasons())) {
                  return GsonUtil.getInstance()
                      .toJson(
                          request.getAlternativeDataSharingPlanReasons().stream()
                              .map(AlternativeDataSharingPlanReason::value)
                              .toList());
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanExplanation",
              PropertyType.String,
              StudyRegistrationRequest::getAlternativeDataSharingPlanExplanation),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanFileName",
              PropertyType.String,
              StudyRegistrationRequest::getAlternativeDataSharingPlanFileName),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanDataSubmitted",
              PropertyType.String,
              request -> {
                if (Objects.nonNull(request.getAlternativeDataSharingPlanDataSubmitted())) {
                  return request.getAlternativeDataSharingPlanDataSubmitted().value();
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanDataReleased",
              PropertyType.Boolean,
              StudyRegistrationRequest::getAlternativeDataSharingPlanDataReleased),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanTargetDeliveryDate",
              PropertyType.Date,
              StudyRegistrationRequest::getAlternativeDataSharingPlanTargetDeliveryDate),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanTargetPublicReleaseDate",
              PropertyType.Date,
              StudyRegistrationRequest::getAlternativeDataSharingPlanTargetPublicReleaseDate),
          new StudyPropertyExtractor(
              "alternativeDataSharingPlanAccessManagement",
              PropertyType.String,
              request -> {
                if (Objects.nonNull(request.getAlternativeDataSharingPlanAccessManagement())) {
                  return request.getAlternativeDataSharingPlanAccessManagement().value();
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "assets",
              PropertyType.Json,
              request -> {
                if (Objects.nonNull(request.getAssets()) && !request.getAssets().isEmpty()) {
                  return GsonUtil.getInstance().toJson(request.getAssets());
                }
                return null;
              }),
          new StudyPropertyExtractor(
              data,
              PropertyType.Json,
              request -> {
                if (Objects.nonNull(request.getData()) && !request.getData().isEmpty()) {
                  return GsonUtil.getInstance().toJson(request.getData());
                }
                return null;
              }),
          new StudyPropertyExtractor(
              "externalIdentifier",
              PropertyType.String,
              StudyRegistrationRequest::getExternalIdentifier),
          new StudyPropertyExtractor(
              "externalIdentifierType",
              PropertyType.String,
              StudyRegistrationRequest::getExternalIdentifierType));

  private static final List<DatasetPropertyExtractor> DATASET_PROPERTY_EXTRACTORS =
      List.of(
          DatasetPropertyExtractor.of(
              "Data Location",
              "dataLocation",
              PropertyType.String,
              group -> {
                if (Objects.nonNull(group.getDataLocation())) {
                  return group.getDataLocation().value();
                }
                return null;
              }),
          DatasetPropertyExtractor.of(
              "# of participants",
              "numberOfParticipants",
              PropertyType.Number,
              ConsentGroupRequest::getNumberOfParticipants),
          DatasetPropertyExtractor.of(
              "File Types",
              "fileTypes",
              PropertyType.Json,
              group -> {
                if (Objects.nonNull(group.getFileTypes())) {
                  return GsonUtil.getInstance().toJson(group.getFileTypes());
                }
                return null;
              }),
          DatasetPropertyExtractor.of(
              "URL",
              "url",
              PropertyType.String,
              group -> {
                if (Objects.nonNull(group.getUrl())) {
                  return group.getUrl();
                }
                return null;
              }),
          DatasetPropertyExtractor.of(
              "Request Location",
              "requestLocation",
              PropertyType.String,
              group -> {
                if (Objects.nonNull(group.getRequestLocation())) {
                  return group.getRequestLocation();
                }
                return null;
              }),
          // accessManagement is not editable on an existing consent group during a study
          // update; new consent groups (created via registration create, or added during an
          // update) still get it extracted normally.
          new DatasetPropertyExtractor(
              "Access Management",
              "accessManagement",
              PropertyType.String,
              true,
              group -> {
                if (Objects.nonNull(group.getAccessManagement())) {
                  return group.getAccessManagement().value();
                }
                return null;
              }),
          DatasetPropertyExtractor.of(
              data,
              data,
              PropertyType.Json,
              group -> {
                if (Objects.nonNull(group.getData())) {
                  return GsonUtil.getInstance().toJson(group.getData());
                }
                return null;
              }));
}
