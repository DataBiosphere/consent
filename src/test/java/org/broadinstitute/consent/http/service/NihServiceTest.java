package org.broadinstitute.consent.http.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.service.dao.NihServiceDAO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @BeforeEach
    public void setUp() throws Exception {
        nihUserAccount = new NIHUserAccount("nih username", new ArrayList(), new Date().toString(), true);
        authUser = new AuthUser("test@test.com");
        MockitoAnnotations.openMocks(this);
    }

    private void initService() {
        service = new NihService(researcherService, userDAO, nihServiceDAO);
    }

    @Test
    public void testAuthenticateNih_InvalidUser() {
        initService();
        try {
            service.authenticateNih(nihUserAccount, new AuthUser("test@test.com"), 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof NotFoundException);
        }
    }

    @Test
    public void testAuthenticateNih() {
        List<UserProperty> props = Collections.singletonList(new UserProperty(1, 1, "test", "value"));
        when(researcherService.describeUserProperties(any())).thenReturn(props);
        User user = new User();
        user.setUserId(1);
        when(userDAO.findUserById(any())).thenReturn(user);
        spy(nihServiceDAO);
        initService();
        try {
            List<UserProperty> properties = service.authenticateNih(nihUserAccount, authUser, user.getUserId());
            Assertions.assertEquals(1, properties.size());
            Assertions.assertEquals(Integer.valueOf(1), properties.get(0).getPropertyId());
            verify(nihServiceDAO, times(1)).updateUserNihStatus(user, nihUserAccount);
        } catch (BadRequestException bre) {
            assert false;
        }
    }

    @Test
    public void testAuthenticateNih_BadRequest() {
        User user = new User();
        user.setUserId(1);
        when(userDAO.findUserById(any())).thenReturn(user);
        nihUserAccount.setNihUsername("");
        initService();
        try {
            service.authenticateNih(nihUserAccount, authUser, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testAuthenticateNih_BadRequestNullAccount() {
        initService();
        try {
            service.authenticateNih(null, authUser, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testAuthenticateNih_BadRequestNullAccountExpiration() {
        NIHUserAccount account = new NIHUserAccount();
        account.setStatus(true);
        initService();
        try {
            service.authenticateNih(account, authUser, 1);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof BadRequestException);
        }
    }

    @Test
    public void testDeleteNihAccountById() {
        doNothing().when(researcherService).deleteResearcherProperties(any());
        initService();
        service.deleteNihAccountById(1);
    }
}