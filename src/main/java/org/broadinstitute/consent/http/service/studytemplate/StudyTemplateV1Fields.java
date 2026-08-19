package org.broadinstitute.consent.http.service.studytemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;

/**
 * The v1 template field catalogue: every supported CSV {@code field}, the wire property it maps to,
 * and how its cells convert. Wire properties the contract keeps out of v1 are listed separately so
 * a producer who names one gets told it belongs on the draft form rather than that it does not
 * exist.
 */
final class StudyTemplateV1Fields {

  static final String ACCESS_MANAGEMENT = "accessManagement";
  static final String FILE_TYPE_FIELD = "fileType";

  /**
   * Values that satisfy a field's own registration rule; see {@link TemplateField#probeValue()}.
   */
  private static final String PROBE_TEXT = "probe";

  private static final String PROBE_EMAIL = "probe@example.org";
  private static final String PROBE_DATE = "2000-01-01";
  private static final Integer PROBE_ID = 1;
  private static final DatasetRegistrationSchemaV1.NihAnvilUse PROBE_ANVIL_USE =
      DatasetRegistrationSchemaV1.NihAnvilUse
          .I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL;

  private static final CellConverter STUDY_TYPE =
      CellConverter.enumOf(DatasetRegistrationSchemaV1.StudyType::fromValue);
  private static final CellConverter NIH_ANVIL_USE =
      CellConverter.enumOf(DatasetRegistrationSchemaV1.NihAnvilUse::fromValue);
  private static final CellConverter NIH_IC =
      CellConverter.enumOf(NihICsSupportingStudy::fromValue);
  private static final CellConverter NIH_IC_SUBMISSION =
      CellConverter.enumOf(DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission::fromValue);
  private static final CellConverter ACCESS_MANAGEMENT_VALUE =
      CellConverter.enumOf(ConsentGroup.AccessManagement::fromValue);
  private static final CellConverter DATA_LOCATION =
      CellConverter.enumOf(ConsentGroup.DataLocation::fromValue);
  private static final CellConverter FILE_TYPE_VALUE =
      CellConverter.enumOf(FileTypeObject.FileType::fromValue);

  static final Map<String, TemplateField<StudyRegistrationRequest>> STUDY = studyFields();
  static final Map<String, TemplateField<ConsentGroupRequest>> CONSENT_GROUP = consentGroupFields();
  static final Map<String, TemplateField<FileTypeObject>> FILE_TYPES = fileTypeFields();

  /**
   * Wire properties excluded from v1: the file-backed alternative sharing plan group, non-file
   * assets that would need a JSON encoding, and integration-owned identifiers.
   */
  private static final Set<String> UNSUPPORTED_STUDY_FIELDS =
      Set.of(
          "alternativeDataSharingPlan",
          "alternativeDataSharingPlanReasons",
          "alternativeDataSharingPlanExplanation",
          "alternativeDataSharingPlanFileName",
          "alternativeDataSharingPlanFile",
          "alternativeDataSharingPlanDataSubmitted",
          "alternativeDataSharingPlanDataReleased",
          "alternativeDataSharingPlanTargetDeliveryDate",
          "alternativeDataSharingPlanTargetPublicReleaseDate",
          "alternativeDataSharingPlanAccessManagement",
          "nihInstitutionalCertificationFile",
          "assets",
          "data",
          "externalIdentifier",
          "externalIdentifierType");

  private static final Set<String> UNSUPPORTED_CONSENT_GROUP_FIELDS = Set.of("datasetId", "data");

  private StudyTemplateV1Fields() {}

  static boolean isExcludedFromV1(String recordType, String field) {
    return switch (recordType) {
      case StudyTemplateV1Parser.STUDY -> UNSUPPORTED_STUDY_FIELDS.contains(field);
      case StudyTemplateV1Parser.CONSENT_GROUP -> UNSUPPORTED_CONSENT_GROUP_FIELDS.contains(field);
      default -> false;
    };
  }

  private static Map<String, TemplateField<StudyRegistrationRequest>> studyFields() {
    Map<String, TemplateField<StudyRegistrationRequest>> fields = new LinkedHashMap<>();
    addStudyDescriptiveFields(fields);
    addStudyContactFields(fields);
    addStudyNihFields(fields);
    addStudyDisclosureFields(fields);
    return Map.copyOf(fields);
  }

  private static void addStudyDescriptiveFields(
      Map<String, TemplateField<StudyRegistrationRequest>> fields) {
    add(
        fields,
        "studyName",
        CellConverter.TEXT,
        StudyRegistrationRequest::getStudyName,
        (r, v) -> r.setStudyName((String) v),
        PROBE_TEXT);
    add(
        fields,
        "studyType",
        STUDY_TYPE,
        StudyRegistrationRequest::getStudyType,
        (r, v) -> r.setStudyType((DatasetRegistrationSchemaV1.StudyType) v),
        null);
    add(
        fields,
        "studyDescription",
        CellConverter.TEXT,
        StudyRegistrationRequest::getStudyDescription,
        (r, v) -> r.setStudyDescription((String) v),
        PROBE_TEXT);
    addMulti(
        fields,
        "dataTypes",
        CellConverter.TEXT,
        StudyRegistrationRequest::getDataTypes,
        (r, v) -> r.setDataTypes(items(v, String.class)),
        PROBE_TEXT);
    add(
        fields,
        "phenotypeIndication",
        CellConverter.TEXT,
        StudyRegistrationRequest::getPhenotypeIndication,
        (r, v) -> r.setPhenotypeIndication((String) v),
        null);
    add(
        fields,
        "species",
        CellConverter.TEXT,
        StudyRegistrationRequest::getSpecies,
        (r, v) -> r.setSpecies((String) v),
        null);
    add(
        fields,
        "embargoReleaseDate",
        CellConverter.TEXT,
        StudyRegistrationRequest::getEmbargoReleaseDate,
        (r, v) -> r.setEmbargoReleaseDate((String) v),
        PROBE_DATE);
    add(
        fields,
        "sequencingCenter",
        CellConverter.TEXT,
        StudyRegistrationRequest::getSequencingCenter,
        (r, v) -> r.setSequencingCenter((String) v),
        null);
  }

  private static void addStudyContactFields(
      Map<String, TemplateField<StudyRegistrationRequest>> fields) {
    add(
        fields,
        "piName",
        CellConverter.TEXT,
        StudyRegistrationRequest::getPiName,
        (r, v) -> r.setPiName((String) v),
        PROBE_TEXT);
    add(
        fields,
        "piEmail",
        CellConverter.TEXT,
        StudyRegistrationRequest::getPiEmail,
        (r, v) -> r.setPiEmail((String) v),
        PROBE_EMAIL);
    addMulti(
        fields,
        "dataCustodianEmail",
        CellConverter.TEXT,
        StudyRegistrationRequest::getDataCustodianEmail,
        (r, v) -> r.setDataCustodianEmail(items(v, String.class)),
        PROBE_EMAIL);
    add(
        fields,
        "piInstitution",
        CellConverter.INTEGER,
        StudyRegistrationRequest::getPiInstitution,
        (r, v) -> r.setPiInstitution((Integer) v),
        PROBE_ID);
    add(
        fields,
        "throughBioId",
        CellConverter.TEXT,
        StudyRegistrationRequest::getThroughBioId,
        (r, v) -> r.setThroughBioId((String) v),
        null);
  }

  private static void addStudyNihFields(
      Map<String, TemplateField<StudyRegistrationRequest>> fields) {
    add(
        fields,
        "nihAnvilUse",
        NIH_ANVIL_USE,
        StudyRegistrationRequest::getNihAnvilUse,
        (r, v) -> r.setNihAnvilUse((DatasetRegistrationSchemaV1.NihAnvilUse) v),
        PROBE_ANVIL_USE);
    add(
        fields,
        "submittingToAnvil",
        CellConverter.BOOLEAN,
        StudyRegistrationRequest::getSubmittingToAnvil,
        (r, v) -> r.setSubmittingToAnvil((Boolean) v),
        null);
    add(
        fields,
        "dbGaPPhsID",
        CellConverter.TEXT,
        StudyRegistrationRequest::getDbGaPPhsID,
        (r, v) -> r.setDbGaPPhsID((String) v),
        PROBE_TEXT);
    add(
        fields,
        "dbGaPStudyRegistrationName",
        CellConverter.TEXT,
        StudyRegistrationRequest::getDbGaPStudyRegistrationName,
        (r, v) -> r.setDbGaPStudyRegistrationName((String) v),
        null);
    add(
        fields,
        "nihGrantContractNumber",
        CellConverter.TEXT,
        StudyRegistrationRequest::getNihGrantContractNumber,
        (r, v) -> r.setNihGrantContractNumber((String) v),
        PROBE_TEXT);
    addMulti(
        fields,
        "nihICsSupportingStudy",
        NIH_IC,
        StudyRegistrationRequest::getNihICsSupportingStudy,
        (r, v) -> r.setNihICsSupportingStudy(items(v, NihICsSupportingStudy.class)),
        null);
    add(
        fields,
        "nihProgramOfficerName",
        CellConverter.TEXT,
        StudyRegistrationRequest::getNihProgramOfficerName,
        (r, v) -> r.setNihProgramOfficerName((String) v),
        null);
    add(
        fields,
        "nihInstitutionCenterSubmission",
        NIH_IC_SUBMISSION,
        StudyRegistrationRequest::getNihInstitutionCenterSubmission,
        (r, v) ->
            r.setNihInstitutionCenterSubmission(
                (DatasetRegistrationSchemaV1.NihInstitutionCenterSubmission) v),
        null);
    add(
        fields,
        "nihGenomicProgramAdministratorName",
        CellConverter.TEXT,
        StudyRegistrationRequest::getNihGenomicProgramAdministratorName,
        (r, v) -> r.setNihGenomicProgramAdministratorName((String) v),
        null);
  }

  private static void addStudyDisclosureFields(
      Map<String, TemplateField<StudyRegistrationRequest>> fields) {
    add(
        fields,
        "publicVisibility",
        CellConverter.BOOLEAN,
        StudyRegistrationRequest::getPublicVisibility,
        (r, v) -> r.setPublicVisibility((Boolean) v),
        Boolean.TRUE);
    add(
        fields,
        "multiCenterStudy",
        CellConverter.BOOLEAN,
        StudyRegistrationRequest::getMultiCenterStudy,
        (r, v) -> r.setMultiCenterStudy((Boolean) v),
        null);
    addMulti(
        fields,
        "collaboratingSites",
        CellConverter.TEXT,
        StudyRegistrationRequest::getCollaboratingSites,
        (r, v) -> r.setCollaboratingSites(items(v, String.class)),
        null);
    add(
        fields,
        "controlledAccessRequiredForGenomicSummaryResultsGSR",
        CellConverter.BOOLEAN,
        StudyRegistrationRequest::getControlledAccessRequiredForGenomicSummaryResultsGSR,
        (r, v) -> r.setControlledAccessRequiredForGenomicSummaryResultsGSR((Boolean) v),
        null);
    add(
        fields,
        "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
        CellConverter.TEXT,
        StudyRegistrationRequest
            ::getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation,
        (r, v) ->
            r.setControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation((String) v),
        PROBE_TEXT);
  }

  private static Map<String, TemplateField<ConsentGroupRequest>> consentGroupFields() {
    Map<String, TemplateField<ConsentGroupRequest>> fields = new LinkedHashMap<>();
    addPrimaryDataUseFields(fields);
    addSecondaryDataUseFields(fields);
    addConsentGroupAccessFields(fields);
    return Map.copyOf(fields);
  }

  private static void addPrimaryDataUseFields(
      Map<String, TemplateField<ConsentGroupRequest>> fields) {
    add(
        fields,
        "generalResearchUse",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getGeneralResearchUse,
        (c, v) -> c.setGeneralResearchUse((Boolean) v),
        null);
    add(
        fields,
        "hmb",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getHmb,
        (c, v) -> c.setHmb((Boolean) v),
        null);
    addMulti(
        fields,
        "diseaseSpecificUse",
        CellConverter.TEXT,
        ConsentGroupRequest::getDiseaseSpecificUse,
        (c, v) -> c.setDiseaseSpecificUse(items(v, String.class)),
        null);
    add(
        fields,
        "poa",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getPoa,
        (c, v) -> c.setPoa((Boolean) v),
        null);
    add(
        fields,
        "otherPrimary",
        CellConverter.TEXT,
        ConsentGroupRequest::getOtherPrimary,
        (c, v) -> c.setOtherPrimary((String) v),
        null);
  }

  private static void addSecondaryDataUseFields(
      Map<String, TemplateField<ConsentGroupRequest>> fields) {
    add(
        fields,
        "nmds",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getNmds,
        (c, v) -> c.setNmds((Boolean) v),
        null);
    add(
        fields,
        "gso",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getGso,
        (c, v) -> c.setGso((Boolean) v),
        null);
    add(
        fields,
        "pub",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getPub,
        (c, v) -> c.setPub((Boolean) v),
        null);
    add(
        fields,
        "col",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getCol,
        (c, v) -> c.setCol((Boolean) v),
        null);
    add(
        fields,
        "irb",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getIrb,
        (c, v) -> c.setIrb((Boolean) v),
        null);
    add(
        fields,
        "gs",
        CellConverter.TEXT,
        ConsentGroupRequest::getGs,
        (c, v) -> c.setGs((String) v),
        null);
    add(
        fields,
        "mor",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getMor,
        (c, v) -> c.setMor((Boolean) v),
        null);
    add(
        fields,
        "morDate",
        CellConverter.TEXT,
        ConsentGroupRequest::getMorDate,
        (c, v) -> c.setMorDate((String) v),
        PROBE_DATE);
    add(
        fields,
        "npu",
        CellConverter.BOOLEAN,
        ConsentGroupRequest::getNpu,
        (c, v) -> c.setNpu((Boolean) v),
        null);
    add(
        fields,
        "otherSecondary",
        CellConverter.TEXT,
        ConsentGroupRequest::getOtherSecondary,
        (c, v) -> c.setOtherSecondary((String) v),
        null);
  }

  private static void addConsentGroupAccessFields(
      Map<String, TemplateField<ConsentGroupRequest>> fields) {
    add(
        fields,
        "consentGroupName",
        CellConverter.TEXT,
        ConsentGroupRequest::getConsentGroupName,
        (c, v) -> c.setConsentGroupName((String) v),
        PROBE_TEXT);
    add(
        fields,
        ACCESS_MANAGEMENT,
        ACCESS_MANAGEMENT_VALUE,
        ConsentGroupRequest::getAccessManagement,
        (c, v) -> c.setAccessManagement((ConsentGroup.AccessManagement) v),
        null);
    add(
        fields,
        "dataAccessCommitteeId",
        CellConverter.INTEGER,
        ConsentGroupRequest::getDataAccessCommitteeId,
        (c, v) -> c.setDataAccessCommitteeId((Integer) v),
        PROBE_ID);
    add(
        fields,
        "dataLocation",
        DATA_LOCATION,
        ConsentGroupRequest::getDataLocation,
        (c, v) -> c.setDataLocation((ConsentGroup.DataLocation) v),
        null);
    add(
        fields,
        "url",
        CellConverter.HTTP_URI,
        ConsentGroupRequest::getUrl,
        (c, v) -> c.setUrl((URI) v),
        null);
    add(
        fields,
        "requestLocation",
        CellConverter.HTTP_URI,
        ConsentGroupRequest::getRequestLocation,
        (c, v) -> c.setRequestLocation((URI) v),
        null);
    add(
        fields,
        "numberOfParticipants",
        CellConverter.INTEGER,
        ConsentGroupRequest::getNumberOfParticipants,
        (c, v) -> c.setNumberOfParticipants((Integer) v),
        PROBE_ID);
  }

  private static Map<String, TemplateField<FileTypeObject>> fileTypeFields() {
    Map<String, TemplateField<FileTypeObject>> fields = new LinkedHashMap<>();
    add(
        fields,
        FILE_TYPE_FIELD,
        FILE_TYPE_VALUE,
        FileTypeObject::getFileType,
        (f, v) -> f.setFileType((FileTypeObject.FileType) v),
        null);
    add(
        fields,
        "functionalEquivalence",
        CellConverter.TEXT,
        FileTypeObject::getFunctionalEquivalence,
        (f, v) -> f.setFunctionalEquivalence((String) v),
        null);
    return Map.copyOf(fields);
  }

  private static <T> void add(
      Map<String, TemplateField<T>> fields,
      String name,
      CellConverter converter,
      Function<T, Object> reader,
      BiConsumer<T, Object> writer,
      Object probeValue) {
    fields.put(name, new TemplateField<>(name, false, converter, reader, writer, probeValue));
  }

  private static <T> void addMulti(
      Map<String, TemplateField<T>> fields,
      String name,
      CellConverter converter,
      Function<T, Object> reader,
      BiConsumer<T, Object> writer,
      Object probeValue) {
    fields.put(name, new TemplateField<>(name, true, converter, reader, writer, probeValue));
  }

  /** Null restores a field a probe found absent, so every writer must accept it. */
  @Nullable
  private static <E> List<E> items(Object value, Class<E> type) {
    return value == null ? null : ((List<?>) value).stream().map(type::cast).toList();
  }
}
