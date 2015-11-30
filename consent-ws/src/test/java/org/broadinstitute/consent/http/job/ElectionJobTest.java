package org.broadinstitute.consent.http.job;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class ElectionJobTest {

    @Mock
    private ElectionAPI electionAPI;

    ElectionJob electionJob;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        electionJob = new ElectionJob();
        electionJob.setElectionAPI(electionAPI);
    }

    @Test
    public void testVerifyCloseElectionsEmpty() throws Exception {
        List<Election> elections = new ArrayList<>();
        when(electionAPI.findExpiredElections(ElectionType.DATA_SET.getValue())).thenReturn(elections);
        electionJob.doJob();
        verify(electionAPI, never()).closeDataOwnerApprovalElection(Mockito.anyInt());
    }

    @Test
    public void testVerifyCloseElectionsNotEmpty() throws Exception {
        List<Election> elections = new ArrayList<>();
        Election election = new Election();
        elections.add(election);
        when(electionAPI.findExpiredElections(ElectionType.DATA_SET.getValue())).thenReturn(elections);
        electionJob.doJob();
        verify(electionAPI,times(1)).closeDataOwnerApprovalElection(Mockito.anyInt());
    }

    @Test
    public void testVerifyCloseMultipleElections() throws Exception {
        List<Election> elections = new ArrayList<>();
        Election election = new Election();
        Election election2 = new Election();
        elections.add(election);
        elections.add(election2);
        when(electionAPI.findExpiredElections(ElectionType.DATA_SET.getValue())).thenReturn(elections);
        electionJob.doJob();
        verify(electionAPI,times(2)).closeDataOwnerApprovalElection(Mockito.anyInt());
    }
}