package org.broadinstitute.consent.http.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.DataSetAudit;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetAuditDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Association;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetAuditProperty;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation class for DataSetAPI database support.
 */
public class DatabaseDataSetAPI extends AbstractDataSetAPI {

    private final DataSetFileParser parser = new DataSetFileParser();
    private final DataSetDAO dsDAO;
    private final DataSetAssociationDAO dataSetAssociationDAO;
    private final UserRoleDAO userRoleDAO;
    private final ConsentDAO consentDAO;
    private DataAccessRequestAPI accessAPI;
    private DataSetAuditDAO dataSetAuditDAO;
    private ElectionDAO electionDAO;
    private final String MISSING_ASSOCIATION = "Sample Collection ID %s doesn't have an associated consent.";
    private final String MISSING_CONSENT = "Consent ID %s does not exist.";
    private final String DUPLICATED_ROW = "Sample Collection ID %s is already present in the database. ";
    private final String DUPLICATED_NAME_ROW = "Dataset Name %s is already present in the database. ";
    private final String OVERWRITE_ON = "If you wish to overwrite DataSet values, you can turn OVERWRITE mode ON.";
    private final String DATASETID_PROPERTY_NAME = "Sample Collection ID";
    private final String CONFLICT_IDS = "Conflict in dataset association identificator,  %s - %s, use either Sample Collection ID or Consent ID";
    private final String CREATE = "CREATE";
    private final String UPDATE = "UPDATE";
    private final String DELETE = "DELETE";
    private final  List<String> predefinedDatasets;
    private final Object aliasDBValueLock = new Object();

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    public static void initInstance(DataSetDAO dsDAO, DataSetAssociationDAO dataSetAssociationDAO, UserRoleDAO userRoleDAO, ConsentDAO consentDAO, DataSetAuditDAO dataSetAuditDAO, ElectionDAO electionDAO, List<String> predefinedDatasets) {
        DataSetAPIHolder.setInstance(new DatabaseDataSetAPI(dsDAO, dataSetAssociationDAO, userRoleDAO, consentDAO, dataSetAuditDAO, electionDAO, predefinedDatasets));
    }

    private DatabaseDataSetAPI(DataSetDAO dsDAO, DataSetAssociationDAO dataSetAssociationDAO, UserRoleDAO userRoleDAO, ConsentDAO consentDAO, DataSetAuditDAO dataSetAuditDAO, ElectionDAO electionDAO, List<String> predefinedDatasets) {
        this.dsDAO = dsDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.userRoleDAO = userRoleDAO;
        this.consentDAO = consentDAO;
        this.accessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.dataSetAuditDAO = dataSetAuditDAO;
        this.electionDAO = electionDAO;
        this.predefinedDatasets = predefinedDatasets;
    }


    @Override
    public ParseResult create(File dataSetFile, Integer userId) {
        synchronized (aliasDBValueLock) {
            Integer lastAlias = dsDAO.findLastAlias();
            ParseResult result = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFieldsOrderByReceiveOrder(), lastAlias, false, predefinedDatasets);
            List<DataSet> dataSets = result.getDatasets();
            if (CollectionUtils.isNotEmpty(dataSets)) {
                if (isValid(dataSets, false)) {

                    List<DataSet> existentdataSets = dsDAO.getDataSetsForObjectIdList(dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList()));

                    List<DataSet> dataSetsToUpdate = new ArrayList<>();
                    List<DataSet> dataSetsToCreate = new ArrayList<>();
                    List<String> existentObjectIds = existentdataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());

                    for (DataSet ds : dataSets) {
                        if (StringUtils.isNotEmpty(ds.getObjectId()) && existentObjectIds.contains(ds.getObjectId())) {
                            dataSetsToUpdate.add(ds);
                        } else {
                            dataSetsToCreate.add(ds);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(dataSetsToCreate)) {
                        dsDAO.insertAll(dataSetsToCreate);
                        insertDataSetAudit(dataSetsToCreate, CREATE, userId, insertProperties(dataSetsToCreate));
                    }
                    if (CollectionUtils.isNotEmpty(dataSetsToUpdate)) {
                        dsDAO.updateAllByObjectId(dataSetsToUpdate);
                        List<String> objectIds = dataSetsToUpdate.stream().map(DataSet::getObjectId).collect(Collectors.toList());
                        List<Integer> dataSetIds = dsDAO.searchDataSetsIdsByObjectIdList(objectIds);
                        if (CollectionUtils.isNotEmpty(dataSetIds))
                            dsDAO.deleteDataSetsProperties(dataSetIds);
                        insertDataSetAudit(dataSetsToUpdate, UPDATE, userId, insertProperties(dataSetsToUpdate));
                    }
                    processAssociation(dataSets);
                } else {
                    result.getErrors().addAll(addIdsErrors(dataSets));
                    result.getErrors().addAll(addMissingAssociationsErrors(dataSets));
                    result.getErrors().addAll(addMissingConsentIdErrors(dataSets));
                    result.getErrors().addAll(addDuplicatedRowsErrors(dataSets));
                    result.getErrors().addAll(addDuplicateDataSetNames(dataSets));
                }
            }
            return result;
        }
    }


    @Override
    public ParseResult overwrite(File dataSetFile, Integer userId) {
        // this code does not need to be synchronized for the alias value because it does not
        // retrieve the last alias from the DB. in processDataSets, there are synchronized blocks
        // that handle the synchronization when the alias value is retrieved and used
        ParseResult result = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFieldsOrderByReceiveOrder(), null, true, predefinedDatasets);
        List<DataSet> dataSets = result.getDatasets();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            List<String> nameList = dataSets.stream().map(DataSet::getName).collect(Collectors.toList());
            List<String> objectIdList = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
            List<DataSet> existentDataSets = dsDAO.searchDataSetsByNameList(nameList);
            if (CollectionUtils.isNotEmpty(objectIdList)) {
                existentDataSets.addAll(dsDAO.searchDataSetsByObjectIdList(objectIdList));
            }
            Map<String, DataSet> dataSetMap = new HashMap<>();

            existentDataSets.stream().forEach(dataSet -> {
                if (StringUtils.isNotEmpty(dataSet.getObjectId())) {
                    dataSetMap.put(dataSet.getObjectId(), dataSet);
                } else {
                    dataSetMap.put(dataSet.getName(), dataSet);
                }

            });

            List<Integer> existentIdList = existentDataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
            if (isValid(dataSets, true)) {
                dsDAO.deleteDataSetsProperties(existentIdList);
                processDataSets(dataSets, dataSetMap, userId);
                processAssociation(dataSets);

            }
        }
        result.getErrors().addAll(addIdsErrors(dataSets));
        result.getErrors().addAll(addMissingAssociationsErrors(dataSets));
        return result;
    }

    @Override
    public List<DataSet> getDataSetsForConsent(String consentId) {
        return dsDAO.getDataSetsForConsent(consentId);
    }

    @Override
    public Collection<DataSetDTO> describeDataSetsByReceiveOrder(List<Integer> dataSetId) {
        return dsDAO.findDataSetsByReceiveOrder(dataSetId);
    }

    @Override
    public Collection<Dictionary> describeDictionaryByDisplayOrder() {
        return dsDAO.getMappedFieldsOrderByDisplayOrder();
    }

    @Override
    public Collection<Dictionary> describeDictionaryByReceiveOrder() {
        return dsDAO.getMappedFieldsOrderByReceiveOrder();
    }

    @Override
    public void deleteDataset(Integer dataSetId, Integer dacUserId) throws IllegalStateException {
        try {
            dsDAO.begin();
            dataSetAuditDAO.begin();
            DataSet dataset = dsDAO.findDataSetById(dataSetId);
            Collection<Integer> dataSetsId = Collections.singletonList(dataset.getDataSetId());
            if (checkDatasetExistence(dataset.getDataSetId())) {
                DataSetAudit dsAudit = new DataSetAudit(dataset.getDataSetId(), dataset.getObjectId(), dataset.getName(), new Date(), true, dacUserId, DELETE);
                dataSetAuditDAO.insertDataSetAudit(dsAudit);
            }
            dataSetAssociationDAO.delete(dataset.getDataSetId());
            dsDAO.deleteDataSetsProperties(dataSetsId);

            if (StringUtils.isNotEmpty(dataset.getObjectId())) {
                dsDAO.logicalDatasetDelete(dataset.getDataSetId());
            } else {
                consentDAO.deleteAssociationsByDataSetId(dataset.getDataSetId());
                dsDAO.deleteDataSets(dataSetsId);
            }

            dsDAO.commit();
            dataSetAuditDAO.commit();
        } catch (Exception e) {
            dsDAO.rollback();
            dataSetAuditDAO.rollback();
            throw new IllegalStateException(e.getMessage());
        }
    }

    private boolean checkDatasetExistence(Integer dataSetId) {
        return dsDAO.findDataSetById(dataSetId) != null ? true : false;
    }

    @Override
    public void disableDataset(Integer datasetId, Boolean active) {
        DataSet dataset = dsDAO.findDataSetById(datasetId);
        if (dataset != null) {
            dsDAO.updateDataSetActive(dataset.getDataSetId(), active);
        }
    }

    @Override
    public DataSet updateNeedsReviewDataSets(Integer dataSetId, Boolean needsApproval) {
        if (dsDAO.findDataSetById(dataSetId) == null) {
            throw new NotFoundException("DataSet doesn't exist");
        }
        dsDAO.updateDataSetNeedsApproval(dataSetId, needsApproval);
        return dsDAO.findDataSetById(dataSetId);
    }

    @Override
    public List<DataSet> findNeedsApprovalDataSetByObjectId(List<Integer> dataSetIdList) {
        return dsDAO.findNeedsApprovalDataSetByDataSetId(dataSetIdList);
    }

    private List<String> addMissingAssociationsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
        List<String> objectIdList = dataSets.stream().filter(dataset -> dataset.getObjectId() != null).map(DataSet::getObjectId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(objectIdList)) {
            List<Association> presentAssociations = dsDAO.getAssociationsForObjectIdList(objectIdList);
            List<String> associationIdList = presentAssociations.stream().map(Association::getObjectId).collect(Collectors.toList());
            errors.addAll(objectIdList.stream().filter(dsId -> (!(associationIdList.contains(dsId)))).map(dsId -> String.format(MISSING_ASSOCIATION, dsId)).collect(Collectors.toList()));

        }
        return errors;
    }


    private List<String> addIdsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
          for(DataSet ds : dataSets) {
              if(StringUtils.isNotEmpty(ds.getObjectId()) && StringUtils.isNotEmpty(ds.getConsentName())) {
                  errors.add(String.format(CONFLICT_IDS, ds.getObjectId(), ds.getConsentName()));
              }
          }
        return errors;
    }

    private List<String> addMissingConsentIdErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
        List<String> consentNames = dataSets.stream().filter(ds -> StringUtils.isNotEmpty(ds.getConsentName())).map(DataSet::getConsentName).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(consentNames)) {
            List<Consent> existentConsents = consentDAO.findConsentsFromConsentNames(consentNames);
            List<String> existentConsentNames = existentConsents.stream().map(n -> n.getName()).collect(Collectors.toList());
            errors.addAll(consentNames.stream().filter(consent -> (!(existentConsentNames.contains(consent)))).map(name -> String.format(MISSING_CONSENT, name)).collect(Collectors.toList()));
        }
        return errors;
    }

    private List<String> addDuplicatedRowsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
        List<String> objectIds = dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList());
        List<DataSet> failingRows = CollectionUtils.isNotEmpty(objectIds) ? dsDAO.getDataSetsWithValidNameForObjectIdList(objectIds) : new ArrayList<>();
        errors.addAll(failingRows.stream().filter(ds -> !ds.getObjectId().isEmpty()).map(ds -> String.format(DUPLICATED_ROW, ds.getObjectId())).collect(Collectors.toList()));
        if (CollectionUtils.isNotEmpty(errors)) errors.add(OVERWRITE_ON);
        return errors;
    }

    private List<String> addDuplicateDataSetNames(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            List<DataSet> failingRows = dsDAO.getDataSetsForNameList(dataSets.stream().map(d -> d.getName()).collect(Collectors.toList()));
            errors.addAll(failingRows.stream().map(ds -> String.format(DUPLICATED_NAME_ROW, ds.getName())).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(errors)) errors.add(OVERWRITE_ON);
        }
        return errors;
    }

    private boolean isValid(List<DataSet> dataSets, boolean overwrite) {
        boolean isValid = true;
        for (DataSet dataSet : dataSets) {
            // duplicated dataset
            DataSet existentDataset = StringUtils.isNotEmpty(dataSet.getObjectId()) ? dsDAO.findDataSetByObjectId(dataSet.getObjectId()) : null;
            if ( StringUtils.isNotEmpty(dataSet.getConsentName()) && StringUtils.isNotEmpty(dataSet.getObjectId())
                    || !overwrite && existentDataset != null && StringUtils.isNotEmpty(existentDataset.getName()) && dsDAO.getConsentAssociationByObjectId(dataSet.getObjectId()) != null
                    // missing association if object id is present
                    || StringUtils.isNotEmpty(dataSet.getObjectId()) && CollectionUtils.isEmpty(dsDAO.getAssociationsForObjectIdList(Arrays.asList(dataSet.getObjectId())))
                    // missing consent if consent id is present
                    || StringUtils.isNotEmpty(dataSet.getConsentName()) && consentDAO.getIdByName(dataSet.getConsentName()) == null
                    // dataset name should be unique
                    || !overwrite && dsDAO.getDatasetIdByName(dataSet.getName()) != null) {
                isValid = false;
                break;
            }
        }
        return isValid;
    }


    private void processAssociation(List<DataSet> dataSetList) {
        dataSetList.forEach(dataSet -> {
            if (StringUtils.isNotEmpty(dataSet.getConsentName())) {
                Integer datasetId = dsDAO.getDatasetIdByName(dataSet.getName());
                if (consentDAO.findAssociationsByDataSetId(datasetId) == null) {
                    consentDAO.insertConsentAssociation(consentDAO.getIdByName(dataSet.getConsentName()), AssociationType.SAMPLESET.getValue(), datasetId);
                }
            }
        });
    }

    private List<DataSetProperty> insertProperties(List<DataSet> dataSets) {
        List<String> nameList = dataSets.stream().map(DataSet::getName).collect(Collectors.toList());
        List<Map<String, Integer>> retrievedValues = dsDAO.searchByNameIdList(nameList);
        Map<String, Integer> retrievedValuesMap = getOneMap(retrievedValues);
        List<DataSetProperty> dataSetPropertiesList = new ArrayList<>();
        dataSets.stream().map((dataSet) -> {
            Set<DataSetProperty> properties = dataSet.getProperties();
            properties.forEach((property) -> {
                property.setDataSetId(retrievedValuesMap.get(dataSet.getName()));
            });
            return dataSet;
        }).forEach((dataSet) -> {
            dataSetPropertiesList.addAll(dataSet.getProperties());
        });
        dsDAO.insertDataSetsProperties(dataSetPropertiesList);
        return dataSetPropertiesList;
    }


    /**
     * This method takes a List<Map<objectId, datasetId>> and returns a merged
     * Map<objectId, datasetIs>.
     * <p/>
     * Due to database constraints, like objectId UNIQUE, each Map in List has
     * only one element. This method will NOT work properly if Maps in the List
     * contains duplicated keys.
     *
     * @param retrievedValues a List<Map<String, Integer>> produced by
     *                        DataSetDAO.
     * @return a single, merged, Map<String, Integer>
     * @see
     */
    private Map<String, Integer> getOneMap(List<Map<String, Integer>> retrievedValues) {
        Map<String, Integer> newMap = new HashMap<>();
        retrievedValues.stream().forEach((map) -> {
            map.forEach((key, value) -> {
                newMap.put(key, value);
            });
        });
        return newMap;
    }


    private boolean userIs(String roleName, Integer dacUserId) {
        return userRoleDAO.findRoleByNameAndUser(roleName, dacUserId) != null;
    }


    private void setConsentNameDTOList(Collection<DataSetDTO> dataSetDTOList) {
        if (CollectionUtils.isNotEmpty(dataSetDTOList)) {
            List<String> consentIds = dataSetDTOList.stream().map(sc -> sc.getConsentId()).collect(Collectors.toList());
            Collection<Consent> consents = consentDAO.findConsentsFromConsentsIDs(consentIds);
            consents.forEach(consent -> {
                List<DataSetDTO> dsList = dataSetDTOList.stream().filter(d -> d.getConsentId().equals(consent.getConsentId())).
                        collect(Collectors.toList());
                dsList.stream().forEach(ds -> {
                    ds.setConsentId(consent.getName());
                });

            });
        }

    }

    private void processDataSets(List<DataSet> dataSets, Map<String, DataSet> existentDataSets, Integer userId) {
        List<DataSet> dataSetToCreate = new ArrayList<>();
        List<DataSet> dataSetToUpdate = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataSets) && MapUtils.isNotEmpty(existentDataSets)) {
            dataSets.stream().forEach(newDataSet -> {
                if (existentDataSets.containsKey(newDataSet.getName()) || existentDataSets.containsKey(newDataSet.getObjectId())) {
                    DataSet existentDataSet = existentDataSets.containsKey(newDataSet.getName()) ? existentDataSets.get(newDataSet.getName()) : existentDataSets.get(newDataSet.getObjectId());
                    newDataSet.setDataSetId(existentDataSet.getDataSetId());
                    if(existentDataSet.getAlias() != null && existentDataSet.getAlias() != 0) {
                        newDataSet.setAlias(existentDataSet.getAlias());
                    }
                    dataSetToUpdate.add(newDataSet);
                } else {
                    dataSetToCreate.add(newDataSet);
                }
            });
        } else {
            dataSetToCreate.addAll(dataSets);
        }
        synchronized (aliasDBValueLock) {
            List<DataSet> dataSetToCreateWithAlias = parser.createAlias(dataSetToCreate, dsDAO.findLastAlias(), predefinedDatasets);
            if (CollectionUtils.isNotEmpty(dataSetToCreateWithAlias)) {
                dsDAO.insertAll(dataSetToCreateWithAlias);
                List<DataSetProperty> properties = insertProperties(dataSetToCreateWithAlias);
                insertDataSetAudit(dataSetToCreateWithAlias, CREATE, userId, properties);
            }
        }
        synchronized (aliasDBValueLock) {
            List<DataSet> dataSetToUpdateWithAlias = parser.createAlias(dataSetToUpdate, dsDAO.findLastAlias(), predefinedDatasets);
            if (CollectionUtils.isNotEmpty(dataSetToUpdateWithAlias)) {
                dsDAO.updateAll(dataSetToUpdateWithAlias);
                List<DataSetProperty> properties = insertProperties(dataSetToUpdateWithAlias);
                insertDataSetAudit(dataSetToUpdateWithAlias, UPDATE, userId, properties);
            }
        }
    }


    private void insertDataSetAudit(List<DataSet> dataSets, String action, Integer userId, List<DataSetProperty> properties) {
        Map<String, DataSet> dataSetObjectIdMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            dataSets.stream().forEach(dataSet -> {
                dataSetObjectIdMap.put(dataSet.getName(), dataSet);
            });
            List<DataSet> existentDataSets = dsDAO.getDataSetsForNameList(new ArrayList(dataSetObjectIdMap.keySet()));
            createDataSetAudit(existentDataSets, userId, action, properties);
        }
    }


    private void createDataSetAudit(List<DataSet> dataSetList, Integer userId, String action, List<DataSetProperty> properties) {
        if (CollectionUtils.isNotEmpty(dataSetList)) {
            Map<Integer, List<DataSetProperty>> dataSetPropertyMap = new HashMap<>();
            properties.stream().forEach(property -> {
                if (dataSetPropertyMap.containsKey(property.getDataSetId())) {
                    dataSetPropertyMap.get(property.getDataSetId()).add(property);
                } else {
                    dataSetPropertyMap.put(property.getDataSetId(), new ArrayList<>(Arrays.asList(property)));
                }

            });
            dataSetList.stream().forEach(dataSet -> {
                DataSetAudit dataSetAudit = new DataSetAudit(dataSet.getDataSetId(), dataSet.getObjectId(), dataSet.getName(), dataSet.getCreateDate(), dataSet.getActive(), userId, action);
                Integer dataSetAuditId = dataSetAuditDAO.insertDataSetAudit(dataSetAudit);
                dataSetAuditDAO.insertDataSetAuditProperties(createDataSetAuditProperties(dataSet, dataSetAuditId, dataSetPropertyMap));
            });
        }

    }

    private List<DataSetAuditProperty> createDataSetAuditProperties(DataSet dataSet, Integer dataSetAuditId, Map<Integer, List<DataSetProperty>> propertiesMap) {
        List<DataSetAuditProperty> auditProperties = new ArrayList<>();
        List<DataSetProperty> properties = propertiesMap.get(dataSet.getDataSetId());
        properties.stream().forEach(property -> {
            auditProperties.add(new DataSetAuditProperty(
                    property.getPropertyId(),
                    property.getDataSetId(),
                    property.getPropertyKey(),
                    property.getPropertyValue(),
                    property.getCreateDate(),
                    dataSetAuditId));
        });
        return auditProperties;
    }
}
