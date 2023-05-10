package org.broadinstitute.consent.http.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.DatasetAssociationDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAssociation;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

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

    @BeforeEach
    public void setUp() {
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
    public void testGetAndVerifyUsersInvalidUsersList() {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(member, chairperson)));
        try {
            service.createDatasetUsersAssociation(1, Arrays.asList(1, 2, 3, 4));
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof BadRequestException);
            Assertions.assertEquals("Invalid UserId list.", e.getMessage());
        }
    }

    @Test
    public void testCreateDatasetUsersAssociation() {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDatasetById(1)).thenReturn(ds1);
        when(dsAssociationDAO.getDatasetAssociation(1)).thenReturn(Arrays.asList(dsAssociation1, dsAssociation2));
        service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
    }

    @Test
    public void testCreateDatasetUsersAssociationNotFoundException() {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDatasetById(1)).thenReturn(null);
        try {
            service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
            Assertions.assertEquals("Invalid DatasetId", e.getMessage());
        }
    }

    @Test
    public void testCreateDatasetUsersAssociationBadRequestException() {
        when(userDAO.findUsersWithRoles(notNull())).thenReturn(new HashSet<>(Arrays.asList(dataOwner1, dataOwner2)));
        when(dsDAO.findDatasetById(1)).thenReturn(ds1);

        doAnswer(invocationOnMock -> {
            throw new BatchUpdateException();
        }).when(dsAssociationDAO).insertDatasetUserAssociation(any());
        try {
            service.createDatasetUsersAssociation(1, Arrays.asList(1, 2));
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof BatchUpdateException);
        }
    }


    /**
     * Private data methods
     **/
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

    private List<UserRole> chairpersonList() {
        return List.of(getChairpersonRole());
    }

    private List<UserRole> memberList() {
        return List.of(getMemberRole());
    }

    private List<UserRole> dataownerList() {
        return List.of(getDataOwnerRole());
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