package org.broadinstitute.consent.http.matching;

import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.Abstain;
import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.Approve;
import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.Deny;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.service.ontology.ParentTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataUseMatcherV4Test {

  private static final String CANCER_NODE = "http://purl.obolibrary.org/obo/DOID_162";
  private static final String INTESTINAL_CANCER_NODE = "http://purl.obolibrary.org/obo/DOID_10155";
  private final Gson gson = GsonUtil.getInstance();

  @Mock private OntologyService ontologyService;

  @Test
  void testDiseaseMatching_positive() {
    DataUse dataset = new DataUseBuilder().setDiseaseRestrictions(List.of(CANCER_NODE)).build();
    DataUse purpose =
        new DataUseBuilder().setDiseaseRestrictions(List.of(INTESTINAL_CANCER_NODE)).build();
    // Build a mock response of term parents based on what is returned when searching on DOID_10155
    List<OntologyTerm> termResources = new ArrayList<>();
    OntologyTerm resource =
        new OntologyTerm(INTESTINAL_CANCER_NODE, "v1", OntologyType.DOID.name());
    ParentTerm parent1 = new ParentTerm();
    parent1.setId("http://purl.obolibrary.org/obo/DOID_3119");
    ParentTerm parent2 = new ParentTerm();
    parent2.setId("http://purl.obolibrary.org/obo/DOID_0050686");
    ParentTerm parent3 = new ParentTerm();
    parent3.setId("http://purl.obolibrary.org/obo/DOID_162");
    ParentTerm parent4 = new ParentTerm();
    parent4.setId("http://purl.obolibrary.org/obo/DOID_14566");
    ParentTerm parent5 = new ParentTerm();
    parent5.setId("http://purl.obolibrary.org/obo/DOID_4");

    resource.addParent(parent1);
    resource.addParent(parent2);
    resource.addParent(parent3);
    resource.addParent(parent4);
    resource.addParent(parent5);

    termResources.add(resource);

    when(ontologyService.findByTermIds(new String[] {INTESTINAL_CANCER_NODE}))
        .thenReturn(
            output -> {
              // Streaming output that writes terms
              output.write(gson.toJson(termResources).getBytes(StandardCharsets.UTF_8));
            });
    assertApprove(purpose, dataset);
  }

  @Test
  void testDiseaseMatching_negative() {
    DataUse dataset =
        new DataUseBuilder().setDiseaseRestrictions(List.of(INTESTINAL_CANCER_NODE)).build();
    DataUse purpose = new DataUseBuilder().setDiseaseRestrictions(List.of(CANCER_NODE)).build();

    // Build a mock response of term parents based on what is returned when searching on DOID_162
    List<OntologyTerm> termResources = new ArrayList<>();
    OntologyTerm resource = new OntologyTerm(CANCER_NODE, "v1", OntologyType.DOID.name());
    ParentTerm parent1 = new ParentTerm();
    parent1.setId("http://purl.obolibrary.org/obo/DOID_14566");
    ParentTerm parent2 = new ParentTerm();
    parent2.setId("http://purl.obolibrary.org/obo/DOID_4");

    resource.addParent(parent1);
    resource.addParent(parent2);

    termResources.add(resource);

    when(ontologyService.findByTermIds(new String[] {CANCER_NODE}))
        .thenReturn(
            output -> {
              // Streaming output that writes terms
              output.write(gson.toJson(termResources).getBytes(StandardCharsets.UTF_8));
            });
    assertDeny(purpose, dataset);
  }

  @Test
  void testHMB_positive_case_1() {
    DataUse dataset = new DataUseBuilder().setGeneralUse(true).build();
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testHMB_positive_case_2() {
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testHMB_negative_case_1() {
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    DataUse purpose = new DataUseBuilder().setGeneralUse(true).build();
    assertDeny(purpose, dataset);
  }

  @Test
  void testHMB_negative_case_2() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).build();
    DataUse dataset = new DataUseBuilder().setDiseaseRestrictions(List.of(CANCER_NODE)).build();
    assertDeny(purpose, dataset);
  }

  @Test
  void testHMB_negative_case_3() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).build();
    DataUse dataset = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    assertDeny(purpose, dataset);
  }

  @Test
  void testPOA_positive_case_1() {
    DataUse dataset = new DataUseBuilder().setGeneralUse(true).build();
    DataUse purpose = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testPOA_positive_case_2() {
    DataUse dataset = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    DataUse purpose = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testPOA_negative_case_1() {
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    DataUse purpose = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    assertDeny(purpose, dataset);
  }

  @Test
  void testPOA_negative_case_2() {
    DataUse dataset = new DataUseBuilder().setDiseaseRestrictions(List.of(CANCER_NODE)).build();
    DataUse purpose = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    assertDeny(purpose, dataset);
  }

  @Test
  void testMDS_positive_case_1() {
    DataUse dataset = new DataUseBuilder().setGeneralUse(true).build();
    DataUse purpose = new DataUseBuilder().setMethodsResearch(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testMDS_positive_case_2() {
    DataUse dataset = new DataUseBuilder().setPopulationOriginsAncestry(true).build();
    DataUse purpose = new DataUseBuilder().setMethodsResearch(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testMDS_positive_case_3() {
    DataUse dataset = new DataUseBuilder().setDiseaseRestrictions(List.of(CANCER_NODE)).build();
    DataUse purpose = new DataUseBuilder().setMethodsResearch(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testMDS_positive_case_4() {
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    DataUse purpose = new DataUseBuilder().setMethodsResearch(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testNPU_positive_case_1() {
    DataUse dataset = new DataUseBuilder().setNonProfitUse(true).build();
    DataUse purpose = new DataUseBuilder().setNonProfitUse(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testNPU_positive_case_2() {
    DataUse dataset = new DataUseBuilder().setNonProfitUse(false).build();
    DataUse purpose = new DataUseBuilder().setNonProfitUse(false).build();
    assertApprove(purpose, dataset);
  }

  // This is a confusing case. When a dataset NPU == false, that means there are effectively NO
  // NPU restrictions on it. That means that any purpose NPU == true|false should be approved.
  @Test
  void testNPU_positive_case_3() {
    DataUse dataset = new DataUseBuilder().setNonProfitUse(false).build();
    DataUse purpose = new DataUseBuilder().setNonProfitUse(true).build();
    assertApprove(purpose, dataset);
  }

  @Test
  void testNPU_negative_case_1() {
    DataUse dataset = new DataUseBuilder().setNonProfitUse(true).build();
    DataUse purpose = new DataUseBuilder().setNonProfitUse(false).build();
    assertDeny(purpose, dataset);
  }

  @Test
  void testAbstainDecision_COL() {
    DataUse purpose = new DataUseBuilder().setCollaboratorRequired(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_Other() {
    DataUse purpose = new DataUseBuilder().setOther("other").build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_SecondaryOther() {
    DataUse purpose = new DataUseBuilder().setSecondaryOther("secondary other").build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_ethicsApprovalRequired() {
    DataUse purpose = new DataUseBuilder().setEthicsApprovalRequired(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_controls() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setControl(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_population() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setPopulation(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_gender() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setGender("M").build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_pediatric() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setPediatric(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_vulnerablePopulations() {
    DataUse purpose =
        new DataUseBuilder().setHmbResearch(true).setVulnerablePopulations(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_illegalBehavior() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setIllegalBehavior(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_sexualDiseases() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setSexualDiseases(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_psychologicalTraits() {
    DataUse purpose =
        new DataUseBuilder().setHmbResearch(true).setPsychologicalTraits(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_notHealth() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setNotHealth(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  @Test
  void testAbstainDecision_stigmatizedDiseases() {
    DataUse purpose = new DataUseBuilder().setHmbResearch(true).setStigmatizeDiseases(true).build();
    DataUse dataset = new DataUseBuilder().setHmbResearch(true).build();
    assertAbstain(purpose, dataset);
  }

  private void assertApprove(DataUse purpose, DataUse dataset) {
    MatchResult match = matchPurposeAndDataset(purpose, dataset);
    assertTrue(Approve(match.getMatchResultType()));
  }

  private void assertDeny(DataUse purpose, DataUse dataset) {
    MatchResult match = matchPurposeAndDataset(purpose, dataset);
    assertTrue(Deny(match.getMatchResultType()));
    assertFalse(match.getMessage().isEmpty());
  }

  private void assertAbstain(DataUse purpose, DataUse dataset) {
    MatchResult match = matchPurposeAndDataset(purpose, dataset);
    assertTrue(Abstain(match.getMatchResultType()));
    assertFalse(match.getMessage().isEmpty());
  }

  private MatchResult matchPurposeAndDataset(DataUse purpose, DataUse dataset) {
    DataUseMatcherV4 matcher = new DataUseMatcherV4();
    matcher.setOntologyService(ontologyService);
    return matcher.matchPurposeAndDatasetV4(purpose, dataset);
  }
}
