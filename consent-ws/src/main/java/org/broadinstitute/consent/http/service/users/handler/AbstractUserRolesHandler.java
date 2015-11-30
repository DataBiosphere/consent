package org.broadinstitute.consent.http.service.users.handler;

public abstract class AbstractUserRolesHandler implements UserHandlerAPI {

    protected static class UserHandlerAPIHolder {

        private static UserHandlerAPI theInstance = null;

        public static void setInstance(UserHandlerAPI api) {
            if (theInstance != null) {
                throw new IllegalStateException();
            }
            theInstance = api;
        }

        public static UserHandlerAPI getInstance() {
            if (theInstance == null) {
                throw new IllegalStateException();
            }
            return theInstance;
        }

        public static void clearInstance() {
            if (theInstance == null) {
                throw new IllegalStateException();
            }
            theInstance = null;
        }

    }

    public static UserHandlerAPI getInstance() {
        return UserHandlerAPIHolder.getInstance();
    }

    public static void clearInstance() {
        UserHandlerAPIHolder.clearInstance();
    }
}
