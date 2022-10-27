package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.mail.SendgridAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.Writer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.openMocks;

public class SendgridAPITest {

    private static final String TO = "to@broadinstitute.org";
    private static final String ID = "DUL-123";
    private static final String TYPE = "Data Use Limitations";
    private SendgridAPI sendgridAPI;

    @Mock
    private Writer template;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        MailConfiguration config = new MailConfiguration();
        config.setSendGridApiKey("test");
        config.setGoogleAccount("from@broadinstitute.org");
        config.setActivateEmailNotifications(false);
        sendgridAPI = new SendgridAPI(config);
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
            sendgridAPI.sendCollectMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewCaseMessage() {
        try {
            sendgridAPI.sendNewCaseMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testReminderMessage() {
        try {
            sendgridAPI.sendReminderMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDisabledDatasetMessage() {
        try {
            sendgridAPI.sendDisabledDatasetMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewDARRequests() {
        try {
            sendgridAPI.sendNewDARRequests(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testCancelDARRequestMessage() {
        try {
            sendgridAPI.sendCancelDARRequestMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testFlaggedDarAdminApprovedMessage() {
        try {
            sendgridAPI.sendFlaggedDarAdminApprovedMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testClosedDatasetElectionsMessage() {
        try {
            sendgridAPI.sendClosedDatasetElectionsMessage(TO, ID, TYPE, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDelegateResponsibilitiesMessage() {
        try {
            sendgridAPI.sendDelegateResponsibilitiesMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherCreatedMessage() {
        try {
            sendgridAPI.sendNewResearcherCreatedMessage(TO, template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testNewResearcherApprovedMessage() {
        try {
            sendgridAPI.sendNewResearcherApprovedMessage(TO, template, "Test");
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

    @Test
    public void testDataCustodianApprovalMessage() {
        try {
            sendgridAPI.sendDataCustodianApprovalMessage(TO, "Test", template);
        } catch (Exception e) {
            Assert.fail("Should not throw exception");
        }
    }

}
