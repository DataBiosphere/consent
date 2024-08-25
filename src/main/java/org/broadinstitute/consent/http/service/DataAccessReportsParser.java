package org.broadinstitute.consent.http.service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class DataAccessReportsParser implements ConsentLogger {

  private final DatasetDAO datasetDAO;
  private final UseRestrictionConverter useRestrictionConverter;

  private static final String DEFAULT_SEPARATOR = "\t";

  private static final String END_OF_LINE = System.lineSeparator();

  public DataAccessReportsParser(DatasetDAO datasetDAO, UseRestrictionConverter useRestrictionConverter) {
    this.datasetDAO = datasetDAO;
    this.useRestrictionConverter = useRestrictionConverter;
  }

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

  public void addApprovedDARLine(FileWriter darWriter, Election election, DataAccessRequest dar,
      String darCode, String profileName, String institution, String consentName,
      String translatedUseRestriction) throws IOException {
    String rusSummary =
        Objects.nonNull(dar.getData()) && StringUtils.isNotEmpty(dar.getData().getNonTechRus())
            ? dar.getData().getNonTechRus().replace("\n", " ") : "";
    String content1 = profileName + DEFAULT_SEPARATOR + institution + DEFAULT_SEPARATOR;
    String electionDate = (Objects.nonNull(election.getFinalVoteDate())) ? formatTimeToDate(
        election.getFinalVoteDate().getTime()) : "";
    String content2 = rusSummary + DEFAULT_SEPARATOR +
        formatTimeToDate(dar.getSortDate().getTime()) + DEFAULT_SEPARATOR +
        electionDate + DEFAULT_SEPARATOR +
        "--";
    addDARLine(darWriter, dar, darCode, content1, content2, consentName, translatedUseRestriction);
  }

  public void addReviewedDARLine(FileWriter darWriter, Election election, DataAccessRequest dar,
      String darCode, String consentName, String translatedUseRestriction) throws IOException {
    String finalVote = election.getFinalVote() ? "Yes" : "No";
    String electionDate = (Objects.nonNull(election.getFinalVoteDate())) ? formatTimeToDate(
        election.getFinalVoteDate().getTime()) : "";
    String customContent2 = electionDate + DEFAULT_SEPARATOR + finalVote;
    addDARLine(darWriter, dar, darCode, "", customContent2, consentName, translatedUseRestriction);
  }

  private String formatTimeToDate(long time) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);
    int day = cal.get(Calendar.DAY_OF_MONTH);
    int month = cal.get(Calendar.MONTH) + 1;
    int year = cal.get(Calendar.YEAR);
    return String.format("%s/%s/%s", month, day, year);
  }

  private void addDARLine(FileWriter darWriter, DataAccessRequest dar, String darCode,
      String customContent1, String customContent2, String consentName,
      String translatedUseRestriction) throws IOException {
    List<String> datasetNames = new ArrayList<>();
    List<Integer> datasetIds = dar.getDatasetIds();
    List<Dataset> datasets =
        datasetIds.isEmpty() ? List.of() : datasetDAO.findDatasetsByIdList(datasetIds);
    List<String> datasetIdentifiers = new ArrayList<>();
    if (Objects.nonNull(dar.getData())) {
      for (Dataset dataset : datasets) {
        datasetIdentifiers.add(dataset.getDatasetIdentifier());
        datasetNames.add(dataset.getName());
      }
      String sDUL =
          StringUtils.isNotEmpty(translatedUseRestriction) ? translatedUseRestriction.replace("\n",
              " ") : "";
      DataUse dataUse = useRestrictionConverter.parseDataUsePurpose(dar);
      String sDAR = useRestrictionConverter.translateDataUse(dataUse, DataUseTranslationType.PURPOSE);
      String formattedSDAR = sDAR.replace("\n", " ");
      darWriter.write(
          darCode + DEFAULT_SEPARATOR +
              StringUtils.join(datasetNames, ",") + DEFAULT_SEPARATOR +
              StringUtils.join(datasetIdentifiers, ",") + DEFAULT_SEPARATOR +
              consentName + DEFAULT_SEPARATOR +
              customContent1 +
              sDUL + DEFAULT_SEPARATOR +
              formattedSDAR + DEFAULT_SEPARATOR +
              customContent2 + END_OF_LINE);

    }
  }


}
