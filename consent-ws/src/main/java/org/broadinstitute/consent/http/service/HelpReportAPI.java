package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.HelpReport;

import javax.ws.rs.NotFoundException;
import java.util.List;

public interface HelpReportAPI {

    List<HelpReport> findHelpReportsByUserId(Integer userId);

    HelpReport findHelpReportById(Integer id) throws NotFoundException;

    HelpReport create(HelpReport helpReport);

    void deleteHelpReportById(Integer id) throws NotFoundException;
}
