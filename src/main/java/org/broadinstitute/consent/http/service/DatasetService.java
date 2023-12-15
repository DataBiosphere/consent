package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
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
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyConversion;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatasetService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  public static final String DATASET_NAME_KEY = "Dataset Name";
  private final DatasetDAO datasetDAO;
  private final DacDAO dacDAO;
  private final EmailService emailService;
  private final OntologyService ontologyService;
  private final StudyDAO studyDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final UserDAO userDAO;

  @Inject
  public DatasetService(DatasetDAO dataSetDAO, DacDAO dacDAO,
      EmailService emailService, OntologyService ontologyService, StudyDAO studyDAO,
      DatasetServiceDAO datasetServiceDAO, UserDAO userDAO) {
    this.datasetDAO = dataSetDAO;
    this.dacDAO = dacDAO;
    this.emailService = emailService;
    this.ontologyService = ontologyService;
    this.studyDAO = studyDAO;
    this.datasetServiceDAO = datasetServiceDAO;
    this.userDAO = userDAO;
  }

  public Collection<DatasetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetId) {
    return datasetDAO.findDatasetsByReceiveOrder(dataSetId);
  }

  @Deprecated
  public Collection<Dictionary> describeDictionaryByReceiveOrder() {
    return datasetDAO.getMappedFieldsOrderByReceiveOrder();
  }

  public Set<DatasetDTO> findDatasetsByDacIds(List<Integer> dacIds) {
    if (CollectionUtils.isEmpty(dacIds)) {
      throw new BadRequestException("No dataset IDs provided");
    }
    return datasetDAO.findDatasetsByDacIds(dacIds);
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
  public Dataset findDatasetByIdentifier(String datasetIdentifier) throws IllegalArgumentException {
    Integer alias = Dataset.parseIdentifierToAlias(datasetIdentifier);
    Dataset d = datasetDAO.findDatasetByAlias(alias);
    if (Objects.isNull(d)) {
      return null;
    }

    // technically, it is possible to have two dataset identifiers which
    // have the same alias but are not the same: e.g., DUOS-5 and DUOS-00005
    if (!Objects.equals(d.getDatasetIdentifier(), datasetIdentifier)) {
      return null;
    }
    return d;
  }

  public DatasetDTO createDatasetFromDatasetDTO(DatasetDTO dataset, String name, Integer userId) {
    if (Objects.nonNull(getDatasetByName(name))) {
      throw new IllegalArgumentException("Dataset name: " + name + " is already in use");
    }
    Timestamp now = new Timestamp(new Date().getTime());
    Integer createdDatasetId = datasetDAO.inTransaction(h -> {
      try {
        Integer id = h.insertDataset(name, now, userId, dataset.getObjectId(),
            dataset.getDataUse().toString(), dataset.getDacId());
        List<DatasetProperty> propertyList = processDatasetProperties(id, dataset.getProperties());
        h.insertDatasetProperties(propertyList);
        return id;
      } catch (Exception e) {
        if (Objects.nonNull(h)) {
          h.rollback();
        }
        logger.error("Exception creating dataset with consent: " + e.getMessage());
        throw e;
      }
    });
    dataset.setDataSetId(createdDatasetId);
    return getDatasetDTO(createdDatasetId);
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

  public Study findStudyById(Integer id) {
    return studyDAO.findStudyById(id);
  }

  public Dataset findDatasetById(Integer id) {
    return datasetDAO.findDatasetById(id);
  }

  public Optional<Dataset> updateDataset(DatasetDTO dataset, Integer datasetId, Integer userId) {
    Timestamp now = new Timestamp(new Date().getTime());

    if (Objects.isNull(dataset.getDatasetName())) {
      throw new IllegalArgumentException("Dataset 'Name' cannot be null");
    }

    Dataset old = findDatasetById(datasetId);
    Set<DatasetProperty> oldProperties = old.getProperties();

    List<DatasetPropertyDTO> updateDatasetPropertyDTOs = dataset.getProperties();
    List<DatasetProperty> updateDatasetProperties = processDatasetProperties(datasetId,
        updateDatasetPropertyDTOs);

    List<DatasetProperty> propertiesToAdd = updateDatasetProperties.stream()
        .filter(p -> oldProperties.stream()
            .noneMatch(op -> op.getPropertyName().equals(p.getPropertyName())))
        .toList();

    List<DatasetProperty> propertiesToUpdate = updateDatasetProperties.stream()
        .filter(p -> oldProperties.stream()
            .noneMatch(p::equals))
        .toList();

    if (propertiesToAdd.isEmpty() && propertiesToUpdate.isEmpty() &&
        dataset.getDatasetName().equals(old.getName())) {
      return Optional.empty();
    }

    updateDatasetProperties(propertiesToUpdate, List.of(), propertiesToAdd);
    datasetDAO.updateDataset(datasetId, dataset.getDatasetName(), now, userId,
        dataset.getDacId());
    Dataset updatedDataset = findDatasetById(datasetId);
    return Optional.of(updatedDataset);
  }

  public Dataset updateDatasetDataUse(User user, Integer datasetId, DataUse dataUse) {
    Dataset d = datasetDAO.findDatasetById(datasetId);
    if (Objects.isNull(d)) {
      throw new NotFoundException("Dataset not found: " + datasetId);
    }
    if (!user.hasUserRole(UserRoles.ADMIN)) {
      throw new IllegalArgumentException("Admin use only");
    }
    datasetDAO.updateDatasetDataUse(datasetId, dataUse.toString());
    return datasetDAO.findDatasetById(datasetId);
  }

  public Dataset syncDatasetDataUseTranslation(Integer datasetId) {
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (Objects.isNull(dataset)) {
      throw new NotFoundException("Dataset not found");
    }

    String translation = ontologyService.translateDataUse(dataset.getDataUse(),
        DataUseTranslationType.DATASET);
    datasetDAO.updateDatasetTranslatedDataUse(datasetId, translation);

    return datasetDAO.findDatasetById(datasetId);
  }

  private void updateDatasetProperties(List<DatasetProperty> updateProperties,
      List<DatasetProperty> deleteProperties, List<DatasetProperty> addProperties) {
    updateProperties.forEach(p -> datasetDAO
        .updateDatasetProperty(p.getDataSetId(), p.getPropertyKey(),
            p.getPropertyValue().toString()));
    deleteProperties.forEach(
        p -> datasetDAO.deleteDatasetPropertyByKey(p.getDataSetId(), p.getPropertyKey()));
    datasetDAO.insertDatasetProperties(addProperties);
  }

  public DatasetDTO getDatasetDTO(Integer datasetId) {
    Set<DatasetDTO> dataset = datasetDAO.findDatasetDTOWithPropertiesByDatasetId(datasetId);
    DatasetDTO result = new DatasetDTO();
    if (Objects.nonNull(dataset) && !dataset.isEmpty()) {
      result = dataset.iterator().next();
    }
    if (Objects.isNull(result.getDataSetId())) {
      throw new NotFoundException("Unable to find dataset with id: " + datasetId);
    }
    return result;
  }


  @Deprecated // Use synchronizeDatasetProperties() instead
  public List<DatasetProperty> processDatasetProperties(Integer datasetId,
      List<DatasetPropertyDTO> properties) {
    Date now = new Date();
    List<Dictionary> dictionaries = datasetDAO.getMappedFieldsOrderByReceiveOrder();
    List<String> keys = dictionaries.stream().map(Dictionary::getKey)
        .collect(Collectors.toList());

    return properties.stream()
        .filter(p -> keys.contains(p.getPropertyName()) && !p.getPropertyName()
            .equals(DATASET_NAME_KEY))
        .map(p ->
            new DatasetProperty(datasetId,
                dictionaries.get(keys.indexOf(p.getPropertyName())).getKeyId(),
                p.getPropertyValue(),
                PropertyType.String,
                now)
        )
        .collect(Collectors.toList());
  }

  public List<DatasetPropertyDTO> findInvalidProperties(List<DatasetPropertyDTO> properties) {
    List<Dictionary> dictionaries = datasetDAO.getMappedFieldsOrderByReceiveOrder();
    List<String> keys = dictionaries.stream().map(Dictionary::getKey)
        .collect(Collectors.toList());

    return properties.stream()
        .filter(p -> !keys.contains(p.getPropertyName()))
        .collect(Collectors.toList());
  }

  public List<DatasetPropertyDTO> findDuplicateProperties(List<DatasetPropertyDTO> properties) {
    Set<String> uniqueKeys = properties.stream()
        .map(DatasetPropertyDTO::getPropertyName)
        .collect(Collectors.toSet());
    if (uniqueKeys.size() != properties.size()) {
      List<DatasetPropertyDTO> allDuplicateProperties = new ArrayList<>();
      uniqueKeys.forEach(key -> {
        List<DatasetPropertyDTO> propertiesPerKey = properties.stream()
            .filter(property -> property.getPropertyName().equals(key))
            .collect(Collectors.toList());
        if (propertiesPerKey.size() > 1) {
          allDuplicateProperties.addAll(propertiesPerKey);
        }
      });
      return allDuplicateProperties;
    }
    return Collections.emptyList();
  }

  public void deleteDataset(Integer datasetId, Integer userId) throws Exception {
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (Objects.nonNull(dataset)) {
      datasetServiceDAO.deleteDataset(dataset, userId);
    }
  }

  public void deleteStudy(Study study, User user) throws Exception {
    datasetServiceDAO.deleteStudy(study, user);
  }

  public List<Dataset> searchDatasets(String query, AccessManagement accessManagement, User user) {
    List<Dataset> datasets = findAllDatasetsByUser(user);
    return datasets.stream().filter(ds -> ds.isDatasetMatch(query, accessManagement)).toList();
  }

  public Dataset approveDataset(Dataset dataset, User user, Boolean approval) {
    Boolean currentApprovalState = dataset.getDacApproval();
    Integer datasetId = dataset.getDataSetId();
    Dataset datasetReturn = dataset;
    //Only update and fetch the dataset if it hasn't already been approved
    //If it has, simply returned the dataset in the argument (which was already queried for in the resource)
    if (Objects.isNull(currentApprovalState) || !currentApprovalState) {
      datasetDAO.updateDatasetApproval(approval, Instant.now(), user.getUserId(), datasetId);
      datasetReturn = datasetDAO.findDatasetById(datasetId);
    } else {
      if (Objects.isNull(approval) || !approval) {
        throw new IllegalArgumentException("Dataset is already approved");
      }
    }

    try {
      // if approval state changed
      if (currentApprovalState != datasetReturn.getDacApproval()) {
        sendDatasetApprovalNotificationEmail(dataset, user, approval);
      }
    } catch (Exception e) {
      logger.error("Unable to notifier Data Submitter of dataset approval status: "
          + dataset.getDatasetIdentifier());
    }
    return datasetReturn;
  }

  private void sendDatasetApprovalNotificationEmail(Dataset dataset, User user, Boolean approval)
      throws Exception {
    Dac dac = dacDAO.findById(dataset.getDacId());
    if (approval) {
      emailService.sendDatasetApprovedMessage(
          user,
          dac.getName(),
          dataset.getDatasetIdentifier());
    } else {
      String dacEmail = "";
      if (!Objects.isNull(dac.getEmail())) {
        dacEmail = dac.getEmail();
      }
      emailService.sendDatasetDeniedMessage(
          user,
          dac.getName(),
          dataset.getDatasetIdentifier(),
          dacEmail);
    }

  }

  public List<Dataset> findAllDatasetsByUser(User user) {
    if (user.hasUserRole(UserRoles.ADMIN)) {
      return datasetDAO.findAllDatasets();
    } else {
      List<Dataset> datasets = datasetDAO.getDatasets();
      if (user.hasUserRole(UserRoles.CHAIRPERSON)) {
        List<Dataset> chairDatasets = datasetDAO.findDatasetsByAuthUserEmail(user.getEmail());
        return Stream
            .concat(chairDatasets.stream(), datasets.stream())
            .distinct()
            .collect(Collectors.toList());
      }
      return datasets;
    }
  }

  public List<Dataset> findDatasetsByIds(List<Integer> datasetIds) {
    return datasetDAO.findDatasetsByIdList(datasetIds);
  }

  public List<Dataset> findAllDatasets() {
    return datasetDAO.findAllDatasets();
  }

  public List<Dataset> findDatasetsForChairperson(User user) {
    List<Dac> dacs = dacDAO.findDacsForEmail(user.getEmail());

    return datasetDAO.findDatasetsForChairperson(dacs.stream().map(Dac::getDacId).toList());
  }

  public List<Dataset> findDatasetsByCustodian(User user) {
    return datasetDAO.findDatasetsByCustodian(user.getUserId(), user.getEmail());
  }

  public List<Dataset> findDatasetsForDataSubmitter(User user) {
    return datasetDAO.findDatasetsForDataSubmitter(user.getUserId(), user.getEmail());
  }

  public List<Dataset> findPublicDatasets() {
    return datasetDAO.findPublicDatasets();
  }

  public Study getStudyWithDatasetsById(Integer studyId) {
    try {
      Study study = studyDAO.findStudyById(studyId);
      if (Objects.isNull(study)) {
        throw new NotFoundException("Study not found");
      }
      if (Objects.nonNull(study.getDatasetIds()) && !study.getDatasetIds().isEmpty()) {
        List<Dataset> datasets = findDatasetsByIds(new ArrayList<>(study.getDatasetIds()));
        study.addDatasets(datasets);
      }
      return study;
    } catch (Exception e) {
      logger.error(e.getMessage());
      throw e;
    }

  }

  public List<ApprovedDataset> getApprovedDatasets(User user) {
    try {
      List<ApprovedDataset> approvedDatasets = datasetDAO.getApprovedDatasets(user.getUserId());
      return approvedDatasets;
    } catch (Exception e) {
      logger.error(e.getMessage());
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
    if (Objects.nonNull(studyConversion.getDacId())) {
      datasetDAO.updateDatasetDacId(dataset.getDataSetId(), studyConversion.getDacId());
    }
    if (Objects.nonNull(studyConversion.getDataUse())) {
      datasetDAO.updateDatasetDataUse(dataset.getDataSetId(),
          studyConversion.getDataUse().toString());
    }
    if (Objects.nonNull(studyConversion.getDataUse())) {
      String translation = ontologyService.translateDataUse(studyConversion.getDataUse(),
          DataUseTranslationType.DATASET);
      datasetDAO.updateDatasetTranslatedDataUse(dataset.getDataSetId(), translation);
    }
    if (Objects.nonNull(studyConversion.getDatasetName())) {
      datasetDAO.updateDatasetName(dataset.getDataSetId(), studyConversion.getDatasetName());
    }

    List<Dictionary> dictionaries = datasetDAO.getDictionaryTerms();
    // Dataset Property updates
    if (Objects.nonNull(studyConversion.getDacId())) {
      newPropConversion(dictionaries, dataset, "DAC ID", "dataAccessCommitteeId",
          PropertyType.Number, studyConversion.getDacId().toString());
    }

    // Handle "Phenotype/Indication"
    if (Objects.nonNull(studyConversion.getPhenotype())) {
      oldPropConversion(dictionaries, dataset, "Phenotype/Indication", PropertyType.String,
          studyConversion.getPhenotype());
    }

    // Handle "Species"
    if (Objects.nonNull(studyConversion.getSpecies())) {
      oldPropConversion(dictionaries, dataset, "Species", PropertyType.String,
          studyConversion.getSpecies());
    }

    if (Objects.nonNull(studyConversion.getPiName())) {
      // Handle "PI Name"
      newPropConversion(dictionaries, dataset, "PI Name", "piName", PropertyType.String,
          studyConversion.getPiName());
      // Handle "Principal Investigator(PI)"
      oldPropConversion(dictionaries, dataset, "Principal Investigator(PI)", PropertyType.String,
          studyConversion.getPiName());
    }

    if (Objects.nonNull(studyConversion.getNumberOfParticipants())) {
      // Handle "Number of Participants"
      newPropConversion(dictionaries, dataset, "Number of Participants", "numberOfParticipants",
          PropertyType.Number, studyConversion.getNumberOfParticipants().toString());
      // Handle "# of participants"
      oldPropConversion(dictionaries, dataset, "# of participants", PropertyType.Number,
          studyConversion.getNumberOfParticipants().toString());
    }

    // Handle "Data Location"
    if (Objects.nonNull(studyConversion.getDataLocation())) {
      newPropConversion(dictionaries, dataset, "Data Location", "dataLocation", PropertyType.String,
          studyConversion.getDataLocation());
    }

    if (Objects.nonNull(studyConversion.getUrl())) {
      // Handle "URL"
      newPropConversion(dictionaries, dataset, "URL", "url", PropertyType.String,
          studyConversion.getUrl());
      // Handle "dbGAP"
      oldPropConversion(dictionaries, dataset, "dbGAP", PropertyType.String,
          studyConversion.getUrl());
    }

    // Handle "Data Submitter User ID"
    if (Objects.nonNull(studyConversion.getDataSubmitterEmail())) {
      User submitter = userDAO.findUserByEmail(studyConversion.getDataSubmitterEmail());
      if (Objects.nonNull(submitter)) {
        newPropConversion(dictionaries, dataset, "Data Submitter User ID", "dataSubmitterUserId",
            PropertyType.Number, user.getUserId().toString());
        datasetDAO.updateDatasetCreateUserId(dataset.getDataSetId(), user.getUserId());
      }
    }

    return studyDAO.findStudyById(studyId);
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
      datasetDAO.updateDatasetProperty(dataset.getDataSetId(), maybeProp.get().getPropertyKey(),
          propValue);
    } else {
      dictionaries.stream()
          .filter(d -> d.getKey().equals(dictionaryName))
          .findFirst()
          .ifPresent(dictionary -> {
            DatasetProperty prop = new DatasetProperty();
            prop.setDataSetId(dataset.getDataSetId());
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
   * This method is used to synchronize an OLD dataset property with values from the study
   * conversion
   *
   * @param dictionaries   List<Dictionary>
   * @param dataset        Dataset
   * @param dictionaryName Name to look for in dictionaries
   * @param propertyType   Property Type of new value
   * @param propValue      New property value
   */
  private void oldPropConversion(List<Dictionary> dictionaries, Dataset dataset,
      String dictionaryName, PropertyType propertyType, String propValue) {
    Optional<DatasetProperty> maybeProp = dataset.getProperties().stream()
        .filter(p -> p.getPropertyName().equals(dictionaryName))
        .findFirst();
    if (maybeProp.isPresent()) {
      datasetDAO.updateDatasetProperty(dataset.getDataSetId(), maybeProp.get().getPropertyKey(),
          propValue);
    } else {
      dictionaries.stream()
          .filter(d -> d.getKey().equals(dictionaryName))
          .findFirst()
          .ifPresent(dictionary -> {
            DatasetProperty prop = new DatasetProperty();
            prop.setDataSetId(dataset.getDataSetId());
            prop.setPropertyKey(dictionary.getKeyId());
            prop.setSchemaProperty(null);
            prop.setPropertyValue(propValue);
            prop.setPropertyType(propertyType);
            prop.setCreateDate(new Date());
            datasetDAO.insertDatasetProperties(List.of(prop));
          });
    }
  }

  private Integer updateStudyFromConversion(User user, Dataset dataset,
      StudyConversion studyConversion) {
    // Ensure that we are not trying to create a new study with an existing name
    Study study = studyDAO.findStudyByName(studyConversion.getName());
    Integer studyId;
    Integer userId =
        Objects.nonNull(dataset.getCreateUserId()) ? dataset.getCreateUserId() : user.getUserId();
    // Create or update the study:
    if (Objects.isNull(study)) {
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
    datasetDAO.updateStudyId(dataset.getDataSetId(), studyId);

    // Create or update study properties:
    Set<StudyProperty> existingProps = studyDAO.findStudyById(studyId).getProperties();
    User submitter = userDAO.findUserByEmail(studyConversion.getDataSubmitterEmail());
    // If we don't have any props, we need to add all of the new ones
    if (Objects.isNull(existingProps) || existingProps.isEmpty()) {
      studyConversion.getStudyProperties(submitter).stream()
          .filter(Objects::nonNull)
          .forEach(p -> studyDAO.insertStudyProperty(studyId, p.getKey(), p.getType().toString(),
              p.getValue().toString()));
    } else {
      // Study props to add:
      studyConversion.getStudyProperties(submitter).stream()
          .filter(Objects::nonNull)
          .filter(p -> existingProps.stream().noneMatch(ep -> ep.getKey().equals(p.getKey())))
          .forEach(p -> studyDAO.insertStudyProperty(studyId, p.getKey(), p.getType().toString(),
              p.getValue().toString()));
      // Study props to update:
      studyConversion.getStudyProperties(submitter).stream()
          .filter(Objects::nonNull)
          .filter(p -> existingProps.stream().anyMatch(ep -> ep.equals(p)))
          .forEach(p -> studyDAO.updateStudyProperty(studyId, p.getKey(), p.getType().toString(),
              p.getValue().toString()));
    }
    return studyId;
  }

}
