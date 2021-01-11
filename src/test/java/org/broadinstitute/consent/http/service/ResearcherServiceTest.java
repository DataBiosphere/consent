package org.broadinstitute.consent.http.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GoogleUser;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.broadinstitute.consent.http.models.User;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({
        AbstractDACUserAPI.class
})
public class ResearcherServiceTest {

    @Mock
    private ResearcherPropertyDAO researcherPropertyDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private EmailNotifierService emailNotifierService;

    @Mock
    private DACUserAPI dacUserAPI;

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
        user.setEmail(authUser.getName());
        user.setDacUserId(RandomUtils.nextInt(1, 10));
        user.setDisplayName(RandomStringUtils.random(10));

        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
    }

    private void initService() {
        service = new ResearcherPropertyHandler(researcherPropertyDAO, userDAO, emailNotifierService);
    }

    @Test
    public void testSetProperties() {
        ResearcherProperty prop = new ResearcherProperty(
                user.getDacUserId(),
                ResearcherFields.INSTITUTION.getValue(),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(Collections.singletonList(prop));
        initService();

        List<ResearcherProperty> props = service.setProperties(propMap, authUser);
        Assert.assertFalse(props.isEmpty());
        Assert.assertEquals(propMap.size(), props.size());
    }

    @Test(expected = NotFoundException.class)
    public void testSetPropertiesNotFound() {
        when(userDAO.findUserByEmail(any())).thenThrow(new NotFoundException("User Not Found"));
        initService();

        service.setProperties(new HashMap<>(), authUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPropertiesIllegalArgument() {
        Map<String, String> propMap = new HashMap<>();
        propMap.put(RandomStringUtils.random(10, true, false), RandomStringUtils.random(10, true, false));
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        initService();

        service.setProperties(propMap, authUser);
    }

    @Test
    public void testUpdatePropertiesWithValidation() {
        List<ResearcherProperty> props = new ArrayList<>();
        Map<String, String> propMap = new HashMap<>();
        for (ResearcherFields researcherField : ResearcherFields.values()) {
            if (researcherField.getRequired()) {
                String val = RandomStringUtils.random(10, true, false);
                props.add(new ResearcherProperty(user.getDacUserId(), researcherField.getValue(), val));
                propMap.put(researcherField.getValue(), val);
            }
        }
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(props);
        initService();

        List<ResearcherProperty> foundProps = service.updateProperties(propMap, authUser, true);
        Assert.assertFalse(foundProps.isEmpty());
        Assert.assertEquals(props.size(), foundProps.size());
    }

    @Test
    public void testUpdatePropertiesNoValidation() {
        ResearcherProperty prop = new ResearcherProperty(
                user.getDacUserId(),
                ResearcherFields.INSTITUTION.getValue(),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(Collections.singletonList(prop));
        initService();

        List<ResearcherProperty> props = service.updateProperties(propMap, authUser, false);
        Assert.assertFalse(props.isEmpty());
        Assert.assertEquals(propMap.size(), props.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePropertiesMissingFields() {
        ResearcherProperty prop = new ResearcherProperty(
                user.getDacUserId(),
                ResearcherFields.INSTITUTION.getValue(),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        initService();

        service.updateProperties(propMap, authUser, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePropertiesInvalidFields() {
        ResearcherProperty prop = new ResearcherProperty(
                user.getDacUserId(),
                RandomStringUtils.random(10, true, false),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        initService();

        service.updateProperties(propMap, authUser, true);
    }

    @Test
    public void testUpdatePropertiesIncompleteProfile() throws Exception {
        List<ResearcherProperty> props = new ArrayList<>();
        Map<String, String> propMap = new HashMap<>();
        for (ResearcherFields researcherField : ResearcherFields.values()) {
            if (researcherField.getRequired()) {
                String val1 = RandomStringUtils.random(10, true, false);
                String val2 = RandomStringUtils.random(10, true, false);
                props.add(new ResearcherProperty(user.getDacUserId(), researcherField.getValue(), val1));
                propMap.put(researcherField.getValue(), val2);
            }
        }
        props.add(new ResearcherProperty(user.getDacUserId(), ResearcherFields.COMPLETED.getValue(), Boolean.FALSE.toString()));
        propMap.put(ResearcherFields.COMPLETED.getValue(), Boolean.FALSE.toString());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(props);
        when(researcherPropertyDAO.isProfileCompleted(any())).thenReturn(Boolean.FALSE.toString());
        doNothing().when(researcherPropertyDAO).deletePropertiesByUserAndKey(any());
        doNothing().when(researcherPropertyDAO).insertAll(any());
        doNothing().when(researcherPropertyDAO).deleteAllPropertiesByUser(any());
        when(dacUserAPI.updateUserStatus(any(), any())).thenReturn(user);
        doNothing().when(emailNotifierService).sendNewResearcherCreatedMessage(any(), any());
        initService();

        List<ResearcherProperty> foundProps = service.updateProperties(propMap, authUser, true);
        Assert.assertFalse(foundProps.isEmpty());
        Assert.assertEquals(props.size(), foundProps.size());
        verifyZeroInteractions(emailNotifierService);
    }

    @Test
    public void testUpdatePropertiesCompleteProfile() throws Exception {
        List<ResearcherProperty> props = new ArrayList<>();
        Map<String, String> propMap = new HashMap<>();
        for (ResearcherFields researcherField : ResearcherFields.values()) {
            String val1 = RandomStringUtils.random(10, true, false);
            String val2 = RandomStringUtils.random(10, true, false);
            props.add(new ResearcherProperty(user.getDacUserId(), researcherField.getValue(), val1));
            propMap.put(researcherField.getValue(), val2);
        }
        props.add(new ResearcherProperty(user.getDacUserId(), ResearcherFields.COMPLETED.getValue(), Boolean.TRUE.toString()));
        propMap.put(ResearcherFields.COMPLETED.getValue(), Boolean.TRUE.toString());
        when(userDAO.findUserByEmail(any())).thenReturn(user);
        when(userDAO.findUserById(any())).thenReturn(user);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(props);
        when(researcherPropertyDAO.isProfileCompleted(any())).thenReturn(Boolean.TRUE.toString());
        doNothing().when(researcherPropertyDAO).deletePropertiesByUserAndKey(any());
        doNothing().when(researcherPropertyDAO).insertAll(any());
        doNothing().when(researcherPropertyDAO).deleteAllPropertiesByUser(any());
        when(dacUserAPI.updateUserStatus(any(), any())).thenReturn(user);
        doNothing().when(emailNotifierService).sendNewResearcherCreatedMessage(any(), any());
        initService();

        List<ResearcherProperty> foundProps = service.updateProperties(propMap, authUser, true);
        Assert.assertFalse(foundProps.isEmpty());
        Assert.assertEquals(props.size(), foundProps.size());
        verify(emailNotifierService, never()).sendNewResearcherCreatedMessage(any(), any());
    }

}
