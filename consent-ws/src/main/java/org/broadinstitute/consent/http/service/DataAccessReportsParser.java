package org.broadinstitute.consent.http.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DataAccessReportsParser {

    private static final String DEFAULT_SEPARATOR =  "\t";;

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

    public void addApprovedDARLine(FileWriter darWriter, Election election, Document dar, String profileName, String institution, Consent consent) throws IOException {
        List<Document> dataSetDetail = dar.get(DarConstants.DATASET_DETAIL, ArrayList.class);
        String dataSetName = CollectionUtils.isNotEmpty(dataSetDetail) ? dataSetDetail.get(0).getString("name") : " ";
        String sDUL = StringUtils.isNotEmpty(consent.getTranslatedUseRestriction()) ?  consent.getTranslatedUseRestriction().replace("\n", " ") : "";
        String translatedRestriction = StringUtils.isNotEmpty(dar.getString(DarConstants.TRANSLATED_RESTRICTION)) ? dar.getString(DarConstants.TRANSLATED_RESTRICTION).replace("<br>", " ") :  "";
        String rus = StringUtils.isNotEmpty( dar.getString(DarConstants.RUS)) ?  dar.getString(DarConstants.RUS).replace("\n", " ") : "";
        darWriter.write(
                dar.getString(DarConstants.DAR_CODE) + DEFAULT_SEPARATOR +
                dataSetName + DEFAULT_SEPARATOR +
                dar.get(DarConstants.DATASET_ID, ArrayList.class).get(0).toString() + DEFAULT_SEPARATOR +
                consent.getName() + DEFAULT_SEPARATOR +
                profileName + DEFAULT_SEPARATOR +
                institution + DEFAULT_SEPARATOR +
                sDUL + DEFAULT_SEPARATOR +
                translatedRestriction + DEFAULT_SEPARATOR +
                rus + DEFAULT_SEPARATOR +
                formatTimeToDate(dar.getDate(DarConstants.SORT_DATE).getTime()) + DEFAULT_SEPARATOR +
                formatTimeToDate(election.getFinalVoteDate().getTime()) + DEFAULT_SEPARATOR +
                " " + END_OF_LINE);
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

    public void addReviewedDARLine(FileWriter darWriter, Election election, Document dar, Consent consent) throws IOException {
        List<Document> dataSetDetail = dar.get(DarConstants.DATASET_DETAIL, ArrayList.class);
        String sDUL = StringUtils.isNotEmpty(consent.getTranslatedUseRestriction()) ? consent.getTranslatedUseRestriction().replace("\n", " ") : "";
        String translatedRestriction = StringUtils.isNotEmpty(dar.getString(DarConstants.TRANSLATED_RESTRICTION)) ? dar.getString(DarConstants.TRANSLATED_RESTRICTION).replace("<br>", " ") :  "";
        String finalVote = election.getFinalVote() ? "Yes" : "No";
        String dataSetName = CollectionUtils.isNotEmpty(dataSetDetail) ? dataSetDetail.get(0).getString("name") : " ";
        darWriter.write(
                dar.getString(DarConstants.DAR_CODE) + DEFAULT_SEPARATOR +
                        dataSetName + DEFAULT_SEPARATOR +
                        dar.get(DarConstants.DATASET_ID, ArrayList.class).get(0).toString() + DEFAULT_SEPARATOR +
                        consent.getName() + DEFAULT_SEPARATOR +
                        sDUL + DEFAULT_SEPARATOR +
                        translatedRestriction + DEFAULT_SEPARATOR +
                        formatTimeToDate(election.getFinalVoteDate().getTime()) + DEFAULT_SEPARATOR +
                        finalVote + END_OF_LINE);
    }

    private String formatTimeToDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        Integer day = cal.get(Calendar.DAY_OF_MONTH);
        Integer month = cal.get(Calendar.MONTH) + 1;
        Integer year = cal.get(Calendar.YEAR);
        return month.toString() + "/" + day.toString() + "/" + year.toString();
    }


}
