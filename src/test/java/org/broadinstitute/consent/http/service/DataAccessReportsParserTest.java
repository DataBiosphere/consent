package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.enumeration.HeaderDAR;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DataAccessReportsParserTest {

    @Mock
    private DatasetDAO datasetDAO;
    private DataAccessReportsParser parser;
    private final String CONSENT_NAME = "ORSP-1903";
    private final String NAME = "Test";
    private final String DS_IDENTIFIER = "DUOS-000001";
    private final String RUS_SUMMARY = "Purpose";
    private final String sDUL = """
            Samples Restricted for use with "cancer" [DOID_162(CC)]
            Future use by for-profit entities is prohibited [NPU]
            Future use of aggregate-level data for general research purposes is prohibited [NPNV]
            Notes:
            Future use for methods research (analytic/software/technology development) is not prohibited
            Future use as a control set for diseases other than those specified is not prohibited
            """;
    private final String DAR_CODE = "DAR_3";
    private final String TRANSLATED_USE_RESTRICTION = "Samples will be used under the following conditions:<br>Data will be used for health/medical/biomedical research <br>Data will be used to study:  kidney-cancer [DOID_263(CC)], kidney-failure [DOID_1074(CC)]<br>Data will be used for commercial purpose [NPU] <br>";

    @Before
    public void setUp() {
        openMocks(this);
        Dataset d = new Dataset();
        d.setDataSetId(1); // This translates to an identifier of "DUOS-000001"
        d.setAlias(1);
        d.setName(NAME);
        List<Dataset> datasets = List.of(d);
        when(datasetDAO.findDatasetsByIdList(List.of(1))).thenReturn(datasets);
        this.parser = new DataAccessReportsParser(datasetDAO);
    }

    @Test
    public void testDataAccessApprovedReport() throws IOException {
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        Date currentDate = new Date();
        Election election = createElection(currentDate);
        DataAccessRequest dar = createDAR(currentDate);
        FileWriter darWriter = new FileWriter(file);
        parser.setApprovedDARHeader(darWriter);
        String REQUESTER = "Wesley";
        String ORGANIZATION = "Broad";
        parser.addApprovedDARLine(darWriter, election, dar, DAR_CODE, REQUESTER, ORGANIZATION, CONSENT_NAME, sDUL);
        darWriter.flush();
        Stream<String> stream = Files.lines(Paths.get(file.getPath()));
        Iterator<String> iterator = stream.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] columns = line.split("\t");
            assertEquals(12, columns.length);
            if (i == 0) {
                assertEquals(columns[0], HeaderDAR.DAR_ID.getValue());
                assertEquals(columns[1], HeaderDAR.DATASET_NAME.getValue());
                assertEquals(columns[2], HeaderDAR.DATASET_ID.getValue());
                assertEquals(columns[3], HeaderDAR.CONSENT_ID.getValue());
                assertEquals(columns[4], HeaderDAR.DATA_REQUESTER_NAME.getValue());
                assertEquals(columns[5], HeaderDAR.ORGANIZATION.getValue());
                assertEquals(columns[6], HeaderDAR.CODED_VERSION_SDUL.getValue());
                assertEquals(columns[7], HeaderDAR.CODED_VERSION_DAR.getValue());
                assertEquals(columns[8], HeaderDAR.RESEARCH_PURPOSE.getValue());
                assertEquals(columns[9], HeaderDAR.DATE_REQUEST_SUBMISSION.getValue());
                assertEquals(columns[10], HeaderDAR.DATE_REQUEST_APPROVAL.getValue());
                assertEquals(columns[11], HeaderDAR.DATE_REQUEST_RE_ATTESTATION.getValue());
            }
            if (i == 1) {
                assertEquals(DAR_CODE, columns[0]);
                assertEquals(NAME, columns[1]);
                assertEquals(DS_IDENTIFIER, columns[2]);
                assertEquals(CONSENT_NAME, columns[3]);
                assertEquals(REQUESTER, columns[4]);
                assertEquals(ORGANIZATION, columns[5]);
                assertEquals(columns[6], sDUL.replace("\n", " "));
                assertEquals(columns[7], TRANSLATED_USE_RESTRICTION.replace("<br>", " "));
                assertEquals(RUS_SUMMARY, columns[8]);
            }
            i++;
        }
        assertEquals(2, i);
    }

    @Test
    public void testDataAccessReviewedReport() throws IOException {
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        Date currentDate = new Date();
        Election election = createElection(currentDate);
        DataAccessRequest dar = createDAR(currentDate);
        FileWriter darWriter = new FileWriter(file);
        parser.setReviewedDARHeader(darWriter);
        parser.addReviewedDARLine(darWriter, election, dar, DAR_CODE, CONSENT_NAME, sDUL);
        darWriter.flush();
        Stream<String> stream = Files.lines(Paths.get(file.getPath()));
        Iterator<String> iterator = stream.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] columns = line.split("\t");
            assertEquals(8, columns.length);
            if (i == 0) {
                assertEquals(columns[0], HeaderDAR.DAR_ID.getValue());
                assertEquals(columns[1], HeaderDAR.DATASET_NAME.getValue());
                assertEquals(columns[2], HeaderDAR.DATASET_ID.getValue());
                assertEquals(columns[3], HeaderDAR.CONSENT_ID.getValue());
                assertEquals(columns[4], HeaderDAR.CODED_VERSION_SDUL.getValue());
                assertEquals(columns[5], HeaderDAR.CODED_VERSION_DAR.getValue());
                assertEquals(columns[6], HeaderDAR.DATE_REQUEST_APPROVAL_DISAPROVAL.getValue());
                assertEquals(columns[7], HeaderDAR.APPROVED_DISAPPROVED.getValue());
            }
            if (i == 1) {
                assertEquals(DAR_CODE, columns[0]);
                assertEquals(NAME, columns[1]);
                assertEquals(DS_IDENTIFIER, columns[2]);
                assertEquals(CONSENT_NAME, columns[3]);
                assertEquals(columns[4], sDUL.replace("\n", " "));
                assertEquals(columns[5], TRANSLATED_USE_RESTRICTION.replace("<br>", " "));
                assertEquals("Yes", columns[7]);
            }
            i++;
        }
        assertEquals(2, i);
    }

    @Test
    public void testDataAccessReviewedReportNullElectionDate() throws IOException {
        File file = File.createTempFile("ApprovedDataAccessRequests.tsv", ".tsv");
        Date currentDate = new Date();
        Election election = new Election();
        election.setFinalVote(true);
        DataAccessRequest dar = createDAR(currentDate);
        FileWriter darWriter = new FileWriter(file);
        parser.setReviewedDARHeader(darWriter);
        parser.addReviewedDARLine(darWriter, election, dar, DAR_CODE, CONSENT_NAME, sDUL);
        darWriter.flush();
        Stream<String> stream = Files.lines(Paths.get(file.getPath()));
        Iterator<String> iterator = stream.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[] columns = line.split("\t");
            assertEquals(8, columns.length);
            if (i == 0) {
                assertEquals(columns[0], HeaderDAR.DAR_ID.getValue());
                assertEquals(columns[1], HeaderDAR.DATASET_NAME.getValue());
                assertEquals(columns[2], HeaderDAR.DATASET_ID.getValue());
                assertEquals(columns[3], HeaderDAR.CONSENT_ID.getValue());
                assertEquals(columns[4], HeaderDAR.CODED_VERSION_SDUL.getValue());
                assertEquals(columns[5], HeaderDAR.CODED_VERSION_DAR.getValue());
                assertEquals(columns[6], HeaderDAR.DATE_REQUEST_APPROVAL_DISAPROVAL.getValue());
                assertEquals(columns[7], HeaderDAR.APPROVED_DISAPPROVED.getValue());
            }
            if (i == 1) {
                assertEquals(DAR_CODE, columns[0]);
                assertEquals(NAME, columns[1]);
                assertEquals(DS_IDENTIFIER, columns[2]);
                assertEquals(CONSENT_NAME, columns[3]);
                assertEquals(columns[4], sDUL.replace("\n", " "));
                assertEquals(columns[5], TRANSLATED_USE_RESTRICTION.replace("<br>", " "));
                assertEquals("Yes", columns[7]);
            }
            i++;
        }
        assertEquals(2, i);
    }

    private Election createElection(Date currentDate) {
        Election election = new Election();
        election.setFinalVoteDate(currentDate);
        election.setFinalVote(true);
        return election;
    }

    private DataAccessRequest createDAR(Date currentDate) {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setDatasetIds(List.of(1));
        DataAccessRequestData data = new DataAccessRequestData();
        data.setTranslatedUseRestriction(TRANSLATED_USE_RESTRICTION);
        data.setNonTechRus(RUS_SUMMARY);
        dar.setData(data);
        dar.setSortDate(new Timestamp(currentDate.getTime()));
        return dar;
    }
}
