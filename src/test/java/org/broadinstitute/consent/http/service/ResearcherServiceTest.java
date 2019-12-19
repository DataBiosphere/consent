package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.users.handler.ResearcherPropertyHandler;
import org.broadinstitute.consent.http.service.users.handler.ResearcherService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractEmailNotifierAPI.class,
        AbstractDACUserAPI.class
})
public class ResearcherServiceTest {

    @Mock
    private ResearcherPropertyDAO researcherPropertyDAO;

    @Mock
    private DACUserDAO dacUserDAO;

    @Mock
    private EmailNotifierAPI emailApi;

    @Mock
    private DACUserAPI dacUserAPI;

    private ResearcherService service;

    private AuthUser authUser;

    private DACUser dacUser;

    @Before
    public void setUp() {
        GoogleUser googleUser = new GoogleUser();
        googleUser.setName("Test User");
        googleUser.setEmail("test@gmail.com");
        authUser = new AuthUser(googleUser);
        dacUser = new DACUser();
        dacUser.setEmail(authUser.getName());
        dacUser.setDacUserId(RandomUtils.nextInt(1, 10));
        dacUser.setDisplayName(RandomStringUtils.random(10));

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractEmailNotifierAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        when(AbstractEmailNotifierAPI.getInstance()).thenReturn(emailApi);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
    }

    private void initService() {
        service = new ResearcherPropertyHandler(researcherPropertyDAO, dacUserDAO, emailApi);
    }

    @Test
    public void testSetProperties() {
        ResearcherProperty prop = new ResearcherProperty(
                dacUser.getDacUserId(),
                ResearcherFields.INSTITUTION.getValue(),
                RandomStringUtils.random(10));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(Collections.singletonList(prop));
        initService();

        List<ResearcherProperty> props = service.setProperties(propMap, authUser);
        Assert.assertFalse(props.isEmpty());
        Assert.assertEquals(propMap.size(), props.size());
    }

    @Test(expected = NotFoundException.class)
    public void testSetPropertiesNotFound() {
        when(dacUserDAO.findDACUserByEmail(any())).thenThrow(new NotFoundException("User Not Found"));
        initService();

        service.setProperties(new HashMap<>(), authUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPropertiesIllegalArgument() {
        Map<String, String> propMap = new HashMap<>();
        propMap.put(RandomStringUtils.random(10), RandomStringUtils.random(10));
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        initService();

        service.setProperties(propMap, authUser);
    }

}
