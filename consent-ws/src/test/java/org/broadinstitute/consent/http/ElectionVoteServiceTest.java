package org.broadinstitute.consent.http;

import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;

import javax.ws.rs.client.Client;
import java.io.IOException;
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

    public Election retrieveElection(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(Election.class);
    }

    public String electionDataRequestPath(String id) {
        try {
            return path2Url(String.format("dataRequest/%s/election", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("dataRequest/%s/election", id);
        }
    }

    public String electionDataRequestPathById(String referenceId, Integer electionId) {
        try {
            return path2Url(String.format("dataRequest/%s/election/%s", URLEncoder.encode(referenceId, "UTF-8"), electionId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("dataRequest/%s/election/%s", referenceId, electionId);
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

    public String electionPathById(Integer electionId) {
            return path2Url(String.format("election/%s",  electionId));
    }


    public String voteConsentPath(String consentId) {
        try {
            return path2Url(String.format("consent/%s/vote", URLEncoder.encode(consentId, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("consent/%s/vote", consentId);
        }
    }

    public Vote retrieveVote(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(Vote.class);
    }

    public String voteDataRequestPath(String id) {
        try {
            return path2Url(String.format("dataRequest/%s/vote", URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("dataRequest/%s/vote", id);
        }
    }

    public String voteDataRequestIdPath(String dataRequestId, Integer voteId) {
        try {
            return path2Url(String.format("dataRequest/%s/vote/%s", URLEncoder.encode(dataRequestId, "UTF-8"), voteId));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(System.err);
            return String.format("dataRequest/%s/vote/%s", dataRequestId, voteId);
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

    public String consentPendingCasesPath(Integer userId) {
        return path2Url(String.format("consent/cases/pending/%s", userId));
    }

    public String dataRequestPendingCasesPath(Integer userId) {
        return path2Url(String.format("dataRequest/cases/pending/%s", userId));
    }

    public String consentManagePath() {
        return path2Url("consent/manage");
    }

}
