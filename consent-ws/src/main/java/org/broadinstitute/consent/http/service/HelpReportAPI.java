package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.HelpReport;

import java.util.List;

public interface HelpReportAPI {

    List<HelpReport> findHelpReportsByUserId(Integer userId);

    HelpReport findHelpReportById(Integer id);

    HelpReport create(HelpReport helpReport);

    void deleteHelpReportById(Integer id);
}
