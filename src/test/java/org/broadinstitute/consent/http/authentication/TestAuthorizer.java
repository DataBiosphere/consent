package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import java.util.Map;
import org.broadinstitute.consent.http.models.AuthUser;

public class TestAuthorizer implements Authorizer<AuthUser> {

    private final Map<String, String> authorizedUserRoleMap;

    public TestAuthorizer(Map<String, String> authorizedUserRoleMap) {
        this.authorizedUserRoleMap = authorizedUserRoleMap;
    }

    @Override
    public boolean authorize(AuthUser user, String role) {
        return authorizedUserRoleMap.containsKey(user.getEmail()) &&
                authorizedUserRoleMap.get(user.getEmail()).equalsIgnoreCase(role);
    }

}
