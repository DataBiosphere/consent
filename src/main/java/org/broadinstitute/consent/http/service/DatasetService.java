package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import java.sql.Timestamp;
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

public class DatasetService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String DATASET_NAME_KEY = "Dataset Name";
    public static final String CONSENT_NAME_PREFIX = "DUOS-DS-CG-";
    private final ConsentDAO consentDAO;
    private final DataAccessRequestDAO dataAccessRequestDAO;
    private final DatasetDAO datasetDAO;
    private final UserRoleDAO userRoleDAO;
    private final UseRestrictionConverter converter;

    @Inject
    public DatasetService(ConsentDAO consentDAO, DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO dataSetDAO,
                          UserRoleDAO userRoleDAO, UseRestrictionConverter converter) {
        this.consentDAO = consentDAO;
        this.dataAccessRequestDAO = dataAccessRequestDAO;
        this.datasetDAO = dataSetDAO;
        this.userRoleDAO = userRoleDAO;
        this.converter = converter;
    }

    public List<Dataset> getDataSetsForConsent(String consentId) {
        return datasetDAO.getDatasetsForConsent(consentId);
    }

    public Collection<DatasetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetId) {
        return datasetDAO.findDatasetsByReceiveOrder(dataSetId);
    }

    public Collection<Dictionary> describeDictionaryByDisplayOrder() {
        return datasetDAO.getMappedFieldsOrderByDisplayOrder();
    }

    public Collection<Dictionary> describeDictionaryByReceiveOrder() {
        return datasetDAO.getMappedFieldsOrderByReceiveOrder();
    }

    public void disableDataset(Integer datasetId, Boolean active) {
        Dataset dataset = datasetDAO.findDatasetById(datasetId);
        if (dataset != null) {
            datasetDAO.updateDatasetActive(dataset.getDataSetId(), active);
        }
    }

    public Dataset updateNeedsReviewDataSets(Integer dataSetId, Boolean needsApproval) {
        if (datasetDAO.findDatasetById(dataSetId) == null) {
            throw new NotFoundException("DataSet doesn't exist");
        }
        datasetDAO.updateDatasetNeedsApproval(dataSetId, needsApproval);
        return datasetDAO.findDatasetById(dataSetId);
    }

    public List<Dataset> findNeedsApprovalDataSetByObjectId(List<Integer> dataSetIdList) {
        return datasetDAO.findNeedsApprovalDatasetByDatasetId(dataSetIdList);
    }

    public Set<DatasetDTO> findDatasetsByDacIds(List<Integer> dacIds) {
        if(Objects.isNull(dacIds) || dacIds.isEmpty()) {
            throw new BadRequestException("No dataset IDs provided");
        }
        return datasetDAO.findDatasetsByDacIds(dacIds);
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
        String groupName = nameProp.isPresent() ? nameProp.get().getPropertyValue() : dataset.getAlias();
        String name = CONSENT_NAME_PREFIX + dataset.getDataSetId();
        Date createDate = new Date();
        if (Objects.nonNull(dataset.getDataUse())) {
            boolean manualReview = isConsentDataUseManualReview(dataset.getDataUse());
            /*
             * Consents created for a dataset do not need the following properties:
             * data user letter
             * data user letter name
             */
            UseRestriction useRestriction = converter.parseUseRestriction(dataset.getDataUse());
            String translatedUseRestriction = converter.translateDataUse(dataset.getDataUse(), DataUseTranslationType.DATASET);
            consentDAO.useTransaction(h -> {
                try {
                    h.insertConsent(consentId, manualReview, useRestriction.toString(), dataset.getDataUse().toString(), null, name, null, createDate, createDate, translatedUseRestriction, groupName, dataset.getDacId());
                    if (Objects.nonNull(dataset.getDacId())) {
                        h.updateConsentDac(consentId, dataset.getDacId());
                    }
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
            throw new IllegalArgumentException("Dataset is missing Data Use information. Consent could not be created.");
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

    public DatasetDTO createDatasetWithConsent(DatasetDTO dataset, String name, Integer userId) throws Exception {
        if (Objects.nonNull(getDatasetByName(name))) {
            throw new IllegalArgumentException("Dataset name: " + name + " is already in use");
        }
        Timestamp now = new Timestamp(new Date().getTime());
        Integer createdDatasetId = datasetDAO.inTransaction(h -> {
            try {
                Integer id = h.insertDatasetV2(name, now, userId, dataset.getObjectId(), dataset.getActive());
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

    public Dataset findDatasetById(Integer id) {
        return datasetDAO.findDatasetById(id);
    }

    public Set<Dataset> getDatasetWithDataUseByIds(List<Integer> datasetIds) {
        return datasetDAO.findDatasetWithDataUseByIdList(datasetIds);
    }

    public Set<DatasetProperty> getDatasetProperties(Integer datasetId) {
        return datasetDAO.findDatasetPropertiesByDatasetId(datasetId);
    }

    public Dataset getDatasetWithPropertiesById(Integer datasetId) {
        Dataset dataset = datasetDAO.findDatasetById(datasetId);
        Set<DatasetProperty> properties = getDatasetProperties(datasetId);
        dataset.setProperties(properties);
        return dataset;
    }

    public Optional<Dataset> updateDataset(DatasetDTO dataset, Integer datasetId, Integer userId) {
        Timestamp now = new Timestamp(new Date().getTime());

        if (Objects.isNull(dataset.getDatasetName())) {
            throw new IllegalArgumentException("Dataset 'Name' cannot be null");
        }
        if (Objects.isNull(dataset.getNeedsApproval())) {
            throw new IllegalArgumentException("Dataset 'Needs Approval' field cannot be null");
        }

        if (Objects.nonNull(dataset.getDacId())) {
            Consent consent = consentDAO.findConsentFromDatasetID(datasetId);
            if (Objects.nonNull(consent)) {
                consentDAO.updateConsentDac(consent.getConsentId(), dataset.getDacId());
            }
        }

        Dataset old = getDatasetWithPropertiesById(datasetId);
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
              .isEmpty()) {
            return Optional.empty();
        }

        updateDatasetProperties(propertiesToUpdate, propertiesToDelete, propertiesToAdd);
        datasetDAO.updateDataset(datasetId, dataset.getDatasetName(), now, userId, dataset.getNeedsApproval());
        Dataset updatedDataset = getDatasetWithPropertiesById(datasetId);
        return Optional.of(updatedDataset);
    }

    private void updateDatasetProperties(List<DatasetProperty> updateProperties,
                                         List<DatasetProperty> deleteProperties, List<DatasetProperty> addProperties) {
        updateProperties.forEach(p -> datasetDAO
              .updateDatasetProperty(p.getDataSetId(), p.getPropertyKey(), p.getPropertyValue()));
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


    public List<DatasetProperty> processDatasetProperties(Integer datasetId,
                                                          List<DatasetPropertyDTO> properties) {
        Date now = new Date();
        List<Dictionary> dictionaries = datasetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey)
              .collect(Collectors.toList());

        return properties.stream()
              .filter(p -> keys.contains(p.getPropertyName()))
              .map(p ->
                    new DatasetProperty(datasetId,
                          dictionaries.get(keys.indexOf(p.getPropertyName())).getKeyId(),
                          p.getPropertyValue(), now)
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
            String dsAuditName = Objects.nonNull(dataset.getName()) ? dataset.getName() : dataset.getDatasetIdentifier();
            DatasetAudit dsAudit = new DatasetAudit(datasetId, dataset.getObjectId(), dsAuditName, new Date(), dataset.getActive(), userId, AuditActions.DELETE.getValue().toUpperCase());
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
        List<DataAccessRequestData> darDatas = dataAccessRequestDAO.findAllDataAccessRequestDatas();
        List<Integer> datasetIdsInUse = darDatas
                .stream()
                .map(DataAccessRequestData::getDatasetIds)
                .filter(Objects::nonNull)
                .filter(l -> !l.isEmpty())
                .flatMap(List::stream)
                .collect(Collectors.toList());
        HashSet<DatasetDTO> datasets = new HashSet<>();
        if (userHasRole(UserRoles.ADMIN.getRoleName(), userId)) {
            datasets.addAll(datasetDAO.findAllDatasets());
        } else {
            datasets.addAll(datasetDAO.findActiveDatasets());
            if (userHasRole(UserRoles.CHAIRPERSON.getRoleName(), userId)) {
                Collection<DatasetDTO> chairSpecificDatasets = datasetDAO.findDatasetsByUserId(userId);
                datasets.addAll(chairSpecificDatasets);
            }
        }
        datasets.forEach(d -> d.setDeletable(!datasetIdsInUse.contains(d.getDataSetId())));
        return datasets;
    }

    public List<Map<String, String>> autoCompleteDatasets(String partial, Integer dacUserId) {
        Set<DatasetDTO> datasets = describeDatasets(dacUserId);
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

    private boolean userHasRole(String roleName, Integer dacUserId) {
        return userRoleDAO.findRoleByNameAndUser(roleName, dacUserId) != null;
    }

    public List<Dataset> findAllDatasetsByUser(User user) {
        if (user.hasUserRole(UserRoles.ADMIN)) {
            return datasetDAO.getAllDatasets();
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
}
