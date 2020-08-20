package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataAccessRequestServiceTest {

    @Mock
    private ConsentDAO consentDAO;
    @Mock
    private CounterService counterService;
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
        DAOContainer container = new DAOContainer();
        container.setConsentDAO(consentDAO);
        container.setDataAccessRequestDAO(dataAccessRequestDAO);
        container.setDacDAO(dacDAO);
        container.setUserDAO(userDAO);
        container.setDatasetDAO(dataSetDAO);
        container.setElectionDAO(electionDAO);
        container.setVoteDAO(voteDAO);
        service = new DataAccessRequestService(counterService, container, dacService, userService);
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

    @Test
    public void testCreateDataAccessRequest() {
        DataAccessRequest dar = generateDataAccessRequest();
        dar.getData().setDatasetIds(Arrays.asList(1, 2, 3));
        User user = new User(1, "email@test.org", "Display Name", new Date());
        when(counterService.getNextDarSequence()).thenReturn(1);
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(null);
        doNothing().when(dataAccessRequestDAO).updateDraftByReferenceId(any(), any());
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceIdVersion2(any(), any(), any(), any(), any(), any(), any());
        doNothing().when(dataAccessRequestDAO).insertVersion2(any(), any(), any(), any(), any(), any(), any());
        initService();
        List<DataAccessRequest> newDars = service.createDataAccessRequest(user, dar);
        assertEquals(3, newDars.size());
    }

    @Test
    public void testUpdateByReferenceIdVersion2() {
        DataAccessRequest dar = generateDataAccessRequest();
        User user = new User(1, "email@test.org", "Display Name", new Date());
        dar.getData().setDatasetIds(Arrays.asList(1, 2, 3));
        doNothing().when(dataAccessRequestDAO).updateDataByReferenceIdVersion2(any(), any(), any(),
            any(), any(), any(), any());
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(dar);
        initService();
        DataAccessRequest newDar = service.updateByReferenceIdVersion2(user, dar);
        assertNotNull(newDar);
    }

    @Test
    public void testInsertDraftDataAccessRequest() {
        User user = new User();
        user.setDacUserId(1);
        DataAccessRequest draft = generateDataAccessRequest();
        doNothing()
            .when(dataAccessRequestDAO)
            .insertVersion2(any(), any(), any(), any(), any(), any(), any());
        doNothing()
            .when(dataAccessRequestDAO)
            .updateDraftByReferenceId(any(), any());
        when(dataAccessRequestDAO.findByReferenceId(any())).thenReturn(draft);
        initService();
        DataAccessRequest dar = service.insertDraftDataAccessRequest(user, draft);
        assertNotNull(dar);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertDraftDataAccessRequestFailure() {
        initService();
        DataAccessRequest dar = service.insertDraftDataAccessRequest(null, null);
        assertNotNull(dar);
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
