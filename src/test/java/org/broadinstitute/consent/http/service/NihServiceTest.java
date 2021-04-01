package org.broadinstitute.consent.http.service;

import junit.framework.TestCase;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class NihServiceTest extends TestCase {

    @Mock
    private ResearcherService researcherService;

    private NihService service;
    private NIHUserAccount nihUserAccount;
    private AuthUser authUser;

    @Before
    public void setUp() throws Exception {
        nihUserAccount = new NIHUserAccount("nih username", new ArrayList(), new Date().toString(), true);
        authUser = new AuthUser("test@test.com");
        MockitoAnnotations.initMocks(this);
    }

    private void initService() {
        service = new NihService(researcherService);
    }

    @Test
    public void testAuthenticateNih_InvalidUser() {
        initService();
        try {
            service.authenticateNih(new NIHUserAccount(), new AuthUser("test@test.com"));
            assert false;
        } catch (BadRequestException bre) {
            assert true;
        }
    }

    @Test
    public void testAuthenticateNih() {
        when(researcherService.updateProperties(any(), any(),any()))
                .thenReturn(Arrays.asList(new UserProperty(1, 1, "test", "value")));
        initService();
        try {
            List<UserProperty> properties = service.authenticateNih(nihUserAccount, authUser);
            assertEquals(1, properties.size());
            assertEquals(Integer.valueOf(1), properties.get(0).getPropertyId());
        } catch (BadRequestException bre) {
            assert false;
        }
    }

    @Test
    public void testDeleteNihAccountById() {
        doNothing().when(researcherService).deleteResearcherProperties(any());
        initService();
        service.deleteNihAccountById(1);
    }
}