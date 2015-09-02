package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.DataSetDAO;
import org.genomebridge.consent.http.models.Association;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.DataSetProperty;
import org.genomebridge.consent.http.models.Dictionary;
import org.genomebridge.consent.http.models.dto.DataSetDTO;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;


/**
 * Implementation class for VoteAPI on top of ElectionDAO database support.
 */
public class DatabaseDataSetAPI extends AbstractDataSetAPI {

    private DataSetFileParser parser = new DataSetFileParser();
    private DataSetDAO dsDAO;

    private String MISSING_ASSOCIATION = "Dataset ID %s doesn't have an associated consent.";
    private String DUPLICATED_ROW = "Dataset ID %s is already present in the database. ";
    private String OVERWRITE_ON = "If you wish to overwrite DataSet values, you can turn OVERWRITE mode ON.";

    protected org.apache.log4j.Logger logger() {
        return org.apache.log4j.Logger.getLogger("DataSetResource");
    }

    public static void initInstance(DataSetDAO dsDAO) {
        DataSetAPIHolder.setInstance(new DatabaseDataSetAPI(dsDAO));
    }

    private DatabaseDataSetAPI(DataSetDAO dsDAO) {
        this.dsDAO = dsDAO;
    }

    @Override
    public Map<String, Object> create(File dataSetFile){
        Map<String, Object> parsedResponse = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFields());
        List<DataSet> dataSets = (List<DataSet>) parsedResponse.get("datasets");
        List<String> errors = (List<String>) parsedResponse.get("validationsErrors");
        
        if (CollectionUtils.isNotEmpty(errors)) {
            return parsedResponse;
        }
        
        if (CollectionUtils.isNotEmpty(dataSets)) {
            if (completeDBCheck(dataSets)) {
                dsDAO.insertAll(dataSets);
                insertProperties(dataSets);
            } else {
                errors.addAll(addMissingAssociationsErrors(dataSets));
                errors.addAll(addDuplicatedRowsErrors(dataSets));
            }
        }
        return parsedResponse;
    }

    @Override
    public Map<String, Object> overwrite(File dataSetFile){
        Map<String, Object> parsedResponse = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFields());
        List<DataSet> dataSets = (List<DataSet>) parsedResponse.get("datasets");
        List<String> errors = (List<String>) parsedResponse.get("validationsErrors");
        if (CollectionUtils.isNotEmpty(dataSets)) {
            List<String> objectIdList = dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList());
            Map<String, Integer> retrievedValuesMap = getOneMap(dsDAO.searchByObjectIdList(objectIdList));
            Collection<Integer> dataSetsIds = retrievedValuesMap.values();
            if (validateExistentAssociations(dataSets)) {
                dsDAO.deleteDataSetsProperties(dataSetsIds);
                dsDAO.deleteDataSets(dataSetsIds);
                dsDAO.insertAll(dataSets);
                insertProperties(dataSets);
            } else {
                errors.addAll(addMissingAssociationsErrors(dataSets));
            }
        }
        return parsedResponse;
    }

    private List<String> addMissingAssociationsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList();
        List<Association> presentAssociations = dsDAO.getAssociationsForObjectIdList(dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList()));
        List<String> associationIdList = presentAssociations.stream().map(a -> a.getObjectId()).collect(Collectors.toList());
        List<String> dataSetIdList = dataSets.stream().map(a -> a.getObjectId()).collect(Collectors.toList());
        for(String dsId: dataSetIdList){
            if(!associationIdList.contains(dsId)){
                errors.add(String.format(MISSING_ASSOCIATION, dsId));
            }
        }
        return errors;
    }

    private List<String> addDuplicatedRowsErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList();
        List<DataSet> failingRows = dsDAO.getDataSetsForObjectIdList(dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList()));
        for(DataSet ds: failingRows){
            errors.add(String.format(DUPLICATED_ROW, ds.getObjectId()));
        }
        errors.add(OVERWRITE_ON);
        return errors;
    }

    private boolean completeDBCheck(List<DataSet> dataSets) {
        if (validateExistentAssociations(dataSets) && validateNoDuplicates(dataSets)) {
            return true;
        } 
        return false;
    }

    private boolean validateNoDuplicates(List<DataSet> dataSets) {
        if (0 == dsDAO.getDataSetsForObjectIdList(dataSets.stream()
                .map(d -> d.getObjectId()).collect(Collectors.toList())).size())
        {
            return true;
        }
        return false;
    }

    private boolean validateExistentAssociations(List<DataSet> dataSets) {
        if (dataSets.size() == dsDAO.consentAssociationCount(dataSets.stream()
                .map(d -> d.getObjectId()).collect(Collectors.toList())))
        {
            return true;
        }
        return false;
    }

    private void insertProperties(List<DataSet> dataSets) {
        List<String> objectIdList = dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList());
        List<Map<String, Integer>> retrievedValues = dsDAO.searchByObjectIdList(objectIdList);
        Map<String, Integer> retrievedValuesMap = getOneMap(retrievedValues);
        List<DataSetProperty> dataSetPropertiesList = new ArrayList<>();
        for (DataSet dataSet : dataSets) {
            Set<DataSetProperty> properties = dataSet.getProperties();
            for (DataSetProperty property : properties) {
                property.setDataSetId(retrievedValuesMap.get(dataSet.getObjectId()));
            }
            dataSetPropertiesList.addAll(dataSet.getProperties());
        }
        dsDAO.insertDataSetsProperties(dataSetPropertiesList);
    }

    private Map<String, Integer> getOneMap(List<Map<String, Integer>> retrievedValues) {
         Map<String, Integer> newMap = new HashMap<>();
         for (Map<String, Integer> map : retrievedValues){
             Iterator Iterator = map.keySet().iterator();
             String objectId = Iterator.next().toString();
             newMap.put(objectId, map.get(objectId));
          }
        return newMap;

    }

    @Override
    public Collection<DataSetDTO> describeDataSets() {
        return dsDAO.findDataSets();
    }
    
    @Override
    public Collection<DataSetDTO> describeDataSets(List<String> objectIds) {
        return dsDAO.findDataSets(objectIds);
    }
    

    @Override
    public Collection<Dictionary> describeDictionary() {
        return dsDAO.getMappedFields();
    }


    private void setDataSetId(Set<DataSetProperty> properties, int dataSetId) {
        for (DataSetProperty property : properties) {
            property.setDataSetId(dataSetId);
        }
      }
    }

