package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.when;

public class DatabaseDataSetAssociationAPITest {

    @Mock
    private DataSetAssociationDAO dsAssociationDAO;
    @Mock
    private DACUserDAO dacUserDAO;
    @Mock
    private DataSetDAO dsDAO;

    DatabaseDataSetAssociationAPI associationAPI;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        associationAPI = new DatabaseDataSetAssociationAPI(dsDAO, dsAssociationDAO, dacUserDAO);
    }

    @Test
    public void testGetAndVerifyUsersUserNotDataOwner() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(member, chairperson)));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("User with id 1 is not a DATA_OWNER");
        associationAPI.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test
    public void testGetAndVerifyUsersInvalidUsersList() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(member, chairperson)));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid UserId list.");
        associationAPI.createDatasetUsersAssociation(1, Arrays.asList(1, 2, 3, 4));
    }

    @Test
    public void testCreateDatasetUsersAssociation() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDataSetById(1)).thenReturn(ds1);
        when(dsAssociationDAO.getDatasetAssociation(1)).thenReturn(Arrays.asList(dsAssociation1, dsAssociation2));
        associationAPI.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test
    public void testCreateDatasetUsersAssociationNotFoundException() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDataSetById(1)).thenReturn(null);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Invalid DatasetId");
        associationAPI.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test(expected = BatchUpdateException.class)
    public void testCreateDatasetUsersAssociationBadRequestException() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDataSetById(1)).thenReturn(ds1);

        Mockito.doThrow(BatchUpdateException.class).when(dsAssociationDAO).insertDatasetUserAssociation(anyObject());
        associationAPI.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }


    /** Private data methods **/
    private static final String CHAIRPERSON = "Chairperson";
    private static final String DACMEMBER = "Member";
    private static final String DATAOWNER = "DataOwner";

    DatasetAssociation dsAssociation1 = new DatasetAssociation(1, 3);
    DatasetAssociation dsAssociation2 = new DatasetAssociation(1, 4);

    DataSet ds1 = new DataSet(1, "DS-001", "DS-001", new Date(), true);
    DataSet ds2 = new DataSet(2, "DS-002", "DS-002", new Date(), true);

    DACUser chairperson = new DACUser(1, "originalchair@broad.com", "Original Chairperson", new Date(), chairpersonList(), null);
    DACUser member = new DACUser(2, "originalchair@broad.com", "Original Chairperson", new Date(), memberList(), null);
    DACUser dataOwner1 = new DACUser(3, "originalchair@broad.com", "Original Chairperson", new Date(), dataownerList(), null);
    DACUser dataOwner2 = new DACUser(4, "originalchair@broad.com", "Original Chairperson", new Date(), dataownerList(), null);

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