package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.data;

import com.google.cloud.storage.BlobId;
import com.google.common.annotations.VisibleForTesting;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.FileStorageObjectDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.message.DatasetSubmittedMessage;
import org.broadinstitute.consent.http.mail.message.NewStudyRegistrationConfirmationMessage;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetPatch;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetUpdate;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.AlternativeDataSharingPlanReason;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.NihICsSupportingStudy;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyUpdate;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.ThreadUtils;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

/**
 * Specialized service class which specifically handles the process of registering a new dataset
 * into the system, i.e. creates new datasets.
 */
public class DatasetRegistrationService implements ConsentLogger {

  private final DatasetDAO datasetDAO;
  private final DacDAO dacDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final FileStorageObjectDAO fileStorageObjectDAO;
  private final GCSService gcsService;
  private final ElasticSearchService elasticSearchService;
  private final StudyDAO studyDAO;
  private final EmailService emailService;
  private final ExecutorService executorService =
      new ThreadUtils().getExecutorService(DatasetRegistrationService.class);

  public DatasetRegistrationService(
      DatasetDAO datasetDAO,
      DacDAO dacDAO,
      DatasetServiceDAO datasetServiceDAO,
      FileStorageObjectDAO fileStorageObjectDAO,
      GCSService gcsService,
      ElasticSearchService elasticSearchService,
      StudyDAO studyDAO,
      EmailService emailService) {
    this.datasetDAO = datasetDAO;
    this.dacDAO = dacDAO;
    this.datasetServiceDAO = datasetServiceDAO;
    this.fileStorageObjectDAO = fileStorageObjectDAO;
    this.gcsService = gcsService;
    this.elasticSearchService = elasticSearchService;
    this.studyDAO = studyDAO;
    this.emailService = emailService;
  }

  public Dataset patchDataset(Integer datasetId, User user, DatasetPatch patch) {
    try {
      datasetServiceDAO.patchDataset(datasetId, user, patch);
      return datasetDAO.findDatasetById(datasetId);
    } catch (SQLException ex) {
      logException(ex);
      throw new InternalServerErrorException("An error occurred while patching the dataset.");
    }
  }

  public Study findStudyById(Integer studyId) {
    Study study = studyDAO.findStudyById(studyId);
    if (Objects.isNull(study)) {
      throw new NotFoundException("Study with ID " + studyId + " is not found");
    }
    return study;
  }

  /**
   * This method takes an instance of a dataset registration schema and updates the study and
   * associated datasets from it.
   *
   * @param registration The DatasetRegistrationSchemaV1.yaml
   * @param user The User updating the study
   * @param files Map of files, where the key is the name of the field
   * @return The updated Study
   */
  public Study updateStudyFromRegistration(
      Integer studyId,
      DatasetRegistrationSchemaV1 registration,
      User user,
      Map<String, FormDataBodyPart> files)
      throws Exception {
    Map<String, BlobId> uploadedFileCache = new HashMap<>();
    List<FileStorageObject> uploadFiles = uploadFilesForStudy(files, uploadedFileCache, user);
    List<DatasetServiceDAO.DatasetUpdate> datasetUpdates = new ArrayList<>();
    List<DatasetServiceDAO.DatasetInsert> datasetInserts = new ArrayList<>();
    // Dataset updates and inserts:
    IntStream.range(0, registration.getConsentGroups().size())
        .forEach(
            idx -> {
              ConsentGroup cg = registration.getConsentGroups().get(idx);
              if (Objects.nonNull(cg.getDatasetId())) {
                Dataset existingDataset =
                    datasetDAO.findDatasetsByIdList(List.of(cg.getDatasetId())).getFirst();
                try {
                  DatasetUpdate datasetUpdate =
                      new DatasetUpdate(
                          cg.getConsentGroupName(),
                          existingDataset.getDacId(),
                          convertConsentGroupToDatasetProperties(cg));
                  DatasetServiceDAO.DatasetUpdate update =
                      createDatasetUpdate(
                          cg.getDatasetId(), user, datasetUpdate, files, uploadedFileCache, idx);
                  datasetUpdates.add(update);
                } catch (Exception e) {
                  logException(e);
                }
              } else {
                try {
                  DatasetServiceDAO.DatasetInsert insert =
                      createDatasetInsert(registration, user, files, uploadedFileCache, idx);
                  datasetInserts.add(insert);
                } catch (Exception e) {
                  logException(e);
                }
              }
            });

    List<StudyProperty> studyProps = convertRegistrationToStudyProperties(registration);
    DatasetServiceDAO.StudyUpdate studyUpdate =
        new StudyUpdate(
            registration.getStudyName(),
            studyId,
            registration.getStudyDescription(),
            registration.getDataTypes(),
            registration.getPiName(),
            registration.getPublicVisibility(),
            user.getUserId(),
            studyProps,
            uploadFiles);

    Study updatedStudy = datasetServiceDAO.updateStudy(studyUpdate, datasetUpdates, datasetInserts);
    sendDatasetSubmittedEmails(createdDatasetsFromUpdatedStudy(updatedStudy, datasetUpdates));
    return updatedStudy;
  }

  /**
   * This method takes an instance of a dataset registration schema and creates datasets from it.
   * There will be one dataset per ConsentGroup in the dataset.
   *
   * @param registration The DatasetRegistrationSchemaV1.yaml
   * @param user The User creating these datasets
   * @param files Map of files, where the key is the name of the field
   * @return List of created Datasets from the provided registration schema
   */
  public List<Dataset> createDatasetsFromRegistration(
      DatasetRegistrationSchemaV1 registration, User user, Map<String, FormDataBodyPart> files)
      throws SQLException, IllegalArgumentException, IOException {

    registration.setDataSubmitterUserId(user.getUserId());

    Map<String, BlobId> uploadedFileCache = new HashMap<>();

    List<DatasetServiceDAO.DatasetInsert> datasetInserts = new ArrayList<>();
    DatasetServiceDAO.StudyInsert studyInsert;

    try {
      studyInsert = createStudyInsert(registration, user, files, uploadedFileCache);

      for (int consentGroupIdx = 0;
          consentGroupIdx < registration.getConsentGroups().size();
          consentGroupIdx++) {
        datasetInserts.add(
            createDatasetInsert(registration, user, files, uploadedFileCache, consentGroupIdx));
      }
    } catch (IOException e) {
      // uploading files to GCS failed. rollback files...
      uploadedFileCache.values().forEach((id) -> gcsService.deleteDocument(id.getName()));
      throw e;
    }

    List<Integer> createdDatasetIds =
        datasetServiceDAO.insertDatasetRegistration(studyInsert, datasetInserts);

    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(createdDatasetIds);
    // Send notification emails to DAC chairs about new datasets
    sendDatasetSubmittedEmails(datasets);
    // Send confirmation email to submitter
    sendSubmissionConfirmationEmail(user, registration, datasets.getFirst().getStudyId());
    try (Response response = elasticSearchService.indexDatasets(createdDatasetIds)) {
      if (response.getStatus() >= 400) {
        logWarn(
            String.format(
                "Error indexing datasets from registration: %s", registration.getStudyName()));
      }
    } catch (Exception e) {
      logException(e);
    }
    return datasets;
  }

  private BlobId uploadFile(FormDataBodyPart file) throws IOException {
    String mediaType = file.getContentDisposition().getType();

    return gcsService.storeDocument(
        file.getValueAs(InputStream.class), mediaType, UUID.randomUUID());
  }

  private DatasetServiceDAO.StudyInsert createStudyInsert(
      DatasetRegistrationSchemaV1 registration,
      User user,
      Map<String, FormDataBodyPart> files,
      Map<String, BlobId> uploadedFileCache)
      throws IOException {
    return new DatasetServiceDAO.StudyInsert(
        registration.getStudyName(),
        registration.getStudyDescription(),
        registration.getDataTypes(),
        registration.getPiName(),
        registration.getPublicVisibility(),
        user.getUserId(),
        convertRegistrationToStudyProperties(registration),
        uploadFilesForStudy(files, uploadedFileCache, user));
  }

  /**
   * This method takes an instance of a dataset registration schema and updates the dataset.
   *
   * @param user The User creating these datasets
   * @param files Map of files, where the key is the name of the field
   * @return List of created Datasets from the provided registration schema
   */
  public Dataset updateDataset(
      Integer datasetId, User user, DatasetUpdate update, Map<String, FormDataBodyPart> files)
      throws IOException, SQLException {

    if (Objects.isNull(update.getName())) {
      throw new BadRequestException("Dataset name is required");
    }

    if (Objects.isNull(update.getDacId())) {
      throw new BadRequestException("DAC Id is required");
    }

    Dataset dataset = datasetDAO.findDatasetById(datasetId);

    if (!Objects.equals(dataset.getDacId(), update.getDacId())) {
      throw new BadRequestException("DAC Id update is not yet supported");
    }

    Map<String, BlobId> uploadedFileCache = new HashMap<>();

    try {
      DatasetServiceDAO.DatasetUpdate datasetUpdates =
          createDatasetUpdate(datasetId, user, update, files, uploadedFileCache, 0);

      // Update or create the objects in the database
      datasetServiceDAO.updateDataset(datasetUpdates);

    } catch (IOException e) {
      // uploading files to GCS failed. rollback files...
      uploadedFileCache.values().forEach((id) -> gcsService.deleteDocument(id.getName()));
      throw e;
    }

    Dataset updatedDataset = datasetDAO.findDatasetById(datasetId);
    elasticSearchService.synchronizeDatasetInESIndex(updatedDataset, false);
    return updatedDataset;
  }

  /*
  Upload all relevant files to GCS and create relevant
   */
  private DatasetServiceDAO.DatasetInsert createDatasetInsert(
      DatasetRegistrationSchemaV1 registration,
      User user,
      Map<String, FormDataBodyPart> files,
      Map<String, BlobId> uploadedFileCache,
      Integer consentGroupIdx)
      throws IOException {
    ConsentGroup consentGroup = registration.getConsentGroups().get(consentGroupIdx);

    if (Objects.isNull(consentGroup.getConsentGroupName())
        || consentGroup.getConsentGroupName().isBlank()) {
      throw new BadRequestException("Consent Group Name is required");
    }

    if (Objects.nonNull(consentGroup.getDataAccessCommitteeId())
        && Objects.isNull(dacDAO.findById(consentGroup.getDataAccessCommitteeId()))) {
      throw new NotFoundException("Could not find DAC");
    }

    List<DatasetProperty> props = convertConsentGroupToDatasetProperties(consentGroup);
    DataUse dataUse = generateDataUseFromConsentGroup(consentGroup);
    List<FileStorageObject> fileStorageObjects =
        uploadFilesForDataset(files, uploadedFileCache, consentGroupIdx, user);

    return new DatasetServiceDAO.DatasetInsert(
        consentGroup.getConsentGroupName(),
        consentGroup.getDataAccessCommitteeId(),
        dataUse,
        user.getUserId(),
        props,
        fileStorageObjects);
  }

  private DatasetServiceDAO.DatasetUpdate createDatasetUpdate(
      Integer datasetId,
      User user,
      DatasetUpdate datasetUpdate,
      Map<String, FormDataBodyPart> files,
      Map<String, BlobId> uploadedFileCache,
      int idx)
      throws IOException {

    List<DatasetProperty> props = datasetUpdate.getDatasetProperties();

    List<FileStorageObject> fileStorageObjects =
        uploadFilesForDatasetUpdate(files, uploadedFileCache, user, idx);

    return new DatasetServiceDAO.DatasetUpdate(
        datasetId,
        datasetUpdate.getName(),
        user.getUserId(),
        datasetUpdate.getDacId(),
        props,
        fileStorageObjects);
  }

  @VisibleForTesting
  public DataUse generateDataUseFromConsentGroup(ConsentGroup group) {
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

  private static final String ALTERNATIVE_DATA_SHARING_PLAN_NAME = "alternativeDataSharingPlan";
  // nosemgrep
  private static final String NIH_INSTITUTIONAL_CERTIFICATION_NAME =
      "consentGroups[%s].nihInstitutionalCertificationFile";

  /**
   * Uploads the files related to the Dataset Registration's dataset object to Google Cloud and
   * returns references to them as FileStorageObjects.
   *
   * @param files The files the user provided: fileType (e.g., alternativeDataSharingPlan) ->
   *     FormDataBodyPart
   * @param uploadedFileCache Previously uploaded files - ensures that the same file is not
   *     reuploaded if used on different datasets.
   * @param consentGroupIdx The index of the consent group that this dataset is associated to
   * @param user The create user
   * @return The list of FSOs created for this study
   * @throws IOException if GCS upload fails
   */
  private List<FileStorageObject> uploadFilesForDataset(
      Map<String, FormDataBodyPart> files,
      Map<String, BlobId> uploadedFileCache,
      Integer consentGroupIdx,
      User user)
      throws IOException {
    List<FileStorageObject> consentGroupFSOs = new ArrayList<>();

    if (files.containsKey(String.format(NIH_INSTITUTIONAL_CERTIFICATION_NAME, consentGroupIdx))) {
      FileStorageObject fso =
          uploadFile(
              files,
              uploadedFileCache,
              user,
              String.format(NIH_INSTITUTIONAL_CERTIFICATION_NAME, consentGroupIdx),
              FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
      if (!gcsService.hasBytes(fso.getBlobId())) {
        gcsService.deleteDocument(fso.getBlobId().getName());
      }
      consentGroupFSOs.add(fso);
    }

    return consentGroupFSOs;
  }

  private List<FileStorageObject> uploadFilesForDatasetUpdate(
      Map<String, FormDataBodyPart> files,
      Map<String, BlobId> uploadedFileCache,
      User user,
      int idx)
      throws IOException {
    List<FileStorageObject> updateDatasetFSOs = new ArrayList<>();
    String fileKey = String.format(NIH_INSTITUTIONAL_CERTIFICATION_NAME, idx);
    FormDataBodyPart fileToUpdate = files.get(fileKey);
    if (fileToUpdate != null) {
      FileStorageObject fso =
          uploadFile(
              files,
              uploadedFileCache,
              user,
              fileKey,
              FileCategory.NIH_INSTITUTIONAL_CERTIFICATION);
      if (!gcsService.hasBytes(fso.getBlobId())) {
        gcsService.deleteDocument(fso.getBlobId().getName());
      }
      updateDatasetFSOs.add(fso);
    }

    return updateDatasetFSOs;
  }

  /**
   * Uploads the files related to the Dataset Registration's study object to Google Cloud and
   * returns references to them as FileStorageObjects.
   *
   * @param files The files the user provided: fileType (e.g., alternativeDataSharingPlan) ->
   *     FormDataBodyPart
   * @param uploadedFileCache Previously uploaded files - ensures that the same file is not
   *     reuploaded if used on different datasets.
   * @param user The create user
   * @return The list of FSOs created for this study
   * @throws IOException if GCS upload fails
   */
  private List<FileStorageObject> uploadFilesForStudy(
      Map<String, FormDataBodyPart> files, Map<String, BlobId> uploadedFileCache, User user)
      throws IOException {
    List<FileStorageObject> studyFSOs = new ArrayList<>();
    FormDataBodyPart alternateSharingPlanFile = files.get(ALTERNATIVE_DATA_SHARING_PLAN_NAME);
    if (alternateSharingPlanFile != null) {
      FileStorageObject fso =
          uploadFile(
              files,
              uploadedFileCache,
              user,
              ALTERNATIVE_DATA_SHARING_PLAN_NAME,
              FileCategory.ALTERNATIVE_DATA_SHARING_PLAN);
      if (!gcsService.hasBytes(fso.getBlobId())) {
        gcsService.deleteDocument(fso.getBlobId().getName());
      } else {
        studyFSOs.add(fso);
      }
    }

    return studyFSOs;
  }

  private FileStorageObject uploadFile(
      Map<String, FormDataBodyPart> files,
      Map<String, BlobId> uploadedFileCache,
      User user,
      String name,
      FileCategory category)
      throws IOException {

    FormDataBodyPart bodyPart = files.get(name);

    if (!uploadedFileCache.containsKey(name)) {
      BlobId id = uploadFile(bodyPart);
      uploadedFileCache.put(name, id);
    }

    BlobId id = uploadedFileCache.get(name);

    FileStorageObject fso = new FileStorageObject();
    fso.setCategory(category);
    fso.setFileName(bodyPart.getContentDisposition().getFileName());
    fso.setMediaType(bodyPart.getMediaType().toString());
    fso.setBlobId(id);
    fso.setCreateUserId(user.getUserId());

    return fso;
  }

  // TODO: refactor these DatasetPropertyExtractors into something cleaner - they work, but they
  // feel a bit clunky.
  //       perhaps a separate class which is more generic would work better.

  /**
   * Extracts an individual field as a dataset property.
   *
   * @param name The human-readable name of the field
   * @param schemaProp The schema property name (camelCase)
   * @param type The type of the field, e.g. Boolean, String
   * @param getField Lambda which gets the field's value
   */
  public record DatasetPropertyExtractor(
      String name,
      String schemaProp,
      PropertyType type,
      /*
       * Takes in: Dataset registration object and consent group
       * Produces: The value of the field, can be null if field not present.
       */
      Function<ConsentGroup, Object> getField) {

    /**
     * Converts a field on the given registration to a DatasetProperty.
     *
     * @param consentGroup The index of the consent group to extract from
     * @return The dataset property, if the field has a value, otherwise Optional.empty()
     */
    Optional<DatasetProperty> extract(ConsentGroup consentGroup) {
      Object value = this.getField.apply(consentGroup);
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

  public record StudyPropertyExtractor(
      String key,
      PropertyType type,
      /*
       * Takes in: Dataset registration object
       * Produces: The value of the field, can be null if field not present.
       */
      Function<DatasetRegistrationSchemaV1, Object> getField) {

    /**
     * Converts a field on the given registration to a StudyProperty.
     *
     * @param registration The registration object to extract from = * @return The study property,
     *     if the field has a value, otherwise Optional.empty()
     */
    Optional<StudyProperty> extract(DatasetRegistrationSchemaV1 registration) {
      Object value = this.getField.apply(registration);
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

  private static final List<StudyPropertyExtractor>
      DATASET_REGISTRATION_V1_STUDY_PROPERTY_EXTRACTORS =
          List.of(
              new StudyPropertyExtractor(
                  "studyType",
                  PropertyType.String,
                  registration -> {
                    if (Objects.nonNull(registration.getStudyType())) {
                      return registration.getStudyType().value();
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "phenotypeIndication",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getPhenotypeIndication),
              new StudyPropertyExtractor(
                  "species", PropertyType.String, DatasetRegistrationSchemaV1::getSpecies),
              new StudyPropertyExtractor(
                  "dataCustodianEmail",
                  PropertyType.Json,
                  registration -> {
                    if (Objects.nonNull(registration.getDataCustodianEmail())) {
                      return GsonUtil.getInstance().toJson(registration.getDataCustodianEmail());
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "throughBioId",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getThroughBioId),
              new StudyPropertyExtractor(
                  "nihAnvilUse", PropertyType.String, DatasetRegistrationSchemaV1::getNihAnvilUse),
              new StudyPropertyExtractor(
                  "submittingToAnvil",
                  PropertyType.Boolean,
                  DatasetRegistrationSchemaV1::getSubmittingToAnvil),
              new StudyPropertyExtractor(
                  "dbGaPPhsID", PropertyType.String, DatasetRegistrationSchemaV1::getDbGaPPhsID),
              new StudyPropertyExtractor(
                  "dbGaPStudyRegistrationName",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getDbGaPStudyRegistrationName),
              new StudyPropertyExtractor(
                  "embargoReleaseDate",
                  PropertyType.Date,
                  DatasetRegistrationSchemaV1::getEmbargoReleaseDate),
              new StudyPropertyExtractor(
                  "sequencingCenter",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getSequencingCenter),
              new StudyPropertyExtractor(
                  "piInstitution",
                  PropertyType.Number,
                  DatasetRegistrationSchemaV1::getPiInstitution),
              new StudyPropertyExtractor(
                  "nihGrantContractNumber",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getNihGrantContractNumber),
              new StudyPropertyExtractor(
                  "nihICsSupportingStudy",
                  PropertyType.Json,
                  registration -> {
                    if (Objects.nonNull(registration.getNihICsSupportingStudy())) {
                      return GsonUtil.getInstance()
                          .toJson(
                              registration.getNihICsSupportingStudy().stream()
                                  .map(NihICsSupportingStudy::value)
                                  .toList());
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "nihProgramOfficerName",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getNihProgramOfficerName),
              new StudyPropertyExtractor(
                  "nihInstitutionCenterSubmission",
                  PropertyType.String,
                  registration -> {
                    if (Objects.nonNull(registration.getNihInstitutionCenterSubmission())) {
                      return registration.getNihInstitutionCenterSubmission().value();
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "nihGenomicProgramAdministratorName",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getNihGenomicProgramAdministratorName),
              new StudyPropertyExtractor(
                  "multiCenterStudy",
                  PropertyType.Boolean,
                  DatasetRegistrationSchemaV1::getMultiCenterStudy),
              new StudyPropertyExtractor(
                  "collaboratingSites",
                  PropertyType.Json,
                  registration -> {
                    if (Objects.nonNull(registration.getCollaboratingSites())) {
                      return GsonUtil.getInstance().toJson(registration.getCollaboratingSites());
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "controlledAccessRequiredForGenomicSummaryResultsGSR",
                  PropertyType.Boolean,
                  DatasetRegistrationSchemaV1
                      ::getControlledAccessRequiredForGenomicSummaryResultsGSR),
              new StudyPropertyExtractor(
                  "controlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1
                      ::getControlledAccessRequiredForGenomicSummaryResultsGSRRequiredExplanation),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlan",
                  PropertyType.Boolean,
                  DatasetRegistrationSchemaV1::getAlternativeDataSharingPlan),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanReasons",
                  PropertyType.Json,
                  registration -> {
                    if (Objects.nonNull(registration.getAlternativeDataSharingPlanReasons())) {
                      return GsonUtil.getInstance()
                          .toJson(
                              registration.getAlternativeDataSharingPlanReasons().stream()
                                  .map(AlternativeDataSharingPlanReason::value)
                                  .toList());
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanExplanation",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getAlternativeDataSharingPlanExplanation),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanFileName",
                  PropertyType.String,
                  DatasetRegistrationSchemaV1::getAlternativeDataSharingPlanFileName),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanDataSubmitted",
                  PropertyType.String,
                  registration -> {
                    if (Objects.nonNull(
                        registration.getAlternativeDataSharingPlanDataSubmitted())) {
                      return registration.getAlternativeDataSharingPlanDataSubmitted().value();
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanDataReleased",
                  PropertyType.Boolean,
                  DatasetRegistrationSchemaV1::getAlternativeDataSharingPlanDataReleased),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanTargetDeliveryDate",
                  PropertyType.Date,
                  DatasetRegistrationSchemaV1::getAlternativeDataSharingPlanTargetDeliveryDate),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanTargetPublicReleaseDate",
                  PropertyType.Date,
                  DatasetRegistrationSchemaV1
                      ::getAlternativeDataSharingPlanTargetPublicReleaseDate),
              new StudyPropertyExtractor(
                  "alternativeDataSharingPlanAccessManagement",
                  PropertyType.String,
                  registration -> {
                    if (Objects.nonNull(
                        registration.getAlternativeDataSharingPlanAccessManagement())) {
                      return registration.getAlternativeDataSharingPlanAccessManagement().value();
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  "assets",
                  PropertyType.Json,
                  registration -> {
                    if (Objects.nonNull(registration.getAssets())
                        && !registration.getAssets().isEmpty()) {
                      return GsonUtil.getInstance().toJson(registration.getAssets());
                    }
                    return null;
                  }),
              new StudyPropertyExtractor(
                  data,
                  PropertyType.Json,
                  registration -> {
                    if (Objects.nonNull(registration.getData())
                        && !registration.getData().isEmpty()) {
                      return GsonUtil.getInstance().toJson(registration.getData());
                    }
                    return null;
                  }));

  private static final List<DatasetPropertyExtractor>
      DATASET_REGISTRATION_V1_DATASET_PROPERTY_EXTRACTORS =
          List.of(
              new DatasetPropertyExtractor(
                  "Data Location",
                  "dataLocation",
                  PropertyType.String,
                  consentGroup -> {
                    if (Objects.nonNull(consentGroup.getDataLocation())) {
                      return consentGroup.getDataLocation().value();
                    }
                    return null;
                  }),
              new DatasetPropertyExtractor(
                  "# of participants",
                  "numberOfParticipants",
                  PropertyType.Number,
                  ConsentGroup::getNumberOfParticipants),
              new DatasetPropertyExtractor(
                  "File Types",
                  "fileTypes",
                  PropertyType.Json,
                  consentGroup -> {
                    if (Objects.nonNull(consentGroup.getFileTypes())) {
                      return GsonUtil.getInstance().toJson(consentGroup.getFileTypes());
                    }
                    return null;
                  }),
              new DatasetPropertyExtractor(
                  "URL",
                  "url",
                  PropertyType.String,
                  consentGroup -> {
                    if (Objects.nonNull(consentGroup.getUrl())) {
                      return consentGroup.getUrl();
                    }
                    return null;
                  }),
              new DatasetPropertyExtractor(
                  "Access Management",
                  "accessManagement",
                  PropertyType.String,
                  consentGroup -> {
                    if (Objects.nonNull(consentGroup.getAccessManagement())) {
                      return consentGroup.getAccessManagement().value();
                    }
                    return null;
                  }),
              new DatasetPropertyExtractor(
                  data,
                  data,
                  PropertyType.Json,
                  consentGroup -> {
                    if (Objects.nonNull(consentGroup.getData())) {
                      return GsonUtil.getInstance().toJson(consentGroup.getData());
                    }
                    return null;
                  }));

  private List<StudyProperty> convertRegistrationToStudyProperties(
      DatasetRegistrationSchemaV1 registration) {

    return DATASET_REGISTRATION_V1_STUDY_PROPERTY_EXTRACTORS.stream()
        .map(e -> e.extract(registration))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private List<DatasetProperty> convertConsentGroupToDatasetProperties(ConsentGroup consentGroup) {

    return DATASET_REGISTRATION_V1_DATASET_PROPERTY_EXTRACTORS.stream()
        .map((e) -> e.extract(consentGroup))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  /**
   * Extracts the datasets that were created from the given study update by subtracting the updated
   * datasets from the list of datasets in the study.
   *
   * @param updatedStudy The study that was updated
   * @param datasetUpdates The list of datasets that were updated in the study
   * @return The list of datasets that were created from updated study
   */
  public List<Dataset> createdDatasetsFromUpdatedStudy(
      Study updatedStudy, List<DatasetServiceDAO.DatasetUpdate> datasetUpdates) {
    List<Integer> datasetUpdateIds =
        (datasetUpdates == null)
            ? List.of()
            : datasetUpdates.stream().map(DatasetServiceDAO.DatasetUpdate::datasetId).toList();
    if (updatedStudy.getDatasets() == null) {
      return List.of();
    }
    return updatedStudy.getDatasets().stream()
        .filter(dataset -> !datasetUpdateIds.contains(dataset.getDatasetId()))
        .toList();
  }

  /**
   * Sends emails to DAC chairs when a dataset is created.
   *
   * @param datasets The datasets that were created
   */
  public void sendDatasetSubmittedEmails(List<Dataset> datasets) {
    try {
      for (Dataset dataset : datasets) {
        Dac dac = dacDAO.findById(dataset.getDacId());
        if (dac == null) {
          logWarn(
              "Could not find DAC for dataset with identifier: " + dataset.getDatasetIdentifier());
        }
        List<User> chairPersons =
            (dac == null)
                ? List.of()
                : dacDAO.findMembersByDacId(dac.getDacId()).stream()
                    .filter(user -> user.hasUserRole(UserRoles.CHAIRPERSON))
                    .toList();
        if (chairPersons.isEmpty()) {
          logWarn("No chairpersons found for Dataset " + dataset.getDatasetIdentifier());
        } else {
          for (User dacChair : chairPersons) {
            sendDatasetSubmittedMessage(
                dacChair, dataset.getCreateUser(), dac.getName(), dataset.getName());
          }
        }
      }
    } catch (Exception e) {
      logException(e);
    }
  }

  /**
   * Combines the assets field on the registration object with the datasets (consent groups) into a
   * single map to be sent in the submission confirmation email. This allows the email template to
   * access all the assets that the user submitted in the registration form, including both the
   * assets field on the registration object and the datasets as 'consentGroups' on the registration
   * object.
   */
  public Map<String, Object> getAssetsWithDatasets(DatasetRegistrationSchemaV1 registration) {
    Map<String, Object> assetsMap = new HashMap<>();
    if (registration.getAssets() != null) {
      assetsMap.putAll(registration.getAssets());
    }
    assetsMap.put("datasets", registration.getConsentGroups());
    return assetsMap;
  }

  @VisibleForTesting
  public void sendDatasetSubmittedMessage(
      User dacChair, User dataSubmitter, String dacName, String datasetName)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new DatasetSubmittedMessage(dacChair, dataSubmitter.getDisplayName(), datasetName, dacName),
        dacChair.getUserId());
  }

  @VisibleForTesting
  public void sendStudySubmissionConfirmation(
      User dataSubmitter, String studyName, Integer studyId, Map<String, Object> studyAssets)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new NewStudyRegistrationConfirmationMessage(dataSubmitter, studyName, studyId, studyAssets),
        dataSubmitter.getUserId());
  }

  /**
   * Sends a confirmation email to the submitter of a dataset registration request with details of
   * their submission.
   *
   * @param submitter The user who submitted the dataset registration request
   * @param registration The dataset registration object containing the details of the submission
   */
  public void sendSubmissionConfirmationEmail(
      User submitter, DatasetRegistrationSchemaV1 registration, Integer studyId) {
    try {
      sendStudySubmissionConfirmation(
          submitter, registration.getStudyName(), studyId, getAssetsWithDatasets(registration));
    } catch (Exception e) {
      logException(e);
    }
  }

  public void asyncCleanupDatasetsAndStudiesWithEmptyFiles(User user) {
    executorService.submit(() -> cleanupDatasetsAndStudiesWithEmptyFiles(user));
  }

  protected void cleanupDatasetsAndStudiesWithEmptyFiles(User user) {
    List<Integer> allDatasetIDs = datasetDAO.findAllDatasetIds();
    logWarn(
        String.format(
            "Cleaning empty NIH Institutional files requested.  %d datasets to be considered",
            allDatasetIDs.size()));
    AtomicInteger datasetDeleted = new AtomicInteger();
    AtomicInteger datasetErrors = new AtomicInteger();
    AtomicInteger datasetProgress = new AtomicInteger();
    AtomicInteger studyProgress = new AtomicInteger();
    AtomicInteger studyDeleted = new AtomicInteger();
    AtomicInteger studyError = new AtomicInteger();
    allDatasetIDs.forEach(
        id -> {
          processDataset(
              id, user, datasetDeleted, datasetErrors, studyProgress, studyDeleted, studyError);
          datasetProgress.getAndIncrement();
          if (datasetProgress.get() % 100 == 0) {
            logWarn(String.format("Cleaned %d entries.", datasetProgress.get()));
          }
        });
    logWarn(
        String.format(
            "Cleaning empty NIH Institutional and Alternative Sharing Plan files complete.  %d datasets, %d dataset files deleted, %d  dataset unexpected errors.  Study files deleted: %d, Study file errors: %d",
            allDatasetIDs.size(),
            datasetDeleted.get(),
            datasetErrors.get(),
            studyDeleted.get(),
            studyError.get()));
  }

  private void processStudy(
      Study study,
      User user,
      AtomicInteger studyProgress,
      AtomicInteger studyDeleted,
      AtomicInteger studyError) {
    FileStorageObject studySharingPlan = study.getAlternativeDataSharingPlan();
    try {
      if (studySharingPlan != null && !gcsService.hasBytes(studySharingPlan.getBlobId())) {
        deleteFile(studySharingPlan, user);
        studyDeleted.getAndIncrement();
      }
    } catch (NotFoundException _) {
      deleteFile(studySharingPlan, user);
      studyDeleted.getAndIncrement();
    } catch (Exception e) {
      logWarn(
          String.format(
              "Error when cleaning up alternative sharing plan for study: %d.  %s",
              study.getStudyId(), e.getMessage()));
      studyError.getAndIncrement();
    }
    studyProgress.getAndIncrement();
  }

  private void processDataset(
      Integer id,
      User user,
      AtomicInteger deleted,
      AtomicInteger errors,
      AtomicInteger studyProgress,
      AtomicInteger studyDeleted,
      AtomicInteger studyError) {
    Dataset dataset = datasetDAO.findDatasetById(id);
    if (dataset != null) {
      FileStorageObject nihFile = dataset.getNihInstitutionalCertificationFile();
      try {
        if (nihFile != null && !gcsService.hasBytes(nihFile.getBlobId())) {
          deleteFile(nihFile, user);
          deleted.getAndIncrement();
        }
      } catch (NotFoundException _) {
        deleteFile(nihFile, user);
        deleted.getAndIncrement();
      } catch (Exception e) {
        logWarn(
            String.format("Error checking file for dataset id %d.  Error: %s", id, e.getMessage()));
        errors.getAndIncrement();
      } finally {
        Study study = dataset.getStudy();
        if (study != null) {
          Study fullStudyObject = studyDAO.findStudyById(study.getStudyId());
          processStudy(fullStudyObject, user, studyProgress, studyDeleted, studyError);
        }
      }
    }
  }

  protected void deleteFile(FileStorageObject fileStorageObject, User user) {
    if (fileStorageObject == null || user == null) {
      return;
    }
    try {
      gcsService.deleteDocument(fileStorageObject.getBlobId().getName());
    } catch (Exception e) {
      logWarn(
          "Error deleting file from GCS.  This can happen when attempting to delete a file in a storage bucket that has already been deleted.");
    } finally {
      // set FSO record to be deleted
      // this will cause the dataset to not join on the file storage object.
      fileStorageObjectDAO.deleteFileById(
          fileStorageObject.getFileStorageObjectId(), user.getUserId());
    }
  }
}
