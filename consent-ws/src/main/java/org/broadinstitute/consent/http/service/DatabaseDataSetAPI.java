package org.broadinstitute.consent.http.service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.broadinstitute.consent.http.DataSetAudit;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.*;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.broadinstitute.consent.http.util.DarConstants;

import org.bson.Document;
/**
 * Implementation class for DataSetAPI database support.
 */
public class DatabaseDataSetAPI extends AbstractDataSetAPI {

    private final DataSetFileParser parser = new DataSetFileParser();
    private final DataSetDAO dsDAO;
    private final DataSetAssociationDAO dataSetAssociationDAO;
    private final DACUserRoleDAO dsRoleDAO;
    private final ConsentDAO consentDAO;
    private DataAccessRequestAPI accessAPI;
    private DataSetAuditDAO dataSetAuditDAO;
    private  ElectionDAO electionDAO;

    public static final String DATA_SET_ID = "Dataset ID";


    private final String MISSING_ASSOCIATION = "Dataset ID %s doesn't have an associated consent.";
    private final String DUPLICATED_ROW = "Dataset ID %s is already present in the database. ";
    private final String OVERWRITE_ON = "If you wish to overwrite DataSet values, you can turn OVERWRITE mode ON.";
    private final String DATASETID_PROPERTY_NAME = "Dataset ID";
    private final String CREATE = "CREATE";
    private final String UPDATE = "UPDATE";
    private final String DELETE = "DELETE";


    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }

    public static void initInstance(DataSetDAO dsDAO,DataSetAssociationDAO dataSetAssociationDAO, DACUserRoleDAO dsRoleDAO, ConsentDAO consentDAO, DataSetAuditDAO dataSetAuditDAO, ElectionDAO electionDAO) {
        DataSetAPIHolder.setInstance(new DatabaseDataSetAPI(dsDAO, dataSetAssociationDAO,  dsRoleDAO, consentDAO, dataSetAuditDAO, electionDAO));
    }

    private DatabaseDataSetAPI(DataSetDAO dsDAO,DataSetAssociationDAO dataSetAssociationDAO, DACUserRoleDAO dsRoleDAO, ConsentDAO consentDAO, DataSetAuditDAO dataSetAuditDAO, ElectionDAO electionDAO) {
        this.dsDAO = dsDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.dsRoleDAO = dsRoleDAO;
        this.consentDAO = consentDAO;
        this.accessAPI = AbstractDataAccessRequestAPI.getInstance();
        this.dataSetAuditDAO = dataSetAuditDAO;
        this.electionDAO = electionDAO;
    }


    @Override
    public ParseResult create(File dataSetFile, Integer userId) {
        ParseResult result = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFieldsOrderByReceiveOrder());
        List<DataSet> dataSets = result.getDatasets();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            if (completeDBCheck(dataSets)) {
                dsDAO.insertAll(dataSets);
                List<DataSetProperty> dataSetProperties = insertProperties(dataSets);
                insertDataSetAudit(dataSets, CREATE, userId, dataSetProperties);
            } else {
                result.getErrors().addAll(addMissingAssociationsErrors(dataSets));
                result.getErrors().addAll(addDuplicatedRowsErrors(dataSets));
            }
        }
        return result;
    }


    @Override
    public ParseResult overwrite(File dataSetFile, Integer userId) {
        ParseResult result = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFieldsOrderByReceiveOrder());
        List<DataSet> dataSets = result.getDatasets();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            List<String> objectIdList = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());

            List<DataSet> existentDataSets = dsDAO.searchDataSetsByObjectIdList(objectIdList);
            Map<String,DataSet> dataSetMap = new HashMap<>();

            existentDataSets.stream().forEach(dataSet -> {
                dataSetMap.put(dataSet.getObjectId(),dataSet);
            });

            List<Integer> existentIdList = existentDataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
            if (validateExistentAssociations(dataSets)) {

                dsDAO.deleteDataSetsProperties(existentIdList);
                processDataSets(dataSets, dataSetMap, userId);

            }
        }
        result.getErrors().addAll(addMissingAssociationsErrors(dataSets));
        return result;
    }


    @Override
    public Collection<DataSetDTO> describeDataSets(Integer dacUserId) {
        Collection<DataSetDTO> dataSetDTOList;
        if (userIs(DACUserRoles.RESEARCHER.getValue(), dacUserId)) {
            dataSetDTOList = dsDAO.findDataSetsForResearcher();
        } else {

            /*
            *  This three collections defined below are used to determine if "Associate Dataset with Data Owners"
            *  functionality must be enabled or disabled for each dataset.
            *  This restriction depends on the status of DataAccess elections and is represented by
            *  DataSetDTO.updateAssociationToDataOwnerAllowed.
            */

            Collection<Election> dataOwnerOpenElections;
            Collection<String> dataAccessElectionsReferenceId;
            Collection<String> datasetsAssociatedToOpenElections = new HashSet<>();
            dataOwnerOpenElections = electionDAO.getElectionByTypeAndStatus(ElectionType.DATA_SET.getValue(),ElectionStatus.OPEN.getValue());
            if(CollectionUtils.isNotEmpty(dataOwnerOpenElections)){
                dataAccessElectionsReferenceId= dataOwnerOpenElections.stream().map(e -> e.getReferenceId()).collect(Collectors.toSet());
                datasetsAssociatedToOpenElections = accessAPI.getDatasetsInDARs(dataAccessElectionsReferenceId);
            }
            dataSetDTOList = dsDAO.findDataSets();
            if (userIs(DACUserRoles.ADMIN.getValue(), dacUserId) && dataSetDTOList.size() != 0) {
                List<Document> accessRequests = accessAPI.describeDataAccessRequests();
                List<String> dataSetObjectIdList = new ArrayList<>();
                dataSetDTOList.stream().forEach(dataSet -> {
                    Map<String,String> dataSetProperties = dataSet.getProperties().stream().collect(Collectors.toMap(DataSetPropertyDTO::getPropertyName,DataSetPropertyDTO::getPropertyValue));
                    dataSetObjectIdList.add(dataSetProperties.get(DATA_SET_ID));
                });
                List<DataSet> dataSetList =  dsDAO.getDataSetsForObjectIdList(dataSetObjectIdList);
                Map<String, Integer> datasetMap =
                        dataSetList.stream().collect(Collectors.toMap(DataSet::getObjectId,
                                DataSet::getDataSetId));
                Set<String> accessRequestsDatasetIdSet = accessRequests.stream().map(ar -> (ArrayList<String>) ar.get(DarConstants.DATASET_ID)).flatMap(l -> l.stream()).collect(Collectors.toSet());
                for (DataSetDTO dataSetDTO : dataSetDTOList) {
                    String datasetObjectId = dataSetDTO.getPropertyValue(DATASETID_PROPERTY_NAME);
                    Map<String,String> dataSetProperties = dataSetDTO.getProperties().stream().collect(Collectors.toMap(DataSetPropertyDTO::getPropertyName,DataSetPropertyDTO::getPropertyValue));
                    String dataSetObjectId = dataSetProperties.get(DATA_SET_ID);
                    if(CollectionUtils.isNotEmpty(datasetsAssociatedToOpenElections) &&
                            datasetsAssociatedToOpenElections.contains(dataSetDTO.getPropertyValue(DATASETID_PROPERTY_NAME))){
                        dataSetDTO.setUpdateAssociationToDataOwnerAllowed(false);
                    }else{
                        dataSetDTO.setUpdateAssociationToDataOwnerAllowed(true);
                    }
                    if(CollectionUtils.isEmpty(dataSetAssociationDAO.getDatasetAssociation(datasetMap.get(dataSetObjectId)))){
                        dataSetDTO.setIsAssociatedToDataOwners(false);
                    }else{
                        dataSetDTO.setIsAssociatedToDataOwners(true);
                    }
                    if(CollectionUtils.isNotEmpty(accessRequests)){
                        if (accessRequestsDatasetIdSet.contains(datasetObjectId)) {
                            dataSetDTO.setDeletable(false);
                        } else {
                            dataSetDTO.setDeletable(true);
                        }
                    }else {
                        dataSetDTO.setDeletable(true);
                    }
                }
            }
        }
        if (!dataSetDTOList.isEmpty()) setConsentNameDTOList(dataSetDTOList);
        return orderByDataSetId(dataSetDTOList);
    }


    @Override
    public List<DataSet> getDataSetsForConsent(String consentId) {
        return dsDAO.getDataSetsForConsent(consentId);
    }

    @Override
    public Collection<DataSetDTO> describeDataSetsByReceiveOrder(List<String> objectIds) {
        return dsDAO.findDataSetsByReceiveOrder(objectIds);
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
    public  List< Map<String, String>> autoCompleteDataSets(String partial) {
        return dsDAO.getObjectIdsbyPartial(partial);
    }

    @Override
    public void deleteDataset(String datasetObjectId, Integer dacUserId) throws IllegalStateException{
        try {
            dsDAO.begin();
            dataSetAuditDAO.begin();
            DataSet dataset = dsDAO.findDataSetByObjectId(datasetObjectId);
            Collection<Integer> dataSetId = new ArrayList<>();
            dataSetId.add(dataset.getDataSetId());
            if(checkDatasetExistence(dataset.getDataSetId())){
                DataSetAudit dsAudit = new DataSetAudit(dataset.getDataSetId(), dataset.getObjectId(), dataset.getName(), new Date(), true, dacUserId, DELETE);
                dataSetAuditDAO.insertDataSetAudit(dsAudit);
            }
            dataSetAssociationDAO.delete(dataset.getDataSetId());
            dsDAO.deleteDataSetsProperties(dataSetId);
            dsDAO.deleteDataSets(dataSetId);
            dsDAO.commit();
            dataSetAuditDAO.commit();
        }catch (Exception e){
            dsDAO.rollback();
            dataSetAuditDAO.rollback();
            throw new IllegalStateException(e.getMessage());
        }
    }

    private boolean checkDatasetExistence(Integer dataSetId) {
        return dsDAO.findDataSetById(dataSetId) != null ? true: false;
    }

    @Override
    public void disableDataset(String datasetId, Boolean active){
        DataSet dataset = dsDAO.findDataSetByObjectId(datasetId);
        if(dataset != null){
            dsDAO.updateDataSetActive(dataset.getDataSetId(), active);
        }
    }

    @Override
    public DataSet updateNeedsReviewDataSets(String objectId, Boolean needsApproval){
        if(dsDAO.findDataSetByObjectId(objectId) == null){
            throw new NotFoundException("DataSet doesn't exist");
        }
        dsDAO.updateDataSetNeedsApproval(objectId, needsApproval);
        return dsDAO.findDataSetByObjectId(objectId);
    }

    @Override
    public List<DataSet>findNeedsApprovalDataSetByObjectId(List<String> objectIdList){
        return dsDAO.findNeedsApprovalDataSetByObjectId(objectIdList);
    }

    private Collection<DataSetDTO> orderByDataSetId(Collection<DataSetDTO> dataSetDTOList) {
        if(CollectionUtils.isNotEmpty(dataSetDTOList)){
            dataSetDTOList.stream().forEach(dataSetDTO -> {
                List<DataSetPropertyDTO> dataSetProperty = dataSetDTO.getProperties().stream().filter(property -> property.getPropertyName().equals(DATA_SET_ID)).collect(Collectors.toList());
                List<DataSetPropertyDTO> properties = dataSetDTO.getProperties().stream().filter(property -> !property.getPropertyName().equals(DATA_SET_ID)).collect(Collectors.toList());
                if(CollectionUtils.isNotEmpty(dataSetProperty)){
                    properties.add(1, dataSetProperty.get(0));
                    dataSetDTO.setProperties(properties);
                }
            });
        }
        return dataSetDTOList;
    }


    public DataSetDTO getDataSetDTO(String objectId){
        Set<DataSetDTO> dataSet = dsDAO.findDataSetWithPropertiesByOBjectId(objectId);
        for( DataSetDTO d : dataSet){
            return d;
        }
        throw new NotFoundException();
    }

    private List<String> addMissingAssociationsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
        List<Association> presentAssociations = dsDAO.getAssociationsForObjectIdList(dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList()));
        List<String> associationIdList = presentAssociations.stream().map(Association::getObjectId).collect(Collectors.toList());
        List<String> dataSetIdList = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
        errors.addAll(dataSetIdList.stream().filter(dsId -> (!(associationIdList.contains(dsId)) && (!dsId.isEmpty()))).map(dsId -> String.format(MISSING_ASSOCIATION, dsId)).collect(Collectors.toList()));
        return errors;
    }

    private List<String> addDuplicatedRowsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList<>();
        List<DataSet> failingRows = dsDAO.getDataSetsForObjectIdList(dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList()));
        errors.addAll(failingRows.stream().filter(ds -> !ds.getObjectId().isEmpty()).map(ds -> String.format(DUPLICATED_ROW, ds.getObjectId())).collect(Collectors.toList()));
        errors.add(OVERWRITE_ON);
        return errors;
    }

    private boolean completeDBCheck(List<DataSet> dataSets) {
        return validateExistentAssociations(dataSets) && validateNoDuplicates(dataSets);
    }

    private boolean validateNoDuplicates(List<DataSet> dataSets) {
        return 0 == dsDAO.getDataSetsForObjectIdList(dataSets.stream()
                .map(DataSet::getObjectId).collect(Collectors.toList())).size();
    }

    private boolean validateExistentAssociations(List<DataSet> dataSets) {
        return dataSets.size() == dsDAO.consentAssociationCount(dataSets.stream()
                .map(DataSet::getObjectId).collect(Collectors.toList()));
    }

    private List<DataSetProperty> insertProperties(List<DataSet> dataSets) {
        List<String> objectIdList = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
        List<Map<String, Integer>> retrievedValues = dsDAO.searchByObjectIdList(objectIdList);
        Map<String, Integer> retrievedValuesMap = getOneMap(retrievedValues);
        List<DataSetProperty> dataSetPropertiesList = new ArrayList<>();
        dataSets.stream().map((dataSet) -> {
            Set<DataSetProperty> properties = dataSet.getProperties();
            properties.stream().forEach((property) -> {
                property.setDataSetId(retrievedValuesMap.get(dataSet.getObjectId()));
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


    private boolean userIs(String rol, Integer dacUserId) {
        if (dsRoleDAO.findRoleByNameAndUser(rol, dacUserId) != null) {
            return true;
        }
        return false;
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

    private void processDataSets(List<DataSet> dataSets, Map<String,DataSet> existentDataSets, Integer userId) {
        List<DataSet> dataSetToCreate = new ArrayList<>();
        List<DataSet> dataSetToUpdate = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(dataSets) && MapUtils.isNotEmpty(existentDataSets)){
            dataSets.stream().forEach(newDataSet -> {
                if(existentDataSets.containsKey(newDataSet.getObjectId())){
                    DataSet existentDataSet = existentDataSets.get(newDataSet.getObjectId());
                    newDataSet.setDataSetId(existentDataSet.getDataSetId());
                    dataSetToUpdate.add(newDataSet);
                }else{
                    dataSetToCreate.add(newDataSet);
                }
            });
        }else{
            dataSetToCreate.addAll(dataSets);
        }
        if(CollectionUtils.isNotEmpty(dataSetToCreate)){
            dsDAO.insertAll(dataSetToCreate);
            List<DataSetProperty> properties = insertProperties(dataSetToCreate);
            insertDataSetAudit(dataSetToCreate, CREATE, userId, properties);
        }
        if(CollectionUtils.isNotEmpty(dataSetToUpdate)){
            dsDAO.updateAll(dataSetToUpdate);
            List<DataSetProperty> properties =  insertProperties(dataSetToUpdate);
            insertDataSetAudit(dataSetToUpdate, UPDATE, userId, properties);
        }
    }


    private void insertDataSetAudit(List<DataSet> dataSets, String action, Integer userId, List<DataSetProperty> properties ) {
        Map<String, DataSet> dataSetObjectIdMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(dataSets)){
            dataSets.stream().forEach(dataSet -> {
                dataSetObjectIdMap.put(dataSet.getObjectId(), dataSet);
            });
        }
        List<DataSet> existentDataSets = dsDAO.getDataSetsForObjectIdList(new ArrayList(dataSetObjectIdMap.keySet()));
        createDataSetAudit(existentDataSets, userId, action, properties);
    }


    private void createDataSetAudit(List<DataSet> dataSetList, Integer userId, String action, List<DataSetProperty> properties ) {
       if(CollectionUtils.isNotEmpty(dataSetList)){
           Map<Integer, List<DataSetProperty>> dataSetPropertyMap = new HashMap<>();
           properties.stream().forEach(property -> {
               if(dataSetPropertyMap.containsKey(property.getDataSetId())){
                   dataSetPropertyMap.get(property.getDataSetId()).add(property);
               }else{
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

    private List<DataSetAuditProperty> createDataSetAuditProperties(DataSet dataSet, Integer dataSetAuditId,  Map<Integer, List<DataSetProperty>> propertiesMap) {
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