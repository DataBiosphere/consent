package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.testcontainers.containers.MockServerContainer;

class UseRestrictionConverterTest implements WithMockServer {

  private MockServerClient client;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  @BeforeAll
  public static void setUp() {
    container.start();
  }

  @AfterAll
  public static void tearDown() {
    container.stop();
  }

  @BeforeEach
  public void startUp() {
    client = new MockServerClient(container.getHost(), container.getServerPort());
    client.reset();
  }

  private void mockDataUseTranslateSuccess() {
    client
        .when(request().withMethod("POST").withPath("/translate"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody(
                    """
                        Samples are restricted for use under the following conditions:
                        Data is limited for health/medical/biomedical research. [HMB]
                        Commercial use is not prohibited.
                        Data use for methods development research irrespective of the specified data use limitations is not prohibited.
                        Restrictions for use as a control set for diseases other than those defined were not specified."""));
  }

  private void mockDataUseTranslateFailure() {
    client
        .when(request().withMethod("POST").withPath("/translate"))
        .respond(
            response()
                .withStatusCode(500)
                .withHeaders(new Header("Content-Type", MediaType.APPLICATION_JSON))
                .withBody("Exception")
        );
  }

  public ServicesConfiguration config() {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setLocalURL("http://localhost:8180/");
    config.setOntologyURL(getRootUrl(container));
    return config;
  }

  /*
   * Test that the UseRestrictionConverter makes a call to the ontology service and gets back a valid translation
   */
  @Test
  void testTranslateDataUsePurpose() {
    mockDataUseTranslateSuccess();
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
    String translation = converter.translateDataUse(dataUse, DataUseTranslationType.PURPOSE);
    assertNotNull(translation);
  }

  /*
   * Test that the UseRestrictionConverter makes a call to the ontology service and gets back a valid translation
   */
  @Test
  void testTranslateDataUseDataset() {
    mockDataUseTranslateSuccess();
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
    String translation = converter.translateDataUse(dataUse, DataUseTranslationType.DATASET);
    assertNotNull(translation);
  }

  /*
   * Test that when the UseRestrictionConverter makes a failed call to the ontology service, a null is returned.
   */
  @Test
  void testFailedDataUseTranslateConverterConnection() {
    mockDataUseTranslateFailure();

    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
    String translation = converter.translateDataUse(dataUse, DataUseTranslationType.PURPOSE);
    assertNull(translation);
  }

  @Test
  void testParseDataUsePurposeEmpty() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNull(dataUse.getGeneralUse());
    assertNull(dataUse.getDiseaseRestrictions());
    assertNull(dataUse.getHmbResearch());
    assertNull(dataUse.getPopulationOriginsAncestry());
    assertNull(dataUse.getMethodsResearch());
    assertNull(dataUse.getNonProfitUse());
    assertNull(dataUse.getOther());
    assertNull(dataUse.getSecondaryOther());
    assertNull(dataUse.getEthicsApprovalRequired());
    assertNull(dataUse.getCollaboratorRequired());
    assertNull(dataUse.getGeographicalRestrictions());
    assertNull(dataUse.getGeneticStudiesOnly());
    assertNull(dataUse.getPublicationResults());
    assertNull(dataUse.getPublicationMoratorium());
    assertNull(dataUse.getControls());
    assertNull(dataUse.getGender());
    assertNull(dataUse.getPediatric());
    assertNull(dataUse.getPopulation());
    assertNull(dataUse.getIllegalBehavior());
    assertNull(dataUse.getSexualDiseases());
    assertNull(dataUse.getStigmatizeDiseases());
    assertNull(dataUse.getVulnerablePopulations());
    assertNull(dataUse.getPsychologicalTraits());
    assertNull(dataUse.getNotHealth());
  }

  @Test
  void testParseDataUsePurposeFalseAsNull() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();

    data.setMethods(false);
    data.setPopulation(false);
    data.setControls(false);
    data.setOntologies(List.of());
    data.setForProfit(false);
    data.setGender("");
    data.setPediatric(false);
    data.setIllegalBehavior(false);
    data.setSexualDiseases(false);
    data.setStigmatizedDiseases(false);
    data.setVulnerablePopulation(false);
    data.setPsychiatricTraits(false);
    data.setNotHealth(false);

    dar.setData(data);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNull(dataUse.getGeneralUse());
    assertNull(dataUse.getDiseaseRestrictions());
    assertNull(dataUse.getHmbResearch());
    assertNull(dataUse.getPopulationOriginsAncestry());
    assertNull(dataUse.getMethodsResearch());
    assertNotNull(dataUse.getNonProfitUse());
    assertNull(dataUse.getOther());
    assertNull(dataUse.getSecondaryOther());
    assertNull(dataUse.getEthicsApprovalRequired());
    assertNull(dataUse.getCollaboratorRequired());
    assertNull(dataUse.getGeographicalRestrictions());
    assertNull(dataUse.getGeneticStudiesOnly());
    assertNull(dataUse.getPublicationResults());
    assertNull(dataUse.getPublicationMoratorium());
    assertNull(dataUse.getControls());
    assertNull(dataUse.getGender());
    assertNull(dataUse.getPediatric());
    assertNull(dataUse.getPopulation());
    assertNull(dataUse.getIllegalBehavior());
    assertNull(dataUse.getSexualDiseases());
    assertNull(dataUse.getStigmatizeDiseases());
    assertNull(dataUse.getVulnerablePopulations());
    assertNull(dataUse.getPsychologicalTraits());
    assertNull(dataUse.getNotHealth());
  }

  @Test
  void testParseDataUsePurposeMethods() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setMethods(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getMethodsResearch());
  }

  @Test
  void testParseDataUsePurposeControls() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setControls(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getControls());
  }

  @Test
  void testParseDataUsePurposeDisease() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    OntologyEntry entry = new OntologyEntry();
    entry.setId("id");
    entry.setDefinition("description");
    entry.setLabel("label");
    dar.getData().setOntologies(List.of(entry));
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getDiseaseRestrictions());
    assertFalse(dataUse.getDiseaseRestrictions().isEmpty());
  }

  @Test
  void testParseDataUsePurposeNonProfit() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setForProfit(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertFalse(dataUse.getNonProfitUse());
  }

  @Test
  void testParseDataUsePurposeGender() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOneGender(true);
    dar.getData().setGender("F");
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getGender());
  }

  @Test
  void testParseDataUsePurposePediatric() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPediatric(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPediatric());
  }

  @Test
  void testParseDataUsePurposeHMB() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setHmb(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getHmbResearch());
  }

  @Test
  void testParseDataUsePurposeOther() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOther(true);
    dar.getData().setOtherText("Other Text");
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getOtherRestrictions());
  }

  @Test
  void testParseDataUseIllegalBehavior() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setIllegalBehavior(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getIllegalBehavior());
  }

  @Test
  void testParseDataUseSexualDiseases() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setSexualDiseases(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getSexualDiseases());
  }

  @Test
  void testParseDataUseStigmatizeDiseases() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setStigmatizedDiseases(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getStigmatizeDiseases());
  }

  @Test
  void testParseDataUseVulnerablePopulations() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setVulnerablePopulation(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getVulnerablePopulations());
  }

  @Test
  void testParseDataUsePsychologicalTraits() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPsychiatricTraits(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPsychologicalTraits());
  }

  @Test
  void testParseDataUseNotHealth() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setNotHealth(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getNotHealth());
  }

  private DataAccessRequest createDataAccessRequest() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    data.setReferenceId(dar.getReferenceId());
    dar.setData(data);
    return dar;
  }

}
