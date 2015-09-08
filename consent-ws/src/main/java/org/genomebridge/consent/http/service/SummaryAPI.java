package org.genomebridge.consent.http.service;

import org.genomebridge.consent.http.models.Summary;

import java.io.File;

public interface SummaryAPI {

    Summary describeConsentSummaryCases();

    File describeConsentSummaryDetail();

    Summary describeDataRequestSummaryCases();

}
