package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserPropertyDAOTest extends DAOTestHelper {

  @Test
  public void testFindResearcherProperties() {
    User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());

    UserProperty suggestedInstitution = new UserProperty();
    suggestedInstitution.setPropertyKey(UserFields.SUGGESTED_INSTITUTION.getValue());
    suggestedInstitution.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    suggestedInstitution.setUserId(user.getUserId());

    UserProperty suggestedSigningOfficial = new UserProperty();
    suggestedSigningOfficial.setPropertyKey(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue());
    suggestedSigningOfficial.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    suggestedSigningOfficial.setUserId(user.getUserId());

    UserProperty notPresent = new UserProperty();
    notPresent.setPropertyKey("nonExistentKey");
    notPresent.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    notPresent.setUserId(user.getUserId());

    List<UserProperty> props = userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
        user.getUserId(),
        List.of(UserFields.SUGGESTED_INSTITUTION.getValue(),
            UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue(),
            UserFields.ERA_EXPIRATION_DATE.getValue()));

    assertEquals(0, props.size());

    userPropertyDAO.insertAll(List.of(
        suggestedInstitution,
        suggestedSigningOfficial,
        notPresent
    ));

    props = userPropertyDAO.findUserPropertiesByUserIdAndPropertyKeys(
        user.getUserId(),
        List.of(UserFields.SUGGESTED_INSTITUTION.getValue(),
            UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue(),
            UserFields.ERA_EXPIRATION_DATE.getValue()));

    assertEquals(2, props.size());

    assertTrue(props.stream().anyMatch((p) ->
        (p.getPropertyKey().equals(UserFields.SUGGESTED_INSTITUTION.getValue())
            && p.getPropertyValue().equals(suggestedInstitution.getPropertyValue()))));

    assertTrue(props.stream().anyMatch((p) ->
        (p.getPropertyKey().equals(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue())
            && p.getPropertyValue().equals(suggestedSigningOfficial.getPropertyValue()))));
  }

  private User createUserWithRole(Integer roleId) {
    int i1 = RandomUtils.nextInt(5, 10);
    int i2 = RandomUtils.nextInt(5, 10);
    int i3 = RandomUtils.nextInt(3, 5);
    String email = RandomStringUtils.randomAlphabetic(i1) +
        "@" +
        RandomStringUtils.randomAlphabetic(i2) +
        "." +
        org.apache.commons.lang3.RandomStringUtils.randomAlphabetic(i3);
    Integer userId = userDAO.insertUser(email, "display name", new Date());
    userRoleDAO.insertSingleUserRole(roleId, userId);
    return userDAO.findUserById(userId);
  }

}
