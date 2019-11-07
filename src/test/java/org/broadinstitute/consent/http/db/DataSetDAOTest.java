package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.DataSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DataSetDAOTest extends AbstractTest {

    // TODO: We have to track what we add because there are tests that rely on existing data.
    // Once those are replaced, this can be replaced with a delete all.
    private List<Integer> createdDataSetIds = new ArrayList<>();

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private DataSetDAO dataSetDAO;

    @Before
    public void setUp() {
        dataSetDAO = getApplicationJdbi().onDemand(DataSetDAO.class);
    }

    @After
    public void tearDown() {
        dataSetDAO.deleteDataSets(createdDataSetIds);
    }

    @Test
    public void testInsertDataset() {
        // No-op ... tested in `createDataset()`
    }

    @Test
    public void testFindDatasetById() {
        Integer id = createDataset();
        DataSet dataSet = dataSetDAO.findDataSetById(id);
        Assert.assertNotNull(dataSet);
        dataSetDAO.deleteDataSets(Collections.singletonList(id));
    }

    @Test
    public void testDeleteDataSets() {
        // No-op ... tested in `tearDown()`
    }

    private Integer createDataset() {
        DataSet ds = new DataSet();
        ds.setName("Name_" + RandomStringUtils.random(20, true, true));
        ds.setCreateDate(new Date());
        ds.setObjectId("Object ID_" + RandomStringUtils.random(20, true, true));
        ds.setActive(true);
        ds.setAlias(RandomUtils.nextInt(1, 1000));
        Integer id = dataSetDAO.insertDataset(ds.getName(), ds.getCreateDate(), ds.getObjectId(), ds.getActive(), ds.getAlias());
        createdDataSetIds.add(id);
        return id;
    }

}
