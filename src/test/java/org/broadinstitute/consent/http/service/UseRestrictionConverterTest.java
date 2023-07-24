package org.broadinstitute.consent.http.service;

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

public class UseRestrictionConverterTest implements WithMockServer {

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
                    "Samples are restricted for use under the following conditions:\n"
                        + "Data is limited for health/medical/biomedical research. [HMB]\n"
                        + "Commercial use is not prohibited.\n"
                        + "Data use for methods development research irrespective of the specified data use limitations is not prohibited.\n"
                        + "Restrictions for use as a control set for diseases other than those defined were not specified."));
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
  public void testTranslateDataUsePurpose() {
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
  public void testTranslateDataUseDataset() {
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
  public void testFailedDataUseTranslateConverterConnection() {
    mockDataUseTranslateFailure();

    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = new DataUseBuilder().setHmbResearch(true).build();
    String translation = converter.translateDataUse(dataUse, DataUseTranslationType.PURPOSE);
    assertNull(translation);
  }

  /*
   * Testing a fleshed out DataUse.
   */
  @Test
  public void testParseDataUse() {
    String json = "{ " +
        "\"methods\":true, " +
        "\"population\":true, " +
        "\"controls\":true, " +
        "\"ontologies\":[  " +
        "      {  " +
        "         \"id\":\"http://purl.obolibrary.org/obo/DOID_4023\"," +
        "         \"label\":\"linitis-plastica\"," +
        "         \"definition\":null," +
        "         \"synonyms\":[  " +
        "            \"Linitis plastica (morphologic abnormality)\"," +
        "            \"Leather-bottle stomach\"" +
        "         ]" +
        "      }" +
        "]," +
        "\"forProfit\":true," +
        "\"onegender\":true," +
        "\"pediatric\":true," +
        "\"gender\":\"F\"" +
        "}";

    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = converter.parseDataUsePurpose(json);
    assertNotNull(dataUse);
    assertTrue(dataUse.getMethodsResearch());
    assertTrue(dataUse.getPopulationStructure());
    assertTrue(dataUse.getControlSetOption().equalsIgnoreCase("Yes"));
    assertTrue(
        dataUse.getDiseaseRestrictions().contains("http://purl.obolibrary.org/obo/DOID_4023"));
    assertTrue(dataUse.getCommercialUse());
    assertTrue(dataUse.getPediatric());
    assertTrue(dataUse.getGender().equalsIgnoreCase("Female"));
  }

  /*
   * Testing a DataUse with invalid ontologies.
   */
  @Test
  public void testParseDataUseInvalidOntologiesCase1() {
    String json = "{ " +
        "\"hmb\":true, " +
        "\"ontologies\":[{},{},{}]" +
        "}";

    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = converter.parseDataUsePurpose(json);
    assertNotNull(dataUse);
    assertNull(dataUse.getDiseaseRestrictions());
  }

  /*
   * Testing a DataUse with invalid ontologies.
   */
  @Test
  public void testParseDataUseInvalidOntologiesCase2() {
    String json = "{ " +
        "\"ontologies\":[null]" +
        "}";

    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataUse dataUse = converter.parseDataUsePurpose(json);
    assertNotNull(dataUse);
    assertNull(dataUse.getDiseaseRestrictions());
  }

  /*
   * Test that the DataUse parser does not set false values incorrectly
   */
  @Test
  public void testTranslateFalseValues() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    String json = "{ " +
        "\"methods\":false, " +
        "\"population\":false, " +
        "\"controls\":false, " +
        "\"poa\":false, " +
        "\"hmb\":false " +
        "}";
    DataUse dataUse = converter.parseDataUsePurpose(json);
    assertNull(dataUse.getMethodsResearch());
    assertNull(dataUse.getPopulationStructure());
    assertNull(dataUse.getControlSetOption());
    assertNull(dataUse.getPopulationOriginsAncestry());
    assertNull(dataUse.getHmbResearch());
  }

  /*
   * Test that the DataUse parser sets true values correctly
   */
  @Test
  public void testTranslateTrueValues() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    String json = "{ " +
        "\"methods\":true, " +
        "\"population\":true, " +
        "\"controls\":true, " +
        "\"poa\":true, " +
        "\"hmb\":true " +
        "}";
    DataUse dataUse = converter.parseDataUsePurpose(json);
    assertTrue(dataUse.getMethodsResearch());
    assertTrue(dataUse.getPopulationStructure());
    assertTrue(dataUse.getControlSetOption().equalsIgnoreCase("Yes"));
    assertTrue(dataUse.getPopulationOriginsAncestry());
    assertTrue(dataUse.getHmbResearch());
  }

  @Test
  public void testParseDataUsePurposeEmpty() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNull(dataUse.getGeneralUse());
    assertNull(dataUse.getMethodsResearch());
    assertNull(dataUse.getControlSetOption());
    assertNull(dataUse.getDiseaseRestrictions());
    assertNull(dataUse.getCommercialUse());
    assertNull(dataUse.getGender());
    assertNull(dataUse.getPediatric());
    assertNull(dataUse.getPopulationOriginsAncestry());
    assertNull(dataUse.getHmbResearch());
    assertNull(dataUse.getOther());
    assertNull(dataUse.getOtherRestrictions());
    assertNull(dataUse.getSecondaryOther());
  }

  @Test
  public void testParseDataUsePurposeMethods() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setMethods(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getMethodsResearch());
  }

  @Test
  public void testParseDataUsePurposePopulation() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPopulation(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPopulationStructure());
  }

  @Test
  public void testParseDataUsePurposeControls() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setControls(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getControlSetOption());
  }

  @Test
  public void testParseDataUsePurposeDisease() {
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
  }

  @Test
  public void testParseDataUsePurposeCommercial() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setForProfit(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getCommercialUse());
  }

  @Test
  public void testParseDataUsePurposeGender() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOneGender(true);
    dar.getData().setGender("F");
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getGender());
  }

  @Test
  public void testParseDataUsePurposePediatric() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPediatric(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPediatric());
  }

  @Test
  public void testParseDataUsePurposePOA() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setPoa(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getPopulationOriginsAncestry());
  }

  @Test
  public void testParseDataUsePurposeHMB() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setHmb(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getHmbResearch());
  }

  @Test
  public void testParseDataUsePurposeOther1() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOther(true);
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertTrue(dataUse.getOtherRestrictions());
  }

  @Test
  public void testParseDataUsePurposeOther2() {
    Client client = ClientBuilder.newClient();
    UseRestrictionConverter converter = new UseRestrictionConverter(client, config());
    DataAccessRequest dar = createDataAccessRequest();
    dar.getData().setOtherText("other");
    DataUse dataUse = converter.parseDataUsePurpose(dar);
    assertNotNull(dataUse.getOther());
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
