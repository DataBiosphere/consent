package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    // Ensure each property type is correctly handled. See dataset-registration-schema_v1.json
    // for full list of properties datasets can have.
    String location = "TDR Location";
    Long participants = 12L;
    Boolean nmds = true;
    String json = """
        {
          "name": "Test Dataset Update",
          "dacId": 1,
          "properties": [
            {
              "propertyName": "Data Location",
              "propertyValue": "%s"
            },
            {
              "propertyName": "# of participants",
              "propertyValue": %d
            },
            {
              "propertyName": "nmds",
              "propertyValue": %s
            },
            {
              "propertyName": "File Types",
              "propertyValue": [{"fileType":"ARRAYS","functionalEquivalence":"testing"}]
            }
          ]
        }""".formatted(location, participants, nmds);
    DatasetUpdate update = new DatasetUpdate(json);
    List<DatasetProperty> props = update.getDatasetProperties();

    assertEquals(location, getPropByName("Data Location", props).getPropertyValue());

    assertEquals(participants, getPropByName("# of participants", props).getPropertyValue());

    assertEquals(nmds, getPropByName("nmds", props).getPropertyValue());

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

}
