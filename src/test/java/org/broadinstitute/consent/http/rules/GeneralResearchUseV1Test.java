package org.broadinstitute.consent.http.rules;

import java.util.Collections;
import java.util.List;
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
class GeneralResearchUseV1Test {

  private static Stream<Arguments> testCompare() {
    return Stream.of(
        Arguments.of(
            new DataUseBuilder()
                .setGeneralUse(true)
                .setDiseaseRestrictions(List.of("setDiseaseRestrictions")),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setDiseaseRestrictions(null),
            new DataAccessRequestDataBuilder().setHmb(true),
            true),
        Arguments.of(
            new DataUseBuilder()
                .setGeneralUse(true)
                .setDiseaseRestrictions(Collections.emptyList()),
            new DataAccessRequestDataBuilder().setHmb(true),
            true),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setNonProfitUse(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setMethodsResearch(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setCollaboratorRequired(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setEthicsApprovalRequired(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setGeneticStudiesOnly(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder()
                .setGeneralUse(true)
                .setGeographicalRestrictions("setGeographicalRestrictions"),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setIllegalBehavior(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setOther("setOther"),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setPopulationOriginsAncestry(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setPopulation(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder()
                .setGeneralUse(true)
                .setPublicationMoratorium("setPublicationMoratorium"),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setSecondaryOther("setSecondaryOther"),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setPublicationResults(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setAiLlmUse(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setControl(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setGender("Gender"),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setStigmatizeDiseases(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setNotHealth(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setPediatric(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setPsychologicalTraits(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setSexualDiseases(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true).setVulnerablePopulations(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setHmb(true),
            true),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(false),
            new DataAccessRequestDataBuilder().setHmb(true),
            false),
        Arguments.of(new DataUseBuilder(), new DataAccessRequestDataBuilder().setHmb(true), false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setDiseases(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setOther(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setOtherText("Other Condition"),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setAiLlmUse(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setControls(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setPopulation(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setForProfit(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setGender("Gender"),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setPediatric(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setVulnerablePopulation(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setIllegalBehavior(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setSexualDiseases(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setPsychiatricTraits(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setNotHealth(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setStigmatizedDiseases(true),
            false),
        Arguments.of(
            new DataUseBuilder().setGeneralUse(true),
            new DataAccessRequestDataBuilder().setAddiction(true),
            false));
  }

  @ParameterizedTest
  @MethodSource
  void testCompare(
      DataUseBuilder dataUseBuilder, DataAccessRequestDataBuilder dataBuilder, boolean expected) {
    Dataset dataset = new Dataset();
    dataset.setDataUse(dataUseBuilder.build());
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(dataBuilder.build());
    GeneralResearchUseV1 rule = new GeneralResearchUseV1();

    Assertions.assertEquals(expected, rule.compare(dataset, dataAccessRequest));
  }
}
