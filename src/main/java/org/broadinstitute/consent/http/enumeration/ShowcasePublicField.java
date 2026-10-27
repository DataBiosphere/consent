package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.List;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;

/**
 * The approved public fields of a Data Showcase dataset summary. Each entry names its JSON key, its
 * source, the property rows the projection reads for it, its type, and whether it can be null.
 * Adding an entry is a change that data stewards and AppSec review; {@link
 * org.broadinstitute.consent.http.models.ShowcaseDatasetSummary} mirrors this list exactly.
 */
public enum ShowcasePublicField {
  IDENTIFIER(
      "identifier",
      Source.DERIVED,
      List.of("dataset.alias"),
      PropertyTable.NONE,
      JsonType.STRING,
      false),
  TITLE(
      "title", Source.COLUMN, List.of("dataset.name"), PropertyTable.NONE, JsonType.STRING, false),
  ACCESS_MANAGEMENT(
      "accessManagement",
      Source.DERIVED,
      List.of(
          Dataset.ACCESS_MANAGEMENT_SCHEMA_PROPERTY,
          Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY),
      // The projection reads these keys in its eligibility subquery, not through the allowlist.
      PropertyTable.NONE,
      JsonType.STRING,
      true),
  DAC_NAME(
      "dacName", Source.COLUMN, List.of("dac.name"), PropertyTable.NONE, JsonType.STRING, true),
  CONSENT_CODES(
      "consentCodes",
      Source.DERIVED,
      List.of("dataset.data_use"),
      PropertyTable.NONE,
      JsonType.STRING_ARRAY,
      false),
  PARTICIPANT_COUNT(
      "participantCount",
      Source.DATASET_PROPERTY,
      List.of(DatasetRegistrationSchemaV1Builder.numberOfParticipants),
      PropertyTable.DATASET,
      JsonType.INTEGER,
      true),
  AVAILABLE_DATE(
      "availableDate",
      Source.DERIVED,
      List.of("dataset.dac_approval_date", "dataset.create_date"),
      PropertyTable.NONE,
      JsonType.DATE,
      true),
  ACCESS_URL(
      "accessUrl",
      Source.DERIVED,
      // Older dataset-registration records persisted the consent-group path, as for access.
      List.of(
          DatasetRegistrationSchemaV1Builder.url,
          "consentGroup." + DatasetRegistrationSchemaV1Builder.url),
      PropertyTable.DATASET,
      JsonType.STRING,
      true),
  SPECIES(
      "species",
      Source.STUDY_PROPERTY,
      List.of(DatasetRegistrationSchemaV1Builder.species),
      PropertyTable.STUDY,
      JsonType.STRING,
      true);

  /**
   * The JSON type names of the field table. A holder class, because enum constants cannot refer to
   * the enum's own static fields.
   */
  private static final class JsonType {
    static final String STRING = "string";
    static final String STRING_ARRAY = "string[]";
    static final String INTEGER = "integer";
    static final String DATE = "date";

    private JsonType() {}
  }

  /** Where a field's value comes from. */
  public enum Source {
    /** A named table column, returned as stored. */
    COLUMN,
    /** A {@code dataset_property} row, matched on {@code schema_property}. */
    DATASET_PROPERTY,
    /** A {@code study_property} row, matched on {@code key}. */
    STUDY_PROPERTY,
    /**
     * Built from the listed columns or properties by converting, validating or choosing among them.
     * The value can still be stored text that passed validation, such as a provider link.
     */
    DERIVED
  }

  /**
   * The property table whose rows the projection reads for a field, through the allowlists below.
   * This is separate from {@link Source}, so that relabeling a field cannot change which rows are
   * read.
   */
  public enum PropertyTable {
    /** No property rows are read through the allowlists. */
    NONE,
    /** {@code dataset_property}, matched on {@code schema_property}. */
    DATASET,
    /** {@code study_property}, matched on {@code key}. */
    STUDY
  }

  private final String jsonName;
  private final Source source;
  private final List<String> sourceNames;
  private final PropertyTable propertyTable;
  private final String type;
  private final boolean nullable;

  ShowcasePublicField(
      String jsonName,
      Source source,
      List<String> sourceNames,
      PropertyTable propertyTable,
      String type,
      boolean nullable) {
    this.jsonName = jsonName;
    this.source = source;
    this.sourceNames = sourceNames;
    this.propertyTable = propertyTable;
    this.type = type;
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

  public PropertyTable getPropertyTable() {
    return propertyTable;
  }

  public String getType() {
    return type;
  }

  public boolean isNullable() {
    return nullable;
  }

  /** The {@code dataset_property.schema_property} values the projection may read. */
  public static List<String> datasetPropertyAllowlist() {
    return sourceNamesOf(PropertyTable.DATASET);
  }

  /** The {@code study_property.key} values the projection may read. */
  public static List<String> studyPropertyAllowlist() {
    return sourceNamesOf(PropertyTable.STUDY);
  }

  public static List<String> jsonNames() {
    return Arrays.stream(values()).map(ShowcasePublicField::getJsonName).toList();
  }

  private static List<String> sourceNamesOf(PropertyTable table) {
    return Arrays.stream(values())
        .filter(field -> field.propertyTable == table)
        .flatMap(field -> field.sourceNames.stream())
        .toList();
  }
}
