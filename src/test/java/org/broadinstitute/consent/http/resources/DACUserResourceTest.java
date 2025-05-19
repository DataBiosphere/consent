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
    String json = """
        {"userId": 1, "email":"email", "what": "Huh?", "createDate": "Oct 28, 2020", "emailPreference": false,
        "roles": [{"roleId": 1, "name":"name", "what": "Huh?", "rationale": "rationale", "status": "pending"}]}""";
    User user = User.fromJson(json);
    assertNotNull(user);
    assertNull(user.getCreateDate());
    assertEquals(1, user.getUserId().intValue());
    assertEquals("email", user.getEmail());
    assertEquals(false, user.getEmailPreference());
    assertFalse(user.getRoles().isEmpty());
    assertEquals(1, user.getRoles().get(0).getRoleId().intValue());
  }

  @Test
  void testConvertJsonToDACUserNoCreateDate() {
    String json = """
        {"userId": 1, "email":"email", "what": "Huh?", "emailPreference": false, 
        "roles": [{"roleId": 1, "name":"name", "what": "Huh?", "rationale": "rationale", "status": "pending"}]}""";
    User user = User.fromJson(json);
    assertNotNull(user);
    assertNull(user.getCreateDate());
    assertEquals(1, user.getUserId().intValue());
    assertEquals("email", user.getEmail());
    assertEquals(false, user.getEmailPreference());
    assertFalse(user.getRoles().isEmpty());
    assertEquals(1, user.getRoles().get(0).getRoleId().intValue());
  }
}
