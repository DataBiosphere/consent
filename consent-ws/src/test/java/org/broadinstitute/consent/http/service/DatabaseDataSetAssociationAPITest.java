package org.broadinstitute.consent.http.service;

import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.DACUserRole;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

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
        associationAPI.createDatasetUsersAssociation("DS-001", Arrays.asList(1, 2));
    }

    @Test
    public void testGetAndVerifyUsersInvalidUsersList() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(member, chairperson)));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid UserId list.");
        associationAPI.createDatasetUsersAssociation("DS-001", Arrays.asList(1, 2, 3, 4));
    }

    @Test
    public void testCreateDatasetUsersAssociation() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDataSetByObjectId("DS-001")).thenReturn(ds1);
        when(dsAssociationDAO.getDatasetAssociation(1)).thenReturn(Arrays.asList(dsAssociation1, dsAssociation2));
        associationAPI.createDatasetUsersAssociation("DS-001", Arrays.asList(1, 2));
    }

    @Test
    public void testCreateDatasetUsersAssociationNotFoundException() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDataSetByObjectId("DS-001")).thenReturn(null);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("Invalid DatasetId");
        associationAPI.createDatasetUsersAssociation("DS-001", Arrays.asList(1, 2));
    }

    @Test(expected = BatchUpdateException.class)
    public void testCreateDatasetUsersAssociationBadRequestException() throws Exception {
        when(dacUserDAO.findUsersWithRoles(anyObject())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDataSetByObjectId("DS-001")).thenReturn(ds1);

        Mockito.doThrow(BatchUpdateException.class).when(dsAssociationDAO).insertDatasetUserAssociation(anyObject());
        associationAPI.createDatasetUsersAssociation("DS-001", Arrays.asList(1, 2));
    }


    /** Private data methods **/
    private static final String CHAIRPERSON = "Chairperson";
    private static final String DACMEMBER = "Member";
    private static final String DATAOWNER = "DataOwner";

    DatasetAssociation dsAssociation1 = new DatasetAssociation(1, 3);
    DatasetAssociation dsAssociation2 = new DatasetAssociation(1, 4);

    DataSet ds1 = new DataSet(1, "DS-001", "DS-001", new Date(), true);
    DataSet ds2 = new DataSet(2, "DS-002", "DS-002", new Date(), true);

    DACUser chairperson = new DACUser(1, "originalchair@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), chairpersonList());
    DACUser member = new DACUser(2, "originalchair@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), memberList());
    DACUser dataOwner1 = new DACUser(3, "originalchair@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), dataownerList());
    DACUser dataOwner2 = new DACUser(4, "originalchair@broad.com", "Original Chairperson", RoleStatus.PENDING.toString(), new Date(), dataownerList());

    private List<DACUserRole> chairpersonList(){
        return Arrays.asList(getChairpersonRole());
    }

    private List<DACUserRole> memberList(){
        return Arrays.asList(getMemberRole());
    }

    private List<DACUserRole> dataownerList(){
        return Arrays.asList(getDataOwnerRole());
    }

    private DACUserRole getMemberRole() {
        return new DACUserRole(1, DACMEMBER);
    }

    private DACUserRole getChairpersonRole() {
        return new DACUserRole(2, CHAIRPERSON);
    }

    private DACUserRole getDataOwnerRole() {
        return new DACUserRole(6, DATAOWNER);
    }

}