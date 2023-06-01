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
  public void testIsStringMatchName() {
    String name = RandomStringUtils.randomAlphanumeric(20);

    Dataset ds = new Dataset();
    ds.setName(name);

    assertTrue(ds.isStringMatch(name));
    assertTrue(ds.isStringMatch(name.substring(5, 10)));
    assertTrue(ds.isStringMatch(name.substring(10, 15)));

    assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(30)));
  }

  @Test
  public void testIsStringMatchNameCaseIndependent() {
    String name = RandomStringUtils.randomAlphabetic(20);

    Dataset ds = new Dataset();
    ds.setName(name.toLowerCase());

    assertTrue(ds.isStringMatch(name.toUpperCase()));
    assertTrue(ds.isStringMatch(name.toUpperCase().substring(7, 14)));
  }

  @Test
  public void testIsStringMatchDatasetProperty() {
    Dataset ds = new Dataset();

    String value = RandomStringUtils.randomAlphanumeric(20);

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isStringMatch(value));
    assertFalse(ds.isStringMatch(RandomStringUtils.randomAlphanumeric(25)));
  }

  @Test
  public void testIsStringMatchIdentifier() {
    Dataset ds = new Dataset();
    ds.setAlias(1235);

    assertTrue(ds.isStringMatch("DUOS-001235"));
    assertTrue(ds.isStringMatch("DUOS"));
    assertTrue(ds.isStringMatch("123"));
    assertTrue(ds.isStringMatch("001235"));
    assertFalse(ds.isStringMatch("DUOS-123456"));
  }

  @Test
  public void testIsStringMatchDataUseCommercial() {
    Dataset ds = new Dataset();

    assertFalse(ds.isStringMatch("collaborator"));

    DataUse du = new DataUseBuilder().setCollaboratorRequired(true).build();

    ds.setDataUse(du);

    assertTrue(ds.isStringMatch("collaborator"));
    assertTrue(ds.isStringMatch("collab"));
  }

  @Test
  public void testIsStringMatchDataUseIrb() {
    Dataset ds = new Dataset();

    assertFalse(ds.isStringMatch("irb"));

    DataUse du = new DataUse();
    du.setEthicsApprovalRequired(true);

    ds.setDataUse(du);

    assertTrue(ds.isStringMatch("irb"));
    assertTrue(ds.isStringMatch("irb"));
  }

  @Test
  public void testIsStringMatchDataUseDiseases() {
    Dataset ds = new Dataset();

    assertFalse(ds.isStringMatch("cancer"));
    assertFalse(ds.isStringMatch("alzheimers"));

    DataUse du = new DataUse();
    du.setDiseaseRestrictions(List.of("cancer", "alzheimers"));

    ds.setDataUse(du);

    assertTrue(ds.isStringMatch("cancer"));
    assertTrue(ds.isStringMatch("alzheimers"));
  }

  @Test
  public void testIsStringMatchMultipleTerms() {
    Dataset ds = new Dataset();

    ds.setName("asdf");
    ds.setAlias(1234);

    assertTrue(ds.isStringMatch("ASD DUOS-001234"));
    assertTrue(ds.isStringMatch("asdf 123"));

    assertFalse(ds.isStringMatch("asf DUOS-001234"));
    assertFalse(ds.isStringMatch("asd 122"));

  }
}
