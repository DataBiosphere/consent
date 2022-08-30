package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.ConsentService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.notNull;

public class ConsentManageResourceTest {

    @Mock
    ConsentService consentService;

    private ConsentManageResource resource;
    private AuthUser authUser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authUser = new AuthUser("Test@gmail.com");
        resource = new ConsentManageResource(consentService);
    }

    @After
    public void tearDown() {
        Mockito.reset(consentService);
    }

    @Test
    public void testGetTotalUnreviewedConsent() throws Exception {
        Response response = resource.getTotalUnreviewedConsent(authUser);
        Assert.assertEquals(200, response.getStatus());
    }

}
