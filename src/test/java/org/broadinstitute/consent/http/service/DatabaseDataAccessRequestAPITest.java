package org.broadinstitute.consent.http.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DatabaseDataAccessRequestAPITest {

    @Mock
    private UseRestrictionConverter converter;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private UserPropertyDAO userPropertyDAO;
    @Mock
    private VoteDAO voteDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private DataSetDAO dataSetDAO;
    @Mock
    private DataAccessRequestService dataAccessRequestService;

    private final Integer USER_ID = 3333;

    private final String LINKEDIN_PROFILE = "https://www.linkedin.com/in/veronicatest/";

    private final String ORCID = "1658474585212365";

    private final String RESEARCHER_GATE = "researcher_gate";

    private final String TRANSLATED_USE_RESTRICTION = "Translated use restriction.";

    private DatabaseDataAccessRequestAPI databaseDataAccessRequestAPI;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        databaseDataAccessRequestAPI = new DatabaseDataAccessRequestAPI(dataAccessRequestService, converter, electionDAO, consentDAO, voteDAO, userDAO, dataSetDAO,
            userPropertyDAO);
    }

    @Test
    public void testCreateDARDocument() throws Exception {
        Document dar = getDocument(null, "845246551313515", null);
        byte[] doc = databaseDataAccessRequestAPI.createDARDocument(dar, getResearcherProperties(), new User(), true, TRANSLATED_USE_RESTRICTION);
        Assert.assertNotNull(doc);
    }

    private Document getDocument(String linkedIn, String orcid, String researcherGate) {
        Document dar = new Document();
        dar.put(UserFields.LINKEDIN_PROFILE.getValue(), linkedIn);
        dar.put(UserFields.ORCID.getValue(), orcid);
        dar.put(UserFields.RESEARCHER_GATE.getValue(), researcherGate);
        dar.put(DarConstants.INVESTIGATOR, randomString());
        dar.put(DarConstants.PI_EMAIL, randomString());
        dar.put(DarConstants.PROJECT_TITLE, randomString());
        dar.put(DarConstants.DATASET_DETAIL, new ArrayList<Document>());
        dar.put(DarConstants.RUS, randomString());
        dar.put(DarConstants.NON_TECH_RUS, randomString());
        dar.put(DarConstants.METHODS, true);
        dar.put(DarConstants.CONTROLS, true);
        dar.put(DarConstants.OTHER, true);
        dar.put(DarConstants.POA, true);
        dar.put(DarConstants.HMB, true);
        dar.put(DarConstants.OTHER_TEXT, randomString());
        return dar;
    }

    private Map<String, String> getResearcherProperties() {
        Map<String, String> researcherProperties = new HashMap<>();
        researcherProperties.put(UserFields.INSTITUTION.getValue(), randomString());
        researcherProperties.put(UserFields.DEPARTMENT.getValue(), randomString());
        researcherProperties.put(UserFields.STREET_ADDRESS_1.getValue(), randomString());
        researcherProperties.put(UserFields.CITY.getValue(), randomString());
        researcherProperties.put(UserFields.ZIP_POSTAL_CODE.getValue(), randomString());
        researcherProperties.put(UserFields.COUNTRY.getValue(), randomString());
        researcherProperties.put(UserFields.STATE.getValue(), randomString());
        researcherProperties.put(UserFields.STREET_ADDRESS_2.getValue(), randomString());
        researcherProperties.put(UserFields.DIVISION.getValue(), randomString());
        researcherProperties.put(DarConstants.ACADEMIC_BUSINESS_EMAIL, randomString());
        researcherProperties.put(DarConstants.ERA_COMMONS_ID, randomString());
        researcherProperties.put(DarConstants.PUBMED_ID, randomString());
        researcherProperties.put(DarConstants.SCIENTIFIC_URL, randomString());
        researcherProperties.put(UserFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "true");
        researcherProperties.put(UserFields.PROFILE_NAME.getValue(), randomString());
        return researcherProperties;
    }

    private String randomString() {
        return RandomStringUtils.random(10, true, false);
    }

}

