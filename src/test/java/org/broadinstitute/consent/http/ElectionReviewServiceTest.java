package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.ElectionReview;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class ElectionReviewServiceTest extends AbstractTest {

    public String electionReviewPath(String referenceId, String type) {
        return path2Url("electionReview?referenceId=" + referenceId + "&type="+ type);
    }

    public String openElectionReviewPath() {
        return path2Url("electionReview/openElection");
    }


    public String electionReviewByElectionIdPath(Integer id) {
        return path2Url(String.format("electionReview/%s", id));
    }

    public String lastElectionReviewPath(String id) {
        return path2Url(String.format("electionReview/last/%s", id));
    }

    public String electionConsentPath(String id) {
        try {
            return path2Url(String.format("consent/%s/election", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election", id);
        }
    }

    public String voteConsentIdPath(String consentId, Integer voteId) {
        try {
            return path2Url(String.format("consent/%s/vote/%s", URLEncoder.encode(consentId, "UTF-8"), voteId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote/%s", consentId, voteId);
        }
    }

    public String electionConsentPathById(String referenceId, Integer electionId) {
        try {
            return path2Url(String.format("consent/%s/election/%s", URLEncoder.encode(referenceId, "UTF-8"), electionId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/election/%s", referenceId, electionId);
        }
    }

    public String voteConsentPath(String consentId) {
        try {
            return path2Url(String.format("consent/%s/vote", URLEncoder.encode(consentId, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote", consentId);
        }
    }
}
