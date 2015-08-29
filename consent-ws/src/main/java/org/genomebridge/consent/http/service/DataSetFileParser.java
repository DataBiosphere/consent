package org.genomebridge.consent.http.service;

import com.opencsv.CSVReader;
import org.genomebridge.consent.http.models.DataSet;
import org.genomebridge.consent.http.models.DataSetProperty;
import org.genomebridge.consent.http.models.Dictionary;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DataSetFileParser {

    private String BLANK_REQUIRED_FIELD = "Dataset ID %s - The required field: %s is empty.";
    private String MISSING_COLUMNS = "Your does not have all the required columns.";
    private String MISSING_MISPLACED_HEADER = "The uploaded file does not comply with the accepted fields. Field: (%s)%s is not recognized/ordered correctly. It should be '%s'";
    private String PLEASE_DOWNLOAD = "Please, download the sample file from your console.";

    public Map<String, Object> parseTSVFile(File file, List<Dictionary> allFields) {
        List<String> errors = new ArrayList();
        List<DataSet> datasets = new ArrayList();
        Map<String, Object> result = new HashMap();
        List<String> allKeys = allFields.stream().map(d -> d.getKey()).collect(Collectors.toList());
        List<Dictionary> requiredKeys = allFields.stream().filter(d -> d.getRequired() == true).collect(Collectors.toList());
        try {
            CSVReader reader = new CSVReader(new FileReader(file), '\t');
            // reading headers from TSV
            errors.addAll(validateHeaderFields(reader.readNext(), requiredKeys, allKeys));
            if (!errors.isEmpty()) {
                result.put("datasets", datasets);
                result.put("validationsErrors", errors);
                return result;
            }
            String[] record;
            while ( (record = reader.readNext() ) != null) {
                // 9 is the index of DataSetId
                errors.addAll(validateRequiredFields(record, requiredKeys, record[9]));
                if (errors.isEmpty()) {
                    DataSet ds = createDataSet(record);
                    Set<DataSetProperty> properties = new HashSet<>();
                    for(int i=1; i<allKeys.size() ; i++){
                        properties.add(new DataSetProperty(ds.getDataSetId(), allFields.get(i).getKeyId(), record[i], ds.getCreateDate()));
                    }
                    ds.setProperties(properties);
                    datasets.add(ds);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        result.put("datasets", datasets);
        result.put("validationsErrors", errors);
        return result;
    }

    private List<String> validateHeaderFields(String[] record, List<Dictionary> requiredKeys, List<String> keys) {
        List<String> errors = new ArrayList();
        if (record.length < requiredKeys.size()) {
            errors.add(MISSING_COLUMNS);
        } else {
            for (int i = 0; i < record.length; i++) {
                if (!(keys.get(i).equalsIgnoreCase(record[i]))) {
                    errors.add(String.format(MISSING_MISPLACED_HEADER, i, record[i], keys.get(i)));
                }
            }
        }
        if ( !errors.isEmpty() ) {
            errors.add(PLEASE_DOWNLOAD);
        }
        return errors;
    }

    private List<String> validateRequiredFields(String[] record, List<Dictionary> requiredFields, String id){
        List<String> errors = new ArrayList();
        for(Dictionary field: requiredFields){
            if(record[field.getDisplayOrder()].isEmpty()){
                errors.add(String.format(BLANK_REQUIRED_FIELD, id, field.getKey()));
            }
        }
        return errors;
    }

    private DataSet createDataSet(String[] record) {
        DataSet dataset = new DataSet();
        dataset.setCreateDate(new Date());
        dataset.setName(record[0]);
        dataset.setObjectId(record[9]);
        return dataset;
    }

}
