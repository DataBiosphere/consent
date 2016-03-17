package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.Summary;

import java.io.File;
import java.util.List;

public interface SummaryAPI {

    Summary describeConsentSummaryCases();

    File describeConsentSummaryDetail();

    File describeDataAccessRequestSummaryDetail();

    Summary describeDataRequestSummaryCases(String electionType);

    List<Summary> describeMatchSummaryCases();

    File describeDataSetElectionsVotesForDar(String darId);

}
