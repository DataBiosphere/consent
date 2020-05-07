package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MailServiceDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * This class can be used to functionally test email notifications as well as unit test.
 * To enable functional tests, configure MailService with correct values (i.e. is active, sendgrid key, etc.)
 * Functional test emails will be directed to the private google group:
 *      https://groups.google.com/a/broadinstitute.org/g/duos-dev
 */
public class EmailNotifierServiceTest {

    private EmailNotifierService service;

    @Mock
    private ConsentDAO consentDAO;

    @Mock
    private DataAccessRequestService dataAccessRequestService;

    @Mock
    private VoteDAO voteDAO;

    @Mock
    private ElectionDAO electionDAO;

    @Mock
    private DACUserDAO dacUserDAO;

    @Mock
    private MailMessageDAO emailDAO;

    @Mock
    private MailServiceDAO mailServiceDAO;

    @Mock
    private ResearcherPropertyDAO researcherPropertyDAO;

    private final String defaultAccount = "duos-dev@broadinstitute.org";

    @Before
    public void setUp() {
    }

    private void initService() {
        String serverUrl =  "http://localhost:8000/#/";
        boolean serviceActive = false;

        MockitoAnnotations.initMocks(this.getClass());
        MailConfiguration mConfig = new MailConfiguration();
        mConfig.setActivateEmailNotifications(serviceActive);
        mConfig.setGoogleAccount("");
        mConfig.setSendGridApiKey("");
        MailService mailService = new MailService(mConfig);

        FreeMarkerConfiguration fmConfig = new FreeMarkerConfiguration();
        fmConfig.setDefaultEncoding("UTF-8");
        fmConfig.setTemplateDirectory("/freemarker");
        FreeMarkerTemplateHelper helper = new FreeMarkerTemplateHelper(fmConfig);
        service = new EmailNotifierService(consentDAO, dataAccessRequestService, voteDAO, electionDAO, dacUserDAO,
                emailDAO, mailService, mailServiceDAO, helper, serverUrl, serviceActive, researcherPropertyDAO);
    }

    @Test
    public void testSendDataCustodianApprovalMessage() {
        initService();
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setDarCode("DAR-123456789");
        data.setTranslatedUseRestriction("Translated Use Restriction");
        dar.setData(data);
        List<DatasetMailDTO> datasets = new ArrayList<>();
        datasets.add(new DatasetMailDTO("DS-1 Name", "DS-1 Alias"));
        datasets.add(new DatasetMailDTO("DS-2 Name", "DS-2 Alias"));
        datasets.add(new DatasetMailDTO("DS-3 Name", "DS-3 Alias"));
        String dataDepositorName = "Data Depositor Name";
        String researcherEmail = "researcher@test.com";
        try {
            service.sendDataCustodianApprovalMessage(Collections.singleton(defaultAccount), defaultAccount,
                    dar, datasets, dataDepositorName, researcherEmail);
        } catch (Exception e) {
            fail("Should not fail sending message: " + e);
        }
    }

}
