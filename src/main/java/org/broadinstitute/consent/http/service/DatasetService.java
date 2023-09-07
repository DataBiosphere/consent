package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatasetService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  public static final String DATASET_NAME_KEY = "Dataset Name";
  private final DatasetDAO datasetDAO;
  private final UserRoleDAO userRoleDAO;
  private final DacDAO dacDAO;
  private final EmailService emailService;
  private final OntologyService ontologyService;
  private final StudyDAO studyDAO;

  @Inject
  public DatasetService(DatasetDAO dataSetDAO, UserRoleDAO userRoleDAO, DacDAO dacDAO,
      EmailService emailService, OntologyService ontologyService, StudyDAO studyDAO) {
    this.datasetDAO = dataSetDAO;
    this.userRoleDAO = userRoleDAO;
    this.dacDAO = dacDAO;
    this.emailService = emailService;
    this.ontologyService = ontologyService;
    this.studyDAO = studyDAO;
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
      // Some legacy dataset names can be null
      String dsAuditName =
          Objects.nonNull(dataset.getName()) ? dataset.getName() : dataset.getDatasetIdentifier();
      DatasetAudit dsAudit = new DatasetAudit(datasetId, dataset.getObjectId(), dsAuditName,
        new Date(), userId, AuditActions.DELETE.getValue().toUpperCase());
      try {
        datasetDAO.useTransaction(h -> {
          try {
            h.insertDatasetAudit(dsAudit);
            h.deleteUserAssociationsByDatasetId(datasetId);
            h.deleteDatasetPropertiesByDatasetId(datasetId);
            h.deleteConsentAssociationsByDatasetId(datasetId);
            h.deleteDatasetById(datasetId);
          } catch (Exception e) {
            h.rollback();
            throw e;
          }
        });
      } catch (Exception e) {
        logger.error(e.getMessage());
        throw e;
      }
    }
  }

  public List<Dataset> searchDatasets(String query, boolean openAccess, User user) {
    List<Dataset> datasets = findAllDatasetsByUser(user);
    return datasets.stream().filter(ds -> ds.isDatasetMatch(query, openAccess)).toList();
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
      emailService.sendDatasetDeniedMessage(
          user,
          dac.getName(),
          dataset.getDatasetIdentifier());
    }

  }

  private boolean filterDatasetOnProperties(DatasetDTO dataset, String term) {
    //datasets need to have consentId, null check to prevent NPE
    String consentId = dataset.getConsentId();
    Boolean consentIdMatch = Objects.nonNull(consentId) && consentId.toLowerCase().contains(term);
    return consentIdMatch || dataset.getProperties()
        .stream()
        .filter(p -> Objects.nonNull(p.getPropertyValue()))
        .anyMatch(p -> {
          return p.getPropertyValue().toLowerCase().contains(term);
        });
  }

  private boolean userHasRole(String roleName, Integer userId) {
    return userRoleDAO.findRoleByNameAndUser(roleName, userId) != null;
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

}
