package org.broadinstitute.consent.http.service;

import java.util.ArrayList;
import java.util.Collections;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

public class DatasetService {

    public static final String DATASET_NAME_KEY = "Dataset Name";
    public static final String CONSENT_NAME_PREFIX = "DUOS-DS-CG-";
    private final ConsentDAO consentDAO;
    private final DataSetDAO dataSetDAO;
    private final UserRoleDAO userRoleDAO;
    private final UseRestrictionConverter converter;
    public static String datasetName = "Dataset Name";

    @Inject
    public DatasetService(ConsentDAO consentDAO, DataSetDAO dataSetDAO, UserRoleDAO userRoleDAO,
          UseRestrictionConverter converter) {
        this.consentDAO = consentDAO;
        this.dataSetDAO = dataSetDAO;
        this.userRoleDAO = userRoleDAO;
        this.converter = converter;
    }

    /**
     * Create a minimal consent from the data provided in a Dataset.
     *
     * @param dataset The DataSetDTO
     * @return The created Consent
     */
    public Consent createConsentForDataset(DataSetDTO dataset) {
        String consentId = UUID.randomUUID().toString();
        Optional<DataSetPropertyDTO> nameProp = dataset.getProperties()
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
             * use restriction
             * data user letter
             * data user letter name
             * translated use restriction
             */
            UseRestriction useRestriction = converter.parseUseRestriction(dataset.getDataUse());
            consentDAO.insertConsent(consentId, manualReview, useRestriction.toString(),
                  dataset.getDataUse().toString(),
                  null, name, null, createDate, createDate, null,
                  true, groupName, dataset.getDacId());
            String associationType = AssociationType.SAMPLESET.getValue();
            consentDAO.insertConsentAssociation(consentId, associationType, dataset.getDataSetId());
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

    public DataSetDTO createDataset(DataSetDTO dataset, String name, Integer userId) {
        Timestamp now = new Timestamp(new Date().getTime());
        int lastAlias = dataSetDAO.findLastAlias();
        int alias = lastAlias + 1;

        Integer id = dataSetDAO
              .insertDatasetV2(name, now, userId, dataset.getObjectId(), dataset.getActive(),
                    alias);

        List<DataSetProperty> propertyList = processDatasetProperties(id, dataset.getProperties());
        dataSetDAO.insertDataSetsProperties(propertyList);
        dataSetDAO.updateDataSetNeedsApproval(id, dataset.getNeedsApproval());
        return getDatasetDTO(id);
    }

    public DataSet getDatasetByName(String name) {
        return dataSetDAO.getDatasetByName(name);
    }

    public DataSet findDatasetById(Integer id) {
        return dataSetDAO.findDataSetById(id);
    }

    public Set<DataSetProperty> getDatasetProperties(Integer datasetId) {
        return dataSetDAO.findDatasetPropertiesByDatasetId(datasetId);
    }

    public DataSet getDatasetWithPropertiesById(Integer datasetId) {
        DataSet dataset = dataSetDAO.findDataSetById(datasetId);
        Set<DataSetProperty> properties = getDatasetProperties(datasetId);
        dataset.setProperties(properties);
        return dataset;
    }

    public Optional<DataSet> updateDataset(DataSetDTO dataset, Integer datasetId, Integer userId) {
        Timestamp now = new Timestamp(new Date().getTime());

        DataSet old = getDatasetWithPropertiesById(datasetId);
        Set<DataSetProperty> oldProperties = old.getProperties();

        List<DataSetPropertyDTO> updateDatasetPropertyDTOs = dataset.getProperties();
        List<DataSetProperty> updateDatasetProperties = processDatasetProperties(datasetId,
              updateDatasetPropertyDTOs);

        List<DataSetProperty> propertiesToAdd = updateDatasetProperties.stream()
              .filter(p -> oldProperties.stream()
                    .noneMatch(op -> op.getPropertyKey() == p.getPropertyKey()))
              .collect(Collectors.toList());

        List<DataSetProperty> propertiesToUpdate = updateDatasetProperties.stream()
              .filter(p -> oldProperties.stream()
                    .noneMatch(op -> p.equals(op)))
              .collect(Collectors.toList());

        List<DataSetProperty> propertiesToDelete = oldProperties.stream()
              .filter(op -> updateDatasetProperties.stream()
                    .noneMatch(p -> p.getPropertyKey() == op.getPropertyKey())
              ).collect(Collectors.toList());

        if (propertiesToAdd.isEmpty() && propertiesToUpdate.isEmpty() && propertiesToDelete
              .isEmpty()) {
            return Optional.empty();
        }

        updateDatasetProperties(propertiesToUpdate, propertiesToDelete, propertiesToAdd);
        dataSetDAO.updateDataSetNeedsApproval(datasetId, dataset.getNeedsApproval());
        dataSetDAO.updateDatasetUpdateUserAndDate(datasetId, now, userId);
        DataSet updatedDataset = getDatasetWithPropertiesById(datasetId);
        return Optional.of(updatedDataset);
    }

    private void updateDatasetProperties(List<DataSetProperty> updateProperties,
          List<DataSetProperty> deleteProperties, List<DataSetProperty> addProperties) {
        updateProperties.forEach(p -> dataSetDAO
              .updateDatasetProperty(p.getDataSetId(), p.getPropertyKey(), p.getPropertyValue()));
        deleteProperties.forEach(
              p -> dataSetDAO.deleteDatasetPropertyByKey(p.getDataSetId(), p.getPropertyKey()));
        dataSetDAO.insertDataSetsProperties(addProperties);
    }

    public DataSetDTO getDatasetDTO(Integer datasetId) {
        Set<DataSetDTO> dataset = dataSetDAO.findDatasetDTOWithPropertiesByDatasetId(datasetId);
        DataSetDTO result = new DataSetDTO();
        for (DataSetDTO d : dataset) {
            result = d;
        }
        return result;
    }


    public List<DataSetProperty> processDatasetProperties(Integer datasetId,
          List<DataSetPropertyDTO> properties) {
        Date now = new Date();
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey)
              .collect(Collectors.toList());

        return properties.stream()
              .filter(p -> keys.contains(p.getPropertyName()) && !p.getPropertyName()
                    .equals(datasetName))
              .map(p ->
                    new DataSetProperty(datasetId,
                          dictionaries.get(keys.indexOf(p.getPropertyName())).getKeyId(),
                          p.getPropertyValue(), now)
              )
              .collect(Collectors.toList());
    }

    public List<DataSetPropertyDTO> findInvalidProperties(List<DataSetPropertyDTO> properties) {
        List<Dictionary> dictionaries = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        List<String> keys = dictionaries.stream().map(Dictionary::getKey)
              .collect(Collectors.toList());

        return properties.stream()
              .filter(p -> !keys.contains(p.getPropertyName()))
              .collect(Collectors.toList());
    }

    public List<DataSetPropertyDTO> findDuplicateProperties(List<DataSetPropertyDTO> properties) {
        Set<String> uniqueKeys = properties.stream()
              .map(DataSetPropertyDTO::getPropertyName)
              .collect(Collectors.toSet());
        if (uniqueKeys.size() != properties.size()) {
            List<DataSetPropertyDTO> allDuplicateProperties = new ArrayList<>();
            uniqueKeys.forEach(key -> {
                List<DataSetPropertyDTO> propertiesPerKey = properties.stream()
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

    public void deleteDataset(Integer datasetId) {
        List<Integer> idList = Collections.singletonList(datasetId);
        dataSetDAO.deleteDataSetsProperties(idList);
        dataSetDAO.deleteDataSets(idList);
    }

    public List<Map<String, String>> autoCompleteDatasets(String partial, Integer dacUserId) {
        Set<DataSetDTO> datasets;
        if (userHasRole(UserRoles.ADMIN.getRoleName(), dacUserId)) {
            datasets = dataSetDAO.findAllDatasets();
        }
        else if (userHasRole(UserRoles.CHAIRPERSON.getRoleName(), dacUserId)) {
            datasets = dataSetDAO.findDatasetsByUser(dacUserId);
        } else {
            datasets = getAllActiveDatasets();
        }
        String lowercasePartial = partial.toLowerCase();
        Set<DataSetDTO> filteredDatasetsContainingPartial = datasets.stream().filter(ds ->
              (ds.getProperties().stream()
                    .anyMatch(
                          p -> p.getPropertyName().equalsIgnoreCase("Principal Investigator(PI)"))
                    &&
                    ds.getConsentId().toLowerCase().contains(lowercasePartial) ||
                    ds.getProperties().stream()
                          .anyMatch(
                                p -> p.getPropertyValue().toLowerCase().contains(lowercasePartial))
              )).collect(Collectors.toSet());
        List<Map<String, String>> result = filteredDatasetsContainingPartial.stream().map(ds ->
              {
                  HashMap<String, String> map = new HashMap<>();
                  List<DataSetPropertyDTO> properties = ds.getProperties();
                  Optional<DataSetPropertyDTO> datasetName = properties.stream()
                        .filter(p -> p.getPropertyName().equalsIgnoreCase("Dataset Name")).findFirst();
                  Optional<DataSetPropertyDTO> pi = properties.stream()
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
        return result;
    }

    public Set<DataSetDTO> getAllActiveDatasets() {
        return dataSetDAO.findActiveDatasets();
    }

    private boolean userHasRole(String roleName, Integer dacUserId) {
        return userRoleDAO.findRoleByNameAndUser(roleName, dacUserId) != null;
    }
}
