package org.broadinstitute.consent.http.util;

import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Converts a dataset registration object into a
 * list of dataset properties. The dataset properties
 * are specific to an individual consent group in the
 * registration object and are the final representation
 * of the dataset registration information in the
 * database.
 */
public class DatasetRegistrationPropertyConverter {

    /**
     * Extracts an individual field as a dataset property.
     *
     * @param name The human-readable name of the field
     * @param schemaProp The schema property name (camelCase)
     * @param type The type of the field, e.g. Boolean, String
     * @param getField Lambda which gets the field's value
     */
    private record DatasetPropertyExtractor(
            String name,
            String schemaProp,
            DatasetPropertyType type,

            /*
             * Takes in: Dataset registration object and consent group index (as integer)
             * Produces: The value of the field, can be null if field not present.
             */
            BiFunction<DatasetRegistrationSchemaV1,Integer,Object> getField
    ) {
        Optional<DatasetProperty> extract(DatasetRegistrationSchemaV1 registration, int consentGroupIdx) {
            Object value = this.getField.apply(registration, consentGroupIdx);
            if (Objects.isNull(value)) {
                return Optional.empty();
            }

            DatasetProperty datasetProperty = new DatasetProperty();
            datasetProperty.setPropertyName(this.name);
            datasetProperty.setPropertyType(this.type);
            datasetProperty.setSchemaProperty(this.schemaProp);
            datasetProperty.setPropertyValue(this.type.coerce(value.toString()));

            return Optional.of(datasetProperty);

        }
    };

    private static final List<DatasetPropertyExtractor> datasetPropertyExtractors = List.of(
            new DatasetPropertyExtractor(
                    "PI Name", "piName", DatasetPropertyType.String,
                    (registration, idx) -> registration.getPiName())
        );


    public static List<DatasetProperty> convert(DatasetRegistrationSchemaV1 registration, int consentGroupIdx) {
        List<DatasetProperty> datasetProperties = new ArrayList<>();
        for (DatasetPropertyExtractor datasetPropertyExtractor : datasetPropertyExtractors) {
            Optional<DatasetProperty> extractedProperty = datasetPropertyExtractor.extract(registration, consentGroupIdx);

            if (extractedProperty.isPresent()) {
                datasetProperties.add(extractedProperty.get());
            }
        }

        return datasetProperties;
    }



}
