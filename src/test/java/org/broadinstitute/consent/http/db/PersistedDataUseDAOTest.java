package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseRow;
import org.junit.jupiter.api.Test;

class PersistedDataUseDAOTest extends DAOTestHelper {

  private PersistedDataUseDAO dao() {
    return jdbi.onDemand(PersistedDataUseDAO.class);
  }

  private PersistedDataUseRow rowFor(Integer datasetId) {
    return dao().findAllPersistedDataUse().stream()
        .filter(row -> row.datasetId().equals(datasetId))
        .findFirst()
        .orElseThrow();
  }

  private Integer insertDatasetWithDataUse(String rawDataUse) {
    User user = createUser();
    return datasetDAO.insertDataset(
        "Name_" + randomAlphanumeric(20),
        new Timestamp(new Date().getTime()),
        user.getUserId(),
        "Object ID_" + randomAlphanumeric(20),
        rawDataUse,
        null);
  }

  private void setAccessManagement(Integer datasetId, String schemaProperty, String value) {
    DatasetProperty property = new DatasetProperty();
    property.setDatasetId(datasetId);
    property.setPropertyKey(1);
    property.setSchemaProperty(schemaProperty);
    property.setPropertyValue(value);
    property.setPropertyType(PropertyType.String);
    property.setCreateDate(new Date());
    datasetDAO.insertDatasetProperties(List.of(property));
  }

  @Test
  void returnsRawDataUseSoAbsentAndMalformedValuesStayDistinct() {
    Integer nullValue = insertDatasetWithDataUse(null);
    Integer emptyValue = insertDatasetWithDataUse("");
    Integer malformedValue = insertDatasetWithDataUse("{not json");

    assertNull(rowFor(nullValue).dataUse());
    assertEquals("", rowFor(emptyValue).dataUse());
    assertEquals("{not json", rowFor(malformedValue).dataUse());
  }

  @Test
  void readsCanonicalAccessManagementProperty() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");
    setAccessManagement(datasetId, "accessManagement", "controlled");

    PersistedDataUseRow row = rowFor(datasetId);

    assertEquals("controlled", row.accessManagement());
    assertTrue(row.isCanonical());
  }

  @Test
  void fallsBackToTheLegacyPrefixedAccessManagementProperty() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");
    setAccessManagement(datasetId, "consentGroup.accessManagement", "external");

    assertEquals("external", rowFor(datasetId).accessManagement());
  }

  /** Mirrors Dataset#getAccessManagement, which prefers the canonical property. */
  @Test
  void canonicalPropertyWinsOverTheLegacyOne() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");
    setAccessManagement(datasetId, "consentGroup.accessManagement", "external");
    setAccessManagement(datasetId, "accessManagement", "controlled");

    assertEquals("controlled", rowFor(datasetId).accessManagement());
  }

  @Test
  void normalizesStoredCasingAndWhitespace() {
    Integer datasetId = insertDatasetWithDataUse("{}");
    setAccessManagement(datasetId, "accessManagement", "  OPEN  ");

    PersistedDataUseRow row = rowFor(datasetId);

    assertEquals("open", row.accessManagement());
    assertTrue(row.isOpenAccess());
    // No primary is the canonical shape under open access
    assertTrue(row.isCanonical());
  }

  @Test
  void reportsMissingAccessManagementRatherThanDroppingTheDataset() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");

    PersistedDataUseRow row = rowFor(datasetId);

    assertNull(row.accessManagement());
    assertEquals("missing", row.accessManagementLabel());
  }

  @Test
  void countsZeroDarsForAnUnreferencedDataset() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");

    assertEquals(0, rowFor(datasetId).darCount());
  }

  @Test
  void countsDistinctDarsReferencingTheDataset() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");
    String firstReference = createDataAccessRequestV3().getReferenceId();
    String secondReference = createDataAccessRequestV3().getReferenceId();
    dataAccessRequestDAO.insertDARDatasetRelation(firstReference, datasetId);
    dataAccessRequestDAO.insertDARDatasetRelation(secondReference, datasetId);

    assertEquals(2, rowFor(datasetId).darCount());
  }

  /** The same relation twice must not inflate the count. */
  @Test
  void countsARepeatedRelationOnce() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");
    String reference = createDataAccessRequestV3().getReferenceId();
    dataAccessRequestDAO.insertDARDatasetRelation(reference, datasetId);
    dataAccessRequestDAO.insertDARDatasetRelation(reference, datasetId);

    assertEquals(1, rowFor(datasetId).darCount());
  }

  @Test
  void findsDarReferenceIdsForTheDataset() {
    Integer datasetId = insertDatasetWithDataUse("{\"hmbResearch\":true,\"other\":\"text\"}");
    String reference = createDataAccessRequestV3().getReferenceId();
    dataAccessRequestDAO.insertDARDatasetRelation(reference, datasetId);

    assertEquals(List.of(reference), dao().findDarReferenceIdsByDatasetId(datasetId));
  }

  @Test
  void findsNoDarReferenceIdsForAnUnreferencedDataset() {
    Integer datasetId = insertDatasetWithDataUse("{\"generalUse\":true}");

    assertTrue(dao().findDarReferenceIdsByDatasetId(datasetId).isEmpty());
  }
}
