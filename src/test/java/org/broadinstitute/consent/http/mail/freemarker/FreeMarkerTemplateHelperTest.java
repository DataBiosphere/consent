package org.broadinstitute.consent.http.mail.freemarker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreeMarkerTemplateHelperTest {

  private FreeMarkerTemplateHelper helper;

  @Mock
  private FreeMarkerConfiguration freeMarkerConfig;

  @BeforeEach
  public void setUp() throws IOException {
    when(freeMarkerConfig.getTemplateDirectory()).thenReturn("/freemarker");
    when(freeMarkerConfig.getDefaultEncoding()).thenReturn("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);

  }

  @Test
  void testGetDisabledDatasetsTemplate() throws Exception {
    Writer template = helper.getDisabledDatasetsTemplate("DatasetTemp User",
        sampleDatasets().stream().map(Dataset::getObjectId).collect(Collectors.toList()),
        "entityId", "serverUrl");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - Disabled Datasets Notification",
        parsedTemplate.title());
    assertEquals("Hello DatasetTemp User,", parsedTemplate.getElementById("userName").text());
    assertTrue(templateString.contains("DS-101"));
    assertTrue(templateString.contains("DS-102"));
    assertTrue(templateString.contains("DS-103"));
  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    Writer template = helper.getNewCaseTemplate("NewCase User", "DARELECTION-1", "DAR-1",
        "localhost:1234");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - New DAR ready for your vote",
        parsedTemplate.title());
    assertEquals("Hello NewCase User,", parsedTemplate.getElementById("userName").text());
  }

  @Test
  void testGetReminderTemplate() throws Exception {
    Writer template = helper.getReminderTemplate("Reminder User", "DARELECTION-1", "DAR-1",
        "localhost:1234");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - Your vote was requested for a DAR",
        parsedTemplate.title());
    assertEquals("Hello Reminder User,", parsedTemplate.getElementById("userName").text());
  }

  @Test
  void testGetNewDARRequestTemplate() throws Exception {
    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    Dataset d1 = new Dataset();
    d1.setDacId(1);
    d1.setDatasetName("Dataset-01");
    d1.setDataSetId(1);
    d1.setAlias(1);
    d1.setDatasetIdentifier();

    Map<String, List<String>> dacDatasetGroups = new HashMap<>();
    dacDatasetGroups.put(dac.getName(), List.of(d1.getDatasetIdentifier()));

    Writer template = helper.getNewDARRequestTemplate(
        "localhost:1234",
        "Admin",
        dacDatasetGroups,
        "ResearcherName",
        "DAR-01"
    );
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals("Broad Data Use Oversight System - New DAR submitted to your DAC",
        parsedTemplate.title());
    Element userNameElement = parsedTemplate.getElementById("userName");
    assertNotNull(userNameElement);
    assertNotNull(userNameElement.text());
    assertEquals("Hello Admin,", userNameElement.text());
  }

  @Test
  void testGetNewResearcherLibraryRequestTemplate() throws Exception {
    Writer template = helper.getNewResearcherLibraryRequestTemplate("John Doe",
        "http://localhost:8000/#/");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);

    assertEquals(
        "Broad Data Use Oversight System - Request from your researcher for Library Card permissions",
        parsedTemplate.title());

    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains("A researcher from your institution, John Doe, has registered in DUOS"));

    assertEquals("http://localhost:8000/#/", parsedTemplate
        .getElementById("serverUrl")
        .attr("href"));

    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetDataCustodianApprovalTemplate() throws Exception {
    List<DatasetMailDTO> datasetMailDTOs = List.of();
    Writer template = helper.getDataCustodianApprovalTemplate(datasetMailDTOs, "Depositor",
        "Dar Code", "researcher@email.com");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - Researcher - A researcher was approved for your dataset",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains("researcher@email.com was approved by the DAC for the following datasets"));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetDatasetSubmittedTemplate() throws Exception {
    Writer template = helper.getDatasetSubmittedTemplate("dacChairName", "dataSubmitterName",
        "testDataset",
        "dacName");
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - Signing Official - Dataset Submitted Notification",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            "A new dataset, testDataset, has been submitted to your DAC, dacName by dataSubmitterName. Please log in to DUOS to review and accept or reject management of this dataset."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  @Test
  void testGetDaaRequestTemplate() throws Exception {
    String signingOfficialUserName = RandomStringUtils.randomAlphabetic(10);
    String userName = RandomStringUtils.randomAlphabetic(10);
    String daaName = RandomStringUtils.randomAlphabetic(10);
    String serverUrl = RandomStringUtils.randomAlphabetic(10);
    Writer template = helper.getDaaRequestTemplate(signingOfficialUserName, userName,
        daaName,
        serverUrl);
    String templateString = template.toString();
    final Document parsedTemplate = getAsHtmlDoc(templateString);
    assertEquals(
        "Broad Data Use Oversight System - New Data Access Agreement-Library Card Relationship Request for your Institution",
        parsedTemplate.title());
    assertTrue(parsedTemplate
        .getElementById("userName")
        .text()
        .contains(
            "Hello " + signingOfficialUserName + ","));
    assertTrue(parsedTemplate
        .getElementById("content")
        .text()
        .contains(
            userName + " has registered with your institution and is requesting you approve them under the " + daaName + " data access agreement, so that they can request access to data."));
    assertTrue(parsedTemplate
        .getElementById("link")
        .text()
        .contains(
            "Please login to review " + userName + "'s Data Access Agreements."));
    // no unspecified values
    assertFalse(templateString.contains("${"));
  }

  /* Helper methods */

  private Document getAsHtmlDoc(String parsedHtml) {
    return Jsoup.parse(parsedHtml);
  }

  private Dataset ds1 = new Dataset(1, "DS-101", "Dataset 1", new Date());
  private Dataset ds2 = new Dataset(2, "DS-102", "Dataset 2", new Date());
  private Dataset ds3 = new Dataset(3, "DS-103", "Dataset 3", new Date());
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
    closedDatasetElections.put("DAR-1", List.of(e1, e2));
    closedDatasetElections.put("DAR-2", List.of(e3));
    return closedDatasetElections;
  }
}
