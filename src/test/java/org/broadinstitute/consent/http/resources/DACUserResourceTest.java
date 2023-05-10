package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;

import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class DACUserResourceTest {

    @BeforeEach
    public void setUp() throws Exception {
        openMocks(this);
    }

    @Test
    public void testConvertJsonToDACUserDateIgnoredCase() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": \"Oct 28, 2020\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        assertNotNull(user);
        Assertions.assertNull(user.getCreateDate());
        Assertions.assertEquals(user.getUserId().intValue(), 1);
        Assertions.assertEquals(user.getEmail(), "email");
        Assertions.assertEquals(user.getEmailPreference(), false);
        assertFalse(user.getRoles().isEmpty());
        Assertions.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
    }

    @Test
    public void testConvertJsonToDACUserNoCreateDate() {
        String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
        String json = "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"emailPreference\": false, \"roles\": " + jsonRole + "}";
        User user = new User(json);
        assertNotNull(user);
        Assertions.assertNull(user.getCreateDate());
        Assertions.assertEquals(user.getUserId().intValue(), 1);
        Assertions.assertEquals(user.getEmail(), "email");
        Assertions.assertEquals(user.getEmailPreference(), false);
        assertFalse(user.getRoles().isEmpty());
        Assertions.assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
    }
}
