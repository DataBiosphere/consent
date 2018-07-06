package org.broadinstitute.consent.http.service;

import com.vividsolutions.jts.util.Assert;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class DataAccessReportsParserTest {

    DataAccessReportsParser parser;
    private final String DATASET_ID = "SC-01253";
    private final String CONSENT_NAME = "ORSP-1903";
    private final String REQUESTER = "Wesley";
    private final String ORGANIZATION = "Broad";
    private final String RUS_SUMMARY = "Purpose";
    private final String sDUL = "Samples Restricted for use with \"cancer\" [DOID_162(CC)]\n" +
            "Future use by for-profit entities is prohibited [NPU]\n" +
            "Future use of aggregate-level data for general research purposes is prohibited [NPNV]\n" +
            "Notes:\n" +
            "Future use for methods research (analytic/software/technology development) is not prohibited\n" +
            "Future use as a control set for diseases other than those specified is not prohibited";
    private final String DAR_CODE = "DAR_3";
    private final String TRANSLATED_USE_RESTRICTION = "Samples will be used under the following conditions:<br>Data will be used for health/medical/biomedical research <br>Data will be used to study:  kidney-cancer [DOID_263(CC)], kidney-failure [DOID_1074(CC)]<br>Data will be used for commercial purpose [NPU] <br>";
    private final String EMAIL = "vvicario@test.com";

    public DataAccessReportsParserTest() {
        this.parser = new DataAccessReportsParser();
    }

    @Test
    public void testDataAccessApprovedReport() throws IOException {
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        Date currentDate = new Date();
        Election election = createElection(currentDate);
        Document dar = createDAR(currentDate);
        FileWriter darWriter = new FileWriter(file);
        parser.setApprovedDARHeader(darWriter);
        parser.addApprovedDARLine(darWriter, election, dar, REQUESTER, ORGANIZATION, CONSENT_NAME, sDUL);
        darWriter.flush();
        Stream<String> stream = Files.lines(Paths.get(file.getPath()));
        Iterator<String> iterator = stream.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] columns = line.split("\t");
            Assert.isTrue(columns.length == 12);
            if(i == 0) {
                Assert.isTrue(columns[0].equals(HeaderDAR.DAR_ID.getValue()));
                Assert.isTrue(columns[1].equals(HeaderDAR.DATASET_NAME.getValue()));
                Assert.isTrue(columns[2].equals(HeaderDAR.DATASET_ID.getValue()));
                Assert.isTrue(columns[3].equals(HeaderDAR.CONSENT_ID.getValue()));
                Assert.isTrue(columns[4].equals(HeaderDAR.DATA_REQUESTER_NAME.getValue()));
                Assert.isTrue(columns[5].equals(HeaderDAR.ORGANIZATION.getValue()));
                Assert.isTrue(columns[6].equals(HeaderDAR.CODED_VERSION_SDUL.getValue()));
                Assert.isTrue(columns[7].equals(HeaderDAR.CODED_VERSION_DAR.getValue()));
                Assert.isTrue(columns[8].equals(HeaderDAR.RESEARCH_PURPOSE.getValue()));
                Assert.isTrue(columns[9].equals(HeaderDAR.DATE_REQUEST_SUBMISSION.getValue()));
                Assert.isTrue(columns[10].equals(HeaderDAR.DATE_REQUEST_APPROVAL.getValue()));
                Assert.isTrue(columns[11].equals(HeaderDAR.DATE_REQUEST_RE_ATTESTATION.getValue()));
            }
            if (i == 1) {
                Assert.isTrue(columns[0].equals(DAR_CODE));
                Assert.isTrue(columns[1].equals(" "));
                Assert.isTrue(columns[2].equals(DATASET_ID));
                Assert.isTrue(columns[3].equals(CONSENT_NAME));
                Assert.isTrue(columns[4].equals(REQUESTER));
                Assert.isTrue(columns[5].equals(ORGANIZATION));
                Assert.isTrue(columns[6].equals(sDUL.replace("\n", " ")));
                Assert.isTrue(columns[7].equals(TRANSLATED_USE_RESTRICTION.replace("<br>"," ")));
                Assert.isTrue(columns[8].equals(RUS_SUMMARY));
            }
            i++;
        }
        Assert.isTrue(i == 2);
    }

    @Test
    public void testDataAccessReviewedReport() throws IOException {
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        Date currentDate = new Date();
        Election election = createElection(currentDate);
        Document dar = createDAR(currentDate);
        FileWriter darWriter = new FileWriter(file);
        parser.setReviewedDARHeader(darWriter);
        parser.addReviewedDARLine(darWriter, election, dar, CONSENT_NAME, sDUL);
        darWriter.flush();
        Stream<String> stream = Files.lines(Paths.get(file.getPath()));
        Iterator<String> iterator = stream.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] columns = line.split("\t");
            Assert.isTrue(columns.length == 8);
            if(i == 0) {
                Assert.isTrue(columns[0].equals(HeaderDAR.DAR_ID.getValue()));
                Assert.isTrue(columns[1].equals(HeaderDAR.DATASET_NAME.getValue()));
                Assert.isTrue(columns[2].equals(HeaderDAR.DATASET_ID.getValue()));
                Assert.isTrue(columns[3].equals(HeaderDAR.CONSENT_ID.getValue()));
                Assert.isTrue(columns[4].equals(HeaderDAR.CODED_VERSION_SDUL.getValue()));
                Assert.isTrue(columns[5].equals(HeaderDAR.CODED_VERSION_DAR.getValue()));
                Assert.isTrue(columns[6].equals(HeaderDAR.DATE_REQUEST_APPROVAL_DISAPROVAL.getValue()));
                Assert.isTrue(columns[7].equals(HeaderDAR.APPROVED_DISAPPROVED.getValue()));
            }
            if (i == 1) {
                Assert.isTrue(columns[0].equals(DAR_CODE));
                Assert.isTrue(columns[1].equals(" "));
                Assert.isTrue(columns[2].equals(DATASET_ID));
                Assert.isTrue(columns[3].equals(CONSENT_NAME));
                Assert.isTrue(columns[4].equals(sDUL.replace("\n", " ")));
                Assert.isTrue(columns[5].equals(TRANSLATED_USE_RESTRICTION.replace("<br>"," ")));
                Assert.isTrue(columns[7].equals("Yes"));
            }
            i++;
        }
        Assert.isTrue(i == 2);
    }
    
    @Test
    public void testDataSetApprovedUsers() throws IOException{
        File file = File.createTempFile("DataSetApprovedUsers", ".tsv");
        FileWriter darWriter = new FileWriter(file);
        parser.setDataSetApprovedUsersHeader(darWriter);
        Date approvalDate = new Date();
        parser.addDataSetApprovedUsersLine(darWriter, EMAIL, REQUESTER, ORGANIZATION, DAR_CODE, approvalDate);
        darWriter.flush();
        Stream<String> stream = Files.lines(Paths.get(file.getPath()));
        Iterator<String> iterator = stream.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] columns = line.split("\t");
            Assert.isTrue(columns.length == 6);
            if(i == 0) {
                Assert.isTrue(columns[0].equals(HeaderDAR.USERNAME.getValue()));
                Assert.isTrue(columns[1].equals(HeaderDAR.NAME.getValue()));
                Assert.isTrue(columns[2].equals(HeaderDAR.ORGANIZATION.getValue()));
                Assert.isTrue(columns[3].equals(HeaderDAR.DAR_ID.getValue()));
                Assert.isTrue(columns[4].equals(HeaderDAR.DATE_REQUEST_APPROVAL.getValue()));
                Assert.isTrue(columns[5].equals(HeaderDAR.RENEWAL_DATE.getValue()));
            }
            if (i == 1) {
                Assert.isTrue(columns[0].equals(EMAIL));
                Assert.isTrue(columns[1].equals(REQUESTER));
                Assert.isTrue(columns[2].equals(ORGANIZATION));
                Assert.isTrue(columns[3].equals(DAR_CODE));
            }
            i++;
        }
    }

    private Election createElection(Date currentDate){
        Election election = new Election();
        election.setTranslatedUseRestriction(TRANSLATED_USE_RESTRICTION);
        election.setFinalVoteDate(currentDate);
        election.setFinalVote(true);
        return election;
    }

    private Document createDAR(Date currentDate) {
        Document dar = new Document();
        ArrayList<String> dataSets = new ArrayList<>(Arrays.asList(DATASET_ID));
        dar.put(DarConstants.DATASET_ID, dataSets);
        dar.put(DarConstants.DAR_CODE, DAR_CODE);
        dar.put(DarConstants.TRANSLATED_RESTRICTION, TRANSLATED_USE_RESTRICTION);
        dar.put(DarConstants.NON_TECH_RUS, RUS_SUMMARY);
        dar.put(DarConstants.SORT_DATE, currentDate);
        return dar;
    }
}
