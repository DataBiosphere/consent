package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.MockitoAnnotations.openMocks;


public class DACUserResourceTest {

    @Before
    public void setUp() throws Exception {
        openMocks(this);
    }

    @Test
    public void testConvertJsonToDACUserDateIgnoredCase() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": \"Oct 28, 2020\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNull(user.getCreateDate());
        Assert.assertEquals(user.getUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
    }

    @Test
    public void testConvertJsonToDACUserNoCreateDate() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        Assert.assertNotNull(user);
        Assert.assertNull(user.getCreateDate());
        Assert.assertEquals(user.getUserId().intValue(), 1);
        Assert.assertEquals(user.getEmail(), "email");
        Assert.assertEquals(user.getEmailPreference(), false);
        Assert.assertFalse(user.getRoles().isEmpty());
        Assert.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
    }
}
