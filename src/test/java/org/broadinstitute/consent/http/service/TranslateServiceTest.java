package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.UUID;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

public class TranslateServiceTest {

  private TranslateServiceImpl translateService;

  @Before
  public void setUp() {
    translateService = new TranslateServiceImpl();
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionEmpty() {
    Document dar = new Document();
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionOntologies() {
    Gson gson = new Gson();
    String darDataString = "{\n"
        + "  \"hmb\": true,\n"
        + "  \"diseases\": true,\n"
        + "  \"ontologies\": [\n"
        + "    {\n"
        + "      \"id\": \"http://purl.obolibrary.org/obo/DOID_3393\",\n"
        + "      \"label\": \"coronary artery disease\",\n"
        + "      \"synonyms\": [\n"
        + "        \"CHD (coronary heart disease)\",\n"
        + "        \"coronary heart disease\",\n"
        + "        \"chronic myocardial ischaemia\",\n"
        + "        \"myocardial ischemia\",\n"
        + "        \"Coronary disease\",\n"
        + "        \"coronary arteriosclerosis\",\n"
        + "        \"ischemic heart disease\"\n"
        + "      ],\n"
        + "      \"definition\": \"An artery disease that is characterized by plaque building up along the inner walls of the arteries of the heart resulting in a narrowing of the arteries and a reduced blood supply to the cardiac muscles.\"\n"
        + "    },\n"
        + "    {\n"
        + "      \"id\": \"http://purl.obolibrary.org/obo/DOID_5844\",\n"
        + "      \"label\": \"myocardial infarction\",\n"
        + "      \"synonyms\": [\n"
        + "        \"Myocardial infarct\",\n"
        + "        \"heart attack\"\n"
        + "      ],\n"
        + "      \"definition\": \"A coronary artery disease characterized by myocardial cell death (myocardial necrosis) due to prolonged ischaemia.\"\n"
        + "    }\n"
        + "  ]\n"
        + "}\n";
    DataAccessRequestData data = DataAccessRequestData.fromString(darDataString);
    data.setReferenceId(UUID.randomUUID().toString());
    Document dar = Document.parse(gson.toJson(data));
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("myocardial infarction"));
    assertTrue(translation.contains("coronary artery disease"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionOntologiesErrorCase1() {
    Gson gson = new Gson();
    String darDataString = "{\n"
        + "  \"hmb\": true,\n"
        + "  \"diseases\": true,\n"
        + "  \"ontologies\": [{}]\n"
        + "}\n";
    DataAccessRequestData data = DataAccessRequestData.fromString(darDataString);
    data.setReferenceId(UUID.randomUUID().toString());
    Document dar = Document.parse(gson.toJson(data));
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertFalse(translation.contains("Data will be used to study"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionOntologiesErrorCase2() {
    Gson gson = new Gson();
    String darDataString = "{\n"
        + "  \"hmb\": true,\n"
        + "  \"diseases\": true,\n"
        + "  \"ontologies\": [{}, {}]\n"
        + "}\n";
    DataAccessRequestData data = DataAccessRequestData.fromString(darDataString);
    data.setReferenceId(UUID.randomUUID().toString());
    Document dar = Document.parse(gson.toJson(data));
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    System.out.println(translation);
    assertNotNull(translation);
    assertFalse(translation.contains("Data will be used to study"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionOntologiesErrorCase3() {
    Gson gson = new Gson();
    String darDataString = "{\n"
        + "  \"hmb\": true,\n"
        + "  \"diseases\": true,\n"
        + "  \"ontologies\": [null]\n"
        + "}\n";
    DataAccessRequestData data = DataAccessRequestData.fromString(darDataString);
    data.setReferenceId(UUID.randomUUID().toString());
    Document dar = Document.parse(gson.toJson(data));
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    System.out.println(translation);
    assertNotNull(translation);
    assertFalse(translation.contains("Data will be used to study"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionMethods() {
    Document dar = new Document();
    dar.put(DarConstants.METHODS, Boolean.TRUE);
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("method"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionControls() {
    Document dar = new Document();
    dar.put(DarConstants.CONTROLS, Boolean.TRUE);
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("control"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionPopulation() {
    Document dar = new Document();
    dar.put(DarConstants.POPULATION, Boolean.TRUE);
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("population"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionForProfit() {
    Document dar = new Document();
    dar.put(DarConstants.FOR_PROFIT, Boolean.TRUE);
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("commercial"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionNotForProfit() {
    Document dar = new Document();
    dar.put(DarConstants.FOR_PROFIT, Boolean.FALSE);
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("not"));
    assertTrue(translation.contains("commercial"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionFemale() {
    Document dar = new Document();
    dar.put(DarConstants.ONE_GENDER, Boolean.TRUE);
    dar.put(DarConstants.GENDER, "F");
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("female"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionMale() {
    Document dar = new Document();
    dar.put(DarConstants.ONE_GENDER, Boolean.TRUE);
    dar.put(DarConstants.GENDER, "M");
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("male"));
  }

  @Test
  public void testGenerateStructuredTranslatedRestrictionPediatric() {
    Document dar = new Document();
    dar.put(DarConstants.PEDIATRIC, Boolean.TRUE);
    String translation = translateService.generateStructuredTranslatedRestriction(dar, false);
    assertNotNull(translation);
    assertTrue(translation.contains("pediatric"));
  }
}
