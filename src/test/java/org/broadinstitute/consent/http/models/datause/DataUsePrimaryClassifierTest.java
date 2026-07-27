package org.broadinstitute.consent.http.models.datause;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassification.Shape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DataUsePrimaryClassifierTest {

  @Test
  void classifyEmptyAndSecondaryOnlyAsNone() {
    assertClassification(new DataUse(), Shape.NONE, List.of());
    assertClassification(
        new DataUseBuilder().setSecondaryOther("secondary text").build(), Shape.NONE, List.of());
  }

  @ParameterizedTest
  @MethodSource("singleCategoryDataUses")
  void classifyEverySingleCategory(DataUse dataUse, DataUsePrimaryCategory expectedCategory) {
    assertClassification(dataUse, Shape.SINGLE, List.of(expectedCategory));
  }

  static Stream<Arguments> singleCategoryDataUses() {
    return Stream.of(
        Arguments.of(new DataUseBuilder().setGeneralUse(true).build(), DataUsePrimaryCategory.GRU),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).build(), DataUsePrimaryCategory.HMB),
        Arguments.of(
            new DataUseBuilder().setPopulationOriginsAncestry(true).build(),
            DataUsePrimaryCategory.POA),
        Arguments.of(
            new DataUseBuilder().setDiseaseRestrictions(List.of("DOID:1")).build(),
            DataUsePrimaryCategory.DS),
        Arguments.of(
            new DataUseBuilder().setOther("primary text").build(), DataUsePrimaryCategory.OTHER));
  }

  @Test
  void classifyMultipleCategoriesInCanonicalOrder() {
    DataUse dataUse =
        new DataUseBuilder()
            .setOther("primary text")
            .setDiseaseRestrictions(List.of("DOID:1"))
            .setGeneralUse(true)
            .build();

    assertClassification(
        dataUse,
        Shape.MULTIPLE,
        List.of(
            DataUsePrimaryCategory.GRU, DataUsePrimaryCategory.DS, DataUsePrimaryCategory.OTHER));
  }

  @Test
  void classificationNormalizesDuplicatesInCanonicalOrder() {
    DataUsePrimaryClassification classification =
        new DataUsePrimaryClassification(
            List.of(
                DataUsePrimaryCategory.OTHER,
                DataUsePrimaryCategory.GRU,
                DataUsePrimaryCategory.OTHER));

    assertEquals(Shape.MULTIPLE, classification.shape());
    assertEquals(
        List.of(DataUsePrimaryCategory.GRU, DataUsePrimaryCategory.OTHER),
        classification.categories());
  }

  private static void assertClassification(
      DataUse dataUse, Shape shape, List<DataUsePrimaryCategory> categories) {
    DataUsePrimaryClassification classification = DataUsePrimaryClassifier.classify(dataUse);
    assertEquals(shape, classification.shape());
    assertEquals(categories, classification.categories());
  }
}
