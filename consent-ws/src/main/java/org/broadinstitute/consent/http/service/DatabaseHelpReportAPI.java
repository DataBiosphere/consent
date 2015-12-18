package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.db.HelpReportDAO;
import org.broadinstitute.consent.http.enumeration.DACUserRoles;
import org.broadinstitute.consent.http.models.HelpReport;

import java.util.Date;
import java.util.List;


public class DatabaseHelpReportAPI extends AbstractHelpReportAPI {

    private HelpReportDAO helpReportDAO;
    private DACUserRoleDAO dsRoleDAO;

    /**
     * Initialize the singleton API instance using the provided DAO.  This method should only be called once
     * during application initialization (from the run() method).  If called a second time it will throw an
     * IllegalStateException.
     * Note that this method is not synchronized, as it is not intended to be called more than once.
     *
     * @param dao The Data Access Object instance that the API should use to read/write data.
     */

    public static void initInstance(HelpReportDAO dao, DACUserRoleDAO dsRoleDAO) {
        HelpReportAPIHolder.setInstance(new DatabaseHelpReportAPI(dao, dsRoleDAO));
    }

    /**
     * The constructor is private to force use of the factory methods and enforce the singleton pattern.
     *
     * @param dao The Data Access Object used to read/write data.
     */
    private DatabaseHelpReportAPI(HelpReportDAO dao, DACUserRoleDAO dsRoleDAO) {
        this.helpReportDAO = dao;
        this.dsRoleDAO = dsRoleDAO;
    }


    @Override
    public List<HelpReport> findHelpReportsByUserId(Integer userId) {
        return  dsRoleDAO.findRoleByNameAndUser(DACUserRoles.ADMIN.getValue(), userId) != null
                ? helpReportDAO.findHelpReports() : helpReportDAO.findHelpReportsByUserId(userId);
    }

    @Override
    public HelpReport create(HelpReport helpReport){
        Integer reportId = helpReportDAO.insertHelpReport(helpReport.getUserId(), new Date(), helpReport.getSubject(), helpReport.getDescription());
        return helpReportDAO.findHelpReportById(reportId);

    }

    @Override
    public HelpReport findHelpReportById(Integer id){
        return helpReportDAO.findHelpReportById(id);
    }


    @Override
    public void deleteHelpReportById(Integer id){
        helpReportDAO.deleteHelpReportById(id);
    }


}