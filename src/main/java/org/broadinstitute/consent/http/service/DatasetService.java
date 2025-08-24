package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataCustodianEmail;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyConversion;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

public class DatasetService implements ConsentLogger {

  private final DatasetDAO datasetDAO;
  private final DaaDAO daaDAO;
  private final DacDAO dacDAO;
  private final ElasticSearchService elasticSearchService;
  private final EmailService emailService;
  private final OntologyService ontologyService;
  private final StudyDAO studyDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final UserDAO userDAO;
  public Integer datasetBatchSize = 50;

  @Inject
  public DatasetService(DatasetDAO dataSetDAO, DaaDAO daaDAO, DacDAO dacDAO, ElasticSearchService
      elasticSearchService, EmailService emailService, OntologyService ontologyService, StudyDAO
      studyDAO, DatasetServiceDAO datasetServiceDAO, UserDAO userDAO) {
    this.datasetDAO = dataSetDAO;
    this.daaDAO = daaDAO;
    this.dacDAO = dacDAO;
    this.elasticSearchService = elasticSearchService;
    this.emailService = emailService;
    this.ontologyService = ontologyService;
    this.studyDAO = studyDAO;
    this.datasetServiceDAO = datasetServiceDAO;
    this.userDAO = userDAO;
  }

  public List<Dataset> findDatasetListByDacIds(List<Integer> dacIds) {
    if (CollectionUtils.isEmpty(dacIds)) {
      throw new BadRequestException("No dataset IDs provided");
    }
    return datasetDAO.findDatasetListByDacIds(dacIds);
  }

  /**
   * TODO: Refactor this to throw a NotFoundException instead of returning null
   * Finds a Dataset by a formatted dataset identifier.
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

  protected Dataset verifyPublicVisibilityAccess(Dataset dataset, User user) {
    // If there is no study, we can't verify visibility, so return the dataset
    if (dataset.getStudy() == null) {
      return dataset;
    }
    // If not visible, check that the user is authorized to see it
    if (Boolean.FALSE.equals(dataset.getStudy().getPublicVisibility())) {
      if (isCreatorOrCustodian(user, dataset)) {
        return dataset;
      } else {
        return null;
      }
    }
    return dataset;
  }

  protected boolean isCreatorOrCustodian(User user, Dataset dataset) {
    if (dataset.getCreateUserId().equals(user.getUserId())) {
      return true;
    }
    if (dataset.getStudy() == null) {
      return false;
    }
    return isCreatorOrCustodian(user, dataset.getStudy());
  }

  public boolean isCreatorOrCustodian(User user, Study study) {
    if (study.getCreateUserId().equals(user.getUserId())) {
      return true;
    }
    Optional<StudyProperty> custodianProp = study.getProperties().stream()
        .filter(p -> p.getKey().equals(dataCustodianEmail))
        .findFirst();
    if (custodianProp.isPresent()) {
      Gson gson = GsonUtil.getInstance();
      // prop is a JsonArray of Strings
      List<String> custodians = gson.fromJson(custodianProp.get().getValue().toString(), new TypeToken<List<String>>() {}.getType());
      for (String custodian : custodians) {
        if (user.getEmail().equals(custodian.trim())) {
          return true;
        }
      }
    }
    return false;
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
    datasetDAO.updateDatasetDataUse(datasetId, dataUse.toString());
    elasticSearchService.synchronizeDatasetInESIndex(d, user, false);
    return datasetDAO.findDatasetById(datasetId);
  }

  public Dataset syncDatasetDataUseTranslation(Integer datasetId, User user) {
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (dataset == null) {
      throw new NotFoundException("Dataset not found");
    }

    String translation = ontologyService.translateDataUse(dataset.getDataUse(),
        DataUseTranslationType.DATASET);
    datasetDAO.updateDatasetTranslatedDataUse(datasetId, translation);
    elasticSearchService.synchronizeDatasetInESIndex(dataset, user, false);
    return datasetDAO.findDatasetById(datasetId);
  }

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
    study.getDatasetIds().forEach(datasetId -> {
      try (var response = elasticSearchService.deleteIndex(datasetId, user.getUserId())) {
        if (!HttpStatusCodes.isSuccess(response.getStatus())) {
          logWarn("Response error, unable to delete dataset from index: %s".formatted(datasetId));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    datasetServiceDAO.deleteStudy(study, user);
  }

  public List<DatasetSummary> searchDatasetSummaries(String query) {
    return datasetDAO.findDatasetSummariesByQuery(query);
  }

  public List<DatasetStudySummary> findAllDatasetStudySummaries() {
    return datasetDAO.findAllDatasetStudySummaries();
  }

  public Dataset approveDataset(Dataset dataset, User user, Boolean approval) {
    Boolean currentApprovalState = dataset.getDacApproval();
    Integer datasetId = dataset.getDatasetId();
    Dataset datasetReturn = dataset;
    //Only update and fetch the dataset if it hasn't already been approved
    //If it has, simply returned the dataset in the argument (which was already queried for in the resource)
    if (currentApprovalState == null || !currentApprovalState) {
      datasetDAO.updateDatasetApproval(approval, Instant.now(), user.getUserId(), datasetId);
      elasticSearchService.asyncDatasetInESIndex(datasetId, user, true);
      datasetReturn = datasetDAO.findDatasetWithoutFSOInformation(datasetId);
    } else {
      if (approval == null || !approval) {
        throw new IllegalArgumentException("Dataset is already approved");
      }
    }

    try {
      // if approval state changed
      if (currentApprovalState != datasetReturn.getDacApproval()) {
        sendDatasetApprovalNotificationEmail(dataset, user, approval);
      }
    } catch (Exception e) {
      logException("Unable to notifier Data Submitter of dataset approval status: %s".formatted(
          dataset.getDatasetIdentifier()), e);
    }
    return datasetReturn;
  }

  private void sendDatasetApprovalNotificationEmail(Dataset dataset, User user, boolean approval)
      throws Exception {
    Dac dac = dacDAO.findById(dataset.getDacId());
    if (approval) {
      emailService.sendDatasetApprovedMessage(
          user,
          dac.getName(),
          dataset.getDatasetIdentifier());
    } else {
      if (dac.getEmail() != null) {
        String dacEmail = dac.getEmail();
        emailService.sendDatasetDeniedMessage(
            user,
            dac.getName(),
            dataset.getDatasetIdentifier(),
            dacEmail);
      } else {
        logWarn("Unable to send dataset denied email to DAC: " + dac.getDacId());
      }
    }

  }

  public List<Dataset> findDatasetsByIds(User user, List<Integer> datasetIds) {
    return datasetDAO.findDatasetsByIdList(datasetIds).stream().filter(
        d -> verifyPublicVisibilityAccess(d, user) != null
    ).toList();
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
      List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
      return approvedDatasets;
    } catch (Exception e) {
      logException(e);
      throw e;
    }
  }

  /**
   * This method is used to convert a dataset into a study if none exist, or if one does, to update
   * the dataset, study, and associated properties with new values. This is an admin function only.
   *
   * @param dataset         The dataset
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
      datasetDAO.updateDatasetDataUse(dataset.getDatasetId(),
          studyConversion.getDataUse().toString());
    }
    if (studyConversion.getDataUse() != null) {
      String translation = ontologyService.translateDataUse(studyConversion.getDataUse(),
          DataUseTranslationType.DATASET);
      datasetDAO.updateDatasetTranslatedDataUse(dataset.getDatasetId(), translation);
    }
    if (studyConversion.getDatasetName() != null) {
      datasetDAO.updateDatasetName(dataset.getDatasetId(), studyConversion.getDatasetName());
    }
    elasticSearchService.synchronizeDatasetInESIndex(dataset, user, false);
    List<Dictionary> dictionaries = datasetDAO.getDictionaryTerms();
    // Handle "Phenotype/Indication"
    if (studyConversion.getPhenotype() != null) {
      legacyPropConversion(dictionaries, dataset, "Phenotype/Indication", null, PropertyType.String,
          studyConversion.getPhenotype());
    }

    // Handle "Species"
    if (studyConversion.getSpecies() != null) {
      legacyPropConversion(dictionaries, dataset, "Species", null, PropertyType.String,
          studyConversion.getSpecies());
    }

    if (studyConversion.getNumberOfParticipants() != null) {
      // Handle "# of participants"
      legacyPropConversion(dictionaries, dataset, "# of participants", "numberOfParticipants",
          PropertyType.Number,
          studyConversion.getNumberOfParticipants().toString());
    }

    // Handle "Data Location"
    if (studyConversion.getDataLocation() != null) {
      newPropConversion(dictionaries, dataset, "Data Location", "dataLocation", PropertyType.String,
          studyConversion.getDataLocation());
    }

    if (studyConversion.getUrl() != null) {
      // Handle "URL"
      newPropConversion(dictionaries, dataset, "URL", "url", PropertyType.String,
          studyConversion.getUrl());
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
    logInfo(String.format("User %s is updating custodians for study id: %s; custodians: %s",
        user.getEmail(), studyId, custodians));
    Study study = studyDAO.findStudyById(studyId);
    if (study == null) {
      throw new NotFoundException("Study not found");
    }
    boolean propPresent = study.getProperties().stream()
        .anyMatch(prop -> prop.getKey().equals(dataCustodianEmail));
    if (propPresent) {
      studyDAO.updateStudyProperty(studyId, dataCustodianEmail, PropertyType.Json.toString(),
          custodians);
    } else {
      studyDAO.insertStudyProperty(studyId, dataCustodianEmail, PropertyType.Json.toString(),
          custodians);
    }
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(study.getDatasetIds());
    datasets.forEach(
        dataset -> elasticSearchService.synchronizeDatasetInESIndex(dataset, user, false));
    return studyDAO.findStudyById(studyId);
  }

  /**
   * Ensure that all requested datasetIds exist in the user's list of accepted DAAs
   *
   * @param user       The requesting User
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

  /**
   * This method is used to synchronize a new dataset property with values from the study
   * conversion
   *
   * @param dictionaries   List<Dictionary>
   * @param dataset        Dataset
   * @param dictionaryName Name to look for in dictionaries
   * @param schemaProperty Schema Property to look for in properties
   * @param propertyType   Property Type of new value
   * @param propValue      New property value
   */
  private void newPropConversion(List<Dictionary> dictionaries, Dataset dataset,
      String dictionaryName, String schemaProperty, PropertyType propertyType, String propValue) {
    Optional<DatasetProperty> maybeProp = dataset.getProperties().stream()
        .filter(p -> Objects.nonNull(p.getSchemaProperty()))
        .filter(p -> p.getSchemaProperty().equals(schemaProperty))
        .findFirst();
    if (maybeProp.isPresent()) {
      datasetDAO.updateDatasetProperty(dataset.getDatasetId(), maybeProp.get().getPropertyKey(),
          propValue);
    } else {
      dictionaries.stream()
          .filter(d -> d.getKey().equals(dictionaryName))
          .findFirst()
          .ifPresent(dictionary -> {
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
   * @param dictionaries   List<Dictionary>
   * @param dataset        Dataset
   * @param dictionaryName Name to look for in dictionaries
   * @param schemaProperty Schema Property to update if necessary
   * @param propertyType   Property Type of new value
   * @param propValue      New property value
   */
  private void legacyPropConversion(List<Dictionary> dictionaries, Dataset dataset,
      String dictionaryName, String schemaProperty, PropertyType propertyType, String propValue) {
    Optional<DatasetProperty> maybeProp = dataset.getProperties().stream()
        .filter(p -> p.getPropertyName().equals(dictionaryName))
        .findFirst();
    Optional<Dictionary> dictionary = dictionaries.stream()
        .filter(d -> d.getKey().equals(dictionaryName))
        .findFirst();
    // Legacy property exists, update it.
    if (dictionary.isPresent() && maybeProp.isPresent()) {
      datasetDAO.updateDatasetProperty(dataset.getDatasetId(), dictionary.get().getKeyId(),
          propValue);
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

  private Integer updateStudyFromConversion(User user, Dataset dataset,
      StudyConversion studyConversion) {
    // Ensure that we are not trying to create a new study with an existing name
    Study study = studyDAO.findStudyByName(studyConversion.getName());
    Integer studyId;
    Integer userId =
        (dataset.getCreateUserId() != null) ? dataset.getCreateUserId() : user.getUserId();
    // Create or update the study:
    if (study == null) {
      study = studyConversion.createNewStudyStub();
      studyId = studyDAO.insertStudy(study.getName(), study.getDescription(), study.getPiName(),
          study.getDataTypes(), study.getPublicVisibility(), userId, Instant.now(),
          UUID.randomUUID());
      study.setStudyId(studyId);
    } else {
      studyId = study.getStudyId();
      studyDAO.updateStudy(study.getStudyId(), studyConversion.getName(),
          studyConversion.getDescription(), studyConversion.getPiName(),
          studyConversion.getDataTypes(), studyConversion.getPublicVisibility(), userId,
          Instant.now());
    }
    datasetDAO.updateStudyId(dataset.getDatasetId(), studyId);

    // Create or update study properties:
    Set<StudyProperty> existingProps = studyDAO.findStudyById(studyId).getProperties();
    // If we don't have any props, we need to add all of the new ones
    if (existingProps == null || existingProps.isEmpty()) {
      studyConversion.getStudyProperties().stream()
          .filter(Objects::nonNull)
          .forEach(p -> studyDAO.insertStudyProperty(studyId, p.getKey(), p.getType().toString(),
              p.getValue().toString()));
    } else {
      // Study props to add:
      studyConversion.getStudyProperties().stream()
          .filter(Objects::nonNull)
          .filter(p -> existingProps.stream().noneMatch(ep -> ep.getKey().equals(p.getKey())))
          .forEach(p -> studyDAO.insertStudyProperty(studyId, p.getKey(), p.getType().toString(),
              p.getValue().toString()));
      // Study props to update:
      studyConversion.getStudyProperties().stream()
          .filter(Objects::nonNull)
          .filter(p -> existingProps.stream().anyMatch(ep -> ep.equals(p)))
          .forEach(p -> studyDAO.updateStudyProperty(studyId, p.getKey(), p.getType().toString(),
              p.getValue().toString()));
    }
    return studyId;
  }

  public void setDatasetBatchSize(Integer datasetBatchSize) {
    this.datasetBatchSize = datasetBatchSize;
  }

}
