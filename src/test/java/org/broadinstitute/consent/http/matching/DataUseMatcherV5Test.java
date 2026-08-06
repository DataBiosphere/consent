package org.broadinstitute.consent.http.matching;

import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.ABSTAIN;
import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.APPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataUseMatcherV5Test {

  private static final String DISEASE = "http://example.org/disease/1";
  private static final String SENSITIVE_OTHER_TEXT = "sensitive free text";

  @Mock private DataUseMatcherV4 dataUseMatcherV4;

  @ParameterizedTest
  @MethodSource("unsupportedDatasetPurposeMatrix")
  void unsupportedDatasetShapesAbstain(DataUse dataset, DataUse purpose, String expectedRationale) {
    DataUseMatcherV5 matcher = new DataUseMatcherV5(dataUseMatcherV4);

    MatchResult result = matcher.matchPurposeAndDatasetV5(purpose, dataset);

    assertEquals(ABSTAIN, result.getMatchResultType());
    assertEquals(List.of(expectedRationale), result.getMessage());
    assertFalse(result.getMessage().contains(SENSITIVE_OTHER_TEXT));
    verifyNoInteractions(dataUseMatcherV4);
  }

  @ParameterizedTest
  @MethodSource("canonicalPrimaryDatasets")
  void supportedSinglePrimaryDatasetsDelegateUnchanged(DataUse dataset) {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).build();
    MatchResult v4Result = MatchResult.from(APPROVE, List.of("unchanged V4 rationale"));
    when(dataUseMatcherV4.matchPurposeAndDatasetV4(purpose, dataset)).thenReturn(v4Result);

    MatchResult result =
        new DataUseMatcherV5(dataUseMatcherV4).matchPurposeAndDatasetV5(purpose, dataset);

    assertSame(v4Result, result);
  }

  @ParameterizedTest
  @MethodSource("allMultiplePrimaryDatasets")
  void everyMultiplePrimaryCombinationAbstains(DataUse dataset) {
    MatchResult result =
        new DataUseMatcherV5(dataUseMatcherV4)
            .matchPurposeAndDatasetV5(new DataUseBuilder().setHmbResearch(true).build(), dataset);

    assertEquals(ABSTAIN, result.getMatchResultType());
    assertEquals(List.of(DataUseMatcherV5.MULTIPLE_PRIMARY_RATIONALE), result.getMessage());
    verifyNoInteractions(dataUseMatcherV4);
  }

  @Test
  void observedLegacyHmbOtherCombinationAbstainsWithoutExposingOtherText() {
    DataUse dataset =
        new DataUseBuilder().setHmbResearch(true).setOther(SENSITIVE_OTHER_TEXT).build();

    MatchResult result =
        new DataUseMatcherV5(dataUseMatcherV4)
            .matchPurposeAndDatasetV5(new DataUseBuilder().setHmbResearch(true).build(), dataset);

    assertEquals(ABSTAIN, result.getMatchResultType());
    assertEquals(List.of(DataUseMatcherV5.MULTIPLE_PRIMARY_RATIONALE), result.getMessage());
    assertFalse(result.getMessage().contains(SENSITIVE_OTHER_TEXT));
  }

  private static Stream<Arguments> unsupportedDatasetPurposeMatrix() {
    List<Arguments> datasetCases =
        List.of(
            Arguments.of(null, DataUseMatcherV5.MISSING_PRIMARY_RATIONALE),
            Arguments.of(new DataUseBuilder().build(), DataUseMatcherV5.MISSING_PRIMARY_RATIONALE),
            Arguments.of(
                new DataUseBuilder().setSecondaryOther(SENSITIVE_OTHER_TEXT).build(),
                DataUseMatcherV5.MISSING_PRIMARY_RATIONALE),
            Arguments.of(
                new DataUseBuilder().setOther(SENSITIVE_OTHER_TEXT).build(),
                DataUseMatcherV5.OTHER_PRIMARY_RATIONALE),
            Arguments.of(
                new DataUseBuilder().setGeneralUse(true).setHmbResearch(true).build(),
                DataUseMatcherV5.MULTIPLE_PRIMARY_RATIONALE),
            Arguments.of(
                new DataUseBuilder().setHmbResearch(true).setOther(SENSITIVE_OTHER_TEXT).build(),
                DataUseMatcherV5.MULTIPLE_PRIMARY_RATIONALE));

    return datasetCases.stream()
        .flatMap(
            datasetCase ->
                researchPurposes()
                    .map(
                        purpose ->
                            Arguments.of(datasetCase.get()[0], purpose, datasetCase.get()[1])));
  }

  private static Stream<DataUse> researchPurposes() {
    return Stream.of(
        new DataUseBuilder().setGeneralUse(true).build(),
        new DataUseBuilder().setHmbResearch(true).build(),
        new DataUseBuilder().setPopulationOriginsAncestry(true).build(),
        new DataUseBuilder().setDiseaseRestrictions(List.of(DISEASE)).build(),
        new DataUseBuilder().setMethodsResearch(true).build());
  }

  private static Stream<DataUse> canonicalPrimaryDatasets() {
    return Stream.of(
        new DataUseBuilder().setGeneralUse(true).build(),
        new DataUseBuilder().setHmbResearch(true).build(),
        new DataUseBuilder().setPopulationOriginsAncestry(true).build(),
        new DataUseBuilder().setDiseaseRestrictions(List.of(DISEASE)).build());
  }

  private static Stream<DataUse> allMultiplePrimaryDatasets() {
    DataUsePrimaryCategory[] categories = DataUsePrimaryCategory.values();
    return IntStream.range(0, 1 << categories.length)
        .filter(mask -> Integer.bitCount(mask) > 1)
        .mapToObj(
            mask -> {
              List<DataUsePrimaryCategory> selected = new ArrayList<>();
              for (int index = 0; index < categories.length; index++) {
                if ((mask & (1 << index)) != 0) {
                  selected.add(categories[index]);
                }
              }
              return dataUseWithPrimaryCategories(selected);
            });
  }

  private static DataUse dataUseWithPrimaryCategories(List<DataUsePrimaryCategory> categories) {
    DataUse dataUse = new DataUse();
    categories.forEach(
        category -> {
          switch (category) {
            case GRU -> dataUse.setGeneralUse(true);
            case HMB -> dataUse.setHmbResearch(true);
            case POA -> dataUse.setPopulationOriginsAncestry(true);
            case DS -> dataUse.setDiseaseRestrictions(List.of(DISEASE));
            case OTHER -> dataUse.setOther(SENSITIVE_OTHER_TEXT);
          }
        });
    return dataUse;
  }
}
