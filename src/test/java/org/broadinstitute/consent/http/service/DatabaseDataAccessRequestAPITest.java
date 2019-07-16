package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class DatabaseDataAccessRequestAPITest {

    @Mock
    private MongoConsentDB mongo;
    @Mock
    private UseRestrictionConverter converter;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private ResearcherPropertyDAO researcherPropertyDAO;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private DACUserDAO dacUserDAO;
    @Mock
    private DataSetDAO dataSetDAO;

    private final Integer USER_ID = 3333;

    private final String LINKEDIN_PROFILE = "https://www.linkedin.com/in/veronicatest/";

    private final String ORCID = "1658474585212365";

    private final String RESEARCHER_GATE = "researcher_gate";

    private final String TRANSLATED_USE_RESTRICTION = "Translated use restriction.";

    DatabaseDataAccessRequestAPI databaseDataAccessRequestAPI;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        databaseDataAccessRequestAPI = new DatabaseDataAccessRequestAPI(mongo, converter, electionDAO, consentDAO, voteDAO, dacUserDAO, dataSetDAO, researcherPropertyDAO);
    }

    @Test
    public void testUpdateResearcherWithLinkedInIdentification() throws Exception {
        when(researcherPropertyDAO.findPropertyValueByPK(USER_ID, ResearcherFields.LINKEDIN_PROFILE.getValue())).thenReturn(LINKEDIN_PROFILE);
        when(researcherPropertyDAO.findPropertyValueByPK(USER_ID, ResearcherFields.ORCID.getValue())).thenReturn(ORCID);
        when(researcherPropertyDAO.findPropertyValueByPK(USER_ID, ResearcherFields.RESEARCHER_GATE.getValue())).thenReturn(RESEARCHER_GATE);
        Document dar = getDocument("https://www.linkedin.com/in/veronica/", ORCID, RESEARCHER_GATE);
        List<ResearcherProperty> rpList = new ArrayList<>();
        rpList.add(new ResearcherProperty(USER_ID, ResearcherFields.LINKEDIN_PROFILE.getValue(),"https://www.linkedin.com/in/veronica/"));
        rpList.add(new ResearcherProperty(USER_ID, ResearcherFields.ORCID.getValue(), ORCID));
        rpList.add(new ResearcherProperty(USER_ID, ResearcherFields.RESEARCHER_GATE.getValue(), RESEARCHER_GATE));
        List<ResearcherProperty> properties = databaseDataAccessRequestAPI.updateResearcherIdentification(dar);
        verify(researcherPropertyDAO, times(1)).insertAll(anyList());
        Assert.assertTrue(properties.size() == 3);
        properties.stream().forEach(researcherProperty -> {
            if(researcherProperty.getPropertyKey().equals(ResearcherFields.LINKEDIN_PROFILE.getValue())){
                Assert.assertTrue(researcherProperty.getPropertyValue().equals("https://www.linkedin.com/in/veronica/"));
            }
            else if(researcherProperty.getPropertyKey().equals(ResearcherFields.ORCID.getValue())){
                Assert.assertTrue(researcherProperty.getPropertyValue().equals(ORCID));
            }
            else if(researcherProperty.getPropertyKey().equals(ResearcherFields.RESEARCHER_GATE.getValue())){
                Assert.assertTrue(researcherProperty.getPropertyValue().equals(RESEARCHER_GATE));
            }
        });
    }

    @Test
    public void testUpdateResearcherWithOrcIdAndNullLinkedInAndResearcherGateIdentification() throws Exception {
        when(researcherPropertyDAO.findPropertyValueByPK(USER_ID, ResearcherFields.LINKEDIN_PROFILE.getValue())).thenReturn(LINKEDIN_PROFILE);
        when(researcherPropertyDAO.findPropertyValueByPK(USER_ID, ResearcherFields.ORCID.getValue())).thenReturn(ORCID);
        when(researcherPropertyDAO.findPropertyValueByPK(USER_ID, ResearcherFields.RESEARCHER_GATE.getValue())).thenReturn(RESEARCHER_GATE);
        Document dar = getDocument(null, "845246551313515", null);
        List<ResearcherProperty> properties = databaseDataAccessRequestAPI.updateResearcherIdentification(dar);
        verify(researcherPropertyDAO, times(1)).insertAll(anyObject());
        Assert.assertTrue(properties.size() == 1);
        Assert.assertTrue(properties.get(0).getPropertyKey().equals(ResearcherFields.ORCID.getValue()));
        Assert.assertTrue(properties.get(0).getPropertyValue().equals("845246551313515"));
    }

    @Test
    public void testCreateDARDocument() throws Exception {
        Document dar = getDocument(null, "845246551313515", null);
        byte[] doc = databaseDataAccessRequestAPI.createDARDocument(dar, getResearcherProperties(), new DACUser(), new UserRole(), true, TRANSLATED_USE_RESTRICTION);
        Assert.assertNotNull(doc);
    }

    private Document getDocument(String linkedIn, String orcid, String researcherGate){
        Document dar = new Document();
        dar.put(DarConstants.USER_ID, USER_ID);
        dar.put(ResearcherFields.LINKEDIN_PROFILE.getValue(), linkedIn);
        dar.put(ResearcherFields.ORCID.getValue(), orcid);
        dar.put(ResearcherFields.RESEARCHER_GATE.getValue(), researcherGate);
        dar.put(DarConstants.INVESTIGATOR, randomString(10));
        dar.put(DarConstants.PI_EMAIL, randomString(10));
        dar.put(DarConstants.PROJECT_TITLE, randomString(10));
        dar.put(DarConstants.DATASET_DETAIL, new ArrayList<Document>());
        dar.put(DarConstants.RUS, randomString(10));
        dar.put(DarConstants.NON_TECH_RUS, randomString(10));
        dar.put(DarConstants.METHODS, true);
        dar.put(DarConstants.CONTROLS, true);
        dar.put(DarConstants.OTHER, true);
        dar.put(DarConstants.POA, true);
        dar.put(DarConstants.HMB, true);
        dar.put(DarConstants.OTHER_TEXT, randomString(10));
        return dar;
    }

    private Map<String, String> getResearcherProperties() {
        Map<String, String> researcherProperties = new HashMap<>();
        researcherProperties.put(ResearcherFields.INSTITUTION.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.DEPARTMENT.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.STREET_ADDRESS_1.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.CITY.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.COUNTRY.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.STATE.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.STREET_ADDRESS_2.getValue(), randomString(10));
        researcherProperties.put(ResearcherFields.DIVISION.getValue(), randomString(10));
        researcherProperties.put(DarConstants.ACADEMIC_BUSINESS_EMAIL, randomString(10));
        researcherProperties.put(DarConstants.ERA_COMMONS_ID, randomString(10));
        researcherProperties.put(DarConstants.PUBMED_ID, randomString(10));
        researcherProperties.put(DarConstants.SCIENTIFIC_URL, randomString(10));
        researcherProperties.put(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "true");
        researcherProperties.put(ResearcherFields.PROFILE_NAME.getValue(), randomString(10));
        return researcherProperties;
    }

    private String randomString(int count) {
        return RandomStringUtils.random(count, true, false);
    }

}

