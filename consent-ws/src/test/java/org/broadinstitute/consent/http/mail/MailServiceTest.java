package org.broadinstitute.consent.http.mail;

import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.Writer;
import java.util.Collections;

public class MailServiceTest {

    private static String TO = "to@broadinstitute.org";
    private static String ID = "DUL-123";
    private static String TYPE = "Data Use Limitations";

    @Mock
    private MailServiceAPI mailService;

    @Mock
    Writer template;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MailConfiguration config = new MailConfiguration();
        config.setSendGridApiKey("test");
        config.setGoogleAccount("from@broadinstitute.org");
        config.setActivateEmailNotifications(false);
        MailService.initInstance(config);
        mailService = AbstractMailServiceAPI.MailServiceAPIHolder.getInstance();
    }

    @After
    public void tearDown() {
        MailService.clearInstance();
    }

    @Test
    public void testCollectMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendCollectMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewCaseMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewCaseMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testReminderMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendReminderMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDisabledDatasetMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendDisabledDatasetMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewDARRequests() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewDARRequests(Collections.singletonList(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testCancelDARRequestMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendCancelDARRequestMessage(Collections.singletonList(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testFlaggedDarAdminApprovedMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendFlaggedDarAdminApprovedMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testClosedDatasetElectionsMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendClosedDatasetElectionsMessage(Collections.singletonList(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDelegateResponsibilitiesMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendDelegateResponsibilitiesMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherCreatedMessage() throws Exception {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewResearcherCreatedMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

}
