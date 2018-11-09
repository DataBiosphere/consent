package org.broadinstitute.consent.http.service;

/**
 * Implement a skeleton for DataSetAPI interface that implements the singleton object management.
 */
public abstract class AbstractDataSetAssociationAPI implements DataSetAssociationAPI {
    // Implement singleton management for the DataSetAssociationAPI.  We need to explicitly implement a singleton pattern,
    // to work around a problem with Dropwizard+GUICE lifecycle management.  Basically using GUICE to create
    // the singleton will cause it to be created too early, so rather than use dependency injection, the
    // factory methods below will create the singleton and give access to it.
    // see https://github.com/HubSpot/dropwizard-guice/issues/19 for discussion of the underlying problem.

    /**
     * Inner class to hold onto the singleton instance.  This has delayed initialization, and the subclass
     * must implement a initInstance() method to call setInstance().
     */
    protected static class DataSetAssociationAPIHolder {
        private static DataSetAssociationAPI theInstance = null;

        /**
         * Initialize the singleton API instance.  This method should only be called once during
         * application initialization (from the run() method).  If called a second time it will throw an
         * IllegalStateException.
         * Note that this method is not synchronized, as it is not intended to be called more than once.
         *
         * @param api The instance of an API class that should be used.
         */
        public static void setInstance(DataSetAssociationAPI api) {
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
        public static DataSetAssociationAPI getInstance() {
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
    public static DataSetAssociationAPI getInstance() {
        return DataSetAssociationAPIHolder.getInstance();
    }

    /**
     * Clear the singleton instance of the API.  This method is used to reset the API to null if the service
     * is shut down.  This is primarily for testing purposes.  If called before the instance has been initialized,
     * an IllegalStateException is thrown.
     */
    public static void clearInstance() {
        DataSetAssociationAPIHolder.clearInstance();
    }

}
