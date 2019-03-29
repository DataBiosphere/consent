package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.AbstractTest;

abstract class DataAccessRequestServiceTest extends AbstractTest{

    String darPath() {
        return path2Url("/dar");
    }

    String darPath(String id) {
        return path2Url(String.format("/dar/%s", id));
    }

    String invalidDarsPath() {
        return path2Url("/dar/invalid");
    }

    String darManagePath(String id) {
        return path2Url(String.format("/dar/manage?userId=%s", id));
    }

    /* Partials */

    String partialsPath() {
        return path2Url("/dar/partials");
    }

    String partialsManagePath(String id) {
        return path2Url(String.format("/dar/partials/manage?userId=%s", id));
    }

    String partialPath() {
        return path2Url("/dar/partial");
    }

    String restrictionFromQuestionsUrl() {
        return path2Url("/dar/restriction");
    }

}
