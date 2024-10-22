package org.broadinstitute.consent.http.models;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.validator.routines.UrlValidator;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.FileTypeObject;
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

  public boolean validateProperties() {
    // The following properties are patch-able:
    Map<String, Function<String, Boolean>> validators = Map.of(
        "# of participants", this::isNumeric,
        "url", this::isUrl,
        "file types", this::isFileTypes,
        "data location", this::isDataLocation
    );
    List<Boolean> validPropValues = properties.stream().map(p -> {
      if (!validators.containsKey(p.getPropertyName().toLowerCase())) {
        return false;
      }
      return validators
          .get(p.getPropertyName().toLowerCase())
          .apply(p.getPropertyValueAsString());
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
      return UrlValidator.getInstance().isValid(str);
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
