package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ResearcherServiceTest {

    @Mock
    private UserPropertyDAO userPropertyDAO;

    @Mock
    private UserDAO userDAO;

    private ResearcherService service;

    private AuthUser authUser;

    private User user;

    @Before
    public void setUp() {
        GoogleUser googleUser = new GoogleUser();
        googleUser.setName("Test User");
        googleUser.setEmail("test@gmail.com");
        authUser = new AuthUser(googleUser);
        user = new User();
        user.setEmail(authUser.getEmail());
        user.setUserId(RandomUtils.nextInt(1, 10));
        user.setDisplayName(RandomStringUtils.randomAlphabetic(10));

        openMocks(this);
    }

    private void initService() {
        service = new ResearcherService(userPropertyDAO, userDAO);
    }

    @Test
    public void testSetProperties() {
        UserProperty prop = new UserProperty(
                user.getUserId(),
                UserFields.SUGGESTED_INSTITUTION.getValue(),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(userPropertyDAO.findResearcherPropertiesByUser(any(), any())).thenReturn(Collections.singletonList(prop));
        initService();

        List<UserProperty> props = service.setProperties(propMap, authUser);
        Assert.assertFalse(props.isEmpty());
        Assert.assertEquals(propMap.size(), props.size());
    }

    @Test(expected = NotFoundException.class)
    public void testSetPropertiesNotFound() {
        when(userDAO.findUserByEmail(any())).thenThrow(new NotFoundException("User Not Found"));
        initService();

        service.setProperties(new HashMap<>(), authUser);
    }

}
