package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject.FileType;
import org.junit.jupiter.api.Test;

public class DatasetUpdateTest {

  @Test
  void testDatasetUpdate() {
    String json = """
        {
          "name": "Test Dataset Update",
          "dacId": 1,
          "properties": [
            {
              "propertyName": "string value",
              "propertyValue": "TDR Location"
            },
            {
              "propertyName": "long value",
              "propertyValue": 12
            },
            {
              "propertyName": "boolean value",
              "propertyValue": true
            },
            {
              "propertyName": "double value",
              "propertyValue": 10.001
            },
            {
              "propertyName": "File Types",
              "propertyValue": [{"fileType":"ARRAYS","functionalEquivalence":"testing"}]
            }
          ]
        }""";
    DatasetUpdate update = new DatasetUpdate(json);

    List<DatasetProperty> props = update.getDatasetProperties();
    assertEquals("TDR Location", getPropByName("string value", props).getPropertyValue());
    assertEquals(12L, getPropByName("long value", props).getPropertyValue());
    assertEquals(true, getPropByName("boolean value", props).getPropertyValue());
    assertEquals(10.001D, getPropByName("double value", props).getPropertyValue());

    // Parsing the object value of this prop is a little complicated due to JSON serialization
    DatasetProperty fileTypeProp = getPropByName("File Types", props);
    java.lang.reflect.Type listOfFileTypes = new TypeToken<ArrayList<FileTypeObject>>() {}.getType();
    Gson gson = new Gson();
    List<FileTypeObject> fileTypes = gson.fromJson(fileTypeProp.getPropertyValueAsString(), listOfFileTypes);
    assertFalse(fileTypes.isEmpty());
    FileTypeObject type = fileTypes.get(0);
    assertEquals(FileType.ARRAYS, type.getFileType());
  }

  private DatasetProperty getPropByName(String name, List<DatasetProperty> props ) {
    return props.stream().filter(p -> p.getPropertyName().equals(name)).findFirst().orElse(null);
  }

  @Test
  void testDatasetUpdateNullValues() {
    String json = """
        {
          "not_a_name": "Test Dataset Update",
          "not_a_dac_id": 1,
          "no_properties": [
            {
              "propertyName": "string value",
              "propertyValue": "TDR Location"
            }
          ]
        }""";
    DatasetUpdate update = new DatasetUpdate(json);
    assertNull(update.getName());
    assertNull(update.getDacId());
    assertNull(update.getDatasetProperties());
  }

}
