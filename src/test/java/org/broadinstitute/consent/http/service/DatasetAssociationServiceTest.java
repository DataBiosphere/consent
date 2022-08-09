package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DatasetAssociationServiceTest {

    @Mock
    private DatasetAssociationDAO dsAssociationDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private DatasetDAO dsDAO;
    @Mock
    private UserRoleDAO userRoleDAO;

    private DatasetAssociationService service;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp(){
        openMocks(this);
        service = new DatasetAssociationService(dsAssociationDAO, userDAO, dsDAO, userRoleDAO);
    }

    @Test
    public void testGetAndVerifyUsersUserNotDataOwner() {
        when(dsDAO.findDatasetById(any())).thenReturn(ds1);
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(member, chairperson)));
        doNothing().when(userRoleDAO).insertSingleUserRole(any(), any());
        service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test
    public void testGetAndVerifyUsersInvalidUsersList() throws Exception {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(member, chairperson)));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid UserId list.");
        service.createDatasetUsersAssociation(1, Arrays.asList(1, 2, 3, 4));
    }

    @Test
    public void testCreateDatasetUsersAssociation() throws Exception {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDatasetById(1)).thenReturn(ds1);
        when(dsAssociationDAO.getDatasetAssociation(1)).thenReturn(Arrays.asList(dsAssociation1, dsAssociation2));
        service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test
    public void testCreateDatasetUsersAssociationNotFoundException() throws Exception {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDatasetById(1)).thenReturn(null);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Invalid DatasetId");
        service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test(expected = BatchUpdateException.class)
    public void testCreateDatasetUsersAssociationBadRequestException() throws Exception {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDatasetById(1)).thenReturn(ds1);

        doAnswer(invocationOnMock -> { throw new BatchUpdateException(); }).when(dsAssociationDAO).insertDatasetUserAssociation(any());
        service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }


    /** Private data methods **/
    private static final String CHAIRPERSON = "Chairperson";
    private static final String DACMEMBER = "Member";
    private static final String DATAOWNER = "DataOwner";

    DatasetAssociation dsAssociation1 = new DatasetAssociation(1, 3);
    DatasetAssociation dsAssociation2 = new DatasetAssociation(1, 4);

    Dataset ds1 = new Dataset(1, "DS-001", "DS-001", new Date(), true);
    Dataset ds2 = new Dataset(2, "DS-002", "DS-002", new Date(), true);

    User chairperson = new User(1, "originalchair@broad.com", "Original Chairperson", new Date(), chairpersonList());
    User member = new User(2, "originalchair@broad.com", "Original Chairperson", new Date(), memberList());
    User dataOwner1 = new User(3, "originalchair@broad.com", "Original Chairperson", new Date(), dataownerList());
    User dataOwner2 = new User(4, "originalchair@broad.com", "Original Chairperson", new Date(), dataownerList());

    private List<UserRole> chairpersonList(){
        return Arrays.asList(getChairpersonRole());
    }

    private List<UserRole> memberList(){
        return Arrays.asList(getMemberRole());
    }

    private List<UserRole> dataownerList(){
        return Arrays.asList(getDataOwnerRole());
    }

    private UserRole getMemberRole() {
        return new UserRole(1, DACMEMBER);
    }

    private UserRole getChairpersonRole() {
        return new UserRole(2, CHAIRPERSON);
    }

    private UserRole getDataOwnerRole() {
        return new UserRole(6, DATAOWNER);
    }

}