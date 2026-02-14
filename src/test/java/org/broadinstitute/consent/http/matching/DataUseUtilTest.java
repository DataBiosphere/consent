package org.broadinstitute.consent.http.matching;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.OntologyType;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.ontology.OntologyTerm;
import org.broadinstitute.consent.http.service.ontology.ParentTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataUseUtilTest {

  private final Gson gson = GsonUtil.getInstance();

  @Mock private OntologyService ontologyService;

  private DataUseUtil dataUseUtil;

  @BeforeEach
  void setUp() {
    dataUseUtil = new DataUseUtil(ontologyService);
  }

  @Test
  void testGeneratePurposeDiseaseIdMap() throws Exception {
    OntologyTerm term = mockTerm();
    String jsonOutput = gson.toJson(List.of(term));
    when(ontologyService.findByTermIds(new String[] {"http://purl.obolibrary.org/obo/DOID_6"}))
        .thenReturn(output -> output.write(jsonOutput.getBytes()));

    Map<String, List<String>> termMap = dataUseUtil.generatePurposeDiseaseIdMap(List.of(term.id()));
    assertTrue(termMap.containsKey(term.id()));
    List<String> parentIds = termMap.get(term.id());
    term.getParents().forEach(parent -> assertTrue(parentIds.contains(parent.getId())));
  }

  @Test
  void testGeneratePurposeDiseaseIdMapEmpty() throws Exception {
    Map<String, List<String>> termMap = dataUseUtil.generatePurposeDiseaseIdMap(null);
    assertTrue(termMap.isEmpty());
  }

  @Test
  void testGetParentTermIds() throws Exception {
    OntologyTerm term = mockTerm();
    String jsonOutput = gson.toJson(List.of(term));
    when(ontologyService.findByTermIds(new String[] {"http://purl.obolibrary.org/obo/DOID_6"}))
        .thenReturn(output -> output.write(jsonOutput.getBytes()));

    List<String> parentIds = dataUseUtil.getParentTermIds(term.id());
    term.getParents().forEach(parent -> assertTrue(parentIds.contains(parent.getId())));
  }

  private OntologyTerm mockTerm() {
    OntologyTerm term =
        new OntologyTerm("http://purl.obolibrary.org/obo/DOID_6", "v1", OntologyType.DOID.name());
    ParentTerm parent1 = new ParentTerm();
    parent1.setId("http://purl.obolibrary.org/obo/DOID_5");
    ParentTerm parent2 = new ParentTerm();
    parent2.setId("http://purl.obolibrary.org/obo/DOID_4");
    ParentTerm parent3 = new ParentTerm();
    parent3.setId("http://purl.obolibrary.org/obo/DOID_3");
    ParentTerm parent4 = new ParentTerm();
    parent4.setId("http://purl.obolibrary.org/obo/DOID_2");
    ParentTerm parent5 = new ParentTerm();
    parent5.setId("http://purl.obolibrary.org/obo/DOID_1");
    term.addParent(parent1);
    term.addParent(parent2);
    term.addParent(parent3);
    term.addParent(parent4);
    term.addParent(parent5);
    return term;
  }
}
