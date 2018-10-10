package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.List;
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
    @Mock
    private DataAccessReportsParser dataAccessReportsParser;

    private final Integer USER_ID = 3333;

    private final String LINKEDIN_PROFILE = "https://www.linkedin.com/in/veronicatest/";

    private final String ORCID = "1658474585212365";

    private final String RESEARCHER_GATE = "researcher_gate";

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

    private Document getDocument(String linkedIn, String orcid, String researcherGate){
        Document dar = new Document();
        dar.put(DarConstants.USER_ID, USER_ID);
        dar.put(ResearcherFields.LINKEDIN_PROFILE.getValue(), linkedIn);
        dar.put(ResearcherFields.ORCID.getValue(), orcid);
        dar.put(ResearcherFields.RESEARCHER_GATE.getValue(), researcherGate);
        return dar;
    }
}
