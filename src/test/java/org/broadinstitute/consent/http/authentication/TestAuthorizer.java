package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import org.broadinstitute.consent.http.models.AuthUser;

import java.util.Map;

public class TestAuthorizer implements Authorizer<AuthUser> {

    private final Map<String, String> authorizedUserRoleMap;

    public TestAuthorizer(Map<String, String> authorizedUserRoleMap) {
        this.authorizedUserRoleMap = authorizedUserRoleMap;
    }

    @Override
    public boolean authorize(AuthUser user, String role) {
        return authorizedUserRoleMap.containsKey(user.getName()) &&
                authorizedUserRoleMap.get(user.getName()).equalsIgnoreCase(role);
    }

}
