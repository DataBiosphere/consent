package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPropertyDAOTest extends DAOTestHelper {

  @Test
  void testFindUserProperties() {
    User user = createUser();

    UserProperty eraExpProp = new UserProperty();
    eraExpProp.setPropertyKey(UserFields.ERA_EXPIRATION_DATE.getValue());
    eraExpProp.setPropertyValue(Instant.now().toString());
    eraExpProp.setUserId(user.getUserId());

    UserProperty notPresent = new UserProperty();
    notPresent.setPropertyKey("nonExistentKey");
    notPresent.setPropertyValue(randomAlphabetic(10));
    notPresent.setUserId(user.getUserId());

    List<UserProperty> props =
        userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(), List.of(UserFields.ERA_EXPIRATION_DATE.getValue()));

    assertEquals(0, props.size());

    userPropertyDAO.insertAll(List.of(eraExpProp, notPresent));

    props =
        userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
            user.getUserId(), List.of(UserFields.ERA_EXPIRATION_DATE.getValue()));

    assertEquals(1, props.size());

    assertTrue(
        props.stream()
            .anyMatch(
                (p) ->
                    (p.getPropertyKey().equals(UserFields.ERA_EXPIRATION_DATE.getValue())
                        && p.getPropertyValue().equals(eraExpProp.getPropertyValue()))));
  }
}
