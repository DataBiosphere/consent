package org.broadinstitute.consent.http.db;


import org.broadinstitute.consent.http.models.HelpReport;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

import java.util.Date;
import java.util.List;

@RegisterRowMapper(HelpReportMapper.class)
public interface HelpReportDAO extends Transactional<HelpReportDAO> {

    @SqlQuery("select hr.subject, hr.report_id, hr.create_date, hr.description, du.displayName from help_report hr inner join dacuser du on du.dacUserId = hr.user_id  where user_id = :dacUserId")
    List<HelpReport> findHelpReportsByUserId(@Bind("dacUserId") Integer dacUserId);

    @SqlQuery("select hr.subject, hr.report_id, hr.create_date, hr.description, du.displayName from help_report hr inner join dacuser du on du.dacUserId = hr.user_id ")
    List<HelpReport> findHelpReports();

    @SqlUpdate("insert into help_report (user_id, create_date, subject, description) values (:userId, :createDate, :subject, :description)")
    @GetGeneratedKeys
    Integer insertHelpReport(@Bind("userId") Integer userId,
                          @Bind("createDate") Date createDate,
                          @Bind("subject") String subject,
                          @Bind("description") String description);

    @SqlQuery("select hr.subject, hr.report_id, hr.create_date, hr.description, du.displayName from help_report hr inner join dacuser du on du.dacUserId = hr.user_id where report_id = :reportId ")
    HelpReport findHelpReportById(@Bind("reportId")Integer reportId);

    @SqlUpdate("delete  from help_report where report_id = :id")
    void deleteHelpReportById(@Bind("id") Integer id);

}



