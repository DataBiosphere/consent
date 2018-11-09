package org.broadinstitute.consent.http.service.validate;

public abstract class AbstractUseRestrictionValidatorAPI implements UseRestrictionValidatorAPI {

    protected static class UseRestrictionValidatorAPIHolder {
        private static UseRestrictionValidatorAPI theInstance = null;

        /**
         * Initialize the singleton API instance.  This method should only be called once during
         * application initialization (from the run() method).  If called a second time it will throw an
         * IllegalStateException.
         * Note that this method is not synchronized, as it is not intended to be called more than once.
         *
         * @param api The instance of an API class that should be used.
         */
        public static void setInstance(UseRestrictionValidatorAPI api) {
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
        public static UseRestrictionValidatorAPI getInstance() {
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
    public static UseRestrictionValidatorAPI getInstance() {
        return UseRestrictionValidatorAPIHolder.getInstance();
    }

    /**
     * Clear the singleton instance of the API.  This method is used to reset the API to null if the service
     * is shut down.  This is primarily for testing purposes.  If called before the instance has been initialized,
     * an IllegalStateException is thrown.
     */
    public static void clearInstance() {
        UseRestrictionValidatorAPIHolder.clearInstance();
    }

}
