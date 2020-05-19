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

    private static final String TO = "to@broadinstitute.org";
    private static final String ID = "DUL-123";
    private static final String TYPE = "Data Use Limitations";
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
        try {
            mailService.sendNewCaseMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testReminderMessage() {
        try {
            mailService.sendReminderMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDisabledDatasetMessage() {
        try {
            mailService.sendDisabledDatasetMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewDARRequests() {
        try {
            mailService.sendNewDARRequests(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testCancelDARRequestMessage() {
        try {
            mailService.sendCancelDARRequestMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testFlaggedDarAdminApprovedMessage() {
        try {
            mailService.sendFlaggedDarAdminApprovedMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testClosedDatasetElectionsMessage() {
        try {
            mailService.sendClosedDatasetElectionsMessage(Collections.singleton(TO), ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDelegateResponsibilitiesMessage() {
        try {
            mailService.sendDelegateResponsibilitiesMessage(Collections.singleton(TO), template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherCreatedMessage() {
        try {
            mailService.sendNewResearcherCreatedMessage(Collections.singleton(TO), template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewHelpReportMessage() {
        try {
            mailService.sendNewHelpReportMessage(Collections.singleton(TO), template, "Test");
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherApprovedMessage() {
        try {
            mailService.sendNewResearcherApprovedMessage(Collections.singleton(TO), template, "Test");
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDataCustodianApprovalMessage() {
        try {
            mailService.sendDataCustodianApprovalMessage(TO, "Test", template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

}
