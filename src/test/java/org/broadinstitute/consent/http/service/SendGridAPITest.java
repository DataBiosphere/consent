package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.Writer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.openMocks;

public class SendGridAPITest {

    private static final String TO = "to@broadinstitute.org";
    private static final String ID = "DUL-123";
    private static final String TYPE = "Data Use Limitations";
    private SendGridAPI sendGridAPI;

    @Mock
    private Writer template;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        MailConfiguration config = new MailConfiguration();
        config.setSendGridApiKey("test");
        config.setGoogleAccount("from@broadinstitute.org");
        config.setActivateEmailNotifications(false);
        sendGridAPI = new SendGridAPI(config);
        doNothing().when(template).write(anyString());
    }

// TODO: Update to reflect new api behavior
//    @Test(expected=MessagingException.class)
//    public void testCollectMessageFailure() throws Exception {
//        MailConfiguration config = new MailConfiguration();
//        config.setSendGridApiKey("test");
//        config.setGoogleAccount("from@broadinstitute.org");
//        config.setActivateEmailNotifications(true);
//        sendgridAPI = new SendgridAPI(config);
//        sendgridAPI.sendCollectMessage(TO, ID, TYPE, template);
//    }

    @Test
    public void testCollectMessage() {
        try {
            sendGridAPI.sendCollectMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewCaseMessage() {
        try {
            sendGridAPI.sendNewCaseMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testReminderMessage() {
        try {
            sendGridAPI.sendReminderMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDisabledDatasetMessage() {
        try {
            sendGridAPI.sendDisabledDatasetMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewDARRequests() {
        try {
            sendGridAPI.sendNewDARRequests(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testCancelDARRequestMessage() {
        try {
            sendGridAPI.sendCancelDARRequestMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testFlaggedDarAdminApprovedMessage() {
        try {
            sendGridAPI.sendFlaggedDarAdminApprovedMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testClosedDatasetElectionsMessage() {
        try {
            sendGridAPI.sendClosedDatasetElectionsMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDelegateResponsibilitiesMessage() {
        try {
            sendGridAPI.sendDelegateResponsibilitiesMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherCreatedMessage() {
        try {
            sendGridAPI.sendNewResearcherCreatedMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherApprovedMessage() {
        try {
            sendGridAPI.sendNewResearcherApprovedMessage(TO, template, "Test");
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDataCustodianApprovalMessage() {
        try {
            sendGridAPI.sendDataCustodianApprovalMessage(TO, "Test", template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

}
