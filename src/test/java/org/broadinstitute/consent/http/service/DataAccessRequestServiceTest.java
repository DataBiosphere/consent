package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DataAccessRequestServiceTest {

    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private DacDAO dacDAO;
    @Mock
    private DACUserDAO dacUserDAO;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private DataSetDAO dataSetDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private DacService dacService;
    @Mock
    private VoteDAO voteDAO;

    private DataAccessRequestService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new DataAccessRequestService(consentDAO, dataAccessRequestDAO, dacDAO, dacUserDAO, dataSetDAO,
                electionDAO, dacService, voteDAO);
    }

    @Test
    public void testGetInvalidDataAccessRequest_EmptyCase() {
        when(dataAccessRequestDAO.findAll()).thenReturn(Collections.emptyList());
        initService();
        List<UseRestrictionDTO> invalidDars = service.getInvalidDARUseRestrictionDTOs();
        assertTrue(invalidDars.isEmpty());
    }

    @Test
    public void testGetInvalidDataAccessRequest_NoResultsCase() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setValidRestriction(true);
        dar.setData(data);
        when(dataAccessRequestDAO.findAll()).thenReturn(Collections.singletonList(dar));
        initService();
        List<UseRestrictionDTO> invalidDars = service.getInvalidDARUseRestrictionDTOs();
        assertTrue(invalidDars.isEmpty());
    }

    @Test
    public void testGetInvalidDataAccessRequest_ResultsCase() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        data.setValidRestriction(false);
        dar.setData(data);
        when(dataAccessRequestDAO.findAll()).thenReturn(Collections.singletonList(dar));
        initService();
        List<UseRestrictionDTO> invalidDars = service.getInvalidDARUseRestrictionDTOs();
        assertFalse(invalidDars.isEmpty());
        assertEquals(1, invalidDars.size());
    }

}
