package org.broadinstitute.consent.http.service;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DataSetFileParser {

    private final String BLANK_REQUIRED_FIELD = "Dataset ID %s - The required field: %s is empty in row %d.";
    private final String MISSING_COLUMNS = "Your file has more/less columns than expected. Expected quantity: %s";
    private final String MISSING_MISPLACED_HEADER = "The uploaded file does not comply with the accepted fields. Field: (%s)%s is not recognized/ordered correctly. It should be '%s'";
    private final String PLEASE_DOWNLOAD = "Please download the Dataset Spreadsheet Model from the 'Add Datasets' window.";
    private int DATASET_ID_INDEX = 9;

    public ParseResult parseTSVFile(File file, List<Dictionary> allFields) {
        ParseResult result = new ParseResult();
        List<String> errors = new ArrayList<>();
        List<DataSet> datasets = new ArrayList<>();
        List<String> allKeys = allFields.stream().map(Dictionary::getKey).collect(Collectors.toList());
        List<Dictionary> requiredKeys = allFields.stream().filter(d -> d.getRequired()).collect(Collectors.toList());
        try {
            CSVReader reader = new CSVReader(new FileReader(file), '\t');
            // reading headers from TSV
            errors.addAll(validateHeaderFields(reader.readNext(), allKeys));
            if (!errors.isEmpty()) {
                result.setDatasets(datasets);
                result.setErrors(errors);
                return result;
            }
            int row = 0;
            String[] record;
            while ( (record = reader.readNext() ) != null) {
                errors.addAll(validateRequiredFields(++row, record, requiredKeys, record[DATASET_ID_INDEX]));
                DataSet ds = createDataSet(record);
                Set<DataSetProperty> properties = new HashSet<>();
                for(int i=1; i<allKeys.size() ; i++){
                   properties.add(new DataSetProperty(ds.getDataSetId(), allFields.get(i).getKeyId(), record[i], ds.getCreateDate()));
                }
                ds.setProperties(properties);
                datasets.add(ds);

            }
        } catch (IOException e) {
            logger().error("An unexpected error had occurred in DataSetFileParser", e);
            errors.add("An unexpected error had occurred in DataSetFileParser - Contact Support");
        }
        result.setDatasets(datasets);
        result.setErrors(errors);
        return result;
    }

    private List<String> validateHeaderFields(String[] record, List<String> keys) {
        List<String> errors = new ArrayList<>();
        if ((record.length < keys.size()) || (record.length > keys.size())){
            errors.add(String.format(MISSING_COLUMNS, keys.size()));
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

    private List<String> validateRequiredFields(int row, String[] record, List<Dictionary> requiredFields, String id){
        return requiredFields.stream().filter(field -> record[field.getReceiveOrder()].isEmpty()).map(field -> String.format(BLANK_REQUIRED_FIELD, id, field.getKey(), row)).collect(Collectors.toList());
    }

    private DataSet createDataSet(String[] record) {
        DataSet dataset = new DataSet();
        dataset.setCreateDate(new Date());
        dataset.setName(record[0]);
        dataset.setObjectId(record[9]);
        dataset.setActive(true);
        return dataset;
    }

    protected Logger logger() {
        return Logger.getLogger("DataSetFileParser");
    }
}
