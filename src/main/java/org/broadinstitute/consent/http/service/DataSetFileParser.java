package org.broadinstitute.consent.http.service;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("FieldCanBeLocal")
public class DataSetFileParser {

    private final String BLANK_REQUIRED_FIELD = "Dataset %s - The required field: %s is empty in row %d.";
    private final String BLANK_REQUIRED_FIELDS = "Dataset %s - Consent ID or Sample Collection ID is required";
    private final String MISSING_COLUMNS = "Your file has more/less columns than expected. Expected quantity: %s";
    private final String MISSING_MISPLACED_HEADER = "The uploaded file does not comply with the accepted fields. Field: (%s)%s is not recognized/ordered correctly. It should be '%s'";
    private final String PLEASE_DOWNLOAD = "Please download the Dataset Spreadsheet Model from the 'Add Datasets' window.";
    private final int DATASET_NAME_INDEX = 0;
    private final int SAMPLE_COLLECTION_INDEX = 9;
    private final int CONSENT_ID_INDEX = 10;

    public ParseResult parseTSVFile(File file, List<Dictionary> allFields, Integer lastAlias, Boolean overwrite, List<String> predefinedDatasets) {
        ParseResult result = new ParseResult();
        List<String> errors = new ArrayList<>();
        List<DataSet> datasets = new ArrayList<>();
        List<String> allKeys = allFields.stream().map(Dictionary::getKey).collect(Collectors.toList());
        List<Dictionary> requiredKeys = allFields.stream().filter(Dictionary::getRequired).collect(Collectors.toList());
        try {
            CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
            CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withCSVParser(parser).build();
            // reading headers from TSV
            errors.addAll(validateHeaderFields(reader.readNext(), allKeys));
            if (!errors.isEmpty()) {
                result.setDatasets(datasets);
                result.setErrors(errors);
                return result;
            }
            int row = 0;
            String[] record;
            while ((record = reader.readNext()) != null) {
                errors.addAll(validateRequiredFields(++row, record, requiredKeys, record[DATASET_NAME_INDEX]));
                errors.addAll(validateConsentAndCollectionId(record, record[DATASET_NAME_INDEX]));
                DataSet ds = createDataSet(record);
                Set<DataSetProperty> properties = new HashSet<>();
                for (int i = 1; i < allKeys.size(); i++) {
                    if(i != CONSENT_ID_INDEX) {
                        properties.add(new DataSetProperty(ds.getDataSetId(), allFields.get(i).getKeyId(), record[i], ds.getCreateDate()));
                    }
                }
                ds.setProperties(properties);
                datasets.add(ds);
            }
        } catch (Exception e) {
            logger().error("An unexpected error had occurred in DataSetFileParser", e);
            errors.add("An unexpected error had occurred in DataSetFileParser - Contact Support");
        }
        if(!overwrite) datasets = createAlias(datasets, lastAlias, predefinedDatasets);
        result.setDatasets(datasets);
        result.setErrors(errors);
        return result;
    }

    private List<String> validateHeaderFields(String[] record, List<String> keys) {
        List<String> errors = new ArrayList<>();
        if (record == null) {
            errors.add("Invalid records");
        }
        if (keys == null) {
            errors.add("Invalid keys");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        if ((record.length < keys.size()) || (record.length > keys.size())) {
            errors.add(String.format(MISSING_COLUMNS, keys.size()));
        } else {
            for (int i = 0; i < record.length; i++) {
                if (!(keys.get(i).equalsIgnoreCase(record[i]))) {
                    errors.add(String.format(MISSING_MISPLACED_HEADER, i, record[i], keys.get(i)));
                }
            }
        }
        if (!errors.isEmpty()) {
            errors.add(PLEASE_DOWNLOAD);
        }
        return errors;
    }

    private List<String> validateRequiredFields(int row, String[] record, List<Dictionary> requiredFields, String id) {
        return requiredFields.stream().filter(field -> record[field.getReceiveOrder()].isEmpty()).map(field -> String.format(BLANK_REQUIRED_FIELD, id, field.getKey(), row)).collect(Collectors.toList());
    }

    private List<String> validateConsentAndCollectionId(String[] record, String datasetName) {
        if (StringUtils.isEmpty(record[SAMPLE_COLLECTION_INDEX]) && StringUtils.isEmpty(record[CONSENT_ID_INDEX])) {
            return Collections.singletonList(String.format(BLANK_REQUIRED_FIELDS, datasetName));
        }
        return Collections.emptyList();
    }

    private DataSet createDataSet(String[] record) {
        DataSet dataset = new DataSet();
        dataset.setCreateDate(new Date());
        dataset.setName(record[0]);
        dataset.setObjectId(StringUtils.isNotEmpty(record[9]) ? record[9] : null);
        dataset.setActive(true);
        dataset.setConsentName(StringUtils.isNotEmpty(record[10]) ? record[10] : null);
        return dataset;
    }

    public List<DataSet> createAlias(final List<DataSet> dataSets, Integer lastAlias, List<String> predefinedDatasets) {
        int initialAlias = 3;
        List<DataSet> results = new ArrayList<>(dataSets);
        for (DataSet ds : results) {
            if (StringUtils.isNotEmpty(ds.getName()) && ds.getName().equals(predefinedDatasets.get(1))) {
                ds.setAlias(2);
            } else if (StringUtils.isNotEmpty(ds.getName()) && ds.getName().equals(predefinedDatasets.get(0))) {
                ds.setAlias(1);
            } else if (lastAlias == null || lastAlias < 3) {
                ds.setAlias(initialAlias);
                ++initialAlias;
            } else if (ds.getAlias() == null || ds.getAlias() == 0) {
                ds.setAlias(++lastAlias);
            }
        }
        return results;
    }

    protected Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }
}
