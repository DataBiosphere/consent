package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyConversionTest extends AbstractTestHelper {

  // -------------------------------------------------------------------------
  // createNewStudyStub()
  // -------------------------------------------------------------------------

  @Test
  void testCreateNewStudyStub_allFieldsPopulated() {
    StudyConversion conversion = new StudyConversion();
    conversion.setName(randomAlphabetic(10));
    conversion.setDescription(randomAlphabetic(20));
    conversion.setPublicVisibility(true);
    conversion.setPiName(randomAlphabetic(10));
    conversion.setPiEmail("pi@example.test");
    conversion.setDataTypes(List.of("Genomics", "Proteomics"));

    Study stub = conversion.createNewStudyStub();

    assertNotNull(stub);
    assertEquals(conversion.getName(), stub.getName());
    assertEquals(conversion.getDescription(), stub.getDescription());
    assertEquals(conversion.getPublicVisibility(), stub.getPublicVisibility());
    assertEquals(conversion.getPiName(), stub.getPiName());
    assertEquals(conversion.getPiEmail(), stub.getPiEmail());
    assertEquals(conversion.getDataTypes(), stub.getDataTypes());
  }

  @Test
  void testCreateNewStudyStub_nullableFieldsAreNull() {
    StudyConversion conversion = new StudyConversion();
    // Leave all fields at their default null values

    Study stub = conversion.createNewStudyStub();

    assertNotNull(stub);
    assertNull(stub.getName());
    assertNull(stub.getDescription());
    assertNull(stub.getPublicVisibility());
    assertNull(stub.getPiName());
    assertNull(stub.getPiEmail());
    assertNull(stub.getDataTypes());
  }

  @Test
  void testCreateNewStudyStub_publicVisibilityFalse() {
    StudyConversion conversion = new StudyConversion();
    conversion.setName(randomAlphabetic(8));
    conversion.setPublicVisibility(false);

    Study stub = conversion.createNewStudyStub();

    assertNotNull(stub);
    assertEquals(Boolean.FALSE, stub.getPublicVisibility());
  }

  @Test
  void testCreateNewStudyStub_dataTypesPreservedExactly() {
    List<String> dataTypes = List.of("WholeGenome", "RNASeq", "Methylation");
    StudyConversion conversion = new StudyConversion();
    conversion.setDataTypes(dataTypes);

    Study stub = conversion.createNewStudyStub();

    assertEquals(dataTypes, stub.getDataTypes());
  }

  // -------------------------------------------------------------------------
  // getStudyProperties()
  // -------------------------------------------------------------------------

  @Test
  void testGetStudyProperties_allThreePresent() {
    StudyConversion conversion = new StudyConversion();
    conversion.setPhenotype("Hypertension");
    conversion.setSpecies("Homo sapiens");
    conversion.setNihAnvilUse("AnVIL_Yes");

    Collection<StudyProperty> props = conversion.getStudyProperties();

    assertNotNull(props);
    assertEquals(3, props.size());
    assertTrue(containsProperty(props, "phenotypeIndication", "Hypertension", PropertyType.String));
    assertTrue(containsProperty(props, "species", "Homo sapiens", PropertyType.String));
    assertTrue(containsProperty(props, "nihAnvilUse", "AnVIL_Yes", PropertyType.String));
  }

  @Test
  void testGetStudyProperties_emptyWhenNoFieldsSet() {
    StudyConversion conversion = new StudyConversion();

    Collection<StudyProperty> props = conversion.getStudyProperties();

    assertNotNull(props);
    assertTrue(props.isEmpty());
  }

  @Test
  void testGetStudyProperties_onlyPhenotype() {
    StudyConversion conversion = new StudyConversion();
    conversion.setPhenotype("Diabetes");

    Collection<StudyProperty> props = conversion.getStudyProperties();

    assertEquals(1, props.size());
    assertTrue(containsProperty(props, "phenotypeIndication", "Diabetes", PropertyType.String));
  }

  @Test
  void testGetStudyProperties_onlySpecies() {
    StudyConversion conversion = new StudyConversion();
    conversion.setSpecies("Mus musculus");

    Collection<StudyProperty> props = conversion.getStudyProperties();

    assertEquals(1, props.size());
    assertTrue(containsProperty(props, "species", "Mus musculus", PropertyType.String));
  }

  @Test
  void testGetStudyProperties_onlyNihAnvilUse() {
    StudyConversion conversion = new StudyConversion();
    conversion.setNihAnvilUse("AnVIL_No");

    Collection<StudyProperty> props = conversion.getStudyProperties();

    assertEquals(1, props.size());
    assertTrue(containsProperty(props, "nihAnvilUse", "AnVIL_No", PropertyType.String));
  }

  @Test
  void testGetStudyProperties_phenotypeAndSpeciesOnly() {
    StudyConversion conversion = new StudyConversion();
    conversion.setPhenotype("Obesity");
    conversion.setSpecies("Rattus norvegicus");

    Collection<StudyProperty> props = conversion.getStudyProperties();

    assertEquals(2, props.size());
    assertTrue(containsProperty(props, "phenotypeIndication", "Obesity", PropertyType.String));
    assertTrue(containsProperty(props, "species", "Rattus norvegicus", PropertyType.String));
  }

  // -------------------------------------------------------------------------
  // Getters / setters (round-trip)
  // -------------------------------------------------------------------------

  @Test
  void testSettersAndGetters_roundTrip() {
    StudyConversion c = new StudyConversion();

    String name = randomAlphabetic(8);
    String description = randomAlphabetic(20);
    List<String> dataTypes = List.of(randomAlphabetic(5), randomAlphabetic(5));
    String phenotype = randomAlphabetic(10);
    String species = randomAlphabetic(10);
    String piName = randomAlphabetic(10);
    String piEmail = "pi@test.example";
    String dataSubmitterEmail = "submitter@test.example";
    Boolean publicVisibility = true;
    String nihAnvilUse = "AnVIL_Yes";
    String datasetName = randomAlphabetic(10);
    DataUse dataUse = new DataUse();
    dataUse.setGeneralUse(true);
    Integer dacId = 42;
    String dataLocation = "gs://bucket/path";
    String url = "https://example.test/study";
    Integer numberOfParticipants = 500;

    c.setName(name);
    c.setDescription(description);
    c.setDataTypes(dataTypes);
    c.setPhenotype(phenotype);
    c.setSpecies(species);
    c.setPiName(piName);
    c.setPiEmail(piEmail);
    c.setDataSubmitterEmail(dataSubmitterEmail);
    c.setPublicVisibility(publicVisibility);
    c.setNihAnvilUse(nihAnvilUse);
    c.setDatasetName(datasetName);
    c.setDataUse(dataUse);
    c.setDacId(dacId);
    c.setDataLocation(dataLocation);
    c.setUrl(url);
    c.setNumberOfParticipants(numberOfParticipants);

    assertEquals(name, c.getName());
    assertEquals(description, c.getDescription());
    assertEquals(dataTypes, c.getDataTypes());
    assertEquals(phenotype, c.getPhenotype());
    assertEquals(species, c.getSpecies());
    assertEquals(piName, c.getPiName());
    assertEquals(piEmail, c.getPiEmail());
    assertEquals(dataSubmitterEmail, c.getDataSubmitterEmail());
    assertEquals(publicVisibility, c.getPublicVisibility());
    assertEquals(nihAnvilUse, c.getNihAnvilUse());
    assertEquals(datasetName, c.getDatasetName());
    assertEquals(dataUse, c.getDataUse());
    assertEquals(dacId, c.getDacId());
    assertEquals(dataLocation, c.getDataLocation());
    assertEquals(url, c.getUrl());
    assertEquals(numberOfParticipants, c.getNumberOfParticipants());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private boolean containsProperty(
      Collection<StudyProperty> props, String key, Object value, PropertyType type) {
    return props.stream()
        .anyMatch(p -> key.equals(p.getKey()) && value.equals(p.getValue()) && type == p.getType());
  }
}
