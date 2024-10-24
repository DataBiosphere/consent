package org.broadinstitute.consent.http.models;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.validator.routines.UrlValidator;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;

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
              return eProp.getPropertyName().equalsIgnoreCase(p.getPropertyName());
            } else if (eProp.getPropertyKey() != null) {
              return eProp.getPropertyKey().equals(p.getPropertyKey());
            } else if (eProp.getSchemaProperty() != null) {
              return eProp.getSchemaProperty().equalsIgnoreCase(p.getSchemaProperty());
            }
            return false;
          })
          .findFirst();
      return existingProp.map(
              eProp ->
                  // If the existing prop value is different from the new one, this is patchable
                  (!eProp.getPropertyValueAsString().equals(p.getPropertyValueAsString())))
          // If no existing prop, we're adding a new prop, and therefore patchable.
          .orElse(true);
    });
  }

  public boolean validateProperties() {
    // The following properties are patch-able:
    Map<String, Function<Object, Boolean>> validators = Map.of(
        "# of participants", this::isNumeric,
        "url", this::isUrl,
        "data location", this::isDataLocation
    );
    return properties.stream()
        .map(p -> {
          if (!validators.containsKey(p.getPropertyName().toLowerCase())) {
            return false;
          }
          return validators
              .get(p.getPropertyName().toLowerCase())
              .apply(p.getPropertyValue());
        })
        .distinct()
        .allMatch(valid -> valid);
  }

  private boolean isNumeric(Object obj) {
    try {
      return Integer.parseInt(obj.toString()) > 0;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isUrl(Object obj) {
    return UrlValidator.getInstance().isValid(obj.toString());
  }

  private boolean isDataLocation(Object obj) {
    try {
      // Try parsing as either the value or the enum
      try {
        DataLocation.fromValue(obj.toString());
      } catch (IllegalArgumentException e) {
        DataLocation.valueOf(obj.toString());
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
