package org.broadinstitute.consent.http.rules;

import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.util.DataAccessRequestDataBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HealthMedicalBioMedicalV1Test {

  private static Stream<Arguments> testCompare() {
    return Stream.of(
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setHmb(true), true),
        Arguments.of(new DataUseBuilder().setHmbResearch(false),
            new DataAccessRequestDataBuilder().setHmb(true), false),
        Arguments.of(new DataUseBuilder(), new DataAccessRequestDataBuilder().setHmb(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setOther(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setOtherText("Other Condition"), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setControls(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setPopulation(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setForProfit(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setGender("Gender"), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setPediatric(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setVulnerablePopulation(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setIllegalBehavior(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setSexualDiseases(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setPsychiatricTraits(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setNotHealth(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setStigmatizedDiseases(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setAddiction(true), false));
  }

  @ParameterizedTest
  @MethodSource
  void testCompare(DataUseBuilder dataUseBuilder, DataAccessRequestDataBuilder dataBuilder,
      boolean expected) {
    Dataset dataset = new Dataset();
    dataset.setDataUse(dataUseBuilder.build());
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(dataBuilder.build());
    HealthMedicalBioMedicalV1 rule = new HealthMedicalBioMedicalV1();

    Assertions.assertEquals(expected, rule.compare(dataset, dataAccessRequest));
  }
}
