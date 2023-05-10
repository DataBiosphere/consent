package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

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

        Assertions.assertEquals(0, props.size());

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

        Assertions.assertEquals(2, props.size());

        Assertions.assertTrue(props.stream().anyMatch((p) ->
                (p.getPropertyKey().equals(UserFields.SUGGESTED_INSTITUTION.getValue())
                        && p.getPropertyValue().equals(suggestedInstitution.getPropertyValue()))));

        Assertions.assertTrue(props.stream().anyMatch((p) ->
                (p.getPropertyKey().equals(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue())
                        && p.getPropertyValue().equals(suggestedSigningOfficial.getPropertyValue()))));
    }
}
