package org.broadinstitute.consent.http.models.dataset_registration_v1.builder;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.requestLocation;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.url;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.junit.jupiter.api.Test;

class ConsentGroupFromDatasetTest extends AbstractTestHelper {

  @Test
  void testBuildReturnsNullWhenDatasetIsNull() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    assertNull(builder.build(null));
  }

  @Test
  void testBuildReturnsConsentGroupForEmptyDataset() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = new Dataset();
    ConsentGroup result = builder.build(dataset);
    assertNotNull(result);
  }

  @Test
  void testBuildUsesLegacyAccessManagementProperty() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(
        createDatasetProperty(
            dataset,
            Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
            PropertyType.String,
            "OPEN"));

    ConsentGroup result = builder.build(dataset);

    assertNotNull(result);
    assertEquals(AccessManagement.OPEN, result.getAccessManagement());
  }

  @Test
  void testBuildWithValidUrl() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(
        createDatasetProperty(dataset, url, PropertyType.String, "https://example.com/data"));

    ConsentGroup result = builder.build(dataset);

    assertNotNull(result);
    assertNotNull(result.getUrl());
  }

  @Test
  void testBuildWithMalformedUrlDoesNotThrow() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    // Spaces in URIs are invalid and cause URI.create() to throw
    dataset.addProperty(
        createDatasetProperty(dataset, url, PropertyType.String, "not a valid uri"));

    assertDoesNotThrow(() -> builder.build(dataset));
  }

  @Test
  void testBuildWithMalformedUrlSetsNullUrl() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(
        createDatasetProperty(dataset, url, PropertyType.String, "not a valid uri"));

    ConsentGroup result = builder.build(dataset);

    assertNotNull(result);
    assertNull(result.getUrl());
  }

  @Test
  void testBuildWithValidRequestLocation() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(
        createDatasetProperty(
            dataset, requestLocation, PropertyType.String, "https://example.com/request"));

    ConsentGroup result = builder.build(dataset);

    assertNotNull(result);
    assertNotNull(result.getRequestLocation());
  }

  @Test
  void testBuildWithMalformedRequestLocationDoesNotThrow() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    // Spaces in URIs are invalid and cause URI.create() to throw
    dataset.addProperty(
        createDatasetProperty(dataset, requestLocation, PropertyType.String, "not a valid uri"));

    assertDoesNotThrow(() -> builder.build(dataset));
  }

  @Test
  void testBuildWithMalformedRequestLocationSetsNullRequestLocation() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(
        createDatasetProperty(dataset, requestLocation, PropertyType.String, "not a valid uri"));

    ConsentGroup result = builder.build(dataset);

    assertNotNull(result);
    assertNull(result.getRequestLocation());
  }

  @Test
  void testBuildWithBothUrlAndRequestLocation() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(
        createDatasetProperty(dataset, url, PropertyType.String, "https://example.com/data"));
    dataset.addProperty(
        createDatasetProperty(
            dataset, requestLocation, PropertyType.String, "https://example.com/request"));

    ConsentGroup result = builder.build(dataset);

    assertNotNull(result);
    assertNotNull(result.getUrl());
    assertNotNull(result.getRequestLocation());
  }

  @Test
  void testBuildWithBothMalformedUrlAndRequestLocationDoesNotThrow() {
    ConsentGroupFromDataset builder = new ConsentGroupFromDataset();
    Dataset dataset = createMockDataset();
    dataset.addProperty(createDatasetProperty(dataset, url, PropertyType.String, "bad url here"));
    dataset.addProperty(
        createDatasetProperty(dataset, requestLocation, PropertyType.String, "bad url here"));

    assertDoesNotThrow(() -> builder.build(dataset));
  }

  private Dataset createMockDataset() {
    User user = new User();
    user.setUserId(randomInt(1, 1000));
    user.setDisplayName(randomAlphabetic(10));
    user.setEmail(randomAlphabetic(5) + "@test.com");
    Date now = new Date();
    Dataset dataset = new Dataset();
    dataset.setName(randomAlphabetic(10));
    dataset.setDatasetId(randomInt(1, 1000));
    dataset.setAlias(randomInt(1, 1000));
    dataset.setDatasetIdentifier();
    dataset.setCreateUser(user);
    dataset.setCreateUserId(user.getUserId());
    dataset.setCreateDate(now);
    return dataset;
  }

  private DatasetProperty createDatasetProperty(
      Dataset dataset, String schemaProp, PropertyType type, Object propValue) {
    DatasetProperty prop = new DatasetProperty();
    prop.setDatasetId(dataset.getDatasetId());
    prop.setSchemaProperty(schemaProp);
    prop.setPropertyName(schemaProp);
    prop.setPropertyType(type);
    prop.setPropertyValue(propValue);
    return prop;
  }
}
