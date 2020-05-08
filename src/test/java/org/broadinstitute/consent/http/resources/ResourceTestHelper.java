package org.broadinstitute.consent.http.resources;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.broadinstitute.consent.http.authentication.TestAuthorizer;
import org.broadinstitute.consent.http.authentication.TestOAuthAuthenticator;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public abstract class ResourceTestHelper {

    OAuthCredentialAuthFilter<AuthUser> getFilter() {
        Map<String, String> authUserRoles = new HashMap<>();
        EnumSet.allOf(UserRoles.class).forEach(e -> authUserRoles.put(e.getRoleName(), e.getRoleName()));
        List<String> authUserNames = new ArrayList<>(authUserRoles.keySet());
        TestOAuthAuthenticator authenticator = new TestOAuthAuthenticator(authUserNames);
        TestAuthorizer authorizer = new TestAuthorizer(authUserRoles);
        return new OAuthCredentialAuthFilter.
                Builder<AuthUser>().
                setAuthenticator(authenticator).
                setAuthorizer(authorizer).
                setPrefix("Bearer").
                setRealm(" ").
                buildAuthFilter();
    }

    /**
     * Construct a basic class rule for a number of resources.
     * @param resources Resource class instances to add to the test rule
     * @return ResourceTestRule
     */
    ResourceTestRule.Builder testRuleBuilder(Resource ... resources) {
        ResourceTestRule.Builder builder = ResourceTestRule.builder()
                .addProvider(MultiPartFeature.class)
                .addProvider(RolesAllowedDynamicFeature.class)
                .addProvider(new AuthDynamicFeature(getFilter()))
                .addProvider(new AuthValueFactoryProvider.Binder<>(AuthUser.class));
        Arrays.asList(resources).forEach(builder::addProvider);
        return builder;
    }

    /**
     * Authentication/authorization rules are configured such that any token with a role name will be
     * authorized as that role.
     *
     * @param role UserRole
     * @return Token
     */
    static String getRoleAuthorizationToken(UserRoles role) {
        return "Bearer " + role.getRoleName();
    }

}
