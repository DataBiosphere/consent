package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPropertyDAOTest extends DAOTestHelper {

  @Test
  void testFindUserProperties() {
    User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());

    UserProperty suggestedSigningOfficial = new UserProperty();
    suggestedSigningOfficial.setPropertyKey(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue());
    suggestedSigningOfficial.setPropertyValue(randomAlphabetic(10));
    suggestedSigningOfficial.setUserId(user.getUserId());

    UserProperty notPresent = new UserProperty();
    notPresent.setPropertyKey("nonExistentKey");
    notPresent.setPropertyValue(randomAlphabetic(10));
    notPresent.setUserId(user.getUserId());

    List<UserProperty> props = userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
        user.getUserId(),
        List.of(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue(),
            UserFields.ERA_EXPIRATION_DATE.getValue()));

    assertEquals(0, props.size());

    userPropertyDAO.insertAll(List.of(
        suggestedSigningOfficial,
        notPresent
    ));

    props = userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
        user.getUserId(),
        List.of(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue(),
            UserFields.ERA_EXPIRATION_DATE.getValue()));

    assertEquals(1, props.size());

    assertTrue(props.stream().anyMatch((p) ->
        (p.getPropertyKey().equals(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue())
            && p.getPropertyValue().equals(suggestedSigningOfficial.getPropertyValue()))));
  }
}
