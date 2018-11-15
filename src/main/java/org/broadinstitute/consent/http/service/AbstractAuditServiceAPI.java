package org.broadinstitute.consent.http.service;

public abstract class AbstractAuditServiceAPI implements AuditServiceAPI{

    protected static class AuditServiceAPIHolder {
        private static AuditServiceAPI theInstance = null;

        public static void setInstance(AuditServiceAPI api) {
            if (theInstance != null)
                throw new IllegalStateException();
            theInstance = api;
        }

        public static AuditServiceAPI getInstance() {
            if (theInstance == null)
                throw new IllegalStateException();
            return theInstance;
        }

        public static void clearInstance() {
            if (theInstance == null)
                throw new IllegalStateException();
            theInstance = null;
        }

    }

    public static AuditServiceAPI getInstance() {
        return AuditServiceAPIHolder.getInstance();
    }

    public static void clearInstance() {
        AuditServiceAPIHolder.clearInstance();
    }

}
