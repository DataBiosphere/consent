package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.HelpReport;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HelpReportTest  extends AbstractTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    private static final Integer USER_ADMIN_ID = 4;
    private static final Integer USER_ID = 1;
    private static final String USER_NAME = "testUser";
    private static final String SUBJECT = "Election can't be created.";
    private static final String DESCRIPTION = "If I have admin role, I can't create elections.";
    private static final String HELP_REPORT_URL = "/report";
    private static final String HELP_REPORT_USER_URL = "/report/user/";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setUp() throws IOException {
        removeReports();
        mockValidateTokenResponse();
    }

    @After
    public void tearDown() throws IOException {
        removeReports();
    }

    @Test
    public void createHelpReport() throws IOException {
        HelpReport adminHelpReport = createHelpReport(USER_ADMIN_ID, SUBJECT, DESCRIPTION);
        HelpReport helpReport1 = createHelpReport(USER_ID, SUBJECT, DESCRIPTION);
        HelpReport helpReport2 = createHelpReport(USER_ID, SUBJECT+1, DESCRIPTION+1);
        HelpReport helpReport3 = createHelpReport(USER_ID, SUBJECT+2, DESCRIPTION+2);
        Client client = ClientBuilder.newClient();
        checkStatus(CREATED, post(client, path2Url(HELP_REPORT_URL), helpReport1));
        checkStatus(CREATED, post(client, path2Url(HELP_REPORT_URL), helpReport2));
        checkStatus(CREATED, post(client, path2Url(HELP_REPORT_URL), helpReport3));
        checkStatus(CREATED, post(client, path2Url(HELP_REPORT_URL), adminHelpReport));
        testRetrieveAllReportsByAdminUser();
        testRetrieveAllReportsByNotAdminUser();
        removeReports();
    }

    private void testRetrieveAllReportsByAdminUser() throws IOException {
        Client client = ClientBuilder.newClient();
        List<HelpReport> reports = getJson(client,  path2Url(HELP_REPORT_USER_URL + USER_ADMIN_ID)).readEntity(new GenericType<List<HelpReport>>() {});
        assertThat(reports.size() == 4);
    }

    private void testRetrieveAllReportsByNotAdminUser() throws IOException {
        Client client = ClientBuilder.newClient();
        List<HelpReport> reports = getJson(client,  path2Url(HELP_REPORT_USER_URL + USER_ID)).readEntity(new GenericType<List<HelpReport>>() {});
        assertThat(reports.size()).isEqualTo(3);
        reports.forEach(r -> {
                    assertThat(r.getUserName().equals(USER_NAME));
                    assertThat(r.getSubject().contains(SUBJECT));
                    assertThat(r.getDescription().contains(DESCRIPTION));
                    assertThat(r.getReportId()).isNotNull();
                    assertThat(r.getCreateDate()).isNotNull();
                }
        );

    }

    private void removeReports() throws IOException {
        Client client = ClientBuilder.newClient();
        List<HelpReport> reports = getJson(client,  path2Url(HELP_REPORT_USER_URL + USER_ADMIN_ID)).readEntity(new GenericType<List<HelpReport>>() {
        });
        reports.forEach(r -> {
                    try {
                        checkStatus(OK,
                                delete(client, path2Url(HELP_REPORT_URL+"/"+r.getReportId())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
        );
    }

    private HelpReport createHelpReport(Integer userId, String subject, String description){
        HelpReport helpReport = new HelpReport();
        helpReport.setUserId(userId);
        helpReport.setSubject(subject);
        helpReport.setDescription(description);
        return helpReport;
    }

}
