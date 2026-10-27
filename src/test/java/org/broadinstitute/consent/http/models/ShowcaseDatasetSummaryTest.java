package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField.PropertyTable;
import org.broadinstitute.consent.http.enumeration.ShowcasePublicField.Source;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

class ShowcaseDatasetSummaryTest {

  /** Responses must use the null-serializing Gson so that every key is present. */
  private final Gson gson = GsonUtil.getNullSerializingInstance();

  /** The Java type of each {@code type} in {@link ShowcasePublicField}. */
  private static final Map<String, String> JAVA_TYPES =
      Map.of(
          "string", "java.lang.String",
          "string[]", "java.util.List<java.lang.String>",
          "integer", "java.lang.Integer",
          "date", "java.lang.String");

  @Test
  void testSerializedKeysEqualAllowlist() {
    // Null only where the allowlist allows it.
    ShowcaseDatasetSummary summary =
        new ShowcaseDatasetSummary(
            "DUOS-000001", "Title", null, null, List.of(), null, null, null, null);
    JsonObject json = gson.toJsonTree(summary).getAsJsonObject();

    // Field order is checked on the record components; Gson's key order is not specified.
    assertEquals(Set.copyOf(ShowcasePublicField.jsonNames()), json.keySet());
  }

  @Test
  void testRecordComponentsEqualAllowlist() {
    List<String> components =
        Arrays.stream(ShowcaseDatasetSummary.class.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();

    assertEquals(ShowcasePublicField.jsonNames(), components);
  }

  @Test
  void testRecordComponentTypesMatchAllowlist() {
    Map<String, Type> componentTypes =
        Arrays.stream(ShowcaseDatasetSummary.class.getRecordComponents())
            .collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getGenericType));

    for (ShowcasePublicField field : ShowcasePublicField.values()) {
      Type type = componentTypes.get(field.getJsonName());
      String javaType =
          type instanceof ParameterizedType ? type.getTypeName() : ((Class<?>) type).getName();
      assertEquals(JAVA_TYPES.get(field.getType()), javaType, field.getJsonName());
    }
  }

  @Test
  void testPropertyAllowlists() {
    assertEquals(
        List.of("numberOfParticipants", "url", "consentGroup.url"),
        ShowcasePublicField.datasetPropertyAllowlist());
    assertEquals(List.of("species"), ShowcasePublicField.studyPropertyAllowlist());
  }

  /** A field labeled as a property row must read that table; a plain column reads none. */
  @Test
  void testPropertySourcesReadTheirTable() {
    Map<Source, PropertyTable> tables =
        Map.of(
            Source.COLUMN, PropertyTable.NONE,
            Source.DATASET_PROPERTY, PropertyTable.DATASET,
            Source.STUDY_PROPERTY, PropertyTable.STUDY);

    for (ShowcasePublicField field : ShowcasePublicField.values()) {
      if (tables.containsKey(field.getSource())) {
        assertEquals(tables.get(field.getSource()), field.getPropertyTable(), field.getJsonName());
      }
    }
  }
}
