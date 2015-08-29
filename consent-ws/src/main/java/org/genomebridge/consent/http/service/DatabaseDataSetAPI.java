package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.db.DataSetDAO;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.DataSetProperty;

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
            if (validate(dataSets)) {
                dsDAO.insertAll(dataSets);
                insertProperties(dataSets);
                List<String> objectIdList = dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList());
                List<Map<String, Integer>> retrievedValues = dsDAO.searchByObjectIdList(objectIdList);
                Map<String, Integer> retrievedValuesMap = getOneMap(retrievedValues);
                List<DataSetProperty> dataSetPropertiesList = new ArrayList<>();
                for (DataSet dataSet : dataSets) {
                    Set<DataSetProperty> properties = dataSet.getProperties();
                    for(DataSetProperty property : properties ){
                        property.setDataSetId(retrievedValuesMap.get(dataSet.getObjectId()));
                    }
                    dataSetPropertiesList.addAll(dataSet.getProperties());
                }
                dsDAO.insertDataSetsProperties(dataSetPropertiesList);
            } else {
                errors.addAll(addDatabaseErrors(dsDAO.missingAssociations(dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList()))));
            }
        }
        return parsedResponse;
    }

    @Override
    public Map<String, Object> overwrite(File dataSetFile){
        Map<String, Object> parsedResponse = parser.parseTSVFile(dataSetFile, dsDAO.getMappedFields());
        List<DataSet> dataSets = (List<DataSet>) parsedResponse.get("datasets");
        List<String> errors = (List<String>) parsedResponse.get("validationsErrors");
        if (dataSets.size() > 0) {
            List<String> objectIdList = dataSets.stream().map(d -> d.getObjectId()).collect(Collectors.toList());
            Map<String, Integer> retrievedValuesMap = getOneMap(dsDAO.searchByObjectIdList(objectIdList));
            Collection<Integer> dataSetsIds = retrievedValuesMap.values();
            dsDAO.deleteDataSetsProperties(dataSetsIds);
            dsDAO.deleteDataSets(dataSetsIds);
            if (validate(dataSets)) {
                dsDAO.insertAll(dataSets);
                insertProperties(dataSets);
            }
        }
        return parsedResponse;
    }

    private List<String> addDatabaseErrors(List<DataSet> dataSets) {
        List<String> errors = new ArrayList();
        for(DataSet ds: dataSets){
            errors.add(String.format(MISSING_ASSOCIATION, ds.getObjectId()));
        }
        return errors;
    }

    private boolean validate(List<DataSet> dataSets) {
        if (validateExistentAssociations(dataSets)) {
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
    public Collection<DataSet> describeDataSets() {
        return dsDAO.findDataSets();
    }

    private void setDataSetId(Set<DataSetProperty> properties, int dataSetId) {
        for (DataSetProperty property : properties) {
            property.setDataSetId(dataSetId);
        }
      }
    }

