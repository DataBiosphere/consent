package org.broadinstitute.consent.http.models;


import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetTests {

  @Test
  void testParseIdentifierToAlias() {
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
  void testIsDatasetMatchName() {
    String name = RandomStringUtils.randomAlphanumeric(20);

    Dataset ds = new Dataset();
    ds.setName(name);

    assertTrue(ds.isDatasetMatch(name, AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch(name.substring(5, 10), AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch(name.substring(10, 15), AccessManagement.CONTROLLED));

    assertFalse(
        ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(30), AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchNameCaseIndependent() {
    String name = RandomStringUtils.randomAlphabetic(20);

    Dataset ds = new Dataset();
    ds.setName(name.toLowerCase());

    assertTrue(ds.isDatasetMatch(name.toUpperCase(), AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch(name.toUpperCase().substring(7, 14), AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchDatasetProperty() {
    Dataset ds = new Dataset();

    String value = RandomStringUtils.randomAlphanumeric(20);

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isDatasetMatch(value, AccessManagement.CONTROLLED));
    assertFalse(
        ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(25), AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchIdentifier() {
    Dataset ds = new Dataset();
    ds.setAlias(1235);

    assertTrue(ds.isDatasetMatch("DUOS-001235", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("DUOS", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("123", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("001235", AccessManagement.CONTROLLED));
    assertFalse(ds.isDatasetMatch("DUOS-123456", AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchDataUseCommercial() {
    Dataset ds = new Dataset();

    assertFalse(ds.isDatasetMatch("collaborator", AccessManagement.CONTROLLED));

    DataUse du = new DataUseBuilder().setCollaboratorRequired(true).build();

    ds.setDataUse(du);

    assertTrue(ds.isDatasetMatch("collaborator", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("collab", AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchDataUseIrb() {
    Dataset ds = new Dataset();

    assertFalse(ds.isDatasetMatch("irb", AccessManagement.CONTROLLED));

    DataUse du = new DataUse();
    du.setEthicsApprovalRequired(true);

    ds.setDataUse(du);

    assertTrue(ds.isDatasetMatch("irb", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("irb", AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchDataUseDiseases() {
    Dataset ds = new Dataset();

    assertFalse(ds.isDatasetMatch("cancer", AccessManagement.CONTROLLED));
    assertFalse(ds.isDatasetMatch("alzheimers", AccessManagement.CONTROLLED));

    DataUse du = new DataUse();
    du.setDiseaseRestrictions(List.of("cancer", "alzheimers"));

    ds.setDataUse(du);

    assertTrue(ds.isDatasetMatch("cancer", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("alzheimers", AccessManagement.CONTROLLED));
  }

  @Test
  void testIsDatasetMatchMultipleTerms() {
    Dataset ds = new Dataset();

    ds.setName("asdf");
    ds.setAlias(1234);

    assertTrue(ds.isDatasetMatch("ASD DUOS-001234", AccessManagement.CONTROLLED));
    assertTrue(ds.isDatasetMatch("asdf 123", AccessManagement.CONTROLLED));

    assertFalse(ds.isDatasetMatch("asf DUOS-001234", AccessManagement.CONTROLLED));
    assertFalse(ds.isDatasetMatch("asd 122", AccessManagement.CONTROLLED));

  }

  @Test
  void testIsDatasetMatchOpenAccess() {
    Dataset ds = new Dataset();

    String value = "open";

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyName("Access Management");
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    dsp.setSchemaProperty("consentGroup.accessManagement");
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isDatasetMatch(value, AccessManagement.OPEN));
    assertFalse(ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(25), AccessManagement.OPEN));
  }

  @Test
  void testIsDatasetMatchControlledAccess() {
    Dataset ds = new Dataset();

    String value = "controlled";

    DatasetProperty dsp = new DatasetProperty();
    dsp.setPropertyName("Access Management");
    dsp.setPropertyValue(value);
    dsp.setPropertyType(PropertyType.String);
    dsp.setSchemaProperty("consentGroup.accessManagement");
    ds.setProperties(Set.of(dsp));

    assertTrue(ds.isDatasetMatch(value, AccessManagement.CONTROLLED));
    assertFalse(ds.isDatasetMatch(RandomStringUtils.randomAlphanumeric(25), AccessManagement.OPEN));
  }

}
