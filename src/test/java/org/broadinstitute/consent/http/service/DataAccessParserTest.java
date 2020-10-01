package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Test;

@SuppressWarnings("FieldCanBeLocal")
public class DataAccessParserTest {

    private final String INSTITUTION = "Institution Test";
    private final String DEPARTMENT = "Department Test";
    private final String STREET_1 = "Lombard Street 1254";
    private final String STREET_2 = "2600 Sloat Blvd";
    private final String CITY = "San Francisco";
    private final String POSTAL_CODE = "CA 94132";
    private final String COUNTRY = "Country Test";
    private final String STATE = "San Francisco State";
    private final String DIVISION = "Division 1";
    private final String INVESTIGATOR = "Veronica";
    private final String ACADEMIC_BUSINESS_EMAIL = "test@broadinstitute.org";
    private final String ERA_COMMONS_ID = "Commons Id";
    private final String PUBMED_ID = "Pubmed Id";
    private final String SCIENTIFIC_URL = "www.test.org";
    private final String PI_EMAIL = "vvtest@gmail.com";
    private final String PROJECT_TITLE = "Tittle Test";
    private final String DATASET_NAME = "Dataset Name";
    private final Integer DATASET_ID = 111;
    private final String RUS = "Purpose";
    private final String NON_TECH_RUS = "Summary";
    private final String RESEARCH_OTHER_TEXT = "Research Other Text";
    private final String PATH = "template/RequestApplication.pdf";
    private final String PROFILE_NAME = "Profile Name Test";
    private final String MANUAL_REVIEW = "THIS DATA ACCESS REQUEST DOES NOT REQUIRE MANUAL REVIEW";
    private final String USER_STATUS = "Approved";
    private final String ADMIN_COMMENT = "Granted bonafide";
    private final String NIH_USERNAME = "nih-test-username";
    private final String LINKEDIN = "linkedin-test-id";
    private final String ORCID = "0001-0002-00122";
    private final String RESEARCHER_GATE = "researcher-gate-0001-test";
    private final String TRANSLATED_USE_RESTRICTION = "Translated use restriction.";

    @Test
    public void testDataAccessParserCompleted() throws IOException {
        PDAcroForm acroForm = new DataAccessParser().
                fillDARForm(
                        populateDocument(),
                        populateProps(),
                        populateUser(),
                        false,
                        PDDocument.load(getTemplateResource()).getDocumentCatalog().getAcroForm(),
                        TRANSLATED_USE_RESTRICTION);
        assertEquals(acroForm.getField(ResearcherFields.INSTITUTION.getValue()).getValueAsString(), INSTITUTION);
        assertEquals(acroForm.getField(ResearcherFields.DEPARTMENT.getValue()).getValueAsString(), DEPARTMENT);
        assertEquals(acroForm.getField(ResearcherFields.STREET_ADDRESS_1.getValue()).getValueAsString(), STREET_1);
        assertEquals(acroForm.getField(ResearcherFields.CITY.getValue()).getValueAsString(), CITY);
        assertEquals(acroForm.getField(ResearcherFields.ZIP_POSTAL_CODE.getValue()).getValueAsString(), POSTAL_CODE);
        assertEquals(acroForm.getField(ResearcherFields.COUNTRY.getValue()).getValueAsString(), COUNTRY);
        assertEquals(acroForm.getField(ResearcherFields.STATE.getValue()).getValueAsString(), STATE);
        assertEquals(acroForm.getField(ResearcherFields.STREET_ADDRESS_2.getValue()).getValueAsString(), STREET_2);
        assertEquals(acroForm.getField(ResearcherFields.DIVISION.getValue()).getValueAsString(), DIVISION);
        assertEquals(acroForm.getField(DarConstants.INVESTIGATOR).getValueAsString(), PROFILE_NAME);
        assertEquals(acroForm.getField(DarConstants.ACADEMIC_BUSINESS_EMAIL).getValueAsString(), ACADEMIC_BUSINESS_EMAIL);
        assertEquals(acroForm.getField(DarConstants.ERA_COMMONS_ID).getValueAsString(), ERA_COMMONS_ID);
        assertEquals(acroForm.getField(DarConstants.PUBMED_ID).getValueAsString(), PUBMED_ID);
        assertEquals(acroForm.getField(DarConstants.SCIENTIFIC_URL).getValueAsString(), SCIENTIFIC_URL);
        assertEquals(acroForm.getField(DarConstants.PI_EMAIL).getValueAsString(), ACADEMIC_BUSINESS_EMAIL);
        assertEquals(acroForm.getField(DarConstants.PROJECT_TITLE).getValueAsString(), PROJECT_TITLE);
        assertEquals(acroForm.getField(DarConstants.RUS).getValueAsString(), RUS);
        assertEquals("Yes", acroForm.getField(DarConstants.METHODS).getValueAsString());
        assertEquals("Yes", acroForm.getField(DarConstants.CONTROLS).getValueAsString());
        assertEquals("Yes", acroForm.getField(DarConstants.OTHER).getValueAsString());
        assertEquals("Yes", acroForm.getField(DarConstants.CHECK_COLLABORATOR).getValueAsString());
        assertEquals(acroForm.getField(DarConstants.NIH_USERNAME).getValueAsString(), NIH_USERNAME);
        assertEquals(acroForm.getField(DarConstants.LINKEDIN).getValueAsString(), LINKEDIN);
        assertEquals(acroForm.getField(DarConstants.ORCID).getValueAsString(), ORCID);
        assertEquals(acroForm.getField(DarConstants.RESEARCHER_GATE).getValueAsString(), RESEARCHER_GATE);
        // Handle legacy all lower cased case
        assertEquals(acroForm.getField(DarConstants.OTHER_TEXT.toLowerCase()).getValueAsString(), RESEARCH_OTHER_TEXT);
        assertEquals("Yes", acroForm.getField(DarConstants.ORIGINS).getValueAsString());
        assertEquals("Yes", acroForm.getField(DarConstants.HEALTH).getValueAsString());
        assertEquals(acroForm.getField(DarConstants.MANUAL_REVIEW).getValueAsString(), MANUAL_REVIEW);
        assertEquals(acroForm.getField(DarConstants.USER_STATUS).getValueAsString(), USER_STATUS);
        assertEquals(acroForm.getField(DarConstants.ADMIN_COMMENT).getValueAsString(), ADMIN_COMMENT);
        assertEquals(acroForm.getField(DarConstants.TRANSLATED_RESTRICTION).getValueAsString(), TRANSLATED_USE_RESTRICTION);
    }

    @Test
    public void testInvalidFieldValue() throws Exception {
        Map<String, String> props = populateProps();
        props.put(ResearcherFields.PROFILE_NAME.getValue(), PROFILE_NAME + "\u200B");
        props.put(ResearcherFields.DEPARTMENT.getValue(), "\u0009" + DEPARTMENT);
        props.put(ResearcherFields.STREET_ADDRESS_1.getValue(), "\u200B" + STREET_1 + "\u00A0");
        PDAcroForm acroForm = new DataAccessParser().
                fillDARForm(
                        populateDocument(),
                        props,
                        populateUser(),
                        false,
                        PDDocument.load(getTemplateResource()).getDocumentCatalog().getAcroForm(),
                        TRANSLATED_USE_RESTRICTION);
        assertEquals(acroForm.getField(ResearcherFields.PROFILE_NAME.getValue()).getValueAsString(), PROFILE_NAME);
        assertEquals(acroForm.getField(ResearcherFields.DEPARTMENT.getValue()).getValueAsString(), DEPARTMENT);
        assertEquals(acroForm.getField(ResearcherFields.STREET_ADDRESS_1.getValue()).getValueAsString(), STREET_1);
    }

    private InputStream getTemplateResource() {
        return getClass().getClassLoader().getResourceAsStream(PATH);
    }

    private Document generateDatasetDetails(Integer datasetId) {
        Document datasetDetails = new Document();
        datasetDetails.put("datasetId", datasetId.toString());
        datasetDetails.put("name", DATASET_NAME);
        datasetDetails.put("objectId", "SC-01253");
        return datasetDetails;
    }

    private User populateUser() {
        User user = new User();
        user.setStatus("approved");
        user.setRationale("granted bonafide");
        return user;
    }

    private Map<String, String> populateProps() {
        Map<String, String> props = new HashMap<>();
        props.put(ResearcherFields.INSTITUTION.getValue(), INSTITUTION);
        props.put(ResearcherFields.DEPARTMENT.getValue(), DEPARTMENT);
        props.put(ResearcherFields.STREET_ADDRESS_1.getValue(), STREET_1);
        props.put(ResearcherFields.CITY.getValue(), CITY);
        props.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), POSTAL_CODE);
        props.put(ResearcherFields.COUNTRY.getValue(), COUNTRY);
        props.put(ResearcherFields.STATE.getValue(), STATE);
        props.put(ResearcherFields.STREET_ADDRESS_2.getValue(), STREET_2);
        props.put(ResearcherFields.DIVISION.getValue(), DIVISION);
        props.put(DarConstants.ACADEMIC_BUSINESS_EMAIL, ACADEMIC_BUSINESS_EMAIL);
        props.put(DarConstants.ERA_COMMONS_ID, ERA_COMMONS_ID);
        props.put(DarConstants.PUBMED_ID, PUBMED_ID);
        props.put(DarConstants.SCIENTIFIC_URL, SCIENTIFIC_URL);
        props.put(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "true");
        props.put(ResearcherFields.PROFILE_NAME.getValue(), PROFILE_NAME);
        return props;
    }

    private Document populateDocument() {
        Document dar = new Document();
        dar.put(DarConstants.INVESTIGATOR, INVESTIGATOR);
        dar.put(DarConstants.PI_EMAIL, PI_EMAIL);
        dar.put(DarConstants.PROJECT_TITLE, PROJECT_TITLE);
        List<Integer> dataSets = Collections.singletonList(DATASET_ID);
        dar.put(DarConstants.DATASET_ID, dataSets);
        ArrayList<Document> documentArrayList = new ArrayList<>();
        documentArrayList.add(generateDatasetDetails(DATASET_ID));
        dar.put(DarConstants.DATASET_DETAIL, documentArrayList);
        dar.put(DarConstants.RUS, RUS);
        dar.put(DarConstants.NON_TECH_RUS, NON_TECH_RUS);
        dar.put(DarConstants.METHODS, true);
        dar.put(DarConstants.CONTROLS, true);
        dar.put(DarConstants.OTHER, true);
        dar.put(DarConstants.POA, true);
        dar.put(DarConstants.HMB, true);
        dar.put(DarConstants.OTHER_TEXT, RESEARCH_OTHER_TEXT);
        dar.put(DarConstants.CHECK_COLLABORATOR, true);
        dar.put(DarConstants.NIH_USERNAME, NIH_USERNAME);
        dar.put(DarConstants.LINKEDIN, LINKEDIN);
        dar.put(DarConstants.ORCID, ORCID);
        dar.put(DarConstants.RESEARCHER_GATE, RESEARCHER_GATE);
        dar.put(DarConstants.TRANSLATED_RESTRICTION, TRANSLATED_USE_RESTRICTION);
        return dar;
    }

}
