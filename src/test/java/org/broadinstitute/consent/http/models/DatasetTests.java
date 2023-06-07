package org.broadinstitute.consent.http.models;


import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.junit.jupiter.api.Test;

public class DatasetTests {

  @Test
  public void testParseIdentifierToAlias() {
    assertEquals(3, (int) Dataset.parseIdentifierToAlias("DUOS-3"));
    assertEquals(3, (int) Dataset.parseIdentifierToAlias("DUOS-000003"));
    assertEquals(123456, (int) Dataset.parseIdentifierToAlias("DUOS-123456"));

    assertThrows(IllegalArgumentException.class,
        () -> Dataset.parseIdentifierToAlias("asdf-123456"));
    assertThrows(IllegalArgumentException.class,
        () -> Dataset.parseIdentifierToAlias("DUOS-1234 56"));
    assertThrows(IllegalArgumentException.class,
        () -> Dataset.parseIdentifierToAlias("DUOS-1234as56"));
  }

  @Test
  public void testIsDatasetMatchName() {
    String name = RandomStringUtils.randomAlphanumeric(20);

    Dataset ds = new Dataset();
    ds.setName(name);

    assertTrue(ds.isDatasetMatch(name, false));
    assertTrue(ds.isDatasetMatch(name.substring(5, 10), false));
    assertTrue(ds.isDatasetMatch(name.substring(10, 15), false));

    assertFalse(ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(30), false));
  }

  @Test
  public void testIsDatasetMatchNameCaseIndependent() {
    String name = RandomStringUtils.randomAlphabetic(20);

    Dataset ds = new Dataset();
    ds.setName(name.toLowerCase());

    assertTrue(ds.isDatasetMatch(name.toUpperCase(), false));
    assertTrue(ds.isDatasetMatch(name.toUpperCase().substring(7, 14), false));
  }

  @Test
  public void testIsDatasetMatchDatasetProperty() {
    Dataset ds = new Dataset();

    String value = RandomStringUtils.randomAlphanumeric(20);

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isDatasetMatch(value, false));
    assertFalse(ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(25), false));
  }

  @Test
  public void testIsDatasetMatchIdentifier() {
    Dataset ds = new Dataset();
    ds.setAlias(1235);

    assertTrue(ds.isDatasetMatch("DUOS-001235", false));
    assertTrue(ds.isDatasetMatch("DUOS", false));
    assertTrue(ds.isDatasetMatch("123", false));
    assertTrue(ds.isDatasetMatch("001235", false));
    assertFalse(ds.isDatasetMatch("DUOS-123456", false));
  }

  @Test
  public void testIsDatasetMatchDataUseCommercial() {
    Dataset ds = new Dataset();

    assertFalse(ds.isDatasetMatch("collaborator", false));

    DataUse du = new DataUseBuilder().setCollaboratorRequired(true).build();

    ds.setDataUse(du);

    assertTrue(ds.isDatasetMatch("collaborator", false));
    assertTrue(ds.isDatasetMatch("collab", false));
  }

  @Test
  public void testIsDatasetMatchDataUseIrb() {
    Dataset ds = new Dataset();

    assertFalse(ds.isDatasetMatch("irb", false));

    DataUse du = new DataUse();
    du.setEthicsApprovalRequired(true);

    ds.setDataUse(du);

    assertTrue(ds.isDatasetMatch("irb", false));
    assertTrue(ds.isDatasetMatch("irb", false));
  }

  @Test
  public void testIsDatasetMatchDataUseDiseases() {
    Dataset ds = new Dataset();

    assertFalse(ds.isDatasetMatch("cancer", false));
    assertFalse(ds.isDatasetMatch("alzheimers", false));

    DataUse du = new DataUse();
    du.setDiseaseRestrictions(List.of("cancer", "alzheimers"));

    ds.setDataUse(du);

    assertTrue(ds.isDatasetMatch("cancer", false));
    assertTrue(ds.isDatasetMatch("alzheimers", false));
  }

  @Test
  public void testIsDatasetMatchMultipleTerms() {
    Dataset ds = new Dataset();

    ds.setName("asdf");
    ds.setAlias(1234);

    assertTrue(ds.isDatasetMatch("ASD DUOS-001234", false));
    assertTrue(ds.isDatasetMatch("asdf 123", false));

    assertFalse(ds.isDatasetMatch("asf DUOS-001234", false));
    assertFalse(ds.isDatasetMatch("asd 122", false));

  }

  @Test
  public void testIsDatasetMatchOpenAccessTrue() {
    Dataset ds = new Dataset();

    String value = "true";

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyName("Open Access");
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    dsp.setSchemaProperty("consentGroup.openAccess");
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isDatasetMatch(value, true));
    assertFalse(ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(25), true));
  }

  @Test
  public void testIsDatasetMatchOpenAccessFalse() {
    Dataset ds = new Dataset();

    String value = "false";

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyName("Open Access");
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    dsp.setSchemaProperty("consentGroup.openAccess");
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isDatasetMatch(value, false));
    assertFalse(ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(25), true));
  }
}
