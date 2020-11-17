package org.broadinstitute.consent.http.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.AssociationType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AbstractConsentAPI.class,
        AbstractDACUserAPI.class
})
public class ConsentAssociationResourceTest {

    @Mock
    private ConsentAPI api;

    @Mock
    private DACUserAPI dacUserAPI;

    @Mock
    private UserService userService;

    private ConsentAssociationResource resource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(AbstractConsentAPI.class);
        PowerMockito.mockStatic(AbstractDACUserAPI.class);
        when(AbstractConsentAPI.getInstance()).thenReturn(api);
        when(AbstractDACUserAPI.getInstance()).thenReturn(dacUserAPI);
    }

    private void initResource() {
        resource = new ConsentAssociationResource(userService);
    }

    @Test
    public void testCreateAssociation() {
        User user = new User();
        user.setEmail("test");
        when(api.hasWorkspaceAssociation(any())).thenReturn(false);
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(api.createAssociation(any(), any(), any())).thenReturn(Collections.emptyList());
        initResource();
        AuthUser authUser = new AuthUser(user.getEmail());
        String consentId = RandomStringUtils.random(25, true, true);
        ArrayList<ConsentAssociation> associationList = new ArrayList<>();
        Response response = resource.createAssociation(authUser, consentId, associationList);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testUpdateAssociation() {
        User user = new User();
        user.setEmail("test");
        when(api.hasWorkspaceAssociation(any())).thenReturn(false);
        when(userService.findUserByEmail(any())).thenReturn(user);
        when(api.createAssociation(any(), any(), any())).thenReturn(Collections.emptyList());
        initResource();
        AuthUser authUser = new AuthUser(user.getEmail());
        String consentId = RandomStringUtils.random(25, true, true);
        ArrayList<ConsentAssociation> associationList = new ArrayList<>();
        Response response = resource.updateAssociation(authUser, consentId, associationList);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testGetAssociation() {
        when(api.getAssociation(any(), any(), any())).thenReturn(Collections.emptyList());
        initResource();
        String consentId = RandomStringUtils.random(25, true, true);
        String associationType = AssociationType.SAMPLE.getValue();
        String objectId = RandomStringUtils.random(25, true, true);
        Response response = resource.getAssociation(consentId, associationType, objectId);
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteAssociation() {
        initResource();
        String consentId = RandomStringUtils.random(25, true, true);
        String associationType = AssociationType.SAMPLE.getValue();
        String objectId = RandomStringUtils.random(25, true, true);
        Response response = resource.deleteAssociation(consentId, associationType, objectId);
        Assert.assertEquals(200, response.getStatus());
    }

}
