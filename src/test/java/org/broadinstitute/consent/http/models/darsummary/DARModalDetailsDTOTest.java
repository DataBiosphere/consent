package org.broadinstitute.consent.http.models.darsummary;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(darDocument.getString(DarConstants.DAR_CODE)).thenReturn(DAR_CODE);
        when(darDocument.getString(DarConstants.INVESTIGATOR)).thenReturn(INVESTIGATOR);
        when(darDocument.getString(DarConstants.INSTITUTION)).thenReturn(INSTITUTION);
        when(darDocument.getString(DarConstants.PROJECT_TITLE)).thenReturn(TITLE);
        when(darDocument.get(DarConstants.DATASET_DETAIL)).thenReturn(datasetDetail);
        when(darDocument.containsKey(DarConstants.DISEASES)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.DISEASES)).thenReturn(true);
        when(darDocument.containsKey(DarConstants.METHODS)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.METHODS)).thenReturn(true);
        when(darDocument.containsKey(DarConstants.CONTROLS)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.CONTROLS)).thenReturn(true);
        when(darDocument.containsKey(DarConstants.POPULATION)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.POPULATION)).thenReturn(true);
        when(darDocument.containsKey(DarConstants.OTHER)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.OTHER)).thenReturn(true);
        when(darDocument.getString(DarConstants.OTHER_TEXT)).thenReturn(OTHERTEXT);
        when(darDocument.get(DarConstants.ONTOLOGIES)).thenReturn(ontologies());
        when(darDocument.getBoolean(DarConstants.FOR_PROFIT)).thenReturn(false);
        when(darDocument.getBoolean(DarConstants.ONE_GENDER)).thenReturn(true);
        when(darDocument.getString(DarConstants.GENDER)).thenReturn("F");
        when(darDocument.getBoolean(DarConstants.PEDIATRIC)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.ILLEGAL_BEHAVE)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.ADDICTION)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.SEXUAL_DISEASES)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.STIGMATIZED_DISEASES)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.VULNERABLE_POP)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.POP_MIGRATION)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.PSYCH_TRAITS)).thenReturn(true);
        when(darDocument.getBoolean(DarConstants.NOT_HEALTH)).thenReturn(true);
        when(darDocument.get(DarConstants.DATASET_DETAIL)).thenReturn(getDatasetDetail());
    }

    @Test
    public void generateModalDetailsDTO(){
        DARModalDetailsDTO modalDetailsDTO = new DARModalDetailsDTO()
            .setDarCode(darDocument.getString(DarConstants.DAR_CODE))
            .setPrincipalInvestigator(darDocument.getString(DarConstants.INVESTIGATOR))
            .setInstitutionName(darDocument.getString(DarConstants.INSTITUTION))
            .setProjectTitle(darDocument.getString(DarConstants.PROJECT_TITLE))
            .setDepartment(darDocument.getString(DarConstants.DEPARTMENT))
            .setCity(darDocument.getString(DarConstants.CITY))
            .setCountry(darDocument.getString(DarConstants.COUNTRY))
            .setNihUsername(darDocument.getString(DarConstants.NIH_USERNAME))
            .setHaveNihUsername(StringUtils.isNotEmpty(darDocument.getString(DarConstants.NIH_USERNAME)))
            .setIsThereDiseases(false)
            .setIsTherePurposeStatements(false)
            .setResearchType(darDocument)
            .setDiseases(darDocument)
            .setPurposeStatements(darDocument)
            .setDatasets(Collections.emptyList());
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