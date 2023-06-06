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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
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
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DatasetService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  public static final String DATASET_NAME_KEY = "Dataset Name";
  public static final String CONSENT_NAME_PREFIX = "DUOS-DS-CG-";
  private final ConsentDAO consentDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final UserRoleDAO userRoleDAO;
  private final DacDAO dacDAO;
  private final UseRestrictionConverter converter;
  private final EmailService emailService;
  private final StudyDAO studyDAO;

  @Inject
  public DatasetService(ConsentDAO consentDAO, DataAccessRequestDAO dataAccessRequestDAO,
      DatasetDAO dataSetDAO,
      DatasetServiceDAO datasetServiceDAO, UserRoleDAO userRoleDAO, DacDAO dacDAO,
      UseRestrictionConverter converter,
      EmailService emailService, StudyDAO studyDAO) {
    this.consentDAO = consentDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.datasetDAO = dataSetDAO;
    this.datasetServiceDAO = datasetServiceDAO;
    this.userRoleDAO = userRoleDAO;
    this.dacDAO = dacDAO;
    this.converter = converter;
    this.emailService = emailService;
    this.studyDAO = studyDAO;
  }

  public Collection<DatasetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetId) {
    return datasetDAO.findDatasetsByReceiveOrder(dataSetId);
  }

  @Deprecated
  public Collection<Dictionary> describeDictionaryByDisplayOrder() {
    return datasetDAO.getMappedFieldsOrderByDisplayOrder();
  }

  @Deprecated
  public Collection<Dictionary> describeDictionaryByReceiveOrder() {
    return datasetDAO.getMappedFieldsOrderByReceiveOrder();
  }

  public void disableDataset(Integer datasetId, Boolean active) {
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (dataset != null) {
      datasetDAO.updateDatasetActive(dataset.getDataSetId(), active);
    }
  }

  public Dataset updateNeedsReviewDatasets(Integer datasetId, Boolean needsApproval) {
    if (datasetDAO.findDatasetById(datasetId) == null) {
      throw new NotFoundException("DataSet doesn't exist");
    }
    datasetDAO.updateDatasetNeedsApproval(datasetId, needsApproval);
    return datasetDAO.findDatasetById(datasetId);
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

  /**
   * Create a minimal consent from the data provided in a Dataset.
   *
   * @param dataset The DataSetDTO
   * @return The created Consent
   */
  public Consent createConsentForDataset(DatasetDTO dataset) {
    String consentId = UUID.randomUUID().toString();
    Optional<DatasetPropertyDTO> nameProp = dataset.getProperties()
        .stream()
        .filter(p -> p.getPropertyName().equalsIgnoreCase(DATASET_NAME_KEY))
        .findFirst();
    // Typically, this is a construct from ORSP consisting of dataset name and some form of investigator code.
    // In our world, we'll use that dataset name if provided, or the alias.
    String groupName =
        nameProp.isPresent() ? nameProp.get().getPropertyValue() : dataset.getAlias();
    String name = CONSENT_NAME_PREFIX + dataset.getDataSetId();
    Date createDate = new Date();
    if (Objects.nonNull(dataset.getDataUse())) {
      boolean manualReview = isConsentDataUseManualReview(dataset.getDataUse());
      /*
       * Consents created for a dataset do not need the following properties:
       * data user letter
       * data user letter name
       */
      String translatedUseRestriction = converter.translateDataUse(dataset.getDataUse(),
          DataUseTranslationType.DATASET);
      consentDAO.useTransaction(h -> {
        try {
          h.insertConsent(consentId, manualReview, dataset.getDataUse().toString(), null, name,
              null, createDate, createDate, translatedUseRestriction, groupName);
          String associationType = AssociationType.SAMPLE_SET.getValue();
          h.insertConsentAssociation(consentId, associationType, dataset.getDataSetId());
        } catch (Exception e) {
          h.rollback();
          logger.error("Exception creating consent: " + e.getMessage());
          throw e;
        }
      });
      return consentDAO.findConsentById(consentId);
    } else {
      throw new IllegalArgumentException(
          "Dataset is missing Data Use information. Consent could not be created.");
    }
  }

  private boolean isConsentDataUseManualReview(DataUse dataUse) {
    return Objects.nonNull(dataUse.getOther()) ||
        (Objects.nonNull(dataUse.getPopulationRestrictions()) && !dataUse
            .getPopulationRestrictions().isEmpty()) ||
        (Objects.nonNull(dataUse.getAddiction()) && dataUse.getAddiction()) ||
        (Objects.nonNull(dataUse.getEthicsApprovalRequired()) && dataUse
            .getEthicsApprovalRequired()) ||
        (Objects.nonNull(dataUse.getIllegalBehavior()) && dataUse.getIllegalBehavior()) ||
        (Objects.nonNull(dataUse.getManualReview()) && dataUse.getManualReview()) ||
        (Objects.nonNull(dataUse.getOtherRestrictions()) && dataUse.getOtherRestrictions()) ||
        (Objects.nonNull(dataUse.getPopulationOriginsAncestry()) && dataUse
            .getPopulationOriginsAncestry()) ||
        (Objects.nonNull(dataUse.getPsychologicalTraits()) && dataUse
            .getPsychologicalTraits()) ||
        (Objects.nonNull(dataUse.getSexualDiseases()) && dataUse.getSexualDiseases()) ||
        (Objects.nonNull(dataUse.getStigmatizeDiseases()) && dataUse.getStigmatizeDiseases())
        ||
        (Objects.nonNull(dataUse.getVulnerablePopulations()) && dataUse
            .getVulnerablePopulations());
  }

  public DatasetDTO createDatasetWithConsent(DatasetDTO dataset, String name, Integer userId)
      throws Exception {
    if (Objects.nonNull(getDatasetByName(name))) {
      throw new IllegalArgumentException("Dataset name: " + name + " is already in use");
    }
    Timestamp now = new Timestamp(new Date().getTime());
    Integer createdDatasetId = datasetDAO.inTransaction(h -> {
      try {
        Integer id = h.insertDataset(name, now, userId, dataset.getObjectId(), false,
            dataset.getDataUse().toString(), dataset.getDacId());
        List<DatasetProperty> propertyList = processDatasetProperties(id, dataset.getProperties());
        h.insertDatasetProperties(propertyList);
        h.updateDatasetNeedsApproval(id, dataset.getNeedsApproval());
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
    try {
      createConsentForDataset(dataset);
    } catch (Exception e) {
      logger.error("Exception creating consent for dataset: " + e.getMessage());
      deleteDataset(createdDatasetId, userId);
      throw e;
    }
    return getDatasetDTO(createdDatasetId);
  }

  public Dataset getDatasetByName(String name) {
    String lowercaseName = name.toLowerCase();
    return datasetDAO.getDatasetByName(lowercaseName);
  }

  public Set<String> findAllActiveStudyNames() {
    return datasetDAO.findAllActiveStudyNames();
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
    if (Objects.isNull(dataset.getNeedsApproval())) {
      throw new IllegalArgumentException("Dataset 'Needs Approval' field cannot be null");
    }

    Dataset old = findDatasetById(datasetId);
    Set<DatasetProperty> oldProperties = old.getProperties();

    List<DatasetPropertyDTO> updateDatasetPropertyDTOs = dataset.getProperties();
    List<DatasetProperty> updateDatasetProperties = processDatasetProperties(datasetId,
        updateDatasetPropertyDTOs);

    List<DatasetProperty> propertiesToAdd = updateDatasetProperties.stream()
        .filter(p -> oldProperties.stream()
            .noneMatch(op -> op.getPropertyKey().equals(p.getPropertyKey())))
        .collect(Collectors.toList());

    List<DatasetProperty> propertiesToUpdate = updateDatasetProperties.stream()
        .filter(p -> oldProperties.stream()
            .noneMatch(p::equals))
        .collect(Collectors.toList());

    List<DatasetProperty> propertiesToDelete = oldProperties.stream()
        .filter(op -> updateDatasetProperties.stream()
            .noneMatch(p -> p.getPropertyKey().equals(op.getPropertyKey()))
        ).collect(Collectors.toList());

    if (propertiesToAdd.isEmpty() && propertiesToUpdate.isEmpty() && propertiesToDelete
        .isEmpty() && dataset.getDatasetName().equals(old.getName())) {
      return Optional.empty();
    }

    updateDatasetProperties(propertiesToUpdate, propertiesToDelete, propertiesToAdd);
    datasetDAO.updateDataset(datasetId, dataset.getDatasetName(), now, userId,
        dataset.getNeedsApproval(), dataset.getDacId());
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
          new Date(), dataset.getActive(), userId, AuditActions.DELETE.getValue().toUpperCase());
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

  @Deprecated
  public Set<DatasetDTO> describeDatasets(Integer userId) {
    List<Integer> datasetIdsInUse = dataAccessRequestDAO.findAllDARDatasetRelationDatasetIds();
    HashSet<DatasetDTO> datasets = new HashSet<>();
    if (userHasRole(UserRoles.ADMIN.getRoleName(), userId)) {
      datasets.addAll(datasetDAO.findAllDatasetDTOs());
    } else {
      datasets.addAll(datasetDAO.findActiveDatasetDTOs());
      if (userHasRole(UserRoles.CHAIRPERSON.getRoleName(), userId)) {
        Collection<DatasetDTO> chairSpecificDatasets = datasetDAO.findDatasetDTOsByUserId(userId);
        datasets.addAll(chairSpecificDatasets);
      }
    }
    datasets.forEach(d -> d.setDeletable(!datasetIdsInUse.contains(d.getDataSetId())));
    return datasets;
  }


  public List<Dataset> searchDatasets(String query, User user) {
    List<Dataset> datasets = findAllDatasetsByUser(user);
    return datasets.stream().filter(ds -> ds.isStringMatch(query)).toList();
  }

  @Deprecated
  public List<Map<String, String>> autoCompleteDatasets(String partial, Integer userId) {
    Set<DatasetDTO> datasets = describeDatasets(userId);
    String lowercasePartial = partial.toLowerCase();
    Set<DatasetDTO> filteredDatasetsContainingPartial = datasets.stream()
        .filter(ds -> filterDatasetOnProperties(ds, lowercasePartial))
        .collect(Collectors.toSet());
    return filteredDatasetsContainingPartial.stream().map(ds ->
        {
          HashMap<String, String> map = new HashMap<>();
          List<DatasetPropertyDTO> properties = ds.getProperties();
          Optional<DatasetPropertyDTO> datasetName = properties.stream()
              .filter(p -> p.getPropertyName().equalsIgnoreCase("Dataset Name")).findFirst();
          Optional<DatasetPropertyDTO> pi = properties.stream()
              .filter(p -> p.getPropertyName().equalsIgnoreCase("Principal Investigator(PI)"))
              .findFirst();
          String datasetNameString =
              datasetName.isPresent() ? datasetName.get().getPropertyValue() : "";
          String piString = pi.isPresent() ? pi.get().getPropertyValue() : "";
          map.put("id", ds.getDataSetId().toString());
          map.put("objectId", ds.getObjectId());
          map.put("concatenation",
              datasetNameString + " | " + piString + " | " + ds.getConsentId());
          return map;
        }
    ).collect(Collectors.toList());
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
      List<Dataset> datasets = datasetDAO.getActiveDatasets();
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
}
