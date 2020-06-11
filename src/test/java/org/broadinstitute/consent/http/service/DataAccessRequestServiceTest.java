package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.NotFoundException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class DataAccessRequestServiceTest {

    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private DataAccessRequestDAO dataAccessRequestDAO;
    @Mock
    private DacDAO dacDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private DataSetDAO dataSetDAO;
    @Mock
    private ElectionDAO electionDAO;
    @Mock
    private DacService dacService;
    @Mock
    private UserService userService;
    @Mock
    private VoteDAO voteDAO;

    private DataAccessRequestService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new DataAccessRequestService(consentDAO, dataAccessRequestDAO, dacDAO, userDAO, dataSetDAO,
                electionDAO, dacService, userService, voteDAO);
    }

    @Test
    public void testCancelDataAccessRequest() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceId(any(), any());
        initService();

        DataAccessRequest updated = service.cancelDataAccessRequest(dar.getReferenceId());
        assertNotNull(updated);
        assertNotNull(updated.getData());
        assertNotNull(updated.getData().getStatus());
        assertEquals(ElectionStatus.CANCELED.getValue(), updated.getData().getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void testCancelDataAccessRequestNotFound() {
        DataAccessRequest dar = generateDataAccessRequest();
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(null);
        initService();

        service.cancelDataAccessRequest(dar.getReferenceId());
    }

    private DataAccessRequest generateDataAccessRequest() {
        DataAccessRequest dar = new DataAccessRequest();
        DataAccessRequestData data = new DataAccessRequestData();
        dar.setReferenceId(UUID.randomUUID().toString());
        data.setReferenceId(dar.getReferenceId());
        dar.setData(data);
        return dar;
    }

}
