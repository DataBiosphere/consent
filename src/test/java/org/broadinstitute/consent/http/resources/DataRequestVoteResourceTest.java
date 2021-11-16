package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.Before;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.openMocks;

public class DataRequestVoteResourceTest {
    @Mock
    private EmailNotifierService emailNotifierService;
    @Mock
    private UserService userService;
    @Mock
    private DatasetService datasetService;
    @Mock
    private DataAccessRequestService dataAccessRequestService;
    @Mock
    private DatasetAssociationService datasetAssociationService;
    @Mock
    private ElectionService electionService;
    @Mock
    private VoteService voteService;

    private DataRequestVoteResource resource;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initResource() {
        resource = new DataRequestVoteResource(
                dataAccessRequestService, datasetAssociationService,
                emailNotifierService, voteService,
                datasetService, electionService, userService
        );
    }

    // This test will be written in a future PR.
}
