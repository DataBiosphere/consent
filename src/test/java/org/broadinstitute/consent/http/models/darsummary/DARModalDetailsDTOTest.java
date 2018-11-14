package org.broadinstitute.consent.http.models.darsummary;

import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DARModalDetailsDTOTest {

    @Mock
    Document darDocument;

    ArrayList<Document> datasetDetail;

    private final String DAR_CODE = "DAR-1";
    private final String INVESTIGATOR = "Mocked Investigator";
    private final String INSTITUTION = "Mocked Institution";
    private final String TITLE = "Mocked Title";
    private final String OTHERTEXT = "Other text";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(darDocument.getString(DarConstants.DAR_CODE)).thenReturn(DAR_CODE);
        when(darDocument.getString(DarConstants.INVESTIGATOR)).thenReturn(INVESTIGATOR);
        when(darDocument.getString(DarConstants.INSTITUTION)).thenReturn(INSTITUTION);
        when(darDocument.getString(DarConstants.PROJECT_TITLE)).thenReturn(TITLE);
        when(darDocument.get(DarConstants.DATASET_DETAIL)).thenReturn(datasetDetail);

        when(darDocument.containsKey("diseases")).thenReturn(true);
        when(darDocument.getBoolean("diseases")).thenReturn(true);
        when(darDocument.containsKey("methods")).thenReturn(true);
        when(darDocument.getBoolean("methods")).thenReturn(true);
        when(darDocument.containsKey("controls")).thenReturn(true);
        when(darDocument.getBoolean("controls")).thenReturn(true);
        when(darDocument.containsKey("population")).thenReturn(true);
        when(darDocument.getBoolean("population")).thenReturn(true);
        when(darDocument.containsKey("other")).thenReturn(true);
        when(darDocument.getBoolean("other")).thenReturn(true);
        when(darDocument.getString("othertext")).thenReturn(OTHERTEXT);

        when(darDocument.get("ontologies")).thenReturn(ontologies());

        when(darDocument.getBoolean("forProfit")).thenReturn(false);
        when(darDocument.getBoolean("onegender")).thenReturn(true);
        when(darDocument.getString("gender")).thenReturn("F");
        when(darDocument.getBoolean("pediatric")).thenReturn(true);
        when(darDocument.getBoolean("illegalbehave")).thenReturn(true);
        when(darDocument.getBoolean("addiction")).thenReturn(true);
        when(darDocument.getBoolean("sexualdiseases")).thenReturn(true);
        when(darDocument.getBoolean("stigmatizediseases")).thenReturn(true);
        when(darDocument.getBoolean("vulnerablepop")).thenReturn(true);
        when(darDocument.getBoolean("popmigration")).thenReturn(true);
        when(darDocument.getBoolean("psychtraits")).thenReturn(true);
        when(darDocument.getBoolean("nothealth")).thenReturn(true);
        when(darDocument.getBoolean("popmigration")).thenReturn(true);

        when(darDocument.get(DarConstants.DATASET_DETAIL)).thenReturn(getDatasetDetail());
    }

    @Test
    public void generateModalDetailsDTO(){
        DARModalDetailsDTO modalDetailsDTO = new DARModalDetailsDTO(darDocument);
        modalDetailsDTO.getDarCode();
        assertTrue(modalDetailsDTO.getDarCode().equals(DAR_CODE));
        assertTrue(modalDetailsDTO.getInstitutionName().equals(INSTITUTION));
        assertTrue(modalDetailsDTO.getPrincipalInvestigator().equals(INVESTIGATOR));
        assertTrue(modalDetailsDTO.getProjectTitle().equals(TITLE));
        assertTrue(modalDetailsDTO.isTherePurposeStatements());
        assertTrue(modalDetailsDTO.isRequiresManualReview());
        assertTrue(modalDetailsDTO.isSensitivePopulation());
        assertTrue(modalDetailsDTO.isThereDiseases());

        assertTrue(modalDetailsDTO.getDiseases().size() == 3);
        assertTrue(modalDetailsDTO.getDiseases().get(0).equals("OD-1: Ontology One"));
        assertTrue(modalDetailsDTO.getDiseases().get(1).equals("OD-2: Ontology Two"));
        assertTrue(modalDetailsDTO.getDiseases().get(2).equals("OD-3: Ontology Three"));

        assertTrue(modalDetailsDTO.getPurposeStatements().size() == 10);

        assertTrue(modalDetailsDTO.getResearchType().size() == 5);
    }

    private List<Map<String, String>> ontologies(){
        Map<String, String> ontology1 = new HashMap<>();
        ontology1.put("label", "OD-1: Ontology One");
        Map<String, String> ontology2 = new HashMap<>();
        ontology2.put("label", "OD-2: Ontology Two");
        Map<String, String> ontology3 = new HashMap<>();
        ontology3.put("label", "OD-3: Ontology Three");
        return Arrays.asList(ontology1, ontology2, ontology3);
    }

    private ArrayList<Document> getDatasetDetail(){
        Document document = new Document();
        document.put("First:", "First Sample Detail");
        Document document1 = new Document();
        document.put("Second:", "Second Sample Detail");
        Document document2 = new Document();
        document.put("Third", "Thirs Sample Detail");
        ArrayList<Document> list = new ArrayList<>();
        list.add(document);
        list.add(document1);
        list.add(document2);
        return list;
    }
}