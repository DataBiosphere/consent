package org.broadinstitute.consent.http;

public abstract class ElectionVoteServiceTest extends AbstractTest {

    public String dataRequestPendingCasesPath(Integer userId) {
        return path2Url(String.format("dataRequest/cases/pending/%s", userId));
    }

}
