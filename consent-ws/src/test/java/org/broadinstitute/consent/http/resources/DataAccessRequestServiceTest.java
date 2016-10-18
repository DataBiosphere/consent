package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.AbstractTest;

public abstract class DataAccessRequestServiceTest extends AbstractTest{

    public String darPath() {
        return path2Url("/dar");
    }

    public String invalidDarsPath() {
        return path2Url(String.format("/dar/invalid"));
    }

    public String darManagePath(String id) {
        return path2Url(String.format("/dar/manage?userId=%s", id));
    }

    /* Partials */

    public String partialsPath() {
        return path2Url(String.format("/dar/partials"));
    }

    public String partialsManagePath(String id) {
        return path2Url(String.format("/dar/partials/manage?userId=%s", id));
    }

    public String partialPath() {
        return path2Url(String.format("/dar/partial"));
    }

    public String restrictionFromQuestionsUrl() {
        return path2Url(String.format("/dar/restriction"));
    }

}
