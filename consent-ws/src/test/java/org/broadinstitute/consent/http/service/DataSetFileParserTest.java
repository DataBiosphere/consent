package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.DataSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DataSetFileParserTest {

    private DataSetFileParser dataSetFileParser = new DataSetFileParser();

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
        List<DataSet> results = dataSetFileParser.createAlias(dataSets,5, DATASET_1, DATASET_2);
        Assert.assertTrue(results.size() == 2);
        Assert.assertTrue(results.get(0).getAlias().equals(DATASET_ALIAS_1));
        Assert.assertTrue(results.get(1).getAlias().equals(DATASET_ALIAS_2));
    }

    @Test
    public void testCreateFirstAlias(){
        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet = new DataSet();
        dataSet.setName("Test");
        dataSets.add(dataSet);
        List<DataSet> results = dataSetFileParser.createAlias(dataSets,0, DATASET_1, DATASET_2);
        Assert.assertTrue(results.size() == 1);
        Assert.assertTrue(results.get(0).getAlias().equals(DATASET_ALIAS_3));
    }

    @Test
    public void testCreatetAlias(){
        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet = new DataSet();
        dataSet.setName("Test");
        dataSets.add(dataSet);
        List<DataSet> results = dataSetFileParser.createAlias(dataSets,3, DATASET_1, DATASET_2);
        Assert.assertTrue(results.size() == 1);
        Assert.assertTrue(results.get(0).getAlias().equals(DATASET_ALIAS_4));
    }
}
