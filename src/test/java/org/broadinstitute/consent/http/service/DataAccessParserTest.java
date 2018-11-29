package org.broadinstitute.consent.http.service;

import com.vividsolutions.jts.util.Assert;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Test;
import java.io.IOException;
import java.util.*;

public class DataAccessParserTest {

    private DataAccessParser dataAccessParser;
    private Map<String, String> researcherProperties;
    private DACUserRole role;
    private Boolean manualReview;
    private Document dar;
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
    private final String OBJECT_ID = "SC-01253";
    private final String DATASET_NAME = "Dataset Name1";
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
    private final String DATA_ACCESS_AGREEMENT_URL = "/url/bucket/id-bucket-test";
    private final String DATA_ACCESS_AGREEMENT_NAME = "Researcher Name";
    private final String TRANSLATED_USE_RESTRICTION = "Translated use restriction.";
    public DataAccessParserTest() {
        this.dataAccessParser = new DataAccessParser();
        this.researcherProperties = new HashMap<>();
        this.dar = new Document();
        this.role = new DACUserRole();
    }

    @Test
    public void testDataAccessParserCompleted() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        researcherProperties.put(ResearcherFields.INSTITUTION.getValue(), INSTITUTION);
        researcherProperties.put(ResearcherFields.DEPARTMENT.getValue(), DEPARTMENT);
        researcherProperties.put(ResearcherFields.STREET_ADDRESS_1.getValue(), STREET_1);
        researcherProperties.put(ResearcherFields.CITY.getValue(), CITY);
        researcherProperties.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), POSTAL_CODE);
        researcherProperties.put(ResearcherFields.COUNTRY.getValue(), COUNTRY);
        researcherProperties.put(ResearcherFields.STATE.getValue(), STATE);
        researcherProperties.put(ResearcherFields.STREET_ADDRESS_2.getValue(), STREET_2);
        researcherProperties.put(ResearcherFields.DIVISION.getValue(), DIVISION);
        researcherProperties.put(DarConstants.ACADEMIC_BUSINESS_EMAIL, ACADEMIC_BUSINESS_EMAIL);
        researcherProperties.put(DarConstants.ERA_COMMONS_ID, ERA_COMMONS_ID);
        researcherProperties.put(DarConstants.PUBMED_ID, PUBMED_ID);
        researcherProperties.put(DarConstants.SCIENTIFIC_URL, SCIENTIFIC_URL);
        researcherProperties.put(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "true");
        researcherProperties.put(ResearcherFields.PROFILE_NAME.getValue(), PROFILE_NAME);
        dar.put(DarConstants.INVESTIGATOR, INVESTIGATOR);
        dar.put(DarConstants.PI_EMAIL, PI_EMAIL);
        dar.put(DarConstants.PROJECT_TITLE, PROJECT_TITLE);
        List<Integer> dataSets = Arrays.asList(DATASET_ID);
        dar.put(DarConstants.DATASET_ID, dataSets);
        ArrayList<Document> content = new ArrayList<>();
        content.add(generateDatasetDetails(DATASET_ID, DATASET_NAME, OBJECT_ID));
        dar.put(DarConstants.DATASET_DETAIL, content);
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
        dar.put(DarConstants.DATA_ACCESS_AGREEMENT_URL, DATA_ACCESS_AGREEMENT_URL);
        dar.put(DarConstants.DATA_ACCESS_AGREEMENT_NAME, DATA_ACCESS_AGREEMENT_NAME);
        dar.put(DarConstants.TRANSLATED_RESTRICTION, TRANSLATED_USE_RESTRICTION);
        this.manualReview = false;
        this.role.setStatus("approved");
        this.role.setRationale("granted bonafide");
        PDAcroForm acroForm = dataAccessParser.fillDARForm(dar, researcherProperties,role, manualReview, PDDocument.load(classLoader.getResourceAsStream(PATH)).getDocumentCatalog().getAcroForm(), TRANSLATED_USE_RESTRICTION);
        Assert.isTrue(acroForm.getField(ResearcherFields.INSTITUTION.getValue()).getValueAsString().equals(INSTITUTION));
        Assert.isTrue(acroForm.getField(ResearcherFields.DEPARTMENT.getValue()).getValueAsString().equals(DEPARTMENT));
        Assert.isTrue(acroForm.getField(ResearcherFields.STREET_ADDRESS_1.getValue()).getValueAsString().equals(STREET_1));
        Assert.isTrue(acroForm.getField(ResearcherFields.CITY.getValue()).getValueAsString().equals(CITY));
        Assert.isTrue(acroForm.getField(ResearcherFields.ZIP_POSTAL_CODE.getValue()).getValueAsString().equals(POSTAL_CODE));
        Assert.isTrue(acroForm.getField(ResearcherFields.COUNTRY.getValue()).getValueAsString().equals(COUNTRY));
        Assert.isTrue(acroForm.getField(ResearcherFields.STATE.getValue()).getValueAsString().equals(STATE));
        Assert.isTrue(acroForm.getField(ResearcherFields.STREET_ADDRESS_2.getValue()).getValueAsString().equals(STREET_2));
        Assert.isTrue(acroForm.getField(ResearcherFields.DIVISION.getValue()).getValueAsString().equals(DIVISION));
        Assert.isTrue(acroForm.getField(DarConstants.INVESTIGATOR).getValueAsString().equals(PROFILE_NAME));
        Assert.isTrue(acroForm.getField(DarConstants.ACADEMIC_BUSINESS_EMAIL).getValueAsString().equals(ACADEMIC_BUSINESS_EMAIL));
        Assert.isTrue(acroForm.getField(DarConstants.ERA_COMMONS_ID).getValueAsString().equals(ERA_COMMONS_ID));
        Assert.isTrue(acroForm.getField(DarConstants.PUBMED_ID).getValueAsString().equals(PUBMED_ID));
        Assert.isTrue(acroForm.getField(DarConstants.SCIENTIFIC_URL).getValueAsString().equals(SCIENTIFIC_URL));
        Assert.isTrue(acroForm.getField(DarConstants.PI_EMAIL).getValueAsString().equals(ACADEMIC_BUSINESS_EMAIL));
        Assert.isTrue(acroForm.getField(DarConstants.PROJECT_TITLE).getValueAsString().equals(PROJECT_TITLE));
        Assert.isTrue(acroForm.getField(DarConstants.DATASET_ID).getValueAsString().equals("Dataset Name1 | " + OBJECT_ID));
        Assert.isTrue(acroForm.getField(DarConstants.RUS).getValueAsString().equals(RUS));
        Assert.isTrue(acroForm.getField(DarConstants.NON_TECH_RUS).getValueAsString().equals(NON_TECH_RUS));
        Assert.isTrue(acroForm.getField(DarConstants.METHODS).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.CONTROLS).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.OTHER).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.CHECK_COLLABORATOR).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.NIH_USERNAME).getValueAsString().equals(NIH_USERNAME));
        Assert.isTrue(acroForm.getField(DarConstants.LINKEDIN).getValueAsString().equals(LINKEDIN));
        Assert.isTrue(acroForm.getField(DarConstants.ORCID).getValueAsString().equals(ORCID));
        Assert.isTrue(acroForm.getField(DarConstants.RESEARCHER_GATE).getValueAsString().equals(RESEARCHER_GATE));
        Assert.isTrue(acroForm.getField(DarConstants.DATA_ACCESS_AGREEMENT).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.OTHER_TEXT).getValueAsString().equals(RESEARCH_OTHER_TEXT));
        Assert.isTrue(acroForm.getField(DarConstants.ORIGINS).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.HEALTH).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.MANUAL_REVIEW).getValueAsString().equals(MANUAL_REVIEW));
        Assert.isTrue(acroForm.getField(DarConstants.USER_STATUS).getValueAsString().equals(USER_STATUS));
        Assert.isTrue(acroForm.getField(DarConstants.ADMIN_COMMENT).getValueAsString().equals(ADMIN_COMMENT));
        Assert.isTrue(acroForm.getField(DarConstants.TRANSLATED_RESTRICTION).getValueAsString().equals(TRANSLATED_USE_RESTRICTION));
    }

    private Document generateDatasetDetails(Integer datasetId, String datasetName, String objectId) {
        Document datasetDetails = new Document();
        datasetDetails.put("datasetId", datasetId.toString());
        datasetDetails.put("name", datasetName);
        datasetDetails.put("objectId", objectId);
        return datasetDetails;
    }

}
