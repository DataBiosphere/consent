package org.broadinstitute.consent.http.models;

import java.util.List;
import java.util.Optional;

/**
 * This model represents the values of a dataset that are modifiable via the PATCH operation
 *
 * @param name       Dataset name
 * @param properties List of DatasetProperties
 */
public record DatasetPatch(String name, List<DatasetProperty> properties) {

  /**
   * Determine if this patch object contains different values from the provided Dataset entity
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
      return (existingProp.isPresent() && !existingProp.get().getPropertyValueAsString().equals(p.getPropertyValueAsString()));
    });
  }

}
