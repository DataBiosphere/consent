package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Dac;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class DacDAOTest extends AbstractTest {

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }


    private DacDAO dacDAO;


    @Before
    public void setUp() {
        dacDAO = getApplicationJdbi().onDemand(DacDAO.class);
    }

    @After
    public void tearDown() {
        // Teardown also tests the delete function
        dacDAO.findAll().forEach(dac -> dacDAO.deleteDac(dac.getDacId()));
    }

    @SuppressWarnings("UnusedReturnValue")
    private Dac createDac() {
        Integer dacId = dacDAO.createDac(RandomStringUtils.random(10), RandomStringUtils.random(10), new Date());
        return dacDAO.findById(dacId);
    }

    @Test
    public void testCreate() {
        // No-op ... tested in `createDac()`
    }

    @Test
    public void testFindById() {
        // No-op ... tested in `createDac()`
    }

    @Test
    public void testDelete() {
        // No-op ... tested in `tearDown()`
    }

    @Test
    public void testFindAll() {
        int count = 4;
        for (int i = 1; i <= count; i++) createDac();

        List<Dac> dacList = dacDAO.findAll();
        Assert.assertEquals(count, dacList.size());
    }

    @Test
    public void testUpdateDac() {
        String newValue = "New Value";
        Dac dac = createDac();
        dacDAO.updateDac(newValue, newValue, new Date(), dac.getDacId());
        Dac updatedDac = dacDAO.findById(dac.getDacId());

        Assert.assertEquals(updatedDac.getName(), newValue);
        Assert.assertEquals(updatedDac.getDescription(), newValue);
    }

}
