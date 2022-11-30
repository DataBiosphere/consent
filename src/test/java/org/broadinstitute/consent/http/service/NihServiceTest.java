package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class NihServiceTest {

    @Mock
    private ResearcherService researcherService;

    @Mock
    private UserDAO userDAO;

    @Mock
    private NihServiceDAO nihServiceDAO;

    private NihService service;
    private NIHUserAccount nihUserAccount;
    private AuthUser authUser;

    @Before
    public void setUp() throws Exception {
        nihUserAccount = new NIHUserAccount("nih username", new ArrayList(), new Date().toString(), true);
        authUser = new AuthUser("test@test.com");
        MockitoAnnotations.openMocks(this);
    }

    private void initService() {
        service = new NihService(researcherService, userDAO, nihServiceDAO);
    }

    @Test (expected = NotFoundException.class)
    public void testAuthenticateNih_InvalidUser() {
        initService();
        service.authenticateNih(new NIHUserAccount(), new AuthUser("test@test.com"), 1);
    }

    @Test
    public void testAuthenticateNih() {
        List<UserProperty> props = Collections.singletonList(new UserProperty(1, 1, "test", "value"));
        when(researcherService.describeUserProperties(any())).thenReturn(props);
        User user = new User();
        user.setUserId(1);
        when(userDAO.findUserById(any())).thenReturn(user);
        initService();
        try {
            List<UserProperty> properties = service.authenticateNih(nihUserAccount, authUser, 1);
            assertEquals(1, properties.size());
            assertEquals(Integer.valueOf(1), properties.get(0).getPropertyId());
        } catch (BadRequestException bre) {
            assert false;
        }
    }

    @Test (expected = BadRequestException.class)
    public void testAuthenticateNih_BadRequest() {
        User user = new User();
        user.setUserId(1);
        when(userDAO.findUserById(any())).thenReturn(user);
        nihUserAccount.setNihUsername("");
        initService();
        service.authenticateNih(nihUserAccount, authUser, 1);
    }

    @Test (expected = BadRequestException.class)
    public void testAuthenticateNih_BadRequestNullAccount() {
        initService();
        service.authenticateNih(null, authUser, 1);
    }

    @Test
    public void testDeleteNihAccountById() {
        doNothing().when(researcherService).deleteResearcherProperties(any());
        initService();
        service.deleteNihAccountById(1);
    }
}