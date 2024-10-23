package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
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
    invalidProp.setPropertyName(RandomStringUtils.randomAlphabetic(25));
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
    participantsProp.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
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

  @ParameterizedTest
  @EnumSource(FileType.class)
  void testValidateFileType(FileType fileType) {
    Gson gson = GsonUtil.buildGson();
    DatasetProperty fileTypeProp = new DatasetProperty();
    fileTypeProp.setPropertyName("file types");
    fileTypeProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.fileTypes);
    fileTypeProp.setPropertyType(PropertyType.Json);
    FileTypeObject fileTypeObj = new FileTypeObject();
    fileTypeObj.setFileType(fileType);
    fileTypeObj.setFunctionalEquivalence(
        RandomStringUtils.randomAlphabetic(5) + " " + RandomStringUtils.randomAlphabetic(5));
    fileTypeProp.setPropertyValue(gson.toJson(List.of(fileTypeObj)));
    DatasetPatch patch = new DatasetPatch(null, List.of(fileTypeProp));
    assertTrue(patch.validateProperties());
  }

  // We need this version of the FileType test due to Gson serialization. When a correctly formed
  // List of FileTypeObject is passed in as a property in a DatasetPatch at the resource level,
  // Gson cannot infer the nested types based on the property name. Therefore, it comes through as
  // an ArrayList of LinkedTreeMap objects. The validateProperties method handles both cases.
  @ParameterizedTest
  @EnumSource(FileType.class)
  void testValidateFileTypeAsLinkedTreeMaps(FileType fileType) {
    DatasetProperty fileTypeProp = new DatasetProperty();
    fileTypeProp.setPropertyName("file types");
    fileTypeProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.fileTypes);
    fileTypeProp.setPropertyType(PropertyType.Json);
    LinkedTreeMap<String, String> treeMap = new LinkedTreeMap<>();
    treeMap.put("fileType", fileType.value());
    treeMap.put("functionalEquivalence", RandomStringUtils.randomAlphabetic(5));
    fileTypeProp.setPropertyValue(List.of(treeMap));
    DatasetPatch patch = new DatasetPatch(null, List.of(fileTypeProp));
    assertTrue(patch.validateProperties());
  }

  @Test
  void testValidateFileTypeFailure() {
    DatasetProperty fileTypeProp = new DatasetProperty();
    fileTypeProp.setPropertyName("file types");
    fileTypeProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.fileTypes);
    fileTypeProp.setPropertyType(PropertyType.Json);
    fileTypeProp.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    DatasetPatch patch = new DatasetPatch(null, List.of(fileTypeProp));
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
    dataLocationProp.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    DatasetPatch patch = new DatasetPatch(null, List.of(dataLocationProp));
    assertFalse(patch.validateProperties());
  }

}
