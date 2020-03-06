package org.broadinstitute.consent.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ElectionVoteServiceTest extends AbstractTest {

    public String electionConsentPath(String id) {
        try {
            return path2Url(String.format("consent/%s/election", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election", id);
        }
    }

    public String dataRequestPendingCasesPath(Integer userId) {
        return path2Url(String.format("dataRequest/cases/pending/%s", userId));
    }

}
