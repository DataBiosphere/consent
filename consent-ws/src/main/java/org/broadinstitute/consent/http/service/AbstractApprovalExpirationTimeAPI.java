package org.broadinstitute.consent.http.service;


/**
 * Implement a skeleton for ApprovalTimeExpirationAPI interface that implements the singleton
 * object management.
 */
public abstract class AbstractApprovalExpirationTimeAPI implements ApprovalExpirationTimeAPI {

    /**
     * Inner class to hold onto the singleton instance. This has delayed
     * initialization, and the subclass must implement a initInstance() method
     * to call setInstance().
     */
    protected static class ApprovalExpirationTimeAPIHolder {
        private static ApprovalExpirationTimeAPI theInstance = null;

        /**
         * Initialize the singleton API instance. This method should only be
         * called once during application initialization (from the run()
         * method). If called a second time it will throw an
         * IllegalStateException. Note that this method is not synchronized, as
         * it is not intended to be called more than once.
         *
         * @param api The instance of an API class that should be used.
         */
        public static void setInstance(ApprovalExpirationTimeAPI api) {
            if (theInstance != null)
                throw new IllegalStateException();
            theInstance = api;
        }

        /**
         * Get the singleton instance of the API. If called before the instance
         * has been initialized, an IllegalStateException is thrown.
         *
         * @return The API instance.
         */
        public static ApprovalExpirationTimeAPI getInstance() {
            if (theInstance == null)
                throw new IllegalStateException();
            return theInstance;
        }

        /**
         * Clear the singleton instance of the API. This method is used to reset
         * the API to null if the service is shut down. This is primarily for
         * testing purposes. If called before the instance has been initialized,
         * an IllegalStateException is thrown.
         */
        public static void clearInstance() {
            if (theInstance == null)
                throw new IllegalStateException();
            theInstance = null;
        }

    }

    /**
     * Get the singleton instance of the API. If called before the instance has
     * been initialized, an IllegalStateException is thrown.
     *
     * @return The API instance.
     */
    public static ApprovalExpirationTimeAPI getInstance() {
        return ApprovalExpirationTimeAPIHolder.getInstance();
    }

    /**
     * Clear the singleton instance of the API. This method is used to reset the
     * API to null if the service is shut down. This is primarily for testing
     * purposes. If called before the instance has been initialized, an
     * IllegalStateException is thrown.
     */
    public static void clearInstance() {
        ApprovalExpirationTimeAPIHolder.clearInstance();
    }


}
