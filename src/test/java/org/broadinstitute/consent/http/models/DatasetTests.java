package org.broadinstitute.consent.http.models;


import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

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
  public void testIsStringMatchName() {
    String name = RandomStringUtils.randomAlphanumeric(20);

    Dataset ds = new Dataset();
    ds.setName(name);

    assertTrue(ds.isStringMatch(name, null));
    assertTrue(ds.isStringMatch(name.substring(5, 10), null));
    assertTrue(ds.isStringMatch(name.substring(10, 15), null));

    assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(30), null));
  }

  @Test
  public void testIsStringMatchNameCaseIndependent() {
    String name = RandomStringUtils.randomAlphabetic(20);

    Dataset ds = new Dataset();
    ds.setName(name.toLowerCase());

    assertTrue(ds.isStringMatch(name.toUpperCase(), null));
    assertTrue(ds.isStringMatch(name.toUpperCase().substring(7, 14), null));
  }

  @Test
  public void testIsStringMatchDatasetProperty() {
    Dataset ds = new Dataset();

    String value = RandomStringUtils.randomAlphanumeric(20);

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isStringMatch(value, null));
    assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(25), null));
  }

  @Test
  public void testIsStringMatchIdentifier() {
    Dataset ds = new Dataset();
    ds.setAlias(1235);

    assertTrue(ds.isStringMatch("DUOS-001235", null));
    assertTrue(ds.isStringMatch("DUOS", null));
    assertTrue(ds.isStringMatch("123", null));
    assertTrue(ds.isStringMatch("001235", null));
    assertFalse(ds.isStringMatch("DUOS-123456", null));
  }

  @Test
  public void testIsStringMatchDataUseCommercial() {
    Dataset ds = new Dataset();

    assertFalse(ds.isStringMatch("collaborator", null));

    DataUse du = new DataUseBuilder().setCollaboratorRequired(true).build();

    ds.setDataUse(du);

    assertTrue(ds.isStringMatch("collaborator", null));
    assertTrue(ds.isStringMatch("collab", null));
  }

  @Test
  public void testIsStringMatchDataUseIrb() {
    Dataset ds = new Dataset();

    assertFalse(ds.isStringMatch("irb", null));

    DataUse du = new DataUse();
    du.setEthicsApprovalRequired(true);

    ds.setDataUse(du);

    assertTrue(ds.isStringMatch("irb", null));
    assertTrue(ds.isStringMatch("irb", null));
  }

  @Test
  public void testIsStringMatchDataUseDiseases() {
    Dataset ds = new Dataset();

    assertFalse(ds.isStringMatch("cancer", null));
    assertFalse(ds.isStringMatch("alzheimers", null));

    DataUse du = new DataUse();
    du.setDiseaseRestrictions(List.of("cancer", "alzheimers"));

    ds.setDataUse(du);

    assertTrue(ds.isStringMatch("cancer", null));
    assertTrue(ds.isStringMatch("alzheimers", null));
  }

  @Test
  public void testIsStringMatchMultipleTerms() {
    Dataset ds = new Dataset();

    ds.setName("asdf");
    ds.setAlias(1234);

    assertTrue(ds.isStringMatch("ASD DUOS-001234", null));
    assertTrue(ds.isStringMatch("asdf 123", null));

    assertFalse(ds.isStringMatch("asf DUOS-001234", null));
    assertFalse(ds.isStringMatch("asd 122", null));

  }

  @Test
  public void testIsStringMatchOpenAccessTrue() {
    Dataset ds = new Dataset();

    String value = "true";

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyName("Open Access");
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    dsp.setSchemaProperty("consentGroup.openAccess");
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isStringMatch(value, true));
    assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(25), true));
  }

  @Test
  public void testIsStringMatchOpenAccessFalse() {
    Dataset ds = new Dataset();

    String value = "false";

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyName("Open Access");
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    dsp.setSchemaProperty("consentGroup.openAccess");
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isStringMatch(value, false));
    assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(25), true));
  }
}
