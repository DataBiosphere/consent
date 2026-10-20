package org.broadinstitute.consent.http.enumeration;

import java.util.List;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;

/**
 * The approved public fields of a Data Showcase dataset summary. Each entry names its JSON key, its
 * source, its type and units, and whether it can be null. Adding an entry is a change that data
 * stewards and AppSec review; {@link org.broadinstitute.consent.http.models.ShowcaseDatasetSummary}
 * mirrors this list exactly.
 */
public enum ShowcasePublicField {
  IDENTIFIER("identifier", Source.COLUMN, List.of("dataset.alias"), "string", null, false),
  TITLE("title", Source.COLUMN, List.of("dataset.name"), "string", null, false),
  ACCESS_MANAGEMENT(
      "accessManagement",
      Source.DERIVED,
      List.of(
          Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
          Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY),
      "string",
      null,
      true),
  DAC_NAME("dacName", Source.COLUMN, List.of("dac.name"), "string", null, true),
  CONSENT_CODES(
      "consentCodes", Source.DERIVED, List.of("dataset.data_use"), "string[]", null, false),
  PARTICIPANT_COUNT(
      "participantCount",
      Source.DATASET_PROPERTY,
      List.of(DatasetRegistrationSchemaV1Builder.numberOfParticipants),
      "integer",
      "participants",
      true),
  AVAILABLE_DATE(
      "availableDate",
      Source.DERIVED,
      List.of("dataset.dac_approval_date", "dataset.create_date"),
      "date",
      null,
      true),
  ACCESS_URL(
      "accessUrl",
      Source.DATASET_PROPERTY,
      List.of(DatasetRegistrationSchemaV1Builder.url),
      "string",
      null,
      true),
  SPECIES(
      "species",
      Source.STUDY_PROPERTY,
      List.of(DatasetRegistrationSchemaV1Builder.species),
      "string",
      null,
      true);

  /** Where a field's value is read from. */
  public enum Source {
    /** A named table column. */
    COLUMN,
    /** A {@code dataset_property} row, matched on {@code schema_property}. */
    DATASET_PROPERTY,
    /** A {@code study_property} row, matched on {@code key}. */
    STUDY_PROPERTY,
    /** Computed from the listed columns or properties; the raw inputs are never returned. */
    DERIVED
  }

  private final String jsonName;
  private final Source source;
  private final List<String> sourceNames;
  private final String type;
  private final String units;
  private final boolean nullable;

  ShowcasePublicField(
      String jsonName,
      Source source,
      List<String> sourceNames,
      String type,
      String units,
      boolean nullable) {
    this.jsonName = jsonName;
    this.source = source;
    this.sourceNames = sourceNames;
    this.type = type;
    this.units = units;
    this.nullable = nullable;
  }

  public String getJsonName() {
    return jsonName;
  }

  public Source getSource() {
    return source;
  }

  public List<String> getSourceNames() {
    return sourceNames;
  }

  public String getType() {
    return type;
  }

  public String getUnits() {
    return units;
  }

  public boolean isNullable() {
    return nullable;
  }

  /** The {@code dataset_property.schema_property} values the projection may read. */
  public static List<String> datasetPropertyAllowlist() {
    return sourceNamesOf(Source.DATASET_PROPERTY);
  }

  /** The {@code study_property.key} values the projection may read. */
  public static List<String> studyPropertyAllowlist() {
    return sourceNamesOf(Source.STUDY_PROPERTY);
  }

  public static List<String> jsonNames() {
    return List.of(values()).stream().map(ShowcasePublicField::getJsonName).toList();
  }

  private static List<String> sourceNamesOf(Source source) {
    return List.of(values()).stream()
        .filter(field -> field.source == source)
        .flatMap(field -> field.sourceNames.stream())
        .toList();
  }
}
