package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject.FileType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class DatasetPatchTest {

  @Test
  void testInvalidPropertyKeys() {
    DatasetProperty invalidProp = new DatasetProperty();
    invalidProp.setPropertyName(RandomStringUtils.randomAlphanumeric(25));
    invalidProp.setPropertyValue(RandomUtils.nextInt(1, 10));
    invalidProp.setPropertyType(PropertyType.Number);
    DatasetPatch patch = new DatasetPatch(null, List.of(invalidProp));
    assertFalse(patch.validateProperties());
  }

  @Test
  void testValidateNumberOfParticipants() {
    DatasetProperty participantsProp = new DatasetProperty();
    participantsProp.setPropertyName("# of participants");
    participantsProp.setPropertyValue(RandomUtils.nextInt(1, 10));
    participantsProp.setPropertyType(PropertyType.Number);
    DatasetPatch patch = new DatasetPatch(null, List.of(participantsProp));
    assertTrue(patch.validateProperties());
  }

  @Test
  void testValidateNumberOfParticipantsFailure() {
    DatasetProperty participantsProp = new DatasetProperty();
    participantsProp.setPropertyName("# of participants");
    participantsProp.setPropertyValue(RandomStringUtils.randomAlphanumeric(10));
    participantsProp.setPropertyType(PropertyType.Number);
    DatasetPatch patch = new DatasetPatch(null, List.of(participantsProp));
    assertFalse(patch.validateProperties());
  }

  @Test
  void testValidateUrl() {
    DatasetProperty urlProp = new DatasetProperty();
    urlProp.setPropertyName("url");
    urlProp.setPropertyValue("https://duos.org");
    urlProp.setPropertyType(PropertyType.String);
    DatasetPatch patch = new DatasetPatch(null, List.of(urlProp));
    assertTrue(patch.validateProperties());
  }

  @Test
  void testValidateUrlFailure() {
    DatasetProperty urlProp = new DatasetProperty();
    urlProp.setPropertyName("url");
    urlProp.setPropertyValue("");
    urlProp.setPropertyType(PropertyType.String);
    DatasetPatch patch = new DatasetPatch(null, List.of(urlProp));
    assertFalse(patch.validateProperties());
  }

  private final String fileTypeInput = """
      {
        "name": "name",
        "properties": [
          {
            "propertyName": "File Types",
            "propertyValue": [
              {
                "fileType": "%s",
                "functionalEquivalence": "%s"
              }
            ]
          }
        ]
      }
      """;

  @ParameterizedTest
  @EnumSource(FileType.class)
  void testValidateFileType(FileType fileType) {
    String json = fileTypeInput.formatted(fileType.value(), RandomStringUtils.randomAlphanumeric(5));
    Gson gson = GsonUtil.buildGson();
    DatasetPatch patch = gson.fromJson(json, DatasetPatch.class);
    assertTrue(patch.validateProperties());
  }

  @Test
  void testValidateFileTypeFailure() {
    String json = fileTypeInput.formatted(RandomStringUtils.randomAlphanumeric(5),
        RandomStringUtils.randomAlphanumeric(5));
    Gson gson = GsonUtil.buildGson();
    DatasetPatch patch = gson.fromJson(json, DatasetPatch.class);
    assertFalse(patch.validateProperties());
  }

  @Test
  void testValidateFileTypeMissingFileTypeFailure() {
    String invalidFileType = """
        [{"invalidFileType": "invalid", "functionalEquivalence": "random"}]
        """;
    DatasetProperty fileTypeProp = new DatasetProperty();
    fileTypeProp.setPropertyName("file types");
    fileTypeProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.fileTypes);
    fileTypeProp.setPropertyType(PropertyType.Json);
    fileTypeProp.setPropertyValue(invalidFileType);
    DatasetPatch patch = new DatasetPatch(null, List.of(fileTypeProp));
    assertFalse(patch.validateProperties());
  }

  @ParameterizedTest
  @EnumSource(DataLocation.class)
  void testValidateDataLocation(DataLocation location) {
    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setPropertyName("data location");
    dataLocationProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    dataLocationProp.setPropertyType(PropertyType.Json);
    dataLocationProp.setPropertyValue(location.value());
    DatasetPatch patch = new DatasetPatch(null, List.of(dataLocationProp));
    assertTrue(patch.validateProperties());
  }

  @Test
  void testValidateDataLocationFailure() {
    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setPropertyName("data location");
    dataLocationProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    dataLocationProp.setPropertyType(PropertyType.Json);
    dataLocationProp.setPropertyValue(RandomStringUtils.randomAlphanumeric(10));
    DatasetPatch patch = new DatasetPatch(null, List.of(dataLocationProp));
    assertFalse(patch.validateProperties());
  }

}
