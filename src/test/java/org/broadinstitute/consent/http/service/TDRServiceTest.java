package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.tdr.ApprovedUser;
import org.broadinstitute.consent.http.models.tdr.ApprovedUsers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class TDRServiceTest {

    @Mock
    private DataAccessRequestService darService;

    @Mock
    private DatasetDAO datasetDAO;

    private TDRService service;

    @Before
    public void setUp() {
        openMocks(this);
    }

    private void initService() {
        service = new TDRService(darService, datasetDAO);
    }

    @Test
    public void testGetApprovedUsersForDataset() {
        Dataset dataset = new Dataset();

        User user1 = new User();
        user1.setEmail("asdf1@gmail.com");
        User user2 = new User();
        user2.setEmail("asdf2@gmail.com");

        Collection<User> userList = List.of(user1, user2);

        when(darService.getUsersApprovedForDataset(dataset)).thenReturn(userList);
        initService();

        ApprovedUsers approvedUsers = service.getApprovedUsersForDataset(dataset);
        List<String> approvedUsersEmails = approvedUsers.getApprovedUsers().stream()
                .map(ApprovedUser::getEmail)
                .toList();

        assertTrue(approvedUsersEmails.containsAll(List.of(user1.getEmail(), user2.getEmail())));
    }

    @Test
    public void testGetDatasetsByIdentifier() {
        String identifiers = "DUOS-00001, DUOS-00002";
        List<Integer> identifierList = Arrays.stream(identifiers.split(","))
                .map(String::trim)
                .filter(identifier -> !identifier.isBlank())
                .map(Dataset::parseIdentifierToAlias)
                .toList();

        Dataset dataset1 = new Dataset();
        dataset1.setDataSetId(1);
        dataset1.setAlias(00001);

        Dataset dataset2 = new Dataset();
        dataset2.setDataSetId(2);
        dataset2.setAlias(00002);

        when(datasetDAO.findDatasetsByAlias(identifierList)).thenReturn(List.of(dataset1, dataset2));

        initService();
        List<Dataset> datasetIds = service.getDatasetsByIdentifier(identifierList);

        assertEquals(datasetIds.size(), identifierList.size());
        assertTrue(datasetIds.containsAll(List.of(dataset1, dataset2)));
    }
}
