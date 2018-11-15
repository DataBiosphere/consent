package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.User;

public abstract class AbstractOAuthAuthenticator implements Authenticator<String, User> {

    protected static class AuthenticatorAPIHolder {
        private static OAuthAuthenticator  theInstance = null;

        public static void setInstance(OAuthAuthenticator  api) {
            if (theInstance != null)
                throw new IllegalStateException();
            theInstance = api;
        }

        /**
         * Get the singleton instance of the API.  If called before the instance has been initialized, an
         * IllegalStateException is thrown.
         *
         * @return The API instance.
         */
        public static OAuthAuthenticator  getInstance() {
            if (theInstance == null)
                throw new IllegalStateException();
            return theInstance;
        }

        /**
         * Clear the singleton instance of the API.  This method is used to reset the API to null if the service
         * is shut down.  This is primarily for testing purposes.  If called before the instance has been initialized,
         * an IllegalStateException is thrown.
         */
        public static void clearInstance() {
            if (theInstance == null)
                throw new IllegalStateException();
            theInstance = null;
        }

    }

    /**
     * Get the singleton instance of the API.  If called before the instance has been initialized, an
     * IllegalStateException is thrown.
     *
     * @return The API instance.
     */
    public static OAuthAuthenticator  getInstance() {
        return AuthenticatorAPIHolder.getInstance();
    }

    /**
     * Clear the singleton instance of the API.  This method is used to reset the API to null if the service
     * is shut down.  This is primarily for testing purposes.  If called before the instance has been initialized,
     * an IllegalStateException is thrown.
     */
    public static void clearInstance() {
        AuthenticatorAPIHolder.clearInstance();
    }

}
