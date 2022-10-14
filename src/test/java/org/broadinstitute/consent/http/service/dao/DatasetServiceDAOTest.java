package org.broadinstitute.consent.http.service.dao;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DatasetServiceDAOTest extends DAOTestHelper {

    private DatasetServiceDAO serviceDAO;

    @Before
    public void setUp() {
        serviceDAO = new DatasetServiceDAO(jdbi, datasetDAO);
    }

    /*
     * The following DatasetProperty synchronization test cases are covered here:
     *  1. Add new dataset property with an existing dictionary key
     *  2. Add new dataset property without an existing dictionary key
     *  3. Update existing dataset property
     *  4. Add new dataset property with an existing dictionary key + Delete existing property
     *  5. Add new dataset property without an existing dictionary key + Delete existing property
     *  6. Update existing dataset property + Delete existing property
     */

    @Test
    public void testSynchronizeDatasetProperties_case3() throws Exception {
        Dataset dataset = createSampleDataset();
        List<Dictionary> dictionaryTerms = datasetDAO.getDictionaryTerms();
        Dictionary dictionary = dictionaryTerms.get(0);
        DatasetProperty prop = createSamplePropFromDictionaryTerm(dataset, dictionary);
        String newPropVal = RandomStringUtils.randomAlphabetic(10);
        prop.setPropertyValue(newPropVal);
        List<DatasetProperty> synchronizedProps = serviceDAO.synchronizeDatasetProperties(dataset.getDataSetId(), List.of(prop));
        assertEquals(1, synchronizedProps.size());
        assertEquals(newPropVal, synchronizedProps.get(0).getPropertyValueAsString());
    }

    // Helper methods

    /**
     * Creates a new sample Dataset along with a User and a Dac
     *
     * @return Dataset
     */
    private Dataset createSampleDataset() {
        User user = createUser();
        Dac dac = createDac();
        DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
        Timestamp now = new Timestamp(new Date().getTime());
        Integer id = datasetDAO.insertDataset(
                RandomStringUtils.randomAlphabetic(10),
                now,
                user.getUserId(),
                RandomStringUtils.randomAlphabetic(10),
                true,
                dataUse.toString(),
                dac.getDacId()
        );
        return datasetDAO.findDatasetById(id);
    }

    /**
     * Creates a DatasetProperty that uses an existing Dictionary term
     *
     * @param dataset    The Dataset
     * @param dictionary The Dictionary
     * @return The created DatasetProperty
     */
    private DatasetProperty createSamplePropFromDictionaryTerm(Dataset dataset, Dictionary dictionary) {
        DatasetProperty prop = new DatasetProperty();
        prop.setDataSetId(dataset.getDataSetId());
        prop.setPropertyKey(dictionary.getKeyId());
        prop.setPropertyName(dictionary.getKey());
        prop.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
        prop.setCreateDate(new Date());
        prop.setSchemaProperty("testSchemaProp");
        prop.setPropertyType(DatasetPropertyType.String);
        datasetDAO.insertDatasetProperties(List.of(prop));
        Set<DatasetProperty> props = datasetDAO.findDatasetPropertiesByDatasetId(dataset.getDataSetId());
        return props.stream().toList().get(0);
    }

}