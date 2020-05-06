package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.MailService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;

public class MailServiceTest {

    private static String TO = "to@broadinstitute.org";
    private static String ID = "DUL-123";
    private static String TYPE = "Data Use Limitations";
    private MailService mailService;

    @Mock
    private Writer template;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MailConfiguration config = new MailConfiguration();
        config.setSendGridApiKey("test");
        config.setGoogleAccount("from@broadinstitute.org");
        config.setActivateEmailNotifications(false);
        mailService = new MailService(config);
        doNothing().when(template).write(anyString());
    }

    @Test(expected=MessagingException.class)
    public void testCollectMessageFailure() throws Exception {
        MailConfiguration config = new MailConfiguration();
        config.setSendGridApiKey("test");
        config.setGoogleAccount("from@broadinstitute.org");
        config.setActivateEmailNotifications(true);
        mailService = new MailService(config);
        mailService.sendCollectMessage(Collections.singleton(TO), ID, TYPE, template);
    }

    @Test
    public void testCollectMessage() {
        try {
            mailService.sendCollectMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewCaseMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewCaseMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testReminderMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendReminderMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDisabledDatasetMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendDisabledDatasetMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewDARRequests() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewDARRequests(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testCancelDARRequestMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendCancelDARRequestMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testFlaggedDarAdminApprovedMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendFlaggedDarAdminApprovedMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testClosedDatasetElectionsMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendClosedDatasetElectionsMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDelegateResponsibilitiesMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendDelegateResponsibilitiesMessage(Collections.singleton(TO), template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherCreatedMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewResearcherCreatedMessage(Collections.singleton(TO), template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewHelpReportMessage() {
        Assert.assertNotNull(mailService);
        try {
            mailService.sendNewHelpReportMessage(Collections.singleton(TO), template, "Test");
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

}
