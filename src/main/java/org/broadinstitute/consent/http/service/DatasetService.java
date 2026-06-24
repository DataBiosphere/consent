package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataCustodianEmail;

import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetAuthorizationReaderDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.message.DatasetApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetDeniedMessage;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAuthorizationReader;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyConversion;
import org.broadinstitute.consent.http.models.StudyPatch;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;

public class DatasetService implements ConsentLogger {

  private final DatasetAuthorizationReaderDAO datasetAuthorizationReaderDAO;
  private final DatasetDAO datasetDAO;
  private final DaaDAO daaDAO;
  private final DacDAO dacDAO;
  private final ElasticSearchService elasticSearchService;
  private final EmailService emailService;
  private final OntologyService ontologyService;
  private final StudyDAO studyDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final UserDAO userDAO;

  @Inject
  public DatasetService(
      Jdbi jdbi,
      DatasetServiceDAO datasetServiceDAO,
      ElasticSearchService elasticSearchService,
      EmailService emailService,
      OntologyService ontologyService) {
    this.datasetAuthorizationReaderDAO = jdbi.onDemand(DatasetAuthorizationReaderDAO.class);
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.daaDAO = jdbi.onDemand(DaaDAO.class);
    this.dacDAO = jdbi.onDemand(DacDAO.class);
    this.elasticSearchService = elasticSearchService;
    this.emailService = emailService;
    this.ontologyService = ontologyService;
    this.studyDAO = jdbi.onDemand(StudyDAO.class);
    this.datasetServiceDAO = datasetServiceDAO;
    this.userDAO = jdbi.onDemand(UserDAO.class);
  }

  public List<Dataset> findDatasetListByDacIds(List<Integer> dacIds) {
    if (CollectionUtils.isEmpty(dacIds)) {
      throw new BadRequestException("No dataset IDs provided");
    }
    return datasetDAO.findDatasetListByDacIds(dacIds);
  }

  /**
   * TODO: Refactor this to throw a NotFoundException instead of returning null Finds a Dataset by a
   * formatted dataset identifier.
   *
   * @param datasetIdentifier The formatted identifier, e.g. DUOS-123456
   * @return the Dataset with the given identifier, if found.
   * @throws IllegalArgumentException if datasetIdentifier is invalid
   */
  public Dataset findDatasetByIdentifier(User user, String datasetIdentifier)
      throws IllegalArgumentException {
    Integer alias = Dataset.parseIdentifierToAlias(datasetIdentifier);
    Dataset d = datasetDAO.findDatasetByAlias(alias);
    if (d == null) {
      return null;
    }

    // technically, it is possible to have two dataset identifiers which
    // have the same alias but are not the same: e.g., DUOS-5 and DUOS-00005
    if (!Objects.equals(d.getDatasetIdentifier(), datasetIdentifier)) {
      return null;
    }
    return verifyPublicVisibilityAccess(d, user);
  }

  /**
   * Finds a minimal version of a Dataset by a formatted dataset identifier.
   *
   * @param datasetIdentifier The formatted identifier, e.g. DUOS-123456
   * @param populateStudy Whether to populate the study object in the returned dataset
   * @return the Dataset with the given identifier, if found.
   * @throws IllegalArgumentException if datasetIdentifier is invalid
   */
  public Dataset findMinimalDatasetByIdentifier(
      User user, String datasetIdentifier, boolean populateStudy) throws IllegalArgumentException {
    Integer alias = Dataset.parseIdentifierToAlias(datasetIdentifier);
    Dataset d = datasetDAO.findMinimalDatasetByAlias(alias);
    if (d == null) {
      throw new NotFoundException("Dataset not found");
    }
    // technically, it is possible to have two dataset identifiers which
    // have the same alias but are not the same: e.g., DUOS-5 and DUOS-00005
    if (!Objects.equals(d.getDatasetIdentifier(), datasetIdentifier)) {
      return null;
    }
    // It is faster to populate the study separately than in a single query with the dataset
    if (d.getStudyId() != null && populateStudy) {
      d.setStudy(studyDAO.findStudyById(d.getStudyId()));
    }
    return verifyPublicVisibilityAccess(d, user);
  }

  protected List<DatasetStudySummary> verifyPublicVisibilityAccess(
      List<DatasetStudySummary> summaries, User user) {
    if (user.hasUserRole(UserRoles.ADMIN)) {
      return summaries;
    }
    List<DatasetStudySummary> authorizedSummaries = new ArrayList<>();
    for (DatasetStudySummary summary : summaries) {
      if (Boolean.TRUE.equals(summary.public_visibility())) {
        authorizedSummaries.add(summary);
      } else if (user.getUserId().equals(summary.study_create_user_id())) {
        authorizedSummaries.add(summary);
      } else if (summary.dataset_create_user_id().equals(user.getUserId())) {
        authorizedSummaries.add(summary);
      } else if (summary.study_id() != null) {
        // fetch study and see if the user is a custodian
        Study study = studyDAO.findStudyById(summary.study_id());
        if (study != null && isCreatorOrCustodian(user, study)) {
          authorizedSummaries.add(summary);
        }
      } else {
        authorizedSummaries.add(summary);
      }
    }
    return authorizedSummaries;
  }

  protected Dataset verifyPublicVisibilityAccess(Dataset dataset, User user) {
    // Admins
    if (user.hasUserRole(UserRoles.ADMIN)) {
      return dataset;
    }
    // If there is no study, we can't verify visibility, so return the dataset
    if (dataset.getStudyId() == null) {
      return dataset;
    }
    // Study isn't always populated, so fetch it if necessary
    if (dataset.getStudy() == null) {
      dataset.setStudy(studyDAO.findStudyById(dataset.getStudyId()));
    }
    if (canReadStudy(user, dataset.getStudy())
        || Objects.equals(dataset.getCreateUserId(), user.getUserId())) {
      return dataset;
    }
    return null;
  }

  protected boolean canReadStudy(User user, Study study) {
    if (study == null) {
      return false;
    }
    if (user.hasUserRole(UserRoles.ADMIN)) {
      return true;
    }
    if (!Boolean.FALSE.equals(study.getPublicVisibility())) {
      return true;
    }
    return isCreatorOrCustodian(user, study);
  }

  protected boolean isCreatorOrCustodian(User user, Dataset dataset) {
    if (dataset.getCreateUserId().equals(user.getUserId())) {
      return true;
    }
    return isCreatorOrCustodian(user, dataset.getStudy());
  }

  public boolean isCreatorOrCustodian(User user, Study study) {
    // User's cannot be a creator or custodian if the study is null
    if (study == null) {
      return false;
    }
    if (study.getCreateUserId().equals(user.getUserId())) {
      return true;
    }
    Optional<StudyProperty> custodianProp =
        study.getProperties().stream()
            .filter(p -> p.getKey().equals(dataCustodianEmail))
            .findFirst();
    if (custodianProp.isPresent()) {
      Gson gson = GsonUtil.getInstance();
      // prop is a JsonArray of Strings
      List<String> custodians =
          gson.fromJson(custodianProp.get().getValue().toString(), new TypeToken<>() {}.getType());
      for (String custodian : custodians) {
        if (user.getEmail().equals(custodian.trim())) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isCreatorCustodianOrAdmin(User user, Study study) {
    return user.hasUserRole(UserRoles.ADMIN) || isCreatorOrCustodian(user, study);
  }

  public Dataset getDatasetByName(String name) {
    String lowercaseName = name.toLowerCase();
    return datasetDAO.getDatasetByName(lowercaseName);
  }

  public Set<String> findAllStudyNames() {
    return datasetDAO.findAllStudyNames();
  }

  public List<String> findAllDatasetNames() {
    return datasetDAO.findAllDatasetNames();
  }

  public Dataset findDatasetById(User user, Integer id) {
    Dataset dataset = datasetDAO.findDatasetById(id);
    return verifyPublicVisibilityAccess(dataset, user);
  }

  /**
   * Finds a dataset by ID and enforces read access using a single dataset query.
   *
   * @throws NotFoundException if the dataset does not exist
   * @throws ForbiddenException if the user cannot view the dataset
   */
  public Dataset findDatasetByIdForRead(User user, Integer id) {
    Dataset dataset = datasetDAO.findDatasetById(id);
    if (dataset == null) {
      throw new NotFoundException("Entity not found");
    }
    Dataset authorizedDataset = verifyPublicVisibilityAccess(dataset, user);
    if (authorizedDataset == null) {
      throw new ForbiddenException("User does not have permission");
    }
    return authorizedDataset;
  }

  /**
   * Find the dataset without files by its ID. This method is intended to return a minimal dataset
   * for performance reasons, avoiding the retrieval of full FSO information.
   *
   * @param id Dataset ID
   * @return The updated Dataset object
   */
  public Dataset findDatasetWithoutFSOInformation(Integer id) {
    return datasetDAO.findDatasetWithoutFSOInformation(id);
  }

  public Dataset updateDatasetDataUse(User user, Integer datasetId, DataUse dataUse) {
    Dataset d = datasetDAO.findDatasetById(datasetId);
    if (d == null) {
      throw new NotFoundException("Dataset not found: " + datasetId);
    }
    if (!user.hasUserRole(UserRoles.ADMIN)) {
      throw new IllegalArgumentException("Admin use only");
    }
    String translation = ontologyService.translateDataUse(dataUse, DataUseTranslationType.DATASET);
    datasetServiceDAO.updateDatasetDataUse(user, d, dataUse, translation);
    elasticSearchService.synchronizeDatasetInESIndex(d, false);
    return datasetDAO.findDatasetById(datasetId);
  }

//  public Dataset syncDatasetDataUseTranslation(Integer datasetId, User user) {
//    Dataset dataset = datasetDAO.findDatasetById(datasetId);
//    if (dataset == null) {
//      throw new NotFoundException("Dataset not found");
//    }
//
//    String translation =
//        ontologyService.translateDataUse(dataset.getDataUse(), DataUseTranslationType.DATASET);
//    datasetServiceDAO.updateDatasetDataUse(user, dataset, dataset.getDataUse(), translation);
//    elasticSearchService.synchronizeDatasetInESIndex(dataset, false);
//    return datasetDAO.findDatasetById(datasetId);
//  }

  public void deleteDataset(Integer datasetId, Integer userId) throws Exception {
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (dataset != null) {
      try (var response = elasticSearchService.deleteIndex(datasetId, userId)) {
        if (!HttpStatusCodes.isSuccess(response.getStatus())) {
          logWarn("Response error, unable to delete dataset from index: %s".formatted(datasetId));
        }
      }
      datasetServiceDAO.deleteDataset(dataset, userId);
    }
  }

  public void deleteStudy(Study study, User user) throws Exception {
    study
        .getDatasetIds()
        .forEach(
            datasetId -> {
              try (var response = elasticSearchService.deleteIndex(datasetId, user.getUserId())) {
                if (!HttpStatusCodes.isSuccess(response.getStatus())) {
                  logWarn(
                      "Response error, unable to delete dataset from index: %s"
                          .formatted(datasetId));
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    datasetServiceDAO.deleteStudy(study, user);
  }

  public Study findStudy(Integer studyId) {
    return studyDAO.findStudyById(studyId);
  }

  public Study findStudyByIdForRead(User user, Integer studyId) {
    Study study = studyDAO.findStudyById(studyId);
    if (study == null) {
      throw new NotFoundException("Entity not found");
    }
    if (!canReadStudy(user, study)) {
      throw new ForbiddenException("User does not have permission");
    }
    return study;
  }

  public List<DatasetStudySummary> findAllDatasetStudySummaries(User user) {
    List<DatasetStudySummary> summaries = datasetDAO.findAllDatasetStudySummaries();
    return verifyPublicVisibilityAccess(summaries, user);
  }

  public Dataset approveDataset(Dataset dataset, User user, Boolean approval) {
    Boolean currentApprovalState = dataset.getDacApproval();
    Integer datasetId = dataset.getDatasetId();
    Dataset datasetReturn = dataset;
    // Only update and fetch the dataset if it hasn't already been approved
    // If it has, simply returned the dataset in the argument (which was already queried for in the
    // resource)
    if (currentApprovalState == null || !currentApprovalState) {
      datasetDAO.updateDatasetApproval(approval, Instant.now(), user.getUserId(), datasetId);
      try {
        elasticSearchService.indexDatasets(List.of(datasetId));
      } catch (IOException ioException) {
        logWarn(
            "Error updating entry in ElasticSearch for dataset Id: %d".formatted(datasetId),
            ioException);
      }
      datasetReturn = datasetDAO.findDatasetWithoutFSOInformation(datasetId);
    } else {
      if (approval == null || !approval) {
        throw new IllegalArgumentException("Dataset is already approved");
      }
    }

    try {
      // if approval state changed
      if (currentApprovalState != datasetReturn.getDacApproval()) {
        User creatorUser = dataset.getCreateUser();
        sendDatasetApprovalNotificationEmail(dataset, creatorUser, approval);
      }
    } catch (Exception e) {
      logException(
          "Unable to notifier Data Submitter of dataset approval status: %s"
              .formatted(dataset.getDatasetIdentifier()),
          e);
    }
    return datasetReturn;
  }

  private void sendDatasetApprovalNotificationEmail(Dataset dataset, User user, boolean approval)
      throws Exception {
    Dac dac = dacDAO.findById(dataset.getDacId());
    if (approval) {
      sendDatasetApprovedMessage(
          user, dac.getName(), dataset.getDatasetIdentifier(), dataset.getName());
    } else {
      if (dac.getEmail() != null) {
        String dacEmail = dac.getEmail();
        sendDatasetDeniedMessage(user, dac.getName(), dataset.getDatasetIdentifier(), dacEmail);
      } else {
        logWarn("Unable to send dataset denied email to DAC: " + dac.getDacId());
      }
    }
  }

  @VisibleForTesting
  protected void sendDatasetDeniedMessage(
      User user, String dacName, String datasetName, String dacEmail)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new DatasetDeniedMessage(user, dacName, datasetName, dacEmail), user.getUserId());
  }

  @VisibleForTesting
  protected void sendDatasetApprovedMessage(
      User user, String dacName, String datasetIdentifier, String datasetName)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new DatasetApprovedMessage(user, dacName, datasetIdentifier, datasetName),
        user.getUserId());
  }

  public List<Dataset> findDatasetsByIds(User user, List<Integer> datasetIds) {
    return datasetDAO.findDatasetsByIdList(datasetIds).stream()
        .filter(d -> verifyPublicVisibilityAccess(d, user) != null)
        .toList();
  }

  public List<Integer> findAllDatasetIds() {
    return datasetDAO.findAllDatasetIds();
  }

  public Study getStudyWithDatasetsById(User user, Integer studyId) {
    try {
      Study study = studyDAO.findStudyById(studyId);
      if (study == null) {
        throw new NotFoundException("Study not found");
      }
      if (study.getDatasetIds() != null && !study.getDatasetIds().isEmpty()) {
        List<Dataset> datasets = findDatasetsByIds(user, new ArrayList<>(study.getDatasetIds()));
        study.addDatasets(datasets);
      }
      return study;
    } catch (NotFoundException nfe) {
      throw nfe;
    } catch (Exception e) {
      logException(e);
      throw e;
    }
  }

  public List<ApprovedDataset> getApprovedDatasets(User user) {
    try {
      return datasetDAO.getApprovedDatasets(user.getUserId());
    } catch (Exception e) {
      logException(e);
      throw e;
    }
  }

  /**
   * This method is used to convert a dataset into a study if none exist, or if one does, to update
   * the dataset, study, and associated properties with new values. This is an admin function only.
   *
   * @param dataset The dataset
   * @param studyConversion Study Conversion object
   * @return Updated/created study
   */
  public Study convertDatasetToStudy(User user, Dataset dataset, StudyConversion studyConversion) {
    if (!user.hasUserRole(UserRoles.ADMIN)) {
      throw new NotAuthorizedException("Admin use only");
    }
    // Study updates:
    Integer studyId = updateStudyFromConversion(user, dataset, studyConversion);

    // Dataset updates
    if (studyConversion.getDacId() != null) {
      datasetDAO.updateDatasetDacId(dataset.getDatasetId(), studyConversion.getDacId());
    }
    if (studyConversion.getDataUse() != null) {
      String translation =
          ontologyService.translateDataUse(
              studyConversion.getDataUse(), DataUseTranslationType.DATASET);
      datasetServiceDAO.updateDatasetDataUse(
          user, dataset, studyConversion.getDataUse(), translation);
    }
    if (studyConversion.getDatasetName() != null) {
      datasetDAO.updateDatasetName(dataset.getDatasetId(), studyConversion.getDatasetName());
    }
    elasticSearchService.synchronizeDatasetInESIndex(dataset, false);
    List<Dictionary> dictionaries = datasetDAO.getDictionaryTerms();
    // Handle "Phenotype/Indication"
    if (studyConversion.getPhenotype() != null) {
      legacyPropConversion(
          dictionaries,
          dataset,
          "Phenotype/Indication",
          null,
          PropertyType.String,
          studyConversion.getPhenotype());
    }

    // Handle "Species"
    if (studyConversion.getSpecies() != null) {
      legacyPropConversion(
          dictionaries,
          dataset,
          "Species",
          null,
          PropertyType.String,
          studyConversion.getSpecies());
    }

    if (studyConversion.getNumberOfParticipants() != null) {
      // Handle "# of participants"
      legacyPropConversion(
          dictionaries,
          dataset,
          "# of participants",
          "numberOfParticipants",
          PropertyType.Number,
          studyConversion.getNumberOfParticipants().toString());
    }

    // Handle "Data Location"
    if (studyConversion.getDataLocation() != null) {
      newPropConversion(
          dictionaries,
          dataset,
          "Data Location",
          "dataLocation",
          PropertyType.String,
          studyConversion.getDataLocation());
    }

    if (studyConversion.getUrl() != null) {
      // Handle "URL"
      newPropConversion(
          dictionaries, dataset, "URL", "url", PropertyType.String, studyConversion.getUrl());
    }

    // Handle "Data Submitter User ID"
    if (studyConversion.getDataSubmitterEmail() != null) {
      User submitter = userDAO.findUserByEmail(studyConversion.getDataSubmitterEmail());
      if (submitter != null) {
        datasetDAO.updateDatasetCreateUserId(dataset.getDatasetId(), user.getUserId());
      }
    }

    return studyDAO.findStudyById(studyId);
  }

  public Study updateStudyCustodians(User user, Integer studyId, String custodians) {
    logInfo(
        String.format(
            "User %s is updating custodians for study id: %s; custodians: %s",
            user.getEmail(), studyId, custodians));
    Study study = studyDAO.findStudyById(studyId);
    if (study == null) {
      throw new NotFoundException("Study not found");
    }
    boolean propPresent =
        study.getProperties().stream().anyMatch(prop -> prop.getKey().equals(dataCustodianEmail));
    if (propPresent) {
      studyDAO.updateStudyProperty(
          studyId, dataCustodianEmail, PropertyType.Json.toString(), custodians);
    } else {
      studyDAO.insertStudyProperty(
          studyId, dataCustodianEmail, PropertyType.Json.toString(), custodians);
    }
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(study.getDatasetIds());
    datasets.forEach(dataset -> elasticSearchService.synchronizeDatasetInESIndex(dataset, false));
    return studyDAO.findStudyById(studyId);
  }

  /**
   * Ensure that all requested datasetIds exist in the user's list of accepted DAAs
   *
   * @param user The requesting User
   * @param datasetIds The list of dataset ids the user is requesting access to
   */
  public void enforceDAARestrictions(User user, List<Integer> datasetIds) {
    List<Integer> userDaaDatasetIds = daaDAO.findDaaDatasetIdsByUserId(user.getUserId());
    boolean containsAll = new HashSet<>(userDaaDatasetIds).containsAll(datasetIds);
    if (!containsAll) {
      throw new BadRequestException(
          "User does not have appropriate Data Access Agreements for provided datasets");
    }
  }

  public List<DatasetAuthorizationReader> getAuthorizationReaders(long datasetId) {
    return datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetId(datasetId);
  }

  /**
   * This method is used to synchronize a new dataset property with values from the study conversion
   *
   * @param dictionaries List<Dictionary>
   * @param dataset Dataset
   * @param dictionaryName Name to look for in dictionaries
   * @param schemaProperty Schema Property to look for in properties
   * @param propertyType Property Type of new value
   * @param propValue New property value
   */
  private void newPropConversion(
      List<Dictionary> dictionaries,
      Dataset dataset,
      String dictionaryName,
      String schemaProperty,
      PropertyType propertyType,
      String propValue) {
    Optional<DatasetProperty> maybeProp =
        dataset.getProperties().stream()
            .filter(p -> Objects.nonNull(p.getSchemaProperty()))
            .filter(p -> p.getSchemaProperty().equals(schemaProperty))
            .findFirst();
    if (maybeProp.isPresent()) {
      datasetDAO.updateDatasetProperty(
          dataset.getDatasetId(), maybeProp.get().getPropertyKey(), propValue);
    } else {
      dictionaries.stream()
          .filter(d -> d.getKey().equals(dictionaryName))
          .findFirst()
          .ifPresent(
              dictionary -> {
                DatasetProperty prop = new DatasetProperty();
                prop.setDatasetId(dataset.getDatasetId());
                prop.setPropertyKey(dictionary.getKeyId());
                prop.setSchemaProperty(schemaProperty);
                prop.setPropertyValue(propValue);
                prop.setPropertyType(propertyType);
                prop.setCreateDate(new Date());
                datasetDAO.insertDatasetProperties(List.of(prop));
              });
    }
  }

  /**
   * This method is used to synchronize a legacy dataset property with values from the study
   * conversion
   *
   * @param dictionaries List<Dictionary>
   * @param dataset Dataset
   * @param dictionaryName Name to look for in dictionaries
   * @param schemaProperty Schema Property to update if necessary
   * @param propertyType Property Type of new value
   * @param propValue New property value
   */
  private void legacyPropConversion(
      List<Dictionary> dictionaries,
      Dataset dataset,
      String dictionaryName,
      String schemaProperty,
      PropertyType propertyType,
      String propValue) {
    Optional<DatasetProperty> maybeProp =
        dataset.getProperties().stream()
            .filter(p -> p.getPropertyName().equals(dictionaryName))
            .findFirst();
    Optional<Dictionary> dictionary =
        dictionaries.stream().filter(d -> d.getKey().equals(dictionaryName)).findFirst();
    // Legacy property exists, update it.
    if (dictionary.isPresent() && maybeProp.isPresent()) {
      datasetDAO.updateDatasetProperty(
          dataset.getDatasetId(), dictionary.get().getKeyId(), propValue);
    }
    // Legacy property does not exist, but we have a valid dictionary term, so create it.
    else if (dictionary.isPresent()) {
      DatasetProperty prop = new DatasetProperty();
      prop.setDatasetId(dataset.getDatasetId());
      prop.setPropertyKey(dictionary.get().getKeyId());
      prop.setSchemaProperty(schemaProperty);
      prop.setPropertyValue(propValue);
      prop.setPropertyType(propertyType);
      prop.setCreateDate(new Date());
      datasetDAO.insertDatasetProperties(List.of(prop));
    }
    // Neither legacy property nor dictionary term does not exist, log a warning.
    else {
      logWarn("Unable to find dictionary term: " + dictionaryName);
    }
  }

  private Integer updateStudyFromConversion(
      User user, Dataset dataset, StudyConversion studyConversion) {
    // Ensure that we are not trying to create a new study with an existing name
    Study study = studyDAO.findStudyByName(studyConversion.getName());
    Integer studyId;
    Integer userId =
        (dataset.getCreateUserId() != null) ? dataset.getCreateUserId() : user.getUserId();
    // Create or update the study:
    if (study == null) {
      study = studyConversion.createNewStudyStub();
      studyId =
          studyDAO.insertStudy(
              study.getName(),
              study.getDescription(),
              study.getPiName(),
              study.getPiEmail(),
              study.getDataTypes(),
              study.getPublicVisibility(),
              userId,
              Instant.now(),
              UUID.randomUUID());
      study.setStudyId(studyId);
    } else {
      studyId = study.getStudyId();
      studyDAO.updateStudy(
          study.getStudyId(),
          studyConversion.getName(),
          studyConversion.getDescription(),
          studyConversion.getPiName(),
          studyConversion.getPiEmail(),
          studyConversion.getDataTypes(),
          studyConversion.getPublicVisibility(),
          userId,
          Instant.now());
    }
    datasetDAO.updateStudyId(dataset.getDatasetId(), studyId);

    // Create or update study properties:
    Set<StudyProperty> existingProps = studyDAO.findStudyById(studyId).getProperties();
    // If we don't have any props, we need to add all of the new ones
    if (existingProps == null || existingProps.isEmpty()) {
      studyConversion.getStudyProperties().stream()
          .filter(Objects::nonNull)
          .forEach(
              p ->
                  studyDAO.insertStudyProperty(
                      studyId, p.getKey(), p.getType().toString(), p.getValue().toString()));
    } else {
      // Study props to add:
      studyConversion.getStudyProperties().stream()
          .filter(Objects::nonNull)
          .filter(p -> existingProps.stream().noneMatch(ep -> ep.getKey().equals(p.getKey())))
          .forEach(
              p ->
                  studyDAO.insertStudyProperty(
                      studyId, p.getKey(), p.getType().toString(), p.getValue().toString()));
      // Study props to update:
      studyConversion.getStudyProperties().stream()
          .filter(Objects::nonNull)
          .filter(p -> existingProps.stream().anyMatch(ep -> ep.equals(p)))
          .forEach(
              p ->
                  studyDAO.updateStudyProperty(
                      studyId, p.getKey(), p.getType().toString(), p.getValue().toString()));
    }
    return studyId;
  }

  public DatasetAuthorizationReader addAuthorizedReader(long id, long userId, long operatorId) {
    long recordId =
        datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(id, userId, operatorId);
    return datasetAuthorizationReaderDAO.findAuthorizedReaderByRecordId(recordId);
  }

  public boolean isAuthorizedToListUsers(Integer datasetId, Integer userId) {
    return !Objects.isNull(
        datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetIdAndUserId(datasetId, userId));
  }

  public void removeAuthorizedAccessReader(long datasetId, long userId) {
    datasetAuthorizationReaderDAO.deleteByDatasetAndUserId(datasetId, userId);
  }

  public Study patchStudy(Integer studyId, User user, StudyPatch patch) {
    try {
      Study study = studyDAO.findStudyById(studyId);
      datasetServiceDAO.patchStudy(study, user, patch);
      elasticSearchService.indexStudy(studyId);
      return studyDAO.findStudyById(studyId);
    } catch (Exception ex) {
      logException(ex);
      throw new InternalServerErrorException(
          "An error occurred patching study %s".formatted(studyId));
    }
  }
}
