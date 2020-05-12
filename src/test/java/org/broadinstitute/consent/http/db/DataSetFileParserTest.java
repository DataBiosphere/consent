package org.broadinstitute.consent.http.db;

import io.dropwizard.testing.ResourceHelpers;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.service.DataSetFileParser;
import org.broadinstitute.consent.http.service.ParseResult;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("FieldCanBeLocal")
public class DataSetFileParserTest extends DAOTestHelper {

    private final DataSetFileParser dataSetFileParser = new DataSetFileParser();

    private final String DATASET_1 = "Melanoma_Regev";
    private final String DATASET_2 = "Melanoma-Regev-Izar-Garraway-DFCI-ICR";
    private final Integer DATASET_ALIAS_1 = 1;
    private final Integer DATASET_ALIAS_2 = 2;
    private final Integer DATASET_ALIAS_3 = 3;
    private final Integer DATASET_ALIAS_4 = 4;


    @Test
    public void testCreateAliasForPredefinedDatasets(){
        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet = new DataSet();
        dataSet.setName(DATASET_1);
        DataSet dataSet2 = new DataSet();
        dataSet2.setName(DATASET_2);
        dataSets.add(dataSet);
        dataSets.add(dataSet2);
        List<String> predefinedDatasets = Arrays.asList(DATASET_1, DATASET_2);
        List<DataSet> results = dataSetFileParser.createAlias(dataSets,5, predefinedDatasets);
        assertEquals(2, results.size());
        assertEquals(DATASET_ALIAS_1, results.get(0).getAlias());
        assertEquals(DATASET_ALIAS_2, results.get(1).getAlias());
    }

    @Test
    public void testCreateFirstAlias(){
        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet = new DataSet();
        dataSet.setName("Test");
        dataSets.add(dataSet);
        List<String> predefinedDatasets = Arrays.asList(DATASET_1, DATASET_2);
        List<DataSet> results = dataSetFileParser.createAlias(dataSets,0, predefinedDatasets);
        assertEquals(1, results.size());
        assertEquals(DATASET_ALIAS_3, results.get(0).getAlias());
    }

    @Test
    public void testCreateAlias(){
        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet = new DataSet();
        dataSet.setName("Test");
        dataSets.add(dataSet);
        List<String> predefinedDatasets = Arrays.asList(DATASET_1, DATASET_2);
        List<DataSet> results = dataSetFileParser.createAlias(dataSets,3, predefinedDatasets);
        assertEquals(1, results.size());
        assertEquals(results.get(0).getAlias(), DATASET_ALIAS_4);
    }

    @Test
    public void testParseTSVFile() {
        File f = new File(ResourceHelpers.resourceFilePath("dataset/correctFile.txt"));
        List<Dictionary> allFields = dataSetDAO.getMappedFieldsOrderByReceiveOrder();
        Integer lastAlias = 0;
        List<String> predefinedDatasets = Arrays.asList(DATASET_1, DATASET_2);
        ParseResult result = dataSetFileParser.parseTSVFile(f, allFields, lastAlias, false, predefinedDatasets);
        assertFalse(result.getDatasets().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

}
