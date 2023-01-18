package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetDetailEntry;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class DataAccessReportsParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String DEFAULT_SEPARATOR =  "\t";

    private static final String END_OF_LINE = System.lineSeparator();

    public void setApprovedDARHeader(FileWriter darWriter) throws IOException {
        darWriter.write(
                HeaderDAR.DAR_ID.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATASET_NAME.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATASET_ID.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.CONSENT_ID.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATA_REQUESTER_NAME.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.ORGANIZATION.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.CODED_VERSION_SDUL.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.CODED_VERSION_DAR.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.RESEARCH_PURPOSE.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATE_REQUEST_SUBMISSION.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATE_REQUEST_APPROVAL.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATE_REQUEST_RE_ATTESTATION.getValue() + END_OF_LINE);
    }

    public void setReviewedDARHeader(FileWriter darWriter) throws IOException {
        darWriter.write(
                HeaderDAR.DAR_ID.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATASET_NAME.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATASET_ID.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.CONSENT_ID.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.CODED_VERSION_SDUL.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.CODED_VERSION_DAR.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.DATE_REQUEST_APPROVAL_DISAPROVAL.getValue() + DEFAULT_SEPARATOR +
                        HeaderDAR.APPROVED_DISAPPROVED.getValue() + END_OF_LINE);
    }

  public String getDatasetApprovedUsersHeader(User user) {
    StringBuilder builder = new StringBuilder();
    if (user.doesUserHaveAnyRoleInSet(EnumSet.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER))) {
      builder
        .append(HeaderDAR.USERNAME.getValue())
        .append(DEFAULT_SEPARATOR);
    }
    builder
        .append(HeaderDAR.NAME.getValue())
        .append(DEFAULT_SEPARATOR)
        .append(HeaderDAR.ORGANIZATION.getValue())
        .append(DEFAULT_SEPARATOR)
        .append(HeaderDAR.DAR_ID.getValue())
        .append(DEFAULT_SEPARATOR)
        .append(HeaderDAR.DATE_REQUEST_APPROVAL.getValue())
        .append(END_OF_LINE);
    return builder.toString();
  }

    public void addApprovedDARLine(FileWriter darWriter, Election election, DataAccessRequest dar, String darCode, String profileName, String institution, String consentName, String translatedUseRestriction) throws IOException {
        String rusSummary = Objects.nonNull(dar.getData()) && StringUtils.isNotEmpty(dar.getData().getNonTechRus()) ?  dar.getData().getNonTechRus().replace("\n", " ") : "";
        String content1 =  profileName + DEFAULT_SEPARATOR + institution + DEFAULT_SEPARATOR;
        String electionDate = (Objects.nonNull(election.getFinalVoteDate())) ? formatTimeToDate(election.getFinalVoteDate().getTime()) : "";
        String content2 = rusSummary + DEFAULT_SEPARATOR +
                formatTimeToDate(dar.getSortDate().getTime()) + DEFAULT_SEPARATOR +
                electionDate + DEFAULT_SEPARATOR +
                "--";
        addDARLine(darWriter, dar, darCode, content1, content2, consentName, translatedUseRestriction);
    }

    public void addReviewedDARLine(FileWriter darWriter, Election election, DataAccessRequest dar, String darCode, String consentName, String translatedUseRestriction) throws IOException {
        String finalVote = election.getFinalVote() ? "Yes" : "No";
        String electionDate = (Objects.nonNull(election.getFinalVoteDate())) ? formatTimeToDate(election.getFinalVoteDate().getTime()) : "";
        String content2 = electionDate + DEFAULT_SEPARATOR + finalVote;
        addDARLine(darWriter, dar, darCode, "", content2, consentName, translatedUseRestriction);
    }

    public String getDataSetApprovedUsersLine(User user, String email, String name, String institution, String darCode, Date approvalDate) {
      StringBuilder builder = new StringBuilder();
      if (user.doesUserHaveAnyRoleInSet(EnumSet.of(UserRoles.ADMIN, UserRoles.CHAIRPERSON, UserRoles.MEMBER))) {
        builder
            .append(email)
            .append(DEFAULT_SEPARATOR);
      }
      builder
          .append(name)
          .append(DEFAULT_SEPARATOR)
          .append(institution)
          .append(DEFAULT_SEPARATOR)
          .append(darCode)
          .append(DEFAULT_SEPARATOR)
          .append(formatTimeToDate(approvalDate.getTime()))
          .append(END_OF_LINE);
      return builder.toString();
    }

    private String formatTimeToDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        Integer day = cal.get(Calendar.DAY_OF_MONTH);
        Integer month = cal.get(Calendar.MONTH) + 1;
        Integer year = cal.get(Calendar.YEAR);
        return month.toString() + "/" + day.toString() + "/" + year.toString();
    }

    private void addDARLine(FileWriter darWriter, DataAccessRequest dar, String darCode, String customContent1, String customContent2, String consentName, String translatedUseRestriction) throws IOException {
        List<String> datasetNames = new ArrayList<>();
        List<Integer> dataSetIds = new ArrayList<>();
        List<String> dataSetUUIds = new ArrayList<>();
        if (Objects.nonNull(dar) && Objects.nonNull(dar.getData())) {
            List<DatasetDetailEntry> dataSetDetails = dar.getData().getDatasetDetail();
            for (DatasetDetailEntry detail : dataSetDetails) {
                try {
                    Integer id = Integer.parseInt(detail.getDatasetId());
                    dataSetIds.add(id);
                    dataSetUUIds.add(Dataset.parseAliasToIdentifier(id));
                } catch (Exception e) {
                    logger.warn("Invalid Dataset ID: " + detail.getDatasetId());
                }
                datasetNames.add(detail.getName());
            }
            String sDUL = StringUtils.isNotEmpty(translatedUseRestriction) ? translatedUseRestriction.replace("\n", " ") : "";
            String translatedRestriction = StringUtils.isNotEmpty(dar.getData().getTranslatedUseRestriction()) ? dar.getData().getTranslatedUseRestriction().replace("<br>", " ") : "";
            darWriter.write(
               darCode + DEFAULT_SEPARATOR +
                StringUtils.join(datasetNames, ",") + DEFAULT_SEPARATOR +
                StringUtils.join(dataSetUUIds, ",") + DEFAULT_SEPARATOR +
                consentName + DEFAULT_SEPARATOR +
                customContent1 +
                sDUL + DEFAULT_SEPARATOR +
                translatedRestriction + DEFAULT_SEPARATOR +
                customContent2 + END_OF_LINE);

        }
    }


}
