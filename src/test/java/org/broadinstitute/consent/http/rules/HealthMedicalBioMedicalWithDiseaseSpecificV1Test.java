package org.broadinstitute.consent.http.rules;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.OntologyEntry;
import org.broadinstitute.consent.http.util.DataAccessRequestDataBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HealthMedicalBioMedicalWithDiseaseSpecificV1Test {

  private static Stream<Arguments> testCompare() {
    return Stream.of(
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(List.of("setDiseaseRestrictions")), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(null), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), true),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setDiseaseRestrictions(
            Collections.emptyList()), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), true),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setNonProfitUse(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setMethodsResearch(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setCollaboratorRequired(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setEthicsApprovalRequired(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setGeneticStudiesOnly(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setGeographicalRestrictions("setGeographicalRestrictions"), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setIllegalBehavior(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setOther("setOther"), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setPopulationOriginsAncestry(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setPopulation(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setPublicationMoratorium("setPublicationMoratorium"), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setSecondaryOther("setSecondaryOther"), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setPublicationResults(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setAiLlmUse(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setControl(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setGender("Gender"), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setStigmatizeDiseases(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setNotHealth(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setPediatric(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setPsychologicalTraits(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setSexualDiseases(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true).setVulnerablePopulations(true), new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
            List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())), true),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(false).setOntologies(
                List.of(new OntologyEntry())), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                Collections.emptyList()), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(false),
            new DataAccessRequestDataBuilder().setHmb(true), false),
        Arguments.of(new DataUseBuilder(), new DataAccessRequestDataBuilder().setHmb(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setOtherText("Other Condition"),
            false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setAiLlmUse(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setControls(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setPopulation(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setForProfit(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setGender("Gender"), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setPediatric(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setVulnerablePopulation(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setIllegalBehavior(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setSexualDiseases(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setPsychiatricTraits(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setNotHealth(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setStigmatizedDiseases(true), false),
        Arguments.of(new DataUseBuilder().setHmbResearch(true),
            new DataAccessRequestDataBuilder().setDiseases(true).setOntologies(
                List.of(new OntologyEntry())).setOther(true).setAddiction(true), false));
  }

  @ParameterizedTest
  @MethodSource
  void testCompare(DataUseBuilder dataUseBuilder, DataAccessRequestDataBuilder dataBuilder,
      boolean expected) {
    Dataset dataset = new Dataset();
    dataset.setDataUse(dataUseBuilder.build());
    DataAccessRequest dataAccessRequest = new DataAccessRequest();
    dataAccessRequest.setData(dataBuilder.build());
    HealthMedicalBioMedicalWithDiseaseSpecificV1 rule = new HealthMedicalBioMedicalWithDiseaseSpecificV1();

    Assertions.assertEquals(expected, rule.compare(dataset, dataAccessRequest));
  }
}
