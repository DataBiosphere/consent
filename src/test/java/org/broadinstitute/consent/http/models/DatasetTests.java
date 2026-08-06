package org.broadinstitute.consent.http.models;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.gson.JsonArray;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.util.TestAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class DatasetTests {

  @Test
  void testIsCreatorOrCustodian_datasetCreator() {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 100));
    Dataset dataset = new Dataset();
    dataset.setDatasetId(RandomUtils.nextInt(1, 100));
    dataset.setCreateUserId(user.getUserId());

    assertTrue(dataset.isCreator(user));
  }

  @Test
  void testIsCreatorOrCustodian_studyCreator() {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 100));
    Dataset dataset = new Dataset();
    dataset.setDatasetId(RandomUtils.nextInt(1, 100));
    Study study = new Study();
    study.setCreateUserId(user.getUserId());
    dataset.setStudy(study);

    assertTrue(dataset.isCreator(user));
  }

  @Test
  void testIsCreatorOrCustodian_dataCustodian() {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 100));
    user.setEmail("test@test.com");
    Dataset dataset = new Dataset();
    dataset.setDatasetId(RandomUtils.nextInt(1, 100));
    Study study = new Study();
    dataset.setStudy(study);
    StudyProperty p = new StudyProperty();
    p.setKey(DatasetRegistrationSchemaV1Builder.dataCustodianEmail);
    p.setType(PropertyType.Json);
    JsonArray a = new JsonArray();
    a.add(user.getEmail());
    p.setValue(a);
    study.addProperties(p);

    assertTrue(dataset.isCustodian(user));
  }

  @Test
  void testIsCreatorOrCustodian_notCustodian() {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 100));
    user.setEmail("test@test.com");
    Dataset dataset = new Dataset();
    dataset.setDatasetId(RandomUtils.nextInt(1, 100));
    Study study = new Study();
    dataset.setStudy(study);
    StudyProperty p = new StudyProperty();
    p.setKey(DatasetRegistrationSchemaV1Builder.dataCustodianEmail);
    p.setType(PropertyType.Json);
    JsonArray a = new JsonArray();
    a.add("different_user@test.com");
    p.setValue(a);
    study.addProperties(p);

    assertFalse(dataset.isCustodian(user));
  }

  @Test
  void testParseIdentifierToAlias() {
    assertEquals(3, (int) Dataset.parseIdentifierToAlias("DUOS-3"));
    assertEquals(3, (int) Dataset.parseIdentifierToAlias("DUOS-000003"));
    assertEquals(123456, (int) Dataset.parseIdentifierToAlias("DUOS-123456"));

    assertThrows(
        IllegalArgumentException.class, () -> Dataset.parseIdentifierToAlias("asdf-123456"));
    assertThrows(
        IllegalArgumentException.class, () -> Dataset.parseIdentifierToAlias("DUOS-1234 56"));
    assertThrows(
        IllegalArgumentException.class, () -> Dataset.parseIdentifierToAlias("DUOS-1234as56"));
  }

  @Test
  void testParseAliasToIdentifierPadsToMinimumWidthWithoutTruncating() {
    assertEquals("DUOS-000042", Dataset.parseAliasToIdentifier(42));
    assertEquals("DUOS-123456", Dataset.parseAliasToIdentifier(123456));
    assertEquals("DUOS-1234567", Dataset.parseAliasToIdentifier(1234567));
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

  @Test
  void testGetAccessManagementLogsUnparseableValue() {
    String invalidValue = "unknown";
    Dataset dataset = new Dataset();
    dataset.setDatasetId(42);
    DatasetProperty property = new DatasetProperty();
    property.setSchemaProperty(Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY);
    property.setPropertyValue(invalidValue);
    dataset.setProperties(Set.of(property));

    Logger logger = (Logger) LoggerFactory.getLogger(Dataset.class);
    TestAppender appender = new TestAppender();
    appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    logger.addAppender(appender);
    appender.start();
    try {
      assertNull(dataset.getAccessManagement());
      List<String> messages =
          appender.getLoggedEvents().stream().map(ILoggingEvent::getFormattedMessage).toList();
      assertTrue(messages.stream().anyMatch(message -> message.contains(invalidValue)));
      assertTrue(messages.stream().anyMatch(message -> message.contains("dataset id: 42")));
    } finally {
      appender.stop();
      logger.detachAppender(appender);
    }
  }
}
