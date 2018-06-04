package org.broadinstitute.consent.http.service;

import com.vividsolutions.jts.util.Assert;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DataAccessParserTest {

    private DataAccessParser dataAccessParser;
    private Map<String, String> researcherProperties;
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
    private final String DATASET_ID = "SC-01253";
    private final String DATASET_ID_2 = "TX-01253";
    private final String RUS = "Purpose";
    private final String NON_TECH_RUS = "Summary";
    private final String RESEARCH_OTHER_TEXT = "Research Other Text";
    private final String PATH = "template/RequestApplication.pdf";

    public DataAccessParserTest() {
        this.dataAccessParser = new DataAccessParser();
        this.researcherProperties = new HashMap<>();
        this.dar = new Document();
    }

    @Test
    public void testDataAccessParserCompleted() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream(PATH);;
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
        dar.put(DarConstants.INVESTIGATOR, INVESTIGATOR);
        dar.put(DarConstants.PI_EMAIL, PI_EMAIL);
        dar.put(DarConstants.PROJECT_TITLE, PROJECT_TITLE);
        List<String> dataSets = Arrays.asList(DATASET_ID, DATASET_ID_2);
        dar.put(DarConstants.DATASET_ID, dataSets);
        dar.put(DarConstants.RUS, RUS);
        dar.put(DarConstants.NON_TECH_RUS, NON_TECH_RUS);
        dar.put(DarConstants.METHODS, true);
        dar.put(DarConstants.CONTROLS, true);
        dar.put(DarConstants.OTHER, true);
        dar.put(DarConstants.OTHER_TEXT, RESEARCH_OTHER_TEXT);
        PDAcroForm acroForm = dataAccessParser.fillDARForm(dar, researcherProperties, PDDocument.load(classLoader.getResourceAsStream(PATH)).getDocumentCatalog().getAcroForm());
        Assert.isTrue(acroForm.getField(ResearcherFields.INSTITUTION.getValue()).getValueAsString().equals(INSTITUTION));
        Assert.isTrue(acroForm.getField(ResearcherFields.DEPARTMENT.getValue()).getValueAsString().equals(DEPARTMENT));
        Assert.isTrue(acroForm.getField(ResearcherFields.STREET_ADDRESS_1.getValue()).getValueAsString().equals(STREET_1));
        Assert.isTrue(acroForm.getField(ResearcherFields.CITY.getValue()).getValueAsString().equals(CITY));
        Assert.isTrue(acroForm.getField(ResearcherFields.ZIP_POSTAL_CODE.getValue()).getValueAsString().equals(POSTAL_CODE));
        Assert.isTrue(acroForm.getField(ResearcherFields.COUNTRY.getValue()).getValueAsString().equals(COUNTRY));
        Assert.isTrue(acroForm.getField(ResearcherFields.STATE.getValue()).getValueAsString().equals(STATE));
        Assert.isTrue(acroForm.getField(ResearcherFields.STREET_ADDRESS_2.getValue()).getValueAsString().equals(STREET_2));
        Assert.isTrue(acroForm.getField(ResearcherFields.DIVISION.getValue()).getValueAsString().equals(DIVISION));
        Assert.isTrue(acroForm.getField(DarConstants.INVESTIGATOR).getValueAsString().equals(INVESTIGATOR));
        Assert.isTrue(acroForm.getField(DarConstants.ACADEMIC_BUSINESS_EMAIL).getValueAsString().equals(ACADEMIC_BUSINESS_EMAIL));
        Assert.isTrue(acroForm.getField(DarConstants.ERA_COMMONS_ID).getValueAsString().equals(ERA_COMMONS_ID));
        Assert.isTrue(acroForm.getField(DarConstants.PUBMED_ID).getValueAsString().equals(PUBMED_ID));
        Assert.isTrue(acroForm.getField(DarConstants.SCIENTIFIC_URL).getValueAsString().equals(SCIENTIFIC_URL));
        Assert.isTrue(acroForm.getField(DarConstants.PI_EMAIL).getValueAsString().equals(PI_EMAIL));
        Assert.isTrue(acroForm.getField(DarConstants.PROJECT_TITLE).getValueAsString().equals(PROJECT_TITLE));
        acroForm.getField(DarConstants.DATASET_ID).getValueAsString().equals(DATASET_ID + ", " + DATASET_ID_2);
        Assert.isTrue(acroForm.getField(DarConstants.RUS).getValueAsString().equals(RUS));
        Assert.isTrue(acroForm.getField(DarConstants.NON_TECH_RUS).getValueAsString().equals(NON_TECH_RUS));
        Assert.isTrue(acroForm.getField(DarConstants.METHODS).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.CONTROLS).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.OTHER).getValueAsString().equals("Yes"));
        Assert.isTrue(acroForm.getField(DarConstants.OTHER_TEXT).getValueAsString().equals(RESEARCH_OTHER_TEXT));
        Assert.isTrue(acroForm.getField(DarConstants.ONTOLOGIES).getValueAsString().equals("cancer"));

    }


}
