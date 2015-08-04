package org.genomebridge.consent.http.service;

import java.io.File;

import org.genomebridge.consent.http.models.Summary;

public interface SummaryAPI {

    Summary describeConsentSummaryCases();
    
    File describeConsentSummaryDetail();
    
    Summary describeDataRequestSummaryCases();
    
    

}
