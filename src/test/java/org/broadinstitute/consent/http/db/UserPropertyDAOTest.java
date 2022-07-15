package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class UserPropertyDAOTest extends DAOTestHelper {

    @Test
    public void testFindResearcherProperties() {
        User user = createUserWithRole(UserRoles.RESEARCHER.getRoleId());

        UserProperty suggestedInstitution = new UserProperty();
        suggestedInstitution.setPropertyKey(UserFields.SUGGESTED_INSTITUTION.getValue());
        suggestedInstitution.setPropertyValue(RandomStringUtils.random(10));
        suggestedInstitution.setUserId(user.getUserId());

        UserProperty suggestedSigningOfficial = new UserProperty();
        suggestedSigningOfficial.setPropertyKey(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue());
        suggestedSigningOfficial.setPropertyValue(RandomStringUtils.random(10));
        suggestedSigningOfficial.setUserId(user.getUserId());

        UserProperty notPresent = new UserProperty();
        notPresent.setPropertyKey("nonExistentKey");
        notPresent.setPropertyValue(RandomStringUtils.random(10));
        notPresent.setUserId(user.getUserId());

        List<UserProperty> props = userPropertyDAO.findResearcherPropertiesByUser(
                user.getUserId(),
                List.of(UserFields.SUGGESTED_INSTITUTION.getValue(),
                        UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue(),
                        UserFields.ERA_EXPIRATION_DATE.getValue()));

        Assert.assertEquals(0, props.size());

        userPropertyDAO.insertAll(List.of(
                suggestedInstitution,
                suggestedSigningOfficial,
                notPresent
        ));

        props = userPropertyDAO.findResearcherPropertiesByUser(
                user.getUserId(),
                List.of(UserFields.SUGGESTED_INSTITUTION.getValue(),
                        UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue(),
                        UserFields.ERA_EXPIRATION_DATE.getValue()));

        Assert.assertEquals(2, props.size());

        Assert.assertTrue(props.stream().anyMatch((p) ->
                (p.getPropertyKey().equals(UserFields.SUGGESTED_INSTITUTION.getValue())
                        && p.getPropertyValue().equals(suggestedInstitution.getPropertyValue()))));

        Assert.assertTrue(props.stream().anyMatch((p) ->
                (p.getPropertyKey().equals(UserFields.SUGGESTED_SIGNING_OFFICIAL.getValue())
                        && p.getPropertyValue().equals(suggestedSigningOfficial.getPropertyValue()))));
    }
}
