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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
                RandomStringUtils.random(10, true, false));
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
        propMap.put(RandomStringUtils.random(10, true, false), RandomStringUtils.random(10, true, false));
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
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
                props.add(new ResearcherProperty(dacUser.getDacUserId(), researcherField.getValue(), val));
                propMap.put(researcherField.getValue(), val);
            }
        }
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(props);
        initService();

        List<ResearcherProperty> foundProps = service.updateProperties(propMap, authUser, true);
        Assert.assertFalse(foundProps.isEmpty());
        Assert.assertEquals(props.size(), foundProps.size());
    }

    @Test
    public void testUpdatePropertiesNoValidation() {
        ResearcherProperty prop = new ResearcherProperty(
                dacUser.getDacUserId(),
                ResearcherFields.INSTITUTION.getValue(),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(Collections.singletonList(prop));
        initService();

        List<ResearcherProperty> props = service.updateProperties(propMap, authUser, false);
        Assert.assertFalse(props.isEmpty());
        Assert.assertEquals(propMap.size(), props.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePropertiesMissingFields() {
        ResearcherProperty prop = new ResearcherProperty(
                dacUser.getDacUserId(),
                ResearcherFields.INSTITUTION.getValue(),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        initService();

        service.updateProperties(propMap, authUser, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePropertiesInvalidFields() {
        ResearcherProperty prop = new ResearcherProperty(
                dacUser.getDacUserId(),
                RandomStringUtils.random(10, true, false),
                RandomStringUtils.random(10, true, false));
        Map<String, String> propMap = new HashMap<>();
        propMap.put(prop.getPropertyKey(), prop.getPropertyValue());
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
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
                props.add(new ResearcherProperty(dacUser.getDacUserId(), researcherField.getValue(), val1));
                propMap.put(researcherField.getValue(), val2);
            }
        }
        props.add(new ResearcherProperty(dacUser.getDacUserId(), ResearcherFields.COMPLETED.getValue(), Boolean.FALSE.toString()));
        propMap.put(ResearcherFields.COMPLETED.getValue(), Boolean.FALSE.toString());
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(props);
        when(researcherPropertyDAO.isProfileCompleted(any())).thenReturn(Boolean.FALSE.toString());
        doNothing().when(researcherPropertyDAO).deletePropertiesByUserAndKey(any());
        doNothing().when(researcherPropertyDAO).insertAll(any());
        doNothing().when(researcherPropertyDAO).deleteAllPropertiesByUser(any());
        when(dacUserAPI.updateUserStatus(any(), any())).thenReturn(dacUser);
        doNothing().when(emailApi).sendNewResearcherCreatedMessage(any(), any());
        initService();

        List<ResearcherProperty> foundProps = service.updateProperties(propMap, authUser, true);
        Assert.assertFalse(foundProps.isEmpty());
        Assert.assertEquals(props.size(), foundProps.size());
        verifyZeroInteractions(emailApi);
    }

    @Test
    public void testUpdatePropertiesCompleteProfile() throws Exception {
        List<ResearcherProperty> props = new ArrayList<>();
        Map<String, String> propMap = new HashMap<>();
        for (ResearcherFields researcherField : ResearcherFields.values()) {
            if (researcherField.getRequired()) {
                String val1 = RandomStringUtils.random(10, true, false);
                String val2 = RandomStringUtils.random(10, true, false);
                props.add(new ResearcherProperty(dacUser.getDacUserId(), researcherField.getValue(), val1));
                propMap.put(researcherField.getValue(), val2);
            }
        }
        props.add(new ResearcherProperty(dacUser.getDacUserId(), ResearcherFields.COMPLETED.getValue(), Boolean.TRUE.toString()));
        propMap.put(ResearcherFields.COMPLETED.getValue(), Boolean.TRUE.toString());
        when(dacUserDAO.findDACUserByEmail(any())).thenReturn(dacUser);
        when(dacUserDAO.findDACUserById(any())).thenReturn(dacUser);
        when(researcherPropertyDAO.findResearcherPropertiesByUser(any())).thenReturn(props);
        when(researcherPropertyDAO.isProfileCompleted(any())).thenReturn(Boolean.TRUE.toString());
        doNothing().when(researcherPropertyDAO).deletePropertiesByUserAndKey(any());
        doNothing().when(researcherPropertyDAO).insertAll(any());
        doNothing().when(researcherPropertyDAO).deleteAllPropertiesByUser(any());
        when(dacUserAPI.updateUserStatus(any(), any())).thenReturn(dacUser);
        doNothing().when(emailApi).sendNewResearcherCreatedMessage(any(), any());
        initService();

        List<ResearcherProperty> foundProps = service.updateProperties(propMap, authUser, true);
        Assert.assertFalse(foundProps.isEmpty());
        Assert.assertEquals(props.size(), foundProps.size());
        verify(emailApi, atLeast(1));
    }

}
