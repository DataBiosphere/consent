package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class NihServiceTest {

    @Mock
    private ResearcherService researcherService;

    @Spy
    private LibraryCardDAO libraryCardDAO;

    @Spy
    private UserDAO userDAO;

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
        service = new NihService(researcherService, libraryCardDAO, userDAO);
    }

    @Test
    public void testAuthenticateNih_InvalidUser() {
        initService();
        try {
            service.authenticateNih(new NIHUserAccount(), new AuthUser("test@test.com"), 1);
            assert false;
        } catch (BadRequestException bre) {
            assert true;
        }
    }

    @Test
    public void testAuthenticateNih() {
        List<UserProperty> props = Collections.singletonList(new UserProperty(1, 1, "test", "value"));
        when(researcherService.updateProperties(any(), any(),any())).thenReturn(props);
        initService();
        try {
            List<UserProperty> properties = service.authenticateNih(nihUserAccount, authUser, 1);
            assertEquals(1, properties.size());
            assertEquals(Integer.valueOf(1), properties.get(0).getPropertyId());
            Mockito.verify(libraryCardDAO, times(1)).updateEraCommonsForUser(any(), any());
            Mockito.verify(userDAO, times(1)).updateEraCommonsId(any(), any());
        } catch (BadRequestException bre) {
            assert false;
        }
    }

    @Test (expected = BadRequestException.class)
    public void testAuthenticateNih_BadRequest() {
        nihUserAccount.setNihUsername("");
        initService();
        service.authenticateNih(nihUserAccount, authUser, 1);
    }

    @Test
    public void testDeleteNihAccountById() {
        doNothing().when(researcherService).deleteResearcherProperties(any());
        initService();
        service.deleteNihAccountById(1);
    }
}