package org.broadinstitute.consent.http.mail.freemarker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class FreeMarkerTemplateHelperTest {

    private FreeMarkerTemplateHelper helper;

    @Mock
    private FreeMarkerConfiguration freeMarkerConfig;

    @BeforeEach
    public void setUp() throws IOException {
        openMocks(this);
        when(freeMarkerConfig.getTemplateDirectory()).thenReturn("/freemarker");
        when(freeMarkerConfig.getDefaultEncoding()).thenReturn("UTF-8");
        helper = new FreeMarkerTemplateHelper(freeMarkerConfig);

    }

    @Test
    public void testGetDisabledDatasetsTemplate() throws Exception {
        Writer template = helper.getDisabledDatasetsTemplate("DatasetTemp User", sampleDatasets().stream().map(ds -> ds.getObjectId()).collect(Collectors.toList()), "entityId", "serverUrl");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Disabled Datasets Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello DatasetTemp User,"));
        assertTrue(templateString.contains("DS-101"));
        assertTrue(templateString.contains("DS-102"));
        assertTrue(templateString.contains("DS-103"));
    }

    @Test
    public void testGetNewCaseTemplate() throws Exception {
        Writer template = helper.getNewCaseTemplate("NewCase User", "DARELECTION-1", "DAR-1", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - New Case Notification"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello NewCase User,"));
    }

    @Test
    public void testGetReminderTemplate() throws Exception {
        Writer template = helper.getReminderTemplate("Reminder User", "DARELECTION-1", "DAR-1", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Vote Reminder"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Reminder User,"));
    }

    @Test
    public void testGetNewDARRequestTemplate() throws Exception {
        Writer template = helper.getNewDARRequestTemplate("localhost:1234", "Admin", "Entity");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertEquals("Broad Data Use Oversight System - New Data Access Request", parsedTemplate.title());
        Element userNameElement = parsedTemplate.getElementById("userName");
        assertNotNull(userNameElement);
        assertNotNull(userNameElement.text());
        assertEquals("Hello Admin,", userNameElement.text());
    }

    @Test
    public void testGetClosedDatasetElectionsTemplate() throws Exception {
        Writer template = helper.getClosedDatasetElectionsTemplate(getClosedDsElections(), "DarCode", "SomeType", "localhost:1234");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - Closed Dataset Elections"));
        assertTrue(parsedTemplate.getElementById("userName").text().equals("Hello Admin,"));
    }

    @Test
    public void testGetNewResearcherLibraryRequestTemplate() throws Exception {
        Writer template = helper.getNewResearcherLibraryRequestTemplate("John Doe", "http://localhost:8000/#/");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);

        assertTrue(parsedTemplate.title().equals("Broad Data Use Oversight System - New Researcher Library Request"));

        assertTrue(
                parsedTemplate
                        .getElementById("content")
                        .text()
                        .contains("A researcher from your institution, John Doe, has registered in DUOS"));

        assertTrue(
                parsedTemplate
                        .getElementById("serverUrl")
                        .attr("href")
                        .equals("http://localhost:8000/#/"));

        // no unspecified values
        assertFalse(templateString.contains("${"));
    }

    @Test
    public void testGetDataCustodianApprovalTemplate() throws Exception {
        List<DatasetMailDTO> datasetMailDTOs = List.of();
        Writer template = helper.getDataCustodianApprovalTemplate(datasetMailDTOs, "Depositor", "Dar Code", "researcher@email.com");
        String templateString = template.toString();
        final Document parsedTemplate = getAsHtmlDoc(templateString);
        assertEquals("Broad Data Use Oversight System - Researcher - DAR Approved Notification", parsedTemplate.title());
        assertTrue(
                parsedTemplate
                        .getElementById("content")
                        .text()
                        .contains("researcher@email.com was approved by the DAC for the following datasets"));
        // no unspecified values
        assertFalse(templateString.contains("${"));
    }

    /* Helper methods */

    private Document getAsHtmlDoc(String parsedHtml) {
        return Jsoup.parse(parsedHtml);
    }

    private Dataset ds1 = new Dataset(1, "DS-101", "Dataset 1", new Date(), true);
    private Dataset ds2 = new Dataset(2, "DS-102", "Dataset 2", new Date(), true);
    private Dataset ds3 = new Dataset(3, "DS-103", "Dataset 3", new Date(), true);
    private User testUser = new User(1, "testuser@email.com", "Test User", new Date(), null);
    private Election e1 = new Election(1, "DataSet", "Closed", new Date(), "DAR-1", null, true, 1);
    private Election e2 = new Election(2, "DataSet", "Closed", new Date(), "DAR-1", null, false, 2);
    private Election e3 = new Election(3, "DataSet", "Closed", new Date(), "DAR-2", null, true, 1);

    private List<Dataset> sampleDatasets() {
        return Arrays.asList(ds1, ds2, ds3);
    }

    private Map<User, List<Dataset>> getApprovedDarMap() {
        Map<User, List<Dataset>> approvedDarMap = new HashMap<>();
        approvedDarMap.put(testUser, sampleDatasets());
        return approvedDarMap;
    }

    private Map<String, List<Election>> getClosedDsElections() {
        Map<String, List<Election>> closedDatasetElections = new HashMap<>();
        closedDatasetElections.put("DAR-1", Arrays.asList(e1, e2));
        closedDatasetElections.put("DAR-2", Arrays.asList(e3));
        return closedDatasetElections;
    }
}
