package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DACUserResourceTest {

  @Test
  void testConvertJsonToDACUserDateIgnoredCase() {
    String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
    String json =
        "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"createDate\": \"Oct 28, 2020\", \"emailPreference\": false, \"roles\": "
            + jsonRole + "}";
    User user = new User(json);
    assertNotNull(user);
    assertNull(user.getCreateDate());
    assertEquals(user.getUserId().intValue(), 1);
    assertEquals(user.getEmail(), "email");
    assertEquals(user.getEmailPreference(), false);
    assertFalse(user.getRoles().isEmpty());
    assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
  }

  @Test
  void testConvertJsonToDACUserNoCreateDate() {
    String jsonRole = "[{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}]";
    String json =
        "{\"userId\": 1, \"email\":\"email\", \"what\": \"Huh?\", \"emailPreference\": false, \"roles\": "
            + jsonRole + "}";
    User user = new User(json);
    assertNotNull(user);
    assertNull(user.getCreateDate());
    assertEquals(user.getUserId().intValue(), 1);
    assertEquals(user.getEmail(), "email");
    assertEquals(user.getEmailPreference(), false);
    assertFalse(user.getRoles().isEmpty());
    assertEquals(user.getRoles().get(0).getRoleId().intValue(), 1);
  }
}
