package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject.FileType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * This model represents the values of a dataset that are modifiable via the PATCH operation
 *
 * @param name       Dataset name
 * @param properties List of DatasetProperties
 */
public record DatasetPatch(String name, List<DatasetProperty> properties) {

  /**
   * Determine if this patch object contains different values from the provided Dataset entity
   *
   * @param dataset Dataset
   * @return True if any patch values are different, false otherwise.
   */
  public boolean isPatchable(Dataset dataset) {
    if (name() != null && !name().equals(dataset.getName())) {
      return true;
    }
    // Find any cases where the new property value is different from the existing one
    return properties().stream().anyMatch(p -> {
      Optional<DatasetProperty> existingProp = dataset.getProperties()
          .stream()
          .filter(eProp -> {
            // Favor the property name as the primary comparator
            if (eProp.getPropertyName() != null) {
              return eProp.getPropertyName().equals(p.getPropertyName());
            } else if (eProp.getPropertyKey() != null) {
              return eProp.getPropertyKey().equals(p.getPropertyKey());
            } else if (eProp.getSchemaProperty() != null) {
              return eProp.getSchemaProperty().equals(p.getSchemaProperty());
            }
            return false;
          })
          .findFirst();
      return (existingProp.isPresent() && !existingProp.get().getPropertyValueAsString()
          .equals(p.getPropertyValueAsString()));
    });
  }

  public static List<String> validPropertyNames = List.of(
      "# of participants",
      "url",
      "file types",
      "data location"
      );

  // The following properties are Patch-able:
  public boolean validateProperties() {
    List<Boolean> validPropValues = properties.stream().map(p -> {
      if (!validPropertyNames.contains(p.getPropertyName().toLowerCase())) {
        return false;
      }
      return switch (p.getPropertyName().toLowerCase()) {
        case "# of participants" -> isNumeric(p.getPropertyTypeAsString());
        case "url" -> isUrl(p.getPropertyTypeAsString());
        case "file types" -> isFileTypes(p.getPropertyValueAsString());
        case "data location" -> isDataLocation(p.getPropertyValueAsString());
        default -> false;
      };
    }).distinct().toList();
    return validPropValues.stream().allMatch(p -> p);
  }

  private boolean isNumeric(String str) {
    try {
      Integer.valueOf(str);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isUrl(String str) {
    try {
      URI.create(str);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isFileTypes(String str) {
    try {
      Gson gson = GsonUtil.buildGson();
      gson.fromJson(str, FileTypeObject.class);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isDataLocation(String str) {
    try {
      Gson gson = GsonUtil.buildGson();
      gson.fromJson(str, DataLocation.class);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
