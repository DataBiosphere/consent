package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import javax.ws.rs.NotFoundException;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

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


    private final String MISSING_ASSOCIATION = "Dataset ID %s doesn't have an associated consent.";
    private final String DUPLICATED_ROW = "Dataset ID %s is already present in the database. ";
    private final String OVERWRITE_ON = "If you wish to overwrite DataSet values, you can turn OVERWRITE mode ON.";

    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }

    public static void initInstance(DataSetDAO dsDAO,DataSetAssociationDAO dataSetAssociationDAO, DACUserRoleDAO dsRoleDAO, ConsentDAO consentDAO) {
        DataSetAPIHolder.setInstance(new DatabaseDataSetAPI(dsDAO, dataSetAssociationDAO,  dsRoleDAO, consentDAO));
    }

    private DatabaseDataSetAPI(DataSetDAO dsDAO,DataSetAssociationDAO dataSetAssociationDAO, DACUserRoleDAO dsRoleDAO, ConsentDAO consentDAO) {
        this.dsDAO = dsDAO;
        this.dataSetAssociationDAO = dataSetAssociationDAO;
        this.dsRoleDAO = dsRoleDAO;
        this.consentDAO = consentDAO;
        this.accessAPI = AbstractDataAccessRequestAPI.getInstance();
    }


    @Override
    public ParseResult create(File dataSetFile) {
        ParseResult result = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFields());
        List<DataSet> dataSets = result.getDatasets();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            if (completeDBCheck(dataSets)) {
                dsDAO.insertAll(dataSets);
                insertProperties(dataSets);
            } else {
                result.getErrors().addAll(addMissingAssociationsErrors(dataSets));
                result.getErrors().addAll(addDuplicatedRowsErrors(dataSets));
            }
        }
        return result;
    }

    @Override
    public ParseResult overwrite(File dataSetFile) {
        ParseResult result = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFields());
        List<DataSet> dataSets = result.getDatasets();
        if (CollectionUtils.isNotEmpty(dataSets)) {
            List<String> objectIdList = dataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
            List<DataSet> existentDataSets = dsDAO.searchDataSetsByObjectIdList(objectIdList);
            List<Integer> existentIdList = existentDataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
            if (validateExistentAssociations(dataSets)) {
                dsDAO.deleteDataSetsProperties(existentIdList);
                dsDAO.deleteDataSets(existentIdList);
                dsDAO.insertAll(processDisableDataSets(dataSets, existentDataSets));
                insertProperties(dataSets);
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
            dataSetDTOList = dsDAO.findDataSets();
            if (userIs(DACUserRoles.ADMIN.getValue(), dacUserId) && dataSetDTOList.size() != 0) {
                List<Document> accessRequests = accessAPI.describeDataAccessRequests();
                List<String> datasetObjectIdList = dataSetDTOList.stream().map(dto -> dto.getProperties().get(9).getPropertyValue()).collect(Collectors.toList());
                List<DataSet> dataSetList =  dsDAO.getDataSetsForObjectIdList(datasetObjectIdList);
                Map<String, Integer> datasetMap =
                        dataSetList.stream().collect(Collectors.toMap(DataSet::getObjectId,
                                DataSet::getDataSetId));
                for (DataSetDTO dataSetDTO : dataSetDTOList) {
                    String datasetObjectId  = dataSetDTO.getProperties().get(9).getPropertyValue();
                    if(CollectionUtils.isEmpty(dataSetAssociationDAO.getDatasetAssociation(datasetMap.get(datasetObjectId)))){
                        dataSetDTO.setIsAssociatedToDataOwners(false);
                    }else{
                        dataSetDTO.setIsAssociatedToDataOwners(true);
                    }
                    if(CollectionUtils.isNotEmpty(accessRequests)){
                        accessRequests.stream().forEach(access -> {
                            if(access.get(DarConstants.DATASET_ID, List.class).stream().anyMatch(objectId -> objectId.equals(datasetObjectId))){
                                dataSetDTO.setDeletable(false);
                            } else {
                                dataSetDTO.setDeletable(true);
                            }
                        });
                    }else {
                        dataSetDTO.setDeletable(true);
                    }
                }
            }
        }
        if (!dataSetDTOList.isEmpty()) setConsentNameDTOList(dataSetDTOList);
        return dataSetDTOList;
    }


    @Override
    public List<DataSet> getDataSetsForConsent(String consentId) {
        return dsDAO.getDataSetsForConsent(consentId);
    }

    @Override
    public Collection<DataSetDTO> describeDataSets(List<String> objectIds) {
        return dsDAO.findDataSets(objectIds);
    }

    @Override
    public Collection<Dictionary> describeDictionary() {
        return dsDAO.getMappedFields();
    }

    @Override
    public  List< Map<String, String>> autoCompleteDataSets(String partial) {
        List< Map<String, String>> map =  dsDAO.getObjectIdsbyPartial(partial);
        return map;
    }

    @Override
    public void deleteDataset(String datasetObjectId) {
        DataSet dataset = dsDAO.findDataSetByObjectId(datasetObjectId);
        Collection<Integer> dataSetId = new ArrayList<>();
        dataSetId.add(dataset.getDataSetId());
        dsDAO.deleteDataSetsProperties(dataSetId);
        dsDAO.deleteDataSets(dataSetId);
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


    public DataSetDTO getDataSetDTO(String objectId){
        Set<DataSetDTO> dataSet = dsDAO.findDataSetWithPropertiesByOBjectId(objectId);
        for( DataSetDTO d : dataSet){
            return d;
        }
        throw new NotFoundException();
    }


    public List<DataSet>findNeedsApprovalDataSetByObjectId(List<String> objectIdList){
        return dsDAO.findNeedsApprovalDataSetByObjectId(objectIdList);
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

    private void insertProperties(List<DataSet> dataSets) {
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

    private Collection<DataSet> processDisableDataSets(List<DataSet> dataSets, List<DataSet> existentDataSets) {
        Map<String, DataSet> dataSetMap = dataSets.stream().collect(Collectors.toMap(DataSet::getObjectId, dataSet -> dataSet));
        if(CollectionUtils.isNotEmpty(dataSets) && CollectionUtils.isNotEmpty(existentDataSets)){
            existentDataSets.stream().filter(ds -> !ds.getActive()).forEach(s ->
                            dataSetMap.get(s.getObjectId()).setActive(false)
            );
        }
        return dataSetMap.values();
    }

}